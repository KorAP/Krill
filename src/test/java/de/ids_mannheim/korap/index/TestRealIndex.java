import java.util.*;
import java.io.*;

import de.ids_mannheim.korap.KorapIndex;
import de.ids_mannheim.korap.index.FieldDocument;
import de.ids_mannheim.korap.KorapCollection;
import de.ids_mannheim.korap.KorapFilter;
import de.ids_mannheim.korap.KorapResult;
import de.ids_mannheim.korap.KorapQuery;
import org.apache.lucene.store.MMapDirectory;
import de.ids_mannheim.korap.filter.BooleanFilter;
import org.apache.lucene.search.spans.SpanQuery;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestRealIndex {

    @Test
    public void realExample1 () throws IOException {

	// Load configuration file
	Properties prop = new Properties();
	FileReader fr = new FileReader(getClass().getResource("/korap.conf").getFile());
	prop.load(fr);

	// Check if the configuration was loaded fine
	assertEquals(prop.getProperty("lucene.properties"), "true");

	String indexDir = prop.getProperty("lucene.index");

	// Get the real index
	KorapIndex ki = new KorapIndex(new MMapDirectory(new File(indexDir)));

	// Create a container for virtual collections:
	KorapCollection kc = new KorapCollection(ki);

	// Construct filter generator
	KorapFilter kf = new KorapFilter();

	// The virtual collection consists of all documents that have
	// the textClasses "reisen" and "freizeit"
	//	kc.filter( kf.and("textClass", "reisen").and("textClass", "freizeit-unterhaltung") );

	// This is real slow atm - sorry
	kc.filter(kf.and("textClass", "kultur"));
	// kc.filter(kf.and("ID", "A00_JAN.02873"));


	// Create a query
	KorapQuery kq = new KorapQuery("tokens");

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


	KorapResult kr = kc.search(query);

	System.err.println(kr.toJSON());
    };
};