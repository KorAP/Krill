package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.search.spans.TermSpans;
import org.apache.lucene.util.Bits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ids_mannheim.korap.query.SpanElementQuery;

/**  
 * @author Nils Diewald, margaretha
 *
 * Use copyFrom instead of clone
 */
public class ElementSpans extends SpansWithId {

	private List<CandidateElementSpans> candidateList;
	private int currentDoc, currentPosition;
	private TermSpans termSpans;	
	
	private Logger logger = LoggerFactory.getLogger(ElementSpans.class);

	public ElementSpans(SpanElementQuery spanElementQuery,
			AtomicReaderContext context, Bits acceptDocs,
			Map<Term, TermContext> termContexts) throws IOException {
		super(spanElementQuery, context, acceptDocs, termContexts);
		candidateList = new ArrayList<>();
		termSpans = (TermSpans) firstSpans;
		hasMoreSpans = termSpans.next();
		if (hasMoreSpans) {
			currentDoc = termSpans.doc();
			currentPosition = termSpans.start();
		}
	}

	@Override
	public boolean next() throws IOException {
		isStartEnumeration=false;
		return advance();
	}
	
	/**	Get the next match by first checking the candidate match list
	 * 	and setting the list when it is empty.
	 * */
	private boolean advance() throws IOException {
		while(hasMoreSpans || !candidateList.isEmpty()){
			if (!candidateList.isEmpty()){
				CandidateElementSpans cs = candidateList.get(0);
				this.matchDocNumber = cs.getDoc();
				this.matchStartPosition = cs.getStart();
				this.matchEndPosition = cs.getEnd();
				this.matchPayload = cs.getPayloads();				
				//this.setElementRef(cs.getSpanId());				
				this.setSpanId(cs.getSpanId());
				candidateList.remove(0);
				return true;
			}
			else{
			    //logger.info("Setting candidate list");
				setCandidateList();				
				currentDoc = termSpans.doc();
				currentPosition = termSpans.start();
			}
		}
		return false;
	}

	/**	Collect all the elements in the same start position and sort them by
	 * 	end position (smallest first).
	 * */
	private void setCandidateList() throws IOException {
		while (hasMoreSpans &&	termSpans.doc() == currentDoc && 
				termSpans.start() == currentPosition){
			CandidateElementSpans cs = new CandidateElementSpans(termSpans,
					spanId);
					//elementRef);
			readPayload(cs);
			candidateList.add(cs);
			hasMoreSpans = termSpans.next();
		}
		Collections.sort(candidateList);
	}
	
	
	/**	This method reads the payload of the termSpan and assigns the end 
	 * 	position and element ref to the candidate match. The character offset
	 *  payload is set as the candidate match payload.
	 *  <br/><br/>
	 * 	<em>Note</em>: payloadbuffer should actually collects all other payload
	 * 	beside end position and element ref, but KorapIndex identify element's 
	 * 	payload by its length (8), which is only the character offsets. So
	 * 	these offsets are directly set as the candidate match payload.	
	 * 
	 * 	@author margaretha
	 * */
	private void readPayload(CandidateElementSpans cs) throws IOException {   	
		List<byte[]> payload = (List<byte[]>) termSpans.getPayload();
		int length = payload.get(0).length;
		ByteBuffer bb = ByteBuffer.allocate(length);
		bb.put(payload.get(0));
		
	    if (!payload.isEmpty()) {
			// set element end position from payload
			cs.setEnd(bb.getInt(8));
			
			if (hasSpanId){ // copy element id
				cs.setSpanId(bb.getShort(12));
			}
			else{ // set element id -1
				cs.setSpanId((short) -1);
			}
			// Copy the start and end character offsets
			byte[] b = new byte[8];			
			b = Arrays.copyOfRange(bb.array(), 0, 8);
			cs.setPayloads(Collections.singletonList(b));
	    }
	    else {	
			cs.setEnd(cs.getStart());
			cs.setSpanId((short) -1);
			cs.setPayloads(null);
    	}
	}
	
	@Override
	public boolean skipTo(int target) throws IOException {
		if (hasMoreSpans && (firstSpans.doc() < target)){
  			if (!firstSpans.skipTo(target)){
  				candidateList.clear();
  				return false;
  			}
  		}		
		setCandidateList();
		matchPayload.clear();
		isStartEnumeration=false;
		return advance();
	}

	@Override
	public long cost() {
		return termSpans.cost();
	}
	
	/** Match candidate for element spans.
	 * */	
	class CandidateElementSpans extends CandidateSpan {
		
		private short elementRef;
		
		public CandidateElementSpans(Spans span, short elementRef) 
				throws IOException {
			super(span);
			setSpanId(elementRef);
		}
		
		public void setSpanId(short elementRef) {
			this.elementRef = elementRef;
		}
		public short getSpanId() {
			return elementRef;
		}	
	}
};
