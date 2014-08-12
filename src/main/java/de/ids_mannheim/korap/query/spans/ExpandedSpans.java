package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.SpanExpansionQuery;

public class ExpandedSpans extends SimpleSpans{
	
	private int min, max;
	private boolean isBefore;	
	private List<CandidateSpan> candidateSpans;
	private long matchCost;
	
	public ExpandedSpans(SpanExpansionQuery spanExpansionQuery,
			AtomicReaderContext context, Bits acceptDocs,
			Map<Term, TermContext> termContexts) throws IOException {
		super(spanExpansionQuery, context, acceptDocs, termContexts);
		this.min = spanExpansionQuery.getMin();
		this.max = spanExpansionQuery.getMax();
		this.isBefore = spanExpansionQuery.isBefore();
		
		candidateSpans = new ArrayList<CandidateSpan>();		
		hasMoreSpans = true;		
	}

	@Override
	public boolean next() throws IOException {
		matchPayload.clear();
		isStartEnumeration = false;
		return advance();
	}

	private boolean advance() throws IOException {		
		while (hasMoreSpans || candidateSpans.size() > 0) {
			if (candidateSpans.size() > 0 ){
				setMatch(candidateSpans.get(0));
				candidateSpans.remove(0);
				return true;
			}
			else {
				hasMoreSpans = firstSpans.next();
				setCandidateList();
			}
		}		
		return false;
	}
	
	private void setCandidateList() throws IOException {
		CandidateSpan cs;
		int counter;
		if (isBefore){
			counter = max;
			while (counter >= min ){
				cs = new CandidateSpan(
						firstSpans.start() - counter, 
						firstSpans.end(), 
						firstSpans.doc(), 
						firstSpans.cost(), 
						firstSpans.getPayload()
				);
				candidateSpans.add(cs);
				counter--;
			}
		}
		else{
			counter = min;
			while (counter <= max){
				cs = new CandidateSpan(
						firstSpans.start(), 
						firstSpans.end() + counter, 
						firstSpans.doc(), 
						firstSpans.cost(), 
						firstSpans.getPayload()
				);
				candidateSpans.add(cs);
				counter++;
			}
		}	
	}

	private void setMatch(CandidateSpan candidateSpan) {
		matchDocNumber = candidateSpan.getDoc();
		matchStartPosition = candidateSpan.getStart();
		matchEndPosition = candidateSpan.getEnd();			
		matchPayload = candidateSpan.getPayloads();
		matchCost = candidateSpan.getCost();
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
		return matchCost;
	}

}
