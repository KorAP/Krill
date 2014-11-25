package de.ids_mannheim.korap.response;

import de.ids_mannheim.korap.util.QueryException;
import de.ids_mannheim.korap.response.Message;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;

import java.lang.*;
import java.util.*;

/**
 * A list of messages for Notifications.
 *
 * <p>
 * <blockquote><pre>
 *   Messages m = new Messages();
 *   m.add(614, "This is a new message");
 * </pre></blockquote>
 *
 * @author Nils Diewald
 * @see de.ids_mannheim.korap.response.Notifications
 * @see de.ids_mannheim.korap.response.Message
 */
public class Messages implements Cloneable, Iterable<Message> {

    // Create object mapper for JSON generation
    ObjectMapper mapper = new ObjectMapper();

    // List of messages
    private ArrayList<Message> messages;

    // Private class for iterator implementation
    private class MessageIterator implements Iterator<Message> {
	int index;

	// Constructor
        public MessageIterator () {
            this.index = 0;
        };

        @Override
	public boolean hasNext () {
            return this.index < messages.size();
        };

        @Override
	public Message next () {
            return messages.get(this.index++);
        };

        @Override
        public void remove () {
	    messages.remove(this.index);
        };
    };

    /**
     * Construct a new Messages object.
     */
    public Messages () {
	this.messages = new ArrayList<Message>(3);
    };

    /**
     * Get the iterator object.
     *
     * @return Iterator for Message object.
     */
    public Iterator<Message> iterator() {
        return new MessageIterator();
    };

    /**
     * Append a new message.
     *
     * @param code  Integer code representation of the warning
     * @param msg   String representation of the warning
     * @param terms Optional strings of additional information
     * @return New Message object
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
     * Append an existing message.
     *
     * @param msg Message object to be added. Message will be cloned.
     * @return Cloned Message object
     */
    public Message add (Message msg) {
	try {
	    Message msgClone = (Message) msg.clone();
	    messages.add(msgClone);
	    return msgClone;
	}
	catch (CloneNotSupportedException e) {
	};
	return (Message) null;
    };

    /**
     * Append an existing message comming from a JsonNode.
     *
     * @param node  <code>JsonNode</code> representing a message
     * @return New Message object
     * @throws QueryException if notification is not well formed (Error 750)
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
     * Append existing messages.
     *
     * @param msgs Messages object to be added. Messages will be cloned.
     * @return Messages object for chaining.
     */
    public Messages add (Messages msgs) {
	try {
	    for (Message msg : msgs.getMessages())
		this.add((Message) msg.clone());
	}
	catch (CloneNotSupportedException e) {
	};
	return this;
    };

    /**
     * Clear all messages.
     *
     * @return Messages object for chaining
     */
    public Messages clear () {
	this.messages.clear();
	return this;
    };

    /**
     * Get the number of the messages.
     *
     * @param Integer representing the number of messages in the list.
     */
    public int size () {
	return this.messages.size();
    };


    /**
     * Return a specific message based on an index.
     *
     * @param index The index of the message in the list of messages.
     * @return The message in case it exists, otherwise <code>null</code>
     */
    @JsonIgnore
    public Message get (int index) {
	if (index >= this.size())
	    return (Message) null;
	return this.messages.get(index);
    };

    /**
     * Return all messages.
     *
     * @return List of all Message objects
     */
    @JsonIgnore
    public List<Message> getMessages () {
	return this.messages;
    };


    /**
     * Create a clone of the Messages.
     *
     * @return The cloned messages object 
     * @throws CloneNotSupportedException if messages can't be cloned
     */
    public Object clone () throws CloneNotSupportedException {
	Messages clone = new Messages();
	for (Message m : this.messages) {
	    clone.add((Message) m.clone());
	};

	return clone;
    };

    /**
     * Serialize Messages as a JsonNode.
     *
     * @return JsonNode representation of all messages
     */
    public JsonNode toJSONnode () {
	ArrayNode messageArray = mapper.createArrayNode();
	for (Message msg : this.messages)
	    messageArray.add(msg.toJSONnode());
	return (JsonNode) messageArray;
    };


    /**
     * Serialize Messages as a JSON string.
     * <p>
     * <blockquote><pre>
     * [
     *   [123, "You are not allowed to serialize these messages"],
     *   [124, "Your request was invalid"]
     * ]
     * </pre></blockquote>
     *
     * @return String representation of all messages
     */
    public String toJSON () {
	String msg = "";
	try {
	    return mapper.writeValueAsString(this.toJSONnode());
	}
	catch (Exception e) {
	    // Bad in case the message contains quotes!
	    msg = ", \"" + e.getLocalizedMessage() + "\"";
	};

	return
	    "[620, " +
	    "\"Unable to generate JSON\"" + msg + "]";
    };
};
