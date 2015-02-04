package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ids_mannheim.korap.query.SpanWithinQuery;


/**
 * SpanWithinQuery is DEPRECATED and will
 * be replaced by SpanPositionQuery in the near future
 *
 * TODO: Support exclusivity
 * TODO: Use the term "queue" and implement it similar to SpanOrQuery
 */

/**
 * Compare two spans and check how they relate positionally.
 *
 * @author diewald
 */
public class WithinSpans extends Spans {

    // Logger
    private final Logger log = LoggerFactory.getLogger(WithinSpans.class);

    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;
    
    private boolean more = false;

    // Boolean value indicating if span B
    // should be forwarded next (true)
    // or span A (false);
    boolean nextSpanB = true;

    private int
        wrapStart     = -1,
        wrapEnd       = -1,
        embeddedStart = -1,
        embeddedEnd   = -1,
        wrapDoc       = -1,
        embeddedDoc   = -1,
        matchDoc      = -1,
        matchStart    = -1,
        matchEnd      = -1;
    
    private Collection<byte[]> matchPayload;
    private Collection<byte[]> embeddedPayload;
    
    // Indicates that the wrap and the embedded spans are in the same doc
    private boolean inSameDoc = false;

    /*
      Supported flags are currently:
      ov  -> 0  | overlap:       A & B != empty
      rov -> 2  | real overlap:  A & B != empty and
                                 ((A | B) != A or
                                 (A | B) != B)
      in  -> 4  | within:        A | B = A
      rin -> 6  | real within:   A | B = A and
                                 A & B != A
      ew  -> 8  | endswith:      A | B = A and
                                 A.start = B.start
      sw  -> 10 | startswith:    A | B = A and
                                 A.end = B.end
      m   -> 12 |                A = B
    */
    public static final byte
        OVERLAP      = (byte) 0,
        REAL_OVERLAP = (byte) 2,
        WITHIN       = (byte) 4,
        REAL_WITHIN  = (byte) 6,
        ENDSWITH     = (byte) 8,
        STARTSWITH   = (byte) 10,
        MATCH        = (byte) 12;

    private byte flag;

    // Contains the query
    private SpanWithinQuery query;

    // Representing the first operand
    private final Spans wrapSpans;

    // Representing the second operand
    private final Spans embeddedSpans;

    // Check flag if the current constellation
    // was checked yet
    private boolean tryMatch = true;

    private LinkedList<KorapLongSpan>
        spanStore1,
        spanStore2;

    public WithinSpans (SpanWithinQuery spanWithinQuery,
                        AtomicReaderContext context,
                        Bits acceptDocs,
                        Map<Term,TermContext> termContexts,
                        byte flag) throws IOException {

        if (DEBUG)
            log.trace("Construct WithinSpans");

        // Init copies
        this.matchPayload = new LinkedList<byte[]>();

        // Get spans
        this.wrapSpans = spanWithinQuery.wrap().getSpans(
            context,
            acceptDocs,
            termContexts
        );
        this.embeddedSpans = spanWithinQuery.embedded().getSpans(
            context,
            acceptDocs,
            termContexts
        );

        this.flag = flag;

        // SpanStores for backtracking
        this.spanStore1 = new LinkedList<KorapLongSpan>();
        this.spanStore2 = new LinkedList<KorapLongSpan>();

        // kept for toString() only.
        this.query = spanWithinQuery;
    };
    

    // Move to next match, returning true iff any such exists.
    @Override
    public boolean next () throws IOException {

        if (DEBUG)
            log.trace("Next with docs {}, {}", wrapDoc, embeddedDoc);

        // Initialize spans
        if (!this.init()) {
            this.more        = false;
            this.inSameDoc   = false;
            this.wrapDoc     = DocIdSetIterator.NO_MORE_DOCS;
            this.embeddedDoc = DocIdSetIterator.NO_MORE_DOCS;
            this.matchDoc    = DocIdSetIterator.NO_MORE_DOCS;
            return false;
        };

        // There are more spans and they are in the same document

        while (this.more && (wrapDoc == embeddedDoc ||
                             // this.inSameDoc ||
                             this.toSameDoc())) {
            if (DEBUG)
                log.trace("We are in the same doc: {}, {}", wrapDoc, embeddedDoc);

            // Both spans match according to the flag
            // Silently the next operations are prepared
            if (this.tryMatch && this.doesMatch()) {

                if (this.wrapEnd == -1)
                    this.wrapEnd = this.wrapSpans.end();
		
                this.matchStart = embeddedStart < wrapStart ? embeddedStart : wrapStart;
                this.matchEnd   = embeddedEnd   > wrapEnd   ? embeddedEnd   : wrapEnd;
                this.matchDoc   = embeddedDoc;
                this.matchPayload.clear();

                if (this.embeddedPayload != null)
                    matchPayload.addAll(embeddedPayload);

                if (this.wrapSpans.isPayloadAvailable())
                    this.matchPayload.addAll(wrapSpans.getPayload());

                if (DEBUG)
                    log.trace(
                        "   ---- MATCH ---- {}-{} ({})",
                        matchStart,
                        matchEnd,
                        matchDoc
                    );

                this.tryMatch = false;
                return true;
            }

            // Get next embedded
            else if (this.nextSpanB) {

                // Next time try the match
                this.tryMatch = true;
		
                if (DEBUG)
                    log.trace("In the next embedded branch");
		
                KorapLongSpan current = null;

                // New - fetch until theres a span in the correct doc or bigger
                while (!this.spanStore2.isEmpty()) {
                    current = spanStore2.removeFirst();
                    if (current.doc >= this.wrapDoc)
                        break;
                };


                // There is nothing in the second store
                if (current == null) {
                    if (DEBUG)
                        log.trace("SpanStore 2 is empty");
                    
                    // Forward with embedding
                    if (!this.embeddedSpans.next()) {
                        this.nextSpanA();
                        continue;
                    }

                    else if (DEBUG) {
                        log.trace("Fetch next embedded span");
                    };
		    
                    this.embeddedStart = -1;
                    this.embeddedEnd = -1;
                    this.embeddedPayload = null;
                    this.embeddedDoc = this.embeddedSpans.doc();

                    if (this.embeddedDoc != this.wrapDoc) {
                        
                        if (DEBUG) {
                            log.trace("Embedded span is in a new document {}",
                                      _currentEmbedded().toString());
                            log.trace("Reset current embedded doc");
                        };	
	
                        /*
                        if (DEBUG)
                            log.trace("Clear all span stores");
                        this.spanStore1.clear();
                        this.spanStore2.clear();
                        */

                        this.storeEmbedded();

                        // That is necessary to backtrack to the last document!
                        this.inSameDoc = true;
                        this.embeddedDoc = wrapDoc;
                        // this.tryMatch = false; // already covered in nextSpanA

                        this.nextSpanA();
                        continue;
                    };
		    
                    if (DEBUG)
                        log.trace(
                            "   Forward embedded span to {}",
                            _currentEmbedded().toString()
                        );
                    
                    if (this.embeddedDoc != this.wrapDoc) {
                        if (DEBUG)
                            log.trace("Delete all span stores");
                     
                        // Is this always a good idea?
                        /*
                        this.spanStore1.clear();
                        this.spanStore2.clear();
                        */

                        this.embeddedStart = -1;
                        this.embeddedEnd = -1;
                        this.embeddedPayload = null;
			
                        if (!this.toSameDoc()) {
                            this.more = false;
                            this.inSameDoc = false;
                            return false;
                        };
                    };

                    this.more = true;
                    this.inSameDoc = true;
                    this.tryMatch = true;
		    
                    this.nextSpanB();
                    continue;
                }
		
                // Fetch from second store?
                else {
                    /** TODO: Change this to a single embedded object! */
                    this.embeddedStart = current.start;
                    this.embeddedEnd = current.end;
                    this.embeddedDoc = current.doc;

                    if (current.payload != null) {
                        this.embeddedPayload = new ArrayList<byte[]>(current.payload.size());
                        this.embeddedPayload.addAll(current.payload);
                    }
                    else {
                        this.embeddedPayload = null;
                    };

                    if (DEBUG)
                        log.trace("Fetch current from SpanStore 2: {}", current.toString());
		    
                    this.tryMatch = true;
                };
                continue;
            };

            // get next wrap
            if (DEBUG)
                log.trace("In the next wrap branch");

            this.tryMatch = true;

            if (DEBUG)
                log.trace("Try next wrap");

            // shift the stored spans
            if (!this.spanStore1.isEmpty()) {
                if (DEBUG) {
                    log.trace("Move everything from SpanStore 1 to SpanStore 2:");
                    for (KorapLongSpan i : this.spanStore1) {
                        log.trace("     | {}", i.toString());
                    };
                };

                // Move everything to spanStore2
                this.spanStore2.addAll(
                    0,
                    (LinkedList<KorapLongSpan>) this.spanStore1.clone()
                );
                this.spanStore1.clear();

                if (DEBUG) {
                    log.trace("SpanStore 2 now is:");
                    for (KorapLongSpan i : this.spanStore2) {
                        log.trace("     | {}", i.toString());
                    };
                };
                
            }
            else if (DEBUG) {
                log.trace("spanStore 1 is empty");
            };

            // Get next wrap
            if (this.wrapSpans.next()) {

                // Reset wrapping information
                this.wrapStart = -1;
                this.wrapEnd = -1;
		
                // Retrieve doc information
                this.wrapDoc = this.wrapSpans.doc();

                if (DEBUG)
                    log.trace(
                        "   Forward wrap span to {}",
                        _currentWrap().toString()
                    );

                if (this.embeddedDoc != this.wrapDoc) {
                    if (DEBUG)
                        log.trace("Delete all span stores");
                    this.spanStore1.clear();
                    this.spanStore2.clear();

                    // Reset embedded:
                    this.embeddedStart = -1;
                    this.embeddedEnd = -1;
                    this.embeddedPayload = null;

                    if (!this.toSameDoc()) {
                        this.inSameDoc = false;
                        this.more = false;
                        return false;
                    };
                }
                else {
                    this.inSameDoc = true;
                    // Do not match with the current state
                    this.tryMatch = false;
                };
                
                this.nextSpanB();
                continue;
            }
            this.more = false;
            this.inSameDoc = false;
            this.spanStore1.clear();
            this.spanStore2.clear();
            return false;
        };

        // No more matches
        return false;
    };


    /**
     * Skip to the next document
     */
    private boolean toSameDoc () throws IOException {

        if (DEBUG)
            log.trace("Forward to find same docs");

        /*
        if (this.embeddedSpans == null) {
            this.more      = false;
            this.matchDoc  = DocIdSetIterator.NO_MORE_DOCS;
            this.inSameDoc = false;
            return false;
        };
        */

        this.more = true;
        this.inSameDoc = true;

        this.wrapDoc     = this.wrapSpans.doc();
        this.embeddedDoc = this.embeddedSpans.doc();

        // Clear all spanStores
        if (this.wrapDoc != this.embeddedDoc) {
            /*
            if (DEBUG)
                log.trace("Clear all spanStores when moving forward");
            // Why??
            this.spanStore1.clear();
            this.spanStore2.clear();
            */
        }

        // Last doc was reached
        else if (this.wrapDoc == DocIdSetIterator.NO_MORE_DOCS) {
            this.more      = false;
            this.matchDoc  = DocIdSetIterator.NO_MORE_DOCS;
            this.inSameDoc = false;
            return false;
        }
        else {
            if (DEBUG)  {
                log.trace("Current position already is in the same doc");
                log.trace("Embedded: {}", _currentEmbedded().toString());
            };
            return true;
        };

        // Forward till match
        while (this.wrapDoc != this.embeddedDoc) {

            // Forward wrapInfo
            if (this.wrapDoc < this.embeddedDoc) {

                // Set document information
                if (!wrapSpans.skipTo(this.embeddedDoc)) {
                    this.more = false;
                    this.inSameDoc = false;
                    return false;
                };
                
                if (DEBUG)
                    log.trace("Skip wrap to doc {}", this.embeddedDoc);
		
                this.wrapDoc = this.wrapSpans.doc();
                
                if (wrapDoc == DocIdSetIterator.NO_MORE_DOCS) {
                    this.more = false;
                    this.inSameDoc = false;
                    this.embeddedDoc = DocIdSetIterator.NO_MORE_DOCS;
                    this.matchDoc = DocIdSetIterator.NO_MORE_DOCS;
                    return false;
                };
		
                this.wrapStart = -1;
                this.wrapEnd   = -1;
                
                if (wrapDoc == embeddedDoc)
                    return true;
                
            }
	
            // Forward embedInfo
            else if (this.wrapDoc > this.embeddedDoc) {
                
                // Set document information
                if (!this.embeddedSpans.skipTo(this.wrapDoc)) {
                    this.more = false;
                    this.inSameDoc = false;
                    return false;
                };
                
                if (DEBUG)
                    log.trace("Skip embedded to doc {}", this.wrapDoc);
                
                this.embeddedDoc = this.embeddedSpans.doc();
                
                if (this.embeddedDoc == DocIdSetIterator.NO_MORE_DOCS) {
                    this.more      = false;
                    this.inSameDoc = false;
                    this.wrapDoc   = DocIdSetIterator.NO_MORE_DOCS;
                    this.matchDoc  = DocIdSetIterator.NO_MORE_DOCS;
                    return false;
                };
                
                this.embeddedStart = -1;
                this.embeddedEnd = -1;
                this.embeddedPayload = null;
                
                if (this.wrapDoc == this.embeddedDoc)
                    return true;
            }
            else {
                return false;
            };
        };
        
        return true;
    };


    // Initialize spans
    private boolean init () throws IOException {

        // There is a missing span
        if (this.embeddedDoc >= 0)
            return true;

        if (DEBUG)
            log.trace("Initialize spans");

        // First tick for both spans
        if (!(this.embeddedSpans.next() && this.wrapSpans.next())) {

            if (DEBUG)
                log.trace("No spans initialized");
	    
            this.embeddedDoc = -1;
            this.more = false;
            return false;
        };
        this.more = true;

        // Store current positions for wrapping and embedded spans
        this.wrapDoc     = this.wrapSpans.doc();
        this.embeddedDoc = this.embeddedSpans.doc();

        // Set inSameDoc to true, if it is true
        if (this.embeddedDoc == this.wrapDoc)
            this.inSameDoc = true;
	
        return true;
    };


    /** Skips to the first match beyond the current, whose document number is
     * greater than or equal to <i>target</i>. <p>Returns true iff there is such
     * a match.  <p>Behaves as if written: <pre class="prettyprint">
     *   boolean skipTo(int target) {
     *     do {
     *       if (!next())
     *         return false;
     *     } while (target > doc());
     *     return true;
     *   }
     * </pre>
     * Most implementations are considerably more efficient than that.
     */
    public boolean skipTo (int target) throws IOException {

        if (DEBUG)
            log.trace("skipTo document {}", target);

        // Initialize spans
        if (!this.init())
            return false;

        assert target > embeddedDoc;

        // Only forward embedded spans
        if (this.more && (this.embeddedDoc < target)) {
            if (this.embeddedSpans.skipTo(target)) {
                this.inSameDoc = false;
                this.embeddedStart = -1;
                this.embeddedEnd = -1;
                this.embeddedPayload = null;
                this.embeddedDoc = this.embeddedSpans.doc();
            }

            // Can't be skipped to target
            else {
                this.inSameDoc = false;
                this.more = false;
                return false;
            };
        };

        // Move to same doc
        return this.toSameDoc();
    };

    private void nextSpanA () {
        if (DEBUG)
            log.trace("Try wrap next time");
        this.tryMatch = false;
        this.nextSpanB = false;
    };

    private void nextSpanB () {
        if (DEBUG)
            log.trace("Try embedded next time");
        this.nextSpanB = true;
    };


    // Check if the current span constellation does match
    // Store backtracking relevant data and say, how to proceed
    private boolean doesMatch () {
	if (DEBUG)
	    log.trace("In the match test branch");

	if (this.wrapStart == -1)
	    this.wrapStart = this.wrapSpans.start();
	
	if (this.embeddedStart == -1) {
	    this.embeddedStart = this.embeddedSpans.start();
	    this.embeddedEnd   = this.embeddedSpans.end();
	};

	this.wrapEnd = -1;
	

	// Shortcut to prevent lazyloading of .end()
	if (this.wrapStart > this.embeddedStart) {
	    // Can't match for in, rin, ew, sw, and m
	    // and will always lead to next_b
	    if (flag >= WITHIN) {
		this.nextSpanB();
		if (DEBUG)
		    _logCurrentCase((byte) 14);
		return false;
	    };
	}

	else if (this.wrapStart < this.embeddedStart) {
	    // Can't match for sw and m and will always
	    // lead to next_a
	    if (flag >= STARTSWITH) {
		this.nextSpanA();
		if (DEBUG)
		    _logCurrentCase((byte) 15);
		return false;
	    };
	};

	// Now check correctly
	byte currentCase = this.withinCase();

	if (DEBUG)
	    _logCurrentCase(currentCase);

	boolean match = false;

	// Test case
	if (currentCase >= (byte) 3 && currentCase <= (byte) 11) {
	    switch (flag) {

	    case WITHIN:
		if (currentCase >= 6  && currentCase <= 10 && currentCase != 8)
		    match = true;
		break;

	    case REAL_WITHIN:
		if (currentCase == 6 ||
		    currentCase == 9 ||
		    currentCase == 10)
		    match = true;
		break;

	    case MATCH:
		if (currentCase == 7)
		    match = true;
		break;

	    case STARTSWITH:
		if (currentCase == 7 ||
		    currentCase == 6)
		    match = true;
		break;

	    case ENDSWITH:
		if (currentCase == 7 ||
		    currentCase == 10)
		    match = true;
		break;

	    case OVERLAP:
		match = true;
		break;

	    case REAL_OVERLAP:
		if (currentCase == 3 ||
		    currentCase == 11)
		    match = true;
		break;
	    };
	};

	try {
	    this.todo(currentCase);
	}
	catch (IOException e) {
	    return false;
	}
	return match;
    };


    private void _logCurrentCase (byte currentCase) {
	log.trace("Current Case is {}", currentCase);
	
	String _e = _currentEmbedded().toString();
	    
	log.trace("    |---|    {}", _currentWrap().toString());

	switch (currentCase) {
	case 1:
	    log.trace("|-|          {}", _e);
	    break;
	case 2:
	    log.trace("|---|        {}", _e);
	    break;
	case 3:
	    log.trace("  |---|      {}", _e);
	    break;
	case 4:
	    log.trace("  |-----|    {}", _e);
	    break;
	case 5:
	    log.trace("  |-------|  {}", _e);
	    break;
	case 6:
	    log.trace("    |-|      {}", _e);
	    break;
	case 7:
	    log.trace("    |---|    {}", _e);
	    break;
	case 8:
	    log.trace("    |-----|  {}", _e);
	    break;
	case 9:
	    log.trace("     |-|     {}", _e);
	    break;
	case 10:
	    log.trace("      |-|    {}", _e);
	    break;
	case 11:
	    log.trace("      |---|  {}", _e);
	    break;
	case 12:
	    log.trace("        |-|  {}", _e);
	    break;
	case 13:
	    log.trace("         |-| {}", _e);
	    break;
	    
	case 15:
	    // Fake case
	    log.trace("      |---?  {}", _e);
	    break;

	case 16:
	    // Fake case
	    log.trace("  |---?      {}", _e);
	    break;
	};
    };
    

    private KorapLongSpan _currentWrap () {
        KorapLongSpan _wrap = new KorapLongSpan();
        _wrap.start = this.wrapStart != -1 ? this.wrapStart : this.wrapSpans.start();
        _wrap.end   = this.wrapEnd   != -1 ? this.wrapEnd   : this.wrapSpans.end();
        _wrap.doc   = this.wrapDoc   != -1 ? this.wrapDoc   : this.wrapSpans.doc();
        return _wrap;
    };
	    
    private KorapLongSpan _currentEmbedded () {
        KorapLongSpan _embedded = new KorapLongSpan();
        _embedded.start = this.embeddedStart != -1 ?
                          this.embeddedStart : this.embeddedSpans.start();
        _embedded.end   = this.embeddedEnd   != -1 ?
                          this.embeddedEnd   : this.embeddedSpans.end();
        _embedded.doc   = this.embeddedDoc   != -1 ?
                          this.embeddedDoc   : this.embeddedSpans.doc();
        return _embedded;
    };
    

    private void todo (byte currentCase) throws IOException {
	/*
	  Check what to do next with the spans.
	  
	  The different follow up steps are:
	  - storeEmbedded -> store span B for later checks
	  - nextSpanA     -> forward a
	  - nextSpanB     -> forward b

	  These rules were automatically generated
	*/

	// Case 1, 2
	if (currentCase <= (byte) 2) {
	    this.nextSpanB();
	}

	// Case 12, 13
	else if (currentCase >= (byte) 12) {
	    this.storeEmbedded();
	    this.nextSpanA();
	}

	// Case 3, 4, 5, 8
	else if (currentCase <= (byte) 5 ||
             currentCase == (byte) 8) {
	    if (flag <= 2)
            this.storeEmbedded();
	    this.nextSpanB();
	}

	// Case 11
	else if (currentCase == (byte) 11) {
	    if (this.flag == REAL_WITHIN) {
            this.nextSpanB();
	    }
	    else if (this.flag >= STARTSWITH) {
            this.nextSpanA();
	    }
	    else {
            this.storeEmbedded();
            this.nextSpanB();
	    };
	}


	// Case 6, 7, 9, 10
	else {
	    
	    if (
		// Case 6
		(currentCase == (byte) 6 && this.flag == MATCH) ||

		// Case 7
		(currentCase == (byte) 7 && this.flag == REAL_WITHIN) ||

		// Case 9, 10
		(currentCase >= (byte) 9 && this.flag >= STARTSWITH)) {
	    
		this.nextSpanA();
	    }
	    else {
		this.storeEmbedded();
		this.nextSpanB();
	    };
	};
    };

    // Store the current embedded span in the first spanStore
    private void storeEmbedded () throws IOException {

        // Create a current copy
        KorapLongSpan embedded = new KorapLongSpan();
        embedded.start = this.embeddedStart != -1 ?
                         this.embeddedStart : this.embeddedSpans.start();
        embedded.end   = this.embeddedEnd   != -1 ?
                         this.embeddedEnd : this.embeddedSpans.end();
        embedded.doc   = this.embeddedDoc;

        // Copy payloads
        if (this.embeddedPayload != null) {
            embedded.payload = new ArrayList<byte[]>(this.embeddedPayload.size());
            embedded.payload.addAll(this.embeddedPayload);
        }
        else if (this.embeddedSpans.isPayloadAvailable()) {
            embedded.payload = new ArrayList<byte[]>(3);
            Collection<byte[]> payload = this.embeddedSpans.getPayload();
            
            this.embeddedPayload = new ArrayList<byte[]>(payload.size());
            this.embeddedPayload.addAll(payload);
            embedded.payload.addAll(payload);
        };

        this.spanStore1.add(embedded);

        if (DEBUG)
            log.trace("Pushed to spanStore 1 {} (in storeEmbedded)", embedded.toString());
    };
    

    // Return case number
    private byte withinCase () {

	// case 1-5
	if (this.wrapStart > this.embeddedStart) {

	    // Case 1
	    //    |-|
	    // |-|
	    if (this.wrapStart > this.embeddedEnd) {
		return (byte) 1;
	    }

	    // Case 2
	    //   |-|
	    // |-|
	    else if (this.wrapStart == this.embeddedEnd) {
		return (byte) 2;
	    };

	    // Load wrapEnd
	    this.wrapEnd = this.wrapSpans.end();

	    // Case 3
	    //   |---|
	    // |---|
	    if (this.wrapEnd > this.embeddedEnd) {
		return (byte) 3;
	    }

	    // Case 4
	    //   |-|
	    // |---|
	    else if (this.wrapEnd == this.embeddedEnd) {
		return (byte) 4;
	    };
	    
	    // Case 5
	    //  |-|
	    // |---|
	    return (byte) 5;
	}

	// case 6-8
	else if (this.wrapStart == this.embeddedStart) {

	    // Load wrapEnd
	    this.wrapEnd = this.wrapSpans.end();

	    // Case 6
	    // |---|
	    // |-|
	    if (this.wrapEnd > this.embeddedEnd) {
		return (byte) 6;
	    }

	    // Case 7
	    // |---|
	    // |---|
	    else if (this.wrapEnd == this.embeddedEnd) {
		return (byte) 7;
	    };

	    // Case 8
	    // |-|
	    // |---|
	    return (byte) 8;
	}

	// wrapStart < embeddedStart

	// Load wrapEnd
	this.wrapEnd = this.wrapSpans.end();

	// Case 13
	// |-|
	//    |-|
	if (this.wrapEnd < this.embeddedStart) {
	    return (byte) 13;
	}

	// Case 9
	// |---|
	//  |-|
	else if (this.wrapEnd > this.embeddedEnd) {
	    return (byte) 9;
	}

	// Case 10
	// |---|
	//   |-|
	else if (this.wrapEnd == this.embeddedEnd) {
	    return (byte) 10;
	}

	// Case 11
	// |---|
	//   |---|
	else if (this.wrapEnd > this.embeddedStart) {
	    return (byte) 11;
	}
	
	// case 12
	// |-|
	//   |-|
	return (byte) 12;
    };   


    /** Returns the document number of the current match.  Initially invalid. */
    @Override
    public int doc () {
	return matchDoc;
    };

    
    /** Returns the start position of the embedding wrap.  Initially invalid. */
    @Override
    public int start () {
	return matchStart;
    };

    
    /** Returns the end position of the embedding wrap.  Initially invalid. */
    @Override
    public int end () {
	return matchEnd;
    };

    
    /**
     * Returns the payload data for the current span.
     * This is invalid until {@link #next()} is called for
     * the first time.
     * This method must not be called more than once after each call
     * of {@link #next()}. However, most payloads are loaded lazily,
     * so if the payload data for the current position is not needed,
     * this method may not be called at all for performance reasons. An ordered
     * SpanQuery does not lazy load, so if you have payloads in your index and
     * you do not want ordered SpanNearQuerys to collect payloads, you can
     * disable collection with a constructor option.<br>
     * <br>
     * Note that the return type is a collection, thus the ordering should not be relied upon.
     * <br/>
     * @lucene.experimental
     *
     * @return a List of byte arrays containing the data of this payload, otherwise null if isPayloadAvailable is false
     * @throws IOException if there is a low-level I/O error
     */
    // public abstract Collection<byte[]> getPayload() throws IOException;
    @Override
    public Collection<byte[]> getPayload() throws IOException {
	return matchPayload;
    };
    

    /**
     * Checks if a payload can be loaded at this position.
     * <p/>
     * Payloads can only be loaded once per call to
     * {@link #next()}.
     *
     * @return true if there is a payload available at this position that can be loaded
     */
    @Override
    public boolean isPayloadAvailable() {
	return matchPayload.isEmpty() == false;
    };

    
    // Todo: This may be in the wrong version
    @Override
    public long cost() {
	return wrapSpans.cost() + embeddedSpans.cost();
    };

    
    @Override
    public String toString() {
	return getClass().getName() + "("+query.toString()+")@"+
	    (embeddedDoc <= 0?"START":(more?(doc()+":"+start()+"-"+end()):"END"));
    };
};
