package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.util.Bits;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ids_mannheim.korap.query.SpanNextQuery;

/**
 * NextSpans is an enumeration of Span matches, which ensures that a
 * span is
 * immediately followed by another span.
 * 
 * The implementation allows multiple matches at the same firstspan
 * position.
 * 
 * @author margaretha
 * @author diewald
 */
public class NextSpans extends SimpleSpans {

    // Logger
    private final Logger log = LoggerFactory.getLogger(NextSpans.class);

    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;

    private List<CandidateSpan> matchList;
    private List<CandidateSpan> candidateList;
    private int candidateListDocNum;
    private boolean hasMoreFirstSpan;

    /**
     * Constructs NextSpans for the given {@link SpanNextQuery}.
     * 
     * @param spanNextQuery
     *            a SpanNextQuery
     * @param context
     * @param acceptDocs
     * @param termContexts
     * @throws IOException
     */
    public NextSpans (SpanNextQuery spanNextQuery, LeafReaderContext context,
                      Bits acceptDocs, Map<Term, TermContext> termContexts)
            throws IOException {
        super(spanNextQuery, context, acceptDocs, termContexts);
        collectPayloads = spanNextQuery.isCollectPayloads();
        hasMoreSpans = secondSpans.next();
        matchList = new ArrayList<>();
        candidateList = new ArrayList<>();
    }


    @Override
    public boolean next () throws IOException {        
        isStartEnumeration = false;
        matchPayload.clear();
        return advance();
    }


    /**
     * Advances the NextSpans to the next match by checking the
     * matchList or
     * setting the matchlist first, if it is empty.
     * 
     * @return <code>true</code> if a match is found,
     *         <code>false</code>
     *         otherwise.
     * @throws IOException
     */
    private boolean advance () throws IOException {
        while (hasMoreSpans || !matchList.isEmpty()
                || !candidateList.isEmpty()) {

            // Check, if the matchlist is fine
            if (!matchList.isEmpty() &&
                candidateListDocNum != firstSpans.doc()) {

                if (DEBUG) {
                    log.debug(
                        "Remove entries from matchlist because " +
                        "it's not in the same doc {}!={}",
                        firstSpans.doc(),
                        candidateListDocNum);
                };

                // Clear matchList
                matchList.clear();

                // Set new matchlist
                setMatchList();
            };

            if (!matchList.isEmpty()) {                
                matchDocNumber = firstSpans.doc();
                matchStartPosition = firstSpans.start();
                matchEndPosition = matchList.get(0).getEnd();
                
                spanId = matchList.get(0).getSpanId();
                if (collectPayloads)
                    matchPayload.addAll(matchList.get(0).getPayloads());
                matchList.remove(0);
                return true;
            }
            
            // Forward firstspan
            hasMoreFirstSpan = firstSpans.next();
            if (hasMoreFirstSpan){
                if (DEBUG) {
                    log.debug("FirstSpan [{}]", firstSpans.toString());
                    log.debug("FirstSpan ({}) {}-{}", firstSpans.doc(), firstSpans.start(), firstSpans.end());
                }
                setMatchList();
            }
            else {
                hasMoreSpans = false;
                candidateList.clear();
            }
        }
        return false;
    }


    /**
     * Sets the matchlist by first searching the candidates and then
     * find all
     * the matches.
     * 
     * @throws IOException
     */
    private void setMatchList () throws IOException {
        if (firstSpans.doc() == candidateListDocNum) {
            searchCandidates();
            searchMatches();
        }
        else {
            candidateList.clear();
            if (hasMoreSpans && ensureSameDoc(firstSpans, secondSpans)) {
                if (DEBUG) {
                    log.debug("First and second span now in same doc: {}-{} and {}-{} in {}={}",
                              firstSpans.start(),
                              firstSpans.end(),
                              secondSpans.start(),
                              secondSpans.end(),
                              firstSpans.doc(),
                              secondSpans.doc()
                        );
                }
                candidateListDocNum = firstSpans.doc();
                searchMatches();
            }
        }
    }


    /**
     * Removes all second span candidates whose start position is not
     * the same
     * as the firstspan's end position, otherwise creates a match and
     * add it to
     * the matchlist.
     * 
     * @throws IOException
     */
    private void searchCandidates () throws IOException {
        if (DEBUG) {
            log.debug("CandidateList: {}", candidateList.toString());
        };
        Iterator<CandidateSpan> i = candidateList.iterator();
        CandidateSpan cs;
        while (i.hasNext()) {
            cs = i.next();

            if (cs.getStart() == firstSpans.end()) {
                addMatch(cs);
            }
            else if (cs.getEnd() < firstSpans.end() 
                    && cs.getStart() <firstSpans.start()){
                i.remove();
            }
        }
    }


    /**
     * Finds all secondspans whose start position is the same as the
     * end
     * position of the firstspans, until the secondspans' start
     * position is
     * bigger than the firstspans' end position. Adds those
     * secondspans to the
     * candidateList and creates matches.
     * 
     * @throws IOException
     */
    private void searchMatches () throws IOException {

        while (hasMoreSpans && candidateListDocNum == secondSpans.doc()) {
            if (DEBUG) {
                log.debug("SecondSpan [{}]", secondSpans.toString());
                log.debug("SecondSpan ({}) {}-{}", secondSpans.doc(), secondSpans.start(), secondSpans.end());
            };
            if (secondSpans.start() > firstSpans.end()) {
                break;
            }
            if (secondSpans.start() == firstSpans.end()) {
                
                if (DEBUG) {
                    log.debug("Check adjacency at {}-{}|{}-{} in {}={}={}",
                              firstSpans.start(), firstSpans.end(),
                              secondSpans.start(), secondSpans.end(),
                              firstSpans.doc(), secondSpans.doc(), candidateListDocNum);
                };
                
                candidateList.add(new CandidateSpan(secondSpans));
                addMatch(new CandidateSpan(secondSpans));
            }
            hasMoreSpans = secondSpans.next();
        }
    }


    /**
     * Creates a match from the given CandidateSpan representing a
     * secondspan
     * state whose start position is identical to the end position of
     * the
     * current firstspan, and adds it to the matchlist.
     * 
     * @param cs
     *            a CandidateSpan
     * @throws IOException
     */
    private void addMatch (CandidateSpan cs) throws IOException {

        int start = firstSpans.start();
        long cost = firstSpans.cost() + cs.getCost();

        List<byte[]> payloads = new ArrayList<byte[]>();
        if (collectPayloads) {
            if (firstSpans.isPayloadAvailable())
                payloads.addAll(firstSpans.getPayload());
            if (cs.getPayloads() != null)
                payloads.addAll(cs.getPayloads());
        }

        matchList.add(new CandidateSpan(start, cs.getEnd(), candidateListDocNum,
                cost, payloads));
    }


    @Override
    public boolean skipTo (int target) throws IOException {
        if (hasMoreSpans && (firstSpans.doc() < target)) {

            if (!firstSpans.skipTo(target)) {
                hasMoreSpans = false;
                return false;
            };

            if (DEBUG) {
                log.debug("Skip firstSpans to {}={} succeed with positions {}-{}",
                          target,
                          firstSpans.doc(),
                          firstSpans.start(),
                          firstSpans.end());
                log.debug("secondSpans is at positions {}-{} at {}",
                          secondSpans.start(),
                          secondSpans.end(),
                          secondSpans.doc());
            };
            
            if (hasMoreFirstSpan){
                setMatchList();
            }
            else {
                hasMoreSpans = false;
                candidateList.clear();
            }
        }
        matchPayload.clear();
        return advance();
    }


    @Override
    public long cost () {
        return firstSpans.cost() + secondSpans.cost();
    }
};
