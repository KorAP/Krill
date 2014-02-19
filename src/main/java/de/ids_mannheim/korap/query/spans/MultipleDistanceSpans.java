package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;
import org.hamcrest.core.IsInstanceOf;

import de.ids_mannheim.korap.query.SpanDistanceQuery;
import de.ids_mannheim.korap.query.SpanMultipleDistanceQuery;

/**	Span enumeration of matches whose two sub-spans has exactly the same 
 * 	first and second sub-sub-spans. This class basically filters the span 
 * 	matches of its child spans.
 * 
 * 	TODO: This doesn't accommodate distance constraint with exclusion
 * 	Case 1: return the match from another non-exclusion constraint.
 * 	Case 2: return only the first-span when all constraints are exclusions.
 * 	Case 3:	spans are not in the same doc 
 * 	 
 *	@author margaretha
 * */
public class MultipleDistanceSpans extends DistanceSpans{
		
	private DistanceSpans x,y;
	private boolean isOrdered;
	
	public MultipleDistanceSpans(SpanMultipleDistanceQuery query,
			AtomicReaderContext context, Bits acceptDocs,
			Map<Term, TermContext> termContexts, Spans firstSpans, 
			Spans secondSpans, boolean isOrdered, boolean exclusion) 
			throws IOException {
		super(query, context, acceptDocs, termContexts);
		this.isOrdered = isOrdered;
		this.exclusion = exclusion;		
		x = (DistanceSpans) firstSpans;
		y = (DistanceSpans) secondSpans;		
		hasMoreSpans = x.next() && y.next();		
	}

	@Override
	public boolean next() throws IOException {
		isStartEnumeration=false;
  		matchPayload.clear();
		return advance();		
	}
	
	/** Find the next match.
	 * */
	protected boolean advance() throws IOException {		
		while (hasMoreSpans && ensureSameDoc(x, y)){ 
			if (findMatch()){
				moveForward();
				return true;
			}
			moveForward();
		}		
		return false;
	}
	
	/** Find the next match of one of the sub/child-span.
	 * */
	private void moveForward() throws IOException{
		if (isOrdered){
			if (x.end() < y.end() || 
					(x.end() == y.end() && x.start() < y.start()) )
				hasMoreSpans = x.next();			 
			else hasMoreSpans = y.next();
		}
		// The matches of unordered distance spans are ordered by the 
		// start position
		else {
			if (x.start() < y.start() || 
					(x.start() == y.start() && x.end() < y.end()) )
				hasMoreSpans = x.next();			 
			else hasMoreSpans = y.next();
		}
	}
	
	/** Check if the sub-spans of x and y having exactly the same position.  
	 * 	This is basically an AND operation.
	 * 	@return true iff the sub-spans are identical.
	 * */	
	protected boolean findMatch() throws IOException {
		 		
		CandidateSpan xf = x.getMatchFirstSpan();
		CandidateSpan xs = x.getMatchSecondSpan();
		
		CandidateSpan yf = y.getMatchFirstSpan();
		CandidateSpan ys = y.getMatchSecondSpan();
		
		if (x.isExclusion() || y.isExclusion()){			
			if (xf.getStart() == yf.getStart() && xf.getEnd() == yf.getEnd()){
				if (x.isExclusion() && y.isExclusion()){
					// set x or y doesnt matter
					setMatchProperties(x,true);
				}
				else if (x.isExclusion()){  
					// set y, the usual match
					setMatchProperties(y,true);
				}
				else { setMatchProperties(x,true); }
				return true;
			}
		}
		else if (xf.getStart() == yf.getStart() &&
				xf.getEnd() == yf.getEnd() &&
				xs.getStart() == ys.getStart() &&
				xs.getEnd() == ys.getEnd()){
			setMatchProperties(x,false);			
			return true;
		}		
		return false;
	}
	

	private void setMatchProperties(DistanceSpans span, boolean exclusion) {
		matchStartPosition = span.start();
		matchEndPosition = span.end();
		matchDocNumber = span.doc();
		matchPayload = span.matchPayload;	
		
		setMatchFirstSpan(span.getMatchFirstSpan());
		if (!exclusion) setMatchSecondSpan(span.getMatchSecondSpan());
		log.trace("doc# {}, start {}, end {}",matchDocNumber,
				matchStartPosition,matchEndPosition);	
	}

	@Override
	public boolean skipTo(int target) throws IOException {
		if (hasMoreSpans && (y.doc() < target)){
  			if (!y.skipTo(target)){  				
  				return false;
  			}
  		}
		matchPayload.clear();
		isStartEnumeration=false;
		return advance();
	}

	@Override
	public long cost() {		
		return x.cost() + y.cost();
	}

}
