package de.ids_mannheim.korap.query;

/**	Create a distance constraint for multiple distance query.
 * 	 
 * 	@author margaretha
 * */

public class DistanceConstraint {
	private int minDistance, maxDistance;
	private String unit;
	private SpanElementQuery elementQuery;
	private boolean exclusion;
	
	public DistanceConstraint(int min, int max, boolean exclusion) {
		this.unit = "w";
		this.minDistance = min;
		this.maxDistance = max;
		this.exclusion = exclusion;
	}
	
	public DistanceConstraint(SpanElementQuery elementQuery, int min, int max, 
			boolean exclusion) {
		this.unit = elementQuery.getElementStr();
		this.minDistance = min;
		this.maxDistance = max;
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
}
