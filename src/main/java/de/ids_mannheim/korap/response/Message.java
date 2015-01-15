package de.ids_mannheim.korap.response;

import java.util.LinkedList;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;

/**
 * A message for Notifications.
 *
 * <p>
 * <blockquote><pre>
 *   Message m = new Message();
 *   m.setCode(614);
 *   m.setMessage("This is a new message");
 *   m.addParameter("MyClass");
 * </pre></blockquote>
 *
 * @author Nils Diewald
 * @see de.ids_mannheim.korap.response.Messages
 */
public class Message implements Cloneable {
    // Mapper for JSON serialization
    ObjectMapper mapper = new ObjectMapper();

    private String msg;
    private int code = 0;
    private LinkedList<String> parameters;


    /**
     * Construct a new message object.
     *
     * @param code Code number representing the message code
     * @param msg String representation of the message
     * @return The new message object
     */
    public Message (int code, String msg) {
        this.code = code;
        this.msg  = msg;
    };

    /**
     * Construct a new message object.
     *
     * @return The new empty message object
     */
    public Message () {};


    /**
     * Return the string representation of the message.
     *
     * @return String representation of the message
     */
    @JsonIgnore
    public String getMessage () {
        return this.msg;
    };


    /**
     * Set the string representation of the message.
     *
     * @param msg String representation of the message
     * @return Message object for chaining
     */
    @JsonIgnore
    public Message setMessage (String msg) {
        this.msg = msg;
        return this;
    };


    /**
     * Return the integer code representation of the message.
     *
     * @return Integer code representation of the message
     */
    @JsonIgnore
    public int getCode () {
        return this.code;
    };



    /**
     * Set the integer representation of the message.
     *
     * @param code Integer code representation of the message
     * @return Message object for chaining
     */
    @JsonIgnore
    public Message setCode (int code) {
        this.code = code;
        return this;
    };


    /**
     * Add additional string parameters to the message.
     *
     * @return Message object for chaining
     */
    public Message addParameter (String param) {
        if (this.parameters == null)
            this.parameters = new LinkedList<String>();
        this.parameters.add(param);
        return this;
    };


    /**
     * Create a clone of the Message.
     *
     * @return The cloned message object 
     * @throws CloneNotSupportedException if message can't be cloned
     */
    public Object clone () throws CloneNotSupportedException {
        Message clone = new Message();

        // Copy message string
        if (this.msg != null)
            clone.msg = this.msg;

        // Copy message code
        clone.code = this.code;
        
        // Copy parameters
        if (this.parameters != null) {
            for (String p : this.parameters) {
                clone.addParameter(p);
            };
        };

        return clone;
    };

    /**
     * Serialize Message as a JsonNode.
     *
     * @return JsonNode representation of the message
     */
    public JsonNode toJsonNode () {
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
     * Serialize Message as a JSON string.
     * <p>
     * <blockquote><pre>
     * [123, "You are not allowed to serialize these messages"]
     * </pre></blockquote>
     *
     * @return String representation of the message
     */
    public String toJsonString () {
        String msg = "";
        try {
            return mapper.writeValueAsString(this.toJsonNode());
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
