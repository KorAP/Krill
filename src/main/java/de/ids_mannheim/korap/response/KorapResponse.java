package de.ids_mannheim.korap.response;

import java.util.*;
import java.io.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.ids_mannheim.korap.response.Notifications;

/**
 * Base class for objects meant to be responded by the server.
 *
 * <p>
 * <blockquote><pre>
 *   KorapResponse km = new KorapResponse();
 *   System.out.println(
 *     km.toJSON()
 *   );
 * </pre></blockquote>
 *
 * @author Nils Diewald
 * @see de.ids_mannheim.korap.response.Notifications
 */
public class KorapResponse extends Notifications {
    ObjectMapper mapper = new ObjectMapper();

    // TODO: Add timeout!!!

    private String version, name, node, listener;
    private String benchmark;
    private boolean timeExceeded = false;


    /**
     * Construct a new KorapResponse object.
     *
     * @return The new KorapResponse object
     */
    public KorapResponse () {};

    /**
     * Get string representation of the backend's version.
     *
     * @return String representation of the backend's version
     */
    @JsonIgnore
    public String getVersion () {
	if (this.version == null)
	    return null;
	return this.version;
    };

    /**
     * Set the string representation of the backend's version.
     *
     * @param version The string representation of the backend's version
     * @return KorapResponse object for chaining
     */
    @JsonIgnore
    public KorapResponse setVersion (String version) {
	this.version = version;
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
	if (this.name == null)
	    return null;
	return this.name;
    };

    /**
     * Set the string representation of the backend's name.
     * All nodes in a cluster should have the same backend name.
     *
     * @param version The string representation of the backend's name
     * @return KorapResponse object for chaining
     */
    @JsonIgnore
    public KorapResponse setName (String name) {
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
     * @param version The string representation of the node's name
     * @return KorapResponse object for chaining
     */
    @JsonIgnore
    public KorapResponse setNode (String name) {
	this.node = name;
	return this;
    };


    @JsonIgnore
    public void setTimeExceeded (boolean timeout) {
	if (timeout)
	    this.addWarning(682, "Search time exceeded");
	this.timeExceeded = timeout;
    };


    @JsonIgnore
    public boolean getTimeExceeded () {
	return this.timeExceeded;
    };


    @JsonIgnore
    public KorapResponse setBenchmark (long t1, long t2) {
        this.benchmark =
                (t2 - t1) < 100_000_000 ?
	    // Store as miliseconds
	    (((double) (t2 - t1) * 1e-6) + " ms") :
	    // Store as seconds
	    (((double) (t2 - t1) / 1000000000.0) + " s");
	return this;
    };

    @JsonIgnore
    public KorapResponse setBenchmark (String bm) {
	this.benchmark = bm;
	return this;
    };

    @JsonIgnore
    public String getBenchmark () {
        return this.benchmark;
    };

    @JsonIgnore
    public KorapResponse setListener (String listener) {
	this.listener = listener;
	return this;
    };

    @JsonIgnore
    public String getListener () {
	return this.listener;
    }

    /**
     * Serialize response to JSON node.
     */
    @Override
    public JsonNode toJSONnode () {

	// Get notifications json response
	ObjectNode json = (ObjectNode) super.toJSONnode();

	StringBuilder sb = new StringBuilder();
        if (this.getName() != null) {
	    sb.append(this.getName());

	    if (this.getVersion() != null)
		sb.append("-").append(this.getVersion());
	}
        else if (this.getVersion() != null) {
	    sb.append(this.getVersion());
	};

	if (this.timeExceeded)
	    json.put("timeExceeded", true);

	if (sb.length() > 0)
	    json.put("version", sb.toString());

        if (this.getNode() != null)
            json.put("node", this.getNode());

        if (this.getListener() != null)
            json.put("listener", this.getListener());

        if (this.getBenchmark() != null)
            json.put("benchmark", this.getBenchmark());

	return (JsonNode) json;
    };
};
