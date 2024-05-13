package de.ids_mannheim.korap.index;

import static de.ids_mannheim.korap.TestSimple.getJsonString;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Test;

import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.response.SearchContext;
import de.ids_mannheim.korap.util.KrillConfiguration;
import de.ids_mannheim.korap.util.KrillProperties;

public class TestMaxContext {
    private static KrillIndex ki;
    private static String jsonQuery;
    
    @BeforeClass
    public static void init () throws IOException {
        ki = new KrillIndex();
        for (String i : new String[] { "00001" }) {
            ki.addDoc(TestMaxContext.class
                    .getResourceAsStream("/wiki/" + i + ".json.gz"), true);
        };
        ki.commit();

        // left and right contexts: token 5
        jsonQuery = getJsonString(TestMaxContext.class
                .getResource("/queries/position/sentence-contain-token.json")
                .getFile());
    }

    @Test
    public void searchWithContextTokenSize () throws IOException {
        Krill ks = new Krill(jsonQuery);
        Result kr = ks.apply(ki);
        
        SearchContext context = kr.getContext();
        assertEquals(500, context.left.getMaxLength());
        assertEquals(500, context.right.getMaxLength());
        assertEquals(5, context.left.getLength());
        assertEquals(5, context.right.getLength());
        
        assertEquals(
                "... eine durchschnittliche Häufigkeit von 6,51 %. [[Er ist damit der sechsthäufigste Buchstabe in deutschen Texten]]. Mit Ausnahme von Fremdwörtern und ...",
                kr.getMatch(0).getSnippetBrackets());
        assertEquals(
                "<span class=\"context-left\"><span class=\"more\"></span>eine durchschnittliche Häufigkeit von 6,51 %. </span><span class=\"match\"><mark>Er ist damit der sechsthäufigste Buchstabe in deutschen Texten</mark></span><span class=\"context-right\">. Mit Ausnahme von Fremdwörtern und<span class=\"more\"></span></span>",
                kr.getMatch(0).getSnippetHTML());
        
        
        // limiting max context tokens
        int maxLength = 2;
        KrillConfiguration config = new KrillConfiguration();
        config.setMaxContextTokens(maxLength);
        ks.setConfig(config);
        
        kr = ks.apply(ki);
        
        context = kr.getContext();
        assertEquals(2, context.left.getMaxLength());
        assertEquals(2, context.right.getMaxLength());
        assertEquals(2, context.left.getLength());
        assertEquals(2, context.right.getLength());
        
        assertEquals(
                "... von 6,51 %. [[Er ist damit der sechsthäufigste Buchstabe in deutschen Texten]]. Mit Ausnahme ...",
                kr.getMatch(0).getSnippetBrackets());
        assertEquals(
                "<span class=\"context-left\"><span class=\"more\"></span>von 6,51 %. </span><span class=\"match\"><mark>Er ist damit der sechsthäufigste Buchstabe in deutschen Texten</mark></span><span class=\"context-right\">. Mit Ausnahme<span class=\"more\"></span></span>",
                kr.getMatch(0).getSnippetHTML());
    };
    
    
    @Test
    public void searchWithProperties () throws IOException {
        Krill ks = new Krill(jsonQuery);
        
        // limiting max context tokens
        Properties properties = KrillProperties.loadDefaultProperties();
        KrillConfiguration config = KrillConfiguration
                .createNewConfiguration(properties );
        ks.setConfig(config);
        
        Result kr = ks.apply(ki);
        
        SearchContext context = kr.getContext();
        assertEquals(3, context.left.getMaxLength());
        assertEquals(3, context.right.getMaxLength());
        assertEquals(3, context.left.getLength());
        assertEquals(3, context.right.getLength());
        
        assertEquals(
                "... Häufigkeit von 6,51 %. [[Er]<!>] ist damit der ...",
                kr.getMatch(0).getSnippetBrackets());
        assertEquals(
                "<span class=\"context-left\"><span class=\"more\"></span>Häufigkeit von 6,51 %. </span><span class=\"match\"><mark>Er</mark><span class=\"cutted\"></span></span><span class=\"context-right\"> ist damit der<span class=\"more\"></span></span>",
                kr.getMatch(0).getSnippetHTML());
    };
}
