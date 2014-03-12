package de.ids_mannheim.korap.query;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.ToStringUtils;

import de.ids_mannheim.korap.query.spans.QuantifierSpans;

public class SpanQuantifierQuery extends SimpleSpanQuery{
	
	private int min, max;
	
	public SpanQuantifierQuery(SpanQuery sq, int min, int max,
			boolean collectPayloads) {
		super(sq, collectPayloads);
		this.min = min;
		this.max = max;
	}

	@Override
	public SimpleSpanQuery clone() {
		SpanQuantifierQuery sq = new SpanQuantifierQuery(
				(SpanQuery) this.firstClause.clone(), 
				this.min, 
				this.max, 
				this.collectPayloads);
		sq.setBoost(getBoost());
		return sq;
	}

	@Override
	public Spans getSpans(AtomicReaderContext context, Bits acceptDocs,
			Map<Term, TermContext> termContexts) throws IOException {
		return new QuantifierSpans(this, context, acceptDocs, termContexts);
	}

	@Override
	public String toString(String field) {
		StringBuilder sb = new StringBuilder();		
		sb.append("spanQuantifier(");
		sb.append(firstClause.toString(field));
		sb.append("[");
		sb.append(min);
		sb.append(":");
		sb.append(max);
		sb.append("])");
		sb.append(ToStringUtils.boost(getBoost()));
		return sb.toString();
	}

	public int getMin() {
		return min;
	}

	public void setMin(int min) {
		this.min = min;
	}

	public int getMax() {
		return max;
	}

	public void setMax(int max) {
		this.max = max;
	}
	
}
