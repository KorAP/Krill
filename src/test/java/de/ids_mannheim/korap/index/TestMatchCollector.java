package de.ids_mannheim.korap.index;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.response.MatchCollector;

// mvn -Dtest=TestWithinIndex#indexExample1 test

// match is shrink and split

@RunWith(JUnit4.class)
public class TestMatchCollector {

    @Test
    public void indexExample1 () throws IOException {
        KrillIndex ki = new KrillIndex();

        // abcabcabac
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-1");
        fd.addString("UID", "1");
        fd.addTV("base", "abcabcabac", "[(0-1)s:a|i:a|_0$<i>0<i>1|-:t$<i>10]"
                + "[(1-2)s:b|i:b|_1$<i>1<i>2]" + "[(2-3)s:c|i:c|_2$<i>2<i>3]"
                + "[(3-4)s:a|i:a|_3$<i>3<i>4]" + "[(4-5)s:b|i:b|_4$<i>4<i>5]"
                + "[(5-6)s:c|i:c|_5$<i>5<i>6]" + "[(6-7)s:a|i:a|_6$<i>6<i>7]"
                + "[(7-8)s:b|i:b|_7$<i>7<i>8]" + "[(8-9)s:a|i:a|_8$<i>8<i>9]"
                + "[(9-10)s:c|i:c|_9$<i>9<i>10]");
        ki.addDoc(fd);

        fd = new FieldDocument();
        fd.addString("ID", "doc-2");
        fd.addString("UID", "2");
        fd.addTV("base", "bcbabd",
                "[(0-1)s:b|i:b|_1$<i>0<i>1]" + "[(1-2)s:c|i:c|s:b|_2$<i>1<i>2]"
                        + "[(2-3)s:b|i:b|_3$<i>2<i>3|<>:e#2-4$<i>4]"
                        + "[(3-4)s:a|i:a|_4$<i>3<i>4|<>:e#3-5$<i>5|<>:e2#3-5$<i>5]"
                        + "[(4-5)s:b|i:b|s:c|_5$<i>4<i>5]"
                        + "[(5-6)s:d|i:d|_6$<i>5<i>6|<>:e2#5-6$<i>6]");
        ki.addDoc(fd);

        ki.commit();

        SpanQuery sq;

        sq = new SpanTermQuery(new Term("base", "s:b"));
        Krill krill = new Krill(sq);
        krill.getMeta().setCount((short) 10);
        MatchCollector mc = ki.collect(krill, new MatchCollector());

        assertEquals(5, mc.getTotalResults());
        assertEquals(2, mc.getTotalResultDocs());
    };
};
