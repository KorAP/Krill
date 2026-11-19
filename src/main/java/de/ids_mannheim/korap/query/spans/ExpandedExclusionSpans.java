package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.SpanExpansionQuery;

/**
 * Enumeration of Spans expanded with minimum <code>m</code> and
 * maximum
 * <code>n</code> tokens, and throughout all the expansions do
 * <em>not</em>
 * contain a specific Spans (notClause). See examples in
 * {@link SpanExpansionQuery}.
 * 
 * The expansion direction is designated with the sign of a number,
 * namely a
 * negative number signifies the expansion to the <em>left</em> of the
 * original
 * span, and a positive number (including 0) signifies the expansion
 * to the
 * <em>right</em> of the original span.
 * 
 * The expansion offsets, namely the start and end positions of an
 * expansion part, are stored in payloads. A class number is assigned
 * to the offsets grouping them altogether.
 * 
 * @author margaretha
 */
public class ExpandedExclusionSpans extends SimpleSpans {

    private int min, max;
    private int direction;
    private byte classNumber;
    private List<CandidateSpan> candidateSpans;
    private boolean hasMoreNotClause;
    private LegacySpans notClause;

    private long matchCost;

    /**
     * Constructs ExpandedExclusionSpans from the given
     * {@link SpanExpansionQuery}.
     * 
     * @param spanExpansionQuery
     *            a SpanExpansionQuery
     * @param context
     * @param acceptDocs
     * @param weights
     * @throws IOException
     */
    public ExpandedExclusionSpans (SpanExpansionQuery spanExpansionQuery,
                                   LeafReaderContext context, Bits acceptDocs,
                                   SpanWeights weights)
            throws IOException {
        super(spanExpansionQuery, context, acceptDocs, weights);

        if (spanExpansionQuery.getSecondClause() == null) {
            throw new IllegalArgumentException("The SpanExpansionQuery "
                    + "is not valid. The spanquery to exclude (notClause) cannot "
                    + "be null.");
        }

        /*
         * if (spanExpansionQuery.getMin() < 1){ throw new
         * IllegalArgumentException("Min occurrence for notClause " +
         * "must be at least 1."); }
         */

        this.min = spanExpansionQuery.getMin();
        this.max = spanExpansionQuery.getMax();
        this.direction = spanExpansionQuery.getDirection();
        this.classNumber = spanExpansionQuery.getClassNumber();

        this.notClause = secondSpans;
        this.hasMoreNotClause = this.advanceNotClause(0);

        candidateSpans = new ArrayList<CandidateSpan>();
    }



    @Override
    protected void startDocument () {
        candidateSpans.clear();
    };


    @Override
    /**
     * Advances the ExpandedExclusionSpans to the next match.
     * 
     * @return <code>true</code> if a match is found,
     *         <code>false</code>
     *         otherwise.
     * @throws IOException
     */
    protected boolean nextMatchInDocument () throws IOException {
        matchPayload.clear();
        isStartEnumeration = false;
        while (hasMoreSpans || candidateSpans.size() > 0) {
            if (candidateSpans.size() > 0) {
                // set a candidate span as a match
                CandidateSpan cs = candidateSpans.get(0);
                matchDocNumber = cs.getDoc();
                matchStartPosition = cs.getStart();
                matchEndPosition = cs.getEnd();
                matchPayload = cs.getPayloads();
                matchCost = cs.getCost() + notClause.cost();
                candidateSpans.remove(0);
                return true;
            }
            else if (!hasMoreNotClause || notClause.docID() > firstSpans.docID()) {
                generateCandidates(min, max, direction);
                hasMoreSpans = nextInDocument(firstSpans);
            }
            else
                findMatches();
        }
        return false;
    }

    /**
     * Move the notClause to its first match in the target document
     * or a following one.
     * 
     * @return <code>true</code> if there is such a match.
     */
    private boolean advanceNotClause (int target) throws IOException {
        if (notClause.advance(target) == NO_MORE_DOCS)
            return false;
        // A document of the spans has at least one match
        notClause.nextStartPosition();
        return true;
    }


    /**
     * Move the notClause to its next match, in the current document
     * or a following one.
     * 
     * @return <code>true</code> if there is such a match.
     */
    private boolean nextNotClause () throws IOException {
        if (notClause.nextStartPosition() != NO_MORE_POSITIONS)
            return true;
        if (notClause.nextDoc() == NO_MORE_DOCS)
            return false;
        notClause.nextStartPosition();
        return true;
    }


    /**
     * Finds matches by expanding the firstspans either to the left or
     * to the
     * right.
     * 
     * @throws IOException
     */
    private void findMatches () throws IOException {
        while (hasMoreNotClause && notClause.docID() <= firstSpans.docID()) {
            if (notClause.docID() == firstSpans.docID()) {
                if (direction < 0) { // left
                    expandLeft();
                } // right
                else {
                    expandRight();
                }
                break;
            }
            else
                hasMoreNotClause = this.advanceNotClause(firstSpans.docID());
        }
    }

    /**
     * Expands the firstspans to the left.
     * 
     * @throws IOException
     */
    private void expandLeft () throws IOException {
        // int counter = max;
        int maxPos = max;
        CandidateSpan lastNotClause = null;
        while (hasMoreNotClause && 
                notClause.docID() == firstSpans.docID() && 
                notClause.startPosition() < firstSpans.startPosition()) {

            // between max and firstspan.startPosition()
            if (notClause.startPosition() >= firstSpans.startPosition() - maxPos) {
                maxPos = firstSpans.startPosition() - notClause.startPosition() - 1;
                lastNotClause = new CandidateSpan(notClause);
                // counter--;
            }
            hasMoreNotClause = this.nextNotClause();
        }

        // if a notClause is between max and firstspan.start,
        // then maxPos = last NotClause pos -1
        generateCandidates(min, maxPos, direction);

        if (lastNotClause != null && hasMoreNotClause)
            while ((hasMoreSpans = nextInDocument(firstSpans))
                    // the next notClause is not in between max and
                    // firstspan.startPosition()
                    && notClause.startPosition() > firstSpans.startPosition()
                    // the last notClause is in between max and
                    // firstspan.startPosition()
                    && lastNotClause.getStart() < firstSpans.startPosition()
                    && lastNotClause.getStart() >= firstSpans.startPosition() - max) {

                maxPos = firstSpans.startPosition() - lastNotClause.getStart() - 1;
                generateCandidates(min, maxPos, direction);
            }
        else {
            hasMoreSpans = nextInDocument(firstSpans);
        }
    }

    /**
     * Expands the firstspans to the right.
     * 
     * @throws IOException
     */
    private void expandRight () throws IOException {
        int expansionEnd = firstSpans.endPosition() + max;
        int maxPos = max;
        boolean isFound = false;

        CandidateSpan firstNotClause = null;
        // System.out.println("main start:"+firstSpans.startPosition());
        while (hasMoreNotClause && notClause.docID() == firstSpans.docID() &&
                notClause.startPosition() < expansionEnd) {
            // between firstspan.endPosition() and expansionEnd
            if (!isFound && notClause.startPosition() >= firstSpans.endPosition()) {
                maxPos = notClause.startPosition() - firstSpans.endPosition() - 1;
                firstNotClause = new CandidateSpan(notClause);
                isFound = true;
            }
            hasMoreNotClause = this.nextNotClause();
        }
        // if a notClause is between firstSpan.end and max
        // then maxPos = the first notClause pos -1
        generateCandidates(min, maxPos, direction);

        if (firstNotClause != null) {
            while ((hasMoreSpans = nextInDocument(firstSpans))
                    // in between
                    && firstNotClause.getStart() < firstSpans.endPosition() + max
                    && firstNotClause.getStart() >= firstSpans.endPosition()) {
                // System.out.println("first
                // start:"+firstNotClause.getStart()+", main
                // start:"+firstSpans.startPosition());
                maxPos = firstNotClause.getStart() - firstSpans.endPosition() - 1;
                generateCandidates(min, maxPos, direction);
            }
        }
        else {
            hasMoreSpans = nextInDocument(firstSpans);
        }
    }

    /**
     * Creates new candidate matches for the given direction, minimum
     * and
     * maximum positions.
     * 
     * @param minPos
     *            minimum position
     * @param maxPos
     *            maximum position
     * @param direction
     *            the expansion direction
     * @throws IOException
     */
    private void generateCandidates (int minPos, int maxPos, int direction)
            throws IOException {
        int counter;
        int start, end;
        CandidateSpan cs;
        if (direction < 0) { // left
            counter = maxPos;
            while (counter >= min) {
                start = Math.max(0, firstSpans.startPosition() - counter);
                if (start > -1) {
                    end = firstSpans.endPosition();
                    // System.out.println(start+","+end);
                    cs = new CandidateSpan(start, end, firstSpans.docID(),
                            firstSpans.cost(),
                            createPayloads(start, firstSpans.startPosition()));
                    candidateSpans.add(cs);
                }
                counter--;
            }
        }
        else { // right
            counter = minPos;
            while (counter <= maxPos) {
                start = firstSpans.startPosition();
                end = firstSpans.endPosition() + counter;
                // System.out.println(start+","+end);

                cs = new CandidateSpan(start, end, firstSpans.docID(),
                        firstSpans.cost(),
                        createPayloads(firstSpans.endPosition(), end));
                candidateSpans.add(cs);
                counter++;
            }
        }
    }

    /**
     * Creates payloads for a candiate match by copying the payloads
     * of the
     * firstspans, and adds expansion offsets with the given start and
     * end
     * positions to the payloads, if the class number is set.
     * 
     * @param start
     *            the start offset
     * @param end
     *            the end offset
     * @return payloads
     * @throws IOException
     */
    private ArrayList<byte[]> createPayloads (int start, int end)
            throws IOException {

        ArrayList<byte[]> payload = new ArrayList<byte[]>();

        if (firstSpans.isPayloadAvailable()) {
            payload.addAll(firstSpans.getPayload());
        }
        if (classNumber > 0) {
            // System.out.println("Extension offsets "+start+","+end);
            payload.add(createExtensionPayloads(start, end));
        }
        return payload;
    }

    /**
     * Generates a byte array of extension offsets and class number to
     * be added
     * into the payloads.
     * 
     * @param start
     *            the start offset
     * @param end
     *            the end offset
     * @return a byte array of extension offsets and class number
     */
    private byte[] createExtensionPayloads (int start, int end) {
        ByteBuffer buffer = ByteBuffer.allocate(10);
        Byte classPTI = 0;
        buffer.put(classPTI);
        buffer.putInt(start);
        buffer.putInt(end);
        buffer.put(classNumber);
        return buffer.array();
    }


    @Override
    public long cost () {
        return matchCost;
    }
}
