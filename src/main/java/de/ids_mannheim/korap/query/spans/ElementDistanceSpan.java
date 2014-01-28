package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.SpanDistanceQuery;

/** Enumeration of element-based distance span matches. 
 * 	The distance unit is an element position, including a gap.
 * 	For example:  
 * 	X &lt;p&gt;&lt;/p&gt;
 * 	Z
 * 	&lt;p&gt;&lt;/p&gt; Y. 
 * 	X and Y has a distance of 4.
 * 
 *	@author margaretha
 *	@deprecated 
 * */
public class ElementDistanceSpan extends DistanceSpan {
			
	private boolean hasMoreElements;
	private Spans elements;	
	private int elementPosition, secondSpanPostion;	
		
	private boolean isGap; // refers to the gap right before the current element
	private int gapPosition;
	
	public ElementDistanceSpan(SpanDistanceQuery query,
			AtomicReaderContext context, Bits acceptDocs,
			Map<Term, TermContext> termContexts)
			throws IOException {
		super(query, context, acceptDocs, termContexts);
  		
  		elements = query.getElementQuery().
  			getSpans(context, acceptDocs, termContexts);
  		
  		hasMoreElements = elements.next();
  		hasMoreSpans = hasMoreFirstSpans && hasMoreElements;
  		elementPosition=0;  		
	}

	@Override
	protected boolean findMatch() throws IOException {
		CandidateSpan candidateSpan = candidateList.get(candidateListIndex);
		int actualDistance = secondSpanPostion - candidateSpan.getPosition();
		
		// In the same element
		if (minDistance == 0 && actualDistance == 0){			
			setMatchProperties(candidateSpan, true);
			return true;			
		}		
		
		if (minDistance <= actualDistance && actualDistance <= maxDistance){
			setMatchProperties(candidateSpan, false);
			return true;
		}
		
		return false;
	}
	
	@Override
	protected void setCandidateList() throws IOException{
 		if (candidateListDocNum == elements.doc() && 
 				candidateListDocNum == secondSpans.doc()){
			addNewCandidates();
			candidateListIndex = -1;
 		}
 		else {
 			candidateList.clear(); 			
 			if (hasMoreFirstSpans && findSameDoc()){
 				candidateListDocNum = firstSpans.doc();
 				elementPosition=0;
				addNewCandidates();
				candidateListIndex = -1;
			}		
		} 		
	}
	
	/** Add new possible candidates
	 * */
	private void addNewCandidates() throws IOException{
		int position;
		while ( hasMoreFirstSpans && 
				firstSpans.doc() == candidateListDocNum &&
				firstSpans.start() < secondSpans.end()){
			
			advanceElementTo(firstSpans);
			position = findPosition(firstSpans);
			candidateList.add(new CandidateSpan(firstSpans,position));
			filterCandidateList(position);
			hasMoreFirstSpans = firstSpans.next();
		}
		
		secondSpanPostion = findSecondSpanPosition();
		filterCandidateList(secondSpanPostion);
	}
	
	/** Advance elements until encountering the span. 
	 * 	Add the elementPosition while searching.
	 *  Add gap positions too. 
	 */
	private void advanceElementTo(Spans span) throws IOException{	
		isGap = false;
		int prevElementEnd = elements.end();
		while (hasMoreElements && 
				elements.doc() == candidateListDocNum &&
				elements.end() <= span.start()){			
			
			// Find a gap
			if (prevElementEnd < elements.start()){
				elementPosition++;
			}
			
			prevElementEnd = elements.end();
			hasMoreElements = elements.next();
			elementPosition++;
		}
		
		// Find the last gap between the prevElement and current element
		if (hasMoreElements && 
				elements.doc() == candidateListDocNum &&	
				prevElementEnd < elements.start()){			
			isGap = true;			
			gapPosition = elementPosition;
			elementPosition++;			
		}
		
	}

	/** Find a span position which can be
	 * 		in the current element,
	 *  	in the gap between the previous and current element,
	 *  	in the very last gap at the end of a doc.
	 * @return the span position    
	 * */
	private int findPosition(Spans span){
		int position = elementPosition;
		
		// in the gap
		if (isGap && span.end() <= elements.start())
			position = gapPosition;
		
		return position;
	}
	
	
	private int findSecondSpanPosition() throws IOException{		
		// in the gap
		if (isGap && secondSpans.end() <= elements.start()){
			return gapPosition;					
		}
		// in current element
		else if (secondSpans.end() <= elements.end()){
			return elementPosition;
		}					
		advanceElementTo(secondSpans);	
		return findPosition(secondSpans);		
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
		//System.out.println("pos "+position+" " +candidateList.size());
	}		

	/** Find the same doc shared by element, firstspan and secondspan.
	 *  @return true iff such a doc is found.
	 * */
	private boolean findSameDoc() throws IOException{
		
		while (hasMoreSpans) {
			if (ensureSameDoc(firstSpans, secondSpans) &&
					elements.doc() == firstSpans.doc()){
				return true;
			}			
			if (!ensureSameDoc(elements,secondSpans)){
				return false;
			};
		}		
  		return false;
	}
	
	@Override
	public long cost() {
		CandidateSpan candidateSpan = candidateList.get(candidateListIndex);
		return elements.cost() + candidateSpan.getCost() + secondSpans.cost();
	}
}
