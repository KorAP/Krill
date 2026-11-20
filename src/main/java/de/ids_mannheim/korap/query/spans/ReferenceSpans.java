package de.ids_mannheim.korap.query.spans;

import static de.ids_mannheim.korap.util.KrillByte.byte2int;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.SpanReferenceQuery;

/**
 * Resolves a reference to some class by searching for the class
 * payload
 * in the payloads of the subspans (firstspans).
 * 
 * @author margaretha
 *
 */
public class ReferenceSpans extends SimpleSpans {

    private byte classNum;


    public ReferenceSpans (SpanReferenceQuery query, LeafReaderContext context,
                           Bits acceptDocs, SpanWeights weights)
            throws IOException {
        super(query, context, acceptDocs, weights);
        this.classNum = query.getClassNum();
    }



    @Override
    protected boolean nextMatchInDocument () throws IOException {
        while (hasMoreSpans) {
            if (hasSameClassPosition()) {
                matchStartPosition = firstSpans.startPosition();
                matchEndPosition = firstSpans.endPosition();
                matchDocNumber = firstSpans.docID();
                hasMoreSpans = nextInDocument(firstSpans);
                return true;
            }
            hasMoreSpans = nextInDocument(firstSpans);
        }
        return false;
    }


    /**
     * Iterates over the payloads of the firstspans and looks for a
     * payload having the same class number as referenced spans. If
     * there are more than one payloads with the same class number,
     * but different start and end positions, the method will return
     * false.
     * 
     * @return true, if such a payload is found, false, otherwise.
     * @throws IOException
     */
    private boolean hasSameClassPosition () throws IOException {
        int start = 0, end = 0;
        boolean isFound = false;
        boolean match = false;

        matchPayload.clear();

        for (byte[] payload : firstSpans.getPayload()) {
            if (payload.length == 10 && payload[9] == classNum) {
                if (isFound) {
                    if (start == byte2int(payload, 1)
                            && end == byte2int(payload, 5)) {
                        match = true;
                        continue;
                    }
                    match = false;
                    break;
                }

                start = byte2int(payload, 1);
                end = byte2int(payload, 5);
                isFound = true;
                matchPayload.add(payload);
            }
            else {
                matchPayload.add(payload);
            }
        }
        return match;
    }



    @Override
    public long cost () {
        return firstSpans.cost();
    }

}
