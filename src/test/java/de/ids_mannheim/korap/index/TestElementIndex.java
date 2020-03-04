package de.ids_mannheim.korap.index;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.query.SpanElementQuery;
import de.ids_mannheim.korap.response.Result;


@RunWith(JUnit4.class)
public class TestElementIndex {

    // Todo: primary data as a non-indexed field separated.

    @Test
    public void indexExample1 () throws IOException {
        KrillIndex ki = new KrillIndex();

        // <a>x<a>y<a>zhij</a>hij</a>hij</a>hij</a>
        FieldDocument fd = new FieldDocument();
        fd.addTV("base", "x  y  z  h  i  j  h  i  j  h  i  j  ",
                "[(0-3)s:x|<>:a$<b>64<i>0<i>3<i>12<b>0||<>:b$<b>64<i>0<i>3<i>1<b>0]"
                        + "[(3-6)s:y|<>:a$<b>64<i>3<i>6<i>9<b>0]"
                        + "[(6-9)s:z|<>:a$<b>64<i>6<i>9<i>6]"
                        + "[(9-12)s:h<b>0]" + "[(12-15)s:i]" + "[(15-18)s:j]"
                        + "[(18-21)s:h]" + "[(21-24)s:i]" + "[(24-27)s:j]"
                        + "[(27-30)s:h]" + "[(30-33)s:i]" + "[(33-36)s:j]");
        ki.addDoc(fd);

        // <a>x<a>y<a>zcde</a>cde</a>cde</a>cde</a>
        fd = new FieldDocument();
        fd.addTV("base", "x  y  z  c  d  e  c  d  e  c  d  e  ",
                "[(0-3)s:x|<>:a$<b>64<i>0<i>3<i>12<b>0]"
                        + "[(3-6)s:y|<>:a$<b>64<i>3<i>6<i>9<b>0]"
                        + "[(6-9)s:z|<>:a$<b>64<i>6<i>9<i>6]"
                        + "[(9-12)s:c<b>0]" + "[(12-15)s:d]" + "[(15-18)s:e]"
                        + "[(18-21)s:c]" + "[(21-24)s:d]" + "[(24-27)s:e]"
                        + "[(27-30)s:c]" + "[(30-33)s:d]" + "[(33-36)s:e]");
        ki.addDoc(fd);

        // Save documents
        ki.commit();

        assertEquals(2, ki.numberOf("documents"));

        SpanQuery sq = new SpanElementQuery("base", "a");

        Result kr = ki.search(sq, (short) 10);

        assertEquals("totalResults", kr.getTotalResults(), 6);

        assertEquals("StartPos (0)", 0, kr.getMatch(0).startPos);
        assertEquals("EndPos (0)", 12, kr.getMatch(0).endPos);
        assertEquals("StartPos (1)", 1, kr.getMatch(1).startPos);
        assertEquals("EndPos (1)", 9, kr.getMatch(1).endPos);
        assertEquals("StartPos (2)", 2, kr.getMatch(2).startPos);
        assertEquals("EndPos (2)", 6, kr.getMatch(2).endPos);

        assertEquals("StartPos (0)", 0, kr.getMatch(3).startPos);
        assertEquals("EndPos (0)", 12, kr.getMatch(3).endPos);
        assertEquals("StartPos (1)", 1, kr.getMatch(4).startPos);
        assertEquals("EndPos (1)", 9, kr.getMatch(4).endPos);
        assertEquals("StartPos (2)", 2, kr.getMatch(5).startPos);
        assertEquals("EndPos (2)", 6, kr.getMatch(5).endPos);

        sq = new SpanTermQuery(new Term("base", "s:x"));

        kr = ki.search(sq, (short) 10);

        assertEquals("totalResults", kr.getTotalResults(), 2);
        
        assertEquals("StartPos (0)", 0, kr.getMatch(0).startPos);
        assertEquals("EndPos (0)", 1, kr.getMatch(0).endPos);

        sq = new SpanElementQuery("base", "b");

        kr = ki.search(sq, (short) 10);

        assertEquals("totalResults", kr.getTotalResults(), 1);

        assertEquals("StartPos (0)", 0, kr.getMatch(0).startPos);
        assertEquals("EndPos (0)", 1, kr.getMatch(0).endPos);
    };


    @Test
    public void indexExample2 () throws IOException {
        KrillIndex ki = new KrillIndex();

        // <a><a><a>h</a>hhij</a>hij</a>hij</a>
        FieldDocument fd = new FieldDocument();
        fd.addTV("base", "h  h        i  j   h  i  j  h  i  j  ",
                "[(0-3)s:h|" + "<>:a$<b>64<i>0<i>18<i>3<b>0|"
                        + "<>:a$<b>64<i>0<i>27<i>6<b>0|"
                        + "<>:a$<b>64<i>0<i>36<i>9]" + "[(3-6)s:h]"
                        + "[(12-15)s:i<b>0]" + "[(15-18)s:j]" + "[(18-21)s:h]"
                        + "[(21-24)s:i]" + "[(24-27)s:j]" + "[(27-30)s:h]"
                        + "[(30-33)s:i]" + "[(33-36)s:j]");
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
    };


    @Test
    public void indexExample3 () throws IOException {
        KrillIndex ki = new KrillIndex();

        // <a><a><a>u</a></a></a>
        FieldDocument fd = new FieldDocument();
        fd.addTV("base", "xyz", "[(0-3)s:xyz|<>:a$<b>64<i>0<i>3<i>0<b>0|"
                + "<>:a$<b>64<i>0<i>3<i>0<b>0|"
                + "<>:a$<b>64<i>0<i>3<i>0<b>0|<>:b$<b>64<i>0<i>3<i>0<b>0]");
        ki.addDoc(fd);

        // <a><b>x<a>y<a>zcde</a>cde</a>cde</b></a>
        fd = new FieldDocument();
        fd.addTV("base", "x  y  z  c  d  e  c  d  e  c  d  e  ",
                "[(0-3)s:x|<>:a$<b>64<i>0<i>36<i>12<b>0|<>:b$<b>64<i>0<i>36<i>12<b>0]"
                        + "[(3-6)s:y|<>:a$<b>64<i>3<i>27<i>9<b>0]"
                        + "[(6-9)s:z|<>:a$<b>64<i>6<i>18<i>6]"
                        + "[(9-12)s:c<b>0]" + "[(12-15)s:d]" + "[(15-18)s:e]"
                        + "[(18-21)s:c]" + "[(21-24)s:d]" + "[(24-27)s:e]"
                        + "[(27-30)s:c]" + "[(30-33)s:d]" + "[(33-36)s:e]");
        ki.addDoc(fd);

        // xyz
        fd = new FieldDocument();
        fd.addTV("base", "x  y  z  ",
                "[(0-3)s:x]" + "[(3-6)s:y]" + "[(6-9)s:z]");
        ki.addDoc(fd);

        // <a>x<a><b>y<a>zcde</a>cde</b></a>cde</a>
        fd = new FieldDocument();
        fd.addTV("base", "x  y  z  k  l  m  k  l  m  k  l  m  ",
                "[(0-3)s:x|<>:a$<b>64<i>0<i>3<i>12<b>0]"
                        + "[(3-6)s:y|<>:a$<b>64<i>3<i>6<i>9<b>0|<>:b$<b>64<i>3<i>6<i>9<b>0]"
                        + "[(6-9)s:z|<>:a$<b>64<i>6<i>9<i>6<b>0]"
                        + "[(9-12)s:k<b>0]" + "[(12-15)s:l]" + "[(15-18)s:m]"
                        + "[(18-21)s:k]" + "[(21-24)s:l]" + "[(24-27)s:m]"
                        + "[(27-30)s:k]" + "[(30-33)s:l]" + "[(33-36)s:m]");
        ki.addDoc(fd);

        // <a><a><a>h</a>hhij</a>hij</a>hij</a>
        fd = new FieldDocument();
        fd.addTV("base", "h  h        i  j  h  i  j  h  i  j  ",
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

        // System.err.println(kr.toJSON());

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
    public void indexExample4 () throws IOException {
        KrillIndex ki = new KrillIndex();

        FieldDocument fd = new FieldDocument();
        fd.addTV("base", "111111ccc222222fff333333iiijjj",
                "[(0-3)s:a|_0$<i>0<i>3]" + "[(3-6)s:b|_1$<i>3<i>6]"
                        + "[(6-9)s:c|_2$<i>6<i>9]"
                        + "[(9-12)s:d|_3$<i>9<i>12|<>:a$<b>64<i>9<i>15<i>4<b>0]"
                        + "[(12-15)s:e|_4$<i>12<i>15]"
                        + "[(15-18)s:f|_5$<i>15<i>18]"
                        + "[(18-21)s:g|_6$<i>18<i>21|<>:a$<b>64<i>18<i>24<i>8<b>0]"
                        + "[(21-24)s:h|_7$<i>21<i>24]"
                        + "[(24-27)s:i|_8$<i>24<i>27]"
                        + "[(27-30)s:j|_9$<i>27<i>30]");
        ki.addDoc(fd);

        // Save documents
        ki.commit();

        assertEquals(1, ki.numberOf("documents"));

        SpanQuery sq = new SpanElementQuery("base", "a");

        Result kr = ki.search(sq, 0, (short) 15, false, (short) 3, false,
                (short) 3);

        assertEquals("... ccc[[222222]]fff ...",
                kr.getMatch(0).getSnippetBrackets());
        assertEquals("... fff[[333333]]iii ...",
                kr.getMatch(1).getSnippetBrackets());
    };


    @Test
    public void indexExample5 () throws IOException {
        KrillIndex ki = new KrillIndex();

        FieldDocument fd = new FieldDocument();
        fd.addTV("base", "111111ccc222222fff333333iiijjj",
                "[(0-3)s:a|_0$<i>0<i>3|<>:a$<b>64<i>0<i>6<i>1<b>0]"
                        + "[(3-6)s:b|_1$<i>3<i>6]" + "[(6-9)s:c|_2$<i>6<i>9]"
                        + "[(9-12)s:d|_3$<i>9<i>12|<>:a$<b>64<i>9<i>15<i>4<b>0]"
                        + "[(12-15)s:e|_4$<i>12<i>15]"
                        + "[(15-18)s:f|_5$<i>15<i>18]"
                        + "[(18-21)s:g|_6$<i>18<i>21|<>:a$<b>64<i>18<i>24<i>8<b>0]"
                        + "[(21-24)s:h|_7$<i>21<i>24]"
                        + "[(24-27)s:i|_8$<i>24<i>27]"
                        + "[(27-30)s:j|_9$<i>27<i>30]");
        ki.addDoc(fd);

        // Save documents
        ki.commit();

        assertEquals(1, ki.numberOf("documents"));

        SpanQuery sq = new SpanElementQuery("base", "a");

        Result kr = ki.search(sq, 0, (short) 15, false, (short) 3, false,
                (short) 3);

        assertEquals("[[111111]]ccc ...", kr.getMatch(0).getSnippetBrackets());
        assertEquals("... ccc[[222222]]fff ...",
                kr.getMatch(1).getSnippetBrackets());
        assertEquals("... fff[[333333]]iii ...",
                kr.getMatch(2).getSnippetBrackets());
    };


    @Test
    public void indexExample6 () throws IOException {

        KrillIndex ki = new KrillIndex();

        // <a>x<a>y<a>zhij</a>hij</a>hij</a>
        FieldDocument fd = new FieldDocument();
        fd.addTV("base", "x  y  z  h  i  j  h  i  j  h  i  j  ",
                "[(0-3)s:x|_0$<i>0<i>3|<>:a$<b>64<i>0<i>36<i>12<b>0]" + // 1
                        "[(3-6)s:y|_1$<i>3<i>6|<>:a$<b>64<i>3<i>27<i>9<b>0]" + // 2
                        "[(6-9)s:z|_2$<i>6<i>9|<>:a$<b>64<i>6<i>18<i>6<b>0]" + // 3
                        "[(9-12)s:h|_3$<i>9<i>12]" + // 4
                        "[(12-15)s:i|_4$<i>12<i>15]" + // 5
                        "[(15-18)s:j|_5$<i>15<i>18]" + // 6
                        "[(18-21)s:h|_6$<i>18<i>21]" + // 7
                        "[(21-24)s:i|_7$<i>21<i>24]" + // 8
                        "[(24-27)s:j|_8$<i>24<i>27]" + // 9
                        "[(27-30)s:h|_9$<i>27<i>30]" + // 10
                        "[(30-33)s:i|_10$<i>30<i>33]" + // 11
                        "[(33-36)s:j|_11$<i>33<i>36]"); // 12
        ki.addDoc(fd);

        fd = new FieldDocument();
        fd.addTV("base", "x  y  z  h  ",
                "[(0-3)s:x|_0$<i>0<i>3]" + // 1
                        "[(3-6)s:y|_1$<i>3<i>6]" + // 2
                        "[(6-9)s:z|_2$<i>6<i>9]" + // 3
                        "[(9-12)s:h|_3$<i>9<i>12]"); // 4
        ki.addDoc(fd);

        // Here is a larger offset than expected
        fd = new FieldDocument();
        fd.addTV("base", "x  y  z  h  ",
                "[(0-3)s:x|_0$<i>0<i>3|<>:a$<b>64<i>0<i>36<i>12<b>0]" + // 1
                        "[(3-6)s:y|_1$<i>3<i>6]" + // 2
                        "[(6-9)s:z|_2$<i>6<i>9]" + // 3
                        "[(9-12)s:h|_3$<i>9<i>12]"); // 4
        ki.addDoc(fd);

        // <a>x<a>y<a>zabc</a>abc</a>abc</a>
        fd = new FieldDocument();
        fd.addTV("base", "x  y  z  a  b  c  a  b  c  a  b  c  ",
                "[(0-3)s:x|_0$<i>0<i>3|<>:a$<b>64<i>0<i>36<i>12<b>0]" + // 1
                        "[(3-6)s:y|_1$<i>3<i>6|<>:a$<b>64<i>3<i>27<i>9<b>0]" + // 2
                        "[(6-9)s:z|_2$<i>6<i>9|<>:a$<b>64<i>6<i>18<i>6<b>0]" + // 3
                        "[(9-12)s:a|_3$<i>9<i>12]" + // 4
                        "[(12-15)s:b|_4$<i>12<i>15]" + // 5
                        "[(15-18)s:c|_5$<i>15<i>18]" + // 6
                        "[(18-21)s:a|_6$<i>18<i>21]" + // 7
                        "[(21-24)s:b|_7$<i>21<i>24]" + // 8
                        "[(24-27)s:c|_8$<i>24<i>27]" + // 9
                        "[(27-30)s:a|_9$<i>27<i>30]" + // 10
                        "[(30-33)s:b|_10$<i>30<i>33]" + // 11
                        "[(33-36)s:c|_11$<i>33<i>36]"); // 12
        ki.addDoc(fd);

        fd = new FieldDocument();
        fd.addTV("base", "x  y  z  h  ",
                "[(0-3)s:x|_0$<i>0<i>3]" + // 1
                        "[(3-6)s:y|_1$<i>3<i>6]" + // 2
                        "[(6-9)s:z|_2$<i>6<i>9]" + // 3
                        "[(9-12)s:h|_3$<i>9<i>12]"); // 4
        ki.addDoc(fd);

        // Save documents
        ki.commit();

        SpanQuery sq;
        Result kr;

        sq = new SpanElementQuery("base", "a");
        kr = ki.search(sq, (short) 15);

        // System.err.println(kr.toJSON());

        assertEquals(5, ki.numberOf("documents"));
        assertEquals("totalResults", kr.getTotalResults(), 7);
    };
};
