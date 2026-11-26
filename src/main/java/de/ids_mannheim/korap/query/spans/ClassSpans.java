package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermState;
import org.apache.lucene.queries.spans.SpanQuery;
import org.apache.lucene.queries.spans.Spans;
import org.apache.lucene.util.Bits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Add a payload to the span with an identification number (a class)
 * And the start and end position of the span, so this information
 * can bubble up for later processing (similar to captures in regular
 * expression).
 * 
 * @author diewald
 */

public class ClassSpans extends SimpleSpans {
    protected List<byte[]> classedPayload;
    protected KrillSpans spans;
    protected byte number;
    protected SpanQuery operand;
    protected Boolean hasmorespans = false;

    private static final Logger log = LoggerFactory.getLogger(ClassSpans.class);

    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;


    /**
     * Construct a new ClassSpans object.
     * 
     * @param operand
     *            An arbitrary nested {@link SpanQuery}.
     * @param context
     *            The {@link LeafReaderContext}.
     * @param acceptDocs
     *            Bit vector representing the documents
     *            to be searched in.
     * @param weights
     *            Creates the spans of the operands.
     * @param number
     *            The identifying class number.
     */
    public ClassSpans (SpanQuery operand, LeafReaderContext context,
                       Bits acceptDocs, SpanWeights weights,
                       byte number)
            throws IOException {
        spans = weights.getSpans(operand, context, acceptDocs);

        // The number of the class
        this.number = number;

        // The current operand
        this.operand = operand;

        // The highlighted payload
        this.classedPayload = new ArrayList<byte[]>(3);

    };


    @Override
    public Collection<byte[]> getPayload () throws IOException {
        return classedPayload;
    };


    @Override
    public boolean isPayloadAvailable () {
        // We set payloads here - so it's always true
        return true;
    };


    public byte getNumber () {
        return number;
    }


    public void setNumber (byte number) {
        this.number = number;
    }






    @Override
    protected KrillSpans driver () {
        return spans;
    };


    @Override
    protected boolean nextMatchInDocument () throws IOException {
        if (DEBUG)
            log.trace("Forward next");

        if (!hasMoreSpans) {
            hasmorespans = false;
            return false;
        };

        this.matchDocNumber = spans.docID();
        this.matchStartPosition = spans.startPosition();
        this.matchEndPosition = spans.endPosition();
        this.addClassPayload();
        hasMoreSpans = nextInDocument(spans);
        return true;
    };


    protected boolean addClassPayload () throws IOException {
        hasmorespans = true;

        classedPayload.clear();

        // Subquery has payloads
        if (spans.isPayloadAvailable()) {
            classedPayload.addAll(spans.getPayload());
            if (DEBUG)
                log.trace("Found payload in nested SpanQuery");
        };

        if (DEBUG) {
            log.trace("Wrap class {} around span {} - {}", number,
                    spans.startPosition(), spans.endPosition());
        };

        // Todo: Better allocate using a Factory!
        ByteBuffer bb = ByteBuffer.allocate(10);
        bb.put((byte) 0);
        bb.putInt(spans.startPosition());
        bb.putInt(spans.endPosition());
        bb.put(number);
        /*
        System.err.println(
                           "####################### " + 
                           spans.startPosition() +
                           "|" + 
                           spans.endPosition() +
                           ":" +
                           number
                           );
        */

        // Add highlight information as byte array
        classedPayload.add(bb.array());

        if (spans instanceof SimpleSpans) {
            SimpleSpans ss = (SimpleSpans) spans;
            this.hasSpanId = ss.hasSpanId;
            this.spanId = ss.spanId;
        }
        return true;
    };



    @Override
    public String toString () {
        return getClass().getName() + "(" + this.operand.toString() + ")@"
                + (docID() + ":" + startPosition() + "-" + endPosition());
    };


    @Override
    public long cost () {
        return spans.cost();
    };
};
