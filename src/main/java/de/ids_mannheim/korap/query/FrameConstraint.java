package de.ids_mannheim.korap.query;

import de.ids_mannheim.korap.util.QueryException;
import org.apache.lucene.search.spans.Spans;

import java.util.*;

/**
 * Class representing a frame constraint, as used by
 * {@link SpanWithinQuery}.
 * Multiple constraints are represented as a bit vector,
 * supporting fast checks if a certain condition is met.
 * The following distinctive frame conditions are supported:
 * 
 * <dl>
 * <dt>precedes</dt>
 * <dd>A precedes B</dd>
 * 
 * <dt>precedesDirectly</dt>
 * <dd>A precedes B directly</dd>
 * 
 * <dt>overlapsLeft</dt>
 * <dd>A overlaps B to the left</dd>
 * 
 * <dt>alignsLeft</dt>
 * <dd>A aligns with B to the left</dd>
 * 
 * <dt>startsWith</dt>
 * <dd>A starts with B</dd>
 * 
 * <dt>matches</dt>
 * <dd>A matches B</dd>
 * 
 * <dt>isWithin</dt>
 * <dd>A is within B</dd>
 * 
 * <dt>isAround</dt>
 * <dd>A is around B</dd>
 * 
 * <dt>endsWith</dt>
 * <dd>A ends with B</dd>
 * 
 * <dt>alignsRight</dt>
 * <dd>A aligns with B to the right</dd>
 * 
 * <dt>overlapsRight</dt>
 * <dd>A overlaps B to the right</dd>
 * 
 * <dt>succeedsDirectly</dt>
 * <dd>A succeeds B directly</dd>
 * 
 * <dt>succeeds</dt>
 * <dd>A succeeds B</dd>
 * </dl>
 * 
 * @author diewald
 */
public class FrameConstraint {

    public static final int PRECEDES = 1, PRECEDES_DIRECTLY = 1 << 1,
            OVERLAPS_LEFT = 1 << 2, ALIGNS_LEFT = 1 << 3, STARTS_WITH = 1 << 4,
            MATCHES = 1 << 5, IS_WITHIN = 1 << 6, IS_AROUND = 1 << 7,
            ENDS_WITH = 1 << 8, ALIGNS_RIGHT = 1 << 9,
            OVERLAPS_RIGHT = 1 << 10, SUCCEEDS_DIRECTLY = 1 << 11,
            SUCCEEDS = 1 << 12, ALL = 1024 * 8 - 1;


    private static final Map<String, Integer> FRAME;
    private static final List<Integer> NEXT_B;

    static {
        Map<String, Integer> FRAME_t = new HashMap<>();
        List<Integer> NEXT_B_t = new ArrayList(16);

        /*
         * A precedes B
         *
         * |-A-|
         *       |-B-|
         *
         * A.end < B.start
         */
        FRAME_t.put("precedes", PRECEDES);
        NEXT_B_t.add(PRECEDES);
        /*
         * A precedes B directly
         *
         * |-A-|
         *     |-B-|
         *
         * A.end == b.start
         */
        FRAME_t.put("precedesDirectly", PRECEDES_DIRECTLY);
        NEXT_B_t.add(PRECEDES | PRECEDES_DIRECTLY);

        /*
         * A overlaps B to the left
         *
         * |-A-|
         *    |-B-|
         *
         * a.end < b.end && a.end > b.start && a.start < b.start
         */
        FRAME_t.put("overlapsLeft", OVERLAPS_LEFT);
        NEXT_B_t.add(PRECEDES | PRECEDES_DIRECTLY | OVERLAPS_LEFT);

        /*
         * A aligns B to the left
         *
         * |-A-|
         * |-B--|
         *
         * a.end < b.end && a.start == b.start
         */
        FRAME_t.put("alignsLeft", ALIGNS_LEFT);
        NEXT_B_t.add(PRECEDES | PRECEDES_DIRECTLY | OVERLAPS_LEFT | ALIGNS_LEFT);

        /*
         * A starts with B
         *
         * |-A--|
         * |-B-|
         *
         * a.end > b.end && a.start == b.start
         */
        FRAME_t.put("startsWith", STARTS_WITH);
        NEXT_B_t.add(PRECEDES | PRECEDES_DIRECTLY | OVERLAPS_LEFT | ALIGNS_LEFT
                | STARTS_WITH | MATCHES | IS_AROUND | ENDS_WITH);

        /*
         * A matches B
         *
         * |-A-|
         * |-B-|
         *
         * a.end = b.end && a.start = b.start
         */
        FRAME_t.put("matches", MATCHES);
        NEXT_B_t.add(PRECEDES | PRECEDES_DIRECTLY | OVERLAPS_LEFT | ALIGNS_LEFT
                | MATCHES | ENDS_WITH);

        /*
         * A is within B
         *
         *  |-A-|
         * |--B--|
         *
         * a.end < b.end && a.start > b.start
         */
        FRAME_t.put("isWithin", IS_WITHIN);
        NEXT_B_t.add(PRECEDES | PRECEDES_DIRECTLY | ALIGNS_LEFT | IS_WITHIN);

        /*
         * A is around B
         *
         * |--A--|
         *  |-B-|
         *
         * a.start < b.start && a.end > b.end
         */
        FRAME_t.put("isAround", IS_AROUND);
        NEXT_B_t.add(PRECEDES | PRECEDES_DIRECTLY | OVERLAPS_LEFT | IS_AROUND
                | ENDS_WITH);

        /*
         * A ends with B
         *
         * |-A--|
         *  |-B-|
         *
         * a.start < b.start && a.end == b.end
         */
        FRAME_t.put("endsWith", ENDS_WITH);
        NEXT_B_t.add(PRECEDES | PRECEDES_DIRECTLY | OVERLAPS_LEFT | ENDS_WITH);

        /*
         * A aligns B to the right
         *
         *  |-A-|
         * |--B-|
         *
         * a.start > b.start && a.end == b.end
         */
        FRAME_t.put("alignsRight", ALIGNS_RIGHT);
        NEXT_B_t.add(PRECEDES | PRECEDES_DIRECTLY | ALIGNS_LEFT | MATCHES
                | IS_WITHIN | ALIGNS_RIGHT);

        /*
         * A overlaps B to the right
         *
         *  |-A-|
         * |-B-|
         *
         * a.start > b.start && a.start < b.end && a.end > b.end
         */
        FRAME_t.put("overlapsRight", OVERLAPS_RIGHT);
        NEXT_B_t.add(PRECEDES | PRECEDES_DIRECTLY | OVERLAPS_LEFT | ALIGNS_LEFT
                | STARTS_WITH | MATCHES | IS_WITHIN | IS_AROUND | ENDS_WITH
                | ALIGNS_RIGHT | OVERLAPS_RIGHT);

        /*
         * A succeeds B directly
         *
         *     |-A-|
         * |-B-|
         *
         * a.start == b.end
         */
        FRAME_t.put("succeedsDirectly", SUCCEEDS_DIRECTLY);
        NEXT_B_t.add(PRECEDES | PRECEDES_DIRECTLY | OVERLAPS_LEFT | ALIGNS_LEFT
                | STARTS_WITH | MATCHES | IS_WITHIN | IS_AROUND | ENDS_WITH
                | ALIGNS_RIGHT | OVERLAPS_RIGHT | SUCCEEDS_DIRECTLY);

        /*
         * A succeeds B
         *
         *       |-A-|
         * |-B-|
         *
         * a.start > b.end
         */
        FRAME_t.put("succeeds", SUCCEEDS);
        NEXT_B_t.add(PRECEDES | PRECEDES_DIRECTLY | ALIGNS_LEFT | STARTS_WITH
                | MATCHES | IS_WITHIN | ALIGNS_RIGHT | SUCCEEDS_DIRECTLY
                | SUCCEEDS);

        FRAME = Collections.unmodifiableMap(FRAME_t);
        NEXT_B = Collections.unmodifiableList(NEXT_B_t);
    };

    // Bitvector representing the frame constraint
    public int vector;


    /**
     * Constructs a new Frame Constraint.
     */
    public FrameConstraint () {
        this.vector = 0;
    };


    /**
     * Add a new valid condition to the Frame Constraint.
     * 
     * @param condition
     *            A string representing a valid condition.
     *            See the synopsis for valid condition names.
     * @return The {@link FrameConstraint} for chaining.
     * @throws QueryException
     */
    public FrameConstraint add (String condition) throws QueryException {
        int or = FRAME.get(condition);
        if (or == 0)
            throw new QueryException(706, "Frame type is unknown");

        this.vector |= or;
        return this;
    };


    /**
     * Add new valid conditions to the Frame Constraint.
     * 
     * @param constraint
     *            A Frame constraint representing a set
     *            of valid conditions.
     * @return The {@link FrameConstraint} for chaining.
     */
    public FrameConstraint add (FrameConstraint constraint) {
        this.vector |= constraint.vector;
        return this;
    };


    /**
     * Invert the condition set of the frame constraint.
     * All valid conditions become invalid, all invalid
     * conditions become valid.
     * 
     * @return The {@link FrameConstraint} for chaining.
     */
    public FrameConstraint invert () {
        this.vector ^= ALL;
        return this;
    };


    /**
     * Check if a condition is valid.
     * 
     * @param condition
     *            A string representing a condition.
     *            See the synopsis for valid condition names.
     * @return A boolean value, indicating if a condition is valid or
     *         not.
     * @throws QueryException
     */
    public boolean check (String condition) throws QueryException {
        int check = FRAME.get(condition);

        if (check == 0)
            throw new QueryException(706, "Frame type is unknown");

        return this.check(check);
    };


    /**
     * Check if conditions are valid.
     * 
     * @param conditions
     *            An integer bit vector representing a set of
     *            conditions.
     * @return A boolean value, indicating if at least one condition
     *         is valid or not.
     */
    public boolean check (int conditions) {
        return (this.vector & conditions) != 0;
    };


    /**
     * Check if conditions are valid.
     * 
     * @param conditions
     *            A {@link FrameConstraint} representing a set of
     *            conditions.
     * @return A boolean value, indicating if at least one condition
     *         is valid or not.
     */
    public boolean check (FrameConstraint conditions) {
        return (this.vector & conditions.vector) != 0;
    };



    // Todo: create nextB arraymatrix by adding all combinations of constellation precomputed
    // NEXTB[SUCCEEDS_DIRECTLY | SUCCEEDS] = NEXTB[SUCEEDS_DIRECTLY] | NEXTB[SUCCEEDS];


    // NextB
    // Integer.numberOfTrailingZeros();
    private static int _next_b (int constellation) {
        return NEXT_B.get(Integer.numberOfTrailingZeros(constellation));
    };


    // Return a configuration, saying:
    // 1. Binary: It matches - it doesn't match!
    // 2. Binary: Go on by forwarding a
    // 3. Binary: Go on by forwarding b
    // 4. The constellation
    public int _constellation (Spans a, Spans b) {

        // Constellation checks are
        // optimized for lazy loading,
        // i.e. trying to minimize callings of end()

        // A starts after B
        if (a.start() > b.start()) {

            // if (this.vector & next_b(SUCCEEDS_DIRECTLY) > 0)

            // Don't call end() on A
            if (a.start() == b.end())
                return SUCCEEDS_DIRECTLY;

            if (a.start() > b.end())
                return SUCCEEDS;

            // a) Check if match is possible
            // b) Check if mext is possible on A

            // Call end() on A
            else if (a.end() == b.end()) {
                return ALIGNS_RIGHT;
            }

            else if (a.end() < b.end()) {
                return IS_WITHIN;
            };

            // a.end() > b.end() &&
            // a.start() < b.end()
            return OVERLAPS_RIGHT;
        }

        // A starts before B
        else if (a.start() < b.start()) {

            // Don't call end() on b
            if (a.end() == b.start()) {
                return PRECEDES_DIRECTLY;
            }

            else if (a.end() < b.start()) {
                return PRECEDES;
            }

            // Call end() on B
            else if (a.end() == b.end()) {
                return ENDS_WITH;
            }

            else if (a.end() > b.end()) {
                return IS_AROUND;
            };

            // a.end() > b.start()
            return OVERLAPS_LEFT;
        }

        // A and B start at the same position
        // a.start() == b.start()
        else if (a.end() > b.end()) {
            return STARTS_WITH;
        }
        else if (a.end() < b.end()) {
            return ALIGNS_LEFT;
        };

        // a.end() == b.end()
        return MATCHES;
    };
};
