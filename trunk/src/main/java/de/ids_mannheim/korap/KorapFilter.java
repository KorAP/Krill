package de.ids_mannheim.korap;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import de.ids_mannheim.korap.filter.BooleanFilter;
import de.ids_mannheim.korap.filter.RegexFilter;
import de.ids_mannheim.korap.util.KorapDate;
import org.apache.lucene.index.Term;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.lucene.search.NumericRangeQuery;


/*
  Todo: WildCardFilter!
  Support: delete boolean etc.
  Support: supports foundries
*/

/**
 * @author Nils Diewald
 *
 * KorapFilter implements a simple API for creating meta queries
 * constituing Virtual Collections.
 */

/*
<request>
  <query>
    ...XYZ...
  </query>
  <filter>
    <cond><foundry value="Treetagger" /></cond>
    <cond><foundry value="MATE" /></cond>
    <condOr>
      <cond><textClass value="sports" /></cond>
      <cond><textClass value="news" /></cond>
    </condOr>
    <cond><pubDate till="2009" /></cond>
    <cond><author regex="Peter .+?" /></cond>
  </filter>
</request>

Suche XYZ in allen Documenten in den Foundries "Treetagger" und "MATE", die entweder den Texttyp "sports" oder den Texttyp "news" haben, bis höchsten 2009 publiziert wurden und deren Autor auf den regulären Ausdruck "Peter .+?" matcht.

*/

public class KorapFilter {
    private KorapFilter filter;
    private Query query;

    // Logger
    private final static Logger jlog = LoggerFactory.getLogger(KorapFilter.class);

    /**
     * Search for documents of a specific genre.
     * @param genre The name of the genre as a string
     */
    public BooleanFilter genre (String genre) {
	return new BooleanFilter("textClass", new TermQuery(
            new Term("textClass", genre)
        ));
    };

    /**
     * Search for documents of specific genres.
     * @param genre The name of the genres as a regular expression.
     */
    public BooleanFilter genre (RegexFilter genre) {
	return new BooleanFilter("textClass", genre.toQuery("textClass"));
    };

    /**
     * Search for a documents of specific genres.
     * @param genre The name of the genre as a string
     * @param genres The names of further genres as strings
     *
     * This method is EXPERIMENTAL and may change without warnings!
     */
    public BooleanFilter genre (String genre, String ... genres) {
	BooleanFilter bf = new BooleanFilter("textClass", new TermQuery(
            new Term("textClass", genre)
        ));
	bf = bf.or(genres);
	return bf;
    };

    public RegexFilter re (String value) {
	return new RegexFilter(value);
    };

    public Query since (String date) {
	int since = new KorapDate(date).floor();
	if (since == 0 || since == KorapDate.BEGINNING)
	    return (Query) null;

	return NumericRangeQuery.newIntRange("pubDate", since, KorapDate.END, true, true);
    };


    public Query till (String date) {
	try {
	    int till =  new KorapDate(date).ceil();
	    if (till == 0 || till == KorapDate.END)
		return (Query) null;

	    return NumericRangeQuery.newIntRange("pubDate", KorapDate.BEGINNING, till, true, true);
	}
	catch (NumberFormatException e) {
	    jlog.warn("Parameter of till(date) is invalid");
	};
	return (Query) null;
    };


    public Query between (String beginStr, String endStr) {
	KorapDate beginDF = new KorapDate(beginStr);

	int begin = beginDF.floor();

	int end = new KorapDate(endStr).ceil();

	if (end == 0)
	    return (Query) null;

	if (begin == KorapDate.BEGINNING && end == KorapDate.END)
	    return (Query) null;

	if (begin == end) {
	    return new TermQuery(new Term("pubDate", beginDF.toString()));
	};

	return NumericRangeQuery.newIntRange("pubDate", begin, end, true, true);
    };


    public Query date (String date) {
	KorapDate dateDF = new KorapDate(date);

	if (dateDF.year() == 0)
	    return (Query) null;

	if (dateDF.day() == 0 || dateDF.month() == 0) {
	    int begin = dateDF.floor();
	    int end = dateDF.ceil();

	    if (end == 0 || (begin == KorapDate.BEGINNING && end == KorapDate.END))
		return (Query) null;
	    
	    return NumericRangeQuery.newIntRange("pubDate", begin, end, true, true);
	};
	
	return new TermQuery(new Term("pubDate", dateDF.toString()));
    };


    /*
textClass
id
title
subtitle
author
corpus
pubDate
pubPlace
    */


};
