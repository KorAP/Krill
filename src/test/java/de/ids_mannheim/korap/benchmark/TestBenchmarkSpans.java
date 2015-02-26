package de.ids_mannheim.korap.benchmark;

import java.util.*;
import java.io.*;

import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.index.FieldDocument;
import de.ids_mannheim.korap.KorapCollection;
import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.KorapResult;
import de.ids_mannheim.korap.KrillQuery;
import de.ids_mannheim.korap.query.QueryBuilder;
import org.apache.lucene.store.MMapDirectory;
import de.ids_mannheim.korap.collection.BooleanFilter;
import org.apache.lucene.search.spans.SpanQuery;
import de.ids_mannheim.korap.query.wrap.SpanQueryWrapper;
import de.ids_mannheim.korap.util.QueryException;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestBenchmarkSpans {

    @Test
    public void checkBenchmark1 () throws IOException {
        Properties prop = new Properties();
        InputStream fr = new FileInputStream(getClass().getResource("/korap.conf").getFile());
        prop.load(fr);

        // Get the real index
        KrillIndex ki = new KrillIndex(new MMapDirectory(new File(prop.getProperty("lucene.indexDir"))));

        // Create a container for virtual collections:
        KorapCollection kc = new KorapCollection(ki);

        long t1 = 0, t2 = 0;
        /// cosmas20.json!!!
        String json = getString(getClass().getResource("/queries/benchmark1.jsonld").getFile());

        int rounds = 100;

        KorapResult kr = new KorapResult();

        t1 = System.nanoTime();
        for (int i = 1; i <= rounds; i++) {
            kr = new Krill(json).apply(ki);
        };
        t2 = System.nanoTime();

        // assertEquals("TotalResults", 30751, kr.getTotalResults());
        assertEquals("TotalResults",  kr.getTotalResults(), 4803739);

        //	long seconds = (long) (t2 - t1 / 1000) % 60 ;
        double seconds = (double)(t2-t1) / 1000000000.0;
	
        // System.out.println("It took " + seconds + " seconds");

        // 100 times:
        // 43,538 sec
        // 4.874
        
        // 1000 times:
        // 36.613 sec

        // After refactoring
        // 100 times
        // 273.58114372 seconds
        
        // After intro of attributes
        // 100 times
        // 350.171506379 seconds
    };


    @Test
    public void checkBenchmark2JSON () throws IOException {
        Properties prop = new Properties();
        InputStream fr = new FileInputStream(getClass().getResource("/korap.conf").getFile());
        prop.load(fr);

        // Get the real index
        KrillIndex ki = new KrillIndex(new MMapDirectory(new File(prop.getProperty("lucene.indexDir"))));
        
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
            kr = new Krill(json).apply(ki);
            length += kr.toJsonString().length();
        };
        t2 = System.nanoTime();

        //	assertEquals("TotalResults", 30751, kr.getTotalResults());

        // System.err.println(kr.toJSON());

        //	long seconds = (long) (t2 - t1 / 1000) % 60 ;
        double seconds = (double)(t2-t1) / 1000000000.0;
	
        // System.out.println("It took " + seconds + " seconds");

        // 10000 times:
        //  77.167124985 sec
    };


    @Test
    public void checkBenchmarkSentences () throws IOException {
        Properties prop = new Properties();
        InputStream fr = new FileInputStream(getClass().getResource("/korap.conf").getFile());
        prop.load(fr);

        // Get the real index
        KrillIndex ki = new KrillIndex(new MMapDirectory(new File(prop.getProperty("lucene.indexDir"))));

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
            kr = new Krill(json).apply(ki);
        };
        t2 = System.nanoTime();

        // System.err.println(kr.getMatch(0).toJSON());
        
        assertEquals("TotalResults1", kr.getTotalResults(), 4116282);
        assertEquals("TotalResults2", kr.getTotalResults(), ki.numberOf("sentences"));

        double seconds = (double)(t2-t1) / 1000000000.0;
        
        // System.out.println("It took " + seconds + " seconds");
        // 100 rounds
        // 56.253 secs
    };

    
    @Test
    public void checkBenchmarkClasses () throws IOException {
        // [orth=Der]{1:[orth=Mann]{2:[orth=und]}}

        Properties prop = new Properties();
        InputStream fr = new FileInputStream(getClass().getResource("/korap.conf").getFile());
        prop.load(fr);

        // Get the real index
        KrillIndex ki = new KrillIndex(new MMapDirectory(new File(prop.getProperty("lucene.indexDir"))));

        // Create a container for virtual collections:
        KorapCollection kc = new KorapCollection(ki);

        long t1 = 0, t2 = 0;
        // Without classes
        String json = getString(getClass().getResource("/queries/benchmark5-ohne.jsonld").getFile());

        int rounds = 2000;
        
        KorapResult kr = new KorapResult();

        t1 = System.nanoTime();
        for (int i = 1; i <= rounds; i++) {
            kr = new Krill(json).apply(ki);
        };
        t2 = System.nanoTime();

        double seconds = (double)(t2-t1) / 1000000000.0;
        
        // System.out.println("It took " + seconds + " seconds without classes");

        t1 = 0;
        t2 = 0;
        // With classes
        json = getString(getClass().getResource("/queries/benchmark5.jsonld").getFile());
        
        t1 = System.nanoTime();
        for (int i = 1; i <= rounds; i++) {
            kr = new Krill(json).apply(ki);
        };
        t2 = System.nanoTime();

        seconds = (double)(t2-t1) / 1000000000.0;
        
        // System.out.println("It took " + seconds + " seconds with classes");

        t1 = 0;
        t2 = 0;
        // With submatch
        json = getString(getClass().getResource("/queries/benchmark5-submatch.jsonld").getFile());

        t1 = System.nanoTime();
        for (int i = 1; i <= rounds; i++) {
            kr = new Krill(json).apply(ki);
        };
        t2 = System.nanoTime();

        seconds = (double)(t2-t1) / 1000000000.0;
        
        // System.out.println("It took " + seconds + " seconds with submatches");

        /** HERE IS A BUG! */
        
        // System.err.println(kr.toJsonString());

        // System.err.println(kr.toJSON());

        // System.err.println(kr.getMatch(3).getSnippetBrackets());


        // 2000 rounds:
        // It took 10.872934435 seconds without classes
        // It took 22.581117396 seconds with classes

        // It took 10.703933598 seconds without classes
        // It took 19.354674517 seconds with classes

        // It took 10.939948726 seconds without classes
        // It took 16.998470662 seconds with classes

        // It took 10.900975837 seconds without classes
        // It took 14.902590949 seconds with classes

        // It took 10.365989238 seconds without classes
        // It took 13.833405885 seconds with classes

        // It took 15.368675425 seconds without classes
        // It took 18.347603186 seconds with classes
        // It took 15.941057294 seconds with submatches
        
        // It took 15.241253549 seconds without classes
        // It took 17.30375624 seconds with classes
        // It took 15.367171254 seconds with submatches
    };

    @Test
    public void checkBenchmarkIndexDocuments () throws IOException {
        long t1 = 0, t2 = 0;

        int rounds = 10;

        ArrayList<String> docs = new ArrayList<String>(700);

        for (int a = 0; a < 50; a++) {
            for (String d : new String[] {
                    "00001",
                    "00002",
                    "00003",
                    "00004",
                    "00005",
                    "00006",
                    "02439"}) {
                docs.add(d);
            };
        };

        t1 = System.nanoTime();
        double length = 0;
        for (int i = 1; i <= rounds; i++) {
            // Construct index
            KrillIndex ki = new KrillIndex();
            
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
        // System.out.println("It took " + seconds + " seconds");

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
        KrillIndex ki = new KrillIndex(new MMapDirectory(new File(prop.getProperty("lucene.indexDir"))));

        // Create a container for virtual collections:
        KorapCollection kc = new KorapCollection(ki);

        long t1 = 0, t2 = 0;
        /// cosmas20.json!!!
        String json = getString(getClass().getResource("/queries/benchmark3.jsonld").getFile());

        int rounds = 500;
    
        KorapResult kr = new KorapResult();

        t1 = System.nanoTime();
        for (int i = 1; i <= rounds; i++) {
            kr = new Krill(json).apply(ki);
        };
        t2 = System.nanoTime();

        assertEquals("TotalResults", kr.getTotalResults(), 70229);

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
    
    public static SpanQueryWrapper jsonQuery (String jsonFile) {
        SpanQueryWrapper sqwi;
	
        try {
            String json = getString(jsonFile);
            sqwi = new KrillQuery("tokens").fromJson(json);
        }
        catch (QueryException e) {
            fail(e.getMessage());
            sqwi = new QueryBuilder("tokens").seg("???");
        };
        return sqwi;
    };
};
