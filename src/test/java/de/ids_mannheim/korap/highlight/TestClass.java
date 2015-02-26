package de.ids_mannheim.korap.highlight;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.apache.lucene.search.spans.SpanQuery;
import org.junit.Test;

import de.ids_mannheim.korap.KorapIndex;
import de.ids_mannheim.korap.response.Match;
import de.ids_mannheim.korap.KrillQuery;
import de.ids_mannheim.korap.KorapResult;
import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.query.SpanNextQuery;
import de.ids_mannheim.korap.query.wrap.SpanQueryWrapper;
import de.ids_mannheim.korap.util.QueryException;


public class TestClass {
	KorapIndex ki;
	KorapResult kr;
	Krill ks;
	
	@Test
	public void queryJSONpoly1() throws QueryException, IOException {
		
		String jsonPath = getClass().getResource("/queries/poly1.json").getFile();
		String jsonQuery = readFile(jsonPath);
		SpanQueryWrapper sqwi = new KrillQuery("tokens").fromJson(
				jsonQuery
		);
		
		SpanNextQuery sq = (SpanNextQuery) sqwi.toQuery();
		//System.out.println(sq.toString());
		
		ki = new KorapIndex();
	    ki.addDocFile(
	    	getClass().getResource("/wiki/JJJ-00785.json.gz").getFile(),true);
	    ki.addDocFile(
		        getClass().getResource("/wiki/DDD-01402.json.gz").getFile(),true);
		ki.commit();
		kr = ki.search(sq, (short) 10);

		assertEquals(61, kr.getMatch(0).getStartPos());
		assertEquals(64, kr.getMatch(0).getEndPos());
		assertEquals("... Bruckner (Wien) und Mathis Lussy (Paris). [{1:Inspiriert} " +
				"{2:durch die}] additiven Modelle arabischer Rhythmik (er half ...", 
				kr.getMatch(0).getSnippetBrackets());
		
		assertEquals(31, kr.getMatch(1).getStartPos());
		assertEquals(34, kr.getMatch(1).getEndPos());
		assertEquals("... des Sendens wird ein unhörbarer Unterton [{1:mitgesendet}, " +
				"{2:auf den}] das angesprochene Funkgerät reagiert. Die Abkürzung ...", 
				kr.getMatch(1).getSnippetBrackets());
	}

    @Test
	public void queryJSONpoly4() throws QueryException, IOException {
		
		String jsonPath = getClass().getResource("/queries/poly4.json").getFile();
		String jsonQuery = readFile(jsonPath);
		SpanQueryWrapper sqwi = new KrillQuery("tokens").fromJson(
				jsonQuery
		);
		SpanQuery sq = sqwi.toQuery();
		
		// System.out.println(sq.toString());
		

		ki = new KorapIndex();
	    ki.addDocFile(
	    	getClass().getResource("/wiki/SSS-09803.json.gz").getFile(),true);
	    
	    ki.commit();
		kr = ki.search(sq, (short) 10);
				
		/*
		for (Match km : kr.getMatches()){
			System.out.println(km.getStartPos() +","+km.getEndPos()+" "
					+km.getSnippetBrackets()
			);
		}
		*/
		assertEquals((long) 495, kr.getTotalResults());
		assertEquals(3, kr.getMatch(0).getStartPos());
		assertEquals(5, kr.getMatch(0).getEndPos());
		
		//fail("Tests have to be updated");
	}
	
	
	private String readFile(String path) {
		StringBuilder sb = new StringBuilder();
		try {
		    BufferedReader in = new BufferedReader(new FileReader(path));
		    String str;
		    while ((str = in.readLine()) != null) {
		    	sb.append(str);
		    };
		    in.close();
		} catch (IOException e) {
		    fail(e.getMessage());
		}
		return sb.toString();
	}
}
