package de.ids_mannheim.korap.model;

import static de.ids_mannheim.korap.util.KorapArray.*;
import de.ids_mannheim.korap.util.CorpusDataException;
import org.apache.lucene.util.BytesRef;
import java.nio.ByteBuffer;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Don't use ByteBuffer!
 */
/**
 * A MultiTerm represents a single term (e.g. a word, an annotation, a relation)
 * that can be part of a MultiTermToken.
 *
 * A MultiTerm consists of a term representation string, optional character offset
 * information that matches the term to the character stream of the input text,
 * and an arbitrary payload.
 *
 * There is a simple string representation of MultiTerms supported:
 * The string is the first sequence of characters.
 * Offsets are written as an appended and dash separated pair of integers.
 * Payloads are written following a dollar sign.
 * Payload segments can be typed as being a short (s), an integer (i), or a long (l)
 * value in leading angular brackets.
 * All other (untyped) payloads are treated as being UTF-8 characer sequences.
 *
 * <blockquote><pre>
 *   MultiTerm test1 = new MultiTerm("test");
 *   MultiTerm test2 = new MultiTerm("test#0-4");
 *   MultiTerm test3 = new MultiTerm("test#0-4$Example");
 *   MultiTerm test4 = new MultiTerm("test#0-4$&lt;i&gt;1278");
 * </pre></blockquote>
 *
 * <strong>Warning</strong>: Strings that are malformed fail silently!
 *
 * @author diewald
 */
public class MultiTerm implements Comparable<MultiTerm> {
    public int start, end = 0;
    public String term = null;
    private boolean storeOffsets = false;
    public BytesRef payload = null;

    private static ByteBuffer bb = ByteBuffer.allocate(8);
    private static String[] stringOffset;

    private static short i, l;

    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;
    private final Logger log = LoggerFactory.getLogger(MultiTermTokenStream.class);


    /**
     * Construct a new MultiTerm object.
     */
    public MultiTerm () {
        this.term = "";
    };


    /**
     * Construct a new MultiTerm object.
     *
     * @param term The term surface (see synopsis).
     */
    public MultiTerm (String term) throws CorpusDataException {
        _fromString(term);
    };

    
    /**
     * Construct a new MultiTerm object.
     *
     * In addition to the normal surface representation,
     * this supports a prefix notation.
     * The following expressions are equal:
     *
     * <blockquote><pre>
     *   MultiTerm test1 = new MultiTerm('a', "bcd");
     *   MultiTerm test2 = new MultiTerm("a:bcd");
     * </pre></blockquote>
     *
     * @param prefix A special prefix for the term.
     * @param term The term surface (see synopsis).
     */
    public MultiTerm (char prefix, String term) throws CorpusDataException {
        StringBuilder sb = new StringBuilder();
        _fromString(sb.append(prefix).append(':').append(term).toString());
    };


    /**
     * Get the term value of the MultiTerm.
     *
     * @return The term as a string.
     */
    public String getTerm () {
        return this.term;
    };
    
    
    /**
     * Set the term value of the MultiTerm.
     *
     * @param term The term as a string.
     * @return The {@link MultIterm} object for chaining.
     */
    public MultiTerm setTerm (String term) {
        this.term = term;
        return this;
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
     * Set the payload as a {@link Byte} value.
     *
     * @param pl The payload.
     * @return The {@link MultiTerm} object for chaining.
     */
    public MultiTerm setPayload (Byte pl) {
        this.payload = new BytesRef( ByteBuffer.allocate(1).put(pl).array());
        return this;
    };

    
    /**
     * Set the payload as a short value.
     *
     * @param pl The payload.
     * @return The {@link MultiTerm} object for chaining.
     */
    public MultiTerm setPayload (short pl) {
        this.payload = new BytesRef( ByteBuffer.allocate(2).putShort(pl).array());
        return this;
    };


    /**
     * Set the payload as an integer value.
     *
     * @param pl The payload.
     * @return The {@link MultiTerm} object for chaining.
     */
    public MultiTerm setPayload (int pl) {
        this.payload = new BytesRef( ByteBuffer.allocate(4).putInt(pl).array());
        return this;
    };

    
    /**
     * Set the payload as a long value.
     *
     * @param pl The payload.
     * @return The {@link MultiTerm} object for chaining.
     */
    public MultiTerm setPayload (long pl) {
        this.payload = new BytesRef( ByteBuffer.allocate(8).putLong(pl).array());
        return this;
    };


    /**
     * Set the payload as a string value.
     *
     * @param pl The payload.
     * @return The {@link MultiTerm} object for chaining.
     */
    public MultiTerm setPayload (String pl) {
        this.payload = new BytesRef(pl);
        return this;
    };


    /**
     * Set the payload as a byte array.
     *
     * @param pl The payload.
     * @return The {@link MultiTerm} object for chaining.
     */
    public MultiTerm setPayload (byte[] pl) {
        this.payload = new BytesRef(pl);
        return this;
    };


    /**
     * Set the payload as a {@link BytesRef} object.
     *
     * @param pl The payload.
     * @return The {@link MultiTerm} object for chaining.
     */
    public MultiTerm setPayload (BytesRef pl) {
        this.payload = pl;
        return this;
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
     * Set the start position.
     *
     * @param start The start position.
     * @return The {@link MultiTerm} object for chaining.
     */
    public MultiTerm setStart (int start) {
        this.start = start;
        return this;
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
     * Set the end position.
     *
     * @param end The end position.
     * @return The {@link MultiTerm} object for chaining.
     */
    public MultiTerm setEnd (int end) {
        this.end = end;
        return this;
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


    /**
     * Set the flag for stored offsets, in case they are relevant.
     *
     * @param value Boolean value indicating that the term
     *        contains stored offsets.
     * @return The {@link MultiTerm} object for chaining.
     */
    public MultiTerm hasStoredOffsets (boolean value) {
        this.storeOffsets = value;
        return this;
    };


    /**
     * Represent the MultiTerm as a string (see synopsis).
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

    @Override
    public int compareTo (MultiTerm o) {
        if (this.payload == null || o.payload == null)
            return 0;
        if (this.end < o.end)
            return -1;
        else if (this.end > o.end)
            return 1;
        else if (this.start < o.start)
            return -1;
        else if (this.start > o.start)
            return 1;
        return 0;
    };


    /**
     * Represent the MultiTerm as a string.
     * Payloads are attached following a dollar sign.
     * All payloads are written as UTF-8 character sequences.
     * Offsets are neglected.
     *
     * Offsets are ignored.
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


    /*
     * Deserialize MultiTerm from string representation.
     */
    private void _fromString (String term) throws CorpusDataException {
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
                            bb = ByteBuffer.allocate(bb.capacity() + 8).
                                put(bb.array());
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
                    if (DEBUG)
                        log.warn(e.getMessage());
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
                    throw new CorpusDataException(
                        952,
                        "Given offset information is not numeric"
                    );
                };
            }
            else {
                throw new CorpusDataException(
                    953,
                    "Given offset information is incomplete"
                );
            };
        };
        this.term = stringOffset[0];
    };
};
