package de.ids_mannheim.korap.query;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.ToStringUtils;

import de.ids_mannheim.korap.query.spans.SegmentSpans;

/** Matching two spans having exactly the same start and end positions.
 * @author margaretha 
 * */
public class SpanSegmentQuery extends SimpleSpanQuery{
	
	private String spanName;
	
	public SpanSegmentQuery(SpanQuery firstClause, SpanQuery secondClause) {
		this(firstClause,secondClause,true);
	}
	
	public SpanSegmentQuery(SpanQuery firstClause, SpanQuery secondClause, 
			boolean collectPayloads) { 
    	super(firstClause,secondClause,collectPayloads);
    	spanName = "spanSegment";
	}
	
	@Override
	public Spans getSpans(AtomicReaderContext context, Bits acceptDocs, 
			Map<Term, TermContext> termContexts) throws IOException {
		return (Spans) new SegmentSpans(this, context, acceptDocs,
				termContexts);
	}		
	
	@Override
	public SpanSegmentQuery clone() {
		SpanSegmentQuery spanSegmentQuery = new SpanSegmentQuery(
			    (SpanQuery) firstClause.clone(),
			    (SpanQuery) secondClause.clone(),
			    collectPayloads
		        );
		spanSegmentQuery.setBoost(getBoost());
		return spanSegmentQuery;		
	}
	
    @Override
	public String toString(String field) {
		StringBuilder sb = new StringBuilder();
		sb.append(this.spanName);
		sb.append("(");
		sb.append(firstClause.toString(field));
	        sb.append(", ");
		sb.append(secondClause.toString(field));
		sb.append(")");
		sb.append(ToStringUtils.boost(getBoost()));
		return sb.toString();	
    }
	
	//TODO: Where is the hashmap?
		
    @Override
    public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof SpanSegmentQuery)) return false;
		
		SpanSegmentQuery spanSegmentQuery = (SpanSegmentQuery) o;
		
		if (collectPayloads != spanSegmentQuery.collectPayloads) return false;
		if (!firstClause.equals(spanSegmentQuery.firstClause)) return false;
		if (!secondClause.equals(spanSegmentQuery.secondClause)) return false;
	
		return getBoost() == spanSegmentQuery.getBoost();
    };

    @Override
    public int hashCode() {
		int result;
		result = firstClause.hashCode() + secondClause.hashCode();
		result ^= (31 * result) + (result >>> 3);
		result += Float.floatToRawIntBits(getBoost());
		return result;
    };

}
