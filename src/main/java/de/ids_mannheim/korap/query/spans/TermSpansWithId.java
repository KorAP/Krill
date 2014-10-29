package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.TermSpans;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;

import de.ids_mannheim.korap.query.SpanTermWithIdQuery;

/** Enumeration of termSpans whose an id. This class just wraps the usual Lucene TermSpans, 
 * 	and adds spanid property. It reads the term-id from a term span payload. The term-id 
 * 	is encoded in a short, starting from (offset) 0 in the payload.
 * 
 * 	@author margaretha
 * */
public class TermSpansWithId extends SpansWithId{

	private TermSpans termSpans;

	public TermSpansWithId(SpanTermWithIdQuery spanTermWithIdQuery,
			AtomicReaderContext context, Bits acceptDocs,
			Map<Term, TermContext> termContexts) throws IOException {
		super(spanTermWithIdQuery, context, acceptDocs, termContexts);
		termSpans = (TermSpans) firstSpans;
		hasMoreSpans = termSpans.next();
	}

	@Override
	public boolean next() throws IOException {
		isStartEnumeration=false;
		return advance();
	}

	private boolean advance() throws IOException{
		while (hasMoreSpans){
			readPayload();
			matchDocNumber = firstSpans.doc();
			matchStartPosition = firstSpans.start();
			matchEndPosition = firstSpans.end();
			hasMoreSpans = firstSpans.next();
			return true;
		}
		return false;
	}
	
	private void readPayload() throws IOException{
		List<byte[]> payload = (List<byte[]>) firstSpans.getPayload();
		ByteBuffer bb = ByteBuffer.allocate(payload.get(0).length);
		bb.put(payload.get(0));
		setSpanId(bb.getShort(0)); //term id
	}
	

	@Override
	public boolean skipTo(int target) throws IOException {
		if (hasMoreSpans && (firstSpans.doc() < target)){
  			if (!firstSpans.skipTo(target)){
  				return false;
  			}
  		}		
		matchPayload.clear();
		isStartEnumeration=false;
		return advance();	
	}

	@Override
	public long cost() {
		return firstSpans.cost(); // plus cost from reading payload
	}

}
