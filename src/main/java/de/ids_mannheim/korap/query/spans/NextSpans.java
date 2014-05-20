package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.util.Bits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ids_mannheim.korap.query.SpanNextQuery;

/**	NextSpans is an enumeration of Span matches, which ensures that  
 * 	a span is immediately followed by another span.
 *  
 *  Update: allow multiple matches at the same firstspan position
 *  
 * 	@author margaretha 
 * */
public class NextSpans extends SimpleSpans {	
	
	private List<CandidateSpan> matchList;
	private List<CandidateSpan> candidateList;
	private int candidateListDocNum;
	private boolean hasMoreFirstSpan;
	
	private Logger log = LoggerFactory.getLogger(NextSpans.class);

    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;

	
    public NextSpans (SpanNextQuery spanNextQuery,
		      AtomicReaderContext context,
		      Bits acceptDocs,
		      Map<Term,TermContext> termContexts) throws IOException {
    	super(spanNextQuery, context, acceptDocs, termContexts);
    	collectPayloads = spanNextQuery.isCollectPayloads();
    	hasMoreSpans =  secondSpans.next();
    	matchList = new ArrayList<>();
		candidateListDocNum = firstSpans.doc();
		candidateList = new ArrayList<>();
    }

	@Override
	public boolean next() throws IOException {		
		isStartEnumeration=false;
  		matchPayload.clear();
  		return advance();  		
	}
	
	private boolean advance() throws IOException {
		
		while (hasMoreSpans || !matchList.isEmpty() || !candidateList.isEmpty()){			
			if (!matchList.isEmpty()){
				matchDocNumber = firstSpans.doc();
			    matchStartPosition = firstSpans.start();
			    matchEndPosition = matchList.get(0).getEnd();
			    if (collectPayloads)
			    	matchPayload.addAll( matchList.get(0).getPayloads() );
			    if (DEBUG) {
				log.trace("Match doc#: {}",matchDocNumber);
				log.trace("Match positions: {}-{}", matchStartPosition,
					  matchEndPosition);
			    };
			    matchList.remove(0);
			    return true;
			}
			// Forward firstspan
			hasMoreFirstSpan = firstSpans.next();
			if (hasMoreFirstSpan) setMatchList();
			else {
				hasMoreSpans = false;
				candidateList.clear(); }
		}		
		return false;
	}

	private void setMatchList() throws IOException {
		if (firstSpans.doc() == candidateListDocNum){			
			searchCandidates();
			searchMatches();
		}
		else{ 
			candidateList.clear();
			if (hasMoreSpans && ensureSameDoc(firstSpans,secondSpans)){
				candidateListDocNum = firstSpans.doc();
				searchMatches();				
			}
		}		
	}
	
	private void searchCandidates() throws IOException {
		Iterator<CandidateSpan> i = candidateList.iterator();
		CandidateSpan cs;
		while(i.hasNext()){
			cs = i.next();
			if (cs.getStart() == firstSpans.end()){
				addMatch(cs);
			}
			else{
				//System.out.println(cs.getStart() + " " +firstSpans.end());
				i.remove();
			}
		}	
	}


	private void searchMatches() throws IOException {
		
		while (hasMoreSpans && candidateListDocNum == secondSpans.doc()){
			if (secondSpans.start() > firstSpans.end()){
				break;
			}
			if (secondSpans.start() == firstSpans.end()){
				candidateList.add(new CandidateSpan(secondSpans));
				addMatch(new CandidateSpan(secondSpans));
			}
			hasMoreSpans = secondSpans.next();
		}
	}
	
	private void addMatch(CandidateSpan cs) throws IOException{
		
		int start = firstSpans.start();
		long cost = firstSpans.cost() + cs.getCost();
		
		List<byte[]> payloads = new ArrayList<byte[]>();
		if (collectPayloads) {
			if (firstSpans.isPayloadAvailable())
				payloads.addAll(firstSpans.getPayload());
			if (cs.getPayloads() != null)
				payloads.addAll(cs.getPayloads());
		}	
		
		matchList.add(new CandidateSpan(start,cs.getEnd(),candidateListDocNum,cost,
				payloads));
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
		return firstSpans.cost() + secondSpans.cost();
	}
};
