package de.ids_mannheim.korap.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import de.ids_mannheim.korap.KrillCollection;
import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.query.QueryBuilder;
import de.ids_mannheim.korap.query.SpanClassQuery;
import de.ids_mannheim.korap.query.SpanElementQuery;
import de.ids_mannheim.korap.query.SpanFocusQuery;
import de.ids_mannheim.korap.query.SpanNextQuery;
import de.ids_mannheim.korap.query.SpanWithinQuery;
import de.ids_mannheim.korap.query.QueryBuilder;
import de.ids_mannheim.korap.query.wrap.SpanQueryWrapper;
import de.ids_mannheim.korap.response.Match;
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.response.SearchContext;

/*
 * Retrieve pagebreak annotations
 */

@RunWith(JUnit4.class)
public class TestPagebreakIndex {

    @Test
    public void indexExample1 () throws Exception {
		KrillIndex ki = new KrillIndex();

		// abcabcabac
		FieldDocument fd = new FieldDocument();
		fd.addTV("tokens", "abcabcabac",
				 "[(0-1)s:a|i:a|_0$<i>0<i>1|-:t$<i>10|~:base/s:pb$<i>528<i>0]" +
				 "[(1-2)s:b|i:b|_1$<i>1<i>2]" +
				 "[(2-3)s:c|i:c|_2$<i>2<i>3]" +
				 "[(3-4)s:a|i:a|_3$<i>3<i>4]" +
				 "[(4-5)s:b|i:b|_4$<i>4<i>5]" +
				 "[(5-6)s:c|i:c|_5$<i>5<i>6|~:base/s:pb$<i>529<i>5]" +
				 "[(6-7)s:a|i:a|_6$<i>6<i>7]" +
				 "[(7-8)s:b|i:b|_7$<i>7<i>8]" +
				 "[(8-9)s:a|i:a|_8$<i>8<i>9|~:base/s:pb$<i>530<i>8]" +
				 "[(9-10)s:c|i:c|_9$<i>9<i>10]");
        ki.addDoc(fd);
        ki.commit();

		SpanQuery sq;
		Result kr;

		sq = new SpanTermQuery(new Term("tokens", "s:c"));
        kr = ki.search(sq, (short) 10);

		assertEquals(2, kr.getMatch(0).getStartPos());
		assertEquals(3, kr.getMatch(0).getEndPos());
		assertEquals(528, kr.getMatch(0).getStartPage());
		assertEquals(-1, kr.getMatch(0).getEndPage());
		assertEquals(
			"snippetHTML",
			"<span class=\"context-left\">"+
			// "<span class=\"pb\" data-after=\"528\"></span>"+
			"ab"+
			"</span>"+
			"<span class=\"match\">"+
			"<mark>"+
			"c"+
			"</mark>"+
			"</span>"+
			"<span class=\"context-right\">"+
			"ab"+
			// "<span class=\"pb\" data-after=\"528\"></span>"+
			"cab"+
			// "<span class=\"pb\" data-after=\"528\"></span>"+
			"a"+
			"<span class=\"more\">"+
			"</span>"+
			"</span>",
			kr.getMatch(0).getSnippetHTML());

				/*

		QueryBuilder qb = new QueryBuilder("tokens");
		sq = qb.seq().append(
			qb.repeat(
				qb.seq().append(qb.seg("s:a")).append(qb.seg("s:b")).append(qb.seg("s:c")),
				2
				)
			).append(qb.seg("s:a"))
			.toQuery();

		assertEquals(sq.toString(), "spanNext(spanRepetition(spanNext(spanNext(tokens:s:a, tokens:s:b), tokens:s:c){2,2}), tokens:s:a)");


		kr = ki.search(sq, (short) 10);
		
		assertEquals(528, kr.getMatch(0).getStartPage());
		assertEquals(529, kr.getMatch(0).getEndPage());
		assertEquals(
			"snippetHTML",
			"<span class=\"context-left\"></span>"+
			"<span class=\"match\">"+
			"<mark>"+
			"<span class=\"pb\" data-after=\"528\"></span>"+
			"abcab"+
			"<span class=\"pb\" data-after=\"529\"></span>"+
			"ca"+
			"</mark>"+
			"</span>"+
			"<span class=\"context-right\">"+
			"bac"+
			"</span>",
			kr.getMatch(0).getSnippetHTML());
		*/
	};
};
