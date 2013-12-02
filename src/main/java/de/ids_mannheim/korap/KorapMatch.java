package de.ids_mannheim.korap;
import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.ids_mannheim.korap.index.PositionsToOffset;
import static de.ids_mannheim.korap.util.KorapHTML.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public short leftContext, rightContext;

    @JsonIgnore
    public int startPos, endPos;

    @JsonIgnore
    public int potentialStartPosChar = -1, potentialEndPosChar = -1;

    @JsonIgnore
    public boolean leftTokenContext, rightTokenContext;

    private String tempSnippet, snippetHTML, snippetBrackets;
    private HighlightCombinator snippetStack;
    private boolean startMore = true, endMore = true;

    private Collection<byte[]> payload;
    private ArrayList<int[]> highlight;

    // Logger
    private final static Logger log = LoggerFactory.getLogger(KorapMatch.class);

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
	this.addHighlight(start, end, (int) 1);
    };

    public void addHighlight (int start, int end, int number) {
	if (this.highlight == null)
	    this.highlight = new ArrayList<int[]>();

	log.trace("Add highlight of class {} from {} to {}", number, start, end);

	this.highlight.add(new int[]{ start, end, number});
    };

    /*
    public JSONObject toJSON() {
	JSONObject json = new JSONObject();
	json.put("internalDocID", this.internalDocID);
	
	if (this.author != null)
	    json.put("author", this.author);

	if (this.getPubDate() != null && this.getPubDate().year() > 0)
	    json.put("pubDate", this.getPubDate().toDisplay());

	if (this.snippetHTML() != null)
	    json.put("snippet", this.snippetHTML);

	// json.put("primary", this.primary);

	JSONArray pos = new JSONArray();
	pos.add(this.startPos);
	pos.add(this.endPos);
	json.put("position", pos);

	return json;
    };
    */


    /**
     * Generates a highlighted snippet for the mach, that can be
     * retrieved afterwards via snippetHTML() and snippetBrackets().
     * <p>
     * The information on offset positions has to be retrieved beforehand
     * by filling up the PositionsToOffset.
     *
     * @param pto The PositionsToOffset object, containing relevant
     *            positional information for highlighting
     * @see #snippetHTML()
     * @see #snippetBrackets()
     * @see PositionsToOffset
     */
    public void processHighlight (PositionsToOffset pto) {

	log.trace("Start highlight processing ...");
	
	// Get the list of spans for matches and highlighting
	LinkedList<int[]> spans = this._processHighlightSpans(
            pto,
	    leftTokenContext,
	    rightTokenContext
        );

	for (int[] s : spans) {
	    log.trace(" >> [Spans] Start: {}, End: {}, Class: {}, Dummy: {}", s[0], s[1], s[2], s[3]);
	};

	ArrayList<int[]> stack = this._processHighlightStack(spans);

	for (int[] s : stack) {
	    log.trace(" >> [Stack] Start: {}, End: {}, Class: {}, Dummy: {}", s[0], s[1], s[2], s[3]);
	};


	if (this.tempSnippet == null)
	    return;

	this._processHighlightSnippet(this.tempSnippet, stack);

	/*

	Collection.sort(openList);
with http://docs.oracle.com/javase/6/docs/api/java/util/Comparator.html
	*/
    };

    private class OpeningTagComparator implements Comparator<int[]> {
	@Override
	public int compare (int[] arg0, int[] arg1) {
	    if (arg0[0] > arg1[0]) {
		return 1;
	    }
	    else if (arg0[0] == arg1[0]) {
		if (arg0[1] > arg1[1])
		    return -1;
		return 1;
	    };
	    return -1;
	};
    };

    private class ClosingTagComparator implements Comparator<int[]> {
	@Override
	public int compare (int[] arg0, int[] arg1) {
	    if (arg0[1] > arg1[1]) {
		return 1;
	    }
	    else if (arg0[1] == arg1[1]) {
		if (arg0[0] < arg1[0])
		    return 1;
		return -1;
	    };
	    return -1;
	};
    };

    private class HighlightCombinatorElement {
	private short type;
	private int number;
	private String characters;

	public HighlightCombinatorElement (short type, int number) {
	    this.type = type;
	    this.number = number;
	};

	public HighlightCombinatorElement (String characters) {
	    this.type = 0;
	    this.characters = characters;
	};

	public String toHTML () {	    
	    if (this.type == 1) {
		StringBuilder sb = new StringBuilder();
		sb.append("<span class=\"");
		if (this.number == -1) {
		    sb.append("korap-match\"");
		}
		else {
		    sb.append("korap-highlight korap-class-")
			.append(this.number)
			.append('"');
		};
		sb.append('>');
		return sb.toString();
	    }
	    else if (this.type == 2) {
		return "</span>";
	    };
	    return encodeHTML(this.characters);
	};

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
		if (this.number == -1) {
		    return "]";
		}
		return "}";
	    };
	    return this.characters;
	};

    };

    private class HighlightCombinator {
	private LinkedList<HighlightCombinatorElement> combine;
	private LinkedList<Integer> balanceStack = new LinkedList<>();
	private ArrayList<Integer> tempStack = new ArrayList<>(32);

	public HighlightCombinator () {
	    this.combine = new LinkedList<>();
	};

	public LinkedList<HighlightCombinatorElement> stack () {
	    return this.combine;
	};

	public HighlightCombinatorElement getFirst () {
	    return this.combine.getFirst();
	};

	public HighlightCombinatorElement getLast () {
	    return this.combine.getLast();
	};

	public HighlightCombinatorElement get (int index) {
	    return this.combine.get(index);
	};

	public short size () {
	    return (short) this.combine.size();
	};

	public void addString (String characters) {
	    this.combine.add(new HighlightCombinatorElement(characters));
	};

	public void addOpen (int number) {
	    this.combine.add(new HighlightCombinatorElement((short) 1, number));
	    this.balanceStack.add(number);
	};

	public void addClose (int number) {
	    HighlightCombinatorElement lastComb;
	    this.tempStack.clear();
	    int eold = this.balanceStack.removeLast();
	    while (eold != number) {
		lastComb = this.combine.peekLast();
		if (lastComb.type == 1 && lastComb.number != eold) {
		    this.combine.removeLast();
		}
		else {
		    this.combine.add(new HighlightCombinatorElement((short) 2, eold));
		};
		tempStack.add(eold);
		eold = this.balanceStack.removeLast();
	    };
	    
	    lastComb = this.combine.peekLast();
	    if (lastComb.type == 1 && lastComb.number == number) {
		this.combine.removeLast();
	    }
	    else {
		this.combine.add(new HighlightCombinatorElement((short) 2, number));
	    };
	    
	    for (int e : tempStack) {
		combine.add(new HighlightCombinatorElement((short) 1, e));
		balanceStack.add(e);
	    };
	};

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
	if (this.snippetHTML != null)
	    return this.snippetHTML;

	StringBuilder sb = new StringBuilder();

	short start = (short) 0;
	short end   = this.snippetStack.size();

	HighlightCombinatorElement elem = this.snippetStack.getFirst();

	// Create context, if there is any
	if ((elem.type == 0) || startMore) {
	    sb.append("<span class=\"korap-context-left\">");
	    if (startMore)
		sb.append("<span class=\"korap-more\"></span>");
	    if (elem.type == 0) {
		sb.append(elem.toHTML());
		start++;
	    };
	    sb.append("</span>");
	};

	elem = this.snippetStack.getLast();

	StringBuilder rightContext = new StringBuilder();

	// Create context, if trhere is any
	if (endMore || (elem != null && elem.type == 0)) {
	    rightContext.append("<span class=\"korap-context-right\">");
	    if (elem != null && elem.type == 0) {
		rightContext.append(elem.toHTML());
		end--;
	    };
	    if (endMore)
		rightContext.append("<span class=\"korap-more\"></span>");
	    rightContext.append("</span>");
	};

	for (short i = start; i < end; i++) {
	    sb.append(this.snippetStack.get(i).toHTML());
	};

	if (rightContext != null) {
	    sb.append(rightContext);
	};

	return (this.snippetHTML = sb.toString());
    };

    @Deprecated
    public String snippetBrackets () {
	return this.getSnippetBrackets();
    };
    
    @JsonIgnore
    public String getSnippetBrackets () {
	if (this.snippetBrackets != null)
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


    // Todo: Not very fast - just a direct translation of the perl script
    private ArrayList<int[]> _processHighlightStack (LinkedList<int[]> spans) {

	log.trace("Create Stack");


	LinkedList<int[]> openList  = new LinkedList<int[]>();
	LinkedList<int[]> closeList = new LinkedList<int[]>();

	openList.addAll(spans);
	closeList.addAll(spans);

	Collections.sort(openList, new OpeningTagComparator());
	Collections.sort(closeList, new ClosingTagComparator());

	ArrayList<int[]> stack = new ArrayList<>(openList.size() * 2);

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


    private LinkedList<int[]> _processHighlightSpans (PositionsToOffset pto,
						      boolean leftTokenContext,
						      boolean rightTokenContext) {
	int startOffsetChar,
	    endOffsetChar,
	    startPosChar,
	    endPosChar;

	log.trace("Create Spans");

	int ldid = this.localDocID;

	// Match position
	startPosChar = pto.start(ldid, this.startPos);

	// Check potential differing start characters
	// e.g. from element spans
	if (potentialStartPosChar != -1 && startPosChar > potentialStartPosChar)
	    startPosChar = potentialStartPosChar;

	endPosChar = pto.end(ldid, this.endPos - 1);

	if (endPosChar < potentialEndPosChar)
	    endPosChar = potentialEndPosChar;

	log.trace("Matchposition: {}-{}", startPosChar, endPosChar);

	// left context
	if (leftTokenContext) {
	    startOffsetChar = pto.start(ldid, startPos - this.leftContext);
	}
	else {
	    startOffsetChar = startPosChar - this.leftContext;
	};

	// right context
	if (rightTokenContext) {
	    endOffsetChar = pto.end(ldid, this.endPos + this.rightContext - 1);
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


	log.trace("Offsetposition {} till {} with contexts {} and {}", startOffsetChar, endOffsetChar, leftContext, rightContext);


	if (endOffsetChar > -1 && endOffsetChar < this.getPrimaryDataLength()) {
	    this.tempSnippet = this.getPrimaryData(startOffsetChar, endOffsetChar);
	}
	else {
	    this.tempSnippet = this.getPrimaryData(startOffsetChar);
	    endMore = false;
	};

	log.trace("Temporary snippet is \"{}\"", this.tempSnippet);

        LinkedList<int[]> spans = new LinkedList<int[]>();

	// Todo: Simplify
	int[] intArray = new int[]{ startPosChar - startOffsetChar, endPosChar - startOffsetChar, -1, 0};
	log.trace("IntArray: {}", intArray);
	spans.add(intArray);

	// highlights
	// I'm not sure about this.
	if (this.highlight != null) {
	    for (int[] highlight : this.highlight) {

		/*

		int start = pto.start(ldid, highlight[0]);
		int end = pto.end(ldid, highlight[1]);

		// Todo: Does this have to be and or or?
		if (start == -1 || end == -1)
		    continue;

		if (start > startOffsetChar) {
		    start -= startOffsetChar;
		}
		else {
		    start = 0;
		};

		end -= startOffsetChar;
		*/

		int start = pto.start(ldid, highlight[0]) - startOffsetChar;
		int end = pto.end(ldid, highlight[1]) - startOffsetChar;

		if (start < 0 || end < 0)
		    continue;

		intArray = new int[]{
		    start,
		    end,
		    highlight[2],
		    0 // Dummy value for later
		};

		log.trace("IntArray: {}", intArray);
		log.trace("PTO-start: {}", pto.start(ldid, highlight[0]));
		log.trace("PTO-end: {}", pto.end(ldid, highlight[1]));

		spans.add(intArray);
	    };
	};

	return spans;
    };

};
