package de.ids_mannheim.korap.collection;

import java.io.IOException;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.ConstantScoreScorer;
import org.apache.lucene.search.ConstantScoreWeight;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryVisitor;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.ScorerSupplier;
import org.apache.lucene.search.Weight;
import org.apache.lucene.util.BitSetIterator;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.FixedBitSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ids_mannheim.korap.KrillCollection;
import de.ids_mannheim.korap.cache.VirtualCorpusCache;
import de.ids_mannheim.korap.util.Fingerprinter;
import de.ids_mannheim.korap.util.QueryException;
import de.ids_mannheim.korap.util.ReaderKeys;

/**
 * Matches the documents of a stored virtual corpus. The documents
 * are kept per leaf in the {@link VirtualCorpusCache}.
 */
public class VirtualCorpusQuery extends Query {

    public final static Logger log = LoggerFactory
            .getLogger(VirtualCorpusQuery.class);
    public static boolean DEBUG = false;

    private String vcId;
    private DocBitsSupplier docBitsSupplier;

    public VirtualCorpusQuery (String vcId) {
        this.vcId = vcId;
        docBitsSupplier = new DocBitsSupplier();
    }


    @Override
    public Weight createWeight (IndexSearcher searcher,
            ScoreMode scoreMode, float boost) throws IOException {
        return new ConstantScoreWeight(this, boost) {
            // Not cached by Lucene's query cache
            @Override
            public boolean isCacheable (LeafReaderContext context) {
                return false;
            };


            @Override
            public ScorerSupplier scorerSupplier (LeafReaderContext context)
                    throws IOException {
                DocBits docBits = VirtualCorpusQuery.this.docBits(context);
                FixedBitSet bitset = new FixedBitSet(docBits.bits,
                        docBits.numbits);
                return new DefaultScorerSupplier(new ConstantScoreScorer(
                        score(), scoreMode,
                        new BitSetIterator(bitset, bitset.cardinality())));
            };
        };
    }


    // The documents of the virtual corpus in a leaf
    private DocBits docBits (LeafReaderContext context) throws IOException {
        String leafFingerprint = Fingerprinter.create(
                ReaderKeys.of(context.reader()));

        return VirtualCorpusCache.getDocBits(vcId, leafFingerprint,
                () -> {
                    try {
                        return docBitsSupplier.supplyDocBits(context, null);
                    }
                    catch (IOException | QueryException e) {
                        throw new RuntimeException(e);
                    }
                });
    }


    // The query of the stored virtual corpus is only loaded when
    // searching, so it is a leaf
    @Override
    public void visit (QueryVisitor visitor) {
        visitor.visitLeaf(this);
    }


    @Override
    public String toString (String field) {
        return "vcFilter("+vcId+")";
    }


    @Override
    public boolean equals (Object obj) {
        return sameClassAs(obj)
                && this.vcId.equals(((VirtualCorpusQuery) obj).vcId);
    }


    @Override
    public int hashCode () {
        return 31 * classHash() + this.vcId.hashCode();
    }


    public DocBitsSupplier getDocBitsSupplier () {
        return docBitsSupplier;
    }

    public class DocBitsSupplier {

        private Query query;
        private CollectionBuilder.Interface cbi;

        public DocBitsSupplier () {}

        /**
         * Find the documents of the virtual corpus in a leaf.
         *
         * @param acceptDocs
         *            The documents to consider, or <tt>null</tt>
         *            for all documents.
         */
        public DocBits supplyDocBits (LeafReaderContext context,
                Bits acceptDocs) throws IOException, QueryException {
            if (cbi == null || query == null) {
                KrillCollection kc = new KrillCollection();
                // load from file
                kc.fromStore(vcId);
                if (kc.hasErrors()) {
                    throw new QueryException(kc.getError(0).getCode(),
                            kc.getError(0).getMessage());
                }

                this.cbi = kc.getBuilder();
                this.query = kc.toQuery();
            }

            DocIdSetIterator docs = QueryDocs.iterator(
                    QueryDocs.createWeight(query, context), context);
            return calculateDocBits(docs, acceptDocs,
                    context.reader().maxDoc());
        }


        private DocBits calculateDocBits (DocIdSetIterator docs,
                Bits acceptDocs, int maxDoc) throws IOException {
            FixedBitSet bitset = new FixedBitSet(maxDoc);
            if (docs != null) {
                for (int doc = docs.nextDoc();
                        doc != DocIdSetIterator.NO_MORE_DOCS;
                        doc = docs.nextDoc()) {
                    if (acceptDocs == null || acceptDocs.get(doc))
                        bitset.set(doc);
                }
            }
            if (cbi.isNegative()) {
                bitset.flip(0, maxDoc);
            }

            return new DocBits(bitset.getBits(), bitset.length());
        }
    }

}
