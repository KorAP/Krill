package de.ids_mannheim.korap;

import java.util.*;
import java.io.IOException;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.NumericRangeFilter;
import org.apache.lucene.search.Filter;

import de.ids_mannheim.korap.KorapIndex;
import de.ids_mannheim.korap.KorapResult;
import de.ids_mannheim.korap.KorapFilter;

import de.ids_mannheim.korap.util.KorapDate;
import de.ids_mannheim.korap.filter.BooleanFilter;
import de.ids_mannheim.korap.filter.FilterOperation;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.FilteredQuery;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.util.FixedBitSet;
import org.apache.lucene.util.Bits;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.DocIdSet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: Make a cache for the bits!!! DELETE IT IN CASE OF AN EXTENSION OR A FILTER!


// accepts as first parameter the index
// THIS MAY CHANGE for stuff like combining virtual collections
// See http://mail-archives.apache.org/mod_mbox/lucene-java-user/200805.mbox/%3C17080852.post@talk.nabble.com%3E

public class KorapCollection {
    private KorapIndex index;
    private KorapDate created;
    private String id;
    private String error;
    private ArrayList<FilterOperation> filter;
    private int filterCount = 0;
    
    // Logger
    private final static Logger log = LoggerFactory.getLogger(KorapCollection.class);

    // user?
    public KorapCollection (KorapIndex ki) {
	this.index = ki;
	this.filter = new ArrayList<FilterOperation>(5);
    };

    public KorapCollection (String jsonString) {
	this.filter = new ArrayList<FilterOperation>(5);
	ObjectMapper mapper = new ObjectMapper();
	try {
	    JsonNode json = mapper.readValue(jsonString, JsonNode.class);
	    if (json.has("meta")) {
		for (JsonNode meta : json.get("meta")) {
		    this.fromJSON(meta);
		};
	    };
	}
	catch (Exception e) {
	    this.error = e.getMessage();
	};
    };

    public KorapCollection () {
	this.filter = new ArrayList<FilterOperation>(5);
    };

    public void fromJSON(JsonNode json) {
	String type = json.get("@type").asText();

	if (type.equals("korap:meta-filter")) {
	    this.filter(new BooleanFilter(json.get("@value")));
	}
	else if (type.equals("korap:meta-extend")) {
	    this.extend(new BooleanFilter(json.get("@value")));
	};
    };

    public int getCount() {
	return this.filterCount;
    };

    public void setIndex (KorapIndex ki) {
	this.index = ki;
    };

    // The checks asre not necessary
    public KorapCollection filter (BooleanFilter filter) {
	log.trace("Added filter: {}", filter.toString());
	if (filter == null) {
	    log.warn("No filter is given");
	    return this;
	};
	Filter f = (Filter) new QueryWrapperFilter(filter.toQuery());
	if (f == null) {
	    log.warn("Filter can't be wrapped");
	    return this;
	};
	FilterOperation fo = new FilterOperation(f,false);
	if (fo == null) {
	    log.warn("Filter operation invalid");
	    return this;
	};
	this.filter.add(fo);
	this.filterCount++;
	return this;
    };

    public KorapCollection extend (BooleanFilter filter) {
	log.trace("Added extension: {}", filter.toString());
	this.filter.add(
	    new FilterOperation(
				(Filter) new QueryWrapperFilter(filter.toQuery()),
                true
            )
        );
	this.filterCount++;
	return this;
    };

    public ArrayList<FilterOperation> getFilters () {
	return this.filter;
    };

    public FilterOperation getFilter (int i) {
	return this.filter.get(i);
    };


    // DEPRECATED BUT USED IN TEST CASES
    public KorapResult search (SpanQuery query) {
	return this.index.search(this, query, 0, (short) 20, true, (short) 5, true, (short) 5);
    };

    public FixedBitSet bits (AtomicReaderContext atomic) throws IOException  {

	/*
	  TODO:
	  Don't check the live docs in advance - combine them afterwards with an "and" operation,
	  so before this you can fully use "and" and "or" on an empty bitset.
	  Use Bits.MatchAllBits(int len)
	*/

	boolean noDoc = true;
	FixedBitSet bitset;

	if (this.filterCount > 0) {
	    bitset = new FixedBitSet(atomic.reader().numDocs());

	    ArrayList<FilterOperation> filters = (ArrayList<FilterOperation>) this.filter.clone();

	    FilterOperation kcInit = filters.remove(0);
	    log.trace("FILTER: {}", kcInit);


	    // Init vector
	    DocIdSet docids = kcInit.filter.getDocIdSet(atomic, null);
	    DocIdSetIterator filterIter = docids.iterator();

	    if (filterIter != null) {
		log.trace("InitFilter has effect");
		bitset.or(filterIter);
		noDoc = false;
	    };

	    if (!noDoc) {
		for (FilterOperation kc : filters) {
		    log.trace("FILTER: {}", kc);

		    // BUG!!!
		    docids = kc.filter.getDocIdSet(atomic, kc.isExtension() ? null : bitset);
		    filterIter = docids.iterator();

		    if (filterIter == null) {
			// There must be a better way ...
			if (kc.isFilter()) {
			    bitset.clear(0, bitset.length());
			    noDoc = true;
			};
			continue;
		    };
		    if (kc.isExtension()) {
			bitset.or(filterIter);
		    }
		    else {
			bitset.and(filterIter);
		    };
		};

		if (!noDoc) {
		    FixedBitSet livedocs = (FixedBitSet) atomic.reader().getLiveDocs();
		    if (livedocs != null) {
			bitset.and(livedocs);
		    };
		};
	    }
	    else {
		return bitset;
	    };
	}
	else {
	    bitset = (FixedBitSet) atomic.reader().getLiveDocs();
	};

	return bitset;
    };

    public long numberOf (String foundry, String type) throws IOException {
	if (this.index == null)
	    return (long) 0;

	return this.index.numberOf(this, foundry, type);
    };

    public long numberOf (String type) throws IOException {
	if (this.index == null)
	    return (long) 0;

	return this.index.numberOf(this, "tokens", type);
    };

    // implement "till" with rangefilter
};
