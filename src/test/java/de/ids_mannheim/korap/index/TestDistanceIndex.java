package de.ids_mannheim.korap.index;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.query.DistanceConstraint;
import de.ids_mannheim.korap.query.SpanDistanceQuery;
import de.ids_mannheim.korap.query.SpanElementQuery;
import de.ids_mannheim.korap.query.SpanSegmentQuery;
import de.ids_mannheim.korap.response.Result;

@RunWith(JUnit4.class)
public class TestDistanceIndex {
    Result kr;
    KrillIndex ki;


    private FieldDocument createFieldDoc0 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-0");
        fd.addTV("base", "text",
                 "[(0-1)s:b|s:c|_1$<i>0<i>1]" +
                 "[(1-2)s:b|_2$<i>1<i>2]" +
                 "[(2-3)s:c|_3$<i>2<i>3]" +
                 "[(3-4)s:c|_4$<i>3<i>4]" +
                 "[(4-5)s:d|_5$<i>4<i>5]" +
                 "[(5-6)s:d|_6$<i>5<i>6]");
        return fd;
    }


    private FieldDocument createFieldDoc1 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-1");
        fd.addTV("base", "ceccdcdeed",
                "[(0-1)s:c|_1$<i>0<i>1]"
                 + "[(1-2)s:e|_2$<i>1<i>2]"
                 + "[(2-3)s:c|_3$<i>2<i>3|<>:y$<b>64<i>2<i>4<i>4<b>0]"
                 + "[(3-4)s:c|_4$<i>3<i>4|<>:x$<b>64<i>3<i>7<i>7<b>0]"
                 + "[(4-5)s:d|s:a|_5$<i>4<i>5|<>:y$<b>64<i>4<i>6<i>6<b>0]"
                 + "[(5-6)s:c|_6$<i>5<i>6|<>:y$<b>64<i>5<i>8<i>8<b>0]"
                 + "[(6-7)s:d|_7$<i>6<i>7]"
                 + "[(7-8)s:e|_8$<i>7<i>8|<>:x$<b>64<i>7<i>9<i>9<b>0]"
                 + "[(8-9)s:e|_9$<i>8<i>9|<>:x$<b>64<i>8<i>10<i>10<b>0]"
                 + "[(9-10)s:d|_10$<i>9<i>10]");
        return fd;
    }


    private FieldDocument createFieldDoc2 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-2");
        fd.addTV("base", "text",
                "[(0-1)s:b|_1$<i>0<i>1]" 
                        + "[(1-2)s:b|_2$<i>1<i>2]"
                        + "[(2-3)s:d|_3$<i>2<i>3]" 
                        + "[(3-4)s:e|_4$<i>3<i>4]"
                        + "[(4-5)s:d|_5$<i>4<i>5]" 
                        + "[(5-6)s:e|_6$<i>5<i>6]");
        return fd;
    }


    private FieldDocument createFieldDoc3 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-3");
        fd.addTV("base", "cffe",
                 "[(0-1)s:c|_1$<i>0<i>1]"
                 + "[(1-2)s:f|_2$<i>1<i>2]"
                 + "[(2-3)s:f|_3$<i>2<i>3]"
                 + "[(3-4)s:e|_4$<i>3<i>4]");
        return fd;
    }

    private FieldDocument createFieldDoc4 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-4");
        fd.addTV("base", "eeef",
                 "[(0-1)s:e|_1$<i>0<i>1]"
                 + "[(1-2)s:e|_2$<i>1<i>2]"
                 + "[(2-3)s:e|_3$<i>2<i>3]"
                 + "[(3-4)s:f|_4$<i>3<i>4]");
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
     * - Intersection
     * - Multiple occurrences in the same doc
     * - hasMoreFirstSpans = false for the current secondspan
     */
    @Test
    public void testCase1 () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc0());
        ki.commit();
        SpanQuery sq;
        // ---- Distance 0 to 1
        sq = createQuery("s:b", "s:c", 0, 1, true);
        kr = ki.search(sq, (short) 10);
        //        System.out.println(sq);
        assertEquals(2, kr.getTotalResults());
        assertEquals(0, kr.getMatch(0).startPos);
        assertEquals(1, kr.getMatch(0).endPos);
        assertEquals(1, kr.getMatch(1).startPos);
        assertEquals(3, kr.getMatch(1).endPos);

        // ---- Distance 2 to 2
        sq = createQuery("s:b", "s:c", 2, 2, true);
        kr = ki.search(sq, (short) 10);

        assertEquals(2, kr.getTotalResults());
        assertEquals(0, kr.getMatch(0).startPos);
        assertEquals(3, kr.getMatch(0).endPos);
        assertEquals(1, kr.getMatch(1).startPos);
        assertEquals(4, kr.getMatch(1).endPos);

        // ---- Distance 2 to 3
        sq = createQuery("s:b", "s:c", 2, 3, true);
        kr = ki.search(sq, (short) 10);

        assertEquals(3, kr.getTotalResults());

        ki.close();
    }


    /**
     * - Check candidate list:
     * - CandidateList should not contain firstspans that are too far
     * from
     * the current secondspan
     * - Add new candidates
     */
    @Test
    public void testCase2 () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc1());
        ki.commit();

        // ---- Distance 1 to 3
        // Candidate list for the current secondspan, is empty
        SpanQuery sq = createQuery("s:c", "s:d", 1, 3, true);
        kr = ki.search(sq, (short) 10);

        assertEquals((long) 4, kr.getTotalResults());
        assertEquals(2, kr.getMatch(0).startPos);
        assertEquals(5, kr.getMatch(0).endPos);
        assertEquals(3, kr.getMatch(2).startPos);
        assertEquals(7, kr.getMatch(2).endPos);

        ki.addDoc(createFieldDoc0());
        ki.commit();

        // ---- Distance 3 to 3
        // Candidate list is empty, but there are secondspans in the other doc
        sq = createQuery("s:c", "s:d", 3, 3, true);
        kr = ki.search(sq, (short) 10);
        assertEquals((long) 2, kr.getTotalResults());

        ki.close();
    }


    /**
     * - Ensure the same document
     * - Multiple matches in multiple documents and atomic indices
     */
    @Test
    public void testCase3 () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc0());
        ki.commit();
        ki.addDoc(createFieldDoc2());
        ki.addDoc(createFieldDoc1());
        ki.commit();

        SpanQuery sq;
        sq = createQuery("s:c", "s:d", 3, 3, true);
        kr = ki.search(sq, (short) 10);

        assertEquals(2, kr.getTotalResults());
    }


    /**
     * - Firstspan.next() is in the other doc, but there is
     * still a secondspans in the same doc
     * - hasMoreFirstSpan and secondspans.next() are true,
     * but ensureSameDoc() = false
     */
    @Test
    public void testCase4 () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc0());
        ki.commit();
        ki.addDoc(createFieldDoc2());
        ki.addDoc(createFieldDoc1());
        ki.commit();

        // ---- Distance 1 to 2
        SpanQuery sq = createQuery("s:b", "s:c", 1, 2, true);
        kr = ki.search(sq, (short) 10);

        assertEquals(3, kr.getTotalResults());
        assertEquals(0, kr.getMatch(0).startPos);
        assertEquals(3, kr.getMatch(0).endPos);
        assertEquals(1, kr.getMatch(1).startPos);
        assertEquals(3, kr.getMatch(1).endPos);
        assertEquals(1, kr.getMatch(2).startPos);
        assertEquals(4, kr.getMatch(2).endPos);
        ki.close();
    }


    /** ElementQueries */
    @Test
    public void testCase5 () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc1());
        ki.commit();

        // Intersection ---- Distance 0:0
        SpanQuery sq = createElementQuery("x", "y", 0, 0, true);
        kr = ki.search(sq, (short) 10);

        assertEquals(4, kr.getTotalResults());
        assertEquals(2, kr.getMatch(0).startPos);
        assertEquals(7, kr.getMatch(0).endPos);
        assertEquals(3, kr.getMatch(1).startPos);
        assertEquals(7, kr.getMatch(1).endPos);
        assertEquals(3, kr.getMatch(2).startPos);
        assertEquals(8, kr.getMatch(2).endPos);

        // Next to ---- Distance 1:1
        sq = createElementQuery("y", "x", 1, 1, true);
        kr = ki.search(sq, (short) 10);

        assertEquals(1, kr.getTotalResults());
        assertEquals(5, kr.getMatch(0).startPos);
        assertEquals(10, kr.getMatch(0).endPos);

        // ---- Distance 1:2
        sq = createElementQuery("y", "x", 1, 2, true);
        kr = ki.search(sq, (short) 10);

        assertEquals(2, kr.getTotalResults());
        assertEquals(4, kr.getMatch(0).startPos);
        assertEquals(9, kr.getMatch(0).endPos);
        assertEquals(5, kr.getMatch(1).startPos);
        assertEquals(10, kr.getMatch(1).endPos);

        // The same element type ---- Distance 1:2
        sq = createElementQuery("x", "x", 1, 2, true);
        kr = ki.search(sq, (short) 10);

        assertEquals(2, kr.getTotalResults());
    }


    @Test
    public void testSkipTo () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc2());
        ki.addDoc(createFieldDoc1());
        ki.commit();

        SpanQuery firstClause = createQuery("s:d", "s:e", 3, 4, true);
        kr = ki.search(firstClause, (short) 10);

        assertEquals(3, kr.getTotalResults());
        assertEquals(0, kr.getMatch(0).getLocalDocID());
        assertEquals(2, kr.getMatch(0).startPos);
        assertEquals(6, kr.getMatch(0).endPos);
        assertEquals(1, kr.getMatch(1).getLocalDocID());
        assertEquals(4, kr.getMatch(1).startPos);
        assertEquals(8, kr.getMatch(1).endPos);
        assertEquals(4, kr.getMatch(2).startPos);
        assertEquals(9, kr.getMatch(2).endPos);

        // The secondspans is skipped to doc# of the current firstspans
        SpanQuery sq = new SpanSegmentQuery(
                createQuery("s:d", "s:e", 3, 4, true),
                createElementQuery("y", "x", 1, 2, true));
        kr = ki.search(sq, (short) 10);

        assertEquals(1, kr.getTotalResults());
        assertEquals(4, kr.getMatch(0).startPos);
        assertEquals(9, kr.getMatch(0).endPos);
    }

    @Test
    public void testSkipToAndHasNoMoreFirstSpan () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc1());
        ki.addDoc(createFieldDoc3());
        ki.commit();

        SpanQuery firstClause = createQuery("s:a", "s:c", 1, 1, true);
        kr = ki.search(firstClause, (short) 10);
        
        SpanDistanceQuery sdq = new SpanDistanceQuery(firstClause, 
                new SpanTermQuery(new Term("base", "s:f")), 
                new DistanceConstraint(1, 1, true, false), true);
        
        kr = ki.search(sdq, (short) 10);
    }
    
//    testSkipToAndHasNoMoreSecondSpan 
//    cannot happen because immediately after a second span is advanced, 
//    hasMoreSpans (=hasMoreSecondSpans) is checked.
    
    @Test
    public void testDistanceOfIdenticalTokens () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc1());
        ki.commit();

        SpanQuery sq = createQuery("s:c", "s:c", 1, 2, true);
        kr = ki.search(sq, (short) 10);

        assertEquals(3, kr.getTotalResults());
        assertEquals(0, kr.getMatch(0).startPos);
        assertEquals(3, kr.getMatch(0).endPos);
        assertEquals(2, kr.getMatch(1).startPos);
        assertEquals(4, kr.getMatch(1).endPos);
        assertEquals(3, kr.getMatch(2).startPos);
        assertEquals(6, kr.getMatch(2).endPos);

        ki.addDoc(createFieldDoc2());
        ki.commit();

        // with order
        sq = createQuery("s:e", "s:e", 1, 1, true);
        kr = ki.search(sq, (short) 10);

        assertEquals(1, kr.getTotalResults());

        // without order
        sq = createQuery("s:e", "s:e", 1, 1, false);
        kr = ki.search(sq, (short) 10);

        assertEquals(2, kr.getTotalResults());
    }

    /** doc() cannot be called when SpanOr has reached an end.
     * 
     * @throws IOException
     */
    @Test
    public void testSpanOr () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc1());
        ki.commit();
        SpanQuery sq;

        // (c or d) /+w1 e
        sq = new SpanDistanceQuery(
            new SpanOrQuery(
                new SpanTermQuery(new Term("base", "s:c")),
                new SpanTermQuery(new Term("base", "s:d"))
                ),
            new SpanTermQuery(new Term("base", "s:e")),
            new DistanceConstraint(0, 1, true, false), true);

        kr = ki.search(sq, (short) 10);
        assertEquals(2,kr.getTotalResults());

        // (c or d) /+w1 c
        sq = new SpanDistanceQuery(
            new SpanOrQuery(
                new SpanTermQuery(new Term("base", "s:c")),
                new SpanTermQuery(new Term("base", "s:d"))
                ),
            new SpanTermQuery(new Term("base", "s:c")),
            new DistanceConstraint(0, 1, true, false), true);

        kr = ki.search(sq, (short) 10);
        assertEquals(6,kr.getTotalResults());
    }
    
    @Test
    public void testHasNoMoreSecondSpans () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc1());
        ki.commit();
        SpanQuery sq;

        // (c or d) /+w1 (e or c)
        sq = new SpanDistanceQuery(
            new SpanOrQuery(
                new SpanTermQuery(new Term("base", "s:c")),
                new SpanTermQuery(new Term("base", "s:d"))
                ),
            new SpanOrQuery(
                new SpanTermQuery(new Term("base", "s:e")),
                new SpanTermQuery(new Term("base", "s:c"))
                ),
            new DistanceConstraint(0, 1, true, false), true);

        kr = ki.search(sq, (short) 10);
        assertEquals(8, kr.getTotalResults());
    }
    
    @Test
    public void testHasNoMoreFirstSpans() throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc1());
        ki.addDoc(createFieldDoc3());
        ki.addDoc(createFieldDoc4());
        ki.commit();

        // (c or d) /+w1 (e or f)
        SpanQuery sq = new SpanDistanceQuery(
            new SpanOrQuery(
                new SpanTermQuery(new Term("base", "s:c")),
                new SpanTermQuery(new Term("base", "s:d"))
                ),
            new SpanOrQuery(
                new SpanTermQuery(new Term("base", "s:e")),
                new SpanTermQuery(new Term("base", "s:f"))
                ),
            new DistanceConstraint(0, 1, true, false), true);

        kr = ki.search(sq, (short) 10);
        assertEquals(3, kr.getTotalResults());
        ki.close();
    }
}
