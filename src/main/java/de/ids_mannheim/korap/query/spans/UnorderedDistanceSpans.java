package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
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
 * distance (within a minimum and a maximum distance) and can occur in
 * any
 * order.
 * 
 * @author margaretha
 */
public abstract class UnorderedDistanceSpans extends DistanceSpans {

    protected int minDistance, maxDistance;
    protected boolean hasMoreFirstSpans, hasMoreSecondSpans;
    protected List<CandidateSpan> firstSpanList, secondSpanList;
    protected List<CandidateSpan> matchList;
    private long matchCost;
    private int matchListSpanNum;
    protected int currentDocNum;


    /**
     * Constructs UnorderedDistanceSpans for the given
     * {@link SpanDistanceQuery} .
     * 
     * @param query
     *            a SpanDistanceQuery
     * @param context
     * @param acceptDocs
     * @param termContexts
     * @throws IOException
     */
    public UnorderedDistanceSpans (SpanDistanceQuery query,
                                   LeafReaderContext context, Bits acceptDocs,
                                   Map<Term, TermContext> termContexts)
            throws IOException {
        super(query, context, acceptDocs, termContexts);
        minDistance = query.getMinDistance();
        maxDistance = query.getMaxDistance();

        firstSpanList = new ArrayList<CandidateSpan>();
        secondSpanList = new ArrayList<CandidateSpan>();
        matchList = new ArrayList<CandidateSpan>();

        hasMoreFirstSpans = firstSpans.next();
        hasMoreSecondSpans = secondSpans.next();
        hasMoreSpans = hasMoreFirstSpans && hasMoreSecondSpans;
    }


    @Override
    protected boolean advance () throws IOException {
        while (hasMoreSpans || !matchList.isEmpty()) {
            if (!matchList.isEmpty()) {
                setMatchProperties();
                return true;
            }
            if (prepareLists())
                setMatchList();
        }
        return false;
    }


    /**
     * Updates the firstSpanList and secondSpanList by adding the next
     * possible
     * first and second spans. Both the spans must be in the same
     * document. In
     * UnorderedElementDistanceSpans, a span that is not in an element
     * (distance
     * unit), is not added to its candidate list. The element must
     * also be in
     * the same document.
     * 
     * @return <code>true</code> if at least one of the candidate
     *         lists can be
     *         filled, <code>false</code> otherwise.
     * @throws IOException
     */
    protected abstract boolean prepareLists () throws IOException;


    /**
     * Sets the list of matches for the span having the smallest
     * position (i.e. between the first and the second spans), and its
     * candidates (i.e. its counterparts). The candidates also must
     * have smaller positions. Simply remove the span if it does not
     * have any candidates.
     * 
     * @throws IOException
     */
    protected void setMatchList () throws IOException {

        hasMoreFirstSpans = setCandidateList(firstSpanList, firstSpans,
                hasMoreFirstSpans, secondSpanList);
        hasMoreSecondSpans = setCandidateList(secondSpanList, secondSpans,
                hasMoreSecondSpans, firstSpanList);
        // System.out.println("--------------------");
        // System.out.println("firstSpanList:");
        // for (CandidateSpan cs : firstSpanList) {
        // System.out.println(cs.getStart() + " " + cs.getEnd());
        // }
        //
        // System.out.println("secondSpanList:");
        // for (CandidateSpan cs : secondSpanList) {
        // System.out.println(cs.getStart() + " " + cs.getEnd());
        // }

        CandidateSpan currentFirstSpan, currentSecondSpan;
        if (!firstSpanList.isEmpty() && !secondSpanList.isEmpty()) {

            currentFirstSpan = firstSpanList.get(0);
            currentSecondSpan = secondSpanList.get(0);

            if (currentFirstSpan.getStart() < currentSecondSpan.getStart()
                    || isLastCandidateSmaller(currentFirstSpan,
                            currentSecondSpan)) {
                // log.trace("current target: "
                // + firstSpanList.get(0).getStart() + " "
                // + firstSpanList.get(0).getEnd());
                // System.out.println("candidates:");
                // for (CandidateSpan cs: secondSpanList) {
                // System.out.println(cs.getStart() +" "+ cs.getEnd());
                // }

                matchList = findMatches(currentFirstSpan, secondSpanList);
                setMatchFirstSpan(currentFirstSpan);
                matchListSpanNum = 2;
                updateList(firstSpanList);
            }
            else {
                // log.trace("current target: "
                // + secondSpanList.get(0).getStart() + " "
                // + secondSpanList.get(0).getEnd());
                // System.out.println("candidates:");
                // for (CandidateSpan cs: firstSpanList) {
                // System.out.println(cs.getStart() +" "+ cs.getEnd());
                // }

                matchList = findMatches(currentSecondSpan, firstSpanList);
                setMatchSecondSpan(currentSecondSpan);
                matchListSpanNum = 1;
                updateList(secondSpanList);
            }
        }
        else if (firstSpanList.isEmpty()) {
            // log.trace("current target: " + secondSpanList.get(0).getStart()
            // + " " + secondSpanList.get(0).getEnd());
            // log.trace("candidates: empty");
            updateList(secondSpanList);
        }
        else {
            // log.trace("current target: " + firstSpanList.get(0).getStart()
            // + " " + firstSpanList.get(0).getEnd());
            // log.trace("candidates: empty");
            updateList(firstSpanList);
        }
    }


    /**
     * Tells if the last candidate from the secondSpanList has a
     * smaller end position than the end position of the the last
     * candidate from the firstSpanList.
     * 
     * @param currentFirstSpan
     *            the current firstspan
     * @param currentSecondSpan
     *            the current secondspan
     * @return <code>true</code> if the end position of the last
     *         candidate from the secondSpanList is smaller than that
     *         from the firstSpanList, <code>false</code> otherwise.
     */
    private boolean isLastCandidateSmaller (CandidateSpan currentFirstSpan,
            CandidateSpan currentSecondSpan) {
        if (currentFirstSpan.getEnd() == currentSecondSpan.getEnd()) {
            int secondEnd = secondSpanList.get(secondSpanList.size() - 1)
                    .getEnd();
            int firstEnd = firstSpanList.get(firstSpanList.size() - 1).getEnd();
            return (secondEnd < firstEnd ? true : false);
        }

        return false;
    }


    /**
     * Performs an update based on the given candidateList. In
     * {@link UnorderedTokenDistanceSpans}, the first candidate in the
     * candidateList is simply removed. In
     * {@link UnorderedElementDistanceSpans} , the elementList is also
     * updated.
     * 
     * @param candidateList
     *            a candidateList
     */
    protected abstract void updateList (List<CandidateSpan> candidateList);


    /**
     * Sets the candidate list for the first element in the target
     * list and
     * tells if the the specified spans has finished or not.
     * 
     * @param candidateList
     *            a list of candidate spans
     * @param candidate
     *            a Spans
     * @param hasMoreCandidates
     *            a boolean
     * @param targetList
     *            a list of target spans
     * @return <code>true</code> if the span enumeration still has a
     *         next
     *         element to be a candidate, <code>false</code>
     *         otherwise.
     * @throws IOException
     */
    protected abstract boolean setCandidateList (
            List<CandidateSpan> candidateList, Spans candidate,
            boolean hasMoreCandidates, List<CandidateSpan> targetList)
            throws IOException;


    /**
     * Finds all matches between the target span and its candidates in
     * the
     * candidate list.
     * 
     * @param target
     *            a target span
     * @param candidateList
     *            a candidate list
     * @return the matches in a list
     */
    protected abstract List<CandidateSpan> findMatches (CandidateSpan target,
            List<CandidateSpan> candidateList);


    /**
     * Computes match properties and creates a candidate span match to
     * be added
     * to the match list.
     * 
     * @return a candidate span match
     */
    protected CandidateSpan createMatchCandidate (CandidateSpan target,
            CandidateSpan cs, boolean isDistanceZero) {

        int start = Math.min(target.getStart(), cs.getStart());
        int end = Math.max(target.getEnd(), cs.getEnd());
        int doc = target.getDoc();
        long cost = target.getCost() + cs.getCost();

        Collection<byte[]> payloads = new LinkedList<byte[]>();
        if (collectPayloads) {
            if (target.getPayloads() != null) {
                payloads.addAll(target.getPayloads());
            }
            if (cs.getPayloads() != null) {
                payloads.addAll(cs.getPayloads());
            }
        }
        CandidateSpan match = new CandidateSpan(start, end, doc, cost,
                payloads);
        match.setChildSpan(cs);
        return match;
    }


    /**
     * Assigns the first candidate span in the match list as the
     * current span
     * match, and removes it from the matchList.
     */
    private void setMatchProperties () {
        CandidateSpan cs = matchList.get(0);
        matchDocNumber = cs.getDoc();
        matchStartPosition = cs.getStart();
        matchEndPosition = cs.getEnd();
        matchCost = cs.getCost();
        matchPayload.addAll(cs.getPayloads());
        matchList.remove(0);

        if (matchListSpanNum == 1)
            setMatchFirstSpan(cs.getChildSpan());
        else
            setMatchSecondSpan(cs.getChildSpan());

        // log.trace("Match doc#={} start={} end={}", matchDocNumber,
        // matchStartPosition, matchEndPosition);
        // log.trace("firstspan " + getMatchFirstSpan().getStart() + " "
        // + getMatchFirstSpan().getEnd());
        // log.trace("secondspan " + getMatchSecondSpan().getStart() + " "
        // + getMatchSecondSpan().getEnd());
    }


    @Override
    public boolean skipTo (int target) throws IOException {
        if (hasMoreSpans && (secondSpans.doc() < target)) {
            if (!secondSpans.skipTo(target)) {
                hasMoreSpans = false;
                return false;
            }
        }

        firstSpanList.clear();
        secondSpanList.clear();
        matchPayload.clear();
        isStartEnumeration = false;
        return advance();
    }


    @Override
    public long cost () {
        return matchCost;
    }

}
