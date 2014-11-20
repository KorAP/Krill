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
import org.apache.lucene.util.Bits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ids_mannheim.korap.query.SpanAttributeQuery;

/** Span enumeration of attributes which are treated as a normal term with 
 * 	special payload assignment referring to to which element it belongs.  
 * 	The class is basically a wrapper of the TermSpan with additional 
 * 	functionality regarding element reference. Element reference is 
 * 	annotated ascendingly starting from the left side.
 * 
 * 	The enumeration is ordered firstly by the start position of the attribute 
 * 	and secondly by the element reference descendingly with respect to the 
 * 	nature of the order of the element enumeration. 
 *  
 *  @author margaretha
 * */
public class AttributeSpans extends SimpleSpans{
	
	private List<CandidateAttributeSpan> candidateList;
	private int currentDoc, currentPosition;
	private short spanId;
	private boolean isFinish;
	private int elementEnd;
	
	protected Logger logger = LoggerFactory.getLogger(AttributeSpans.class);

	public AttributeSpans(SpanAttributeQuery simpleSpanQuery,
			AtomicReaderContext context, Bits acceptDocs,
			Map<Term, TermContext> termContexts) throws IOException {
		super(simpleSpanQuery, context, acceptDocs, termContexts);		
		candidateList = new ArrayList<>();
		hasMoreSpans = firstSpans.next();
		if (hasMoreSpans) {
			currentDoc = firstSpans.doc();
			currentPosition = firstSpans.start();
		}		
	}

	@Override
	public boolean next() throws IOException {		
		isStartEnumeration=false;
		matchPayload.clear();
		return advance();
	}


	/**	Get the next match by first checking the candidate match list
	 * 	and setting the list when it is empty.
	 * */
	private boolean advance() throws IOException {		
		while(hasMoreSpans || !candidateList.isEmpty()){
			if (!candidateList.isEmpty()){
				// set AttributeSpan from 
				CandidateAttributeSpan cs = candidateList.get(0);
				this.matchDocNumber = cs.getDoc();
				this.matchStartPosition = cs.getStart();
				this.matchEndPosition = cs.getEnd();
				this.setSpanId(cs.getSpanId());
				this.setElementEnd(cs.getElementEnd());
				candidateList.remove(0);
				return true;
			}
			else{
			    //logger.info("Setting candidate list");
				setCandidateList();
//				for (CandidateAttributeSpan cs: candidateList){
//					logger.info("cs ref "+cs.getElementRef());
//				}
				currentDoc = firstSpans.doc();
				currentPosition = firstSpans.start();
			}
		}
		return false;
	}
	
	/**	Collects all the attributes in the same start position and sort
	 * 	them by elementRef in reverse order (the ones with the bigger 
	 * 	elementRef first). 
	 * */
	private void setCandidateList() throws IOException {
		
		while (hasMoreSpans &&	firstSpans.doc() == currentDoc && 
				firstSpans.start() == currentPosition){
			
			candidateList.add(createCandidateSpan(firstSpans));
			hasMoreSpans = firstSpans.next();
		}
		
		Collections.sort(candidateList);
		Collections.reverse(candidateList);
	}
	
	private CandidateAttributeSpan createCandidateSpan(Spans firstSpans) throws IOException {
		List<byte[]> payload = (List<byte[]>) firstSpans.getPayload();
		ByteBuffer wrapper = ByteBuffer.wrap(payload.get(0));
		
		short spanId = wrapper.getShort(0);		
		int elementEnd = -1;
		if (payload.get(0).length == 6){
			elementEnd = wrapper.getInt(2);
		}		
		return new CandidateAttributeSpan(firstSpans,spanId, elementEnd);
	}
	
	public short getSpanId(){
		return this.spanId;
	}

	public void setSpanId(short spanId) {
		this.spanId = spanId;
	}

	public int getElementEnd() {
		return elementEnd;
	}

	public void setElementEnd(int elementEnd) {
		this.elementEnd = elementEnd;
	}

	public boolean isFinish() {
		return isFinish;
	}

	public void setFinish(boolean isFinish) {
		this.isFinish = isFinish;
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
		return firstSpans.cost();
	}
	
	/** Match candidate for attribute spans.
	 * */
	class CandidateAttributeSpan extends CandidateSpans 
			implements Comparable<CandidateSpans>{

		private short spanId;
		private int elementEnd;
		
		public CandidateAttributeSpan(Spans span, short spanId, int elementEnd) 
				throws IOException {
			super(span);
			setSpanId(spanId);
			setElementEnd(elementEnd);
		}
		
		public void setSpanId(short spanId) {
			this.spanId = spanId;
		}
		public short getSpanId() {
			return spanId;
		}

		public int getElementEnd() {
			return elementEnd;
		}

		public void setElementEnd(int elementEnd) {
			this.elementEnd = elementEnd;
		}

		@Override
		public int compareTo(CandidateSpans o) {
			CandidateAttributeSpan cs = (CandidateAttributeSpan) o;
			if (this.spanId == cs.spanId)
				return 0;
			else if (this.spanId > cs.spanId )
				return 1;
			return -1;			
		}		
	}
}
