package de.ids_mannheim.korap;

import java.io.*;

import org.apache.lucene.search.spans.SpanQuery;
import de.ids_mannheim.korap.query.wrap.SpanQueryWrapperInterface;
import de.ids_mannheim.korap.KorapCollection;
import de.ids_mannheim.korap.KorapIndex;
import de.ids_mannheim.korap.KorapResult;

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
    };

    public KorapSearch (String json) {
	ObjectMapper mapper = new ObjectMapper();
	try {
	    JsonNode rootNode = mapper.readValue(json, JsonNode.class);

	    this.query = new KorapQuery("tokens").fromJSON(rootNode.get("query")).toQuery();
	}
	catch (IOException e) {
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

    public short getCount () {
	return this.count;
    };

    public short getCountMax () {
	return this.countMax;
    };

    public KorapSearch setCount (int value) {
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
	this.getCollection().setIndex(ki);
	return ki.search(this.getCollection(), this);
    };
};