package de.ids_mannheim.korap.query;
import de.ids_mannheim.korap.util.QueryException;
import java.util.*;

/**
 * Class representing a frame constraint, as used by
 * {@link SpanWithinQuery}.
 * Multiple constraints are represented as a bit vector,
 * supporting fast checks if a certain condition is met.
 * The following distinctive frame conditions are supported:
 *
 * <dl>
 *   <dt>precedes</dt>
 *   <dd>A precedes B</dd>
 *
 *   <dt>precedesDirectly</dt>
 *   <dd>A precedes B directly</dd>
 *
 *   <dt>overlapsLeft</dt>
 *   <dd>A overlaps B to the left</dd>
 *
 *   <dt>alignsLeft</dt>
 *   <dd>A aligns with B to the left</dd>
 *
 *   <dt>startsWith</dt>
 *   <dd>A starts with B</dd>
 *
 *   <dt>matches</dt>
 *   <dd>A matches B</dd>
 *
 *   <dt>isWithin</dt>
 *   <dd>A is within B</dd>
 *
 *   <dt>isAround</dt>
 *   <dd>A is around B</dd>
 *
 *   <dt>endsWith</dt>
 *   <dd>A ends with B</dd>
 *
 *   <dt>alignsRight</dt>
 *   <dd>A aligns with B to the right</dd>
 *
 *   <dt>overlapsRight</dt>
 *   <dd>A overlaps B to the right</dd>
 *
 *   <dt>succeedsDirectly</dt>
 *   <dd>A succeeds B directly</dd>
 *
 *   <dt>succeeds</dt>
 *   <dd>A succeeds B</dd>
 * </dl>
 *
 * @author diewald
 */
public class FrameConstraint {

    public static final Integer FRAME_ALL = 1024 * 8 - 1;
    private static final Map<String,Integer> FRAME;
    static {
        Map<String, Integer> FRAME_t = new HashMap<>();

        /*
         * A precedes B
         *
         * |-A-|
         *       |-B-|
         *
         * A.end < B.start
         */
        FRAME_t.put("precedes", 1);

        /*
         * A precedes B directly
         *
         * |-A-|
         *     |-B-|
         *
         * A.end == b.start
         */
        FRAME_t.put("precedesDirectly", 1 << 1);

        /*
         * A overlaps B to the left
         *
         * |-A-|
         *    |-B-|
         *
         * a.end < b.end && a.end > b.start && a.start < b.start
         */
        FRAME_t.put("overlapsLeft", 1 << 2);

        /*
         * A aligns B to the left
         *
         * |-A-|
         * |-B--|
         *
         * a.end < b.end && a.start == b.start
         */
        FRAME_t.put("alignsLeft", 1 << 3);
        
        /*
         * A starts with B
         *
         * |-A--|
         * |-B-|
         *
         * a.end > b.end && a.start == b.start
         */
        FRAME_t.put("startsWith", 1 << 4);
        
        /*
         * A matches B
         *
         * |-A-|
         * |-B-|
         *
         * a.end = b.end && a.start = b.start
         */
        FRAME_t.put("matches", 1 << 5);
        
        /*
         * A is within B
         *
         *  |-A-|
         * |--B--|
         *
         * a.end < b.end && a.start > b.start
         */
        FRAME_t.put("isWithin", 1 << 6);

        /*
         * A is around B
         *
         * |--A--|
         *  |-B-|
         *
         * a.start < b.start && a.end > b.end
         */
        FRAME_t.put("isAround", 1 << 7);

        /*
         * A ends with B
         *
         * |-A--|
         *  |-B-|
         *
         * a.start < b.start && a.end == b.end
         */
        FRAME_t.put("endsWith", 1 << 8);

        /*
         * A aligns B to the right
         *
         *  |-A-|
         * |--B-|
         *
         * a.start > b.start && a.end == b.end
         */
        FRAME_t.put("alignsRight", 1 << 9);

        /*
         * A overlaps B to the right
         *
         *  |-A-|
         * |-B-|
         *
         * a.start > b.start && a.start < b.end && && a.end > b.end
         */
        FRAME_t.put("overlapsRight", 1 << 10);

        /*
         * A succeeds B directly
         *
         *     |-A-|
         * |-B-|
         *
         * a.start == b.end
         */
        FRAME_t.put("succeedsDirectly", 1 << 11);

        /*
         * A succeeds B
         *
         *       |-A-|
         * |-B-|
         *
         * a.start > b.end
         */
        FRAME_t.put("succeeds", 1 << 12);
        FRAME = Collections.unmodifiableMap(FRAME_t);
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
     * @param condition A string representing a valid condition.
     *        See the synopsis for valid condition names.
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
     * @param constraint A Frame constraint representing a set
     *        of valid conditions.
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
        this.vector ^= FRAME_ALL;
        return this;
    };


    /**
     * Check if a condition is valid.
     *
     * @param condition A string representing a condition.
     *        See the synopsis for valid condition names.
     * @return A boolean value, indicating if a condition is valid or not.
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
     * @param conditions An integer bit vector representing a set of conditions.
     * @return A boolean value, indicating if at least one condition is valid or not.
     */
    public boolean check (int conditions) {
        return (this.vector & conditions) != 0;
    };


    /**
     * Check if conditions are valid.
     *
     * @param conditions A {@link FrameConstraint} representing a set of conditions.
     * @return A boolean value, indicating if at least one condition is valid or not.
     */
    public boolean check (FrameConstraint conditions) {
        return (this.vector & conditions.vector) != 0;
    };
};
