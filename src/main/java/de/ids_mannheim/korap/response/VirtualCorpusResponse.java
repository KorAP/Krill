package de.ids_mannheim.korap.response;

import java.util.List;
import static de.ids_mannheim.korap.util.KrillString.quote;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.util.RawValue;

public class VirtualCorpusResponse extends Response {

    public JsonNode createKoralQueryForField (String fieldName,
            List<String> fieldValues) {

        StringBuffer sb = new StringBuffer("[");
        int size = fieldValues.size();
        for (int i=0; i < size; i++) {
            if (fieldValues.get(i).length() == 0)
                continue;
            sb.append(quote(fieldValues.get(i)));
            if (i < size-1) {
                sb.append(",");
            }
        }
        sb.append("]");

        ObjectNode collectionNode = mapper.createObjectNode();
        collectionNode.put("@type", "koral:doc");
        collectionNode.put("key", fieldName);
        collectionNode.put("type", "type:string");
        collectionNode.putRawValue("value", new RawValue(sb.toString()));

        ObjectNode root = mapper.createObjectNode();
        root.put("@context", KORAL_VERSION);
        root.set("corpus", collectionNode);

        return (JsonNode) root;
    }
}
