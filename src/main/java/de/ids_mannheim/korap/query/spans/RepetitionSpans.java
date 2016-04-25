package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.SpanRepetitionQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enumeration of spans occurring multiple times in a sequence. The
 * number of
 * repetition depends on the min and max parameters.
 * 
 * @author margaretha
 * */
public class RepetitionSpans extends SimpleSpans {

    // Logger
    private final Logger log = LoggerFactory.getLogger(RepetitionSpans.class);

    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;

    private int min, max;
    private long matchCost;
    private List<CandidateSpan> matchList;
    private List<CandidateSpan> candidates;

    /**
     * Constructs RepetitionSpans from the given
     * {@link SpanRepetitionQuery}.
     * 
     * @param query
     *            a SpanRepetitionQuery
     * @param context
     * @param acceptDocs
     * @param termContexts
     * @throws IOException
     */
    public RepetitionSpans (SpanRepetitionQuery query,
                            LeafReaderContext context, Bits acceptDocs,
                            Map<Term, TermContext> termContexts)
            throws IOException {
        super(query, context, acceptDocs, termContexts);
        this.min = query.getMin();
        this.max = query.getMax();
        matchList = new ArrayList<CandidateSpan>();
        candidates = new ArrayList<CandidateSpan>();
        hasMoreSpans = firstSpans.next();
    }


    @Override
    public boolean next () throws IOException {
        isStartEnumeration = false;
        matchPayload.clear();
        return advance();
    }


    /**
     * Advances the RepetitionSpans to the next match by setting the
     * first
     * element in the matchlist as the current match. When the
     * matchlist is
     * empty, it has to be set first.
     * 
     * @return <code>true</code> if a match is found,
     *         <code>false</code>
     *         otherwise.
     * @throws IOException
     */
    private boolean advance () throws IOException {

        while (hasMoreSpans || !matchList.isEmpty()) {
            if (!matchList.isEmpty()) {
                setMatchProperties(matchList.get(0));
                matchList.remove(0);
                return true;
            }
            matchCost = 0;

            List<CandidateSpan> adjacentSpans = collectAdjacentSpans();
            setMatchList(adjacentSpans);
        }
        return false;
    }


    /**
     * Collects all adjacent firstspans occurring in a sequence.
     * 
     * @return a list of the adjacent spans
     * @throws IOException
     */
    private List<CandidateSpan> collectAdjacentSpans () throws IOException {

        CandidateSpan startSpan;
        if (!candidates.isEmpty()) {
            startSpan = candidates.get(0);
            candidates.remove(0);
        }
        else {
            startSpan = new CandidateSpan(firstSpans);
        }

        List<CandidateSpan> adjacentSpans = new ArrayList<CandidateSpan>();
        adjacentSpans.add(startSpan);

        CandidateSpan prevSpan = startSpan;

        int i = 0;
        while (i < candidates.size()) {
            CandidateSpan cs = candidates.get(i);
            if (cs.getStart() > prevSpan.getEnd()) {
                break;
            }
            else if (startSpan.getDoc() == cs.getDoc()
                    && cs.getStart() == prevSpan.getEnd()) {
                prevSpan = cs;
                adjacentSpans.add(prevSpan);
            }
            i++;
        }
        while ((hasMoreSpans = firstSpans.next())
                && startSpan.getDoc() == firstSpans.doc()) {

            if (DEBUG) {
                log.debug("Check adjacency at {}-{}|{}-{} in {}",
                          prevSpan.getStart(),
                          prevSpan.getEnd(),
                          firstSpans.start(),
                          firstSpans.end(),
                          startSpan.getDoc());
            };

            if (firstSpans.start() > prevSpan.getEnd()) {
                candidates.add(new CandidateSpan(firstSpans));
                break;
            }
            else if (firstSpans.start() == prevSpan.getEnd()) {
                prevSpan = new CandidateSpan(firstSpans);
                adjacentSpans.add(prevSpan);
            }
            else {
                candidates.add(new CandidateSpan(firstSpans));
            }
        }
        return adjacentSpans;
    }

    /**
     * Generates all possible repetition match spans from the given
     * list of
     * adjacent spans and add them to the match list.
     * 
     * @param adjacentSpans
     */
    private void setMatchList (List<CandidateSpan> adjacentSpans) {
        CandidateSpan startSpan, endSpan, matchSpan;
        for (int i = min; i < max + 1; i++) {
            int j = 0;
            int endIndex;
            while ((endIndex = j + i - 1) < adjacentSpans.size()) {
                startSpan = adjacentSpans.get(j);
                
                if (i == 1) {
                    try {
                         matchSpan = startSpan.clone();
                         matchSpan.setPayloads(computeMatchPayload(
                                 adjacentSpans, 0, endIndex - 1));
                         matchList.add(matchSpan);
                    }
                    catch (CloneNotSupportedException e) {
                            e.printStackTrace();
                        }
                }
                else {
                    endSpan = adjacentSpans.get(endIndex);
                    matchSpan = new CandidateSpan(startSpan.getStart(),
                            endSpan.getEnd(), startSpan.getDoc(),
                            computeMatchCost(adjacentSpans, 0, endIndex),
                            computeMatchPayload(adjacentSpans, 0, endIndex));
                    //System.out.println("c:"+matchSpan.getCost() +" p:"+ matchSpan.getPayloads().size());
                    //System.out.println(startSpan.getStart() +","+endSpan.getEnd());
                    matchList.add(matchSpan);
                }
                j++;
            }

            if (j + i == adjacentSpans.size()) {

            }
        }
        Collections.sort(matchList);
    }


    /**
     * Creates payloads by adding all the payloads of some adjacent
     * spans, that
     * are all spans in the given list whose index is between the
     * start and end
     * index (including those with these indexes).
     * 
     * @param adjacentSpans
     *            a list of adjacentSpans
     * @param start
     *            the start index representing the first adjacent span
     *            in the
     *            list to be computed
     * @param end
     *            the end index representing the last adjacent span in
     *            the list
     *            to be computed
     * @return payloads
     */
    private Collection<byte[]> computeMatchPayload (
            List<CandidateSpan> adjacentSpans, int start, int end) {
        Collection<byte[]> payload = new ArrayList<byte[]>();
        for (int i = start; i <= end; i++) {
            payload.addAll(adjacentSpans.get(i).getPayloads());
        }
        return payload;
    }


    /**
     * Computes the matchcost by adding all the cost of the adjacent
     * spans
     * between the start and end index in the given list.
     * 
     * @param adjacentSpans
     *            a list of adjacent spans
     * @param start
     *            the start index
     * @param end
     *            the end index
     * @return
     */
    private long computeMatchCost (List<CandidateSpan> adjacentSpans,
            int start, int end) {
        long matchCost = 0;
        for (int i = start; i <= end; i++) {
            matchCost += adjacentSpans.get(i).getCost();
        }
        return matchCost;
    }


    /**
     * Sets properties for the current match from the given candidate
     * span.
     * 
     * @param candidateSpan
     *            the match candidate span
     * @throws IOException
     */
    private void setMatchProperties (CandidateSpan candidateSpan)
            throws IOException {
        matchDocNumber = candidateSpan.getDoc();
        matchStartPosition = candidateSpan.getStart();
        matchEndPosition = candidateSpan.getEnd();
        if (collectPayloads && candidateSpan.getPayloads() != null) {
            matchPayload.addAll(candidateSpan.getPayloads());
        }
    }


    @Override
    public boolean skipTo (int target) throws IOException {
        if (!candidates.isEmpty()) {
            Iterator<CandidateSpan> i = candidates.iterator();
            while (i.hasNext()) {
                CandidateSpan cs = i.next();
                if (cs.getDoc() < target) {
                    i.remove();
                }
                else if (cs.getDoc() == target) {
                    matchList.clear();
                    return advance();
                }
            }
        }
        if (hasMoreSpans && firstSpans.doc() < target) {
            if (!firstSpans.skipTo(target)) {
                hasMoreSpans = false;
                return false;
            }
        }
        matchList.clear();
        return advance();
    }


    @Override
    public long cost () {
        return matchCost;
    }
}
