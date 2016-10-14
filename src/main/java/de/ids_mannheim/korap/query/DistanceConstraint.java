package de.ids_mannheim.korap.query;

/**
 * DistanceConstraint specifies the constraints used in
 * {@link SpanDistanceQuery SpanDistanceQueries} or
 * {@link SpanMultipleDistanceQuery SpanMultipleDistanceQueries}. The
 * constraints comprise the distance unit, the minimum and maximum
 * distances,
 * the order and the co-occurence of the compared spans.
 * 
 * Distance constraint examples:
 * 
 * <ol>
 * <li>Two terms x and y are separated by minimum two and maximum
 * three other
 * words. The order of x and y does not matter.
 * 
 * <pre>
 * DistanceConstraint dc = new DistanceConstraint(2, 3, false, false);
 * </pre>
 * 
 * </li>
 * <li>Two terms x and y are separated by minimum two and maximum
 * three other
 * words. X must precede y.
 * 
 * <pre>
 * DistanceConstraint dc = new DistanceConstraint(2, 3, true, false);
 * </pre>
 * 
 * </li>
 * <li>
 * Term x do not occur with term y in minimum two and maximum three
 * other words.
 * X must precede y.
 * 
 * <pre>
 * DistanceConstraint dc = new DistanceConstraint(2, 3, true, true);
 * </pre>
 * 
 * </li>
 * <li>Two terms x and y separated by minimum one and maximum two
 * <em>sentences</em>. X must precede y.
 * 
 * <pre>
 * SpanElementQuery e = new SpanElementQuery(&quot;tokens&quot;,
 * &quot;s&quot;);
 * DistanceConstraint dc = new DistanceConstraint(e, 2, 3, true,
 * false);
 * </pre>
 * 
 * </li>
 * </ol>
 * 
 * @author margaretha
 */

public class DistanceConstraint {
    private int minDistance, maxDistance;
    private String unit;
    private SpanElementQuery elementQuery;
    private boolean exclusion;
    private boolean isOrdered;


    /**
     * Constructs a DistanceConstraint object with the specified
     * minimum and
     * maximum distances. If isOrdered is true, the spans must occur
     * in order.
     * If exclusion is true, the second span must <em>not</em> occur
     * together
     * with the first span.
     * 
     * @param min
     *            the minimum distance
     * @param max
     *            the maximum distance
     * @param isOrdered
     *            a boolean flag representing the value
     *            <code>true</code>
     *            if the spans should occur in order, false otherwise.
     * @param exclusion
     *            a boolean flag representing the value
     *            <code>true</code>,
     *            if the second span should occur together with the
     *            first span,
     *            false otherwise.
     */
    public DistanceConstraint (int min, int max, boolean isOrdered,
                               boolean exclusion) {
        this.unit = "w";
        this.minDistance = min;
        this.maxDistance = max;
        this.isOrdered = isOrdered;
        this.exclusion = exclusion;
    }


    /**
     * Constructs a DistanceContraint object with the specified
     * SpanElementQuery
     * as the distance unit, and the specified minimum and the maximum
     * distances. If isOrdered is true, the spans must occur in order.
     * If
     * exclusion is true, the second span must <em>not</em> occur
     * together with
     * the first span.
     * 
     * @param elementQuery
     *            the distance unit
     * @param min
     *            the minimum distance
     * @param max
     *            the maximum distance
     * @param isOrdered
     *            a boolean flag representing the value
     *            <code>true</code>
     *            if the spans should occur in order, false otherwise.
     * @param exclusion
     *            a boolean flag representing the value
     *            <code>true</code>,
     *            if the second span should occur together with the
     *            first span,
     *            false otherwise.
     */
    public DistanceConstraint (SpanElementQuery elementQuery, int min, int max,
                               boolean isOrdered, boolean exclusion) {
        if (elementQuery == null) {
            throw new IllegalArgumentException("Element query cannot be null.");
        }

        this.unit = elementQuery.getElementStr();
        this.minDistance = min;
        this.maxDistance = max;
        this.isOrdered = isOrdered;
        this.exclusion = exclusion;
        this.elementQuery = elementQuery;
    }


    /**
     * Returns the minimum distance.
     * 
     * @return the minimum distance
     */
    public int getMinDistance () {
        return minDistance;
    }


    /**
     * Sets the minimum distance.
     * 
     * @param minDistance
     *            the minimum distance
     */
    public void setMinDistance (int minDistance) {
        this.minDistance = minDistance;
    }


    /**
     * Returns the maximum distance.
     * 
     * @return the maximum distance
     */
    public int getMaxDistance () {
        return maxDistance;
    }


    /**
     * Sets the maximum distance.
     * 
     * @param maxDistance
     *            the maximum distance
     */
    public void setMaxDistance (int maxDistance) {
        this.maxDistance = maxDistance;
    }


    /**
     * Returns the distance unit.
     * 
     * @return the distance unit
     */
    public String getUnit () {
        return unit;
    }


    /**
     * Sets the distance unit.
     * 
     * @param unit
     *            the distance unit
     */
    public void setUnit (String unit) {
        this.unit = unit;
    }


    /**
     * Returns the element query used as the distance unit.
     * 
     * @return the element query used as the distance unit
     */
    public SpanElementQuery getElementQuery () {
        return elementQuery;
    }


    /**
     * Sets the element query used as the distance unit.
     * 
     * @param elementQuery
     *            the element query used as the distance unit.
     */
    public void setElementQuery (SpanElementQuery elementQuery) {
        this.elementQuery = elementQuery;
    }


    /**
     * Tells if the second span must occur together with the first
     * span, or not.
     * 
     * @return <code>true</code> if the second span must <em>not</em>
     *         occur
     *         together with the first span, <code>false</code>
     *         otherwise.
     */
    public boolean isExclusion () {
        return exclusion;
    }


    /**
     * Sets <code>true</code> if the second span must <em>not</em>
     * occur
     * together with the first span, <code>false</code> otherwise.
     * 
     * @param exclusion
     *            a boolean with value <code>true</code> if the second
     *            span must <em>not</em> occur together with the first
     *            span,
     *            <code>false</code> otherwise.
     */
    public void setExclusion (boolean exclusion) {
        this.exclusion = exclusion;
    }


    /**
     * Tells if the spans must occur in order or not.
     * 
     * @return <code>true</code> if the spans must occur in order,
     *         <code>false</code> otherwise.
     */
    public boolean isOrdered () {
        return isOrdered;
    }


    /**
     * Sets if the spans must occur in order or not.
     * 
     * @param isOrdered
     *            a boolean with value <code>true</code> if the spans
     *            must
     *            occur in order, <code>false</code> otherwise.
     */
    public void setOrdered (boolean isOrdered) {
        this.isOrdered = isOrdered;
    }
}
