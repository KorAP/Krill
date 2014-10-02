package de.ids_mannheim.korap.server;

/*
  http://harryjoy.com/2012/09/08/simple-rest-client-in-java/
*/
import java.io.*;
import java.util.ArrayList;
import java.util.List;


import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.client.Entity;

import org.glassfish.grizzly.http.server.HttpServer;

import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;

import de.ids_mannheim.korap.KorapNode;
import de.ids_mannheim.korap.KorapResult;
import de.ids_mannheim.korap.server.KorapResponse;
import static de.ids_mannheim.korap.util.KorapString.*;

public class TestResource {

    private HttpServer server;
    private WebTarget target;

    @Before
    public void setUp() throws Exception {
        // start the server
        server = KorapNode.startServer("milena", (String) null);
        // create the client
        Client c = ClientBuilder.newClient();

        // uncomment the following line if you want to enable
        // support for JSON in the client (you also have to uncomment
        // dependency on jersey-media-json module in pom.xml and Main.startServer())
        // --
        // c.configuration().enable(new org.glassfish.jersey.media.json.JsonJaxbFeature());

        target = c.target(KorapNode.BASE_URI);
    };

    @After
    public void tearDown() throws Exception {
        server.stop();
    };

    /**
     * Test to see that the message "Gimme 5 minutes, please!" is sent in the response.
     */
    @Test
    public void testPing() {
        String responseMsg = target.path("ping").request().get(String.class);
        assertEquals("Gimme 5 minutes, please!", responseMsg);
    };

    @Test
    public void testResource() throws IOException {
	for (String i : new String[] {"00001",
				      "00002",
				      "00003",
				      "00004",
				      "00005",
				      "00006",
				      "02439"
	    }) {

	    String json = StringfromFile(getClass().getResource("/wiki/" + i + ".json").getFile());
	    KorapResponse kresp = target.path("/index/" + i).
		request("application/json").
		put(Entity.json(json), KorapResponse.class);

	    assertEquals(kresp.getNode(), "milena");
	    assertEquals(kresp.getErr(), 0);
	    assertEquals(kresp.getUnstaged(), true);
	};

	KorapResponse kresp = target.path("/index").
	    request("application/json").
	    post(Entity.text(""), KorapResponse.class);
	assertEquals(kresp.getNode(), "milena");
	assertEquals(kresp.getMsg(), "Unstaged data was committed");	
    };

    @Test
    public void testCollection() throws IOException {

	String json = getString(
	    getClass().getResource("/queries/bsp-uid-example.jsonld").getFile()
        );

	 KorapResponse kresp
	    = target.path("/").
	    queryParam("uid", "1").
	    queryParam("uid", "4").
	    request("application/json").
	    post(Entity.json(json), KorapResponse.class);

	 assertEquals(2, kresp.getTotalResults());
	 assertEquals(0, kresp.getErr());
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
	} catch (IOException e) {
	    fail(e.getMessage());
	}
	return contentBuilder.toString();
    };

};
