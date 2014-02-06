package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ids_mannheim.korap.query.SpanDistanceQuery;

/** Enumeration of span matches, whose two child spans have a specific range 
 * 	of distance (within a min and a max distance) and can be in any order. 
 * 	
 * 	@author margaretha
 * */
public abstract class UnorderedDistanceSpans extends SimpleSpans{

	protected int minDistance, maxDistance;
	private boolean collectPayloads;	
	
	protected boolean hasMoreFirstSpans, hasMoreSecondSpans;
	protected List<CandidateSpan> firstSpanList, secondSpanList;	
	protected List<CandidateSpan> matchList;
	private long matchCost;
	
	protected int updatedListNum;
	
	private Logger log = LoggerFactory.getLogger(UnorderedDistanceSpans.class);
	
	public UnorderedDistanceSpans(SpanDistanceQuery query,
			AtomicReaderContext context, Bits acceptDocs,
			Map<Term, TermContext> termContexts) throws IOException {
		super(query, context, acceptDocs, termContexts);
		minDistance = query.getMinDistance();
		maxDistance =  query.getMaxDistance();
		collectPayloads = query.isCollectPayloads();
		
		firstSpanList = new ArrayList<CandidateSpan>();
		secondSpanList = new ArrayList<CandidateSpan>();
		matchList = new ArrayList<CandidateSpan>();
		
		hasMoreFirstSpans = firstSpans.next();
		hasMoreSecondSpans = secondSpans.next();
		hasMoreSpans = hasMoreFirstSpans && hasMoreSecondSpans;
	}

	@Override
	public boolean next() throws IOException {
		isStartEnumeration = false;
		matchPayload.clear();
		return advance();
	}
	
	/** Find the next span match.
	 * @return true iff a span match is available.
	 * */
	private boolean advance() throws IOException {
		while (hasMoreSpans || !matchList.isEmpty()){			
			if (!matchList.isEmpty()){
				setMatchProperties();
				return true;
			}
			
			if (firstSpanList.isEmpty() && secondSpanList.isEmpty()){
				if (fillEmptyCandidateLists()){							
					setMatchList();
				}
				else { hasMoreSpans = false; }
			}
			else { setMatchList(); }			
		}
		return false;
	}
	
	/** Add the next possible first and second spans. Both the spans must be in the
	 * 	same document. In UnorderedElementDistanceSpans, a span that is not in 
	 * 	an element, is not added to its candidate list. The element must also be in
	 * 	the same document.
	 *   
	 * 	@return true iff at least one of the candidate lists can be filled.
	 * */
	protected abstract boolean fillEmptyCandidateLists() throws IOException;
	
	/** Set the list of matches between the span having the smallest position, and 
	 * 	its candidates. Simply remove the span if it does not have any candidates.
	 * */
	protected void setMatchList() throws IOException {
		
		hasMoreFirstSpans = setCandidateList(firstSpanList,firstSpans,
				hasMoreFirstSpans,secondSpanList);
		hasMoreSecondSpans = setCandidateList(secondSpanList,secondSpans,
				hasMoreSecondSpans,firstSpanList);
		
		CandidateSpan currentFirstSpan, currentSecondSpan;
		if (!firstSpanList.isEmpty() && !secondSpanList.isEmpty()){
			
			currentFirstSpan = firstSpanList.get(0)	;
			currentSecondSpan = secondSpanList.get(0);
			
			if (currentFirstSpan.getEnd() <= currentSecondSpan.getEnd()){
				matchList = findMatches(currentFirstSpan, secondSpanList);
				updateList(firstSpanList);				
			}
			else {
				matchList = findMatches(currentSecondSpan, firstSpanList);
				updateList(secondSpanList);				
			}
		}
		else if (firstSpanList.isEmpty()){
			updateList(secondSpanList);			
		}
		else{ 
			updateList(firstSpanList);			
		}
	}
	
	protected abstract void updateList(List<CandidateSpan> candidateList);
	
	/** Set the candidate list for the first element in the target list.
	 * @return true iff the spans enumeration still has a next element 
	 * to be a candidate
	 */
	protected abstract boolean setCandidateList(List<CandidateSpan> 
			candidateList, Spans candidate, boolean hasMoreCandidates,
			List<CandidateSpan> targetList) throws IOException;
	
	/** Search all matches between the target span and its candidates in the candidate 
	 * 	list.
	 * 	@return the matches in a list 
	 * */
	protected abstract List<CandidateSpan> findMatches(CandidateSpan target, 
			List<CandidateSpan> candidateList);
	
	/** Compute match properties and create a candidate span match 
	 * 	to be added to the match list.
	 * 	@return a candidate span match 
	 * */
	protected CandidateSpan createMatchCandidate(CandidateSpan target,
			CandidateSpan cs, boolean isDistanceZero) {
		
		int start = Math.min(target.getStart(), cs.getStart());
		int end = Math.max(target.getEnd(),cs.getEnd());
		int doc = target.getDoc();
		long cost = target.getCost() + cs.getCost();
		
		Collection<byte[]> payloads = new LinkedList<byte[]>();
		if (collectPayloads) {
			if (target.getPayloads() != null){
				payloads.addAll(target.getPayloads());
			}
			if (cs.getPayloads() != null){
				payloads.addAll(cs.getPayloads());
			}
		}
		return new CandidateSpan(start,end,doc,cost,payloads);
	}


	/** Assign the first candidate span in the match list as the current span match.
	 * */
	private void setMatchProperties() {
		CandidateSpan cs = matchList.get(0);
		matchDocNumber = cs.getDoc();
		matchStartPosition = cs.getStart();
		matchEndPosition = cs.getEnd();
		matchCost = cs.getCost();
		matchPayload.addAll(cs.getPayloads());
		matchList.remove(0);
				
		log.trace("Match doc#={} start={} end={}", matchDocNumber,
				matchStartPosition,matchEndPosition);
	}

	@Override
	public boolean skipTo(int target) throws IOException {
		if (hasMoreSpans && (secondSpans.doc() < target)){
  			if (!secondSpans.skipTo(target)){
  				hasMoreSpans = false;
  				return false;
  			}  			
  		}
		
		firstSpanList.clear();
		secondSpanList.clear();
		matchPayload.clear();
		isStartEnumeration=false;
		return advance();		
	}

	@Override
	public long cost() {
		return matchCost;
	}

}
