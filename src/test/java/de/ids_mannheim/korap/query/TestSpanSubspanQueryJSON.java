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
        assertEquals(sq.toString(),
                "subspan(spanContain(<tokens:s />, tokens:tt/l:Haus),1,4)");
    }

    @Test
    public void testCase2() throws QueryException {
        String filepath = getClass().getResource("/queries/submatch2.jsonld")
                .getFile();
        SpanQueryWrapper sqwi = getJSONQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(sq.toString(), "subspan(<tokens:s />,1,4)");
    }

    public void testCase3() throws QueryException {
        String filepath = getClass().getResource("/queries/submatch3.jsonld")
                .getFile();
        SpanQueryWrapper sqwi = getJSONQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(sq.toString(), "subspan(<tokens:s />,1,0)");

    }

}
