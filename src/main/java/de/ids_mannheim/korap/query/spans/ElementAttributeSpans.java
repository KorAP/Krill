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
import de.ids_mannheim.korap.query.SpanElementAttributeQuery;

/** Span enumeration of elements that have some attribute and/or do <em>not</em> 
 * 	have some attributes. This class handles <em>and</em> operation on attributes.
 * 
 * 	Use SpanOrQuery to perform <em>or</em> operation on attributes, i.e. choose 
 * 	between two elements with some attribute constraints. Note that the attribute 
 * 	constraints have to be in Conjunctive Normal Form (CNF). 
 *
 * 	@author margaretha
 * */
public class ElementAttributeSpans extends SimpleSpans{
	
	private ElementSpans elements;
	private List<AttributeSpans> attributeList;
	private List<AttributeSpans> notAttributeList;
	
	protected Logger logger = LoggerFactory.getLogger(ElementAttributeSpans.class);
	
	public ElementAttributeSpans(SpanElementAttributeQuery simpleSpanQuery,
			AtomicReaderContext context, Bits acceptDocs,
			Map<Term, TermContext> termContexts) throws IOException {
		super(simpleSpanQuery, context, acceptDocs, termContexts);		
		elements = (ElementSpans) firstSpans;
		elements.isElementRef = true; // dummy setting enabling reading elementRef
		hasMoreSpans = elements.next();
		
		attributeList = new ArrayList<AttributeSpans>();
		notAttributeList = new ArrayList<AttributeSpans>();
		
		List<SpanQuery> sqs = simpleSpanQuery.getClauseList();
		AttributeSpans as;
		for (SpanQuery sq: sqs){
			as = (AttributeSpans) sq.getSpans(context, acceptDocs, termContexts);
			if (((SpanAttributeQuery) sq).isNegation()){
				notAttributeList.add(as);
				as.next();
			}
			else {
				attributeList.add(as);
				hasMoreSpans &= as.next();
			}
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
		
		while (hasMoreSpans && computeElementPosition()){			
			logger.info("element: " + elements.start() + ","+ elements.end() +
					" ref:"+elements.getElementRef());
			
			if (checkElementRef() && checkNotElementRef()){			
				this.matchDocNumber = elements.doc();
				this.matchStartPosition = elements.start();
				this.matchEndPosition = elements.end();
				this.matchPayload = elements.getPayload();
				hasMoreSpans = attributeList.get(0).next();
				logger.info("MATCH "+matchDocNumber);
				
				hasMoreSpans = elements.next();		
				return true;
			}
		}
		return false;
	}
	
	/** Ensuring all the attribute spans having the same elementRef with 
	 * 	the actual element's elementRef.
	 * */
	private boolean checkElementRef() throws IOException{
		
		for (AttributeSpans attribute: attributeList){			
			if (elements.getElementRef() != attribute.getElementRef()){
				logger.info("attribute ref doesn't match");
				if (elements.getElementRef() < attribute.getElementRef())
					hasMoreSpans = attribute.next();
				else {
					hasMoreSpans = elements.next();				
				}
				
				return false;
			}
		}		
		return true;
	}
	
	/** Ensuring elements do not contain the not attributes. In other words, 
	 * 	the elementRef is not the same as the not attribute's elementRefs. 
	 * */
	private boolean checkNotElementRef() throws IOException{
		for (AttributeSpans notAttribute: notAttributeList){
			if (elements.start() == notAttribute.start() &&
					elements.getElementRef() == notAttribute.getElementRef()){
				logger.info("not attribute ref exists");
				hasMoreSpans = elements.next();	
				return false;
			}
		}
		return true;
	}
	
	/**	Search for a possible element having the same doc and start position as
	 * 	the attributes.
	 * */
	private boolean computeElementPosition() throws IOException {		

		while (hasMoreSpans){
			
			if (elements.getElementRef() < 1){ // the element does not have an attribute
				elements.isElementRef = true; // dummy setting enabling reading elementRef
				hasMoreSpans = elements.next();
				logger.info("skip");
				continue;
			}
			
			if (checkAttributeListPosition() && 
					checkNotAttributeListPosition()){
				logger.info("element is found: "+ elements.start());
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
			logger.info("a "+a.start());
			while (!a.isFinish() &&	 a.doc() <= elements.doc()){
				
				if (a.doc() == elements.doc() &&
						a.start() >= elements.start())
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
		int currentPosition = elements.start();
		boolean isSame = true;
		boolean isFirst = true;
		
		for (AttributeSpans a : attributeList){
			if(!ensureSamePosition(elements, a)) return false;
				
				logger.info("pos:" + elements.start());
				if (isFirst){ 
					isFirst = false;
					currentPosition = elements.start();
				}
				else if (currentPosition != elements.start()){					
					currentPosition = elements.start();
					isSame = false;
				
			}				 
		}
		logger.info("same pos: "+isSame+ ", pos "+elements.start());
		return isSame;
	}
	
	/** Advance the element or attribute spans to be in the same doc 
	 * 	and start position.
	 * */
	private boolean ensureSamePosition(ElementSpans elements,
			AttributeSpans attributes) throws IOException {
		
		while (hasMoreSpans && ensureSameDoc(elements, attributes)){
			if (attributes.start() == elements.start())
				return true;
			else if (attributes.start() > elements.start()) 
				hasMoreSpans = elements.next();
			else 
				hasMoreSpans= attributes.next();
		}
		
		return false;
	}

	@Override
	public boolean skipTo(int target) throws IOException {
		if (hasMoreSpans && (elements.doc() < target)){
  			if (!elements.skipTo(target)){
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
		return elements.cost() + cost;
	}


}
