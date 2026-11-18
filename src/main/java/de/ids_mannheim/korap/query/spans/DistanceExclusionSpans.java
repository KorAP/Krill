package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.SpanDistanceQuery;

/**
 * Span enumeration of spans (the firstSpans) which do <em>not</em>
 * occur
 * together with other spans (the secondSpans) within a range of
 * distance.
 * 
 * @author margaretha
 */
public class DistanceExclusionSpans extends DistanceSpans {

    private int minDistance, maxDistance;
    private boolean isOrdered;
    private boolean hasMoreSecondSpans;


    /**
     * Constructs DistanceExclusionSpans for the specified
     * {@link SpanDistanceQuery}.
     * 
     * @param query
     *            a SpanDistanceQuery
     * @param context
     * @param acceptDocs
     * @param weights
     * @param isOrdered
     *            a boolean flag representing the value
     *            <code>true</code>
     *            if the spans must occur in order, <code>false</code>
     *            otherwise.
     * @throws IOException
     */
    public DistanceExclusionSpans (SpanDistanceQuery query,
                                   LeafReaderContext context, Bits acceptDocs,
                                   SpanWeights weights)
            throws IOException {
        super(query, context, acceptDocs, weights);
        minDistance = query.getMinDistance();
        maxDistance = query.getMaxDistance();
        this.isOrdered = query.isOrdered();
    }


    // The second spans are optional: a document without them has
    // matches as well. They are moved to the document of the first
    // spans, and hasMoreSecondSpans tells if they have positions in
    // it.
    @Override
    protected void startDocument () throws IOException {
        int doc = firstSpans.docID();
        if (secondSpans.docID() < doc)
            secondSpans.advance(doc);
        hasMoreSecondSpans = secondSpans.docID() == doc
                && nextInDocument(secondSpans);
    }


    @Override
    protected boolean nextMatchInDocument () throws IOException {
        isStartEnumeration = false;
        matchPayload.clear();

        while (hasMoreSpans) {
            if (hasMoreSecondSpans)
                forwardSecondSpans();

            boolean isMatch = findMatch();
            hasMoreSpans = nextInDocument(firstSpans);
            if (isMatch)
                return true;
        }
        return false;
    }


    /**
     * Advance the second span in the current document until it
     * occurs on the right side of the first span.
     * 
     * @throws IOException
     */
    private void forwardSecondSpans () throws IOException {

        // skip the secondSpan to the right side of the firstSpan
        while (hasMoreSecondSpans
                && firstSpans.startPosition() >= secondSpans.endPosition()) {

            // the firstspan is within maxDistance
            if (!isOrdered && calculateActualDistance() <= maxDistance) {
                break;
            }
            hasMoreSecondSpans = nextInDocument(secondSpans);
        }
    }


    /**
     * Calculate the distance / difference between a firstspan and a
     * secondspan
     * positions.
     * 
     * @return distance the difference between the positions of a
     *         firstspan and
     *         a secondspan.
     */
    private int calculateActualDistance () {
        // right secondSpan
        if (firstSpans.endPosition() <= secondSpans.startPosition())
            return secondSpans.startPosition() - firstSpans.endPosition() + 1;
        // left secondSpan
        return firstSpans.startPosition() - secondSpans.endPosition() + 1;
    }


    /**
     * Check the distance between the current first span and second
     * span against
     * the min and max distance constraints.
     * 
     * @return true if the distance between the first and the second
     *         spans are
     *         smaller as the minimum distance or bigger than the max
     *         distance.
     */
    private boolean findMatch () throws IOException {
        // No more second spans in the document
        if (!hasMoreSecondSpans) {
            setMatchProperties();
            return true;
        }
        if (minDistance == 0
                && firstSpans.startPosition() < secondSpans.endPosition()
                && secondSpans.startPosition() < firstSpans.endPosition()) {
            return false;
        }

        int actualDistance = calculateActualDistance();
        if (actualDistance < minDistance || actualDistance > maxDistance) {
            setMatchProperties();
            return true;
        }

        return false;
    }


    /**
     * Set the current firstspan as the current match.
     * 
     * @throws IOException
     */
    private void setMatchProperties () throws IOException {
        matchDocNumber = firstSpans.docID();
        matchStartPosition = firstSpans.startPosition();
        matchEndPosition = firstSpans.endPosition();

        if (collectPayloads && firstSpans.isPayloadAvailable())
            matchPayload.addAll(firstSpans.getPayload());

        setMatchFirstSpan(new CandidateSpan(firstSpans));
    }


    @Override
    public long cost () {
        return firstSpans.cost() + secondSpans.cost();
    }

}
