package de.ids_mannheim.korap;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.search.spans.SpanQuery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.ids_mannheim.korap.cache.VirtualCorpusCache;
import de.ids_mannheim.korap.query.wrap.SpanQueryWrapper;
import de.ids_mannheim.korap.response.Response;
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.response.VirtualCorpusResponse;
import de.ids_mannheim.korap.util.QueryException;

/**
 * <p>Krill is a corpus data retrieval index using Lucene for
 * Look-Ups.</p>
 * 
 * <p>
 * It is the reference implementation for KoralQuery consumption,
 * and this class acts as the central point for consuming and
 * responding to KoralQuery requests.
 * </p>
 * 
 * <p>
 * The processing of the collection section of the request is
 * delegated
 * to {@link KrillCollection}, the query section to {@link KrillQuery}
 * ,
 * and the meta section to {@link KrillMeta}.
 * </p>
 * 
 * <blockquote><pre>
 * // Create or receive a KoralQuery JSON string
 * String koral = "{\"query\":{...}, \"collection\":{...}, ... }";
 * 
 * // Create a new krill search object by passing the Query
 * Krill krill = new Krill(koral);
 * 
 * // Apply the query to an index and receive a search result
 * // This may invoke different actions depending on the request
 * Result result = krill.setIndex(new KrillIndex()).apply();
 * </pre></blockquote>
 * 
 * @author diewald
 * @author margaretha
 * 
 * @see KrillCollection
 * @see KrillQuery
 * @see KrillMeta
 * @see KrillIndex
 */
// TODO: Use a krill.properties configuration file
// TODO: Reuse passed JSON object instead of creating a new response!
public class Krill extends Response {
    private KrillIndex index;
    private SpanQuery spanQuery;
    private JsonNode request;

    private int maxTokenMatchSize;
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Construct a new Krill object.
     */
    public Krill () {};


    /**
     * Construct a new Krill object,
     * consuming a KoralQuery json string.
     * 
     * @param query
     *            The KoralQuery json string.
     */
    public Krill (String query) {
        this.fromKoral(query);
    };


    /**
     * Construct a new Krill object,
     * consuming a KoralQuery {@link JsonNode} object.
     * 
     * @param query
     *            The KoralQuery {@link JsonNode} object.
     */
    public Krill (JsonNode query) {
        this.fromKoral(query);
    };


    /**
     * Construct a new Krill object,
     * consuming a {@link SpanQueryWrapper} object.
     * 
     * @param query
     *            The {@link SpanQueryWrapper} object.
     */
    public Krill (SpanQueryWrapper query) {
        try {
            this.spanQuery = query.toQuery();
        }

        // Add the error to the KoralQuery response
        catch (QueryException q) {
            this.addError(q.getErrorCode(), q.getMessage());
        };
    };


    /**
     * Construct a new Krill object,
     * consuming a {@link SpanQuery} object.
     * 
     * @param query
     *            The {@link SpanQuery} object.
     */
    public Krill (SpanQuery query) {
        this.spanQuery = query;
    };


    /**
     * Parse KoralQuery as a json string.
     * 
     * @param query
     *            The KoralQuery json string.
     * @return The {@link Krill} object for chaining.
     * @throws QueryException
     */
    public Krill fromKoral (final String query) {
        // Parse query string
        try {
            this.request = mapper.readTree(query);
            this.fromKoral(this.request);
        }

        // Unable to parse JSON
        catch (IOException e) {
            this.addError(621, "Unable to parse JSON");
        };

        return this;
    };


    /**
     * Parse KoralQuery as a {@link JsonNode} object.
     * 
     * @param query
     *            The KoralQuery {@link JsonNode} object.
     * @return The {@link Krill} object for chaining.
     * @throws QueryException
     */
    public Krill fromKoral (JsonNode json) {

        // Parse "query" attribute
        if (json.has("query")) {
            try {
                final KrillQuery kq = new KrillQuery("tokens");
                this.setQuery(kq);

                final SpanQueryWrapper qw = kq.fromKoral(json.get("query"));

                // Koral messages are moved to the Krill object
                this.moveNotificationsFrom(kq);

                // Throw an error, in case the query matches everywhere
                if (qw.isEmpty()) {
                    this.addError(780, "This query matches everywhere");
                }
                else if (qw.isNull()) {
                    this.addError(783, "This query can't match anywhere");
                }

                else {

                    // Serialize a Lucene SpanQuery based on the SpanQueryWrapper
                    this.spanQuery = qw.toQuery();

                    // TODO: Make these information query rewrites

                    // Throw a warning in case the root object is optional
                    if (qw.isOptional())
                        this.addWarning(781, "Optionality of query is ignored");

                    // Throw a warning in case the root object is negative
                    if (qw.isNegative())
                        this.addWarning(782, "Exclusivity of query is ignored");
                };
            }
            catch (QueryException q) {
                this.addError(q.getErrorCode(), q.getMessage());
            };
        }
        else
            this.addError(700, "No query given");

        // <legacycode>
        if (json.has("warning") && json.get("warning").asText().length() > 0) {
            this.addWarning(799, json.get("warning").asText());
        };
        // </legacycode>

        // Copy notifications from request
        this.copyNotificationsFrom(json);

        // Parse "collection" or "collections" attribute
        try {
        	if (json.has("corpus")) {
                final JsonNode collNode = json.get("corpus");

                // TODO: Temporary
                if (collNode.fieldNames().hasNext()) {
                	KrillCollection kc = new KrillCollection().fromKoral(collNode);
                	kc.isCorpus=true;
                    this.setCollection(kc);
                };
            }
        	// EM: legacy
        	else if (json.has("collection")) {
                final JsonNode collNode = json.get("collection");

                // TODO: Temporary
                if (collNode.fieldNames().hasNext()) {
                	KrillCollection kc = new KrillCollection().fromKoral(collNode);
                	kc.isCorpus=false;
                    this.setCollection(kc);
                };
            }

            else if (json.has("collections")) {
                this.addError(899,
                        "Collections are not supported anymore in favour of a single collection");
            };
        }
        catch (QueryException q) {
            this.addError(q.getErrorCode(), q.getMessage());
        };

        // Parse "meta" attribute
        // !this.hasErrors() && 
        if (json.has("meta"))
            this.setMeta(new KrillMeta(json.get("meta")));

        return this;
    };


    /**
     * Get the associated {@link KrillIndex} object.
     * 
     * @return The associated {@link KrillIndex} object.
     */
    public KrillIndex getIndex () {
        return this.index;
    };


    /**
     * Set the {@link KrillIndex} object.
     * 
     * @param index
     *            The associated {@link KrillIndex} object.
     * @return The {@link Krill} object for chaining.
     */
    public Krill setIndex (KrillIndex index) {
        this.index = index;
            VirtualCorpusCache.setIndexInfo(index);
        return this;
    };


    /**
     * Apply the KoralQuery to an index.
     * This may invoke different actions depending
     * on the meta information, like {@link KrillIndex#search} or
     * {@link KrillIndex#collect}.
     * 
     * @param index
     *            The {@link KrillIndex} the search should be applyied
     *            to.
     * @return The result as a {@link Result} object.
     */
    public Result apply (KrillIndex index) {
        VirtualCorpusCache.setIndexInfo(index);
        return this.setIndex(index).apply();
    };


    /**
     * Apply the KoralQuery to an index.
     * This may invoke different actions depending
     * on the meta information, like {@link KrillIndex#search} or
     * {@link KrillIndex#collect}.
     * 
     * @return The result as a {@link Result} object.
     */
    public Result apply () {

        // Create new Result object to return
        Result kr = new Result();

        // There were errors
        if (this.hasErrors()) {
            kr.copyNotificationsFrom(this);
        }

        // There was no index
        else if (this.index == null) {
            kr.addError(601, "Unable to find index");
        }

        // Apply search
        else {
            // This contains meta and matches
            kr = this.index.search(this);
            // this.getCollection().setIndex(this.index);
            kr.copyNotificationsFrom(this);
        };

        kr.setQuery(this.getQuery());

        kr.setCollection(this.getCollection());
        kr.setMeta(this.getMeta());

        return kr;
    };


    /**
     * Get the associated {@link SpanQuery} deserialization
     * (i.e. the internal correspandence to KoralQuery's query
     * object).
     * 
     * <strong>Warning</strong>: SpanQueries may be lazy deserialized
     * in future versions of Krill, rendering this API obsolete.
     * 
     * @return The deserialized {@link SpanQuery} object.
     */
    @Deprecated
    public SpanQuery getSpanQuery () {
        return this.spanQuery;
    }

    //EM
    public void setSpanQuery (SpanQuery sq) {
        this.spanQuery = sq;
        
    }

    public JsonNode retrieveFieldValues (String corpusQuery, KrillIndex index,
            String fieldName) {
        KrillCollection kc = new KrillCollection(corpusQuery);
        List<String> fieldValues = index.getFieldVector(fieldName, kc);
        VirtualCorpusResponse r = new VirtualCorpusResponse();
        return r.createKoralQueryForField(fieldName, fieldValues);
    }


    public int getMaxTokenMatchSize () {
        return maxTokenMatchSize;
    }


    public void setMaxTokenMatchSize (int maxMatchTokens) {
        this.maxTokenMatchSize = maxMatchTokens;
    }
};
