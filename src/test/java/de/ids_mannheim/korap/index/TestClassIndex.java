package de.ids_mannheim.korap.index;

import java.util.*;
import java.io.*;

import org.apache.lucene.util.Version;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Bits;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import de.ids_mannheim.korap.KorapIndex;
import de.ids_mannheim.korap.KorapQuery;
import de.ids_mannheim.korap.KorapResult;
import de.ids_mannheim.korap.query.SpanNextQuery;
import de.ids_mannheim.korap.query.SpanClassQuery;
import de.ids_mannheim.korap.query.SpanElementQuery;
import de.ids_mannheim.korap.query.SpanWithinQuery;
import de.ids_mannheim.korap.index.FieldDocument;
import de.ids_mannheim.korap.analysis.MultiTermTokenStream;

import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.index.Term;

// mvn -Dtest=TestWithinIndex#indexExample1 test

@RunWith(JUnit4.class)
public class TestClassIndex {

    @Test
    public void indexExample1 () throws IOException {
	KorapIndex ki = new KorapIndex();

	// abcabcabac
	FieldDocument fd = new FieldDocument();
	fd.addTV("base",
		 "abcabcabac",
		 "[(0-1)s:a|i:a|_0#0-1|-:t$<i>10]" +
		 "[(1-2)s:b|i:b|_1#1-2]" +
		 "[(2-3)s:c|i:c|_2#2-3]" +
		 "[(3-4)s:a|i:a|_3#3-4]" +
		 "[(4-5)s:b|i:b|_4#4-5]" +
		 "[(5-6)s:c|i:c|_5#5-6]" +
		 "[(6-7)s:a|i:a|_6#6-7]" +
		 "[(7-8)s:b|i:b|_7#7-8]" +
		 "[(8-9)s:a|i:a|_8#8-9]" +
		 "[(9-10)s:c|i:c|_9#9-10]");
	ki.addDoc(fd);

	ki.commit();

	SpanQuery sq;
	KorapResult kr;

	sq = new SpanNextQuery(
            new SpanTermQuery(new Term("base", "s:b")),
            new SpanTermQuery(new Term("base", "s:a"))
        );
	kr = ki.search(sq, (short) 10);

	assertEquals("totalResults", kr.getTotalResults(), 1);
	assertEquals("StartPos (0)", 7, kr.getMatch(0).startPos);
	assertEquals("EndPos (0)", 9, kr.getMatch(0).endPos);
	assertEquals("SnippetBrackets (0)", "... bcabca[ba]c", kr.getMatch(0).snippetBrackets());
	assertEquals("SnippetHTML (0)", "<span class=\"context-left\"><span class=\"more\">" +
			"</span>bcabca</span><span class=\"match\">ba</span><span class=\"context-right" +
			"\">c</span>", kr.getMatch(0).snippetHTML());

	sq = new SpanTermQuery(new Term("base", "s:b"));
	kr = ki.search(sq, (short) 10);

	assertEquals("totalResults", kr.getTotalResults(), 3);
	assertEquals("StartPos (0)", 1, kr.getMatch(0).startPos);
	assertEquals("EndPos (0)", 2, kr.getMatch(0).endPos);
	assertEquals("SnippetBrackets (0)", "a[b]cabcab ...", kr.getMatch(0).snippetBrackets());


	assertEquals("SnippetHTML (0)", "<span class=\"context-left\">a</span><span class=\"match\">" +
			"b</span><span class=\"context-right\">cabcab<span class=\"more\"></span></span>", 
			kr.getMatch(0).snippetHTML());

	assertEquals("StartPos (1)", 4, kr.getMatch(1).startPos);
	assertEquals("EndPos (1)", 5, kr.getMatch(1).endPos);
	assertEquals("SnippetBrackets (1)", "abca[b]cabac", kr.getMatch(1).snippetBrackets());
	assertEquals("StartPos (2)", 7, kr.getMatch(2).startPos);
	assertEquals("EndPos (2)", 8, kr.getMatch(2).endPos);
	assertEquals("SnippetBrackets (2)", "... bcabca[b]ac", kr.getMatch(2).snippetBrackets());

	sq = new SpanClassQuery(new SpanTermQuery(new Term("base", "s:b")));
	kr = ki.search(sq, (short) 10);

	assertEquals("totalResults", kr.getTotalResults(), 3);
	assertEquals("StartPos (0)", 1, kr.getMatch(0).startPos);
	assertEquals("EndPos (0)", 2, kr.getMatch(0).endPos);
	assertEquals("snippetBrackets (0)", "a[{1:b}]cabcab ...", kr.getMatch(0).snippetBrackets());
	assertEquals("snippetHTML (0)", "<span class=\"context-left\">a</span><span class=\"match\">" +
			"<em class=\"class-1 level-0\">b</em></span><span class=\"context-right\">cabcab<span " +
			"class=\"more\"></span></span>", kr.getMatch(0).snippetHTML());

	assertEquals("StartPos (1)", 4, kr.getMatch(1).startPos);
	assertEquals("EndPos (1)", 5, kr.getMatch(1).endPos);
	assertEquals("snippetBrackets (1)", "abca[{1:b}]cabac", kr.getMatch(1).snippetBrackets());
	
	assertEquals("StartPos (2)", 7, kr.getMatch(2).startPos);
	assertEquals("EndPos (2)", 8, kr.getMatch(2).endPos);
	assertEquals("snippetBrackets (2)", "... bcabca[{1:b}]ac", kr.getMatch(2).snippetBrackets());


	sq = new SpanNextQuery(
            new SpanTermQuery(new Term("base", "s:a")),
            new SpanClassQuery(new SpanTermQuery(new Term("base", "s:b")), (byte) 1)
        );

	kr = ki.search(sq, (short) 10);

	assertEquals("totalResults", kr.getTotalResults(), 3);
	assertEquals("StartPos (0)", 0, kr.getMatch(0).startPos);
	assertEquals("EndPos (0)", 2, kr.getMatch(0).endPos);
	assertEquals("SnippetBrackets (0)", "[a{1:b}]cabcab ...", kr.getMatch(0).snippetBrackets());

	assertEquals("SnippetHTML (0)", "<span class=\"context-left\"></span><span class=\"match\">a<em class=\"class-1 level-0\">b</em></span><span class=\"context-right\">cabcab<span class=\"more\"></span></span>", kr.getMatch(0).snippetHTML());

	assertEquals("StartPos (1)", 3, kr.getMatch(1).startPos);
	assertEquals("EndPos (1)", 5, kr.getMatch(1).endPos);
	assertEquals("SnippetBrackets (1)", "abc[a{1:b}]cabac", kr.getMatch(1).snippetBrackets());
	assertEquals("StartPos (2)", 6, kr.getMatch(2).startPos);
	assertEquals("EndPos (2)", 8, kr.getMatch(2).endPos);
	assertEquals("SnippetBrackets (2)", "abcabc[a{1:b}]ac", kr.getMatch(2).snippetBrackets());


	// abcabcabac
	sq = new SpanNextQuery(
	    new SpanClassQuery(new SpanTermQuery(new Term("base", "s:a")), (byte) 2),
            new SpanClassQuery(new SpanTermQuery(new Term("base", "s:b")), (byte) 3)
        );

	kr = ki.search(sq, (short) 10);

	assertEquals("StartPos (0)", 0, kr.getMatch(0).startPos);
	assertEquals("EndPos (0)", 2, kr.getMatch(0).endPos);
	assertEquals("SnippetBrackets (0)", "[{2:a}{3:b}]cabcab ...", kr.getMatch(0).snippetBrackets());
	assertEquals("StartPos (1)", 3, kr.getMatch(1).startPos);
	assertEquals("EndPos (1)", 5, kr.getMatch(1).endPos);
	assertEquals("SnippetBrackets (1)", "abc[{2:a}{3:b}]cabac", kr.getMatch(1).snippetBrackets());

	assertEquals("StartPos (2)", 6, kr.getMatch(2).startPos);
	assertEquals("EndPos (2)", 8, kr.getMatch(2).endPos);
	assertEquals("SnippetBrackets (2)", "abcabc[{2:a}{3:b}]ac", kr.getMatch(2).snippetBrackets());

	// abcabcabac
	sq = new SpanNextQuery(
            new SpanTermQuery(new Term("base", "s:a")),
            new SpanClassQuery(
	        new SpanNextQuery(
		    new SpanTermQuery(new Term("base", "s:b")),
	    	    new SpanClassQuery(new SpanTermQuery(new Term("base", "s:a")))
		), (byte) 2
	));

	kr = ki.search(sq, (short) 10);

	assertEquals("totalResults", kr.getTotalResults(), 1);
	assertEquals("SnippetBrackets (0)", "abcabc[a{2:b{1:a}}]c", kr.getMatch(0).snippetBrackets());
	assertEquals("SnippetHTML (0)", "<span class=\"context-left\">abcabc</span><span class=\"match\">a<em class=\"class-2 level-0\">b<em class=\"class-1 level-1\">a</em></em></span><span class=\"context-right\">c</span>", kr.getMatch(0).snippetHTML());

	// Offset tokens
	kr = ki.search(sq, 0, (short) 10, true, (short) 2, true, (short) 2);
	assertEquals("totalResults", kr.getTotalResults(), 1);
	assertEquals("SnippetBrackets (0)", "... bc[a{2:b{1:a}}]c", kr.getMatch(0).snippetBrackets());
	assertEquals("SnippetHTML (0)", "<span class=\"context-left\"><span class=\"more\"></span>bc</span><span class=\"match\">a<em class=\"class-2 level-0\">b<em class=\"class-1 level-1\">a</em></em></span><span class=\"context-right\">c</span>", kr.getMatch(0).snippetHTML());

	// Offset Characters
	kr = ki.search(sq, 0, (short) 10, false, (short) 2, false, (short) 2);
	assertEquals("totalResults", kr.getTotalResults(), 1);
	assertEquals("SnippetBrackets (0)", "... bc[a{2:b{1:a}}]c", kr.getMatch(0).snippetBrackets());
	assertEquals("SnippetHTML (0)", "<span class=\"context-left\"><span class=\"more\"></span>bc</span><span class=\"match\">a<em class=\"class-2 level-0\">b<em class=\"class-1 level-1\">a</em></em></span><span class=\"context-right\">c</span>", kr.getMatch(0).snippetHTML());


	// System.err.println(kr.toJSON());

	sq = new SpanNextQuery(
	    new SpanClassQuery(new SpanTermQuery(new Term("base", "s:b")), (byte) 1),
            new SpanClassQuery(new SpanTermQuery(new Term("base", "s:c")), (byte) 2)
        );

	kr = ki.search(sq, (short) 10);
	
	assertEquals("totalResults", kr.getTotalResults(), 2);
	assertEquals("StartPos (0)", 1, kr.getMatch(0).startPos);
	assertEquals("EndPos (0)", 3, kr.getMatch(0).endPos);
	assertEquals("StartPos (1)", 4, kr.getMatch(1).startPos);
	assertEquals("EndPos (1)", 6, kr.getMatch(1).endPos);

	assertEquals("Document count", 1, ki.numberOf("base", "documents"));
	assertEquals("Token count", 10, ki.numberOf("base", "t"));


	sq = new SpanNextQuery(
            new SpanTermQuery(new Term("base", "s:a")),
            new SpanClassQuery(
	      new SpanNextQuery(
	        new SpanTermQuery(new Term("base", "s:b")),
	        new SpanTermQuery(new Term("base", "s:c"))
	      )
	   )
        );

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
	KorapIndex ki = new KorapIndex();

	// abcabcabac
	FieldDocument fd = new FieldDocument();
	fd.addTV("base",
		 "abcabcabac",
		 "[(0-1)s:a|i:a|_0#0-1|-:t$<i>10]" +
		 "[(1-2)s:b|i:b|_1#1-2]" +
		 "[(2-3)s:c|i:c|_2#2-3]" +
		 "[(3-4)s:a|i:a|_3#3-4|<>:x#3-7$<i>7]" +
		 "[(4-5)s:b|i:b|_4#4-5]" +
		 "[(5-6)s:c|i:c|_5#5-6]" +
		 "[(6-7)s:a|i:a|_6#6-7]" +
		 "[(7-8)s:b|i:b|_7#7-8]" +
		 "[(8-9)s:a|i:a|_8#8-9]" +
		 "[(9-10)s:c|i:c|_9#9-10]");
	ki.addDoc(fd);

	ki.commit();

	SpanQuery sq;
	KorapResult kr;

	/*
	sq = new SpanNextQuery(
	       new SpanTermQuery(new Term("base", "s:c")),
 	       new SpanElementQuery("base", "x")
	);
	
	kr = ki.search(sq, (short) 10);
	assertEquals("ab[cabca]bac", kr.getMatch(0).getSnippetBrackets());
	System.err.println();
	*/

	/*
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
