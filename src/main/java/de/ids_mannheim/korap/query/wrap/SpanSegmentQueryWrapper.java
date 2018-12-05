package de.ids_mannheim.korap.query.wrap;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.lucene.search.spans.SpanNotQuery;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;

import de.ids_mannheim.korap.query.SpanSegmentQuery;
import de.ids_mannheim.korap.util.QueryException;


/**
 * @author Nils Diewald
 * 
 *         Creates a query object for segments, i.e. terms in a term
 *         vector
 *         sharing the same position. A SpanSegment can include simple
 *         string terms,
 *         regular expressions and alternatives. These elements can
 *         also be excluded.
 */

public class SpanSegmentQueryWrapper extends SpanQueryWrapper {
    public ArrayList<SpanQueryWrapper> inclusive;
    public ArrayList<SpanQueryWrapper> exclusive;
    private String field;


    /**
     * Constructor.
     * 
     * @param field
     *            The field name
     */
    public SpanSegmentQueryWrapper (String field) {
        this.field = field;
        this.inclusive = new ArrayList<SpanQueryWrapper>();
        this.exclusive = new ArrayList<SpanQueryWrapper>();
    };


    /**
     * Constructor.
     * 
     * @param field
     *            The field name
     * @param terms
     *            An arbitrary number of terms
     */
    public SpanSegmentQueryWrapper (String field, String ... terms) {
        this(field);
        for (int i = 0; i < terms.length; i++) {
            this.inclusive.add(new SpanSimpleQueryWrapper(field, terms[i]));
            this.isNull = false;
        };
    };


    public SpanSegmentQueryWrapper (String field, SpanRegexQueryWrapper re) {
        this(field);
        this.inclusive.add(re);
        this.isNull = false;
    };


    public SpanSegmentQueryWrapper (String field, SpanAlterQueryWrapper alter) {
        this(field);
        if (!alter.isNull()) {
            if (alter.isNegative())
                this.isNegative = true;
            this.inclusive.add(alter);
            this.isNull = false;
        };
    };


    public SpanSegmentQueryWrapper (String field, SpanSegmentQueryWrapper ssq) {
        this(field);

        if (!ssq.isNull()) {
            Iterator<SpanQueryWrapper> clause = ssq.inclusive.iterator();
            while (clause.hasNext()) {
                this.inclusive.add((SpanQueryWrapper) clause.next());
                // .clone()
            };

            clause = ssq.exclusive.iterator();
            while (clause.hasNext()) {
                this.exclusive.add((SpanQueryWrapper) clause.next());
                // .clone()
            };
            this.isNull = false;
        };
    };


    public SpanSegmentQueryWrapper with (String term) {
        this.inclusive.add(new SpanSimpleQueryWrapper(field, term));
        this.isNull = false;
        return this;
    };


    public SpanSegmentQueryWrapper with (SpanQueryWrapper re) {
        this.inclusive.add((SpanQueryWrapper) re);
        this.isNull = false;
        return this;
    };


    public SpanSegmentQueryWrapper with (SpanWildcardQueryWrapper wc) {
        this.inclusive.add((SpanQueryWrapper) wc);
        this.isNull = false;
        return this;
    };


    public SpanSegmentQueryWrapper with (SpanAlterQueryWrapper alter) {
        if (!alter.isNull()) {
            if (alter.isNegative())
                this.isNegative = true;
            this.inclusive.add(alter);
            this.isNull = false;
        };
        return this;
    };


    // Identical to without
    public SpanSegmentQueryWrapper with (SpanSegmentQueryWrapper seg) {
        if (!seg.isNull()) {
            for (SpanQueryWrapper sq : seg.inclusive) {
                this.inclusive.add(sq);
            };
            for (SpanQueryWrapper sq : seg.exclusive) {
                this.exclusive.add(sq);
            };
            this.isNull = false;
        };
        return this;
    };


    public SpanSegmentQueryWrapper without (String term) {
        this.exclusive.add(new SpanSimpleQueryWrapper(field, term));
        this.isNull = false;
        return this;
    };


    // TODO: THESE MAYBE NOT NECESSARY:

    public SpanSegmentQueryWrapper without (SpanRegexQueryWrapper re) {
        this.exclusive.add(re);
        this.isNull = false;
        return this;
    };


    public SpanSegmentQueryWrapper without (SpanWildcardQueryWrapper wc) {
        this.exclusive.add(wc);
        this.isNull = false;
        return this;
    };


    public SpanSegmentQueryWrapper without (SpanAlterQueryWrapper alter) {
        if (!alter.isNull()) {
            if (alter.isNegative()) {
                this.inclusive.add(alter);
            }
            else {
                this.exclusive.add(alter);
            };
            this.isNull = false;
        };
        return this;
    };


    public SpanSegmentQueryWrapper without (SpanSegmentQueryWrapper seg) {
        if (!seg.isNull()) {
            this.with(seg);
            this.isNull = false;
        };
        isNegative = true;
        return this;
    };


    public SpanQuery toFragmentQuery () throws QueryException {
        if (this.isNull
                || (this.inclusive.size() + this.exclusive.size() == 0)) {
            return (SpanQuery) null;
        }
        else if (this.inclusive.size() >= 1 && this.exclusive.size() >= 1) {
            return (SpanQuery) new SpanNotQuery(
                    this._listToQuery(this.inclusive),
                    this._listToOrQuery(this.exclusive));
        }

        // These are now identical but may be negative
        else if (this.inclusive.size() == 0 && this.exclusive.size() >= 1) {
            return (SpanQuery) this._listToQuery(this.exclusive);
        }
        else if (this.inclusive.size() >= 1 && this.exclusive.size() == 0) {
            return (SpanQuery) this._listToQuery(this.inclusive);
        };

        return (SpanQuery) null;
    };


    private SpanQuery _listToQuery (ArrayList<SpanQueryWrapper> list)
            throws QueryException {
        SpanQuery query = list.get(0).toFragmentQuery();

        for (int i = 1; i < list.size(); i++) {
            query = new SpanSegmentQuery(query, list.get(i).toFragmentQuery());
        };

        return (SpanQuery) query;
    };


    private SpanQuery _listToOrQuery (ArrayList<SpanQueryWrapper> list)
            throws QueryException {
        if (list.size() == 1) {
            return (SpanQuery) list.get(0).toFragmentQuery();
        };

        Iterator<SpanQueryWrapper> clause = list.iterator();
        SpanOrQuery soquery = new SpanOrQuery(clause.next().toFragmentQuery());
        while (clause.hasNext()) {
            soquery.addClause(clause.next().toFragmentQuery());
        };
        return (SpanQuery) soquery;
    };


    public SpanSegmentQueryWrapper clone () {
        return new SpanSegmentQueryWrapper(this.field, this);
    };


    public boolean isOptional () {
        return false;
    };


    public boolean isNegative () {
        if (this.inclusive.size() == 0 && this.exclusive.size() >= 1) {
            return true;
        };
        return false;
    };


    public void makeNegative () {
        /*
          TODO: THIS IS A BIT MORE COMPLICATED!
          and and or groups have to be switched
         */

        this.exclusive.addAll(this.inclusive);
        this.inclusive.clear();
    };
};
