package de.ids_mannheim.korap.response;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import de.ids_mannheim.korap.index.AbstractDocument;

/**
 * Representation of Texts in a Result.
 * <strong>Warning:</strong> This is currently highly dependent
 * on DeReKo data and will change in the future.
 * 
 * @author Nils Diewald
 * @see Result
 */
@JsonInclude(Include.NON_NULL)
public class Text extends AbstractDocument {

    // Logger
    private final static Logger log = LoggerFactory.getLogger(Text.class);

    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;

    // Mapper for JSON serialization
    ObjectMapper mapper = new ObjectMapper();


    public Text () {};


    public String toJsonString () {
        ObjectNode json = (ObjectNode) this.toJsonNode();

		ArrayNode fields = json.putArray("fields");
       
		// Iterate over all fields
		Iterator<MetaField> fIter = mFields.iterator();
		while (fIter.hasNext()) {
            MetaField mf = fIter.next();
            fields.add(mf.toJsonNode());

            // Legacy flat field support
            String mfs = mf.key;
            String value = this.getFieldValue(mfs);
            if (value != null)
                json.set(mfs, new TextNode(value));
		};

        this.addMessage(0, "Support for flat field values is eprecated");

        
        // Match was no match
        if (json.size() == 0)
            return "{}";
        try {
            return mapper.writeValueAsString(json);
        }
        catch (Exception e) {
            log.warn(e.getLocalizedMessage());
        };

        return "{}";
    };
};
