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
import de.ids_mannheim.korap.query.DistanceConstraint;
import de.ids_mannheim.korap.query.SpanDistanceQuery;
import de.ids_mannheim.korap.query.SpanElementQuery;
import de.ids_mannheim.korap.query.SpanSegmentQuery;

@RunWith(JUnit4.class)
public class TestDistanceIndex {
    KorapResult kr;
    KorapIndex ki;   
 
    private FieldDocument createFieldDoc0() {
    	FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-0");
        fd.addTV("base",
            "text",             
            "[(0-1)s:b|s:c|_1#0-1]" +
            "[(1-2)s:b|_2#1-2]" +             
            "[(2-3)s:c|_3#2-3]" +
            "[(3-4)s:c|_4#3-4]" + 
            "[(4-5)s:d|_5#4-5]" +             
            "[(5-6)s:d|_6#5-6]");
        return fd;
	}
    
    private FieldDocument createFieldDoc1(){
    	FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-1");
        fd.addTV("base",
            "text",             
            "[(0-1)s:c|_1#0-1]" +
            "[(1-2)s:e|_2#1-2]" +             
            "[(2-3)s:c|_3#2-3|<>:y#2-4$<i>4]" +
            "[(3-4)s:c|_4#3-4|<>:x#3-7$<i>7]" + 
            "[(4-5)s:d|_5#4-5|<>:y#4-6$<i>6]" +             
            "[(5-6)s:c|_6#5-6|<>:y#5-8$<i>8]" +
            "[(6-7)s:d|_7#6-7]" +
            "[(7-8)s:e|_8#7-8|<>:x#7-9$<i>9]" + 
            "[(8-9)s:e|_9#8-9|<>:x#8-10$<i>10]" + 
            "[(9-10)s:d|_10#9-10]");
        return fd;
    }
    
    private FieldDocument createFieldDoc2() {
    	FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-2");
        fd.addTV("base",
            "text",             
            "[(0-1)s:b|_1#0-1]" +
            "[(1-2)s:b|_2#1-2]" +             
            "[(2-3)s:d|_3#2-3]" +
            "[(3-4)s:e|_4#3-4]" + 
            "[(4-5)s:d|_5#4-5]" +             
            "[(5-6)s:e|_6#5-6]");
        return fd;
	}
    
    private SpanQuery createQuery(String x, String y, int min, int max, boolean isOrdered){    	
    	SpanQuery sq = new SpanDistanceQuery(
        		new SpanTermQuery(new Term("base",x)),
        		new SpanTermQuery(new Term("base",y)),
        		new DistanceConstraint(min, max, isOrdered, false),
        		true
        );
    	return sq;
    }
    
    private SpanQuery createElementQuery(String x, String y, int min, int max, boolean isOrdered){
    	SpanQuery sq = new SpanDistanceQuery(
        		new SpanElementQuery("base",x),
        		new SpanElementQuery("base",y),
        		new DistanceConstraint(min, max, isOrdered, false),
        		true
        );
    	return sq;
    }
    
    /**	- Intersection 
     * 	- Multiple occurrences in the same doc  
     *  - hasMoreFirstSpans = false for the current secondspan  
     * */
    @Test
    public void testCase1() throws IOException{
    	ki = new KorapIndex();
        ki.addDoc(createFieldDoc0()); 
        ki.commit();
        SpanQuery sq;
        // ---- Distance 0 to 1
        sq = createQuery("s:b","s:c",0,1,true);                
        kr = ki.search(sq, (short) 10);
//        System.out.println(sq);
        assertEquals(kr.getTotalResults(), 2);
        assertEquals(0, kr.getMatch(0).startPos);
        assertEquals(1, kr.getMatch(0).endPos);
        assertEquals(1, kr.getMatch(1).startPos);
        assertEquals(3, kr.getMatch(1).endPos);
        
        // ---- Distance 2 to 2
        sq = createQuery("s:b","s:c",2,2,true);                
        kr = ki.search(sq, (short) 10);
        
        assertEquals(kr.getTotalResults(), 2);
        assertEquals(0, kr.getMatch(0).startPos);
        assertEquals(3, kr.getMatch(0).endPos);
        assertEquals(1, kr.getMatch(1).startPos);
        assertEquals(4, kr.getMatch(1).endPos);
        
        // ---- Distance 2 to 3
        sq = createQuery("s:b","s:c",2,3,true);                
        kr = ki.search(sq, (short) 10);
        
        assertEquals(kr.getTotalResults(), 3);
        
        ki.close();
    }
    
    /** - Check candidate list: 
     * 	- CandidateList should not contain firstspans that are too far from 
     * 	  the current secondspan
     * 	- Add new candidates  
     * */
    @Test
    public void testCase2() throws IOException{
    	ki = new KorapIndex();
	    ki.addDoc(createFieldDoc1()); 
	    ki.commit();    
	    
	    // ---- Distance 1 to 3
	    // Candidate list for the current secondspan, is empty
	    SpanQuery sq = createQuery("s:c","s:d",1,3,true);                
	    kr = ki.search(sq, (short) 10);
	        	    
	    assertEquals((long) 4, kr.getTotalResults());
	    assertEquals(2, kr.getMatch(0).startPos);
	    assertEquals(5, kr.getMatch(0).endPos);
	    assertEquals(3, kr.getMatch(2).startPos);
	    assertEquals(7, kr.getMatch(2).endPos);
	    
	    ki.addDoc(createFieldDoc0());
	    ki.commit();

	    // ---- Distance 3 to 3
	    // Candidate list is empty, but there are secondspans in the other doc
	    sq = createQuery("s:c","s:d",3,3,true);                
	    kr = ki.search(sq, (short) 10);
	    assertEquals((long) 2, kr.getTotalResults());
	    
	    ki.close();
    }
        
    /** - Ensure the same document
     *  - Multiple matches in multiple documents and atomic indices
     * */
    @Test
    public void testCase3() throws IOException{
    	ki = new KorapIndex();
    	ki.addDoc(createFieldDoc0());
        ki.commit();
        ki.addDoc(createFieldDoc2());
        ki.addDoc(createFieldDoc1());
        ki.commit();
        
        SpanQuery sq;
        sq = createQuery("s:c","s:d",3,3,true);    
        kr = ki.search(sq, (short) 10);
        
        assertEquals(kr.getTotalResults(), 2);
    }
    
    /** - Firstspan.next() is in the other doc, but there is
     *    still a secondspans in the same doc
     *  - hasMoreFirstSpan and secondspans.next() are true,
     *    but ensureSameDoc() = false 
     * */ 
    @Test
    public void testCase4() throws IOException{
    	ki = new KorapIndex();
    	ki.addDoc(createFieldDoc0());
        ki.commit();
        ki.addDoc(createFieldDoc2());
        ki.addDoc(createFieldDoc1());
        ki.commit();
               
        // ---- Distance 1 to 2
        SpanQuery sq = createQuery("s:b","s:c",1,2,true);                
        kr = ki.search(sq, (short) 10);
        
        assertEquals(kr.getTotalResults(), 3);
        assertEquals(0, kr.getMatch(0).startPos);
	    assertEquals(3, kr.getMatch(0).endPos);
	    assertEquals(1, kr.getMatch(1).startPos);
	    assertEquals(3, kr.getMatch(1).endPos);
	    assertEquals(1, kr.getMatch(2).startPos);
	    assertEquals(4, kr.getMatch(2).endPos);
	    ki.close();      
    }
    
    /** ElementQueries */    
    @Test
    public void testCase5() throws IOException{    	
    	ki = new KorapIndex();
	    ki.addDoc(createFieldDoc1()); 
	    ki.commit();    
	    
	    // Intersection ---- Distance 0:0
	    SpanQuery sq = createElementQuery("x","y",0,0,true);                
	    kr = ki.search(sq, (short) 10);
    	
	    assertEquals(kr.getTotalResults(), 4);
	    assertEquals(2, kr.getMatch(0).startPos);
	    assertEquals(7, kr.getMatch(0).endPos);
	    assertEquals(3, kr.getMatch(1).startPos);
	    assertEquals(7, kr.getMatch(1).endPos);
	    assertEquals(3, kr.getMatch(2).startPos);
	    assertEquals(8, kr.getMatch(2).endPos);
    	
	    // Next to ---- Distance 1:1
	    sq = createElementQuery("y","x",1,1,true);                
	    kr = ki.search(sq, (short) 10);
	    
	    assertEquals(kr.getTotalResults(), 1);
	    assertEquals(5, kr.getMatch(0).startPos);
	    assertEquals(10, kr.getMatch(0).endPos);
	    
	    // ---- Distance 1:2
	    sq = createElementQuery("y","x",1,2,true);                
	    kr = ki.search(sq, (short) 10);
	    
	    assertEquals(kr.getTotalResults(), 2);	    
	    assertEquals(4, kr.getMatch(0).startPos);
	    assertEquals(9, kr.getMatch(0).endPos);
	    assertEquals(5, kr.getMatch(1).startPos);
	    assertEquals(10, kr.getMatch(1).endPos);
	    
	    // The same element type ---- Distance 1:2
	    sq = createElementQuery("x","x",1,2,true);
	    kr = ki.search(sq, (short) 10);
	    
	    assertEquals(kr.getTotalResults(), 2);
    }
    
    /** Skip to */    
    @Test
    public void testCase6() throws IOException{    	
    	ki = new KorapIndex();
    	ki.addDoc(createFieldDoc2());
	    ki.addDoc(createFieldDoc1());
	    ki.commit();
	    	    
	    SpanQuery firstClause = createQuery("s:d", "s:e", 3, 4,true);
	    kr = ki.search(firstClause, (short) 10); 
	    
	    assertEquals(kr.getTotalResults(), 3);
	    assertEquals(0, kr.getMatch(0).getLocalDocID());
	    assertEquals(2, kr.getMatch(0).startPos);
	    assertEquals(6, kr.getMatch(0).endPos);
	    assertEquals(1, kr.getMatch(1).getLocalDocID());
	    assertEquals(4, kr.getMatch(1).startPos);
	    assertEquals(8, kr.getMatch(1).endPos);
	    assertEquals(4, kr.getMatch(2).startPos);
	    assertEquals(9, kr.getMatch(2).endPos);	    
	    
		// The secondspans is skipped to doc# of the current firstspans
		SpanQuery sq = new SpanSegmentQuery(
	    		createQuery("s:d","s:e",3,4,true),
	    		createElementQuery("y","x",1,2,true)
		);	    
	    kr = ki.search(sq, (short) 10);
	    
	    assertEquals(kr.getTotalResults(), 1);
	    assertEquals(4, kr.getMatch(0).startPos);
	    assertEquals(9, kr.getMatch(0).endPos);	    
    }
    
    /** Same tokens */
    @Test
    public void testCase7() throws IOException{    	
    	ki = new KorapIndex();    	
	    ki.addDoc(createFieldDoc1());
	    ki.commit();
	    	    
	    SpanQuery sq = createQuery("s:c", "s:c", 1, 2,true);
	    kr = ki.search(sq, (short) 10); 
    
	    assertEquals(kr.getTotalResults(), 3);
        assertEquals(0, kr.getMatch(0).startPos);
	    assertEquals(3, kr.getMatch(0).endPos);
	    assertEquals(2, kr.getMatch(1).startPos);
	    assertEquals(4, kr.getMatch(1).endPos);
	    assertEquals(3, kr.getMatch(2).startPos);
	    assertEquals(6, kr.getMatch(2).endPos);
	    
	    ki.addDoc(createFieldDoc2());
	    ki.commit();
	    
	    // with order
	    sq = createQuery("s:e", "s:e", 1, 1,true);
	    kr = ki.search(sq, (short) 10);
	    
	    assertEquals(kr.getTotalResults(), 1);
	    
	    // without order
	    sq = createQuery("s:e", "s:e", 1, 1,false);
	    kr = ki.search(sq, (short) 10);
	    
	    assertEquals(kr.getTotalResults(), 2);
    }    
    
}
