package de.ids_mannheim.korap.query.spans;

import de.ids_mannheim.korap.query.spans.KorapSpan;

import java.util.Collection;

public class KorapLongSpan extends KorapSpan {
    public Collection<byte[]> payload;
    
    @Override
    public Object clone() {
	KorapLongSpan span = new KorapLongSpan();
	span.start = this.start;
	span.end = this.end;
	span.doc = this.doc;
	span.payload.addAll(this.payload);
	return span;
    };

    public KorapSpan copyFrom (KorapLongSpan o) {
	super.copyFrom((KorapSpan) o);
	this.payload.addAll(o.payload);
	return this;
    };

    @Override
    public void clearPayload () {
	if (this.payload != null)
	    this.payload.clear();
    };

    @Override
    public void initPayload () {
    };

    @Override
    public String toString () {
	StringBuilder sb = new StringBuilder("[");
	return sb.append(this.start).append('-')
	    .append(this.end)
	    .append('(').append(this.doc).append(')')
	    .append(']')
	    .toString();
    };

};
