package de.ids_mannheim.korap.collection;

import java.util.*;

import org.apache.lucene.search.RegexpQuery;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.Filter;

/**
 * @author Nils Diewald
 * 
 *         RegexFilter implements a helper object for
 *         regular expressions used in KrillCollection
 *         constraints.
 */

public class RegexFilter {
    String regex;


    public RegexFilter (String regex) {
        this.regex = regex;
    };

    @Deprecated
    public RegexpQuery toQuery (String field) {
        return new RegexpQuery(new Term(field, this.regex));
    };

    public Filter toFilter (String field) {
        return new QueryWrapperFilter(
            new RegexpQuery(new Term(field, this.regex))
        );
    };

};
