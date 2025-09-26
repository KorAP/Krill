package de.ids_mannheim.korap.index;

import static de.ids_mannheim.korap.TestSimple.getJsonString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.After;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import org.apache.lucene.index.Term;
import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.response.Match;
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.response.SearchContext;
import de.ids_mannheim.korap.util.KrillProperties;
import org.apache.lucene.search.spans.SpanTermQuery;


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

    @After
    public void resetGlobals() {
        KrillProperties.leftContextAdjustment = 0;
        KrillProperties.rightContextAdjustment = 0;
    };

    @Test
    public void testSmallerTokenContextSize () throws IOException {
        
        assertEquals(25, KrillProperties.maxTokenContextSize);

        Krill ks = new Krill(jsonQuery);
        Result kr = ks.apply(ki);

        SearchContext context = kr.getContext();
        assertEquals(KrillProperties.maxTokenContextSize,
                context.left.getMaxTokenLength()); // default
        assertEquals(KrillProperties.maxTokenContextSize,
                context.right.getMaxTokenLength());
        assertEquals(5, context.left.getLength());
        assertEquals(5, context.right.getLength());

        Match km = kr.getMatch(0);
        assertEquals(5, km.getContext().left.getLength());
        assertEquals(5, km.getContext().right.getLength());
    };
    
    @Test
    public void searchWithLargerContextTokenSize ()
            throws JsonMappingException, JsonProcessingException {
        String query = new String(jsonQuery);
        JsonNode jsonNode = mapper.readTree(query);
        ArrayNode leftNode = (ArrayNode) jsonNode.at("/meta/context/left");
        ArrayNode rightNode = (ArrayNode) jsonNode.at("/meta/context/right");
        leftNode.set(1, "70");
        rightNode.set(1, "70");

        Krill ks = new Krill(jsonNode);
        Result kr = ks.apply(ki);

        SearchContext context = kr.getContext();
        assertEquals(KrillProperties.maxTokenContextSize,
                context.left.getLength());
        assertEquals(KrillProperties.maxTokenContextSize,
                context.right.getLength());

        Match km = kr.getMatch(0);
        assertEquals(KrillProperties.maxTokenContextSize,
                km.getContext().left.getLength());
        assertEquals(KrillProperties.maxTokenContextSize,
                km.getContext().right.getLength());
        
        String rightContext = km.getSnippetBrackets().split("]]")[1];
        assertEquals(KrillProperties.maxTokenContextSize,
                rightContext.split(" ").length - 2);
    }


    @Test
    public void searchWithLargerContextCharSize ()
        throws JsonMappingException, JsonProcessingException {
        JsonNode jsonNode = mapper.readTree(jsonQuery);
        ArrayNode leftNode = (ArrayNode) jsonNode.at("/meta/context/left");
        ArrayNode rightNode = (ArrayNode) jsonNode.at("/meta/context/right");
        leftNode.set(0, "char");
        rightNode.set(0, "char");
        leftNode.set(1, "600");
        rightNode.set(1, "600");

        Krill ks = new Krill(jsonNode);
        Result kr = ks.apply(ki);

        SearchContext context = kr.getContext();
        assertEquals(KrillProperties.maxCharContextSize,
                context.left.getLength());
        assertEquals(KrillProperties.maxCharContextSize,
                context.right.getLength());

        Match km = kr.getMatch(0);
        assertEquals(KrillProperties.maxCharContextSize,
                km.getContext().left.getLength());
        assertEquals(KrillProperties.maxCharContextSize,
                km.getContext().right.getLength());
        
        String rightContext = km.getSnippetBrackets().split("]]")[1];
        assertEquals(KrillProperties.maxCharContextSize,rightContext.length() -4);
    }
    
    // for Kokokom
    @Test
    public void testIncreaseDefaultSearchContextSize () throws IOException {
        KrillProperties.defaultSearchContextLength = 1000000000;
        
        String jsonQuery = getJsonString(TestMaxContext.class
                .getResource("/queries/flags/caseInsensitive.jsonld")
                .getFile());
        Krill ks = new Krill(jsonQuery);
        Result kr = ks.apply(ki);
        Match km = kr.getMatch(0);
        assertEquals(6089, km.getSnippetBrackets().length());
        KrillProperties.defaultSearchContextLength = 6;
    };


    @Test
    public void testTokenSnippetMatchLength1 () throws IOException {       
        SpanTermQuery stq = new SpanTermQuery(new Term("tokens", "s:des"));
        Result kr = ki.search(stq, (short) 10);
        
        Match km = kr.getMatch(0);
        assertEquals(7, km.getStartPos());
        assertEquals(8, km.getEndPos());
        assertEquals(6, km.getContext().left.getLength());
        assertEquals(6, km.getContext().right.getLength());
        

        assertEquals("{\"left\":[\"bzw.\",\"a\",\"ist\",\"der\",\"erste\",\"Buchstabe\"]," +
                     "\"match\":[\"des\"]," +
                     "\"right\":[\"lateinischen\",\"Alphabets\",\"und\",\"ein\",\"Vokal\"]}",
                     kr.getMatch(0).getSnippetTokens().toString());

        KrillProperties.leftContextAdjustment = 1;
        KrillProperties.rightContextAdjustment = 1;

        // Shrinks the left context by 1 - as that is the match length - although it could be 2
        assertEquals("{\"left\":[\"gibt\",\"es\",\"zwei\",\"verschiedene\",\"Phoneme\"],"+
                     "\"match\":[\"des\"],"+
                     "\"right\":[\"Vokals\",\"den\",\"Kurzvokal\",\"a,\",\"wie\"]}",
                     kr.getMatch(1).getSnippetTokens().toString());

        KrillProperties.leftContextAdjustment = 5;
        KrillProperties.rightContextAdjustment = 5;

        // Shrinks the left context by 1 - as that is the match length - although it could be 10
        assertEquals("{\"left\":[\"B.\",\"in\",\"Rat\",\"Die\",\"Länge\"],"+
                     "\"match\":[\"des\"],"+
                     "\"right\":[\"Vokals\",\"ist\",\"unterschiedlich\",\"gekennzeichnet\",\"Langer\"]}",
                     kr.getMatch(2).getSnippetTokens().toString());
    };

    @Test
    public void testTokenSnippetMatchLengthLong () throws JsonMappingException, JsonProcessingException {       
        JsonNode jsonNode = mapper.readTree(jsonQuery);
        Krill ks = new Krill(jsonNode);
        Result kr = ks.apply(ki);

        Match km = kr.getMatch(0);
        assertEquals(34, km.getStartPos());
        assertEquals(60, km.getEndPos());
        assertEquals(5, km.getContext().left.getLength());
        assertEquals(5, km.getContext().right.getLength());

        String snippetToken = kr.getMatch(0).getSnippetTokens().toString();
        assertTrue(snippetToken.contains(
                       "\"left\":[\"sechsthäufigste\",\"Buchstabe\",\"in\",\"deutschen\",\"Texten\"]"
                       )
            );
        assertTrue(snippetToken.contains(
                       "\"right\":[\"1.\",\"Aussprache\",\"Im\",\"Deutschen\"]"
                       )
            );

        String snippetHTML = kr.getMatch(0).getSnippetHTML();
        assertTrue(snippetHTML.contains("<span class=\"context-left\"><span class=\"more\"></span>sechsthäufigste Buchstabe in deutschen Texten. </span>"));
        assertTrue(snippetHTML.contains("<span class=\"context-right\">. 1. Aussprache Im Deutschen und<span class=\"more\"></span></span>"));
    };

    @Test
    public void testTokenSnippetMatchLengthLong2 () throws JsonMappingException, JsonProcessingException {       
        JsonNode jsonNode = mapper.readTree(jsonQuery);
        Krill ks = new Krill(jsonNode);
        Result kr = ks.apply(ki);

        Match km = kr.getMatch(0);
        assertEquals(34, km.getStartPos());
        assertEquals(60, km.getEndPos());
        assertEquals(5, km.getContext().left.getLength());
        assertEquals(5, km.getContext().right.getLength());

        KrillProperties.leftContextAdjustment = 1;
        KrillProperties.rightContextAdjustment = 1;

        String snippetToken = kr.getMatch(0).getSnippetTokens().toString();

        assertTrue(snippetToken.contains(
                       "\"left\":[\"Buchstabe\",\"in\",\"deutschen\",\"Texten\"]"
                       )
            );

        assertTrue(snippetToken.contains(
                       "\"right\":[\"1.\",\"Aussprache\",\"Im\"]"
                       )
            );

        String snippetHTML = kr.getMatch(0).getSnippetHTML();
        assertTrue(snippetHTML.contains("<span class=\"context-left\"><span class=\"more\"></span>Buchstabe in deutschen Texten. </span>"));
        assertTrue(snippetHTML.contains("<span class=\"context-right\">. 1. Aussprache Im Deutschen<span class=\"more\"></span></span>"));

    };

    @Test
    public void testTokenSnippetMatchLengthLong3 () throws JsonMappingException, JsonProcessingException {       
        JsonNode jsonNode = mapper.readTree(jsonQuery);
        Krill ks = new Krill(jsonNode);
        Result kr = ks.apply(ki);

        Match km = kr.getMatch(0);
        assertEquals(34, km.getStartPos());
        assertEquals(60, km.getEndPos());
        assertEquals(5, km.getContext().left.getLength());
        assertEquals(5, km.getContext().right.getLength());

        KrillProperties.leftContextAdjustment = 4;
        KrillProperties.rightContextAdjustment = 2;

        String snippetToken = kr.getMatch(0).getSnippetTokens().toString();

        assertTrue(snippetToken.contains(
                       "\"left\":[\"Texten\"]"
                       )
            );

        assertTrue(snippetToken.contains(
                       "\"right\":[\"1.\",\"Aussprache\"]"
                       )
            );

        String snippetHTML = kr.getMatch(0).getSnippetHTML();
        assertTrue(snippetHTML.contains("<span class=\"context-left\"><span class=\"more\"></span>Texten. </span>"));
        assertTrue(snippetHTML.contains("<span class=\"context-right\">. 1. Aussprache Im<span class=\"more\"></span></span>"));

    };

    @Test
    public void testTokenSnippetMatchLengthLong4 () throws JsonMappingException, JsonProcessingException {       
        int before = KrillProperties.maxTokenMatchSize;
        KrillProperties.maxTokenMatchSize = 5;
        JsonNode jsonNode = mapper.readTree(jsonQuery);
        Krill ks = new Krill(jsonNode);
        Result kr = ks.apply(ki);

        Match km = kr.getMatch(0);
        assertEquals(34, km.getStartPos());
        assertEquals(39, km.getEndPos());
        assertEquals(5, km.getContext().left.getLength());
        assertEquals(5, km.getContext().right.getLength());

        KrillProperties.leftContextAdjustment = 5;
        KrillProperties.rightContextAdjustment = 0;

        String snippetToken = kr.getMatch(0).getSnippetTokens().toString();
        KrillProperties.maxTokenMatchSize = before;

        assertTrue(!snippetToken.contains("\"left\""));
        assertTrue(snippetToken.contains(
                       "\"match\":[\"Mit\",\"Ausnahme\",\"von\",\"Fremdwörtern\",\"und\"]"
                       )
            );

        // TODO: This should be one token longer
        assertTrue(snippetToken.contains(
                       "\"right\":[\"Namen\",\"ist\",\"das\",\"A\"]"
                       )
            );

        String snippetHTML = kr.getMatch(0).getSnippetHTML();
        assertTrue(snippetHTML.contains("<span class=\"context-left\"><span class=\"more\"></span></span>"));
        assertTrue(snippetHTML.contains("<span class=\"match\"><mark>Mit Ausnahme von Fremdwörtern und</mark><span class=\"cutted\"></span></span>"));
        assertTrue(snippetHTML.contains("<span class=\"context-right\"> Namen ist das A der<span class=\"more\"></span></span>"));
    };


    @Test
    public void testTokenSnippetMatchLengthLong5 () throws JsonMappingException, JsonProcessingException {       
        int before = KrillProperties.maxTokenMatchSize;
        KrillProperties.maxTokenMatchSize = 5;
        JsonNode jsonNode = mapper.readTree(jsonQuery);
        Krill ks = new Krill(jsonNode);
        Result kr = ks.apply(ki);

        Match km = kr.getMatch(0);
        assertEquals(34, km.getStartPos());
        assertEquals(39, km.getEndPos());
        assertEquals(5, km.getContext().left.getLength());
        assertEquals(5, km.getContext().right.getLength());

        // Adjust all context for the matchsize
        KrillProperties.leftContextAdjustment = 5;
        KrillProperties.rightContextAdjustment = 5;

        // 5 Context tokens are left (as the match is only 5 tokens long)
        // TODO: This is missing one token at the right side at the moment
        String snippetToken = kr.getMatch(0).getSnippetTokens().toString();
        KrillProperties.maxTokenMatchSize = before;

        // TODO: Right context need one more token
        assertEquals("{\"left\":[\"deutschen\",\"Texten\"],\"match\":[\"Mit\",\"Ausnahme\",\"von\",\"Fremdwörtern\",\"und\"],\"right\":[\"Namen\",\"ist\"]}", snippetToken);

        String snippetHTML = kr.getMatch(0).getSnippetHTML();
        assertTrue(snippetHTML.contains("<span class=\"context-left\"><span class=\"more\"></span>deutschen Texten. </span>"));
        assertTrue(snippetHTML.contains("<span class=\"match\"><mark>Mit Ausnahme von Fremdwörtern und</mark><span class=\"cutted\"></span></span>"));
        assertTrue(snippetHTML.contains("<span class=\"context-right\"> Namen ist das<span class=\"more\"></span></span>"));
    };

    @Test
    public void testTokenSnippetMatchLengthLongAllKwic () throws JsonMappingException, JsonProcessingException {       
        int before = KrillProperties.maxTokenMatchSize;
        KrillProperties.maxTokenMatchSize = 10;
        JsonNode jsonNode = mapper.readTree(jsonQuery);
        Krill ks = new Krill(jsonNode);
        Result kr = ks.apply(ki);

        Match km = kr.getMatch(0);
        assertEquals(34, km.getStartPos());
        assertEquals(44, km.getEndPos());
        assertEquals(5, km.getContext().left.getLength());
        assertEquals(5, km.getContext().right.getLength());

        // Adjust all context for the matchsize
        KrillProperties.leftContextAdjustment = 5;
        KrillProperties.rightContextAdjustment = 5;
        String snippetToken = kr.getMatch(0).getSnippetTokens().toString();
        KrillProperties.maxTokenMatchSize = before;

        assertEquals("{\"match\":[\"Mit\",\"Ausnahme\",\"von\",\"Fremdwörtern\",\"und\",\"Namen\",\"ist\",\"das\",\"A\",\"der\"]}", snippetToken);

        String snippetHTML = kr.getMatch(0).getSnippetHTML();
        assertTrue(snippetHTML.contains("<span class=\"context-left\"><span class=\"more\"></span></span>"));
        assertTrue(snippetHTML.contains("<span class=\"match\"><mark>Mit Ausnahme von Fremdwörtern und Namen ist das A der</mark><span class=\"cutted\"></span></span>"));
        assertTrue(snippetHTML.contains("<span class=\"context-right\"><span class=\"more\"></span></span>"));

    };
};
