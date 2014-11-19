package de.ids_mannheim.korap.response;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;

import java.util.*;
import java.io.*;
import de.ids_mannheim.korap.response.Message;
import de.ids_mannheim.korap.response.Messages;
import de.ids_mannheim.korap.util.QueryException;

/**
 * Unified notification class for KorAP related errors,
 * warnings and messages.
 *
 * @author Nils Diewald
 */

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Notifications {

    // Create object mapper for JSON generation
    ObjectMapper mapper = new ObjectMapper();

    private Messages warnings,
	             errors,
	             messages;

    /**
     * Add a new warning.
     *
     * @param code Integer code representation of the warning
     * @param msg String representation of the warning
     */
    public void addWarning (int code, String msg, String ... terms) {
	if (this.warnings == null)
	    this.warnings = new Messages();
	this.warnings.add(code, msg, terms);
    };

    public void addWarning (JsonNode msg) {
	if (this.warnings == null)
	    this.warnings = new Messages();

	try {
	    this.warnings.add(msg);
	}
	catch (QueryException qe) {
	    this.warnings.add(qe.getErrorCode(), qe.getMessage());
	};
    };

    /**
     * Add new warnings.
     *
     * @param msgs Array representation of the warning
     */
    public void addWarnings (Messages msgs) {
	if (this.warnings == null)
	    this.warnings = msgs;
	else
	    this.warnings.add(msgs);
    };


    /**
     * Get all warnings.
     */
    public Messages getWarnings () {
	return this.warnings;
    };


    public Message getWarning (int index) {
	if (this.warnings != null)
	    return this.warnings.get(index);
	return (Message) null;
    };


    /**
     * Check for warnings.
     */
    public boolean hasWarnings () {
	if (this.warnings == null || this.warnings.size() == 0)
	    return false;
	return true;
    };

    /**
     * Add a new error.
     *
     * @param code Integer code representation of the error
     * @param msg String representation of the error
     */
    public void addError (int code, String msg, String ... terms) {
	if (this.errors == null)
	    this.errors = new Messages();
	this.errors.add(code, msg, terms);
    };

    public void addError (JsonNode msg) {
	if (this.errors == null)
	    this.errors = new Messages();
	try {
	    this.errors.add(msg);
	}
	catch (QueryException qe) {
	    this.errors.add(qe.getErrorCode(), qe.getMessage());
	};
    };


    /**
     * Add new warnings.
     *
     * @param msgs Array representation of the warning
     */
    public void addErrors (Messages msgs) {
	if (this.errors == null)
	    this.errors = msgs;
	else
	    this.errors.add(msgs);
    };


    /**
     * Get all errors.
     */
    public Messages getErrors () {
	return this.errors;
    };

    public Message getError (int index) {
	if (this.errors != null)
	    return this.errors.get(index);
	return (Message) null;
    };

    /**
     * Check for errors.
     */
    public boolean hasErrors () {
	if (this.errors == null || this.errors.size() == 0)
	    return false;
	return true;
    };


    /**
     * Add a new message.
     *
     * @param code Integer code representation of the message
     * @param msg String representation of the message
     */
    public void addMessage (int code, String msg, String ... terms) {
	if (this.messages == null)
	    this.messages = new Messages();
	this.messages.add(code, msg, terms);
    };

    public void addMessage (JsonNode msg) {
	if (this.messages == null)
	    this.messages = new Messages();
	try {
	    this.messages.add(msg);
	}
	catch (QueryException qe) {
	    this.messages.add(qe.getErrorCode(), qe.getMessage());
	};
    };

    /**
     * Add new warnings.
     *
     * @param msgs Array representation of the warning
     */
    public void addMessages (Messages msgs) {
	if (this.messages == null)
	    this.messages = msgs;
	else
	    this.messages.add(msgs);
    };


    /**
     * Get all messages.
     */
    public Messages getMessages () {
	return this.messages;
    };

    public Message getMessage (int index) {
	if (this.messages != null)
	    return this.messages.get(index);
	return (Message) null;
    };


    /**
     * Check for messages.
     */
    public boolean hasMessages () {
	if (this.messages == null || this.messages.size() == 0)
	    return false;
	return true;
    };


    /**
     * Copy notifications
     */
    public void copyNotificationsFrom (Notifications notes) {
	try {
	    if (notes.hasErrors())
		this.addErrors((Messages) notes.getErrors().clone());
	    if (notes.hasWarnings())
		this.addWarnings((Messages) notes.getWarnings().clone());
	    if (notes.hasMessages())
		this.addMessages((Messages) notes.getMessages().clone());
	}
	catch (CloneNotSupportedException cnse) {
	};
	return;
    };


    /**
     * Copy notifications from JsonNode
     */
    public void copyNotificationsFrom (JsonNode request) {

	// Add warnings from JSON
	if (request.has("warnings") &&
	    request.get("warnings").isArray()) {
	    JsonNode msgs = request.get("warnings");
	    for (JsonNode msg : msgs)
		this.addWarning(msg);
	};

	// Add messages from JSON
	if (request.has("messages") &&
	    request.get("messages").isArray()) {
	    JsonNode msgs = request.get("messages");
	    if (msgs.isArray())
		for (JsonNode msg : msgs)
		    this.addMessage(msg);
	};

	// Add errors from JSON
	if (request.has("errors") &&
	    request.get("errors").isArray()) {
	    JsonNode msgs = request.get("errors");
	    if (msgs.isArray())
		for (JsonNode msg : msgs)
		    this.addError(msg);
	};
    };


    /**
     * Serialize response to JSON node.
     */
    public JsonNode toJSONnode () {
	ObjectNode json =  mapper.createObjectNode();

	// Add messages
	if (this.hasWarnings())
	    json.put("warnings", this.getWarnings().toJSONnode());
	if (this.hasErrors())
	    json.put("errors", this.getErrors().toJSONnode());
	if (this.hasMessages())
	    json.put("messages", this.getMessages().toJSONnode());

	return (JsonNode) json;
    };

    /**
     * Serialize response to JSON string.
     */
    public String toJSON () {
	String msg = "";
	try {
	    JsonNode node = this.toJSONnode();
	    if (node == null)
		return "{}";
	    return mapper.writeValueAsString(node);
	}
	catch (Exception e) {
	    msg = ", \"" + e.getLocalizedMessage() + "\"";
	};

	return
	    "{\"errors\" : [" +
	    "[620, " +
	    "\"Unable to generate JSON\"" + msg + "]" +
	    "]}";
    };

    /**
     * Clear all notifications.
     */
    public void clearNotifications () {
	if (this.warnings != null)
	    this.warnings.clear();
	if (this.messages != null)
	    this.messages.clear();
	if (this.errors != null)
	    this.errors.clear();
    };


    // Remove:
    @Deprecated
    public void addError (String msg) {
	System.err.println("DEPRECATED " + msg);
    };

    @Deprecated
    public void addWarning (String msg) {
	System.err.println("DEPRECATED " + msg);
    };

    @Deprecated
    public void addMessage (String msg) {
	System.err.println("DEPRECATED " + msg);
    };

    @Deprecated
    public void setError (String msg) {
	System.err.println("DEPRECATED " + msg);
    };

    @Deprecated
    public void setWarning (String msg) {
	System.err.println("DEPRECATED " + msg);
    };

    @Deprecated
    public void setMessage (String msg) {
	System.err.println("DEPRECATED " + msg);
    };

    @Deprecated
    public void setError (int code, String msg) {
	System.err.println("DEPRECATED " + msg);
    };

    @Deprecated
    public void setWarning (int code, String msg) {
	System.err.println("DEPRECATED " + msg);
    };

    @Deprecated
    public void setMessage (int code, String msg) {
	System.err.println("DEPRECATED " + msg);
    };
};
