package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.SimpleSpanQuery;
import de.ids_mannheim.korap.query.SpanSegmentQuery;

/**	SegmentSpans is an enumeration of Span matches, which ensures that two spans 
 * 	have exactly the same start and end positions.
 * 
 * 	@author margaretha 
 * */
public class SegmentSpans extends SimpleSpans {	
	
    public SegmentSpans (SpanSegmentQuery spanSegmentQuery,
  	      AtomicReaderContext context,
  	      Bits acceptDocs,
  	      Map<Term,TermContext> termContexts) throws IOException {
    	this(spanSegmentQuery, context, acceptDocs, termContexts, true);    	
    }
    
    public SegmentSpans (SpanSegmentQuery spanSegmentQuery,
	      AtomicReaderContext context,
	      Bits acceptDocs,
	      Map<Term,TermContext> termContexts,
	      boolean collectPayloads) throws IOException {		 			
		super(spanSegmentQuery, context, acceptDocs, termContexts,collectPayloads);
	}

    /** Check weather the start and end positions of the current 
     * 	firstspan and secondspan are identical. 
  	 * */
	protected int findMatch() {
		
		if (firstSpans.start() == secondSpans.start() &&
			firstSpans.end() == secondSpans.end() ){
			matchDocNumber = firstSpans.doc();
			matchStartPosition = firstSpans.start();
			matchEndPosition = firstSpans.end();			
			return 0;
		}
		else if (firstSpans.start() < secondSpans.start() || 
				firstSpans.end() < secondSpans.end())
			return -1;
		
		return 1;
	}	
}
