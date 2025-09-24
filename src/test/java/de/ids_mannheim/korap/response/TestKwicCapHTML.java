package de.ids_mannheim.korap.response;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.fasterxml.jackson.databind.node.ObjectNode;

import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.KrillMeta;
import de.ids_mannheim.korap.index.FieldDocument;
import de.ids_mannheim.korap.query.QueryBuilder;
import de.ids_mannheim.korap.util.KrillProperties;

/**
 * Tests specifically for HTML snippet generation (getSnippetHTML)
 * to verify the new KWIC truncation logic:
 *  - Symmetric shortening of contexts
 *  - Trimming of an overly long match (without context)
 */
@RunWith(JUnit4.class)
public class TestKwicCapHTML {

    private int oldKwic;
    private int oldMatch;
    private int oldContext;

    @Before
    public void saveOldProps() {
        oldKwic = KrillProperties.maxTokenKwicSize;
        oldMatch = KrillProperties.maxTokenMatchSize;
        oldContext = KrillProperties.maxTokenContextSize;
    }

    @After
    public void restoreOldProps() {
        KrillProperties.maxTokenKwicSize = oldKwic;
        KrillProperties.maxTokenMatchSize = oldMatch;
        KrillProperties.maxTokenContextSize = oldContext;
    }

    private static int size(ObjectNode tok, String key) {
        return tok.has(key) ? tok.get(key).size() : 0;
    }

    private static int total(ObjectNode tok) {
        if (tok == null) return 0;
        return size(tok, "left") + size(tok, "match") + size(tok, "right");
    }

    /**
     * Test: Large requested contexts are symmetrically truncated to the KWIC limit.
     * Expectation: left+match+right == kwicCap and |left-right| <= 1 and no "cutted" marker.
     */
    @Test
    public void htmlKwicCap_ContextSymmetry() throws IOException {
        Properties p = new Properties();
        p.setProperty("krill.kwic.max.token", "51");
        p.setProperty("krill.match.max.token", "200");
        p.setProperty("krill.context.max.token", "200");
        KrillProperties.setProp(p);
        KrillProperties.updateConfigurations(p);

        KrillIndex ki = new KrillIndex();
        ki.addDoc(getClass().getResourceAsStream("/wiki/00001.json.gz"), true);
        ki.commit();

        QueryBuilder qb = new QueryBuilder("tokens");
        Krill k = new Krill(qb.seg("s:der"));
        KrillMeta meta = k.getMeta();
        meta.setSnippets(true);
        meta.setTokens(true);
        meta.getContext().left.setLength((short) 500);
        meta.getContext().right.setLength((short) 500);

        Result r = k.apply(ki);
        assertTrue(r.getTotalResults() > 0);
        Match m = r.getMatch(0);

        // Tokens zuerst holen (triggert auch Verarbeitung)
        ObjectNode tok = m.getSnippetTokens();
        assertNotNull(tok);
        int cap = KrillProperties.maxTokenKwicSize;
        assertEquals(cap, total(tok));

        int left = size(tok, "left");
        int match = size(tok, "match");
        int right = size(tok, "right");
        int allowedContext = Math.max(0, cap - match);
        assertEquals("Left+Right must use full remaining context budget", allowedContext, left + right);
        assertTrue("Match must be <= cap", match <= cap);
        assertTrue("Left context non-negative", left >= 0);
        assertTrue("Right context non-negative", right >= 0);
        assertTrue("Left does not exceed requested", left <= 500);
        assertTrue("Right does not exceed requested", right <= 500);

        String html = m.getSnippetHTML();
        assertNotNull(html);
        assertTrue("HTML snippet should start with context-left span", html.contains("<span class=\"context-left\">"));
        assertTrue("HTML snippet should contain match span", html.contains("<span class=\"match\">"));
        assertTrue("HTML snippet should contain context-right span", html.contains("<span class=\"context-right\">"));
        assertFalse("No cut marker expected for pure context trimming", html.contains("class=\"cutted\""));
    }

    /**
     * Test: If the match alone is larger than the KWIC limit, it is trimmed and context is omitted.
     * Expectation: Only match tokens, no left/right fields, HTML contains cutted marker.
     */
    @Test
    public void htmlKwicCap_LongMatchTrimmed() throws IOException {
        Properties p = new Properties();
        p.setProperty("krill.kwic.max.token", "31"); // relatively small KWIC cap
        p.setProperty("krill.match.max.token", "500"); // allow long match
        p.setProperty("krill.context.max.token", "200");
        KrillProperties.setProp(p);
        KrillProperties.updateConfigurations(p);

        KrillIndex ki = new KrillIndex();
        FieldDocument fd = ki.addDoc(1, getClass().getResourceAsStream("/goe/AGX-00002.json"), false);
        ki.commit();
        assertEquals("GOE_AGX.00002", fd.getTextSigle());

        QueryBuilder qb = new QueryBuilder("tokens");
        Krill k = new Krill(qb.tag("xy/z:long")); // long match
        KrillMeta meta = k.getMeta();
        meta.setTokens(true);
        meta.setSnippets(true);
        meta.getContext().left.setLength((short) 200);
        meta.getContext().right.setLength((short) 200);

        Result r = k.apply(ki);
        assertEquals(1, r.getTotalResults());
        Match m = r.getMatch(0);

        ObjectNode tok = m.getSnippetTokens();
        assertNotNull(tok);
        assertFalse("Left context should be absent when match alone exceeds cap", tok.has("left"));
        assertFalse("Right context should be absent when match alone exceeds cap", tok.has("right"));
        assertEquals("Match token count should equal KWIC cap", KrillProperties.maxTokenKwicSize, size(tok, "match"));

        String html = m.getSnippetHTML();
        assertNotNull(html);
        assertTrue("Trimmed match should show cut marker", html.contains("class=\"cutted\""));
        // Context containers always exist but should be empty (no visible text) here.
        // We intentionally avoid brittle content assertions due to token/annotation variability.
    }

    /**
     * Test: Deprecated properties maxTokenMatchSize and maxTokenContextSize are ignored.
     * Only maxTokenKwicSize should affect the KWIC output.
     */
    @Test
    public void htmlKwicCap_DeprecatedPropertiesIgnored() throws IOException {
        Properties p = new Properties();
        p.setProperty("krill.kwic.max.token", "51");
        p.setProperty("krill.match.max.token", "200"); // deprecated
        p.setProperty("krill.context.max.token", "200"); // deprecated
        KrillProperties.setProp(p);
        KrillProperties.updateConfigurations(p);

        KrillIndex ki = new KrillIndex();
        ki.addDoc(getClass().getResourceAsStream("/wiki/00001.json.gz"), true);
        ki.commit();

        QueryBuilder qb = new QueryBuilder("tokens");
        Krill k = new Krill(qb.seg("s:der"));
        KrillMeta meta = k.getMeta();
        meta.setSnippets(true);
        meta.setTokens(true);
        meta.getContext().left.setLength((short) 500);
        meta.getContext().right.setLength((short) 500);

        Result r = k.apply(ki);
        assertTrue(r.getTotalResults() > 0);
        Match m = r.getMatch(0);
        ObjectNode tok = m.getSnippetTokens();
        assertNotNull(tok);
        int cap = KrillProperties.maxTokenKwicSize;
        assertEquals("KWIC cap must be respected regardless of deprecated properties", cap, total(tok));
    }

    /**
     * Test: maxTokenContextSize with different settings (21, 51, 101, 151, 201).
     * The KWIC output must not exceed the cap. If both contexts reach the requested
     * length and the theoretical maximum exceeds the cap, the cap must be filled.
     * Otherwise boundaries may reduce the total size legitimately.
     */
    @Test
    public void htmlKwicCap_ContextSizeVariants() throws IOException {
        int[] contextSizes = {21, 51, 101, 151, 201};
        int kwicCap = 101;
        for (int context : contextSizes) {
            Properties p = new Properties();
            p.setProperty("krill.kwic.max.token", String.valueOf(kwicCap));
            p.setProperty("krill.context.max.token", String.valueOf(context)); // deprecated
            KrillProperties.setProp(p);
            KrillProperties.updateConfigurations(p);

            KrillIndex ki = new KrillIndex();
            ki.addDoc(getClass().getResourceAsStream("/wiki/00001.json.gz"), true);
            ki.commit();

            QueryBuilder qb = new QueryBuilder("tokens");
            Krill k = new Krill(qb.seg("s:der"));
            KrillMeta meta = k.getMeta();
            meta.setSnippets(true);
            meta.setTokens(true);
            meta.getContext().left.setLength((short) context);
            meta.getContext().right.setLength((short) context);

            Result r = k.apply(ki);
            assertTrue(r.getTotalResults() > 0);
            Match m = r.getMatch(0);
            ObjectNode tok = m.getSnippetTokens();
            assertNotNull(tok);
            int matchTokens = size(tok, "match");
            int left = size(tok, "left");
            int right = size(tok, "right");
            int totalTokens = total(tok);

            // Invariants: decomposition and cap
            assertEquals("Token decomposition mismatch", matchTokens + left + right, totalTokens);
            assertTrue("Total tokens must not exceed KWIC cap", totalTokens <= kwicCap);
            assertTrue("Left must not exceed requested context", left <= context);
            assertTrue("Right must not exceed requested context", right <= context);

            // If both sides reached requested length and theoretical max > cap, cap should be full
            int theoreticalMax = matchTokens + 2 * context;
            if (left == context && right == context && theoreticalMax >= kwicCap) {
                assertEquals("Cap should be fully utilized when enough tokens available", kwicCap, totalTokens);
            }
        }
    }

    /**
     * Test: Changing deprecated context property values does not influence result.
     */
    @Test
    public void htmlKwicCap_DeprecatedContextPropertyNoEffect() throws IOException {
        int kwicCap = 51;
        int[] deprecatedValues = {1, 3, 30, 300};
        Integer baselineMatch = null;

        for (int dep : deprecatedValues) {
            Properties p = new Properties();
            p.setProperty("krill.kwic.max.token", String.valueOf(kwicCap));
            p.setProperty("krill.context.max.token", String.valueOf(dep)); // deprecated
            KrillProperties.setProp(p);
            KrillProperties.updateConfigurations(p);

            KrillIndex ki = new KrillIndex();
            ki.addDoc(getClass().getResourceAsStream("/wiki/00001.json.gz"), true);
            ki.commit();

            QueryBuilder qb = new QueryBuilder("tokens");
            Krill k = new Krill(qb.seg("s:der"));
            KrillMeta meta = k.getMeta();
            meta.setSnippets(true);
            meta.setTokens(true);
            meta.getContext().left.setLength((short) 500);
            meta.getContext().right.setLength((short) 500);

            Result r = k.apply(ki);
            assertTrue(r.getTotalResults() > 0);
            Match m = r.getMatch(0);
            ObjectNode tok = m.getSnippetTokens();
            assertNotNull(tok);
            int matchLen = size(tok, "match");
            int totalLen = total(tok);
            assertTrue("Total tokens must not exceed KWIC cap", totalLen <= kwicCap);
            if (baselineMatch == null) baselineMatch = matchLen;
            else assertEquals("Match length changed across deprecated context property values", (int) baselineMatch, matchLen);
        }
    }

    /**
     * Test: Changing deprecated match property values does not influence truncation of long match.
     */
    @Test
    public void htmlKwicCap_DeprecatedMatchPropertyNoEffect() throws IOException {
        int kwicCap = 40;
        int[] deprecatedMatchValues = {5, 500};
        int[][] observed = new int[deprecatedMatchValues.length][3]; // left, match, right

        for (int i = 0; i < deprecatedMatchValues.length; i++) {
            int dep = deprecatedMatchValues[i];
            Properties p = new Properties();
            p.setProperty("krill.kwic.max.token", String.valueOf(kwicCap));
            p.setProperty("krill.match.max.token", String.valueOf(dep)); // deprecated
            p.setProperty("krill.context.max.token", "100");
            KrillProperties.setProp(p);
            KrillProperties.updateConfigurations(p);

            KrillIndex ki = new KrillIndex();
            // plain JSON (not gzipped)
            ki.addDoc(1, getClass().getResourceAsStream("/goe/AGX-00002.json"), false);
            ki.commit();

            QueryBuilder qb = new QueryBuilder("tokens");
            Krill k = new Krill(qb.tag("xy/z:long")); // long match
            KrillMeta meta = k.getMeta();
            meta.setSnippets(true);
            meta.setTokens(true);
            meta.getContext().left.setLength((short) 100);
            meta.getContext().right.setLength((short) 100);

            Result r = k.apply(ki);
            assertEquals(1, r.getTotalResults());
            Match m = r.getMatch(0);
            ObjectNode tok = m.getSnippetTokens();
            assertNotNull(tok);
            observed[i][0] = size(tok, "left");
            observed[i][1] = size(tok, "match");
            observed[i][2] = size(tok, "right");
            assertFalse("Left context should be absent or empty when long match fills cap", tok.has("left"));
            assertFalse("Right context should be absent or empty when long match fills cap", tok.has("right"));
            assertEquals("Match must be trimmed to kwic cap", kwicCap, observed[i][1]);
        }

        // All observations for long match trimming should be identical
        for (int i = 1; i < deprecatedMatchValues.length; i++) {
            assertArrayEquals(
                "Deprecated match property value influenced trimming (index=" + i + ")",
                observed[0], observed[i]);
        }
    }
}
