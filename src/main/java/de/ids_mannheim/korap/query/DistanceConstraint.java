package de.ids_mannheim.korap.query;

import org.apache.lucene.search.spans.SpanQuery;

public class DistanceConstraint {
	int minDistance, maxDistance;
	String unit;
	SpanQuery elementQuery;
	
	public DistanceConstraint(String unit, int min, int max) {
		this.unit = unit;
		this.minDistance = min;
		this.maxDistance = max;
	}
	
	public DistanceConstraint(SpanQuery elementQuery, String unit, 
			int min, int max) {
		this(unit, min, max);
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
	public SpanQuery getElementQuery() {
		return elementQuery;
	}
	public void setElementQuery(SpanQuery elementQuery) {
		this.elementQuery = elementQuery;
	}
}
