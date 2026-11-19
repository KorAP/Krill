package de.ids_mannheim.korap.collection;

import java.io.IOException;

import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.ReaderUtil;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;

/**
 * Finds the documents a query matches in a leaf, as
 * <tt>QueryWrapperFilter</tt> did: without scores, without caching
 * and without checking for deleted documents.
 *
 * The weight of a query is created once for the top-level reader
 * and can be used for all of its leaves.
 */
public final class QueryDocs {

    private QueryDocs () {};


    /**
     * Create the weight of a query for a top-level reader.
     */
    public static Weight createWeight (Query query, IndexReaderContext top)
            throws IOException {
        IndexSearcher searcher = new IndexSearcher(top);
        searcher.setQueryCache(null);
        return searcher.createNormalizedWeight(query, false);
    };


    /**
     * Create the weight of a query for the top-level reader of a
     * leaf.
     */
    public static Weight createWeight (Query query, LeafReaderContext leaf)
            throws IOException {
        return createWeight(query, ReaderUtil.getTopLevelContext(leaf));
    };


    /**
     * The documents of a leaf the weight matches.
     *
     * @return The documents, or <tt>null</tt> if there are none.
     */
    public static DocIdSetIterator iterator (Weight weight,
            LeafReaderContext leaf) throws IOException {
        Scorer scorer = weight.scorer(leaf);
        return (scorer == null) ? null : scorer.iterator();
    };
};
