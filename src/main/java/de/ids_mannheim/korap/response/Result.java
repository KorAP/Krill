package de.ids_mannheim.korap.response;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import de.ids_mannheim.korap.index.PositionsToOffset;

// Remove:
import de.ids_mannheim.korap.response.SearchContext;

import de.ids_mannheim.korap.response.Response;
import de.ids_mannheim.korap.response.Match;
import de.ids_mannheim.korap.Krill;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/*
  TODO: Reuse the Krill code for data serialization!
*/
/**
 * Response class for search results.
 * 
 * TODO: Synopsis and let it base on KoralQuery
 * 
 * @author diewald
 * @see Response
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Result extends Krill {
    ObjectMapper mapper = new ObjectMapper();

    @JsonIgnore
    public static final short ITEMS_PER_PAGE = 25;
    public static final short ITEMS_PER_PAGE_MAX = 100;

    private int startIndex = 0;
    private String serialQuery;

    private List<Match> matches;

    private SearchContext context;

    private short itemsPerPage = ITEMS_PER_PAGE, itemsPerResource = 0;

    private JsonNode request;

    // Logger
    // This is Match instead of Result!
    private final static Logger log = LoggerFactory.getLogger(Match.class);


    /**
     * Construct a new Result object.
     */
    public Result () {
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    };


    /**
     * Construct a new Result object.
     * 
     * @param serialQuery
     *            Query representation as a string.
     * @param startIndex
     *            Offset position in match array.
     * @param itemsPerPage
     *            Number of matches per page.
     * @param context
     *            Requested {@link SearchContext}
     */
    public Result (String query, int startIndex, short itemsPerPage,
                   SearchContext context) {

        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        // mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        // mapper.disable(SerializationFeature.WRITE_NULL_MAP_VALUES);

        this.matches = new ArrayList<>(itemsPerPage);
        this.serialQuery = query;
        this.startIndex = startIndex;
        this.itemsPerPage = (itemsPerPage > ITEMS_PER_PAGE_MAX || itemsPerPage < 1) ? ITEMS_PER_PAGE
                : itemsPerPage;
        this.context = context;
    };


    /**
     * Add a new match to the result set.
     * 
     * @param match
     *            A {@link Match} to add.
     */
    public void add (Match km) {
        this.matches.add(km);
    };


    /**
     * Get the number of items (documents) shown per page.
     * 
     * @return Number of items shown per page.
     */
    public short getItemsPerPage () {
        return this.itemsPerPage;
    };


    /**
     * Set the number of items (documents) shown per page.
     * 
     * @param count
     *            Number of items shown per page.
     * @return {@link Result} object for chaining.
     */
    public Result setItemsPerPage (short count) {
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
     * @param request
     *            {@link JsonNode} representation of the query object.
     * @return {@link Result} object for chaining.
     */
    public Result setRequest (JsonNode request) {
        this.request = request;
        return this;
    };


    /**
     * Get the number of items shown per resource (document).
     * Defaults to <tt>0</tt>, which is infinite.
     * 
     * @return The number of items shown per resource.
     */
    public short getItemsPerResource () {
        return this.itemsPerResource;
    };


    /**
     * Set the number of items (matches) shown per resource (text).
     * Defaults to <tt>0</tt>, which is infinite.
     * 
     * @param value
     *            The number of items shown per resource.
     * @return {@link Result} object for chaining.
     */
    public Result setItemsPerResource (short value) {
        this.itemsPerResource = value;
        return this;
    };


    /**
     * Set the number of items (matches) shown per resource
     * (document).
     * Defaults to <tt>0</tt>, which is infinite.
     * 
     * @param value
     *            The number of items shown per resource.
     * @return {@link Result} object for chaining.
     */
    public Result setItemsPerResource (int value) {
        this.itemsPerResource = (short) value;
        return this;
    };


    /**
     * Get the string representation of the search query.
     * 
     * @return The string representation of the search query.
     */
    public String getSerialQuery () {
        return this.serialQuery;
    };


    /**
     * Get a certain {@link Match} by index.
     * 
     * @param index
     *            The numerical index of the match,
     *            starts with <tt>0</tt>.
     * @return The {@link Match} object.
     */
    @JsonIgnore
    public Match getMatch (int index) {
        return this.matches.get(index);
    };


    /**
     * Get the list of {@link Match} matches.
     * 
     * @return The list of {@link Match} objects.
     */
    public List<Match> getMatches () {
        return this.matches;
    };


    /**
     * Get the number of the first match in the result set
     * (<i>aka</i> the offset). Starts with <tt>0</tt>.
     * 
     * @return The index number of the first match in the result set.
     */
    public int getStartIndex () {
        return startIndex;
    };


    /**
     * Get the context parameters of the search by means of a
     * {@link SearchContext} object.
     * 
     * @return The {@link SearchContext} object.
     */
    public SearchContext getContext () {
        return this.context;
    };


    /**
     * Set the context parameters of the search by means of a
     * {@link SearchContext} object.
     * 
     * @param context
     *            The {@link SearchContext} object providing
     *            search context parameters.
     * @return {@link Result} object for chaining.
     */
    public Result setContext (SearchContext context) {
        this.context = context;
        return this;
    };


    /**
     * Serialize the result set as a {@link JsonNode}.
     * 
     * @return {@link JsonNode} representation of the search results.
     */
    public JsonNode toJsonNode () {
        ObjectNode json = (ObjectNode) mapper.valueToTree(super.toJsonNode());
        this._addMeta(json);

        // Add matches
        if (this.matches != null)
            json.putPOJO("matches", this.getMatches());

        return json;
    };


    /**
     * Stringifies the matches to give a brief overview on
     * the result. Mainly used for testing.
     * 
     * @return The stringified matches
     */
    public String getOverview () {
        StringBuilder sb = new StringBuilder();

        sb.append("Search for: ").append(this.serialQuery).append("\n");

        int i = 1;

        // Add matches as bracket strings
        for (Match km : this.getMatches())
            sb.append(i++).append(": ").append(km.getSnippetBrackets())
                    .append(" (Doc ").append(km.getLocalDocID()).append(")\n");

        return sb.toString();
    };


    // For Collocation Analysis API
    @Deprecated
    public String toTokenListJsonString () {
        ObjectNode json = (ObjectNode) mapper.valueToTree(super.toJsonNode());
        // ObjectNode json = (ObjectNode) mapper.valueToTree(this);
        this._addMeta(json);

        ArrayNode array = json.putArray("matches");

        // Add matches as token lists
        for (Match km : this.getMatches())
            array.add(km.toTokenList());

        try {
            return mapper.writeValueAsString(json);
        }
        catch (Exception e) {
            log.warn(e.getLocalizedMessage());
        };

        return "{}";
    };


    private void _addMeta (ObjectNode json) {
        ObjectNode meta = json.has("meta") ? (ObjectNode) json.get("meta")
                : (ObjectNode) json.putObject("meta");


        // Relevant context setting
        if (this.context != null)
            meta.put("context", this.getContext().toJsonNode());

        // ItemsPerPage
        meta.put("itemsPerPage", this.itemsPerPage);

        // Relevant itemsPerResource setting
        if (this.itemsPerResource > 0)
            meta.put("itemsPerResource", this.itemsPerResource);

        // TODO: <test>
        /*
        if (this.request != null)
            json.put("request", this.request);
        */
        if (this.serialQuery != null)
            meta.put("serialQuery", this.serialQuery);
        // </test>


        meta.put("startIndex", this.startIndex);
    };
};
