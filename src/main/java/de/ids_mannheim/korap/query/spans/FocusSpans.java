package de.ids_mannheim.korap.query.spans;

import static de.ids_mannheim.korap.util.KrillByte.byte2int;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.index.TermState;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.util.Bits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ids_mannheim.korap.query.SpanFocusQuery;


/**
 * originalSpans, that can focus on the span boundaries of classed
 * subqueries.
 * The boundaries of the classed subquery may exceed the boundaries of
 * the
 * nested query.
 * 
 * In case multiple classes are found with the very same number, the
 * span is
 * maximized to start on the first occurrence from the left and end on
 * the last
 * occurrence on the right.
 * 
 * In case the class to focus on is not found in the payloads, the
 * match is
 * ignored.
 * 
 * <strong>Warning</strong>: Payloads other than class payloads won't
 * bubble up
 * currently. That behaviour may change in the futures
 * 
 * @author diewald
 */

public class FocusSpans extends SimpleSpans {
    private List<Byte> classNumbers;
    private SpanQuery query;
    private final Logger log = LoggerFactory.getLogger(FocusSpans.class);

    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;

    private boolean isSorted, matchTemporaryClass, removeTemporaryClasses;
    private List<CandidateSpan> candidateSpans;
    private int windowSize = 10;
    private int currentDoc;


    /**
     * Construct a FocusSpan for the given {@link SpanQuery}.
     * 
     * @param query
     *            A {@link SpanQuery}.
     * @param context
     *            The {@link AtomicReaderContext}.
     * @param acceptDocs
     *            Bit vector representing the documents
     *            to be searched in.
     * @param termContexts
     *            A map managing {@link TermState TermStates}.
     * @param number
     *            The class number to focus on.
     * @throws IOException
     */
    public FocusSpans (SpanFocusQuery query, AtomicReaderContext context,
                       Bits acceptDocs, Map<Term, TermContext> termContexts)
            throws IOException {
        super(query, context, acceptDocs, termContexts);
        if (query.getClassNumbers() == null) {
            throw new IllegalArgumentException(
                    "At least one class number must be specified.");
        }
        classNumbers = query.getClassNumbers();
        isSorted = query.isSorted();
        matchTemporaryClass = query.matchTemporaryClass();
        removeTemporaryClasses = query.removeTemporaryClasses();
        candidateSpans = new ArrayList<CandidateSpan>();
        hasMoreSpans = firstSpans.next();
        currentDoc = firstSpans.doc();

        this.query = query;
        if (getSpanId() > 0) {
            hasSpanId = true;
        }
    }


    @Override
    public boolean next () throws IOException {
        matchPayload.clear();
        CandidateSpan cs;
        while (hasMoreSpans || candidateSpans.size() > 0) {
            if (isSorted) {

                if (firstSpans.isPayloadAvailable()
                        && updateSpanPositions(cs = new CandidateSpan(
                                firstSpans))) {
                    setMatch(cs);
                    hasMoreSpans = firstSpans.next();
                    return true;
                }
                hasMoreSpans = firstSpans.next();
            }
            else if (candidateSpans.isEmpty()) {
                currentDoc = firstSpans.doc();
                collectCandidates();
                Collections.sort(candidateSpans);
            }
            else {
                setMatch(candidateSpans.get(0));
                candidateSpans.remove(0);
                return true;
            }
        }

        return false;
    }


    private void collectCandidates () throws IOException {
        CandidateSpan cs = null;
        while (hasMoreSpans && candidateSpans.size() < windowSize
                && firstSpans.doc() == currentDoc) {

            if (firstSpans.isPayloadAvailable()
                    && updateSpanPositions(cs = new CandidateSpan(firstSpans))) {
                candidateSpans.add(cs);
            }
            hasMoreSpans = firstSpans.next();
        }
    }


    private void setMatch (CandidateSpan cs) {
        matchStartPosition = cs.getStart();
        matchEndPosition = cs.getEnd();
        matchDocNumber = cs.getDoc();
        matchPayload.addAll(cs.getPayloads());
        setSpanId(cs.getSpanId());
    }


    private boolean updateSpanPositions (CandidateSpan candidateSpan)
            throws IOException {
        int minPos = 0, maxPos = 0;
        int classStart, classEnd;
        boolean isStart = true;
        boolean isClassFound = false;

        candidateSpan.getPayloads().clear();

        // Iterate over all payloads and find the maximum span per class
        for (byte[] payload : firstSpans.getPayload()) {
            // No class payload - ignore
            // this may be problematic for other calculated payloads!
            if ((!matchTemporaryClass && payload.length == 9)
                    || (matchTemporaryClass && payload.length == 10)) {
                if (classNumbers.contains(payload[8])) {
                    isClassFound = true;
                    classStart = byte2int(payload, 0);
                    classEnd = byte2int(payload, 4);

                    if (isStart || classStart < minPos) {
                        minPos = classStart;
                        isStart = false;
                    }
                    if (classEnd > maxPos) {
                        maxPos = classEnd;
                    }
                }
            }

            if (removeTemporaryClasses && payload.length == 10) {
                continue;
            }
            candidateSpan.getPayloads().add(payload.clone());
        }

        if (isClassFound) {
            candidateSpan.start = minPos;
            candidateSpan.end = maxPos;
        }

        return isClassFound;
    }


    // Todo: Check for this on document boundaries!
    @Override
    public boolean skipTo (int target) throws IOException {
        if (this.doc() < target && firstSpans.skipTo(target)) {
            return next();
        }
        return false;
    };


    @Override
    public String toString () {
        return getClass().getName() + "(" + this.query.toString() + ")@"
                + (doc() + ":" + start() + "-" + end());
    };


    @Override
    public long cost () {
        return firstSpans.cost();
    };
};
