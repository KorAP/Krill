package de.ids_mannheim.korap.query;

import java.io.IOException;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.spans.SpanWeights;

/**
 * A span query of Krill. Krill's queries create their spans
 * themselves, for a leaf and limited to the accepted documents, and
 * get the spans of their operands from {@link SpanWeights}.
 * 
 * Lucene (since 5.3) creates spans only through a
 * {@link org.apache.lucene.search.spans.SpanWeight}: Krill's queries
 * provide a {@link KrillSpanWeight} for that, so they can be used
 * where Lucene asks for a weight, e.g. as clauses of a
 * <tt>SpanOrQuery</tt>.
 */
public interface KrillSpanQuery {

    /**
     * Create the spans of this query for a leaf.
     * 
     * @param context
     *            The leaf.
     * @param acceptDocs
     *            The documents matches may occur in, or
     *            <tt>null</tt> for all documents.
     * @param weights
     *            Creates the spans of the operands.
     * @return The spans, never <tt>null</tt>.
     * @throws IOException
     */
    public Spans getSpans (LeafReaderContext context, Bits acceptDocs,
            SpanWeights weights) throws IOException;
};
