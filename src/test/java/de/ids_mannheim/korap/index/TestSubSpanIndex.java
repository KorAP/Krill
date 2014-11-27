package de.ids_mannheim.korap.index;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.junit.Test;

import de.ids_mannheim.korap.KorapIndex;
import de.ids_mannheim.korap.KorapMatch;
import de.ids_mannheim.korap.KorapResult;
import de.ids_mannheim.korap.query.DistanceConstraint;
import de.ids_mannheim.korap.query.SpanDistanceQuery;
import de.ids_mannheim.korap.query.SpanSubspanQuery;


public class TestSubSpanIndex {
	
	KorapResult kr;
    KorapIndex ki;  
    
    @Test
    public void testCase1() throws IOException {
    	ki = new KorapIndex();
    	ki.addDocFile(
    	        getClass().getResource("/wiki/00001.json.gz").getFile(),true);
		ki.commit();
		
		SpanDistanceQuery sdq = new SpanDistanceQuery(
				new SpanTermQuery(new Term("tokens","tt/p:NN")), 
				new SpanTermQuery(new Term("tokens","tt/p:VAFIN")), 
				new DistanceConstraint(5, 5, true, false), 
				true);
		
		SpanSubspanQuery ssq = new SpanSubspanQuery(sdq, 0, 2, true);		
		kr = ki.search(ssq, (short) 10);
		
		assertEquals((long) 8,kr.getTotalResults());
		assertEquals(35, kr.getMatch(0).getStartPos());
        assertEquals(37, kr.getMatch(0).getEndPos());
        assertEquals(179, kr.getMatch(1).getStartPos());
        assertEquals(181, kr.getMatch(1).getEndPos());
		
		ssq = new SpanSubspanQuery(sdq, -2, 2, true);		
		kr = ki.search(ssq, (short) 10);
		
		assertEquals(39, kr.getMatch(0).getStartPos());
        assertEquals(41, kr.getMatch(0).getEndPos());
        assertEquals(183, kr.getMatch(1).getStartPos());
        assertEquals(185, kr.getMatch(1).getEndPos());
		
		/*for (KorapMatch km : kr.getMatches()){
			System.out.println(km.getStartPos() +","+km.getEndPos()
					+km.getSnippetBrackets());
		}*/
	}
}
