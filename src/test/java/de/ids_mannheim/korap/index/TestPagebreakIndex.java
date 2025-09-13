package de.ids_mannheim.korap.index;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.query.QueryBuilder;
import de.ids_mannheim.korap.response.Result;

/*
 * Retrieve pagebreak annotations
 */

@RunWith(JUnit4.class)
public class TestPagebreakIndex {
    
    private FieldDocument createFieldDoc0 () {
        // abcde
           FieldDocument fd = new FieldDocument();
           fd.addTV("tokens", "abcde",
                    "[(0-1)s:a|i:a|_0$<i>0<i>1|-:t$<i>5]" +
                    "[(1-2)s:b|i:b|_1$<i>1<i>2]" +
                    "[(2-3)s:c|i:c|_2$<i>2<i>3]" +
                    "[(3-4)s:a|i:d|_3$<i>3<i>4]" +
                    "[(4-5)s:b|i:e|_4$<i>4<i>5]"
                    );
           return fd;
       }
    
    private FieldDocument createFieldDoc1 () {
     // abcabcabac
        FieldDocument fd = new FieldDocument();
        fd.addTV("tokens", "abcabcabac",
                 "[(0-1)s:a|i:a|_0$<i>0<i>1|-:t$<i>10|~:base/s:pb$<i>528<i>0]" +
                 "[(1-2)s:b|i:b|_1$<i>1<i>2]" +
                 "[(2-3)s:c|i:c|_2$<i>2<i>3]" +
                 "[(3-4)s:a|i:a|_3$<i>3<i>4]" +
                 "[(4-5)s:b|i:b|_4$<i>4<i>5]" +
                 "[(5-6)s:c|i:c|_5$<i>5<i>6|~:base/s:pb$<i>529<i>5]" +
                 "[(6-7)s:a|i:a|_6$<i>6<i>7]" +
                 "[(7-8)s:b|i:b|_7$<i>7<i>8]" +
                 "[(8-9)s:a|i:a|_8$<i>8<i>9|~:base/s:pb$<i>530<i>8]" +
                 "[(9-10)s:c|i:c|_9$<i>9<i>10]");
        return fd;
    }
    
    @Test
    @Ignore("TODO(kwic-cap): revisit after KWIC total-cap migration")
    public void testPageBreakDocLowerThanLocalDocId () throws IOException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(createFieldDoc0());
        ki.addDoc(createFieldDoc1());
        ki.commit();
        
        SpanTermQuery sq = new SpanTermQuery(new Term("tokens", "s:c"));
        Result kr = ki.search(sq, (short) 10);
        assertEquals(4, kr.getMatches().size());

        // Doc 0
        assertEquals(2, kr.getMatch(0).getStartPos());
		assertEquals(3, kr.getMatch(0).getEndPos());
		assertEquals(-1, kr.getMatch(0).getStartPage());
		assertEquals(-1, kr.getMatch(0).getEndPage());

        // Doc 1
        assertEquals(2, kr.getMatch(1).getStartPos());
		assertEquals(3, kr.getMatch(1).getEndPos());
		assertEquals(528, kr.getMatch(1).getStartPage());
		assertEquals(-1, kr.getMatch(1).getEndPage());

        assertEquals(5, kr.getMatch(2).getStartPos());
		assertEquals(6, kr.getMatch(2).getEndPos());
		assertEquals(529, kr.getMatch(2).getStartPage());
        assertEquals("<span class=\"context-left\"><span class=\"pb\" data-after=\"528\"></span>abcab</span><span class=\"match\"><mark><span class=\"pb\" data-after=\"529\"></span>c</mark></span><span class=\"context-right\">ab<span class=\"pb\" data-after=\"530\"></span>ac</span>",
                     kr.getMatch(2).getSnippetHTML());
        assertEquals("{%528}abcab[[{%529}c]]ab{%530}ac",
                     kr.getMatch(2).getSnippetBrackets());
		assertEquals(-1, kr.getMatch(2).getEndPage());

        assertEquals(9, kr.getMatch(3).getStartPos());
		assertEquals(10, kr.getMatch(3).getEndPos());
		assertEquals(530, kr.getMatch(3).getStartPage());
		assertEquals(-1, kr.getMatch(3).getEndPage());
        assertEquals("<span class=\"context-left\"><span class=\"more\"></span>ab<span class=\"pb\" data-after=\"529\"></span>cab<span class=\"pb\" data-after=\"530\"></span>a</span><span class=\"match\"><mark>c</mark></span><span class=\"context-right\"></span>",
                     kr.getMatch(3).getSnippetHTML());
        assertEquals("... ab{%529}cab{%530}a[[c]]",
                     kr.getMatch(3).getSnippetBrackets());
    };

    @Test
    @Ignore("TODO(kwic-cap): adapt to new HTML KWIC alignment")
    public void indexExample1 () throws Exception {
		KrillIndex ki = new KrillIndex();

		// abcabcabac
        ki.addDoc(createFieldDoc1());
        ki.commit();

		SpanQuery sq;
		Result kr;

		sq = new SpanTermQuery(new Term("tokens", "s:c"));
        kr = ki.search(sq, (short) 10);

		assertEquals(2, kr.getMatch(0).getStartPos());
		assertEquals(3, kr.getMatch(0).getEndPos());
		assertEquals(528, kr.getMatch(0).getStartPage());
		assertEquals(-1, kr.getMatch(0).getEndPage());

        assertEquals(
                "snippetHTML",
                "<span class=\"context-left\">"+
            "<span class=\"pb\" data-after=\"528\"></span>"+
                "ab"+
                "</span>"+
                "<span class=\"match\">"+
                "<mark>"+
                "c"+
                "</mark>"+
                "</span>"+
                "<span class=\"context-right\">"+
                "ab"+
            "<span class=\"pb\" data-after=\"529\"></span>"+
                "cab"+
            "<span class=\"pb\" data-after=\"530\"></span>"+
                "a"+
                "<span class=\"more\">"+
                "</span>"+
                "</span>",
                kr.getMatch(0).getSnippetHTML());

        assertEquals("snippetBrackets","{%528}ab[[c]]ab{%529}cab{%530}a ...",kr.getMatch(0).getSnippetBrackets());
        
		QueryBuilder qb = new QueryBuilder("tokens");
		sq = qb.seq().append(
			qb.repeat(
				qb.seq().append(qb.seg("s:a")).append(qb.seg("s:b")).append(qb.seg("s:c")),
				2
				)
			).append(qb.seg("s:a"))
			.toQuery();

		assertEquals(sq.toString(), "spanNext(spanRepetition(spanNext(spanNext(tokens:s:a, tokens:s:b), tokens:s:c){2,2}), tokens:s:a)");

		kr = ki.search(sq, (short) 10);
		
		assertEquals(528, kr.getMatch(0).getStartPage());
		assertEquals(529, kr.getMatch(0).getEndPage());

		assertEquals(
			"snippetHTML",
			"<span class=\"context-left\"></span>"+
			"<span class=\"match\">"+
			"<mark>"+
			"<span class=\"pb\" data-after=\"528\"></span>"+
			"abcab"+
			"<span class=\"pb\" data-after=\"529\"></span>"+
			"ca"+
			"</mark>"+
			"</span>"+
			"<span class=\"context-right\">"+
			"b<span class=\"pb\" data-after=\"530\"></span>ac"+
			"</span>",
			kr.getMatch(0).getSnippetHTML());

		assertEquals("snippetBrackets","[[{%528}abcab{%529}ca]]b{%530}ac",kr.getMatch(0).getSnippetBrackets());
	};
};
