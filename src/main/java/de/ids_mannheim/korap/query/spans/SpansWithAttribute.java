package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ids_mannheim.korap.query.SpanAttributeQuery;
import de.ids_mannheim.korap.query.SpanWithAttributeQuery;

/**
 * Span enumeration of element or relation spans (referent spans)
 * having and/or
 * <em>not</em> having some attributes. This class only handles
 * <em>and</em>
 * operation on attributes.
 * 
 * Use SpanOrQuery to perform <em>or</em> operation on attributes,
 * i.e. choose
 * between two elements with some attribute constraints. Note that the
 * attribute
 * constraints have to be formulated in Conjunctive Normal Form (CNF).
 * 
 * @author margaretha
 */
public class SpansWithAttribute extends SimpleSpans {

    private SimpleSpans referentSpans;
    private List<AttributeSpans> attributeList;
    private List<AttributeSpans> notAttributeList;

    protected Logger logger = LoggerFactory.getLogger(SpansWithAttribute.class);


    /**
     * Constructs SpansWithAttribute from the given
     * {@link SpanWithAttributeQuery} and {@link SpansWithId}, such as
     * elementSpans and relationSpans.
     * 
     * @param spanWithAttributeQuery
     *            a spanWithAttributeQuery
     * @param spansWithId
     *            a SpansWithId
     * @param context
     * @param acceptDocs
     * @param termContexts
     * @throws IOException
     */
    public SpansWithAttribute (SpanWithAttributeQuery spanWithAttributeQuery,
                               SimpleSpans referentSpans,
                               LeafReaderContext context, Bits acceptDocs,
                               Map<Term, TermContext> termContexts)
            throws IOException {
        super(spanWithAttributeQuery, context, acceptDocs, termContexts);
        // if (!referentSpans.hasSpanId) {
        // throw new
        // IllegalArgumentException("Referent spans must have ids.");
        // }
        this.referentSpans = referentSpans;
        hasMoreSpans = referentSpans.next();
        setAttributeList(spanWithAttributeQuery, context, acceptDocs,
                termContexts);
    }


    // if there is no (positive) attributes, but there are *not attributes*
    // hasmorespan = true
    public SpansWithAttribute (SpanWithAttributeQuery spanWithAttributeQuery,
                               LeafReaderContext context, Bits acceptDocs,
                               Map<Term, TermContext> termContexts)
            throws IOException {
        super(spanWithAttributeQuery, context, acceptDocs, termContexts);
        hasMoreSpans = true;
        setAttributeList(spanWithAttributeQuery, context, acceptDocs,
                termContexts);
        if (attributeList.size() == 0) {
            throw new IllegalArgumentException(
                    "No (positive) attribute is defined.");
        }
        else if (attributeList.size() > 1) {
            referentSpans = attributeList.get(0);
            attributeList.remove(0);
        }
    }


    public void setAttributeList (SpanWithAttributeQuery swaq,
            LeafReaderContext context, Bits acceptDocs,
            Map<Term, TermContext> termContexts) throws IOException {

        attributeList = new ArrayList<AttributeSpans>();
        notAttributeList = new ArrayList<AttributeSpans>();

        List<SpanQuery> attributeList = swaq.getClauseList();
        if (swaq.isMultipleAttributes) {
            if (attributeList != null) {
                for (SpanQuery sq : attributeList) {
                    addAttributes((SpanAttributeQuery) sq, context, acceptDocs,
                            termContexts);
                }
            }
            else {
                throw new NullPointerException("Attribute list is null.");
            }
        }
        else if (swaq.getSecondClause() != null) {
            addAttributes((SpanAttributeQuery) swaq.getSecondClause(), context,
                    acceptDocs, termContexts);
        }
        else if (swaq.getType().equals("spanWithAttribute")
                && swaq.getFirstClause() != null) {
            addAttributes((SpanAttributeQuery) swaq.getFirstClause(), context,
                    acceptDocs, termContexts);
        }
        else {
            throw new NullPointerException("No attribute is defined.");
        }
    }


    /**
     * Adds the given {@link SpanAttributeQuery} to the attributeList
     * or
     * notAttributeList depending on the query, whether it is a
     * negation or not.
     * 
     * @param sq
     *            a SpanAttributeQuery
     * @param context
     * @param acceptDocs
     * @param termContexts
     * @throws IOException
     */
    private void addAttributes (SpanAttributeQuery sq,
            LeafReaderContext context, Bits acceptDocs,
            Map<Term, TermContext> termContexts) throws IOException {

        AttributeSpans as = (AttributeSpans) sq.getSpans(context, acceptDocs,
                termContexts);
        if (sq.isNegation()) {
            notAttributeList.add(as);
            as.next();
        }
        else {
            attributeList.add(as);
            hasMoreSpans &= as.next();
        }
    }


    @Override
    public boolean next () throws IOException {
        isStartEnumeration = false;
        matchPayload.clear();
        if (referentSpans == null) { // only one (positive) attribute
            return advanceAttribute();
        }
        else {
            return advance();
        }
    }


    private boolean advanceAttribute () throws IOException {
        while (hasMoreSpans) {
            SimpleSpans referentSpans = attributeList.get(0);
            advanceNotAttributes(referentSpans);
            if (checkNotReferentId(referentSpans)) {
                this.matchDocNumber = referentSpans.doc();
                this.matchStartPosition = referentSpans.start();
                this.matchEndPosition = referentSpans.end();
                this.matchPayload = referentSpans.getPayload();
                this.spanId = referentSpans.getSpanId();
                if (this.spanId > 0) {
                    hasSpanId = true;
                }
                hasMoreSpans = referentSpans.next();
                return true;
            }
        }
        return false;
    }


    /**
     * Searches for the next match by first identify a possible
     * element
     * position, and then ensuring that the element contains all the
     * attributes
     * and <em>do not</em> contain any of the not attributes.
     * 
     * @return <code>true</code> if the a match is found,
     *         <code>false</code>
     *         otherwise.
     * @throws IOException
     */
    private boolean advance () throws IOException {

        while (hasMoreSpans && searchSpanPosition()) {
            // System.out.println(referentSpans.start() + ","
            // + referentSpans.end() + " " +
            // referentSpans.getSpanId());

            if (checkReferentId() && checkNotReferentId(referentSpans)) {
                this.matchDocNumber = referentSpans.doc();
                this.matchStartPosition = referentSpans.start();
                this.matchEndPosition = referentSpans.end();
                this.matchPayload.addAll(referentSpans.getPayload());
                this.spanId = referentSpans.getSpanId();

                if (attributeList.size() > 0)
                    hasMoreSpans = attributeList.get(0).next();

                hasMoreSpans &= referentSpans.next();
                return true;
            }
        }
        return false;
    }


    /**
     * Searches for a possible referentSpan having the same document
     * number and
     * start position as the attributes', and the position is
     * different from the
     * <em>not attributes'</em> positions.
     * 
     * @return <code>true</code> if the referentSpan position is
     *         valid,
     *         <code>false</code> otherwise.
     * @throws IOException
     */
    private boolean searchSpanPosition () throws IOException {
        while (hasMoreSpans) {
            // the element does not have an attribute
            if (referentSpans.getSpanId() < 1) {
                if (attributeList.size() == 0 || notAttributeList.size() > 0) {
                    return true;
                }
                hasMoreSpans = referentSpans.next();
                continue;
            }
            if (checkAttributeListPosition()) {
                advanceNotAttributes(referentSpans);
                // logger.info("element is found: "+ withAttributeSpans.start());
                return true;
            }
        }
        return false;
    }


    /**
     * Advances the attributes to be in the same document and start
     * position as
     * the referentSpan.
     * 
     * @return <code>true</code> if the attributes are in the same
     *         document and
     *         start position as the referentSpan.
     * @throws IOException
     */
    private boolean checkAttributeListPosition () throws IOException {
        int currentPosition = referentSpans.start();
        boolean isSame = true;
        boolean isFirst = true;

        for (AttributeSpans a : attributeList) {
            if (!ensureSamePosition(referentSpans, a))
                return false;
            //	    logger.info("pos:" + withAttributeSpans.start());
            if (isFirst) {
                isFirst = false;
                currentPosition = referentSpans.start();
            }
            else if (currentPosition != referentSpans.start()) {
                currentPosition = referentSpans.start();
                isSame = false;

            }
        }
        //    logger.info("same pos: "+isSame+ ", pos "+withAttributeSpans.start());
        return isSame;
    }


    /**
     * Advances the element or attribute spans to be in the same
     * document and
     * start position.
     */
    private boolean ensureSamePosition (SimpleSpans spans,
            AttributeSpans attributes) throws IOException {

        while (hasMoreSpans && ensureSameDoc(spans, attributes)) {
            if (attributes.start() == spans.start()
                    && attributes.end() == spans.end())
                return true;
            else if (attributes.start() >= spans.start())
                hasMoreSpans = spans.next();
            else
                hasMoreSpans = attributes.next();
        }

        return false;
    }


    /**
     * Advances the <em>not-attributes</em> to be in the same or
     * greater
     * document number than referentSpans' document number. If a
     * <em>not-attribute</em> is in the same document, it is advanced
     * to be in
     * the same as or greater start position than the current
     * referentSpan.
     * 
     * @throws IOException
     */
    private void advanceNotAttributes (Spans referentSpans) throws IOException {

        for (AttributeSpans a : notAttributeList) {
            // advance the doc# of not AttributeSpans
            // logger.info("a "+a.start());
            while (!a.isFinish() && a.doc() <= referentSpans.doc()) {

                if (a.doc() == referentSpans.doc()
                        && a.start() >= referentSpans.start())
                    break;

                if (!a.next())
                    a.setFinish(true);
            }
        }
    }


    /**
     * Ensures that the referent id of each attributeSpans in the
     * attributeList
     * is the same as the spanId of the actual referentSpans.
     * 
     * @return <code>true</code> if the spanId of the current
     *         referentSpans is
     *         the same as all the referentId of all the
     *         attributeSpans in the
     *         attributeList, <code>false</code> otherwise.
     * @throws IOException
     */
    private boolean checkReferentId () throws IOException {
        for (AttributeSpans attribute : attributeList) {
            if (referentSpans.getSpanId() != attribute.getSpanId()) {
                if (referentSpans.getSpanId() < attribute.getSpanId())
                    hasMoreSpans = attribute.next();
                else {
                    hasMoreSpans = referentSpans.next();
                }

                return false;
            }
        }
        return true;
    }


    /**
     * Ensures that the referentSpans do <em>not</em> contain the
     * <em>not attributes</em> (with negation). In other words, the
     * spanId must
     * not the same as the <em>not attribute</em>'s referentId.
     * 
     * @return <code>true</code> if the referentSpan does not have the
     *         same
     *         spanId as the referentIds of all the not attributes,
     *         <code>false</code> otherwise.
     * @throws IOException
     */
    private boolean checkNotReferentId (SimpleSpans referentSpans)
            throws IOException {
        for (AttributeSpans notAttribute : notAttributeList) {
            if (!notAttribute.isFinish()
                    && referentSpans.start() == notAttribute.start()
                    && referentSpans.getSpanId() == notAttribute.getSpanId()) {
                hasMoreSpans = referentSpans.next();
                return false;
            }
        }
        return true;
    }


    @Override
    public boolean skipTo (int target) throws IOException {
        if (hasMoreSpans && (referentSpans.doc() < target)) {
            if (!referentSpans.skipTo(target)) {
                return false;
            }
        }
        isStartEnumeration = false;
        return advance();
    }


    @Override
    public long cost () {

        long cost = 0;
        for (AttributeSpans as : attributeList) {
            cost += as.cost();
        }
        for (AttributeSpans as : notAttributeList) {
            cost += as.cost();
        }

        if (referentSpans != null)
            return referentSpans.cost() + cost;
        else
            return cost;

    }

}
