package de.ids_mannheim.korap.query.spans;

import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.Bits;

import java.io.IOException;

import java.util.Map;
import java.util.ArrayList;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * Modify matches to, for example, return only certain class or span ranges.
 *
 * @author diewald
 */

public class MatchModifyClassSpans extends Spans {
    private List<byte[]> wrappedPayload;
    private Collection<byte[]> payload;
    private final Spans spans;
    private byte number;
    private boolean divide;
    private ByteBuffer bb;

    private SpanQuery wrapQuery;
    private final Logger log = LoggerFactory.getLogger(MatchModifyClassSpans.class);

    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = true;

    private int start = -1, end;
    private int tempStart = 0, tempEnd = 0;

    public MatchModifyClassSpans (
      SpanQuery wrapQuery,
      AtomicReaderContext context,
      Bits acceptDocs,
      Map<Term,TermContext> termContexts,
      byte number,
      boolean divide) throws IOException {
	this.spans     = wrapQuery.getSpans(context, acceptDocs, termContexts);
	this.number    = number;
	this.divide    = divide;
	this.wrapQuery = wrapQuery;
	this.bb        = ByteBuffer.allocate(20);
	this.wrappedPayload = new ArrayList<byte[]>(6);
    };

    @Override
    public Collection<byte[]> getPayload() throws IOException {
	return wrappedPayload;
    };

    @Override
    public boolean isPayloadAvailable() {
	return wrappedPayload.isEmpty() == false;
    };

    public int doc() { return spans.doc(); }

    // inherit javadocs
    @Override
    public int start() { return start; }

    // inherit javadocs
    @Override
    public int end() { return end; }


    // inherit javadocs
    @Override
    public boolean next() throws IOException {
	/* TODO:
	 * In case of a split() (instead of a submatch())
	 * Is the cache empty?
	 * Otherwise: Next from list
	 */

	if (DEBUG)
	    log.trace("Forward next match");

	// Next span
	while (spans.next()) {

	    if (DEBUG)
		log.trace("Forward next inner span");

	    // No classes stored
	    wrappedPayload.clear();

	    start = -1;
	    if (spans.isPayloadAvailable()) {
		end = 0;

		// Iterate over all payloads and find the maximum span per class
		for (byte[] payload : spans.getPayload()) {
		    bb.clear();
		    bb.put(payload);
		    bb.position(8);

		    // Todo: Implement Divide
		    // Found class payload of structure <i>start<i>end<b>class
		    if (payload.length == 9 && bb.get() == this.number) {
			bb.rewind();
			tempStart = bb.getInt();
			tempEnd   = bb.getInt();

			if (DEBUG)
			    log.trace("Found matching class {}-{}", tempStart, tempEnd);

			// Set start position 
			if (start == -1)
			    start = tempStart;
			else if (tempStart < start)
			    start = tempStart;

			// Set end position
			if (tempEnd > end)
			    end = tempEnd;
		    }

		    // No class payload - but keep!
		    else {
			if (DEBUG)
			    log.trace("Remember old payload {}", payload);
			wrappedPayload.add(payload);
		    };
		};
	    };

	    // Class not found
	    if (start == -1)
		continue;

	    if (DEBUG)
		log.trace(
		    "Start to focus on class {} from {} to {}",
		    number,
		    start,
		    end
		);

	    return true;
	};

	// No more spans
	return false;
    };


    // inherit javadocs
    @Override
    public boolean skipTo (int target) throws IOException {
	return spans.skipTo(target);
    };

    @Override
    public String toString () {
	return getClass().getName() + "(" + this.wrapQuery.toString() + ")@" +
	    (doc() + ":" + start() + "-" + end());
    };


    @Override
    public long cost () {
	return spans.cost();
    };
};
