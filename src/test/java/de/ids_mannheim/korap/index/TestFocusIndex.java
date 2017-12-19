package de.ids_mannheim.korap.index;


import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.junit.Test;

import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.constants.RelationDirection;
import de.ids_mannheim.korap.query.SpanFocusQuery;
import de.ids_mannheim.korap.query.SpanNextQuery;
import de.ids_mannheim.korap.query.SpanRelationQuery;
import de.ids_mannheim.korap.response.Match;
import de.ids_mannheim.korap.response.Result;

public class TestFocusIndex {
    private KrillIndex ki;
    private Result kr;


    public TestFocusIndex () throws IOException {
        ki = new KrillIndex();
    }


    /**
     * Check Skipto focus spans
     */
    @Test
    public void testCase12 () throws IOException {
        ki.addDoc(TestRelationIndex.createFieldDoc0());
        ki.addDoc(TestRelationIndex.createFieldDoc1());
        ki.commit();
        SpanRelationQuery sq = new SpanRelationQuery(
                new SpanTermQuery(new Term("base", ">:xip/syntax-dep_rel")),
                true, RelationDirection.RIGHT);
        sq.setSourceClass((byte) 1);

        SpanFocusQuery sfq = new SpanFocusQuery(sq, (byte) 1);
        sfq.setSorted(false);
        SpanTermQuery stq = new SpanTermQuery(new Term("base", "s:c"));
        SpanNextQuery snq = new SpanNextQuery(stq, sfq);

        kr = ki.search(snq, (short) 20);

        assertEquals(0, kr.getMatch(0).getStartPos());
        assertEquals(2, kr.getMatch(0).getEndPos());
        assertEquals(5, kr.getMatch(1).getStartPos());
        assertEquals(9, kr.getMatch(1).getEndPos());
        // for (Match m : kr.getMatches()) {
        // System.out.println(m.getStartPos() + " " + m.getEndPos());
        // }
    }
}
