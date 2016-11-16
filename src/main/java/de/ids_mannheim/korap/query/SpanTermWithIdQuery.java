package de.ids_mannheim.korap.query;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.spans.TermSpansWithId;

/**
 * SpanTermWithIdQuery wraps a SpanTermQuery retrieving TermSpans and
 * add a spanid to the TermSpans. It is used in other spanqueries
 * requiring spans with id as their child spans, for example in span
 * relation with variable query ( {@link SpanRelationPartQuery}).
 * 
 * <pre>
 * SpanTermWithIdQuery sq = new SpanTermWithIdQuery(new
 * Term("base","tt:p/NN"),true)
 * </pre>
 * 
 * In this example, the SpanTermWithIdQuery retrieves
 * {@link SpansWithId} for
 * the Term "tt:p/NN".
 * 
 * @author margaretha
 */
public class SpanTermWithIdQuery extends SimpleSpanQuery {

    /**
     * Constructs a SpanTermWithIdQuery for the given term.
     * 
     * @param term
     *            a {@link Term}
     * @param collectPayloads
     *            a boolean flag representing the value
     *            <code>true</code> if payloads are to be collected,
     *            otherwise
     *            <code>false</code>.
     */
    public SpanTermWithIdQuery (Term term, boolean collectPayloads) {
        super(new SpanTermQuery(term), collectPayloads);
    }


    @Override
    public SimpleSpanQuery clone () {
        SpanTermQuery sq = (SpanTermQuery) this.firstClause;
        return new SpanTermWithIdQuery(sq.getTerm(), this.collectPayloads);
    }


    @Override
    public Spans getSpans (LeafReaderContext context, Bits acceptDocs,
            Map<Term, TermContext> termContexts) throws IOException {
        return new TermSpansWithId(this, context, acceptDocs, termContexts);
    }


    @Override
    public String toString (String field) {
        StringBuilder sb = new StringBuilder();
        sb.append("spanTermWithId(");
        sb.append(firstClause.toString(field));
        sb.append(")");
        return sb.toString();
    }

}
