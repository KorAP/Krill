package de.ids_mannheim.korap.query;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanWeight;
import org.apache.lucene.search.spans.Spans;

import de.ids_mannheim.korap.query.spans.SpanWeights;

/**
 * The weight of a {@link KrillSpanQuery}. It creates the spans of the
 * query (for all documents of a leaf - accepted documents are checked
 * by whoever asks).
 * 
 * Krill does not score matches itself. If Lucene asks for scores
 * (e.g. <tt>IndexSearcher.search()</tt>), the term statistics of all
 * terms of the query are used, as Lucene's span queries do.
 */
public class KrillSpanWeight extends SpanWeight {

    private final KrillSpanQuery query;
    private final SpanWeights weights;

    // Term statistics, only if scores are needed
    private final Map<Term, TermContext> termContexts;


    public KrillSpanWeight (SpanQuery query, IndexSearcher searcher,
            boolean needsScores, float boost) throws IOException {
        this(query, searcher,
                needsScores ? buildTermContexts(query, searcher) : null,
                boost);
    };


    private KrillSpanWeight (SpanQuery query, IndexSearcher searcher,
            Map<Term, TermContext> termContexts, float boost)
            throws IOException {
        super(query, searcher, termContexts, boost);
        this.query = (KrillSpanQuery) query;
        this.weights = new SpanWeights(searcher);
        this.termContexts = termContexts;
    };


    private static Map<Term, TermContext> buildTermContexts (SpanQuery query,
            IndexSearcher searcher) throws IOException {
        Set<Term> terms = new TreeSet<>();
        SimpleSpanQuery.collectTerms(query, terms);
        IndexReaderContext top = searcher.getTopReaderContext();
        Map<Term, TermContext> termContexts = new TreeMap<>();
        for (Term term : terms)
            termContexts.put(term, TermContext.build(top, term));
        return termContexts;
    };


    @Override
    public void extractTermContexts (Map<Term, TermContext> contexts) {
        if (this.termContexts != null)
            contexts.putAll(this.termContexts);
    };


    @Override
    public void extractTerms (Set<Term> terms) {
        SimpleSpanQuery.collectTerms((SpanQuery) this.getQuery(), terms);
    };


    // Krill's spans are not cached by Lucene's query cache
    @Override
    public boolean isCacheable (LeafReaderContext context) {
        return false;
    };


    @Override
    public Spans getSpans (LeafReaderContext context,
            Postings requiredPostings) throws IOException {
        return this.query.getSpans(context, null, this.weights);
    };
};
