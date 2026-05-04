package de.ids_mannheim.korap.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.util.KrillProperties;
import de.ids_mannheim.korap.util.StatusCodes;

/** AI generated
 * 
 */
public class TestMemoryLimit {

    private long savedMemoryLimit;

    @Before
    public void saveMemoryLimit () {
        savedMemoryLimit = KrillProperties.maxMemoryMB;
    }

    @After
    public void restoreMemoryLimit () {
        KrillProperties.maxMemoryMB = savedMemoryLimit;
    }

    @Test
    public void testMemoryLimitAbortsSingleSegment () throws IOException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(getClass().getResourceAsStream("/wiki/00001.json.gz"), true);
        ki.commit();

        // Set the limit to 1 MB so it is exceeded immediately
        KrillProperties.maxMemoryMB = 1;

        SpanQuery q = new SpanTermQuery(new Term("tokens", "s:der"));
        Result kr = new Krill(q).apply(ki);
        assertTrue("Expected memory exceeded warning", kr.hasWarnings());
        assertEquals(1, kr.getWarnings().size());
        assertEquals(StatusCodes.MEMORY_LIMIT_EXCEEDED,
                kr.getWarning(0).getCode());
    }

    @Test
    public void testMemoryLimitAbortsMultipleSegments () throws IOException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(getClass().getResourceAsStream("/wiki/00001.json.gz"), true);
        ki.commit();
        ki.addDoc(getClass().getResourceAsStream("/wiki/00002.json.gz"), true);
        ki.commit();

        // Set the limit to 1 MB so it is exceeded immediately
        KrillProperties.maxMemoryMB = 1;

        SpanQuery q = new SpanTermQuery(new Term("tokens", "s:der"));
        Result kr = new Krill(q).apply(ki);

        assertTrue("Expected memory exceeded warning", kr.hasWarnings());
        assertEquals(1, kr.getWarnings().size());
        assertEquals(StatusCodes.MEMORY_LIMIT_EXCEEDED,
                kr.getWarning(0).getCode());
    }

    @Test
    public void testNoMemoryLimitWhenDisabled () throws IOException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(getClass().getResourceAsStream("/wiki/00001.json.gz"), true);
        ki.commit();

        // 0 = disabled
        KrillProperties.maxMemoryMB = 0;

        SpanQuery q = new SpanTermQuery(new Term("tokens", "s:der"));
        Result kr = new Krill(q).apply(ki);
       
        assertTrue("Expected results when memory limit is disabled",
                kr.getTotalResults() > 0);
        assertTrue("Expected no warnings when memory limit is disabled",
                !kr.hasWarnings());
    }
}
