package de.ids_mannheim.korap.collection;

import java.io.Serializable;

import org.apache.lucene.util.BitDocIdSet;
import org.apache.lucene.util.FixedBitSet;

/** Serializable object for caching Lucene doc bit vector.
 * 
 * @author margaretha
 *
 */
public class DocBits implements Serializable {

    /**
     * Auto generated
     */
    private static final long serialVersionUID = -3505650918983180852L;
    final long[] bits;

    public DocBits (long[] bits) {
        this.bits = bits;
    }

    public BitDocIdSet createBitDocIdSet () {
        FixedBitSet bitset = new FixedBitSet(bits, bits.length);
        BitDocIdSet docIdSet = new BitDocIdSet(bitset);
        return docIdSet;
    }

    @Override
    public String toString () {
        StringBuilder sb = new StringBuilder("[");
        int i = 1;
        for (long b : bits) {
            sb.append(b);
            if (i < bits.length) {
                sb.append(",");
            }
            i++;
        }
        sb.append("]");
        return sb.toString();
    }
}
