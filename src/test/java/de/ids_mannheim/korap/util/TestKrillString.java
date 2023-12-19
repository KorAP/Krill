package de.ids_mannheim.korap.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
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


    @Test
    public void testQuote () {
        assertEquals("\"hallo\"", quote("hallo"));
        assertEquals("\"h'all'o\"", quote("h'all'o"));
        assertEquals("\"er sagte: \\\"Hallo!\\\"\"",
                quote("er sagte: \"Hallo!\""));
        assertEquals("\"a \\\\\\\" b\"", quote("a \\\" b"));
    };

    @Test
    public void testSurrogateSubstrings () {

        // Follows https://stackoverflow.com/questions/55663213/java-substring-by-code-point-indices-treating-pairs-of-surrogate-code-units-as
        String s = "👦👩👪👫";
        
        assertNotEquals("👦", s.substring(0,1));
        assertEquals("👦", codePointSubstring(s,0,1));
        assertNotEquals("👩", s.substring(1,2));
        assertEquals("👩", codePointSubstring(s,1,2));
        assertNotEquals("👪", s.substring(2,3));
        assertEquals("👪", codePointSubstring(s,2,3));
        assertNotEquals("👫", s.substring(3,4));
        assertEquals("👫", codePointSubstring(s,3,4));
    };
};
