package de.ids_mannheim.korap.query;

import static de.ids_mannheim.korap.TestSimple.getJSONQuery;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.lucene.search.spans.SpanQuery;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import de.ids_mannheim.korap.query.wrap.SpanQueryWrapper;
import de.ids_mannheim.korap.util.QueryException;

/**
 * @author diewald
 */

@RunWith(JUnit4.class)
public class TestSpanSequenceQueryJSON {

    static String path = "/queries/sequence/";


    // Test Extensions

    @Test
    public void queryJSONseqEmpty () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile("empty.jsonld");

        // []
        assertTrue(sqwi.isEmpty());
    };


    @Test
    public void queryJSONseqEmptyEnd () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile("empty-last.jsonld");
        assertEquals(sqwi.toQuery().toString(),
                "spanExpansion(tokens:s:der, []{1, 1}, right)");
    };


    @Test
    public void queryJSONseqEmptyEndClass () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile("empty-last-class.jsonld");
        // der{3:[]}
        assertEquals(sqwi.toQuery().toString(),
                "spanExpansion(tokens:s:der, []{1, 1}, right, class:3)");
    };


    @Test
    public void queryJSONseqEmptyEndRepetition () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile("empty-last-repetition.jsonld");
        // der[]{3,5}
        assertEquals(sqwi.toQuery().toString(),
                "spanExpansion(tokens:s:der, []{3, 5}, right)");
    };


    @Test
    public void queryJSONseqEmptyStart () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile("empty-first.jsonld");
        // [][tt/p=NN]
        assertEquals(sqwi.toQuery().toString(),
                "spanExpansion(tokens:tt/p:NN, []{1, 1}, left)");
    };


    @Test
    public void queryJSONseqEmptyStartClass () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile("empty-first-class.jsonld");
        // {2:[]}[tt/p=NN]
        assertEquals(sqwi.toQuery().toString(),
                "spanExpansion(tokens:tt/p:NN, []{1, 1}, left, class:2)");
    };


    @Test
    public void queryJSONseqEmptyStartRepetition () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile("empty-first-repetition.jsonld");
        // []{2,7}[tt/p=NN]
        assertEquals(sqwi.toQuery().toString(),
                "spanExpansion(tokens:tt/p:NN, []{2, 7}, left)");
    };


    @Test
    public void queryJSONseqEmptyStartRepetition2 () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile("empty-first-repetition-2.jsonld");
        // []{0,0}[tt/p=NN]
        assertEquals(sqwi.toQuery().toString(), "tokens:tt/p:NN");
    };


    @Test
    public void queryJSONseqEmptyMiddle () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile("empty-middle.jsonld");
        // der[][tt/p=NN]
        assertEquals(sqwi.toQuery().toString(),
                "spanNext(tokens:s:der, spanExpansion(tokens:tt/p:NN, []{1, 1}, left))");
    };


    @Test
    public void queryJSONseqEmptyMiddleClass () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile("empty-middle-class.jsonld");
        // der{1:[]}[tt/p=NN]
        assertEquals(
                sqwi.toQuery().toString(),
                "spanNext(tokens:s:der, spanExpansion(tokens:tt/p:NN, []{1, 1}, left, class:1))");
    };


    @Test
    public void queryJSONseqEmptyMiddleRepetition () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile("empty-middle-repetition.jsonld");
        // der[]{4,8}[tt/p=NN]
        assertEquals(sqwi.toQuery().toString(),
                "spanNext(tokens:s:der, spanExpansion(tokens:tt/p:NN, []{4, 8}, left))");
    };


    @Test
    public void queryJSONseqEmptySurround () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile("empty-surround.jsonld");
        // [][tt/p=NN][]
        assertEquals(sqwi.toQuery().toString(),
                "spanExpansion(spanExpansion(tokens:tt/p:NN, []{1, 1}, left), []{1, 1}, right)");
    };


    @Test
    public void queryJSONseqEmptySurroundClass () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile("empty-surround-class.jsonld");
        // [][tt/p=NN]{2:[]}
        assertEquals(
                sqwi.toQuery().toString(),
                "spanExpansion(spanExpansion(tokens:tt/p:NN, []{1, 1}, left), []{1, 1}, right, class:2)");
    };


    @Test
    public void queryJSONseqEmptySurroundClass2 () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile("empty-surround-class-2.jsonld");
        // {3:[]}[tt/p=NN]{2:[]}
        assertEquals(
                sqwi.toQuery().toString(),
                "spanExpansion(spanExpansion(tokens:tt/p:NN, []{1, 1}, left, class:3), []{1, 1}, right, class:2)");
    };


    @Test
    public void queryJSONseqEmptySurroundRepetition () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile("empty-surround-repetition.jsonld");
        // [][tt/p=NN][]{2,7}
        assertEquals(sqwi.toQuery().toString(),
                "spanExpansion(spanExpansion(tokens:tt/p:NN, []{1, 1}, left), []{2, 7}, right)");
    };


    @Test
    public void queryJSONseqEmptySurroundRepetition2 () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile("empty-surround-repetition-2.jsonld");
        // []{3,5}[tt/p=NN][]{2,7}
        assertEquals(sqwi.toQuery().toString(),
                "spanExpansion(spanExpansion(tokens:tt/p:NN, []{3, 5}, left), []{2, 7}, right)");
    };


    @Test
    public void queryJSONseqEmptySurroundRepetitionClass ()
            throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile("empty-surround-repetition-class.jsonld");
        // {1:[]}{3,8}[tt/p=NN]{2:[]{2,7}}
        // Ist gleichbedeutend mit
        // {1:[]{3,8}}[tt/p=NN]{2:[]}{2,7}
        assertEquals(
                sqwi.toQuery().toString(),
                "spanExpansion(spanExpansion(tokens:tt/p:NN, []{3, 8}, left, class:1), []{2, 7}, right, class:2)");
    };


    @Test
    public void queryJSONseqNegative () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile("negative.jsonld");
        // [tt/p!=NN]
        assertTrue(sqwi.isNegative());
    };


    @Test
    public void queryJSONseqNegativeStart () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile("negative-first.jsonld");
        // [tt/p!=NN][tt/p=NN]
        assertEquals(sqwi.toQuery().toString(),
                "spanExpansion(tokens:tt/p:NN, !tokens:tt/p:NN{1, 1}, left)");
    };


    @Test
    public void queryJSONseqNegativeEnd () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile("negative-last.jsonld");
        // [tt/p=NN][tt/p!=NN]
        assertEquals(sqwi.toQuery().toString(),
                "spanExpansion(tokens:tt/p:NN, !tokens:tt/p:NN{1, 1}, right)");
    };


    @Test
    public void queryJSONseqNegativeStartRepetition () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile("negative-first-repetition.jsonld");
        // [tt/p!=NN]{4,5}[tt/p=NN]
        assertEquals(sqwi.toQuery().toString(),
                "spanExpansion(tokens:tt/p:NN, !tokens:tt/p:NN{4, 5}, left)");
    };


    @Test
    public void queryJSONseqNegativeStartRepetition2 () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile("negative-first-repetition-2.jsonld");
        // [tt/p!=NN]{0,5}[tt/p=NN]
        assertEquals(sqwi.toQuery().toString(),
                "spanExpansion(tokens:tt/p:NN, !tokens:tt/p:NN{0, 5}, left)");
    };


    @Test
    public void queryJSONseqNegativeStartRepetition3 () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile("negative-first-repetition-3.jsonld");
        // [tt/p!=NN]{0,0}[tt/p=NN]
        assertEquals(sqwi.toQuery().toString(), "tokens:tt/p:NN");
    };


    @Test
    public void queryJSONseqNegativeEndClass () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile("negative-last-class.jsonld");
        // [tt/p=NN]{2:[tt/p!=NN]}
        SpanQuery sq = sqwi.toQuery();
        assertEquals(sq.toString(),
                "spanExpansion(tokens:tt/p:NN, !tokens:tt/p:NN{1, 1}, right, class:2)");
    };


    @Test
    public void queryJSONseqNegativeEndRepetitionClass () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile("negative-last-class-repetition.jsonld");
        // [tt/p=NN]{2:[tt/p!=NN]{4,5}}
        assertEquals(sqwi.toQuery().toString(),
                "spanExpansion(tokens:tt/p:NN, !tokens:tt/p:NN{4, 5}, right, class:2)");
    };


    @Test
    public void queryJSONseqNegativeEndRepetitionClass2 ()
            throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile("negative-last-class-repetition-2.jsonld");
        // [tt/p=NN]{2:[tt/p!=NN]}{4,5}
        assertEquals(sqwi.toQuery().toString(),
                "spanExpansion(tokens:tt/p:NN, !tokens:tt/p:NN{4, 5}, right, class:2)");
    };


    @Test
    public void queryJSONseqNegativelastConstraint () {
        SpanQueryWrapper sqwi = jsonQueryFile("negative-last-constraint.jsonld");
        try {
            sqwi.toQuery().toString();
            fail("Should throw an exception");
        }
        catch (QueryException qe) {
            assertEquals(
                    "Distance constraints not supported with empty or negative operands",
                    qe.getMessage());
        };
    };


    @Test
    public void queryJSONseqNegativeEndSequence () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile("negative-last-sequence.jsonld");
        // [tt/p=NN]([tt/p!=DET][tt/p!=NN])
        assertEquals(
                "spanExpansion(spanExpansion(tokens:tt/p:NN, !tokens:tt/p:DET{1, 1}, right), !tokens:tt/p:ADJ{1, 1}, right)",
                sqwi.toQuery().toString());
    };


    @Test
    public void queryJSONseqNegativeEndSequence2 () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile("negative-last-sequence-2.jsonld");
        // [tt/p!=NN]([tt/p!=DET][tt/p=NN])

        // spanNext(tokens:tt/p:NN, 
        assertEquals(
                "spanExpansion(spanExpansion(tokens:tt/p:ADJ, !tokens:tt/p:DET{1, 1}, left), !tokens:tt/p:NN{1, 1}, left)",
                sqwi.toQuery().toString());
    };


    @Test
    public void queryJSONseqMultipleDistances () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile("distance-multiple.jsonld");
        // er []{,10} kann []{1,10} sagte 

        assertEquals(
                "spanDistance(tokens:s:er, spanDistance(tokens:s:kann, tokens:s:sagte, [(w[2:11], ordered, notExcluded)]), [(w[1:11], ordered, notExcluded)])",
                sqwi.toQuery().toString());
    };

    @Test
    public void queryJSONseqSentenceDistance () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile("distance-sentence.jsonld");

        assertEquals("spanElementDistance({129: tokens:s:der}, {129: tokens:s:Baum}, [(base/s:s[0:0], notOrdered, notExcluded)])",sqwi.toQuery().toString());
    };

    @Test
    public void queryJSONseqSentenceDistanceExcluded () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile("distance-sentence-excluded.jsonld");

        assertEquals("spanElementDistance({129: tokens:s:der}, {129: tokens:s:Baum}, [(base/s:s[0:0], notOrdered, excluded)])",sqwi.toQuery().toString());
    };


    @Test
    public void queryJSONkoralSimpleDistanceBug () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile("distance-simple.jsonld");

        assertEquals("spanDistance(tokens:s:der, tokens:s:Baum, [(w[2:2], ordered, notExcluded)])",sqwi.toQuery().toString());
    };


    // get query wrapper based on json file
    public SpanQueryWrapper jsonQueryFile (String filename) {
        return getJSONQuery(getClass().getResource(path + filename).getFile());
    };
};
