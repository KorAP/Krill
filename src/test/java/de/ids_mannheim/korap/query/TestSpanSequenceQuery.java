import java.util.*;
import de.ids_mannheim.korap.query.wrap.SpanSequenceQueryWrapper;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestSpanSequenceQuery {

    @Test
    public void spanSequenceQuery () {
	SpanSequenceQueryWrapper sssq = new SpanSequenceQueryWrapper("field");
	assertNull(sssq.toQuery());
	assertFalse(sssq.hasConstraints());

	sssq.append("a").append("b");
	assertEquals("spanNext(field:a, field:b)", sssq.toQuery().toString());
	assertFalse(sssq.hasConstraints());

	sssq.append("c");
	assertEquals("spanNext(spanNext(field:a, field:b), field:c)", sssq.toQuery().toString());
	assertFalse(sssq.hasConstraints());

	sssq = new SpanSequenceQueryWrapper("field");
	sssq.append("a");
	assertEquals("field:a", sssq.toQuery().toString());
	assertFalse(sssq.hasConstraints());

	sssq.append("b");
	assertEquals("spanNext(field:a, field:b)", sssq.toQuery().toString());
	assertFalse(sssq.hasConstraints());

	sssq.withConstraint(2,3);
	assertTrue(sssq.hasConstraints());

	// Todo: This does not mention the constraint!
	assertEquals("spanDistance(field:a, field:b)", sssq.toQuery().toString());

	sssq.append("c");
	// Todo: This does not mention the constraint!
	assertEquals("spanDistance(spanDistance(field:a, field:b), field:c)", sssq.toQuery().toString());
	sssq.withConstraint(6,8, "s");
	assertTrue(sssq.hasConstraints());


	// Todo: This does not mention the constraint!
	assertEquals("spanMultipleDistance(spanMultipleDistance(field:a, field:b), field:c)", sssq.toQuery().toString());
    };
};
