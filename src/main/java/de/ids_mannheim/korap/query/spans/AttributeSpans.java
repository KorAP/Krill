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
 * UPDATE THIS!
 * Span enumeration of attributes which are term spans with special
 * payload
 * assignments referring to another span (e.g. element/relation span)
 * to which
 * an attribute span belongs. The class is basically a wrapper of
 * Lucene {@link TermSpans} with additional functionality regarding
 * element/relation
 * reference. Element/relation id is annotated ascendingly starting
 * from the
 * left side. <br/>
 * <br/>
 * The enumeration is ordered firstly by the start position of the
 * attribute and
 * secondly by the element/relation id descendingly. This order helps
 * to match
 * element and attributes faster.
 * 
 * AttributeSpans contain information about the elements they belongs
 * to, thus
 * querying them alone is sufficient to get
 * "any element having a specific attribute".
 * 
 * @author margaretha
 */
public class AttributeSpans extends SimpleSpans {

    private List<CandidateAttributeSpan> candidateList;
    private int currentDoc, currentPosition;
    private boolean isFinish;

    public static enum PayloadTypeIdentifier {
        TERM_ATTRIBUTE(16), ELEMENT_ATTRIBUTE(17), RELATION_ATTRIBUTE(18);

        private int value;


        private PayloadTypeIdentifier (int value) {
            this.value = value;
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
     * @param termContexts
     * @throws IOException
     */
    public AttributeSpans (SpanAttributeQuery spanAttributeQuery,
                           LeafReaderContext context, Bits acceptDocs,
                           Map<Term, TermContext> termContexts)
            throws IOException {
        super(spanAttributeQuery, context, acceptDocs, termContexts);
        this.hasSpanId = true;

        candidateList = new ArrayList<>();
        hasMoreSpans = firstSpans.next();
        if (hasMoreSpans) {
            currentDoc = firstSpans.doc();
            currentPosition = firstSpans.start();
        }
    }


    @Override
    public boolean next () throws IOException {
        isStartEnumeration = false;
        matchPayload.clear();
        return advance();
    }


    /**
     * Moves to the next match by checking the candidate match list or
     * setting
     * the list first when it is empty.
     * 
     * @return true if a match is found
     * @throws IOException
     */
    private boolean advance () throws IOException {
        while (hasMoreSpans || !candidateList.isEmpty()) {
            if (!candidateList.isEmpty()) {
                // set the current match from the first CandidateAttributeSpan
                // in the candidate list
                CandidateAttributeSpan cs = candidateList.get(0);
                this.matchDocNumber = cs.getDoc();
                this.matchStartPosition = cs.getStart();
                this.matchEndPosition = cs.getEnd();
                this.setSpanId(cs.getSpanId()); // referentId
                candidateList.remove(0);
                return true;
            }
            else {
                setCandidateList();
                currentDoc = firstSpans.doc();
                currentPosition = firstSpans.start();
            }
        }
        return false;
    }


    /**
     * Collects all the attributes in the same start position and sort
     * them by
     * element/relation Id in a reverse order (the ones with the
     * bigger
     * element/relation Id first).
     * 
     * @throws IOException
     */
    private void setCandidateList () throws IOException {

        while (hasMoreSpans && firstSpans.doc() == currentDoc
                && firstSpans.start() == currentPosition) {

            candidateList.add(createCandidateSpan());
            hasMoreSpans = firstSpans.next();
        }

        Collections.sort(candidateList);
        Collections.reverse(candidateList);
    }


    /**
     * Creates a CandidateAttributeSpan based on the child span and
     * set the
     * spanId and elementEnd from its payloads.
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
        short spanId = payloadBuffer.getShort(5);
        // if (payload.get(0).length == 6) {
        int end = payloadBuffer.getInt(1);

        return new CandidateAttributeSpan(firstSpans, payloadTypeIdentifier,
                spanId, end);

        // }
        // else if (payload.get(0).length == 10) {
        // start = wrapper.getInt(0);
        // end = wrapper.getInt(4);
        // spanId = wrapper.getShort(8);
        // return new CandidateAttributeSpan(firstSpans, spanId, start, end);
        // }

        // throw new NullPointerException("Missing element end in payloads.");
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
    public boolean skipTo (int target) throws IOException {
        if (hasMoreSpans && (firstSpans.doc() < target)) {
            if (!firstSpans.skipTo(target)) {
                candidateList.clear();
                return false;
            }
        }
        setCandidateList();
        matchPayload.clear();
        isStartEnumeration = false;
        return advance();
    }


    @Override
    public long cost () {
        return firstSpans.cost();
    }

    /**
     * CandidateAttributeSpan contains information about an Attribute
     * span. All
     * attribute spans occurring in an identical position are
     * collected as
     * CandidateAttributeSpans. The list of these
     * CandidateAttributeSpans are
     * sorted based on the span ids to which the attributes belong to.
     * The
     * attributes with smaller spanIds come first on the list.
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
        public CandidateAttributeSpan (Spans span, byte payloadTypeIdenfitier,
                                       short spanId, int elementEnd)
                throws IOException {
            super(span);
            this.spanId = spanId;
            this.end = elementEnd;
            this.payloadTypeIdentifier = payloadTypeIdenfitier;
        }


        // public CandidateAttributeSpan (Spans span, short spanId, int start,
        // int end) throws IOException {
        // super(span);
        // setSpanId(spanId);
        // this.start = start;
        // this.end = end;
        // }

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
