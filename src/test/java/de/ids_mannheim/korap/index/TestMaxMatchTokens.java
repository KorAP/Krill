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
        assertEquals(213, km.getStartPos());
        assertEquals(228, km.getEndPos());
        assertEquals(15, km.getLength());
        assertEquals("[[<!>{opennlp/p:ADV:auch} {opennlp/p:APPRART:zur} "
                + "{opennlp/p:NN:Nutzung} {opennlp/p:ART:des} {opennlp/p:NN:Namens} "
                + "{opennlp/p:VVPP:berechtigt} {opennlp/p:VAFIN:ist} "
                + "({opennlp/p:VVIMP:siehe} {opennlp/p:PROAV:dazu} "
                + "{opennlp/p:PPOSAT:unsere} {opennlp/p:NN:Hinweise} "
                + "{opennlp/p:APPRART:zur} [{opennlp/p:NN:Wahl}] "
                + "{opennlp/p:ART:des} {opennlp/p:NN:Benutzernamens}). ]]", 
                km.getSnippetBrackets());
        
        // cut right match expansion        
        km = ki.getMatchInfo("match-WUD17/C94/39360-p210-211", "tokens", false,
                foundry, layer, false, false, false, false, true);
        assertEquals(199, km.getStartPos());
        assertEquals(223, km.getEndPos());
        assertEquals(24, km.getLength());
        assertEquals("[Benutzerkonten sollen nur dann einen offiziell klingenden"
                + " Namen haben, wenn der [Betreiber] des Kontos auch zur Nutzung "
                + "des Namens berechtigt ist (siehe dazu unsere <!>]", 
                km.getSnippetBrackets());
        
        // cut left and right match expansion
        km = ki.getMatchInfo("match-WUD17/C94/39360-p213-214", "tokens", false,
                foundry, layer, false, false, false, false, true);
        assertEquals(201, km.getStartPos());
        assertEquals(226, km.getEndPos());
        assertEquals(25, km.getLength());
        assertEquals("[<!>nur dann einen offiziell klingenden Namen haben, wenn "
                + "der Betreiber des Kontos [auch] zur Nutzung des Namens "
                + "berechtigt ist (siehe dazu unsere Hinweise zur Wahl <!>]", 
                km.getSnippetBrackets());
        
        // no cut
        km = ki.getMatchInfo("match-WUD17/C94/39360-p160-161", "tokens", false,
                foundry, layer, false, false, false, false, true);
        assertEquals(150, km.getStartPos());
        assertEquals(162, km.getEndPos());
        assertEquals(12, km.getLength());
        
        KrillProperties.maxTokenMatchSize = 20;
    }
    
}
