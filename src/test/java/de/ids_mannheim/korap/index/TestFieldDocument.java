package de.ids_mannheim.korap.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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
import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.KrillMeta;
import de.ids_mannheim.korap.KrillQuery;
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
        
        assertEquals(doc.getField("title").name(), "title");
        assertEquals(doc.getField("title").stringValue(), "Wikipedia");
       
        assertEquals(doc.getField("corpusID").name(), "corpusID");
        assertEquals(doc.getField("corpusID").stringValue(), "WPD");

        assertEquals(doc.getField("ID").name(), "ID");
        assertEquals(doc.getField("ID").stringValue(), "WPD-AAA-00001");

        assertEquals(doc.getField("subTitle").name(), "subTitle");
        assertEquals(doc.getField("subTitle").stringValue(),
                "Die freie Enzyklopädie");

        assertEquals(doc.getField("pubPlace").name(), "pubPlace");
        assertEquals(doc.getField("pubPlace").stringValue(), "Bochum");

        assertEquals(doc.getField("lastModified").name(), "lastModified");
        assertEquals(doc.getField("lastModified").stringValue(), "20130717");

        assertEquals(doc.getField("tokens").name(), "tokens");
        assertEquals(doc.getField("tokens").stringValue(), "abc");

        assertEquals(doc.getField("author").name(), "author");
        assertEquals(doc.getField("author").stringValue(),
                "Peter Frankenfeld");

        assertEquals(doc.getField("layerInfo").name(), "layerInfo");
        assertEquals(doc.getField("layerInfo").stringValue(),
                "opennlp/p=pos");

        assertEquals(doc.getField("textClass").name(), "textClass");
        assertEquals(doc.getField("textClass").stringValue(),
                "music entertainment");
        assertEquals(doc.getField("Wikilink").name(), "Wikilink");
        assertEquals(doc.getField("Wikilink").stringValue(),
                     "data:application/x.korap-link,https://de.wikipedia.org/wiki/Beispiel"
            );

        assertEquals(doc.getField("justanumber").numericValue().intValue(), 12345678);

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

        assertEquals(fd.getPrimaryData(), "abc");
        assertEquals(fd.getCorpusID(), "WPD");
        assertEquals(fd.getID(), "WPD-AAA-00001");
        assertEquals(fd.getFieldValue("textClass"), "music entertainment");
        assertEquals(fd.getFieldValue("author"), "Peter Frankenfeld");
        assertEquals(fd.getFieldValue("title"), "Wikipedia");
        assertEquals(fd.getFieldValue("subTitle"), "Die freie Enzyklopädie");
        assertEquals(fd.getFieldValue("pubPlace"), "Bochum");
        assertEquals(fd.getFieldValueAsDate("pubDate").toDisplay(), "2013-06-17");

        QueryBuilder kq = new QueryBuilder("tokens");
        Result kr = ki
                .search((SpanQuery) kq.seq(kq.nr(3, kq.seg("s:b"))).toQuery());

        Match km = kr.getMatch(0);

        assertEquals(km.getPrimaryData(), "abc");
        assertEquals(km.getCorpusID(), "WPD");
        assertEquals(km.getDocID(), "WPD-AAA-00001");
        assertEquals(km.getFieldValue("textClass"), "music entertainment");
        assertEquals(km.getFieldValue("author"), "Peter Frankenfeld");
        assertEquals(km.getFieldValue("title"), "Wikipedia");
        assertEquals(km.getFieldValue("subTitle"), "Die freie Enzyklopädie");
        assertEquals(km.getFieldValue("pubPlace"), "Bochum");
        assertEquals(km.getFieldValueAsDate("pubDate").toDisplay(), "2013-06-17");

        assertEquals(km.getSnippetBrackets(), "a[[{3:b}]]c");
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

        assertEquals(fd.getPrimaryData(), "abc");
        assertEquals(fd.getCorpusID(), "WPD");
        assertEquals(fd.getID(), "WPD-AAA-00001");
        assertEquals(fd.getFieldValue("textClass"), "music entertainment");
        assertEquals(fd.getFieldValue("author"), "Peter Frankenfeld");
        assertEquals(fd.getFieldValue("title"), "Wikipedia");
        assertEquals(fd.getFieldValue("subTitle"), "Die freie Enzyklopädie");
        assertEquals(fd.getFieldValue("pubPlace"), "Bochum");
        assertEquals(fd.getFieldValueAsDate("pubDate").toDisplay(), "");
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

        assertEquals(fd.getPrimaryData(), "abc");
        // assertEquals(fd.doc.getField("corpusID").stringValue(), "WPD");
        assertEquals(fd.doc.getField("textSigle").stringValue(), "x/y/z");
        assertEquals(fd.doc.getField("ID").stringValue(), "WPD-AAA-00001");
        assertEquals(fd.doc.getField("textClass").stringValue(), "music entertainment");
        assertEquals(fd.doc.getField("author").stringValue(), "Peter Frankenfeld");
        assertEquals(fd.doc.getField("title").stringValue(), "Wikipedia");
        assertEquals(fd.doc.getField("subTitle").stringValue(), "Die freie Enzyklopädie");
        assertEquals(fd.doc.getField("pubPlace").stringValue(), "Bochum");
        assertEquals(fd.doc.getField("pubDate").stringValue(), "20150501");
        assertEquals(fd.doc.getField("link").stringValue(), "data:application/x.korap-link,https://de.wikipedia.org/wiki/Beispiel");

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

        assertEquals(fd.getPrimaryData(), "abc");
        assertEquals(fd.doc.getField("alter").stringValue(), "40.0");
        assertEquals(fd.doc.getField("name").stringValue(), "Frank");
        assertEquals(fd.doc.getField("schluesselwoerter").stringValue(), "musik unterhaltung");
        assertEquals(fd.doc.getField("tags").stringValue(), "nachrichten feuilleton sport raetsel");
        assertEquals(fd.doc.getField("titel").stringValue(), "Der alte Baum");
        assertEquals(fd.doc.getField("anhang").stringValue(), "data:application/x.korap-link,http://spiegel.de/");
        assertEquals(fd.doc.getField("referenz").stringValue(), "So war das");
        assertEquals(fd.doc.getField("datum").stringValue(), "20180403");

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


    @Test
    public void indexUpsert () throws Exception {
        KrillIndex ki = new KrillIndex();

        // Add new document
        FieldDocument fd = new FieldDocument();
        fd.addString("textSigle", "AAA/BBB/001");
        fd.addString("content", "Example1");
        ki.upsertDoc(fd);
        ki.commit();

        MetaFields mfs = ki.getFields("AAA/BBB/001");
        assertEquals(mfs.getFieldValue("indexCreationDate").length(), 10);
        assertTrue(mfs.getFieldValue("indexCreationDate").matches("\\d{4}-\\d{2}-\\d{2}"));
        assertEquals(
            mfs.getFieldValue("indexCreationDate"),
            mfs.getFieldValue("indexLastModified")
            );
        assertEquals(mfs.getFieldValue("content"), "Example1");


        // Add new document
        fd = new FieldDocument();
        fd.addString("textSigle", "AAA/BBB/002");
        fd.addString("content", "Example2");

        ki.upsertDoc(fd);
        ki.commit();

        mfs = ki.getFields("AAA/BBB/002");
        assertEquals(mfs.getFieldValue("indexCreationDate").length(), 10);
       
        assertTrue(mfs.getFieldValue("indexCreationDate").matches("\\d{4}-\\d{2}-\\d{2}"));
        assertEquals(mfs.getFieldValue("content"), "Example2");

        fd = new FieldDocument();
        fd.addString("textSigle", "AAA/BBB/001");
        fd.addString("content", "Example3");

        ki.upsertDoc(fd);
        ki.commit();

        mfs = ki.getFields("AAA/BBB/001");
        assertEquals(mfs.getFieldValue("indexCreationDate").length(), 10);
        assertTrue(mfs.getFieldValue("indexCreationDate").matches("\\d{4}-\\d{2}-\\d{2}"));
        assertEquals(mfs.getFieldValue("content"), "Example3");

        assertEquals(ki.numberOf("documents"), 2);

        // Test Inputstream method
        ki.upsertDoc(getClass().getResourceAsStream("/wiki/WPD17-H81-63495.json.gz"), true);
        ki.commit();
        assertEquals(ki.numberOf("documents"), 3);
    };

    
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
