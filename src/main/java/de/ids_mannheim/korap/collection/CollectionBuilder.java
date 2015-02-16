package de.ids_mannheim.korap.collection;

import de.ids_mannheim.korap.collection.BooleanFilter;
import de.ids_mannheim.korap.collection.RegexFilter;
import de.ids_mannheim.korap.util.QueryException;
import de.ids_mannheim.korap.util.KorapDate;

import org.apache.lucene.search.Query;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CollectionBuilder implements a simple API for creating queries
 * constituing Virtual Collections.
 *
 * @author diewald
 */
/*
 * Todo: WildCardFilter!
 * Todo: Support delete boolean etc.
 * Todo: Supports foundries
 */
public class CollectionBuilder {
    private BooleanFilter filter;
    private String field = "tokens";

    // Logger
    private final static Logger log = LoggerFactory.getLogger(
        CollectionBuilder.class
    );

    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;
    
    public CollectionBuilder () {
        filter = new BooleanFilter();
    };
    
    public BooleanFilter and (String type, String ... terms) {
        BooleanFilter bf = new BooleanFilter();
        bf.and(type, terms);
        return bf;
    };

    public BooleanFilter or (String type, String ... terms) {
        BooleanFilter bf = new BooleanFilter();
        bf.or(type, terms);
        return bf;
    };

    public BooleanFilter and (String type, RegexFilter re) {
        BooleanFilter bf = new BooleanFilter();
        bf.and(type, re);
        return bf;
    };

    public BooleanFilter or (String type, RegexFilter re) {
        BooleanFilter bf = new BooleanFilter();
        bf.or(type, re);
        return bf;
    };

    public BooleanFilter since (String date) {
        BooleanFilter bf = new BooleanFilter();
        bf.since(date);
        return bf;
    };

    public BooleanFilter till (String date) {
        BooleanFilter bf = new BooleanFilter();
        bf.till(date);
        return bf;
    };

    public BooleanFilter date (String date) {
        BooleanFilter bf = new BooleanFilter();
        bf.date(date);
        return bf;
    };

    public BooleanFilter between (String date1, String date2) {
        BooleanFilter bf = new BooleanFilter();
        bf.between(date1, date2);
        return bf;
    };

    public RegexFilter re (String regex) {
        return new RegexFilter(regex);
    };

    public BooleanFilter getBooleanFilter ()  {
        return this.filter;
    };

    public void setBooleanFilter (BooleanFilter bf) {
        this.filter = bf;
    };

    public Query toQuery () {
        return this.filter.toQuery();
    };

    public String toString () {
        return this.filter.toQuery().toString();
    };
};
