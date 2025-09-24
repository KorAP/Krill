package de.ids_mannheim.korap.index;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.query.DistanceConstraint;
import de.ids_mannheim.korap.query.SpanDistanceQuery;
import de.ids_mannheim.korap.query.SpanElementQuery;
import de.ids_mannheim.korap.query.SpanNextQuery;
import de.ids_mannheim.korap.response.Result;

@RunWith(JUnit4.class)
public class TestUnorderedDistanceIndex {

    private KrillIndex ki;
    private Result kr;


    private FieldDocument createFieldDoc0 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-0");
        fd.addTV("base", "text",
                "[(0-1)s:c|_1$<i>0<i>1]" + "[(1-2)s:e|_2$<i>1<i>2]"
                        + "[(2-3)s:c|_3$<i>2<i>3|<>:y$<b>64<i>2<i>4<i>4<b>0]"
                        + "[(3-4)s:c|_4$<i>3<i>4|<>:x$<b>64<i>3<i>7<i>7<b>0]"
                        + "[(4-5)s:d|_5$<i>4<i>5|<>:y$<b>64<i>4<i>6<i>6<b>0]"
                        + "[(5-6)s:c|_6$<i>5<i>6|<>:y$<b>64<i>5<i>8<i>8<b>0]"
                        + "[(6-7)s:d|_7$<i>6<i>7]"
                        + "[(7-8)s:f|_8$<i>7<i>8|<>:x$<b>64<i>7<i>9<i>9<b>0]"
                        + "[(8-9)s:e|_9$<i>8<i>9|<>:x$<b>64<i>8<i>10<i>10<b>0]"
                        + "[(9-10)s:d|_10$<i>9<i>10]");
        return fd;
    }


    private FieldDocument createFieldDoc1 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-1");
        fd.addTV("base", "text",
                "[(0-1)s:d|_1$<i>0<i>1]" + "[(1-2)s:c|_2$<i>1<i>2]"
                        + "[(2-3)s:e|_3$<i>2<i>3]" + "[(3-4)s:e|_4$<i>3<i>4]"
                        + "[(4-5)s:d|_5$<i>4<i>5]" + "[(5-6)s:e|_6$<i>5<i>6]"
                        + "[(6-7)s:e|_7$<i>6<i>7]" + "[(7-8)s:c|_8$<i>7<i>8]"
                        + "[(8-9)s:e|_9$<i>8<i>9]"
                        + "[(9-10)s:d|_10$<i>9<i>10]");
        return fd;
    }


    private FieldDocument createFieldDoc2 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-2");
        fd.addTV("base", "text",
                "[(0-1)s:f|_1$<i>0<i>1]" + "[(1-2)s:c|_2$<i>1<i>2]"
                        + "[(2-3)s:e|_3$<i>2<i>3]" + "[(3-4)s:e|_4$<i>3<i>4]"
                        + "[(4-5)s:d|_5$<i>4<i>5]" + "[(5-6)s:f|_6$<i>5<i>6]"
                        + "[(6-7)s:f|_7$<i>6<i>7]");
        return fd;
    }


    private SpanQuery createQuery (String x, String y, int min, int max,
            boolean isOrdered) {
        SpanQuery sq = new SpanDistanceQuery(
                new SpanTermQuery(new Term("base", x)),
                new SpanTermQuery(new Term("base", y)),
                new DistanceConstraint(min, max, isOrdered, false), true);
        return sq;
    }


    private SpanQuery createElementQuery (String x, String y, int min, int max,
            boolean isOrdered) {
        SpanQuery sq = new SpanDistanceQuery(new SpanElementQuery("base", x),
                new SpanElementQuery("base", y),
                new DistanceConstraint(min, max, isOrdered, false), true);
        return sq;
    }


    /**
     * One document, multiple occurrences
     * The first first and second spans are too far from each other
     * One of the spans ends first
     * One of the candidate list is empty
     */
    @Test
    public void testCase1 () throws IOException {
        //System.out.println("testcase 1");
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc0());
        ki.commit();

        SpanQuery sq = createQuery("s:c", "s:d", 0, 3, false);
        kr = ki.search(sq, (short) 10);

        assertEquals(5, kr.getTotalResults());
    }


    /**
     * Multiple documents
     * Ensure same doc
     * Both candidate lists are empty, but there is a span left in the
     * doc
     * Both candidate lists are empty, but there are more matches in
     * the doc
     * 
     * @throws IOException
     */
    @Test
    public void testCase2 () throws IOException {
        //System.out.println("testcase 2");
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc0());
        ki.addDoc(createFieldDoc1());
        ki.commit();

        SpanQuery sq = createQuery("s:c", "s:d", 1, 2, false);
        kr = ki.search(sq, (short) 10);

        assertEquals(6, kr.getTotalResults());
    }


    /**
     * Multiple documents
     * Ensure same Doc
     * 
     * @throws IOException
     */
    @Test
    public void testCase3 () throws IOException {
        //System.out.println("testcase 3");
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc0());
        ki.addDoc(createFieldDoc1());
        ki.addDoc(createFieldDoc2());
        ki.commit();

        SpanQuery sq = createQuery("s:e", "s:f", 1, 2, false);
        kr = ki.search(sq, (short) 10);

        assertEquals(3, kr.getTotalResults());
        assertEquals(0, kr.getMatch(0).getLocalDocID());
        assertEquals(7, kr.getMatch(0).getStartPos());
        assertEquals(9, kr.getMatch(0).getEndPos());
        assertEquals(2, kr.getMatch(1).getLocalDocID());
        assertEquals(0, kr.getMatch(1).getStartPos());
        assertEquals(3, kr.getMatch(1).getEndPos());
    }


    /** Skip to */
    @Test
    public void testCase4 () throws IOException {
        //System.out.println("testcase 4");
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc0());
        ki.addDoc(createFieldDoc1());
        ki.addDoc(createFieldDoc2());
        ki.commit();

        SpanQuery sq = new SpanNextQuery(createQuery("s:d", "s:e", 1, 2, false),
                new SpanTermQuery(new Term("base", "s:f")));

        kr = ki.search(sq, (short) 10);
        assertEquals(2, kr.getTotalResults());
        assertEquals(2, kr.getMatch(0).getLocalDocID());
        assertEquals(2, kr.getMatch(0).getStartPos());
        assertEquals(6, kr.getMatch(0).getEndPos());
        assertEquals(3, kr.getMatch(1).getStartPos());
        assertEquals(6, kr.getMatch(1).getEndPos());
    }


    /** ElementQueries */
    @Test
    public void testCase5 () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc0());
        ki.commit();

        // Intersection ---- Distance 0:0
        //System.out.println("Intersection ---- Distance 0:0");
        SpanQuery sq = createElementQuery("x", "y", 0, 0, false);
        kr = ki.search(sq, (short) 10);

        assertEquals(4, kr.getTotalResults());
        assertEquals(2, kr.getMatch(0).startPos);
        assertEquals(7, kr.getMatch(0).endPos);
        assertEquals(3, kr.getMatch(1).startPos);
        assertEquals(7, kr.getMatch(1).endPos);
        assertEquals(3, kr.getMatch(2).startPos);
        assertEquals(8, kr.getMatch(2).endPos);

        // Next to ---- Distance 1:1
        //System.out.println("Next to ---- Distance 1:1");
        sq = createElementQuery("x", "y", 1, 1, false);
        kr = ki.search(sq, (short) 10);

        assertEquals(1, kr.getTotalResults());
        assertEquals(5, kr.getMatch(0).startPos);
        assertEquals(10, kr.getMatch(0).endPos);

        // ---- Distance 1:2
        //System.out.println("---- Distance 1:2");
        sq = createElementQuery("x", "y", 1, 2, false);
        kr = ki.search(sq, (short) 10);

        assertEquals(2, kr.getTotalResults());
        assertEquals(4, kr.getMatch(0).startPos);
        assertEquals(9, kr.getMatch(0).endPos);
        assertEquals(5, kr.getMatch(1).startPos);
        assertEquals(10, kr.getMatch(1).endPos);

    }


    /**
     * The same element type
     * 
     * WARNING:
     * This kind of query is not appropriate for an unordered distance
     * span query.
     * Instead, it must be an ordered distance span query. Such an
     * unordered distance
     * span query yields "redundant results" because matches are
     * searched for each
     * child span.
     */
    @Test
    public void testCase6 () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc0());
        ki.commit();

        //---- Distance 1:2
        SpanQuery sq = createElementQuery("x", "x", 1, 2, false);
        kr = ki.search(sq, (short) 10);

        assertEquals(4, kr.getTotalResults());
    }


    /**
     * Nested distance queries
     */
    @Test
    public void testCase7 () throws IOException {
        //System.out.println("testcase 7");
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc0());
        ki.addDoc(createFieldDoc1());
        ki.commit();

        SpanQuery sq = createQuery("s:c", "s:d", 1, 2, false);
        SpanQuery sq2 = new SpanDistanceQuery(sq,
                new SpanTermQuery(new Term("base", "s:e")),
                new DistanceConstraint(1, 2, true, false), true);
        kr = ki.search(sq2, (short) 10);
        assertEquals(3, kr.getTotalResults());
        assertEquals(5, kr.getMatch(0).getStartPos());
        assertEquals(9, kr.getMatch(0).getEndPos());
        assertEquals(1, kr.getMatch(1).getLocalDocID());
        assertEquals(0, kr.getMatch(1).getStartPos());
        assertEquals(3, kr.getMatch(1).getEndPos());
        assertEquals(0, kr.getMatch(2).getStartPos());
        assertEquals(4, kr.getMatch(2).getEndPos());
    }


    /**
     * Multiple NextSpans in the same first span position
     */
    @Test
    public void testCase8 () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc1());
        ki.commit();
        SpanQuery sq = new SpanNextQuery(
                new SpanTermQuery(new Term("base", "s:d")),
                createQuery("s:c", "s:e", 1, 2, false));
        kr = ki.search(sq, (short) 10);

        assertEquals(3, kr.getTotalResults());
        assertEquals(0, kr.getMatch(1).getStartPos());
        assertEquals(4, kr.getMatch(1).getEndPos());

    }

}
