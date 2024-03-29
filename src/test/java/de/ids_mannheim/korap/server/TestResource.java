package de.ids_mannheim.korap.server;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.client.Entity;

import static de.ids_mannheim.korap.TestSimple.*;

import org.glassfish.grizzly.http.server.HttpServer;

import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;

import de.ids_mannheim.korap.server.Node;
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.response.Response;
import static de.ids_mannheim.korap.util.KrillString.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author diewald
 */
// http://harryjoy.com/2012/09/08/simple-rest-client-in-java/
public class TestResource {
    private HttpServer server;
    private WebTarget target;

    final ObjectMapper mapper = new ObjectMapper();
    long t1 = 0, t2 = 0, t3 = 0, t4 = 0;


    @Before
    public void setUp () throws Exception {
        // start the server
        t1 = System.nanoTime();
        server = Node.startServer(new String[] { "--name", "milena", "--dir",
                ":memory:", "--port", "9157" });
        // create the client
        Client c = ClientBuilder.newClient();

        t2 = System.nanoTime();

        // uncomment the following line if you want to enable
        // support for JSON in the client (you also have to uncomment
        // dependency on jersey-media-json module in pom.xml and Main.startServer())
        // --

        // c.configuration().enable(com.sun.jersey.api.json.POJOMappingFeature());
        // c.configuration().enable(new org.glassfish.jersey.media.json.JsonJaxbFeature());
        // c.register(JacksonFeatures.class);
        // c.register(com.fasterxml.jackson.jaxrs.annotation.JacksonFeatures.class);

        /*
        ClientConfig clientConfig = new DefaultClientConfig();
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        Client c = Client.create(clientConfig);
        */
        target = c.target(Node.getBaseURI());
    };


    @After
    public void tearDown () throws Exception {
        t3 = System.nanoTime();
        server.stop();
        Node.closeDBPool();
        t4 = System.nanoTime();

        double startup = (double) (t2 - t1) / 1000000000.0;
        double action = (double) (t3 - t2) / 1000000000.0;
        double shutdown = (double) (t4 - t3) / 1000000000.0;
    };


    /**
     * Test to see that the message "Gimme 5 minutes, please!" is sent
     * in the response.
     */
    @Test
    public void testPing () {
        String responseMsg = target.path("ping").request().get(String.class);
        assertEquals("Gimme 5 minutes, please!", responseMsg);
    };


    // This tests the node info
    @Test
    public void testInfo () throws IOException {
        String responseMsg = target.path("/").request().get(String.class);
        JsonNode res = mapper.readTree(responseMsg);
        assertEquals("milena", res.at("/meta/node").asText());
        assertEquals(680, res.at("/messages/0/0").asInt());
    };


    @Test
    public void testIndexing () throws IOException {
        String resp;
        JsonNode res;

        for (String i : new String[] { "00001", "00002", "00003", "00004",
                "00005", "00006" }) {



            String json = StringfromFile(
                    getClass().getResource("/wiki/" + i + ".json").getFile());

            Entity jsonE = Entity.json(json);

            try {
                // Put new documents to the index
                resp = target.path("/index/" + i).request("application/json")
                        .put(jsonE, String.class);

                res = mapper.readTree(resp);
                assertEquals("milena", res.at("/meta/node").asText());
                assertEquals(681, res.at("/messages/0/0").asInt());
            }
            catch (Exception e) {
                fail("Server response failed " + e.getMessage()
                        + " (Known issue)");
            }
        };

        String json = StringfromFile(
                getClass().getResource("/wiki/02439.json").getFile());
        Entity jsonE = Entity.json(json);

        try {
            // Put new documents to the index
            resp = target.path("/index/02439").request("application/json")
                    .put(jsonE, String.class);

            res = mapper.readTree(resp);

            // Check mirroring
            assertEquals(2439, res.at("/text/UID").asInt());
            assertEquals("milena", res.at("/meta/node").asText());
            assertEquals(681, res.at("/messages/0/0").asInt());
        }
        catch (Exception e) {
            fail("Server response failed " + e.getMessage() + " (Known issue)");
        };

        // Commit!
        resp = target.path("/index").request("application/json")
                .post(Entity.text(""), String.class);
        res = mapper.readTree(resp);
        assertEquals("milena", res.at("/meta/node").asText());

        // Staged data committed
        assertEquals(683, res.at("/messages/0/0").asInt());


        // Get document by UID
        resp = target.path("/index/00005").request().get(String.class);
        res = mapper.readTree(resp);

        assertEquals("freizeit-unterhaltung reisen schrott tabellen",
                res.at("/textClass").asText());
        assertEquals("Å (Orte in Norwegen)", res.at("/title").asText());
        assertEquals("WPD", res.at("/corpusID").asText());
        assertEquals(5, res.at("/UID").asInt());
        assertEquals("WPD_AAA.00005", res.at("/ID").asText());


        // Get document by UID
        resp = target.path("/index/5").request().get(String.class);
        res = mapper.readTree(resp);

        assertEquals("freizeit-unterhaltung reisen schrott tabellen",
                res.at("/textClass").asText());
        assertEquals("Å (Orte in Norwegen)", res.at("/title").asText());
        assertEquals("WPD", res.at("/corpusID").asText());
        assertEquals(5, res.at("/UID").asInt());
        assertEquals("WPD_AAA.00005", res.at("/ID").asText());

        // Get document by UID
        resp = target.path("/index/17").request().get(String.class);
        res = mapper.readTree(resp);

        assertEquals(630, res.at("/errors/0/0").asInt());
        assertTrue(res.at("/UID").isMissingNode());

        // Get corpus statistics
        resp = target.path("/corpus").request().get(String.class);
        res = mapper.readTree(resp);

        assertEquals(281, res.at("/stats/sentences").asInt());
        assertEquals(174, res.at("/stats/paragraphs").asInt());
        assertEquals(2661, res.at("/stats/tokens").asInt());

        assertEquals(7, res.at("/stats/base~1texts").asInt());
    };


    /*
    @Test
    public void testRemoving () throws IOException {
        String resp;
        JsonNode res;
    
        String json = StringfromFile(getClass().getResource("/wiki/02439.json")
                .getFile());
        Entity jsonE = Entity.json(json);
    
        try {
            // Put new documents to the index
            resp = target.path("/index/02439").request("application/json")
                    .put(jsonE, String.class);
    
            res = mapper.readTree(resp);
    
            // Check mirroring
            assertEquals(2439, res.at("/text/UID").asInt());
            assertEquals("milena", res.at("/meta/node").asText());
            assertEquals(681, res.at("/messages/0/0").asInt());
        }
        catch (Exception e) {
            fail("Server response failed " + e.getMessage() + " (Known issue)");
        };
    
        // Commit!
        resp = target.path("/index").request("application/json")
                .post(Entity.text(""), String.class);
        res = mapper.readTree(resp);
        assertEquals("milena", res.at("/meta/node").asText());
    
        // Staged data committed
        assertEquals(683, res.at("/messages/0/0").asInt());
    };
    */


    @Test
    public void testCollection () throws IOException {

        // mate/l:sein
        String json = getJsonString(getClass()
                .getResource("/queries/bsp-uid-example.jsonld").getFile());

        try {
            String resp = target.path("/").queryParam("uid", "1")
                    .queryParam("uid", "4").request("application/json")
                    .post(Entity.json(json), String.class);
            JsonNode res = mapper.readTree(resp);
            assertEquals(2, res.at("/meta/totalResults").asInt());
        }
        catch (Exception e) {
            fail("Server response failed: " + e.getMessage()
                    + " (Known issue)");
        };

    };
};
