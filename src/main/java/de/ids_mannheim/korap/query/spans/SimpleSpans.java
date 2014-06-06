package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.SimpleSpanQuery;

/** An abstract class for Span enumeration including span match properties
 * 	and basic methods.
 *  
 * 	@author margaretha 
 * */
public abstract class SimpleSpans extends Spans{
	private SimpleSpanQuery query;
	protected boolean isStartEnumeration;
	protected boolean collectPayloads;
	
	protected boolean hasMoreSpans;
	// Warning: enumeration of Spans
	protected Spans firstSpans, secondSpans;
	
	protected int matchDocNumber, matchStartPosition, matchEndPosition;	
	protected Collection<byte[]> matchPayload;	
      
    public SimpleSpans (SimpleSpanQuery simpleSpanQuery,
			AtomicReaderContext context,
			Bits acceptDocs,
			Map<Term,TermContext> termContexts) throws IOException {
    	
    	query = simpleSpanQuery;
    	collectPayloads = query.isCollectPayloads();
    	
  		matchDocNumber= -1;
  		matchStartPosition= -1;
  		matchEndPosition= -1;
  		matchPayload = new LinkedList<byte[]>(); // why linkedlist?
		// -> Can be an array as well, don't know if it comes from me or from you (ndiewald)
  		
  		// Get the enumeration of the two spans to match
  		firstSpans = simpleSpanQuery.getFirstClause().
  			getSpans(context, acceptDocs, termContexts);
  		if (simpleSpanQuery.getSecondClause() != null)
  			secondSpans = simpleSpanQuery.getSecondClause().
  				getSpans(context, acceptDocs, termContexts);
  		
  		isStartEnumeration=true;
      }
  	
  	/** If the current x and y are not in the same document, to skip the 
  	 * 	span with the smaller document number, to the same OR a greater 
  	 * 	document number than, the document number of the other span. Do 
  	 * 	this until the x and the y are in the same doc, OR until the last 
  	 * 	document.	
  	 *	@return true iff such a document exists.
  	 * */
  	protected boolean ensureSameDoc(Spans x, Spans y) throws IOException {		
  		while (x.doc() != y.doc()) {
  			if (x.doc() < y.doc()){
  				if (!x.skipTo(y.doc())){
  					hasMoreSpans = false;
  					return false;
  				}				
  			}		
  			else {
  				if (!y.skipTo(x.doc())){
  					hasMoreSpans = false;
  					return false;
  				}	
  			}			
  		}		
  		return true;
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
  	public String toString() { // who does call this?				
  		return getClass().getName() + "("+query.toString()+")@"+
  		    (isStartEnumeration?"START":(hasMoreSpans?(doc()+":"+
  		    start()+"-"+end()):"END"));
  	}
    
}
