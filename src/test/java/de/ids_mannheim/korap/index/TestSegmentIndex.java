package de.ids_mannheim.korap.index;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.KorapResult;
import de.ids_mannheim.korap.query.SpanElementQuery;
import de.ids_mannheim.korap.query.SpanNextQuery;
import de.ids_mannheim.korap.query.SpanSegmentQuery;
import de.ids_mannheim.korap.query.wrap.SpanSegmentQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanSequenceQueryWrapper;


@RunWith(JUnit4.class)
public class TestSegmentIndex {
	private SpanQuery sq;
	private KrillIndex ki;
	private KorapResult kr;
	private FieldDocument fd;
	private Logger log;	
	
	public TestSegmentIndex() throws IOException {
		ki = new KrillIndex();
		ki.addDoc(createFieldDoc0());
		ki.addDoc(createFieldDoc1());
		ki.addDoc(createFieldDoc2());
		ki.commit();
		
		log = LoggerFactory.getLogger(getClass());
	}

	/** Multiple matches in one document. */
	@Test
	public void testCase1() throws IOException {
		sq = new SpanSegmentQuery(
				new SpanTermQuery(new Term("base","s:b")),
				new SpanTermQuery(new Term("base","s:c"))
		);
		
		kr = ki.search(sq, (short) 10);
		ki.close();
		
		assertEquals("totalResults", kr.getTotalResults(), 3);
		assertEquals("StartPos (0)", 1, kr.getMatch(0).startPos);
		assertEquals("EndPos (0)", 2, kr.getMatch(0).endPos);
		assertEquals("StartPos (1)", 4, kr.getMatch(1).startPos);
		assertEquals("EndPos (1)", 5, kr.getMatch(1).endPos);		
	}
	
	/** Matches in multiple documents.
	 * 	Ensure the same document. The current secondspan is skipped to 
	 * 	the doc number of the firstspan.  */
	@Test
	public void testCase2() throws IOException {		
//		log.trace("Testcase2");
		sq = new SpanSegmentQuery(
				new SpanTermQuery(new Term("base","s:a")),				
				new SpanTermQuery(new Term("base","s:b"))
		);
		
		kr = ki.search(sq, (short) 10);
		ki.close();
				
		assertEquals("totalResults", kr.getTotalResults(), 3);
		// Match #0
		assertEquals("doc-number", 1, kr.getMatch(0).getLocalDocID());
		assertEquals("StartPos", 1, kr.getMatch(0).startPos);
		assertEquals("EndPos", 2, kr.getMatch(0).endPos);
		// Match #2
		assertEquals("doc-number", 2, kr.getMatch(2).getLocalDocID());
		assertEquals("StartPos", 2, kr.getMatch(2).startPos);
		assertEquals("EndPos", 3, kr.getMatch(2).endPos);		
	}
	
	
	/** Ensure the same document, skip to a greater doc number */
	@Test
	public void testCase3() throws IOException{
//		log.trace("Testcase3");
		sq = new SpanSegmentQuery(
				new SpanTermQuery(new Term("base","s:d")),
				new SpanTermQuery(new Term("base","s:b"))
		);
		
		kr = ki.search(sq, (short) 10);
		ki.close();
		
		assertEquals("totalResults", kr.getTotalResults(), 1);
		assertEquals("doc-number", 2, kr.getMatch(0).getLocalDocID());
		assertEquals("StartPos (0)", 1, kr.getMatch(0).startPos);
		assertEquals("EndPos (0)", 2, kr.getMatch(0).endPos);		
	}
	
	/** Matching a SpanElementQuery and a SpanNextQuery 
	 * 	Multiple atomic indices
	 * */
	@Test
	public void testCase4() throws IOException{
//		log.trace("Testcase4");
		
		ki = new KrillIndex();
		ki.addDoc(createFieldDoc0());
		ki.commit();
		ki.addDoc(createFieldDoc1());		
		ki.addDoc(createFieldDoc2());
		ki.commit();
		
		sq = new SpanSegmentQuery(
				new SpanElementQuery("base","e"),
				new SpanNextQuery(
					new SpanTermQuery(new Term("base","s:a")),
					new SpanTermQuery(new Term("base","s:b"))
				)
		);
		
		kr = ki.search(sq, (short) 10);
		ki.close();
		
		assertEquals("totalResults", kr.getTotalResults(), 2);
		// Match #0
		assertEquals("doc-number", 0, kr.getMatch(0).getLocalDocID());
		assertEquals("StartPos", 3, kr.getMatch(0).startPos);
		assertEquals("EndPos", 5, kr.getMatch(0).endPos);
		// Match #1
		assertEquals("doc-number", 0, kr.getMatch(1).getLocalDocID());
		assertEquals("StartPos", 1, kr.getMatch(1).startPos);
		assertEquals("EndPos", 3, kr.getMatch(1).endPos);				
	}
	
	/** Matching SpanElementQueries */
	@Test
	public void testCase5() throws IOException{
//		log.trace("Testcase5");
		sq = new SpanSegmentQuery(
				new SpanElementQuery("base","e"),
				new SpanElementQuery("base","e2")
		);
		
		kr = ki.search(sq, (short) 10);
		ki.close();			
		
		assertEquals("totalResults", kr.getTotalResults(), 1);
		// Match #0
		assertEquals("doc-number", 0, kr.getMatch(0).getLocalDocID());
		assertEquals("StartPos", 3, kr.getMatch(0).startPos);
		assertEquals("EndPos", 5, kr.getMatch(0).endPos);				
	}
		
	/** Skip to SegmentSpan */
	@Test
	public void testcase6() throws IOException{
		ki.addDoc(createFieldDoc4());
		ki.commit();
		sq = new SpanNextQuery(
				new SpanSegmentQuery(
					new SpanTermQuery(new Term("base","s:b")),
					new SpanTermQuery(new Term("base","s:c"))
				),
				new SpanTermQuery(new Term("base","s:d"))
			);		
		
		kr = ki.search(sq, (short) 10);
		ki.close();
		
		assertEquals("totalResults", kr.getTotalResults(), 2);				
		// Match #0
		assertEquals("doc-number", 0, kr.getMatch(0).getLocalDocID());
		assertEquals("StartPos (0)", 4, kr.getMatch(0).startPos);
		assertEquals("EndPos (0)", 6, kr.getMatch(0).endPos);
		// Match #1 in the other atomic index
		assertEquals("doc-number", 0, kr.getMatch(1).getLocalDocID());
		assertEquals("StartPos (0)", 0, kr.getMatch(1).startPos);
		assertEquals("EndPos (0)", 2, kr.getMatch(1).endPos);
	}

    
	private FieldDocument createFieldDoc0(){
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
	
	private FieldDocument createFieldDoc1(){
		fd = new FieldDocument();
		fd.addString("ID", "doc-1");
		fd.addTV("base",
			 "babaa",			 
			 "[(0-1)s:b|i:b|s:c|_1#0-1]" +
			 "[(1-2)s:a|i:a|s:b|_2#1-2|<>:e#1-3$<i>3]" +			 
			 "[(2-3)s:b|i:b|s:a|_3#2-3]" +
			 "[(3-4)s:a|i:a|_4#3-4]" +
			 "[(4-5)s:a|i:a|_5#4-5]");
		return fd;
	} 
	
	private FieldDocument createFieldDoc2(){
		fd = new FieldDocument();
		fd.addString("ID", "doc-2");
		fd.addTV("base",
			 "bdb",			 
			 "[(0-1)s:b|i:b|_1#0-1]" +
			 "[(1-2)s:d|i:d|s:b|_2#1-2]"+
			 "[(2-3)s:b|i:b|s:a|_3#2-3]");			 	
		return fd;
	}
	
	private FieldDocument createFieldDoc4(){
		fd = new FieldDocument();
		fd.addString("ID", "doc-4");
		fd.addTV("base",
			 "bdb",			 
			 "[(0-1)s:b|i:b|s:c|_1#0-1]" +
			 "[(1-2)s:d|_2#1-2]"+
			 "[(2-3)s:d|i:d|_3#2-3]");			 	
		return fd;
	}
}
