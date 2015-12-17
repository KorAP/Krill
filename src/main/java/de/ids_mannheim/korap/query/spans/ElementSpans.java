package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.index.TermState;
import org.apache.lucene.search.spans.TermSpans;
import org.apache.lucene.util.Bits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ids_mannheim.korap.query.SpanElementQuery;

/**
 * Enumeration of spans representing elements such as phrases, sentences and
 * paragraphs. Span length is stored as a payload.
 * 
 * Depth and certainty value payloads have not been loaded and handled yet.
 * 
 * @author margaretha
 * @author diewald
 */
public final class ElementSpans extends SimpleSpans {
    private final TermSpans termSpans;
	private boolean isPayloadLoaded;

    private final Logger log = LoggerFactory.getLogger(ElementSpans.class);
    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;

    private byte[] b = new byte[10];
    
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
     * @param termContexts
     *            A map managing {@link TermState TermStates}.
     * @throws IOException
     */
    public ElementSpans (SpanElementQuery spanElementQuery,
                         LeafReaderContext context, Bits acceptDocs,
                         Map<Term, TermContext> termContexts)
            throws IOException {
        super(spanElementQuery, context, acceptDocs, termContexts);
        termSpans = (TermSpans) this.firstSpans;
		hasMoreSpans = true;
		// termSpans.next();
    };


    @Override
    public boolean next () throws IOException {
        if (!hasMoreSpans || !(hasMoreSpans = termSpans.next()))
            return false;
        
		isStartEnumeration = false;
		this.matchPayload = null;
		matchEndPosition = -1;
		return advance();
	};

	private boolean advance() throws IOException {
		

		this.matchStartPosition = termSpans.start();
		this.matchDocNumber = termSpans.doc();		
		isPayloadLoaded = false;
		return true;
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
		else{
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
            this.matchPayload = null;
            return;
		}

		if (!payload.isEmpty()) {
            // Get payload one by one
            final int length = payload.get(0).length;
            final ByteBuffer bb = ByteBuffer.allocate(length);
            bb.put(payload.get(0));
			
			this.payloadTypeIdentifier = bb.get(0);
			this.matchEndPosition = bb.getInt(9);

            if (payloadTypeIdentifier == PayloadTypeIdentifier.ELEMENT.value
                    && length > 15) {
				this.setSpanId(bb.getShort(14));
				this.hasSpanId = true;
			}
            else {
                this.setSpanId((short) -1);
            }

			// FIX ME
			// Copy the start and end character offsets
            b = Arrays.copyOfRange(bb.array(), 1, 9);
            this.matchPayload = Collections.singletonList(b);
            return;
        }

        this.matchEndPosition = this.matchStartPosition;
        this.setSpanId((short) -1);
        this.matchPayload = null;
    };


    @Override
    public int end () {
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
        return !this.matchPayload.isEmpty();
    };


    @Override
    public short getSpanId () {
        this.loadPayload();
        return spanId;
    };


    @Override
    public boolean skipTo (int target) throws IOException {

        if (DEBUG)
            log.trace("Skip ElementSpans {} -> {}", firstSpans.doc(), target);

        if (hasMoreSpans && firstSpans.doc() < target
                && firstSpans.skipTo(target)) {
			return this.advance();
        };

        hasMoreSpans = false;
        this.matchPayload = null;
        return false;
    };


    @Override
    public long cost () {
        return termSpans.cost();
    };
};
