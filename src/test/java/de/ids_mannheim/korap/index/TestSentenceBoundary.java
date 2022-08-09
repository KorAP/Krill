package de.ids_mannheim.korap.index;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.junit.Test;

import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.KrillCollection;
import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.query.DistanceConstraint;
import de.ids_mannheim.korap.query.SpanClassQuery;
import de.ids_mannheim.korap.query.SpanDistanceQuery;
import de.ids_mannheim.korap.query.SpanElementQuery;
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.response.SearchContext;

public class TestSentenceBoundary {

    private FieldDocument createFieldDoc () {
        // expected:1, actual:2,
        FieldDocument fd = new FieldDocument();
        fd.addString("docGroup", "a");
        fd.addTV("base", "bhhfcbibldea",
                "[(0-1)s:b|<>:base/s:t$<b>64<i>0<i>63<i>63<b>0"
                        + "|a:l|<>:base/s:s$<b>64<i>0<i>2<i>2<b>1|_0$<i>0<i>1]"
                        + "[(1-2)s:h|_1$<i>1<i>2]" + "[(2-3)s:h|_2$<i>2<i>3]"
                        + "[(3-4)s:f|<>:base/s:s$<b>64<i>3<i>8<i>8<b>1|_3$<i>3<i>4]"
                        + "[(4-5)s:c|a:k|_4$<i>4<i>5]"
                        + "[(5-6)s:b|a:i|_5$<i>5<i>6]"
                        + "[(6-7)s:i|a:g|_6$<i>6<i>7]"
                        + "[(7-8)s:b|a:i|_7$<i>7<i>8]"
                        + "[(8-9)s:l|a:c|_8$<i>8<i>9]"
                        + "[(9-10)s:d|a:f|a:a|<>:base/s:s$<b>64<i>9<i>9<i>9<b>1|_9$<i>9<i>10]"
                        + "[(10-11)s:e|a:d|<>:base/s:s$<b>64<i>10<i>11<i>11<b>1|_10$<i>10<i>11]"
                        + "[(11-12)s:a|_11$<i>11<i>12]");

        return fd;
    }


    private FieldDocument createFieldDoc2 () {
        // expected:1, actual:3, docs:
        //a:~mfiae~i~lceech~cbimm~m~i~dm~cgdbhjbhefb~jbhd~jgl~i~jm
        FieldDocument fd = new FieldDocument();
        fd.addString("docGroup", "a");
        fd.addTV("base", "mfiaeilceechcbimmmidmcgdbhjbhefb",
                "[(0-1)s:m|<>:base/s:t$<b>64<i>0<i>42<i>42<b>0|a:a|a:d|"
                        + "<>:base/s:s$<b>64<i>0<i>4<i>4<b>1|_0$<i>0<i>1]"
                        + "[(1-2)s:f|_1$<i>1<i>2]" + "[(2-3)s:i|_2$<i>2<i>3]"
                        + "[(3-4)s:a|a:j|_3$<i>3<i>4]"
                        + "[(4-5)s:e|a:g|_4$<i>4<i>5]"
                        + "[(5-6)s:i|<>:base/s:s$<b>64<i>5<i>5<i>5<b>1|_5$<i>5<i>6]"
                        + "[(6-7)s:l|a:m|a:h|<>:base/s:s$<b>64<i>6<i>11<i>11<b>1|_6$<i>6<i>7]"
                        + "[(7-8)s:c|a:e|_7$<i>7<i>8]"
                        + "[(8-9)s:e|_8$<i>8<i>9]"
                        + "[(9-10)s:e|a:f|_9$<i>9<i>10]"
                        + "[(10-11)s:c|a:m|_10$<i>10<i>11]"
                        + "[(11-12)s:h|a:k|_11$<i>11<i>12]"
                        + "[(12-13)s:c|a:d|<>:base/s:s$<b>64<i>12<i>16<i>16<b>1|_12$<i>12<i>13]"
                        + "[(13-14)s:b|_13$<i>13<i>14]"
                        + "[(14-15)s:i|a:h|_14$<i>14<i>15]"
                        + "[(15-16)s:m|a:e|_15$<i>15<i>16]"
                        + "[(16-17)s:m|a:f|_16$<i>16<i>17]"
                        + "[(17-18)s:m|a:l|<>:base/s:s$<b>64<i>17<i>17<i>17<b>1|_17$<i>17<i>18]"
                        + "[(18-19)s:i|a:i|<>:base/s:s$<b>64<i>18<i>18<i>18<b>1|_18$<i>18<i>19]"
                        + "[(19-20)s:d|a:m|a:j|<>:base/s:s$<b>64<i>19<i>20<i>20<b>1|_19$<i>19<i>20]"
                        + "[(20-21)s:m|a:d|_20$<i>20<i>21]"
                        + "[(21-22)s:c|<>:base/s:s$<b>64<i>21<i>31<i>31<b>1|_21$<i>21<i>22]"
                        + "[(22-23)s:g|a:k|a:e|_22$<i>22<i>23]"
                        + "[(23-24)s:d|_23$<i>23<i>24]"
                        + "[(24-25)s:b|a:d|_24$<i>24<i>25]"
                        + "[(25-26)s:h|a:f|_25$<i>25<i>26]"
                        + "[(26-27)s:j|a:m|_26$<i>26<i>27]"
                        + "[(27-28)s:b|a:g|a:a|_27$<i>27<i>28]"
                        + "[(28-29)s:h|_28$<i>28<i>29]"
                        + "[(29-30)s:e|a:b|a:h|_29$<i>29<i>30]"
                        + "[(30-31)s:f|a:c|_30$<i>30<i>31]"
                        + "[(31-32)s:b|a:d|_31$<i>31<i>32]");
        return fd;
    }



    @Test
    public void testSequenceQueryInSentence () throws IOException {

        Krill ks = new Krill();

        SpanQuery stq = new SpanClassQuery(
                new SpanTermQuery(new Term("base", "s:c")), (byte) 129);
        SpanQuery stq2 = new SpanClassQuery(
                new SpanOrQuery(new SpanTermQuery(new Term("base", "s:a")),
                        new SpanTermQuery(new Term("base", "s:b"))),
                (byte) 129);

        DistanceConstraint dc = new DistanceConstraint(
                new SpanElementQuery("base", "base/s:s"), 0, 0, true, false);

        SpanDistanceQuery sdq = new SpanDistanceQuery(stq, stq2, dc, true);

        // Create Virtual collections:
        KrillCollection kc = new KrillCollection();
        kc.filter(kc.build().term("docGroup", "a"));
        ks.setCollection(kc).getMeta().setStartIndex(0).setCount((short) 20)
                .setContext(
                        new SearchContext(true, (short) 5, true, (short) 5));

        ks.setSpanQuery(sdq);

        assertEquals(
                "spanElementDistance({129: base:s:c}, "
                        + "{129: spanOr([base:s:a, base:s:b])}, "
                        + "[(base/s:s[0:0], ordered, notExcluded)])",
                sdq.toString());

        KrillIndex ki = new KrillIndex();
        ki.addDoc(createFieldDoc2());
        ki.commit();

        Result kr = ks.apply(ki);

        assertEquals("... ceech[[cb]]immmi ...", kr.getMatch(0).getSnippetBrackets());
        assertEquals("... mmidm[[cgdb]]hjbhe ...",kr.getMatch(1).getSnippetBrackets());
        assertEquals("... mmidm[[cgdbhjb]]hefb", kr.getMatch(2).getSnippetBrackets());
        assertEquals("... mmidm[[cgdbhjbhefb]]", kr.getMatch(2).getSnippetBrackets());
        
        assertEquals(4, kr.getTotalResults());
    }
}
