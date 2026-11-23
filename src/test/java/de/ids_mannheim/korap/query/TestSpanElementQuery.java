package de.ids_mannheim.korap.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.index.Term;

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


    // Every element query has to report its own term,
    // independent of other element queries created later
    @Test
    public void spanElementQueryExtractTerms () {
        SpanElementQuery sQuery = new SpanElementQuery("tokens", "s");
        SpanElementQuery pQuery = new SpanElementQuery("tokens", "p");

        Set<Term> terms = new HashSet<>();
        sQuery.extractTerms(terms);
        assertEquals(1, terms.size());
        assertTrue(terms.contains(new Term("tokens", "<>:s")));

        terms.clear();
        pQuery.extractTerms(terms);
        assertEquals(1, terms.size());
        assertTrue(terms.contains(new Term("tokens", "<>:p")));

        // Cloning must not change the term of the original query
        SpanElementQuery pClone = (SpanElementQuery) pQuery.clone();
        new SpanElementQuery("base", "x");
        terms.clear();
        sQuery.extractTerms(terms);
        pClone.extractTerms(terms);
        assertEquals(2, terms.size());
        assertTrue(terms.contains(new Term("tokens", "<>:s")));
        assertTrue(terms.contains(new Term("tokens", "<>:p")));
    };
};
