package de.ids_mannheim.korap.query;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.spans.SpanQuery;
import org.apache.lucene.queries.spans.Spans;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.spans.SegmentSpans;
import de.ids_mannheim.korap.query.spans.SpanWeights;

/**
 * SpanSegmentQuery matches two spans having exactly the same start
 * and end
 * positions, for instance:
 * 
 * <pre>
 * sq = new SpanSegmentQuery(new SpanTermQuery(new
 * Term(&quot;tokens&quot;, &quot;s:Hund&quot;)),
 * new SpanTermQuery(new Term(&quot;tokens&quot;,
 * &quot;tt/p:NN&quot;)));
 * </pre>
 * 
 * @author margaretha
 */
public class SpanSegmentQuery extends SimpleSpanQuery {

    private boolean isRelation;


    /**
     * Constructs a SpanSegmentQuery from the two given SpanQueries,
     * by default
     * payloads are to be collected.
     * 
     * @param firstClause
     *            a {@link SpanQuery}
     * @param secondClause
     *            a {@link SpanQuery}
     */
    public SpanSegmentQuery (SpanQuery firstClause, SpanQuery secondClause) {
        this(firstClause, secondClause, true);
    }


    /**
     * Constructs a SpanSegmentQuery from the two given SpanQueries.
     * 
     * @param firstClause
     *            a {@link SpanQuery}
     * @param secondClause
     *            a {@link SpanQuery}
     * @param collectPayloads
     *            a boolean flag representing the value
     *            <code>true</code> if payloads are to be collected,
     *            otherwise
     *            <code>false</code>.
     */
    public SpanSegmentQuery (SpanQuery firstClause, SpanQuery secondClause,
                             boolean collectPayloads) {
        super(firstClause, secondClause, collectPayloads);
        if (firstClause instanceof SpanRelationQuery) {
            isRelation = true;
        }
    }


    @Override
    public Spans getSpans (LeafReaderContext context, Bits acceptDocs,
            SpanWeights weights) throws IOException {
        return (Spans) new SegmentSpans(this, context, acceptDocs,
                weights);
    }


    @Override
    public SpanSegmentQuery clone () {
        SpanSegmentQuery spanSegmentQuery = new SpanSegmentQuery(
                (SpanQuery) cloneClause(firstClause),
                (SpanQuery) cloneClause(secondClause), collectPayloads);
        return spanSegmentQuery;
    }


    @Override
    public String toString (String field) {
        StringBuilder sb = new StringBuilder();
        sb.append("spanSegment(");
        sb.append(firstClause.toString(field));
        sb.append(", ");
        sb.append(secondClause.toString(field));
        sb.append(")");
        return sb.toString();
    }


    @Override
    public boolean equals (Object o) {
        if (this == o)
            return true;
        if (!(o instanceof SpanSegmentQuery))
            return false;

        SpanSegmentQuery spanSegmentQuery = (SpanSegmentQuery) o;

        if (collectPayloads != spanSegmentQuery.collectPayloads)
            return false;
        if (!firstClause.equals(spanSegmentQuery.firstClause))
            return false;
        if (!secondClause.equals(spanSegmentQuery.secondClause))
            return false;

        return true;
    }


    @Override
    public int hashCode () {
        int result;
        result = firstClause.hashCode() + secondClause.hashCode();
        result ^= (31 * result) + (result >>> 3);
        return result;
    }


    public boolean isRelation () {
        return isRelation;
    }


    public void setRelation (boolean isRelation) {
        this.isRelation = isRelation;
    }
}
