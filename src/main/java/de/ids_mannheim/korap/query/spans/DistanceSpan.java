package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.util.Bits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ids_mannheim.korap.query.SpanDistanceQuery;

/** DistanceSpan is a base class for enumeration of span matches, 
 * 	whose two child spans have a specific range of distance (within 
 * 	a min and a max distance) and must be in order (a firstspan is 
 * 	followed by a secondspan). 
 * 
 * @author margaretha
 * */
public abstract class DistanceSpan extends SimpleSpans{	

	protected boolean hasMoreFirstSpans;	
	protected boolean collectPayloads;
	protected int minDistance,maxDistance;
	
	protected List<CandidateSpan> candidateList;
	protected int candidateListIndex;
	protected int candidateListDocNum;
	
    private Logger log = LoggerFactory.getLogger(DistanceSpan.class);
    
	public DistanceSpan(SpanDistanceQuery query,
			AtomicReaderContext context, Bits acceptDocs,
			Map<Term, TermContext> termContexts)
			throws IOException {		
		super(query, context, acceptDocs, termContexts);
		
		minDistance = query.getMinDistance();
		maxDistance = query.getMaxDistance();		
  		collectPayloads = query.isCollectPayloads();
  		 		  		
  		hasMoreFirstSpans = firstSpans.next();
  		
		candidateList = new ArrayList<>();
		candidateListIndex = -1;
		candidateListDocNum = firstSpans.doc();
	}	
	
	@Override
	public boolean next() throws IOException {		
		isStartEnumeration=false;
  		matchPayload.clear();
		return advance();
	}
	
	/** Find a span match in the candidate list.
	 * */
	private boolean advance() throws IOException {
		while( hasMoreSpans && candidateListIndex < candidateList.size() ){					
			// Check candidates
			for (candidateListIndex++;candidateListIndex < candidateList.size();
					candidateListIndex++){
				if (findMatch())					
					return true;					
			}			
			// Forward secondspan
			if (hasMoreSpans = secondSpans.next())			
				setCandidateList();
		}
		return false;
	}
	
	/** Collect all possible firstspan instances as candidate spans for
	 * 	the current secondspan. The candidate spans are within the max 
	 * 	distance from the current secondspan. 
	 *  
	 * */
	protected abstract void setCandidateList() throws IOException;
	
	/** Define the conditions for a match. 
	 * */
	protected abstract boolean findMatch() throws IOException;	
	
	/** Define the properties of a span match.
	 * */
	protected void setMatchProperties(CandidateSpan candidateSpan, 
			boolean isDistanceZero) throws IOException{
		
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
