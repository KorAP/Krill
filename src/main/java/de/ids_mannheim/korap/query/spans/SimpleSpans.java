package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.SimpleSpanQuery;

/**
 * An abstract class for Span enumeration including span match
 * properties
 * and basic methods.
 * 
 * @author margaretha
 * */
public abstract class SimpleSpans extends Spans {
    private SimpleSpanQuery query;
    protected boolean isStartEnumeration;
    protected boolean collectPayloads;

    protected boolean hasMoreSpans;
    // Warning: enumeration of Spans
    protected Spans firstSpans, secondSpans;

    protected int matchDocNumber, matchStartPosition, matchEndPosition;
    protected Collection<byte[]> matchPayload;


    public SimpleSpans () {
        collectPayloads = true;
        matchDocNumber = -1;
        matchStartPosition = -1;
        matchEndPosition = -1;
        matchPayload = new ArrayList<byte[]>();
        isStartEnumeration = true;
    };


    public SimpleSpans (SimpleSpanQuery simpleSpanQuery,
                        AtomicReaderContext context, Bits acceptDocs,
                        Map<Term, TermContext> termContexts) throws IOException {
        this();
        query = simpleSpanQuery;
        collectPayloads = query.isCollectPayloads();
        // Get the enumeration of the two spans to match
        SpanQuery sq;
        if ((sq = simpleSpanQuery.getFirstClause()) != null)
            firstSpans = sq.getSpans(context, acceptDocs, termContexts);

        if ((sq = simpleSpanQuery.getSecondClause()) != null)
            secondSpans = sq.getSpans(context, acceptDocs, termContexts);

    }


    /**
     * If the current x and y are not in the same document, to skip
     * the
     * span with the smaller document number, to the same OR a greater
     * document number than, the document number of the other span. Do
     * this until the x and the y are in the same doc, OR until the
     * last
     * document.
     * 
     * @return true iff such a document exists.
     * */
    protected boolean ensureSameDoc (Spans x, Spans y) throws IOException {
        while (x.doc() != y.doc()) {
            if (x.doc() < y.doc()) {
                if (!x.skipTo(y.doc())) {
                    hasMoreSpans = false;
                    return false;
                }
            }
            else {
                if (!y.skipTo(x.doc())) {
                    hasMoreSpans = false;
                    return false;
                }
            }
        }
        return true;
    }


    /**
     * Find the same doc shared by element, firstspan and secondspan.
     * 
     * @return true iff such a doc is found.
     * */
    protected boolean findSameDoc (Spans x, Spans y, Spans e)
            throws IOException {

        while (hasMoreSpans) {
            if (ensureSameDoc(x, y) && e.doc() == x.doc()) {
                return true;
            }
            if (!ensureSameDoc(e, y)) {
                return false;
            };
        }
        return false;
    }


    @Override
    public int doc () {
        return matchDocNumber;
    }


    @Override
    public int start () {
        return matchStartPosition;
    }


    @Override
    public int end () {
        return matchEndPosition;
    }


    @Override
    public Collection<byte[]> getPayload () throws IOException {
        return matchPayload;
    }


    @Override
    public boolean isPayloadAvailable () throws IOException {
        return !matchPayload.isEmpty();
    }


    @Override
    public String toString () {
        return getClass().getName()
                + "("
                + query.toString()
                + ")@"
                + (isStartEnumeration ? "START" : (hasMoreSpans ? (doc() + ":"
                        + start() + "-" + end()) : "END"));
    }

}
