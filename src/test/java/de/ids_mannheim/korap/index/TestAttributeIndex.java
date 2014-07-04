package de.ids_mannheim.korap.index;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.junit.Test;

import de.ids_mannheim.korap.KorapIndex;
import de.ids_mannheim.korap.KorapResult;
import de.ids_mannheim.korap.query.SpanAttributeQuery;
import de.ids_mannheim.korap.query.SpanElementAttributeQuery;
import de.ids_mannheim.korap.query.SpanElementQuery;
import de.ids_mannheim.korap.query.SpanNextQuery;

public class TestAttributeIndex {
	
	private KorapIndex ki = new KorapIndex();
	private KorapResult kr;
	private FieldDocument fd;

	public TestAttributeIndex() throws IOException {
		ki = new KorapIndex();		
	}
	
	private FieldDocument createFieldDoc0(){		
		fd = new FieldDocument();
		fd.addString("ID", "doc-0");
		fd.addTV("base",
			 "bcbabd",			 
			 "[(0-1)s:a|_1#0-1|<>:s#0-5$<i>5<s>-1|<>:div#0-3$<i>3<s>1|<>:div#0-2$<i>2<s>2|@:class=header$<s>1|@:class=header$<s>2]" +
			 "[(1-2)s:e|_2#1-2|<>:a#1-2$<i>2<s>1|@:class=header$<s>1]" +			 
			 "[(2-3)s:e|_3#2-3|<>:div#2-3$<i>5<s>1|@:class=time$<s>1]" +
			 "[(3-4)s:a|_4#3-4|<>:div#3-5$<i>5<s>1|@:class=header$<s>1]" + 
			 "[(4-5)s:b|_5#4-5|<>:div#4-5$<i>5<s>1|<>:a#4-5$<i>5<s>2|@:class=header$<s>2]" +
			 "[(5-6)s:d|_6#5-6|<>:s#5-6$<i>6<s>1|<>:div#5-6$<i>6<s>-1|@:class=header$<s>1]"+
			 "[(6-7)s:d|_7#6-7|<>:s#6-7$<i>7<s>2|<>:div#6-7$<i>7<s>1|@:class=header$<s>1|@:class=header$<s>2]");
			 
		return fd;
	}
	
	private FieldDocument createFieldDoc1(){
		fd = new FieldDocument();
		fd.addString("ID", "doc-1");
		fd.addTV("base",
			 "bcbabd",			 
			 "[(0-1)s:b|_1#0-1|<>:s#0-5$<i>5<s>-1|<>:div#0-3$<i>3<s>1|@:class=header$<s>1|@:class=title$<s>1|@:class=book$<s>1]" +
			 "[(1-2)s:c|_2#1-2|<>:div#1-2$<i>2<s>1|@:class=header$<s>1|@:class=title$<s>1]" +			 
			 "[(2-3)s:b|_3#2-3|<>:div#2-3$<i>5<s>1|@:class=book$<s>1]" +
			 "[(3-4)s:a|_4#3-4|<>:div#3-5$<i>5<s>1|@:class=title$<s>1]" + 
			 "[(4-5)s:b|_5#4-5|<>:div#4-5$<i>5<s>1|@:class=header$<s>1|@:class=book$<s>1|@:class=title$<s>1]" +
			 "[(5-6)s:d|_6#5-6|<>:s#5-6$<i>6<s>-1|<>:div#5-6$<i>6<s>1|@:class=header$<s>1]"+
			 "[(6-7)s:d|_7#6-7|<>:s#6-7$<i>7<s>2|<>:div#6-7$<i>7<s>1|@:class=header$<s>1|@:class=title$<s>1]");
			 
		return fd;
	}
	
	private FieldDocument createFieldDoc2(){
		fd = new FieldDocument();
		fd.addString("ID", "doc-1");
		fd.addTV("base",
			 "bcbabd",			 
			 "[(0-1)s:b|_1#0-1|<>:s#0-5$<i>5<s>1|<>:div#0-3$<i>3<s>2|@:class=header$<s>2|@:class=book$<s>1|@:class=book$<s>2]" +
			 "[(1-2)s:e|_2#1-2|<>:div#1-2$<i>2<s>1|<>:a#1-2$<i>2<s>2|@:class=book$<s>2|@:class=header$<s>1]" +
			 "[(2-3)s:b|_3#2-3|<>:div#2-3$<i>5<s>1|<>:a#1-2$<i>2<s>2|@:class=header$<s>2|@:class=book$<s>1]" +
			 "[(3-4)s:a|_4#3-4|<>:div#3-5$<i>5<s>1|@:class=title$<s>1]" + 
			 "[(4-5)s:b|_5#4-5|<>:div#4-5$<i>5<s>1|@:class=header$<s>1|@:class=book$<s>1|@:class=book$<s>1]" +
			 "[(5-6)s:d|_6#5-6|<>:s#5-6$<i>6<s>-1|<>:div#5-6$<i>6<s>1|@:class=header$<s>1]"+
			 "[(6-7)s:d|_7#6-7|<>:s#6-7$<i>7<s>2|<>:div#6-7$<i>7<s>1|@:class=header$<s>1|@:class=book$<s>2]");
			 
		return fd;
	}
	
		
	/** Test matching elementRef
	 * @throws IOException 
	 * */
	@Test
	public void testCase1() throws IOException {
		ki.addDoc(createFieldDoc0());
		ki.commit();
		
		SpanAttributeQuery saq = new SpanAttributeQuery(
				new SpanTermQuery(new Term("base","@:class=header")), 
				true);
		
		List<SpanQuery> sql = new ArrayList<>();
		sql.add(saq);
		
		SpanQuery sq = new SpanElementAttributeQuery(
				new SpanElementQuery("base", "div"),
				sql, true);
		
		kr = ki.search(sq, (short) 10);
		
		assertEquals(4, kr.getTotalResults());
		assertEquals(0,kr.getMatch(0).getStartPos());
		assertEquals(2,kr.getMatch(0).getEndPos());
		assertEquals(0,kr.getMatch(1).getStartPos());
		assertEquals(3,kr.getMatch(1).getEndPos());
		assertEquals(3,kr.getMatch(2).getStartPos());
		assertEquals(5,kr.getMatch(2).getEndPos());
		assertEquals(6,kr.getMatch(3).getStartPos());
		assertEquals(7,kr.getMatch(3).getEndPos());
	}	
	
	/** Test multiple attributes and negation
	 * @throws IOException 
	 * */
	@Test
	public void testCase2() throws IOException{
		ki.addDoc(createFieldDoc1());
		ki.commit();
		
		List<SpanQuery> sql = new ArrayList<>();
		sql.add(new SpanAttributeQuery(
				new SpanTermQuery(new Term("base","@:class=header")),true)
		);
		sql.add(new SpanAttributeQuery(
				new SpanTermQuery(new Term("base","@:class=title")),true)
		);
		
		SpanQuery sq = new SpanElementAttributeQuery(
				new SpanElementQuery("base", "div"),
				sql, true);
		
		kr = ki.search(sq, (short) 10);
		
		assertEquals(4, kr.getTotalResults());
		assertEquals(0,kr.getMatch(0).getStartPos());
		assertEquals(3,kr.getMatch(0).getEndPos());
		assertEquals(1,kr.getMatch(1).getStartPos());
		assertEquals(2,kr.getMatch(1).getEndPos());
		assertEquals(4,kr.getMatch(2).getStartPos());
		assertEquals(5,kr.getMatch(2).getEndPos());
		assertEquals(6,kr.getMatch(3).getStartPos());
		assertEquals(7,kr.getMatch(3).getEndPos());
		
		// Add not Attribute		
		sql.add(new SpanAttributeQuery(
				new SpanTermQuery(new Term("base","@:class=book")),true,true)
		);
		
		sq = new SpanElementAttributeQuery(
				new SpanElementQuery("base", "div"),
				sql, true);
		
		kr = ki.search(sq, (short) 10);
		
		assertEquals(2, kr.getTotalResults());
		assertEquals(1,kr.getMatch(0).getStartPos());
		assertEquals(2,kr.getMatch(0).getEndPos());
		assertEquals(6,kr.getMatch(1).getStartPos());
		assertEquals(7,kr.getMatch(1).getEndPos());		
		
		// Test multiple negations
		sql.remove(1);
		sql.add(new SpanAttributeQuery(
				new SpanTermQuery(new Term("base","@:class=title")),true,true)
		);
		
		sq = new SpanElementAttributeQuery(
				new SpanElementQuery("base", "div"),
				sql, true);
		
		kr = ki.search(sq, (short) 10);
		assertEquals(1, kr.getTotalResults());
		assertEquals(5,kr.getMatch(0).getStartPos());
		assertEquals(6,kr.getMatch(0).getEndPos());
	}	
		
	/** same attribute types referring to different element types
	 * */	 
	@Test
	public void testCase3() throws IOException{
		ki.addDoc(createFieldDoc2());
		ki.commit();
		
		List<SpanQuery> sql = new ArrayList<>();
		sql.add(new SpanAttributeQuery(
				new SpanTermQuery(new Term("base","@:class=header")),true)
		);		
		sql.add(new SpanAttributeQuery(
				new SpanTermQuery(new Term("base","@:class=book")),true,true)
		);
		SpanQuery sq = new SpanElementAttributeQuery(
				new SpanElementQuery("base", "div"),
				sql, true);
		
		kr = ki.search(sq, (short) 10);
		
		assertEquals(3, kr.getTotalResults());
		assertEquals(1,kr.getMatch(0).getStartPos());
		assertEquals(2,kr.getMatch(0).getEndPos());
		assertEquals(5,kr.getMatch(1).getStartPos());
		assertEquals(6,kr.getMatch(1).getEndPos());
		assertEquals(6,kr.getMatch(2).getStartPos());
		assertEquals(7,kr.getMatch(2).getEndPos());
	}
	
	/** Test SkipTo Doc */
	@Test
	public void testCase4() throws IOException{
		ki.addDoc(createFieldDoc1());
		ki.addDoc(createFieldDoc0());		
		ki.addDoc(createFieldDoc2());
		ki.commit();
		
		SpanAttributeQuery saq = new SpanAttributeQuery(
				new SpanTermQuery(new Term("base","@:class=book")), 
				true);
		
		List<SpanQuery> sql = new ArrayList<>();
		sql.add(saq);
		
		SpanElementAttributeQuery sq = new SpanElementAttributeQuery(
				new SpanElementQuery("base", "div"),
				sql, true);
		
		SpanNextQuery snq = new SpanNextQuery(
				new SpanTermQuery(new Term("base", "s:e"))
				,sq);
		
		kr = ki.search(snq, (short) 10);
		
		assertEquals(2,kr.getMatch(0).getLocalDocID());
		assertEquals(2,kr.getMatch(0).getStartPos());
		assertEquals(5,kr.getMatch(0).getEndPos());
	}
	
	
}
