package de.ids_mannheim.korap.response;

import de.ids_mannheim.korap.util.QueryException;
import de.ids_mannheim.korap.response.Message;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Nils Diewald
 *
 * An array of messages.
 */
public class Messages implements Cloneable {

    // Mapper for JSON serialization
    ObjectMapper mapper = new ObjectMapper();
    private ArrayList<Message> messages;

    public Messages () {
	this.messages = new ArrayList<Message>(3);
    };

    /**
     * Add message
     */
    public Message add (int code,
			String message,
			String ... terms) {
	Message newMsg = new Message(code, message);
	messages.add(newMsg);
	if (terms != null)
	    for (String t : terms)
		newMsg.addParameter(t);
	return newMsg;
    };

    /**
     * Add message
     */
    public Message add (Message msg) {
	messages.add(msg);
	return msg;
    };

    /**
     * Add message usng JsonNode
     */
    public Message add (JsonNode msg) throws QueryException {
	if (!msg.isArray() || !msg.has(0))
	    throw new QueryException(
	        750, "Passed notifications are not well formed"
	    );

	// Valid message
	Message newMsg = new Message();
	short i = 1;
	if (msg.get(0).isNumber()) {
	    newMsg.setCode(msg.get(0).asInt());
	    if (!msg.has(1))
		throw new QueryException(
	            750, "Passed notifications are not well formed"
	        );
	    newMsg.setMessage(msg.get(1).asText());
	    i++;
	}
	else {
	    newMsg.setMessage(msg.get(0).asText());
	};

	// Add parameters
	while (msg.has(i))
	    newMsg.addParameter(msg.get(i++).asText());

	// Add messages to list
	this.add(newMsg);
	return newMsg;
    };


    /**
     * Add messages
     */
    public void add (Messages msgs) {
	for (Message msg : msgs.getMessages()) {
	    this.add(msg);
	};
    };

    /**
     * Clear all messages
     */
    public void clear () {
	messages.clear();
    };

    public int size () {
	return this.messages.size();
    };

    @JsonIgnore
    public Message get (int index) {
	return this.messages.get(index);
    };

    @JsonIgnore
    public List<Message> getMessages () {
	return this.messages;
    };

    public Object clone () throws CloneNotSupportedException
    {
	Messages clone = new Messages();
	for (Message m : this.messages) {
	    clone.add((Message) m.clone());
	};

	return clone;
    };

    /**
     * Get JSON node
     */
    public JsonNode toJSONnode () {
	ArrayNode messageArray = mapper.createArrayNode();
	for (Message msg : this.messages)
	    messageArray.add(msg.toJSONnode());
	return (JsonNode) messageArray;
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
