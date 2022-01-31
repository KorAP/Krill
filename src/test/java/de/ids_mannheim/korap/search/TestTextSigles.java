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

public class TestTextSigles {
    private KrillIndex ki;

    private FieldDocument createFieldDoc (int uid, String textSigle) {
        FieldDocument fd = new FieldDocument();
        fd.addString("textSigle", textSigle);
        fd.setUID(uid);
        return fd;

    }


    public TestTextSigles () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc(1, "WPD/AAA/00001"));
        ki.addDoc(createFieldDoc(2, "WPD/AAA/00002"));
        ki.addDoc(createFieldDoc(3, "WPD/AAA/00003"));
        ki.addDoc(createFieldDoc(4, "WPD/AAA/00004"));
        ki.commit();
        ki.addDoc(createFieldDoc(5, "WPD/AAA/00005"));
        ki.addDoc(createFieldDoc(6, "WPD/AAA/00006"));
        ki.commit();
    }


    private String getJsonResource (String file) throws IOException {
        InputStream is = TestTextSigles.class.getResourceAsStream(file);
        return IOUtils.toString(is, "utf-8");
    }


    @Test
    public void testCreateTextSiglesForVC1 () throws IOException {

        String file = "/queries/collections/named-vcs/named-vc1.jsonld";
        JsonNode n = new Krill().createTextSigles(getJsonResource(file), ki);
        
        assertEquals(Response.KORAL_VERSION, n.at("/@context").asText());
        assertEquals("koral:doc", n.at("/collection/@type").asText());
        assertEquals("textSigle", n.at("/collection/key").asText());
        assertEquals("type:string", n.at("/collection/type").asText());
        assertEquals(2, n.at("/collection/value").size());
        
        assertEquals("WPD/AAA/00002", n.at("/collection/value/0").asText());
        assertEquals("WPD/AAA/00003", n.at("/collection/value/1").asText());
    }
    
    @Test
    public void testCreateTextSiglesForVC3 () throws IOException {
        // uid 5000 is not in the index
        String file = "/queries/collections/named-vcs/named-vc3.jsonld";
        JsonNode n = new Krill().createTextSigles(getJsonResource(file), ki);
        assertEquals(2, n.at("/collection/value").size());
    }
    
    @Test
    public void testCreateTextSiglesForVC4 () throws IOException {

        String file = "/queries/collections/named-vcs/named-vc4.jsonld";
        JsonNode n = new Krill().createTextSigles(getJsonResource(file), ki);
        assertEquals(3, n.at("/collection/value").size());
    }
}
