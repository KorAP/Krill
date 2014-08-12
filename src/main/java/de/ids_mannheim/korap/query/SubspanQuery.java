package de.ids_mannheim.korap.query;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.spans.SubSpans;

/**	This query extracts a subspan from a span. The subspan starts from 
 * 	a startOffset until startOffset + length. A positive startOffset 
 * 	is counted from the start of the span, while a negative startOffset 
 * 	is counted from the end of the span. 
 * 
 * 	@author margaretha
 * */
public class SubspanQuery extends SimpleSpanQuery{
	
	private int startOffset, length;
			
	public SubspanQuery(SpanQuery firstClause, int startOffset, int length, 
			boolean collectPayloads) {
		super(firstClause, collectPayloads);
		this.startOffset = startOffset;
		this.length = length;
	}

	@Override
	public SimpleSpanQuery clone() {
		SubspanQuery sq = new SubspanQuery(
				this.getFirstClause(), 
				this.startOffset,
				this.length, 
				this.collectPayloads);
		sq.setBoost(this.getBoost());
		return sq;
	}

	@Override
	public Spans getSpans(AtomicReaderContext context, Bits acceptDocs,
			Map<Term, TermContext> termContexts) throws IOException {
		return new SubSpans(this, context, acceptDocs, termContexts);
	}

	@Override
	public String toString(String field) {		
		StringBuilder sb = new StringBuilder();
		sb.append("subspan(");
		sb.append(this.startOffset);
		sb.append(",");
		sb.append(this.length);
		sb.append(")");
		return sb.toString();
	}

	public int getStartOffset() {
		return startOffset;
	}

	public void setStartOffset(int startOffset) {
		this.startOffset = startOffset;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}
}
