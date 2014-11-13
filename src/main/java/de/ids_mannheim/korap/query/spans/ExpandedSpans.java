package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.SpanExpansionQuery;

/** Spans expanded with min m tokens and max n tokens.
 * @author margaretha
 * */
public class ExpandedSpans extends SimpleSpans{
	
	private int min, max;
	private byte classNumber;
	private int direction;	
	private List<CandidateSpans> candidateSpans;
	private long matchCost;
	
	public ExpandedSpans(SpanExpansionQuery spanExpansionQuery,
			AtomicReaderContext context, Bits acceptDocs,
			Map<Term, TermContext> termContexts) throws IOException {
		super(spanExpansionQuery, context, acceptDocs, termContexts);
		this.min = spanExpansionQuery.getMin();
		this.max = spanExpansionQuery.getMax();
		this.direction = spanExpansionQuery.getDirection();
		this.classNumber = spanExpansionQuery.getClassNumber();
		
		candidateSpans = new ArrayList<CandidateSpans>();		
		hasMoreSpans = true;		
	}

	@Override
	public boolean next() throws IOException {
		matchPayload.clear();
		isStartEnumeration = false;
		if (candidateSpans.size() == 0 && hasMoreSpans)
			hasMoreSpans = firstSpans.next();
		return advance();
	}

	private boolean advance() throws IOException {		
		while (candidateSpans.size() > 0 || hasMoreSpans) {
			if (candidateSpans.size() > 0 ){
				setMatch(candidateSpans.get(0));
				candidateSpans.remove(0);								
				return true;
			}
			else { setCandidateList(); }
		}		
		return false;
	}
	
	private void setCandidateList() throws IOException {
		CandidateSpans cs;
		int counter, start, end;
		
		if (direction < 0 ){
			counter = max;
			while (counter >= min ){
				start =  Math.max(0,firstSpans.start() - counter);			
				cs = new CandidateSpans(
						start, 
						firstSpans.end(), 
						firstSpans.doc(), 
						firstSpans.cost(), 
						calculatePayload(start, firstSpans.start())
				);
								
				candidateSpans.add(cs);
				counter--;
			}
		}
		else{
			counter = min;
			while (counter <= max){
				// TODO: How do I know if the end is already too far (over the end of the doc)? 
				end = firstSpans.end() + counter;
				cs = new CandidateSpans(
						firstSpans.start(), 
						end, 
						firstSpans.doc(), 
						firstSpans.cost(), 
						calculatePayload(firstSpans.end(), end)
				);
				candidateSpans.add(cs);
				counter++;
			}
		}	
	}
	
	private ArrayList<byte[]> calculatePayload(int start, int end)
			throws IOException{
		
		ArrayList<byte[]> payload = new ArrayList<byte[]>();
		if (firstSpans.isPayloadAvailable()){				
			payload.addAll(firstSpans.getPayload());
		}
		if (classNumber > 0 ){	
			//System.out.println("Extension offsets "+start+","+end);
			payload.add(calculateExtensionOffsets(start, end));
		}
		return payload;
	}
	
	private byte[] calculateExtensionOffsets(int start, int end) {
		ByteBuffer buffer = ByteBuffer.allocate(9);		
		buffer.putInt(start);
		buffer.putInt(end);
		buffer.put(classNumber);
		return buffer.array();
	}

	private void setMatch(CandidateSpans candidateSpan) {
		matchDocNumber = candidateSpan.getDoc();
		matchStartPosition = candidateSpan.getStart();
		matchEndPosition = candidateSpan.getEnd();			
		matchPayload = candidateSpan.getPayloads();
		matchCost = candidateSpan.getCost();
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
		return matchCost;
	}

}
