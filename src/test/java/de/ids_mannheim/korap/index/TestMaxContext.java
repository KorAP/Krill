package de.ids_mannheim.korap.index;

import static de.ids_mannheim.korap.TestSimple.getJsonString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Properties;

import org.junit.After;
import org.junit.BeforeClass;
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

    private int savedMaxTokenMatchSize;
    private int savedMaxTokenContextSize;
    private int savedMaxCharContextSize;
    private int savedDefaultSearchContextLength;
    private boolean savedMatchExpansionIncludeContextSize;

    @org.junit.Before
    public void saveGlobals() {
        savedMaxTokenMatchSize = KrillProperties.maxTokenMatchSize;
        savedMaxTokenContextSize = KrillProperties.maxTokenContextSize;
        savedMaxCharContextSize = KrillProperties.maxCharContextSize;
        savedDefaultSearchContextLength = KrillProperties.defaultSearchContextLength;
        savedMatchExpansionIncludeContextSize = KrillProperties.matchExpansionIncludeContextSize;
    };

    @After
    public void resetGlobals() {
        KrillProperties.leftContextMaxShrink = 0;
        KrillProperties.rightContextMaxShrink = 0;
        KrillProperties.kwicMaxToken = -1;
        KrillProperties.maxTokenMatchSize = savedMaxTokenMatchSize;
        KrillProperties.maxTokenContextSize = savedMaxTokenContextSize;
        KrillProperties.maxCharContextSize = savedMaxCharContextSize;
        KrillProperties.defaultSearchContextLength = savedDefaultSearchContextLength;
        KrillProperties.matchExpansionIncludeContextSize = savedMatchExpansionIncludeContextSize;
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
                     "\"right\":[\"lateinischen\",\"Alphabets\",\"und\",\"ein\",\"Vokal\",\"Der\"]}",
                     kr.getMatch(0).getSnippetTokens().toString());

        KrillProperties.leftContextMaxShrink = 1;
        KrillProperties.rightContextMaxShrink = 1;

        // Shrinks the left context by 1 - as that is the match length - although it could be 2
        assertEquals("{\"left\":[\"gibt\",\"es\",\"zwei\",\"verschiedene\",\"Phoneme\"],"+
                     "\"match\":[\"des\"],"+
                     "\"right\":[\"Vokals\",\"den\",\"Kurzvokal\",\"a,\",\"wie\",\"z\"]}",
                     kr.getMatch(1).getSnippetTokens().toString());

        KrillProperties.leftContextMaxShrink = 5;
        KrillProperties.rightContextMaxShrink = 5;

        // Shrinks the left context by 1 - as that is the match length - although it could be 10
        assertEquals("{\"left\":[\"B.\",\"in\",\"Rat\",\"Die\",\"Länge\"],"+
                     "\"match\":[\"des\"],"+
                     "\"right\":[\"Vokals\",\"ist\",\"unterschiedlich\",\"gekennzeichnet\",\"Langer\",\"Vokal\"]}",
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
                       "\"right\":[\"1.\",\"Aussprache\",\"Im\",\"Deutschen\",\"und\"]"
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

        KrillProperties.leftContextMaxShrink = 1;
        KrillProperties.rightContextMaxShrink = 1;

        String snippetToken = kr.getMatch(0).getSnippetTokens().toString();

        assertTrue(snippetToken.contains(
                       "\"left\":[\"Buchstabe\",\"in\",\"deutschen\",\"Texten\"]"
                       )
            );

        assertTrue(snippetToken.contains(
                       "\"right\":[\"1.\",\"Aussprache\",\"Im\",\"Deutschen\"]"
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

        KrillProperties.leftContextMaxShrink = 4;
        KrillProperties.rightContextMaxShrink = 2;

        String snippetToken = kr.getMatch(0).getSnippetTokens().toString();

        assertTrue(snippetToken.contains(
                       "\"left\":[\"Texten\"]"
                       )
            );

        assertTrue(snippetToken.contains(
                       "\"right\":[\"1.\",\"Aussprache\",\"Im\"]"
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

        KrillProperties.leftContextMaxShrink = 5;
        KrillProperties.rightContextMaxShrink = 0;

        String snippetToken = kr.getMatch(0).getSnippetTokens().toString();
        KrillProperties.maxTokenMatchSize = before;

        assertTrue(!snippetToken.contains("\"left\""));
        assertTrue(snippetToken.contains(
                       "\"match\":[\"Mit\",\"Ausnahme\",\"von\",\"Fremdwörtern\",\"und\"]"
                       )
            );

        assertTrue(snippetToken.contains(
                       "\"right\":[\"Namen\",\"ist\",\"das\",\"A\",\"der\"]"
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
        KrillProperties.leftContextMaxShrink = 5;
        KrillProperties.rightContextMaxShrink = 5;

        String snippetToken = kr.getMatch(0).getSnippetTokens().toString();
        KrillProperties.maxTokenMatchSize = before;

        assertEquals("{\"left\":[\"deutschen\",\"Texten\"],\"match\":[\"Mit\",\"Ausnahme\",\"von\",\"Fremdwörtern\",\"und\"],\"right\":[\"Namen\",\"ist\",\"das\"]}", snippetToken);

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
        KrillProperties.leftContextMaxShrink = 5;
        KrillProperties.rightContextMaxShrink = 5;
        String snippetToken = kr.getMatch(0).getSnippetTokens().toString();
        KrillProperties.maxTokenMatchSize = before;

        assertEquals("{\"match\":[\"Mit\",\"Ausnahme\",\"von\",\"Fremdwörtern\",\"und\",\"Namen\",\"ist\",\"das\",\"A\",\"der\"]}", snippetToken);

        String snippetHTML = kr.getMatch(0).getSnippetHTML();
        assertTrue(snippetHTML.contains("<span class=\"context-left\"><span class=\"more\"></span></span>"));
        assertTrue(snippetHTML.contains("<span class=\"match\"><mark>Mit Ausnahme von Fremdwörtern und Namen ist das A der</mark><span class=\"cutted\"></span></span>"));
        assertTrue(snippetHTML.contains("<span class=\"context-right\"><span class=\"more\"></span></span>"));

    };

    @Test
    public void testUpdateConfigurationsMax () {
        Properties props = new Properties();
        props.setProperty("krill.context.left.maxShrink", "max");
        props.setProperty("krill.context.right.maxShrink", "max");
        KrillProperties.updateConfigurations(props);
        assertEquals(KrillProperties.maxTokenContextSize,
                     KrillProperties.leftContextMaxShrink);
        assertEquals(KrillProperties.maxTokenContextSize,
                     KrillProperties.rightContextMaxShrink);
    };

    @Test
    public void testUpdateConfigurationsEdgeCases () {
        Properties props = new Properties();

        // Negative values should be clamped to 0
        props.setProperty("krill.context.left.maxShrink", "-5");
        props.setProperty("krill.context.right.maxShrink", "-10");
        KrillProperties.updateConfigurations(props);
        assertEquals(0, KrillProperties.leftContextMaxShrink);
        assertEquals(0, KrillProperties.rightContextMaxShrink);

        // Values exceeding maxTokenContextSize should be clamped
        props.setProperty("krill.context.left.maxShrink", "9999");
        props.setProperty("krill.context.right.maxShrink", "9999");
        KrillProperties.updateConfigurations(props);
        assertEquals(KrillProperties.maxTokenContextSize,
                     KrillProperties.leftContextMaxShrink);
        assertEquals(KrillProperties.maxTokenContextSize,
                     KrillProperties.rightContextMaxShrink);

        // Normal value
        props.setProperty("krill.context.left.maxShrink", "3");
        props.setProperty("krill.context.right.maxShrink", "7");
        KrillProperties.updateConfigurations(props);
        assertEquals(3, KrillProperties.leftContextMaxShrink);
        assertEquals(7, KrillProperties.rightContextMaxShrink);
    };

    @Test
    public void testTokenSnippetMatchLengthLongRightOnly ()
        throws JsonMappingException, JsonProcessingException {
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

        KrillProperties.leftContextMaxShrink = 0;
        KrillProperties.rightContextMaxShrink = 5;

        String snippetToken = kr.getMatch(0).getSnippetTokens().toString();
        KrillProperties.maxTokenMatchSize = before;

        assertTrue(snippetToken.contains(
                       "\"left\":[\"sechsthäufigste\",\"Buchstabe\",\"in\",\"deutschen\",\"Texten\"]"
                       ));
        assertTrue(snippetToken.contains(
                       "\"match\":[\"Mit\",\"Ausnahme\",\"von\",\"Fremdwörtern\",\"und\"]"
                       ));
        assertTrue(!snippetToken.contains("\"right\""));
    };

    @Test
    public void testSnippetBracketsWithAdjustment ()
        throws JsonMappingException, JsonProcessingException {
        JsonNode jsonNode = mapper.readTree(jsonQuery);
        Krill ks = new Krill(jsonNode);
        Result kr = ks.apply(ki);

        Match km = kr.getMatch(0);
        assertEquals(34, km.getStartPos());
        assertEquals(60, km.getEndPos());

        KrillProperties.leftContextMaxShrink = 4;
        KrillProperties.rightContextMaxShrink = 2;

        String brackets = kr.getMatch(0).getSnippetBrackets();

        assertTrue(brackets.contains("Texten."));
        assertTrue(!brackets.contains("sechsthäufigste"));
        assertTrue(brackets.contains("[["));
        assertTrue(brackets.contains("]]"));
        assertTrue(brackets.contains("Aussprache Im"));
        assertTrue(!brackets.contains("Deutschen und"));
    };

    @Test
    public void testSmallClientContextWithLargeAdjustment ()
        throws IOException {
        SpanTermQuery stq = new SpanTermQuery(new Term("tokens", "s:des"));
        Result kr = ki.search(stq, (short) 10);

        KrillProperties.leftContextMaxShrink = 25;
        KrillProperties.rightContextMaxShrink = 25;

        Match km = kr.getMatch(0);
        String snippetToken = km.getSnippetTokens().toString();

        assertTrue(snippetToken.contains("\"match\":[\"des\"]"));

        String snippetHTML = km.getSnippetHTML();
        assertTrue(snippetHTML.contains("<span class=\"match\">"));

        km = kr.getMatch(1);
        snippetToken = km.getSnippetTokens().toString();
        assertTrue(snippetToken.contains("\"match\":[\"des\"]"));
    };

    @Test
    public void testGuardClampsOverShrinkWithLongMatch ()
        throws JsonMappingException, JsonProcessingException {
        // Use a small context (2 tokens) with the sentence query (match is 26 tokens)
        // and a large adjustment. The guard must clamp shrink to available context.
        JsonNode jsonNode = mapper.readTree(jsonQuery);
        ArrayNode leftNode = (ArrayNode) jsonNode.at("/meta/context/left");
        ArrayNode rightNode = (ArrayNode) jsonNode.at("/meta/context/right");
        leftNode.set(1, "2");
        rightNode.set(1, "2");

        Krill ks = new Krill(jsonNode);
        Result kr = ks.apply(ki);

        Match km = kr.getMatch(0);
        assertEquals(34, km.getStartPos());
        assertEquals(60, km.getEndPos());
        assertEquals(2, km.getContext().left.getLength());
        assertEquals(2, km.getContext().right.getLength());

        // Set adjustment much larger than available context
        KrillProperties.leftContextMaxShrink = 25;
        KrillProperties.rightContextMaxShrink = 25;

        // Without the guard, shrinkLeft/shrinkRight would be 13 each (half of 26),
        // but available context is only 2 on each side.
        // The guard clamps to 2, so all context is consumed.
        String snippetToken = km.getSnippetTokens().toString();
        assertTrue(!snippetToken.contains("\"left\""));
        assertTrue(!snippetToken.contains("\"right\""));
        assertTrue(snippetToken.contains("\"match\""));

        String snippetHTML = km.getSnippetHTML();
        assertTrue(snippetHTML.contains("<span class=\"context-left\"><span class=\"more\"></span></span>"));
        assertTrue(snippetHTML.contains("<span class=\"context-right\"><span class=\"more\"></span></span>"));
    };

    @Test
    public void testMaxCharContextSizeProperty () {
        Properties props = new Properties();
        props.setProperty("krill.context.max.char", "100");
        KrillProperties.updateConfigurations(props);
        assertEquals(100, KrillProperties.maxCharContextSize);
    };

    @Test
    public void testMaxCharContextClampsBracketsSnippet ()
            throws JsonMappingException, JsonProcessingException {
        KrillProperties.maxCharContextSize = 50;

        JsonNode jsonNode = mapper.readTree(jsonQuery);
        ArrayNode leftNode = (ArrayNode) jsonNode.at("/meta/context/left");
        ArrayNode rightNode = (ArrayNode) jsonNode.at("/meta/context/right");
        leftNode.set(0, "char");
        rightNode.set(0, "char");
        leftNode.set(1, "200");
        rightNode.set(1, "200");

        Krill ks = new Krill(jsonNode);
        Result kr = ks.apply(ki);

        SearchContext context = kr.getContext();
        assertEquals(50, context.left.getLength());
        assertEquals(50, context.right.getLength());

        Match km = kr.getMatch(0);
        assertEquals(50, km.getContext().left.getLength());
        assertEquals(50, km.getContext().right.getLength());

        String rightContext = km.getSnippetBrackets().split("]]")[1];
        assertEquals(50, rightContext.length() - 4);
    };

    @Test
    public void testKwicMaxTokenBasic () {
        // kwic.max.token = matchMax + 2*contextMax - totalShrink
        // Setting kwicMaxToken=60 with matchMax=50 and contextMax=25:
        // maxShrink = contextMax*2 + matchMax - kwicMaxToken = 25*2 + 50 - 60 = 40
        // split evenly: leftMaxShrink=20, rightMaxShrink=20
        Properties props = new Properties();
        props.setProperty("krill.kwic.max.token", "60");
        KrillProperties.updateConfigurations(props);
        assertEquals(60, KrillProperties.kwicMaxToken);
        assertEquals(20, KrillProperties.leftContextMaxShrink);
        assertEquals(20, KrillProperties.rightContextMaxShrink);
    };

    @Test
    public void testKwicMaxTokenOverridesIndividual () {
        // When kwic.max.token is set, individual maxShrink values are ignored
        Properties props = new Properties();
        props.setProperty("krill.context.left.maxShrink", "3");
        props.setProperty("krill.context.right.maxShrink", "7");
        props.setProperty("krill.kwic.max.token", "60");
        KrillProperties.updateConfigurations(props);
        assertEquals(60, KrillProperties.kwicMaxToken);
        // Derived from kwic.max.token, not from the individual values
        assertEquals(20, KrillProperties.leftContextMaxShrink);
        assertEquals(20, KrillProperties.rightContextMaxShrink);
    };

    @Test
    public void testKwicMaxTokenEqualsTotalAllowance () {
        // kwicMaxToken = matchMax + 2*contextMax means no shrink needed
        // 50 + 2*25 = 100
        Properties props = new Properties();
        props.setProperty("krill.kwic.max.token", "100");
        KrillProperties.updateConfigurations(props);
        assertEquals(100, KrillProperties.kwicMaxToken);
        assertEquals(0, KrillProperties.leftContextMaxShrink);
        assertEquals(0, KrillProperties.rightContextMaxShrink);
    };

    @Test
    public void testKwicMaxTokenSmall () {
        // kwicMaxToken = matchMax means context fully shrinks
        // maxShrink = 25*2 + 50 - 50 = 50, clamped to 2*contextMax = 50
        // split evenly: 25/25
        Properties props = new Properties();
        props.setProperty("krill.kwic.max.token", "50");
        KrillProperties.updateConfigurations(props);
        assertEquals(50, KrillProperties.kwicMaxToken);
        assertEquals(25, KrillProperties.leftContextMaxShrink);
        assertEquals(25, KrillProperties.rightContextMaxShrink);
    };

    @Test
    public void testKwicMaxTokenTooSmall () {
        // kwicMaxToken below matchMax is clamped: shrink can't exceed 2*contextMax
        Properties props = new Properties();
        props.setProperty("krill.kwic.max.token", "10");
        KrillProperties.updateConfigurations(props);
        assertEquals(10, KrillProperties.kwicMaxToken);
        assertEquals(25, KrillProperties.leftContextMaxShrink);
        assertEquals(25, KrillProperties.rightContextMaxShrink);
    };

    @Test
    public void testKwicMaxTokenWithSearch ()
        throws JsonMappingException, JsonProcessingException {
        // End-to-end test: kwic.max.token=15, matchMax=5, contextMax=25
        // maxShrink = 25*2+5-15 = 40, clamped to 50 = 20/20
        // But context is only 5 on each side (query requests 5).
        // With a 5-token match, requiredShrink = min(5, 40) = 5, split 2/3
        int before = KrillProperties.maxTokenMatchSize;
        KrillProperties.maxTokenMatchSize = 5;

        Properties props = new Properties();
        props.setProperty("krill.kwic.max.token", "15");
        KrillProperties.updateConfigurations(props);

        JsonNode jsonNode = mapper.readTree(jsonQuery);
        Krill ks = new Krill(jsonNode);
        Result kr = ks.apply(ki);

        Match km = kr.getMatch(0);
        assertEquals(34, km.getStartPos());
        assertEquals(39, km.getEndPos());
        assertEquals(5, km.getContext().left.getLength());
        assertEquals(5, km.getContext().right.getLength());

        String snippetToken = km.getSnippetTokens().toString();
        KrillProperties.maxTokenMatchSize = before;

        // maxShrink=40 = left=20,right=20; match=5; requiredShrink=min(5,40)=5
        // shrinkLeft = round(5*(20/40))=round(2.5)=3, shrinkRight=2
        // left context: 5-3=2, right context: 5-2=3
        assertTrue(snippetToken.contains("\"left\":[\"deutschen\",\"Texten\"]"));
        assertTrue(snippetToken.contains(
                       "\"match\":[\"Mit\",\"Ausnahme\",\"von\",\"Fremdwörtern\",\"und\"]"
                       ));
        assertTrue(snippetToken.contains("\"right\":[\"Namen\",\"ist\",\"das\"]"));
    };

    @Test
    public void testKwicMaxTokenNotSet () {
        // When kwic.max.token is not set, individual maxShrink values work normally
        Properties props = new Properties();
        props.setProperty("krill.context.left.maxShrink", "3");
        props.setProperty("krill.context.right.maxShrink", "7");
        KrillProperties.updateConfigurations(props);
        assertEquals(-1, KrillProperties.kwicMaxToken);
        assertEquals(3, KrillProperties.leftContextMaxShrink);
        assertEquals(7, KrillProperties.rightContextMaxShrink);
    };

    @Test
    public void testMaxCharContextClampsHTMLSnippet ()
            throws JsonMappingException, JsonProcessingException {
        KrillProperties.maxCharContextSize = 50;

        JsonNode jsonNode = mapper.readTree(jsonQuery);
        ArrayNode leftNode = (ArrayNode) jsonNode.at("/meta/context/left");
        ArrayNode rightNode = (ArrayNode) jsonNode.at("/meta/context/right");
        leftNode.set(0, "char");
        rightNode.set(0, "char");
        leftNode.set(1, "200");
        rightNode.set(1, "200");

        Krill ks = new Krill(jsonNode);
        Result kr = ks.apply(ki);
        Match km = kr.getMatch(0);

        String html = km.getSnippetHTML();
        assertTrue(html.contains("<span class=\"context-left\">"));
        assertTrue(html.contains("<span class=\"match\">"));
        assertTrue(html.contains("<span class=\"context-right\">"));
    };

    @Test
    public void testCharContextBelowMaxIsNotClamped ()
            throws JsonMappingException, JsonProcessingException {
        assertEquals(500, KrillProperties.maxCharContextSize);

        JsonNode jsonNode = mapper.readTree(jsonQuery);
        ArrayNode leftNode = (ArrayNode) jsonNode.at("/meta/context/left");
        ArrayNode rightNode = (ArrayNode) jsonNode.at("/meta/context/right");
        leftNode.set(0, "char");
        rightNode.set(0, "char");
        leftNode.set(1, "30");
        rightNode.set(1, "30");

        Krill ks = new Krill(jsonNode);
        Result kr = ks.apply(ki);

        SearchContext context = kr.getContext();
        assertEquals(30, context.left.getLength());
        assertEquals(30, context.right.getLength());

        Match km = kr.getMatch(0);
        assertEquals(30, km.getContext().left.getLength());
        assertEquals(30, km.getContext().right.getLength());

        String rightContext = km.getSnippetBrackets().split("]]")[1];
        assertEquals(30, rightContext.length() - 4);
    };

    @Test
    public void testMaxCharContextViaPropertiesAffectsSearch ()
            throws JsonMappingException, JsonProcessingException {
        Properties props = new Properties();
        props.setProperty("krill.context.max.char", "80");
        KrillProperties.updateConfigurations(props);
        assertEquals(80, KrillProperties.maxCharContextSize);

        JsonNode jsonNode = mapper.readTree(jsonQuery);
        ArrayNode leftNode = (ArrayNode) jsonNode.at("/meta/context/left");
        ArrayNode rightNode = (ArrayNode) jsonNode.at("/meta/context/right");
        leftNode.set(0, "char");
        rightNode.set(0, "char");
        leftNode.set(1, "200");
        rightNode.set(1, "200");

        Krill ks = new Krill(jsonNode);
        Result kr = ks.apply(ki);

        SearchContext context = kr.getContext();
        assertEquals(80, context.left.getLength());
        assertEquals(80, context.right.getLength());

        Match km = kr.getMatch(0);
        assertEquals(80, km.getContext().left.getLength());
        assertEquals(80, km.getContext().right.getLength());

        String rightContext = km.getSnippetBrackets().split("]]")[1];
        assertEquals(80, rightContext.length() - 4);
    };

    @Test
    public void testMaxCharContextDoesNotAffectTokenContext ()
            throws JsonMappingException, JsonProcessingException {
        KrillProperties.maxCharContextSize = 10;

        Krill ks = new Krill(jsonQuery);
        Result kr = ks.apply(ki);

        SearchContext context = kr.getContext();
        assertEquals(5, context.left.getLength());
        assertEquals(5, context.right.getLength());
        assertTrue(context.left.isToken());
        assertTrue(context.right.isToken());

        Match km = kr.getMatch(0);
        assertEquals(5, km.getContext().left.getLength());
        assertEquals(5, km.getContext().right.getLength());
    };
}
