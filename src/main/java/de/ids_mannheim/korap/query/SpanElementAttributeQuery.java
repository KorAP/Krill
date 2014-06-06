package de.ids_mannheim.korap.query;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.spans.ElementAttributeSpans;

public class SpanElementAttributeQuery extends SimpleSpanQuery{
	
	public SpanElementAttributeQuery(SpanElementQuery firstClause,
			SpanAttributeQuery secondClause, boolean collectPayloads) {
		super(firstClause, secondClause, collectPayloads);
	}

	@Override
	public SimpleSpanQuery clone() {
		SpanElementAttributeQuery sq = new SpanElementAttributeQuery( 
				(SpanElementQuery) firstClause.clone(), 
				(SpanAttributeQuery) secondClause.clone(), 
				collectPayloads);
		return null;
	}

	@Override
	public Spans getSpans(AtomicReaderContext context, Bits acceptDocs,
			Map<Term, TermContext> termContexts) throws IOException {
		return new ElementAttributeSpans(this, context, acceptDocs, termContexts);
	}

	@Override
	public String toString(String field) {
		StringBuilder sb = new StringBuilder();
		sb.append("spanElementAttribute");
		sb.append("(");
		sb.append(firstClause.toString(field));
		sb.append(", ");
		sb.append(secondClause.toString(field));
		sb.append(")");
		return sb.toString();
	}
}
