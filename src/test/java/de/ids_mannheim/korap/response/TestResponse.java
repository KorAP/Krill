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
};
