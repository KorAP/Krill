package de.ids_mannheim.korap;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.net.URL;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;
import java.io.FileInputStream;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
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
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.DocIdSetIterator;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.lucene.util.Version;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.OpenBitSet;
import org.apache.lucene.search.DocIdSet;

import de.ids_mannheim.korap.index.FieldDocument;
import de.ids_mannheim.korap.KorapResult;
import de.ids_mannheim.korap.KorapMatch;
import de.ids_mannheim.korap.KorapCollection;
import de.ids_mannheim.korap.KorapSearch;
import de.ids_mannheim.korap.index.PositionsToOffset;
import de.ids_mannheim.korap.document.KorapPrimaryData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/*
  Todo: Use FieldCache!


  http://invertedindex.blogspot.co.il/2009/04/lucene-dociduid-mapping-and-payload.html
  see korap/search.java -> retrieveTokens

  Support multiple indices.

  Support frequency search with regular expressions, so multiple bookkeeping:
  c<:VVFIN:ging:gehen:past::
  c>:VVFIN:gnig:neheg:past::
  -> search for frequencies of VVFIN/gehen
  -> c:VVFIN:[^:]*?:gehen:past:...
*/

/**
 * @author Nils Diewald
 * 
 * KorapIndex implements a simple API for searching in and writing to a
 * Lucene index and equesting several information but the index's nature.
 */
public class KorapIndex {
    private Directory directory;

    // Temp:
    public IndexReader reader;

    private IndexWriter writer;
    private IndexSearcher searcher;
    private boolean readerOpen = false;
    private int commitCounter = 0;
    private int autoCommit = 500;
    private HashMap termContexts;
    private ObjectMapper mapper = new ObjectMapper();


    private static ByteBuffer bb = ByteBuffer.allocate(4);
    private static ByteBuffer bbOffset = ByteBuffer.allocate(8);


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

	fieldsToLoad = new HashSet<String>();
	fieldsToLoad.add("author");
	fieldsToLoad.add("ID");
	fieldsToLoad.add("title");
	fieldsToLoad.add("subTitle");
	fieldsToLoad.add("textClass");
	fieldsToLoad.add("pubPlace");
	fieldsToLoad.add("pubDate");
	fieldsToLoad.add("corpusID");
	fieldsToLoad.add("foundries");
	fieldsToLoad.add("tokenization");
	// don't load tokenization

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
		if (docs.docID() == DocsAndPositionsEnum.NO_MORE_DOCS) {
		    return 0;
		};

		// Init some variables for data copying
		long occurrences = 0;
		BytesRef payload;

		// Init nextDoc()
		while (docs.nextDoc() != DocsAndPositionsEnum.NO_MORE_DOCS) {

		    // Go to first term (initialization phase)
// TODO: THIS MAY BE WRONG!
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
     * @param foundry The foundry to search in.
     * @param type The type of meta information, e.g. "documents" or "sentences".
     */
    public long numberOf (KorapCollection collection, String foundry, String type) throws IOException {
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
	Term term = new Term(foundry, "-:" + type);
	// System.err.println(">> Search for -:" + type + " in " + foundry);

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

    public long numberOf (String foundry, String type) throws IOException {
	return this.numberOf(new KorapCollection(this), foundry, type);
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
     * @param foundry The foundry to search in.
     * @param type The type of meta information, e.g. "documents" or "sentences".
     *
     * @see #numberOf(String, String)
     */
    public long numberOf (Bits docvec, String foundry, String type) throws IOException {

	// Shortcut for documents
	if (type.equals("documents")) {
	    OpenBitSet os = (OpenBitSet) docvec;
	    return os.cardinality();
	};
    
	Term term = new Term(foundry, "-:" + type);

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


    /**
     * search
     */
    public KorapResult search (SpanQuery query) {
	return this.search((Bits) null, query, 0, (short) 10, true, (short) 6, true, (short) 6);
    };

    public KorapResult search (SpanQuery query,
			       short count) {
	return this.search((Bits) null, query, 0, count, true, (short) 6, true, (short) 6);
    };

    public KorapResult search (Bits bitset,
			       SpanQuery query,
			       short count) {
	return this.search((Bits) bitset, query, 0, count, true, (short) 6, true, (short) 6);
    };

    public KorapResult search (SpanQuery query,
			       int startIndex,
			       short count,
			       boolean leftTokenContext,
			       short leftContext,
			       boolean rightTokenContext,
			       short rightContext) {
	return this.search((Bits) null, query, startIndex, count,
			   leftTokenContext, leftContext, rightTokenContext, rightContext);
    };

    // This is just a fallback! Delete!
    @Deprecated
    public KorapResult search (Bits bitset,
			       SpanQuery query,
			       int startIndex,
			       short count,
			       boolean leftTokenContext,
			       short leftContext,
			       boolean rightTokenContext,
			       short rightContext) {
	// TODO: This might leak as hell!!!
	return this.search(new KorapCollection(this), query, startIndex, count, leftTokenContext, leftContext, rightTokenContext, rightContext);
    };

    public KorapResult search (KorapCollection kc, KorapSearch ks) {
	return this.search(kc,
			   ks.getQuery(),
			   ks.getStartIndex(),
			   ks.getCount(),
			   ks.leftContext.isToken(),
			   ks.leftContext.getLength(),
			   ks.rightContext.isToken(),
			   ks.rightContext.getLength()
			   );
    };

    public KorapResult search (KorapSearch ks) {
	return this.search(new KorapCollection(this), ks);
    };



    // old: Bits bitset
    public KorapResult search (KorapCollection collection,
			       SpanQuery query,
			       int startIndex,
			       short count,
			       boolean leftTokenContext,
			       short leftContext,
			       boolean rightTokenContext,
			       short rightContext) {


	log.trace("Start search");

	this.termContexts = new HashMap<Term, TermContext>();
	String foundry = query.getField();

	KorapResult kr = new KorapResult(
	    query.toString(),
	    startIndex,
	    count,
	    leftTokenContext,
	    leftContext,
	    rightTokenContext,
	    rightContext
        );

	HashSet<String> fieldsToLoadLocal = new HashSet<>(fieldsToLoad);
	fieldsToLoadLocal.add(foundry);

	try {
	    int i = 0;
	    long t1 = 0;
	    long t2 = 0;

	    int hits = kr.itemsPerPage() + startIndex;

	    ArrayList<KorapMatch> atomicMatches = new ArrayList<KorapMatch>(kr.itemsPerPage());

	    for (AtomicReaderContext atomic : this.reader().leaves()) {

		log.trace("NUKULAR!");

		// Use OpenBitSet;
		Bits bitset = collection.bits(atomic);

		PositionsToOffset pto = new PositionsToOffset(atomic, foundry);

		// Spans spans = NearSpansOrdered();
		Spans spans = query.getSpans(atomic, (Bits) bitset, termContexts);

		IndexReader lreader = atomic.reader();

		// TODO: Get document information from Cache!

		// See: http://www.ibm.com/developerworks/java/library/j-benchmark1/index.html
		t1 = System.nanoTime();

		for (; i < hits; i++) {

		    log.trace("Match Nr {}/{}", i, count);

		    if (spans.next() != true) {
			break;
		    };
		   
		    if (startIndex > i)
			continue;

		    int localDocID = spans.doc();
		    int docID = atomic.docBase + localDocID;

		    // Document doc = lreader.document(docID, fieldsToLoadLocal);
		    Document doc = lreader.document(localDocID, fieldsToLoadLocal);
		    KorapMatch match = new KorapMatch();

		    match.startPos = spans.start();
		    match.endPos = spans.end();
		    match.localDocID = localDocID;

		    pto.add(localDocID, match.startPos);
		    pto.add(localDocID, match.endPos - 1);

		    match.leftContext = leftContext;
		    match.rightContext = rightContext;

		    match.leftTokenContext = leftTokenContext;
		    match.rightTokenContext = rightTokenContext;

		    // Add pos for context
		    if (leftTokenContext) {
			pto.add(localDocID, match.startPos - leftContext);
		    };

		    // Add pos for context
		    if (rightTokenContext) {
			pto.add(localDocID, match.endPos + rightContext - 1);
		    };

		    if (spans.isPayloadAvailable()) {

			// TODO: Here are offsets and highlight offsets!
			// <> payloads have 12 bytes (iii) or 8!?
			// highlightoffsets have 10 bytes (iis)!

			// 11 bytes!!!

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

				    // Add this for offset search
				    pto.add(localDocID, start);
				    pto.add(localDocID, end);

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
			}

			// match.payload(spans.getPayload());
		    };


		    match.internalDocID = docID;
		    // match.foundry = foundry; // This is "tokens" or "base" or so

		    match.setAuthor(doc.get("author"));
		    match.setTextClass(doc.get("textClass"));
		    match.setID(doc.get("ID"));
		    match.setTitle(doc.get("title"));
		    match.setSubTitle(doc.get("subTitle"));
		    match.setPubPlace(doc.get("pubPlace"));
		    match.setCorpusID(doc.get("corpusID"));
		    match.setPubDate(doc.get("pubDate"));

		    log.trace("I've got a match in {} of {}", match.getID(), count);

		    // Temporary (later meta fields in term vector)
		    match.setFoundries(doc.get("foundries"));
		    match.setTokenization(doc.get("tokenization"));

		    match.setPrimaryData(
		      new KorapPrimaryData(doc.get(foundry))
		    );
		    atomicMatches.add(match);
		    kr.add(match);
		};

		// Benchmark till now
		if (i >= kr.itemsPerPage() &&
		    kr.getBenchmarkSearchResults().length() == 0) {
		    t2 = System.nanoTime();
		    kr.setBenchmarkSearchResults(t1, t2);
		};

		while (spans.next()) {
		    i++;
		};

		for (KorapMatch km : atomicMatches) {
		    km.processHighlight(pto);
		};
		atomicMatches.clear();
	    };

	    t1 = System.nanoTime();
	    kr.setBenchmarkHitCounter(t2, t1);
	    if (kr.getBenchmarkSearchResults().length() == 0) {
		kr.setBenchmarkSearchResults(t2, t1);
	    };
	    kr.setTotalResults(i);


	    // if (spans.isPayloadAvailable()) {
	    // for (byte[] payload : spans.getPayload()) {
	    // // retrieve payload for current matching span
	    // payloadString.append(new String(payload));
	    // payloadString.append(" | ");
	    // };
	    // };
	}
	catch (IOException e) {
	    kr.setError("There was an IO error");
	    log.warn( e.getLocalizedMessage() );
	};

	return kr;
    };


	/*

    public void getFoundryStatistics {
- provides statistical information:
  - which documents have which foundries
    - Collect all Bitvectors of each foundry and make the intersections afterwards
  - ???
    };


    public KorapResult search (Bits bits, KorapQuery query) {
	//	this.search(bits, query);
    };

    // countAllTokens
    public int getNumberOfTokens (String corpus) throws IOException {
	return this.getNumberOf("token", "base", corpus);
    };


    // retrieveTokens(docname, startOffset, endOffset, layer);


    /*


    // todo mit pagesize und offset


    };
*/

};
