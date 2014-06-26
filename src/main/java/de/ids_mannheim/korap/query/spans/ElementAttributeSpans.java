package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.util.Bits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ids_mannheim.korap.query.SpanElementAttributeQuery;

/** A wrapper matching the element and attribute spans. Specifically searching
 * 	the elements to which a certain attribute belongs to. 
 * 
 * */
public class ElementAttributeSpans extends SimpleSpans{
	
	ElementSpans elements;
	AttributeSpans attributes;
	
	protected Logger logger = LoggerFactory.getLogger(ElementAttributeSpans.class);
	
	public ElementAttributeSpans(SpanElementAttributeQuery simpleSpanQuery,
			AtomicReaderContext context, Bits acceptDocs,
			Map<Term, TermContext> termContexts) throws IOException {
		super(simpleSpanQuery, context, acceptDocs, termContexts);		
		elements = (ElementSpans) firstSpans;
		attributes = (AttributeSpans) secondSpans;
		elements.isElementRef = true; // dummy setting enabling reading elementRef
		hasMoreSpans = elements.next() & attributes.next();		
	}

	@Override
	public boolean next() throws IOException {
		isStartEnumeration=false;
		return advance();
	}

	private boolean advance() throws IOException {
		
		while (hasMoreSpans && ensureSamePosition(elements,attributes)){ 
			
			logger.info("element: " + elements.start() + ","+ elements.end() +" ref:"+elements.getElementRef());
			
			if (elements.getElementRef() < 1){
				elements.isElementRef = true; // dummy setting enabling reading elementRef
				hasMoreSpans = elements.next();
				logger.info("skip");
				continue;
			}
			
			logger.info("attribute {} ref:{}", attributes.start(),  attributes.getElementRef());
			
			if (elements.getElementRef() == attributes.getElementRef()){
				this.matchDocNumber = elements.doc();
				this.matchStartPosition = elements.start();
				this.matchEndPosition = elements.end();
				this.matchPayload = elements.getPayload();
				hasMoreSpans = attributes.next();
				return true;
			}
			
			if (elements.getElementRef() < attributes.getElementRef())
				hasMoreSpans = attributes.next();
			else {
				elements.isElementRef = true; // dummy setting enabling reading elementRef
				hasMoreSpans = elements.next();				
			}
		}
		
		return false;
	}

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
		if (hasMoreSpans && (attributes.doc() < target)){
  			if (!attributes.skipTo(target)){
  				return false;
  			}
  		}		
		matchPayload.clear();
		isStartEnumeration=false;
		return advance();
	}

	@Override
	public long cost() {
		return elements.cost() + attributes.cost();
	}


}
