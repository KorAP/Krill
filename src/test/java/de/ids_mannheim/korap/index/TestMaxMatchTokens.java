package de.ids_mannheim.korap.index;

import static de.ids_mannheim.korap.TestSimple.getJsonString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;

import org.junit.Test;

import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.response.Match;
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.util.KrillConfiguration;
import de.ids_mannheim.korap.util.QueryException;

public class TestMaxMatchTokens {
    
    @Test
    public void searchJSONmatchSize () throws IOException {
        // Limiting default match token size
        KrillIndex ki = new KrillIndex();
        KrillConfiguration config = new KrillConfiguration();
        config.setMaxMatchTokens(2);
        ki.setKrillConfig(config);
        
        // Indexing test files
        for (String i : new String[] { "00001" }) {
            ki.addDoc(getClass().getResourceAsStream("/wiki/" + i + ".json.gz"),
                    true);
        };
        ki.commit();

        String json = getJsonString(getClass()
                .getResource("/queries/position/sentence-contain-token.json").getFile());

        Krill ks = new Krill(json);
        Result kr = ks.apply(ki);
        assertEquals(78, kr.getTotalResults());

        assertEquals(
                "... des lateinischen Alphabets und ein Vokal. [[Der Buchstabe]<!>] A hat in deutschen Texten eine ...",
                kr.getMatch(0).getSnippetBrackets());
        assertEquals(
                "<span class=\"context-left\"><span class=\"more\"></span>des lateinischen Alphabets und ein Vokal. </span><span class=\"match\"><mark>Der Buchstabe</mark><span class=\"cutted\"></span></span><span class=\"context-right\"> A hat in deutschen Texten eine<span class=\"more\"></span></span>",
                kr.getMatch(0).getSnippetHTML());
        
        // Change limit via Krill
        Krill ks2 = new Krill(json);
        KrillConfiguration config2 = new KrillConfiguration();
        config.setMaxMatchTokens(3);
        ks2.setConfig(config2);
        Result kr2 = ks.apply(ki);
        
        assertEquals(
                "... des lateinischen Alphabets und ein Vokal. [[Der Buchstabe A]<!>] hat in deutschen Texten eine durchschnittliche ...",
                kr2.getMatch(0).getSnippetBrackets());
        assertEquals(
                "<span class=\"context-left\"><span class=\"more\"></span>des lateinischen Alphabets und ein Vokal. </span><span class=\"match\"><mark>Der Buchstabe A</mark><span class=\"cutted\"></span></span><span class=\"context-right\"> hat in deutschen Texten eine durchschnittliche<span class=\"more\"></span></span>",
                kr2.getMatch(0).getSnippetHTML());
    };

    @Test
    public void testMatchInfoWithKrillConfig () throws IOException, QueryException {
        KrillIndex ki = new KrillIndex();
        // Indexing test files
        ki.addDoc(getClass().getResourceAsStream("/wiki/WUD17-C94-39360.json.gz"), true);
        ki.commit();
        Match km;
        
        ArrayList<String> foundry = new ArrayList<String>();
        foundry.add("opennlp");
        ArrayList<String> layer = new ArrayList<String>();
        layer.add("opennlp");

        km = ki.getMatchInfo("match-WUD17/C94/39360-p390-396",
                "tokens",
                false,
                foundry,
                layer,
                false,
                false,
                false,
                false,
                true); 

        assertEquals("... [[g. Artikel vornimmst, wäre es fein]] ...", km.getSnippetBrackets());

        
        KrillConfiguration config = new KrillConfiguration();
        config.setMaxMatchTokens(2);
        Match km2 = ki.getMatchInfo("match-WUD17/C94/39360-p390-396",
                "tokens",
                false,
                foundry,
                layer,
                false,
                false,
                false,
                false,
                true, // extendToSentence
                config); 
        
        assertTrue(km2.endCutted);
        assertEquals("... [[g. Artikel]<!>] ...", km2.getSnippetBrackets());

    }
}
