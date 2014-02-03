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
import de.ids_mannheim.korap.query.SpanDistanceQuery;
import de.ids_mannheim.korap.query.SpanElementQuery;
import de.ids_mannheim.korap.query.SpanNextQuery;
import de.ids_mannheim.korap.query.SpanSegmentQuery;

@RunWith(JUnit4.class)
public class TestUnorderedDistanceIndex{
	
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
            "[(3-4)s:c|_4#3-4|<>:x#3-7$<i>7]" + 
            "[(4-5)s:d|_5#4-5|<>:y#4-6$<i>6]" +             
            "[(5-6)s:c|_6#5-6|<>:y#5-8$<i>8]" +
            "[(6-7)s:d|_7#6-7]" +
            "[(7-8)s:f|_8#7-8|<>:x#7-9$<i>9]" + 
            "[(8-9)s:e|_9#8-9|<>:x#8-10$<i>10]" + 
            "[(9-10)s:d|_10#9-10]");
        return fd;
    }
	
	private FieldDocument createFieldDoc1(){
		FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-1");
        fd.addTV("base",
            "text",             
            "[(0-1)s:d|_1#0-1]" +
            "[(1-2)s:c|_2#1-2]" +             
            "[(2-3)s:e|_3#2-3]" +
            "[(3-4)s:e|_4#3-4]" + 
            "[(4-5)s:d|_5#4-5]" +             
            "[(5-6)s:e|_6#5-6]" +
            "[(6-7)s:e|_7#6-7]" +
            "[(7-8)s:c|_8#7-8]" + 
            "[(8-9)s:e|_9#8-9]" + 
            "[(9-10)s:d|_10#9-10]");
        return fd;
	}
	
	private FieldDocument createFieldDoc2(){
		FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-2");
        fd.addTV("base",
            "text",             
            "[(0-1)s:f|_1#0-1]" +
            "[(1-2)s:c|_2#1-2]" +             
            "[(2-3)s:e|_3#2-3]" +
            "[(3-4)s:e|_4#3-4]" + 
            "[(4-5)s:d|_5#4-5]" +             
            "[(5-6)s:f|_6#5-6]" +
            "[(6-7)s:f|_7#6-7]" );
        return fd;
	}
	
    private SpanQuery createQuery(String x, String y, int min, int max, 
    		boolean isOrdered){
    	SpanQuery sq = new SpanDistanceQuery(
        		new SpanTermQuery(new Term("base",x)),
        		new SpanTermQuery(new Term("base",y)),
        		min,
        		max,
        		isOrdered,
        		true
        );    	 
    	return sq;
    }
	
    private SpanQuery createElementQuery(String x, String y, int min, int max, 
    		boolean isOrdered){
    	SpanQuery sq = new SpanDistanceQuery(
        		new SpanElementQuery("base",x),
        		new SpanElementQuery("base",y),
        		min,
        		max,
        		isOrdered,
        		true
        );
    	return sq;
    }
    
    /** One document, multiple occurrences
     * 	The first first and second spans are too far from each other
     * 	One of the spans ends first
     * 	One of the candidate list is empty
     * */
	@Test
	public void testCase1() throws IOException{
		//System.out.println("testcase 1");
		ki = new KorapIndex();
	    ki.addDoc(createFieldDoc0()); 
	    ki.commit();    
	    
	    SpanQuery sq = createQuery("s:c","s:d",0,3,false);                
	    kr = ki.search(sq, (short) 10);
	    	    
	    assertEquals(5, kr.totalResults());
	}
	
	/** Multiple documents 
	 * 	Ensure same doc
	 * 	Both candidate lists are empty, but there is a span left in the doc
	 * 	Both candidate lists are empty, but there are more matches in the doc
	 * @throws IOException 
	 * */
	@Test
	public void testCase2() throws IOException{
		//System.out.println("testcase 2");
		ki = new KorapIndex();
		ki.addDoc(createFieldDoc0());
	    ki.addDoc(createFieldDoc1()); 
	    ki.commit();    
	    
	    SpanQuery sq = createQuery("s:c","s:d",1,2,false);                
	    kr = ki.search(sq, (short) 10);
	    	    
	    assertEquals(6, kr.totalResults());
		
	}
	
	/** Multiple documents 
	 * 	Ensure same Doc
	 * @throws IOException 
	 * */
	@Test
	public void testCase3() throws IOException{		
		//System.out.println("testcase 3");
		ki = new KorapIndex();
		ki.addDoc(createFieldDoc0());
	    ki.addDoc(createFieldDoc1()); 
	    ki.addDoc(createFieldDoc2());
	    ki.commit();    
	    
	    SpanQuery sq = createQuery("s:e","s:f",1,2,false);                
	    kr = ki.search(sq, (short) 10);
	    	    
	    assertEquals(3, kr.totalResults());
	    assertEquals(0, kr.match(0).getLocalDocID());
	    assertEquals(7, kr.match(0).getStartPos());
	    assertEquals(9, kr.match(0).getEndPos());
	    assertEquals(2, kr.match(1).getLocalDocID());
	    assertEquals(0, kr.match(1).getStartPos());
	    assertEquals(3, kr.match(1).getEndPos());
	}
	
	/** Skip to */
	@Test
	public void testCase4() throws IOException{
		//System.out.println("testcase 4");
		ki = new KorapIndex();
		ki.addDoc(createFieldDoc0());
	    ki.addDoc(createFieldDoc1()); 
	    ki.addDoc(createFieldDoc2());
	    ki.commit();
	    
	    SpanQuery sq = new SpanNextQuery(
	    		createQuery("s:d","s:e",1,2,false),
	    		new SpanTermQuery(new Term("base","s:f"))	    		
		);
	    
	    kr = ki.search(sq, (short) 10);
	    assertEquals(2, kr.totalResults());
	    assertEquals(2,kr.getMatch(0).getLocalDocID());
	    assertEquals(2,kr.getMatch(0).getStartPos());
	    assertEquals(6,kr.getMatch(0).getEndPos());
	    assertEquals(3,kr.getMatch(1).getStartPos());
	    assertEquals(6,kr.getMatch(1).getEndPos());
	    
//	    System.out.print(kr.getTotalResults()+"\n");
//			for (int i=0; i< kr.getTotalResults(); i++){
//				System.out.println(
//					kr.match(i).getLocalDocID()+" "+
//					kr.match(i).startPos + " " +
//					kr.match(i).endPos
//			    );
//			}
	}
	
	
	 /** ElementQueries */    
    @Test
    public void testCase5() throws IOException{    	
    	ki = new KorapIndex();
	    ki.addDoc(createFieldDoc0()); 
	    ki.commit();    
	    
	    // Intersection ---- Distance 0:0
	    //System.out.println("Intersection ---- Distance 0:0");
	    SpanQuery sq = createElementQuery("x","y",0,0,false);                
	    kr = ki.search(sq, (short) 10);
    	
	    assertEquals(4, kr.totalResults());
	    assertEquals(2, kr.getMatch(0).startPos);
	    assertEquals(7, kr.getMatch(0).endPos);
	    assertEquals(3, kr.getMatch(1).startPos);
	    assertEquals(7, kr.getMatch(1).endPos);
	    assertEquals(3, kr.getMatch(2).startPos);
	    assertEquals(8, kr.getMatch(2).endPos);
    	
	    // Next to ---- Distance 1:1
	    //System.out.println("Next to ---- Distance 1:1");
	    sq = createElementQuery("x","y",1,1,false);                
	    kr = ki.search(sq, (short) 10);
	    
	    assertEquals(1, kr.totalResults());
	    assertEquals(5, kr.getMatch(0).startPos);
	    assertEquals(10, kr.getMatch(0).endPos);
	    
	    // ---- Distance 1:2
	    //System.out.println("---- Distance 1:2");
	    sq = createElementQuery("x","y",1,2,false);                
	    kr = ki.search(sq, (short) 10);
	    
	    assertEquals(2, kr.totalResults());	    
	    assertEquals(4, kr.getMatch(0).startPos);
	    assertEquals(9, kr.getMatch(0).endPos);
	    assertEquals(5, kr.getMatch(1).startPos);
	    assertEquals(10, kr.getMatch(1).endPos);
	    
	    // The same element type ---- Distance 1:2
	    //System.out.println("The same element type ---- Distance 1:2");

	    sq = createElementQuery("x","x",1,2,false);
	    kr = ki.search(sq, (short) 10);
	    
	    // TO DO: redundant results
	    assertEquals(2, kr.totalResults());
    }
}
