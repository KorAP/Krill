package de.ids_mannheim.korap.query.wrap;

import org.apache.lucene.search.spans.SpanQuery;

import de.ids_mannheim.korap.query.SpanElementQuery;
import de.ids_mannheim.korap.query.wrap.SpanQueryWrapperInterface;

public class SpanElementQueryWrapper implements SpanQueryWrapperInterface {
    String element;
    String field;

    public SpanElementQueryWrapper (String field, String element) {
	this.field = field;
	this.element = element;
    };

    public SpanQuery toQuery () {
	return (SpanQuery) new SpanElementQuery(this.field, this.element);
    };

    public boolean isOptional () {
	return false;
    };

    public boolean isNull () {
	return false;
    };
};
