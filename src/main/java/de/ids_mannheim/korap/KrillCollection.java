package de.ids_mannheim.korap;

import java.util.*;
import java.io.IOException;

import de.ids_mannheim.korap.*;
import de.ids_mannheim.korap.util.KrillDate;
import de.ids_mannheim.korap.util.QueryException;
import de.ids_mannheim.korap.collection.BooleanFilter;
import de.ids_mannheim.korap.collection.RegexFilter;
import de.ids_mannheim.korap.collection.FilterOperation;
import de.ids_mannheim.korap.collection.CollectionBuilder;
import de.ids_mannheim.korap.response.Notifications;
import de.ids_mannheim.korap.response.Result;

import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.*;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.util.FixedBitSet;
import org.apache.lucene.util.Bits;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.StringWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create a Virtual Collection of documents by means of a KoralQuery
 * collection object.
 * Alternatively by applying manual filters and extensions on Lucene
 * fields.
 * 
 * <blockquote><pre>
 * KrillCollection kc = new KrillCollection(json);
 * kc.filterUIDS("a1", "a2", "a3");
 * </pre></blockquote>
 * 
 * <strong>Warning</strong>: This API is deprecated and will
 * be replaced in future versions. It supports legacy versions of
 * KoralQuery that will be disabled.
 * 
 * @author diewald
 */
/*
 * TODO: Clean up for new KoralQuery
 * TODO: Make a cache for the bits
 *       Delete it in case of an extension or a filter
 * TODO: Maybe use randomaccessfilterstrategy
 * TODO: Maybe a constantScoreQuery can make things faster?
 * See http://mail-archives.apache.org/mod_mbox/lucene-java-user/
 *     200805.mbox/%3C17080852.post@talk.nabble.com%3E
 */
public class KrillCollection extends Notifications {
    private KrillIndex index;
    private KrillDate created;
    private String id;
    private ArrayList<FilterOperation> filter;
    private int filterCount = 0;
    private JsonNode json;

    // Logger
    private final static Logger log = LoggerFactory
            .getLogger(KrillCollection.class);

    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = true;


    /**
     * Construct a new KrillCollection by passing a KrillIndex.
     * 
     * @param index
     *            The {@link KrillIndex} object.
     */
    public KrillCollection (KrillIndex index) {
        this.index = index;
        this.filter = new ArrayList<FilterOperation>(5);
    };


    /**
     * Construct a new KrillCollection by passing a KoralQuery.
     * This supports collections with the key "collection" and
     * legacy collections with the key "collections".
     * 
     * @param jsonString
     *            The virtual collection as a KoralQuery.
     */
    public KrillCollection (String jsonString) {
        ObjectMapper mapper = new ObjectMapper();
        this.filter = new ArrayList<FilterOperation>(5);

        try {
            JsonNode json = mapper.readTree(jsonString);

            // Deserialize from recent collections
            if (json.has("collection")) {
                this.fromJson(json.get("collection"));
            }

            // Legacy collection serialization
            // This will be removed!
            else if (json.has("collections")) {
                this.addMessage(850,
                        "Collections are deprecated in favour of a single collection");
                for (JsonNode collection : json.get("collections")) {
                    this.fromJsonLegacy(collection);
                };
            };
        }
        // Some exceptions ...
        catch (QueryException qe) {
            this.addError(qe.getErrorCode(), qe.getMessage());
        }
        catch (IOException e) {
            this.addError(621, "Unable to parse JSON", "KrillCollection",
                    e.getLocalizedMessage());
        };
    };


    /**
     * Construct a new KrillCollection.
     */
    public KrillCollection () {
        this.filter = new ArrayList<FilterOperation>(5);
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
        this.filter(this._fromJson(json));
        return this;
    };


    // Create a boolean filter from JSON
    private BooleanFilter _fromJson (JsonNode json) throws QueryException {
        return this._fromJson(json, "tokens");
    };


    // Create a booleanfilter from JSON
    private BooleanFilter _fromJson (JsonNode json, String field)
            throws QueryException {
        BooleanFilter bfilter = new BooleanFilter();

        if (!json.has("@type")) {
            throw new QueryException(701,
                    "JSON-LD group has no @type attribute");
        };

        String type = json.get("@type").asText();

        // Single filter
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
                    throw new QueryException(612, "Dates require value fields");

                String dateStr = json.get("value").asText();
                if (json.has("match"))
                    match = json.get("match").asText();

                // TODO: This isn't stable yet
                switch (match) {
                    case "match:eq":
                        bfilter.date(dateStr);
                        break;
                    case "match:geq":
                        bfilter.since(dateStr);
                        break;
                    case "match:leq":
                        bfilter.till(dateStr);
                        break;
                };

                // No good reason for gt or lt
                return bfilter;
            }

            // Filter based on string
            else if (valtype.equals("type:string")) {
                if (json.has("match"))
                    match = json.get("match").asText();

                if (match.equals("match:eq")) {
                    bfilter.and(key, json.get("value").asText());
                }
                else if (match.equals("match:ne")) {
                    bfilter.andNot(key, json.get("value").asText());
                }
                // This may change - but for now it means the elements are lowercased
                else if (match.equals("match:contains")) {
                    bfilter.and(key, json.get("value").asText().toLowerCase());
                }
                else if (match.equals("match:containsnot")) {
                    bfilter.andNot(key, json.get("value").asText().toLowerCase());
                }
                // <LEGACY>
                else if (match.equals("match:excludes")) {
                    bfilter.andNot(key, json.get("value").asText().toLowerCase());
                }
                // </LEGACY>
                else {
                    throw new QueryException(0, "Unknown match type");
                };

                return bfilter;
            }

            // Filter based on regex
            else if (valtype.equals("type:regex")) {
                if (json.has("match"))
                    match = json.get("match").asText();

                if (match.equals("match:eq")) {
                    return bfilter.and(key, new RegexFilter(json.get("value")
                            .asText()));
                }
                else if (match.equals("match:ne")) {
                    return bfilter.andNot(key, new RegexFilter(json
                            .get("value").asText()));
                };

                // TODO! for excludes and contains
                throw new QueryException(0, "Unknown document type");
            };

            // TODO!
            throw new QueryException(0, "Unknown document operation");
        }

        // nested group
        else if (type.equals("koral:docGroup")) {
            if (!json.has("operands") || !json.get("operands").isArray())
                throw new QueryException(612, "Groups need operands");

            String operation = "operation:and";
            if (json.has("operation"))
                operation = json.get("operation").asText();

            BooleanFilter group = new BooleanFilter();

            for (JsonNode operand : json.get("operands")) {
                if (operation.equals("operation:and"))
                    group.and(this._fromJson(operand, field));

                else if (operation.equals("operation:or"))
                    group.or(this._fromJson(operand, field));

                else
                    throw new QueryException(613,
                            "Unknown document group operation");
            };
            bfilter.and(group);
            return bfilter;
        }

        // Unknown type
        else
            throw new QueryException(613,
                    "Collection query type has to be doc or docGroup");

        // return new BooleanFilter();
    };


    /**
     * Import the "collections" part of a KoralQuery.
     * This method is deprecated and will vanish in future versions.
     * 
     * @param jsonString
     *            The "collections" part of a KoralQuery.
     * @throws QueryException
     */
    @Deprecated
    public KrillCollection fromJsonLegacy (String jsonString)
            throws QueryException {
        ObjectMapper mapper = new ObjectMapper();
        try {
            this.fromJsonLegacy((JsonNode) mapper.readValue(jsonString,
                    JsonNode.class));
        }
        catch (Exception e) {
            this.addError(621, "Unable to parse JSON", "KrillCollection");
        };
        return this;
    };


    /**
     * Import the "collections" part of a KoralQuery.
     * This method is deprecated and will vanish in future versions.
     * 
     * @param json
     *            The "collections" part of a KoralQuery
     *            as a {@link JsonNode} object.
     * @throws QueryException
     */
    @Deprecated
    public KrillCollection fromJsonLegacy (JsonNode json) throws QueryException {
        if (!json.has("@type"))
            throw new QueryException(701,
                    "JSON-LD group has no @type attribute");

        if (!json.has("@value"))
            throw new QueryException(851, "Legacy filter need @value fields");

        BooleanFilter bf = this._fromJsonLegacy(json.get("@value"), "tokens");
        String type = json.get("@type").asText();

        // Filter the collection
        if (type.equals("koral:meta-filter")) {
            if (DEBUG)
                log.trace("Add Filter LEGACY");
            this.filter(bf);
        }

        // Extend the collection
        else if (type.equals("koral:meta-extend")) {
            if (DEBUG)
                log.trace("Add Extend LEGACY");
            this.extend(bf);
        };

        return this;
    };


    // Create a boolean filter from a Json string
    @Deprecated
    private BooleanFilter _fromJsonLegacy (JsonNode json, String field)
            throws QueryException {
        BooleanFilter bfilter = new BooleanFilter();

        if (!json.has("@type"))
            throw new QueryException(612,
                    "JSON-LD group has no @type attribute");

        String type = json.get("@type").asText();

        if (DEBUG)
            log.trace("@type: " + type);

        if (json.has("@field"))
            field = _getFieldLegacy(json);

        if (type.equals("koral:term")) {
            if (field != null && json.has("@value"))
                bfilter.and(field, json.get("@value").asText());
            return bfilter;
        }
        else if (type.equals("koral:group")) {
            if (!json.has("relation"))
                throw new QueryException(612, "Group needs relation");

            if (!json.has("operands"))
                throw new QueryException(612, "Group needs operand list");

            String dateStr, till;
            JsonNode operands = json.get("operands");

            if (!operands.isArray())
                throw new QueryException(612, "Group needs operand list");

            if (DEBUG)
                log.trace("relation found {}", json.get("relation").asText());

            BooleanFilter group = new BooleanFilter();

            switch (json.get("relation").asText()) {
                case "between":
                    dateStr = _getDateLegacy(json, 0);
                    till = _getDateLegacy(json, 1);
                    if (dateStr != null && till != null)
                        bfilter.between(dateStr, till);
                    break;

                case "until":
                    dateStr = _getDateLegacy(json, 0);
                    if (dateStr != null)
                        bfilter.till(dateStr);
                    break;

                case "since":
                    dateStr = _getDateLegacy(json, 0);
                    if (dateStr != null)
                        bfilter.since(dateStr);
                    break;

                case "equals":
                    dateStr = _getDateLegacy(json, 0);
                    if (dateStr != null)
                        bfilter.date(dateStr);
                    break;

                case "and":
                    if (operands.size() < 1)
                        throw new QueryException(612,
                                "Operation needs at least two operands");

                    for (JsonNode operand : operands) {
                        group.and(this._fromJsonLegacy(operand, field));
                    }
                    ;
                    bfilter.and(group);
                    break;

                case "or":
                    if (operands.size() < 1)
                        throw new QueryException(612,
                                "Operation needs at least two operands");

                    for (JsonNode operand : operands) {
                        group.or(this._fromJsonLegacy(operand, field));
                    }
                    ;
                    bfilter.and(group);
                    break;

                default:
                    throw new QueryException(613, "Relation is not supported");
            };
        }
        else {
            throw new QueryException(613,
                    "Filter type is not a supported group");
        };
        return bfilter;
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
     * Add a filter by means of a {@link BooleanFilter}.
     * 
     * <strong>Warning</strong>: Filters are part of the collections
     * legacy API and may vanish without warning.
     * 
     * @param filter
     *            The filter to add to the collection.
     * @return The {@link KrillCollection} object for chaining.
     */
    // TODO: The checks may not be necessary
    public KrillCollection filter (BooleanFilter filter) {
        if (DEBUG)
            log.trace("Added filter: {}", filter.toString());

        if (filter == null) {
            this.addWarning(830, "Filter was empty");
            return this;
        };

        Filter f = (Filter) new QueryWrapperFilter(filter.toQuery());
        if (f == null) {
            this.addWarning(831, "Filter is not wrappable");
            return this;
        };
        FilterOperation fo = new FilterOperation(f, false);
        if (fo == null) {
            this.addWarning(832, "Filter operation is invalid");
            return this;
        };
        this.filter.add(fo);
        this.filterCount++;
        return this;
    };


    /**
     * Add a filter by means of a {@link CollectionBuilder} object.
     * 
     * <strong>Warning</strong>: Filters are part of the collections
     * legacy API and may vanish without warning.
     * 
     * @param filter
     *            The filter to add to the collection.
     * @return The {@link KrillCollection} object for chaining.
     */
    public KrillCollection filter (CollectionBuilder filter) {
        return this.filter(filter.getBooleanFilter());
    };


    /**
     * Add an extension by means of a {@link BooleanFilter}.
     * 
     * <strong>Warning</strong>: Extensions are part of the
     * collections
     * legacy API and may vanish without warning.
     * 
     * @param extension
     *            The extension to add to the collection.
     * @return The {@link KrillCollection} object for chaining.
     */
    public KrillCollection extend (BooleanFilter extension) {
        if (DEBUG)
            log.trace("Added extension: {}", extension.toString());

        this.filter.add(new FilterOperation((Filter) new QueryWrapperFilter(
                extension.toQuery()), true));
        this.filterCount++;
        return this;
    };


    /**
     * Add an extension by means of a {@link CollectionBuilder}
     * object.
     * 
     * <strong>Warning</strong>: Extensions are part of the
     * collections
     * legacy API and may vanish without warning.
     * 
     * @param extension
     *            The extension to add to the collection.
     * @return The {@link KrillCollection} object for chaining.
     */
    public KrillCollection extend (CollectionBuilder extension) {
        return this.extend(extension.getBooleanFilter());
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
        BooleanFilter filter = new BooleanFilter();
        filter.or("UID", uids);
        if (DEBUG)
            log.debug("UID based filter: {}", filter.toString());
        return this.filter(filter);
    };


    /**
     * Get the list of filters constructing the collection.
     * 
     * <strong>Warning</strong>: This is part of the collections
     * legacy API and may vanish without warning.
     * 
     * @return The list of filters.
     */
    public List<FilterOperation> getFilters () {
        return this.filter;
    };


    /**
     * Get a certain {@link FilterOperation} from the list of filters
     * constructing the collection by its numerical index.
     * 
     * <strong>Warning</strong>: This is part of the collections
     * legacy API and may vanish without warning.
     * 
     * @param index
     *            The index position of the requested
     *            {@link FilterOperation}.
     * @return The {@link FilterOperation} at the certain list
     *         position.
     */
    public FilterOperation getFilter (int index) {
        return this.filter.get(index);
    };


    /**
     * Get the number of filter operations constructing this
     * collection.
     * 
     * <strong>Warning</strong>: This is part of the collections
     * legacy API and may vanish without warning.
     * 
     * @return The number of filter operations constructing this
     *         collection.
     */
    public int getCount () {
        return this.filterCount;
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
        StringBuilder sb = new StringBuilder();
        for (FilterOperation fo : this.filter) {
            sb.append(fo.toString()).append("; ");
        };
        return sb.toString();
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
        return this.index.search(this, query, 0, (short) 20, true, (short) 5,
                true, (short) 5);
    };


    /**
     * Create a bit vector representing the live documents of the
     * virtual collection to be used in searches.
     * 
     * @param The
     *            {@link AtomicReaderContext} to search in.
     * @return A bit vector representing the live documents of the
     *         virtual collection.
     * @throws IOException
     */
    public FixedBitSet bits (AtomicReaderContext atomic) throws IOException {
        // TODO: Probably use Bits.MatchAllBits(int len)
        boolean noDoc = true;
        FixedBitSet bitset;

        // There are filters set
        if (this.filterCount > 0) {
            bitset = new FixedBitSet(atomic.reader().maxDoc());

            ArrayList<FilterOperation> filters = (ArrayList<FilterOperation>) this.filter
                    .clone();

            FilterOperation kcInit = filters.remove(0);
            if (DEBUG)
                log.trace("FILTER: {}", kcInit);

            // Init vector
            DocIdSet docids = kcInit.filter.getDocIdSet(atomic, null);

            DocIdSetIterator filterIter = docids.iterator();

            // The filter has an effect
            if (filterIter != null) {
                if (DEBUG)
                    log.trace("InitFilter has effect");
                bitset.or(filterIter);
                noDoc = false;
            };

            // Apply all filters sequentially
            for (FilterOperation kc : filters) {
                if (DEBUG)
                    log.trace("FILTER: {}", kc);

                // TODO: BUG???
                docids = kc.filter.getDocIdSet(atomic, kc.isExtension() ? null
                        : bitset);
                filterIter = docids.iterator();

                if (filterIter == null) {
                    // There must be a better way ...
                    if (kc.isFilter()) {
                        // TODO: Check if this is really correct!
                        // Maybe here is the bug
                        bitset.clear(0, bitset.length());
                        noDoc = true;
                    };
                    continue;
                };
                if (kc.isExtension())
                    bitset.or(filterIter);
                else
                    bitset.and(filterIter);
            };

            if (!noDoc) {
                FixedBitSet livedocs = (FixedBitSet) atomic.reader()
                        .getLiveDocs();
                if (livedocs != null)
                    bitset.and(livedocs);
            };
        }
        else {
            bitset = (FixedBitSet) atomic.reader().getLiveDocs();
        };

        return bitset;
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
        if (this.index == null)
            return (long) -1;

        return this.index.numberOf(this, field, type);
    };


    /**
     * Search for the number of occurrences of different types,
     * e.g. <i>documents</i>, <i>sentences</i> etc. in the virtual
     * collection, in the <i>base</i> foundry.
     * 
     * @param type
     *            The type of meta information,
     *            e.g. <i>documents</i> or <i>sentences</i> as a
     *            string.
     * @return The number of the occurrences.
     * @throws IOException
     * @see KrillIndex#numberOf
     */
    public long numberOf (String type) throws IOException {
        if (this.index == null)
            return (long) -1;

        return this.index.numberOf(this, "tokens", type);
    };


    // Term relation API is not in use anymore
    @Deprecated
    public HashMap getTermRelation (String field) throws Exception {
        if (this.index == null) {
            HashMap<String, Long> map = new HashMap<>(1);
            map.put("-docs", (long) 0);
            return map;
        };

        return this.index.getTermRelation(this, field);
    };


    // Term relation API is not in use anymore
    @Deprecated
    public String getTermRelationJSON (String field) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        StringWriter sw = new StringWriter();
        sw.append("{\"field\":");
        mapper.writeValue(sw, field);
        sw.append(",");

        try {
            HashMap<String, Long> map = this.getTermRelation(field);

            sw.append("\"documents\":");
            mapper.writeValue(sw, map.remove("-docs"));
            sw.append(",");

            String[] keys = map.keySet().toArray(new String[map.size()]);

            HashMap<String, Integer> setHash = new HashMap<>(20);
            ArrayList<HashMap<String, Long>> set = new ArrayList<>(20);
            ArrayList<Long[]> overlap = new ArrayList<>(100);

            int count = 0;
            for (String key : keys) {
                if (!key.startsWith("#__")) {
                    HashMap<String, Long> simpleMap = new HashMap<>();
                    simpleMap.put(key, map.remove(key));
                    set.add(simpleMap);
                    setHash.put(key, count++);
                };
            };

            keys = map.keySet().toArray(new String[map.size()]);
            for (String key : keys) {
                String[] comb = key.substring(3).split(":###:");
                Long[] l = new Long[3];
                l[0] = (long) setHash.get(comb[0]);
                l[1] = (long) setHash.get(comb[1]);
                l[2] = map.remove(key);
                overlap.add(l);
            };

            sw.append("\"sets\":");
            mapper.writeValue(sw, (Object) set);
            sw.append(",\"overlaps\":");
            mapper.writeValue(sw, (Object) overlap);
            sw.append(",\"error\":null");
        }
        catch (Exception e) {
            sw.append("\"error\":");
            mapper.writeValue(sw, e.getMessage());
        };

        sw.append("}");
        return sw.getBuffer().toString();
    };


    // Get legacy field
    @Deprecated
    private static String _getFieldLegacy (JsonNode json) {
        if (!json.has("@field"))
            return (String) null;

        String field = json.get("@field").asText();
        return field.replaceFirst("koral:field#", "");
    };


    // Get legacy date
    @Deprecated
    private static String _getDateLegacy (JsonNode json, int index) {
        if (!json.has("operands"))
            return (String) null;

        if (!json.get("operands").has(index))
            return (String) null;

        JsonNode date = json.get("operands").get(index);

        if (!date.has("@type"))
            return (String) null;

        if (!date.get("@type").asText().equals("koral:date"))
            return (String) null;

        if (!date.has("@value"))
            return (String) null;

        return date.get("@value").asText();
    };
};
