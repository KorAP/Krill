package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.SpanDistanceQuery;

/** Base class for calculating a distance between two ordered spans.
 * 	@author margaretha
 * */
public abstract class OrderedDistanceSpans extends DistanceSpans {

	protected boolean hasMoreFirstSpans;	
	protected int minDistance,maxDistance;
	
	protected List<CandidateSpan> candidateList;
	protected int candidateListIndex;
	protected int candidateListDocNum;
	
    
	public OrderedDistanceSpans(SpanDistanceQuery query,
			AtomicReaderContext context, Bits acceptDocs,
			Map<Term, TermContext> termContexts)
			throws IOException {		
		super(query, context, acceptDocs, termContexts);
		
		minDistance = query.getMinDistance();
		maxDistance = query.getMaxDistance();		
  		 		  		
  		hasMoreFirstSpans = firstSpans.next();
  		
		candidateList = new ArrayList<>();
		candidateListIndex = -1;
		candidateListDocNum = firstSpans.doc();
	}
	
	/** Find a span match in the candidate list.
	 * */
	@Override
	protected boolean advance() throws IOException {
		while( hasMoreSpans && candidateListIndex < candidateList.size() ){					
			// Check candidates
			for (candidateListIndex++;candidateListIndex < candidateList.size();
					candidateListIndex++){
				if (findMatch())					
					return true;					
			}			
			
			do { // Forward secondspan 
				hasMoreSpans = secondSpans.next();
				setCandidateList();
			}
			while (hasMoreSpans && !isSecondSpanValid());
		}
		return false;
	}
	
	/** Determine if the current second span is valid. It is always valid in 
	 * 	TokenDistanceSpan, but it can be invalid in the ElementDistanceSpan,
	 * 	namely when it is not within a particular element (a sentence or a 
	 * 	paragraph depends on the element distance unit).
	 *  
	 * */
	protected abstract boolean isSecondSpanValid() throws IOException;
	
	/** Collect all possible firstspan instances as candidate spans for
	 * 	the current secondspan. The candidate spans are within the max 
	 * 	distance from the current secondspan. 
	 * */
	protected abstract void setCandidateList() throws IOException;
	
	/** Define the conditions for a match. 
	 * */
	protected abstract boolean findMatch() throws IOException;	
	
	/** Define the properties of a span match.
	 * */
	protected void setMatchProperties(CandidateSpan candidateSpan, 
			boolean isDistanceZero) throws IOException{
		
		setMatchFirstSpan(candidateSpan);
		setMatchSecondSpan(new CandidateSpan(secondSpans));
		
		if (isDistanceZero){
			matchStartPosition = Math.min(candidateSpan.getStart(), secondSpans.start());
			matchEndPosition = Math.max(candidateSpan.getEnd(), secondSpans.end());
		}
		else {
			matchStartPosition = candidateSpan.getStart();
			matchEndPosition = secondSpans.end();
		}
		
		this.matchDocNumber = secondSpans.doc();		
		if (collectPayloads){			
  		    if (candidateSpan.getPayloads() != null) {
  		    	matchPayload.addAll(candidateSpan.getPayloads()); 
  		    }
  		    if (secondSpans.isPayloadAvailable()) {
  		    	matchPayload.addAll(secondSpans.getPayload());  		    	
  		    }
		}
		
		log.trace("doc# {}, start {}, end {}",matchDocNumber,matchStartPosition,
				matchEndPosition);	
	}

	@Override
	public boolean skipTo(int target) throws IOException {
		if (hasMoreSpans && (secondSpans.doc() < target)){
  			if (!secondSpans.skipTo(target)){
  				candidateList.clear();
  				return false;
  			}
  		} 	
		
		setCandidateList();
		matchPayload.clear();
		isStartEnumeration=false;
		return advance();
	}
}
