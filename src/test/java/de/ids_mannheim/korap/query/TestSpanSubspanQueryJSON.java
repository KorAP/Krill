package de.ids_mannheim.korap.query;

import static de.ids_mannheim.korap.TestSimple.getJSONQuery;
import static org.junit.Assert.assertEquals;

import org.apache.lucene.search.spans.SpanQuery;
import org.junit.Test;

import de.ids_mannheim.korap.query.wrap.SpanQueryWrapper;
import de.ids_mannheim.korap.util.QueryException;

/**
 * @author margaretha, diewald
 */
public class TestSpanSubspanQueryJSON {

    @Test
    public void testCase1() throws QueryException {
        String filepath = getClass().getResource("/queries/submatch/1.jsonld")
                .getFile();
        SpanQueryWrapper sqwi = getJSONQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(sq.toString(),
                "subspan(spanContain(<tokens:s />, tokens:tt/l:Haus),1,4)");
    }

    @Test
    public void testCase2() throws QueryException {
        String filepath = getClass().getResource("/queries/submatch/2.jsonld")
            .getFile();
        SpanQueryWrapper sqwi = getJSONQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(sq.toString(), "subspan(<tokens:s />,1,4)");
    }

    @Test
    public void testCase3() throws QueryException {
        String filepath = getClass().getResource("/queries/submatch/3.jsonld")
                .getFile();
        SpanQueryWrapper sqwi = getJSONQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(sq.toString(), "subspan(<tokens:s />,1,0)");
    }

    @Test
    public void testCaseWrapped() throws QueryException {
        String filepath = getClass().getResource("/queries/submatch/wrapped.jsonld")
                .getFile();
        SpanQueryWrapper sqwi = getJSONQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(sq.toString(), "shrink(129: spanElementDistance({129: tokens:s:der},"+
                     " {129: subspan(<tokens:s />,0,1)}, [(s[0:0], ordered, notExcluded)]))");
    }


    @Test
    public void testCaseEmbedded() throws QueryException {
        String filepath = getClass().getResource("/queries/submatch/embedded.jsonld")
                .getFile();
        SpanQueryWrapper sqwi = getJSONQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(sq.toString(), "spanNext({1: tokens:s:die},"+
                     " {1: subspan(spanExpansion(tokens:s:der, []{1, 100}, right),2,3)})");
    }

    @Test
    public void testCaseEmbeddedNull() throws QueryException {
        String filepath = getClass().getResource("/queries/submatch/embedded-null.jsonld")
                .getFile();
        SpanQueryWrapper sqwi = getJSONQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(sq.toString(), "tokens:s:die");
    }

    @Test
    public void testCaseEmbeddedValidEmpty() throws QueryException {
        String filepath = getClass().getResource("/queries/submatch/embedded-valid-empty.jsonld")
                .getFile();
        SpanQueryWrapper sqwi = getJSONQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(sq.toString(), "??? (Known issue)");
    }
}
