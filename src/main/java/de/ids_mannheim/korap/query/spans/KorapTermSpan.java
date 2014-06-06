package de.ids_mannheim.korap.query.spans;

import java.nio.ByteBuffer;
import de.ids_mannheim.korap.query.spans.KorapSpan;

// TODO: Store payloads in 12 byte instead of the complicated ByteBuffer stuff!


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KorapTermSpan extends KorapSpan {

    public ByteBuffer payload;
    public boolean isPayloadRead = false;
    
    private final Logger log = LoggerFactory.getLogger(ElementSpans.class);
    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;

    @Override
    public Object clone() {
	KorapTermSpan span = new KorapTermSpan();
	span.start = this.start;
	span.end   = this.end;
	span.doc   = this.doc;
	span.isPayloadRead = this.isPayloadRead;
	span.elementRef = this.elementRef;

	if (this.payload != null) {
	    this.payload.rewind();
	    span.payload.put(this.payload);

	    if (DEBUG) {
		log.trace("[TS] Clone payload {} to payload {} ...",
			  this.payload.toString(),
			  span.payload.toString());
		log.trace("[TS] ... from {}-{} to {}-{}",
			  this.startChar(),
			  this.endChar(),
			  span.startChar(),
			  span.endChar());
	    };
	};

	return span;
    };

    public KorapSpan copyFrom (KorapTermSpan o) {
	this.start = o.start;
	this.end = o.end;
	this.doc = o.doc;
	this.payload.rewind();
	this.payload.put(o.payload.array());
	return this;
    };

    public KorapSpan shallowCopyFrom (KorapTermSpan o) {
	this.start = o.start;
	this.end = o.end;
	this.doc = o.doc;
	this.payload = o.payload;
	return this;
    };


    @Override
    public void clearPayload () {
	if (this.payload != null) {
	    this.payload.clear();
	    // this.payload.rewind();
	};
    };

    @Override
    public void initPayload () {
	this.payload = ByteBuffer.allocate(128);
    };

    @Override
    public String toString () {
	StringBuilder sb = new StringBuilder("[");
	return sb.append(this.start).append('-')
	    .append(this.end)
	    .append("#")
	    .append(this.startChar()).append('-').append(this.endChar())
	    .append('(').append(this.doc).append(')')
	    .append('$').append(this.payload.toString())
	    .append(']')
	    .toString();
    };

    public int startChar () {
	return this.payload.getInt(0);
    };

    public int endChar () {
	return this.payload.getInt(4);
    };
    
    public short elementRef(){
    	return this.payload.getShort(8);
    }
    
    public void reset () {
	this.clearPayload();
	this.start = -1;
	this.end = -1;
	this.doc = -1;
	this.isPayloadRead = false;
    };
};
