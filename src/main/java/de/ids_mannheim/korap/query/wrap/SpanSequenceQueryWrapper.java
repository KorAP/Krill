package de.ids_mannheim.korap.query.wrap;

import java.util.*;
import de.ids_mannheim.korap.query.SpanNextQuery;
import de.ids_mannheim.korap.query.wrap.SpanSegmentQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanRegexQueryWrapper;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import de.ids_mannheim.korap.query.wrap.SpanQueryWrapperInterface;

/**
 * @author Nils Diewald
 */
public class SpanSequenceQueryWrapper implements SpanQueryWrapperInterface {
    private String field;
    public ArrayList<SpanQuery> segments;

    public SpanSequenceQueryWrapper (String field) {
	this.field = field;
	this.segments = new ArrayList<SpanQuery>();
    };

    public SpanSequenceQueryWrapper (String field, String ... terms) {
	this(field);
	for (int i = 0; i < terms.length; i++) {
	    this.segments.add((SpanQuery) new SpanTermQuery(new Term(field, terms[i])));
	};
    };

    public SpanSequenceQueryWrapper (String field, SpanQuery sq) {
	this(field);
	this.segments.add((SpanQuery) sq);
    };

    public SpanSequenceQueryWrapper (String field, SpanQueryWrapperInterface sswq) {
	this(field);
	this.segments.add((SpanQuery) sswq.toQuery());
    };

    public SpanSequenceQueryWrapper (String field, SpanRegexQueryWrapper re) {
	this(field);
	this.segments.add((SpanQuery) re.toQuery());
    };

    public SpanQuery get (int index) {
	return this.segments.get(index);
    };

    public void set (int index, SpanQuery sq) {
	this.segments.set(index, sq);
    };

    public SpanSequenceQueryWrapper append (String term) {
	this.segments.add((SpanQuery) new SpanTermQuery(new Term(field, term)));
	return this;
    };

    public SpanSequenceQueryWrapper append (SpanQueryWrapperInterface ssq) {
	this.segments.add((SpanQuery) ssq.toQuery());
	return this;
    };

    public SpanSequenceQueryWrapper append (SpanRegexQueryWrapper srqw) {
	this.segments.add((SpanQuery) srqw.toQuery());
	return this;
    };

    public SpanSequenceQueryWrapper prepend (String term) {
	this.segments.add(0, (SpanQuery) new SpanTermQuery(new Term(field, term)));
	return this;
    };

    public SpanSequenceQueryWrapper prepend (SpanSegmentQueryWrapper ssq) {
	this.segments.add(0, (SpanQuery) ssq.toQuery());
	return this;
    };

    public SpanSequenceQueryWrapper prepend (SpanRegexQueryWrapper re) {
	this.segments.add(0, (SpanQuery) re.toQuery());
	return this;
    };

    public SpanQuery toQuery () {
	if (this.segments.size() == 0) {
	    return (SpanQuery) null;
	};

	SpanQuery query = this.segments.get(0);

	for (int i = 1; i < this.segments.size(); i++) {
	    query = new SpanNextQuery(
		query,
	        this.segments.get(i),
		false
            );
	};
	return (SpanQuery) query;
    };
};
