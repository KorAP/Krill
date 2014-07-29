package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.search.spans.TermSpans;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ids_mannheim.korap.query.SpanElementQuery;

/**  
 * @author Nils Diewald, margaretha
 *
 * Use copyFrom instead of clone
 */
public class ElementSpans extends SimpleSpans {

	private List<CandidateElementSpans> candidateList;
	private int currentDoc, currentPosition;
	private short elementRef;
	private TermSpans termSpans;
	
	public boolean isElementRef = false; // A dummy flag
	
	protected Logger logger = LoggerFactory.getLogger(AttributeSpans.class);
	
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
				this.setElementRef(cs.getElementRef());				
				candidateList.remove(0);
				return true;
			}
			else{
				logger.info("Setting candidate list");
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
					elementRef);
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
	    BytesRef payload = termSpans.getPostings().getPayload();
	    //ByteBuffer payloadBuffer = ByteBuffer.allocate(128);
	    
	    if (payload != null) {
			// Copy some payloads like start character and end character
	    	//payloadBuffer.put(payload.bytes, payload.offset, 8);
			
			cs.setEnd(readEndPostion(payload));
			
			if (isElementRef ){
				// Copy rest of payloads after the end position and elementref
				//payloadBuffer.put(payload.bytes, payload.offset + 14, payload.length - 14);				
				cs.setElementRef(readElementRef(payload));
			}
			else{
				// Copy rest of payloads after the end position
				//payloadBuffer.put(payload.bytes, payload.offset + 12, payload.length - 12);
				cs.setElementRef((short) -1);
			}
			
			//byte[] offsetCharacters = new byte[8];
			//System.arraycopy(payloadBuffer.array(), 0, offsetCharacters, 0, 8);
			
			cs.setPayloads(Collections.singletonList(readOffset(payload)));
	    }
	    else {	
			cs.setEnd(cs.getStart());
			cs.setElementRef((short) -1);
			cs.setPayloads(null);
    	}
	}
	
	
	/**	Get the offset bytes from the payload.
	 * */
	private byte[] readOffset(BytesRef payload){
		byte[] b = new byte[8];
		System.arraycopy(payload.bytes, payload.offset, b, 0, 8);
		return b;
	}
	
	/**	Get the end position bytes from the payload and cast it to int. 
	 * */
	private int readEndPostion(BytesRef payload) {
		byte[] b = new byte[4];
		System.arraycopy(payload.bytes, payload.offset + 8, b, 0, 4);
		return ByteBuffer.wrap(b).getInt();		
	}
	
	/**	Get the elementRef bytes from the payload and cast it into short.
	 * */
	private short readElementRef(BytesRef payload) {
    	byte[] b = new byte[2];
    	System.arraycopy(payload.bytes, payload.offset + 12, b, 0, 2);
    	return ByteBuffer.wrap(b).getShort();
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
	
	public short getElementRef() {
		return elementRef;
	}

	public void setElementRef(short elementRef) {
		this.elementRef = elementRef;
	}
	
	/** Match candidate for element spans.
	 * */	
	class CandidateElementSpans extends CandidateSpan {
		
		private short elementRef;
		
		public CandidateElementSpans(Spans span, short elementRef) 
				throws IOException {
			super(span);
			setElementRef(elementRef);
		}
		
		public void setElementRef(short elementRef) {
			this.elementRef = elementRef;
		}
		public short getElementRef() {
			return elementRef;
		}	
	}
};
