package de.ids_mannheim.korap.analysis;

import static de.ids_mannheim.korap.util.KorapArray.*;
import org.apache.lucene.util.BytesRef;
import java.nio.ByteBuffer;
import java.util.*;


/**
 * @author Nils Diewald
 * @version 0.2
 *
 * MultiTerm represents a term in a MultiTermToken.
 */
public class MultiTerm {
    public int start, end = 0;
    public String term = null;
    public Integer posIncr = 1;
    public boolean storeOffsets = false;
    public BytesRef payload = null;

    /**
     * The constructor.
     *
     * @param term The term surface.
              Offsets can be written as an appended and dash separated pair of integers,
	      payloads can be written following a dollar sign.
	      payloads can be typed as being a short (s), an integer (i), or a long (l)
	      in leading angular brackets. All other payloads are treated as being UTF-8
	      characer sequences.
	      
	      Examples:
	      MultiTerm test = new MultiTerm("test");
	      MultiTerm test = new MultiTerm("test#0-4");
	      MultiTerm test = new MultiTerm("test#0-4$Example");
	      MultiTerm test = new MultiTerm("test#0-4$&lt;i&gt;1278");
     */
    public MultiTerm (String term) {
	/*
	this.start = this.end = 0;
	this.storeOffsets = false;
	this.payload = null;
	*/
	_fromString(term);
    };

    /**
     * The constructor with a separated prefix.
     * new MultiTerm('a', "bcd") is equivalent to
     * new MultiTerm("a:bcd");
     *
     * @param prefix A special prefix for the term.
     * @param term The term surface.
     *
     * @see #MultiTerm(String)
     */
    public MultiTerm (char prefix, String term) {
	StringBuilder sb = new StringBuilder();
	/*
	this.start = this.end = 0;
	this.storeOffsets = false;
	this.payload = null;
	*/
	sb.append(prefix).append(':').append(term);
	_fromString(sb.toString());
    };

    public void term (String term) {
	this.term = term;
    };

    public String term () {
	return this.term;
    };

    /**
     * The constructor.
     */
    public MultiTerm () {
	this.term = "";
	/*
	this.start = this.end = 0;
	this.storeOffsets = false;
	this.payload = null;
	*/
    };

    public void payload (Byte pl) {
	this.payload = new BytesRef( ByteBuffer.allocate(1).put(pl).array());
    };

    public void payload (short pl) {
	this.payload = new BytesRef( ByteBuffer.allocate(2).putShort(pl).array());
    };

    public void payload (int pl) {
	this.payload = new BytesRef( ByteBuffer.allocate(4).putInt(pl).array());
    };

    public void payload (long pl) {
	this.payload = new BytesRef( ByteBuffer.allocate(8).putLong(pl).array());
    };

    public void payload (String pl) {
	this.payload = new BytesRef(pl);
    };

    public void payload (byte[] pl) {
	this.payload = new BytesRef(pl);
    };

    public void payload (BytesRef pl) {
	this.payload = pl;
    };

    public BytesRef payload () {
	return this.payload;
    };

    public void start (int value) {
	this.start = value;
    };

    public int start () {
	return this.start;
    };

    public void end (int value) {
	this.end = value;
    };

    public int end () {
	return this.end;
    };

    public boolean storeOffsets () {
	return this.storeOffsets;
    };

    public void storeOffsets (boolean value) {
	this.storeOffsets = value;
    };

    private void _fromString (String term) {
	String[] termSurface = term.split("\\$", 2);

	// Payload is given
	if (termSurface.length == 2) {
	    String payloadStr = termSurface[1];

	    // Payload has a type
	    if (payloadStr.charAt(0) == '<' && payloadStr.charAt(2) == '>') {
		ByteBuffer bb = ByteBuffer.allocate(8);

		String[] pls = payloadStr.split("(?=<)|(?<=>)");
		int l = 0;

		for (int i = 1; i < pls.length;) {

		    // Resize the buffer
		    if ((bb.capacity() - l) < 8) {
			bb = ByteBuffer.allocate(bb.capacity() + 8).put(bb.array());
			bb.position(l);
		    };
		    switch (pls[i]) {
		    case "<b>": // byte
			bb.put(Byte.parseByte(pls[i+1]));
			l++;
			break;
		    case "<s>":
			bb.putShort(Short.parseShort(pls[i+1]));
			l+=2;
			break;
		    case "<i>":
			bb.putInt(Integer.parseInt(pls[i+1]));
			l+=4;
			break;
		    case "<l>":
			bb.putLong(Long.parseLong(pls[i+1]));
			l+=8;
			break;
		    };
		    i+=2;
		};
		byte[] bytes = new byte[l];
		System.arraycopy(bb.array(), 0, bytes, 0, l);
		this.payload = new BytesRef(bytes);


		/*
		payloadStr = payloadStr.substring(3, payloadStr.length());
		switch (type) {
		case 'b':  // byte

		    System.err.println("bbb");
		    payloadBytes = ByteBuffer.allocate(1).put(new Byte(payloadStr)).array();
		    break;
		case 's':  // short
		    payloadBytes = ByteBuffer.allocate(2).putShort(
								   Short.parseShort(payloadStr)
								   ).array();
		    break;
		case 'i': // integer
		    payloadBytes = ByteBuffer.allocate(4).putInt(
								 Integer.parseInt(payloadStr)
								 ).array();
		    break;
		case 'l': // long
		    payloadBytes = ByteBuffer.allocate(8).putLong(
								  Long.parseLong(payloadStr)
								  ).array();
		    break;
		};
		TODO:
		case '?': // arbitrary
		    payloadStr = 
		*/
	    }

	    // Payload is a string
	    else {
		this.payload = new BytesRef(payloadStr);
	    };
	};
	String[] stringOffset = termSurface[0].split("\\#", 2);
	if (stringOffset.length == 2) {
	    String[] offset = stringOffset[1].split("\\-", 2);

	    if (offset.length == 2 && offset[0].length() > 0) {
		this.start = Integer.parseInt(offset[0]);
		this.end   = Integer.parseInt(offset[1]);
	    /*
	    }
	    else {
		this.storeOffsets(false);
	    */
	    };
	};
	this.term = stringOffset[0];
    };


    /**
     * Represent the MultiTerm as a string.
     * Offsets are attached following a hash sign,
     * payloads are attached following a dollar sign.
     * All payloads are written as UTF-8 character sequences.
     *
     * @see #toStringShort().
     */
    public String toString () {
	StringBuilder sb = new StringBuilder(this.term);
	if (this.start != this.end) {
	    sb.append('#').append(this.start).append('-').append(this.end);
	/*
	}
	else if (!this.storeOffsets()) {
	    sb.append("#-");
	*/
	};

	if (this.payload != null) {
	    sb.append('$');
	    try {
		sb.append(this.payload.utf8ToString());
	    }
	    catch (AssertionError e) {
		sb.append("<?>").append(join(',', this.payload.toString().split(" ")));
	    };
	};

	return sb.toString();
    };

    /**
     * Represent the MultiTerm as a string.
     * Payloads are attached following a dollar sign.
     * All payloads are written as UTF-8 character sequences.
     * Offsets are neglected.
     * 
     * @see #toString().
     */
    public String toStringShort () {
	StringBuilder sb = new StringBuilder(this.term);
	if (this.payload != null) {
	    sb.append('$').append(this.payload.utf8ToString());
	};
	return sb.toString();
    };
};
