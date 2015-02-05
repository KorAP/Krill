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
 * Enumeration of special spans which length is stored in their payload,
 * representing elements such as phrases, sentences and paragraphs.
 * 
 * @author margaretha
 * @author diewald
 */
public class ElementSpans extends SpansWithId {
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
        termSpans = (TermSpans) this.firstSpans;
        hasMoreSpans = true;
    };


    @Override
    public boolean next() throws IOException {
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

        // No need to check if there is a pl - there has to be a payload!
        this.matchPayload = termSpans.getPayload();

        List<byte[]> payload = (List<byte[]>) this.matchPayload;

        if (!payload.isEmpty()) {

            // Get payload one by one
            int length = payload.get(0).length;
            ByteBuffer bb = ByteBuffer.allocate(length);
            bb.put(payload.get(0));

            // set element end position from payload
            this.matchEndPosition = bb.getInt(8);

            // Copy element id
            this.setSpanId(this.hasSpanId ? bb.getShort(12) : (short) -1);

            // Copy the start and end character offsets
            byte[] b = new byte[8];
            b = Arrays.copyOfRange(bb.array(), 0, 8);
            this.matchPayload = Collections.singletonList(b);
        }

        // The span is extremely short ... well ...
        else {
            this.matchEndPosition = this.matchStartPosition;
            this.setSpanId((short) -1);
            this.matchPayload = null;
        };
        return true;
    };


    @Override
    public boolean skipTo(int target) throws IOException {
        if (hasMoreSpans &&
            firstSpans.doc() < target &&
            firstSpans.skipTo(target)) {
            return this.setToCurrent();
        };

        hasMoreSpans = false;
        this.matchPayload = null;
        return false;
    };


    @Override
    public long cost() {
        return termSpans.cost();
    };
};
