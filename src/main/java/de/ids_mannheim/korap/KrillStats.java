package de.ids_mannheim.korap;

import java.util.*;
import java.io.IOException;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.ids_mannheim.korap.response.Notifications;

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create a Statistics object.
 * 
 * This is early work and highliy experimental!
 *
 * <blockquote><pre>
 * KrillStats ks = new KrillStats(json);
 * </pre></blockquote>
 *
 * Should serialize to something like
 *
 * "stats" : {
 *   "@type" : "koral:stats",
 *   "collection" : [
 *     {
 *       "@type" : "stats:collection",
 *       "foundry" : "base",
 *       "layer" : "s",
 *       "key" : "s",
 *       "value" : 450
 *     },
 *     {
 *       "@type" : "stats:collection",
 *       "key" : "texts",
 *       "value" : 2
 *     }
 *   ]
 * }
 *
 * 
 * @author diewald
 */
/*
 * TODO: THIS IS CURRENTLY HIGHLY EXPERIMENTAL
 */
public final class KrillStats extends Notifications {

    // Logger
    private final static Logger log = LoggerFactory
            .getLogger(KrillStats.class);

    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;


    /**
     * Construct a new KrillStats.
     * 
     */
    public KrillStats () {};

    @Override
    public JsonNode toJsonNode () {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode json = mapper.createObjectNode();

        json.put("@type", "koral:stats");

        return (JsonNode) json;
    }    
};
