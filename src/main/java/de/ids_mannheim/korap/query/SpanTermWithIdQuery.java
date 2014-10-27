package de.ids_mannheim.korap.query;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.spans.TermSpansWithId;

/** This query wraps the usual SpanTermQuery and returns TermSpans with a spanid property.
 * 	This query is used in other spanqueries that require spans with id as their child spans, 
 * 	for example span relation with variable query.
 * 	
 * 	@author margaretha
 * */
public class SpanTermWithIdQuery extends SimpleSpanQuery{

	public SpanTermWithIdQuery(Term term, boolean collectPayloads) {
		super(new SpanTermQuery(term), collectPayloads);		
	}

	@Override
	public SimpleSpanQuery clone() {	
		SpanTermQuery sq = (SpanTermQuery)  this.firstClause;
		return new SpanTermWithIdQuery(sq.getTerm(), 
				this.collectPayloads
		);
	}

	@Override
	public Spans getSpans(AtomicReaderContext context, Bits acceptDocs,
			Map<Term, TermContext> termContexts) throws IOException {
		return new TermSpansWithId(this, context, acceptDocs, termContexts);
	}

	@Override
	public String toString(String field) {
		StringBuilder sb = new StringBuilder();		
		sb.append("spanTermWithId(");
		sb.append(firstClause.toString(field));
		sb.append(")");		
		return sb.toString();
	}

}
