package de.ids_mannheim.korap.server;

import java.util.*;
import java.io.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

/*
  Todo: Ignore unstaged information as this may be incorrect in
  Multithreading environment.
*/

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class KorapResponse {
    ObjectMapper mapper = new ObjectMapper();

    private String errstr, msg, version, node, listener;
    private int err, unstaged;
    private int totalResults;
    private String benchmark;

    public KorapResponse (String node, String version) {
	this.setNode(node);
	this.setVersion(version);
    };

    public KorapResponse () {};

    @JsonIgnore
    public KorapResponse setError (int code, String msg) {
	this.err = code;
	this.errstr = msg;
	return this;
    };

    @JsonIgnore
    public KorapResponse setError (String msg) {
	this.err = 699;
	this.errstr = msg;
	return this;
    };

    public KorapResponse setErrstr (String msg) {
	this.errstr = msg;
	return this;
    };

    public String getErrstr () {
	return this.errstr;
    };

    public KorapResponse setErr (int num) {
	this.err = num;
	return this;
    };

    public int getErr () {
	return this.err;
    };

    public KorapResponse setMsg (String msg) {
	this.msg = msg;
	return this;
    };

    public String getMsg () {
	return this.msg;
    };

    public String getVersion () {
	return this.version;
    };

    public KorapResponse setVersion (String version) {
	this.version = version;
	return this;
    };

    public String getNode () {
	return this.node;
    };

    public KorapResponse setNode (String name) {
	this.node = name;
	return this;
    };

    public int getUnstaged () {
	return this.unstaged;
    };

    public KorapResponse setUnstaged (int unstaged) {
	this.unstaged = unstaged;
	return this;
    };

    public KorapResponse setTotalResults (int i) {
        this.totalResults = i;
	return this;
    };

    public KorapResponse incrTotalResults (int i) {
        this.totalResults += i;
	return this;
    };


    public int getTotalResults() {
        return this.totalResults;
    };

    @JsonIgnore
    public KorapResponse setBenchmark (long t1, long t2) {
        this.benchmark =
                (t2 - t1) < 100_000_000 ? (((double) (t2 - t1) * 1e-6) + " ms") :
                        (((double) (t2 - t1) / 1000000000.0) + " s");
	return this;
    };

    public KorapResponse setBenchmark (String bm) {
	this.benchmark = bm;
	return this;
    };

    public String getBenchmark () {
        return this.benchmark;
    };

    public KorapResponse setListener (String listener) {
	this.listener = listener;
	return this;
    };

    public String getListener () {
	return this.listener;
    }

    // Serialize
    public String toJSON () {
	ObjectNode json =  (ObjectNode) mapper.valueToTree(this);
	if (json.size() == 0)
	    return "{}";

	try {
	    return mapper.writeValueAsString(json);
	}
	catch (Exception e) {
	    this.errstr = e.getLocalizedMessage();
	};

	return "{\"errstr\" : \"" + this.errstr + "\"}";
    };
};
