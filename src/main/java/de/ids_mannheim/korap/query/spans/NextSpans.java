package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
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
    // The first span of the document is not processed yet
    private boolean atFirstSpan;

    // The second span has more positions in the document
    private boolean hasMoreSecondSpan;

    /**
     * Constructs NextSpans for the given {@link SpanNextQuery}.
     * 
     * @param spanNextQuery
     *            a SpanNextQuery
     * @param context
     * @param acceptDocs
     * @param weights
     * @throws IOException
     */
    public NextSpans (SpanNextQuery spanNextQuery, LeafReaderContext context,
                      Bits acceptDocs, SpanWeights weights)
            throws IOException {
        super(spanNextQuery, context, acceptDocs, weights);
        collectPayloads = spanNextQuery.isCollectPayloads();
        matchList = new ArrayList<>();
        candidateList = new ArrayList<>();
    }




    @Override
    protected List<? extends KrillSpans> requiredOperands () {
        return Collections.singletonList(secondSpans);
    };


    @Override
    protected void startDocument () throws IOException {
        matchList.clear();
        candidateList.clear();
        candidateListDocNum = firstSpans.docID();
        hasMoreSecondSpan = nextInDocument(secondSpans);
        atFirstSpan = true;
    };


    @Override
    protected boolean nextMatchInDocument () throws IOException {
        isStartEnumeration = false;
        matchPayload.clear();

        while (true) {
            if (!matchList.isEmpty()) {
                matchDocNumber = firstSpans.docID();
                matchStartPosition = firstSpans.startPosition();
                matchEndPosition = matchList.get(0).getEnd();
                spanId = matchList.get(0).getSpanId();
                if (collectPayloads)
                    matchPayload.addAll(matchList.get(0).getPayloads());
                matchList.remove(0);
                return true;
            };

            // No more matches in this document
            if (!hasMoreSecondSpan && candidateList.isEmpty())
                return false;

            // Forward firstspan
            if (atFirstSpan)
                atFirstSpan = false;
            else if (!(hasMoreSpans = nextInDocument(firstSpans)))
                return false;

            if (DEBUG) {
                log.debug("FirstSpan [{}]", firstSpans.toString());
                log.debug("FirstSpan ({}) {}-{}", firstSpans.docID(),
                        firstSpans.startPosition(), firstSpans.endPosition());
            };

            searchCandidates();
            searchMatches();
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

            if (cs.getStart() == firstSpans.endPosition()) {
                addMatch(cs);
            }
            else if (cs.getEnd() < firstSpans.endPosition() 
                    && cs.getStart() <firstSpans.startPosition()){
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

        while (hasMoreSecondSpan) {
            if (DEBUG) {
                log.debug("SecondSpan [{}]", secondSpans.toString());
                log.debug("SecondSpan ({}) {}-{}", secondSpans.docID(), secondSpans.startPosition(), secondSpans.endPosition());
            };
            if (secondSpans.startPosition() > firstSpans.endPosition()) {
                break;
            }
            if (secondSpans.startPosition() == firstSpans.endPosition()) {
                
                if (DEBUG) {
                    log.debug("Check adjacency at {}-{}|{}-{} in {}={}={}",
                              firstSpans.startPosition(), firstSpans.endPosition(),
                              secondSpans.startPosition(), secondSpans.endPosition(),
                              firstSpans.docID(), secondSpans.docID(), candidateListDocNum);
                };
                
                candidateList.add(new CandidateSpan(secondSpans));
                addMatch(new CandidateSpan(secondSpans));
            }
            hasMoreSecondSpan = nextInDocument(secondSpans);
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

        int start = firstSpans.startPosition();
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
    public long cost () {
        return firstSpans.cost() + secondSpans.cost();
    }
};
