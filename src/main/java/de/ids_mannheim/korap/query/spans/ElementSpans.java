package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.search.spans.TermSpans;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.SpanElementQuery;

/**
 * Enumeration of spans which are elements such as phrases, sentences and
 * paragraphs.
 * 
 * @author margaretha
 * @author diewald
 */
public class ElementSpans extends SpansWithId {

    private List<CandidateElementSpan> candidateList;
    private int currentDoc, currentPosition;
    private TermSpans termSpans;

    /**
     * Constructs ElementSpans for the given {@link SpanElementQuery}.
     * 
     * @param spanElementQuery a SpanElementQuery
     * @param context
     * @param acceptDocs
     * @param termContexts
     * @throws IOException
     */
    public ElementSpans(SpanElementQuery spanElementQuery,
            AtomicReaderContext context, Bits acceptDocs,
            Map<Term, TermContext> termContexts) throws IOException {
        super(spanElementQuery, context, acceptDocs, termContexts);
        candidateList = new ArrayList<>();
        termSpans = (TermSpans) firstSpans;
        hasMoreSpans = termSpans.next();
        if (hasMoreSpans) {
            currentDoc = termSpans.doc();
            currentPosition = termSpans.start();
        }
    }

    @Override
    public boolean next() throws IOException {
        isStartEnumeration = false;
        return advance();
    }

    /**
     * Advances the ElementSpans to the next match by first checking the
     * candidate match list. If the list is empty, it will be set/filled in
     * first. Tells if there is a next match or not.
     * 
     * @return <code>true</code> if a match is found, <code>false</code>
     *         otherwise.
     * @throws IOException
     */
    private boolean advance() throws IOException {
        while (hasMoreSpans || !candidateList.isEmpty()) {
            if (!candidateList.isEmpty()) {
                CandidateElementSpan cs = candidateList.get(0);
                this.matchDocNumber = cs.getDoc();
                this.matchStartPosition = cs.getStart();
                this.matchEndPosition = cs.getEnd();
                this.matchPayload = cs.getPayloads();
                // this.setElementRef(cs.getSpanId());
                this.setSpanId(cs.getSpanId());
                candidateList.remove(0);
                return true;
            } else {
                // logger.info("Setting candidate list");
                setCandidateList();
                currentDoc = termSpans.doc();
                currentPosition = termSpans.start();
            }
        }
        return false;
    }

    /**
     * Collects all the elements starting at the same position and sort them by
     * their end positions. The list starts with the element having the smallest
     * end position.
     * 
     * @throws IOException
     */
    private void setCandidateList() throws IOException {
        while (hasMoreSpans && termSpans.doc() == currentDoc
                && termSpans.start() == currentPosition) {
            CandidateElementSpan cs = new CandidateElementSpan(termSpans,
                    spanId);
            // elementRef);
            readPayload(cs);
            candidateList.add(cs);
            hasMoreSpans = termSpans.next();
        }
        Collections.sort(candidateList);
    }

    /**
     * Reads the payloads of the termSpan and sets the end position and element
     * id from the payloads for the candidate match. The payloads for
     * character-offsets are set as the candidate match payloads. <br/>
     * <br/>
     * <em>Note</em>: payloadbuffer should actually collects all other payload
     * beside end position and element id, but KorapIndex identify element's
     * payloads by its length (8), which represents the character offset
     * payloads. So these offsets are directly set as the candidate match
     * payload.
     * 
     * @param cs a candidate match
     * @throws IOException
     */
    private void readPayload(CandidateElementSpan cs) throws IOException {
        List<byte[]> payload = (List<byte[]>) termSpans.getPayload();
        int length = payload.get(0).length;
        ByteBuffer bb = ByteBuffer.allocate(length);
        bb.put(payload.get(0));

        if (!payload.isEmpty()) {
            // set element end position from payload
            cs.setEnd(bb.getInt(8));

            if (hasSpanId) { // copy element id
                cs.setSpanId(bb.getShort(12));
            } else { // set element id -1
                cs.setSpanId((short) -1);
            }
            // Copy the start and end character offsets
            byte[] b = new byte[8];
            b = Arrays.copyOfRange(bb.array(), 0, 8);
            cs.setPayloads(Collections.singletonList(b));
        } else {
            cs.setEnd(cs.getStart());
            cs.setSpanId((short) -1);
            cs.setPayloads(null);
        }
    }

    @Override
    public boolean skipTo(int target) throws IOException {
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
    public long cost() {
        return termSpans.cost();
    }

    /**
     * Match candidate for element spans.
     * 
     * @author margaretha
     * 
     */
    class CandidateElementSpan extends CandidateSpan {

        private short elementId;

        public CandidateElementSpan(Spans span, short elementId)
                throws IOException {
            super(span);
            setSpanId(elementId);
        }

        public void setSpanId(short elementId) {
            this.elementId = elementId;
        }

        public short getSpanId() {
            return elementId;
        }
    }
};
