package de.ids_mannheim.korap.response;

import java.io.*;

import de.ids_mannheim.korap.response.Messages;
import de.ids_mannheim.korap.response.Notifications;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestNotifications {

    ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testNotification () {
	Notifications notes = new Notifications();
	assertEquals("{}", notes.toJSON());
    };

    @Test
    public void testNotificationWarnings () throws IOException {
	Notifications notes = new Notifications();
	assertFalse(notes.hasWarnings());
	assertFalse(notes.hasMessages());
	assertFalse(notes.hasErrors());

	notes.addWarning(613, "Foo");
	notes.addWarning(614, "Bar", "Spiel");

	assertEquals("{\"warnings\":[[613,\"Foo\"],[614,\"Bar\"," +
		     "\"Spiel\"]]}", notes.toJSON());

	assertTrue(notes.hasWarnings());
	assertFalse(notes.hasMessages());
	assertFalse(notes.hasErrors());

	notes.addError(412, "Test");

	assertTrue(notes.hasWarnings());
	assertFalse(notes.hasMessages());
	assertTrue(notes.hasErrors());

	JsonNode noteJson = mapper.readTree(notes.toJSON());

	// {"warnings":[[613,"Foo"],[614,"Bar","Spiel"]],"errors":[[412,"Test"]]}
	assertEquals(613, noteJson.at("/warnings/0/0").asInt());
	assertEquals("Foo", noteJson.at("/warnings/0/1").asText());
	assertEquals(614, noteJson.at("/warnings/1/0").asInt());
	assertEquals("Bar", noteJson.at("/warnings/1/1").asText());
	assertEquals("Spiel", noteJson.at("/warnings/1/2").asText());
	assertEquals(412, noteJson.at("/errors/0/0").asInt());
	assertEquals("Test", noteJson.at("/errors/0/1").asText());

	notes.addMessage(567, "Probe", "huhu", "hihi");

	assertTrue(notes.hasWarnings());
	assertTrue(notes.hasMessages());
	assertTrue(notes.hasErrors());

	noteJson = mapper.readTree(notes.toJSON());

	// {"warnings":[[613,"Foo"],[614,"Bar","Spiel"]],
	// "errors":[[412,"Test"]]}
	assertEquals(613, noteJson.at("/warnings/0/0").asInt());
	assertEquals("Foo", noteJson.at("/warnings/0/1").asText());
	assertEquals(614, noteJson.at("/warnings/1/0").asInt());
	assertEquals("Bar", noteJson.at("/warnings/1/1").asText());
	assertEquals("Spiel", noteJson.at("/warnings/1/2").asText());
	assertEquals(412, noteJson.at("/errors/0/0").asInt());
	assertEquals("Test", noteJson.at("/errors/0/1").asText());
	assertEquals(567, noteJson.at("/messages/0/0").asInt());
	assertEquals("Probe", noteJson.at("/messages/0/1").asText());
	assertEquals("huhu", noteJson.at("/messages/0/2").asText());
	assertEquals("hihi", noteJson.at("/messages/0/3").asText());

	// Todo: Check how to check for missing node

	Messages msgs = notes.getWarnings();
	assertEquals("[[613,\"Foo\"],[614,\"Bar\",\"Spiel\"]]",
		     msgs.toJSON());
    };


    @Test
    public void testNotificationCopy () throws IOException {

	Notifications notes1 = new Notifications();
	notes1.addWarning(1, "Foo");
	notes1.addWarning(2, "Bar", "Test");
	notes1.addError(3, "Probe");

	Notifications notes2 = new Notifications();
	notes2.addMessage(4, "Krah");
	notes2.addWarning(5, "Wu", "Niegel");

	assertTrue(notes1.hasWarnings());
	assertFalse(notes1.hasMessages());
	assertTrue(notes1.hasErrors());

	assertTrue(notes2.hasWarnings());
	assertTrue(notes2.hasMessages());
	assertFalse(notes2.hasErrors());

	// Copy notations
	notes1.copyNotificationsFrom(notes2);
	assertTrue(notes1.hasWarnings());
	assertTrue(notes1.hasMessages());
	assertTrue(notes1.hasErrors());

	JsonNode noteJson = mapper.readTree(notes1.toJSON());
	assertEquals(1, noteJson.at("/warnings/0/0").asInt());
	assertEquals("Foo", noteJson.at("/warnings/0/1").asText());
	assertEquals(2, noteJson.at("/warnings/1/0").asInt());
	assertEquals("Bar", noteJson.at("/warnings/1/1").asText());
	assertEquals("Test", noteJson.at("/warnings/1/2").asText());
	assertEquals(5, noteJson.at("/warnings/2/0").asInt());
	assertEquals("Wu", noteJson.at("/warnings/2/1").asText());
	assertEquals("Niegel", noteJson.at("/warnings/2/2").asText());
	assertEquals(4, noteJson.at("/messages/0/0").asInt());
	assertEquals("Krah", noteJson.at("/messages/0/1").asText());
	assertEquals(3, noteJson.at("/errors/0/0").asInt());
	assertEquals("Probe", noteJson.at("/errors/0/1").asText());
    };

    @Test
    public void testNotificationJSONCopy () throws IOException {

	Notifications notes1 = new Notifications();
	notes1.addWarning(1, "Foo");
	notes1.addWarning(2, "Bar", "Test");
	notes1.addError(3, "Probe");

	assertTrue(notes1.hasWarnings());
	assertFalse(notes1.hasMessages());
	assertTrue(notes1.hasErrors());

	JsonNode noteJson = mapper.readTree(
	    "{\"warnings\":[[5,\"Wu\",\"Niegel\"]]," +
	    "\"messages\":[[4,\"Krah\"]]}"
        );
	notes1.copyNotificationsFrom(noteJson);

	assertTrue(notes1.hasWarnings());
	assertTrue(notes1.hasMessages());
	assertTrue(notes1.hasErrors());

	noteJson = mapper.readTree(notes1.toJSON());

	assertEquals(1, noteJson.at("/warnings/0/0").asInt());
	assertEquals("Foo", noteJson.at("/warnings/0/1").asText());
	assertEquals(2, noteJson.at("/warnings/1/0").asInt());
	assertEquals("Bar", noteJson.at("/warnings/1/1").asText());
	assertEquals("Test", noteJson.at("/warnings/1/2").asText());
	assertEquals(5, noteJson.at("/warnings/2/0").asInt());
	assertEquals("Wu", noteJson.at("/warnings/2/1").asText());
	assertEquals("Niegel", noteJson.at("/warnings/2/2").asText());

	assertEquals(4, noteJson.at("/messages/0/0").asInt());
	assertEquals("Krah", noteJson.at("/messages/0/1").asText());

	assertEquals(3, noteJson.at("/errors/0/0").asInt());
	assertEquals("Probe", noteJson.at("/errors/0/1").asText());

	noteJson = mapper.readTree(
	    "{\"warnings\":[[8, \"Tanja\", \"Gaby\"]],\"errors\":" +
	    "[[\"Klößchen\"],[9,\"Karl\"]]}"
        );
	notes1.copyNotificationsFrom(noteJson);

	assertTrue(notes1.hasWarnings());
	assertTrue(notes1.hasMessages());
	assertTrue(notes1.hasErrors());

	noteJson = mapper.readTree(notes1.toJSON());

	assertEquals(1, noteJson.at("/warnings/0/0").asInt());
	assertEquals("Foo", noteJson.at("/warnings/0/1").asText());
	assertEquals(2, noteJson.at("/warnings/1/0").asInt());
	assertEquals("Bar", noteJson.at("/warnings/1/1").asText());
	assertEquals("Test", noteJson.at("/warnings/1/2").asText());
	assertEquals(5, noteJson.at("/warnings/2/0").asInt());
	assertEquals("Wu", noteJson.at("/warnings/2/1").asText());
	assertEquals("Niegel", noteJson.at("/warnings/2/2").asText());
	assertEquals(8, noteJson.at("/warnings/3/0").asInt());
	assertEquals("Tanja", noteJson.at("/warnings/3/1").asText());
	assertEquals("Gaby", noteJson.at("/warnings/3/2").asText());

	assertEquals(4, noteJson.at("/messages/0/0").asInt());
	assertEquals("Krah", noteJson.at("/messages/0/1").asText());

	assertEquals(3, noteJson.at("/errors/0/0").asInt());
	assertEquals("Probe", noteJson.at("/errors/0/1").asText());
	assertEquals("Klößchen", noteJson.at("/errors/1/0").asText());
	assertEquals(9, noteJson.at("/errors/2/0").asInt());
	assertEquals("Karl", noteJson.at("/errors/2/1").asText());
    };
};
