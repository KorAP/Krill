package de.ids_mannheim.korap.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import de.ids_mannheim.korap.KrillCollection;
import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.query.QueryBuilder;
import de.ids_mannheim.korap.query.SpanClassQuery;
import de.ids_mannheim.korap.query.SpanElementQuery;
import de.ids_mannheim.korap.query.SpanFocusQuery;
import de.ids_mannheim.korap.query.SpanNextQuery;
import de.ids_mannheim.korap.query.SpanWithinQuery;
import de.ids_mannheim.korap.response.Match;
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.response.SearchContext;

// mvn -Dtest=TestWithinIndex#indexExample1 test

// match is focus and split

@RunWith(JUnit4.class)
public class TestMatchIndex {
    @Test
    public void testEmbeddedClassQuery () throws IOException {
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

        sq = new SpanFocusQuery(
                new SpanClassQuery(
                        new SpanNextQuery(
                                new SpanClassQuery(
                                        new SpanTermQuery(
                                                new Term("base", "s:b")),
                                        (byte) 1),
                                new SpanClassQuery(
                                        new SpanTermQuery(
                                                new Term("base", "s:c")),
                                        (byte) 2)),
                        (byte) 3),
                (byte) 3);

        kr = ki.search(sq, (short) 10);

        assertEquals("totalResults", kr.getTotalResults(), 2);
        assertEquals("StartPos (0)", 1, kr.getMatch(0).startPos);
        assertEquals("EndPos (0)", 3, kr.getMatch(0).endPos);
        assertEquals("SnippetBrackets (0)", "a[[{3:{1:b}{2:c}}]]abcaba ...",
                kr.getMatch(0).getSnippetBrackets());
        assertEquals("StartPos (1)", 4, kr.getMatch(1).startPos);
        assertEquals("EndPos (1)", 6, kr.getMatch(1).endPos);
        assertEquals("SnippetBrackets (1)", "abca[[{3:{1:b}{2:c}}]]abac",
                kr.getMatch(1).getSnippetBrackets());

        assertEquals("Document count", 1, ki.numberOf("base", "documents"));
        assertEquals("Token count", 10, ki.numberOf("base", "t"));

    }


    @Test
    @Ignore("TODO(kwic-cap): adapt to new HTML KWIC alignment")
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

        sq = new SpanNextQuery(new SpanTermQuery(new Term("base", "s:b")),
                new SpanClassQuery(new SpanTermQuery(new Term("base", "s:a"))));
        kr = ki.search(sq, (short) 10);

        assertEquals("totalResults", kr.getTotalResults(), 1);
        assertEquals("StartPos (0)", 7, kr.getMatch(0).startPos);
        assertEquals("EndPos (0)", 9, kr.getMatch(0).endPos);

        assertEquals("SnippetBrackets (0)", "... bcabca[[b{1:a}]]c",
                kr.getMatch(0).getSnippetBrackets());

        assertEquals("Test no 'more' context",
                "<span class=\"context-left\"><span class=\"more\"></span>bcabca</span><span class=\"match\"><mark>b<mark class=\"class-1 level-0\">a</mark></mark></span><span class=\"context-right\">c</span>",
                kr.getMatch(0).getSnippetHTML());


        sq = new SpanFocusQuery(new SpanNextQuery(
                new SpanTermQuery(new Term("base", "s:b")), new SpanClassQuery(
                        new SpanTermQuery(new Term("base", "s:a")))));
        kr = ki.search(sq, (short) 10);

        assertEquals("totalResults", kr.getTotalResults(), 1);
        assertEquals("StartPos (0)", 8, kr.getMatch(0).startPos);
        assertEquals("EndPos (0)", 9, kr.getMatch(0).endPos);
        assertEquals("SnippetBrackets (0)", "... cabcab[[{1:a}]]c",
                kr.getMatch(0).getSnippetBrackets());
        sq = new SpanFocusQuery(new SpanNextQuery(
                new SpanClassQuery(new SpanTermQuery(new Term("base", "s:a")),
                        (byte) 2),
                new SpanClassQuery(new SpanTermQuery(new Term("base", "s:b")),
                        (byte) 3)),
                (byte) 3);

        kr = ki.search(sq, (short) 10);

        assertEquals("totalResults", kr.getTotalResults(), 3);
        assertEquals("StartPos (0)", 1, kr.getMatch(0).startPos);
        assertEquals("EndPos (0)", 2, kr.getMatch(0).endPos);
        assertEquals("SnippetBrackets (0)", "a[[{3:b}]]cabcab ...",
                kr.getMatch(0).getSnippetBrackets());


        assertEquals(
                "<span class=\"context-left\">a</span><span class=\"match\"><mark><mark class=\"class-3 level-0\">b</mark></mark></span><span class=\"context-right\">cabcab<span class=\"more\"></span></span>",
                kr.getMatch(0).getSnippetHTML());

        assertEquals("StartPos (1)", 4, kr.getMatch(1).startPos);
        assertEquals("EndPos (1)", 5, kr.getMatch(1).endPos);
        assertEquals("SnippetBrackets (1)", "abca[[{3:b}]]cabac",
                kr.getMatch(1).getSnippetBrackets());

        assertEquals(
                "<span class=\"context-left\">abca</span><span class=\"match\"><mark><mark class=\"class-3 level-0\">b</mark></mark></span><span class=\"context-right\">cabac</span>",
                kr.getMatch(1).getSnippetHTML());

        assertEquals("StartPos (2)", 7, kr.getMatch(2).startPos);
        assertEquals("EndPos (2)", 8, kr.getMatch(2).endPos);
        assertEquals("SnippetBrackets (2)", "... bcabca[[{3:b}]]ac",
                kr.getMatch(2).getSnippetBrackets());



        // abcabcabac
        sq = new SpanFocusQuery(new SpanNextQuery(
                new SpanTermQuery(new Term("base", "s:a")),
                new SpanClassQuery(
                        new SpanNextQuery(new SpanTermQuery(
                                new Term("base", "s:b")),
                                new SpanClassQuery(new SpanTermQuery(
                                        new Term("base", "s:a")))),
                        (byte) 2)),
                (byte) 2);

        kr = ki.search(sq, (short) 10);

        assertEquals("totalResults", kr.getTotalResults(), 1);
        assertEquals("SnippetBrackets (0)", "... bcabca[[{2:b{1:a}}]]c",
                kr.getMatch(0).getSnippetBrackets());

        assertEquals("SnippetHTML (0) 1",
                "<span class=\"context-left\"><span class=\"more\"></span>bcabca</span><span class=\"match\"><mark><mark class=\"class-2 level-0\">b<mark class=\"class-1 level-1\">a</mark></mark></mark></span><span class=\"context-right\">c</span>",
                kr.getMatch(0).getSnippetHTML());

        // Offset tokens
        kr = ki.search(sq, 0, (short) 10, true, (short) 2, true, (short) 2);
        assertEquals("totalResults", kr.getTotalResults(), 1);
        assertEquals("SnippetBrackets (0)", "... ca[[{2:b{1:a}}]]c",
                kr.getMatch(0).getSnippetBrackets());



        // Offset Characters
        kr = ki.search(sq, 0, (short) 10, false, (short) 1, false, (short) 0);
        assertEquals("totalResults", kr.getTotalResults(), 1);
        assertEquals("SnippetBrackets (0)", "... a[[{2:b{1:a}}]] ...",
                kr.getMatch(0).getSnippetBrackets());

        assertEquals("SnippetHTML (0) 2",
                "<span class=\"context-left\"><span class=\"more\"></span>a</span><span class=\"match\"><mark><mark class=\"class-2 level-0\">b<mark class=\"class-1 level-1\">a</mark></mark></mark></span><span class=\"context-right\"><span class=\"more\"></span></span>",
                kr.getMatch(0).getSnippetHTML());



        // Don't match the expected class!
        sq = new SpanFocusQuery(new SpanNextQuery(
                new SpanClassQuery(new SpanTermQuery(new Term("base", "s:b")),
                        (byte) 1),
                new SpanClassQuery(new SpanTermQuery(new Term("base", "s:c")),
                        (byte) 2)),
                (byte) 3);

        kr = ki.search(sq, (short) 10);

        assertEquals("totalResults", kr.getTotalResults(), 0);

        sq = new SpanFocusQuery(
                new SpanNextQuery(new SpanTermQuery(new Term("base", "s:a")),
                        new SpanClassQuery(new SpanNextQuery(
                                new SpanTermQuery(new Term("base",
                                        "s:b")),
                                new SpanTermQuery(new Term("base", "s:c"))))));

        kr = ki.search(sq, (short) 2);

        assertEquals("totalResults", kr.getTotalResults(), 2);
        assertEquals("StartPos (0)", 1, kr.getMatch(0).startPos);
        assertEquals("EndPos (0)", 3, kr.getMatch(0).endPos);
        assertEquals("SnippetBrackets (0)", "a[[{1:bc}]]abcaba ...",
                kr.getMatch(0).getSnippetBrackets());
        assertEquals("StartPos (1)", 4, kr.getMatch(1).startPos);
        assertEquals("EndPos (1)", 6, kr.getMatch(1).endPos);
        assertEquals("SnippetBrackets (1)", "abca[[{1:bc}]]abac",
                kr.getMatch(1).getSnippetBrackets());

        assertEquals(1, ki.numberOf("base", "documents"));
        assertEquals(10, ki.numberOf("base", "t"));
    };


    @Test
    @Ignore("TODO(kwic-cap): adapt to new HTML KWIC alignment")
    public void indexExample2 () throws IOException {
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

        // No contexts:
        sq = new SpanOrQuery(new SpanTermQuery(new Term("base", "s:a")),
                new SpanTermQuery(new Term("base", "s:c")));
        kr = ki.search(sq, (short) 20);

        assertEquals("totalResults", kr.getTotalResults(), 7);
        assertEquals("SnippetBrackets (0)",
                "<span class=\"context-left\"></span><span class=\"match\"><mark>a</mark></span><span class=\"context-right\">bcabca<span class=\"more\"></span></span>",
                kr.getMatch(0).getSnippetHTML());
        assertEquals("SnippetBrackets (0)", "[[a]]bcabca ...",
                kr.getMatch(0).getSnippetBrackets());

        assertEquals("SnippetBrackets (1)", "ab[[c]]abcaba ...",
                kr.getMatch(1).getSnippetBrackets());
        assertEquals("SnippetBrackets (1)",
                "<span class=\"context-left\">ab</span><span class=\"match\"><mark>c</mark></span><span class=\"context-right\">abcaba<span class=\"more\"></span></span>",
                kr.getMatch(1).getSnippetHTML());

        assertEquals("SnippetBrackets (6)", "... abcaba[[c]]",
                kr.getMatch(6).getSnippetBrackets());
        assertEquals("SnippetBrackets (6)",
                "<span class=\"context-left\"><span class=\"more\"></span>abcaba</span><span class=\"match\"><mark>c</mark></span><span class=\"context-right\"></span>",
                kr.getMatch(6).getSnippetHTML());

        kr = ki.search(sq, 0, (short) 20, true, (short) 0, true, (short) 0);

        assertEquals("totalResults", kr.getTotalResults(), 7);
        assertEquals("SnippetBrackets (0)", "[[a]] ...",
                kr.getMatch(0).getSnippetBrackets());
        assertEquals("SnippetHTML (0)",
                "<span class=\"context-left\"></span><span class=\"match\"><mark>a</mark></span><span class=\"context-right\"><span class=\"more\"></span></span>",
                kr.getMatch(0).getSnippetHTML());

        assertEquals("SnippetBrackets (1)", "... [[c]] ...",
                kr.getMatch(1).getSnippetBrackets());
        assertEquals("SnippetHTML (1)",
                "<span class=\"context-left\"><span class=\"more\"></span></span><span class=\"match\"><mark>c</mark></span><span class=\"context-right\"><span class=\"more\"></span></span>",
                kr.getMatch(1).getSnippetHTML());

        assertEquals("SnippetBrackets (6)", "... [[c]]",
                kr.getMatch(6).getSnippetBrackets());
        assertEquals("SnippetBrackets (6)",
                "<span class=\"context-left\"><span class=\"more\"></span></span><span class=\"match\"><mark>c</mark></span><span class=\"context-right\"></span>",
                kr.getMatch(6).getSnippetHTML());
    };


    @Test
    public void indexExample3 () throws Exception {
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

        Result kr;

        QueryBuilder kq = new QueryBuilder("base");

        SpanQuery sq = kq.nr(1, kq.seq(kq.seg("s:b")).append(kq.seg("s:a"))
                .append(kq.nr(2, kq.seg("s:c")))).toQuery();

        kr = ki.search(sq, 0, (short) 20, true, (short) 2, true, (short) 5);

        assertEquals("totalResults", kr.getTotalResults(), 1);
        assertEquals("SnippetBrackets (0)", "... ca[[{1:ba{2:c}}]]",
                kr.getMatch(0).getSnippetBrackets());
    };


    @Test
    public void indexExampleExtend () throws IOException {
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

        sq = new SpanFocusQuery(new SpanNextQuery(
                new SpanClassQuery(new SpanTermQuery(new Term("base", "s:a")),
                        (byte) 2),
                new SpanClassQuery(new SpanTermQuery(new Term("base", "s:b")),
                        (byte) 3)),
                (byte) 3);

        kr = ki.search(sq, (short) 10);

        assertEquals("totalResults", kr.getTotalResults(), 3);

        Match km = kr.getMatch(0);
        assertEquals("StartPos (0)", 1, km.startPos);
        assertEquals("EndPos (0)", 2, km.endPos);
        assertEquals("SnippetBrackets (0)", "a[[{3:b}]]cabcab ...",
                km.getSnippetBrackets());

        sq = new SpanFocusQuery(
                new SpanFocusQuery(
                        new SpanNextQuery(
                                new SpanClassQuery(
                                        new SpanTermQuery(
                                                new Term("base", "s:a")),
                                        (byte) 2),
                                new SpanClassQuery(
                                        new SpanTermQuery(
                                                new Term("base", "s:b")),
                                        (byte) 3)),
                        (byte) 3),
                (byte) 2);

        kr = ki.search(sq, (short) 10);

        km = kr.getMatch(0);
        assertEquals("StartPos (0)", 0, km.startPos);
        assertEquals("EndPos (0)", 1, km.endPos);
        assertEquals("SnippetBrackets (0)", "[[{2:a}]]bcabca ...",
                km.getSnippetBrackets());

        // TODO: Check ID
    };


    @Test
    public void indexExampleFocusWithSpan () throws IOException {
        KrillIndex ki = new KrillIndex();

        // abcabcabac
        FieldDocument fd = new FieldDocument();
        fd.addTV("base", "abcabcabac", "[(0-1)s:a|i:a|_0$<i>0<i>1|-:t$<i>10]"
                + "[(1-2)s:b|i:b|_1$<i>1<i>2|<>:s$<b>64<i>1<i>5<i>5]"
                + "[(2-3)s:c|i:c|_2$<i>2<i>3|<>:s$<b>64<i>2<i>7<i>7]"
                + "[(3-4)s:a|i:a|_3$<i>3<i>4]" + "[(4-5)s:b|i:b|_4$<i>4<i>5]"
                + "[(5-6)s:c|i:c|_5$<i>5<i>6]" + "[(6-7)s:a|i:a|_6$<i>6<i>7]"
                + "[(7-8)s:b|i:b|_7$<i>7<i>8]" + "[(8-9)s:a|i:a|_8$<i>8<i>9]"
                + "[(9-10)s:c|i:c|_9$<i>9<i>10]");
        ki.addDoc(fd);
        ki.commit();

        SpanQuery sq;
        Result kr;

        //        sq = new SpanWithinQuery(new SpanClassQuery(new SpanElementQuery(
        //                "base", "s"), (byte) 2), new SpanClassQuery(new SpanTermQuery(
        //                new Term("base", "s:b")), (byte) 3));
        //
        //        kr = ki.search(sq, (short) 10);
        //        assertEquals(kr.getSerialQuery(),
        //                "spanContain({2: <base:s />}, {3: base:s:b})");
        //        assertEquals(kr.getMatch(0).getSnippetBrackets(),
        //                "a[{2:{3:b}cab}]cabac");

        sq = new SpanFocusQuery(new SpanWithinQuery(
                new SpanClassQuery(new SpanElementQuery("base", "s"), (byte) 2),
                new SpanClassQuery(new SpanTermQuery(new Term("base", "s:b")),
                        (byte) 3)),
                (byte) 3);

        kr = ki.search(sq, (short) 10);
        assertEquals(kr.getSerialQuery(),
                "focus(3: spanContain({2: <base:s />}, {3: base:s:b}))");

        assertEquals(kr.getMatch(0).getSnippetBrackets(),
                "a[[{3:b}]]cabcab ...");
    };


    @Ignore
    public void indexExampleFocusWithSkip () throws IOException {
        KrillIndex ki = new KrillIndex();

        // abcabcabac
        FieldDocument fd = new FieldDocument();
        fd.addTV("base", "abcabcabac",
                // The payload should be ignored
                "[(0-1)s:a|i:a|_0$<i>0<i>1|-:t$<i>10]" + // |<>:p#0-10<i>9]" +
                        "[(1-2)s:b|i:b|_1$<i>1<i>2|<>:s$<b>64<i>1<i>5<i>5]"
                        + "[(2-3)s:c|i:c|_2$<i>2<i>3|<>:s$<b>64<i>2<i>7<i>7]"
                        + "[(3-4)s:a|i:a|_3$<i>3<i>4]"
                        + "[(4-5)s:b|i:b|_4$<i>4<i>5]"
                        + "[(5-6)s:c|i:c|_5$<i>5<i>6]"
                        + "[(6-7)s:a|i:a|_6$<i>6<i>7]"
                        + "[(7-8)s:b|i:b|_7$<i>7<i>8]"
                        + "[(8-9)s:a|i:a|_8$<i>8<i>9]"
                        + "[(9-10)s:c|i:c|_9$<i>9<i>10]");
        ki.addDoc(fd);
        fd = new FieldDocument();
        fd.addTV("base", "gbcgbcgbgc",
                "[(0-1)s:g|i:g|_0$<i>0<i>1|-:t$<i>10|<>:p$<b>64<i>0<i>10<i>9]"
                        + "[(1-2)s:b|i:b|_1$<i>1<i>2|<>:s$<b>64<i>1<i>5<i>5]"
                        + "[(2-3)s:c|i:c|_2$<i>2<i>3|<>:s$<b>64<i>2<i>7<i>7]"
                        + "[(3-4)s:g|i:g|_3$<i>3<i>4]"
                        + "[(4-5)s:b|i:b|_4$<i>4<i>5]"
                        + "[(5-6)s:c|i:c|_5$<i>5<i>6]"
                        + "[(6-7)s:g|i:g|_6$<i>6<i>7]"
                        + "[(7-8)s:b|i:b|_7$<i>7<i>8]"
                        + "[(8-9)s:g|i:g|_8$<i>8<i>9]"
                        + "[(9-10)s:c|i:c|_9$<i>9<i>10]");
        ki.addDoc(fd);
        fd = new FieldDocument();
        fd.addTV("base", "gbcgbcgbgc", "[(0-1)s:g|i:g|_0$<i>0<i>1|-:t$<i>10]"
                + "[(1-2)s:b|i:b|_1$<i>1<i>2]" + "[(2-3)s:c|i:c|_2$<i>2<i>3]"
                + "[(3-4)s:g|i:g|_3$<i>3<i>4]" + "[(4-5)s:b|i:b|_4$<i>4<i>5]"
                + "[(5-6)s:c|i:c|_5$<i>5<i>6]" + "[(6-7)s:g|i:g|_6$<i>6<i>7]"
                + "[(7-8)s:b|i:b|_7$<i>7<i>8]" + "[(8-9)s:g|i:g|_8$<i>8<i>9]"
                + "[(9-10)s:c|i:c|_9$<i>9<i>10]");
        ki.addDoc(fd);
        fd = new FieldDocument();
        // contains(<p>, focus(3: contains({2:<s>}, {3:a})))
        fd.addTV("base", "acabcabac",
                "[(0-1)s:a|i:a|_0$<i>0<i>1|-:t$<i>10|<>:p$<b>64<i>0<i>9<i>8]"
                        + "[(1-2)s:b|i:b|_1$<i>1<i>2|<>:s$<b>64<i>1<i>5<i>5]"
                        + "[(2-3)s:a|i:a|_2$<i>2<i>3|<>:s$<b>64<i>2<i>7<i>7]"
                        + "[(3-4)s:b|i:b|_3$<i>3<i>4]"
                        + "[(4-5)s:c|i:c|_4$<i>4<i>5]"
                        + "[(5-6)s:a|i:a|_5$<i>5<i>6]"
                        + "[(6-7)s:b|i:b|_6$<i>6<i>7]"
                        + "[(7-8)s:a|i:a|_7$<i>7<i>8]"
                        + "[(8-9)s:c|i:c|_8$<i>8<i>9]");
        ki.addDoc(fd);
        ki.commit();

        SpanQuery sq;
        Result kr;
        KrillCollection kc = new KrillCollection(ki);

        assertEquals("Documents", 4, kc.numberOf("documents"));

        // within(<p>, focus(3:within({2:<s>}, {3:a})))
        sq = new SpanWithinQuery(new SpanElementQuery("base", "p"),
                new SpanFocusQuery(new SpanWithinQuery(
                        new SpanClassQuery(new SpanElementQuery("base", "s"),
                                (byte) 2),
                        new SpanClassQuery(
                                new SpanTermQuery(new Term("base", "s:a")),
                                (byte) 3)),
                        (byte) 3));

        // fail("Skipping may go horribly wrong! (Known issue)");

        Krill ks = new Krill(sq);
        ks.getMeta().setStartIndex(0).setCount((short) 20)
                .setContext(new SearchContext(true, (short) 5, true, (short) 5))
        // .setCollection(kc)
        ;

        kr = ks.apply(ki);
        // kr = ki.search(kc, sq, 0, (short) 20, true, (short) 5, true, (short) 5);

        assertEquals(kr.getSerialQuery(),
                "spanContain(<base:p />, focus(3: spanContain({2: <base:s />}, {3: base:s:a})))");
        assertEquals(12, kr.getTotalResults());
        assertEquals("[a{2:bc{3:a}b}cabac]",
                kr.getMatch(0).getSnippetBrackets());
        assertEquals("[ab{2:c{3:a}bcab}ac]",
                kr.getMatch(1).getSnippetBrackets());
        assertEquals("[ab{2:cabc{3:a}}bac]",
                kr.getMatch(2).getSnippetBrackets());
    };

};
