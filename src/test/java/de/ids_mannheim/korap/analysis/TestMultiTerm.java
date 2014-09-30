package de.ids_mannheim.korap.analysis;

import java.util.*;
import de.ids_mannheim.korap.analysis.MultiTerm;
import java.io.IOException;
import org.apache.lucene.util.BytesRef;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;


@RunWith(JUnit4.class)
public class TestMultiTerm {
    @Test
    public void multiTermSimple () {
	MultiTerm mt = new MultiTerm("test");
	assertEquals(mt.term, "test");
	assertNull(mt.payload);
	assertEquals(mt.start, 0);
	assertEquals(mt.end, 0);
    };

    @Test
    public void multiTermPayload () {
	MultiTerm mt = new MultiTerm("test$5");
	assertEquals("test", mt.term);
	assertEquals(new BytesRef("5"), mt.payload);
	assertEquals(mt.start, 0);
	assertEquals(mt.end, 0);
    };

    @Test
    public void multiTermOffset () {
	MultiTerm mt = new MultiTerm("versuch#2-34");
	assertEquals(mt.term, "versuch");
	assertNull(mt.payload);
	assertEquals(mt.start, 2);
	assertEquals(mt.end, 34);
    };

    @Test
    public void multiTermOffsetPayload () {
	MultiTerm mt = new MultiTerm("example#6-42$hihi");
	assertEquals(mt.term, "example");
	assertEquals(new BytesRef("hihi"), mt.payload);
	assertEquals(mt.start,6);
	assertEquals(mt.end, 42);
    };

    @Test
    public void multiTermString () {
	MultiTerm mt = new MultiTerm("example#6-42$hihi");
	assertEquals("example#6-42$hihi", mt.toString());
	mt.term = "spassmacher";
	assertEquals("spassmacher#6-42$hihi", mt.toString());
    };

    @Test
    public void multiTermStringPayloadType () {
	MultiTerm mt = new MultiTerm("example$<i>4000");
	assertEquals("example$<?>[0,0,f,a0]", mt.toString());

	mt = new MultiTerm("example$<l>757574643438");
	assertEquals("example$<?>[0,0,0,b0,62,f7,ae,ee]", mt.toString());
    };

    @Test
    public void multiTermStringPayloadType2 () {
	MultiTerm mt = new MultiTerm();
	mt.setTerm("beispiel");
	mt.setStart(40);
	assertEquals(mt.getStart(), mt.start);
	mt.setEnd(50);
	assertEquals(mt.getEnd(), mt.end);
	mt.setPayload((int) 4000);
	assertEquals("beispiel#40-50$<?>[0,0,f,a0]", mt.toString());
    };

    @Test
    public void multiTermStringPayloadType3 () {
	MultiTerm mt = new MultiTerm("example$<b>120");
	assertEquals("example$x", mt.toString());
    };

    @Test
    public void multiTermStringPayloadType4 () {
	MultiTerm mt = new MultiTerm("example$<i>420<b>120");
	assertEquals("example$<?>[0,0,1,a4,78]", mt.toString());
    };


    @Test
    public void multiTermStringPayloadType5 () {
	MultiTerm mt = new MultiTerm("example$<i>4000");
	assertEquals("example$<?>[0,0,f,a0]", mt.toString());

	mt = new MultiTerm("example$<i>4000<b>120");
	assertEquals("example$<?>[0,0,f,a0,78]", mt.toString());

	mt = new MultiTerm("example$<l>4000<b>120");
	assertEquals("example$<?>[0,0,0,0,0,0,f,a0,78]", mt.toString());
    };

    @Test
    public void multiTermStringFail () {
	MultiTerm mt = new MultiTerm("example#56-66");
	assertEquals(56, mt.getStart());
	assertEquals(66,mt.getEnd());

	mt = new MultiTerm("example#56-66$<i>a");
	assertEquals(56, mt.getStart());
	assertEquals(66, mt.getEnd());

	mt = new MultiTerm("example#56$<i>a");
	assertEquals(mt.getPayload(), null);
	assertEquals(mt.getStart(), 0);
	assertEquals(mt.getEnd(), 0);
    };
};
