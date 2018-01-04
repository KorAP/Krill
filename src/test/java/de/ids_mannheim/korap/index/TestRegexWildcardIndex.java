package de.ids_mannheim.korap.index;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.search.spans.SpanQuery;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.query.wrap.SpanQueryWrapper;
import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.query.QueryBuilder;
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.query.DistanceConstraint;
import de.ids_mannheim.korap.query.SpanMultipleDistanceQuery;


@RunWith(JUnit4.class)
public class TestRegexWildcardIndex {

    @Test
    public void indexRegex () throws Exception {
        KrillIndex ki = new KrillIndex();

        // abcabcabac
        FieldDocument fd = new FieldDocument();
        fd.addTV("base",
                "affe afffe baum baumgarten steingarten franz hans haus efeu effe",
                "[(0-4)s:affe|_0$<i>0<i>4|-:t$<i>10]"
                        + "[(5-10)s:afffe|_1$<i>5<i>10]"
                        + "[(11-15)s:baum|_2$<i>11<i>15]"
                        + "[(16-26)s:baumgarten|_3$<i>16<i>26]"
                        + "[(27-38)s:steingarten|_4$<i>27<i>38]"
                        + "[(39-44)s:franz|_5$<i>39<i>44]"
                        + "[(45-49)s:hans|_6$<i>45<i>49]"
                        + "[(50-54)s:haus|_7$<i>50<i>54]"
                        + "[(55-59)s:efeu|_8$<i>55<i>59]"
                        + "[(60-64)s:effe|_9$<i>60<i>64]");
        ki.addDoc(fd);

        ki.commit();

        QueryBuilder kq = new QueryBuilder("base");
        SpanQueryWrapper sqw = kq.re("s:af*e");
        assertEquals("SpanMultiTermQueryWrapper(base:/s:af*e/)",
                sqw.toQuery().toString());

        Krill ks = _newKrill(sqw);

        Result kr = ki.search(ks);
        assertEquals((long) 2, kr.getTotalResults());
        assertEquals("[[affe]] afffe ...", kr.getMatch(0).getSnippetBrackets());
        assertEquals("affe [[afffe]] baum ...",
                kr.getMatch(1).getSnippetBrackets());

        ks = _newKrill(kq.re("s:baum.*"));
        kr = ki.search(ks);

        assertEquals((long) 2, kr.getTotalResults());
        assertEquals("... afffe [[baum]] baumgarten ...",
                kr.getMatch(0).getSnippetBrackets());
        assertEquals("... baum [[baumgarten]] steingarten ...",
                kr.getMatch(1).getSnippetBrackets());

        ks = _newKrill(kq.re("s:.....?garten"));
        kr = ki.search(ks);
        assertEquals((long) 2, kr.getTotalResults());
        assertEquals("... baum [[baumgarten]] steingarten ...",
                kr.getMatch(0).getSnippetBrackets());
        assertEquals("... baumgarten [[steingarten]] franz ...",
                kr.getMatch(1).getSnippetBrackets());

        ks = _newKrill(kq.re("s:ha.s"));
        kr = ki.search(ks);
        assertEquals((long) 2, kr.getTotalResults());
        assertEquals("... franz [[hans]] haus ...",
                kr.getMatch(0).getSnippetBrackets());
        assertEquals("... hans [[haus]] efeu ...",
                kr.getMatch(1).getSnippetBrackets());

        ks = _newKrill(kq.re("s:.*ff.*"));
        kr = ki.search(ks);
        assertEquals((long) 3, kr.getTotalResults());
        assertEquals("[[affe]] afffe ...", kr.getMatch(0).getSnippetBrackets());
        assertEquals("affe [[afffe]] baum ...",
                kr.getMatch(1).getSnippetBrackets());
        assertEquals("... efeu [[effe]]", kr.getMatch(2).getSnippetBrackets());

		SpanQueryWrapper sq = kq.seq(
			kq.re("s:.*garten")
			).append(
				kq.seg().without(
					kq.re("s:.*an.*")
					)
				);
		System.err.println(sq.toQuery().toString());
		ks = _newKrill(sq);
        kr = ki.search(ks);

        assertEquals((long) 1, kr.getTotalResults());
	};


    @Test
    public void indexWildcard () throws Exception {
        KrillIndex ki = new KrillIndex();

        // abcabcabac
        FieldDocument fd = new FieldDocument();
        fd.addTV("base",
                "affe afffe baum baumgarten steingarten franz hans haus efeu effe",
                "[(0-4)s:affe|_0$<i>0<i>4|-:t$<i>10]"
                        + "[(5-10)s:afffe|_1$<i>5<i>10]"
                        + "[(11-15)s:baum|_2$<i>11<i>15]"
                        + "[(16-26)s:baumgarten|_3$<i>16<i>26]"
                        + "[(27-38)s:steingarten|_4$<i>27<i>38]"
                        + "[(39-44)s:franz|_5$<i>39<i>44]"
                        + "[(45-49)s:hans|_6$<i>45<i>49]"
                        + "[(50-54)s:haus|_7$<i>50<i>54]"
                        + "[(55-59)s:efeu|_8$<i>55<i>59]"
                        + "[(60-64)s:effe|_9$<i>60<i>64]");
        ki.addDoc(fd);

        ki.commit();

        QueryBuilder kq = new QueryBuilder("base");
        SpanQueryWrapper sq = kq.wc("s:af*e");
        assertEquals("SpanMultiTermQueryWrapper(base:s:af*e)",
                sq.toQuery().toString());

        Krill ks = _newKrill(sq);

        Result kr = ki.search(ks);
        assertEquals((long) 2, kr.getTotalResults());
        assertEquals("[[affe]] afffe ...", kr.getMatch(0).getSnippetBrackets());
        assertEquals("affe [[afffe]] baum ...",
                kr.getMatch(1).getSnippetBrackets());

        ks = _newKrill(new QueryBuilder("base").wc("s:baum.*"));
        kr = ki.search(ks);

        assertEquals((long) 0, kr.getTotalResults());

        ks = _newKrill(new QueryBuilder("base").wc("s:baum*"));
        kr = ki.search(ks);
        assertEquals((long) 2, kr.getTotalResults());
        assertEquals("... afffe [[baum]] baumgarten ...",
                kr.getMatch(0).getSnippetBrackets());
        assertEquals("... baum [[baumgarten]] steingarten ...",
                kr.getMatch(1).getSnippetBrackets());

        ks = _newKrill(new QueryBuilder("base").wc("s:*garten"));
        kr = ki.search(ks);
        assertEquals((long) 2, kr.getTotalResults());
        assertEquals("... baum [[baumgarten]] steingarten ...",
                kr.getMatch(0).getSnippetBrackets());
        assertEquals("... baumgarten [[steingarten]] franz ...",
                kr.getMatch(1).getSnippetBrackets());

        ks = _newKrill(new QueryBuilder("base").wc("s:ha?s"));
        kr = ki.search(ks);
        assertEquals((long) 2, kr.getTotalResults());
        assertEquals("... franz [[hans]] haus ...",
                kr.getMatch(0).getSnippetBrackets());
        assertEquals("... hans [[haus]] efeu ...",
                kr.getMatch(1).getSnippetBrackets());

        ks = _newKrill(new QueryBuilder("base").wc("s:?ff?"));
        kr = ki.search(ks);
        assertEquals((long) 2, kr.getTotalResults());
        assertEquals("[[affe]] afffe ...", kr.getMatch(0).getSnippetBrackets());
        assertEquals("... efeu [[effe]]", kr.getMatch(1).getSnippetBrackets());
    };


    @Test
    public void indexRegexCaseInsensitive () throws Exception {
        KrillIndex ki = new KrillIndex();

        // abcabcabac
        FieldDocument fd = new FieldDocument();
        fd.addTV("base",
                "AfFe aFfFE Baum Baumgarten SteinGarten franZ HaNs Haus Efeu effe",
                "[(0-4)s:AfFe|i:affe|_0$<i>0<i>4|-:t$<i>10]"
                        + "[(5-10)s:aFfFE|i:afffe|_1$<i>5<i>10]"
                        + "[(11-15)s:Baum|i:baum|_2$<i>11<i>15]"
                        + "[(16-26)s:Baumgarten|i:baumgarten|_3$<i>16<i>26]"
                        + "[(27-38)s:SteinGarten|i:steingarten|_4$<i>27<i>38]"
                        + "[(39-44)s:franZ|i:franz|_5$<i>39<i>44]"
                        + "[(45-49)s:HaNs|i:hans|_6$<i>45<i>49]"
                        + "[(50-54)s:Haus|i:haus|_7$<i>50<i>54]"
                        + "[(55-59)s:Efeu|i:efeu|_8$<i>55<i>59]"
                        + "[(60-64)s:effe|i:effe|_9$<i>60<i>64]");
        ki.addDoc(fd);

        ki.commit();

        QueryBuilder kq = new QueryBuilder("base");
        SpanQueryWrapper sqw = kq.re("s:Af*e", true);
        assertEquals("SpanMultiTermQueryWrapper(base:/i:af*e/)",
                sqw.toQuery().toString());

        Krill ks = _newKrill(sqw);
        Result kr = ki.search(ks);
        assertEquals((long) 2, kr.getTotalResults());
        assertEquals("[[AfFe]] aFfFE ...", kr.getMatch(0).getSnippetBrackets());
        assertEquals("AfFe [[aFfFE]] Baum ...",
                kr.getMatch(1).getSnippetBrackets());

        ks = _newKrill(new QueryBuilder("base").re("s:Af.*e"));
        kr = ki.search(ks);
        assertEquals((long) 1, kr.getTotalResults());
        assertEquals("[[AfFe]] aFfFE ...", kr.getMatch(0).getSnippetBrackets());

        ks = _newKrill(new QueryBuilder("base").re("s:baum.*", true));
        kr = ki.search(ks);
        assertEquals((long) 2, kr.getTotalResults());
        assertEquals("... aFfFE [[Baum]] Baumgarten ...",
                kr.getMatch(0).getSnippetBrackets());
        assertEquals("... Baum [[Baumgarten]] SteinGarten ...",
                kr.getMatch(1).getSnippetBrackets());

        ks = _newKrill(new QueryBuilder("base").re("s:.*garten", true));
        kr = ki.search(ks);
        assertEquals((long) 2, kr.getTotalResults());
        assertEquals("... Baum [[Baumgarten]] SteinGarten ...",
                kr.getMatch(0).getSnippetBrackets());
        assertEquals("... Baumgarten [[SteinGarten]] franZ ...",
                kr.getMatch(1).getSnippetBrackets());

        ks = _newKrill(new QueryBuilder("base").re("s:.*garten", false));
        kr = ki.search(ks);
        assertEquals((long) 1, kr.getTotalResults());
        assertEquals("... Baum [[Baumgarten]] SteinGarten ...",
                kr.getMatch(0).getSnippetBrackets());

        ks = _newKrill(new QueryBuilder("base").re("s:ha.s", true));
        kr = ki.search(ks);
        assertEquals((long) 2, kr.getTotalResults());
        assertEquals("... franZ [[HaNs]] Haus ...",
                kr.getMatch(0).getSnippetBrackets());
        assertEquals("... HaNs [[Haus]] Efeu ...",
                kr.getMatch(1).getSnippetBrackets());

        ks = _newKrill(new QueryBuilder("base").re("s:.*f*e", true));
        kr = ki.search(ks);
        assertEquals((long) 3, kr.getTotalResults());
        assertEquals("[[AfFe]] aFfFE ...", kr.getMatch(0).getSnippetBrackets());
        assertEquals("AfFe [[aFfFE]] Baum ...",
                kr.getMatch(1).getSnippetBrackets());
        assertEquals("... Efeu [[effe]]", kr.getMatch(2).getSnippetBrackets());
    };


    @Test
    public void indexRegexCombined () throws Exception {
        KrillIndex ki = new KrillIndex();

        // abcabcabac
        FieldDocument fd = new FieldDocument();
        fd.addTV("base",
                "affe afffe baum baumgarten steingarten franz hans haus efeu effe",
                "[(0-4)s:affe|_0$<i>0<i>4|-:t$<i>10]"
                        + "[(5-10)s:afffe|_1$<i>5<i>10]"
                        + "[(11-15)s:baum|_2$<i>11<i>15]"
                        + "[(16-26)s:baumgarten|_3$<i>16<i>26]"
                        + "[(27-38)s:steingarten|_4$<i>27<i>38]"
                        + "[(39-44)s:franz|_5$<i>39<i>44]"
                        + "[(45-49)s:hans|_6$<i>45<i>49]"
                        + "[(50-54)s:haus|_7$<i>50<i>54]"
                        + "[(55-59)s:efeu|_8$<i>55<i>59]"
                        + "[(60-64)s:effe|_9$<i>60<i>64]");
        ki.addDoc(fd);

        ki.commit();

        QueryBuilder kq = new QueryBuilder("base");
        SpanQuery sq = kq.seq(kq.seg("s:affe")).append(kq.re("s:af*e"))
                .toQuery();
        assertEquals(
                "spanNext(base:s:affe, SpanMultiTermQueryWrapper(base:/s:af*e/))",
                sq.toString());

        Krill ks = new Krill(sq);
        ks.getMeta().getContext().left.setToken(true).setLength(1);
        ks.getMeta().getContext().right.setToken(true).setLength(1);

        Result kr = ki.search(ks);
        assertEquals((long) 1, kr.getTotalResults());
        assertEquals("[[affe afffe]] baum ...",
                kr.getMatch(0).getSnippetBrackets());
    };


    @Test
    public void indexRegexWithinRewrite () throws Exception {
        KrillIndex ki = new KrillIndex();

        // abcabcabac
        FieldDocument fd = new FieldDocument();
        fd.addTV("base",
                "affe afffe baum baumgarten steingarten franz hans haus efeu effe",
                "[(0-4)s:affe|_0$<i>0<i>4|-:t$<i>10]"
                        + "[(5-10)s:afffe|_1$<i>5<i>10]"
                        + "[(11-15)s:baum|_2$<i>11<i>15]"
                        + "[(16-26)s:baumgarten|_3$<i>16<i>26]"
                        + "[(27-38)s:steingarten|_4$<i>27<i>38]"
                        + "[(39-44)s:franz|_5$<i>39<i>44]"
                        + "[(45-49)s:hans|_6$<i>45<i>49]"
                        + "[(50-54)s:haus|_7$<i>50<i>54]"
                        + "[(55-59)s:efeu|_8$<i>55<i>59]"
                        + "[(60-64)s:effe|_9$<i>60<i>64]");
        ki.addDoc(fd);

        ki.commit();

        QueryBuilder kq = new QueryBuilder("base");
        SpanQuery sq = kq
                .contains(kq.seq(kq.re("s:a.*e")).append(kq.re("s:af*e")),
                        kq.seg("s:affe"))
                .toQuery();
        assertEquals(
                "spanContain(spanNext(SpanMultiTermQueryWrapper(base:/s:a.*e/), SpanMultiTermQueryWrapper(base:/s:af*e/)), base:s:affe)",
                sq.toString());
        Krill ks = new Krill(sq);
        ks.getMeta().getContext().left.setToken(true).setLength(1);
        ks.getMeta().getContext().right.setToken(true).setLength(1);

        Result kr = ki.search(ks);
        assertEquals((long) 1, kr.getTotalResults());
        assertEquals("[[affe afffe]] baum ...",
                kr.getMatch(0).getSnippetBrackets());


		// Test without matches in sequence
		sq = kq.seq(kq.re("s:z.*e")).append(kq.seg("s:affe")).toQuery();
        assertEquals(
			"spanNext(SpanMultiTermQueryWrapper(base:/s:z.*e/), base:s:affe)",
			sq.toString());
        kr = ki.search(new Krill(sq));
        assertEquals((long) 0, kr.getTotalResults());

		// Test without matches in segment
		sq = kq.seg().with(kq.re("s:z.*e")).with("s:affe").toQuery();
        assertEquals(
			"spanSegment(SpanMultiTermQueryWrapper(base:/s:z.*e/), base:s:affe)",
			sq.toString());
        kr = ki.search(new Krill(sq));
        assertEquals((long) 0, kr.getTotalResults());

		// Test without matches in or
		sq = kq.or(kq.re("s:z.*e"), kq.seg("s:affe")).toQuery();
        assertEquals(
			"spanOr([SpanMultiTermQueryWrapper(base:/s:z.*e/), base:s:affe])",
			sq.toString());
        kr = ki.search(new Krill(sq));
        assertEquals((long) 1, kr.getTotalResults());

		// Test without matches in within
		sq = kq.within(kq.re("s:z.*e"), kq.seg("s:affe")).toQuery();
        assertEquals(
			"spanContain(SpanMultiTermQueryWrapper(base:/s:z.*e/), base:s:affe)",
			sq.toString());
        kr = ki.search(new Krill(sq));
        assertEquals((long) 0, kr.getTotalResults());

		// Test without matches in within (reversed)
		sq = kq.within(kq.seg("s:affe"), kq.re("s:z.*e")).toQuery();
        assertEquals(
			"spanContain(base:s:affe, SpanMultiTermQueryWrapper(base:/s:z.*e/))",
			sq.toString());
        kr = ki.search(new Krill(sq));
        assertEquals((long) 0, kr.getTotalResults());

		// Test with classes
		sq = kq._(kq.re("s:z.*e")).toQuery();
        assertEquals(
			"{1: SpanMultiTermQueryWrapper(base:/s:z.*e/)}",
			sq.toString());
        kr = ki.search(new Krill(sq));
        assertEquals((long) 0, kr.getTotalResults());

		// Test with nested classes
		sq = kq.within(kq._(kq.re("s:z.*e")), kq.seg("s:affe")).toQuery();
        assertEquals(
			"spanContain({1: SpanMultiTermQueryWrapper(base:/s:z.*e/)}, base:s:affe)",
			sq.toString());
        kr = ki.search(new Krill(sq));
        assertEquals((long) 0, kr.getTotalResults());

		// Test with multiple distances
		List<DistanceConstraint> constraints = new ArrayList<DistanceConstraint>();
        constraints.add(
			TestMultipleDistanceIndex.createConstraint(
				"w",
				1,
				2,
				true,
				false
				)
			);
        constraints.add(
			TestMultipleDistanceIndex.createConstraint(
				"tokens",
				"base/s:s",
				0,
				0,
				true,
				false
				)
			);
		sq = new SpanMultipleDistanceQuery(
			kq.re("s:z.*e").toQuery(),
			kq.seg("s:affe").toQuery(),
			constraints,
			true,
			true
			);
        assertEquals(
			"spanMultipleDistance(SpanMultiTermQueryWrapper(base:/s:z.*e/), "
			+ "base:s:affe, [(w[1:2], ordered, notExcluded), ("
			+ "base/s:s[0:0], ordered, notExcluded)])",
			sq.toString());
        kr = ki.search(new Krill(sq));
        assertEquals((long) 0, kr.getTotalResults());

		// Test with multiple distances and a class
		sq = new SpanMultipleDistanceQuery(
			kq._(kq.re("s:z.*e")).toQuery(),
			kq.seg("s:affe").toQuery(),
			constraints,
			true,
			true
			);
        assertEquals(
                "spanMultipleDistance({1: SpanMultiTermQueryWrapper(base:/s:z.*e/)}, "
                + "base:s:affe, [(w[1:2], ordered, notExcluded), (base/s:s[0:0], ordered, notExcluded)])",
                sq.toString());
        kr = ki.search(new Krill(sq));
        assertEquals((long) 0, kr.getTotalResults());
    };


    private Krill _newKrill (SpanQueryWrapper query) {
        Krill ks = new Krill(query);
        ks.getMeta().getContext().left.setToken(true).setLength(1);
        ks.getMeta().getContext().right.setToken(true).setLength(1);
        return ks;
    };
};
