package de.ids_mannheim.korap.query;

import java.util.List;

import org.apache.lucene.search.spans.SpanQuery;

/**
 * Base query for span queries whose resulting spans requires an id,
 * for
 * instance {@link SpanElementQuery} and {@link SpanRelationQuery}.
 * 
 * @author margaretha
 * 
 */
public abstract class SpanWithIdQuery extends SimpleSpanQuery {

    /**
     * Constructs SpanWithIdQuery based on the given {@link SpanQuery}
     * and the
     * collectPayloads flag, for example, {@link SpanElementQuery}.
     * 
     * @param firstClause
     *            a SpanQuery
     * @param collectPayloads
     *            a boolean flag representing the value
     *            <code>true</code> if payloads are to be collected,
     *            otherwise
     *            <code>false</code>.
     */
    public SpanWithIdQuery (SpanQuery firstClause, boolean collectPayloads) {
        super(firstClause, collectPayloads);
    }


    /**
     * Constructs SpanWithIdQuery based on two span queries and the
     * collectPayloads flag, for instance, query a relation having a
     * specific
     * attribute.
     * 
     * @param firstClause
     *            a SpanQuery
     * @param secondClause
     *            a SpanQuery
     * @param collectPayloads
     *            a boolean flag representing the value
     *            <code>true</code> if payloads are to be collected,
     *            otherwise
     *            <code>false</code>.
     */
    public SpanWithIdQuery (SpanQuery firstClause, SpanQuery secondClause,
                            boolean collectPayloads) {
        super(firstClause, secondClause, collectPayloads);
    }


    /**
     * Constructs SpanWithIdQuery based on a span query and a list of
     * span
     * queries, for instance, query an element having two specific
     * attributes.
     * 
     * @param firstClause
     *            a SpanQuery
     * @param secondClauses
     *            a list of SpanQuery
     * @param collectPayloads
     *            a boolean flag representing the value
     *            <code>true</code> if payloads are to be collected,
     *            otherwise
     *            <code>false</code>.
     */
    public SpanWithIdQuery (SpanQuery firstClause,
                            List<SpanQuery> secondClauses,
                            boolean collectPayloads) {
        super(firstClause, secondClauses, collectPayloads);
    }


    public SpanWithIdQuery (List<SpanQuery> clauses, boolean collectPayloads) {
        super(clauses, collectPayloads);
    }
}
