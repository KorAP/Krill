package de.ids_mannheim.korap.collection;

import java.util.*;

import org.apache.lucene.search.RegexpQuery;
import org.apache.lucene.index.Term;

/**
 * @author Nils Diewald
 *
 * RegexFilter implements a helper object for
 * regular expressions used in KorapFilter
 * constraints.
 */

public class RegexFilter {
    String regex;

    public RegexFilter (String regex) {
        this.regex = regex;
    };

    public RegexpQuery toQuery (String field) {
        return new RegexpQuery(
            new Term(field, this.regex)
        );
    };
};
