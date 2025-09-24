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
import de.ids_mannheim.korap.collection.CollectionBuilder;
import de.ids_mannheim.korap.KrillCollection;

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
        assertEquals(false, ki.isReaderOpen());

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

        assertEquals(false, ki.isReaderOpen());
        assertEquals(2, ki.numberOf("base", "documents"));
        assertEquals(7, ki.numberOf("base", "sentences"));
        assertEquals(true, ki.isReaderOpen());

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
        fd.addText("title", "Peter");
        fd.setUID(22);
        ki.addDoc(fd);

        fd = new FieldDocument();
        fd.addText("title", "Akron");
        fd.setUID("05678");
        ki.addDoc(fd);

        ki.commit();

        assertEquals(2, ki.numberOf("base", "documents"));

        assertEquals("Peter", ki.getDoc("22").getFieldValue("title"));
        assertEquals(22, ki.getDoc("22").getUID());

        assertEquals("Akron", ki.getDoc("5678").getFieldValue("title"));
        assertEquals(5678, ki.getDoc("5678").getUID());

        assertEquals("Akron", ki.getDoc("05678").getFieldValue("title"));
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
		fd.addAttachement("ref2", "data:text/plain;charset=UTF-8,My reference2");

		fd.addKeywords("keyword", "baum");
		fd.addKeywords("keyword", "wald");

        fd.addKeywords("keyword", "garten laube");

		fd.addText("title", "Der Name der Rose");

        ki.addDoc(fd);

        /* Save documents */
        ki.commit();

        JsonNode res = ki.getFields("a/b/c").toJsonNode();

		// TODO: Check if the sorting is always identical!

		Iterator fieldIter = res.at("/document/fields").elements();

		int checkC = 0;
		while (fieldIter.hasNext()) {
			JsonNode field = (JsonNode) fieldIter.next();

			String key = field.at("/key").asText();

			switch (key) {
			case "ref":
				assertEquals("type:store", field.at("/type").asText());
				assertEquals("koral:field", field.at("/@type").asText());
				assertEquals("My reference", field.at("/value").asText());
				checkC++;
				break;

			case "ref2":
				assertEquals("type:attachement", field.at("/type").asText());
				assertEquals("koral:field", field.at("/@type").asText());
				assertEquals("data:text/plain;charset=UTF-8,My reference2", field.at("/value").asText());
				checkC++;
				break;
                
			case "title":
				assertEquals("type:text", field.at("/type").asText());
				assertEquals("koral:field", field.at("/@type").asText());
				assertEquals("Der Name der Rose", field.at("/value").asText());
				checkC++;
				break;

			case "textSigle":
				assertEquals("type:string", field.at("/type").asText());
				assertEquals("koral:field", field.at("/@type").asText());
				assertEquals("a/b/c", field.at("/value").asText());
				checkC++;
				break;

			case "keyword":
				assertEquals("type:keywords", field.at("/type").asText());
				assertEquals("koral:field", field.at("/@type").asText());
				assertEquals("baum", field.at("/value/0").asText());
				assertEquals("wald", field.at("/value/1").asText());
				assertEquals("garten", field.at("/value/2").asText());
				assertEquals("laube", field.at("/value/3").asText());
				checkC++;
				break;

			case "zahl1":
				assertEquals("type:integer", field.at("/type").asText());
				assertEquals("koral:field", field.at("/@type").asText());
				assertEquals(56, field.at("/value").asInt());
				checkC++;
				break;

			case "name":
				assertEquals("type:string", field.at("/type").asText());
				assertEquals("koral:field", field.at("/@type").asText());
				assertEquals("Peter", field.at("/value").asText());
				checkC++;
				break;
			};
		};
		
		assertEquals(7, checkC);


		// Test with real document
        ki.addDoc(getClass().getResourceAsStream("/wiki/wdd17-982-72848.json.gz"),true);

        /* Save documents */
        ki.commit();

		res = ki.getFields("wdd17/982/72841").toJsonNode();
		assertEquals("Document not found", res.at("/errors/0/1").asText());

		res = ki.getFields("WDD17/982/72848").toJsonNode();

		fieldIter = res.at("/document/fields").elements();

		checkC = 0;
		while (fieldIter.hasNext()) {
			JsonNode field = (JsonNode) fieldIter.next();

			String key = field.at("/key").asText();

			switch (key) {
			case "pubDate":

				assertEquals("type:date", field.at("/type").asText());
				assertEquals("2017-07-01", field.at("/value").asText());
				break;

			case "textSigle":

				assertEquals("type:string", field.at("/type").asText());
				assertEquals("WDD17/982/72848", field.at("/value").asText());
				break;

			case "foundries":
				assertEquals("type:keywords", field.at("/type").asText());
				assertEquals("dereko", field.at("/value/0").asText());
				assertEquals("dereko/structure", field.at("/value/1").asText());
				assertEquals("dereko/structure/base-sentences-paragraphs-pagebreaks", field.at("/value/2").asText());
				break;
			};
		};
	};

	@Test
    public void indexRetrieveFieldInfoUpsert () throws IOException {
        KrillIndex ki = new KrillIndex();

        FieldDocument fd = new FieldDocument();
        fd.addString("name", "Peter");
        fd.addString("textSigle", "a/b/c");
        ki.upsertDoc(fd);

        /* Save documents */
        ki.commit();

        fd = new FieldDocument();
        fd.addString("name", "Frank");
        fd.addString("textSigle", "a/b/d");
        ki.upsertDoc(fd);

        /* Save documents */
        ki.commit();

        fd = new FieldDocument();
        fd.addString("name", "Franz");
        fd.addString("textSigle", "a/b/c");
        ki.upsertDoc(fd);

        /* Save documents */
        ki.commit();
        JsonNode res = ki.getFields("a/b/c").toJsonNode();
		Iterator fieldIter = res.at("/document/fields").elements();

		int checkC = 0;
        while (fieldIter.hasNext()) {
			JsonNode field = (JsonNode) fieldIter.next();

			String key = field.at("/key").asText();

			switch (key) {
            case "name":
				assertEquals("type:string", field.at("/type").asText());
				assertEquals("koral:field", field.at("/@type").asText());
				assertEquals("Franz", field.at("/value").asText());
				checkC++;
				break;
            }
        }

        assertEquals(1, checkC);
        
    }

    @Test
    public void indexFieldVector () throws IOException {
        KrillIndex ki = new KrillIndex();

        FieldDocument fd = new FieldDocument();
        fd.addString("textSigle", "aaaa");
        ki.addDoc(fd);

        fd = new FieldDocument();
        fd.addString("textSigle", "bbbb");
        fd.setUID("05678");
        ki.addDoc(fd);

        ki.commit();

        CollectionBuilder cb = new CollectionBuilder();
        KrillCollection kcn = new KrillCollection(ki);
        
        List fieldValues = ki.getFieldVector("textSigle", kcn);
        assertEquals(2, fieldValues.size());
        assertEquals("aaaa", fieldValues.get(0));
        assertEquals("bbbb", fieldValues.get(1));

        fieldValues = ki.getFieldVector("UID", kcn);
        assertEquals(1, fieldValues.size(), 1);
        assertEquals("5678", fieldValues.get(0));
        
        kcn.fromBuilder(cb.term("textSigle","bbbb"));
        fieldValues = ki.getFieldVector("textSigle", kcn);
        assertEquals(1, fieldValues.size());
        assertEquals("bbbb", fieldValues.get(0));

        
        fd = new FieldDocument();
        fd.addString("textSigle", "cccc");
        ki.addDoc(fd);

        ki.commit();
        
        kcn.fromBuilder(null);
        fieldValues = ki.getFieldVector("textSigle", kcn);
        assertEquals(3, fieldValues.size());
        assertEquals("aaaa", fieldValues.get(0));
        assertEquals("bbbb", fieldValues.get(1));
        assertEquals("cccc", fieldValues.get(2));

        kcn.fromBuilder(cb.orGroup().with(cb.term("textSigle","aaaa")).with(cb.term("textSigle","cccc")));
        fieldValues = ki.getFieldVector("textSigle", kcn);
        assertEquals(2, fieldValues.size());
        assertEquals("aaaa", fieldValues.get(0));
        assertEquals("cccc", fieldValues.get(1));
    };

    @Test
    public void indexFieldVectorReal () throws IOException {
        KrillIndex ki = new KrillIndex();

		// Test with real document
        ki.addDoc(getClass().getResourceAsStream("/wiki/wdd17-982-72848.json.gz"),true);

        ki.commit();

        CollectionBuilder cb = new CollectionBuilder();
        KrillCollection kcn = new KrillCollection(ki);
        
        List fieldValues = ki.getFieldVector("textSigle", kcn);
        assertEquals(1, fieldValues.size());
        assertEquals("WDD17/982/72848", fieldValues.get(0));

        fieldValues = ki.getFieldVector("author", kcn);
        assertEquals(1, fieldValues.size());
        assertEquals("ArchivBot, u.a.", fieldValues.get(0));

        fieldValues = ki.getFieldVector("base", kcn);
        assertEquals(0, fieldValues.size());

        fieldValues = ki.getFieldVector("tokens", kcn);
        assertEquals(0, fieldValues.size());
    };
};
