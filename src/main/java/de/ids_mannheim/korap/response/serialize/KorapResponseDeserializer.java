package de.ids_mannheim.korap.response.serialize;

import java.util.*;
import java.io.*;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.DeserializationContext;

import de.ids_mannheim.korap.response.KorapResponse;

/**
 * JSON Deserialization class for Jackson, used by KorapResponse.
 * No direct usage intended.
 *
 * @author Nils Diewald
 * @see de.ids_mannheim.korap.KorapResponse
 */
public class KorapResponseDeserializer extends JsonDeserializer<KorapResponse> {

    /**
     * Deserialization of JSON format.
     *
     * @param parser A parser instance for consuming JSON.
     * @param ctxt A deserialization context.
     * @return The deserialized KorapResponse object.
     */ 

    @Override
    public KorapResponse deserialize (JsonParser parser, DeserializationContext ctxt)
        throws IOException, JsonProcessingException {
        JsonNode node = parser.getCodec().readTree(parser);
        KorapResponse kresp = new KorapResponse();

        // Deserialize version information
        if (node.has("version")) {
            String fullVersion = node.get("version").asText();
            int found = fullVersion.lastIndexOf('-');

            // Is combined name and version
            if (found > 0 && (found + 1 < fullVersion.length())) {
                kresp.setName(fullVersion.substring(0, found))
                    .setVersion(fullVersion.substring(found + 1));
            }
            // Is only version number
            else {
                kresp.setVersion(fullVersion);
            };
        };

        // Deserialize timeout information
        if (node.has("timeExceeded") && node.get("timeExceeded").asBoolean())
            kresp.setTimeExceeded(true);

        // Deserialize benchmark information
        if (node.has("benchmark"))
            kresp.setBenchmark(node.get("benchmark").asText());

        // Deserialize listener information
        if (node.has("listener"))
            kresp.setListener(node.get("listener").asText());

        // Deserialize listener information
        if (node.has("node"))
            kresp.setNode(node.get("node").asText());

        // Copy notifications
        kresp.copyNotificationsFrom(node);

        return kresp;
    };
};
