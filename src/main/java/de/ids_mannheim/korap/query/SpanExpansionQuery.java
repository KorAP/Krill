package de.ids_mannheim.korap.query;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.spans.ExpandedSpans;

/** Query to make a span longer by stretching out the start or 
 * 	the end position of the span.
 * 
 * 	@author margaretha
 * */
public class SpanExpansionQuery extends SimpleSpanQuery{

	int min, max; 
	byte classNumber;
	boolean isBefore;
	
	public SpanExpansionQuery(SpanQuery firstClause, int min, int max,  
			boolean isBefore, boolean collectPayloads) {		
		super(firstClause, collectPayloads);
		this.min = min;
		this.max = max;
		this.isBefore = isBefore;
	}
	
	public SpanExpansionQuery(SpanQuery firstClause, int min, int max, 
			byte classNumber, boolean isBefore, boolean collectPayloads) {		
		this(firstClause, min,max,isBefore,collectPayloads);
		this.classNumber = classNumber;
	}

	@Override
	public SimpleSpanQuery clone() {
		SpanExpansionQuery sq = new SpanExpansionQuery(
				firstClause, 
				min, 
				max, 
				isBefore,
				collectPayloads);
		//sq.setBoost(sq.getBoost());
		return sq;
	}

	@Override
	public Spans getSpans(AtomicReaderContext context, Bits acceptDocs,
			Map<Term, TermContext> termContexts) throws IOException {
		return new ExpandedSpans(this, context, acceptDocs, termContexts);
	}

	@Override
	public String toString(String field) {
		StringBuilder sb = new StringBuilder();
		sb.append("spanExpansion(");
		sb.append(firstClause.toString());
		sb.append(",[");
		sb.append(min);
		sb.append(",");
		sb.append(max);
		sb.append("],");		
		if (isBefore)
			sb.append("left)");
		else sb.append("right)");
		return sb.toString();
	}

	public int getMin() {
		return min;
	}

	public void setMin(int min) {
		this.min = min;
	}

	public int getMax() {
		return max;
	}

	public void setMax(int max) {
		this.max = max;
	}

	public boolean isBefore() {
		return isBefore;
	}

	public void setBefore(boolean isBefore) {
		this.isBefore = isBefore;
	}

	public byte getClassNumber() {
		return classNumber;
	}

	public void setClassNumber(byte classNumber) {
		this.classNumber = classNumber;
	}
}
