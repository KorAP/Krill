package de.ids_mannheim.korap.index;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.spans.SpanMultiTermQueryWrapper;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.junit.Test;

import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.query.DistanceConstraint;
import de.ids_mannheim.korap.query.SpanClassQuery;
import de.ids_mannheim.korap.query.SpanMultipleDistanceQuery;
import de.ids_mannheim.korap.response.Result;

public class TestWildcardIndex {

    private SpanTermQuery sq;
    private KrillIndex ki;
    private Result kr;
    private ArrayList<DistanceConstraint> constraints;


    public TestWildcardIndex () {
        // &Erfahrung
        sq = new SpanTermQuery(new Term("tokens", "tt/l:Erfahrung"));

        // /+w1:2,s0
        constraints = new ArrayList<DistanceConstraint>();
        constraints.add(TestMultipleDistanceIndex.createConstraint("w", 1, 2,
                true, false));
        constraints.add(TestMultipleDistanceIndex.createConstraint("tokens",
                "base/s:s", 0, 0, true, false));
    }


    private FieldDocument createFieldDoc1 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-1");
        fd.addTV("tokens", "text",
                "[(0-1)s:meine|_1$<i>0<i>1|<>:base/s:s$<b>64<i>0<i>9<i>10<b>0]"
                        + "[(1-2)tt/l:Erfahrung|_2$<i>1<i>2]"
                        + "[(2-3)s:meiner|_3$<i>2<i>3]"
                        + "[(3-4)tt/l:Erfahrung|_4$<i>3<i>4]"
                        + "[(4-5)s:mein|_5$<i>4<i>5]"
                        + "[(5-6)tt/l:Erfahrung|_6$<i>5<i>6]"
                        + "[(6-7)s:meinem|_7$<i>6<i>7]"
                        + "[(7-8)tt/l:Erfahrung|_8$<i>7<i>8]"
                        + "[(8-9)s:meinen|_9$<i>8<i>9]"
                        + "[(9-10)tt/l:Erfahrung|_10$<i>9<i>10]");
        return fd;
    }


    @Test
    public void testWildcardStarWithCollection () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc1());
        ki.commit();
        // meine*
        WildcardQuery wcquery =
                new WildcardQuery(new Term("tokens", "s:meine*"));
        SpanMultiTermQueryWrapper<WildcardQuery> mtq =
                new SpanMultiTermQueryWrapper<WildcardQuery>(wcquery);

        // meine* /+w1:2,s0 &Erfahrung
        SpanQuery mdsq = new SpanMultipleDistanceQuery(
                new SpanClassQuery(mtq, (byte) 129),
                new SpanClassQuery(sq, (byte) 129), constraints, true, true);

        kr = ki.search(mdsq, (short) 10);
        assertEquals(4, kr.getMatches().size());
    }


    @Test
    public void testWildcardQuestionMark1 () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc1());
        ki.commit();

        // Wildcard ? means regex . (expects exactly one character)
        SpanMultiTermQueryWrapper<WildcardQuery> mtq =
                new SpanMultiTermQueryWrapper<WildcardQuery>(
                        new WildcardQuery(new Term("tokens", "s:meine?")));
        SpanMultipleDistanceQuery mdsq = new SpanMultipleDistanceQuery(
                new SpanClassQuery(mtq, (byte) 129),
                new SpanClassQuery(sq, (byte) 129), constraints, true, true);

        kr = ki.search(mdsq, (short) 10);
        assertEquals(3, kr.getMatches().size());

    }


    @Test
    public void testWildcardQuestionMark2 () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc1());
        ki.commit();

        // Wildcard ? means regex . (expects exactly one character)
        SpanMultiTermQueryWrapper<WildcardQuery> mtq =
                new SpanMultiTermQueryWrapper<WildcardQuery>(
                        new WildcardQuery(new Term("tokens", "s:mein?")));
        SpanMultipleDistanceQuery mdsq = new SpanMultipleDistanceQuery(
                new SpanClassQuery(mtq, (byte) 129),
                new SpanClassQuery(sq, (byte) 129), constraints, true, true);

        kr = ki.search(mdsq, (short) 10);
        assertEquals(1, kr.getMatches().size());

    }


    @Test
    public void testWildcardPlusWithCollection () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc1());
        ki.commit();
        // mein+ /+w1:2,s0 &Erfahrung
        SpanMultiTermQueryWrapper<WildcardQuery> mtq =
                new SpanMultiTermQueryWrapper<WildcardQuery>(
                        new WildcardQuery(new Term("tokens", "s:mein+")));


        // Just to make sure, Lucene internal queries treat SpanOr([]) correctly
        SpanQuery soq = new SpanNearQuery(new SpanQuery[] { mtq, sq }, 1, true);
        kr = ki.search(soq, (short) 10);
        // As described in http://korap.github.io/Koral/, '+' is not a valid wildcard
        assertEquals(0, kr.getMatches().size());



        // Check the reported classed query
        SpanMultipleDistanceQuery mdsq = new SpanMultipleDistanceQuery(
                new SpanClassQuery(mtq, (byte) 129),
                new SpanClassQuery(sq, (byte) 129), constraints, true, true);

        kr = ki.search(mdsq, (short) 10);
        assertEquals(0, kr.getMatches().size());


        // Check multiple distance query
        mdsq = new SpanMultipleDistanceQuery(mtq, sq, constraints, true, true);

        kr = ki.search(mdsq, (short) 10);
        assertEquals(0, kr.getMatches().size());
    }
}
