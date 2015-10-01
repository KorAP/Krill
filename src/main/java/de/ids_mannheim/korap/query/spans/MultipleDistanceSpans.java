package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.SpanMultipleDistanceQuery;

/**
 * Span enumeration of matches whose two sub-spans have exactly the
 * same first
 * and second sub-sub-spans. To obtain these matches, the span matches
 * of the
 * child spans are filtered.
 * 
 * MultipleDistanceSpans accommodates distance constraint with
 * exclusion. <br />
 * <br />
 * 
 * This class deals with the following cases:
 * <ol>
 * <li>return the match from another non-exclusion constraint.</li>
 * <li>return only the first-span when all constraints are
 * exclusions.</li>
 * <li>spans are not in the same doc</li>
 * </ol>
 * 
 * @author margaretha
 * */
public class MultipleDistanceSpans extends DistanceSpans {

    private DistanceSpans x, y;
    private boolean isOrdered;


    /**
     * Constructs MultipleDistanceSpans for the two given Spans with
     * the given {@link SpanMultipleDistanceQuery}.
     * 
     * @param query
     *            a SpanMultipleDistanceQuery
     * @param context
     * @param acceptDocs
     * @param termContexts
     * @param firstSpans
     *            the firstspans
     * @param secondSpans
     *            the secondspans
     * @param isOrdered
     *            <code>true</code> if the spans must occur in order,
     *            <code>false</code> otherwise.
     * @param exclusion
     *            <code>true</code> if the secondspans must
     *            <em>not</em>
     *            occur together with the firstspans.
     * @throws IOException
     */
    public MultipleDistanceSpans (SpanMultipleDistanceQuery query,
                                  LeafReaderContext context, Bits acceptDocs,
                                  Map<Term, TermContext> termContexts,
                                  Spans firstSpans, Spans secondSpans,
                                  boolean isOrdered, boolean exclusion)
            throws IOException {
        super(query, context, acceptDocs, termContexts);
        this.isOrdered = isOrdered;
        this.exclusion = exclusion;
        x = (DistanceSpans) firstSpans;
        y = (DistanceSpans) secondSpans;
        hasMoreSpans = x.next() && y.next();
    }


    @Override
    public boolean next () throws IOException {
        isStartEnumeration = false;
        matchPayload.clear();
        return advance();
    }


    /**
     * Finds the next match.
     * */
    protected boolean advance () throws IOException {
        while (hasMoreSpans && ensureSameDoc(x, y)) {
            if (findMatch()) {
                moveForward();
                return true;
            }
            moveForward();
        }
        return false;
    }


    /**
     * Finds the next match of one of the sub/child-span.
     * 
     * @throws IOException
     */
    private void moveForward () throws IOException {
        if (isOrdered) {
            if (x.end() < y.end()
                    || (x.end() == y.end() && x.start() < y.start()))
                hasMoreSpans = x.next();
            else
                hasMoreSpans = y.next();
        }
        // The matches of unordered distance spans are ordered by the 
        // start position
        else {
            if (x.start() < y.start()
                    || (x.start() == y.start() && x.end() < y.end()))
                hasMoreSpans = x.next();
            else
                hasMoreSpans = y.next();
        }
    }


    /**
     * Checks if the sub-spans of x and y having exactly the same
     * position. This
     * is basically an AND operation.
     * 
     * @return true iff the sub-spans are identical.
     * @throws IOException
     */
    protected boolean findMatch () throws IOException {

        CandidateSpan xf = x.getMatchFirstSpan();
        CandidateSpan xs = x.getMatchSecondSpan();

        CandidateSpan yf = y.getMatchFirstSpan();
        CandidateSpan ys = y.getMatchSecondSpan();

        if (x.isExclusion() || y.isExclusion()) {
            if (xf.getStart() == yf.getStart() && xf.getEnd() == yf.getEnd()) {
                // case 2
                if (x.isExclusion() && y.isExclusion()) {
                    // set x or y doesnt matter
                    setMatchProperties(x, true);
                }
                // case 1
                else if (x.isExclusion()) {
                    // set y, the usual match
                    setMatchProperties(y, true);
                }
                // case 1
                else {
                    setMatchProperties(x, true);
                }
                return true;
            }
        }
        else if (xf.getStart() == yf.getStart() && xf.getEnd() == yf.getEnd()
                && xs.getStart() == ys.getStart() && xs.getEnd() == ys.getEnd()) {
            setMatchProperties(x, false);
            return true;
        }
        return false;
    }


    /**
     * Sets the properties of the given span as the current match
     * properties.
     * 
     * @param span
     *            a DistanceSpan
     * @param exclusion
     *            <code>true</code> if the spans must <em>not</em>
     *            occur
     *            together, <code>false</code> otherwise.
     */
    private void setMatchProperties (DistanceSpans span, boolean exclusion) {
        matchStartPosition = span.start();
        matchEndPosition = span.end();
        matchDocNumber = span.doc();
        matchPayload = span.matchPayload;

        setMatchFirstSpan(span.getMatchFirstSpan());
        if (!exclusion)
            setMatchSecondSpan(span.getMatchSecondSpan());
    }


    @Override
    public boolean skipTo (int target) throws IOException {
        if (hasMoreSpans && (y.doc() < target)) {
            if (!y.skipTo(target)) {
                return false;
            }
        }
        matchPayload.clear();
        isStartEnumeration = false;
        return advance();
    }


    @Override
    public long cost () {
        return x.cost() + y.cost();
    }

}
