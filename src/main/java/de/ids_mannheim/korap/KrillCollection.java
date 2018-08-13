package de.ids_mannheim.korap;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.BitsFilteredDocIdSet;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Filter;
import org.apache.lucene.util.BitDocIdSet;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.FixedBitSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.ids_mannheim.korap.collection.CachedVCData;
import de.ids_mannheim.korap.collection.CollectionBuilder;
import de.ids_mannheim.korap.collection.DocBits;
import de.ids_mannheim.korap.response.Notifications;
import de.ids_mannheim.korap.util.KrillProperties;
import de.ids_mannheim.korap.util.QueryException;
import de.ids_mannheim.korap.util.StatusCodes;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

/**
 * Create a Virtual Collection of documents by means of a KoralQuery
 * collection object.
 * 
 * <blockquote><pre>
 * KrillCollection kc = new KrillCollection(json);
 * </pre></blockquote>
 * 
 * @author diewald
 */
/*
 * TODO: Make a cache for the bits
 *       Delete it in case of an extension or a filter
 * TODO: Maybe use randomaccessfilterstrategy
 * TODO: Maybe a constantScoreQuery can make things faster?
 * See http://mail-archives.apache.org/mod_mbox/lucene-java-user/
 *     200805.mbox/%3C17080852.post@talk.nabble.com%3E
 */
public final class KrillCollection extends Notifications {
    private KrillIndex index;
    private JsonNode json;
    private CollectionBuilder cb = new CollectionBuilder();
    private CollectionBuilder.Interface cbi;
    private byte[] pl = new byte[4];
    // private static ByteBuffer bb = ByteBuffer.allocate(4);

    // Logger
    private final static Logger log =
            LoggerFactory.getLogger(KrillCollection.class);

    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;

    public final static CacheManager cacheManager = CacheManager.newInstance();
    public final static Cache cache = cacheManager.getCache("named_vc");

    /**
     * Construct a new KrillCollection.
     * 
     */
    public KrillCollection () {};


    /**
     * Construct a new KrillCollection by passing a KrillIndex.
     * 
     * @param index
     *            The {@link KrillIndex} object.
     */
    public KrillCollection (KrillIndex index) {
        this.index = index;
    };

    /**
     * Construct a new KrillCollection by passing a KoralQuery.
     * 
     * @param json
     *            The KoralQuery document as a JSON string.
     */
    public KrillCollection (String jsonString) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode json = mapper.readTree(jsonString);

            if (json.has("errors") && json.get("errors").size() > 0) {
                this.addError(StatusCodes.INVALID_QUERY, "Json has errors.");
            }
            else if (json.has("collection")) {
                this.fromKoral(json.get("collection"));
            }
            else if (json.has("collections")) {
                this.addError(899,
                        "Collections are not supported anymore in favour of a single collection");
            }
            else {
                this.addError(StatusCodes.MISSING_COLLECTION,
                        "Collection is not found.");
                this.fromBuilder(this.build().nothing());
            }
        }

        // Query Exception
        catch (QueryException qe) {
            this.addError(qe.getErrorCode(), qe.getMessage());
            this.fromBuilder(this.build().nothing());
        }

        // JSON exception
        catch (IOException e) {
            this.addError(621, "Unable to parse JSON", "KrillCollection",
                    e.getLocalizedMessage());
            this.fromBuilder(this.build().nothing());
        };
    };


    /**
     * Set the {@link KrillIndex} the virtual collection refers to.
     * 
     * @param index
     *            The {@link KrillIndex} the virtual collection refers
     *            to.
     */
    public void setIndex (KrillIndex index) {
        this.index = index;
    };


    /**
     * Import the "collection" part of a KoralQuery.
     * 
     * @param jsonString
     *            The "collection" part of a KoralQuery.
     * @throws QueryException
     */
    public KrillCollection fromKoral (String jsonString) throws QueryException {
        ObjectMapper mapper = new ObjectMapper();
        try {
            this.fromKoral((JsonNode) mapper.readTree(jsonString));
        }
        catch (Exception e) {
            this.addError(621, "Unable to parse JSON", "KrillCollection");
            this.fromBuilder(this.build().nothing());
        };

        return this;
    };


    /**
     * Import the "collection" part of a KoralQuery.
     * 
     * @param json
     *            The "collection" part of a KoralQuery
     *            as a {@link JsonNode} object.
     * @throws QueryException
     */
    public KrillCollection fromKoral (JsonNode json) throws QueryException {
        this.json = json;
        return this.fromBuilder(this._fromKoral(json));
    };


    // Create collection from KoralQuery
    private CollectionBuilder.Interface _fromKoral (JsonNode json)
            throws QueryException {

        if (!json.has("@type")) {
            throw new QueryException(701,
                    "JSON-LD group has no @type attribute");
        };

        String type = json.get("@type").asText();

        if (type.equals("koral:doc")) {

            // default key
            String key = "tokens";
            String valtype = "type:string";
            String match = "match:eq";

            if (json.has("key")) key = json.get("key").asText();

            if (json.has("type")) valtype = json.get("type").asText();

            // Filter based on date
            if (valtype.equals("type:date")) {

                if (!json.has("value"))
                    throw new QueryException(820, "Dates require value fields");

                String dateStr = json.get("value").asText();

                if (json.has("match")) match = json.get("match").asText();

                // TODO: This isn't stable yet
                switch (match) {
                    case "match:eq":
                        return this.cb.date(key, dateStr);
                    case "match:ne":
                        return this.cb.date(key, dateStr).not();
                    case "match:geq":
                        return this.cb.since(key, dateStr);
                    case "match:leq":
                        return this.cb.till(key, dateStr);
                };

                throw new QueryException(841,
                        "Match relation unknown for type");
            }

            // Filter based on string
            else if (valtype.equals("type:string")) {
                if (json.get("value").size() > 1){
                    if (json.has("match")) {
                        match = json.get("match").asText();
                    }

                    CollectionBuilder.Group group = null;
                    if (match.equals("match:eq")) {
                        group = this.cb.orGroup();
                        for (JsonNode value : json.get("value")) {
                            group.with(cb.term(key, value.asText()));
                        }
                    }
                    else if (match.equals("match:ne")) {
                        group = this.cb.andGroup();
                        for (JsonNode value : json.get("value")) {
                            group.with(cb.term(key, value.asText()).not());
                        }
                    }
                    return group;
                }
                
                if (json.has("match")) match = json.get("match").asText();

                switch (match) {

                    case "match:eq":
                        return this.cb.term(key, json.get("value").asText());
                    case "match:ne":
                        return this.cb.term(key, json.get("value").asText())
                                .not();

                    // Contains and containsnot (or excludes) is only
                    // effective on text fields and ineffective on
                    // string fields
                    case "match:contains":
                        return this.cb.text(key, json.get("value").asText());

                    case "match:containsnot":
                        return this.cb.text(key, json.get("value").asText())
                                .not();

                    // <LEGACY>
                    case "match:excludes":
                        return this.cb.text(key, json.get("value").asText())
                                .not();
                    // </LEGACY>
                };

                throw new QueryException(841,
                        "Match relation unknown for type");
            }

            // Filter based on regex
            else if (valtype.equals("type:regex")) {

                if (json.has("match")) match = json.get("match").asText();

                if (match.equals("match:eq")) {
                    return this.cb.re(key, json.get("value").asText());
                }
                else if (match.equals("match:ne")) {
                    return this.cb.re(key, json.get("value").asText()).not();
                }

                // Contains and containsnot (or excludes) is
                // identical to eq and ne in case of regexes for the
                // moment,
                // though it may be beneficial to circumfix these
                // with .*
                else if (match.equals("match:contains")) {
                    return this.cb.re(key, json.get("value").asText());
                }
                else if (match.equals("match:containsnot")) {
                    return this.cb.re(key, json.get("value").asText());
                }
                // <LEGACY>
                else if (match.equals("match:excludes")) {
                    return this.cb.re(key, json.get("value").asText()).not();
                };
                // </LEGACY>

                throw new QueryException(841,
                        "Match relation unknown for type");
            }

            throw new QueryException(843, "Document type is not supported");
        }

        // nested group
        else if (type.equals("koral:docGroup"))

        {

            if (!json.has("operands") || !json.get("operands").isArray())
                throw new QueryException(842,
                        "Document group needs operand list");

            CollectionBuilder.Group group;

            String operation = "operation:and";
            if (json.has("operation"))
                operation = json.get("operation").asText();

            if (operation.equals("operation:or"))
                group = this.cb.orGroup();
            else if (operation.equals("operation:and"))
                group = this.cb.andGroup();
            else
                throw new QueryException(810,
                        "Unknown document group operation");

            for (JsonNode operand : json.get("operands")) {
                group.with(this._fromKoral(operand));
            };
            return group;
        }
        // vc reference
        else if (type.equals("koral:docGroupRef")) {
            if (!json.has("ref")) {
                throw new QueryException(StatusCodes.MISSING_VC_REFERENCE,
                        "ref is not found");
            }

            String ref = json.get("ref").asText();
            if (ref.isEmpty()) {
                throw new QueryException(StatusCodes.MISSING_VC_REFERENCE,
                        "ref is empty");
            }

            Element element = KrillCollection.cache.get(ref);
            if (element == null) {
                String corpusQuery = loadVCFile(ref);
                if (corpusQuery == null){
                    return this.build().nothing();
                }
                else{
                    JsonNode node;
                    try {
                        node = mapper.readTree(corpusQuery);
                    }
                    catch (IOException e) {
                        throw new QueryException(StatusCodes.INVALID_QUERY, 
                                "Failed parsing collection query to JsonNode.");
                    }
                    if (!node.has("collection")){
                        this.addError(StatusCodes.MISSING_COLLECTION,
                                "KoralQuery does not contain a collection.");
                        return this.build().nothing();
                    }
                    return cb.toCacheVC(ref, this._fromKoral(node.at("/collection")));
                }
            }
            else {
                CachedVCData cc = (CachedVCData) element.getObjectValue();
                return cb.namedVC(cc);
            }
        }


        // Unknown type
        throw new QueryException(813, "Collection type is not supported");
    };

    
    private String loadVCFile (String ref) {
        Properties prop = KrillProperties.loadDefaultProperties();
        if (prop == null){
            this.addError(StatusCodes.MISSING_KRILL_PROPERTIES,
                    "krill.properties is not found.");
            return null;
        }
        
        String namedVCPath = prop.getProperty("krill.namedVC");
        if (!namedVCPath.endsWith("/")){
            namedVCPath += "/";
        }
        File file = new File(namedVCPath+ref+".jsonld");
        
        String json = null;
        try {
            FileInputStream fis = new FileInputStream(file);
            json = IOUtils.toString(fis);
        }
        catch (IOException e) {
            this.addError(StatusCodes.MISSING_COLLECTION,
                    "Collection is not found.");
        }
        return json;
    }

    /**
     * Set the collection from a {@link CollectionBuilder} object.
     * 
     * @param cb
     *            The CollectionBuilder object.
     */
    public KrillCollection fromBuilder (CollectionBuilder.Interface cbi) {
        this.cbi = cbi;
        return this;
    };


    public CollectionBuilder.Interface getBuilder () {
        return this.cbi;
    };


    public CollectionBuilder build () {
        return this.cb;
    };


    public KrillCollection filter (CollectionBuilder.Interface filter) {
        return this.fromBuilder(this.cb.andGroup().with(this.cbi).with(filter));
    };


    public KrillCollection extend (CollectionBuilder.Interface extension) {
        return this
                .fromBuilder(this.cb.orGroup().with(this.cbi).with(extension));
    };



    /**
     * Add a filter based on a list of unique document identifiers.
     * UIDs may be indexed in the field "UID".
     * 
     * This filter is not part of the legacy API!
     * 
     * @param uids
     *            The list of unique document identifier.
     * @return The {@link KrillCollection} object for chaining.
     */
    public KrillCollection filterUIDs (String ... uids) {
        CollectionBuilder.Group cbg = this.cb.orGroup();
        for (String uid : uids) {
            cbg.with(this.cb.term("UID", uid));
        };
        return this.filter(cbg);
    };


    /**
     * Serialize collection to a {@link Filter} object.
     */
    public Filter toFilter () {
        if (this.cbi == null) return null;

        return this.cbi.toFilter();
    };


    /**
     * Boolean value if the collection should work inverted or
     * not.
     */
    public boolean isNegative () {
        if (this.cbi == null) return false;

        return this.cbi.isNegative();
    };


    /**
     * Generate a string representatio of the virtual collection.
     * 
     * <strong>Warning</strong>: This currently does not generate a
     * valid
     * KoralQuery string, so this may change in a future version.
     * 
     * @return A string representation of the virtual collection.
     */
    public String toString () {
        Filter filter = this.toFilter();
        if (filter == null) return "";

        return (this.isNegative() ? "-" : "") + filter.toString();
    };


    /**
     * Return the associated KoralQuery collection object
     * as a {@link JsonNode}. This won't work,
     * if the object was build using a CollectionBuilder,
     * therefore it is limited to mirror a deserialized KoralQuery
     * object.
     * 
     * @return The {@link JsonNode} representing the collection object
     *         of a deserialized KoralQuery object.
     */
    public JsonNode toJsonNode () {
        return this.json;
    };


    /**
     * Create a bit vector representing the live documents of the
     * virtual collection to be used in searches.
     * This will respect deleted documents.
     * 
     * @param The
     *            {@link LeafReaderContext} to search in.
     * @return A bit vector representing the live documents of the
     *         virtual collection.
     * @throws IOException
     */
    public FixedBitSet bits (LeafReaderContext atomic) throws IOException {

        LeafReader r = atomic.reader();
        FixedBitSet bitset = new FixedBitSet(r.maxDoc());
        DocIdSet docids = this.getDocIdSet(atomic, (Bits) r.getLiveDocs());

        if (docids == null) {
            if (this.cbi != null) {
                bitset.clear(0, bitset.length());
            }
            else {
                bitset.set(0, bitset.length());
            };
        }
        else
            bitset.or(docids.iterator());

        return bitset;
    };


    /**
     * Return the {@link DocIdSet} representing the documents of the
     * virtual collection to be used in searches.
     * This will respect deleted documents.
     * 
     * @param atomic
     *            The {@link LeafReaderContext} to search in.
     * @param accepted
     *            {@link Bits} vector of accepted documents.
     * @throws IOException
     */
    public DocIdSet getDocIdSet (LeafReaderContext atomic, Bits acceptDocs)
            throws IOException {

        int maxDoc = atomic.reader().maxDoc();
        FixedBitSet bitset = new FixedBitSet(maxDoc);

        Filter filter;
        if (this.cbi == null || (filter = this.cbi.toFilter()) == null) {
            if (acceptDocs == null) return null;

            bitset.set(0, maxDoc);
        }
        else {

            // Init vector
            DocIdSet docids = filter.getDocIdSet(atomic, null);
            DocIdSetIterator filterIter =
                    (docids == null) ? null : docids.iterator();

            if (filterIter == null) {
                if (!this.cbi.isNegative()) return null;

                bitset.set(0, maxDoc);
            }
            else {
                // Or bit set
                bitset.or(filterIter);

                // Revert for negation
                if (this.cbi.isNegative()) bitset.flip(0, maxDoc);
            };
        };

        if (DEBUG) {
            log.debug("Bit set is  {}", _bits(bitset));
            log.debug("Livedocs is {}", _bits(acceptDocs));
        };

        // Remove deleted docs
        return (DocIdSet) BitsFilteredDocIdSet
                .wrap((DocIdSet) new BitDocIdSet(bitset), acceptDocs);
    };


    public long numberOf (String type) throws IOException {
        return this.numberOf("tokens", type);
    };


    /**
     * Search for the number of occurrences of different types,
     * e.g. <i>documents</i>, <i>sentences</i> etc. in the virtual
     * collection.
     * 
     * @param field
     *            The field containing the textual data and the
     *            annotations as a string.
     * @param type
     *            The type of meta information,
     *            e.g. <i>documents</i> or <i>sentences</i> as a
     *            string.
     * @return The number of the occurrences.
     * @throws IOException
     * @see KrillIndex#numberOf
     */
    public long numberOf (String field, String type) throws IOException {

        // No index defined
        if (this.index == null) return (long) -1;

        // No reader (inex is empty)
        if (this.index.reader() == null) return (long) 0;

        // This is redundant to index stuff
        if (type.equals("documents") || type.equals("base/texts")) {
            if (this.cbi == null) {
                if (this.index.reader() == null) return (long) 0;
                return (long) this.index.reader().numDocs();
            }
            else
                return this.docCount();
        };

        // Create search term
        // This may be prefixed by foundries
        Term term = new Term(field, "-:" + type);

        if (DEBUG) log.debug("Iterate for {}/{}", field, type);

        long occurrences = 0;
        try {
            // Iterate over all atomic readers and collect occurrences
            for (LeafReaderContext atomic : this.index.reader().leaves()) {
                Bits bits = this.bits(atomic);

                if (DEBUG) log.debug("Final bits  {}", _bits(bits));

                occurrences += this._numberOfAtomic(bits, atomic, term);
                if (DEBUG) log.debug("Added up to {} for {}/{}", occurrences,
                        field, type);
            };
        }

        // Something went wrong
        catch (IOException e) {
            log.warn(e.getMessage());
        };

        return occurrences;
    };


    // Search for meta information in term vectors
    // This will create the sum of all numerical payloads
    // of the term in the document vector
    private long _numberOfAtomic (Bits docvec, LeafReaderContext atomic,
            Term term) throws IOException {

        // This reimplements docsAndPositionsEnum with payloads
        final Terms terms = atomic.reader().fields().terms(term.field());

        // No terms were found
        if (terms != null) {
            // Todo: Maybe reuse a termsEnum!
            final TermsEnum termsEnum = terms.iterator(null);

            // Set the position in the iterator to the term that is
            // seeked
            if (termsEnum.seekExact(term.bytes())) {

                // TODO: Reuse a DocsAndPositionsEnum!!

                // Start an iterator to fetch all payloads of the term
                DocsAndPositionsEnum docs = termsEnum.docsAndPositions(docvec,
                        null, DocsAndPositionsEnum.FLAG_PAYLOADS);


                // The iterator is empty
                // This may even be an error, but we return 0
                if (docs.docID() == DocsAndPositionsEnum.NO_MORE_DOCS) return 0;

                // Init some variables for data copying
                long occurrences = 0;
                BytesRef payload;

                // Init nextDoc()
                while (docs.nextDoc() != DocsAndPositionsEnum.NO_MORE_DOCS) {

                    if (docs.freq() < 1) continue;

                    // Initialize (go to first term)
                    docs.nextPosition();

                    // Copy payload with the offset of the BytesRef
                    payload = docs.getPayload();
                    if (payload != null) {
                        System.arraycopy(payload.bytes, payload.offset, pl, 0,
                                4);

                        // Add payload as integer
                        occurrences += ByteBuffer.wrap(pl).getInt();

                        if (DEBUG) log.debug(
                                "Value for {} incremented by {} to {} in {}",
                                term, ByteBuffer.wrap(pl).getInt(), occurrences,
                                docs.docID());
                    };
                };

                // Return the sum of all occurrences
                return occurrences;
            };
        };

        // Nothing found
        return 0;
    };


    /**
     * Return the number of documents in the virtual
     * collection.
     * 
     * @return The number of the occurrences.
     * @see #numberOf
     */
    public long docCount () {

        // No index defined
        if (this.index == null) return (long) 0;

        // TODO: Caching!

        long docCount = 0;
        try {
            FixedBitSet bitset;
            for (LeafReaderContext atomic : this.index.reader().leaves()) {
                if ((bitset = this.bits(atomic)) != null)
                    docCount += bitset.cardinality();
            };
        }
        catch (IOException e) {
            log.warn(e.getLocalizedMessage());
        };
        return docCount;
    };


    private static String _bits (Bits bitset) {
        String str = "";
        for (int i = 0; i < bitset.length(); i++) {
            str += bitset.get(i) ? "1" : "0";
        };
        return str;
    };


    public void storeInCache (String cacheKey) throws IOException {
        if (cacheKey ==null || cacheKey.isEmpty()) {
            this.addError(StatusCodes.MISSING_ID,
                    "Collection name is required for caching.");
        }
        
        List<LeafReaderContext> leaves = this.index.reader().leaves();
        Map<Integer, DocBits> docIdMap =
                new HashMap<Integer, DocBits>(leaves.size());

        for (LeafReaderContext context : leaves) {
            if (docIdMap.get(context.hashCode()) == null) {
                FixedBitSet bitset = bits(context);
                docIdMap.put(context.hashCode(),
                        new DocBits(bitset.getBits()));
            }
        }

        CachedVCData cc = new CachedVCData(docIdMap);
        cache.put(new Element(cacheKey, cc));
        this.cbi = cb.namedVC(cc);
    }
    
    /*
     * Analyze how terms relate
     */
    /*
    @Deprecated
    public HashMap getTermRelation (KrillCollection kc, String field)
            throws Exception {
        HashMap<String, Long> map = new HashMap<>(100);
        long docNumber = 0, checkNumber = 0;
    
        try {
            if (kc.getCount() <= 0) {
                checkNumber = (long) this.reader().numDocs();
            };
    
            for (LeafReaderContext atomic : this.reader().leaves()) {
                HashMap<String, FixedBitSet> termVector = new HashMap<>(20);
    
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
    
                    bitset.clear(0, docLength);
    
                    // Get frequency
                    bitset.or((DocIdSetIterator) termsEnum.docs((Bits) docvec,
                            null));
    
                    long value = 0;
                    if (map.containsKey(termString))
                        value = map.get(termString);
    
                    map.put(termString, value + bitset.cardinality());
    
                    termVector.put(termString, bitset.clone());
                };
    
                int keySize = termVector.size();
                String[] keys = termVector.keySet()
                        .toArray(new String[keySize]);
                java.util.Arrays.sort(keys);
    
                if (keySize > maxTermRelations) {
                    throw new Exception("termRelations are limited to "
                            + maxTermRelations + " sets"
                            + " (requested were at least " + keySize + " sets)");
                };
    
                for (int i = 0; i < keySize; i++) {
                    for (int j = i + 1; j < keySize; j++) {
                        FixedBitSet comby = termVector.get(keys[i]).clone();
                        comby.and(termVector.get(keys[j]));
    
                        StringBuilder sb = new StringBuilder();
                        sb.append("#__").append(keys[i]).append(":###:")
                                .append(keys[j]);
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
    */


};
