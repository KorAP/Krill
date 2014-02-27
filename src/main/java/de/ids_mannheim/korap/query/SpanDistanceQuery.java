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

import de.ids_mannheim.korap.query.spans.DistanceExclusionSpan;
import de.ids_mannheim.korap.query.spans.ElementDistanceExclusionSpan;
import de.ids_mannheim.korap.query.spans.ElementDistanceSpans;
import de.ids_mannheim.korap.query.spans.TokenDistanceSpans;
import de.ids_mannheim.korap.query.spans.UnorderedElementDistanceSpans;
import de.ids_mannheim.korap.query.spans.UnorderedTokenDistanceSpans;

/** Match two ordered or unordered Spans with minimum and maximum 
 * 	distance constraints. The distance unit can be word (token), 
 * 	sentence or paragraph. The distance constraint can also be
 * 	specified to match some Spans which do not co-occur with some 
 * 	other Spans within a min and max distance. 
 * 
 * 	@author margaretha
 * */
public class SpanDistanceQuery extends SimpleSpanQuery {
	
	private boolean exclusion;
	private boolean isOrdered;	
	private int minDistance, maxDistance;	
	private SpanElementQuery elementQuery; // element distance unit
	private String distanceUnit;
	private String spanName;

	public SpanDistanceQuery(SpanQuery firstClause, SpanQuery secondClause, 
			int minDistance, int maxDistance, boolean isOrdered, 
			boolean collectPayloads) {		
		super(firstClause, secondClause, collectPayloads); 
		init(minDistance, maxDistance, isOrdered);
		distanceUnit = "w";
		spanName = "spanDistance";
	}
	
	public SpanDistanceQuery(SpanElementQuery elementQuery, SpanQuery firstClause, 
			SpanQuery secondClause, int minDistance, int maxDistance, 
			boolean isOrdered, boolean collectPayloads) {
		super(firstClause, secondClause, collectPayloads);
    	init(minDistance, maxDistance, isOrdered);
		this.elementQuery = elementQuery;
		distanceUnit = elementQuery.getElementStr();
		spanName = "spanElementDistance";
	}
	
	private void init(int minDistance, int maxDistance,boolean isOrdered){
		this.minDistance = minDistance;
		this.maxDistance = maxDistance;
		this.isOrdered = isOrdered;
		this.exclusion = false;
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
		sb.append("[(");
		sb.append(distanceUnit);
		sb.append("[");
		sb.append(minDistance);
		sb.append(":");
		sb.append(maxDistance);
		sb.append("], ");		
		sb.append( isOrdered ? "ordered, " : "notOrdered, " );
		sb.append( exclusion ? "excluded)]" : "notExcluded)])");
		sb.append(ToStringUtils.boost(getBoost()));
    	return sb.toString();
    }
	
	@Override
	public SpanDistanceQuery clone() {
		SpanDistanceQuery spanDistanceQuery = new SpanDistanceQuery(
		    (SpanQuery) firstClause.clone(),
		    (SpanQuery) secondClause.clone(),
		    this.minDistance,
		    this.maxDistance,
		    this.isOrdered,
		    this.collectPayloads
        );
				
		if (this.elementQuery != null) {
			spanDistanceQuery.setElementQuery(this.elementQuery);
		}
		spanDistanceQuery.setExclusion(this.exclusion);
		spanDistanceQuery.setBoost(getBoost());
		return spanDistanceQuery;	
	}

	@Override
	public Spans getSpans(AtomicReaderContext context, Bits acceptDocs,
			Map<Term, TermContext> termContexts) throws IOException {		
		
		if (this.elementQuery != null) {
			if (isExclusion()){
				return new ElementDistanceExclusionSpan(this, context, acceptDocs, 
						termContexts, isOrdered);
			}			
			else if (isOrdered){
				return new ElementDistanceSpans(this, context, acceptDocs, 
						termContexts);
			}
			return new UnorderedElementDistanceSpans(this, context, acceptDocs, 
					termContexts);
			
		}
		else if (isExclusion()){
			return new DistanceExclusionSpan(this, context, acceptDocs, 
					termContexts, isOrdered);
		}
		else if (isOrdered) {
			return new TokenDistanceSpans(this, context, acceptDocs, 
					termContexts);		
		}
		return new UnorderedTokenDistanceSpans(this, context, acceptDocs, 
				termContexts);
	}

	public int getMinDistance() {
		return minDistance;
	}

	public void setMinDistance(int minDistance) {
		this.minDistance = minDistance;
	}

	public int getMaxDistance() {
		return maxDistance;
	}

	public void setMaxDistance(int maxDistance) {
		this.maxDistance = maxDistance;
	}
	
	public SpanElementQuery getElementQuery() {
		return elementQuery;
	}
	
	public void setElementQuery(SpanElementQuery elementQuery) {
		this.elementQuery = elementQuery;
	}

	public boolean isExclusion() {
		return exclusion;
	}

	public void setExclusion(boolean exclusion) {
		this.exclusion = exclusion;
	}


}
