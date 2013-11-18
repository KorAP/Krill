package de.ids_mannheim.korap.query.spans;

/* Inspired by NearSpansOrdered
 *
 * REIMPLEMENTATION
 *
 */

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.Bits;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.search.spans.SpanQuery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import de.ids_mannheim.korap.query.SpanNextQuery;

// Todo: Disable the option to discard payloads

import java.util.*;
import java.io.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** From Spans.java:
 * Expert: an enumeration of span matches.  Used to implement span searching.
 * Each span represents a range of term positions within a document.  Matches
 * are enumerated in order, by increasing document number, within that by
 * increasing start position and finally by increasing end position. */
public class NextSpans extends Spans {
    private boolean firstTime = true;
    private boolean more = false;

    // Initialize as invalid
    private int matchDoc   = -1;
    private int matchStart = -1;
    private int matchEnd   = -1;

    /** Indicates that all both spans have the same doc() */
    private boolean inSameDoc = false;

    // First span
    private final Spans firstSpans;
    private final Spans firstSpansByDoc;

    // Second span
    private final Spans secondSpans;
    private final Spans secondSpansByDoc;

    private SpanNextQuery query;

    private List<byte[]> matchPayload;
    private boolean collectPayloads = true;

    private final static Logger log = LoggerFactory.getLogger(NextSpans.class);

    // Constructor
    public NextSpans (SpanNextQuery spanNextQuery,
		      AtomicReaderContext context,
		      Bits acceptDocs,
		      Map<Term,TermContext> termContexts) throws IOException {
	this(spanNextQuery, context, acceptDocs, termContexts, true);
    };

    // Constructor
    public NextSpans (SpanNextQuery spanNextQuery,
		      AtomicReaderContext context,
		      Bits acceptDocs,
		      Map<Term,TermContext> termContexts,
		      boolean collectPayloads) throws IOException {

	log.trace("Init NextSpans");

	//	this.collectPayloads = collectPayloads;

	// Init copies
	matchPayload = new LinkedList<byte[]>();

	firstSpans = spanNextQuery.firstClause().getSpans(
	    context, acceptDocs, termContexts
        );
	firstSpansByDoc = firstSpans; // used in toSameDoc()

	secondSpans = spanNextQuery.secondClause().getSpans(
            context, acceptDocs, termContexts
        );
	secondSpansByDoc = secondSpans; // used in toSameDoc()

	/*
	if (DEBUG) {
	    System.err.println("***");
	    while (subSpans[i].next()) {
		StringBuffer payloadString = new StringBuffer();
		int docid = subSpans[i].doc();
		System.err.println("Span: "+i+" Doc: " + docid + " with " + subSpans[i].start() + "-" + subSpans[i].end() + " || " + payloadString.toString());
	    };
	};
	*/
	query = spanNextQuery; // kept for toString() only.
    };


    /** Move to the next match, returning true iff any such exists. */
    @Override
    public boolean next () throws IOException {
	log.trace("Next with doc {}", matchDoc);

	// Check for init next
	if (firstTime) {
	    log.trace("First retrieval of NextSpans");
	    firstTime = false;
	    if (!firstSpans.next() || !secondSpans.next()) {
		log.trace("No next in firstSpan nor in secondSpan");
		more = false;
		return false;
	    };
	    log.trace("Spans are initialized");
	    more = true;
	};

	//	if (collectPayloads)
	    matchPayload.clear();

	return advance();
    };


    /** Skips to the first match beyond the current, whose document number is
     * greater than or equal to <i>target</i>. <p>Returns true iff there is such
     * a match.  <p>Behaves as if written: <pre class="prettyprint">
     *   boolean skipTo(int target) {
     *     do {
     *       if (!next())
     *         return false;
     *     } while (target > doc());
     *     return true;
     *   }
     * </pre>
     * Most implementations are considerably more efficient than that.
     */
    public boolean skipTo (int target) throws IOException {
	log.trace("skipTo {}", target);

	// Check for init next
	if (firstTime) {
	    firstTime = false;
	    if (!firstSpans.next() && !secondSpans.next()) {
		more = false;
		return false;
	    };
	    more = true;
	}

	// There are more spans, but the doc has to be skipped to target
	// Warning: This only skips firstSpans!
	//          Maybe that's wrong ...
	else if (more && (firstSpans.doc() < target)) {
	    if (firstSpans.skipTo(target)) {
		inSameDoc = false;
	    }

	    else {
		more = false;
		return false;
	    };
	};

	//	if (collectPayloads)
	    matchPayload.clear();

	return advance();
    };


    /** Advance the subSpans to the same document */
    private boolean toSameDoc() throws IOException {
	log.trace("toSameDoc");

	if (firstSpansByDoc.doc() < secondSpansByDoc.doc()) {
	    if (!firstSpansByDoc.skipTo(secondSpansByDoc.doc())) {
		more = false;
		inSameDoc = false;
		return false;
	    };
	}
	else if (firstSpansByDoc.doc() > secondSpansByDoc.doc()) {
	    if (!secondSpansByDoc.skipTo( firstSpansByDoc.doc() )) {
		more = false;
		inSameDoc = false;
		return false;
	    };
	};
	inSameDoc = true;
	return true;
    };


    /** Advances the subSpans to just after an ordered match with a minimum slop
     * that is smaller than the slop allowed by the SpanNearQuery.
     * @return true iff there is such a match.
     */
    private boolean advance() throws IOException {
	log.trace("advance");
	boolean match = false;

	// There are more spans, and both spans are either in the
	// same doc or can be forwarded to the same doc.
	while (more && (inSameDoc || toSameDoc())) {

	    log.trace("More spans in the same Doc: {}", firstSpansByDoc.doc());
	    
	    /* spans are in the same doc and in the correct order next to each other */
	    if (match()) {

		// start and end position of last span
		matchStart = firstSpans.start();
		matchEnd = secondSpans.end();

		log.trace("Matching: {}-{}", matchStart, matchEnd);

		log.trace("Check for payloads");


		//		if (collectPayloads) {
		    log.trace("copy payloads");

		    if (firstSpans.isPayloadAvailable()) {
			Collection<byte[]> payload = firstSpans.getPayload();
			log.trace("Found {} payloads in firstSpans", payload.size());
			matchPayload.addAll(payload);
		    };
		    if (secondSpans.isPayloadAvailable()) {
			Collection<byte[]> payload = secondSpans.getPayload();
			log.trace("Found {} payloads in secondSpans", payload.size());
			matchPayload.addAll(payload);
		    };
		    //		};

		log.trace("=> MATCH");
		match = true;
		break;
	    };
	};

	log.trace("Forward secondSpans");
	if (!secondSpans.next()) {
	    log.trace("No more secondSpans");
	    more = false;
	};
	inSameDoc = false;
	return match;
    };


    /** Returns the document number of the current match.  Initially invalid. */
    @Override
    public int doc () {
	return matchDoc;
    };

    /** Returns the start position of the current match.  Initially invalid. */
    @Override
    public int start () {
	return matchStart;
    };

    /** Returns the end position of the current match.  Initially invalid. */
    @Override
    public int end () {
	return matchEnd;
    };

    /**
     * Returns the payload data for the current span.
     * This is invalid until {@link #next()} is called for
     * the first time.
     * This method must not be called more than once after each call
     * of {@link #next()}. However, most payloads are loaded lazily,
     * so if the payload data for the current position is not needed,
     * this method may not be called at all for performance reasons. An ordered
     * SpanQuery does not lazy load, so if you have payloads in your index and
     * you do not want ordered SpanNearQuerys to collect payloads, you can
     * disable collection with a constructor option.<br>
     * <br>
     * Note that the return type is a collection, thus the ordering should not be relied upon.
     * <br/>
     * @lucene.experimental
     *
     * @return a List of byte arrays containing the data of this payload, otherwise null if isPayloadAvailable is false
     * @throws IOException if there is a low-level I/O error
     */
    // public abstract Collection<byte[]> getPayload() throws IOException;
    @Override
    public Collection<byte[]> getPayload() throws IOException {
	log.trace("Payload is requested with payload count {}", matchPayload.size());
	return matchPayload;
    };
    

    /**
     * Checks if a payload can be loaded at this position.
     * <p/>
     * Payloads can only be loaded once per call to
     * {@link #next()}.
     *
     * @return true if there is a payload available at this position that can be loaded
     */
    @Override
    public boolean isPayloadAvailable() {
	log.trace("Check for payload emptyness: {}", matchPayload.isEmpty());

	return matchPayload.isEmpty() == false;
    };


    // Todo: This may be in the wrong version
    @Override
    public long cost() {
	return Math.min(firstSpans.cost(), secondSpans.cost());
    };


    @Override
    public String toString() {
	return getClass().getName() + "("+query.toString()+")@"+
	    (firstTime?"START":(more?(doc()+":"+start()+"-"+end()):"END"));
    };


    public boolean match () throws IOException {
	matchDoc = firstSpans.doc();
	log.trace("Check for next match");

	byte check;
	while (inSameDoc && ((check = docNext(firstSpans, secondSpans)) != (byte) 0)) {

	    log.trace("There's no match");

	    if ((check == (byte) -1) && !secondSpans.next()) {
		log.trace("No more secondSpans");
		inSameDoc = false;
		more = false;
		break;
	    }
	    else if (check == (byte) 1 && !firstSpans.next()) {
		log.trace("No more firstSpans");
		inSameDoc = false;
		more = false;
		break;
	    }
	    else if (matchDoc != secondSpans.doc()) {
		log.trace("secondSpans has another doc");
		inSameDoc = false;
		break;
	    };
	};
	return inSameDoc;
    };


    /** Check whether two Spans in the same document are ordered.
     * @return true iff spans1 starts before spans2
     *              or the spans start at the same position,
     *              and spans1 ends before spans2.
     */
    static final byte docNext (Spans spans1, Spans spans2) {
	// check does
	int start1 = spans1.start();
	int start2 = spans2.start();

	//	boolean val = (start1 == start2) ? (spans1.end() < spans2.end()) : (start1 < start2);
	byte val;
	if (start1 >= start2) {
	    val = (byte) -1;
	}
	else {
	    int end1 = spans1.end();
	    if (end1 == start2) {
		val = (byte) 0;
	    }
	    else if (end1 > start2) {
		val = (byte) -1;
	    }
	    else {
		val = (byte) 1;
	    };
	}
	// -1: forward secondSpans
	// 1: forward firstSpans

	log.trace("{}-{} next to {}-{}", start1, spans1.end(), start2, spans2.end());
	log.trace("docSpansOrdered: {}", val);

	return val;
    };
};
