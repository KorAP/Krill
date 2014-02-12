package de.ids_mannheim.korap.query;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.spans.ElementDistanceSpans;
import de.ids_mannheim.korap.query.spans.TokenDistanceSpans;
import de.ids_mannheim.korap.query.spans.UnorderedElementDistanceSpans;
import de.ids_mannheim.korap.query.spans.UnorderedTokenDistanceSpans;

/** Match two ordered or unordered Spans with minimum and maximum 
 * 	distance constraints. The distance unit can be word (token), 
 * 	sentence or paragraph. 
 * 
 * 	@author margaretha
 * */
public class SpanDistanceQuery extends SimpleSpanQuery {
	
	public boolean isOrdered;
	protected int minDistance, maxDistance;	
	private SpanQuery elementQuery; // element distance unit

	public SpanDistanceQuery(SpanQuery firstClause, SpanQuery secondClause, 
			int minDistance, int maxDistance, boolean isOrdered, 
			boolean collectPayloads) {		
		super(firstClause, secondClause, "spanDistance",collectPayloads); 
    	this.minDistance =minDistance;
		this.maxDistance = maxDistance;
		this.isOrdered = isOrdered;
	}
	
	public SpanDistanceQuery(SpanQuery elementQuery, SpanQuery firstClause, 
			SpanQuery secondClause, int minDistance, int maxDistance, 
			boolean isOrdered, boolean collectPayloads) {
		super(firstClause, secondClause, "spanElementDistance",collectPayloads);
    	this.minDistance =minDistance;
		this.maxDistance = maxDistance;
		this.isOrdered = isOrdered;
		this.elementQuery = elementQuery;
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
		
		spanDistanceQuery.setBoost(getBoost());
		return spanDistanceQuery;	
	}

	@Override
	public Spans getSpans(AtomicReaderContext context, Bits acceptDocs,
			Map<Term, TermContext> termContexts) throws IOException {
		
		if (isOrdered){
			if (this.elementQuery != null) {
				return new ElementDistanceSpans(this, context, acceptDocs, termContexts);		
			}
			return new TokenDistanceSpans(this, context, acceptDocs, termContexts);
		}
		else if (this.elementQuery != null) {
			return new UnorderedElementDistanceSpans(this, context, acceptDocs, termContexts);		
		}
		return new UnorderedTokenDistanceSpans(this, context, acceptDocs, termContexts);
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
	
	public SpanQuery getElementQuery() {
		return elementQuery;
	}
	
	public void setElementQuery(SpanQuery elementQuery) {
		this.elementQuery = elementQuery;
	}

}
