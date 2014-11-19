package de.ids_mannheim.korap.response;

import java.util.LinkedList;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;

public class Message implements Cloneable {
    // Mapper for JSON serialization
    ObjectMapper mapper = new ObjectMapper();

    private String msg;
    private int code = 0;
    private LinkedList<String> parameters;

    public Message (int code, String msg) {
	this.code = code;
	this.msg  = msg;
    };

    public Message () {};

    @JsonIgnore
    public void setMessage (String msg) {
	this.msg = msg;
    };

    @JsonIgnore
    public String getMessage () {
	return this.msg;
    };

    @JsonIgnore
    public void setCode (int code) {
	this.code = code;
    };

    @JsonIgnore
    public int getCode () {
	return this.code;
    };

    public void addParameter (String param) {
	if (this.parameters == null)
	    this.parameters = new LinkedList<String>();
	this.parameters.add(param);
    };

    public Object clone () throws CloneNotSupportedException
    {
	Message clone = new Message();
	if (this.msg != null)
	    clone.msg = this.msg;

	clone.code = this.code;

	if (this.parameters != null) {
	    for (String p : this.parameters) {
		clone.addParameter(p);
	    };
	};

	return clone;
    };

    
    public JsonNode toJSONnode () {
	ArrayNode message = mapper.createArrayNode();

	if (this.code != 0)
	    message.add(this.getCode());

	message.add(this.getMessage());
	if (parameters != null)
	    for (String p : parameters)
		message.add(p);
	return (JsonNode) message;
    };

    /**
     * Get JSON string
     */
    public String toJSON () {
	String msg = "";
	try {
	    return mapper.writeValueAsString(this.toJSONnode());
	}
	catch (Exception e) {
	    msg = ", \"" + e.getLocalizedMessage() + "\"";
	};

	return
	    "[620, " +
	    "\"Unable to generate JSON\"" + msg + "]";
    };
};
