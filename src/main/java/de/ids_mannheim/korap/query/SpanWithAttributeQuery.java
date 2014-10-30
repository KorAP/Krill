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

import de.ids_mannheim.korap.query.spans.SpansWithAttribute;
import de.ids_mannheim.korap.query.spans.ElementSpans;
import de.ids_mannheim.korap.query.spans.RelationSpans;
import de.ids_mannheim.korap.query.spans.TermSpansWithId;

/**	Enumeration of spans (e.g. element or relation spans) having some 
 * 	specific attribute(s) or <em>not</em> having some attribute(s).
 * 
 * 	@author margaretha
 */
public class SpanWithAttributeQuery extends SpanWithIdQuery{
		
	private boolean isMultipleAttributes;
	private String type;
	
	public SpanWithAttributeQuery(SpanWithIdQuery firstClause,
			SpanAttributeQuery secondClause, boolean collectPayloads) {
		super(firstClause, secondClause, collectPayloads);
		setType();
	}
	
	public SpanWithAttributeQuery(SpanWithIdQuery firstClause,
			List<SpanQuery> secondClauses, boolean collectPayloads) {
		super(firstClause, secondClauses, collectPayloads);
		isMultipleAttributes = true;
		setType();
	}	
	
	public String getType() {
		return type;
	}

	public void setType() {
		if (SpanElementQuery.class.isInstance(firstClause)){
			type = "spanElementWithAttribute"; 
		}
		else if (SpanRelationQuery.class.isInstance(firstClause)){
			type = "spanRelationWithAttribute";	
		}
		else if (SpanTermWithIdQuery.class.isInstance(firstClause)){
			type = "spanTermWithAttribute";	
		}
	}

	@Override
	public SimpleSpanQuery clone() {
		SpanWithAttributeQuery sq;
		if (!isMultipleAttributes){			
			sq = new SpanWithAttributeQuery( 
					(SpanWithIdQuery) firstClause.clone(), 
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
			sq = new SpanWithAttributeQuery( 
					(SpanWithIdQuery) firstClause.clone(), 
					clauseList, 
					collectPayloads);
		}
		return sq;
	}

	@Override
	public Spans getSpans(AtomicReaderContext context, Bits acceptDocs,
			Map<Term, TermContext> termContexts) throws IOException {
		
		Spans spans = this.getFirstClause().getSpans(context, acceptDocs, termContexts);
		
		if (type.equals("spanElementWithAttribute")){			
			return new SpansWithAttribute(this, (ElementSpans) spans, 
					context, acceptDocs, termContexts);
		}
		else if (type.equals("spanRelationWithAttribute")){
			return new SpansWithAttribute(this, (RelationSpans) spans,
					context, acceptDocs, termContexts);
		}
		
		return new SpansWithAttribute(this, (TermSpansWithId) spans,
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
