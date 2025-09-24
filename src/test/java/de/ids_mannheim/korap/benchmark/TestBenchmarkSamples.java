package de.ids_mannheim.korap.benchmark;

import java.util.*;
import java.io.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.KrillQuery;
import de.ids_mannheim.korap.KrillCollection;
import de.ids_mannheim.korap.KrillMeta;
import de.ids_mannheim.korap.query.QueryBuilder;
import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.util.QueryException;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;


@RunWith(JUnit4.class)
public class TestBenchmarkSamples {

    private final ObjectMapper mapper = new ObjectMapper();
    private final int rounds = 1000;
    private long t1 = 0, t2 = 0;


    @Test
    public void simpleSegmentQuery () throws Exception {
        // Construct index

        KrillIndex ki = new KrillIndex();
        double seconds;

        // Indexing test files
        for (String i : new String[] { "00001", "00002", "00003", "00004",
                "00005", "00006", "02439" }) {
            ki.addDoc(getClass().getResourceAsStream("/wiki/" + i + ".json.gz"),
                    true);
        };
        ki.commit();

        t1 = System.nanoTime();
        for (int i = 1; i <= rounds; i++) {
            final QueryBuilder qb = new QueryBuilder("tokens");
            final Krill ks = new Krill(qb.seg("mate/m:gender:masc").toQuery());
            final Result kr = ks.apply(ki);
            assertEquals(497, kr.getTotalResults());
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
            assertEquals(497, kr.getTotalResults());
        };
        t2 = System.nanoTime();
        seconds = (double) (t2 - t1) / 1000000000.0;
        System.out.println("No snippets - Seconds: " + seconds);
    };
};
