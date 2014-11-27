package de.ids_mannheim.korap.index;

import static org.junit.Assert.*;

import java.io.IOException;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.junit.Test;

import de.ids_mannheim.korap.KorapIndex;
import de.ids_mannheim.korap.KorapResult;
import de.ids_mannheim.korap.query.DistanceConstraint;
import de.ids_mannheim.korap.query.SpanDistanceQuery;
import de.ids_mannheim.korap.query.SpanElementQuery;
import de.ids_mannheim.korap.query.SpanNextQuery;

public class TestDistanceExclusionIndex {

    private KorapIndex ki;
	private KorapResult kr;

	/** Ordered, unordered
	 * */
	@Test
    public void testCase1() throws IOException{
    	ki = new KorapIndex();
        ki.addDoc(createFieldDoc0()); 
        ki.commit();
        SpanQuery sq;
        //ordered distance 0 to 1
        sq = createQuery("s:c","s:e",0,1,true);                
        kr = ki.search(sq, (short) 10);        
        assertEquals((long) 3, kr.getTotalResults());	    
	    assertEquals(2, kr.getMatch(0).getStartPos());
	    assertEquals(3, kr.getMatch(0).getEndPos());
	    assertEquals(3, kr.getMatch(1).getStartPos());
	    assertEquals(4, kr.getMatch(1).getEndPos());
	    assertEquals(5, kr.getMatch(2).getStartPos());
	    assertEquals(6, kr.getMatch(2).getEndPos());
        
        // Unordered
        sq = createQuery("s:c","s:e",0,1,false);                
        kr = ki.search(sq, (short) 10);
        assertEquals((long) 2, kr.getTotalResults());
    }
	
	/** Multiple docs, unordered
	 * 	No more secondSpans
	 * */
	@Test
    public void testCase2() throws IOException{
    	ki = new KorapIndex();
        ki.addDoc(createFieldDoc0()); 
        ki.addDoc(createFieldDoc1());
        ki.commit();
        SpanQuery sq;
        // ---- Distance 0 to 1
        sq = createQuery("s:c","s:e",0,1,false);                
        kr = ki.search(sq, (short) 10);        
        assertEquals((long) 5, kr.getTotalResults());	    
	    assertEquals(1, kr.getMatch(3).getLocalDocID());
	}
	
	/** Secondspans' document number is bigger than firstspans' 
	 * 	Actual distance is smaller than min distance.
	 * */
	@Test
    public void testCase3() throws IOException{
		ki = new KorapIndex();
		ki.addDoc(createFieldDoc1());
		ki.addDoc(createFieldDoc0());        
        ki.commit();
        
        SpanQuery sq;
        // Unordered
        sq = createQuery("s:c","s:e",2,2,false);                
        kr = ki.search(sq, (short) 10);
        assertEquals((long) 5, kr.getTotalResults());
	}
	
	/** Unordered: firstspan in on the right side of the secondspan, 
	 * 	but within max distance.
	 * */
	@Test
    public void testCase4() throws IOException{
		ki = new KorapIndex();
		ki.addDoc(createFieldDoc2());		        
        ki.commit();
        
        SpanQuery sq;
        // Unordered
        sq = createQuery("s:b","s:c",2,2,false);                
        kr = ki.search(sq, (short) 10);
        assertEquals((long) 1, kr.getTotalResults());
        assertEquals(1, kr.getMatch(0).getStartPos());
	    assertEquals(2, kr.getMatch(0).getEndPos());
    }
	
	/**	Element queries
	 * */
	@Test
    public void testCase5() throws IOException{
		ki = new KorapIndex();
		ki.addDoc(createFieldDoc0());		        
        ki.commit();
        
        SpanDistanceQuery sq;        
        sq = new SpanDistanceQuery(
        		new SpanElementQuery("base", "x"),
        		new SpanElementQuery("base", "y"),
        		new DistanceConstraint(0, 1, false, true),
        		true);        
        
        kr = ki.search(sq, (short) 10);
        assertEquals((long) 1, kr.getTotalResults());
        assertEquals(9, kr.getMatch(0).getStartPos());
	    assertEquals(10, kr.getMatch(0).getEndPos());
    }
	
	// Add skipTo test	
	@Test
	public void testCase6() throws IOException{
		ki = new KorapIndex();
		ki.addDoc(createFieldDoc1());
		ki.addDoc(createFieldDoc2());
        ki.commit();
        
        SpanQuery sq;
        //ordered distance 0 to 1
        sq = createQuery("s:d","s:b",0,1,true);
        kr = ki.search(sq, (short) 10);
        assertEquals((long) 4, kr.getTotalResults());
        
		SpanTermQuery stq = new SpanTermQuery(new Term("base", "s:c"));
		kr = ki.search(stq, (short) 10);
		assertEquals((long) 6, kr.getTotalResults());
		
		SpanNextQuery snq = new SpanNextQuery(stq,sq);		
        kr = ki.search(snq, (short) 10);
        assertEquals((long) 2, kr.getTotalResults());
        assertEquals(3, kr.getMatch(0).getStartPos());
	    assertEquals(5, kr.getMatch(0).getEndPos());
	    assertEquals(8, kr.getMatch(1).getStartPos());
	    assertEquals(10, kr.getMatch(1).getEndPos());
	    
        /*System.out.print(kr.getTotalResults()+"\n");
		for (int i=0; i< kr.getTotalResults(); i++){
			System.out.println(
				kr.getMatch(i).getLocalDocID()+" "+
				kr.getMatch(i).startPos + " " +
				kr.getMatch(i).endPos
		    );
		}*/
	}
	
    private SpanQuery createQuery(String x, String y, int min, int max, boolean isOrdered){
    	SpanDistanceQuery sq = new SpanDistanceQuery(
        		new SpanTermQuery(new Term("base",x)),
        		new SpanTermQuery(new Term("base",y)),
        		new DistanceConstraint(min, max, isOrdered,true),
        		true
        );    	
    	return sq;
    }
    
    private FieldDocument createFieldDoc0(){
    	FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-0");
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
            "[(8-9)s:e|_9#8-9]" + 
            "[(9-10)s:d|_10#9-10|<>:x#9-10$<i>10]");
        return fd;
    }
    
    private FieldDocument createFieldDoc1() {
    	FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-1");
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
    
    private FieldDocument createFieldDoc2() {
    	FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-2");
        fd.addTV("base",
            "text",             
            "[(0-1)s:b|_1#0-1]" +
            "[(1-2)s:b|_2#1-2]" +             
            "[(2-3)s:c|_3#2-3]" +
            "[(3-4)s:c|_4#3-4]" + 
            "[(4-5)s:b|_5#4-5]" +             
            "[(5-6)s:d|_6#5-6]" + 
            "[(6-7)s:b|_7#6-7]" +
            "[(7-8)s:d|_8#7-8]" + 
            "[(8-9)s:c|_9#8-9]" + 
            "[(9-10)s:d|_10#9-10]");
        return fd;
	}
    
}
