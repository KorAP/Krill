package de.ids_mannheim.korap.search;

import java.util.*;
import java.io.*;

import static de.ids_mannheim.korap.TestSimple.*;

import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.KrillCollection;
import de.ids_mannheim.korap.KrillQuery;
import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.index.FieldDocument;
import de.ids_mannheim.korap.response.Result;
import java.nio.file.Files;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestMetaFields {

    @Test
    public void searchMetaFields () throws IOException {

        // Construct index
        KrillIndex ki = new KrillIndex();
        // Indexing test files
        for (String i : new String[] { "00001", "00002" }) {
            ki.addDoc(
                    getClass().getResourceAsStream("/wiki/" + i + ".json.gz"),
                    true);
        };
        ki.commit();

        String jsonString = getString(getClass().getResource(
                "/queries/metas/fields.jsonld").getFile());

        Krill ks = new Krill(jsonString);

        Result kr = ks.apply(ki);
        assertEquals((long) 17, kr.getTotalResults());
        assertEquals(0, kr.getStartIndex());
        assertEquals(9, kr.getItemsPerPage());

        ObjectMapper mapper = new ObjectMapper();
        JsonNode res = mapper.readTree(kr.toJsonString());
        assertEquals(0, res.at("/matches/0/UID").asInt());
        assertEquals("WPD", res.at("/matches/0/corpusID").asText());
        assertTrue(res.at("/matches/0/docID").isMissingNode());
        assertTrue(res.at("/matches/0/textSigle").isMissingNode());
        assertTrue(res.at("/matches/0/ID").isMissingNode());
        assertTrue(res.at("/matches/0/author").isMissingNode());
        assertTrue(res.at("/matches/0/title").isMissingNode());
        assertTrue(res.at("/matches/0/subTitle").isMissingNode());
        assertTrue(res.at("/matches/0/textClass").isMissingNode());
        assertTrue(res.at("/matches/0/pubPlace").isMissingNode());
        assertTrue(res.at("/matches/0/pubDate").isMissingNode());
        assertTrue(res.at("/matches/0/foundries").isMissingNode());
        assertTrue(res.at("/matches/0/layerInfos").isMissingNode());
        assertTrue(res.at("/matches/0/tokenization").isMissingNode());

        jsonString = getString(getClass().getResource(
                "/queries/metas/fields_2.jsonld").getFile());
        ks = new Krill(jsonString);
        kr = ks.apply(ki);
        assertEquals((long) 17, kr.getTotalResults());
        assertEquals(0, kr.getStartIndex());
        assertEquals(2, kr.getItemsPerPage());

        mapper = new ObjectMapper();
        res = mapper.readTree(kr.toJsonString());
        assertEquals(0, res.at("/matches/0/UID").asInt());
        assertTrue(res.at("/matches/0/corpusID").isMissingNode());
        assertEquals("Ruru,Jens.Ol,Aglarech", res.at("/matches/0/author")
                .asText());
        assertEquals("A", res.at("/matches/0/title").asText());
        assertEquals("WPD_AAA.00001", res.at("/matches/0/docID").asText());
        assertTrue(res.at("/matches/0/textSigle").isMissingNode());
        assertEquals("match-WPD_AAA.00001-p6-7", res.at("/matches/0/matchID")
                .asText());
        // assertEquals("p6-7", res.at("/matches/0/matchID").asText());
        assertEquals("", res.at("/matches/0/subTitle").asText());
        assertEquals("", res.at("/matches/0/textClass").asText());
        assertEquals("", res.at("/matches/0/pubPlace").asText());
        assertEquals("", res.at("/matches/0/pubDate").asText());
        assertEquals("", res.at("/matches/0/foundries").asText());
        assertEquals("", res.at("/matches/0/layerInfo").asText());
        assertEquals("", res.at("/matches/0/tokenization").asText());
    };


    @Test
    public void searchMetaFieldsNew () throws IOException {

        // Construct index
        KrillIndex ki = new KrillIndex();
        ki.addDoc(getClass().getResourceAsStream("/goe/AGX-00002.json"), false);
        ki.commit();

        String jsonString = getString(getClass().getResource(
                "/queries/metas/fields_no.jsonld").getFile());

        Krill ks = new Krill(jsonString);
        Result kr = ks.apply(ki);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode res = mapper.readTree(kr.toJsonString());
        assertEquals(0, res.at("/matches/0/UID").asInt());
        assertEquals("GOE_AGX.00002", res.at("/matches/0/textSigle").asText());
        assertEquals("Maximen und Reflexionen", res.at("/matches/0/title")
                .asText());
        assertEquals("1982", res.at("/matches/0/pubDate").asText());
        assertEquals("Goethe, Johann Wolfgang von", res.at("/matches/0/author")
                .asText());
        assertEquals("GOE_AGX", res.at("/matches/0/docSigle").asText());
        assertEquals("GOE", res.at("/matches/0/corpusSigle").asText());
        assertEquals("Religion und Christentum", res.at("/matches/0/subTitle")
                .asText());
        assertEquals("München", res.at("/matches/0/pubPlace").asText());
        assertEquals(
                "base/s=spans cnx/c=spans cnx/l=tokens cnx/m=tokens cnx/p=tokens cnx/s=spans cnx/syn=tokens corenlp/c=spans corenlp/ne=tokens corenlp/p=tokens corenlp/s=spans glemm/l=tokens mate/l=tokens mate/m=tokens mate/p=tokens opennlp/p=tokens opennlp/s=spans tt/l=tokens tt/p=tokens tt/s=spans xip/c=spans xip/l=tokens xip/p=tokens xip/s=spans",
                res.at("/matches/0/layerInfos").asText());
        assertTrue(res.at("/matches/0/textType").isMissingNode());
        assertEquals("match-GOE_AGX.00002-p7-8", res.at("/matches/0/matchID")
                .asText());


        // All fields
        jsonString = getString(getClass().getResource(
                "/queries/metas/fields_all.jsonld").getFile());

        ks = new Krill(jsonString);
        kr = ks.apply(ki);
        mapper = new ObjectMapper();
        res = mapper.readTree(kr.toJsonString());
        assertEquals("Verlag C. H. Beck", res.at("/matches/0/publisher")
                .asText());
        assertEquals("Aphorismus", res.at("/matches/0/textType").asText());
        assertEquals("Aphorismen", res.at("/matches/0/textTypeRef").asText());
        assertEquals(
                "Goethe, Johann Wolfgang von: Maximen und Reflexionen. Religion und Christentum, [Aphorismen], (Erstveröffentlichung: Stuttgart ; Tübingen, 1827-1842), In: Goethe, Johann Wolfgang von: Goethes Werke, Bd. 12, Schriften zur Kunst. Schriften zur Literatur. Maximen und Reflexionen, Hrsg.: Trunz, Erich. München: Verlag C. H. Beck, 1982, S. 372-377",
                res.at("/matches/0/reference").asText());
        assertEquals("de", res.at("/matches/0/language").asText());
        assertEquals("opennlp#tokens", res.at("/matches/0/tokenSource")
                .asText());
        assertEquals(
                "base base/paragraphs base/sentences connexor connexor/morpho connexor/phrase connexor/sentences connexor/syntax corenlp corenlp/constituency corenlp/morpho corenlp/namedentities corenlp/sentences glemm glemm/morpho mate mate/morpho opennlp opennlp/morpho opennlp/sentences treetagger treetagger/morpho treetagger/sentences xip xip/constituency xip/morpho xip/sentences",
                res.at("/matches/0/foundries").asText());
        assertEquals("Goethe-Korpus", res.at("/matches/0/corpusTitle").asText());
        assertEquals("QAO-NC", res.at("/matches/0/license").asText());
        assertEquals("Goethe: Maximen und Reflexionen, (1827-1842)",
                res.at("/matches/0/docTitle").asText());
        assertEquals("1827", res.at("/matches/0/creationDate").asText());
        assertEquals("372-377", res.at("/matches/0/pages").asText());
        assertEquals("match-GOE_AGX.00002-p7-8", res.at("/matches/0/matchID")
                .asText());
    };


    @Test
    public void searchMetaContext () throws IOException {

        // All fields
        String jsonString = getString(getClass().getResource(
                "/queries/metas/context_paragraph.jsonld").getFile());

        Krill ks = new Krill(jsonString);
        assertTrue(ks.getMeta().getContext().isSpanDefined());
        assertEquals("base/p", ks.getMeta().getContext().getSpanContext());
    };
};
