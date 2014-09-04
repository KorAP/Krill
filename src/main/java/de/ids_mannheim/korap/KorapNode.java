package de.ids_mannheim.korap;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import de.ids_mannheim.korap.KorapIndex;
import org.apache.lucene.store.MMapDirectory;


/**
 * Standalone REST-Service for the Lucene Search Backend.
 *
 * @author Nils Diewald
 */
public class KorapNode {

    // Base URI the Grizzly HTTP server will listen on
    public static final String BASE_URI = "http://localhost:8080/";

    // Logger
    private final static Logger log = LoggerFactory.getLogger(KorapNode.class);

    // Index
    private static KorapIndex index;

    /*
      Todo: Use korap.config for paths to
            indexDirectory
     */
    private static String path = new String("/home/ndiewald/Repositories/korap/KorAP-modules/KorAP-lucene-index/sandbox/index");



    /**
     * Starts Grizzly HTTP server exposing JAX-RS resources defined in this application.
     * @return Grizzly HTTP server.
     */
    public static HttpServer startServer() {

        // create a resource config that scans for JAX-RS resources and providers
        // in de.ids_mannheim.korap.server package
        final ResourceConfig rc =
	    new ResourceConfig().packages("de.ids_mannheim.korap.server");

        // create and start a new instance of grizzly http server
        // exposing the Jersey application at BASE_URI
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc);
    }

    /**
     * Main method.
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        final HttpServer server = startServer();
        System.out.println("KorapNode started\nHit enter to stop it...");
	// WADL available at BASE_URI + application.wadl
        System.in.read();
        server.stop();
    };

    public static KorapIndex getIndex () {
	if (index != null)
	    return index;

    	try {
	    File file = new File(path);

	    log.info("Loading index from {}", path);
	    if (!file.exists()) {
		log.error("Index not found at {}", path);
		return null;
	    };

	    System.out.println("Loading index from " + path);

	    index = new KorapIndex(new MMapDirectory(file));
	    return index;
	    /*
	    // Temporarily!
	    static String path = new String();

	    this.index = new KorapIndex(new MMapDirectory(f));
	    return this.index;
	    */
	}
	catch (IOException e) {
	    log.error("Index not loadable at {}: {}", path, e.getMessage());
	};
	return null;
    };
};
