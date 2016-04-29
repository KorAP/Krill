package de.ids_mannheim.korap.index;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.junit.Test;

import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.query.DistanceConstraint;
import de.ids_mannheim.korap.query.SpanDistanceQuery;
import de.ids_mannheim.korap.query.SpanElementQuery;
import de.ids_mannheim.korap.query.SpanSubspanQuery;

/*
 * @author margaretha
 * @author diewald
 */
public class TestSubSpanIndex {

    Result kr;
    KrillIndex ki;

    public TestSubSpanIndex () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(getClass().getResourceAsStream("/wiki/00001.json.gz"), true);
        ki.commit();
    }

    @Test
    public void testCase1 () throws IOException {
        SpanDistanceQuery sdq = new SpanDistanceQuery(new SpanTermQuery(
                new Term("tokens", "tt/p:NN")), new SpanTermQuery(new Term(
                "tokens", "tt/p:VAFIN")), new DistanceConstraint(5, 5, true,
                false), true);

        SpanSubspanQuery ssq = new SpanSubspanQuery(sdq, 0, 2, true);
        kr = ki.search(ssq, (short) 10);

        assertEquals((long) 8, kr.getTotalResults());
        assertEquals(35, kr.getMatch(0).getStartPos());
        assertEquals(37, kr.getMatch(0).getEndPos());
        assertEquals(179, kr.getMatch(1).getStartPos());
        assertEquals(181, kr.getMatch(1).getEndPos());

        ssq = new SpanSubspanQuery(sdq, -2, 2, true);
        kr = ki.search(ssq, (short) 10);

        assertEquals(39, kr.getMatch(0).getStartPos());
        assertEquals(41, kr.getMatch(0).getEndPos());
        assertEquals(183, kr.getMatch(1).getStartPos());
        assertEquals(185, kr.getMatch(1).getEndPos());

        /*
         * for (Match km : kr.getMatches()){
         * System.out.println(km.getStartPos() +","+km.getEndPos()
         * +km.getSnippetBrackets()); }
         */
    }


    @Test
    public void testCase2 () {
        SpanDistanceQuery sdq = new SpanDistanceQuery(new SpanTermQuery(
                new Term("tokens", "tt/p:NN")), new SpanTermQuery(new Term(
                "tokens", "tt/p:VAFIN")), new DistanceConstraint(5, 5, true,
                false), true);

        // the subspan length is longer than the span length
        SpanSubspanQuery ssq = new SpanSubspanQuery(sdq, 0, 7, true);
        kr = ki.search(ssq, (short) 10);

        assertEquals(35, kr.getMatch(0).getStartPos());
        assertEquals(41, kr.getMatch(0).getEndPos());
        assertEquals(179, kr.getMatch(1).getStartPos());
        assertEquals(185, kr.getMatch(1).getEndPos());

        // the subspan start is before the span start
        ssq = new SpanSubspanQuery(sdq, -7, 4, true);
        kr = ki.search(ssq, (short) 10);

        assertEquals((long) 8, kr.getTotalResults());
        assertEquals(35, kr.getMatch(0).getStartPos());
        assertEquals(39, kr.getMatch(0).getEndPos());
        assertEquals(179, kr.getMatch(1).getStartPos());
        assertEquals(183, kr.getMatch(1).getEndPos());

    }


    // Length 0
    @Test
    public void testCase3 () {
        SpanDistanceQuery sdq = new SpanDistanceQuery(new SpanTermQuery(
                new Term("tokens", "tt/p:NN")), new SpanTermQuery(new Term(
                "tokens", "tt/p:VAFIN")), new DistanceConstraint(5, 5, true,
                false), true);

        SpanSubspanQuery ssq = new SpanSubspanQuery(sdq, 3, 0, true);
        kr = ki.search(ssq, (short) 10);

        assertEquals(38, kr.getMatch(0).getStartPos());
        assertEquals(41, kr.getMatch(0).getEndPos());
        assertEquals(182, kr.getMatch(1).getStartPos());
        assertEquals(185, kr.getMatch(1).getEndPos());

        ssq = new SpanSubspanQuery(sdq, -2, 0, true);
        kr = ki.search(ssq, (short) 10);

        assertEquals(39, kr.getMatch(0).getStartPos());
        assertEquals(41, kr.getMatch(0).getEndPos());
        assertEquals(183, kr.getMatch(1).getStartPos());
        assertEquals(185, kr.getMatch(1).getEndPos());

        // for (Match km : kr.getMatches()) {
        // System.out.println(km.getStartPos() + "," + km.getEndPos()
        // + km.getSnippetBrackets());
        // }
    }

    // Negative SubSpanQuery
    @Test
    public void testCaseNegativeSubSpan () throws IOException {
        KrillIndex ki = new KrillIndex();
        FieldDocument fd = new FieldDocument();

        fd.addTV(
                "base",
                // <x>a <x>b </x>c </x>
                "a b c ",
                "[(0-1)s:a|i:a|_0$<i>0<i>2|<>:x$<b>64<i>0<i>6<i>3<b>0]" +
                "[(1-2)s:b|i:b|_1$<i>2<i>4|<>:x$<b>64<i>2<i>4<i>2<b>1]" +
                "[(3-4)s:c|i:c|_2$<i>4<i>6]");
        ki.addDoc(fd);
        ki.commit();
        SpanSubspanQuery ssq = new SpanSubspanQuery(new SpanElementQuery("base", "x"), -1, 1, true);
        kr = ki.search(ssq, (short) 10);
        
        assertEquals(2, kr.getTotalResults());
        assertEquals("a [b ]c ", kr.getMatch(0).getSnippetBrackets());
        assertEquals("a b [c ]", kr.getMatch(1).getSnippetBrackets());
    };
};
