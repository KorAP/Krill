package de.ids_mannheim.korap.query;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.ToStringUtils;

import de.ids_mannheim.korap.query.spans.MultipleDistanceSpans;

/** Match two spans with respect to a list of distance constraints.
 * 	No repetition of constraints of the same type is allowed. For example,
 * 	there must only exactly one constraint for word/token-based distance.
 * 	
 * 	Warning: Exclusion constraint is not suitable yet!
 * 	@author margaretha
 * */
public class SpanMultipleDistanceQuery extends SimpleSpanQuery{
	
	private List<DistanceConstraint> constraints;  
	private boolean isOrdered;
	private String spanName;
			
	public SpanMultipleDistanceQuery(SpanQuery firstClause, SpanQuery secondClause,
			List<DistanceConstraint> constraints, boolean isOrdered, 
			boolean collectPayloads) {
		super(firstClause, secondClause, collectPayloads);
		this.constraints = constraints;
		this.isOrdered = isOrdered;
		spanName = "spanMultipleDistance";
	}

	@Override
	public SpanMultipleDistanceQuery clone() {
		SpanMultipleDistanceQuery query = new SpanMultipleDistanceQuery(
		    (SpanQuery) firstClause.clone(),
		    (SpanQuery) secondClause.clone(),
		    this.constraints,
		    this.isOrdered,
		    collectPayloads
        );		
		
		query.setBoost(getBoost());
		return query;
	}
	
	@Override
	public String toString(String field) {
		StringBuilder sb = new StringBuilder();
		sb.append(this.spanName);
		sb.append("(");
		sb.append(firstClause.toString(field));
	    sb.append(", ");
		sb.append(secondClause.toString(field));
		sb.append(", ");
		sb.append("[");
		
		DistanceConstraint c;
		int size = constraints.size();
		for (int i=0; i < size; i++){
			c = constraints.get(i);
			sb.append("(");
			sb.append(c.getUnit());
			sb.append("[");
			sb.append(c.getMinDistance());
			sb.append(":");
			sb.append(c.getMaxDistance());
			sb.append("], ");		
			sb.append(c.isOrdered() ? "ordered, " : "notOrdered, ");
			sb.append(c.isExclusion() ? "excluded)]" : "notExcluded)");			
			if (i < size-1) sb.append(", ");
		}		
		sb.append("])");
		sb.append(ToStringUtils.boost(getBoost()));
    	return sb.toString();
    }
	
	/** Filter the span matches of each constraint, returning only the matches 
	 * 	meeting all the constraints.
	 * 	@return only the span matches meeting all the constraints.
	 * */
	@Override
	public Spans getSpans(AtomicReaderContext context, Bits acceptDocs,
			Map<Term, TermContext> termContexts) throws IOException {			
		
		SpanDistanceQuery sdq,sdq2;		
		Spans ds,ds2;
		MultipleDistanceSpans mds = null;
		boolean exclusion;
				
		sdq = new SpanDistanceQuery(firstClause, secondClause, constraints.get(0), collectPayloads);
		ds = sdq.getSpans(context, acceptDocs, termContexts);
				
		for (int i=1; i< constraints.size(); i++){
			sdq2 = new SpanDistanceQuery(firstClause, secondClause, constraints.get(i), 
					collectPayloads);
			ds2 = sdq2.getSpans(context, acceptDocs, termContexts);			
			
			exclusion = sdq.isExclusion() && sdq2.isExclusion();
			mds = new MultipleDistanceSpans(this, context, acceptDocs, 
					termContexts, ds, ds2, isOrdered, exclusion); 
			ds = mds;
		}
		
		return mds;
	}	

	public List<DistanceConstraint> getConstraints() {
		return constraints;
	}

	public void setConstraints(List<DistanceConstraint> constraints) {
		this.constraints = constraints;
	}
	
}
