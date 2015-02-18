package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.lucene.search.spans.Spans;

/**
 * CandidateSpan stores the current state of a Lucene {@link Spans}, which is an
 * enumeration. CandidateSpan is used for various purposes, such as for
 * collecting spans which will be used in a latter process or next matching.
 * 
 * @author margaretha
 * */
public class CandidateSpan implements Comparable<CandidateSpan>, Cloneable {
	protected int doc, start, end;
    private long cost;
    private Collection<byte[]> payloads = new ArrayList<>();
    private int position;
    private CandidateSpan childSpan; // used for example for multiple distance
                                     // with unordered constraint
    protected short spanId;

    /**
     * Constructs a CandidateSpan for the given Span.
     * 
     * @param span a Span
     * @throws IOException
     */
    public CandidateSpan(Spans span) throws IOException {
        this.doc = span.doc();
        this.start = span.start();
        this.end = span.end();
        this.cost = span.cost();
        if (span.isPayloadAvailable())
            setPayloads(span.getPayload());
    }

    /**
     * Constructs a CandidateSpan for the given Span and element position (where
     * the span is included in a document). The element position is important
     * for the matching process in {@link ElementDistanceSpans}.
     * 
     * @param span a Span
     * @param position an element position
     * @throws IOException
     */
    public CandidateSpan(Spans span, int position) throws IOException {
        this(span);
        this.position = position;
    }

    /**
     * Constructs a CandidateSpan from all the given variables which are
     * properties of a Span.
     * 
     * @param start the start position of a span
     * @param end the end position of a span
     * @param doc the document including the span
     * @param cost the cost of finding a span
     * @param payloads the payloads of a span
     */
    public CandidateSpan(int start, int end, int doc, long cost,
            Collection<byte[]> payloads) {
        this.start = start;
        this.end = end;
        this.doc = doc;
        this.cost = cost;
        if (payloads != null)
            setPayloads(payloads);
    }

    @Override
    protected CandidateSpan clone() throws CloneNotSupportedException {
        return new CandidateSpan(this.start, this.end, this.doc, this.cost,
                this.payloads);
    }

    /**
     * Returns the document number containing the CandidateSpan.
     * 
     * @return the document number
     */
    public int getDoc() {
        return doc;
    }

    /**
     * Sets the document number containing the CandidateSpan.
     * 
     * @param doc the document number
     */
    public void setDoc(int doc) {
        this.doc = doc;
    }

    /**
     * Returns the start position of the CandidateSpan.
     * 
     * @return the start position
     */
    public int getStart() {
        return start;
    }

    /**
     * Sets the start position of the CandidateSpan.
     * 
     * @param start the start position
     */
    public void setStart(int start) {
        this.start = start;
    }

    /**
     * Returns the end position of the CandidateSpan.
     * 
     * @return the end position
     */
    public int getEnd() {
        return end;
    }

    /**
     * Sets the end position of the CandidateSpan.
     * 
     * @param end the end position
     */
    public void setEnd(int end) {
        this.end = end;
    }

    /**
     * Returns the payloads of the CandidateSpan.
     * 
     * @return the payloads
     */
    public Collection<byte[]> getPayloads() {
        return payloads;
    }

    /**
     * Sets the payloads of the CandidateSpan.
     * 
     * @param payloads the payloads
     */
    public void setPayloads(Collection<byte[]> payloads) {

        for (byte[] b : payloads) {
            if (b == null)
                this.payloads.add(null);
            else
                this.payloads.add(b.clone());
        }
    }

    /**
     * Returns the cost of finding the CandidateSpan.
     * 
     * @return the cost
     */
    public long getCost() {
        return cost;
    }

    /**
     * Sets the cost of finding the CandidateSpan.
     * 
     * @param cost the cost
     */
    public void setCost(long cost) {
        this.cost = cost;
    }

    /**
     * Returns the element position number containing the CandidateSpan.
     * 
     * @return the element position number
     */
    public int getPosition() {
        return position;
    }

    /**
     * Sets the element position number containing the CandidateSpan.
     * 
     * @param position the element position number
     */
    public void setPosition(int position) {
        this.position = position;
    }

    /**
     * Returns a child/sub Span of the CandidateSpan.
     * 
     * @return a child/sub span of the CandidateSpan
     */
    public CandidateSpan getChildSpan() {
        return childSpan;
    }

    /**
     * Sets the child/sub span of the CandidateSpan.
     * 
     * @param childSpan a child/sub span of the CandidateSpan
     */
    public void setChildSpan(CandidateSpan childSpan) {
        this.childSpan = childSpan;
    }

    /**
     * Returns the span id of another Span related to the CandidateSpan. Only
     * CandidateSpan of particular Spans such as {@link AttributeSpans} having
     * this property. For instance, an AttributeSpan has a spanId of the element
     * it belongs to.
     * 
     * @return the span id of another Span related to the CandidateSpan
     */
    public short getSpanId() {
        return spanId;
    }

    /**
     * Sets the span id of another Span related to the CandidateSpan. Only
     * CandidateSpan of particular Spans such as {@link AttributeSpans} having
     * this property. For instance, an AttributeSpan has a spanId of the element
     * it belongs to.
     * 
     * @param spanId the span id of another Span related to the CandidateSpan
     */
    public void setSpanId(short spanId) {
        this.spanId = spanId;
    }

    @Override
    public int compareTo(CandidateSpan o) {
        if (this.doc == o.doc) {
            if (this.getStart() == o.getStart()) {
                if (this.getEnd() == o.getEnd())
                    return 0;
                if (this.getEnd() > o.getEnd())
                    return 1;
                else
                    return -1;
            } else if (this.getStart() < o.getStart())
                return -1;
            else
                return 1;
        } else if (this.doc < o.doc)
            return -1;
        else
            return 1;
    }
}
