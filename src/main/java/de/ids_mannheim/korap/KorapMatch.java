package de.ids_mannheim.korap;
import java.util.*;
import java.io.*;

import java.nio.ByteBuffer;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;

import de.ids_mannheim.korap.index.PositionsToOffset;
import de.ids_mannheim.korap.index.SearchContext;
import de.ids_mannheim.korap.document.KorapPrimaryData;

import de.ids_mannheim.korap.match.HighlightCombinator;
import de.ids_mannheim.korap.match.HighlightCombinatorElement;
import de.ids_mannheim.korap.match.Relation;
import de.ids_mannheim.korap.match.MatchIdentifier;
import de.ids_mannheim.korap.match.PosIdentifier;
import de.ids_mannheim.korap.query.SpanElementQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.util.FixedBitSet;
import org.apache.lucene.util.Bits;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.spans.Spans;

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
    public SearchContext context;

    // Should be deprecated, but used wildly in tests!
    @JsonIgnore
    public int startPos, endPos = -1;

    @JsonIgnore
    public int potentialStartPosChar = -1,
	       potentialEndPosChar   = -1;

    private String error = null;
    private String version;

    // TEMPORARILY
    @JsonIgnore
    public int localDocID = -1;

    private HashMap<Integer, String>   annotationNumber = new HashMap<>(16);
    private HashMap<Integer, Relation> relationNumber   = new HashMap<>(16);
    private HashMap<Integer, Integer>  identifierNumber = new HashMap<>(16);

    // -1 is match highlight
    int annotationNumberCounter = 256;
    int relationNumberCounter   = 2048;
    int identifierNumberCounter = -2;

    private String tempSnippet,
	           snippetHTML,
	           snippetBrackets,
	           identifier;

    private HighlightCombinator snippetArray;

    public boolean startMore = true,
	           endMore = true;

    private Collection<byte[]> payload;
    private ArrayList<Highlight> highlight;
    private LinkedList<int[]> span;

    private PositionsToOffset positionsToOffset;
    private boolean processed = false;

    /**
     * Constructs a new KorapMatch object.
     * Todo: Maybe that's not necessary!
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
	this.localDocID     = localDocID;
	this.startPos       = startPos;
	this.endPos         = endPos;
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
	if (id.getStartPos() > -1) {
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

    
    // TODO: Here are offsets and highlight offsets!
    // <> payloads have 12 bytes (iii) or 8!?
    // highlightoffsets have 11 bytes (iis)!
    public void addPayload (Collection<byte[]> payload) {
	try {
	    ByteBuffer bb = ByteBuffer.allocate(10);
	    for (byte[] b : payload) {

		if (DEBUG)
		    log.trace("Found a payload with length {}", b.length);

		// Todo element searches!

		// Highlights!
		if (b.length == 9) {
		    bb.put(b);
		    bb.rewind();

		    int start   = bb.getInt();
		    int end     = bb.getInt() -1;
		    byte number = bb.get();

		    if (DEBUG)
			log.trace("Have a payload: {}-{}", start, end);

		    this.addHighlight(start, end, number);
		}

		// Element payload for match!
		// This MAY BE the correct match
		else if (b.length == 8) {
		    bb.put(b);
		    bb.rewind();

		    if (this.potentialStartPosChar == -1) {
			this.potentialStartPosChar = bb.getInt(0);
		    }
		    else {
			if (bb.getInt(0) < this.potentialStartPosChar)
			    this.potentialStartPosChar = bb.getInt(0);
		    };
		    
		    if (bb.getInt(4) > this.potentialEndPosChar)
			this.potentialEndPosChar = bb.getInt(4);

		    if (DEBUG)
			log.trace(
			    "Element payload from {} to {}",
			    this.potentialStartPosChar,
			    this.potentialEndPosChar
			);
		}

		else if (b.length == 4) {
		    bb.put(b);
		    bb.rewind();
		    log.warn("Unknown[4]: {}", bb.getInt());
		};
	
		// Clear bytebuffer
		bb.clear();
	    };
	}

	catch (Exception e) {
	    log.error(e.getMessage());
	}
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
     * Get the positional start offset of the class.
     *
     * @param number Class number of the highlight.
     */
    @JsonIgnore
    public int getStartPos (int number) {
	if (number > 256 || this.highlight == null)
	    return -1;

	// Iterate over highlights to find matching class
	for (Highlight h : this.highlight) {
	    if (h.number == number)
		return h.start;
	};

	return -1;
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
    public int getEndPos () {
	return this.endPos;
    };


    /**
     * Get the positional end offset of the class.
     *
     * @param number Class number of the highlight.
     */
    @JsonIgnore
    public int getEndPos (int number) {
	if (number > 256 || this.highlight == null)
	    return -1;

	// Iterate over highlights to find matching class
	for (Highlight h : this.highlight) {

	    // Get the number (incremented by 1)
	    if (h.number == number)
		return h.end + 1;
	};

	return -1;
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

	MatchIdentifier id = this.getMatchIdentifier();

	// Get prefix string corpus/doc
	id.setCorpusID(this.getCorpusID());
	id.setDocID(this.getDocID());

	return (this.identifier = id.toString());
    };

    @JsonIgnore
    public MatchIdentifier getMatchIdentifier () {
	MatchIdentifier id = new MatchIdentifier();

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

	return id;
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


    public KorapMatch setContext (SearchContext context) {
	this.context = context;
	return this;
    };

    @JsonIgnore
    public SearchContext getContext () {
	if (this.context == null)
	    this.context = new SearchContext();
	return this.context;
    };
    

    // Expand the context to a span
    public int[] expandContextToSpan (String element) {

	// TODO: THE BITS HAVE TO BE SET!
	
	if (this.positionsToOffset != null)
	    return this.expandContextToSpan(
	        this.positionsToOffset.getAtomicReader(),
		(Bits) null,
		"tokens",
		element
	    );
	return new int[]{0,0,0,0};
    };

    // Expand the context to a span
    // THIS IS NOT VERY CLEVER - MAKE IT MORE CLEVER!
    public int[] expandContextToSpan (AtomicReaderContext atomic,
				      Bits bitset,
				      String field,
				      String element) {

	try {
	    // Store character offsets in ByteBuffer
	    ByteBuffer bb = ByteBuffer.allocate(8);

	    SpanElementQuery cquery =
		new SpanElementQuery(field, element);

	    Spans contextSpans = cquery.getSpans(
	        atomic,
		bitset,
		new HashMap<Term, TermContext>()
	    );

	    int newStart = -1,
		newEnd = -1;
	    int newStartChar = -1,
		newEndChar = -1;

	    if (DEBUG)
		log.trace("Extend match to context boundary with {} in {}",
			  cquery.toString(),
			  this.localDocID);

	    while (true) {

		// Game over
		if (contextSpans.next() != true)
		    break;

		if (contextSpans.doc() != this.localDocID) {
		    contextSpans.skipTo(this.localDocID);
		    if (contextSpans.doc() != this.localDocID)
			break;
		};

		// There's a <context> found -- I'm curious,
		// if it's closer to the match than everything before
		if (contextSpans.start() <= this.getStartPos() &&
		    contextSpans.end() >= this.getStartPos()) {

		    // Set as newStart
		    newStart = contextSpans.start() > newStart ?
			contextSpans.start() : newStart;

		    if (DEBUG)
			log.trace("NewStart is at {}", newStart);

		    // Get character offset (start)
		    if (contextSpans.isPayloadAvailable()) {
			try {
			    bb.rewind();
			    for (byte[] b : contextSpans.getPayload()) {

				// Not an element span
				if (b.length != 8)
				    continue;

				bb.put(b);
				bb.rewind();
				newStartChar = bb.getInt();
				newEndChar = bb.getInt();
				break;
			    };
			}
			catch (Exception e) {
			    log.warn(e.getMessage());
			};
		    };
		}
		else {
		    // Has to be resettet to avoid multiple readings of the payload
		    newEndChar = 0;
		};
		
		// There's an s found, that ends after the match
		if (contextSpans.end() >= this.getEndPos()) {
		    newEnd = contextSpans.end();

		    // Get character offset (end)
		    if (newEndChar == 0 && contextSpans.isPayloadAvailable()) {
			try {
			    bb.rewind();
			    for (byte[] b : contextSpans.getPayload()) {

				// Not an element span
				if (b.length != 8)
				    continue;

				bb.put(b);
				bb.rewind();
				newEndChar = bb.getInt(1);
				break;
			    };
			}
			catch (Exception e) {
			    log.warn(e.getMessage());
			};
		    };
		    break;
		};
	    };
	    
	    // We have a new match surrounding
	    if (DEBUG)
		log.trace("New match spans from {}-{}/{}-{}", newStart, newEnd, newStartChar, newEndChar);

	    return new int[]{newStart, newEnd, newStartChar, newEndChar};
	}
	catch (IOException e) {
	    log.error(e.getMessage());
	};
	
	return new int[]{-1,-1,-1,-1};
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
		if (hl.start >= this.getStartPos() && hl.end <= this.getEndPos()) {
		    pto.add(this.localDocID, hl.start);
		    pto.add(this.localDocID, hl.end);

		    if (DEBUG)
			log.trace("PTO will retrieve {} & {} (Highlight boundary)",
				  hl.start, hl.end);
		};
	    };
	};
	
	// Get the list of spans for matches and highlighting
	if (this.span == null || this.span.size() == 0) {
	    if (!this._processHighlightSpans())
		return false;
	};

	// Create a stack for highlighted elements
	// (opening and closing elements)
	ArrayList<int[]> stack = this._processHighlightStack();

	if (DEBUG)
	    log.trace("The snippet is {}", this.tempSnippet);


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
		if (arg0[1] > arg1[1]) {
		    return -1;
		}
		else if (arg0[1] == arg1[1]) {
		    return 0;
		}
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
		if (arg0[0] < arg1[0]) {
		    return 1;
		}
		else if (arg0[0] == arg1[0]) {
		    return 0;
		};
		return -1;
	    };
	    return -1;
	};
    };



    private void _processHighlightSnippet (String clean,
					   ArrayList<int[]> stack) {

	if (DEBUG)
	    log.trace("--- Process Highlight snippet");

	int pos = 0, oldPos = 0;

	this.snippetArray = new HighlightCombinator();

	for (int[] element : stack) {
	    pos = element[3] != 0 ? element[0] : element[1];

	    if (pos > oldPos) {

	      if (pos > clean.length()) {
		pos = clean.length() - 1;
	      };

		snippetArray.addString(clean.substring(oldPos, pos));

		oldPos = pos;
	    };

	    if (element[3] != 0) {
		snippetArray.addOpen(element[2]);
	    }
	    else {
		snippetArray.addClose(element[2]);
	    };
	};

	if (clean.length() > pos) {
	    snippetArray.addString(clean.substring(pos));
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

	// Snippet stack sizes
	short start = (short) 0;
	short end = this.snippetArray.size();
	end--;

	// Set levels for highlights 
	FixedBitSet level = new FixedBitSet(16);
	level.set(0, 15);
	byte[] levelCache = new byte[16];

	// First element of sorted array
	HighlightCombinatorElement elem = this.snippetArray.getFirst();

	// Create context
	sb.append("<span class=\"context-left\">");
	if (this.startMore)
	    sb.append("<span class=\"more\"></span>");

	// First element is textual
	if (elem.type == 0) {
	    sb.append(elem.toHTML(this, level, levelCache));
	    // Move start position
	    start++;
	};
	sb.append("</span>");

	// Last element of sorted array
	elem = this.snippetArray.getLast();

	StringBuilder rightContext = new StringBuilder();

	// Create right context, if there is any
	rightContext.append("<span class=\"context-right\">");

	// Last element is textual
	if (elem != null && elem.type == 0) {
	    rightContext.append(elem.toHTML(this, level, levelCache));

	    // decrement end
	    end--;
	};
	if (this.endMore)
	    rightContext.append("<span class=\"more\"></span>");
	rightContext.append("</span>");

	// Iterate through all remaining elements
	for (short i = start; i <= end; i++) {
	    sb.append(this.snippetArray.get(i).toHTML(this, level,levelCache));
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

	if (this.startMore)
	    sb.append("... ");

	for (HighlightCombinatorElement hce : this.snippetArray.list()) {
	    sb.append(hce.toBrackets(this));
	};

	if (this.endMore)
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

	// Filter multiple identifiers, that may be introduced and would
	// result in invalid xml
	this._filterMultipleIdentifiers();

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
    private boolean _processHighlightSpans () {

	if (DEBUG)
	    log.trace("--- Process Highlight spans");

	// Local document ID
	int ldid = this.localDocID;

	int startPosChar = -1, endPosChar = -1;

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
	    (startPosChar > this.potentialStartPosChar))
	    startPosChar = this.potentialStartPosChar;

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

	this.identifier = null;

	// No spans yet
	if (this.span == null)
	    this.span = new LinkedList<int[]>();

	// Process offset char findings
	int[] intArray = this._processOffsetChars(ldid, startPosChar, endPosChar);

	// Recalculate startOffsetChar
	int startOffsetChar = startPosChar - intArray[0];

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


    // Pass the local docid to retrieve character positions for the offset
    private int[] _processOffsetChars (int ldid, int startPosChar, int endPosChar) {

	int startOffsetChar = -1, endOffsetChar = -1;
	int startOffset = -1, endOffset = -1;

	// The offset is defined by a span
	if (this.getContext().isSpanDefined()) {

	    if (DEBUG)
		log.trace("Try to expand to <{}>",
			  this.context.getSpanContext());

	    this.startMore = false;
	    this.endMore = false;

	    int [] spanContext = this.expandContextToSpan(
	        this.positionsToOffset.getAtomicReader(),
	        (Bits) null,
	        "tokens",
	        this.context.getSpanContext()
	    );
	    startOffset = spanContext[0];
	    endOffset = spanContext[1];
	    startOffsetChar = spanContext[2];
	    endOffsetChar = spanContext[3];
	    if (DEBUG)
		log.trace("Got context is based from span {}-{}/{}-{}",
			  startOffset, endOffset, startOffsetChar, endOffsetChar);
	};

	// The offset is defined by tokens or characters
	if (endOffset == -1) {

	    PositionsToOffset pto = this.positionsToOffset;
	    
	    // The left offset is defined by tokens
	    if (this.context.left.isToken()) {
		startOffset = this.startPos - this.context.left.getLength();
		if (DEBUG)
		    log.trace("PTO will retrieve {} (Left context)", startOffset);
		pto.add(ldid, startOffset);
	    }

	    // The left offset is defined by characters
	    else {
		startOffsetChar = startPosChar - this.context.left.getLength();
	    };

	    // The right context is defined by tokens
	    if (this.context.right.isToken()) {
		endOffset = this.endPos + this.context.right.getLength() -1;
		if (DEBUG)
		    log.trace("PTO will retrieve {} (Right context)", endOffset);
		pto.add(ldid, endOffset);

	    }

	    // The right context is defined by characters
	    else {
		endOffsetChar = (endPosChar == -1) ? -1 :
		    endPosChar + this.context.right.getLength();
	    };

	    if (startOffset != -1)
		startOffsetChar = pto.start(ldid, startOffset);

	    if (endOffset != -1)
		endOffsetChar = pto.end(ldid, endOffset);
	};

	if (DEBUG)
	    log.trace("Premature found offsets at {}-{}",
		      startOffsetChar,
		      endOffsetChar);
	

	// This can happen in case of non-token characters
	// in the match and null offsets
	if (startOffsetChar > startPosChar)
	    startOffsetChar = startPosChar;
	else if (startOffsetChar < 0)
	    startOffsetChar = 0;

	// No "..." at the beginning
	if (startOffsetChar == 0)
	    this.startMore = false;

	if (endOffsetChar != -1 && endOffsetChar < endPosChar)
	    endOffsetChar = endPosChar;

	if (DEBUG)
	    log.trace("The context spans from chars {}-{}",
		      startOffsetChar, endOffsetChar);

	// Get snippet information from the primary data
	if (endOffsetChar > -1 &&
	    (endOffsetChar < this.getPrimaryDataLength())) {
	    this.tempSnippet = this.getPrimaryData(
		startOffsetChar,
		endOffsetChar
	    );
	}
	else {
	    this.tempSnippet = this.getPrimaryData(startOffsetChar);
	    this.endMore = false;
	};

	if (DEBUG)
	    log.trace("Snippet: '" + this.tempSnippet + "'");

	if (DEBUG)
	    log.trace("The match entry is {}-{} ({}-{}) with absolute offsetChars {}-{}",
		      startPosChar - startOffsetChar,
		      endPosChar - startOffsetChar,
		      startPosChar,
		      endPosChar,
		      startOffsetChar,
		      endOffsetChar);

	// TODO: Simplify
	return new int[]{
	    startPosChar - startOffsetChar,
	    endPosChar - startOffsetChar,
	    -1,
	    0};
    };
    

    // Identical to KorapResult!
    public String toJSON () {
	ObjectNode json =  (ObjectNode) mapper.valueToTree(this);

	// Match was no match
	if (json.size() == 0)
	    return "{}";

	if (this.context != null)
	    json.put("context", this.getContext().toJSON());

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


    // Remove duplicate identifiers
    // Yeah ... I mean ... why not?
    private void _filterMultipleIdentifiers () {
	ArrayList<Integer> removeDuplicate = new ArrayList<>(10);
	HashSet<Integer> identifiers = new HashSet<>(20);
	for (int i = 0; i < this.span.size(); i++) {
	    // span is an int array: [Start, End, Number, Dummy]
	    int highlightNumber = this.span.get(i)[2];

	    // Number is an identifier
	    if (highlightNumber < -1) {

		// Get the real identifier
		int idNumber = identifierNumber.get(highlightNumber);
		if (identifiers.contains(idNumber)) {
		    removeDuplicate.add(i);
		}
		else {
		    identifiers.add(idNumber);
		};
	    };
	};

	// Order the duplicates to filter from the tail
	Collections.sort(removeDuplicate);
	Collections.reverse(removeDuplicate);

	// Delete all duplicate identifiers
	for (int delete : removeDuplicate) {
	    this.span.remove(delete);
	};
    };


    /*
     * Get identifier based on class number
     */
    public int getClassID (int nr) {
	return this.identifierNumber.get(nr);
    };

    /*
     * Get annotation based on id
     */
    public String getAnnotationID (int nr) {
	return this.annotationNumber.get(nr);
    };


    /*
     * Get relation based on id
     */
    public Relation getRelationID (int nr) {
	return this.relationNumber.get(nr);
    };
};
