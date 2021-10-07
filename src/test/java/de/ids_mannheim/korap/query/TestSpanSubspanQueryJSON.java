package de.ids_mannheim.korap.query;

import static de.ids_mannheim.korap.TestSimple.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.lucene.search.spans.SpanQuery;
import org.junit.Test;

import de.ids_mannheim.korap.KrillQuery;
import de.ids_mannheim.korap.query.wrap.SpanQueryWrapper;
import de.ids_mannheim.korap.util.QueryException;

/**
 * @author margaretha, diewald
 */
public class TestSpanSubspanQueryJSON {

    @Test
    public void testTermQuery () throws QueryException {
        // subspan(tokens:tt/l:Haus, 0, 1)
        String filepath = getClass()
                .getResource("/queries/submatch/termquery.jsonld").getFile();
        SpanQueryWrapper sqwi = getJsonQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals("tokens:tt/l:Haus", sq.toString());
    }


    @Test
    public void testTermStartOffset () throws QueryException {
        // subspan(tokens:tt/l:Haus, -1, 0)
        String filepath = getClass()
                .getResource("/queries/submatch/term-start-offset.jsonld")
                .getFile();
        SpanQueryWrapper sqwi = getJsonQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals("tokens:tt/l:Haus", sq.toString());
    }


    @Test
    public void testTermNull () throws QueryException {
        // subspan(tokens:tt/l:Haus, 1, 1)
        String filepath = getClass()
                .getResource("/queries/submatch/term-null.jsonld").getFile();
        SpanQueryWrapper sqwi = getJsonQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(null, sq);
    }


    @Test
    public void testElementQuery () throws QueryException {
        // submatch(1,4:<s>)
        String filepath = getClass()
                .getResource("/queries/submatch/simpleElement.jsonld")
                .getFile();
        // SpanQueryWrapper sqwi = getJsonQuery(filepath);

        String jsonPQuery = getJsonString(filepath);
        KrillQuery kq = new KrillQuery("tokens");
        SpanQueryWrapper sqwi = kq.fromKoral(jsonPQuery);

        assertTrue(!kq.hasErrors());
        assertTrue(!kq.hasWarnings());
        assertTrue(!kq.hasMessages());
        
        SpanQuery sq = sqwi.toQuery();
        assertEquals(
                "subspan(spanContain(<tokens:s />, tokens:tt/l:Haus), 1, 4)",
                sq.toString());
    }


    @Test
    public void testNoLength () throws QueryException {
        // submatch(1,:<s>)
        String filepath = getClass()
                .getResource("/queries/submatch/noLength.jsonld").getFile();
        SpanQueryWrapper sqwi = getJsonQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals("subspan(<tokens:s />, 1, 0)", sq.toString());
    }


    @Test
    public void testMinusStartOffset () throws QueryException {
        // submatch(-1,4:<s>)
        String filepath = getClass()
                .getResource("/queries/submatch/minusStart.jsonld").getFile();
        SpanQueryWrapper sqwi = getJsonQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals("subspan(<tokens:s />, -1, 4)", sq.toString());
    }


    @Test
    public void testEmptyMinusStartOffset () throws QueryException {
        // no optimization
        // submatch(-1,4:der []{1,8})
        String filepath = getClass()
                .getResource("/queries/submatch/empty-minusStart.jsonld")
                .getFile();
        SpanQueryWrapper sqwi = getJsonQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(
                "subspan(spanExpansion(tokens:s:der, []{1, 8}, right), -1, 4)",
                sq.toString());
    }


    @Test
    public void testEmptyMax () throws QueryException {
        // no optimization
        // submatch(1,2:der []{1,8})
        String filepath = getClass()
                .getResource("/queries/submatch/empty-max.jsonld").getFile();
        SpanQueryWrapper sqwi = getJsonQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(
                "subspan(spanExpansion(tokens:s:der, []{1, 8}, right), 1, 2)",
                sq.toString());
    }


    @Test
    public void testCaseEmptyWrapped () throws QueryException {
        String filepath = getClass()
                .getResource("/queries/submatch/wrapped.jsonld").getFile();
        SpanQueryWrapper sqwi = getJsonQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(
                "focus(129: spanElementDistance({129: tokens:s:der}, {129: subspan"
                        + "(<tokens:s />, 0, 1)}, [(base/s:s[0:0], ordered, notExcluded)]))",
                sq.toString());
    }


    @Test
    public void testCaseEmptyEmbedded () throws QueryException {
        // die subspan(der []{1,}, 2,3)
        String filepath = getClass()
                .getResource("/queries/submatch/embedded.jsonld").getFile();
        SpanQueryWrapper sqwi = getJsonQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(
                "spanNext({1: tokens:s:die}, {1: subspan(spanExpansion("
                        + "tokens:s:der, []{1, 100}, right), 2, 3)})",
                sq.toString());
    }


    @Test
    public void testCaseEmptyEmbeddedNull () throws QueryException {
        // die subspan([],5,6)
        // start offset is bigger than the original span
        String filepath = getClass()
                .getResource("/queries/submatch/embedded-null.jsonld")
                .getFile();
        SpanQueryWrapper sqwi = getJsonQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals("tokens:s:die", sq.toString());
    }


    @Test
    public void testCaseEmptyEmbeddedValid () throws QueryException {
        // die subspan([]{0,5},2)
        String filepath = getClass()
                .getResource("/queries/submatch/embedded-valid-empty.jsonld")
                .getFile();
        SpanQueryWrapper sqwi = getJsonQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(
                "focus(254: spanContain(<tokens:base/s:t />, {254: spanExpansion(tokens:s:die, []{2, 5}, right)}))",
                sq.toString());
    }


    @Test
    public void testNegativeToken () throws QueryException {
        // submatch(0,1:[base != Baum])
        String filepath = getClass()
                .getResource("/queries/submatch/negative-token.jsonld")
                .getFile();
        SpanQueryWrapper sqwi = getJsonQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals("tokens:l:Baum", sq.toString());
    }


    @Test
    public void testNegativeSequence () throws QueryException {
        // das submatch(0,1:[base != Baum])
        String filepath = getClass()
                .getResource("/queries/submatch/negative-seq.jsonld").getFile();
        SpanQueryWrapper sqwi = getJsonQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(
                "focus(254: spanContain(<tokens:base/s:t />, {254: spanExpansion(tokens:s:das, !tokens:l:Baum{1, 1}, right)}))",
                sq.toString());
    }


    @Test
    public void testNegativeSequenceWithClass () throws QueryException {
        // das {1:submatch(0,1:[base != Baum])}
        String filepath = getClass()
                .getResource("/queries/submatch/negative-sequence-class.jsonld")
                .getFile();
        SpanQueryWrapper sqwi = getJsonQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(
                "focus(254: spanContain(<tokens:base/s:t />, {254: spanExpansion(tokens:s:das, !tokens:l:Baum{1, 1}, right, class:1)}))",
                sq.toString());
    }


    @Test
    public void testNegativeEmbeddedSequence () throws QueryException {
        // submatch(1,1:das [base != Baum])
        String filepath = getClass()
                .getResource("/queries/submatch/embedded-negative-seq.jsonld")
                .getFile();
        SpanQueryWrapper sqwi = getJsonQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(
                "subspan(spanExpansion(tokens:s:das, !tokens:l:Baum{1, 1}, right), 1, 1)",
                sq.toString());
    }


    @Test
    public void testNegativeEmbeddedSequenceWithClass () throws QueryException {
        // submatch(0,1:{1:[base != Baum] dass})
        String filepath = getClass()
                .getResource(
                        "/queries/submatch/embedded-negative-class-seq.jsonld")
                .getFile();
        SpanQueryWrapper sqwi = getJsonQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(
                "subspan({1: spanExpansion(tokens:s:dass, !tokens:l:Baum{1, 1}, left)}, 0, 1)",
                sq.toString());
    }


    @Test
    public void testEmbeddedNegativeRepetition () throws QueryException {
        // submatch(1,1:das [base != Baum]{1,3})
        String filepath = getClass()
                .getResource(
                        "/queries/submatch/embedded-negative-repetition.jsonld")
                .getFile();
        SpanQueryWrapper sqwi = getJsonQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(
                "subspan(spanExpansion(tokens:s:das, !tokens:l:Baum{1, 3}, right), 1, 1)",
                sq.toString());
    }


    @Test
    public void testNegativeRepetition () throws QueryException {
        // das submatch(1,4:[base != Baum]{1,3})
        String filepath = getClass()
                .getResource("/queries/submatch/negative-repetition.jsonld")
                .getFile();
        SpanQueryWrapper sqwi = getJsonQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(
                "focus(254: spanContain(<tokens:base/s:t />, {254: spanExpansion(tokens:s:das, !tokens:l:Baum{2, 2}, right)}))",
                sq.toString());
    }
}
