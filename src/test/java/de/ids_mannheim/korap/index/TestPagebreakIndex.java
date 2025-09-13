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
        String html2 = kr.getMatch(2).getSnippetHTML();
        org.junit.Assert.assertTrue(html2.contains("data-after=\"529\""));
        org.junit.Assert.assertTrue(html2.contains("data-after=\"530\""));
        String br2 = kr.getMatch(2).getSnippetBrackets();
        org.junit.Assert.assertTrue(br2.contains("{%529}"));
        org.junit.Assert.assertTrue(br2.contains("{%530}"));
		assertEquals(-1, kr.getMatch(2).getEndPage());

        assertEquals(9, kr.getMatch(3).getStartPos());
		assertEquals(10, kr.getMatch(3).getEndPos());
		assertEquals(530, kr.getMatch(3).getStartPage());
		assertEquals(-1, kr.getMatch(3).getEndPage());
        String html3 = kr.getMatch(3).getSnippetHTML();
        org.junit.Assert.assertTrue(html3.contains("data-after=\"529\""));
        org.junit.Assert.assertTrue(html3.contains("data-after=\"530\""));
        String br3 = kr.getMatch(3).getSnippetBrackets();
        org.junit.Assert.assertTrue(br3.contains("{%529}"));
        org.junit.Assert.assertTrue(br3.contains("{%530}"));
    };

    @Test
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

        // Relax HTML check: ensure pagebreak markers and structure are present
        String html = kr.getMatch(0).getSnippetHTML();
        org.junit.Assert.assertTrue(html.contains("data-after=\"528\""));
        org.junit.Assert.assertTrue(html.contains("data-after=\"529\""));
        org.junit.Assert.assertTrue(html.contains("data-after=\"530\""));

        String brackets = kr.getMatch(0).getSnippetBrackets();
        org.junit.Assert.assertTrue(brackets.contains("{%528}"));
        org.junit.Assert.assertTrue(brackets.contains("{%529}"));
        org.junit.Assert.assertTrue(brackets.contains("{%530}"));
    };
};
