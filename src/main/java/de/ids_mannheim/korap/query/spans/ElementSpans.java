package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.index.TermState;
import org.apache.lucene.search.spans.TermSpans;
import org.apache.lucene.util.Bits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ids_mannheim.korap.query.SpanElementQuery;

/**
 * Enumeration of special spans which length is stored in their
 * payload,
 * representing elements such as phrases, sentences and paragraphs.
 * 
 * @author margaretha
 * @author diewald
 */
public class ElementSpans extends SimpleSpans {
    private TermSpans termSpans;
    private boolean lazyLoaded = false;

    private final Logger log = LoggerFactory.getLogger(ElementSpans.class);
    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;


    /**
     * Constructs ElementSpans for the given {@link SpanElementQuery}.
     * 
     * @param spanElementQuery
     *            A {@link SpanElementQuery}.
     * @param context
     *            The {@link AtomicReaderContext}.
     * @param acceptDocs
     *            Bit vector representing the documents
     *            to be searched in.
     * @param termContexts
     *            A map managing {@link TermState TermStates}.
     * @throws IOException
     */
    public ElementSpans (SpanElementQuery spanElementQuery,
                         AtomicReaderContext context, Bits acceptDocs,
                         Map<Term, TermContext> termContexts)
            throws IOException {
        super(spanElementQuery, context, acceptDocs, termContexts);
        termSpans = (TermSpans) this.firstSpans;
        hasMoreSpans = true;
        // hasSpanId = true;
    };


    @Override
    public boolean next () throws IOException {
        isStartEnumeration = false;

        if (!hasMoreSpans || !(hasMoreSpans = termSpans.next()))
            return false;

        // Set current values
        return this.setToCurrent();
    };


    // Set term values to current
    private boolean setToCurrent () throws IOException {
        // Get payload
        this.matchStartPosition = termSpans.start();
        this.matchDocNumber = termSpans.doc();
        this.lazyLoaded = false;
        return true;
    };


    /*
     * Process payload lazily.
     * This may have a little impact on queries like
     * position queries, where spans can be rejected
     * solely based on their starting and doc position.
     */
    private void processPayload () {
        if (this.lazyLoaded)
            return;

        // This will prevent failures for IOExceptions
        this.lazyLoaded = true;

        // No need to check if there is a pl - there has to be a payload!
        try {
            this.matchPayload = termSpans.getPayload();
        }
        catch (IOException e) {
            this.matchEndPosition = this.matchStartPosition;
            this.setSpanId((short) -1);
            this.matchPayload = null;
            return;
        };

        List<byte[]> payload = (List<byte[]>) this.matchPayload;

        if (!payload.isEmpty()) {

            // Get payload one by one
            int length = payload.get(0).length;
            ByteBuffer bb = ByteBuffer.allocate(length);
            bb.put(payload.get(0));

            // set element end position from payload
            this.matchEndPosition = bb.getInt(8);

            // Copy element id
            if (length >= 14) {
                this.setSpanId(bb.getShort(12));
                this.hasSpanId = true;
            }
            else {
                this.setSpanId((short) -1);
            }

            // Copy the start and end character offsets
            byte[] b = new byte[8];
            b = Arrays.copyOfRange(bb.array(), 0, 8);
            this.matchPayload = Collections.singletonList(b);
            return;
        }

        this.matchEndPosition = this.matchStartPosition;
        this.setSpanId((short) -1);
        this.matchPayload = null;
    };


    @Override
    public int end () {
        this.processPayload();
        return this.matchEndPosition;
    };


    @Override
    public Collection<byte[]> getPayload () {
        this.processPayload();
        return this.matchPayload;
    };


    @Override
    public boolean isPayloadAvailable () {
        this.processPayload();
        return !this.matchPayload.isEmpty();
    };


    @Override
    public short getSpanId () {
        this.processPayload();
        return spanId;
    };


    @Override
    public boolean skipTo (int target) throws IOException {

        if (DEBUG)
            log.trace("Skip ElementSpans {} -> {}", firstSpans.doc(), target);

        if (hasMoreSpans && firstSpans.doc() < target
                && firstSpans.skipTo(target)) {
            return this.setToCurrent();
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
