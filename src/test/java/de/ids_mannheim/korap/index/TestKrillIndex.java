package de.ids_mannheim.korap.index;

import java.util.*;
import java.io.*;

import org.apache.lucene.util.Version;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Bits;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.KrillQuery;
import de.ids_mannheim.korap.query.QueryBuilder;
import de.ids_mannheim.korap.index.FieldDocument;
import de.ids_mannheim.korap.index.MultiTermTokenStream;
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.util.QueryException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

@RunWith(JUnit4.class)
public class TestKrillIndex {


    /*
     * Todo: Currently fields can only be set if they are
     * part of the general field set.
     * this will change soon!
     */

    @Test
    public void indexExample () throws IOException {
        KrillIndex ki = new KrillIndex();

        assertEquals(0, ki.numberOf("base", "documents"));
        assertEquals(0, ki.numberOf("base", "tokens"));
        assertEquals(0, ki.numberOf("base", "sentences"));
        assertEquals(0, ki.numberOf("base", "paragraphs"));

        FieldDocument fd = new FieldDocument();

        fd.addString("name", "Peter");
        fd.addInt("zahl1", 56);
        fd.addInt("zahl2", "58");
        fd.addInt("zahl3", "059");
        fd.addInt("UID", 1);
        fd.addText("teaser", "Das ist der Name der Rose");
        fd.addTV("base", "ich bau", "[(0-3)s:ich|l:ich|p:PPER|-:sentences$<i>2]"
                + "[(4-7)s:bau|l:bauen|p:VVFIN]");
        ki.addDoc(fd);

        fd = new FieldDocument();

        fd.addString("name", "Hans");
        fd.addInt("zahl1", 14);
        fd.addText("teaser", "Das Sein");
        fd.addInt("UID", 2);

        MultiTermTokenStream mtts = fd.newMultiTermTokenStream();
        mtts.addMultiTermToken("s:wir#0-3", "l:wir", "p:PPER");
        mtts.addMultiTermToken("s:sind#4-8", "l:sein", "p:VVFIN");
        mtts.addMeta("sentences", (int) 5);
        fd.addTV("base", "wir sind", mtts);

        ki.addDoc(fd);

        /* Save documents */
        ki.commit();

        assertEquals(2, ki.numberOf("base", "documents"));
        assertEquals(7, ki.numberOf("base", "sentences"));

        fd = new FieldDocument();

        fd.addString("name", "Frank");
        fd.addInt("zahl1", 59);
        fd.addInt("zahl2", 65);
        fd.addInt("UID", 3);
        fd.addText("teaser", "Noch ein Versuch");
        fd.addTV("base", "ich bau", "[(0-3)s:der|l:der|p:DET|-:sentences$<i>3]"
                + "[(4-8)s:baum|l:baum|p:NN]");
        ki.addDoc(fd);

        /* Save documents */
        ki.commit();

        assertEquals(3, ki.numberOf("base", "documents"));
        assertEquals(10, ki.numberOf("base", "sentences"));

        // KrillQuery kq = new KrillQuery("text");
        // ki.search();

        ki.getDoc("1");
    };


    @Test
    public void indexAlteration () throws IOException {
        KrillIndex ki = new KrillIndex();

        assertEquals(0, ki.numberOf("base", "documents"));

        FieldDocument fd = new FieldDocument();
        fd.addString("name", "Peter");
        ki.addDoc(fd);

        assertEquals(0, ki.numberOf("base", "documents"));

        fd = new FieldDocument();
        fd.addString("name", "Michael");
        ki.addDoc(fd);

        assertEquals(0, ki.numberOf("base", "documents"));

        ki.commit();

        assertEquals(2, ki.numberOf("base", "documents"));

        // hasDeletions, hasPendingMerges
    };


    /*
     * This test demonstrates the behaviour
     */
    @Test
    public void indexUnicode () throws IOException, QueryException {
        KrillIndex ki = new KrillIndex();

        FieldDocument fd = new FieldDocument();
        fd.addString("name", "Peter");

        // These values are canonically equivalent
        // But indexed as byte sequences
        fd.addTV("base",
                new String("ju" + "\u006E" + "\u0303" + "o") + " "
                        + new String("ju" + "\u00F1" + "o"),
                "[(0-5)s:ju" + "\u006E" + "\u0303" + "o|_0$<i>0<i>5|-:t$<i>2]"
                        + "[(6-10)s:ju" + "\u00F1" + "o|_1$<i>6<i>10]");
        ki.addDoc(fd);
        ki.commit();

        assertEquals(1, ki.numberOf("base", "documents"));

        QueryBuilder kq = new QueryBuilder("base");
        Result kr = ki.search(kq.seg("s:ju" + "\u00F1" + "o").toQuery());
        assertEquals(1, kr.getTotalResults());

        kr = ki.search(kq.seg("s:ju" + "\u006E" + "\u0303" + "o").toQuery());
        assertEquals(1, kr.getTotalResults());
    };


    @Test
    public void indexFieldInfo () throws IOException {
        KrillIndex ki = new KrillIndex();

        FieldDocument fd = new FieldDocument();
        fd.setTitle("Peter");
        fd.setUID(22);
        ki.addDoc(fd);

        fd = new FieldDocument();
        fd.setTitle("Akron");
        fd.setUID("05678");
        ki.addDoc(fd);

        ki.commit();

        assertEquals(2, ki.numberOf("base", "documents"));

        assertEquals("Peter", ki.getDoc("22").getTitle());
        assertEquals(22, ki.getDoc("22").getUID());

        assertEquals("Akron", ki.getDoc("5678").getTitle());
        assertEquals(5678, ki.getDoc("5678").getUID());

        assertEquals("Akron", ki.getDoc("05678").getTitle());
        assertEquals(5678, ki.getDoc("05678").getUID());
    };


    @Test
    public void indexRetrieveFieldInfo () throws IOException {
        KrillIndex ki = new KrillIndex();

        FieldDocument fd = new FieldDocument();

        fd.addString("name", "Peter");
        fd.addString("textSigle", "a/b/c");
        fd.addInt("zahl1", 56);
		fd.addStored("ref", "My reference");

		fd.addKeyword("keyword", "baum");
		fd.addKeyword("keyword", "wald");

		fd.addText("title", "Der Name der Rose");

        ki.addDoc(fd);

        /* Save documents */
        ki.commit();

        JsonNode res = ki.getFields("a/b/c").toJsonNode();

		// TODO: Check if the sorting is always identical!

		assertEquals("ref", res.at("/document/fields/0/key").asText());
		assertEquals("type:string", res.at("/document/fields/0/type").asText());
		assertEquals("koral:field", res.at("/document/fields/0/@type").asText());
		assertEquals(true, res.at("/document/fields/0/retrieveOnly").asBoolean());
		assertEquals("My reference", res.at("/document/fields/0/value").asText());

		assertEquals("title", res.at("/document/fields/1/key").asText());
		assertEquals("type:text", res.at("/document/fields/1/type").asText());
		assertEquals("koral:field", res.at("/document/fields/1/@type").asText());
		assertEquals("Der Name der Rose", res.at("/document/fields/1/value").asText());

		assertEquals("textSigle", res.at("/document/fields/2/key").asText());
		assertEquals("type:string", res.at("/document/fields/2/type").asText());
		assertEquals("koral:field", res.at("/document/fields/2/@type").asText());
		assertEquals("a/b/c", res.at("/document/fields/2/value").asText());

		assertEquals("keyword", res.at("/document/fields/3/key").asText());
		assertEquals("type:string", res.at("/document/fields/3/type").asText());
		assertEquals("koral:field", res.at("/document/fields/3/@type").asText());
		assertEquals("baum", res.at("/document/fields/3/value/0").asText());
		assertEquals("wald", res.at("/document/fields/3/value/1").asText());

		assertEquals("zahl1", res.at("/document/fields/4/key").asText());
		assertEquals("type:number", res.at("/document/fields/4/type").asText());
		assertEquals("koral:field", res.at("/document/fields/4/@type").asText());
		assertEquals(56, res.at("/document/fields/4/value").asInt());

		assertEquals("name", res.at("/document/fields/5/key").asText());
		assertEquals("type:string", res.at("/document/fields/5/type").asText());
		assertEquals("koral:field", res.at("/document/fields/5/@type").asText());
		assertEquals("Peter", res.at("/document/fields/5/value").asText());

	};
};
