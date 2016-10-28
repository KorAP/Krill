package de.ids_mannheim.korap.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.ids_mannheim.korap.query.wrap.SpanQueryWrapper;
import de.ids_mannheim.korap.util.QueryException;


/**
 * @author margaretha
 *
 */
public class TestSpanSequenceQueryWithEmptyToken extends BaseTest {
    public TestSpanSequenceQueryWithEmptyToken () {
        path = "/queries/sequence/";
    }


    @Test
    public void testEmptyInSequence () throws QueryException {
        // der [] Mann?
        SpanQueryWrapper sqwi = jsonQueryFile("empty-token-in-sequence.jsonld");

        assertEquals("focus(254: spanContain(<tokens:base/s:t />, {254: spanOr("
                + "[spanExpansion(tokens:l:der, []{1, 1}, right), "
                + "spanNext(spanExpansion(tokens:l:der, []{1, 1}, right), "
                + "tokens:l:Mann)])}))", sqwi.toQuery().toString());
    }


    @Test
    public void testEmptyInSequence2 () throws QueryException {
        // der [][] Mann?
        SpanQueryWrapper sqwi = jsonQueryFile(
                "empty-token-in-sequence2.jsonld");

        assertEquals("focus(254: spanContain(<tokens:base/s:t />, {254: spanOr("
                + "[spanExpansion(tokens:l:der, []{2, 2}, right), "
                + "spanNext(spanExpansion(tokens:l:der, []{2, 2}, right), "
                + "tokens:l:Mann)])}))", sqwi.toQuery().toString());
    }


    @Test
    public void testEmptyInSequence3 () throws QueryException {
        // [base=der][]{2,5}[base=Mann]?[]?[][base=Frau]
        SpanQueryWrapper sqwi = jsonQueryFile(
                "empty-token-in-sequence3.jsonld");

        assertEquals(
                "spanNext(" + "spanOr("
                        + "[spanExpansion(tokens:l:der, []{2, 5}, right), "
                        + "spanNext(spanExpansion(tokens:l:der, []{2, 5}, right), "
                        + "tokens:l:Mann)]), "
                        + "spanExpansion(tokens:l:Frau, []{1, 2}, left)" + ")",
                sqwi.toQuery().toString());
    }


    @Test
    public void testEmptyInSequence4 () throws QueryException {
        // der? [] Mann?        
        SpanQueryWrapper sqwi = jsonQueryFile(
                "empty-token-in-sequence4.jsonld");
        assertEquals(
                "spanOr([spanExpansion(tokens:l:der, []{1, 1}, right), "
                        + "spanExpansion(tokens:l:Mann, []{1, 1}, left), "
                        + "spanNext(spanExpansion(tokens:l:der, []{1, 1}, right), tokens:l:Mann)]])",
                sqwi.toQuery().toString());
    }
}
