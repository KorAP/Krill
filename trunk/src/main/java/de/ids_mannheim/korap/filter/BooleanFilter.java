package de.ids_mannheim.korap.filter;

import java.util.*;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.RegexpQuery;
import org.apache.lucene.index.Term;

/*
  Todo: !not
*/

/**
 * @author Nil Diewald
 *
 * BooleanFilter implements a simple API for boolean operations
 * on constraints for KorapFilter.
 */
public class BooleanFilter {
    private String type;
    private Query query;

    public BooleanFilter (String type, Query query) {
	this.type = type;
	this.query = query;
    };

    public BooleanFilter or (String ... values) {
	BooleanQuery bool = new BooleanQuery();
	bool.add(this.query, BooleanClause.Occur.SHOULD);
	for (String val : values) {
	    bool.add(new TermQuery(new Term(this.type, val)), BooleanClause.Occur.SHOULD);
	};
	this.query = bool;
	return this;
    };

    public BooleanFilter or (RegexFilter value) {
	BooleanQuery bool = new BooleanQuery();
	bool.add(this.query, BooleanClause.Occur.SHOULD);
	bool.add(value.toQuery(this.type), BooleanClause.Occur.SHOULD);
	this.query = bool;
	return this;
    };

    
    public BooleanFilter and (String value) {
	BooleanQuery bool = new BooleanQuery();
	bool.add(this.query, BooleanClause.Occur.MUST);
	bool.add(new TermQuery(new Term(this.type, value)), BooleanClause.Occur.MUST);
	this.query = bool;
	return this;
    };

    public BooleanFilter and (RegexFilter value) {
	BooleanQuery bool = new BooleanQuery();
	bool.add(this.query, BooleanClause.Occur.MUST);
	bool.add(value.toQuery(this.type), BooleanClause.Occur.MUST);
	this.query = bool;
	return this;
    };
    
    public Query toQuery () {
	return this.query;
    };

    public String toString () {
	return this.query.toString();
    };
};
