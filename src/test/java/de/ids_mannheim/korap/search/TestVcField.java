package de.ids_mannheim.korap.search;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.index.FieldDocument;
import de.ids_mannheim.korap.response.Response;

public class TestVcField {
    private KrillIndex ki;
    private ObjectMapper mapper = new ObjectMapper();

    private FieldDocument createFieldDoc (int uid, String textSigle) {
        FieldDocument fd = new FieldDocument();
        fd.addString("textSigle", textSigle);
        fd.setUID(uid);
        return fd;
    }


    public TestVcField () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc(1, "WPD/AAA/00001"));

        FieldDocument fd = createFieldDoc(2, "WPD/AAA/00002");
        fd.addString("author", "Frank");
        fd.addString("quote", "The \"quoted\" example");
        fd.addString("empty", "");
        ki.addDoc(fd);

        fd = createFieldDoc(3, "WPD/AAA/00003");
        fd.addTV("tokens", "a b c", "[(0-1)s:a|i:a|_0$<i>0<i>1|-:t$<i>3]"
                + "[(2-3)s:b|i:b|_1$<i>2<i>3]" + "[(4-5)s:c|i:c|_2$<i>4<i>5]");
        ki.addDoc(fd);

        ki.addDoc(createFieldDoc(4, "WPD/AAA/00004"));
        ki.commit();
        ki.addDoc(createFieldDoc(5, "WPD/AAA/00005"));
        ki.addDoc(createFieldDoc(6, "WPD/AAA/00006"));
        ki.commit();
    }


    private String getJsonResource (String file) throws IOException {
        InputStream is = TestVcField.class.getResourceAsStream(file);
        return IOUtils.toString(is, "utf-8");
    }


    @Test
    public void testRetrieveTextSiglesOfVc1 () throws IOException {

        String file = "/queries/collections/named-vcs/named-vc1.jsonld";
        String json = getJsonResource(file);
        JsonNode n = new Krill().retrieveFieldValues(json, ki, "textSigle");

        assertEquals(Response.KORAL_VERSION, n.at("/@context").asText());
        assertEquals("koral:doc", n.at("/corpus/@type").asText());
        assertEquals("textSigle", n.at("/corpus/key").asText());
        assertEquals("type:string", n.at("/corpus/type").asText());

        assertEquals("[\"WPD/AAA/00002\",\"WPD/AAA/00003\"]",
                n.at("/corpus/value").toString());

        testRetrieveAuthorOfVc1(json);
        testRetrieveTokensOfVc1(json);
        testRetrieveNullOfVc1(json);
        testRetrieveQuoteOfVc1(json);
        testRetrieveEmptyOfVc1(json);
    }


    private void testRetrieveAuthorOfVc1 (String json) {
        JsonNode n = new Krill().retrieveFieldValues(json, ki, "author");
        assertEquals("author", n.at("/corpus/key").asText());
        assertEquals("[\"Frank\"]", n.at("/corpus/value").toString());
    }


    private void testRetrieveTokensOfVc1 (String json) {
        JsonNode n = new Krill().retrieveFieldValues(json, ki, "tokens");
        assertEquals("tokens", n.at("/corpus/key").asText());
        assertEquals("[]", n.at("/corpus/value").toString());
    }

    private void testRetrieveNullOfVc1 (String json) {
        JsonNode n = new Krill().retrieveFieldValues(json, ki, "hello");
        assertEquals("hello", n.at("/corpus/key").asText());
        assertEquals("[]", n.at("/corpus/value").toString());
    }

    private void testRetrieveQuoteOfVc1 (String json) {
        JsonNode n = new Krill().retrieveFieldValues(json, ki, "quote");
        assertEquals("quote", n.at("/corpus/key").asText());
        assertEquals("[\"The \\\"quoted\\\" example\"]", n.at("/corpus/value").toString());
    }

    private void testRetrieveEmptyOfVc1 (String json) {
        JsonNode n = new Krill().retrieveFieldValues(json, ki, "empty");
        assertEquals("empty", n.at("/corpus/key").asText());
        assertEquals("[]", n.at("/corpus/value").toString());
    }


    @Test
    public void testRetrieveTextSiglesOfVc3 () throws IOException {
        // uid 5000 is not in the index
        String file = "/queries/collections/named-vcs/named-vc3.jsonld";
        JsonNode n = new Krill().retrieveFieldValues(getJsonResource(file), ki,
                "textSigle");
        
        n = mapper.readTree(n.at("/corpus/value").toString());
        assertEquals(2, n.size());
        assertEquals("WPD/AAA/00002", n.at("/0").asText());
        assertEquals("WPD/AAA/00003", n.at("/1").asText());
    }


    @Test
    public void testRetrieveTextSiglesOfVc4 () throws IOException {

        String file = "/queries/collections/named-vcs/named-vc4.jsonld";
        JsonNode n = new Krill().retrieveFieldValues(getJsonResource(file), ki,
                "textSigle");
        n = mapper.readTree(n.at("/corpus/value").toString());
        assertEquals(3, n.size());
        assertEquals("WPD/AAA/00001", n.at("/0").asText());
        assertEquals("WPD/AAA/00002", n.at("/1").asText());
        assertEquals("WPD/AAA/00003", n.at("/2").asText());    }
}
