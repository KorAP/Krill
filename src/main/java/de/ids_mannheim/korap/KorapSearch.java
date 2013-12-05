package de.ids_mannheim.korap;

import java.io.*;

import org.apache.lucene.search.spans.SpanQuery;
import de.ids_mannheim.korap.query.wrap.SpanQueryWrapperInterface;
import de.ids_mannheim.korap.KorapCollection;
import de.ids_mannheim.korap.KorapIndex;
import de.ids_mannheim.korap.KorapResult;
import de.ids_mannheim.korap.util.QueryException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

// Todo: Use configuration file

/*
KorapResult = new KorapSearch(String json).run(KorapIndex ki);
startPage!!!
*/

public class KorapSearch {
    private int startIndex;
    private short count = 25;
    private short countMax = 50;
    // private int limit = -1;
    private SpanQuery query;
    public KorapSearchContext leftContext, rightContext;
    private KorapCollection collection;
    private KorapIndex index;
    private String error;

    {
	leftContext = new KorapSearchContext();
	rightContext = new KorapSearchContext();
    };

    public class KorapSearchContext {
	private boolean type = true;
	private short length = 6;
	private short maxLength = 12;

	public boolean isToken () {
	    return this.type;
	};

	public boolean isCharacter () {
	    return !(this.type);
	};

	public KorapSearchContext setToken (boolean value) {
	    this.type = value;
	    return this;
	};

	public KorapSearchContext setCharacter (boolean value) {
	    this.type = !(value);
	    return this;
	};

	public short getLength() {
	    return this.length;
	};

	public KorapSearchContext setLength (short value) {
	    if (value >= 0) {
		if (value <= maxLength) {
		    this.length = value;
		}
		else {
		    this.length = this.maxLength;
		}
	    };
	    return this;
	};

	public KorapSearchContext setLength (int value) {
	    return this.setLength((short) value);
	};

	public void fromJSON (JsonNode json) {
	    String type = json.get(0).asText();
	    if (type.equals("token")) {
		this.setToken(true);
	    }
	    else if (type.equals("char")) {
		this.setCharacter(true);
	    };
	    this.setLength(json.get(1).asInt());
	};
    };


    public KorapSearch (String jsonString) {
	ObjectMapper mapper = new ObjectMapper();
	try {
	    JsonNode json = mapper.readValue(jsonString, JsonNode.class);

	    if (json.has("query")) {
		try {
		    this.query = new KorapQuery("tokens").fromJSON(json.get("query")).toQuery();
		}
		catch (QueryException q) {
		    this.error = q.getMessage();
		};
	    }
	    else {
		this.error = "No query defined";
	    };

	    if (json.has("meta")) {
		KorapCollection kc = new KorapCollection(jsonString);
		this.setCollection(kc);
	    };

	    if (this.error == null) {

		// Defined count
		if (json.has("count"))
		    this.setCount(json.get("count").asInt());

		// Defined startIndex
		if (json.has("startIndex"))
		    this.setStartIndex(json.get("startIndex").asInt());

		// Defined startPage
		if (json.has("startPage"))
		    this.setStartPage(json.get("startPage").asInt());

		// Defined contexts
		if (json.has("context")) {
		    JsonNode context = json.get("context");
		    if (context.has("left"))
			this.leftContext.fromJSON(context.get("left"));
		    if (context.has("right"))
			this.rightContext.fromJSON(context.get("right"));

		};
	    };
	}
	catch (IOException e) {
	    this.error = e.getMessage();
	};
    };


    // Maybe accept queryWrapperStuff
    public KorapSearch (SpanQueryWrapperInterface sqwi) {
	this.query = sqwi.toQuery();
    };

    public KorapSearch (SpanQuery sq) {
	this.query = sq;
    };

    public SpanQuery getQuery () {
	return this.query;
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
	    this.startIndex = (value * this.getCount()) - this.getCount();
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

    public KorapSearch setCount (int value) {
	// Todo: Maybe update startIndex with known startPage!
	this.setCount((short) value);
	return this;
    };

    public KorapSearch setCount (short value) {
	if (value > 0) {
	    if (value <= this.countMax) {
		this.count = value;
	    }
	    else {
		this.count = this.countMax;
	    };
	};
	return this;
    };

    public KorapSearch setCollection (KorapCollection kc) {
	this.collection = kc;
	return this;
    };

    public KorapCollection getCollection () {
	if (this.collection == null)
	    this.collection = new KorapCollection();

	return this.collection;
    };

    public KorapResult run (KorapIndex ki) {
	if (this.error != null) {
	    KorapResult kr = new KorapResult();
	    kr.setError(this.error);
	    return kr;
	};

	this.getCollection().setIndex(ki);
	return ki.search(this.getCollection(), this);
    };
};