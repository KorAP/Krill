package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.spans.SpanCollector;
import org.apache.lucene.util.BytesRef;

/**
 * Collects the payloads of the current position of spans, as
 * <tt>Spans.getPayload()</tt> returned them up to Lucene 5.2: the
 * payloads of all terms the position consists of, as copies.
 * 
 * Krill's span classes keep their payloads themselves and hand them
 * over with {@link #collectPayloads(Collection)}, so Lucene's
 * composite spans (e.g. of a <tt>SpanOrQuery</tt>) deliver the
 * payloads of Krill's spans they consist of.
 */
public class PayloadCollector implements SpanCollector {

    private final List<byte[]> payloads = new ArrayList<>();


    @Override
    public void collectLeaf (PostingsEnum postings, int position, Term term)
            throws IOException {
        BytesRef payload = postings.getPayload();
        if (payload != null && payload.length > 0)
            this.payloads.add(Arrays.copyOfRange(payload.bytes, payload.offset,
                    payload.offset + payload.length));
    };


    /**
     * Collect the payloads of Krill's spans.
     */
    public void collectPayloads (Collection<byte[]> payloads) {
        if (payloads != null)
            this.payloads.addAll(payloads);
    };


    @Override
    public void reset () {
        this.payloads.clear();
    };


    /**
     * The collected payloads.
     */
    public List<byte[]> getPayloads () {
        return this.payloads;
    };
};
