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
 * Shrinks spans to a classed span.
 */
public class SpanMatchModifyClassQuery extends SpanClassQuery {
    private boolean divide = false;

    public SpanMatchModifyClassQuery (SpanQuery operand, byte number, boolean divide) {
	super(operand, number);
	this.divide = divide;
    };

    public SpanMatchModifyClassQuery (SpanQuery operand, boolean divide) {
	this(operand, (byte) 1, divide);
    };

    public SpanMatchModifyClassQuery (SpanQuery operand, byte number) {
	this(operand, number, false);
    };

    public SpanMatchModifyClassQuery (SpanQuery operand) {
	this(operand, (byte) 1, false);
    };

    @Override
    public String toString (String field) {
	StringBuffer buffer = new StringBuffer();
	if (divide) {
	    buffer.append("split(");
	}
	else {
	    buffer.append("shrink(");
	};
	short classNr = (short) this.number;
	buffer.append(classNr & 0xFF).append(": ");
        buffer.append(this.operand.toString());
	buffer.append(')');

	buffer.append(ToStringUtils.boost(getBoost()));
	return buffer.toString();
    };

    @Override
    public Spans getSpans (final AtomicReaderContext context, Bits acceptDocs, Map<Term,TermContext> termContexts) throws IOException {
	return (Spans) new MatchModifyClassSpans(this.operand, context, acceptDocs, termContexts, number, divide);
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
	    this.number,
	    this.divide
        );
	spanMatchQuery.setBoost(getBoost());
	return spanMatchQuery;
    };


    /** Returns true iff <code>o</code> is equal to this. */
    @Override
    public boolean equals(Object o) {
	if (this == o) return true;
	if (!(o instanceof SpanMatchModifyClassQuery)) return false;
	
	final SpanMatchModifyClassQuery spanMatchModifyClassQuery = (SpanMatchModifyClassQuery) o;
	
	if (!this.operand.equals(spanMatchModifyClassQuery.operand)) return false;
	if (this.number != spanMatchModifyClassQuery.number) return false;
	if (this.divide != spanMatchModifyClassQuery.divide) return false;
	return getBoost() == spanMatchModifyClassQuery.getBoost();
    };


    // I don't know what I am doing here
    @Override
    public int hashCode() {
	int result = 1;
	result = operand.hashCode();
	result += number + 33_333;
	result += divide ? 1 : 0;
	result ^= (result << 15) | (result >>> 18);
	result += Float.floatToRawIntBits(getBoost());
	return result;
    };
};
