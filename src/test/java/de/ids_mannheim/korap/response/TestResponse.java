package de.ids_mannheim.korap.response;

import java.io.*;

import de.ids_mannheim.korap.response.Messages;
import de.ids_mannheim.korap.response.Notifications;
import de.ids_mannheim.korap.response.KorapResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

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
		KorapResponse resp = new KorapResponse();
		assertEquals("{}", resp.toJsonString());
		resp.setVersion("0.24");
        resp.setNode("Tanja");
        assertEquals("0.24",resp.getVersion());
        assertEquals("Tanja", resp.getNode());

        assertFalse(resp.hasWarnings());
        assertFalse(resp.hasMessages());
        assertFalse(resp.hasErrors());

        JsonNode respJson = mapper.readTree(resp.toJsonString());
        assertEquals("0.24", respJson.at("/version").asText());
        assertEquals("Tanja", respJson.at("/node").asText());

        resp.setName("Index");
        respJson = mapper.readTree(resp.toJsonString());
        assertEquals("Index-0.24", respJson.at("/version").asText());
        assertEquals("Tanja", respJson.at("/node").asText());

        resp.setBenchmark("took a while");
        resp.setListener("localhost:3000");
        respJson = mapper.readTree(resp.toJsonString());
        assertEquals("localhost:3000", respJson.at("/listener").asText());
        assertEquals("took a while", respJson.at("/benchmark").asText());
    };

    @Test
    public void testResponseNotifications () throws IOException {
        KorapResponse resp = new KorapResponse();
        assertEquals("{}", resp.toJsonString());
        resp.setVersion("0.24");
        resp.setNode("Tanja");
        assertEquals("0.24",resp.getVersion());
        assertEquals("Tanja", resp.getNode());

        assertFalse(resp.hasWarnings());
        assertFalse(resp.hasMessages());
        assertFalse(resp.hasErrors());

        JsonNode respJson = mapper.readTree(resp.toJsonString());
        assertEquals("0.24", respJson.at("/version").asText());
        assertEquals("Tanja", respJson.at("/node").asText());

        resp.addWarning(1, "Fehler 1");
        resp.addWarning(2, "Fehler 2");
        resp.addWarning(3, "Fehler 3");

        resp.addError(4, "Fehler 4");

        respJson = mapper.readTree(resp.toJsonString());
        assertEquals("0.24", respJson.at("/version").asText());
        assertEquals("Tanja", respJson.at("/node").asText());
        
        assertEquals("Fehler 1", respJson.at("/warnings/0/1").asText());
        assertEquals("Fehler 2", respJson.at("/warnings/1/1").asText());
        assertEquals("Fehler 3", respJson.at("/warnings/2/1").asText());
        assertEquals("Fehler 4", respJson.at("/errors/0/1").asText());
    };

    @Test
    public void testResponseDeserialzation () throws IOException {
        String jsonResponse = "{\"version\":\"0.38\"}";
        KorapResponse kresp = mapper.readValue(jsonResponse, KorapResponse.class);
        assertEquals("0.38", kresp.getVersion());
        assertNull(kresp.getName());
        assertEquals(jsonResponse, kresp.toJsonString());

        jsonResponse = "{\"version\":\"seaweed-0.49\"}";
        kresp = mapper.readValue(jsonResponse, KorapResponse.class);
        assertEquals("0.49", kresp.getVersion());
        assertEquals("seaweed", kresp.getName());
        assertTrue(kresp.toJsonString().contains("seaweed-0.49"));

        jsonResponse = "{\"version\":\"seaweed-\"}";
        kresp = mapper.readValue(jsonResponse, KorapResponse.class);
        assertEquals("seaweed-", kresp.getVersion());
        assertNull(kresp.getName());
        assertTrue(kresp.toJsonString().contains("seaweed-"));

        jsonResponse = "{\"timeExceeded\":true}";
        kresp = mapper.readValue(jsonResponse, KorapResponse.class);
        assertTrue(kresp.hasTimeExceeded());
        assertTrue(kresp.hasWarnings());

        jsonResponse = "{\"benchmark\":\"40.5s\", \"foo\":\"bar\"}";
        kresp = mapper.readValue(jsonResponse, KorapResponse.class);
        assertEquals("40.5s", kresp.getBenchmark());

        jsonResponse = "{\"listener\":\"10.0.10.14:678\", \"foo\":\"bar\"}";
        kresp = mapper.readValue(jsonResponse, KorapResponse.class);
        assertEquals("10.0.10.14:678", kresp.getListener());

        jsonResponse = "{\"node\":\"tanja\", \"foo\":\"bar\"}";
        kresp = mapper.readValue(jsonResponse, KorapResponse.class);
        assertEquals("tanja", kresp.getNode());

        jsonResponse = "{\"node\":\"tanja\", \"version\":\"seaweed-0.49\", " +
            " \"benchmark\":\"40.5s\",  \"listener\":\"10.0.10.14:678\"," +
            "\"timeExceeded\":true }";
        kresp = mapper.readValue(jsonResponse, KorapResponse.class);
        assertEquals("0.49", kresp.getVersion());
        assertEquals("seaweed", kresp.getName());
        assertEquals("40.5s", kresp.getBenchmark());
        assertEquals("10.0.10.14:678", kresp.getListener());
        assertEquals("tanja", kresp.getNode());
        assertTrue(kresp.hasTimeExceeded());
        assertTrue(kresp.hasWarnings());

        jsonResponse = "{\"warnings\":[[123,\"This is a warning\"]," +
            "[124,\"This is a second warning\"]],"+
            "\"errors\":[[125,\"This is a single error\"]], "+
            " \"node\":\"tanja\", \"version\":\"seaweed-0.49\", " +
            " \"benchmark\":\"40.5s\",  \"listener\":\"10.0.10.14:678\"," +
            "\"timeExceeded\":true }";
        kresp = mapper.readValue(jsonResponse, KorapResponse.class);
        assertTrue(kresp.hasWarnings());
        assertTrue(kresp.hasErrors());
        assertFalse(kresp.hasMessages());
        assertEquals(kresp.getError(0).getMessage(), "This is a single error");
        assertEquals(kresp.getWarning(0).getMessage(), "Response time exceeded");
        assertEquals(kresp.getWarning(1).getMessage(), "This is a warning");
        assertEquals(kresp.getWarning(2).getMessage(), "This is a second warning");
        assertEquals("0.49", kresp.getVersion());
        assertEquals("seaweed", kresp.getName());
        assertEquals("40.5s", kresp.getBenchmark());
        assertEquals("10.0.10.14:678", kresp.getListener());
        assertEquals("tanja", kresp.getNode());
        assertTrue(kresp.hasTimeExceeded());
    };
};
