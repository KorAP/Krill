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
    public void checkspans1 () throws IOException {
	Properties prop = new Properties();
	InputStream fr = new FileInputStream(getClass().getResource("/korap.conf").getFile());
	prop.load(fr);

	// Get the real index
	KorapIndex ki = new KorapIndex(new MMapDirectory(new File(prop.getProperty("lucene.indexDir"))));

	// Create a container for virtual collections:
	KorapCollection kc = new KorapCollection(ki);

	// Construct filter generator
	// KorapFilter kf = new KorapFilter();

	// The virtual collection consists of all documents that have
	// the textClasses "reisen" and "freizeit"
	//	kc.filter( kf.and("textClass", "reisen").and("textClass", "freizeit-unterhaltung") );

	// This is real slow atm - sorry
	// kc.filter(kf.and("textClass", "kultur"));


	// Create a query
	// KorapQuery kq = new KorapQuery("tokens");

	long t1 = 0, t2 = 0;
	t1 = System.nanoTime();

	String json = getString(getClass().getResource("/queries/bsp19.jsonld").getFile());

	int rounds = 1;

	for (int i = 1; i <= rounds; i++) {
	    /*
	    SpanQuery query =
		kq.within(
			  kq.tag("xip/const:NPA"),
			  kq._(1,
			       kq.seq(
				      kq._(2, kq.seg("cnx/p:A").with("mate/m:number:sg"))
				      ).append(
					       kq.seg("opennlp/p:NN").with("tt/p:NN")
					       )
			       )
			  ).toQuery();
	    */
	    // SpanQuery query = kq.tag("s").toQuery();

	    KorapResult kr = new KorapSearch(json).run(ki);
	    System.err.println(kr.toJSON());
	};

	t2 = System.nanoTime();
	
	System.out.println("It took " + ((t2 - t1) / rounds) * 1e-6 + " ms per query");



	//	kc = new KorapCollection("{\"meta\":[{\"@type\":\"korap:meta-filter\",\"@value\":{\"@type\":\"korap:term\",\"@field\":\"korap:field#corpusID\",\"@value\":\"A00\"}},{\"@type\":\"korap:meta-extend\",\"@value\":{\"@type\":\"korap:term\",\"@field\":\"korap:field#corpusID\",\"@value\":\"A01\"}}]}");

	//	kc = new KorapCollection("{\"meta\":[{\"@type\":\"korap:meta-filter\",\"@value\":{\"@type\":\"korap:term\",\"@field\":\"korap:field#corpusID\",\"@value\":\"A01\"}}]}");
	/*
	kc = new KorapCollection("{\"meta\":[{\"@type\":\"korap:meta-filter\",\"@value\":{\"@type\":\"korap:term\",\"@field\":\"korap:field#textClass\",\"@value\":\"reisen\"}}]}");
	kc.setIndex(ki);

	System.err.println(kc.getFilter(0).toString());
	System.err.println(kc.numberOf("documents"));

	*/

	// assertEquals(14, kc.numberOf("documents"));
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
