package de.ids_mannheim.korap.index;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import de.ids_mannheim.korap.KorapIndex;
import de.ids_mannheim.korap.KorapResult;
import de.ids_mannheim.korap.query.SpanElementQuery;
import de.ids_mannheim.korap.query.SpanNextQuery;
import de.ids_mannheim.korap.query.SpanSegmentQuery;


@RunWith(JUnit4.class)
public class TestSegmentIndex {
	private SpanQuery sq;
	private KorapIndex ki;
	private KorapResult kr;
	private FieldDocument fd;
	
	public TestSegmentIndex() throws IOException {
		ki = new KorapIndex();
		ki.addDoc(createFieldDoc1());
		ki.addDoc(createFieldDoc2());
		ki.addDoc(createFieldDoc3());
		ki.commit();
	}

	/** Multiple matches in one document. */
	@Test
	public void testCase1() throws IOException {
		System.out.println("Testcase1");
		sq = new SpanSegmentQuery(
				new SpanTermQuery(new Term("base","s:b")),
				new SpanTermQuery(new Term("base","s:c"))
		);
		
		kr = ki.search(sq, (short) 10);
		ki.close();
		
		assertEquals("totalResults", 2, kr.totalResults());
		assertEquals("StartPos (0)", 1, kr.match(0).startPos);
		assertEquals("EndPos (0)", 2, kr.match(0).endPos);
		assertEquals("StartPos (1)", 4, kr.match(1).startPos);
		assertEquals("EndPos (1)", 5, kr.match(1).endPos);		
	}
	
	/** Matches in multiple documents.
	 * 	Ensure the same document. The current secondspan is skipped to 
	 * 	the doc number of the firstspan.  */
	@Test
	public void testCase2() throws IOException {
		System.out.println("Testcase2");
		sq = new SpanSegmentQuery(
				new SpanTermQuery(new Term("base","s:a")),				
				new SpanTermQuery(new Term("base","s:b"))
		);
		
		kr = ki.search(sq, (short) 10);
		ki.close();
				
		assertEquals("totalResults", 3, kr.totalResults());
		// Match #0
		assertEquals("doc-number", 1, kr.match(0).localDocID);
		assertEquals("StartPos", 1, kr.match(0).startPos);
		assertEquals("EndPos", 2, kr.match(0).endPos);
		// Match #2
		assertEquals("doc-number", 2, kr.match(2).localDocID);
		assertEquals("StartPos", 2, kr.match(2).startPos);
		assertEquals("EndPos", 3, kr.match(2).endPos);		
	}
	
	
	/** Ensure the same document, skip to a greater doc number */
	@Test
	public void testCase3() throws IOException{
		System.out.println("Testcase3");
		sq = new SpanSegmentQuery(
				new SpanTermQuery(new Term("base","s:d")),
				new SpanTermQuery(new Term("base","s:b"))
		);
		
		kr = ki.search(sq, (short) 10);
		ki.close();
		
		assertEquals("totalResults", 1, kr.totalResults());
		assertEquals("doc-number", 2, kr.match(0).localDocID);
		assertEquals("StartPos (0)", 1, kr.match(0).startPos);
		assertEquals("EndPos (0)", 2, kr.match(0).endPos);		
	}
	
	/** Matching a SpanElementQuery and a SpanNextQuery */
	@Test
	public void testCase4() throws IOException{
		System.out.println("Testcase4");
		sq = new SpanSegmentQuery(
				new SpanElementQuery("base","e"),
				new SpanNextQuery(
					new SpanTermQuery(new Term("base","s:a")),
					new SpanTermQuery(new Term("base","s:b"))
				)
		);
		
		kr = ki.search(sq, (short) 10);
		ki.close();
		
		assertEquals("totalResults", 2, kr.totalResults());
		// Match #0
		assertEquals("doc-number", 0, kr.match(0).localDocID);
		assertEquals("StartPos", 3, kr.match(0).startPos);
		assertEquals("EndPos", 5, kr.match(0).endPos);
		// Match #1
		assertEquals("doc-number", 1, kr.match(1).localDocID);
		assertEquals("StartPos", 1, kr.match(1).startPos);
		assertEquals("EndPos", 3, kr.match(1).endPos);				
	}
	
	/** SpanElementQueries */
	@Test
	public void testCase5() throws IOException{
		System.out.println("Testcase5");
		sq = new SpanSegmentQuery(
				new SpanElementQuery("base","e"),
				new SpanElementQuery("base","e2")
		);
		
		kr = ki.search(sq, (short) 10);
		ki.close();
		
		assertEquals("totalResults", 1, kr.totalResults());
		// Match #0
		assertEquals("doc-number", 0, kr.match(0).localDocID);
		assertEquals("StartPos", 3, kr.match(0).startPos);
		assertEquals("EndPos", 5, kr.match(0).endPos);				
	}
	
	
	private FieldDocument createFieldDoc1(){
		fd = new FieldDocument();
		fd.addString("ID", "doc-0");
		fd.addTV("base",
			 "bcbabd",			 
			 "[(0-1)s:b|i:b|_1#0-1]" +
			 "[(1-2)s:c|i:c|s:b|_2#1-2]" +			 
			 "[(2-3)s:b|i:b|_3#2-3|<>:e#2-4$<i>4]" +
			 "[(3-4)s:a|i:a|_4#3-4|<>:e#3-5$<i>5|<>:e2#3-5$<i>5]" + 
			 "[(4-5)s:b|i:b|s:c|_5#4-5]" +			 
			 "[(5-6)s:d|i:d|_6#5-6|<>:e2#5-6$<i>6]");
		return fd;
	}
	
	private FieldDocument createFieldDoc2(){
		fd = new FieldDocument();
		fd.addString("ID", "doc-1");
		fd.addTV("base",
			 "babaa",			 
			 "[(0-1)s:b|i:b|_1#0-1]" +
			 "[(1-2)s:a|i:a|s:b|_2#1-2|<>:e#1-3$<i>3]" +			 
			 "[(2-3)s:b|i:b|s:a|_3#2-3]" +
			 "[(3-4)s:a|i:a|_4#3-4]" +
			 "[(4-5)s:a|i:a|_5#4-5]");
		return fd;
	} 
	
	private FieldDocument createFieldDoc3(){
		fd = new FieldDocument();
		fd.addString("ID", "doc-2");
		fd.addTV("base",
			 "bdb",			 
			 "[(0-1)s:b|i:b|_1#0-1]" +
			 "[(1-2)s:d|i:d|s:b|_2#1-2]"+
			 "[(2-3)s:b|i:b|s:a|_3#2-3]");			 	
		return fd;
	}
}
