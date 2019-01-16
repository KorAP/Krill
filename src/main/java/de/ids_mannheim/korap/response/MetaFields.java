package de.ids_mannheim.korap.response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import de.ids_mannheim.korap.index.AbstractDocument;

import org.apache.lucene.index.*;
import org.apache.lucene.document.Document;

public class MetaFields extends AbstractDocument {

    // Mapper for JSON serialization
    ObjectMapper mapper = new ObjectMapper();

    public MetaFields (String id) {
		this.addMessage(0, "Response format is temporary");
    };


    /**
     * Serialize response as a {@link JsonNode}.
     * 
     * @return {@link JsonNode} representation of the response
     */
    public JsonNode toJsonNode () {

		// Get notifications
        ObjectNode json = (ObjectNode) super.toJsonNode();

		ObjectNode doc = json.putObject("document");
		doc.put("@type", "koral:document");
		
		ArrayNode fields = doc.putArray("fields");
       
		// Iterate over all fields
		Iterator<MetaField> fIter = mFields.iterator();
		while (fIter.hasNext()) {
            MetaField mf = fIter.next();
            fields.add(mf.toJsonNode());
		};

		return json;
	};
};
