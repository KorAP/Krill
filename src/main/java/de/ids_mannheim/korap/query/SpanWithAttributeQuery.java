package de.ids_mannheim.korap.query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.spans.SimpleSpans;
import de.ids_mannheim.korap.query.spans.SpansWithAttribute;

/**
 * Enumeration of spans (e.g. element or relation spans) having some
 * specific attribute(s) or <em>not</em> having some attribute(s). It
 * is necessary that the spans have ids to be able to identify to
 * which span an attribute belongs. <br />
 * <br />
 * 
 * In the example below, the SpanWithAttributeQuery retrieves
 * <code>&lt;div&gt;</code> elements having attributes
 * <code>@:class=header</code>.
 * 
 * <pre>
 * SpanAttributeQuery saq = new SpanAttributeQuery(new
 * SpanTermQuery(new Term(
 * &quot;tokens&quot;, &quot;@:class=header&quot;)), true);
 * SpanWithAttributeQuery sq = new SpanWithAttributeQuery(new
 * SpanElementQuery(
 * &quot;tokens&quot;, &quot;div&quot;), saq, true);
 * </pre>
 * 
 * 
 * @author margaretha
 */
public class SpanWithAttributeQuery extends SimpleSpanQuery {

    public boolean isMultipleAttributes;
    private String type;


    /**
     * Constructs a SpanWithAttributeQuery for any arbitrary
     * SpansWithId (e.g. elements, relations) having the specified
     * {@link SpanAttributeQuery}.
     * 
     * @param attributeQuery
     *            a SpanAttributeQuery
     * @param collectPayloads
     *            a boolean flag representing the value
     *            <code>true</code> if payloads are to be collected,
     *            otherwise <code>false</code>.
     */
    public SpanWithAttributeQuery (SpanAttributeQuery attributeQuery,
                                   boolean collectPayloads) {
        super(attributeQuery, collectPayloads);
        type = "spanWithAttribute";
    }


    public SpanWithAttributeQuery (List<SpanQuery> attributeQueries,
                                   boolean collectPayloads) {
        super(attributeQueries, collectPayloads);
        isMultipleAttributes = true;
        type = "spanWithAttribute";
    }


    /**
     * Constructs a SpanWithAttributeQuery for the specified
     * SpanWithIdQuery and SpanAttributeQuery retrieving spans having
     * a specific attribute.
     * 
     * If the SpanWithIdQuery is a SpanAttributeQuery, this will
     * return arbitrary elements with two specified attributes (i.e.
     * and relation between the two attributes).
     * 
     * @param firstClause
     *            a SpanWithIdQuery
     * @param secondClause
     *            a SpanAttributeQuery
     * @param collectPayloads
     *            a boolean flag representing the value
     *            <code>true</code> if payloads are to be collected,
     *            otherwise <code>false</code>.
     */
    public SpanWithAttributeQuery (SimpleSpanQuery firstClause,
                                   SpanAttributeQuery secondClause,
                                   boolean collectPayloads) {
        super(firstClause, secondClause, collectPayloads);
        setType();
    }


    /**
     * @param firstClause
     *            a SpanWithIdQuery
     * @param secondClauses
     *            a list of SpanAttributeQueries
     * @param collectPayloads
     *            a boolean flag representing the value
     *            <code>true</code> if payloads are to be collected,
     *            otherwise <code>false</code>.
     */
    public SpanWithAttributeQuery (SimpleSpanQuery firstClause,
                                   List<SpanQuery> secondClauses,
                                   boolean collectPayloads) {
        super(firstClause, secondClauses, collectPayloads);
        isMultipleAttributes = true;
        setType();
    }


    /**
     * Returns the type of the query.
     * 
     * @return the type of the query
     */
    public String getType () {
        return type;
    }


    /**
     * Sets the type of the query based of the class of the
     * firstClause / first span.
     * 
     */
    public void setType () {
        if (SpanElementQuery.class.isInstance(firstClause)) {
            type = "spanElementWithAttribute";
        }
        else if (SpanFocusQuery.class.isInstance(firstClause)) {
            type = "spanRelationWithAttribute";
        }
        else if (SpanTermWithIdQuery.class.isInstance(firstClause)) {
            type = "spanTermWithAttribute";
        }
    }


    @Override
    public SpanWithAttributeQuery clone () {
        if (secondClause != null) {
            if (isMultipleAttributes) {
                return new SpanWithAttributeQuery(
                        (SimpleSpanQuery) firstClause.clone(),
                        cloneClauseList(), collectPayloads);
            }
            else {
                return new SpanWithAttributeQuery(
                        (SimpleSpanQuery) firstClause.clone(),
                        (SpanAttributeQuery) secondClause.clone(),
                        collectPayloads);
            }
        }
        else {
            if (isMultipleAttributes) {
                return new SpanWithAttributeQuery(cloneClauseList(),
                        collectPayloads);
            }
            else {
                return new SpanWithAttributeQuery(
                        (SpanAttributeQuery) firstClause.clone(),
                        collectPayloads);
            }
        }
    }


    private List<SpanQuery> cloneClauseList () {
        List<SpanQuery> clauseList = new ArrayList<SpanQuery>();
        SpanAttributeQuery saq;
        for (SpanQuery q : this.clauseList) {
            saq = (SpanAttributeQuery) q;
            clauseList.add(saq.clone());
        }
        return clauseList;
    }


    @Override
    public Spans getSpans (LeafReaderContext context, Bits acceptDocs,
            Map<Term, TermContext> termContexts) throws IOException {

        if (type.equals("spanWithAttribute")) {
            return new SpansWithAttribute(this, context, acceptDocs,
                    termContexts);
        }

        SimpleSpans spans = (SimpleSpans) this.getFirstClause().getSpans(
                context, acceptDocs, termContexts);
        return new SpansWithAttribute(this, spans, context, acceptDocs,
                termContexts);
    }


    @Override
    public String toString (String field) {
        boolean isFirstClassNull = true;
        StringBuilder sb = new StringBuilder();
        sb.append(type);
        sb.append("(");
        if (firstClause != null) {
            sb.append(firstClause.toString(field));
            isFirstClassNull = false;
        }
        if (secondClause != null) {
            sb.append(", ");
            sb.append(secondClause.toString(field));
        }
        else if (isMultipleAttributes) {
            if (!isFirstClassNull)
                sb.append(", ");
            sb.append("[");

            SpanQuery sq;
            for (int i = 0; i < clauseList.size(); i++) {
                sq = clauseList.get(i);
                sb.append(sq.toString(field));

                if (i < clauseList.size() - 1)
                    sb.append(", ");
            }

            sb.append("]");
        }
        sb.append(")");
        return sb.toString();
    }
}
