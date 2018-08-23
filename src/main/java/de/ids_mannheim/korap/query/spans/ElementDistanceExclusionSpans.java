package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.SpanDistanceQuery;

/**
 * Span enumeration of spans (firstSpans) which do <em>not</em> occur
 * together
 * with other spans (secondSpans) on the right side, within a range of
 * an
 * element-based distance (i.e. a sentence or a paragraph as the
 * distance unit).
 * If the query requires that the spans are ordered, then the
 * firstSpans must
 * occur before the secondSpans. In this class, firstSpans are also
 * referred to
 * as target spans and second spans as candidate spans.<br/>
 * <br/>
 * Note: The element distance unit does not overlap to each other.
 * 
 * @author margaretha
 */
public class ElementDistanceExclusionSpans extends DistanceSpans {

    private Spans elements;
    private boolean hasMoreElements;
    private int elementPosition;

    private boolean isOrdered;
    private boolean hasMoreSecondSpans;

    // other first spans occurred between the current target and the second
    // spans
    protected List<CandidateSpan> targetList;
    // secondSpans occurring near the firstSpans
    protected List<CandidateSpan> candidateList;
    private int currentDocNum;

    private int minDistance, maxDistance;
    private int firstSpanPostion;


    /**
     * Constructs ElementDistanceExclusionSpans from the specified
     * {@link SpanDistanceQuery}.
     * 
     * @param query
     *            a SpanDistanceQuery
     * @param context
     * @param acceptDocs
     * @param termContexts
     * @throws IOException
     */
    public ElementDistanceExclusionSpans (SpanDistanceQuery query,
                                          LeafReaderContext context,
                                          Bits acceptDocs,
                                          Map<Term, TermContext> termContexts)
            throws IOException {
        super(query, context, acceptDocs, termContexts);

        elements = query.getElementQuery().getSpans(context, acceptDocs,
                termContexts);
        hasMoreElements = elements.next();
        hasMoreSpans = firstSpans.next() && hasMoreElements;
        hasMoreSecondSpans = secondSpans.next();

        elementPosition = 0;
        this.isOrdered = query.isOrdered();
        candidateList = new ArrayList<CandidateSpan>();
        targetList = new ArrayList<CandidateSpan>();
        currentDocNum = firstSpans.doc();

        minDistance = query.getMinDistance();
        maxDistance = query.getMaxDistance();
    }


    @Override
    protected boolean advance () throws IOException {
        while (!targetList.isEmpty()
                || (hasMoreSpans && ensureSameDoc(firstSpans, elements))) {
            if (!targetList.isEmpty()) {
                if (isFirstTargetValid())
                    return true;
                else
                    continue;
            }
            if (findMatch())
                return true;
        }
        return false;
    }


    /**
     * Tells if the first target from the target list is a match.
     * 
     * @return <code>true</code> if the first target from the target
     *         list is a
     *         match, <code>false</code> otherwise.
     * @throws IOException
     */
    private boolean isFirstTargetValid () throws IOException {
        CandidateSpan target = targetList.get(0);
        targetList.remove(0);
        firstSpanPostion = target.getPosition();
        filterCandidateList(firstSpanPostion);
        collectRightCandidates();

        if (isWithinDistance()) {
            return false;
        }
        setMatchProperties(target);
        return true;
    }


    /**
     * Validate if the current firstSpan is a match.
     * 
     * @return <code>true</code> if a match is found,
     *         <code>false</code>
     *         otherwise.
     * @throws IOException
     */
    private boolean findMatch () throws IOException {
        if (firstSpans.doc() != currentDocNum) {
            currentDocNum = firstSpans.doc();
            candidateList.clear();
        }

        if (hasMoreSecondSpans) {
            if (secondSpans.doc() == firstSpans.doc()) {
                return (isFirstSpanValid() ? true : false);
            }
            else if (secondSpans.doc() < firstSpans.doc()) {
                hasMoreSecondSpans = secondSpans.skipTo(firstSpans.doc());
                return false;
            }
        }

        // return (isFirstSpanValid() ? true : false);

        if (candidateList.isEmpty()) {
            if (isFirstSpanInElement()) {
                setMatchProperties(new CandidateSpan(firstSpans,
                        elementPosition));
                hasMoreSpans = firstSpans.next();
                return true;
            }
            hasMoreSpans = firstSpans.next();
            return false;
        }
        return (isFirstSpanValid() ? true : false);
    }


    /**
     * Tells if the current firstSpan is a match.
     * 
     * @return <code>true</code> if a match is found,
     *         <code>false</code>
     *         otherwise.
     * @throws IOException
     *             <pre>
     *             private boolean isFirstSpanValid() throws
     *             IOException {
     *             if (candidateList.isEmpty()) {
     *             if (isFirstSpanInElement()) {
     *             setMatchProperties(new CandidateSpan(firstSpans,
     *             elementPosition));
     *             hasMoreSpans = firstSpans.next();
     *             return true;
     *             }
     *             hasMoreSpans = firstSpans.next();
     *             return false;
     *             }
     *             return (findMatch() ? true : false);
     *             }
     *             </pre>
     */

    /**
     * Tells if the given span is in an element distance unit, or not,
     * by
     * advancing the element distance unit to the span position.
     * 
     * @param span
     *            a span
     * @return <code>true</code> if the element distance unit can be
     *         advanced to
     *         contain the given span, <code>false</code> otherwise.
     * @throws IOException
     */
    private boolean advanceElementTo (Spans span) throws IOException {
        while (hasMoreElements && elements.doc() == currentDocNum
                && elements.start() < span.end()) {

            if (span.start() >= elements.start()
                    && span.end() <= elements.end()) {
                return true;
            }

            hasMoreElements = elements.next();
            elementPosition++;
        }
        return false;
    }


    /**
     * Tells if the current firstSpan is a match.
     * 
     * @return <code>true</code> if a match is found,
     *         <code>false</code>
     *         otherwise.
     * @throws IOException
     */
    private boolean isFirstSpanValid () throws IOException {
        if (!isOrdered)
            collectLeftCandidates();

        if (isFirstSpanInElement()) {
            CandidateSpan target = new CandidateSpan(firstSpans,
                    elementPosition);
            hasMoreSpans = firstSpans.next();
            // Checking if the secondspans in the *left* side are not within the
            // distance range
            if (!isOrdered && isWithinDistance())
                return false;
            // Checking if the secondspans in the *right* side are not within
            // the distance range
            collectRightCandidates();
            if (isWithinDistance())
                return false;

            setMatchProperties(target);
            return true;
        }
        hasMoreSpans = firstSpans.next();
        return false;
    }


    /**
     * Collects all second spans (candidates) on the right side of the
     * current
     * first span (target) position. At the same time, also collects
     * all other
     * first spans occurring before the second spans.
     * 
     * @throws IOException
     */
    private void collectRightCandidates () throws IOException {
        while (hasMoreSecondSpans && secondSpans.doc() == currentDocNum) {

            if (elementPosition > firstSpanPostion + maxDistance) {
                break;
            }
            // stores all first spans occurring before the current second span
            // in the target list.
            if (hasMoreSpans && firstSpans.start() < secondSpans.start()
                    && firstSpans.doc() == currentDocNum) {
                if (advanceElementTo(firstSpans)) {
                    targetList.add(new CandidateSpan(firstSpans,
                            elementPosition));
                }
                hasMoreSpans = firstSpans.next();
                continue;
            }
            // collects only second spans occurring inside an element
            if (advanceElementTo(secondSpans)) {
                candidateList.add(new CandidateSpan(secondSpans,
                        elementPosition));
            }
            hasMoreSecondSpans = secondSpans.next();
        }
    }


    /**
     * Collects all the second spans (candidates) occurring before the
     * first
     * spans, and are within an element distance unit.
     * 
     * @throws IOException
     */
    private void collectLeftCandidates () throws IOException {
        while (hasMoreSecondSpans && secondSpans.doc() == firstSpans.doc()
                && secondSpans.start() < firstSpans.end()) {
            if (advanceElementTo(secondSpans)) {
                candidateList.add(new CandidateSpan(secondSpans,
                        elementPosition));
                filterCandidateList(elementPosition);
            }
            hasMoreSecondSpans = secondSpans.next();
        }
    }


    /**
     * Tells if there is a candidate span (second span) occurring
     * together with
     * the target span (firstspan) within the minimum and maximum
     * distance
     * range.
     * 
     * @return <code>true</code> if there is a candidate span (second
     *         span)
     *         occurring together with the target span (firstspan)
     *         within the
     *         minimum and maximum distance range, <code>false</code>
     *         otherwise.
     */
    private boolean isWithinDistance () {
        int actualDistance;
        for (CandidateSpan cs : candidateList) {
            actualDistance = cs.getPosition() - firstSpanPostion;
            if (!isOrdered)
                actualDistance = Math.abs(actualDistance);

            if (minDistance <= actualDistance && actualDistance <= maxDistance)
                return true;
        }
        return false;
    }


    /**
     * Tells if the current firstSpans is in an element.
     * 
     * @return <code>true</code> if the current firstSpans in is an
     *         element,
     *         <code>false</code> otherwise.
     * @throws IOException
     */
    private boolean isFirstSpanInElement () throws IOException {
        if (advanceElementTo(firstSpans)) {
            firstSpanPostion = elementPosition;
            filterCandidateList(firstSpanPostion);
            return true;
        }
        return false;
    }


    /**
     * From the candidateList, removes all candidate spans that are
     * too far from
     * the given target position, and have exactly the same position
     * as the
     * target position. Only candidate spans occurring within a range
     * of
     * distance from the target position, are retained.
     * 
     * @param position
     *            target/firstSpan position
     */
    private void filterCandidateList (int position) {

        Iterator<CandidateSpan> i = candidateList.iterator();
        CandidateSpan cs;
        while (i.hasNext()) {
            cs = i.next();
            if (cs.getPosition() == position
                    || cs.getPosition() + maxDistance >= position) {
                break;
            }
            i.remove();
        }
    }


    /**
     * Sets the given target/match CandidateSpan as the current match.
     * 
     * @param match
     *            a target/firstSpan wrapped as a CandidateSpan
     * @throws IOException
     */
    private void setMatchProperties (CandidateSpan match) throws IOException {
        matchDocNumber = match.getDoc();
        matchStartPosition = match.getStart();
        matchEndPosition = match.getEnd();

        if (collectPayloads && match.getPayloads() != null)
            matchPayload.addAll(match.getPayloads());

        setMatchFirstSpan(match);
    }


    @Override
    public boolean skipTo (int target) throws IOException {
        if (hasMoreSpans && firstSpans.doc() < target) {
            if (!firstSpans.skipTo(target)) {
                hasMoreSpans = false;
                return false;
            }
        }
        return advance();
    }


    @Override
    public long cost () {
        return elements.cost() + firstSpans.cost() + secondSpans.cost();
    }

}
