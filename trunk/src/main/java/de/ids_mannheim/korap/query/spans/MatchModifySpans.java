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

public class MatchModifySpans extends Spans {
    private List<byte[]> highlightedPayload;
    private Collection<byte[]> payload;
    private final Spans spans;
    private byte number;
    private boolean divide;
    private ByteBuffer bb;

    private SpanQuery highlight;
    private final Logger log = LoggerFactory.getLogger(MatchModifySpans.class);

    private int start = -1, end;
    private int tempStart, tempEnd = 0;


    public MatchModifySpans (SpanQuery highlight,
		       AtomicReaderContext context,
		       Bits acceptDocs,
		       Map<Term,TermContext> termContexts,
		       byte number,
		       boolean divide) throws IOException {
	spans = highlight.getSpans(context, acceptDocs, termContexts);
	this.number = number;
	this.divide = divide;
	this.highlight = highlight;
	this.highlightedPayload = new ArrayList<byte[]>(6);
	bb = ByteBuffer.allocate(9);
    };

    @Override
    public Collection<byte[]> getPayload() throws IOException {
	return highlightedPayload;
    };

    @Override
    public boolean isPayloadAvailable() {
	return highlightedPayload.isEmpty() == false;
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
	log.trace("||> Forward next");

	highlightedPayload.clear();

	/*
	  Bei divide:
	  Ist der Speicher leer?
	  Sonst der nächste Treffer vom Speicher!
	*/

	if (spans.next()) {
	    start = -1;
	    if (spans.isPayloadAvailable()) {
		end = 0;

		for (byte[] payload : spans.getPayload()) {
		    bb.clear();
		    bb.put(payload);
		    //		    bb = ByteBuffer.wrap(payload, 0, 10);
		    bb.position(8);

		    // Todo: Implement Divide
		    if (payload.length == 9 && bb.get() == this.number) {
			bb.rewind();
			tempStart = bb.getInt();
			tempEnd = bb.getInt();

			log.trace("Found matching class {}-{}", tempStart, tempEnd);

			if (start == -1)
			    start = tempStart;
			else if (tempStart < start)
			    start = tempStart;

			if (tempEnd > end)
			    end = tempEnd;
		    }
		    else {
			log.trace("Remember old payload {}", payload);
			highlightedPayload.add(payload);
		    };
		};

		log.trace("All payload processed, now clean up");

		if (start != -1) {
		    int i = highlightedPayload.size() - 1;

		    for (; i >= 0; i--) {
			bb.clear();
			bb.put(highlightedPayload.get(i),0,8);
			bb.rewind();
			if (bb.getInt() < start || bb.getInt() > end) {
			    bb.rewind();
			    log.trace("Remove highlight {} with {}-{} for {}-{}", i, bb.getInt(), bb.getInt(), start, end);
			    highlightedPayload.remove(i);
			    continue;
			};
 			bb.rewind();
			log.trace("Highlight {} will stay with {}-{} for {}-{}", i, bb.getInt(), bb.getInt(), start, end);
		    };
		    /*
		     * Todo: SPLIT
		     * Vorsicht! Bei divide könnten Payloads mehrmals vergeben werden
		     * müssen!
		     */
		};
	    };


	    if (start == -1) {
		start = spans.start();
		end = spans.end();
	    }
	    else {
		log.trace("Start to shrink to {} - {} class: {}",
			  start, end, number);
	    };

	    return true;
	};
	return false;
    };

    // inherit javadocs
    @Override
    public boolean skipTo(int target) throws IOException {
	return spans.skipTo(target);
    };

    @Override
    public String toString() {
	return getClass().getName() + "(" + this.highlight.toString() + ")@" +
	    (doc() + ":" + start() + "-" + end());
    };


    @Override
    public long cost() {
	return spans.cost();
    }
};
