package de.ids_mannheim.korap.index;

import static de.ids_mannheim.korap.TestSimple.getJsonString;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.response.SearchContext;
import de.ids_mannheim.korap.util.KrillConfiguration;

public class TestMaxContext {

    @Test
    public void searchWithContextTokenSize () throws IOException {
        KrillIndex ki = new KrillIndex();
        for (String i : new String[] { "00001" }) {
            ki.addDoc(getClass().getResourceAsStream("/wiki/" + i + ".json.gz"),
                    true);
        };
        ki.commit();

        String json = getJsonString(getClass()
                .getResource("/queries/position/sentence-contain-token.json").getFile());

        Krill ks = new Krill(json);
        
        // limiting max context tokens
        int maxLength = 2;
        KrillConfiguration config = new KrillConfiguration();
        config.setMaxContextTokens(maxLength);
        ks.setConfig(config);
        
        Result kr = ks.apply(ki);
        
        SearchContext context = kr.getContext();
        assertEquals(2, context.left.getMaxLength());
        assertEquals(2, context.right.getMaxLength());
        
        assertEquals(
                "... ein Vokal. [[Der Buchstabe A hat in deutschen Texten eine durchschnittliche Häufigkeit von 6,51 %.]] Er ist ...",
                kr.getMatch(0).getSnippetBrackets());
        assertEquals(
                "<span class=\"context-left\"><span class=\"more\"></span>ein Vokal. </span><span class=\"match\"><mark>Der Buchstabe A hat in deutschen Texten eine durchschnittliche Häufigkeit von 6,51 %.</mark></span><span class=\"context-right\"> Er ist<span class=\"more\"></span></span>",
                kr.getMatch(0).getSnippetHTML());
    };
}
