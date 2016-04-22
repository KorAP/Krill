package de.ids_mannheim.korap.search;

import static de.ids_mannheim.korap.TestSimple.getString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.HashMap;

import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.KrillCollection;
import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.KrillMeta;
import de.ids_mannheim.korap.collection.CollectionBuilder;
import de.ids_mannheim.korap.index.FieldDocument;
import de.ids_mannheim.korap.query.QueryBuilder;
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.response.SearchContext;

@RunWith(JUnit4.class)
public class TestKrill {
    @Test
    public void searchCount () {
        Krill k = new Krill(new QueryBuilder("field1").seg("a").with("b"));

        KrillMeta meta = k.getMeta();

        // Count:
        meta.setCount(30);
        assertEquals(meta.getCount(), 30);
        meta.setCount(20);
        assertEquals(meta.getCount(), 20);
        meta.setCount(-50);
        assertEquals(meta.getCount(), 20);
        meta.setCount(500);
        assertEquals(meta.getCount(), meta.getCountMax());
    };


    @Test
    public void searchStartIndex () {
        Krill k = new Krill(new QueryBuilder("field1").seg("a").with("b"));

        KrillMeta meta = k.getMeta();

        // startIndex
        meta.setStartIndex(5);
        assertEquals(meta.getStartIndex(), 5);
        meta.setStartIndex(1);
        assertEquals(meta.getStartIndex(), 1);
        meta.setStartIndex(0);
        assertEquals(meta.getStartIndex(), 0);
        meta.setStartIndex(70);
        assertEquals(meta.getStartIndex(), 70);
        meta.setStartIndex(-5);
        assertEquals(meta.getStartIndex(), 0);
    };


    @Test
    public void searchQuery () {
        Krill ks = new Krill(new QueryBuilder("field1").seg("a").with("b"));
        // query
        assertEquals(ks.getSpanQuery().toString(),
                "spanSegment(field1:a, field1:b)");
    };


    @Test
    public void searchIndex () throws IOException {
        // Construct index
        KrillIndex ki = new KrillIndex();
        // Indexing test files
        for (String i : new String[] { "00001", "00002", "00003", "00004",
                "00005", "00006", "02439" }) {
            ki.addDoc(
                    getClass().getResourceAsStream("/wiki/" + i + ".json.gz"),
                    true);
        };
        ki.commit();

        Krill ks = new Krill(new QueryBuilder("tokens").seg("s:Buchstaben"));

        CollectionBuilder cb = new CollectionBuilder();

        ks.getCollection().fromBuilder(cb.term("textClass", "reisen"));

        KrillMeta meta = ks.getMeta();
        meta.setCount(3);
        meta.setStartIndex(5);
        meta.getContext().left.setLength(1);
        meta.getContext().right.setLength(1);

        Result kr = ks.apply(ki);
        assertEquals(kr.getTotalResults(), 6);
        assertEquals(kr.getMatch(0).getSnippetBrackets(),
                "... dem [Buchstaben] A ...");

        JsonNode res = ks.toJsonNode();
        assertEquals(3, res.at("/meta/count").asInt());
        assertEquals(5, res.at("/meta/startIndex").asInt());
        assertEquals("token", res.at("/meta/context/left/0").asText());
        assertEquals(1, res.at("/meta/context/left/1").asInt());
        assertEquals("token", res.at("/meta/context/right/0").asText());
        assertEquals(1, res.at("/meta/context/right/1").asInt());
    };


    @Test
    public void searchJSON () throws IOException {
        // Construct index
        KrillIndex ki = new KrillIndex();
        // Indexing test files
        for (String i : new String[] { "00001", "00002", "00003", "00004",
                "00005", "00006", "02439" }) {
            ki.addDoc(
                    getClass().getResourceAsStream("/wiki/" + i + ".json.gz"),
                    true);
        };
        ki.commit();

        String json = getString(getClass().getResource(
                "/queries/metaquery3.jsonld").getFile());

        Krill ks = new Krill(json);
        Result kr = ks.apply(ki);
        assertEquals(kr.getTotalResults(), 66);
        assertEquals(5, kr.getItemsPerPage());
        assertEquals(5, kr.getStartIndex());
        assertEquals("... a: A ist [der klangreichste] der V ...",
                kr.getMatch(0).getSnippetBrackets());
    };


    @Test
    public void searchJSON2 () throws IOException {
        // Construct index
        KrillIndex ki = new KrillIndex();
        // Indexing test files
        for (String i : new String[] { "00001", "00002", "00003", "00004",
                "00005", "00006", "02439", "00012-fakemeta", "00030-fakemeta",
        /*
          "02035-substring",
          "05663-unbalanced",
          "07452-deep"
        */
        }) {
            ki.addDoc(
                    getClass().getResourceAsStream("/wiki/" + i + ".json.gz"),
                    true);
        };
        ki.commit();

        String json = getString(getClass().getResource(
                "/queries/metaquery4.jsonld").getFile());

        Krill ks = new Krill(json);
        Result kr = ks.apply(ki);

        assertEquals(kr.getTotalResults(), 1);

        ks = new Krill(json);
        // Ignore the collection part of the query!
        ks.setCollection(new KrillCollection());
        kr = ks.apply(ki);

        assertEquals(kr.getTotalResults(), 5);

        json = getString(getClass().getResource("/queries/metaquery5.jsonld")
                .getFile());

        ks = new Krill(json);
        kr = ks.apply(ki);
        assertEquals(kr.getTotalResults(), 1);

        json = getString(getClass().getResource("/queries/metaquery6.jsonld")
                .getFile());
        ks = new Krill(json);
        kr = ks.apply(ki);
        assertEquals(kr.getTotalResults(), 1);
    };


    // Todo: There SHOULD be a failure here, but Koral currently creates empty collections
    @Test
    public void queryJSONapiTest1 () {
        Krill test = new Krill(
                "{\"@context\":\"http://korap.ids-mannheim.de/ns/koral/0.3/context.jsonld\",\"errors\":[],\"warnings\":[],\"messages\":[],\"collection\":{},\"query\":{\"@type\":\"koral:token\",\"wrap\":{\"@type\":\"koral:term\",\"layer\":\"orth\",\"key\":\"Baum\",\"match\":\"match:eq\"}},\"meta\":{}}");
        assertFalse(test.hasErrors());
    };


    @Test
    public void searchJSONFailure () throws IOException {
        // Construct index
        KrillIndex ki = new KrillIndex();
        // Indexing test files
        for (String i : new String[] { "00001", "00002", "00003", "00004",
                "00005", "00006", "02439" }) {
            ki.addDoc(
                    getClass().getResourceAsStream("/wiki/" + i + ".json.gz"),
                    true);
        };
        ki.commit();
        Result kr = new Krill("{ query").apply(ki);
        assertEquals(kr.getTotalResults(), 0);
        assertEquals(kr.getError(0).getMessage(), "Unable to parse JSON");
    };


    @Test
    public void searchJSONindexboundary () throws IOException {
        // Construct index
        KrillIndex ki = new KrillIndex();
        // Indexing test files
        for (String i : new String[] { "00001", "00002", "00003", "00004",
                "00005", "00006", "02439" }) {
            ki.addDoc(
                    getClass().getResourceAsStream("/wiki/" + i + ".json.gz"),
                    true);
        };
        ki.commit();

        String json = getString(getClass().getResource(
                "/queries/bsp-fail1.jsonld").getFile());

        Result kr = new Krill(json).apply(ki);
        assertEquals(0, kr.getStartIndex());
        assertEquals(kr.getTotalResults(), 0);
        assertEquals(25, kr.getItemsPerPage());
    };


    @Test
    public void searchJSONindexboundary2 () throws IOException {
        // Construct index
        KrillIndex ki = new KrillIndex();
        // Indexing test files
        for (String i : new String[] { "00001", "00002", "00003", "00004",
                "00005", "00006", "02439" }) {
            ki.addDoc(
                    getClass().getResourceAsStream("/wiki/" + i + ".json.gz"),
                    true);
        };
        ki.commit();

        String json = getString(getClass().getResource(
                "/queries/bsp-fail2.jsonld").getFile());

        Result kr = new Krill(json).apply(ki);
        assertEquals(50, kr.getItemsPerPage());
        assertEquals(49950, kr.getStartIndex());
        assertEquals(kr.getTotalResults(), 0);
    };


    /*
     * Queries should be mirrored correctly for debugging reasons.
     */
    @Test
    public void queryJSONmirrorTestBug () throws IOException {
        // Construct index
        KrillIndex ki = new KrillIndex();
        String json = getString(getClass().getResource(
                "/queries/bugs/failing_mirror.jsonld").getFile());
        Krill ks = new Krill(json);
        Result kr = ks.apply(ki);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode res = mapper.readTree(kr.toJsonString());

        assertEquals("Unable to parse JSON", res.at("/errors/0/1").asText());

        json = getString(getClass().getResource(
                "/queries/bugs/failing_mirror_2.jsonld").getFile());
        ks = new Krill(json);
        kr = ks.apply(ki);

        res = mapper.readTree(kr.toJsonString());

        assertEquals(23, res.at("/meta/count").asInt());
        assertEquals(25, res.at("/meta/itemsPerPage").asInt());
        assertEquals("base/s:p", res.at("/meta/context").asText());
        assertFalse(res.at("/query").isMissingNode());
        assertTrue(res.at("/query/@type").isMissingNode());
        assertTrue(res.at("/collection/@type").isMissingNode());
    };



    @Test
    public void searchJSONcontext () throws IOException {
        // Construct index
        KrillIndex ki = new KrillIndex();
        // Indexing test files
        for (String i : new String[] { "00001", "00002", "00003", "00004",
                "00005", "00006", "02439" }) {
            ki.addDoc(
                    getClass().getResourceAsStream("/wiki/" + i + ".json.gz"),
                    true);
        };
        ki.commit();

        String json = getString(getClass().getResource(
                "/queries/bsp-context.jsonld").getFile());

        Krill ks = new Krill(json);
        Result kr = ks.apply(ki);
        assertEquals(kr.getTotalResults(), 10);
        assertEquals("A bzw. a ist der erste Buchstabe des"
                + " lateinischen [Alphabets] und ein Vokal."
                + " Der Buchstabe A hat in deutschen Texten"
                + " eine durchschnittliche Häufigkeit  ...", kr.getMatch(0)
                .getSnippetBrackets());

        ks.getMeta().setCount(5);
        ks.getMeta().setStartPage(2);
        kr = ks.apply(ki);
        assertEquals(kr.getTotalResults(), 10);
        assertEquals(5, kr.getStartIndex());
        assertEquals(5, kr.getItemsPerPage());

        json = getString(getClass()
                .getResource("/queries/bsp-context-2.jsonld").getFile());

        kr = new Krill(json).apply(ki);

        assertEquals(kr.getTotalResults(), -1);
        assertEquals("... lls seit den Griechen beibehalten worden."
                + " 3. Bedeutungen in der Biologie steht A für"
                + " das Nukleosid Adenosin steht A die Base"
                + " Adenin steht A für die Aminosäure Alanin"
                + " in der Informatik steht a für den dezimalen"
                + " [Wert] 97 sowohl im ASCII- als auch im"
                + " Unicode-Zeichensatz steht A für den dezimalen"
                + " Wert 65 sowohl im ASCII- als auch im"
                + " Unicode-Zeichensatz als Kfz-Kennzeichen"
                + " steht A in Deutschland für Augsburg."
                + " in Österreich auf ...", kr.getMatch(0).getSnippetBrackets());
    };


    @Test
    public void searchJSONstartPage () throws IOException {
        // Construct index
        KrillIndex ki = new KrillIndex();
        // Indexing test files
        for (String i : new String[] { "00001", "00002", "00003", "00004",
                "00005", "00006", "02439" }) {
            ki.addDoc(
                    getClass().getResourceAsStream("/wiki/" + i + ".json.gz"),
                    true);
        };
        ki.commit();

        String json = getString(getClass().getResource(
                "/queries/bsp-paging.jsonld").getFile());

        Krill ks = new Krill(json);
        Result kr = ks.apply(ki);
        assertEquals(kr.getTotalResults(), 10);
        assertEquals(5, kr.getStartIndex());
        assertEquals(5, kr.getItemsPerPage());

        json = getString(getClass().getResource("/queries/bsp-cutoff.jsonld")
                .getFile());
        ks = ks = new Krill(json);
        kr = ks.apply(ki);
        assertEquals(kr.getTotalResults(), -1);
        assertEquals(2, kr.getStartIndex());
        assertEquals(2, kr.getItemsPerPage());

        json = getString(getClass().getResource("/queries/metaquery9.jsonld")
                .getFile());
        KrillCollection kc = new KrillCollection(json);
        kc.setIndex(ki);
        assertEquals(7, kc.numberOf("documents"));
    };


    @Test
    public void searchJSONitemsPerResource () throws IOException {
        // Construct index
        KrillIndex ki = new KrillIndex();
        // Indexing test files
        for (String i : new String[] { "00001", "00002", "00003", "00004",
                "00005", "00006", "02439" }) {
            ki.addDoc(
                    getClass().getResourceAsStream("/wiki/" + i + ".json.gz"),
                    true);
        };
        ki.commit();
        String json = getString(getClass().getResource(
                "/queries/bsp-itemsPerResource.jsonld").getFile());

        Krill ks = new Krill(json);
        Result kr = ks.apply(ki);
        assertEquals(kr.getTotalResults(), 10);
        assertEquals(0, kr.getStartIndex());
        assertEquals(20, kr.getItemsPerPage());

        assertEquals("WPD_AAA.00001", kr.getMatch(0).getDocID());
        assertEquals("WPD_AAA.00001", kr.getMatch(1).getDocID());
        assertEquals("WPD_AAA.00001", kr.getMatch(6).getDocID());
        assertEquals("WPD_AAA.00002", kr.getMatch(7).getDocID());
        assertEquals("WPD_AAA.00002", kr.getMatch(8).getDocID());
        assertEquals("WPD_AAA.00004", kr.getMatch(9).getDocID());

        ks = new Krill(json);
        ks.getMeta().setItemsPerResource(1);

        kr = ks.apply(ki);

        assertEquals("WPD_AAA.00001", kr.getMatch(0).getDocID());
        assertEquals("WPD_AAA.00002", kr.getMatch(1).getDocID());
        assertEquals("WPD_AAA.00004", kr.getMatch(2).getDocID());

        assertEquals(kr.getTotalResults(), 3);
        assertEquals(0, kr.getStartIndex());
        assertEquals(20, kr.getItemsPerPage());

        ks = new Krill(json);
        ks.getMeta().setItemsPerResource(2);

        kr = ks.apply(ki);

        assertEquals("WPD_AAA.00001", kr.getMatch(0).getDocID());
        assertEquals("WPD_AAA.00001", kr.getMatch(1).getDocID());
        assertEquals("WPD_AAA.00002", kr.getMatch(2).getDocID());
        assertEquals("WPD_AAA.00002", kr.getMatch(3).getDocID());
        assertEquals("WPD_AAA.00004", kr.getMatch(4).getDocID());

        assertEquals(kr.getTotalResults(), 5);
        assertEquals(0, kr.getStartIndex());
        assertEquals(20, kr.getItemsPerPage());

        ks = new Krill(json);
        KrillMeta meta = ks.getMeta();
        meta.setItemsPerResource(1);
        meta.setStartIndex(1);
        meta.setCount(1);

        kr = ks.apply(ki);

        assertEquals("WPD_AAA.00002", kr.getMatch(0).getDocID());

        assertEquals(kr.getTotalResults(), 3);
        assertEquals(1, kr.getStartIndex());
        assertEquals(1, kr.getItemsPerPage());

        assertEquals((short) 1, kr.getItemsPerResource());
    };


    @Test
    public void searchJSONitemsPerResourceServer () throws IOException {
        /*
         * This test is a server-only implementation of
         * TestResource#testCollection
         */
        // Construct index
        KrillIndex ki = new KrillIndex();
        // Indexing test files
        int uid = 1;
        for (String i : new String[] { "00001", "00002", "00003", "00004",
                "00005", "00006", "02439" }) {
            ki.addDoc(uid++,
                    getClass().getResourceAsStream("/wiki/" + i + ".json.gz"),
                    true);
        };
        ki.commit();

        String json = getString(getClass().getResource(
                "/queries/bsp-uid-example.jsonld").getFile());

        Krill ks = new Krill(json);
        ks.getMeta().setItemsPerResource(1);

        KrillCollection kc = new KrillCollection();
        kc.filterUIDs(new String[] { "1", "4" });
        kc.setIndex(ki);
        ks.setCollection(kc);

        Result kr = ks.apply(ki);

        assertEquals(kr.getTotalResults(), 2);
        assertEquals(0, kr.getStartIndex());
        assertEquals(25, kr.getItemsPerPage());
    };


    @Test
    public void searchJSONnewJSON () throws IOException {
        // Construct index
        KrillIndex ki = new KrillIndex();
        // Indexing test files
        FieldDocument fd = ki.addDoc(1,
                getClass().getResourceAsStream("/goe/AGA-03828.json.gz"), true);
        ki.commit();

        assertEquals(fd.getUID(), 1);
        assertEquals(fd.getTextSigle(), "GOE_AGA.03828");
        assertEquals(fd.getDocSigle(), "GOE_AGA");
        assertEquals(fd.getCorpusSigle(), "GOE");
        assertEquals(fd.getTitle(), "Autobiographische Einzelheiten");
        assertNull(fd.getSubTitle());
        assertEquals(fd.getTextType(), "Autobiographie");
        assertNull(fd.getTextTypeArt());
        assertNull(fd.getTextTypeRef());
        assertNull(fd.getTextColumn());
        assertNull(fd.getTextDomain());
        assertEquals(fd.getPages(), "529-547");
        assertEquals(fd.getLicense(), "QAO-NC");
        assertEquals(fd.getCreationDate().toString(), "18200000");
        assertEquals(fd.getPubDate().toString(), "19820000");
        assertEquals(fd.getAuthor(), "Goethe, Johann Wolfgang von");
        assertNull(fd.getTextClass());
        assertEquals(fd.getLanguage(), "de");
        assertEquals(fd.getPubPlace(), "München");
        assertEquals(fd.getReference(), "Goethe, Johann Wolfgang von:"
                + " Autobiographische Einzelheiten,"
                + " (Geschrieben bis 1832), In: Goethe,"
                + " Johann Wolfgang von: Goethes Werke,"
                + " Bd. 10, Autobiographische Schriften"
                + " II, Hrsg.: Trunz, Erich. München: "
                + "Verlag C. H. Beck, 1982, S. 529-547");
        assertEquals(fd.getPublisher(), "Verlag C. H. Beck");
        assertNull(fd.getEditor());
        assertNull(fd.getFileEditionStatement());
        assertNull(fd.getBiblEditionStatement());
        assertNull(fd.getKeywords());

        assertEquals(fd.getTokenSource(), "opennlp#tokens");
        assertEquals(fd.getFoundries(),
                "base base/paragraphs base/sentences corenlp "
                        + "corenlp/constituency corenlp/morpho "
                        + "corenlp/namedentities corenlp/sentences "
                        + "glemm glemm/morpho mate mate/morpho"
                        + " opennlp opennlp/morpho opennlp/sentences"
                        + " treetagger treetagger/morpho "
                        + "treetagger/sentences");
        assertEquals(fd.getLayerInfos(),
                "base/s=spans corenlp/c=spans corenlp/ne=tokens"
                        + " corenlp/p=tokens corenlp/s=spans glemm/l=tokens"
                        + " mate/l=tokens mate/m=tokens mate/p=tokens"
                        + " opennlp/p=tokens opennlp/s=spans tt/l=tokens"
                        + " tt/p=tokens tt/s=spans");

        assertEquals(fd.getCorpusTitle(), "Goethes Werke");
        assertNull(fd.getCorpusSubTitle());
        assertEquals(fd.getCorpusAuthor(), "Goethe, Johann Wolfgang von");
        assertEquals(fd.getCorpusEditor(), "Trunz, Erich");
        assertEquals(fd.getDocTitle(),
                "Goethe: Autobiographische Schriften II, (1817-1825, 1832)");
        assertNull(fd.getDocSubTitle());
        assertNull(fd.getDocEditor());
        assertNull(fd.getDocAuthor());

        Krill ks = new Krill(new QueryBuilder("tokens").seg("mate/m:case:nom")
                .with("mate/m:number:pl"));
        Result kr = ks.apply(ki);

        assertEquals(kr.getTotalResults(), 148);
        assertEquals(0, kr.getStartIndex());
        assertEquals(25, kr.getItemsPerPage());
    };


    @Test
    public void searchJSONnewJSON2 () throws IOException {
        // Construct index
        KrillIndex ki = new KrillIndex();
        // Indexing test files
        FieldDocument fd = ki.addDoc(1,
                getClass().getResourceAsStream("/bzk/D59-00089.json.gz"), true);
        ki.commit();

        assertEquals(fd.getUID(), 1);
        assertEquals(fd.getTextSigle(), "BZK_D59.00089");
        assertEquals(fd.getDocSigle(), "BZK_D59");
        assertEquals(fd.getCorpusSigle(), "BZK");
        assertEquals(fd.getTitle(), "Saragat-Partei zerfällt");
        assertEquals(fd.getPubDate().toString(), "19590219");

        assertNull(fd.getSubTitle());
        assertNull(fd.getAuthor());
        assertNull(fd.getEditor());
        assertEquals(fd.getPubPlace(), "Berlin");
        assertNull(fd.getPublisher());
        assertEquals(fd.getTextType(), "Zeitung: Tageszeitung");
        assertNull(fd.getTextTypeArt());
        assertEquals(fd.getTextTypeRef(), "Tageszeitung");
        assertEquals(fd.getTextDomain(), "Politik");
        assertEquals(fd.getCreationDate().toString(), "19590219");
        assertEquals(fd.getLicense(), "ACA-NC-LC");
        assertEquals(fd.getTextColumn(), "POLITIK");
        assertNull(fd.getPages());
        assertEquals(fd.getTextClass(), "politik ausland");
        assertNull(fd.getFileEditionStatement());
        assertNull(fd.getBiblEditionStatement());

        assertEquals(fd.getLanguage(), "de");
        assertEquals(fd.getReference(),
                "Neues Deutschland, [Tageszeitung], 19.02.1959, Jg. 14,"
                        + " Berliner Ausgabe, S. 7. - Sachgebiet: Politik, "
                        + "Originalressort: POLITIK; Saragat-Partei zerfällt");
        assertNull(fd.getPublisher());
        assertNull(fd.getKeywords());

        assertEquals(fd.getTokenSource(), "opennlp#tokens");

        assertEquals(
                fd.getFoundries(),
                "base base/paragraphs base/sentences corenlp "
                        + "corenlp/constituency corenlp/morpho corenlp/namedentities"
                        + " corenlp/sentences glemm glemm/morpho mate mate/morpho"
                        + " opennlp opennlp/morpho opennlp/sentences treetagger"
                        + " treetagger/morpho treetagger/sentences");

        assertEquals(fd.getLayerInfos(),
                "base/s=spans corenlp/c=spans corenlp/ne=tokens"
                        + " corenlp/p=tokens corenlp/s=spans glemm/l=tokens"
                        + " mate/l=tokens mate/m=tokens mate/p=tokens"
                        + " opennlp/p=tokens opennlp/s=spans tt/l=tokens"
                        + " tt/p=tokens tt/s=spans");

        assertEquals(fd.getCorpusTitle(), "Bonner Zeitungskorpus");
        assertNull(fd.getCorpusSubTitle());
        assertNull(fd.getCorpusAuthor());
        assertNull(fd.getCorpusEditor());

        assertEquals(fd.getDocTitle(), "Neues Deutschland");
        assertEquals(fd.getDocSubTitle(),
                "Organ des Zentralkomitees der Sozialistischen "
                        + "Einheitspartei Deutschlands");
        assertNull(fd.getDocEditor());
        assertNull(fd.getDocAuthor());

        Krill ks = new Krill(new QueryBuilder("tokens").seg("mate/m:case:nom")
                .with("mate/m:number:sg"));
        Result kr = ks.apply(ki);

        assertEquals(kr.getTotalResults(), 6);
        assertEquals(0, kr.getStartIndex());
        assertEquals(25, kr.getItemsPerPage());
    };


    @Test
    public void searchJSONcosmasBoundaryBug () throws IOException {
        // Construct index
        KrillIndex ki = new KrillIndex();
        // Indexing test files
        FieldDocument fd = ki.addDoc(1,
                getClass().getResourceAsStream("/bzk/D59-00089.json.gz"), true);
        ki.commit();

        String json = getString(getClass().getResource(
                "/queries/bugs/cosmas_boundary.jsonld").getFile());

        QueryBuilder kq = new QueryBuilder("tokens");
        Krill ks = new Krill(kq.focus(1,
                kq.contains(kq.tag("base/s:s"), kq._(1, kq.seg("s:Leben")))));

        Result kr = ks.apply(ki);
        assertEquals(kr.getSerialQuery(),
                "focus(1: spanContain(<tokens:base/s:s />, {1: tokens:s:Leben}))");
        assertEquals(40, kr.getMatch(0).getStartPos());
        assertEquals(41, kr.getMatch(0).getEndPos());

        assertEquals(
                kr.getMatch(0).getSnippetBrackets(),
                "... Initiative\" eine neue politische Gruppierung ins "
                        + "[{1:Leben}] gerufen hatten. Pressemeldungen zufolge haben sich ...");

        // Try with high class - don't highlight
        ks = new Krill(kq.focus(129,
                kq.contains(kq.tag("base/s:s"), kq._(129, kq.seg("s:Leben")))));

        kr = ks.apply(ki);
        assertEquals(kr.getSerialQuery(),
                "focus(129: spanContain(<tokens:base/s:s />, {129: tokens:s:Leben}))");
        assertEquals(
                kr.getMatch(0).getSnippetBrackets(),
                "... Initiative\" eine neue politische Gruppierung ins "
                        + "[Leben] gerufen hatten. Pressemeldungen zufolge haben sich ...");

        ks = new Krill(json);
        kr = ks.apply(ki);
        assertEquals(
                kr.getSerialQuery(),
                "focus(129: spanElementDistance({129: tokens:s:Namen}, "
                        + "{129: tokens:s:Leben}, [(base/s:s[0:1], notOrdered, notExcluded)]))");
        assertEquals(
                kr.getMatch(0).getSnippetBrackets(),
                "... ihren Austritt erklärt und unter dem [Namen \"Einheitsbewegung "
                        + "der sozialistischen Initiative\" eine neue politische Gruppierung "
                        + "ins Leben] gerufen hatten. Pressemeldungen zufolge haben sich ...");
        assertEquals(kr.getTotalResults(), 1);
        assertEquals(0, kr.getStartIndex());
    };


    @Test
    public void searchJSONmultipleClassesBug () throws IOException {
        // Construct index
        KrillIndex ki = new KrillIndex();
        // Indexing test files
        ki.addDoc(1, getClass().getResourceAsStream("/bzk/D59-00089.json.gz"),
                true);
        ki.addDoc(2, getClass().getResourceAsStream("/bzk/D59-00089.json.gz"),
                true);

        ki.commit();

        String json = getString(getClass().getResource(
                "/queries/bugs/multiple_classes.jsonld").getFile());

        Krill ks = new Krill(json);
        Result kr = ks.apply(ki);
        assertEquals(kr.getSerialQuery(),
                "{4: spanNext({1: spanNext({2: tokens:s:ins}, "
                        + "{3: tokens:s:Leben})}, tokens:s:gerufen)}");
        assertEquals(
                kr.getMatch(0).getSnippetBrackets(),
                "... sozialistischen Initiative\" eine neue politische"
                        + " Gruppierung [{4:{1:{2:ins} {3:Leben}} gerufen}] hatten. "
                        + "Pressemeldungen zufolge haben sich in ...");
        assertEquals(kr.getTotalResults(), 2);
        assertEquals(0, kr.getStartIndex());
    };


    @Test
    public void searchJSONmultipleClassesBugTokenList () throws IOException {
        // Construct index
        KrillIndex ki = new KrillIndex();
        // Indexing test files
        ki.addDoc(1, getClass().getResourceAsStream("/goe/AGA-03828.json.gz"),
                true);
        ki.addDoc(2, getClass().getResourceAsStream("/bzk/D59-00089.json.gz"),
                true);

        ki.commit();

        String json = getString(getClass().getResource(
                "/queries/bugs/multiple_classes.jsonld").getFile());

        Krill ks = new Krill(json);
        Result kr = ks.apply(ki);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode res = mapper.readTree(kr.toTokenListJsonString());

        assertEquals(1, res.at("/meta/totalResults").asInt());
        assertEquals("{4: spanNext({1: spanNext({2: tokens:s:ins}, "
                + "{3: tokens:s:Leben})}, tokens:s:gerufen)}",
                res.at("/meta/serialQuery").asText());
        assertEquals(0, res.at("/meta/startIndex").asInt());
        assertEquals(25, res.at("/meta/itemsPerPage").asInt());

        assertEquals("BZK_D59.00089", res.at("/matches/0/textSigle").asText());
        assertEquals(328, res.at("/matches/0/tokens/0/0").asInt());
        assertEquals(331, res.at("/matches/0/tokens/0/1").asInt());
        assertEquals(332, res.at("/matches/0/tokens/1/0").asInt());
        assertEquals(337, res.at("/matches/0/tokens/1/1").asInt());
        assertEquals(338, res.at("/matches/0/tokens/2/0").asInt());
        assertEquals(345, res.at("/matches/0/tokens/2/1").asInt());
    };


    @Test
    public void searchJSONmultitermRewriteBug () throws IOException {
        // Construct index
        KrillIndex ki = new KrillIndex();

        assertEquals(ki.numberOf("documents"), 0);

        // Indexing test files
        FieldDocument fd = ki.addDoc(1,
                getClass().getResourceAsStream("/bzk/D59-00089.json.gz"), true);
        ki.commit();

        assertEquals(ki.numberOf("documents"), 1);
        assertEquals("BZK", fd.getCorpusSigle());

        // [tt/p="A.*"]{0,3}[tt/p="N.*"]
        String json = getString(getClass().getResource(
                "/queries/bugs/multiterm_rewrite.jsonld").getFile());

        Krill ks = new Krill(json);
        KrillCollection kc = ks.getCollection();

        // No index was set
        assertEquals(-1, kc.numberOf("documents"));
        kc.setIndex(ki);

        // Index was set but vc restricted to WPD
        assertEquals(0, kc.numberOf("documents"));

        /*
        kc.extend(new CollectionBuilder().or("corpusSigle", "BZK"));
        */
        CollectionBuilder cb = new CollectionBuilder();
        kc.fromBuilder(cb.orGroup().with(kc.getBuilder())
                .with(cb.term("corpusSigle", "BZK")));

        ks.setCollection(kc);
        assertEquals(1, kc.numberOf("documents"));

        Result kr = ks.apply(ki);

        assertEquals(kr.getSerialQuery(),
                "spanOr([SpanMultiTermQueryWrapper(tokens:/tt/p:N.*/), "
                        + "spanNext(spanRepetition(SpanMultiTermQueryWrapper"
                        + "(tokens:/tt/p:A.*/){1,3}), "
                        + "SpanMultiTermQueryWrapper(tokens:/tt/p:N.*/))])");

        assertEquals(kr.getTotalResults(), 58);
        assertEquals(0, kr.getStartIndex());

        assertEquals(kr.getMatch(0).getSnippetBrackets(),
                "[Saragat-Partei] zerfällt Rom (ADN) die von dem ...");
        assertEquals(kr.getMatch(1).getSnippetBrackets(),
                "[Saragat-Partei] zerfällt Rom (ADN) die von dem ...");
        assertEquals(kr.getMatch(2).getSnippetBrackets(),
                "Saragat-Partei zerfällt [Rom] (ADN) "
                        + "die von dem Rechtssozialisten Saragat ...");
        assertEquals(kr.getMatch(3).getSnippetBrackets(),
                "Saragat-Partei zerfällt Rom ([ADN]) "
                        + "die von dem Rechtssozialisten Saragat geführte ...");
        assertEquals(
                kr.getMatch(23).getSnippetBrackets(),
                "... dem Namen \"Einheitsbewegung der sozialistischen "
                        + "Initiative\" [eine neue politische Gruppierung] "
                        + "ins Leben gerufen hatten. Pressemeldungen zufolge ...");
    };


    @Test
    public void searchJSONtokenDistanceSpanBug () throws IOException {
        // Construct index
        KrillIndex ki = new KrillIndex();
        ki.addDoc(1, getClass().getResourceAsStream("/goe/AGX-00002.json"),
                false);
        ki.addDoc(2, getClass().getResourceAsStream("/bzk/D59-00089.json.gz"),
                true);
        ki.commit();

        // ({1:Sonne []* Erde} | {2: Erde []* Sonne})
        String json = getString(getClass().getResource(
                "/queries/bugs/tokendistancespan_bug.jsonld").getFile());

        Krill ks = new Krill(json);
        Result kr = ks.apply(ki);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode res = mapper.readTree(kr.toJsonString());
        assertTrue(res.at("/errors").isMissingNode());
    };


    @Test
    public void searchJSONCollection () throws IOException {
        // Construct index
        KrillIndex ki = new KrillIndex();
        // Indexing test files
        for (String i : new String[] { "00001", "00002", "00003", "00004",
                "00005", "00006", "02439" }) {
            ki.addDoc(
                    getClass().getResourceAsStream("/wiki/" + i + ".json.gz"),
                    true);
        };
        ki.commit();
        String json = getString(getClass().getResource(
                "/queries/metaquery8-nocollection.jsonld").getFile());

        Krill ks = new Krill(json);
        Result kr = ks.apply(ki);
        assertEquals(kr.getTotalResults(), 276);
        assertEquals(0, kr.getStartIndex());
        assertEquals(10, kr.getItemsPerPage());

        json = getString(getClass().getResource("/queries/metaquery8.jsonld")
                .getFile());

        ks = new Krill(json);
        kr = ks.apply(ki);

        assertEquals(kr.getTotalResults(), 147);
        assertEquals("WPD_AAA.00001", kr.getMatch(0).getDocID());
        assertEquals(0, kr.getStartIndex());
        assertEquals(10, kr.getItemsPerPage());

        json = getString(getClass().getResource(
                "/queries/metaquery8-filtered.jsonld").getFile());

        ks = new Krill(json);
        kr = ks.apply(ki);

        assertEquals(kr.getTotalResults(), 28);
        assertEquals("WPD_AAA.00002", kr.getMatch(0).getDocID());
        assertEquals(0, kr.getStartIndex());
        assertEquals(10, kr.getItemsPerPage());

        json = getString(getClass().getResource(
                "/queries/metaquery8-filtered-further.jsonld").getFile());

        ks = new Krill(json);
        kr = ks.apply(ki);

        assertEquals(kr.getTotalResults(), 0);
        assertEquals(0, kr.getStartIndex());
        assertEquals(10, kr.getItemsPerPage());


        json = getString(getClass().getResource(
                "/queries/metaquery8-filtered-nested.jsonld").getFile());

        ks = new Krill(json);
        kr = ks.apply(ki);

        /*
        assertEquals("filter with QueryWrapperFilter("
                + "+(ID:WPD_AAA.00003 (+tokens:s:die"
                + " +tokens:s:Schriftzeichen)))",
                ks.getCollection().getFilter(1).toString());
        */
        assertEquals(
                "AndGroup(OrGroup(ID:WPD_AAA.00001 ID:WPD_AAA.00002) OrGroup(ID:WPD_AAA.00003 AndGroup(tokens:s:die tokens:s:Schriftzeichen)))",
                ks.getCollection().toString());

        assertEquals(kr.getTotalResults(), 119);
        assertEquals(0, kr.getStartIndex());
        assertEquals(10, kr.getItemsPerPage());
    };


    @Test
    public void searchJSONSentenceContext () throws IOException {
        // Construct index
        KrillIndex ki = new KrillIndex();
        // Indexing test files
        for (String i : new String[] { "00001", "00002", "00003", "00004",
                "00005", "00006", "02439" }) {
            ki.addDoc(
                    getClass().getResourceAsStream("/wiki/" + i + ".json.gz"),
                    true);
        };
        ki.commit();

        String json = getString(getClass().getResource(
                "/queries/bsp-context-2.jsonld").getFile());

        Krill ks = new Krill(json);
        ks.getMeta().setCutOff(false);
        SearchContext sc = ks.getMeta().getContext();
        sc.left.setLength((short) 10);
        sc.right.setLength((short) 10);

        Result kr = ks.apply(ki);

        assertEquals(kr.getMatch(1).getSnippetBrackets(),
                "... dezimalen [Wert] 65 sowohl ...");
        assertEquals(kr.getTotalResults(), 3);
        assertEquals(0, kr.getStartIndex());
        assertEquals(25, kr.getItemsPerPage());

        assertFalse(kr.getContext().toJsonNode().toString()
                .equals("\"base/s:s\""));

        json = getString(getClass().getResource(
                "/queries/bsp-context-sentence.jsonld").getFile());

        kr = new Krill(json).apply(ki);
        assertEquals(kr.getContext().toJsonNode().toString(), "\"base/s:s\"");

        assertEquals(kr.getMatch(0).getSnippetBrackets(),
                "steht a für den dezimalen [Wert] 97 sowohl im ASCII-"
                        + " als auch im Unicode-Zeichensatz");
        assertEquals(kr.getMatch(1).getSnippetBrackets(),
                "steht A für den dezimalen [Wert] 65 sowohl im ASCII-"
                        + " als auch im Unicode-Zeichensatz");
        assertEquals(kr.getMatch(2).getSnippetBrackets(),
                "In einem Zahlensystem mit einer Basis größer "
                        + "als 10 steht A oder a häufig für den dezimalen"
                        + " [Wert] 10, siehe auch Hexadezimalsystem.");
    };


    @Test
    public void searchJSONbug () throws IOException {
        // Construct index
        KrillIndex ki = new KrillIndex();
        // Indexing test files
        for (String i : new String[] { "00001", "00002", "00003", "00004",
                "00005", "00006", "02439" }) {
            ki.addDoc(
                    getClass().getResourceAsStream("/wiki/" + i + ".json.gz"),
                    true);
        };
        ki.commit();

        String json = getString(getClass().getResource(
                "/queries/bsp-bug.jsonld").getFile());

        Result kr = new Krill(json).apply(ki);

        assertEquals(kr.getError(0).getMessage(),
                "Operation needs operand list");
    };


    /**
     * This is a breaking test for #179
     */
    @Test
    public void searchJSONexpansionBug () throws IOException {
        // Construct index
        KrillIndex ki = new KrillIndex();
        // Indexing test files
        ki.addDoc(getClass().getResourceAsStream("/wiki/00002.json.gz"), true);
        ki.commit();

        // Expansion bug
        // der alte Digraph Aa durch Å
        String json = getString(getClass().getResource(
                "/queries/bugs/expansion_bug_2.jsonld").getFile());

        Result kr = new Krill(json).apply(ki);
        assertEquals("... Buchstabe des Alphabetes. In Dänemark ist "
                + "[der alte Digraph Aa durch Å] ersetzt worden, "
                + "in Eigennamen und Ortsnamen ...", kr.getMatch(0)
                .getSnippetBrackets());
        assertEquals("WPD_AAA.00002", kr.getMatch(0).getDocID());
        assertEquals(kr.getTotalResults(), 1);


        // TODO: base/s:t needs to be defined!!!
        QueryBuilder qb = new QueryBuilder("tokens");
        kr = new Krill(qb.tag("base/s:t")).apply(ki);
        assertEquals(kr.getTotalResults(), 1);


        // der alte Digraph Aa durch []
        // Works with one document
        json = getString(getClass().getResource(
                "/queries/bugs/expansion_bug.jsonld").getFile());

        kr = new Krill(json).apply(ki);

        // focus(254: spanContain(<tokens:base/s:t />, {254: spanNext(spanNext(spanNext(spanNext(tokens:s:der, tokens:s:alte), tokens:s:Digraph), tokens:s:Aa), spanExpansion(tokens:s:durch, []{1, 1}, right))}))

        assertEquals("... Buchstabe des Alphabetes. In Dänemark ist "
                + "[der alte Digraph Aa durch Å] ersetzt worden, "
                + "in Eigennamen und Ortsnamen ...", kr.getMatch(0)
                .getSnippetBrackets());
        assertEquals("WPD_AAA.00002", kr.getMatch(0).getDocID());
        assertEquals(kr.getTotalResults(), 1);

        // Now try with one file ahead
        ki = new KrillIndex();
        for (String i : new String[] { "00001", "00002" }) {
            ki.addDoc(
                    getClass().getResourceAsStream("/wiki/" + i + ".json.gz"),
                    true);
        };
        ki.commit();

        // Expansion bug
        // der alte Digraph Aa durch Å
        json = getString(getClass().getResource(
                "/queries/bugs/expansion_bug_2.jsonld").getFile());

        kr = new Krill(json).apply(ki);

        assertEquals("... Buchstabe des Alphabetes. In Dänemark ist "
                + "[der alte Digraph Aa durch Å] ersetzt worden, "
                + "in Eigennamen und Ortsnamen ...", kr.getMatch(0)
                .getSnippetBrackets());
        assertEquals("WPD_AAA.00002", kr.getMatch(0).getDocID());
        assertEquals(kr.getTotalResults(), 1);

        // der alte Digraph Aa durch []
        json = getString(getClass().getResource(
                "/queries/bugs/expansion_bug.jsonld").getFile());

        kr = new Krill(json).apply(ki);
        assertEquals("... Buchstabe des Alphabetes. In Dänemark ist "
                + "[der alte Digraph Aa durch Å] ersetzt worden, "
                + "in Eigennamen und Ortsnamen ...", kr.getMatch(0)
                .getSnippetBrackets());
        assertEquals("WPD_AAA.00002", kr.getMatch(0).getDocID());
        assertEquals(kr.getTotalResults(), 1);
    };


    /**
     * This is a Schreibgebrauch ressource that didn't work for
     * element queries.
     */
    @Test
    public void searchSchreibgebrauchData () throws IOException {
        // Construct index
        KrillIndex ki = new KrillIndex();
        // Indexing test files
        ki.addDoc(getClass()
                .getResourceAsStream("/sgbr/BSP-2013-01-32.json.gz"), true);
        ki.commit();

        Krill k = new Krill(new QueryBuilder("tokens").tag("base/s:s"));

        assertEquals(k.getSpanQuery().toString(), "<tokens:base/s:s />");

        Result kr = k.apply(ki);
        assertEquals(kr.getTotalResults(), 1);
        assertEquals(kr.getMatch(0).getSnippetBrackets(),
                "[Selbst ist der Jeck]");

        assertEquals(kr.getMatch(0).getTextSigle(), "PRO-DUD_BSP-2013-01.32");
    };

};
