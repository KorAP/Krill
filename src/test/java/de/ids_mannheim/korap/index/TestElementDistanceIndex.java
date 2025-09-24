package de.ids_mannheim.korap.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.*;
import java.io.*;
import static de.ids_mannheim.korap.TestSimple.*;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.KrillQuery;
import de.ids_mannheim.korap.query.DistanceConstraint;
import de.ids_mannheim.korap.query.QueryBuilder;
import de.ids_mannheim.korap.query.SpanDistanceQuery;
import de.ids_mannheim.korap.query.SpanElementQuery;
import de.ids_mannheim.korap.query.SpanNextQuery;
import de.ids_mannheim.korap.query.wrap.SpanQueryWrapper;
import de.ids_mannheim.korap.response.Match;
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.util.QueryException;

/**
 * @author margaretha
 *
 */
@RunWith(JUnit4.class)
public class TestElementDistanceIndex {

    Result kr;
    KrillIndex ki;

    private FieldDocument createFieldDoc0 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-0");
        fd.addTV("tokens", "bbcbbbc",
                "[(0-1)s:b|s:c|_0$<i>0<i>1|<>:s$<b>64<i>0<i>2<i>1<b>0]"
                        + "[(1-2)s:b|_1$<i>1<i>2]"
                        + "[(2-3)s:c|_2$<i>2<i>3|<>:s$<b>64<i>2<i>3<i>3<b>0]"
                        + "[(3-4)s:b|_3$<i>3<i>4|<>:s$<b>64<i>3<i>4<i>4<b>0]"
                        + "[(4-5)s:b|_4$<i>4<i>5|<>:s$<b>64<i>4<i>5<i>5<b>0]"
                        + "[(5-6)s:b|_5$<i>5<i>6]"
                        + "[(6-7)s:c|_6$<i>6<i>7|<>:s$<b>64<i>6<i>7<i>7<b>0]");
        return fd;
    }

    private FieldDocument createFieldDoc1 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-1");
        fd.addTV("tokens", "ecebdc",
                "[(0-1)s:e|_0$<i>0<i>1|<>:base/s:t$<b>64<i>0<i>6<i>6<b>0|<>:s$<b>64<i>0<i>1<i>1<b>0]"
                        + "[(1-2)s:c|s:b|_1$<i>1<i>2|<>:s$<b>64<i>1<i>2<i>2<b>0]"
                        + "[(2-3)s:e|_2$<i>2<i>3|<>:s$<b>64<i>2<i>3<i>3<b>0]"
                        + "[(3-4)s:b|_3$<i>3<i>4|<>:s$<b>64<i>3<i>4<i>4<b>0]"
                        + "[(4-5)s:c|_4$<i>4<i>5|<>:s$<b>64<i>4<i>5<i>5<b>0]"
                        + "[(5-6)s:c|_5$<i>5<i>6|<>:s$<b>64<i>5<i>6<i>6<b>0]");
        return fd;
    }

    private FieldDocument createFieldDoc2 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-2");
        fd.addTV("tokens", "bbbddd",
                "[(0-1)s:b|_0$<i>0<i>1|<>:p$<b>64<i>0<i>1<i>1<b>0]"
                        + "[(1-2)s:b|_1$<i>1<i>2]"
                        + "[(2-3)s:b|_2$<i>2<i>3|<>:p$<b>64<i>2<i>3<i>3<b>0]"
                        + "[(3-4)s:d|_3$<i>3<i>4|<>:p$<b>64<i>3<i>4<i>4<b>0]"
                        + "[(4-5)s:d|_4$<i>4<i>5|<>:p$<b>64<i>4<i>5<i>5<b>0]"
                        + "[(5-6)s:d|_5$<i>5<i>6]");
        return fd;
    }

    private FieldDocument createFieldDoc3 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-3");
        fd.addTV("tokens", "bdbcdd",
                "[(0-1)s:b|_0$<i>0<i>1|<>:s$<b>64<i>0<i>2<i>2<b>0]"
                        + "[(1-2)s:d|_1$<i>1<i>2]"
                        + "[(2-3)s:b|_2$<i>2<i>3|<>:s$<b>64<i>2<i>3<i>3<b>0]"
                        + "[(3-4)s:c|_3$<i>3<i>4|<>:s$<b>64<i>3<i>5<i>5<b>0]"
                        + "[(4-5)s:d|_4$<i>4<i>5|<>:s$<b>64<i>4<i>5<i>5<b>0]"
                        + "[(5-6)s:d|_5$<i>5<i>6]");
        return fd;
    }

    private FieldDocument createFieldDoc4 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-4");
        fd.addTV("tokens", "bdbcdd",
                "[(0-1)s:b|_0$<i>0<i>1|<>:s$<b>64<i>0<i>1<i>1<b>0]"
                        + "[(1-2)s:d|_1$<i>1<i>2]"
                        + "[(2-3)s:c|s:b|_2$<i>2<i>3|<>:s$<b>64<i>2<i>3<i>3<b>0]"
                        + "[(3-4)s:c|_3$<i>3<i>4|<>:s$<b>64<i>3<i>5<i>5<b>0]"
                        + "[(4-5)s:d|_4$<i>4<i>5|<>:s$<b>64<i>4<i>5<i>5<b>0]"
                        + "[(5-6)s:d|_5$<i>5<i>6]");
        return fd;
    }
    
    private FieldDocument createFieldDoc5 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-5");
        fd.addTV("tokens", "edef",
                 "[(0-1)s:e|_0$<i>0<i>1]"
                 + "[(1-2)s:d|_1$<i>1<i>2]"
                 + "[(2-3)s:e|_2$<i>2<i>3]"
                 + "[(3-4)s:f|_3$<i>3<i>4]");
        return fd;
    }
    
    private FieldDocument createFieldDoc6 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-6");
        fd.addTV("tokens", "bebcee",
                "[(0-1)s:b|_0$<i>0<i>1|<>:s$<b>64<i>0<i>4<i>4<b>0]"
                        + "[(1-2)s:e|_1$<i>1<i>2]"
                        + "[(2-3)s:b|_2$<i>2<i>3]"
                        + "[(3-4)s:c|_3$<i>3<i>4]"
                        + "[(4-5)s:e|_4$<i>4<i>5|<>:s$<b>64<i>4<i>6<i>6<b>0]"
                        + "[(5-6)s:e|_5$<i>5<i>6]");
        return fd;
    }
    
    private FieldDocument createFieldDoc7 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-5");
        fd.addTV("tokens", "beef",
                 "[(0-1)s:b|_0$<i>0<i>1|<>:s$<b>64<i>0<i>4<i>4<b>0]"
                 + "[(1-2)s:e|_1$<i>1<i>2]"
                 + "[(2-3)s:e|_2$<i>2<i>3]"
                 + "[(3-4)s:f|_3$<i>3<i>4]");
        return fd;
    }
    

    public SpanQuery createQuery (String elementType, String x, String y,
            int min, int max, boolean isOrdered) {

        SpanElementQuery e = new SpanElementQuery("tokens", elementType);
        return new SpanDistanceQuery(new SpanTermQuery(new Term("tokens", x)),
                new SpanTermQuery(new Term("tokens", y)),
                new DistanceConstraint(e, min, max, isOrdered, false), true);
    }

    /**
     * Multiple documents
     * Ensure terms and elements are in the same doc
     * Ensure terms are in elements
     * Check filter candidate list
     */
    @Test
    public void testCase1 () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc0());
        ki.addDoc(createFieldDoc1());
        ki.addDoc(createFieldDoc3());
        ki.commit();

        SpanQuery sq;
        sq = createQuery("s", "s:b", "s:c", 1, 1, true);

        kr = ki.search(sq, (short) 10);

        assertEquals(4, kr.getTotalResults());
        assertEquals(0, kr.getMatch(0).startPos);
        assertEquals(3, kr.getMatch(0).endPos);
        assertEquals(4, kr.getMatch(1).startPos);
        assertEquals(7, kr.getMatch(1).endPos);
        assertEquals(3, kr.getMatch(2).startPos);
        assertEquals(5, kr.getMatch(2).endPos);
        assertEquals(2, kr.getMatch(3).startPos);
        assertEquals(4, kr.getMatch(3).endPos);
    }

    /**
     * Ignore nested element distance unit
     */
    @Test
    public void testCase1b () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc4());
        ki.commit();
        // b~d~b/c~c~dd
        SpanQuery sq;
        sq = createQuery("s", "s:b", "s:c", 1, 1, true);

        kr = ki.search(sq, (short) 10);

        assertEquals(2, kr.getTotalResults());
        assertEquals(0, kr.getMatch(0).startPos);
        assertEquals(3, kr.getMatch(0).endPos);
        assertEquals(2, kr.getMatch(1).startPos);
        assertEquals(4, kr.getMatch(1).endPos);
    }

    /**
     * Ensure terms and elements are in the same doc
     */
    @Test
    public void testCase2 () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc0());
        ki.addDoc(createFieldDoc1());
        ki.addDoc(createFieldDoc2());
        ki.commit();

        SpanQuery sq;
        sq = createQuery("p", "s:b", "s:d", 1, 1, true);
        kr = ki.search(sq, (short) 10);

        assertEquals(1, kr.getTotalResults());
        assertEquals(2, kr.getMatch(0).getLocalDocID());
        assertEquals(2, kr.getMatch(0).startPos);
        assertEquals(4, kr.getMatch(0).endPos);

    }

    /** Skip to */
    @Test
    public void testCase3 () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc0());
        ki.addDoc(createFieldDoc1());
        ki.addDoc(createFieldDoc3());
        ki.commit();

        SpanQuery sq, edq;
        edq = createQuery("s", "s:b", "s:c", 1, 1, true);

        sq = new SpanNextQuery(edq,
                new SpanTermQuery(new Term("tokens", "s:d")));

        kr = ki.search(sq, (short) 10);

        assertEquals(1, kr.getTotalResults());
        assertEquals(2, kr.getMatch(0).getLocalDocID());
        assertEquals(2, kr.getMatch(0).startPos);
        assertEquals(5, kr.getMatch(0).endPos);

    }

    /** Same tokens in different elements */
    @Test
    public void testCase4 () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc0());
        ki.commit();

        SpanQuery sq;
        sq = createQuery("s", "s:b", "s:b", 1, 2, true);
        kr = ki.search(sq, (short) 10);

        assertEquals(2, kr.getTotalResults());
        assertEquals(0, kr.getMatch(0).startPos);
        assertEquals(4, kr.getMatch(0).endPos);
        assertEquals(3, kr.getMatch(1).startPos);
        assertEquals(5, kr.getMatch(1).endPos);

    }

    /** Test query from json */
    @Test
    public void testCase5 () throws Exception {
        ki = new KrillIndex();
        ki.addDoc(getClass().getResourceAsStream("/wiki/00001.json.gz"), true);
        ki.commit();

        String jsonPath =
                getClass().getResource("/queries/cosmas1.json").getFile();
        SpanQueryWrapper sqwi = getJsonQuery(jsonPath);
        kr = ki.search(sqwi.toQuery(), (short) 10);

        assertEquals((long) 3, kr.getTotalResults());
        assertEquals(14, kr.getMatch(0).startPos);
        assertEquals(19, kr.getMatch(0).endPos);
        assertEquals(30, kr.getMatch(1).startPos);
        assertEquals(33, kr.getMatch(1).endPos);
    }

    @Test
    public void testCQLAnd () throws Exception {
        ki = new KrillIndex();
        ki.addDoc(getClass().getResourceAsStream("/wiki/00001.json.gz"), true);
        ki.commit();

        String jsonPath = getClass()
                .getResource("/queries/distances/cql-and.jsonld").getFile();
        SpanQueryWrapper sqwi = getJsonQuery(jsonPath);
        SpanQuery query = sqwi.toQuery();
        assertEquals(
                "spanElementDistance(tokens:s:Buchstaben, tokens:s:Alphabet, "
                        + "[(base/s:s[0:0], notOrdered, notExcluded)])",
                query.toString());
        kr = ki.search(sqwi.toQuery(), (short) 10);

        assertEquals((long) 1, kr.getTotalResults());
    }

    /** Test query from json (2) */
    @Test
    public void testCase6 () throws Exception {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc0());
        ki.addDoc(createFieldDoc1());
        ki.addDoc(createFieldDoc2());
        ki.commit();

        SpanQueryWrapper sqwi;
        sqwi = new QueryBuilder("tokens").tag("base/s:t");

        kr = ki.search(sqwi.toQuery(), (short) 10);
        assertEquals(1, kr.getTotalResults());
        assertEquals("[[ecebdc]]", kr.getMatch(0).getSnippetBrackets());

        String jsonPath = getClass()
                .getResource("/queries/distances/in-same-t.jsonld").getFile();
        sqwi = getJsonQuery(jsonPath);

        assertEquals(
                "spanElementDistance(tokens:s:c, tokens:s:e, [(base/s:t[0:0], ordered, notExcluded)])",
                sqwi.toQuery().toString());

        kr = ki.search(sqwi.toQuery(), (short) 10);
        assertEquals(1, kr.getTotalResults()); // Is 1 correct or
                                               // should it not be
                                               // ordered?
        assertEquals("e[[ce]]bdc", kr.getMatch(0).getSnippetBrackets());
    }
    
    @Test
    public void testNoMoreFirstSpans () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc3());
        ki.addDoc(createFieldDoc5());
        ki.commit();

        // c or b
        SpanOrQuery soq = new SpanOrQuery(
                new SpanTermQuery(new Term("tokens", "s:c")),
                new SpanTermQuery(new Term("tokens", "s:b")));
        
        // (c or b) /s0 d
        SpanElementQuery e = new SpanElementQuery("tokens", "s");
        SpanDistanceQuery sdq = new SpanDistanceQuery(
                soq,
                new SpanTermQuery(new Term("tokens", "s:d")),
                new DistanceConstraint(e, 0, 0, true, false), true);

        kr = ki.search(sdq, (short) 10);
        assertEquals(2, kr.getTotalResults());
    }
    
    @Test
    public void testNoMoreSecondSpans () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc3());
        ki.addDoc(createFieldDoc5());
        ki.commit();

        // c or b
        SpanOrQuery soq = new SpanOrQuery(
                new SpanTermQuery(new Term("tokens", "s:c")),
                new SpanTermQuery(new Term("tokens", "s:b")));
        
        // d /s0(c or b)
        SpanElementQuery e = new SpanElementQuery("tokens", "s");
        SpanDistanceQuery sdq = new SpanDistanceQuery(
                new SpanTermQuery(new Term("tokens", "s:d")),
                soq,
                new DistanceConstraint(e, 0, 0, true, false), true);

        kr = ki.search(sdq, (short) 10);
        assertEquals(0, kr.getTotalResults());
    }
    
    
    
    @Test
    public void testNoElementSpans () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc3());
        ki.addDoc(createFieldDoc5());
        ki.commit();

        // c or e
        SpanOrQuery soq = new SpanOrQuery(
                new SpanTermQuery(new Term("tokens", "s:c")),
                new SpanTermQuery(new Term("tokens", "s:e")));
        
        // (c or e) /s0 d
        SpanElementQuery e = new SpanElementQuery("tokens", "s");
        SpanDistanceQuery sdq = new SpanDistanceQuery(
                soq,
                new SpanTermQuery(new Term("tokens", "s:d")),
                new DistanceConstraint(e, 0, 0, true, false), true);

        kr = ki.search(sdq, (short) 10);
        assertEquals(1, kr.getTotalResults());
    }
    
    @Test
    public void testNoMoreFirstSpanWithSpanOrQuery () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc3());
        ki.addDoc(createFieldDoc5());
        ki.commit();

        // c or d
        SpanOrQuery soq = new SpanOrQuery(
                new SpanTermQuery(new Term("tokens", "s:c")),
                new SpanTermQuery(new Term("tokens", "s:d")));
        
        // b /s0 (c or d)
        SpanElementQuery e = new SpanElementQuery("tokens", "s");
        SpanDistanceQuery sdq = new SpanDistanceQuery(
                new SpanTermQuery(new Term("tokens", "s:b")),
                soq,
                new DistanceConstraint(e, 0, 0, true, false), true);

        kr = ki.search(sdq, (short) 10);
        assertEquals(1, kr.getTotalResults());
    }
    
    @Test
    public void testNoMoreSecondSpansOrQuery () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc6());
        ki.addDoc(createFieldDoc5());
        ki.addDoc(createFieldDoc7());
        ki.commit();

        // c or d
        SpanOrQuery soq = new SpanOrQuery(
                new SpanTermQuery(new Term("tokens", "s:c")),
                new SpanTermQuery(new Term("tokens", "s:d")));
        
        // b /s0(c or d)
        SpanElementQuery e = new SpanElementQuery("tokens", "s");
        SpanDistanceQuery sdq = new SpanDistanceQuery(
                new SpanTermQuery(new Term("tokens", "s:b")),
                soq,
                new DistanceConstraint(e, 0, 0, true, false), true);

        kr = ki.search(sdq, (short) 10);
        
        assertEquals(2, kr.getTotalResults());
        
//        System.out.println(kr.getTotalResults());
//        for (Match m : kr.getMatches()) {
//            System.out.println(m.getSnippetBrackets());
//        }
    }
}
