package de.ids_mannheim.korap.query;

import java.util.*;
import de.ids_mannheim.korap.query.wrap.SpanSegmentQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanRegexQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanAlterQueryWrapper;

import de.ids_mannheim.korap.util.QueryException;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestSpanSegmentQuery {
    @Test
    public void spanSegmentQuery () throws QueryException {

	SpanSegmentQueryWrapper ssquery = new SpanSegmentQueryWrapper("field","a");
	assertEquals("field:a", ssquery.toQuery().toString());

	ssquery = new SpanSegmentQueryWrapper("field", "a", "b");
	assertEquals("spanSegment(field:a, field:b)", ssquery.toQuery().toString());

	ssquery = new SpanSegmentQueryWrapper("field","a", "b", "c");
	assertEquals("spanSegment(spanSegment(field:a, field:b), field:c)", ssquery.toQuery().toString());
    };

    @Test
    public void spanSegmentQueryExclusive () throws QueryException {

	SpanSegmentQueryWrapper ssquery = new SpanSegmentQueryWrapper("field","a");
	assertEquals("field:a", ssquery.toQuery().toString());

	ssquery = new SpanSegmentQueryWrapper("field", "a", "b");
	assertEquals("spanSegment(field:a, field:b)", ssquery.toQuery().toString());

	ssquery.without("c");
	assertEquals("spanNot(spanSegment(field:a, field:b), field:c, 0, 0)", ssquery.toQuery().toString());

	ssquery.without("d");
	assertEquals("spanNot(spanSegment(field:a, field:b), spanOr([field:c, field:d]), 0, 0)", ssquery.toQuery().toString());
    };


    @Test
    public void spanSegmentRegexQuery () throws QueryException {
	SpanSegmentQueryWrapper ssquery = new SpanSegmentQueryWrapper("field");
	assertNull(ssquery.toQuery());
	ssquery.with("a");
	assertEquals("field:a", ssquery.toQuery().toString());

	ssquery.with(new SpanRegexQueryWrapper("field", "a.*b"));

	assertEquals("spanSegment(field:a, SpanMultiTermQueryWrapper(field:/a.*b/))", ssquery.toQuery().toString());

	ssquery.with("c");

	assertEquals("spanSegment(spanSegment(field:a, SpanMultiTermQueryWrapper(field:/a.*b/)), field:c)", ssquery.toQuery().toString());

	ssquery.with("d").with("e");

	assertEquals("spanSegment(spanSegment(spanSegment(spanSegment(field:a, SpanMultiTermQueryWrapper(field:/a.*b/)), field:c), field:d), field:e)", ssquery.toQuery().toString());

	ssquery.without(new SpanRegexQueryWrapper("field", "x.?y"));

	assertEquals("spanNot(spanSegment(spanSegment(spanSegment(spanSegment(field:a, SpanMultiTermQueryWrapper(field:/a.*b/)), field:c), field:d), field:e), SpanMultiTermQueryWrapper(field:/x.?y/), 0, 0)", ssquery.toQuery().toString());

	ssquery.without(new SpanRegexQueryWrapper("field", "z{5,9}"));

	assertEquals("spanNot(spanSegment(spanSegment(spanSegment(spanSegment(field:a, SpanMultiTermQueryWrapper(field:/a.*b/)), field:c), field:d), field:e), spanOr([SpanMultiTermQueryWrapper(field:/x.?y/), SpanMultiTermQueryWrapper(field:/z{5,9}/)]), 0, 0)", ssquery.toQuery().toString());

    };

    @Test
    public void spanSegmentAlterQuery () throws QueryException {
	SpanSegmentQueryWrapper ssquery = new SpanSegmentQueryWrapper("field");
	assertNull(ssquery.toQuery());

	ssquery.with("a");
	assertEquals("field:a", ssquery.toQuery().toString());
	ssquery.with(new SpanAlterQueryWrapper("field", "c", "d"));
	ssquery.with(new SpanRegexQueryWrapper("field", "a.*b"));

	assertEquals("spanSegment(spanSegment(field:a, spanOr([field:c, field:d])), SpanMultiTermQueryWrapper(field:/a.*b/))", ssquery.toQuery().toString());
    };


    @Test
    public void spanSegmentCloneQuery () throws QueryException {
	SpanSegmentQueryWrapper ssquery = new SpanSegmentQueryWrapper("field", "a", "b");
	assertEquals("spanSegment(field:a, field:b)", ssquery.toQuery().toString());

	SpanSegmentQueryWrapper ssquery2 = new SpanSegmentQueryWrapper("field", ssquery);
	assertEquals(ssquery.toQuery().toString(), ssquery2.toQuery().toString());

	SpanSegmentQueryWrapper ssquery3 = ssquery2.clone();
	assertEquals(ssquery.toQuery().toString(), ssquery3.toQuery().toString());
	assertEquals(ssquery2.toQuery().toString(), ssquery3.toQuery().toString());
    };
};
