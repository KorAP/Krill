package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import org.apache.lucene.search.spans.Spans;

/**
 * Bridge between the span enumeration API Krill's span classes
 * were written for (Lucene up to 5.1: <tt>next()</tt>,
 * <tt>skipTo()</tt>, <tt>doc()</tt>, <tt>start()</tt>,
 * <tt>end()</tt>) and the iterator API of Lucene 5.2 and later
 * (<tt>nextDoc()</tt>, <tt>advance()</tt>,
 * <tt>nextStartPosition()</tt>, <tt>docID()</tt>,
 * <tt>startPosition()</tt>, <tt>endPosition()</tt>).
 *
 * Every subclass implements <b>one</b> of the two APIs, and gets
 * the other one from this class:
 *
 * <ul>
 * <li><b>Not yet ported classes</b> implement the old methods.
 * The iterator API is derived from them here, so Lucene and ported
 * classes can consume them.</li>
 * <li><b>Ported classes</b> return <tt>true</tt> from
 * {@link #usesIteratorApi()} and implement the iterator API. The
 * old methods are derived from it here, so not yet ported classes
 * can still consume them.</li>
 * </ul>
 *
 * Span enumerations created by Lucene itself (for example by a
 * <tt>SpanTermQuery</tt> or a <tt>SpanOrQuery</tt>) are made
 * available through both APIs by {@link #wrap(Spans)}.
 *
 * A single enumeration has to be consumed through one of the two
 * APIs only - never both.
 *
 * This class is a migration aid: once all span classes are ported,
 * it can be removed and the classes can extend {@link Spans}
 * directly.
 */
public abstract class LegacySpans extends Spans {

    /**
     * Tells which API the class implements.
     *
     * @return <tt>true</tt> if the class implements the iterator
     *         API, <tt>false</tt> (the default) if it implements the
     *         old API.
     */
    protected boolean usesIteratorApi () {
        return false;
    };


    private UnsupportedOperationException notImplemented (String method) {
        return new UnsupportedOperationException(getClass().getName()
                + " has to implement " + method + "()");
    };


    // ------------------------------------------------------------
    // The old API: implemented by not yet ported classes,
    // derived from the iterator API for ported classes
    // ------------------------------------------------------------

    // The iterator API has no more matches (ported classes only)
    private boolean oldExhausted = false;

    // The last match - Krill's span classes keep reporting it
    // once the enumeration is exhausted
    private int oldDoc = -1, oldStart = -1, oldEnd = -1;


    /**
     * Move to the next match, returning true iff any such exists.
     */
    public boolean next () throws IOException {
        if (!usesIteratorApi())
            throw notImplemented("next");

        if (oldExhausted)
            return false;

        if (this.docID() == -1 && this.nextDoc() == NO_MORE_DOCS)
            return !(oldExhausted = true);

        return this.oldNextPosition();
    };


    /**
     * Skip to the first match beyond the current one, whose
     * document number is greater than or equal to <i>target</i>.
     * Returns true iff there is such a match.
     */
    public boolean skipTo (int target) throws IOException {
        if (!usesIteratorApi())
            throw notImplemented("skipTo");

        if (oldExhausted)
            return false;

        // The current document is fine already -
        // move to the match beyond the current one
        if (this.docID() >= target)
            return this.next();

        if (this.advance(target) == NO_MORE_DOCS)
            return !(oldExhausted = true);

        return this.oldNextPosition();
    };


    // Move to the next position, in the current document
    // or in the following ones
    private boolean oldNextPosition () throws IOException {
        while (this.nextStartPosition() == NO_MORE_POSITIONS) {
            if (this.nextDoc() == NO_MORE_DOCS)
                return !(oldExhausted = true);
        };
        oldDoc = this.docID();
        oldStart = this.startPosition();
        oldEnd = this.endPosition();
        return true;
    };


    /** The document number of the current match. */
    public int doc () {
        if (!usesIteratorApi())
            throw notImplemented("doc");
        return oldExhausted ? oldDoc : this.docID();
    };


    /** The start position of the current match. */
    public int start () {
        if (!usesIteratorApi())
            throw notImplemented("start");
        return oldExhausted ? oldStart : this.startPosition();
    };


    /** The end position of the current match. */
    public int end () {
        if (!usesIteratorApi())
            throw notImplemented("end");
        return oldExhausted ? oldEnd : this.endPosition();
    };


    // ------------------------------------------------------------
    // The iterator API: implemented by ported classes,
    // derived from the old API for not yet ported classes
    // ------------------------------------------------------------

    // Document the iterator API is positioned on
    private int iterDoc = -1;

    // The old API has no more matches
    private boolean iterExhausted = false;

    // The old API is positioned on a match the iterator API
    // has not delivered yet
    private boolean iterLookahead = false;

    // The match the old API is positioned on is the current
    // position of the iterator API
    private boolean iterOnPosition = false;

    // All positions of the current document were delivered
    private boolean iterDocDone = false;


    @Override
    public int docID () {
        if (usesIteratorApi())
            throw notImplemented("docID");
        return iterDoc;
    };


    @Override
    public int nextDoc () throws IOException {
        if (usesIteratorApi())
            throw notImplemented("nextDoc");

        if (iterDoc == NO_MORE_DOCS)
            return NO_MORE_DOCS;

        // The match read ahead is already in a following document
        if (iterLookahead && this.doc() != iterDoc)
            return iterEnterDoc();

        if (iterExhausted)
            return iterEnd();

        // Skip the remaining matches of the current document
        do {
            if (!this.next())
                return iterEnd();
        } while (this.doc() == iterDoc);

        return iterEnterDoc();
    };


    @Override
    public int advance (int target) throws IOException {
        if (usesIteratorApi())
            throw notImplemented("advance");

        if (iterDoc == NO_MORE_DOCS)
            return NO_MORE_DOCS;

        if (target <= iterDoc)
            return this.nextDoc();

        // The match read ahead is already in a matching document
        if (iterLookahead && this.doc() != iterDoc && this.doc() >= target)
            return iterEnterDoc();

        if (iterExhausted)
            return iterEnd();

        if (!this.skipTo(target))
            return iterEnd();

        // Be lenient with implementations of skipTo() that do
        // not reach the target
        while (this.doc() < target) {
            if (!this.next())
                return iterEnd();
        };

        return iterEnterDoc();
    };


    @Override
    public int nextStartPosition () throws IOException {
        if (usesIteratorApi())
            throw notImplemented("nextStartPosition");

        if (iterDoc == -1 || iterDoc == NO_MORE_DOCS || iterDocDone)
            return NO_MORE_POSITIONS;

        // The first match of the document was already read
        if (iterLookahead) {
            iterLookahead = false;
            iterOnPosition = true;
            return this.start();
        };

        if (!this.next()) {
            iterExhausted = true;
            iterOnPosition = false;
            iterDocDone = true;
            return NO_MORE_POSITIONS;
        };

        // The next match is in a following document -
        // keep it for nextDoc() or advance()
        if (this.doc() != iterDoc) {
            iterLookahead = true;
            iterOnPosition = false;
            iterDocDone = true;
            return NO_MORE_POSITIONS;
        };

        return this.start();
    };


    @Override
    public int startPosition () {
        if (usesIteratorApi())
            throw notImplemented("startPosition");
        if (iterOnPosition)
            return this.start();
        return iterDocDone ? NO_MORE_POSITIONS : -1;
    };


    @Override
    public int endPosition () {
        if (usesIteratorApi())
            throw notImplemented("endPosition");
        if (iterOnPosition)
            return this.end();
        return iterDocDone ? NO_MORE_POSITIONS : -1;
    };


    // The old API is positioned on the first match of a new document
    private int iterEnterDoc () {
        iterDoc = this.doc();
        iterLookahead = true;
        iterOnPosition = false;
        iterDocDone = false;
        return iterDoc;
    };


    private int iterEnd () {
        iterExhausted = true;
        iterLookahead = false;
        iterOnPosition = false;
        iterDocDone = true;
        return (iterDoc = NO_MORE_DOCS);
    };


    // ------------------------------------------------------------
    // Span enumerations of Lucene
    // ------------------------------------------------------------

    /**
     * Make a span enumeration usable through both APIs.
     * Enumerations that are {@link LegacySpans} already are returned
     * as they are, so casts to Krill's span classes keep working.
     *
     * @param spans
     *            A span enumeration, as returned by
     *            <tt>SpanQuery.getSpans()</tt>. May be
     *            <tt>null</tt>, which Lucene returns when there
     *            are no matches at all.
     * @return The enumeration with both APIs. Never
     *         <tt>null</tt>.
     */
    public static LegacySpans wrap (Spans spans) {
        if (spans == null)
            return new EmptySpans();
        if (spans instanceof LegacySpans)
            return (LegacySpans) spans;
        return new NativeSpans(spans);
    };


    /**
     * A span enumeration without any matches.
     */
    private static final class EmptySpans extends LegacySpans {

        private int doc = -1;


        @Override
        protected boolean usesIteratorApi () {
            return true;
        };


        @Override
        public int docID () {
            return doc;
        };


        @Override
        public int nextDoc () {
            return (doc = NO_MORE_DOCS);
        };


        @Override
        public int advance (int target) {
            return (doc = NO_MORE_DOCS);
        };


        @Override
        public int nextStartPosition () {
            return NO_MORE_POSITIONS;
        };


        @Override
        public int startPosition () {
            return doc == -1 ? -1 : NO_MORE_POSITIONS;
        };


        @Override
        public int endPosition () {
            return doc == -1 ? -1 : NO_MORE_POSITIONS;
        };


        @Override
        public Collection<byte[]> getPayload () {
            return Collections.emptyList();
        };


        @Override
        public boolean isPayloadAvailable () {
            return false;
        };


        @Override
        public long cost () {
            return 0;
        };


        @Override
        public String toString () {
            return "EmptySpans";
        };
    };


    /**
     * A span enumeration of Lucene, made usable through both APIs.
     */
    private static final class NativeSpans extends LegacySpans {

        private final Spans in;

        // Payload of the current position, read at most once
        private Collection<byte[]> payload = null;
        private boolean payloadLoaded = false;


        private NativeSpans (Spans in) {
            this.in = in;
        };


        @Override
        protected boolean usesIteratorApi () {
            return true;
        };


        @Override
        public int docID () {
            return in.docID();
        };


        @Override
        public int nextDoc () throws IOException {
            this.resetPayload();
            return in.nextDoc();
        };


        @Override
        public int advance (int target) throws IOException {
            this.resetPayload();
            return in.advance(target);
        };


        @Override
        public int nextStartPosition () throws IOException {
            this.resetPayload();
            return in.nextStartPosition();
        };


        @Override
        public int startPosition () {
            return in.startPosition();
        };


        @Override
        public int endPosition () {
            return in.endPosition();
        };


        // Lucene's own spans report NO_MORE_DOCS when exhausted
        @Override
        public int doc () {
            return in.docID();
        };


        @Override
        public int start () {
            return in.startPosition();
        };


        @Override
        public int end () {
            return in.endPosition();
        };


        private void resetPayload () {
            payload = null;
            payloadLoaded = false;
        };


        /*
         * Since Lucene 5.2 the payload of a position may only be
         * read once, and it is null if there is none. Krill's span
         * classes may ask several times and expect an empty
         * collection instead of null, like Lucene 5.0 returned.
         */
        @Override
        public Collection<byte[]> getPayload () throws IOException {
            if (!payloadLoaded) {
                payloadLoaded = true;
                payload = in.isPayloadAvailable() ? in.getPayload() : null;
                if (payload == null)
                    payload = Collections.emptyList();
            };
            return payload;
        };


        @Override
        public boolean isPayloadAvailable () throws IOException {
            if (payloadLoaded)
                return !payload.isEmpty();
            return in.isPayloadAvailable();
        };


        @Override
        public long cost () {
            return in.cost();
        };


        @Override
        public String toString () {
            return "NativeSpans(" + in.toString() + ")";
        };
    };
};
