package de.ids_mannheim.korap.query;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.spans.ElementSpans;
import de.ids_mannheim.korap.query.spans.SubSpans;

/**
 * This query extracts a subspan from another span. The subspan starts
 * from a
 * startOffset until startOffset + length. A positive startOffset is
 * counted
 * from the start of the span, while a negative startOffset is
 * calculated from
 * the end of the span. <br />
 * <br />
 * SpanSubspanQuery takes a SpanQuery as its input and creates
 * subspans from the
 * resulting spans of the SpanQuery. For instance:
 * 
 * <pre>
 * SpanElementQuery seq = new SpanElementQuery(new
 * SpanElementQuery(&quot;tokens&quot;, &quot;s&quot;);
 * SpanSubspanQuery ssq = new SpanSubspanQuery(seq, 0, 2, true);
 * </pre>
 * 
 * In this example, the SpanSubspanQuery creates subspans, that are
 * the first
 * two tokens of all sentences.
 * 
 * @author margaretha
 * */
public class SpanSubspanQuery extends SimpleSpanQuery {

    private int startOffset, length;


    /**
     * Creates a SpanSubspanQuery (subspan) from the given
     * {@link SpanQuery} with the specified startOffset and length.
     * 
     * @param firstClause
     *            a SpanQuery
     * @param startOffset
     *            the start offset of the subspan relative to the
     *            original span
     * @param length
     *            the length of the subspan
     * @param collectPayloads
     *            a boolean flag representing the value
     *            <code>true</code> if payloads are to be collected,
     *            otherwise
     *            <code>false</code>.
     */
    public SpanSubspanQuery (SpanQuery firstClause, int startOffset,
                             int length, boolean collectPayloads) {
        super(firstClause, collectPayloads);
        this.startOffset = startOffset;
        this.length = length;
    }


    @Override
    public SimpleSpanQuery clone () {
        SpanSubspanQuery sq = new SpanSubspanQuery(this.getFirstClause(),
                this.startOffset, this.length, this.collectPayloads);
        sq.setBoost(this.getBoost());
        return sq;
    }


    @Override
    public Spans getSpans (LeafReaderContext context, Bits acceptDocs,
            Map<Term, TermContext> termContexts) throws IOException {
        return new SubSpans(this, context, acceptDocs, termContexts);
    }


    @Override
    public String toString (String field) {
        StringBuilder sb = new StringBuilder();
        sb.append("subspan(");
        sb.append(this.firstClause.toString());
        sb.append(", ");
        sb.append(this.startOffset);
        sb.append(", ");
        sb.append(this.length);
        sb.append(")");
        return sb.toString();
    }


    /**
     * Returns the start offset.
     * 
     * @return the start offset.
     */
    public int getStartOffset () {
        return startOffset;
    }


    /**
     * Sets the start offset.
     * 
     * @param startOffset
     *            the start offset
     */
    public void setStartOffset (int startOffset) {
        this.startOffset = startOffset;
    }


    /**
     * Returns the length of the subspan.
     * 
     * @return the length of the subspan
     */
    public int getLength () {
        return length;
    }


    /**
     * Sets the length of the subspan.
     * 
     * @param length
     *            the length of the subspan.
     */
    public void setLength (int length) {
        this.length = length;
    }
}
