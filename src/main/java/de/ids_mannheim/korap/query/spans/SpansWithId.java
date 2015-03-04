package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.SpanWithIdQuery;

/**
 * Base class for enumeration of span requiring an id, such as
 * elements and
 * relations.
 * 
 * @author margaretha
 * */
public abstract class SpansWithId extends SimpleSpans {

    protected short spanId;
    protected boolean hasSpanId = false; // A dummy flag


    /**
     * Constructs SpansWithId for the given {@link SpanWithIdQuery}.
     * 
     * @param spanWithIdQuery
     *            a SpanWithIdQuery
     * @param context
     * @param acceptDocs
     * @param termContexts
     * @throws IOException
     */
    public SpansWithId (SpanWithIdQuery spanWithIdQuery,
                        AtomicReaderContext context, Bits acceptDocs,
                        Map<Term, TermContext> termContexts) throws IOException {
        super(spanWithIdQuery, context, acceptDocs, termContexts);
    }


    public SpansWithId () {}


    /**
     * Returns the span id of the current span
     * 
     * @return the span id of the current span
     */
    public short getSpanId () {
        return spanId;
    }


    /**
     * Sets the span id of the current span
     * 
     * @param spanId
     *            span id
     */
    public void setSpanId (short spanId) {
        this.spanId = spanId;
    }
}
