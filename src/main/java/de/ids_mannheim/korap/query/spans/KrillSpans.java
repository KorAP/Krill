package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.lucene.search.spans.SpanCollector;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;

/**
 * Base class of Krill's span enumerations, and wrapper of Lucene's
 * ones (see {@link #wrap(Spans, Bits)}), giving all of them
 *
 * <ul>
 * <li>the payloads of the current match ({@link #getPayload()}),
 * which Lucene's spans no longer provide since Lucene 5.3,</li>
 * <li>a walk through all matches of all documents
 * ({@link #nextMatch()}, {@link #advanceToMatch(int)}).</li>
 * </ul>
 *
 * Spans are iterated with the iterator API of Lucene
 * (<tt>nextDoc()</tt>, <tt>advance()</tt>,
 * <tt>nextStartPosition()</tt>, ...).
 */
public abstract class KrillSpans extends Spans {


    // ------------------------------------------------------------
    // Payloads and the other methods Lucene 5.3 added to Spans
    // ------------------------------------------------------------

    /**
     * The payloads of the current match. Krill's span classes keep
     * their payloads themselves (Lucene's spans no longer provide
     * them since Lucene 5.3, see {@link PayloadCollector}).
     */
    public abstract Collection<byte[]> getPayload () throws IOException;


    /**
     * Tells if the current match has payloads.
     */
    public abstract boolean isPayloadAvailable () throws IOException;


    /**
     * Krill's spans hand their payloads over to a
     * {@link PayloadCollector}, so Lucene's composite spans (e.g. of
     * a <tt>SpanOrQuery</tt>) consisting of them deliver them.
     */
    @Override
    public void collect (SpanCollector collector) throws IOException {
        if (collector instanceof PayloadCollector)
            ((PayloadCollector) collector).collectPayloads(this.getPayload());
    };


    // Only used for scoring, which Krill does not do
    @Override
    public int width () {
        return 0;
    };


    // Only used to order two-phase iteration, which Krill's spans do
    // not support
    @Override
    public float positionsCost () {
        return 1f;
    };


    // ------------------------------------------------------------
    // Walking through all matches
    // ------------------------------------------------------------

    /**
     * Move to the next match, in the current document or in a
     * following one. For consumers that walk through all matches of
     * all documents (as <tt>next()</tt> of Lucene up to 5.1 did).
     *
     * @return <tt>true</tt> if there is such a match.
     */
    public final boolean nextMatch () throws IOException {
        if (this.docID() == NO_MORE_DOCS)
            return false;
        if (this.docID() == -1 && this.nextDoc() == NO_MORE_DOCS)
            return false;
        return this.toNextPosition();
    };


    /**
     * Move to the first match beyond the current one in a document
     * greater than or equal to <i>target</i> (as <tt>skipTo()</tt> of
     * Lucene up to 5.1 did).
     *
     * @return <tt>true</tt> if there is such a match.
     */
    public final boolean advanceToMatch (int target) throws IOException {
        if (this.docID() == NO_MORE_DOCS)
            return false;
        if (this.docID() >= target)
            return this.nextMatch();
        if (this.advance(target) == NO_MORE_DOCS)
            return false;
        return this.toNextPosition();
    };


    // Move to the next position, in the current document or in the
    // following ones
    private boolean toNextPosition () throws IOException {
        while (this.nextStartPosition() == NO_MORE_POSITIONS) {
            if (this.nextDoc() == NO_MORE_DOCS)
                return false;
        };
        return true;
    };


    // ------------------------------------------------------------
    // Span enumerations of Lucene
    // ------------------------------------------------------------

    /**
     * Make a span enumeration usable by Krill.
     * Enumerations that are {@link KrillSpans} already are returned
     * as they are, so casts to Krill's span classes keep working.
     *
     * @param spans
     *            A span enumeration, as returned by
     *            <tt>SpanQuery.getSpans()</tt>. May be
     *            <tt>null</tt>, which Lucene returns when there
     *            are no matches at all.
     * @return The enumeration. Never
     *         <tt>null</tt>.
     */
    public static KrillSpans wrap (Spans spans) {
        return wrap(spans, null);
    };


    /**
     * Make a span enumeration usable by Krill, limited to
     * the accepted documents. Krill's span classes are returned as
     * they are - they are limited to the accepted documents when they
     * are created.
     *
     * @param spans
     *            A span enumeration, as returned by
     *            <tt>SpanWeight.getSpans()</tt>. May be
     *            <tt>null</tt>.
     * @param acceptDocs
     *            The documents of Lucene's spans to keep, or
     *            <tt>null</tt> for all documents (up to Lucene 5.2,
     *            Lucene's spans were limited to them when created).
     * @return The enumeration. Never
     *         <tt>null</tt>.
     */
    public static KrillSpans wrap (Spans spans, Bits acceptDocs) {
        if (spans == null)
            return new EmptySpans();
        if (spans instanceof KrillSpans)
            return (KrillSpans) spans;
        return new NativeSpans(spans, acceptDocs);
    };


    /**
     * A span enumeration without any matches.
     */
    private static final class EmptySpans extends KrillSpans {

        private int doc = -1;



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
        public void collect (SpanCollector collector) {};


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
     * A span enumeration of Lucene, made usable by Krill.
     */
    private static final class NativeSpans extends KrillSpans {

        private final Spans in;

        // The documents to keep, or null for all documents
        private final Bits acceptDocs;

        // Payload of the current position, collected at most once
        private final PayloadCollector payloadCollector = new PayloadCollector();
        private Collection<byte[]> payload = null;
        private boolean payloadLoaded = false;


        private NativeSpans (Spans in, Bits acceptDocs) {
            this.in = in;
            this.acceptDocs = acceptDocs;
        };


        // Skip documents that are not accepted
        private int toAcceptedDoc (int doc) throws IOException {
            if (this.acceptDocs == null)
                return doc;
            while (doc != NO_MORE_DOCS && !this.acceptDocs.get(doc))
                doc = this.in.nextDoc();
            return doc;
        };



        @Override
        public int docID () {
            return in.docID();
        };


        @Override
        public int nextDoc () throws IOException {
            this.resetPayload();
            return this.toAcceptedDoc(in.nextDoc());
        };


        @Override
        public int advance (int target) throws IOException {
            this.resetPayload();
            return this.toAcceptedDoc(in.advance(target));
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



        private void resetPayload () {
            payloadLoaded = false;
        };


        /*
         * Since Lucene 5.3 payloads are collected with a
         * SpanCollector. Krill's span classes may ask several times
         * and expect an empty collection if there is no payload, like
         * Lucene 5.0 returned.
         */
        @Override
        public Collection<byte[]> getPayload () throws IOException {
            if (!payloadLoaded) {
                payloadLoaded = true;
                payloadCollector.reset();
                in.collect(payloadCollector);
                // A new collection per position, as callers keep it
                payload = new ArrayList<byte[]>(
                        payloadCollector.getPayloads());
            };
            return payload;
        };


        @Override
        public boolean isPayloadAvailable () throws IOException {
            return !this.getPayload().isEmpty();
        };


        @Override
        public void collect (SpanCollector collector) throws IOException {
            in.collect(collector);
        };


        @Override
        public int width () {
            return in.width();
        };


        @Override
        public float positionsCost () {
            return in.positionsCost();
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
