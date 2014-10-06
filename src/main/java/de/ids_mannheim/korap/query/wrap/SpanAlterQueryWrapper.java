package de.ids_mannheim.korap.query.wrap;

import de.ids_mannheim.korap.query.wrap.SpanRegexQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanWildcardQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanSegmentQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanQueryWrapper;

import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.index.Term;

import java.util.*;

public class SpanAlterQueryWrapper extends SpanQueryWrapper {
    private String field;
    private SpanQuery query;
    private List<SpanQuery> alternatives;

    public SpanAlterQueryWrapper (String field) {
	this.field = field;
	this.alternatives = new ArrayList<>();
    };

    public SpanAlterQueryWrapper (String field, SpanQuery query) {
	this.field = field;
	this.alternatives = new ArrayList<>();
	this.alternatives.add(query);
    };

    public SpanAlterQueryWrapper (String field, String ... terms) {
	this.field = field;
	this.alternatives = new ArrayList<>();
	for (String term : terms) {
	    this.isNull = false;
	    this.alternatives.add(new SpanTermQuery(new Term(this.field, term)));
	};
    };

    public SpanAlterQueryWrapper or (String term) {
	return this.or(new SpanTermQuery(new Term(this.field, term)));
    };

    public SpanAlterQueryWrapper or (SpanQuery query) {
	this.alternatives.add(query);
	this.isNull = false;
	return this;
    };

    public SpanAlterQueryWrapper or (SpanQueryWrapper term) {
	if (term.isNull())
	    return this;

	if (term.isNegative())
	    this.isNegative = true;

	// If one operand is optional, the whole group can be optional
	// a | b* | c
	if (term.isOptional())
	    this.isOptional = true;

	this.alternatives.add( term.toQuery() );
	this.isNull = false;
	return this;
    };

    public SpanAlterQueryWrapper or (SpanRegexQueryWrapper term) {
	this.alternatives.add( term.toQuery() );
	this.isNull = false;
	return this;
    };

    public SpanAlterQueryWrapper or (SpanWildcardQueryWrapper wc) {
	this.alternatives.add( wc.toQuery() );
	this.isNull = false;
	return this;
    };

    public SpanQuery toQuery() {
	if (this.isNull || this.alternatives.size() == 0)
	    return (SpanQuery) null;
	    
	if (this.alternatives.size() == 1) {
	    return (SpanQuery) this.alternatives.get(0);
	};

	Iterator<SpanQuery> clause = this.alternatives.iterator();
	SpanOrQuery soquery = new SpanOrQuery( clause.next() );
	while (clause.hasNext()) {
	    soquery.addClause( clause.next() );
	};
	return (SpanQuery) soquery;
    };
};
