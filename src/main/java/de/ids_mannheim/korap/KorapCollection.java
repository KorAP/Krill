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
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.FilteredQuery;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.util.FixedBitSet;
import org.apache.lucene.util.Bits;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.DocIdSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


// accepts as first parameter the index
// THIS MAY CHANGE for stuff like combining virtual collections
// See http://mail-archives.apache.org/mod_mbox/lucene-java-user/200805.mbox/%3C17080852.post@talk.nabble.com%3E


public class KorapCollection {
    private KorapIndex index;
    private String id;
    private KorapDate created;
    private ArrayList<Filter> filter;
    private int filterCount = 0;

    // Logger
    private final static Logger log = LoggerFactory.getLogger(KorapCollection.class);


    // user?

    public KorapCollection (KorapIndex ki) {
	this.index = ki;
	this.filter = new ArrayList<Filter>(5);
    };

    public int getCount() {
	return this.filterCount;
    };

    public void filter (BooleanFilter filter) {
	this.filter.add(new QueryWrapperFilter(filter.toQuery()));
	this.filterCount++;
    };

    public ArrayList<Filter> getFilters () {
	return this.filter;
    };


    public KorapResult search (SpanQuery query) {
	return this.index.search(this, query, 0, (short) 5, true, (short) 5, true, (short) 5);
    };

    public Bits bits (AtomicReaderContext atomic) throws IOException  {

	/*
	  TODO:
	  Don't check the live docs in advance - combine them afterwards with an "and" operation,
	  so before this you can fully use "and" and "or" on an empty bitset.
	*/

	Bits bitset = (Bits) atomic.reader().getLiveDocs();

	if (this.filterCount > 0) {
	    FixedBitSet fbitset = new FixedBitSet(atomic.reader().numDocs());

	    ArrayList<Filter> filters = (ArrayList<Filter>) this.filter.clone();

	    // Init vector
	    if (bitset == null) {
		DocIdSet docids = filters.remove(0).getDocIdSet(atomic, null);
		DocIdSetIterator filterIter = docids.iterator();
		fbitset.or(filterIter);
	    };

	    for (Filter kc : filters) {
		log.trace("FILTER: {}", kc);
		DocIdSet docids = kc.getDocIdSet(atomic, bitset);
		DocIdSetIterator filterIter = docids.iterator();
		fbitset.and(filterIter);
	    };
	    
	    bitset = fbitset.bits();
	};

	return bitset;
    };

    public long numberOf (String foundry, String type) {
	return this.index.numberOf(this, foundry, type);
    };

    // implement "till" with rangefilter
};

/*

Spans spans = yourSpanQuery.getSpans(reader);
BitSet bits = yourFilter.bits(reader);
int filterDoc = bits.nextSetBit(0);
while ((filterDoc >= 0) and spans.skipTo(filterDoc)) {
  boolean more = true;
  while (more and (spans.doc() == filterDoc)) {
     // use spans.start() and spans.end() here
     // ...
     more = spans.next();
  }
  if (! more) {
    break;
  }
  filterDoc = bits.nextSetBit(spans.doc());
}

Please check the javadocs of java.util.BitSet, there may
be a 1 off error in the arguments to nextSetBit().

At this point, no skipping on the spans should be done when filterDoc 
equals spans.doc(), so this code still needs some work.
But I think you get the idea.

*/