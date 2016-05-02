package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.Map;
import java.util.ArrayList;
import java.util.PriorityQueue;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.SpanSubspanQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enumeration of SubSpans, which are parts of another Spans. The
 * SubSpans are specified with a start offset relative to the original
 * span and a length. If the length is unspecified or 0, the end
 * position of the subspans is the same as that of the original spans.
 * 
 * @author margaretha
 * @author diewald
 * 
 */
public class SubSpans extends SimpleSpans {

    // Logger
    private final Logger log = LoggerFactory.getLogger(SubSpans.class);

    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;

    private int startOffset, length;
    private int windowSize;
    private int currentDoc;
    private int prevStart;
    private int prevDoc;
    private PriorityQueue<CandidateSpan> candidates;
    private CandidateSpanComparator comparator;

    /**
     * Constructs SubSpans for the given {@link SpanSubspanQuery}
     * specifiying the start offset and the length of the subspans.
     * 
     * @param subspanQuery
     *            a SpanSubspanQuery
     * @param context
     * @param acceptDocs
     * @param termContexts
     * @throws IOException
     */
    public SubSpans (SpanSubspanQuery subspanQuery, LeafReaderContext context,
                     Bits acceptDocs, Map<Term, TermContext> termContexts)
            throws IOException {
        super(subspanQuery, context, acceptDocs, termContexts);
        this.startOffset = subspanQuery.getStartOffset();
        this.length = subspanQuery.getLength();
        this.matchPayload = new ArrayList<byte[]>(6);
        this.windowSize = subspanQuery.getWindowSize();
        candidates = new PriorityQueue<>(windowSize, comparator);

        if (DEBUG) {
            log.trace("Init SubSpan at {} with length {}", this.startOffset, this.length);
        };
        hasMoreSpans = firstSpans.next();
    }


    @Override
    public boolean next () throws IOException {
        isStartEnumeration = false;
        return advance();
    }


    /**
     * Advances the SubSpans to the next match.
     * 
     * @return <code>true</code> if a match is found,
     *         <code>false</code> otherwise.
     * @throws IOException
     */
    private boolean advance () throws IOException {
        while (hasMoreSpans || candidates.size() > 0) {
            CandidateSpan cs = new CandidateSpan(firstSpans);
            if (startOffset > 0) {
                if (findMatch(cs)) {
                    setMatch(cs);
                    hasMoreSpans = firstSpans.next();
                    return true;
                }
                hasMoreSpans = firstSpans.next();
            }
            else if (candidates.isEmpty()) {
                currentDoc = firstSpans.doc();
                collectCandidates();
            }
            else {
                setMatch(candidates.poll());
                collectCandidates();
                return true;
            }
        }
        return false;
    }

    private void collectCandidates() throws IOException {

        while (hasMoreSpans && candidates.size() < windowSize
                && firstSpans.doc() == currentDoc) {
            CandidateSpan cs;
            if (findMatch(cs = new CandidateSpan(firstSpans))) {
                if (cs.getDoc() == prevDoc && cs.getStart() < prevStart) {
                    log.warn("Span (" + cs.getStart() + ", " + cs.getEnd()
                            + ") is out of order and skipped.");
                }
                else {
                    candidates.add(cs);
                }
            }
            hasMoreSpans = firstSpans.next();
        }
    }

    /**
     * Sets the properties of the current match/subspan.
     * 
     * @throws IOException
     */
    public boolean findMatch(CandidateSpan cs) throws IOException {

        // Check at span ending
        if (this.startOffset < 0) {
            cs.setStart(firstSpans.end() + startOffset);
            if (cs.getStart() < firstSpans.start()) {
                cs.setStart(firstSpans.start());
            };
        }
        // Check at span beginning
        else {
            cs.setStart(firstSpans.start() + startOffset);
            if (cs.getStart() >= firstSpans.end()) {
                return false;
            }
        }

        // Find end position of span
        if (this.length > 0) {
            cs.setEnd(cs.getStart() + this.length);
            if (cs.getEnd() > firstSpans.end()) {
                cs.setEnd(firstSpans.end());
            }
        }
        else {
            cs.setEnd(firstSpans.end());
        }

        // matchPayload.clear();

        // Remove element payloads
        for (byte[] payload : firstSpans.getPayload()) {
            if ((payload[0] & ((byte) 64)) != 0) {
                continue;
            };
            cs.getPayloads().add(payload.clone());
        };

        cs.setDoc(firstSpans.doc());

        if (DEBUG) {
            log.trace("Start at absolute position {} " +
                      "and end at absolute position {}",
                    cs.getStart(),
                    cs.getEnd());
        };

        return true;
    }

    private void setMatch(CandidateSpan cs) {
        matchStartPosition = cs.getStart();
        prevStart = matchStartPosition;
        matchEndPosition = cs.getEnd();
        matchDocNumber = cs.getDoc();
        prevDoc = matchDocNumber;
        matchPayload.clear();
        matchPayload.addAll(cs.getPayloads());
    }

    @Override
    public boolean skipTo (int target) throws IOException {
        if (candidates.size() > 0) {
            CandidateSpan cs;
            while ((cs = candidates.poll()) != null) {
                if (cs.getDoc() == target) {
                    return next();
                }
            }
        }
        if (firstSpans.doc() == target) {
            return next();
        }
        if (firstSpans.doc() < target && firstSpans.skipTo(target)) {
            return next();
        }
        return advance();
    }


    @Override
    public long cost () {
        return firstSpans.cost() + 1;
    }

}
