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
 * A unified notification class for KorAP related errors,
 * warnings and messages.
 * 
 * <p>
 * The object contains lists of errors, warnings and messages
 * and new warnings, errors or messages are appended to these lists.
 * 
 * <p>
 * <blockquote><pre>
 * Notifications n = new Notifications();
 * n.addWarning(456, "Something went wrong");
 * if (n.hasWarnings()) {
 * for (Message msg : n.getWarnings())
 * System.err.out(msg.getCode() + ": " + msg.getMessage());
 * };
 * System.err.println(n.toJsonString());
 * </pre></blockquote>
 * 
 * @author Nils Diewald
 * @see de.ids_mannheim.korap.response.Messages
 */
/*
 * This will be inherited most of the time as Java does not support roles
 * and I have no idea how to do this more elegantly.
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Notifications {

    // Create object mapper for JSON generation
    ObjectMapper mapper = new ObjectMapper();

    private Messages warnings, errors, messages;


    /**
     * Check for warnings.
     * 
     * @return <tt>true</tt> in case there are warnings, otherwise
     *         <tt>false</tt>
     */
    public boolean hasWarnings () {
        if (this.warnings == null || this.warnings.size() == 0)
            return false;
        return true;
    };


    /**
     * Return all warnings.
     * 
     * @return {@link Messages} representing all warnings
     */
    public Messages getWarnings () {
        return this.warnings;
    };


    /**
     * Set warnings by means of a {@link JsonNode}.
     * 
     * @param msgs
     *            JSON array of warnings.
     * @return {@link Notifications} object for chaining.
     */
    public Notifications setWarnings (JsonNode msgs) {
        for (JsonNode msg : msgs)
            this.addWarning(msg);
        return this;
    };



    /**
     * Return a specific warning based on an index.
     * 
     * @param index
     *            The index of the warning in the list of warnings.
     * @return The message in case it exists, otherwise
     *         <code>null</code>
     */
    public Message getWarning (int index) {
        if (this.warnings != null)
            return this.warnings.get(index);
        return (Message) null;
    };


    /**
     * Appends a new warning.
     * 
     * @param code
     *            Integer code representation of the warning
     * @param msg
     *            String representation of the warning
     * @param terms
     *            Optional strings of additional information
     * @return Notification object for chaining
     */
    public Notifications addWarning (int code, String msg, String ... terms) {
        if (this.warnings == null)
            this.warnings = new Messages();
        this.warnings.add(code, msg, terms);
        return this;
    };


    /**
     * Appends a new warning.
     * 
     * @param node
     *            {@link JsonNode} representing a warning message
     * @return Notification object for chaining
     */
    public Notifications addWarning (JsonNode node) {
        if (this.warnings == null)
            this.warnings = new Messages();

        try {
            this.warnings.add(node);
        }
        catch (QueryException qe) {
            this.warnings.add(qe.getErrorCode(), qe.getMessage());
        };

        return this;
    };


    /**
     * Appends new warnings.
     * 
     * @param msgs
     *            {@link Messages} representing multiple warnings
     * @return Notification object for chaining
     */
    public Notifications addWarnings (Messages msgs) {
        if (this.warnings == null)
            this.warnings = msgs;
        else
            this.warnings.add(msgs);
        return this;
    };


    /**
     * Return all errors.
     * 
     * @return The {@link Messages} object representing all errors
     */
    public Messages getErrors () {
        return this.errors;
    };


    /**
     * Set errors by means of a {@link JsonNode}.
     * 
     * @param msgs
     *            JSON array of errors.
     * @return Notifications object for chaining.
     */
    public Notifications setErrors (JsonNode msgs) {
        for (JsonNode msg : msgs)
            this.addError(msg);
        return this;
    };


    /**
     * Return a specific error based on an index.
     * 
     * @param index
     *            The index of the error in the list of errors.
     * @return The message in case it exists, otherwise
     *         <code>null</code>
     */
    public Message getError (int index) {
        if (this.errors != null)
            return this.errors.get(index);
        return (Message) null;
    };


    /**
     * Check for errors.
     * 
     * @return <tt>true</tt> in case there are errors, otherwise
     *         <tt>false</tt>
     */
    public boolean hasErrors () {
        if (this.errors == null || this.errors.size() == 0)
            return false;
        return true;
    };


    /**
     * Appends a new error.
     * 
     * @param code
     *            Integer code representation of the error
     * @param msg
     *            String representation of the error
     * @param terms
     *            Optional strings of additional information
     * @return Notification object for chaining
     */
    public Notifications addError (int code, String msg, String ... terms) {
        if (this.errors == null)
            this.errors = new Messages();
        this.errors.add(code, msg, terms);
        return this;
    };


    /**
     * Appends a new error.
     * 
     * @param node
     *            {@link JsonNode} representing an error message
     * @return Notification object for chaining
     */
    public Notifications addError (JsonNode msg) {
        if (this.errors == null)
            this.errors = new Messages();
        try {
            this.errors.add(msg);
        }
        catch (QueryException qe) {
            this.errors.add(qe.getErrorCode(), qe.getMessage());
        };

        return this;
    };


    /**
     * Appends new errors.
     * 
     * @param msgs
     *            {@link Messages} representing multiple errors
     * @return Notification object for chaining
     */
    public Notifications addErrors (Messages msgs) {
        if (this.errors == null)
            this.errors = msgs;
        else
            this.errors.add(msgs);
        return this;
    };


    /**
     * Return all messages.
     * 
     * @return {@link Messages} representing all messages
     */
    public Messages getMessages () {
        return this.messages;
    };


    /**
     * Set messages by means of a {@link JsonNode}.
     * 
     * @param msgs
     *            JSON array of messages.
     * @return Notifications object for chaining.
     */
    public Notifications setMessages (JsonNode msgs) {
        for (JsonNode msg : msgs)
            this.addMessage(msg);
        return this;
    };


    /**
     * Return a specific message based on an index.
     * 
     * @param index
     *            The index of the message in the list of messages.
     * @return The message in case it exists, otherwise
     *         <code>null</code>
     */
    public Message getMessage (int index) {
        if (this.messages != null)
            return this.messages.get(index);
        return (Message) null;
    };


    /**
     * Check for messages.
     * 
     * @return <tt>true</tt> in case there are messages, otherwise
     *         <tt>false</tt>
     */
    public boolean hasMessages () {
        if (this.messages == null || this.messages.size() == 0)
            return false;
        return true;
    };


    /**
     * Appends a new message.
     * 
     * @param code
     *            Integer code representation of the message
     * @param msg
     *            String representation of the message
     * @param terms
     *            Optional strings of additional information
     * @return Notification object for chaining
     */
    public Notifications addMessage (int code, String msg, String ... terms) {
        if (this.messages == null)
            this.messages = new Messages();
        this.messages.add(code, msg, terms);
        return this;
    };


    /**
     * Appends a new message.
     * 
     * @param node
     *            {@link JsonNode} representing a message
     * @return Notification object for chaining
     */
    public Notifications addMessage (JsonNode msg) {
        if (this.messages == null)
            this.messages = new Messages();
        try {
            this.messages.add(msg);
        }
        catch (QueryException qe) {
            this.messages.add(qe.getErrorCode(), qe.getMessage());
        };
        return this;
    };


    /**
     * Appends new messages.
     * 
     * @param msgs
     *            {@link Messages} representing multiple messages
     * @return Notification object for chaining
     */
    public Notifications addMessages (Messages msgs) {
        if (this.messages == null)
            this.messages = msgs;
        else
            this.messages.add(msgs);
        return this;
    };


    /**
     * Copy notifications from another notification object.
     * 
     * @param notes
     *            Notification object to copy notifications from.
     * @return Notification object for chaining
     */
    public Notifications copyNotificationsFrom (Notifications notes) {
        try {
            if (notes.hasErrors())
                this.addErrors((Messages) notes.getErrors().clone());
            if (notes.hasWarnings())
                this.addWarnings((Messages) notes.getWarnings().clone());
            if (notes.hasMessages())
                this.addMessages((Messages) notes.getMessages().clone());
        }
        catch (CloneNotSupportedException cnse) {};
        return this;
    };


    /**
     * Copy notifications from a {@link JsonNode} object.
     * 
     * @param request
     *            Notifications containing {@link JsonNode}.
     * @return Notification object for chaining
     */
    public Notifications copyNotificationsFrom (JsonNode request) {

        // Add warnings from JSON
        if (request.has("warnings") && request.get("warnings").isArray()) {
            JsonNode msgs = request.get("warnings");
            for (JsonNode msg : msgs)
                this.addWarning(msg);
        };

        // Add messages from JSON
        if (request.has("messages") && request.get("messages").isArray()) {
            JsonNode msgs = request.get("messages");
            if (msgs.isArray())
                for (JsonNode msg : msgs)
                    this.addMessage(msg);
        };

        // Add errors from JSON
        if (request.has("errors") && request.get("errors").isArray()) {
            JsonNode msgs = request.get("errors");
            if (msgs.isArray())
                for (JsonNode msg : msgs)
                    this.addError(msg);
        };

        return this;
    };


    /**
     * Move notifications from a passed {@link Notification} object
     * to the invocant.
     * 
     * @param notes
     *            Notification object.
     * @return The invocant object for chaining
     */
    public Notifications moveNotificationsFrom (Notifications notes) {
        this.copyNotificationsFrom(notes);
        notes.clearNotifications();
        return this;
    };


    /**
     * Clear all notifications.
     * 
     * @return Notification object for chaining
     */
    public Notifications clearNotifications () {
        if (this.warnings != null)
            this.warnings.clear();
        if (this.messages != null)
            this.messages.clear();
        if (this.errors != null)
            this.errors.clear();
        return this;
    };



    /**
     * Serialize Notifications as a {@link JsonNode}.
     * 
     * @return {@link JsonNode} representation of all warnings,
     *         errors, and messages.
     */
    public JsonNode toJsonNode () {
        ObjectNode json = mapper.createObjectNode();

        // Add messages
        if (this.hasWarnings())
            json.put("warnings", this.getWarnings().toJsonNode());
        if (this.hasErrors())
            json.put("errors", this.getErrors().toJsonNode());
        if (this.hasMessages())
            json.put("messages", this.getMessages().toJsonNode());

        return (JsonNode) json;
    };


    /**
     * Serialize Notifications as a JSON string.
     * <p>
     * <blockquote><pre>
     * {
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
     * @return String representation of all warnings, errors, and
     *         messages
     */
    public String toJsonString () {
        String msg = "";
        try {
            JsonNode node = this.toJsonNode();
            if (node == null)
                return "{}";
            return mapper.writeValueAsString(node);
        }
        catch (Exception e) {
            // Bad in case the message contains quotes!
            msg = ", \"" + e.getLocalizedMessage() + "\"";
        };

        return "{\"errors\" : [" + "[620, " + "\"Unable to generate JSON\""
                + msg + "]" + "]}";
    };
};
