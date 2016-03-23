package de.ids_mannheim.korap.index;

import static de.ids_mannheim.korap.TestSimple.getJSONQuery;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.junit.Test;

import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.query.DistanceConstraint;
import de.ids_mannheim.korap.query.SpanClassQuery;
import de.ids_mannheim.korap.query.SpanElementQuery;
import de.ids_mannheim.korap.query.SpanFocusQuery;
import de.ids_mannheim.korap.query.SpanWithinQuery;
import de.ids_mannheim.korap.query.wrap.SpanQueryWrapper;
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.response.Match;
import de.ids_mannheim.korap.util.QueryException;

public class TestFocusIndex {
    private KrillIndex ki;
    private Result kr;

    @Test
    public void testFocusSorting () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc());
        ki.commit();

        SpanElementQuery elemX = new SpanElementQuery("tokens", "x");

        assertEquals("<tokens:x />", elemX.toString());

        kr = ki.search(elemX, (short) 10);
        assertEquals("[abc]d", kr.getMatch(0).getSnippetBrackets());
        assertEquals("a[bcd]", kr.getMatch(1).getSnippetBrackets());
        assertEquals(2, kr.getTotalResults());

        SpanQuery termB = new SpanTermQuery(new Term("tokens", "s:b"));
        SpanQuery termC = new SpanTermQuery(new Term("tokens", "s:c"));

        SpanQuery classB = new SpanClassQuery(termB, (byte) 1);
        SpanQuery classC = new SpanClassQuery(termC, (byte) 1);

        SpanQuery within = new SpanWithinQuery(elemX, classB);

        kr = ki.search(within, (short) 10);
        assertEquals("[a{1:b}c]d", kr.getMatch(0).getSnippetBrackets());
        assertEquals("a[{1:b}cd]", kr.getMatch(1).getSnippetBrackets());
        assertEquals(2, kr.getTotalResults());

        SpanQuery or = new SpanOrQuery(classB, classC);
        within = new SpanWithinQuery(elemX, or);

        kr = ki.search(within, (short) 10);
        assertEquals("[a{1:b}c]d", kr.getMatch(0).getSnippetBrackets());
        assertEquals("[ab{1:c}]d", kr.getMatch(1).getSnippetBrackets());
        assertEquals("a[{1:b}cd]", kr.getMatch(2).getSnippetBrackets());
        assertEquals("a[b{1:c}d]", kr.getMatch(3).getSnippetBrackets());
        assertEquals(4, kr.getTotalResults());


        SpanFocusQuery focus = new SpanFocusQuery(within, (byte) 1);
        kr = ki.search(focus, (short) 10);
        assertEquals("a[{1:b}]cd", kr.getMatch(0).getSnippetBrackets());
        assertEquals("a[{1:b}]cd", kr.getMatch(1).getSnippetBrackets());
        assertEquals("ab[{1:c}]d", kr.getMatch(2).getSnippetBrackets());
        assertEquals("ab[{1:c}]d", kr.getMatch(3).getSnippetBrackets());
        assertEquals(4, kr.getTotalResults());
    }

    public static FieldDocument createFieldDoc () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-0");
        fd.addTV(
                "tokens",
                "abcd",

                "[(0-1)s:a|_0$<i>0<i>1|"
                + "<>:x$<b>64<i>0<i>3<i>3<b>0]"
                + "[(1-2)s:b|_1$<i>1<i>2|"
                + "<>:x$<b>64<i>1<i>4<i>4<b>0]"
                + "[(2-3)s:c|_2$<i>2<i>3]"
                + "[(3-4)s:d|_3$<i>3<i>4]"
                 );
        
        return fd;
    }
}
