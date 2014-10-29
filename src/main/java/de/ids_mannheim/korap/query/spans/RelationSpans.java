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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ids_mannheim.korap.query.SpanRelationQuery;

/**	Enumeration of spans denoting relations between two tokens/elements. The start and end of 
 * 	a RelationSpan always denote the start and end of the left-side token/element.
 * 
 * 	There are 4 types of relations, which is differentiated by the payload length in bytes.  
 * 	1. Token to token relation (1 int & 3 short, length: 10) 
 * 	2. Token to span (2 int & 3 short, length: 14)
 * 	3. Span to token (int, byte, int, 3 short, length: 15)
 * 	4. Span to Span (3 int & 3 short, length: 18) 
 * 	
 * 	Every integer value denotes the start/end position of the start/target of a relation, 
 * 	in this format:	(sourceEndPos?, startTargetPos, endTargetPos?). The end position of a token is 
 * 	identical to its start position, and therefore not is saved in a payload.
 *
 *	The short values denote the relation id, left id, and right id.
 * 	The byte in relation #3 is just a dummy to create a different length from the relation #2.
 * 
 * 	NOTE: Sorting of the candidate spans can alternatively be done in indexing, instead of here. 
 * 	(first by left positions and then by right positions)
 * 
 * 	@author margaretha
 * */
public class RelationSpans extends SpansWithId{

	//short relationId;
	private int rightStart, rightEnd;
	private int currentDoc, currentPosition;
	private short leftId, rightId;
	
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
				this.setRightStart(cs.getRightStart());
				this.setRightEnd(cs.getRightEnd());
				this.spanId = cs.getSpanId(); // relation id
				this.leftId = cs.getLeftId();
				this.rightId = cs.getRightId();
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
		
//		for (CandidateRelationSpan cs:candidateList){
//			System.out.println(cs.getStart()+","+cs.getEnd() //+" <size:" +payload.get(0).length 
//				+" target "+cs.getRightStart()+","+cs.getRightEnd() +" id:"+cs.getSpanId());
//		}
	}

	private void readPayload(CandidateRelationSpan cs) {
		List<byte[]> payload = (List<byte[]>) cs.getPayloads();
		int length = payload.get(0).length;
		ByteBuffer bb = ByteBuffer.allocate(length);
		bb.put(payload.get(0));
		
		int i;		
		switch (length) {
			case 10: // Token to token				
				i = bb.getInt(0);
				cs.setRightStart(i-1);
				cs.setRightEnd(i);
				break;
	
			case 14: // Token to span
				cs.setRightStart(bb.getInt(0));
				cs.setRightEnd(bb.getInt(4));
				break;
				
			case 15: // Span to token
				cs.setEnd(bb.getInt(0));
				i = bb.getInt(5);
				cs.setRightStart(i-1);
				cs.setRightEnd(i);
				break;
			
			case 18: // Span to span
				cs.setEnd(bb.getInt(0));
				cs.setRightStart(bb.getInt(4));
				cs.setRightEnd(bb.getInt(8));
				break;
		}		
		
		cs.setRightId(bb.getShort(length-2)); //right id
		cs.setLeftId(bb.getShort(length-4)); //left id
		cs.setSpanId(bb.getShort(length-6)); //relation id
		// Payload is cleared.		
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

	public int getRightStart() {
		return rightStart;
	}

	public void setRightStart(int rightStart) {
		this.rightStart = rightStart;
	}

	public int getRightEnd() {
		return rightEnd;
	}

	public void setRightEnd(int rightEnd) {
		this.rightEnd = rightEnd;
	}



	public short getLeftId() {
		return leftId;
	}

	public void setLeftId(short leftId) {
		this.leftId = leftId;
	}



	public short getRightId() {
		return rightId;
	}

	public void setRightId(short rightId) {
		this.rightId = rightId;
	}



	class CandidateRelationSpan extends CandidateSpans implements Comparable<CandidateSpans>{
		
		private int rightStart, rightEnd;
		private short leftId, rightId;
		
		public CandidateRelationSpan(Spans span) throws IOException{
			super(span);
		}

		@Override
		public int compareTo(CandidateSpans o) {

			int sourcePositionComparison = super.compareTo(o);
			
			CandidateRelationSpan cs = (CandidateRelationSpan) o;			
			if (sourcePositionComparison == 0){
				if (this.getRightStart() == cs.getRightStart()){
					if (this.getRightEnd() == cs.getRightEnd())
						return 0;
					if (this.getRightEnd() > cs.getRightEnd() )
						return 1;
					else return -1;
				}
				else if (this.getRightStart() < cs.getRightStart())
					return -1;
				else return 1;	
			}

			return sourcePositionComparison;
		}
		
		public int getRightEnd() {
			return rightEnd;
		}

		public void setRightEnd(int rightEnd) {
			this.rightEnd = rightEnd;
		}

		public int getRightStart() {
			return rightStart;
		}

		public void setRightStart(int rightStart) {
			this.rightStart = rightStart;
		}

		public short getLeftId() {
			return leftId;
		}

		public void setLeftId(short leftId) {
			this.leftId = leftId;
		}

		public short getRightId() {
			return rightId;
		}

		public void setRightId(short rightId) {
			this.rightId = rightId;
		}

	}
	
}
