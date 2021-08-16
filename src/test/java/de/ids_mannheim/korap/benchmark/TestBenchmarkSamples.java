package de.ids_mannheim.korap.benchmark;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.KrillMeta;
import de.ids_mannheim.korap.query.QueryBuilder;
import de.ids_mannheim.korap.response.Result;


@RunWith(JUnit4.class)
public class TestBenchmarkSamples {

    private final ObjectMapper mapper = new ObjectMapper();
    private final int rounds = 1000;
    private long t1 = 0, t2 = 0;
    private double duration;
    
    @Test
    public void testSnippetAndAvailabilityAll() throws IOException {
    	KrillIndex ki = new KrillIndex();
    	ki.addDoc(getClass().getResourceAsStream("/goe/AGA-03828.json.gz"),
                true);
    	ki.addDoc(getClass().getResourceAsStream("/bzk/D59-00089.json.gz"),
                true);
    	ki.commit();
    	
    	String json = TestBenchmarkSpans.getString(
                getClass().getResource("/queries/benchmark6-availability.jsonld").getFile());

    	// with snippet
    	
    	Krill ks = new Krill(json);
    	Result kr1 = new Result();
    	
    	ks.getMeta().setSnippets(false);
    	for (int i=0; i<10;i++) {
    		t1=System.nanoTime();
	    	kr1 = ks.apply(ki);
	    	t2=System.nanoTime();
	    	duration = (double) (t2 - t1)/ 1000000000.0;;
	        System.out.println("no snippet: "+ duration);
    	}
        
    	// without snippet
    	Result kr2 = new Result();
        ks.getMeta().setSnippets(true);
        for (int i=0; i<10;i++) {
	        t1=System.nanoTime();
	        kr2 = ks.apply(ki);
	    	t2=System.nanoTime();
	    	duration = (double) (t2 - t1)/ 1000000000.0;;
	        System.out.println("with snippet: "+duration);
        }
        assertEquals(kr1.getTotalResults(),kr2.getTotalResults());
        System.out.println("Total Results: "+ kr1.getTotalResults());
    	
	}

    @Test
    public void simpleSegmentQuery () throws Exception {
    	// Construct index
    	KrillIndex ki = new KrillIndex();
        // Indexing test files
        for (String i : new String[] { "00001", "00002", "00003", "00004",
                "00005", "00006", "02439" }) {
            ki.addDoc(getClass().getResourceAsStream("/wiki/" + i + ".json.gz"),
                    true);
        };
        ki.commit();
        
        double seconds;
        t1 = System.nanoTime();
        for (int i = 1; i <= rounds; i++) {
            final QueryBuilder qb = new QueryBuilder("tokens");
            final Krill ks = new Krill(qb.seg("mate/m:gender:masc").toQuery());
            final Result kr = ks.apply(ki);
            assertEquals(kr.getTotalResults(), 497);
        };
        t2 = System.nanoTime();
        seconds = (double) (t2 - t1) / 1000000000.0;
        System.out.println("Seconds: " + seconds);

        // Seconds: 9.465514311
        // Seconds: 9.302011468
        // Seconds: 9.052496918
        // Seconds: 9.0567007
        // Seconds: 9.113724089
        // Seconds: 8.700548842
        // Seconds: 9.390980437
        // Seconds: 8.817503952
        // New machine (ND):
        // Seconds: 3.679194927
        // EM
        // Seconds: 3.176065455
        // No snippets - Seconds: 1.620091942

        t1 = System.nanoTime();
        for (int i = 1; i <= rounds; i++) {
            final QueryBuilder qb = new QueryBuilder("tokens");
            final Krill ks = new Krill(qb.seg("mate/m:gender:masc").toQuery());
            KrillMeta meta = ks.getMeta();
            meta.setSnippets(false);
            final Result kr = ks.apply(ki);
            assertEquals(kr.getTotalResults(), 497);
        };
        t2 = System.nanoTime();
        seconds = (double) (t2 - t1) / 1000000000.0;
        System.out.println("No snippets - Seconds: " + seconds);
    };
};
