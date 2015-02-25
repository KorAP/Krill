package de.ids_mannheim.korap;

import java.io.*;
import java.util.*;

import org.apache.lucene.search.spans.SpanQuery;
import de.ids_mannheim.korap.query.wrap.SpanQueryWrapper;
import de.ids_mannheim.korap.KorapIndex;
import de.ids_mannheim.korap.KorapResult;
import de.ids_mannheim.korap.util.QueryException;
import de.ids_mannheim.korap.response.Notifications;
import de.ids_mannheim.korap.response.KorapResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Krill is a corpus data retrieval index using Lucene for Look-Ups.
 * It is the reference implementation for KoralQuery consumption,
 * and supports specified query and collection objects,
 * and proprietary meta objects.
 *
 * <blockquote><pre>
 *   // Create a new krill search object passing a KoralQuery string
 *   Krill krill = new Krill(koralQueryString);
 *
 *   // Run the query on an index - receive a search result
 *   KrillResult kr = krill.apply(new KrillIndex());
 * </pre></blockquote>
 *
 * @author diewald
 * @author margaretha
 *
 * @see KrillMeta
 * @see KorapCollection
 * @see KorapQuery
 * @see KorapIndex
 */
/*
 * Todo: Use a configuration file
 */
public class Krill extends KorapResponse {
    private KorapIndex index;
    private SpanQuery spanQuery;
    private JsonNode request;
    private String spanContext;

    // Logger
    private final static Logger log = LoggerFactory.getLogger(Krill.class);
    

    /**
     * Construct a new Krill object.
     */
    public Krill () {};


    /**
     * Construct a new Krill object,
     * consuming a KoralQuery json string.
     *
     * @param query The KoralQuery json string.
     */
    public Krill (String query) {
        this.fromJson(query);
    };


    /**
     * Construct a new Krill object,
     * consuming a KoralQuery {@link JsonNode} object.
     *
     * @param query The KoralQuery {@link JsonNode} object.
     */
    public Krill (JsonNode query) {
        this.fromJson(query);
    };


    /**
     * Construct a new Krill object,
     * consuming a {@link SpanQueryWrapper} object.
     *
     * @param query The {@link SpanQueryWrapper} object.
     */
    public Krill (SpanQueryWrapper query) {
        try {
            this.spanQuery = query.toQuery();
        }
        catch (QueryException q) {
            this.addError(q.getErrorCode(), q.getMessage());
        };
    };
    

    /**
     * Construct a new Krill object,
     * consuming a {@link SpanQuery} object.
     *
     * @param query The {@link SpanQuery} object.
     */
    public Krill (SpanQuery query) {
        this.spanQuery = query;
    };


    /**
     * Parse KoralQuery as a json string.
     *
     * @param query The KoralQuery json string.
     * @return The {@link Krill} object for chaining.
     * @throws QueryException
     */
    public Krill fromJson (String query) {
        // Parse query string
        try {
            ObjectMapper mapper = new ObjectMapper();
            this.request = mapper.readTree(query);
            this.fromJson(this.request);
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
     * @param query The KoralQuery {@link JsonNode} object.
     * @return The {@link Krill} object for chaining.
     * @throws QueryException
     */
    public Krill fromJson (JsonNode json) {

        // Parse "query" attribute
        if (json.has("query")) {
            try {
                KorapQuery kq = new KorapQuery("tokens");
                SpanQueryWrapper qw = kq.fromJson(json.get("query"));
                this.setQuery(kq);

                // Throw an error, in case the query matches everywhere
                if (qw.isEmpty())
                    this.addError(780, "This query matches everywhere");

                else {
                    // Serialize a Lucene SpanQuery based on the SpanQueryWrapper
                    this.spanQuery = qw.toQuery();

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
        if (json.has("warning") &&
            json.get("warning").asText().length() > 0) {
            this.addWarning(799, json.get("warning").asText());
        };
        // </legacycode>

        // Copy notifications from request
        this.copyNotificationsFrom(json);
	    
        // Parse virtual collections
        try {
            if (json.has("collection")) {
                this.setCollection(
                    new KorapCollection().fromJson(json.get("collection"))
                );
            }

            // <legacycode>
            else if (json.has("collections")) {
                KorapCollection kc = new KorapCollection();
                for (JsonNode collection : json.get("collections")) {
                    kc.fromJsonLegacy(collection);
                };
                
                this.setCollection(kc);
            };
            // </legacycode>
        }
        catch (QueryException q) {
            this.addError(q.getErrorCode(), q.getMessage());
        };

        // No errors occured - parse meta object
        if (!this.hasErrors() && json.has("meta"))
            this.setMeta(new KrillMeta(json.get("meta")));

        return this;
    };


    /**
     * Get the associated {@link KorapIndex} object.
     *
     * @return The associated {@link KorapIndex} object.
     */
    public KorapIndex getIndex () {
        return this.index;
    };


    /**
     * Set the associated {@link KorapIndex} object.
     *
     * @param index The associated {@link KorapIndex} object.
     */
    public Krill setIndex (KorapIndex index) {
        this.index = index;
        return this;
    };


    /**
     * Apply the KoralQuery to an index.
     *
     * @param index The {@link KorapIndex}
     *        the search should be applyied to.
     * @return The result as a {@link KorapResult} object.
     */
    public KorapResult apply (KorapIndex index) {
        return this.setIndex(index).apply();
    };


    /**
     * Apply the KoralQuery to an index.
     *
     * @return The result as a {@link KorapResult} object.
     */
    public KorapResult apply () {

        // Create new KorapResult object to return
        KorapResult kr = new KorapResult();

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
            kr = this.index.search(this);
            this.getCollection().setIndex(this.index);
            kr.copyNotificationsFrom(this);
        };

        // TODO: Only for development mode
        kr.setRequest(this.request);
        kr.setQuery(this.getQuery());

        return kr;
    };


    @Deprecated
    public JsonNode getRequest () {
        return this.request;
    };


    @Deprecated
    public SpanQuery getSpanQuery () {
        return this.spanQuery;
    };


    @Deprecated
    public Krill setSpanQuery (SpanQueryWrapper sqwi) {
        try {
            this.spanQuery = sqwi.toQuery();
        }
        catch (QueryException q) {
            this.addError(q.getErrorCode(), q.getMessage());
        };
        return this;
    };

    
    @Deprecated
    public Krill setSpanQuery (SpanQuery sq) {
        this.spanQuery = sq;
        return this;
    };
};
