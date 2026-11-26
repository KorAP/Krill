package de.ids_mannheim.korap.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.queries.spans.SpanQuery;
import org.apache.lucene.queries.spans.SpanTermQuery;
import org.junit.Test;

public class TestSpanFocusQuery {

    private SpanFocusQuery focus (byte ... classNumbers) {
        SpanQuery term = new SpanTermQuery(new Term("base", "s:der"));
        List<Byte> numbers = new ArrayList<Byte>();
        for (byte number : classNumbers)
            numbers.add(number);
        return new SpanFocusQuery(new SpanClassQuery(term, (byte) 1), numbers);
    };


    @Test
    public void testEqualsWithSameClassNumbers () {
        // Different list objects with the same class numbers
        SpanFocusQuery a = focus((byte) 1, (byte) 2);
        SpanFocusQuery b = focus((byte) 1, (byte) 2);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());

        assertNotEquals(a, focus((byte) 1));
        assertNotEquals(a, focus((byte) 2, (byte) 1));
    };


    @Test
    public void testEqualsWithDifferentSettings () {
        SpanFocusQuery a = focus((byte) 1);

        SpanFocusQuery b = focus((byte) 1);
        b.setSorted(false);
        assertNotEquals(a, b);

        b = focus((byte) 1);
        b.setMatchTemporaryClass(true);
        assertNotEquals(a, b);

        b = focus((byte) 1);
        b.setRemoveTemporaryClasses(true);
        assertNotEquals(a, b);
    };


    @Test
    public void testEqualsClone () {
        SpanFocusQuery a = focus((byte) 1);
        a.setSorted(false);
        a.setMatchTemporaryClass(true);
        a.setRemoveTemporaryClasses(true);
        assertEquals(a, a.clone());
    };
};
