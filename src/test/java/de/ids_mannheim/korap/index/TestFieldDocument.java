package de.ids_mannheim.korap.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.util.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URLDecoder;

import org.apache.lucene.search.spans.SpanQuery;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.fasterxml.jackson.databind.JsonNode;

import static de.ids_mannheim.korap.TestSimple.*;
import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.KrillCollection;
import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.KrillMeta;
import de.ids_mannheim.korap.KrillQuery;
import de.ids_mannheim.korap.collection.CollectionBuilder;
import de.ids_mannheim.korap.query.QueryBuilder;
import de.ids_mannheim.korap.query.wrap.SpanQueryWrapper;
import de.ids_mannheim.korap.response.Match;
import de.ids_mannheim.korap.response.MetaFields;
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.util.QueryException;

import org.apache.lucene.document.Document;


@RunWith(JUnit4.class)
public class TestFieldDocument {

    @Test
    public void indexExample1 () throws IOException {
        FieldDocument fd = new FieldDocument();

        fd.addString("corpusID", "WPD");
        fd.addString("ID", "WPD-AAA-00001");
        fd.addText("textClass", "music entertainment");
        fd.addText("author", "Peter Frankenfeld");
        fd.addDate("pubDate", 20130617);
        fd.addInt("justanumber", 12345678);
        fd.addText("title", "Wikipedia");
        fd.addText("subTitle", "Die freie Enzyklopädie");
        fd.addStored("layerInfo", "opennlp/p=pos");
        fd.addString("pubPlace", "Bochum");
        fd.addDate("lastModified", 20130717);
        fd.addTV("tokens", "abc", "[(0-1)s:a|i:a|_0$<i>0<i>1|-:t$<i>10]"
                + "[(1-2)s:b|i:b|_1$<i>1<i>2]" + "[(2-3)s:c|i:c|_2$<i>2<i>3]");
        fd.addAttachement("Wikilink", "data:application/x.korap-link,https://de.wikipedia.org/wiki/Beispiel");

        Document doc = fd.compile();
        
        assertEquals("title", doc.getField("title").name());
        assertEquals("Wikipedia", doc.getField("title").stringValue());
       
        assertEquals("corpusID", doc.getField("corpusID").name());
        assertEquals("WPD", doc.getField("corpusID").stringValue());

        assertEquals("ID", doc.getField("ID").name());
        assertEquals("WPD-AAA-00001", doc.getField("ID").stringValue());

        assertEquals("subTitle", doc.getField("subTitle").name());
        assertEquals(
            "Die freie Enzyklopädie",
            doc.getField("subTitle").stringValue());

        assertEquals("pubPlace", doc.getField("pubPlace").name());
        assertEquals("Bochum", doc.getField("pubPlace").stringValue());

        assertEquals("lastModified", doc.getField("lastModified").name());
        assertEquals("20130717", doc.getField("lastModified").stringValue());

        assertEquals("tokens", doc.getField("tokens").name());
        assertEquals("abc", doc.getField("tokens").stringValue());

        assertEquals("author", doc.getField("author").name());
        assertEquals(
            "Peter Frankenfeld",
            doc.getField("author").stringValue());

        assertEquals("layerInfo", doc.getField("layerInfo").name());
        assertEquals(
            "opennlp/p=pos",
            doc.getField("layerInfo").stringValue());

        assertEquals("textClass", doc.getField("textClass").name());
        assertEquals(
            "music entertainment",
            doc.getField("textClass").stringValue());
        assertEquals("Wikilink", doc.getField("Wikilink").name());
        assertEquals(
            "data:application/x.korap-link,https://de.wikipedia.org/wiki/Beispiel",
            doc.getField("Wikilink").stringValue());

        assertEquals(12345678, doc.getField("justanumber").numericValue().intValue());

    };


    @Test
    public void indexExample2 () throws Exception {

        String json = new String("{" + "  \"fields\" : [" + "    { "
                + "      \"primaryData\" : \"abc\"" + "    }," + "    {"
                + "      \"name\" : \"tokens\"," + "      \"data\" : ["
                + "         [ \"s:a\", \"i:a\", \"_0$<i>0<i>1\", \"-:t$<i>3\"],"
                + "         [ \"s:b\", \"i:b\", \"_1$<i>1<i>2\" ],"
                + "         [ \"s:c\", \"i:c\", \"_2$<i>2<i>3\" ]" + "      ]"
                + "    }" + "  ]," + "  \"corpusID\"  : \"WPD\","
                + "  \"ID\"        : \"WPD-AAA-00001\","
                + "  \"textClass\" : \"music entertainment\","
                + "  \"author\"    : \"Peter Frankenfeld\","
                + "  \"pubDate\"   : 20130617,"
                + "  \"title\"     : \"Wikipedia\","
                + "  \"subTitle\"  : \"Die freie Enzyklopädie\","
                + "  \"pubPlace\"  : \"Bochum\"" + "}");

        KrillIndex ki = new KrillIndex();
        FieldDocument fd = ki.addDoc(json);

        ki.commit();

        assertEquals("abc", fd.getPrimaryData());
        assertEquals("WPD", fd.getCorpusID());
        assertEquals("WPD-AAA-00001", fd.getID());
        assertEquals("music entertainment", fd.getFieldValue("textClass"));
        assertEquals("Peter Frankenfeld", fd.getFieldValue("author"));
        assertEquals("Wikipedia", fd.getFieldValue("title"));
        assertEquals("Die freie Enzyklopädie", fd.getFieldValue("subTitle"));
        assertEquals("Bochum", fd.getFieldValue("pubPlace"));
        assertEquals("2013-06-17", fd.getFieldValueAsDate("pubDate").toDisplay());

        QueryBuilder kq = new QueryBuilder("tokens");
        Result kr = ki
                .search((SpanQuery) kq.seq(kq.nr(3, kq.seg("s:b"))).toQuery());

        Match km = kr.getMatch(0);

        assertEquals("abc", km.getPrimaryData());
        assertEquals("WPD", km.getCorpusID());
        assertEquals("WPD-AAA-00001", km.getDocID());
        assertEquals("music entertainment", km.getFieldValue("textClass"));
        assertEquals("Peter Frankenfeld", km.getFieldValue("author"));
        assertEquals("Wikipedia", km.getFieldValue("title"));
        assertEquals("Die freie Enzyklopädie", km.getFieldValue("subTitle"));
        assertEquals("Bochum", km.getFieldValue("pubPlace"));
        assertEquals("2013-06-17", km.getFieldValueAsDate("pubDate").toDisplay());

        assertEquals("a[[{3:b}]]c", km.getSnippetBrackets());
    };


    @Test
    public void indexExample3 () throws IOException {

        // Construct index
        KrillIndex ki = new KrillIndex();

        // Indexing test files
        for (String i : new String[] { "00001", "00002", "00003", "00004",
                "00005", "00006", "02439" }) {
            FieldDocument fd = ki.addDoc(
                    getClass().getResourceAsStream("/wiki/" + i + ".json.gz"),
                    true);
        };
        ki.commit();

        QueryBuilder kq = new QueryBuilder("tokens");

        Krill ks;
        Result kr;

        // Start creating query
        // within(<s>, {1: {2: [mate/p=ADJA & mate/m=number:sg]}[opennlp/p=NN & tt/p=NN]})

        ks = new Krill(kq.contains(kq.tag("base/s:s"), kq.nr(1,
                kq.seq(kq.seg("mate/p:ADJA")).append(kq.seg("opennlp/p:NN")))));

        KrillMeta meta = ks.getMeta();
        meta.setCount(1);
        meta.setCutOff(true);

        meta.getContext().left.setCharacter(true).setLength(6);
        meta.getContext().right.setToken(true).setLength(6);

        assertEquals(
                "... okal. [[Der Buchstabe A hat in {1:deutschen Texten} eine durchschnittliche Häufigkeit von 6,51 %.]] Er ist damit der sechsthäufigste Buchstabe ...",
                ks.apply(ki).getMatch(0).getSnippetBrackets());


        // Do not retrieve snippets
        meta.setSnippets(false);

        Match km = ks.apply(ki).getMatch(0);
        
        assertEquals("Ruru,Jens.Ol,Aglarech", km.toJsonNode().get("author").asText());
        assertTrue(!km.toJsonNode().has("snippet"));
        assertEquals("", km.getPrimaryData());
        assertFalse(km.toJsonNode().has("startMore"));
        assertFalse(km.toJsonNode().has("endMore"));
        assertFalse(km.toJsonNode().has("endCutted"));
        assertFalse(km.toJsonNode().has("snippet"));
    };


    @Test
    public void queryJSONBsp18 () throws Exception {

        // Construct index
        KrillIndex ki = new KrillIndex();

        // Indexing test files
        for (String i : new String[] { "00001", "00002", "00003", "00004",
                "00005", "00006", "02439" }) {
            FieldDocument fd = ki.addDoc(
                    getClass().getResourceAsStream("/wiki/" + i + ".json.gz"),
                    true);

        };
        ki.commit();

        String jsonPath = URLDecoder.decode(
                getClass().getResource("/queries/bsp18.jsonld").getFile(),
                "UTF-8");

        // {1:der} \w0:5 nicht
        SpanQueryWrapper sqwi = getJsonQuery(jsonPath);

        Result kr = ki.search(sqwi.toQuery(), 0, (short) 5, true, (short) 2,
                false, (short) 5);

        assertEquals(1, kr.getTotalResults());
        assertEquals(
                "... bezeichnen, sofern [[{1:der} schwedische Buchstabe „Å“ nicht]] verfügbar ist im SI-Einheitensystem ist ...",
                kr.getMatch(0).getSnippetBrackets());
    };


    @Test
    public void indexNoValidDate () throws Exception {

        String json = new String("{" + "  \"fields\" : [" + "    { "
                + "      \"primaryData\" : \"abc\"" + "    }," + "    {"
                + "      \"name\" : \"tokens\"," + "      \"data\" : ["
                + "         [ \"s:a\", \"i:a\", \"_0$<i>0<i>1\", \"-:t$<i>3\"],"
                + "         [ \"s:b\", \"i:b\", \"_1$<i>1<i>2\" ],"
                + "         [ \"s:c\", \"i:c\", \"_2$<i>2<i>3\" ]" + "      ]"
                + "    }" + "  ]," + "  \"corpusID\"  : \"WPD\","
                + "  \"ID\"        : \"WPD-AAA-00001\","
                + "  \"textClass\" : \"music entertainment\","
                + "  \"author\"    : \"Peter Frankenfeld\","
                + "  \"pubDate\"   : \"00000000\","
                + "  \"title\"     : \"Wikipedia\","
                + "  \"subTitle\"  : \"Die freie Enzyklopädie\","
                + "  \"pubPlace\"  : \"Bochum\"" + "}");

        KrillIndex ki = new KrillIndex();
        FieldDocument fd = ki.addDoc(json);

        ki.commit();

        assertEquals("abc", fd.getPrimaryData());
        assertEquals("WPD", fd.getCorpusID());
        assertEquals("WPD-AAA-00001", fd.getID());
        assertEquals("music entertainment", fd.getFieldValue("textClass"));
        assertEquals("Peter Frankenfeld", fd.getFieldValue("author"));
        assertEquals("Wikipedia", fd.getFieldValue("title"));
        assertEquals("Die freie Enzyklopädie", fd.getFieldValue("subTitle"));
        assertEquals("Bochum", fd.getFieldValue("pubPlace"));
        assertEquals("", fd.getFieldValueAsDate("pubDate").toDisplay());
	};

    @Test
    public void indexNewMetaData () throws Exception {

        String json = new String(
            "{"
            + "  \"data\" : {"
            + "    \"text\" : \"abc\","
            + "    \"name\" : \"tokens\","
            + "    \"stream\" : ["
            + "       [ \"s:a\", \"i:a\", \"_0$<i>0<i>1\", \"-:t$<i>3\"],"
            + "       [ \"s:b\", \"i:b\", \"_1$<i>1<i>2\" ],"
            + "       [ \"s:c\", \"i:c\", \"_2$<i>2<i>3\" ]"
            + "    ]"
            + "  },"
            + "  \"fields\" : ["
            + "    {"
            + "      \"@type\" : \"koral:field\","
            + "      \"type\" : \"type:string\","
            + "      \"key\" : \"corpusID\","
            + "      \"value\" : \"WPD\""
            + "    },"
            + "    {"
            + "      \"@type\" : \"koral:field\","
            + "      \"type\" : \"type:string\","
            + "      \"key\" : \"textSigle\","
            + "      \"value\" : \"x/y/z\""
            + "    },"
            + "    {"
            + "      \"@type\" : \"koral:field\","
            + "      \"type\" : \"type:string\","
            + "      \"key\" : \"ID\","
            + "      \"value\" : \"WPD-AAA-00001\""
            + "    },"
            + "    {"
            + "      \"@type\" : \"koral:field\","
            + "      \"type\" : \"type:string\","
            + "      \"key\" : \"textClass\","
            + "      \"value\" : [\"music\",\"entertainment\"]"
            + "    },"
            + "    {"
            + "      \"@type\" : \"koral:field\","
            + "      \"type\" : \"type:text\","
            + "      \"key\" : \"author\","
            + "      \"value\" : \"Peter Frankenfeld\""
            + "    },"
            + "    {"
            + "      \"@type\" : \"koral:field\","
            + "      \"type\" : \"type:date\","
            + "      \"key\" : \"pubDate\","
            + "      \"value\" : \"2015-05-01\""
            + "    },"
            + "    {"
            + "      \"@type\" : \"koral:field\","
            + "      \"type\" : \"type:text\","
            + "      \"key\" : \"title\","
            + "      \"value\" : \"Wikipedia\""
            + "    },"
            + "    {"
            + "      \"@type\" : \"koral:field\","
            + "      \"type\" : \"type:text\","
            + "      \"key\" : \"subTitle\","
            + "      \"value\" : \"Die freie Enzyklopädie\""
            + "    },"
            + "    {"
            + "      \"@type\" : \"koral:field\","
            + "      \"type\" : \"type:string\","
            + "      \"key\" : \"pubPlace\","
            + "      \"value\" : \"Bochum\""
            + "    },"
            + "    {"
            + "      \"@type\" : \"koral:field\","
            + "      \"type\" : \"type:attachement\","
            + "      \"key\" : \"link\","
            + "      \"value\" : \"data:application/x.korap-link,https://de.wikipedia.org/wiki/Beispiel\""
            + "    }"
            + "  ]"
            + "}");

        KrillIndex ki = new KrillIndex();
        FieldDocument fd = ki.addDoc(json);

        ki.commit();

        assertEquals("abc", fd.getPrimaryData());
        // assertEquals("WPD", fd.doc.getField("corpusID").stringValue());
        assertEquals("x/y/z", fd.doc.getField("textSigle").stringValue());
        assertEquals("WPD-AAA-00001", fd.doc.getField("ID").stringValue());
        assertEquals("music entertainment", fd.doc.getField("textClass").stringValue());
        assertEquals("Peter Frankenfeld", fd.doc.getField("author").stringValue());
        assertEquals("Wikipedia", fd.doc.getField("title").stringValue());
        assertEquals("Die freie Enzyklopädie", fd.doc.getField("subTitle").stringValue());
        assertEquals("Bochum", fd.doc.getField("pubPlace").stringValue());
        assertEquals("20150501", fd.doc.getField("pubDate").stringValue());
        assertEquals(
            "data:application/x.korap-link,https://de.wikipedia.org/wiki/Beispiel",
            fd.doc.getField("link").stringValue());

        JsonNode res = ki.getFields("x/y/z").toJsonNode();

        Iterator fieldIter = res.at("/document/fields").elements();

        		int checkC = 0;
		while (fieldIter.hasNext()) {
			JsonNode field = (JsonNode) fieldIter.next();

			String key = field.at("/key").asText();

			switch (key) {
			case "corpusID":
				assertEquals("type:string", field.at("/type").asText());
				assertEquals("koral:field", field.at("/@type").asText());
				assertEquals("WPD", field.at("/value").asText());
				checkC++;
				break;

			case "textSigle":
				assertEquals("type:string", field.at("/type").asText());
				assertEquals("koral:field", field.at("/@type").asText());
				assertEquals("x/y/z", field.at("/value").asText());
				checkC++;
				break;

			case "ID":
				assertEquals("type:string", field.at("/type").asText());
				assertEquals("koral:field", field.at("/@type").asText());
				assertEquals("WPD-AAA-00001", field.at("/value").asText());
				checkC++;
				break;

			case "textClass":
				assertEquals("type:keywords", field.at("/type").asText());
				assertEquals("koral:field", field.at("/@type").asText());
				assertEquals("music", field.at("/value/0").asText());
				assertEquals("entertainment", field.at("/value/1").asText());
				checkC++;
				break;

            case "author":
				assertEquals("type:text", field.at("/type").asText());
				assertEquals("koral:field", field.at("/@type").asText());
				assertEquals("Peter Frankenfeld", field.at("/value").asText());
				checkC++;
				break;

            case "title":
				assertEquals("type:text", field.at("/type").asText());
				assertEquals("koral:field", field.at("/@type").asText());
				assertEquals("Wikipedia", field.at("/value").asText());
				checkC++;
				break;

            case "subTitle":
				assertEquals("type:text", field.at("/type").asText());
				assertEquals("koral:field", field.at("/@type").asText());
				assertEquals("Die freie Enzyklopädie", field.at("/value").asText());
				checkC++;
				break;

            case "pubPlace":
				assertEquals("type:string", field.at("/type").asText());
				assertEquals("koral:field", field.at("/@type").asText());
				assertEquals("Bochum", field.at("/value").asText());
				checkC++;
				break;

            case "pubDate":
				assertEquals("type:date", field.at("/type").asText());
				assertEquals("koral:field", field.at("/@type").asText());
				assertEquals("2015-05-01", field.at("/value").asText());
				checkC++;
				break;

            case "link":
				assertEquals("type:attachement", field.at("/type").asText());
				assertEquals("koral:field", field.at("/@type").asText());
				assertEquals("data:application/x.korap-link,https://de.wikipedia.org/wiki/Beispiel", field.at("/value").asText());
				checkC++;
				break;

            default:
                fail("Unknown field: " + key);
            };
        };
    };

    
    @Test
    public void indexArbitraryMetaData () throws Exception {
        String json = createDocString1();

        KrillIndex ki = new KrillIndex();
        FieldDocument fd = ki.addDoc(json);

        ki.commit();

        assertEquals("abc", fd.getPrimaryData());
        assertEquals("40.0", fd.doc.getField("alter").stringValue());
        assertEquals("Frank", fd.doc.getField("name").stringValue());
        assertEquals("musik unterhaltung", fd.doc.getField("schluesselwoerter").stringValue());
        assertEquals("nachrichten feuilleton sport raetsel", fd.doc.getField("tags").stringValue());
        assertEquals("Der alte Baum", fd.doc.getField("titel").stringValue());
        assertEquals("data:application/x.korap-link,http://spiegel.de/", fd.doc.getField("anhang").stringValue());
        assertEquals("So war das", fd.doc.getField("referenz").stringValue());
        assertEquals("20180403", fd.doc.getField("datum").stringValue());

        JsonNode res = ki.getFields("aa/bb/cc").toJsonNode();

        Iterator fieldIter = res.at("/document/fields").elements();

        int checkC = 0;
		while (fieldIter.hasNext()) {
			JsonNode field = (JsonNode) fieldIter.next();

			String key = field.at("/key").asText();

			switch (key) {
			case "textSigle":
				assertEquals("type:string", field.at("/type").asText());
				assertEquals("koral:field", field.at("/@type").asText());
				assertEquals("aa/bb/cc", field.at("/value").asText());
				checkC++;
				break;

			case "alter":
				assertEquals("type:integer", field.at("/type").asText());
				assertEquals("koral:field", field.at("/@type").asText());
				assertEquals(40, field.at("/value").asInt());
				checkC++;
				break;

			case "name":
				assertEquals("type:string", field.at("/type").asText());
				assertEquals("koral:field", field.at("/@type").asText());
				assertEquals("Frank", field.at("/value").asText());
				checkC++;
				break;

			case "schluesselwoerter":
				assertEquals("type:keywords", field.at("/type").asText());
				assertEquals("koral:field", field.at("/@type").asText());
				assertEquals("musik", field.at("/value/0").asText());
				assertEquals("unterhaltung", field.at("/value/1").asText());
				checkC++;
				break;

            case "tags":
				assertEquals("type:keywords", field.at("/type").asText());
				assertEquals("koral:field", field.at("/@type").asText());
				assertEquals("nachrichten", field.at("/value/0").asText());
				assertEquals("feuilleton", field.at("/value/1").asText());
				assertEquals("sport", field.at("/value/2").asText());
				assertEquals("raetsel", field.at("/value/3").asText());
				checkC++;
				break;

            case "titel":
				assertEquals("type:text", field.at("/type").asText());
				assertEquals("koral:field", field.at("/@type").asText());
				assertEquals("Der alte Baum", field.at("/value").asText());
				checkC++;
				break;

            case "anhang":
				assertEquals("type:attachement", field.at("/type").asText());
				assertEquals("koral:field", field.at("/@type").asText());
				assertEquals("data:application/x.korap-link,http://spiegel.de/", field.at("/value").asText());
				checkC++;
				break;

            case "referenz":
				assertEquals("type:store", field.at("/type").asText());
				assertEquals("koral:field", field.at("/@type").asText());
				assertEquals("So war das", field.at("/value").asText());
				checkC++;
				break;

            case "datum":
				assertEquals("type:date", field.at("/type").asText());
				assertEquals("koral:field", field.at("/@type").asText());
				assertEquals("2018-04-03", field.at("/value").asText());
				checkC++;
				break;

            default:
                fail("Unknown field: " + key);
            };
        };
    };

    @Test
    public void indexArbitraryMetaDataPartial () throws Exception {
        String json = createDocString1();

        KrillIndex ki = new KrillIndex();
        FieldDocument fd = ki.addDoc(json);

        ki.commit();

        ArrayList hs = new ArrayList<String>();
        hs.add("datum");
        hs.add("titel");
        JsonNode res = ki.getFields("aa/bb/cc", hs).toJsonNode();
        assertEquals("type:date", res.at("/document/fields/0/type").asText());
        assertEquals("datum", res.at("/document/fields/0/key").asText());
        assertEquals("2018-04-03", res.at("/document/fields/0/value").asText());
        assertEquals("type:text", res.at("/document/fields/1/type").asText());
        assertEquals("titel", res.at("/document/fields/1/key").asText());
        assertEquals("Der alte Baum", res.at("/document/fields/1/value").asText());
        assertTrue(res.at("/document/fields/2").isMissingNode());
    };

    @Test
    public void indexArbitraryMetaDataSorted () throws Exception {
        String json = createDocString1();

        KrillIndex ki = new KrillIndex();
        FieldDocument fd = ki.addDoc(json);

        ki.commit();

        ArrayList hs = new ArrayList<String>();
        hs.add("titel");
        hs.add("datum");
        JsonNode res = ki.getFields("aa/bb/cc", hs).toJsonNode();
        assertEquals("type:text", res.at("/document/fields/0/type").asText());
        assertEquals("titel", res.at("/document/fields/0/key").asText());
        assertEquals("Der alte Baum", res.at("/document/fields/0/value").asText());
        assertEquals("type:date", res.at("/document/fields/1/type").asText());
        assertEquals("datum", res.at("/document/fields/1/key").asText());
        assertEquals("2018-04-03", res.at("/document/fields/1/value").asText());
        assertTrue(res.at("/document/fields/2").isMissingNode());
    };
    
    @Test
    public void indexArbitraryMetaDataEmpty () throws Exception {
        String json = createDocString1();

        KrillIndex ki = new KrillIndex();
        FieldDocument fd = ki.addDoc(json);

        ki.commit();

        ArrayList hs = new ArrayList<String>();
        hs.add("titel");
        hs.add("frage");
        hs.add("datum");
        JsonNode res = ki.getFields("aa/bb/cc", hs).toJsonNode();
        assertEquals("type:text", res.at("/document/fields/0/type").asText());
        assertEquals("titel", res.at("/document/fields/0/key").asText());
        assertEquals("Der alte Baum", res.at("/document/fields/0/value").asText());
        assertEquals("frage", res.at("/document/fields/1/key").asText());
        assertTrue(res.at("/document/fields/1/type").isMissingNode());
        assertEquals("type:date", res.at("/document/fields/2/type").asText());
        assertEquals("datum", res.at("/document/fields/2/key").asText());
        assertEquals("2018-04-03", res.at("/document/fields/2/value").asText());
        assertTrue(res.at("/document/fields/3").isMissingNode());
    };


    /**
     * A document with only metadata (no token stream / no "data" section)
     * should be rejected by addDoc with an error.
     */
    @Test
    public void testAddDocRejectsMetadataOnlyDocument () throws Exception {
        KrillIndex ki = new KrillIndex();

        FieldDocument fd = new FieldDocument();
        fd.addString("textSigle", "TEST/META/001");
        fd.addString("author", "Test Author");
        fd.addDate("pubDate", 20200101);

        FieldDocument result = ki.addDoc(fd);
        ki.commit();

        assertEquals(0, ki.numberOf("documents"));
        assertTrue(
            "addDoc should return the doc even when rejected",
            result != null
        );
    }

    /**
     * A document with only metadata should also be rejected via upsertDoc.
     */
    @Test
    public void testUpsertDocRejectsMetadataOnlyDocument () throws Exception {
        KrillIndex ki = new KrillIndex();

        FieldDocument fd = new FieldDocument();
        fd.addString("textSigle", "TEST/META/002");
        fd.addString("author", "Test Author");

        FieldDocument result = ki.upsertDoc(fd);
        ki.commit();

        assertEquals(0, ki.numberOf("documents"));
    }

    /**
     * A JSON document with fields but no data section should be rejected.
     */
    @Test
    public void testAddDocRejectsJsonWithoutTokenStream () throws Exception {
        String json = "{"
            + "  \"fields\" : ["
            + "    {"
            + "      \"@type\" : \"koral:field\","
            + "      \"type\" : \"type:string\","
            + "      \"key\" : \"textSigle\","
            + "      \"value\" : \"TEST/NODATA/001\""
            + "    },"
            + "    {"
            + "      \"@type\" : \"koral:field\","
            + "      \"type\" : \"type:text\","
            + "      \"key\" : \"author\","
            + "      \"value\" : \"Nobody\""
            + "    },"
            + "    {"
            + "      \"@type\" : \"koral:field\","
            + "      \"type\" : \"type:date\","
            + "      \"key\" : \"pubDate\","
            + "      \"value\" : \"2020-01-01\""
            + "    }"
            + "  ]"
            + "}";

        KrillIndex ki = new KrillIndex();
        FieldDocument fd = ki.addDoc(json);
        ki.commit();

        assertEquals(
            "Metadata-only document should not be indexed",
            0, ki.numberOf("documents")
        );
    }

    /**
     * A document with a token stream on an unknown field name
     * (neither "tokens" nor "base") should be rejected.
     */
    @Test
    public void testAddDocRejectsUnknownTokenFieldName () throws Exception {
        KrillIndex ki = new KrillIndex();

        FieldDocument fd = new FieldDocument();
        fd.addString("textSigle", "TEST/WRONG/001");
        fd.addTV("other", "hello world",
            "[(0-5)s:hello|i:hello|_0$<i>0<i>5|-:tokens$<i>2]"
            + "[(6-11)s:world|i:world|_1$<i>6<i>11]");
        ki.addDoc(fd);
        ki.commit();

        assertEquals(
            "Document with token stream on unknown field should be rejected",
            0, ki.numberOf("documents")
        );
    }

    /**
     * A document with a token stream on the legacy "base" field
     * should be accepted.
     */
    @Test
    public void testAddDocAcceptsLegacyBaseField () throws Exception {
        KrillIndex ki = new KrillIndex();

        FieldDocument fd = new FieldDocument();
        fd.addString("textSigle", "TEST/BASE/001");
        fd.addTV("base", "hello world",
            "[(0-5)s:hello|i:hello|_0$<i>0<i>5|-:t$<i>2]"
            + "[(6-11)s:world|i:world|_1$<i>6<i>11]");
        ki.addDoc(fd);
        ki.commit();

        assertEquals(
            "Document with legacy 'base' field should be accepted",
            1, ki.numberOf("documents")
        );
    }

    /**
     * When metadata-only documents are mixed with proper documents,
     * only proper documents should be indexed and contribute to stats.
     */
    @Test
    public void testStatsWithMixedValidAndMetadataOnlyDocuments () throws Exception {
        KrillIndex ki = new KrillIndex();

        // Valid document with token stream
        FieldDocument fd1 = new FieldDocument();
        fd1.addString("textSigle", "TEST/OK/001");
        fd1.addString("author", "Good Author");
        fd1.addTV("tokens", "hello world",
            "[(0-5)s:hello|i:hello|_0$<i>0<i>5|-:tokens$<i>2]"
            + "[(6-11)s:world|i:world|_1$<i>6<i>11]");
        ki.addDoc(fd1);

        // Metadata-only document (should be rejected)
        FieldDocument fd2 = new FieldDocument();
        fd2.addString("textSigle", "TEST/BAD/001");
        fd2.addString("author", "Bad Author");
        ki.addDoc(fd2);

        // Another valid document
        FieldDocument fd3 = new FieldDocument();
        fd3.addString("textSigle", "TEST/OK/002");
        fd3.addString("author", "Another Author");
        fd3.addTV("tokens", "foo bar baz",
            "[(0-3)s:foo|i:foo|_0$<i>0<i>3|-:tokens$<i>3]"
            + "[(4-7)s:bar|i:bar|_1$<i>4<i>7]"
            + "[(8-11)s:baz|i:baz|_2$<i>8<i>11]");
        ki.addDoc(fd3);

        ki.commit();

        assertEquals(
            "Only valid documents should be counted",
            2, ki.numberOf("documents")
        );
        assertEquals(5, ki.numberOf("tokens"));
    }

    /**
     * A document with an empty stream (data section present but stream
     * is empty) has a token stream field but no tokens. This is allowed
     * through indexing since the document structure is valid (it has a
     * "data" section), but it contributes 0 to token statistics.
     */
    @Test
    public void testAddDocAcceptsEmptyTokenStream () throws Exception {
        String json = "{"
            + "  \"data\" : {"
            + "    \"text\" : \"\","
            + "    \"name\" : \"tokens\","
            + "    \"stream\" : []"
            + "  },"
            + "  \"fields\" : ["
            + "    {"
            + "      \"@type\" : \"koral:field\","
            + "      \"type\" : \"type:string\","
            + "      \"key\" : \"textSigle\","
            + "      \"value\" : \"TEST/EMPTY/001\""
            + "    }"
            + "  ]"
            + "}";

        KrillIndex ki = new KrillIndex();
        FieldDocument fd = ki.addDoc(json);
        ki.commit();

        assertEquals(
            "Document with empty stream is accepted (has token field)",
            1, ki.numberOf("documents")
        );
        assertEquals(
            "Empty stream contributes 0 tokens",
            0, ki.numberOf("tokens")
        );
    }

    /**
     * Upserting a valid document with a metadata-only replacement should
     * NOT remove the original and should reject the new one.
     */
    @Test
    public void testUpsertDoesNotReplaceValidDocWithMetadataOnly () throws Exception {
        KrillIndex ki = new KrillIndex();

        // First: add a valid document
        FieldDocument fd1 = new FieldDocument();
        fd1.addString("textSigle", "TEST/UPSERT/001");
        fd1.addString("author", "Original");
        fd1.addTV("tokens", "good data",
            "[(0-4)s:good|i:good|_0$<i>0<i>4|-:tokens$<i>2]"
            + "[(5-9)s:data|i:data|_1$<i>5<i>9]");
        ki.addDoc(fd1);
        ki.commit();

        assertEquals(1, ki.numberOf("documents"));
        assertEquals(2, ki.numberOf("tokens"));

        // Now try to upsert with metadata-only (should be rejected)
        FieldDocument fd2 = new FieldDocument();
        fd2.addString("textSigle", "TEST/UPSERT/001");
        fd2.addString("author", "Replacement Without Tokens");
        ki.upsertDoc(fd2);
        ki.commit();

        // The original document should still be there since the upsert
        // was rejected before the delete could happen
        assertEquals(1, ki.numberOf("documents"));
        assertEquals(2, ki.numberOf("tokens"));

        MetaFields mfs = ki.getFields("TEST/UPSERT/001");
        assertEquals("Original", mfs.getFieldValue("author"));
    }

    @Test
    public void indexUpsert () throws Exception {
        KrillIndex ki = new KrillIndex();

        // Add new document
        FieldDocument fd = new FieldDocument();
        fd.addString("textSigle", "AAA/BBB/001");
        fd.addString("content", "Example1");
        fd.addTV("tokens", "Example1",
            "[(0-8)s:Example1|i:example1|_0$<i>0<i>8|-:tokens$<i>1]");
        ki.upsertDoc(fd);
        ki.commit();

        MetaFields mfs = ki.getFields("AAA/BBB/001");
        assertEquals(10, mfs.getFieldValue("indexCreationDate").length());
        assertTrue(mfs.getFieldValue("indexCreationDate").matches("\\d{4}-\\d{2}-\\d{2}"));
        assertEquals(
            mfs.getFieldValue("indexCreationDate"),
            mfs.getFieldValue("indexLastModified")
            );
        assertEquals("Example1", mfs.getFieldValue("content"));


        // Add new document
        fd = new FieldDocument();
        fd.addString("textSigle", "AAA/BBB/002");
        fd.addString("content", "Example2");
        fd.addTV("tokens", "Example2",
            "[(0-8)s:Example2|i:example2|_0$<i>0<i>8|-:tokens$<i>1]");

        ki.upsertDoc(fd);
        ki.commit();

        mfs = ki.getFields("AAA/BBB/002");
        assertEquals(10, mfs.getFieldValue("indexCreationDate").length());
       
        assertTrue(mfs.getFieldValue("indexCreationDate").matches("\\d{4}-\\d{2}-\\d{2}"));
        assertEquals("Example2", mfs.getFieldValue("content"));

        fd = new FieldDocument();
        fd.addString("textSigle", "AAA/BBB/001");
        fd.addString("content", "Example3");
        fd.addTV("tokens", "Example3",
            "[(0-8)s:Example3|i:example3|_0$<i>0<i>8|-:tokens$<i>1]");

        ki.upsertDoc(fd);
        ki.commit();

        mfs = ki.getFields("AAA/BBB/001");
        assertEquals(10, mfs.getFieldValue("indexCreationDate").length());
        assertTrue(mfs.getFieldValue("indexCreationDate").matches("\\d{4}-\\d{2}-\\d{2}"));
        assertEquals("Example3", mfs.getFieldValue("content"));

        assertEquals(2, ki.numberOf("documents"));

        // Test Inputstream method
        ki.upsertDoc(getClass().getResourceAsStream("/wiki/WPD17-H81-63495.json.gz"), true);
        ki.commit();
        assertEquals(3, ki.numberOf("documents"));

        ki.close();

        fd = new FieldDocument();
        fd.addString("textSigle", "AAA/DDD/005");
        fd.addString("content", "Example4");
        fd.addTV("tokens", "Example4",
            "[(0-8)s:Example4|i:example4|_0$<i>0<i>8|-:tokens$<i>1]");
        
        ki.upsertDoc(fd);
        ki.commit();

        assertEquals(4, ki.numberOf("documents"));

    };


    private static FieldDocument createSimpleDoc (String textSigle,
            String author, int tokenCount) {
        FieldDocument fd = new FieldDocument();
        fd.addString("textSigle", textSigle);
        fd.addString("author", author);

        StringBuilder primaryData = new StringBuilder();
        StringBuilder tvBuilder = new StringBuilder();
        for (int i = 0; i < tokenCount; i++) {
            char c = (char) ('a' + (i % 26));
            if (i > 0) primaryData.append(' ');
            primaryData.append(c);
            int start = i * 2;
            int end = start + 1;
            tvBuilder.append("[(").append(start).append("-").append(end)
                .append(")s:").append(c).append("|i:").append(c)
                .append("|_").append(i).append("$<i>").append(start)
                .append("<i>").append(end);
            if (i == 0) {
                tvBuilder.append("|-:tokens$<i>").append(tokenCount);
            }
            tvBuilder.append("]");
        }
        fd.addTV("tokens", primaryData.toString(), tvBuilder.toString());
        return fd;
    }


    @Test
    public void testStatsAfterDeleteSingleSegment () throws IOException {
        KrillIndex ki = new KrillIndex();

        ki.addDoc(createSimpleDoc("A/B/001", "Frank", 5));
        ki.addDoc(createSimpleDoc("A/B/002", "Peter", 10));
        ki.addDoc(createSimpleDoc("A/B/003", "Frank", 7));
        ki.commit();

        assertEquals(3, ki.numberOf("documents"));
        assertEquals(22, ki.numberOf("tokens"));

        ki.delDocs("textSigle", "A/B/002");
        ki.commit();

        assertEquals(2, ki.numberOf("documents"));
        assertEquals(12, ki.numberOf("tokens"));

        ki.close();
    }


    @Test
    public void testStatsAfterDeleteMultipleSegments () throws IOException {
        KrillIndex ki = new KrillIndex();

        ki.addDoc(createSimpleDoc("A/B/001", "Frank", 5));
        ki.commit();

        ki.addDoc(createSimpleDoc("A/B/002", "Peter", 10));
        ki.commit();

        ki.addDoc(createSimpleDoc("A/B/003", "Frank", 7));
        ki.commit();

        assertEquals(3, ki.numberOf("documents"));
        assertEquals(22, ki.numberOf("tokens"));

        ki.delDocs("textSigle", "A/B/002");
        ki.commit();

        assertEquals(2, ki.numberOf("documents"));
        assertEquals(12, ki.numberOf("tokens"));

        ki.close();
    }


    @Test
    public void testStatsAfterUpsertSingleSegment () throws IOException {
        KrillIndex ki = new KrillIndex();

        ki.addDoc(createSimpleDoc("A/B/001", "Frank", 5));
        ki.addDoc(createSimpleDoc("A/B/002", "Peter", 10));
        ki.commit();

        assertEquals(2, ki.numberOf("documents"));
        assertEquals(15, ki.numberOf("tokens"));

        FieldDocument updated = createSimpleDoc("A/B/001", "Frank", 3);
        ki.upsertDoc(updated);
        ki.commit();

        assertEquals(2, ki.numberOf("documents"));
        assertEquals(13, ki.numberOf("tokens"));

        ki.close();
    }


    @Test
    public void testStatsAfterUpsertMultipleSegments () throws IOException {
        KrillIndex ki = new KrillIndex();

        ki.addDoc(createSimpleDoc("A/B/001", "Frank", 5));
        ki.commit();

        ki.addDoc(createSimpleDoc("A/B/002", "Peter", 10));
        ki.commit();

        ki.addDoc(createSimpleDoc("A/B/003", "Frank", 7));
        ki.commit();

        assertEquals(3, ki.numberOf("documents"));
        assertEquals(22, ki.numberOf("tokens"));

        FieldDocument updated = createSimpleDoc("A/B/002", "Peter", 4);
        ki.upsertDoc(updated);
        ki.commit();

        assertEquals(3, ki.numberOf("documents"));
        assertEquals(16, ki.numberOf("tokens"));

        ki.close();
    }


    @Test
    public void testStatsWithVCAfterDelete () throws IOException {
        KrillIndex ki = new KrillIndex();

        ki.addDoc(createSimpleDoc("A/B/001", "Frank", 5));
        ki.addDoc(createSimpleDoc("A/B/002", "Peter", 10));
        ki.addDoc(createSimpleDoc("A/B/003", "Frank", 7));
        ki.commit();

        CollectionBuilder cb = new CollectionBuilder();
        KrillCollection kc = new KrillCollection(ki);
        kc.fromBuilder(cb.term("author", "Frank"));

        assertEquals(2, kc.docCount());
        assertEquals(12, kc.numberOf("tokens", "tokens"));

        ki.delDocs("textSigle", "A/B/001");
        ki.commit();

        kc = new KrillCollection(ki);
        kc.fromBuilder(cb.term("author", "Frank"));

        assertEquals(1, kc.docCount());
        assertEquals(7, kc.numberOf("tokens", "tokens"));

        ki.close();
    }


    @Test
    public void testStatsWithVCAfterDeleteMultipleSegments ()
            throws IOException {
        KrillIndex ki = new KrillIndex();

        ki.addDoc(createSimpleDoc("A/B/001", "Frank", 5));
        ki.commit();

        ki.addDoc(createSimpleDoc("A/B/002", "Peter", 10));
        ki.commit();

        ki.addDoc(createSimpleDoc("A/B/003", "Frank", 7));
        ki.commit();

        CollectionBuilder cb = new CollectionBuilder();
        KrillCollection kc = new KrillCollection(ki);
        kc.fromBuilder(cb.term("author", "Frank"));

        assertEquals(2, kc.docCount());
        assertEquals(12, kc.numberOf("tokens", "tokens"));

        ki.delDocs("textSigle", "A/B/001");
        ki.commit();

        kc = new KrillCollection(ki);
        kc.fromBuilder(cb.term("author", "Frank"));

        assertEquals(1, kc.docCount());
        assertEquals(7, kc.numberOf("tokens", "tokens"));

        ki.close();
    }


    @Test
    public void testStatsWithVCAfterUpsert () throws IOException {
        KrillIndex ki = new KrillIndex();

        ki.addDoc(createSimpleDoc("A/B/001", "Frank", 5));
        ki.addDoc(createSimpleDoc("A/B/002", "Peter", 10));
        ki.addDoc(createSimpleDoc("A/B/003", "Frank", 7));
        ki.commit();

        CollectionBuilder cb = new CollectionBuilder();
        KrillCollection kc = new KrillCollection(ki);
        kc.fromBuilder(cb.term("author", "Frank"));

        assertEquals(2, kc.docCount());
        assertEquals(12, kc.numberOf("tokens", "tokens"));

        FieldDocument updated = createSimpleDoc("A/B/001", "Frank", 3);
        ki.upsertDoc(updated);
        ki.commit();

        kc = new KrillCollection(ki);
        kc.fromBuilder(cb.term("author", "Frank"));

        assertEquals(2, kc.docCount());
        assertEquals(10, kc.numberOf("tokens", "tokens"));

        ki.close();
    }


    @Test
    public void testStatsWithVCAfterUpsertMultipleSegments ()
            throws IOException {
        KrillIndex ki = new KrillIndex();

        ki.addDoc(createSimpleDoc("A/B/001", "Frank", 5));
        ki.commit();

        ki.addDoc(createSimpleDoc("A/B/002", "Peter", 10));
        ki.commit();

        ki.addDoc(createSimpleDoc("A/B/003", "Frank", 7));
        ki.commit();

        CollectionBuilder cb = new CollectionBuilder();
        KrillCollection kc = new KrillCollection(ki);
        kc.fromBuilder(cb.term("author", "Frank"));

        assertEquals(2, kc.docCount());
        assertEquals(12, kc.numberOf("tokens", "tokens"));

        FieldDocument updated = createSimpleDoc("A/B/002", "Peter", 4);
        ki.upsertDoc(updated);
        ki.commit();

        kc = new KrillCollection(ki);
        kc.fromBuilder(cb.term("author", "Peter"));

        assertEquals(1, kc.docCount());
        assertEquals(4, kc.numberOf("tokens", "tokens"));

        ki.close();
    }


    @Test
    public void testStatsConsistencyAfterMultipleUpserts ()
            throws IOException {
        KrillIndex ki = new KrillIndex();

        ki.addDoc(createSimpleDoc("A/B/001", "Frank", 5));
        ki.addDoc(createSimpleDoc("A/B/002", "Peter", 10));
        ki.commit();

        assertEquals(2, ki.numberOf("documents"));
        assertEquals(15, ki.numberOf("tokens"));

        ki.upsertDoc(createSimpleDoc("A/B/001", "Frank", 3));
        ki.commit();

        assertEquals(2, ki.numberOf("documents"));
        assertEquals(13, ki.numberOf("tokens"));

        ki.upsertDoc(createSimpleDoc("A/B/001", "Frank", 8));
        ki.commit();

        assertEquals(2, ki.numberOf("documents"));
        assertEquals(18, ki.numberOf("tokens"));

        ki.upsertDoc(createSimpleDoc("A/B/002", "Peter", 2));
        ki.commit();

        assertEquals(2, ki.numberOf("documents"));
        assertEquals(10, ki.numberOf("tokens"));

        ki.close();
    }


    @Test
    public void testStatsAfterDeleteAllInSegment () throws IOException {
        KrillIndex ki = new KrillIndex();

        ki.addDoc(createSimpleDoc("A/B/001", "Frank", 5));
        ki.commit();

        ki.addDoc(createSimpleDoc("A/B/002", "Peter", 10));
        ki.commit();

        assertEquals(2, ki.numberOf("documents"));
        assertEquals(15, ki.numberOf("tokens"));

        ki.delDocs("textSigle", "A/B/001");
        ki.commit();

        assertEquals(1, ki.numberOf("documents"));
        assertEquals(10, ki.numberOf("tokens"));

        ki.delDocs("textSigle", "A/B/002");
        ki.commit();

        assertEquals(0, ki.numberOf("documents"));
        assertEquals(0, ki.numberOf("tokens"));

        ki.close();
    }


    @Test
    public void testStatsDocCountAndTokensConsistentWithVC ()
            throws IOException {
        KrillIndex ki = new KrillIndex();

        ki.addDoc(createSimpleDoc("A/B/001", "Frank", 5));
        ki.addDoc(createSimpleDoc("A/B/002", "Peter", 10));
        ki.commit();

        ki.addDoc(createSimpleDoc("A/B/003", "Frank", 7));
        ki.commit();

        ki.upsertDoc(createSimpleDoc("A/B/001", "Frank", 3));
        ki.commit();

        ki.delDocs("textSigle", "A/B/002");
        ki.commit();

        assertEquals(2, ki.numberOf("documents"));
        assertEquals(10, ki.numberOf("tokens"));

        CollectionBuilder cb = new CollectionBuilder();

        KrillCollection kcFrank = new KrillCollection(ki);
        kcFrank.fromBuilder(cb.term("author", "Frank"));
        assertEquals(2, kcFrank.docCount());
        assertEquals(10, kcFrank.numberOf("tokens", "tokens"));

        KrillCollection kcPeter = new KrillCollection(ki);
        kcPeter.fromBuilder(cb.term("author", "Peter"));
        assertEquals(0, kcPeter.docCount());
        assertEquals(0, kcPeter.numberOf("tokens", "tokens"));

        ki.close();
    }


    @Test
    public void testStatsWithNegatedVCAfterDelete () throws IOException {
        KrillIndex ki = new KrillIndex();

        ki.addDoc(createSimpleDoc("A/B/001", "Frank", 5));
        ki.addDoc(createSimpleDoc("A/B/002", "Peter", 10));
        ki.addDoc(createSimpleDoc("A/B/003", "Frank", 7));
        ki.commit();

        ki.delDocs("textSigle", "A/B/002");
        ki.commit();

        CollectionBuilder cb = new CollectionBuilder();
        KrillCollection kc = new KrillCollection(ki);
        kc.fromBuilder(cb.term("author", "Peter").not());

        assertEquals(2, kc.docCount());
        assertEquals(12, kc.numberOf("tokens", "tokens"));

        ki.close();
    }


    @Test
    public void testStatsAfterUpsertChangingAuthor () throws IOException {
        KrillIndex ki = new KrillIndex();

        ki.addDoc(createSimpleDoc("A/B/001", "Frank", 5));
        ki.addDoc(createSimpleDoc("A/B/002", "Peter", 10));
        ki.commit();

        ki.upsertDoc(createSimpleDoc("A/B/001", "Peter", 5));
        ki.commit();

        CollectionBuilder cb = new CollectionBuilder();

        KrillCollection kcFrank = new KrillCollection(ki);
        kcFrank.fromBuilder(cb.term("author", "Frank"));
        assertEquals(0, kcFrank.docCount());
        assertEquals(0, kcFrank.numberOf("tokens", "tokens"));

        KrillCollection kcPeter = new KrillCollection(ki);
        kcPeter.fromBuilder(cb.term("author", "Peter"));
        assertEquals(2, kcPeter.docCount());
        assertEquals(15, kcPeter.numberOf("tokens", "tokens"));

        ki.close();
    }


    @Test
    public void testStatsAfterManyUpsertsAcrossSegments () throws IOException {
        KrillIndex ki = new KrillIndex();

        for (int i = 1; i <= 10; i++) {
            ki.addDoc(createSimpleDoc(
                "C/D/" + String.format("%03d", i),
                i <= 5 ? "Frank" : "Peter",
                i * 2));
            ki.commit();
        }

        assertEquals(10, ki.numberOf("documents"));
        assertEquals(110, ki.numberOf("tokens"));

        for (int i = 1; i <= 10; i++) {
            ki.upsertDoc(createSimpleDoc(
                "C/D/" + String.format("%03d", i),
                i <= 5 ? "Frank" : "Peter",
                i));
            ki.commit();
        }

        assertEquals(10, ki.numberOf("documents"));
        assertEquals(55, ki.numberOf("tokens"));

        CollectionBuilder cb = new CollectionBuilder();
        KrillCollection kcFrank = new KrillCollection(ki);
        kcFrank.fromBuilder(cb.term("author", "Frank"));
        assertEquals(5, kcFrank.docCount());
        assertEquals(15, kcFrank.numberOf("tokens", "tokens"));

        KrillCollection kcPeter = new KrillCollection(ki);
        kcPeter.fromBuilder(cb.term("author", "Peter"));
        assertEquals(5, kcPeter.docCount());
        assertEquals(40, kcPeter.numberOf("tokens", "tokens"));

        ki.close();
    }


    @Test
    public void testStatsDocCountVsNumberOfDocumentsAfterUpsert ()
            throws IOException {
        KrillIndex ki = new KrillIndex();

        ki.addDoc(createSimpleDoc("A/B/001", "Frank", 5));
        ki.addDoc(createSimpleDoc("A/B/002", "Peter", 10));
        ki.addDoc(createSimpleDoc("A/B/003", "Frank", 7));
        ki.commit();

        ki.upsertDoc(createSimpleDoc("A/B/001", "Frank", 3));
        ki.commit();

        ki.upsertDoc(createSimpleDoc("A/B/002", "Peter", 8));
        ki.commit();

        CollectionBuilder cb = new CollectionBuilder();
        KrillCollection kc = new KrillCollection(ki);
        kc.fromBuilder(cb.term("author", "Frank"));

        long docCountMethod = kc.docCount();
        long numberOfDocuments = kc.numberOf("documents");
        assertEquals(docCountMethod, numberOfDocuments);
        assertEquals(2, docCountMethod);

        kc = new KrillCollection(ki);
        kc.fromBuilder(
            cb.orGroup()
                .with(cb.term("author", "Frank"))
                .with(cb.term("author", "Peter")));
        long allDocs = kc.docCount();
        long allDocsNumberOf = kc.numberOf("documents");
        assertEquals(allDocs, allDocsNumberOf);

        ki.close();
    }


    @Test
    public void testStatsWithDateFilterAfterUpsert () throws IOException {
        KrillIndex ki = new KrillIndex();

        FieldDocument fd1 = createSimpleDoc("A/B/001", "Frank", 5);
        fd1.addDate("pubDate", 20180315);
        ki.addDoc(fd1);

        FieldDocument fd2 = createSimpleDoc("A/B/002", "Peter", 10);
        fd2.addDate("pubDate", 20180620);
        ki.addDoc(fd2);

        FieldDocument fd3 = createSimpleDoc("A/B/003", "Frank", 7);
        fd3.addDate("pubDate", 20190101);
        ki.addDoc(fd3);
        ki.commit();

        CollectionBuilder cb = new CollectionBuilder();
        KrillCollection kc = new KrillCollection(ki);
        kc.fromBuilder(cb.date("pubDate", "2018"));

        assertEquals(2, kc.docCount());
        assertEquals(15, kc.numberOf("tokens", "tokens"));

        FieldDocument updated = createSimpleDoc("A/B/001", "Frank", 3);
        updated.addDate("pubDate", 20180315);
        ki.upsertDoc(updated);
        ki.commit();

        kc = new KrillCollection(ki);
        kc.fromBuilder(cb.date("pubDate", "2018"));

        assertEquals(2, kc.docCount());
        assertEquals(13, kc.numberOf("tokens", "tokens"));

        ki.close();
    }


    @Test
    public void testStatsWithDateFilterAndMultipleSegmentsAfterUpsert ()
            throws IOException {
        KrillIndex ki = new KrillIndex();

        FieldDocument fd1 = createSimpleDoc("A/B/001", "Frank", 5);
        fd1.addDate("pubDate", 20180315);
        ki.addDoc(fd1);
        ki.commit();

        FieldDocument fd2 = createSimpleDoc("A/B/002", "Peter", 10);
        fd2.addDate("pubDate", 20180620);
        ki.addDoc(fd2);
        ki.commit();

        FieldDocument fd3 = createSimpleDoc("A/B/003", "Frank", 7);
        fd3.addDate("pubDate", 20190101);
        ki.addDoc(fd3);
        ki.commit();

        CollectionBuilder cb = new CollectionBuilder();

        KrillCollection kc = new KrillCollection(ki);
        kc.fromBuilder(cb.date("pubDate", "2018"));
        assertEquals(2, kc.docCount());
        assertEquals(15, kc.numberOf("tokens", "tokens"));

        FieldDocument updated = createSimpleDoc("A/B/001", "Frank", 3);
        updated.addDate("pubDate", 20180315);
        ki.upsertDoc(updated);
        ki.commit();

        kc = new KrillCollection(ki);
        kc.fromBuilder(cb.date("pubDate", "2018"));
        assertEquals(2, kc.docCount());
        assertEquals(13, kc.numberOf("tokens", "tokens"));

        FieldDocument updated2 = createSimpleDoc("A/B/002", "Peter", 4);
        updated2.addDate("pubDate", 20180620);
        ki.upsertDoc(updated2);
        ki.commit();

        kc = new KrillCollection(ki);
        kc.fromBuilder(cb.date("pubDate", "2018"));
        assertEquals(2, kc.docCount());
        assertEquals(7, kc.numberOf("tokens", "tokens"));

        ki.close();
    }


    @Test
    public void testStatsWithComplexVCAfterMultipleUpserts ()
            throws IOException {
        KrillIndex ki = new KrillIndex();

        for (int i = 1; i <= 5; i++) {
            FieldDocument fd = createSimpleDoc(
                "X/Y/" + String.format("%03d", i),
                i <= 3 ? "Frank" : "Peter",
                i * 3);
            fd.addDate("pubDate", 20180100 + i);
            ki.addDoc(fd);
            ki.commit();
        }

        assertEquals(5, ki.numberOf("documents"));
        assertEquals(45, ki.numberOf("tokens"));

        CollectionBuilder cb = new CollectionBuilder();
        KrillCollection kc = new KrillCollection(ki);
        kc.fromBuilder(
            cb.andGroup()
                .with(cb.date("pubDate", "2018"))
                .with(cb.term("author", "Frank")));
        assertEquals(3, kc.docCount());
        assertEquals(18, kc.numberOf("tokens", "tokens"));

        for (int i = 1; i <= 5; i++) {
            FieldDocument fd = createSimpleDoc(
                "X/Y/" + String.format("%03d", i),
                i <= 3 ? "Frank" : "Peter",
                i);
            fd.addDate("pubDate", 20180100 + i);
            ki.upsertDoc(fd);
            ki.commit();
        }

        assertEquals(5, ki.numberOf("documents"));
        assertEquals(15, ki.numberOf("tokens"));

        kc = new KrillCollection(ki);
        kc.fromBuilder(
            cb.andGroup()
                .with(cb.date("pubDate", "2018"))
                .with(cb.term("author", "Frank")));
        assertEquals(3, kc.docCount());
        assertEquals(6, kc.numberOf("tokens", "tokens"));

        ki.close();
    }


    @Test
    public void testStatsWithNegationAndDeletedDocs ()
            throws IOException {
        KrillIndex ki = new KrillIndex();

        ki.addDoc(createSimpleDoc("A/B/001", "Frank", 5));
        ki.commit();

        ki.addDoc(createSimpleDoc("A/B/002", "Peter", 10));
        ki.commit();

        ki.addDoc(createSimpleDoc("A/B/003", "Frank", 7));
        ki.commit();

        ki.addDoc(createSimpleDoc("A/B/004", "Maria", 3));
        ki.commit();

        ki.delDocs("textSigle", "A/B/002");
        ki.commit();

        CollectionBuilder cb = new CollectionBuilder();
        KrillCollection kc = new KrillCollection(ki);
        kc.fromBuilder(cb.term("author", "Frank").not());

        assertEquals(1, kc.docCount());
        assertEquals(3, kc.numberOf("tokens", "tokens"));

        kc = new KrillCollection(ki);
        kc.fromBuilder(cb.term("author", "Peter").not());

        assertEquals(3, kc.docCount());
        assertEquals(15, kc.numberOf("tokens", "tokens"));

        ki.close();
    }


    @Test
    public void testStatsUpsertThenBatchDelete () throws IOException {
        KrillIndex ki = new KrillIndex();

        for (int i = 1; i <= 5; i++) {
            ki.addDoc(createSimpleDoc(
                "A/B/" + String.format("%03d", i), "Frank", i * 2));
            ki.commit();
        }

        assertEquals(5, ki.numberOf("documents"));
        assertEquals(30, ki.numberOf("tokens"));

        ki.upsertDoc(createSimpleDoc("A/B/003", "Frank", 1));
        ki.commit();

        assertEquals(5, ki.numberOf("documents"));
        assertEquals(25, ki.numberOf("tokens"));

        ki.delDocs("textSigle", "A/B/001");
        ki.delDocs("textSigle", "A/B/002");
        ki.delDocs("textSigle", "A/B/004");
        ki.commit();

        assertEquals(2, ki.numberOf("documents"));
        assertEquals(11, ki.numberOf("tokens"));

        ki.close();
    }


    @Test
    public void testStatsVCMatchesOnlyDeletedDocs () throws IOException {
        KrillIndex ki = new KrillIndex();

        ki.addDoc(createSimpleDoc("A/B/001", "Frank", 5));
        ki.addDoc(createSimpleDoc("A/B/002", "Peter", 10));
        ki.commit();

        ki.delDocs("textSigle", "A/B/002");
        ki.commit();

        CollectionBuilder cb = new CollectionBuilder();
        KrillCollection kc = new KrillCollection(ki);
        kc.fromBuilder(cb.term("author", "Peter"));
        assertEquals(0, kc.docCount());
        assertEquals(0, kc.numberOf("tokens", "tokens"));

        ki.close();
    }


    @Test
    public void testStatsAfterCloseAndReopen () throws IOException {
        KrillIndex ki = new KrillIndex();

        ki.addDoc(createSimpleDoc("A/B/001", "Frank", 5));
        ki.addDoc(createSimpleDoc("A/B/002", "Peter", 10));
        ki.commit();

        assertEquals(2, ki.numberOf("documents"));
        assertEquals(15, ki.numberOf("tokens"));

        ki.close();

        ki.upsertDoc(createSimpleDoc("A/B/001", "Frank", 3));
        ki.commit();

        assertEquals(2, ki.numberOf("documents"));
        assertEquals(13, ki.numberOf("tokens"));

        ki.close();

        assertEquals(2, ki.numberOf("documents"));
        assertEquals(13, ki.numberOf("tokens"));

        ki.close();
    }


    @Test
    public void testStatsMultipleUpsertsWithoutIntermediateCommit ()
            throws IOException {
        KrillIndex ki = new KrillIndex();

        ki.addDoc(createSimpleDoc("A/B/001", "Frank", 5));
        ki.addDoc(createSimpleDoc("A/B/002", "Peter", 10));
        ki.addDoc(createSimpleDoc("A/B/003", "Frank", 7));
        ki.commit();

        assertEquals(3, ki.numberOf("documents"));
        assertEquals(22, ki.numberOf("tokens"));

        ki.upsertDoc(createSimpleDoc("A/B/001", "Frank", 3));
        ki.upsertDoc(createSimpleDoc("A/B/002", "Peter", 4));
        ki.upsertDoc(createSimpleDoc("A/B/003", "Frank", 2));
        ki.commit();

        assertEquals(3, ki.numberOf("documents"));
        assertEquals(9, ki.numberOf("tokens"));

        CollectionBuilder cb = new CollectionBuilder();
        KrillCollection kc = new KrillCollection(ki);
        kc.fromBuilder(cb.term("author", "Frank"));
        assertEquals(2, kc.docCount());
        assertEquals(5, kc.numberOf("tokens", "tokens"));

        ki.close();
    }


    @Test
    public void testStatsAfterForceMerge () throws IOException {
        KrillIndex ki = new KrillIndex();

        ki.addDoc(createSimpleDoc("A/B/001", "Frank", 5));
        ki.commit();
        ki.addDoc(createSimpleDoc("A/B/002", "Peter", 10));
        ki.commit();
        ki.addDoc(createSimpleDoc("A/B/003", "Frank", 7));
        ki.commit();

        ki.upsertDoc(createSimpleDoc("A/B/001", "Frank", 3));
        ki.commit();
        ki.upsertDoc(createSimpleDoc("A/B/002", "Peter", 4));
        ki.commit();

        ki.writer().forceMerge(1);
        ki.commit();

        assertEquals(3, ki.numberOf("documents"));
        assertEquals(14, ki.numberOf("tokens"));

        CollectionBuilder cb = new CollectionBuilder();
        KrillCollection kc = new KrillCollection(ki);
        kc.fromBuilder(cb.term("author", "Frank"));
        assertEquals(2, kc.docCount());
        assertEquals(10, kc.numberOf("tokens", "tokens"));

        ki.close();
    }


    @Test
    public void testStatsStressWithManySegments () throws IOException {
        KrillIndex ki = new KrillIndex();

        int numDocs = 100;
        long expectedTotalTokens = 0;

        for (int i = 1; i <= numDocs; i++) {
            String author = (i % 3 == 0) ? "Peter" : "Frank";
            int tokenCount = (i % 7) + 1;
            FieldDocument fd = createSimpleDoc(
                "S/T/" + String.format("%04d", i), author, tokenCount);
            fd.addDate("pubDate", 20180100 + (i % 28) + 1);
            ki.addDoc(fd);
            if (i % 10 == 0) ki.commit();
            expectedTotalTokens += tokenCount;
        }
        ki.commit();

        assertEquals(numDocs, ki.numberOf("documents"));
        assertEquals(expectedTotalTokens, ki.numberOf("tokens"));

        long newTotalTokens = 0;
        int newFrankDocs = 0;
        long newFrankTokens = 0;

        for (int i = 1; i <= numDocs; i++) {
            String author = (i % 3 == 0) ? "Peter" : "Frank";
            int tokenCount = (i % 5) + 1;
            FieldDocument fd = createSimpleDoc(
                "S/T/" + String.format("%04d", i), author, tokenCount);
            fd.addDate("pubDate", 20180100 + (i % 28) + 1);
            ki.upsertDoc(fd);
            if (i % 10 == 0) ki.commit();
            newTotalTokens += tokenCount;
            if (author.equals("Frank")) {
                newFrankDocs++;
                newFrankTokens += tokenCount;
            }
        }
        ki.commit();

        assertEquals(numDocs, ki.numberOf("documents"));
        assertEquals(newTotalTokens, ki.numberOf("tokens"));

        CollectionBuilder cb = new CollectionBuilder();
        KrillCollection kc = new KrillCollection(ki);
        kc.fromBuilder(cb.term("author", "Frank"));
        assertEquals(newFrankDocs, kc.docCount());
        assertEquals(newFrankTokens, kc.numberOf("tokens", "tokens"));

        kc = new KrillCollection(ki);
        kc.fromBuilder(
            cb.andGroup()
                .with(cb.date("pubDate", "2018"))
                .with(cb.term("author", "Frank")));
        assertEquals(newFrankDocs, kc.docCount());
        assertEquals(newFrankTokens, kc.numberOf("tokens", "tokens"));

        ki.close();
    }

    private static String createDocString1 () {
        return new String(
            "{"
            + "  \"data\" : {"
            + "    \"text\" : \"abc\","
            + "    \"name\" : \"tokens\","
            + "    \"stream\" : ["
            + "       [ \"s:a\", \"i:a\", \"_0$<i>0<i>1\", \"-:t$<i>3\"],"
            + "       [ \"s:b\", \"i:b\", \"_1$<i>1<i>2\" ],"
            + "       [ \"s:c\", \"i:c\", \"_2$<i>2<i>3\" ]"
            + "    ]"
            + "  },"
            + "  \"fields\" : ["
            + "    {"
            + "      \"@type\" : \"koral:field\","
            + "      \"type\" : \"type:string\","
            + "      \"key\" : \"textSigle\","
            + "      \"value\" : \"aa/bb/cc\""
            + "    },"
            + "    {"
            + "      \"@type\" : \"koral:field\","
            + "      \"type\" : \"type:integer\","
            + "      \"key\" : \"alter\","
            + "      \"value\" : 40"
            + "    },"
            + "    {"
            + "      \"@type\" : \"koral:field\","
            + "      \"type\" : \"type:string\","
            + "      \"key\" : \"name\","
            + "      \"value\" : \"Frank\""
            + "    },"
            + "    {"
            + "      \"@type\" : \"koral:field\","
            + "      \"type\" : \"type:string\","
            + "      \"key\" : \"name\","
            + "      \"value\" : \"Julian\""
            + "    },"
            + "    {"
            + "      \"@type\" : \"koral:field\","
            + "      \"type\" : \"type:string\","
            + "      \"key\" : \"schluesselwoerter\","
            + "      \"value\" : [\"musik\",\"unterhaltung\"]"
            + "    },"
            + "    {"
            + "      \"@type\" : \"koral:field\","
            + "      \"type\" : \"type:keywords\","
            + "      \"key\" : \"tags\","
            + "      \"value\" : \"nachrichten feuilleton\""
            + "    },"
            + "    {"
            + "      \"@type\" : \"koral:field\","
            + "      \"type\" : \"type:keywords\","
            + "      \"key\" : \"tags\","
            + "      \"value\" : [\"sport\",\"raetsel\"]"
            + "    },"
            + "    {"
            + "      \"@type\" : \"koral:field\","
            + "      \"type\" : \"type:text\","
            + "      \"key\" : \"titel\","
            + "      \"value\" : \"Der alte Baum\""
            + "    },"
            + "    {"
            + "      \"@type\" : \"koral:field\","
            + "      \"type\" : \"type:attachement\","
            + "      \"key\" : \"anhang\","
            + "      \"value\" : \"data:application/x.korap-link,http://spiegel.de/\""
            + "    },"
            + "    {"
            + "      \"@type\" : \"koral:field\","
            + "      \"type\" : \"type:store\","
            + "      \"key\" : \"referenz\","
            + "      \"value\" : \"So war das\""
            + "    },"
            + "    {"
            + "      \"@type\" : \"koral:field\","
            + "      \"type\" : \"type:date\","
            + "      \"key\" : \"datum\","
            + "      \"value\" : \"2018-04-03\""
            + "    }"
            + "  ]"
            + "}");
    };
};
