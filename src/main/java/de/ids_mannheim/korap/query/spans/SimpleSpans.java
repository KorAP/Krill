package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.spans.SpanQuery;
import org.apache.lucene.queries.spans.Spans;
import org.apache.lucene.util.Bits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ids_mannheim.korap.query.SimpleSpanQuery;

/**
 * An abstract class for Span enumeration including span match
 * properties and basic methods.
 * 
 * @author margaretha
 */
public abstract class SimpleSpans extends KrillSpans {
    protected final Logger log = LoggerFactory.getLogger(SimpleSpans.class);
    private SimpleSpanQuery query;
    protected boolean isStartEnumeration;
    protected boolean collectPayloads;

    protected boolean hasMoreSpans;
    // Enumeration of Spans
    protected KrillSpans firstSpans, secondSpans;

    protected int matchDocNumber, matchStartPosition, matchEndPosition;
    protected Collection<byte[]> matchPayload;

    protected short spanId;
    protected boolean hasSpanId = false;

    protected byte payloadTypeIdentifier;


    public SimpleSpans () {
        collectPayloads = true;
        matchDocNumber = -1;
        matchStartPosition = -1;
        matchEndPosition = -1;
        matchPayload = new ArrayList<byte[]>();
        isStartEnumeration = true;
    }


    public SimpleSpans (SimpleSpanQuery simpleSpanQuery,
                        LeafReaderContext context, Bits acceptDocs,
                        SpanWeights weights)
            throws IOException {
        this();
        query = simpleSpanQuery;
        collectPayloads = query.isCollectPayloads();
        // Get the enumeration of the two spans to match
        SpanQuery sq;
        if ((sq = simpleSpanQuery.getFirstClause()) != null)
            firstSpans = weights.getSpans(sq, context, acceptDocs);

        if ((sq = simpleSpanQuery.getSecondClause()) != null)
            secondSpans = weights.getSpans(sq, context, acceptDocs);

    }


    // ------------------------------------------------------------
    // Iterator API for classes finding their matches "document by
    // document"
    //
    // A class that implements nextMatchInDocument() gets the whole
    // iterator API from here. Its matching logic only ever sees the positions
    // of its driving child spans (see driver()) in the current
    // document, and moves that child with nextInDocument(). Classes
    // that implement the iterator API completely by themselves
    // override these methods.
    // ------------------------------------------------------------

    // The first match of the current document was found by
    // nextDoc() or advance(), but not returned yet by
    // nextStartPosition()
    private boolean firstMatchPending = false;


    /**
     * The child spans that drive the iteration over the documents.
     * 
     * @return the first child spans by default.
     */
    protected KrillSpans driver () {
        return firstSpans;
    }


    /**
     * Further operands that have to be in the same document as the
     * driver for the document to have a match at all. Before a
     * document is searched for matches, they are moved to the
     * document of the driver (and the driver to theirs, if they are
     * ahead), see {@link #alignOperands(int)}. They are not moved
     * to a position in the document - see {@link #startDocument()}.
     * 
     * @return no further operands by default.
     */
    protected List<? extends KrillSpans> requiredOperands () {
        return Collections.emptyList();
    }


    /**
     * Move the driver and all required operands to the next document
     * all of them have, starting with the document the driver is in.
     * 
     * @param doc
     *            The document the driver is in.
     * @return The common document, or NO_MORE_DOCS.
     */
    private int alignOperands (int doc) throws IOException {
        boolean aligned = false;
        while (doc != NO_MORE_DOCS && !aligned) {
            aligned = true;
            for (KrillSpans operand : requiredOperands()) {
                int operandDoc = operand.docID();
                if (operandDoc < doc)
                    operandDoc = operand.advance(doc);
                if (operandDoc == NO_MORE_DOCS)
                    return NO_MORE_DOCS;
                if (operandDoc > doc) {
                    doc = driver().advance(operandDoc);
                    aligned = false;
                    break;
                };
            };
        };
        return doc;
    }


    /**
     * Called whenever a new document is entered - after the driver
     * was moved to its first position in the document, and before
     * the first match is searched. Classes reset the state they keep
     * per document here.
     */
    protected void startDocument () throws IOException {}


    /**
     * Find the next match in the current document and set the match
     * properties (start and end position, payload, ...).
     * When this is called, the driver is positioned on its next
     * unprocessed position in the document if
     * <tt>hasMoreSpans</tt> is true.
     * 
     * @return <tt>true</tt> if a match was found, <tt>false</tt>
     *         if the document has no more matches.
     */
    protected boolean nextMatchInDocument () throws IOException {
        throw new UnsupportedOperationException(getClass().getName()
                + " has to implement nextMatchInDocument()");
    }


    /**
     * Move the given child spans to their next position in the
     * current document.
     * 
     * @return <tt>true</tt> if there is such a position.
     */
    protected static boolean nextInDocument (KrillSpans spans)
            throws IOException {
        return spans.nextStartPosition() != NO_MORE_POSITIONS;
    }


    @Override
    public int docID () {
        return matchDocNumber;
    }


    @Override
    public int nextDoc () throws IOException {
        if (matchDocNumber == NO_MORE_DOCS)
            return NO_MORE_DOCS;
        return this.toDocumentWithMatch(driver().nextDoc());
    }


    @Override
    public int advance (int target) throws IOException {
        if (matchDocNumber == NO_MORE_DOCS)
            return NO_MORE_DOCS;
        return this.toDocumentWithMatch(driver().advance(target));
    }


    // Starting with the given document, find the first document
    // with a match, and keep that match for nextStartPosition()
    private int toDocumentWithMatch (int doc) throws IOException {
        isStartEnumeration = false;
        firstMatchPending = false;
        while ((doc = alignOperands(doc)) != NO_MORE_DOCS) {
            matchDocNumber = doc;
            hasMoreSpans = nextInDocument(driver());
            startDocument();
            if (this.nextMatchInDocument()) {
                matchDocNumber = doc;
                firstMatchPending = true;
                return doc;
            };
            doc = driver().nextDoc();
        };
        hasMoreSpans = false;
        matchDocNumber = NO_MORE_DOCS;
        matchStartPosition = NO_MORE_POSITIONS;
        matchEndPosition = NO_MORE_POSITIONS;
        return NO_MORE_DOCS;
    }


    @Override
    public int nextStartPosition () throws IOException {

        if (firstMatchPending) {
            firstMatchPending = false;
            return matchStartPosition;
        };

        if (matchDocNumber == -1 || matchDocNumber == NO_MORE_DOCS)
            return NO_MORE_POSITIONS;

        if (matchStartPosition != NO_MORE_POSITIONS
                && this.nextMatchInDocument())
            return matchStartPosition;

        matchStartPosition = NO_MORE_POSITIONS;
        matchEndPosition = NO_MORE_POSITIONS;
        return NO_MORE_POSITIONS;
    }


    @Override
    public int startPosition () {
        return firstMatchPending ? -1 : matchStartPosition;
    }


    @Override
    public int endPosition () {
        return firstMatchPending ? -1 : matchEndPosition;
    }


    @Override
    public Collection<byte[]> getPayload () throws IOException {
        return matchPayload;
    }


    @Override
    public boolean isPayloadAvailable () throws IOException {
        return !matchPayload.isEmpty();
    }


    @Override
    public String toString () {
        return getClass().getName() + "(" + query.toString() + ")@"
                + (isStartEnumeration ? "START"
                        : (hasMoreSpans ? (docID() + ":" + startPosition() + "-" + endPosition())
                                : "END"));
    }



    /**
     * Returns the span id of the current span
     * 
     * @return the span id of the current span
     */
    public short getSpanId () {
        return spanId;
    }


    /**
     * Sets the span id of the current span
     * 
     * @param spanId
     *            span id
     */
    public void setSpanId (short spanId) {
        this.spanId = spanId;
    }


    /**
     * Gets the payload type identifier (PTI) of the current span
     * 
     * @return a payload type identifier
     */
    public byte getPayloadTypeIdentifier () {
        return payloadTypeIdentifier;
    }


    /**
     * Sets the payload type identifier (PTI) of the current span
     * 
     * @param payloadTypeIdentifier
     */
    public void setPayloadTypeIdentifier (byte payloadTypeIdentifier) {
        this.payloadTypeIdentifier = payloadTypeIdentifier;
    }

}
