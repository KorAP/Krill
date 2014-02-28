package de.ids_mannheim.korap.query.spans;

import de.ids_mannheim.korap.query.spans.KorapTermSpan;

import org.apache.lucene.index.Term;
import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.BytesRef;

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.Collection;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;

// TODO: Store payloads in 12 byte instead of the complicated ByteBuffer stuff!

/**
 * @author Nils Diewald
 */
public class ElementSpans extends Spans {

    private byte[] payloadByte = new byte[4];
    private ByteBuffer bb = ByteBuffer.allocate(4);

    protected final DocsAndPositionsEnum postings;
    protected final Term term;
    private int doc, freq, count, position, end;
    protected boolean readPayload;

    private  LinkedList<KorapTermSpan> memory;
    private ByteBuffer storedPayload = ByteBuffer.allocate(128);
    boolean hasStoredPayload = false;

    private KorapTermSpan overflow, tempSpan;

    private final static Logger log = LoggerFactory.getLogger(ElementSpans.class);
    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;


    /**
     * The constructor.
     */
    public ElementSpans(DocsAndPositionsEnum postings, Term term) {
	this.postings = postings;
	this.term = term;
	this.doc = -1;
	this.end = -1;
	storedPayload.clear();
	hasStoredPayload = false;
	// storedPayload = null;
	memory = new LinkedList<KorapTermSpan>();
	overflow = new KorapTermSpan();
	tempSpan = new KorapTermSpan();
    };

    // only for EmptyElementSpans (below)
    public ElementSpans() {
	term = null;
	postings = null;
    };

    @Override
    public boolean next() throws IOException {
	end = -1;

	if (memory.size() > 0) {
	    if (DEBUG)
		log.trace("There is a memory entry");

	    _setToCurrent(memory.removeFirst());

	    if (DEBUG)
		log.trace("Current1: [{}-{}]", position, end);

	    return true;
	};

	if (DEBUG)
	    log.trace("There is no memory entry");

	if (count == freq) {

	    if (DEBUG)
		log.trace("last position in document");

	    // Check for overflow on document boundary
	    if (overflow.start != -1) {

		if (DEBUG)
		    log.trace("  but there is an overflow");

		_setToCurrent(overflow).clear();

		if (DEBUG)
		    log.trace("Current2: [{}-{}]", position, end);

		return true;
	    };

	    if (postings == null) {
		if (DEBUG)
		    log.trace("no more postings");
		return false;
	    };

	    if (DEBUG)
		log.trace("Go to next doc");

	    doc = postings.nextDoc();

	    if (doc == DocIdSetIterator.NO_MORE_DOCS) {
		if (DEBUG)
		    log.trace("no more docs");
		return false;
	    };

	    // New doc!
	    end = -1;
	    storedPayload.clear();
	    hasStoredPayload = false;

	    freq = postings.freq();
	    count = 0;
	};

	int pos = overflow.start;
	
	while (true) {
	    if (DEBUG) {
		log.trace("pos is {}", pos);
		_log_payloads(1);
	    };

	    if (count == freq) {
		if (DEBUG)
		    log.trace("last position in document");

		if (postings == null) {

		    if (DEBUG)
			log.trace("no more postings");

		    // Check for overflow on document boundary
		    if (overflow.start != -1) {
			if (DEBUG)
			    log.trace("  but there is an overflow");

			_setToCurrent(overflow).clear();
			if (DEBUG)
			    log.trace("Current3: [{}-{}]", position, end);

			return true;
		    };

		    return false;
		};

		if (DEBUG) {
		    log.trace("go to next doc");
		    _log_payloads(2);
		};

		if (overflow.start != -1) {
		    if (DEBUG) {
			log.trace("Storing overflow {} ...", overflow.toString());
			log.trace("... in memory with {}-{}", overflow.startChar(), overflow.endChar());
		    };
		    memory.add((KorapTermSpan) overflow.clone());
		    overflow.clear();
		};
		if (DEBUG)
		    _log_payloads(3);

		if (memory.size() > 0) {
		    if (DEBUG) {
			log.trace("sort and return first");
			_log_payloads(4);
		    };

		    Collections.sort(memory);

		    if (DEBUG)
			_log_payloads(5);

		    _setToCurrent(memory.removeFirst());

		    if (DEBUG)
			_log_payloads(6);

		    if (DEBUG)
			log.trace("Current4: [{}-{}]]", position, end);
		    break;
		};

		doc = postings.nextDoc();
		// New doc
		end = pos = -1;

		if (doc == DocIdSetIterator.NO_MORE_DOCS) {
		    if (DEBUG)
			log.trace("no more docs");
		    return false;
		};

		freq = postings.freq();
		count = 0;
	    };


	    if (DEBUG)
		log.trace("Forward postings");
	    
	    position = postings.nextPosition();
	    // New pos!
	    end = -1;

	    if (DEBUG) {
		_log_payloads(9);
		log.trace("CLEAR PAYLOAD");
	    };
	
	    storedPayload.clear();
	    hasStoredPayload = false;

	    if (DEBUG) {
		_log_payloads(10);
		log.trace("next position is {}", position);
	    };
	    
	    count++;

	    // There was no overflow
	    if (pos == -1 || pos == position) {
		if (pos == position) {
		    if (DEBUG)
			log.trace("Add overflow to memory");
		    
		    memory.add((KorapTermSpan) overflow.clone());
		}

		else {
		    if (DEBUG)
			log.trace("There was no overflow");
		    pos = position;
		};

		if (DEBUG) {
		    _log_payloads(8);
		    log.trace("*****************************");
		};
		
		_setCurrentTo(overflow);

		if (DEBUG) {
		    log.trace("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
		    log.trace("Set overflow and continue: {} ...", overflow.toString());
		    log.trace("... with {}-{}", overflow.startChar(), overflow.endChar());
		};

		continue;
	    }

	    // overflow was older
	    else if (pos != position) {

		if (DEBUG)
		    log.trace("Overflow was older");

		// Use memory
		if (memory.size() > 0) {

		    if (DEBUG)
			log.trace("Add overflow to memory");

		    memory.add((KorapTermSpan) overflow.clone());

		    if (DEBUG)
			log.trace("Sort memory");

		    // Sort by end position
		    Collections.sort(memory);

		    // Store current information in overflow
		    _setCurrentTo(overflow);

		    if (DEBUG) {
			log.trace("Set new overflow: {}", overflow.toString());
			log.trace("Get first element from sorted memory");
		    };

		    _setToCurrent(memory.removeFirst());
		}

		// Nothing in memory - use overflow!
		else {

		    if (DEBUG)
			log.trace("There is nothing in memory");

		    /* Make overflow active and store last position in overflow */
		    _setCurrentTo(tempSpan);

		    if (DEBUG)
			log.trace("Temp is now {}", overflow.toString());

		    _setToCurrent(overflow);
		    
		    // Store current information in overflow
		    overflow.copyFrom(tempSpan);

		    if (DEBUG)
			log.trace("Overflow is now {}", overflow.toString());

		};
		break;
	    };
	};

	if (DEBUG)
	    log.trace("Current4: [{}-{}]", position, end);

	readPayload = false;
	return true;
    };

    private KorapTermSpan _setToCurrent (KorapTermSpan act) {
	if (act.payload != null)
	    act.payload.rewind();

	if (DEBUG)
	    log.trace("Set to current with {}, meaning {} - {}",
		      act.toString(),
		      act.payload.getInt(0),
		      act.payload.getInt(4)
		      );
	
	if (act.payload != null)
	    act.payload.rewind();

	position = act.start;
	end = act.end;
	storedPayload.clear();
	hasStoredPayload = false;

	if (act.payload != null) {
	    if (DEBUG)
		log.trace("Payload is not null");

	    act.payload.rewind();
	    storedPayload.put(act.payload);
	    hasStoredPayload = true;
	}
	else if (DEBUG)
	    log.trace("Payload is null");

	return act;
    };

    private void _log_payloads (int nr) {
	if (!DEBUG)
	    return;
	
	if (hasStoredPayload)
	    log.trace(
		      "[{}] payload offsets are {}-{}",
		      nr,
		      storedPayload.getInt(0),
		      storedPayload.getInt(4)
		      );
	else
	    log.trace("[{}] payload is empty", nr);
    };

    private void _setCurrentTo () {
	overflow.start = position;
	overflow.end = this.end();
	overflow.payload.clear();

	if (hasStoredPayload)
	    overflow.payload.put(storedPayload);

	if (DEBUG)
	    log.trace("Set current to Overflow {} with {}-{}", overflow.toString(), overflow.startChar(), overflow.endChar());
    };

    private void _setCurrentTo (KorapTermSpan o) {

	if (DEBUG)
	    _log_payloads(7);
	
	o.start = position;
	o.end = this.end();
	o.payload.clear();
	
	if (hasStoredPayload) {
	    storedPayload.rewind();
	    o.payload.put(storedPayload);

	    if (DEBUG)
		log.trace("Object now has offset {}-{}", o.payload.getInt(0), o.payload.getInt(4));

	    // Import:
	    o.payload.rewind();
	};

	if (DEBUG)
	    log.trace("Set current to object {} ...", o.toString());
	
	if (hasStoredPayload) {
	    if (DEBUG)
		log.trace("with {}-{} from {}-{}", o.startChar(), o.endChar(), storedPayload.getInt(0), storedPayload.getInt(4));

	    storedPayload.rewind();
	};
    };


    @Override
    public boolean skipTo(int target) throws IOException {
	assert target > doc;
	doc = postings.advance(target);

	end = -1;
	overflow.clear();
	storedPayload.clear();
	hasStoredPayload = false;
	
	if (memory != null)
	    memory.clear();

	if (doc == DocIdSetIterator.NO_MORE_DOCS)
	    return false;

	freq = postings.freq();
	count = 0;
	position = postings.nextPosition();
	count++;
	readPayload = false;
	return true;
    };

    @Override
    public int doc() {
	return doc;
    };

    @Override
    public int start() {
	return position;
    };

    @Override
    public int end() {
	if (end >= 0)
	    return end;

	try {
	    end = this.getPayloadEndPosition();
	}
	catch (Exception e) {
	    end = position;
	};
	return end;
    };

    @Override
    public long cost() {
	return postings.cost();
    };

    @Override
    public Collection<byte[]> getPayload() throws IOException {
	byte[] offsetCharacters = new byte[8];

	if (storedPayload.position() <= 0)
	    this.getPayloadEndPosition();

	if (DEBUG) {
	    if (hasStoredPayload)
		log.trace("storedPayload: {} - {}",
			  storedPayload.getInt(0),
			  storedPayload.getInt(4));
	    else
		log.trace("storedPayload is empty");
	};
	
	System.arraycopy(storedPayload.array(), 0, offsetCharacters, 0, 8);

	return Collections.singletonList(offsetCharacters);
    };

    @Override
    public boolean isPayloadAvailable() throws IOException {
	return readPayload == false && postings.getPayload() != null;
    };

    @Override
    public String toString() {
	return "spans(" + term.toString() + ")@" +
            (doc == -1 ? "START" : (doc == Integer.MAX_VALUE) ? "END" : doc + "-" + position);
    };

    public DocsAndPositionsEnum getPostings() {
	return postings;
    };

    private int getPayloadEndPosition () {
	if (DEBUG)
	    log.trace("getPayloadEndPosition of element ...");

	try {
	    BytesRef payload = postings.getPayload();

	    if (DEBUG)
		log.trace("  BytesRef: {}", payload.toString());
	    
	    readPayload = true;
	    storedPayload.clear();
	    hasStoredPayload = false;

	    if (payload != null) {
		if (DEBUG)
		    log.trace("Do bit magic");
		
		storedPayload.put(payload.bytes, payload.offset, 8);
		storedPayload.put(payload.bytes, payload.offset + 12, payload.length - 12);
		System.arraycopy(payload.bytes, payload.offset + 8, payloadByte, 0, 4);
		hasStoredPayload = true;

		if (DEBUG)
		    log.trace("~~ Bytes: {}-{}-{}",
			      storedPayload.getInt(0),
			      storedPayload.getInt(4),
			      payloadByte);
	    }

	    else {
		if (DEBUG)
		    log.trace("There's no payload available");
		
		payloadByte = null;
	    };

	    if (payloadByte != null) {
		bb.clear();
		int t = bb.wrap(payloadByte).getInt();

		if (DEBUG)
		    log.trace("   |-> {}", t);
		
		return t;
	    };

	}
	catch (IOException e) {
	    if (DEBUG)
		log.trace("IOException {}", e);	   
	};
	return -1;
    };


    private static final class EmptyElementSpans extends ElementSpans {

	@Override
	public boolean next() { return false; };

	@Override
	public boolean skipTo(int target) { return false; };

	@Override
	public int doc() { return DocIdSetIterator.NO_MORE_DOCS; };
	
	@Override
	public int start() { return -1; };

	@Override
	public int end() { return -1; };

	@Override
	public Collection<byte[]> getPayload() { return null; };

	@Override
	public boolean isPayloadAvailable() { return false; };
	
	@Override
	public long cost() { return 0; };
    };
    
    public static final ElementSpans EMPTY_ELEMENT_SPANS = new EmptyElementSpans();
};
