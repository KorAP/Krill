package de.ids_mannheim.korap.model;

import static de.ids_mannheim.korap.util.KorapByte.*;
import org.apache.lucene.util.BytesRef;

import java.util.*;
import java.util.regex.*;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.IOException;

/*
 * Todo:
 * - !Payload is
 *   [4ByteStartOffset][14BitEndOffset-startOffset]
 *   [1BitBooleanIfSpan][1BitBooleanIfOpen]
 * - Payload is
 *   [4ByteOffsetStart][4ByteOffsetStart]
 */

/**
 * MultiTermTokenStream extends Lucenes {@link TokenStream}
 * to work with {@link MultiTermToken MultiTermTokens}.
 *
 * <blockquote><pre>
 *   MultiTermTokenStream mtts = new MultiTermTokenStream(
 *       "[s:den#0-3|i:den|p:DET|l:der|m:c:acc|m:n:sg|m:masc]"
 *   );
 * </pre></blockquote>
 *
 * @author diewald
 * @see TokenStream
 */
public class MultiTermTokenStream extends TokenStream {
    private CharTermAttribute charTermAttr;
    private PositionIncrementAttribute posIncrAttr;
    private PayloadAttribute payloadAttr;

    private static final Pattern pattern =
        Pattern.compile("\\[(?:\\([0-9]+-[0-9]+\\))?([^\\]]+?)\\]");

    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;
    private final Logger log = LoggerFactory.getLogger(MultiTermTokenStream.class);

    private List<MultiTermToken> multiTermTokens;
    private int mttIndex   = 0,
                mtIndex    = 0;
    private short i = 0;

    /**
     * Construct a new MultiTermTokenStream object.
     */
    public MultiTermTokenStream () {
        this.charTermAttr    = this.addAttribute(CharTermAttribute.class);
        this.posIncrAttr     = this.addAttribute(PositionIncrementAttribute.class);
        this.payloadAttr     = this.addAttribute(PayloadAttribute.class);
        this.multiTermTokens = new ArrayList<MultiTermToken>(100);
    };


    /**
     * Construct a new MultiTermTokenStream object
     *
     * @param stream The stream as a string representation.
     */
    public MultiTermTokenStream (String stream) {
        this();
        this._fromString(stream);
    };


    /**
     * Construct a new MultiTermTokenStream object
     *
     * @param stream The stream as a {@link Reader} object.
     * @throws IOException
     */
    public MultiTermTokenStream (Reader stream) throws IOException {
        this();
	
        StringBuilder sb = new StringBuilder(4096);
        char[] buf = new char[128];

        int j;
        while ((j = stream.read(buf)) > 0)
            sb.append(buf, 0, j);

        this._fromString(sb.toString());
    };


    /**
     * Append a {@link MultiTermToken} to the MultiTermTokenStream.
     *
     * @param mtt A {@link MultiTermToken}.
     * @return The {@link MultiTermTokenStream} object for chaining.
     */
    public MultiTermTokenStream addMultiTermToken (MultiTermToken mtt) {
        this.multiTermTokens.add(mtt);
        return this;
    };


    /**
     * Append a {@link MultiTermToken} to the MultiTermTokenStream
     * by means of a set of {@link MultiTerm MultiTerms}.
     *
     * @param mts A list of {@link MultiTerm} objects.
     * @return The {@link MultiTermTokenStream} object for chaining.
     */
    public MultiTermTokenStream addMultiTermToken
        (MultiTerm mts, MultiTerm ... moreTerms) {
        return this.addMultiTermToken(new MultiTermToken(mts, moreTerms));
    };


    /**
     * Append a {@link MultiTermToken} to the MultiTermTokenStream
     * by means of a single {@link MultiTerm} as a prefixed term.
     *
     * @param prefix A prefix character of a surface form of a {@link MultiTerm}.
     * @param surface A surface string of a {@link MultiTerm}.
     * @return The {@link MultiTermTokenStream} object for chaining.
     */
    public MultiTermTokenStream addMultiTermToken
        (char prefix, String surface) {
        return this.addMultiTermToken(new MultiTermToken(prefix, surface));
    };


    /**
     * Append a {@link MultiTermToken} to the MultiTermTokenStream
     * by means of {@link MultiTerm MultiTerm} represented as a set
     * of terms represented as strings.
     *
     * @param surface At least one surface string of a {@link MultiTerm}.
     * @return The {@link MultiTermTokenStream} object for chaining.
     */
    public MultiTermTokenStream addMultiTermToken
        (String surface, String ... moreTerms) {
        return this.addMultiTermToken(new MultiTermToken(surface, moreTerms));
    };


    /**
     * Add meta information to the MultiTermTokenStream.
     *
     * <strong>This is experimental!</strong>
     *
     * @param key A string for denoting the meta information.
     * @param value The value of the meta key as a string.
     * @return The {@link MultiTermTokenStream} object for chaining.
     */
    public MultiTermTokenStream addMeta (String key, String value) {
        MultiTerm mt = new MultiTerm('-', key);
        mt.setPayload(value);
        this.multiTermTokens.get(0).add(mt);
        return this;
    };


    /**
     * Add meta information to the MultiTermTokenStream.
     *
     * <strong>This is experimental!</strong>
     *
     * @param key A string for denoting the meta information.
     * @param value The value of the meta key as a byte array.
     * @return The {@link MultiTermTokenStream} object for chaining.
     */
    public MultiTermTokenStream addMeta (String key, byte[] value) {
        MultiTerm mt = new MultiTerm('-', key);
        mt.setPayload(value);
        this.multiTermTokens.get(0).add(mt);
        return this;
    };


    /**
     * Add meta information to the MultiTermTokenStream.
     *
     * <strong>This is experimental!</strong>
     *
     * @param key A string for denoting the meta information.
     * @param value The value of the meta key as a short value.
     * @return The {@link MultiTermTokenStream} object for chaining.
     */
    public MultiTermTokenStream addMeta (String key, short value) {
        MultiTerm mt = new MultiTerm('-', key);
        mt.setPayload(value);
        this.multiTermTokens.get(0).add(mt);
        return this;
    };


    /**
     * Add meta information to the MultiTermTokenStream.
     *
     * <strong>This is experimental!</strong>
     *
     * @param key A string for denoting the meta information.
     * @param value The value of the meta key as a long value.
     * @return The {@link MultiTermTokenStream} object for chaining.
     */
    public MultiTermTokenStream addMeta (String key, long value) {
        MultiTerm mt = new MultiTerm('-', key);
        mt.setPayload(value);
        this.multiTermTokens.get(0).add(mt);
        return this;
    };


    /**
     * Add meta information to the MultiTermTokenStream.
     *
     * <strong>This is experimental!</strong>
     *
     * @param key A string for denoting the meta information.
     * @param value The value of the meta key as a integer value.
     * @return The {@link MultiTermTokenStream} object for chaining.
     */
    public MultiTermTokenStream addMeta (String key, int value) {
        MultiTerm mt = new MultiTerm('-', key);
        mt.setPayload(value);
        this.multiTermTokens.get(0).add(mt);
        return this;
    };


    /**
     * Get a {@link MultiTermToken} by index.
     *
     * @param index The index position of a {@link MultiTermToken}
     *        in the {@link MultiTermTokenStream}.
     * @return A {@link MultiTermToken}.
     */
    public MultiTermToken get (int index) {
        return this.multiTermTokens.get(index);
    };


    /**
     * Get the number of {@link MultiTermToken MultiTermTokens}
     * in the stream.
     *
     * @return The number of {@link MultiTermToken MultiTermTokens}
     *         in the stream.
     */
    public int getSize () {
        return this.multiTermTokens.size();
    };


    /**
     * Serialize the MultiTermTokenStream to a string.
     *
     * @return The MultiTermTokenStream as a string.
     */
    public String toString () {
        StringBuffer sb = new StringBuffer();
        for (MultiTermToken mtt : this.multiTermTokens) {
            sb.append( mtt.toString() );
        };
        return sb.toString();
    };


    // Deserialize a string
    private void _fromString (String stream) {
        Matcher matcher = pattern.matcher(stream);

        while (matcher.find()) {
            String[] seg = matcher.group(1).split("\\|");
            MultiTermToken mtt = new MultiTermToken( seg[0] );

            for (i = 1; i < seg.length; i++)
                mtt.add(seg[i]);
            
            this.addMultiTermToken(mtt);
        };
    };


    /*
     * Increment the token in the MultiTermTokenStream.
     * This overrides the function in Lucene's TokenStream.
     */
    @Override
    public final boolean incrementToken() throws IOException {
        this.payloadAttr.setPayload(null);

        // Last token reached
        if (this.multiTermTokens.size() == this.mttIndex) {
            reset();
            return false;
        };

        // Get current token
        MultiTermToken mtt = this.multiTermTokens.get( this.mttIndex );

        // Sort the MultiTermToken
        mtt.sort();

        // Last term reached
        if (mtt.terms.size() == this.mtIndex) {
            this.mtIndex = 0;
            this.mttIndex++;

            // Last term of last token reached
            if (this.multiTermTokens.size() == this.mttIndex) {
                reset();
                return false;
            }

            // Get last token
            else {
                mtt = this.multiTermTokens.get( this.mttIndex );
            };
        };

        // Get current term
        MultiTerm mt = mtt.terms.get(this.mtIndex);

        // Set the relative position to the former term
        posIncrAttr.setPositionIncrement( this.mtIndex == 0 ? 1 : 0 );
        charTermAttr.setEmpty();
        charTermAttr.append( mt.term );

        BytesRef payload = new BytesRef();

        // There is offset information
        if (mt.start != mt.end) {
            if (DEBUG)
                log.trace("MultiTerm with payload offset: {}-{}", mt.start, mt.end);

            // Add offsets to BytesRef payload
            payload.append(new BytesRef(int2byte(mt.start)));
            payload.append(new BytesRef(int2byte(mt.end)));
        };

        // There is payload in the MultiTerm
        if (mt.payload != null) {
            payload.append(mt.payload);
            if (DEBUG)
                log.trace("Create payload[1] {}", payload.toString());
        };
        
        // There is payload in the current token to index
        if (payload.length > 0) {
            payloadAttr.setPayload(payload);
            if (DEBUG)
                log.trace("Set payload[2] {}", payload.toString());
        };

        // Some debug loggings
        if (DEBUG) {
            StringBuilder sb = new StringBuilder("Index: [");
            sb.append(mt.term);
            if (payload.length > 0)
                sb.append('$').append(payload.toString());
            sb.append(']');
            sb.append(" with increment ").append(this.mtIndex == 0 ? 1 : 0);
            
            log.trace(sb.toString());
        };

        this.mtIndex++;
        return true;
    };

    @Override
    public void reset() {
        this.mttIndex = 0;
        this.mtIndex = 0;
    };
};
