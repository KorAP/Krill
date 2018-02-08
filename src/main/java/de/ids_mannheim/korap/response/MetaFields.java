package de.ids_mannheim.korap.response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import de.ids_mannheim.korap.index.AbstractDocument;

import java.util.*;

import org.apache.lucene.index.*;

@JsonInclude(Include.NON_NULL)
public class MetaFields extends AbstractDocument {

	// Logger
	private final static Logger log = LoggerFactory.getLogger(MetaFields.class);

	// This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;

	// Mapper for JSON serialization
    ObjectMapper mapper = new ObjectMapper();

	private Map<String, MetaField> fieldsMap = new HashMap<>();

	public MetaFields (String id) {
		this.addMessage(0, "Response format is temporary");		
	};


	/**
	 * Add field to collection
	 */
	public void add (IndexableField iField) {
					
		IndexableFieldType iFieldType = iField.fieldType();

		// Field type needs to be restored heuristically
		// - though that's not very elegant

		// Ignore non-stored fields
		if (!iFieldType.stored())
			return;

		MetaField mf = new MetaField(iField.name());

		// Reuse existing metafield
		if (fieldsMap.containsKey(mf.key)) {
			mf = fieldsMap.get(mf.key);
		}

		// Add new field
		else {
			fieldsMap.put(mf.key, mf);
		};
		
		// TODO: Check if metaField exists for that field

		Number n = iField.numericValue();
		String s = iField.stringValue();

		// Field has numeric value (possibly a date)
		if (n != null) {

			// TODO:
			//   check if the number is a date!
			mf.type = "type:number";
			mf.values.add(n.toString());
		}
		
		// Field has a textual value
		else if (s != null) {

			// Field is not indexed
			if (iFieldType.indexOptions() == IndexOptions.NONE) {
				mf.type = "type:store";
			}
			else if (iFieldType.indexOptions() != IndexOptions.DOCS) {
				mf.type = "type:text";
			};

			mf.values.add(s.toString());
		}
		
		else {
			log.error("Unknown field type {}", iField.name());
		};
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
		Iterator fIter = fieldsMap.keySet().iterator();
		while (fIter.hasNext()) {
			// System.err.println(fIter.next());
			MetaField mf = fieldsMap.get(fIter.next());
			// System.err.println(mf.type);
			fields.add(mf.toJsonNode());
		};

		return json;
	};
};
