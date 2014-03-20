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
// Todo: Use copyFrom() instead of clone()

/**
 * @author Nils Diewald
 *
 * Use copyFrom instead of clone
 */
public class ElementSpans extends Spans {

    private byte[] payloadByte;
    private ByteBuffer bb = ByteBuffer.allocate(4);

    protected final DocsAndPositionsEnum postings;
    protected final Term term;
    private int freq = 0, count = 0;
    
    private LinkedList<KorapTermSpan> memory;
    private KorapTermSpan overflow, current, temp;
    
    public static final ElementSpans EMPTY_ELEMENT_SPANS
	= new EmptyElementSpans();

    private final static Logger log = LoggerFactory.getLogger(ElementSpans.class);
    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;


    /**
     * The constructor.
     */
    public ElementSpans(DocsAndPositionsEnum postings, Term term) {
	this.postings = postings;
	this.term = term;

	// storedPayload = null;
	this.memory   = new LinkedList<KorapTermSpan>();

	// Overflow span
	this.overflow = new KorapTermSpan();

	// Current span
	this.current = new KorapTermSpan();

    	// Temporary span
	this.temp = new KorapTermSpan();
    };
    

    // only for EmptyElementSpans (below)
    public ElementSpans() {
	this.term = null;
	this.postings = null;
    };

    @Override
    public boolean next() throws IOException {
	
	// There is a memory
	if (this.memory.size() > 0) {
	    this.setToCurrent(memory.removeFirst(), 1);

	    if (DEBUG)
		log.trace(" --- MATCH --- Fetch from memory {}",
			  this.current.toString());
	    
	    return true;
	};

	// Last element in document is reached
	if (this.count == this.freq) {

	    if (this.postings == null)
		return false;


	    // There is an overflow
	    if (this.overflow.doc != -1) {
		if (DEBUG)
		    log.trace("Fetch from overflow");

		this.setToCurrent(this.overflow, 2);

		// Reset overflow
		this.overflow.reset();

		if (DEBUG)
		    log.trace(" --- MATCH --- Fetch from memory {}",
			      this.current.toString());
	       
		return true;
	    };

	    // There is no next document
	    if (!this.nextDoc())
		return false;
	};

	// overflow is not empty - let's treat this as current
	if (this.overflow.doc != -1) {

	    if (DEBUG)
		log.trace("Overflow is not empty");
	    
	    this.setToCurrent(this.overflow, 3);

	    // TODO: newOverflow() ???
	    this.overflow.reset();
	}
	else {
	    if (DEBUG)
		log.trace("Overflow is empty");

	    // Get next posting - count is still < freq
	    this.setToCurrent(4);

	    if (this.count == this.freq) {
		if (DEBUG)
		    log.trace(" --- MATCH --- Direct {}",
			      this.current.toString());
		return true;
	    };
	};

	while (this.count < this.freq) {

	    // Temp is now the old current
	    this.setCurrentToTemp();

	    // Get new current
	    this.setToCurrent(5);

	    if (DEBUG)
		log.trace("Compare {} with {}",
			  this.current.toString(),
			  this.temp.toString());

	    // The next span is not at the same position
	    if (this.current.start != this.temp.start) {

		// Add this to memory
		if (this.memory.size() > 0) {
		    if (DEBUG)
			log.trace("[1] Add to memory {}", this.temp.toString());
		    this.memory.add((KorapTermSpan) this.temp.clone());
		    this.overflow = this.current;
		    break;
		};

		// There is no reason to start a memory
		this.overflow = this.current;
		this.current = this.temp;

		if (DEBUG)
		    log.trace(" --- MATCH --- Fetch from memory {}",
			      this.current.toString());

		return true;
	    }

	    // The positions are equal
	    else {
		if (DEBUG)
		    log.trace("[2] Add to memory {}", this.temp.toString());
		this.memory.add((KorapTermSpan) this.temp.clone());
	    };
	};

	if (this.temp.doc == this.current.doc &&
	    this.temp.start == this.current.start) {
	    if (DEBUG)
		log.trace("[3] Add to memory {}", this.current.toString());
	    this.memory.add((KorapTermSpan) this.current.clone());
	};

	// Sort the memory
	Collections.sort(memory);

	// There is now a memory
	return this.next();
    };
    

    // get next doc
    private boolean nextDoc () throws IOException {

	// Check if this doc is the last
	if (this.current.doc == DocIdSetIterator.NO_MORE_DOCS)
	    return false;

	if (DEBUG)
	    log.trace("Go to next document");

	this.current.reset();

	// Advance to next doc
	this.current.doc = this.postings.nextDoc();

	// Check if this doc is the last
	if (this.current.doc == DocIdSetIterator.NO_MORE_DOCS)
	    return false;
	
	// check frequencies
	this.freq = this.postings.freq();

	if (DEBUG)
	    log.trace("Document <{}> has {} occurrences",
		      this.current.doc,
		      this.freq);


	this.count = 0;
	return true;
    };

    
    @Override
    public boolean skipTo(int target) throws IOException {

	assert target > this.current.doc;

	// Get this doc
	this.current.doc = postings.advance(target);

	if (this.current.doc == DocIdSetIterator.NO_MORE_DOCS)
	    return false;

	if (this.memory != null)
	    this.memory.clear();

	this.overflow.reset();
	

	this.freq = this.postings.freq();

	if (DEBUG)
	    log.trace("Document {} has {} occurrences", this.current.doc, this.freq);

	
	this.count = 0;

	if (this.next())
	    return true;

	return false;
    };

    
    @Override
    public int doc() {
	return this.current.doc;
    };

    
    @Override
    public int start() {
	return this.current.start;
    };

    
    @Override
    public int end() {
	if (this.current.end >= 0)
	    return this.current.end;

	try {
	    this.current.end = this.getPayloadEndPosition();
	}
	catch (Exception e) {
	    this.current.end = this.current.start;
	};
	return this.current.end;
    };

    
    @Override
    public long cost() {
	// ???
	return this.postings.cost();
    };

    
    @Override
    public Collection<byte[]> getPayload() throws IOException {
	byte[] offsetCharacters = new byte[8];
	if (this.current.end <= 0)
	    this.getPayloadEndPosition();

	System.arraycopy(this.current.payload.array(), 0, offsetCharacters, 0, 8);

	return Collections.singletonList(offsetCharacters);
    };


    /**
     * Sets KorapTermSpan to current element
     */
    private void setToCurrent (KorapTermSpan act, int debugNumber) {

	if (DEBUG)
	    log.trace(
		"[{}] Set to current with {}",
		debugNumber,
		act.toString()
	    );

	this.current = (KorapTermSpan) act.clone();
    };

    /**
     * Sets KorapTermSpan to current element
     */
    private void setToCurrent (int debugNumber) throws IOException {
	
	this.current.start = this.postings.nextPosition();

	// This will directly save stored payloads
	this.current.end = this.getPayloadEndPosition();

	if (DEBUG)
	    log.trace(
		"[{}] Set new to current with {}",
		debugNumber,
		this.current.toString()
	    );

	this.count++;
    };

    private void setCurrentToTemp () {
	this.temp = (KorapTermSpan) this.current.clone();
	// this.temp.copyFrom(this.current);
    };


    private int getPayloadEndPosition () {
	try {
	    BytesRef payload = postings.getPayload();

	    this.current.clearPayload();
	    
	    if (payload != null) {

		this.payloadByte = new byte[4];

		// Copy some payloads like start character and end character
		this.current.payload.put(payload.bytes, payload.offset, 8);
		this.current.payload.put(payload.bytes, payload.offset + 12, payload.length - 12);

		// Copy end position integer to payloadByte
		System.arraycopy(payload.bytes, payload.offset + 8, this.payloadByte, 0, 4);
	    }

	    else {	
		this.payloadByte = null;
	    };

	    // Todo: REWRITE!
	    if (this.payloadByte != null) {

		// Todo: This is weird!
		
		bb.clear();
		int t = bb.wrap(payloadByte).getInt();


		if (DEBUG)
		    log.trace("Get Endposition and payload: {}-{} with end position {} in doc {}",
			      this.current.payload.getInt(0),
			      this.current.payload.getInt(4),
			      t,
			      this.current.doc);
		
		return t;
	    }
	    else if (DEBUG) {
		log.trace("Get Endposition and payload: None found");
	    };
	}
	catch (IOException e) {
	    if (DEBUG)
		log.trace("IOException {}", e);	   
	};
	
	return -1;
    };


    @Override
    public boolean isPayloadAvailable() throws IOException {

	if (current.payload != null)
	    return true;
	
	return false;
    };

    
    @Override
    public String toString() {
	return "spans(" + this.term.toString() + ")@" +
            (this.current.doc == -1 ? "START" : (this.current.doc == Integer.MAX_VALUE) ? "END" : this.current.doc + "-" + this.current.start);
    };

    public DocsAndPositionsEnum getPostings() {
	return postings;
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
};
