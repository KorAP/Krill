package de.ids_mannheim.korap.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.*;
import java.net.URLDecoder;
import static de.ids_mannheim.korap.TestSimple.*;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.KrillQuery;
import de.ids_mannheim.korap.query.QueryBuilder;
import de.ids_mannheim.korap.query.SpanClassQuery;
import de.ids_mannheim.korap.query.SpanElementQuery;
import de.ids_mannheim.korap.query.wrap.SpanQueryWrapper;
import de.ids_mannheim.korap.query.SpanNextQuery;
import de.ids_mannheim.korap.query.SpanWithinQuery;
import de.ids_mannheim.korap.query.wrap.SpanQueryWrapper;
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
    @Ignore("TODO(kwic-cap): adapt to new HTML KWIC alignment")
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
};
