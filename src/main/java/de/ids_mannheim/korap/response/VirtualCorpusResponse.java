package de.ids_mannheim.korap.response;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class VirtualCorpusResponse extends Response {

    public JsonNode createKoralQueryForField (String fieldName,
            List<String> fieldValues) {

        ArrayNode arrayNode = mapper.createArrayNode();
        for (String v : fieldValues) {
            arrayNode.add(v);
        }

        ObjectNode collectionNode = mapper.createObjectNode();
        collectionNode.put("@type", "koral:doc");
        collectionNode.put("key", fieldName);
        collectionNode.put("type", "type:string");
        collectionNode.set("value", arrayNode);

        ObjectNode root = mapper.createObjectNode();
        root.put("@context", KORAL_VERSION);
        root.set("corpus", collectionNode);

        return (JsonNode) root;
    }
}
