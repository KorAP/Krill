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
import de.ids_mannheim.korap.KorapMatch;
import de.ids_mannheim.korap.KorapResult;
import de.ids_mannheim.korap.KorapSearch;
import de.ids_mannheim.korap.filter.BooleanFilter;
import de.ids_mannheim.korap.query.DistanceConstraint;
import de.ids_mannheim.korap.query.SpanDistanceQuery;
import de.ids_mannheim.korap.query.SpanElementQuery;
import de.ids_mannheim.korap.query.SpanNextQuery;
import de.ids_mannheim.korap.query.SpanRepetitionQuery;

public class TestWPDIndex {
	long start, end;
	KorapIndex ki;
	KorapResult kr;
	KorapSearch ks;
	
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
		ks = new KorapSearch(sq);
		kr = ks.run(ki);
		assertEquals(8, kr.getTotalResults());

		// unordered
		sq = createDistanceQuery("s:Wir", "s:kommen", 1, 1, false,false);				
		ks = new KorapSearch(sq);
		kr = ks.run(ki);
		assertEquals(11, kr.getTotalResults());
		
		sq = createDistanceQuery("s:kommen", "s:Wir", 1, 1, false,false);				
		ks = new KorapSearch(sq);
		kr = ks.run(ki);
		assertEquals(11, kr.getTotalResults());
	}
	
	/** Token exclusion distance spans */
	@Test
	public void testCase2() throws IOException{

		SpanQuery q = new SpanTermQuery(new Term("tokens","s:Wir"));
		ks = new KorapSearch(q);
		kr = ks.run(ki);
		assertEquals(1907, kr.getTotalResults());
		
		SpanDistanceQuery sq;
		// ordered
		sq = createDistanceQuery("s:Wir", "s:kommen", 1, 1, true, true);				
		ks = new KorapSearch(sq);
		kr = ks.run(ki);
		assertEquals(1899, kr.getTotalResults());
		
		// unordered
		sq = createDistanceQuery("s:Wir", "s:kommen", 1, 1, false, true);				
		ks = new KorapSearch(sq);
		kr = ks.run(ki);
		assertEquals(1896, kr.getTotalResults());	
		
//		System.out.println(kr.getTotalResults());
//		for (KorapMatch km : kr.getMatches()){
//			System.out.println(km.getDocID() +" "+km.getStartPos() +" "+ km.getEndPos());
//        	System.out.println(km.getSnippetBrackets());
//        }
	}
	
	/** Element distance spans */
	@Test
	public void testCase3() throws IOException{
		// ordered
		SpanDistanceQuery sq = createElementDistanceQuery("s","s:weg", "s:fahren", 
				0, 1, true, false);
		ks = new KorapSearch(sq);
		kr = ks.run(ki);		
		assertEquals(3,kr.getTotalResults());
		
		// unordered
		sq = createElementDistanceQuery("s","s:weg", "s:fahren", 0, 1, false,false);
		ks = new KorapSearch(sq);
		kr = ks.run(ki);
		assertEquals(5,kr.getTotalResults());
		
		// only 0
		sq = createElementDistanceQuery("s","s:weg", "s:fahren", 0, 0, false,false);
		kr = ki.search(sq, (short) 100);
		assertEquals(2,kr.getTotalResults());
		assertEquals("WPD_BBB.04463", kr.match(0).getDocID());
		assertEquals(1094,kr.getMatch(0).getStartPos());
		assertEquals(1115,kr.getMatch(0).getEndPos());
		assertEquals("WPD_III.00758", kr.match(1).getDocID());
		assertEquals(444,kr.getMatch(1).getStartPos());
		assertEquals(451,kr.getMatch(1).getEndPos());
		
		// only 1
		sq = createElementDistanceQuery("s","s:weg", "s:fahren", 1, 1, false,false);
		ks = new KorapSearch(sq);
		kr = ks.run(ki);
		assertEquals(3,kr.getTotalResults());		
	}
	
	/** Element distance exclusion */
	@Test
	public void testCase4() throws IOException{
		SpanDistanceQuery sq = createElementDistanceQuery("s","s:weg", "s:fahren", 1, 1, false, true);
		ks = new KorapSearch(sq);
		kr = ks.run(ki);
		assertEquals(979,kr.getTotalResults());
		//0.8s
		
		// Check if it includes some results
		BooleanFilter bf = new BooleanFilter();
		bf.or("ID", "WPD_BBB.04463", "WPD_III.00758");
		KorapCollection kc = new KorapCollection();	
		kc.filter(bf);
		ks.setCollection(kc);
		kr = ks.run(ki);		
		assertEquals(1094,kr.getMatch(0).getStartPos());
		assertEquals(451,kr.getMatch(1).getEndPos());		
	}
			
	/** Quantifier */
	@Test
	public void testCase5() throws IOException{
		SpanQuery sq;
		sq = new SpanRepetitionQuery(new SpanTermQuery(new Term("tokens","mate/p:ADJA")),1,2, true);
		ks = new KorapSearch(sq);
		kr = ks.run(ki);
		assertEquals(4116416, kr.getTotalResults());
		//0.9s
		
		sq = new SpanRepetitionQuery(new SpanTermQuery(new Term("tokens","mate/p:ADJA")),1,1, true);
		ks = new KorapSearch(sq);
		kr = ks.run(ki);
		assertEquals(3879671, kr.getTotalResults());
		
		sq = new SpanRepetitionQuery(new SpanTermQuery(new Term("tokens","mate/p:ADJA")),2,2, true);
		ks = new KorapSearch(sq);
		kr = ks.run(ki);
		assertEquals(236745, kr.getTotalResults());
		//0.65s
	}
	
	/** Next and quantifier */
	@Test
	public void testCase6() throws IOException{
		SpanQuery sq = new SpanNextQuery(
        		new SpanTermQuery(new Term("tokens", "tt/p:NN")),
        		new SpanRepetitionQuery(new SpanTermQuery(new Term("tokens","mate/p:ADJA")),2,2, true)
    		);
		ks = new KorapSearch(sq);
		kr = ks.run(ki);
		assertEquals(30223, kr.getTotalResults());
		// 1.1s
		
		SpanQuery sq2 = new SpanNextQuery(sq,
				new SpanTermQuery(new Term("tokens", "tt/p:NN")));
		ks = new KorapSearch(sq2);
		kr = ks.run(ki);
		assertEquals(26607, kr.getTotalResults());
		// 1.1s
	}
}
