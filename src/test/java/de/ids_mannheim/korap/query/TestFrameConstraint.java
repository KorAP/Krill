package de.ids_mannheim.korap.query;

import de.ids_mannheim.korap.query.FrameConstraint;
import de.ids_mannheim.korap.util.QueryException;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author diewald
 */
@RunWith(JUnit4.class)
public class TestFrameConstraint {

    @Test
    public void testInitialization () throws QueryException {
        FrameConstraint fc = new FrameConstraint();
        assertEquals(fc.check(1), false);
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
        assertEquals(fc.check("precedes"),         false);
        assertEquals(fc.check("precedesDirectly"), false);
        assertEquals(fc.check("overlapsLeft"),     false);
        assertEquals(fc.check("alignsLeft"),       false);
        assertEquals(fc.check("startsWith"),       false);
        assertEquals(fc.check("matches"),          false);
        assertEquals(fc.check("isWithin"),         false);
        assertEquals(fc.check("isAround"),         false);
        assertEquals(fc.check("endsWith"),         false);
        assertEquals(fc.check("alignsRight"),      false);
        assertEquals(fc.check("overlapsRight"),    false);
        assertEquals(fc.check("succeedsDirectly"), false);
        assertEquals(fc.check("succeeds"),         false);

        // Some or
        fc.add("succeeds").add("succeedsDirectly");
        assertEquals(fc.check("precedes"),         false);
        assertEquals(fc.check("precedesDirectly"), false);
        assertEquals(fc.check("overlapsLeft"),     false);
        assertEquals(fc.check("alignsLeft"),       false);
        assertEquals(fc.check("startsWith"),       false);
        assertEquals(fc.check("matches"),          false);
        assertEquals(fc.check("isWithin"),         false);
        assertEquals(fc.check("isAround"),         false);
        assertEquals(fc.check("endsWith"),         false);
        assertEquals(fc.check("alignsRight"),      false);
        assertEquals(fc.check("overlapsRight"),    false);
        assertEquals(fc.check("succeedsDirectly"), true);
        assertEquals(fc.check("succeeds"),         true);

        // Moar or
        fc.add("precedes").add("precedesDirectly");
        assertEquals(fc.check("precedes"),         true);
        assertEquals(fc.check("precedesDirectly"), true);
        assertEquals(fc.check("overlapsLeft"),     false);
        assertEquals(fc.check("alignsLeft"),       false);
        assertEquals(fc.check("startsWith"),       false);
        assertEquals(fc.check("matches"),          false);
        assertEquals(fc.check("isWithin"),         false);
        assertEquals(fc.check("isAround"),         false);
        assertEquals(fc.check("endsWith"),         false);
        assertEquals(fc.check("alignsRight"),      false);
        assertEquals(fc.check("overlapsRight"),    false);
        assertEquals(fc.check("succeedsDirectly"), true);
        assertEquals(fc.check("succeeds"),         true);

        // Moar or
        fc.add("matches").add("startsWith");
        assertEquals(fc.check("precedes"),         true);
        assertEquals(fc.check("precedesDirectly"), true);
        assertEquals(fc.check("overlapsLeft"),     false);
        assertEquals(fc.check("alignsLeft"),       false);
        assertEquals(fc.check("startsWith"),       true);
        assertEquals(fc.check("matches"),          true);
        assertEquals(fc.check("isWithin"),         false);
        assertEquals(fc.check("isAround"),         false);
        assertEquals(fc.check("endsWith"),         false);
        assertEquals(fc.check("alignsRight"),      false);
        assertEquals(fc.check("overlapsRight"),    false);
        assertEquals(fc.check("succeedsDirectly"), true);
        assertEquals(fc.check("succeeds"),         true);

        // Invert
        fc.invert();
        assertEquals(fc.check("precedes"),         false);
        assertEquals(fc.check("precedesDirectly"), false);
        assertEquals(fc.check("overlapsLeft"),     true);
        assertEquals(fc.check("alignsLeft"),       true);
        assertEquals(fc.check("startsWith"),       false);
        assertEquals(fc.check("matches"),          false);
        assertEquals(fc.check("isWithin"),         true);
        assertEquals(fc.check("isAround"),         true);
        assertEquals(fc.check("endsWith"),         true);
        assertEquals(fc.check("alignsRight"),      true);
        assertEquals(fc.check("overlapsRight"),    true);
        assertEquals(fc.check("succeedsDirectly"), false);
        assertEquals(fc.check("succeeds"),         false);

        fc.add("precedes").
            add("precedesDirectly").
            add("startsWith").
            add("matches");
        assertEquals(fc.check("precedes"),         true);
        assertEquals(fc.check("precedesDirectly"), true);
        assertEquals(fc.check("overlapsLeft"),     true);
        assertEquals(fc.check("alignsLeft"),       true);
        assertEquals(fc.check("startsWith"),       true);
        assertEquals(fc.check("matches"),          true);
        assertEquals(fc.check("isWithin"),         true);
        assertEquals(fc.check("isAround"),         true);
        assertEquals(fc.check("endsWith"),         true);
        assertEquals(fc.check("alignsRight"),      true);
        assertEquals(fc.check("overlapsRight"),    true);
        assertEquals(fc.check("succeedsDirectly"), false);
        assertEquals(fc.check("succeeds"),         false);

        fc.add("succeeds").
            add("succeedsDirectly");
        assertEquals(fc.check("precedes"),         true);
        assertEquals(fc.check("precedesDirectly"), true);
        assertEquals(fc.check("overlapsLeft"),     true);
        assertEquals(fc.check("alignsLeft"),       true);
        assertEquals(fc.check("startsWith"),       true);
        assertEquals(fc.check("matches"),          true);
        assertEquals(fc.check("isWithin"),         true);
        assertEquals(fc.check("isAround"),         true);
        assertEquals(fc.check("endsWith"),         true);
        assertEquals(fc.check("alignsRight"),      true);
        assertEquals(fc.check("overlapsRight"),    true);
        assertEquals(fc.check("succeedsDirectly"), true);
        assertEquals(fc.check("succeeds"),         true);
    };

    @Test
    public void testOrVector () throws QueryException {
        FrameConstraint fc1 = new FrameConstraint();
        // Some or
        fc1.add("succeeds").add("succeedsDirectly");
        assertEquals(fc1.check("precedes"),         false);
        assertEquals(fc1.check("precedesDirectly"), false);
        assertEquals(fc1.check("overlapsLeft"),     false);
        assertEquals(fc1.check("alignsLeft"),       false);
        assertEquals(fc1.check("startsWith"),       false);
        assertEquals(fc1.check("matches"),          false);
        assertEquals(fc1.check("isWithin"),         false);
        assertEquals(fc1.check("isAround"),         false);
        assertEquals(fc1.check("endsWith"),         false);
        assertEquals(fc1.check("alignsRight"),      false);
        assertEquals(fc1.check("overlapsRight"),    false);
        assertEquals(fc1.check("succeedsDirectly"), true);
        assertEquals(fc1.check("succeeds"),         true);

        FrameConstraint fc2 = new FrameConstraint();
        fc2.add("precedes").add("precedesDirectly");
        assertEquals(fc2.check("precedes"),         true);
        assertEquals(fc2.check("precedesDirectly"), true);
        assertEquals(fc2.check("overlapsLeft"),     false);
        assertEquals(fc2.check("alignsLeft"),       false);
        assertEquals(fc2.check("startsWith"),       false);
        assertEquals(fc2.check("matches"),          false);
        assertEquals(fc2.check("isWithin"),         false);
        assertEquals(fc2.check("isAround"),         false);
        assertEquals(fc2.check("endsWith"),         false);
        assertEquals(fc2.check("alignsRight"),      false);
        assertEquals(fc2.check("overlapsRight"),    false);
        assertEquals(fc2.check("succeedsDirectly"), false);
        assertEquals(fc2.check("succeeds"),         false);

        fc1.add(fc2);
        assertEquals(fc1.check("precedes"),         true);
        assertEquals(fc1.check("precedesDirectly"), true);
        assertEquals(fc1.check("overlapsLeft"),     false);
        assertEquals(fc1.check("alignsLeft"),       false);
        assertEquals(fc1.check("startsWith"),       false);
        assertEquals(fc1.check("matches"),          false);
        assertEquals(fc1.check("isWithin"),         false);
        assertEquals(fc1.check("isAround"),         false);
        assertEquals(fc1.check("endsWith"),         false);
        assertEquals(fc1.check("alignsRight"),      false);
        assertEquals(fc1.check("overlapsRight"),    false);
        assertEquals(fc1.check("succeedsDirectly"), true);
        assertEquals(fc1.check("succeeds"),         true);
    };
};
