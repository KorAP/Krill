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
    private short flag;

    public SpanWithinQueryWrapper (SpanQueryWrapperInterface element, SpanQueryWrapperInterface wrap) {
	this.element = element;
	this.wrap = wrap;
	this.flag = (short) 0;
    };

    public SpanWithinQueryWrapper (SpanQueryWrapperInterface element, SpanQueryWrapperInterface wrap, short flag) {
	this.element = element;
	this.wrap = wrap;
	this.flag = flag;
    };

    public SpanQuery toQuery () {
	return new SpanWithinQuery(this.element.toQuery(), this.wrap.toQuery(), this.flag);
    };
};
