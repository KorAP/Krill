package de.ids_mannheim.korap.query.wrap;

import de.ids_mannheim.korap.query.SpanWithinQuery;
import de.ids_mannheim.korap.query.wrap.SpanSegmentQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanRegexQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanSequenceQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanQueryWrapperInterface;

import java.util.*;

import org.apache.lucene.search.spans.SpanQuery;



public class SpanWithinQueryWrapper implements SpanQueryWrapperInterface {
    private SpanQueryWrapperInterface element;
    private SpanQueryWrapperInterface wrap;
    private byte flag;
    private boolean isNull = true;;

    public SpanWithinQueryWrapper (SpanQueryWrapperInterface element, SpanQueryWrapperInterface wrap) {
	this.element = element;
	this.wrap = wrap;
	this.flag = (byte) SpanWithinQuery.WITHIN;
	if (!element.isNull() && !wrap.isNull())
	    this.isNull = false;
    };

    public SpanWithinQueryWrapper (SpanQueryWrapperInterface element, SpanQueryWrapperInterface wrap, byte flag) {
	this.element = element;
	this.wrap = wrap;
	this.flag = flag;
	if (!element.isNull() && !wrap.isNull())
	    this.isNull = false;
    };

    public SpanQuery toQuery () {
	if (this.isNull)
	    return (SpanQuery) null;
	
	return new SpanWithinQuery(this.element.toQuery(), this.wrap.toQuery(), this.flag);
    };

    public boolean isOptional () {
	return false;
    };

    public boolean isNull () {
	return this.isNull;
    };
};
