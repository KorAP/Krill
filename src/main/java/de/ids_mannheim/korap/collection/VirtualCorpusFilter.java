package de.ids_mannheim.korap.collection;

import java.io.IOException;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Filter;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.FixedBitSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ids_mannheim.korap.KrillCollection;
import de.ids_mannheim.korap.cache.VirtualCorpusCache;
import de.ids_mannheim.korap.util.Fingerprinter;
import de.ids_mannheim.korap.util.QueryException;

public class VirtualCorpusFilter extends Filter {

    public final static Logger log = LoggerFactory
            .getLogger(VirtualCorpusFilter.class);
    public static boolean DEBUG = false;

    private String vcId;
    private DocBitsSupplier docBitsSupplier;

    public VirtualCorpusFilter (String vcId) {
        this.vcId = vcId;
        docBitsSupplier = new DocBitsSupplier();
    }


    @Override
    public DocIdSet getDocIdSet (LeafReaderContext context, Bits acceptDocs)
            throws IOException {
        String leafFingerprint = Fingerprinter.create(
                context.reader().getCombinedCoreAndDeletesKey().toString());
        
        DocBits docBits = VirtualCorpusCache.getDocBits(vcId, leafFingerprint,
                () -> {
                    try {
                        return docBitsSupplier.supplyDocBits(context, acceptDocs);
                    }
                    catch (IOException | QueryException e) {
                        throw new RuntimeException(e);
                    }
                });
        return docBits.createBitDocIdSet();
    }

    @Override
    public String toString () {
        return "vcFilter("+vcId+")";
    }
    
    public DocBitsSupplier getDocBitsSupplier () {
        return docBitsSupplier;
    }
    
    public class DocBitsSupplier {

        private Filter filter;
        private CollectionBuilder.Interface cbi;
        
        public DocBitsSupplier () {}
        
        public DocBits supplyDocBits (LeafReaderContext context,
                Bits acceptDocs) throws IOException, QueryException {
            if (cbi == null || filter == null) {
                KrillCollection kc = new KrillCollection();
                // load from file
                kc.fromStore(vcId);
                if (kc.hasErrors()) {
                    throw new QueryException(kc.getError(0).getCode(),
                            kc.getError(0).getMessage());
                }

                this.cbi = kc.getBuilder();
                this.filter = kc.toFilter();
            }
                
            DocIdSet docIdSet = filter.getDocIdSet(context, acceptDocs);
            return calculateDocBits(docIdSet, context.reader().maxDoc());
        }


        private DocBits calculateDocBits (DocIdSet docIdSet, int maxDoc)
                throws IOException {
            FixedBitSet bitset = new FixedBitSet(maxDoc);
            if (docIdSet == null) {
                if (cbi.isNegative()) {
                    bitset.set(0, maxDoc);
                }
                else {
                    bitset.clear(0, maxDoc);
                }
            }
            else {
                DocIdSetIterator docIdSetIterator = docIdSet.iterator();
                if (docIdSetIterator != null) {
                    bitset.or(docIdSetIterator);
                }
                if (cbi.isNegative()) {
                    bitset.flip(0, maxDoc);
                }
            }

            return new DocBits(bitset.getBits(), bitset.length());
        }
    }
    
}
