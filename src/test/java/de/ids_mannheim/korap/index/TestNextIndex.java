package de.ids_mannheim.korap.index;

import static de.ids_mannheim.korap.TestSimple.simpleFieldDoc;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.TestSimple;
import de.ids_mannheim.korap.query.SpanClassQuery;
import de.ids_mannheim.korap.query.SpanElementQuery;
import de.ids_mannheim.korap.query.SpanExpansionQuery;
import de.ids_mannheim.korap.query.SpanFocusQuery;
import de.ids_mannheim.korap.query.SpanNextQuery;
import de.ids_mannheim.korap.query.wrap.SpanSequenceQueryWrapper;
import de.ids_mannheim.korap.response.Match;
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.util.QueryException;

@RunWith(JUnit4.class)
public class TestNextIndex {

    // Todo: primary data as a non-indexed field separated.
    
//    @Test
    public void fuzzyTest () throws IOException, QueryException {
        List<String> chars = Arrays.asList("a", "b", "c", "c","d", "e");

        // c c a
        SpanTermQuery stq = new SpanTermQuery(new Term("base", "s:c"));
        SpanTermQuery stq2 = new SpanTermQuery(new Term("base", "s:a"));
        SpanNextQuery snq = new SpanNextQuery(stq, stq);
        SpanNextQuery snq2 = new SpanNextQuery(snq, stq2);

        Pattern resultPattern = Pattern.compile("cca");
        TestSimple.fuzzingTest(chars, resultPattern, snq2,
                               5, 10, 8, 0);
    }
    
    @Test
    public void testInfiniteSkipTo () throws IOException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(simpleFieldDoc("ddc"));
        ki.addDoc(simpleFieldDoc("cc"));
        ki.addDoc(simpleFieldDoc("abedaed"));
        ki.commit();

        //cca
        SpanTermQuery stq = new SpanTermQuery(new Term("base", "s:c"));
        SpanTermQuery stq2 = new SpanTermQuery(new Term("base", "s:a"));
        SpanNextQuery snq = new SpanNextQuery(stq, stq);
        SpanNextQuery snq2 = new SpanNextQuery(snq, stq2);
        
        Result kr = ki.search(snq2, (short) 10);
        assertEquals(0, kr.getTotalResults());
    }
    
    @Test
    public void testNextExpansionBug () throws IOException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(simpleFieldDoc("ccecc"));
        ki.commit();

        SpanTermQuery stq = new SpanTermQuery(new Term("base", "s:c"));
        SpanExpansionQuery seq = new SpanExpansionQuery(stq, 0, 2, 0, true);
        Result kr = ki.search(seq, (short) 20);
//        assertEquals(8, kr.getTotalResults());
        assertEquals(12, kr.getTotalResults());
        
        SpanNextQuery snq = new SpanNextQuery(seq, stq);
        kr = ki.search(snq, (short) 10);

        // cc ccec cec cecc cc
        // 1-3 1-5 2-5 2-6 4-6
        assertEquals(5, kr.getTotalResults());
    }
    
    @Test
    public void testNextExpansionBug2 () throws IOException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(simpleFieldDoc("cccc"));
        ki.commit();

        SpanTermQuery stq = new SpanTermQuery(new Term("base", "s:c"));
        SpanExpansionQuery seq = new SpanExpansionQuery(stq, 0, 2, 0, true);
//        Result kr = ki.search(seq, (short) 20);
//        assertEquals(12, kr.getTotalResults());
        
        SpanNextQuery snq = new SpanNextQuery(seq, stq);
        Result kr = ki.search(snq, (short) 10);

        // cc ccc cccc cc  ccc cc
        // 1-3 1-4 1-5 2-4 2-5 3-5
        assertEquals(6, kr.getTotalResults());
    }
    
    @Test
    public void testNextExpansionBug3 () throws IOException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(simpleFieldDoc("r a d m d d v b", " "));
        ki.commit();
        
        SpanTermQuery stq = new SpanTermQuery(new Term("base", "s:d"));
        SpanExpansionQuery seq = new SpanExpansionQuery(stq, 0, 4, 0, true);
        Result kr = ki.search(seq, (short) 20);
        
        SpanNextQuery snq = new SpanNextQuery(seq, stq);
        kr = ki.search(snq, (short) 10);
        
        // 2-5, 2-6, 4-6
        assertEquals(3, kr.getTotalResults());
    }
    
    @Test
    public void testNextRightExpansion () throws IOException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(simpleFieldDoc("F u G S h A d F ü d T F u d m", " "));
        ki.commit();
        
        SpanTermQuery stq = new SpanTermQuery(new Term("base", "s:d"));
        SpanExpansionQuery seq = new SpanExpansionQuery(stq, 0, 6, 0, true);
        SpanNextQuery snq = new SpanNextQuery(seq,stq);
        assertEquals("spanNext(spanExpansion(base:s:d, []{0, 6}, right), base:s:d)", snq.toString());
        Result kr = ki.search(snq, (short) 10);

//        6,10 FuGShA[[dFüd]]TFudm
//        6,14 FuGShA[[dFüdTFud]]m
//        9,14 ... ShAdFü[[dTFud]]
        assertEquals(3, kr.getTotalResults());
    }
    
    @Test
    public void testNextLeftExpansion () throws IOException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(simpleFieldDoc("F u G S h A d F ü d T F u d m", " "));
        ki.commit();
        
        SpanTermQuery stq = new SpanTermQuery(new Term("base", "s:d"));
        SpanExpansionQuery seq = new SpanExpansionQuery(stq, 0, 6, -1, true);
        Result kr = ki.search(seq, (short) 20);
        
        assertEquals(21, kr.getTotalResults());
        
        SpanNextQuery snq = new SpanNextQuery(stq,seq);
        assertEquals("spanNext(base:s:d, spanExpansion(base:s:d, []{0, 6}, left))", snq.toString());
        kr = ki.search(snq, (short) 10);
        
        assertEquals(3, kr.getTotalResults());
    }
    
    @Test
    public void indexExample1 () throws IOException {
        KrillIndex ki = new KrillIndex();

        // abcabcabac
        FieldDocument fd = new FieldDocument();
        fd.addTV("base", "abcabcabac", "[(0-1)s:a|i:a|_0$<i>0<i>1|-:t$<i>10]"
                + "[(1-2)s:b|i:b|_1$<i>1<i>2]" + "[(2-3)s:c|i:c|_2$<i>2<i>3]"
                + "[(3-4)s:a|i:a|_3$<i>3<i>4]" + "[(4-5)s:b|i:b|_4$<i>4<i>5]"
                + "[(5-6)s:c|i:c|_5$<i>5<i>6]" + "[(6-7)s:a|i:a|_6$<i>6<i>7]"
                + "[(7-8)s:b|i:b|_7$<i>7<i>8]" + "[(8-9)s:a|i:a|_8$<i>8<i>9]"
                + "[(9-10)s:c|i:c|_9$<i>9<i>10]");
        ki.addDoc(fd);
        ki.commit();

        SpanQuery sq;
        Result kr;

        sq = new SpanNextQuery(new SpanTermQuery(new Term("base", "s:a")),
                new SpanTermQuery(new Term("base", "s:b")));

        kr = ki.search(sq, (short) 10);

        assertEquals("totalResults", kr.getTotalResults(), 3);
        assertEquals("StartPos (0)", 0, kr.getMatch(0).startPos);
        assertEquals("EndPos (0)", 2, kr.getMatch(0).endPos);
        assertEquals("StartPos (1)", 3, kr.getMatch(1).startPos);
        assertEquals("EndPos (1)", 5, kr.getMatch(1).endPos);
        assertEquals("StartPos (2)", 6, kr.getMatch(2).startPos);
        assertEquals("EndPos (2)", 8, kr.getMatch(2).endPos);

        sq = new SpanNextQuery(new SpanTermQuery(new Term("base", "s:b")),
                new SpanTermQuery(new Term("base", "s:c")));

        kr = ki.search(sq, (short) 10);

        assertEquals("totalResults", kr.getTotalResults(), 2);
        assertEquals("StartPos (0)", 1, kr.getMatch(0).startPos);
        assertEquals("EndPos (0)", 3, kr.getMatch(0).endPos);
        assertEquals("StartPos (1)", 4, kr.getMatch(1).startPos);
        assertEquals("EndPos (1)", 6, kr.getMatch(1).endPos);

        assertEquals(1, ki.numberOf("base", "documents"));
        assertEquals(10, ki.numberOf("base", "t"));

        sq = new SpanNextQuery(new SpanTermQuery(new Term("base", "s:a")),
                new SpanNextQuery(new SpanTermQuery(new Term("base", "s:b")),
                        new SpanTermQuery(new Term("base", "s:c"))));

        kr = ki.search(sq, (short) 2);

        assertEquals("totalResults", kr.getTotalResults(), 2);
        assertEquals("StartPos (0)", 0, kr.getMatch(0).startPos);
        assertEquals("EndPos (0)", 3, kr.getMatch(0).endPos);
        assertEquals("StartPos (1)", 3, kr.getMatch(1).startPos);
        assertEquals("EndPos (1)", 6, kr.getMatch(1).endPos);

        assertEquals(1, ki.numberOf("base", "documents"));
        assertEquals(10, ki.numberOf("base", "t"));

    };

    @Test
    public void indexExample2 () throws IOException {
        KrillIndex ki = new KrillIndex();

        // abcabcabac
        FieldDocument fd = new FieldDocument();
        fd.addTV("base", "abcabcabac", "[(0-1)s:a|i:a|_0$<i>0<i>1|-:t$<i>10]"
                + "[(1-2)s:b|i:b|_1$<i>1<i>2]" + "[(2-3)s:c|i:c|_2$<i>2<i>3]"
                + "[(3-4)s:a|i:a|_3$<i>3<i>4|<>:x$<b>64<i>3<i>4<i>4<b>0|<>:x$<b>64<i>3<i>7<i>7<b>0]"
                + "[(4-5)s:b|i:b|_4$<i>4<i>5]" + "[(5-6)s:c|i:c|_5$<i>5<i>6]"
                + "[(6-7)s:a|i:a|_6$<i>6<i>7]" + "[(7-8)s:b|i:b|_7$<i>7<i>8]"
                + "[(8-9)s:a|i:a|_8$<i>8<i>9]"
                + "[(9-10)s:c|i:c|_9$<i>9<i>10]");
        ki.addDoc(fd);

        ki.commit();

        SpanQuery sq;
        Result kr;

        sq = new SpanNextQuery(new SpanTermQuery(new Term("base", "s:c")),
                new SpanElementQuery("base", "x"));

        kr = ki.search(sq, (short) 10);
        assertEquals("ab[[cabca]]bac", kr.getMatch(1).getSnippetBrackets());

    };

    @Test
    public void indexExample3 () throws IOException {
        KrillIndex ki = new KrillIndex();

        // abcabcabac
        FieldDocument fd = new FieldDocument();
        fd.addTV("base", "abcabcabac", "[(0-1)s:a|i:a|_0$<i>0<i>1|-:t$<i>10]"
                + "[(1-2)s:b|i:b|_1$<i>1<i>2]" + "[(2-3)s:c|i:c|_2$<i>2<i>3]"
                + "[(3-4)s:a|i:a|_3$<i>3<i>4|<>:x$<b>64<i>3<i>7<i>7<b>0]"
                + "[(4-5)s:b|i:b|_4$<i>4<i>5]" + "[(5-6)s:c|i:c|_5$<i>5<i>6]"
                + "[(6-7)s:a|i:a|_6$<i>6<i>7]" + "[(7-8)s:b|i:b|_7$<i>7<i>8]"
                + "[(8-9)s:a|i:a|_8$<i>8<i>9]"
                + "[(9-10)s:c|i:c|_9$<i>9<i>10]");
        ki.addDoc(fd);

        ki.commit();

        SpanQuery sq;
        Result kr;

        sq = new SpanNextQuery(new SpanElementQuery("base", "x"),
                new SpanTermQuery(new Term("base", "s:b")));

        kr = ki.search(sq, (short) 10);
        assertEquals("abc[[abcab]]ac", kr.getMatch(0).getSnippetBrackets());
    };

    @Test
    public void indexExample4 () throws IOException {
        KrillIndex ki = new KrillIndex();

        // abcabcabac
        // abc<x>abc<x>a</x>b</x>ac
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-1");
        fd.addTV("base", "abcabcabac", "[(0-1)s:a|i:a|_0$<i>0<i>1|-:t$<i>10]"
                + "[(1-2)s:b|i:b|_1$<i>1<i>2]" + "[(2-3)s:c|i:c|_2$<i>2<i>3]"
                + "[(3-4)s:a|i:a|_3$<i>3<i>4|<>:x$<b>64<i>3<i>7<i>7<b>0]"
                + "[(4-5)s:b|i:b|_4$<i>4<i>5]" + "[(5-6)s:c|i:c|_5$<i>5<i>6]"
                + "[(6-7)s:a|i:a|_6$<i>6<i>7]<>:x$<b>64<i>6<i>8<i>8<b>0]"
                + "[(7-8)s:b|i:b|_7$<i>7<i>8]" + "[(8-9)s:a|i:a|_8$<i>8<i>9]"
                + "[(9-10)s:c|i:c|_9$<i>9<i>10]");
        ki.addDoc(fd);

        // xbz<x>xbzx</x>bxz
        fd = new FieldDocument();
        fd.addString("ID", "doc-2");
        fd.addTV("base", "xbzxbzxbxz", "[(0-1)s:x|i:x|_0$<i>0<i>1|-:t$<i>10]"
                + "[(1-2)s:b|i:b|_1$<i>1<i>2]" + "[(2-3)s:z|i:z|_2$<i>2<i>3]"
                + "[(3-4)s:x|i:x|_3$<i>3<i>4|<>:x$<b>64<i>3<i>7<i>7<b>0]"
                + "[(4-5)s:b|i:b|_4$<i>4<i>5]" + "[(5-6)s:z|i:z|_5$<i>5<i>6]"
                + "[(6-7)s:x|i:x|_6$<i>6<i>7]" + "[(7-8)s:b|i:b|_7$<i>7<i>8]"
                + "[(8-9)s:x|i:x|_8$<i>8<i>9]"
                + "[(9-10)s:z|i:z|_9$<i>9<i>10]");
        ki.addDoc(fd);
        ki.commit();

        SpanQuery sq;
        Result kr;

        sq = new SpanNextQuery(new SpanElementQuery("base", "x"),
                new SpanTermQuery(new Term("base", "s:b")));

        kr = ki.search(sq, (short) 10);
        assertEquals("TotalResults", kr.getTotalResults(), 2);
        assertEquals("abc[[abcab]]ac", kr.getMatch(0).getSnippetBrackets());
        assertEquals("xbz[[xbzxb]]xz", kr.getMatch(1).getSnippetBrackets());

        sq = new SpanNextQuery(new SpanTermQuery(new Term("base", "s:c")),
                new SpanElementQuery("base", "x"));

        kr = ki.search(sq, (short) 10);
        assertEquals(1, kr.getTotalResults());
        assertEquals("ab[[cabca]]bac", kr.getMatch(0).getSnippetBrackets());

        sq = new SpanNextQuery(new SpanTermQuery(new Term("base", "s:z")),
                new SpanElementQuery("base", "x"));

        kr = ki.search(sq, (short) 10);
        assertEquals(1, kr.getTotalResults());
        assertEquals("xb[[zxbzx]]bxz", kr.getMatch(0).getSnippetBrackets());
    };

    /**
     * Multiple atomic indices
     * Skip to a greater doc#
     */
    @Test
    public void indexExample5 () throws IOException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(createFieldDoc1());
        ki.addDoc(createFieldDoc2());
        ki.commit();
        ki.addDoc(createFieldDoc3());
        ki.commit();

        SpanQuery sq =
                new SpanNextQuery(new SpanTermQuery(new Term("base", "s:d")),
                        new SpanTermQuery(new Term("base", "s:b")));
        Result kr = ki.search(sq, (short) 10);

        assertEquals("totalResults", kr.getTotalResults(), 2);
        // Match #0
        assertEquals("doc-number", 0, kr.getMatch(0).getLocalDocID());
        assertEquals("StartPos", 4, kr.getMatch(0).startPos);
        assertEquals("EndPos", 6, kr.getMatch(0).endPos);
        // Match #1
        assertEquals("doc-number", 0, kr.getMatch(1).getLocalDocID());
        assertEquals("StartPos", 1, kr.getMatch(1).startPos);
        assertEquals("EndPos", 3, kr.getMatch(1).endPos);

        sq = new SpanNextQuery(new SpanTermQuery(new Term("base", "s:b")),
                new SpanTermQuery(new Term("base", "s:d")));
        kr = ki.search(sq, (short) 10);

        assertEquals("totalResults", kr.getTotalResults(), 1);
        assertEquals("doc-number", 0, kr.getMatch(0).getLocalDocID());
        assertEquals("StartPos", 2, kr.getMatch(0).startPos);
        assertEquals("EndPos", 4, kr.getMatch(0).endPos);
    }

    /** Skip to NextSpan */
    @Test
    public void indexExample6 () throws IOException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(createFieldDoc1());
        ki.addDoc(createFieldDoc2());
        ki.addDoc(createFieldDoc3());
        ki.commit();

        SpanQuery sq =
                new SpanNextQuery(new SpanTermQuery(new Term("base", "s:c")),
                        new SpanNextQuery(
                                new SpanTermQuery(new Term("base", "s:d")),
                                new SpanTermQuery(new Term("base", "s:b"))));

        Result kr = ki.search(sq, (short) 10);
        assertEquals("totalResults", kr.getTotalResults(), 1);
        assertEquals("doc-number", 2, kr.getMatch(0).getLocalDocID());
        assertEquals("StartPos", 0, kr.getMatch(0).startPos);
        assertEquals("EndPos", 3, kr.getMatch(0).endPos);

        sq = new SpanNextQuery(
                new SpanTermQuery(
                        new Term("base", "s:c")),
                new SpanNextQuery(
                        new SpanFocusQuery(new SpanClassQuery(
                                new SpanTermQuery(new Term("base", "s:d")),
                                (byte) 1), (byte) 1),
                        new SpanFocusQuery(new SpanClassQuery(
                                new SpanTermQuery(new Term("base", "s:b")),
                                (byte) 2), (byte) 2)));

        kr = ki.search(sq, (short) 10);
        assertEquals("doc-number", 2, kr.getMatch(0).getLocalDocID());
        assertEquals("StartPos", 0, kr.getMatch(0).startPos);
        assertEquals("EndPos", 3, kr.getMatch(0).endPos);

        // for (Match km : kr.getMatches()) {
        // System.out.println(km.getStartPos() + "," + km.getEndPos()
        // + " "
        // + km.getSnippetBrackets());
        // }
    }

    @Test
    public void indexExample7Distances () throws Exception {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(createFieldDoc1());
        ki.addDoc(createFieldDoc2());
        ki.addDoc(createFieldDoc3());
        ki.addDoc(createFieldDoc4());
        ki.commit();

        SpanSequenceQueryWrapper sq = new SpanSequenceQueryWrapper("base");
        sq.append("i:b").append("i:d").withConstraint(1, 3);

        Result kr = ki.search(sq.toQuery(), (short) 10);

        assertEquals("totalResults", kr.getTotalResults(), 3);
        assertEquals("doc-number", "match-doc-0-p2-5", kr.getMatch(0).getID());
        assertEquals("doc-number", "match-doc-2-p2-4", kr.getMatch(1).getID());
        assertEquals("doc-number", "match-doc-3-p2-5", kr.getMatch(2).getID());
    };

    @Test
    public void indexExample8Distances () throws Exception {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(createFieldDoc1());
        ki.addDoc(createFieldDoc2());
        ki.addDoc(createFieldDoc3());
        ki.addDoc(createFieldDoc4());
        ki.commit();

        SpanSequenceQueryWrapper sq = new SpanSequenceQueryWrapper("base");
        sq.append("i:a").append("i:b").withConstraint(0, 3, "e");

        Result kr = ki.search(sq.toQuery(), (short) 10);

        assertEquals("totalResults", kr.getTotalResults(), 3);
        assertEquals("doc-number", "match-doc-0-p3-6", kr.getMatch(0).getID());
        assertEquals("doc-number", "match-doc-1-p1-3", kr.getMatch(1).getID());
        assertEquals("doc-number", "match-doc-3-p3-6", kr.getMatch(2).getID());
    };

    @Test
    public void indexExample9 () throws IOException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(createFieldDoc1());
        ki.commit();

        SpanQuery sq = new SpanNextQuery(
                new SpanOrQuery(new SpanTermQuery(new Term("base", "s:a")),
                        new SpanTermQuery(new Term("base", "s:b"))),
                new SpanTermQuery(new Term("base", "s:c")));

        Result kr = ki.search(sq, (short) 10);

        assertEquals(0, kr.getMatch(0).getStartPos());
        assertEquals(2, kr.getMatch(0).getEndPos());
        assertEquals(3, kr.getMatch(1).getStartPos());
        assertEquals(5, kr.getMatch(1).getEndPos());
    };

    @Test
    public void sequenceSkipBug () throws IOException {
        KrillIndex ki = new KrillIndex();

        ki.addDoc(createFieldDoc1());
        ki.addDoc(createFieldDoc3());
        ki.addDoc(createFieldDoc4());
        ki.addDoc(createFieldDoc5()); // match for 2
        ki.addDoc(createFieldDoc1());
        ki.addDoc(createFieldDoc3());
        ki.addDoc(createFieldDoc4());
        ki.addDoc(createFieldDoc1());
        ki.addDoc(createFieldDoc1());
        ki.addDoc(createFieldDoc3());
        ki.addDoc(createFieldDoc1());
        ki.commit();

        ki.addDoc(createFieldDoc5()); // match for 2
        ki.addDoc(createFieldDoc1());
        ki.addDoc(createFieldDoc2()); // match for 1 and 2
        ki.addDoc(createFieldDoc1());
        ki.addDoc(createFieldDoc3());
        ki.addDoc(createFieldDoc4());
        ki.addDoc(createFieldDoc1());
        ki.addDoc(createFieldDoc3());

        ki.commit();

        // "cab" is in 2
        SpanQuery sq = new SpanNextQuery(
                new SpanNextQuery(new SpanTermQuery(new Term("base", "s:c")),
                        new SpanTermQuery(new Term("base", "s:a"))),
                new SpanTermQuery(new Term("base", "s:b")));

        Result kr = ki.search(sq, (short) 10);

        assertEquals(0, kr.getMatch(0).getStartPos());
        assertEquals(3, kr.getMatch(0).getEndPos());
        assertEquals("totalResults", kr.getTotalResults(), 1);

        // "aba" is in 2 and 5
        sq = new SpanNextQuery(
                new SpanNextQuery(new SpanTermQuery(new Term("base", "s:a")),
                        new SpanTermQuery(new Term("base", "s:b"))),
                new SpanTermQuery(new Term("base", "s:a")));

        kr = ki.search(sq, (short) 10);
        assertEquals("totalResults", kr.getTotalResults(), 3);
    };

    private FieldDocument createFieldDoc1 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-0");
        fd.addTV("base", "bbadb", // bba[dc]b
                "[(0-1)s:b|i:b|_0$<i>0<i>1]" + "[(1-2)s:c|i:c|s:b|_1$<i>1<i>2]"
                        + "[(2-3)s:b|i:b|_2$<i>2<i>3]"
                        + "[(3-4)s:a|i:a|_3$<i>3<i>4|<>:e$<b>64<i>3<i>6<i>6<b>0]"
                        + "[(4-5)s:d|i:d|s:c|_4$<i>4<i>5]"
                        + "[(5-6)s:b|i:b|_5$<i>5<i>6]");
        return fd;
    }

    private FieldDocument createFieldDoc2 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-1");
        fd.addTV("base", "caba", // c[ac][ba]a
                "[(0-1)s:c|i:c|_0$<i>0<i>1]"
                        + "[(1-2)s:a|i:a|s:c|_1$<i>1<i>2|<>:e$<b>64<i>1<i>3<i>3<b>0]"
                        + "[(2-3)s:b|i:b|s:a|_2$<i>2<i>3]"
                        + "[(3-4)s:a|i:a|_3$<i>3<i>4]");
        return fd;
    }

    private FieldDocument createFieldDoc3 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-2");
        fd.addTV("base", "cdbd", // c[ba]d
                "[(0-1)s:c|i:c|_0$<i>0<i>1]" + "[(1-2)s:d|i:d|_1$<i>1<i>2]"
                        + "[(2-3)s:b|i:b|s:a|_2$<i>2<i>3]"
                        + "[(3-4)s:d|i:d|_3$<i>3<i>4]");

        return fd;
    }

    private FieldDocument createFieldDoc4 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-3");
        fd.addTV("base", "bcbadb", // b[cb]ba[dc]b
                "[(0-1)s:b|i:b|_0$<i>0<i>1]"
                        + "[(1-2)s:c|i:c|s:b|<>:s$<b>64<i>1<i>3<i>3<b>0|_1$<i>1<i>2<b>0]"
                        + "[(2-3)s:b|i:b|_2$<i>2<i>3]"
                        + "[(3-4)s:a|i:a|_3$<i>3<i>4|<>:e$<b>64<i>3<i>6<i>6<b>0]"
                        + "[(4-5)s:d|i:d|s:c|_4$<i>4<i>5]"
                        + "[(5-6)s:b|i:b|_5$<i>5<i>6]");
        return fd;
    }

    private FieldDocument createFieldDoc5 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-4");
        fd.addTV("base", "dabaca", "[(0-1)s:d|i:d|_0$<i>0<i>1]"
                + "[(1-2)s:a|i:a|_1$<i>1<i>2|<>:e$<b>64<i>1<i>3<i>3<b>0]"
                + "[(2-3)s:b|i:b|_2$<i>2<i>3]" + "[(3-4)s:a|i:a|_3$<i>3<i>4]"
                + "[(4-5)s:c|i:c|_4$<i>4<i>5]" + "[(5-6)s:a|i:a|_5$<i>5<i>6]");
        return fd;
    }

};
