package de.ids_mannheim.korap.response;

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
	assertEquals("[]", km.toJSON());
    };

    @Test
    public void StringMessageSet () {
	Messages km = new Messages();
	km.add(612,"Foo");
	assertEquals("[[612,\"Foo\"]]", km.toJSON());
	km.add(613,"Bar");
	assertEquals("[[612,\"Foo\"],[613,\"Bar\"]]", km.toJSON());
    };

    @Test
    public void StringMessageParameters () {
	Messages km = new Messages();
	km.add(612,"Foo");
	assertEquals("[[612,\"Foo\"]]", km.toJSON());
	km.add(613,"Bar", "Instanz");
	assertEquals("[[612,\"Foo\"],[613,\"Bar\",\"Instanz\"]]", km.toJSON());
	km.add(614,"Test");
	assertEquals("[[612,\"Foo\"],[613,\"Bar\",\"Instanz\"],[614,\"Test\"]]", km.toJSON());
    };
};
