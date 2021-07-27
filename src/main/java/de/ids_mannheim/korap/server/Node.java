package de.ids_mannheim.korap.server;

import java.util.*;
import java.io.*;
import java.net.URL;
import java.nio.file.Paths;
import java.nio.file.Path;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.logging.LogManager;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.net.URI;
import jakarta.ws.rs.core.UriBuilder;
import java.beans.PropertyVetoException;

import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.util.KrillProperties;

import org.apache.lucene.store.MMapDirectory;

import static de.ids_mannheim.korap.util.KrillProperties.*;

import com.mchange.v2.c3p0.*;

/**
 * Standalone REST-Service for the Krill node.
 * Reads a property file at <tt>krill.properties</tt>.
 * Defaults to port <tt>9876</tt> if no information is given,
 * and an unprotected in-memory SQLite database for collections.
 * 
 * @author diewald
 */
public class Node {

    // Base URI the Grizzly HTTP server will listen on
    private static UriBuilder BASE_URI;
    private static String propFile = "krill.properties";

    private static int port = -1;
    private static String path = null;
    private static String name = "unknown";

    // Logger
    private final static Logger log = LoggerFactory.getLogger(Node.class);

    // Index
    private static KrillIndex index;

    // Database
    private static ComboPooledDataSource cpds;
    private static String dbUser, dbPwd;
    private static String dbClass = "org.sqlite.JDBC";
    private static String dbURL = "jdbc:sqlite:";


    /*
     * Todo: Close cdps.close() on shutdown.
     * see: https://10.0.10.12/trac/korap/browser/KorAP-modules/KorAP-REST/src/main/java/de/ids_mannheim/korap/web/Application.java
     * https://10.0.10.12/trac/korap/browser/KorAP-modules/KorAP-REST/src/main/java/de/ids_mannheim/korap/web/ShutdownHook.java
     */

    /**
     * Starts Grizzly HTTP server exposing JAX-RS
     * resources defined in this application.
     * This will load a <tt>krill.properties</tt> property file.
     * 
     * @return Grizzly HTTP server.
     */
    public static HttpServer startServer (String[] argv) {
        LogManager.getLogManager().reset();
        SLF4JBridgeHandler.install();


        for (int i = 0; i < argv.length; i += 2) {
            switch (argv[i]) {
                case "--config":
                case "-cfg":
                    propFile = argv[i + 1];
                    break;
                case "--port":
                case "-p":
                    port = Integer.valueOf(argv[i + 1]);
                    break;
                case "--name":
                case "-n":
                    name = argv[i + 1];
                    break;
                case "--dir":
                case "-d":
                    path = argv[i + 1];
                    break;
            };
        };

        Properties prop = KrillProperties.loadProperties(propFile);

        // Node properties
        if (path != null && path.equals(":memory:")) {
            path = null;
        }
        else {
            path = prop.getProperty("krill.indexDir", path);
        };

        if (name.equals("unknown"))
            name = prop.getProperty("krill.server.name", name);

        BASE_URI = UriBuilder.fromUri(prop.getProperty("krill.server.baseURI",
                "http://localhost:9876/"));

        if (port != -1)
            BASE_URI.port(port);

        // Database properties
        dbUser = prop.getProperty("krill.db.user", dbUser);
        dbPwd = prop.getProperty("krill.db.pwd", dbPwd);
        dbClass = prop.getProperty("krill.db.class", dbClass);
        dbURL = prop.getProperty("krill.db.jdbcURL", dbURL);

        // create a resource config that scans for JAX-RS resources and providers
        // in de.ids_mannheim.korap.server package
        final ResourceConfig rc = new ResourceConfig()
                .packages("de.ids_mannheim.korap.server");

        // create and start a new instance of grizzly http server
        // exposing the Jersey application at BASE_URI
        return GrizzlyHttpServerFactory.createHttpServer(BASE_URI.build(), rc);
    };


    /**
     * Runner method for Krill node.
     * Accepts various parameters:
     * 
     * <dl>
     * <dt>--config</dt>
     * <dd>Pass a configuration file overriding krill.properties</dd>
     * 
     * <dt>--port</dt>
     * <dd>Set the port for listener URI</dd>
     * 
     * <dt>--name</dt>
     * <dd>Set the name for the Krill node</dd>
     * 
     * <dt>--dir</dt>
     * <dd>Set the index directory for the Krill node</dd>
     * 
     * </dl>
     * 
     * @param argv
     *            No special arguments required. Supported arguments
     *            are listed above.
     * @throws IOException
     */
    public static void main (String[] argv) throws IOException {


        // WADL available at BASE_URI + application.wadl
        // Start the server with krill properties or given defaults
        final HttpServer server = startServer(argv);

        // Establish shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

            @Override
            public void run () {
                System.out.println("Stop Server");
                server.stop();
                if (cpds != null)
                    cpds.close();
            };

        }, "shutdownHook"));

        // Start server
        try {
            server.start();
            System.out.println("\nHello. My name is " + getName()
                    + " and I am a Krill node");
            System.out.println("listening on " + getListener() + ".");
            Thread.currentThread().join();
        }
        catch (Exception e) {
            log.error("Unable to start server: {}", e.getLocalizedMessage());
        };
    };


    /**
     * Get the name of the node.
     * The name is unique in the cluster and should be persistent.
     * 
     * @return The unique name of the node.
     */
    public static String getName () {
        return name;
    };


    /**
     * Get the URI (incl. port) the node is listening on.
     * 
     * @return The URI the node is listening on.
     */
    public static String getListener () {
        return getBaseURI().toString();
    };


    public static URI getBaseURI () {
        return BASE_URI.build();
    };


    /**
     * Shut down the database pool.
     */
    public static void closeDBPool () {
        if (cpds != null)
            cpds.close();
    };


    /**
     * Get the associated database pool
     * for match collection.
     * 
     * @return The CPDS {@link ComboPooledDataSource} object.
     */
    public static ComboPooledDataSource getDBPool () {

        // Pool already initiated
        if (cpds != null)
            return cpds;

        // Initiate pool
        try {

            // Parameters are defined in the property file
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


    /**
     * Get the associuated {@link KrillIndex}.
     * 
     * @return The associated {@link KrillIndex}.
     */
    public static KrillIndex getIndex () {

        // Index already instantiated
        if (index != null)
            return index;

        try {

            // Get a temporary index
            if (path == null) {

                // Temporary index
                index = new KrillIndex();
            }

            // Get a MMap directory index
            else {
                Path file = Paths.get(path);

                log.info("Loading index from {}", path);
                if (!file.toFile().exists()) {
                    log.error("Index not found at {}", path);
                    return null;
                };

                // Set real index
                index = new KrillIndex(new MMapDirectory(file));
            };
            return index;
        }
        catch (IOException e) {
            log.error("Index not loadable at {}: {}", path, e.getMessage());
        };
        return null;
    };
};
