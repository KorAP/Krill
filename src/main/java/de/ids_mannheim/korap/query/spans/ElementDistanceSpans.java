package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.spans.Spans;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.SpanDistanceQuery;

/**
 * Span enumeration of element-based distance span matches. Each match
 * consists of two child spans. The element-distance between the child
 * spans is the difference between the element position numbers where
 * the child spans are. The element-distance unit can be a sentence or
 * a paragraph. All other child spans' occurrences which are not in a
 * sentence or a paragraph (with respect to the element distance type
 * currently used), are ignored.
 * 
 * Note: elements cannot overlap with each other.
 * 
 * @author margaretha
 */
public class ElementDistanceSpans extends OrderedDistanceSpans {

    private KrillSpans elements;
    private boolean hasMoreElements;
    private int elementPosition;
    private int secondSpanPostion;


    /**
     * Constructs ElementDistanceSpans based on the given
     * SpanDistanceQuery.
     * 
     * @param query
     *            a SpanDistanceQuery
     * @param context
     * @param acceptDocs
     * @param weights
     * @throws IOException
     */
    public ElementDistanceSpans (SpanDistanceQuery query,
                                 LeafReaderContext context, Bits acceptDocs,
                                 SpanWeights weights)
            throws IOException {
        super(query, context, acceptDocs, weights);

        elements = weights.getSpans(query.getElementQuery(), context, acceptDocs);

        elementPosition = 0;
    }


    @Override
    protected List<? extends KrillSpans> requiredOperands () {
        return Arrays.asList(firstSpans, elements);
    }


    @Override
    protected void startDocument () throws IOException {
        super.startDocument();
        hasMoreElements = nextInDocument(elements);
        elementPosition = 0;
    }


    @Override
    protected boolean findMatch () throws IOException {
        CandidateSpan candidateSpan = candidateList.get(candidateListIndex);
        int actualDistance = secondSpanPostion - candidateSpan.getPosition();

        // In the same element
        if (minDistance == 0 && actualDistance == 0) {
            setMatchProperties(candidateSpan, true);
            return true;
        }

        if (minDistance <= actualDistance && actualDistance <= maxDistance) {
            setMatchProperties(candidateSpan, false);
            return true;
        }

        return false;
    }


    @Override
    protected void setCandidateList () throws IOException {
        if (hasMoreElements
                && (hasMoreFirstSpans || !candidateList.isEmpty())) {
            candidateListIndex = -1;
            addNewCandidates();
        }
        else {
            candidateList.clear();
        }
    }

    /**
     * Add new possible (candidate) firstspans. Candidate firstspans
     * must be in an element and not too far from the secondspan.
     * 
     * @throws IOException
     */
    private void addNewCandidates () throws IOException {
        while (hasMoreFirstSpans
                && firstSpans.startPosition() < secondSpans.endPosition()) {
            if (advanceElementTo(firstSpans)) {
                candidateList
                        .add(new CandidateSpan(firstSpans, elementPosition));
                filterCandidateList(elementPosition);
            }
            hasMoreFirstSpans = nextInDocument(firstSpans);
        }
    }


    /**
     * Advance elements in the current document until encountering
     * the given span.
     * 
     * @return true iff an element containing the span, is found.
     */
    private boolean advanceElementTo (KrillSpans span) throws IOException {
        while (hasMoreElements
                && elements.startPosition() < span.endPosition()) {

            if (span.startPosition() >= elements.startPosition()
                    && span.endPosition() <= elements.endPosition()) {
                return true;
            }

            hasMoreElements = nextInDocument(elements);
            elementPosition++;
        }
        return false;
    }


    /**
     * Reduce the number of candidates by removing all candidates that
     * are not
     * within the max distance from the given element position.
     * 
     * @param position
     *            an element position
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

    @Override
    protected boolean isSecondSpanValid () throws IOException {
        if (advanceElementTo(secondSpans)) {
            secondSpanPostion = elementPosition;
            filterCandidateList(secondSpanPostion);
            return true;
        }
        // second span is not in an element
        return false;
    }

    @Override
    public long cost () {
        // The index is beyond the list once all candidates of the
        // current second span were checked
        if (candidateListIndex >= 0
                && candidateListIndex < candidateList.size()) {
            CandidateSpan candidateSpan = candidateList.get(candidateListIndex);
            return elements.cost() + candidateSpan.getCost() + secondSpans.cost();
        }
        else{
            return elements.cost() + secondSpans.cost();
        }
    }
}
