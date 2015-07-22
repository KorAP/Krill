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

    // Logger
    private final static Logger log = LoggerFactory.getLogger(KrillCollection.class);

    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;

    public CollectionBuilderInterface term (String field, String term) {
        return new CollectionBuilderTerm(field, term);
    };

    public CollectionBuilderInterface re (String field, String term) {
        return new CollectionBuilderTerm(field, term, true);
    };

    public CollectionBuilderInterface since (String field, String date) {
        int since = new KrillDate(date).floor();

        if (since == 0 || since == KrillDate.BEGINNING)
            return null;

        return new CollectionBuilderRange(field, since, KrillDate.END);
    };

    public CollectionBuilderInterface till (String field, String date) {
        try {
            int till = new KrillDate(date).ceil();
            if (till == 0 || till == KrillDate.END)
                return null;

            return new CollectionBuilderRange(field, KrillDate.BEGINNING, till);
        }
        catch (NumberFormatException e) {
            log.warn("Parameter of till(date) is invalid");
        };
        return null;
    };

    public CollectionBuilderInterface date (String field, String date) {
        KrillDate dateDF = new KrillDate(date);

        if (dateDF.year == 0)
            return null;

        if (dateDF.day == 0 || dateDF.month == 0) {
            int begin = dateDF.floor();
            int end = dateDF.ceil();

            if (end == 0
                || (begin == KrillDate.BEGINNING && end == KrillDate.END))
                return null;

            return new CollectionBuilderRange(field, begin, end);
        };
        return new CollectionBuilderTerm(field, dateDF.toString());
    };


    public CollectionBuilderGroup andGroup (CollectionBuilderInterface cb) {
        return new CollectionBuilderGroup(false).with(cb);
    };

    public CollectionBuilderGroup orGroup (CollectionBuilderInterface cb) {
        return new CollectionBuilderGroup(true).with(cb);
    };

    public interface CollectionBuilderInterface {
        public String toString ();
        public Filter toFilter ();
        public boolean isNegative ();
        public CollectionBuilderInterface not ();
    };

    public class CollectionBuilderTerm implements CollectionBuilderInterface {
        private boolean isNegative = false;
        private boolean regex = false;
        private String field;
        private String term;

        public CollectionBuilderTerm (String field, String term) {
            this.field = field;
            this.term = term;
        };

        public CollectionBuilderTerm (String field, String term, boolean regex) {
            this.field = field;
            this.term = term;
            this.regex = regex;
        };

        public Filter toFilter () {
            // Regular expression
            if (this.regex)
                return new QueryWrapperFilter(
                    new RegexpQuery(new Term(this.field, this.term))
                );
            
            // Simple term
            return new TermsFilter(new Term(this.field, this.term));
        };

        public String toString () {
            return this.toFilter().toString();
        };

        public boolean isNegative () {
            return this.isNegative;
        };


        public CollectionBuilderInterface not () {
            this.isNegative = true;
            return this;
        };
    };

    public class CollectionBuilderGroup implements CollectionBuilderInterface {
        private boolean isOptional = false;
        private boolean isNegative = true;

        public boolean isNegative () {
            return this.isNegative;
        };

        public boolean isOptional () {
            return this.isOptional;
        };


        private ArrayList<CollectionBuilderInterface> operands;

        public CollectionBuilderGroup (boolean optional) {
            this.isOptional = optional;
            this.operands = new ArrayList<CollectionBuilderInterface>(3);
        };

        public CollectionBuilderGroup with (CollectionBuilderInterface cb) {
            if (!cb.isNegative())
                this.isNegative = false;
            this.operands.add(cb);
            return this;
        };

        public Filter toFilter () {

            if (this.operands == null || this.operands.isEmpty())
                return null;

            if (this.operands.size() == 1)
                return this.operands.get(0).toFilter();

            BooleanFilter bool = new BooleanFilter();

            Iterator<CollectionBuilderInterface> i = this.operands.iterator();
            while (i.hasNext()) {
                CollectionBuilderInterface cb = i.next();
                if (cb.isNegative()) {
                    bool.add(cb.toFilter(), BooleanClause.Occur.MUST_NOT);
                }
                else if (this.isOptional()) {
                    bool.add(cb.toFilter(), BooleanClause.Occur.SHOULD);
                }
                else {
                    bool.add(cb.toFilter(), BooleanClause.Occur.MUST);
                }
            };

            return bool;
        };

        public String toString () {
            return this.toFilter().toString();
        };

        public CollectionBuilderInterface not () {
            this.isNegative = true;
            return this;
        };
    };

    public class CollectionBuilderRange implements CollectionBuilderInterface {
        private boolean isNegative = false;
        private String field;
        private int start, end;

        public CollectionBuilderRange (String field, int start, int end) {
            this.field = field;
            this.start = start;
            this.end = end;
        };

        public boolean isNegative () {
            return this.isNegative;
        };

        public String toString () {
            return this.toFilter().toString();
        };

        public Filter toFilter () {
            return NumericRangeFilter.newIntRange(this.field,
                                                  this.start,
                                                  this.end,
                                                  true,
                                                  true);
        };

        public CollectionBuilderInterface not () {
            this.isNegative = true;
            return this;
        };
    };
};
