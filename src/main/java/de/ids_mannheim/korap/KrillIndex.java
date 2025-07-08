package de.ids_mannheim.korap;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.time.LocalDate;
// Java core classes
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
// Lucene classes
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.FixedBitSet;
import org.apache.lucene.util.automaton.CompiledAutomaton;
import org.apache.lucene.util.automaton.RegExp;
// Log4j Logger classes
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.*;

import com.fasterxml.jackson.databind.ObjectMapper;

// Krill classes
import de.ids_mannheim.korap.index.FieldDocument;
import de.ids_mannheim.korap.index.KeywordAnalyzer;
import de.ids_mannheim.korap.index.PositionsToOffset;
import de.ids_mannheim.korap.index.SpanInfo;
import de.ids_mannheim.korap.index.TermInfo;
import de.ids_mannheim.korap.index.TextAnalyzer;
import de.ids_mannheim.korap.index.TimeOutThread;
import de.ids_mannheim.korap.response.Match;
import de.ids_mannheim.korap.response.MatchCollector;
import de.ids_mannheim.korap.response.MetaFields;
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.response.SearchContext;
import de.ids_mannheim.korap.response.Text;
import de.ids_mannheim.korap.util.Fingerprinter;
import de.ids_mannheim.korap.util.KrillDate;
import de.ids_mannheim.korap.util.KrillProperties;
import de.ids_mannheim.korap.util.QueryException;

import static com.fasterxml.jackson.core.StreamReadConstraints.DEFAULT_MAX_STRING_LEN;

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
public final class KrillIndex implements IndexInfo {

    // Logger
    private final static Logger log = LoggerFactory.getLogger(KrillIndex.class);

    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;

    // TODO: Use configuration instead.
    // Last line of defense against DOS
    // Keep in mind - upsert requires 2 commits
    // and isn't atomic
    private int autoCommit = 10000;
    private String version = "Unknown";
    private String name = "Unknown";

    private String indexRevision;
    
    // Temp:
    private IndexReader reader;

    private IndexWriter writer;
    private boolean readerOpen = false;
    private boolean writerOpen = false;
    private Directory directory;

    // The commit counter is only there for
    // counting unstaged changes per thread (for bulk insertions)
    // It does not represent real unstaged documents.
    private int commitCounter = 0;
    private HashMap termContexts;
    private ObjectMapper mapper = new ObjectMapper();

    // Prelim CacheKey
    record PrelimCacheKey(String queryStr, String collStr, short itemsPerRessource) {}

    // CacheKey
    record SearchCacheKey(PrelimCacheKey prelimck, String atomicHash) {}

    // CacheValue
    record SearchCacheValue(int matchCount, int matchDocCount) {}

    Cache<SearchCacheKey, SearchCacheValue> searchCache;
    
    // private ByteBuffer bbTerm;

    // Some initializations ...
    {
        Properties prop = KrillProperties.loadDefaultProperties();
        Properties info = KrillProperties.loadInfo();
        
        if (info != null) {
            this.version = info.getProperty("krill.version");
            this.name = info.getProperty("krill.name");
        };

        // Check for auto commit value
        String autoCommitStr = null;
        String cacheSizeStr = null;
        int cacheSize = (64 * 1024 * 1024); // 64 MB 
        if (prop != null) {
            autoCommitStr = prop.getProperty("krill.index.commit.auto");
            cacheSizeStr = prop.getProperty("krill.cache.size");
        }
        
        if (autoCommitStr != null) {
            try {
                this.autoCommit = Integer.parseInt(autoCommitStr);
            }
            catch (NumberFormatException e) {
                log.error(
                        "krill.index.commit.auto expected to be a numerical value");
            };
        };

        if (cacheSizeStr != null) {
            try {
                int retVal = Integer.parseInt(cacheSizeStr);
                cacheSize = retVal;
            } catch (NumberFormatException e) {
                log.warn("krill.cache.size expected to be a numerical value");
            }
        };

        searchCache = Caffeine.newBuilder()
            .maximumWeight(cacheSize)
            .weigher((SearchCacheKey key, SearchCacheValue value) -> 80) // estimate per-entry size
            .build();
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
    };


    /**
     * Constructs a new KrillIndex bound to a persistant index,
     * that will be lifted as an mmap.
     * 
     * @param path
     *            A path pointing to an mmapable index
     * @throws IOException
     */
    public KrillIndex (Path path) throws IOException {
        this.directory = new MMapDirectory(path);
    };
    
    /**
     * Get the version number of the index.
     * 
     * @return A string containing the version number.
     */
    public String getVersion () {
        return this.version;
    };

    public void setMaxStringLength(int maxStringLength) {
        if (maxStringLength < DEFAULT_MAX_STRING_LEN) {
            throw new IllegalArgumentException("Maximum string length must not be smaller than the default value: "
                    + DEFAULT_MAX_STRING_LEN);
        }

        StreamReadConstraints constraints = StreamReadConstraints.builder()
                .maxStringLength(maxStringLength)
                .build();

        JsonFactory factory = JsonFactory.builder()
                .streamReadConstraints(constraints)
                .build();

        this.mapper = new ObjectMapper(factory);
        log.info("Maximum string length set to {}.", maxStringLength);
    }

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
        if (!readerOpen)
            return null;

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
        if (!writerOpen)
            this.openWriter();
        if (!writerOpen)
            return null;
        return this.writer;
    };
    

    // Open index reader
    private void openWriter () {
        if (writerOpen) {
            return;
        };

        try {

            // Add analyzers
            // This is just for legacy reasons
            Map<String, Analyzer> analyzerPerField = new HashMap<String, Analyzer>();
            analyzerPerField.put("textClass", new KeywordAnalyzer());
            analyzerPerField.put("keywords", new KeywordAnalyzer());
            analyzerPerField.put("foundries", new KeywordAnalyzer());
            PerFieldAnalyzerWrapper analyzer = new PerFieldAnalyzerWrapper(
                new TextAnalyzer(), analyzerPerField);

            // Create configuration with base analyzer
            this.writer = new IndexWriter(this.directory, new IndexWriterConfig(analyzer));
            writerOpen = true;
        }

        // Failed to open writer
        catch (IOException e) {
            // e.printStackTrace();
            log.warn(e.getLocalizedMessage());
        };
    };


    // Open index reader
    private void openReader () {
        if (readerOpen) {
            return;
        };

        try {
            // open reader
            this.reader = DirectoryReader.open(this.directory);
            readerOpen = true;
        }

        // Failed to open reader
        catch (IOException e) {
            // This is in tests most of the time
            // no problem, because the message just says
            // "No segments found", because the reader
            // is empty initially.
            log.warn(e.getLocalizedMessage());
        };
    };


    // Close index reader
    public void closeReader () throws IOException {
        if (readerOpen || this.reader != null) {
            this.reader.close();
            this.reader = null;
            readerOpen = false;
        };
    };


    // Close index writer
    public void closeWriter () throws IOException {
        if (writerOpen || this.writer != null) {
            this.writer.close();
            this.writer = null;
            writerOpen = false;
        };
    };


    /**
     * Close the associated {@link IndexReader} and the associated
     * {@link IndexWriter},
     * in case they are opened.
     * 
     * @throws IOException
     */
    public void close () throws IOException {
        this.closeWriter();
        this.closeReader();
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
        log.info("Internal committing index ... ");
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
     * Update a document in the index as a {@link FieldDocument}
     * if it already exists (based on the textSigle), otherwise
     * insert it to the index.
     * 
     * @param doc
     *            The {@link FieldDocument} to add to the index.
     * @return The {@link FieldDocument}, which means, the same
     *         object, that was passed to the method.
     */
    public FieldDocument upsertDoc (FieldDocument doc) {
        if (doc == null)
            return doc;

        // Create a filter based on the corpusID and the docID
        String textSigle = doc.getTextSigle();
        KrillDate current = new KrillDate(LocalDate.now());
        KrillDate indexCreationDate = current;
        KrillDate indexLastModified = current;
               
        // Delete the document if exists
        if (textSigle != null) {

            // First find the document
            Filter filter = (Filter) new QueryWrapperFilter(
                new TermQuery(new Term("textSigle", textSigle))
                );
            
            try {
                // Iterate over all atomic indices and find the matching document

            UPSERT:
                while (true) {

                    if (this.reader() != null) {
                
                        for (LeafReaderContext atomic : this.reader().leaves()) {
                        
                            // The reader is closed
                            /*
                            if (this.reader.getRefCount() == 0) {

                                // Retry update
                                break;
                            };
                            */
                        
                            // Retrieve the single document of interest
                            DocIdSet filterSet = filter.getDocIdSet(
                                atomic,
                                atomic.reader().getLiveDocs());
                        
                            DocIdSetIterator filterIterator = filterSet.iterator();

                            if (filterIterator == null) {
                                continue;
                            };
                    
                            // Go to the matching doc - and remember its ID
                            int localDocID = filterIterator.nextDoc();

                            if (localDocID == DocIdSetIterator.NO_MORE_DOCS) {
                                continue;
                            };

                            // We've found the correct document! Hurray!
                            if (DEBUG)
                                log.trace("We've found a matching document");
                            
                            // TODO: Probably use
                            // document(int docID, StoredFieldVisitor visitor)
                            Document storedDoc = atomic.reader().document(localDocID);

                            // Document is loadable
                            if (storedDoc != null) {
                                IndexableField indexCreationField =
                                    storedDoc.getField("indexCreationDate");
                        
                                if (indexCreationField == null) {
                                    indexCreationDate = current;
                                }
                                else {
                                    indexCreationDate = new KrillDate(
                                        indexCreationField.numericValue().toString()
                                        );
                                };
                            };

                            this.delDocs("textSigle", textSigle);
                            break UPSERT;
                        };
                    }
                    else {
                        log.warn("Reader is null");
                    };
                    break;
                };
            }

            catch (IOException e) {
                log.error("Unable to upsert document");
            };
        };

        doc.addDate("indexCreationDate", indexCreationDate.toDisplay());
        doc.addDate("indexLastModified", indexLastModified.toDisplay());

        return this.addDoc(doc);
    };


    /**
     * Update a document in the index as a {@link FieldDocument}
     * if it already exists (based on the textSigle), otherwise
     * insert it to the index.
     * 
     * @param json
     *            The JSON document to add to the index.
     * @return The {@link FieldDocument}.
     */
    public FieldDocument upsertDoc (InputStream json, boolean gzip) {
        return this.upsertDoc(_fromFile(json, gzip));
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
            this.writer().addDocument(doc.compile());
            if (++commitCounter > autoCommit) {
                this.commit();
                commitCounter = 0;
            };
            this.indexRevision = null;
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

            this.indexRevision = null;
            return true;
        }

        // Failed to delete document
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

                GZIPInputStream gzipFile = new GZIPInputStream(json);

                // Create json field document
                FieldDocument fd = this.mapper.readValue(
                        gzipFile, FieldDocument.class);
                gzipFile.close();
                return fd;
            };
            FieldDocument field = this.mapper.readValue(json, FieldDocument.class);
            json.close();
            return field;
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


    public Text getDoc (String uid) {
        // This is very similar to getMatchInfo

        Text text = new Text();

        // Rewrite parse ID
        uid = Integer.valueOf(Integer.parseInt(uid)).toString();

        Filter filter = (Filter) new QueryWrapperFilter(
            new TermQuery(new Term("UID", uid)));

        try {

            // Iterate over all atomic indices and find the matching document
            for (LeafReaderContext atomic : this.reader().leaves()) {

                // Retrieve the single document of interest
                DocIdSet filterSet = filter.getDocIdSet(atomic,
                        atomic.reader().getLiveDocs());

                DocIdSetIterator filterIterator = filterSet.iterator();

                if (DEBUG) {
					// Create a bitset for the correct document
					Bits bitset = filterSet.bits();
					
                    log.trace("Checking document in {} with {}", filterSet,
							  bitset);
				};

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

    public Match getMatchInfo (String idString, String field, boolean info,
                               List<String> foundry, List<String> layer, boolean includeSpans,
                               boolean includeHighlights, boolean extendToSentence)
        throws QueryException {
        return getMatchInfo(
            idString, field, info,
            foundry, layer, includeSpans,
            true, // include Snippets
            false, // include Tokens
            includeHighlights, extendToSentence
            );
    }

    /**
     * Get a match.
     */
    /*
      KorapInfo is associated with a Match and has an array with all informations
      per position in the match.
    */
    public Match getMatchInfo (String idString, String field, boolean info,
            List<String> foundry, List<String> layer, boolean includeSpans,
                               boolean includeSnippets, boolean includeTokens,
                               boolean includeHighlights, boolean extendToSentence)
            throws QueryException {

        if (DEBUG)
            log.trace("Get info on {}", idString);
        
        int maxTokenMatchSize = KrillProperties.maxTokenMatchSize;
        Match match = new Match(maxTokenMatchSize, idString, includeHighlights);

        if (this.getVersion() != null)
            match.setVersion(this.getVersion());

        if (this.getName() != null)
            match.setName(this.getName());

        if (match.getStartPos() == -1)
            return match;

        if (includeTokens)
            match.hasTokens = true;

        if (includeSnippets) {
            match.hasSnippet = true;
        } else {
            includeHighlights = false;
            includeSpans = false;
            info = false;
        };
        
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
        ByteBuffer bbTerm = ByteBuffer.allocate(32);

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

                DocIdSetIterator filterIterator = filterSet.iterator();

                if (DEBUG) {
					// Create a bitset for the correct document
					Bits bitset = filterSet.bits();

                    log.trace("Checking document in {} with {}", filterSet,
							  bitset);
				};

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

				// The following fields should be lifted for the match
                List<String> fields = (ArrayList<String>) new Krill().getMeta()
                    .getFields().clone();

				// Lift all fields
				if (fields.contains("@all"))
					fields = null;

                HashSet<String> fieldsSet = new HashSet<String>(fields);

				// Lift primary field
                fieldsSet.add(field);
                
                // Load the necessary fields of the document
                Document doc = (fields != null)
                    ? atomic.reader().document(localDocID, fieldsSet)
                    : atomic.reader().document(localDocID);

                // Put some more information to the match
                PositionsToOffset pto = new PositionsToOffset(atomic, field);
                match.setPositionsToOffset(pto);
                match.setLocalDocID(localDocID);
                match.populateDocument(doc, field, (List<String>) fields);
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
                    match.expandContextToSpan(element);

                    if (DEBUG)
                        log.trace("Extend to sentence element '{}'", element);
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
                                log.trace(">> {}: freq:{}, pos:{}, payload:{}",
										  termString,
										  docs.freq(),
										  pos,
										  docs.getPayload());

                            BytesRef payload = docs.getPayload();

                            // Copy the payload
                            bbTerm.clear();

                            if (payload != null && payload.length <= bbTerm.capacity()) {
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
                        log.trace(
							"Add term {}/{}:{} with char:{}(pos:{})-char:{}(pos:{})",
							t.getFoundry(), t.getLayer(), t.getValue(),
							t.getStartChar(), t.getStartPos(),
							t.getEndChar(), t.getEndPos());


					// Ignore empty types for the moment
                    if (t.getType().equals("term") || t.getType().equals("span")) {
                        match.addAnnotation(t.getStartPos(), t.getEndPos(),
                                t.getAnnotation());
					}

					// TODO:
					// else if (t.getType().equals("empty")) {
					// }

					// Use relSrc for annotation views
					else if (t.getType().equals("relSrc")) {
						// This only respects relSrc!
						// May require more information for bidirectional relations
                        match.addRelation(
							t.getStartPos(),
							t.getEndPos(),
							t.getTargetStartPos(),
							t.getTargetEndPos(),
							t.getAnnotation()
							);
					};
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
        final Krill krill = new Krill(query);
        krill.getMeta().setSnippets(true);
        return this.search(krill);
    };


    public Result search (SpanQuery query, short count) {
        final Krill krill = new Krill(query);
        krill.getMeta().setCount(count);
        krill.getMeta().setSnippets(true);
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
        meta.setSnippets(true);
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
        List<String> fields = (ArrayList<String>) meta.getFields().clone();
        HashSet<String> fieldsSet = new HashSet<String>(fields);
        boolean snippets = meta.hasSnippets() || meta.hasTokens();

        // Lift all fields
        if (fields.contains("@all")) {
            fields = null;
        }
        else  {
            // Lift primary field
            fieldsSet.add(field);
        };
        
        // Some initializations ...
        int i = 0; // matchcount
        int j = 0; // matchdoccount
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

        if (cutoff && count == 0) {
            kr.setTotalResults(-1);
            kr.setTotalResources(-1);
            return kr;
        };

        // Collect matches from atomic readers
        final ArrayList<Match> atomicMatches = new ArrayList<Match>(
                kr.getItemsPerPage());

        // Start time out thread
        final TimeOutThread tthread = new TimeOutThread();
        tthread.start();
        final long timeout = meta.getTimeOut();
        boolean isTimeout = false;
       
        // See: http://www.ibm.com/developerworks/java/library/j-benchmark1/index.html
        long t1 = System.nanoTime();

        int fromCache = 0;

        try {
            // Rewrite query (for regex and wildcard queries)
            // Revise!
            // Based on core/src/java/org/apache/lucene/search/IndexSearcher.java
            // and highlighter/src/java/org/apache/lucene/search/
			//   postingshighlight/PostingsHighlighter.java
            for (Query rewrittenQuery = query.rewrite(this.reader());
				 !rewrittenQuery.equals(query);
				 rewrittenQuery = query.rewrite(this.reader())) {
                query = (SpanQuery) rewrittenQuery;
            };

			if (DEBUG)
				log.trace("Rewritten query is {}", query.toString());

            String collStr = "";
            Filter collf = collection.toFilter();
            if (collf != null) {
                collStr = collf.toString();
            };

            PrelimCacheKey prelim = new PrelimCacheKey(query.toString(), collStr, itemsPerResource);
            
            // Todo: run this in a separated thread
            for (LeafReaderContext atomic : this.reader().leaves()) {

                int oldLocalDocID = -1;

                if (isTimeout)
                    break;

                SearchCacheKey finalCacheKey = new SearchCacheKey(prelim, atomic.reader().getCombinedCoreAndDeletesKey().toString());
                SearchCacheValue foundCache = searchCache.getIfPresent(finalCacheKey);
                
                if (foundCache != null) {
                    if (DEBUG) {
                        log.trace(
                            "Found cache for Query: {}, Collection: {}, itemsPerRessource: {}, Reader: {}",
                            query.toString(), collStr, itemsPerResource, atomic.reader().getCombinedCoreAndDeletesKey().toString()
                            );
                    };

                    if (startIndex > (i + foundCache.matchCount)) {
                        fromCache += foundCache.matchCount;
                        i += foundCache.matchCount;
                        j += foundCache.matchDocCount;
                        continue;
                    };
                } else if (DEBUG) {
                    log.trace(
                        "Found no cache for Query: {}, Collection: {}, itemsPerRessource: {}, Reader: {}",
                        query.toString(), collStr, itemsPerResource, atomic.reader().getCombinedCoreAndDeletesKey().toString()
                        );
                } 

                /*
                 * Todo: There may be a way to know early if the bitset is emty
                 * by using LongBitSet - but this may not be as fast as I think.
                 */
                final FixedBitSet bitset = collection.bits(atomic);

				if (bitset.nextSetBit(0) == DocIdSetIterator.NO_MORE_DOCS) {
                    if (foundCache == null)
                        searchCache.put(
                            finalCacheKey,
                            new SearchCacheValue(0, 0)
                            );
                    
                    if (DEBUG) {
                        log.trace(
                            "Store cache (1) for Query: {}, Collection: {}, itemsPerRessource: {}, Reader: {}, store:0/0",
                            query.toString(),
                            collStr,
                            itemsPerResource,
                            atomic.reader().getCombinedCoreAndDeletesKey().toString()
                            );
                    };
					continue;
                };

                final PositionsToOffset pto = snippets ? new PositionsToOffset(atomic, field) : null;
				
                // Spans spans = NearSpansOrdered();
                final Spans spans = query.getSpans(atomic, (Bits) bitset,
                        termContexts);

                final IndexReader lreader = atomic.reader();
                int localDocID, docID;

                int li = i;
                int lj = j;
                
                // TODO: Get document information from Cache! Fieldcache?
                for (; i < hits; i++) {

                    if (DEBUG)
                        log.trace("Match Nr {}/{}", i, count);
                   
                    // There are no more spans to find
                    if (!spans.next()) {
                        if (foundCache == null)
                            foundCache = new SearchCacheValue(i - li, j - lj);
                            searchCache.put(
                                finalCacheKey,
                                foundCache
                                );

                        if (DEBUG) {
                            log.trace(
                                "Store cache (2) for Query: {}, Collection: {}, itemsPerRessource: {}, Reader: {}, store:{}/{}",
                                query.toString(),
                                collStr,
                                itemsPerResource,
                                atomic.reader().getCombinedCoreAndDeletesKey().toString(),
                                i - li,
                                j - lj
                                );
                        };
                        break;
                    };
                    
                    // Increment resource counter
                    itemsPerResourceCounter++;
                    
                    // Timeout!
                    if (tthread.getTime() > timeout) {
                        kr.setTimeExceeded(true);
                        isTimeout=true;
                        break;
                    };

                    localDocID = spans.doc();

                    // IDS are identical
                    if (localDocID == oldLocalDocID
                        || oldLocalDocID == -1) {

                        // Count hits per resource
                        if (itemsPerResource > 0) {
                            
                            // End of resourcecounter is reached
                            if (itemsPerResourceCounter > itemsPerResource) {

                                // Skip to next resource
                                if (spans.skipTo(localDocID + 1) != true) {
                                    break;
                                }

                                itemsPerResourceCounter = 1;
                                localDocID = spans.doc();
                            };
                        }
                    }

                    // localDoc is new
                    else
                        itemsPerResourceCounter = 1;


                    if (itemsPerResourceCounter == 1)
                        j++;

                    oldLocalDocID = localDocID;
                    
                    
                    // The next matches are not yet part of the result
                    if (startIndex > i)
                        continue;

                    docID = atomic.docBase + localDocID;

                    // Do not load all of this, in case the doc is the same!
                    final Document doc = (fields != null)
                            ? lreader.document(localDocID, fieldsSet)
                            : lreader.document(localDocID);
                    
                    int maxMatchSize = ks.getMaxTokenMatchSize();
                    if (maxMatchSize <= 0
                            || maxMatchSize > KrillProperties.maxTokenMatchSize) {
                        maxMatchSize = KrillProperties.maxTokenMatchSize;
                    };
                    
                    // Create new Match
                    final Match match = new Match(maxMatchSize, pto, localDocID,
                            spans.start(), spans.end());
                    
                    // Add snippet if existing
                    if (snippets) {
                        match.setContext(kr.getContext());
                        match.retrieveMarkers("~:base/s:pb");
                        match.retrieveMarkers("~:base/s:marker");

                        if (DEBUG)
                            log.trace("Retrieve pagebreaks from index");

                        if (spans.isPayloadAvailable())
                            match.addPayload((List<byte[]>) spans.getPayload());
                        
                        if (meta.hasSnippets()) {
                            match.hasSnippet = true;
                        };
                        
                        if (meta.hasTokens()) {
                            match.hasTokens = true;
                        };
                    };

                    // Add match to Result
                    kr.add(match);
                    
                    match.internalDocID = docID;
                    
                    // Lift certain fields
                    if (fields != null) {
                        match.populateDocument(doc, snippets ? field : null, fields);
                    }
                    // Lift all fields
                    else {
                        match.populateDocument(doc, snippets ? field : null);
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
                while (!cutoff && !isTimeout && spans.next()) {

                    // TODO: Deprecated
                    if (limit > 0 && i >= limit)
                        break;

                    // Timeout!
                    if (tthread.getTime() > timeout) {
                        kr.setTimeExceeded(true);
                        isTimeout=true;
                        break;
                    };

                    // Increment resource counter
                    itemsPerResourceCounter++;
                    
                    localDocID = spans.doc();

                    if (localDocID == DocIdSetIterator.NO_MORE_DOCS)
                        break;

                    // IDS are identical
                    if (localDocID == oldLocalDocID
                        || oldLocalDocID == -1) {
                                
                        if (localDocID == -1)
                            break;
                        
                        // Count hits per resource
                        if (itemsPerResource > 0) {

                            // End of resourcecounter is reached
                            if (itemsPerResourceCounter > itemsPerResource) {
                                if (spans.skipTo(localDocID + 1) != true) {
                                    break;
                                };
                                itemsPerResourceCounter = 1;
                                localDocID = spans.doc();
                            };
                        }
                    }
                    // Reset counter
                    else
                        itemsPerResourceCounter = 1;

                    if (itemsPerResourceCounter == 1)
                        j++;
                    
                    oldLocalDocID = localDocID;
                    i++;
                };

                if (!isTimeout && !cutoff) {
                    if (foundCache == null) {
                        searchCache.put(
                            finalCacheKey,
                            new SearchCacheValue(i - li, j -lj)
                            );
                    
                        if (DEBUG) {
                            log.trace(
                                "Store cache (3) for Query: {}, Collection: {}, itemsPerRessource: {}, Reader: {}, store:{}/{}",
                                query.toString(),
                                collStr,
                                itemsPerResource,
                                atomic.reader().getCombinedCoreAndDeletesKey().toString(),
                                i - li,
                                j - lj
                                );
                        };
                    };
                };
                
                atomicMatches.clear();
            };

            if (itemsPerResource > 0)
                kr.setItemsPerResource(itemsPerResource);

            kr.setTotalResults(cutoff ? (long) -1 : (long) i);
            kr.setTotalResources(cutoff ? (long) -1 : (long) j);
        }

        catch (IOException e) {
            kr.addError(600, "Unable to read index", e.getLocalizedMessage());
            log.warn(e.getLocalizedMessage());
		}

		catch (QueryException e) {
            kr.addError(e.getErrorCode(),e.getLocalizedMessage());
            log.warn(e.getLocalizedMessage());          
        }
        catch (IllegalArgumentException e) {
            // 104 ILLEGAL_ARGUMENT, see Kustvakt core
            // de.ids_mannheim.korap.exceptions.StatusCodes.ILLEGAL_ARGUMENT
            kr.addError(104,e.getLocalizedMessage());
            log.warn(e.getMessage());
        }
        catch (Exception e) {
            // 100 GENERAL ERROR, see Kustvakt core StatusCodes
            kr.addError(100,e.getMessage());
            log.error(e.getMessage());
            e.printStackTrace();
        }

        if (fromCache > 0)
            kr.addMessage(0, "Some results were cached", String.valueOf(fromCache));

        // Stop timer thread
        tthread.stopTimer();

        // Calculate time
        kr.setBenchmark(t1, System.nanoTime());

        return kr;
    };

    public MetaFields getFields (String textSigle) {

        List hs = new ArrayList<String>();
        hs.add("@all");
        return this.getFields(textSigle, hs);
    };


	// Return field values
    public MetaFields getFields (String textSigle, List<String> fields) {

		// Create TermQuery for document
		TermQuery textSigleQuery = new TermQuery(new Term("textSigle", textSigle));

		Filter filter = (Filter) new QueryWrapperFilter(textSigleQuery);

		if (fields.contains("@all"))
			fields = null;

		MetaFields metaFields = new MetaFields(textSigle);

        try {

            // Iterate over all atomic indices and find the matching document
            for (LeafReaderContext atomic : this.reader().leaves()) {

				// Retrieve the single document of interest
                DocIdSet filterSet = filter.getDocIdSet(atomic, atomic.reader().getLiveDocs());

                DocIdSetIterator filterIterator = filterSet.iterator();

				// No document found
                if (filterIterator == null)
                    continue;


                // Go to the matching doc - and remember its ID
                int localDocID = filterIterator.nextDoc();

                if (localDocID == DocIdSetIterator.NO_MORE_DOCS)
                    continue;

                Document doc = atomic.reader().document(localDocID);
                if (fields == null)
                    metaFields.populateFields(doc);
                else
                    metaFields.populateFields(doc, fields);

				return metaFields;
			};
		}
		catch  (IOException e) {
            metaFields.addError(600, "Unable to read index", e.getLocalizedMessage());
            log.warn(e.getLocalizedMessage());
        };

        metaFields.addError(630, "Document not found");

		return metaFields;
    };


    public void getValues (String field) {
            
    };


	/**
     * Return a fingerprint of the current state of the index.
     * Contains information about the number of segments, docs per segment
     * and deletions per segment.
     */
    public String getFingerprint () {

        // indexRevision is cached
        if (this.indexRevision != null) {
            return this.indexRevision;
        };

        // Reader is empty
        if (this.reader() == null) {
            return "null";
        }
        
        String hash = this.reader().getCombinedCoreAndDeletesKey().toString();
        this.indexRevision = Fingerprinter.create(hash);

        return this.indexRevision;
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
		}
		catch (QueryException e) {
            mc.addError(e.getErrorCode(),e.getLocalizedMessage());
            log.warn(e.getLocalizedMessage());			
		};

        mc.close();
        return mc;
    };
    
    public boolean isReaderOpen () {
        return readerOpen;
    }


    @Override
    public Set<String> getAllLeafFingerprints () {
        List<LeafReaderContext> leaves = this.reader().leaves();
        Set<String> fingerprints = new HashSet<>(leaves.size() * 2);
        for (LeafReaderContext context : leaves) {
            String fp = Fingerprinter.create(
                    context.reader().getCombinedCoreAndDeletesKey().toString());
            fingerprints.add(fp);
        }
        return fingerprints;
    }


    // Return a vector representation of all
    // different values for a certain field.
    // This is a simplified "group" API and should in the future be
    // succeeded by group.
    public List<String> getFieldVector (String field, KrillCollection collection) {
        collection.setIndex(this);
       
        List fieldValues = new ArrayList<String>();
        String fieldValue;

        // Do not return fieldValues for token fields
        if (field.equals("tokens") || field.equals("base")) {
            return fieldValues;
        };

        
        try {
            final Filter filter = collection.toFilter();

            // Get from filtered index
            if (filter != null) {
            
                // Iterate over all atomic readers and collect occurrences
                for (LeafReaderContext atomic : this.reader().leaves()) {

                    LeafReader lreader = atomic.reader();

                    DocIdSet docids = filter.getDocIdSet(atomic, null);
                
                    DocIdSetIterator docs = (docids == null) ? null : docids.iterator();

                    if (docs == null)
                        continue;
                
                    while (docs.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
                        fieldValue = lreader.document(docs.docID()).get(field);
                        if (fieldValue != null && fieldValue != "")
                            fieldValues.add(fieldValue);
                    };
                    
                }
            } else { // Get from unfiltered index

                // Iterate over all atomic readers and collect occurrences
                for (LeafReaderContext atomic : this.reader().leaves()) {

                    LeafReader lreader = atomic.reader();
                    Bits live = lreader.getLiveDocs();

                    for (int i=0; i<lreader.maxDoc(); i++) {
                        if (live != null && !live.get(i))
                            continue;
                        
                        Document doc = lreader.document(i);
                        fieldValue = doc.get(field);
                        if (fieldValue != null && fieldValue != "")
                            fieldValues.add(fieldValue);
                    };
                };
            };
        }

        // Something went wrong
        catch (IOException e) {
            log.warn(e.getLocalizedMessage());
		}

        // E.g. reference corpus not found
        catch (QueryException e) {
            log.warn(e.getLocalizedMessage());
        };

        return fieldValues;
    };
};
