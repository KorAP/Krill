import java.util.*;
import java.io.IOException;

import org.apache.lucene.search.spans.SpanQuery;

import de.ids_mannheim.korap.KorapIndex;
import de.ids_mannheim.korap.KorapQuery;
import de.ids_mannheim.korap.KorapResult;
import de.ids_mannheim.korap.KorapMatch;
import de.ids_mannheim.korap.index.FieldDocument;

import static de.ids_mannheim.korap.Test.*;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestHighlight { // extends LuceneTestCase {

    @Test
    public void checkHighlights () throws IOException  {

	KorapIndex ki = new KorapIndex();
	String json = new String(
"{" +
"  \"fields\" : [" +
"    { "+
"      \"primaryData\" : \"abc\"" +
"    }," +
"    {" +
"      \"name\" : \"tokens\"," +
"      \"data\" : [" +
"         [ \"s:a\", \"i:a\", \"_0#0-1\", \"-:t$<i>3\"]," +
"         [ \"s:b\", \"i:b\", \"_1#1-2\" ]," +
"         [ \"s:c\", \"i:c\", \"_2#2-3\" ]" +
"      ]" +
"    }" +
"  ]" +
"}");

	FieldDocument fd = ki.addDoc(json);
	ki.commit();


	KorapQuery kq = new KorapQuery("tokens");
	KorapResult kr = ki.search((SpanQuery) kq.seq(kq._(1, kq.seg("s:b"))).toQuery());
	KorapMatch km = kr.getMatch(0);
	assertEquals("<span class=\"context-left\">a</span><span class=\"match\"><em class=\"class-1 level-0\">b</em></span><span class=\"context-right\">c</span>", km.getSnippetHTML());

	kr = ki.search((SpanQuery) kq.seq(kq._(1, kq.seg("s:b"))).append(kq._(2, kq.seg("s:c"))).toQuery());
	km = kr.getMatch(0);
	assertEquals("<span class=\"context-left\">a</span><span class=\"match\"><em class=\"class-1 level-0\">b</em><em class=\"class-2 level-0\">c</em></span><span class=\"context-right\"></span>", km.getSnippetHTML());


	kr = ki.search((SpanQuery) kq.seq(kq._(1, kq.seg("s:b"))).append(kq._(2, kq.seg("s:c"))).toQuery());
	km = kr.getMatch(0);
	assertEquals("<span class=\"context-left\">a</span><span class=\"match\"><em class=\"class-1 level-0\">b</em><em class=\"class-2 level-0\">c</em></span><span class=\"context-right\"></span>", km.getSnippetHTML());

	kr = ki.search((SpanQuery) kq.seq(kq._(1, kq.seq(kq.seg("s:a")).append(kq.seg("s:b")))).append(kq._(2, kq.seg("s:c"))).toQuery());
	km = kr.getMatch(0);
	assertEquals("<span class=\"context-left\"></span><span class=\"match\"><em class=\"class-1 level-0\">ab</em><em class=\"class-2 level-0\">c</em></span><span class=\"context-right\"></span>", km.getSnippetHTML());


	kr = ki.search((SpanQuery) kq._(3, kq.seq(kq._(1, kq.seq(kq.seg("s:a")).append(kq.seg("s:b")))).append(kq._(2, kq.seg("s:c")))).toQuery());
	km = kr.getMatch(0);
	assertEquals("<span class=\"context-left\"></span><span class=\"match\"><em class=\"class-3 level-0\"><em class=\"class-1 level-1\">ab</em><em class=\"class-2 level-1\">c</em></em></span><span class=\"context-right\"></span>", km.getSnippetHTML());
    };

    @Test
    public void checkHighlightsManually () throws IOException  {

	KorapIndex ki = new KorapIndex();
	String json = new String(
"{" +
"  \"fields\" : [" +
"    { "+
"      \"primaryData\" : \"abc\"" +
"    }," +
"    {" +
"      \"name\" : \"tokens\"," +
"      \"data\" : [" +
"         [ \"s:a\", \"i:a\", \"_0#0-1\", \"-:t$<i>3\"]," +
"         [ \"s:b\", \"i:b\", \"_1#1-2\" ]," +
"         [ \"s:c\", \"i:c\", \"_2#2-3\" ]" +
"      ]" +
"    }" +
"  ]" +
"}");

	FieldDocument fd = ki.addDoc(json);
	ki.commit();

	KorapQuery kq = new KorapQuery("tokens");

	KorapResult kr = ki.search((SpanQuery) kq.seq(kq.seg("s:a")).append(kq.seg("s:b")).append(kq.seg("s:c")).toQuery());
	KorapMatch km = kr.getMatch(0);
	km.addHighlight(0, 1, (short) 7);
	assertEquals("<span class=\"context-left\"></span><span class=\"match\"><em class=\"class-7 level-0\">ab</em>c</span><span class=\"context-right\"></span>", km.getSnippetHTML());

	km.addHighlight(1, 2, (short) 6);
	assertEquals("<span class=\"context-left\"></span><span class=\"match\"><em class=\"class-7 level-0\">a<em class=\"class-6 level-1\">b</em></em><em class=\"class-6 level-1\">c</em></span><span class=\"context-right\"></span>", km.getSnippetHTML());

	km.addHighlight(0, 1, (short) 5);
	assertEquals("[{7:{5:a{6:b}}}{6:c}]", km.getSnippetBrackets());
	assertEquals("<span class=\"context-left\"></span><span class=\"match\"><em class=\"class-7 level-0\"><em class=\"class-5 level-1\">a<em class=\"class-6 level-2\">b</em></em></em><em class=\"class-6 level-2\">c</em></span><span class=\"context-right\"></span>", km.getSnippetHTML());

    };
};
