package de.ids_mannheim.korap.collection;

import de.ids_mannheim.korap.collection.BooleanFilterOperation;
import de.ids_mannheim.korap.collection.RegexFilter;
import de.ids_mannheim.korap.util.QueryException;
import de.ids_mannheim.korap.util.KrillDate;

import org.apache.lucene.search.Query;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CollectionBuilder implements a simple API for creating queries
 * constituing Virtual Collections.
 * 
 * <strong>Warning</strong>: The API is likely to change.
 * 
 * @author diewald
 */
/*
 * Todo: WildCardFilter!
 * Todo: Support delete boolean etc.
 * Todo: Supports foundries
 */
public class CollectionBuilder {
    private BooleanFilterOperation filter;
    private String field = "tokens";

    // Logger
    private final static Logger log = LoggerFactory
            .getLogger(CollectionBuilder.class);

    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;


    /**
     * Construct a new CollectionBuilder object.
     */
    public CollectionBuilder () {
        filter = new BooleanFilterOperation();
    };


    public BooleanFilterOperation and (String type, String ... terms) {
        BooleanFilterOperation bf = new BooleanFilterOperation();
        bf.and(type, terms);
        return bf;
    };


    public BooleanFilterOperation or (String type, String ... terms) {
        BooleanFilterOperation bf = new BooleanFilterOperation();
        bf.or(type, terms);
        return bf;
    };


    public BooleanFilterOperation and (String type, RegexFilter re) {
        BooleanFilterOperation bf = new BooleanFilterOperation();
        bf.and(type, re);
        return bf;
    };


    public BooleanFilterOperation or (String type, RegexFilter re) {
        BooleanFilterOperation bf = new BooleanFilterOperation();
        bf.or(type, re);
        return bf;
    };


    public BooleanFilterOperation since (String date) {
        BooleanFilterOperation bf = new BooleanFilterOperation();
        bf.since(date);
        return bf;
    };


    public BooleanFilterOperation till (String date) {
        BooleanFilterOperation bf = new BooleanFilterOperation();
        bf.till(date);
        return bf;
    };


    public BooleanFilterOperation date (String date) {
        BooleanFilterOperation bf = new BooleanFilterOperation();
        bf.date(date);
        return bf;
    };


    public BooleanFilterOperation between (String date1, String date2) {
        BooleanFilterOperation bf = new BooleanFilterOperation();
        bf.between(date1, date2);
        return bf;
    };


    public RegexFilter re (String regex) {
        return new RegexFilter(regex);
    };


    public BooleanFilterOperation getBooleanFilterOperation () {
        return this.filter;
    };


    public void setBooleanFilterOperation (BooleanFilterOperation bf) {
        this.filter = bf;
    };


    public Query toQuery () {
        return this.filter.toQuery();
    };


    public String toString () {
        return this.filter.toQuery().toString();
    };
};
