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

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class KorapResult extends KorapResponse {
    ObjectMapper mapper = new ObjectMapper();

    @JsonIgnore
    public static final short ITEMS_PER_PAGE = 25;

    private int totalResults;
    private long totalTexts;

    private String query;

    private List<KorapMatch> matches;

    private int startIndex = 0;

    private SearchContext context;

    private short itemsPerPage = ITEMS_PER_PAGE;
    private short itemsPerResource = 0;

    private String benchmarkSearchResults,
            benchmarkHitCounter;
    private String error = null;

    private JsonNode request;


    // Logger
    // This is KorapMatch instead of KorapResult!
    private final static Logger log = LoggerFactory.getLogger(KorapMatch.class);

    // Empty result
    public KorapResult() {
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    };

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
        this.itemsPerPage = (itemsPerPage > 50 || itemsPerPage < 1) ?
	    ITEMS_PER_PAGE : itemsPerPage;
        this.context = context;
    };


    public void add (KorapMatch km) {
        this.matches.add(km);
    };

    public KorapMatch addMatch (PositionsToOffset pto,
				int localDocID,
				int startPos,
				int endPos) {
        KorapMatch km = new KorapMatch(pto, localDocID, startPos, endPos);

        // Temporary - should use the same interface like results
        // in the future:
        km.setContext(this.context);
        this.add(km);
        return km;
    };

    @Deprecated
    public int totalResults() {
        return this.getTotalResults();
    };

    public short getItemsPerPage() {
        return this.itemsPerPage;
    };

    @Deprecated
    public short itemsPerPage() {
        return this.itemsPerPage;
    };

    public void setRequest(JsonNode request) {
        this.request = request;
    };

    public JsonNode getRequest() {
        return this.request;
    };

    /*
    @JsonIgnore
    public void setBenchmarkHitCounter(long t1, long t2) {
        this.benchmarkHitCounter =
                (t2 - t1) < 100_000_000 ? (((double) (t2 - t1) * 1e-6) + " ms") :
                        (((double) (t2 - t1) / 1000000000.0) + " s");
    };

    public void setBenchmarkHitCounter(String bm) {
	this.benchmarkHitCounter = bm;
    };

    public String getBenchmarkHitCounter() {
        return this.benchmarkHitCounter;
    };

    */

    // Make this working in a KorapResult class
    // that is independent from search and collection
    public KorapResult setTotalTexts (long i) {
        this.totalTexts = i;
	return this;
    };

    public long getTotalTexts() {
        return this.totalTexts;
    };

    public KorapResult setTotalResults (int i) {
        this.totalResults = i;
	return this;
    };

    public KorapResult incrTotalResults (int i) {
        this.totalResults += i;
	return this;
    };

    public int getTotalResults() {
        return this.totalResults;
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

    @Deprecated
    public KorapMatch match (int index) {
        return this.matches.get(index);
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

    public JsonNode toJSONnode () {
	ObjectNode json = (ObjectNode) mapper.valueToTree(super.toJSONnode());

	if (this.context != null)
	    json.put("context", this.getContext().toJSON());

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
    public String toTokenListJSON () {
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
