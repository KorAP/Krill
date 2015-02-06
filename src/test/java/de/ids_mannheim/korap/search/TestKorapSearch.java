package de.ids_mannheim.korap.search;

import java.util.*;
import java.io.*;

import static de.ids_mannheim.korap.TestSimple.*;

import de.ids_mannheim.korap.KorapSearch;
import de.ids_mannheim.korap.KorapCollection;
import de.ids_mannheim.korap.KorapQuery;
import de.ids_mannheim.korap.KorapIndex;
import de.ids_mannheim.korap.index.FieldDocument;
import de.ids_mannheim.korap.index.SearchContext;
import de.ids_mannheim.korap.KorapFilter;
import de.ids_mannheim.korap.KorapResult;
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
public class TestKorapSearch {
    @Test
    public void searchCount () {
        KorapSearch ks = new KorapSearch(
            new KorapQuery("field1").seg("a").with("b")
        );
        // Count:
        ks.setCount(30);
        assertEquals(ks.getCount(), 30);
        ks.setCount(20);
        assertEquals(ks.getCount(), 20);
        ks.setCount(-50);
        assertEquals(ks.getCount(), 20);
        ks.setCount(500);
        assertEquals(ks.getCount(), ks.getCountMax());
    };

    @Test
    public void searchStartIndex () {
        KorapSearch ks = new KorapSearch(
            new KorapQuery("field1").seg("a").with("b")
        );
        // startIndex
        ks.setStartIndex(5);
        assertEquals(ks.getStartIndex(), 5);
        ks.setStartIndex(1);
        assertEquals(ks.getStartIndex(), 1);
        ks.setStartIndex(0);
        assertEquals(ks.getStartIndex(), 0);
        ks.setStartIndex(70);
        assertEquals(ks.getStartIndex(), 70);
        ks.setStartIndex(-5);
        assertEquals(ks.getStartIndex(), 0);
    };

    @Test
    public void searchQuery () {
        KorapSearch ks = new KorapSearch(
            new KorapQuery("field1").seg("a").with("b")
        );
        // query
        assertEquals(ks.getQuery().toString(), "spanSegment(field1:a, field1:b)");
    };


    @Test
    public void searchIndex () throws IOException {
        // Construct index
        KorapIndex ki = new KorapIndex();
        // Indexing test files
        for (String i : new String[] {"00001",
                                      "00002",
                                      "00003",
                                      "00004",
                                      "00005",
                                      "00006",
                                      "02439"}) {
            ki.addDocFile(
                getClass().getResource("/wiki/" + i + ".json.gz").getFile(),
                true
            );
        };
        ki.commit();

        KorapSearch ks = new KorapSearch(
	        new KorapQuery("tokens").seg("s:Buchstaben")
        );
        ks.getCollection().filter(
            new KorapFilter().and("textClass", "reisen")
        );
        ks.setCount(3);
        ks.setStartIndex(5);
        ks.context.left.setLength(1);
        ks.context.right.setLength(1);
        KorapResult kr = ks.run(ki);
        assertEquals(kr.getTotalResults(), 6);
        assertEquals(
            kr.getMatch(0).getSnippetBrackets(),
            "... dem [Buchstaben] A ..."
        );
    };


    @Test
    public void searchJSON () throws IOException {
        // Construct index
        KorapIndex ki = new KorapIndex();
        // Indexing test files
        for (String i : new String[] {"00001",
                                      "00002",
                                      "00003",
                                      "00004",
                                      "00005",
                                      "00006",
                                      "02439"}) {
            ki.addDocFile(
                getClass().getResource("/wiki/" + i + ".json.gz").getFile(),
                true
            );
        };
        ki.commit();

        String json = getString(
            getClass().getResource("/queries/metaquery3.jsonld").getFile()
        );

        KorapSearch ks = new KorapSearch(json);
		KorapResult kr = ks.run(ki);
        assertEquals(kr.getTotalResults(), 66);
        assertEquals(5, kr.getItemsPerPage());
        assertEquals(5, kr.getStartIndex());
        assertEquals(
            "... a: A ist [der klangreichste] der V ...",
            kr.getMatch(0).getSnippetBrackets()
        );
    };

    @Test
    public void searchJSON2 () throws IOException {
        // Construct index
        KorapIndex ki = new KorapIndex();
        // Indexing test files
        for (String i : new String[] {"00001",
                                      "00002",
                                      "00003",
                                      "00004",
                                      "00005",
                                      "00006",
                                      "02439",
                                      "00012-fakemeta",
                                      "00030-fakemeta",
                                      /*
                                        "02035-substring",
                                        "05663-unbalanced",
                                        "07452-deep"
                                      */
            }) {
            ki.addDocFile(
                getClass().getResource("/wiki/" + i + ".json.gz").getFile(),
                true
            );
        };
        ki.commit();

        String json = getString(
            getClass().getResource("/queries/metaquery4.jsonld").getFile()
        );

        KorapSearch ks = new KorapSearch(json);
        KorapResult kr = ks.run(ki);

        assertEquals(kr.getTotalResults(), 1);

        ks = new KorapSearch(json);
        // Ignore the collection part of the query!
        ks.setCollection(new KorapCollection());
        kr = ks.run(ki);

        assertEquals(kr.getTotalResults(), 5);

        json = getString(
            getClass().getResource("/queries/metaquery5.jsonld").getFile()
        );

        ks = new KorapSearch(json);
        kr = ks.run(ki);
        assertEquals(kr.getTotalResults(), 1);

        json = getString(
            getClass().getResource("/queries/metaquery6.jsonld").getFile()
        );
        ks = new KorapSearch(json);
        kr = ks.run(ki);
        assertEquals(kr.getTotalResults(), 1);
    };


    @Test
    public void searchJSONFailure () throws IOException {
        // Construct index
        KorapIndex ki = new KorapIndex();
        // Indexing test files
        for (String i : new String[] {"00001",
                                      "00002",
                                      "00003",
                                      "00004",
                                      "00005",
                                      "00006",
                                      "02439"
            }) {
            ki.addDocFile(
                getClass().getResource("/wiki/" + i + ".json.gz").getFile(),
                true
            );
        };
        ki.commit();
        KorapResult kr = new KorapSearch("{ query").run(ki);
        assertEquals(kr.getTotalResults(), 0);
        assertEquals(kr.getError(0).getMessage(), "Unable to parse JSON");
    };


    @Test
    public void searchJSONindexboundary () throws IOException {
        // Construct index
        KorapIndex ki = new KorapIndex();
        // Indexing test files
        for (String i : new String[] {"00001",
                                      "00002",
                                      "00003",
                                      "00004",
                                      "00005",
                                      "00006",
                                      "02439"}) {
            ki.addDocFile(
                getClass().getResource("/wiki/" + i + ".json.gz").getFile(),
                true
            );
        };
        ki.commit();

        String json = getString(
            getClass().getResource("/queries/bsp-fail1.jsonld").getFile()
        );

        KorapResult kr = new KorapSearch(json).run(ki);
        assertEquals(0, kr.getStartIndex());
        assertEquals(kr.getTotalResults(), 0);
        assertEquals(25, kr.getItemsPerPage());
    };


    @Test
    public void searchJSONindexboundary2 () throws IOException {
        // Construct index
        KorapIndex ki = new KorapIndex();
        // Indexing test files
        for (String i : new String[] {"00001",
                                      "00002",
                                      "00003",
                                      "00004",
                                      "00005",
                                      "00006",
                                      "02439"}) {
            ki.addDocFile(
                getClass().getResource("/wiki/" + i + ".json.gz").getFile(),
                true
            );
        };
        ki.commit();

        String json = getString(
            getClass().getResource("/queries/bsp-fail2.jsonld").getFile()
        );

        KorapResult kr = new KorapSearch(json).run(ki);
        assertEquals(50, kr.getItemsPerPage());
        assertEquals(49950, kr.getStartIndex());
        assertEquals(kr.getTotalResults(), 0);
    };


    @Test
    public void searchJSONcontext () throws IOException {
        // Construct index
        KorapIndex ki = new KorapIndex();
        // Indexing test files
        for (String i : new String[] {"00001",
                                      "00002",
                                      "00003",
                                      "00004",
                                      "00005",
                                      "00006",
                                      "02439"}) {
            ki.addDocFile(
                getClass().getResource("/wiki/" + i + ".json.gz").getFile(),
                true
            );
        };
        ki.commit();

        String json = getString(
            getClass().getResource("/queries/bsp-context.jsonld").getFile()
        );

        KorapSearch ks = new KorapSearch(json);
        KorapResult kr = ks.run(ki);
        assertEquals(kr.getTotalResults(), 10);
        assertEquals("A bzw. a ist der erste Buchstabe des" +
                     " lateinischen [Alphabets] und ein Vokal." +
                     " Der Buchstabe A hat in deutschen Texten" +
                     " eine durchschnittliche Häufigkeit  ...",
                     kr.getMatch(0).getSnippetBrackets());

        ks.setCount(5);
        ks.setStartPage(2);
        kr = ks.run(ki);
        assertEquals(kr.getTotalResults(), 10);
        assertEquals(5, kr.getStartIndex());
        assertEquals(5, kr.getItemsPerPage());

        json = getString(
            getClass().getResource("/queries/bsp-context-2.jsonld").getFile()
        );

        kr = new KorapSearch(json).run(ki);
        assertEquals(kr.getTotalResults(), -1);
        assertEquals("... lls seit den Griechen beibehalten worden." +
                     " 3. Bedeutungen in der Biologie steht A für"+
                     " das Nukleosid Adenosin steht A die Base"+
                     " Adenin steht A für die Aminosäure Alanin"+
                     " in der Informatik steht a für den dezimalen"+
                     " [Wert] 97 sowohl im ASCII- als auch im"+
                     " Unicode-Zeichensatz steht A für den dezimalen"+
                     " Wert 65 sowohl im ASCII- als auch im"+
                     " Unicode-Zeichensatz als Kfz-Kennzeichen"+
                     " steht A in Deutschland für Augsburg."+
                     " in Österreich auf ...",
                     kr.getMatch(0).getSnippetBrackets());
    };

    @Test
    public void searchJSONstartPage () throws IOException {
        // Construct index
        KorapIndex ki = new KorapIndex();
        // Indexing test files
        for (String i : new String[] {"00001",
                                      "00002",
                                      "00003",
                                      "00004",
                                      "00005",
                                      "00006",
                                      "02439"}) {
            ki.addDocFile(
                getClass().getResource("/wiki/" + i + ".json.gz").getFile(),
                true
            );
        };
        ki.commit();

        String json = getString(
            getClass().getResource("/queries/bsp-paging.jsonld").getFile()
        );

        KorapSearch ks = new KorapSearch(json);
        KorapResult kr = ks.run(ki);
        assertEquals(kr.getTotalResults(), 10);
        assertEquals(5, kr.getStartIndex());
        assertEquals(5, kr.getItemsPerPage());

        json = getString(
            getClass().getResource("/queries/bsp-cutoff.jsonld").getFile()
        );
        ks = ks = new KorapSearch(json);
        kr = ks.run(ki);
        assertEquals(kr.getTotalResults(), -1);
        assertEquals(2, kr.getStartIndex());
        assertEquals(2, kr.getItemsPerPage());

        json = getString(
            getClass().getResource("/queries/metaquery9.jsonld").getFile()
        );
        KorapCollection kc = new KorapCollection(json);
        kc.setIndex(ki);
        assertEquals(7, kc.numberOf("documents"));
    };


    @Test
    public void searchJSONitemsPerResource () throws IOException {
        // Construct index
        KorapIndex ki = new KorapIndex();
        // Indexing test files
        for (String i : new String[] {"00001",
                                      "00002",
                                      "00003",
                                      "00004",
                                      "00005",
                                      "00006",
                                      "02439"}) {
            ki.addDocFile(
                getClass().getResource("/wiki/" + i + ".json.gz").getFile(),
                true
            );
        };
        ki.commit();
        String json = getString(
            getClass().
            getResource("/queries/bsp-itemsPerResource.jsonld").
            getFile()
        );

        KorapSearch ks = new KorapSearch(json);
        KorapResult kr = ks.run(ki);
        assertEquals(kr.getTotalResults(), 10);
        assertEquals(0, kr.getStartIndex());
        assertEquals(20, kr.getItemsPerPage());

        assertEquals("WPD_AAA.00001", kr.getMatch(0).getDocID());
        assertEquals("WPD_AAA.00001", kr.getMatch(1).getDocID());
        assertEquals("WPD_AAA.00001", kr.getMatch(6).getDocID());
        assertEquals("WPD_AAA.00002", kr.getMatch(7).getDocID());
        assertEquals("WPD_AAA.00002", kr.getMatch(8).getDocID());
        assertEquals("WPD_AAA.00004", kr.getMatch(9).getDocID());

        ks = new KorapSearch(json);
        ks.setItemsPerResource(1);

        kr = ks.run(ki);

        assertEquals("WPD_AAA.00001", kr.getMatch(0).getDocID());
        assertEquals("WPD_AAA.00002", kr.getMatch(1).getDocID());
        assertEquals("WPD_AAA.00004", kr.getMatch(2).getDocID());
        
        assertEquals(kr.getTotalResults(), 3);
        assertEquals(0, kr.getStartIndex());
        assertEquals(20, kr.getItemsPerPage());
        
        ks = new KorapSearch(json);
        ks.setItemsPerResource(2);

        kr = ks.run(ki);

        assertEquals("WPD_AAA.00001", kr.getMatch(0).getDocID());
        assertEquals("WPD_AAA.00001", kr.getMatch(1).getDocID());
        assertEquals("WPD_AAA.00002", kr.getMatch(2).getDocID());
        assertEquals("WPD_AAA.00002", kr.getMatch(3).getDocID());
        assertEquals("WPD_AAA.00004", kr.getMatch(4).getDocID());
        
        assertEquals(kr.getTotalResults(), 5);
        assertEquals(0, kr.getStartIndex());
        assertEquals(20, kr.getItemsPerPage());

        ks = new KorapSearch(json);
        ks.setItemsPerResource(1);
        ks.setStartIndex(1);
        ks.setCount(1);

        kr = ks.run(ki);
	
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
        KorapIndex ki = new KorapIndex();
        // Indexing test files
        int uid = 1;
        for (String i : new String[] {"00001",
                                      "00002",
                                      "00003",
                                      "00004",
                                      "00005",
                                      "00006",
                                      "02439"}) {
            ki.addDocFile(
                uid++,
                getClass().getResource("/wiki/" + i + ".json.gz").getFile(),
                true
            );
        };
        ki.commit();

        String json = getString(
            getClass().
            getResource("/queries/bsp-uid-example.jsonld").
            getFile()
        );

        KorapSearch ks = new KorapSearch(json);
        ks.setItemsPerResource(1);
        KorapCollection kc = new KorapCollection();
        kc.filterUIDs(new String[]{"1", "4"});
        kc.setIndex(ki);
        ks.setCollection(kc);

        KorapResult kr = ks.run(ki);

        assertEquals(kr.getTotalResults(), 2);
        assertEquals(0, kr.getStartIndex());
        assertEquals(25, kr.getItemsPerPage());
    };


    @Test
    public void searchJSONnewJSON () throws IOException {
        // Construct index
        KorapIndex ki = new KorapIndex();
        // Indexing test files
        FieldDocument fd = ki.addDocFile(
            1,
            getClass().
            getResource("/goe/AGA-03828.json.gz").
            getFile(),
            true
        );
        ki.commit();

        assertEquals(fd.getUID(), 1);
        assertEquals(fd.getTextSigle(),   "GOE_AGA.03828");
        assertEquals(fd.getDocSigle(),    "GOE_AGA");
        assertEquals(fd.getCorpusSigle(), "GOE");
        assertEquals(fd.getTitle()  ,     "Autobiographische Einzelheiten");
        assertNull(fd.getSubTitle());
        assertEquals(fd.getTextType(),    "Autobiographie");
        assertNull(fd.getTextTypeArt());
        assertNull(fd.getTextTypeRef());
        assertNull(fd.getTextColumn());
        assertNull(fd.getTextDomain());
        assertEquals(fd.getPages(),       "529-547");
        assertEquals(fd.getLicense(),     "QAO-NC");
        assertEquals(fd.getCreationDate().toString(), "18200000");
        assertEquals(fd.getPubDate().toString(),      "19820000");
        assertEquals(fd.getAuthor(),      "Goethe, Johann Wolfgang von");
        assertNull(fd.getTextClass());
        assertEquals(fd.getLanguage(),    "de");
        assertEquals(fd.getPubPlace(),    "München");
        assertEquals(fd.getReference(),
                     "Goethe, Johann Wolfgang von:"+
                     " Autobiographische Einzelheiten,"+
                     " (Geschrieben bis 1832), In: Goethe,"+
                     " Johann Wolfgang von: Goethes Werke,"+
                     " Bd. 10, Autobiographische Schriften"+
                     " II, Hrsg.: Trunz, Erich. München: "+
                     "Verlag C. H. Beck, 1982, S. 529-547");
        assertEquals(fd.getPublisher(),   "Verlag C. H. Beck");
        assertNull(fd.getEditor());
        assertNull(fd.getFileEditionStatement());
        assertNull(fd.getBiblEditionStatement());
        assertNull(fd.getKeywords());

        assertEquals(fd.getTokenSource(), "opennlp#tokens");
        assertEquals(fd.getFoundries(),
                     "base base/paragraphs base/sentences corenlp "+
                     "corenlp/constituency corenlp/morpho "+
                     "corenlp/namedentities corenlp/sentences "+
                     "glemm glemm/morpho mate mate/morpho"+
                     " opennlp opennlp/morpho opennlp/sentences"+
                     " treetagger treetagger/morpho "+
                     "treetagger/sentences");
        assertEquals(fd.getLayerInfos(),
                     "base/s=spans corenlp/c=spans corenlp/ne=tokens"+
                     " corenlp/p=tokens corenlp/s=spans glemm/l=tokens"+
                     " mate/l=tokens mate/m=tokens mate/p=tokens"+
                     " opennlp/p=tokens opennlp/s=spans tt/l=tokens"+
                     " tt/p=tokens tt/s=spans");

        assertEquals(fd.getCorpusTitle(), "Goethes Werke");
        assertNull(fd.getCorpusSubTitle());
        assertEquals(fd.getCorpusAuthor(),  "Goethe, Johann Wolfgang von");
        assertEquals(fd.getCorpusEditor(),  "Trunz, Erich");
        assertEquals(fd.getDocTitle(),
            "Goethe: Autobiographische Schriften II, (1817-1825, 1832)"
        );
        assertNull(fd.getDocSubTitle());
        assertNull(fd.getDocEditor());
        assertNull(fd.getDocAuthor());

        KorapSearch ks = new KorapSearch(
            new KorapQuery("tokens").
            seg("mate/m:case:nom").
            with("mate/m:number:pl")
        );
        KorapResult kr = ks.run(ki);

        assertEquals(kr.getTotalResults(), 148);
        assertEquals(0, kr.getStartIndex());
        assertEquals(25, kr.getItemsPerPage());
    };


    @Test
    public void searchJSONnewJSON2 () throws IOException {
        // Construct index
        KorapIndex ki = new KorapIndex();
        // Indexing test files
        FieldDocument fd = ki.addDocFile(
            1,
            getClass().
            getResource("/bzk/D59-00089.json.gz").
            getFile(),
            true
        );
        ki.commit();

        assertEquals(fd.getUID(), 1);
        assertEquals(fd.getTextSigle(),   "BZK_D59.00089");
        assertEquals(fd.getDocSigle(),    "BZK_D59");
        assertEquals(fd.getCorpusSigle(), "BZK");
        assertEquals(fd.getTitle()  ,     "Saragat-Partei zerfällt");
        assertEquals(fd.getPubDate().toString(),      "19590219");

        assertNull(fd.getSubTitle());
        assertNull(fd.getAuthor());
        assertNull(fd.getEditor());
        assertEquals(fd.getPubPlace(),    "Berlin");
        assertNull(fd.getPublisher());
        assertEquals(fd.getTextType(),    "Zeitung: Tageszeitung");
        assertNull(fd.getTextTypeArt());
        assertEquals(fd.getTextTypeRef(), "Tageszeitung");
        assertEquals(fd.getTextDomain(),  "Politik");
        assertEquals(fd.getCreationDate().toString(), "19590219");
        assertEquals(fd.getLicense(),     "ACA-NC-LC");
        assertEquals(fd.getTextColumn(),  "POLITIK");
        assertNull(fd.getPages());
        assertEquals(fd.getTextClass(), "politik ausland");
        assertNull(fd.getFileEditionStatement());
        assertNull(fd.getBiblEditionStatement());
        
        assertEquals(fd.getLanguage(),    "de");
        assertEquals(
            fd.getReference(),
            "Neues Deutschland, [Tageszeitung], 19.02.1959, Jg. 14,"+
            " Berliner Ausgabe, S. 7. - Sachgebiet: Politik, "+
            "Originalressort: POLITIK; Saragat-Partei zerfällt");
        assertNull(fd.getPublisher());
        assertNull(fd.getKeywords());

        assertEquals(fd.getTokenSource(), "opennlp#tokens");

        assertEquals(
            fd.getFoundries(),
            "base base/paragraphs base/sentences corenlp "+
            "corenlp/constituency corenlp/morpho corenlp/namedentities"+
            " corenlp/sentences glemm glemm/morpho mate mate/morpho"+
            " opennlp opennlp/morpho opennlp/sentences treetagger"+
            " treetagger/morpho treetagger/sentences");

        assertEquals(
            fd.getLayerInfos(),
            "base/s=spans corenlp/c=spans corenlp/ne=tokens"+
            " corenlp/p=tokens corenlp/s=spans glemm/l=tokens"+
            " mate/l=tokens mate/m=tokens mate/p=tokens"+
            " opennlp/p=tokens opennlp/s=spans tt/l=tokens"+
            " tt/p=tokens tt/s=spans");

        assertEquals(fd.getCorpusTitle(), "Bonner Zeitungskorpus");
        assertNull(fd.getCorpusSubTitle());
        assertNull(fd.getCorpusAuthor());
        assertNull(fd.getCorpusEditor());

        assertEquals(fd.getDocTitle(), "Neues Deutschland");
        assertEquals(
            fd.getDocSubTitle(),
            "Organ des Zentralkomitees der Sozialistischen "+
            "Einheitspartei Deutschlands");
        assertNull(fd.getDocEditor());
        assertNull(fd.getDocAuthor());
        
        KorapSearch ks = new KorapSearch(
            new KorapQuery("tokens").
            seg("mate/m:case:nom").
            with("mate/m:number:sg")
        );
        KorapResult kr = ks.run(ki);

        assertEquals(kr.getTotalResults(), 6);
        assertEquals(0, kr.getStartIndex());
        assertEquals(25, kr.getItemsPerPage());
    };


    @Test
    public void searchJSONcosmasBoundaryBug () throws IOException {
        // Construct index
        KorapIndex ki = new KorapIndex();
        // Indexing test files
        FieldDocument fd = ki.addDocFile(
            1,
            getClass().
            getResource("/bzk/D59-00089.json.gz").
            getFile(),
            true
	    );
        ki.commit();

        String json = getString(
	        getClass().
            getResource("/queries/bugs/cosmas_boundary.jsonld").
            getFile()
        );

        KorapQuery kq = new KorapQuery("tokens");
        KorapSearch ks = new KorapSearch(
            kq.focus(
                1,
                kq.contains(kq.tag("base/s:s"), kq._(1, kq.seg("s:Leben")))
            )
        );

        KorapResult kr = ks.run(ki);
        assertEquals(
            kr.getQuery(),
            "focus(1: spanContain(<tokens:base/s:s />, {1: tokens:s:Leben}))"
        );
        assertEquals(
            kr.getMatch(0).getSnippetBrackets(),
            "... Initiative\" eine neue politische Gruppierung ins " +
            "[{1:Leben}] gerufen hatten. Pressemeldungen zufolge haben sich ..."
        );

        // Try with high class - don't highlight
        ks = new KorapSearch(
            kq.focus(
                129,
                kq.contains(kq.tag("base/s:s"), kq._(129, kq.seg("s:Leben")))
            )
        );

        kr = ks.run(ki);
        assertEquals(
            kr.getQuery(),
            "focus(129: spanContain(<tokens:base/s:s />, {129: tokens:s:Leben}))"
        );
        assertEquals(
            kr.getMatch(0).getSnippetBrackets(),
            "... Initiative\" eine neue politische Gruppierung ins " +
            "[Leben] gerufen hatten. Pressemeldungen zufolge haben sich ..."
        );

        ks = new KorapSearch(json);
        kr = ks.run(ki);
        assertEquals(
            kr.getQuery(),
            "focus(129: spanElementDistance({129: tokens:s:Namen}, " +
            "{129: tokens:s:Leben}, [(base/s:s[0:1], notOrdered, notExcluded)]))"
        );
        assertEquals(
            kr.getMatch(0).getSnippetBrackets(),
            "... ihren Austritt erklärt und unter dem [Namen \"Einheitsbewegung " +
            "der sozialistischen Initiative\" eine neue politische Gruppierung " +
            "ins Leben] gerufen hatten. Pressemeldungen zufolge haben sich ..."
        );
        assertEquals(kr.getTotalResults(), 1);
        assertEquals(0, kr.getStartIndex());
    };

    @Test
    public void searchJSONmultipleClassesBug () throws IOException {
        // Construct index
        KorapIndex ki = new KorapIndex();
        // Indexing test files
        ki.addDocFile(
            1,
            getClass().
            getResource("/bzk/D59-00089.json.gz").
            getFile(),
            true
        );
        ki.addDocFile(
            2,
            getClass().
            getResource("/bzk/D59-00089.json.gz").
            getFile(),
            true
        );

        ki.commit();

        String json = getString(
            getClass().
            getResource("/queries/bugs/multiple_classes.jsonld").
            getFile()
        );
	
        KorapSearch ks = new KorapSearch(json);
        KorapResult kr = ks.run(ki);
        assertEquals(
            kr.getQuery(),
            "{4: spanNext({1: spanNext({2: tokens:s:ins}, "+
            "{3: tokens:s:Leben})}, tokens:s:gerufen)}"
        );
        assertEquals(
            kr.getMatch(0).getSnippetBrackets(),
            "... sozialistischen Initiative\" eine neue politische"+
            " Gruppierung [{4:{1:{2:ins} {3:Leben}} gerufen}] hatten. " +
            "Pressemeldungen zufolge haben sich in ..."
        );
        assertEquals(kr.getTotalResults(), 2);
        assertEquals(0, kr.getStartIndex());
    };

    @Test
    public void searchJSONmultipleClassesBugTokenList () throws IOException {
        // Construct index
        KorapIndex ki = new KorapIndex();
        // Indexing test files
        ki.addDocFile(
            1,
            getClass().
            getResource("/goe/AGA-03828.json.gz").
            getFile(),
            true
        );
        ki.addDocFile(
            2,
            getClass().
            getResource("/bzk/D59-00089.json.gz").
            getFile(),
            true
        );

        ki.commit();

        String json = getString(
            getClass().
            getResource("/queries/bugs/multiple_classes.jsonld").
            getFile()
        );
	
        KorapSearch ks = new KorapSearch(json);
        KorapResult kr = ks.run(ki);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode res = mapper.readTree(kr.toTokenListJsonString());

        assertEquals(1, res.at("/totalResults").asInt());
        assertEquals(
            "{4: spanNext({1: spanNext({2: tokens:s:ins}, " +
            "{3: tokens:s:Leben})}, tokens:s:gerufen)}",
            res.at("/query").asText());
        assertEquals(0, res.at("/startIndex").asInt());
        assertEquals(25, res.at("/itemsPerPage").asInt());

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
        KorapIndex ki = new KorapIndex();

        assertEquals(ki.numberOf("documents"), 0);

        // Indexing test files
        FieldDocument fd = ki.addDocFile(
            1,
            getClass().
            getResource("/bzk/D59-00089.json.gz").
            getFile(),
            true
        );
        ki.commit();

        assertEquals(ki.numberOf("documents"), 1);
        assertEquals("BZK", fd.getCorpusSigle());

        // [tt/p="A.*"]{0,3}[tt/p="N.*"]
        String json = getString(
            getClass().
            getResource("/queries/bugs/multiterm_rewrite.jsonld").
            getFile()
        );
	
        KorapSearch ks = new KorapSearch(json);
        KorapCollection kc = ks.getCollection();

        // No index was set
        assertEquals(-1, kc.numberOf("documents"));
        kc.setIndex(ki);

        // Index was set but vc restricted to WPD
        assertEquals(0, kc.numberOf("documents"));

        kc.extend(
            new KorapFilter().or("corpusSigle", "BZK")
        );
        ks.setCollection(kc);
        assertEquals(1, kc.numberOf("documents"));

        KorapResult kr = ks.run(ki);
        
        assertEquals(
            kr.getQuery(),
            "spanOr([SpanMultiTermQueryWrapper(tokens:/tt/p:N.*/), " +
            "spanNext(spanRepetition(SpanMultiTermQueryWrapper"+
            "(tokens:/tt/p:A.*/){1,3}), " +
            "SpanMultiTermQueryWrapper(tokens:/tt/p:N.*/))])"
        );

        assertEquals(kr.getTotalResults(), 58);
        assertEquals(0, kr.getStartIndex());

        assertEquals(
            kr.getMatch(0).getSnippetBrackets(),
            "[Saragat-Partei] zerfällt Rom (ADN) die von dem"
        );
        assertEquals(
            kr.getMatch(1).getSnippetBrackets(),
            "[Saragat-Partei] zerfällt Rom (ADN) die von dem"
        );
        assertEquals(
            kr.getMatch(2).getSnippetBrackets(),
            "Saragat-Partei zerfällt [Rom] (ADN) "+
            "die von dem Rechtssozialisten Saragat"
        );
        assertEquals(
            kr.getMatch(3).getSnippetBrackets(),
            "Saragat-Partei zerfällt Rom ([ADN]) "+
            "die von dem Rechtssozialisten Saragat geführte"
        );

        assertEquals(
            kr.getMatch(23).getSnippetBrackets(),
            "dem Namen \"Einheitsbewegung der sozialistischen "+
            "Initiative\" [eine neue politische Gruppierung] "+
            "ins Leben gerufen hatten. Pressemeldungen zufolge"
        );
    };


    @Test
    public void searchJSONCollection () throws IOException {
        // Construct index
        KorapIndex ki = new KorapIndex();
        // Indexing test files
        for (String i : new String[] {"00001",
                                      "00002",
                                      "00003",
                                      "00004",
                                      "00005",
                                      "00006",
                                      "02439"}) {
            ki.addDocFile(
                getClass().
                getResource("/wiki/" + i + ".json.gz").
                getFile(),
                true
            );
        };
        ki.commit();
        String json = getString(
            getClass().
            getResource("/queries/metaquery8-nocollection.jsonld").
            getFile()
        );
	
        KorapSearch ks = new KorapSearch(json);
        KorapResult kr = ks.run(ki);
        assertEquals(kr.getTotalResults(), 276);
        assertEquals(0, kr.getStartIndex());
        assertEquals(10, kr.getItemsPerPage());

        json = getString(
            getClass().
            getResource("/queries/metaquery8.jsonld").
            getFile()
        );
	
        ks = new KorapSearch(json);
        kr = ks.run(ki);

        assertEquals(kr.getTotalResults(), 147);
        assertEquals("WPD_AAA.00001", kr.getMatch(0).getDocID());
        assertEquals(0, kr.getStartIndex());
        assertEquals(10, kr.getItemsPerPage());

        json = getString(
            getClass().
            getResource("/queries/metaquery8-filtered.jsonld").
            getFile()
        );
	
        ks = new KorapSearch(json);
        kr = ks.run(ki);

        assertEquals(kr.getTotalResults(), 28);
        assertEquals("WPD_AAA.00002", kr.getMatch(0).getDocID());
        assertEquals(0, kr.getStartIndex());
        assertEquals(10, kr.getItemsPerPage());

        json = getString(
            getClass().
            getResource("/queries/metaquery8-filtered-further.jsonld").
            getFile()
        );
	
        ks = new KorapSearch(json);
        kr = ks.run(ki);

        assertEquals(kr.getTotalResults(), 0);
        assertEquals(0, kr.getStartIndex());
        assertEquals(10, kr.getItemsPerPage());
        
        json = getString(
            getClass().
            getResource("/queries/metaquery8-filtered-nested.jsonld").
            getFile()
        );
	
        ks = new KorapSearch(json);
        kr = ks.run(ki);

        assertEquals("filter with QueryWrapperFilter("+
                     "+(ID:WPD_AAA.00003 (+tokens:s:die"+
                     " +tokens:s:Schriftzeichen)))",
                     ks.getCollection().getFilter(1).toString());

        assertEquals(kr.getTotalResults(), 119);
        assertEquals(0, kr.getStartIndex());
        assertEquals(10, kr.getItemsPerPage());
    };


    @Test
    public void searchJSONSentenceContext () throws IOException {
        // Construct index
        KorapIndex ki = new KorapIndex();
        // Indexing test files
        for (String i : new String[] {"00001",
                                      "00002",
                                      "00003",
                                      "00004",
                                      "00005",
                                      "00006",
                                      "02439"}) {
            ki.addDocFile(
                getClass().
                getResource("/wiki/" + i + ".json.gz").
                getFile(),
                true
            );
        };
        ki.commit();

        String json = getString(
            getClass().
            getResource("/queries/bsp-context-2.jsonld").
            getFile()
        );
	
        KorapSearch ks = new KorapSearch(json);
        ks.setCutOff(false);
        SearchContext sc = ks.getContext();
        sc.left.setLength((short) 10);
        sc.right.setLength((short) 10);
        
        KorapResult kr = ks.run(ki);
        assertEquals(
            kr.getMatch(1).getSnippetBrackets(),
            "... dezimalen [Wert] 65 sowohl ..."
        );
        assertEquals(kr.getTotalResults(), 3);
        assertEquals(0, kr.getStartIndex());
        assertEquals(25, kr.getItemsPerPage());
        assertFalse(kr.getContext().toJsonNode().toString().equals("\"s\""));

        json = getString(
            getClass().
            getResource("/queries/bsp-context-sentence.jsonld").
            getFile()
        );

        kr = new KorapSearch(json).run(ki);
        assertEquals(
            kr.getMatch(0).getSnippetBrackets(),
            "steht a für den dezimalen [Wert] 97 sowohl im ASCII-"+
            " als auch im Unicode-Zeichensatz"
        );
        assertEquals(
            kr.getMatch(1).getSnippetBrackets(),
            "steht A für den dezimalen [Wert] 65 sowohl im ASCII-"+
            " als auch im Unicode-Zeichensatz"
        );
        assertEquals(
            kr.getMatch(2).getSnippetBrackets(),
            "In einem Zahlensystem mit einer Basis größer "+
            "als 10 steht A oder a häufig für den dezimalen"+
            " [Wert] 10, siehe auch Hexadezimalsystem."
        );

        assertEquals(kr.getContext().toJsonNode().toString(), "\"s\"");
    };


    @Test
    public void searchJSONbug () throws IOException {
        // Construct index
        KorapIndex ki = new KorapIndex();
        // Indexing test files
        for (String i : new String[] {"00001",
                                      "00002",
                                      "00003",
                                      "00004",
                                      "00005",
                                      "00006",
                                      "02439"}) {
            ki.addDocFile(
                getClass().
                getResource("/wiki/" + i + ".json.gz").
                getFile(),
                true
            );
        };
        ki.commit();

        String json = getString(
            getClass().
            getResource("/queries/bsp-bug.jsonld").
            getFile()
        );

        KorapResult kr = new KorapSearch(json).run(ki);

        assertEquals(
            kr.getError(0).getMessage(),
            "Operation needs operand list"
        );
    };


    /**
     * This is a breaking test for #179
     */
    @Test
    public void searchJSONexpansionBug () throws IOException {
		// Construct index
		KorapIndex ki = new KorapIndex();
		// Indexing test files
		ki.addDocFile(
            getClass().
            getResource("/wiki/00002.json.gz").
            getFile(),
            true
        );
		ki.commit();
	
		// Expansion bug
		// der alte Digraph Aa durch Å
		String json = getString(
            getClass().
            getResource("/queries/bugs/expansion_bug_2.jsonld").
            getFile()
        );
	
		KorapResult kr = new KorapSearch(json).run(ki);				
		assertEquals("... Buchstabe des Alphabetes. In Dänemark ist " +
                     "[der alte Digraph Aa durch Å] ersetzt worden, " +
                     "in Eigennamen und Ortsnamen ...",
                     kr.getMatch(0).getSnippetBrackets());
		assertEquals("WPD_AAA.00002", kr.getMatch(0).getDocID());
		assertEquals(kr.getTotalResults(), 1);
        
		// der alte Digraph Aa durch []
		// Works with one document
		json = getString(
            getClass().
            getResource("/queries/bugs/expansion_bug.jsonld").
            getFile()
        );
	
		kr = new KorapSearch(json).run(ki);
		
		assertEquals("... Buchstabe des Alphabetes. In Dänemark ist " +
                     "[der alte Digraph Aa durch Å] ersetzt worden, " +
                     "in Eigennamen und Ortsnamen ...",
                     kr.getMatch(0).getSnippetBrackets());
		assertEquals("WPD_AAA.00002", kr.getMatch(0).getDocID());
		assertEquals(kr.getTotalResults(), 1);
	
		// Now try with one file ahead
		ki = new KorapIndex();
		for (String i : new String[] {"00001",
                                      "00002"}) {
		    ki.addDocFile(
                getClass().
                getResource("/wiki/" + i + ".json.gz").
                getFile(),
                true
            );
		};
		ki.commit();
	
		// Expansion bug
		// der alte Digraph Aa durch Å
		json = getString(
            getClass().
            getResource("/queries/bugs/expansion_bug_2.jsonld").
            getFile()
        );
	
		kr = new KorapSearch(json).run(ki);
		
		assertEquals("... Buchstabe des Alphabetes. In Dänemark ist " +
                     "[der alte Digraph Aa durch Å] ersetzt worden, " +
                     "in Eigennamen und Ortsnamen ...",
                     kr.getMatch(0).getSnippetBrackets());
		assertEquals("WPD_AAA.00002", kr.getMatch(0).getDocID());
		assertEquals(kr.getTotalResults(), 1);
	
		// der alte Digraph Aa durch []
		json = getString(
            getClass().
            getResource("/queries/bugs/expansion_bug.jsonld").
            getFile()
        );
	
		kr = new KorapSearch(json).run(ki);	
		assertEquals("... Buchstabe des Alphabetes. In Dänemark ist " +
                     "[der alte Digraph Aa durch Å] ersetzt worden, " +
                     "in Eigennamen und Ortsnamen ...",
                     kr.getMatch(0).getSnippetBrackets());
		assertEquals("WPD_AAA.00002", kr.getMatch(0).getDocID());
		assertEquals(kr.getTotalResults(), 1);
    };


    /*
      This test will crash soon - it's just here for nostalgic reasons!
    */
    @Test
    public void getFoundryDistribution () throws Exception {
        // Construct index
        KorapIndex ki = new KorapIndex();
        // Indexing test files
        for (String i : new String[] {"00001",
                                      "00002",
                                      "00003",
                                      "00004",
                                      "00005",
                                      "00006",
                                      "02439"}) {
            ki.addDocFile(
                getClass().
                getResource("/wiki/" + i + ".json.gz").
                getFile(),
                true
            );
        };
        ki.commit();

        KorapCollection kc = new KorapCollection(ki);

        assertEquals(7, kc.numberOf("documents"));

    	HashMap map = kc.getTermRelation("foundries");
        assertEquals((long) 7, map.get("-docs"));
        assertEquals((long) 7, map.get("treetagger"));
        assertEquals((long) 6, map.get("opennlp/morpho"));
        assertEquals((long) 6, map.get("#__opennlp/morpho:###:treetagger"));
        assertEquals((long) 7, map.get("#__opennlp:###:treetagger"));
    };


    @Test
    public void getTextClassDistribution () throws Exception {
        KorapIndex ki = new KorapIndex();
        ki.addDoc(
"{" +
"  \"fields\" : [" +
"    { \"primaryData\" : \"abc\" },{" +
"      \"name\" : \"tokens\"," +
"      \"data\" : [" +
"         [ \"s:a\", \"i:a\", \"_0#0-1\", \"-:t$<i>3\"]," +
"         [ \"s:b\", \"i:b\", \"_1#1-2\" ]," +
"         [ \"s:c\", \"i:c\", \"_2#2-3\" ]]}]," +
"  \"textClass\" : \"music entertainment\"" +
"}"
        );

        ki.addDoc(
"{" +
"  \"fields\" : [" +
"    { \"primaryData\" : \"abc\" },{" +
"      \"name\" : \"tokens\"," +
"      \"data\" : [" +
"         [ \"s:a\", \"i:a\", \"_0#0-1\", \"-:t$<i>3\"]," +
"         [ \"s:b\", \"i:b\", \"_1#1-2\" ]," +
"         [ \"s:c\", \"i:c\", \"_2#2-3\" ]]}]," +
"  \"textClass\" : \"music singing\"" +
"}"
        );

        ki.addDoc(
"{" +
"  \"fields\" : [" +
"    { \"primaryData\" : \"abc\" },{" +
"      \"name\" : \"tokens\"," +
"      \"data\" : [" +
"         [ \"s:a\", \"i:a\", \"_0#0-1\", \"-:t$<i>3\"]," +
"         [ \"s:b\", \"i:b\", \"_1#1-2\" ]," +
"         [ \"s:c\", \"i:c\", \"_2#2-3\" ]]}]," +
"  \"textClass\" : \"music entertainment jumping\"" +
"}"
        );
        ki.commit();

        KorapCollection kc = new KorapCollection(ki);
        assertEquals(3, kc.numberOf("documents"));

    	HashMap map = kc.getTermRelation("textClass");
        assertEquals((long) 1, map.get("singing"));
        assertEquals((long) 1, map.get("jumping"));
        assertEquals((long) 3, map.get("music"));
        assertEquals((long) 2, map.get("entertainment"));
        assertEquals((long) 3, map.get("-docs"));
        assertEquals((long) 2, map.get("#__entertainment:###:music"));
        assertEquals((long) 1, map.get("#__entertainment:###:jumping"));
        assertEquals((long) 0, map.get("#__entertainment:###:singing"));
        assertEquals((long) 0, map.get("#__jumping:###:singing"));
        assertEquals((long) 1, map.get("#__jumping:###:music"));
        assertEquals((long) 1, map.get("#__music:###:singing"));
        assertEquals(11, map.size());
        
        // System.err.println(kc.getTermRelationJSON("textClass"));
    };


    @Test
    public void getTextClassDistribution2 () throws Exception {
        KorapIndex ki = new KorapIndex();
        ki.addDoc(
"{" +
"  \"fields\" : [" +
"    { \"primaryData\" : \"abc\" },{" +
"      \"name\" : \"tokens\"," +
"      \"data\" : [" +
"         [ \"s:a\", \"i:a\", \"_0#0-1\", \"-:t$<i>3\"]," +
"         [ \"s:b\", \"i:b\", \"_1#1-2\" ]," +
"         [ \"s:c\", \"i:c\", \"_2#2-3\" ]]}]," +
"  \"textClass\" : \"\"" +
"}"
        );
        ki.commit();
        ki.addDoc(
"{" +
"  \"fields\" : [" +
"    { \"primaryData\" : \"abc\" },{" +
"      \"name\" : \"tokens\"," +
"      \"data\" : [" +
"         [ \"s:a\", \"i:a\", \"_0#0-1\", \"-:t$<i>3\"]," +
"         [ \"s:b\", \"i:b\", \"_1#1-2\" ]," +
"         [ \"s:c\", \"i:c\", \"_2#2-3\" ]]}]," +
"  \"textClass\" : \"music entertainment\"" +
"}"
        );

        ki.commit();
        ki.addDoc(
"{" +
"  \"fields\" : [" +
"    { \"primaryData\" : \"abc\" },{" +
"      \"name\" : \"tokens\"," +
"      \"data\" : [" +
"         [ \"s:a\", \"i:a\", \"_0#0-1\", \"-:t$<i>3\"]," +
"         [ \"s:b\", \"i:b\", \"_1#1-2\" ]," +
"         [ \"s:c\", \"i:c\", \"_2#2-3\" ]]}]," +
"  \"textClass\" : \"music singing\"" +
"}"
        );

        ki.addDoc(
"{" +
"  \"fields\" : [" +
"    { \"primaryData\" : \"abc\" },{" +
"      \"name\" : \"tokens\"," +
"      \"data\" : [" +
"         [ \"s:a\", \"i:a\", \"_0#0-1\", \"-:t$<i>3\"]," +
"         [ \"s:b\", \"i:b\", \"_1#1-2\" ]," +
"         [ \"s:c\", \"i:c\", \"_2#2-3\" ]]}]," +
"  \"textClass\" : \"music entertainment jumping\"" +
"}"
        );
        ki.commit();

        KorapCollection kc = new KorapCollection(ki);
        assertEquals(4, kc.numberOf("documents"));

    	HashMap map = kc.getTermRelation("textClass");
        assertEquals((long) 1, map.get("singing"));
        assertEquals((long) 1, map.get("jumping"));
        assertEquals((long) 3, map.get("music"));
        assertEquals((long) 2, map.get("entertainment"));
        assertEquals((long) 4, map.get("-docs"));
        assertEquals((long) 2, map.get("#__entertainment:###:music"));
        assertEquals((long) 1, map.get("#__entertainment:###:jumping"));
        assertEquals((long) 0, map.get("#__entertainment:###:singing"));
        assertEquals((long) 0, map.get("#__jumping:###:singing"));
        assertEquals((long) 1, map.get("#__jumping:###:music"));
        assertEquals((long) 1, map.get("#__music:###:singing"));
        assertEquals(11, map.size());
    };
};
