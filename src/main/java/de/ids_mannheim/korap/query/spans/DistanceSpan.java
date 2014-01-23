package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.util.Bits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ids_mannheim.korap.query.SpanDistanceQuery;

public class DistanceSpan extends SimpleSpans{	

	private boolean hasMoreFirstSpans;	
	private boolean collectPayloads;
	private int minDistance,maxDistance;
	
	private List<CandidateSpan> candidateList;
	private int candidateListIndex;
	private int candidateListDocNum;
	
    private Logger log = LoggerFactory.getLogger(DistanceSpan.class);
    
	public DistanceSpan(SpanDistanceQuery query,
			AtomicReaderContext context, Bits acceptDocs,
			Map<Term, TermContext> termContexts)
			throws IOException {		
		super(query, context, acceptDocs, termContexts);
		
		minDistance = query.getMinDistance();
		maxDistance = query.getMaxDistance();		
  		collectPayloads = query.isCollectPayloads();
  		 		  		
  		hasMoreFirstSpans = firstSpans.next();
  		hasMoreSpans = hasMoreFirstSpans;
  		
		candidateList = new ArrayList<>();
		candidateListIndex = -1;
		candidateListDocNum = firstSpans.doc();
	}	
	
	@Override
	public boolean next() throws IOException {		
		isStartEnumeration=false;
  		matchPayload.clear();
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

	protected boolean findMatch() throws IOException {
		CandidateSpan candidateSpan = candidateList.get(candidateListIndex);		
		if (minDistance == 0 &&
				// intersection
				candidateSpan.getStart() < secondSpans.end() && 
				secondSpans.start() < candidateSpan.getEnd()){
			
			matchStartPosition = Math.min(candidateSpan.getStart(), secondSpans.start());
			matchEndPosition = Math.max(candidateSpan.getEnd(), secondSpans.end());
			setDocAndPayload(candidateSpan);
			return true;			
		}
		
		int actualDistance = secondSpans.start() - candidateSpan.getEnd() +1;
		if (candidateSpan.getStart() < secondSpans.start() &&
				minDistance <= actualDistance && 
				actualDistance <= maxDistance){
						
			matchStartPosition = candidateSpan.getStart();
			matchEndPosition = secondSpans.end();
			setDocAndPayload(candidateSpan);
			return true;
		}		
		return false;
	}
	
	private void setDocAndPayload(CandidateSpan candidateSpan) throws IOException{ 
		this.matchDocNumber = secondSpans.doc();		
		if (collectPayloads){			
  		    if (candidateSpan.getPayloads() != null) {  		    	
  		    	matchPayload.addAll(candidateSpan.getPayloads());
  		    	log.trace("first",matchPayload.size());
  		    }
  		    if (secondSpans.isPayloadAvailable()) {
  		    	matchPayload.addAll(secondSpans.getPayload());
  		    	log.trace("second",matchPayload.size());
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
		matchPayload.clear();
		isStartEnumeration=false;
		return advance();
	}

	@Override
	public long cost() {
		CandidateSpan candidateSpan = candidateList.get(candidateListIndex);
		return candidateSpan.getCost() + secondSpans.cost();
	}
}
