package de.ids_mannheim.korap.query;

import java.util.*;
import de.ids_mannheim.korap.query.wrap.SpanSequenceQueryWrapper;
import de.ids_mannheim.korap.query.SpanWithinQuery;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestSpanWithinQuery {
    @Test
    public void spanSegmentWithinQuery () {

	SpanSequenceQueryWrapper ssquery = new SpanSequenceQueryWrapper("field", "a", "b", "c");
	SpanWithinQuery ssequery = new SpanWithinQuery("s", ssquery.toQuery());

	assertEquals("spanContain(<field:s />, spanNext(spanNext(field:a, field:b), field:c))", ssequery.toString());

	ssquery = new SpanSequenceQueryWrapper("field", "a", "b");
	ssequery = new SpanWithinQuery("p", ssquery.toQuery());
	assertEquals("spanContain(<field:p />, spanNext(field:a, field:b))", ssequery.toString());

    };
    
};
