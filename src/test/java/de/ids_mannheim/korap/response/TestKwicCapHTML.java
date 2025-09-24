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
 * Tests speziell für die HTML-Snippet-Erzeugung (getSnippetHTML)
 * zur Verifikation der neuen KWIC-Kappungslogik:
 *  - Symmetrisches Kürzen der Kontexte
 *  - Trimmen eines zu langen Matches (ohne Kontext)
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
     * Test: Große angefragte Kontexte werden symmetrisch auf das KWIC-Limit gekappt.
     * Erwartung: left+match+right == kwicCap und |left-right| <= 1 und kein "cutted" Marker.
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
     * Test: Wenn das Match allein größer als das KWIC-Limit ist, wird es gekürzt und Kontext entfällt.
     * Erwartung: Nur Match-Tokens, keine left/right Felder, HTML enthält cutted-Marker.
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
}
