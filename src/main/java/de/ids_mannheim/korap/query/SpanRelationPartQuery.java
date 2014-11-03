package de.ids_mannheim.korap.query;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.ToStringUtils;

import de.ids_mannheim.korap.query.spans.RelationPartSpans;

/** This query match one part of a relation (either left or right) to certain 
 * 	elements or terms, and return the other part of the relation.	
 * 
 * 	@author margaretha
 * */
public class SpanRelationPartQuery extends SpanRelationQuery{
	
	private static String elementStr = "s"; // default element interval type
	
	private SpanElementQuery elementQuery;
	private boolean matchRight;  // if false, match left
	private boolean inverseRelation;  // if false, sort result by the left
	private int window;	
	
	public SpanRelationPartQuery(SpanRelationQuery spanRelationQuery,
			SpanWithIdQuery secondClause, // match tokenWithIdQuery, ElementQuery, RelationQuery
			boolean matchRight,
			boolean inverseRelation,
			boolean collectPayloads) {
		this(spanRelationQuery, secondClause, elementStr, matchRight, inverseRelation, collectPayloads);		
	}
	
	public SpanRelationPartQuery(SpanRelationQuery spanRelationQuery,
			SpanWithIdQuery secondClause, 
			String elementStr,
			boolean matchRight,
			boolean inverseRelation,
			boolean collectPayloads) {
		super(spanRelationQuery, secondClause, collectPayloads);
		this.matchRight = matchRight;
		this.inverseRelation = inverseRelation;
		elementQuery = new SpanElementQuery(spanRelationQuery.getField(), elementStr);		
	}
	
	public SpanRelationPartQuery(SpanRelationQuery spanRelationQuery,
			SpanWithIdQuery secondClause, // match tokenWithIdQuery, ElementQuery, RelationQuery
			int window,
			boolean matchRight,
			boolean inverseRelation,
			boolean collectPayloads) {
		super(spanRelationQuery, secondClause, collectPayloads);
		this.matchRight = matchRight;
		this.inverseRelation = inverseRelation;
		this.window = window;
	}
	
	@Override
	public Spans getSpans(AtomicReaderContext context, Bits acceptDocs,
			Map<Term, TermContext> termContexts) throws IOException {
			return new RelationPartSpans(this, context, acceptDocs, termContexts);
	}
	
	@Override
	public SimpleSpanQuery clone() {
		SpanRelationPartQuery sq = new SpanRelationPartQuery(
				(SpanRelationQuery) this.firstClause, 
				(SpanWithIdQuery) this.secondClause,
				this.elementQuery.getElementStr(),
				this.matchRight, 
				this.inverseRelation,
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

	public boolean isInverseRelation() {
		return inverseRelation;
	}

	public void setInverseRelation(boolean inverseRelation) {
		this.inverseRelation = inverseRelation;
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
