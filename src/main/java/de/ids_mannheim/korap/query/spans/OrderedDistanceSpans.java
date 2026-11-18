package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.SpanDistanceQuery;

/**
 * Base class for calculating a distance between two ordered spans.
 * 
 * @author margaretha
 */
public abstract class OrderedDistanceSpans extends DistanceSpans {

    protected boolean hasMoreFirstSpans;
    protected int minDistance, maxDistance;

    protected List<CandidateSpan> candidateList;
    protected int candidateListIndex;

    // The candidates have to be checked against the current
    // second span
    private boolean candidatesPending;


    /**
     * Constructs OrderedDistanceSpans based on the given
     * SpanDistanceQuery.
     * 
     * @param query
     *            a SpanDistanceQuery
     * @param context
     * @param acceptDocs
     * @param termContexts
     * @throws IOException
     */
    public OrderedDistanceSpans (SpanDistanceQuery query,
                                 LeafReaderContext context, Bits acceptDocs,
                                 Map<Term, TermContext> termContexts)
            throws IOException {
        super(query, context, acceptDocs, termContexts);

        minDistance = query.getMinDistance();
        maxDistance = query.getMaxDistance();

        candidateList = new ArrayList<>();
        candidateListIndex = -1;
    }


    // The second spans drive the iteration: the matches are
    // collected for each second span
    @Override
    protected LegacySpans driver () {
        return secondSpans;
    }


    @Override
    protected List<? extends LegacySpans> requiredOperands () {
        return Collections.singletonList(firstSpans);
    }


    @Override
    protected void startDocument () throws IOException {
        candidateList.clear();
        candidateListIndex = -1;
        candidatesPending = false;
        hasMoreFirstSpans = nextInDocument(firstSpans);
    }


    /**
     * Finds the next span match in the current document by checking
     * the candidates of each second span.
     */
    @Override
    protected boolean nextMatchInDocument () throws IOException {
        isStartEnumeration = false;
        matchPayload.clear();

        while (true) {
            // Check candidates
            if (candidatesPending) {
                for (candidateListIndex++; candidateListIndex < candidateList
                        .size(); candidateListIndex++) {
                    if (findMatch())
                        return true;
                }
                candidatesPending = false;
                // Forward secondspan
                hasMoreSpans = nextInDocument(secondSpans);
            }

            if (!hasMoreSpans)
                return false;

            setCandidateList();
            if (isSecondSpanValid())
                candidatesPending = true;
            else
                hasMoreSpans = nextInDocument(secondSpans);
        }
    }


    /**
     * Determines if the current second span is valid (i.e. within an
     * element).
     * It is always valid in TokenDistanceSpan, but it can be invalid
     * in the
     * ElementDistanceSpan, namely when it is not within a particular
     * element (a
     * sentence or a paragraph depends on the element distance unit).
     * 
     * @return <code>true</code> of the current second span is valid,
     *         <code>false</code> otherwise.
     * @throws IOException
     */
    protected abstract boolean isSecondSpanValid () throws IOException;


    /**
     * Stores/collects the states of all possible firstspans as
     * candidate spans
     * for the current secondspan (in the current document). The candidate spans must be within
     * the
     * maximum distance from the current secondspan.
     * 
     * @throws IOException
     */
    protected abstract void setCandidateList () throws IOException;


    /**
     * Defines the conditions for a match and tells if a match is
     * found.
     * 
     * @return <code>true</code> if a match is found,
     *         <code>false</code>
     *         otherwise.
     * @throws IOException
     */
    protected abstract boolean findMatch () throws IOException;


    /**
     * Defines the properties of a span match. The distance between
     * the first
     * and the second spans is zero, when there is an intersection
     * between them
     * in {@link TokenDistanceSpans}, or they occur in the same
     * element in {@link ElementDistanceSpans}.
     * 
     * @param candidateSpan
     *            a match span
     * @param isDistanceZero
     *            <code>true</code> if the distance between the first
     *            and the second spans is zero, <code>false</code>
     *            otherwise.
     * @throws IOException
     */
    protected void setMatchProperties (CandidateSpan candidateSpan,
            boolean isDistanceZero) throws IOException {

        setMatchFirstSpan(candidateSpan);
        setMatchSecondSpan(new CandidateSpan(secondSpans));

        if (isDistanceZero) {
            matchStartPosition = Math.min(candidateSpan.getStart(),
                    secondSpans.startPosition());
            matchEndPosition = Math.max(candidateSpan.getEnd(),
                    secondSpans.endPosition());
        }
        else {
            matchStartPosition = candidateSpan.getStart();
            matchEndPosition = secondSpans.endPosition();
        }

        this.matchDocNumber = secondSpans.docID();
        if (collectPayloads) {
            if (candidateSpan.getPayloads() != null) {
                matchPayload.addAll(candidateSpan.getPayloads());
            }
            if (secondSpans.isPayloadAvailable()) {
                matchPayload.addAll(secondSpans.getPayload());
            }
        }
    }
}
