package de.ids_mannheim.korap.query.wrap;

import java.util.*;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.spans.SpanNotQuery;
import org.apache.lucene.search.spans.SpanOrQuery;
import de.ids_mannheim.korap.query.wrap.SpanRegexQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanWildcardQueryWrapper;
import de.ids_mannheim.korap.query.SpanSegmentQuery;

/**
 * @author Nils Diewald
 * @version 0.01
 *
 * Creates a query object for segments, i.e. terms in a term vector
 * sharing the same position. A SpanSegment can include simple string terms,
 * regular expressions and alternatives. These elements can also be excluded.
 */

public class SpanSegmentQueryWrapper implements SpanQueryWrapperInterface {
    public ArrayList<SpanQuery> inclusive;
    public ArrayList<SpanQuery> exclusive;
    private String field;
    private boolean isNull = true;

    /**
     * Constructor.
     *
     * @param field The field name
     */
    public SpanSegmentQueryWrapper (String field) {
	this.field = field;
	this.inclusive = new ArrayList<SpanQuery>();
	this.exclusive = new ArrayList<SpanQuery>();
    };

    /**
     * Constructor.
     *
     * @param field The field name
     * @param terms An arbitrary number of terms
     */
    public SpanSegmentQueryWrapper (String field, String ... terms) {
	this(field);
	for (int i = 0; i < terms.length; i++) {
	    this.inclusive.add((SpanQuery) new SpanTermQuery(new Term(field, terms[i])));
	    this.isNull = false;
	};
    };

    public SpanSegmentQueryWrapper (String field, SpanRegexQueryWrapper re) {
	this(field);
	this.inclusive.add((SpanQuery) re.toQuery());
	this.isNull = false;
    };

    public SpanSegmentQueryWrapper (String field, SpanAlterQueryWrapper alter) {
	this(field);
	if (!alter.isNull()) {
	    this.inclusive.add((SpanQuery) alter.toQuery());
	    this.isNull = false;
	};
    };

    public SpanSegmentQueryWrapper (String field, SpanSegmentQueryWrapper ssq) {
	this(field);

	if (!ssq.isNull()) {
	    Iterator<SpanQuery> clause = ssq.inclusive.iterator();
	    while (clause.hasNext()) {
		this.inclusive.add( (SpanQuery) clause.next().clone() );
	    };

	    clause = ssq.exclusive.iterator();
	    while (clause.hasNext()) {
		this.exclusive.add( (SpanQuery) clause.next().clone() );
	    };
	    this.isNull = false;
	};
    };

    public SpanSegmentQueryWrapper with (String term) {
	this.inclusive.add(new SpanTermQuery(new Term(field, term)));
	this.isNull = false;
	return this;
    };

    public SpanSegmentQueryWrapper with (SpanRegexQueryWrapper re) {
	this.inclusive.add((SpanQuery) re.toQuery());
	this.isNull = false;
	return this;
    };

    public SpanSegmentQueryWrapper with (SpanWildcardQueryWrapper wc) {
	this.inclusive.add((SpanQuery) wc.toQuery());
	this.isNull = false;
	return this;
    };

    public SpanSegmentQueryWrapper with (SpanAlterQueryWrapper alter) {
	if (!alter.isNull()) {
	    this.inclusive.add((SpanQuery) alter.toQuery());
	    this.isNull = false;
	};
	return this;
    };

    // Identical to without
    public SpanSegmentQueryWrapper with (SpanSegmentQueryWrapper seg) {
	if (!seg.isNull()) {
	    for (SpanQuery sq : seg.inclusive) {
		this.inclusive.add(sq);
	    };
	    for (SpanQuery sq : seg.exclusive) {
		this.exclusive.add(sq);
	    };
	    this.isNull = false;
	};
	return this;
    };

    public SpanSegmentQueryWrapper without (String term) {
	this.exclusive.add(new SpanTermQuery(new Term(field, term)));
	this.isNull = false;
	return this;
    };

    public SpanSegmentQueryWrapper without (SpanRegexQueryWrapper re) {
	this.exclusive.add((SpanQuery) re.toQuery());
	this.isNull = false;
	return this;
    };

    public SpanSegmentQueryWrapper without (SpanWildcardQueryWrapper wc) {
	this.exclusive.add((SpanQuery) wc.toQuery());
	this.isNull = false;
	return this;
    };

    public SpanSegmentQueryWrapper without (SpanAlterQueryWrapper alter) {
	if (!alter.isNull()) {
	    this.exclusive.add((SpanQuery) alter.toQuery());
	    this.isNull = false;
	};
	return this;
    };

    // Identical to with
    public SpanSegmentQueryWrapper without (SpanSegmentQueryWrapper seg) {
	if (!seg.isNull()) {
	    this.with(seg);
	    this.isNull = false;
	};
	return this;
    };

    public SpanQuery toQuery () {
	if (this.isNull || (this.inclusive.size() + this.exclusive.size() == 0)) {
	    return (SpanQuery) null;
	}
	else if (this.inclusive.size() >= 1 && this.exclusive.size() >= 1) {
	    return (SpanQuery) new SpanNotQuery(
		this._listToQuery(this.inclusive),
	        this._listToOrQuery(this.exclusive)
            );
	}

	else if (this.inclusive.size() == 0 && this.exclusive.size() >= 1) {

	    // Not supported anymore
	    // TODO: Raise error
	    return (SpanQuery) new SpanNotQuery(
		new SpanTermQuery(new Term(this.field, "T")),
	        this._listToOrQuery(this.exclusive)
            );
	}

	else if (this.inclusive.size() >= 1 && this.exclusive.size() == 0) {
	    return (SpanQuery) this._listToQuery(this.inclusive);
	};

	return (SpanQuery) null;
    };


    private SpanQuery _listToQuery (ArrayList<SpanQuery> list) {
	SpanQuery query = list.get(0);

	for (int i = 1; i < list.size(); i++) {
	    query = new SpanSegmentQuery(query, list.get(i));
	};

	return (SpanQuery) query;
    };


    private SpanQuery _listToOrQuery (ArrayList<SpanQuery> list) {
	if (list.size() == 1) {
	    return (SpanQuery) list.get(0);
	};

	Iterator<SpanQuery> clause = list.iterator();
	SpanOrQuery soquery = new SpanOrQuery( clause.next() );
	while (clause.hasNext()) {
	    soquery.addClause( clause.next() );
	};
	return (SpanQuery) soquery;
    };

    public SpanSegmentQueryWrapper clone () {
	return new SpanSegmentQueryWrapper(this.field, this);
    };

    public boolean isOptional () {
	return false;
    };

    public boolean isNull () {
	return this.isNull;
    };
};

