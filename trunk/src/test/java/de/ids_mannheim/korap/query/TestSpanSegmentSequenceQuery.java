import java.util.*;
import de.ids_mannheim.korap.query.wrap.SpanSegmentQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanRegexQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanAlterQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanSequenceQueryWrapper;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestSpanSegmentSequenceQuery {

    @Test
    public void spanSegmentSequenceQuery () {
	SpanSequenceQueryWrapper sssq = new SpanSequenceQueryWrapper("field");

	assertNull(sssq.toQuery());

	sssq.append("a").append("b");

	assertEquals("spanNext(field:a, field:b)", sssq.toQuery().toString());

	sssq.append("c");

	assertEquals("spanNext(spanNext(field:a, field:b), field:c)", sssq.toQuery().toString());
    };

    @Test
    public void spanSegmentSequenceQuery2 () {
	SpanSegmentQueryWrapper ssq = new SpanSegmentQueryWrapper("field", "-c", "-d", "-e");
	SpanSequenceQueryWrapper sssq = new SpanSequenceQueryWrapper("field", "a", "b");

	sssq.append(ssq);

	assertEquals("spanNext(spanNext(field:a, field:b), spanNear([spanNear([field:-c, field:-d], -1, false), field:-e], -1, false))", sssq.toQuery().toString());

    };

    @Test
    public void spanSegmentSequenceQuery3 () {
	SpanSequenceQueryWrapper sssq = new SpanSequenceQueryWrapper("field", "a", "b");
	SpanRegexQueryWrapper ssreq = new SpanRegexQueryWrapper("field", "c.?d");

	sssq.append(ssreq);

	assertEquals("spanNext(spanNext(field:a, field:b), SpanMultiTermQueryWrapper(field:/c.?d/))", sssq.toQuery().toString());
    };

    @Test
    public void spanSegmentSequenceQueryPrepend () {
	SpanSequenceQueryWrapper sssq = new SpanSequenceQueryWrapper("field", "b", "c");

	sssq.prepend("a");

	assertEquals("spanNext(spanNext(field:a, field:b), field:c)", sssq.toQuery().toString());
    };

    @Test
    public void spanSegmentSequenceQueryPrepend2 () {
	SpanSequenceQueryWrapper sssq = new SpanSequenceQueryWrapper("field", "d", "e");
	SpanSegmentQueryWrapper ssq = new SpanSegmentQueryWrapper("field", "-a", "-b", "-c");

	sssq.prepend(ssq);

	assertEquals("spanNext(spanNext(spanNear([spanNear([field:-a, field:-b], -1, false), field:-c], -1, false), field:d), field:e)", sssq.toQuery().toString());
    };

    @Test
    public void spanSegmentSequenceQueryPrepend3 () {
	SpanSequenceQueryWrapper sssq = new SpanSequenceQueryWrapper("field", "c", "d");
	SpanRegexQueryWrapper ssreq = new SpanRegexQueryWrapper("field", "a.?b");

	sssq.prepend(ssreq);

	assertEquals("spanNext(spanNext(SpanMultiTermQueryWrapper(field:/a.?b/), field:c), field:d)", sssq.toQuery().toString());
    };
};
