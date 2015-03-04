package de.ids_mannheim.korap.util;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import static de.ids_mannheim.korap.util.KrillString.*;
import de.ids_mannheim.korap.util.QueryException;

/**
 * @author diewald
 */
public class TestKrillString {

    @Test
    public void testHTMLescape () {
        assertEquals("Der &amp; Die", escapeHTML("Der & Die"));
        assertEquals("Der &amp; Die &amp;", escapeHTML("Der & Die &"));
        assertEquals("&lt;x&gt;Hui&lt;/x&gt;", escapeHTML("<x>Hui</x>"));
        assertEquals("Er sagte: &quot;Das ist ja toll!&quot;",
                escapeHTML("Er sagte: \"Das ist ja toll!\""));
    };
};
