import java.util.*;
import java.io.*;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import de.ids_mannheim.korap.index.MatchIdentifier;

import de.ids_mannheim.korap.KorapIndex;
import de.ids_mannheim.korap.KorapQuery;
import de.ids_mannheim.korap.KorapSearch;
import de.ids_mannheim.korap.KorapResult;
import de.ids_mannheim.korap.KorapMatch;

import de.ids_mannheim.korap.index.FieldDocument;

@RunWith(JUnit4.class)
public class TestMatchIdentifier {

    @Test
    public void identifierExample1 () throws IOException {
	MatchIdentifier id = new MatchIdentifier("match-c1!d1-p4-20");
	assertEquals(id.getCorpusID(), "c1");
	assertEquals(id.getDocID(), "d1");
	assertEquals(id.getStartPos(), 4);
	assertEquals(id.getEndPos(), 20);

	assertEquals(id.toString(), "match-c1!d1-p4-20");
	id.addPos(10,14,2);
	assertEquals(id.toString(), "match-c1!d1-p4-20(2)10-14");
	id.addPos(11,12,5);
	assertEquals(id.toString(), "match-c1!d1-p4-20(2)10-14(5)11-12");
	// Ignore
	id.addPos(11,12,-8);
	assertEquals(id.toString(), "match-c1!d1-p4-20(2)10-14(5)11-12");
	id.addPos(11,-12,8);
	assertEquals(id.toString(), "match-c1!d1-p4-20(2)10-14(5)11-12");
	id.addPos(-11,12,8);
	assertEquals(id.toString(), "match-c1!d1-p4-20(2)10-14(5)11-12");

	id = new MatchIdentifier("matc-c1!d1-p4-20");
	assertNull(id.toString());
	id = new MatchIdentifier("match-d1-p4-20");
	assertNull(id.getCorpusID());
	assertEquals(id.getDocID(), "d1");
	id = new MatchIdentifier("match-p4-20");
	assertNull(id.toString());

	id = new MatchIdentifier("match-c1!d1-p4-20");
	assertEquals(id.toString(), "match-c1!d1-p4-20");

	id = new MatchIdentifier("match-c1!d1-p4-20(5)7-8");
	assertEquals(id.toString(), "match-c1!d1-p4-20(5)7-8");

	id = new MatchIdentifier("match-c1!d1-p4-20(5)7-8(-2)9-10");
	assertEquals(id.toString(), "match-c1!d1-p4-20(5)7-8");

	id = new MatchIdentifier("match-c1!d1-p4-20(5)7-8(-2)9-10(2)3-4(3)-5-6");
	assertEquals(id.toString(), "match-c1!d1-p4-20(5)7-8(2)3-4");

	id = new MatchIdentifier("match-c1!d1-p4-20(5)7-8(-2)9-10(2)3-4(3)-5-6(4)7-8");
	assertEquals(id.toString(), "match-c1!d1-p4-20(5)7-8(2)3-4(4)7-8");

	id = new MatchIdentifier("match-c1!d1-p4-20(5)7-8(-2)9-10(2)3-4(3)-5-6(4)7-8(5)9--10");
	assertEquals(id.toString(), "match-c1!d1-p4-20(5)7-8(2)3-4(4)7-8");
    };

    @Test
    public void indexExample1 () throws IOException {
	KorapIndex ki = new KorapIndex();
	ki.addDoc(createSimpleFieldDoc());
	ki.commit();

	KorapQuery kq = new KorapQuery("tokens");
	KorapSearch ks = new KorapSearch(kq._(2,kq.seq(kq.seg("s:b")).append(kq._(kq.seg("s:a")))));
	KorapResult kr = ki.search(ks);

	assertEquals("totalResults", 1, kr.totalResults());
	assertEquals("StartPos (0)", 7, kr.match(0).startPos);
	assertEquals("EndPos (0)", 9, kr.match(0).endPos);

	KorapMatch km = kr.match(0);

	assertEquals("SnippetBrackets (0)", "... bcabca[{2:b{a}}]c", km.snippetBrackets());
	assertEquals("ID (0)", "match-c1!d1-p7-9(0)8-8(2)7-8", km.getID());
    };

    @Test
    public void indexExample2 () throws IOException {
	KorapIndex ki = new KorapIndex();
	ki.addDoc(createSimpleFieldDoc());
	ki.commit();

	KorapMatch km = ki.getMatch("match-c1!d1-p7-9(0)8-8(2)7-8");

	assertEquals("StartPos (0)", 7, km.getStartPos());
	assertEquals("EndPos (0)", 9, km.getEndPos());

	assertEquals("SnippetBrackets (0)",
		     "... [{2:b{a}}] ...",
		     km.getSnippetBrackets());
	assertEquals("ID (0)", "match-c1!d1-p7-9(0)8-8(2)7-8", km.getID());

	km = ki.getMatchInfo("match-c1!d1-p7-9(0)8-8(2)7-8",
			     "tokens",
			     "f",
			     "m",
			     false,
			     false);

	assertEquals("SnippetBrackets (1)",
		     "... [{f/m:acht:b}{f/m:neun:a}] ...",
		     km.getSnippetBrackets());

	km = ki.getMatchInfo("match-c1!d1-p7-9(0)8-8(2)7-8",
			     "tokens",
			     "f",
			     "m",
			     false,
			     true);

	assertEquals("SnippetBrackets (2)",
		     "... [{2:{f/m:acht:b}{{f/m:neun:a}}}] ...",
		     km.getSnippetBrackets());

	km = ki.getMatchInfo("match-c1!d1-p7-9(4)8-8(2)7-8",
			     "tokens",
			     "f",
			     "m",
			     false,
			     true);

	assertEquals("SnippetBrackets (3)",
		     "... [{2:{f/m:acht:b}{4:{f/m:neun:a}}}] ...",
		     km.getSnippetBrackets());

	km = ki.getMatchInfo("match-c1!d1-p7-9(4)8-8(2)7-8",
			     "tokens",
			     "f",
			     null,
			     false,
			     true);

	assertEquals("SnippetBrackets (4)",
		     "... [{2:{f/m:acht:{f/y:eight:b}}{4:{f/m:neun:{f/y:nine:a}}}}] ...",
		     km.getSnippetBrackets());

	assertEquals("SnippetHTML (4)",
		     "<span class=\"context-left\">"+
		     "<span class=\"more\">"+
		     "</span>"+
		     "</span>"+
		     "<span class=\"match\">"+
		     "<em class=\"class-2 level-0\">"+
		     "<span title=\"f/m:acht\">"+
		     "<span title=\"f/y:eight\">"+
		     "b"+
		     "</span>"+
		     "</span>"+
		     "<em class=\"class-4 level-1\">"+
		     "<span title=\"f/m:neun\">"+
		     "<span title=\"f/y:nine\">"+
		     "a"+
		     "</span>"+
		     "</span>"+
		     "</em>"+
		     "</em>"+
		     "</span>"+
		     "<span class=\"context-right\">"+
		     "<span class=\"more\">"+
		     "</span>"+
		     "</span>",
		     km.getSnippetHTML());

	km = ki.getMatchInfo("match-c1!d1-p7-9(4)8-8(2)7-8",
			     "tokens",
			     null,
			     null,
			     false,
			     true);

	// --> bug:
	// System.err.println(km.snippetHTML());
    };

    private FieldDocument createSimpleFieldDoc(){
	FieldDocument fd = new FieldDocument();
	fd.addString("corpusID", "c1");
	fd.addString("ID", "d1");
	fd.addTV("tokens",
		 "abcabcabac",
		 "[(0-1)s:a|i:a|f/m:eins|f/y:one|x/o:erstens|_0#0-1|-:t$<i>10]" +
		 "[(1-2)s:b|i:b|f/m:zwei|f/y:two|x/o:zweitens|_1#1-2]" +
		 "[(2-3)s:c|i:c|f/m:drei|f/y:three|x/o:drittens|_2#2-3]" +
		 "[(3-4)s:a|i:a|f/m:vier|f/y:four|x/o:viertens|_3#3-4]" +
		 "[(4-5)s:b|i:b|f/m:fuenf|f/y:five|x/o:fünftens|_4#4-5]" +
		 "[(5-6)s:c|i:c|f/m:sechs|f/y:six|x/o:sechstens|_5#5-6]" +
		 "[(6-7)s:a|i:a|f/m:sieben|f/y:seven|x/o:siebtens|_6#6-7]" +
		 "[(7-8)s:b|i:b|f/m:acht|f/y:eight|x/o:achtens|_7#7-8]" +
		 "[(8-9)s:a|i:a|f/m:neun|f/y:nine|x/o:neuntens|_8#8-9]" +
		 "[(9-10)s:c|i:c|f/m:zehn|f/y:ten|x/o:zehntens|_9#9-10]");
	return fd;
    };
};
