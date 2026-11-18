package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.KrillSpanQuery;

/**
 * Creates the spans of span queries for a leaf, limited to the
 * accepted documents.
 * 
 * Krill's queries ({@link KrillSpanQuery}) create their spans
 * directly. Lucene's queries create them with the term contexts of
 * the search; their spans are made usable by Krill with
 * {@link LegacySpans#wrap(org.apache.lucene.search.spans.Spans)}.
 * 
 * Instances are created for one search and are not thread-safe.
 */
public class SpanWeights {

    private final Map<Term, TermContext> termContexts;


    public SpanWeights () {
        this(new HashMap<Term, TermContext>());
    };


    /**
     * @param termContexts
     *            The term contexts of the search.
     */
    public SpanWeights (Map<Term, TermContext> termContexts) {
        this.termContexts = termContexts;
    };


    /**
     * Create SpanWeights for a leaf.
     */
    public static SpanWeights forLeaf (LeafReaderContext context) {
        return new SpanWeights();
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
    public LegacySpans getSpans (SpanQuery query, LeafReaderContext context,
            Bits acceptDocs) throws IOException {

        if (query instanceof KrillSpanQuery)
            return LegacySpans.wrap(((KrillSpanQuery) query).getSpans(context,
                    acceptDocs, this));

        return LegacySpans.wrap(
                query.getSpans(context, acceptDocs, this.termContexts));
    };
};
