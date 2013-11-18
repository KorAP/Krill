import java.util.*;
import de.ids_mannheim.korap.query.wrap.SpanSegmentQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanRegexQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanAlterQueryWrapper;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestSpanSegmentQuery {
    @Test
    public void spanSegmentQuery () {

	SpanSegmentQueryWrapper ssquery = new SpanSegmentQueryWrapper("field","a");
	assertEquals("field:a", ssquery.toQuery().toString());

	ssquery = new SpanSegmentQueryWrapper("field", "a", "b");
	assertEquals("spanNear([field:a, field:b], -1, false)", ssquery.toQuery().toString());

	ssquery = new SpanSegmentQueryWrapper("field","a", "b", "c");
	assertEquals("spanNear([spanNear([field:a, field:b], -1, false), field:c], -1, false)", ssquery.toQuery().toString());
    };

    @Test
    public void spanSegmentQueryExclusive () {

	SpanSegmentQueryWrapper ssquery = new SpanSegmentQueryWrapper("field","a");
	assertEquals("field:a", ssquery.toQuery().toString());

	ssquery = new SpanSegmentQueryWrapper("field", "a", "b");
	assertEquals("spanNear([field:a, field:b], -1, false)", ssquery.toQuery().toString());

	ssquery.without("c");
	assertEquals("spanNot(spanNear([field:a, field:b], -1, false), field:c)", ssquery.toQuery().toString());

	ssquery.without("d");
	assertEquals("spanNot(spanNear([field:a, field:b], -1, false), spanOr([field:c, field:d]))", ssquery.toQuery().toString());
    };


    @Test
    public void spanSegmentRegexQuery () {
	SpanSegmentQueryWrapper ssquery = new SpanSegmentQueryWrapper("field");
	assertNull(ssquery.toQuery());
	ssquery.with("a");
	assertEquals("field:a", ssquery.toQuery().toString());

	ssquery.with(new SpanRegexQueryWrapper("field", "a.*b"));

	assertEquals("spanNear([field:a, SpanMultiTermQueryWrapper(field:/a.*b/)], -1, false)", ssquery.toQuery().toString());

	ssquery.with("c");

	assertEquals("spanNear([spanNear([field:a, SpanMultiTermQueryWrapper(field:/a.*b/)], -1, false), field:c], -1, false)", ssquery.toQuery().toString());

	ssquery.with("d").with("e");

	assertEquals("spanNear([spanNear([spanNear([spanNear([field:a, SpanMultiTermQueryWrapper(field:/a.*b/)], -1, false), field:c], -1, false), field:d], -1, false), field:e], -1, false)", ssquery.toQuery().toString());

	ssquery.without(new SpanRegexQueryWrapper("field", "x.?y"));

	assertEquals("spanNot(spanNear([spanNear([spanNear([spanNear([field:a, SpanMultiTermQueryWrapper(field:/a.*b/)], -1, false), field:c], -1, false), field:d], -1, false), field:e], -1, false), SpanMultiTermQueryWrapper(field:/x.?y/))", ssquery.toQuery().toString());

	ssquery.without(new SpanRegexQueryWrapper("field", "z{5,9}"));

	assertEquals("spanNot(spanNear([spanNear([spanNear([spanNear([field:a, SpanMultiTermQueryWrapper(field:/a.*b/)], -1, false), field:c], -1, false), field:d], -1, false), field:e], -1, false), spanOr([SpanMultiTermQueryWrapper(field:/x.?y/), SpanMultiTermQueryWrapper(field:/z{5,9}/)]))", ssquery.toQuery().toString());

    };

    @Test
    public void spanSegmentAlterQuery () {
	SpanSegmentQueryWrapper ssquery = new SpanSegmentQueryWrapper("field");
	assertNull(ssquery.toQuery());

	ssquery.with("a");
	assertEquals("field:a", ssquery.toQuery().toString());
	ssquery.with(new SpanAlterQueryWrapper("field", "c", "d"));
	ssquery.with(new SpanRegexQueryWrapper("field", "a.*b"));

	assertEquals("spanNear([spanNear([field:a, spanOr([field:c, field:d])], -1, false), SpanMultiTermQueryWrapper(field:/a.*b/)], -1, false)", ssquery.toQuery().toString());
    };


    @Test
    public void spanSegmentCloneQuery () {
	SpanSegmentQueryWrapper ssquery = new SpanSegmentQueryWrapper("field", "a", "b");
	assertEquals("spanNear([field:a, field:b], -1, false)", ssquery.toQuery().toString());

	SpanSegmentQueryWrapper ssquery2 = new SpanSegmentQueryWrapper("field", ssquery);
	assertEquals(ssquery.toQuery().toString(), ssquery2.toQuery().toString());

	SpanSegmentQueryWrapper ssquery3 = ssquery2.clone();
	assertEquals(ssquery.toQuery().toString(), ssquery3.toQuery().toString());
	assertEquals(ssquery2.toQuery().toString(), ssquery3.toQuery().toString());
    };
};
