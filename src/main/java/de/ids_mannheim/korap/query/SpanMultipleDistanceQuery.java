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

import de.ids_mannheim.korap.query.spans.MultipleDistanceSpans;

/** Match two spans with respect to a list of distance constraints.
 * 	No repetition of constraints of the same type is allowed. For example,
 * 	there must only exactly one constraint for word/token-based distance.
 * 	
 * 	@author margaretha
 * */
public class SpanMultipleDistanceQuery extends SimpleSpanQuery{
	
	private SpanQuery firstClause, secondClause;	 
	private List<DistanceConstraint> constraints;  
	private boolean isOrdered;
	private boolean collectPayloads;	
			
	public SpanMultipleDistanceQuery(SpanQuery firstClause, SpanQuery secondClause,
			List<DistanceConstraint> constraints, boolean isOrdered, 
			boolean collectPayloads) {
		super(firstClause, secondClause, "spanMultipleDistance");
		this.constraints = constraints;
    	this.firstClause=firstClause;
    	this.secondClause=secondClause;
		this.isOrdered = isOrdered;
		this.collectPayloads = collectPayloads;
	}

	@Override
	public SpanMultipleDistanceQuery clone() {
		SpanMultipleDistanceQuery query = new SpanMultipleDistanceQuery(
		    (SpanQuery) firstClause.clone(),
		    (SpanQuery) secondClause.clone(),
		    this.constraints,
		    this.isOrdered,
		    this.collectPayloads
        );		
		
		query.setBoost(getBoost());
		return query;
	}	
	
	/** Filter the span matches of each constraint, returning only the matches 
	 * 	meeting all the constraints.
	 * 	@return only the span matches meeting all the constraints.
	 * */
	@Override
	public Spans getSpans(AtomicReaderContext context, Bits acceptDocs,
			Map<Term, TermContext> termContexts) throws IOException {			
		
		DistanceConstraint c;		
		SpanDistanceQuery sdq,sdq2;		
		Spans ds,ds2;
		MultipleDistanceSpans mds = null;
		
		c = constraints.get(0);
		sdq = createSpanDistanceQuery(c);
		ds = sdq.getSpans(context, acceptDocs, termContexts);
				
		for (int i=1; i< constraints.size(); i++){
			sdq2 = createSpanDistanceQuery(constraints.get(i));
			ds2 = sdq2.getSpans(context, acceptDocs, termContexts);			
						
			mds = new MultipleDistanceSpans(this, context, acceptDocs, 
					termContexts, ds, ds2, isOrdered); 
			ds = mds;
		}
		
		return mds;
	}
	
	/** Create a SpanDistanceQuery based on the given constraint.
	 * 	@return a SpanDistanceQuery 
	 * */
	private SpanDistanceQuery createSpanDistanceQuery(DistanceConstraint c) {		
				
		if (c.getUnit().equals("w")){
			return new SpanDistanceQuery(firstClause, secondClause,
					c.getMinDistance(), c.getMaxDistance(),isOrdered, 
					collectPayloads);
		}
		
		return new SpanDistanceQuery(c.getElementQuery(), firstClause, 
				secondClause, c.getMinDistance(), c.getMaxDistance(),
				isOrdered, collectPayloads);
	}

	public List<DistanceConstraint> getConstraints() {
		return constraints;
	}

	public void setConstraints(List<DistanceConstraint> constraints) {
		this.constraints = constraints;
	}
	
}
