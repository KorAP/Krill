package de.ids_mannheim.korap.query.wrap;

import de.ids_mannheim.korap.query.wrap.SpanRegexQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanWildcardQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanSegmentQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanQueryWrapperInterface;

import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.index.Term;

import java.util.*;

public class SpanAlterQueryWrapper implements SpanQueryWrapperInterface {
    private String field;
    private SpanQuery query;
    private List<SpanQuery> alternatives;
    private boolean isNull = true;

    public SpanAlterQueryWrapper (String field) {
	this.field = field;
	this.alternatives = new ArrayList<>();
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
	this.alternatives.add(new SpanTermQuery(new Term(this.field, term)));
	this.isNull = false;
	return this;
    };

    public SpanAlterQueryWrapper or (SpanQueryWrapperInterface term) {
	if (term.isNull())
	    return this;
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

    public boolean isOptional () {
	return false;
    };

    public boolean isNull () {
	return this.isNull;
    };
};
