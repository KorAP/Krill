package de.ids_mannheim.korap.query.wrap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;

import de.ids_mannheim.korap.util.QueryException;

public class SpanAlterQueryWrapper extends SpanQueryWrapper {
    private String field;
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

    public SpanAlterQueryWrapper setNegative (Boolean neg) {
        this.isNegative = neg;
        return this;
    };

    public SpanAlterQueryWrapper or (String term) {
        // TODO:
        //   Potential optimizable by directly add()ing
        SpanQueryWrapper sqw = new SpanSimpleQueryWrapper(this.field, term);
        return this.or(sqw);
    };


    public SpanAlterQueryWrapper or (SpanQueryWrapper term) {
        if (term.isNull())
            return this;

        // Check! This seems to render the whole group negative!
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
