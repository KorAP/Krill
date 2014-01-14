package de.ids_mannheim.korap.query;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.spans.SegmentSpans;

public class SpanSegmentQuery extends SimpleSpanQuery{
	
	private boolean collectPayloads;
	private SpanQuery firstClause, secondClause;
	
	public SpanSegmentQuery(SpanQuery firstClause, SpanQuery secondClause) {
		this(firstClause,secondClause,true);
	}
	
	public SpanSegmentQuery(SpanQuery firstClause, SpanQuery secondClause, 
			boolean collectPayloads) { 
    	super(firstClause,secondClause,"spanSegment");
    	this.collectPayloads = collectPayloads;
    	this.firstClause=firstClause;
    	this.secondClause=secondClause;
	}
	
	@Override
	public Spans getSpans(AtomicReaderContext context, Bits acceptDocs, 
			Map<Term, TermContext> termContexts) throws IOException {
		return (Spans) new SegmentSpans(this, context, acceptDocs,
				termContexts, collectPayloads);
	}

	@Override
	public SimpleSpanQuery clone() {
		SpanSegmentQuery spanSegmentQuery = new SpanSegmentQuery(
			    (SpanQuery) firstClause.clone(),
			    (SpanQuery) secondClause.clone(),
			    this.collectPayloads
		        );
		spanSegmentQuery.setBoost(getBoost());
		return spanSegmentQuery;		
	}
	
	/* TODO: Where is the hashmap?
		
    @Override
    public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof SpanNextQuery)) return false;
		
		final SpanNextQuery spanNextQuery = (SpanNextQuery) o;
		
		if (collectPayloads != spanNextQuery.collectPayloads) return false;
		if (!firstClause.equals(spanNextQuery.firstClause)) return false;
		if (!secondClause.equals(spanNextQuery.secondClause)) return false;
	
		return getBoost() == spanNextQuery.getBoost();
    };


    // I don't know what I am doing here
    @Override
    public int hashCode() {
		int result;
		result = firstClause.hashCode() + secondClause.hashCode();
		result ^= (result << 31) | (result >>> 2);  // reversible
		result += Float.floatToRawIntBits(getBoost());
		return result;
    };
    */
}
