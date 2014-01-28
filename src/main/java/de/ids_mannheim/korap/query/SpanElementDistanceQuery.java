package de.ids_mannheim.korap.query;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.spans.ElementDistanceSpan;

/** Match two ordered Spans with minimum and maximum distance constraints.
 * 	In this query, the distance unit is the difference between two element 
 * 	positions, where the elements can be sentence or paragraph elements.
 * 
 * 	@author margaretha
 * */
public class SpanElementDistanceQuery extends SpanDistanceQuery {
	
	private SpanQuery elementQuery;
	
	public SpanElementDistanceQuery(SpanQuery elementQuery, 
			SpanQuery firstClause, SpanQuery secondClause, 
			int minDistance, int maxDistance, 
			boolean collectPayloads) {
		super(firstClause, secondClause, minDistance, maxDistance, collectPayloads);
		this.elementQuery = elementQuery;    	    	
	}
	
	@Override
	public SpanElementDistanceQuery clone() {
		SpanElementDistanceQuery query = new SpanElementDistanceQuery(
			(SpanQuery) elementQuery.clone(),
		    (SpanQuery) firstClause.clone(),
		    (SpanQuery) secondClause.clone(),			    
		    this.minDistance,
		    this.maxDistance,
		    this.collectPayloads
        );
		query.setBoost(getBoost());
		return query;	
	}

	@Override
	public Spans getSpans(AtomicReaderContext context, Bits acceptDocs,
			Map<Term, TermContext> termContexts) throws IOException {		
		return new ElementDistanceSpan(this,context, acceptDocs,termContexts);
	}

	public SpanQuery getElementQuery() {
		return elementQuery;
	}

	public void setElementQuery(SpanQuery elementQuery) {
		this.elementQuery = elementQuery;
	}
}
