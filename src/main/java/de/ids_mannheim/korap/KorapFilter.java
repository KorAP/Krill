package de.ids_mannheim.korap;

import de.ids_mannheim.korap.filter.BooleanFilter;
import de.ids_mannheim.korap.filter.RegexFilter;
import de.ids_mannheim.korap.util.QueryException;

import org.apache.lucene.search.Query;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/*
  Todo: WildCardFilter!
  Support: delete boolean etc.
  Support: supports foundries
*/

/**
 * @author Nils Diewald
 *
 * KorapFilter implements a simple API for creating meta queries
 * constituing Virtual Collections.
 */

/*
Suche XYZ in allen Documenten in den Foundries "Treetagger" und "MATE", die entweder den Texttyp "sports" oder den Texttyp "news" haben, bis höchsten 2009 publiziert wurden und deren Autor auf den regulären Ausdruck "Peter .+?" matcht.

textClass
ID
title
subTitle
author
corpusID
pubDate
pubPlace

Query: (corpusID=BRZ13 | corpusID=WPD) & textClass=wissenschaft

{
    "@type": "korap:filter",
    "filter": {
        "@type": "korap:docGroup",
        "relation": "relation:and",
        "operands": [
            {
                "@type": "korap:docGroup",
                "relation": "relation:or",
                "operands": [
                    {
                        "@type": "korap:doc",
                        "key": "corpusID",
                        "value": "BRZ13",
                        "match": "match:eq"
                    },
                    {
                        "@type": "korap:doc",
                        "key": "corpusID",
                        "value": "WPD",
                        "match": "match:eq"
                    }
                ]
            },
            {
                "@type": "korap:doc",
                "key": "textClass",
                "value": "wissenschaft",
                "match": "match:eq"
            }
        ]
    }
}
*/

public class KorapFilter {
    private BooleanFilter filter;

    // Logger
    private final static Logger log = LoggerFactory.getLogger(KorapFilter.class);

    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;
    
    public KorapFilter () {
	filter = new BooleanFilter();
    };

    public KorapFilter (JsonNode json) throws QueryException {
	filter = this.fromJSON(json, "tokens");
    };

    protected BooleanFilter fromJSON (JsonNode json, String field) throws QueryException {
	String type = json.get("@type").asText();

	// Single filter
	if (type.equals("korap:doc")) {

	    String key     = "tokens";
	    String valtype = "type:string";
	    String match   = "match:eq";

	    if (json.has("key"))
		key = json.get("key").asText();

	    if (json.has("type"))
		valtype = json.get("type").asText();

	    // Filter based on date
	    if (valtype.equals("type:date")) {
		String date = json.get("value").asText();
		if (json.has("match"))
		    match = json.get("match").asText();


		// TODO: This isn't stable yet
		switch (match) {
		case "match:eq":
		    filter.date(date);
		case "match:geq":
		    filter.since(date);
		case "match:leq":
		    filter.till(date);
		};
		/*
		  No good reason for gt or lt
		*/
		return filter;
	    };
	}

	// nested group
	else if (type.equals("korap:docGroup")) {
	}

	// UNknown type
	else {
	    throw new QueryException(613, "Collection query type has to be doc or docGroup");
	};

	return new BooleanFilter();
    };

	/*
	String type = json.get("@type").asText();
	String field = _getField(json);

	if (type.equals("korap:term")) {
	    this.fromJSON(json, field);
	}
	else if (type.equals("korap:group")) {
	    // TODO: relation
	    for (JsonNode operand : json.get("operands")) {
		this.fromJSON(operand, field);
	    };
	};
	*/
    //    };
    
    protected BooleanFilter fromJSONLegacy (JsonNode json, String field) throws QueryException {
	BooleanFilter filter = new BooleanFilter();
	
	String type = json.get("@type").asText();

	if (DEBUG)
	    log.trace("@type: " + type);

	if (json.has("@field"))
	    field = _getFieldLegacy(json);

	if (type.equals("korap:term")) {
	    if (field != null && json.has("@value"))
		filter.and(field, json.get("@value").asText());
	    return filter;
	}
	else if (type.equals("korap:group")) {
	    if (!json.has("relation") || !json.has("operands"))
		return filter;

	    String date, till;

	    if (DEBUG)
		log.trace("relation: " + json.get("relation").asText());

	    BooleanFilter group = new BooleanFilter();
	    
	    switch (json.get("relation").asText())  {
	    case "between":
		date = _getDateLegacy(json, 0);
		till = _getDateLegacy(json, 1);
		if (date != null && till != null)
		    filter.between(date, till);
		break;

	    case "until":
		date = _getDateLegacy(json, 0);
		if (date != null)
		    filter.till(date);
		break;

	    case "since":
		date = _getDateLegacy(json, 0);
		if (date != null)
		    filter.since(date);
		break;

	    case "equals":
		date = _getDateLegacy(json, 0);
		if (date != null)
		    filter.date(date);
		break;

	    case "and":
		for (JsonNode operand : json.get("operands")) {
		    group.and(this.fromJSONLegacy(operand, field));
		};
		filter.and(group);
		break;

	    case "or":
		for (JsonNode operand : json.get("operands")) {
		    group.or(this.fromJSONLegacy(operand, field));
		};
		filter.and(group);
		break;

	    default:
		throw new QueryException(
		    json.get("relation").asText() + " is not a supported relation"
	        );
	    };
	}
	else {
	    throw new QueryException(type + " is not a supported group");
	};
	return filter;
    };
    

    private static String  _getFieldLegacy (JsonNode json)  {
	if (!json.has("@field"))
	    return (String) null;

	String field = json.get("@field").asText();
	return field.replaceFirst("korap:field#", "");
    };

    private static String _getDateLegacy (JsonNode json, int index) {
	if (!json.has("operands"))
	    return (String) null;

	if (!json.get("operands").has(index))
	    return (String) null;

	JsonNode date = json.get("operands").get(index);
	if (!date.get("@type").asText().equals("korap:date"))
	    return (String) null;

	if (!date.has("@value"))
	    return (String) null;

	return date.get("@value").asText();
    };

    
    public BooleanFilter and (String type, String ... terms) {
	BooleanFilter bf = new BooleanFilter();
	bf.and(type, terms);
	return bf;
    };

    public BooleanFilter or (String type, String ... terms) {
	if (DEBUG)
	    log.debug("Got some terms here");
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

    public BooleanFilter getBooleanFilter()  {
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
