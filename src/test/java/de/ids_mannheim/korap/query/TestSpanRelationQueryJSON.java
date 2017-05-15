package de.ids_mannheim.korap.query;

import static de.ids_mannheim.korap.TestSimple.getJSONQuery;
import static org.junit.Assert.assertEquals;

import org.apache.lucene.search.spans.SpanQuery;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import de.ids_mannheim.korap.query.wrap.SpanQueryWrapper;
import de.ids_mannheim.korap.util.QueryException;

public class TestSpanRelationQueryJSON {

    @Rule
    public ExpectedException exception = ExpectedException.none();


    @Test
    public void testMatchAnyRelationSourceWithAttribute ()
            throws QueryException {
        String filepath = getClass()
                .getResource("/queries/relation/any-source-with-attribute.json")
                .getFile();
        SpanQueryWrapper sqwi = getJSONQuery(filepath);
        SpanQuery sq = sqwi.toQuery();

        assertEquals(
                "focus(#[1,2]spanSegment(<tokens:c:vp />, "
                        + "focus(#2: spanSegment(spanRelation(tokens:>:mate/d:HEAD), "
                        + "spanWithAttribute(spanAttribute(tokens:type:case:accusative))))))",
                sq.toString());
    }


    @Test
    public void testMatchAnyRelationTargetWithAttribute ()
            throws QueryException {
        String filepath = getClass()
                .getResource("/queries/relation/any-target-with-attribute.json")
                .getFile();
        SpanQueryWrapper sqwi = getJSONQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(
                // "focus(#[1,2]spanSegment(focus(#2: spanSegment(spanRelation(tokens:>:mate/d:HEAD), "
                // +
                // "<tokens:c:vp />)), spanWithAttribute(spanAttribute(tokens:type:case:accusative))))",
                //
                "focus(#[1,2]spanSegment(spanWithAttribute(spanAttribute(tokens:type:case:accusative)), "
                        + "focus(#2: spanSegment(spanRelation(tokens:>:mate/d:HEAD), "
                        + "<tokens:c:vp />))))",
                sq.toString());

        // System.out.println(sq.toString());
    }


    @Test
    public void testMatchSpecificRelationSourceWithAttribute ()
            throws QueryException {
        String filepath = getClass()
                .getResource(
                        "/queries/relation/specific-source-with-attribute.json")
                .getFile();
        SpanQueryWrapper sqwi = getJSONQuery(filepath);
        SpanQuery sq = sqwi.toQuery();

        assertEquals(
                "focus(#[1,2]spanSegment(<tokens:c:vp />, "
                        + "focus(#2: spanSegment(spanRelation(tokens:>:mate/d:HEAD), "
                        + "spanElementWithAttribute(<tokens:c:np />, "
                        + "spanAttribute(tokens:type:case:accusative))))))",
                sq.toString());
    }


    @Test
    public void testMatchBothRelationNodesWithAttribute ()
            throws QueryException {
        String filepath = getClass()
                .getResource(
                        "/queries/relation/both-operands-with-attribute.json")
                .getFile();
        SpanQueryWrapper sqwi = getJSONQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(
                "focus(#[1,2]spanSegment(spanElementWithAttribute(<tokens:c: />, "
                        + "spanAttribute(tokens:type:case:accusative)), "
                        + "focus(#2: spanSegment(spanRelation(tokens:>:mate/d:HEAD), "
                        + "spanElementWithAttribute(<tokens:c: />, "
                        + "spanAttribute(tokens:type:case:accusative))))))",
                sq.toString());
    }


    @Test
    public void testMatchRelationSource () throws QueryException {
        //
        String filepath = getClass()
                .getResource("/queries/relation/match-source.json").getFile();
        SpanQueryWrapper sqwi = getJSONQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(
                "focus(#[1,2]spanSegment(spanRelation(tokens:>:mate/d:HEAD), <tokens:c:s />))",
                sq.toString());
    }
    
    @Test
    public void testMatchRelationSourceToken () throws QueryException {
        //
        String filepath = getClass()
                .getResource("/queries/relation/match-source-token.json").getFile();
        SpanQueryWrapper sqwi = getJSONQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(
                "focus(#[1,2]spanSegment(spanRelation(tokens:>:malt/d:KONJ), tokens:tt/l:um))",
                sq.toString());
    }

    
    @Test
    public void testMatchRelationTarget () throws QueryException {
        //
        String filepath = getClass()
                .getResource("/queries/relation/match-target.json").getFile();
        SpanQueryWrapper sqwi = getJSONQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(
                "focus(#[1,2]spanSegment(spanRelation(tokens:<:mate/d:HEAD), <tokens:c:vp />))",
                sq.toString());
    }


    @Test
    public void testMatchRelationSourceAndTarget () throws QueryException {
        //
        String filepath = getClass()
                .getResource("/queries/relation/match-source-and-target.json")
                .getFile();
        SpanQueryWrapper sqwi = getJSONQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(
                "focus(#[1,2]spanSegment(<tokens:c:vp />, "
                        + "focus(#2: spanSegment(spanRelation(tokens:>:mate/d:HEAD), <tokens:c:s />))))",
                sq.toString());
    }


    @Test
    public void testMatchOperandWithProperty () throws QueryException {
        //
        String filepath = getClass()
                .getResource("/queries/relation/operand-with-property.json")
                .getFile();
        SpanQueryWrapper sqwi = getJSONQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(
                "focus(#[1,2]spanSegment(<tokens:c:vp />, focus(#2: spanSegment(spanRelation(tokens:>:mate/d:HEAD), "
                        + "spanElementWithAttribute(<tokens:c:s />, spanAttribute(tokens:@root))))))",
                sq.toString());
    }


    @Test
    public void testMatchOperandWithAttribute () throws QueryException {
        //
        String filepath = getClass()
                .getResource("/queries/relation/operand-with-attribute.json")
                .getFile();
        SpanQueryWrapper sqwi = getJSONQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(
                "focus(#[1,2]spanSegment(<tokens:c:vp />, focus(#2: spanSegment(spanRelation(tokens:>:mate/d:HEAD), "
                        + "spanElementWithAttribute(<tokens:c:s />, spanAttribute(tokens:type:top))))))",
                sq.toString());
    }


    @Test
    public void testMatchRelationOnly () throws QueryException {
        //
        String filepath = getClass()
                .getResource("/queries/relation/relation-only.json").getFile();
        SpanQueryWrapper sqwi = getJSONQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals("focus(#[1,2]spanRelation(tokens:>:mate/d:HEAD))",
                sq.toString());
    }


    @Test
    public void testFocusSource () throws QueryException {
        //
        String filepath = getClass()
                .getResource("/queries/relation/focus-source.json").getFile();
        SpanQueryWrapper sqwi = getJSONQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(
                "focus(1: focus(#[1,2]spanSegment(spanRelation(tokens:<:mate/d:HEAD), {1: <tokens:c:np />})))",
                sq.toString());
    }


    @Test
    public void testFocusTarget () throws QueryException {
        String filepath = getClass()
                .getResource("/queries/relation/focus-target.json").getFile();
        SpanQueryWrapper sqwi = getJSONQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(
                "focus(2: focus(#[1,2]spanSegment({2: <tokens:c:np />}, "
                        + "focus(#2: spanSegment(spanRelation(tokens:>:mate/d:HEAD), {1: <tokens:c:s />})))))",
                sq.toString());
    }


    @Test
    public void testFocusEmptyTarget () throws QueryException {
        String filepath = getClass()
                .getResource("/queries/relation/focus-empty-target.json")
                .getFile();
        SpanQueryWrapper sqwi = getJSONQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(
                "focus(2: focus(#[1,2]spanSegment({2: target:spanRelation(tokens:>:mate/d:HEAD)}, {1: <tokens:c:s />})))",
                sq.toString());
    }


    @Test
    public void testFocusEmptyBoth () throws QueryException {
        String filepath = getClass()
                .getResource("/queries/relation/focus-empty-both.json")
                .getFile();
        SpanQueryWrapper sqwi = getJSONQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(
                "focus(2: focus(#[1,2]{1: source:{2: target:spanRelation(tokens:>:mate/d:HEAD)}}))",
                sq.toString());
    }


    // EM: should relation term allow empty key?
    @Test
    public void testTypedRelationWithoutKey () throws QueryException {

        exception.expectMessage("Key definition is missing in term or span");

        String filepath = getClass()
                .getResource(
                        "/queries/relation/typed-relation-without-key.json")
                .getFile();
        SpanQueryWrapper sqwi = getJSONQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals("tokens:???", sq.toString());
    }

    @Test
    public void testTypedRelationWithKey () throws QueryException {
        String filepath = getClass()
                .getResource("/queries/relation/typed-relation-with-key.json")
                .getFile();
        SpanQueryWrapper sqwi = getJSONQuery(filepath);
        SpanQuery sq = sqwi.toQuery();

        assertEquals("focus(#[1,2]spanRelation(tokens:>:malt/d:PP))",
                sq.toString());
    }


    @Test
    public void testTypedRelationWithAnnotationNodes () throws QueryException {
        // query = "corenlp/c=\"VP\" & corenlp/c=\"NP\" & #1 ->malt/d[func=\"PP\"] #2";
        String filepath = getClass()
                .getResource(
                        "/queries/relation/typed-relation-with-annotation-nodes.json")
                .getFile();
        SpanQueryWrapper sqwi = getJSONQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(
                "focus(#[1,2]spanSegment(<tokens:corenlp/c:NP />, "
                        + "focus(#2: spanSegment("
                        + "spanRelation(tokens:>:malt/d:PP), <tokens:corenlp/c:VP />))))",
                sq.toString());

    }

    @Test
    public void testTypedRelationWithWrapTokenNodes () throws QueryException {
        // query = "corenlp/c=\"VP\" & corenlp/c=\"NP\" & #1 ->malt/d[func=\"PP\"] #2";
        String filepath = getClass()
                .getResource(
                        "/queries/relation/typed-relation-with-wrap-token-nodes.json")
                .getFile();
        SpanQueryWrapper sqwi = getJSONQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(
                "focus(#[1,2]spanSegment(tokens:tt/p:VVINF, "
                + "focus(#2: spanSegment("
                + "spanRelation(tokens:>:malt/d:KONJ), tokens:tt/p:KOUI))))",
                sq.toString());

    }
    
    // EM: handle empty koral:span
}
