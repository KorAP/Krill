package de.ids_mannheim.korap.query.wrap;

import de.ids_mannheim.korap.query.wrap.SpanRegexQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanWildcardQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanSegmentQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanSimpleQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanQueryWrapper;

import de.ids_mannheim.korap.util.QueryException;

import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.index.Term;

import java.util.*;

public class SpanAlterQueryWrapper extends SpanQueryWrapper {
    private String field;
    private SpanQuery query;
    private List<SpanQueryWrapper> alternatives;


    public SpanAlterQueryWrapper (String field) {
        this.field = field;
        this.alternatives = new ArrayList<>();
    };


    public SpanAlterQueryWrapper (String field, SpanQueryWrapper query) {
        this.field = field;
        this.alternatives = new ArrayList<>();
        this.maybeUnsorted = query.maybeUnsorted();
        this.alternatives.add(query);
    };


    public SpanAlterQueryWrapper (String field, String ... terms) {
        this.field = field;
        this.alternatives = new ArrayList<>();
        for (String term : terms) {
            this.isNull = false;
            this.alternatives.add(
                new SpanSimpleQueryWrapper(this.field, term)
                );
        };
    };


    public SpanAlterQueryWrapper or (String term) {
        SpanQueryWrapper sqw = new SpanSimpleQueryWrapper(this.field, term);
        return this.or(sqw);
    };


    public SpanAlterQueryWrapper or (SpanQueryWrapper term) {
        if (term.isNull())
            return this;

        if (term.isNegative())
            this.isNegative = true;

        // If one operand is optional, the whole group can be optional
        // a | b* | c
        if (term.isOptional())
            this.isOptional = true;

        this.alternatives.add(term);

        if (term.maybeUnsorted())
            this.maybeUnsorted = true;

        this.isNull = false;
        return this;
    };


    public SpanAlterQueryWrapper or (SpanRegexQueryWrapper term) {
        this.alternatives.add(term);
        this.isNull = false;
        return this;
    };


    public SpanAlterQueryWrapper or (SpanWildcardQueryWrapper wc) {
        this.alternatives.add(wc);
        this.isNull = false;
        return this;
    };


    /*
     * The query is extended to right in case one alternative is extended to the right.
     */
    @Override
    public boolean isExtendedToTheRight () {
        if (this.alternatives.size() == 0)
            return this.alternatives.get(0).isExtendedToTheRight();
        Iterator<SpanQueryWrapper> clause = this.alternatives.iterator();
        while (clause.hasNext()) {
            if (clause.next().isExtendedToTheRight()) {
                return true;
            };
        };
        return false;
    };


    @Override
    public SpanQuery toFragmentQuery () throws QueryException {
        if (this.isNull || this.alternatives.size() == 0)
            return (SpanQuery) null;

        if (this.alternatives.size() == 1) {
            return (SpanQuery) this.alternatives.get(0)
                    .retrieveNode(this.retrieveNode).toFragmentQuery();
        };

        Iterator<SpanQueryWrapper> clause = this.alternatives.iterator();
        SpanOrQuery soquery = new SpanOrQuery(clause.next()
                .retrieveNode(this.retrieveNode).toFragmentQuery());
        while (clause.hasNext()) {
            soquery.addClause(clause.next().retrieveNode(this.retrieveNode)
                    .toFragmentQuery());
        };
        return (SpanQuery) soquery;
    };
};
