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

/** Enumeration of token-based distance span matches. 
 * 	Each match consists of two specified spans having an actual distance
 *  in the range of the min and max distance parameters given in the query.
 * 
 *	@author margaretha 
 * */
public class TokenDistanceSpan extends DistanceSpan{

	public TokenDistanceSpan(SpanDistanceQuery query,
			AtomicReaderContext context, Bits acceptDocs,
			Map<Term, TermContext> termContexts) throws IOException {
		super(query, context, acceptDocs, termContexts);		
		hasMoreSpans = hasMoreFirstSpans;
	}
	
	@Override
	protected void setCandidateList() throws IOException{
 		if (candidateListDocNum == secondSpans.doc()){						
			copyPossibleCandidates();
			addNewCandidates();
			candidateListIndex = -1;
 		}
 		else {
 			candidateList.clear(); 			
 			if (hasMoreFirstSpans && ensureSameDoc(firstSpans,secondSpans)){
 				candidateListDocNum = firstSpans.doc();
				addNewCandidates();
				candidateListIndex = -1;
			}		
		} 		
	}
	
	/** Copy candidate spans which are still possible to create a match,
	 * 	from the candidate list prepared for the previous second spans. 
	 * */
	private void copyPossibleCandidates(){
		List<CandidateSpan> temp = new ArrayList<>();
		for (CandidateSpan cs : candidateList){
			if (cs.getEnd()+maxDistance > secondSpans.start())
				temp.add(cs);
		}
		candidateList = temp;
	}
	
	/** Add new possible candidates for the current secondspan.
	 * */
	private void addNewCandidates() throws IOException{
		while ( hasMoreFirstSpans && 
				firstSpans.doc() == candidateListDocNum &&
				firstSpans.start() < secondSpans.end()){
			
			if (firstSpans.end()+maxDistance > secondSpans.start())
				candidateList.add(new CandidateSpan(firstSpans));
			
			hasMoreFirstSpans = firstSpans.next();
		}
	}
	
	@Override
	protected boolean findMatch() throws IOException {
		CandidateSpan candidateSpan = candidateList.get(candidateListIndex);		
		if (minDistance == 0 &&
				// intersection
				candidateSpan.getStart() < secondSpans.end() && 
				secondSpans.start() < candidateSpan.getEnd()){		
			
			setMatchProperties(candidateSpan, true);
			return true;			
		}
		
		int actualDistance = secondSpans.start() - candidateSpan.getEnd() +1;
		if (candidateSpan.getStart() < secondSpans.start() &&
				minDistance <= actualDistance && 
				actualDistance <= maxDistance){					
			
			setMatchProperties(candidateSpan, false);
			return true;
		}		
		return false;
	}
	
	@Override
	public long cost() {
		CandidateSpan candidateSpan = candidateList.get(candidateListIndex);
		return candidateSpan.getCost() + secondSpans.cost();
	}

}
