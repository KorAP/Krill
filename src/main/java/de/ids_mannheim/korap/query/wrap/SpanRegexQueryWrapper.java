package de.ids_mannheim.korap.query.wrap;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.RegexpQuery;
import org.apache.lucene.search.spans.SpanMultiTermQueryWrapper;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.util.automaton.RegExp;

/*
  TODO: Don't allow queries like ".*?"!!!
*/

public class SpanRegexQueryWrapper extends SpanQueryWrapper {
    private SpanQuery query;
    public String error = null;

    private static int regexFlag = RegExp.NONE;

    public SpanRegexQueryWrapper (String field, String re) {
        this(field, re, regexFlag, false);
    };


    public SpanRegexQueryWrapper (String field, String re, int flags) {
        this(field, re, flags, false);
    };


    public SpanRegexQueryWrapper (String field, String re,
                                  boolean caseinsensitive) {
        this(field, re, regexFlag, caseinsensitive);
    };


    public SpanRegexQueryWrapper (String field, String re, int flags,
                                  boolean caseinsensitive) {
        if (caseinsensitive) {
            if (re.startsWith("s:")) {
                re = re.replaceFirst("s:", "i:");
            };
            re = re.toLowerCase();
        };

        try {
            RegexpQuery requery = new RegexpQuery(new Term(field, re), flags);
            query = new SpanMultiTermQueryWrapper<RegexpQuery>(requery);
        } catch (Exception e) {
            this.error = e.getLocalizedMessage();
        }
    };

    public SpanRegexQueryWrapper (RegexpQuery requery) {
        query = new SpanMultiTermQueryWrapper<RegexpQuery>(requery);
    };    

    public SpanQuery toFragmentQuery () {
        return this.query;
    };


    public boolean isNull () {
        return false;
    };
};
