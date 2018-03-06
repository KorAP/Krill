package de.ids_mannheim.korap.response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;

import org.apache.lucene.index.*;

/**
 * Class representing a meta field.
 */
public class MetaField {

	// Mapper for JSON serialization
    ObjectMapper mapper = new ObjectMapper();

	public String type = "type:string";
	public String key;
	public List<String> values = new ArrayList<>();

	public MetaField (String key) {
		this.key = key;
	};

	/**
	 * Create JsonNode
	 */
	public JsonNode toJsonNode () {
        ObjectNode json = mapper.createObjectNode();
		json.put("@type", "koral:field");
		json.put("type", this.type);
		json.put("key", this.key);

		// Value is numerical
		if (this.type.equals("type:number")) {

			// Value is a list
			if (this.values.size() > 1) {
				ArrayNode list = json.putArray("value");

				Iterator vIter = this.values.iterator();
				while (vIter.hasNext()) {
					list.add((int) Integer.parseInt((String) vIter.next()));
				};
			}

			// Value is a single
			else {
				json.put("value", Integer.parseInt(this.values.get(0)));
			};
		}

		// Value is textual or keywords
		else {
			// Value is a list
			if (this.values.size() > 1) {
				ArrayNode list = json.putArray("value");

				Iterator vIter = this.values.iterator();
				while (vIter.hasNext()) {
					list.add((String) vIter.next());
				};
			}

			// Value is a single
			else if (this.values.size() > 0) {
				json.put("value", this.values.get(0));
			};
		};

		return (JsonNode) json;
	};
};
