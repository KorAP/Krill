package de.ids_mannheim.korap.query;

public class DistanceConstraint {
	private int minDistance, maxDistance;
	private String unit;
	private SpanElementQuery elementQuery;
	private boolean exclusion;
	
	public DistanceConstraint(String unit, int min, int max, boolean exclusion) {
		this.unit = unit;
		this.minDistance = min;
		this.maxDistance = max;
		this.exclusion = exclusion;
	}
	
	public DistanceConstraint(SpanElementQuery elementQuery, String unit, 
			int min, int max, boolean exclusion) {
		this(unit, min, max, exclusion);
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
