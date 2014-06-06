package de.ids_mannheim.korap.index;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.junit.Test;

import de.ids_mannheim.korap.KorapIndex;
import de.ids_mannheim.korap.KorapResult;
import de.ids_mannheim.korap.query.SpanAttributeQuery;
import de.ids_mannheim.korap.query.SpanElementAttributeQuery;
import de.ids_mannheim.korap.query.SpanElementQuery;

public class TestAttributeIndex {
	
	private KorapIndex ki;
	private KorapResult kr;
	private FieldDocument fd;

	public TestAttributeIndex() throws IOException {
		ki = new KorapIndex();
		ki.addDoc(createFieldDoc0());
//		ki.addDoc(createFieldDoc1());
//		ki.addDoc(createFieldDoc2());
		ki.commit();
	}
	
	private FieldDocument createFieldDoc0(){
		fd = new FieldDocument();
		fd.addString("ID", "doc-0");
		fd.addTV("base",
			 "bcbabd",			 
			 "[(0-1)s:b|_1#0-1|<>:s#0-5$<i>5<s>1|<>:div#0-3$<i>3<s>2|<>:div#0-2$<i>2<s>3|@:class=header$<s>2|@:class=header$<s>3]" +
			 "[(1-2)s:c|_2#1-2|<>:a#1-2$<i>2<s>1|@:class=header$<s>1]" +			 
			 "[(2-3)s:b|_3#2-3|<>:div#2-3$<i>5<s>1|@:class=time$<s>1]" +
			 "[(3-4)s:a|_4#3-4|<>:div#3-5$<i>5<s>1|@:class=header$<s>1]" + 
			 "[(4-5)s:b|_5#4-5|<>:div#4-5$<i>5<s>1|<>:a#4-5$<i>5<s>2|@:class=header$<s>2]" +			 
			 "[(5-6)s:d|_6#5-6|<>:s#5-6$<i>6<s>2|<>:div#5-6$<i>6<s>1|@:class=header$<s>1|@:class=header$<s>2]");
		return fd;
	}
	
	
	@Test
	public void testCase1() {
		SpanAttributeQuery saq = new SpanAttributeQuery(
				new SpanTermQuery(new Term("base","@:class=header")), 
				true);
		
		SpanQuery sq = new SpanElementAttributeQuery(
				new SpanElementQuery("base", "div"),
				saq, true);
		
		kr = ki.search(sq, (short) 10);
		
		assertEquals(4, kr.getTotalResults());
		assertEquals(0,kr.getMatch(0).getStartPos());
		assertEquals(2,kr.getMatch(0).getEndPos());
		assertEquals(0,kr.getMatch(1).getStartPos());
		assertEquals(3,kr.getMatch(1).getEndPos());
		assertEquals(3,kr.getMatch(2).getStartPos());
		assertEquals(5,kr.getMatch(2).getEndPos());
		assertEquals(5,kr.getMatch(3).getStartPos());
		assertEquals(6,kr.getMatch(3).getEndPos());
	}	
	
	
}
