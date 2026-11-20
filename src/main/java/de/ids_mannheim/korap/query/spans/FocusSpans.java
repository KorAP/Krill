package de.ids_mannheim.korap.query.spans;

import static de.ids_mannheim.korap.util.KrillByte.byte2int;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermState;
import org.apache.lucene.queries.spans.SpanQuery;
import org.apache.lucene.util.Bits;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ids_mannheim.korap.constants.RelationDirection;
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

    // Logger
    private final Logger log = LoggerFactory.getLogger(FocusSpans.class);
    
    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;

    private boolean isSorted, matchTemporaryClass, removeTemporaryClasses;
    // private List<CandidateSpan> candidateSpans;
    private int windowSize;
    private int currentDoc;
    private int prevStart;
    private int prevDoc;
    private PriorityQueue<CandidateSpan> candidates;
    private CandidateSpanComparator comparator;

    /**
     * Construct a FocusSpan for the given {@link SpanQuery}.
     * 
     * @param query
     *            A {@link SpanQuery}.
     * @param context
     *            The {@link LeafReaderContext}.
     * @param acceptDocs
     *            Bit vector representing the documents
     *            to be searched in.
     * @param weights
     *            Creates the spans of the operands.
     * @param number
     *            The class number to focus on.
     * @throws IOException
     */
    public FocusSpans (SpanFocusQuery query, LeafReaderContext context,
                       Bits acceptDocs, SpanWeights weights)
            throws IOException {
        super(query, context, acceptDocs, weights);
        if (query.getClassNumbers() == null) {
            throw new IllegalArgumentException(
                    "At least one class number must be specified.");
        }
        classNumbers = query.getClassNumbers();
        windowSize = query.getWindowSize();
        isSorted = query.isSorted();
        matchTemporaryClass = query.matchTemporaryClass();
        removeTemporaryClasses = query.removeTemporaryClasses();

        candidates = new PriorityQueue<>(windowSize, comparator);

        this.query = query;
    }


    @Override
    protected void startDocument () {
        candidates.clear();
        currentDoc = firstSpans.docID();
    };


    @Override
    protected boolean nextMatchInDocument () throws IOException {
        matchPayload.clear();
        spanId = 0;
        CandidateSpan cs;

        if (DEBUG) {
            log.debug("FirstSpan [{}]", firstSpans.toString());
            log.debug("FirstSpan: ({}) {}-{}", firstSpans.docID(), firstSpans.startPosition(), firstSpans.endPosition());
        }
        
        while (hasMoreSpans || candidates.size() > 0) {
            if (isSorted) {

                if (firstSpans.isPayloadAvailable() && updateSpanPositions(
                        cs = new CandidateSpan(firstSpans))) {
                    setMatch(cs);
                    hasMoreSpans = nextInDocument(firstSpans);
                    return true;
                }
                hasMoreSpans = nextInDocument(firstSpans);
            }
            else if (candidates.isEmpty()) {
                currentDoc = firstSpans.docID();
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

    private void collectCandidates () throws IOException {
        CandidateSpan cs = null;
        CandidateSpan head = null;

        while (hasMoreSpans && firstSpans.docID() == currentDoc) {
            if (head == null) {
                head = candidates.peek();
            }
            else if (head.getStart() < firstSpans.startPosition()) {
                break;
            }

            if (firstSpans.isPayloadAvailable() && updateSpanPositions(
                    cs = new CandidateSpan(firstSpans))) {
                if (cs.getDoc() == prevDoc && cs.getStart() < prevStart) {
                    if (DEBUG) {
                        log.debug("Span (" + cs.getStart() + ", " + cs.getEnd()
                                + ") is out of order and skipped.");
                    }
                }
                else {
                    candidates.add(cs);
                }
            }
            hasMoreSpans = nextInDocument(firstSpans);
        }
    }

    private void setMatch (CandidateSpan cs) {
        matchStartPosition = cs.getStart();
        prevStart = matchStartPosition;
        matchEndPosition = cs.getEnd();
        matchDocNumber = cs.getDoc();
        prevDoc = matchDocNumber;
        matchPayload.addAll(cs.getPayloads());

        if (firstSpans instanceof RelationSpans && classNumbers.size() == 1) {
            RelationSpans relationSpans = (RelationSpans) firstSpans;
            RelationDirection direction = relationSpans.getDirection();

            if (classNumbers.get(0) == relationSpans.getTempSourceNum()) {
                if (direction.equals(RelationDirection.RIGHT)) {
                    setSpanId(relationSpans.getLeftId());
                }
                else {
                    setSpanId(relationSpans.getRightId());
                }
            }
            else if (classNumbers.get(0) == relationSpans.getTempTargetNum()) {
                if (direction.equals(RelationDirection.RIGHT)) {
                    setSpanId(relationSpans.getRightId());
                }
                else {
                    setSpanId(relationSpans.getLeftId());
                }
            }
            // else {
            // throw new
            // IllegalArgumentException("Classnumber is not found.");
            // }
            if (spanId > 0) {
                hasSpanId = true;
            }
        }
        else if (cs.getSpanId() > 0) {
            setSpanId(cs.getSpanId());
            hasSpanId = true;
        }

    }

    private boolean updateSpanPositions (CandidateSpan candidateSpan)
            throws IOException {
        int minPos = 0, maxPos = 0;
        int classStart, classEnd;
        boolean isStart = true;
        boolean isClassFound = false;

        candidateSpan.getPayloads().clear();

        // Iterate over all payloads and find the maximum span per
        // class
        for (byte[] payload : firstSpans.getPayload()) {
            // No class payload - ignore
            // this may be problematic for other calculated payloads!

            if ((!matchTemporaryClass && payload.length == 10)
                    || (matchTemporaryClass && payload.length == 11)) {

                if (payload[0] == 0) {
                    if (classNumbers.contains(payload[9])) {
                        isClassFound = true;
                        classStart = byte2int(payload, 1);
                        classEnd = byte2int(payload, 5);

                        if (isStart || classStart < minPos) {
                            minPos = classStart;
                            isStart = false;
                        }
                        if (classEnd > maxPos) {
                            maxPos = classEnd;
                        }
                    }

                    if (removeTemporaryClasses) {
                        continue;
                    };
                }
            }

            // Remove span elements
            else if ((payload[0] & (byte) 64) != 0) {
                continue;
            };

            if (// payload.length == 8 ||
            (removeTemporaryClasses && payload.length == 11)) {
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
    public String toString () {
        return getClass().getName() + "(" + this.query.toString() + ")@"
                + (docID() + ":" + startPosition() + "-" + endPosition());
    };

    @Override
    public long cost () {
        return firstSpans.cost();
    };
};
