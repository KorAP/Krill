package de.ids_mannheim.korap.index;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.query.SpanClassQuery;
import de.ids_mannheim.korap.query.SpanNextQuery;
import de.ids_mannheim.korap.response.Result;

// mvn -Dtest=TestWithinIndex#indexExample1 test

@RunWith(JUnit4.class)
public class TestClassIndex {

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

        sq = new SpanNextQuery(new SpanTermQuery(new Term("base", "s:b")),
                new SpanTermQuery(new Term("base", "s:a")));
        kr = ki.search(sq, (short) 10);

        assertEquals("totalResults", kr.getTotalResults(), 1);
        assertEquals("StartPos (0)", 7, kr.getMatch(0).startPos);
        assertEquals("EndPos (0)", 9, kr.getMatch(0).endPos);
        assertEquals("SnippetBrackets (0)", "... bcabca[[ba]]c", kr.getMatch(0)
                .getSnippetBrackets());
        assertEquals(
                "SnippetHTML (0)",
                "<span class=\"context-left\"><span class=\"more\">"
                        + "</span>bcabca</span><span class=\"match\"><mark>ba</mark></span><span class=\"context-right"
                        + "\">c</span>", kr.getMatch(0).getSnippetHTML());

        sq = new SpanTermQuery(new Term("base", "s:b"));
        kr = ki.search(sq, (short) 10);

        assertEquals("totalResults", kr.getTotalResults(), 3);
        assertEquals("StartPos (0)", 1, kr.getMatch(0).startPos);
        assertEquals("EndPos (0)", 2, kr.getMatch(0).endPos);
        assertEquals("SnippetBrackets (0)", "a[[b]]cabcab ...", kr.getMatch(0)
                .getSnippetBrackets());


        assertEquals(
                "SnippetHTML (0)",
                "<span class=\"context-left\">a</span><span class=\"match\"><mark>"
                        + "b</mark></span><span class=\"context-right\">cabcab<span class=\"more\"></span></span>",
                kr.getMatch(0).getSnippetHTML());

        assertEquals("StartPos (1)", 4, kr.getMatch(1).startPos);
        assertEquals("EndPos (1)", 5, kr.getMatch(1).endPos);
        assertEquals("SnippetBrackets (1)", "abca[[b]]cabac", kr.getMatch(1)
                .getSnippetBrackets());
        assertEquals("StartPos (2)", 7, kr.getMatch(2).startPos);
        assertEquals("EndPos (2)", 8, kr.getMatch(2).endPos);
        assertEquals("SnippetBrackets (2)", "... bcabca[[b]]ac", kr.getMatch(2)
                .getSnippetBrackets());

        sq = new SpanClassQuery(new SpanTermQuery(new Term("base", "s:b")));
        kr = ki.search(sq, (short) 10);

        assertEquals("totalResults", kr.getTotalResults(), 3);
        assertEquals("StartPos (0)", 1, kr.getMatch(0).startPos);
        assertEquals("EndPos (0)", 2, kr.getMatch(0).endPos);
        assertEquals("snippetBrackets (0)", "a[[{1:b}]]cabcab ...", kr
                .getMatch(0).getSnippetBrackets());
        assertEquals(
                "snippetHTML (0)",
                "<span class=\"context-left\">a</span><span class=\"match\"><mark>"
                        + "<mark class=\"class-1 level-0\">b</mark></mark></span><span class=\"context-right\">cabcab<span "
                        + "class=\"more\"></span></span>", kr.getMatch(0)
                        .getSnippetHTML());

        assertEquals("StartPos (1)", 4, kr.getMatch(1).startPos);
        assertEquals("EndPos (1)", 5, kr.getMatch(1).endPos);
        assertEquals("snippetBrackets (1)", "abca[[{1:b}]]cabac", kr
                .getMatch(1).getSnippetBrackets());

        assertEquals("StartPos (2)", 7, kr.getMatch(2).startPos);
        assertEquals("EndPos (2)", 8, kr.getMatch(2).endPos);
        assertEquals("snippetBrackets (2)", "... bcabca[[{1:b}]]ac", kr
                .getMatch(2).getSnippetBrackets());


        sq = new SpanNextQuery(new SpanTermQuery(new Term("base", "s:a")),
                new SpanClassQuery(new SpanTermQuery(new Term("base", "s:b")),
                        (byte) 1));

        kr = ki.search(sq, (short) 10);

        assertEquals("totalResults", kr.getTotalResults(), 3);
        assertEquals("StartPos (0)", 0, kr.getMatch(0).startPos);
        assertEquals("EndPos (0)", 2, kr.getMatch(0).endPos);
        assertEquals("SnippetBrackets (0)", "[[a{1:b}]]cabcab ...", kr
                .getMatch(0).getSnippetBrackets());

        assertEquals(
                "SnippetHTML (0)",
                "<span class=\"context-left\"></span><span class=\"match\"><mark>a<mark class=\"class-1 level-0\">b</mark></mark></span><span class=\"context-right\">cabcab<span class=\"more\"></span></span>",
                kr.getMatch(0).getSnippetHTML());

        assertEquals("StartPos (1)", 3, kr.getMatch(1).startPos);
        assertEquals("EndPos (1)", 5, kr.getMatch(1).endPos);
        assertEquals("SnippetBrackets (1)", "abc[[a{1:b}]]cabac", kr
                .getMatch(1).getSnippetBrackets());
        assertEquals("StartPos (2)", 6, kr.getMatch(2).startPos);
        assertEquals("EndPos (2)", 8, kr.getMatch(2).endPos);
        assertEquals("SnippetBrackets (2)", "abcabc[[a{1:b}]]ac", kr
                .getMatch(2).getSnippetBrackets());


        // abcabcabac
        sq = new SpanNextQuery(new SpanClassQuery(new SpanTermQuery(new Term(
                "base", "s:a")), (byte) 2), new SpanClassQuery(
                new SpanTermQuery(new Term("base", "s:b")), (byte) 3));

        kr = ki.search(sq, (short) 10);

        assertEquals("StartPos (0)", 0, kr.getMatch(0).startPos);
        assertEquals("EndPos (0)", 2, kr.getMatch(0).endPos);
        assertEquals("SnippetBrackets (0)", "[[{2:a}{3:b}]]cabcab ...", kr
                .getMatch(0).getSnippetBrackets());
        assertEquals("StartPos (1)", 3, kr.getMatch(1).startPos);
        assertEquals("EndPos (1)", 5, kr.getMatch(1).endPos);
        assertEquals("SnippetBrackets (1)", "abc[[{2:a}{3:b}]]cabac", kr
                .getMatch(1).getSnippetBrackets());

        assertEquals("StartPos (2)", 6, kr.getMatch(2).startPos);
        assertEquals("EndPos (2)", 8, kr.getMatch(2).endPos);
        assertEquals("SnippetBrackets (2)", "abcabc[[{2:a}{3:b}]]ac", kr
                .getMatch(2).getSnippetBrackets());

        // abcabcabac
        sq = new SpanNextQuery(new SpanTermQuery(new Term("base", "s:a")),
                new SpanClassQuery(new SpanNextQuery(new SpanTermQuery(
                        new Term("base", "s:b")), new SpanClassQuery(
                        new SpanTermQuery(new Term("base", "s:a")))), (byte) 2));

        kr = ki.search(sq, (short) 10);

        assertEquals("totalResults", kr.getTotalResults(), 1);
        assertEquals("SnippetBrackets (0)", "abcabc[[a{2:b{1:a}}]]c", kr
                .getMatch(0).getSnippetBrackets());
        assertEquals(
                "SnippetHTML (0)",
                "<span class=\"context-left\">abcabc</span><span class=\"match\"><mark>a<mark class=\"class-2 level-0\">b<mark class=\"class-1 level-1\">a</mark></mark></mark></span><span class=\"context-right\">c</span>",
                kr.getMatch(0).getSnippetHTML());

        // Offset tokens
        kr = ki.search(sq, 0, (short) 10, true, (short) 2, true, (short) 2);
        assertEquals("totalResults", kr.getTotalResults(), 1);
        assertEquals("SnippetBrackets (0)", "... bc[[a{2:b{1:a}}]]c", kr
                .getMatch(0).getSnippetBrackets());
        assertEquals(
                "SnippetHTML (0)",
                "<span class=\"context-left\"><span class=\"more\"></span>bc</span><span class=\"match\"><mark>a<mark class=\"class-2 level-0\">b<mark class=\"class-1 level-1\">a</mark></mark></mark></span><span class=\"context-right\">c</span>",
                kr.getMatch(0).getSnippetHTML());

        // Offset Characters
        kr = ki.search(sq, 0, (short) 10, false, (short) 2, false, (short) 2);
        assertEquals("totalResults", kr.getTotalResults(), 1);
        assertEquals("SnippetBrackets (0)", "... bc[[a{2:b{1:a}}]]c", kr
                .getMatch(0).getSnippetBrackets());
        assertEquals(
                "SnippetHTML (0)",
                "<span class=\"context-left\"><span class=\"more\"></span>bc</span><span class=\"match\"><mark>a<mark class=\"class-2 level-0\">b<mark class=\"class-1 level-1\">a</mark></mark></mark></span><span class=\"context-right\">c</span>",
                kr.getMatch(0).getSnippetHTML());


        // System.err.println(kr.toJSON());

        sq = new SpanNextQuery(new SpanClassQuery(new SpanTermQuery(new Term(
                "base", "s:b")), (byte) 1), new SpanClassQuery(
                new SpanTermQuery(new Term("base", "s:c")), (byte) 2));

        kr = ki.search(sq, (short) 10);

        assertEquals("totalResults", kr.getTotalResults(), 2);
        assertEquals("StartPos (0)", 1, kr.getMatch(0).startPos);
        assertEquals("EndPos (0)", 3, kr.getMatch(0).endPos);
        assertEquals("StartPos (1)", 4, kr.getMatch(1).startPos);
        assertEquals("EndPos (1)", 6, kr.getMatch(1).endPos);

        assertEquals("Document count", 1, ki.numberOf("base", "documents"));
        assertEquals("Token count", 10, ki.numberOf("base", "t"));


        sq = new SpanNextQuery(new SpanTermQuery(new Term("base", "s:a")),
                new SpanClassQuery(new SpanNextQuery(new SpanTermQuery(
                        new Term("base", "s:b")), new SpanTermQuery(new Term(
                        "base", "s:c")))));

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
                + "[(3-4)s:a|i:a|_3$<i>3<i>4|<>:x$<b>64<i>3<i>7<i>7]"
                + "[(4-5)s:b|i:b|_4$<i>4<i>5]" + "[(5-6)s:c|i:c|_5$<i>5<i>6]"
                + "[(6-7)s:a|i:a|_6$<i>6<i>7]" + "[(7-8)s:b|i:b|_7$<i>7<i>8]"
                + "[(8-9)s:a|i:a|_8$<i>8<i>9]" + "[(9-10)s:c|i:c|_9$<i>9<i>10]");
        ki.addDoc(fd);

        ki.commit();

        SpanQuery sq;
        Result kr;
        /*
                sq = new SpanNextQuery(
                       new SpanTermQuery(new Term("base", "s:c")),
                       new SpanElementQuery("base", "x")
                );
                
                kr = ki.search(sq, (short) 10);
                assertEquals("ab[cabca]bac", kr.getMatch(0).getSnippetBrackets());
        */
        /*
        System.err.println();
        sq = new SpanNextQuery(
               new SpanElementQuery("base", "x"),
               new SpanTermQuery(new Term("base", "s:b"))
        );
        
        kr = ki.search(sq, (short) 10);
        assertEquals("abc[abcab}ac]", kr.getMatch(0).getSnippetBrackets());
        System.err.println();
        
        */

        /*
        sq = new SpanWithinQuery(
                new SpanElementQuery("base", "x"),
                new SpanClassQuery(
                  new SpanTermQuery(new Term("base", "s:a"))
                )
            );
        
               //	       new SpanTermQuery(new Term("base", "s:a")),
            //            new SpanClassQuery(
            //            )
            //        );
        
        */

    }
};
