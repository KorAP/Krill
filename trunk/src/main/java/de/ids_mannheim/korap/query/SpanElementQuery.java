package de.ids_mannheim.korap.query;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.index.TermState;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.ToStringUtils;
import org.apache.lucene.search.spans.Spans;

import de.ids_mannheim.korap.query.spans.ElementSpans;

import java.io.IOException;
import java.util.Map;
import java.util.Set;


/** Matches spans wrapped by an element. */
public class SpanElementQuery extends SpanQuery {
    protected Term element;
    private String elementStr;
    private String field;
    
    /** Constructor. */
    public SpanElementQuery(String field, String term) {
	StringBuilder sb = new StringBuilder("<>:");
	this.field = field;
	this.elementStr = term;
	this.element = new Term(field, sb.append(term).toString());
    };

    /** Return the element whose spans are matched. */
    public Term getElement() { return element; };

    @Override
    public String getField() { return element.field(); };
  
    @Override
    public void extractTerms(Set<Term> terms) {
	terms.add(element);
    };

    @Override
    public String toString(String field) {
	StringBuilder buffer = new StringBuilder("<");
	buffer.append(this.field).append(':').append(elementStr);
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

    @Override
    public Spans getSpans(final AtomicReaderContext context,
			  Bits acceptDocs,
			  Map<Term,TermContext> termContexts) throws IOException {
	TermContext termContext = termContexts.get(element);
	final TermState state;
	if (termContext == null) {
	    // this happens with span-not query,
	    // as it doesn't include the NOT side in extractTerms()
	    // so we seek to the term now in this segment...,
	    // this sucks because its ugly mostly!
	    final Fields fields = context.reader().fields();
	    if (fields != null) {
		final Terms terms = fields.terms(element.field());
		if (terms != null) {
		    final TermsEnum termsEnum = terms.iterator(null);
		    if (termsEnum.seekExact(element.bytes(), true)) { 
			state = termsEnum.termState();
		    } else {
			state = null;
		    }
		} else {
		    state = null;
		}
	    } else {
		state = null;
	    }
	} else {
	    state = termContext.get(context.ord);
	};
    
	if (state == null) { // term is not present in that reader
	    return ElementSpans.EMPTY_ELEMENT_SPANS;
	};
    
	final TermsEnum termsEnum = context.reader().terms(element.field()).iterator(null);
	termsEnum.seekExact(element.bytes(), state);
    
	final DocsAndPositionsEnum postings = termsEnum.docsAndPositions(acceptDocs, null, DocsAndPositionsEnum.FLAG_PAYLOADS);

	if (postings != null) {
	    return new ElementSpans(postings, element);
	};

	// element does exist, but has no positions
	throw new IllegalStateException("field \"" + element.field() + "\" was indexed without position data; cannot run SpanElementQuery (element=" + element.text() + ")");
    };
};
