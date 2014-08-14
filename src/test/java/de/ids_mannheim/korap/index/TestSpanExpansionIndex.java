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
		SpanExpansionQuery seq = new SpanExpansionQuery(stq, 0, 2, true, true);		
		kr = ki.search(seq, (short) 10);
		
		assertEquals(72,kr.getTotalResults());
		assertEquals(5, kr.getMatch(0).getStartPos());
        assertEquals(8, kr.getMatch(0).getEndPos());
        assertEquals(6, kr.getMatch(1).getStartPos());
        assertEquals(8, kr.getMatch(1).getEndPos());
        assertEquals(7, kr.getMatch(2).getStartPos());
        assertEquals(8, kr.getMatch(2).getEndPos());
        
        seq = new SpanExpansionQuery(stq, 3, 4, false, true);
        kr = ki.search(seq, (short) 10);
        
        assertEquals(7, kr.getMatch(0).getStartPos());
        assertEquals(11, kr.getMatch(0).getEndPos());
        assertEquals(7, kr.getMatch(1).getStartPos());
        assertEquals(12, kr.getMatch(1).getEndPos());
        assertEquals(15, kr.getMatch(2).getStartPos());
        assertEquals(19, kr.getMatch(2).getEndPos());
        assertEquals(15, kr.getMatch(3).getStartPos());
        assertEquals(20, kr.getMatch(3).getEndPos());
        
//		for (KorapMatch km : kr.getMatches()){
//			System.out.println(km.getStartPos() +","+km.getEndPos()+" "
//					+km.getSnippetBrackets());
//		}	
		
	}
	
	/** Classnumber
	 * 	Cannot check the correctness directly
	 * */
	@Test
	public void testCase2() {
		byte classNumber = 1;
		
		// create new payload for the expansion offsets
		SpanTermQuery stq = new SpanTermQuery(new Term("tokens","s:Kaiser")	);
		SpanExpansionQuery sq = new SpanExpansionQuery(stq, 0, 2, classNumber, true, true);		
		kr = ki.search(sq, (short) 10);
				
		// add expansion offsets to the existing payload
		SpanElementQuery seq = new SpanElementQuery("tokens", "cnx/c:np");
		sq = new SpanExpansionQuery(seq, 1, 2, classNumber, false, true);		
		kr = ki.search(sq, (short) 10);
		
		/*for (KorapMatch km : kr.getMatches()){		
			System.out.println(km.getStartPos() +","+km.getEndPos()+" "
				+km.getSnippetBrackets());
		}*/
	}
	
}
