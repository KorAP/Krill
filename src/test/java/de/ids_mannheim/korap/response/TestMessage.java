package de.ids_mannheim.korap.response;

import de.ids_mannheim.korap.response.Message;
import de.ids_mannheim.korap.response.Messages;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestMessage {

    @Test
    public void StringMessage () {
        Messages km = new Messages();
        assertEquals("[]", km.toJsonString());
    };


    @Test
    public void StringMessageSet () {
        Messages km = new Messages();
        km.add(612, "Foo");
        assertEquals("[[612,\"Foo\"]]", km.toJsonString());
        km.add(613, "Bar");
        assertEquals("[[612,\"Foo\"],[613,\"Bar\"]]", km.toJsonString());
    };


    @Test
    public void StringMessageParameters () {
        Messages km = new Messages();
        km.add(612, "Foo");
        assertEquals("[[612,\"Foo\"]]", km.toJsonString());
        km.add(613, "Bar", "Instanz");
        assertEquals("[[612,\"Foo\"],[613,\"Bar\",\"Instanz\"]]",
                km.toJsonString());
        km.add(614, "Test");
        assertEquals("[[612,\"Foo\"],[613,\"Bar\",\"Instanz\"],[614,\"Test\"]]",
                km.toJsonString());
    };


    @Test
    public void CheckIterability () {
        Messages km = new Messages();
        km.add(612, "Foo");
        km.add(613, "Bar", "Instanz");
        km.add(614, "Test");
        String test = "";
        for (Message msg : km)
            test += msg.getCode();

        assertEquals("612613614", test);
    };
};
