package de.ids_mannheim.korap.query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.spans.ElementAttributeSpans;

/** Span enumerations of elements having some specific attribute(s) or 
 * 	<em>not</em> having some attribute(s).
 * 
 * 	@author margaretha
 * */
public class SpanElementAttributeQuery extends SimpleSpanQuery{
	
	boolean isMultipleAttributes;
	
	public SpanElementAttributeQuery(SpanElementQuery firstClause,
			SpanAttributeQuery secondClause, boolean collectPayloads) {
		super(firstClause, secondClause, collectPayloads);
	}
	
	public SpanElementAttributeQuery(SpanElementQuery firstClause,
			List<SpanQuery> secondClauses, boolean collectPayloads) {
		super(firstClause, secondClauses, collectPayloads);
		isMultipleAttributes = true;
	}

	@Override
	public SimpleSpanQuery clone() {
		SpanElementAttributeQuery sq;
		if (!isMultipleAttributes){
			sq = new SpanElementAttributeQuery( 
					(SpanElementQuery) firstClause.clone(), 
					(SpanAttributeQuery) secondClause.clone(), 
					collectPayloads);
		}
		else {
			List<SpanQuery> clauseList = new ArrayList<SpanQuery>();
			SpanAttributeQuery saq;
			for (SpanQuery q : this.clauseList ){
				saq = (SpanAttributeQuery) q;
				clauseList.add(saq.clone());
			}
			
			sq = new SpanElementAttributeQuery(
					(SpanElementQuery) firstClause.clone(), 
					clauseList, 
					collectPayloads);
		}
		return sq;
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
		if (isMultipleAttributes){
			sb.append("[");
			
			SpanQuery sq;
			for (int i=0; i < clauseList.size(); i++){
				sq = clauseList.get(i); 
				sb.append(sq.toString(field));
				
				if (i < clauseList.size() -1)
					sb.append(", ");
			}
			
			sb.append("]");
		}
		else {
			sb.append(secondClause.toString(field));
		}
		sb.append(")");
		return sb.toString();
	}
}
