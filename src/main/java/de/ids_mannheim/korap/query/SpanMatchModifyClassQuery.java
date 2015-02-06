package de.ids_mannheim.korap.query;

import java.io.IOException;

import java.util.Set;
import java.util.Map;

import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.ToStringUtils;

import de.ids_mannheim.korap.query.spans.MatchModifyClassSpans;
import de.ids_mannheim.korap.query.SpanClassQuery;

/**
 * Modify the span of a match to the boundaries of a certain class.
 *
 * In case multiple classes are found with the very same number, the span
 * is maximized to start on the first occurrence from the left and end on
 * the last occurrence on the right.
 *
 * In case the class to modify on is not found in the subquery,
 * the match is ignored.
 *
 * @author diewald
 *
 * @see MatchModifyClassSpans
 */
public class SpanMatchModifyClassQuery extends SpanClassQuery {

    /**
     * Construct a new SpanMatchModifyClassQuery.
     *
     * @param operand The nested {@link SpanQuery}, that contains one or
     *        more classed spans.
     * @param number The class number to focus on.
     */
    public SpanMatchModifyClassQuery (SpanQuery operand, byte number) {
        super(operand, number);
    };


    /**
     * Construct a new SpanMatchModifyClassQuery.
     * The class to focus on defaults to <tt>1</tt>.
     *
     * @param operand The nested {@link SpanQuery}, that contains one or
     *        more classed spans.
     */
    public SpanMatchModifyClassQuery (SpanQuery operand) {
        this(operand, (byte) 1);
    };


    @Override
    public String toString (String field) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("focus(");
        short classNr = (short) this.number;
        buffer.append(classNr & 0xFF).append(": ");
        buffer.append(this.operand.toString());
        buffer.append(')');
        buffer.append(ToStringUtils.boost(getBoost()));
        return buffer.toString();
    };


    @Override
    public Spans getSpans (final AtomicReaderContext context,
                           Bits acceptDocs,
                           Map<Term,TermContext> termContexts) throws IOException {
        return (Spans) new MatchModifyClassSpans(
            this.operand,
            context,
            acceptDocs,
            termContexts,
            number
        );
    };


    @Override
    public Query rewrite (IndexReader reader) throws IOException {
        SpanMatchModifyClassQuery clone = null;
        SpanQuery query = (SpanQuery) this.operand.rewrite(reader);
        
        if (query != this.operand) {
            if (clone == null)
                clone = this.clone();
            clone.operand = query;
        };

        if (clone != null)
            return clone;

        return this;
    };


    @Override
    public SpanMatchModifyClassQuery clone() {
        SpanMatchModifyClassQuery spanMatchQuery = new SpanMatchModifyClassQuery(
            (SpanQuery) this.operand.clone(),
            this.number
        );
        spanMatchQuery.setBoost(getBoost());
        return spanMatchQuery;
    };


    @Override
    public boolean equals (Object o) {
        if (this == o) return true;
        if (!(o instanceof SpanMatchModifyClassQuery)) return false;
	
        final SpanMatchModifyClassQuery spanMatchModifyClassQuery =
            (SpanMatchModifyClassQuery) o;
	
        if (!this.operand.equals(spanMatchModifyClassQuery.operand)) return false;
        if (this.number != spanMatchModifyClassQuery.number) return false;
        return getBoost() == spanMatchModifyClassQuery.getBoost();
    };


    @Override
    public int hashCode () {
        int result = operand.hashCode();
        result = 31 * result + number;
        result += Float.floatToRawIntBits(getBoost());
        return result;
    };
};
