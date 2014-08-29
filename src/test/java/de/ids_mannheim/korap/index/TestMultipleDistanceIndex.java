package de.ids_mannheim.korap.index;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
import de.ids_mannheim.korap.query.SpanMultipleDistanceQuery;
import de.ids_mannheim.korap.query.SpanNextQuery;

@RunWith(JUnit4.class)
public class TestMultipleDistanceIndex {
	
	private KorapIndex ki;
	private KorapResult kr;

	public SpanQuery createQuery(String x, String y, List<DistanceConstraint> 
			constraints, boolean isOrdered){
		
		SpanQuery sx = new SpanTermQuery(new Term("base",x)); 
		SpanQuery sy = new SpanTermQuery(new Term("base",y));
		
		return new SpanMultipleDistanceQuery(sx, sy, constraints, isOrdered, true);
	}
	
	public DistanceConstraint createConstraint(String unit, int min, int max, 
			boolean isOrdered, boolean exclusion){
		
		if (unit.equals("w")){
			return new DistanceConstraint(min, max,isOrdered,exclusion);
		}		
		return new DistanceConstraint(new SpanElementQuery("base", unit), 
				min, max, isOrdered, exclusion);	
	}
	
    private FieldDocument createFieldDoc0() {
    	FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-0");
        fd.addTV("base",
            "text",             
            "[(0-1)s:b|_1#0-1|<>:s#0-2$<i>2|<>:p#0-4$<i>4]" +
            "[(1-2)s:b|s:c|_2#1-2]" +             
            "[(2-3)s:c|_3#2-3|<>:s#2-3$<i>4]" +
            "[(3-4)s:b|_4#3-4]" + 
            "[(4-5)s:c|_5#4-5|<>:s#4-6$<i>6|<>:p#4-6$<i>6]" +             
            "[(5-6)s:e|_6#5-6]");
        return fd;
	}
    
    private FieldDocument createFieldDoc1() {
    	FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-1");
        fd.addTV("base",
            "text",             
            "[(0-1)s:c|_1#0-1|<>:s#0-2$<i>2|<>:p#0-4$<i>4]" +
            "[(1-2)s:c|s:e|_2#1-2]" +             
            "[(2-3)s:e|_3#2-3|<>:s#2-3$<i>4]" +
            "[(3-4)s:c|_4#3-4]" + 
            "[(4-5)s:e|_5#4-5|<>:s#4-6$<i>6|<>:p#4-6$<i>6]" +             
            "[(5-6)s:c|_6#5-6]");
        return fd;
    }   
    
    private FieldDocument createFieldDoc2() {
    	FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-2");
        fd.addTV("base",
            "text",             
            "[(0-1)s:b|_1#0-1|<>:s#0-2$<i>2|<>:p#0-4$<i>4]" +
            "[(1-2)s:b|s:e|_2#1-2]" +             
            "[(2-3)s:e|_3#2-3|<>:s#2-3$<i>4]" +
            "[(3-4)s:b|s:c|_4#3-4]" + 
            "[(4-5)s:e|_5#4-5|<>:s#4-6$<i>6|<>:p#4-6$<i>6]" +             
            "[(5-6)s:d|_6#5-6]" +
        	"[(6-7)s:b|_7#6-7|<>:s#6-7$<i>7|<>:p#6-7$<i>7]" );
        return fd;
	}
    
    private FieldDocument createFieldDoc3() {
    	FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-0");
        fd.addTV("base",
            "text",             
            "[(0-1)s:b|_1#0-1|<>:s#0-2$<i>2|<>:p#0-4$<i>4]" +
            "[(1-2)s:b|s:c|_2#1-2]" +             
            "[(2-3)s:c|_3#2-3|<>:s#2-3$<i>5]" +
            "[(3-4)s:b|_4#3-4]" +
            "[(4-5)s:b|_5#4-5]" + 
            "[(5-6)s:b|_6#5-6]" + // gap
            "[(6-7)s:c|_7#6-7|<>:s#6-7$<i>7|<>:p#6-7$<i>7]" );
        return fd;
	}
    
    /** Unordered, same sentence
     * */
    @Test
	public void testCase1() throws IOException {
    	ki = new KorapIndex();
        ki.addDoc(createFieldDoc0()); 
        ki.commit();
        
    	List<DistanceConstraint> constraints = new ArrayList<DistanceConstraint>();
 	    constraints.add(createConstraint("w", 0, 2, false, false));	    
 	    constraints.add(createConstraint("s", 0, 0, false, false));
 	    
 	    SpanQuery mdq;	   
		mdq = createQuery("s:b", "s:c", constraints,false);
		kr = ki.search(mdq, (short) 10);
		// System.out.println(mdq);
		
		assertEquals(3, kr.getTotalResults());
	    assertEquals(0, kr.getMatch(0).getStartPos());
	    assertEquals(2, kr.getMatch(0).getEndPos());
	    assertEquals(1, kr.getMatch(1).getStartPos());
	    assertEquals(2, kr.getMatch(1).getEndPos());
	    assertEquals(2, kr.getMatch(2).getStartPos());
	    assertEquals(4, kr.getMatch(2).getEndPos());
    }
	
    /** Ordered
     * 	Unordered
     * 	Two constraints
     * 	Three constraints 	
     * */
	@Test
	public void testCase2() throws IOException {
		ki = new KorapIndex();
        ki.addDoc(createFieldDoc0()); 
        ki.commit();
		
        // Ordered - two constraints
	    List<DistanceConstraint> constraints = new ArrayList<DistanceConstraint>();
	    constraints.add(createConstraint("w", 0, 2, true, false));	    
	    constraints.add(createConstraint("s", 1, 1, true, false));
	    
	    SpanQuery mdq;	    
		mdq = createQuery("s:b", "s:c", constraints,true);
		kr = ki.search(mdq, (short) 10);		
	    assertEquals(3, kr.getTotalResults());
	    assertEquals(0, kr.getMatch(0).getStartPos());
	    assertEquals(3, kr.getMatch(0).getEndPos());
	    assertEquals(1, kr.getMatch(1).getStartPos());
	    assertEquals(3, kr.getMatch(1).getEndPos());
	    assertEquals(3, kr.getMatch(2).getStartPos());
	    assertEquals(5, kr.getMatch(2).getEndPos());	   
	   				
		// Three constraints
		constraints.add(createConstraint("p", 0, 0, true, false));		
		mdq = createQuery("s:b", "s:c", constraints,true);
		kr = ki.search(mdq, (short) 10);
		assertEquals(2, kr.getTotalResults());
		
		
		// Unordered - two constraints
		constraints.clear();
	    constraints.add(createConstraint("w", 0, 2, false, false));	    
	    constraints.add(createConstraint("s", 1, 1, false, false));
	    	   
	    mdq = createQuery("s:c", "s:b", constraints,false);
	    kr = ki.search(mdq, (short) 10);
	    assertEquals(4, kr.getTotalResults());
	    assertEquals(1, kr.getMatch(2).getStartPos());
	    assertEquals(4, kr.getMatch(2).getEndPos());
		
	    // Three constraints
		constraints.add(createConstraint("p", 0, 0, false, false));
		mdq = createQuery("s:b", "s:c", constraints,false);
		kr = ki.search(mdq, (short) 10);		
		assertEquals(3, kr.getTotalResults());	

	}
    
    /** Multiple documents
     * 	Ensure same doc (inner term span)
     * */
    @Test
   	public void testCase3() throws IOException {
    	ki = new KorapIndex();
    	ki.addDoc(createFieldDoc0()); 
    	ki.addDoc(createFieldDoc1());
    	ki.addDoc(createFieldDoc2());
    	ki.commit();
           
       	List<DistanceConstraint> constraints = new ArrayList<DistanceConstraint>();
	    constraints.add(createConstraint("w", 1, 2, false, false));	    
	    constraints.add(createConstraint("s", 1, 2, false, false));
    	    
	    SpanQuery mdq;	   
		mdq = createQuery("s:b", "s:e", constraints,false);
		kr = ki.search(mdq, (short) 10);		

		assertEquals(5, kr.getTotalResults());		
	    assertEquals(3, kr.getMatch(0).getStartPos());
	    assertEquals(6, kr.getMatch(0).getEndPos());
	    assertEquals(2, kr.getMatch(1).getLocalDocID());
	    assertEquals(1, kr.getMatch(2).getStartPos());
	    assertEquals(4, kr.getMatch(2).getEndPos());
	    assertEquals(3, kr.getMatch(3).getStartPos());
	    assertEquals(5, kr.getMatch(3).getEndPos());	
	    assertEquals(4, kr.getMatch(4).getStartPos());
	    assertEquals(7, kr.getMatch(4).getEndPos());
	    
//	    System.out.print(kr.getTotalResults()+"\n");
//		for (int i=0; i< kr.getTotalResults(); i++){
//			System.out.println(
//				kr.match(i).getLocalDocID()+" "+
//				kr.match(i).startPos + " " +
//				kr.match(i).endPos
//		    );
//		}
		
    }
    
	/**	Skip to
     * */
    @Test
   	public void testCase4() throws IOException {
    	ki = new KorapIndex();
    	ki.addDoc(createFieldDoc0());
    	ki.addDoc(createFieldDoc3());
    	ki.addDoc(createFieldDoc1());
    	ki.addDoc(createFieldDoc2());
    	ki.commit();
    	
    	List<DistanceConstraint> constraints = new ArrayList<DistanceConstraint>();
	    constraints.add(createConstraint("w", 1, 2, false, false));	    
	    constraints.add(createConstraint("s", 1, 2, false, false));
		
	    SpanQuery mdq;	   
		mdq = createQuery("s:b", "s:c", constraints,false);
		
		SpanQuery sq = new SpanNextQuery(mdq, 
				new SpanTermQuery(new Term("base","s:e")));
		kr = ki.search(sq, (short) 10);
		
		assertEquals(2, kr.getTotalResults());
	    assertEquals(3, kr.getMatch(0).getStartPos());
	    assertEquals(6, kr.getMatch(0).getEndPos());
	    assertEquals(3, kr.getMatch(1).getLocalDocID());
	    assertEquals(1, kr.getMatch(1).getStartPos());
	    assertEquals(5, kr.getMatch(1).getEndPos());

    }
    
    /** Same tokens: ordered and unordered yield the same results
	 * */
    @Test
   	public void testCase5() throws IOException {
    	ki = new KorapIndex();
    	ki.addDoc(createFieldDoc0());    	
    	ki.addDoc(createFieldDoc1());    	
    	ki.commit();
    	
    	List<DistanceConstraint> constraints = new ArrayList<DistanceConstraint>();
	    constraints.add(createConstraint("w", 1, 2, false, false));	    
	    constraints.add(createConstraint("s", 1, 2, false, false));
		
	    SpanQuery mdq;	   
		mdq = createQuery("s:c", "s:c", constraints,false);
		kr = ki.search(mdq, (short) 10);
		
		assertEquals(4, kr.getTotalResults());
	    assertEquals(1, kr.getMatch(0).getStartPos());
	    assertEquals(3, kr.getMatch(0).getEndPos());
	    assertEquals(2, kr.getMatch(1).getStartPos());
	    assertEquals(5, kr.getMatch(1).getEndPos());
	    assertEquals(1, kr.getMatch(2).getLocalDocID());
	    assertEquals(1, kr.getMatch(2).getStartPos());
	    assertEquals(4, kr.getMatch(2).getEndPos());
	    assertEquals(3, kr.getMatch(3).getStartPos());
	    assertEquals(6, kr.getMatch(3).getEndPos());	

    }
	
    /** Exclusion
     * 	Gaps
     * */
    @Test
   	public void testCase6() throws IOException {
    	ki = new KorapIndex();
    	ki.addDoc(createFieldDoc3());    	
    	ki.commit();
    	
    	// First constraint - token exclusion
    	SpanQuery sx = new SpanTermQuery(new Term("base","s:b")); 
		SpanQuery sy = new SpanTermQuery(new Term("base","s:c"));
		
		DistanceConstraint dc1 = createConstraint("w", 0, 1, false, true);
		SpanDistanceQuery sq = new SpanDistanceQuery(sx, sy, dc1, true);
		
		kr = ki.search(sq, (short) 10);
		assertEquals(1, kr.getTotalResults());
		// 4-5
		
    	// Second constraint - element distance
		DistanceConstraint  dc2 = createConstraint("s", 1, 1, false, false);
		sq = new SpanDistanceQuery(sx, sy, dc2, true);		
		kr = ki.search(sq, (short) 10);		
		// 0-3, 1-3, 1-4, 1-5, 3-7, 4-7
		assertEquals(6, kr.getTotalResults());
		
		
    	List<DistanceConstraint> constraints = new ArrayList<DistanceConstraint>();
	    constraints.add(dc1);
	    constraints.add(dc2);
	    	    
	    SpanQuery mdq;	   
		mdq = createQuery("s:b", "s:c", constraints,false);
		kr = ki.search(mdq, (short) 10);
		
		assertEquals(2, kr.getTotalResults());
	    assertEquals(1, kr.getMatch(0).getStartPos());
	    assertEquals(5, kr.getMatch(0).getEndPos());
	    assertEquals(4, kr.getMatch(1).getStartPos());
	    assertEquals(7, kr.getMatch(1).getEndPos());
    }
    
    
    /** Exclusion, multiple documents
     * */
    @Test
    public void testCase7() throws IOException {
    	ki = new KorapIndex();   	
    	ki.addDoc(createFieldDoc2());    	
    	ki.commit();
    	
    	SpanQuery sx = new SpanTermQuery(new Term("base","s:b")); 
		SpanQuery sy = new SpanTermQuery(new Term("base","s:c"));
    	// Second constraint
		SpanDistanceQuery sq = new SpanDistanceQuery(sx,sy, 
				createConstraint("s", 0, 0, false, true),
				true);
		kr = ki.search(sq, (short) 10);
		assertEquals(3, kr.getTotalResults());
    	// 0-1, 1-2, 6-7
				
	    // Exclusion within the same sentence
    	List<DistanceConstraint> constraints = new ArrayList<DistanceConstraint>();
	    constraints.add(createConstraint("w", 0, 2,false,true));
	    constraints.add(createConstraint("s", 0, 0,false,true));

	    SpanQuery mdq;	   
		mdq = createQuery("s:b", "s:c", constraints,false);
		kr = ki.search(mdq, (short) 10);		
		assertEquals(2, kr.getTotalResults());
	    assertEquals(0, kr.getMatch(0).getStartPos());
	    assertEquals(1, kr.getMatch(0).getEndPos());
	    assertEquals(6, kr.getMatch(1).getStartPos());
	    assertEquals(7, kr.getMatch(1).getEndPos());
		
	    
	    // Third constraint
	    sq = new SpanDistanceQuery(sx, sy, 
				createConstraint("p", 0, 0, false, true), 
				true);
	    kr = ki.search(sq, (short) 10);
		assertEquals(1, kr.getTotalResults());
		// 6-7
	    
		constraints.add(createConstraint("p", 0, 0, false, true));
	    mdq = createQuery("s:b", "s:c", constraints,false);
		kr = ki.search(mdq, (short) 10);			
		
	    assertEquals(1, kr.getTotalResults());
	    assertEquals(6, kr.getMatch(0).getStartPos());
	    assertEquals(7, kr.getMatch(0).getEndPos());

	}
}

