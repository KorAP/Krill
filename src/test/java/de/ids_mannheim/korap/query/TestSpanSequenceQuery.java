package de.ids_mannheim.korap.query;

import java.util.*;
import de.ids_mannheim.korap.query.wrap.SpanSequenceQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanClassQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanElementQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanRepetitionQueryWrapper;

import de.ids_mannheim.korap.util.QueryException;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;


/**
 * @author margaretha, diewald
 */
@RunWith(JUnit4.class)
public class TestSpanSequenceQuery {

    @Test
    public void spanSequenceQuery () throws QueryException {
        SpanSequenceQueryWrapper sssq = new SpanSequenceQueryWrapper("field");
        assertNull(sssq.toQuery());
        assertFalse(sssq.hasConstraints());

        sssq.append("a").append("b");
        assertEquals("spanNext(field:a, field:b)", sssq.toQuery().toString());
        assertFalse(sssq.hasConstraints());

        sssq.append("c");
        assertEquals("spanNext(spanNext(field:a, field:b), field:c)", sssq.toQuery().toString());
        assertFalse(sssq.hasConstraints());

        sssq = new SpanSequenceQueryWrapper("field");
        sssq.append("a");
        assertEquals("field:a", sssq.toQuery().toString());
        assertFalse(sssq.hasConstraints());

        sssq.append("b");
        assertEquals("spanNext(field:a, field:b)", sssq.toQuery().toString());
        assertFalse(sssq.hasConstraints());

        sssq.withConstraint(2,3);
        assertTrue(sssq.hasConstraints());

        assertEquals("spanDistance(field:a, field:b, [(w[2:3], ordered, notExcluded)])", sssq.toQuery().toString());

        sssq.append("c");
        assertEquals("spanDistance(spanDistance(field:a, field:b, [(w[2:3], ordered, notExcluded)]), field:c, [(w[2:3], ordered, notExcluded)])", sssq.toQuery().toString());
        sssq.withConstraint(6,8, "s");
        assertTrue(sssq.hasConstraints());

        assertEquals("spanMultipleDistance(spanMultipleDistance(field:a, field:b, [(w[2:3], ordered, notExcluded), (s[6:8], ordered, notExcluded)]), field:c, [(w[2:3], ordered, notExcluded), (s[6:8], ordered, notExcluded)])", sssq.toQuery().toString());
    };

    @Test
    public void spanSequenceQueryWrapper () throws QueryException {

        SpanSequenceQueryWrapper ssqw, ssqw2;
        SpanRepetitionQueryWrapper srqw;
        SpanClassQueryWrapper scqw;

        // Synopsis 1
        ssqw =
            new SpanSequenceQueryWrapper("tokens", "der", "Baum");
        assertEquals("spanNext(tokens:der, tokens:Baum)", ssqw.toQuery().toString());

        // Synopsis 2
        ssqw = new SpanSequenceQueryWrapper("tokens");
        ssqw.append("der").append("Baum");
        assertEquals("spanNext(tokens:der, tokens:Baum)", ssqw.toQuery().toString());

        // Append a sequence
        ssqw = new SpanSequenceQueryWrapper("tokens");
        ssqw2 = new SpanSequenceQueryWrapper("tokens");
        ssqw.append("der").append("Baum");
        ssqw2.append("fiel").append("still");
        ssqw.append(ssqw2);
        // This may not be final
        assertEquals(
            "spanNext(spanNext(spanNext(tokens:der, tokens:Baum), tokens:fiel), tokens:still)",
            ssqw.toQuery().toString()
        );

        // Synopsis 3
        ssqw = new SpanSequenceQueryWrapper("tokens", "Baum");
        ssqw.prepend("der");
        assertEquals("spanNext(tokens:der, tokens:Baum)", ssqw.toQuery().toString());

        // Prepend a sequence
        ssqw = new SpanSequenceQueryWrapper("tokens");
        ssqw2 = new SpanSequenceQueryWrapper("tokens");
        ssqw.append("fiel").append("still");
        ssqw2.append("der").append("Baum");
        ssqw.prepend(ssqw2);

        // This may change
        assertEquals(
            "spanNext(spanNext(spanNext(tokens:der, tokens:Baum), tokens:fiel), tokens:still)",
            ssqw.toQuery().toString()
        );

        // Add constraint
        ssqw.withConstraint(2,4);
        // This may change
        assertEquals(
            "spanDistance(spanDistance(spanDistance(tokens:der, "+
            "tokens:Baum, [(w[2:4], ordered, notExcluded)]), "+
            "tokens:fiel, [(w[2:4], ordered, notExcluded)]), "+
            "tokens:still, [(w[2:4], ordered, notExcluded)])",
            ssqw.toQuery().toString()
        );

        ssqw = new SpanSequenceQueryWrapper("tokens", "der", "Baum").withConstraint(1,1);
        assertEquals("spanNext(tokens:der, tokens:Baum)", ssqw.toQuery().toString());

        ssqw = new SpanSequenceQueryWrapper("tokens", "der", "Baum").withConstraint(1,2, "s");
        assertEquals("spanElementDistance(tokens:der, tokens:Baum, [(s[1:2], ordered, notExcluded)])", ssqw.toQuery().toString());

        ssqw = new SpanSequenceQueryWrapper("tokens", "der", "Baum")
            .withConstraint(1,2, "s")
            .withConstraint(2,3, "x");
        assertEquals("spanMultipleDistance(tokens:der, tokens:Baum, " +
                     "[(s[1:2], ordered, notExcluded), " +
                     "(x[2:3], ordered, notExcluded)])",
                     ssqw.toQuery().toString());

        ssqw = new SpanSequenceQueryWrapper("tokens")
            .append("Baum")
            .prepend("der")
            .withConstraint(1,2, "s", true)
            .withConstraint(2,3, "x");
        assertEquals("spanMultipleDistance(tokens:der, " +
                     "tokens:Baum, [(s[1:2], ordered, excluded), " +
                     "(x[2:3], ordered, notExcluded)])",
                     ssqw.toQuery().toString());


        // Support empty class ins sequence 
        ssqw = new SpanSequenceQueryWrapper("field", "Der");
        srqw = new SpanRepetitionQueryWrapper();
        scqw = new SpanClassQueryWrapper(srqw, (short) 3);
        ssqw.append(scqw);
        assertEquals(
            "spanExpansion(field:Der, []{1, 1}, right, class:3)",
            ssqw.toQuery().toString()
        );

        // Support empty class ins sequence 
        ssqw = new SpanSequenceQueryWrapper("field");
        srqw = new SpanRepetitionQueryWrapper();
        ssqw.append(srqw);
        scqw = new SpanClassQueryWrapper(ssqw, (short) 2);
        try {
            scqw.toQuery();
        }
        catch (Exception e) {
            fail(e.getMessage() + " (Known issue)");
        };
        // assertEquals("", scqw.toQuery().toString());

        /*
        sssq = new SpanSequenceQueryWrapper("field");
        sssc = new SpanClassQueryWrapper(sssq, (short) 2);
        SpanSequenceQueryWrapper sssq2 = new SpanSequenceQueryWrapper("field");
        sssq2.append("hui").append(sssc);
        assertEquals("", sssq2.toQuery().toString());
        */
    };
};
