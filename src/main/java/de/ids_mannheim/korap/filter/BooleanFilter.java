package de.ids_mannheim.korap.filter;

import java.util.*;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.RegexpQuery;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.NumericRangeQuery;

import de.ids_mannheim.korap.util.KorapDate;
import de.ids_mannheim.korap.filter.RegexFilter;
import de.ids_mannheim.korap.KorapFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/*
  Todo: !not
*/

/**
 * @author Nils Diewald
 *
 * BooleanFilter implements a simple API for boolean operations
 * on constraints for KorapFilter.
 */
public class BooleanFilter {
    private String type;

    // Logger
    private final static Logger jlog = LoggerFactory.getLogger(KorapFilter.class);


    private BooleanQuery bool;

    public BooleanFilter () {
	bool = new BooleanQuery();
    };

    public BooleanFilter or (String type, String ... terms) {
	for (String term : terms) {
	    bool.add(
	        new TermQuery(new Term(type, term)),
		BooleanClause.Occur.SHOULD
	    );
	};
	return this;
    };

    public BooleanFilter or (String type, RegexFilter value) {
	bool.add(
          value.toQuery(type),
          BooleanClause.Occur.SHOULD
        );
	return this;
    };

    public BooleanFilter or (BooleanFilter bf) {
	bool.add(
 	    bf.toQuery(),
	    BooleanClause.Occur.SHOULD
        );
	return this;
    };

    public BooleanFilter or (NumericRangeQuery<Integer> nrq) {
	bool.add(nrq, BooleanClause.Occur.SHOULD);
	return this;
    };

    public BooleanFilter and (String type, String ... terms) {
	for (String term : terms) {
	    bool.add(
	        new TermQuery(new Term(type, term)),
		BooleanClause.Occur.MUST
	    );
	};
	return this;
    };

    public BooleanFilter and (String type, RegexFilter value) {
	bool.add(
          value.toQuery(type),
          BooleanClause.Occur.MUST
        );
	return this;
    };

    public BooleanFilter and (BooleanFilter bf) {
	bool.add(
 	    bf.toQuery(),
	    BooleanClause.Occur.MUST
        );
	return this;
    };

    public BooleanFilter since (String date) {
	int since = new KorapDate(date).floor();

	if (since == 0 || since == KorapDate.BEGINNING)
	    return this;

	bool.add(
	    NumericRangeQuery.newIntRange(
	        "pubDate",
		since,
		KorapDate.END,
		true,
		true
	    ),
	    BooleanClause.Occur.MUST
	);

	return this;
    };


    public BooleanFilter till (String date) {
	try {
	    int till =  new KorapDate(date).ceil();
	    if (till == 0 || till == KorapDate.END)
		return this;

	    bool.add(
                NumericRangeQuery.newIntRange(
  	            "pubDate",
                    KorapDate.BEGINNING,
                    till,
                    true,
                    true
                ),
	        BooleanClause.Occur.MUST
	    );
	}
	catch (NumberFormatException e) {
	    jlog.warn("Parameter of till(date) is invalid");
	};
	return this;
    };


    public BooleanFilter between (String beginStr, String endStr) {
	KorapDate beginDF = new KorapDate(beginStr);

	int begin = beginDF.floor();

	int end = new KorapDate(endStr).ceil();

	if (end == 0)
	    return this;

	if (begin == KorapDate.BEGINNING && end == KorapDate.END)
	    return this;

	if (begin == end) {
	    this.and("pubDate", beginDF.toString());
	    return this;
	};

	this.bool.add(
	    NumericRangeQuery.newIntRange(
	        "pubDate",
		begin,
		end,
		true,
		true
	    ),
	    BooleanClause.Occur.MUST
        );
	return this;
    };


    public BooleanFilter date (String date) {
	KorapDate dateDF = new KorapDate(date);

	if (dateDF.year() == 0)
	    return this;

	if (dateDF.day() == 0 || dateDF.month() == 0) {
	    int begin = dateDF.floor();
	    int end = dateDF.ceil();

	    if (end == 0 || (begin == KorapDate.BEGINNING && end == KorapDate.END))
		return this;
	    
	    this.bool.add(
	        NumericRangeQuery.newIntRange(
		    "pubDate",
		    begin,
		    end,
		    true,
		    true
		),
		BooleanClause.Occur.MUST
	    );
	    return this;
	};
	
	this.and("pubDate", dateDF.toString());
	return this;
    };

    
    public Query toQuery () {
	return this.bool;
    };

    public String toString () {
	return this.bool.toString();
    };
};
