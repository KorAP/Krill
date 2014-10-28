package de.ids_mannheim.korap.query;

import org.apache.lucene.search.spans.SpanQuery;

public abstract class SpanWithIdQuery extends SimpleSpanQuery{

	public SpanWithIdQuery(SpanQuery firstClause, boolean collectPayloads) {
		super(firstClause, collectPayloads);
	}
	
	public SpanWithIdQuery(SpanQuery firstClause, SpanQuery secondClause,
			boolean collectPayloads) {
		super(firstClause, secondClause, collectPayloads);
	}
}
