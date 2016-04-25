package de.ids_mannheim.korap.index;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.junit.Test;

import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.query.SpanNextQuery;
import de.ids_mannheim.korap.query.SpanRepetitionQuery;
import de.ids_mannheim.korap.response.Result;

public class TestRepetitionIndex {

    private KrillIndex ki;
    private Result kr;


    private FieldDocument createFieldDoc0 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-0");
        fd.addTV("base", "text", "[(0-1)s:c|_1$<i>0<i>1]"
                + "[(1-2)s:e|_2$<i>1<i>2]"
                + "[(2-3)s:c|_3$<i>2<i>3|<>:y$<b>64<i>2<i>4<i>4<b>0]"
                + "[(3-4)s:c|s:b|_4$<i>3<i>4|<>:x$<b>64<i>3<i>7<i>7<b>0]"
                + "[(4-5)s:e|s:d|_5$<i>4<i>5|<>:y$<b>64<i>4<i>6<i>6<b>0]"
                + "[(5-6)s:c|_6$<i>5<i>6|<>:y$<b>64<i>5<i>8<i>8]"
                + "[(6-7)s:d|_7$<i>6<i>7<b>0]"
                + "[(7-8)s:e|_8$<i>7<i>8|<>:x$<b>64<i>7<i>9<i>9<b>0]"
                + "[(8-9)s:e|s:b|_9$<i>8<i>9|<>:x$<b>64<i>8<i>10<i>10<b>0]"
                + "[(9-10)s:d|_10$<i>9<i>10]");
        return fd;
    }


    private FieldDocument createFieldDoc1 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-1");
        fd.addTV("base", "text", "[(0-1)s:b|_1$<i>0<i>1]"
                + "[(1-2)s:e|_2$<i>1<i>2]" + "[(2-3)s:c|_3$<i>2<i>3]"
                + "[(3-4)s:c|s:d]" + "[(4-5)s:d|s:c|_5$<i>4<i>5]"
                + "[(5-6)s:e|s:c|_6$<i>5<i>6]" + "[(6-7)s:e|_7$<i>6<i>7]"
                + "[(7-8)s:c|_8$<i>7<i>8]" + "[(8-9)s:d|_9$<i>8<i>9]"
                + "[(9-10)s:d|_10$<i>9<i>10]");
        return fd;
    }


    private FieldDocument createFieldDoc2 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-2");
        fd.addTV("base", "text",
                "[(0-1)s:b|s:c|_1$<i>0<i>1|<>:s$<b>64<i>0<i>2<i>1<b>0]"
                        + "[(1-2)s:c|_2$<i>1<i>2]"
                        + "[(2-3)s:b|_3$<i>2<i>3|<>:s$<b>64<i>2<i>3<i>3<b>0]"
                        + "[(3-4)s:c|_4$<i>3<i>4|<>:s$<b>64<i>3<i>4<i>4<b>0]"
                        + "[(4-5)s:c|_5$<i>4<i>5|<>:s$<b>64<i>4<i>5<i>5]"
                        + "[(5-6)s:b|_6$<i>5<i>6<b>0]"
                        + "[(6-7)s:c|_7$<i>6<i>7|<>:s$<b>64<i>6<i>7<i>7<b>0]");
        return fd;
    }


    private FieldDocument createFieldDoc3 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-3");
        fd.addTV("base", "text",
                "[(0-1)s:a|_1$<i>0<i>1|<>:s$<b>64<i>0<i>2<i>1<b>0]"
                        + "[(1-2)s:d|_2$<i>1<i>2|<>:s$<b>64<i>1<i>2<i>3]"
                        + "[(2-3)s:e|_3$<i>2<i>3<b>0]");
        return fd;
    }


    @Test
    public void testCase1 () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc0());
        ki.commit();

        SpanQuery sq, sq2;
        // Quantifier only
        // c{1,2}
        sq = new SpanRepetitionQuery(
                new SpanTermQuery(new Term("base", "s:c")), 1, 2, true);
        kr = ki.search(sq, (short) 10);
        // 0-1, 2-3, 2-4, 3-4, 5-6
        assertEquals((long) 5, kr.getTotalResults());

        // ec{1,2}
        sq = new SpanNextQuery(new SpanTermQuery(new Term("base", "s:e")),
                new SpanRepetitionQuery(new SpanTermQuery(new Term("base",
                        "s:c")), 1, 2, true));

        kr = ki.search(sq, (short) 10);
        // 1-3, 1-4, 4-6
        assertEquals((long) 3, kr.getTotalResults());

        // ec{1,2}d
        sq2 = new SpanNextQuery(sq, new SpanTermQuery(new Term("base", "s:d")));
        kr = ki.search(sq2, (short) 10);
        assertEquals((long) 2, kr.getTotalResults());
        assertEquals(1, kr.getMatch(0).startPos);
        assertEquals(5, kr.getMatch(0).endPos);
        assertEquals(4, kr.getMatch(1).startPos);
        assertEquals(7, kr.getMatch(1).endPos);

        // Multiple documents        
        ki.addDoc(createFieldDoc1());
        ki.commit();
        kr = ki.search(sq2, (short) 10);
        assertEquals((long) 5, kr.getTotalResults());
    }


    /** Skip to */
    @Test
    public void testCase2 () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc0());
        ki.addDoc(createFieldDoc3());
        ki.addDoc(createFieldDoc2());
        ki.addDoc(createFieldDoc1());
        ki.commit();

        SpanQuery sq;
        // c{2,2}
        // sq = new SpanRepetitionQuery(
        // new SpanTermQuery(new Term("base", "s:c")), 2, 2, true);
        // kr = ki.search(sq, (short) 10);
        // // doc1 2-4, 3-5, 4-6
        // assertEquals((long) 6, kr.getTotalResults());

        // ec{2,2}
        sq = new SpanNextQuery(new SpanTermQuery(new Term("base", "s:e")),
                new SpanRepetitionQuery(new SpanTermQuery(new Term("base",
                        "s:c")), 2, 2, true));

        kr = ki.search(sq, (short) 10);
        assertEquals((long) 2, kr.getTotalResults());
        assertEquals(3, kr.getMatch(1).getLocalDocID());

    }


    /** OR */
    @Test
    public void testCase3 () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc0());
        ki.commit();

        SpanQuery sq, sq2;
        // ec{1,2}
        sq = new SpanNextQuery(new SpanTermQuery(new Term("base", "s:e")),
                new SpanOrQuery(new SpanRepetitionQuery(new SpanTermQuery(
                        new Term("base", "s:c")), 1, 1, true),
                        new SpanRepetitionQuery(new SpanTermQuery(new Term(
                                "base", "s:b")), 1, 1, true)));
        kr = ki.search(sq, (short) 10);
        assertEquals((long) 3, kr.getTotalResults());
        assertEquals(1, kr.getMatch(0).startPos);
        assertEquals(3, kr.getMatch(0).endPos);
        assertEquals(4, kr.getMatch(1).startPos);
        assertEquals(6, kr.getMatch(1).endPos);
        assertEquals(7, kr.getMatch(2).startPos);
        assertEquals(9, kr.getMatch(2).endPos);

    }


    @Test
    public void testCase4 () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc1());
        ki.commit();

        SpanQuery sq;
        // c{2,2}
        sq = new SpanRepetitionQuery(
                new SpanTermQuery(new Term("base", "s:c")), 1, 3, true);
        kr = ki.search(sq, (short) 10);
        // 2-3, 2-4, 2-5, 3-4, 3-5, 3-6, 4-5, 4-6, 5-6, 7-8  
        assertEquals((long) 10, kr.getTotalResults());

        sq = new SpanRepetitionQuery(
                new SpanTermQuery(new Term("base", "s:c")), 2, 3, true);
        kr = ki.search(sq, (short) 10);
        // 2-4, 2-5, 3-5, 3-6, 4-6 
        assertEquals((long) 5, kr.getTotalResults());

        //        System.out.print(kr.getTotalResults()+"\n");
        //		for (int i=0; i< kr.getTotalResults(); i++){
        //			System.out.println(
        //				kr.match(i).getLocalDocID()+" "+
        //				kr.match(i).startPos + " " +
        //				kr.match(i).endPos
        //			);
        //		}
    }


    @Test
    public void testCase5 () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(getClass().getResourceAsStream("/wiki/00001.json.gz"), true);
        ki.commit();

        SpanQuery sq0, sq1, sq2;
        sq0 = new SpanTermQuery(new Term("tokens", "tt/p:NN"));
        sq1 = new SpanRepetitionQuery(new SpanTermQuery(new Term("tokens",
                "tt/p:ADJA")), 2, 3, true);
        sq2 = new SpanNextQuery(sq1, sq0);
        kr = ki.search(sq2, (short) 10);

        assertEquals((long) 2, kr.getTotalResults());
        assertEquals(73, kr.getMatch(0).getStartPos());
        assertEquals(77, kr.getMatch(0).getEndPos());
        assertEquals(74, kr.getMatch(1).getStartPos());
        assertEquals(77, kr.getMatch(1).getEndPos());
        /* for (Match km : kr.getMatches()){
         	System.out.println(km.getSnippetBrackets());
         	System.out.println(km.getStartPos() +","+km.getEndPos());
         }*/

        sq2 = new SpanNextQuery(new SpanTermQuery(new Term("tokens",
                "s:offenen")), sq2);
        kr = ki.search(sq2, (short) 10);

        assertEquals((long) 1, kr.getTotalResults());
        assertEquals(73, kr.getMatch(0).getStartPos());
        assertEquals(77, kr.getMatch(0).getEndPos());
        /*
        for (Match km : kr.getMatches()){
        	System.out.println(km.getSnippetBrackets());
        	System.out.println(km.getStartPos() +","+km.getEndPos());
        }*/
    }
}
