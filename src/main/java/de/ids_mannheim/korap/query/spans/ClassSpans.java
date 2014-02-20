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

public class ClassSpans extends Spans {
    private List<byte[]> highlightedPayload;
    private Collection<byte[]> payload;
    private final Spans spans;
    private byte number;
    private ByteBuffer bb;
    private SpanQuery highlight;
    private Boolean hasmorespans = false;

    private final Logger log = LoggerFactory.getLogger(ClassSpans.class);

    public ClassSpans (SpanQuery highlight, AtomicReaderContext context, Bits acceptDocs, Map<Term,TermContext> termContexts, byte number) throws IOException {
	spans = highlight.getSpans(context, acceptDocs, termContexts);
	this.number = number;
	this.highlight = highlight;
	this.highlightedPayload = new ArrayList<byte[]>(6);
    };

    @Override
    public Collection<byte[]> getPayload() throws IOException {
	/*
	for (byte[] x: highlightedPayload) {
	    ByteBuffer b = ByteBuffer.wrap(x, 0, x.length);
	    log.trace(">> Get Payload: {}-{} in class {}", b.getInt(), b.getInt(), b.get());
	};
	*/
	return highlightedPayload;
    };

    @Override
    public boolean isPayloadAvailable() {
	// return highlightedPayload.isEmpty() == false;
	return true;
    };

    public int doc() { return spans.doc(); }

    // inherit javadocs
    @Override
    public int start() { return spans.start(); }

    // inherit javadocs
    @Override
    public int end() { return spans.end(); }


    // inherit javadocs
    @Override
    public boolean next() throws IOException {
	log.trace("Forward next");

	if (spans.next()) {
	    hasmorespans = true;

	    highlightedPayload.clear();

	    if (spans.isPayloadAvailable()) {
		highlightedPayload.addAll(spans.getPayload());
		log.trace("Found payload");
	    };


	    log.trace("Start to create class {} with span {} - {}",
		      number,
		      spans.start(),
		      spans.end());

	    // Todo: Better allocate using a Factory!

	    bb = ByteBuffer.allocate(9);

	    bb.putInt(spans.start()).putInt(spans.end()).put(number);
	    // Add highlight information as byte after offsets
	    highlightedPayload.add(bb.array());
	    return true;
	};
	hasmorespans = false;
	return false;
    };

    // inherit javadocs
    @Override
    public boolean skipTo(int target) throws IOException {
	if (hasmorespans && spans.doc() < target)
	    return spans.skipTo(target);
	return false;
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
