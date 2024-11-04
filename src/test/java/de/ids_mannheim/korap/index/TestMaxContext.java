package de.ids_mannheim.korap.index;

import static de.ids_mannheim.korap.TestSimple.getJsonString;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.response.Match;
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.response.SearchContext;
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
        kr = ks.apply(ki);

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
}
