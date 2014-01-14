package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ids_mannheim.korap.query.SimpleSpanQuery;

/** An abstract class for Span enumeration whose two child spans are matched by 
 * 	their positions and do not have a partial overlap.
 *  
 * 	@author margaretha
 * 
 * */
public abstract class SimpleSpans extends Spans{
	private boolean isStartEnumeration;
	private boolean hasMoreSpans;
	protected int matchDocNumber, matchStartPosition, matchEndPosition;	
	private List<byte[]> matchPayload;
    private boolean collectPayloads;  
    
	// Warning: enumeration of Spans
	protected Spans firstSpans, secondSpans;
	private SimpleSpanQuery query;	  
    
    private Logger log = LoggerFactory.getLogger(SimpleSpans.class);
      
    public SimpleSpans (SimpleSpanQuery simpleSpanQuery,
  	      AtomicReaderContext context,
  	      Bits acceptDocs,
  	      Map<Term,TermContext> termContexts,
  	      boolean collectPayloads) throws IOException {
    	  
    	// Initialize as invalid
  		matchDocNumber= -1;
  		matchStartPosition= -1;
  		matchEndPosition= -1;
  			
  		// TODO: what is this
  		this.collectPayloads = true;
  		matchPayload = new LinkedList<byte[]>();
  		
  		// Get the enumeration of the two spans to match
  		firstSpans = simpleSpanQuery.getFirstClause().
  			getSpans(context, acceptDocs, termContexts);
  		secondSpans = simpleSpanQuery.getSecondClause().
  			getSpans(context, acceptDocs, termContexts);
  	
  		query = simpleSpanQuery;		
  		hasMoreSpans = secondSpans.next();
  		isStartEnumeration=true;
      }
      
    @Override
  	public boolean next() throws IOException {
    	// Warning: this does not work for overlapping spans 
    	// e.g. get multiple second spans in a firstspan
  		hasMoreSpans &= firstSpans.next();
  		isStartEnumeration=false;
  		matchPayload.clear();
  		return advance();  		
  	}	
  	
  	/** Advance is a lucene terminology to search for the next match.
  	 * */
    private boolean advance() throws IOException {	    	
		// The complexity is linear for searching in a document. 
		// It's better if we can skip to >= position in a document.
    	while (hasMoreSpans && ensureSameDoc()){
    		int matchCase = findMatch();
			if (matchCase == 0){
				log.trace("Match doc#: {}",matchDocNumber);
				log.trace("Match positions: {}-{}", matchStartPosition, 
						matchEndPosition);
				doCollectPayloads();
				return true;
			} 
			else if (matchCase == 1) {
				hasMoreSpans = secondSpans.next();			
			}			
			else{ 
				hasMoreSpans = firstSpans.next();
			}
		}
		return false;
	}	
        
    /** Specify the condition for a match 
     * @return 0 iff match is found,
     * 			-1 to advance the firstspan,		
     * 			1 to advance the secondspan
     * */
  	protected abstract int findMatch();

  	
  	/** If the current firstspan and secondspan are not in the same document,
  	 * 	try to skip the span with the smaller document number, to the same 
  	 * 	OR a greater document number than, the document number of the other 
  	 * 	span. Do this until the firstspan and the secondspan are in the same 
  	 * 	doc, OR until reaching the last document. 
  	 *	@return true iff such a document exists.
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
  	
  	/** Collecting available payloads from the current first and second spans */
  	private void doCollectPayloads() throws IOException {
  		if (collectPayloads){
  			log.trace("Collect payloads");
  		    if (firstSpans.isPayloadAvailable()) {
  		    	Collection<byte[]> payload = firstSpans.getPayload();
  		    	log.trace("Found {} payloads in firstSpans", payload.size());
  		    	matchPayload.addAll(payload);
  		    }
  		    if (secondSpans.isPayloadAvailable()) {
  		    	Collection<byte[]> payload = secondSpans.getPayload();
  		    	log.trace("Found {} payloads in secondSpans", payload.size());
  		    	matchPayload.addAll(payload);
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
  	public int doc() {
  		return matchDocNumber;
  	}

  	@Override
  	public int start() {
  		return matchStartPosition;
  	}

  	@Override
  	public int end() {
  		return matchEndPosition;
  	}

  	@Override
  	public Collection<byte[]> getPayload() throws IOException {
  		return matchPayload;
  	}

  	@Override
  	public boolean isPayloadAvailable() throws IOException {
  		return !matchPayload.isEmpty();
  	}

  	@Override
  	public long cost() {
  		return firstSpans.cost() + secondSpans.cost();
  	}
  	
  	@Override
  	public String toString() { // who does call this?				
  		return getClass().getName() + "("+query.toString()+")@"+
  		    (isStartEnumeration?"START":(hasMoreSpans?(doc()+":"+
  		    start()+"-"+end()):"END"));
  	}
    
}
