package de.ids_mannheim.korap.query.wrap;

import java.util.*;
import de.ids_mannheim.korap.query.DistanceConstraint;
import de.ids_mannheim.korap.query.SpanElementQuery;
import de.ids_mannheim.korap.query.SpanNextQuery;
import de.ids_mannheim.korap.query.SpanDistanceQuery;
import de.ids_mannheim.korap.query.SpanMultipleDistanceQuery;

import de.ids_mannheim.korap.query.wrap.SpanSegmentQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanRegexQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanWildcardQueryWrapper;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import de.ids_mannheim.korap.query.wrap.SpanQueryWrapperInterface;

/**
 * @author Nils Diewald
 */
public class SpanSequenceQueryWrapper implements SpanQueryWrapperInterface {
    private String field;
    private ArrayList<SpanQuery> segments;
    private ArrayList<DistanceConstraint> constraints;
    private boolean isInOrder = true;
    private boolean isNull = true;

    public SpanSequenceQueryWrapper (String field) {
	this.field = field;
	this.segments = new ArrayList<SpanQuery>(2);
    };

    public SpanSequenceQueryWrapper (String field, String ... terms) {
	this(field);
	for (int i = 0; i < terms.length; i++) {
	    this.segments.add((SpanQuery) new SpanTermQuery(new Term(field, terms[i])));
	};
	this.isNull = false;
    };

    public SpanSequenceQueryWrapper (String field, SpanQuery sq) {
	this(field);
	this.segments.add((SpanQuery) sq);
	this.isNull = false;
    };

    public SpanSequenceQueryWrapper (String field, SpanQueryWrapperInterface sswq) {
	this(field);
	if (!sswq.isNull()) {
	    this.segments.add((SpanQuery) sswq.toQuery());
	    this.isNull = false;
	};
    };

    public SpanSequenceQueryWrapper (String field, SpanRegexQueryWrapper re) {
	this(field);
	if (!re.isNull()) {
	    this.segments.add((SpanQuery) re.toQuery());
	    this.isNull = false;
	};
    };

    public SpanSequenceQueryWrapper (String field, SpanWildcardQueryWrapper wc) {
	this(field);
	if (!wc.isNull()) {
	    this.segments.add((SpanQuery) wc.toQuery());
	    this.isNull = false;
	};
    };

    public SpanQuery get (int index) {
	return this.segments.get(index);
    };

    public void set (int index, SpanQuery sq) {
	this.segments.set(index, sq);
    };

    public SpanSequenceQueryWrapper append (String term) {
	this.segments.add((SpanQuery) new SpanTermQuery(new Term(field, term)));
	this.isNull = false;
	return this;
    };

    public SpanSequenceQueryWrapper append (SpanQueryWrapperInterface ssq) {
	if (!ssq.isNull()) {
	    this.segments.add((SpanQuery) ssq.toQuery());
	    this.isNull = false;
	};
	return this;
    };

    public SpanSequenceQueryWrapper append (SpanRegexQueryWrapper srqw) {
	if (!srqw.isNull()) {
	    this.segments.add((SpanQuery) srqw.toQuery());
	    this.isNull = false;
	};
	return this;
    };
    
    public SpanSequenceQueryWrapper append (SpanWildcardQueryWrapper swqw) {
	if (!swqw.isNull()) {
	    this.segments.add((SpanQuery) swqw.toQuery());
	    this.isNull = false;
	};
	return this;
    };

    public SpanSequenceQueryWrapper prepend (String term) {
	this.segments.add(0, (SpanQuery) new SpanTermQuery(new Term(field, term)));
	this.isNull = false;
	return this;
    };

    public SpanSequenceQueryWrapper prepend (SpanSegmentQueryWrapper ssq) {
	if (!ssq.isNull()) {
	    this.segments.add(0, (SpanQuery) ssq.toQuery());
	    this.isNull = false;
	};
	return this;
    };

    public SpanSequenceQueryWrapper prepend (SpanRegexQueryWrapper re) {
	if (!re.isNull()) {
	    this.segments.add(0, (SpanQuery) re.toQuery());
	    this.isNull = false;
	};
	return this;
    };

    public SpanSequenceQueryWrapper prepend (SpanWildcardQueryWrapper swqw) {
	if (!swqw.isNull()) {
	    this.segments.add(0, (SpanQuery) swqw.toQuery());
	    this.isNull = false;
	};
	return this;
    };

    public SpanSequenceQueryWrapper withConstraint (int min, int max) {
	return this.withConstraint(min, max, false);
    };

    public SpanSequenceQueryWrapper withConstraint (int min, int max, boolean exclusion) {
	if (this.constraints == null)
	    this.constraints = new ArrayList<DistanceConstraint>(1);
	this.constraints.add(new DistanceConstraint(min, max, isInOrder, exclusion));
	return this;
    };

    public SpanSequenceQueryWrapper withConstraint (int min, int max, String unit) {
	return this.withConstraint(min, max, unit, false);
    };
    
    public SpanSequenceQueryWrapper withConstraint (int min,
						    int max,
						    String unit,
						    boolean exclusion) {
	if (this.constraints == null)
	    this.constraints = new ArrayList<DistanceConstraint>(1);
	if (unit.equals("w"))
	    this.constraints.add(new DistanceConstraint(min, max, isInOrder, exclusion));
	else
	    this.constraints.add(
		 new DistanceConstraint(
                     new SpanElementQuery(this.field, unit), min, max, isInOrder, exclusion)
	    );
	return this;
    };

    
    public SpanQuery toQuery () {
	if (this.segments.size() == 0 || this.isNull) {
	    return (SpanQuery) null;
	};

	SpanQuery query = this.segments.get(0);

	// NextQueries:
	if (this.constraints == null || this.constraints.size() == 0 ||
	    (this.constraints.size() == 1 &&
	     (this.constraints.get(0).getMinDistance() == 1 &&
	      this.constraints.get(0).getMaxDistance() == 1))) {
	    for (int i = 1; i < this.segments.size(); i++) {
		query = new SpanNextQuery(
	            query,
		    this.segments.get(i) // Todo: Maybe payloads are not necessary
		);
	    };
	    return (SpanQuery) query;
	};

	// DistanceQueries
	if (this.constraints.size() == 1) {
	    DistanceConstraint constraint = this.constraints.get(0);

	    // Create spanElementDistance query
	    if (!constraint.getUnit().equals("w")) {
		for (int i = 1; i < this.segments.size(); i++) {
		    SpanDistanceQuery sdquery = new SpanDistanceQuery(
			    query,
				this.segments.get(i),
				constraint,
				true
		    );
		    query = (SpanQuery) sdquery;
		};
	    }

	    // Create spanDistance query
	    else {
		for (int i = 1; i < this.segments.size(); i++) {
		    SpanDistanceQuery sdquery = new SpanDistanceQuery(
	                query,
			this.segments.get(i),
			constraint,
			true
		    );
		    query = (SpanQuery) sdquery;
		};
	    };

	    return (SpanQuery) query;
	};

	// MultipleDistanceQueries
	for (int i = 1; i < this.segments.size(); i++) {
	    query = new SpanMultipleDistanceQuery(
	        query,
		this.segments.get(i),
		this.constraints,
		isInOrder,
		true
	    );
	};

	return (SpanQuery) query;
    };

    public void setInOrder (boolean isInOrder) {
	this.isInOrder = isInOrder;
    };

    public boolean isInOrder () {
	return this.isInOrder;
    };

    public boolean hasConstraints () {
	if (this.constraints == null)
	    return false;
	if (this.constraints.size() <= 0)
	    return false;
	return true;
    };

    public boolean isOptional () {
	return false;
    };

    public boolean isNull () {
	return this.isNull;
    };
};
