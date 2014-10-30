package de.ids_mannheim.korap.query;

import java.util.List;

import org.apache.lucene.search.spans.SpanQuery;

public abstract class SpanWithIdQuery extends SimpleSpanQuery{

	public SpanWithIdQuery(SpanQuery firstClause, boolean collectPayloads) {
		super(firstClause, collectPayloads);
	}
	
	public SpanWithIdQuery(SpanQuery firstClause, SpanQuery secondClause,
			boolean collectPayloads) {
		super(firstClause, secondClause, collectPayloads);
	}
	
	public SpanWithIdQuery(SpanQuery firstClause,
			List<SpanQuery> secondClauses, boolean collectPayloads) {
		super(firstClause, secondClauses, collectPayloads);
	}
}
