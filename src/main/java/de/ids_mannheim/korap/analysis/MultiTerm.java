package de.ids_mannheim.korap.analysis;

import static de.ids_mannheim.korap.util.KorapArray.*;
import org.apache.lucene.util.BytesRef;
import java.nio.ByteBuffer;
import java.util.*;


/**
 * @author Nils Diewald
 * @version 0.3
 *
 * MultiTerm represents a term in a MultiTermToken.
 */
public class MultiTerm {
    public int start, end = 0;
    public String term = null;
    public Integer posIncr = 1;
    public boolean storeOffsets = false;
    public BytesRef payload = null;

    private static ByteBuffer bb = ByteBuffer.allocate(8);
    private static String[] stringOffset;

    private static short i, l;


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

	      Strings that are malformed fail silently.
     */
    public MultiTerm (String term) {
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
	_fromString(sb.append(prefix).append(':').append(term).toString());
    };
    
    /**
     * The empty constructor.
     */
    public MultiTerm () {
	this.term = "";
    };

    
    /**
     * Sets the term value.
     *
     * @param term The term as a string
     */
    public void setTerm (String term) {
	this.term = term;
    };


    /**
     * Returns the term value.
     *
     * @return The term value.
     */
    public String getTerm () {
	return this.term;
    };

    
    /**
     * Set the payload as a byte value.
     *
     * @param pl The payload.
     */
    public void setPayload (Byte pl) {
	this.payload = new BytesRef( ByteBuffer.allocate(1).put(pl).array());
    };

    
    /**
     * Set the payload as a short value.
     *
     * @param pl The payload.
     */
    public void setPayload (short pl) {
	this.payload = new BytesRef( ByteBuffer.allocate(2).putShort(pl).array());
    };


    /**
     * Set the payload as an integer value.
     *
     * @param pl The payload.
     */
    public void setPayload (int pl) {
	this.payload = new BytesRef( ByteBuffer.allocate(4).putInt(pl).array());
    };

    
    /**
     * Set the payload as a long value.
     *
     * @param pl The payload.
     */
    public void setPayload (long pl) {
	this.payload = new BytesRef( ByteBuffer.allocate(8).putLong(pl).array());
    };


    /**
     * Set the payload as a string value.
     *
     * @param pl The payload.
     */
    public void setPayload (String pl) {
	this.payload = new BytesRef(pl);
    };


    /**
     * Set the payload as a byte array.
     *
     * @param pl The payload.
     */
    public void setPayload (byte[] pl) {
	this.payload = new BytesRef(pl);
    };


    /**
     * Set the payload as a BytesRef.
     *
     * @param pl The payload.
     */
    public void setPayload (BytesRef pl) {
	this.payload = pl;
    };

    /**
     * Get the payload.
     *
     * @return The payload as a BytesRef.
     */
    public BytesRef getPayload () {
	return this.payload;
    };


    /**
     * Set the start position of the term.
     *
     * @param The start position.
     */
    public void setStart (int value) {
	this.start = value;
    };


    /**
     * Get the start position.
     *
     * @return The start position.
     */
    public int getStart () {
	return this.start;
    };


    /**
     * Set the end position of the term.
     *
     * @param The end position.
     */
    public void setEnd (int value) {
	this.end = value;
    };


    /**
     * Get the end position.
     *
     * @return The end position.
     */
    public int getEnd () {
	return this.end;
    };


    /**
     * Set the flag for stored offsets.
     *
     * @param value Boolean value indicating that the term
     *        contains stored offsets.
     */
    public void hasStoredOffsets (boolean value) {
	this.storeOffsets = value;
    };


    /**
     * Check if there are offsets stored.
     *
     * @return Boolean value indicating that the term
     *         contains stored offsets.
     */
    public boolean hasStoredOffsets () {
	return this.storeOffsets;
    };


    private void _fromString (String term) {
	String[] termSurface = term.split("\\$", 2);

	// Payload is given
	if (termSurface.length == 2) {
	    String payloadStr = termSurface[1];

	    // Payload has a type
	    if (payloadStr.charAt(0) == '<' && payloadStr.charAt(2) == '>') {

		// Rewind bytebuffer
		bb.rewind();

		// Split payload at type marker boundaries
		String[] pls = payloadStr.split("(?=<)|(?<=>)");

		l = 0; // Bytearray length

		try {
		    for (i = 1; i < pls.length;) {

			// Resize the bytebuffer
			if ((bb.capacity() - l) < 8) {
			    bb = ByteBuffer.allocate(bb.capacity() + 8)
				.put(bb.array());
			    bb.position(l);
			};

			switch (pls[i]) {
			case "<b>": // byte
			    bb.put(Byte.parseByte(pls[i+1]));
			    l++;
			    break;
			case "<s>": // short
			    bb.putShort(Short.parseShort(pls[i+1]));
			    l+=2;
			    break;
			case "<i>": // integer
			    bb.putInt(Integer.parseInt(pls[i+1]));
			    l+=4;
			    break;
			case "<l>": // long
			    bb.putLong(Long.parseLong(pls[i+1]));
			    l+=8;
			    break;
			};
			i+=2;
		    };
		
		    byte[] bytes = new byte[l];
		    System.arraycopy(bb.array(), 0, bytes, 0, l);
		    this.payload = new BytesRef(bytes);
		}
		catch (Exception e) {
		};
	    }

	    // Payload is a string
	    else {
		this.payload = new BytesRef(payloadStr);
	    };
	};
	
	// Parse offset information
	stringOffset = termSurface[0].split("\\#", 2);

	if (stringOffset.length == 2) {

	    // Split start and end position of the offset
	    String[] offset = stringOffset[1].split("\\-", 2);
   
	    // Start and end is given
	    if (offset.length == 2 && offset[0].length() > 0) {
		try {
		    this.start = Integer.parseInt(offset[0]);
		    this.end   = Integer.parseInt(offset[1]);

		}
		catch (NumberFormatException e) {
		};
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
	    sb.append('#')
	      .append(this.start)
	      .append('-')
	      .append(this.end);
	};

	if (this.payload != null) {
	    sb.append('$');
	    try {
		sb.append(this.payload.utf8ToString());
	    }
	    catch (AssertionError e) {
		sb.append("<?>")
	          .append(this.payload.toString().replace(' ', ','));
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
	    sb.append('$');
	    try {
		sb.append(this.payload.utf8ToString());
	    }
	    catch (AssertionError e) {
		sb.append("<?>")
		.append(this.payload.toString().replace(' ', ','));
	    };
	};
	return sb.toString();
    };
};
