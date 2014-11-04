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

import de.ids_mannheim.korap.query.spans.ClassSpans;


/**
 * Marks spans with a special class payload.
 */
public class SpanClassQuery extends SpanQuery {
    public String field;
    protected byte number;
    protected SpanQuery operand;

    public SpanClassQuery (SpanQuery operand, byte number) {
	this.field = operand.getField();
	this.operand = operand;
	this.number = number;
    };

    public SpanClassQuery (SpanQuery operand) {
	this.field = operand.getField();
	this.operand = operand;
	this.number = (byte) 1;
    };

    public byte number () {
	return this.number;
    };

    @Override
    public String getField () { return field; }

    @Override
    public void extractTerms (Set<Term> terms) {
	this.operand.extractTerms(terms);
    };

    @Override
    public String toString (String field) {
	StringBuffer buffer = new StringBuffer("{");
	short classNr = (short) this.number;
	buffer.append(classNr & 0xFF).append(": ");
        buffer.append(this.operand.toString()).append('}');
	buffer.append(ToStringUtils.boost(getBoost()));
	return buffer.toString();
    };

    @Override
    public Spans getSpans (final AtomicReaderContext context,
			   Bits acceptDocs,
			   Map<Term,TermContext> termContexts) throws IOException {
	return (Spans) new ClassSpans(
	    this.operand,
	    context,
	    acceptDocs,
	    termContexts,
	    number
        );
    };

    @Override
    public Query rewrite (IndexReader reader) throws IOException {
	SpanClassQuery clone = null;
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
    public SpanClassQuery clone() {
	SpanClassQuery spanClassQuery = new SpanClassQuery(
	    (SpanQuery) this.operand.clone(),
	    this.number
        );
	spanClassQuery.setBoost(getBoost());
	return spanClassQuery;
    };


    /** Returns true iff <code>o</code> is equal to this. */
    @Override
    public boolean equals (Object o) {
	if (this == o) return true;
	if (!(o instanceof SpanClassQuery)) return false;
	
	final SpanClassQuery spanClassQuery = (SpanClassQuery) o;
	
	if (!this.operand.equals(spanClassQuery.operand)) return false;

	if (this.number != spanClassQuery.number) return false;

	return getBoost() == spanClassQuery.getBoost();
    };


    // I don't know what I am doing here
    @Override
    public int hashCode() {
	int result = 1;
	result = operand.hashCode();
	result += (int) number;
	result ^= (result << 15) | (result >>> 18);
	result += Float.floatToRawIntBits(getBoost());
	return result;
    };
};
