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
    }
    
    @Before
    public void init() {
        KrillProperties.maxTokenMatchSize = 40;
    }
    
    @AfterClass
    public static void resetMaxTokenMatchSize() {
        KrillProperties.maxTokenMatchSize = 50;
    }

    @Test
    public void testLimitingMatchWithProperties () throws IOException {
        // default properties file
        Krill ks = new Krill(json);
        Result kr = ks.apply(ki);
        Match km = kr.getMatch(0);
        assertEquals(40, KrillProperties.maxTokenMatchSize);
        assertTrue(km.getLength() < 40);
    };

    @Test
    public void testLimitingMatchInKrill () throws IOException {
        // Change limit via Krill
        Krill ks = new Krill(json);
        ks.setMaxTokenMatchSize(3);

        Result kr = ks.apply(ki);

        assertEquals(
                "... sechsthäufigste Buchstabe in deutschen Texten. [[Mit Ausnahme von]<!>] Fremdwörtern und Namen ist das ...",
                kr.getMatch(0).getSnippetBrackets());
        assertEquals(
                "<span class=\"context-left\"><span class=\"more\"></span>sechsthäufigste Buchstabe in deutschen Texten. </span><span class=\"match\"><mark>Mit Ausnahme von</mark><span class=\"cutted\"></span></span><span class=\"context-right\"> Fremdwörtern und Namen ist das<span class=\"more\"></span></span>",
                kr.getMatch(0).getSnippetHTML());
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

        ArrayList<String> foundry = new ArrayList<String>();
        foundry.add("opennlp");
        ArrayList<String> layer = new ArrayList<String>();
        layer.add("opennlp");

        // maxMatchTokens from properties = 40
        km = ki.getMatchInfo("match-WUD17/C94/39360-p390-396", "tokens", false,
                foundry, layer, false, false, false, false, false);

        assertEquals("... [[g. Artikel vornimmst, wäre es fein]] ...",
                km.getSnippetBrackets());
        
        // request lower than limit
        // int maxMatchTokens = 2;
        km = ki.getMatchInfo("match-WUD17/C94/39360-p390-392", "tokens",
                false, foundry, layer, false, false, false, false, true);

        assertEquals("... [[g. Artikel]] ...", km.getSnippetBrackets());
        
        // request more than limit
        // maxMatchTokens = 51;
        km = ki.getMatchInfo("match-WUD17/C94/39360-p380-431", "tokens",
                false, foundry, layer, false, false, false, false, false);
        assertTrue(km.endCutted);
        assertEquals(420, km.getEndPos());
    }
}