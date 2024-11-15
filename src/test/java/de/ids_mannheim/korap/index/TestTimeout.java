package de.ids_mannheim.korap.index;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.KrillMeta;
import de.ids_mannheim.korap.TestSimple;
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.util.StatusCodes;

public class TestTimeout extends TestSimple {

    @Test
    public void testMultipleWarningBug () throws IOException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(getClass().getResourceAsStream("/wiki/00001.json.gz"), true);
        ki.commit();

        SpanQuery q = new SpanTermQuery(new Term("tokens", "s:der"));
        Krill ks = new Krill(q);
        KrillMeta meta = ks.getMeta();
        meta.setTimeOut(-1);
        Result kr = ks.apply(ki);
        assertEquals(1, kr.getWarnings().size());
        assertEquals(StatusCodes.RESPONSE_TIME_EXCEEDED,
                kr.getWarning(0).getCode());
    }

    @Test
    public void testMultipleWarningMultilpleFragmentBug () throws IOException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(getClass().getResourceAsStream("/wiki/00001.json.gz"), true);
        ki.commit();
        ki.addDoc(getClass().getResourceAsStream("/wiki/00002.json.gz"), true);
        ki.commit();

        SpanQuery q = new SpanTermQuery(new Term("tokens", "s:der"));
        Krill ks = new Krill(q);
        KrillMeta meta = ks.getMeta();
        meta.setTimeOut(-1);
        Result kr = ks.apply(ki);
        assertEquals(1, kr.getWarnings().size());
        assertEquals(StatusCodes.RESPONSE_TIME_EXCEEDED,
                kr.getWarning(0).getCode());
    }

    @Test
    public void testMaxTimeout () throws IOException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(getClass().getResourceAsStream("/wiki/00001.json.gz"), true);
        ki.commit();

        String json = getJsonString(getClass()
                .getResource("/queries/metas/timeout.jsonld").getFile());
        Krill ks = new Krill(json);
        Result kr = ks.apply(ki);
        KrillMeta meta = kr.getMeta();
        
        //EM: 120000 seems to be the default
        assertEquals("120000", meta.getTimeOut());
        
        JsonNode jsonNode = kr.toJsonNode();
        assertEquals("120000", jsonNode.at("/meta/timeout").asInt());        
    }
}
