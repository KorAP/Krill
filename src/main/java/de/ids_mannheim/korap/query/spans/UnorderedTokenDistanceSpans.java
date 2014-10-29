package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.SpanDistanceQuery;

/** Enumeration of span matches, whose two child spans have a specific range 
 * 	of distance (within a min and a max distance) and can be in any order. 
 * 	The unit distance is a token position.
 * 
 * @author margaretha
 * */
public class UnorderedTokenDistanceSpans extends UnorderedDistanceSpans{

	public UnorderedTokenDistanceSpans(SpanDistanceQuery query,
			AtomicReaderContext context, Bits acceptDocs,
			Map<Term, TermContext> termContexts) throws IOException {
		super(query, context, acceptDocs, termContexts);		
	}
	
	@Override
	protected boolean prepareLists() throws IOException {		
		
		if (firstSpanList.isEmpty() && secondSpanList.isEmpty()){
			if (hasMoreFirstSpans && hasMoreSecondSpans &&
					ensureSameDoc(firstSpans, secondSpans)){			
				firstSpanList.add(new CandidateSpans(firstSpans));
				secondSpanList.add(new CandidateSpans(secondSpans));
				currentDocNum = firstSpans.doc();
				hasMoreFirstSpans = firstSpans.next();
				hasMoreSecondSpans = secondSpans.next();				
			}
			else { 
				hasMoreSpans = false;
				return false;
			}
		}
		else if (firstSpanList.isEmpty() && hasMoreFirstSpans && 
				firstSpans.doc() == currentDocNum){
			firstSpanList.add(new CandidateSpans(firstSpans));
			hasMoreFirstSpans = firstSpans.next();
		}
		else if (secondSpanList.isEmpty() && hasMoreSecondSpans && 
				secondSpans.doc() == currentDocNum){
			secondSpanList.add(new CandidateSpans(secondSpans));
			hasMoreSecondSpans = secondSpans.next();
		}
		return true;
	}
	
	@Override
	protected boolean setCandidateList(List<CandidateSpans> 
			candidateList, Spans candidate, boolean hasMoreCandidates,
			List<CandidateSpans> targetList) throws IOException {
		
		if (!targetList.isEmpty()){
			CandidateSpans target = targetList.get(0);
			while (hasMoreCandidates && candidate.doc() == target.getDoc()
					&& isWithinMaxDistance(target,candidate)){
				candidateList.add(new CandidateSpans(candidate));
				hasMoreCandidates = candidate.next();
			}
		}
		return hasMoreCandidates;
	}
	
	/** Check if the target and candidate spans are not too far from each other.
	 *  @return true iff the target and candidate spans are within the maximum
	 *  distance
	 * */
	protected boolean isWithinMaxDistance(CandidateSpans target, Spans candidate) {
		// left candidate
		if (candidate.end() < target.getStart() && 
				candidate.end() + maxDistance <= target.getStart()){
			return false;
		}
		// right candidate
		if (candidate.start() > target.getEnd() &&
				target.getEnd() + maxDistance <= candidate.start()){
			return false;
		}
		return true;
	}
	
	@Override	
	protected List<CandidateSpans> findMatches(CandidateSpans target, List<CandidateSpans> 
			candidateList) {
		
		List<CandidateSpans> matches = new ArrayList<>();		
		int actualDistance;
		for (CandidateSpans cs : candidateList){
			if (minDistance == 0 &&
					//intersection
					target.getStart() < cs.getEnd() &&
					cs.getStart() < target.getEnd()){
				matches.add(createMatchCandidate(target,cs,true));
				continue;
			}
			
			// left candidate
			if (cs.getEnd() < target.getStart())
				actualDistance = target.getStart() - cs.getEnd() +1;			 
			else // right candidate
				actualDistance = cs.getStart() - target.getEnd() +1;
			 
			if (minDistance <= actualDistance && actualDistance <= maxDistance)
				matches.add(createMatchCandidate(target, cs, false));			
		}		
		return matches;
	}

	@Override
	protected void updateList(List<CandidateSpans> candidateList) {
		candidateList.remove(0);		
	}

}
