package de.ids_mannheim.korap.index;

import java.util.*;
import java.io.*;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import de.ids_mannheim.korap.match.MatchIdentifier;
import de.ids_mannheim.korap.match.PosIdentifier;

import de.ids_mannheim.korap.KorapIndex;
import de.ids_mannheim.korap.KorapQuery;
import de.ids_mannheim.korap.KorapSearch;
import de.ids_mannheim.korap.KorapResult;
import de.ids_mannheim.korap.KorapMatch;
import de.ids_mannheim.korap.util.QueryException;


import de.ids_mannheim.korap.index.FieldDocument;

@RunWith(JUnit4.class)
public class TestMatchIdentifier {

    @Test
    public void identifierExample1 () throws IOException, QueryException {
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
    public void posIdentifierExample1 () throws IOException {
	PosIdentifier id = new PosIdentifier();
	id.setCorpusID("c1");
	id.setDocID("d1");
	id.setPos(8);
	assertEquals(id.getCorpusID(), "c1");
	assertEquals(id.getDocID(), "d1");
	assertEquals(id.getPos(), 8);
	assertEquals(id.toString(), "word-c1!d1-p8");
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

	assertEquals("SnippetBrackets (0)", "... bcabca[{2:b{1:a}}]c", km.snippetBrackets());
	assertEquals("ID (0)", "match-c1!d1-p7-9(2)7-8(1)8-8", km.getID());
    };

    @Test
    public void indexExample2 () throws IOException, QueryException {
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


	/*
	km = ki.getMatchInfo("match-c1!d1-p7-9(0)8-8(2)7-8",
			     "tokens",
			     "f",
			     null,
			     false,
			     false);

	System.err.println(km.toJSON());
	*/

	
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
    };


    @Test
    public void indexExample3 () throws IOException, QueryException {
	KorapIndex ki = new KorapIndex();
	ki.addDoc(createSimpleFieldDoc());
	ki.commit();

	KorapMatch km = ki.getMatchInfo("match-c1!d1-p7-9(4)8-8(2)7-8",
			     "tokens",
			     null,
			     null,
			     false,
			     true);


	assertEquals("SnippetHTML (1)",
		     "<span class=\"context-left\">" +
		     "<span class=\"more\">" +
		     "</span>" +
		     "</span>" +
		     "<span class=\"match\">" +
		     "<em class=\"class-2 level-0\">" +
		     "<span title=\"f/m:acht\">" +
		     "<span title=\"f/y:eight\">" +
		     "<span title=\"it/is:8\">" +
		     "<span title=\"x/o:achtens\">" +
		     "b" +
		     "</span>" +
		     "</span>" +
		     "</span>" +
		     "</span>" +
		     "<em class=\"class-4 level-1\">" +
		     "<span title=\"f/m:neun\">" +
		     "<span title=\"f/y:nine\">" +
		     "<span title=\"it/is:9\">" +
		     "<span title=\"x/o:neuntens\">" +
		     "a" +
		     "</span>" +
		     "</span>" +
		     "</span>" +
		     "</span>" +
		     "</em>" +
		     "</em>" +
		     "</span>" +
		     "<span class=\"context-right\">" +
		     "<span class=\"more\">" +
		     "</span>" +
		     "</span>",
		     km.getSnippetHTML());
    };

    @Test
    public void indexExample4 () throws IOException, QueryException {
	KorapIndex ki = new KorapIndex();
	ki.addDoc(createSimpleFieldDoc());
	ki.commit();

	KorapMatch km = ki.getMatchInfo("match-c1!d1-p7-9(4)8-8(2)7-8",
			     "tokens",
			     null,
			     null,
			     false,
			     false);


	assertEquals("SnippetHTML (1)",
		     "<span class=\"context-left\">" +
		     "<span class=\"more\">" +
		     "</span>" +
		     "</span>" +
		     "<span class=\"match\">" +
		     "<span title=\"f/m:acht\">" +
		     "<span title=\"f/y:eight\">" +
		     "<span title=\"it/is:8\">" +
		     "<span title=\"x/o:achtens\">" +
		     "b" +
		     "</span>" +
		     "</span>" +
		     "</span>" +
		     "</span>" +
		     "<span title=\"f/m:neun\">" +
		     "<span title=\"f/y:nine\">" +
		     "<span title=\"it/is:9\">" +
		     "<span title=\"x/o:neuntens\">" +
		     "a" +
		     "</span>" +
		     "</span>" +
		     "</span>" +
		     "</span>" +
		     "</span>" +
		     "<span class=\"context-right\">" +
		     "<span class=\"more\">" +
		     "</span>" +
		     "</span>",
		     km.getSnippetHTML());
    };

    @Test
    public void indexExample5Spans () throws IOException, QueryException {
	KorapIndex ki = new KorapIndex();
	ki.addDoc(createSimpleFieldDoc());
	ki.commit();

	KorapMatch km = ki.getMatchInfo("match-c1!d1-p7-9(4)8-8(2)7-8",
			     "tokens",
			     null,
			     null,
			     true,
			     false);


	assertEquals("SnippetBrackets (1)",
		     "... [{f/m:acht:{f/y:eight:{it/is:8:{x/o:achtens:b}}}}{f/m:neun:{f/y:nine:{it/is:9:{x/o:neuntens:a}}}}] ...",
		     km.getSnippetBrackets());
    };

    @Test
    public void indexExample6Spans () throws IOException, QueryException {
	KorapIndex ki = new KorapIndex();
	ki.addDoc(createSimpleFieldDoc());
	ki.commit();

	KorapMatch km = ki.getMatchInfo("match-c1!d1-p7-10(4)8-8(2)7-8",
			     "tokens",
			     null,
			     null,
			     true,
			     false);


	assertEquals("SnippetBrackets (1)",
		     "... [{x/tag:{f/m:acht:{f/y:eight:{it/is:8:{x/o:achtens:b}}}}{f/m:neun:{f/y:nine:{it/is:9:{x/o:neuntens:a}}}}{f/m:zehn:{f/y:ten:{it/is:10:{x/o:zehntens:c}}}}}]",
		     km.getSnippetBrackets());
    };

    @Test
    public void indexExample7Spans () throws IOException, QueryException {
	KorapIndex ki = new KorapIndex();
	ki.addDoc(createSimpleFieldDoc());
	ki.commit();

	KorapMatch km = ki.getMatchInfo("match-c1!d1-p7-10(4)8-8(2)7-8",
			     "tokens",
			     null,
			     null,
			     true,
			     true);


	assertEquals("SnippetBrackets (1)",
		     "... [{x/tag:{2:{f/m:acht:{f/y:eight:{it/is:8:{x/o:achtens:b}}}}{4:{f/m:neun:{f/y:nine:{it/is:9:{x/o:neuntens:a}}}}}}{f/m:zehn:{f/y:ten:{it/is:10:{x/o:zehntens:c}}}}}]",
		     km.getSnippetBrackets());

	assertEquals("SnippetHTML (1)",
		     "<span class=\"context-left\">" +
		     "<span class=\"more\">" +
		     "</span>" +
		     "</span>" +
		     "<span class=\"match\">" +
		     "<span title=\"x/tag\">" +
		     "<em class=\"class-2 level-0\">" +
		     "<span title=\"f/m:acht\">" +
		     "<span title=\"f/y:eight\">" +
		     "<span title=\"it/is:8\">" +
		     "<span title=\"x/o:achtens\">" +
		     "b" +
		     "</span>" +
		     "</span>" +
		     "</span>" +
		     "</span>" +
		     "<em class=\"class-4 level-1\">" +
		     "<span title=\"f/m:neun\">" +
		     "<span title=\"f/y:nine\">" +
		     "<span title=\"it/is:9\">" +
		     "<span title=\"x/o:neuntens\">" +
		     "a" +
		     "</span>" +
		     "</span>" +
		     "</span>" +
		     "</span>" +
		     "</em>" +
		     "</em>" +
		     "<span title=\"f/m:zehn\">" +
		     "<span title=\"f/y:ten\">" +
		     "<span title=\"it/is:10\">" +
		     "<span title=\"x/o:zehntens\">" +
		     "c" +
		     "</span>" +
		     "</span>" +
		     "</span>" +
		     "</span>" +
		     "</span>" +
		     "</span>" +
		     "<span class=\"context-right\">" +
		     "</span>",
		     km.getSnippetHTML());
    };

    @Test
    public void indexExample6Relations () throws IOException, QueryException {
	KorapIndex ki = new KorapIndex();
	ki.addDoc(createSimpleFieldDoc());
	ki.commit();

	KorapMatch km = ki.getMatchInfo("match-c1!d1-p0-5(4)8-8(2)7-8",
			     "tokens",
			     "x",
			     null,
			     true,
			     false);

	assertEquals("SnippetBrackets (1)",
		     "[{x/rel:a>3:{x/o:erstens:a}}{x/o:zweitens:b}{x/o:drittens:c}{#3:{x/o:viertens:a}}{x/o:fünftens:b}] ...",
		     km.getSnippetBrackets());

	assertEquals("SnippetBrackets (1)",
		     "<span class=\"context-left\">" +
		     "</span>" +
		     "<span class=\"match\">" +
		     "<span xlink:title=\"x/rel:a\" " +
		     "xlink:type=\"simple\" " +
		     "xlink:href=\"#word-c1!d1-p3\">" +
		     "<span title=\"x/o:erstens\">" +
		     "a" +
		     "</span>" +
		     "</span>" +
		     "<span title=\"x/o:zweitens\">" +
		     "b" +
		     "</span>" +
		     "<span title=\"x/o:drittens\">" +
		     "c" +
		     "</span>" +
		     "<span xml:id=\"word-c1!d1-p3\">" +
		     "<span title=\"x/o:viertens\">" +
		     "a" +
		     "</span>" +
		     "</span>" +
		     "<span title=\"x/o:fünftens\">" +
		     "b" +
		     "</span>" +
		     "</span>" +
		     "<span class=\"context-right\">" +
		     "<span class=\"more\">" +
		     "</span>" +
		     "</span>",
		     km.getSnippetHTML());

	km = ki.getMatchInfo("match-c1!d1-p0-5(7)2-3(4)8-8(2)7-8",
			     "tokens",
			     "x",
			     null,
			     true,
			     true);

	assertEquals("SnippetBrackets (1)",
		     "<span class=\"context-left\">" +
		     "</span>" +
		     "<span class=\"match\">" +
		     "<span xlink:title=\"x/rel:a\" " +
		     "xlink:type=\"simple\" " +
		     "xlink:href=\"#word-c1!d1-p3\">" +
		     "<span title=\"x/o:erstens\">" +
		     "a" +
		     "</span>" +
		     "</span>" +
		     "<span title=\"x/o:zweitens\">" +
		     "b" +
		     "</span>" +
		     "<em class=\"class-7 level-0\">" +
		     "<span title=\"x/o:drittens\">" +
		     "c" +
		     "</span>" +
		     "<span xml:id=\"word-c1!d1-p3\">" +
		     "<span title=\"x/o:viertens\">" +
		     "a" +
		     "</span>" +
		     "</span>" +
		     "</em>" +
		     "<span title=\"x/o:fünftens\">" +
		     "b" +
		     "</span>" +
		     "</span>" +
		     "<span class=\"context-right\">" +
		     "<span class=\"more\">" +
		     "</span>" +
		     "</span>",
		     km.getSnippetHTML());
    };


    @Test
    public void indexExample7SentenceExpansion () throws IOException, QueryException {
	KorapIndex ki = new KorapIndex();
	ki.addDoc(createSimpleFieldDoc());
	ki.addDoc(createSimpleFieldDoc2());
	ki.addDoc(createSimpleFieldDoc3());
	ki.addDoc(createSimpleFieldDoc4());
	ki.commit();
	KorapMatch km;

	km = ki.getMatchInfo("match-c1!d1-p3-4",
			     "tokens",
			     null,
			     null,
			     false,
			     false);

	assertEquals("... [{f/m:vier:{f/y:four:{it/is:4:{x/o:viertens:a}}}}] ...",
		     km.getSnippetBrackets());


	km = ki.getMatchInfo("match-c1!d1-p3-4",
			     "tokens",
			     null,
			     null,
			     false,
			     false,
			     true); // extendToSentence

	assertEquals("[{f/m:drei:{f/y:three:{it/is:3:{x/o:drittens:c}}}}{f/m:vier:{f/y:four:{it/is:4:{x/o:viertens:a}}}}{f/m:fuenf:{f/y:five:{it/is:5:{x/o:fünftens:b}}}}]",
		     km.getSnippetBrackets());

	km = ki.getMatchInfo("match-c1!d3-p3-4",
			     "tokens",
			     null,
			     null,
			     false,
			     false,
			     true); // extendToSentence

	assertEquals("[{f/m:drei:{f/y:three:{it/is:3:{x/o:drittens:cc}}}} {f/m:vier:{f/y:four:{it/is:4:{x/o:viertens:aa}}}} {f/m:fuenf:{f/y:five:{it/is:5:{x/o:fünftens:bb}}}}]",
		     km.getSnippetBrackets());


	km = ki.getMatchInfo("match-c1!d4-p4-6",
			     "tokens",
			     null,
			     null,
			     false,
			     false,
			     true); // extendToSentence
	assertEquals("[{f/m:drei:{f/y:three:{it/is:3:{x/o:drittens:c}}}}{f/m:vier:{f/y:four:{it/is:4:{x/o:viertens:a}}}}{f/m:fuenf:{f/y:five:{it/is:5:{x/o:fünftens:b}}}}{f/m:sechs:{f/y:six:{it/is:6:{x/o:sechstens:c}}}}{f/m:sieben:{f/y:seven:{it/is:7:{x/o:siebtens:a}}}}]",
		     km.getSnippetBrackets());

    };

    @Test
    public void indexExample7Dependencies () throws IOException, QueryException {
	KorapIndex ki = new KorapIndex();
	ki.addDoc(createSimpleFieldDoc2());
	ki.commit();

	KorapMatch km = ki.getMatchInfo("match-c1!d1-p0-4",
			     "tokens",
			     null,
			     null,
			     true,
			     true);


	assertEquals("SnippetHTML (2)",
		     "<span class=\"context-left\">" +
		     "</span>" +
		     "<span class=\"match\">"+
		     "<span xlink:title=\"x/rel:a\" xlink:type=\"simple\" xlink:href=\"#word-c1!d1-p3\">"+
		     "<span title=\"f/m:eins\">"+
		     "<span title=\"f/y:one\">"+
		     "<span title=\"it/is:1\">" +
		     "<span title=\"x/o:erstens\">a</span>" +
		     "</span>" +
		     "</span>" +
		     "</span>" +
		     "</span>" +
		     "<span xlink:title=\"x/rel:b\" xlink:type=\"simple\" xlink:href=\"#word-c1!d1-p3\">"+
		     "<span title=\"f/m:zwei\">"+
		     "<span title=\"f/y:two\">"+
		     "<span title=\"it/is:2\">"+
		     "<span title=\"x/o:zweitens\">b</span>"+
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
		     "<span xml:id=\"word-c1!d1-p3\">"+
		     "<span title=\"f/m:vier\">"+
		     "<span title=\"f/y:four\">"+
		     "<span title=\"it/is:4\">"+
		     "<span title=\"x/o:viertens\">a</span>"+
		     "</span>"+
		     "</span>"+
		     "</span>"+
		     "</span>"+
		     "</span>"+
		     "<span class=\"context-right\">"+
		     "<span class=\"more\">"+
		     "</span>"+
		     "</span>",
		     km.getSnippetHTML());
    };


    @Test
    public void indexExampleMultipleFoundries () throws IOException, QueryException {
	KorapIndex ki = new KorapIndex();
	ki.addDoc(createSimpleFieldDoc4());
	ki.commit();

	KorapMatch km = ki.getMatchInfo("match-c1!d4-p3-9",
					"tokens",
					"f",
					"m",
					false,
					false);
	assertEquals("f:m info",
		     km.getSnippetBrackets(),
		     "... [{f/m:vier:a}{f/m:fuenf:b}{f/m:sechs:c}{f/m:sieben:a}{f/m:acht:b}{f/m:neun:a}] ...");

	km = ki.getMatchInfo("match-c1!d4-p3-9",
			     "tokens",
			     "f",
			     null,
			     false,
			     false);
	assertEquals("f info",
		     km.getSnippetBrackets(),
		     "... [{f/m:vier:{f/y:four:a}}{f/m:fuenf:{f/y:five:b}}{f/m:sechs:{f/y:six:c}}{f/m:sieben:{f/y:seven:a}}{f/m:acht:{f/y:eight:b}}{f/m:neun:{f/y:nine:a}}] ..."
		     );


	km = ki.getMatchInfo("match-c1!d4-p3-4",
			     "tokens",
			     null,
			     null,
			     false,
			     false);
	assertEquals("all info",
		     km.getSnippetBrackets(),
		     "... [{f/m:vier:{f/y:four:{it/is:4:{x/o:viertens:a}}}}] ..."
		     );

	ArrayList<String> foundryList = new ArrayList<>(2);
	foundryList.add("f");
	foundryList.add("x");

	km = ki.getMatchInfo("match-c1!d4-p3-4",
			     "tokens",
			     true,
			     foundryList,
			     (ArrayList<String>) null,
			     false,
			     false,
			     false);
	assertEquals("f|x info",
		     km.getSnippetBrackets(),
		     "... [{f/m:vier:{f/y:four:{x/o:viertens:a}}}] ..."
		     );

	foundryList.clear();
	foundryList.add("y");
	foundryList.add("x");

	km = ki.getMatchInfo("match-c1!d4-p3-4",
			     "tokens",
			     true,
			     foundryList,
			     (ArrayList<String>) null,
			     false,
			     false,
			     false);
	assertEquals("y|x info",
		     km.getSnippetBrackets(),
		     "... [{x/o:viertens:a}] ..."
		     );


	foundryList.clear();
	foundryList.add("f");
	foundryList.add("it");

	ArrayList<String> layerList = new ArrayList<>(2);
	layerList.add("is");

	km = ki.getMatchInfo("match-c1!d4-p3-4",
			     "tokens",
			     true,
			     foundryList,
			     layerList,
			     false,
			     false,
			     false);
	assertEquals("f|it/is",
		     km.getSnippetBrackets(),
		     "... [{it/is:4:a}] ..."
		     );
    };
    

    private FieldDocument createSimpleFieldDoc(){
	FieldDocument fd = new FieldDocument();
	fd.addString("corpusID", "c1");
	fd.addString("ID", "d1");
	fd.addTV("tokens",
		 "abcabcabac",
		 "[(0-1)s:a|i:a|f/m:eins|f/y:one|x/o:erstens|it/is:1|>:x/rel:a$<i>4|_0#0-1|-:t$<i>10]" +
		 "[(1-2)s:b|i:b|f/m:zwei|f/y:two|x/o:zweitens|it/is:2|_1#1-2]" +
		 "[(2-3)s:c|i:c|f/m:drei|f/y:three|x/o:drittens|it/is:3|_2#2-3|<>:s#2-5$<i>5]" +
		 "[(3-4)s:a|i:a|f/m:vier|f/y:four|x/o:viertens|it/is:4|<:x/rel:b$<i>1|_3#3-4]" +
		 "[(4-5)s:b|i:b|f/m:fuenf|f/y:five|x/o:fünftens|it/is:5|_4#4-5]" +
		 "[(5-6)s:c|i:c|f/m:sechs|f/y:six|x/o:sechstens|it/is:6|_5#5-6]" +
		 "[(6-7)s:a|i:a|f/m:sieben|f/y:seven|x/o:siebtens|it/is:7|_6#6-7]" +
		 "[(7-8)s:b|i:b|f/m:acht|f/y:eight|x/o:achtens|it/is:8|<>:x/tag#7-10$<i>10|_7#7-8]" +
		 "[(8-9)s:a|i:a|f/m:neun|f/y:nine|x/o:neuntens|it/is:9|_8#8-9]" +
		 "[(9-10)s:c|i:c|f/m:zehn|f/y:ten|x/o:zehntens|it/is:10|_9#9-10]");
	return fd;
    };

    private FieldDocument createSimpleFieldDoc2(){
	FieldDocument fd = new FieldDocument();
	fd.addString("corpusID", "c1");
	fd.addString("ID", "d1");
	fd.addTV("tokens",
		 "abcabcabac",
		 "[(0-1)s:a|i:a|f/m:eins|f/y:one|x/o:erstens|it/is:1|>:x/rel:a$<i>4|_0#0-1|-:t$<i>10]" +
		 "[(1-2)s:b|i:b|f/m:zwei|f/y:two|x/o:zweitens|it/is:2|>:x/rel:b$<i>4|_1#1-2]" +
		 "[(2-3)s:c|i:c|f/m:drei|f/y:three|x/o:drittens|it/is:3|_2#2-3|<>:s#2-5$<i>5]" +
		 "[(3-4)s:a|i:a|f/m:vier|f/y:four|x/o:viertens|it/is:4|<:x/rel:b$<i>1|_3#3-4]" +
		 "[(4-5)s:b|i:b|f/m:fuenf|f/y:five|x/o:fünftens|it/is:5|_4#4-5]" +
		 "[(5-6)s:c|i:c|f/m:sechs|f/y:six|x/o:sechstens|it/is:6|_5#5-6]" +
		 "[(6-7)s:a|i:a|f/m:sieben|f/y:seven|x/o:siebtens|it/is:7|_6#6-7]" +
		 "[(7-8)s:b|i:b|f/m:acht|f/y:eight|x/o:achtens|it/is:8|<>:x/tag#7-10$<i>10|_7#7-8]" +
		 "[(8-9)s:a|i:a|f/m:neun|f/y:nine|x/o:neuntens|it/is:9|_8#8-9]" +
		 "[(9-10)s:c|i:c|f/m:zehn|f/y:ten|x/o:zehntens|it/is:10|_9#9-10]");
	return fd;
    };

    private FieldDocument createSimpleFieldDoc3(){
	FieldDocument fd = new FieldDocument();
	fd.addString("corpusID", "c1");
	fd.addString("ID", "d3");
	fd.addTV("tokens",
		 "aa bb cc aa bb cc aa bb aa cc ",
		 "[(0-2)s:aa|i:a|f/m:eins|f/y:one|x/o:erstens|it/is:1|>:x/rel:a$<i>4|_0#0-2|-:t$<i>10]" +
		 "[(3-5)s:bb|i:b|f/m:zwei|f/y:two|x/o:zweitens|it/is:2|_1#3-5]" +
		 "[(6-8)s:cc|i:c|f/m:drei|f/y:three|x/o:drittens|it/is:3|_2#6-8|<>:s#6-14$<i>5]" +
		 "[(9-11)s:aa|i:a|f/m:vier|f/y:four|x/o:viertens|it/is:4|<:x/rel:b$<i>1|_3#9-11]" +
		 "[(12-14)s:bb|i:b|f/m:fuenf|f/y:five|x/o:fünftens|it/is:5|_4#12-14]" +
		 "[(15-17)s:cc|i:c|f/m:sechs|f/y:six|x/o:sechstens|it/is:6|_5#15-17]" +
		 "[(18-20)s:aa|i:a|f/m:sieben|f/y:seven|x/o:siebtens|it/is:7|_6#18-20]" +
		 "[(21-23)s:bb|i:b|f/m:acht|f/y:eight|x/o:achtens|it/is:8|<>:x/tag#7-10$<i>10|_7#21-23]" +
		 "[(24-26)s:aa|i:a|f/m:neun|f/y:nine|x/o:neuntens|it/is:9|_8#24-26]" +
		 "[(27-29)s:cc|i:c|f/m:zehn|f/y:ten|x/o:zehntens|it/is:10|_9#27-29]");
	return fd;
    };

    private FieldDocument createSimpleFieldDoc4(){
	FieldDocument fd = new FieldDocument();
	fd.addString("corpusID", "c1");
	fd.addString("ID", "d4");
	fd.addTV("tokens",
		 "abcabcabac",
		 "[(0-1)s:a|i:a|f/m:eins|f/y:one|x/o:erstens|it/is:1|>:x/rel:a$<i>4|_0#0-1|-:t$<i>10]" +
		 "[(1-2)s:b|i:b|f/m:zwei|f/y:two|x/o:zweitens|it/is:2|_1#1-2]" +
		 "[(2-3)s:c|i:c|f/m:drei|f/y:three|x/o:drittens|it/is:3|_2#2-3|<>:s#2-5$<i>5]" +
		 "[(3-4)s:a|i:a|f/m:vier|f/y:four|x/o:viertens|it/is:4|<:x/rel:b$<i>1|_3#3-4]" +
		 "[(4-5)s:b|i:b|f/m:fuenf|f/y:five|x/o:fünftens|it/is:5|_4#4-5]" +
		 "[(5-6)s:c|i:c|f/m:sechs|f/y:six|x/o:sechstens|it/is:6|_5#5-6|<>:s#5-7$<i>7]" +
		 "[(6-7)s:a|i:a|f/m:sieben|f/y:seven|x/o:siebtens|it/is:7|_6#6-7]" +
		 "[(7-8)s:b|i:b|f/m:acht|f/y:eight|x/o:achtens|it/is:8|<>:x/tag#7-10$<i>10|_7#7-8]" +
		 "[(8-9)s:a|i:a|f/m:neun|f/y:nine|x/o:neuntens|it/is:9|_8#8-9]" +
		 "[(9-10)s:c|i:c|f/m:zehn|f/y:ten|x/o:zehntens|it/is:10|_9#9-10]");
	return fd;
    };

};
