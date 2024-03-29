package de.ids_mannheim.korap.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author diewald
 */

@RunWith(JUnit4.class)
public class TestSpanElementQuery {

    @Test
    public void spanElementQuery () {
        SpanElementQuery sequery = new SpanElementQuery("field", "b");
        assertEquals("<field:b />", sequery.toString());
    };


    @Test
    public void spanElement2Query () {
        SpanElementQuery sequery = new SpanElementQuery("field", "xyz");
        assertEquals("<field:xyz />", sequery.toString());
    };


    @Test
    public void spanElement3Query () {
        SpanElementQuery sequery = new SpanElementQuery("field", "");
        assertEquals("<field: />", sequery.toString());
    };
};
