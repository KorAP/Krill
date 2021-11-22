package de.ids_mannheim.korap.query;

import static de.ids_mannheim.korap.TestSimple.getJsonQuery;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.apache.lucene.search.spans.SpanQuery;
import org.junit.Test;

import de.ids_mannheim.korap.query.wrap.SpanQueryWrapper;
import de.ids_mannheim.korap.util.QueryException;
import de.ids_mannheim.korap.util.StatusCodes;

public class TestSpanWithAttributeJSON {
    @Test
    public void testElementRegexAttribute () throws QueryException {
        String filepath = getClass()
                .getResource(
                        "/queries/attribute/element-regex-attribute.jsonld")
                .getFile();
        SpanQueryWrapper sqwi = getJsonQuery(filepath);

        QueryException exception = assertThrows(QueryException.class, () -> {
            sqwi.toQuery();
        });
        assertEquals("SpanAttributeQuery only supports SpanTermQuery.",
                exception.getMessage());
        assertEquals(StatusCodes.UNSUPPORTED_QUERY, exception.getErrorCode());
    }

    @Test
    public void testElementSingleAttribute () throws QueryException {
        String filepath = getClass()
                .getResource(
                        "/queries/attribute/element-single-attribute.jsonld")
                .getFile();
        SpanQueryWrapper sqwi = getJsonQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(
                "spanElementWithAttribute(<tokens:head />, spanAttribute(tokens:@:type:top))",
                sq.toString());
    }

    @Test
    public void testElementSingleAttributeBug () throws QueryException {
        String filepath = getClass()
                .getResource(
                        "/queries/attribute/element-single-attribute-2.jsonld")
                .getFile();
        SpanQueryWrapper sqwi = getJsonQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(
                "spanElementWithAttribute(<tokens:dereko/s:said />, " +
                "spanAttribute(tokens:@:dereko/s:mode:direct))",
                sq.toString());
    }

    @Test
    public void testElementSingleNotAttribute () throws QueryException {
        String filepath = getClass()
                .getResource(
                        "/queries/attribute/element-single-not-attribute.jsonld")
                .getFile();
        SpanQueryWrapper sqwi = getJsonQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(
                "spanElementWithAttribute(<tokens:head />, spanAttribute(!tokens:@:type:top))",
                sq.toString());
    }

    @Test
    public void testElementSingleNotAttribute2 () throws QueryException {
        String filepath = getClass()
                .getResource(
                        "/queries/attribute/element-single-not-attribute-2.jsonld")
                .getFile();
        SpanQueryWrapper sqwi = getJsonQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(
                "spanElementWithAttribute(<tokens:head />, spanAttribute(!tokens:@:type:top))",
                sq.toString());
    }
    

    @Test
    public void testElementMultipleAndNotAttributes () throws QueryException {
        String filepath = getClass()
                .getResource(
                        "/queries/attribute/element-multiple-and-not-attributes.jsonld")
                .getFile();
        SpanQueryWrapper sqwi = getJsonQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(
                "spanElementWithAttribute(<tokens:div />, [spanAttribute(tokens:@:type:Zeitschrift), "
                        + "spanAttribute(!tokens:@:complete:Y), spanAttribute(tokens:@:n:0)])",
                sq.toString());
    }

    @Test
    public void testElementMultipleAndNotAttributes2 () throws QueryException {
        String filepath = getClass()
                .getResource(
                        "/queries/attribute/element-multiple-and-not-attributes-2.jsonld")
                .getFile();
        SpanQueryWrapper sqwi = getJsonQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(
                "spanElementWithAttribute(<tokens:div />, [spanAttribute(tokens:@:type:Zeitschrift), "
                        + "spanAttribute(!tokens:@:complete:Y), spanAttribute(tokens:@:n:0)])",
                sq.toString());
    }
    

    @Test
    public void testElementMultipleOrAttributes () throws QueryException {
        String filepath = getClass()
                .getResource(
                        "/queries/attribute/element-multiple-or-attributes.jsonld")
                .getFile();
        SpanQueryWrapper sqwi = getJsonQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(
                "spanOr([spanElementWithAttribute(<tokens:div />, spanAttribute(tokens:@:type:Zeitschrift)), "
                        + "spanElementWithAttribute(<tokens:div />, spanAttribute(tokens:@:complete:Y)), "
                        + "spanElementWithAttribute(<tokens:div />, spanAttribute(tokens:@:n:0))])",
                sq.toString());
    }


    @Test
    public void testElementMultipleOrAttributes2 () throws QueryException {
        String filepath = getClass()
                .getResource(
                        "/queries/attribute/element-multiple-or-attributes-2.jsonld")
                .getFile();
        SpanQueryWrapper sqwi = getJsonQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(
                "spanOr([spanElementWithAttribute(<tokens:div />, spanAttribute(tokens:@:type:Zeitschrift)), "
                        + "spanElementWithAttribute(<tokens:div />, spanAttribute(tokens:@:complete:Y)), "
                        + "spanElementWithAttribute(<tokens:div />, spanAttribute(tokens:@:n:0))])",
                sq.toString());
    }
    

    @Test
    public void testAnyElementWithAttribute () throws QueryException {
        String filepath = getClass()
                .getResource(
                        "/queries/attribute/any-element-with-attribute.jsonld")
                .getFile();
        SpanQueryWrapper sqwi = getJsonQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals("spanWithAttribute(spanAttribute(tokens:@:type:top))",
                sq.toString());
    }

    @Test
    public void testAnyElementWithMultipleOrAttributes ()
            throws QueryException {
        String filepath = getClass()
                .getResource(
                        "/queries/attribute/any-element-with-multiple-or-attributes.jsonld")
                .getFile();
        SpanQueryWrapper sqwi = getJsonQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(
                "spanOr([spanWithAttribute(spanAttribute(tokens:@:type:Zeitschrift)), "
                        + "spanWithAttribute(spanAttribute(tokens:@:complete:Y)), "
                        + "spanWithAttribute(spanAttribute(tokens:@:n:0))])",
                sq.toString());
    }


    @Test
    public void testAnyElementMultipleAndNotAttributes ()
            throws QueryException {
        String filepath = getClass()
                .getResource(
                        "/queries/attribute/any-element-with-multiple-and-not-attributes.jsonld")
                .getFile();
        SpanQueryWrapper sqwi = getJsonQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(
                "spanWithAttribute([spanAttribute(tokens:@:type:Zeitschrift), "
                        + "spanAttribute(!tokens:@:complete:Y), spanAttribute(tokens:@:n:0)])",
                sq.toString());
    }


    @Test
    public void testAnyElementSingleNotAttribute () throws QueryException {

        String filepath = getClass()
                .getResource(
                        "/queries/attribute/any-element-with-single-not-attribute.jsonld")
                .getFile();
        
        Exception exception = assertThrows(QueryException.class, () -> {
            getJsonQuery(filepath);
        });
        assertEquals("The query requires a positive attribute.",
                exception.getMessage());
        //        assertEquals("tokens:???", sq.toString());
    }
}
