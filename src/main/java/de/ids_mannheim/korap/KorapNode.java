package de.ids_mannheim.korap;

import java.util.*;
import java.io.*;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.beans.PropertyVetoException;

import de.ids_mannheim.korap.KorapIndex;
import org.apache.lucene.store.MMapDirectory;

import com.mchange.v2.c3p0.*;

/**
 * Standalone REST-Service for the Lucene Search Backend.
 *
 * @author Nils Diewald
 */
public class KorapNode {

    // Base URI the Grizzly HTTP server will listen on
    public static String BASE_URI = "http://localhost:8080/";

    // Logger
    private final static Logger log = LoggerFactory.getLogger(KorapNode.class);

    // Index
    private static KorapIndex index;
    private static ComboPooledDataSource cpds;
    private static String path;
    private static String name = "unknown";

    private static String dbUser, dbPwd;

    private static String dbClass = "org.sqlite.JDBC";
    private static String dbURL   = "jdbc:sqlite:";

    /*
     * Todo: Add shutdown hook,
     * Then also close cdps.close();
     * see: https://10.0.10.12/trac/korap/browser/KorAP-modules/KorAP-REST/src/main/java/de/ids_mannheim/korap/web/Application.java
     * https://10.0.10.12/trac/korap/browser/KorAP-modules/KorAP-REST/src/main/java/de/ids_mannheim/korap/web/ShutdownHook.java
     */

    /**
     * Starts Grizzly HTTP server exposing JAX-RS resources defined in this application.
     * @return Grizzly HTTP server.
     */
    public static HttpServer startServer() {

	// Load configuration
	try {
	    InputStream file = new FileInputStream(
	      KorapNode.class.getClassLoader().getResource("server.properties").getFile()
            );
	    Properties prop = new Properties();
	    prop.load(file);

	    // Node properties
	    path     = prop.getProperty("lucene.indexDir", path);
	    name     = prop.getProperty("lucene.node.name", name);
	    BASE_URI = prop.getProperty("lucene.node.baseURI", BASE_URI);

	    // Database properties
	    dbUser  = prop.getProperty("lucene.db.user",    dbUser);
	    dbPwd   = prop.getProperty("lucene.db.pwd",     dbPwd);
	    dbClass = prop.getProperty("lucene.db.class",   dbClass);
	    dbURL   = prop.getProperty("lucene.db.jdbcURL", dbURL);

	}
	catch (IOException e) {
	    log.error(e.getLocalizedMessage());
	};

        // create a resource config that scans for JAX-RS resources and providers
        // in de.ids_mannheim.korap.server package
        final ResourceConfig rc =
	    new ResourceConfig().packages("de.ids_mannheim.korap.server");

        // create and start a new instance of grizzly http server
        // exposing the Jersey application at BASE_URI
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc);
    };


    public static HttpServer startServer(String nodeName, String indexPath) {

        // create a resource config that scans for JAX-RS resources and providers
        // in de.ids_mannheim.korap.server package
        final ResourceConfig rc =
	    new ResourceConfig().packages("de.ids_mannheim.korap.server");

	name = nodeName;
	path = indexPath;

        // create and start a new instance of grizzly http server
        // exposing the Jersey application at BASE_URI
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc);
    };


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

    public static String getName () {
	return name;
    };

    public static String getListener () {
	return BASE_URI;
    };

    public static ComboPooledDataSource getDBPool () {
	if (cpds != null)
	    return cpds;

	try {
	    cpds = new ComboPooledDataSource();
	    cpds.setDriverClass(dbClass);
	    cpds.setJdbcUrl(dbURL);
	    if (dbUser != null)
		cpds.setUser(dbUser);
	    if (dbPwd != null)
		cpds.setPassword(dbPwd);
	    cpds.setMaxStatements(100);
	    return cpds;
	}
	catch (PropertyVetoException e) {
	    log.error(e.getLocalizedMessage());
	};
	return null;
    };


    // Get Index
    public static KorapIndex getIndex () {
	if (index != null)
	    return index;

    	try {
	    if (path == null) {
		// Temporary index
		index = new KorapIndex();
	    }

	    else {
		File file = new File(path);

		log.info("Loading index from {}", path);
		if (!file.exists()) {
		    log.error("Index not found at {}", path);
		    return null;
		};

		System.out.println("Loading index from " + path);

		// Real index
		index = new KorapIndex(new MMapDirectory(file));
	    };
	    return index;
	}
	catch (IOException e) {
	    log.error("Index not loadable at {}: {}", path, e.getMessage());
	};
	return null;
    };
};
