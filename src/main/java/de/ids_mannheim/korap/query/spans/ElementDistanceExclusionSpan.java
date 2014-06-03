package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.SpanDistanceQuery;

/** Span enumeration of the first spans which do NOT occur together 
 * 	with the second spans within a range of some element-based distance 
 * 	(sentence or paragraph). Note: The element distance unit does not 
 * 	overlap to each other.
 * 
 * 	@author margaretha
 * */
public class ElementDistanceExclusionSpan extends DistanceSpans{

	private Spans elements;
	private boolean hasMoreElements;
	private int elementPosition;
	
	private boolean isOrdered;
	private boolean hasMoreSecondSpans;
		
	protected List<CandidateSpan> candidateList, targetList;	
	private int currentDocNum;
	
	private int minDistance, maxDistance;
	private int firstSpanPostion;

        public static final boolean DEBUG = false;

	public ElementDistanceExclusionSpan(SpanDistanceQuery query,
			AtomicReaderContext context, Bits acceptDocs,
			Map<Term, TermContext> termContexts, boolean isOrdered) 
			throws IOException {
		super(query, context, acceptDocs, termContexts);
		
		elements = query.getElementQuery().
	  			getSpans(context, acceptDocs, termContexts);	  		
  		hasMoreElements = elements.next();  		
  		hasMoreSpans = firstSpans.next() && hasMoreElements;  		  		
  		hasMoreSecondSpans = secondSpans.next();
  		
  		elementPosition=0;
  		this.isOrdered = isOrdered;
  		candidateList = new ArrayList<CandidateSpan>();
  		targetList = new ArrayList<CandidateSpan>();
  		currentDocNum = firstSpans.doc();
  		
  		minDistance = query.getMinDistance();
		maxDistance = query.getMaxDistance();	
	}

	@Override
	protected boolean advance() throws IOException {
		while(!targetList.isEmpty() || (hasMoreSpans && ensureSameDoc(firstSpans, elements))){
			if (!targetList.isEmpty()){
				if (isTargetValid()) return true;
				else continue;
			}
			if (checkFirstSpan()) return true;			
		}
		return false;
	}
	
	private boolean isTargetValid() throws IOException{
		CandidateSpan target = targetList.get(0);
		targetList.remove(0);			
		firstSpanPostion = target.getPosition();
		filterCandidateList(firstSpanPostion);
		collectRightCandidates();
		
		if (isWithinDistance()){
			return false;
		}		
		setMatchProperties(target);
		return true;
	}
	
	private boolean checkFirstSpan() throws IOException{
		if (firstSpans.doc() != currentDocNum){
			currentDocNum = firstSpans.doc();
			candidateList.clear();
		}
		
		if (hasMoreSecondSpans) {
			if (secondSpans.doc() == firstSpans.doc()){ 
				return (findMatch() ? true : false);
			}			
			else if (secondSpans.doc() < firstSpans.doc()){
				hasMoreSecondSpans = secondSpans.skipTo(firstSpans.doc());
				return false;
			}
		}		
		return (isFirstSpanValid() ? true : false);
	}
	
	private boolean isFirstSpanValid() throws IOException{
		if (candidateList.isEmpty()){
			if (isFirstSpanInElement()){		
				setMatchProperties(new CandidateSpan(firstSpans,elementPosition));
				hasMoreSpans = firstSpans.next();
				return true;
			}
			hasMoreSpans = firstSpans.next();
			return false;		
		}
		return (findMatch() ? true : false);			
	}
	
	private boolean advanceElementTo(Spans span) throws IOException{
		while (hasMoreElements && 
				elements.doc() == currentDocNum &&
				elements.start() < span.end()){
			
			if (span.start() >= elements.start() &&
					span.end() <= elements.end()){
				return true;
			}			
			
			hasMoreElements = elements.next();
			elementPosition++;
		}
		return false;
	}

	private boolean findMatch() throws IOException {		
		if (!isOrdered) collectLeftCandidates();
		
		if (isFirstSpanInElement()){
			CandidateSpan target = new CandidateSpan(firstSpans,elementPosition);
			hasMoreSpans = firstSpans.next();
			// Checking the secondspans in the left side
			if (!isOrdered && isWithinDistance()) return false;
			// Checking the secondspans in the right side
			collectRightCandidates();
			if (isWithinDistance()) return false;
			
			setMatchProperties(target);
			return true;
		}
		hasMoreSpans = firstSpans.next();
		return false;
	}
	
	private void collectRightCandidates() throws IOException{
		while (hasMoreSecondSpans && secondSpans.doc() == currentDocNum){
			
			if (elementPosition > firstSpanPostion+maxDistance){
				break;
			}
			if (hasMoreSpans && firstSpans.start() < secondSpans.start() &&
					firstSpans.doc() == currentDocNum){
				if (advanceElementTo(firstSpans)){
					targetList.add(new CandidateSpan(firstSpans, elementPosition));
				}
				hasMoreSpans = firstSpans.next();
				continue;
			}
			
			if (advanceElementTo(secondSpans)){
				candidateList.add(new CandidateSpan(secondSpans,elementPosition));
			}
			hasMoreSecondSpans = secondSpans.next();
		}		
	}
	
	private void collectLeftCandidates() throws IOException{
		while(hasMoreSecondSpans && secondSpans.doc() == firstSpans.doc() &&
				secondSpans.start() < firstSpans.end()){
			if (advanceElementTo(secondSpans)){
				candidateList.add(new CandidateSpan(secondSpans,elementPosition));
				filterCandidateList(elementPosition);
			}
			hasMoreSecondSpans = secondSpans.next();
		}	
	}
	
	private boolean isWithinDistance(){
		int actualDistance; 
		for (CandidateSpan cs: candidateList){
			actualDistance = cs.getPosition() - firstSpanPostion;
			if (!isOrdered) actualDistance = Math.abs(actualDistance);
			
			if (minDistance <= actualDistance && actualDistance <= maxDistance)
				return true;
		}
		return false;
	}
	
	private boolean isFirstSpanInElement() throws IOException {
		if (advanceElementTo(firstSpans)){
			firstSpanPostion = elementPosition;
			filterCandidateList(firstSpanPostion);
			return true;
		}
		return false;
	}
	
	private void filterCandidateList(int position){
		
		Iterator<CandidateSpan> i = candidateList.iterator();
		CandidateSpan cs;
		while(i.hasNext()){
			cs = i.next();
			if (cs.getPosition() == position || 
					cs.getPosition()+maxDistance >= position){
				break;
			}			
			i.remove();
		}		
	}	
	
	private void setMatchProperties(CandidateSpan match) throws IOException{
		matchDocNumber = match.getDoc();
		matchStartPosition = match.getStart();
		matchEndPosition = match.getEnd();
		
  		if (collectPayloads && match.getPayloads() != null)
	    	matchPayload.addAll(match.getPayloads());
  		
  		setMatchFirstSpan(match);

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
		return advance();
	}

	@Override
	public long cost() {
		return elements.cost() + firstSpans.cost() + secondSpans.cost();
	}

}
