package de.ids_mannheim.korap.index;

import static de.ids_mannheim.korap.TestSimple.getJsonString;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.response.SearchContext;
import de.ids_mannheim.korap.util.KrillConfiguration;
import de.ids_mannheim.korap.util.KrillProperties;

public class TestMaxContext {
    private static KrillIndex ki;
    private static String jsonQuery;
    public static ObjectMapper mapper = new ObjectMapper();
    
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
        assertEquals(60, context.left.getMaxTokenLength()); // default
        assertEquals(60, context.right.getMaxTokenLength());
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
        assertEquals(maxLength, context.left.getMaxTokenLength());
        assertEquals(maxLength, context.right.getMaxTokenLength());
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
    public void searchWithLargerContextTokenSize () {
        int maxLength = 600;
        KrillConfiguration config = new KrillConfiguration();
        config.setMaxContextTokens(maxLength);
        
        Krill ks = new Krill(jsonQuery);
        Result kr = ks.apply(ki);
        
        ks.setConfig(config);
        
        kr = ks.apply(ki);
        
        SearchContext context = kr.getContext();
        assertEquals(maxLength, context.left.getMaxTokenLength());
        assertEquals(maxLength, context.right.getMaxTokenLength());
        assertEquals(5, context.left.getLength());
        assertEquals(5, context.right.getLength());
    }
    
    @Test
    public void searchWithProperties () throws IOException {
        Krill ks = new Krill(jsonQuery);
        
        // limiting max context tokens
        Properties properties = KrillProperties.loadDefaultProperties();
        KrillConfiguration config = KrillConfiguration
                .createNewConfiguration(properties);
        ks.setConfig(config);
        
        Result kr = ks.apply(ki);
        
        SearchContext context = kr.getContext();
        assertEquals(3, context.left.getMaxTokenLength());
        assertEquals(3, context.right.getMaxTokenLength());
        assertEquals(3, context.left.getLength());
        assertEquals(3, context.right.getLength());
        
        assertEquals(
                "... Häufigkeit von 6,51 %. [[Er]<!>] ist damit der ...",
                kr.getMatch(0).getSnippetBrackets());
        assertEquals(
                "<span class=\"context-left\"><span class=\"more\"></span>Häufigkeit von 6,51 %. </span><span class=\"match\"><mark>Er</mark><span class=\"cutted\"></span></span><span class=\"context-right\"> ist damit der<span class=\"more\"></span></span>",
                kr.getMatch(0).getSnippetHTML());
    };
    
    @Test
    public void searchWithContextCharSize () throws JsonMappingException, JsonProcessingException {
        JsonNode jsonNode = mapper.readTree(jsonQuery);
        ArrayNode leftNode = (ArrayNode) jsonNode.at("/meta/context/left");
        ArrayNode rightNode = (ArrayNode) jsonNode.at("/meta/context/right");
        leftNode.set(0, "char");
        rightNode.set(0, "char");
        
        // limiting char context length
        int maxLength = 10;
        KrillConfiguration config = new KrillConfiguration();
        config.setMaxContextChars(maxLength);
        
        Krill ks = new Krill(jsonNode);
        ks.setConfig(config);
        Result kr = ks.apply(ki);
        
        SearchContext context = kr.getContext();
        assertEquals(maxLength, context.left.getMaxCharLength());
        assertEquals(maxLength, context.right.getMaxCharLength());
        assertEquals(5, context.left.getLength());
        assertEquals(5, context.right.getLength());
        
        leftNode.set(1, "15");
        rightNode.set(1, "15"); 
        searchWithLargerCharContextLength(jsonNode, config);
        testSettingLargerCharContextLength(jsonNode, config);
    }
    
    private void searchWithLargerCharContextLength (JsonNode jsonNode,
            KrillConfiguration config) {

        Krill ks = new Krill(jsonNode);
        ks.setConfig(config);
        Result kr = ks.apply(ki);
        
        SearchContext context = kr.getContext();
        assertEquals(config.getMaxContextChars(), context.left.getMaxCharLength());
        assertEquals(config.getMaxContextChars(), context.right.getMaxCharLength());
        assertEquals(10, context.left.getLength());
        assertEquals(10, context.right.getLength());
    }
    
    private void testSettingLargerCharContextLength (JsonNode jsonNode,
            KrillConfiguration config) {
        int maxLength = 600;
        config.setMaxContextChars(maxLength);
        assertEquals(maxLength, config.getMaxContextChars());
        
        Krill ks = new Krill(jsonNode);
        ks.setConfig(config);
        Result kr = ks.apply(ki);
     
        SearchContext context = kr.getContext();
        assertEquals(config.getMaxContextChars(), context.left.getMaxCharLength());
        assertEquals(config.getMaxContextChars(), context.right.getMaxCharLength());
        assertEquals(15, context.left.getLength());
        assertEquals(15, context.right.getLength());        
    }
}
