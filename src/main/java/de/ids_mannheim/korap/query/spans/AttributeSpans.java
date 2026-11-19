package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.search.spans.TermSpans;
import org.apache.lucene.util.Bits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ids_mannheim.korap.query.SpanAttributeQuery;

/**
 * Span enumeration of attributes which are term spans with special
 * payload assignments referring to another span (e.g.
 * element/relation span) to which an attribute span belongs. The
 * class is basically a wrapper of Lucene {@link TermSpans} with
 * additional functionality regarding element/relation
 * reference. Element/relation id is annotated ascendingly starting
 * from the left side.
 * <br/><br/>
 * The enumeration is ordered firstly by the start position of the
 * attribute and secondly by the element/relation id descendingly.
 * This order helps to match element and attributes faster.
 * 
 * AttributeSpans have the same start and end positions of the
 * element/relations they belongs to, thus querying them alone
 * is sufficient to get "any element having a specific
 * attribute".
 * 
 * @author margaretha
 */
public class AttributeSpans extends SimpleSpans {

    private List<CandidateAttributeSpan> candidateList;
    private boolean isFinish;

    // Start position of the child that is not collected yet,
    // NO_MORE_POSITIONS when the child has no more positions
    // in the current document
    private int childStart = -1;

    public static enum PayloadTypeIdentifier {
        TERM_ATTRIBUTE(16), ELEMENT_ATTRIBUTE(17), RELATION_ATTRIBUTE(18);

        private int value;


        private PayloadTypeIdentifier (int value) {
            this.value = value;
        }


        public int getValue () {
            return value;
        }
    }

    protected Logger logger = LoggerFactory.getLogger(AttributeSpans.class);


    /**
     * Constructs Attributespans based on the specified
     * SpanAttributeQuery.
     * 
     * @param spanAttributeQuery
     *            a spanAttributeQuery
     * @param context
     * @param acceptDocs
     * @param weights
     * @throws IOException
     */
    public AttributeSpans (SpanAttributeQuery spanAttributeQuery,
                           LeafReaderContext context, Bits acceptDocs,
                           SpanWeights weights)
            throws IOException {
        super(spanAttributeQuery, context, acceptDocs, weights);
        this.hasSpanId = true;
        this.hasMoreSpans = true;
        candidateList = new ArrayList<>();
    }



    @Override
    public int docID () {
        return matchDocNumber;
    }


    @Override
    public int nextDoc () throws IOException {
        isStartEnumeration = false;
        return this.enterDoc(firstSpans.nextDoc());
    }


    @Override
    public int advance (int target) throws IOException {
        isStartEnumeration = false;
        return this.enterDoc(firstSpans.advance(target));
    }


    // Position the child on the first attribute of the new document
    private int enterDoc (int doc) throws IOException {
        candidateList.clear();
        matchPayload.clear();
        matchDocNumber = doc;
        matchStartPosition = -1;
        matchEndPosition = -1;
        if (doc == NO_MORE_DOCS) {
            hasMoreSpans = false;
            childStart = NO_MORE_POSITIONS;
            matchStartPosition = NO_MORE_POSITIONS;
            matchEndPosition = NO_MORE_POSITIONS;
        }
        else {
            childStart = firstSpans.nextStartPosition();
        };
        return doc;
    }


    /**
     * Moves to the next match by checking the candidate match list or
     * setting the list first when it is empty.
     */
    @Override
    public int nextStartPosition () throws IOException {
        matchPayload.clear();

        if (candidateList.isEmpty()) {
            if (childStart == NO_MORE_POSITIONS) {
                matchStartPosition = NO_MORE_POSITIONS;
                matchEndPosition = NO_MORE_POSITIONS;
                return NO_MORE_POSITIONS;
            };
            setCandidateList();
        };

        // set the current match from the first CandidateAttributeSpan
        // in the candidate list
        CandidateAttributeSpan cs = candidateList.get(0);
        this.matchStartPosition = cs.getStart();
        this.matchEndPosition = cs.getEnd();
        this.setSpanId(cs.getSpanId()); // referentId
        candidateList.remove(0);
        return this.matchStartPosition;
    }


    @Override
    public int startPosition () {
        return matchStartPosition;
    }


    @Override
    public int endPosition () {
        return matchEndPosition;
    }


    /**
     * Collects all the attributes in the same start position and sort
     * them by element/relation Id in a reverse order (the ones with
     * the
     * bigger element/relation Id first).
     * 
     * @throws IOException
     */
    private void setCandidateList () throws IOException {
        final int position = childStart;

        while (childStart == position) {
            candidateList.add(createCandidateSpan());
            childStart = firstSpans.nextStartPosition();
        }

        Collections.sort(candidateList);
        Collections.reverse(candidateList);
    }


    /**
     * Creates a CandidateAttributeSpan based on the child span and
     * set the spanId and elementEnd from its payloads.
     * 
     * @param firstSpans
     *            an AttributeSpans
     * @return a CandidateAttributeSpan
     * @throws IOException
     */
    private CandidateAttributeSpan createCandidateSpan () throws IOException {
        List<byte[]> payload = (List<byte[]>) firstSpans.getPayload();
        ByteBuffer payloadBuffer = ByteBuffer.wrap(payload.get(0));

        byte payloadTypeIdentifier = payloadBuffer.get(0);
        // short spanId = payloadBuffer.getShort(5);
        // int end = payloadBuffer.getInt(1);
        short spanId = payloadBuffer.getShort(1);

        int end;

        if (payload.get(0).length >= 7) {
            end = payloadBuffer.getInt(3);
        }

        // Associated with a milestone element
        else {
            end = firstSpans.startPosition();
        }

        return new CandidateAttributeSpan(firstSpans, payloadTypeIdentifier,
                spanId, end);
    }


    /**
     * Tells if the enumeration of the AttributeSpans has come to an
     * end.
     * 
     * @return true if the enumeration has finished.
     */
    public boolean isFinish () {
        return isFinish;
    }


    /**
     * Sets true if the enumeration of the AttributeSpans has come to
     * an end.
     * 
     * @param isFinish
     *            <code>true</code> if the enumeration of the
     *            AttributeSpans has come to an end,
     *            <code>false</code> otherwise.
     */
    public void setFinish (boolean isFinish) {
        this.isFinish = isFinish;
    }


    @Override
    public long cost () {
        return firstSpans.cost();
    }

    /**
     * CandidateAttributeSpan contains information about an Attribute
     * span. All attribute spans occurring in an identical position
     * are collected as CandidateAttributeSpans. The list of these
     * CandidateAttributeSpans are sorted based on the span ids to
     * which the attributes belong to. The attributes with smaller
     * spanIds come first on the list.
     * 
     */
    class CandidateAttributeSpan extends CandidateSpan
            implements Comparable<CandidateSpan> {

        /**
         * Construct a CandidateAttributeSpan based on the given span,
         * spanId,
         * and elementEnd.
         * 
         * @param span
         *            an AttributeSpans
         * @param spanId
         *            the element or relation span id to which the
         *            current
         *            state of the specified AttributeSpans belongs
         *            to.
         * @param elementEnd
         *            the end position of the element or relation span
         *            to
         *            which the current state of the specified
         *            AttributeSpans
         *            belongs to.
         * @throws IOException
         */
        public CandidateAttributeSpan (KrillSpans span, byte payloadTypeIdenfitier,
                                       short spanId, int elementEnd)
                throws IOException {
            super(span);
            this.spanId = spanId;
            this.end = elementEnd;
            this.payloadTypeIdentifier = payloadTypeIdenfitier;
        }


        @Override
        public int compareTo (CandidateSpan o) {
            CandidateAttributeSpan cs = (CandidateAttributeSpan) o;
            if (this.spanId == cs.spanId)
                return 0;
            else if (this.spanId > cs.spanId)
                return 1;
            return -1;
        }
    }
}
