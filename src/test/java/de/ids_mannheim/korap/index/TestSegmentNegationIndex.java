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
public class TestSegmentNegationIndex {
    private SpanQuery sq;
    private KrillIndex ki;
    private KorapResult kr;
    private FieldDocument fd;
    private Logger log;	
	
    @Test
    public void testcaseNegation() throws Exception {
	ki = new KrillIndex();
	ki.addDoc(createFieldDoc0());
	ki.addDoc(createFieldDoc1());
	ki.addDoc(createFieldDoc2());
	ki.addDoc(createFieldDoc3());
	ki.commit();
	SpanSegmentQueryWrapper ssqw = new SpanSegmentQueryWrapper("base","s:b");
	ssqw.with("s:c");
	SpanSequenceQueryWrapper sqw = new SpanSequenceQueryWrapper("base", ssqw).append("s:d");

	kr = ki.search(sqw.toQuery(), (short) 10);
		
	assertEquals("totalResults", kr.getTotalResults(), 2);				
	// Match #0
	assertEquals("doc-number", 0, kr.getMatch(0).getLocalDocID());
	assertEquals("StartPos (0)", 4, kr.getMatch(0).startPos);
	assertEquals("EndPos (0)", 6, kr.getMatch(0).endPos);

	// Match #1 in the other atomic index
	assertEquals("doc-number", 3, kr.getMatch(1).getLocalDocID());
	assertEquals("StartPos (0)", 0, kr.getMatch(1).startPos);
	assertEquals("EndPos (0)", 2, kr.getMatch(1).endPos);
		
	ssqw = new SpanSegmentQueryWrapper("base","s:b");
	ssqw.without("s:c");
	sqw = new SpanSequenceQueryWrapper("base", ssqw).append("s:a");

	kr = ki.search(sqw.toQuery(), (short) 10);

	assertEquals("doc-number", 0, kr.getMatch(0).getLocalDocID());
	assertEquals("StartPos (0)", 2, kr.getMatch(0).startPos);
	assertEquals("EndPos (0)", 4, kr.getMatch(0).endPos);

	assertEquals("doc-number", 1, kr.getMatch(1).getLocalDocID());
	assertEquals("StartPos (1)", 1, kr.getMatch(1).startPos);
	assertEquals("EndPos (1)", 3, kr.getMatch(1).endPos);

	assertEquals("doc-number", 1, kr.getMatch(2).getLocalDocID());
	assertEquals("StartPos (2)", 2, kr.getMatch(2).startPos);
	assertEquals("EndPos (2)", 4, kr.getMatch(2).endPos);

	assertEquals("doc-number", 2, kr.getMatch(3).getLocalDocID());
	assertEquals("StartPos (3)", 1, kr.getMatch(3).startPos);
	assertEquals("EndPos (3)", 3, kr.getMatch(3).endPos);
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
	
    private FieldDocument createFieldDoc3(){
	fd = new FieldDocument();
	fd.addString("ID", "doc-3");
	fd.addTV("base",
		 "bdb",			 
		 "[(0-1)s:b|i:b|s:c|_1#0-1]" +
		 "[(1-2)s:d|_2#1-2]"+
		 "[(2-3)s:d|i:d|_3#2-3]");			 	
	return fd;
    }
}
