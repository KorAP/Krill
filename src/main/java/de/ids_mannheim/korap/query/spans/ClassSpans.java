package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author diewald
 */

public class ClassSpans extends Spans {
    private List<byte[]> highlightedPayload;
    private final Spans spans;
    private byte number;
    private SpanQuery highlight;
    private Boolean hasmorespans = false;

    private ByteBuffer bb = ByteBuffer.allocate(9);

    private final static Logger log = LoggerFactory.getLogger(ClassSpans.class);
    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;

    public ClassSpans (SpanQuery highlight,
		       AtomicReaderContext context,
		       Bits acceptDocs,
		       Map<Term,TermContext> termContexts,
		       byte number) throws IOException {
	spans = highlight.getSpans(context, acceptDocs, termContexts);
	this.number = number;
	this.highlight = highlight;
	this.highlightedPayload = new ArrayList<byte[]>(6);
    };

    @Override
    public Collection<byte[]> getPayload() throws IOException {
	return highlightedPayload;
    };

    @Override
    public boolean isPayloadAvailable() {
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
	if (DEBUG)
	    log.trace("Forward next");

	if (spans.next()) {
		addClassPayload();
	    return true;
	};
	hasmorespans = false;
	return false;
    };
    
    private void addClassPayload() throws IOException {
    	hasmorespans = true;

	    highlightedPayload.clear();

	    if (spans.isPayloadAvailable()) {
		highlightedPayload.addAll(spans.getPayload());
		if (DEBUG)
		    log.trace("Found payload");
	    };

	    if (DEBUG)
		log.trace("Start to create class {} with span {} - {}",
			  number,
			  spans.start(),
			  spans.end());

	    // Todo: Better allocate using a Factory!

	    //private
	    bb.clear();
	    bb.putInt(spans.start()).putInt(spans.end()).put(number);
	   // System.out.println(number+":"+spans.start()+","+spans.end());
	    /*
	    if (DEBUG)
		log.trace("Results in {} with {}", bb.toString(), bb.array());
	    */
	    // Add highlight information as byte after offsets
	    highlightedPayload.add(bb.array());
	    /*
	    if (DEBUG) {
		bb.rewind();
		log.trace("That was a class from {}-{} of class {}", bb.getInt(), bb.getInt(), bb.get());
	    };
	    */
		
	}

    // inherit javadocs
    @Override
    public boolean skipTo(int target) throws IOException {
	highlightedPayload.clear();
	if (hasmorespans && spans.doc() < target &&
			spans.skipTo(target)){
		addClassPayload();
	    return true;
	}
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
