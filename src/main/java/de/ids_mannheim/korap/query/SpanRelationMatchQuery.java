package de.ids_mannheim.korap.query;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.constants.RelationDirection;
import de.ids_mannheim.korap.query.spans.FocusSpans;

/**
 * Matches the source and/or target of a SpanRelationQuery to specific
 * SpanQueries.
 * 
 * @author margaretha
 *
 */
public class SpanRelationMatchQuery extends SimpleSpanQuery {

    private SpanQuery operandQuery;
    private SpanQuery operand2Query;
    private SpanRelationQuery relationQuery;


    /**
     * Matches the left node of the given relation with the given
     * SpanQuery.
     * 
     * @param relation
     *            a SpanRelationQuery
     * @param spanQuery
     *            a SpanQuery
     * @param collectPayloads
     *            a boolean flag representing the value
     *            <code>true</code> if payloads are to be collected,
     *            otherwise
     *            <code>false</code>.
     */
    public SpanRelationMatchQuery (SpanRelationQuery relation,
                                   SpanQuery spanQuery,
                                   boolean collectPayloads) {

        checkArguments(relation, spanQuery);
        SpanFocusQuery sq = new SpanFocusQuery(
                new SpanSegmentQuery(relationQuery, operandQuery, true),
                relation.getTempClassNumbers());
        sq.setMatchTemporaryClass(true);
        sq.setRemoveTemporaryClasses(true);
        sq.setSorted(false); // which operand to focus might be
                            // different from that to match

        this.setFirstClause(sq);
        this.collectPayloads = collectPayloads;
    }


    /**
     * Matches both the source and target of the given relations with
     * the given operands.
     * 
     * @param relation
     *            a SpanRelationQuery
     * @param source
     *            a SpanQuery
     * @param target
     *            a SpanQuery
     * @param collectPayloads
     *            a boolean flag representing the value
     *            <code>true</code> if payloads are to be collected,
     *            otherwise
     *            <code>false</code>.
     */
    public SpanRelationMatchQuery (SpanRelationQuery relation, SpanQuery source,
                                   SpanQuery target, boolean collectPayloads) {

        checkArguments(relation, source, target);
        SpanFocusQuery sq = null;
        SpanFocusQuery sq2 = null;
        // match source and then target
        if (relationQuery.getDirection().equals(RelationDirection.RIGHT)) {
            sq = new SpanFocusQuery(
                    new SpanSegmentQuery(relationQuery, operandQuery, true),
                    relation.getTempTargetNum());
            sq.setSorted(false);
            sq.setMatchTemporaryClass(true);

            sq2 = new SpanFocusQuery(
                    new SpanSegmentQuery(operand2Query, sq, true),
                    relation.getTempClassNumbers());
        }
        // match target and then source
        else {
            sq = new SpanFocusQuery(
                    new SpanSegmentQuery(relationQuery, operandQuery, true),
                    relation.getTempSourceNum());
            sq.setMatchTemporaryClass(true);

            sq2 = new SpanFocusQuery(
                    new SpanSegmentQuery(sq, operand2Query, true),
                    relation.getTempClassNumbers());
        }

        sq2.setMatchTemporaryClass(true);
        sq2.setRemoveTemporaryClasses(true);
        sq2.setSorted(false);

        this.setFirstClause(sq2);
        this.collectPayloads = collectPayloads;

    }


    /**
     * Checks if the SpanRelationQuery and the SpanQuery are not null
     * and if the SpanQuery has the same field as the
     * SpanRelationQuery.
     * 
     * @param relation
     *            SpanRelationQery
     * @param spanQuery
     *            SpanQuery
     */
    public void checkArguments (SpanRelationQuery relation,
            SpanQuery spanQuery) {
        if (relation == null) {
            throw new IllegalArgumentException(
                    "The relation query cannot be null.");
        }
        if (spanQuery == null) {
            throw new IllegalArgumentException(
                    "The operand query cannot be null.");
        }
        this.field = relation.getField();
        if (!spanQuery.getField().equals(field)) {
            throw new IllegalArgumentException(
                    "Clauses must have the same field.");
        }
        this.relationQuery = relation;
        this.operandQuery = spanQuery;
    }


    /**
     * Checks if the SpanRelationQuery and the source and target
     * SpanQuery are not null and if the SpanQueries have the same
     * field as the SpanRelationQuery.
     * 
     * @param relation
     *            SpanRelationQery
     * @param source
     *            SpanQuery
     * @param target
     *            SpanQuery
     */
    public void checkArguments (SpanRelationQuery relation, SpanQuery source,
            SpanQuery target) {
        checkArguments(relation, source);
        if (target == null) {
            if (source == null) {
                throw new IllegalArgumentException(
                        "The target query cannot be null.");
            }
        }
        if (!target.getField().equals(field)) {
            throw new IllegalArgumentException(
                    "Clauses must have the same field.");
        }
        this.operand2Query = target;
    }


    @Override
    public SimpleSpanQuery clone () {
        if (operand2Query != null) {
            return new SpanRelationMatchQuery(
                    (SpanRelationQuery) relationQuery.clone(),
                    (SpanQuery) operandQuery.clone(),
                    (SpanQuery) operand2Query.clone(), collectPayloads);
        }

        return new SpanRelationMatchQuery(
                (SpanRelationQuery) relationQuery.clone(),
                (SpanQuery) operandQuery.clone(), collectPayloads);
    }


    @Override
    public Spans getSpans (LeafReaderContext context, Bits acceptDocs,
            Map<Term, TermContext> termContexts) throws IOException {

        return new FocusSpans((SpanFocusQuery) firstClause, context, acceptDocs,
                termContexts);
    }


    @Override
    public String toString (String field) {
        return getFirstClause().toString();
    }
}
