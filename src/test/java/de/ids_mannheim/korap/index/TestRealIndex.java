package de.ids_mannheim.korap.index;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.store.MMapDirectory;
import org.junit.Test;

import de.ids_mannheim.korap.*;
import de.ids_mannheim.korap.util.QueryException;

public class TestRealIndex {
    KorapIndex ki;
    KorapResult kr;
    Krill ks;
    KrillQuery kq;
	
    public TestRealIndex() throws IOException {
        InputStream is = getClass().getResourceAsStream("/server.properties");
        Properties prop = new Properties();
        prop.load(is);
	
        String indexPath = prop.getProperty("lucene.indexDir");
        System.err.println(indexPath);
        MMapDirectory md = new MMapDirectory(new File(indexPath));
        ki = new KorapIndex(md);
    };

    @Test
    public void testCase1() throws IOException, QueryException {
        KrillQuery kq = new KrillQuery("tokens");
        ks = new Krill(kq.within(kq.tag("base/s:s"), kq.seq(kq.re("s:.*")).append(kq._(kq.re("s:.*")))).toQuery());
        ks.getMeta().setTimeOut(10000);
        kr = ks.apply(ki);
        System.err.println(kr.toJsonString());
        assertEquals(8, kr.getTotalResults());
    };
}
