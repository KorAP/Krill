package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.SpanDistanceQuery;

/** Span enumeration of the first spans which do NOT occur 
 * 	together with the second spans within a range of distance.
 * 
 * 	@author margaretha
 * */
public class DistanceExclusionSpans extends DistanceSpans{
	
	private int minDistance, maxDistance;
	private boolean isOrdered;
	private boolean hasMoreSecondSpans;

	public DistanceExclusionSpans(SpanDistanceQuery query,
			AtomicReaderContext context, Bits acceptDocs,
			Map<Term, TermContext> termContexts, boolean isOrdered) 
			throws IOException {
		super(query, context, acceptDocs, termContexts);
		minDistance = query.getMinDistance();
		maxDistance = query.getMaxDistance();		
  		this.isOrdered = isOrdered;
  		hasMoreSpans = firstSpans.next();
  		hasMoreSecondSpans = secondSpans.next();
	}

	@Override
	protected boolean advance() throws IOException {
		
		while(hasMoreSpans){			
			if (hasMoreSecondSpans) forwardSecondSpans();			
			
			if (findMatch()){ 
				hasMoreSpans = firstSpans.next();
				return true;
			}
			hasMoreSpans = firstSpans.next();
		}
		return false;
	}

	private void forwardSecondSpans() throws IOException{
		
		if (secondSpans.doc() < firstSpans.doc()){			
			hasMoreSecondSpans = secondSpans.skipTo(firstSpans.doc());			
		}
		
		// skip the secondSpan to the right side of the firstspan
		while (hasMoreSecondSpans && secondSpans.doc() == firstSpans.doc() &&
				firstSpans.start() >= secondSpans.end()){

			// the firstspan is within maxDistance
			if (!isOrdered && calculateActualDistance() <= maxDistance){
				break; 
			}		
			hasMoreSecondSpans = secondSpans.next();
						
		}		
	}
	
	/** Calculate the distance / difference between a firstspan and 
	 * 	a secondspan positions.
	 * 	
	 * @return distance
	 * */
	private int calculateActualDistance(){
		// right secondSpan
		if (firstSpans.end() <= secondSpans.start())
			return secondSpans.start() - firstSpans.end() +1;
		// left secondSpan
		return firstSpans.start() - secondSpans.end() +1; 
	}
	
	/** Check the distance between the current first span and second span 
	 * 	against the min and max distance constraints.
	 * 	
	 * 	@return true if the distance between the first and the second spans
	 * 			 are smaller as the minimum distance or bigger than the max
	 * 			 distance.   
	 * */
	private boolean findMatch() throws IOException {
		if (!hasMoreSecondSpans || secondSpans.doc() > firstSpans.doc()){
			setMatchProperties();
			return true;
		}
		if (minDistance == 0 &&
				firstSpans.start() < secondSpans.end() && 
				secondSpans.start() < firstSpans.end()){
			return false;
		}
			
		int actualDistance = calculateActualDistance();
		if (actualDistance < minDistance || actualDistance > maxDistance){
			setMatchProperties();
			return true;
		}
		
		return false;
	}
	
	/** Set the current firstspan as the match
	 * 
	 * */
	private void setMatchProperties() throws IOException{
		matchDocNumber = firstSpans.doc();
		matchStartPosition = firstSpans.start();
		matchEndPosition = firstSpans.end();
		
  		if (collectPayloads && firstSpans.isPayloadAvailable())
	    	matchPayload.addAll(firstSpans.getPayload());
  		
  		setMatchFirstSpan(new CandidateSpans(firstSpans));
  		
//	    log.trace("doc# {}, start {}, end {}",
//		      matchDocNumber,
//		      matchStartPosition,
//		      matchEndPosition);		
	}

	@Override
	public boolean skipTo(int target) throws IOException {
		if (hasMoreSpans && firstSpans.doc() < target){
			if (!firstSpans.skipTo(target)){
				hasMoreSpans = false;
				return false;
			}
		}
		return advance();
	}

	@Override
	public long cost() {
		return firstSpans.cost() + secondSpans.cost();
	}
	
}
