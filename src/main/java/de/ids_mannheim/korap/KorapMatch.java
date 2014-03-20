package de.ids_mannheim.korap;
import java.util.*;
import java.lang.StringBuffer;
import java.nio.ByteBuffer;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;

import de.ids_mannheim.korap.index.PositionsToOffset;
import de.ids_mannheim.korap.document.KorapPrimaryData;

import static de.ids_mannheim.korap.util.KorapHTML.*;
import de.ids_mannheim.korap.index.MatchIdentifier;
import de.ids_mannheim.korap.index.PosIdentifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.lucene.util.FixedBitSet;
import org.apache.lucene.document.Document;

/*
  Todo: The implemented classes and private names are horrible!
  Refactor, future-me!

  The number based Highlighttype is ugly - UGLY!
*/

/**
 * Representation of Matches in a KorapResult.
 *
 * @author Nils Diewald
 * @see KorapResult
 */
@JsonInclude(Include.NON_NULL)
public class KorapMatch extends KorapDocument {

    // Logger
    private final static Logger log = LoggerFactory.getLogger(KorapMatch.class);

    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;

    // Mapper for JSON serialization
    ObjectMapper mapper = new ObjectMapper();

    // Snippet information
    @JsonIgnore
    public short leftContextOffset,
  	         rightContextOffset;

    // Should be deprecated, but used wildly in tests!
    @JsonIgnore
    public int startPos, endPos;

    @JsonIgnore
    public int potentialStartPosChar = -1,
	       potentialEndPosChar   = -1;

    private String error = null;
    private String version;

    // TEMPRARILY
    @JsonIgnore
    public int localDocID = -1;

    HashMap<Integer, String>   annotationNumber = new HashMap<>(16);
    HashMap<Integer, Relation> relationNumber   = new HashMap<>(16);
    HashMap<Integer, Integer>  identifierNumber = new HashMap<>(16);

    // -1 is match highlight
    int annotationNumberCounter = 256;
    int relationNumberCounter   = 2048;
    int identifierNumberCounter = -2;

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
    private ArrayList<Highlight> highlight;
    private LinkedList<int[]> span;

    private PositionsToOffset positionsToOffset;
    private boolean processed = false;

    /**
     * Constructs a new KorapMatch object.
     * TODo: Maybe that's not necessary!
     *
     * @param pto The PositionsToOffset object, containing relevant
     *            positional information for highlighting
     * @param localDocID Document ID based on the atomic reader.
     * @param startPos Start position of the match in the document.
     * @param endPos End position of the match in the document.
     *
     * @see #snippetHTML()
     * @see #snippetBrackets()
     * @see PositionsToOffset
     */
    public KorapMatch (PositionsToOffset pto, int localDocID, int startPos, int endPos) {
	this.positionsToOffset = pto;
	this.localDocID = localDocID;
	this.startPos = startPos;
	this.endPos = endPos;
    };

    
    /**
     * Constructs a new KorapMatch object.
     */
    public KorapMatch () {};

    
    /**
     * Constructs a new KorapMatch object.
     *
     * @param idString Match identifier string as provided by KorapResult.
     * @param includeHighlights Boolean value indicating if possible provided
     *        highlight information should be ignored or not.
     */
    public KorapMatch (String idString, boolean includeHighlights) {
	MatchIdentifier id = new MatchIdentifier(idString);
	this.setCorpusID(id.getCorpusID());
	this.setDocID(id.getDocID());
	this.setStartPos(id.getStartPos());
	this.setEndPos(id.getEndPos());

	if (includeHighlights)
	    for (int[] pos : id.getPos()) {
		if (pos[0] < id.getStartPos() || pos[1] > id.getEndPos())
		    continue;

		this.addHighlight(pos[0], pos[1], pos[2]);
	    };
    };

    
    /**
     * Private class of highlights.
     */   
    private class Highlight {
	public int start, end;
	public int number = -1;

	// Relational highlight
       	public Highlight (int start, int end, String annotation, int ref) {
	    this.start = start;
	    this.end   = end;
	    // TODO: This can overflow!
	    this.number = relationNumberCounter++;
	    relationNumber.put(this.number, new Relation(annotation, ref));
	};

	// Span highlight
	public Highlight (int start, int end, String annotation) {
	    this.start = start;
	    this.end   = end;
	    // TODO: This can overflow!
	    if (annotationNumberCounter < 2048) {
		this.number = annotationNumberCounter++;
		annotationNumber.put(this.number, annotation);
	    };
	};

	// Simple highlight
	public Highlight (int start, int end, int number) {
	    this.start  = start;
	    this.end    = end;
	    this.number = number;
	};
    };

    
    /**
     * Private class of relations.
     */   
    private class Relation {
	public int ref;
	public String annotation;
	public Relation (String annotation, int ref) {
	    this.annotation = annotation;
	    this.ref = ref;
	};
    };


    /**
     * Insert a highlight for the snippet view by means of positional
     * offsets and an optional class number.
     *
     * @param start  Integer value of a span's positional start offset.
     * @param end    Integer value of a span's positional end offset.
     * @param number Optional class number of the highlight.
     */
    public void addHighlight (int start, int end) {
	this.addHighlight(new Highlight(start, end, (int) 0));
    };

    public void addHighlight (int start, int end, byte number) {
	this.addHighlight(new Highlight(start, end, (int) number));
    };

    public void addHighlight (int start, int end, short number) {
	this.addHighlight(new Highlight(start, end, (int) number));
    };

    public void addHighlight (int start, int end, int number) {
	this.addHighlight(new Highlight(start, end, number));
    };

    
    /**
     * Insert a highlight for the snippet view.
     *
     * @param hl A highlight object to add to the match.
     */
    public void addHighlight (Highlight hl) {

	if (this.highlight == null)
	    this.highlight = new ArrayList<Highlight>(16);

	if (DEBUG)
	    log.trace("Add highlight from pos {}-{} of class {}",
		      hl.start, hl.end, hl.number);

	// Reset the fetched match data
	this._reset();

	this.highlight.add(hl);
    };


    /**
     * Insert a textual annotation for the snippet view by
     * means of positional offsets and an annotation string.
     *
     * @param start      Integer value of a span's positional start offset.
     * @param end        Integer value of a span's positional end offset.
     * @param annotation Annotation string.
     */
    public void addAnnotation (int start, int end, String annotation) {
	this.addHighlight(new Highlight(start, end, annotation));
    };

    
    /**
     * Insert an annotated relation for the snippet view by
     * means of relational participant positions and an annotation string.
     *
     * @param src        Integer value of a span's positional source object.
     * @param target     Integer value of a span's positional target object.
     * @param annotation Annotation string.
     */
    public void addRelation (int src, int target, String annotation) {
	this.addHighlight(new Highlight(src, src, annotation, target));
	int id = identifierNumberCounter--;
	identifierNumber.put(id, target);
	this.addHighlight(new Highlight(target, target, id));
    };


    /**
     * Populate document meta information with information coming from the index.
     *
     * @param doc    Document object.
     * @param field  Primary data field.
     * @param fields Hash object with all supported fields.
     */
    public void populateDocument (Document doc, String field, HashSet<String> fields) {
	this.setField(field);
	this.setPrimaryData( new KorapPrimaryData(doc.get(field)) );
	if (fields.contains("corpusID"))
	    this.setCorpusID(doc.get("corpusID"));
	if (fields.contains("ID"))
	    this.setDocID(doc.get("ID"));
	if (fields.contains("author"))
	    this.setAuthor(doc.get("author"));
	if (fields.contains("textClass"))
	    this.setTextClass(doc.get("textClass"));
	if (fields.contains("title"))
	    this.setTitle(doc.get("title"));
	if (fields.contains("subTitle"))
	    this.setSubTitle(doc.get("subTitle"));
	if (fields.contains("pubDate"))
	    this.setPubDate(doc.get("pubDate"));
	if (fields.contains("pubPlace"))
	    this.setPubPlace(doc.get("pubPlace"));

	// Temporary (later meta fields in term vector)
	if (fields.contains("foundries"))
	    this.setFoundries(doc.get("foundries"));
	if (fields.contains("tokenization"))
	    this.setTokenization(doc.get("tokenization"));
	if (fields.contains("layerInfo"))
	    this.setLayerInfo(doc.get("layerInfo"));
    };


    /**
     * Get document id.
     */
    @JsonProperty("docID")
    public String getDocID () {
	return super.getID();
    };

    
    /**
     * Set document id.
     *
     * @param id String representation of document ID.
     */
    public void setDocID (String id) {
	super.setID(id);
    };


    /**
     * Set version of the index
     */
    @JsonIgnore
    public String getVersion () {
	if (this.version == null)
	    return null;
	StringBuilder sb = new StringBuilder("lucene-backend-");
	return sb.append(this.version).toString();
    };


    /**
     * Set version number.
     *
     * @param version The version number of the index as
     *                a string representation.
     */
    @JsonIgnore
    public void setVersion (String version) {
	this.version = version;
    };


    /**
     * Get the positional start offset of the match.
     */
    @JsonIgnore
    public int getStartPos() {
	return this.startPos;
    };


    /**
     * Set the positional start offset of the match.
     *
     * @param pos The positional offset.
     */
    @JsonIgnore
    public void setStartPos(int pos) {
	this.startPos = pos;
    };


    /**
     * Get the positional end offset of the match.
     */
    @JsonIgnore
    public int getEndPos() {
	return this.endPos;
    };


    /**
     * Set the positional end offset of the match.
     *
     * @param pos The positional offset.
     */
    @JsonIgnore
    public void setEndPos(int pos) {
	this.endPos = pos;
    };

    
    /**
     * Get the local (i.e. Lucene given) ID of the document.
     */
    @JsonIgnore
    public int getLocalDocID () {
	return this.localDocID;
    };


    /**
     * Set the local (i.e. Lucene given) ID of the document.
     *
     * @param id The id of the document.
     */
    @JsonIgnore
    public void setLocalDocID (int id) {
	this.localDocID = id;
    };

    
    /**
     * Get the PositionsToOffset object.
     *
     * @see PositionsToOffset
     */
    @JsonIgnore
    public PositionsToOffset getPositionsToOffset () {
	return this.positionsToOffset;
    };


    /**
     * Set the PositionsToOffset object.
     *
     * @param pto The PositionsToOffset object
     * @see PositionsToOffset
     */
    @JsonIgnore
    public void setPositionsToOffset (PositionsToOffset pto) {
	this.positionsToOffset = pto;
    };


    /**
     * Get match ID (for later retrieval).
     *
     * @see MatchIdentifier
     */
    @Override
    @JsonProperty("ID")
    public String getID () {

	// Identifier already given
	if (this.identifier != null)
	    return this.identifier;

	// No, nada, nix
	if (this.localDocID == -1)
	    return null;

	MatchIdentifier id = new MatchIdentifier();

	// Get prefix string corpus/doc
	id.setCorpusID(this.getCorpusID());
	id.setDocID(this.getDocID());
	id.setStartPos(startPos);
	id.setEndPos(endPos);

	// There are highlights to integrate
	if (this.highlight != null) {
	    for (Highlight h : this.highlight) {
		if (h.number >= 256)
		    continue;

		// Add highlight to the snippet
		id.addPos(h.start, h.end, h.number);
	    };
	};

	return (this.identifier = id.toString());
    };


    /**
     * Get identifier for a specific position.
     *
     * @param int Position to get identifier on.
     */
    @JsonIgnore
    public String getPosID (int pos) {

	// Identifier already given
	if (this.identifier != null)
	    return this.identifier;

	// Nothing here
	if (this.localDocID == -1)
	    return null;

	PosIdentifier id = new PosIdentifier();

	// Get prefix string corpus/doc
	id.setCorpusID(this.getCorpusID());
	id.setDocID(this.getDocID());
	id.setPos(pos);

	return id.toString();
    };

    /**
     * Get possible error message.
     */
    // Identical to KorapResult
    public String getError () {
	return this.error;
    };

    /**
     * Set error message.
     *
     * @param msg The error message.
     */
    public void setError (String msg) {
	this.error = msg;
    };


    
    // Reset all internal data
    private void _reset () {
	this.processed       = false;
	this.snippetHTML     = null;
	this.snippetBrackets = null;
	this.identifier      = null;

	// Delete all spans
	if (this.span != null)
	    this.span.clear();
    };

    
    // Start building highlighted snippets
    private boolean _processHighlight () {
	if (processed)
	    return true;

	// Relevant details are missing
	if (this.positionsToOffset == null || this.localDocID == -1) {
	    log.warn("You have to define " +
		     "positionsToOffset and localDocID first " +
		     "before");
	    return false;
	};

	if (DEBUG)
	    log.trace("--- Start highlight processing ...");

	// Get pto object
	PositionsToOffset pto = this.positionsToOffset;
	pto.add(this.localDocID, this.getStartPos());
	pto.add(this.localDocID, this.getEndPos() - 1);

	if (DEBUG)
	    log.trace("PTO will retrieve {} & {} (Match boundary)",
		      this.getStartPos(),
		      this.getEndPos());

	// Add all highlights for character retrieval
	if (this.highlight != null) {
	    for (Highlight hl : this.highlight) {
		pto.add(this.localDocID, hl.start);
		pto.add(this.localDocID, hl.end);

		if (DEBUG)
		    log.trace("PTO will retrieve {} & {} (Highlight boundary)",
			      hl.start, hl.end);
	    };
	};
	
	// Get the list of spans for matches and highlighting
	if (this.span == null || this.span.size() == 0) {
	    if (!this._processHighlightSpans(
	            leftTokenContext,
		    rightTokenContext
	       ))
		return false;
	};

	// Create a stack for highlighted elements
	// (opening and closing elements)
	ArrayList<int[]> stack = this._processHighlightStack();

	// The temporary snippet is empty, nothing to do
	if (this.tempSnippet == null) {
	    processed = true;
	    return false;
	};

	// Merge the element stack with the primary textual data
	this._processHighlightSnippet(this.tempSnippet, stack);

	// Match is processed - done
	return (processed = true);
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
	public String toHTML (KorapMatch match, FixedBitSet level, byte[] levelCache) {	    
	    // Opening
	    if (this.type == 1) {
		StringBuilder sb = new StringBuilder();
		if (this.number == -1) {
		    sb.append("<span class=\"match\">");
		}

		else if (this.number < -1) {
		    sb.append("<span xml:id=\"")
		      .append(match.getPosID(
                          identifierNumber.get(this.number)))
		      .append("\">");
		}

		else if (this.number >= 256) {
		    sb.append("<span ");
		    if (this.number < 2048) {
			sb.append("title=\"")
			  .append(annotationNumber.get(this.number))
			  .append('"');
		    }
		    else {
			Relation rel = relationNumber.get(this.number);
			sb.append("xlink:title=\"")
			  .append(rel.annotation)
			  .append('"');
			sb.append(" xlink:type=\"simple\"");
			sb.append(" xlink:href=\"#");
			sb.append(match.getPosID(rel.ref));
			sb.append('"');
		    };
		    sb.append('>');
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
		if (this.number <= -1 || this.number >= 256)
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

		// Match
		if (this.number == -1) {
		    sb.append("[");
		}

		// Identifier
		else if (this.number < -1) {
		    sb.append("{#");
		    sb.append(identifierNumber.get(this.number));
		    sb.append(':');
		}

		// Highlight, Relation, Span
		else {
		    sb.append("{");
		    if (this.number >= 256) {
			if (this.number < 2048)
			    sb.append(annotationNumber.get(this.number));
			else {
			    Relation rel = relationNumber.get(this.number);
			    sb.append(rel.annotation);
			    sb.append('>').append(rel.ref);
			};
			sb.append(':');
		    }
		    else if (this.number != 0)
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

	    // Shouldn't happen
	    if (this.balanceStack.size() == 0) {
		if (DEBUG)
		    log.trace("The balance stack is empty");
		return;
	    };

	    if (DEBUG) {
		StringBuilder sb = new StringBuilder(
		    "Stack for checking with class "
	        );
		sb.append(number).append(" is ");
		for (int s : this.balanceStack) {
		    sb.append('[').append(s).append(']');
		};
		log.trace(sb.toString());
	    };

	    // class number of the last element
	    int eold = this.balanceStack.removeLast();

	    // the closing element is not balanced
	    while (eold != number) {

		// Retrieve last combinator on stack
		lastComb = this.combine.peekLast();

		if (DEBUG)
		    log.trace("Closing element is unbalanced - {} " +
			      "!= {} with lastComb {}|{}|{}",
			      eold,
			      number,
			      lastComb.type,
			      lastComb.number,
			      lastComb.characters);

		// combinator is opening and the number is not equal to the last
		// element on the balanceStack
		if (lastComb.type == 1 && lastComb.number == eold) {

		    // Remove the last element - it's empty and uninteresting!
		    this.combine.removeLast();
		}

		// combinator is either closing (??) or another opener
		else {

		    if (DEBUG)
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

	    if (DEBUG) {
		log.trace("LastComb: " + lastComb.type + '|' + lastComb.number + '|' + lastComb.characters + " for " + number);
		log.trace("Stack for checking 2: {}|{}|{}|{}", lastComb.type, lastComb.number, lastComb.characters, number);
	    };

	    if (lastComb.type == 1 && lastComb.number == number) {
		while (lastComb.type == 1 && lastComb.number == number) {
		    // Remove the damn thing - It's empty and uninteresting!
		    this.combine.removeLast();
		    lastComb = this.combine.peekLast();
		};
	    }
	    else {
		if (DEBUG)
		    log.trace("close element b) {}", number);

		// Add a closer
		this.combine.add(new HighlightCombinatorElement((byte) 2, number));
	    };


	    // Fetch everything from the tempstack and reopen it
	    for (int e : tempStack) {
		if (DEBUG)
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

    private void _processHighlightSnippet (String clean,
					   ArrayList<int[]> stack) {

	if (DEBUG)
	    log.trace("--- Process Highlight snippet");

	int pos = 0,
	    oldPos = 0;

	this.snippetStack = new HighlightCombinator();

	for (int[] element : stack) {
	    pos = element[3] != 0 ? element[0] : element[1];

	    if (pos > oldPos) {

	      if (pos > clean.length()) {
		pos = clean.length() - 1;
	      };

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

	if (!this._processHighlight())
	    return null;

	if (this.processed && this.snippetHTML != null)
	    return this.snippetHTML;

	if (DEBUG)
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
	    sb.append(elem.toHTML(this, level, levelCache));
	    start++;
	};
	sb.append("</span>");

	elem = this.snippetStack.getLast();

	StringBuilder rightContext = new StringBuilder();

	// Create context, if trhere is any
	rightContext.append("<span class=\"context-right\">");
	if (elem != null && elem.type == 0) {
	    rightContext.append(elem.toHTML(this, level, levelCache));
	    end--;
	};
	if (endMore)
	    rightContext.append("<span class=\"more\"></span>");
	rightContext.append("</span>");

	for (short i = start; i < end; i++) {
	    sb.append(this.snippetStack.get(i).toHTML(this, level,levelCache));
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

	if (!this._processHighlight())
	    return null;

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
	if (DEBUG)
	    log.trace("--- Process Highlight stack");

	LinkedList<int[]> openList  = new LinkedList<int[]>();
	LinkedList<int[]> closeList = new LinkedList<int[]>();

	// Add highlight spans to balance lists
	openList.addAll(this.span);
	closeList.addAll(this.span);

	// Sort balance lists
	Collections.sort(openList, new OpeningTagComparator());
	Collections.sort(closeList, new ClosingTagComparator());

	// New stack array
	ArrayList<int[]> stack = new ArrayList<>(openList.size() * 2);

	// Create stack unless both lists are empty
	while (!openList.isEmpty() || !closeList.isEmpty()) {

	    if (openList.isEmpty()) {
		stack.addAll(closeList);
		break;
	    }

	    // Not sure about this, but it can happen
	    else if (closeList.isEmpty()) {
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

    /**
     * This will retrieve character offsets for all spans.
     */
    private boolean _processHighlightSpans (boolean leftTokenContext,
					    boolean rightTokenContext) {

	if (DEBUG)
	    log.trace("--- Process Highlight spans");

	int startOffsetChar,
	    endOffsetChar,
	    startPosChar,
	    endPosChar;

	// Local document ID
	int ldid = this.localDocID;

	// No positionsToOffset object found
	if (this.positionsToOffset == null)
	    return false;

	// Match position
	startPosChar = this.positionsToOffset.start(ldid, this.startPos);

	if (DEBUG)
	    log.trace("Unaltered startPosChar is {}", startPosChar);

	// Check potential differing start characters
	// e.g. from element spans
	if (potentialStartPosChar != -1 &&
	    (startPosChar > potentialStartPosChar))
	    startPosChar = potentialStartPosChar;

	endPosChar = this.positionsToOffset.end(ldid, this.endPos - 1);

	if (DEBUG)
	    log.trace("Unaltered endPosChar is {}", endPosChar);

	// Potential end characters may come from spans with
	// defined character offsets like sentences including .", ... etc.
	if (endPosChar < potentialEndPosChar)
	    endPosChar = potentialEndPosChar;

	if (DEBUG)
	    log.trace("Refined: Match offset is pos {}-{} (chars {}-{})",
		      this.startPos,
		      this.endPos,
		      startPosChar,
		      endPosChar);

	// left context
	if (leftTokenContext) {
	    if (DEBUG)
		log.trace("PTO will retrieve {} (Left context)",
			  this.startPos - this.leftContextOffset);

	    startOffsetChar = this.positionsToOffset.start(
	      ldid,
	      this.startPos - this.leftContextOffset
	    );
	}
	else {
	    startOffsetChar = startPosChar - this.leftContextOffset;
	};

	// right context
	if (rightTokenContext) {
	    if (DEBUG)
		log.trace("PTO will retrieve {} (Right context)",
			  this.endPos + this.rightContextOffset - 1);

	    endOffsetChar = this.positionsToOffset.end(
	        ldid,
		this.endPos + this.rightContextOffset - 1
	    );
	}
	else {
	    if (endPosChar == -1) {
		endOffsetChar = -1;
	    }
	    else {
		endOffsetChar = endPosChar + this.rightContextOffset;
	    };
	};

	// This can happen in case of non-token characters
	// in the match and null offsets
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

	if (DEBUG)
	    log.trace("The context spans from chars {}-{}",
		      startOffsetChar, endOffsetChar);

	if (endOffsetChar > -1 &&
	    (endOffsetChar < this.getPrimaryDataLength())) {
	    this.tempSnippet = this.getPrimaryData(
	        startOffsetChar,
		endOffsetChar
	    );
	}
	else {
	    this.tempSnippet = this.getPrimaryData(startOffsetChar);
	    // endPosChar = this.tempSnippet.length() - 1 + startOffsetChar;
	    endMore = false;
	};

	if (DEBUG)
	    log.trace("Snippet: '" + this.tempSnippet + "'");

	// No spans yet
	if (this.span == null)
	    this.span = new LinkedList<int[]>();

	this.identifier = null;

	// TODO: Simplify
	int[] intArray = new int[]{
	    startPosChar - startOffsetChar,
	    endPosChar - startOffsetChar,
	    -1,
	    0};

	if (DEBUG)
	    log.trace("The match entry is {}-{} ({}-{}) with startOffsetChar {}",
		      startPosChar - startOffsetChar,
		      endPosChar - startOffsetChar,
		      startPosChar,
		      endPosChar,
		      startOffsetChar);

	// Add match span
	this.span.add(intArray);

	// highlights
	// -- I'm not sure about this.
	if (this.highlight != null) {
	    if (DEBUG)
		log.trace("There are highlights!");

	    for (Highlight highlight : this.highlight) {
		int start = this.positionsToOffset.start(
		  ldid, highlight.start
	        );
		
		int end = this.positionsToOffset.end(
	          ldid,
		  highlight.end
		);

		if (DEBUG)
		    log.trace("PTO has retrieved {}-{} for class {}",
			      start,
			      end,
			      highlight.number);
		
		start -= startOffsetChar;
		end   -= startOffsetChar;
		
		if (start < 0 || end < 0)
		    continue;

		// Create intArray for highlight
		intArray = new int[]{
		    start,
		    end,
		    highlight.number,
		    0 // Dummy value for later
		};

		this.span.add(intArray);
	    };
	};
	return true;
    };


    // Identical to KorapResult!
    public String toJSON () {
	ObjectNode json =  (ObjectNode) mapper.valueToTree(this);

	// Match was no match
	if (json.size() == 0)
	    return "{}";

	ArrayNode leftContext = mapper.createArrayNode();
	leftContext.add(this.leftTokenContext ? "token" : "char");
	leftContext.add(this.leftContextOffset);

	ArrayNode rightContext = mapper.createArrayNode();
	rightContext.add(this.rightTokenContext ? "token" : "char");
	rightContext.add(this.rightContextOffset);

	ObjectNode context = mapper.createObjectNode();
	context.put("left", leftContext);
	context.put("right", rightContext);
	json.put("context", context);

	if (this.version != null)
	    json.put("version", this.getVersion());

	try {
	    return mapper.writeValueAsString(json);
	}
	catch (Exception e) {
	    log.warn(e.getLocalizedMessage());
	};

	return "{}";
    };
};
