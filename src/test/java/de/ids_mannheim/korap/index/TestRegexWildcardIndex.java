package de.ids_mannheim.korap.index;

import java.util.*;
import java.io.*;

import org.apache.lucene.util.Version;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Bits;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import de.ids_mannheim.korap.KorapIndex;
import de.ids_mannheim.korap.KorapQuery;
import de.ids_mannheim.korap.KorapResult;
import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.index.FieldDocument;
import de.ids_mannheim.korap.model.MultiTermTokenStream;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;

@RunWith(JUnit4.class)
public class TestRegexWildcardIndex {

    @Test
    public void indexRegex () throws Exception {
	KorapIndex ki = new KorapIndex();

	// abcabcabac
	FieldDocument fd = new FieldDocument();
	fd.addTV("base",
		 "affe afffe baum baumgarten steingarten franz hans haus efeu effe",
		 "[(0-4)s:affe|_0#0-4|-:t$<i>10]" +
		 "[(5-10)s:afffe|_1#5-10]" +
		 "[(11-15)s:baum|_2#11-15]" +
		 "[(16-26)s:baumgarten|_3#16-26]" +
		 "[(27-38)s:steingarten|_4#27-38]" +
		 "[(39-44)s:franz|_5#39-44]" +
		 "[(45-49)s:hans|_6#45-49]" +
		 "[(50-54)s:haus|_7#50-54]" +
		 "[(55-59)s:efeu|_8#55-59]" +
		 "[(60-64)s:effe|_9#60-64]");
	ki.addDoc(fd);

	ki.commit();

	KorapQuery kq = new KorapQuery("base");
	SpanQuery sq = kq.re("s:af*e").toQuery();
	assertEquals("SpanMultiTermQueryWrapper(base:/s:af*e/)", sq.toString());
			
	Krill ks = new Krill(sq);
	ks.getMeta().getContext().left.setToken(true).setLength(1);
	ks.getMeta().getContext().right.setToken(true).setLength(1);

	KorapResult kr = ki.search(ks);
	assertEquals((long) 2, kr.getTotalResults());
	assertEquals("[affe] afffe ...", kr.getMatch(0).getSnippetBrackets());
	assertEquals("affe [afffe] baum ...", kr.getMatch(1).getSnippetBrackets());

	kr = ki.search(ks.setSpanQuery(new KorapQuery("base").re("s:baum.*").toQuery()));
	assertEquals((long) 2, kr.getTotalResults());
	assertEquals("... afffe [baum] baumgarten ...", kr.getMatch(0).getSnippetBrackets());
	assertEquals("... baum [baumgarten] steingarten ...", kr.getMatch(1).getSnippetBrackets());

	kr = ki.search(ks.setSpanQuery(new KorapQuery("base").re("s:.....?garten").toQuery()));
	assertEquals((long) 2, kr.getTotalResults());
	assertEquals("... baum [baumgarten] steingarten ...", kr.getMatch(0).getSnippetBrackets());
	assertEquals("... baumgarten [steingarten] franz ...", kr.getMatch(1).getSnippetBrackets());

	kr = ki.search(ks.setSpanQuery(new KorapQuery("base").re("s:ha.s").toQuery()));
	assertEquals((long) 2, kr.getTotalResults());
	assertEquals("... franz [hans] haus ...", kr.getMatch(0).getSnippetBrackets());
	assertEquals("... hans [haus] efeu ...", kr.getMatch(1).getSnippetBrackets());

	kr = ki.search(ks.setSpanQuery(new KorapQuery("base").re("s:.*ff.*").toQuery()));
	assertEquals((long) 3, kr.getTotalResults());
	assertEquals("[affe] afffe ...", kr.getMatch(0).getSnippetBrackets());
	assertEquals("affe [afffe] baum ...", kr.getMatch(1).getSnippetBrackets());
	assertEquals("... efeu [effe]", kr.getMatch(2).getSnippetBrackets());
    };

    @Test
    public void indexWildcard () throws Exception {
	KorapIndex ki = new KorapIndex();

	// abcabcabac
	FieldDocument fd = new FieldDocument();
	fd.addTV("base",
		 "affe afffe baum baumgarten steingarten franz hans haus efeu effe",
		 "[(0-4)s:affe|_0#0-4|-:t$<i>10]" +
		 "[(5-10)s:afffe|_1#5-10]" +
		 "[(11-15)s:baum|_2#11-15]" +
		 "[(16-26)s:baumgarten|_3#16-26]" +
		 "[(27-38)s:steingarten|_4#27-38]" +
		 "[(39-44)s:franz|_5#39-44]" +
		 "[(45-49)s:hans|_6#45-49]" +
		 "[(50-54)s:haus|_7#50-54]" +
		 "[(55-59)s:efeu|_8#55-59]" +
		 "[(60-64)s:effe|_9#60-64]");
	ki.addDoc(fd);

	ki.commit();

	KorapQuery kq = new KorapQuery("base");
	SpanQuery sq = kq.wc("s:af*e").toQuery();
	assertEquals("SpanMultiTermQueryWrapper(base:s:af*e)", sq.toString());

	Krill ks = new Krill(sq);
	ks.getMeta().getContext().left.setToken(true).setLength(1);
	ks.getMeta().getContext().right.setToken(true).setLength(1);

	KorapResult kr = ki.search(ks);
	assertEquals((long) 2, kr.getTotalResults());
	assertEquals("[affe] afffe ...", kr.getMatch(0).getSnippetBrackets());
	assertEquals("affe [afffe] baum ...", kr.getMatch(1).getSnippetBrackets());

	kr = ki.search(ks.setSpanQuery(new KorapQuery("base").wc("s:baum.*").toQuery()));
	assertEquals((long) 0, kr.getTotalResults());

	kr = ki.search(ks.setSpanQuery(new KorapQuery("base").wc("s:baum*").toQuery()));
	assertEquals((long) 2, kr.getTotalResults());
	assertEquals("... afffe [baum] baumgarten ...", kr.getMatch(0).getSnippetBrackets());
	assertEquals("... baum [baumgarten] steingarten ...", kr.getMatch(1).getSnippetBrackets());

	kr = ki.search(ks.setSpanQuery(new KorapQuery("base").wc("s:*garten").toQuery()));
	assertEquals((long) 2, kr.getTotalResults());
	assertEquals("... baum [baumgarten] steingarten ...", kr.getMatch(0).getSnippetBrackets());
	assertEquals("... baumgarten [steingarten] franz ...", kr.getMatch(1).getSnippetBrackets());

	kr = ki.search(ks.setSpanQuery(new KorapQuery("base").wc("s:ha?s").toQuery()));
	assertEquals((long) 2, kr.getTotalResults());
	assertEquals("... franz [hans] haus ...", kr.getMatch(0).getSnippetBrackets());
	assertEquals("... hans [haus] efeu ...", kr.getMatch(1).getSnippetBrackets());

	kr = ki.search(ks.setSpanQuery(new KorapQuery("base").wc("s:?ff?").toQuery()));
	assertEquals((long) 2, kr.getTotalResults());
	assertEquals("[affe] afffe ...", kr.getMatch(0).getSnippetBrackets());
	assertEquals("... efeu [effe]", kr.getMatch(1).getSnippetBrackets());
    };

    @Test
    public void indexRegexCaseInsensitive () throws Exception {
	KorapIndex ki = new KorapIndex();

	// abcabcabac
	FieldDocument fd = new FieldDocument();
	fd.addTV("base",
		 "AfFe aFfFE Baum Baumgarten SteinGarten franZ HaNs Haus Efeu effe",
		 "[(0-4)s:AfFe|i:affe|_0#0-4|-:t$<i>10]" +
		 "[(5-10)s:aFfFE|i:afffe|_1#5-10]" +
		 "[(11-15)s:Baum|i:baum|_2#11-15]" +
		 "[(16-26)s:Baumgarten|i:baumgarten|_3#16-26]" +
		 "[(27-38)s:SteinGarten|i:steingarten|_4#27-38]" +
		 "[(39-44)s:franZ|i:franz|_5#39-44]" +
		 "[(45-49)s:HaNs|i:hans|_6#45-49]" +
		 "[(50-54)s:Haus|i:haus|_7#50-54]" +
		 "[(55-59)s:Efeu|i:efeu|_8#55-59]" +
		 "[(60-64)s:effe|i:effe|_9#60-64]");
	ki.addDoc(fd);

	ki.commit();

	KorapQuery kq = new KorapQuery("base");
	SpanQuery sq = kq.re("s:Af*e", true).toQuery();
	assertEquals("SpanMultiTermQueryWrapper(base:/i:af*e/)", sq.toString());

	Krill ks = new Krill(sq);
	ks.getMeta().getContext().left.setToken(true).setLength(1);
	ks.getMeta().getContext().right.setToken(true).setLength(1);

	KorapResult kr = ki.search(ks);
	assertEquals((long) 2, kr.getTotalResults());
	assertEquals("[AfFe] aFfFE ...", kr.getMatch(0).getSnippetBrackets());
	assertEquals("AfFe [aFfFE] Baum ...", kr.getMatch(1).getSnippetBrackets());

	kr = ki.search(ks.setSpanQuery(new KorapQuery("base").re("s:Af.*e").toQuery()));
	assertEquals((long) 1, kr.getTotalResults());
	assertEquals("[AfFe] aFfFE ...", kr.getMatch(0).getSnippetBrackets());

	kr = ki.search(ks.setSpanQuery(new KorapQuery("base").re("s:baum.*", true).toQuery()));
	assertEquals((long) 2, kr.getTotalResults());
	assertEquals("... aFfFE [Baum] Baumgarten ...", kr.getMatch(0).getSnippetBrackets());
	assertEquals("... Baum [Baumgarten] SteinGarten ...", kr.getMatch(1).getSnippetBrackets());

	kr = ki.search(ks.setSpanQuery(new KorapQuery("base").re("s:.*garten", true).toQuery()));
	assertEquals((long) 2, kr.getTotalResults());
	assertEquals("... Baum [Baumgarten] SteinGarten ...", kr.getMatch(0).getSnippetBrackets());
	assertEquals("... Baumgarten [SteinGarten] franZ ...", kr.getMatch(1).getSnippetBrackets());

	kr = ki.search(ks.setSpanQuery(new KorapQuery("base").re("s:.*garten", false).toQuery()));
	assertEquals((long) 1, kr.getTotalResults());
	assertEquals("... Baum [Baumgarten] SteinGarten ...", kr.getMatch(0).getSnippetBrackets());

	kr = ki.search(ks.setSpanQuery(new KorapQuery("base").re("s:ha.s", true).toQuery()));
	assertEquals((long) 2, kr.getTotalResults());
	assertEquals("... franZ [HaNs] Haus ...", kr.getMatch(0).getSnippetBrackets());
	assertEquals("... HaNs [Haus] Efeu ...", kr.getMatch(1).getSnippetBrackets());

	kr = ki.search(ks.setSpanQuery(new KorapQuery("base").re("s:.*f*e", true).toQuery()));
	assertEquals((long) 3, kr.getTotalResults());
	assertEquals("[AfFe] aFfFE ...", kr.getMatch(0).getSnippetBrackets());
	assertEquals("AfFe [aFfFE] Baum ...", kr.getMatch(1).getSnippetBrackets());
	assertEquals("... Efeu [effe]", kr.getMatch(2).getSnippetBrackets());
    };

    @Test
    public void indexRegexCombined () throws Exception {
	KorapIndex ki = new KorapIndex();

	// abcabcabac
	FieldDocument fd = new FieldDocument();
	fd.addTV("base",
		 "affe afffe baum baumgarten steingarten franz hans haus efeu effe",
		 "[(0-4)s:affe|_0#0-4|-:t$<i>10]" +
		 "[(5-10)s:afffe|_1#5-10]" +
		 "[(11-15)s:baum|_2#11-15]" +
		 "[(16-26)s:baumgarten|_3#16-26]" +
		 "[(27-38)s:steingarten|_4#27-38]" +
		 "[(39-44)s:franz|_5#39-44]" +
		 "[(45-49)s:hans|_6#45-49]" +
		 "[(50-54)s:haus|_7#50-54]" +
		 "[(55-59)s:efeu|_8#55-59]" +
		 "[(60-64)s:effe|_9#60-64]");
	ki.addDoc(fd);

	ki.commit();

	KorapQuery kq = new KorapQuery("base");
	SpanQuery sq = kq.seq(kq.seg("s:affe")).append(kq.re("s:af*e")).toQuery();
	assertEquals("spanNext(base:s:affe, SpanMultiTermQueryWrapper(base:/s:af*e/))", sq.toString());

	Krill ks = new Krill(sq);
	ks.getMeta().getContext().left.setToken(true).setLength(1);
	ks.getMeta().getContext().right.setToken(true).setLength(1);

	KorapResult kr = ki.search(ks);
	assertEquals((long) 1, kr.getTotalResults());
	assertEquals("[affe afffe] baum ...", kr.getMatch(0).getSnippetBrackets());
    };


    @Test
    public void indexRegexWithinRewrite () throws Exception {
	KorapIndex ki = new KorapIndex();

	// abcabcabac
	FieldDocument fd = new FieldDocument();
	fd.addTV("base",
		 "affe afffe baum baumgarten steingarten franz hans haus efeu effe",
		 "[(0-4)s:affe|_0#0-4|-:t$<i>10]" +
		 "[(5-10)s:afffe|_1#5-10]" +
		 "[(11-15)s:baum|_2#11-15]" +
		 "[(16-26)s:baumgarten|_3#16-26]" +
		 "[(27-38)s:steingarten|_4#27-38]" +
		 "[(39-44)s:franz|_5#39-44]" +
		 "[(45-49)s:hans|_6#45-49]" +
		 "[(50-54)s:haus|_7#50-54]" +
		 "[(55-59)s:efeu|_8#55-59]" +
		 "[(60-64)s:effe|_9#60-64]");
	ki.addDoc(fd);

	ki.commit();

	KorapQuery kq = new KorapQuery("base");
	SpanQuery sq = kq.contains(
				   kq.seq(
					  kq.re("s:a.*e")
					  ).append(
						   kq.re("s:af*e")
						   ),
				   kq.seg("s:affe")).toQuery();
	assertEquals("spanContain(spanNext(SpanMultiTermQueryWrapper(base:/s:a.*e/), SpanMultiTermQueryWrapper(base:/s:af*e/)), base:s:affe)", sq.toString());
	Krill ks = new Krill(sq);
	ks.getMeta().getContext().left.setToken(true).setLength(1);
	ks.getMeta().getContext().right.setToken(true).setLength(1);

	KorapResult kr = ki.search(ks);
	assertEquals((long) 1, kr.getTotalResults());
	assertEquals("[affe afffe] baum ...", kr.getMatch(0).getSnippetBrackets());
    };
};
