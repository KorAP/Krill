package de.ids_mannheim.korap.collection;

import java.util.*;
import java.io.IOException;
import org.apache.lucene.queries.BooleanFilter;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.TermsFilter;
import org.apache.lucene.search.*;
import org.apache.lucene.search.NumericRangeFilter;
import de.ids_mannheim.korap.util.KrillDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ids_mannheim.korap.KrillCollection;



public class CollectionBuilderNew {
    // private BooleanFilter filter;
    public boolean isNegative = true;
    public boolean isOptional = false;
    private ArrayList<CollectionBuilderNew> operands;
    private Filter filter;

    // Logger
    private final static Logger log = LoggerFactory
            .getLogger(KrillCollection.class);

    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;

    public boolean isNegative () {
        return this.isNegative;
    };

    public boolean isOptional () {
        return this.isOptional;
    };

    public CollectionBuilderNew () {
        // filter = new BooleanFilter();
    };

    // Filter filter
    public CollectionBuilderNew or (CollectionBuilderNew cb) {
        this.isNegative = false;
        cb.isNegative = false;
        cb.isOptional = true;
        if (this.operands == null)
            this.operands = new ArrayList<CollectionBuilderNew>(3);
        this.operands.add(cb);
        return this;
    };

    public CollectionBuilderNew and (CollectionBuilderNew cb) {
        this.isNegative = false;
        cb.isNegative = false;
        cb.isOptional = false;
        if (this.operands == null)
            this.operands = new ArrayList<CollectionBuilderNew>(3);
        this.operands.add(cb);
        return this;
    };

    public CollectionBuilderNew not (CollectionBuilderNew cb) {
        cb.isOptional = false;
        cb.isNegative = true;
        if (this.operands == null)
            this.operands = new ArrayList<CollectionBuilderNew>(3);
        this.operands.add(cb);
        return this;
    };

    /*
    public CollectionBuilderNew since (String field, String date) {
        int since = new KrillDate(date).floor();

        if (since == 0 || since == KrillDate.BEGINNING)
            return null;

        this.filter.add(
            NumericRangeFilter.newIntRange(field, since, KrillDate.END, true, true),
            BooleanClause.Occur.MUST
        );

        return this;
    };

    public CollectionBuilderNew till (String field, String date) {
        try {
            int till = new KrillDate(date).ceil();
            if (till == 0 || till == KrillDate.END)
                return this;

            this.filter.add(
                NumericRangeFilter.newIntRange(field,
                KrillDate.BEGINNING, till, true, true),
                BooleanClause.Occur.MUST);
        }
        catch (NumberFormatException e) {
            log.warn("Parameter of till(date) is invalid");
        };
        return this;
    };


    public CollectionBuilderNew date (String field, String date) {
        KrillDate dateDF = new KrillDate(date);

        if (dateDF.year == 0)
            return this;

        if (dateDF.day == 0 || dateDF.month == 0) {
            int begin = dateDF.floor();
            int end = dateDF.ceil();

            if (end == 0
                    || (begin == KrillDate.BEGINNING && end == KrillDate.END))
                return this;

            this.filter.add(NumericRangeFilter.newIntRange(field, begin, end,
                    true, true), BooleanClause.Occur.MUST);
            return this;
        };

        this.and(this.term(field, dateDF.toString()));
        return this;
    };
    */

    public CollectionBuilderNew re (String field, String regex) {
        CollectionBuilderNew cb = new CollectionBuilderNew();
        cb.filter = new QueryWrapperFilter(new RegexpQuery(new Term(field, regex)));
        return cb;
    };

    public CollectionBuilderNew term (String field, String term) {
        CollectionBuilderNew cb = new CollectionBuilderNew();
        cb.filter = new TermsFilter(new Term(field, term));
        return cb;
    };

    public Filter toFilter () {
        if (this.filter != null) {
            return this.filter;
        };

        if (this.operands == null || this.operands.isEmpty())
            return null;

        BooleanFilter bool = new BooleanFilter();

        if (this.operands.size() == 1)
            return this.operands.get(0).toFilter();

        Iterator<CollectionBuilderNew> i = this.operands.iterator();
        while (i.hasNext()) {
            CollectionBuilderNew cb = i.next();
            if (cb.isNegative()) {
                bool.add(cb.toFilter(), BooleanClause.Occur.MUST_NOT);
            }
            else if (cb.isOptional()) {
                bool.add(cb.toFilter(), BooleanClause.Occur.SHOULD);
            }
            else {
                bool.add(cb.toFilter(), BooleanClause.Occur.MUST);
            }
        };

        return bool;
    };

    public String toString () {
        Filter filter = this.toFilter();
        if (filter == null)
            return "";
        return filter.toString();
    };
};
