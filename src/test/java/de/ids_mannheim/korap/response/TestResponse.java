package de.ids_mannheim.korap.response;

import java.io.*;

import de.ids_mannheim.korap.response.Messages;
import de.ids_mannheim.korap.response.Notifications;
import de.ids_mannheim.korap.response.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestResponse {

    ObjectMapper mapper = new ObjectMapper();


    @Test
    public void testResponse () throws IOException {
        Response resp = new Response();
        JsonNode respJson = mapper.readTree(resp.toJsonString());
        assertEquals(
                "http://korap.ids-mannheim.de/ns/KoralQuery/v0.3/context.jsonld",
                respJson.at("/@context").asText());
        assertEquals("", respJson.at("/meta").asText());

        resp.setVersion("0.24");
        resp.setNode("Tanja");
        assertEquals("0.24", resp.getVersion());
        assertEquals("Tanja", resp.getNode());

        assertFalse(resp.hasWarnings());
        assertFalse(resp.hasMessages());
        assertFalse(resp.hasErrors());

        respJson = mapper.readTree(resp.toJsonString());
        assertEquals("0.24", respJson.at("/meta/version").asText());
        assertEquals("Tanja", respJson.at("/meta/node").asText());

        resp.setName("Index");
        respJson = mapper.readTree(resp.toJsonString());
        assertEquals("Index-0.24", respJson.at("/meta/version").asText());
        assertEquals("Tanja", respJson.at("/meta/node").asText());

        resp.setBenchmark("took a while");
        resp.setListener("localhost:3000");
        respJson = mapper.readTree(resp.toJsonString());
        assertEquals("localhost:3000", respJson.at("/meta/listener").asText());
        assertEquals("took a while", respJson.at("/meta/benchmark").asText());
    };


    @Test
    public void testResponseNotifications () throws IOException {
        Response resp = new Response();
        JsonNode respJson = mapper.readTree(resp.toJsonString());
        assertEquals(
                "http://korap.ids-mannheim.de/ns/KoralQuery/v0.3/context.jsonld",
                respJson.at("/@context").asText());
        assertEquals("", respJson.at("/meta").asText());
        resp.setVersion("0.24");
        resp.setNode("Tanja");
        assertEquals("0.24", resp.getVersion());
        assertEquals("Tanja", resp.getNode());

        assertFalse(resp.hasWarnings());
        assertFalse(resp.hasMessages());
        assertFalse(resp.hasErrors());

        respJson = mapper.readTree(resp.toJsonString());
        assertEquals("0.24", respJson.at("/meta/version").asText());
        assertEquals("Tanja", respJson.at("/meta/node").asText());

        resp.addWarning(1, "Fehler 1");
        resp.addWarning(2, "Fehler 2");
        resp.addWarning(3, "Fehler 3");

        resp.addError(4, "Fehler 4");

        respJson = mapper.readTree(resp.toJsonString());
        assertEquals("0.24", respJson.at("/meta/version").asText());
        assertEquals("Tanja", respJson.at("/meta/node").asText());

        assertEquals("Fehler 1", respJson.at("/warnings/0/1").asText());
        assertEquals("Fehler 2", respJson.at("/warnings/1/1").asText());
        assertEquals("Fehler 3", respJson.at("/warnings/2/1").asText());
        assertEquals("Fehler 4", respJson.at("/errors/0/1").asText());
    };


    // TODO: Skip this for the moment and refactor later
    @Ignore
    public void testResponseDeserialzation () throws IOException {
        String jsonResponse = "{\"version\":\"0.38\"}";
        Response kresp = mapper.readValue(jsonResponse, Response.class);

        assertEquals("0.38", kresp.getVersion());
        assertNull(kresp.getName());
        assertEquals(jsonResponse, kresp.toJsonString());

        jsonResponse = "{\"meta\":{\"version\":\"seaweed-0.49\"}}";
        kresp = mapper.readValue(jsonResponse, Response.class);
        assertEquals("0.49", kresp.getVersion());
        assertEquals("seaweed", kresp.getName());
        assertTrue(kresp.toJsonString().contains("seaweed-0.49"));

        jsonResponse = "{\"version\":\"seaweed-\"}";
        kresp = mapper.readValue(jsonResponse, Response.class);
        assertEquals("seaweed-", kresp.getVersion());
        assertNull(kresp.getName());
        assertTrue(kresp.toJsonString().contains("seaweed-"));

        jsonResponse = "{\"timeExceeded\":true}";
        kresp = mapper.readValue(jsonResponse, Response.class);
        assertTrue(kresp.hasTimeExceeded());
        assertTrue(kresp.hasWarnings());

        jsonResponse = "{\"benchmark\":\"40.5s\", \"foo\":\"bar\"}";
        kresp = mapper.readValue(jsonResponse, Response.class);
        assertEquals("40.5s", kresp.getBenchmark());

        jsonResponse = "{\"listener\":\"10.0.10.14:678\", \"foo\":\"bar\"}";
        kresp = mapper.readValue(jsonResponse, Response.class);
        assertEquals("10.0.10.14:678", kresp.getListener());

        jsonResponse = "{\"node\":\"tanja\", \"foo\":\"bar\"}";
        kresp = mapper.readValue(jsonResponse, Response.class);
        assertEquals("tanja", kresp.getNode());

        jsonResponse = "{\"node\":\"tanja\", \"version\":\"seaweed-0.49\", "
                + " \"benchmark\":\"40.5s\",  \"listener\":\"10.0.10.14:678\","
                + "\"timeExceeded\":true }";
        kresp = mapper.readValue(jsonResponse, Response.class);
        assertEquals("0.49", kresp.getVersion());
        assertEquals("seaweed", kresp.getName());
        assertEquals("40.5s", kresp.getBenchmark());
        assertEquals("10.0.10.14:678", kresp.getListener());
        assertEquals("tanja", kresp.getNode());
        assertTrue(kresp.hasTimeExceeded());
        assertTrue(kresp.hasWarnings());

        jsonResponse = "{\"warnings\":[[123,\"This is a warning\"],"
                + "[124,\"This is a second warning\"]],"
                + "\"errors\":[[125,\"This is a single error\"]], "
                + " \"node\":\"tanja\", \"version\":\"seaweed-0.49\", "
                + " \"benchmark\":\"40.5s\",  \"listener\":\"10.0.10.14:678\","
                + "\"timeExceeded\":true }";
        kresp = mapper.readValue(jsonResponse, Response.class);
        assertTrue(kresp.hasWarnings());
        assertTrue(kresp.hasErrors());
        assertFalse(kresp.hasMessages());
        assertEquals("This is a single error", kresp.getError(0).getMessage());

        // THIS MAY BREAK!
        assertEquals("This is a warning", kresp.getWarning(0).getMessage());
        assertEquals(
            "This is a second warning",
            kresp.getWarning(1).getMessage());
        assertEquals(
            "Response time exceeded",
            kresp.getWarning(2).getMessage());
        assertEquals("0.49", kresp.getVersion());
        assertEquals("seaweed", kresp.getName());
        assertEquals("40.5s", kresp.getBenchmark());
        assertEquals("10.0.10.14:678", kresp.getListener());
        assertEquals("tanja", kresp.getNode());
        assertTrue(kresp.hasTimeExceeded());
    };


    @Test
    public void testResponseJSONadd () throws IOException {
        Response resp = new Response();
        ObjectNode jNode = mapper.createObjectNode();
        jNode.put("Hui", "works");
        resp.addJsonNode("test", jNode);
        JsonNode respJson = mapper.readTree(resp.toJsonString());

        assertEquals(
                "http://korap.ids-mannheim.de/ns/KoralQuery/v0.3/context.jsonld",
                respJson.at("/@context").asText());
        assertEquals("works", respJson.at("/test/Hui").asText());
    };
};
