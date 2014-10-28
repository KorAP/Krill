package de.ids_mannheim.korap.query;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.ToStringUtils;

import de.ids_mannheim.korap.query.spans.RelationSpansWithVariable;

/** This query match one side of a relation (either left or right) to certain 
 * 	elements or terms, and return the other side of the relation.	
 * 
 * 	@author margaretha
 * */
public class SpanRelationWithVariableQuery extends SpanWithIdQuery{
	
	private static String elementStr = "s"; // default element interval type
	
	private SpanElementQuery elementQuery;
	private boolean matchRight;  // if false, match left
	private int window;
	
	public SpanRelationWithVariableQuery(SpanRelationQuery spanRelationQuery,
			SpanWithIdQuery secondClause, // match tokenWithIdQuery, ElementQuery, RelationQuery
			boolean matchRight,
			boolean collectPayloads) {
		this(spanRelationQuery, secondClause, elementStr, matchRight, collectPayloads);		
	}
	
	public SpanRelationWithVariableQuery(SpanRelationQuery spanRelationQuery,
			SpanWithIdQuery secondClause, 
			String elementStr,
			boolean matchRight,
			boolean collectPayloads) {
		super(spanRelationQuery, secondClause, collectPayloads);
		this.matchRight = matchRight;
		elementQuery = new SpanElementQuery(spanRelationQuery.getField(), elementStr);		
	}
	
	public SpanRelationWithVariableQuery(SpanRelationQuery spanRelationQuery,
			SpanWithIdQuery secondClause, // match tokenWithIdQuery, ElementQuery, RelationQuery
			int window,
			boolean matchRight,
			boolean collectPayloads) {
		super(spanRelationQuery, secondClause, collectPayloads);
		this.matchRight = matchRight;
		this.window = window;
	}
	
	@Override
	public Spans getSpans(AtomicReaderContext context, Bits acceptDocs,
			Map<Term, TermContext> termContexts) throws IOException {
			return new RelationSpansWithVariable(this, context, acceptDocs, termContexts);
	}
	
	@Override
	public SimpleSpanQuery clone() {
		SpanRelationWithVariableQuery sq = new SpanRelationWithVariableQuery(
				(SpanRelationQuery) this.firstClause, 
				(SpanWithIdQuery) this.secondClause,
				this.elementQuery.getElementStr(),
				this.matchRight, 
				this.collectPayloads
		);		
		return sq;
	}

	@Override
	public String toString(String field) {
		StringBuilder sb = new StringBuilder();		
		sb.append("spanRelationWithVariable(");
		sb.append(firstClause.toString(field));
		sb.append(",");
		sb.append(secondClause.toString(field));
		sb.append(",");
		sb.append( matchRight ? "matchRight, " : "matchLeft, " );
		sb.append(",");
		if (elementQuery != null){
			sb.append("element:");
			sb.append(elementQuery.getElementStr());
		}
		else {
			sb.append("window:");
			sb.append(this.window);
		}
		sb.append(")");
		sb.append(ToStringUtils.boost(getBoost()));
		return sb.toString();
	}

	public boolean isMatchRight() {
		return matchRight;
	}

	public void setMatchRight(boolean matchRight) {
		this.matchRight = matchRight;
	}

	public SpanElementQuery getElementQuery() {
		return elementQuery;
	}

	public void setElementQuery(SpanElementQuery root) {
		this.elementQuery = root;
	}

	public int getWindow() {
		return window;
	}

	public void setWindow(int window) {
		this.window = window;
	}
}
