package de.ids_mannheim.korap.query.wrap;

import java.util.*;
import de.ids_mannheim.korap.query.SpanNextQuery;
import de.ids_mannheim.korap.query.SpanDistanceQuery;
import de.ids_mannheim.korap.query.wrap.SpanSegmentQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanRegexQueryWrapper;

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


    private class DistanceConstraint {
	private int min = 0;
	private int max = 0;
	private String element = null;

	public DistanceConstraint (int min, int max) {
	    this.min = min;
	    this.max = max;
	};

	public DistanceConstraint (int min, int max, String element) {
	    this.min = min;
	    this.max = max;
	    this.element = element;
	};

	public boolean hasElement () {
	    return (this.element != null ? true : false);
	};

	public String getElement () {
	    return this.element;
	};

	public int getMin () {
	    return this.min;
	};

	public int getMax () {
	    return this.max;
	};
    };

    public SpanSequenceQueryWrapper (String field) {
	this.field = field;
	this.segments = new ArrayList<SpanQuery>(2);
    };

    public SpanSequenceQueryWrapper (String field, String ... terms) {
	this(field);
	for (int i = 0; i < terms.length; i++) {
	    this.segments.add((SpanQuery) new SpanTermQuery(new Term(field, terms[i])));
	};
    };

    public SpanSequenceQueryWrapper (String field, SpanQuery sq) {
	this(field);
	this.segments.add((SpanQuery) sq);
    };

    public SpanSequenceQueryWrapper (String field, SpanQueryWrapperInterface sswq) {
	this(field);
	this.segments.add((SpanQuery) sswq.toQuery());
    };

    public SpanSequenceQueryWrapper (String field, SpanRegexQueryWrapper re) {
	this(field);
	this.segments.add((SpanQuery) re.toQuery());
    };

    public SpanQuery get (int index) {
	return this.segments.get(index);
    };

    public void set (int index, SpanQuery sq) {
	this.segments.set(index, sq);
    };

    public SpanSequenceQueryWrapper append (String term) {
	this.segments.add((SpanQuery) new SpanTermQuery(new Term(field, term)));
	return this;
    };

    public SpanSequenceQueryWrapper append (SpanQueryWrapperInterface ssq) {
	this.segments.add((SpanQuery) ssq.toQuery());
	return this;
    };

    public SpanSequenceQueryWrapper append (SpanRegexQueryWrapper srqw) {
	this.segments.add((SpanQuery) srqw.toQuery());
	return this;
    };

    public SpanSequenceQueryWrapper prepend (String term) {
	this.segments.add(0, (SpanQuery) new SpanTermQuery(new Term(field, term)));
	return this;
    };

    public SpanSequenceQueryWrapper prepend (SpanSegmentQueryWrapper ssq) {
	this.segments.add(0, (SpanQuery) ssq.toQuery());
	return this;
    };

    public SpanSequenceQueryWrapper prepend (SpanRegexQueryWrapper re) {
	this.segments.add(0, (SpanQuery) re.toQuery());
	return this;
    };

    public SpanSequenceQueryWrapper withConstraint (int min, int max) {
	this.constraints.add(new DistanceConstraint(min, max));
	return this;
    };

    public SpanSequenceQueryWrapper withConstraint (int min, int max, String element) {
	this.constraints.add(new DistanceConstraint(min, max, element));
	return this;
    };


    public SpanQuery toQuery () {
	if (this.segments.size() == 0) {
	    return (SpanQuery) null;
	};

	SpanQuery query = this.segments.get(0);

	// NextQueries:
	if (this.constraints == null) {
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
	};

	// MultiDistanceQueries

	return (SpanQuery) null;
    };

    public void setInOrder (boolean isInOrder) {
	this.isInOrder = isInOrder;
    };

    public boolean isInOrder () {
	return this.isInOrder;
    };
};
