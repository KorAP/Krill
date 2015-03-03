package de.ids_mannheim.korap.server;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.client.Entity;

import org.glassfish.grizzly.http.server.HttpServer;
import com.fasterxml.jackson.jaxrs.annotation.JacksonFeatures;

import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;

import java.io.FileInputStream;

import de.ids_mannheim.korap.server.Node;
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.response.Response;
import static de.ids_mannheim.korap.util.KrillString.*;


/**
 * @author diewald
 */
// http://harryjoy.com/2012/09/08/simple-rest-client-in-java/
public class TestResource {
    private HttpServer server;
    private WebTarget target;

    @Before
    public void setUp () throws Exception {
        // start the server
        server = Node.startServer("milena", (String) null);
        // create the client
        Client c = ClientBuilder.newClient();

        // uncomment the following line if you want to enable
        // support for JSON in the client (you also have to uncomment
        // dependency on jersey-media-json module in pom.xml and Main.startServer())
        // --

        // c.configuration().enable(com.sun.jersey.api.json.POJOMappingFeature());
        // c.configuration().enable(new org.glassfish.jersey.media.json.JsonJaxbFeature());
        // c.register(JacksonFeature.class);
        // c.register(com.fasterxml.jackson.jaxrs.annotation.JacksonFeatures.class);

        /*
        ClientConfig clientConfig = new DefaultClientConfig();
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        Client c = Client.create(clientConfig);
*/
        target = c.target(Node.BASE_URI);
    };

    @After
    public void tearDown() throws Exception {
        server.stop();
    };

    /**
     * Test to see that the message "Gimme 5 minutes, please!" is sent in the response.
     */
    @Test
    public void testPing () {
        String responseMsg = target.path("ping").request().get(String.class);
        assertEquals("Gimme 5 minutes, please!", responseMsg);
    };

    @Ignore
    public void testResource() throws IOException {
        Response kresp;

        for (String i : new String[] {"00001",
                                      "00002",
                                      "00003",
                                      "00004",
                                      "00005",
                                      "00006",
                                      "02439"
            }) {

            String json = StringfromFile(
                getClass().getResource("/wiki/" + i + ".json").getFile()
            );

            Entity jsonE = Entity.json(json);

            try {
                kresp = target.path("/index/" + i).
                    request("application/json").
                    put(jsonE, Response.class);

                assertEquals(kresp.getNode(), "milena");
                assertFalse(kresp.hasErrors());
                assertFalse(kresp.hasWarnings());
                assertFalse(kresp.hasMessages());
            }
            catch (Exception e) {
                fail("Server response failed " + e.getMessage() + " (Known issue)");
            }
        };

        kresp = target.path("/index").
            request("application/json").
            post(Entity.text(""), Response.class);
        assertEquals(kresp.getNode(), "milena");
        assertFalse(kresp.hasErrors());
        assertFalse(kresp.hasWarnings());
        assertFalse(kresp.hasMessages());
    };


    @Ignore
    public void testCollection() throws IOException {

        String json = getString(
            getClass().getResource("/queries/bsp-uid-example.jsonld").getFile()
        );

        try {
            Response kresp
                = target.path("/").
                queryParam("uid", "1").
                queryParam("uid", "4").
                request("application/json").
                post(Entity.json(json), Response.class);

            assertEquals(2, kresp.getTotalResults());
            assertFalse(kresp.hasErrors());
            assertFalse(kresp.hasWarnings());
            assertFalse(kresp.hasMessages());
        }
        catch (Exception e) {
            fail("Server response failed: " + e.getMessage() + " (Known issue)");
        };

    };

    public static String getString (String path) {
        StringBuilder contentBuilder = new StringBuilder();
        try {
            BufferedReader in = new BufferedReader(new FileReader(path));
            String str;
            while ((str = in.readLine()) != null) {
                contentBuilder.append(str);
            };
            in.close();
        }
        catch (IOException e) {
            fail(e.getMessage());
        };
        return contentBuilder.toString();
    };
};
