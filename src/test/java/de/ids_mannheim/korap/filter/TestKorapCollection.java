import java.io.*;

import de.ids_mannheim.korap.KorapIndex;
import de.ids_mannheim.korap.index.FieldDocument;
import de.ids_mannheim.korap.KorapCollection;
import de.ids_mannheim.korap.KorapFilter;
import de.ids_mannheim.korap.KorapResult;
import de.ids_mannheim.korap.KorapQuery;
import de.ids_mannheim.korap.filter.BooleanFilter;
import org.apache.lucene.search.spans.SpanQuery;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestKorapCollection {

    @Test
    public void filterExample () throws IOException {
	
	// Construct index
	KorapIndex ki = new KorapIndex();
	// Indexing test files
	for (String i : new String[] {"00001", "00002", "00003", "00004", "00005", "00006", "02439"}) {
	    ki.addDocFile(
	        getClass().getResource("/wiki/" + i + ".json.gz").getFile(), true
            );
	};
	ki.commit();

	KorapFilter kf = new KorapFilter();

	// Create Virtual collections:
	KorapCollection kc = new KorapCollection(ki);

	// The virtual collection consists of all documents that have the textClass "reisen" and "freizeit"

	assertEquals("Documents", 7, kc.numberOf("documents"));

	kc.filter( kf.and("textClass", "reisen").and("textClass", "freizeit-unterhaltung") );

	assertEquals("Documents", 5, kc.numberOf("documents"));
	assertEquals("Tokens", 1678, kc.numberOf("tokens"));
	assertEquals("Sentences", 194, kc.numberOf("sentences"));
	assertEquals("Paragraphs", 139, kc.numberOf("paragraphs"));

	// Subset this to all documents that have also the text
	kc.filter(kf.and("textClass", "kultur"));

	assertEquals("Documents", 1, kc.numberOf("documents"));
	assertEquals("Tokens", 405, kc.numberOf("tokens"));
	assertEquals("Sentences", 75, kc.numberOf("sentences"));
	assertEquals("Paragraphs", 48, kc.numberOf("paragraphs"));

	kc.filter(kf.and("corpusID", "WPD"));

	assertEquals("Documents", 1, kc.numberOf("documents"));
	assertEquals("Tokens", 405, kc.numberOf("tokens"));
	assertEquals("Sentences", 75, kc.numberOf("sentences"));
	assertEquals("Paragraphs", 48, kc.numberOf("paragraphs"));

	// Create a query
	KorapQuery kq = new KorapQuery("tokens");
	SpanQuery query = kq.seg("opennlp/p:NN").with("tt/p:NN").toQuery();

	KorapResult kr = kc.search(query);

	assertEquals(70, kr.totalResults());

	kc.extend( kf.and("textClass", "uninteresting") );
	assertEquals("Documents", 1, kc.numberOf("documents"));

	kc.extend( kf.and("textClass", "wissenschaft") );

	assertEquals("Documents", 3, kc.numberOf("documents"));
	assertEquals("Tokens", 1669, kc.numberOf("tokens"));
	assertEquals("Sentences", 188, kc.numberOf("sentences"));
	assertEquals("Paragraphs", 130, kc.numberOf("paragraphs"));
	// System.err.println(kr.toJSON());
    };

    @Test
    public void filterExampleAtomic () throws IOException {
	
	// That's exactly the same test class, but with multiple atomic indices

	// Construct index
	KorapIndex ki = new KorapIndex();
	// Indexing test files
	for (String i : new String[] {"00001", "00002", "00003", "00004", "00005", "00006", "02439"}) {
	    ki.addDocFile(
	      getClass().getResource("/wiki/" + i + ".json.gz").getFile(), true
            );
	    ki.commit();
	};

	KorapFilter kf = new KorapFilter();

	// Create Virtual collections:
	KorapCollection kc = new KorapCollection(ki);

	assertEquals("Documents", 7, kc.numberOf("documents"));

	/*
	  If this is set - everything is fine automatically ...
	kc.filter(kf.and("corpusID", "WPD"));
	assertEquals("Documents", 7, kc.numberOf("documents"));
	*/

	// The virtual collection consists of all documents that have the textClass "reisen" and "freizeit"
	kc.filter( kf.and("textClass", "reisen").and("textClass", "freizeit-unterhaltung") );

	assertEquals("Documents", 5, kc.numberOf("documents"));

	assertEquals("Tokens", 1678, kc.numberOf("tokens"));
	assertEquals("Sentences", 194, kc.numberOf("sentences"));
	assertEquals("Paragraphs", 139, kc.numberOf("paragraphs"));

	// Subset this to all documents that have also the text
	kc.filter(kf.and("textClass", "kultur"));

	assertEquals("Documents", 1, kc.numberOf("documents"));
	assertEquals("Tokens", 405, kc.numberOf("tokens"));
	assertEquals("Sentences", 75, kc.numberOf("sentences"));
	assertEquals("Paragraphs", 48, kc.numberOf("paragraphs"));

	kc.filter(kf.and("corpusID", "WPD"));

	assertEquals("Documents", 1, kc.numberOf("documents"));
	assertEquals("Tokens", 405, kc.numberOf("tokens"));
	assertEquals("Sentences", 75, kc.numberOf("sentences"));
	assertEquals("Paragraphs", 48, kc.numberOf("paragraphs"));

	// Create a query
	KorapQuery kq = new KorapQuery("tokens");
	SpanQuery query = kq.seg("opennlp/p:NN").with("tt/p:NN").toQuery();

	KorapResult kr = kc.search(query);
	assertEquals(70, kr.totalResults());

	kc.extend( kf.and("textClass", "uninteresting") );
	assertEquals("Documents", 1, kc.numberOf("documents"));

	kc.extend( kf.and("textClass", "wissenschaft") );

	assertEquals("Documents", 3, kc.numberOf("documents"));
	/*
	assertEquals("Tokens", 1669, kc.numberOf("tokens"));
	assertEquals("Sentences", 188, kc.numberOf("sentences"));
	assertEquals("Paragraphs", 130, kc.numberOf("paragraphs"));
	// System.err.println(kr.toJSON());
	*/
    };



    @Test
    public void filterExample2 () throws IOException {
	
	// Construct index
	KorapIndex ki = new KorapIndex();
	// Indexing test files
	for (String i : new String[] {"00001", "00002", "00003", "00004", "00005", "00006", "02439"}) {
	  ki.addDocFile(
	      getClass().getResource("/wiki/" + i + ".json.gz").getFile(), true
            );
	};
	ki.commit();

	ki.addDocFile(getClass().getResource("/wiki/AUG-55286.json.gz").getFile(), true);

	ki.commit();

	KorapFilter kf = new KorapFilter();

	// Create Virtual collections:
	KorapCollection kc = new KorapCollection(ki);
	kc.filter( kf.and("textClass", "reisen").and("textClass", "freizeit-unterhaltung") );
	assertEquals("Documents", 6, kc.numberOf("documents"));
	assertEquals("Tokens", 2089, kc.numberOf("tokens"));
	assertEquals("Sentences", 234, kc.numberOf("sentences"));
	assertEquals("Paragraphs", 141, kc.numberOf("paragraphs"));

	kc.filter( kf.and("corpusID", "A00") );

	assertEquals("Documents", 1, kc.numberOf("documents"));
	assertEquals("Tokens", 411, kc.numberOf("tokens"));
	assertEquals("Sentences", 40, kc.numberOf("sentences"));
	assertEquals("Paragraphs", 2, kc.numberOf("paragraphs"));

	//	assertEquals("Documents", 1, kc.numberOf("documents"));

	// Create a query
	KorapQuery kq = new KorapQuery("tokens");
	SpanQuery query = kq.seg("opennlp/p:NN").with("tt/p:NN").toQuery();

	KorapResult kr = kc.search(query);

	assertEquals(87, kr.totalResults());
	// System.out.println(kr.toJSON());
    };
};



// kc.filter( kf.and("textClass", "kultur").or("textClass", "wissenschaft") );
