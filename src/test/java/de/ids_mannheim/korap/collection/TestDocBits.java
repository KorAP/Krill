package de.ids_mannheim.korap.collection;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.util.BitDocIdSet;
import org.apache.lucene.util.FixedBitSet;
import org.junit.Test;

import de.ids_mannheim.korap.KrillCollection;
import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.util.QueryException;

public class TestDocBits {

    private KrillIndex ki;

    @Test
    public void testRecreatedDocBitsLength () throws IOException, QueryException {
        ki = new KrillIndex();
        ki.addDoc(TestKrillCollectionIndex.createDoc1());
        ki.addDoc(TestKrillCollectionIndex.createDoc2());
        ki.addDoc(TestKrillCollectionIndex.createDoc3());
        ki.commit();

        KrillCollection kc = new KrillCollection();
        List<LeafReaderContext> leaves = this.ki.reader().leaves();
        for (LeafReaderContext context : leaves) {
            FixedBitSet bitset = kc.bits(context);
            DocBits docBits = new DocBits(bitset.getBits(), bitset.length());
            BitDocIdSet bitDocIdSet = docBits.createBitDocIdSet();
            assertEquals(bitset.length(), bitDocIdSet.bits().length());
        }
    }
}
