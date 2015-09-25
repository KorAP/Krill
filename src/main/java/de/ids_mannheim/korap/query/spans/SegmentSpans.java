package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.SpanSegmentQuery;

/**
 * SegmentSpans is an enumeration of Span matches in which that two
 * child spans
 * have exactly the same start and end positions.
 * 
 * @author margaretha
 * */
public class SegmentSpans extends SimpleSpans {

    private boolean isRelation;


    /**
     * Creates SegmentSpans from the given {@link SpanSegmentQuery}.
     * 
     * @param spanSegmentQuery
     *            a spanSegmentQuery.
     * @param context
     * @param acceptDocs
     * @param termContexts
     * @throws IOException
     */
    public SegmentSpans (SpanSegmentQuery spanSegmentQuery,
                         LeafReaderContext context, Bits acceptDocs,
                         Map<Term, TermContext> termContexts)
            throws IOException {
        super(spanSegmentQuery, context, acceptDocs, termContexts);
        if (spanSegmentQuery.isRelation()) {
            isRelation = true;
        }

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
     *         <code>false</code> otherwise.
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
     * Check weather the start and end positions of the current
     * firstspan and
     * secondspan are identical.
     * 
     * */
    protected int findMatch () {
        RelationSpans s1;
        SimpleSpans s2;

        if (firstSpans.start() == secondSpans.start()
                && firstSpans.end() == secondSpans.end()) {

            if (isRelation) {
                s1 = (RelationSpans) firstSpans;
                s2 = (SimpleSpans) secondSpans;

                if (s2.hasSpanId) {
                    if (s1.getLeftId() == s2.getSpanId()) {
                        setSpanId(s2.getSpanId());
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

        if (firstSpans.start() < secondSpans.start()
                || firstSpans.end() < secondSpans.end())
            return -1;

        return 1;
    }


    private void setMatch () {
        matchDocNumber = firstSpans.doc();
        matchStartPosition = firstSpans.start();
        matchEndPosition = firstSpans.end();
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
