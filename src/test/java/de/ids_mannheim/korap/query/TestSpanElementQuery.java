package de.ids_mannheim.korap.query;

import java.util.*;
import org.apache.lucene.index.Term;

import de.ids_mannheim.korap.query.SpanElementQuery;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

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
};
