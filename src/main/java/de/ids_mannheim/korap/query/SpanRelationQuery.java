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

import de.ids_mannheim.korap.query.spans.RelationSpans;

public class SpanRelationQuery extends SimpleSpanQuery {

	public SpanRelationQuery(SpanQuery firstClause, boolean collectPayloads) {
		super(firstClause, collectPayloads);
	}

	@Override
	public SimpleSpanQuery clone() {
		SimpleSpanQuery sq = new SpanRelationQuery(
				(SpanQuery) this.firstClause.clone(),
				this.collectPayloads);
		return sq;
	}

	@Override
	public Spans getSpans(AtomicReaderContext context, Bits acceptDocs,
			Map<Term, TermContext> termContexts) throws IOException {
		return new RelationSpans(this, context, acceptDocs, termContexts);
	}

	@Override
	public String toString(String field) {
		StringBuilder sb = new StringBuilder();		
		sb.append("spanRelation(");
		sb.append(firstClause.toString(field));
		sb.append(")");
		sb.append(ToStringUtils.boost(getBoost()));
		return sb.toString();
	}

}
