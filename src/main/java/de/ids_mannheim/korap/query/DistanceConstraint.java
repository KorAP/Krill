package de.ids_mannheim.korap.query;

/**
 * DistanceConstraint specifies the constraints used in
 * {@link SpanDistanceQueries} or {@link SpanMultipleDistanceQueries}. The
 * constraints comprise the distance unit, the minimum and maximum distance, the
 * order and the co-occurence of the compared spans.
 * 
 * Distance constraint examples:
 * 
 * <ol>
 * <li>Two terms x and y are separated by minimum two and maximum three other
 * words. The order of x and y does not matter.
 * 
 * <pre>
 * DistanceConstraint dc = new DistanceConstraint(2, 3, false, false);
 * </pre>
 * 
 * </li>
 * <li>Two terms x and y are separated by minimum two and maximum three other
 * words. X must precede y.
 * 
 * <pre>
 * DistanceConstraint dc = new DistanceConstraint(2, 3, true, false);
 * </pre>
 * 
 * </li>
 * <li>
 * Term x do not occur with term y in minimum two and maximum three other words.
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
 * SpanElementQuery e = new SpanElementQuery(&quot;tokens&quot;, &quot;s&quot;);
 * DistanceConstraint dc = new DistanceConstraint(e, 2, 3, true, false);
 * </pre>
 * 
 * </li>
 * </ol>
 * 
 * @author margaretha
 * */

public class DistanceConstraint {
    private int minDistance, maxDistance;
    private String unit;
    private SpanElementQuery elementQuery;
    private boolean exclusion;
    private boolean isOrdered;

    public DistanceConstraint(int min, int max, boolean isOrdered,
            boolean exclusion) {
        this.unit = "w";
        this.minDistance = min;
        this.maxDistance = max;
        this.isOrdered = isOrdered;
        this.exclusion = exclusion;
    }

    public DistanceConstraint(SpanElementQuery elementQuery, int min, int max,
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

    public int getMinDistance() {
        return minDistance;
    }

    public void setMinDistance(int minDistance) {
        this.minDistance = minDistance;
    }

    public int getMaxDistance() {
        return maxDistance;
    }

    public void setMaxDistance(int maxDistance) {
        this.maxDistance = maxDistance;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public SpanElementQuery getElementQuery() {
        return elementQuery;
    }

    public void setElementQuery(SpanElementQuery elementQuery) {
        this.elementQuery = elementQuery;
    }

    public boolean isExclusion() {
        return exclusion;
    }

    public void setExclusion(boolean exclusion) {
        this.exclusion = exclusion;
    }

    public boolean isOrdered() {
        return isOrdered;
    }

    public void setOrdered(boolean isOrdered) {
        this.isOrdered = isOrdered;
    }
}
