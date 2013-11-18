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

	assertEquals("totalResults", 1, kr.totalResults());
	assertEquals("StartPos (0)", 7, kr.match(0).startPos);
	assertEquals("EndPos (0)", 9, kr.match(0).endPos);
	assertEquals("SnippetBrackets (0)", "... bcabca[ba]c", kr.match(0).snippetBrackets());
	assertEquals("SnippetHTML (0)", "<span class=\"korap-more-left\"></span>bcabca<span class=\"korap-match\">ba</span>c", kr.match(0).snippetHTML());

	sq = new SpanTermQuery(new Term("base", "s:b"));
	kr = ki.search(sq, (short) 10);

	assertEquals("totalResults", 3, kr.totalResults());
	assertEquals("StartPos (0)", 1, kr.match(0).startPos);
	assertEquals("EndPos (0)", 2, kr.match(0).endPos);
	assertEquals("SnippetBrackets (0)", "a[b]cabcab ...", kr.match(0).snippetBrackets());


	assertEquals("SnippetHTML (0)", "a<span class=\"korap-match\">b</span>cabcab<span class=\"korap-more-right\"></span>", kr.match(0).snippetHTML());

	assertEquals("StartPos (1)", 4, kr.match(1).startPos);
	assertEquals("EndPos (1)", 5, kr.match(1).endPos);
	assertEquals("SnippetBrackets (1)", "abca[b]cabac", kr.match(1).snippetBrackets());
	assertEquals("StartPos (2)", 7, kr.match(2).startPos);
	assertEquals("EndPos (2)", 8, kr.match(2).endPos);
	assertEquals("SnippetBrackets (2)", "... bcabca[b]ac", kr.match(2).snippetBrackets());

	sq = new SpanClassQuery(new SpanTermQuery(new Term("base", "s:b")));
	kr = ki.search(sq, (short) 10);

	assertEquals("totalResults", 3, kr.totalResults());
	assertEquals("StartPos (0)", 1, kr.match(0).startPos);
	assertEquals("EndPos (0)", 2, kr.match(0).endPos);
	assertEquals("snippetBrackets (0)", "a[{b}]cabcab ...", kr.match(0).snippetBrackets());
	assertEquals("snippetHTML (0)", "a<span class=\"korap-match\"><span class=\"korap-highlight korap-class-0\">b</span></span>cabcab<span class=\"korap-more-right\"></span>", kr.match(0).snippetHTML());

	assertEquals("StartPos (1)", 4, kr.match(1).startPos);
	assertEquals("EndPos (1)", 5, kr.match(1).endPos);
	assertEquals("snippetBrackets (1)", "abca[{b}]cabac", kr.match(1).snippetBrackets());
	assertEquals("StartPos (2)", 7, kr.match(2).startPos);
	assertEquals("EndPos (2)", 8, kr.match(2).endPos);
	assertEquals("snippetBrackets (2)", "... bcabca[{b}]ac", kr.match(2).snippetBrackets());


	sq = new SpanNextQuery(
            new SpanTermQuery(new Term("base", "s:a")),
            new SpanClassQuery(new SpanTermQuery(new Term("base", "s:b")), (byte) 1)
        );

	kr = ki.search(sq, (short) 10);

	assertEquals("totalResults", 3, kr.totalResults());
	assertEquals("StartPos (0)", 0, kr.match(0).startPos);
	assertEquals("EndPos (0)", 2, kr.match(0).endPos);
	assertEquals("SnippetBrackets (0)", "[a{1:b}]cabcab ...", kr.match(0).snippetBrackets());

	assertEquals("SnippetHTML (0)", "<span class=\"korap-match\">a<span class=\"korap-highlight korap-class-1\">b</span></span>cabcab<span class=\"korap-more-right\"></span>", kr.match(0).snippetHTML());

	assertEquals("StartPos (1)", 3, kr.match(1).startPos);
	assertEquals("EndPos (1)", 5, kr.match(1).endPos);
	assertEquals("SnippetBrackets (1)", "abc[a{1:b}]cabac", kr.match(1).snippetBrackets());
	assertEquals("StartPos (2)", 6, kr.match(2).startPos);
	assertEquals("EndPos (2)", 8, kr.match(2).endPos);
	assertEquals("SnippetBrackets (2)", "abcabc[a{1:b}]ac", kr.match(2).snippetBrackets());


	// abcabcabac
	sq = new SpanNextQuery(
	    new SpanClassQuery(new SpanTermQuery(new Term("base", "s:a")), (byte) 2),
            new SpanClassQuery(new SpanTermQuery(new Term("base", "s:b")), (byte) 3)
        );

	kr = ki.search(sq, (short) 10);

	assertEquals("StartPos (0)", 0, kr.match(0).startPos);
	assertEquals("EndPos (0)", 2, kr.match(0).endPos);
	assertEquals("SnippetBrackets (0)", "[{2:a}{3:b}]cabcab ...", kr.match(0).snippetBrackets());
	assertEquals("StartPos (1)", 3, kr.match(1).startPos);
	assertEquals("EndPos (1)", 5, kr.match(1).endPos);
	assertEquals("SnippetBrackets (1)", "abc[{2:a}{3:b}]cabac", kr.match(1).snippetBrackets());

	assertEquals("StartPos (2)", 6, kr.match(2).startPos);
	assertEquals("EndPos (2)", 8, kr.match(2).endPos);
	assertEquals("SnippetBrackets (2)", "abcabc[{2:a}{3:b}]ac", kr.match(2).snippetBrackets());

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

	// System.err.println(kr.toJSON());

	assertEquals("totalResults", 1, kr.totalResults());
	assertEquals("SnippetBrackets (0)", "abcabc[a{2:b{a}}]c", kr.match(0).snippetBrackets());
	assertEquals("SnippetHTML (0)", "abcabc<span class=\"korap-match\">a<span class=\"korap-highlight korap-class-2\">b<span class=\"korap-highlight korap-class-0\">a</span></span></span>c", kr.match(0).snippetHTML());

	// Offset tokens
	kr = ki.search(sq, 0, (short) 10, true, (short) 2, true, (short) 2);
	assertEquals("totalResults", 1, kr.totalResults());
	assertEquals("SnippetBrackets (0)", "... bc[a{2:b{a}}]c", kr.match(0).snippetBrackets());
	assertEquals("SnippetHTML (0)", "<span class=\"korap-more-left\"></span>bc<span class=\"korap-match\">a<span class=\"korap-highlight korap-class-2\">b<span class=\"korap-highlight korap-class-0\">a</span></span></span>c", kr.match(0).snippetHTML());

	// Offset Characters
	kr = ki.search(sq, 0, (short) 10, false, (short) 2, false, (short) 2);
	assertEquals("totalResults", 1, kr.totalResults());
	assertEquals("SnippetBrackets (0)", "... bc[a{2:b{a}}]c", kr.match(0).snippetBrackets());
	assertEquals("SnippetHTML (0)", "<span class=\"korap-more-left\"></span>bc<span class=\"korap-match\">a<span class=\"korap-highlight korap-class-2\">b<span class=\"korap-highlight korap-class-0\">a</span></span></span>c", kr.match(0).snippetHTML());


	// System.err.println(kr.toJSON());

	sq = new SpanNextQuery(
	    new SpanClassQuery(new SpanTermQuery(new Term("base", "s:b")), (byte) 1),
            new SpanClassQuery(new SpanTermQuery(new Term("base", "s:c")), (byte) 2)
        );

	kr = ki.search(sq, (short) 10);
	
	assertEquals("totalResults", 2, kr.totalResults());
	assertEquals("StartPos (0)", 1, kr.match(0).startPos);
	assertEquals("EndPos (0)", 3, kr.match(0).endPos);
	assertEquals("StartPos (1)", 4, kr.match(1).startPos);
	assertEquals("EndPos (1)", 6, kr.match(1).endPos);

	assertEquals("Document count", 1, ki.numberOf("documents"));
	assertEquals("Token count", 10, ki.numberOf("t"));


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
	
	assertEquals("totalResults", 2, kr.totalResults());
	assertEquals("StartPos (0)", 0, kr.match(0).startPos);
	assertEquals("EndPos (0)", 3, kr.match(0).endPos);
	assertEquals("StartPos (1)", 3, kr.match(1).startPos);
	assertEquals("EndPos (1)", 6, kr.match(1).endPos);

	assertEquals(1, ki.numberOf("documents"));
	assertEquals(10, ki.numberOf("t"));
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
	assertEquals("ab[cabca]bac", kr.match(0).getSnippetBrackets());
	System.err.println();
	*/

	/*
	sq = new SpanNextQuery(
	       new SpanElementQuery("base", "x"),
	       new SpanTermQuery(new Term("base", "s:b"))
	);
	
	kr = ki.search(sq, (short) 10);
	assertEquals("abc[abcab}ac]", kr.match(0).getSnippetBrackets());
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
