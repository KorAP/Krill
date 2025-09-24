package de.ids_mannheim.korap.index;

import static de.ids_mannheim.korap.TestSimple.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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
        assertEquals(
            "QueryWrapperFilter(availability:/QAO.*/)",
            krill.getCollection().toString());
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

		assertEquals("... [[meine eigne Erfahrung]] ...", km.getSnippetBrackets());
		assertEquals(
		    "<span class=\"context-left\"><span class=\"more\"></span></span><span class=\"match\"><mark>meine eigne Erfahrung</mark></span><span class=\"context-right\"><span class=\"more\"></span></span>",
		    km.getSnippetHTML());

		km = sample.getMatchInfo("match-GOE/AGD/00000-p132566-132569",
								   "tokens",
								   "malt",
								   "d",
								   true,
								   true);

		//assertEquals(
		//    "... [[{malt/d:DET>132567:meine} {#132567:{malt/d:ATTR>132567:eigne}} {malt/d:PN>132564:Erfahrung}]] ...",
		//    km.getSnippetBrackets());
  		assertEquals(
  		    "<span class=\"context-left\"><span class=\"more\"></span></span><span class=\"match\"><mark><span xlink:title=\"malt/d:DET\" xlink:show=\"none\" xlink:href=\"#token-GOE/AGD/00000-p132568\">meine</span> <span xlink:title=\"malt/d:ATTR\" xlink:show=\"none\" xlink:href=\"#token-GOE/AGD/00000-p132568\">eigne</span> <span xml:id=\"token-GOE/AGD/00000-p132568\"><span xlink:title=\"malt/d:PN\" xlink:show=\"none\" xlink:href=\"#token-GOE/AGD/00000-p132565\">Erfahrung</span></span></mark></span><span class=\"context-right\"><span class=\"more\"></span></span>",
  		    km.getSnippetHTML());
                
		km = sample.getMatchInfo("match-GOE/AGD/00000-p132566-132569",
								 "tokens",
								 "malt",
								 "d",
								 true,
								 true,
								 true);

		// assertEquals("[{#132507:{malt/d:SU", km.getSnippetBrackets().substring(0,20));
		assertEquals("<span class=\"context", km.getSnippetHTML().substring(0,20));
	}
}
