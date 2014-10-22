package de.ids_mannheim.korap.query.wrap;

import java.util.*;
import de.ids_mannheim.korap.query.DistanceConstraint;
import de.ids_mannheim.korap.query.SpanElementQuery;
import de.ids_mannheim.korap.query.SpanNextQuery;
import de.ids_mannheim.korap.query.SpanDistanceQuery;
import de.ids_mannheim.korap.query.SpanExpansionQuery;
import de.ids_mannheim.korap.query.SpanMultipleDistanceQuery;

import de.ids_mannheim.korap.query.wrap.SpanQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanSimpleQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanSegmentQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanAlterQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanRegexQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanWildcardQueryWrapper;

import de.ids_mannheim.korap.util.QueryException;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
  TODO:
  Make isNegative work!
  Make isEmpty work!
  Make isExtendedToTheRight work!

  Probably the problemsolving should be done on attribute check
  not on toQuery().
*/


/**
 * Deserialize complexe sequence queries to Lucene SpanQueries.
 *
 * @author Nils Diewald
 * @version 0.02
 */
public class SpanSequenceQueryWrapper extends SpanQueryWrapper {
    private String field;
    private ArrayList<SpanQueryWrapper> segments;
    private ArrayList<DistanceConstraint> constraints;

    // Logger
    private final static Logger log = LoggerFactory.getLogger(SpanSequenceQueryWrapper.class);

    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;

    private boolean isInOrder = true;

    /**
     * Empty constructor.
     */
    public SpanSequenceQueryWrapper (String field) {
	this.field = field;
	this.segments = new ArrayList<SpanQueryWrapper>(2);
    };


    /**
     * Constructor accepting term sequences.
     */
    public SpanSequenceQueryWrapper (String field, String ... terms) {
	this(field);
	for (int i = 0; i < terms.length; i++) {
	    this.segments.add(
                new SpanSimpleQueryWrapper(
                    new SpanTermQuery(new Term(field, terms[i]))
                )
            );
	};
	this.isNull = false;
    };


    /**
     * Constructor accepting SpanQuery sequences.
     */
    public SpanSequenceQueryWrapper (String field, SpanQuery sq) {
	this(field);
	this.segments.add(new SpanSimpleQueryWrapper(sq));
	this.isNull = false;
    };


    /**
     * Constructor accepting SpanQueryWrapper sequences.
     * These wrappers may be optional, negative or empty.
     */
    public SpanSequenceQueryWrapper (String field, SpanQueryWrapper sswq) {
	this(field);

	// Ignore null queries
	if (sswq.isNull())
	    return;

	if (DEBUG && !sswq.isEmpty) {
	    try {
		log.trace("New span sequence {}", sswq.toQuery().toString());
	    }
	    catch (QueryException qe) {
		log.trace("Unable to serialize query {}", qe.getMessage());
	    };
	};

	this.segments.add(sswq);
	this.isNull = false;
    };


    /**
     * Append a term to the sequence.
     */
    public SpanSequenceQueryWrapper append (String term) {
	return this.append(
	    new SpanSimpleQueryWrapper(
                new SpanTermQuery(new Term(field, term))
	    )
        );
    };


    /**
     * Append a SpanQuery to the sequence.
     */
    public SpanSequenceQueryWrapper append (SpanQuery query) {
	return this.append(new SpanSimpleQueryWrapper(query));
    };


    /**
     * Append a SpanQueryWrapper to the sequence.
     */
    public SpanSequenceQueryWrapper append (SpanQueryWrapper ssq) {
	if (ssq.isNull())
	    return this;

	this.isNull = false;
	this.segments.add(ssq);

	return this;
    };

    /**
     * Prepend a term to the sequence.
     */
    public SpanSequenceQueryWrapper prepend (String term) {
	return this.prepend(
            new SpanSimpleQueryWrapper(
	        new SpanTermQuery(new Term(field, term))
	    )
        );
    };


    /**
     * Prepend a SpanQuery to the sequence.
     */
    public SpanSequenceQueryWrapper prepend (SpanQuery query) {
	return this.prepend(
            new SpanSimpleQueryWrapper(query)
        );
    };

    /**
     * Prepend a SpanQueryWrapper to the sequence.
     */
    public SpanSequenceQueryWrapper prepend (SpanQueryWrapper ssq) {
	if (ssq.isNull())
	    return this;

	this.isNull = false;
	this.segments.add(0, ssq);

	return this;
    };


    /**
     * Add a sequence constraint to the sequence for tokens,
     * aka distance constraints.
     */
    public SpanSequenceQueryWrapper withConstraint (int min, int max) {
	return this.withConstraint(min, max, false);
    };


    /**
     * Add a sequence constraint to the sequence for tokens,
     * aka distance constraints (with exclusion).
     */
    public SpanSequenceQueryWrapper withConstraint (int min, int max, boolean exclusion) {
	if (this.constraints == null)
	    this.constraints = new ArrayList<DistanceConstraint>(1);
	this.constraints.add(new DistanceConstraint(min, max, isInOrder, exclusion));
	return this;
    };


    /**
     * Add a sequence constraint to the sequence for various units,
     * aka distance constraints.
     */
    public SpanSequenceQueryWrapper withConstraint (int min, int max, String unit) {
	return this.withConstraint(min, max, unit, false);
    };

    
    /**
     * Add a sequence constraint to the sequence for various units,
     * aka distance constraints (with exclusion).
     */
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

    /**
     * Respect the order of distances.
     */
    public void setInOrder (boolean isInOrder) {
	this.isInOrder = isInOrder;
    };

    /**
     * Check if the order is relevant.
     */
    public boolean isInOrder () {
	return this.isInOrder;
    };

    /**
     * Check if there are constraints defined for the sequence.
     */
    public boolean hasConstraints () {
	if (this.constraints == null)
	    return false;
	if (this.constraints.size() <= 0)
	    return false;
	return true;
    };


    /**
     * Serialize Query to Lucene SpanQueries
     */
    public SpanQuery toQuery () throws QueryException {

	int size = this.segments.size();

	// Nothing to do
	if (size == 0 || this.isNull)
	    return (SpanQuery) null;

	if (size == 1) {
	    if (this.segments.get(0).maybeAnchor())
		return (SpanQuery) this.segments.get(0).toQuery();

	    if (this.segments.get(0).isEmpty())
		throw new QueryException("Sequence is not allowed to be empty");
	    if (this.segments.get(0).isOptional())
		throw new QueryException("Sequence is not allowed to be optional");
	    if (this.segments.get(0).isNegative())
		throw new QueryException("Sequence is not allowed to be negative");
	};

	if (!_solveProblematicSequence(size)) {
	    if (this.segments.get(0).isNegative())
		throw new QueryException("Sequence contains unresolvable "+
					 "empty, optional, or negative segments");
	};

	// Create the initial query
	SpanQuery query = this.segments.get(0).toQuery();

	// NextQueries:
	if (this.constraints == null || this.constraints.size() == 0 ||
	    (this.constraints.size() == 1 &&
	     (this.constraints.get(0).getMinDistance() == 1 &&
	      this.constraints.get(0).getMaxDistance() == 1))) {
	    for (int i = 1; i < this.segments.size(); i++) {
		query = new SpanNextQuery(
	            query,
		    this.segments.get(i).toQuery()
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
			    this.segments.get(i).toQuery(),
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
			this.segments.get(i).toQuery(),
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
		this.segments.get(i).toQuery(),
		this.constraints,
		isInOrder,
		true
	    );
	};
	return (SpanQuery) query;
    };

    /*
      While there is a segment isNegative() or isOptional() or isEmpty() do
      - look for an anchor next to it
      - merge the problematic segment with the anchor
      - go on
    */
    private boolean _solveProblematicSequence (int size) throws QueryException {

	// Check if there is a problematic segment
	SpanQueryWrapper underScrutiny;
	boolean noRemainingProblem = true;
	int i = 0;

	if (DEBUG)
	    log.trace("Try to solve a query of {} segments", size);

	for (; i < size;) {
	    underScrutiny = this.segments.get(i);

	    // Check if there is a problem!
	    if (!underScrutiny.maybeAnchor()) {

		if (DEBUG)
		    log.trace("segment {} is problematic", i);

		// [problem][anchor]
		if (i < (size-1) && this.segments.get(i+1).maybeAnchor()) {

		    // Insert the solution
		    this.segments.set(
		        i+1,
		        _merge(this.segments.get(i+1), underScrutiny, false)
		    );

		    // Remove the problem
		    this.segments.remove(i);
		    size--;

		    if (DEBUG)
			log.trace("Remove segment {} - now size {}", i, size);
		}

		// [anchor][problem]
		else if (i >= 1 && this.segments.get(i-1).maybeAnchor()) {
		    // Insert the solution
		    this.segments.set(
		        i-1,
		        _merge(this.segments.get(i-1), underScrutiny, true)
		    );

		    // Remove the problem
		    this.segments.remove(i);
		    size--;

		    if (DEBUG)
			log.trace("Remove segment {} - now size {}", i, size);
		}
		else {
		    noRemainingProblem = false;
		    i++;
		};
	    }
	    else {
		i++;
	    };
	};

	// There is still a remaining problem
	if (!noRemainingProblem) {

	    // The size has changed - retry!
	    if (size != this.segments.size())
		return _solveProblematicSequence(this.segments.size());
	    else
		return true;
	};

	return false;
    };


    // Todo: Deal with negative and optional!
    // [base=der][base!=Baum]?
    public SpanQueryWrapper _merge (
        SpanQueryWrapper anchor,
	SpanQueryWrapper problem,
	boolean mergeLeft) throws QueryException {

	// Extend to the right - merge to the left
	int direction = 1;
	if (!mergeLeft)
	    direction = -1;

	if (DEBUG)
	    log.trace("Will merge two spans to {}", mergeLeft ? "left" : "right");
	    
	// Make empty extension to anchor
	if (problem.isEmpty()) {
	    SpanQuery query;

	    if (DEBUG)
		log.trace("Problem is empty");

	    if (problem.hasClass) {

		if (DEBUG)
		    log.trace("Problem has class {}", problem.getClassNumber());

		query = new SpanExpansionQuery(
		    anchor.toQuery(),
		    problem.getMin(),
		    problem.getMax(),
		    direction,
		    problem.getClassNumber(),
		    true
		);
	    }
	    else {

		if (DEBUG)
		    log.trace("Problem has no class");

		query = new SpanExpansionQuery(
		    anchor.toQuery(),
		    problem.getMin(),
		    problem.getMax(),
		    direction,
		    true
		);
	    };
	    return new SpanSimpleQueryWrapper(query);
	}

	// make negative extension to anchor
	else if (problem.isNegative()) {

	    if (DEBUG)
		log.trace("Problem is negative");

	    SpanQuery query;
	    if (problem.hasClass) {

		if (DEBUG)
		    log.trace("Problem has class {}", problem.getClassNumber());

		query = new SpanExpansionQuery(
		    anchor.toQuery(),
		    problem.toQuery(),
		    problem.getMin(),
		    problem.getMax(),
		    direction,
		    problem.getClassNumber(),
		    true
		);
	    }
	    else {
		if (DEBUG)
		    log.trace("Problem has no class");

		query = new SpanExpansionQuery(
		    anchor.toQuery(),
		    problem.toQuery(),
		    problem.getMin(),
		    problem.getMax(),
		    direction,
		    true
		);
	    };
	    return new SpanSimpleQueryWrapper(query);
	};

	if (DEBUG)
	    log.trace("Problem is optional");

	// [base=der][base=baum]?

	// [base=der]
	SpanAlterQueryWrapper saqw = new SpanAlterQueryWrapper(this.field, anchor);

	// [base=der]
	SpanSequenceQueryWrapper ssqw = new SpanSequenceQueryWrapper(this.field, anchor);

	// [base=der][base=baum]
	if (mergeLeft)
	    ssqw.append(new SpanSimpleQueryWrapper(problem.toQuery()));
	// [base=baum][base=der]
	else
	    ssqw.prepend(new SpanSimpleQueryWrapper(problem.toQuery()));
	
	saqw.or(ssqw);

	return (SpanQueryWrapper) saqw;
    };
};
