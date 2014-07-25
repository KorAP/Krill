package de.ids_mannheim.korap.query;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.ToStringUtils;

import de.ids_mannheim.korap.query.spans.ElementSpans;

/** 
 * 	@author Nils Diewald, Margaretha
 */

/** Matches spans wrapped by an element. */
public class SpanElementQuery extends SimpleSpanQuery {
    protected static Term element;
    private String elementStr;
    
    /** Constructor. */
    public SpanElementQuery (String field, String term) {   
    	super(new SpanTermQuery(
    			(element = new Term(field,"<>:"+term))
    		  ),
    		true
		);
    	this.elementStr = term;
    };
    
    @Override
    public Spans getSpans(final AtomicReaderContext context,
			  Bits acceptDocs,
			  Map<Term,TermContext> termContexts) throws IOException {
    	return new ElementSpans(this, context, acceptDocs, termContexts);
    };

	public String getElementStr () {
		return elementStr;
    };

    public void setElementStr (String elementStr) {
    	this.elementStr = elementStr;
    }

	@Override
	public SimpleSpanQuery clone() {
		// TODO Auto-generated method stub
		return null;
	};
	
    @Override
    public void extractTerms(Set<Term> terms) {
    	terms.add(element);
    };

    @Override
    public String toString(String field) {
    	StringBuilder buffer = new StringBuilder("<");
    	buffer.append(getField()).append(':').append(elementStr);
    	buffer.append(ToStringUtils.boost(getBoost()));
    	return buffer.append(" />").toString();
    };
    
    @Override
    public int hashCode() {
    	final int prime = 37; // Instead of 31
    	int result = super.hashCode();
    	result = prime * result + ((element == null) ? 0 : element.hashCode());
    	return result;
    };

    @Override
    public boolean equals(Object obj) {
		if (this == obj)
		    return true;
		if (!super.equals(obj))
		    return false;
		if (getClass() != obj.getClass())
		    return false;
		SpanElementQuery other = (SpanElementQuery) obj;
		if (element == null) {
		    if (other.element != null)
			return false;
		} else if (!element.equals(other.element))
		    return false;
		return true;
    };
 
};
