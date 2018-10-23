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
    private Spans notClause;

    private long matchCost;

    /**
     * Constructs ExpandedExclusionSpans from the given
     * {@link SpanExpansionQuery}.
     * 
     * @param spanExpansionQuery
     *            a SpanExpansionQuery
     * @param context
     * @param acceptDocs
     * @param termContexts
     * @throws IOException
     */
    public ExpandedExclusionSpans (SpanExpansionQuery spanExpansionQuery,
                                   LeafReaderContext context, Bits acceptDocs,
                                   Map<Term, TermContext> termContexts)
            throws IOException {
        super(spanExpansionQuery, context, acceptDocs, termContexts);

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
        this.hasMoreNotClause = notClause.next();

        candidateSpans = new ArrayList<CandidateSpan>();
        hasMoreSpans = firstSpans.next();
    }

    @Override
    public boolean next () throws IOException {
        matchPayload.clear();
        isStartEnumeration = false;
        return advance();
    }

    /**
     * Advances the ExpandedExclusionSpans to the next match.
     * 
     * @return <code>true</code> if a match is found,
     *         <code>false</code>
     *         otherwise.
     * @throws IOException
     */
    private boolean advance () throws IOException {
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
            else if (!hasMoreNotClause || notClause.doc() > firstSpans.doc()) {
                generateCandidates(min, max, direction);
                hasMoreSpans = firstSpans.next();
            }
            else
                findMatches();
        }
        return false;
    }

    /**
     * Finds matches by expanding the firstspans either to the left or
     * to the
     * right.
     * 
     * @throws IOException
     */
    private void findMatches () throws IOException {
        while (hasMoreNotClause && notClause.doc() <= firstSpans.doc()) {
            if (notClause.doc() == firstSpans.doc()) {
                if (direction < 0) { // left
                    expandLeft();
                } // right
                else {
                    expandRight();
                }
                break;
            }
            else if (!notClause.next()) hasMoreNotClause = false;
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
                notClause.doc() == firstSpans.doc() && 
                notClause.start() < firstSpans.start()) {

            // between max and firstspan.start()
            if (notClause.start() >= firstSpans.start() - maxPos) {
                maxPos = firstSpans.start() - notClause.start() - 1;
                lastNotClause = new CandidateSpan(notClause);
                // counter--;
            }
            if (!notClause.next()) {
                hasMoreNotClause = false;
            }
        }

        // if a notClause is between max and firstspan.start,
        // then maxPos = last NotClause pos -1
        generateCandidates(min, maxPos, direction);

        if (lastNotClause != null && hasMoreNotClause)
            while ((hasMoreSpans = firstSpans.next())
                    // the next notClause is not in between max and
                    // firstspan.start()
                    && notClause.start() > firstSpans.start()
                    // the last notClause is in between max and
                    // firstspan.start()
                    && lastNotClause.getStart() < firstSpans.start()
                    && lastNotClause.getStart() >= firstSpans.start() - max) {

                maxPos = firstSpans.start() - lastNotClause.getStart() - 1;
                generateCandidates(min, maxPos, direction);
            }
        else {
            hasMoreSpans = firstSpans.next();
        }
    }

    /**
     * Expands the firstspans to the right.
     * 
     * @throws IOException
     */
    private void expandRight () throws IOException {
        int expansionEnd = firstSpans.end() + max;
        int maxPos = max;
        boolean isFound = false;

        CandidateSpan firstNotClause = null;
        // System.out.println("main start:"+firstSpans.start());
        while (hasMoreNotClause && notClause.start() < expansionEnd) {
            // between firstspan.end() and expansionEnd
            if (!isFound && notClause.start() >= firstSpans.end()) {
                maxPos = notClause.start() - firstSpans.end() - 1;
                firstNotClause = new CandidateSpan(notClause);
                isFound = true;
            }
            if (!notClause.next()) {
                hasMoreNotClause = false;
            }
        }
        // if a notClause is between firstSpan.end and max
        // then maxPos = the first notClause pos -1
        generateCandidates(min, maxPos, direction);

        if (firstNotClause != null) {
            while ((hasMoreSpans = firstSpans.next())
                    // in between
                    && firstNotClause.getStart() < firstSpans.end() + max
                    && firstNotClause.getStart() >= firstSpans.end()) {
                // System.out.println("first
                // start:"+firstNotClause.getStart()+", main
                // start:"+firstSpans.start());
                maxPos = firstNotClause.getStart() - firstSpans.end() - 1;
                generateCandidates(min, maxPos, direction);
            }
        }
        else {
            hasMoreSpans = firstSpans.next();
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
                start = Math.max(0, firstSpans.start() - counter);
                if (start > -1) {
                    end = firstSpans.end();
                    // System.out.println(start+","+end);
                    cs = new CandidateSpan(start, end, firstSpans.doc(),
                            firstSpans.cost(),
                            createPayloads(start, firstSpans.start()));
                    candidateSpans.add(cs);
                }
                counter--;
            }
        }
        else { // right
            counter = minPos;
            while (counter <= maxPos) {
                start = firstSpans.start();
                end = firstSpans.end() + counter;
                // System.out.println(start+","+end);

                cs = new CandidateSpan(start, end, firstSpans.doc(),
                        firstSpans.cost(),
                        createPayloads(firstSpans.end(), end));
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
        return matchCost;
    }
}
