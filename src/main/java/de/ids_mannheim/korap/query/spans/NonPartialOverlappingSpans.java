package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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
    }


    // The first span of the document is not processed yet
    private boolean atFirstSpan;


    @Override
    protected boolean usesIteratorApi () {
        return true;
    };


    @Override
    protected List<? extends LegacySpans> requiredOperands () {
        return Collections.singletonList(secondSpans);
    };


    @Override
    protected void startDocument () throws IOException {
        if (hasMoreSpans)
            hasMoreSpans = nextInDocument(secondSpans);
        atFirstSpan = true;
    };


    @Override
    protected boolean nextMatchInDocument () throws IOException {
        // Warning: this does not work for overlapping spans 
        // e.g. get multiple second spans in a firstspan
        if (!atFirstSpan && hasMoreSpans)
            hasMoreSpans = nextInDocument(firstSpans);
        atFirstSpan = false;
        isStartEnumeration = false;
        matchPayload.clear();

        // The complexity is linear for searching in a document. 
        // It's better if we can skip to >= position in a document.
        while (hasMoreSpans) {
            int matchCase = findMatch();
            if (matchCase == 0) {
                doCollectPayloads();
                return true;
            }
            else if (matchCase == 1) {
                hasMoreSpans = nextInDocument(secondSpans);
            }
            else {
                hasMoreSpans = nextInDocument(firstSpans);
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
    public long cost () {
        return firstSpans.cost() + secondSpans.cost();
    }
}
