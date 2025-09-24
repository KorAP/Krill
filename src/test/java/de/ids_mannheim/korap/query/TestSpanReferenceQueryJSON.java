package de.ids_mannheim.korap.query;

import static de.ids_mannheim.korap.TestSimple.*;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.lucene.search.spans.SpanQuery;
import org.junit.Test;

import de.ids_mannheim.korap.query.wrap.SpanQueryWrapper;
import de.ids_mannheim.korap.util.QueryException;

public class TestSpanReferenceQueryJSON {
    
    @Test
    public void testNegativeClassNumBug () throws IOException, QueryException {
        String filepath = getClass()
                .getResource(
                        "/queries/bugs/annis_reference_bug.jsonld")
                .getFile();
        SpanQueryWrapper sqwi = getJsonQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        
        // "ich" & pos="VVFIN" & #1 ->malt/d[func="SUBJ"] #2 & #1 . #2
        assertEquals(
            "spanReference(spanNext("
                + "focus(129: focus(#[1,2]spanSegment({130: tokens:tt/p:VVFIN}, "
                + "focus(#2: spanSegment(spanRelation(tokens:>:malt/d:SUBJ), "
                + "{129: tokens:s:ich}),sorting)),sorting),sorting), "
                + "{130: tokens:tt/p:VVFIN}), -126)",
            sq.toString());
    }
    
    @Test
    public void testFirstOperandRef () throws IOException, QueryException {

        String filepath = getClass()
                .getResource(
                        "/queries/reference/first-operand-reference.jsonld")
                .getFile();
        SpanQueryWrapper sqwi = getJsonQuery(filepath);
        SpanQuery sq = sqwi.toQuery();

        // 'cat="V" & cat="NP" & cat="PP" & #2 . #1 & #1 ->dep #3 &
        // #3 . #2

        assertEquals(
                "spanReference(spanNext({3: tokens:p:P}, "
                        + "focus(2: focus(#[1,2]spanSegment({3: tokens:p:P}, "
                        + "focus(#2: spanSegment(spanRelation(tokens:>:mate/d:HEAD), "
                        + "focus(1: spanNext({2: tokens:p:V}, {1: <tokens:c:NP />}))),sorting)),sorting),sorting)), 3)",
                sq.toString());
    }


    @Test
    public void testSecondOperandRef () throws QueryException {

        String filepath = getClass()
                .getResource(
                        "/queries/reference/second-operand-reference.jsonld")
                .getFile();
        SpanQueryWrapper sqwi = getJsonQuery(filepath);
        SpanQuery sq = sqwi.toQuery();

        // 'cat="V" & cat="NP" & cat="PP" & #2 . #1 & #1 ->dep #3 &
        // #2 . #3
        assertEquals(
                "spanReference(spanNext(focus(2: focus(#[1,2]spanSegment({3: tokens:p:P}, "
                        + "focus(#2: spanSegment(spanRelation(tokens:>:mate/d:HEAD), "
                        + "focus(1: spanNext({2: tokens:p:V}, {1: <tokens:c:NP />}))),sorting)),sorting),sorting), "
                        + "{3: tokens:p:P}), 3)",
                sq.toString());
    }


    @Test
    public void testMultipleReferences () throws QueryException {
        String filepath = getClass()
                .getResource("/queries/reference/multiple-references.jsonld")
                .getFile();
        SpanQueryWrapper sqwi = getJsonQuery(filepath);
        SpanQuery sq = sqwi.toQuery();

        // 'cat="VP" & cat="NP" & cat="PP" & #1 . #2 & #2 . #3 & #1 .
        // #3 & #2 ->dep #1'
        assertEquals(
                "spanReference(focus(#[1,2]spanSegment({1: <tokens:c:VP />}, "
                        + "focus(#2: spanSegment(spanRelation(tokens:>:mate/d:HEAD), "
                        + "focus(2: spanReference(spanNext(focus(1: spanNext(focus(2: spanNext({1: <tokens:c:VP />}, "
                        + "{2: <tokens:c:NP />})), {3: <tokens:c:PP />})), {3: <tokens:c:PP />}), 3))),sorting)),sorting), 1)",
                sq.toString());
    }


    @Test
    public void testDistanceReferences () throws QueryException {

        // ND: I don't understand what this query should be about ...
        // EM: There was just a bug with multiple distance
        String filepath = getClass()
                .getResource(
                        "/queries/reference/bug-multiple-distance-simple.jsonld")
                .getFile();
        SpanQueryWrapper sqwi = getJsonQuery(filepath);
        SpanQuery sq = sqwi.toQuery();

        assertEquals(
                "spanDistance(<tokens:c:vb />, <tokens:c:prp />, [(w[1:1], notOrdered, notExcluded)])",
                sq.toString());

        // "/queries/reference/distance-reference.jsonld").getFile();
        filepath = getClass()
                .getResource("/queries/reference/bug-multiple-distance.jsonld")
                .getFile();
        sqwi = getJsonQuery(filepath);
        sq = sqwi.toQuery();

        // 'cat="VP" & cat="NP" & cat="PP" & #1 . #2 & #2 . #3 & #1 .
        // #3 & #2 ->dep #1'
        assertEquals(
                "spanReference(focus(#[1,2]spanSegment({1: <tokens:c:prp />}, "
                        + "focus(#2: spanSegment(spanRelation(tokens:>:stanford/d:tag), "
                        + "focus(2: spanDistance(focus(1: spanDistance(<tokens:c:vb />, "
                        + "{1: <tokens:c:prp />}, [(w[1:1], notOrdered, notExcluded)]),sorting), "
                        + "{2: <tokens:c:nn />}, [(w[1:3], ordered, notExcluded)]))),sorting)),sorting), 1)",
                sq.toString());
    }
}
