package de.ids_mannheim.korap.highlight;

import java.util.*;
import java.io.IOException;

import org.apache.lucene.search.spans.SpanQuery;

import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.KrillQuery;
import de.ids_mannheim.korap.query.QueryBuilder;
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.response.Match;
import de.ids_mannheim.korap.index.FieldDocument;

import de.ids_mannheim.korap.util.QueryException;

import static de.ids_mannheim.korap.TestSimple.*;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestHighlight { // extends LuceneTestCase {

    @Test
    public void checkHighlights () throws IOException, QueryException {

        KrillIndex ki = new KrillIndex();
        String json = new String("{" + "  \"fields\" : [" + "    { "
                + "      \"primaryData\" : \"abc\"" + "    }," + "    {"
                + "      \"name\" : \"tokens\"," + "      \"data\" : ["
                + "         [ \"s:a\", \"i:a\", \"_0#0-1\", \"-:t$<i>3\"],"
                + "         [ \"s:b\", \"i:b\", \"_1#1-2\" ],"
                + "         [ \"s:c\", \"i:c\", \"_2#2-3\" ]" + "      ]"
                + "    }" + "  ]" + "}");

        FieldDocument fd = ki.addDoc(json);
        ki.commit();


        QueryBuilder kq = new QueryBuilder("tokens");
        Result kr = ki
                .search((SpanQuery) kq.seq(kq._(1, kq.seg("s:b"))).toQuery());
        Match km = kr.getMatch(0);
        assertEquals(km.getStartPos(), 1);
        assertEquals(km.getEndPos(), 2);
        assertEquals(km.getStartPos(1), 1);
        assertEquals(km.getEndPos(1), 2);
        assertEquals(
                "<span class=\"context-left\">a</span><span class=\"match\"><mark><mark class=\"class-1 level-0\">b</mark></mark></span><span class=\"context-right\">c</span>",
                km.getSnippetHTML());

        kr = ki.search((SpanQuery) kq.seq(kq._(1, kq.seg("s:b")))
                .append(kq._(2, kq.seg("s:c"))).toQuery());
        km = kr.getMatch(0);
        assertEquals(km.getStartPos(), 1);
        assertEquals(km.getEndPos(), 3);
        assertEquals(km.getStartPos(1), 1);
        assertEquals(km.getEndPos(1), 2);
        assertEquals(km.getStartPos(2), 2);
        assertEquals(km.getEndPos(2), 3);
        assertEquals(
                "<span class=\"context-left\">a</span><span class=\"match\"><mark><mark class=\"class-1 level-0\">b</mark><mark class=\"class-2 level-0\">c</mark></mark></span><span class=\"context-right\"></span>",
                km.getSnippetHTML());


        kr = ki.search((SpanQuery) kq
                .seq(kq._(1, kq.seq(kq.seg("s:a")).append(kq.seg("s:b"))))
                .append(kq._(2, kq.seg("s:c"))).toQuery());
        km = kr.getMatch(0);
        assertEquals(km.getStartPos(), 0);
        assertEquals(km.getEndPos(), 3);
        assertEquals(km.getStartPos(1), 0);
        assertEquals(km.getEndPos(1), 2);
        assertEquals(km.getStartPos(2), 2);
        assertEquals(km.getEndPos(2), 3);
        assertEquals(
                "<span class=\"context-left\"></span><span class=\"match\"><mark><mark class=\"class-1 level-0\">ab</mark><mark class=\"class-2 level-0\">c</mark></mark></span><span class=\"context-right\"></span>",
                km.getSnippetHTML());


        kr = ki.search(
                (SpanQuery) kq._(
                        3, kq
                                .seq(kq._(1,
                                        kq.seq(kq.seg("s:a"))
                                                .append(kq.seg("s:b"))))
                                .append(kq._(2, kq.seg("s:c"))))
                        .toQuery());
        km = kr.getMatch(0);
        assertEquals(km.getStartPos(), 0);
        assertEquals(km.getEndPos(), 3);
        assertEquals(km.getStartPos(1), 0);
        assertEquals(km.getEndPos(1), 2);
        assertEquals(km.getStartPos(2), 2);
        assertEquals(km.getEndPos(2), 3);
        assertEquals(km.getStartPos(3), 0);
        assertEquals(km.getEndPos(3), 3);
        assertEquals(
                "<span class=\"context-left\"></span><span class=\"match\"><mark><mark class=\"class-3 level-0\"><mark class=\"class-1 level-1\">ab</mark><mark class=\"class-2 level-1\">c</mark></mark></mark></span><span class=\"context-right\"></span>",
                km.getSnippetHTML());
    };


    @Test
    public void checkHighlightsManually () throws IOException, QueryException {

        KrillIndex ki = new KrillIndex();
        String json = new String("{" + "  \"fields\" : [" + "    { "
                + "      \"primaryData\" : \"abc\"" + "    }," + "    {"
                + "      \"name\" : \"tokens\"," + "      \"data\" : ["
                + "         [ \"s:a\", \"i:a\", \"_0#0-1\", \"-:t$<i>3\"],"
                + "         [ \"s:b\", \"i:b\", \"_1#1-2\" ],"
                + "         [ \"s:c\", \"i:c\", \"_2#2-3\" ]" + "      ]"
                + "    }" + "  ]" + "}");

        FieldDocument fd = ki.addDoc(json);
        ki.commit();

        QueryBuilder kq = new QueryBuilder("tokens");

        Result kr = ki.search((SpanQuery) kq.seq(kq.seg("s:a"))
                .append(kq.seg("s:b")).append(kq.seg("s:c")).toQuery());
        Match km = kr.getMatch(0);
        km.addHighlight(0, 1, (short) 7);
        assertEquals(
                "<span class=\"context-left\"></span><span class=\"match\"><mark><mark class=\"class-7 level-0\">ab</mark>c</mark></span><span class=\"context-right\"></span>",
                km.getSnippetHTML());

        km.addHighlight(1, 2, (short) 6);
        assertEquals(
                "<span class=\"context-left\"></span><span class=\"match\"><mark><mark class=\"class-7 level-0\">a<mark class=\"class-6 level-1\">b</mark></mark><mark class=\"class-6 level-1\">c</mark></mark></span><span class=\"context-right\"></span>",
                km.getSnippetHTML());

        km.addHighlight(0, 1, (short) 5);
        assertEquals("[[{7:{5:a{6:b}}}{6:c}]]", km.getSnippetBrackets());
        assertEquals(
                "<span class=\"context-left\"></span><span class=\"match\"><mark><mark class=\"class-7 level-0\"><mark class=\"class-5 level-1\">a<mark class=\"class-6 level-2\">b</mark></mark></mark><mark class=\"class-6 level-2\">c</mark></mark></span><span class=\"context-right\"></span>",
                km.getSnippetHTML());

    };


    @Test
    public void highlightMissingBug () throws IOException, QueryException {
        KrillIndex ki = new KrillIndex();
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-1");
        fd.addString("UID", "1");
        fd.addTV("base", "abab",
                "[(0-1)s:a|i:a|_0#0-1|-:t$<i>4]" + "[(1-2)s:b|i:b|_1#1-2]"
                        + "[(2-3)s:a|i:c|_2#2-3]" + "[(3-4)s:b|i:a|_3#3-4]");
        ki.addDoc(fd);
        fd = new FieldDocument();
        fd.addString("ID", "doc-2");
        fd.addString("UID", "2");
        fd.addTV("base", "aba", "[(0-1)s:a|i:a|_0#0-1|-:t$<i>3]"
                + "[(1-2)s:b|i:b|_1#1-2]" + "[(2-3)s:a|i:c|_2#2-3]");
        ki.addDoc(fd);

        // Commit!
        ki.commit();
        fd = new FieldDocument();
        fd.addString("ID", "doc-3");
        fd.addString("UID", "3");
        fd.addTV("base", "abab",
                "[(0-1)s:a|i:a|_0#0-1|-:t$<i>4]" + "[(1-2)s:b|i:b|_1#1-2]"
                        + "[(2-3)s:a|i:c|_2#2-3]" + "[(3-4)s:b|i:a|_3#3-4]");
        ki.addDoc(fd);

        // Commit!
        ki.commit();
        fd = new FieldDocument();
        fd.addString("ID", "doc-4");
        fd.addString("UID", "4");
        fd.addTV("base", "aba", "[(0-1)s:a|i:a|_0#0-1|-:t$<i>3]"
                + "[(1-2)s:b|i:b|_1#1-2]" + "[(2-3)s:a|i:c|_2#2-3]");
        ki.addDoc(fd);

        // Commit!
        ki.commit();

        QueryBuilder kq = new QueryBuilder("base");
        SpanQuery q = (SpanQuery) kq.or(kq._(1, kq.seg("s:a")))
                .or(kq._(2, kq.seg("s:b"))).toQuery();
        Result kr = ki.search(q);
        assertEquals((long) 14, kr.getTotalResults());
        assertEquals("[[{1:a}]]bab", kr.getMatch(0).getSnippetBrackets());
        assertEquals("a[[{2:b}]]ab", kr.getMatch(1).getSnippetBrackets());
        assertEquals("ab[[{1:a}]]b", kr.getMatch(2).getSnippetBrackets());
        assertEquals("aba[[{2:b}]]", kr.getMatch(3).getSnippetBrackets());

        assertEquals("[[{1:a}]]ba", kr.getMatch(4).getSnippetBrackets());
        assertEquals("a[[{2:b}]]a", kr.getMatch(5).getSnippetBrackets());
        assertEquals("ab[[{1:a}]]", kr.getMatch(6).getSnippetBrackets());

        assertEquals("[[{1:a}]]bab", kr.getMatch(7).getSnippetBrackets());
        assertEquals("a[[{2:b}]]ab", kr.getMatch(8).getSnippetBrackets());
        assertEquals("ab[[{1:a}]]b", kr.getMatch(9).getSnippetBrackets());
        assertEquals("aba[[{2:b}]]", kr.getMatch(10).getSnippetBrackets());

        assertEquals("[[{1:a}]]ba", kr.getMatch(11).getSnippetBrackets());
        assertEquals("a[[{2:b}]]a", kr.getMatch(12).getSnippetBrackets());
        assertEquals("ab[[{1:a}]]", kr.getMatch(13).getSnippetBrackets());

        kq = new QueryBuilder("base");
        q = (SpanQuery) kq.or(kq._(1, kq.seg("i:a"))).or(kq._(2, kq.seg("i:c")))
                .toQuery();
        Krill qs = new Krill(q);
        qs.getMeta().getContext().left.setToken(true).setLength((short) 1);
        qs.getMeta().getContext().right.setToken(true).setLength((short) 1);
        kr = ki.search(qs);
        assertEquals((long) 10, kr.getTotalResults());

        assertEquals("[[{1:a}]]b ...", kr.getMatch(0).getSnippetBrackets());
        assertEquals("... b[[{2:a}]]b", kr.getMatch(1).getSnippetBrackets());
        assertEquals("... a[[{1:b}]]", kr.getMatch(2).getSnippetBrackets());
        assertEquals("[[{1:a}]]b ...", kr.getMatch(3).getSnippetBrackets());
        assertEquals("... b[[{2:a}]]", kr.getMatch(4).getSnippetBrackets());
        assertEquals("[[{1:a}]]b ...", kr.getMatch(5).getSnippetBrackets());
        assertEquals("... b[[{2:a}]]b", kr.getMatch(6).getSnippetBrackets());
        assertEquals("... a[[{1:b}]]", kr.getMatch(7).getSnippetBrackets());
        assertEquals("[[{1:a}]]b ...", kr.getMatch(8).getSnippetBrackets());
        assertEquals("... b[[{2:a}]]", kr.getMatch(9).getSnippetBrackets());

        qs.getMeta().getContext().left.setToken(true).setLength((short) 0);
        qs.getMeta().getContext().right.setToken(true).setLength((short) 0);
        kr = ki.search(qs);
        assertEquals((long) 10, kr.getTotalResults());

        assertEquals("[[{1:a}]] ...", kr.getMatch(0).getSnippetBrackets());
        assertEquals("... [[{2:a}]] ...", kr.getMatch(1).getSnippetBrackets());
        assertEquals("... [[{1:b}]]", kr.getMatch(2).getSnippetBrackets());
        assertEquals("[[{1:a}]] ...", kr.getMatch(3).getSnippetBrackets());
        assertEquals("... [[{2:a}]]", kr.getMatch(4).getSnippetBrackets());
        assertEquals("[[{1:a}]] ...", kr.getMatch(5).getSnippetBrackets());
        assertEquals("... [[{2:a}]] ...", kr.getMatch(6).getSnippetBrackets());
        assertEquals("... [[{1:b}]]", kr.getMatch(7).getSnippetBrackets());
        assertEquals("[[{1:a}]] ...", kr.getMatch(8).getSnippetBrackets());
        assertEquals("... [[{2:a}]]", kr.getMatch(9).getSnippetBrackets());

        q = (SpanQuery) kq
                ._(3, kq.or(kq._(1, kq.seg("i:a"))).or(kq._(2, kq.seg("i:c"))))
                .toQuery();
        qs = new Krill(q);
        qs.getMeta().getContext().left.setToken(true).setLength((short) 0);
        qs.getMeta().getContext().right.setToken(true).setLength((short) 0);
        kr = ki.search(qs);
        assertEquals((long) 10, kr.getTotalResults());

        assertEquals("[[{3:{1:a}}]] ...", kr.getMatch(0).getSnippetBrackets());
        assertEquals("... [[{3:{2:a}}]] ...",
                kr.getMatch(1).getSnippetBrackets());
        assertEquals("... [[{3:{1:b}}]]", kr.getMatch(2).getSnippetBrackets());
        assertEquals("[[{3:{1:a}}]] ...", kr.getMatch(3).getSnippetBrackets());
        assertEquals("... [[{3:{2:a}}]]", kr.getMatch(4).getSnippetBrackets());
        assertEquals("[[{3:{1:a}}]] ...", kr.getMatch(5).getSnippetBrackets());
        assertEquals("... [[{3:{2:a}}]] ...",
                kr.getMatch(6).getSnippetBrackets());
        assertEquals("... [[{3:{1:b}}]]", kr.getMatch(7).getSnippetBrackets());
        assertEquals("[[{3:{1:a}}]] ...", kr.getMatch(8).getSnippetBrackets());
        assertEquals("... [[{3:{2:a}}]]", kr.getMatch(9).getSnippetBrackets());
    };


    @Test
    public void highlightGreaterClassBug () throws IOException, QueryException {

        // Construct index
        KrillIndex ki = new KrillIndex();
        // Indexing test files
        for (String i : new String[] { "00001", "00002" }) {
            ki.addDoc(getClass().getResourceAsStream("/wiki/" + i + ".json.gz"),
                    true);
        };
        ki.commit();

        // 15
        String json = getJsonString(getClass()
                .getResource("/queries/bugs/greater_highlights_15.jsonld")
                .getFile());

        Krill ks = new Krill(json);
        Result kr = ks.apply(ki);
        assertEquals(kr.getSerialQuery(), "{15: tokens:s:Alphabet}");
        assertEquals(kr.getTotalResults(), 7);
        assertEquals(kr.getStartIndex(), 0);
        assertEquals(kr.getMatch(0).getSnippetBrackets(),
                "... 2. Herkunft Die aus dem proto-semitischen [[{15:Alphabet}]] stammende Urform des Buchstaben ist wahrscheinlich ...");
        assertEquals(kr.getMatch(0).getSnippetHTML(),
                "<span class=\"context-left\"><span class=\"more\"></span>2. Herkunft Die aus dem proto-semitischen </span><span class=\"match\"><mark><mark class=\"class-15 level-0\">Alphabet</mark></mark></span><span class=\"context-right\"> stammende Urform des Buchstaben ist wahrscheinlich<span class=\"more\"></span></span>");

        json = getJsonString(getClass()
                .getResource("/queries/bugs/greater_highlights_16.jsonld")
                .getFile());

        // 16
        ks = new Krill(json);
        kr = ks.apply(ki);
        assertEquals(kr.getSerialQuery(), "{16: tokens:s:Alphabet}");
        assertEquals(kr.getTotalResults(), 7);
        assertEquals(kr.getStartIndex(), 0);
        assertEquals(kr.getMatch(0).getSnippetBrackets(),
                "... 2. Herkunft Die aus dem proto-semitischen [[{16:Alphabet}]] stammende Urform des Buchstaben ist wahrscheinlich ...");
        assertEquals(kr.getMatch(0).getSnippetHTML(),
                "<span class=\"context-left\"><span class=\"more\"></span>2. Herkunft Die aus dem proto-semitischen </span><span class=\"match\"><mark><mark class=\"class-16 level-0\">Alphabet</mark></mark></span><span class=\"context-right\"> stammende Urform des Buchstaben ist wahrscheinlich<span class=\"more\"></span></span>");

        // 127
        json = getJsonString(getClass()
                .getResource("/queries/bugs/greater_highlights_127.jsonld")
                .getFile());

        ks = new Krill(json);
        kr = ks.apply(ki);
        assertEquals(kr.getSerialQuery(), "{127: tokens:s:Alphabet}");
        assertEquals(kr.getTotalResults(), 7);
        assertEquals(kr.getStartIndex(), 0);
        assertEquals(kr.getMatch(0).getSnippetBrackets(),
                "... 2. Herkunft Die aus dem proto-semitischen [[{127:Alphabet}]] stammende Urform des Buchstaben ist wahrscheinlich ...");
        assertEquals(kr.getMatch(0).getSnippetHTML(),
                "<span class=\"context-left\"><span class=\"more\"></span>2. Herkunft Die aus dem proto-semitischen </span><span class=\"match\"><mark><mark class=\"class-127 level-0\">Alphabet</mark></mark></span><span class=\"context-right\"> stammende Urform des Buchstaben ist wahrscheinlich<span class=\"more\"></span></span>");

        // 255
        json = getJsonString(getClass()
                .getResource("/queries/bugs/greater_highlights_255.jsonld")
                .getFile());

        ks = new Krill(json);
        kr = ks.apply(ki);
        assertEquals(kr.getSerialQuery(), "{255: tokens:s:Alphabet}");
        assertEquals(kr.getTotalResults(), 7);
        assertEquals(kr.getStartIndex(), 0);
        assertEquals(kr.getMatch(0).getSnippetBrackets(),
                "... 2. Herkunft Die aus dem proto-semitischen [[Alphabet]] stammende Urform des Buchstaben ist wahrscheinlich ...");
        assertEquals(kr.getMatch(0).getSnippetHTML(),
                "<span class=\"context-left\"><span class=\"more\"></span>2. Herkunft Die aus dem proto-semitischen </span><span class=\"match\"><mark>Alphabet</mark></span><span class=\"context-right\"> stammende Urform des Buchstaben ist wahrscheinlich<span class=\"more\"></span></span>");

        // 300
        json = getJsonString(getClass()
                .getResource("/queries/bugs/greater_highlights_300.jsonld")
                .getFile());

        ks = new Krill(json);
        kr = ks.apply(ki);
        assertEquals(709, kr.getError(0).getCode());
        assertEquals("Valid class numbers exceeded",
                kr.getError(0).getMessage());

        assertEquals(kr.getError(0).getMessage(),
                "Valid class numbers exceeded");
    };


    @Test
    public void highlightEscapes () throws IOException, QueryException {
        KrillIndex ki = new KrillIndex();
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-1");
        fd.addString("UID", "1");
        fd.addString("textSigle", "c1/d1/1");

        // Make this clean for HTML and Brackets!

        fd.addTV("base", "Mit \"Mann\" & {Ma\\us}",
                "[(0-3)s:Mit|i:mit|_0#0-3|-:t$<i>4|<>:base/t:t$<b>64<i>0<i>20<i>4<b>0]"
                        + "[(4-10)s:\"Mann\"|i:\"mann\"|base/l:\"Mann\"|_1#4-10]"
                        + "[(11-12)s:&|i:&|base/l:&|_2#11-12]"
                        + "[(13-20)s:{Ma\\us}|i:{ma\\us}|_3#13-20]");
        ki.addDoc(fd);

        // Commit!
        ki.commit();
        QueryBuilder kq = new QueryBuilder("base");
        SpanQuery q = (SpanQuery) kq.tag("base/t:t").toQuery();

        Krill qs = new Krill(q);
        qs.getMeta().getContext().left.setToken(true).setLength((short) 0);
        qs.getMeta().getContext().right.setToken(true).setLength((short) 0);

        Result kr = ki.search(qs);
        assertEquals((long) 1, kr.getTotalResults());
        assertEquals("[[Mit \"Mann\" & \\{Ma\\\\us\\}]]",
                kr.getMatch(0).getSnippetBrackets());
        assertEquals(
                "<span class=\"context-left\"></span><span class=\"match\"><mark>Mit &quot;Mann&quot; &amp; {Ma\\us}</mark></span><span class=\"context-right\"></span>",
                kr.getMatch(0).getSnippetHTML());
        assertEquals("match-c1/d1/1-p0-4", kr.getMatch(0).getID());

        Match km = ki.getMatchInfo("match-c1/d1/1-p0-4", "base", true,
                (ArrayList) null, (ArrayList) null, true, true, false);
        assertEquals(0, km.getStartPos());
        assertEquals(
                "<span class=\"context-left\"></span>"
                        + "<span class=\"match\"><mark><span title=\"base/t:t\">"
                        + "Mit " + "<span title=\"base/l:&quot;Mann&quot;\">"
                        + "&quot;Mann&quot;" + "</span>" + " "
                        + "<span title=\"base/l:&amp;\">&amp;</span>" + " "
                        + "{Ma\\us}" + "</span>" + "</mark></span>"
                        + "<span class=\"context-right\"></span>",
                km.getSnippetHTML());
    };
};
