package de.ids_mannheim.korap.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import de.ids_mannheim.korap.query.wrap.SpanAlterQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanRegexQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanSegmentQueryWrapper;
import de.ids_mannheim.korap.util.QueryException;

@RunWith(JUnit4.class)
public class TestSpanSegmentAlterQuery {
    @Test
    public void spanAlterQuery () throws QueryException {

        SpanAlterQueryWrapper ssaquery = new SpanAlterQueryWrapper("field");
        ssaquery.or("b");
        assertEquals("field:b", ssaquery.toQuery().toString());
    };


    @Test
    public void spanAlterQuery2 () throws QueryException {

        SpanAlterQueryWrapper ssaquery = new SpanAlterQueryWrapper("field");
        ssaquery.or("b").or("c");
        assertEquals("spanOr([field:b, field:c])",
                ssaquery.toQuery().toString());
    };


    @Test
    public void spanAlterQuery3 () throws QueryException {
        SpanAlterQueryWrapper ssaquery = new SpanAlterQueryWrapper("field");
        ssaquery.or("b").or("c").or("d");
        assertEquals("spanOr([field:b, field:c, field:d])",
                ssaquery.toQuery().toString());
    };


    @Test
    public void spanAlterQuery4 () throws QueryException {
        SpanSegmentQueryWrapper segquery = new SpanSegmentQueryWrapper("field",
                "a", "b", "c");
        SpanAlterQueryWrapper ssaquery = new SpanAlterQueryWrapper("field");
        ssaquery.or("d").or(segquery).or("e");
        assertEquals(
                "spanOr([field:d, spanSegment(spanSegment(field:a, field:b), field:c), field:e])",
                ssaquery.toQuery().toString());
    };


    @Test
    public void spanAlterQuery5 () throws QueryException {
        SpanRegexQueryWrapper srequery = new SpanRegexQueryWrapper("field",
                "a[bc]d.?e");
        SpanAlterQueryWrapper ssaquery = new SpanAlterQueryWrapper("field");
        ssaquery.or("f").or(srequery).or("g");
        assertEquals(
                "spanOr([field:f, SpanMultiTermQueryWrapper(field:/a[bc]d.?e/), field:g])",
                ssaquery.toQuery().toString());
    };

};
