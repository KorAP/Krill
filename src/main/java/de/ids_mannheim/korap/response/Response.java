package de.ids_mannheim.korap.response;

import static de.ids_mannheim.korap.util.KrillString.quote;

import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.ids_mannheim.korap.KrillCollection;
import de.ids_mannheim.korap.KrillMeta;
import de.ids_mannheim.korap.KrillQuery;
import de.ids_mannheim.korap.KrillStats;

/**
 * Base class for objects meant to be responded by the server.
 * This inherits KoralQuery requests.
 * 
 * <p>
 * <blockquote><pre>
 * Response km = new Response();
 * System.out.println(
 * km.toJsonString()
 * );
 * </pre></blockquote>
 * 
 * @author diewald
 * @see Notifications
 */
/*
 * TODO: Use configuration file to get default token field "tokens"
 * TODO: All these fields should be in meta!
*/
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Response extends Notifications {
    ObjectMapper mapper = new ObjectMapper();

    private KrillMeta meta;
    private KrillCollection collection;
    private KrillQuery query;
    private KrillStats stats;

    private String version, name, node, listener;

    private long totalResources = -2, // Not set
            totalResults = -2; // Not set
    private String benchmark;
    private boolean timeExceeded = false;

    private HashMap<String, ObjectNode> jsonFields;

    public static final String KORAL_VERSION = "http://korap.ids-mannheim.de/ns/KoralQuery/v0.3/context.jsonld";


    /**
     * Construct a new Response object.
     */
    public Response () {};


    /**
     * Get string representation of the backend's version.
     * 
     * @return String representation of the backend's version
     */
    @JsonIgnore
    public String getVersion () {
        return this.version;
    };


    /**
     * Set the string representation of the backend's version.
     * 
     * @param version
     *            The string representation of the backend's version
     * @return Response object for chaining
     */
    public Response setVersion (String fullVersion) {
        int found = fullVersion.lastIndexOf('-');

        // Is combined name and version
        if (found > 0 && (found + 1 < fullVersion.length())) {
            this.setName(fullVersion.substring(0, found));
            this.version = fullVersion.substring(found + 1);
        }
        // Is only version number
        else {
            this.version = fullVersion;
        };
        return this;
    };


    /**
     * Get string representation of the backend's name.
     * All nodes in a cluster should have the same backend name.
     * 
     * @return String representation of the backend's name
     */
    @JsonIgnore
    public String getName () {
        return this.name;
    };


    /**
     * Set the string representation of the backend's name.
     * All nodes in a cluster should have the same backend name.
     * 
     * @param name
     *            The string representation of the backend's name
     * @return Response object for chaining
     */
    public Response setName (String name) {
        this.name = name;
        return this;
    };


    /**
     * Get string representation of the node's name.
     * Each node in a cluster has a unique name.
     * 
     * @return String representation of the node's name
     */
    @JsonIgnore
    public String getNode () {
        return this.node;
    };


    /**
     * Set the string representation of the node's name.
     * Each node in a cluster has a unique name.
     * 
     * @param version
     *            The string representation of the node's name
     * @return Response object for chaining
     */
    public Response setNode (String name) {
        this.node = name;
        return this;
    };


    /**
     * Check if the response time was exceeded.
     * 
     * @return <tt>true</tt> in case the response had a timeout,
     *         otherwise <tt>false</tt>
     */
    @JsonIgnore
    public boolean hasTimeExceeded () {
        return this.timeExceeded;
    };


    /**
     * Set to <tt>true</tt> if time is exceeded
     * based on a timeout.
     * 
     * <p>
     * Will add a warning (682) to the output.
     * 
     * @param timeout
     *            Either <tt>true</tt> or <tt>false</tt>,
     *            in case the response timed out
     * @return Response object for chaining
     */
    public Response setTimeExceeded (boolean timeout) {
        if (timeout)
            this.addWarning(682, "Response time exceeded");
        this.timeExceeded = timeout;
        return this;
    };


    /**
     * Get the benchmark time as a string.
     * 
     * @return String representation of the benchmark
     *         (including trailing time unit)
     */
    @JsonIgnore
    public String getBenchmark () {
        return this.benchmark;
    };


    /**
     * Set the benchmark as timestamp differences.
     * 
     * @param ts1
     *            Starting time of the benchmark
     * @param ts2
     *            Ending time of the benchmark
     * @return Response object for chaining
     */
    @JsonIgnore
    public Response setBenchmark (long ts1, long ts2) {
        this.benchmark = (ts2 - ts1) < 100_000_000 ?
        // Store as miliseconds
                (((double) (ts2 - ts1) * 1e-6) + " ms") :
                // Store as seconds
                (((double) (ts2 - ts1) / 1000000000.0) + " s");
        return this;
    };


    /**
     * Set the benchmark as a string representation.
     * 
     * @param bm
     *            String representation of a benchmark
     *            (including trailing time unit)
     * @return Response for chaining
     */
    public Response setBenchmark (String bm) {
        this.benchmark = bm;
        return this;
    };


    /**
     * Get the listener URI as a string.
     * 
     * @return The listener URI as a string representation
     */
    @JsonIgnore
    public String getListener () {
        return this.listener;
    };


    /**
     * Set the listener URI as a String. This is probably the
     * localhost
     * with an arbitrary port, like
     * 
     * <p>
     * <blockquote><pre>
     * http://localhost:8080/
     * </pre></blockquote>
     * 
     * @param listener
     *            String representation of the listener URI
     * @return Response object for chaining
     */
    public Response setListener (String listener) {
        this.listener = listener;
        return this;
    };


    /**
     * Get the total number of results.
     * 
     * @return The total number of results.
     */
    @JsonIgnore
    public long getTotalResults () {
        if (this.totalResults == -2)
            return (long) 0;
        return this.totalResults;
    };


    /**
     * Set the total number of results.
     * 
     * @param results
     *            The total number of results.
     * @return {link Response} object for chaining.
     */
    public Response setTotalResults (long results) {
        this.totalResults = results;
        return this;
    };


    /**
     * Increment the total number of results by a certain value.
     * 
     * @param incr
     *            The number of results the total number should
     *            be incremented by.
     * @return {@link Response} object for chaining.
     */
    public Response incrTotalResults (int incr) {
        if (this.totalResults < 0)
            this.totalResults = incr;
        else
            this.totalResults += incr;
        return this;
    };


    /**
     * Get the total number of resources the total number of
     * results occur in.
     * 
     * @return The total number of resources the total number of
     *         results occur in.
     */
    @JsonIgnore
    public long getTotalResources () {
        if (this.totalResources == -2)
            return (long) 0;
        return this.totalResources;
    };


    /**
     * Set the total number of resources the total number of
     * results occur in.
     * 
     * @param resources
     *            The total number of resources the total
     *            number of results occur in.
     * @return {@link Response} object for chaining.
     */
    public Response setTotalResources (long resources) {
        this.totalResources = resources;
        return this;
    };


    /**
     * Increment the total number of resources the total number
     * of results occur in by a certain value.
     * 
     * @param incr
     *            The number of resources the total number of
     *            results occur in should be incremented by.
     *            (I don't care that this isn't English!)
     * @return {@link Response} object for chaining.
     */
    public Response incrTotalResources (int i) {
        if (this.totalResources < 0)
            this.totalResources = i;
        else
            this.totalResources += i;
        return this;
    };


    /**
     * Get the KoralQuery query object.
     * 
     * @return The {@link KrillQuery} object,
     *         representing the KoralQuery query object.
     */
    // TODO: "tokens" shouldn't be fixed.
    @JsonIgnore
    public KrillQuery getQuery () {
        if (this.query == null)
            this.query = new KrillQuery("tokens");
        return this.query;
    };


    /**
     * Set the KoralQuery query object.
     * 
     * @param query
     *            The {@link KrillQuery} object,
     *            representing the KoralQuery query object.
     * @return The {@link Response} object for chaining
     */
    @JsonIgnore
    public Response setQuery (KrillQuery query) {
        this.query = query;

        // Move messages from the query
        return (Response) this.moveNotificationsFrom(query);
    };


    /**
     * Get the associated collection object.
     * In case no collection information was defined yet,
     * a new {@link KrillCollection} object will be created.
     * 
     * @return The attached {@link KrillCollection} object.
     */
    @JsonIgnore
    public KrillCollection getCollection () {
        if (this.collection == null)
            this.collection = new KrillCollection();
        return this.collection;
    };


    /**
     * Set a new {@link KrillCollection} object.
     * 
     * @param collection
     *            A {@link KrillCollection} object.
     * @return The {@link Response} object for chaining
     */
    @JsonIgnore
    public Response setCollection (KrillCollection collection) {
        this.collection = collection;

        // Move messages from the collection
        Response resp = (Response) this.moveNotificationsFrom(collection);

        return resp;
    };


    /**
     * Get the associated meta object.
     * In case no meta information was defined yet,
     * a new {@link KrillMeta} object will be created.
     * 
     * @return The attached {@link KrillMeta} object.
     */
    @JsonIgnore
    public KrillMeta getMeta () {
        if (this.meta == null)
            this.meta = new KrillMeta();
        return this.meta;
    };


    /**
     * Set a new {@link KrillMeta} object.
     * 
     * @param meta
     *            A {@link KrillMeta} object.
     * @return The {@link Response} object for chaining
     */
    @JsonIgnore
    public Response setMeta (KrillMeta meta) {
        this.meta = meta;

        // Move messages from the collection
        return (Response) this.moveNotificationsFrom(meta);
    };


    /**
     * Get the associated statistics object.
     * In case no statistics information was defined yet,
     * a new {@link KrillStats} object will be created.
     * 
     * @return The attached {@link KrillStats} object.
     */
    @JsonIgnore
    public KrillStats getStats () {
        if (this.stats == null)
            this.stats = new KrillStats();
        return this.stats;
    };


    /**
     * Set a new {@link KrillStats} object.
     * 
     * @param stats
     *            A {@link KrillStats} object.
     * @return The {@link Response} object for chaining
     */
    @JsonIgnore
    public Response setStats (KrillStats stats) {
        this.stats = stats;

        // Move messages from the stats
        return (Response) this.moveNotificationsFrom(stats);
    };
    
    public void addJsonNode (String key, ObjectNode value) {
        if (this.jsonFields == null)
            this.jsonFields = new HashMap<String, ObjectNode>(4);
        this.jsonFields.put(key, value);
    };


    /**
     * Serialize response as a {@link JsonNode}.
     * 
     * @return {@link JsonNode} representation of the response
     */
    @Override
    public JsonNode toJsonNode () {
        // Get notifications json response
        ObjectNode json = (ObjectNode) super.toJsonNode();

        json.put("@context", KORAL_VERSION);

        StringBuilder sb = new StringBuilder();
        if (this.getName() != null) {
            sb.append(this.getName());

            if (this.getVersion() != null)
                sb.append("-");
        };

        // No name but version is given
        if (this.getVersion() != null)
            sb.append(this.getVersion());

        // KoralQuery meta object
        if (this.meta != null) {
            JsonNode metaNode = this.meta.toJsonNode();
            if (metaNode != null)
                json.set("meta", metaNode);
        };

        ObjectNode meta = json.has("meta") ? (ObjectNode) json.get("meta")
                : (ObjectNode) json.putObject("meta");

        if (sb.length() > 0)
            meta.put("version", sb.toString());

        if (this.timeExceeded)
            meta.put("timeExceeded", true);

        if (this.getNode() != null)
            meta.put("node", this.getNode());

        if (this.getListener() != null)
            meta.put("listener", this.getListener());

        if (this.getBenchmark() != null)
            meta.put("benchmark", this.getBenchmark());

        // totalResources is set
        if (this.totalResources != -2)
            meta.put("totalResources", this.totalResources);

        // totalResults is set
        if (this.totalResults != -2)
            meta.put("totalResults", this.totalResults);

        // Add json fields as passed to the object
        if (this.jsonFields != null) {
            json.setAll(this.jsonFields);
        };

        // KoralQuery query object
        if (this.query != null) {
            JsonNode queryNode = this.getQuery().toJsonNode();
            if (queryNode != null)
                json.set("query", queryNode);
        };

        // KoralQuery collection object
        if (this.collection != null) {
            // && this.collection.getFilters().toArray().length > 0) {
            JsonNode collNode = this.collection.toJsonNode();
            if (collNode != null) {
            	if (collection.isCorpus) {
            		json.set("corpus", collNode);
            	}
                // EM: legacy
            	else {
            		json.set("collection", collNode);
            	}
            };
        };

        return (JsonNode) json;
    };


    /**
     * Serialize response as a JSON string.
     * <p>
     * <blockquote><pre>
     * {
     * "version" : "Lucene-Backend-0.49.1",
     * "timeExceeded" : true,
     * "node" : "Tanja",
     * "listener" : "http://localhost:8080/",
     * "benchmark" : "12.3s",
     * "errors": [
     * [123, "You are not allowed to serialize these messages"],
     * [124, "Your request was invalid"]
     * ],
     * "messages" : [
     * [125, "Class is deprecated", "Notifications"]
     * ]
     * }
     * </pre></blockquote>
     * 
     * @return String representation of the response
     */
    public String toJsonString () {
        String msg = "";
        try {
            return mapper.writeValueAsString(this.toJsonNode());
        }
        catch (Exception e) {
            // Bad in case the message contains quotes!
            msg = ", " + quote(e.getLocalizedMessage());
        };

        return "{\"errors\":[" + "[620, " + "\"Unable to generate JSON\"" + msg
                + "]" + "]}";
    };
};
