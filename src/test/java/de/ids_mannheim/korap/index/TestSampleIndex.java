package de.ids_mannheim.korap.index;

import static de.ids_mannheim.korap.TestSimple.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.spans.SpanMultiTermQueryWrapper;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.store.MMapDirectory;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.ids_mannheim.korap.query.QueryBuilder;
import de.ids_mannheim.korap.util.QueryException;
import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.KrillCollection;
import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.KrillQuery;
import de.ids_mannheim.korap.query.DistanceConstraint;
import de.ids_mannheim.korap.query.SpanClassQuery;
import de.ids_mannheim.korap.query.SpanDistanceQuery;
import de.ids_mannheim.korap.query.SpanMultipleDistanceQuery;
import de.ids_mannheim.korap.query.wrap.SpanQueryWrapper;
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.response.Match;
import de.ids_mannheim.korap.util.QueryException;

public class TestSampleIndex {

    private KrillIndex sample;
    private Krill krillAvailabilityAll;
    private SpanTermQuery sq;
    private List<DistanceConstraint> constraints;
    private Result kr;
    

    private KrillIndex getSampleIndex () throws IOException {
        return new KrillIndex(new MMapDirectory(
                Paths.get(getClass().getResource("/sample-index").getFile())));

    };


    public TestSampleIndex () throws IOException {
        sample = getSampleIndex();
        String jsonCollection = getJsonString(getClass()
                .getResource("/queries/collections/availability-all.jsonld").getFile());
        KrillCollection collection = new KrillCollection(jsonCollection);
        krillAvailabilityAll = new Krill();
        krillAvailabilityAll.setCollection(collection);

        // &Erfahrung
        sq = new SpanTermQuery(new Term("tokens", "tt/l:Erfahrung"));

        // /+w1:2,s0
        constraints = new ArrayList<DistanceConstraint>();
        constraints.add(TestMultipleDistanceIndex.createConstraint("w", 1, 2,
                true, false));
        constraints.add(TestMultipleDistanceIndex.createConstraint("tokens",
                "base/s:s", 0, 0, true, false));

    }

    @Test
    public void testRelationLemmaBug () throws IOException, QueryException {
        String filepath = getClass()
                .getResource("/queries/relation/lemma-bug.json")
                .getFile();
        SpanQueryWrapper sqwi = getJsonQuery(filepath);
        SpanQuery sq = sqwi.toQuery();

        kr = sample.search(sq, (short) 10);
        assertNotEquals(0, kr.getMatches().size());
      }
    
    @Test
    public void testMultipleDistanceWithWildcards ()
            throws IOException, QueryException {
        WildcardQuery wcquery =
                new WildcardQuery(new Term("tokens", "s:meine*"));
        SpanMultiTermQueryWrapper<WildcardQuery> mtq =
                new SpanMultiTermQueryWrapper<WildcardQuery>(wcquery);

        // meine* /+w1:2 &Erfahrung
        SpanQuery tdq = new SpanDistanceQuery(mtq, sq, TestMultipleDistanceIndex
                .createConstraint("w", 1, 2, true, false), true);

        kr = sample.search(tdq, (short) 10);
        assertEquals(4, kr.getMatches().size());
        assertEquals(107, kr.getMatch(0).getStartPos());
        assertEquals(109, kr.getMatch(0).getEndPos());
        assertEquals(132566, kr.getMatch(1).getStartPos());
        assertEquals(132569, kr.getMatch(1).getEndPos());
        assertEquals(161393, kr.getMatch(2).getStartPos());
        assertEquals(161396, kr.getMatch(2).getEndPos());
        assertEquals(10298, kr.getMatch(3).getStartPos());
        assertEquals(10301, kr.getMatch(3).getEndPos());

        // meine* /+s0 &Erfahrung
        SpanQuery edq = new SpanDistanceQuery(mtq, sq, TestMultipleDistanceIndex
                .createConstraint("tokens", "base/s:s", 0, 0, true, false),
                true);
        kr = sample.search(edq, (short) 20);
        assertEquals(18, kr.getMatches().size());

        //meine* /+w1:2,s0 &Erfahrung

        SpanQuery mdsq = new SpanMultipleDistanceQuery(
                new SpanClassQuery(mtq, (byte) 129),
                new SpanClassQuery(sq, (byte) 129), constraints, true, true);
        kr = sample.search(mdsq, (short) 10);
        assertEquals(4, kr.getMatches().size());

        // check SpanQueryWrapper generated query
        SpanQueryWrapper sqwi = getJsonQuery(
                getClass().getResource("/queries/bugs/cosmas_wildcards.jsonld")
                        .getFile());
        SpanQuery jsq = sqwi.toQuery();
        assertEquals(mdsq.toString(), jsq.toString());
    }


    @Test
    public void testWildcardsWithJson () throws IOException, QueryException {
        SpanQueryWrapper sqwi = getJsonQuery(getClass()
                .getResource("/queries/bugs/cosmas_wildcards_all.jsonld")
                .getFile());
        SpanQuery sq = sqwi.toQuery();
        kr = sample.search(sq, (short) 10);
        assertEquals(4, kr.getMatches().size());

        // test krill apply
        Krill krill = new Krill();
        krill.setSpanQuery(sq);
        krill.setIndex(sample);
        kr = krill.apply();
        assertEquals(4, kr.getMatches().size());

        // test krill deserialization
        String jsonString = getJsonString(getClass()
                .getResource("/queries/bugs/cosmas_wildcards_all.jsonld")
                .getFile());
        krill = new Krill();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(jsonString);
        final KrillQuery kq = new KrillQuery("tokens");
        krill.setQuery(kq);

        SpanQueryWrapper qw = kq.fromKoral(jsonNode.get("query"));
        assertEquals(sqwi.toQuery(), qw.toQuery());

        krill.setSpanQuery(qw.toQuery());
        kr = krill.apply(sample);
        assertEquals(4, kr.getMatches().size());
    }


    @Test
    public void testWildcardsWithCollectionJSON () throws IOException {
        // collection .*
        String json = getJsonString(getClass()
                .getResource("/queries/bugs/cosmas_wildcards_all.jsonld")
                .getFile());
        Krill krill = new Krill(json);
        kr = krill.apply(sample);
        assertEquals(4, kr.getMatches().size());

        // collection QAO.*
        json = getJsonString(getClass()
                .getResource("/queries/bugs/cosmas_wildcards_qao.jsonld")
                .getFile());
        krill = new Krill(json);
        assertEquals(krill.getCollection().toString(),
                "QueryWrapperFilter(availability:/QAO.*/)");
        kr = krill.apply(sample);
        assertEquals(4, kr.getMatches().size());

    }

    @Test
    public void testWildcardStarWithCollection () throws IOException {

        // meine*
        WildcardQuery wcquery =
                new WildcardQuery(new Term("tokens", "s:meine*"));
        SpanMultiTermQueryWrapper<WildcardQuery> mtq =
                new SpanMultiTermQueryWrapper<WildcardQuery>(wcquery);

        // meine* /+w1:2,s0 &Erfahrung
        SpanQuery mdsq = new SpanMultipleDistanceQuery(
                new SpanClassQuery(mtq, (byte) 129),
                new SpanClassQuery(sq, (byte) 129), constraints, true, true);

        krillAvailabilityAll.setSpanQuery(mdsq);
        kr = sample.search(krillAvailabilityAll);

        assertEquals(4, kr.getMatches().size());

        assertEquals("match-GOE/AGI/04846-p107-109", kr.getMatch(0).getID());
        assertEquals("QAO-NC-LOC:ids", kr.getMatch(0).getFieldValue("availability"));
        assertEquals(
                "... gelesen und erzählt hat, ich in "
                        + "[[meine Erfahrungen]] hätte mit aufnehmen sollen. "
                        + "heute jedoch ...",
                kr.getMatch(0).getSnippetBrackets());

        assertEquals("match-GOE/AGD/00000-p132566-132569",
                kr.getMatch(1).getID());
        assertEquals("QAO-NC-LOC:ids-NU:1", kr.getMatch(1).getFieldValue("availability"));
        assertEquals("... Mannes umständlich beibringen und solches "
                + "durch [[meine eigne Erfahrung]] bekräftigen: das "
                + "alles sollte nicht gelten ...",
                kr.getMatch(1).getSnippetBrackets());

        assertEquals("match-GOE/AGD/00000-p161393-161396",
                kr.getMatch(2).getID());
        assertEquals("QAO-NC-LOC:ids-NU:1", kr.getMatch(2).getFieldValue("availability"));
        assertEquals("... lassen, bis er sich zuletzt an "
                + "[[meine sämtlichen Erfahrungen]] und Überzeugungen "
                + "anschloß, in welchem Sinne ...",
                kr.getMatch(2).getSnippetBrackets());

        assertEquals("match-GOE/AGD/06345-p10298-10301",
                kr.getMatch(3).getID());
        assertEquals("QAO-NC", kr.getMatch(3).getFieldValue("availability"));
        assertEquals("... bis aufs Äußerste verfolgte, und, über "
                + "[[meine enge Erfahrung]] hinaus, nach ähnlichen Fällen "
                + "in der ...", kr.getMatch(3).getSnippetBrackets());
    }

	@Test
    public void testMatchWithDependency () throws IOException, QueryException {
		// /GOE/AGA/01784/p104-105/matchInfo?layer=c&foundry=corenlp&spans=true
		Match km = sample.getMatchInfo("match-GOE/AGD/00000-p132566-132569",
								   "tokens",
								   "corenlp",
								   "c",
								   true,
								   true);

		assertEquals(km.getSnippetBrackets(), "... [[meine eigne Erfahrung]] ...");
		assertEquals(km.getSnippetHTML(), "<span class=\"context-left\"><span class=\"more\"></span></span><span class=\"match\"><mark>meine eigne Erfahrung</mark></span><span class=\"context-right\"><span class=\"more\"></span></span>");

		km = sample.getMatchInfo("match-GOE/AGD/00000-p132566-132569",
								   "tokens",
								   "malt",
								   "d",
								   true,
								   true);

		//assertEquals(km.getSnippetBrackets(), "... [[{malt/d:DET>132567:meine} {#132567:{malt/d:ATTR>132567:eigne}} {malt/d:PN>132564:Erfahrung}]] ...");
  		assertEquals(km.getSnippetHTML(), "<span class=\"context-left\"><span class=\"more\"></span></span><span class=\"match\"><mark><span xlink:title=\"malt/d:DET\" xlink:show=\"none\" xlink:href=\"#token-GOE/AGD/00000-p132568\">meine</span> <span xlink:title=\"malt/d:ATTR\" xlink:show=\"none\" xlink:href=\"#token-GOE/AGD/00000-p132568\">eigne</span> <span xml:id=\"token-GOE/AGD/00000-p132568\"><span xlink:title=\"malt/d:PN\" xlink:show=\"none\" xlink:href=\"#token-GOE/AGD/00000-p132565\">Erfahrung</span></span></mark></span><span class=\"context-right\"><span class=\"more\"></span></span>");
                
		km = sample.getMatchInfo("match-GOE/AGD/00000-p132566-132569",
								 "tokens",
								 "malt",
								 "d",
								 true,
								 true,
								 true);

		// assertEquals(km.getSnippetBrackets().substring(0,20), "[{#132507:{malt/d:SU");
		assertEquals(km.getSnippetHTML().substring(0,20), "<span class=\"context");
	}   

    @Test
    public void TestSampleIndexParallel () throws IOException, QueryException, InterruptedException, ExecutionException {

        // The sample index is global

        final SpanQuery sq1 = new QueryBuilder("tokens").seg("s:meine").toQuery();
        final SpanQuery sq2 = new QueryBuilder("tokens").seg("s:ihre").toQuery();
        final SpanQuery sq3 = new QueryBuilder("tokens").seg("s:unseres").toQuery();

        Callable<String> req1 = new Callable<String>(){
                @Override
                public String call() throws Exception {

                    Result kr = sample.search(sq1, (short) 10);

                    if (kr.getMatch(0).getStartPos() != 131) {
                        return "1-1StartPos=" + kr.getMatch(0).getStartPos();
                    }

                    if (kr.getMatch(0).getEndPos() != 132) {
                        return "1-1EndPos=" + kr.getMatch(0).getEndPos();
                    }

                    if (kr.getMatch(1).getStartPos() != 803) {
                        return "1-2StartPos=" + kr.getMatch(1).getStartPos();
                    }

                    if (kr.getMatch(1).getEndPos() != 804) {
                        return "1-2EndPos=" + kr.getMatch(1).getEndPos();
                    }

                    if (!kr.getMatch(1).getSnippetBrackets().equals(
                            "... der Jesuiten Tun und Wesen hält [[meine]] Betrachtungen fest. Kirchen, Türme, Gebäude haben ..."
                            )) {
                        return "1-Snippet=" + kr.getMatch(1).getSnippetBrackets();
                    }
                    
                    return "ok";
                }
            };

        Callable<String> req2 = new Callable<String>(){
                @Override
                public String call() throws Exception {
                    Result kr = sample.search(sq2, (short) 10);

                    if (kr.getMatch(0).getStartPos() != 471) {
                        return "2-1StartPos=" + kr.getMatch(0).getStartPos();
                    }

                    if (kr.getMatch(0).getEndPos() != 472) {
                        return "2-1EndPos=" + kr.getMatch(0).getEndPos();
                    }

                    if (kr.getMatch(1).getStartPos() != 715) {
                        return "2-2StartPos=" + kr.getMatch(1).getStartPos();
                    }

                    if (kr.getMatch(1).getEndPos() != 716) {
                        return "2-2EndPos=" + kr.getMatch(1).getEndPos();
                    }

                    if (!kr.getMatch(1).getSnippetBrackets().equals(
                            "... und wie durch gefälligen Prunk sich [[ihre]] Kirchen auszeichnen, so bemächtigen sich die ..."
                            )) {
                        return "2-Snippet=" + kr.getMatch(1).getSnippetBrackets();
                    }
                    
                    return "ok";
                }
            };

        Callable<String> req3 = new Callable<String>(){
                @Override
                public String call() throws Exception {
                    Result kr = sample.search(sq3, (short) 10);

                    if (kr.getMatch(0).getStartPos() != 69582) {
                        return "3-1StartPos=" + kr.getMatch(0).getStartPos();
                    }

                    if (kr.getMatch(0).getEndPos() != 69583) {
                        return "3-1EndPos=" + kr.getMatch(0).getEndPos();
                    }

                    if (kr.getMatch(1).getStartPos() != 70671) {
                        return "3-2StartPos=" + kr.getMatch(1).getStartPos();
                    }

                    if (kr.getMatch(1).getEndPos() != 70672) {
                        return "3-2EndPos=" + kr.getMatch(1).getEndPos();
                    }

                    if (!kr.getMatch(1).getSnippetBrackets().equals(
                            "... Blatt gibt euch bloß ein Zeugnis [[unseres]] Unvermögens, diese Gegenstände genugsam zu fassen ..."
                            )) {
                        return "3-Snippet=" + kr.getMatch(1).getSnippetBrackets();
                    }
                    
                    return "ok";
                }
            };
        

        // Create a pool with n threads
        ExecutorService executor = Executors.newFixedThreadPool(16);

        for (int i = 0; i < 2000; i++) {
            Future<String> res3 = executor.submit(req3);
            Future<String> res1 = executor.submit(req1);
            Future<String> res2 = executor.submit(req2);

            String value1 = res1.get();
            String value2 = res2.get();
            String value3 = res3.get();

            if (!value1.equals("ok")) {
                System.err.println("at "+ i);
                assertEquals("ok", value1);
                break;
            }
            if (!value2.equals("ok")) {
                System.err.println("at "+ i);
                assertEquals("ok", value2);
                break;
            }
            if (!value3.equals("ok")) {
                System.err.println("at "+ i);
                assertEquals("ok", value3);
                break;
            }
            System.err.println("Run "+i);
        };
        
        executor.shutdown();
    };
}
