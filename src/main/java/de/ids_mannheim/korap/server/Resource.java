package de.ids_mannheim.korap.server;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.MultivaluedMap;

import de.ids_mannheim.korap.KorapNode;
import de.ids_mannheim.korap.KorapIndex;
import de.ids_mannheim.korap.KorapSearch;
import de.ids_mannheim.korap.KorapMatch;
import de.ids_mannheim.korap.KorapResult;
import de.ids_mannheim.korap.util.QueryException;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import com.mchange.v2.c3p0.*;

/**
 * Root resource (exposed at root path)
 *
 * @author Nils Diewald
 *
 * Look at http://www.mkyong.com/webservices/jax-rs/json-example-with-jersey-jackson/
 */
@Path("/")
public class Resource {

    static Pattern p = Pattern.compile("\\s*(?i:false|null)\\s*");

    private static boolean isNull (String value) {
	if (value == null)
	    return true;

	Matcher m = p.matcher(value);
	if (m.matches())
	    return true;

	return false;
    };

    /**
     * Search the lucene index.
     *
     * @param json JSON-LD string with search and potential meta filters.
     */
    @POST
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String search (String json) {

	// Get index
	KorapIndex index = KorapNode.getIndex();

	// Search index
        if (index != null)
            return new KorapSearch(json).run(index).toJSON();

	// Response with error message
        KorapResult kr = new KorapResult();
        kr.setError("Index not found");
        return kr.toJSON();
    };

    @GET
    @Path("/match/{matchID}")
    @Produces(MediaType.APPLICATION_JSON)
    public String match (@PathParam("matchID") String id,
			 @Context UriInfo uri) {

	// Get index
	KorapIndex index = KorapNode.getIndex();

	// Search index
        if (index != null) {

	    // Get query parameters
	    MultivaluedMap<String,String> qp = uri.getQueryParameters();

	    boolean includeSpans = false,
		includeHighlights = true,
		extendToSentence = false,
		info = false;

	    // Optional query parameter "info" for more information on the match
	    if (!isNull(qp.getFirst("info")))
		info = true;
	    
	    // Optional query parameter "spans" for span information inclusion
	    if (!isNull(qp.getFirst("spans"))) {
		includeSpans = true;
		info = true;
	    };

	    // Optional query parameter "highlights" for highlight information inclusion
	    String highlights = qp.getFirst("highlights");
	    if (highlights != null && isNull(highlights))
		includeHighlights = false;

	    // Optional query parameter "extended" for sentence expansion
	    if (!isNull(qp.getFirst("extended")))
		extendToSentence = true;

	    List<String> foundries = qp.get("foundry");
	    List<String> layers    = qp.get("layer");

            try {		
		// Get match info
                return index.getMatchInfo(
		    id,
		    "tokens",
		    info,
		    foundries,
		    layers,
		    includeSpans,
		    includeHighlights,
		    extendToSentence
		).toJSON();
            }

	    // Nothing found
	    catch (QueryException qe) {
                KorapMatch km = new KorapMatch();
                km.setError(qe.getMessage());
                return km.toJSON();
            }
	};

	// Response with error message
        KorapMatch km = new KorapMatch();
        km.setError("Index not found");
        return km.toJSON();
    };

    @POST
    @Path("/collection")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String collection (String json) {

	// Get index
	KorapIndex index = KorapNode.getIndex();

	if (index == null)
	    return "{\"documents\" : -1, error\" : \"No index given\" }";

	return "{}";
    };
};
