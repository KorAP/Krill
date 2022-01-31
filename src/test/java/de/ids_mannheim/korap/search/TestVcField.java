package de.ids_mannheim.korap.search;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.index.FieldDocument;
import de.ids_mannheim.korap.response.Response;

public class TestVcField {
    private KrillIndex ki;

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

        System.out.println(n.toPrettyString());
        assertEquals(Response.KORAL_VERSION, n.at("/@context").asText());
        assertEquals("koral:doc", n.at("/corpus/@type").asText());
        assertEquals("textSigle", n.at("/corpus/key").asText());
        assertEquals("type:string", n.at("/corpus/type").asText());
        assertEquals(2, n.at("/corpus/value").size());

        assertEquals("WPD/AAA/00002", n.at("/corpus/value/0").asText());
        assertEquals("WPD/AAA/00003", n.at("/corpus/value/1").asText());
        
        testRetrieveAuthorOfVc1(json);
        testRetrieveTokensOfVc1(json);
    }


    private void testRetrieveAuthorOfVc1 (String json) {
        JsonNode n = new Krill().retrieveFieldValues(json, ki, "author");
        assertEquals("author", n.at("/corpus/key").asText());
        assertEquals("Frank", n.at("/corpus/value/0").asText());        
    }


    private void testRetrieveTokensOfVc1 (String json) {
        JsonNode n = new Krill().retrieveFieldValues(json, ki, "tokens");
        assertEquals("tokens", n.at("/corpus/key").asText());
        assertEquals(1, n.at("/corpus/value").size());
        assertEquals("a b c", n.at("/corpus/value/0").asText());
    }


    @Test
    public void testRetrieveTextSiglesOfVc3 () throws IOException {
        // uid 5000 is not in the index
        String file = "/queries/collections/named-vcs/named-vc3.jsonld";
        JsonNode n = new Krill().retrieveFieldValues(getJsonResource(file), ki,
                "textSigle");
        assertEquals(2, n.at("/corpus/value").size());
    }


    @Test
    public void testRetrieveTextSiglesOfVc4 () throws IOException {

        String file = "/queries/collections/named-vcs/named-vc4.jsonld";
        JsonNode n = new Krill().retrieveFieldValues(getJsonResource(file), ki,
                "textSigle");
        assertEquals(3, n.at("/corpus/value").size());
    }
}
