package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.SpanDistanceQuery;

/**
 * Enumeration of span matches, whose two child spans have a specific
 * range of
 * distance (within a min and a max distance) and can be in any order.
 * The unit
 * distance is a token position.
 * 
 * @author margaretha
 */
public class UnorderedTokenDistanceSpans extends UnorderedDistanceSpans {

    /**
     * Constructs UnorderedTokenDistanceSpans for the given
     * SpanDistanceQuery.
     * 
     * @param query
     *            a SpanDistanceQuery
     * @param context
     * @param acceptDocs
     * @param weights
     * @throws IOException
     */
    public UnorderedTokenDistanceSpans (SpanDistanceQuery query,
                                        LeafReaderContext context,
                                        Bits acceptDocs,
                                        SpanWeights weights)
            throws IOException {
        super(query, context, acceptDocs, weights);
    }


    @Override
    protected boolean prepareLists () throws IOException {

        if (firstSpanList.isEmpty() && secondSpanList.isEmpty()) {
            if (hasMoreFirstSpans && hasMoreSecondSpans) {
                firstSpanList.add(new CandidateSpan(firstSpans));
                secondSpanList.add(new CandidateSpan(secondSpans));
                hasMoreFirstSpans = nextInDocument(firstSpans);
                hasMoreSecondSpans = nextInDocument(secondSpans);
            }
            else {
                hasMoreSpans = false;
                return false;
            }
        }
        else if (firstSpanList.isEmpty() && hasMoreFirstSpans) {
            firstSpanList.add(new CandidateSpan(firstSpans));
            hasMoreFirstSpans = nextInDocument(firstSpans);
        }
        else if (secondSpanList.isEmpty() && hasMoreSecondSpans) {
            secondSpanList.add(new CandidateSpan(secondSpans));
            hasMoreSecondSpans = nextInDocument(secondSpans);
        }
        return true;
    }


    @Override
    protected boolean setCandidateList (List<CandidateSpan> candidateList,
            KrillSpans candidate, boolean hasMoreCandidates,
            List<CandidateSpan> targetList) throws IOException {

        if (!targetList.isEmpty()) {
            CandidateSpan target = targetList.get(0);
            while (hasMoreCandidates
                    && isWithinMaxDistance(target, candidate)) {
                candidateList.add(new CandidateSpan(candidate));
                hasMoreCandidates = nextInDocument(candidate);
            }
        }
        return hasMoreCandidates;
    }


    /**
     * Tells if the target and candidate spans are not too far from
     * each other
     * (within the maximum distance).
     * 
     * @param target
     *            a target span
     * @param candidate
     *            a candidate span
     * @return <code>true</code> if the target and candidate spans are
     *         within
     *         the maximum distance, <code>false</code> otherwise.
     */
    protected boolean isWithinMaxDistance (CandidateSpan target,
            KrillSpans candidate) {
        // left candidate
        if (candidate.endPosition() < target.getStart()
                && candidate.endPosition() + maxDistance <= target.getStart()) {
            return false;
        }
        // right candidate
        if (candidate.startPosition() > target.getEnd()
                && target.getEnd() + maxDistance <= candidate
                        .startPosition()) {
            return false;
        }
        return true;
    }


    @Override
    protected List<CandidateSpan> findMatches (CandidateSpan target,
            List<CandidateSpan> candidateList, boolean isTargetFirstSpan) {

        List<CandidateSpan> matches = new ArrayList<>();
        int actualDistance;
        CandidateSpan match;
        for (CandidateSpan cs : candidateList) {
            if (minDistance == 0 &&
            // intersection
                    target.getStart() < cs.getEnd()
                    && cs.getStart() < target.getEnd()) {
                match = createMatchCandidate(target, cs, true, isTargetFirstSpan);
                matches.add(match);
                continue;
            }

            // left candidate
            if (cs.getEnd() < target.getStart()) {
                actualDistance = target.getStart() - cs.getEnd() + 1;
            }
            else {
                // right candidate
                actualDistance = cs.getStart() - target.getEnd() + 1;
            }
            if (minDistance <= actualDistance
                    && actualDistance <= maxDistance) {
                match = createMatchCandidate(target, cs, false, isTargetFirstSpan);
                matches.add(match);
            }
        }
        return matches;
    }


    @Override
    protected void updateList (List<CandidateSpan> candidateList) {
        candidateList.remove(0);
    }

}
