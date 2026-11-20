package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Map;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.ReaderUtil;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanWeight;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.KrillSpanQuery;

/**
 * Creates the spans of span queries for a leaf, limited to the
 * accepted documents.
 * 
 * Krill's queries ({@link KrillSpanQuery}) create their spans
 * directly. Lucene's queries create them through their
 * {@link SpanWeight}, which is created once per query; their spans
 * are made usable by Krill (payloads, accepted documents) with
 * {@link KrillSpans#wrap(org.apache.lucene.search.spans.Spans, Bits)}.
 * 
 * Instances are created for one searcher (i.e. one top-level reader)
 * and are not thread-safe.
 */
public class SpanWeights {

    private final IndexSearcher searcher;

    // Weights of Lucene's queries, by query object
    private final Map<SpanQuery, SpanWeight> weights = new IdentityHashMap<>();


    /**
     * @param searcher
     *            A searcher on the top-level reader of all leaves the
     *            spans are created for.
     */
    public SpanWeights (IndexSearcher searcher) {
        this.searcher = searcher;
    };


    /**
     * Create SpanWeights for the top-level reader of a leaf.
     */
    public static SpanWeights forLeaf (LeafReaderContext context) {
        return new SpanWeights(
                new IndexSearcher(ReaderUtil.getTopLevelContext(context)));
    };


    /**
     * Create the spans of a query for a leaf.
     * 
     * @param query
     *            A span query (rewritten).
     * @param context
     *            The leaf.
     * @param acceptDocs
     *            The documents matches may occur in, or
     *            <tt>null</tt> for all documents.
     * @return The spans, never <tt>null</tt>.
     * @throws IOException
     */
    public KrillSpans getSpans (SpanQuery query, LeafReaderContext context,
            Bits acceptDocs) throws IOException {

        if (query instanceof KrillSpanQuery)
            return KrillSpans.wrap(((KrillSpanQuery) query).getSpans(context,
                    acceptDocs, this));

        SpanWeight weight = this.weights.get(query);
        if (weight == null) {
            weight = query.createWeight(this.searcher,
                    ScoreMode.COMPLETE_NO_SCORES, 1f);
            this.weights.put(query, weight);
        };
        return KrillSpans.wrap(
                weight.getSpans(context, SpanWeight.Postings.PAYLOADS),
                acceptDocs);
    };
};
