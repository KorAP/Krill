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
import de.ids_mannheim.korap.query.spans.ElementSpans;
import de.ids_mannheim.korap.query.spans.RelationSpans;

/**	Enumeration of spans (e.g. element or relation spans) having some specific attribute(s) or 
 * 	<em>not</em> having some attribute(s).
 * 
 * 	@author margaretha
 */
public class SpanWithAttributeQuery extends SimpleSpanQuery{
		
	private boolean isMultipleAttributes;
	private String type;
	
	public SpanWithAttributeQuery(SpanElementQuery firstClause,
			SpanAttributeQuery secondClause, boolean collectPayloads) {
		super(firstClause, secondClause, collectPayloads);
		type = "spanElementWithAttribute"; 
	}
	
	public SpanWithAttributeQuery(SpanElementQuery firstClause,
			List<SpanQuery> secondClauses, boolean collectPayloads) {
		super(firstClause, secondClauses, collectPayloads);
		isMultipleAttributes = true;
		type = "spanElementWithAttribute";
	}
	
	public SpanWithAttributeQuery(SpanRelationQuery firstClause,
			SpanAttributeQuery secondClause, boolean collectPayloads) {
		super(firstClause, secondClause, collectPayloads);
		type = "spanRelationWithAttribute";		
	}
	
	public SpanWithAttributeQuery(SpanRelationQuery firstClause,
			List<SpanQuery> secondClauses, boolean collectPayloads) {
		super(firstClause, secondClauses, collectPayloads);
		isMultipleAttributes = true;
		type = "spanRelationWithAttribute";		
	}

	@Override
	public SimpleSpanQuery clone() {
		SpanWithAttributeQuery sq;
		if (!isMultipleAttributes){			
			if (type.equals("spanElementWithAttribute")){			
				sq = new SpanWithAttributeQuery( 
						(SpanElementQuery) firstClause.clone(), 
						(SpanAttributeQuery) secondClause.clone(), 
						collectPayloads);
			}
			else {
				sq = new SpanWithAttributeQuery( 
						(SpanRelationQuery) firstClause.clone(), 
						(SpanAttributeQuery) secondClause.clone(), 
						collectPayloads);
			}
		}
		else {
			List<SpanQuery> clauseList = new ArrayList<SpanQuery>();
			SpanAttributeQuery saq;
			for (SpanQuery q : this.clauseList ){
				saq = (SpanAttributeQuery) q;
				clauseList.add(saq.clone());
			}
			
			if (type.equals("spanElementWithAttribute")){			
				sq = new SpanWithAttributeQuery( 
						(SpanElementQuery) firstClause.clone(), 
						clauseList, 
						collectPayloads);
			}
			else {
				sq = new SpanWithAttributeQuery( 
						(SpanRelationQuery) firstClause.clone(), 
						clauseList, 
						collectPayloads);
			}
		}
		return sq;
	}

	@Override
	public Spans getSpans(AtomicReaderContext context, Bits acceptDocs,
			Map<Term, TermContext> termContexts) throws IOException {
		
		Spans spans = this.getFirstClause().getSpans(context, acceptDocs, termContexts);
		
		if (type.equals("spanElementWithAttribute")){			
			return new ElementAttributeSpans(this, (ElementSpans) spans, 
					context, acceptDocs, termContexts);
		}
		
		return new ElementAttributeSpans(this, (RelationSpans) spans,
				context, acceptDocs, termContexts);
	}

	@Override
	public String toString(String field) {
		
		StringBuilder sb = new StringBuilder();
		sb.append(type);
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
