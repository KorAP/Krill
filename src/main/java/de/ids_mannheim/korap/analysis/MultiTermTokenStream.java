package de.ids_mannheim.korap.analysis;

import de.ids_mannheim.korap.analysis.MultiTerm;
import de.ids_mannheim.korap.analysis.MultiTermToken;
import static de.ids_mannheim.korap.util.KorapByte.*;
import org.apache.lucene.util.BytesRef;

import java.util.*;
import java.util.regex.*;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/*
  Todo:
   - !Payload is [4ByteStartOffset][14BitEndOffset-startOffset][1BitBooleanIfSpan][1BitBooleanIfOpen]
   - Payload is [4ByteOffsetStart][4ByteOffsetStart]
*/

/**
 * @author Nils Diewald
 * @version 0.3
 *
 * MultiTermTokenStream extends Lucenes TokenStream class to work with MultiTermTokens.
 *
 * @see org.apache.lucene.analysis.TokenStream
 */
public class MultiTermTokenStream extends TokenStream {
    private CharTermAttribute charTermAttr;
    private PositionIncrementAttribute posIncrAttr;
    private PayloadAttribute payloadAttr;

    private static final Pattern pattern = Pattern.compile("\\[(?:\\([0-9]+-[0-9]+\\))?([^\\]]+?)\\]");

    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;
    private final Logger log = LoggerFactory.getLogger(MultiTermTokenStream.class);

    private List<MultiTermToken> multiTermTokens;
    private int mttIndex = 0, mtIndex  = 0;
    private static short i = 0;


    /**
     * The empty Constructor.
     */
    public MultiTermTokenStream () {
        this.charTermAttr    = this.addAttribute(CharTermAttribute.class);
        this.posIncrAttr     = this.addAttribute(PositionIncrementAttribute.class);
	this.payloadAttr     = this.addAttribute(PayloadAttribute.class);
	this.multiTermTokens = new ArrayList<MultiTermToken>(100);
    };


    /**
     * The Constructor.
     *
     * @param stream The MultiTermTokenStream as a string representation.
     */
    public MultiTermTokenStream (String stream) {
	this();

	Matcher matcher = pattern.matcher(stream);

	while (matcher.find()) {

	    String[] seg = matcher.group(1).split("\\|");
	    MultiTermToken mtt = new MultiTermToken( seg[0] );

	    for (i = 1; i < seg.length; i++)
		mtt.add(seg[i]);

	    this.addMultiTermToken(mtt);
	};
    };


    /**
     * Add a MultiTermToken to the end of the MultiTermTokenStream.
     *
     * @param mtt A MultiTermToken.
     */
    public void addMultiTermToken (MultiTermToken mtt) {
	this.multiTermTokens.add(mtt);
    };


    /**
     * Add a MultiTermToken by means of MultiTerms to the end of
     * the MultiTermTokenStream.
     *
     * @param term At least one MultiTerm.
     */
    public void addMultiTermToken (MultiTerm term, MultiTerm ... moreTerms) {
	this.addMultiTermToken(new MultiTermToken(term, moreTerms));
    };


    /**
     * Add a MultiTermToken by means of a single MultiTerm to the end of
     * the MultiTermTokenStream.
     *
     * @param prefix A prefix character of a surface form of a MultiTerm.
     * @param surface A surface string of a MultiTerm.
     */
    public void addMultiTermToken (char prefix, String surface) {
	this.addMultiTermToken(new MultiTermToken(prefix, surface));
    };


    /**
     * Add a MultiTermToken by means of a a series of surface strings
     * to the end of the MultiTermTokenStream.
     *
     * @param surface At least one surface string of a MultiTerm.
     */
    public void addMultiTermToken (String surface, String ... moreTerms) {
	this.addMultiTermToken(new MultiTermToken(surface, moreTerms));
    };


    /**
     * Add meta information to the MultiTermTokenStream.
     *
     * @param key A string for denoting the meta information.
     * @param value The value of the meta key as a string.
     */
    public void addMeta (String key, String value) {
	MultiTerm mt = new MultiTerm('-', key);
	mt.setPayload(value);
	this.multiTermTokens.get(0).add(mt);
    };


    /**
     * Add meta information to the MultiTermTokenStream.
     *
     * @param key A string for denoting the meta information.
     * @param value The value of the meta key as a byte array.
     */
    public void addMeta (String key, byte[] value) {
	MultiTerm mt = new MultiTerm('-', key);
	mt.setPayload(value);
	this.multiTermTokens.get(0).add(mt);
    };


    /**
     * Add meta information to the MultiTermTokenStream.
     *
     * @param key A string for denoting the meta information.
     * @param value The value of the meta key as a short value.
     */
    public void addMeta (String key, short value) {
	MultiTerm mt = new MultiTerm('-', key);
	mt.setPayload(value);
	this.multiTermTokens.get(0).add(mt);
    };


    /**
     * Add meta information to the MultiTermTokenStream.
     *
     * @param key A string for denoting the meta information.
     * @param value The value of the meta key as a long value.
     */
    public void addMeta (String key, long value) {
	MultiTerm mt = new MultiTerm('-', key);
	mt.setPayload(value);
	this.multiTermTokens.get(0).add(mt);
    };


    /**
     * Add meta information to the MultiTermTokenStream.
     *
     * @param key A string for denoting the meta information.
     * @param value The value of the meta key as a integer value.
     */
    public void addMeta (String key, int value) {
	MultiTerm mt = new MultiTerm('-', key);
	mt.setPayload(value);
	this.multiTermTokens.get(0).add(mt);
    };


    /**
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
        posIncrAttr.setPositionIncrement( mt.posIncr );
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

	if (DEBUG) {
	    StringBuilder sb = new StringBuilder("Index: [");
	    sb.append(mt.term);
	    if (payload.length > 0)
		sb.append('$').append(payload.toString());
	    sb.append(']');
	    sb.append(" with increment ").append(mt.posIncr);

	    log.trace(sb.toString());
	};

	this.mtIndex++;

        return true;
    };

    public String toString () {
	StringBuffer sb = new StringBuffer();
	for (MultiTermToken mtt : this.multiTermTokens) {
	    sb.append( mtt.toString() );
	};
	return sb.toString();
    };

    @Override
    public void reset() {
	this.mttIndex = 0;
	this.mtIndex = 0;
    };
};
