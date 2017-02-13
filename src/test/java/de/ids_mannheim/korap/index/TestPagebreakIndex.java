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
import de.ids_mannheim.korap.response.Match;
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.response.SearchContext;

/*
 * Retrieve pagebreak annotations
 */

@RunWith(JUnit4.class)
public class TestPagebreakIndex {

    @Test
    public void indexExample1 () throws IOException {
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

		SpanQuery sq = new SpanTermQuery(new Term("tokens", "s:c"));

        Result kr = ki.search(sq, (short) 10);

		assertEquals(528, kr.getMatch(0).getStartPage());
		assertEquals(-1, kr.getMatch(0).getEndPage());
		assertEquals(
			"snippetHTML",
			"<span class=\"context-left\">"+
			"<span class=\"pb\" data-after=\"528\"></span>"+
			"ab"+
			"</span>"+
			"<span class=\"match\">"+
			"<mark>"+
			"c"+
			"</mark>"+
			"</span>"+
			"<span class=\"context-right\">"+
			"ab"+
			"<span class=\"pb\" data-after=\"528\"></span>"+
			"cab"+
			"<span class=\"pb\" data-after=\"528\"></span>"+
			"a"+
			"<span class=\"more\">"+
			"</span>"+
			"</span>",
			kr.getMatch(0).getSnippetHTML());
	};
};
