package de.ids_mannheim.korap.response;

import java.util.*;
import java.io.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.ids_mannheim.korap.response.Notifications;

public class KorapResponse extends Notifications {
    ObjectMapper mapper = new ObjectMapper();

    private String
	version,
	name,
	node,
	listener;
    private String benchmark;

    // add timeout!!!
    // remove totalResults, totalTexts

    public KorapResponse () {};

    /**
     * Get version of the index
     */
    @JsonIgnore
    public String getVersion () {
	if (this.version == null)
	    return null;
	return this.version;
    };

    /**
     * Set version number.
     *
     * @param version The version number of the index as
     *                a string representation.
     */
    @JsonIgnore
    public void setVersion (String version) {
	this.version = version;
    };

    /**
     * Get name the index
     */
    @JsonIgnore
    public String getName () {
	if (this.name == null)
	    return null;
	return this.name;
    };

    /**
     * Set name.
     *
     * @param name The name of the index as
     *             a string representation.
     */
    @JsonIgnore
    public void setName (String name) {
	this.name = name;
    };

    @JsonIgnore
    public String getNode () {
	return this.node;
    };

    @JsonIgnore
    public KorapResponse setNode (String name) {
	this.node = name;
	return this;
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
