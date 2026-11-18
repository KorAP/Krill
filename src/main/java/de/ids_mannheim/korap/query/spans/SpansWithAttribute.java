package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
    // The not-attribute has (more) positions in the current document
    private boolean[] notAttributeInDoc;

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
     * @param weights
     * @throws IOException
     */
    public SpansWithAttribute (SpanWithAttributeQuery spanWithAttributeQuery,
                               SimpleSpans referentSpans,
                               LeafReaderContext context, Bits acceptDocs,
                               SpanWeights weights)
            throws IOException {
        super(spanWithAttributeQuery, context, acceptDocs, weights);
        // if (!referentSpans.hasSpanId) {
        // throw new
        // IllegalArgumentException("Referent spans must have ids.");
        // }
        this.referentSpans = referentSpans;
        setAttributeList(spanWithAttributeQuery, context, acceptDocs,
                weights);
    }


    // if there is no (positive) attributes, but there are *not attributes*
    // hasmorespan = true
    public SpansWithAttribute (SpanWithAttributeQuery spanWithAttributeQuery,
                               LeafReaderContext context, Bits acceptDocs,
                               SpanWeights weights)
            throws IOException {
        super(spanWithAttributeQuery, context, acceptDocs, weights);
        setAttributeList(spanWithAttributeQuery, context, acceptDocs,
                weights);
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
            SpanWeights weights) throws IOException {

        attributeList = new ArrayList<AttributeSpans>();
        notAttributeList = new ArrayList<AttributeSpans>();

        List<SpanQuery> attributeList = swaq.getClauseList();
        if (swaq.isMultipleAttributes) {
            if (attributeList != null) {
                for (SpanQuery sq : attributeList) {
                    addAttributes((SpanAttributeQuery) sq, context, acceptDocs,
                            weights);
                }
            }
            else {
                throw new NullPointerException("Attribute list is null.");
            }
        }
        else if (swaq.getSecondClause() != null) {
            addAttributes((SpanAttributeQuery) swaq.getSecondClause(), context,
                    acceptDocs, weights);
        }
        else if (swaq.getType().equals("spanWithAttribute")
                && swaq.getFirstClause() != null) {
            addAttributes((SpanAttributeQuery) swaq.getFirstClause(), context,
                    acceptDocs, weights);
        }
        else {
            throw new NullPointerException("No attribute is defined.");
        }
        notAttributeInDoc = new boolean[notAttributeList.size()];
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
     * @param weights
     * @throws IOException
     */
    private void addAttributes (SpanAttributeQuery sq,
            LeafReaderContext context, Bits acceptDocs,
            SpanWeights weights) throws IOException {

        AttributeSpans as = (AttributeSpans) weights.getSpans(sq, context, acceptDocs);
        if (sq.isNegation()) {
            notAttributeList.add(as);
        }
        else {
            attributeList.add(as);
        }
    }


    @Override
    protected boolean usesIteratorApi () {
        return true;
    };


    // With a single (positive) attribute and no referent spans,
    // the attribute spans are the referent spans
    @Override
    protected LegacySpans driver () {
        return referentSpans != null ? referentSpans : attributeList.get(0);
    };


    @Override
    protected List<? extends LegacySpans> requiredOperands () {
        if (referentSpans == null)
            return Collections.emptyList();
        return attributeList;
    };


    @Override
    protected void startDocument () throws IOException {
        int doc = driver().docID();

        // The attributes were moved to the document, but not to a
        // position in it yet
        if (referentSpans != null) {
            for (AttributeSpans a : attributeList) {
                if (hasMoreSpans)
                    hasMoreSpans = nextInDocument(a);
            };
        };

        // The not-attributes are optional: only those having
        // positions in this document are considered
        for (int i = 0; i < notAttributeList.size(); i++) {
            AttributeSpans a = notAttributeList.get(i);
            if (a.docID() < doc)
                a.advance(doc);
            notAttributeInDoc[i] = a.docID() == doc && nextInDocument(a);
        };
    };


    @Override
    protected boolean nextMatchInDocument () throws IOException {
        isStartEnumeration = false;
        matchPayload.clear();
        if (referentSpans == null) { // only one (positive) attribute
            return advanceAttribute();
        }
        else {
            return advanceReferent();
        }
    }


    private boolean advanceAttribute () throws IOException {
        SimpleSpans referentSpans = attributeList.get(0);
        while (hasMoreSpans) {
            advanceNotAttributes(referentSpans);
            if (checkNotReferentId(referentSpans)) {
                setMatch(referentSpans);
                if (this.spanId > 0) {
                    hasSpanId = true;
                }
                hasMoreSpans = nextInDocument(referentSpans);
                return true;
            }
        }
        return false;
    }


    /**
     * Searches for the next match in the current document by first
     * identify a possible element position, and then ensuring that
     * the element contains all the attributes and <em>do not</em>
     * contain any of the not attributes.
     * 
     * @return <code>true</code> if the a match is found,
     *         <code>false</code>
     *         otherwise.
     * @throws IOException
     */
    private boolean advanceReferent () throws IOException {

        while (hasMoreSpans && searchSpanPosition()) {
            if (checkReferentId() && checkNotReferentId(referentSpans)) {
                setMatch(referentSpans);

                if (attributeList.size() > 0)
                    hasMoreSpans = nextInDocument(attributeList.get(0));

                hasMoreSpans &= nextInDocument(referentSpans);
                return true;
            }
        }
        return false;
    }


    // The current position of the referent spans is a match
    private void setMatch (SimpleSpans referentSpans) throws IOException {
        this.matchDocNumber = referentSpans.docID();
        this.matchStartPosition = referentSpans.startPosition();
        this.matchEndPosition = referentSpans.endPosition();
        this.matchPayload.addAll(referentSpans.getPayload());
        this.spanId = referentSpans.getSpanId();
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
                if (attributeList.size() == 0) {
                    return true;
                }
                hasMoreSpans = nextInDocument(referentSpans);
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
        int currentPosition = referentSpans.startPosition();
        boolean isSame = true;
        boolean isFirst = true;

        for (AttributeSpans a : attributeList) {
            if (!ensureSamePosition(referentSpans, a))
                return false;
            //	    logger.info("pos:" + withAttributeSpans.start());
            if (isFirst) {
                isFirst = false;
                currentPosition = referentSpans.startPosition();
            }
            else if (currentPosition != referentSpans.startPosition()) {
                currentPosition = referentSpans.startPosition();
                isSame = false;

            }
        }
        //    logger.info("same pos: "+isSame+ ", pos "+withAttributeSpans.start());
        return isSame;
    }


    /**
     * Advances the element or attribute spans in the current
     * document to be in the same start and end position.
     */
    private boolean ensureSamePosition (SimpleSpans spans,
            AttributeSpans attributes) throws IOException {

        while (hasMoreSpans) {
            if (attributes.startPosition() == spans.startPosition()
                    && attributes.endPosition() == spans.endPosition())
                return true;
            else if (attributes.startPosition() >= spans.startPosition())
                hasMoreSpans = nextInDocument(spans);
            else
                hasMoreSpans = nextInDocument(attributes);
        }

        return false;
    }


    /**
     * Advances the <em>not-attributes</em> in the current document
     * to be in the same as or greater start position than the
     * current referentSpan.
     * 
     * @throws IOException
     */
    private void advanceNotAttributes (LegacySpans referentSpans)
            throws IOException {
        for (int i = 0; i < notAttributeList.size(); i++) {
            AttributeSpans a = notAttributeList.get(i);
            while (notAttributeInDoc[i]
                    && a.startPosition() < referentSpans.startPosition()) {
                notAttributeInDoc[i] = nextInDocument(a);
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
                    hasMoreSpans = nextInDocument(attribute);
                else {
                    hasMoreSpans = nextInDocument(referentSpans);
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
        for (int i = 0; i < notAttributeList.size(); i++) {
            AttributeSpans notAttribute = notAttributeList.get(i);
            if (notAttributeInDoc[i]
                    && referentSpans.startPosition() == notAttribute
                            .startPosition()
                    && referentSpans.getSpanId() == notAttribute.getSpanId()) {
                hasMoreSpans = nextInDocument(referentSpans);
                return false;
            }
        }
        return true;
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
