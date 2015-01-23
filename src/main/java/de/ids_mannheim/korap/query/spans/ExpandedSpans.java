package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.SpanExpansionQuery;

/**
 * Enumeration of spans expanded with minimum <code>m</code> and maximum
 * <code>n</code> token positions to either left or right direction from the
 * original spans. See examples in {@link SpanExpansionQuery}.
 * 
 * The expansion offsets, namely the start and end position of an expansion
 * part, can be stored in payloads. A class number is assigned to the offsets
 * grouping them altogether.
 * 
 * @author margaretha
 * */
public class ExpandedSpans extends SimpleSpans {

    private int min, max;
    private byte classNumber;
    private int direction;
    private List<CandidateSpan> candidateSpans;
    private long matchCost;

    /**
     * Constructs ExpandedSpans from the given {@link SpanExpansionQuery}.
     * 
     * @param spanExpansionQuery a SpanExpansionQuery
     * @param context
     * @param acceptDocs
     * @param termContexts
     * @throws IOException
     */
    public ExpandedSpans(SpanExpansionQuery spanExpansionQuery,
            AtomicReaderContext context, Bits acceptDocs,
            Map<Term, TermContext> termContexts) throws IOException {
        super(spanExpansionQuery, context, acceptDocs, termContexts);
        this.min = spanExpansionQuery.getMin();
        this.max = spanExpansionQuery.getMax();
        this.direction = spanExpansionQuery.getDirection();
        this.classNumber = spanExpansionQuery.getClassNumber();

        candidateSpans = new ArrayList<CandidateSpan>();
        hasMoreSpans = true;
    }

    @Override
    public boolean next() throws IOException {
        matchPayload.clear();
        isStartEnumeration = false;
        if (candidateSpans.size() == 0 && hasMoreSpans)
            hasMoreSpans = firstSpans.next();
        return advance();
    }

    /**
     * Advances the ExpandedSpans to the next match by setting the first element
     * in the candidateList as the match. Set the candidateList, if it is empty
     * 
     * @return <code>true</code> if a match is found, <code>false</code>
     *         otherwise.
     * @throws IOException
     */
    private boolean advance() throws IOException {
        while (candidateSpans.size() > 0 || hasMoreSpans) {
            if (candidateSpans.size() > 0) {
                setMatch(candidateSpans.get(0));
                candidateSpans.remove(0);
                return true;
            } else {
                setCandidateList();
            }
        }
        return false;
    }

    /**
     * Sets the candidateList by adding new candidate match spans for all
     * possible expansion with respect to the expansion length (min,max)
     * variables.
     * 
     * @throws IOException
     */
    private void setCandidateList() throws IOException {
        CandidateSpan cs;
        int counter, start, end;

        if (direction < 0) {
            counter = max;
            while (counter >= min) {
                start = Math.max(0, firstSpans.start() - counter);
                cs = new CandidateSpan(start, firstSpans.end(),
                        firstSpans.doc(), firstSpans.cost(), createPayloads(
                                start, firstSpans.start()));

                candidateSpans.add(cs);
                counter--;
            }
        } else {
            counter = min;
            while (counter <= max) {
                // TODO: How do I know if the end is already too far (over the end of the doc)? 
                end = firstSpans.end() + counter;
                cs = new CandidateSpan(firstSpans.start(), end,
                        firstSpans.doc(), firstSpans.cost(), createPayloads(
                                firstSpans.end(), end));
                candidateSpans.add(cs);
                counter++;
            }
        }
    }

    /**
     * Prepares the payloads for a candidate match (ExpandedSpans). If the class
     * number is set, the extension offsets with the given start and end
     * positions are to be stored in the payloads.
     * 
     * @param start
     * @param end
     * @return the payloads for a candidaete match
     * @throws IOException
     */
    private ArrayList<byte[]> createPayloads(int start, int end)
            throws IOException {

        ArrayList<byte[]> payload = new ArrayList<byte[]>();
        if (firstSpans.isPayloadAvailable()) {
            payload.addAll(firstSpans.getPayload());
        }
        if (classNumber > 0) {
            //System.out.println("Extension offsets "+start+","+end);
            payload.add(createExtensionPayloads(start, end));
        }
        return payload;
    }

    /**
     * Prepares a byte array of extension offsets with the given start and end
     * positions and the class number, to be stored in payloads.
     * 
     * @param start
     * @param end
     * @return a byte array of extension offsets and the class number
     */
    private byte[] createExtensionPayloads(int start, int end) {
        ByteBuffer buffer = ByteBuffer.allocate(9);
        buffer.putInt(start);
        buffer.putInt(end);
        buffer.put(classNumber);
        return buffer.array();
    }

    /**
     * Sets the properties of the given candidate match span as the current
     * match (state of ExpandedSpans).
     * 
     * @param candidateSpan
     */
    private void setMatch(CandidateSpan candidateSpan) {
        matchDocNumber = candidateSpan.getDoc();
        matchStartPosition = candidateSpan.getStart();
        matchEndPosition = candidateSpan.getEnd();
        matchPayload = candidateSpan.getPayloads();
        matchCost = candidateSpan.getCost();
    }

    @Override
    public boolean skipTo(int target) throws IOException {
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
    public long cost() {
        return matchCost;
    }

}
