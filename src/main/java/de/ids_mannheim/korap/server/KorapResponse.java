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
public class KorapResponse {
    ObjectMapper mapper = new ObjectMapper();

    private String errstr, msg, version, node;
    private int err, unstaged = 0;

    public KorapResponse (String node, String version) {
	this.setNode(node);
	this.setVersion(version);
    };

    public KorapResponse () {};

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
