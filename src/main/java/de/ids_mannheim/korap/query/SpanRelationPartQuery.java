package de.ids_mannheim.korap.query;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.ToStringUtils;

import de.ids_mannheim.korap.query.spans.RelationPartSpans;

/**
 * This query match a part of a relation (either left or right) to certain
 * elements or terms. If inversed, the start and end positions of the right part
 * of the relation are set as positions of the match.
 * 
 * @author margaretha
 * */
public class SpanRelationPartQuery extends SpanRelationQuery {

    private static String elementStr = "s"; // default element interval type

    private SpanElementQuery elementQuery;
    private boolean matchRight; // if false, match left
    private boolean inverseRelation; // if false, sort result by the left
    private int window;

    /**
     * Constructs a SpanRelationPartQuery based on the specified
     * {@link SpanRelationQuery} and {@link SpanWithIdQuery} within a sentence.
     * 
     * @param spanRelationQuery a SpanRelationQuery
     * @param secondClause a SpanWithIdQuery
     * @param matchRight <code>true</code> if the right side have to be matched
     *        with the specified SpanWithIdQuery, <code>false</code> otherwise.
     * @param inverseRelation <code>true</code> if the resulting
     *        {@link RelationPartSpans} is to be ordered by right side
     *        positions, <code>false</code> otherwise.
     * @param collectPayloads a boolean flag representing the value
     *        <code>true</code> if payloads are to be collected, otherwise
     *        <code>false</code>.
     */
    public SpanRelationPartQuery(SpanRelationQuery spanRelationQuery,
            SpanWithIdQuery secondClause, // match tokenWithIdQuery, ElementQuery, RelationQuery
            boolean matchRight, boolean inverseRelation, boolean collectPayloads) {
        this(spanRelationQuery, secondClause, elementStr, matchRight,
                inverseRelation, collectPayloads);
    }

    /**
     * Constructs a SpanRelationPartQuery based on the specified
     * {@link SpanRelationQuery} and {@link SpanWithIdQuery} within a custom
     * element type specified by the elementStr.
     * 
     * @param spanRelationQuery a SpanRelationQuery
     * @param secondClause a SpanWithIdQuery
     * @param elementStr a custom element interval type
     * @param matchRight <code>true</code> if the right side have to be matched
     *        with the specified SpanWithIdQuery, <code>false</code> otherwise.
     * @param inverseRelation <code>true</code> if the resulting
     *        {@link RelationPartSpans} is to be ordered by right side
     *        positions, <code>false</code> otherwise.
     * @param collectPayloads a boolean flag representing the value
     *        <code>true</code> if payloads are to be collected, otherwise
     *        <code>false</code>.
     */
    public SpanRelationPartQuery(SpanRelationQuery spanRelationQuery,
            SpanWithIdQuery secondClause, String elementStr,
            boolean matchRight, boolean inverseRelation, boolean collectPayloads) {
        super(spanRelationQuery, secondClause, collectPayloads);
        this.matchRight = matchRight;
        this.inverseRelation = inverseRelation;
        elementQuery = new SpanElementQuery(spanRelationQuery.getField(),
                elementStr);
    }

    /**
     * * Constructs a SpanRelationPartQuery based on the specified
     * {@link SpanRelationQuery} and {@link SpanWithIdQuery} within a custom
     * window length (i.e. number of terms / token positions). A window starts
     * at the same token position as a relation span, and ends at the start +
     * window length.
     * 
     * @param spanRelationQuery a SpanRelationQuery
     * @param secondClause a SpanWithIdQuery
     * @param window a window length
     * @param matchRight <code>true</code> if the right side have to be matched
     *        with the specified SpanWithIdQuery, <code>false</code> otherwise.
     * @param inverseRelation <code>true</code> if the resulting
     *        {@link RelationPartSpans} is to be ordered by right side
     *        positions, <code>false</code> otherwise.
     * @param collectPayloads a boolean flag representing the value
     *        <code>true</code> if payloads are to be collected, otherwise
     *        <code>false</code>.
     */
    public SpanRelationPartQuery(SpanRelationQuery spanRelationQuery,
            SpanWithIdQuery secondClause, int window, boolean matchRight,
            boolean inverseRelation, boolean collectPayloads) {
        super(spanRelationQuery, secondClause, collectPayloads);
        this.matchRight = matchRight;
        this.inverseRelation = inverseRelation;
        this.window = window;
    }

    @Override
    public Spans getSpans(AtomicReaderContext context, Bits acceptDocs,
            Map<Term, TermContext> termContexts) throws IOException {
        return new RelationPartSpans(this, context, acceptDocs, termContexts);
    }

    @Override
    public SimpleSpanQuery clone() {
        SpanRelationPartQuery sq = new SpanRelationPartQuery(
                (SpanRelationQuery) this.firstClause,
                (SpanWithIdQuery) this.secondClause,
                this.elementQuery.getElementStr(), this.matchRight,
                this.inverseRelation, this.collectPayloads);
        return sq;
    }

    @Override
    public String toString(String field) {
        StringBuilder sb = new StringBuilder();
        sb.append("spanRelationWithVariable(");
        sb.append(firstClause.toString(field));
        sb.append(",");
        sb.append(secondClause.toString(field));
        sb.append(",");
        sb.append(matchRight ? "matchRight, " : "matchLeft, ");
        sb.append(",");
        if (elementQuery != null) {
            sb.append("element:");
            sb.append(elementQuery.getElementStr());
        } else {
            sb.append("window:");
            sb.append(this.window);
        }
        sb.append(")");
        sb.append(ToStringUtils.boost(getBoost()));
        return sb.toString();
    }

    /**
     * Tells if the right side of the RelationSpans is to be match.
     * 
     * @return <code>true</code> if the right side of the RelationSpans is to be
     *         match, <code>false</code> otherwise.
     */
    public boolean isMatchRight() {
        return matchRight;
    }

    /**
     * Sets which part of the RelationSpans is to be match.
     * 
     * @param matchRight <code>true</code> if the right side of the
     *        RelationSpans is to be match, <code>false</code> otherwise.
     */
    public void setMatchRight(boolean matchRight) {
        this.matchRight = matchRight;
    }

    /**
     * Tells if start and end positions of the resulting span should be set from
     * the right part of the RelationSpans. Normally the start and end positions
     * of RelationSpans are those of the left part.
     * 
     * @return <code>true</code> if the resulting {@link RelationPartSpans} is
     *         to be ordered by right side positions, <code>false</code>
     *         otherwise.
     */
    public boolean isInverseRelation() {
        return inverseRelation;
    }

    /**
     * Sets if start and end positions of the resulting span should be set from
     * the right part of the RelationSpans. Normally the start and end positions
     * of RelationSpans are those of the left part.
     * 
     * @param inverseRelation <code>true</code> if the resulting
     *        {@link RelationPartSpans} is to be ordered by right side
     *        positions, <code>false</code> otherwise.
     */
    public void setInverseRelation(boolean inverseRelation) {
        this.inverseRelation = inverseRelation;
    }

    /**
     * Returns the SpanElementQuery of the custom element interval type.
     * 
     * @return the SpanElementQuery of the custom element interval type
     */
    public SpanElementQuery getElementQuery() {
        return elementQuery;
    }

    /**
     * Sets the SpanElementQuery of the custom element interval type.
     * 
     * @param intervalType the SpanElementQuery of the custom element interval
     *        type
     */
    public void setElementQuery(SpanElementQuery intervalType) {
        this.elementQuery = intervalType;
    }

    /**
     * Returns the custom window length.
     * 
     * @return the custom window length.
     */
    public int getWindow() {
        return window;
    }

    /**
     * Sets a custom window length.
     * 
     * @param window a custom window length
     */
    public void setWindow(int window) {
        this.window = window;
    }
}
