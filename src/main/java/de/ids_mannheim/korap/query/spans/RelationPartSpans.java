package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.SpanRelationPartQuery;

/**
 * This span enumeration returns the right part of relation spans whose left
 * part token/element positions matching the second spans, or vice versa.
 * 
 * All relations within a certain window, e.g element-based or token-
 * distance-based, are sorted to resolve reference within that window.
 * Resolution is limited only within a window.
 * 
 * @author margaretha
 * */
public class RelationPartSpans extends RelationBaseSpans {

    private RelationBaseSpans relationSpans;
    private SpansWithId matcheeSpans;
    private ElementSpans element; // element as the window
    private List<CandidateRelationSpan> candidateRelations;

    private boolean matchRight;
    private boolean inverse;
    private boolean hasMoreMatchees;

    private int window; // number of tokens as the window

    /**
     * Constructs RelationPartSpans from the specified
     * {@link SpanRelationPartQuery}.
     * 
     * @param query a SpanRelationPartQuery
     * @param context
     * @param acceptDocs
     * @param termContexts
     * @throws IOException
     */
    public RelationPartSpans(SpanRelationPartQuery query,
            AtomicReaderContext context, Bits acceptDocs,
            Map<Term, TermContext> termContexts) throws IOException {
        super(query, context, acceptDocs, termContexts);
        if (query.getElementQuery() != null) {
            element = (ElementSpans) query.getElementQuery().getSpans(context,
                    acceptDocs, termContexts);
        } else {
            window = query.getWindow();
        }
        relationSpans = (RelationBaseSpans) firstSpans;
        matcheeSpans = (SpansWithId) secondSpans;
        // hack
        matcheeSpans.hasSpanId = true;

        hasMoreMatchees = matcheeSpans.next();
        hasMoreSpans = relationSpans.next() && hasMoreMatchees;
        if (element != null) {
            hasMoreSpans &= element.next();
        }
        candidateRelations = new ArrayList<CandidateRelationSpan>();
        matchRight = query.isMatchRight();
        inverse = query.isInverseRelation();
    }

    @Override
    public boolean next() throws IOException {
        isStartEnumeration = false;
        matchPayload.clear();
        return advance();
    }

    /**
     * Advances to the next match, by setting the first candidate relation from
     * candidateRelations list, if it is not empty. Otherwise, set the candidate
     * list first based on element or token window.
     * 
     * @return
     * @throws IOException
     */
    protected boolean advance() throws IOException {
        while (candidateRelations.size() > 0 || hasMoreSpans) {
            if (candidateRelations.size() > 0) {
                setMatchSpan(candidateRelations.get(0));
                candidateRelations.remove(0);
                return true;
            } else if (element != null) {
                setCandidateList();
            } else {
                setCandidateListWithWindow();
            }
        }
        return false;
    }

    /**
     * Sets the specified {@link CandidateRelationSpan} as the current match. If
     * the match should be sorted by the right side positions of the original
     * relation, then it should be inverted. In this case, the start and end
     * positions of the original <em>right</em> side, will be set as the match
     * <em>left</em> start and end positions, and vice versa.
     * 
     * @param relationSpan a CandidateRelationSpan
     */
    private void setMatchSpan(CandidateRelationSpan relationSpan) {
        matchDocNumber = relationSpan.getDoc();
        if (!inverse) {
            matchStartPosition = relationSpan.getStart();
            matchEndPosition = relationSpan.getEnd();
            setRightStart(relationSpan.getRightStart());
            setRightEnd(relationSpan.getRightEnd());
        } else { // maybe a bit confusing -- inverse relation
            matchStartPosition = relationSpan.getRightStart();
            matchEndPosition = relationSpan.getRightEnd();
            setRightStart(relationSpan.getStart());
            setRightEnd(relationSpan.getEnd());
        }

        setLeftId(relationSpan.getLeftId());
        setRightId(relationSpan.getRightId());
        setSpanId(relationSpan.getSpanId());
    }

    /**
     * Sets the candidate relation list based on token window that starts at the
     * same token position as a relation span, and ends at the start + window
     * length.
     * 
     * @throws IOException
     */
    private void setCandidateListWithWindow() throws IOException {
        if (hasMoreSpans && ensureSameDoc(relationSpans, matcheeSpans)) {
            int windowEnd = relationSpans.start() + window;
            if (relationSpans.end() > windowEnd) {
                throw new IllegalArgumentException("The window length "
                        + window + " is too small. The relation span ("
                        + relationSpans.start() + "," + relationSpans.end()
                        + ") is longer than " + "the window " + "length.");
            } else {
                collectRelations(relationSpans.doc(), windowEnd);
                // sort results
                Collections.sort(candidateRelations);
            }
        }
    }

    /**
     * Sets the candidate relation list based on the element window.
     * 
     * @throws IOException
     */
    private void setCandidateList() throws IOException {
        while (hasMoreSpans
                && findSameDoc(element, relationSpans, matcheeSpans)) {
            // if the relation is within a sentence
            if (relationSpans.start() >= element.start()
                    && relationSpans.end() <= element.end()) {
                collectRelations(element.doc(), element.end());
                // sort results
                Collections.sort(candidateRelations);
            } else if (relationSpans.end() < element.end()) {
                hasMoreSpans = relationSpans.next();
            } else {
                hasMoreSpans = element.next();
            }
        }
    }

    /**
     * Collects all relations whose end position is before or identical to the
     * given window end, within the specified document number, and match either
     * the left or right side of the relation to the matcheeSpans.
     * 
     * @param currentDoc the current document number
     * @param windowEnd the end position of the current window
     * @throws IOException
     */
    private void collectRelations(int currentDoc, int windowEnd)
            throws IOException {
        List<CandidateRelationSpan> temp = new ArrayList<CandidateRelationSpan>();
        boolean sortRight = false;
        if (matchRight)
            sortRight = true;
        // collect all relations within an element	
        while (hasMoreSpans && relationSpans.doc() == currentDoc
                && relationSpans.end() <= windowEnd) {
            temp.add(new CandidateRelationSpan(relationSpans, sortRight));
            hasMoreSpans = relationSpans.next();
        }

        if (matchRight)
            Collections.sort(temp);

        // do the matching for each relation
        int i = 0;
        CandidateRelationSpan r;
        while (hasMoreMatchees && i < temp.size()) {
            r = temp.get(i);
            if (matchRight) {
                /*
                 * System.out.println(r.getStart()+","+r.getEnd()+" "+
                 * r.getRightStart()+","+r.getRightEnd()+ " #"+r.getRightId()+
                 * " "+matcheeSpans.start()+","+matcheeSpans.end()+
                 * " #"+matcheeSpans.getSpanId() );
                 */
                i = matchRelation(i, r, r.getRightStart(), r.getRightEnd());
            } else {
                /*
                 * System.out.println(r.getStart()+","+r.getEnd()+" "+
                 * r.getRightStart()+","+r.getRightEnd()+" "
                 * +matcheeSpans.start()+","+matcheeSpans.end()+
                 * " #"+matcheeSpans.getSpanId());
                 */
                i = matchRelation(i, r, r.getStart(), r.getEnd());
            }
        }

        hasMoreSpans &= hasMoreMatchees;
    }

    /**
     * Matches the relation part from the given candidate relation, and start
     * and end positions to the matcheeSpans.
     * 
     * @param i the position counter for iterating the collected relations
     * @param r a CandidateRelationSpan
     * @param startPos the start position of the relation part to match
     * @param endPos the end position of the relation part to match
     * @return the next position counter to compute
     * @throws IOException
     */
    private int matchRelation(int i, CandidateRelationSpan r, int startPos,
            int endPos) throws IOException {

        if (startPos == matcheeSpans.start()) {
            if (endPos == matcheeSpans.end()) {

                int id;
                if (matcheeSpans instanceof RelationPartSpans) {
                    if (matchRight) {
                        id = ((RelationPartSpans) matcheeSpans).getRightId();
                    } else {
                        id = ((RelationPartSpans) matcheeSpans).getLeftId();
                    }
                } else {
                    id = matcheeSpans.getSpanId();
                }

                if (!inverse && r.getRightId() == id) {
                    r.sortRight = false;
                    candidateRelations.add(r);
                } else if (inverse && r.getLeftId() == id) {
                    r.sortRight = true;
                    candidateRelations.add(r);
                }
                i++;
            } else if (endPos <= matcheeSpans.end()) {
                i++;
            } else {
                hasMoreMatchees = matcheeSpans.next();
            }
        } else if (startPos < matcheeSpans.start()) {
            i++;
        } else {
            hasMoreMatchees = matcheeSpans.next();
        }
        return i;
    }

    @Override
    public boolean skipTo(int target) throws IOException {
        if (hasMoreSpans && (relationSpans.doc() < target)) {
            if (!relationSpans.skipTo(target)) {
                candidateRelations.clear();
                return false;
            }
        }
        setCandidateList();
        matchPayload.clear();
        isStartEnumeration = false;
        return advance();
    }

    @Override
    public long cost() {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * CandidateRelationSpan stores a state of RelationSpans and enables sorting
     * a relation list by the right side positions of the relations. Normally,
     * such a list are sorted by left side positions of the relations.
     * 
     */
    class CandidateRelationSpan extends CandidateSpan implements
            Comparable<CandidateSpan> {

        private int rightStart, rightEnd;
        private short leftId, rightId;
        private boolean sortRight;

        public CandidateRelationSpan(RelationBaseSpans span, boolean sortRight)
                throws IOException {
            super(span);
            this.rightStart = span.getRightStart();
            this.rightEnd = span.getRightEnd();
            this.sortRight = sortRight;
            this.leftId = span.getLeftId();
            this.rightId = span.getRightId();
            this.spanId = span.getSpanId();
        }

        @Override
        public int compareTo(CandidateSpan o) {
            CandidateRelationSpan cs = (CandidateRelationSpan) o;
            if (sortRight)
                return sortByRight(cs);

            return super.compareTo(o);
        }

        /**
         * Determines the position of this CandidateRelationSpan relative to the
         * given CandidateRelationSpan.
         * 
         * @param cs a CandidateRelationSpan
         * @return 0 if this CandidateRelationSpan has identical position as cs,
         *         1 if it should follows cs, and -1 if it should preceeds cs.
         */
        private int sortByRight(CandidateRelationSpan cs) {
            if (this.getRightStart() == cs.getRightStart()) {
                if (this.getRightEnd() == cs.getRightEnd())
                    return 0;
                if (this.getRightEnd() > cs.getRightEnd())
                    return 1;
                else
                    return -1;
            } else if (this.getRightStart() < cs.getRightStart())
                return -1;
            else
                return 1;
        }

        public int getRightStart() {
            return rightStart;
        }

        public void setRightStart(int rightStart) {
            this.rightStart = rightStart;
        }

        public int getRightEnd() {
            return rightEnd;
        }

        public void setRightEnd(int rightEnd) {
            this.rightEnd = rightEnd;
        }

        public short getLeftId() {
            return leftId;
        }

        public void setLeftId(short leftId) {
            this.leftId = leftId;
        }

        public short getRightId() {
            return rightId;
        }

        public void setRightId(short rightId) {
            this.rightId = rightId;
        }
    }
}
