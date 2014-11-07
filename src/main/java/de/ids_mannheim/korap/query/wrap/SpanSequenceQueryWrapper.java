package de.ids_mannheim.korap.query.wrap;

import java.util.*;
import de.ids_mannheim.korap.query.*;
import de.ids_mannheim.korap.query.wrap.*;

import de.ids_mannheim.korap.util.QueryException;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
  TODO:
  Make isNegative work!
  Make isEmpty work!
  Make isExtendedToTheRight work!
*/

/**
 * Deserialize complexe sequence queries to Lucene SpanQueries.
 *
 * @author Nils Diewald
 * @version 0.03
 */
public class SpanSequenceQueryWrapper extends SpanQueryWrapper {
    private String field;
    private ArrayList<SpanQueryWrapper> segments;
    private ArrayList<DistanceConstraint> constraints;

    private final String limitationError =
	"Distance constraints not supported with " +
	"empty or negative operands";

    // Logger
    private final static Logger log = LoggerFactory.getLogger(SpanSequenceQueryWrapper.class);

    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;

    private boolean isInOrder = true;

    // The sequence is problem solved
    private boolean isSolved = false;

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
	/*
	System.err.println("Is negative: ");
	System.err.println(sswq.isNegative());
	*/
	this.segments.add(sswq);
	this.isNull = false;
    };


    /**
     * Append a term to the sequence.
     */
    public SpanSequenceQueryWrapper append (String term) {
	return this.append(new SpanTermQuery(new Term(field, term)));
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

	this.isSolved = false;
	this.isNull = false;

	// Embed a sequence
	if (ssq instanceof SpanSequenceQueryWrapper) {

	    if (DEBUG)
		log.trace("Add SpanSequenceQueryWrapper to sequence");

	    // There are no constraints - just next spans
	    // Flatten!
	    SpanSequenceQueryWrapper ssqw = (SpanSequenceQueryWrapper) ssq;
	    if (!this.hasConstraints() &&
		!ssqw.hasConstraints() &&
		this.isInOrder() == ssqw.isInOrder()) {
		for (int i = 0; i < ssqw.segments.size(); i++) {
		    this.append(ssqw.segments.get(i));
		};
	    }

	    // No flattening
	    else {
		this.segments.add(ssq);
	    };
	}

	// Only one segment
	else {
	    this.segments.add(ssq);
	};

	return this;
    };


    /**
     * Prepend a term to the sequence.
     */
    public SpanSequenceQueryWrapper prepend (String term) {
	return this.prepend(new SpanTermQuery(new Term(field, term)));
    };


    /**
     * Prepend a SpanQuery to the sequence.
     */
    public SpanSequenceQueryWrapper prepend (SpanQuery query) {
	return this.prepend(new SpanSimpleQueryWrapper(query));
    };


    /**
     * Prepend a SpanQueryWrapper to the sequence.
     */
    public SpanSequenceQueryWrapper prepend (SpanQueryWrapper ssq) {
	if (ssq.isNull())
	    return this;

	this.isSolved = false;
	this.isNull = false;

	// Embed a sequence
	if (ssq instanceof SpanSequenceQueryWrapper) {

	    // There are no constraints - just next spans
	    // Flatten!
	    SpanSequenceQueryWrapper ssqw = (SpanSequenceQueryWrapper) ssq;
	    if (!this.hasConstraints() &&
		!ssqw.hasConstraints() &&
		this.isInOrder() == ssqw.isInOrder()) {
		for (int i = ssqw.segments.size() - 1; i >= 0; i--) {
		    this.prepend(ssqw.segments.get(i));
		};
	    }

	    // No flattening
	    else {
		this.segments.add(0, ssq);
	    };
	}

	// Only one segment
	else {
	    this.segments.add(0, ssq);
	};

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

	// Word unit
	if (unit.equals("w"))
	    this.constraints.add(new DistanceConstraint(min, max, isInOrder, exclusion));

	// Element unit (sentence or paragraph)
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

	// The constraint is in fact a next query
	if (this.constraints.size() == 1) {
	    DistanceConstraint dc = this.constraints.get(0);
	    if (dc.getUnit().equals("w") &&
		dc.getMinDistance() == 1 &&
		dc.getMaxDistance() == 1) {
		return false;
	    };
	};

	return true;
    };

    /**
     * Serialize Query to Lucene SpanQueries
     */
    public SpanQuery toQuery () throws QueryException {

	int size = this.segments.size();

	// Nothing to do
	if (size == 0 || this.isNull())
	    return (SpanQuery) null;

	// No real sequence - only one element
	if (size == 1) {

	    // But the element may be expanded
	    if (this.segments.get(0).isExtended() &&
		(this.hasConstraints() || !this.isInOrder())) {
		throw new QueryException(613, limitationError);
	    };

	    // Unproblematic single query
	    if (this.segments.get(0).maybeAnchor())
		return (SpanQuery) this.segments.get(0).toQuery();

	    if (this.segments.get(0).isEmpty())
		throw new QueryException(613, "Sequence is not allowed to be empty");
	    if (this.segments.get(0).isOptional())
		throw new QueryException(613, "Sequence is not allowed to be optional");
	    if (this.segments.get(0).isNegative())
		throw new QueryException(613, "Sequence is not allowed to be negative");
	};

	if (!this.isSolved) {
	    if (!_solveProblematicSequence()) {
		if (this.segments.get(0).maybeExtension())
		    throw new QueryException(
			613,
		        "Sequence contains unresolvable "+
			"empty, optional, or negative segments"
		    );
	    };
	};

	// The element may be expanded
	if (this.segments.size() == 1 &&
	    this.segments.get(0).isExtended() &&
	    (this.hasConstraints() || !this.isInOrder())) {
	    throw new QueryException(613, limitationError);
	};

	// Create the initial query
	SpanQuery query = this.segments.get(0).toQuery();

	// NextQueries:
	if (!this.hasConstraints() && this.isInOrder()) {
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

		    // No support for extended spans in constraints
		    if (this.segments.get(i).isExtended())
			throw new QueryException(613, limitationError);

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

		    // No support for extended spans in constraints
		    if (this.segments.get(i).isExtended())
			throw new QueryException(613, limitationError);

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

	    // No support for extended spans in constraints
	    if (this.segments.get(i).isExtended())
		throw new QueryException(613, limitationError);

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
    private boolean _solveProblematicSequence () {

	int size = this.segments.size();

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
		    if (DEBUG)
			log.trace("Situation is [problem][anchor]");

		    // Insert the solution
		    try {
	 	        this.segments.set(
		            i+1,
		            _merge(this.segments.get(i+1), underScrutiny, false)
		        );
		    }
		    catch (QueryException e) {
			return false;
		    };

		    // Remove the problem
		    this.segments.remove(i);
		    size--;

		    if (DEBUG)
			log.trace("Remove segment {} - now size {}", i, size);

		    // Restart checking
		    i = 0;
		}

		// [anchor][problem]
		else if (i >= 1 && this.segments.get(i-1).maybeAnchor()) {
		    if (DEBUG)
			log.trace("Situation is [anchor][problem]");

		    // Insert the solution
		    try {
			this.segments.set(
			    i-1,
			    _merge(this.segments.get(i-1), underScrutiny, true)
			);
		    }
		    catch (QueryException e) {
			return false;
		    };

		    // Remove the problem
		    this.segments.remove(i);
		    size--;

		    if (DEBUG)
			log.trace("Remove segment {} - now size {}", i, size);

		    // Restart checking
		    i = 0;
		}
		// [problem][problem]
		else {
		    if (DEBUG)
			log.trace("Situation is [problem][problem]");
		    noRemainingProblem = false;
		    i++;
		};
	    }
	    else {
		if (DEBUG)
		    log.trace("segment {} can be an anchor", i);
		i++;
	    };
	};

	// There is still a remaining problem
	if (!noRemainingProblem) {

	    // The size has changed - retry!
	    if (size != this.segments.size())
		return _solveProblematicSequence();

	    this.isSolved = true;
	    return true;
	};

	this.isSolved = true;
	return false;
    };


    // Todo: Deal with negative and optional!
    // [base=der][base!=Baum]?
    private SpanQueryWrapper _merge (
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
	    return new SpanSimpleQueryWrapper(query).isExtended(true);
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
	    return new SpanSimpleQueryWrapper(query).isExtended(true);
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

    public boolean isEmpty () {
	if (this.segments.size() == 0)
	    return this.segments.get(0).isEmpty();
	if (!this.isSolved)
	    _solveProblematicSequence();
	return super.isEmpty();
    };


    public boolean isOptional () {
	if (this.segments.size() == 0)
	    return this.segments.get(0).isOptional();
	if (!this.isSolved)
	    _solveProblematicSequence();
	return super.isOptional();
    };

    public boolean isNegative () {
	if (this.segments.size() == 0)
	    return this.segments.get(0).isNegative();
	if (!this.isSolved)
	    _solveProblematicSequence();
	return super.isNegative();
    };

    public boolean isExtendedToTheRight () {
	if (!this.isSolved) {
	    _solveProblematicSequence();
	};
	return this.isExtendedToTheRight;
    };
};
