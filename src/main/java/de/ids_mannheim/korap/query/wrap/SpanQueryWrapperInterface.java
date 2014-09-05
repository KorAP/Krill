package de.ids_mannheim.korap.query.wrap;

import org.apache.lucene.search.spans.SpanQuery;

// Todo: Make this an abstract class to deal with regexes
// in a parent abstract class!
// Add warning and error

public interface SpanQueryWrapperInterface {
    public SpanQuery toQuery    ();
    public boolean   isOptional ();
    public boolean   isNull     ();
    public boolean   isNegative ();
};
