package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermState;
import org.apache.lucene.util.Bits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ids_mannheim.korap.query.SpanElementQuery;

/**
 * Enumeration of spans representing elements such as phrases,
 * sentences and paragraphs. Span length is stored as a payload.
 * 
 * Depth and certainty value payloads have not been loaded and handled
 * yet.
 * 
 * @author margaretha
 * @author diewald
 */
public final class ElementSpans extends SimpleSpans {
    private final KrillSpans termSpans;
    private boolean isPayloadLoaded;

    private final Logger log = LoggerFactory.getLogger(ElementSpans.class);
    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;

    public static enum PayloadTypeIdentifier {
        ELEMENT(64),
        // ELEMENT_WITH_TUI(65),
        // ELEMENT_WITH_CERTAINTY_VALUE (66),
        // ELEMENT_WITH_TUI_AND_CERTAINTY_VALUE (67),
        MILESTONE(65);

        private byte value;

        private PayloadTypeIdentifier (int value) {
            this.value = (byte) value;
        }
    }


    /**
     * Constructs ElementSpans for the given {@link SpanElementQuery}.
     * 
     * @param spanElementQuery
     *            A {@link SpanElementQuery}.
     * @param context
     *            The {@link LeafReaderContext}.
     * @param acceptDocs
     *            Bit vector representing the documents
     *            to be searched in.
     * @param weights
     *            Creates the spans of the operands.
     * @throws IOException
     */
    public ElementSpans (SpanElementQuery spanElementQuery,
                         LeafReaderContext context, Bits acceptDocs,
                         SpanWeights weights)
            throws IOException {
        super(spanElementQuery, context, acceptDocs, weights);
        termSpans = this.firstSpans;
        hasMoreSpans = true;
        isPayloadLoaded = true;
        if (DEBUG)
            log.trace("Create ElementSpan");
    };



    @Override
    public int docID () {
        return termSpans.docID();
    };


    @Override
    public int nextDoc () throws IOException {
        if (DEBUG)
            log.trace("Next document for element");
        isStartEnumeration = false;
        return this.enterDoc(termSpans.nextDoc());
    };


    @Override
    public int advance (int target) throws IOException {
        if (DEBUG)
            log.trace("Advance ElementSpans {} -> {}", termSpans.docID(),
                    target);
        isStartEnumeration = false;
        return this.enterDoc(termSpans.advance(target));
    };


    // No position of the new document is read yet
    private int enterDoc (int doc) {
        this.matchDocNumber = doc;
        this.matchStartPosition = -1;
        this.matchEndPosition = -1;
        this.matchPayload = null;
        this.isPayloadLoaded = true;
        if (doc == NO_MORE_DOCS) {
            hasMoreSpans = false;
            this.matchStartPosition = NO_MORE_POSITIONS;
            this.matchEndPosition = NO_MORE_POSITIONS;
        };
        return doc;
    };


    @Override
    public int nextStartPosition () throws IOException {
        if (DEBUG)
            log.trace("Next element span for element");

        this.matchPayload = null;
        this.matchStartPosition = termSpans.nextStartPosition();

        // No more elements in this document
        if (this.matchStartPosition == NO_MORE_POSITIONS) {
            this.matchEndPosition = NO_MORE_POSITIONS;
            this.isPayloadLoaded = true;
            return NO_MORE_POSITIONS;
        };

        // The end position is read lazily from the payload
        this.matchEndPosition = -1;
        this.isPayloadLoaded = false;
        return this.matchStartPosition;
    };


    @Override
    public int startPosition () {
        return this.matchStartPosition;
    };


    /*
     * Process payload lazily.
     * This may have a little impact on queries like
     * position queries, where spans can be rejected
     * solely based on their starting and doc position.
     */
    private void loadPayload () {
        if (this.isPayloadLoaded) {
            return;
        }
        else {
            this.isPayloadLoaded = true;
        }

        List<byte[]> payload;

        try {
            payload = (List<byte[]>) termSpans.getPayload();
        }
        catch (IOException e) {
            // silently setting empty element and payload
            this.matchEndPosition = this.matchStartPosition;
            this.setSpanId((short) -1);
            this.hasSpanId = false;
            this.matchPayload = null;
            return;
        }

        if (!payload.isEmpty() && payload.get(0) != null){
                
            // Get payload one by one
            final int length = payload.get(0).length;
            final ByteBuffer bb = ByteBuffer.allocate(length);
            bb.put(payload.get(0));

            this.payloadTypeIdentifier = bb.get(0);

            if (payloadTypeIdentifier != PayloadTypeIdentifier.MILESTONE.value) {
                this.matchEndPosition = bb.getInt(9);
            };

            if (payloadTypeIdentifier == PayloadTypeIdentifier.ELEMENT.value
                    && length > 15) {
                this.setSpanId(bb.getShort(14));
                this.hasSpanId = true;
            }
            else {
                this.setSpanId((short) -1);
                this.hasSpanId = false;
            }

            // FIX ME
            // Copy the start and end character offsets
            // b = Arrays.copyOfRange(bb.array(), 1, 9);
            // this.matchPayload = Collections.singletonList(b);
            this.matchPayload = Collections.singletonList(bb.array());
            return;
        }

        this.matchEndPosition = this.matchStartPosition;
        this.setSpanId((short) -1);
        this.hasSpanId = false;
        this.matchPayload = null;
    };


    @Override
    public int endPosition () {
        this.loadPayload();
        return this.matchEndPosition;
    };


    @Override
    public Collection<byte[]> getPayload () {
        this.loadPayload();
        return this.matchPayload;
    };


    @Override
    public boolean isPayloadAvailable () {
        this.loadPayload();
        return this.matchPayload != null && !this.matchPayload.isEmpty();
    };


    @Override
    public short getSpanId () {
        this.loadPayload();
        return spanId;
    };


    @Override
    public long cost () {
        return termSpans.cost();
    };
};
