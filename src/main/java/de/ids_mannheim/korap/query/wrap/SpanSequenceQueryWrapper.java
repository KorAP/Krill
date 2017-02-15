package de.ids_mannheim.korap.query.wrap;

import java.util.ArrayList;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ids_mannheim.korap.query.DistanceConstraint;
import de.ids_mannheim.korap.query.SpanDistanceQuery;
import de.ids_mannheim.korap.query.SpanElementQuery;
import de.ids_mannheim.korap.query.SpanExpansionQuery;
import de.ids_mannheim.korap.query.SpanMultipleDistanceQuery;
import de.ids_mannheim.korap.query.SpanNextQuery;
import de.ids_mannheim.korap.util.QueryException;

/*
  TODO: Make isNegative work!
  TODO: Evaluate if spanNext(spanNext(a,b),spanNext(c,d)) is faster
        than spanNext(spanNext(spanNext(a,b),c),d)
  TODO: Improve support for SpanElementQueryWrapper in constraints!
*/

/**
 * Deserialize complexe sequence queries to SpanQueries.
 * This will try to make queries work, that by simple nesting won't
 * (like queries with empty sequences), and will optimize queries
 * if possible.
 * 
 * Todo: Synopsis
 * 
 * @author diewald
 */
public class SpanSequenceQueryWrapper extends SpanQueryWrapper {
    private String field;
    private ArrayList<SpanQueryWrapper> segments;
    private ArrayList<DistanceConstraint> constraints;

    private QueryException constraintException = null;

    private final String limitationError = "Distance constraints not supported with "
            + "empty or negative operands";

    // Logger
    private final static Logger log = LoggerFactory
            .getLogger(SpanSequenceQueryWrapper.class);

    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;

    private boolean isInOrder = true;

    // The sequence is problem solved
    private boolean isSolved = false;


    /**
     * Constructs a new object for sequence deserialization.
     * 
     * @param field
     *            The fields the nested SpanQueries should
     *            search in.
     */
    public SpanSequenceQueryWrapper (String field) {
        this.field = field;
        this.segments = new ArrayList<SpanQueryWrapper>(2);
    };


    /**
     * Constructs a new object for sequence deserialization
     * by passing a sequence of terms.
     * 
     * <blockquote><pre>
     * SpanSequenceQueryWrapper ssqw =
     * new SpanSequenceQueryWrapper("tokens", "der", "Baum");
     * System.out.println(ssqw.toQuery());
     * // spanNext(tokens:der, tokens:Baum)
     * </pre></blockquote>
     * 
     * @param field
     *            The fields the nested SpanQueries should
     *            search in.
     * @param terms
     *            [] Arbitrary list of terms to search for.
     */
    public SpanSequenceQueryWrapper (String field, String ... terms) {
        this(field);
        for (int i = 0; i < terms.length; i++) {
            this.segments.add(new SpanSimpleQueryWrapper(
                    new SpanTermQuery(new Term(field, terms[i]))));
        };
        // Query can't be null anymore
        this.isNull = false;
    };


    /**
     * Constructs a new object for sequence deserialization
     * by passing a single {@link SpanQuery} object.
     * 
     * @param query
     *            Initial {@link SpanQuery} to search for.
     */
    public SpanSequenceQueryWrapper (SpanQuery query) {
        this(query.getField());
        this.segments.add(new SpanSimpleQueryWrapper(query));
        this.isNull = false;
    };


    /**
     * Constructs a new object for sequence deserialization
     * by passing a single {@link SpanQueryWrapper} object.
     * These wrapper queries may be optional, negative, or empty.
     * 
     * @param query
     *            Initial {@link SpanQueryWrapper} to search for.
     */
    public SpanSequenceQueryWrapper (String field, SpanQueryWrapper sswq) {
        this(field);

        // Ignore null queries
        if (sswq.isNull())
            return;

        if (sswq.maybeUnsorted())
            this.maybeUnsorted = true;

        // Some debugging on initiating new sequences
        if (DEBUG) {
            if (!sswq.isEmpty()) {
                try {
                    log.trace("New span sequence {}",
                            sswq.toFragmentQuery().toString());
                }
                catch (QueryException qe) {
                    log.trace("Unable to serialize query {}", qe.getMessage());
                };
            }
            else {
                log.trace("New span sequence, that's initially empty");
            };
        };

        this.segments.add(sswq);
        this.isNull = false;
    };



    /**
     * Append a new term to the sequence.
     * 
     * <blockquote><pre>
     * SpanSequenceQueryWrapper ssqw =
     * new SpanSequenceQueryWrapper("tokens");
     * ssqw.append("der").append("Baum");
     * System.out.println(ssqw.toQuery());
     * // spanNext(tokens:der, tokens:Baum)
     * </pre></blockquote>
     * 
     * @param term
     *            A new string to search for.
     * @return The {@link SpanSequenceQueryWrapper} object for
     *         chaining.
     */
    public SpanSequenceQueryWrapper append (String term) {
        return this.append(new SpanTermQuery(new Term(field, term)));
    };


    /**
     * Append a new {@link SpanQuery} object to the sequence.
     * 
     * @param query
     *            A new {@link SpanQuery} to search for.
     * @return The {@link SpanSequenceQueryWrapper} object for
     *         chaining.
     */
    public SpanSequenceQueryWrapper append (SpanQuery query) {
        return this.append(new SpanSimpleQueryWrapper(query));
    };


    /**
     * Append a new {@link SpanQueryWrapper} object to the sequence.
     * 
     * @param query
     *            A new {@link SpanQueryWrapper} to search for.
     * @return The {@link SpanSequenceQueryWrapper} object for
     *         chaining.
     */
    public SpanSequenceQueryWrapper append (SpanQueryWrapper ssq) {

        // The wrapper is null - ignore this in the sequence
        if (ssq.isNull())
            return this;

        if (ssq.maybeUnsorted())
            this.maybeUnsorted = true;

        // As the spanQueryWrapper is not null,
        // the sequence can't be null as well
        this.isNull = false;

        // The sequence may be problematic
        this.isSolved = false;

        // Embed a nested sequence
        if (ssq instanceof SpanSequenceQueryWrapper) {

            if (DEBUG)
                log.trace("Add SpanSequenceQueryWrapper to sequence");

            // Some casting
            SpanSequenceQueryWrapper ssqw = (SpanSequenceQueryWrapper) ssq;

            // There are no constraints and the order is equal - Flatten!
            if (!this.hasConstraints() && !ssqw.hasConstraints()
                    && this.isInOrder() == ssqw.isInOrder()) {
                for (int i = 0; i < ssqw.segments.size(); i++) {
                    this.append(ssqw.segments.get(i));
                };
            }

            // Unable to flatten ... :-(
            else {
                this.segments.add(ssq);
            };
        }

        // This is not a sequence
        else {
            this.segments.add(ssq);
        };

        return this;
    };


    /**
     * Prepend a new term to the sequence.
     * 
     * <blockquote><pre>
     * SpanSequenceQueryWrapper ssqw =
     * new SpanSequenceQueryWrapper("tokens", "Baum");
     * ssqw.prepend("der");
     * System.out.println(ssqw.toQuery());
     * // spanNext(tokens:der, tokens:Baum)
     * </pre></blockquote>
     * 
     * @param term
     *            A new string to search for.
     * @return The {@link SpanSequenceQueryWrapper} object for
     *         chaining.
     */
    public SpanSequenceQueryWrapper prepend (String term) {
        return this.prepend(new SpanTermQuery(new Term(field, term)));
    };


    /**
     * Prepend a new {@link SpanQuery} object to the sequence.
     * 
     * @param query
     *            A new {@link SpanQuery} to search for.
     * @return The {@link SpanSequenceQueryWrapper} object for
     *         chaining.
     */
    public SpanSequenceQueryWrapper prepend (SpanQuery query) {
        return this.prepend(new SpanSimpleQueryWrapper(query));
    };


    /**
     * Prepend a new {@link SpanQueryWrapper} object to the sequence.
     * 
     * @param query
     *            A new {@link SpanQueryWrapper} to search for.
     * @return The {@link SpanSequenceQueryWrapper} object for
     *         chaining.
     */
    public SpanSequenceQueryWrapper prepend (SpanQueryWrapper ssq) {

        // The wrapper is null - ignore this in the sequence
        if (ssq.isNull())
            return this;

        // As the spanQueryWrapper is not null,
        // the sequence can't be null as well
        this.isNull = false;

        // The sequence may be problematic
        this.isSolved = false;

        if (ssq.maybeUnsorted())
            this.maybeUnsorted = true;

        // Embed a nested sequence
        if (ssq instanceof SpanSequenceQueryWrapper) {

            // There are no constraints and the order is equal - Flatten!
            SpanSequenceQueryWrapper ssqw = (SpanSequenceQueryWrapper) ssq;
            if (!this.hasConstraints() && !ssqw.hasConstraints()
                    && this.isInOrder() == ssqw.isInOrder()) {
                for (int i = ssqw.segments.size() - 1; i >= 0; i--) {
                    this.prepend(ssqw.segments.get(i));
                };
            }

            // Unable to flatten ... :-(
            else {
                this.segments.add(0, ssq);
            };
        }

        // This is not a sequence
        else {
            this.segments.add(0, ssq);
        };

        return this;
    };


    /**
     * Add a token based sequence constraint (aka distance constraint)
     * to the sequence.
     * 
     * Multiple constraints are supported.
     * 
     * A minimum value of zero means, there may be an overlap,
     * a minimum value of 1 means, there is no token between the
     * spans.
     * It's weird - we know and dislike that. That's why we have to
     * say:
     * 
     * <strong>Warning!</strong> Sequence constraints are experimental
     * and may (hopefully) change in future versions!
     * 
     * @param min
     *            The minimum number of tokens between the elements
     *            of the sequence.
     * @param max
     *            The minimum number of tokens between the elements
     *            of the sequence.
     * @return The {@link SpanSequenceQueryWrapper} object for
     *         chaining.
     * @see DistanceConstraint
     */
    public SpanSequenceQueryWrapper withConstraint (int min, int max) {
        return this.withConstraint(min, max, false);
    };


    /**
     * Add a token based sequence constraint (aka distance constraint)
     * to the sequence with an exclusion constraint, meaning
     * the constraint is fine in case the operands are <em>not</em>
     * within the distance.
     * 
     * Multiple constraints are supported.
     * 
     * A minimum value of zero means, there may be an overlap,
     * a minimum value of 1 means, there is no token between the
     * spans.
     * It's weird - we know and dislike that. That's why we have to
     * say:
     * 
     * <strong>Warning!</strong> Sequence constraints are experimental
     * and may (hopefully) change in future versions!
     * 
     * @param min
     *            The minimum number of tokens between the elements
     *            of the sequence.
     * @param max
     *            The minimum number of tokens between the elements
     *            of the sequence.
     * @param exclusion
     *            Boolean value indicating, the distance constraint
     *            has to fail.
     * @return The {@link SpanSequenceQueryWrapper} object for
     *         chaining.
     * @see DistanceConstraint
     */
    public SpanSequenceQueryWrapper withConstraint (int min, int max,
            boolean exclusion) {
        if (this.constraints == null)
            this.constraints = new ArrayList<DistanceConstraint>(1);
        if (DEBUG)
            log.trace("With contraint {}-{} (excl {})", min, max, exclusion);
        this.constraints.add(
                new DistanceConstraint(min, max, this.isInOrder, exclusion));
        return this;
    };


    /**
     * Add a sequence constraint (aka distance constraint)
     * to the sequence based on a certain unit.
     * The unit has to be a valid {@link SpanElementQuery} term
     * or <tt>w</tt> for tokens.
     * 
     * Multiple constraints are supported.
     * 
     * A minimum value of zero means, there may be an overlap,
     * a minimum value of 1 means, there is no token between the
     * spans.
     * It's weird - we know and dislike that. That's why we have to
     * say:
     * 
     * <strong>Warning!</strong> Sequence constraints are experimental
     * and
     * may (hopefully) change in future versions!
     * 
     * @param min
     *            The minimum number of tokens between the elements
     *            of the sequence.
     * @param max
     *            The minimum number of tokens between the elements
     *            of the sequence.
     * @param unit
     *            Unit for distance - will be evaluated to a
     *            {@link SpanElementQuery}.
     * @return The {@link SpanSequenceQueryWrapper} object for
     *         chaining.
     * @see DistanceConstraint
     */
    public SpanSequenceQueryWrapper withConstraint (int min, int max,
            String unit) {
        return this.withConstraint(min, max, unit, false);
    };



    /**
     * Add a sequence constraint (aka distance constraint)
     * to the sequence based on a certain unit and with an
     * exclusion constraint, meaning the constraint is fine
     * in case the operands are <em>not</em> within the distance.
     * The unit has to be a valid {@link SpanElementQuery} term
     * or <tt>w</tt> for tokens.
     * 
     * Multiple constraints are supported.
     * 
     * A minimum value of zero means, there may be an overlap,
     * a minimum value of 1 means, there is no token between the
     * spans.
     * It's weird - we know and dislike that. That's why we have to
     * say:
     * 
     * <strong>Warning!</strong> Sequence constraints are experimental
     * and
     * may (hopefully) change in future versions!
     * 
     * @param min
     *            The minimum number of tokens between the elements
     *            of the sequence.
     * @param max
     *            The minimum number of tokens between the elements
     *            of the sequence.
     * @param unit
     *            Unit for distance - will be evaluated to a
     *            {@link SpanElementQuery}.
     * @param exclusion
     *            Boolean value indicating, the distance constraint
     *            has to fail.
     * @return The {@link SpanSequenceQueryWrapper} object for
     *         chaining.
     * @see DistanceConstraint
     */
    public SpanSequenceQueryWrapper withConstraint (int min, int max,
            String unit, boolean exclusion) {

        if (DEBUG)
            log.trace("With contraint {}-{} (unit {}, excl {})", min, max, unit,
                    exclusion);

        // Word unit
        if (unit.equals("w")) {

            // Initialize constraint
            if (this.constraints == null)
                this.constraints = new ArrayList<DistanceConstraint>(1);

            this.constraints.add(
                    new DistanceConstraint(min, max, isInOrder, exclusion));

            return this;
        };

        // Element unit (sentence or paragraph)
        return this.withConstraint(min, max,
                new SpanElementQueryWrapper(this.field, unit), exclusion);
    };


    /**
     * Add a sequence constraint (aka distance constraint)
     * to the sequence based on a certain unit and with an
     * exclusion constraint, meaning the constraint is fine
     * in case the operands are <em>not</em> within the distance.
     * 
     * Multiple constraints are supported.
     * 
     * A minimum value of zero means, there may be an overlap,
     * a minimum value of 1 means, there is no token between the
     * spans.
     * It's weird - we know and dislike that. That's why we have to
     * say:
     * 
     * <strong>Warning!</strong> Sequence constraints are experimental
     * and
     * may (hopefully) change in future versions!
     * 
     * @param min
     *            The minimum number of tokens between the elements
     *            of the sequence.
     * @param max
     *            The minimum number of tokens between the elements
     *            of the sequence.
     * @param unit
     *            A {@link SpanElementQueryWrapper} as the unit for
     *            distance.
     * @param exclusion
     *            Boolean value indicating, the distance constraint
     *            has to fail.
     * @return The {@link SpanSequenceQueryWrapper} object for
     *         chaining.
     * @see DistanceConstraint
     */
    public SpanSequenceQueryWrapper withConstraint (int min, int max,
            SpanElementQueryWrapper unit, boolean exclusion) {

        if (DEBUG)
            log.trace("With contraint {}-{} (unit {}, excl {})", min, max,
                    unit.toString(), exclusion);

        if (this.constraints == null)
            this.constraints = new ArrayList<DistanceConstraint>(1);

        // Element unit (sentence or paragraph)
        // Todo: This should possibly be evaluated to a query later on!
        try {
            this.constraints.add(new DistanceConstraint(
                    (SpanElementQuery) unit.retrieveNode(this.retrieveNode)
                            .toFragmentQuery(),
                    min, max, isInOrder, exclusion));
        }
        catch (QueryException qe) {
            this.constraintException = qe;
        };
        return this;
    };


    /**
     * Check if the sequence has to be in order.
     * 
     * @return <tt>true</tt> in case the sequence
     *         has to be in order and <tt>false</tt>
     *         in case the order is not relevant.
     */
    public boolean isInOrder () {
        return this.isInOrder;
    };


    /**
     * Set the boolean value indicating if the sequence
     * has to be in order.
     * 
     * @param order
     *            <tt>true</tt> in case the sequence
     *            has to be in order and <tt>false</tt>
     *            in case the order is not relevant.
     */
    public void setInOrder (boolean order) {
        this.isInOrder = order;
    };


    /**
     * Check if the sequence has constraints.
     * 
     * @return <tt>true</tt> in case the sequence
     *         has any constraints and <tt>false</tt>
     *         in case it is a simple next query.
     */
    public boolean hasConstraints () {
        if (this.constraints == null)
            return false;

        if (this.constraints.size() <= 0)
            return false;

        // The constraint is in fact a next query,
        // that will be optimized away later on
        if (this.constraints.size() == 1) {
            DistanceConstraint dc = this.constraints.get(0);
            if (dc.getUnit().equals("w") && dc.getMinDistance() == 1
                    && dc.getMaxDistance() == 1 && this.isInOrder) {
                return false;
            };
        };

        return true;
    };


    public boolean isEmpty () {

        if (this.segments.size() == 1)
            return this.segments.get(0).isEmpty();

        if (!this.isSolved)
            _solveProblematicSequence();
        return super.isEmpty();
    };


    public boolean isOptional () {
        if (this.segments.size() == 1)
            return this.segments.get(0).isOptional();
        if (!this.isSolved)
            _solveProblematicSequence();
        return super.isOptional();
    };


    public boolean isNegative () {
        if (this.segments.size() == 1)
            return this.segments.get(0).isNegative();
        if (!this.isSolved)
            _solveProblematicSequence();
        return super.isNegative();
    };


    public boolean isExtendedToTheRight () {
        if (!this.isSolved)
            _solveProblematicSequence();

        int size = this.segments.size();

        // Nothing to do
        if (size == 0 || this.isNull())
            return false;

        // Return the value of the last segment
        return this.segments.get(size - 1).isExtendedToTheRight();
    };


    /**
     * Serialize the wrapped sequence to a {@link SpanQuery} object.
     * 
     * @return A {@link SpanQuery} object.
     * @throws QueryException
     */
    public SpanQuery toFragmentQuery () throws QueryException {

        // There was a serialization failure not yet reported
        if (this.constraintException != null)
            throw constraintException;

        int size = this.segments.size();

        // Nothing to do
        if (size == 0 || this.isNull())
            return (SpanQuery) null;

        // No real sequence - only one element
        if (size == 1) {
            // But the element may be expanded
            if (this.segments.get(0).isExtended()
                    && (this.hasConstraints() || !this.isInOrder())) {
                throw new QueryException(613, limitationError);
            };

            // Unproblematic single query
            if (this.segments.get(0).maybeAnchor())
                return (SpanQuery) this.segments.get(0)
                        .retrieveNode(this.retrieveNode).toFragmentQuery();

            if (this.segments.get(0).isEmpty())
                throw new QueryException(613,
                        "Sequence is not allowed to be empty");

            if (this.segments.get(0).isOptional())
                throw new QueryException(613,
                        "Sequence is not allowed to be optional");

            if (this.segments.get(0).isNegative())
                throw new QueryException(613,
                        "Sequence is not allowed to be negative");
        };

        if (!this.isSolved) {
            if (!_solveProblematicSequence()) {
                if (this.segments.get(0).maybeExtension()) {
                    throw new QueryException(613,
                            "Sequence contains unresolvable "
                                    + "empty, optional, or negative segments");
                };
            };
        };

        // The element may be expanded
        if (this.segments.size() == 1 && this.segments.get(0).isExtended()
                && (this.hasConstraints() || !this.isInOrder())) {
            throw new QueryException(613, limitationError);
        };

        // Create the initial query
        SpanQuery query = null;
        int i = 0;

		// Get the first valid segment
        while (query == null && i < this.segments.size()) {
            query = this.segments.get(i).retrieveNode(this.retrieveNode)
				.toFragmentQuery();
            i++;
        };

		// No valid segment found
        if (query == null)
            return (SpanQuery) null;
	
        // NextQueries
        if (!this.hasConstraints() && this.isInOrder()) {

			// TODO: Optimize:
			// Join identicals to repetition, so spanNext(a,a) -> a{2}
			
            for (; i < this.segments.size(); i++) {

				// Get the first query for next sequence
                SpanQuery second = this.segments.get(i)
					.retrieveNode(this.retrieveNode).toFragmentQuery();
                if (second == null)
                    continue;

                query = new SpanNextQuery(query, second);
            };
            return (SpanQuery) query;
        };

        // DistanceQueries with problems
        if (this.hasConstraints() && this.isProblematic) {
            throw new QueryException(613,
                    "Distance constraints not supported with empty, optional or negative operands");
        };

        // DistanceQueries
        if (this.constraints.size() == 1) {
            DistanceConstraint constraint = this.constraints.get(0);

            // Create spanElementDistance query
            if (!constraint.getUnit().equals("w")) {
                for (i = 1; i < this.segments.size(); i++) {

                    // No support for extended spans in constraints
                    if (this.segments.get(i).isExtended())
                        throw new QueryException(613, limitationError);

                    /* Maybe important
                    if (this.segments.get(i).isOptional())
                        throw new QueryException(613, limitationError);
                    */

                    SpanQuery sq = (SpanQuery) this.segments.get(i)
                            .retrieveNode(this.retrieveNode).toFragmentQuery();
                    if (sq == null)
                        continue;

                    SpanDistanceQuery sdquery = new SpanDistanceQuery(query, sq,
                            constraint, true);
                    query = (SpanQuery) sdquery;
                };
            }

            // Create spanDistance query
            else {
                for (i = 1; i < this.segments.size(); i++) {

                    // No support for extended spans in constraints
                    if (this.segments.get(i).isExtended())
                        throw new QueryException(613, limitationError);

                    /* May be necessary
                    if (this.segments.get(i).isOptional())
                        throw new QueryException(613, limitationError);
                    */

                    SpanQuery sq = (SpanQuery) this.segments.get(i)
                            .retrieveNode(this.retrieveNode).toFragmentQuery();
                    if (sq == null)
                        continue;

                    SpanDistanceQuery sdquery = new SpanDistanceQuery(query, sq,
                            constraint, true);
                    query = (SpanQuery) sdquery;
                };
            };

            return (SpanQuery) query;
        };

        // MultipleDistanceQueries
        for (i = 1; i < this.segments.size(); i++) {

            // No support for extended spans in constraints
            if (this.segments.get(i).isExtended())
                throw new QueryException(613, limitationError);

            SpanQuery sq = (SpanQuery) this.segments.get(i)
                    .retrieveNode(this.retrieveNode).toFragmentQuery();
            if (sq == null)
                continue;

            query = new SpanMultipleDistanceQuery(query, sq, this.constraints,
                    isInOrder, true);
        };
        return (SpanQuery) query;
    };


    /*
      Check if there are problematic segments in the sequence
      (either negative, optional or empty) and deal with them
      (make optional segments to or-queries and negative and empty
      segments to extensions).
      This has to be done as long as there are problematic segments
      In the queries.
    
      While there is a segment isNegative() or isOptional() or isEmpty() do
      - look for an anchor next to it
      - merge the problematic segment with the anchor
      - go on
    
      - This does not work for distance constraints!
    */
    private boolean _solveProblematicSequence () {
        int size = this.segments.size();
        // Check if there is a problematic segment
        SpanQueryWrapper underScrutiny;
        boolean noRemainingProblem = true;
        int i = 0;

        if (DEBUG)
            log.trace("Try to solve a query of {} segments", size);

        // Iterate over all segments
        for (; i < size;) {
            underScrutiny = this.segments.get(i);

            // Check if there is a problem with the current segment
            if (!underScrutiny.maybeAnchor()) {

                if (DEBUG)
                    log.trace("segment {} is problematic", i);

                // Sequences with distance constraints do not support empty or optional
                // elemets
                if (this.hasConstraints()) {
                    if (DEBUG) {
                        log.trace("Sequence has constraints, "
                                + "that do not allow empty or optional elements");
                    };
                    this.isSolved = true;
                    this.isProblematic = true;
                    return false;
                };

                // [problem][anchor]
                if (i < (size - 1) && this.segments.get(i + 1).maybeAnchor()) {
                    if (DEBUG)
                        log.trace("Situation is [problem][anchor]");

                    // Insert the solution
                    try {
                        this.segments.set(i + 1,
                                _merge(this.segments.get(i + 1), underScrutiny,
                                        false));
                    }

                    // An error occurred while solving the problem
                    catch (QueryException e) {
                        this.isSolved = true;
                        this.isProblematic = true;
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
                else if (i >= 1 && this.segments.get(i - 1).maybeAnchor()) {
                    if (DEBUG)
                        log.trace("Situation is [anchor][problem]");

                    // Insert the solution
                    try {
                        this.segments.set(i - 1, _merge(
                                this.segments.get(i - 1), underScrutiny, true));
                    }
                    catch (QueryException e) {
                        this.isSolved = true;
                        this.isProblematic = true;
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
            this.isProblematic = true;
            return false; // true;
        };

        this.isSolved = true;
        this.isProblematic = false;
        return false;
    };


    // Todo: Deal with negative, empty and optional!
    // [base=der][base!=Baum]?
    private SpanQueryWrapper _merge (SpanQueryWrapper anchor,
            SpanQueryWrapper problem, boolean mergeLeft) throws QueryException {

        // Extend to the right - merge to the left
        int direction = mergeLeft ? 1 : -1;

        if (DEBUG)
            log.trace("Will merge two spans to {}",
                    mergeLeft ? "left" : "right");

        // Make empty extension to anchor
        if (problem.isEmpty()) {
            SpanQuery query;

            if (DEBUG)
                log.trace("Problem is empty with class {}",
                        problem.getClassNumber());

            // Merge extensions!
            if (!problem.hasClass() && !anchor.hasClass()
                    && anchor.isExtended()) {

                if (DEBUG)
                    log.trace(
                            "It may be possible to extend anchor with problem");

                if (
                // Further extend to the right ...
                (direction >= 0 && anchor.isExtendedToTheRight) ||

                // or the left
                        (direction < 0 && !anchor.isExtendedToTheRight)) {

                    if (DEBUG)
                        log.trace("Readjust min and max");

                    // Readjust the anchor
                    anchor.setMin(anchor.getMin() + problem.getMin());
                    anchor.setMax(anchor.getMax() + problem.getMax());

                    /*
                     * This is wrong - min is only relevant for extensions
                    if (anchor.getMin() > 0)
                    	anchor.isOptional = false;
                    */
                    return anchor;
                };
            };

            // Can't merge extensions
            SpanQueryWrapper sqw = new SpanExpansionQueryWrapper(anchor,
                    problem.isOptional() ? 0 : problem.getMin(),
                    problem.getMax(), direction,
                    problem.hasClass() ? problem.getClassNumber() : (byte) 0)
                            .isExtended(true);

            // Set right extension
            if (direction >= 0)
                sqw.isExtendedToTheRight(true);

            return sqw;
        }

        // make negative extension to anchor
        else if (problem.isNegative()) {

            SpanQuery query;

            if (DEBUG)
                log.trace("Problem is negative with class {}",
                        problem.getClassNumber());

            // TODO: Should probably wrapped as well!
            // A sequence of negative tokens may expand jointly!
            query = new SpanExpansionQuery(
                    anchor.retrieveNode(this.retrieveNode).toFragmentQuery(),
                    problem.retrieveNode(this.retrieveNode).toFragmentQuery(),
                    problem.getMin(), problem.getMax(), direction,
                    problem.hasClass() ? problem.getClassNumber() : (byte) 0,
                    true);

            SpanQueryWrapper sqw = new SpanSimpleQueryWrapper(query)
                    .isExtended(true);

            // Set right extension
            if (direction >= 0)
                sqw.isExtendedToTheRight(true);

            return sqw;
        };

        if (DEBUG)
            log.trace("Problem is optional");

        // [base=der][][base=Baum]?
        // [base=der][base=baum]?

        // [base=der]
        SpanAlterQueryWrapper saqw = new SpanAlterQueryWrapper(this.field,
                anchor);

        // [base=der]
        SpanSequenceQueryWrapper ssqw = new SpanSequenceQueryWrapper(this.field,
                anchor);

        // [base=der][base=baum]
        if (mergeLeft) {
            ssqw.append(problem.isOptional(false));
        }
        // [base=baum][base=der]
        else {
            ssqw.prepend(problem.isOptional(false));
        }

        saqw.or(ssqw);

        return (SpanQueryWrapper) saqw;
    };
};
