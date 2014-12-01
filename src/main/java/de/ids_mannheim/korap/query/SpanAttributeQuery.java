package de.ids_mannheim.korap.query;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.ToStringUtils;

import de.ids_mannheim.korap.query.spans.AttributeSpans;

/**
 * SpanAttributeQuery retrieves attribute spans, that are tokens annotated with
 * prefix @, for example {@literal @}:class=header. SpanAttributeQueries are
 * commonly used to search for elements or relations having some specific
 * attribute(s).
 * 
 * Example: <br/>
 * <br/>
 * 
 * <pre>
 * SpanAttributeQuery saq = new SpanAttributeQuery(new SpanTermQuery(new Term(
 *         &quot;base&quot;, &quot;@:class=title&quot;)), true);
 * </pre>
 * 
 * Negation enables searching for elements <em>without</em> some attribute(s).
 * Example:
 * 
 * <pre>
 * SpanAttributeQuery saq = new SpanAttributeQuery(new SpanTermQuery(new Term(
 *         &quot;base&quot;, &quot;@:class=title&quot;)), true, true);
 * </pre>
 * 
 * @author margaretha
 * */
public class SpanAttributeQuery extends SimpleSpanQuery {

    boolean negation;

    /**
     * Constructs a SpanAttributeQuery based on the specified
     * {@link SpanTermQuery} and set whether payloads are to be collected or
     * not.
     * 
     * @param firstClause a {@link SpanTermQuery}
     * @param collectPayloads a boolean flag representing the value
     *        <code>true</code> if payloads are to be collected, otherwise
     *        <code>false</code>.
     */
    public SpanAttributeQuery(SpanTermQuery firstClause, boolean collectPayloads) {
        super(firstClause, collectPayloads);
    }

    /**
     * Constructs a SpanAttributeQuery based on the specified
     * {@link SpanTermQuery}, which is also marked for negation/omission when
     * matching to element/relation spans. Additionally set whether payloads are
     * to be collected or not.
     * 
     * @param firstClause a {@link SpanQuery}
     * @param negation a boolean flag representing the value <code>true</code>
     *        if the attributes are to be omitted when matching with element or
     *        relation spans, otherwise <code>false</code>.
     * @param collectPayloads a boolean flag representing the value
     *        <code>true</code> if payloads are to be collected, otherwise
     *        <code>false</code>.
     */
    public SpanAttributeQuery(SpanTermQuery firstClause, boolean negation,
            boolean collectPayloads) {
        super(firstClause, collectPayloads);
        this.negation = negation;
    }

    /** {@inheritDoc} */
    @Override
    public SimpleSpanQuery clone() {
        SpanAttributeQuery sq = new SpanAttributeQuery(
                (SpanTermQuery) this.firstClause.clone(), this.negation,
                this.collectPayloads);
        sq.setBoost(getBoost());
        return sq;
    }

    @Override
    public Spans getSpans(AtomicReaderContext context, Bits acceptDocs,
            Map<Term, TermContext> termContexts) throws IOException {
        return new AttributeSpans(this, context, acceptDocs, termContexts);
    }

    @Override
    public String toString(String field) {
        StringBuilder sb = new StringBuilder();
        sb.append("spanAttribute(");
        sb.append(firstClause.toString(field));
        if (negation)
            sb.append(", not");
        sb.append(")");
        sb.append(ToStringUtils.boost(getBoost()));
        return sb.toString();
    }

    /**
     * Tells weather the attributes are to be omitted when matching to element
     * or relation spans, or not.
     * 
     * @return <code>true</code> if the attributes are to be omitted when
     *         matching to element or relation spans, <code>false</code>
     *         otherwise.
     */
    public boolean isNegation() {
        return negation;
    }

    /**
     * Sets true if the attributes are to be omitted when matching to element or
     * relation spans, false otherwise.
     * 
     * @param negation a boolean with value <code>true</code>, if the attributes
     *        are to be omitted when matching to element or relation spans,
     *        <code>false</code> otherwise.
     */
    public void setNegation(boolean negation) {
        this.negation = negation;
    }

}
