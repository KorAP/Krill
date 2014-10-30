package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.util.Bits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ids_mannheim.korap.query.SpanAttributeQuery;
import de.ids_mannheim.korap.query.SpanWithAttributeQuery;

/** Span enumeration of element or relation spans having and/or <em>not</em> 
 * 	having some attributes. This class handles <em>and</em> operation on attributes.
 * 
 * 	Use SpanOrQuery to perform <em>or</em> operation on attributes, i.e. choose 
 * 	between two elements with some attribute constraints. Note that the attribute 
 * 	constraints have to be in Conjunctive Normal Form (CNF). 
 *
 * 	@author margaretha
 * */
public class SpansWithAttribute extends SpansWithId{
	
	private SpansWithId withAttributeSpans;
	private List<AttributeSpans> attributeList;
	private List<AttributeSpans> notAttributeList;
	
	protected Logger logger = LoggerFactory.getLogger(SpansWithAttribute.class);

	public SpansWithAttribute(SpanWithAttributeQuery spanWithAttributeQuery,
			SpansWithId withIdSpans,
			AtomicReaderContext context, Bits acceptDocs,
			Map<Term, TermContext> termContexts) throws IOException {
		super(spanWithAttributeQuery, context, acceptDocs, termContexts);		
		withAttributeSpans = withIdSpans;
		withAttributeSpans.hasSpanId = true; // dummy setting enabling reading elementRef
		hasMoreSpans = withAttributeSpans.next();
		
		attributeList = new ArrayList<AttributeSpans>();
		notAttributeList = new ArrayList<AttributeSpans>();		
		
		List<SpanQuery> sqs = spanWithAttributeQuery.getClauseList();
		if (sqs != null){
			for (SpanQuery sq: sqs){
				addAttributes(sq, context, acceptDocs, termContexts);
			}
		}
		else {
			addAttributes(spanWithAttributeQuery.getSecondClause(), 
					context, acceptDocs, termContexts);
		}
	}
	
	private void addAttributes(SpanQuery sq, AtomicReaderContext context, Bits acceptDocs,
			Map<Term, TermContext> termContexts) throws IOException {
		AttributeSpans as = (AttributeSpans) sq.getSpans(context, acceptDocs, termContexts);
		if (((SpanAttributeQuery) sq).isNegation()){
			notAttributeList.add(as);
			as.next();
		}
		else {
			attributeList.add(as);
			hasMoreSpans &= as.next();
		}	
	}

	@Override
	public boolean next() throws IOException {
		isStartEnumeration=false;
		return advance();
	}
	
	/** Search for the next match by first identify a possible 
	 * 	element position, and then ensuring that the element contains
	 * 	all the attributes and <em>do not</em> contain any of the 
	 *  not attributes.
	 * */
	private boolean advance() throws IOException {
		
		while (hasMoreSpans && searchSpanPosition()){			
		 	    //logger.info("element: " + withAttributeSpans.start() + ","+ withAttributeSpans.end() +
				//	" ref:"+withAttributeSpans.getSpanId());
			
			if (checkSpanId() && checkNotSpanId()){			
				this.matchDocNumber = withAttributeSpans.doc();
				this.matchStartPosition = withAttributeSpans.start();
				this.matchEndPosition = withAttributeSpans.end();
				this.matchPayload = withAttributeSpans.getPayload();
				this.spanId = withAttributeSpans.getSpanId();
				
				if (attributeList.size() > 0)
					hasMoreSpans = attributeList.get(0).next();
				
			    //logger.info("MATCH "+matchDocNumber);
				
				hasMoreSpans &= withAttributeSpans.next();		
				return true;
			}
		}
		return false;
	}
	
	/** Ensuring all the attribute spans having the same elementRef with 
	 * 	the actual element's elementRef.
	 * */
	private boolean checkSpanId() throws IOException{
		
		for (AttributeSpans attribute: attributeList){			
			if (withAttributeSpans.getSpanId() != attribute.getSpanId()){
//				    logger.info("attribute ref doesn't match");
				if (withAttributeSpans.getSpanId() < attribute.getSpanId())
					hasMoreSpans = attribute.next();
				else {
					hasMoreSpans = withAttributeSpans.next();				
				}
				
				return false;
			}
		}		
		return true;
	}
	
	/** Ensuring elements do not contain the not attributes. In other words, 
	 * 	the elementRef is not the same as the not attribute's elementRefs. 
	 * */
	private boolean checkNotSpanId() throws IOException{
		for (AttributeSpans notAttribute: notAttributeList){
			if (!notAttribute.isFinish() && 
					withAttributeSpans.start() == notAttribute.start() &&
					withAttributeSpans.getSpanId() == notAttribute.getSpanId()){
//				    logger.info("not attribute ref exists");
				hasMoreSpans = withAttributeSpans.next();	
				return false;
			}
		}
		return true;
	}
	
	/**	Search for a possible element having the same doc and start position as
	 * 	the attributes.
	 * */
	private boolean searchSpanPosition() throws IOException {		

		while (hasMoreSpans){
			
			if (withAttributeSpans.getSpanId() < 1){ // the element does not have an attribute
				hasMoreSpans = withAttributeSpans.next();
//			    logger.info("skip");
				continue;
			}
			
			if (checkAttributeListPosition() && 
					checkNotAttributeListPosition()){
//				    logger.info("element is found: "+ withAttributeSpans.start());
				return true;
			}			
		}		
		
		return false;
	}
	
	/**	Advancing the not attributes to be in the same or greater doc# than 
	 * 	element doc#. If a not attribute is in the same doc, advance it to
	 * 	be in the same or greater start position than the element.
	 * 
	 * */
	private boolean checkNotAttributeListPosition() throws IOException{
		
		for (AttributeSpans a : notAttributeList){
			// advance the doc# of not AttributeSpans
			// logger.info("a "+a.start());
			while (!a.isFinish() &&	 a.doc() <= withAttributeSpans.doc()){
				
				if (a.doc() == withAttributeSpans.doc() &&
						a.start() >= withAttributeSpans.start())
					break;
				
				if (!a.next()) a.setFinish(true);
			}
		}
		
		return true;
	}
	
	/** Advancing the attributes to be in the same doc and start position 
	 * 	as the element.
	 * */
	private boolean checkAttributeListPosition() throws IOException{
		int currentPosition = withAttributeSpans.start();
		boolean isSame = true;
		boolean isFirst = true;
		
		for (AttributeSpans a : attributeList){
			if(!ensureSamePosition(withAttributeSpans, a)) return false;
			//	    logger.info("pos:" + withAttributeSpans.start());
				if (isFirst){ 
					isFirst = false;
					currentPosition = withAttributeSpans.start();
				}
				else if (currentPosition != withAttributeSpans.start()){					
					currentPosition = withAttributeSpans.start();
					isSame = false;
				
			}				 
		}
		//    logger.info("same pos: "+isSame+ ", pos "+withAttributeSpans.start());
		return isSame;
	}
	
	/** Advance the element or attribute spans to be in the same doc 
	 * 	and start position.
	 * */
	private boolean ensureSamePosition(SpansWithId spans,
			AttributeSpans attributes) throws IOException {
		
		while (hasMoreSpans && ensureSameDoc(spans, attributes)){
			if (attributes.start() == spans.start())
				return true;
			else if (attributes.start() > spans.start()) 
				hasMoreSpans = spans.next();
			else 
				hasMoreSpans= attributes.next();
		}
		
		return false;
	}

	@Override
	public boolean skipTo(int target) throws IOException {
		if (hasMoreSpans && (withAttributeSpans.doc() < target)){
  			if (!withAttributeSpans.skipTo(target)){
  				return false;
  			}
  		}		
		isStartEnumeration=false;
		return advance();
	}

	@Override
	public long cost() {
		
		long cost = 0;
		for (AttributeSpans as: attributeList){
			cost += as.cost();
		}
		for (AttributeSpans as: notAttributeList){
			cost += as.cost();
		}
		return withAttributeSpans.cost() + cost;
	}


}
