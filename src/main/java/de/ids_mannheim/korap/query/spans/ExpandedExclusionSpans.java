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

import de.ids_mannheim.korap.query.SpanExpansionQuery;

public class ExpandedExclusionSpans extends SimpleSpans{
	
	private int min, max;
	private int direction;
	private List<CandidateSpan> candidateSpans;
	private boolean hasMoreNotClause;
	private Spans notClause;
	
	public ExpandedExclusionSpans(SpanExpansionQuery spanExpansionQuery,
			AtomicReaderContext context, Bits acceptDocs,
			Map<Term, TermContext> termContexts) throws IOException {
		super(spanExpansionQuery, context, acceptDocs, termContexts);
		
		if (spanExpansionQuery.getSecondClause() == null){
			throw new IllegalArgumentException("The SpanExpansionQuery " +
				"is not valid. The spanquery to exclude (notClause) cannot " +
				"be null.");
		}
		
		/*if (spanExpansionQuery.getMin() < 1){
			throw new IllegalArgumentException("Min occurrence for notClause " +
				"must be at least 1.");
		}*/
		
		this.min = spanExpansionQuery.getMin();
		this.max = spanExpansionQuery.getMax();
		this.direction = spanExpansionQuery.getDirection();
		
		this.notClause = secondSpans;
		this.hasMoreNotClause = notClause.next();		
		
		candidateSpans = new ArrayList<CandidateSpan>();		
		hasMoreSpans = firstSpans.next();
	}

	@Override
	public boolean next() throws IOException {
		matchPayload.clear();
		isStartEnumeration = false;
		return advance();
	}

	private boolean advance() throws IOException {
		while (hasMoreSpans || candidateSpans.size() > 0){
			if (candidateSpans.size() > 0){				
				CandidateSpan cs = candidateSpans.get(0);
				matchDocNumber = cs.getDoc();
				matchStartPosition = cs.getStart();
				matchEndPosition = cs.getEnd();
				matchPayload = cs.getPayloads();
				candidateSpans.remove(0);
				return true;
			}
			else if (!hasMoreNotClause || notClause.doc() > firstSpans.doc()){
				generateCandidates(min, max, direction);
				hasMoreSpans = firstSpans.next();				
			}
			else findMatches();
		}
		return false;
	}
	
	private void findMatches() throws IOException {
		while (hasMoreNotClause &&	notClause.doc() <= firstSpans.doc()){
			
			if (notClause.doc() == firstSpans.doc()){ 
				
				if (direction < 0 ){ // left
					int counter = max;
					int maxPos = max;
					CandidateSpan lastNotClause = null;
					while (hasMoreNotClause && 
							notClause.start() < firstSpans.start()){
						
						// between max and firstspan.start()
						if (notClause.start() >= firstSpans.start() - counter){
							maxPos = firstSpans.start() - notClause.start() -1;
							lastNotClause = new CandidateSpan(notClause);
							counter--;
						} 
						if (!notClause.next()) hasMoreNotClause = false;
					}									
					
					// if a notClause is between max and firstspan.start, 
					// then maxPos = last NotClause pos -1
					generateCandidates(min, maxPos, direction);
					
					if (lastNotClause != null)
						while ((hasMoreSpans = firstSpans.next())
								// the next notClause is not in between max and firstspan.start()
								&& notClause.start() > firstSpans.start()
								// the last notClause is in between max and firstspan.start()
								&& lastNotClause.getStart() < firstSpans.start()								
								&& lastNotClause.getStart() >= firstSpans.start() - max
							){
														
							maxPos = firstSpans.start() - lastNotClause.getStart() -1;
							generateCandidates(min, maxPos, direction);
						}
					else hasMoreSpans = firstSpans.next();
				}
				
				else { // right
					
					int expansionEnd = firstSpans.end() + max;
					int maxPos = max;
					boolean isFound = false;
					
					CandidateSpan firstNotClause = null;
					//System.out.println("main start:"+firstSpans.start());
					while (hasMoreNotClause && notClause.start() < expansionEnd){
						// between firstspan.end() and expansionEnd
						if (!isFound && notClause.start() >= firstSpans.start()){							
							maxPos = notClause.start() - firstSpans.start() -1;
							firstNotClause = new CandidateSpan(notClause);
							isFound = true;
						}						
						if (!notClause.next()) hasMoreNotClause = false;
					}
					// if a notClause is between firstSpan.end and max
					// then maxPos = the first notClause pos -1 
					generateCandidates(min, maxPos, direction);
					
					if (firstNotClause !=null){
						while ((hasMoreSpans = firstSpans.next()) 
								// in between
								&& firstNotClause.getStart() < firstSpans.end() + max
								&& firstNotClause.getStart() >= firstSpans.start())								
							{
							//System.out.println("first start:"+firstNotClause.getStart()+", main start:"+firstSpans.start());
							maxPos = firstNotClause.getStart() - firstSpans.start() -1;
							generateCandidates(min, maxPos, direction);
						} 
					}
					else hasMoreSpans = firstSpans.next();
				}
				
				
				break;
			}
			
			else if (!notClause.next()) hasMoreNotClause = false;
		}
	}
	
	private void generateCandidates(int minPos, int maxPos, int direction) 
			throws IOException {
		int counter;
		int start, end;
				
		if (direction < 0 ) { // left
			counter = maxPos;
			while (counter >= min){
				start = firstSpans.start() - counter;
				if (start > -1 ){
					
				end = firstSpans.end();
				//System.out.println(start+","+end);
				candidateSpans.add(new CandidateSpan(start, end, firstSpans.doc(), 
						firstSpans.cost(), firstSpans.getPayload()));
				}
				counter --;
			}
		}
		else { // right
			counter = minPos;
			while(counter <= maxPos){
				start = firstSpans.start();
				end = firstSpans.end() + counter;
				//System.out.println(start+","+end);
				candidateSpans.add(new CandidateSpan(start, end, firstSpans.doc(), 
						firstSpans.cost(), firstSpans.getPayload()));
				counter++;
			}			
		}
	}
	
	
	@Override
	public boolean skipTo(int target) throws IOException {
		if (hasMoreSpans && (firstSpans.doc() < target)){
  			if (!firstSpans.skipTo(target)){
  				hasMoreSpans = false;
  				return false;
  			}
  		} 		
  		matchPayload.clear();
  		return advance();
	}

	@Override
	public long cost() {
		// TODO Auto-generated method stub
		return 0;
	}
}
