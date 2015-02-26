package de.ids_mannheim.korap.index;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.store.MMapDirectory;
import org.junit.Test;

import de.ids_mannheim.korap.KorapCollection;
import de.ids_mannheim.korap.KorapIndex;
import de.ids_mannheim.korap.response.Match;
import de.ids_mannheim.korap.KorapResult;
import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.collection.BooleanFilter;
import de.ids_mannheim.korap.query.DistanceConstraint;
import de.ids_mannheim.korap.query.SpanDistanceQuery;
import de.ids_mannheim.korap.query.SpanElementQuery;
import de.ids_mannheim.korap.query.SpanNextQuery;
import de.ids_mannheim.korap.query.SpanRepetitionQuery;

public class TestWPDIndex {
	long start, end;
	KorapIndex ki;
	KorapResult kr;
	Krill ks;
	
	private SpanDistanceQuery createElementDistanceQuery(String e, String x, String y, 
			int min, int max, boolean isOrdered, boolean exclusion){
		SpanElementQuery eq = new SpanElementQuery("tokens", e);
		SpanDistanceQuery sq = new SpanDistanceQuery(
				new SpanTermQuery(new Term("tokens",x)),
				new SpanTermQuery(new Term("tokens",y)),
				new DistanceConstraint(eq, min, max, isOrdered, exclusion),
        		true
        ); 
		return sq;
	}
	
	private SpanDistanceQuery createDistanceQuery(String x, String y, int min, int max, 
			boolean isOrdered, boolean exclusion){
		SpanDistanceQuery sq = new SpanDistanceQuery(
        		new SpanTermQuery(new Term("tokens",x)),
        		new SpanTermQuery(new Term("tokens",y)),
        		new DistanceConstraint(min, max, isOrdered, exclusion),
        		true
        );
    	return sq;
    }
	
	public TestWPDIndex() throws IOException {
		InputStream is = getClass().getResourceAsStream("/korap.conf");
		Properties prop = new Properties();
		prop.load(is);
		
		String indexPath = prop.getProperty("lucene.indexDir");
		MMapDirectory md = new MMapDirectory(new File(indexPath));
		ki = new KorapIndex(md);
	}
	
	/** Token distance spans */
	@Test
	public void testCase1() throws IOException{
		SpanDistanceQuery sq;
		// ordered
		sq = createDistanceQuery("s:Wir", "s:kommen", 1, 1, true,false);
		ks = new Krill(sq);
		kr = ks.apply(ki);
		assertEquals(kr.getTotalResults(), 8);

		// unordered
		sq = createDistanceQuery("s:Wir", "s:kommen", 1, 1, false,false);				
		ks = new Krill(sq);
		kr = ks.apply(ki);
		assertEquals(kr.getTotalResults(), 11);
		
		sq = createDistanceQuery("s:kommen", "s:Wir", 1, 1, false,false);				
		ks = new Krill(sq);
		kr = ks.apply(ki);
		assertEquals(kr.getTotalResults(), 11);
		//System.out.println(kr.getTotalResults());
		//for (Match km : kr.getMatches()){
			//System.out.println(km.getDocID() +" "+km.getStartPos() +" "+ km.getEndPos());
        	//System.out.println(km.getSnippetBrackets());
        	//System.out.println(km.toJSON());
        //}	
	}
	
	/** Token exclusion distance spans */
	@Test
	public void testCase2() throws IOException{

		SpanQuery q = new SpanTermQuery(new Term("tokens","s:Wir"));
		ks = new Krill(q);
		kr = ks.apply(ki);
		assertEquals(kr.getTotalResults(), 1907);
		
		SpanDistanceQuery sq;
		// ordered
		sq = createDistanceQuery("s:Wir", "s:kommen", 1, 1, true, true);				
		ks = new Krill(sq);
		kr = ks.apply(ki);
		assertEquals(kr.getTotalResults(), 1899);
		
		// unordered
		sq = createDistanceQuery("s:Wir", "s:kommen", 1, 1, false, true);				
		ks = new Krill(sq);
		kr = ks.apply(ki);
		assertEquals(kr.getTotalResults(), 1896);	
	}
	
	/** Element distance spans */
	@Test
	public void testCase3() throws IOException{
		// ordered
		SpanDistanceQuery sq = createElementDistanceQuery("s","s:weg", "s:fahren", 
				0, 1, true, false);
		ks = new Krill(sq);
		kr = ks.apply(ki);		
		assertEquals(kr.getTotalResults(), 3);
		
		// unordered
		sq = createElementDistanceQuery("s","s:weg", "s:fahren", 0, 1, false,false);
		ks = new Krill(sq);
		kr = ks.apply(ki);
		assertEquals(kr.getTotalResults(), 5);
		
		// only 0
		sq = createElementDistanceQuery("s","s:weg", "s:fahren", 0, 0, false,false);
		kr = ki.search(sq, (short) 100);
		assertEquals(kr.getTotalResults(), 2);
		assertEquals("WPD_BBB.04463", kr.getMatch(0).getDocID());
		assertEquals(1094,kr.getMatch(0).getStartPos());
		assertEquals(1115,kr.getMatch(0).getEndPos());
		assertEquals("WPD_III.00758", kr.getMatch(1).getDocID());
		assertEquals(444,kr.getMatch(1).getStartPos());
		assertEquals(451,kr.getMatch(1).getEndPos());
		
		// only 1
		sq = createElementDistanceQuery("s","s:weg", "s:fahren", 1, 1, false,false);
		ks = new Krill(sq);
		kr = ks.apply(ki);
		assertEquals(kr.getTotalResults(), 3);		
	}
	
	/** Element distance exclusion */
	@Test
	public void testCase4() throws IOException{
		SpanDistanceQuery sq = createElementDistanceQuery("s","s:weg", "s:fahren", 1, 1, false, true);
		ks = new Krill(sq);
		kr = ks.apply(ki);
		assertEquals(kr.getTotalResults(), 979);
		//0.8s
		
		// Check if it includes some results
		BooleanFilter bf = new BooleanFilter();
		bf.or("ID", "WPD_BBB.04463", "WPD_III.00758");
		KorapCollection kc = new KorapCollection();	
		kc.filter(bf);
		ks.setCollection(kc);
		kr = ks.apply(ki);		
		assertEquals(1094,kr.getMatch(0).getStartPos());
		assertEquals(451,kr.getMatch(1).getEndPos());		
	}
			
	/** Repetition */
	@Test
	public void testCase5() throws IOException{
		SpanQuery sq;
		sq = new SpanRepetitionQuery(new SpanTermQuery(new Term("tokens","mate/p:ADJA")),1,2, true);
		ks = new Krill(sq);
		kr = ks.apply(ki);
		assertEquals(kr.getTotalResults(), 4116416);
		//0.9s
		
		sq = new SpanRepetitionQuery(new SpanTermQuery(new Term("tokens","mate/p:ADJA")),1,1, true);
		ks = new Krill(sq);
		kr = ks.apply(ki);
		assertEquals(kr.getTotalResults(), 3879671);
		
		sq = new SpanRepetitionQuery(new SpanTermQuery(new Term("tokens","mate/p:ADJA")),2,2, true);
		ks = new Krill(sq);
		kr = ks.apply(ki);
		assertEquals(kr.getTotalResults(), 236745);
		//0.65s
	}
	
	/** Next and repetition */
	@Test
	public void testCase6() throws IOException{
		SpanQuery sq = new SpanNextQuery(
        		new SpanTermQuery(new Term("tokens", "tt/p:NN")),
        		new SpanRepetitionQuery(new SpanTermQuery(new Term("tokens","mate/p:ADJA")),2,2, true)
    		);
		ks = new Krill(sq);
		kr = ks.apply(ki);
		assertEquals(kr.getTotalResults(), 30223);
		// 1.1s
		
		SpanQuery sq2 = new SpanNextQuery(sq,
				new SpanTermQuery(new Term("tokens", "tt/p:NN")));
		ks = new Krill(sq2);
		kr = ks.apply(ki);
		assertEquals(kr.getTotalResults(), 26607);
		// 1.1s
	}
}
