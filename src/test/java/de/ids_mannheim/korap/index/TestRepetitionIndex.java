package de.ids_mannheim.korap.index;

import static org.junit.Assert.*;

import java.io.IOException;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.junit.Test;

import de.ids_mannheim.korap.KorapIndex;
import de.ids_mannheim.korap.response.Match;
import de.ids_mannheim.korap.KorapResult;
import de.ids_mannheim.korap.query.SpanNextQuery;
import de.ids_mannheim.korap.query.SpanRepetitionQuery;

public class TestRepetitionIndex {
	
    private KorapIndex ki;
	private KorapResult kr;

	private FieldDocument createFieldDoc0(){
    	FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-0");
        fd.addTV("base",
            "text",             
            "[(0-1)s:c|_1#0-1]" +
            "[(1-2)s:e|_2#1-2]" +             
            "[(2-3)s:c|_3#2-3|<>:y#2-4$<i>4]" +
            "[(3-4)s:c|s:b|_4#3-4|<>:x#3-7$<i>7]" + 
            "[(4-5)s:e|s:d|_5#4-5|<>:y#4-6$<i>6]" +             
            "[(5-6)s:c|_6#5-6|<>:y#5-8$<i>8]" +
            "[(6-7)s:d|_7#6-7]" +
            "[(7-8)s:e|_8#7-8|<>:x#7-9$<i>9]" + 
            "[(8-9)s:e|s:b|_9#8-9|<>:x#8-10$<i>10]" + 
            "[(9-10)s:d|_10#9-10]");
        return fd;
    }
	
	private FieldDocument createFieldDoc1() {
    	FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-1");
        fd.addTV("base",
            "text",             
            "[(0-1)s:b|_1#0-1]" +
            "[(1-2)s:e|_2#1-2]" +             
            "[(2-3)s:c|_3#2-3]" +
            "[(3-4)s:c|s:d]" + 
            "[(4-5)s:d|s:c|_5#4-5]" +             
            "[(5-6)s:e|s:c|_6#5-6]" +
    		"[(6-7)s:e|_7#6-7]" +	
	        "[(7-8)s:c|_8#7-8]" + 
	        "[(8-9)s:d|_9#8-9]" + 
	        "[(9-10)s:d|_10#9-10]");
        return fd;
	}
	
	private FieldDocument createFieldDoc2() {
    	FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-2");
        fd.addTV("base",
            "text",             
            "[(0-1)s:b|s:c|_1#0-1|<>:s#0-2$<i>1]" +
            "[(1-2)s:c|_2#1-2]" +             
            "[(2-3)s:b|_3#2-3|<>:s#2-3$<i>3]" +
            "[(3-4)s:c|_4#3-4|<>:s#3-4$<i>4]" + 
            "[(4-5)s:c|_5#4-5|<>:s#4-5$<i>5]" +             
            "[(5-6)s:b|_6#5-6]" +
            "[(6-7)s:c|_7#6-7|<>:s#6-7$<i>7]");
        return fd;
	}
	
	private FieldDocument createFieldDoc3() {
    	FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-3");
        fd.addTV("base",
            "text",             
            "[(0-1)s:a|_1#0-1|<>:s#0-2$<i>1]" +
            "[(1-2)s:d|_2#1-2|<>:s#1-2$<i>3]" +             
            "[(2-3)s:e|_3#2-3]");	
        return fd;
	}

	@Test
	public void testCase1() throws IOException{
		ki = new KorapIndex();
        ki.addDoc(createFieldDoc0());
        ki.commit();
        
        SpanQuery sq, sq2;
        // Quantifier only
        // c{1,2}
        sq = new SpanRepetitionQuery(new SpanTermQuery(new Term("base","s:c")),1,2, true);
        kr = ki.search(sq, (short) 10);
        // 0-1, 2-3, 2-4, 3-4, 5-6
        assertEquals((long) 5,kr.getTotalResults());        
        
        // ec{1,2}
        sq = new SpanNextQuery(
        		new SpanTermQuery(new Term("base", "s:e")),
        		new SpanRepetitionQuery(new SpanTermQuery(new Term("base","s:c")),1,2, true)
    		);
        
        kr = ki.search(sq, (short) 10);
        // 1-3, 1-4, 4-6
        assertEquals((long) 3, kr.getTotalResults());   
        
        // ec{1,2}d
        sq2 = new SpanNextQuery(sq, new SpanTermQuery(new Term("base", "s:d")));        
        kr = ki.search(sq2, (short) 10);        
        assertEquals((long) 2,kr.getTotalResults());
        assertEquals(1, kr.getMatch(0).startPos);
        assertEquals(5, kr.getMatch(0).endPos);
        assertEquals(4, kr.getMatch(1).startPos);
        assertEquals(7, kr.getMatch(1).endPos);
        
        // Multiple documents        
        ki.addDoc(createFieldDoc1());
        ki.commit();
        kr = ki.search(sq2, (short) 10);
        assertEquals((long) 5, kr.getTotalResults());
	}
	
	/** Skip to */
	@Test
	public void testCase2() throws IOException{
		ki = new KorapIndex();
        ki.addDoc(createFieldDoc0());
        ki.addDoc(createFieldDoc3());
        ki.addDoc(createFieldDoc2());
        ki.addDoc(createFieldDoc1());
        ki.commit();
        
        SpanQuery sq;
        // c{2,2}
        sq = new SpanRepetitionQuery(new SpanTermQuery(new Term("base","s:c")),2,2, true);
        kr = ki.search(sq, (short) 10);
        // doc1 2-4, 3-5, 4-6
        assertEquals((long)6,kr.getTotalResults());
        
        // ec{2,2}
        kr = ki.search(sq, (short) 10); 
        sq = new SpanNextQuery(
        		new SpanTermQuery(new Term("base", "s:e")),
        		new SpanRepetitionQuery(new SpanTermQuery(new Term("base","s:c")),2,2, true)
    		);
        
        kr = ki.search(sq, (short) 10); 
        assertEquals((long)2,kr.getTotalResults());
        assertEquals(3,kr.getMatch(1).getLocalDocID());        
       
	}
	
	/** OR */
	@Test
	public void testCase3() throws IOException{
		ki = new KorapIndex();
        ki.addDoc(createFieldDoc0());
        ki.commit();
        
        SpanQuery sq,sq2;
     // ec{1,2}
        sq = new SpanNextQuery(
        		new SpanTermQuery(new Term("base", "s:e")),
        		new SpanOrQuery(
    				new SpanRepetitionQuery(new SpanTermQuery(new Term("base","s:c")),1,1, true),
    				new SpanRepetitionQuery(new SpanTermQuery(new Term("base","s:b")),1,1, true)
				)
    		);        
        kr = ki.search(sq, (short) 10); 
        assertEquals((long)3,kr.getTotalResults());
        assertEquals(1, kr.getMatch(0).startPos);
        assertEquals(3, kr.getMatch(0).endPos);
        assertEquals(4, kr.getMatch(1).startPos);
        assertEquals(6, kr.getMatch(1).endPos);
        assertEquals(7, kr.getMatch(2).startPos);
        assertEquals(9, kr.getMatch(2).endPos);

	}
	
	@Test
	public void testCase4() throws IOException {
		ki = new KorapIndex();
        ki.addDoc(createFieldDoc1());
        ki.commit();
        
		SpanQuery sq;
        // c{2,2}
        sq = new SpanRepetitionQuery(new SpanTermQuery(new Term("base","s:c")),1,3, true);
        kr = ki.search(sq, (short) 10);
        // 2-3, 2-4, 2-5, 3-4, 3-5, 3-6, 4-5, 4-6, 5-6, 7-8  
        assertEquals((long)10,kr.getTotalResults());
        
        sq = new SpanRepetitionQuery(new SpanTermQuery(new Term("base","s:c")),2,3, true);
        kr = ki.search(sq, (short) 10);
        // 2-4, 2-5, 3-5, 3-6, 4-6 
        assertEquals((long)5,kr.getTotalResults());
        
//        System.out.print(kr.getTotalResults()+"\n");
//		for (int i=0; i< kr.getTotalResults(); i++){
//			System.out.println(
//				kr.match(i).getLocalDocID()+" "+
//				kr.match(i).startPos + " " +
//				kr.match(i).endPos
//			);
//		}
	}
	
	@Test
	public void testCase5() throws IOException {
		ki = new KorapIndex();
	    ki.addDocFile(
	        getClass().getResource("/wiki/00001.json.gz").getFile(), true
        );
		ki.commit();
		
		SpanQuery sq0, sq1, sq2;
		sq0 = new SpanTermQuery(new Term("tokens", "tt/p:NN"));
		sq1 = new SpanRepetitionQuery(new SpanTermQuery(new Term("tokens","tt/p:ADJA")),2,3, true);
        sq2 = new SpanNextQuery(sq1,sq0);        
        kr = ki.search(sq2, (short) 10);
        
        assertEquals((long)2,kr.getTotalResults());
        assertEquals(73, kr.getMatch(0).getStartPos());
        assertEquals(77, kr.getMatch(0).getEndPos());
        assertEquals(74, kr.getMatch(1).getStartPos());
        assertEquals(77, kr.getMatch(1).getEndPos());
       /* for (Match km : kr.getMatches()){
        	System.out.println(km.getSnippetBrackets());
        	System.out.println(km.getStartPos() +","+km.getEndPos());
        }*/
        
        sq2 = new SpanNextQuery(
        		new SpanTermQuery(new Term("tokens", "s:offenen")),
        		sq2);
        kr = ki.search(sq2, (short) 10);
        
        assertEquals((long)1,kr.getTotalResults());
        assertEquals(73, kr.getMatch(0).getStartPos());
        assertEquals(77, kr.getMatch(0).getEndPos());
        /*
        for (Match km : kr.getMatches()){
        	System.out.println(km.getSnippetBrackets());
        	System.out.println(km.getStartPos() +","+km.getEndPos());
        }*/
	}
}
