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
/*
import org.apache.lucene.util.Bits.MatchAllBits;
import org.apache.lucene.util.Bits.MatchNoBits;
*/
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.DocIdSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: Make a cache for the bits!!! DELETE IT IN CASE OF AN EXTENSION OR A FILTER!


// accepts as first parameter the index
// THIS MAY CHANGE for stuff like combining virtual collections
// See http://mail-archives.apache.org/mod_mbox/lucene-java-user/200805.mbox/%3C17080852.post@talk.nabble.com%3E

public class KorapCollection {
    private KorapIndex index;
    private String id;
    private KorapDate created;
    private ArrayList<FilterOperation> filter;
    private int filterCount = 0;
    
    // Logger
    private final static Logger log = LoggerFactory.getLogger(KorapCollection.class);

    // user?
    public KorapCollection (KorapIndex ki) {
	this.index = ki;
	this.filter = new ArrayList<FilterOperation>(5);
    };

    public int getCount() {
	return this.filterCount;
    };

    public void filter (BooleanFilter filter) {
	this.filter.add(
	    new FilterOperation(
				(Filter) new QueryWrapperFilter(filter.toQuery()),
                false
            )
        );
	this.filterCount++;
    };

    public void extend (BooleanFilter filter) {
	this.filter.add(
	    new FilterOperation(
				(Filter) new QueryWrapperFilter(filter.toQuery()),
                true
            )
        );
	this.filterCount++;
    };

    public ArrayList<FilterOperation> getFilters () {
	return this.filter;
    };

    // Todo: Create new KorapSearch Object!

    public KorapResult search (SpanQuery query) {
	return this.index.search(this, query, 17, (short) 20, true, (short) 5, true, (short) 5);
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
	return this.index.numberOf(this, foundry, type);
    };

    public long numberOf (String type) throws IOException {
	return this.index.numberOf(this, "tokens", type);
    };

    // implement "till" with rangefilter
};