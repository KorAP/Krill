package de.ids_mannheim.korap.filter;

import java.util.*;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.RegexpQuery;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.NumericRangeQuery;

import de.ids_mannheim.korap.util.KorapDate;
import de.ids_mannheim.korap.filter.RegexFilter;
import de.ids_mannheim.korap.KorapFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/*
  Todo: !not

THE JSON STUFF DEFINITIVELY BELONGS INTO KORAPFILTER

*/

/**
 * @author Nils Diewald
 *
 * BooleanFilter implements a simple API for boolean operations
 * on constraints for KorapFilter.
 */
public class BooleanFilter {
    private String type;

    // Logger
    private final static Logger jlog = LoggerFactory.getLogger(KorapFilter.class);


    private BooleanQuery bool;

    public BooleanFilter () {
	bool = new BooleanQuery();
    };

    public BooleanFilter (JsonNode json) {
	bool = new BooleanQuery();

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
    };


    private void fromJSON (JsonNode json, String field) {
	String type = json.get("@type").asText();

	if (json.has("@field"))
	    field = _getField(json);

	if (type.equals("korap:term")) {
	    if (field != null && json.has("@value"))
		this.and(field, json.get("@value").asText());
	    return;
	}
	else if (type.equals("korap:group")) {
	    if (!json.has("relation"))
		return;

	    String date, till;

	    switch (json.get("relation").asText())  {
	    case "between":
		date = _getDate(json, 0);
		till = _getDate(json, 1);
		if (date != null && till != null)
		    this.between(date, till);
		break;

	    case "until":
		date = _getDate(json, 0);
		if (date != null)
		    this.till(date);
		break;

	    case "since":
		date = _getDate(json, 0);
		if (date != null)
		    this.since(date);
		break;

	    case "equals":
		date = _getDate(json, 0);
		if (date != null)
		    this.date(date);
		break;
	    };
	}
    };

    private static String  _getField (JsonNode json)  {
	if (!json.has("@field"))
	    return (String) null;

	String field = json.get("@field").asText();
	return field.replaceFirst("korap:field#", "");
    };

    private static String _getDate (JsonNode json, int index) {
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

    public BooleanFilter or (String type, String ... terms) {
	for (String term : terms) {
	    bool.add(
	        new TermQuery(new Term(type, term)),
		BooleanClause.Occur.SHOULD
	    );
	};
	return this;
    };

    public BooleanFilter or (String type, RegexFilter value) {
	bool.add(
          value.toQuery(type),
          BooleanClause.Occur.SHOULD
        );
	return this;
    };

    public BooleanFilter or (BooleanFilter bf) {
	bool.add(
 	    bf.toQuery(),
	    BooleanClause.Occur.SHOULD
        );
	return this;
    };

    public BooleanFilter or (NumericRangeQuery<Integer> nrq) {
	bool.add(nrq, BooleanClause.Occur.SHOULD);
	return this;
    };

    public BooleanFilter and (String type, String ... terms) {
	for (String term : terms) {
	    bool.add(
	        new TermQuery(new Term(type, term)),
		BooleanClause.Occur.MUST
	    );
	};
	return this;
    };

    public BooleanFilter and (String type, RegexFilter value) {
	bool.add(
          value.toQuery(type),
          BooleanClause.Occur.MUST
        );
	return this;
    };

    public BooleanFilter and (BooleanFilter bf) {
	bool.add(
 	    bf.toQuery(),
	    BooleanClause.Occur.MUST
        );
	return this;
    };

    public BooleanFilter since (String date) {
	int since = new KorapDate(date).floor();

	if (since == 0 || since == KorapDate.BEGINNING)
	    return this;

	bool.add(
	    NumericRangeQuery.newIntRange(
	        "pubDate",
		since,
		KorapDate.END,
		true,
		true
	    ),
	    BooleanClause.Occur.MUST
	);

	return this;
    };


    public BooleanFilter till (String date) {
	try {
	    int till =  new KorapDate(date).ceil();
	    if (till == 0 || till == KorapDate.END)
		return this;

	    bool.add(
                NumericRangeQuery.newIntRange(
  	            "pubDate",
                    KorapDate.BEGINNING,
                    till,
                    true,
                    true
                ),
	        BooleanClause.Occur.MUST
	    );
	}
	catch (NumberFormatException e) {
	    jlog.warn("Parameter of till(date) is invalid");
	};
	return this;
    };


    public BooleanFilter between (String beginStr, String endStr) {
	KorapDate beginDF = new KorapDate(beginStr);

	int begin = beginDF.floor();

	int end = new KorapDate(endStr).ceil();

	if (end == 0)
	    return this;

	if (begin == KorapDate.BEGINNING && end == KorapDate.END)
	    return this;

	if (begin == end) {
	    this.and("pubDate", beginDF.toString());
	    return this;
	};

	this.bool.add(
	    NumericRangeQuery.newIntRange(
	        "pubDate",
		begin,
		end,
		true,
		true
	    ),
	    BooleanClause.Occur.MUST
        );
	return this;
    };


    public BooleanFilter date (String date) {
	KorapDate dateDF = new KorapDate(date);

	if (dateDF.year() == 0)
	    return this;

	if (dateDF.day() == 0 || dateDF.month() == 0) {
	    int begin = dateDF.floor();
	    int end = dateDF.ceil();

	    if (end == 0 || (begin == KorapDate.BEGINNING && end == KorapDate.END))
		return this;
	    
	    this.bool.add(
	        NumericRangeQuery.newIntRange(
		    "pubDate",
		    begin,
		    end,
		    true,
		    true
		),
		BooleanClause.Occur.MUST
	    );
	    return this;
	};
	
	this.and("pubDate", dateDF.toString());
	return this;
    };

    
    public Query toQuery () {
	return this.bool;
    };

    public String toString () {
	return this.bool.toString();
    };
};
