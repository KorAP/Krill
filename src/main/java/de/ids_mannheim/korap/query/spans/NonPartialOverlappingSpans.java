package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.SimpleSpanQuery;

/**
 * An abstract class for Span enumeration whose two child spans are
 * matched by their positions and do not have a partial overlap.
 * 
 * @author margaretha
 */
public abstract class NonPartialOverlappingSpans extends SimpleSpans {

    /**
     * Constructs NonPartialOverlappingSpans from the given
     * {@link SimpleSpanQuery}.
     * 
     * @param simpleSpanQuery
     *            a SimpleSpanQuery
     * @param context
     * @param acceptDocs
     * @param termContexts
     * @throws IOException
     */
    public NonPartialOverlappingSpans (SimpleSpanQuery simpleSpanQuery,
                                       LeafReaderContext context,
                                       Bits acceptDocs,
                                       Map<Term, TermContext> termContexts)
            throws IOException {
        super(simpleSpanQuery, context, acceptDocs, termContexts);

        // Warning: not implemented, results in errors for SpanNextQuery 
        // This.collectPayloads = simpleSpanQuery.isCollectPayloads()
        collectPayloads = true;
        hasMoreSpans = secondSpans.next();

    }


    @Override
    public boolean next () throws IOException {
        // Warning: this does not work for overlapping spans 
        // e.g. get multiple second spans in a firstspan
        hasMoreSpans &= firstSpans.next();
        isStartEnumeration = false;
        matchPayload.clear();
        return advance();
    }


    /**
     * Advances to the next match.
     * 
     * @return <code>true</code> if a match is found,
     *         <code>false</code>
     *         otherwise.
     * @throws IOException
     */
    protected boolean advance () throws IOException {
        // The complexity is linear for searching in a document. 
        // It's better if we can skip to >= position in a document.
        while (hasMoreSpans && ensureSameDoc(firstSpans, secondSpans)) {
            int matchCase = findMatch();
            if (matchCase == 0) {
                doCollectPayloads();
                return true;
            }
            else if (matchCase == 1) {
                hasMoreSpans = secondSpans.next();
            }
            else {
                hasMoreSpans = firstSpans.next();
            }
        }
        return false;
    }


    /**
     * Specifies the condition for a match.
     * 
     * @return 0 iff match is found, -1 to advance the firstspan, 1 to
     *         advance
     *         the secondspan
     */
    protected abstract int findMatch ();


    /**
     * Collects available payloads from the current first and second
     * spans.
     * 
     * @throws IOException
     */
    private void doCollectPayloads () throws IOException {
        Collection<byte[]> payload;
        if (collectPayloads) {
            if (firstSpans.isPayloadAvailable()) {
                payload = firstSpans.getPayload();
                matchPayload.addAll(payload);
            }
            if (secondSpans.isPayloadAvailable()) {
                payload = secondSpans.getPayload();
                matchPayload.addAll(payload);
            }
        }
    }


    @Override
    public boolean skipTo (int target) throws IOException {
        if (hasMoreSpans && (firstSpans.doc() < target)) {
            if (!firstSpans.skipTo(target)) {
                hasMoreSpans = false;
                return false;
            }
        }
        matchPayload.clear();
        return advance();
    }


    @Override
    public long cost () {
        return firstSpans.cost() + secondSpans.cost();
    }
}
