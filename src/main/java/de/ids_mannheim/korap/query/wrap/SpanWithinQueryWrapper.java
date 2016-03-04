package de.ids_mannheim.korap.query.wrap;

import de.ids_mannheim.korap.query.SpanWithinQuery;
import de.ids_mannheim.korap.query.wrap.SpanSegmentQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanRegexQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanSequenceQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanQueryWrapper;

import de.ids_mannheim.korap.util.QueryException;

import java.util.*;

import org.apache.lucene.search.spans.SpanQuery;

/*
  Todo:

  contains(token,token) und matches(token, token) -> termGroup


  - Exclusivity has to be supported
  - In case the wrap is negative,
    the query has to be interpreted as being exclusive!
    - within(<s>,[base=term])   -> all <s> including [base=term]
    - within(<s>,[base!=term])  -> all <s> not including [base=term]
    - !within(<s>,[base=term]) -> all <s> not including [base=term]
    - within(!<s>,[base!=term]) -> failure - embedding span has to be positive
      -> Exception: It is an Overlap!
    -> BUT! This becomes weird with classes, as
       - within(<s>, {2:[base!=term]}) will match quite often!
    -> so this is no valid solution!

    Better - Exclusivity and Negation:
    - within(<s>,[base!=term])  -> all <s>, hitting only [base!=term] tokens
      -> is this technically doable? NO!
    - !within(<s>,[base=term])  -> all <s>, not containing [base=term]
    - within(!<s>,[base=term])  -> failure


  - Optionality:
    - At the moment:
      - Optionality of operands will be ignored
        while the optionality of the wrap is herited!
    - within(<s>?, [base=term])      -> opt
    - within(<s>, {2:[base=term]*})  -> (<s>|within(<s>, {2:[base=term]+}))
    - within(<s>?, {2:[base=term]*}) -> (<s>|within(<s>, {2:[base=term]+})) and opt

  - Speed improvement:
    - Check for classes!
    - within(<s>, [base=term]*) -> <s>
    - within(<s>, {2:[base=term]*})  -> (<s>|within(<s>, {2:[base=term]+}))

  - Special case overlaps(), overlapsStrictly():
    - overlaps(<s>, <p>) == overlaps(<p>, <s>)
    - overlaps(<s>?, <p>) -> optionality is always inherited!

*/


public class SpanWithinQueryWrapper extends SpanQueryWrapper {
    private SpanQueryWrapper element;
    private SpanQueryWrapper wrap;
    private byte flag;


    public SpanWithinQueryWrapper (SpanQueryWrapper element,
                                   SpanQueryWrapper wrap) {
        this.element = element;
        this.wrap = wrap;

        // TODO: if (wrap.isNegative())	    

        this.flag = (byte) SpanWithinQuery.WITHIN;
        if (!element.isNull() && !wrap.isNull())
            this.isNull = false;
    };


    public SpanWithinQueryWrapper (SpanQueryWrapper element,
                                   SpanQueryWrapper wrap, byte flag) {
        this.element = element;
        this.wrap = wrap;
        this.flag = flag;

        // TODO: if (wrap.isNegative())

        if (!element.isNull() && !wrap.isNull())
            this.isNull = false;
    };

    @Override
    public SpanQuery toFragmentQuery () throws QueryException {
        if (this.isNull)
            return (SpanQuery) null;

        // TODO: if (wrap.isNegative())

        return new SpanWithinQuery(this.element.retrieveNode(this.retrieveNode)
                .toFragmentQuery(),
                this.wrap.retrieveNode(this.retrieveNode).toFragmentQuery(), this.flag);
    };


    @Override
    public boolean maybeUnsorted () {
        if (this.wrap.maybeUnsorted())
            return true;

        // Todo: This is only true in case of non-exclusivity!
        if (this.element.maybeUnsorted())
            return true;

        return this.maybeUnsorted;
    };


    @Override
    public boolean isNegative () {
        if (this.element.isNegative())
            return true;
        return false;
    };
};
