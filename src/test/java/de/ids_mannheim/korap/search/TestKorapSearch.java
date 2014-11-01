package de.ids_mannheim.korap.search;

import java.util.*;
import java.io.*;

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
	      getClass().getResource("/wiki/" + i + ".json.gz").getFile(), true
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
	assertEquals(6, kr.totalResults());
	assertEquals(kr.getMatch(0).getSnippetBrackets(), "... dem [Buchstaben] A ...");
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
	      getClass().getResource("/wiki/" + i + ".json.gz").getFile(), true
            );
	};
	ki.commit();

	String json = getString(getClass().getResource("/queries/metaquery3.jsonld").getFile());

	KorapSearch ks = new KorapSearch(json);
	
	KorapResult kr = ks.run(ki);
	assertEquals(66, kr.getTotalResults());
	assertEquals(5, kr.getItemsPerPage());
	assertEquals(5, kr.getStartIndex());
	assertEquals("... a: A ist [der klangreichste] der V ...", kr.getMatch(0).getSnippetBrackets());
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
	      getClass().getResource("/wiki/" + i + ".json.gz").getFile(), true
            );
	};
	ki.commit();

	String json = getString(getClass().getResource("/queries/metaquery4.jsonld").getFile());

	KorapSearch ks = new KorapSearch(json);
	KorapResult kr = ks.run(ki);

	assertEquals(1, kr.getTotalResults());

	ks = new KorapSearch(json);
	// Ignore the collection part of the query!
	ks.setCollection(new KorapCollection());
	kr = ks.run(ki);

	assertEquals(5, kr.getTotalResults());

	json = getString(getClass().getResource("/queries/metaquery5.jsonld").getFile());
	ks = new KorapSearch(json);
	kr = ks.run(ki);
	assertEquals(1, kr.getTotalResults());

	json = getString(getClass().getResource("/queries/metaquery6.jsonld").getFile());
	ks = new KorapSearch(json);
	kr = ks.run(ki);
	assertEquals(1, kr.getTotalResults());
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
	      getClass().getResource("/wiki/" + i + ".json.gz").getFile(), true
            );
	};
	ki.commit();

	KorapResult kr = new KorapSearch("{ query").run(ki);

	assertEquals(0, kr.getTotalResults());
	assertNotNull(kr.getErr());
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
	      getClass().getResource("/wiki/" + i + ".json.gz").getFile(), true
            );
	};
	ki.commit();

	String json = getString(getClass().getResource("/queries/bsp-fail1.jsonld").getFile());

	KorapResult kr = new KorapSearch(json).run(ki);
	assertEquals(0, kr.getStartIndex());
	assertEquals(0, kr.getTotalResults());
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
	      getClass().getResource("/wiki/" + i + ".json.gz").getFile(), true
            );
	};
	ki.commit();

	String json = getString(getClass().getResource("/queries/bsp-fail2.jsonld").getFile());

	KorapResult kr = new KorapSearch(json).run(ki);
	assertEquals(50, kr.getItemsPerPage());
	assertEquals(49950, kr.getStartIndex());
	assertEquals(0, kr.getTotalResults());
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
	      getClass().getResource("/wiki/" + i + ".json.gz").getFile(), true
            );
	};
	ki.commit();

	String json = getString(getClass().getResource("/queries/bsp-context.jsonld").getFile());

	KorapSearch ks = new KorapSearch(json);
	KorapResult kr = ks.run(ki);
	assertEquals(10, kr.getTotalResults());
	assertEquals("A bzw. a ist der erste Buchstabe des lateinischen [Alphabets] und ein Vokal. Der Buchstabe A hat in deutschen Texten eine durchschnittliche Häufigkeit  ...", kr.getMatch(0).getSnippetBrackets());

	ks.setCount(5);
	ks.setStartPage(2);
	kr = ks.run(ki);
	assertEquals(10, kr.getTotalResults());
	assertEquals(5, kr.getStartIndex());
	assertEquals(5, kr.getItemsPerPage());


	json = getString(getClass().getResource("/queries/bsp-context-2.jsonld").getFile());

	kr = new KorapSearch(json).run(ki);
	assertEquals(-1, kr.getTotalResults());
	assertEquals("... lls seit den Griechen beibehalten worden. 3. Bedeutungen in der Biologie steht A für das Nukleosid Adenosin steht A die Base Adenin steht A für die Aminosäure Alanin in der Informatik steht a für den dezimalen [Wert] 97 sowohl im ASCII- als auch im Unicode-Zeichensatz steht A für den dezimalen Wert 65 sowohl im ASCII- als auch im Unicode-Zeichensatz als Kfz-Kennzeichen steht A in Deutschland für Augsburg. in Österreich auf ...", kr.getMatch(0).getSnippetBrackets());
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
	      getClass().getResource("/wiki/" + i + ".json.gz").getFile(), true
            );
	};
	ki.commit();

	String json = getString(getClass().getResource("/queries/bsp-paging.jsonld").getFile());

	KorapSearch ks = new KorapSearch(json);
	KorapResult kr = ks.run(ki);
	assertEquals(10, kr.getTotalResults());
	assertEquals(5, kr.getStartIndex());
	assertEquals(5, kr.getItemsPerPage());

	json = getString(getClass().getResource("/queries/bsp-cutoff.jsonld").getFile());
	ks = ks = new KorapSearch(json);

	kr = ks.run(ki);
	assertEquals(-1, kr.getTotalResults());
	assertEquals(2, kr.getStartIndex());
	assertEquals(2, kr.getItemsPerPage());


	json = getString(getClass().getResource("/queries/metaquery9.jsonld").getFile());
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
	      getClass().getResource("/wiki/" + i + ".json.gz").getFile(), true
            );
	};
	ki.commit();

	String json = getString(getClass().getResource("/queries/bsp-itemsPerResource.jsonld").getFile());

	KorapSearch ks = new KorapSearch(json);
	KorapResult kr = ks.run(ki);
	assertEquals(10, kr.getTotalResults());
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

	assertEquals(3, kr.getTotalResults());
	assertEquals(0, kr.getStartIndex());
	assertEquals(20, kr.getItemsPerPage());


	ks = new KorapSearch(json);
	ks.setItemsPerResource(2);

	kr = ks.run(ki);

	//	System.err.println(kr.toJSON());

	assertEquals("WPD_AAA.00001", kr.getMatch(0).getDocID());
	assertEquals("WPD_AAA.00001", kr.getMatch(1).getDocID());
	assertEquals("WPD_AAA.00002", kr.getMatch(2).getDocID());
	assertEquals("WPD_AAA.00002", kr.getMatch(3).getDocID());
	assertEquals("WPD_AAA.00004", kr.getMatch(4).getDocID());

	assertEquals(5, kr.getTotalResults());
	assertEquals(0, kr.getStartIndex());
	assertEquals(20, kr.getItemsPerPage());


	ks = new KorapSearch(json);
	ks.setItemsPerResource(1);
	ks.setStartIndex(1);
	ks.setCount(1);

	kr = ks.run(ki);
	
	assertEquals("WPD_AAA.00002", kr.getMatch(0).getDocID());

	assertEquals(3, kr.getTotalResults());
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

	String json = getString(getClass().getResource("/queries/bsp-uid-example.jsonld").getFile());

	KorapSearch ks = new KorapSearch(json);
	ks.setItemsPerResource(1);
	KorapCollection kc = new KorapCollection();
	kc.filterUIDs(new String[]{"1", "4"});
	kc.setIndex(ki);
	ks.setCollection(kc);

	KorapResult kr = ks.run(ki);

	assertEquals(2, kr.getTotalResults());
	assertEquals(0, kr.getStartIndex());
	assertEquals(25, kr.getItemsPerPage());
    };

    @Test
    public void searchJSONnewJSON () throws IOException {
	// Construct index
	KorapIndex ki = new KorapIndex();
	// Indexing test files
	FieldDocument fd = ki.addDocFile(
            1,getClass().getResource("/goe/AGA-03828.json.gz").getFile(), true
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
	assertEquals(fd.getCorpusTitle(), "Goethes Werke");
	assertEquals(fd.getReference(),   "Goethe, Johann Wolfgang von: Autobiographische Einzelheiten, (Geschrieben bis 1832), In: Goethe, Johann Wolfgang von: Goethes Werke, Bd. 10, Autobiographische Schriften II, Hrsg.: Trunz, Erich. München: Verlag C. H. Beck, 1982, S. 529-547");
	assertEquals(fd.getPublisher(),   "Verlag C. H. Beck");
	assertEquals(fd.getCollEditor(),  "Trunz, Erich");
	assertEquals(fd.getCollEditor(),  "Trunz, Erich");
	assertNull(fd.getEditor());
	assertNull(fd.getFileEditionStatement());
	assertNull(fd.getBiblEditionStatement());
	assertNull(fd.getCollTitle());
	assertNull(fd.getCollSubTitle());
	assertNull(fd.getCollAuthor());
	assertNull(fd.getCorpusSubTitle());
	assertNull(fd.getKeywords());

	assertEquals(fd.getTokenSource(), "opennlp#tokens");
	assertEquals(fd.getFoundries(), "base base/paragraphs base/sentences corenlp corenlp/constituency corenlp/morpho corenlp/namedentities corenlp/sentences glemm glemm/morpho mate mate/morpho opennlp opennlp/morpho opennlp/sentences treetagger treetagger/morpho treetagger/sentences");
	assertEquals(fd.getLayerInfos(),  "base/s=spans corenlp/c=spans corenlp/ne=tokens corenlp/p=tokens corenlp/s=spans glemm/l=tokens mate/l=tokens mate/m=tokens mate/p=tokens opennlp/p=tokens opennlp/s=spans tt/l=tokens tt/p=tokens tt/s=spans");

	KorapSearch ks = new KorapSearch(
	    new KorapQuery("tokens").seg("mate/m:case:nom").with("mate/m:number:pl")
        );
	KorapResult kr = ks.run(ki);

	assertEquals(148, kr.getTotalResults());
	assertEquals(0, kr.getStartIndex());
	assertEquals(25, kr.getItemsPerPage());
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
	      getClass().getResource("/wiki/" + i + ".json.gz").getFile(), true
            );
	};
	ki.commit();

	String json = getString(getClass().getResource("/queries/metaquery8-nocollection.jsonld").getFile());
	
	KorapSearch ks = new KorapSearch(json);
	KorapResult kr = ks.run(ki);
	assertEquals(276, kr.getTotalResults());
	assertEquals(0, kr.getStartIndex());
	assertEquals(10, kr.getItemsPerPage());

	json = getString(getClass().getResource("/queries/metaquery8.jsonld").getFile());
	
	ks = new KorapSearch(json);
	kr = ks.run(ki);

	assertEquals(147, kr.getTotalResults());
	assertEquals("WPD_AAA.00001", kr.getMatch(0).getDocID());
	assertEquals(0, kr.getStartIndex());
	assertEquals(10, kr.getItemsPerPage());

	json = getString(getClass().getResource("/queries/metaquery8-filtered.jsonld").getFile());
	
	ks = new KorapSearch(json);
	kr = ks.run(ki);

	assertEquals(28, kr.getTotalResults());
	assertEquals("WPD_AAA.00002", kr.getMatch(0).getDocID());
	assertEquals(0, kr.getStartIndex());
	assertEquals(10, kr.getItemsPerPage());

	json = getString(getClass().getResource("/queries/metaquery8-filtered-further.jsonld").getFile());
	
	ks = new KorapSearch(json);
	kr = ks.run(ki);

	assertEquals(0, kr.getTotalResults());
	assertEquals(0, kr.getStartIndex());
	assertEquals(10, kr.getItemsPerPage());

	json = getString(getClass().getResource("/queries/metaquery8-filtered-nested.jsonld").getFile());
	
	ks = new KorapSearch(json);
	kr = ks.run(ki);

	assertEquals("filter with QueryWrapperFilter(+(ID:WPD_AAA.00003 (+tokens:s:die +tokens:s:Schriftzeichen)))", ks.getCollection().getFilter(1).toString());

	assertEquals(119, kr.getTotalResults());
	assertEquals(0, kr.getStartIndex());
	assertEquals(10, kr.getItemsPerPage());
    };


    @Test
    public void searchJSONSentenceContext () throws IOException {

	// Construct index
	KorapIndex ki = new KorapIndex();
	// Indexing test files
	for (String i : new String[] {"00001", "00002", "00003", "00004", "00005", "00006", "02439"}) {
	    ki.addDocFile(
	      getClass().getResource("/wiki/" + i + ".json.gz").getFile(), true
            );
	};
	ki.commit();

	String json = getString(getClass().getResource("/queries/bsp-context-2.jsonld").getFile());
	
	KorapSearch ks = new KorapSearch(json);
	ks.setCutOff(false);
	SearchContext sc = ks.getContext();
	sc.left.setLength((short) 10);
	sc.right.setLength((short) 10);

	KorapResult kr = ks.run(ki);
	assertEquals(kr.getMatch(1).getSnippetBrackets(), "... dezimalen [Wert] 65 sowohl ...");
	assertEquals(3, kr.getTotalResults());
	assertEquals(0, kr.getStartIndex());
	assertEquals(25, kr.getItemsPerPage());
	assertFalse(kr.getContext().toJSON().toString().equals("\"s\""));

	json = getString(getClass().getResource("/queries/bsp-context-sentence.jsonld").getFile());

	kr = new KorapSearch(json).run(ki);
	assertEquals(kr.getMatch(0).getSnippetBrackets(),
		     "steht a für den dezimalen [Wert] 97 sowohl im ASCII- als auch im Unicode-Zeichensatz");
	assertEquals(kr.getMatch(1).getSnippetBrackets(),
		     "steht A für den dezimalen [Wert] 65 sowohl im ASCII- als auch im Unicode-Zeichensatz");
	assertEquals(kr.getMatch(2).getSnippetBrackets(),
		     "In einem Zahlensystem mit einer Basis größer als 10 steht A oder a häufig für den dezimalen [Wert] 10, siehe auch Hexadezimalsystem.");

	assertEquals(kr.getContext().toJSON().toString(), "\"s\"");
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
	      getClass().getResource("/wiki/" + i + ".json.gz").getFile(), true
            );
	};
	ki.commit();

	String json = getString(getClass().getResource("/queries/bsp-bug.jsonld").getFile());

	KorapResult kr = new KorapSearch(json).run(ki);
	assertEquals(kr.getErrstr(), "Operation needs exactly two operands");
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
	      getClass().getResource("/wiki/" + i + ".json.gz").getFile(), true
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
"}");

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
"}");

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
"}");
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
"}");

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
"}");

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
"}");

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
"}");
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



    public static String getString (String path) {
	StringBuilder contentBuilder = new StringBuilder();
	try {
	    BufferedReader in = new BufferedReader(new FileReader(path));
	    String str;
	    while ((str = in.readLine()) != null) {
		contentBuilder.append(str);
	    };
	    in.close();
	} catch (IOException e) {
	    fail(e.getMessage());
	}
	return contentBuilder.toString();
    };
};
