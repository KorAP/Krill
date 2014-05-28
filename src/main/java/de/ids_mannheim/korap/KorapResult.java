package de.ids_mannheim.korap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.ids_mannheim.korap.index.PositionsToOffset;
import de.ids_mannheim.korap.index.SearchContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/*
TODO: Reuse the KorapSearch code for data serialization!
*/

public class KorapResult {
    ObjectMapper mapper = new ObjectMapper();

    public static final short ITEMS_PER_PAGE = 25;
    private String query;

    private List<KorapMatch> matches;

    private int totalResults = 0;
    private int startIndex = 0;

    private SearchContext context;

    private short itemsPerPage = ITEMS_PER_PAGE;
    private short itemsPerResource = 0;

    private String benchmarkSearchResults,
            benchmarkHitCounter;
    private String error = null;
    private String version;

    private JsonNode request;

    // Logger
    // This is KorapMatch instead of KorapResult!
    private final static Logger log = LoggerFactory.getLogger(KorapMatch.class);

    // Empty result
    public KorapResult() {
    }


    public KorapResult(String query,
                       int startIndex,
                       short itemsPerPage,
                       SearchContext context) {

        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        // mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        mapper.disable(SerializationFeature.WRITE_NULL_MAP_VALUES);

        this.matches = new ArrayList<>(itemsPerPage);
        this.query = query;
        this.startIndex = startIndex;
        this.itemsPerPage = (itemsPerPage > 50 || itemsPerPage < 1) ? ITEMS_PER_PAGE : itemsPerPage;
        this.context = context;
    }


    public void add(KorapMatch km) {
        this.matches.add(km);
    }


    public KorapMatch addMatch(PositionsToOffset pto, int localDocID, int startPos, int endPos) {
        KorapMatch km = new KorapMatch(pto, localDocID, startPos, endPos);

        // Temporary - should use the same interface like results
        // in the future:
        km.setContext(this.context);

        // Add pos for context
        // That's not really a good position for it,
        // to be honest ...
        // But maybe it will make the offset
        // information in the match be obsolete!

        // TODO:
    /*
    if (km.leftTokenContext) {
	    pto.add(localDocID, startPos - this.leftContextOffset);
	};
	if (km.rightTokenContext) {
	    pto.add(localDocID, endPos + this.rightContextOffset - 1);
	};
	*/

        this.add(km);
        return km;
    }


    public void setTotalResults(int i) {
        this.totalResults = i;
    }


    public int getTotalResults() {
        return this.totalResults;
    }


    @Deprecated
    public int totalResults() {
        return this.totalResults;
    }


    @JsonIgnore
    public void setVersion(String version) {
        this.version = version;
    }


    @JsonIgnore
    public String getVersion() {
        if (this.version == null)
            return null;
        return "lucene-backend-" + this.version;
    }


    public short getItemsPerPage() {
        return this.itemsPerPage;
    }


    @Deprecated
    public short itemsPerPage() {
        return this.itemsPerPage;
    }


    public String getError() {
        return this.error;
    }


    public void setError(String msg) {
        this.error = msg;
    }


    public void setRequest(JsonNode request) {
        this.request = request;
    }


    public JsonNode getRequest() {
        return this.request;
    }


    public void setBenchmarkSearchResults(long t1, long t2) {
        this.benchmarkSearchResults =
                (t2 - t1) < 100_000_000 ? (((double) (t2 - t1) * 1e-6) + " ms") :
                        (((double) (t2 - t1) / 1000000000.0) + " s");
    }


    public String getBenchmarkSearchResults() {
        return this.benchmarkSearchResults;
    }


    public void setBenchmarkHitCounter(long t1, long t2) {
        this.benchmarkHitCounter =
                (t2 - t1) < 100_000_000 ? (((double) (t2 - t1) * 1e-6) + " ms") :
                        (((double) (t2 - t1) / 1000000000.0) + " s");
    }


    public String getBenchmarkHitCounter() {
        return this.benchmarkHitCounter;
    }

    public void setItemsPerResource (short value) {
	this.itemsPerResource = value;
    };

    @JsonIgnore
    public short getItemsPerResource () {
	return this.itemsPerResource;
    };


    public String getQuery() {
        return this.query;
    }


    public KorapMatch getMatch(int index) {
        return this.matches.get(index);
    }


    public List<KorapMatch> getMatches() {
        return this.matches;
    }


    @Deprecated
    public KorapMatch match(int index) {
        return this.matches.get(index);
    }


    public int getStartIndex() {
        return startIndex;
    }


    public KorapResult setContext(SearchContext context) {
        this.context = context;
        return this;
    }


    @JsonIgnore
    public SearchContext getContext() {
        return this.context;
    }


    // Identical to KorapMatch!
    public String toJSON() {
        ObjectNode json = (ObjectNode) mapper.valueToTree(this);

        json.put("context", this.getContext().toJSON());

	if (this.itemsPerResource > 0)
	    json.put("itemsPerResource", this.itemsPerResource);

        if (this.version != null)
            json.put("version", this.version);

        try {
            return mapper.writeValueAsString(json);
        } catch (Exception e) {
            log.warn(e.getLocalizedMessage());
        }


        return "{}";
    }


};
