package de.ids_mannheim.korap.query;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.spans.FocusSpans;

public class SpanRelationMatchQuery extends SimpleSpanQuery{

    private SpanQuery operandQuery;
    private SpanQuery operand2Query;
    private SpanRelationQuery relationQuery;

    public SpanRelationMatchQuery (SpanRelationQuery relation, SpanQuery operand,
            boolean collectPayloads) {

        checkVariables(relation, operand);
        SpanFocusQuery sq = new SpanFocusQuery(new SpanSegmentQuery(
                relationQuery, operandQuery, true),
                relation.getTempClassNumbers());
        sq.setMatchTemporaryClass(true);
        sq.setRemoveTemporaryClasses(true);
        sq.setSorted(false); // which operand to focus might be
                             // different from that to match

        this.setFirstClause(sq);
        this.collectPayloads = collectPayloads;
    }

    public SpanRelationMatchQuery (SpanRelationQuery relation, SpanQuery source,
            SpanQuery target, boolean collectPayloads) {

        checkVariables(relation, source, target);
        SpanFocusQuery sq = null;
        // match source
        if (relationQuery.getDirection() == 0) {
            sq = new SpanFocusQuery(new SpanSegmentQuery(relationQuery,
                    operandQuery, true), relation.getTempTargetNum());
        }
        // match target
        else {
            sq = new SpanFocusQuery(new SpanSegmentQuery(relationQuery,
                    operandQuery, true), relation.getTempSourceNum());
        }
        sq.setSorted(false);
        sq.setMatchTemporaryClass(true);

        SpanFocusQuery sq2 = new SpanFocusQuery(new SpanSegmentQuery(sq,
                operand2Query, true), relation.getTempClassNumbers());
        sq2.setMatchTemporaryClass(true);
        sq2.setRemoveTemporaryClasses(true);
        sq2.setSorted(false);

        this.setFirstClause(sq2);
        this.collectPayloads = collectPayloads;

    }

    public void checkVariables(SpanRelationQuery relation, SpanQuery operand) {
        if (relation == null) {
            throw new IllegalArgumentException(
                    "The relation query cannot be null.");
        }
        if (operand == null) {
            throw new IllegalArgumentException(
                    "The operand query cannot be null.");
        }
        this.field = relation.getField();
        if (!operand.getField().equals(field)) {
            throw new IllegalArgumentException(
                    "Clauses must have the same field.");
        }
        this.relationQuery = relation;
        this.operandQuery = operand;
    }
    
    public void checkVariables(SpanRelationQuery relation, SpanQuery operand, SpanQuery target) {
        checkVariables(relation, operand);
        if (target == null) {
            if (operand == null) {
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
    public SimpleSpanQuery clone() {
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
    public Spans getSpans(AtomicReaderContext context, Bits acceptDocs,
            Map<Term, TermContext> termContexts) throws IOException {

        return new FocusSpans((SpanFocusQuery) firstClause, context,
                acceptDocs, termContexts);
    }

    @Override
    public String toString(String field) {
        return getFirstClause().toString();
    }
}
