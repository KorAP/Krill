package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.util.Bits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ids_mannheim.korap.query.SpanRepetitionQuery;

public class RepetitionSpans extends SimpleSpans{
	
	private int min,max;
	private long matchCost;
	private List<CandidateSpan> matchList;
	private Logger log = LoggerFactory.getLogger(RepetitionSpans.class);
        // This advices the java compiler to ignore all loggings
        public static final boolean DEBUG = false;


	public RepetitionSpans(SpanRepetitionQuery query,
			AtomicReaderContext context, Bits acceptDocs,
			Map<Term, TermContext> termContexts) 
			throws IOException {
		super(query, context, acceptDocs, termContexts);
		this.min = query.getMin();
		this.max = query.getMax();
		matchList = new ArrayList<CandidateSpan>();
		hasMoreSpans = firstSpans.next();
	}

	@Override
	public boolean next() throws IOException {
		isStartEnumeration = false;
		return advance();
	}

	private boolean advance() throws IOException {

		while (hasMoreSpans  || !matchList.isEmpty()){
			if (!matchList.isEmpty()){			
				setMatchProperties(matchList.get(0));
				matchList.remove(0);
				return true;
			}
			matchPayload.clear();
			matchCost = 0;
			setMatchList();
		}	
		
		return false;
	}
	
	private void setMatchList() throws IOException {		
		
		CandidateSpan startSpan = new CandidateSpan(firstSpans);
		if (min == 1 ) matchList.add(startSpan);
		
		if (max == 1) {
			hasMoreSpans = firstSpans.next();
		}
		else {
			CandidateSpan prevSpan = startSpan;
			Collection<byte[]> payload;
			int n = 2;		
			while (n <= max && 
					(hasMoreSpans = firstSpans.next()) &&
					startSpan.getDoc() == firstSpans.doc() ){
				if (firstSpans.start() > prevSpan.getEnd()){
					break;
				} 
				else if (min <= n){
					if (firstSpans.isPayloadAvailable()){
						payload = firstSpans.getPayload();
					} else {payload = null;}
						
					matchCost += firstSpans.cost(); 
					matchList.add(new CandidateSpan(
							startSpan.getStart(),
							firstSpans.end(), 
							firstSpans.doc(), 
							matchCost,
							payload)
					);				
				}
				prevSpan = new CandidateSpan(firstSpans);
				n++;
			}
		}
	}
	

	private void setMatchProperties(CandidateSpan candidateSpan) 
			throws IOException {
	    matchDocNumber = candidateSpan.getDoc();
	    matchStartPosition = candidateSpan.getStart();
	    matchEndPosition = candidateSpan.getEnd();		
		if (collectPayloads && candidateSpan.getPayloads() != null) {  		    	
	    	matchPayload.addAll(candidateSpan.getPayloads());  		    	
	    }
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
		matchList.clear();
		return advance();
	}

	@Override
	public long cost() {
		return matchCost;
	}
	
}
