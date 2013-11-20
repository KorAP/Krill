package de.ids_mannheim.korap;

import de.ids_mannheim.korap.filter.BooleanFilter;
import de.ids_mannheim.korap.filter.RegexFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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

textClass
ID
title
subTitle
author
corpusID
pubDate
pubPlace

*/

public class KorapFilter {
    private BooleanFilter filter;

    // Logger
    private final static Logger jlog = LoggerFactory.getLogger(KorapFilter.class);

    public BooleanFilter and (String type, String ... terms) {
	BooleanFilter bf = new BooleanFilter();
	bf.and(type, terms);
	return bf;
    };

    public BooleanFilter or (String type, String ... terms) {
	BooleanFilter bf = new BooleanFilter();
	bf.or(type, terms);
	return bf;
    };

    public BooleanFilter and (String type, RegexFilter re) {
	BooleanFilter bf = new BooleanFilter();
	bf.and(type, re);
	return bf;
    };

    public BooleanFilter or (String type, RegexFilter re) {
	BooleanFilter bf = new BooleanFilter();
	bf.or(type, re);
	return bf;
    };

    public BooleanFilter since (String date) {
	BooleanFilter bf = new BooleanFilter();
	bf.since(date);
	return bf;
    };

    public BooleanFilter till (String date) {
	BooleanFilter bf = new BooleanFilter();
	bf.till(date);
	return bf;
    };

    public BooleanFilter date (String date) {
	BooleanFilter bf = new BooleanFilter();
	bf.date(date);
	return bf;
    };

    public BooleanFilter between (String date1, String date2) {
	BooleanFilter bf = new BooleanFilter();
	bf.between(date1, date2);
	return bf;
    };

    public RegexFilter re (String regex) {
	return new RegexFilter(regex);
    };
};
