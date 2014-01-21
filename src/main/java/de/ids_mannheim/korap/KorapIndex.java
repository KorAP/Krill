package de.ids_mannheim.korap;

import java.util.*;

import java.io.File;
import java.io.IOException;

// import java.net.URL;

import java.nio.ByteBuffer;
import java.util.zip.GZIPInputStream;
import java.io.FileInputStream;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.QueryWrapperFilter;

import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.spans.SpanOrQuery;

import org.apache.lucene.document.Document;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;

import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;

import org.apache.lucene.search.Filter;
import org.apache.lucene.search.DocIdSetIterator;

import org.apache.lucene.util.Version;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.OpenBitSet;
import org.apache.lucene.util.FixedBitSet;

// Automata
import org.apache.lucene.util.automaton.Automaton;
import org.apache.lucene.util.automaton.RegExp;
import org.apache.lucene.util.automaton.CompiledAutomaton;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.ids_mannheim.korap.KorapResult;
import de.ids_mannheim.korap.KorapMatch;
import de.ids_mannheim.korap.KorapCollection;
import de.ids_mannheim.korap.KorapSearch;
import de.ids_mannheim.korap.index.FieldDocument;
import de.ids_mannheim.korap.index.PositionsToOffset;
import de.ids_mannheim.korap.index.TermInfo;
import de.ids_mannheim.korap.index.SpanInfo;
import de.ids_mannheim.korap.index.MatchIdentifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
  Todo: Use FieldCache!
  TODO: Reuse the indexreader everywhere - it should be threadsafe!

  http://invertedindex.blogspot.co.il/2009/04/lucene-dociduid-mapping-and-payload.html
  see korap/search.java -> retrieveTokens

  Support a callback for interrupts (to stop the searching)!

  Support multiple indices.

  Support frequency search with regular expressions, so multiple bookkeeping:
  c<:VVFIN:ging:gehen:past::
  c>:VVFIN:gnig:neheg:past::
  -> search for frequencies of VVFIN/gehen
  -> c:VVFIN:[^:]*?:gehen:past:...
*/

/**
 * KorapIndex implements a simple API for searching in and writing to a
 * Lucene index and equesting several information but the index's nature.
 *
 * @author ndiewald
 */
public class KorapIndex {
    private Directory directory;

    // Temp:
    public IndexReader reader;

    private IndexWriter writer;
    private IndexSearcher searcher;
    private boolean readerOpen = false;
    private int commitCounter = 0;
    private int autoCommit = 500; // Todo: Use configuration
    private HashMap termContexts;
    private ObjectMapper mapper = new ObjectMapper();


    private static ByteBuffer bb       = ByteBuffer.allocate(4),
	                      bbOffset = ByteBuffer.allocate(8),
	                      bbTerm   = ByteBuffer.allocate(16);

    private byte[] pl = new byte[4];

    private Set<String> fieldsToLoad;

    // Logger
    private final static Logger log = LoggerFactory.getLogger(KorapIndex.class);

    public KorapIndex () throws IOException {
        this((Directory) new RAMDirectory());
    };


    public KorapIndex (String index) throws IOException {
	this(FSDirectory.open(new File( index )));
    };

    public KorapIndex (Directory directory) throws IOException {
	this.directory = directory;

	fieldsToLoad = new HashSet<String>(16);
	fieldsToLoad.add("author");
	fieldsToLoad.add("ID");
	fieldsToLoad.add("title");
	fieldsToLoad.add("subTitle");
	fieldsToLoad.add("textClass");
	fieldsToLoad.add("pubPlace");
	fieldsToLoad.add("pubDate");
	fieldsToLoad.add("corpusID");
	fieldsToLoad.add("foundries");
	fieldsToLoad.add("layerInfo");
	fieldsToLoad.add("tokenization");

	// Base analyzer for searching and indexing
	// StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_CURRENT);

	Map<String,Analyzer> analyzerPerField = new HashMap<String,Analyzer>();
	analyzerPerField.put("textClass", new WhitespaceAnalyzer(Version.LUCENE_CURRENT));
	analyzerPerField.put("foundries", new WhitespaceAnalyzer(Version.LUCENE_CURRENT));
	PerFieldAnalyzerWrapper analyzer = new PerFieldAnalyzerWrapper(
            new StandardAnalyzer(Version.LUCENE_CURRENT),
            analyzerPerField
        );

	// Create configuration with base analyzer
	IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_CURRENT, analyzer);

	this.writer = new IndexWriter(this.directory, config);
    };


    public void close () throws IOException {
	this.closeReader();
	this.closeWriter();
    };


    public IndexReader reader () {
	if (!readerOpen)
	    this.openReader();

	return this.reader;
    };

    public IndexSearcher searcher () {
	if (this.searcher == null) {
	    this.searcher = new IndexSearcher(this.reader());
	};
	return this.searcher;
    };

    public void closeWriter () throws IOException {
	this.writer.close();
    };


    public void closeReader () throws IOException {
	if (readerOpen) {
	    this.reader.close();
	    readerOpen = false;
	};
    };


    public void openReader () {
	try {
	    this.reader = DirectoryReader.open(this.directory);
	    readerOpen = true;
	    if (this.searcher != null) {
		this.searcher = new IndexSearcher(reader);
	    };
	}

	catch (IOException e) {
	    log.warn( e.getLocalizedMessage() );
	};
    };


    public FieldDocument addDoc (FieldDocument fd) throws IOException {
	
	// Add document to writer
	this.writer.addDocument( fd.doc );
	if (++commitCounter > autoCommit) {
	    this.commit();
	    commitCounter = 0;
	};
	return fd;
    };

    // Add with file!
    public FieldDocument addDoc (String json) throws IOException {
	FieldDocument fd = this.mapper.readValue(json, FieldDocument.class);
	return this.addDoc(fd);
    };

    public FieldDocument addDoc (File json) throws IOException {
      FieldDocument fd = this.mapper.readValue(json, FieldDocument.class);
      return this.addDoc(fd);
    };

    public FieldDocument addDocFile(String json) throws IOException {
      return this.addDocFile(json, false);
    };

    public FieldDocument addDocFile(String json, boolean gzip) {
      try {
	if (gzip) {
	  FieldDocument fd = this.mapper.readValue(new GZIPInputStream(new FileInputStream(json)), FieldDocument.class);
	  return this.addDoc(fd);
	};
	return this.addDoc(json);
      }
      catch (IOException e) {
	log.error("File json not found");
      };
      return (FieldDocument) null;
    };

    public void commit () throws IOException {
	if (commitCounter > 0) {
	    this.writer.commit();
	    commitCounter = 0;
	    this.closeReader();
	};
    };


    // Get autoCommit valiue
    public int autoCommit () {
	return this.autoCommit;
    };


    // Set autoCommit value
    public void autoCommit (int number) {
	this.autoCommit = number;
    };


    // Search for meta information in term vectors
    private long numberOfAtomic (Bits docvec,
				 AtomicReaderContext atomic,
				 Term term) throws IOException {

	// This reimplements docsAndPositionsEnum with payloads
	final Terms terms = atomic.reader().fields().terms(term.field());

	// No terms were found
	if (terms != null) {
	    // Todo: Maybe reuse a termsEnum!
	    final TermsEnum termsEnum = terms.iterator(null);

	    // Set the positioon in the iterator to the term that is seeked
	    if (termsEnum.seekExact(term.bytes(), true)) {

		// Start an iterator to fetch all payloads of the term
		DocsAndPositionsEnum docs = termsEnum.docsAndPositions(
		    docvec,
		    null,
		    DocsAndPositionsEnum.FLAG_PAYLOADS
		);

		// Iterator is empty
		// TODO: Maybe this is an error ...
		if (docs.docID() == DocsAndPositionsEnum.NO_MORE_DOCS) {
		    return 0;
		};

		// Init some variables for data copying
		long occurrences = 0;
		BytesRef payload;

		// Init nextDoc()
		while (docs.nextDoc() != DocsAndPositionsEnum.NO_MORE_DOCS) {

		    // Initialize (go to first term)
		    docs.nextPosition();

		    // Copy payload with the offset of the BytesRef
		    payload = docs.getPayload();
		    System.arraycopy(payload.bytes, payload.offset, pl, 0, 4);

		    // Add payload as integer
		    occurrences += bb.wrap(pl).getInt();
		};

		// Return the sum of all occurrences
		return occurrences;
	    };
	};

	// Nothing found
	return 0;
    };


    /**
     * Search for the number of occurrences of different types,
     * e.g. "documents", "sentences" etc.
     *
     * @param field The field containing the textual data and the annotations.
     * @param type The type of meta information, e.g. "documents" or "sentences".
     */
    public long numberOf (KorapCollection collection, String field, String type) throws IOException {
	// Short cut for documents
	if (type.equals("documents")) {
	    if (collection.getCount() <= 0) {
		return (long) this.reader().numDocs();
	    };

	    long docCount = 0;
	    // System.err.println("CHECK");
	    int i = 1;
	    for (AtomicReaderContext atomic : this.reader().leaves()) {
		// System.err.println("READER" + i + "a-" + docCount);
		docCount += collection.bits(atomic).cardinality();
		// System.err.println("READER" + i + "b-" + docCount);
		i++;
	    };
	    return docCount;
	};
    
	// Create search term
	Term term = new Term(field, "-:" + type);
	// System.err.println(">> Search for -:" + type + " in " + field);

	long occurrences = 0;
	try {
	    // Iterate over all atomic readers and collect occurrences
	    for (AtomicReaderContext atomic : this.reader().leaves()) {
		occurrences += this.numberOfAtomic(
		   collection.bits(atomic),
		    atomic,
		    term
		);
	    };
	}

	// Something went wrong
	catch (IOException e) {
	    log.warn( e.getLocalizedMessage() );
	};

	return occurrences;
    };

    public long numberOf (String field, String type) throws IOException {
	return this.numberOf(new KorapCollection(this), field, type);
    };


    /**
     * Search for the number of occurrences of different types,
     * e.g. "documents", "sentences" etc., in the base foundry.
     *
     * @param type The type of meta information, e.g. "documents" or "sentences".
     *
     * @see #numberOf(String, String)
     */
    public long numberOf (String type) throws IOException {
	return this.numberOf("tokens", type);
    };


    /**
     * Search for the number of occurrences of different types,
     * e.g. "documents", "sentences" etc., in a specific set of documents.
     *
     * @param docvec The document vector for filtering the search space.
     * @param field The field containing the textual data and the annotations.
     * @param type The type of meta information, e.g. "documents" or "sentences".
     *
     * @see #numberOf(String, String)
     */
    public long numberOf (Bits docvec, String field, String type) throws IOException {

	// Shortcut for documents
	if (type.equals("documents")) {
	    OpenBitSet os = (OpenBitSet) docvec;
	    return os.cardinality();
	};
    
	Term term = new Term(field, "-:" + type);

	int occurrences = 0;
	try {
	    for (AtomicReaderContext atomic : this.reader().leaves()) {
		occurrences += this.numberOfAtomic(docvec, atomic, term);
	    };
	}
	catch (IOException e) {
	    log.warn( e.getLocalizedMessage() );
	};

	return occurrences;
    };

    @Deprecated
    public long countDocuments () throws IOException {
	log.warn("countDocuments() is DEPRECATED in favor of numberOf(\"documents\")!");
	return this.numberOf("documents");
    };


    @Deprecated
    public long countAllTokens () throws IOException {
	log.warn("countAllTokens() is DEPRECATED in favor of numberOf(\"tokens\")!");
	return this.numberOf("tokens");
    };


    public KorapMatch getMatch (String id) {
	return this.getMatchInfo(id, "tokens", false, null, null, false, true);
    };

    public KorapMatch getMatchInfo (String id,
				    String field,
				    String foundry,
				    String layer,
				    boolean includeSpans,
				    boolean includeHighlights) {
	return this.getMatchInfo(id, field, true, foundry, layer, includeSpans, includeHighlights);
    };

    /**
     * Get a match.
     * BE AWARE - THIS IS STILL A PLAYGROUND!
     */
    /*
      KorapInfo is associated with a KorapMatch and has an array with all informations
      per position in the match.
    */
    public KorapMatch getMatchInfo (String idString,
				    String field,
				    boolean info,
				    String foundry,
				    String layer,
				    boolean includeSpans,
				    boolean includeHighlights) {

	KorapMatch match = new KorapMatch(idString, includeHighlights);

	// Create a filter based on the corpusID and the docID
	BooleanQuery bool = new BooleanQuery();
	bool.add(new TermQuery(new Term("ID",       match.getDocID())),    BooleanClause.Occur.MUST);
	bool.add(new TermQuery(new Term("corpusID", match.getCorpusID())), BooleanClause.Occur.MUST);
	Filter filter = (Filter) new QueryWrapperFilter(bool);

	CompiledAutomaton fst = null;

	if (info) {
	    /* Create an automaton for prefixed terms of interest.
	     * You can define the necessary foundry, the necessary layer,
	     * in case the foundry is given, and if span annotations
	     * are of interest.
	     */
	    StringBuffer regex = new StringBuffer();

	    // Todo: Only support one direction!
	    if (includeSpans)
		regex.append("((\">\"|\"<\"\">\"?)\":\")?");
	    if (foundry != null) {
		regex.append(foundry).append('/');
		if (layer != null)
		    regex.append(layer).append(":");
	    }
	    else if (includeSpans) {
		regex.append("([^-is]|[-is][^:])");
	    }
	    else {
		regex.append("([^-is<>]|([-is>][^:])|<[^:>])");
	    };
	    regex.append("(.){1,}|_[0-9]+");


	    log.trace("The final regexString is {}", regex.toString());
	    RegExp regexObj = new RegExp(regex.toString(), RegExp.COMPLEMENT);
	    fst = new CompiledAutomaton(regexObj.toAutomaton());
	    log.trace("The final regexObj is {}", regexObj.toString());
	};


	try {
	    // Iterate over all atomic indices and find the matching document
	    for (AtomicReaderContext atomic : this.reader().leaves()) {

		// Retrieve the single document of interest
		DocIdSet filterSet = filter.getDocIdSet(
		    atomic,
		    atomic.reader().getLiveDocs()
		);

		// Create a bitset for the correct document
		Bits bitset = filterSet.bits();

		DocIdSetIterator filterIterator = filterSet.iterator();

		// No document found
		if (filterIterator == null)
		    continue;

		// Go to the matching doc - and remember its ID
		int localDocID = filterIterator.nextDoc();

		if (localDocID == DocIdSetIterator.NO_MORE_DOCS)
		    continue;

		// We've found the correct document! Hurray!
		log.trace("We've found a matching document");
		HashSet<String> fieldsToLoadLocal = new HashSet<>(fieldsToLoad);
		fieldsToLoadLocal.add(field);

		// Get terms from the document
		Terms docTerms = atomic.reader().getTermVector(localDocID, field);

		// Load the necessary fields of the document
		Document doc = atomic.reader().document(localDocID, fieldsToLoadLocal);

		// Put some more information to the match
		PositionsToOffset pto = new PositionsToOffset(atomic, field);
		match.setPositionsToOffset(pto);
		match.setLocalDocID(localDocID);
		match.populateDocument(doc, field, fieldsToLoadLocal);

		log.trace("The document has the id '{}'", match.getDocID());

		if (!info) break;

		// Limit the terms to all the terms of interest
		TermsEnum termsEnum = docTerms.intersect(fst, null);

		DocsAndPositionsEnum docs = null;

		// List of terms to populate
		SpanInfo termList = new SpanInfo(pto, localDocID);

		// Iterate over all terms in the document
		while (termsEnum.next() != null) {

		    // Get the positions and payloads of the term in the document
		    // The bitvector may look different (don't know why)
		    // and so the local ID may differ.
		    // That's why the requesting bitset is null.
		    docs = termsEnum.docsAndPositions(
		        null,
			docs,
			DocsAndPositionsEnum.FLAG_PAYLOADS
		    );

		    // Init document iterator
		    docs.nextDoc();

		    // Should never happen ... but hell.
		    if (docs.docID() == DocIdSetIterator.NO_MORE_DOCS)
			continue;

		    // How often does this term occur in the document?
		    int termOccurrences = docs.freq();

		    // log.trace("I found {} documents with this term", termOccurrences);

		    // String representation of the term
		    String termString = termsEnum.term().utf8ToString();

		    // Iterate over all occurrences
		    for (int i = 0; i < termOccurrences; i++) {

			// Init positions and get the current
			int pos = docs.nextPosition();

			// Check, if the position of the term is in the interesting area

			// log.trace("Check position!");

			if (pos >= match.getStartPos() && pos < match.getEndPos()) {

			    log.trace(
			        ">> {}: {}-{}-{}",
				termString, 
				docs.freq(),
				pos,
				docs.getPayload()
			    );

			    BytesRef payload = docs.getPayload();

			    // Copy the payload
			    bbTerm.clear();
			    if (payload != null) {
				bbTerm.put(
				    payload.bytes,
				    payload.offset,
				    payload.length
				);
			    };
			    TermInfo ti = new TermInfo(termString, pos, bbTerm).analyze();
			    if (ti.getEndPos() < match.getEndPos()) {
				log.trace("Add {}", ti.toString());
				termList.add(ti);
			    };
			};
		    };
		};

		// Add annotations based on the retrieved infos
		for (TermInfo t : termList.getTerms()) {
		    log.trace("Add term {}/{}:{} to {}({})-{}({})",
			      t.getFoundry(),
			      t.getLayer(),
			      t.getValue(),
			      t.getStartChar(),
			      t.getStartPos(),
			      t.getEndChar(),
			      t.getEndPos());

		    if (t.getType() == "term" || t.getType() == "span")
			match.addAnnotation(t.getStartPos(), t.getEndPos(), t.getAnnotation());
		    else if (t.getType() == "relSrc")
			match.addRelation(t.getStartPos(), t.getEndPos(), t.getAnnotation());
		};

		break;
	    };
	}
	catch (IOException e) {
	    log.warn(e.getLocalizedMessage());
	    match.setError(e.getLocalizedMessage());
	};

	return match;
    };


    /**
     * Search in the index.
     */
    public KorapResult search (SpanQuery query) {
	return this.search(new KorapCollection(this), new KorapSearch(query));
    };

    public KorapResult search (SpanQuery query, short count) {
	return this.search(
	    new KorapCollection(this),
	    new KorapSearch(query).setCount(count)
        );
    };

    public KorapResult search (SpanQuery query,
			       int startIndex,
			       short count,
			       boolean leftTokenContext,
			       short leftContext,
			       boolean rightTokenContext,
			       short rightContext) {
	return this.search(
	    new KorapCollection(this),
	    query,
	    startIndex,
	    count,
	    leftTokenContext,
	    leftContext,
	    rightTokenContext,
	    rightContext
        );
    };

    public KorapResult search (KorapSearch ks) {
	// TODO: This might leak
	return this.search(new KorapCollection(this), ks);
    };

    public KorapResult search (KorapCollection collection,
			       SpanQuery query,
			       int startIndex,
			       short count,
			       boolean leftTokenContext,
			       short leftContext,
			       boolean rightTokenContext,
			       short rightContext) {
	KorapSearch ks = new KorapSearch(query);
	ks.setStartIndex(startIndex).setCount(count);
	ks.leftContext.setToken(leftTokenContext).setLength(leftContext);
	ks.rightContext.setToken(rightTokenContext).setLength(rightContext);
	return this.search(collection, ks);
    };


    public KorapResult search (KorapCollection collection, KorapSearch ks) {
	log.trace("Start search");

	this.termContexts = new HashMap<Term, TermContext>();

	SpanQuery query = ks.getQuery();

	// Get the field of textual data and annotations
	String field = query.getField();

	// Todo: Make kr subclassing ks - so ks has a method for a new KorapResult!
	KorapResult kr = new KorapResult(
	    query.toString(),
	    ks.getStartIndex(),
	    ks.getCount(),
	    ks.leftContext.isToken(),
	    ks.leftContext.getLength(),
	    ks.rightContext.isToken(),
	    ks.rightContext.getLength()
	);

	HashSet<String> fieldsToLoadLocal = new HashSet<>(fieldsToLoad);
	fieldsToLoadLocal.add(field);

	int i = 0;
	long t1 = 0, t2 = 0;
	int startIndex = kr.getStartIndex();
	int count = kr.getItemsPerPage();
	int hits = kr.itemsPerPage() + startIndex;
	int limit = ks.getLimit();
	boolean cutoff = ks.doCutOff();

	if (limit > 0) {
	    if (hits > limit)
		hits = limit;

	    if (limit < startIndex)
		return kr;
	};

	ArrayList<KorapMatch> atomicMatches = new ArrayList<KorapMatch>(kr.itemsPerPage());

	try {

	    // Rewrite query (for regex and wildcard queries)
	    for (Query rewrittenQuery = query.rewrite(this.reader());
                 rewrittenQuery != (Query) query;
                 rewrittenQuery = query.rewrite(this.reader())) {
		query = (SpanQuery) rewrittenQuery;
	    };

	    for (AtomicReaderContext atomic : this.reader().leaves()) {

		// Use OpenBitSet;
		Bits bitset = collection.bits(atomic);

		PositionsToOffset pto = new PositionsToOffset(atomic, field);

		// Spans spans = NearSpansOrdered();
		Spans spans = query.getSpans(atomic, (Bits) bitset, termContexts);

		IndexReader lreader = atomic.reader();

		// TODO: Get document information from Cache!

		// See: http://www.ibm.com/developerworks/java/library/j-benchmark1/index.html
		t1 = System.nanoTime();

		for (; i < hits; i++) {

		    log.trace("Match Nr {}/{}", i, count);

		    // There are no more spans to find
		    if (spans.next() != true)
			break;
		   
		    // The next matches are not yet part of the result
		    if (startIndex > i)
			continue;

		    int localDocID = spans.doc();
		    int docID = atomic.docBase + localDocID;

		    // Document doc = lreader.document(docID, fieldsToLoadLocal);


		    // Do not load all of this, in case the doc is the same!
		    Document doc = lreader.document(localDocID, fieldsToLoadLocal);
		    KorapMatch match = kr.addMatch(
		        pto,
			localDocID,
			spans.start(),
			spans.end()
		    ); // new KorapMatch();

		    if (spans.isPayloadAvailable()) {

			// TODO: Here are offsets and highlight offsets!
			// <> payloads have 12 bytes (iii) or 8!?
			// highlightoffsets have 11 bytes (iis)!

			/*
			int[] offsets = getOffsetsFromPayload(spans.getPayload());
			match.startOffset(offsets[0]);
			match.startOffset(offsets[1]);
			*/

			try {
			    ByteBuffer bb = ByteBuffer.allocate(10);
			    for (byte[] b : spans.getPayload()) {

				log.trace("Found a payload!!! with length {}", b.length);

				// Todo element searches!

				// Highlights!
				if (b.length == 9) {
				    bb.put(b);
				    bb.rewind();

				    int start = bb.getInt();
				    int end = bb.getInt() -1;
				    byte number = bb.get();

				    log.trace("Have a payload: {}-{}", start, end);
				    match.addHighlight(start, end, number);
				}

				// Element payload for match!
				// This MAY BE the correct match
				else if (b.length == 8) {
				    bb.put(b);
				    bb.rewind();

				    if (match.potentialStartPosChar == -1) {
					match.potentialStartPosChar = bb.getInt(0);
				    }
				    else {
					if (bb.getInt(0) < match.potentialStartPosChar)
					match.potentialStartPosChar = bb.getInt(0);
				    };

				    if (bb.getInt(4) > match.potentialEndPosChar)
					match.potentialEndPosChar = bb.getInt(4);

				    log.trace("Element payload from {} to {}",
					      match.potentialStartPosChar,
					      match.potentialEndPosChar);
				}

				else if (b.length == 4) {
				    bb.put(b);
				    bb.rewind();
				    log.debug("Unknown[4]: {}", bb.getInt());
				};

				bb.clear();
			    };
			}

			catch (Exception e) {
			    log.error(e.getMessage());
			}

			// match.payload(spans.getPayload());
		    };


		    match.internalDocID = docID;
		    match.populateDocument(doc, field, fieldsToLoadLocal);

		    log.trace("I've got a match in {} of {}", match.getDocID(), count);

		    atomicMatches.add(match);
		};

		// Benchmark till now
		if (i >= kr.itemsPerPage() &&
		    kr.getBenchmarkSearchResults().length() == 0) {
		    t2 = System.nanoTime();
		    kr.setBenchmarkSearchResults(t1, t2);
		};

		// Can be disabled TEMPORARILY
		while (!cutoff && spans.next()) {
		    if (limit > 0 && i <= limit)
			break;
		    i++;
		};
		atomicMatches.clear();
	    };

	    t1 = System.nanoTime();
	    kr.setBenchmarkHitCounter(t2, t1);
	    if (kr.getBenchmarkSearchResults().length() == 0) {
		kr.setBenchmarkSearchResults(t2, t1);
	    };

	    kr.setTotalResults(cutoff ? -1 : i);
	}
	catch (IOException e) {
	    kr.setError("There was an IO error");
	    log.warn( e.getLocalizedMessage() );
	};

	return kr;
    };
};
