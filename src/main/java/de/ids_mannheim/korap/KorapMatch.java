package de.ids_mannheim.korap;
import java.util.*;
import java.lang.StringBuffer;
import java.nio.ByteBuffer;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.ids_mannheim.korap.index.PositionsToOffset;
import static de.ids_mannheim.korap.util.KorapHTML.*;

// import org.apache.commons.codec.binary.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.lucene.util.FixedBitSet;

/*
  Todo: The implemented classes and private names are horrible!
  Refactor, future-me!
*/

/**
 * Representation of Matches in a KorapResult.
 *
 * @see KorapResult
 * @author ndiewald
 */
public class KorapMatch extends KorapDocument {
    ObjectMapper mapper = new ObjectMapper();

    // Snippet information
    @JsonIgnore
    public short leftContext,
  	         rightContext;

    @JsonIgnore
    public int startPos,
	       endPos;

    @JsonIgnore
    public int potentialStartPosChar = -1,
	       potentialEndPosChar   = -1;

    private int startOffsetChar = 0;

    @JsonIgnore
    public boolean leftTokenContext,
	           rightTokenContext;

    private String tempSnippet,
	           snippetHTML,
	           snippetBrackets,
	           identifier;

    private HighlightCombinator snippetStack;

    private boolean startMore = true,
	            endMore = true;

    private Collection<byte[]> payload;
    private ArrayList<int[]> highlight;
    private LinkedList<int[]> span;

    private PositionsToOffset positionsToOffset;
    private boolean processed = false;

    // Logger
    private final static Logger log = LoggerFactory.getLogger(KorapMatch.class);

    /**
     * Constructs a new KorapMatch object.
     *
     * @param pto The PositionsToOffset object, containing relevant
     *            positional information for highlighting
     * @see #snippetHTML()
     * @see #snippetBrackets()
     * @see PositionsToOffset
     */
    public KorapMatch (PositionsToOffset pto, int localDocID, int startPos, int endPos) {
	this.positionsToOffset = pto;
	this.localDocID = localDocID;
	this.startPos = startPos;
	this.endPos = endPos;

	// Preprocess matching
	pto.add(localDocID, startPos);
	pto.add(localDocID, endPos - 1);
    };

    /**
     * Insert a highlight for the snippet view by means of positional
     * offsets and an optional class number.
     *
     * @param start  Integer value of a span's positional start offset.
     * @param end    Integer value of a span's positional end offset.
     * @param number Optional class number of the highlight.
     */
    public void addHighlight (int start, int end, byte number) {
	this.addHighlight(start, end, (int) number);
    };

    public void addHighlight (int start, int end, short number) {
	this.addHighlight(start, end, (int) number);
    };

    public void addHighlight (int start, int end) {
	this.addHighlight(start, end, (int) 0);
    };

    public void addHighlight (int start, int end, int number) {
	if (this.highlight == null)
	    this.highlight = new ArrayList<int[]>(16);
	log.trace("Add highlight of class {} from {} to {}", number, start, end);

	this._reset();

	// Add this for offset search
	this.positionsToOffset.add(this.localDocID, start);
	this.positionsToOffset.add(this.localDocID, end);

	this.highlight.add(new int[]{ start, end, number});
    };

    @JsonProperty("docID")
    public String getDocID () {
	return super.getID();
    };

    public void setDocID (String id) {
	super.setID(id);
    };

    @Override
    @JsonProperty("ID")
    public String getID () {

	if (this.identifier != null)
	    return this.identifier;

	StringBuffer sb = new StringBuffer("match-");

	// Get prefix string corpus/doc
	if (this.getCorpusID() != null) {
	    sb.append(this.getCorpusID());

	    if (this.getDocID() != null) {
		sb.append('-');
		sb.append(this.getDocID());
	    };
	}
	else {
	    sb.append(this.localDocID);
	};

	sb.append('p');

	// Get Position information
	sb.append(startPos).append('-').append(endPos);

	if (this.highlight != null) {
	    for (int[] h : this.highlight) {
		sb.append('(').append(h[2]).append(')');
		sb.append(h[0]).append('-').append(h[1]);
	    };
	};

	if (this.processed) {
	    sb.append('c');
	    for (int[] s : this.span) {
		if (s[2] != -1)
		    sb.append('(').append(s[2]).append(')');
		sb.append(s[0] + this.startOffsetChar);
		sb.append('-');
		sb.append(s[1] + this.startOffsetChar);
	    };
	};
	return (this.identifier = sb.toString());
    };

    private void _reset () {
	this.processed = false;
	this.snippetHTML = null;
	this.snippetBrackets = null;
	this.identifier = null;
	if (this.span != null)
	    this.span.clear();
    };

    // Start building highlighted snippets
    private void _processHighlight () {

	if (processed)
	    return;

	log.trace("Start highlight processing ...");
	
	// Get the list of spans for matches and highlighting
	if (this.span == null || this.span.size() == 0) {
	    this._processHighlightSpans(
	        leftTokenContext,
		rightTokenContext
	    );
	};

	/*
	for (int[] s : spans) {
	    log.trace(" >> [Spans] Start: {}, End: {}, Class: {}, Dummy: {}",
		      s[0], s[1], s[2], s[3]);
	};
	*/

	// Create a stack for highlighted elements (opening and closing elements)
	ArrayList<int[]> stack = this._processHighlightStack();

	/*
	for (int[] s : stack) {
	    log.trace(" >> [Stack] Start: {}, End: {}, Class: {}, Dummy: {}",
		      s[0], s[1], s[2], s[3]);
	};
	*/

	// The temparary snippet is empty, nothing to do
	if (this.tempSnippet == null) {
	    processed = true;
	    return;
	};

	// Merge the element stack with the primary textual data
	this._processHighlightSnippet(this.tempSnippet, stack);

	// Match is processed - done
	processed = true;
    };

    /*
      Comparator class for opening tags
     */
    private class OpeningTagComparator implements Comparator<int[]> {
	@Override
	public int compare (int[] arg0, int[] arg1) {
	    // Check start positions
	    if (arg0[0] > arg1[0]) {
		return 1;
	    }
	    else if (arg0[0] == arg1[0]) {
		// Check endpositions
		if (arg0[1] > arg1[1])
		    return -1;
		return 1;
	    };
	    return -1;
	};
    };

    /*
      Comparator class for closing tags
     */
    private class ClosingTagComparator implements Comparator<int[]> {
	@Override
	public int compare (int[] arg0, int[] arg1) {
	    // Check end positions
	    if (arg0[1] > arg1[1]) {
		return 1;
	    }
	    else if (arg0[1] == arg1[1]) {
		// Check start positions
		if (arg0[0] < arg1[0])
		    return 1;
		return -1;
	    };
	    return -1;
	};
    };

    /*
      Private class for elements with highlighting information
     */
    private class HighlightCombinatorElement {

	// Type 0: Textual data
	// Type 1: Opening
	// Type 2: Closing
	private byte type;

	private int number = 0;
	// TODO: Should be possibly a short (as for the -1)

	private String characters;
	private boolean terminal = true;

	// Constructor for highlighting elements
	public HighlightCombinatorElement (byte type, int number) {
	    this.type = type;
	    this.number = number;
	};

	// Constructor for highlighting elements,
	// that may not be terminal, i.e. they were closed and will
	// be reopened for overlapping issues.
	public HighlightCombinatorElement (byte type, int number, boolean terminal) {
	    this.type     = type;
	    this.number   = number;
	    this.terminal = terminal;
	};

	// Constructor for textual data
	public HighlightCombinatorElement (String characters) {
	    this.type = (byte) 0;
	    this.characters = characters;
	};

	// Return html fragment for this combinator element
	public String toHTML (FixedBitSet level, byte[] levelCache) {	    
	    // Opening
	    if (this.type == 1) {
		StringBuilder sb = new StringBuilder();
		if (this.number == -1) {
		    sb.append("<span class=\"match\">");
		}
		else {
		    // Get the first free level slot
		    byte pos;
		    if (levelCache[this.number] != '\0') {
			pos = levelCache[this.number];
		    }
		    else {
			pos = (byte) level.nextSetBit(0);
			level.clear(pos);
			levelCache[this.number] = pos;
		    };
		    sb.append("<em class=\"class-")
                      .append(this.number)
		      .append(" level-")
                      .append(pos)
                      .append("\">");
		};
		return sb.toString();
	    }
	    // Closing
	    else if (this.type == 2) {
		if (this.number == -1)
		    return "</span>";

		if (this.terminal)
		    level.set((int) levelCache[this.number]);
		return "</em>";
	    };

	    // HTML encode primary data
	    return encodeHTML(this.characters);
	};

	// Return bracket fragment for this combinator element
	public String toBrackets () {
	    if (this.type == 1) {
		StringBuilder sb = new StringBuilder();
		if (this.number == -1) {
		    sb.append("[");
		}
		else {
		    sb.append("{");
		    if (this.number != 0)
			sb.append(this.number).append(':');
		};
		return sb.toString();
	    }
	    else if (this.type == 2) {
		if (this.number == -1)
		    return "]";
		return "}";
	    };
	    return this.characters;
	};

    };

    /*
      Private class for combining highlighting elements
     */
    private class HighlightCombinator {
	private LinkedList<HighlightCombinatorElement> combine;
	private LinkedList<Integer> balanceStack = new LinkedList<>();
	private ArrayList<Integer> tempStack = new ArrayList<>(32);

	// Empty constructor
	public HighlightCombinator () {
	    this.combine = new LinkedList<>();
	};

	// Return the combination stack
	public LinkedList<HighlightCombinatorElement> stack () {
	    return this.combine;
	};

	// get the first element (without removing)
	public HighlightCombinatorElement getFirst () {
	    return this.combine.getFirst();
	};

	// get the last element (without removing)
	public HighlightCombinatorElement getLast () {
	    return this.combine.getLast();
	};

	// get an element by index (without removing)
	public HighlightCombinatorElement get (int index) {
	    return this.combine.get(index);
	};

	// Get the size of te combinator stack
	public short size () {
	    return (short) this.combine.size();
	};

	// Add primary data to the stack
	public void addString (String characters) {
	    this.combine.add(new HighlightCombinatorElement(characters));
	};

	// Add opening highlight combinator to the stack
	public void addOpen (int number) {
	    this.combine.add(new HighlightCombinatorElement((byte) 1, number));
	    this.balanceStack.add(number);
	};

	// Add closing highlight combinator to the stack
	public void addClose (int number) {
	    HighlightCombinatorElement lastComb;
	    this.tempStack.clear();

	    StringBuilder sb = new StringBuilder("Stack for checking with ");
	    sb.append(number).append(" is ");
	    for (int s : this.balanceStack) {
		sb.append('[').append(s).append(']');
	    };
	    log.trace(sb.toString());

	    // class number of the last element
	    int eold = this.balanceStack.removeLast();

	    // the closing element is not balanced
	    while (eold != number) {

		// Retrieve last combinator on stack
		lastComb = this.combine.peekLast();

		log.trace("Closing element is unbalanced - {} != {} with lastComb {}|{}|{}", eold, number, lastComb.type, lastComb.number, lastComb.characters);

		// combinator is opening and the number is not equal to the last
		// element on the balanceStack
		if (lastComb.type == 1 && lastComb.number == eold) {

		    // Remove the last element - it's empty and uninteresting!
		    this.combine.removeLast();
		}

		// combinator is either closing (??) or another opener
		else {

		    log.trace("close element a) {}", eold);

		    // Add a closer for the old element (this has following elements)
		    this.combine.add(new HighlightCombinatorElement((byte) 2, eold, false));
		};

		// add this element number temporarily on the stack
		tempStack.add(eold);

		// Check next element
		eold = this.balanceStack.removeLast();
	    };

	    // Get last combinator on the stack
	    lastComb = this.combine.peekLast();

	    log.trace("LastComb: " + lastComb.type + '|' + lastComb.number + '|' + lastComb.characters + " for " + number);
	    /*
	    // The last combinator is opening and identical to the current one
	    if (lastComb.type == 1 && lastComb.number == number) {
		// Remove the damn thing - It's empty and uninteresting!
		this.combine.removeLast();
	    }
	    else {
		// Add a closer
		this.combine.add(new HighlightCombinatorElement((byte) 2, number));
	    };
	    */

	    log.trace("Stack for checking 2: {}|{}|{}|{}", lastComb.type, lastComb.number, lastComb.characters, number);

	    if (lastComb.type == 1 && lastComb.number == number) {
		while (lastComb.type == 1 && lastComb.number == number) {
		    // Remove the damn thing - It's empty and uninteresting!
		    this.combine.removeLast();
		    lastComb = this.combine.peekLast();
		};
	    }
	    else {
		log.trace("close element b) {}", number);

		// Add a closer
		this.combine.add(new HighlightCombinatorElement((byte) 2, number));
	    };


	    // Fetch everything from the tempstack and reopen it
	    for (int e : tempStack) {
		log.trace("Reopen element {}", e);
		combine.add(new HighlightCombinatorElement((byte) 1, e));
		balanceStack.add(e);
	    };
	};

	// Get all combined elements as a string
	public String toString () {
	    StringBuilder sb = new StringBuilder();
	    for (HighlightCombinatorElement e : combine) {
		sb.append(e.toString()).append("\n");
	    };
	    return sb.toString();
	};
    };

    private void _processHighlightSnippet (String clean, ArrayList<int[]> stack) {
	int pos = 0;
	int oldPos = 0;

	log.trace("Create Snippet");

	this.snippetStack = new HighlightCombinator();

	for (int[] element : stack) {
	    pos = element[3] != 0 ? element[0] : element[1];

	    if (pos > oldPos) {
		snippetStack.addString(clean.substring(oldPos, pos));

		oldPos = pos;
	    };

	    if (element[3] != 0) {
		snippetStack.addOpen(element[2]);
	    }
	    else {
		snippetStack.addClose(element[2]);
	    };
	};

	if (clean.length() > pos) {
	    snippetStack.addString(clean.substring(pos));
	};
    };

    @Deprecated
    public String snippetHTML () {
	return this.getSnippetHTML();
    };

    @JsonProperty("snippet")
    public String getSnippetHTML () {

	this._processHighlight();

	if (this.processed && this.snippetHTML != null)
	    return this.snippetHTML;

	log.trace("Create HTML Snippet");

	StringBuilder sb = new StringBuilder();

	short start = (short) 0;
	short end = this.snippetStack.size();
	FixedBitSet level = new FixedBitSet(16);
	level.set(0, 15);
	byte[] levelCache = new byte[16];

	HighlightCombinatorElement elem = this.snippetStack.getFirst();

	// Create context
	sb.append("<span class=\"context-left\">");
	if (startMore)
	    sb.append("<span class=\"more\"></span>");

	if (elem.type == 0) {
	    sb.append(elem.toHTML(level, levelCache));
	    start++;
	};
	sb.append("</span>");

	elem = this.snippetStack.getLast();

	StringBuilder rightContext = new StringBuilder();

	// Create context, if trhere is any
	rightContext.append("<span class=\"context-right\">");
	if (elem != null && elem.type == 0) {
	    rightContext.append(elem.toHTML(level, levelCache));
	    end--;
	};
	if (endMore)
	    rightContext.append("<span class=\"more\"></span>");
	rightContext.append("</span>");

	for (short i = start; i < end; i++) {
	    sb.append(this.snippetStack.get(i).toHTML(level,levelCache));
	};

	sb.append(rightContext);

	return (this.snippetHTML = sb.toString());
    };

    @Deprecated
    public String snippetBrackets () {
	return this.getSnippetBrackets();
    };
    
    @JsonIgnore
    public String getSnippetBrackets () {

	this._processHighlight();

	if (this.processed && this.snippetBrackets != null)
	    return this.snippetBrackets;

	StringBuilder sb = new StringBuilder();

	if (startMore)
	    sb.append("... ");

	for (HighlightCombinatorElement hce : this.snippetStack.stack()) {
	    sb.append(hce.toBrackets());
	};

	if (endMore)
	    sb.append(" ...");

	return (this.snippetBrackets = sb.toString());
    };


    // This sorts all highlight and match spans to make them nesting correctly,
    // even in case they overlap
    // TODO: Not very fast - improve!
    private ArrayList<int[]> _processHighlightStack () {

	log.trace("Create Stack");

	LinkedList<int[]> openList  = new LinkedList<int[]>();
	LinkedList<int[]> closeList = new LinkedList<int[]>();

	openList.addAll(span);
	closeList.addAll(span);

	Collections.sort(openList, new OpeningTagComparator());
	Collections.sort(closeList, new ClosingTagComparator());

	ArrayList<int[]> stack = new ArrayList<>(openList.size() * 2);

	// Create stack unless both lists are empty
	while (!openList.isEmpty() || !closeList.isEmpty()) {

	    if (openList.isEmpty()) {
		stack.addAll(closeList);
		break;
	    };

	    if (openList.peekFirst()[0] < closeList.peekFirst()[1]) {
		int[] e = openList.removeFirst().clone();
		e[3] = 1;
		stack.add(e);
	    }
	    else {
		stack.add(closeList.removeFirst());
	    };
	};
	return stack;
    };


    private void _processHighlightSpans (boolean leftTokenContext,
					 boolean rightTokenContext) {
	int startOffsetChar,
	    endOffsetChar,
	    startPosChar,
	    endPosChar;

	log.trace("Create Spans");

	int ldid = this.localDocID;

	// Match position
	startPosChar = this.positionsToOffset.start(ldid, this.startPos);

	// Check potential differing start characters
	// e.g. from element spans
	if (potentialStartPosChar != -1 && startPosChar > potentialStartPosChar)
	    startPosChar = potentialStartPosChar;

	endPosChar = this.positionsToOffset.end(ldid, this.endPos - 1);

	if (endPosChar < potentialEndPosChar)
	    endPosChar = potentialEndPosChar;

	log.trace("Matchposition: {}-{}", startPosChar, endPosChar);

	// left context
	if (leftTokenContext) {
	    startOffsetChar = this.positionsToOffset.start(ldid, startPos - this.leftContext);
	}
	else {
	    startOffsetChar = startPosChar - this.leftContext;
	};

	// right context
	if (rightTokenContext) {
	    endOffsetChar = this.positionsToOffset.end(
	        ldid,
		this.endPos + this.rightContext - 1
	    );
	    log.trace("For endOffset {} ({}+{}-1) pto returns {}", (this.endPos + this.rightContext - 1), this.endPos, this.rightContext, endOffsetChar);
	}
	else {
	    if (endPosChar == -1) {
		endOffsetChar = -1;
	    }
	    else {
		endOffsetChar = endPosChar + this.rightContext;
	    };
	};

	// This can happen in case of non-token characters in the match and null offsets
	if (startOffsetChar > startPosChar) {
	    startOffsetChar = startPosChar;
	}
	else if (startOffsetChar < 0) {
	    startOffsetChar = 0;
	};

	// No ... at the beginning
	if (startOffsetChar == 0) {
	    startMore = false;
	};

	if (endOffsetChar != -1 && endOffsetChar < endPosChar)
	    endOffsetChar = endPosChar;

	this.startOffsetChar = startOffsetChar;


	log.trace("Offsetposition {} till {} with contexts {} and {}", startOffsetChar, endOffsetChar, leftContext, rightContext);

	if (endOffsetChar > -1 && endOffsetChar < this.getPrimaryDataLength()) {
	    this.tempSnippet = this.getPrimaryData(startOffsetChar, endOffsetChar);
	}
	else {
	    this.tempSnippet = this.getPrimaryData(startOffsetChar);
	    endMore = false;
	};

	log.trace("Temporary snippet is \"{}\"", this.tempSnippet);

	if (this.span == null)
	    this.span = new LinkedList<int[]>();

	this.identifier = null;

	// Todo: Simplify
	int[] intArray = new int[]{ startPosChar - startOffsetChar, endPosChar - startOffsetChar, -1, 0};
	log.trace("IntArray: {}", intArray);
	this.span.add(intArray);

	// highlights
	// -- I'm not sure about this.
	if (this.highlight != null) {
	    for (int[] highlight : this.highlight) {
		int start = this.positionsToOffset.start(ldid, highlight[0]) - startOffsetChar;
		int end = this.positionsToOffset.end(ldid, highlight[1]) - startOffsetChar;

		if (start < 0 || end < 0)
		    continue;

		intArray = new int[]{
		    start,
		    end,
		    highlight[2],
		    0 // Dummy value for later
		};

		log.trace("IntArray: {}", intArray);
		log.trace("PTO-start: {}", start + startOffsetChar);
		log.trace("PTO-end: {}", end + startOffsetChar);

		this.span.add(intArray);
	    };
	};
    };
};
