package de.ids_mannheim.korap.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URLDecoder;

import org.apache.lucene.search.spans.SpanQuery;
import org.junit.Test;
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
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.util.QueryException;

@RunWith(JUnit4.class)
public class TestFieldDocument {

    @Test
    public void indexExample1 () throws IOException {
        FieldDocument fd = new FieldDocument();

        fd.addString("corpusID", "WPD");
        fd.addString("ID", "WPD-AAA-00001");
        fd.addText("textClass", "music entertainment");
        fd.addText("author", "Peter Frankenfeld");
        fd.addInt("pubDate", 20130617);
        fd.addText("title", "Wikipedia");
        fd.addText("subTitle", "Die freie Enzyklopädie");
        fd.addStored("layerInfo", "opennlp/p=pos");
        fd.addString("pubPlace", "Bochum");
        fd.addInt("lastModified", 20130717);
        fd.addTV("tokens", "abc", "[(0-1)s:a|i:a|_0$<i>0<i>1|-:t$<i>10]"
                + "[(1-2)s:b|i:b|_1$<i>1<i>2]" + "[(2-3)s:c|i:c|_2$<i>2<i>3]");
        fd.addAttachement("Wikilink", "data:application/x.korap-link,https://de.wikipedia.org/wiki/Beispiel");

        assertEquals(fd.doc.getField("title").name(), "title");
        assertEquals(fd.doc.getField("title").stringValue(), "Wikipedia");

        assertEquals(fd.doc.getField("corpusID").name(), "corpusID");
        assertEquals(fd.doc.getField("corpusID").stringValue(), "WPD");

        assertEquals(fd.doc.getField("ID").name(), "ID");
        assertEquals(fd.doc.getField("ID").stringValue(), "WPD-AAA-00001");

        assertEquals(fd.doc.getField("subTitle").name(), "subTitle");
        assertEquals(fd.doc.getField("subTitle").stringValue(),
                "Die freie Enzyklopädie");

        assertEquals(fd.doc.getField("pubPlace").name(), "pubPlace");
        assertEquals(fd.doc.getField("pubPlace").stringValue(), "Bochum");

        assertEquals(fd.doc.getField("lastModified").name(), "lastModified");
        assertEquals(fd.doc.getField("lastModified").stringValue(), "20130717");

        assertEquals(fd.doc.getField("tokens").name(), "tokens");
        assertEquals(fd.doc.getField("tokens").stringValue(), "abc");

        assertEquals(fd.doc.getField("author").name(), "author");
        assertEquals(fd.doc.getField("author").stringValue(),
                "Peter Frankenfeld");

        assertEquals(fd.doc.getField("layerInfo").name(), "layerInfo");
        assertEquals(fd.doc.getField("layerInfo").stringValue(),
                "opennlp/p=pos");

        assertEquals(fd.doc.getField("textClass").name(), "textClass");
        assertEquals(fd.doc.getField("textClass").stringValue(),
                "music entertainment");
        assertEquals(fd.doc.getField("Wikilink").name(), "Wikilink");
        assertEquals(fd.doc.getField("Wikilink").stringValue(),
                     "data:application/x.korap-link,https://de.wikipedia.org/wiki/Beispiel"
            );
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
        assertEquals(fd.getTextClass(), "music entertainment");
        assertEquals(fd.getAuthor(), "Peter Frankenfeld");
        assertEquals(fd.getTitle(), "Wikipedia");
        assertEquals(fd.getSubTitle(), "Die freie Enzyklopädie");
        assertEquals(fd.getPubPlace(), "Bochum");
        assertEquals(fd.getPubDate().toDisplay(), "2013-06-17");

        QueryBuilder kq = new QueryBuilder("tokens");
        Result kr = ki
                .search((SpanQuery) kq.seq(kq.nr(3, kq.seg("s:b"))).toQuery());

        Match km = kr.getMatch(0);

        assertEquals(km.getPrimaryData(), "abc");
        assertEquals(km.getCorpusID(), "WPD");
        assertEquals(km.getDocID(), "WPD-AAA-00001");
        assertEquals(km.getTextClass(), "music entertainment");
        assertEquals(km.getAuthor(), "Peter Frankenfeld");
        assertEquals(km.getTitle(), "Wikipedia");
        assertEquals(km.getSubTitle(), "Die freie Enzyklopädie");
        assertEquals(km.getPubPlace(), "Bochum");
        assertEquals(km.getPubDate().toDisplay(), "2013-06-17");

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
        assertEquals(fd.getTextClass(), "music entertainment");
        assertEquals(fd.getAuthor(), "Peter Frankenfeld");
        assertEquals(fd.getTitle(), "Wikipedia");
        assertEquals(fd.getSubTitle(), "Die freie Enzyklopädie");
        assertEquals(fd.getPubPlace(), "Bochum");
        assertEquals(fd.getPubDate().toDisplay(), "");
	};

    @Test
    public void indexNewMetaData () throws Exception {

        String json = new String(
            "{"
            + "  \"fields\" : ["
            + "    { "
            + "      \"primaryData\" : \"abc\""
            + "    },"
            + "    {"
            + "      \"name\" : \"tokens\","
            + "      \"data\" : ["
            + "         [ \"s:a\", \"i:a\", \"_0$<i>0<i>1\", \"-:t$<i>3\"],"
            + "         [ \"s:b\", \"i:b\", \"_1$<i>1<i>2\" ],"
            + "         [ \"s:c\", \"i:c\", \"_2$<i>2<i>3\" ]"
            + "      ]"
            + "    }"
            + "  ],"
            + "  \"metaFields\" : ["
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
        assertEquals(fd.doc.getField("corpusID").stringValue(), "WPD");
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
            };
        };
    };
};
