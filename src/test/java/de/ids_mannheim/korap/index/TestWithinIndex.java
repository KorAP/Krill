package de.ids_mannheim.korap.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.*;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import static de.ids_mannheim.korap.TestSimple.*;

import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.util.Bits;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.KrillCollection;
import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.KrillQuery;
import de.ids_mannheim.korap.collection.CollectionBuilder;
import de.ids_mannheim.korap.query.QueryBuilder;
import de.ids_mannheim.korap.query.SpanClassQuery;
import de.ids_mannheim.korap.query.SpanElementQuery;
import de.ids_mannheim.korap.query.SpanFocusQuery;
import de.ids_mannheim.korap.query.wrap.SpanQueryWrapper;
import de.ids_mannheim.korap.query.SpanNextQuery;
import de.ids_mannheim.korap.query.SpanWithinQuery;
import de.ids_mannheim.korap.query.wrap.SpanQueryWrapper;
import de.ids_mannheim.korap.response.Match;
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.util.QueryException;

// mvn -Dtest=TestWithinIndex#indexExample1 test


/**
 * @author diewald
 * @author margaretha
 */
@RunWith(JUnit4.class)
public class TestWithinIndex {

    // Todo: primary data as a non-indexed field separated.

    @Test
    public void indexExample1a () throws IOException {
        KrillIndex ki = new KrillIndex();

        // <a>x<a>y<a>zhij</a>hij</a>hij</a>
        FieldDocument fd = new FieldDocument();
        fd.addTV("base", "x   y   z   h   i   j   h   i   j   h   i   j   ",
                "[(0-3)s:x|<>:a$<b>64<i>0<i>36<i>12<b>0]" + // 1
                        "[(3-6)s:y|<>:a$<b>64<i>3<i>27<i>9<b>0]" + // 2
                        "[(6-9)s:z|<>:a$<b>64<i>6<i>18<i>6<b>0]" + // 3
                        "[(9-12)s:h]" +   // 4
                        "[(12-15)s:i]" +  // 5
                        "[(15-18)s:j]" +  // 6
                        "[(18-21)s:h]" +  // 7
                        "[(21-24)s:i]" +  // 8
                        "[(24-27)s:j]" +  // 9
                        "[(27-30)s:h]" +  // 10
                        "[(30-33)s:i]" +  // 11
                        "[(33-36)s:j]");  // 12
        ki.addDoc(fd);

        ki.commit();

        SpanQuery sq;
        Result kr;

        sq = new SpanWithinQuery(new SpanElementQuery("base", "a"),
                new SpanTermQuery(new Term("base", "s:h")));

        kr = ki.search(sq, (short) 10);

        assertEquals("totalResults", kr.getTotalResults(), 6);

        assertEquals("StartPos (0)", 0, kr.getMatch(0).startPos);
        assertEquals("EndPos (0)", 12, kr.getMatch(0).endPos);
        assertEquals("StartPos (1)", 0, kr.getMatch(1).startPos);
        assertEquals("EndPos (1)", 12, kr.getMatch(1).endPos);
        assertEquals("StartPos (2)", 0, kr.getMatch(2).startPos);
        assertEquals("EndPos (2)", 12, kr.getMatch(2).endPos);
        assertEquals("StartPos (3)", 1, kr.getMatch(3).startPos);
        assertEquals("EndPos (3)", 9, kr.getMatch(3).endPos);
        assertEquals("StartPos (4)", 1, kr.getMatch(4).startPos);
        assertEquals("EndPos (4)", 9, kr.getMatch(4).endPos);
        assertEquals("StartPos (5)", 2, kr.getMatch(5).startPos);
        assertEquals("EndPos (5)", 6, kr.getMatch(5).endPos);

        assertEquals(1, ki.numberOf("documents"));
    };


    @Test
    public void indexExample1b () throws IOException {
        // Cases 9, 12, 13
        KrillIndex ki = new KrillIndex();

        // <a>x<a>y<a>zhij</a>hij</a>hij</a>
        FieldDocument fd = new FieldDocument();
        fd.addTV("base", "x   y   z   h   i   j   h   i   j   h   i   j   ",
                "[(0-3)s:x|<>:a$<b>64<i>0<i>36<i>12<b>0]" + // 1
                        "[(3-6)s:y|<>:a$<b>64<i>3<i>27<i>9<b>0]" + // 2
                        "[(6-9)s:z|<>:a$<b>64<i>6<i>18<i>6<b>0]" + // 3
                        "[(9-12)s:h]" +   // 4
                        "[(12-15)s:i]" +  // 5
                        "[(15-18)s:j]" +  // 6
                        "[(18-21)s:h]" +  // 7
                        "[(21-24)s:i]" +  // 8
                        "[(24-27)s:j]" +  // 9
                        "[(27-30)s:h]" +  // 10
                        "[(30-33)s:i]" +  // 11
                        "[(33-36)s:j]");  // 12
        ki.addDoc(fd);

        // <a>x<a>y<a>zhij</a>hij</a>hij</a>
        fd = new FieldDocument();
        fd.addTV("base", "x   y   z   h   i   j   h   i   j   h   i   j   ",
                "[(0-3)s:x|<>:a$<b>64<i>0<i>36<i>12<b>0]" + // 1
                        "[(3-6)s:y|<>:a$<b>64<i>3<i>27<i>9<b>0]" + // 2
                        "[(6-9)s:z|<>:a$<b>64<i>6<i>18<i>6<b>0]" + // 3
                        "[(9-12)s:h]" +   // 4
                        "[(12-15)s:i]" +  // 5
                        "[(15-18)s:j]" +  // 6
                        "[(18-21)s:h]" +  // 7
                        "[(21-24)s:i]" +  // 8
                        "[(24-27)s:j]" +  // 9
                        "[(27-30)s:h]" +  // 10
                        "[(30-33)s:i]" +  // 11
                        "[(33-36)s:j]");  // 12
        ki.addDoc(fd);


        // Save documents
        ki.commit();

        SpanQuery sq;
        Result kr;

        sq = new SpanWithinQuery(new SpanElementQuery("base", "a"),
                new SpanTermQuery(new Term("base", "s:h")));

        kr = ki.search(sq, (short) 15);

        assertEquals("totalResults", kr.getTotalResults(), 12);

        assertEquals("StartPos (0)", 0, kr.getMatch(0).startPos);
        assertEquals("EndPos (0)", 12, kr.getMatch(0).endPos);
        assertEquals("Doc (0)", 0, kr.getMatch(0).internalDocID);
        assertEquals("StartPos (1)", 0, kr.getMatch(1).startPos);
        assertEquals("EndPos (1)", 12, kr.getMatch(1).endPos);
        assertEquals("Doc (1)", 0, kr.getMatch(1).internalDocID);
        assertEquals("StartPos (2)", 0, kr.getMatch(2).startPos);
        assertEquals("EndPos (2)", 12, kr.getMatch(2).endPos);
        assertEquals("Doc (2)", 0, kr.getMatch(2).internalDocID);
        assertEquals("StartPos (3)", 1, kr.getMatch(3).startPos);
        assertEquals("EndPos (3)", 9, kr.getMatch(3).endPos);
        assertEquals("Doc (3)", 0, kr.getMatch(3).internalDocID);
        assertEquals("StartPos (4)", 1, kr.getMatch(4).startPos);
        assertEquals("EndPos (4)", 9, kr.getMatch(4).endPos);
        assertEquals("Doc (4)", 0, kr.getMatch(4).internalDocID);
        assertEquals("StartPos (5)", 2, kr.getMatch(5).startPos);
        assertEquals("EndPos (5)", 6, kr.getMatch(5).endPos);
        assertEquals("Doc (5)", 0, kr.getMatch(5).internalDocID);

        assertEquals("StartPos (6)", 0, kr.getMatch(6).startPos);
        assertEquals("EndPos (6)", 12, kr.getMatch(6).endPos);
        assertEquals("Doc (6)", 1, kr.getMatch(6).internalDocID);
        assertEquals("StartPos (7)", 0, kr.getMatch(7).startPos);
        assertEquals("EndPos (7)", 12, kr.getMatch(7).endPos);
        assertEquals("Doc (7)", 1, kr.getMatch(7).internalDocID);
        assertEquals("StartPos (8)", 0, kr.getMatch(8).startPos);
        assertEquals("EndPos (8)", 12, kr.getMatch(8).endPos);
        assertEquals("Doc (8)", 1, kr.getMatch(8).internalDocID);
        assertEquals("StartPos (9)", 1, kr.getMatch(9).startPos);
        assertEquals("EndPos (9)", 9, kr.getMatch(9).endPos);
        assertEquals("Doc (9)", 1, kr.getMatch(9).internalDocID);
        assertEquals("StartPos (10)", 1, kr.getMatch(10).startPos);
        assertEquals("EndPos (10)", 9, kr.getMatch(10).endPos);
        assertEquals("Doc (10)", 1, kr.getMatch(10).internalDocID);
        assertEquals("StartPos (11)", 2, kr.getMatch(11).startPos);
        assertEquals("EndPos (11)", 6, kr.getMatch(11).endPos);
        assertEquals("Doc (11)", 1, kr.getMatch(11).internalDocID);

        /*
        for (Match km : kr.getMatches()){		
        	System.out.println(km.getStartPos() +","+km.getEndPos()+" "
                               +km.getSnippetBrackets());
        };	
        */

        assertEquals(2, ki.numberOf("documents"));
    };


    @Test
    public void indexExample1c () throws IOException {
        // Cases 9, 12, 13
        KrillIndex ki = new KrillIndex();

        // <a>x<a>y<a>zhij</a>hij</a>hij</a>
        FieldDocument fd = new FieldDocument();
        fd.addTV("base", "x   y   z   h   i   j   h   i   j   h   i   j   ",
                "[(0-3)s:x|<>:a$<b>64<i>0<i>36<i>12<b>0]" + // 1
                        "[(3-6)s:y|<>:a$<b>64<i>3<i>27<i>9<b>0]" + // 2
                        "[(6-9)s:z|<>:a$<b>64<i>6<i>18<i>6<b>0]" + // 3
                        "[(9-12)s:h]" +   // 4
                        "[(12-15)s:i]" +  // 5
                        "[(15-18)s:j]" +  // 6
                        "[(18-21)s:h]" +  // 7
                        "[(21-24)s:i]" +  // 8
                        "[(24-27)s:j]" +  // 9
                        "[(27-30)s:h]" +  // 10
                        "[(30-33)s:i]" +  // 11
                        "[(33-36)s:j]");  // 12
        ki.addDoc(fd);

        // <a>x<a>y<a>zabc</a>abc</a>abc</a>
        fd = new FieldDocument();
        fd.addTV("base", "x   y   z   a   b   c   a   b   c   a   b   c   ",
                "[(0-3)s:x|<>:a$<b>64<i>0<i>36<i>12<b>0]" + // 1
                        "[(3-6)s:y|<>:a$<b>64<i>3<i>27<i>9<b>0]" + // 2
                        "[(6-9)s:z|<>:a$<b>64<i>6<i>18<i>6<b>0]" + // 3
                        "[(9-12)s:a]" +   // 4
                        "[(12-15)s:b]" +  // 5
                        "[(15-18)s:c]" +  // 6
                        "[(18-21)s:a]" +  // 7
                        "[(21-24)s:b]" +  // 8
                        "[(24-27)s:c]" +  // 9
                        "[(27-30)s:a]" +  // 10
                        "[(30-33)s:b]" +  // 11
                        "[(33-36)s:c]");  // 12
        ki.addDoc(fd);

        // Save documents
        ki.commit();

        SpanQuery sq;
        Result kr;

        sq = new SpanWithinQuery(new SpanElementQuery("base", "a"),
                new SpanTermQuery(new Term("base", "s:h")));

        kr = ki.search(sq, (short) 15);

        assertEquals("totalResults", kr.getTotalResults(), 6);

        assertEquals("StartPos (0)", 0, kr.getMatch(0).startPos);
        assertEquals("EndPos (0)", 12, kr.getMatch(0).endPos);
        assertEquals("Doc (0)", 0, kr.getMatch(0).internalDocID);
        assertEquals("StartPos (1)", 0, kr.getMatch(1).startPos);
        assertEquals("EndPos (1)", 12, kr.getMatch(1).endPos);
        assertEquals("Doc (1)", 0, kr.getMatch(1).internalDocID);
        assertEquals("StartPos (2)", 0, kr.getMatch(2).startPos);
        assertEquals("EndPos (2)", 12, kr.getMatch(2).endPos);
        assertEquals("Doc (2)", 0, kr.getMatch(2).internalDocID);
        assertEquals("StartPos (3)", 1, kr.getMatch(3).startPos);
        assertEquals("EndPos (3)", 9, kr.getMatch(3).endPos);
        assertEquals("Doc (3)", 0, kr.getMatch(3).internalDocID);
        assertEquals("StartPos (4)", 1, kr.getMatch(4).startPos);
        assertEquals("EndPos (4)", 9, kr.getMatch(4).endPos);
        assertEquals("Doc (4)", 0, kr.getMatch(4).internalDocID);
        assertEquals("StartPos (5)", 2, kr.getMatch(5).startPos);
        assertEquals("EndPos (5)", 6, kr.getMatch(5).endPos);
        assertEquals("Doc (5)", 0, kr.getMatch(5).internalDocID);

        assertEquals(2, ki.numberOf("documents"));
    };


    @Test
    public void indexExample1d () throws IOException {
        // Cases 9, 12, 13
        KrillIndex ki = new KrillIndex();

        // <a>x<a>y<a>zhij</a>hij</a>hij</a>
        FieldDocument fd = new FieldDocument();
        fd.addTV("base", "x   y   z   h   i   j   h   i   j   h   i   j   ",
                "[(0-3)s:x|<>:a$<b>64<i>0<i>36<i>12<b>0]" + // 1
                        "[(3-6)s:y|<>:a$<b>64<i>3<i>27<i>9<b>0]" + // 2
                        "[(6-9)s:z|<>:a$<b>64<i>6<i>18<i>6<b>0]" + // 3
                        "[(9-12)s:h]" +   // 4
                        "[(12-15)s:i]" +  // 5
                        "[(15-18)s:j]" +  // 6
                        "[(18-21)s:h]" +  // 7
                        "[(21-24)s:i]" +  // 8
                        "[(24-27)s:j]" +  // 9
                        "[(27-30)s:h]" +  // 10
                        "[(30-33)s:i]" +  // 11
                        "[(33-36)s:j]");  // 12
        ki.addDoc(fd);

        fd = new FieldDocument();
        fd.addTV("base", "x   y   z   h   ",
                "[(0-3)s:x]" +  // 1
                        "[(3-6)s:y]" +  // 2
                        "[(6-9)s:z]" +  // 3
                        "[(9-12)s:h]"); // 4
        ki.addDoc(fd);

        // <a>x<a>y<a>zabc</a>abc</a>abc</a>
        fd = new FieldDocument();
        fd.addTV("base", "x   y   z   a   b   c   a   b   c   a   b   c   ",
                "[(0-3)s:x|<>:a$<b>64<i>0<i>36<i>12<b>0]" + // 1
                        "[(3-6)s:y|<>:a$<b>64<i>3<i>27<i>9<b>0]" + // 2
                        "[(6-9)s:z|<>:a$<b>64<i>6<i>18<i>6<b>0]" + // 3
                        "[(9-12)s:a]" +   // 4
                        "[(12-15)s:b]" +  // 5
                        "[(15-18)s:c]" +  // 6
                        "[(18-21)s:a]" +  // 7
                        "[(21-24)s:b]" +  // 8
                        "[(24-27)s:c]" +  // 9
                        "[(27-30)s:a]" +  // 10
                        "[(30-33)s:b]" +  // 11
                        "[(33-36)s:c]");  // 12
        ki.addDoc(fd);

        // Save documents
        ki.commit();

        SpanQuery sq;
        Result kr;

        sq = new SpanElementQuery("base", "a");
        kr = ki.search(sq, (short) 15);

        sq = new SpanWithinQuery(new SpanElementQuery("base", "a"),
                new SpanTermQuery(new Term("base", "s:h")));

        kr = ki.search(sq, (short) 15);

        assertEquals("totalResults", kr.getTotalResults(), 6);

        assertEquals("StartPos (0)", 0, kr.getMatch(0).startPos);
        assertEquals("EndPos (0)", 12, kr.getMatch(0).endPos);
        assertEquals("Doc (0)", 0, kr.getMatch(0).internalDocID);
        assertEquals("StartPos (1)", 0, kr.getMatch(1).startPos);
        assertEquals("EndPos (1)", 12, kr.getMatch(1).endPos);
        assertEquals("Doc (1)", 0, kr.getMatch(1).internalDocID);
        assertEquals("StartPos (2)", 0, kr.getMatch(2).startPos);
        assertEquals("EndPos (2)", 12, kr.getMatch(2).endPos);
        assertEquals("Doc (2)", 0, kr.getMatch(2).internalDocID);
        assertEquals("StartPos (3)", 1, kr.getMatch(3).startPos);
        assertEquals("EndPos (3)", 9, kr.getMatch(3).endPos);
        assertEquals("Doc (3)", 0, kr.getMatch(3).internalDocID);
        assertEquals("StartPos (4)", 1, kr.getMatch(4).startPos);
        assertEquals("EndPos (4)", 9, kr.getMatch(4).endPos);
        assertEquals("Doc (4)", 0, kr.getMatch(4).internalDocID);
        assertEquals("StartPos (5)", 2, kr.getMatch(5).startPos);
        assertEquals("EndPos (5)", 6, kr.getMatch(5).endPos);
        assertEquals("Doc (5)", 0, kr.getMatch(5).internalDocID);

        assertEquals(3, ki.numberOf("documents"));
    };


    @Test
    public void indexExample2a () throws IOException {
        KrillIndex ki = new KrillIndex();

        // <a><a><a>h</a>hij</a>hij</a>
        FieldDocument fd = new FieldDocument();
        fd.addTV("base",
                // <a><a>hhij</a>hijh</a>ij</a>
                "h  h  i  j  h  i  j  h  i  j        ",
                "[s:h|_0$<i>0<i>3|<>:a$<b>64<i>0<i>12<i>4<b>0|"
                        + "<>:a$<b>64<i>0<i>24<i>8<b>0|"
                        + "<>:a$<b>64<i>0<i>30<i>10<b>0]" + // 1
                        "[s:h|_1$<i>3<i>6]" + // 2
                        "[s:i|_2$<i>6<i>9]" + // 3
                        "[s:j|_3$<i>9<i>12]" + // 4
                        "[s:h|_4$<i>12<i>15]" + // 5
                        "[s:i|_5$<i>15<i>18]" + // 6
                        "[s:j|_6$<i>18<i>21]" + // 7
                        "[s:h|_7$<i>21<i>24]" + // 8
                        "[s:i|_8$<i>24<i>27]" + // 9
                        "[s:j|_9$<i>27<i>30]"); // 10
        ki.addDoc(fd);

        // Save documents
        ki.commit();

        assertEquals(1, ki.numberOf("documents"));

        SpanQuery sq;
        Result kr;

        sq = new SpanElementQuery("base", "a");
        kr = ki.search(sq, (short) 10);

        assertEquals("totalResults", kr.getTotalResults(), 3);
        assertEquals("StartPos (0)", 0, kr.getMatch(0).startPos);
        assertEquals("EndPos (0)", 4, kr.getMatch(0).endPos);
        assertEquals("StartPos (1)", 0, kr.getMatch(1).startPos);
        assertEquals("EndPos (1)", 8, kr.getMatch(1).endPos);
        assertEquals("StartPos (2)", 0, kr.getMatch(2).startPos);
        assertEquals("EndPos (2)", 10, kr.getMatch(2).endPos);

    };


    @Test
    public void indexExample2e () throws IOException {
        KrillIndex ki = new KrillIndex();

        // <a><a><a>h</a>hij</a>hij</a>
        FieldDocument fd = new FieldDocument();
        fd.addTV("base",
                // <a><a>hhij</a>hijh</a>ij</a>
                "h  h  i  j  h  i  j  h  i  j        ",
                "[s:h|_0$<i>0<i>3|<>:a$<b>64<i>0<i>12<i>4<b>0|"
                        + "<>:a$<b>64<i>0<i>24<i>8<b>0|"
                        + "<>:a$<b>64<i>0<i>30<i>10<b>0]" + // 1
                        "[s:h|_1$<i>3<i>6]" + // 2
                        "[s:i|_2$<i>6<i>9]" + // 3
                        "[s:j|_3$<i>9<i>12]" + // 4
                        "[s:h|_4$<i>12<i>15]" + // 5
                        "[s:i|_5$<i>15<i>18]" + // 6
                        "[s:j|_6$<i>18<i>21]" + // 7
                        "[s:h|_7$<i>21<i>24]" + // 8
                        "[s:i|_8$<i>24<i>27]" + // 9
                        "[s:j|_9$<i>27<i>30]"); // 10
        ki.addDoc(fd);

        // Save documents
        ki.commit();

        assertEquals(1, ki.numberOf("documents"));

        SpanQuery sq;
        Result kr;

        sq = new SpanWithinQuery(new SpanElementQuery("base", "a"),
                new SpanTermQuery(new Term("base", "s:h")));

        kr = ki.search(sq, (short) 10);

        // assertEquals("totalResults", 10, kr.getTotalResults());

        assertEquals("StartPos (0)", 0, kr.getMatch(0).startPos);
        assertEquals("EndPos (0)", 4, kr.getMatch(0).endPos);
        assertEquals("Snippet (0)", "[[h  h  i  j  ]]h  i  j  h  i  j   ...",
                kr.getMatch(0).getSnippetBrackets());
        assertEquals("StartPos (1)", 0, kr.getMatch(1).startPos);
        assertEquals("EndPos (1)", 4, kr.getMatch(1).endPos);
        assertEquals("Snippet (1)", "[[h  h  i  j  ]]h  i  j  h  i  j   ...",
                kr.getMatch(1).getSnippetBrackets());

        assertEquals("StartPos (2)", 0, kr.getMatch(2).startPos);
        assertEquals("EndPos (2)", 8, kr.getMatch(2).endPos);
        assertEquals("Snippet (2)", "[[h  h  i  j  h  i  j  h  ]]i  j        ",
                kr.getMatch(2).getSnippetBrackets());
        assertEquals("StartPos (3)", 0, kr.getMatch(3).startPos);
        assertEquals("EndPos (3)", 8, kr.getMatch(3).endPos);
        assertEquals("Snippet (3)", "[[h  h  i  j  h  i  j  h  ]]i  j        ",
                kr.getMatch(3).getSnippetBrackets());
        assertEquals("StartPos (4)", 0, kr.getMatch(4).startPos);
        assertEquals("EndPos (4)", 8, kr.getMatch(4).endPos);
        assertEquals("Snippet (4)", "[[h  h  i  j  h  i  j  h  ]]i  j        ",
                kr.getMatch(4).getSnippetBrackets());
        assertEquals("StartPos (5)", 0, kr.getMatch(5).startPos);
        assertEquals("EndPos (5)", 8, kr.getMatch(5).endPos);
        assertEquals("Snippet (5)", "[[h  h  i  j  h  i  j  h  ]]i  j        ",
                kr.getMatch(5).getSnippetBrackets());


        assertEquals("StartPos (6)", 0, kr.getMatch(6).startPos);
        assertEquals("EndPos (6)", 10, kr.getMatch(6).endPos);
        assertEquals("StartPos (7)", 0, kr.getMatch(7).startPos);
        assertEquals("EndPos (7)", 10, kr.getMatch(7).endPos);
        assertEquals("StartPos (8)", 0, kr.getMatch(8).startPos);
        assertEquals("EndPos (8)", 10, kr.getMatch(8).endPos);
        assertEquals("StartPos (9)", 0, kr.getMatch(9).startPos);
        assertEquals("EndPos (9)", 10, kr.getMatch(9).endPos);
    };


    @Test
    public void indexExample2b () throws IOException {
        KrillIndex ki = new KrillIndex();

        // 6,9,12
        // <a><a><a>h</a>hij</a>hij</a>h
        FieldDocument fd = new FieldDocument();
        fd.addTV("base", "h  h  i  j  h  i  j  h  i  j  h  ",
                "[(0-3)s:h|<>:a$<b>64<i>0<i>12<i>3<b>0|"
                        + "<>:a$<b>64<i>0<i>21<i>6<b>0|"
                        + "<>:a$<b>64<i>0<i>30<i>9<b>0]" + // 1
                        "[(3-6)s:h]" +    // 2
                        "[(6-9)s:i]" +    // 3
                        "[(9-12)s:j]" +   // 4
                        "[(12-15)s:h]" +  // 5
                        "[(15-18)s:i]" +  // 6
                        "[(18-21)s:j]" +  // 7
                        "[(21-24)s:h]" +  // 8
                        "[(24-27)s:i]" +  // 9
                        "[(27-30)s:j]" +  // 10
                        "[(30-33)s:h]");
        ki.addDoc(fd);

        // Save documents
        ki.commit();

        assertEquals(1, ki.numberOf("documents"));

        SpanQuery sq = new SpanElementQuery("base", "a");
        Result kr = ki.search(sq, (short) 10);

        assertEquals("totalResults", kr.getTotalResults(), 3);
        assertEquals("StartPos (0)", 0, kr.getMatch(0).startPos);
        assertEquals("EndPos (0)", 3, kr.getMatch(0).endPos);
        assertEquals("StartPos (1)", 0, kr.getMatch(1).startPos);
        assertEquals("EndPos (1)", 6, kr.getMatch(1).endPos);
        assertEquals("StartPos (2)", 0, kr.getMatch(2).startPos);
        assertEquals("EndPos (2)", 9, kr.getMatch(2).endPos);

        sq = new SpanWithinQuery(new SpanElementQuery("base", "a"),
                new SpanTermQuery(new Term("base", "s:h")));

        kr = ki.search(sq, (short) 10);

        assertEquals("totalResults", kr.getTotalResults(), 9);
        assertEquals("StartPos (0)", 0, kr.getMatch(0).startPos);
        assertEquals("EndPos (0)", 3, kr.getMatch(0).endPos);
        assertEquals("StartPos (1)", 0, kr.getMatch(1).startPos);
        assertEquals("EndPos (1)", 3, kr.getMatch(1).endPos);
        assertEquals("StartPos (2)", 0, kr.getMatch(2).startPos);
        assertEquals("EndPos (2)", 6, kr.getMatch(2).endPos);
        assertEquals("StartPos (3)", 0, kr.getMatch(3).startPos);
        assertEquals("EndPos (3)", 6, kr.getMatch(3).endPos);
        assertEquals("StartPos (4)", 0, kr.getMatch(4).startPos);
        assertEquals("EndPos (4)", 6, kr.getMatch(4).endPos);
        assertEquals("StartPos (5)", 0, kr.getMatch(5).startPos);
        assertEquals("EndPos (5)", 9, kr.getMatch(5).endPos);
        assertEquals("StartPos (6)", 0, kr.getMatch(6).startPos);
        assertEquals("EndPos (6)", 9, kr.getMatch(6).endPos);
        assertEquals("StartPos (7)", 0, kr.getMatch(7).startPos);
        assertEquals("EndPos (7)", 9, kr.getMatch(7).endPos);
        assertEquals("StartPos (8)", 0, kr.getMatch(8).startPos);
        assertEquals("EndPos (8)", 9, kr.getMatch(8).endPos);
    };


    @Test
    public void indexExample2c () throws IOException {
        KrillIndex ki = new KrillIndex();

        // <a><a><a>h  h  i  j  </a>h  i  j  </a>h  i  j  </a>h  <a>i  </a>
        FieldDocument fd = new FieldDocument();
        fd.addTV("base", "h  h  i  j  h  i  j  h  i  j  h  i  ",
                "[(0-3)s:h|<>:a$<b>64<i>0<i>15<i>4<b>0|"
                        + "<>:a$<b>64<i>0<i>21<i>7<b>0|"
                        + "<>:a$<b>64<i>0<i>30<i>10<b>0]" + // 1
                        "[(3-6)s:h]" +    // 2
                        "[(6-9)s:i]" +  // 3
                        "[(9-12)s:j]" +  // 4
                        "[(12-15)s:h]" +  // 5
                        "[(15-18)s:i]" +  // 6
                        "[(18-21)s:j]" +  // 7
                        "[(21-24)s:h]" +  // 8
                        "[(24-27)s:i]" +  // 9
                        "[(27-30)s:j]" +  // 10
                        "[(30-33)s:h]" +  // 11
                        "[(33-36)s:i|<>:a$<b>64<i>33<i>36<i>12<b>0]"); // 12
        ki.addDoc(fd);

        // Save documents
        ki.commit();

        assertEquals(1, ki.numberOf("documents"));

        SpanQuery sq = new SpanElementQuery("base", "a");

        Result kr = ki.search(sq, (short) 10);

        assertEquals("totalResults", kr.getTotalResults(), 4);
        assertEquals("StartPos (0)", 0, kr.getMatch(0).startPos);
        assertEquals("EndPos (0)", 4, kr.getMatch(0).endPos);
        assertEquals("StartPos (1)", 0, kr.getMatch(1).startPos);
        assertEquals("EndPos (1)", 7, kr.getMatch(1).endPos);
        assertEquals("StartPos (2)", 0, kr.getMatch(2).startPos);
        assertEquals("EndPos (2)", 10, kr.getMatch(2).endPos);
        assertEquals("StartPos (3)", 11, kr.getMatch(3).startPos);
        assertEquals("EndPos (3)", 12, kr.getMatch(3).endPos);

        sq = new SpanWithinQuery(new SpanElementQuery("base", "a"),
                new SpanTermQuery(new Term("base", "s:h")));

        kr = ki.search(sq, (short) 10);

        // <a><a><a>h  h  i  j  </a>h  i  j  </a>h  i  j  </a>h  <a>i  </a>

        assertEquals("totalResults", 9, kr.getTotalResults());
        assertEquals("StartPos (0)", 0, kr.getMatch(0).startPos);
        assertEquals("EndPos (0)", 4, kr.getMatch(0).endPos);
        assertEquals("StartPos (1)", 0, kr.getMatch(1).startPos);
        assertEquals("EndPos (1)", 4, kr.getMatch(1).endPos);

        assertEquals("StartPos (2)", 0, kr.getMatch(2).startPos);
        assertEquals("EndPos (2)", 7, kr.getMatch(2).endPos);
        assertEquals("StartPos (3)", 0, kr.getMatch(3).startPos);
        assertEquals("EndPos (3)", 7, kr.getMatch(3).endPos);
        assertEquals("StartPos (4)", 0, kr.getMatch(4).startPos);
        assertEquals("EndPos (4)", 7, kr.getMatch(4).endPos);

        assertEquals("StartPos (5)", 0, kr.getMatch(5).startPos);
        assertEquals("EndPos (5)", 10, kr.getMatch(5).endPos);
        assertEquals("StartPos (6)", 0, kr.getMatch(6).startPos);
        assertEquals("EndPos (6)", 10, kr.getMatch(6).endPos);
        assertEquals("StartPos (7)", 0, kr.getMatch(7).startPos);
        assertEquals("EndPos (7)", 10, kr.getMatch(7).endPos);
        assertEquals("StartPos (8)", 0, kr.getMatch(8).startPos);
        assertEquals("EndPos (8)", 10, kr.getMatch(8).endPos);
    };


    @Test
    public void indexExample2d () throws IOException {
        KrillIndex ki = new KrillIndex();

        // 2, 6, 9, 12, 7
        // <a><a><a>h  h  i  j  </a>h  i  </a>j  h  </a>i  j  <a>h  <a>h  </a></a>
        FieldDocument fd = new FieldDocument();
        fd.addTV("base", "h  h  i  j  h  i  j  h  i  j  h  h  ",
                "[(0-3)s:h|<>:a$<b>64<i>0<i>15<i>4<b>0|"
                        + "<>:a$<b>64<i>0<i>18<i>6<b>0|"
                        + "<>:a$<b>64<i>0<i>27<i>8<b>0|_0$<i>0<i>3]" + // 1
                        "[(3-6)s:h|_1$<i>3<i>6]" + // 2
                        "[(6-9)s:i|_2$<i>6<i>9]" + // 3
                        "[(9-12)s:j|_3$<i>9<i>12]" + // 4
                        "[(12-15)s:h|_4$<i>12<i>15]" + // 5
                        "[(15-18)s:i|_5$<i>15<i>18]" + // 6
                        "[(18-21)s:j|_6$<i>18<i>21]" + // 7
                        "[(21-24)s:h|_7$<i>21<i>24]" + // 8
                        "[(24-27)s:i|_8$<i>24<i>27]" + // 9
                        "[(27-30)s:j|_9$<i>27<i>30]" + // 10
                        "[(30-33)s:h|_10$<i>30<i>33|<>:a$<b>64<i>30<i>36<i>12<b>0]"
                        + // 11
                        "[(33-36)s:h|_11$<i>33<i>36|<>:a$<b>64<i>33<i>36<i>12<b>0]"); // 12
        ki.addDoc(fd);

        // Save documents
        ki.commit();

        assertEquals(1, ki.numberOf("documents"));

        SpanQuery sq = new SpanElementQuery("base", "a");

        Result kr = ki.search(sq, (short) 10);

        assertEquals("totalResults", kr.getTotalResults(), 5);

        // <a><a><a>h  h  i  j  </a>h  i  </a>j  h  </a>i  j  <a>h  <a>h  </a></a>
        assertEquals("StartPos (0)", 0, kr.getMatch(0).startPos);
        assertEquals("EndPos (0)", 4, kr.getMatch(0).endPos);
        assertEquals("StartPos (1)", 0, kr.getMatch(1).startPos);
        assertEquals("EndPos (1)", 6, kr.getMatch(1).endPos);
        assertEquals("StartPos (2)", 0, kr.getMatch(2).startPos);
        assertEquals("EndPos (2)", 8, kr.getMatch(2).endPos);
        assertEquals("StartPos (3)", 10, kr.getMatch(3).startPos);
        assertEquals("EndPos (3)", 12, kr.getMatch(3).endPos);
        assertEquals("StartPos (4)", 11, kr.getMatch(4).startPos);
        assertEquals("EndPos (4)", 12, kr.getMatch(4).endPos);

        sq = new SpanWithinQuery(new SpanElementQuery("base", "a"),
                new SpanTermQuery(new Term("base", "s:h")));

        kr = ki.search(sq, (short) 15);

        // <a><a><a>h  h  i  j  </a>h  i  </a>j  h  </a>i  j  <a>h  <a>h  </a></a>
        assertEquals("totalResults", 12, kr.getTotalResults());

        assertEquals("StartPos (0)", 0, kr.getMatch(0).startPos);
        assertEquals("EndPos (0)", 4, kr.getMatch(0).endPos);
        assertEquals("StartPos (1)", 0, kr.getMatch(1).startPos);
        assertEquals("EndPos (1)", 4, kr.getMatch(1).endPos);

        assertEquals("StartPos (2)", 0, kr.getMatch(2).startPos);
        assertEquals("EndPos (2)", 6, kr.getMatch(2).endPos);
        assertEquals("StartPos (3)", 0, kr.getMatch(3).startPos);
        assertEquals("EndPos (3)", 6, kr.getMatch(3).endPos);
        assertEquals("StartPos (4)", 0, kr.getMatch(4).startPos);
        assertEquals("EndPos (4)", 6, kr.getMatch(4).endPos);

        assertEquals("StartPos (5)", 0, kr.getMatch(5).startPos);
        assertEquals("EndPos (5)", 8, kr.getMatch(5).endPos);
        assertEquals("StartPos (6)", 0, kr.getMatch(6).startPos);
        assertEquals("EndPos (6)", 8, kr.getMatch(6).endPos);
        assertEquals("StartPos (7)", 0, kr.getMatch(7).startPos);
        assertEquals("EndPos (7)", 8, kr.getMatch(7).endPos);
        assertEquals("StartPos (8)", 0, kr.getMatch(8).startPos);
        assertEquals("EndPos (8)", 8, kr.getMatch(8).endPos);

        assertEquals("StartPos (9)", 10, kr.getMatch(9).startPos);
        assertEquals("EndPos (9)", 12, kr.getMatch(9).endPos);
        assertEquals("StartPos (10)", 10, kr.getMatch(10).startPos);
        assertEquals("EndPos (10)", 12, kr.getMatch(10).endPos);

        assertEquals("StartPos (11)", 11, kr.getMatch(11).startPos);
        assertEquals("EndPos (11)", 12, kr.getMatch(11).endPos);
    };


    @Test
    public void indexExample3 () throws IOException {
        KrillIndex ki = new KrillIndex();

        // <a><a><a>u</a></a></a>
        FieldDocument fd = new FieldDocument();
        fd.addTV("base", "xyz", "[(0-3)s:xyz|<>:a$<b>64<i>0<i>3<i>0<b>0|"
                + "<>:a$<b>64<i>0<i>3<i>0<b>0|" + "<>:a$<b>64<i>0<i>3<i>0<b>0|"
                + "<>:b$<b>64<i>0<i>3<i>0<b>0]");
        ki.addDoc(fd);

        // <a><b>x<a>y<a>zcde</a>cde</a>cde</b></a>
        fd = new FieldDocument();
        fd.addTV("base", "x   y   z   c   d   e   c   d   e   c   d   e   ",
                "[(0-3)s:x|<>:a$<b>64<i>0<i>36<i>12<b>0|"
                        + "<>:b$<b>64<i>0<i>36<i>12<b>0]"
                        + "[(3-6)s:y|<>:a$<b>64<i>3<i>27<i>9<b>0]"
                        + "[(6-9)s:z|<>:a$<b>64<i>6<i>18<i>6<b>0]"
                        + "[(9-12)s:c]" + "[(12-15)s:d]" + "[(15-18)s:e]"
                        + "[(18-21)s:c]" + "[(21-24)s:d]" + "[(24-27)s:e]"
                        + "[(27-30)s:c]" + "[(30-33)s:d]" + "[(33-36)s:e]");
        ki.addDoc(fd);

        // xyz
        fd = new FieldDocument();
        fd.addTV("base", "x   y   z   ",
                "[(0-3)s:x]" + "[(3-6)s:y]" + "[(6-9)s:z]");
        ki.addDoc(fd);

        // <a>x<a><b>y<a>zcde</a>cde</b></a>cde</a>
        fd = new FieldDocument();
        fd.addTV("base", "x   y   z   k   l   m   k   l   m   k   l   m   ",
                "[(0-3)s:x|<>:a$<b>64<i>0<i>3<i>12<b>0]"
                        + "[(3-6)s:y|<>:a$<b>64<i>3<i>6<i>9<b>0|"
                        + "<>:b$<b>64<i>3<i>6<i>9<b>0]"
                        + "[(6-9)s:z|<>:a$<b>64<i>6<i>9<i>6<b>0]"
                        + "[(9-12)s:k]" + "[(12-15)s:l]" + "[(15-18)s:m]"
                        + "[(18-21)s:k]" + "[(21-24)s:l]" + "[(24-27)s:m]"
                        + "[(27-30)s:k]" + "[(30-33)s:l]" + "[(33-36)s:m]");
        ki.addDoc(fd);

        // <a><a><a>h</a>hhij</a>hij</a>hij</a>
        fd = new FieldDocument();
        fd.addTV("base", "h   i   j   h   i   j   h   i   j   ",
                "[(0-3)s:h|" + "<>:a$<b>64<i>0<i>18<i>3<b>0|"
                        + "<>:a$<b>64<i>0<i>27<i>6<b>0|"
                        + "<>:a$<b>64<i>0<i>36<i>9<b>0]" + "[(3-6)s:h]"
                        + "[(12-15)s:i]" + "[(15-18)s:j]" + "[(18-21)s:h]"
                        + "[(21-24)s:i]" + "[(24-27)s:j]" + "[(27-30)s:h]"
                        + "[(30-33)s:i]" + "[(33-36)s:j]");
        ki.addDoc(fd);

        // xyz
        fd = new FieldDocument();
        fd.addTV("base", "a  b  c  ",
                "[(0-3)s:a]" + "[(3-6)s:b]" + "[(6-9)s:c]");
        ki.addDoc(fd);

        // Save documents
        ki.commit();

        assertEquals(6, ki.numberOf("documents"));

        SpanQuery sq = new SpanElementQuery("base", "a");

        Result kr = ki.search(sq, (short) 15);

        assertEquals("totalResults", kr.getTotalResults(), 12);
        assertEquals("StartPos (0)", 0, kr.getMatch(0).startPos);
        assertEquals("EndPos (0)", 0, kr.getMatch(0).endPos);
        assertEquals("StartPos (1)", 0, kr.getMatch(1).startPos);
        assertEquals("EndPos (1)", 0, kr.getMatch(1).endPos);
        assertEquals("StartPos (2)", 0, kr.getMatch(2).startPos);
        assertEquals("EndPos (2)", 0, kr.getMatch(2).endPos);
        assertEquals("StartPos (3)", 0, kr.getMatch(3).startPos);
        assertEquals("EndPos (3)", 12, kr.getMatch(3).endPos);
        assertEquals("StartPos (4)", 1, kr.getMatch(4).startPos);
        assertEquals("EndPos (4)", 9, kr.getMatch(4).endPos);
        assertEquals("StartPos (5)", 2, kr.getMatch(5).startPos);
        assertEquals("EndPos (5)", 6, kr.getMatch(5).endPos);

        assertEquals("StartPos (6)", 0, kr.getMatch(6).startPos);
        assertEquals("EndPos (6)", 12, kr.getMatch(6).endPos);
        assertEquals("StartPos (7)", 1, kr.getMatch(7).startPos);
        assertEquals("EndPos (7)", 9, kr.getMatch(7).endPos);
        assertEquals("StartPos (8)", 2, kr.getMatch(8).startPos);
        assertEquals("EndPos (8)", 6, kr.getMatch(8).endPos);

        assertEquals("StartPos (9)", 0, kr.getMatch(9).startPos);
        assertEquals("EndPos (9)", 3, kr.getMatch(9).endPos);
        assertEquals("StartPos (10)", 0, kr.getMatch(10).startPos);
        assertEquals("EndPos (10)", 6, kr.getMatch(10).endPos);
        assertEquals("StartPos (11)", 0, kr.getMatch(11).startPos);
        assertEquals("EndPos (11)", 9, kr.getMatch(11).endPos);
    };


    @Test
    public void indexExample3Offsets () throws IOException {
        KrillIndex ki = new KrillIndex();

        // Er schrie: <s>"Das war ich!"</s>
        FieldDocument fd = new FieldDocument();
        fd = new FieldDocument();
        fd.addTV("base", "Er schrie: \"Das war ich!\" und ging.",
                "[(0-2)s:Er|_0$<i>0<i>3]" + "[(3-9)s:schrie|_1$<i>3<i>9]"
                        + "[(12-15)s:Das|_2$<i>12<i>15|<>:sentence$<b>64<i>11<i>25<i>5<b>0]"
                        + "[(16-19)s:war|_3$<i>16<i>19]"
                        + "[(20-23)s:ich|_4$<i>20<i>23]"
                        + "[(26-29)s:und|_5$<i>26<i>29]"
                        + "[(30-34)s:ging|_6$<i>30<i>34]");
        ki.addDoc(fd);

        // Save documents
        ki.commit();

        SpanQuery sq = new SpanClassQuery(
                new SpanElementQuery("base", "sentence"), (byte) 3);
        Result kr;
        kr = ki.search(sq, 0, (short) 15, true, (short) 1, true, (short) 1);
        assertEquals("totalResults", kr.getTotalResults(), 1);

        assertEquals("... schrie: [[\"{3:Das war ich}!\"]] und ...",
                kr.getMatch(0).getSnippetBrackets());
        assertEquals(
                "<span class=\"context-left\"><span class=\"more\"></span>schrie: </span><span class=\"match\"><mark>&quot;<mark class=\"class-3 level-0\">Das war ich</mark>!&quot;</mark></span><span class=\"context-right\"> und<span class=\"more\"></span></span>",
                kr.getMatch(0).getSnippetHTML());

        kr = ki.search(sq, 0, (short) 15, true, (short) 0, true, (short) 0);
        assertEquals("... [[\"{3:Das war ich}!\"]] ...",
                kr.getMatch(0).getSnippetBrackets());
        assertEquals("totalResults", kr.getTotalResults(), 1);

        kr = ki.search(sq, 0, (short) 15, true, (short) 6, true, (short) 6);
        assertEquals("Er schrie: [[\"{3:Das war ich}!\"]] und ging.",
                kr.getMatch(0).getSnippetBrackets());
        assertEquals("totalResults", kr.getTotalResults(), 1);

        kr = ki.search(sq, 0, (short) 15, true, (short) 2, true, (short) 2);
        assertEquals("Er schrie: [[\"{3:Das war ich}!\"]] und ging ...",
                kr.getMatch(0).getSnippetBrackets());
        assertEquals("totalResults", kr.getTotalResults(), 1);

        sq = new SpanClassQuery(
                new SpanWithinQuery(new SpanElementQuery("base", "sentence"),
                        new SpanClassQuery(
                                new SpanTermQuery(new Term("base", "s:Das")),
                                (byte) 2)),
                (byte) 1);

        kr = ki.search(sq, (short) 15);
        assertEquals("Er schrie: [[\"{1:{2:Das} war ich}!\"]] und ging.",
                kr.getMatch(0).getSnippetBrackets());
        assertEquals("totalResults", kr.getTotalResults(), 1);

        sq = new SpanClassQuery(
                new SpanWithinQuery(new SpanElementQuery("base", "sentence"),
                        new SpanClassQuery(
                                new SpanTermQuery(new Term("base", "s:war")),
                                (byte) 2)),
                (byte) 1);

        kr = ki.search(sq, (short) 15);
        assertEquals("Er schrie: [[\"{1:Das {2:war} ich}!\"]] und ging.",
                kr.getMatch(0).getSnippetBrackets());
        assertEquals("totalResults", kr.getTotalResults(), 1);

        sq = new SpanClassQuery(
                new SpanWithinQuery(new SpanElementQuery("base", "sentence"),
                        new SpanClassQuery(
                                new SpanTermQuery(new Term("base", "s:ich")),
                                (byte) 2)),
                (byte) 1);

        kr = ki.search(sq, (short) 15);
        assertEquals("Er schrie: [[\"{1:Das war {2:ich}}!\"]] und ging.",
                kr.getMatch(0).getSnippetBrackets());
        assertEquals("totalResults", kr.getTotalResults(), 1);

        sq = new SpanClassQuery(
                new SpanWithinQuery(new SpanElementQuery("base", "sentence"),
                        new SpanClassQuery(
                                new SpanTermQuery(new Term("base", "s:und")),
                                (byte) 2)),
                (byte) 1);

        kr = ki.search(sq, (short) 15);
        assertEquals("totalResults", kr.getTotalResults(), 0);

        sq = new SpanClassQuery(
                new SpanWithinQuery(new SpanElementQuery("base", "sentence"),
                        new SpanClassQuery(
                                new SpanTermQuery(new Term("base", "s:schrie")),
                                (byte) 2)),
                (byte) 1);

        kr = ki.search(sq, (short) 15);
        assertEquals("totalResults", kr.getTotalResults(), 0);
    };


    @Test
    public void indexExample4 () throws IOException {
        KrillIndex ki = new KrillIndex();

        // Case 1, 6, 7, 13
        // xy<a><a>x</a>b<a>c</a></a>x
        FieldDocument fd = new FieldDocument();
        fd.addTV("base", "x  y  x  b  c  x  ",
                "[(0-3)s:x|_0$<i>0<i>3]" + "[(3-6)s:y|_1$<i>3<i>6]"
                        + "[(6-9)s:x|_2$<i>6<i>9|<>:a$<b>64<i>6<i>9<i>3<b>0|"
                        + "<>:a$<b>64<i>6<i>15<i>5<b>0]"
                        + "[(9-12)s:b|_3$<i>9<i>12]"
                        + "[(12-15)s:c|_4$<i>12<i>15|<>:a$<b>64<i>12<i>15<i>5<b>0]"
                        + "[(15-18)s:x|_5$<i>15<i>18]");
        ki.addDoc(fd);

        // Save documents
        ki.commit();

        assertEquals(1, ki.numberOf("documents"));

        SpanQuery sq = new SpanWithinQuery(new SpanElementQuery("base", "a"),
                new SpanTermQuery(new Term("base", "s:x")));

        assertEquals("spanContain(<base:a />, base:s:x)", sq.toString());
        Result kr = ki.search(sq, (short) 10);

        assertEquals("totalResults", kr.getTotalResults(), 2);

        assertEquals("x  y  [[x  ]]b  c  x  ",
                kr.getMatch(0).getSnippetBrackets());
        assertEquals("x  y  [[x  b  c  ]]x  ",
                kr.getMatch(1).getSnippetBrackets());

        assertEquals("StartPos (0)", 2, kr.getMatch(0).startPos);
        assertEquals("EndPos (0)", 3, kr.getMatch(0).endPos);
        assertEquals("StartPos (1)", 2, kr.getMatch(1).startPos);
        assertEquals("EndPos (1)", 5, kr.getMatch(1).endPos);
    };


    @Test
    public void indexExample5 () throws IOException {
        // 1,2,3,6,9,10,12
        KrillIndex ki = new KrillIndex();

        // hij<a>hi<a>h<a>ij</a></a>hi</a>
        FieldDocument fd = new FieldDocument();
        fd.addTV("base", "hijhihijhi",
                "[(0-1)s:h|i:h|_0$<i>0<i>1|-:a$<i>3|-:t$<i>10]"
                        + "[(1-2)s:i|i:i|_1$<i>1<i>2]"
                        + "[(2-3)s:j|i:j|_2$<i>2<i>3]"
                        + "[(3-4)s:h|i:h|_3$<i>3<i>4|<>:a$<b>64<i>3<i>10<i>10<b>0]"
                        + "[(4-5)s:i|i:i|_4$<i>4<i>5]"
                        + "[(5-6)s:h|i:h|_5$<i>5<i>6|<>:a$<b>64<i>5<i>8<i>8<b>0]"
                        + "[(6-7)s:i|i:i|_6$<i>6<i>7|<>:a$<b>64<i>6<i>8<i>8<b>0]"
                        + "[(7-8)s:j|i:j|_7$<i>7<i>8]"
                        + "[(8-9)s:h|i:h|_8$<i>8<i>9]"
                        + "[(9-10)s:i|i:i|_9$<i>9<i>10]");
        ki.addDoc(fd);

        // Save documents
        ki.commit();

        assertEquals(1, ki.numberOf("documents"));

        SpanQuery sq = new SpanWithinQuery(new SpanElementQuery("base", "a"),
                new SpanNextQuery(new SpanTermQuery(new Term("base", "s:h")),
                        new SpanTermQuery(new Term("base", "s:i"))));

        Result kr = ki.search(sq, (short) 10);

        assertEquals("totalResults", kr.getTotalResults(), 4);

        assertEquals("StartPos (0)", 3, kr.getMatch(0).startPos);
        assertEquals("EndPos (0)", 10, kr.getMatch(0).endPos);
        assertEquals("StartPos (1)", 3, kr.getMatch(1).startPos);
        assertEquals("EndPos (1)", 10, kr.getMatch(1).endPos);
        assertEquals("StartPos (2)", 3, kr.getMatch(2).startPos);
        assertEquals("EndPos (2)", 10, kr.getMatch(2).endPos);
        assertEquals("StartPos (3)", 5, kr.getMatch(3).startPos);
        assertEquals("EndPos (3)", 8, kr.getMatch(3).endPos);
    };


    @Test
    public void indexExample6 () throws IOException {
        KrillIndex ki = new KrillIndex();
        // 2,5,8,12,13
        // h<a><a>i</a>j</a><a>h</a>i j<a>h i</a>j
        FieldDocument fd = new FieldDocument();
        fd.addTV("base", "hijhi jh ij",
                "[(0-1)s:h|i:h|_0$<i>0<i>1|-:a$<i>4|-:t$<i>9]"
                        + "[(1-2)s:i|i:i|_1$<i>1<i>2|<>:a$<b>64<i>1<i>2<i>2<b>0|"
                        + "<>:a$<b>64<i>1<i>3<i>3<b>0]"
                        + "[(2-3)s:j|i:j|_2$<i>2<i>3]"
                        + "[(3-4)s:h|i:h|_3$<i>3<i>4|<>:a$<b>64<i>3<i>4<i>4<b>0]"
                        + "[(4-5)s:i|i:i|_4$<i>4<i>5]"
                        + "[(6-7)s:j|i:j|_5$<i>6<i>7]"
                        + "[(7-8)s:h|i:h|_6$<i>7<i>8|<>:a$<b>64<i>7<i>10<i>8<b>0]"
                        + "[(9-10)s:i|i:i|_7$<i>9<i>10]"
                        + "[(10-11)s:j|i:j|_8$<i>10<i>11]");
        ki.addDoc(fd);

        // Save documents
        ki.commit();

        assertEquals(1, ki.numberOf("documents"));

        SpanQuery sq = new SpanWithinQuery(new SpanElementQuery("base", "a"),
                new SpanNextQuery(new SpanTermQuery(new Term("base", "s:h")),
                        new SpanNextQuery(
                                new SpanTermQuery(new Term("base", "s:i")),
                                new SpanTermQuery(new Term("base", "s:j")))));

        Result kr = ki.search(sq, (short) 10);
        assertEquals("totalResults", kr.getTotalResults(), 0);
    };


    @Test
    public void indexExample7 () throws IOException {
        KrillIndex ki = new KrillIndex();
        // 4,5,11,13
        // x<a>x h</a>i j h<a>i j</a>
        FieldDocument fd = new FieldDocument();
        fd.addTV("base", "xx hi j hi j",
                "[(0-1)s:x|i:x|_0$<i>0<i>1|-:a$<i>2|-:t$<i>8]"
                        + "[(1-2)s:x|i:x|_1$<i>1<i>2|<>:a$<b>64<i>1<i>4<i>3<b>0]"
                        + "[(3-4)s:h|i:h|_2$<i>3<i>4]"
                        + "[(4-5)s:i|i:i|_3$<i>4<i>5]"
                        + "[(6-7)s:j|i:j|_4$<i>6<i>7]"
                        + "[(8-9)s:h|i:h|_5$<i>8<i>9]"
                        + "[(9-10)s:i|i:i|_6$<i>9<i>10|<>:a$<b>64<i>9<i>12<i>8<b>0]"
                        + "[(11-12)s:j|i:j|_7$<i>11<i>12]");
        ki.addDoc(fd);

        // Save documents
        ki.commit();

        assertEquals(1, ki.numberOf("documents"));

        SpanQuery sq = new SpanWithinQuery(new SpanElementQuery("base", "a"),
                new SpanNextQuery(new SpanTermQuery(new Term("base", "s:h")),
                        new SpanNextQuery(
                                new SpanTermQuery(new Term("base", "s:i")),
                                new SpanTermQuery(new Term("base", "s:j")))));

        Result kr = ki.search(sq, (short) 10);

        assertEquals("totalResults", kr.getTotalResults(), 0);
    };


    /** SpanElementQueries */
    @Test
    public void indexExample8 () throws QueryException, IOException {
        KrillIndex ki = new KrillIndex();
        FieldDocument fd = new FieldDocument();
        // <a>xx <e>hi j <e>hi j</e></e></a>
        fd.addTV("base", "xx hi j hi j",
                "[(0-1)s:x|i:x|_0$<i>0<i>1|<>:a$<b>64<i>0<i>12<i>8<b>0]"
                        + "[(1-2)s:x|i:x|_1$<i>1<i>2]"
                        + "[(3-4)s:h|i:h|_2$<i>3<i>4|<>:e$<b>64<i>3<i>12<i>8<b>0]"
                        + "[(4-5)s:i|i:i|_3$<i>4<i>5]"
                        + "[(6-7)s:j|i:j|_4$<i>6<i>7]"
                        + "[(8-9)s:h|i:h|_5$<i>8<i>9|<>:e$<b>64<i>8<i>9<i>8<b>0]"
                        + "[(9-10)s:i|i:i|_6$<i>9<i>10]"
                        + "[(11-12)s:j|i:j|_7$<i>11<i>12]");
        ki.addDoc(fd);
        ki.commit();


        assertEquals(1, ki.numberOf("documents"));

        QueryBuilder qb = new KrillQuery("base").builder();
        SpanQueryWrapper sqw;
        Result kr;
        /*
        sqw = qb.seg("i:x");
        kr = ki.search(sqw.toQuery(), (short) 10);
        assertEquals(2, kr.getTotalResults());
        
        sqw = qb.tag("a");
        kr = ki.search(sqw.toQuery(), (short) 10);
        assertEquals(1, kr.getTotalResults());
        
        sqw = qb.startswith(qb.tag("a"), qb.seg("i:x"));
        assertEquals("spanStartsWith(<base:a />, base:i:x)",
                     sqw.toQuery().toString());
        kr = ki.search(sqw.toQuery(), (short) 10);
        assertEquals(1, kr.getTotalResults());
        */
        sqw = qb.startswith(qb.tag("e"), qb.seg("i:h"));
        assertEquals("spanStartsWith(<base:e />, base:i:h)",
                sqw.toQuery().toString());
        kr = ki.search(sqw.toQuery(), (short) 10);
        assertEquals(2, kr.getTotalResults());
    };


    // contains(<s>, (es wird | wird es))
    @Test
    public void queryJSONpoly2 () throws QueryException, IOException {
        String jsonPath = getClass().getResource("/queries/poly2.json").getFile();
        String jsonPQuery = getJsonString(jsonPath);
        KrillQuery kq = new KrillQuery("tokens");
        SpanQueryWrapper sqwi = kq.fromKoral(jsonPQuery);
        
        SpanWithinQuery sq = (SpanWithinQuery) sqwi.toQuery();

        KrillIndex ki = new KrillIndex();

        ki.addDoc(getClass().getResourceAsStream("/wiki/DDD-08370.json.gz"),
                true);
        ki.addDoc(getClass().getResourceAsStream("/wiki/PPP-02924.json.gz"),
                true);

        ki.commit();
        Result kr = ki.search(sq, (short) 10);
        assertTrue(!kq.hasErrors());
        assertTrue(!kq.hasWarnings());
        assertTrue(kq.hasMessages());
        assertEquals("'isAround' will have a different meaning in the future and is therefore temporarily deprecated in favor of 'contains'",
                     kq.getMessage(0).getMessage());

        assertEquals(2, kr.getTotalResults());
        assertEquals(0, kr.getMatch(0).getLocalDocID());
        assertEquals(76, kr.getMatch(0).getStartPos());
        assertEquals(93, kr.getMatch(0).getEndPos());
        assertEquals(1, kr.getMatch(1).getLocalDocID());
        assertEquals(237, kr.getMatch(1).getStartPos());
        assertEquals(252, kr.getMatch(1).getEndPos());
    };


    @Test
    public void queryJSONcomplexSpanOrTerm ()
            throws QueryException, IOException {
        /*
          at org.apache.lucene.search.spans.SpanOrQuery$1.doc(SpanOrQuery.java:234)
          at de.ids_mannheim.korap.query.spans.WithinSpans.toSameDoc(WithinSpans.java:423)
          at de.ids_mannheim.korap.query.spans.WithinSpans.next(WithinSpans.java:375)
          at de.ids_mannheim.korap.KrillIndex.search(KrillIndex.java:1293)
          at de.ids_mannheim.korap.Krill.apply(Krill.java:304)
        */

        String jsonPath = getClass()
                .getResource("/queries/bugs/span_or_bug.jsonld").getFile();
        String jsonPQuery = getJsonString(jsonPath);
        SpanQueryWrapper sqwi = new KrillQuery("tokens").fromKoral(jsonPQuery);

        SpanWithinQuery sq = (SpanWithinQuery) sqwi.toQuery();

        assertEquals(
                "spanStartsWith(<tokens:base/s:s />, "
                        + "spanOr([tokens:s:Er, tokens:s:Sie]))",
                sq.toString());

        KrillIndex ki = new KrillIndex();

        ki.addDoc(getClass().getResourceAsStream("/wiki/DDD-08370.json.gz"),
                true);
        ki.addDoc(getClass().getResourceAsStream("/wiki/SSS-09803.json.gz"),
                true);
        ki.commit();
        Result kr = ki.search(sq, (short) 1);
        assertEquals(1, kr.getTotalResults());
    }


    // Build one "tokens" FieldDocument from sentence token arrays.
    // Each input array is one sentence, e.g. ["b","a","w2"].
    // Example doc: [["b","a","w2"],["a","w4","w5"]].
    private FieldDocument multiSentenceDoc (String[]... sentences) {
        FieldDocument fd = new FieldDocument();

        int total = 0;
        for (String[] s : sentences) total += s.length;

        StringBuilder surface = new StringBuilder();
        StringBuilder tv = new StringBuilder();

        int tokIdx = 0;
        int charOff = 0;
        int sentStart = 0;

        for (int si = 0; si < sentences.length; si++) {
            String[] sent = sentences[si];
            int sentEnd = sentStart + sent.length;

            for (int ti = 0; ti < sent.length; ti++) {
                String tok = sent[ti];
                int cStart = charOff;
                int cEnd = charOff + tok.length();

                surface.append(tok);

                tv.append("[(").append(cStart).append("-").append(cEnd)
                        .append(")");
                tv.append("s:").append(tok);

                if (tokIdx == 0) {
                    int totalChars = 0;
                    for (String[] s : sentences)
                        for (String t : s)
                            totalChars += t.length();
                    tv.append("|-:t$<i>").append(total);
                    tv.append("|<>:base/s:t$<b>64<i>0<i>")
                            .append(totalChars).append("<i>")
                            .append(total).append("<b>0");
                }
                if (ti == 0) {
                    int sentCharEnd = cStart;
                    for (String t : sent) sentCharEnd += t.length();
                    tv.append("|<>:base/s:s$<b>64<i>")
                            .append(cStart).append("<i>")
                            .append(sentCharEnd).append("<i>")
                            .append(sentEnd).append("<b>1");
                }

                tv.append("|_").append(tokIdx).append("$<i>")
                        .append(cStart).append("<i>").append(cEnd);
                tv.append("]");

                charOff = cEnd;
                tokIdx++;
            }
            sentStart = sentEnd;
        }

        fd.addTV("tokens", surface.toString(), tv.toString());
        return fd;
    }

    // focus(1: contains(contains(<s>, {1: s:a}), {2: s:b}))
    private SpanQuery nestedContainsDiffClassQuery () {
        SpanQuery sentence = new SpanElementQuery("tokens", "base/s:s");
        SpanQuery tokenA = new SpanClassQuery(
                new SpanTermQuery(new Term("tokens", "s:a")), (byte) 1);
        SpanQuery tokenB = new SpanClassQuery(
                new SpanTermQuery(new Term("tokens", "s:b")), (byte) 2);
        SpanQuery innerContains = new SpanWithinQuery(sentence, tokenA);
        SpanQuery outerContains = new SpanWithinQuery(innerContains, tokenB);
        SpanFocusQuery focus = new SpanFocusQuery(outerContains, (byte) 1);
        focus.setSorted(false);
        return focus;
    }

    /**
     * Two docs in one segment where doc 1 has b in a separate sentence.
     * The fix prevents b from leaking into doc 0's a-only sentence.
     */
    @Test
    public void testNestedContainsSameSegmentCrossDocLeakage ()
            throws IOException {
        KrillIndex ki = new KrillIndex();

        // Doc 0: S0 has b+a, S1 has a only.
        ki.addDoc(multiSentenceDoc(
                new String[] { "b", "a", "w2" },
                new String[] { "a", "w4", "w5" }));

        // Doc 1: S0 has a only, S1 has b only.
        ki.addDoc(multiSentenceDoc(
                new String[] { "a", "w1", "w2" },
                new String[] { "b", "w4", "w5" }));

        ki.commit();

        Result kr = ki.search(nestedContainsDiffClassQuery(), (short) 50);
        assertEquals("Only Doc 0 S0 has both a+b, expect 1 match",
                1, kr.getTotalResults());
        assertEquals(1, kr.getMatch(0).getStartPos());
        assertEquals(2, kr.getMatch(0).getEndPos());
        ki.close();
    }

    /**
     * Same data as above but each doc in its own segment.
     * Verifies no cross-doc leak with separate segments.
     */
    @Test
    public void testNestedContainsSeparateSegmentsNoLeakage ()
            throws IOException {
        KrillIndex ki = new KrillIndex();

        // Doc 0: S0 has b+a, S1 has a only.
        ki.addDoc(multiSentenceDoc(
                new String[] { "b", "a", "w2" },
                new String[] { "a", "w4", "w5" }));
        ki.commit();

        // Doc 1: S0 has a only, S1 has b only.
        ki.addDoc(multiSentenceDoc(
                new String[] { "a", "w1", "w2" },
                new String[] { "b", "w4", "w5" }));
        ki.commit();

        Result kr = ki.search(nestedContainsDiffClassQuery(), (short) 50);
        assertEquals("Separate segments: expect 1 match",
                1, kr.getTotalResults());
        assertEquals(1, kr.getMatch(0).getStartPos());
        assertEquals(2, kr.getMatch(0).getEndPos());
        ki.close();
    }

    /**
     * Multiple a-only docs then one doc with both a+b in same segment.
     * B from last doc must not leak into earlier docs.
     */
    @Test
    public void testNestedContainsManyAOnlyDocsThenBoth ()
            throws IOException {
        KrillIndex ki = new KrillIndex();

        // Doc 0: single sentence with a only.
        ki.addDoc(multiSentenceDoc(
                new String[] { "a", "w1", "w2" }));
        // Doc 1: single sentence with a only.
        ki.addDoc(multiSentenceDoc(
                new String[] { "w0", "a", "w2" }));
        // Doc 2: single sentence with a only.
        ki.addDoc(multiSentenceDoc(
                new String[] { "a", "w1", "w2" }));
        // Doc 3: single sentence with both a and b.
        ki.addDoc(multiSentenceDoc(
                new String[] { "a", "b", "w2" }));

        ki.commit();

        Result kr = ki.search(nestedContainsDiffClassQuery(), (short) 50);
        assertEquals("Only Doc 3 has both, expect 1 match",
                1, kr.getTotalResults());
        assertEquals(0, kr.getMatch(0).getStartPos());
        assertEquals(1, kr.getMatch(0).getEndPos());
        ki.close();
    }

    /**
     * Multi-sentence a-only doc then doc with a+b in one sentence.
     * Tests scenario from fuzz seed 125901200207375.
     */
    @Test
    public void testNestedContainsMultiSentenceAOnlyThenBoth ()
            throws IOException {
        KrillIndex ki = new KrillIndex();

        // Doc 0: two sentences, both are a only.
        ki.addDoc(multiSentenceDoc(
                new String[] { "w0", "w1", "a", "w3", "w4" },
                new String[] { "w5", "a", "w7", "w8", "w9" }));

        // Doc 1: one sentence with both a and b.
        ki.addDoc(multiSentenceDoc(
                new String[] { "a", "w1", "w2", "b", "w4" }));

        ki.commit();

        Result kr = ki.search(nestedContainsDiffClassQuery(), (short) 50);
        assertEquals("Only Doc 1 has both a+b, expect 1 match",
                1, kr.getTotalResults());
        assertEquals(0, kr.getMatch(0).getStartPos());
        assertEquals(1, kr.getMatch(0).getEndPos());
        ki.close();
    }

    // Regression: skipTo() must also work before first next().
    @Test
    public void testWithinSkipToBeforeNextInitializesSpans ()
            throws IOException {
        KrillIndex ki = new KrillIndex();

        // One local doc with a single sentence that contains token a.
        ki.addDoc(multiSentenceDoc(
                new String[] { "a", "w1", "w2" }));
        ki.commit();

        SpanQuery sq = new SpanWithinQuery(
                new SpanElementQuery("tokens", "base/s:s"),
                new SpanTermQuery(new Term("tokens", "s:a")));

        Map<Term, TermContext> termContexts = new HashMap<>();
        for (LeafReaderContext atomic : ki.reader().leaves()) {
            Bits bitset = atomic.reader().getLiveDocs();
            Spans spans = sq.getSpans(atomic, bitset, termContexts);
            assertTrue("skipTo() before next() must initialize spans",
                    spans.skipTo(0));
            assertEquals(0, spans.doc());
            assertEquals(0, spans.start());
            assertEquals(3, spans.end());
        }

        ki.close();
    }

    // ----------------------------------------------------------------------
    // Fuzzing tests for nested contains + focus
    //
    // Call with:
    // $ mvn test -Dtest="TestWithinIndex" -Dfuzz.iterations=200 -pl .
    // ----------------------------------------------------------------------

    // Read iteration count; default 0 keeps fuzz tests disabled.
    private static int getFuzzIterations () {
        String prop = System.getProperty("fuzz.iterations");
        if (prop != null) return Integer.parseInt(prop);
        return 0;
    }

    // Read optional seed; default uses current nano time.
    private static long getFuzzBaseSeed () {
        String prop = System.getProperty("fuzz.seed");
        if (prop != null) return Long.parseLong(prop);
        return System.nanoTime();
    }

    enum BiasStrategy {
        UNIFORM,
        HEAVY_A_RARE_B,
        ALTERNATING,
        CLUSTER,
        DENSE
    }

    static class SentenceInfo {
        final int startPos;
        final int endPos;
        final List<Integer> aPositions;
        final List<Integer> bPositions;

        SentenceInfo (int startPos, int endPos,
                List<Integer> aPositions, List<Integer> bPositions) {
            this.startPos = startPos;
            this.endPos = endPos;
            this.aPositions = aPositions;
            this.bPositions = bPositions;
        }

        boolean hasA () { return !aPositions.isEmpty(); }
        boolean hasB () { return !bPositions.isEmpty(); }
        boolean hasBoth () { return hasA() && hasB(); }
    }

    static class DocInfo {
        final String textSigle;
        final String genre;
        final List<SentenceInfo> sentences;

        DocInfo (String textSigle, String genre,
                List<SentenceInfo> sentences) {
            this.textSigle = textSigle;
            this.genre = genre;
            this.sentences = sentences;
        }
    }

    static class IndexConfig {
        final List<DocInfo> docs;
        final int[] commitAfterDoc;
        final long seed;
        final BiasStrategy strategy;

        IndexConfig (List<DocInfo> docs, int[] commitAfterDoc,
                long seed, BiasStrategy strategy) {
            this.docs = docs;
            this.commitAfterDoc = commitAfterDoc;
            this.seed = seed;
            this.strategy = strategy;
        }
    }

    // Generate one random fuzz corpus with a random bias strategy.
    private IndexConfig generateFuzzConfig (long seed) {
        Random rng = new Random(seed);
        BiasStrategy[] strategies = BiasStrategy.values();
        BiasStrategy strategy = strategies[rng.nextInt(strategies.length)];
        return generateFuzzConfig(seed, rng, strategy);
    }

    // Generate one random fuzz corpus using a fixed strategy.
    private IndexConfig generateFuzzConfig (long seed, Random rng,
            BiasStrategy strategy) {
        int numDocs = 1 + rng.nextInt(5);
        List<DocInfo> docs = new ArrayList<>();

        for (int d = 0; d < numDocs; d++) {
            int numSentences = 2 + rng.nextInt(14);
            List<SentenceInfo> sentences = new ArrayList<>();
            int docTokenPos = 0;

            for (int s = 0; s < numSentences; s++) {
                int sentLen = decideSentenceLength(rng, strategy);
                int sentStart = docTokenPos;
                int sentEnd = docTokenPos + sentLen;

                boolean placeA = decidePlaceA(rng, strategy, s, numSentences);
                boolean placeB = decidePlaceB(rng, strategy, s, numSentences);

                int numA = placeA ? (1 + rng.nextInt(
                        strategy == BiasStrategy.DENSE ? 3 : 2)) : 0;
                int numB = placeB ? (1 + rng.nextInt(
                        strategy == BiasStrategy.DENSE ? 3 : 2)) : 0;
                numA = Math.min(numA, sentLen / 2);
                numB = Math.min(numB, Math.max(0, sentLen - numA));

                Set<Integer> usedOffsets = new HashSet<>();
                List<Integer> aPositions = new ArrayList<>();
                List<Integer> bPositions = new ArrayList<>();

                for (int i = 0; i < numA; i++) {
                    int off = pickUnusedOffset(rng, sentLen, usedOffsets);
                    if (off >= 0) {
                        usedOffsets.add(off);
                        aPositions.add(sentStart + off);
                    }
                }
                for (int i = 0; i < numB; i++) {
                    int off = pickUnusedOffset(rng, sentLen, usedOffsets);
                    if (off >= 0) {
                        usedOffsets.add(off);
                        bPositions.add(sentStart + off);
                    }
                }

                sentences.add(new SentenceInfo(sentStart, sentEnd,
                        aPositions, bPositions));
                docTokenPos = sentEnd;
            }

            String genre = (d % 3 == 0) ? "science"
                    : (d % 3 == 1) ? "fiction" : "news";
            docs.add(new DocInfo("TST/D" + d + "/T0", genre, sentences));
        }

        int[] commitAfterDoc = decideCommitPoints(rng, numDocs);
        return new IndexConfig(docs, commitAfterDoc, seed, strategy);
    }

    // Choose sentence length for one random sentence.
    private int decideSentenceLength (Random rng, BiasStrategy strategy) {
        if (strategy == BiasStrategy.DENSE) {
            return 6 + rng.nextInt(10);
        }
        int r = rng.nextInt(100);
        if (r < 5) return 2 + rng.nextInt(2);
        if (r < 15) return 20 + rng.nextInt(11);
        return 3 + rng.nextInt(15);
    }

    // Decide if sentence gets at least one a token.
    private boolean decidePlaceA (Random rng, BiasStrategy strategy,
            int sentIdx, int numSentences) {
        switch (strategy) {
            case HEAVY_A_RARE_B:
                return rng.nextDouble() < 0.9;
            case ALTERNATING:
                return sentIdx % 2 == 0 || rng.nextDouble() < 0.15;
            case CLUSTER:
                return sentIdx < numSentences / 2 || rng.nextDouble() < 0.2;
            case DENSE:
                return true;
            default:
                return rng.nextDouble() < 0.6;
        }
    }

    // Decide if sentence gets at least one b token.
    private boolean decidePlaceB (Random rng, BiasStrategy strategy,
            int sentIdx, int numSentences) {
        switch (strategy) {
            case HEAVY_A_RARE_B:
                return rng.nextDouble() < 0.1;
            case ALTERNATING:
                return sentIdx % 2 == 1 || rng.nextDouble() < 0.15;
            case CLUSTER:
                return sentIdx >= numSentences / 2 || rng.nextDouble() < 0.2;
            case DENSE:
                return true;
            default:
                return rng.nextDouble() < 0.4;
        }
    }

    // Pick a random token offset not used yet in sentence.
    private int pickUnusedOffset (Random rng, int sentLen,
            Set<Integer> used) {
        if (used.size() >= sentLen) return -1;
        for (int attempt = 0; attempt < 20; attempt++) {
            int pos = rng.nextInt(sentLen);
            if (!used.contains(pos)) return pos;
        }
        for (int pos = 0; pos < sentLen; pos++) {
            if (!used.contains(pos)) return pos;
        }
        return -1;
    }

    // Choose where to commit so we get multiple segment layouts.
    private int[] decideCommitPoints (Random rng, int numDocs) {
        if (numDocs <= 1) return new int[] { 0 };
        List<Integer> points = new ArrayList<>();
        for (int d = 0; d < numDocs; d++) {
            if (d == numDocs - 1 || rng.nextDouble() < 0.4) {
                points.add(d);
            }
        }
        if (!points.contains(numDocs - 1)) points.add(numDocs - 1);
        int[] result = new int[points.size()];
        for (int i = 0; i < result.length; i++) result[i] = points.get(i);
        return result;
    }

    // Build a KrillIndex from generated docs and commit plan.
    private KrillIndex buildFuzzIndex (IndexConfig config)
            throws IOException {
        KrillIndex ki = new KrillIndex();
        int commitIdx = 0;

        for (int d = 0; d < config.docs.size(); d++) {
            DocInfo doc = config.docs.get(d);
            FieldDocument fd = buildFuzzFieldDocument(doc);
            ki.addDoc(fd);

            if (commitIdx < config.commitAfterDoc.length
                    && config.commitAfterDoc[commitIdx] == d) {
                ki.commit();
                commitIdx++;
            }
        }

        if (commitIdx == 0 || config.commitAfterDoc[commitIdx - 1]
                != config.docs.size() - 1) {
            ki.commit();
        }

        return ki;
    }

    // Convert one generated doc into a tokens field with TV data.
    private FieldDocument buildFuzzFieldDocument (DocInfo doc) {
        FieldDocument fd = new FieldDocument();
        fd.addString("textSigle", doc.textSigle);
        fd.addString("genre", doc.genre);

        Set<Integer> aPositions = new HashSet<>();
        Set<Integer> bPositions = new HashSet<>();
        for (SentenceInfo sent : doc.sentences) {
            aPositions.addAll(sent.aPositions);
            bPositions.addAll(sent.bPositions);
        }

        int totalTokens = 0;
        for (SentenceInfo sent : doc.sentences) {
            totalTokens += (sent.endPos - sent.startPos);
        }

        String[] allTokens = new String[totalTokens];
        int idx = 0;
        for (SentenceInfo sent : doc.sentences) {
            for (int pos = sent.startPos; pos < sent.endPos; pos++) {
                if (aPositions.contains(pos)) {
                    allTokens[idx] = "a";
                }
                else if (bPositions.contains(pos)) {
                    allTokens[idx] = "b";
                }
                else {
                    allTokens[idx] = "w" + pos;
                }
                idx++;
            }
        }

        int totalChars = 0;
        for (String t : allTokens) totalChars += t.length();

        StringBuilder surface = new StringBuilder();
        StringBuilder tv = new StringBuilder();

        int tokIdx = 0;
        int charOff = 0;

        for (int si = 0; si < doc.sentences.size(); si++) {
            SentenceInfo sent = doc.sentences.get(si);
            int sentLen = sent.endPos - sent.startPos;

            int sentCharStart = charOff;
            int sentCharEnd = sentCharStart;
            for (int ti = 0; ti < sentLen; ti++) {
                sentCharEnd += allTokens[tokIdx + ti].length();
            }

            for (int ti = 0; ti < sentLen; ti++) {
                String tok = allTokens[tokIdx + ti];
                int cStart = charOff;
                int cEnd = charOff + tok.length();
                surface.append(tok);

                tv.append("[(").append(cStart).append("-").append(cEnd)
                        .append(")");
                tv.append("s:").append(tok);

                if (tokIdx + ti == 0) {
                    tv.append("|-:t$<i>").append(totalTokens);
                    tv.append("|<>:base/s:t$<b>64<i>0<i>")
                            .append(totalChars).append("<i>")
                            .append(totalTokens).append("<b>0");
                }

                if (ti == 0) {
                    tv.append("|<>:base/s:s$<b>64<i>")
                            .append(sentCharStart).append("<i>")
                            .append(sentCharEnd).append("<i>")
                            .append(sent.endPos).append("<b>1");
                }

                tv.append("|_").append(tokIdx + ti).append("$<i>")
                        .append(cStart).append("<i>").append(cEnd);
                tv.append("]");

                charOff = cEnd;
            }
            tokIdx += sentLen;
        }

        fd.addTV("tokens", surface.toString(), tv.toString());
        return fd;
    }

    // focus(1: contains(contains(<s>, {1: a}), {1: b}))
    private SpanQuery nestedContainsSameClassQuery () {
        SpanQuery sentence = new SpanElementQuery("tokens", "base/s:s");
        SpanQuery tokenA = new SpanClassQuery(
                new SpanTermQuery(new Term("tokens", "s:a")), (byte) 1);
        SpanQuery tokenB = new SpanClassQuery(
                new SpanTermQuery(new Term("tokens", "s:b")), (byte) 1);
        SpanQuery innerContains = new SpanWithinQuery(sentence, tokenA);
        SpanQuery outerContains = new SpanWithinQuery(innerContains, tokenB);
        SpanFocusQuery focus = new SpanFocusQuery(outerContains, (byte) 1);
        focus.setSorted(false);
        return focus;
    }

    // Collect all a positions from sentences that also contain b.
    private Set<Integer> expectedAPositions (DocInfo doc) {
        Set<Integer> result = new TreeSet<>();
        for (SentenceInfo sent : doc.sentences) {
            if (sent.hasBoth()) {
                result.addAll(sent.aPositions);
            }
        }
        return result;
    }

    // Collect all a positions in one generated document.
    private Set<Integer> allAPositions (DocInfo doc) {
        Set<Integer> result = new TreeSet<>();
        for (SentenceInfo sent : doc.sentences) {
            result.addAll(sent.aPositions);
        }
        return result;
    }

    // Collect all a and b positions in one generated document.
    private Set<Integer> allABPositions (DocInfo doc) {
        Set<Integer> result = new TreeSet<>();
        for (SentenceInfo sent : doc.sentences) {
            result.addAll(sent.aPositions);
            result.addAll(sent.bPositions);
        }
        return result;
    }

    // Check invariants for focus(class1) over different class ids.
    private void verifyDiffClassInvariants (Result kr,
            IndexConfig config, String context) {

        Set<Integer> anyDocExpectedA = new TreeSet<>();
        for (DocInfo doc : config.docs) {
            anyDocExpectedA.addAll(expectedAPositions(doc));
        }

        Set<Integer> matchedPositions = new TreeSet<>();
        int storedMatches = kr.getMatches().size();

        for (int i = 0; i < storedMatches; i++) {
            Match m = kr.getMatch(i);
            int mStart = m.getStartPos();
            int mEnd = m.getEndPos();

            assertTrue(context + ": Match " + i + " [" + mStart + ","
                    + mEnd + ") must be single token (end==start+1)",
                    mEnd == mStart + 1);

            boolean validInSomeDoc = false;
            for (DocInfo doc : config.docs) {
                if (allAPositions(doc).contains(mStart)
                        && expectedAPositions(doc).contains(mStart)) {
                    validInSomeDoc = true;
                    break;
                }
            }
            assertTrue(context + ": Match " + i + " start=" + mStart
                    + " must be a valid 'a' in a sentence with 'b'",
                    validInSomeDoc);

            matchedPositions.add(mStart);
        }

        if (config.docs.size() == 1
                && kr.getTotalResults() == storedMatches) {
            Set<Integer> expected = expectedAPositions(config.docs.get(0));
            for (int pos : expected) {
                assertTrue(context + ": Expected 'a' at position "
                        + pos + " not found in results",
                        matchedPositions.contains(pos));
            }
        }
    }

    // Check invariants for focus(class1) with shared class ids.
    private void verifySameClassInvariants (Result kr,
            IndexConfig config, String context) {

        int storedMatches = kr.getMatches().size();
        for (int i = 0; i < storedMatches; i++) {
            Match m = kr.getMatch(i);
            int mStart = m.getStartPos();
            int mEnd = m.getEndPos();

            boolean validInSomeDoc = false;
            for (DocInfo doc : config.docs) {
                Set<Integer> abPos = allABPositions(doc);

                if (!abPos.contains(mStart)) continue;
                if (!abPos.contains(mEnd - 1)) continue;

                boolean withinSentence = false;
                boolean sentHasBoth = false;
                for (SentenceInfo sent : doc.sentences) {
                    if (mStart >= sent.startPos && mEnd <= sent.endPos) {
                        withinSentence = true;
                        sentHasBoth = sent.hasBoth();
                        break;
                    }
                }
                if (withinSentence && sentHasBoth) {
                    validInSomeDoc = true;
                    break;
                }
            }

            assertTrue(context + ": Match " + i + " [" + mStart + ","
                    + mEnd + ") failed same-class invariants",
                    validInSomeDoc);
        }
    }

    // Check diff-class invariants after virtual corpus filtering.
    private void verifyDiffClassInvariantsFiltered (Result kr,
            IndexConfig config, Set<String> includedSigles,
            String context) {

        int storedMatches = kr.getMatches().size();
        for (int i = 0; i < storedMatches; i++) {
            Match m = kr.getMatch(i);
            int mStart = m.getStartPos();
            int mEnd = m.getEndPos();

            assertTrue(context + ": Match " + i + " [" + mStart + ","
                    + mEnd + ") must be single token",
                    mEnd == mStart + 1);

            boolean validInFilteredDoc = false;
            for (DocInfo doc : config.docs) {
                if (!includedSigles.contains(doc.textSigle)) continue;
                if (expectedAPositions(doc).contains(mStart)) {
                    validInFilteredDoc = true;
                    break;
                }
            }
            assertTrue(context + ": Match " + i + " start=" + mStart
                    + " must be valid 'a' in filtered docs",
                    validInFilteredDoc);
        }
    }

    // Function type used by fuzzLoop for one seeded iteration.
    @FunctionalInterface
    interface FuzzAction {
        void run (long seed) throws Exception;
    }

    // Run a fuzz action over N seeds and print reproducible failures.
    private void fuzzLoop (String testName, FuzzAction action) {
        int iterations = getFuzzIterations();
        assumeTrue("Fuzz tests skipped (set -Dfuzz.iterations=N to run)",
                iterations > 0);
        long baseSeed = getFuzzBaseSeed();

        for (int i = 0; i < iterations; i++) {
            long seed = baseSeed + i;
            try {
                action.run(seed);
            }
            catch (AssertionError e) {
                throw new AssertionError(
                        "FUZZ FAILURE [" + testName + "] iteration="
                        + i + " seed=" + seed + "\n"
                        + "Reproduce: -Dfuzz.seed=" + seed
                        + " -Dfuzz.iterations=1", e);
            }
            catch (Exception e) {
                throw new AssertionError(
                        "FUZZ ERROR [" + testName + "] iteration="
                        + i + " seed=" + seed, e);
            }
        }
    }

    // Fuzz test: class1 focus with inner class1 and outer class2.
    @Test
    public void fuzzNestedContainsDiffClass () {
        fuzzLoop("fuzzNestedContainsDiffClass", seed -> {
            IndexConfig config = generateFuzzConfig(seed);
            KrillIndex ki = buildFuzzIndex(config);
            try {
                Result kr = ki.search(
                        nestedContainsDiffClassQuery(), (short) 500);
                verifyDiffClassInvariants(kr, config,
                        "seed=" + seed + " strategy=" + config.strategy);
            }
            finally {
                ki.close();
            }
        });
    }

    // Fuzz test: class1 focus with both terms in class1.
    @Test
    public void fuzzNestedContainsSameClass () {
        fuzzLoop("fuzzNestedContainsSameClass", seed -> {
            IndexConfig config = generateFuzzConfig(seed);
            KrillIndex ki = buildFuzzIndex(config);
            try {
                Result kr = ki.search(
                        nestedContainsSameClassQuery(), (short) 500);
                verifySameClassInvariants(kr, config,
                        "seed=" + seed + " strategy=" + config.strategy);
            }
            finally {
                ki.close();
            }
        });
    }

    // Fuzz test: force one commit per doc to maximize segment count.
    @Test
    public void fuzzNestedContainsMultiSegment () {
        fuzzLoop("fuzzNestedContainsMultiSegment", seed -> {
            Random rng = new Random(seed);
            BiasStrategy strategy = BiasStrategy.values()[
                    rng.nextInt(BiasStrategy.values().length)];
            IndexConfig config = generateFuzzConfig(seed, rng, strategy);

            int numDocs = config.docs.size();
            int[] manyCommits = new int[numDocs];
            for (int d = 0; d < numDocs; d++) manyCommits[d] = d;
            IndexConfig multiSegConfig = new IndexConfig(
                    config.docs, manyCommits, seed, strategy);

            KrillIndex ki = buildFuzzIndex(multiSegConfig);
            try {
                Result kr = ki.search(
                        nestedContainsDiffClassQuery(), (short) 500);
                verifyDiffClassInvariants(kr, multiSegConfig,
                        "seed=" + seed + " strategy=" + strategy);
            }
            finally {
                ki.close();
            }
        });
    }

    // Fuzz test: run diff-class query on a filtered virtual corpus.
    @Test
    public void fuzzNestedContainsVirtualCorpus () {
        fuzzLoop("fuzzNestedContainsVirtualCorpus", seed -> {
            IndexConfig config = generateFuzzConfig(seed);
            if (config.docs.size() < 2) return;

            KrillIndex ki = buildFuzzIndex(config);
            try {
                String targetSigle = config.docs.get(0).textSigle;
                Set<String> included = new HashSet<>();
                included.add(targetSigle);

                Krill ks = new Krill(nestedContainsDiffClassQuery());
                ks.getMeta().setCount((short) 500);

                CollectionBuilder cb = new CollectionBuilder();
                KrillCollection kc = new KrillCollection(ki);
                kc.fromBuilder(cb.term("textSigle", targetSigle));
                ks.setCollection(kc);

                Result kr = ks.apply(ki);
                verifyDiffClassInvariantsFiltered(kr, config, included,
                        "seed=" + seed + " vc_sigle=" + targetSigle);
            }
            finally {
                ki.close();
            }
        });
    }

    // Fuzz test: heavy backtracking setup with many a and rare b.
    @Test
    public void fuzzNestedContainsHeavyBacktracking () {
        fuzzLoop("fuzzNestedContainsHeavyBacktracking", seed -> {
            Random rng = new Random(seed);
            IndexConfig config = generateFuzzConfig(seed, rng,
                    BiasStrategy.HEAVY_A_RARE_B);
            KrillIndex ki = buildFuzzIndex(config);
            try {
                Result kr = ki.search(
                        nestedContainsDiffClassQuery(), (short) 500);
                verifyDiffClassInvariants(kr, config,
                        "seed=" + seed + " strategy=HEAVY_A_RARE_B");
            }
            finally {
                ki.close();
            }
        });
    }

    // Fuzz test: dense setup where almost every sentence has a and b.
    @Test
    public void fuzzNestedContainsDense () {
        fuzzLoop("fuzzNestedContainsDense", seed -> {
            Random rng = new Random(seed);
            IndexConfig config = generateFuzzConfig(seed, rng,
                    BiasStrategy.DENSE);
            KrillIndex ki = buildFuzzIndex(config);
            try {
                Result kr = ki.search(
                        nestedContainsDiffClassQuery(), (short) 500);
                verifyDiffClassInvariants(kr, config,
                        "seed=" + seed + " strategy=DENSE");
            }
            finally {
                ki.close();
            }
        });
    }
};
