import java.util.*;
import java.io.*;

import de.ids_mannheim.korap.KorapSearch;
import de.ids_mannheim.korap.KorapQuery;
import de.ids_mannheim.korap.KorapIndex;
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
	assertEquals(ks.getQuery().toString(), "spanNear([field1:a, field1:b], -1, false)");
    };

    @Test
    public void searchIndex () throws IOException {

	// Construct index
	KorapIndex ki = new KorapIndex();
	// Indexing test files
	for (String i : new String[] {"00001", "00002", "00003", "00004", "00005", "00006", "02439"}) {
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
	ks.leftContext.setLength(1);
	ks.rightContext.setLength(1);
	KorapResult kr = ks.run(ki);
	assertEquals(6, kr.totalResults());
	assertEquals(kr.getMatch(0).getSnippetBrackets(), "... dem [Buchstaben] A ...");
    };

    @Test
    public void searchJSON () throws IOException {

	// Construct index
	KorapIndex ki = new KorapIndex();
	// Indexing test files
	for (String i : new String[] {"00001", "00002", "00003", "00004", "00005", "00006", "02439"}) {
	    ki.addDocFile(
	      getClass().getResource("/wiki/" + i + ".json.gz").getFile(), true
            );
	};
	ki.commit();

	String json = getString(getClass().getResource("/queries/metaquery3.json").getFile());

	KorapResult kr = new KorapSearch(json).run(ki);
	assertEquals(29, kr.getTotalResults());
	assertEquals(5, kr.getItemsPerPage());
	assertEquals(5, kr.getStartIndex());
	assertEquals("... a: A ist [der klangreichste] der V ...", kr.getMatch(0).getSnippetBrackets());

	json = getString(getClass().getResource("/queries/metaquery4.json").getFile());

	kr = new KorapSearch(json).run(ki);
	assertEquals(0, kr.getTotalResults());
	assertEquals(5, kr.getItemsPerPage());
	assertEquals(5, kr.getStartIndex());
    };


    @Test
    public void searchJSONFailure () throws IOException {

	// Construct index
	KorapIndex ki = new KorapIndex();
	// Indexing test files
	for (String i : new String[] {"00001", "00002", "00003", "00004", "00005", "00006", "02439"}) {
	    ki.addDocFile(
	      getClass().getResource("/wiki/" + i + ".json.gz").getFile(), true
            );
	};
	ki.commit();

	KorapResult kr = new KorapSearch("{ query").run(ki);

	assertEquals(0, kr.getTotalResults());
	assertNotNull(kr.getError());
    };



    @Test
    public void searchJSONindexboundary () throws IOException {

	// Construct index
	KorapIndex ki = new KorapIndex();
	// Indexing test files
	for (String i : new String[] {"00001", "00002", "00003", "00004", "00005", "00006", "02439"}) {
	    ki.addDocFile(
	      getClass().getResource("/wiki/" + i + ".json.gz").getFile(), true
            );
	};
	ki.commit();

	String json = getString(getClass().getResource("/queries/bsp-fail1.json").getFile());

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
	for (String i : new String[] {"00001", "00002", "00003", "00004", "00005", "00006", "02439"}) {
	    ki.addDocFile(
	      getClass().getResource("/wiki/" + i + ".json.gz").getFile(), true
            );
	};
	ki.commit();

	String json = getString(getClass().getResource("/queries/bsp-fail2.json").getFile());

	KorapResult kr = new KorapSearch(json).run(ki);
	assertEquals(50, kr.getItemsPerPage());
	assertEquals(49950, kr.getStartIndex());
	assertEquals(0, kr.getTotalResults());
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
