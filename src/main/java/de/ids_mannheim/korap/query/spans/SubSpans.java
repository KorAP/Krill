package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.SpanSubspanQuery;

public class SubSpans extends SimpleSpans{

	private int startOffset, length;
	
	public SubSpans(SpanSubspanQuery subspanQuery,
			AtomicReaderContext context, Bits acceptDocs,
			Map<Term, TermContext> termContexts) throws IOException {
		super(subspanQuery, context, acceptDocs, termContexts);
		this.startOffset= subspanQuery.getStartOffset();
		this.length = subspanQuery.getLength();
		hasMoreSpans = firstSpans.next();
	}

	@Override
	public boolean next() throws IOException {
		isStartEnumeration=false;		
		return advance();
	}

	private boolean advance() throws IOException {
		while (hasMoreSpans){
			setMatch();
			hasMoreSpans = firstSpans.next();
			return true;
		}
		return false;
	}
	
	public void setMatch() throws IOException {
		if (this.startOffset < 0)
			matchStartPosition = firstSpans.end() + startOffset;
		else matchStartPosition = firstSpans.start() + startOffset;
		
		matchEndPosition = matchStartPosition + this.length;
		matchPayload = firstSpans.getPayload();
		matchDocNumber = firstSpans.doc();
	}
	
	@Override
	public boolean skipTo(int target) throws IOException {
		if (hasMoreSpans && (firstSpans.doc() < target)){
  			if (!firstSpans.skipTo(target)){
  				hasMoreSpans = false;
  				return false;
  			}
  		} 		
  		matchPayload.clear();
  		return advance();
	}

	@Override
	public long cost() {		
		return firstSpans.cost() + 1;
	}

}
