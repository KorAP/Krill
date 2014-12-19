package de.ids_mannheim.korap.query;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.ToStringUtils;

import de.ids_mannheim.korap.query.spans.RelationSpans;

/**
 * SpanRelationQuery retrieves spans representing a relation between tokens,
 * elements, or a-token-and-an-element. Relation are marked with prefix "<" or
 * ">". The direction of the angle bracket represents the direction of the 
 * corresponding relation. <br/><br/>
 * 
 * This class provides two types of query:
 * <ol>
 * <li>querying any relations, for instance dependency relation "<:xip/syntax-dep_rel".
 * 
 * <pre>SpanRelationQuery sq = new SpanRelationQuery(
 *          new SpanTermQuery(
 *              new Term("tokens","<:xip/syntax-dep_rel")),
 *          true); 
 * </pre>
 * </li>
 * <li>querying relations matching a certain type of sources/targets, that are the
 * left or the right sides of the relations. This query is used within 
 * {@link SpanRelationPartQuery}, for instance, to retrieve all dependency relations 
 * "<:xip/syntax-dep_rel" whose sources (right side) are noun phrases. 
 * <pre>
 * SpanRelationPartQuery rv =
 *      new SpanRelationPartQuery(sq, new SpanElementQuery("tokens","np"), true, 
 *      false, true);
 * </pre>
 * </li>
 * 
 * </ol>
 * 
 * @author margaretha
 * */
public class SpanRelationQuery extends SpanWithIdQuery {

    /**
     * Constructs a SpanRelationQuery based on the given span query.
     * 
     * @param firstClause a SpanQuery.
     * @param collectPayloads a boolean flag representing the value
     *        <code>true</code> if payloads are to be collected, otherwise
     *        <code>false</code>.
     */
    public SpanRelationQuery(SpanQuery firstClause, boolean collectPayloads) {
        super(firstClause, collectPayloads);
    }

    /**
     * Constructs a SpanRelationQuery which embeds another
     * {@link SpanRelationQuery}. This is useful for querying a relation having
     * a specific variable.
     * 
     * @param spanRelationQuery a SpanRelationQuery
     * @param secondClause a SpanQuery
     * @param collectPayloads a boolean flag representing the value
     *        <code>true</code> if payloads are to be collected, otherwise
     *        <code>false</code>.
     */
    public SpanRelationQuery(SpanRelationQuery spanRelationQuery,
            SpanQuery secondClause, boolean collectPayloads) {
        super(spanRelationQuery, secondClause, collectPayloads);
    }

    @Override
    public SimpleSpanQuery clone() {
        SimpleSpanQuery sq = new SpanRelationQuery(
                (SpanQuery) this.firstClause.clone(), this.collectPayloads);
        return sq;
    }

    @Override
    public Spans getSpans(AtomicReaderContext context, Bits acceptDocs,
            Map<Term, TermContext> termContexts) throws IOException {
        return new RelationSpans(this, context, acceptDocs, termContexts);
    }

    @Override
    public String toString(String field) {
        StringBuilder sb = new StringBuilder();
        sb.append("spanRelation(");
        sb.append(firstClause.toString(field));
        sb.append(")");
        sb.append(ToStringUtils.boost(getBoost()));
        return sb.toString();
    }

}
