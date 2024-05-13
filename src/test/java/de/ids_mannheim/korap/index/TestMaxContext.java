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
                "... sechsthäufigste Buchstabe in deutschen Texten. [[Mit Ausnahme von Fremdwörtern und Namen ist das A der einzige Buchstabe im Deutschen, der zweifach am Anfang eines Wortes stehen darf, etwa im Wort Aal]]. 1. Aussprache Im Deutschen und ...",
                kr.getMatch(0).getSnippetBrackets());
        assertEquals(
                "<span class=\"context-left\"><span class=\"more\"></span>sechsthäufigste Buchstabe in deutschen Texten. </span><span class=\"match\"><mark>Mit Ausnahme von Fremdwörtern und Namen ist das A der einzige Buchstabe im Deutschen, der zweifach am Anfang eines Wortes stehen darf, etwa im Wort Aal</mark></span><span class=\"context-right\">. 1. Aussprache Im Deutschen und<span class=\"more\"></span></span>",
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
                "... deutschen Texten. [[Mit Ausnahme von Fremdwörtern und Namen ist das A der einzige Buchstabe im Deutschen, der zweifach am Anfang eines Wortes stehen darf, etwa im Wort Aal]]. 1. Aussprache ...",
                kr.getMatch(0).getSnippetBrackets());
        assertEquals(
                "<span class=\"context-left\"><span class=\"more\"></span>deutschen Texten. </span><span class=\"match\"><mark>Mit Ausnahme von Fremdwörtern und Namen ist das A der einzige Buchstabe im Deutschen, der zweifach am Anfang eines Wortes stehen darf, etwa im Wort Aal</mark></span><span class=\"context-right\">. 1. Aussprache<span class=\"more\"></span></span>",
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
                "... in deutschen Texten. [[Mit]<!>] Ausnahme von Fremdwörtern ...",
                kr.getMatch(0).getSnippetBrackets());
        assertEquals(
                "<span class=\"context-left\"><span class=\"more\"></span>in deutschen Texten. </span><span class=\"match\"><mark>Mit</mark><span class=\"cutted\"></span></span><span class=\"context-right\"> Ausnahme von Fremdwörtern<span class=\"more\"></span></span>",
                kr.getMatch(0).getSnippetHTML());
    };
}
