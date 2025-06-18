package de.ids_mannheim.korap;

import java.io.*;
import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;

import de.ids_mannheim.korap.response.SearchContext;
import de.ids_mannheim.korap.util.QueryException;
import de.ids_mannheim.korap.response.Notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//  Todo: Set timeout default value per config file
public final class KrillMeta extends Notifications {
    // <legacy>
    private boolean cutOff = false;
    // </legacy>

    private int limit = 0;
    private short count = 25, countMax = 50;
    private int startIndex = 0;
    private short itemsPerResource = 0;
    private SearchContext context;

    // Per default snippets are requested
    private boolean snippets = true;

    private boolean tokens = false;

    private ArrayList<String> fields;
    HashSet<Integer> highlights;

    private JsonNode rewrites;
    
    // Timeout search after milliseconds
    private long timeout = (long) 120_000;
    // private long timeoutStart = Long.MIN_VALUE;

    // Logger
    private final static Logger log = LoggerFactory.getLogger(Krill.class);

    {
        fields = new ArrayList<String>(16);

        // Lift following fields per default
        // These fields are chosen for
        // <legacy /> reasons
        for (String field : new String[] {
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
				// "foundries",
                // "tokenization",
                // New:
                "availability",
                "layerInfos",
				"docSigle",
				"corpusSigle"
			}) {
            fields.add(field);
        };

        // Classes used for highlights
        highlights = new HashSet<Integer>(3);
        context = new SearchContext();
    };


    public KrillMeta () {};


    public KrillMeta (JsonNode json) {
        this.fromJson(json);
    };


    public KrillMeta (String json) {
        try {
            this.fromJson(json);
        }
        catch (QueryException q) {
            this.addError(q.getErrorCode(), q.getMessage());
        };
    };


    public KrillMeta fromJson (String json) throws QueryException {
        JsonNode jsonN;
        try {
            // Read Json string
            jsonN = new ObjectMapper().readValue(json, JsonNode.class);
        }

        // Something went wrong
        catch (IOException e) {
            String msg = e.getMessage();
            log.warn("Unable to parse JSON: " + msg.split("\n")[0]);
            throw new QueryException(621, "Unable to parse JSON");
        };

        // Deserialize from node
        return this.fromJson(jsonN);
    };


    public KrillMeta fromJson (JsonNode json) {
        // The object type of meta is undefined in KoralQuery,
        // so it may or may have no @type

        // The query is nested in a parent query
        if (!json.has("@type") && json.has("meta"))
            json = json.get("meta");

        // Defined cutOff
        // <legacy>
        if (json.has("cutOff"))
            this.setCutOff(json.get("cutOff").asBoolean());
        // </legacy>

        // Defined count
        if (json.has("count"))
            this.setCount(json.get("count").asInt());

        // Defined startIndex
        if (json.has("startIndex"))
            this.setStartIndex(json.get("startIndex").asInt());

        // Defined startPage
        if (json.has("startPage"))
            this.setStartPage(json.get("startPage").asInt());

        // Defined timeout
        if (json.has("timeout"))
            this.setTimeOut(json.get("timeout").asLong());

        // Defined resource count
        if (json.has("itemsPerResource"))
            this.setItemsPerResource(json.get("itemsPerResource").asInt());

        // Defined snippets
        if (json.has("snippets")) {
            this.snippets = json.get("snippets").asBoolean();
        };

        // Defined tokens
        if (json.has("tokens")) {
            this.tokens = json.get("tokens").asBoolean();
        };
        
        // Defined context
        if (json.has("context"))
            this.context.fromJson(json.get("context"));

        if (json.has("rewrites"))
            this.rewrites = json.get("rewrites");
        
        // Defined highlights
        if (json.has("highlight")) {

            // Add highlights
            if (json.get("highlight").isArray()) {
                for (JsonNode highlight : (JsonNode) json.get("highlight")) {
                    this.addHighlight(highlight.asInt());
                };
            }
            else
                this.addHighlight(json.get("highlight").asInt());
        };

        // Defined fields to lift from the index
        if (json.has("fields")) {

            // Remove default fields
            this.fields.clear();

            // Add fields
            if (json.get("fields").isArray()) {
                for (JsonNode field : (JsonNode) json.get("fields")) {
                    this.addField(field.asText());
                };
            }
            else {
                this.addField(json.get("fields").asText());
            };
        };

        return this;
    };


    public short getCount () {
        return this.count;
    };


    public KrillMeta setCount (int value) {
        // Todo: Maybe update startIndex with known startPage!
        this.setCount((short) value);
        return this;
    };


    public KrillMeta setCount (short value) {
        if (value >= 0)
            this.count = (value <= this.countMax) ? value : this.countMax;
        return this;
    };


    public short getCountMax () {
        return this.countMax;
    };


    public int getStartIndex () {
        return this.startIndex;
    };


    public KrillMeta setStartIndex (int value) {
        this.startIndex = (value >= 0) ? value : 0;
        return this;
    };


    public KrillMeta setStartPage (int value) {
        if (value >= 0)
            this.setStartIndex((value * this.getCount()) - this.getCount());
        else
            this.startIndex = 0;
        return this;
    };


    public long getTimeOut () {
        return this.timeout;
    };


    public void setTimeOut (long timeout) {
        this.timeout = timeout;
    };


    public KrillMeta setItemsPerResource (short value) {
        if (value >= 0)
            this.itemsPerResource = value;
        return this;
    };


    public KrillMeta setItemsPerResource (int value) {
        return this.setItemsPerResource((short) value);
    };


    public short getItemsPerResource () {
        return this.itemsPerResource;
    };


    public SearchContext getContext () {
        return this.context;
    };


    public JsonNode getRewrites () {
        return this.rewrites;
    };    

    
    public KrillMeta setContext (SearchContext context) {
        this.context = context;
        return this;
    };


    /**
     * Get if snippets should be retrieved.
     */
    public boolean hasSnippets () {
        return this.snippets;
    };


    /**
     * Set if snippets should be retrieved.
     */
    public KrillMeta setSnippets (boolean snippets) {
        this.snippets = snippets;
        return this;
    };


    /**
     * Get if tokens should be retrieved.
     */
    public boolean hasTokens () {
        return this.tokens;
    };


    /**
     * Set if tokens should be retrieved.
     */
    public KrillMeta setTokens (boolean tokens) {
        this.tokens = tokens;
        return this;
    };
    

    // Get set of fields
    /**
     * Get the fields as a set
     */
    public ArrayList<String> getFields () {
        return this.fields;
    };


    /**
     * Add a field to the set of fields to retrieve.
     * 
     * @param field
     *            The field to retrieve.
     * @return The {@link Krill} object for chaining.
     */
    public KrillMeta addField (String field) {
        this.fields.add(field);
        return this;
    };


    /**
     * Add class numbers to highlight in KWIC view.
     * 
     * @param classNumber
     *            The number of a class to highlight.
     * @return The {@link Krill} object for chaining.
     */
    public KrillMeta addHighlight (int classNumber) {
        this.highlights.add(classNumber);
        return this;
    };


    @Deprecated
    public boolean doCutOff () {
        return this.cutOff;
    };


    @Deprecated
    public KrillMeta setCutOff (boolean cutOff) {
        this.cutOff = cutOff;
        return this;
    };


    // TODO:
    // This limits the search results with offset
    // Maybe can be deprecated!
    @Deprecated
    public int getLimit () {
        return this.limit;
    };


    // TODO:
    // This limits the search results with offset
    // Maybe can be deprecated!
    @Deprecated
    public KrillMeta setLimit (int limit) {
        if (limit > 0)
            this.limit = limit;
        return this;
    };


    @Override
    public JsonNode toJsonNode () {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode json = mapper.createObjectNode();
        // json.put("@type", "koral:meta");

        // <legacy>
        // Add cutOff attribute
        if (this.cutOff)
            json.put("cutOff", this.doCutOff());

        // Add limit attribute
        if (this.limit > 0)
            json.put("limit", this.getLimit());
        // </legacy>

        // Add count attribute
        json.put("count", this.getCount());

        // Add startindex attribute
        json.put("startIndex", this.getStartIndex());

        // Add timeout attribute
        json.put("timeout", this.getTimeOut());

        // Add context attribute
        json.set("context", this.getContext().toJsonNode());

        // Add fields attribute
        ArrayNode fieldNode = mapper.createArrayNode();
        Iterator<String> field = this.fields.iterator();
        while (field.hasNext())
            fieldNode.add(field.next());
        json.set("fields", fieldNode);

        // Add itemsPerResource attribute
        if (this.itemsPerResource > 0)
            json.put("itemsPerResource", (int) this.getItemsPerResource());

        // Add highlight attribute
        if (!this.highlights.isEmpty()) {
            ArrayNode highlightNode = mapper.createArrayNode();
            highlightNode.addPOJO(this.highlights);
            json.set("highlight", highlightNode);
        };

        if (this.getRewrites() != null)
            json.set("rewrites", this.getRewrites());
        
        return json;
    };
};
