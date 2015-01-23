package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.SpanDistanceQuery;

/**
 * Span enumeration of element-based distance span matches. Each match consists
 * of two child spans. The element-distance between the child spans is the
 * difference between the element position numbers where the child spans are.
 * The element-distance unit can be a sentence or a paragraph. All other child
 * spans' occurrences which are not in a sentence or a paragraph (with respect
 * to the element distance type currently used), are ignored.
 * 
 * Note: elements cannot overlap with each other.
 * 
 * @author margaretha
 * */
public class ElementDistanceSpans extends OrderedDistanceSpans {

    private Spans elements;
    private boolean hasMoreElements;
    private int elementPosition;
    private int secondSpanPostion;

    /**
     * Constructs ElementDistanceSpans based on the given SpanDistanceQuery.
     * 
     * @param query a SpanDistanceQuery
     * @param context
     * @param acceptDocs
     * @param termContexts
     * @throws IOException
     */
    public ElementDistanceSpans(SpanDistanceQuery query,
            AtomicReaderContext context, Bits acceptDocs,
            Map<Term, TermContext> termContexts) throws IOException {
        super(query, context, acceptDocs, termContexts);

        elements = query.getElementQuery().getSpans(context, acceptDocs,
                termContexts);

        hasMoreElements = elements.next();
        hasMoreSpans = hasMoreFirstSpans && hasMoreElements;
        elementPosition = 0;
    }

    @Override
    protected boolean findMatch() throws IOException {
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
    protected void setCandidateList() throws IOException {
        if (candidateListDocNum == elements.doc()
                && candidateListDocNum == secondSpans.doc()) {
            candidateListIndex = -1;
            addNewCandidates();
        } else {
            candidateList.clear();
            if (hasMoreFirstSpans
                    && findSameDoc(firstSpans, secondSpans, elements)) {
                candidateListDocNum = firstSpans.doc();
                elementPosition = 0;
                candidateListIndex = -1;
                addNewCandidates();
            }
        }
    }

    /**
     * Add new possible (candidate) firstspans. Candidate firstspans must be in
     * an element and not too far from the secondspan.
     * 
     * @throws IOException
     */
    private void addNewCandidates() throws IOException {
        while (hasMoreFirstSpans && firstSpans.doc() == candidateListDocNum
                && firstSpans.start() < secondSpans.end()) {

            if (advanceElementTo(firstSpans)) {
                candidateList
                        .add(new CandidateSpan(firstSpans, elementPosition));
                filterCandidateList(elementPosition);
            }
            hasMoreFirstSpans = firstSpans.next();
        }
    }

    /**
     * Advance elements until encountering a span within the given document.
     * 
     * @return true iff an element containing the span, is found.
     */
    private boolean advanceElementTo(Spans span) throws IOException {
        while (hasMoreElements && elements.doc() == candidateListDocNum
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
     * Reduce the number of candidates by removing all candidates that are not
     * within the max distance from the given element position.
     * 
     * @param position an element position
     */
    private void filterCandidateList(int position) {

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
        // System.out.println("pos "+position+" " +candidateList.size());
    }

    @Override
    protected boolean isSecondSpanValid() throws IOException {
        if (advanceElementTo(secondSpans)) {
            secondSpanPostion = elementPosition;
            filterCandidateList(secondSpanPostion);
            return true;
        }
        // second span is not in an element
        return false;
    }

    @Override
    public long cost() {
        CandidateSpan candidateSpan = candidateList.get(candidateListIndex);
        return elements.cost() + candidateSpan.getCost() + secondSpans.cost();
    }
}
