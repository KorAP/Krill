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

import de.ids_mannheim.korap.query.SpanSegmentQuery;

/**
 * SegmentSpans is an enumeration of Span matches in which that two
 * child spans have exactly the same start and end positions.
 * 
 * @author margaretha
 */
public class SegmentSpans extends SimpleSpans {

    private boolean isRelation;


    /**
     * Creates SegmentSpans from the given {@link SpanSegmentQuery}.
     * 
     * @param spanSegmentQuery
     *            a spanSegmentQuery.
     * @param context
     * @param acceptDocs
     * @param weights
     * @throws IOException
     */
    public SegmentSpans (SpanSegmentQuery spanSegmentQuery,
                         LeafReaderContext context, Bits acceptDocs,
                         SpanWeights weights)
            throws IOException {
        super(spanSegmentQuery, context, acceptDocs, weights);
        if (spanSegmentQuery.isRelation()) {
            isRelation = true;
        }

        collectPayloads = true;
    }




    // The first span of the document is not processed yet
    private boolean atFirstSpan;


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
     * Check weather the start and end positions of the current
     * firstspan and
     * secondspan are identical.
     * 
     */
    protected int findMatch () {
        if (firstSpans.startPosition() == secondSpans.startPosition()
                && firstSpans.endPosition() == secondSpans.endPosition()) {

            if (isRelation) {
                RelationSpans s1 = (RelationSpans) firstSpans;
                if (secondSpans instanceof SimpleSpans) {
                    SimpleSpans s2 = (SimpleSpans) secondSpans;

                    if (s2.hasSpanId) {
                        if (s1.getLeftId() == s2.getSpanId()) {
                            // setSpanId(s2.getSpanId());
                            setMatch();
                            return 0;
                        }
                    }
                    else {
                        setMatch();
                        return 0;
                    }
                }
                else {
                    setMatch();
                    return 0;
                }
            }
            else if (firstSpans instanceof FocusSpans
                    && secondSpans instanceof SimpleSpans) {
                SimpleSpans s1 = (SimpleSpans) firstSpans;
                SimpleSpans s2 = (SimpleSpans) secondSpans;

                if (s1.hasSpanId && s2.hasSpanId) {
                    if (s1.getSpanId() == s2.getSpanId()) {
                        setSpanId(s2.getSpanId());
                        hasSpanId = true;
                    }
                }
                else {
                    setMatch();
                    return 0;
                }
            }
            else {
                setMatch();
                return 0;
            }
        }

        if (firstSpans.startPosition() < secondSpans.startPosition()
                || firstSpans.endPosition() < secondSpans.endPosition())
            return -1;

        return 1;
    }


    private void setMatch () {
        matchDocNumber = firstSpans.docID();
        matchStartPosition = firstSpans.startPosition();
        matchEndPosition = firstSpans.endPosition();
    }


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
