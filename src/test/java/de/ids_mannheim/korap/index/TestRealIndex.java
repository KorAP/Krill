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
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.query.QueryBuilder;
import de.ids_mannheim.korap.util.QueryException;
import java.nio.file.Paths;

public class TestRealIndex {
    KrillIndex ki;
    Result kr;
    Krill ks;


    public TestRealIndex () throws IOException {
        InputStream is = getClass().getResourceAsStream("/server.properties");
        Properties prop = new Properties();
        prop.load(is);

        String indexPath = prop.getProperty("lucene.indexDir");
        System.err.println(indexPath);
        MMapDirectory md = new MMapDirectory(Paths.get(indexPath));
        ki = new KrillIndex(md);
    };


    @Test
    public void testCase1 () throws IOException, QueryException {
        QueryBuilder kq = new QueryBuilder("tokens");
        ks = new Krill(kq.within(kq.tag("base/s:s"),
                kq.seq(kq.re("s:.*")).append(kq._(kq.re("s:.*")))).toQuery());
        ks.getMeta().setTimeOut(10000);
        kr = ks.apply(ki);
        System.err.println(kr.toJsonString());
        assertEquals(8, kr.getTotalResults());
    };
}
