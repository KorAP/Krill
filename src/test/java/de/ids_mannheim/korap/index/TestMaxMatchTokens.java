package de.ids_mannheim.korap.index;

import static de.ids_mannheim.korap.TestSimple.getJsonString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import org.junit.Test;

import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.response.Match;
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.util.KrillConfiguration;
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

    @Test
    public void searchJSONmatchSize () throws IOException {
        // Limiting default match token size in KrillIndex
        KrillConfiguration config = new KrillConfiguration();
        config.setMaxMatchTokens(2);
        ki.setKrillConfig(config);

        Krill ks = new Krill(json);
        Result kr = ks.apply(ki);
        assertEquals(3, kr.getTotalResults());

        assertTrue(kr.getMatch(0).endCutted);
        
        assertEquals(
                "... eine durchschnittliche Häufigkeit von 6,51 %. [[Er ist]<!>] damit der sechsthäufigste Buchstabe in ...",
                kr.getMatch(0).getSnippetBrackets());
        assertEquals(
                "<span class=\"context-left\"><span class=\"more\"></span>eine durchschnittliche Häufigkeit von 6,51 %. </span><span class=\"match\"><mark>Er ist</mark><span class=\"cutted\"></span></span><span class=\"context-right\"> damit der sechsthäufigste Buchstabe in<span class=\"more\"></span></span>",
                kr.getMatch(0).getSnippetHTML());
    }
    
    @Test
    public void testLimitingMatchInKrill () throws IOException {

        // Change limit via Krill
        Krill ks = new Krill(json);
        KrillConfiguration config = new KrillConfiguration();
        config.setMaxMatchTokens(3);
        ks.setConfig(config);
        Result kr = ks.apply(ki);

        assertEquals(
                "... eine durchschnittliche Häufigkeit von 6,51 %. [[Er ist damit]<!>] der sechsthäufigste Buchstabe in deutschen ...",
                kr.getMatch(0).getSnippetBrackets());
        assertEquals(
                "<span class=\"context-left\"><span class=\"more\"></span>eine durchschnittliche Häufigkeit von 6,51 %. </span><span class=\"match\"><mark>Er ist damit</mark><span class=\"cutted\"></span></span><span class=\"context-right\"> der sechsthäufigste Buchstabe in deutschen<span class=\"more\"></span></span>",
                kr.getMatch(0).getSnippetHTML());
    };
    
    
    @Test
    public void testLimitingMatchWithProperties () throws IOException {

        // Change limit via Krill
        Krill ks = new Krill(json);
        Properties properties = KrillProperties.loadDefaultProperties();
        KrillConfiguration config = KrillConfiguration
                .createNewConfiguration(properties );
        ks.setConfig(config);
        
        Result kr = ks.apply(ki);
        
        assertEquals(
                "... Häufigkeit von 6,51 %. [[Er]<!>] ist damit der ...",
                kr.getMatch(0).getSnippetBrackets());
        assertEquals(
                "<span class=\"context-left\"><span class=\"more\"></span>Häufigkeit von 6,51 %. </span><span class=\"match\"><mark>Er</mark><span class=\"cutted\"></span></span><span class=\"context-right\"> ist damit der<span class=\"more\"></span></span>",
                kr.getMatch(0).getSnippetHTML());
    };

    @Test
    public void testMatchInfoWithKrillConfig ()
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

        km = ki.getMatchInfo("match-WUD17/C94/39360-p390-396", "tokens", false,
                foundry, layer, false, false, false, false, true);

        assertEquals("... [[g. Artikel vornimmst, wäre es fein]] ...",
                km.getSnippetBrackets());

        KrillConfiguration config = new KrillConfiguration();
        config.setMaxMatchTokens(2);
        Match km2 = ki.getMatchInfo("match-WUD17/C94/39360-p390-396", "tokens",
                false, foundry, layer, false, false, false, false, true, // extendToSentence
                config);

        assertTrue(km2.endCutted);
        assertEquals("... [[g. Artikel]<!>] ...", km2.getSnippetBrackets());

    }
}
