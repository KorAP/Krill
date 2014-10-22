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

import de.ids_mannheim.korap.query.SpanRelationWithVariableQuery;

/** This span enumeration returns the right side of relation spans 
 * 	whose left side token/element positions matching the second spans. 
 * 
 * 	@author margaretha
 * */
public class RelationSpansWithVariable extends SimpleSpans{
	
	private RelationSpans relationSpans;
	private ElementSpans element;
	private List<CandidateRelationSpan> candidateRelations;
	
	private boolean matchRight;
	private boolean hasMoreSecondSpans;
	
	public RelationSpansWithVariable(SpanRelationWithVariableQuery query,	
			AtomicReaderContext context, Bits acceptDocs,
			Map<Term, TermContext> termContexts) throws IOException {
		super(query, context, acceptDocs, termContexts);		
		element = (ElementSpans) query.getRoot().getSpans(context, acceptDocs, 
				termContexts);
		relationSpans = (RelationSpans) firstSpans;
		hasMoreSecondSpans = secondSpans.next();
  		hasMoreSpans = element.next() && relationSpans.next() && hasMoreSecondSpans;
		candidateRelations = new ArrayList<CandidateRelationSpan>();
		
		matchRight = query.isMatchRight();
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
				CandidateRelationSpan relationSpan = candidateRelations.get(0);				
				matchDocNumber = relationSpan.getDoc();
				if (matchRight){
					matchStartPosition = relationSpan.getStart();
					matchEndPosition = relationSpan.getEnd();
				}					
				else{
					matchStartPosition = relationSpan.getRightStart();
					matchEndPosition = relationSpan.getRightEnd();
				}				
				candidateRelations.remove(0);
				return true;
			}
			else {	setCandidateList(); }
		}		
		return false;
	}

	private void setCandidateList() throws IOException {
		while (hasMoreSpans && findSameDoc(element, relationSpans, secondSpans) ){
			// if the relation is within a sentence
			if (relationSpans.start() >= element.start() && 
					relationSpans.end() <= element.end()){
				collectRelations();
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
	private void collectRelations() throws IOException {			
		List<CandidateRelationSpan> temp = new ArrayList<CandidateRelationSpan>();
		boolean sortRight = false;
		if (matchRight) sortRight = true;
		// collect all relations within an element	
		while (hasMoreSpans &&
				relationSpans.doc() == element.doc() &&
				relationSpans.end() <= element.end()){
			temp.add(new CandidateRelationSpan(relationSpans,sortRight));
			hasMoreSpans = relationSpans.next();
		}
		
		if(matchRight) Collections.sort(temp);
		
		// do the matching for each relation
		int i=0;
		CandidateRelationSpan r;
		while (hasMoreSecondSpans && i < temp.size()){
			r = temp.get(i);
			if (matchRight){
				System.out.println(i+" "+r.getStart()+","+r.getEnd()+" "+
						r.getRightStart()+","+r.getRightEnd()+
						" "+secondSpans.start()+","+secondSpans.end());
				i = matchRelation(i, r,r.getRightStart(), r.getRightEnd());
			}
			else{
				System.out.println(i+" "+r.getStart()+","+r.getEnd()+" "+
						r.getRightStart()+","+r.getRightEnd()+" "
						+secondSpans.start()+","+secondSpans.end());
				i = matchRelation(i, r,r.getStart(), r.getEnd());
			}			
		}
		
		hasMoreSpans &= hasMoreSecondSpans;
	}
	
	private int matchRelation(int i, CandidateRelationSpan r, int startPos, int endPos) throws IOException {
		if(startPos == secondSpans.start() ){
			if 	(endPos == secondSpans.end()){		
				if (matchRight) r.sortRight = false;
				else r.sortRight = true;
				
				candidateRelations.add(r);
				i++;
			}
			else if (endPos <= secondSpans.end()){
				i++;
			}
			else { hasMoreSecondSpans = secondSpans.next(); }
		}
		else if (startPos < secondSpans.start()){
			i++;
		}				
		else { hasMoreSecondSpans = secondSpans.next(); }	
		return i;
	}

	@Override
	public boolean skipTo(int target) throws IOException {
		if (hasMoreSpans && (firstSpans.doc() < target)){
  			if (!firstSpans.skipTo(target)){
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
		private boolean sortRight;
		public CandidateRelationSpan(RelationSpans span, boolean sortRight) 
				throws IOException {
			super(span);
			this.rightStart = span.getRightStart();
			this.rightEnd = span.getRightEnd();
			this.sortRight = sortRight;
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
	}
}
