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

import de.ids_mannheim.korap.query.spans.DistanceExclusionSpans;
import de.ids_mannheim.korap.query.spans.ElementDistanceExclusionSpans;
import de.ids_mannheim.korap.query.spans.ElementDistanceSpans;
import de.ids_mannheim.korap.query.spans.TokenDistanceSpans;
import de.ids_mannheim.korap.query.spans.UnorderedElementDistanceSpans;
import de.ids_mannheim.korap.query.spans.UnorderedTokenDistanceSpans;

/**
 * SpanDistanceQuery calculates the distance between two spans and compares it
 * to the distance constraints. The distance constraints are specified as a
 * {@link DistanceConstraint} instance having various properties: the distance
 * unit, the order of the spans (ordered or unordered), co-occurrence (i.e. the
 * spans should co-occur or not), minimum and maximum distance. <br/>
 * <br/>
 * The distance unit can be a word (token), a sentence or a paragraph. The
 * resulting spans typically stretch from the starting position of a former span
 * to the end position of the latter span. <br/>
 * <br/>
 * Query examples:
 * 
 * <ol>
 * <li>Search two terms x and y which are separated by minimum two and maximum
 * three other words. The order of x and y does not matter.
 * 
 * <pre>
 * DistanceConstraint dc = new DistanceConstraint(2, 3, false, false);
 * </pre>
 * 
 * </li>
 * <li>Search two terms x and y which are separated by minimum two and maximum
 * three other words. X must precede y.
 * 
 * <pre>
 * DistanceConstraint dc = new DistanceConstraint(2, 3, true, false);
 * </pre>
 * 
 * </li>
 * <li>
 * Search term x which do not occur with term y in minimum two and maximum three
 * other words. X must precede y.
 * 
 * <pre>
 * DistanceConstraint dc = new DistanceConstraint(2, 3, true, true);
 * </pre>
 * 
 * </li>
 * <li>Search two terms x and y separated by minimum one and maximum two
 * sentences. X must precede y.
 * 
 * <pre>
 * SpanElementQuery e = new SpanElementQuery(&quot;tokens&quot;, &quot;s&quot;);
 * DistanceConstraint dc = new DistanceConstraint(e, 2, 3, true, false);
 * </pre>
 * 
 * </li>
 * </ol>
 * 
 * SpanDistanceQuery examples:
 * 
 * <ol>
 * <li>
 * 
 * <pre>
 * SpanDistanceQuery sq = new SpanDistanceQuery(new SpanTermQuery(new Term(
 *         &quot;tokens&quot;, x)), new SpanTermQuery(new Term(&quot;tokens&quot;, y)), dc, true);
 * </pre>
 * 
 * </li>
 * <li>
 * 
 * <pre>
 * SpanDistanceQuery sq = new SpanDistanceQuery(
 *         new SpanElementQuery(&quot;tokens&quot;, &quot;s&quot;), new SpanElementQuery(&quot;tokens&quot;, y),
 *         dc, true);
 * </pre>
 * 
 * </li>
 * </ol>
 * 
 * 
 * @author margaretha
 * */
public class SpanDistanceQuery extends SimpleSpanQuery {

    private boolean exclusion;
    private boolean isOrdered;
    private int minDistance, maxDistance;
    private SpanElementQuery elementQuery; // element distance unit (sentence or
                                           // paragraph)
    private String distanceUnit;
    private String spanName;
    private DistanceConstraint constraint;

    /**
     * Constructs a SpanDistanceQuery comparing the distance between the spans
     * of the two specified spanqueries and based-on the given distance
     * constraints.
     * 
     * @param firstClause a span query
     * @param secondClause a span query
     * @param constraint a DistanceConstraint containing all the constraints
     *        required for the distance query
     * @param collectPayloads a boolean flag representing the value
     *        <code>true</code> if payloads are to be collected, otherwise
     *        <code>false</code>.
     */
    public SpanDistanceQuery(SpanQuery firstClause, SpanQuery secondClause,
            DistanceConstraint constraint, boolean collectPayloads) {
        super(firstClause, secondClause, collectPayloads);

        if (constraint == null) {
            throw new IllegalArgumentException(
                    "Distance constraint cannot be null.");
        }

        this.constraint = constraint;
        this.minDistance = constraint.getMinDistance();
        this.maxDistance = constraint.getMaxDistance();
        this.isOrdered = constraint.isOrdered();
        this.exclusion = constraint.isExclusion();
        this.distanceUnit = constraint.getUnit();

        if (constraint.getElementQuery() != null) {
            spanName = "spanElementDistance";
            this.elementQuery = constraint.getElementQuery();
        } else {
            spanName = "spanDistance";
        }
    }

    @Override
    public String toString(String field) {
        StringBuilder sb = new StringBuilder();
        sb.append(this.spanName);
        sb.append("(");
        sb.append(firstClause.toString(field));
        sb.append(", ");
        sb.append(secondClause.toString(field));
        sb.append(", ");
        sb.append("[(");
        sb.append(distanceUnit);
        sb.append("[");
        sb.append(minDistance);
        sb.append(":");
        sb.append(maxDistance);
        sb.append("], ");
        sb.append(isOrdered ? "ordered, " : "notOrdered, ");
        sb.append(exclusion ? "excluded)])" : "notExcluded)])");
        sb.append(ToStringUtils.boost(getBoost()));
        return sb.toString();
    }

    @Override
    public SpanDistanceQuery clone() {
        SpanDistanceQuery spanDistanceQuery = new SpanDistanceQuery(
                (SpanQuery) firstClause.clone(),
                (SpanQuery) secondClause.clone(), this.constraint,
                this.collectPayloads);

        if (this.elementQuery != null) {
            spanDistanceQuery.setElementQuery(this.elementQuery);
        }
        spanDistanceQuery.setBoost(getBoost());
        return spanDistanceQuery;
    }

    @Override
    public Spans getSpans(AtomicReaderContext context, Bits acceptDocs,
            Map<Term, TermContext> termContexts) throws IOException {

        if (this.elementQuery != null) {
            if (isExclusion()) {
                return new ElementDistanceExclusionSpans(this, context,
                        acceptDocs, termContexts);
            } else if (isOrdered) {
                return new ElementDistanceSpans(this, context, acceptDocs,
                        termContexts);
            }
            return new UnorderedElementDistanceSpans(this, context, acceptDocs,
                    termContexts);

        } else if (isExclusion()) {
            return new DistanceExclusionSpans(this, context, acceptDocs,
                    termContexts);
        } else if (isOrdered) {
            return new TokenDistanceSpans(this, context, acceptDocs,
                    termContexts);
        }
        return new UnorderedTokenDistanceSpans(this, context, acceptDocs,
                termContexts);
    }

    /**
     * Returns the minimum distance constraint.
     * 
     * @return the minimum distance constraint
     */
    public int getMinDistance() {
        return minDistance;
    }

    /**
     * Sets the minimum distance constraint.
     * 
     * @param minDistance the minimum distance constraint
     */
    public void setMinDistance(int minDistance) {
        this.minDistance = minDistance;
    }

    /**
     * Returns the maximum distance.
     * 
     * @return the maximum distance constraint
     */
    public int getMaxDistance() {
        return maxDistance;
    }

    /**
     * Sets a maximum distance.
     * 
     * @param maxDistance the maximum distance
     */
    public void setMaxDistance(int maxDistance) {
        this.maxDistance = maxDistance;
    }

    /**
     * Returns the element query used as the distance unit.
     * 
     * @return the element distance unit
     */
    public SpanElementQuery getElementQuery() {
        return elementQuery;
    }

    /**
     * Sets the specified element query used as the distance unit.
     * 
     * @param elementQuery the SpanElementQuery used as the distance unit
     */
    public void setElementQuery(SpanElementQuery elementQuery) {
        this.elementQuery = elementQuery;
    }

    /**
     * Tells weather the second sub-span should co-occur or not.
     * 
     * @return a boolean with <code>true</code> if the second sub-span should
     *         <em>not</em> co-occur, <code>false</code> otherwise.
     */
    public boolean isExclusion() {
        return exclusion;
    }

    /**
     * Sets <code>true</code> if the second sub-span should <em>not</em>
     * co-occur, <code>false</code> otherwise.
     * 
     * @param exclusion a boolean with value <code>true</code> if the second
     *        sub-span should <em>not</em> co-occur, <code>false</code>
     *        otherwise.
     */
    public void setExclusion(boolean exclusion) {
        this.exclusion = exclusion;
    }

    /**
     * Tells whether the spans must occur in order or not.
     * 
     * @return <code>true</code> if the spans must occur in order,
     *         <code>false</code> otherwise.
     */
    public boolean isOrdered() {
        return isOrdered;
    }

    /**
     * Sets whether the spans must occur in order or not.
     * 
     * @param isOrdered <code>true</code> if the spans must occur in order,
     *        <code>false</code> otherwise.
     */
    public void setOrder(boolean isOrdered) {
        this.isOrdered = isOrdered;
    }

}
