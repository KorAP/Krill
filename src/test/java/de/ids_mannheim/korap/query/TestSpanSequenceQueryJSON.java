package de.ids_mannheim.korap.query;

import static de.ids_mannheim.korap.TestSimple.*;
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
        assertEquals(
            "focus(254: spanContain(<tokens:base/s:t />, {254: spanExpansion(tokens:s:der, []{1, 1}, right)}))",
            sqwi.toQuery().toString());
    };


    @Test
    public void queryJSONseqEmptyEndClass () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile("empty-last-class.jsonld");
        // der{3:[]}
        assertEquals(
            "focus(254: spanContain(<tokens:base/s:t />, {254: spanExpansion(tokens:s:der, []{1, 1}, right, class:3)}))",
            sqwi.toQuery().toString());
    };


    @Test
    public void queryJSONseqEmptyEndRepetition () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile("empty-last-repetition.jsonld");
        // der[]{3,5}
        assertEquals(
            "focus(254: spanContain(<tokens:base/s:t />, {254: spanExpansion(tokens:s:der, []{3, 5}, right)}))",
            sqwi.toQuery().toString());
    };


    @Test
    public void queryJSONseqEmptyStart () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile("empty-first.jsonld");
        // [][tt/p=NN]
        assertEquals(
            "spanExpansion(tokens:tt/p:NN, []{1, 1}, left)",
            sqwi.toQuery().toString());
    };


    @Test
    public void queryJSONseqEmptyStartClass () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile("empty-first-class.jsonld");
        // {2:[]}[tt/p=NN]
        assertEquals(
            "spanExpansion(tokens:tt/p:NN, []{1, 1}, left, class:2)",
            sqwi.toQuery().toString());
    };


    @Test
    public void queryJSONseqEmptyStartRepetition () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile("empty-first-repetition.jsonld");
        // []{2,7}[tt/p=NN]
        assertEquals(
            "spanExpansion(tokens:tt/p:NN, []{2, 7}, left)",
            sqwi.toQuery().toString());
    };


    @Test
    public void queryJSONseqEmptyStartRepetition2 () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile(
                "empty-first-repetition-2.jsonld");
        // []{0,0}[tt/p=NN]
        assertEquals("tokens:tt/p:NN", sqwi.toQuery().toString());
    };


    @Test
    public void queryJSONseqEmptyMiddle () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile("empty-middle.jsonld");
        // der[][tt/p=NN]
        assertEquals(
            "spanNext(tokens:s:der, spanExpansion(tokens:tt/p:NN, []{1, 1}, left))",
            sqwi.toQuery().toString());
    };


    @Test
    public void queryJSONseqEmptyMiddleClass () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile("empty-middle-class.jsonld");
        // der{1:[]}[tt/p=NN]
        assertEquals(
            "spanNext(tokens:s:der, spanExpansion(tokens:tt/p:NN, []{1, 1}, left, class:1))",
            sqwi.toQuery().toString());
    };


    @Test
    public void queryJSONseqEmptyMiddleRepetition () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile("empty-middle-repetition.jsonld");
        // der[]{4,8}[tt/p=NN]
        assertEquals(
            "spanNext(tokens:s:der, spanExpansion(tokens:tt/p:NN, []{4, 8}, left))",
            sqwi.toQuery().toString());
    };


    @Test
    public void queryJSONseqEmptySurround () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile("empty-surround.jsonld");
        // [][tt/p=NN][]
        assertEquals(
            "focus(254: spanContain(<tokens:base/s:t />, {254: spanExpansion(spanExpansion(tokens:tt/p:NN, []{1, 1}, left), []{1, 1}, right)}))",
            sqwi.toQuery().toString());
    };


    @Test
    public void queryJSONseqEmptySurroundClass () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile("empty-surround-class.jsonld");
        // [][tt/p=NN]{2:[]}
        assertEquals(
            "focus(254: spanContain(<tokens:base/s:t />, {254: spanExpansion(spanExpansion(tokens:tt/p:NN, []{1, 1}, left), []{1, 1}, right, class:2)}))",
            sqwi.toQuery().toString());
    };


    @Test
    public void queryJSONseqEmptySurroundClass2 () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile("empty-surround-class-2.jsonld");
        // {3:[]}[tt/p=NN]{2:[]}
        assertEquals(
            "focus(254: spanContain(<tokens:base/s:t />, {254: spanExpansion(spanExpansion(tokens:tt/p:NN, []{1, 1}, left, class:3), []{1, 1}, right, class:2)}))",
            sqwi.toQuery().toString());
    };


    @Test
    public void queryJSONseqEmptySurroundRepetition () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile(
                "empty-surround-repetition.jsonld");
        // [][tt/p=NN][]{2,7}
        assertEquals(
            "focus(254: spanContain(<tokens:base/s:t />, {254: spanExpansion(spanExpansion(tokens:tt/p:NN, []{1, 1}, left), []{2, 7}, right)}))",
            sqwi.toQuery().toString());
    };


    @Test
    public void queryJSONseqEmptySurroundRepetition2 () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile(
                "empty-surround-repetition-2.jsonld");
        // []{3,5}[tt/p=NN][]{2,7}
        assertEquals(
            "focus(254: spanContain(<tokens:base/s:t />, {254: spanExpansion(spanExpansion(tokens:tt/p:NN, []{3, 5}, left), []{2, 7}, right)}))",
            sqwi.toQuery().toString());
    };


    @Test
    public void queryJSONseqEmptySurroundRepetitionClass ()
            throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile(
                "empty-surround-repetition-class.jsonld");
        // {1:[]}{3,8}[tt/p=NN]{2:[]{2,7}}
        // Ist gleichbedeutend mit
        // {1:[]{3,8}}[tt/p=NN]{2:[]}{2,7}
        assertEquals(
            "focus(254: spanContain(<tokens:base/s:t />, {254: spanExpansion(spanExpansion(tokens:tt/p:NN, []{3, 8}, left, class:1), []{2, 7}, right, class:2)}))",
            sqwi.toQuery().toString());
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
        assertEquals(
            "spanExpansion(tokens:tt/p:NN, !tokens:tt/p:NN{1, 1}, left)",
            sqwi.toQuery().toString());
    };


    @Test
    public void queryJSONseqNegativeEnd () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile("negative-last.jsonld");
        // [tt/p=NN][tt/p!=NN]
        assertEquals(
            "focus(254: spanContain(<tokens:base/s:t />, {254: spanExpansion(tokens:tt/p:NN, !tokens:tt/p:NN{1, 1}, right)}))",
            sqwi.toQuery().toString());
    };


	@Test
    public void queryJSONseqNegativeRegexEnd () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile("negative-regex.jsonld");
        // [tt/p=NN][tt/p!="NN"]
        assertEquals(
			"focus(254: spanContain(<tokens:base/s:t />, {254: spanExpansion(tokens:tt/p:NN, !SpanMultiTermQueryWrapper(tokens:/opennlp/p:NN/){1, 1}, right)}))",
			sqwi.toQuery().toString()
			);
    };


    @Test
    public void queryJSONseqNegativeStartRepetition () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile(
                "negative-first-repetition.jsonld");
        // [tt/p!=NN]{4,5}[tt/p=NN]
        assertEquals(
            "spanExpansion(tokens:tt/p:NN, !tokens:tt/p:NN{4, 5}, left)",
            sqwi.toQuery().toString());
    };


    @Test
    public void queryJSONseqNegativeStartRepetition2 () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile(
                "negative-first-repetition-2.jsonld");
        // [tt/p!=NN]{0,5}[tt/p=NN]
        assertEquals(
            "spanExpansion(tokens:tt/p:NN, !tokens:tt/p:NN{0, 5}, left)",
            sqwi.toQuery().toString());
    };


    @Test
    public void queryJSONseqNegativeStartRepetition3 () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile(
                "negative-first-repetition-3.jsonld");
        // [tt/p!=NN]{0,0}[tt/p=NN]
        assertEquals("tokens:tt/p:NN", sqwi.toQuery().toString());
    };


    @Test
    public void queryJSONseqNegativeEndClass () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile("negative-last-class.jsonld");
        // [tt/p=NN]{2:[tt/p!=NN]}
        SpanQuery sq = sqwi.toQuery();
        assertEquals(
            "focus(254: spanContain(<tokens:base/s:t />, {254: spanExpansion(tokens:tt/p:NN, !tokens:tt/p:NN{1, 1}, right, class:2)}))",
            sq.toString());
    };


    @Test
    public void queryJSONseqNegativeEndRepetitionClass ()
            throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile(
                "negative-last-class-repetition.jsonld");
        // [tt/p=NN]{2:[tt/p!=NN]{4,5}}
        assertEquals(
            "focus(254: spanContain(<tokens:base/s:t />, {254: spanExpansion(tokens:tt/p:NN, !tokens:tt/p:NN{4, 5}, right, class:2)}))",
            sqwi.toQuery().toString());
    };


    @Test
    public void queryJSONseqNegativeEndRepetitionClass2 ()
            throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile(
                "negative-last-class-repetition-2.jsonld");
        // [tt/p=NN]{2:[tt/p!=NN]}{4,5}
        assertEquals(
            "focus(254: spanContain(<tokens:base/s:t />, {254: spanExpansion(tokens:tt/p:NN, !tokens:tt/p:NN{4, 5}, right, class:2)}))",
            sqwi.toQuery().toString());
    };


    @Test
    public void queryJSONseqNegativelastConstraint () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile(
                "negative-last-constraint.jsonld");
        try {
            sqwi.toQuery().toString();
            fail("Should throw an exception");
        }
        catch (QueryException qe) {
            assertEquals(
                    "Distance constraints not supported with empty, optional or negative operands",
                    qe.getMessage());
        };
    };


    @Test
    public void queryJSONseqNegativeEndSequence () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile("negative-last-sequence.jsonld");
        // [tt/p=NN]([tt/p!=DET][tt/p!=NN])
        assertEquals(
                "focus(254: spanContain(<tokens:base/s:t />, {254: spanExpansion(spanExpansion(tokens:tt/p:NN, !tokens:tt/p:DET{1, 1}, right), !tokens:tt/p:ADJ{1, 1}, right)}))",
                sqwi.toQuery().toString());
    };


    @Test
    public void queryJSONseqNegativeEndSequence2 () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile(
                "negative-last-sequence-2.jsonld");
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

        assertEquals(
                "spanElementDistance({129: tokens:s:der}, {129: tokens:s:Baum}, [(base/s:s[0:0], notOrdered, notExcluded)])",
                sqwi.toQuery().toString());
    };


    @Test
    public void queryJSONseqSentenceDistanceExcluded () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile(
                "distance-sentence-excluded.jsonld");

        assertEquals(
                "spanElementDistance({129: tokens:s:der}, {129: tokens:s:Baum}, [(base/s:s[0:0], notOrdered, excluded)])",
                sqwi.toQuery().toString());
    };


    @Test
    public void queryJSONkoralSimpleDistanceBug () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile("distance-simple.jsonld");

        assertEquals(
                "spanDistance(tokens:s:der, tokens:s:Baum, [(w[2:2], ordered, notExcluded)])",
                sqwi.toQuery().toString());
    };


    @Test
    public void queryJSONkoralOptionalityInDistanceBug () {
        try {
            // Sonne [] Mond?
            SpanQueryWrapper sqwi = jsonQueryFile(
                    "distance-with-optionality.jsonld");
            sqwi.toQuery().toString();
        }
        catch (QueryException qe) {
            assertEquals(
                    "Distance constraints not supported with empty, optional or negative operands",
                    qe.getMessage());
        }
        // Could also be a distance at the end ... that's a query planner thing.
    };


    @Test
    public void queryJSONkoralOptionalityAfterEmptyBug ()
            throws QueryException {
        // Sonne [] Mond?
        SpanQueryWrapper sqwi = jsonQueryFile(
                "empty-followed-by-optionality.jsonld");
        assertEquals(
                "focus(254: spanContain(<tokens:base/s:t />, {254: spanOr([spanExpansion(tokens:s:Sonne, []{1, 1}, right), spanNext(spanExpansion(tokens:s:Sonne, []{1, 1}, right), tokens:s:Mond)])}))",
                sqwi.toQuery().toString());
        // Could also be a distance at the end ... that's a query planner thing.
    };


	@Test
    public void queryJSONseqEmptyNegativeOptionalClass () throws QueryException {
        SpanQueryWrapper sqwi = jsonQueryFile("empty-negative-optional.jsonld");
        // der {[pos!=ADJA]*} Mann
        assertEquals(
            "spanNext(tokens:s:der, spanExpansion(tokens:s:Mann, !tokens:tt/p:ADJA{0, 100}, left, class:1))",
            sqwi.toQuery().toString());
    };

	@Test
    public void queryJSONcosmas2Bug () throws QueryException {

        SpanQueryWrapper sqwi = getJsonQuery(getClass().getResource("/queries/bugs/cosmas_wildcards.jsonld").getFile());
        SpanQuery sq = sqwi.toQuery();
        // meine* /+w1:2,s0 &Erfahrung
        assertEquals(
            "spanMultipleDistance({129: SpanMultiTermQueryWrapper(tokens:s:meine*)}, "+
					 "{129: tokens:tt/l:Erfahrung}, "+
					 "[(w[1:2], ordered, notExcluded), "+
					 "(base/s:s[0:0], ordered, notExcluded)])",
            sq.toString());
    };


	@Test
    public void queryJSONdistanceMultipleOps () throws QueryException {

        SpanQueryWrapper sqwi = getJsonQuery(getClass().getResource("/queries/sequence/distance-with-multiple-ops.jsonld").getFile());
        SpanQuery sq = sqwi.toQuery();
        assertEquals(
            "spanDistance(spanDistance(tokens:s:der, tokens:s:alte, [(w[2:2], ordered, notExcluded)]), tokens:s:Baum, [(w[2:2], ordered, notExcluded)])",
            sq.toString());
    };

    
	@Test
    public void queryJSONcosmas2Bug2 () throws QueryException {

        SpanQueryWrapper sqwi = getJsonQuery(getClass().getResource("/queries/bugs/cosmas-exclude.jsonld").getFile());
        
        SpanQuery sq = sqwi.toQuery();

        // (Pop-up OR Pop-ups) %s0 (Internet OR  Programm)
        assertEquals(
            "spanElementDistance({129: spanOr([tokens:s:Blatt, tokens:s:Augen])}, {129: spanOr([tokens:s:Wald, tokens:s:Baum])}, [(base/s:s[0:0], notOrdered, excluded)])",
            sq.toString());
    };
	

    // get query wrapper based on json file
    public SpanQueryWrapper jsonQueryFile (String filename) throws QueryException {
        return getJsonQuery(getClass().getResource(path + filename).getFile());
    };
};
