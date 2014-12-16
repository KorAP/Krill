package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.SpanRelationPartQuery;

/** This span enumeration returns the right part of relation spans 
 * 	whose left part token/element positions matching the second spans, 
 * 	or vice versa.
 * 	
 * 	Relations within a certain window, e.g element-based or token-
 * 	distance-based, are sorted to resolve reference within that window.
 * 	Resolution is limited only within an window. 
 * 
 * 	@author margaretha
 * */
public class RelationPartSpans extends RelationBaseSpans{
	
	private RelationBaseSpans relationSpans;
	private SpansWithId matcheeSpans;
	private ElementSpans element;
	private List<CandidateRelationSpan> candidateRelations;
	
	private boolean matchRight;
	private boolean inverse;
	private boolean hasMoreMatchees;
	
//	private short leftId, rightId;
	private int window;
	
	public RelationPartSpans(SpanRelationPartQuery query,	
			AtomicReaderContext context, Bits acceptDocs,
			Map<Term, TermContext> termContexts) throws IOException {
		super(query, context, acceptDocs, termContexts);
		if (query.getElementQuery() != null){
			element = (ElementSpans) query.getElementQuery().getSpans(context, acceptDocs, 
					termContexts);
		}
		else{
			window = query.getWindow();
		}
		relationSpans = (RelationBaseSpans) firstSpans;
		matcheeSpans = (SpansWithId) secondSpans;		
		// hack
		matcheeSpans.hasSpanId = true;
		
		hasMoreMatchees = matcheeSpans.next();
  		hasMoreSpans = relationSpans.next() && hasMoreMatchees;
  		if (element != null){
  			hasMoreSpans &= element.next();
  		}
		candidateRelations = new ArrayList<CandidateRelationSpan>();
		matchRight = query.isMatchRight();
		inverse = query.isInverseRelation();
	}

	@Override
	public boolean next() throws IOException {
		isStartEnumeration=false;
  		matchPayload.clear();
		return advance();
	}
	
	protected boolean advance() throws IOException {
		while (candidateRelations.size() > 0 || hasMoreSpans){
			if (candidateRelations.size() > 0){
				setMatchSpan(candidateRelations.get(0));
				candidateRelations.remove(0);
				return true;
			}
			else if (element != null){
				setCandidateList();
			}
			else {	setCandidateListWithWindow(); }
		}		
		return false;
	}
	
	private void setMatchSpan(CandidateRelationSpan relationSpan) {
		matchDocNumber = relationSpan.getDoc();
		if (!inverse){
			matchStartPosition = relationSpan.getStart();
			matchEndPosition = relationSpan.getEnd();
			setRightStart(relationSpan.getRightStart());
			setRightEnd(relationSpan.getRightEnd());
		}					
		else{ // maybe a bit confusing -- inverse relation
			matchStartPosition = relationSpan.getRightStart();
			matchEndPosition = relationSpan.getRightEnd();
			setRightStart(relationSpan.getStart());
			setRightEnd(relationSpan.getEnd());
		}
		
		setLeftId(relationSpan.getLeftId());
		setRightId(relationSpan.getRightId());
		setSpanId(relationSpan.getSpanId());
	}
	
	/** A window starts at the same token position as a relation span, 
	 * 	and ends at the start + window length.
	 * */
	private void setCandidateListWithWindow() throws IOException {
		if (hasMoreSpans && ensureSameDoc(relationSpans, matcheeSpans) ){
			int windowEnd = relationSpans.start() + window;
			if (relationSpans.end() > windowEnd){
				throw new IllegalArgumentException("The window length "+window
					+" is too small. The relation span ("+relationSpans.start()+
					","+relationSpans.end()+") is longer than " +"the window " +
					"length.");
			}
			else {
				collectRelations(relationSpans.doc(), windowEnd);
				// sort results
				Collections.sort(candidateRelations);
			}
		}
	}

	private void setCandidateList() throws IOException {
		while (hasMoreSpans && findSameDoc(element, relationSpans, matcheeSpans) ){
			// if the relation is within a sentence
			if (relationSpans.start() >= element.start() && 
					relationSpans.end() <= element.end()){
				collectRelations(element.doc(),element.end());
				// sort results
				Collections.sort(candidateRelations);
			}			
			else if (relationSpans.end() < element.end()){
				hasMoreSpans = relationSpans.next();
			}
			else {
				hasMoreSpans = element.next();
			}
		}		
	}

	/** Collect all relations within an element whose left side matching the secondspans.
	 * */
	private void collectRelations(int currentDoc, int windowEnd) throws IOException {			
		List<CandidateRelationSpan> temp = new ArrayList<CandidateRelationSpan>();
		boolean sortRight = false;
		if (matchRight) sortRight = true;
		// collect all relations within an element	
		while (hasMoreSpans &&
				relationSpans.doc() == currentDoc &&
				relationSpans.end() <= windowEnd){
			temp.add(new CandidateRelationSpan(relationSpans,sortRight));
			hasMoreSpans = relationSpans.next();
		}
		
		if(matchRight) Collections.sort(temp);
		
		// do the matching for each relation
		int i=0;
		CandidateRelationSpan r;
		while (hasMoreMatchees && i < temp.size()){
			r = temp.get(i);
			if (matchRight){
				/*System.out.println(r.getStart()+","+r.getEnd()+" "+
						r.getRightStart()+","+r.getRightEnd()+
						" #"+r.getRightId()+
						" "+matcheeSpans.start()+","+matcheeSpans.end()+
						" #"+matcheeSpans.getSpanId()
				);*/
				i = matchRelation(i, r,r.getRightStart(), r.getRightEnd());
			}
			else{
				/*System.out.println(r.getStart()+","+r.getEnd()+" "+
						r.getRightStart()+","+r.getRightEnd()+" "
						+matcheeSpans.start()+","+matcheeSpans.end()+
						" #"+matcheeSpans.getSpanId());*/
				i = matchRelation(i, r,r.getStart(), r.getEnd());
			}			
		}
		
		hasMoreSpans &= hasMoreMatchees;
	}
	
	private int matchRelation(int i, CandidateRelationSpan r, int startPos, 
			int endPos) throws IOException {
		
		if(startPos == matcheeSpans.start() ){
			if 	(endPos == matcheeSpans.end()){
				
				int id;
				if ( matcheeSpans instanceof RelationPartSpans){
					if (matchRight) {
						id = ((RelationPartSpans) matcheeSpans).getRightId();
					}
					else { id = ((RelationPartSpans) matcheeSpans).getLeftId(); }				
				}
				else { id = matcheeSpans.getSpanId(); }
				
				if (!inverse && r.getRightId() == id){
					r.sortRight = false;
					candidateRelations.add(r);
				}
				else if (inverse && r.getLeftId() == id) {
					r.sortRight = true;
					candidateRelations.add(r);
				}
				i++;
			}
			else if (endPos <= matcheeSpans.end()){
				i++;
			}
			else { hasMoreMatchees = matcheeSpans.next(); }
		}
		else if (startPos < matcheeSpans.start()){
			i++;
		}				
		else { hasMoreMatchees = matcheeSpans.next(); }	
		return i;
	}

	@Override
	public boolean skipTo(int target) throws IOException {
		if (hasMoreSpans && (relationSpans.doc() < target)){
  			if (!relationSpans.skipTo(target)){
  				candidateRelations.clear();
  				return false;
  			}
  		}		
		setCandidateList();
		matchPayload.clear();
		isStartEnumeration=false;
		return advance();	
	}

	@Override
	public long cost() {
		// TODO Auto-generated method stub
		return 0;
	}

	class CandidateRelationSpan extends CandidateSpan implements Comparable<CandidateSpan>{
		
		private int rightStart, rightEnd; 
		private short leftId, rightId;
		private boolean sortRight;
		
		
		public CandidateRelationSpan(RelationBaseSpans span, boolean sortRight) 
				throws IOException {
			super(span);
			this.rightStart = span.getRightStart();
			this.rightEnd = span.getRightEnd();
			this.sortRight = sortRight;
			this.leftId = span.getLeftId();
			this.rightId = span.getRightId();
			this.spanId = span.getSpanId();
		}
		
		@Override
		public int compareTo(CandidateSpan o) {
			CandidateRelationSpan cs = (CandidateRelationSpan) o;
			if (sortRight)
				return sortByRight(cs);
			
			return super.compareTo(o);
		}
		
		private int sortByRight(CandidateRelationSpan cs) {
			if (this.getRightStart() == cs.getRightStart()){
				if (this.getRightEnd() == cs.getRightEnd())
					return 0;
				if (this.getRightEnd() > cs.getRightEnd() )
					return 1;
				else return -1;
			}
			else if (this.getRightStart() < cs.getRightStart())
				return -1;
			else return 1;	
		}
		
		/*private void sortByLeft(CandidateSpan o) {
			super.compareTo(o);
		}*/

		public int getRightStart() {
			return rightStart;
		}

		public void setRightStart(int rightStart) {
			this.rightStart = rightStart;
		}

		public int getRightEnd() {
			return rightEnd;
		}

		public void setRightEnd(int rightEnd) {
			this.rightEnd = rightEnd;
		}

		public short getLeftId() {
			return leftId;
		}

		public void setLeftId(short leftId) {
			this.leftId = leftId;
		}

		public short getRightId() {
			return rightId;
		}

		public void setRightId(short rightId) {
			this.rightId = rightId;
		}	
	}
}
