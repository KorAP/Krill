package de.ids_mannheim.korap;

// Java classes
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.regex.Pattern;
import java.io.*;
import java.net.URL;
import java.nio.ByteBuffer;

// Lucene classes
import org.apache.lucene.search.*;
import org.apache.lucene.search.spans.*;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.*;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.util.*;
import org.apache.lucene.util.automaton.*;

// JSON helper class
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;

// KorAP classes
import de.ids_mannheim.korap.*;
import de.ids_mannheim.korap.index.*;
import de.ids_mannheim.korap.query.SpanElementQuery;
import de.ids_mannheim.korap.util.QueryException;

// Log4j Logger classes
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
  TODO: Add word count as a meta data field!
  TODO: Validate document import!
  TODO: DON'T STORE THE TEXT IN THE TOKENS FIELD!
        It has only to be lifted for match views!!!
  TODO: Support layer for specific foundries (IMPORTANT)
  TODO: Use FieldCache!
  TODO: Reuse the indexreader everywhere - it should be threadsafe!

  http://invertedindex.blogspot.co.il/2009/04/lucene-dociduid-mapping-and-payload.html
  see korap/search.java -> retrieveTokens

  Todo: Support document removal!
  Todo: Support document update! // it's now part of IndexWriter

  Support a callback for interrupts (to stop the searching)!

  Support multiple indices.

  Support frequency search with regular expressions, so multiple bookkeeping:
  c<:VVFIN:ging:gehen:past::
  c>:VVFIN:gnig:neheg:past::
  -> search for frequencies of VVFIN/gehen
  -> c:VVFIN:[^:]*?:gehen:past:...
*/

/**
 *
 * KorapIndex implements a simple API for searching in and writing to a
 * Lucene index and requesting several information about the index' nature.
 * <br />
 *
 * <pre>
 *   KorapIndex ki = new KorapIndex(
 *       new MMapDirectory(new File("/myindex"))
 *   );
 * </pre>
 *
 * Properties can be stored in a properies file called 'index.properties'.
 * Relevant properties are <code>lucene.version</code> and
 * <code>lucene.name</code>.
 *
 * @author diewald
 */
public class KorapIndex {

    // Todo: Use configuration
    // Last line of defense for simple DOS attacks!
    private int maxTermRelations = 100;
    private int autoCommit = 500;

    private Directory directory;

    // Temp:
    public IndexReader reader;

    private IndexWriter writer;
    private IndexWriterConfig config;
    private IndexSearcher searcher;
    private boolean readerOpen = false;

    // The commit counter is only there for
    // counting unstaged changes per thread (for bulk insertions)
    // It does not represent real unstaged documents.
    private int commitCounter = 0;
    private HashMap termContexts;
    private ObjectMapper mapper = new ObjectMapper();
    private String version, name;

    private byte[] pl = new byte[4];
    private static ByteBuffer
        bb       = ByteBuffer.allocate(4),
        bbOffset = ByteBuffer.allocate(8),
        bbTerm   = ByteBuffer.allocate(16);

    // Logger
    private final static Logger log = LoggerFactory.getLogger(KorapIndex.class);

    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;

    {
        Properties prop = new Properties();
        URL file = getClass().getClassLoader().getResource("index.properties");

        // File found
        if (file != null) {
            String f = file.getFile();
            // Read property file
            try {
                InputStream fr = new FileInputStream(f);
                prop.load(fr);
                this.version = prop.getProperty("lucene.version");
                this.name    = prop.getProperty("lucene.name");
            }

            // Unable to read property file
            catch (FileNotFoundException e) {
                log.warn(e.getLocalizedMessage());
            };
        };
    };


    /**
     * Constructs a new KorapIndex in-memory.
     *
     * @throws IOException
     */
    public KorapIndex () throws IOException {
        this((Directory) new RAMDirectory());
    };


    /**
     * Constructs a new KorapIndex bound to a persistant index.
     *
     * @param index Path to an {@link FSDirectory} index
     * @throws IOException
     */
    public KorapIndex (String index) throws IOException {
        this(FSDirectory.open(new File( index )));
    };


    /**
     * Constructs a new KorapIndex bound to a persistant index.
     *
     * @param directory A {@link Directory} pointing to an index
     * @throws IOException
     */
    public KorapIndex (Directory directory) throws IOException {
        this.directory = directory;

        // TODO: Shouldn't be here
        // Add analyzers
        Map<String,Analyzer> analyzerPerField = new HashMap<String,Analyzer>();
        analyzerPerField.put("textClass", new WhitespaceAnalyzer(Version.LUCENE_CURRENT));
        analyzerPerField.put("foundries", new WhitespaceAnalyzer(Version.LUCENE_CURRENT));
        PerFieldAnalyzerWrapper analyzer = new PerFieldAnalyzerWrapper(
            new StandardAnalyzer(Version.LUCENE_CURRENT),
            analyzerPerField
        );

        // Create configuration with base analyzer
        this.config = new IndexWriterConfig(Version.LUCENE_CURRENT, analyzer);
    };


    /**
     * Get the version number of the index.
     *
     * @return A string containing the version number.
     */    
    public String getVersion () {
        return this.version;
    };


    /**
     * Get the name of the index.
     *
     * @return A string containing the name of the index.
     */ 
    public String getName () {
        return this.name;
    };


    /**
     * Close the connections of the index reader and the writer.
     * @throws IOException
     */ 
    public void close () throws IOException {
        this.closeReader();
        this.closeWriter();
    };


    // Get index reader object
    public IndexReader reader () {
        if (!readerOpen)
            this.openReader();
        // Todo: Maybe use DirectoryReader.openIfChanged(DirectoryReader)
        
        return this.reader;
    };
    

    // Get index searcher object
    public IndexSearcher searcher () {
        if (this.searcher == null) {
            this.searcher = new IndexSearcher(this.reader());
        };
        return this.searcher;
    };


    // Close index writer
    public void closeWriter () throws IOException {
        if (this.writer != null)
            this.writer.close();
    };


    // Open index reader
    public void openReader () {
        try {

            // open reader
            this.reader = DirectoryReader.open(this.directory);
            readerOpen = true;
            if (this.searcher != null)
                this.searcher = new IndexSearcher(reader);
        }

        // Failed to open reader
        catch (IOException e) {
            //e.printStackTrace();
            log.warn( e.getLocalizedMessage() );
        };
    };


    // Close index reader
    public void closeReader () throws IOException {
        if (readerOpen) {
            this.reader.close();
            readerOpen = false;
        };
    };

    /*
     * Some of these addDoc methods will probably be DEPRECATED,
     * as they were added while the API changed slowly.
     */

    // Add document to index as FieldDocument
    public FieldDocument addDoc (FieldDocument fd) {
        try {
            // Open writer if not already opened
            if (this.writer == null)
                this.writer = new IndexWriter(this.directory, this.config);

            // Add document to writer
            this.writer.addDocument( fd.doc );
            if (++commitCounter > autoCommit) {
                this.commit();
                commitCounter = 0;
            };
        }

        // Failed to add document
        catch (IOException e) {
            log.error("File json not found");
        };
        return fd;
    };

    // Add document to index as JSON object with a unique ID
    public FieldDocument addDoc (int uid, String json) throws IOException {
        FieldDocument fd = this.mapper.readValue(json, FieldDocument.class);
        fd.setUID(uid);
        return this.addDoc(fd);
    };


    // Add document to index as JSON object
    public FieldDocument addDoc (String json) throws IOException {
        FieldDocument fd = this.mapper.readValue(json, FieldDocument.class);
        return this.addDoc(fd);
    };


    // Add document to index as JSON file
    public FieldDocument addDoc (File json) {
        try {
            FieldDocument fd = this.mapper.readValue(json, FieldDocument.class);
            return this.addDoc(fd);
        }
        catch (IOException e) {
            log.error("File json not parseable");	    
        };
        return (FieldDocument) null;
    };


    // Add document to index as JSON file
    public FieldDocument addDocFile(String json) {
        return this.addDocFile(json, false);
    };


    private FieldDocument _addDocfromFile (String json, boolean gzip) {
        try {
            if (gzip) {

                // Create json field document
                FieldDocument fd = this.mapper.readValue(
                    new GZIPInputStream(new FileInputStream(json)),
                    FieldDocument.class
                );
                return fd;
            };
            return this.mapper.readValue(json, FieldDocument.class);
        }

        // Fail to add json object
        catch (IOException e) {
            log.error("File json not found");
        };
        return (FieldDocument) null;
    };

    
    // Add document to index as JSON file (possibly gzipped)
    public FieldDocument addDocFile(String json, boolean gzip) {
        return this.addDoc(this._addDocfromFile(json, gzip));
    };


    // Add document to index as JSON file (possibly gzipped)
    public FieldDocument addDocFile(int uid, String json, boolean gzip) {
        FieldDocument fd = this._addDocfromFile(json, gzip);
        if (fd != null) {
            fd.setUID(uid);
            return this.addDoc(fd);
        };
        return fd;
    };


    // Commit changes to the index
    public void commit (boolean force) throws IOException {
        // There is something to commit
        if (commitCounter > 0 || !force)
            this.commit();
    };


    // Commit changes to the index
    public void commit () throws IOException {
        // Open writer if not already opened
        if (this.writer == null)
            this.writer = new IndexWriter(this.directory, this.config);

        // Force commit
        this.writer.commit();
        commitCounter = 0;
        this.closeReader();
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

            // Set the position in the iterator to the term that is seeked
            if (termsEnum.seekExact(term.bytes())) {

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
     * e.g. <i>documents</i>, <i>sentences</i> etc.
     *
     * @param collection The scope of the numbering by means of a
     *        {@link KorapCollection}
     * @param field The field containing the textual data and the
     *        annotations as a string.
     * @param type The type of meta information,
     *        e.g. <i>documents</i> or <i>sentences</i> as a string.
     * @return The number of the occurrences.
     */
    public long numberOf (KorapCollection collection,
                          String field,
                          String type) {
        // Short cut for documents
        // This will be only "texts" in the future
        if (type.equals("documents") || type.equals("base/texts")) {
            if (collection.getCount() <= 0) {
                try {
                    return (long) this.reader().numDocs();
                }
                catch (Exception e) {
                    log.warn(e.getLocalizedMessage());
                };
                return (long) 0;
            };

            long docCount = 0;
            int i = 1;
            try {
                for (AtomicReaderContext atomic : this.reader().leaves()) {
                    docCount += collection.bits(atomic).cardinality();
                    i++;
                };
            }
            catch (IOException e) {
                log.warn(e.getLocalizedMessage());
            };
            return docCount;
        };
    
        // Create search term
        // This may be prefixed by foundries
        Term term = new Term(field, "-:" + type);

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
        catch (Exception e) {
            log.warn( e.getLocalizedMessage() );
        };

        return occurrences;
    };



    public long numberOf (String field, String type) {
        return this.numberOf(new KorapCollection(this), field, type);
    };


    /**
     * Search for the number of occurrences of different types,
     * e.g. <i>documents<i>, <i>sentences</i> etc., in the
     * <i>base</i> foundry.
     *
     * @param type The type of meta information,
     *        e.g. <i>documents</i> or <i>sentences</i> as a string.
     * @return The number of the occurrences.
     */
    public long numberOf (String type) {
        return this.numberOf("tokens", type);
    };


    /**
     * Search for the number of occurrences of different types,
     * e.g. <i>documents</i>, <i>sentences</i> etc.
     *
     * @param docvec The scope of the numbering by means of a
     *        {@link Bits} vector
     * @param field The field containing the textual data and the
     *        annotations as a string.
     * @param type The type of meta information,
     *        e.g. <i>documents</i> or <i>sentences</i> as a string.
     * @return The number of the occurrences.
     * @throws IOException
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


    public String getMatchIDWithContext (String id) {
        /* No includeHighlights */
        return "";
    };


    public KorapMatch getMatch (String id) throws QueryException {
        return this.getMatchInfo(
            id,       // MatchID
            "tokens", // field
            false,    // info
            (ArrayList) null,     // foundry
            (ArrayList) null,     // layer
            false,    // includeSpans
            true,     // includeHighlights
            false     // extendToSentence
        );
    };


    // There is a good chance that some of these methods will die ...
    public KorapMatch getMatchInfo (String id,
                                    String field,
                                    String foundry,
                                    String layer,
                                    boolean includeSpans,
                                    boolean includeHighlights) throws QueryException {
        return this.getMatchInfo(
            id,
            field,
            true,
            foundry,
            layer,
            includeSpans,
            includeHighlights,
            false
        );
    };


    public KorapMatch getMatchInfo (String id,
                                    String field,
                                    String foundry,
                                    String layer,
                                    boolean includeSpans,
                                    boolean includeHighlights,
                                    boolean extendToSentence) throws QueryException {
        return this.getMatchInfo(
            id,
            field,
            true,
            foundry,
            layer,
            includeSpans,
            includeHighlights,
            extendToSentence
        );
    };

    public KorapMatch getMatchInfo (String id,
                                    String field,
                                    boolean info,
                                    String foundry,
                                    String layer,
                                    boolean includeSpans,
                                    boolean includeHighlights,
                                    boolean extendToSentence) throws QueryException {
    	ArrayList<String> foundryList = new ArrayList<>(1);
        if (foundry != null)
            foundryList.add(foundry);
        ArrayList<String> layerList = new ArrayList<>(1);
        if (layer != null)
            layerList.add(layer);
        return this.getMatchInfo(
            id,
            field,
            info,
            foundryList,
            layerList,
            includeSpans,
            includeHighlights,
            extendToSentence
        );
    };


    /**
     * Get a match.
     */
    /*
      KorapInfo is associated with a KorapMatch and has an array with all informations
      per position in the match.
    */
    public KorapMatch getMatchInfo (String idString,
                                    String field,
                                    boolean info,
                                    List<String> foundry,
                                    List<String> layer,
                                    boolean includeSpans,
                                    boolean includeHighlights,
                                    boolean extendToSentence) throws QueryException {

        KorapMatch match = new KorapMatch(idString, includeHighlights);

        if (this.getVersion() != null)
            match.setVersion(this.getVersion());

        if (this.getName() != null)
            match.setName(this.getName());

        if (match.getStartPos() == -1)
            return match;

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
            StringBuilder regex = new StringBuilder();
            // TODO: Make these static
            Pattern harmlessFoundry = Pattern.compile("^[-a-zA-Z0-9_]+$");
            Pattern harmlessLayer   = Pattern.compile("^[-a-zA-Z0-9_:]+$");
            Iterator<String> iter;
            int i = 0;
	    
            if (includeSpans)
                regex.append("((\">\"|\"<\"\">\")\":\")?");
            
            // There is a foundry given
            if (foundry != null && foundry.size() > 0) {

                // Filter out bad foundries
                for (i = foundry.size() - 1; i >= 0 ; i--) {
                    if (!harmlessFoundry.matcher(foundry.get(i)).matches()) {
                        throw new QueryException("Invalid foundry requested: '" + foundry.get(i) + "'");
                        // foundry.remove(i);
                    };
                };

                // Build regex for multiple foundries
                if (foundry.size() > 0) {
                    regex.append("(");
                    iter = foundry.iterator();
                    while (iter.hasNext()) {
                        regex.append(iter.next()).append("|");
                    };
                    regex.replace(regex.length() - 1, regex.length(), ")");
                    regex.append("\"/\"");

                    // There is a filter given
                    if (layer != null && layer.size() > 0) {

                        // Filter out bad layers
                        for (i = layer.size() - 1; i >= 0 ; i--) {
                            if (!harmlessLayer.matcher(layer.get(i)).matches()) {
                                throw new QueryException("Invalid layer requested: " + layer.get(i));
                                // layer.remove(i);
                            };
                        };

                        // Build regex for multiple layers
                        if (layer.size() > 0) {
                            regex.append("(");
                            iter = layer.iterator();
                            while (iter.hasNext()) {
                                regex.append(iter.next()).append("|");
                            };
                            regex.replace(regex.length() - 1, regex.length(), ")");
                            regex.append("\":\"");
                        };
                    };
                };
            }
            else if (includeSpans) {
                // No foundries - but spans
                regex.append("([^-is]|[-is][^:])");
            }
            else {
                // No foundries - no spans
                regex.append("([^-is<>]|[-is>][^:]|<[^:>])");
            };
            regex.append("(.){1,}|_[0-9]+");
            
            if (DEBUG)
                log.trace("The final regexString is {}", regex.toString());
            RegExp regexObj = new RegExp(regex.toString(), RegExp.COMPLEMENT);
            fst = new CompiledAutomaton(regexObj.toAutomaton());
            if (DEBUG)
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
                if (DEBUG)
                    log.trace("We've found a matching document");

                HashSet<String> fields = (HashSet<String>)
                    new KorapSearch().getFields().clone();
                fields.add(field);

                // Get terms from the document
                Terms docTerms = atomic.reader().getTermVector(localDocID, field);

                // Load the necessary fields of the document
                Document doc = atomic.reader().document(localDocID, fields);

                // Put some more information to the match
                PositionsToOffset pto = new PositionsToOffset(atomic, field);
                match.setPositionsToOffset(pto);
                match.setLocalDocID(localDocID);
                match.populateDocument(doc, field, fields);
                if (DEBUG)
                    log.trace("The document has the id '{}'", match.getDocID());

                SearchContext context = match.getContext();

                // Search for minimal surrounding sentences
                if (extendToSentence) {
                    int [] spanContext = match.expandContextToSpan("s");
                    match.setStartPos(spanContext[0]);
                    match.setEndPos(spanContext[1]);
                    match.startMore = false;
                    match.endMore = false;
                }
                else {
                    if (DEBUG)
                        log.trace("Don't expand context");
                };
                
                context.left.setToken(true).setLength(0);
                context.right.setToken(true).setLength(0);

                if (!info)
                    break;

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
                    
                    // String representation of the term
                    String termString = termsEnum.term().utf8ToString();

                    // Iterate over all occurrences
                    for (int i = 0; i < termOccurrences; i++) {

                        // Init positions and get the current
                        int pos = docs.nextPosition();

                        // Check, if the position of the term is in the area of interest
                        if (pos >= match.getStartPos() && pos < match.getEndPos()) {

                            if (DEBUG)
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
                                if (DEBUG)
                                    log.trace("Add {}", ti.toString());
                                termList.add(ti);
                            };
                        };
                    };
                };

                // Add annotations based on the retrieved infos
                for (TermInfo t : termList.getTerms()) {
                    if (DEBUG)
                        log.trace(
                            "Add term {}/{}:{} to {}({})-{}({})",
                            t.getFoundry(),
                            t.getLayer(),
                            t.getValue(),
                            t.getStartChar(),
                            t.getStartPos(),
                            t.getEndChar(),
                            t.getEndPos()
                        );

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


    @Deprecated
    public HashMap getTermRelation (String field) throws Exception {
        return this.getTermRelation(new KorapCollection(this), field);
    };


    /**
     * Analyze how terms relate
     */
    @Deprecated
    public HashMap getTermRelation (KorapCollection kc, String field) throws Exception {
        HashMap<String,Long> map = new HashMap<>(100);
        long docNumber = 0, checkNumber = 0;
        
        try {
            if (kc.getCount() <= 0) {
                checkNumber = (long) this.reader().numDocs();
            };

            for (AtomicReaderContext atomic : this.reader().leaves()) {
                HashMap<String,FixedBitSet> termVector = new HashMap<>(20);
        
                FixedBitSet docvec = kc.bits(atomic);
                if (docvec != null) {
                    docNumber += docvec.cardinality();		    
                };

                Terms terms = atomic.reader().fields().terms(field);

                if (terms == null) {
                    continue;
                };
		
                int docLength = atomic.reader().maxDoc();
                FixedBitSet bitset = new FixedBitSet(docLength);

                // Iterate over all tokens in this field
                TermsEnum termsEnum = terms.iterator(null);

                while (termsEnum.next() != null) {
                    
                    String termString = termsEnum.term().utf8ToString();

                    bitset.clear(0,docLength);
	    
                    // Get frequency
                    bitset.or((DocIdSetIterator) termsEnum.docs((Bits) docvec, null));
            
                    long value = 0;
                    if (map.containsKey(termString))
                        value = map.get(termString);

                    map.put(termString, value + bitset.cardinality());
                    
                    termVector.put(termString, bitset.clone());
                };
                
                int keySize = termVector.size();
                String[] keys = termVector.keySet().toArray(new String[keySize]);
                java.util.Arrays.sort(keys);

                if (keySize > maxTermRelations) {
                    throw new Exception(
                        "termRelations are limited to " + maxTermRelations + " sets" +
                        " (requested were at least " + keySize + " sets)"
                    );
                };
                
                for (int i = 0; i < keySize; i++) {
                    for (int j = i+1; j < keySize; j++) {
                        FixedBitSet comby = termVector.get(keys[i]).clone();
                        comby.and(termVector.get(keys[j]));

                        StringBuilder sb = new StringBuilder();
                        sb.append("#__").append(keys[i]).append(":###:").append(keys[j]);
                        String combString = sb.toString();
                        
                        long cap = (long) comby.cardinality();
                        if (map.containsKey(combString)) {
                            cap += map.get(combString);
                        };
                        map.put(combString, cap);
                    };
                };
            };
            map.put("-docs", checkNumber != 0 ? checkNumber : docNumber);
        }
        catch (IOException e) {
            log.warn(e.getMessage());	    
        };
        return map;
    };

    
    /**
     * Search in the index.
     */
    public KorapResult search (SpanQuery query) {
        return this.search(new KorapSearch(query));
    };


    public KorapResult search (SpanQuery query, short count) {
        return this.search(
            new KorapSearch(query).setCount(count)
        );
    };


    @Deprecated
    public KorapResult search (SpanQuery query,
                               int startIndex,
                               short count,
                               boolean leftTokenContext,
                               short leftContext,
                               boolean rightTokenContext,
                               short rightContext) {

        KorapSearch ks = new KorapSearch(query);
        ks.setStartIndex(startIndex).setCount(count);
        ks.setContext(
            new SearchContext(
                leftTokenContext,
                leftContext,
                rightTokenContext,
                rightContext
            )
        );	
        return this.search(ks);
    };


    @Deprecated
    public KorapResult search (KorapCollection collection,
                               SpanQuery query,
                               int startIndex,
                               short count,
                               boolean leftTokenContext,
                               short leftContext,
                               boolean rightTokenContext,
                               short rightContext) {
        KorapSearch ks = new KorapSearch(query);
        ks.setContext(
            new SearchContext(
                leftTokenContext,
                leftContext,
                rightTokenContext,
                rightContext
            )
        );
        ks.setCollection(collection);
        return this.search(ks);
    };


    /**
     * Search the endpoint.
     */
    public KorapResult search (KorapSearch ks) {
        if (DEBUG)
            log.trace("Start search");

        this.termContexts = new HashMap<Term, TermContext>();

        KorapCollection collection = ks.getCollection();
        collection.setIndex(this);

        // Get the spanquery from the KorapSearch object
        SpanQuery query = ks.getQuery();

        // Get the field of textual data and annotations ("tokens")
        String field = query.getField();

        // Todo: Make kr subclassing ks - so ks has a method for a new KorapResult!
        KorapResult kr = new KorapResult(
            query.toString(),
            ks.getStartIndex(),
            ks.getCount(),
            ks.getContext()
        );

        // Set version info to result
        if (this.getVersion() != null)
            kr.setVersion(this.getVersion());

        // The following fields should be lifted for matches
        HashSet<String> fields = (HashSet<String>) ks.getFields().clone();
        fields.add(field);

        // Some initializations ...
        int i                       = 0,
            startIndex              = kr.getStartIndex(),
            count                   = kr.getItemsPerPage(),
            hits                    = kr.getItemsPerPage() + startIndex,
            limit                   = ks.getLimit(),
            itemsPerResourceCounter = 0;
        boolean cutoff              = ks.doCutOff();
        short itemsPerResource      = ks.getItemsPerResource();

        // Check if there is work to do at all
        if (limit > 0) {
            if (hits > limit)
                hits = limit;

            // Nah - nothing to do! Let's go shopping!
            if (limit < startIndex)
                return kr;
        };

        // Collect matches from atomic readers
        ArrayList<KorapMatch> atomicMatches = new ArrayList<KorapMatch>(kr.getItemsPerPage());
        
        // Start time out thread
        TimeOutThread tthread = new TimeOutThread();
        tthread.start();
        long timeout = ks.getTimeOut();

        // See: http://www.ibm.com/developerworks/java/library/j-benchmark1/index.html
        long t1 = System.nanoTime();

        try {
            // Rewrite query (for regex and wildcard queries)
            // Revise!
            // Based on core/src/java/org/apache/lucene/search/IndexSearcher.java
            // and highlighter/src/java/org/apache/lucene/search/postingshighlight/PostingsHighlighter.java
            for ( Query rewrittenQuery = query.rewrite(this.reader());
                  !rewrittenQuery.equals(query);
                  rewrittenQuery = query.rewrite(this.reader())) {
                query = (SpanQuery) rewrittenQuery;
            };
	    

            // Todo: run this in a separated thread
            for (AtomicReaderContext atomic : this.reader().leaves()) {

                int oldLocalDocID = -1;

                /*
                 * Todo: There may be a way to know early if the bitset is emty
                 * by using OpenBitSet - but this may not be as fast as I think.
                 */
                Bits bitset = collection.bits(atomic);

                PositionsToOffset pto = new PositionsToOffset(atomic, field);

                // Spans spans = NearSpansOrdered();
                Spans spans = query.getSpans(atomic, (Bits) bitset, termContexts);

                IndexReader lreader = atomic.reader();
                
                // TODO: Get document information from Cache! Fieldcache?
                for (; i < hits;i++) {

                    if (DEBUG)
                        log.trace("Match Nr {}/{}", i, count);
                    
                    // There are no more spans to find
                    if (!spans.next())
                        break;

                    // Timeout!
                    if (tthread.getTime() > timeout) {
                        kr.setTimeExceeded(true);
                        break;
                    };
                    
                    int localDocID = spans.doc();

                    // Count hits per resource
                    if (itemsPerResource > 0) {

                        // IDS are identical
                        if (localDocID == oldLocalDocID || oldLocalDocID == -1) {
                            if (itemsPerResourceCounter++ >= itemsPerResource) {
                                if (spans.skipTo(localDocID + 1) != true) {
                                    break;
                                }
                                else {
                                    itemsPerResourceCounter = 1;
                                    localDocID = spans.doc();
                                };
                            };
                        }
                        
                        // Reset counter
                        else
                            itemsPerResourceCounter = 0;
                        
                        oldLocalDocID = localDocID;
                    };

                    // The next matches are not yet part of the result
                    if (startIndex > i)
                        continue;

                    int docID = atomic.docBase + localDocID;
                    
                    // Do not load all of this, in case the doc is the same!
                    Document doc = lreader.document(localDocID, fields);

                    // Create new KorapMatch
                    KorapMatch match = new KorapMatch(
                        pto,
                        localDocID,
                        spans.start(),
                        spans.end()
                    );
                    match.setContext(kr.getContext());

                    // Add match to KorapResult
                    kr.add(match);

                    if (spans.isPayloadAvailable())
                        match.addPayload((List<byte[]>) spans.getPayload());

                    match.internalDocID = docID;
                    match.populateDocument(doc, field, fields);
		    
                    if (DEBUG) {
                        if (match.getDocID() != null)
                            log.trace("I've got a match in {} of {}",
                                      match.getDocID(), count);
                        else
                            log.trace("I've got a match in {} of {}",
                                      match.getUID(), count);
                    };
                    
                    atomicMatches.add(match);
                };

                // Can be disabled TEMPORARILY
                while (!cutoff && spans.next()) {
                    if (limit > 0 && i >= limit)
                        break;
                    
                    // Timeout!
                    if (tthread.getTime() > timeout) {
                        kr.setTimeExceeded(true);
                        break;
                    };

                    // Count hits per resource
                    if (itemsPerResource > 0) {
                        int localDocID = spans.doc();

                        if (localDocID == DocIdSetIterator.NO_MORE_DOCS)
                            break;
			
                        // IDS are identical
                        if (localDocID == oldLocalDocID || oldLocalDocID == -1) {
                            if (localDocID == -1)
                                break;

                            if (itemsPerResourceCounter++ >= itemsPerResource) {
                                if (spans.skipTo(localDocID + 1) != true) {
                                    break;
                                };
                                itemsPerResourceCounter = 1;
                                localDocID = spans.doc();
                            };
                        }

                        // Reset counter
                        else
                            itemsPerResourceCounter = 0;

                        oldLocalDocID = localDocID;
                    };
                    i++;
                };
                atomicMatches.clear();
            };

            if (itemsPerResource > 0)
                kr.setItemsPerResource(itemsPerResource);

            kr.setTotalResults(cutoff ? (long) -1 : (long) i);
        }
        catch (IOException e) {
            kr.addError(
                600,
                "Unable to read index",
                e.getLocalizedMessage()
            );
            log.warn( e.getLocalizedMessage() );
        };

        // Stop timer thread
        tthread.stopTimer();

        // Calculate time
        kr.setBenchmark(t1, System.nanoTime());

        return kr;
    };


    // Collect matches
    public MatchCollector collect (KorapSearch ks, MatchCollector mc) {
        if (DEBUG)
            log.trace("Start collecting");

        KorapCollection collection = ks.getCollection();
        collection.setIndex(this);

        // Init term context
        this.termContexts = new HashMap<Term, TermContext>();

        // Get span query
        SpanQuery query = ks.getQuery();

        // Get the field of textual data and annotations
        String field = query.getField();

        // TODO: Get document information from Cache!
        // See: http://www.ibm.com/developerworks/java/library/j-benchmark1/index.html
        long t1 = System.nanoTime();

        // Only load UIDs
        HashSet<String> fields = new HashSet<>(1);
        fields.add("UID");

        // List<KorapMatch> atomicMatches = new ArrayList<KorapMatch>(10);
        try {

            // Rewrite query (for regex and wildcard queries)
            for (Query rewrittenQuery = query.rewrite(this.reader());
                 rewrittenQuery != (Query) query;
                 rewrittenQuery = query.rewrite(this.reader())) {
                query = (SpanQuery) rewrittenQuery;
            };

            int matchcount = 0;
            String uniqueDocIDString;;
            int uniqueDocID = -1;

            // start thread:
            for (AtomicReaderContext atomic : this.reader().leaves()) {

                int previousDocID = -1;
                int oldLocalDocID = -1;

                // Use OpenBitSet;
                Bits bitset = collection.bits(atomic);

                // PositionsToOffset pto = new PositionsToOffset(atomic, field);
                
                Spans spans = query.getSpans(atomic, (Bits) bitset, termContexts);

                IndexReader lreader = atomic.reader();

                while (spans.next()) {
                    int localDocID = spans.doc();

                    // New match
                    // MatchIdentifier possibly needs more
                    /*
                      KorapMatch match = new KorapMatch();
                      match.setStartPos(spans.start());
                      match.setEndPos(spans.end());
                      
                      // Add payload information to match
                      if (spans.isPayloadAvailable())
                      match.addPayload(spans.getPayload());
                    */

                    if (previousDocID != localDocID) {
                        if (matchcount > 0) {
                            mc.add(uniqueDocID, matchcount);
                            matchcount = 0;
                        };

                        // Read document id from index
                        uniqueDocIDString =
                            lreader.document(localDocID, fields).get("UID");

                        if (uniqueDocIDString != null)
                            uniqueDocID = Integer.parseInt(uniqueDocIDString);
                        
                        previousDocID = localDocID;
                    }
                    else {
                        matchcount++;
                    };
                };

                // Add count to collector
                if (matchcount > 0) {
                    mc.add(uniqueDocID, matchcount);
                    matchcount = 0;
                };
            };
            // end thread

            // Benchmark the collector
            mc.setBenchmark(t1, System.nanoTime());
        }
        catch (IOException e) {
            mc.addError(
                600,
                "Unable to read index",
                e.getLocalizedMessage()
            );
            log.warn(e.getLocalizedMessage());
        };

        mc.close();
        return mc; 
    };
};
