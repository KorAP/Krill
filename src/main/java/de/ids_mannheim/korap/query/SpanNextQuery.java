package de.ids_mannheim.korap.query;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.search.spans.TermSpans;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.ToStringUtils;

import de.ids_mannheim.korap.query.spans.NextSpans;

/*
 * Based on SpanNearQuery
 * Todo: Make one Spanarray and switch between the results of A and B.
 */

/**
 * SpanNextQuery matches two spans which are directly next to each other. It is
 * identical to a phrase query with exactly two clauses.
 * 
 * In the example below, the SpanNextQuery retrieves {@link NextSpans} starting
 * from the start position of {@link TermSpans} "turn" and ending at the end
 * position of {@link TermSpans} "off" occurring immediately after the
 * {@link TermSpans} "turn".
 * 
 * <pre>
 * SpanNextQuery sq = new SpanNextQuery(
 *      new SpanTermQuery(new Term(&quot;tokens&quot;,&quot;s:turn&quot;)), 
 *      new SpanTermQuery(new Term(&quot;tokens&quot;, &quot;s:off&quot;)));
 * </pre>
 * 
 * @author diewald
 * @author margaretha
 * 
 */
public class SpanNextQuery extends SimpleSpanQuery implements Cloneable {

    /**
     * Constructs a SpanNextQuery for the two specified {@link SpanQuery
     * SpanQueries} whose payloads are to be collected for the resulting
     * {@link NextSpans}. The first SpanQuery is immediately followed by the
     * second SpanQuery.
     * 
     * @param firstClause the first SpanQuery
     * @param secondClause the second SpanQuery
     */
    public SpanNextQuery(SpanQuery firstClause, SpanQuery secondClause) {
        this(firstClause, secondClause, true);
    };

    /**
     * Constructs a SpanNextQuery for the two specified {@link SpanQuery
     * SpanQueries} where the first SpanQuery is immediately followed by the
     * second SpanQuery.
     * 
     * @param firstClause the first SpanQuery
     * @param secondClause the second SpanQuery
     * @param collectPayloads a boolean flag representing the value
     *        <code>true</code> if payloads are to be collected, otherwise
     *        <code>false</code>.
     */
    public SpanNextQuery(SpanQuery firstClause, SpanQuery secondClause,
            boolean collectPayloads) {
        super(firstClause, secondClause, collectPayloads);
    };

    @Override
    public Spans getSpans(final AtomicReaderContext context, Bits acceptDocs,
            Map<Term, TermContext> termContexts) throws IOException {
        return (Spans) new NextSpans(this, context, acceptDocs, termContexts);
    };

    @Override
    public SpanNextQuery clone() {
        SpanNextQuery spanNextQuery = new SpanNextQuery(
                (SpanQuery) firstClause.clone(),
                (SpanQuery) secondClause.clone(), collectPayloads);
        spanNextQuery.setBoost(getBoost());
        return spanNextQuery;
    };

    /*
     * Rewrite query in case it includes regular expressions or wildcards
     */
    @Override
    public Query rewrite(IndexReader reader) throws IOException {
        SpanNextQuery clone = null;

        // Does the first clause needs a rewrite?
        SpanQuery query = (SpanQuery) firstClause.rewrite(reader);
        if (query != firstClause) {
            if (clone == null)
                clone = this.clone();
            clone.firstClause = query;
        }
        ;

        // Does the second clause needs a rewrite?
        query = (SpanQuery) secondClause.rewrite(reader);
        if (query != secondClause) {
            if (clone == null)
                clone = this.clone();
            clone.secondClause = query;
        }
        ;

        // There is a clone and it is important
        if (clone != null)
            return clone;

        return this;
    };

    @Override
    public String toString(String field) {
        StringBuilder sb = new StringBuilder();
        sb.append("spanNext(");
        sb.append(firstClause.toString(field));
        sb.append(", ");
        sb.append(secondClause.toString(field));
        sb.append(")");
        sb.append(ToStringUtils.boost(getBoost()));
        return sb.toString();
    }

    /** Returns true iff <code>o</code> is equal to this. */
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof SpanNextQuery))
            return false;

        final SpanNextQuery spanNextQuery = (SpanNextQuery) o;

        if (collectPayloads != spanNextQuery.collectPayloads)
            return false;
        if (!firstClause.equals(spanNextQuery.firstClause))
            return false;
        if (!secondClause.equals(spanNextQuery.secondClause))
            return false;

        return getBoost() == spanNextQuery.getBoost();
    };

    // I don't know what I am doing here
    @Override
    public int hashCode() {
        int result;
        result = firstClause.hashCode() + secondClause.hashCode();
        result ^= (result << 31) | (result >>> 2); // reversible
        result += Float.floatToRawIntBits(getBoost());
        return result;
    };
};
