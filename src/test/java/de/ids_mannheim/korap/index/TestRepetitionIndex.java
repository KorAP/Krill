package de.ids_mannheim.korap.index;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.*;
import java.util.regex.*;

import static de.ids_mannheim.korap.TestSimple.getJsonString;
import static de.ids_mannheim.korap.TestSimple.simpleFieldDoc;
import static de.ids_mannheim.korap.TestSimple.simpleFuzzyFieldDoc;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.junit.Test;
import org.junit.Ignore;

import de.ids_mannheim.korap.query.QueryBuilder;
import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.query.SpanNextQuery;
import de.ids_mannheim.korap.query.SpanRepetitionQuery;
import de.ids_mannheim.korap.response.Match;
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.util.QueryException;

public class TestRepetitionIndex {

    private KrillIndex ki;
    private Result kr;
    private FieldDocument fd;

    private FieldDocument createFieldDoc0 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-0");
        fd.addTV("base", "text",
                "[(0-1)s:c|_1$<i>0<i>1]" + "[(1-2)s:e|_2$<i>1<i>2]"
                        + "[(2-3)s:c|_3$<i>2<i>3|<>:y$<b>64<i>2<i>4<i>4<b>0]"
                        + "[(3-4)s:c|s:b|_4$<i>3<i>4|<>:x$<b>64<i>3<i>7<i>7<b>0]"
                        + "[(4-5)s:e|s:d|_5$<i>4<i>5|<>:y$<b>64<i>4<i>6<i>6<b>0]"
                        + "[(5-6)s:c|_6$<i>5<i>6|<>:y$<b>64<i>5<i>8<i>8]"
                        + "[(6-7)s:d|_7$<i>6<i>7<b>0]"
                        + "[(7-8)s:e|_8$<i>7<i>8|<>:x$<b>64<i>7<i>9<i>9<b>0]"
                        + "[(8-9)s:e|s:b|_9$<i>8<i>9|<>:x$<b>64<i>8<i>10<i>10<b>0]"
                        + "[(9-10)s:d|_10$<i>9<i>10]");
        return fd;
    }


    private FieldDocument createFieldDoc1 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-1");
        fd.addTV("base", "text", "[(0-1)s:b|_1$<i>0<i>1]"
                + "[(1-2)s:e|_2$<i>1<i>2]" + "[(2-3)s:c|_3$<i>2<i>3]"
                + "[(3-4)s:c|s:d]" + "[(4-5)s:d|s:c|_5$<i>4<i>5]"
                + "[(5-6)s:e|s:c|_6$<i>5<i>6]" + "[(6-7)s:e|_7$<i>6<i>7]"
                + "[(7-8)s:c|_8$<i>7<i>8]" + "[(8-9)s:d|_9$<i>8<i>9]"
                + "[(9-10)s:d|_10$<i>9<i>10]");
        return fd;
    }


    private FieldDocument createFieldDoc2 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-2");
        fd.addTV("base", "text",
                "[(0-1)s:b|s:c|_1$<i>0<i>1|<>:s$<b>64<i>0<i>2<i>1<b>0]"
                        + "[(1-2)s:c|_2$<i>1<i>2]"
                        + "[(2-3)s:b|_3$<i>2<i>3|<>:s$<b>64<i>2<i>3<i>3<b>0]"
                        + "[(3-4)s:c|_4$<i>3<i>4|<>:s$<b>64<i>3<i>4<i>4<b>0]"
                        + "[(4-5)s:c|_5$<i>4<i>5|<>:s$<b>64<i>4<i>5<i>5]"
                        + "[(5-6)s:b|_6$<i>5<i>6<b>0]"
                        + "[(6-7)s:c|_7$<i>6<i>7|<>:s$<b>64<i>6<i>7<i>7<b>0]");
        return fd;
    }


    private FieldDocument createFieldDoc3 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-3");
        fd.addTV("base", "text",
                "[(0-1)s:a|_1$<i>0<i>1|<>:s$<b>64<i>0<i>2<i>1<b>0]"
                        + "[(1-2)s:d|_2$<i>1<i>2|<>:s$<b>64<i>1<i>2<i>3]"
                        + "[(2-3)s:e|_3$<i>2<i>3<b>0]");
        return fd;
    }


    @Test
    public void testTermQuery () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc0());
        ki.commit();

        // Quantifier only
        // c{1,2}
        SpanQuery sq = new SpanRepetitionQuery(
                new SpanTermQuery(new Term("base", "s:c")), 1, 2, true);
        kr = ki.search(sq, (short) 10);
        // 0-1, 2-3, 2-4, 3-4, 5-6
        assertEquals((long) 5, kr.getTotalResults());
        assertEquals(0, kr.getMatch(0).getStartPos());
        assertEquals(1, kr.getMatch(0).getEndPos());
        assertEquals(2, kr.getMatch(1).getStartPos());
        assertEquals(3, kr.getMatch(1).getEndPos());
        assertEquals(2, kr.getMatch(2).getStartPos());
        assertEquals(4, kr.getMatch(2).getEndPos());
        assertEquals(3, kr.getMatch(3).getStartPos());
        assertEquals(4, kr.getMatch(3).getEndPos());
        assertEquals(5, kr.getMatch(4).getStartPos());
        assertEquals(6, kr.getMatch(4).getEndPos());
    }


    @Test
    public void testRepetitionInSequences () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc0());
        ki.commit();

        SpanQuery sq, sq2;
        // ec{1,2}
        sq = new SpanNextQuery(new SpanTermQuery(new Term("base", "s:e")),
                new SpanRepetitionQuery(
                        new SpanTermQuery(new Term("base", "s:c")), 1, 2,
                        true));

        kr = ki.search(sq, (short) 10);
        // 1-3, 1-4, 4-6
        assertEquals((long) 3, kr.getTotalResults());
        assertEquals(1, kr.getMatch(0).getStartPos());
        assertEquals(3, kr.getMatch(0).getEndPos());
        assertEquals(1, kr.getMatch(1).getStartPos());
        assertEquals(4, kr.getMatch(1).getEndPos());
        assertEquals(4, kr.getMatch(2).getStartPos());
        assertEquals(6, kr.getMatch(2).getEndPos());

        // ec{1,2}d
        sq2 = new SpanNextQuery(sq, new SpanTermQuery(new Term("base", "s:d")));
        kr = ki.search(sq2, (short) 10);
        assertEquals((long) 2, kr.getTotalResults());
        assertEquals(1, kr.getMatch(0).startPos);
        assertEquals(5, kr.getMatch(0).endPos);
        assertEquals(4, kr.getMatch(1).startPos);
        assertEquals(7, kr.getMatch(1).endPos);

        // Multiple documents        
        ki.addDoc(createFieldDoc1());
        ki.commit();
        kr = ki.search(sq2, (short) 10);
        assertEquals((long) 5, kr.getTotalResults());
    }


    @Test
    public void testMinZeroRepetition () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc0());
        ki.commit();

        SpanQuery sq, sq2;
        sq = new SpanTermQuery(new Term("base", "s:e"));
        kr = ki.search(sq, (short) 10);

        assertEquals((long) 4, kr.getTotalResults());
        assertEquals(1, kr.getMatch(0).getStartPos());
        assertEquals(2, kr.getMatch(0).getEndPos());
        assertEquals(4, kr.getMatch(1).getStartPos());
        assertEquals(5, kr.getMatch(1).getEndPos());
        assertEquals(7, kr.getMatch(2).getStartPos());
        assertEquals(8, kr.getMatch(2).getEndPos());
        assertEquals(8, kr.getMatch(3).getStartPos());
        assertEquals(9, kr.getMatch(3).getEndPos());
        try {
            sq2 = new SpanNextQuery(sq, new SpanRepetitionQuery(
                    new SpanTermQuery(new Term("base", "s:c")), 0, 1, true));
        }
        catch (IllegalArgumentException e) {
            assertEquals("Minimum repetition must not lower than 1.",
                    e.getMessage());
        }
    }


    /** Skip to */
    @Test
    public void testCase2 () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc0());
        ki.addDoc(createFieldDoc3());
        ki.addDoc(createFieldDoc2());
        ki.addDoc(createFieldDoc1());
        ki.commit();

        SpanQuery sq;
        // c{2,2}
        // sq = new SpanRepetitionQuery(
        // new SpanTermQuery(new Term("base", "s:c")), 2, 2, true);
        // kr = ki.search(sq, (short) 10);
        // // doc1 2-4, 3-5, 4-6
        // assertEquals((long) 6, kr.getTotalResults());

        // ec{2,2}
        sq = new SpanNextQuery(new SpanTermQuery(new Term("base", "s:e")),
                new SpanRepetitionQuery(
                        new SpanTermQuery(new Term("base", "s:c")), 2, 2,
                        true));

        kr = ki.search(sq, (short) 10);
        assertEquals((long) 2, kr.getTotalResults());
        assertEquals(3, kr.getMatch(1).getLocalDocID());

    }


    /** OR */
    @Test
    public void testCase3 () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc0());
        ki.commit();

        SpanQuery sq, sq2;
        // ec{1,2}
        sq = new SpanNextQuery(new SpanTermQuery(new Term("base", "s:e")),
                new SpanOrQuery(new SpanRepetitionQuery(
                        new SpanTermQuery(new Term("base", "s:c")), 1, 1, true),
                        new SpanRepetitionQuery(
                                new SpanTermQuery(new Term("base", "s:b")), 1,
                                1, true)));
        kr = ki.search(sq, (short) 10);
        assertEquals((long) 3, kr.getTotalResults());
        assertEquals(1, kr.getMatch(0).startPos);
        assertEquals(3, kr.getMatch(0).endPos);
        assertEquals(4, kr.getMatch(1).startPos);
        assertEquals(6, kr.getMatch(1).endPos);
        assertEquals(7, kr.getMatch(2).startPos);
        assertEquals(9, kr.getMatch(2).endPos);

    }


    @Test
    public void testCase4 () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc1());
        ki.commit();

        SpanQuery sq;
        // c{2,2}
        sq = new SpanRepetitionQuery(new SpanTermQuery(new Term("base", "s:c")),
                1, 3, true);
        kr = ki.search(sq, (short) 10);
        // 2-3, 2-4, 2-5, 3-4, 3-5, 3-6, 4-5, 4-6, 5-6, 7-8  
        assertEquals((long) 10, kr.getTotalResults());

        sq = new SpanRepetitionQuery(new SpanTermQuery(new Term("base", "s:c")),
                2, 3, true);
        kr = ki.search(sq, (short) 10);
        // 2-4, 2-5, 3-5, 3-6, 4-6 
        assertEquals((long) 5, kr.getTotalResults());
    }


    @Test
    public void testCase5 () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(getClass().getResourceAsStream("/wiki/00001.json.gz"), true);
        ki.commit();

        SpanQuery sq0, sq1, sq2;
        sq0 = new SpanTermQuery(new Term("tokens", "tt/p:NN"));
        sq1 = new SpanRepetitionQuery(
                new SpanTermQuery(new Term("tokens", "tt/p:ADJA")), 2, 3, true);
        sq2 = new SpanNextQuery(sq1, sq0);
        kr = ki.search(sq2, (short) 10);

        assertEquals((long) 2, kr.getTotalResults());
        assertEquals(73, kr.getMatch(0).getStartPos());
        assertEquals(77, kr.getMatch(0).getEndPos());
        assertEquals(74, kr.getMatch(1).getStartPos());
        assertEquals(77, kr.getMatch(1).getEndPos());


        sq2 = new SpanNextQuery(
                new SpanTermQuery(new Term("tokens", "s:offenen")), sq2);
        kr = ki.search(sq2, (short) 10);

        assertEquals((long) 1, kr.getTotalResults());
        assertEquals(73, kr.getMatch(0).getStartPos());
        assertEquals(77, kr.getMatch(0).getEndPos());
        /*
        for (Match km : kr.getMatches()){
        	System.out.println(km.getSnippetBrackets());
        	System.out.println(km.getStartPos() +","+km.getEndPos());
        }*/
    };

    @Test
    public void testRepetitionSnippetBug1 () throws IOException, QueryException {
        // Construct index
        Pattern p = Pattern.compile("bccc?d");
        
        // Der [corenlp/p=ADJA]{2,3} Baum

        QueryBuilder qb = new QueryBuilder("base");

        // b c{2,3} d
        SpanQuery sq = qb.seq(
            qb.seg("s:b")
            ).append(
                qb.repeat(qb.seg("s:c"),2,3)
                ).append(
                    qb.seg("s:d")
                    ).toQuery();
        
        Krill ks = new Krill(sq);

        assertEquals(
            "spanNext(spanNext(base:s:b, spanRepetition(base:s:c{2,3})), base:s:d)",
            ks.getSpanQuery().toString());

        // simpleDocTest
        KrillIndex ki = new KrillIndex();
        ki.addDoc(simpleFieldDoc("abccde"));
        ki.commit();
        Result kr = ks.apply(ki);
        assertEquals(1,kr.getTotalResults());

        // fuzzingRepetitionBug();

        // First fuzzed failure (0 vs 1)
        ki = new KrillIndex();
        ki.addDoc(simpleFieldDoc("cccd"));        // 0
        ki.addDoc(simpleFieldDoc("bccccccaeae")); // 1
        ki.addDoc(simpleFieldDoc("cbcedb"));      // 2

        ki.commit();
        kr = ks.apply(ki);
        assertEquals(0,kr.getTotalResults());

        // Third fuzzed failure (1 vs 2)
        ki = new KrillIndex();
        ki.addDoc(simpleFieldDoc("bccdcb"));
        ki.addDoc(simpleFieldDoc("ebccce"));
        ki.addDoc(simpleFieldDoc("adbdcd"));
        
        ki.commit();
        kr = ks.apply(ki);
        assertEquals(1,kr.getTotalResults());
    };

    @Test
    public void testRepetitionSnippetBug2 () throws IOException, QueryException {
        // Construct index
        Pattern p = Pattern.compile("bccc?d");
        
        QueryBuilder qb = new QueryBuilder("base");

        // b c{2,3} d
        SpanQuery sq = qb.seq(
            qb.seg("s:b")
            ).append(
                qb.repeat(qb.seg("s:c"),2,3)
                ).append(
                    qb.seg("s:d")
                    ).toQuery();
        
        Krill ks = new Krill(sq);

        assertEquals(
            "spanNext(spanNext(base:s:b, spanRepetition(base:s:c{2,3})), base:s:d)",
            ks.getSpanQuery().toString());

        // fuzzingRepetitionBug();

        // Second fuzzed failure (1 vs 0)
        ki = new KrillIndex();
        ki.addDoc(simpleFieldDoc("cdddbc"));
        ki.addDoc(simpleFieldDoc("bccc"));
        ki.addDoc(simpleFieldDoc("cbcccd"));

        ki.commit();
        kr = ks.apply(ki);
        assertEquals(1,kr.getTotalResults());
    };

    @Test
    public void testRepetitionSnippetBug3 () throws IOException, QueryException {
        // Construct index
        Pattern p = Pattern.compile("bccc?d");
        
        QueryBuilder qb = new QueryBuilder("base");

        // b c{2,3} d
        SpanQuery sq = qb.seq(
            qb.seg("s:b")
            ).append(
                qb.repeat(qb.seg("s:c"),2,3)
                ).append(
                    qb.seg("s:d")
                    ).toQuery();
        
        Krill ks = new Krill(sq);

        assertEquals(
            "spanNext(spanNext(base:s:b, spanRepetition(base:s:c{2,3})), base:s:d)",
            ks.getSpanQuery().toString());

        // fuzzingRepetitionBug();

        // Fourth fuzzed failure (1 vs 0)
        ki = new KrillIndex();
        ki.addDoc(simpleFieldDoc("cdcd"));
        ki.addDoc(simpleFieldDoc("bcebccac"));
        ki.addDoc(simpleFieldDoc("bccdcecc")); // !

        ki.commit();
        kr = ks.apply(ki);
        assertEquals(1,kr.getTotalResults());
    };


    /**
     * This method creates a corpus using fuzzing to
     * check for unexpected, failing constellations
     * regarding repetition queries.
     * By shrinking the accepted result length, it tries
     * to minimize the complexity of the constellations.
     */
    public void fuzzingRepetitionBug () throws IOException, QueryException {

        List<String> chars = Arrays.asList("a", "b", "c", "c", "d", "e");

        // Construct index
        Pattern p = Pattern.compile("bccc?d");
        QueryBuilder qb = new QueryBuilder("base");

        // b c{2,3} d
        SpanQuery sq = qb.seq(
            qb.seg("s:b")
            ).append(
                qb.repeat(qb.seg("s:c"),2,3)
                ).append(
                    qb.seg("s:d")
                    ).toQuery();
        
        Krill ks = new Krill(sq);

        assertEquals(
            "spanNext(spanNext(base:s:b, spanRepetition(base:s:c{2,3})), base:s:d)",
            ks.getSpanQuery().toString());

        String lastFailureConf = "";

        int minLength = 6;
        int maxLength = 22;
        int maxDocs = 8;

        // Create fuzzy corpora (1000 trials)
        for (int x = 0; x < 100000; x++) {
            KrillIndex ki = new KrillIndex();
            ArrayList<String> list = new ArrayList<String>();
            int c = 0;

            // Create a corpus of 8 fuzzy docs
            for (int i = 0; i < (int)(Math.random() * maxDocs); i++) {
                FieldDocument testDoc = simpleFuzzyFieldDoc(chars, minLength, maxLength);
                String testString = testDoc.doc.getField("base").stringValue();
                Matcher m = p.matcher(testString);
                list.add(testString);
                while (m.find())
                    c++;
                ki.addDoc(testDoc);
            };

            ki.commit();

            Result kr = ks.apply(ki);

            // Check if the regex-calculated matches are correct, otherwise
            // spit out the corpus configurations
            if (c != kr.getTotalResults()) {
                String failureConf = c + ":" + kr.getTotalResults() + " " + list.toString();
                if (lastFailureConf.length() == 0 ||
                    failureConf.length() < lastFailureConf.length()) {
                    System.err.println(failureConf);
                    lastFailureConf = failureConf;
                    minLength--;
                    maxDocs--;
                };
            };
        };
    };
}
