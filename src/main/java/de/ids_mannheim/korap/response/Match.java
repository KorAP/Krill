package de.ids_mannheim.korap.response;

import static de.ids_mannheim.korap.util.KrillByte.unsignedByte;
import static de.ids_mannheim.korap.util.KrillString.codePointSubstring;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.FixedBitSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.ids_mannheim.korap.index.AbstractDocument;
import de.ids_mannheim.korap.index.PositionsToOffset;
import de.ids_mannheim.korap.query.SpanElementQuery;
import de.ids_mannheim.korap.response.match.HighlightCombinator;
import de.ids_mannheim.korap.response.match.HighlightCombinatorElement;
import de.ids_mannheim.korap.response.match.MatchIdentifier;
import de.ids_mannheim.korap.response.match.PosIdentifier;
import de.ids_mannheim.korap.response.match.Relation;
import de.ids_mannheim.korap.util.KrillProperties;

/*
 * The snippet building algorithm is quite complicated for now
 * and should probably be refactored.
 * It works like this:
 *
 * 1. For all spans and highlights, pagebreaks etc. all necessary
 *    positions are collected (processHighlight)
 * 2. For all collected positions the character offsets are retrieved
 *    and based on that for all spans and highlights a list
 *    is created with arrays of the spans with the structure
 *    [startchar, endchar, highlightClass] (processHighlightSpans)
 *    2.1 The primary data and optional context information is retrieved
 *        (processOffsetChars)
 * 3. Based on the collected spans 2 lists are created for opening and
 *    closing tags (pretty much clones of the initial span list),
 *    sorted for opening resp. closing, and processed in parallel
 *    to form an open/close stack. The new structure on the stack is
 *    [startchar, endchar, highlightclass, close=0/open=1/empty=2]
 *    (processHighlightStack)
 *    3.1. If the element is a relation with an identifier, this may
 *         be removed if duplicate (filterMultipleIdentifiers)
 * 4. Based on the stack and the primary data the snippet is created.
 *    (processHighlightSnippet)
 *    4.1. To avoid unbalanced elements, all open/close/empty tags
 *         are balanced (i.e. closed and reopened if overlaps occur).
 *         (Highlightcombinator)
 */

/*
 * Todo: The implemented classes and private names are horrible!
 * Refactor, future-me!
 *
 * The number based Highlighttype is ugly - UGLY!
 *
 * substrings may be out of range - e.g. if snippets are not lifted!
 */

/**
 * Representation of Matches in a Result.
 * <strong>Warning:</strong> This is currently highly dependent
 * on DeReKo data and will change in the future.
 * 
 * @author Nils Diewald
 * @see Result
 */
@JsonInclude(Include.NON_NULL)
public class Match extends AbstractDocument {

    // Logger
    private final static Logger log = LoggerFactory.getLogger(Match.class);

	// end marker of highlights that are pagebreaks
	private static final int PB_MARKER = -99999;
    private static final int ALL_MARKER = -99998;

	// Textual elements that are in context
	private static final int CONTEXT = -99997;

    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;

    // Mapper for JSON serialization
    ObjectMapper mapper = new ObjectMapper();

    // Snippet information
    @JsonIgnore
    public SearchContext context;

    // Public, while used wildly in tests!
    @JsonIgnore
    public int startPos, endPos = -1;

    @JsonIgnore
    private int innerMatchStartPos, innerMatchEndPos = -1;

    @JsonIgnore
    public int potentialStartPosChar = -1, potentialEndPosChar = -1;

	@JsonIgnore
	public boolean startCutted = false, endCutted = false;

    private String version;

    // TEMPORARILY
    @JsonIgnore
    public int localDocID = -1;

    private HashMap<Integer, String> annotationNumber = new HashMap<>(16);
    private HashMap<Integer, Relation> relationNumber = new HashMap<>(16);
    private HashMap<Integer, String> identifierNumber = new HashMap<>(16);

    // -1 is match highlight
    int annotationNumberCounter = 256;
    int relationNumberCounter = 2048;
    int identifierNumberCounter = -2;

	private int startPage = -1;
	private int endPage = -1;
	
    private String tempSnippet,
		snippetHTML,
		snippetBrackets,
		identifier,
		mirrorIdentifier;

    private ObjectNode snippetTokens;
    
    private HighlightCombinator snippetArray;

    public boolean hasSnippet = false;
    public boolean hasTokens = false;

    
    @JsonIgnore
    public boolean startMore = true, endMore = true;

//    private Collection<byte[]> payload;
    private ArrayList<Highlight> highlight;
    private LinkedList<int[]> span;

    private PositionsToOffset positionsToOffset;
    private boolean processed = false;
    
    /**
     * Constructs a new Match object.
     * Todo: Maybe that's not necessary!
     * 
     * @param maxTokenMatchSize
     *            The maximum number of tokens a match may have
     * @param pto
     *            The PositionsToOffset object, containing relevant
     *            positional information for highlighting
     * @param localDocID
     *            Document ID based on the atomic reader.
     * @param startPos
     *            Start position of the match in the document.
     * @param endPos
     *            End position of the match in the document.
     * 
     * @see #snippetHTML()
     * @see #snippetBrackets()
     * @see PositionsToOffset
     */
    public Match (int maxTokenMatchSize, PositionsToOffset pto,
                  int localDocID, int startPos, int endPos) {
        this.positionsToOffset = pto;
        this.localDocID = localDocID;
        this.setStartPos(maxTokenMatchSize, startPos);
        this.setEndPos(maxTokenMatchSize, endPos);
    };


    /**
     * Constructs a new Match object.
     */
    public Match () {};


    /**
     * Constructs a new Match object.
     * 
     * @param idString
     *            Match identifier string as provided by Result.
     * @param includeHighlights
     *            Boolean value indicating if possible provided
     *            highlight information should be ignored or not.
     */
    public Match (int maxTokenMatchSize, String idString, boolean includeHighlights) {
        MatchIdentifier id = new MatchIdentifier(idString);

        if (id.getTextSigle() == "" && id.getDocID() == "")
            return;
        
        if (id.getStartPos() > -1) {
			this.mirrorIdentifier = id.toString();

            if (id.getTextSigle() != null)
                this.addString("textSigle", id.getTextSigle());

            // <legacy>
            this.addString("corpusID", id.getCorpusID());
            this.addString("ID", id.getDocID());
            // </legacy>

            this.setStartPos(maxTokenMatchSize, id.getStartPos());
            this.setEndPos(maxTokenMatchSize, id.getEndPos());

            if (includeHighlights) {
                for (int[] pos : id.getPos()) {
                    if (pos[0] < id.getStartPos() || pos[1] > id.getEndPos())
                        continue;
                    this.addHighlight(pos[0], pos[1], pos[2]);
				};
            };
        };
    };

    /**
     * Private class of highlights.
	 * TODO: This should probably be renamed, as it not only contains highlights
	 * but also annotations, markers, pagebreaks and relations
     */
    private class Highlight {
        public int start, end;
        public int number = -1;

        // Relational highlight
        public Highlight (int start, int end, String annotation, int refStart, int refEnd) {
            this.start = start;
            this.end = end;
            // TODO: This can overflow!
            this.number = relationNumberCounter++;

			if (DEBUG) {
				log.trace("Add relation (2) '{}': source={}-{} >> target={}-{}",
						  annotation, start, end, refStart, refEnd);
			};
			
            relationNumber.put(this.number, new Relation(annotation, refStart, refEnd));
        };


        // Span highlight
        public Highlight (int start, int end, String annotation) {
            this.start = start;
            this.end = end;

            // TODO: This can overflow!
            if (annotationNumberCounter < 2048) {
                this.number = annotationNumberCounter++;
                annotationNumber.put(this.number, annotation);
            };
        };


        // Simple highlight
        public Highlight (int start, int end, int number) {
            this.start = start;
            this.end = end;
            this.number = number;
        };

		// Pagebreak
		public Highlight (int start, int pagenumber) {
			this.start = start;
			this.end = PB_MARKER;
			this.number = pagenumber;
		};

		// Marker
		public Highlight (int start, String marker) {
			this.start = start;
			this.end = ALL_MARKER;

            // TODO: This can overflow!
            if (annotationNumberCounter < 2048) {
                this.number = annotationNumberCounter++;
                annotationNumber.put(this.number, marker);
            };
		};

    };


    // TODO: Here are offsets and highlight offsets!
    // <> payloads have 12 bytes (iii) or 8!?
    // highlightoffsets have 11 bytes (iis)!
    public void addPayload (List<byte[]> payload) {

        if (DEBUG)
            log.trace("Add payloads to match");

        // Reverse to make embedding of highlights correct
        Collections.reverse(payload);
        try {

            ByteBuffer bb = ByteBuffer.allocate(24);

            // TODO: REVERSE ITERATOR!
            for (byte[] b : payload) {

                if (DEBUG)
                    log.trace("Found a payload of pti {}", b[0]);

                // Todo element searches!

                // Highlights! This is a class PTI
                if (b[0] == 0) {
                    bb.put(b);
                    bb.position(1); // Ignore PTI
                    int start = bb.getInt();
                    int end = bb.getInt();
                    byte number = bb.get();

                    if (DEBUG)
                        log.trace(
                                "Have a highlight of class {} in {}-{} inside of {}-{}",
                                unsignedByte(number), start, end,
                                this.getStartPos(), this.getEndPos());

                    // Ignore classes out of match range and set by the system
                    // TODO: This may be decidable by PTI!
                    if (unsignedByte(number) <= 128
                            && start >= this.getStartPos()
                            && end <= this.getEndPos()) {

                        if (DEBUG) {
                            log.trace("Add highlight with class/relationnr {}!",
                                    unsignedByte(number));
                        };

                        this.addHighlight(start, end - 1, number);
                    }
                    else if (DEBUG) {
                        log.trace("Don't add highlight of class {}!",
                                unsignedByte(number));
                    };
                }

                // Element payload for match!
                // This MAY BE the correct match
                else if (b[0] == (byte) 64) {

                    bb.put(b);
                    bb.position(1); // Ignore pti

                    // Wasn't set before
                    if (this.potentialStartPosChar == -1) {
                        this.potentialStartPosChar = bb.getInt(1);
                    }
                    else {
                        if (bb.getInt(0) < this.potentialStartPosChar)
                            this.potentialStartPosChar = bb.getInt(1);
                    };

                    if (bb.getInt(4) > this.potentialEndPosChar && !this.endCutted)
                        this.potentialEndPosChar = bb.getInt(5);

                    if (DEBUG)
                        log.trace("Element payload from {} to {}",
                                this.potentialStartPosChar,
                                this.potentialEndPosChar);
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
     * @param start
     *            Integer value of a span's positional start offset.
     * @param end
     *            Integer value of a span's positional end offset.
     * @param number
     *            Optional class number of the highlight.
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
     * @param hl
     *            A highlight object to add to the match.
     */
    public void addHighlight (Highlight hl) {

        if (this.highlight == null)
            this.highlight = new ArrayList<Highlight>(16);

        if (DEBUG)
            log.trace("Add highlight from pos {}-{} of class {}", hl.start,
                    hl.end, hl.number);

        // Reset the fetched match data
        this._reset();

        this.highlight.add(hl);
    };


    /**
     * Insert a textual annotation for the snippet view by
     * means of positional offsets and an annotation string.
     * 
     * @param start
     *            Integer value of a span's positional start offset.
     * @param end
     *            Integer value of a span's positional end offset.
     * @param annotation
     *            Annotation string.
     */
    public void addAnnotation (int start, int end, String annotation) {

		if (DEBUG && start > end)
			log.warn("Annotation span is negative: {}, {} for {}", start, end, annotation);

        this.addHighlight(new Highlight(start, end, annotation));
    };


    /**
     * Insert an annotated relation for the snippet view by
     * means of relational participant positions and an annotation
     * string.
     * 
     * @param src
     *            Integer value of a span's positional source object.
     * @param target
     *            Integer value of a span's positional target object.
     * @param annotation
     *            Annotation string.
     */
    public void addRelation (int srcStart,
							 int srcEnd,
							 int targetStart,
							 int targetEnd,
							 String annotation) {

		if (DEBUG)
			log.trace("Add relation (1) '{}': source={}-{} >> target={}-{}",
					  annotation, srcStart, srcEnd, targetStart, targetEnd);

		// Add source token
		if (srcEnd == -1) { // || srcStart == srcEnd) {
			this.addHighlight(
				new Highlight(srcStart, srcStart, annotation, targetStart, targetEnd)
				);
		}
		// Add source span
		else {
			this.addHighlight(
				new Highlight(srcStart, srcEnd, annotation, targetStart, targetEnd)
				);
		};

        int id = identifierNumberCounter--;

		// Here is probably the problem: the identifier-number
		// needs to incorporate targetEnd as well

		// Add target token
		// (The last part was previously commented
		// out for unknown reason)
		if (targetEnd == -1 || targetStart == targetEnd) {
			this.addHighlight(new Highlight(targetStart, targetStart, id));

			identifierNumber.put(id, String.valueOf(targetStart));
		}

		// Add target span
		else {
			this.addHighlight(new Highlight(targetStart, targetEnd, id));
			identifierNumber.put(id, targetStart + "-" + targetEnd);

		};
    };

	public void addPagebreak (int start, int pagenumber) {
		this.addHighlight(new Highlight(start, pagenumber));
	};

	public void addMarker (int start, String data) {
		this.addHighlight(new Highlight(start, data));
	};


    /**
     * Get document id.
     */
    @JsonProperty("docID")
    public String getDocID () {
        return super.getID();
    };


	/**
	 * Get start page.
	 */
    @JsonIgnore
	public int getStartPage () {
		return this.startPage;
	};

	
	/**
	 * Get end page.
	 */
    @JsonIgnore
	public int getEndPage () {
		return this.endPage;
	};


    /**
     * Get the positional start offset of the match.
     */
    @JsonIgnore
    public int getStartPos () {
        return this.startPos;
    };


    /**
     * Get the positional start offset of the class.
     * 
     * @param number
     *            Class number of the highlight.
     */
    @JsonIgnore
    public int getStartPos (int number) {
        if (number > 256 || this.highlight == null)
            return -1;

        // Iterate over highlights to find matching class
        for (Highlight h : this.highlight) {
            if (h.number == number && h.end != PB_MARKER && h.end != ALL_MARKER)
                return h.start;
        };

        return -1;
    };


    /**
     * Set the positional start offset of the match.
     * 
     * @param pos
     *            The positional offset.
     */
    @JsonIgnore
    public void setStartPos (int maxTokenMatchSize, int pos) {
        this.startPos = pos;
		if (this.endPos != -1 && (this.endPos - pos) > maxTokenMatchSize) {
			this.endPos = pos + maxTokenMatchSize;
			this.endCutted = true;
		};
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
     * @param number
     *            Class number of the highlight.
     */
    @JsonIgnore
    public int getEndPos (int number) {
        if (number > 256 || this.highlight == null)
            return -1;

        // Iterate over highlights to find matching class
        for (Highlight h : this.highlight) {

            // Get the number (incremented by 1)
            if (h.number == number && h.end != PB_MARKER)
                return h.end + 1;
        };

        return -1;
    };


    /**
     * Set the positional end offset of the match.
     * 
     * @param pos
     *            The positional offset.
     */
    @JsonIgnore
    public void setEndPos (int maxTokenMatchSize, int pos) {
        if (this.startPos != -1 && (pos - this.startPos) > maxTokenMatchSize) {
			pos = this.startPos + maxTokenMatchSize;
			    this.endCutted = true;
		};
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
     * @param id
     *            The id of the document.
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
     * @param pto
     *            The PositionsToOffset object
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
    @JsonProperty("matchID")
    public String getID () {
		
		// Return identifier as given
        if (this.mirrorIdentifier != null) {
            return this.mirrorIdentifier;
		};

        // Identifier already created
        if (this.identifier != null) {
            return this.identifier;
		};

        // No, nada, nix
        if (this.localDocID == -1)
            return null;


        MatchIdentifier id = this.getMatchIdentifier();

        // Get prefix string corpus/doc
        if (this.getTextSigle() != null) {
            id.setTextSigle(this.getTextSigle());
        }
        // LEGACY
        else {
            id.setCorpusID(this.getCorpusID());
            id.setDocID(this.getDocID());
        };

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
                if (h.number >= 256 || h.end == PB_MARKER || h.end == ALL_MARKER)
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
     * @param int
     *            Position to get identifier on.
     */
	@JsonIgnore
    public String getPosID (int pos) {
		return this.getPosID(pos, -1);
	};


	/**
     * Get identifier for a specific position.
     * 
     * @param String
     *            Start and optional end position to get
	 *            identifier on, separated by a dash.
     */
	@JsonIgnore
    public String getPosID (String pos) {

        if (pos == null) {
            return "";
        };

        String[] startEnd = pos.split("-");
		if (startEnd.length == 2) {
			return this.getPosID(
				Integer.parseInt(startEnd[0]),
				Integer.parseInt(startEnd[1])
				);
		}
		return this.getPosID(Integer.parseInt(startEnd[0]), -1);
	};

	

    /**
     * Get identifier for a specific position.
     * 
     * @param int
     *            Start position to get identifier on.
     * @param int
     *            End position to get identifier on.
     */
    @JsonIgnore
	public String getPosID (int start, int end) {

		if (DEBUG)
			log.trace("Retrieve identifier for position {}-{}", start, end);

        // Identifier already given
        if (this.identifier != null)
            return this.identifier;

        // Nothing here
        if (this.localDocID == -1)
            return null;

        PosIdentifier id = new PosIdentifier();

        // Get prefix string corpus/doc
		// <legacy>
        id.setCorpusID(this.getCorpusID());
        id.setDocID(this.getDocID());
		// </legacy>
        id.setTextSigle(this.getTextSigle());
        id.setStart(start);
        id.setEnd(end);

		if (DEBUG)
			log.trace(
				"Identifier is {} in {} ({}-{}) {}",
				id.toString(),
				this.getTextSigle(),
				this.getCorpusID(),
				this.getDocID(),
				start
				);

        return id.toString();
    };


    public Match setContext (SearchContext context) {
        this.context = context;
        return this;
    };


    @JsonIgnore
    public SearchContext getContext () {
        if (this.context == null)
            this.context = new SearchContext();
        return this.context;
    };

    @JsonIgnore
    public int getLength () {
        return this.getEndPos() - this.getStartPos();
    };  

	
	// Retrieve markers in a certain area
	public List<int[]> retrieveMarkers (String marker) {
		if (this.positionsToOffset != null) {
			return this.retrieveMarkers(
				this.positionsToOffset.getLeafReader(),
				(Bits) null,
				"tokens",
				marker
				);
		};

		return null;
	};

	// Retrieve markers in a certain area
    // THIS IS NOT VERY CLEVER - MAKE IT MORE CLEVER!
    public List<int[]> retrieveMarkers (LeafReaderContext atomic,
										   Bits bitset,
										   String field,
										   String marker) {

		// List of relevant pagebreaks - only used for pagebreak markers!
		List<int[]> pagebreaks = new ArrayList<>(24);

		int charOffset = 0, pagenumber = 0, start = 0;

        int minStartPos = this.getStartPos() - KrillProperties.maxTokenContextSize;
        int maxEndPos = this.getEndPos() + KrillProperties.maxTokenContextSize;

		if (DEBUG) {
            log.debug("=================================");
			log.debug("Retrieve markers between {}-{}",
					  this.getStartPos(),
					  this.getEndPos());
        };
        
		try {

            // Store character offsets in ByteBuffer
            ByteBuffer bb = ByteBuffer.allocate(256);

			// Store last relevant marker in byte array
			byte[] b = null;

			SpanTermQuery stq = new SpanTermQuery(new Term(field, marker));

			if (DEBUG)
				log.trace("Check markers with {}", stq.toString());

			Spans markerSpans = stq.getSpans(
				atomic, bitset, new HashMap<Term, TermContext>()
				);

			// Iterate over all markers
			while (markerSpans.next() == true) {

				if (DEBUG) {
					log.debug("There is a marker at {}/{} and we are at {}",
							  markerSpans.doc(),
							  markerSpans.start(),
                              this.localDocID);
				};
				
				// Current marker is not in the correct document
                if (markerSpans.doc() != this.localDocID) {
                    if (markerSpans.doc() < this.localDocID) {
                        markerSpans.skipTo(this.localDocID);
                        
                        // No pagebreaks in this document
                        if (markerSpans.doc() != this.localDocID)
                            break;
                    }
                    else {
                        break;
                    };
                    continue;
                };

				if (DEBUG)
					log.debug("The marker occurs in the document");

				// There is a marker found - check,
				// if it is in the correct area
				if (markerSpans.start() < minStartPos) {
                    
					// Only the first payload is relevant
					b = markerSpans.getPayload().iterator().next();
					start = markerSpans.start();

                    if (DEBUG)
						log.debug("Marker start position is before match at {}:{}",
								  markerSpans.start(),
                                  b);
					
				}

				// This captures all markers starting in the potential (i.e. maximum) context of the match
				else {

                    // b is already defined!
                    // This may be due to the last next
					if (b != null) {
						bb.rewind();
						bb.put(b);
						bb.rewind();

						pagenumber = bb.getInt();
                        charOffset = bb.getInt();
                        
                        // This marker is a pagebreak
                        if (pagenumber != 0) {
                            if (DEBUG)
						    	log.debug("Add pagebreak to list: {}-{}", charOffset, pagenumber);
						
						    // Add all pagebreaks for later counting
						    pagebreaks.add(new int[]{charOffset, pagenumber});
                        
						    if (start >= minStartPos) {
    							if (DEBUG)
	    							log.debug("Add marker to rendering: {}-{}",
		    								  charOffset,
			    							  pagenumber);
				    			this.addPagebreak(charOffset, pagenumber);
					    	};
                        }
                        
                        // This marker is no pagebreak
                        else {
                            int bytelength = bb.getInt();
                            byte[] anno = new byte[bytelength];
                            bb.get(anno, 0, bytelength);
                            String annoStr = new String(anno, StandardCharsets.UTF_8);
                            this.addMarker(charOffset, annoStr);
                        }

                        b = null;
					};

					// b wasn't used yet
					if (markerSpans.start() <= maxEndPos) {

						// Set new marker
						// Only the first payload is relevant
						b = markerSpans.getPayload().iterator().next();
						bb.rewind();
						bb.put(b);
						bb.rewind();
							
						pagenumber = bb.getInt();
						charOffset = bb.getInt();
                       
                        // This marker is a pagebreak
                        if (pagenumber != 0) {
                            if (DEBUG)
						    	log.debug("Add pagebreak to list: {}-{}", charOffset, pagenumber);
						
						    // This is the first pagebreak!
						    pagebreaks.add(new int[]{charOffset, pagenumber});
                        
						    if (start >= minStartPos) {

                                
    							if (DEBUG)
	    							log.debug("Add pagebreak to rendering: {}-{}",
		    								  charOffset,
			    							  pagenumber);
				    			this.addPagebreak(charOffset, pagenumber);
					    	};
                        }

                        // This marker is no pagebreak
                        else {
                            int bytelength = bb.getInt();

                            byte[] anno = new byte[bytelength];
                            bb.get(anno);
                            String annoStr = new String(anno, StandardCharsets.UTF_8);
                            this.addMarker(charOffset, annoStr);
                        }

                        b = null;
					}

					// Pagebreak beyond the current position
					else {
						break;
					};
				};
			};

            // That's identical to the above approach and should only occur once
            if (b != null) {
                bb.rewind();
                bb.put(b);
                bb.rewind();

                pagenumber = bb.getInt();
                charOffset = bb.getInt();

                // This marker is a pagebreak
                if (pagenumber != 0) {

                    if (DEBUG)
                        log.debug("Add pagebreak to list: {}-{}", charOffset, pagenumber);
						
                    // This is a remembered pagebreak!
                    pagebreaks.add(new int[]{charOffset, pagenumber});

                    if (start >= minStartPos) {
                                            
                        if (DEBUG)
                            log.debug("Add pagebreak to rendering: {}-{}",
                                      charOffset,
                                      pagenumber);
                        this.addPagebreak(charOffset, pagenumber);
                    };
                }
                // This marker is no pagebreak
                else {
                    int bytelength = bb.getInt();
                    
                    byte[] anno = new byte[bytelength];
                    bb.get(anno);
                    String annoStr = new String(anno, StandardCharsets.UTF_8);
                    this.addMarker(charOffset, annoStr);
                }
                
                b = null;
            };
		}
		catch (Exception e) {
			log.warn("Some problems with ByteBuffer: {}", e.getMessage());
		};

        // For references calculate the page for the match
		if (pagebreaks.size() > 0) {
            int i = 0;
            for (; i < pagebreaks.size(); i++) {
                if (pagebreaks.get(i)[0] <= this.getStartPos()) {
                    this.startPage = pagebreaks.get(i)[1];
                } else {
                    // i++;
                    break;
                };
            };
            for (; i < pagebreaks.size(); i++) {
                if (pagebreaks.get(i)[0] < this.getEndPos()) {
                    this.endPage = pagebreaks.get(i)[1];
                } else {
                    break;
                };
            };
		};
		
		return pagebreaks;
	};

    // Expand the context to a span
    public void expandContextToSpan (String element) {

        // TODO: THE BITS HAVE TO BE SET!

        int[] spanContext = new int[] { 0, 0, 0, 0 };
        
        if (this.positionsToOffset != null) {
            spanContext = this.expandContextToSpan(
                    this.positionsToOffset.getLeafReader(), (Bits) null,
                    "tokens", element);
        }
        
        if (spanContext[0] >= 0
                && spanContext[0] < spanContext[1]) {
            
            int maxExpansionSize = KrillProperties.maxTokenMatchSize;
            if (KrillProperties.matchExpansionIncludeContextSize) {
                maxExpansionSize += KrillProperties.maxTokenContextSize;
            }

            // Match needs to be cutted!
            boolean cutExpansion = false;
            if ((spanContext[1] - spanContext[0]) > maxExpansionSize) {
                cutExpansion=true;
                int contextLength = maxExpansionSize - this.getLength();
                int halfContext = contextLength / 2;

                // This is the extended context calculated
                int realLeftLength = this.getStartPos() - spanContext[0];

                // The length is too large - cut!
                if (realLeftLength > halfContext) {
                    this.startCutted = true;
                    spanContext[0] = this.getStartPos() - halfContext;
                }
                
                int realRightLength = spanContext[1] - this.getEndPos();
                
                // The length is too large - cut!
                if (realRightLength > halfContext) {
                    this.endCutted = true;
                    spanContext[1] = this.getEndPos() + halfContext;
                }
            }

            this.setStartPos(maxExpansionSize,spanContext[0]);
            this.setEndPos(maxExpansionSize,spanContext[1]);
            // EM: update char offsets
            
            if (cutExpansion) {
                this.positionsToOffset.add(localDocID, startPos);
                this.positionsToOffset.add(localDocID, endPos);
                
                int start = this.positionsToOffset.start(localDocID, startPos);
                int end = this.positionsToOffset.start(localDocID, endPos)-1;
                spanContext[2] = start; //spanContext[2];
                spanContext[3] = end; // spanContext[3];
            }

            this.potentialStartPosChar = spanContext[2];
            this.potentialEndPosChar = spanContext[3];
            this.startMore = false;
            this.endMore = false;
            
            this.positionsToOffset.clear();
        }
        else {
            this.addMessage(651, "Unable to extend context");
        };
    };

	

    // Expand the context to a span
    // THIS IS NOT VERY CLEVER - MAKE IT MORE CLEVER!
    public int[] expandContextToSpan (LeafReaderContext atomic, Bits bitset,
            String field, String element) {

        try {
            // Store character offsets in ByteBuffer
            ByteBuffer bb = ByteBuffer.allocate(24);

            SpanElementQuery cquery = new SpanElementQuery(field, element);

            Spans contextSpans = cquery.getSpans(atomic, bitset,
                    new HashMap<Term, TermContext>());

            int newStart = -1, newEnd = -1;
            int newStartChar = -1, newEndChar = -1;

            if (DEBUG)
                log.trace(
                        "Extend match to context boundary with {} in docID {}",
                        cquery.toString(), this.localDocID);

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
                if (contextSpans.start() <= this.getStartPos()
                        && contextSpans.end() >= this.getStartPos()) {

                    // Set as newStart
                    newStart = contextSpans.start() > newStart
                            ? contextSpans.start() : newStart;

                    if (DEBUG)
                        log.trace("NewStart is at {}", newStart);

                    // Get character offset (start)
                    if (contextSpans.isPayloadAvailable()) {
                        try {
                            bb.rewind();
                            for (byte[] b : contextSpans.getPayload()) {

                                // Not an element span
                                if (b[0] != (byte) 64)
                                    continue;

                                bb.rewind();
                                bb.put(b);
                                bb.position(1);
                                newStartChar = bb.getInt();
                                newEndChar = bb.getInt();
                                break;
                            };
                        }
                        catch (Exception e) {
                            log.warn("Some problems with ByteBuffer: {}",
                                     e.getMessage());
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
                                if (b[0] != (byte) 64)
                                    continue;

                                bb.rewind();
                                bb.put(b);
                                bb.position(1);
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
                log.trace("New match spans from {}-{}/{}-{}", newStart, newEnd,
                        newStartChar, newEndChar);

            return new int[] { newStart, newEnd, newStartChar, newEndChar };
        }
        catch (IOException e) {
            log.error(e.getMessage());
        };

        return new int[] { -1, -1, -1, -1 };
    };


    // Reset all internal data
    private void _reset () {
        this.processed = false;
        this.snippetHTML = null;
        this.snippetBrackets = null;
        this.snippetTokens = null;
		this.identifier = null;

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
            if (DEBUG) {
                log.warn("You have to define "
                         + "positionsToOffset and localDocID first before");
            }
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
                    this.getStartPos(), this.getEndPos());

        // Set inner match (ensure it's not added twice)
        if (this.innerMatchEndPos != 1) {
            boolean alreadyHasInnerMatch = false;
            if (this.highlight != null) {
                for (Highlight hl : this.highlight) {
                    if (hl.number == -1 &&
                        hl.start == this.innerMatchStartPos &&
                        hl.end == this.innerMatchEndPos) {
                        alreadyHasInnerMatch = true;
                        break;
                    }
                }
            }

            if (!alreadyHasInnerMatch) {
                this.addHighlight(this.innerMatchStartPos, this.innerMatchEndPos, -1);
            }
        }

        // Add all highlights for character retrieval
        if (this.highlight != null) {
            for (Highlight hl : this.highlight) {
                if (hl.start >= this.getStartPos()
                    && hl.end <= this.getEndPos()) {

					// Highlight is no pagebreak
					if (hl.end != PB_MARKER && hl.end != ALL_MARKER) {
						pto.add(this.localDocID, hl.start);
						pto.add(this.localDocID, hl.end);

						if (DEBUG)
							log.trace(
                                "PTO will retrieve offsets from token {} & {} (Highlight boundary)",
                                hl.start, hl.end);
						
					}

					else if (DEBUG) {
						log.trace("Highlight is a pagebreak or marker - do not retrieve PTO");
					};					
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

                int end0 = arg0[1];
                int end1 = arg1[1];            

                if (arg0[1] == PB_MARKER || arg0[1] == ALL_MARKER) {
                    end0 = arg0[0];
                };
                if (arg1[1] == PB_MARKER || arg1[1] == ALL_MARKER) {
                    end1 = arg1[0];
                };
                
                // Check endpositions
                if (end0 > end1) {
                    return -1;
                }
                else if (end0 == end1) {

                    // Compare class number
                    if (arg0[2] > arg1[2])
                        return 1;
                    else if (arg0[2] < arg1[2])
                        return -1;
                    return 0;

                }
                return 1;
            };
            return -1;
        };
    };

    /*
     * Comparator class for closing tags
     */
    private class ClosingTagComparator implements Comparator<int[]> {
        @Override
        public int compare (int[] arg0, int[] arg1) {

            int end0 = arg0[1];
            int end1 = arg1[1];
            
            if (arg0[1] == PB_MARKER || arg0[1] == ALL_MARKER) {
                end0 = arg0[0];
            };

            if (arg1[1] == PB_MARKER || arg1[1] == ALL_MARKER) {
                end1 = arg1[0];
            };
            
            // Check end positions
            if (end0 > end1) {
                return 1;
            }
            else if (end0 == end1) {

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


    /*
     * This takes a clean string and the tag stack
     * to decorate the string with annotations.
     */
    private void _processHighlightSnippet (String clean,
            ArrayList<int[]> stack) {

        if (DEBUG) {
            log.trace("--- Process Highlight snippet");
            log.trace("--- Snippet: {}", clean);
		};

        int pos = 0, oldPos = 0;
        boolean exceeded = false;

        this.snippetArray = new HighlightCombinator();

        // The snippetArray can have preceeding and following pagebreaks
        // and markers that need to be removed

        
        // Iterate over all elements of the stack
        for (int[] element : stack) {

            // The position is the start position for opening and
			// empty/marker elements and the end position for closing elements
            pos = element[3] != 0 ? element[0] : element[1];

			if (DEBUG) {
				log.trace("Check tag at position {} (was {}) [{},{},{},{}]",
						  pos,
						  oldPos,
						  element[0],
						  element[1],
                          element[2],
                          element[3]);
            };
			
			// The new position is behind the old position
            if (pos > oldPos) {
                
				// The position is behind the string length,
				// which may end when an element ends beyond
                if (pos > clean.length()) {

					// Reposition to the end
                    pos = clean.length();

					if (DEBUG)
						log.trace("Position exceeds string, now {}", pos);

                    exceeded = true;
                };

				// Add partial string
				if (pos > 0 && pos > oldPos) {
                    if (DEBUG)
                        log.trace("Add string {}", codePointSubstring(clean, oldPos, pos));
					snippetArray.addString(codePointSubstring(clean, oldPos, pos));
				};

				// Remember the new position
				oldPos = pos;
            };

			// close tag
            if (element[3] == 0) {

                if (DEBUG)
                    log.trace("Add closer: {}", element[2]);
                
				// Add close
                snippetArray.addClose(element[2]);
            }

			// empty tag (pagebreak)
			else if (!exceeded && element[3] == 2) {

				// Add Empty (pagebreak)
                snippetArray.addEmpty(element[2]);
			}            

            // empty tag (marker)
            else if (!exceeded && element[3] == 3) {

                // Add Empty (pagebreak)
                snippetArray.addMarker(element[2]);
            } 

            // opening element exceeds primary data
            else if (exceeded) {
                break;     
            }

            // open tag
            else {
                snippetArray.addOpen(element[2]);
            };
        };

        if (clean.length() > pos && pos >= 0) {
            snippetArray.addString(codePointSubstring(clean, pos));
            if (DEBUG)
                log.trace("Add rest string {}", codePointSubstring(clean, pos));
        };
    };

    /*
     * Return the snippet as a list of tokens
     */
    @JsonIgnore
    public ObjectNode getSnippetTokens () {
        ObjectNode json = mapper.createObjectNode();

        if (!this._processHighlight())
            return null;

        if (this.processed && this.snippetTokens != null)
            return this.snippetTokens;
        
        if (DEBUG)
            log.trace("--- Process tokens");
                    
        if (this.positionsToOffset == null || this.localDocID == -1)
            return null;

        PositionsToOffset pto = this.positionsToOffset;
        int ldid = this.localDocID;

        int startContext = -1;
        int endContext = -1;
        int startContextChar = -1;
        int endContextChar = -1;

        int pdl = this.getPrimaryDataLength();
        
        // Get context based on a span definition
        log.info(
            "KWIC tokens begin: spanDefined={} left(token={},len={}) right(token={},len={}) startPos={} endPos={} id={} uid={}",
            this.getContext().isSpanDefined(),
            this.getContext().left.isToken(), this.getContext().left.getLength(),
            this.getContext().right.isToken(), this.getContext().right.getLength(),
            this.getStartPos(), this.getEndPos(), this.getID(), this.getUID()
        );
        if (this.getContext().isSpanDefined()) {

            if (DEBUG)
                log.debug("Context defined by span");
            
            int[] spanContext = this.expandContextToSpan(
                this.positionsToOffset.getLeafReader(), (Bits) null,
                "tokens", this.context.getSpanContext());
            startContext = spanContext[0];
            endContext = spanContext[1];
            startContextChar = spanContext[2];
            endContextChar = spanContext[3];
        }

        // The offset is not yet defined - and defined by tokens
        if (endContext == -1) {

            if (DEBUG)
                log.debug("No context defined by span");

            if (this.context.left.isToken() && this.context.left.getLength() > 0) {
                startContext = this.startPos - this.context.left.getLength();
                if (startContext < 0)
                    startContext = 0;
            };
        
            if (this.context.right.isToken() && this.context.right.getLength() > 0) {
                // Use exclusive bound for endContext to simplify iteration
                endContext = this.endPos + this.context.right.getLength();
            };
        };
       
        if (startContext == -1) {
            startContext = this.startPos;
            if (DEBUG)
                log.debug("Set startContext {}", endContext);
        };

        if (endContext == -1) {
            endContext = this.endPos - 1;
            if (DEBUG)
                log.debug("Set endContext {}", endContext);
        };
        
        // Report raw token window before cap
        int rawLeftLen = (startContext < this.startPos) ? (this.startPos - startContext) : 0;
        int rawMatchLen = (this.endPos > this.startPos) ? (this.endPos - this.startPos) : 0;
        int rawRightLen = (endContext > this.endPos) ? ((endContext == -1 ? this.endPos : endContext) - this.endPos) : 0;
        log.info(
            "KWIC tokens raw: L/M/R={}/{}/{} startCtxTok={} endCtxTok={} id={} uid={}",
            rawLeftLen, rawMatchLen, rawRightLen, startContext, endContext, this.getID(), this.getUID()
        );

        // Enforce total KWIC token cap (left + match + right)
        int kwicMax = KrillProperties.getMaxTokenKwicSize();
        if (kwicMax > 0) {
            // Convert endContext to exclusive bound for iteration ease
            int leftLen = (startContext < this.startPos) ? (this.startPos - startContext) : 0;
            int matchLen = (this.endPos > this.startPos) ? (this.endPos - this.startPos) : 0;
            int rightLen = (endContext > this.endPos) ? (endContext - this.endPos) : 0;
            int total = leftLen + matchLen + rightLen;
            if (DEBUG)
                log.info("KWIC tokens pre-cap: total={} (L/M/R={}/{}/{}) cap={} id={} uid={}",
                         total, leftLen, matchLen, rightLen, kwicMax, this.getID(), this.getUID());

            if (matchLen >= kwicMax) {
                // Cut match to kwicMax, drop all context
                this.endPos = this.startPos + kwicMax;
                this.endCutted = true;
                startContext = this.startPos;
                endContext = this.endPos; // exclusive bound
            }
            else if (total > kwicMax) {
                int toReduce = total - kwicMax;
                int reduceLeft = Math.min((toReduce + 1) / 2, leftLen);
                int reduceRight = Math.min(toReduce - reduceLeft, rightLen);

                int rest = toReduce - (reduceLeft + reduceRight);
                if (rest > 0) {
                    int extraRight = Math.min(rest, rightLen - reduceRight);
                    reduceRight += extraRight;
                    rest -= extraRight;
                }
                if (rest > 0) {
                    int extraLeft = Math.min(rest, leftLen - reduceLeft);
                    reduceLeft += extraLeft;
                    rest -= extraLeft;
                }

                if (DEBUG)
                    log.info("KWIC tokens cap apply: reduceLeft={} reduceRight={} rest={} id={}",
                             reduceLeft, reduceRight, rest, this.getID());
                startContext += reduceLeft;
                endContext -= reduceRight;

                if (rest > 0) {
                    // Trim remaining from match end
                    int newMatchLen = matchLen - rest;
                    if (newMatchLen < 0) newMatchLen = 0;
                    this.endPos = this.startPos + newMatchLen;
                    this.endCutted = true;
                    if (endContext < this.endPos)
                        endContext = this.endPos;
                }
            }
            else {
                // No trimming necessary
                if (DEBUG)
                    log.debug("KWIC cap not reached: total={} ≤ cap={}",
                              leftLen + matchLen + rightLen, kwicMax);
            }
        }
        
        // Retrieve the character offsets for all tokens
        for (int i = startContext; i < endContext; i++) {
            pto.add(ldid, i);
        };

        if (startContextChar == -1)
            startContextChar = pto.start(ldid, startContext);

        if (endContextChar == -1)
            endContextChar = pto.end(ldid, endContext);
            
        if (DEBUG)
            log.debug("Match is {}/{} - {}/{}",startContext,startContextChar,endContext,endContextChar);

        if (endContextChar == -1 || endContextChar == 0 || endContextChar > pdl) {
            this.tempSnippet = this.getPrimaryData(startContextChar);
            // Do not alter endMore here; HTML/Brackets decide based on char offsets
        } else  {
            this.tempSnippet = this.getPrimaryData(startContextChar, endContextChar);
        }

        // Do not alter startMore here; HTML/Brackets decide based on char offsets
        
        Integer[] offsets;
        ArrayNode tokens;
        int i;

        // Create left context token list
        if (startContext < this.startPos) {
            tokens = json.putArray("left");
            for (i = startContext; i < this.startPos; i++) {
                offsets = pto.span(ldid,i);
                if (offsets == null) {
                    continue;
                }
                tokens.add(
                    codePointSubstring(this.tempSnippet,
                                       offsets[0]- startContextChar, offsets[1] - startContextChar)
                    );
            };
        };

        tokens = json.putArray("match");
        for (i = this.startPos; i < this.endPos; i++) {
            offsets = pto.span(ldid,i);
            if (offsets == null) {
                continue;
            }
            tokens.add(
                codePointSubstring(this.tempSnippet,
                                   offsets[0]- startContextChar, offsets[1] - startContextChar)
                );
        };

        // Create right context token list
        if (endContext > this.endPos) {
            tokens = null;
            for (i = this.endPos; i < endContext; i++) {
                offsets = pto.span(ldid,i);
                if (offsets == null) {
                    break;
                };

                if (tokens == null)
                    tokens = json.putArray("right");
                
                tokens.add(
                    codePointSubstring(this.tempSnippet,
                        offsets[0]- startContextChar, offsets[1] - startContextChar)
                    );
            };
        };

        // Add class arrays to JSON
        if (this.highlight != null) {

            ArrayNode classes = null;
            for (Highlight highlight : this.highlight) {

                if (highlight.number < 0 || highlight.number > 255)
                    continue;

                // Highlight is a pagebreak
                if (highlight.end == PB_MARKER || highlight.end == ALL_MARKER)
                    continue;

                if (classes == null)
                    classes = json.putArray("classes");
                
                ArrayNode cls = mapper.createArrayNode();                
                cls.add(highlight.number);
                cls.add(highlight.start - this.startPos);
                cls.add(highlight.end - this.startPos);
                classes.add(cls);
            };
        };

        int finalLeft = json.has("left") ? json.get("left").size() : 0;
        int finalMatch = json.has("match") ? json.get("match").size() : 0;
        int finalRight = json.has("right") ? json.get("right").size() : 0;
        log.info("KWIC tokens post-cap: total={} (L/M/R={}/{}/{}) id={} uid={}",
                 finalLeft + finalMatch + finalRight, finalLeft, finalMatch, finalRight, this.getID(), this.getUID());

        return (this.snippetTokens = json);
    };
    

    @JsonIgnore
    public String getSnippetHTML () {
        // Entry log: Show context and cap (helps verify HTML path executes)
        log.info(
            "Enter getSnippetHTML: id={} uid={} spanDefined={} left(token={},len={}) right(token={},len={}) cap={}",
            this.getID(), this.getUID(), this.getContext().isSpanDefined(),
            this.getContext().left.isToken(), this.getContext().left.getLength(),
            this.getContext().right.isToken(), this.getContext().right.getLength(),
            KrillProperties.getMaxTokenKwicSize()
        );

        // Removed enforced HTML KWIC alignment; default behavior remains

        if (!this._processHighlight()) {
            log.warn("getSnippetHTML: _processHighlight() returned false id={} uid={}", this.getID(), this.getUID());
            return null;
        }

        if (this.processed && this.snippetHTML != null)
            return this.snippetHTML;

        if (DEBUG)
            log.trace("Create HTML Snippet");

        StringBuilder sb = new StringBuilder();
		StringBuilder rightContext = new StringBuilder();

		// Remember ids already defined to
		// have joined elements
		HashSet<String> joins = new HashSet<>(100);

        // Snippet stack sizes
        short start = (short) 0;
        short end = this.snippetArray.size();
        end--;

		// Set levels for highlights 
		FixedBitSet level = new FixedBitSet(255);
		level.set(0, 255);
		byte[] levelCache = new byte[255];

		HighlightCombinatorElement elem;
        
		// Create context
        sb.append("<span class=\"context-left\">");
        if (this.startMore)
            sb.append("<span class=\"more\"></span>");

        // Iterate over the snippet array
        // Start with left context
        int leftTextChars = 0, leftNodes = 0;
		while (end > 0) {

			// Get element of sorted array
			elem = this.snippetArray.get(start);

			// Element is in context - but only markers are allowed!
            // The problem with other elements is, that they may span the whole range
            // around the match, so we have overlaps.
            if (elem.type == 1 || elem.type == 2)
                break;

            // Text or marker
            
            String elemString = elem.toHTML(this, level, levelCache, joins);
            sb.append(elemString);
            if (elem.type == 0 && elem.characters != null)
                leftTextChars += elem.characters.length();
            leftNodes++;

            if (DEBUG)
                log.trace("Add node {}", elemString);

            // Move start position
            start++;
		};

        // end of context
        sb.append("</span>");

        // Iterate through all the match
        sb.append("<span class=\"match\">");

		if (this.startCutted) {
			sb.append("<span class=\"cutted\"></span>");
		};
        int matchTextChars = 0, matchNodes = 0;
        
        for (; start <= end; start++) {
			elem = this.snippetArray.get(start);

			if (elem == null)
                continue;
                
            String elemString = elem.toHTML(
                this, level, levelCache, joins
                );
            if (DEBUG) {
                log.trace("Add node {}", elemString);
            };
            sb.append(elemString);
            if (elem.type == 0 && elem.characters != null)
                matchTextChars += elem.characters.length();
            matchNodes++;

            // The match closes
            if (elem.type == 2 && elem.number == CONTEXT) {
                start++;
                break;
            };
        };

        // Warning! TODO:
        // Check that all elements are closed that are opened at this point
        // and only inline markers
        // can follow in the context!

		if (this.endCutted) {
			sb.append("<span class=\"cutted\"></span>");
		};

        
        sb.append("</span>");


        // There is the right context
        // if (start <= end) {
        sb.append("<span class=\"context-right\">");
        int rightTextChars = 0, rightNodes = 0;

        for (; start <= end; start++) {
            elem = this.snippetArray.get(start);

            if (elem == null)
                continue;
                
            String elemString = elem.toHTML(
                this, level, levelCache, joins
                );
            if (DEBUG) {
                log.trace("Add node {}", elemString);
            };
            sb.append(elemString);
            if (elem.type == 0 && elem.characters != null)
                rightTextChars += elem.characters.length();
            rightNodes++;
        };
        
        if (this.endMore)
            sb.append("<span class=\"more\"></span>");

        // End of context
        sb.append("</span>");

        this.snippetHTML = sb.toString();
        log.info("KWIC HTML append: leftChars={} matchChars={} rightChars={} leftNodes={} matchNodes={} rightNodes={} startCutted={} endCutted={} endMore={} snippetLen={} id={} uid={}",
                 leftTextChars, matchTextChars, rightTextChars, leftNodes, matchNodes, rightNodes,
                 this.startCutted, this.endCutted, this.endMore,
                 (this.snippetHTML != null ? this.snippetHTML.length() : -1),
                 this.getID(), this.getUID());
        // Finalize logging: HTML snippet size and ellipsis flags
        log.info(
            "KWIC HTML finalize: snippetLen={} startMore={} endMore={} containsMoreTag={} id={} uid={}",
            (this.snippetHTML != null ? this.snippetHTML.length() : -1),
            this.startMore, this.endMore,
            (this.snippetHTML != null && this.snippetHTML.contains("class=\"more\"")),
            this.getID(), this.getUID()
        );
        return this.snippetHTML;
    };


    @JsonIgnore
    public String getSnippetBrackets () {

        if (!this._processHighlight())
            return null;

        if (this.processed && this.snippetBrackets != null)
            return this.snippetBrackets;

        // Snippet stack sizes
        short start = (short) 0;
        short end = this.snippetArray.size();
        end--;

        StringBuilder sb = new StringBuilder();

        if (this.startMore)
            sb.append("... ");

        // First element of sorted array
        HighlightCombinatorElement elem = this.snippetArray.getFirst();

        while (end > 0) {

			// Get element of sorted array
			elem = this.snippetArray.get(start);

            if (elem.type == 1 || elem.type == 2) {
                break;
            }
            else {
                sb.append(elem.toBrackets(this));
                start++;
            };
        };

        sb.append("[");

		if (this.startCutted) {
			sb.append("<!>");
		};
        
  
        for (; start <= end; start++) {
			elem = this.snippetArray.get(start);
            
			if (elem == null)
                continue;
            
            sb.append(elem.toBrackets(this));

            // The match closes
            if (elem.type == 2 && elem.number == CONTEXT) {
                start++;
                break;
            };
        };
      
		if (this.endCutted) {
			sb.append("<!>");
		};
        sb.append("]");

        for (; start <= end; start++) {
			elem = this.snippetArray.get(start);
            
			if (elem != null)
				sb.append(elem.toBrackets(this));
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

        LinkedList<int[]> openList = new LinkedList<int[]>();
        LinkedList<int[]> closeList = new LinkedList<int[]>();

        // Filter multiple identifiers, that may be introduced and would
        // result in invalid xml
        this._filterMultipleIdentifiers();

        // the start and end of the snippet is currently stored in span[0]
        // this should be trimmed here!

        // Add highlight spans to balance lists
        openList.addAll(this.span);
        closeList.addAll(this.span);

        // Sort balance lists
        Collections.sort(openList, new OpeningTagComparator());
        Collections.sort(closeList, new ClosingTagComparator());

        if (DEBUG) {
            log.trace("OpenList: {}", openList);
            log.trace("CloseList: {}", closeList);
        };

        // New stack array
        ArrayList<int[]> stack = new ArrayList<>(openList.size() * 2);

        // Create stack unless both lists are empty
        while (!openList.isEmpty() || !closeList.isEmpty()) {

			// Nothing more to open -- close all
            if (openList.isEmpty()) {

				if (DEBUG)
					log.debug("No more open tags -- close all non pagebreaks");

                int pf = closeList.peekFirst()[1];

				if (pf != PB_MARKER && pf != ALL_MARKER) {
                    //closeList.removeFirst();
                    
                    int[] e = closeList.removeFirst().clone();

                    if (DEBUG) {
                        log.trace(
                            "Add close with number {} to stack at {}-{} as {}",
                            e[2], e[0], e[1], e[3]
                            );
                    }
					stack.add(e);
				}
				else {
                    closeList.removeFirst();

                    if (DEBUG)
                        log.debug("Close is pagebreak -- ignore (1)");
				};
                
                continue;
            }

            // Not sure about this, but it can happen
            else if (closeList.isEmpty()) {

                if (DEBUG)
                    log.debug("Closelist is empty");

                int[] e = openList.removeFirst().clone();

				if (e[1] == PB_MARKER || e[1] == ALL_MARKER) {

                    if (e[1] == PB_MARKER) {
                        e[3] =  2;
                    } else {
                        e[3] = 3;
                    };

                    // Mark as empty
                    e[1] = e[0]; // Remove pagebreak marker
                    
                    if (DEBUG)
                        log.trace(
                            "Add pagebreak or marker with {} to stack at {}-{} as {}",
                            e[2], e[0], e[1], e[3]
                            );

                    // Add empty pagebreak
                    stack.add(e);
				};
                
                continue;
            };

            int clpf = closeList.peekFirst()[1];
            int olpf = openList.peekFirst()[1];


            // Closener is pagebreak or marker
            if (clpf == PB_MARKER || clpf == ALL_MARKER) {
                
				if (DEBUG)
					log.debug("Close is pagebreak or a marker -- remove (2)");

				// Remove closing pagebreak
				closeList.removeFirst();
			}

			// Opener is pagebreak or marker
            else if ((olpf == PB_MARKER || olpf == ALL_MARKER) && closeList.peekFirst()[1] >= openList.peekFirst()[0]) {
                
                int[] e = openList.removeFirst().clone();

				// Mark as empty
                e[1] = e[0]; // Remove pagebreak marker

                if (olpf == PB_MARKER) {
                    e[3] =  2;
                } else {
                    e[3] = 3;
                };
                
                if (DEBUG)
					log.trace(
						"Add pagebreak or marker with {} to stack at {}-{} as {}",
						e[2], e[0], e[1], e[3]
						);

                
				// Add empty pagebreak
				stack.add(e);
			}
            
			// check if the opener is smaller than the closener
			else if (openList.peekFirst()[0] < closeList.peekFirst()[1]) {
                
				if (DEBUG)
					log.debug("Open tag starts before close tag ends");

                int[] e = openList.removeFirst().clone();

				// Mark as opener
				e[3] = 1;

				if (DEBUG) {

					//      -1: match
					//    < -1: relation target
					//  -99998: context
					// >= 2048: relation source
					// >=  256: annotation
					
					log.trace(
						"Add open with number {} to stack at {}-{} as {}",
						e[2], e[0], e[1], e[3]
						);
				};

				// Add opener to stack
                stack.add(e);
            }

			else {
				int[] e = closeList.removeFirst();

				if (DEBUG) {
					log.debug("Close ends before next opens or at the same position");

					log.trace(
						"Add close with number {} to stack at {}-{}",
						e[2], e[0], e[1]
						);
				};

				// Add closener to stack
                stack.add(e);
            };
        };
        return stack;
    };


    /**
     * Sometimes the match start and end positions are inside the
     * matching region, e.g. when the match was expanded.
     * This will override the original matching positions
     * And mark the real matching.
     */
    public void overrideMatchPosition (int start, int end) {
        if (DEBUG)
            log.trace("--- Override match position");

        this.innerMatchStartPos = start;
        this.innerMatchEndPos = end;
    };


    /**
     * This will retrieve character offsets for all spans.
     * This includes pagebreaks and markers.
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
        if (potentialStartPosChar != -1
                && (startPosChar > this.potentialStartPosChar))
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
                    this.startPos, this.endPos, startPosChar, endPosChar);

        this.identifier = null;

        // No spans yet
        if (this.span == null)
            this.span = new LinkedList<int[]>();

        // Process offset char findings
        int[] intArray = this._processOffsetChars(ldid, startPosChar,
                endPosChar);

        // Recalculate startOffsetChar
        int startOffsetChar = startPosChar - intArray[0];
        int endRelOffsetChar = intArray[1];

        // Add match span, in case no inner match is defined
        if (this.innerMatchEndPos == -1) {
			if (DEBUG)
				log.debug("Added array to match span with {} (1)", intArray);
            this.span.add(intArray);
		};

		// Add context highlight
        intArray = new int[]{intArray[0], intArray[1], CONTEXT, 0};

		this.span.add(intArray);

        if (DEBUG)
            log.debug("Added array to context span with {} (1)", intArray);

        
        // All spans starting before startOffsetChar and end before
        // endOffsetChar can be dismissed, as they are not part of tempSnippet
        // This can actually be seen based on the first element of this.span
        // at the moment.

        // highlights
        // -- I'm not sure about this.
        if (this.highlight != null) {
            if (DEBUG)
                log.trace("There are highlights!");

            for (Highlight highlight : this.highlight) {                
				if (DEBUG && (highlight.start > highlight.end)) {
					log.warn("Start position is before end position {} - {}!",
							 highlight.start,
							 highlight.end);
				};
				
				int start = -1;
                int end = -1;

				// Highlight is a pagebreak
				if (highlight.end != PB_MARKER && highlight.end != ALL_MARKER) {
					start = this.positionsToOffset.start(ldid, highlight.start);
					end = this.positionsToOffset.end(ldid, highlight.end);
				}
				else {

					if (DEBUG)
						log.trace("Highlight is pagebreak -- do not retrieve offset");

					// In pagebreak highlights
					// there is already a character
					start = highlight.start;
					end = highlight.end;
                };

                start -= startOffsetChar;

				// Keep end equal -1
				if (end != PB_MARKER && end != ALL_MARKER) {
                    if (DEBUG)
                        log.trace("PTO whas retrieved {}-{} for class {}", start,
                                  end, highlight.number);
                    end -= startOffsetChar;

                    // Cut longer spans (e.g. from relation references)
                    if (end > endRelOffsetChar) {
                        end = endRelOffsetChar;
                    };
				}
				else if (DEBUG) {
					log.debug("Pagebreak keeps end position");
				};

                if (start < 0 ||
                    ((end < 0 | start > endRelOffsetChar) && end != PB_MARKER && end != ALL_MARKER)) {
                    continue;
                };

                if (DEBUG && (start > endRelOffsetChar))
                    log.debug("Ignore marker {}/{}/{}/{}", start, end, highlight.number, endRelOffsetChar);

                
                // Create intArray for highlight
                intArray = new int[] {
					start,
					end,
					highlight.number,
					0 // Dummy value for later use
                };

				if (DEBUG)
					log.debug("Added array to span with {} (2)", intArray);

                this.span.add(intArray);
            };
        };
        return true;
    };


    // Pass the local docid to retrieve character positions for the offset
    private int[] _processOffsetChars (int ldid, int startPosChar,
            int endPosChar) {

        int startOffsetChar = -1, endOffsetChar = -1;
        int startOffset = -1, endOffset = -1;
        PositionsToOffset pto = this.positionsToOffset;

        // The offset is defined by a span
        if (this.getContext().isSpanDefined()) {

            if (DEBUG)
                log.trace("Try to expand to <{}>",
                        this.context.getSpanContext());

            this.startMore = false;
            this.endMore = false;

            int[] spanContext = this.expandContextToSpan(
                    this.positionsToOffset.getLeafReader(), (Bits) null,
                    "tokens", this.context.getSpanContext());
            startOffset = spanContext[0];
            endOffset = spanContext[1];
            startOffsetChar = spanContext[2];
            endOffsetChar = spanContext[3];
            if (DEBUG)
                log.trace("Got context based on span {}-{}/{}-{}",
                        startOffset, endOffset, startOffsetChar, endOffsetChar);
            // Make sure we can (re)compute character offsets after adjustments
            this.positionsToOffset.add(ldid, startOffset);
            this.positionsToOffset.add(ldid, endOffset);
        };

        // The offset is defined by tokens or characters
        if (endOffset == -1) {

            PositionsToOffset ptoTok = pto;

            // The left offset is defined by tokens
            if (this.context.left.isToken()) {
                startOffset = this.startPos - this.context.left.getLength();
                if (DEBUG)
                    log.trace("PTO will retrieve {} (Left context)",
                            startOffset);
                ptoTok.add(ldid, startOffset);
            }

            // The left offset is defined by characters
            else {
                startOffsetChar = startPosChar - this.context.left.getLength();
            };

            // The right context is defined by tokens
            if (this.context.right.isToken()) {
                endOffset = this.endPos + this.context.right.getLength() - 1;
                if (DEBUG)
                    log.trace("PTO will retrieve {} (Right context)",
                            endOffset);
                ptoTok.add(ldid, endOffset);
            }

            // The right context is defined by characters
            else {
                endOffsetChar = (endPosChar == -1) ? -1
                        : endPosChar + this.context.right.getLength();
            };

        // Enforce total KWIC token cap (left + match + right) on token offsets (removed dead guard)
            }

        // Enforce total KWIC token cap (left + match + right), regardless of span or token context (removed dead guard)

        // Ensure PTO knows the adjusted token boundaries before resolving chars
        if (startOffset != -1)
            pto.add(ldid, startOffset);
        if (endOffset != -1)
            pto.add(ldid, endOffset);

        // Compute character offsets according to potentially adjusted token offsets
        if (startOffset != -1)
            startOffsetChar = pto.start(ldid, startOffset);
        if (endOffset != -1)
            endOffsetChar = pto.end(ldid, endOffset);

        // Diagnostic: show computed offsets and context (debug only)
        if (DEBUG)
            log.trace("_processOffsetChars: startOffset={} endOffset={} startOffsetChar={} endOffsetChar={} startPos={} endPos={} leftTok?{} leftLen={} rightTok?{} rightLen={} id={}",
                      startOffset, endOffset, startOffsetChar, endOffsetChar,
                      this.startPos, this.endPos,
                      this.context.left.isToken(), this.context.left.getLength(),
                      this.context.right.isToken(), this.context.right.getLength(),
                      this.getID());

        // Ensure zero-context means match-only and not full document
        if (startOffset == -1 && (startOffsetChar < 0 || this.context.left.getLength() == 0))
            startOffsetChar = startPosChar;
        if (endOffset == -1 && (endOffsetChar < 0 || this.context.right.getLength() == 0))
            endOffsetChar = endPosChar;



        // This can happen in case of non-token characters
        // in the match and null offsets
        if (startOffsetChar > startPosChar)
            startOffsetChar = startPosChar;
        else if (startOffsetChar < 0)
            startOffsetChar = startPosChar;

        // No "..." at the beginning
        if (startOffsetChar == 0)
            this.startMore = false;

        if (endOffsetChar != -1 && endOffsetChar < endPosChar)
            endOffsetChar = endPosChar;

        if (DEBUG)
            log.trace("The context spans from chars {}-{}", startOffsetChar,
                    endOffsetChar);

        // Removed optional hard character cap for the HTML window

        // One-line summary of the final HTML character window
        int charWinLen = (endOffsetChar > -1) ? Math.max(0, endOffsetChar - startOffsetChar) : -1;
        log.info("KWIC HTML char window: length={} (start={} end={}) id={} uid={}",
                 charWinLen, startOffsetChar, endOffsetChar, this.getID(), this.getUID());

        // Get snippet information from the primary data
        boolean htmlCharFallback = false;
        // Use legacy behavior: no safe char bounds clamping
        if (endOffsetChar > -1 && (endOffsetChar < this.getPrimaryDataLength())) {
            this.tempSnippet = this.getPrimaryData(startOffsetChar, endOffsetChar);
        }
        else {
            this.tempSnippet = this.getPrimaryData(startOffsetChar);
            this.endMore = false;
            htmlCharFallback = true;
        };

        log.info("KWIC HTML chars: startChar={} endChar={} pdl={} fallback={} snippetLen={} id={} uid={}",
                 startOffsetChar,
                 (endOffsetChar > -1 ? endOffsetChar : -1),
                 this.getPrimaryDataLength(),
                 htmlCharFallback,
                 (this.tempSnippet != null ? this.tempSnippet.length() : -1),
                 this.getID(), this.getUID());

        if (DEBUG)
            log.trace("Snippet: '{}'", this.tempSnippet);

        if (DEBUG)
            log.trace(
                    "The match entry is {}-{} ({}-{}) with absolute offsetChars {}-{}",
                    startPosChar - startOffsetChar,
                    endPosChar - startOffsetChar, startPosChar, endPosChar,
                    startOffsetChar, endOffsetChar);

        // TODO: Simplify
        return new int[] { startPosChar - startOffsetChar,
                endPosChar - startOffsetChar, -1, 0 };
    };


    // Identical to Result!
    public JsonNode toJsonNode () {
        ObjectNode json = (ObjectNode) super.toJsonNode();

        if (this.context != null)
            json.set("context", this.getContext().toJsonNode());

        if (this.version != null)
            json.put("version", this.getVersion());

		if (this.startPage != -1) {
			ArrayNode pages = mapper.createArrayNode();
			pages.add(this.startPage);
			if (this.endPage != -1 && this.endPage != this.startPage)
				pages.add(this.endPage);

			json.set("pages", pages);
		};
        
        if (this.hasSnippet)
            json.put("snippet", this.getSnippetHTML());

        if (this.hasTokens)
            json.set("tokens", this.getSnippetTokens());

		ArrayNode fields = json.putArray("fields");
       
		// Iterate over all fields
		Iterator<MetaField> fIter = mFields.iterator();
		while (fIter.hasNext()) {
            MetaField mf = fIter.next();
            fields.add(mf.toJsonNode());

            // Legacy flat field support
            String mfs = mf.key;
            String value = this.getFieldValue(mfs);
            if (value != null && !json.has(mfs))
                json.set(mfs, new TextNode(value));
		};

        this.addMessage(0, "Support for flat field values is deprecated");
        
        return json;
    };


    public String toJsonString () {
        JsonNode json = (JsonNode) this.toJsonNode();

        // Match was no match
        if (json.size() == 0)
            return "{}";
        try {
            return mapper.writeValueAsString(json);
        }
        catch (Exception e) {
            log.warn(e.getLocalizedMessage());
        };

        return "{}";
    };


    // Return match as token list
    // TODO: This will be retrieved in case "tokenList" is
    //       requested in "fields"
    public ObjectNode toTokenList () {
        ObjectNode json = mapper.createObjectNode();

        if (this.getDocID() != null)
            json.put("textSigle", this.getDocID());
        else if (this.getTextSigle() != null)
            json.put("textSigle", this.getTextSigle());

        ArrayNode tokens = json.putArray("tokens");

        // Get pto object
        PositionsToOffset pto = this.positionsToOffset;

        // Add for position retrieval
        for (int i = this.getStartPos(); i < this.getEndPos(); i++) {
            pto.add(this.localDocID, i);
        };

        // Retrieve positions
        for (int i = this.getStartPos(); i < this.getEndPos(); i++) {
            ArrayNode token = tokens.addArray();
            for (int offset : pto.span(this.localDocID, i)) {
                token.add(offset);
            };
        };

        return json;
    };


    // Remove duplicate identifiers
    // Yeah ... I mean ... why not?
    private void _filterMultipleIdentifiers () {
        ArrayList<Integer> removeDuplicate = new ArrayList<>(10);
        HashSet<String> identifiers = new HashSet<>(20);
        for (int i = 0; i < this.span.size(); i++) {

            // span is an int array: [Start, End, Number, Dummy]
            int highlightNumber = this.span.get(i)[2];

            // Number is an identifier
            if (highlightNumber < -1) {

                // Get the real identifier
                String idNumber =
					identifierNumber.get(highlightNumber);
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
    @JsonIgnore
    public String getClassID (int nr) {
        return this.identifierNumber.get(nr);
    };


    /*
     * Get annotation based on id
     */
    @JsonIgnore
    public String getAnnotationID (int nr) {
        return this.annotationNumber.get(nr);
    };


    /*
     * Get relation based on id
     */
    @JsonIgnore
    public Relation getRelationID (int nr) {
        return this.relationNumber.get(nr);
    };
};
