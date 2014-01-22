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

import de.ids_mannheim.korap.query.SimpleSpanQuery;
import de.ids_mannheim.korap.query.SpanDistanceQuery;

public class DistanceSpan extends Spans{
	
	private boolean isStartEnumeration;
	private boolean hasMoreSpans;
	private boolean hasMoreFirstSpans;	
	private boolean collectPayloads;
	private int minDistance,maxDistance;
		
	protected int doc, start, end;	
	private List<byte[]> payload;   
    
    private SpanDistanceQuery query;
    protected Spans firstSpans, secondSpans;   
	private List<CandidateSpan> candidateList;
	private int candidateListIndex;
	private int candidateListDocNum;
	
    private Logger log = LoggerFactory.getLogger(SimpleSpans.class);
    
	public DistanceSpan(SpanDistanceQuery spanDistanceQuery,
			AtomicReaderContext context, Bits acceptDocs,
			Map<Term, TermContext> termContexts,
			int minDistance,
			int maxDistance)
			throws IOException {
		this(spanDistanceQuery, context, acceptDocs, termContexts, 
			minDistance, maxDistance, true);		
	}
	
	public DistanceSpan(SpanDistanceQuery spanDistanceQuery,
			AtomicReaderContext context, Bits acceptDocs,
			Map<Term, TermContext> termContexts,
			int minDistance,
			int maxDistance,
			boolean collectPayloads)
			throws IOException {
		
		this.query = spanDistanceQuery;
		this.minDistance = minDistance;
		this.maxDistance = maxDistance;		
  		this.collectPayloads = collectPayloads; // TODO: always true ?
  		this.payload = new LinkedList<byte[]>();
  		this.doc = -1;
  		this.start = -1;
  		this.end = -1;
		  		
  		// Get the enumeration of the two spans to match
  		firstSpans = spanDistanceQuery.getFirstClause().
  			getSpans(context, acceptDocs, termContexts);
  		secondSpans = spanDistanceQuery.getSecondClause().
  			getSpans(context, acceptDocs, termContexts);  	  	
  		 		  		
  		hasMoreFirstSpans = firstSpans.next();
  		hasMoreSpans = hasMoreFirstSpans;
  		
		candidateList = new ArrayList<>();
		candidateListIndex = -1;
		candidateListDocNum = firstSpans.doc();
		
		isStartEnumeration=true;
	}	
	
	@Override
	public boolean next() throws IOException {		
		isStartEnumeration=false;
  		payload.clear();
		return advance();
	}
	
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
	
 	private void setCandidateList() throws IOException{
 		if (candidateListDocNum == secondSpans.doc()){						
			copyPossibleCandidates();
			addNewCandidates();
			candidateListIndex = -1;
 		}
 		else {
 			candidateList.clear(); 			
 			if (hasMoreFirstSpans && ensureSameDoc()){
 				candidateListDocNum = firstSpans.doc();
				addNewCandidates();
				candidateListIndex = -1;
			}		
		} 		
	}
	
	private void copyPossibleCandidates(){
		List<CandidateSpan> temp = new ArrayList<>();
		for (CandidateSpan cs : candidateList){
			if (cs.getEnd()+maxDistance > secondSpans.start())
				temp.add(cs);
		}
		candidateList = temp;
	}
	
	private void addNewCandidates() throws IOException{
		while ( hasMoreFirstSpans && 
				firstSpans.doc() == candidateListDocNum &&
				firstSpans.start() < secondSpans.end()){
			
			if (firstSpans.end()+maxDistance > secondSpans.start())
				candidateList.add(new CandidateSpan(firstSpans));
			
			hasMoreFirstSpans = firstSpans.next();
		}
	}
	
	
	
	/** Skip the current first or second span until both the spans are in the same doc.
	 * @return true iff the first and second spans are in the same doc.
	 * */
  	private boolean ensureSameDoc() throws IOException {  		
  		while (firstSpans.doc() != secondSpans.doc()) {
  			if (firstSpans.doc() < secondSpans.doc()){
  				if (!firstSpans.skipTo(secondSpans.doc())){
  					hasMoreSpans = false;  					
  					return false;
  				}				
  			}		
  			else {
  				if (!secondSpans.skipTo(firstSpans.doc())){
  					hasMoreSpans = false;
  					return false;
  				}	
  			}			
  		}  		
  		return true;
  	}


	protected boolean findMatch() throws IOException {
		CandidateSpan candidateSpan = candidateList.get(candidateListIndex);		
		if (minDistance == 0 &&
				// intersection
				candidateSpan.getStart() < secondSpans.end() && 
				secondSpans.start() < candidateSpan.getEnd()){
			
			this.start = Math.min(candidateSpan.getStart(), secondSpans.start());
			this.end = Math.max(candidateSpan.getEnd(), secondSpans.end());
			setDocAndPayload(candidateSpan);
			return true;			
		}
		
		int actualDistance = secondSpans.start() - candidateSpan.getEnd() +1;
		if (candidateSpan.getStart() < secondSpans.start() &&
				minDistance <= actualDistance && 
				actualDistance <= maxDistance){
						
			this.start = candidateSpan.getStart();
			this.end = secondSpans.end();
			setDocAndPayload(candidateSpan);
			return true;
		}		
		return false;
	}
	
	private void setDocAndPayload(CandidateSpan candidateSpan) throws IOException{ 
		this.doc = secondSpans.doc();		
		if (collectPayloads){			
  		    if (candidateSpan.getPayloads() != null) {  		    	
  		    	payload.addAll(candidateSpan.getPayloads());
  		    	log.trace("first",payload.size());
  		    }
  		    if (secondSpans.isPayloadAvailable()) {
  		    	payload.addAll(secondSpans.getPayload());
  		    	log.trace("second",payload.size());
  		    }
		} 
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
		payload.clear();
		isStartEnumeration=false;
		return advance();
	}

	@Override
	public int doc() {		
		return this.doc;
	}

	@Override
	public int start() {
		return this.start;
	}

	@Override
	public int end() {
		return this.end;
	}

	@Override
	public Collection<byte[]> getPayload() throws IOException {
		return this.payload;
	}

	@Override
	public boolean isPayloadAvailable() throws IOException {		
		return !this.payload.isEmpty();
	}

	@Override
	public long cost() {
		CandidateSpan candidateSpan = candidateList.get(candidateListIndex);
		return candidateSpan.getCost() + secondSpans.cost();
	}
}
