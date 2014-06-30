package de.ids_mannheim.korap.query.wrap;

import org.apache.lucene.search.spans.SpanQuery;

// Todo: Make this an abstract class to deal with regexes in a parent abstract class!

// Add optional and null attributes

public interface SpanQueryWrapperInterface {
    public SpanQuery toQuery ();
};
