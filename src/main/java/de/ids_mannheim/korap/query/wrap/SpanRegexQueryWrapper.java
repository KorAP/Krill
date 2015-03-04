package de.ids_mannheim.korap.query.wrap;

import org.apache.lucene.search.RegexpQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanMultiTermQueryWrapper;
import org.apache.lucene.util.automaton.RegExp;
import org.apache.lucene.index.Term;
import de.ids_mannheim.korap.query.wrap.SpanQueryWrapper;

import java.util.*;

/*
  TODO: Don't allow queries like ".*?"!!!
*/

public class SpanRegexQueryWrapper extends SpanQueryWrapper {
    private SpanQuery query;


    public SpanRegexQueryWrapper (String field, String re) {
        this(field, re, RegExp.ALL, false);
    };


    public SpanRegexQueryWrapper (String field, String re, int flags) {
        this(field, re, flags, false);
    };


    public SpanRegexQueryWrapper (String field, String re,
                                  boolean caseinsensitive) {
        this(field, re, RegExp.ALL, caseinsensitive);
    };


    public SpanRegexQueryWrapper (String field, String re, int flags,
                                  boolean caseinsensitive) {
        if (caseinsensitive) {
            if (re.startsWith("s:")) {
                re = re.replaceFirst("s:", "i:");
            };
            re = re.toLowerCase();
        };
        RegexpQuery requery = new RegexpQuery(new Term(field, re), flags);
        query = new SpanMultiTermQueryWrapper<RegexpQuery>(requery);

    };


    public SpanQuery toQuery () {
        return this.query;
    };


    public boolean isNull () {
        return false;
    };
};
