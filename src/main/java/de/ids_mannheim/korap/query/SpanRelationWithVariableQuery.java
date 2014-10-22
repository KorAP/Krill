package de.ids_mannheim.korap.query;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.spans.RelationSpansWithVariable;

/**	This query returns the tokens/elements of the left-side of a relation
 * 	whose right-side tokens/elements' positions match the spans of another
 * 	spanquery. 
 * 
 * */
public class SpanRelationWithVariableQuery extends SpanRelationQuery{

	private String rootElementStr = "s";
	private SpanElementQuery root;
	private boolean matchRight;  // if false, match left
	
	public SpanRelationWithVariableQuery(SpanRelationQuery spanRelationQuery,
			SpanQuery secondClause, // span to match
			boolean matchRight,
			boolean collectPayloads) {
		super(spanRelationQuery, secondClause, collectPayloads);
		this.matchRight = matchRight;
		root = new SpanElementQuery(spanRelationQuery.getField(), rootElementStr);
	}	
	
	@Override
	public Spans getSpans(AtomicReaderContext context, Bits acceptDocs,
			Map<Term, TermContext> termContexts) throws IOException {
			return new RelationSpansWithVariable(this, context, acceptDocs, termContexts);
	}
	
	@Override
	public SimpleSpanQuery clone() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toString(String field) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isMatchRight() {
		return matchRight;
	}

	public void setMatchRight(boolean matchRight) {
		this.matchRight = matchRight;
	}

	public SpanElementQuery getRoot() {
		return root;
	}

	public void setRoot(SpanElementQuery root) {
		this.root = root;
	}

}
