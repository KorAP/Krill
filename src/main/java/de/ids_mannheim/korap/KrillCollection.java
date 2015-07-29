package de.ids_mannheim.korap;

import java.util.*;
import java.io.IOException;

import de.ids_mannheim.korap.collection.CollectionBuilder;
import de.ids_mannheim.korap.response.Notifications;
import de.ids_mannheim.korap.util.QueryException;
import de.ids_mannheim.korap.response.Result;

import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.*;
import org.apache.lucene.index.*;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.FixedBitSet;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.OpenBitSet;
import org.apache.lucene.util.DocIdBitSet;
import org.apache.lucene.search.BitsFilteredDocIdSet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class KrillCollection extends Notifications {
    private KrillIndex index;
    private JsonNode json;
    private CollectionBuilder.CollectionBuilderInterface cb;
    private byte[] pl = new byte[4];
    private static ByteBuffer bb = ByteBuffer.allocate(4);

    // Logger
    private final static Logger log = LoggerFactory
            .getLogger(KrillCollection.class);

    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;


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

            if (json.has("collection"))
                this.fromJson(json.get("collection"));

            else if (json.has("collections"))
                this.addError(899, "Collections are not supported anymore in favour of a single collection");
        }

        // Query Exception
        catch (QueryException qe) {
            this.addError(qe.getErrorCode(), qe.getMessage());
        }

        // JSON exception
        catch (IOException e) {
            this.addError(621, "Unable to parse JSON", "KrillCollection",
                    e.getLocalizedMessage());
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
    public KrillCollection fromJson (String jsonString) throws QueryException {
        ObjectMapper mapper = new ObjectMapper();
        try {
            this.fromJson((JsonNode) mapper.readTree(jsonString));
        }
        catch (Exception e) {
            this.addError(621, "Unable to parse JSON", "KrillCollection");
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
    public KrillCollection fromJson (JsonNode json) throws QueryException {
        this.json = json;
        return this.fromBuilder(this._fromJson(json));
    };


    private CollectionBuilder.CollectionBuilderInterface _fromJson (JsonNode json) throws QueryException {

        CollectionBuilder cb = new CollectionBuilder();

        if (!json.has("@type")) {
            throw new QueryException(701,
                    "JSON-LD group has no @type attribute");
        };

        String type = json.get("@type").asText();

        if (type.equals("koral:doc")) {

            String key = "tokens";
            String valtype = "type:string";
            String match = "match:eq";

            if (json.has("key"))
                key = json.get("key").asText();

            if (json.has("type"))
                valtype = json.get("type").asText();

            // Filter based on date
            if (valtype.equals("type:date")) {

                if (!json.has("value"))
                    throw new QueryException(820, "Dates require value fields");

                String dateStr = json.get("value").asText();

                if (json.has("match"))
                    match = json.get("match").asText();

                // TODO: This isn't stable yet
                switch (match) {
                case "match:eq":
                    return cb.date(key, dateStr);
                case "match:ne":
                    return cb.date(key, dateStr).not();
                case "match:geq":
                    return cb.since(key, dateStr);
                case "match:leq":
                    return cb.till(key, dateStr);
                };

                throw new QueryException(841, "Match relation unknown for type");
            }

            // Filter based on string
            else if (valtype.equals("type:string")) {
                if (json.has("match"))
                    match = json.get("match").asText();

                switch (match) {

                case "match:eq":
                    return cb.term(key, json.get("value").asText());
                case "match:ne":
                    return cb.term(key, json.get("value").asText()).not();

                // This may change - but for now it means the elements are lowercased
                case "match:contains":
                    return cb.term(key, json.get("value").asText().toLowerCase());

                case "match:containsnot":
                    return cb.term(key, json.get("value").asText().toLowerCase()).not();

                    // <LEGACY>
                case "match:excludes":
                    return cb.term(key, json.get("value").asText().toLowerCase()).not();
                    // </LEGACY>
                };

                throw new QueryException(841, "Match relation unknown for type");
            }

            // Filter based on regex
            else if (valtype.equals("type:regex")) {

                if (json.has("match"))
                    match = json.get("match").asText();

                if (match.equals("match:eq")) {
                    return cb.re(key, json.get("value").asText());
                }
                else if (match.equals("match:ne")) {
                    return cb.re(key, json.get("value").asText()).not();
                }
                else if (match.equals("match:contains")) {
                    return cb.re(key, json.get("value").asText());
                }
                else if (match.equals("match:excludes")) {
                    return cb.re(key, json.get("value").asText()).not();
                };

                throw new QueryException(841, "Match relation unknown for type");
            };

            throw new QueryException(843, "Document type is not supported");
        }

        // nested group
        else if (type.equals("koral:docGroup")) {

            if (!json.has("operands") || !json.get("operands").isArray())
                throw new QueryException(842, "Document group needs operand list");

            CollectionBuilder.CollectionBuilderGroup group;

            String operation = "operation:and";
            if (json.has("operation"))
                operation = json.get("operation").asText();            

            if (operation.equals("operation:or"))
                group = cb.orGroup();
            else if (operation.equals("operation:and"))
                group = cb.andGroup();
            else
                throw new QueryException(810, "Unknown document group operation");
    
            for (JsonNode operand : json.get("operands")) {
                group.with(this._fromJson(operand));
            };
            return group;
        }

        // Unknown type
        throw new QueryException(813, "Collection type is not supported");
    }; 

    // Returns the number of filters - always one!
    @Deprecated
    public int getCount () {
        return 1;
    };





    /**
     * Set the collection from a {@link CollectionBuilder} object.
     * 
     * @param cb The CollectionBuilder object.
     */
    public KrillCollection fromBuilder (CollectionBuilder.CollectionBuilderInterface cb) {
        this.cb = cb;
        return this;
    };

    public CollectionBuilder.CollectionBuilderInterface getBuilder () {
        return this.cb;
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
        /*
        BooleanFilter filter = new BooleanFilter();
        filter.or("UID", uids);
        if (DEBUG)
            log.debug("UID based filter: {}", filter.toString());
        return this.filter(filter);
        */
        return this;
    };


    /**
     * Serialize collection to a {@link Filter} object.
     */
    public Filter toFilter () {
        if (this.cb == null)
            return null;

        return this.cb.toFilter();
    };


    /**
     * Boolean value if the collection should work inverted or
     * not.
     */
    public boolean isNegative () {
        if (this.cb == null)
            return false;

        return this.cb.isNegative();
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
        if (filter == null)
            return "";

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
     * Search in the virtual collection.
     * This is mostly used for testing purposes
     * and <strong>is not recommended</strong>
     * as a common search API.
     * 
     * Please use {@link KrillQuery#run} instead.
     * 
     * @param query
     *            a {@link SpanQuery} to apply on the
     *            virtual collection.
     * @return A {@link Result} object representing the search's
     *         result.
     */
    public Result search (SpanQuery query) {
        // return this.index.search(this, query, 0, (short) 20, true, (short) 5, true, (short) 5);
        return null;
    };


    /**
     * Create a bit vector representing the live documents of the
     * virtual collection to be used in searches.
     * This will respect deleted documents.
     * 
     * @param The
     *            {@link AtomicReaderContext} to search in.
     * @return A bit vector representing the live documents of the
     *         virtual collection.
     * @throws IOException
     */
    public FixedBitSet bits (AtomicReaderContext atomic) throws IOException {
        AtomicReader r = atomic.reader();
        FixedBitSet bitset = new FixedBitSet(r.maxDoc());
        DocIdSet docids = this.getDocIdSet(atomic, (Bits) r.getLiveDocs());

        if (docids == null)
            return null;

        bitset.or(docids.iterator());
        return bitset;
    };


    /**
     * Return the {@link DocIdSet} representing the documents of the
     * virtual collection to be used in searches.
     * This will respect deleted documents.
     * 
     * @param atomic
     *           The {@link AtomicReaderContext} to search in.
     * @param accepted
     *            {@link Bits} vector of accepted documents.
     * @throws IOException
     */
    public DocIdSet getDocIdSet (AtomicReaderContext atomic, Bits acceptDocs) throws IOException {

        int maxDoc = atomic.reader().maxDoc();
        FixedBitSet bitset = new FixedBitSet(maxDoc);

        Filter filter;
        if (this.cb == null || (filter = this.cb.toFilter()) == null)
            return null;

        // Init vector
        DocIdSet docids = filter.getDocIdSet(atomic, null);
        DocIdSetIterator filterIter = (docids == null) ? null : docids.iterator();

        if (filterIter == null) {
            if (!this.cb.isNegative())
                return null;

            bitset.set(0, maxDoc);
        }
        else {
            // Or bit set
            bitset.or(filterIter);

            // Revert for negation
            if (this.cb.isNegative())
                bitset.flip(0, maxDoc);
        };

        // Remove deleted docs
        return (DocIdSet) BitsFilteredDocIdSet.wrap(
            (DocIdSet) bitset,
            acceptDocs
        );
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
        if (this.index == null)
            return (long) -1;

        // This is redundant to index stuff
        if (type.equals("documents") || type.equals("base/texts")) {
            if (this.cb == null)
                return (long) this.index.reader().numDocs();
            else
                return this.docCount();
        };
        
        // Create search term
        // This may be prefixed by foundries
        Term term = new Term(field, "-:" + type);

        long occurrences = 0;
        try {
            // Iterate over all atomic readers and collect occurrences
            for (AtomicReaderContext atomic : this.index.reader().leaves()) {
                occurrences += this._numberOfAtomic(this.bits(atomic), atomic, term);
            };
        }
        
        // Something went wrong
        catch (Exception e) {
            log.warn(e.getLocalizedMessage());
        };

        return occurrences;
    };


    // Search for meta information in term vectors
    // This will create the sum of all numerical payloads
    // of the term in the document vector
    private long _numberOfAtomic (Bits docvec, AtomicReaderContext atomic,
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


    /**
     * Return the number of documents in the virtual
     * collection.
     * 
     * @return The number of the occurrences.
     * @see #numberOf
     */
    public long docCount () {

        // No index defined
        if (this.index == null)
            return (long) 0;

        // TODO: Caching!

        long docCount = 0;
        try {
            FixedBitSet bitset;
            for (AtomicReaderContext atomic : this.index.reader().leaves()) {
                if ((bitset = this.bits(atomic)) != null)
                    docCount += bitset.cardinality();
            };
        }
        catch (IOException e) {
            log.warn(e.getLocalizedMessage());
        };
        return docCount;
    };



    /*
    @Deprecated
    public HashMap getTermRelation (String field) throws Exception {
        return this.getTermRelation(new KrillCollection(this), field);
    };
*/

    /**
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

            for (AtomicReaderContext atomic : this.reader().leaves()) {
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
