package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.SpanWithIdQuery;

/** Base class for span enumeration with spanid property.
 * 	@author margaretha
 * */
public abstract class SpansWithId extends SimpleSpans{

	protected short spanId;
	protected boolean hasSpanId = false; // A dummy flag
	
	public SpansWithId(SpanWithIdQuery spanWithIdQuery,
			AtomicReaderContext context, Bits acceptDocs,
			Map<Term, TermContext> termContexts) throws IOException {
		super(spanWithIdQuery, context, acceptDocs, termContexts);
	}

	public short getSpanId() {
		return spanId;
	}

	public void setSpanId(short spanId) {
		this.spanId = spanId;
	}
}
