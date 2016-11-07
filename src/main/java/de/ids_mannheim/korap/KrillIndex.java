package de.ids_mannheim.korap;

// Krill classes
import de.ids_mannheim.korap.*;
import de.ids_mannheim.korap.index.*;
import de.ids_mannheim.korap.response.*;
import de.ids_mannheim.korap.query.SpanElementQuery;
import de.ids_mannheim.korap.util.QueryException;
import static de.ids_mannheim.korap.util.KrillProperties.*;

// Lucene classes
import org.apache.lucene.search.*;
import org.apache.lucene.search.spans.*;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.*;
import org.apache.lucene.analysis.Analyzer;
/*
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
*/
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.util.*;
import org.apache.lucene.util.automaton.*;

// JSON helper class
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;

// Log4j Logger classes
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Java core classes
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.regex.Pattern;
import java.io.*;
import java.net.URL;
import java.nio.ByteBuffer;

/**
 * <p>KrillIndex implements a simple API for searching in and writing
 * to a
 * Lucene index and requesting several information about the index'
 * nature.
 * Please consult {@link Krill} for the preferred use of this
 * class.</p>
 * 
 * <blockquote><pre>
 * // Create new file backed index
 * KrillIndex ki = new KrillIndex(
 * new MMapDirectory(new File("/myindex"))
 * );
 * 
 * // Add documents to the index
 * ki.addDoc(1, "{\"ID\":\"WPD-001\", ... }");
 * ki.addDoc(2, "{\"ID\":\"WPD-002\", ... }");
 * 
 * // Apply Krill searches on the index
 * String koral = "{\"@type\":"koral:group", ... }";
 * Result result = new Krill(koral).apply(ki);
 * </pre></blockquote>
 * 
 * <p>Properties can be stored in a properies file called
 * <tt>krill.properties</tt>.
 * 
 * @author diewald
 */
/*
 * Concerning parallel processing:
 * ===============================
 * Search /could/ be run in parallel on atomic readers
 * (although Lucene developers strongly discourage that).
 * Benefits are not clear and would need some benchmarks,
 * the huge drawback would be more complicated testing.
 * Aside from (probably) co-occurrence analysis, shared memory
 * is not an important thing, so I guess the preferred
 * way of using Krill on multicore machines for now is by using
 * the same mechanism as for distribution:
 * Running multiple nodes (and separated indices) per machine,
 * registered independently at the Zookeeper.
 *
 * On the other hand: Threaded indexing should be implemented!
 */
/*
  TODO: Use FieldCache!!!
  TODO: Add word count as a meta data field!
  TODO: Improve validation of document import!
  TODO: Don't store the text in the token field!
        (It has only to be lifted for match views!
        Benchmark how worse that is!)
  TODO: Support layer for specific foundries in terminfo (IMPORTANT)
  TODO: Reuse the indexreader everywhere - it should be threadsafe!
  TODO: Support document removal!
  TODO: Support document update!
  TODO: Support callback for interrupts (to stop the searching)!

  http://invertedindex.blogspot.co.il/2009/04/lucene-dociduid-mapping-and-payload.html
  see korap/search.java -> retrieveTokens

  Support frequency search with regular expressions, so multiple bookkeeping:
  c<:VVFIN:ging:gehen:past::
  c>:VVFIN:gnig:neheg:past::
  -> search for frequencies of VVFIN/gehen
  -> c:VVFIN:[^:]*?:gehen:past:...
*/
public final class KrillIndex {

    // Logger
    private final static Logger log = LoggerFactory.getLogger(KrillIndex.class);

    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;

    // TODO: Use configuration instead.
    // Last line of defense against DOS
    private int autoCommit = 500;
    private String version = "Unknown";
    private String name = "Unknown";

    // Temp:
    private IndexReader reader;

    private IndexWriter writer;
    private IndexWriterConfig config;
    private IndexSearcher searcher;
    private boolean readerOpen = false;
    private Directory directory;

    // The commit counter is only there for
    // counting unstaged changes per thread (for bulk insertions)
    // It does not represent real unstaged documents.
    private int commitCounter = 0;
    private HashMap termContexts;
    private ObjectMapper mapper = new ObjectMapper();

    private byte[] pl = new byte[4];
    private static ByteBuffer bb = ByteBuffer.allocate(4),
            bbOffset = ByteBuffer.allocate(8), bbTerm = ByteBuffer.allocate(16);

    // Some initializations ...
    {
        Properties prop = loadProperties();
        Properties info = loadInfo();
        if (info != null) {
            this.version = info.getProperty("krill.version");
            this.name = info.getProperty("krill.name");
        };

        // Check for auto commit value
        String autoCommitStr = null;
        if (prop != null)
            autoCommitStr = prop.getProperty("krill.index.commit.auto");

        if (autoCommitStr != null) {
            try {
                this.autoCommit = Integer.parseInt(autoCommitStr);
            }
            catch (NumberFormatException e) {
                log.error(
                        "krill.index.commit.auto expected to be a numerical value");
            };
        };
    };


    /**
     * Constructs a new KrillIndex.
     * This will be in-memory.
     * 
     * @throws IOException
     */
    public KrillIndex () throws IOException {
        this((Directory) new RAMDirectory());
    };


    /**
     * Constructs a new KrillIndex bound to a persistant index.
     * 
     * @param directory
     *            A {@link Directory} pointing to an index
     * @throws IOException
     */
    public KrillIndex (Directory directory) throws IOException {
        this.directory = directory;

        // Add analyzers
        // TODO: Should probably not be here - make configurable
        Map<String, Analyzer> analyzerPerField = new HashMap<String, Analyzer>();
        analyzerPerField.put("textClass", new KeywordAnalyzer());
        analyzerPerField.put("keywords", new KeywordAnalyzer());
        analyzerPerField.put("foundries", new KeywordAnalyzer());
        PerFieldAnalyzerWrapper analyzer = new PerFieldAnalyzerWrapper(
                new TextAnalyzer(), analyzerPerField);

        // Create configuration with base analyzer
        this.config = new IndexWriterConfig(analyzer);
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
     * The Lucene {@link IndexReader} object.
     * 
     * Will be opened, in case it's closed.
     * 
     * @return The {@link IndexReader} object.
     */
    public IndexReader reader () {
        // Todo: Maybe use DirectoryReader.openIfChanged(DirectoryReader)
        if (!readerOpen)
            this.openReader();
        return this.reader;
    };


    /**
     * The Lucene {@link IndexWriter} object.
     * 
     * Will be created, in case it doesn't exist yet.
     * 
     * @return The {@link IndexWriter} object.
     * @throws IOException
     */
    public IndexWriter writer () throws IOException {
        // Open writer if not already opened
        if (this.writer == null)
            this.writer = new IndexWriter(this.directory, this.config);
        return this.writer;
    };


    /**
     * The Lucene {@link IndexSearcher} object.
     * 
     * Will be created, in case it doesn't exist yet.
     * 
     * @return The {@link IndexSearcher} object.
     */
    public IndexSearcher searcher () {
        if (this.searcher == null)
            this.searcher = new IndexSearcher(this.reader());
        return this.searcher;
    };


    // Open index reader
    private void openReader () {
        try {
            // open reader
            this.reader = DirectoryReader.open(this.directory);
            readerOpen = true;
            if (this.searcher != null)
                this.searcher = new IndexSearcher(reader);
        }

        // Failed to open reader
        catch (IOException e) {
            // e.printStackTrace();
            log.warn(e.getLocalizedMessage());
        };
    };


    // Close index reader
    private void closeReader () throws IOException {
        if (readerOpen) {
            this.reader.close();
            readerOpen = false;
        };
    };


    // Close index writer
    private void closeWriter () throws IOException {
        if (this.writer != null)
            this.writer.close();
    };


    /**
     * Close the associated {@link IndexReader} and the associated
     * {@link IndexWriter},
     * in case they are opened.
     * 
     * @throws IOException
     */
    public void close () throws IOException {
        this.closeReader();
        this.closeWriter();
    };


    /**
     * Commit staged data to the index,
     * if the commit counter indicates there is staged data.
     * 
     * @param force
     *            Force the commit,
     *            even if there is no staged data.
     * @throws IOException
     */
    public void commit (boolean force) throws IOException {
        // There is something to commit
        if (commitCounter > 0 || !force)
            this.commit();
    };


    /**
     * Commit staged data to the index.
     * 
     * @throws IOException
     */
    public void commit () throws IOException {
        this.writer().commit();
        commitCounter = 0;
        this.closeReader();
    };


    /**
     * Get autocommit value.
     * 
     * @return The autocommit value.
     */
    public int getAutoCommit () {
        return this.autoCommit;
    };


    /**
     * Set the autocommit value.
     * 
     * @param value
     *            The autocommit value.
     */
    public void setAutoCommit (int value) {
        this.autoCommit = value;
    };


    /**
     * Add a document to the index as a {@link FieldDocument}.
     * 
     * @param doc
     *            The {@link FieldDocument} to add to the index.
     * @return The {@link FieldDocument}, which means, the same
     *         object, that was passed to the method.
     */
    public FieldDocument addDoc (FieldDocument doc) {
        if (doc == null)
            return doc;

        try {

            // Add document to writer
            this.writer().addDocument(doc.doc);
            if (++commitCounter > autoCommit) {
                this.commit();
                commitCounter = 0;
            };
        }

        // Failed to add document
        catch (IOException e) {
            log.error("Unable to add document");
        };

        return doc;
    };


    /**
     * Delete documents of the index by passing field information.
     * 
     * @param field
     *            The meta field name.
     * @param term
     *            The meta field term.
     */
    public boolean delDocs (String field, String term) {
        if (field == null || term == null)
            return false;
        try {
            this.writer().deleteDocuments(new Term(field, term));
            if (++commitCounter > autoCommit) {
                this.commit();
                commitCounter = 0;
            };

            return true;
        }

        // Failed to add document
        catch (IOException e) {
            log.error("Unable to delete documents");
        };

        return false;
    };


    /**
     * Delete a document of the index by passing a UID.
     * 
     * @param uid
     *            The unique identifier of the document.
     */
    public boolean delDoc (Integer uid) {
        if (uid < 0)
            return false;
        return this.delDocs("UID", uid.toString());
    };


    /**
     * Add a document to the index as a JSON string.
     * 
     * @param json
     *            The document to add to the index as a string.
     * @return The created {@link FieldDocument}.
     * @throws IOException
     */
    public FieldDocument addDoc (String json) {
        return this.addDoc(_fromJson(json));
    };


    /**
     * Add a document to the index as a JSON string
     * with a unique integer ID (unique throughout the index
     * or even throughout the cluster of indices).
     * 
     * @param uid
     *            The unique document identifier.
     * @param json
     *            The document to add to the index as a string.
     * @return The created {@link FieldDocument}.
     * @throws IOException
     */
    public FieldDocument addDoc (Integer uid, String json) {
        FieldDocument fd = _fromJson(json);
        if (fd != null) {
            fd.setUID(uid);
            fd = this.addDoc(fd);
        };
        return fd;
    };


    /**
     * Add a document to the index as a JSON string.
     * 
     * @param json
     *            The document to add to the index as
     *            an {@link InputStream}.
     * @return The created {@link FieldDocument}.
     * @throws IOException
     */
    public FieldDocument addDoc (InputStream json) {
        return this.addDoc(_fromFile(json, false));
    };


    /**
     * Add a document to the index as a JSON string.
     * 
     * @param json
     *            The document to add to the index as
     *            an {@link InputStream}.
     * @param gzip
     *            Boolean value indicating if the file is gzipped.
     * @return The created {@link FieldDocument}.
     * @throws IOException
     */
    public FieldDocument addDoc (InputStream json, boolean gzip) {
        return this.addDoc(_fromFile(json, gzip));
    };


    /**
     * Add a document to the index as a JSON string
     * with a unique integer ID (unique throughout the index
     * or even throughout the cluster of indices).
     * 
     * @param uid
     *            The unique document identifier.
     * @param json
     *            The document to add to the index as
     *            an {@link InputStream}.
     * @param gzip
     *            Boolean value indicating if the file is gzipped.
     * @return The created {@link FieldDocument}.
     * @throws IOException
     */
    public FieldDocument addDoc (Integer uid, InputStream json, boolean gzip) {
        FieldDocument fd = _fromFile(json, gzip);
        if (fd != null) {
            fd.setUID(uid);
            return this.addDoc(fd);
        };
        return fd;
    };


    // Parse JSON document from Input stream
    private FieldDocument _fromJson (String json) {
        try {
            FieldDocument fd = this.mapper.readValue(json, FieldDocument.class);
            return fd;
        }
        catch (IOException e) {
            log.error("File json not found or unmappable: {}",
                    e.getLocalizedMessage());
        };
        return (FieldDocument) null;
    };


    // Load json document from file
    private FieldDocument _fromFile (InputStream json, boolean gzip) {
        try {
            if (gzip) {

                // Create json field document
                FieldDocument fd = this.mapper.readValue(
                        new GZIPInputStream(json), FieldDocument.class);
                return fd;
            };
            return this.mapper.readValue(json, FieldDocument.class);
        }

        // Fail to add json object
        catch (IOException e) {
            log.error("File {} not found", json, e);
        };
        return (FieldDocument) null;
    };


    /**
     * Search for the number of occurrences of different types,
     * e.g. <i>documents</i>, <i>sentences</i> etc.
     * 
     * @param collection
     *            The scope of the numbering by means of a
     *            {@link KrillCollection}
     * @param field
     *            The field containing the textual data and the
     *            annotations as a string.
     * @param type
     *            The type of meta information,
     *            e.g. <i>documents</i> or <i>sentences</i> as a
     *            string.
     * @return The number of the occurrences.
     * @see KrillCollection#numberOf
     */
    public long numberOf (KrillCollection collection, String field,
            String type) {

        collection.setIndex(this);
        try {
            return collection.numberOf(field, type);
        }
        catch (IOException e) {
            log.warn(e.getLocalizedMessage());
        };
        return (long) -1;

        // Short cut for documents
        // This will be only "texts" in the future
        /*
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
            // int i = 1;
            try {
                for (LeafReaderContext atomic : this.reader().leaves()) {
                    docCount += collection.bits(atomic).cardinality();
                    // i++;
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
            for (LeafReaderContext atomic : this.reader().leaves()) {
                occurrences += this._numberOfAtomic(collection.bits(atomic),
                        atomic, term);
            };
        }
        
        // Something went wrong
        catch (Exception e) {
            log.warn(e.getLocalizedMessage());
        };
        
        return occurrences;
        */
    };


    /**
     * Search for the number of occurrences of different types,
     * e.g. <i>documents</i>, <i>sentences</i> etc.
     * 
     * @param field
     *            The field containing the textual data and the
     *            annotations as a string.
     * @param type
     *            The type of meta information,
     *            e.g. <i>documents</i> or <i>sentences</i> as a
     *            string.
     * @return The number of the occurrences.
     * @see KrillCollection#numberOf
     */
    public long numberOf (String field, String type) {
        return this.numberOf(new KrillCollection(this), field, type);
    };


    /**
     * Search for the number of occurrences of different types,
     * e.g. <i>documents<i>, <i>sentences</i> etc., in the
     * <i>base</i> foundry.
     * 
     * @param type
     *            The type of meta information,
     *            e.g. <i>documents</i> or <i>sentences</i> as a
     *            string.
     * @return The number of the occurrences.
     * @see KrillCollection#numberOf
     */
    public long numberOf (String type) {
        return this.numberOf("tokens", type);
    };


    /**
     * Search for the number of occurrences of different types,
     * e.g. <i>documents</i>, <i>sentences</i> etc.
     * 
     * @param docvec
     *            The scope of the numbering by means of a
     *            {@link Bits} vector
     * @param field
     *            The field containing the textual data and the
     *            annotations as a string.
     * @param type
     *            The type of meta information,
     *            e.g. <i>documents</i> or <i>sentences</i> as a
     *            string.
     * @return The number of the occurrences.
     * @throws IOException
     */
    /*
    @Deprecated
    public long numberOf (Bits docvec, String field, String type)
        throws IOException {
    // Shortcut for documents
    if (type.equals("documents")) {
        FixedBitSet os = (FixedBitSet) docvec;
        return os.cardinality();
    };
    
    Term term = new Term(field, "-:" + type);
    
    int occurrences = 0;
    try {
        for (LeafReaderContext atomic : this.reader().leaves()) {
            occurrences += this._numberOfAtomic(docvec, atomic, term);
        };
    }
    catch (IOException e) {
        log.warn(e.getLocalizedMessage());
    };
    
    return occurrences;
    };
    */


    // Search for meta information in term vectors
    // This will create the sum of all numerical payloads
    // of the term in the document vector
    @Deprecated
    private long _numberOfAtomic (Bits docvec, LeafReaderContext atomic,
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
                DocsAndPositionsEnum docs = termsEnum.docsAndPositions(docvec,
                        null, DocsAndPositionsEnum.FLAG_PAYLOADS);

                // The iterator is empty
                // This may even be an error, but we return 0
                if (docs.docID() == DocsAndPositionsEnum.NO_MORE_DOCS)
                    return 0;

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


    public Text getDoc (String uid) {
        // This is very similar to getMatchInfo

        Text text = new Text();

        // Rewrite parse ID
        uid = new Integer(Integer.parseInt(uid)).toString();

        Filter filter = (Filter) new QueryWrapperFilter(
                new TermQuery(new Term("UID", uid)));

        try {

            // Iterate over all atomic indices and find the matching document
            for (LeafReaderContext atomic : this.reader().leaves()) {

                // Retrieve the single document of interest
                DocIdSet filterSet = filter.getDocIdSet(atomic,
                        atomic.reader().getLiveDocs());

                // Create a bitset for the correct document
                Bits bitset = filterSet.bits();

                DocIdSetIterator filterIterator = filterSet.iterator();

                if (DEBUG)
                    log.trace("Checking document in {} with {}", filterSet,
                            bitset);

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

                // HashSet<String> fields = (HashSet<String>) new Krill()
                //    .getMeta().getFields().clone();
                // fields.add(field);

                // Load the necessary fields of the document

                // TODO: Probably use
                // document(int docID, StoredFieldVisitor visitor)
                Document doc = atomic.reader().document(localDocID);
                text.populateFields(doc);

                return text;
            };
        }
        catch (IOException e) {
            text.addError(600, "Unable to read index", e.getLocalizedMessage());
            log.warn(e.getLocalizedMessage());
        };

        text.addError(630, "Document not found");

        return text;
    };


    public String getMatchIDWithContext (String id) {
        /* No includeHighlights */
        return "";
    };


    public Match getMatch (String id) throws QueryException {
        return this.getMatchInfo(id,       // MatchID
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
    public Match getMatchInfo (String id, String field, String foundry,
            String layer, boolean includeSpans, boolean includeHighlights)
            throws QueryException {
        return this.getMatchInfo(id, field, true, foundry, layer, includeSpans,
                includeHighlights, false);
    };


    public Match getMatchInfo (String id, String field, String foundry,
            String layer, boolean includeSpans, boolean includeHighlights,
            boolean extendToSentence) throws QueryException {
        return this.getMatchInfo(id, field, true, foundry, layer, includeSpans,
                includeHighlights, extendToSentence);
    };


    public Match getMatchInfo (String id, String field, boolean info,
            String foundry, String layer, boolean includeSpans,
            boolean includeHighlights, boolean extendToSentence)
            throws QueryException {
        ArrayList<String> foundryList = new ArrayList<>(1);
        if (foundry != null)
            foundryList.add(foundry);
        ArrayList<String> layerList = new ArrayList<>(1);
        if (layer != null)
            layerList.add(layer);
        return this.getMatchInfo(id, field, info, foundryList, layerList,
                includeSpans, includeHighlights, extendToSentence);
    };


    /**
     * Get a match.
     */
    /*
      KorapInfo is associated with a Match and has an array with all informations
      per position in the match.
    */
    public Match getMatchInfo (String idString, String field, boolean info,
            List<String> foundry, List<String> layer, boolean includeSpans,
            boolean includeHighlights, boolean extendToSentence)
            throws QueryException {

        if (DEBUG)
            log.trace("Get info on {}", idString);

        Match match = new Match(idString, includeHighlights);

        if (this.getVersion() != null)
            match.setVersion(this.getVersion());

        if (this.getName() != null)
            match.setName(this.getName());

        if (match.getStartPos() == -1)
            return match;

        // Create a filter based on the corpusID and the docID
        BooleanQuery bool = new BooleanQuery();
        if (match.getTextSigle() != null) {
            bool.add(new TermQuery(new Term("textSigle", match.getTextSigle())),
                    BooleanClause.Occur.MUST);
        }

        // <legacy>
        else if (match.getDocID() != null) {
            bool.add(new TermQuery(new Term("ID", match.getDocID())),
                    BooleanClause.Occur.MUST);
            bool.add(new TermQuery(new Term("corpusID", match.getCorpusID())),
                    BooleanClause.Occur.MUST);
        }
        // </legacy>

        // Invalid
        else {
            match.addError(730, "Invalid match identifier", idString);
            return match;
        };

        if (DEBUG)
            log.trace("The bool query is {}", bool.toString());

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
            Pattern harmlessLayer = Pattern.compile("^[-a-zA-Z0-9_:]+$");
            Iterator<String> iter;
            int i = 0;

            if (includeSpans)
                regex.append("((\">\"|\"<\"\">\")\":\")?");

            // There is a foundry given
            if (foundry != null && foundry.size() > 0) {

                // Filter out bad foundries
                for (i = foundry.size() - 1; i >= 0; i--) {
                    if (!harmlessFoundry.matcher(foundry.get(i)).matches()) {
                        match.addError(970, "Invalid foundry requested",
                                foundry.get(i));
                        return match;
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
                        for (i = layer.size() - 1; i >= 0; i--) {
                            if (!harmlessLayer.matcher(layer.get(i))
                                    .matches()) {
                                throw new QueryException(
                                        "Invalid layer requested: "
                                                + layer.get(i));
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
                            regex.replace(regex.length() - 1, regex.length(),
                                    ")");
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
            for (LeafReaderContext atomic : this.reader().leaves()) {

                // Retrieve the single document of interest
                DocIdSet filterSet = filter.getDocIdSet(atomic,
                        atomic.reader().getLiveDocs());


                // Create a bitset for the correct document
                Bits bitset = filterSet.bits();

                DocIdSetIterator filterIterator = filterSet.iterator();

                if (DEBUG)
                    log.trace("Checking document in {} with {}", filterSet,
                            bitset);

                // No document found
                if (filterIterator == null)
                    continue;

                // Go to the matching doc - and remember its ID
                int localDocID = filterIterator.nextDoc();

                if (DEBUG)
                    log.trace("localDocID is {}", localDocID);

                if (localDocID == DocIdSetIterator.NO_MORE_DOCS)
                    continue;

                // We've found the correct document! Hurray!
                if (DEBUG)
                    log.trace("We've found a matching document");

                // Get terms from the document
                Terms docTerms = atomic.reader().getTermVector(localDocID,
                        field);

                HashSet<String> fields = (HashSet<String>) new Krill().getMeta()
                        .getFields().clone();

                fields.add(field);

                // Load the necessary fields of the document
                Document doc = atomic.reader().document(localDocID, fields);

                // Put some more information to the match
                PositionsToOffset pto = new PositionsToOffset(atomic, field);
                match.setPositionsToOffset(pto);
                match.setLocalDocID(localDocID);
                match.populateDocument(doc, field, fields);
                if (DEBUG)
                    log.trace("The document has the id '{}' or the sigle '{}'",
                            match.getDocID(), match.getTextSigle());

                // Todo:
                SearchContext context = match.getContext();

                // Override the normal match marking
                // to have an inner match
                match.overrideMatchPosition(match.getStartPos(),
                        match.getEndPos() - 1);


                // Search for minimal surrounding sentences
                if (extendToSentence) {
                    String element = "base/s:s";
                    int[] spanContext = match.expandContextToSpan(element);

                    if (DEBUG)
                        log.trace("Extend to sentence element '{}'", element);

                    if (spanContext[0] >= 0
                            && spanContext[0] < spanContext[1]) {
                        match.setStartPos(spanContext[0]);
                        match.setEndPos(spanContext[1]);
                        match.startMore = false;
                        match.endMore = false;
                    }
                    else {
                        match.addWarning(651, "Unable to extend context");
                    };
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
                    docs = termsEnum.docsAndPositions(null, docs,
                            DocsAndPositionsEnum.FLAG_PAYLOADS);

                    // Init document iterator
                    docs.nextDoc();

                    // Should never happen ... but hell!
                    if (docs.docID() == DocIdSetIterator.NO_MORE_DOCS)
                        continue;

                    // String representation of the term
                    String termString = termsEnum.term().utf8ToString();

                    // Iterate over all occurrences
                    for (int i = 0; i < docs.freq(); i++) {

                        // Init positions and get the current
                        int pos = docs.nextPosition();

                        // Check, if the position of the term is in the area of interest
                        if (pos >= match.getStartPos()
                                && pos < match.getEndPos()) {

                            if (DEBUG)
                                log.trace(">> {}: {}-{}-{}", termString,
                                        docs.freq(), pos, docs.getPayload());

                            BytesRef payload = docs.getPayload();

                            // Copy the payload
                            bbTerm.clear();
                            if (payload != null) {
                                bbTerm.put(payload.bytes, payload.offset,
                                        payload.length);
                            };
                            TermInfo ti = new TermInfo(termString, pos, bbTerm)
                                    .analyze();
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
                        log.trace("Add term {}/{}:{} to {}({})-{}({})",
                                t.getFoundry(), t.getLayer(), t.getValue(),
                                t.getStartChar(), t.getStartPos(),
                                t.getEndChar(), t.getEndPos());

                    if (t.getType() == "term" || t.getType() == "span")
                        match.addAnnotation(t.getStartPos(), t.getEndPos(),
                                t.getAnnotation());
                    else if (t.getType() == "relSrc")
                        match.addRelation(t.getStartPos(), t.getEndPos(),
                                t.getAnnotation());
                };

                break;
            };
        }
        catch (IOException e) {
            match.addError(600, "Unable to read index",
                    e.getLocalizedMessage());
            log.warn(e.getLocalizedMessage());
        };

        return match;
    };


    /**
     * Search in the index.
     */
    public Result search (SpanQuery query) {
        return this.search(new Krill(query));
    };


    public Result search (SpanQuery query, short count) {
        final Krill krill = new Krill(query);
        krill.getMeta().setCount(count);
        return this.search(krill);
    };


    @Deprecated
    public Result search (SpanQuery query, int startIndex, short count,
            boolean leftTokenContext, short leftContext,
            boolean rightTokenContext, short rightContext) {

        Krill ks = new Krill(query);
        KrillMeta meta = ks.getMeta();
        meta.setStartIndex(startIndex).setCount(count);
        meta.setContext(new SearchContext(leftTokenContext, leftContext,
                rightTokenContext, rightContext));
        return this.search(ks);
    };


    /**
     * Search the endpoint.
     */
    public Result search (Krill ks) {
        if (DEBUG)
            log.trace("Start search");

        this.termContexts = new HashMap<Term, TermContext>();

        final KrillCollection collection = ks.getCollection();
        collection.setIndex(this);

        // Get the spanquery from the Krill object
        SpanQuery query = ks.getSpanQuery();

        // Get the field of textual data and annotations ("tokens")
        final String field = query.getField();

        final KrillMeta meta = ks.getMeta();

        // Todo: Make kr subclassing ks - so ks has a method for a new Result!
        // Or allow passing of Krill object
        final Result kr = new Result(query.toString(), meta.getStartIndex(),
                meta.getCount(), meta.getContext());

        // Copy notification info
        kr.moveNotificationsFrom(ks);

        // Set version info to result
        if (this.getVersion() != null)
            kr.setVersion(this.getVersion());

        // The following fields should be lifted for matches
        HashSet<String> fields = (HashSet<String>) meta.getFields().clone();

        // Lift primary field
        fields.add(field);

        // Lift all fields
        if (fields.contains("@all"))
            fields = null;

        // Some initializations ...
        int i = 0;
        int startIndex = kr.getStartIndex();
        int count = kr.getItemsPerPage();
        int hits = kr.getItemsPerPage() + startIndex;
        int limit = meta.getLimit();
        int itemsPerResourceCounter = 0;
        boolean cutoff = meta.doCutOff();
        short itemsPerResource = meta.getItemsPerResource();

        // Check if there is work to do at all
        // TODO: Deprecated
        if (limit > 0) {
            if (hits > limit)
                hits = limit;

            // Nah - nothing to do! Let's go shopping!
            if (limit < startIndex)
                return kr;
        };

        // Collect matches from atomic readers
        final ArrayList<Match> atomicMatches = new ArrayList<Match>(
                kr.getItemsPerPage());

        // Start time out thread
        final TimeOutThread tthread = new TimeOutThread();
        tthread.start();
        final long timeout = meta.getTimeOut();

        // See: http://www.ibm.com/developerworks/java/library/j-benchmark1/index.html
        long t1 = System.nanoTime();

        try {
            // Rewrite query (for regex and wildcard queries)
            // Revise!
            // Based on core/src/java/org/apache/lucene/search/IndexSearcher.java
            // and highlighter/src/java/org/apache/lucene/search/postingshighlight/PostingsHighlighter.java
            for (Query rewrittenQuery = query
                    .rewrite(this.reader()); !rewrittenQuery
                            .equals(query); rewrittenQuery = query
                                    .rewrite(this.reader())) {
                query = (SpanQuery) rewrittenQuery;
            };


            // Todo: run this in a separated thread
            for (LeafReaderContext atomic : this.reader().leaves()) {

                int oldLocalDocID = -1;

                /*
                 * Todo: There may be a way to know early if the bitset is emty
                 * by using LongBitSet - but this may not be as fast as I think.
                 */
                final Bits bitset = collection.bits(atomic);
                final PositionsToOffset pto = new PositionsToOffset(atomic,
                        field);

                // Spans spans = NearSpansOrdered();
                final Spans spans = query.getSpans(atomic, (Bits) bitset,
                        termContexts);

                final IndexReader lreader = atomic.reader();
                int localDocID, docID;

                // TODO: Get document information from Cache! Fieldcache?
                for (; i < hits; i++) {

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

                    localDocID = spans.doc();

                    // Count hits per resource
                    if (itemsPerResource > 0) {

                        // IDS are identical
                        if (localDocID == oldLocalDocID
                                || oldLocalDocID == -1) {
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

                    docID = atomic.docBase + localDocID;

                    // Do not load all of this, in case the doc is the same!
                    final Document doc = (fields != null)
                            ? lreader.document(localDocID, fields)
                            : lreader.document(localDocID);

                    // Create new Match
                    final Match match = new Match(pto, localDocID,
                            spans.start(), spans.end());
                    match.setContext(kr.getContext());

                    // Add match to Result
                    kr.add(match);

                    if (spans.isPayloadAvailable())
                        match.addPayload((List<byte[]>) spans.getPayload());

                    match.internalDocID = docID;

                    // Lift certain fields
                    if (fields != null) {
                        match.populateDocument(doc, field, fields);
                    }
                    // Lift all fields
                    else {
                        match.populateDocument(doc, field);
                    };

                    if (DEBUG) {
                        if (match.getDocID() != null)
                            log.trace(
                                    "With DocID: I've got 1 match of {} in {}",
                                    count, match.getDocID());
                        else
                            log.trace("With UID: I've got 1 match of {} in {}",
                                    count, match.getUID());
                    };

                    atomicMatches.add(match);
                };

                // Can be disabled TEMPORARILY
                while (!cutoff && spans.next()) {

                    // TODO: Deprecated
                    if (limit > 0 && i >= limit)
                        break;

                    // Timeout!
                    if (tthread.getTime() > timeout) {
                        kr.setTimeExceeded(true);
                        break;
                    };

                    // Count hits per resource
                    if (itemsPerResource > 0) {
                        localDocID = spans.doc();

                        if (localDocID == DocIdSetIterator.NO_MORE_DOCS)
                            break;

                        // IDS are identical
                        if (localDocID == oldLocalDocID
                                || oldLocalDocID == -1) {
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
            kr.addError(600, "Unable to read index", e.getLocalizedMessage());
            log.warn(e.getLocalizedMessage());
        };

        // Stop timer thread
        tthread.stopTimer();

        // Calculate time
        kr.setBenchmark(t1, System.nanoTime());

        return kr;
    };


    public void getFields () {
        /*
         * Return a map of key, value pairs:
         *
         * keywords => keywords (contains)
         * author => text (contains)
         */
    };


    public void getValues (String field) {

    };


    // Collect matches
    public MatchCollector collect (Krill ks, MatchCollector mc) {
        if (DEBUG)
            log.trace("Start collecting");

        KrillCollection collection = ks.getCollection();
        collection.setIndex(this);

        // Init term context
        this.termContexts = new HashMap<Term, TermContext>();

        // Get span query
        SpanQuery query = ks.getSpanQuery();

        // Get the field of textual data and annotations
        String field = query.getField();

        // TODO: Get document information from Cache!
        // See: http://www.ibm.com/developerworks/java/library/j-benchmark1/index.html
        long t1 = System.nanoTime();

        // Only load UIDs
        HashSet<String> fields = new HashSet<>(1);
        fields.add("UID");

        // List<Match> atomicMatches = new ArrayList<Match>(10);
        try {

            // Rewrite query (for regex and wildcard queries)
            for (Query rewrittenQuery = query.rewrite(
                    this.reader()); rewrittenQuery != (Query) query; rewrittenQuery = query
                            .rewrite(this.reader())) {
                query = (SpanQuery) rewrittenQuery;
            };

            int matchcount = 0;
            String uniqueDocIDString;;
            int uniqueDocID = -1;

            // start thread:
            for (LeafReaderContext atomic : this.reader().leaves()) {

                int previousDocID = -1;
                int oldLocalDocID = -1;

                // Use LongBitSet;
                Bits bitset = collection.bits(atomic);

                // PositionsToOffset pto = new PositionsToOffset(atomic, field);

                Spans spans = query.getSpans(atomic, (Bits) bitset,
                        termContexts);

                IndexReader lreader = atomic.reader();

                while (spans.next()) {
                    int localDocID = spans.doc();

                    // New match
                    // MatchIdentifier possibly needs more
                    /*
                      Match match = new Match();
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
                        uniqueDocIDString = lreader.document(localDocID, fields)
                                .get("UID");

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
            mc.addError(600, "Unable to read index", e.getLocalizedMessage());
            log.warn(e.getLocalizedMessage());
        };

        mc.close();
        return mc;
    };
};
