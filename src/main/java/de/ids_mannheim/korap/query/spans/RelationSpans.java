package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
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

import de.ids_mannheim.korap.query.SpanRelationQuery;

/**	Enumeration of spans denoting relations between two tokens/elements. The start and end of 
 * 	a RelationSpan always denote the start and end of the source token/element.
 * 
 * 	There are 4 types of relations, which is differentiated by the payload length in bytes.  
 * 	1. Token to token relation (1 int & 1 short, length: 6) 
 * 	2. Token to span (2 int & 1 short, length: 10)
 * 	3. Span to token (int, byte, int, short, length: 11)
 * 	4. Span to Span (3 int & 1 short, length: 14) 
 * 	
 * 	Every integer value denotes the start/end position of the start/target of a relation, 
 * 	in this format:	(sourceEndPos?, startTargetPos, endTargetPos?). The end position of a token is 
 * 	identical to its start position, and therefore not is saved in a payload.
 * 
 * 	A short value denote the relation id, used for matching relation-attributes.
 * 	The byte in relation #3 is just a dummy to create a different length from the relation #2.
 * 
 * 	NOTE: Sorting of the candidate spans can alternatively be done in indexing, instead of here.
 * 
 * 	@author margaretha
 * */
public class RelationSpans extends SpansWithId{

	//short relationId;
	int targetStart, targetEnd;
	int currentDoc, currentPosition;
	
	private TermSpans relationTermSpan;
	
	protected Logger logger = LoggerFactory.getLogger(RelationSpans.class);
	private List<CandidateRelationSpan> candidateList;
	
	public RelationSpans(SpanRelationQuery relationSpanQuery,
			AtomicReaderContext context, Bits acceptDocs,
			Map<Term, TermContext> termContexts) throws IOException {
		super(relationSpanQuery, context, acceptDocs, termContexts);
		candidateList = new ArrayList<>();
		relationTermSpan = (TermSpans) firstSpans;
		hasMoreSpans = relationTermSpan.next();
	}

	@Override
	public boolean next() throws IOException {
		isStartEnumeration=false;
		return advance();
	}
	
	private boolean advance() throws IOException{
		while(hasMoreSpans || !candidateList.isEmpty()){
			if (!candidateList.isEmpty()){
				CandidateRelationSpan cs = candidateList.get(0);
				this.matchDocNumber = cs.getDoc();
				this.matchStartPosition = cs.getStart();
				this.matchEndPosition = cs.getEnd();
				this.matchPayload = cs.getPayloads();	
				this.spanId = cs.getSpanId(); // relation id
				candidateList.remove(0);
				return true;
			}
			else{
				setCandidateList();				
				currentDoc = relationTermSpan.doc();
				currentPosition = relationTermSpan.start();
			}
		}
		return false;
	}

	private void setCandidateList() throws IOException {
		while (hasMoreSpans &&	relationTermSpan.doc() == currentDoc && 
				relationTermSpan.start() == currentPosition){
			CandidateRelationSpan cs = new CandidateRelationSpan(relationTermSpan);
			readPayload(cs);
			
			candidateList.add(cs);
			hasMoreSpans = relationTermSpan.next();
		}
		Collections.sort(candidateList);
		
		/*for (CandidateRelationSpan cs:candidateList){
			System.out.println(cs.getStart()+","+cs.getEnd() //+" <size:" +payload.get(0).length 
				+" target "+cs.getTargetStart()+","+cs.getTargetEnd() +" id:"+cs.getRelationId());
		}*/
	}

	private void readPayload(CandidateRelationSpan cs) {
		List<byte[]> payload = (List<byte[]>) cs.getPayloads();
		int length = payload.get(0).length;
		BytesRef payloadBytesRef = new BytesRef(payload.get(0));
		
		int i;
		
		switch (length) {
			case 6: // Token to token
				i = PayloadReader.readInteger(payloadBytesRef,0);
				cs.setTargetStart(i);
				cs.setTargetEnd(i);
				break;
	
			case 10: // Token to span
				cs.setTargetStart(PayloadReader.readInteger(payloadBytesRef,0));
				cs.setTargetEnd(PayloadReader.readInteger(payloadBytesRef,4));
				break;
				
			case 11: // Span to token
				cs.setEnd(PayloadReader.readInteger(payloadBytesRef,0));
				i = PayloadReader.readInteger(payloadBytesRef,5);
				cs.setTargetStart(i);
				cs.setTargetEnd(i);
				break;
			
			case 14: // Span to span
				cs.setEnd(PayloadReader.readInteger(payloadBytesRef,0));
				cs.setTargetStart(PayloadReader.readInteger(payloadBytesRef,4));
				cs.setTargetEnd(PayloadReader.readInteger(payloadBytesRef,8));
				break;
		}
		
		cs.setSpanId(PayloadReader.readShort(payloadBytesRef, length-2)); //relation id
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
/*
	public short getRelationId() {
		return relationId;
	}

	public void setRelationId(short relationId) {
		this.relationId = relationId;
	}*/

	public int getTargetStart() {
		return targetStart;
	}

	public void setTargetStart(int targetStart) {
		this.targetStart = targetStart;
	}

	public int getTargetEnd() {
		return targetEnd;
	}

	public void setTargetEnd(int targetEnd) {
		this.targetEnd = targetEnd;
	}
	
	
	class CandidateRelationSpan extends CandidateSpan implements Comparable<CandidateSpan>{
		
		private int targetStart, targetEnd;
		
		public CandidateRelationSpan(Spans span) throws IOException{
			super(span);
		}

		@Override
		public int compareTo(CandidateSpan o) {

			int sourcePositionComparison = super.compareTo(o);
			
			CandidateRelationSpan cs = (CandidateRelationSpan) o;			
			if (sourcePositionComparison == 0){
				if (this.getTargetStart() == cs.getTargetStart()){
					if (this.getTargetEnd() == cs.getTargetEnd())
						return 0;
					if (this.getTargetEnd() > cs.getTargetEnd() )
						return 1;
					else return -1;
				}
				else if (this.getTargetStart() < cs.getTargetStart())
					return -1;
				else return 1;	
			}

			return sourcePositionComparison;
		}
		
		public int getTargetEnd() {
			return targetEnd;
		}

		public void setTargetEnd(int targetEnd) {
			this.targetEnd = targetEnd;
		}

		public int getTargetStart() {
			return targetStart;
		}

		public void setTargetStart(int targetStart) {
			this.targetStart = targetStart;
		}
	}
	
}
