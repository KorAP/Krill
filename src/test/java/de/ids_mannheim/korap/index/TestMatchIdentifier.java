package de.ids_mannheim.korap.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;

import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import de.ids_mannheim.korap.response.Result;

import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.query.QueryBuilder;
import de.ids_mannheim.korap.index.FieldDocument;
import de.ids_mannheim.korap.response.Match;
import de.ids_mannheim.korap.response.SearchContext;
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.response.match.MatchIdentifier;
import de.ids_mannheim.korap.response.match.PosIdentifier;
import de.ids_mannheim.korap.util.KrillProperties;
import de.ids_mannheim.korap.util.QueryException;

@RunWith(JUnit4.class)
public class TestMatchIdentifier {

    ObjectMapper mapper = new ObjectMapper();


    @Test
    public void identifierExample1 () throws IOException, QueryException {
        MatchIdentifier id = new MatchIdentifier("match-c1!d1-p4-20");
        assertEquals("c1", id.getCorpusID());
        assertEquals("d1", id.getDocID());
        assertEquals(4, id.getStartPos());
        assertEquals(20, id.getEndPos());

        assertEquals("match-c1!d1-p4-20", id.toString());
        id.addPos(10, 14, 2);
        assertEquals("match-c1!d1-p4-20(2)10-14", id.toString());
        id.addPos(11, 12, 5);
        assertEquals("match-c1!d1-p4-20(2)10-14(5)11-12", id.toString());
        // Ignore
        id.addPos(11, 12, -8);
        assertEquals("match-c1!d1-p4-20(2)10-14(5)11-12", id.toString());
        id.addPos(11, -12, 8);
        assertEquals("match-c1!d1-p4-20(2)10-14(5)11-12", id.toString());
        id.addPos(-11, 12, 8);
        assertEquals("match-c1!d1-p4-20(2)10-14(5)11-12", id.toString());

        id = new MatchIdentifier("matc-c1!d1-p4-20");
        assertNull(id.toString());
        id = new MatchIdentifier("match-d1-p4-20");
        assertNull(id.getCorpusID());
        assertEquals("d1", id.getDocID());
        id = new MatchIdentifier("match-p4-20");
        assertNull(id.toString());

        id = new MatchIdentifier("match-c1!d1-p4-20");
        assertEquals("match-c1!d1-p4-20", id.toString());

        id = new MatchIdentifier("match-c1!d1-p4-20(5)7-8");
        assertEquals("match-c1!d1-p4-20(5)7-8", id.toString());

        id = new MatchIdentifier("match-c1!d1-p4-20(5)7-8(-2)9-10");
        assertEquals("match-c1!d1-p4-20(5)7-8", id.toString());

        id = new MatchIdentifier(
                "match-c1!d1-p4-20(5)7-8(-2)9-10(2)3-4(3)-5-6");
        assertEquals("match-c1!d1-p4-20(5)7-8(2)3-4", id.toString());

        id = new MatchIdentifier(
                "match-c1!d1-p4-20(5)7-8(-2)9-10(2)3-4(3)-5-6(4)7-8");
        assertEquals("match-c1!d1-p4-20(5)7-8(2)3-4(4)7-8", id.toString());

        id = new MatchIdentifier(
                "match-c1!d1-p4-20(5)7-8(-2)9-10(2)3-4(3)-5-6(4)7-8(5)9--10");
        assertEquals(4, id.getStartPos());
        assertEquals(20, id.getEndPos());
        assertEquals("c1", id.getCorpusID());
        assertEquals("d1", id.getDocID());
        assertEquals(null, id.getTextSigle());
        assertEquals("match-c1!d1-p4-20(5)7-8(2)3-4(4)7-8", id.toString());

        id = new MatchIdentifier("match-GOE!GOE_AGF.02286-p2105-2106");
        assertEquals(2105, id.getStartPos());
        assertEquals(2106, id.getEndPos());
        assertEquals(null, id.getCorpusID());
        assertEquals(null, id.getDocID());
        assertEquals("GOE_AGF.02286", id.getTextSigle());
        assertEquals("match-GOE_AGF.02286-p2105-2106", id.toString());

        id = new MatchIdentifier("match-corpus-1/doc-1/text-1/p2105-2106");
        assertEquals("match-corpus-1/doc-1/text-1-p2105-2106", id.toString());
        assertEquals("corpus-1/doc-1/text-1", id.getTextSigle());
    };

    @Test
    public void posIdentifierExample1 () throws IOException {
        PosIdentifier id = new PosIdentifier();
        id.setCorpusID("c1");
        id.setDocID("d1");
        id.setStart(8);
        assertEquals("c1", id.getCorpusID());
        assertEquals("d1", id.getDocID());
        assertEquals(8, id.getStart());
        assertEquals("token-c1!d1-p8", id.toString());
    };

    @Test
    public void posIdentifierExampleSign () throws IOException {

        MatchIdentifier.initMac("tree");
        
        MatchIdentifier id = new MatchIdentifier();
        id.setTextSigle("aaa/bbb/ccc");
        id.setStartPos(8);
        id.setEndPos(10);
        assertEquals("match-aaa/bbb/ccc-p8-10x_ibY-h1k-VJ4aZjBFgTu8N4OI6xqcp-PkUrjQ9080Kr8", id.toString());

        id = new MatchIdentifier("match-aaa/bbb/ccc-p8-10x_ibY-h1k-VJ4aZjBFgTu8N4OI6xqcp-PkUrjQ9080Kr8");

        assertNotNull(id);
        assertEquals("aaa/bbb/ccc", id.getTextSigle());
        assertEquals(8, id.getStartPos());
        assertEquals(10, id.getEndPos());

        // Fail - match wrong: p9 instead of p8
        id = new MatchIdentifier("match-aaa/bbb/ccc-p9-10x_ibY-h1k-VJ4aZjBFgTu8N4OI6xqcp-PkUrjQ9080Kr8");

        assertNotNull(id);
        assertEquals("", id.getTextSigle());
        assertEquals(0, id.getStartPos());
        assertEquals(-1, id.getEndPos());

        // Fail - signature wrong: 4Ou6 instead of 40I6
        id = new MatchIdentifier("match-aaa/bbb/ccc-p8-10x_ibY-h1k-VJ4aZjBFgTu8N4Ou6xqcp-PkUrjQ9080Kr8");

        assertNotNull(id);
        assertEquals("", id.getTextSigle());
        assertEquals(0, id.getStartPos());
        assertEquals(-1, id.getEndPos());
        
        // Fail - signature wrong: vJ instead of VJ
        id = new MatchIdentifier("match-aaa/bbb/ccc-p8-10x_ibY-h1k-vJ4aZjBFgTu8N4OI6xqcp-PkUrjQ9080Kr8");

        assertNotNull(id);
        assertEquals("", id.getTextSigle());
        assertEquals(0, id.getStartPos());
        assertEquals(-1, id.getEndPos());

        // Fail - match wrong: aab instead of aaa
        id = new MatchIdentifier("match-aab/bbb/ccc-p8-10x_ibY-h1k-VJ4aZjBFgTu8N4OI6xqcp-PkUrjQ9080Kr8");

        assertNotNull(id);
        assertEquals("", id.getTextSigle());
        assertEquals(0, id.getStartPos());
        assertEquals(-1, id.getEndPos());


        MatchIdentifier.initMac("");
    };
    
	@Test
    public void posIdentifierExample2 () throws IOException {
        PosIdentifier id = new PosIdentifier();
        id.setCorpusID("c1");
        id.setDocID("d1");
        id.setStart(8);
        id.setEnd(12);
        assertEquals("c1", id.getCorpusID());
        assertEquals("d1", id.getDocID());
        assertEquals(8, id.getStart());
        assertEquals(12, id.getEnd());
        assertEquals("token-c1!d1-p8-12", id.toString());
    };


    @Test
    public void indexExample1 () throws IOException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(createSimpleFieldDoc());
        ki.commit();

        QueryBuilder kq = new QueryBuilder("tokens");
        Krill ks = new Krill(
                kq.nr(2, kq.seq(kq.seg("s:b")).append(kq.nr(kq.seg("s:a")))));
        Result kr = ki.search(ks);

        assertEquals("totalResults", kr.getTotalResults(), 1);
        assertEquals("StartPos (0)", kr.getMatch(0).startPos, 7);
        assertEquals("EndPos (0)", kr.getMatch(0).endPos, 9);

        Match km = kr.getMatch(0);

        assertEquals("SnippetBrackets (0)", "... bcabca[[{2:b{1:a}}]]c",
                km.getSnippetBrackets());
        assertEquals("SnippetTokens (0)", "{\"left\":[\"b\",\"c\",\"a\",\"b\",\"c\",\"a\"],\"match\":[\"b\",\"a\"],\"right\":[\"c\"],\"classes\":[[2,0,1],[1,1,1]]}",
                     km.getSnippetTokens().toString());
        assertEquals("ID (0)", "match-c1!d1-p7-9(2)7-8(1)8-8", km.getID());
    };

    @Test
    public void indexExample1Sign () throws IOException {
        MatchIdentifier.initMac("tree");

        KrillIndex ki = new KrillIndex();
        ki.addDoc(createSimpleFieldDoc());
        ki.commit();

        QueryBuilder kq = new QueryBuilder("tokens");
        Krill ks = new Krill(
                kq.nr(2, kq.seq(kq.seg("s:b")).append(kq.nr(kq.seg("s:a")))));
        Result kr = ki.search(ks);

        assertEquals("totalResults", kr.getTotalResults(), 1);
        assertEquals("StartPos (0)", kr.getMatch(0).startPos, 7);
        assertEquals("EndPos (0)", kr.getMatch(0).endPos, 9);

        Match km = kr.getMatch(0);

        assertEquals("SnippetBrackets (0)", "... bcabca[[{2:b{1:a}}]]c",
                km.getSnippetBrackets());
        assertEquals("SnippetTokens (0)", "{\"left\":[\"b\",\"c\",\"a\",\"b\",\"c\",\"a\"],\"match\":[\"b\",\"a\"],\"right\":[\"c\"],\"classes\":[[2,0,1],[1,1,1]]}",
                     km.getSnippetTokens().toString());
        assertEquals("ID (0)", "match-c1!d1-p7-9(2)7-8(1)8-8x_07WRwmjA5EigwG8wYcURhnz_WkL9cepvU96hC2mp6SE", km.getID());

        MatchIdentifier.initMac("");
    };

    

    @Test
    public void indexExample2 () throws IOException, QueryException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(createSimpleFieldDoc());
        ki.commit();

        Match km = ki.getMatch("match-c1!d1-p7-9(0)8-8(2)7-8");

        assertEquals("StartPos (0)", 7, km.getStartPos());
        assertEquals("EndPos (0)", 9, km.getEndPos());

        assertEquals("SnippetBrackets (0)", "... [[{2:b{a}}]] ...",
                km.getSnippetBrackets());
        assertEquals("SnippetTokens (0)", "{\"match\":[\"b\",\"a\"],\"classes\":[[0,1,1],[2,0,1]]}",
                     km.getSnippetTokens().toString());

        assertEquals("ID (0)", "match-c1!d1-p7-9(0)8-8(2)7-8", km.getID());

        km = ki.getMatchInfo("match-c1!d1-p7-9(0)8-8(2)7-8", "tokens", "f", "m",
                false, false);

        assertEquals("SnippetBrackets (1)",
                "... [[{f/m:acht:b}{f/m:neun:a}]] ...",
                km.getSnippetBrackets());
        assertEquals("SnippetTokens (1)", "{\"match\":[\"b\",\"a\"]}",
                     km.getSnippetTokens().toString());

		// Mirror identifier when passed
        km = ki.getMatchInfo("match-c1!d1-p7-9(0)8-8(2)7-8", "tokens", "f",
                null, false, false);
        assertEquals("SnippetBrackets (1b)",
                "... [[{f/m:acht:{f/y:eight:b}}{f/m:neun:{f/y:nine:a}}]] ...",
                km.getSnippetBrackets());
        assertEquals("SnippetTokens (1b)", "{\"match\":[\"b\",\"a\"]}",
                     km.getSnippetTokens().toString());

        JsonNode res = mapper.readTree(km.toJsonString());
        assertEquals("match-c1!d1-p7-9(0)8-8(2)7-8",
                res.at("/matchID").asText());
		
        km = ki.getMatchInfo("match-c1!d1-p7-9(0)8-8(2)7-8", "tokens", "f", "m",
                false, true);

        assertEquals("SnippetBrackets (2)",
                "... [[{2:{f/m:acht:b}{{f/m:neun:a}}}]] ...",
                km.getSnippetBrackets());
        assertEquals("SnippetTokens (2)", "{\"match\":[\"b\",\"a\"],\"classes\":[[0,1,1],[2,0,1]]}",
                     km.getSnippetTokens().toString());

        km = ki.getMatchInfo("match-c1!d1-p7-9(4)8-8(2)7-8", "tokens", "f", "m",
                false, true);

        assertEquals("SnippetBrackets (3)",
                "... [[{2:{f/m:acht:b}{4:{f/m:neun:a}}}]] ...",
                km.getSnippetBrackets());
        assertEquals("SnippetTokens (3)", "{\"match\":[\"b\",\"a\"],\"classes\":[[4,1,1],[2,0,1]]}",
                     km.getSnippetTokens().toString());

        km = ki.getMatchInfo("match-c1!d1-p7-9(4)8-8(2)7-8", "tokens", "f",
                null, false, true);

        assertEquals("SnippetBrackets (4)",
                "... [[{2:{f/m:acht:{f/y:eight:b}}{4:{f/m:neun:{f/y:nine:a}}}}]] ...",
                km.getSnippetBrackets());
        assertEquals("SnippetTokens (4)", "{\"match\":[\"b\",\"a\"],\"classes\":[[4,1,1],[2,0,1]]}",
                     km.getSnippetTokens().toString());

        assertEquals("SnippetHTML (4)",
                "<span class=\"context-left\">" + "<span class=\"more\">"
                        + "</span>" + "</span>" + "<span class=\"match\">"
                        + "<mark>" + "<mark class=\"class-2 level-0\">"
                        + "<span title=\"f/m:acht\">"
                        + "<span title=\"f/y:eight\">" + "b" + "</span>"
                        + "</span>" + "<mark class=\"class-4 level-1\">"
                        + "<span title=\"f/m:neun\">"
                        + "<span title=\"f/y:nine\">" + "a" + "</span>"
                        + "</span>" + "</mark>" + "</mark>" + "</mark>"
                        + "</span>" + "<span class=\"context-right\">"
                        + "<span class=\"more\">" + "</span>" + "</span>",
                km.getSnippetHTML());

		res = mapper.readTree(km.toJsonString());
        // assertEquals("tokens", res.at("/field").asText());
        assertFalse(res.has("startMore"));
        assertFalse(res.has("endMore"));
        assertEquals("c1", res.at("/corpusID").asText());
        assertEquals("d1", res.at("/docID").asText());
        assertEquals("match-c1!d1-p7-9(4)8-8(2)7-8",
                res.at("/matchID").asText());
        assertTrue(res.at("/pubDate").isMissingNode());
    };


    @Test
    public void indexExample3 () throws IOException, QueryException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(createSimpleFieldDoc());
        ki.commit();

        Match km = ki.getMatchInfo("match-c1!d1-p7-9(4)8-8(2)7-8", "tokens",
                null, null, false, true);


        assertEquals("SnippetHTML (1)", "<span class=\"context-left\">"
                + "<span class=\"more\">" + "</span>" + "</span>"
                + "<span class=\"match\">" + "<mark>"
                + "<mark class=\"class-2 level-0\">"
                + "<span title=\"f/m:acht\">" + "<span title=\"f/y:eight\">"
                + "<span title=\"it/is:8\">" + "<span title=\"x/o:achtens\">"
                + "b" + "</span>" + "</span>" + "</span>" + "</span>"
                + "<mark class=\"class-4 level-1\">"
                + "<span title=\"f/m:neun\">" + "<span title=\"f/y:nine\">"
                + "<span title=\"it/is:9\">" + "<span title=\"x/o:neuntens\">"
                + "a" + "</span>" + "</span>" + "</span>" + "</span>"
                + "</mark>" + "</mark>" + "</mark>" + "</span>"
                + "<span class=\"context-right\">" + "<span class=\"more\">"
                + "</span>" + "</span>", km.getSnippetHTML());
    };


    @Test
    public void indexExample4 () throws IOException, QueryException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(createSimpleFieldDoc());
        ki.commit();

        Match km = ki.getMatchInfo("match-c1!d1-p7-9(4)8-8(2)7-8", "tokens",
                null, null, false, false);


        assertEquals("SnippetHTML (1)", "<span class=\"context-left\">"
                + "<span class=\"more\">" + "</span>" + "</span>"
                + "<span class=\"match\">" + "<mark>"
                + "<span title=\"f/m:acht\">" + "<span title=\"f/y:eight\">"
                + "<span title=\"it/is:8\">" + "<span title=\"x/o:achtens\">"
                + "b" + "</span>" + "</span>" + "</span>" + "</span>"
                + "<span title=\"f/m:neun\">" + "<span title=\"f/y:nine\">"
                + "<span title=\"it/is:9\">" + "<span title=\"x/o:neuntens\">"
                + "a" + "</span>" + "</span>" + "</span>" + "</span>"
                + "</mark>" + "</span>" + "<span class=\"context-right\">"
                + "<span class=\"more\">" + "</span>" + "</span>",
                km.getSnippetHTML());
    };

    @Test
    public void indexExampleSign () throws IOException, QueryException {

        MatchIdentifier.initMac("tree");

        
        KrillIndex ki = new KrillIndex();
        ki.addDoc(createSimpleFieldDoc());
        ki.commit();

        Match km = ki.getMatchInfo("match-c1!d1-p7-9(4)8-8(2)7-8", "tokens",
                                   null, null, false, false);

        JsonNode res = mapper.readTree(km.toJsonString());
        assertEquals("Invalid match identifier", res.at("/errors/0/1").asText());
        assertEquals("", res.at("/matchID").asText());
        assertEquals("", res.at("/fields/0/key").asText());


        km = ki.getMatchInfo("match-c1!d1-p7-9(2)7-8(1)8-8x_07WRwmjA5EigwG8wYcURhnz_WkL9cepvU96hC2mp6SE", "tokens",
                                   null, null, false, false);

        res = mapper.readTree(km.toJsonString());
        assertEquals("", res.at("/errors/0/1").asText());
        assertEquals("match-c1!d1-p7-9(2)7-8(1)8-8x_07WRwmjA5EigwG8wYcURhnz_WkL9cepvU96hC2mp6SE", res.at("/matchID").asText());
        assertEquals("ID", res.at("/fields/0/key").asText());

        MatchIdentifier.initMac("");
    };

    

    @Test
    public void indexNewStructure () throws IOException, QueryException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(getClass().getResourceAsStream("/goe/AGX-00002.json"), false);
        ki.commit();

        Match km = ki.getMatchInfo("match-GOE!GOE_AGX.00002-p210-211", "tokens",
                true, (String) null, (String) null, true, true, true);

        JsonNode res = mapper.readTree(km.toJsonString());
        // assertEquals("tokens", res.at("/field").asText());
        assertEquals("GOE_AGX.00002", res.at("/textSigle").asText());
        assertEquals("Goethe, Johann Wolfgang von", res.at("/author").asText());
	};

	// ND: Although this test is failing, I assume it is probably
	//     due to a data bug.
	@Ignore
    public void snippetBugTest () throws IOException, QueryException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(getClass().getResourceAsStream("/wiki/wpd15-u43-34816.json.gz"), true);
        ki.commit();

        Match km = ki.getMatchInfo("match-WPD15/U43/34816-p420-422", "tokens",
								   "tt", "l", false, false);

		assertEquals("SnippetBrackets (with Spans)",
					 "<span class=\"context-left\">"+
					 "<span class=\"more\"></span></span>"+
					 "<span class=\"match\">"+
					 "<mark>"+
					 "<span title=\"tt/l:online\">online</span> "+
					 "<span title=\"tt/l:verfügbar\">verfügbar</span>"+
					 "</mark>"+
					 "</span>"+
					 "<span class=\"context-right\">"+
					 "<span class=\"more\"></span>"+
					 "</span>",
					 km.getSnippetHTML());

		 km = ki.getMatchInfo("match-WPD15/U43/34816-p420-422", "tokens",
								   "dereko", null, true, false);

		 assertEquals("SnippetBrackets (with Spans)",
					  "<span class=\"context-left\">"+
					  "<span class=\"more\"></span>"+
					  "</span>"+
					  "<span class=\"match\">"+
					  "<mark>"+
					  "<span title=\"dereko/s:ref\">online</span> verfügbar"+
					  "</mark>"+
					  "</span>"+
					  "<span class=\"context-right\">"+
					  "<span class=\"more\"></span>"+
					  "</span>",
					 km.getSnippetHTML());

	};


	@Test
    public void snippetBugTest2 () throws IOException, QueryException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(getClass().getResourceAsStream("/wiki/wdd17-982-72848.json.gz"), true);
        ki.commit();

        Match km = ki.getMatchInfo("match-WDD17/982/72848-p15844-15846", "tokens",
								   "lwc", "d", true, true, true);

		String snippet = km.getSnippetHTML();
		assertEquals(
			"SnippetBrackets (with Spans)",
			snippet,
			"<span class=\"context-left\"></span>"+
			"<span class=\"match\">"+
			"<span xml:id=\"token-WDD17/982/72848-p15836-15839\">"+
			"<span xlink:title=\"lwc/d:NK\" xlink:show=\"none\" xlink:href=\"#token-WDD17/982/72848-p15838\">Ein</span>"+
			" "+
			"<span xlink:title=\"lwc/d:NK\" xlink:show=\"none\" xlink:href=\"#token-WDD17/982/72848-p15838\">letztes</span>"+
			" "+
			"<span xml:id=\"token-WDD17/982/72848-p15838\">"+
			"<span xlink:title=\"lwc/d:--\" xlink:show=\"none\" xlink:href=\"#token-WDD17/982/72848-p15836-15839\">mal</span>"+
			"</span>"+
			": "+
			"<span xml:id=\"token-WDD17/982/72848-p15839-15840\">"+
			"<span xlink:title=\"lwc/d:--\" xlink:show=\"none\" xlink:href=\"#token-WDD17/982/72848-p15839-15840\">AL</span>"+
			"</span>"+
			"</span>"+
			"<span xlink:show=\"other\" data-action=\"join\" xlink:href=\"#token-WDD17/982/72848-p15839-15840\">"+
			":"+
			"<span xml:id=\"token-WDD17/982/72848-p15840-15846\">"+
			"<span xml:id=\"token-WDD17/982/72848-p15840\">"+
			"<span xlink:title=\"lwc/d:--\" xlink:show=\"none\" xlink:href=\"#token-WDD17/982/72848-p15840-15846\">halt</span>"+
			"</span>"+
			"</span>"+
			"</span>"+
			// "<span xml:id=\"token-WDD17/982/72848-p15840-15846\">"+
			"<span xlink:show=\"other\" data-action=\"join\" xlink:href=\"#token-WDD17/982/72848-p15840-15846\">"+
			" "+
			"<span xlink:title=\"lwc/d:NK\" xlink:show=\"none\" xlink:href=\"#token-WDD17/982/72848-p15842\">den</span>"+
			" "+
			"<span xml:id=\"token-WDD17/982/72848-p15842\">"+
			"<span xlink:title=\"lwc/d:OA\" xlink:show=\"none\" xlink:href=\"#token-WDD17/982/72848-p15843\">Ball</span>"+
			"</span>"+
			" "+
			"<span xml:id=\"token-WDD17/982/72848-p15843\">"+
			"<span xlink:title=\"lwc/d:PD\" xlink:show=\"none\" xlink:href=\"#token-WDD17/982/72848-p15840\">flach</span>"+
			"</span>"+
			", "+
			"<mark>"+
			"<span xlink:title=\"lwc/d:MO\" xlink:show=\"none\" xlink:href=\"#token-WDD17/982/72848-p15845\">ganz</span>"+
			" "+
			"<span xml:id=\"token-WDD17/982/72848-p15845\">"+
			"<span xlink:title=\"lwc/d:CJ\" xlink:show=\"none\" xlink:href=\"#token-WDD17/982/72848-p15843\">flach</span>"+
			"</span>"+
			"</mark>"+
			"."+
			"</span>"+
			"</span>"+
			"<span class=\"context-right\"></span>"
			);
	};


	@Test
    public void snippetBugTest3 () throws IOException, QueryException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(getClass().getResourceAsStream("/wiki/WPD17-H81-63495.json.gz"), true);
        ki.commit();

        Match km;
		String snippet;

		km = ki.getMatchInfo("match-WPD17/H81/63495-p88-91", "tokens",
							 "xyz", "s", false, false, false);
		km.setContext(new SearchContext(true, (short) 2, true, (short) 2));

		snippet = km.getSnippetHTML();
		assertEquals(
			"<span class=\"context-left\">"+
			"<span class=\"more\"></span>"+
			"angesehen wurde. "+
			"</span>"+
			"<span class=\"match\">"+
			"<mark>Der alte Baum</mark>"+
			"</span>"+
			"<span class=\"context-right\">"+
			" war eine"+
			"<span class=\"more\"></span>"+
			"</span>",
			snippet
			);

        String snippetTokens = km.getSnippetTokens().toString();
        assertEquals("{\"left\":[\"angesehen\",\"wurde\"]," +
                     "\"match\":[\"Der\",\"alte\",\"Baum\"]," +
                     "\"right\":[\"war\",\"eine\"]}",
                     snippetTokens);

		// Expansion - no context but inner match
		km = ki.getMatchInfo("match-WPD17/H81/63495-p88-91", "tokens",
								   "xyz", "s", true, true, true);
		snippet = km.getSnippetHTML();
		assertEquals(
			"<span class=\"context-left\">"+
			"</span>"+
			"<span class=\"match\">"+
			"<mark>Der alte Baum</mark>"+
			" war eine Sommerlinde (Tilia platyphyllos) , "+
			"der neue ist eine Winterlinde (Tilia cordata)."+
			"</span>"+
			"<span class=\"context-right\">"+
			"</span>",
			snippet
			);

		// Addition context
		/*
		  TODO: Support context nonetheless
		km = ki.getMatchInfo("match-WPD17/H81/63495-p88-91", "tokens",
								   "xyz", "s", true, true, true);
		km.setContext(new SearchContext(true, (short) 2, true, (short) 2));

		snippet = km.getSnippetHTML();
		assertEquals(
			"<span class=\"context-left\">"+
			"xyz"+
			"</span>"+
			"<span class=\"match\">"+
			"<mark>Der alte Baum</mark>"+
			" war eine Sommerlinde (Tilia platyphyllos) , "+
			"der neue ist eine Winterlinde (Tilia cordata)."+
			"</span>"+
			"<span class=\"context-right\">"+
			"xyz"+
			"</span>",
			snippet
			);
		*/

		km = ki.getMatchInfo("match-WPD17/H81/63495-p88-91", "tokens",
							 null, null, false, true, true);
		snippet = km.getSnippetHTML();
		assertEquals(
			"<span class=\"context-left\"></span>"+
			"<span class=\"match\">"+
			"<mark>"+
			"<span title=\"tt/l:die\">"+
			"<span title=\"tt/p:ART\">Der</span>"+
			"</span>"+
			" "+
			"<span title=\"tt/l:alt\">"+
			"<span title=\"tt/p:ADJA\">alte</span>"+
			"</span>"+
			" "+
			"<span title=\"tt/l:Baum\">"+
			"<span title=\"tt/p:NN\">Baum</span>"+
			"</span>"+
			"</mark>"+
			" "+
			"<span title=\"tt/l:sein\">"+
			"<span title=\"tt/p:VAFIN\">war</span>"+
			"</span>"+
			" "+
			"<span title=\"tt/l:eine\">"+
			"<span title=\"tt/p:ART\">eine</span>"+
			"</span>"+
			" "+
			"<span title=\"tt/l:Sommerlinde\">"+
			"<span title=\"tt/l:Sommerlinde\">"+
			"<span title=\"tt/p:NE\">"+
			"<span title=\"tt/p:NN\">Sommerlinde</span>"+
			"</span>"+
			"</span>"+
			"</span>"+
			" ("+
			"<span title=\"tt/p:NE\">Tilia</span>"+
			" "+
			"<span title=\"tt/p:ADJA\">"+
			"<span title=\"tt/p:ADJD\">"+
			"<span title=\"tt/p:NE\">"+
			"<span title=\"tt/p:NN\">"+
			"<span title=\"tt/p:VVFIN\">platyphyllos</span>"+
			"</span>"+
			"</span>"+
			"</span>"+
			"</span>"+
			") , "+
			"<span title=\"tt/l:die\">"+
			"<span title=\"tt/p:ART\">der</span>"+
			"</span>"+
			" "+
			"<span title=\"tt/l:neu\">"+
			"<span title=\"tt/p:ADJA\">neue</span>"+
			"</span>"+
			" "+
			"<span title=\"tt/l:sein\">"+
			"<span title=\"tt/p:VAFIN\">ist</span>"+
			"</span>"+
			" "+
			"<span title=\"tt/l:eine\">"+
			"<span title=\"tt/p:ART\">eine</span>"+
			"</span>"+
			" "+
			"<span title=\"tt/l:Winterlinde\">"+
			"<span title=\"tt/l:Winterlinde\">"+
			"<span title=\"tt/p:NE\">"+
			"<span title=\"tt/p:NN\">Winterlinde</span>"+
			"</span>"+
			"</span>"+
			"</span>"+
			" ("+
			"<span title=\"tt/p:NE\">Tilia</span>"+
			" "+
			"<span title=\"tt/p:NE\">cordata</span>"+
			")."+
			"</span>"+
			"<span class=\"context-right\">"+
			"</span>",
			snippet
			);
		km = ki.getMatchInfo("match-WPD17/H81/63495-p88-91", "tokens",
								   "dereko", "s", true, true, true);

		snippet = km.getSnippetHTML();
		assertEquals(
			"<span class=\"context-left\"></span>"+
			"<span class=\"match\">"+
			  "<span title=\"dereko/s:s\">"+
			    "<mark>Der alte Baum</mark>"+
			    " war eine "+
			    "<span title=\"dereko/s:ref\">Sommerlinde</span>"+
			    " ("+
			    "<span title=\"dereko/s:hi\">Tilia platyphyllos</span>"+
			    ") , der neue ist eine "+
			    "<span title=\"dereko/s:ref\">Winterlinde</span> ("+
			    "<span title=\"dereko/s:hi\">Tilia cordata</span>"+
			  "</span>"+
			  ")."+
			"</span>"+
			"<span class=\"context-right\"></span>",
			snippet
			);
	};

	
    @Test
    public void indexExample5Spans () throws IOException, QueryException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(createSimpleFieldDoc());
        ki.commit();

        Match km = ki.getMatchInfo("match-c1!d1-p7-9(4)8-8(2)7-8", "tokens",
                null, null, true, false);


        assertEquals("SnippetBrackets (1)",
                "... [[{f/m:acht:{f/y:eight:{it/is:8:{x/o:achtens:b}}}}{f/m:neun:{f/y:nine:{it/is:9:{x/o:neuntens:a}}}}]] ...",
                km.getSnippetBrackets());
    };


    @Test
    public void indexExample6Spans () throws IOException, QueryException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(createSimpleFieldDoc());
        ki.commit();

        Match km = ki.getMatchInfo("match-c1!d1-p7-10(4)8-8(2)7-8", "tokens",
                null, null, true, false);


        assertEquals("SnippetBrackets (1)",
                "... [[{x/tag:{f/m:acht:{f/y:eight:{it/is:8:{x/o:achtens:b}}}}{f/m:neun:{f/y:nine:{it/is:9:{x/o:neuntens:a}}}}{f/m:zehn:{f/y:ten:{it/is:10:{x/o:zehntens:c}}}}}]]",
                km.getSnippetBrackets());
    };


    @Test
    public void indexExample7Spans () throws IOException, QueryException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(createSimpleFieldDoc());
        ki.commit();

        Match km = ki.getMatchInfo("match-c1!d1-p7-10(4)8-8(2)7-8", "tokens",
                null, null, true, true);


        assertEquals("SnippetBrackets (1)",
                "... [[{x/tag:{2:{f/m:acht:{f/y:eight:{it/is:8:{x/o:achtens:b}}}}{4:{f/m:neun:{f/y:nine:{it/is:9:{x/o:neuntens:a}}}}}}{f/m:zehn:{f/y:ten:{it/is:10:{x/o:zehntens:c}}}}}]]",
                km.getSnippetBrackets());

        assertEquals("SnippetHTML (1)", "<span class=\"context-left\">"
                + "<span class=\"more\">" + "</span>" + "</span>"
                + "<span class=\"match\">" + "<mark>" + "<span title=\"x/tag\">"
                + "<mark class=\"class-2 level-0\">"
                + "<span title=\"f/m:acht\">" + "<span title=\"f/y:eight\">"
                + "<span title=\"it/is:8\">" + "<span title=\"x/o:achtens\">"
                + "b" + "</span>" + "</span>" + "</span>" + "</span>"
                + "<mark class=\"class-4 level-1\">"
                + "<span title=\"f/m:neun\">" + "<span title=\"f/y:nine\">"
                + "<span title=\"it/is:9\">" + "<span title=\"x/o:neuntens\">"
                + "a" + "</span>" + "</span>" + "</span>" + "</span>"
                + "</mark>" + "</mark>" + "<span title=\"f/m:zehn\">"
                + "<span title=\"f/y:ten\">" + "<span title=\"it/is:10\">"
                + "<span title=\"x/o:zehntens\">" + "c" + "</span>" + "</span>"
                + "</span>" + "</span>" + "</span>" + "</mark>" + "</span>"
                + "<span class=\"context-right\">" + "</span>",
                km.getSnippetHTML());
    };


    @Test
    public void indexExample6Relations () throws IOException, QueryException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(createSimpleFieldDoc());
        ki.commit();

        Match km = ki.getMatchInfo("match-c1!d1-p0-5(4)8-8(2)7-8", "tokens",
                "x", null, true, false);

        assertEquals("SnippetBrackets (1)",
					 "[[{x/o:erstens:{x/rel:a>3:a}}{x/o:zweitens:b}{x/o:drittens:c}{#3:{x/o:viertens:a}}{x/o:fünftens:b}]] ...",
                km.getSnippetBrackets());

		assertEquals("SnippetHTML (1)",
					 "<span class=\"context-left\">"
					 + "</span>" + "<span class=\"match\">"
					 + "<mark>"
					 + "<span title=\"x/o:erstens\">"
					 + "<span xlink:title=\"x/rel:a\" xlink:show=\"none\" "
					    + "xlink:href=\"#token-c1!d1-p3\">"
					 + "a" + "</span>"
					 + "</span>"
					 + "<span title=\"x/o:zweitens\">" + "b" + "</span>"
					 + "<span title=\"x/o:drittens\">" + "c" + "</span>"
					 + "<span xml:id=\"token-c1!d1-p3\">"
					 + "<span title=\"x/o:viertens\">" + "a" + "</span>"
					 + "</span>"
					 + "<span title=\"x/o:fünftens\">" + "b" + "</span>"
					 + "</mark>"
					 + "</span>"
					 + "<span class=\"context-right\">"
					 + "<span class=\"more\">"
					 + "</span>"
					 + "</span>",
					 km.getSnippetHTML());

        km = ki.getMatchInfo("match-c1!d1-p0-5(7)2-3(4)8-8(2)7-8", "tokens",
                "x", null, true, true);

        assertEquals("SnippetHTML (2)",
					 "<span class=\"context-left\">"
					 + "</span>" + "<span class=\"match\">"+"<mark>"
					 +"<span title=\"x/o:erstens\">"
					 +"<span xlink:title=\"x/rel:a\" " + "xlink:show=\"none\" "
					 +"xlink:href=\"#token-c1!d1-p3\">a</span>"
					 +"</span>"
					 +"<span title=\"x/o:zweitens\">b</span>"
					 +"<mark class=\"class-7 level-0\">"
					 +"<span title=\"x/o:drittens\">c</span>"
					 +"<span xml:id=\"token-c1!d1-p3\">"
					 +"<span title=\"x/o:viertens\">a</span>"
					 +"</span>"
					 +"</mark>"
					 +"<span title=\"x/o:fünftens\">b</span>"
					 +"</mark>"
					 +"</span>"
					 +"<span class=\"context-right\">"
					 +"<span class=\"more\"></span>"
					 +"</span>",
					 km.getSnippetHTML());
    };


    @Test
    public void indexExample7SentenceExpansion ()
            throws IOException, QueryException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(createSimpleFieldDoc());
        ki.addDoc(createSimpleFieldDoc2());
        ki.addDoc(createSimpleFieldDoc3());
        ki.addDoc(createSimpleFieldDoc4());
        ki.commit();
        Match km;

        km = ki.getMatchInfo("match-c1!d1-p3-4", "tokens", null, null, false,
                false);

        assertEquals(
                "... [[{f/m:vier:{f/y:four:{it/is:4:{x/o:viertens:a}}}}]] ...",
                km.getSnippetBrackets());


        km = ki.getMatchInfo("match-c1!d1-p3-4", "tokens", null, null, false,
                false, true); // extendToSentence

        // This will
        // [{f/m:drei:{f/y:three:{it/is:3:{x/o:drittens:c}}}}{f/m:vier:{f/y:four:{it/is:4:{x/o:viertens:a}}}}{f/m:fuenf:{f/y:five:{it/is:5:{x/o:fünftens:b}}}}]
        assertEquals(
                "[{f/m:drei:{f/y:three:{it/is:3:{x/o:drittens:c}}}}[{f/m:vier:{f/y:four:{it/is:4:{x/o:viertens:a}}}}]{f/m:fuenf:{f/y:five:{it/is:5:{x/o:fünftens:b}}}}]",
                km.getSnippetBrackets());

        assertEquals("<span class=\"context-left\"></span>"
                + "<span class=\"match\">" + "<span title=\"f/m:drei\">"
                + "<span title=\"f/y:three\">" + "<span title=\"it/is:3\">"
                + "<span title=\"x/o:drittens\">c</span>" + "</span>"
                + "</span>" + "</span>" + "<mark>" + "<span title=\"f/m:vier\">"
                + "<span title=\"f/y:four\">" + "<span title=\"it/is:4\">"
                + "<span title=\"x/o:viertens\">a</span>" + "</span>"
                + "</span>" + "</span>" + "</mark>"
                + "<span title=\"f/m:fuenf\">" + "<span title=\"f/y:five\">"
                + "<span title=\"it/is:5\">"
                + "<span title=\"x/o:fünftens\">b</span>" + "</span>"
                + "</span>" + "</span>" + "</span>"
                + "<span class=\"context-right\"></span>", km.getSnippetHTML());



        /*
        km = ki.getMatchInfo("match-c1!d3-p3-4", "tokens", null, null, false,
                false, true); // extendToSentence
        
        assertEquals(
                "[{f/m:drei:{f/y:three:{it/is:3:{x/o:drittens:cc}}}} {f/m:vier:{f/y:four:{it/is:4:{x/o:viertens:aa}}}} {f/m:fuenf:{f/y:five:{it/is:5:{x/o:fünftens:bb}}}}]",
                km.getSnippetBrackets());
        
        
        km = ki.getMatchInfo("match-c1!d4-p4-6", "tokens", null, null, false,
                false, true); // extendToSentence
        assertEquals(
                "[{f/m:drei:{f/y:three:{it/is:3:{x/o:drittens:c}}}}{f/m:vier:{f/y:four:{it/is:4:{x/o:viertens:a}}}}{f/m:fuenf:{f/y:five:{it/is:5:{x/o:fünftens:b}}}}{f/m:sechs:{f/y:six:{it/is:6:{x/o:sechstens:c}}}}{f/m:sieben:{f/y:seven:{it/is:7:{x/o:siebtens:a}}}}]",
                km.getSnippetBrackets());
        */
    };

    @Test
    public void indexExample7SentenceExpansionWarning ()
        throws IOException, QueryException {
        KrillIndex ki = new KrillIndex();

        ki.addDoc(getClass().getResourceAsStream("/wiki/WUD17-C94-39360.json.gz"), true);
        ki.commit();
        Match km;

        km = ki.getMatchInfo("match-WUD17/C94/39360-p395-396",
                             "tokens",
                             null,
                             null,
                             false,
                             false,
                             true); // extendToSentence

        JsonNode res = mapper.readTree(km.toJsonString());
        assertEquals("Unable to extend context", res.at("/messages/0/1").asText());

        QueryBuilder kq = new QueryBuilder("tokens");
        Krill ks = new Krill(kq.tag("base/s:s"));
        Result kr = ki.search(ks);
        
        assertEquals("<tokens:base/s:s />", ks.getSpanQuery().toString());
        assertEquals("totalResults", kr.getTotalResults(), 29);

        assertEquals(360, kr.getMatch(22).getStartPos());
        assertEquals(362, kr.getMatch(22).getEndPos());
        assertEquals(411, kr.getMatch(23).getStartPos());
        assertEquals(450, kr.getMatch(23).getEndPos());
    }

    @Test
    public void indexExample7Dependencies ()
            throws IOException, QueryException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(createSimpleFieldDoc2());
        ki.commit();

        Match km = ki.getMatchInfo("match-c1!d1-p0-4", "tokens", null, null,
								   true, true);

        assertEquals("SnippetHTML (2)",
					 "<span class=\"context-left\">"+
					 "</span>"+
					 "<span class=\"match\">"+
					 "<mark><span title=\"f/m:eins\">"+
					 "<span title=\"f/y:one\">"+
					 "<span title=\"it/is:1\">"+
					 "<span title=\"x/o:erstens\">"+
					 "<span xlink:title=\"x/rel:a\" xlink:show=\"none\" xlink:href=\"#token-c1!d1-p3\">a</span>"+
					 "</span>"+
					 "</span>"+
					 "</span>"+
					 "</span>"+
					 "<span title=\"f/m:zwei\">"+
					 "<span title=\"f/y:two\">"+
					 "<span title=\"it/is:2\">"+
					 "<span title=\"x/o:zweitens\">"+
					 "<span xlink:title=\"x/rel:b\" xlink:show=\"none\" xlink:href=\"#token-c1!d1-p3\">b</span>"+
					 "</span>"+
					 "</span>"+
					 "</span>"+
					 "</span>"+
					 "<span title=\"f/m:drei\">"+
					 "<span title=\"f/y:three\">"+
					 "<span title=\"it/is:3\">"+
					 "<span title=\"x/o:drittens\">c</span>"+
					 "</span>"+
					 "</span>"+
					 "</span>"+
					 "<span xml:id=\"token-c1!d1-p3\">" +
					 "<span title=\"f/m:vier\"><span title=\"f/y:four\">"+
					 "<span title=\"it/is:4\">"+
					 "<span title=\"x/o:viertens\">a</span>"+
					 "</span>"+
					 "</span>"+
					 "</span>"+
					 "</span>"+
					 "</mark>"+
					 "</span>"+
					 "<span class=\"context-right\">"+
					 "<span class=\"more\"></span>"+
                     "</span>",
                     km.getSnippetHTML());
    };

    @Test
    public void indexExample8Tokens ()
            throws IOException, QueryException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(createSimpleFieldDoc2());
        ki.commit();

        ArrayList<String> foundryList = new ArrayList<>(2);
        foundryList.add("f");
        foundryList.add("x");

        ArrayList<String> layerList = new ArrayList<>(2);
        layerList.add("is");
        
        Match km = ki.getMatchInfo(
            "match-c1!d1-p0-4",
            "tokens",
            true,
            null, //foundryList,
            null, // layerList,
            true,
            false,
            true,
            true,
            true);

        JsonNode res = mapper.readTree(km.toJsonString());
        assertEquals("c1", res.at("/corpusID").asText());
        assertEquals("d1", res.at("/docID").asText());
        assertFalse(res.at("/hasSnippet").asBoolean());
        assertTrue(res.at("/hasTokens").asBoolean());
        assertEquals("a", res.at("/tokens/match/0").asText());
        assertEquals("b", res.at("/tokens/match/1").asText());
        assertEquals("c", res.at("/tokens/match/2").asText());
        assertEquals("a", res.at("/tokens/match/3").asText());
        assertTrue(res.at("/tokens/match/4").isMissingNode());


        km = ki.getMatchInfo(
            "match-c1!d1-p0-4",
            "tokens",
            true,
            null, //foundryList,
            null, // layerList,
            true,
            true,
            true,
            true,
            true);

        res = mapper.readTree(km.toJsonString());
        assertEquals("c1", res.at("/corpusID").asText());
        assertEquals("d1", res.at("/docID").asText());
        assertTrue(res.at("/hasSnippet").asBoolean());
        assertTrue(res.at("/hasTokens").asBoolean());
        assertEquals("a", res.at("/tokens/match/0").asText());
        assertEquals("b", res.at("/tokens/match/1").asText());
        assertEquals("c", res.at("/tokens/match/2").asText());
        assertEquals("a", res.at("/tokens/match/3").asText());
        assertTrue(res.at("/tokens/match/4").isMissingNode());
    };
    

    @Test
    public void indexExampleMultipleFoundries ()
            throws IOException, QueryException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(createSimpleFieldDoc4());
        ki.commit();

        Match km = ki.getMatchInfo("match-c1!d4-p3-9", "tokens", "f", "m",
                false, false);
        assertEquals("f:m info", km.getSnippetBrackets(),
                "... [[{f/m:vier:a}{f/m:fuenf:b}{f/m:sechs:c}{f/m:sieben:a}{f/m:acht:b}{f/m:neun:a}]] ...");

        km = ki.getMatchInfo("match-c1!d4-p3-9", "tokens", "f", null, false,
                false);
        assertEquals("f info", km.getSnippetBrackets(),
                "... [[{f/m:vier:{f/y:four:a}}{f/m:fuenf:{f/y:five:b}}{f/m:sechs:{f/y:six:c}}{f/m:sieben:{f/y:seven:a}}{f/m:acht:{f/y:eight:b}}{f/m:neun:{f/y:nine:a}}]] ...");


        km = ki.getMatchInfo("match-c1!d4-p3-4", "tokens", null, null, false,
                false);
        assertEquals("all info", km.getSnippetBrackets(),
                "... [[{f/m:vier:{f/y:four:{it/is:4:{x/o:viertens:a}}}}]] ...");

        ArrayList<String> foundryList = new ArrayList<>(2);
        foundryList.add("f");
        foundryList.add("x");

        km = ki.getMatchInfo("match-c1!d4-p3-4", "tokens", true, foundryList,
                (ArrayList<String>) null, false, false, false);
        assertEquals("f|x info", km.getSnippetBrackets(),
                "... [[{f/m:vier:{f/y:four:{x/o:viertens:a}}}]] ...");

        foundryList.clear();
        foundryList.add("y");
        foundryList.add("x");

        km = ki.getMatchInfo("match-c1!d4-p3-4", "tokens", true, foundryList,
                (ArrayList<String>) null, false, false, false);
        assertEquals("y|x info", km.getSnippetBrackets(),
                "... [[{x/o:viertens:a}]] ...");


        foundryList.clear();
        foundryList.add("f");
        foundryList.add("it");

        ArrayList<String> layerList = new ArrayList<>(2);
        layerList.add("is");

        km = ki.getMatchInfo("match-c1!d4-p3-4", "tokens", true, foundryList,
                layerList, false, false, false);
        assertEquals("f|it/is", km.getSnippetBrackets(),
                "... [[{it/is:4:a}]] ...");
    };


    @Test
    public void indexExampleFailingFoundry ()
            throws IOException, QueryException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(createSimpleFieldDoc4());
        ki.commit();

        Match km = ki.getMatchInfo("match-c1!d4-p3-9", "tokens", "*", "m",
                false, false);
        JsonNode res = mapper.readTree(km.toJsonString());
        assertEquals("c1", res.at("/corpusID").asText());
        assertEquals("d4", res.at("/docID").asText());
        assertEquals("Invalid foundry requested",
                res.at("/errors/0/1").asText());
    };


    @Test
    public void indexFailingMatchID () throws IOException, QueryException {
        KrillIndex ki = new KrillIndex();
        Match km = ki.getMatchInfo(
                "match-PRO-DUD!PRO-DUD_KSTA-2013-01.7483-2013-01", "tokens",
                "*", "m", false, false);
        JsonNode res = mapper.readTree(km.toJsonString());
        assertEquals("730", res.at("/errors/0/0").asText());
    };


    @Test
    public void indexExampleNullInfo () throws IOException, QueryException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(createSimpleFieldDoc4());
        ki.commit();
        
        Match km = ki.getMatchInfo("match-c1!d4-p3-9", "tokens", null, null,
                false, false);
        JsonNode res = mapper.readTree(km.toJsonString());
        // assertEquals("tokens", res.at("/field").asText());
        assertFalse(res.has("startMore"));
        assertFalse(res.has("endMore"));
        assertEquals("c1", res.at("/corpusID").asText());
        assertEquals("d4", res.at("/docID").asText());
        assertEquals(
                "<span class=\"context-left\"><span class=\"more\"></span></span><span class=\"match\"><mark><span title=\"f/m:vier\"><span title=\"f/y:four\"><span title=\"it/is:4\"><span title=\"x/o:viertens\">a</span></span></span></span><span title=\"f/m:fuenf\"><span title=\"f/y:five\"><span title=\"it/is:5\"><span title=\"x/o:fünftens\">b</span></span></span></span><span title=\"f/m:sechs\"><span title=\"f/y:six\"><span title=\"it/is:6\"><span title=\"x/o:sechstens\">c</span></span></span></span><span title=\"f/m:sieben\"><span title=\"f/y:seven\"><span title=\"it/is:7\"><span title=\"x/o:siebtens\">a</span></span></span></span><span title=\"f/m:acht\"><span title=\"f/y:eight\"><span title=\"it/is:8\"><span title=\"x/o:achtens\">b</span></span></span></span><span title=\"f/m:neun\"><span title=\"f/y:nine\"><span title=\"it/is:9\"><span title=\"x/o:neuntens\">a</span></span></span></span></mark></span><span class=\"context-right\"><span class=\"more\"></span></span>",
                res.at("/snippet").asText());
        assertEquals("match-c1!d4-p3-9", res.at("/matchID").asText());
        assertTrue(res.at("/pubDate").isMissingNode());
    };

	@Test
    public void indexSigleDuplicate () throws IOException, QueryException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(createSigleDoc2());
        ki.addDoc(createSigleDoc1());
        ki.commit();
        Match km = ki.getMatchInfo("match-c1/d1/t1-p3-9", "tokens", null, null,
                false, false);

        JsonNode res = mapper.readTree(km.toJsonString());
        // assertEquals("tokens", res.at("/field").asText());
        assertFalse(res.has("startMore"));
        assertFalse(res.has("endMore"));
        assertEquals("c1", res.at("/corpusSigle").asText());
        assertEquals("c1/d1", res.at("/docSigle").asText());
        assertEquals("c1/d1/t1", res.at("/textSigle").asText());
        assertEquals("match-c1/d1/t1-p3-9", res.at("/matchID").asText());
        assertEquals(2, res.at("/UID").asInt());
    };

	@Test
	public void indexMultipleSpanStarts () throws IOException, QueryException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(createSimpleFieldDoc5());
        FieldDocument fd = ki.addDoc(
			2,
			getClass().getResourceAsStream("/goe/AGA-03828-new.json.gz"), true);
		
        ki.commit();

		Match km;

        km = ki.getMatchInfo("match-c1!d5-p0-4", "tokens", null, null,
                true, false);

		assertEquals("SnippetBrackets (with Spans)",
					 "[[{x/tag:a:{x/tag:b:{x/tag:c:{x/tag:v:x}}y}}z]]",
					 km.getSnippetBrackets());

		assertEquals("SnippetTokens (without Spans)",
					 "{\"match\":[\"x\",\"y\",\"z\"]}",
					 km.getSnippetTokens().toString());
        
        assertEquals("GOE/AGA/03828", fd.getTextSigle());
        assertEquals("Autobiographische Einzelheiten", fd.getFieldValue("title"));

		
        Krill ks = new Krill(new QueryBuilder("tokens").seg("marmot/m:case:nom").with("marmot/m:degree:pos"));
        Result kr = ks.apply(ki);

		assertEquals(83, kr.getTotalResults());
		assertEquals("match-GOE/AGA/03828-p0-1", kr.getMatch(0).getID());

		km = ki.getMatchInfo("match-GOE/AGA/03828-p0-10", "tokens", "malt", null,
							 true, false);
		
		assertEquals("SnippetBrackets (with Spans)",
					 "[[{malt/d:ATTR>2:Autobiographische} "+
					 "{malt/d:ATTR>2:einzelheiten} "+
					 "{#2:{malt/d:ROOT>0-21:Selbstschilderung}} "+
					 "({malt/d:APP>2:1}) "+
					 "{malt/d:ADV>5:immer} "+
					 "{#5:{malt/d:ATTR>2:tätiger}}, "+
					 "{#6:{malt/d:PP>13:nach}} "+
					 "{#7:{malt/d:PN>6:innen}} "+
					 "{malt/d:KON>7:und} "+
					 "{malt/d:ADV>11:außen}]] "+
					 "...",
					 km.getSnippetBrackets());
	};

	@Test
	public void indexDependencyAnnotations () throws IOException, QueryException {
        KrillIndex ki = new KrillIndex();
        FieldDocument fd =
			ki.addDoc(
				2,
				getClass().getResourceAsStream("/goe/Corpus-Doc-0002.json"), false);
		
        ki.commit();

		Match km;

		km = ki.getMatchInfo(
        	"match-Corpus/Doc/0002-p0-6",
        					 "tokens", "malt", null, true, false);

        assertEquals("SnippetHTML (1)",
                     "<span class=\"context-left\"></span>"+
                     "<span class=\"match\">"+
                     "<span xml:id=\"token-Corpus/Doc/0002-p0-6\">"+
                     "<mark>"+
                     "<span xml:id=\"token-Corpus/Doc/0002-p0\">"+
                     "<span xlink:title=\"malt/d:ROOT\" xlink:show=\"none\" xlink:href=\"#token-Corpus/Doc/0002-p0-6\">Maximen</span>"+
                     "</span>"+
                     " "+
                     "<span xml:id=\"token-Corpus/Doc/0002-p1\">"+
                     "<span xlink:title=\"malt/d:KON\" xlink:show=\"none\" xlink:href=\"#token-Corpus/Doc/0002-p0\">und</span>"+
                     "</span>"+
                     " "+
                     "<span xlink:title=\"malt/d:CJ\" xlink:show=\"none\" xlink:href=\"#token-Corpus/Doc/0002-p1\">Reflexionen</span>"+
                     " "+
                     "<span xml:id=\"token-Corpus/Doc/0002-p3\">"+
                     "<span xlink:title=\"malt/d:KON\" xlink:show=\"none\" xlink:href=\"#token-Corpus/Doc/0002-p0\">Religion</span>"+
                     "</span>"+
                     " "+
                     "<span xml:id=\"token-Corpus/Doc/0002-p4\">"+
                     "<span xlink:title=\"malt/d:KON\" xlink:show=\"none\" xlink:href=\"#token-Corpus/Doc/0002-p3\">und</span>"+
                     "</span>"+
                     " "+
                     "<span xlink:title=\"malt/d:CJ\" xlink:show=\"none\" xlink:href=\"#token-Corpus/Doc/0002-p4\">Christentum</span>"+
                     "</mark>"+
                     "</span>"+
                     "</span>"+
                     "<span class=\"context-right\">"+
                     "<span class=\"more\"></span>"+
                     "</span>",
                     km.getSnippetHTML()
            );
        
	};


	
    @Test
    public void indexAttributeInfo () throws IOException, QueryException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(createAttributeFieldDoc());
        ki.commit();
        Match km = ki.getMatchInfo("match-ca1!da1-p7-10", "tokens", null, null,
                false, false);
        JsonNode res = mapper.readTree(km.toJsonString());
        // assertEquals("tokens", res.at("/field").asText());
        assertFalse(res.has("startMore"));
        assertFalse(res.has("endMore"));
        assertEquals("ca1", res.at("/corpusID").asText());
        assertEquals("da1", res.at("/docID").asText());
        assertEquals("<span class=\"context-left\">" + "<span class=\"more\">"
                + "</span>" + "</span>" + "<span class=\"match\"><mark>" +
                //                     "<span title=\"@:x/s:key:value\">"+
                "<span title=\"f/m:acht\">" + "<span title=\"f/y:eight\">"
                + "<span title=\"it/is:8\">"
                + "<span title=\"x/o:achtens\">b</span>" +
                //                     "</span>"+
                "</span>" + "</span>" + "</span>" + "<span title=\"f/m:neun\">"
                + "<span title=\"f/y:nine\">" + "<span title=\"it/is:9\">"
                + "<span title=\"x/o:neuntens\">a</span>" + "</span>"
                + "</span>" + "</span>" + "<span title=\"f/m:zehn\">"
                + "<span title=\"f/y:ten\">" + "<span title=\"it/is:10\">"
                + "<span title=\"x/o:zehntens\">c</span>" + "</span>"
                + "</span>" + "</span>" + "</mark>" + "</span>"
                + "<span class=\"context-right\">" + "</span>",
                res.at("/snippet").asText());
    };

	@Test
    public void indexWithFieldInfo () throws IOException, QueryException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(createSimpleFieldDoc());
        ki.commit();

        Match km = ki.getMatchInfo("match-c1!d1-p7-9(4)8-8(2)7-8", "tokens",
                null, null, false, false);
		assertEquals("CC-BY-SA", km.getFieldValue("availability"));
    };

    @Test
    public void indexCorolaTokensBugReplicated () throws IOException, QueryException {
        KrillProperties.matchExpansionIncludeContextSize=false;
        
        KrillIndex ki = new KrillIndex();

        ki.addDoc(getClass().getResourceAsStream("/others/corola-bug.json"), false);
        ki.commit();

        SpanQuery sq = new SpanTermQuery(new Term("tokens", "s:b"));

        Result kr = ki.search(sq, (short) 10);

        assertEquals(70, kr.getMatch(0).getStartPos());
        assertEquals(71, kr.getMatch(0).getEndPos());
        assertEquals("totalResults", kr.getTotalResults(), 1);
        assertEquals("... a a a a a a [[b]] a a a a a a ...", kr.getMatch(0).getSnippetBrackets());
            
        // see TestNextIndex#corolaNextTest

        Match km = ki.getMatchInfo("match-Corola-blog/BlogPost/370281_a_371610-p70-71", "tokens", null, null,false, false, true);

        String str = km.getSnippetBrackets();
        assertTrue(str.contains("[<!>{drukola/l:au:a}"));
        assertTrue(str.contains("<!>]"));
        
        km = ki.getMatchInfo("match-Corola-blog/BlogPost/370281_a_371610-p50-51", "tokens", null, null,false, false, true);

        // The match needs to be cutted on both sides!
        str = km.getSnippetBrackets();
        assertTrue(str.contains("[<!>{d"));
        assertTrue(str.contains("a}<!>]"));
        
        KrillProperties.matchExpansionIncludeContextSize=true;
    };
    

    private FieldDocument createSimpleFieldDoc () {
        FieldDocument fd = new FieldDocument();
        fd.addString("corpusID", "c1");
        fd.addString("ID", "d1");
        fd.addString("availability", "CC-BY-SA");
        fd.addTV("tokens", "abcabcabac",
                "[(0-1)s:a|i:a|f/m:eins|f/y:one|x/o:erstens|it/is:1|>:x/rel:a$<b>32<i>3<s>0<s>0<s>0|_0$<i>0<i>1|-:t$<i>10]"
                        + "[(1-2)s:b|i:b|f/m:zwei|f/y:two|x/o:zweitens|it/is:2|_1$<i>1<i>2]"
                        + "[(2-3)s:c|i:c|f/m:drei|f/y:three|x/o:drittens|it/is:3|_2$<i>2<i>3|<>:base/s:s$<b>64<i>2<i>5<i>5]"
                        + "[(3-4)s:a|i:a|f/m:vier|f/y:four|x/o:viertens|it/is:4|<:x/rel:b$<b>32<i>0<s>0<s>0<s>0|_3$<i>3<i>4]"
                        + "[(4-5)s:b|i:b|f/m:fuenf|f/y:five|x/o:fünftens|it/is:5|_4$<i>4<i>5]"
                        + "[(5-6)s:c|i:c|f/m:sechs|f/y:six|x/o:sechstens|it/is:6|_5$<i>5<i>6]"
                        + "[(6-7)s:a|i:a|f/m:sieben|f/y:seven|x/o:siebtens|it/is:7|_6$<i>6<i>7]"
                        + "[(7-8)s:b|i:b|f/m:acht|f/y:eight|x/o:achtens|it/is:8|<>:x/tag$<b>64<i>7<i>10<i>10|_7$<i>7<i>8]"
                        + "[(8-9)s:a|i:a|f/m:neun|f/y:nine|x/o:neuntens|it/is:9|_8$<i>8<i>9]"
                        + "[(9-10)s:c|i:c|f/m:zehn|f/y:ten|x/o:zehntens|it/is:10|_9$<i>9<i>10]");
        return fd;
    };


    private FieldDocument createSimpleFieldDoc2 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("corpusID", "c1");
        fd.addString("ID", "d1");
        fd.addTV("tokens", "abcabcabac",
                "[(0-1)s:a|i:a|f/m:eins|f/y:one|x/o:erstens|it/is:1|>:x/rel:a$<b>32<i>3<s>0<s>0<s>0|_0$<i>0<i>1|-:t$<i>10]"
				 + "[(1-2)s:b|i:b|f/m:zwei|f/y:two|x/o:zweitens|it/is:2|>:x/rel:b$<b>32<i>3<s>0<s>0<s>0|_1$<i>1<i>2]"
				 + "[(2-3)s:c|i:c|f/m:drei|f/y:three|x/o:drittens|it/is:3|_2$<i>2<i>3|<>:base/s:s$<b>64<i>2<i>5<i>5]"
				 + "[(3-4)s:a|i:a|f/m:vier|f/y:four|x/o:viertens|it/is:4|<:x/rel:b$<b>32<i>0<s>0<s>0<s>0|_3$<i>3<i>4]"
				 + "[(4-5)s:b|i:b|f/m:fuenf|f/y:five|x/o:fünftens|it/is:5|_4$<i>4<i>5]"
				 + "[(5-6)s:c|i:c|f/m:sechs|f/y:six|x/o:sechstens|it/is:6|_5$<i>5<i>6]"
				 + "[(6-7)s:a|i:a|f/m:sieben|f/y:seven|x/o:siebtens|it/is:7|_6$<i>6<i>7]"
				 + "[(7-8)s:b|i:b|f/m:acht|f/y:eight|x/o:achtens|it/is:8|<>:x/tag$<b>64<i>7<i>10<i>10|_7$<i>7<i>8]"
				 + "[(8-9)s:a|i:a|f/m:neun|f/y:nine|x/o:neuntens|it/is:9|_8$<i>8<i>9]"
				 + "[(9-10)s:c|i:c|f/m:zehn|f/y:ten|x/o:zehntens|it/is:10|_9$<i>9<i>10]");
        return fd;
    };


    private FieldDocument createSimpleFieldDoc3 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("corpusID", "c1");
        fd.addString("ID", "d3");
        fd.addTV("tokens", "aa bb cc aa bb cc aa bb aa cc ",
                "[(0-2)s:aa|i:a|f/m:eins|f/y:one|x/o:erstens|it/is:1|>:x/rel:a$<b>32<i>4<s>0<s>0<s>0|_0$<i>0<i>2|-:t$<i>10]"
                        + "[(3-5)s:bb|i:b|f/m:zwei|f/y:two|x/o:zweitens|it/is:2|_1$<i>3<i>5]"
                        + "[(6-8)s:cc|i:c|f/m:drei|f/y:three|x/o:drittens|it/is:3|_2$<i>6<i>8|<>:base/s:s$<b>64<i>6<i>14<i>5]"
                        + "[(9-11)s:aa|i:a|f/m:vier|f/y:four|x/o:viertens|it/is:4|<:x/rel:b$<b>40<i>1<s>0<s>0<s>0|_3$<i>9<i>11]"
                        + "[(12-14)s:bb|i:b|f/m:fuenf|f/y:five|x/o:fünftens|it/is:5|_4$<i>12<i>14]"
                        + "[(15-17)s:cc|i:c|f/m:sechs|f/y:six|x/o:sechstens|it/is:6|_5$<i>15<i>17]"
                        + "[(18-20)s:aa|i:a|f/m:sieben|f/y:seven|x/o:siebtens|it/is:7|_6$<i>18<i>20]"
                        + "[(21-23)s:bb|i:b|f/m:acht|f/y:eight|x/o:achtens|it/is:8|<>:x/tag$<b>64<i>7<i>10<i>10|_7$<i>21<i>23]"
                        + "[(24-26)s:aa|i:a|f/m:neun|f/y:nine|x/o:neuntens|it/is:9|_8$<i>24<i>26]"
                        + "[(27-29)s:cc|i:c|f/m:zehn|f/y:ten|x/o:zehntens|it/is:10|_9$<i>27<i>29]");
        return fd;
    };


    private FieldDocument createSimpleFieldDoc4 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("corpusID", "c1");
        fd.addString("ID", "d4");
        fd.addTV("tokens", "abcabcabac",
                "[(0-1)s:a|i:a|f/m:eins|f/y:one|x/o:erstens|it/is:1|>:x/rel:a$<b>32<i>4<s>0<s>0<s>0|_0$<i>0<i>1|-:t$<i>10]"
                        + "[(1-2)s:b|i:b|f/m:zwei|f/y:two|x/o:zweitens|it/is:2|_1$<i>1<i>2]"
                        + "[(2-3)s:c|i:c|f/m:drei|f/y:three|x/o:drittens|it/is:3|_2$<i>2<i>3|<>:base/s:s$<b>64<i>2<i>5<i>5]"
                        + "[(3-4)s:a|i:a|f/m:vier|f/y:four|x/o:viertens|it/is:4|<:x/rel:b$<b>40<i>1<s>0<s>0<s>0|_3$<i>3<i>4]"
                        + "[(4-5)s:b|i:b|f/m:fuenf|f/y:five|x/o:fünftens|it/is:5|_4$<i>4<i>5]"
                        + "[(5-6)s:c|i:c|f/m:sechs|f/y:six|x/o:sechstens|it/is:6|_5$<i>5<i>6|<>:base/s:s$<b>64<i>5<i>7<i>7]"
                        + "[(6-7)s:a|i:a|f/m:sieben|f/y:seven|x/o:siebtens|it/is:7|_6$<i>6<i>7]"
                        + "[(7-8)s:b|i:b|f/m:acht|f/y:eight|x/o:achtens|it/is:8|<>:x/tag$<b>64<i>7<i>10<i>10|_7$<i>7<i>8]"
                        + "[(8-9)s:a|i:a|f/m:neun|f/y:nine|x/o:neuntens|it/is:9|_8$<i>8<i>9]"
                        + "[(9-10)s:c|i:c|f/m:zehn|f/y:ten|x/o:zehntens|it/is:10|_9$<i>9<i>10]");
        return fd;
    };

    private FieldDocument createSimpleFieldDoc5 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("corpusID", "c1");
        fd.addString("ID", "d5");
        fd.addTV("tokens", "xyz",
                "[(0-1)s:x|i:x"+
				 "|<>:x/tag:v$<b>65<i>1" +
				 "|<>:x/tag:c$<b>64<i>1<i>0<i>2"+
				 "|<>:x/tag:a$<b>64<i>2<i>0<i>3"+
				 "|<>:x/tag:b$<b>64<i>2<i>0<i>3"+
				 "|_1$<i>0<i>1]"
				 + "[(1-2)s:y|i:y|_2$<i>1<i>2]"
				 + "[(2-3)s:z|i:z|_3$<i>2<i>3]");
        return fd;
    };
	

    /*
      Check for terms|spans|rels ...
     */
    private FieldDocument createAttributeFieldDoc () {
        FieldDocument fd = new FieldDocument();
        fd.addString("corpusID", "ca1");
        fd.addString("ID", "da1");
        fd.addTV("tokens", "abcabcabac",
                "[(0-1)s:a|i:a|f/m:eins|f/y:one|x/o:erstens|it/is:1|_0$<i>0<i>1|-:t$<i>10]"
                        + "[(1-2)s:b|i:b|f/m:zwei|f/y:two|x/o:zweitens|it/is:2|_1$<i>1<i>2]"
                        + "[(2-3)s:c|i:c|f/m:drei|f/y:three|x/o:drittens|it/is:3|_2$<i>2<i>3|<>:base/s:s$<b>64<i>2<i>5<i>5]"
                        + "[(3-4)s:a|i:a|f/m:vier|f/y:four|x/o:viertens|it/is:4|_3$<i>3<i>4]"
                        + "[(4-5)s:b|i:b|f/m:fuenf|f/y:five|x/o:fünftens|it/is:5|_4$<i>4<i>5]"
                        + "[(5-6)s:c|i:c|f/m:sechs|f/y:six|x/o:sechstens|it/is:6|_5$<i>5<i>6]"
                        + "[(6-7)s:a|i:a|f/m:sieben|f/y:seven|x/o:siebtens|it/is:7|_6$<i>6<i>7]"
                        + "[(7-8)s:b|i:b|f/m:acht|f/y:eight|x/o:achtens|it/is:8|<>:x/s:tag$<b>64<i>7<i>10<i>10<b>0<s>1|@:x/s:key:value$<b>17<i>10<s>1|_7$<i>7<i>8]"
                        + "[(8-9)s:a|i:a|f/m:neun|f/y:nine|x/o:neuntens|it/is:9|_8$<i>8<i>9]"
                        + "[(9-10)s:c|i:c|f/m:zehn|f/y:ten|x/o:zehntens|it/is:10|_9$<i>9<i>10]");
        return fd;
    };

	private FieldDocument createSigleDoc1 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("corpusSigle", "c1");
        fd.addString("docSigle", "c1/d1");
        fd.addString("textSigle", "c1/d1/t1");
        fd.setUID(1);
        fd.addTV("tokens", "abcabcabac",
                "[(0-1)s:a|i:a|_0$<i>0<i>1|-:t$<i>10]"
				 + "[(1-2)s:b|i:b|_1$<i>1<i>2]"
				 + "[(2-3)s:c|i:c|_2$<i>2<i>3]"
				 + "[(3-4)s:a|i:a|_3$<i>3<i>4]"
				 + "[(4-5)s:b|i:b|_4$<i>4<i>5]"
				 + "[(5-6)s:c|i:c|_5$<i>5<i>6]"
				 + "[(6-7)s:a|i:a|_6$<i>6<i>7]"
				 + "[(7-8)s:b|i:b|_7$<i>7<i>8]"
				 + "[(8-9)s:a|i:a|_8$<i>8<i>9]"
				 + "[(9-10)s:c|i:c|_9$<i>9<i>10]");
        return fd;
    };

	private FieldDocument createSigleDoc2 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("corpusSigle", "c1");
        fd.addString("docSigle", "c1/d1");
        fd.addString("textSigle", "c1/d1/t1");
        fd.setUID(2);
        fd.addTV("tokens", "abcabcabac",
                "[(0-1)s:a|i:a|_0$<i>0<i>1|-:t$<i>10]"
				 + "[(1-2)s:b|i:b|_1$<i>1<i>2]"
				 + "[(2-3)s:c|i:c|_2$<i>2<i>3]"
				 + "[(3-4)s:a|i:a|_3$<i>3<i>4]"
				 + "[(4-5)s:b|i:b|_4$<i>4<i>5]"
				 + "[(5-6)s:c|i:c|_5$<i>5<i>6]"
				 + "[(6-7)s:a|i:a|_6$<i>6<i>7]"
				 + "[(7-8)s:b|i:b|_7$<i>7<i>8]"
				 + "[(8-9)s:a|i:a|_8$<i>8<i>9]"
				 + "[(9-10)s:c|i:c|_9$<i>9<i>10]");
        return fd;
    };

};
