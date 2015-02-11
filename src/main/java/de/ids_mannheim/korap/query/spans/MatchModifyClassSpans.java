package de.ids_mannheim.korap.query.spans;

import static de.ids_mannheim.korap.util.KorapByte.*;

import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.Bits;

import java.io.IOException;

import java.util.Map;
import java.util.ArrayList;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Spans, that can focus on the span boundaries of classed subqueries.
 * The boundaries of the classed subquery may exceed the boundaries of the
 * nested query.
 *
 * In case multiple classes are found with the very same number, the span
 * is maximized to start on the first occurrence from the left and end on
 * the last occurrence on the right.
 *
 * In case the class to focus on is not found in the payloads,
 * the match is ignored.
 *
 * <strong>Warning</strong>: Payloads other than class payloads won't
 * bubble up currently. That behaviour may change in the future
 *
 * @author diewald
 */

public class MatchModifyClassSpans extends Spans {
    private List<byte[]> wrappedPayload;
    private Collection<byte[]> payload;
    private final Spans spans;
    private byte number;

    private SpanQuery wrapQuery;
    private final Logger log = LoggerFactory.getLogger(MatchModifyClassSpans.class);

    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;

    private int start = -1,
                end;
    private int tempStart = 0,
                tempEnd = 0;

    /**
     * Construct a MatchModifyClassSpan for the given {@link SpanQuery}.
     * 
     * @param wrapQuery A {@link SpanQuery}.
     * @param context The {@link AtomicReaderContext}.
     * @param acceptDocs Bit vector representing the documents
     *        to be searched in.
     * @param termContexts A map managing {@link TermState TermStates}.
     * @param number The class number to focus on.
     * @throws IOException
     */
    public MatchModifyClassSpans (SpanQuery wrapQuery,
                                  AtomicReaderContext context,
                                  Bits acceptDocs,
                                  Map<Term,TermContext> termContexts,
                                  byte number) throws IOException {
        this.spans     = wrapQuery.getSpans(context, acceptDocs, termContexts);
        this.number    = number;
        this.wrapQuery = wrapQuery;
        this.wrappedPayload = new ArrayList<byte[]>(6);
    };


    @Override
    public Collection<byte[]> getPayload() throws IOException {
        return wrappedPayload;
    };


    @Override
    public boolean isPayloadAvailable () {
        return wrappedPayload.isEmpty() == false;
    };


    @Override
    public int doc () {
        return spans.doc();
    };


    @Override
    public int start () {
        return start;
    };


    @Override
    public int end () {
        return end;
    };


    @Override
    public boolean next() throws IOException {
        if (DEBUG) log.trace("Forward next match in {}",
                             this.doc());

        // Next span
        while (spans.next()) {
            if (DEBUG) log.trace("Forward next inner span");

            // No classes stored
            wrappedPayload.clear();

            start = -1;
            if (spans.isPayloadAvailable()) {
                end = 0;

                // Iterate over all payloads and find the maximum span per class
                for (byte[] payload : spans.getPayload()) {

                    // No class payload - ignore
                    // this may be problematic for other calculated payloads!
                    if (payload.length != 9) {
                        if (DEBUG) log.trace("Ignore old payload {}", payload);
                        continue;
                    };

                    // Found class payload of structure <i>start<i>end<b>class
                    // and classes are matches!
                    if (payload[8] == this.number) {
                        tempStart = byte2int(payload, 0);
                        tempEnd   = byte2int(payload, 4);

                        if (DEBUG) {
                            log.trace(
                                "Found matching class {}-{}",
                                tempStart,
                                tempEnd
                            );
                        };

                        // Set start position 
                        if (start == -1 || tempStart < start)
                            start = tempStart;
			
                        // Set end position
                        if (tempEnd > end)
                            end = tempEnd;
                    };

                    // Definately keep class information
                    // Even if it is already used for shrinking
                    wrappedPayload.add(payload);
                };
            };

            // Class not found
            if (start == -1)
                continue;

            if (DEBUG) {
                log.trace(
                    "Start to focus on class {} from {} to {}",
                    number,
                    start,
                    end
                );
            };
            return true;
        };

        // No more spans
        this.wrappedPayload.clear();
        return false;
    };


    // Todo: Check for this on document boundaries!
    @Override
    public boolean skipTo (int target) throws IOException {
        if (DEBUG) log.trace("Skip MatchSpans {} -> {}",
                             this.doc(), target);

        if (this.doc() < target && spans.skipTo(target)) {
            
        };
        return false;
    };


    @Override
    public String toString () {
        return getClass().getName() + "(" + this.wrapQuery.toString() + ")@" +
            (doc() + ":" + start() + "-" + end());
    };

    @Override
    public long cost () {
        return spans.cost();
    };
};
