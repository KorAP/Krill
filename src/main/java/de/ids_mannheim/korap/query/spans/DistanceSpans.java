package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.SpanDistanceQuery;
import de.ids_mannheim.korap.query.SpanMultipleDistanceQuery;

/**
 * DistanceSpan is a base class for enumeration of span matches, whose
 * two child spans have a specific range of distance (within a min and
 * a max distance) and other constraints (i.e. order and
 * co-occurrence) depending on the {@link SpanDistanceQuery}. All
 * distance related spans extends this class.
 * 
 * @see DistanceExclusionSpans
 * @see ElementDistanceExclusionSpans
 * @see OrderedDistanceSpans
 * @see UnorderedDistanceSpans
 * @see MultipleDistanceSpans
 * 
 * @author margaretha
 */
public abstract class DistanceSpans extends SimpleSpans {

    protected CandidateSpan matchFirstSpan, matchSecondSpan;
    protected boolean exclusion; // for MultipleDistanceQuery


    /**
     * Constructs a DistanceSpans enumeration for the given
     * {@link SpanDistanceQuery}.
     * 
     * @param query
     *            a SpanDistanceQuery
     * @param context
     * @param acceptDocs
     * @param termContexts
     * @throws IOException
     */
    public DistanceSpans (SpanDistanceQuery query, LeafReaderContext context,
                          Bits acceptDocs, Map<Term, TermContext> termContexts)
            throws IOException {
        super(query, context, acceptDocs, termContexts);
        exclusion = query.isExclusion();
    }


    /**
     * Constructs a DistanceSpans enumeration for the given
     * {@link SpanMultipleDistanceQuery}.
     * 
     * @param query
     *            a SpanMultipleDistanceQuery
     * @param context
     * @param acceptDocs
     * @param termContexts
     * @throws IOException
     */
    public DistanceSpans (SpanMultipleDistanceQuery query,
                          LeafReaderContext context, Bits acceptDocs,
                          Map<Term, TermContext> termContexts)
            throws IOException {
        super(query, context, acceptDocs, termContexts);
    }


    @Override
    public boolean next () throws IOException {
        isStartEnumeration = false;
        matchPayload.clear();
        return advance();
    }


    /**
     * Advances the current span enumeration to the next span match.
     * 
     * @return <code>true</code> if a span match is available,
     *         <code>false</code> otherwise.
     */
    protected abstract boolean advance () throws IOException;


    /**
     * Returns the first span of the current match.
     * 
     * @return the first span of the current match.
     */
    public CandidateSpan getMatchFirstSpan () {
        return matchFirstSpan;
    }


    /**
     * Sets the first span of the current match.
     * 
     * @param matchFirstSpan
     *            the first span of the current match.
     */
    public void setMatchFirstSpan (CandidateSpan matchFirstSpan) {
        this.matchFirstSpan = matchFirstSpan;
    }


    /**
     * Returns the second span of the current match.
     * 
     * @return the second span of the current match.
     */
    public CandidateSpan getMatchSecondSpan () {
        return matchSecondSpan;
    }


    /**
     * Sets the second span of the current match.
     * 
     * @param matchSecondSpan
     *            the second span of the current match.
     */
    public void setMatchSecondSpan (CandidateSpan matchSecondSpan) {
        this.matchSecondSpan = matchSecondSpan;
    }


    /**
     * Tells if the second span must occur together with the first
     * span, or not.
     * 
     * @return <code>true</code> if the second span must <em>not</em>
     *         occur
     *         together with the first span, <code>false</code>
     *         otherwise.
     */
    public boolean isExclusion () {
        return exclusion;
    }


    /**
     * Sets <code>true</code> if the second span must <em>not</em>
     * occur
     * together with the first span, <code>false</code> otherwise.
     * 
     * @param exclusion
     *            a boolean with the value <code>true</code> if the
     *            second
     *            span must <em>not</em> occur together with the first
     *            span,
     *            <code>false</code> otherwise.
     */
    public void setExclusion (boolean exclusion) {
        this.exclusion = exclusion;
    }

}
