package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.SpanTermWithIdQuery;

/**
 * Enumeration of termSpans having an id. This class just wraps the
 * usual Lucene
 * TermSpans, and adds spanid property. It reads the term-id from a
 * term span
 * payload. The term-id is encoded in a short, starting from (offset)
 * 0 in the
 * payload.
 * 
 * @author margaretha
 */
public class TermSpansWithId extends SimpleSpans {

    /**
     * Creates TermSpansWithId from the given spanTermWithIdQuery.
     * 
     * @param spanTermWithIdQuery
     *            a spanTermWithIdQuery
     * @param context
     * @param acceptDocs
     * @param weights
     * @throws IOException
     */
    public TermSpansWithId (SpanTermWithIdQuery spanTermWithIdQuery,
                            LeafReaderContext context, Bits acceptDocs,
                            SpanWeights weights)
            throws IOException {
        super(spanTermWithIdQuery, context, acceptDocs, weights);
        hasSpanId = true;
    }



    @Override
    public int docID () {
        return firstSpans.docID();
    }


    @Override
    public int nextDoc () throws IOException {
        isStartEnumeration = false;
        return firstSpans.nextDoc();
    }


    @Override
    public int advance (int target) throws IOException {
        isStartEnumeration = false;
        return firstSpans.advance(target);
    }


    @Override
    public int nextStartPosition () throws IOException {
        int start = firstSpans.nextStartPosition();
        if (start != NO_MORE_POSITIONS)
            readPayload();
        return start;
    }


    @Override
    public int startPosition () {
        return firstSpans.startPosition();
    }


    @Override
    public int endPosition () {
        return firstSpans.endPosition();
    }


    /**
     * Read the payloads of the current firstspan and set the term id
     * info from
     * the payloads.
     * 
     * @throws IOException
     */
    private void readPayload () throws IOException {
        List<byte[]> payload = (List<byte[]>) firstSpans.getPayload();
        ByteBuffer bb = ByteBuffer.allocate(payload.get(0).length);
        bb.put(payload.get(0));
        setSpanId(bb.getShort(0)); //term id
    }


    @Override
    public long cost () {
        return firstSpans.cost(); // plus cost from reading payload
    }

}
