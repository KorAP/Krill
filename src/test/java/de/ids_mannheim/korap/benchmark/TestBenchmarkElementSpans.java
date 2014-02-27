package de.ids_mannheim.korap.benchmark;

import java.util.*;
import java.io.*;

import de.ids_mannheim.korap.KorapIndex;
import de.ids_mannheim.korap.index.FieldDocument;
import de.ids_mannheim.korap.KorapCollection;
import de.ids_mannheim.korap.KorapFilter;
import de.ids_mannheim.korap.KorapSearch;
import de.ids_mannheim.korap.KorapResult;
import de.ids_mannheim.korap.KorapQuery;
import org.apache.lucene.store.MMapDirectory;
import de.ids_mannheim.korap.filter.BooleanFilter;
import org.apache.lucene.search.spans.SpanQuery;
import de.ids_mannheim.korap.query.wrap.SpanQueryWrapperInterface;
import de.ids_mannheim.korap.util.QueryException;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestBenchmarkElementSpans {

    @Test
    public void checkBenchmark1 () throws IOException {
	Properties prop = new Properties();
	InputStream fr = new FileInputStream(getClass().getResource("/korap.conf").getFile());
	prop.load(fr);

	// Get the real index
	KorapIndex ki = new KorapIndex(new MMapDirectory(new File(prop.getProperty("lucene.indexDir"))));

	// Create a container for virtual collections:
	KorapCollection kc = new KorapCollection(ki);

	long t1 = 0, t2 = 0;
	/// cosmas20.json!!!
	String json = getString(getClass().getResource("/queries/benchmark1.jsonld").getFile());

	int rounds = 1000;

	KorapResult kr = new KorapResult();

	t1 = System.nanoTime();
	for (int i = 1; i <= rounds; i++) {
	    kr = new KorapSearch(json).run(ki);
	};
	t2 = System.nanoTime();

	assertEquals("TotalResults", 30751, kr.getTotalResults());

	// System.err.println(kr.toJSON());


	//	long seconds = (long) (t2 - t1 / 1000) % 60 ;
	double seconds = (double)(t2-t1) / 1000000000.0;
	
	System.out.println("It took " + seconds + " seconds");

	// 100 times:
	// 43,538 sec
	// 4.874

	// 1000 times:
	// 36.613 sec
    };


    @Test
    public void checkBenchmark2JSON () throws IOException {
	Properties prop = new Properties();
	InputStream fr = new FileInputStream(getClass().getResource("/korap.conf").getFile());
	prop.load(fr);

	// Get the real index
	KorapIndex ki = new KorapIndex(new MMapDirectory(new File(prop.getProperty("lucene.indexDir"))));

	// Create a container for virtual collections:
	KorapCollection kc = new KorapCollection(ki);

	long t1 = 0, t2 = 0;
	/// cosmas20.json!!!
	String json = getString(getClass().getResource("/queries/benchmark2.jsonld").getFile());

	int rounds = 10000;

	KorapResult kr = new KorapResult();
	String result = new String("");

	t1 = System.nanoTime();
	double length = 0;
	for (int i = 1; i <= rounds; i++) {
	    kr = new KorapSearch(json).run(ki);
	    length += kr.toJSON().length();
	};
	t2 = System.nanoTime();

	//	assertEquals("TotalResults", 30751, kr.getTotalResults());

	// System.err.println(kr.toJSON());

	//	long seconds = (long) (t2 - t1 / 1000) % 60 ;
	double seconds = (double)(t2-t1) / 1000000000.0;
	
	System.out.println("It took " + seconds + " seconds");

	// 10000 times:
	//  77.167124985 sec
    };


    @Test
    public void checkBenchmarkSentences () throws IOException {
	Properties prop = new Properties();
	InputStream fr = new FileInputStream(getClass().getResource("/korap.conf").getFile());
	prop.load(fr);

	// Get the real index
	KorapIndex ki = new KorapIndex(new MMapDirectory(new File(prop.getProperty("lucene.indexDir"))));

	// Create a container for virtual collections:
	KorapCollection kc = new KorapCollection(ki);

	long t1 = 0, t2 = 0;
	/// cosmas20.json!!!
	String json = getString(getClass().getResource("/queries/benchmark4.jsonld").getFile());

	int rounds = 10;

	KorapResult kr = new KorapResult();

	t1 = System.nanoTime();
	double length = 0;
	for (int i = 1; i <= rounds; i++) {
	    kr = new KorapSearch(json).run(ki);
	};
	t2 = System.nanoTime();

	System.err.println(kr.getMatch(0).toJSON());

	assertEquals("TotalResults1", 4116282, kr.getTotalResults());
	assertEquals("TotalResults2", kr.getTotalResults(), ki.numberOf("sentences"));

	double seconds = (double)(t2-t1) / 1000000000.0;
	
	System.out.println("It took " + seconds + " seconds");
    };

    
    @Test
    public void checkBenchmarkIndexDocuments () throws IOException {
	long t1 = 0, t2 = 0;

	int rounds = 10;

	ArrayList<String> docs = new ArrayList<String>(700);

	for (int a = 0; a < 50; a++) {
	    for (String d : new String[] {"00001", "00002", "00003",
				   "00004", "00005", "00006", "02439"}) {
		docs.add(d);
	    };
	};

	t1 = System.nanoTime();
	double length = 0;
	for (int i = 1; i <= rounds; i++) {
	    // Construct index
	    KorapIndex ki = new KorapIndex();

	    // Indexing test files
	    for (String d : docs) {
		FieldDocument fd = ki.addDocFile(
		    getClass().getResource("/wiki/" + d + ".json.gz").getFile(),
		    true
		);
	    };
	    ki.commit();
	};
	t2 = System.nanoTime();

	double seconds = (double)(t2-t1) / 1000000000.0;
	System.out.println("It took " + seconds + " seconds");

	// 10 times / 350 docs:
	// 36.26158006 seconds
	// 32.52575097 seconds
	// 31.818091536 seconds
	// 32.055321123 seconds
	// 32.32125959 seconds
	// 31.726277979 seconds
	// 31.65826188 seconds
	// 31.287057537 seconds
    };


    @Test
    public void checkBenchmark3 () throws IOException {
	Properties prop = new Properties();
	InputStream fr = new FileInputStream(getClass().getResource("/korap.conf").getFile());
	prop.load(fr);

	// Get the real index
	KorapIndex ki = new KorapIndex(new MMapDirectory(new File(prop.getProperty("lucene.indexDir"))));

	// Create a container for virtual collections:
	KorapCollection kc = new KorapCollection(ki);

	long t1 = 0, t2 = 0;
	/// cosmas20.json!!!
	String json = getString(getClass().getResource("/queries/benchmark3.jsonld").getFile());

	int rounds = 500;

	KorapResult kr = new KorapResult();

	t1 = System.nanoTime();
	for (int i = 1; i <= rounds; i++) {
	    kr = new KorapSearch(json).run(ki);
	};
	t2 = System.nanoTime();

	assertEquals("TotalResults", 70229, kr.getTotalResults());

	// System.err.println(kr.toJSON());

	//	long seconds = (long) (t2 - t1 / 1000) % 60 ;
	double seconds = (double)(t2-t1) / 1000000000.0;
	
	System.out.println("It took " + seconds + " seconds");

	// 500 times:
	// 71.715862716 seconds
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

    public static SpanQueryWrapperInterface jsonQuery (String jsonFile) {
	SpanQueryWrapperInterface sqwi;
	
	try {
	    String json = getString(jsonFile);
	    sqwi = new KorapQuery("tokens").fromJSON(json);
	}
	catch (QueryException e) {
	    fail(e.getMessage());
	    sqwi = new KorapQuery("tokens").seg("???");
	};
	return sqwi;
    };
    
};
