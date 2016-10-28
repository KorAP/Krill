package de.ids_mannheim.korap.query;

import static de.ids_mannheim.korap.TestSimple.getJSONQuery;

import de.ids_mannheim.korap.query.wrap.SpanQueryWrapper;

public abstract class BaseTest {

    protected String path;
    
    // get query wrapper based on json file
    public SpanQueryWrapper jsonQueryFile (String filename) {
        return getJSONQuery(getClass().getResource(path + filename).getFile());
    }
}
