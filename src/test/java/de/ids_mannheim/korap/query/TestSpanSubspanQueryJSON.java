package de.ids_mannheim.korap.query;

import static de.ids_mannheim.korap.TestSimple.getJSONQuery;
import static org.junit.Assert.assertEquals;

import org.apache.lucene.search.spans.SpanQuery;
import org.junit.Test;

import de.ids_mannheim.korap.query.wrap.SpanQueryWrapper;
import de.ids_mannheim.korap.util.QueryException;

/**
 * @author margaretha
 * 
 */
public class TestSpanSubspanQueryJSON {

    @Test
    public void testCase1() throws QueryException {
        String filepath = getClass().getResource("/queries/submatch.jsonld")
                .getFile();
        SpanQueryWrapper sqwi = getJSONQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(sq.toString(), "subspan(<tokens:s />,1,4)");
    }
}
