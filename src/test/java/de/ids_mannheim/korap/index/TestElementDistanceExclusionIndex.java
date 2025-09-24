package de.ids_mannheim.korap.index;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.junit.Test;

import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.query.DistanceConstraint;
import de.ids_mannheim.korap.query.SpanDistanceQuery;
import de.ids_mannheim.korap.query.SpanElementQuery;
import de.ids_mannheim.korap.query.SpanNextQuery;
import de.ids_mannheim.korap.response.Result;

public class TestElementDistanceExclusionIndex {

    Result kr;
    KrillIndex ki;


    private SpanQuery createQuery (String e, String x, String y, int min,
            int max, boolean isOrdered, boolean exclusion) {
        SpanElementQuery eq = new SpanElementQuery("base", e);
        SpanDistanceQuery sq = new SpanDistanceQuery(
                new SpanTermQuery(new Term("base", x)),
                new SpanTermQuery(new Term("base", y)),
                new DistanceConstraint(eq, min, max, isOrdered, exclusion),
                true);
        return sq;
    }


    private FieldDocument createFieldDoc0 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-0");
        fd.addTV("base", "ceccdcdecd",
                "[(0-1)s:c|_1$<i>0<i>1|<>:s$<b>64<i>0<i>1<i>1<b>0]"
                        + "[(1-2)s:e|_2$<i>1<i>2|<>:s$<b>64<i>1<i>2<i>2<b>0]"
                        + "[(2-3)s:c|_3$<i>2<i>3|<>:s$<b>64<i>2<i>4<i>4<b>0]"
                        + "[(3-4)s:c|_4$<i>3<i>4]"
                        + "[(4-5)s:d|_5$<i>4<i>5|<>:s$<b>64<i>4<i>6<i>6<b>0]"
                        + "[(5-6)s:c|_6$<i>5<i>6]"
                        + "[(6-7)s:d|_7$<i>6<i>7|<>:s$<b>64<i>6<i>7<i>7<b>0]"
                        + "[(7-8)s:e|_8$<i>7<i>8|<>:s$<b>64<i>7<i>9<i>9<b>0]"
                        + "[(8-9)s:c|_9$<i>8<i>9]"
                        + "[(9-10)s:d|_10$<i>9<i>10]");
        return fd;
    }


    private FieldDocument createFieldDoc1 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-1");
        fd.addTV("base", "eedadaeed",
                "[(0-1)s:e|_1$<i>0<i>1|<>:s$<b>64<i>0<i>1<i>1<b>0]"
                        + "[(1-2)s:e|_2$<i>1<i>2|<>:s$<b>64<i>1<i>2<i>2<b>0]"
                        + "[(2-3)s:d|_3$<i>2<i>3|<>:s$<b>64<i>2<i>4<i>4<b>0]"
                        + "[(3-4)s:a|_4$<i>3<i>4]"
                        + "[(4-5)s:d|_5$<i>4<i>5|<>:s$<b>64<i>4<i>7<i>6<b>0]"
                        + "[(5-6)s:a|_6$<i>5<i>6]"
                        + "[(6-7)s:e|_7$<i>6<i>7|<>:s$<b>64<i>6<i>7<i>9<b>0]"
                        + "[(7-8)s:e|_8$<i>7<i>8]" + "[(8-9)s:d|_9$<i>8<i>9]");
        return fd;
    }


    private FieldDocument createFieldDoc2 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-");
        fd.addTV("base", "dcacacdac",
                "[(0-1)s:d|_1$<i>0<i>1|<>:s$<b>64<i>0<i>1<i>1<b>0]"
                        + "[(1-2)s:c|_2$<i>1<i>2|<>:s$<b>64<i>1<i>2<i>2<b>0]"
                        + "[(2-3)s:a|_3$<i>2<i>3|<>:s$<b>64<i>2<i>4<i>4<b>0]"
                        + "[(3-4)s:c|_4$<i>3<i>4]"
                        + "[(4-5)s:a|_5$<i>4<i>5|<>:s$<b>64<i>4<i>6<i>6<b>0]"
                        + "[(5-6)s:c|_6$<i>5<i>6]"
                        + "[(6-7)s:d|_7$<i>6<i>7|<>:s$<b>64<i>6<i>7<i>7<b>0]"
                        + "[(7-8)s:a|_8$<i>7<i>8|<>:s$<b>64<i>7<i>9<i>9<b>0]"
                        + "[(8-9)s:c|_9$<i>8<i>9]");
        return fd;
    }


    /**
     * Distance Zero, unordered
     * There is a secondspan on the right side
     */
    @Test
    public void testCase1 () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc0());
        ki.commit();
        SpanQuery sq;
        sq = createQuery("s", "s:d", "s:c", 0, 0, false, true);
        kr = ki.search(sq, (short) 10);
        assertEquals(1, kr.getTotalResults());
        assertEquals(6, kr.getMatch(0).startPos);
        assertEquals(7, kr.getMatch(0).endPos);
    }


    /**
     * There is another firstspan within max distance
     * Unordered
     */
    @Test
    public void testCase2 () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc0());
        ki.commit();
        SpanQuery sq;

        sq = createQuery("s", "s:c", "s:d", 0, 0, false, true);
        kr = ki.search(sq, (short) 10);

        assertEquals(4, kr.getTotalResults());
        assertEquals(0, kr.getMatch(0).startPos);
        assertEquals(1, kr.getMatch(0).endPos);
        assertEquals(2, kr.getMatch(1).startPos);
        assertEquals(3, kr.getMatch(1).endPos);
        assertEquals(3, kr.getMatch(2).startPos);
        assertEquals(4, kr.getMatch(2).endPos);
        assertEquals(8, kr.getMatch(3).startPos);
        assertEquals(9, kr.getMatch(3).endPos);
    }


    /**
     * Distance 0-1, ordered, unordered
     */
    @Test
    public void testCase3 () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc0());
        ki.commit();
        SpanQuery sq;
        // unordered
        sq = createQuery("s", "s:c", "s:e", 0, 1, false, true);
        kr = ki.search(sq, (short) 10);
        assertEquals(1, kr.getTotalResults());
        assertEquals(5, kr.getMatch(0).startPos);
        assertEquals(6, kr.getMatch(0).endPos);

        //ordered 
        sq = createQuery("s", "s:c", "s:e", 0, 1, true, true);
        kr = ki.search(sq, (short) 10);
        assertEquals(3, kr.getTotalResults());
        assertEquals(2, kr.getMatch(0).startPos);
        assertEquals(3, kr.getMatch(0).endPos);
        assertEquals(3, kr.getMatch(1).startPos);
        assertEquals(4, kr.getMatch(1).endPos);
    }


    /**
     * Multiple documents, ordered
     * No more secondspans, but there is still a firstspan
     */
    @Test
    public void testCase4 () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc0());
        ki.addDoc(createFieldDoc1());
        ki.commit();
        SpanQuery sq;

        sq = createQuery("s", "s:d", "s:e", 1, 1, true, true);
        kr = ki.search(sq, (short) 10);

        assertEquals(3, kr.getTotalResults());
        assertEquals(4, kr.getMatch(0).startPos);
        assertEquals(5, kr.getMatch(0).endPos);
        assertEquals(1, kr.getMatch(1).getLocalDocID());
        assertEquals(2, kr.getMatch(1).startPos);
        assertEquals(3, kr.getMatch(1).endPos);
        assertEquals(8, kr.getMatch(2).startPos);
        assertEquals(9, kr.getMatch(2).endPos);
    }


    /**
     * Skip to
     */
    @Test
    public void testCase5 () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc0());
        ki.addDoc(createFieldDoc1());
        ki.addDoc(createFieldDoc0());
        ki.addDoc(createFieldDoc2());
        ki.commit();

        SpanQuery sq = createQuery("s", "s:c", "s:d", 1, 1, false, true);
        kr = ki.search(sq, (short) 10);

        assertEquals(3, kr.getTotalResults());
        assertEquals(3, kr.getMatch(2).getLocalDocID());
        assertEquals(3, kr.getMatch(2).startPos);
        assertEquals(4, kr.getMatch(2).endPos);

        sq = new SpanNextQuery(
                createQuery("s", "s:c", "s:d", 1, 1, false, true),
                new SpanTermQuery(new Term("base", "s:a")));

        kr = ki.search(sq, (short) 10);
        assertEquals(1, kr.getTotalResults());
        assertEquals(3, kr.getMatch(0).getLocalDocID());
        assertEquals(3, kr.getMatch(0).startPos);
        assertEquals(5, kr.getMatch(0).endPos);
    }
}
