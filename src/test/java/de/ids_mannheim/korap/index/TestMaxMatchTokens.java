package de.ids_mannheim.korap.index;

import static de.ids_mannheim.korap.TestSimple.getJsonString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.response.Match;
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.util.KrillProperties;
import de.ids_mannheim.korap.util.QueryException;

public class TestMaxMatchTokens {

    private KrillIndex ki;
    private String json;

    private ArrayList<String> foundry = new ArrayList<String>();
    private ArrayList<String> layer = new ArrayList<String>();
    
    
    public TestMaxMatchTokens () throws IOException {
        ki = new KrillIndex();
        // Indexing test files
        for (String i : new String[] { "00001" }) {
            ki.addDoc(getClass().getResourceAsStream("/wiki/" + i + ".json.gz"),
                    true);
        };
        ki.commit();

        json = getJsonString(getClass()
                .getResource("/queries/position/sentence-contain-token.json")
                .getFile());
        
        foundry.add("opennlp");
        layer.add("p");
        
    }
    
    @Before
    public void init() {
        KrillProperties.maxTokenMatchSize = 40;
    }
    
    @AfterClass
    public static void resetMaxTokenMatchSize() {
        KrillProperties.maxTokenMatchSize = 50;
    }

    private int getKwicTokenCount(Match m) {
        com.fasterxml.jackson.databind.node.ObjectNode tok = m.getSnippetTokens();
        if (tok == null) return 0;
        int total = 0;
        if (tok.has("left")) total += tok.get("left").size();
        if (tok.has("match")) total += tok.get("match").size();
        if (tok.has("right")) total += tok.get("right").size();
        return total;
    }

    @Test
    public void testLimitingMatchWithProperties () throws IOException {
        // default properties file
        Krill ks = new Krill(json);
        Result kr = ks.apply(ki);
        Match km = kr.getMatch(0);
        assertEquals(40, KrillProperties.maxTokenMatchSize);

        int kwic = getKwicTokenCount(km);
        int expectedMax = (2 * KrillProperties.defaultSearchContextLength)
                + KrillProperties.maxTokenMatchSize;
        assertTrue(kwic <= expectedMax);
    };

    @Test
    public void testLimitingMatchInKrill () throws IOException {
        // Per-query match-size is deprecated for capping;
        // total KWIC token cap applies globally
        Krill ks = new Krill(json);

        Result kr = ks.apply(ki);
        Match km = kr.getMatch(0);

        int kwic = getKwicTokenCount(km);
        int expectedMax = KrillProperties.maxTokenKwicSize;
        assertTrue(kwic <= expectedMax);
    };

    @Test
    public void testMatchInfo ()
            throws IOException, QueryException {
        KrillIndex ki = new KrillIndex();
        // Indexing test files
        ki.addDoc(
                getClass().getResourceAsStream("/wiki/WUD17-C94-39360.json.gz"),
                true);
        ki.commit();
        Match km;

        // maxMatchTokens from properties = 40
        km = ki.getMatchInfo("match-WUD17/C94/39360-p390-396", "tokens", false,
                foundry, layer, false, false, false, false, false);
        int kwic1 = getKwicTokenCount(km);
        int expectedMax1 = (2 * KrillProperties.defaultSearchContextLength)
                + KrillProperties.maxTokenMatchSize;
        assertTrue(kwic1 <= expectedMax1);

        // request lower than limit (maxMatchTokens = 2 via ID)
        km = ki.getMatchInfo("match-WUD17/C94/39360-p390-392", "tokens",
                false, foundry, layer, false, false, false, false, true);
        int kwic2 = getKwicTokenCount(km);
        assertTrue(kwic2 <= expectedMax1);

        // request more than limit (maxMatchTokens = 51)
        km = ki.getMatchInfo("match-WUD17/C94/39360-p380-431", "tokens",
                false, foundry, layer, false, false, false, false, false);
        int kwic3 = getKwicTokenCount(km);
        assertTrue(kwic3 <= expectedMax1);
    }
    
    @Test
    public void testMatchInfoExpansion () throws QueryException, IOException {
        KrillProperties.maxTokenMatchSize = 1;
        KrillIndex ki = new KrillIndex();
        // Indexing test files
        ki.addDoc(
                getClass().getResourceAsStream("/wiki/WUD17-C94-39360.json.gz"),
                true);
        ki.commit();
        
        // cut left match expansion
        Match km = ki.getMatchInfo("match-WUD17/C94/39360-p225-226", "tokens",
                true, foundry , layer, true, true, true, true, true);
        int kwicA = getKwicTokenCount(km);
        int expectedMaxA = de.ids_mannheim.korap.util.KrillProperties.getMaxTokenKwicSize();
        assertTrue(kwicA <= expectedMaxA);
        
        // cut right match expansion        
        km = ki.getMatchInfo("match-WUD17/C94/39360-p210-211", "tokens", false,
                foundry, layer, false, false, false, false, true);
        int kwicB = getKwicTokenCount(km);
        assertTrue(kwicB <= expectedMaxA);
        
        // cut left and right match expansion
        km = ki.getMatchInfo("match-WUD17/C94/39360-p213-214", "tokens", false,
                foundry, layer, false, false, false, false, true);
        int kwicC = getKwicTokenCount(km);
        assertTrue(kwicC <= expectedMaxA);
        
        // no cut
        km = ki.getMatchInfo("match-WUD17/C94/39360-p160-161", "tokens", false,
                foundry, layer, false, false, false, false, true);
        int kwicD = getKwicTokenCount(km);
        assertTrue(kwicD <= expectedMaxA);
        
        KrillProperties.maxTokenMatchSize = 20;
    }
    
}
