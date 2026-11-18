package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
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
 * TODO: Implement a incrStartPos() method to forward an embedded span
 *       until the start position is higher than the current start position.
 */

/**
 * Compare two spans and check how they relate positionally.
 * 
 * @author diewald
 */
public class WithinSpans extends LegacySpans {

    // Logger
    private final Logger log = LoggerFactory.getLogger(WithinSpans.class);

    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;

    // Boolean value indicating if span B
    // should be forwarded next (true)
    // or span A (false);
    boolean nextSpanB = true;

    private int wrapStart = -1, wrapEnd = -1, embeddedStart = -1,
            embeddedEnd = -1, wrapDoc = -1, embeddedDoc = -1, matchDoc = -1,
            matchStart = -1, matchEnd = -1;

    private Collection<byte[]> matchPayload;
    private Collection<byte[]> embeddedPayload;

    // The embedded spans have more positions in the current document
    private boolean embeddedInDoc = false;

    // The first match of the current document was found by
    // nextDoc() or advance(), but not returned yet by
    // nextStartPosition()
    private boolean firstMatchPending = false;

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
    public static final byte OVERLAP = (byte) 0, REAL_OVERLAP = (byte) 2,
            WITHIN = (byte) 4, REAL_WITHIN = (byte) 6, ENDSWITH = (byte) 8,
            STARTSWITH = (byte) 10, MATCH = (byte) 12;

    private byte flag;

    // Contains the query
    private SpanWithinQuery query;

    // Representing the first operand
    private final LegacySpans wrapSpans;

    // Representing the second operand
    private final LegacySpans embeddedSpans;

    // Check flag if the current constellation
    // was checked yet
    private boolean tryMatch = true;

    // Two buffers for storing candidates
    private LinkedList<WithinSpan> spanStore1, spanStore2;


    /**
     * Construct a new WithinSpans object.
     * 
     * @param spanWithinQuery
     *            The parental {@link SpanWithinQuery}.
     * @param context
     *            The {@link LeafReaderContext}.
     * @param acceptDocs
     *            Bit vector representing the documents
     *            to be searched in.
     * @param weights
     *            Creates the spans of the operands.
     * @param flag
     *            A byte flag indicating the positional condition of
     *            the sub spans.
     */
    public WithinSpans (SpanWithinQuery spanWithinQuery,
                        LeafReaderContext context, Bits acceptDocs,
                        SpanWeights weights, byte flag)
            throws IOException {

        if (DEBUG)
            log.trace("Construct WithinSpans");

        // Init copies
        this.matchPayload = new LinkedList<byte[]>();

        // Get spans
        this.wrapSpans = weights.getSpans(spanWithinQuery.wrap(), context, acceptDocs);
        this.embeddedSpans = weights.getSpans(spanWithinQuery.embedded(), context, acceptDocs);

        this.flag = flag;

        // SpanStores for backtracking
        this.spanStore1 = new LinkedList<WithinSpan>();
        this.spanStore2 = new LinkedList<WithinSpan>();

        // kept for toString() only.
        this.query = spanWithinQuery;
    };


    @Override
    protected boolean usesIteratorApi () {
        return true;
    };


    @Override
    public int docID () {
        return matchDoc;
    };


    @Override
    public int nextDoc () throws IOException {
        if (matchDoc == NO_MORE_DOCS)
            return NO_MORE_DOCS;
        return this.toDocumentWithMatch(this.wrapSpans.nextDoc());
    };


    @Override
    public int advance (int target) throws IOException {
        if (matchDoc == NO_MORE_DOCS)
            return NO_MORE_DOCS;
        return this.toDocumentWithMatch(this.wrapSpans.advance(target));
    };


    @Override
    public int nextStartPosition () throws IOException {
        if (firstMatchPending) {
            firstMatchPending = false;
            return matchStart;
        };

        if (matchDoc == -1 || matchDoc == NO_MORE_DOCS)
            return NO_MORE_POSITIONS;

        if (matchStart != NO_MORE_POSITIONS && this.nextMatchInDocument())
            return matchStart;

        matchStart = NO_MORE_POSITIONS;
        matchEnd = NO_MORE_POSITIONS;
        return NO_MORE_POSITIONS;
    };


    @Override
    public int startPosition () {
        return firstMatchPending ? -1 : matchStart;
    };


    @Override
    public int endPosition () {
        return firstMatchPending ? -1 : matchEnd;
    };


    // Starting with the given document of the wrap spans, find the
    // first document with a match, and keep that match for
    // nextStartPosition()
    private int toDocumentWithMatch (int doc) throws IOException {
        firstMatchPending = false;
        while ((doc = this.toSameDoc(doc)) != NO_MORE_DOCS) {
            matchDoc = doc;
            this.startDocument(doc);
            if (this.nextMatchInDocument()) {
                firstMatchPending = true;
                return doc;
            };
            doc = this.wrapSpans.nextDoc();
        };
        matchDoc = NO_MORE_DOCS;
        matchStart = NO_MORE_POSITIONS;
        matchEnd = NO_MORE_POSITIONS;
        return NO_MORE_DOCS;
    };


    /**
     * Move the wrap and the embedded spans to the next document both
     * of them have, starting with the given document of the wrap
     * spans.
     */
    private int toSameDoc (int doc) throws IOException {
        while (doc != NO_MORE_DOCS) {
            int embedded = this.embeddedSpans.docID();
            if (embedded < doc)
                embedded = this.embeddedSpans.advance(doc);
            if (embedded == NO_MORE_DOCS)
                return NO_MORE_DOCS;
            if (embedded == doc)
                return doc;
            doc = this.wrapSpans.advance(embedded);
        };
        return NO_MORE_DOCS;
    };


    // Both spans are in a new document - move them to their first
    // positions and reset the state kept per document
    private void startDocument (int doc) throws IOException {
        this.spanStore1.clear();
        this.spanStore2.clear();

        this.wrapDoc = doc;
        this.embeddedDoc = doc;

        this.wrapSpans.nextStartPosition();
        this.wrapStart = -1;
        this.wrapEnd = -1;

        this.embeddedInDoc = this.embeddedSpans
                .nextStartPosition() != NO_MORE_POSITIONS;
        this.embeddedStart = -1;
        this.embeddedEnd = -1;
        this.embeddedPayload = null;

        // Spans returning a document always have a position in it
        this.tryMatch = this.embeddedInDoc;
        this.nextSpanB = true;
    };


    // Find the next match in the current document
    private boolean nextMatchInDocument () throws IOException {

        if (DEBUG)
            log.trace("Next match in doc {}", matchDoc);

        while (true) {

            // Both spans match according to the flag
            // Silently the next operations are prepared
            if (this.tryMatch && this.doesMatch()) {

                if (this.wrapEnd == -1)
                    this.wrapEnd = this.wrapSpans.endPosition();

                this.matchStart = embeddedStart < wrapStart ? embeddedStart
                        : wrapStart;
                this.matchEnd = embeddedEnd > wrapEnd ? embeddedEnd : wrapEnd;
                this.matchPayload.clear();

                if (this.embeddedPayload != null)
                    matchPayload.addAll(embeddedPayload);

                if (this.wrapSpans.isPayloadAvailable())
                    this.matchPayload.addAll(wrapSpans.getPayload());

                if (DEBUG)
                    log.trace("   ---- MATCH ---- {}-{} ({})", matchStart,
                            matchEnd, matchDoc);

                this.tryMatch = false;
                return true;
            }

            // Get next embedded
            else if (this.nextSpanB) {

                // Next time try the match
                this.tryMatch = true;

                if (DEBUG)
                    log.trace("In the next embedded branch");

                WithinSpan current = this.spanStore2.pollFirst();

                if (current == null) {

                    if (DEBUG)
                        log.trace("SpanStore 2 is empty");

                    // Forward with embedding
                    if (this.embeddedInDoc)
                        this.embeddedInDoc = this.embeddedSpans
                                .nextStartPosition() != NO_MORE_POSITIONS;

                    // No more embedded spans in the document
                    if (!this.embeddedInDoc) {
                        // TODO: May need storeEmbedded
                        this.nextSpanA();
                        continue;
                    };

                    this.embeddedStart = this.embeddedSpans.startPosition();
                    this.embeddedEnd = -1;
                    this.embeddedPayload = null;

                    if (DEBUG)
                        log.trace("   Forward embedded span to {}",
                                _currentEmbedded().toString());

                    this.nextSpanB();
                    continue;
                }

                // Fetch from second store
                /**
                 * TODO: Change this to a single embedded object!
                 */
                this.embeddedStart = current.start;
                this.embeddedEnd = current.end;

                if (current.payload != null) {
                    this.embeddedPayload = new ArrayList<byte[]>(
                            current.payload.size());
                    this.embeddedPayload.addAll(current.payload);
                }
                // An empty payload, as storeEmbedded() would
                // otherwise take the payload of the current
                // position of the embedded spans
                else {
                    this.embeddedPayload = new ArrayList<byte[]>(0);
                };

                if (DEBUG)
                    log.trace("Fetch current from SpanStore 2: {}",
                            current.toString());

                continue;
            };

            // get next wrap
            if (DEBUG)
                log.trace("In the next wrap branch");

            // shift the stored spans
            if (!this.spanStore1.isEmpty()) {
                if (DEBUG) {
                    log.trace(
                            "Move everything from SpanStore 1 to SpanStore 2:");
                    for (WithinSpan i : this.spanStore1) {
                        log.trace("     | {}", i.toString());
                    };
                };

                // Move everything to spanStore2
                this.spanStore2.addAll(0, this.spanStore1);
                this.spanStore1.clear();
            };

            // No more wrap spans in the document
            if (this.wrapSpans.nextStartPosition() == NO_MORE_POSITIONS) {
                this.spanStore1.clear();
                this.spanStore2.clear();
                return false;
            };

            // Reset wrapping information
            this.wrapStart = this.wrapSpans.startPosition();
            this.wrapEnd = -1;

            if (DEBUG)
                log.trace("   Forward wrap span to {}",
                        _currentWrap().toString());

            // Do not match with the current state
            this.tryMatch = false;
            this.nextSpanB();
        }
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
    private boolean doesMatch () throws IOException {
        if (DEBUG)
            log.trace("In the match test branch");

        if (this.wrapStart == -1)
            this.wrapStart = this.wrapSpans.startPosition();

        if (this.embeddedStart == -1) {
            this.embeddedStart = this.embeddedSpans.startPosition();
        };

        this.wrapEnd = -1;

        // Shortcut to prevent lazyloading of .end()
        //   [---
        // [---
        if (this.wrapStart > this.embeddedStart) {
            // Can't match for in, rin, ew, sw, and m
            // and will always lead to next_b
            if (flag >= WITHIN) {
                this.nextSpanB();
                if (DEBUG)
                    _logCurrentCase((byte) 16);
                return false;
            };
        }

        // [---
        //   [---
        else if (this.wrapStart < this.embeddedStart) {
            // Can't match for sw and m and will always
            // lead to next_a

            if (flag >= STARTSWITH) {
                if (DEBUG)
                    log.trace("Shortcut for lazy loading");

                this.storeEmbedded();
                this.nextSpanA();

                if (DEBUG)
                    _logCurrentCase((byte) 15);
                return false;
            };
        };

        if (this.embeddedEnd == -1) {
            this.embeddedEnd = this.embeddedSpans.endPosition();
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
                    if (currentCase >= 6 && currentCase <= 10
                            && currentCase != 8)
                        match = true;
                    break;

                case REAL_WITHIN:
                    if (currentCase == 6 || currentCase == 9
                            || currentCase == 10)
                        match = true;
                    break;

                case MATCH:
                    if (currentCase == 7)
                        match = true;
                    break;

                case STARTSWITH:
                    if (currentCase == 7 || currentCase == 6)
                        match = true;
                    break;

                case ENDSWITH:
                    if (currentCase == 7 || currentCase == 10)
                        match = true;
                    break;

                case OVERLAP:
                    match = true;
                    break;

                case REAL_OVERLAP:
                    if (currentCase == 3 || currentCase == 11)
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


    private WithinSpan _currentWrap () {
        WithinSpan _wrap = new WithinSpan();
        _wrap.start = this.wrapStart != -1 ? this.wrapStart
                : this.wrapSpans.startPosition();
        _wrap.end = this.wrapEnd != -1 ? this.wrapEnd : this.wrapSpans.endPosition();
        _wrap.doc = this.wrapDoc != -1 ? this.wrapDoc : this.wrapSpans.docID();
        return _wrap;
    };


    private WithinSpan _currentEmbedded () {
        WithinSpan _embedded = new WithinSpan();
        _embedded.start = this.embeddedStart != -1 ? this.embeddedStart
                : this.embeddedSpans.startPosition();
        _embedded.end = this.embeddedEnd != -1 ? this.embeddedEnd
                : this.embeddedSpans.endPosition();
        _embedded.doc = this.embeddedDoc != -1 ? this.embeddedDoc
                : this.embeddedSpans.docID();
        return _embedded;
    };


    private void todo (byte currentCase) throws IOException {
        if (DEBUG) {
            log.trace("Check what to do next ...");
        };

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
        else if (currentCase <= (byte) 5 || currentCase == (byte) 8) {
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

                // TODO: May need storeEmbedded

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

                // TODO: May need storeEmbedded
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
        WithinSpan embedded = new WithinSpan();
        embedded.start = this.embeddedStart != -1 ? this.embeddedStart
                : this.embeddedSpans.startPosition();
        embedded.end = this.embeddedEnd != -1 ? this.embeddedEnd
                : this.embeddedSpans.endPosition();
        embedded.doc = this.embeddedDoc;

        // Copy payloads
        if (this.embeddedPayload != null) {
            embedded.payload = new ArrayList<byte[]>(
                    this.embeddedPayload.size());
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
            log.trace("Pushed to spanStore 1 {} (in storeEmbedded)",
                    embedded.toString());
    };


    // Return case number
    private byte withinCase () {

        if (DEBUG) {
            log.trace(">>>>>>>>>>>>>> {}-{}|{}-{}", this.wrapStart,
                    this.wrapSpans.endPosition(), this.embeddedStart,
                    this.embeddedSpans.endPosition());
        };

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
            this.wrapEnd = this.wrapSpans.endPosition();

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
            this.wrapEnd = this.wrapSpans.endPosition();
            // this.embeddedEnd = this.embeddedSpans.endPosition();

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
        this.wrapEnd = this.wrapSpans.endPosition();

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


    /**
     * Returns the payload data for the current span.
     * This is invalid until {@link #next()} is called for
     * the first time.
     * This method must not be called more than once after each call
     * of {@link #next()}. However, most payloads are loaded lazily,
     * so if the payload data for the current position is not needed,
     * this method may not be called at all for performance reasons.
     * An ordered
     * SpanQuery does not lazy load, so if you have payloads in your
     * index and
     * you do not want ordered SpanNearQuerys to collect payloads, you
     * can
     * disable collection with a constructor option.<br>
     * <br>
     * Note that the return type is a collection, thus the ordering
     * should not be relied upon.
     * <br/>
     * 
     * @lucene.experimental
     * 
     * @return a List of byte arrays containing the data of this
     *         payload, otherwise null if isPayloadAvailable is false
     * @throws IOException
     *             if there is a low-level I/O error
     */
    // public abstract Collection<byte[]> getPayload() throws IOException;
    @Override
    public Collection<byte[]> getPayload () throws IOException {
        return matchPayload;
    };


    /**
     * Checks if a payload can be loaded at this position.
     * <p/>
     * Payloads can only be loaded once per call to {@link #next()}.
     * 
     * @return true if there is a payload available at this position
     *         that can be loaded
     */
    @Override
    public boolean isPayloadAvailable () {
        return matchPayload.isEmpty() == false;
    };


    // Todo: This may be in the wrong version
    @Override
    public long cost () {
        return wrapSpans.cost() + embeddedSpans.cost();
    };


    @Override
    public String toString () {
        return getClass().getName() + "(" + query.toString() + ")@"
                + (matchDoc == -1 ? "START"
                        : (matchDoc != NO_MORE_DOCS
                                ? (matchDoc + ":" + matchStart + "-" + matchEnd)
                                : "END"));
    };


    // This was formerly the default candidate span class,
    // before it was refactored out
    private class WithinSpan implements Comparable<WithinSpan>, Cloneable {
        public int start = -1, end = -1, doc = -1;

        public Collection<byte[]> payload;

        public short elementRef = -1;


        public void clear () {
            this.start = -1;
            this.end = -1;
            this.doc = -1;
            clearPayload();
        };


        @Override
        public int compareTo (WithinSpan o) {
            /* optimizable for short numbers to return o.end - this.end */
            if (this.doc < o.doc) {
                return -1;
            }
            else if (this.doc == o.doc) {
                if (this.start < o.start) {
                    return -1;
                }
                else if (this.start == o.start) {
                    if (this.end < o.end)
                        return -1;
                };
            };
            return 1;
        };


        public short getElementRef () {
            return elementRef;
        }


        public void setElementRef (short elementRef) {
            this.elementRef = elementRef;
        };


        @Override
        public Object clone () {
            WithinSpan span = new WithinSpan();
            span.start = this.start;
            span.end = this.end;
            span.doc = this.doc;
            span.payload.addAll(this.payload);
            return span;
        };


        public WithinSpan copyFrom (WithinSpan o) {
            this.start = o.start;
            this.end = o.end;
            this.doc = o.doc;
            // this.clearPayload();
            this.payload.addAll(o.payload);
            return this;
        };


        public void clearPayload () {
            if (this.payload != null)
                this.payload.clear();
        };


        public String toString () {
            StringBuilder sb = new StringBuilder("[");
            return sb.append(this.start).append('-').append(this.end)
                    .append('(').append(this.doc).append(')').append(']')
                    .toString();
        };
    };
};
