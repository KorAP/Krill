package de.ids_mannheim.korap;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import de.ids_mannheim.korap.index.PositionsToOffset;
import de.ids_mannheim.korap.index.SearchContext;
import de.ids_mannheim.korap.response.KorapResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/*
  TODO: Reuse the KorapSearch code for data serialization!
*/
/**
 * Response class for search results.
 *
 * @author diewald
 * @see KorapResponse
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class KorapResult extends KorapResponse {
    ObjectMapper mapper = new ObjectMapper();

    @JsonIgnore
    public static final short ITEMS_PER_PAGE     = 25;
    public static final short ITEMS_PER_PAGE_MAX = 100;

    private int startIndex = 0;
    private String query;

    private List<KorapMatch> matches;

    private SearchContext context;

    private short itemsPerPage = ITEMS_PER_PAGE,
              itemsPerResource = 0;

    private JsonNode request;

    // Logger
    // This is KorapMatch instead of KorapResult!
    private final static Logger log = LoggerFactory.getLogger(KorapMatch.class);


    /**
     * Construct a new KorapResult object.
     */
    public KorapResult() {
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    };


    /**
     * Construct a new KorapResult object.
     *
     * @param query Query representation as a string.
     * @param startIndex Offset position in match array.
     * @param itemsPerPage Number of matches per page.
     * @param context Requested {@link SearchContext}
     */
    public KorapResult(String query,
                       int startIndex,
                       short itemsPerPage,
                       SearchContext context) {

        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        // mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        // mapper.disable(SerializationFeature.WRITE_NULL_MAP_VALUES);

        this.matches = new ArrayList<>(itemsPerPage);
        this.query = query;
        this.startIndex = startIndex;
        this.itemsPerPage =
            (itemsPerPage > ITEMS_PER_PAGE_MAX || itemsPerPage < 1) ?
            ITEMS_PER_PAGE : itemsPerPage;
        this.context = context;
    };


    /**
     * Add a new match to the result set.
     *
     * @param match A {@link KorapMatch} to add.
     */
    public void add (KorapMatch km) {
        this.matches.add(km);
    };


    /**
     * Get number of items shown per page.
     *
     * @return Number of items shown per page.
     */
    public short getItemsPerPage () {
        return this.itemsPerPage;
    };


    /**
     * Set number of items shown per page.
     *
     * @param count Number of items shown per page.
     * @return {@link KorapResult} object for chaining.
     */
    public KorapResult setItemsPerPage (short count) {
        this.itemsPerPage = count;
        return this;
    };


    /**
     * Get serialized query as a {@link JsonNode}.
     *
     * @return {@link JsonNode} representation of the query object.
     */
    public JsonNode getRequest () {
        return this.request;
    };


    /**
     * Set serialized query as a {@link JsonNode}.
     *
     * @param request {@link JsonNode} representation of the query object.
     * @return {@link KorapResult} object for chaining.
     */    
    public KorapResult setRequest (JsonNode request) {
        this.request = request;
        return this;
    };


    @JsonIgnore
    public void setItemsPerResource (short value) {
	this.itemsPerResource = value;
    };

    @JsonIgnore
    public void setItemsPerResource (int value) {
	this.itemsPerResource = (short) value;
    };

    @JsonIgnore
    public short getItemsPerResource () {
	return this.itemsPerResource;
    };

    public String getQuery () {
        return this.query;
    };

    @JsonIgnore
    public KorapMatch getMatch (int index) {
        return this.matches.get(index);
    };

    @JsonIgnore
    public List<KorapMatch> getMatches() {
        return this.matches;
    };

    public int getStartIndex () {
        return startIndex;
    };

    @JsonIgnore
    public KorapResult setContext(SearchContext context) {
        this.context = context;
        return this;
    }


    @JsonIgnore
    public SearchContext getContext() {
        return this.context;
    }


    public JsonNode toJsonNode () {
	ObjectNode json = (ObjectNode) mapper.valueToTree(super.toJsonNode());

	if (this.context != null)
	    json.put("context", this.getContext().toJsonNode());

	if (this.itemsPerResource > 0)
	    json.put("itemsPerResource",
		     this.itemsPerResource);

	json.put("itemsPerPage",
		 this.itemsPerPage);

	// TODO: If test
	if (this.request != null)
	    json.put("request", this.request);

	// TODO: If test
	if (this.request != null)
	    json.put("request", this.request);
	if (this.query != null)
	    json.put("query", this.query);

	json.put("startIndex", this.startIndex);

	json.put("totalResults", this.getTotalResults());

	// Add matches
	if (this.matches != null)
	    json.putPOJO("matches", this.getMatches());

	return json;
    };


    // For Collocation Analysis API
    public String toTokenListJsonString () {
        ObjectNode json = (ObjectNode) mapper.valueToTree(this);

	ArrayNode array = json.putArray("matches");
	
	// Add matches as token lists
	for (KorapMatch km : this.getMatches()) {
	    array.add(km.toTokenList());
	};

        try {
            return mapper.writeValueAsString(json);
        }
	catch (Exception e) {
            log.warn(e.getLocalizedMessage());
        };

        return "{}";
    };

};
