package de.ids_mannheim.korap;

import java.io.*;
import java.util.*;

import org.apache.lucene.search.spans.SpanQuery;
import de.ids_mannheim.korap.query.wrap.SpanQueryWrapper;
import de.ids_mannheim.korap.KorapCollection;
import de.ids_mannheim.korap.KorapIndex;
import de.ids_mannheim.korap.KorapResult;
import de.ids_mannheim.korap.util.QueryException;
import de.ids_mannheim.korap.index.SearchContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

// Todo: Use configuration file

/*
  Todo: Let this class extend KorapResult!
  KorapResult = new KorapSearch(String json).run(KorapIndex ki);
*/

/**
 * @author Nils Diewald
 *
 * KorapSearch implements an object for all search relevant parameters.
 */
public class KorapSearch {
    private int startIndex = 0, limit = 0;
    private short count = 25,
	          countMax = 50;
    private boolean cutOff = false;
    private short itemsPerResource = 0;
    private SpanQuery query;
    private KorapCollection collection;
    private KorapIndex index;
    private String error;
    private String warning;

    private HashSet<String> fields;

    private JsonNode request;

    public SearchContext context;
    private String spanContext;

    {
	context  = new SearchContext();
    };

    public KorapSearch (String jsonString) {
	ObjectMapper mapper = new ObjectMapper();
	try {
	    this.request = mapper.readValue(jsonString, JsonNode.class);
	    
	    // "query" value
	    if (this.request.has("query")) {
		try {
		    SpanQueryWrapper qw = new KorapQuery("tokens").fromJSON(this.request.get("query"));

		    if (qw.isEmpty()) {
			this.error = "This query matches everywhere";
		    }
		    else {
		    
			this.query = qw.toQuery();
			if (qw.isOptional())
			    this.addWarning("Optionality of query is ignored");
			if (qw.isNegative())
			    this.addWarning("Exclusivity of query is ignored");
		    };
		}
		catch (QueryException q) {
		    this.error = q.getMessage();
		};
	    }
	    else {
		this.error = "No query defined";
	    };

	    if (this.request.has("warning"))
		this.addWarning(this.request.get("warning").asText());
	    
	    // virtual collections
	    if (this.request.has("collections") ||
		// Legacy collections
		this.request.has("collections"))
		this.setCollection(new KorapCollection(jsonString));

	    if (this.error == null) {
		if (this.request.has("meta")) {
		    JsonNode meta = this.request.get("meta");

		    // Defined count
		    if (meta.has("count"))
			this.setCount(meta.get("count").asInt());

		    // Defined startIndex
		    if (meta.has("startIndex"))
			this.setStartIndex(meta.get("startIndex").asInt());

		    // Defined startPage
		    if (meta.has("startPage"))
			this.setStartPage(meta.get("startPage").asInt());

		    // Defined cutOff
		    if (meta.has("cutOff"))
			this.setCutOff(meta.get("cutOff").asBoolean());

		    // Defined contexts
		    if (meta.has("context"))
			this.context.fromJSON(meta.get("context"));

		    // Defined resource count
		    if (meta.has("itemsPerResource"))
			this.setItemsPerResource(meta.get("itemsPerResource").asInt());
		};
	    };
	}

	// Unable to parse JSON
	catch (IOException e) {
	    this.error = e.getMessage();
	};
    };

    // Maybe accept queryWrapperStuff
    public KorapSearch (SpanQueryWrapper sqwi) {
	try {
	    this.query = sqwi.toQuery();
	}
	catch (QueryException q) {
	    this.error = q.getMessage();
	};
    };

    public KorapSearch (SpanQuery sq) {
	this.query = sq;
    };

    // Empty constructor
    public KorapSearch () { };

    public String getError () {
	return this.error;
    };

    public String getWarning () {
	return this.warning;
    };

    public void addWarning (String warning) {
	if (this.warning == null)
	    this.warning = warning;
	else
	    this.warning += "; " + warning;
    };

    public SpanQuery getQuery () {
	return this.query;
    };

    public JsonNode getRequest () {
	return this.request;
    };

    public KorapSearch setQuery (SpanQueryWrapper sqwi) {
	try {
	    this.query = sqwi.toQuery();
	}
	catch (QueryException q) {
	    this.error = q.getMessage();
	};
	return this;
    };

    public KorapSearch setQuery (SpanQuery sq) {
	this.query = sq;
	return this;
    };

    public SearchContext getContext () {
	return this.context;
    };

    public KorapSearch setContext (SearchContext context) {
	this.context = context;
	return this;
    };

    public int getStartIndex () {
	return this.startIndex;
    };

    public KorapSearch setStartIndex (int value) {
	if (value >= 0) {
	    this.startIndex = value;
	}
	else {
	    this.startIndex = 0;
	};

	return this;
    };

    public KorapSearch setStartPage (int value) {
	if (value >= 0) {
	    this.setStartIndex((value * this.getCount()) - this.getCount());
	}
	else {
	    this.startIndex = 0;
	};

	return this;
    };

    public short getCount () {
	return this.count;
    };

    public short getCountMax () {
	return this.countMax;
    };

    public int getLimit () {
	return this.limit;
    };

    public KorapSearch setLimit (int limit) {
	if (limit > 0)
	    this.limit = limit;
	return this;
    };

    public boolean doCutOff () {
	return this.cutOff;
    };

    public KorapSearch setCutOff (boolean cutOff) {
	this.cutOff = cutOff;
	return this;
    };

    public KorapSearch setCount (int value) {
	// Todo: Maybe update startIndex with known startPage!
	this.setCount((short) value);
	return this;
    };

    public KorapSearch setCount (short value) {
	if (value > 0) {
	    if (value <= this.countMax)
		this.count = value;
	    else
		this.count = this.countMax;
	};
	return this;
    };

    public KorapSearch setItemsPerResource (short value) {
	if (value >= 0)
	    this.itemsPerResource = value;
	return this;
    };

    public KorapSearch setItemsPerResource (int value) {
	return this.setItemsPerResource((short) value);
    };

    public short getItemsPerResource () {
	return this.itemsPerResource;
    };

    // Get map of fields to lift
    public HashSet<String> getFields () {
	if (this.fields != null)
	    return this.fields;
	return (HashSet<String>) null;
    };

    public KorapSearch setCollection (KorapCollection kc) {
	this.collection = kc;
	if (kc.getError() != null)
	    this.error = kc.getError();
	return this;
    };

    public KorapCollection getCollection () {
	if (this.collection == null)
	    this.collection = new KorapCollection();

	return this.collection;
    };

    public KorapResult run (KorapIndex ki) {
	if (this.query == null) {
	    KorapResult kr = new KorapResult();
	    kr.setRequest(this.request);

	    if (this.error != null)
		kr.setError(this.error);
	    else
		kr.setError(this.getClass() + " expects a query");
	    return kr;
	};

	if (this.error != null) {
	    KorapResult kr = new KorapResult();
	    kr.setRequest(this.request);
	    kr.setError(this.error);
	    if (this.warning != null)
		kr.addWarning(this.warning);
	    return kr;
	};

	this.getCollection().setIndex(ki);
	KorapResult kr = ki.search(this);
	kr.setRequest(this.request);
	if (this.warning != null)
	    kr.addWarning(this.warning);
	return kr;
    };
};
