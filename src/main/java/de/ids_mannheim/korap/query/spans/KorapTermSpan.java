package de.ids_mannheim.korap.query.spans;

import java.nio.ByteBuffer;
import de.ids_mannheim.korap.query.spans.KorapSpan;

// TODO: Store payloads in 12 byte instead of the complicated ByteBuffer stuff!


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KorapTermSpan extends KorapSpan {
    public ByteBuffer payload;

    private final Logger log = LoggerFactory.getLogger(WithinSpans.class);

    @Override
    public Object clone() {
	KorapTermSpan span = new KorapTermSpan();
	span.start = this.start;
	span.end = this.end;
	span.doc = this.doc;

	this.payload.rewind();
	span.payload.put(this.payload);

	log.trace("Clone payload {} to payload {} ...",
		  this.payload.toString(),
		  span.payload.toString());
	log.trace("... from {}-{} to {}-{}",
		  this.startChar(),
		  this.endChar(),
		  span.startChar(),
		  span.endChar());

	return span;
    };

    public KorapSpan copyFrom (KorapTermSpan o) {
	super.copyFrom((KorapSpan) o);
	this.payload.put(o.payload);
	return this;
    };

    @Override
    public void clearPayload () {
	if (this.payload != null) {
	    this.payload.clear();
	    this.payload.rewind();
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
};
