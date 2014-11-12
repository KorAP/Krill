package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.util.Bits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ids_mannheim.korap.query.SpanRepetitionQuery;

/**	Enumeration of spans occurring multiple times in a sequence.
 * 	The number of min and max repetition can be set. 
 *   
 * 	@author margaretha
 * */
public class RepetitionSpans extends SimpleSpans{
	
	private int min,max;
	private long matchCost;
	private List<CandidateSpans> matchList;
	private Logger log = LoggerFactory.getLogger(RepetitionSpans.class);
    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;


	public RepetitionSpans(SpanRepetitionQuery query,
			AtomicReaderContext context, Bits acceptDocs,
			Map<Term, TermContext> termContexts) 
			throws IOException {
		super(query, context, acceptDocs, termContexts);
		this.min = query.getMin();
		this.max = query.getMax();
		matchList = new ArrayList<CandidateSpans>();
		hasMoreSpans = firstSpans.next();
	}

	@Override
	public boolean next() throws IOException {
		isStartEnumeration = false;
		matchPayload.clear();
		return advance();
	}

	/** Get the next span from the candidate match list, or set it first when
	 * 	it is empty.
	 * */
	private boolean advance() throws IOException {

		while (hasMoreSpans  || !matchList.isEmpty()){
			if (!matchList.isEmpty()){			
				setMatchProperties(matchList.get(0));
				matchList.remove(0);
				return true;
			}			
			matchCost = 0;
			
			List<CandidateSpans> adjacentSpans = collectAdjacentSpans();
			setMatchList(adjacentSpans);
		}	
		return false;
	}
	
	/** Collect all adjacent spans occurring in a sequence.
	 * 	@return a list of the adjacent spans 
	 * */
	private List<CandidateSpans> collectAdjacentSpans() throws IOException {
		
		CandidateSpans startSpan = new CandidateSpans(firstSpans);
		
		List<CandidateSpans> adjacentSpans = new ArrayList<CandidateSpans>();
		adjacentSpans.add(startSpan);
		
		CandidateSpans prevSpan = startSpan;
		
		while ((hasMoreSpans = firstSpans.next()) &&
			startSpan.getDoc() == firstSpans.doc() ){
			
			if (firstSpans.start() > prevSpan.getEnd()){
				break;
			}
			else if (firstSpans.start() == prevSpan.getEnd()){
				prevSpan = new CandidateSpans(firstSpans);
				adjacentSpans.add(prevSpan);
			}
		}		
		return 	adjacentSpans;	
	}
	
	/** Generate all possible repetition candidate spans from the adjacent spans 
	 * 	and add them to the match list. 
	 * */
	private void setMatchList(List<CandidateSpans> adjacentSpans){
		CandidateSpans startSpan, endSpan, matchSpan;
		for (int i=min; i<max+1; i++){
			//System.out.println("num: "+i);			
			int j=0; 
			int endIndex;
			while ((endIndex = j+i-1) < adjacentSpans.size()){
				startSpan = adjacentSpans.get(j);
				if (i == 1){					
					try {
						matchSpan = startSpan.clone();
						matchSpan.setPayloads(computeMatchPayload(adjacentSpans, 0, endIndex-1));
						matchList.add(matchSpan);		
					} catch (CloneNotSupportedException e) {
						e.printStackTrace();
					}
				}
				else {
					endSpan = adjacentSpans.get(endIndex);														
					matchSpan = new CandidateSpans(
							startSpan.getStart(), 
							endSpan.getEnd(), 
							startSpan.getDoc(), 
							computeMatchCost(adjacentSpans, 0, endIndex), 
							computeMatchPayload(adjacentSpans, 0, endIndex));
					
					//System.out.println("c:"+matchSpan.getCost() +" p:"+ matchSpan.getPayloads().size());
					//System.out.println(startSpan.getStart() +","+endSpan.getEnd());
					
					matchList.add(matchSpan);
				}
				j++;
			}			
		}
		
		Collections.sort(matchList);
	}
	
	/** Add all the payloads of a candidate span
	 * */
	private Collection<byte[]> computeMatchPayload(
			List<CandidateSpans> adjacentSpans, int start, int end) {
		Collection<byte[]> payload = new ArrayList<byte[]>();
		for (int i=start; i<= end; i++){
			payload.addAll(adjacentSpans.get(i).getPayloads());
		}
		return payload;
	}

	/** Add all the cost of a candidate span
	 * */
	private long computeMatchCost(List<CandidateSpans> adjacentSpans, 
			int start, int end){		
		long matchCost = 0;
		for (int i=start; i<= end; i++){
			CandidateSpans c = adjacentSpans.get(i);
			matchCost += adjacentSpans.get(i).getCost();
		}		
		return matchCost;
	}
	
	
	/** Setting match properties from the candidate span
	 * */	
	private void setMatchProperties(CandidateSpans candidateSpan) 
			throws IOException {
	    matchDocNumber = candidateSpan.getDoc();
	    matchStartPosition = candidateSpan.getStart();
	    matchEndPosition = candidateSpan.getEnd();		
		if (collectPayloads && candidateSpan.getPayloads() != null) {  		    	
	    	matchPayload.addAll(candidateSpan.getPayloads());  		    	
	    }
		
		if (DEBUG)
		    log.trace("doc# {}, start {}, end {}",matchDocNumber,matchStartPosition,
			      matchEndPosition);		
	}

	@Override
	public boolean skipTo(int target) throws IOException {
		if (hasMoreSpans && firstSpans.doc() < target){
			if (!firstSpans.skipTo(target)){
				hasMoreSpans = false;
				return false;
			}
		}		
		matchList.clear();
		return advance();
	}

	@Override
	public long cost() {
		return matchCost;
	}	
}
