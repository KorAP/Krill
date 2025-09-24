package de.ids_mannheim.korap.query;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Collection;

import org.apache.lucene.search.spans.Spans;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import de.ids_mannheim.korap.util.QueryException;

/**
 * @author diewald
 */
@RunWith(JUnit4.class)
public class TestFrameConstraint {

    @Test
    public void testInitialization () throws QueryException {
        FrameConstraint fc = new FrameConstraint();
        assertEquals(false, fc.check(1));
    };


    @Test
    public void testBitVector () throws QueryException {
        int a = 1;
        int b = 1 << 1;

        assertEquals(3, a ^ b);
    };


    @Test
    public void testOr () throws QueryException {
        FrameConstraint fc = new FrameConstraint();

        // Nothing set
        assertEquals(false, fc.check("precedes"));
        assertEquals(false, fc.check("precedesDirectly"));
        assertEquals(false, fc.check("overlapsLeft"));
        assertEquals(false, fc.check("alignsLeft"));
        assertEquals(false, fc.check("startsWith"));
        assertEquals(false, fc.check("matches"));
        assertEquals(false, fc.check("isWithin"));
        assertEquals(false, fc.check("isAround"));
        assertEquals(false, fc.check("endsWith"));
        assertEquals(false, fc.check("alignsRight"));
        assertEquals(false, fc.check("overlapsRight"));
        assertEquals(false, fc.check("succeedsDirectly"));
        assertEquals(false, fc.check("succeeds"));

        // Some or
        fc.add("succeeds").add("succeedsDirectly");
        assertEquals(false, fc.check("precedes"));
        assertEquals(false, fc.check("precedesDirectly"));
        assertEquals(false, fc.check("overlapsLeft"));
        assertEquals(false, fc.check("alignsLeft"));
        assertEquals(false, fc.check("startsWith"));
        assertEquals(false, fc.check("matches"));
        assertEquals(false, fc.check("isWithin"));
        assertEquals(false, fc.check("isAround"));
        assertEquals(false, fc.check("endsWith"));
        assertEquals(false, fc.check("alignsRight"));
        assertEquals(false, fc.check("overlapsRight"));
        assertEquals(true, fc.check("succeedsDirectly"));
        assertEquals(true, fc.check("succeeds"));

        // Moar or
        fc.add("precedes").add("precedesDirectly");
        assertEquals(true, fc.check("precedes"));
        assertEquals(true, fc.check("precedesDirectly"));
        assertEquals(false, fc.check("overlapsLeft"));
        assertEquals(false, fc.check("alignsLeft"));
        assertEquals(false, fc.check("startsWith"));
        assertEquals(false, fc.check("matches"));
        assertEquals(false, fc.check("isWithin"));
        assertEquals(false, fc.check("isAround"));
        assertEquals(false, fc.check("endsWith"));
        assertEquals(false, fc.check("alignsRight"));
        assertEquals(false, fc.check("overlapsRight"));
        assertEquals(true, fc.check("succeedsDirectly"));
        assertEquals(true, fc.check("succeeds"));

        // Moar or
        fc.add("matches").add("startsWith");
        assertEquals(true, fc.check("precedes"));
        assertEquals(true, fc.check("precedesDirectly"));
        assertEquals(false, fc.check("overlapsLeft"));
        assertEquals(false, fc.check("alignsLeft"));
        assertEquals(true, fc.check("startsWith"));
        assertEquals(true, fc.check("matches"));
        assertEquals(false, fc.check("isWithin"));
        assertEquals(false, fc.check("isAround"));
        assertEquals(false, fc.check("endsWith"));
        assertEquals(false, fc.check("alignsRight"));
        assertEquals(false, fc.check("overlapsRight"));
        assertEquals(true, fc.check("succeedsDirectly"));
        assertEquals(true, fc.check("succeeds"));

        // Invert
        fc.invert();
        assertEquals(false, fc.check("precedes"));
        assertEquals(false, fc.check("precedesDirectly"));
        assertEquals(true, fc.check("overlapsLeft"));
        assertEquals(true, fc.check("alignsLeft"));
        assertEquals(false, fc.check("startsWith"));
        assertEquals(false, fc.check("matches"));
        assertEquals(true, fc.check("isWithin"));
        assertEquals(true, fc.check("isAround"));
        assertEquals(true, fc.check("endsWith"));
        assertEquals(true, fc.check("alignsRight"));
        assertEquals(true, fc.check("overlapsRight"));
        assertEquals(false, fc.check("succeedsDirectly"));
        assertEquals(false, fc.check("succeeds"));

        fc.add("precedes").add("precedesDirectly").add("startsWith")
                .add("matches");
        assertEquals(true, fc.check("precedes"));
        assertEquals(true, fc.check("precedesDirectly"));
        assertEquals(true, fc.check("overlapsLeft"));
        assertEquals(true, fc.check("alignsLeft"));
        assertEquals(true, fc.check("startsWith"));
        assertEquals(true, fc.check("matches"));
        assertEquals(true, fc.check("isWithin"));
        assertEquals(true, fc.check("isAround"));
        assertEquals(true, fc.check("endsWith"));
        assertEquals(true, fc.check("alignsRight"));
        assertEquals(true, fc.check("overlapsRight"));
        assertEquals(false, fc.check("succeedsDirectly"));
        assertEquals(false, fc.check("succeeds"));

        fc.add("succeeds").add("succeedsDirectly");
        assertEquals(true, fc.check("precedes"));
        assertEquals(true, fc.check("precedesDirectly"));
        assertEquals(true, fc.check("overlapsLeft"));
        assertEquals(true, fc.check("alignsLeft"));
        assertEquals(true, fc.check("startsWith"));
        assertEquals(true, fc.check("matches"));
        assertEquals(true, fc.check("isWithin"));
        assertEquals(true, fc.check("isAround"));
        assertEquals(true, fc.check("endsWith"));
        assertEquals(true, fc.check("alignsRight"));
        assertEquals(true, fc.check("overlapsRight"));
        assertEquals(true, fc.check("succeedsDirectly"));
        assertEquals(true, fc.check("succeeds"));
    };


    @Test
    public void testOrVector () throws QueryException {
        FrameConstraint fc1 = new FrameConstraint();
        // Some or
        fc1.add("succeeds").add("succeedsDirectly");
        assertEquals(false, fc1.check("precedes"));
        assertEquals(false, fc1.check("precedesDirectly"));
        assertEquals(false, fc1.check("overlapsLeft"));
        assertEquals(false, fc1.check("alignsLeft"));
        assertEquals(false, fc1.check("startsWith"));
        assertEquals(false, fc1.check("matches"));
        assertEquals(false, fc1.check("isWithin"));
        assertEquals(false, fc1.check("isAround"));
        assertEquals(false, fc1.check("endsWith"));
        assertEquals(false, fc1.check("alignsRight"));
        assertEquals(false, fc1.check("overlapsRight"));
        assertEquals(true, fc1.check("succeedsDirectly"));
        assertEquals(true, fc1.check("succeeds"));

        FrameConstraint fc2 = new FrameConstraint();
        fc2.add("precedes").add("precedesDirectly");
        assertEquals(true, fc2.check("precedes"));
        assertEquals(true, fc2.check("precedesDirectly"));
        assertEquals(false, fc2.check("overlapsLeft"));
        assertEquals(false, fc2.check("alignsLeft"));
        assertEquals(false, fc2.check("startsWith"));
        assertEquals(false, fc2.check("matches"));
        assertEquals(false, fc2.check("isWithin"));
        assertEquals(false, fc2.check("isAround"));
        assertEquals(false, fc2.check("endsWith"));
        assertEquals(false, fc2.check("alignsRight"));
        assertEquals(false, fc2.check("overlapsRight"));
        assertEquals(false, fc2.check("succeedsDirectly"));
        assertEquals(false, fc2.check("succeeds"));

        fc1.add(fc2);
        assertEquals(true, fc1.check("precedes"));
        assertEquals(true, fc1.check("precedesDirectly"));
        assertEquals(false, fc1.check("overlapsLeft"));
        assertEquals(false, fc1.check("alignsLeft"));
        assertEquals(false, fc1.check("startsWith"));
        assertEquals(false, fc1.check("matches"));
        assertEquals(false, fc1.check("isWithin"));
        assertEquals(false, fc1.check("isAround"));
        assertEquals(false, fc1.check("endsWith"));
        assertEquals(false, fc1.check("alignsRight"));
        assertEquals(false, fc1.check("overlapsRight"));
        assertEquals(true, fc1.check("succeedsDirectly"));
        assertEquals(true, fc1.check("succeeds"));
    };


    @Test
    public void testConstellation () throws QueryException {
        FrameConstraint fc1 = new FrameConstraint();
        fc1.invert();

        // Precedes
        assertEquals(FrameConstraint.PRECEDES,
                fc1._constellation(new TestSpans(2, 3), new TestSpans(4, 5)));

        // PrecedesDirectly
        assertEquals(FrameConstraint.PRECEDES_DIRECTLY,
                fc1._constellation(new TestSpans(2, 3), new TestSpans(3, 5)));

        // OverlapsLeft
        assertEquals(FrameConstraint.OVERLAPS_LEFT,
                fc1._constellation(new TestSpans(2, 4), new TestSpans(3, 5)));

        // AlignsLeft
        assertEquals(FrameConstraint.ALIGNS_LEFT,
                fc1._constellation(new TestSpans(2, 4), new TestSpans(2, 5)));

        // StartsWith
        assertEquals(FrameConstraint.STARTS_WITH,
                fc1._constellation(new TestSpans(2, 5), new TestSpans(2, 4)));

        // Matches
        assertEquals(FrameConstraint.MATCHES,
                fc1._constellation(new TestSpans(2, 5), new TestSpans(2, 5)));

        // IsWithin
        assertEquals(FrameConstraint.IS_WITHIN,
                fc1._constellation(new TestSpans(3, 4), new TestSpans(2, 5)));

        // IsAround
        assertEquals(FrameConstraint.IS_AROUND,
                fc1._constellation(new TestSpans(2, 5), new TestSpans(3, 4)));

        // EndsWith
        assertEquals(FrameConstraint.ENDS_WITH,
                fc1._constellation(new TestSpans(3, 5), new TestSpans(4, 5)));

        // AlignsRight
        assertEquals(FrameConstraint.ALIGNS_RIGHT,
                fc1._constellation(new TestSpans(3, 4), new TestSpans(2, 4)));

        // OverlapsRight
        assertEquals(FrameConstraint.OVERLAPS_RIGHT,
                fc1._constellation(new TestSpans(3, 5), new TestSpans(2, 4)));

        // SucceedsDirectly
        assertEquals(FrameConstraint.SUCCEEDS_DIRECTLY,
                fc1._constellation(new TestSpans(4, 6), new TestSpans(2, 4)));

        // Succeeds
        assertEquals(FrameConstraint.SUCCEEDS,
                fc1._constellation(new TestSpans(5, 6), new TestSpans(2, 4)));
    };

    private class TestSpans extends Spans {
        private int s, e;


        // Constructor
        public TestSpans (int start, int end) {
            this.s = start;
            this.e = end;
        };


        @Override
        public int doc () {
            return 0;
        };


        @Override
        public int start () {
            return this.s;
        };


        @Override
        public int end () {
            return this.e;
        };


        @Override
        public boolean skipTo (int target) {
            return true;
        };


        @Override
        public boolean next () {
            return true;
        };


        public Collection<byte[]> getPayload () throws IOException {
            return null;
        }


        @Override
        public boolean isPayloadAvailable () throws IOException {
            return false;
        };


        @Override
        public String toString () {
            return "";
        };


        @Override
        public long cost () {
            return 1;
        };

    };
};
