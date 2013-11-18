package de.ids_mannheim.korap.analysis;

import de.ids_mannheim.korap.analysis.MultiTerm;
import de.ids_mannheim.korap.analysis.MultiTermToken;
import static de.ids_mannheim.korap.util.KorapByte.*;
import org.apache.lucene.util.BytesRef;

import java.util.*;
import java.util.regex.*;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
// import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/*
  Todo:
  - Do not use offsetAttr!
#  - Payload is [4ByteStartOffset][14BitEndOffset-startOffset][1BitBooleanIfSpan][1BitBooleanIfOpen]
  - Payload is [4ByteOffsetStart][4ByteOffsetStart]
*/

/**
 * @author Nils Diewald
 *
 * MultiTermTokenStream extends Lucenes TokenStream class to work with MultiTermTokens.
 *
 * @see org.apache.lucene.analysis.TokenStream
 */
public class MultiTermTokenStream extends TokenStream {
    private CharTermAttribute charTermAttr;
    //     private OffsetAttribute offsetAttr;
    private PositionIncrementAttribute posIncrAttr;
    private PayloadAttribute payloadAttr;

    private static Pattern pattern = Pattern.compile("\\[(\\(([0-9]+)-([0-9]+)\\))?([^\\]]+?)\\]");

    private List<MultiTermToken> multiTermTokens;
    private int mttIndex = 0;
    private int mtIndex  = 0;
    //    private TokenTextGenerator ttGen = new TokenTextGenerator();

    private final Logger log = LoggerFactory.getLogger(MultiTermTokenStream.class);

    public MultiTermTokenStream () {
	//	this.offsetAttr   = this.addAttribute(OffsetAttribute.class);
        this.charTermAttr = this.addAttribute(CharTermAttribute.class);
        this.posIncrAttr  = this.addAttribute(PositionIncrementAttribute.class);
	this.payloadAttr = this.addAttribute(PayloadAttribute.class);
	this.multiTermTokens  = new ArrayList<MultiTermToken>();

	/*
        if (!indexTokens.isEmpty()){
            indexTokens.get(indexTokens.size() - 1).setIncrement(false);
        };
	*/
    };

    public MultiTermTokenStream (String stream) {
	this();

	int pos = 0;

	Matcher matcher = pattern.matcher(stream);

	while (matcher.find()) {

	    String[] seg = matcher.group(4).split("\\|");
	    MultiTermToken mtt = new MultiTermToken( seg[0] );

	    if (matcher.group(2) != null)
		mtt.start = Integer.parseInt(matcher.group(2));

	    if (matcher.group(3) != null)
		mtt.end = Integer.parseInt(matcher.group(3));

	    for (int i = 1; i < seg.length; i++)
		mtt.add(seg[i]);

	    this.addMultiTermToken(mtt);
	};
    };

    public void addMultiTermToken (MultiTermToken mtt) {
	this.multiTermTokens.add(mtt);
    };

    public void addMultiTermToken (MultiTerm term, MultiTerm ... moreTerms) {
	this.addMultiTermToken(new MultiTermToken(term, moreTerms));
    };

    public void addMultiTermToken (char prefix, String surface) {
	this.addMultiTermToken(new MultiTermToken(prefix, surface));
    };

    public void addMultiTermToken (String surface, String ... moreTerms) {
	this.addMultiTermToken(new MultiTermToken(surface, moreTerms));
    };

    public void addMeta (String key, String value) {
	MultiTerm mt = new MultiTerm('-', key);
	//	mt.storeOffsets(false);
	mt.payload(value);
	this.multiTermTokens.get(0).add(mt);
    };

    public void addMeta (String key, byte[] value) {
	MultiTerm mt = new MultiTerm('-', key);
	//	mt.storeOffsets(false);
	mt.payload(value);
	this.multiTermTokens.get(0).add(mt);
    };


    public void addMeta (String key, short value) {
	MultiTerm mt = new MultiTerm('-', key);
	//	mt.storeOffsets(false);
	mt.payload(value);
	this.multiTermTokens.get(0).add(mt);
    };

    public void addMeta (String key, long value) {
	MultiTerm mt = new MultiTerm('-', key);
	//	mt.storeOffsets(false);
	mt.payload(value);
	this.multiTermTokens.get(0).add(mt);
    };

    public void addMeta (String key, int value) {
	MultiTerm mt = new MultiTerm('-', key);
	//	mt.storeOffsets(false);
	mt.payload(value);
	this.multiTermTokens.get(0).add(mt);
    };

    @Override
    public final boolean incrementToken() throws IOException {
	this.payloadAttr.setPayload(null);

	if (this.multiTermTokens.size() == this.mttIndex) {
	    reset();
	    return false;
	};

	MultiTermToken mtt = this.multiTermTokens.get( this.mttIndex );

	if (mtt.terms.size() == this.mtIndex) {
	    this.mtIndex = 0;
	    this.mttIndex++;
	    if (this.multiTermTokens.size() == this.mttIndex) {
		reset();
		return false;
	    }
	    else {
		mtt = this.multiTermTokens.get( this.mttIndex );
	    };
	};

	MultiTerm mt = mtt.terms.get(this.mtIndex);

	// Get the current index token

	// Set the relative position to the former term
        posIncrAttr.setPositionIncrement( mt.posIncr );
        charTermAttr.setEmpty();
	charTermAttr.append( mt.term );

	BytesRef payload = new BytesRef();
	if (mt.start != mt.end) {
	    log.trace("MultiTerm with payload offset: {}-{}", mt.start, mt.end);
	    payload.append(new BytesRef(int2byte(mt.start)));
	    payload.append(new BytesRef(int2byte(mt.end)));
	    /*
	      }
	      else if (mtt.start != mtt.end) {
	      payload.append(new BytesRef(int2byte(mtt.start)));
	      payload.append(new BytesRef(int2byte(mtt.end)));
	    */
	};

	// Payload
	if (mt.payload != null) {
	    payload.append(mt.payload());
	    log.trace("Create payload[1] {}", payload.toString());
	};

	if (payload.length > 0) {
	    log.trace("Set payload[2] {}", payload.toString());
	    payloadAttr.setPayload(payload);
	};

	if (log.isTraceEnabled()) {
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
