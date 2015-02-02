package de.ids_mannheim.korap.query;
import de.ids_mannheim.korap.util.QueryException;
import java.util.*;

/**
 * Class representing a frame constraint as a bit vector.
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

    public FrameConstraint () {
        this.vector = 0;
    };

    public FrameConstraint or (String constraint) throws QueryException {
        int or = FRAME.get(constraint);
        if (or == 0)
            throw new QueryException(706, "Frame type is unknown");

        this.vector |= or;
        return this;
    };

    public FrameConstraint or (FrameConstraint constraint) {
        this.vector |= constraint.vector;
        return this;
    };

    public FrameConstraint invert () {
        this.vector ^= FRAME_ALL;
        return this;
    };


    /**
     * Check for constraint per bit vector.
     */
    public boolean check (int check) {
        return (this.vector & check) != 0;
    };


    /**
     * Check for constraint per FrameConstraint.
     */
    public boolean check (FrameConstraint check) {
        return (this.vector & check.vector) != 0;
    };


    /**
     * Check for constraint per string.
     */
    public boolean check (String constraint) throws QueryException {
        int check = FRAME.get(constraint);
        if (check == 0)
            throw new QueryException(706, "Frame type is unknown");

        return this.check(check);
    };
};
