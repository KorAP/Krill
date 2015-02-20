package de.ids_mannheim.korap;

import java.io.*;
import java.util.*;

import org.apache.lucene.search.spans.SpanQuery;
import de.ids_mannheim.korap.query.wrap.SpanQueryWrapper;
import de.ids_mannheim.korap.KorapCollection;
import de.ids_mannheim.korap.KorapIndex;
import de.ids_mannheim.korap.KorapResult;
import de.ids_mannheim.korap.util.QueryException;
import de.ids_mannheim.korap.index.SearchContext;
import de.ids_mannheim.korap.response.Notifications;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Krill is a corpus data retrieval index using Lucene for Look-Ups.
 * It is the reference implementation for KoralQuery consumption.
 *
 * <blockquote><pre>
 *   // Create a new krill search object passing a KoralQuery string
 *   Krill krill = new Krill(jsonString);
 *
 *   // Run the query on an index
 *   KrillResult kr = krill.apply(new KrillIndex());
 * </pre></blockquote>
 *
 * @author diewald
 * @author margaretha
 */
/*
 * Todo: Use a configuration file
 * Todo: Let this class extend KorapResult!
 *   KorapResult = new KorapSearch(String json).run(KorapIndex ki);
 * Todo: Set timeout default value per config file
 */

public class KorapSearch extends Notifications {
    private int startIndex = 0, limit = 0;
    private short count = 25, countMax = 50, itemsPerResource = 0;
    private boolean cutOff = false;

    private SpanQuery spanQuery;

    private KorapCollection collection;
    private KorapIndex index;

    // Timeout search after milliseconds
    private long timeout = (long) 120_000;

    private HashSet<String> fields;
    private HashSet<Integer> highlights;

    private JsonNode request;

    public SearchContext context;
    private String spanContext;

    private long timeoutStart = Long.MIN_VALUE;

    {
        context  = new SearchContext();

        fields = new HashSet<String>(16);

        // Lift legacy fields per default
        for (String field : new String[]{
                "ID",
                "UID",
                "textSigle",
                "corpusID",
                "author",
                "title",
                "subTitle",
                "textClass",
                "pubPlace",
                "pubDate",
                "foundries",
                "layerInfo",
                "tokenization"}) {
            fields.add(field);
        };

        // Classes used for highlights
        highlights = new HashSet<Integer>(3);
    };


    /**
     * Construct a new Krill object.
     */
    public KorapSearch () {};


    /**
     * Construct a new Krill object,
     * consuming a {@link SpanQueryWrapper} object.
     *
     * @param query The {@link SpanQueryWrapper} object.
     */
    public KorapSearch (SpanQueryWrapper query) {
        try {
            this.spanQuery = query.toQuery();
        }
        catch (QueryException q) {
            this.addError(q.getErrorCode(), q.getMessage());
        };
    };
    

    /**
     * Construct a new Krill object,
     * consuming a {@link SpanQuery} object.
     *
     * @param query The {@link SpanQuery} object.
     */
    public KorapSearch (SpanQuery query) {
        this.spanQuery = query;
    };


    public KorapSearch (String jsonString) {
        try {
            // Parse json string
            this.fromJson(jsonString);
        }
        catch (QueryException qe) {
            this.addError(qe.getErrorCode(), qe.getMessage());
        };
    };

    public KorapSearch (JsonNode json) {
        try {
            this.fromJson(json);
        }
        catch (QueryException qe) {
            this.addError(qe.getErrorCode(), qe.getMessage());
        };
    };


    public KorapSearch fromJson (JsonNode json) throws QueryException {
        // Parse "query" attribute
        if (json.has("query")) {
            try {
                KorapQuery kq = new KorapQuery("tokens");
                SpanQueryWrapper qw = kq.fromJson(json.get("query"));

                // Unable to process result
                if (qw.isEmpty())
                    this.addError(780, "This query matches everywhere");
                else {
                    this.spanQuery = qw.toQuery();
                    if (qw.isOptional())
                        this.addWarning(781, "Optionality of query is ignored");
                    if (qw.isNegative())
                        this.addWarning(782, "Exclusivity of query is ignored");
                };
                // Copy notifications from query
                this.copyNotificationsFrom(kq);
                kq.clearNotifications();
            }
            catch (QueryException q) {
                this.addError(q.getErrorCode(), q.getMessage());
            };
        }
        else
            this.addError(700, "No query given");

        // <legacycode>
        if (json.has("warning") &&
            json.get("warning").asText().length() > 0) {
            this.addWarning(799, json.get("warning").asText());
        };
        // </legacycode>

        // <legacycode>
        if (json.has("warnings")) {
            JsonNode warnings = json.get("warnings");
            for (JsonNode node : warnings)
                if (node.asText().length() > 0)
                    this.addWarning(799, node.asText());
        };
        // </legacycode>

        // Copy notifications from request
        this.copyNotificationsFrom(json);
	    
        // virtual collections
        if (json.has("collection")) {
            this.setCollection(
                new KorapCollection().fromJson(json.get("collection"))
            );
        }

        // <legacycode>
        else if (json.has("collections")) {
            KorapCollection kc = new KorapCollection();
            for (JsonNode collection : json.get("collections")) {
                kc.fromJsonLegacy(collection);
            };

            this.setCollection(kc);
        };
        // </legacycode>


        // No errors
        if (!this.hasErrors()) {

            // Parse meta section
            if (json.has("meta")) {
                JsonNode meta = json.get("meta");

                // Defined count
                if (meta.has("count"))
                    this.setCount(meta.get("count").asInt());

                // Defined startIndex
                if (meta.has("startIndex"))
                    this.setStartIndex(meta.get("startIndex").asInt());

                // Defined startPage
                if (meta.has("startPage"))
                    this.setStartPage(meta.get("startPage").asInt());

                // Defined cutOff
                if (meta.has("cutOff"))
                    this.setCutOff(meta.get("cutOff").asBoolean());

                // Defined contexts
                if (meta.has("context"))
                    this.context.fromJson(meta.get("context"));

                // Defined resource count
                if (meta.has("timeout"))
                    this.setTimeOut(meta.get("timeout").asLong());

                // Defined resource count
                if (meta.has("itemsPerResource"))
                    this.setItemsPerResource(
                        meta.get("itemsPerResource").asInt()
                    );

                // Defined highlights
                if (meta.has("highlight")) {

                    // Add highlights
                    if (meta.get("highlight").isArray()) {
                        for (JsonNode highlight : (JsonNode) meta.get("highlight")) {
                            this.addHighlight(highlight.asInt());
                        };
                    }
                    else
                        this.addHighlight(meta.get("highlight").asInt());
                };

                // Only lift a limited amount of fields from the metadata
                if (meta.has("fields")) {
                        
                    // Remove legacy default fields
                    this.fields.clear();

                    // Add fields
                    if (meta.get("fields").isArray()) {
                        for (JsonNode field : (JsonNode) meta.get("fields")) {
                            this.addField(field.asText());
                        };
                    }
                    else
                        this.addField(meta.get("fields").asText());
                };
            };
        };
        return this;
    };


    public KorapSearch fromJson (String jsonString) throws QueryException {
        ObjectMapper mapper = new ObjectMapper();
        try {
            this.request = mapper.readTree(jsonString);
            this.fromJson(this.request);
        }

        // Unable to parse JSON
        catch (IOException e) {
            this.addError(621, "Unable to parse JSON");
        };

        return this;
    };


    public long getTimeOut () {
        return this.timeout;
    };

    public void setTimeOut (long timeout) {
        this.timeout = timeout;
    };

    public SpanQuery getSpanQuery () {
        return this.spanQuery;
    };

    public JsonNode getRequest () {
        return this.request;
    };

    public KorapSearch setSpanQuery (SpanQueryWrapper sqwi) {
        // this.copyNotifications(sqwi);
        try {
            this.spanQuery = sqwi.toQuery();
        }
        catch (QueryException q) {
            this.addError(q.getErrorCode(), q.getMessage());
        };
        return this;
    };

    public KorapSearch setSpanQuery (SpanQuery sq) {
        this.spanQuery = sq;
        return this;
    };

    public SearchContext getContext () {
        return this.context;
    };

    public KorapSearch setContext (SearchContext context) {
        this.context = context;
        return this;
    };

    public int getStartIndex () {
        return this.startIndex;
    };
    
    public KorapSearch setStartIndex (int value) {
        this.startIndex = (value >= 0) ? value : 0;
        return this;
    };

    public KorapSearch setStartPage (int value) {
        if (value >= 0)
            this.setStartIndex((value * this.getCount()) - this.getCount());
        else
            this.startIndex = 0;
        return this;
    };

    public short getCount () {
        return this.count;
    };

    public short getCountMax () {
        return this.countMax;
    };

    public int getLimit () {
        return this.limit;
    };

    public KorapSearch setLimit (int limit) {
        if (limit > 0)
            this.limit = limit;
        return this;
    };

    public boolean doCutOff () {
        return this.cutOff;
    };

    public KorapSearch setCutOff (boolean cutOff) {
        this.cutOff = cutOff;
        return this;
    };

    public KorapSearch setCount (int value) {
        // Todo: Maybe update startIndex with known startPage!
        this.setCount((short) value);
        return this;
    };

    public KorapSearch setCount (short value) {
        if (value > 0)
            this.count = (value <= this.countMax) ? value : this.countMax;
        return this;
    };

    public KorapSearch setItemsPerResource (short value) {
        if (value >= 0)
            this.itemsPerResource = value;
        return this;
    };

    public KorapSearch setItemsPerResource (int value) {
        return this.setItemsPerResource((short) value);
    };

    public short getItemsPerResource () {
        return this.itemsPerResource;
    };


    // Get set of fields
    /**
     * Get the fields as a set
     */
    public HashSet<String> getFields () {
        return this.fields;
    };


    /**
     * Add a field to the set of fields to retrieve.
     *
     * @param field The field to retrieve.
     * @return The {@link KorapSearch} object for chaining.
     */
    public KorapSearch addField (String field) {
        this.fields.add(field);
        return this;
    };


    /**
     * Add class numbers to highlight in KWIC view.
     *
     * @param classNumber The number of a class to highlight.
     * @return The {@link KorapSearch} object for chaining.
     */
    public KorapSearch addHighlight (int classNumber) {
        this.highlights.add(classNumber);
        return this;
    };


    public KorapSearch setCollection (KorapCollection kc) {
        this.collection = kc;
        
        // Move messages from the collection
        this.copyNotificationsFrom(kc);
        kc.clearNotifications();
        return this;
    };

    public KorapCollection getCollection () {
        if (this.collection == null)
            this.collection = new KorapCollection();
        return this.collection;
    };


    /**
     * Apply the KoralQuery to an index.
     */
    public KorapResult apply (KorapIndex ki) {
        if (this.spanQuery == null) {
            KorapResult kr = new KorapResult();
            kr.setRequest(this.request);

            if (this.hasErrors())
                kr.copyNotificationsFrom(this);
            else
                kr.addError(700, "No query given");
            return kr;
        };

        if (this.hasErrors()) {
            KorapResult kr = new KorapResult();

            // TODO: dev mode
            kr.setRequest(this.request);
            kr.copyNotificationsFrom(this);
            return kr;
        };

        this.getCollection().setIndex(ki);
        KorapResult kr = ki.search(this);
        kr.setRequest(this.request);
        kr.copyNotificationsFrom(this);
        this.clearNotifications();
        return kr;
    };
};
