package de.ids_mannheim.korap.query;

import static de.ids_mannheim.korap.TestSimple.getJsonString;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.index.FieldDocument;
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.util.QueryException;

/**
 * These tests are meant to fail in a predictable way - but they
 * describe
 * temporary behaviour and will be disabled once these features are
 * part of Krill.
 */

@RunWith(JUnit4.class)
public class TestTemporaryQueryLimitations {

    @Test
    public void classRefCheckNotSupported ()
            throws IOException, QueryException {

        // Construct index
        KrillIndex ki = new KrillIndex();
        String json = new String("{" + "  \"fields\" : [" + "    { "
                + "      \"primaryData\" : \"abc\"" + "    }," + "    {"
                + "      \"name\" : \"tokens\"," + "      \"data\" : ["
                + "         [ \"s:a\", \"i:a\", \"_0#0-1\", \"-:t$<i>3\"],"
                + "         [ \"s:b\", \"i:b\", \"_1#1-2\" ],"
                + "         [ \"s:c\", \"i:c\", \"_2#2-3\" ]" + "      ]"
                + "    }" + "  ]" + "}");

        FieldDocument fd = ki.addDoc(json);
        ki.commit();

        json = getJsonString(getClass()
                .getResource("/queries/bugs/cosmas_classrefcheck.jsonld")
                .getFile());

        Krill ks = new Krill(json);
        Result kr = ks.apply(ki);
        assertEquals(
            "focus(130: {131: spanContain({129: <tokens:s />}, {130: tokens:s:wegen})},sorting)",
            kr.getSerialQuery());
        assertEquals(0, kr.getTotalResults());
        assertEquals(0, kr.getStartIndex());

        assertEquals("This is a warning coming from the serialization",
                kr.getWarning(1).getMessage());

        assertEquals(
                "Class reference checks are currently not supported"
                        + " - results may not be correct",
                kr.getWarning(0).getMessage());

        assertEquals(2, kr.getWarnings().size());
    };
};
