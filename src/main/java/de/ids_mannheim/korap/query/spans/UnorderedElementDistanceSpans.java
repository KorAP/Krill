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
 * Enumeration of span matches, whose two child spans have a specific
 * range of
 * distance (within a min and a max distance) and can be in any order.
 * The unit
 * distance is an element, which can be a sentence or a paragraph for
 * instance.
 * The distance is the difference between the positions of elements
 * containing
 * the spans.
 * 
 * @author margaretha
 * */
public class UnorderedElementDistanceSpans extends UnorderedDistanceSpans {

    private Spans elements;
    private boolean hasMoreElements;
    private int elementPosition;

    // contains all previous elements whose position is greater than the last
    // target span
    private List<CandidateSpan> elementList;


    /**
     * Constructs UnorderedElementDistanceSpans for the given
     * {@link SpanDistanceQuery}.
     * 
     * @param query
     *            a SpanDistanceQuery
     * @param context
     * @param acceptDocs
     * @param termContexts
     * @throws IOException
     */
    public UnorderedElementDistanceSpans (SpanDistanceQuery query,
                                          LeafReaderContext context,
                                          Bits acceptDocs,
                                          Map<Term, TermContext> termContexts)
            throws IOException {
        super(query, context, acceptDocs, termContexts);
        elements = query.getElementQuery().getSpans(context, acceptDocs,
                termContexts);
        hasMoreElements = elements.next();
        elementPosition = 0;
        elementList = new ArrayList<CandidateSpan>();
    }


    @Override
    protected boolean prepareLists () throws IOException {

        if (firstSpanList.isEmpty() && secondSpanList.isEmpty()) {
            if (hasMoreFirstSpans && hasMoreSecondSpans && hasMoreElements
                    && findSameDoc(firstSpans, secondSpans, elements)) {

                if (currentDocNum != firstSpans.doc()) {
                    currentDocNum = firstSpans.doc();
                    elementList.clear();
                }

                hasMoreFirstSpans = addSpan(firstSpans, firstSpanList,
                        hasMoreFirstSpans);
                hasMoreSecondSpans = addSpan(secondSpans, secondSpanList,
                        hasMoreSecondSpans);
            }
            else {
                hasMoreSpans = false;
                return false;
            }
        }
        else if (firstSpanList.isEmpty() && hasMoreFirstSpans
                && firstSpans.doc() == currentDocNum) {
            hasMoreFirstSpans = addSpan(firstSpans, firstSpanList,
                    hasMoreFirstSpans);
        }
        else if (secondSpanList.isEmpty() && hasMoreSecondSpans
                && secondSpans.doc() == currentDocNum) {
            hasMoreSecondSpans = addSpan(secondSpans, secondSpanList,
                    hasMoreSecondSpans);
        }

        return true;
    }


    /**
     * Adds all the spans occurring in the current document, as
     * CandidateSpans
     * to the specified candidate list, and tells if the enumeration
     * of the
     * spans has finished, or not.
     * 
     * @param span
     *            a Span
     * @param list
     *            a candidateList
     * @param hasMoreSpan
     *            a boolean describing if the span enumeration has
     *            finished or not.
     * @return <code>true</code> if the the span enumeration has
     *         finished,
     *         <code>false</code> otherwise.
     * @throws IOException
     */
    private boolean addSpan (Spans span, List<CandidateSpan> list,
            boolean hasMoreSpan) throws IOException {
        int position;
        while (hasMoreSpan && span.doc() == currentDocNum) {
            position = findElementPosition(span);
            if (position != -1) {
                list.add(new CandidateSpan(span, position));
                hasMoreSpan = span.next();
                return hasMoreSpan;
            }
            hasMoreSpan = span.next();
        }
        return hasMoreSpan;
    }


    /**
     * Finds the element position of the specified span in the element
     * list or
     * by advancing the element spans until encountering the span.
     * 
     * @param span
     *            a Span
     * @return the element position
     * @throws IOException
     */
    private int findElementPosition (Spans span) throws IOException {
        // Check in the element list
        if (!elementList.isEmpty()
                && span.end() <= elementList.get(elementList.size() - 1)
                        .getEnd()) {

            for (CandidateSpan e : elementList)
                if (e.getEnd() >= span.end() && e.getStart() <= span.start()) {
                    return e.getPosition();
                }
            return -1; // The span is not in an element.
        }

        return (advanceElementTo(span) ? elementPosition : -1);
    }


    /**
     * Advances the element spans until encountering the given span.
     * 
     * @param span
     * @return <code>true</code> if such an element is found,
     *         <code>false</code>
     *         if the span is not in an element.
     * @throws IOException
     */
    private boolean advanceElementTo (Spans span) throws IOException {
        while (hasMoreElements && elements.doc() == currentDocNum
                && elements.start() < span.end()) {

            if (span.start() >= elements.start()
                    && span.end() <= elements.end()) {
                return true;
            }
            elementList.add(new CandidateSpan(elements, elementPosition));
            hasMoreElements = elements.next();
            elementPosition++;
        }

        return false; // invalid
    }


    @Override
    protected boolean setCandidateList (List<CandidateSpan> candidateList,
            Spans candidate, boolean hasMoreCandidates,
            List<CandidateSpan> targetList) throws IOException {

        if (!targetList.isEmpty()) {
            CandidateSpan cs;
            CandidateSpan target = targetList.get(0);
            int position;
            while (hasMoreCandidates && candidate.doc() == target.getDoc()) {
                position = findElementPosition(candidate);
                if (position != -1) {
                    cs = new CandidateSpan(candidate, position);

                    if (isWithinMaxDistance(target, cs)) {
                        candidateList.add(cs);
                    }
                    else
                        break;
                }
                hasMoreCandidates = candidate.next();
            }
        }
        return hasMoreCandidates;
    }


    /**
     * Tells if the target and candidate spans are not too far from
     * each other
     * (within the maximum distance).
     * 
     * @return <code>true</code> if the target and candidate spans are
     *         within
     *         the maximum distance, <code>false</code> otherwise.
     * */
    protected boolean isWithinMaxDistance (CandidateSpan target,
            CandidateSpan candidate) {
        int candidatePos = candidate.getPosition();
        int targetPos = target.getPosition();

        // left candidate
        if (candidatePos < targetPos && candidatePos + maxDistance < targetPos) {
            return false;
        }
        // right candidate
        if (candidatePos > targetPos && targetPos + maxDistance < candidatePos) {
            return false;
        }
        return true;
    }


    @Override
    protected List<CandidateSpan> findMatches (CandidateSpan target,
            List<CandidateSpan> candidateList) {

        List<CandidateSpan> matches = new ArrayList<>();

        int actualDistance;
        int targetPos = target.getPosition();

        for (CandidateSpan cs : candidateList) {
            actualDistance = Math.abs(targetPos - cs.getPosition());

            if (minDistance == 0 && actualDistance == 0) {
                matches.add(createMatchCandidate(target, cs, true));
                continue;
            }

            if (minDistance <= actualDistance && actualDistance <= maxDistance)
                matches.add(createMatchCandidate(target, cs, false));
        }
        return matches;
    }


    @Override
    protected void updateList (List<CandidateSpan> candidateList) {
        updateElementList(candidateList.get(0).getPosition());
        candidateList.remove(0);
    }


    /**
     * Reduces the number of elements kept in the element list by
     * removing the
     * elements whose position is smaller than or identical to the
     * position of
     * the last target span.
     * 
     * @param position
     *            the last target span position
     */
    private void updateElementList (int position) {
        Iterator<CandidateSpan> i = elementList.iterator();
        CandidateSpan e;
        while (i.hasNext()) {
            e = i.next();
            if (e.getPosition() <= position) {
                i.remove();
            }
            break;
        }
    }
}
