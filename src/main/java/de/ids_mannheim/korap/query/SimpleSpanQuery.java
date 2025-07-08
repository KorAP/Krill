package de.ids_mannheim.korap.query;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;

import de.ids_mannheim.korap.query.spans.AttributeSpans;
import de.ids_mannheim.korap.query.spans.ElementSpans;

/**
 * A base class for Spanqueries. It added some properties and methods
 * to the
 * Lucene {@link SpanQuery} class.
 * 
 * The constructors of this class specify three kinds of spanqueries:
 * <br/>
 * <br/>
 * 
 * <ol>
 * <li>Term span based queries are spanqueries retrieving spans based
 * on a
 * single sub/child spanquery. <br/>
 * This kind of query is similar to the Lucene {@link SpanTermQuery}.
 * It
 * searches for term spans in an index and creates a span enumeration
 * of them.
 * Additionally, the retrieved spans contain some information related
 * to the
 * type of the term spans, or modified the term span positions.
 * 
 * For instance, a {@link SpanAttributeQuery} retrieves
 * {@link AttributeSpans},
 * which in addition to the Lucene SpanTermQuery properties, also have
 * references to element or relation spans. <br/>
 * <br/>
 * </li>
 * 
 * <li>Spanqueries based on two sub/child spanqueries. <br/>
 * These queries compare the positions or other properties of two sub
 * spanqueries. Examples of such queries are distance-based queries
 * calculating
 * the distance between two sub/child spans. The resulting spans
 * possibly
 * stretch from the start position of a sub/child span to the end
 * position of
 * the other sub/child span. <br/>
 * <br/>
 * </li>
 * 
 * <li>Spanqueries comparing a sub/child spanquery to a list of
 * spanqueries. <br/>
 * An example of such queries is {@link SpanWithAttributeQuery}
 * matching an {@link SpanElementQuery} and a list of
 * SpanAttributeQueries. In other words,
 * it retrieves {@link ElementSpans} having some specific
 * attributes.<br/>
 * <br/>
 * </li>
 * </ol>
 * 
 * @see SpanQuery
 * 
 * @author margaretha
 */
public abstract class SimpleSpanQuery extends SpanQuery implements Cloneable {

    protected SpanQuery firstClause = null, secondClause = null;
    protected List<SpanQuery> clauseList = null;
    protected String field;
    protected boolean collectPayloads;
    protected boolean isFieldNull = false;
    // private Logger log = Logger.getLogger(SimpleSpanQuery.class);
    private final static Logger log = LoggerFactory.getLogger(SimpleSpanQuery.class);


    public SimpleSpanQuery () {}


    /**
     * Constructs a new SimpleSpanQuery using the specified
     * {@link SpanQuery} and set whether payloads are to be collected
     * or not.
     * 
     * @param firstClause
     *            a {@link SpanQuery}
     * @param collectPayloads
     *            a boolean flag representing the value
     *            <code>true</code> if payloads are to be collected,
     *            otherwise
     *            <code>false</code>.
     */
    public SimpleSpanQuery (SpanQuery firstClause, boolean collectPayloads) {
        if (firstClause == null) {
            throw new IllegalArgumentException(
                    "The first clause cannot be null.");
        }
        this.field = firstClause.getField();
        if (field == null){
            isFieldNull = true;
            log .warn("Field is null for "+ firstClause.toString());
        }
        this.setFirstClause(firstClause);
        this.collectPayloads = collectPayloads;
    }


    /**
     * Constructs a new SimpleSpanQuery using the specified
     * spanqueries and set
     * whether payloads are to be collected or not.
     * 
     * @param firstClause
     *            a {@link SpanQuery}
     * @param secondClause
     *            a {@link SpanQuery}
     * @param collectPayloads
     *            a boolean flag representing the value
     *            <code>true</code> if payloads are to be collected,
     *            otherwise
     *            <code>false</code>.
     */
    public SimpleSpanQuery (SpanQuery firstClause, SpanQuery secondClause,
                            boolean collectPayloads) {
        this(firstClause, collectPayloads);
        if (secondClause == null) {
            throw new IllegalArgumentException(
                    "The second clause cannot be null.");
        }
        checkField(secondClause);
        this.setSecondClause(secondClause);
    }


    /**
     * Constructs a new SimpleSpanQuery using the spanqueries in the
     * specified
     * list and set whether payloads are to be collected or not.
     * 
     * @param firstClause
     *            a {@link SpanQuery}
     * @param secondClauses
     *            a list of spanqueries
     * @param collectPayloads
     *            a boolean flag representing the value
     *            <code>true</code> if payloads are to be collected,
     *            otherwise
     *            <code>false</code>.
     */
    public SimpleSpanQuery (SpanQuery firstClause,
                            List<SpanQuery> secondClauses,
                            boolean collectPayloads) {
        this(firstClause, collectPayloads);
        setClauseList(secondClauses);
    }


    public SimpleSpanQuery (List<SpanQuery> clauses, boolean collectPayloads) {
        this.collectPayloads = collectPayloads;
        setClauseList(clauses);
    }


    private void checkField (SpanQuery clause) {
        String field = clause.getField();
        if (field == null){
            log .warn("Field is null for "+ secondClause.toString());
            isFieldNull = true;
        }
        else if (!isFieldNull && !clause.getField().equals(field)) {
            throw new IllegalArgumentException(
                    "Clauses must have the same field.");
        }
    }


    /**
     * Returns a set of child spanqueries used in this query.
     * 
     * @return a list of spanqueries
     */
    public List<SpanQuery> getClauseList () {
        return clauseList;
    }


    /**
     * Sets a list of child spanqueries.
     * 
     * @param clauseList
     *            a list of spanqueries
     */
    public void setClauseList (List<SpanQuery> clauses) {
        if (clauses == null) {
            throw new IllegalArgumentException(
                    "The list of clauses cannot be null.");
        }
        if (clauses.size() < 1) {
            throw new IllegalArgumentException(
                    "The list of clauses cannot be empty.");
        }

        if (this.field == null) {
            this.field = clauses.get(0).getField();
        }

        for (SpanQuery clause : clauses) {
            if (clause == null) {
                throw new IllegalArgumentException("A clause cannot be null.");
            }
            checkField(clause);
        }
        this.clauseList = clauses;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getField () {
        return field;
    }


    /**
     * Returns the first child {@link SpanQuery}.
     * 
     * @return the first child {@link SpanQuery}.
     */
    public SpanQuery getFirstClause () {
        return firstClause;
    }


    /**
     * Sets the first child {@link SpanQuery}.
     * 
     * @param firstClause
     *            the first child {@link SpanQuery}.
     */
    public void setFirstClause (SpanQuery firstClause) {
        this.firstClause = firstClause;
    }


    /**
     * Returns the second child {@link SpanQuery}.
     * 
     * @return the second child {@link SpanQuery}.
     */
    public SpanQuery getSecondClause () {
        return secondClause;
    }


    /**
     * Sets the second child {@link SpanQuery}.
     * 
     * @param secondClause
     *            the second child {@link SpanQuery}.
     */
    public void setSecondClause (SpanQuery secondClause) {
        this.secondClause = secondClause;
    }


    /**
     * Tells if payloads are to be collected or not.
     * 
     * @return <code>true</code> if payloads are to be collected,
     *         <code>false</code> otherwise.
     */
    public boolean isCollectPayloads () {
        return collectPayloads;
    }


    /**
     * Sets <code>true</code> if payloads are to be collected,
     * <code>false</code> otherwise.
     * 
     * @param collectPayloads
     *            a boolean flag determining if payloads are to be
     *            collected or not.
     */
    public void setCollectPayloads (boolean collectPayloads) {
        this.collectPayloads = collectPayloads;
    }


    // For rewriting fuzzy searches like wildcard and regex
    /** {@inheritDoc} */
    @Override
    public void extractTerms (Set<Term> terms) {

        if (terms == null) {
            throw new IllegalArgumentException("The term set cannot be null.");
        }

        if (firstClause != null) {
            firstClause.extractTerms(terms);
        }

        if (secondClause != null) {
            secondClause.extractTerms(terms);
        }
        else if (clauseList != null) {
            for (SpanQuery clause : clauseList) {
                clause.extractTerms(terms);
            }
        }
    };


    /** {@inheritDoc} */
    @Override
    public Query rewrite (IndexReader reader) throws IOException {
        SimpleSpanQuery clone = null;
        if (firstClause != null) {
            clone = updateClone(reader, clone, firstClause, 1);
        }
        if (secondClause != null) {
            clone = updateClone(reader, clone, secondClause, 2);
        }
        else if (clauseList != null) {
            clone = updateClone(reader, clone, clauseList);
        }
        return (clone != null ? clone : this);
    }


    /**
     * Rewrites the spanqueries from the specified list, sets them to
     * the clone,
     * and return the clone.
     * 
     * @param reader
     * @param clone
     * @param spanQueries
     * @return a SimpleSpanQuery
     * @throws IOException
     */
    private SimpleSpanQuery updateClone (IndexReader reader,
            SimpleSpanQuery clone, List<SpanQuery> spanQueries)
            throws IOException {

        for (int i = 0; i < spanQueries.size(); i++) {
            final SpanQuery query = (SpanQuery) spanQueries.get(i)
                    .rewrite(reader);
            if (!query.equals(spanQueries.get(i))) {
                if (clone == null)
                    clone = clone();
                clone.getClauseList().set(i, query);
            }
        }
        return clone;
    }


    /**
     * Rewrites the specified {@link SpanQuery} and sets it either as
     * the first
     * or the second child {@link SpanQuery} of the clone.
     * 
     * @param reader
     * @param clone
     * @param sq
     * @param clauseNumber
     * @return a SimpleSpanQuery
     * @throws IOException
     */
    private SimpleSpanQuery updateClone (IndexReader reader,
            SimpleSpanQuery clone, SpanQuery sq, int clauseNumber)
            throws IOException {
        final SpanQuery query = (SpanQuery) sq.rewrite(reader);
        if (!query.equals(sq)) {
            if (clone == null)
                clone = clone();
            if (clauseNumber == 1)
                clone.firstClause = query;
            else
                clone.secondClause = query;
        }
        return clone;
    }


    /**
     * {@inheritDoc}
     */
    // Used in rewriting query
    @Override
    public boolean equals (Object o) {
        if (this == o)
            return true;
        if (getClass() != o.getClass())
            return false;

        final SimpleSpanQuery q = (SimpleSpanQuery) o;
        if (collectPayloads != q.collectPayloads)
            return false;
        if (!firstClause.equals(q.firstClause))
            return false;
        if (secondClause != null && !secondClause.equals(q.secondClause)) {
            return false;
        }
        else if (clauseList != null) {
            for (int i = 0; i < clauseList.size(); i++) {
                if (!clauseList.get(i).equals(q.getClauseList().get(i))) {
                    return false;
                }
            }
        }

        return true;
    };


    /** {@inheritDoc} */
    @Override
    public int hashCode () {
        int hc = 0;
        if (firstClause != null)
            hc += firstClause.hashCode();

        if (secondClause != null) {
            hc += secondClause.hashCode();
        }
        else if (clauseList != null) {
            for (int i = 0; i < clauseList.size(); i++) {
                hc += clauseList.get(i).hashCode();
            }
        }
        hc ^= (hc << 31) | (hc >>> 3);
        hc += Float.floatToRawIntBits(getBoost());
        return hc;
    };


    public abstract SimpleSpanQuery clone ();

}
