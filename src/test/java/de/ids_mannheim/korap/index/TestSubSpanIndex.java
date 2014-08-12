package de.ids_mannheim.korap.index;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.junit.Test;

import de.ids_mannheim.korap.KorapIndex;
import de.ids_mannheim.korap.KorapResult;
import de.ids_mannheim.korap.query.DistanceConstraint;
import de.ids_mannheim.korap.query.SpanDistanceQuery;
import de.ids_mannheim.korap.query.SubspanQuery;


public class TestSubSpanIndex {
	
	KorapResult kr;
    KorapIndex ki;  
    
    @Test
    public void testCase1() throws IOException {
    	ki = new KorapIndex();
		for (String i : new String[] {"AAA-12402"}) {
		    ki.addDocFile(
		        getClass().getResource("/wiki/" + i + ".json.gz").getFile(), true
	            );
		};
		ki.commit();
		
		SpanDistanceQuery sdq = new SpanDistanceQuery(
				new SpanTermQuery(new Term("tokens","cnx/p:N")), 
				new SpanTermQuery(new Term("tokens","cnx/p:V")), 
				new DistanceConstraint(5, 5, true, false), 
				true);
		
		SubspanQuery ssq = new SubspanQuery(sdq, 0, 2, true);		
		kr = ki.search(ssq, (short) 10);
		
		assertEquals(27,kr.getTotalResults());
		assertEquals(30, kr.getMatch(0).getStartPos());
        assertEquals(32, kr.getMatch(0).getEndPos());
        assertEquals(81, kr.getMatch(1).getStartPos());
        assertEquals(83, kr.getMatch(1).getEndPos());
		
		/*for (KorapMatch km : kr.getMatches()){
			System.out.println(km.getStartPos() +","+km.getEndPos()
					+km.getSnippetBrackets());
		}*/
		
		ssq = new SubspanQuery(sdq, -2, 2, true);		
		kr = ki.search(ssq, (short) 10);
		
		assertEquals(34, kr.getMatch(0).getStartPos());
        assertEquals(35, kr.getMatch(0).getEndPos());
        assertEquals(85, kr.getMatch(1).getStartPos());
        assertEquals(87, kr.getMatch(1).getEndPos());
	}
}
