package de.ids_mannheim.korap.query;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.search.spans.TermSpans;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.ToStringUtils;

import de.ids_mannheim.korap.query.spans.ElementSpans;

/**
 * SpanElementQuery retrieves {@link ElementSpans} which are special
 * {@link Term Terms} with prefix &quot;&lt;&gt;&quot;.
 * Unlike {@link TermSpans} ElementSpans may span multiple tokens
 * comprising a
 * phrase, a clause, a sentence and so on. <br/>
 * <br/>
 * Examples of {@link ElementSpans} are
 * 
 * <ul>
 * <li>sentences indexed as &lt;&gt;:s
 * 
 * <pre>
 * SpanElementQuery seq = new SpanElementQuery(&quot;tokens&quot;,
 * &quot;s&quot;);
 * </pre>
 * 
 * </li>
 * <li>paragraphs indexed as &lt;&gt;:p
 * 
 * <pre>
 * SpanElementQuery seq = new SpanElementQuery(&quot;tokens&quot;,
 * &quot;p&quot;);
 * </pre>
 * 
 * </li>
 * 
 * </ul>
 * 
 * @author diewald
 * @author margaretha
 */
public class SpanElementQuery extends SimpleSpanQuery {
    private static Term elementTerm;
    private String elementStr;


    /**
     * Constructs a SpanElementQuery for the given term in the given
     * field.
     * 
     * @param field
     *            a field where a term belongs to
     * @param term
     *            a term
     */
    public SpanElementQuery (String field, String term) {
        super(new SpanTermQuery((elementTerm = new Term(field, "<>:" + term))),
                true);
        this.elementStr = term;
    };


    @Override
    public Spans getSpans (final AtomicReaderContext context, Bits acceptDocs,
            Map<Term, TermContext> termContexts) throws IOException {
        return new ElementSpans(this, context, acceptDocs, termContexts);
    };


    /**
     * Returns the element name or string, for instance "s" for
     * sentence
     * elements.
     * 
     * @return the element name/string.
     */
    public String getElementStr () {
        return elementStr;
    };


    /**
     * Sets the element name or string, for instance "s" for sentence
     * elements.
     * 
     * @param elementStr
     *            the element name or string
     */
    public void setElementStr (String elementStr) {
        this.elementStr = elementStr;
    }


    @Override
    public SimpleSpanQuery clone () {
        SpanElementQuery sq = new SpanElementQuery(this.getField(),
                this.getElementStr());
        sq.setBoost(this.getBoost());
        return sq;
    };


    @Override
    public void extractTerms (Set<Term> terms) {
        terms.add(elementTerm);
    };


    @Override
    public String toString (String field) {
        StringBuilder buffer = new StringBuilder("<");
        buffer.append(getField()).append(':').append(elementStr);
        buffer.append(ToStringUtils.boost(getBoost()));
        return buffer.append(" />").toString();
    };


    @Override
    public int hashCode () {
        final int prime = 37; // Instead of 31
        int result = super.hashCode();
        result = prime * result
                + ((elementStr == null) ? 0 : elementStr.hashCode());
        return result;
    };


    @Override
    public boolean equals (Object obj) {
        if (this == obj)
            return true;
        // if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass())
            return false;

        SpanElementQuery other = (SpanElementQuery) obj;
        if (elementStr == null && other.elementStr != null)
            return false;
        else if (!elementStr.equals(other.elementStr))
            return false;

        if (!getField().equals(other.getField()))
            return false;
        return true;
    };

};
