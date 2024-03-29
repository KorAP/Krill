package de.ids_mannheim.korap.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import de.ids_mannheim.korap.query.wrap.SpanRegexQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanSegmentQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanSequenceQueryWrapper;
import de.ids_mannheim.korap.util.QueryException;

@RunWith(JUnit4.class)
public class TestSpanSegmentSequenceQuery {

    @Test
    public void spanSegmentSequenceQuery () throws QueryException {
        SpanSequenceQueryWrapper sssq = new SpanSequenceQueryWrapper("field");

        assertNull(sssq.toQuery());

        sssq.append("a").append("b");

        assertEquals("spanNext(field:a, field:b)", sssq.toQuery().toString());

        sssq.append("c");

        assertEquals("spanNext(spanNext(field:a, field:b), field:c)",
                sssq.toQuery().toString());
    };


    @Test
    public void spanSegmentSequenceQuery2 () throws QueryException {
        SpanSegmentQueryWrapper ssq = new SpanSegmentQueryWrapper("field", "-c",
                "-d", "-e");
        SpanSequenceQueryWrapper sssq = new SpanSequenceQueryWrapper("field",
                "a", "b");

        sssq.append(ssq);

        assertEquals(
                "spanNext(spanNext(field:a, field:b), spanSegment(spanSegment(field:-c, field:-d), field:-e))",
                sssq.toQuery().toString());

    };


    @Test
    public void spanSegmentSequenceQuery3 () throws QueryException {
        SpanSequenceQueryWrapper sssq = new SpanSequenceQueryWrapper("field",
                "a", "b");
        SpanRegexQueryWrapper ssreq = new SpanRegexQueryWrapper("field",
                "c.?d");

        sssq.append(ssreq);

        assertEquals(
                "spanNext(spanNext(field:a, field:b), SpanMultiTermQueryWrapper(field:/c.?d/))",
                sssq.toQuery().toString());
    };


    @Test
    public void spanSegmentSequenceQueryPrepend () throws QueryException {
        SpanSequenceQueryWrapper sssq = new SpanSequenceQueryWrapper("field",
                "b", "c");

        sssq.prepend("a");

        assertEquals("spanNext(spanNext(field:a, field:b), field:c)",
                sssq.toQuery().toString());
    };


    @Test
    public void spanSegmentSequenceQueryPrepend2 () throws QueryException {
        SpanSequenceQueryWrapper sssq = new SpanSequenceQueryWrapper("field",
                "d", "e");
        SpanSegmentQueryWrapper ssq = new SpanSegmentQueryWrapper("field", "-a",
                "-b", "-c");

        sssq.prepend(ssq);

        assertEquals(
                "spanNext(spanNext(spanSegment(spanSegment(field:-a, field:-b), field:-c), field:d), field:e)",
                sssq.toQuery().toString());
    };


    @Test
    public void spanSegmentSequenceQueryPrepend3 () throws QueryException {
        SpanSequenceQueryWrapper sssq = new SpanSequenceQueryWrapper("field",
                "c", "d");
        SpanRegexQueryWrapper ssreq = new SpanRegexQueryWrapper("field",
                "a.?b");

        sssq.prepend(ssreq);

        assertEquals(
                "spanNext(spanNext(SpanMultiTermQueryWrapper(field:/a.?b/), field:c), field:d)",
                sssq.toQuery().toString());
    };
};
