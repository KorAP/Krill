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
public class TestUnorderedElementDistanceIndex {

    private KrillIndex ki;
    private Result kr;


    private FieldDocument createFieldDoc0 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-0");
        fd.addTV("base", "text",
                "[(0-1)s:b|s:c|_1$<i>0<i>1|<>:s$<b>64<i>0<i>2<i>1]"
                        + "[(1-2)s:b|_2$<i>1<i>2]"
                        + "[(2-3)s:b|_3$<i>2<i>3|<>:s$<b>64<i>2<i>3<i>3]"
                        + "[(3-4)s:c|_4$<i>3<i>4|<>:s$<b>64<i>3<i>4<i>4]"
                        + "[(4-5)s:b|_5$<i>4<i>5|<>:s$<b>64<i>4<i>5<i>5]"
                        + "[(5-6)s:b|_6$<i>5<i>6]"
                        + "[(6-7)s:c|_7$<i>6<i>7|<>:s$<b>64<i>6<i>7<i>7]");
        return fd;
    }


    private FieldDocument createFieldDoc1 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-1");
        fd.addTV("base", "text",
                "[(0-1)s:b|_1$<i>0<i>1|<>:s$<b>64<i>0<i>2<i>1]"
                        + "[(1-2)s:c|_2$<i>1<i>2|<>:s$<b>64<i>1<i>2<i>4]"
                        + "[(2-3)s:e|_3$<i>2<i>3]" + "[(3-4)s:c|_4$<i>3<i>4]"
                        + "[(4-5)s:b|_5$<i>4<i>5|<>:s$<b>64<i>4<i>5<i>7]"
                        + "[(5-6)s:e|_6$<i>5<i>6]" + "[(6-7)s:e|_7$<i>6<i>7]");
        return fd;
    }


    private FieldDocument createFieldDoc2 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-2");
        fd.addTV("base", "text",
                "[(0-1)s:e|_1$<i>0<i>1|<>:p$<b>64<i>0<i>2<i>1]"
                        + "[(1-2)s:e|_2$<i>1<i>2|<>:p$<b>64<i>1<i>2<i>2]"
                        + "[(2-3)s:c|_3$<i>2<i>3|<>:p$<b>64<i>2<i>3<i>3]"
                        + "[(3-4)s:e|_4$<i>3<i>4|<>:p$<b>64<i>3<i>4<i>4]"
                        + "[(4-5)s:b|_5$<i>4<i>5|<>:p$<b>64<i>4<i>5<i>5]"
                        + "[(5-6)s:c|_6$<i>5<i>6|<>:p$<b>64<i>5<i>6<i>6]"
                        + "[(6-7)s:e|_7$<i>6<i>7|<>:p$<b>64<i>6<i>7<i>7]"
                        + "[(7-8)s:b|_8$<i>7<i>8|<>:p$<b>64<i>7<i>8<i>8]");
        return fd;
    }


    private FieldDocument createFieldDoc3 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-3");
        fd.addTV("base", "text",
                "[(0-1)s:a|_1$<i>0<i>1|<>:s$<b>64<i>0<i>2<i>1]"
                        + "[(1-2)s:d|_2$<i>1<i>2|<>:s$<b>64<i>1<i>2<i>3]"
                        + "[(2-3)s:e|_3$<i>2<i>3]");
        return fd;
    }


    private FieldDocument createFieldDoc4 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-4");
        fd.addTV("base", "text",
                "[(0-1)s:c|_1$<i>0<i>1|<>:s$<b>64<i>0<i>2<i>2]"
                        + "[(1-2)s:e|_2$<i>1<i>2]"
                        + "[(2-3)s:b|_3$<i>2<i>3|<>:s$<b>64<i>2<i>3<i>3]"
                        + "[(3-4)s:e|_4$<i>3<i>4|<>:s$<b>64<i>3<i>4<i>4]");
        return fd;
    }


    public SpanQuery createQuery (String elementType, String x, String y,
            int minDistance, int maxDistance, boolean isOrdered) {
        SpanElementQuery e = new SpanElementQuery("base", elementType);
        return new SpanDistanceQuery(new SpanTermQuery(new Term("base", x)),
                new SpanTermQuery(new Term("base", y)), new DistanceConstraint(
                        e, minDistance, maxDistance, isOrdered, false), true);
    }


    /**
     * Only terms within an element are matched.
     * */
    @Test
    public void testCase1 () throws IOException {
        //System.out.println("testCase1");
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc0());
        ki.commit();

        SpanQuery sq;
        sq = createQuery("s", "s:b", "s:c", 0, 1, false);
        kr = ki.search(sq, (short) 10);

        assertEquals((long) 5, kr.getTotalResults());
        assertEquals(0, kr.getMatch(0).startPos);
        assertEquals(1, kr.getMatch(0).endPos);
        assertEquals(0, kr.getMatch(1).startPos);
        assertEquals(3, kr.getMatch(1).endPos);
        assertEquals(2, kr.getMatch(2).startPos);
        assertEquals(4, kr.getMatch(2).endPos);
        assertEquals(3, kr.getMatch(3).startPos);
        assertEquals(5, kr.getMatch(3).endPos);
        assertEquals(4, kr.getMatch(4).startPos);
        assertEquals(7, kr.getMatch(4).endPos);

    }


    /**
     * Ensure same doc.
     * In the beginning, first and second spans are already too far
     * from each other
     * (one-list-empty case, both-list-empty-case).
     * */
    @Test
    public void testCase2 () throws IOException {
        //System.out.println("testCase2");
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc0());
        ki.addDoc(createFieldDoc1());
        ki.addDoc(createFieldDoc2());
        ki.commit();

        SpanQuery sq;
        sq = createQuery("p", "s:b", "s:e", 0, 2, false);
        kr = ki.search(sq, (short) 10);

        assertEquals((long) 3, kr.getTotalResults());
        assertEquals(2, kr.getMatch(0).getLocalDocID());
        assertEquals(3, kr.getMatch(0).startPos);
        assertEquals(5, kr.getMatch(0).endPos);
        assertEquals(4, kr.getMatch(1).startPos);
        assertEquals(7, kr.getMatch(1).endPos);
        assertEquals(6, kr.getMatch(2).startPos);
        assertEquals(8, kr.getMatch(2).endPos);
    }


    /**
     * Multiple occurrences in an element.
     * */
    @Test
    public void testCase3 () throws IOException {
        //System.out.println("testCase3");
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc1());
        ki.commit();

        SpanQuery sq;
        sq = createQuery("s", "s:c", "s:e", 1, 2, false);
        kr = ki.search(sq, (short) 10);

        assertEquals((long) 4, kr.getTotalResults());
        assertEquals(1, kr.getMatch(0).startPos);
        assertEquals(6, kr.getMatch(0).endPos);
        assertEquals(1, kr.getMatch(1).startPos);
        assertEquals(7, kr.getMatch(1).endPos);
        assertEquals(3, kr.getMatch(2).startPos);
        assertEquals(6, kr.getMatch(2).endPos);
        assertEquals(3, kr.getMatch(3).startPos);
        assertEquals(7, kr.getMatch(3).endPos);
    }


    /**
     * Multiple documents
     * Skip to
     */
    @Test
    public void testCase4 () throws IOException {
        //System.out.println("testCase4");
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc1());
        ki.addDoc(createFieldDoc0());
        ki.addDoc(createFieldDoc3());
        ki.addDoc(createFieldDoc4());
        ki.commit();

        SpanQuery sq, edq;
        edq = createQuery("s", "s:b", "s:c", 1, 1, false);

        sq = new SpanNextQuery(edq, new SpanTermQuery(new Term("base", "s:e")));

        kr = ki.search(sq, (short) 10);

        assertEquals((long) 4, kr.getTotalResults());
        assertEquals(0, kr.getMatch(0).startPos);
        assertEquals(3, kr.getMatch(0).endPos);
        assertEquals(1, kr.getMatch(1).startPos);
        assertEquals(6, kr.getMatch(1).endPos);
        assertEquals(3, kr.getMatch(2).startPos);
        assertEquals(6, kr.getMatch(2).endPos);
        assertEquals(3, kr.getMatch(3).getLocalDocID());
        assertEquals(0, kr.getMatch(3).startPos);
        assertEquals(4, kr.getMatch(3).endPos);

    }


    /** Next */
    @Test
    public void testCase5 () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc0());
        ki.commit();

        SpanQuery sq, edq;
        edq = createQuery("s", "s:b", "s:c", 0, 2, false);
        kr = ki.search(edq, (short) 10);
        assertEquals((long) 6, kr.getTotalResults());

        sq = new SpanNextQuery(new SpanTermQuery(new Term("base", "s:b")), edq);
        kr = ki.search(sq, (short) 10);
        assertEquals((long) 2, kr.getTotalResults());
        assertEquals(1, kr.getMatch(0).getStartPos());
        assertEquals(4, kr.getMatch(0).getEndPos());
        assertEquals(2, kr.getMatch(1).getStartPos());
        assertEquals(5, kr.getMatch(1).getEndPos());

        //        System.out.print(kr.getTotalResults()+"\n");
        //		for (int i=0; i< kr.getTotalResults(); i++){
        //			System.out.println(
        //				kr.match(i).getLocalDocID()+" "+
        //				kr.match(i).startPos + " " +
        //				kr.match(i).endPos
        //		    );
        //		}
    }
}
