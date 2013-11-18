package de.ids_mannheim.korap.query.spans;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.Bits;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.DocIdSetIterator;

import java.nio.ByteBuffer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import de.ids_mannheim.korap.query.SpanWithinQuery;
import de.ids_mannheim.korap.query.spans.KorapLongSpan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.io.*;

/*
<a>x<b>y<c>z[1]</c>z[2]</b>z[3]</a>

Erst:
a mit ?
-> fetch empty
-> [].next -> 1
=> match!
-> speicher [1]
a mit ?
-> fetch gecheckt! (WIE MERKE ICH MIR DAS?)
-> [].next -> 1
=> match!
-> speicher [2]
a mit [3]
=> match!
-> speicher [3]
b mit ?
-> fetch [1]
=> match!
-> wenn [1].end <= b.end
   -> speicher [1]
b mit ?
-> fetch [2]
=> match!
-> wenn [2].end <= b.end
   -> speicher [2]

Speicher: start, end, payload, wrapStart, wrapEnd

Problem: Es ist nicht garantiert, dass bei
<a><b><c>x</c>y</b>z</a>
die Wrapreihenfolge a,b,c rauskommt!
*/

public class WithinSpans extends Spans {
    private boolean firstTime = true;
    private boolean more = false;

    // Initialize as invalid
    private int matchDoc   = -1;
    private int matchStart = -1;
    private int matchEnd   = -1;

    /** Indicates that the wrap and the embedded spans are in the same doc */
    private boolean inSameDoc = false;
    private int wrapDoc;
    private int embeddedDoc;
    private int wrapStart, wrapEnd, embeddedStart, embeddedEnd;
    private Collection<byte[]> embeddedPayload;

    // Wrap span
    private final Spans wrapSpans;
    private final Spans wrapSpansByDoc; // Necessary?

    // Embedded span
    private final Spans embeddedSpans;
    private final Spans embeddedSpansByDoc;

    private SpanWithinQuery query;

    private LinkedList<KorapLongSpan> spanStore1, spanStore2;

    private List<byte[]> matchPayload;

    private short flag;

    private final Logger log = LoggerFactory.getLogger(WithinSpans.class);

    // Constructor
    public WithinSpans (SpanWithinQuery spanWithinQuery,
			AtomicReaderContext context,
			Bits acceptDocs,
			Map<Term,TermContext> termContexts,
			short flag) throws IOException {

	log.trace("Init WithinSpans");

	// Init copies
	this.matchPayload = new LinkedList<byte[]>();

	// Get span
	this.wrapSpans = spanWithinQuery.wrap().getSpans(context, acceptDocs, termContexts);
	this.wrapSpansByDoc = wrapSpans; // used in toSameDoc()

	this.embeddedSpans = spanWithinQuery.embedded().getSpans(context, acceptDocs, termContexts);
	this.embeddedSpansByDoc = embeddedSpans; // used in toSameDoc()

	this.flag = flag;

	this.spanStore1 = new LinkedList<KorapLongSpan>();
	this.spanStore2 = new LinkedList<KorapLongSpan>();
	
	this.query = spanWithinQuery; // kept for toString() only.
    };


    /** Move to the next match, returning true iff any such exists. */
    @Override
    public boolean next () throws IOException {
	log.trace("Next with doc {}", matchDoc);

	// Check for init next
	if (firstTime) {
	    firstTime = false;
	    if (!embeddedSpans.next() || !wrapSpans.next()) {
		log.trace("No next in firstSpan nor in secondSpan 1");
		more = false;
		return false;
	    };
	    log.trace("Spans are initialized");
	    more = true;
	    wrapStart = wrapSpans.start();
	    wrapEnd = wrapSpans.end();
	    wrapDoc = matchDoc = wrapSpans.doc();

	    embeddedStart = embeddedSpans.start();
	    embeddedEnd = embeddedSpans.end();
	    embeddedDoc = embeddedSpans.doc();

	    if (embeddedSpans.isPayloadAvailable()) {
		Collection<byte[]> payload = embeddedSpans.getPayload();
		embeddedPayload = new ArrayList<byte[]>(payload.size());
		embeddedPayload.addAll(payload);
	    };

	    log.trace("Init spans: {}", _actPos());

	    if (embeddedDoc == matchDoc) {
		inSameDoc = true;
		log.trace("Is now inSameDoc");
	    }
	    else {
		log.trace("Is not inSameDoc");
	    };
	    log.trace("Next with doc {} (wrap) and {} (embedded)", wrapDoc, embeddedDoc);
	};

	matchPayload.clear();
	return advanceAfterCheck();
    };


    /** Advances the subSpans to just after a within match.
     * @return true iff there is such a match.
     */
    private boolean advanceAfterCheck() throws IOException {
	log.trace("advanceAfterChecked inSameDoc: {} and more: {}", inSameDoc, more);
	log.trace("advanceAfterCheck with doc {} (wrap) and {} (embedded)", wrapDoc, embeddedDoc);

	// There are more spans, and both spans are either in the
	// same doc or can be forwarded to the same doc.
	while (more && (inSameDoc || toSameDoc())) {

	    log.trace("There are more spans in doc {}", embeddedDoc);
	    
	    /* spans are in the same doc */
	    if (within()) {
		return true;
	    }
	    else {
		log.trace("No within");
	    };
	};

	log.trace("No more matches");

	return false; // no more matches
    };


    /** Advance the subSpans to the same document */
    private boolean toSameDoc () throws IOException {
	log.trace("toSameDoc");

	/*
	wrapDoc = wrapSpansByDoc.doc();
	embeddedDoc = embeddedSpansByDoc.doc();

	*/

	if (wrapDoc != embeddedDoc) {
	    log.trace("Docs not identical: {} vs {}", wrapDoc, embeddedDoc);

	    spanStore1.clear(); // = new LinkedList<KorapLongSpan>();
	    spanStore2.clear(); // = new LinkedList<KorapLongSpan>();

	    if (wrapDoc < embeddedDoc) {
		log.trace("Skip wrap from {} to {}", wrapDoc, embeddedDoc);
		if (!wrapSpansByDoc.skipTo(embeddedDoc)) {
		    more = false;
		    inSameDoc = false;
		    return false;
		};
		wrapDoc = wrapSpans.doc();
	    }
	    else if (wrapDoc > embeddedDoc) {
		log.trace("Skip embedded from {} to {}", embeddedSpans.doc(), wrapDoc);
		//		if (!embeddedSpansByDoc.skipTo( wrapDoc )) {
		if (wrapDoc != embeddedSpans.doc()) {
		    if (embeddedSpans.doc() == DocIdSetIterator.NO_MORE_DOCS || !embeddedSpans.skipTo( wrapDoc )) {
			more = false;
			inSameDoc = false;
			return false;
		    };
		}
		else {
		    _add_current();
		    //		    embeddedDoc = embeddedSpans.doc();
		};
	    };
	}
	else {
	    log.trace("Docs identical");
	};
	embeddedStart = embeddedSpans.start();
	embeddedEnd = embeddedSpans.end();
	log.trace("The new embedded start is {}-{}", embeddedStart, embeddedEnd);
	inSameDoc = true;
	return true;
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
	    if (!embeddedSpans.next() || !wrapSpans.next()) {
		log.trace("No next in firstSpan nor in secondSpan 2");
		more = false;
		return false;
	    };
	    more = true;
	    wrapStart = wrapSpans.start();
	    wrapEnd = wrapSpans.end();
	    wrapDoc = embeddedSpans.doc();
	    embeddedStart = embeddedSpans.start();
	    embeddedEnd = embeddedSpans.end();
	    embeddedDoc = embeddedSpans.doc();
	}

	/*
	  See NextSpans for the same problem!
	  This should be dealt with in toSameDoc!!!
	 */
	else if (more && (embeddedSpans.doc() < target)) {
	    if (embeddedSpans.skipTo(target)) {
		inSameDoc = false;
		embeddedDoc = target;
	    }

	    // Can't be skipped to target
	    else {
		more = false;
		return false;
	    };
	};

	matchPayload.clear();
	return advanceAfterCheck();
    };

    private String _actPos () {
	StringBuilder sb = new StringBuilder();
	sb.append("<").append(wrapStart).append('-').append(wrapEnd).append('>');
	sb.append(embeddedStart).append('-').append(embeddedEnd);
	sb.append("</>");
	return sb.toString();
    };


    private boolean within () throws IOException {
	log.trace("within");
	
	while (more && inSameDoc) {

	    // Case 1-5
	    // Case 1
	    //     |---|
	    // |-|
	    // Case 2
	    //   |---|
	    // |-|
	    // Case 3
	    //   |---|
	    // |---|
	    // Case 4
	    //   |-|
	    // |---|
	    // Case 5
	    //  |-|"
	    // |---|
	    if (wrapStart > embeddedStart) {
		log.trace("[Case] 1-5 with {}", _actPos());

		if (this.fetchNext()) {
		    continue;
		};

		// Forward wrapSpan
		if (wrapSpans.next()) {
		    wrapDoc = wrapSpans.doc();
		    if (this.toSameDoc()) {
			wrapStart = wrapSpans.start();
			wrapEnd = wrapSpans.end();
			continue;
		    };
		};

		this.more = false;
		this.inSameDoc = false;
		return false;
	    };

	    // Get wrapEnd
	    // wrapEnd = wrapSpans.end();

	    KorapLongSpan embedded = new KorapLongSpan();
	    embedded.start = embeddedStart;
	    embedded.end = embeddedEnd;
	    embedded.doc = embeddedDoc;
	    if (embeddedPayload != null)
		embedded.payload = embeddedPayload;

	    this.spanStore1.add(embedded);
	    log.trace("pushed to spanStore1: {}", embedded.toString());


	    // Case 12
	    // |---|
	    //     |-|
	    // Case 13
	    // |---|
	    //       |-|
	    if (wrapEnd <= embeddedStart) {
		log.trace("[Case] 12-13 with {}", _actPos());

		// Copy content of spanStores
		if (!spanStore1.isEmpty()) {
		    log.trace("First store is not empty - copy to second store!");
		    spanStore2.addAll(0, (LinkedList<KorapLongSpan>) spanStore1.clone());
		    spanStore1.clear();
		    log.trace("Second store is now: {}", spanStore2.toString());
		};

		// Forward wrapSpan
		log.trace("Try to forward wrapspan");

		if (wrapSpans.next()) {
		    wrapDoc = wrapSpans.doc();
		    log.trace("wrapDoc is now {} while embeddedDoc is {}", wrapDoc, embeddedDoc);
		    if (this.toSameDoc()) {
			wrapStart = wrapSpans.start();
			wrapEnd = wrapSpans.end();
			if (fetchTwoNext())
			    continue;
		    };
		}
		else {
		    log.trace("Unable to forward wrapspan");
		};

		this.inSameDoc = false;
		this.more = false;
		return false;
	    }


	    // Case 6 - 8
	    else if (wrapStart == embeddedStart) {

		// Case 6
		// |---|
		// |-|
		if (wrapEnd > embeddedEnd) {
		    log.trace("[Case] 6 with {}", _actPos());

		    // neither match nor endWith
		    if (this.flag < (short) 2) {
			_setMatch(embedded);
			log.trace("MATCH1!! with {}", _actPos());
			fetchTwoNext();
			return true;
		    };

		    fetchTwoNext();
		    continue;
		}

		// Case 7
		// |---|
		// |---|
		else if (wrapEnd == embeddedEnd) {
		    log.trace("[Case] 7 with {}", _actPos());

		    _setMatch(embedded);
		    log.trace("MATCH2!! with {}", _actPos());
		    fetchTwoNext();
		    return true;
		};

		// Case 8
		// |-|
		// |---|
		// wrapEnd < embeddedEnd
		log.trace("[Case] 8 with {}", _actPos());
		fetchTwoNext();
		continue;
	    };

	    // Case 9-11
	    // wrapStart < wrapEnd

	    // Case 9
	    // |---|
	    //  |-|
	    if (wrapEnd > embeddedEnd) {
		log.trace("[Case] 9 with {}", _actPos());

		// neither match nor endWith
		if (this.flag == (short) 0) {
		    _setMatch(embedded);
		    log.trace("MATCH3!! with {}", _actPos());
		    fetchTwoNext();
		    return true;
		};

		fetchTwoNext();
		continue;
	    }
	    // Case 10
	    // |---|
	    //   |-|
	    else if (wrapEnd == embeddedEnd) {
		log.trace("[Case] 10 with {}", _actPos());

		// neither match nor endWith
		if (this.flag == (short) 0 || this.flag == (short) 2) {
		    _setMatch(embedded);
		    log.trace("MATCH4!! with {}", _actPos());
		    fetchTwoNext();
		    return true;
		};

		fetchTwoNext();
		continue;
	    };

	    // Case 11
	    // |---|
	    //   |---|
	    // wrapEnd < embeddedEnd
	    log.trace("[Case] 11 with {}", _actPos());
	    fetchTwoNext();
	    continue;
	};

	this.more = false;
	return false;
    };


    private boolean fetchNext () throws IOException {

	// Fetch span from first store
	if (spanStore1.isEmpty()) {
	    log.trace("First store is empty");
	    return fetchTwoNext();
	};

	KorapLongSpan current = spanStore1.removeFirst();
	log.trace("Fetch from first store: {}", current.toString());

	embeddedStart = current.start;
	embeddedEnd = current.end;
	embeddedDoc = current.doc;
	if (current.payload != null)
	    embeddedPayload = current.payload;

	return true;
    };


    private boolean fetchTwoNext () throws IOException {

	// Fetch span from second store
	if (spanStore2.isEmpty()) {
	    log.trace("Second store is empty");

	    // Forward spans
	    if (this.embeddedSpans.next()) {
		log.trace("Forwarded embeddedSpans");

		if (this.embeddedSpans.doc() != wrapDoc && !spanStore1.isEmpty()) {

		    log.trace("No docmatch and still stuff in store");
		    log.trace("First store is not empty - copy to second store!");
		    spanStore2.addAll(0, (LinkedList<KorapLongSpan>) spanStore1.clone());
		    spanStore1.clear();

		    _add_current();

		    log.trace("Second store is now: {}", spanStore2.toString());
		}
		else {
		    embeddedStart = embeddedSpans.start();
		    embeddedEnd = embeddedSpans.end();
		    embeddedDoc = embeddedSpans.doc();

		    if (embeddedSpans.isPayloadAvailable()) {
			Collection<byte[]> payload = embeddedSpans.getPayload();
			// Maybe just clear
			embeddedPayload = new ArrayList<byte[]>(payload.size());
			embeddedPayload.addAll(payload);
		    };

		    return this.toSameDoc();
		};
	    }
	    else {
		log.trace("Forwarded embeddedSpans failed");
	    };

	    log.trace("EmbeddedDoc: " + embeddedDoc);

	    // Forward wrapSpan
	    log.trace("Try to forward wrapspan");
	    if (wrapSpans.next()) {
		wrapDoc = wrapSpans.doc();
		if (this.toSameDoc()) {	    
		    wrapStart = wrapSpans.start();
		    wrapEnd = wrapSpans.end();

		    log.trace("WrapSpan forwarded");

		    // Copy content of spanStores
		    if (!spanStore1.isEmpty()) {
			log.trace("First store is not empty - copy to second store!");
			spanStore2.addAll(0, (LinkedList<KorapLongSpan>) spanStore1.clone());
			spanStore1.clear();
			log.trace("Second store is now: {}", spanStore2.toString());
		    };

		    return this.fetchTwoNext();
		};
	    };

	    // Don't know.
	    log.trace("No more fetchNext()");

	    more = false;
	    return false;
	};

	KorapLongSpan current = spanStore2.removeFirst();
	log.trace("Fetch from second store: {}", current.toString());

	embeddedStart = current.start;
	embeddedEnd = current.end;
	embeddedDoc = current.doc;
	embeddedPayload = current.payload;

	return true;
    };


    /*
TODO: Maybe ignore "embedded" parameter and use embeddedPayload directly
     */
    private void _setMatch (KorapLongSpan embedded) throws IOException {
	matchStart = wrapStart;
	matchEnd = wrapEnd;
	matchDoc = embeddedDoc;
	matchPayload.clear();

	if (embedded.payload != null)
	    matchPayload.addAll(embedded.payload);

	if (wrapSpans.isPayloadAvailable()) {
	    Collection<byte[]> payload = wrapSpans.getPayload();
	    matchPayload.addAll(payload);
	};
    };


    private void _add_current () throws IOException{
	KorapLongSpan embedded = new KorapLongSpan();
	embedded.start = embeddedSpans.start();
	embedded.end = embeddedSpans.end();
	embedded.doc = embeddedSpans.doc();

	if (embeddedSpans.isPayloadAvailable()) {
	    Collection<byte[]> payload = embeddedSpans.getPayload();
	    embedded.payload = new ArrayList<byte[]>(payload.size());
	    embedded.payload.addAll(payload);
	};

	this.spanStore2.add(embedded);	    
	log.trace("pushed to spanStore2: {}", embedded.toString());  
    };


    /** Returns the document number of the current match.  Initially invalid. */
    @Override
    public int doc () {
	return matchDoc;
    };

    /** Returns the start position of the embedding wrap.  Initially invalid. */
    @Override
    public int start () {
	return matchStart;
    };

    /** Returns the end position of the embedding wrap.  Initially invalid. */
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
	return matchPayload.isEmpty() == false;
    };

    // Todo: This may be in the wrong version
    @Override
    public long cost() {
	return Math.min(wrapSpans.cost(), embeddedSpans.cost());
    };

    @Override
    public String toString() {
	return getClass().getName() + "("+query.toString()+")@"+
	    (firstTime?"START":(more?(doc()+":"+start()+"-"+end()):"END"));
    };
};
