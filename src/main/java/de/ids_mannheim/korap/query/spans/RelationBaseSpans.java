package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.SpanElementQuery;
import de.ids_mannheim.korap.query.SpanRelationQuery;
import de.ids_mannheim.korap.query.SpanWithIdQuery;

/**
 * RelationBaseSpans is a base class for relation spans containing properties
 * about the start and end positions of right side of the relation. It can also
 * store information about the id of the left/right side, for instance, when it
 * is an element or another relation.
 * 
 * @author margaretha
 * 
 */
public abstract class RelationBaseSpans extends SpansWithId {

    protected short leftId, rightId;
	protected int leftStart, leftEnd;
    protected int rightStart, rightEnd;

    /**
     * Constructs RelationBaseSpans based on the given SpanWithIdQuery.
     * 
     * @param spanWithIdQuery a SpanWithIdQuery, for instance a
     *        {@link SpanElementQuery} or {@link SpanRelationQuery}.
     * @param context
     * @param acceptDocs
     * @param termContexts
     * @throws IOException
     */
    public RelationBaseSpans(SpanWithIdQuery spanWithIdQuery,
            AtomicReaderContext context, Bits acceptDocs,
            Map<Term, TermContext> termContexts) throws IOException {
        super(spanWithIdQuery, context, acceptDocs, termContexts);
    }

    /**
     * Returns the id of the left hand side of the relation.
     * 
     * @return an id
     */
    public short getLeftId() {
        return leftId;
    }

    /**
     * Sets the id of the left hand side of the relation.
     * 
     * @param leftId the id of the left hand side of the relation.
     */
    public void setLeftId(short leftId) {
        this.leftId = leftId;
    }

    /**
     * Returns the id of the right hand side of the relation.
     * 
     * @return an id
     */
    public short getRightId() {
        return rightId;
    }

    /**
     * Sets the id of the right hand side of the relation.
     * 
     * @param rightId the id of the right hand side of the relation.
     */
    public void setRightId(short rightId) {
        this.rightId = rightId;
    }

    /**
     * Returns the start position of the right hand side of the relation.
     * 
     * @return the start position
     */
    public int getRightStart() {
        return rightStart;
    }

    /**
     * Sets the start position of the right hand side of the relation.
     * 
     * @param rightStart the start position of the right hand side of the
     *        relation.
     */
    public void setRightStart(int rightStart) {
        this.rightStart = rightStart;
    }

    /**
     * Returns the end position of the right hand side of the relation.
     * 
     * @return the end position
     */
    public int getRightEnd() {
        return rightEnd;
    }

    /**
     * Sets the start position of the right hand side of the relation.
     * 
     * @param rightEnd the end position of the right hand side of the relation.
     */
    public void setRightEnd(int rightEnd) {
        this.rightEnd = rightEnd;
    }
}
