package de.ids_mannheim.korap.query;

import static de.ids_mannheim.korap.TestSimple.*;
import static org.junit.Assert.assertEquals;

import org.apache.lucene.search.spans.SpanQuery;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import de.ids_mannheim.korap.query.wrap.SpanQueryWrapper;
import de.ids_mannheim.korap.util.QueryException;

public class TestSpanWithinQueryJSON {

    @Rule
    public ExpectedException exception = ExpectedException.none();


    @Test
    public void testTokenContainSentence () throws QueryException {
        exception
                .expectMessage("Token cannot contain another token or element.");

        String filepath = getClass().getResource(
                "/queries/position/token-contain-sentence.json").getFile();
        SpanQueryWrapper sqwi = getJsonQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
    }
}
