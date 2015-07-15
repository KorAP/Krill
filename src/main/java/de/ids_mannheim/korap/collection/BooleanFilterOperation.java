package de.ids_mannheim.korap.collection;

import java.util.*;

import org.apache.lucene.index.Term;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.queries.BooleanFilter;
import org.apache.lucene.queries.TermFilter;
import org.apache.lucene.queries.FilterClause;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RegexpQuery;
import org.apache.lucene.search.NumericRangeQuery;

// Temporary
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanQuery;

import de.ids_mannheim.korap.util.KrillDate;
import de.ids_mannheim.korap.KrillCollection;
import de.ids_mannheim.korap.util.QueryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/*
 * THIS IS LIMITED TO PUBDATE AT THE MOMENT AND COMPLETELY LEGACY!
 */

/**
 * @author Nils Diewald
 * 
 *         BooleanFilterOperation implements a simple API for boolean
 *         operations
 *         on constraints for KorapFilter.
 */
public class BooleanFilterOperation {
    private String type;

    // Logger
    private final static Logger log = LoggerFactory
            .getLogger(KrillCollection.class);

    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;

    private BooleanFilter bool;
    private String error;


    public BooleanFilterOperation () {
        bool = new BooleanFilter();
    };


    public BooleanFilterOperation or (String type, String ... terms) {
        for (String term : terms) {

            if (DEBUG)
                log.trace("Filter: OR {}={}", type, term);

            bool.add(new TermFilter(new Term(type, term)),
                    BooleanClause.Occur.SHOULD);
        };
        return this;
    };


    public BooleanFilterOperation or (String type, RegexFilter value) {
        bool.add(value.toFilter(type), BooleanClause.Occur.SHOULD);
        return this;
    };


    public BooleanFilterOperation or (BooleanFilterOperation bf) {
        if (bf.bool.clauses().size() == 1) {
            FilterClause bc = bf.bool.clauses().get(0);
            bc.setOccur(BooleanClause.Occur.SHOULD);
            bool.add(bc);
            return this;
        }
        bool.add(bf.toFilter(), BooleanClause.Occur.SHOULD);
        return this;
    };


    public BooleanFilterOperation or (NumericRangeQuery<Integer> nrq) {
        bool.add(nrq, BooleanClause.Occur.SHOULD);
        return this;
    };


    public BooleanFilterOperation and (String type, String ... terms) {
        for (String term : terms) {
            bool.add(new TermFilter(new Term(type, term)),
                    BooleanClause.Occur.MUST);
        };
        return this;
    };


    public BooleanFilterOperation and (String type, RegexFilter value) {
        bool.add(value.toFilter(type), BooleanClause.Occur.MUST);
        return this;
    };


    public BooleanFilterOperation and (BooleanFilterOperation bf) {
        if (bf.bool.clauses().size() == 1) {
            FilterClause bc = bf.bool.clauses().get(0);
            bc.setOccur(BooleanClause.Occur.MUST);
            bool.add(bc);
            return this;
        }
        bool.add(bf.toFilter(), BooleanClause.Occur.MUST);
        return this;
    };


    public BooleanFilterOperation andNot (String type, String ... terms) {
        for (String term : terms) {
            bool.add(new TermFilter(new Term(type, term)),
                    BooleanClause.Occur.MUST_NOT);
        };
        return this;
    };


    public BooleanFilterOperation andNot (String type, RegexFilter value) {
        bool.add(value.toFilter(type), BooleanClause.Occur.MUST_NOT);
        return this;
    };


    public BooleanFilterOperation andNot (BooleanFilterOperation bf) {
        if (bf.bool.clauses().size() == 1) {
            FilterClause bc = bf.bool.clauses().get(0);
            bc.setOccur(BooleanClause.Occur.MUST_NOT);
            bool.add(bc);
            return this;
        }
        bool.add(bf.toFilter(), BooleanClause.Occur.MUST_NOT);
        return this;
    };


    public BooleanFilterOperation since (String dateStr) {
        int since = new KrillDate(dateStr).floor();

        if (since == 0 || since == KrillDate.BEGINNING)
            return this;

        bool.add(NumericRangeQuery.newIntRange("pubDate", since, KrillDate.END,
                true, true), BooleanClause.Occur.MUST);

        return this;
    };


    public BooleanFilterOperation till (String dateStr) {
        try {
            int till = new KrillDate(dateStr).ceil();
            if (till == 0 || till == KrillDate.END)
                return this;

            bool.add(NumericRangeQuery.newIntRange("pubDate",
                    KrillDate.BEGINNING, till, true, true),
                    BooleanClause.Occur.MUST);
        }
        catch (NumberFormatException e) {
            log.warn("Parameter of till(date) is invalid");
        };
        return this;
    };


    public BooleanFilterOperation between (String beginStr, String endStr) {
        KrillDate beginDF = new KrillDate(beginStr);

        int begin = beginDF.floor();

        int end = new KrillDate(endStr).ceil();

        if (end == 0)
            return this;

        if (begin == KrillDate.BEGINNING && end == KrillDate.END)
            return this;

        if (begin == end) {
            this.and("pubDate", beginDF.toString());
            return this;
        };

        this.bool.add(NumericRangeQuery.newIntRange("pubDate", begin, end,
                true, true), BooleanClause.Occur.MUST);
        return this;
    };


    public BooleanFilterOperation date (String dateStr) {
        KrillDate dateDF = new KrillDate(dateStr);

        if (dateDF.year == 0)
            return this;

        if (dateDF.day == 0 || dateDF.month == 0) {
            int begin = dateDF.floor();
            int end = dateDF.ceil();

            if (end == 0
                    || (begin == KrillDate.BEGINNING && end == KrillDate.END))
                return this;

            this.bool.add(NumericRangeQuery.newIntRange("pubDate", begin, end,
                    true, true), BooleanClause.Occur.MUST);
            return this;
        };

        this.and("pubDate", dateDF.toString());
        return this;
    };

    @Deprecated
    public Query toQuery () {
        return this.bool;
    };

    public Query toFilter () {
        return this.bool;
    };

    public String toString () {
        return this.bool.toString();
    };
};
