package de.ids_mannheim.korap.server;

import java.io.*;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import javax.ws.rs.WebApplicationException;

import de.ids_mannheim.korap.server.Node;
import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.KrillCollection;
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.response.Match;
import de.ids_mannheim.korap.response.Response;
import de.ids_mannheim.korap.response.MatchCollector;
import de.ids_mannheim.korap.response.collector.MatchCollectorDB;
import de.ids_mannheim.korap.util.QueryException;
import de.ids_mannheim.korap.index.FieldDocument;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import com.mchange.v2.c3p0.ComboPooledDataSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Root resource (exposed at root path) of the Krill node.
 * The responses only represent JSON responses, although HTML
 * responses may be handy.
 * 
 * @author diewald
 */
/* Look at
 * http://www.mkyong.com/webservices/jax-rs/json-example-with-jersey-jackson/
 */
@Path("/")
public class Resource {

    private String version;

    // Initiate Logger
    private final static Logger log = LoggerFactory.getLogger(Node.class);

    // This advices the java compiler to ignore all loggings
    public final static boolean DEBUG = false;

    // Slightly based on String::BooleanSimple
    private final static Pattern p = Pattern
            .compile("\\s*(?i:false|no|inactive|disabled|"
                    + "off|n|neg(?:ative)?|not|null|undef)\\s*");

    private KrillIndex index;


    /**
     * Return information on the node, like name etc.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String info () {
        final Response kresp = _initResponse();

        if (kresp.hasErrors())
            return kresp.toJsonString();

        // TODO: Name the number of documents in the index
        kresp.addMessage(680, "Server is up and running!");
        return kresp.toJsonString();
    };


    /**
     * Add new documents to the index
     * 
     * @param json
     *            JSON-LD string with search and potential meta
     *            filters.
     */
    /*
     * Support GZip:
     * Or maybe it's already supported ...
     * http://stackoverflow.com/questions/19765582/how-to-make-jersey-use-gzip-compression-for-the-response-message-body
    */
    @PUT
    @Path("/index/{textID}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String add (@PathParam("textID") Integer uid, @Context UriInfo uri,
            String json) {
        /*
         * See
         * http://www.mkyong.com/webservices/jax-rs/file-upload-example-in-jersey/
         */
        // Todo: Parameter for server node
        if (DEBUG)
            log.trace("Added new document with unique identifier {}", uid);

        final Response kresp = _initResponse();
        if (kresp.hasErrors())
            return kresp.toJsonString();

        // Get index
        index = Node.getIndex();


        FieldDocument fd = index.addDoc(uid, json);
        if (fd == null) {
            // Set HTTP to ???
            // TODO: This may be a field error!
            kresp.addError(602, "Unable to add document to index");
            return kresp.toJsonString();
        };

        // Set HTTP to 200
        kresp.addMessage(681, "Document was added successfully",
                fd.getID() != null ? fd.getID() : "Unknown");

        // Mirror meta data
        kresp.addJsonNode("text", (ObjectNode) fd.toJsonNode());

        return kresp.toJsonString();
    };


    @GET
    @Path("/index/{textID}")
    @Produces(MediaType.APPLICATION_JSON)
    public String get (@PathParam("textID") String uid, @Context UriInfo uri) {

        if (DEBUG)
            log.trace("Get document with unique identifier {}", uid);

        final Response kresp = _initResponse();
        if (kresp.hasErrors())
            return kresp.toJsonString();

        // Get index
        index = Node.getIndex();

        return index.getDoc(uid).toJsonString();
    };


    // TODO: Commit changes to the index before the server dies!
    /**
     * Commit data changes to the index
     */
    @POST
    @Path("/index")
    @Produces(MediaType.APPLICATION_JSON)
    public String commit () {

        final Response kresp = _initResponse();
        if (kresp.hasErrors())
            return kresp.toJsonString();

        // There are documents to commit
        try {
            Node.getIndex().commit();
            kresp.addMessage(683, "Staged data committed");
        }
        catch (IOException e) {
            // Set HTTP to ???
            kresp.addError(603, "Unable to commit staged data to index");
            return kresp.toJsonString();
        };

        // Set HTTP to ???
        return kresp.toJsonString();
    };


    // Return corpus info
    @GET
    @Path("/corpus")
    @Produces(MediaType.APPLICATION_JSON)
    public String getCorpus (@Context UriInfo uri) {
        ObjectMapper mapper = new ObjectMapper();

        // TODO: Accept fields!!!!

        final Response kresp = _initResponse();
        if (kresp.hasErrors())
            return kresp.toJsonString();

        // TODO: Statistics should be node fields - not annotations!
        // TODO: This is just temporary
        KrillIndex ki = Node.getIndex();

        ObjectNode obj = mapper.createObjectNode();
        obj.put("tokens", ki.numberOf("tokens"));
        obj.put("base/texts", ki.numberOf("base/texts"));
        obj.put("base/sentences", ki.numberOf("base/sentences"));
        obj.put("base/paragraphs", ki.numberOf("base/paragraphs"));

        // <legacy>
        obj.put("sentences", ki.numberOf("sentences"));
        obj.put("paragraphs", ki.numberOf("paragraphs"));
        // </legacy>

        kresp.addJsonNode("stats", obj);
        return kresp.toJsonString();
    };


    // PUT: Return corpus info for virtual corpus


    /**
     * Find matches in the lucene index based on UIDs and return one
     * match per doc.
     * 
     * @param text_id
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String find (String json, @Context UriInfo uri) {

        final Response kresp = _initResponse();
        if (kresp.hasErrors())
            return kresp.toJsonString();

        // Search index
        final Krill ks = new Krill(json);

        // Get query parameters
        final MultivaluedMap<String, String> qp = uri.getQueryParameters();

        if (qp.get("uid") == null) {
            kresp.addError(610, "Missing request parameters",
                    "No unique IDs were given");
            return kresp.toJsonString();
        };

        // Build Collection based on a list of uids
        final List<String> uids = qp.get("uid");

        // TODO: RESTRICT COLLECTION TO ONLY RESPECT SELF DOCS (REPLICATION)

        // Ignore a Collection that may already be established
        final KrillCollection kc = new KrillCollection();
        kc.filterUIDs(uids.toArray(new String[uids.size()]));
        ks.setCollection(kc);

        // Only return the first match per text
        ks.getMeta().setItemsPerResource(1);

        return ks.apply(Node.getIndex()).toJsonString();
    };


    /**
     * Collect matches and aggregate the UIDs plus matchcount in the
     * database.
     * 
     * @param text_id
     */
    @PUT
    @Path("/collect/{resultID}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String collect (String json, @PathParam("resultID") String resultID,
            @Context UriInfo uri) {

        Response kresp = _initResponse();
        if (kresp.hasErrors())
            return kresp.toJsonString();

        // Get the database
        try {
            final MatchCollectorDB mc = new MatchCollectorDB(1000, "Res_"
                    + resultID);
            final ComboPooledDataSource pool = Node.getDBPool();
            mc.setDBPool("mysql", pool, pool.getConnection());

            // TODO: Only search in self documents (REPLICATION FTW!)

            final Krill ks = new Krill(json);

            // TODO: Reuse response!
            final MatchCollector result = Node.getIndex().collect(ks, mc);

            result.setNode(Node.getName());
            return result.toJsonString();
        }
        catch (SQLException e) {
            log.error(e.getLocalizedMessage());
        };

        kresp.addError(604, "Unable to connect to database");
        return kresp.toJsonString();
    };



    /* These routes are still wip: */



    /**
     * Search the lucene index.
     * 
     * @param json
     *            JSON-LD string with search and potential meta
     *            filters.
     */
    @POST
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String search (String json) {

        Response kresp = _initResponse();
        if (kresp.hasErrors())
            return kresp.toJsonString();

        // Search index
        // Reuse Response
        Result kr = new Krill(json).apply(Node.getIndex());
        return kr.toJsonString();
    };


    @GET
    @Path("/match/{matchID}")
    @Produces(MediaType.APPLICATION_JSON)
    public String match (@PathParam("matchID") String id, @Context UriInfo uri) {

        Response kresp = _initResponse();
        if (kresp.hasErrors())
            return kresp.toJsonString();

        // Get index
        KrillIndex index = Node.getIndex();


        // Get query parameters
        MultivaluedMap<String, String> qp = uri.getQueryParameters();

        boolean includeSpans = false, includeHighlights = true, extendToSentence = false, info = false;

        // Optional query parameter "info" for more information on the match
        if (!_isNull(qp.getFirst("info")))
            info = true;

        // Optional query parameter "spans" for span information inclusion
        if (!_isNull(qp.getFirst("spans"))) {
            includeSpans = true;
            info = true;
        };

        // Optional query parameter "highlights" for highlight information inclusion
        String highlights = qp.getFirst("highlights");
        if (highlights != null && _isNull(highlights))
            includeHighlights = false;

        // Optional query parameter "extended" for sentence expansion
        if (!_isNull(qp.getFirst("extended")))
            extendToSentence = true;

        List<String> foundries = qp.get("foundry");
        List<String> layers = qp.get("layer");

        try {
            // Get match info
            return index.getMatchInfo(id, "tokens", info, foundries, layers,
                    includeSpans, includeHighlights, extendToSentence)
                    .toJsonString();
        }

        // Nothing found
        catch (QueryException qe) {
            // Todo: Make Match rely on Response!
            kresp.addError(qe.getErrorCode(), qe.getMessage());
        };

        return kresp.toJsonString();
    };


    /*
      POST /collect/:result_id
      POST /peek
      POST /?text_id=...
      POST /:text_id/

      PUT /:text_id

      DELETE /:text_id
      DELETE /:corpus_sigle
      DELETE /:corpus_sigle/:doc_sigle
      DELETE /:corpus_sigle/:doc_sigle/:text_sigle
     */

    @POST
    @Path("/collection")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String collection (String json) {

        // Get index
        KrillIndex index = Node.getIndex();

        if (index == null)
            return "{\"documents\" : -1, error\" : \"No index given\" }";

        return "{}";
    };



    // Interceptor class
    public class GZIPReaderInterceptor implements ReaderInterceptor {
        @Override
        public Object aroundReadFrom (ReaderInterceptorContext context)
                throws IOException, WebApplicationException {
            final InputStream originalInputStream = context.getInputStream();
            context.setInputStream(new GZIPInputStream(originalInputStream));
            return context.proceed();
        };
    };

    public class GZIPWriterInterceptor implements WriterInterceptor {
        @Override
        public void aroundWriteTo (WriterInterceptorContext context)
                throws IOException, WebApplicationException {
            final OutputStream outputStream = context.getOutputStream();
            context.setOutputStream(new GZIPOutputStream(outputStream));
            context.proceed();
        };
    };


    private Response _initResponse () {
        Response kresp = new Response();
        kresp.setNode(Node.getName());
        kresp.setListener(Node.getListener());

        // Get index
        KrillIndex index = Node.getIndex();

        if (index == null) {
            kresp.addError(601, "Unable to find index");
            return kresp;
        };

        kresp.setVersion(index.getVersion());
        kresp.setName(index.getName());
        return kresp;
    };


    // Check if a string is meant to represent null
    private static boolean _isNull (String value) {
        if (value == null)
            return true;

        final Matcher m = p.matcher(value);
        if (m.matches())
            return true;

        return false;
    };
};
