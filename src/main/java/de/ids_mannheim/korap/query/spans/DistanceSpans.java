package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ids_mannheim.korap.query.SpanDistanceQuery;
import de.ids_mannheim.korap.query.SpanMultipleDistanceQuery;

/** DistanceSpan is a base class for enumeration of span matches, 
 * 	whose two child spans have a specific range of distance (within 
 * 	a min and a max distance) and must be in order (a firstspan is 
 * 	followed by a secondspan). 
 * 
 * @author margaretha
 * */
public abstract class DistanceSpans extends SimpleSpans{	

	protected CandidateSpan matchFirstSpan,matchSecondSpan;	
	protected Logger log = LoggerFactory.getLogger(DistanceSpans.class);
	protected boolean exclusion; // because MultipleDistanceQuery doesn't have this property
    
	public DistanceSpans(SpanDistanceQuery query,
			AtomicReaderContext context, Bits acceptDocs,
			Map<Term, TermContext> termContexts) throws IOException {
		super(query, context, acceptDocs, termContexts);
		exclusion = query.isExclusion();
	}
	
	public DistanceSpans(SpanMultipleDistanceQuery query,
			AtomicReaderContext context, Bits acceptDocs,
			Map<Term, TermContext> termContexts) throws IOException {
		super(query, context, acceptDocs, termContexts);
	}
	
	@Override
	public boolean next() throws IOException {		
		isStartEnumeration=false;
  		matchPayload.clear();
		return advance();
	}	

		
	/** Find the next span match.
	 * @return true iff a span match is available.
	 * */
	protected abstract boolean advance() throws IOException; 

	/** Find the same doc shared by element, firstspan and secondspan.
	 *  @return true iff such a doc is found.
	 * */
	protected boolean findSameDoc(Spans x, 
			Spans y, Spans e) throws IOException{
		
		while (hasMoreSpans) {
			if (ensureSameDoc(x, y) &&
					e.doc() == x.doc()){
				return true;
			}			
			if (!ensureSameDoc(e,y)){
				return false;
			};
		}		
  		return false;
	}
	
	public CandidateSpan getMatchFirstSpan() {
		return matchFirstSpan;
	}

	public void setMatchFirstSpan(CandidateSpan matchFirstSpan) {
		this.matchFirstSpan = matchFirstSpan;
	}

	public CandidateSpan getMatchSecondSpan() {
		return matchSecondSpan;
	}

	public void setMatchSecondSpan(CandidateSpan matchSecondSpan) {
		this.matchSecondSpan = matchSecondSpan;
	}

	public boolean isExclusion() {
		return exclusion;
	}

	public void setExclusion(boolean exclusion) {
		this.exclusion = exclusion;
	}

}
