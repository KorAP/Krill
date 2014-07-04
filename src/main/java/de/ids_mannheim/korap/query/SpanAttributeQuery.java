package de.ids_mannheim.korap.query;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.ToStringUtils;

import de.ids_mannheim.korap.query.spans.AttributeSpans;

public class SpanAttributeQuery extends SimpleSpanQuery{
	
	boolean isNegation;
	
	public SpanAttributeQuery(SpanTermQuery firstClause, boolean collectPayloads) {
		super(firstClause, collectPayloads);
	}
	
	public SpanAttributeQuery(SpanTermQuery firstClause, boolean isNegation, boolean collectPayloads) {
		super(firstClause, collectPayloads);
		this.isNegation = isNegation;
	}

	@Override
	public SimpleSpanQuery clone() {
		SpanAttributeQuery sq = new SpanAttributeQuery(
				(SpanTermQuery) this.firstClause.clone(), 
				this.collectPayloads);
		sq.setBoost(getBoost());
		return sq;
	}

	@Override
	public Spans getSpans(AtomicReaderContext context, Bits acceptDocs,
			Map<Term, TermContext> termContexts) throws IOException {
		return new AttributeSpans(this, context, acceptDocs, termContexts);
	}

	@Override
	public String toString(String field) {
		StringBuilder sb = new StringBuilder();
		sb.append("spanAttribute(");
		sb.append(firstClause.toString(field));
		sb.append(")");
		sb.append(ToStringUtils.boost(getBoost()));
		return sb.toString();
	}

	public boolean isNegation() {
		return isNegation;
	}

	public void setNegation(boolean isNegation) {
		this.isNegation = isNegation;
	}

}
