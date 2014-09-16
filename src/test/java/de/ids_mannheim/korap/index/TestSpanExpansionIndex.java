package de.ids_mannheim.korap.index;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.junit.Test;

import de.ids_mannheim.korap.KorapIndex;
import de.ids_mannheim.korap.KorapMatch;
import de.ids_mannheim.korap.KorapResult;
import de.ids_mannheim.korap.query.SpanElementQuery;
import de.ids_mannheim.korap.query.SpanExpansionQuery;

public class TestSpanExpansionIndex {

	KorapResult kr;
    KorapIndex ki;
    
    public TestSpanExpansionIndex() throws IOException {
    	ki = new KorapIndex();
		for (String i : new String[] {"AAA-12402"}) {
		    ki.addDocFile(
		        getClass().getResource("/wiki/" + i + ".json.gz").getFile(),true);
		};
		ki.commit();
	}
    
    /** Left and right expansions
     * */
	@Test
	public void testCase1() throws IOException {
		
		SpanTermQuery stq = new SpanTermQuery(new Term("tokens","s:Kaiser")	);
		SpanExpansionQuery seq = new SpanExpansionQuery(stq, 0, 2, -1, true);		
		kr = ki.search(seq, (short) 10);
		
		assertEquals(72,kr.getTotalResults());
		assertEquals(5, kr.getMatch(0).getStartPos());
        assertEquals(8, kr.getMatch(0).getEndPos());
        assertEquals(6, kr.getMatch(1).getStartPos());
        assertEquals(8, kr.getMatch(1).getEndPos());
        assertEquals(7, kr.getMatch(2).getStartPos());
        assertEquals(8, kr.getMatch(2).getEndPos());
        
        seq = new SpanExpansionQuery(stq, 3, 4, 0, true);
        kr = ki.search(seq, (short) 10);
        
        assertEquals(7, kr.getMatch(0).getStartPos());
        assertEquals(11, kr.getMatch(0).getEndPos());
        assertEquals(7, kr.getMatch(1).getStartPos());
        assertEquals(12, kr.getMatch(1).getEndPos());
        assertEquals(15, kr.getMatch(2).getStartPos());
        assertEquals(19, kr.getMatch(2).getEndPos());
        assertEquals(15, kr.getMatch(3).getStartPos());
        assertEquals(20, kr.getMatch(3).getEndPos());
        
		/*for (KorapMatch km : kr.getMatches()){
			System.out.println(km.getStartPos() +","+km.getEndPos()+" "
					+km.getSnippetBrackets());
		}	
		*/
	}
	
	/** Classnumber
	 * 	Cannot check the expansion offset correctness directly
	 * */
	@Test
	public void testCase2() {
		byte classNumber = 1;
		SpanExpansionQuery sq;
		// create new payload for the expansion offsets
		SpanTermQuery stq = new SpanTermQuery(new Term("tokens","s:Kaiser")	);
		sq = new SpanExpansionQuery(stq, 0, 2, -1, classNumber, true);		
		kr = ki.search(sq, (short) 10);
		
		assertEquals(5, kr.getMatch(0).getStartPos());	// expansion 5,7
        assertEquals(8, kr.getMatch(0).getEndPos());	
        assertEquals(6, kr.getMatch(1).getStartPos());	// expansion 6,9
        assertEquals(8, kr.getMatch(1).getEndPos());        
        assertEquals(7, kr.getMatch(2).getStartPos()); 	// expansion 7,7
        assertEquals(8, kr.getMatch(2).getEndPos());	
        assertEquals(13, kr.getMatch(3).getStartPos());	// expansion 13,15
        assertEquals(16, kr.getMatch(3).getEndPos());	 
		
		/*for (KorapMatch km : kr.getMatches()){		
			System.out.println(km.getStartPos() +","+km.getEndPos()+" "
				+km.getSnippetBrackets());
		}*/
		
		// add expansion offsets to the existing payload
		SpanElementQuery seq = new SpanElementQuery("tokens", "cnx/c:np");
		sq = new SpanExpansionQuery(seq, 1, 2, 0, classNumber, true);		
		kr = ki.search(sq, (short) 10);
		
		assertEquals(1, kr.getMatch(0).getStartPos());
        assertEquals(3, kr.getMatch(0).getEndPos());
        assertEquals(1, kr.getMatch(1).getStartPos());
        assertEquals(4, kr.getMatch(1).getEndPos());        
        assertEquals(6, kr.getMatch(2).getStartPos()); 	// expansion 8,9
        assertEquals(9, kr.getMatch(2).getEndPos());	
        assertEquals(6, kr.getMatch(3).getStartPos());	// expansion 8,10
        assertEquals(10, kr.getMatch(3).getEndPos());	 
        
        
		/*for (KorapMatch km : kr.getMatches()){		
			System.out.println(km.getStartPos() +","+km.getEndPos()+" "
				+km.getSnippetBrackets());
		}*/
	}
	
	
	/** Right expansion with exclusion
	 * */
	@Test
	public void testCase3() throws IOException {
		byte classNumber = 1;
		SpanTermQuery stq = new SpanTermQuery(new Term("tokens","cnx/p:N")	);
		SpanTermQuery notQuery = new SpanTermQuery(new Term("tokens","s:September"));
		
		SpanExpansionQuery seq = new SpanExpansionQuery(stq, notQuery, 2, 3, 0, 
				classNumber, true);		
		kr = ki.search(seq, (short) 20);
		
		assertEquals(13, kr.getMatch(11).getStartPos());	// expansion 14,17
        assertEquals(17, kr.getMatch(11).getEndPos());	 
		assertEquals(18, kr.getMatch(12).getStartPos());	// expansion 19,21
        assertEquals(21, kr.getMatch(12).getEndPos());
        assertEquals(18, kr.getMatch(13).getStartPos());	// expansion 19,22
        assertEquals(22, kr.getMatch(13).getEndPos());        
        assertEquals(20, kr.getMatch(14).getStartPos()); 	// expansion 21,23
        assertEquals(23, kr.getMatch(14).getEndPos());	
        		
		/*for (KorapMatch km : kr.getMatches()){
			System.out.println(km.getStartPos() +","+km.getEndPos()+" "
					+km.getSnippetBrackets());
		}*/
	}
	
	/** Left expansion with exclusion
	 * */
	@Test
	public void testCase4() throws IOException {
		byte classNumber = 1;
		SpanTermQuery stq = new SpanTermQuery(new Term("tokens","cnx/p:N")	);
		SpanTermQuery notQuery = new SpanTermQuery(new Term("tokens","cnx/p:A"));
		
		SpanExpansionQuery seq = new SpanExpansionQuery(stq, notQuery, 0, 2, -1, 
				classNumber, true);		
		kr = ki.search(seq, (short) 10);

		assertEquals(7, kr.getMatch(3).getStartPos());	// expansion 7,7
        assertEquals(8, kr.getMatch(3).getEndPos());
        assertEquals(7, kr.getMatch(4).getStartPos());	// expansion 7,8
        assertEquals(9, kr.getMatch(4).getEndPos());        
        assertEquals(8, kr.getMatch(5).getStartPos()); 	// expansion 8,8 // no expansion // no need???
        assertEquals(9, kr.getMatch(5).getEndPos());	
        assertEquals(8, kr.getMatch(6).getStartPos());	// expansion 8,10
        assertEquals(11, kr.getMatch(6).getEndPos());	 
		
		
		for (KorapMatch km : kr.getMatches()){
			System.out.println(km.getStartPos() +","+km.getEndPos()+" "
					+km.getSnippetBrackets());
		}
		
	}
	
}
