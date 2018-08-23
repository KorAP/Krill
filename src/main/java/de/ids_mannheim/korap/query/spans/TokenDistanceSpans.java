package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.SpanDistanceQuery;

/**
 * Enumeration of token-based distance span matches consisting of two
 * child
 * spans having an actual distance in the range of the minimum and
 * maximum
 * distance parameters specified in the corresponding query. A
 * TokenDistanceSpan
 * starts from the minimum start positions of its child spans and ends
 * at the
 * maximum end positions of the child spans.
 * 
 * @author margaretha
 */
public class TokenDistanceSpans extends OrderedDistanceSpans {

    /**
     * Constructs TokenDistanceSpans from the given query.
     * 
     * @param query
     *            a SpanDistanceQuery
     * @param context
     * @param acceptDocs
     * @param termContexts
     * @throws IOException
     */
    public TokenDistanceSpans (SpanDistanceQuery query,
                               LeafReaderContext context, Bits acceptDocs,
                               Map<Term, TermContext> termContexts)
            throws IOException {
        super(query, context, acceptDocs, termContexts);
        hasMoreSpans = hasMoreFirstSpans;
    }


    @Override
    protected void setCandidateList () throws IOException {
        if (candidateListDocNum == secondSpans.doc()) {
            copyPossibleCandidates();
            addNewCandidates();
            candidateListIndex = -1;
        }
        else {
            candidateList.clear();
            if (hasMoreFirstSpans && ensureSameDoc(firstSpans, secondSpans)) {
                candidateListDocNum = firstSpans.doc();
                addNewCandidates();
                candidateListIndex = -1;
            }
        }
    }


    /**
     * Restructures the candidateList to contain only candidate
     * (first) spans
     * which are still possible to create a match, from the candidate
     * list
     * prepared for the previous second spans.
     * 
     */
    private void copyPossibleCandidates () {
        List<CandidateSpan> temp = new ArrayList<>();
        for (CandidateSpan cs : candidateList) {
            if (cs.getEnd() + maxDistance > secondSpans.start())
                temp.add(cs);
        }
        candidateList = temp;
    }


    /**
     * Add new possible firstspan candidates for the current
     * secondspan.
     */
    private void addNewCandidates () throws IOException {
        while (hasMoreFirstSpans && firstSpans.doc() == candidateListDocNum
                && firstSpans.start() < secondSpans.end()) {

            if (firstSpans.end() + maxDistance > secondSpans.start())
                candidateList.add(new CandidateSpan(firstSpans));

            hasMoreFirstSpans = firstSpans.next();
        }
    }


    @Override
    protected boolean findMatch () throws IOException {
        CandidateSpan candidateSpan = candidateList.get(candidateListIndex);
        if (minDistance == 0
                &&
                // intersection
                candidateSpan.getStart() < secondSpans.end()
                && secondSpans.start() < candidateSpan.getEnd()) {

            setMatchProperties(candidateSpan, true);
            return true;
        }

        int actualDistance = secondSpans.start() - candidateSpan.getEnd() + 1;
        if (candidateSpan.getStart() < secondSpans.start()
                && minDistance <= actualDistance
                && actualDistance <= maxDistance) {

            setMatchProperties(candidateSpan, false);
            return true;
        }
        return false;
    }


    @Override
    public long cost () {
        if (candidateList.size() > 0) {
            long cost = 0;
            for (CandidateSpan candidateSpan : candidateList) {
                cost += candidateSpan.getCost();
            }
            return cost + secondSpans.cost();
        }
        else {
            return firstSpans.cost() + secondSpans.cost();
        }
    }


    @Override
    protected boolean isSecondSpanValid () throws IOException {
        return true;
    }
}
