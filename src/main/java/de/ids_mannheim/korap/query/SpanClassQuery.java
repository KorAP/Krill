package de.ids_mannheim.korap.query;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.ToStringUtils;

import de.ids_mannheim.korap.query.spans.ClassSpans;

/**
 * Marks spans with a special class payload.
 */
public class SpanClassQuery extends SimpleSpanQuery {
    protected byte number = 1;


    public SpanClassQuery (SpanQuery operand) {
        super(operand, false);
    };


    public SpanClassQuery (SpanQuery operand, byte number) {
        super(operand, false);
        this.number = number;
    };


    @Override
    public String toString (String field) {
        StringBuffer buffer = new StringBuffer("{");
        short classNr = (short) this.number;
        buffer.append(classNr & 0xFF).append(": ");
        buffer.append(this.firstClause.toString()).append('}');
        buffer.append(ToStringUtils.boost(getBoost()));
        return buffer.toString();
    };


    @Override
    public Spans getSpans (final LeafReaderContext context, Bits acceptDocs,
            Map<Term, TermContext> termContexts) throws IOException {
        return (Spans) new ClassSpans(this.firstClause, context, acceptDocs,
                termContexts, number);
    };


    @Override
    public Query rewrite (IndexReader reader) throws IOException {

        SpanQuery query = (SpanQuery) this.firstClause.rewrite(reader);

        if (query != this.firstClause) {

            // Rewritten spanquery is empty
            if (query.getField() == null) {

                // Return the empty child query
                return query;
            };

            SpanClassQuery clone = this.clone();
            clone.firstClause = query;
            return clone;
        };
        return this;
    };


    @Override
    public SpanClassQuery clone () {
        SpanClassQuery spanClassQuery = new SpanClassQuery(
                (SpanQuery) this.firstClause.clone(), this.number);
        spanClassQuery.setBoost(getBoost());
        return spanClassQuery;
    };


    /** Returns true iff <code>o</code> is equal to this. */
    @Override
    public boolean equals (Object o) {
        if (this == o)
            return true;
        if (!(o instanceof SpanClassQuery))
            return false;

        final SpanClassQuery spanClassQuery = (SpanClassQuery) o;

        if (!this.firstClause.equals(spanClassQuery.firstClause))
            return false;

        if (this.number != spanClassQuery.number)
            return false;

        return getBoost() == spanClassQuery.getBoost();
    };


    // I don't know what I am doing here
    @Override
    public int hashCode () {
        int result = 1;
        result = firstClause.hashCode();
        result += (int) number;
        result ^= (result << 15) | (result >>> 18);
        result += Float.floatToRawIntBits(getBoost());
        return result;
    }


    public byte getNumber () {
        return number;
    }


    public void setNumber (byte number) {
        this.number = number;
    };
};
