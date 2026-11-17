package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.spans.Spans;

/**
 * Bridge between the span enumeration API Krill's span classes
 * were written for (Lucene up to 5.1: <tt>next()</tt>,
 * <tt>skipTo()</tt>, <tt>doc()</tt>, <tt>start()</tt>,
 * <tt>end()</tt>) and the iterator API of Lucene 5.2 and later
 * (<tt>nextDoc()</tt>, <tt>advance()</tt>,
 * <tt>nextStartPosition()</tt>, ...).
 *
 * <ul>
 * <li>Krill's span classes extend this class and keep implementing
 * the old methods. The iterator API is implemented here, once, on
 * top of the old methods - so Lucene can consume every Krill span
 * enumeration.</li>
 * <li>Span enumerations created by Lucene itself (for example by a
 * <tt>SpanTermQuery</tt> or a <tt>SpanOrQuery</tt>) only have the
 * iterator API. {@link #wrap(Spans)} gives them the old methods, so
 * Krill's span classes can consume them unchanged.</li>
 * </ul>
 *
 * A single enumeration has to be consumed through one of the two
 * APIs only - never both.
 *
 * This class is a migration aid: once a span class is ported to
 * the iterator API it should extend {@link Spans} directly, and
 * once all of them are ported this class can be removed.
 */
public abstract class LegacySpans extends Spans {

    // ------------------------------------------------------------
    // The old API, implemented by Krill's span classes
    // ------------------------------------------------------------

    /**
     * Move to the next match, returning true iff any such exists.
     */
    public abstract boolean next () throws IOException;


    /**
     * Skip to the first match beyond the current one, whose
     * document number is greater than or equal to <i>target</i>.
     * Returns true iff there is such a match.
     */
    public abstract boolean skipTo (int target) throws IOException;


    /** The document number of the current match. */
    public abstract int doc ();


    /** The start position of the current match. */
    public abstract int start ();


    /** The end position of the current match. */
    public abstract int end ();


    // ------------------------------------------------------------
    // The iterator API, implemented on top of the old API
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
    public final int docID () {
        return iterDoc;
    };


    @Override
    public final int nextDoc () throws IOException {
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
    public final int advance (int target) throws IOException {
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
    public final int nextStartPosition () throws IOException {
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
    public final int startPosition () {
        if (iterOnPosition)
            return this.start();
        return iterDocDone ? NO_MORE_POSITIONS : -1;
    };


    @Override
    public final int endPosition () {
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
    // The old API for span enumerations of Lucene
    // ------------------------------------------------------------

    /**
     * Make a span enumeration usable through the old API.
     * Enumerations that are {@link LegacySpans} already are returned
     * as they are, so casts to Krill's span classes keep working.
     *
     * @param spans
     *            A span enumeration, as returned by
     *            <tt>SpanQuery.getSpans()</tt>. May be
     *            <tt>null</tt>, which Lucene returns when there
     *            are no matches at all.
     * @return The enumeration with the old API. Never
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

        @Override
        public boolean next () {
            return false;
        };


        @Override
        public boolean skipTo (int target) {
            return false;
        };


        @Override
        public int doc () {
            return DocIdSetIterator.NO_MORE_DOCS;
        };


        @Override
        public int start () {
            return -1;
        };


        @Override
        public int end () {
            return -1;
        };


        @Override
        public Collection<byte[]> getPayload () {
            return null;
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
     * The old API on top of a span enumeration with the iterator
     * API.
     */
    private static final class NativeSpans extends LegacySpans {

        private final Spans in;
        private boolean exhausted = false;

        // Payload of the current position, read at most once
        private Collection<byte[]> payload = null;
        private boolean payloadLoaded = false;


        private NativeSpans (Spans in) {
            this.in = in;
        };


        @Override
        public boolean next () throws IOException {
            if (exhausted)
                return false;

            if (in.docID() == -1 && in.nextDoc() == NO_MORE_DOCS)
                return !(exhausted = true);

            return this.nextPosition();
        };


        @Override
        public boolean skipTo (int target) throws IOException {
            if (exhausted)
                return false;

            // The current document is fine already -
            // move to the match beyond the current one
            if (in.docID() >= target)
                return this.next();

            if (in.advance(target) == NO_MORE_DOCS)
                return !(exhausted = true);

            return this.nextPosition();
        };


        // Move to the next position, in the current document
        // or in the following ones
        private boolean nextPosition () throws IOException {
            payload = null;
            payloadLoaded = false;
            while (in.nextStartPosition() == NO_MORE_POSITIONS) {
                if (in.nextDoc() == NO_MORE_DOCS)
                    return !(exhausted = true);
            };
            return true;
        };


        @Override
        public int doc () {
            return exhausted ? DocIdSetIterator.NO_MORE_DOCS : in.docID();
        };


        @Override
        public int start () {
            return in.startPosition();
        };


        @Override
        public int end () {
            return in.endPosition();
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
