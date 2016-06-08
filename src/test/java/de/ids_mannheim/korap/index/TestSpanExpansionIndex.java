package de.ids_mannheim.korap.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.RegexpQuery;
import org.apache.lucene.search.spans.SpanMultiTermQueryWrapper;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.util.automaton.RegExp;
import org.junit.Test;

import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.KrillQuery;
import de.ids_mannheim.korap.query.QueryBuilder;
import de.ids_mannheim.korap.query.SpanElementQuery;
import de.ids_mannheim.korap.query.SpanExpansionQuery;
import de.ids_mannheim.korap.query.SpanRepetitionQuery;
import de.ids_mannheim.korap.query.wrap.SpanQueryWrapper;
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.response.Match;
import de.ids_mannheim.korap.util.QueryException;

public class TestSpanExpansionIndex {

    Result kr;
    KrillIndex ki;


    public TestSpanExpansionIndex () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(getClass().getResourceAsStream("/wiki/00001.json.gz"), true);
        ki.commit();
    }


    /**
     * Left and right expansions
     * */
    @Test
    public void testCase1 () throws IOException {

        SpanTermQuery stq = new SpanTermQuery(new Term("tokens", "s:des"));
        // left
        SpanExpansionQuery seq = new SpanExpansionQuery(stq, 0, 2, -1, true);
        kr = ki.search(seq, (short) 10);

        //assertEquals(69,kr.getTotalResults());
        assertEquals(5, kr.getMatch(0).getStartPos());
        assertEquals(8, kr.getMatch(0).getEndPos());
        assertEquals(6, kr.getMatch(1).getStartPos());
        assertEquals(8, kr.getMatch(1).getEndPos());
        assertEquals(7, kr.getMatch(2).getStartPos());
        assertEquals(8, kr.getMatch(2).getEndPos());

        /*
         * for (Match km : kr.getMatches()) {
         * System.out.println(km.getStartPos() + "," + km.getEndPos() + " " +
         * km.getSnippetBrackets()); }
         */

        // right
        seq = new SpanExpansionQuery(stq, 3, 4, 0, true);
        kr = ki.search(seq, (short) 10);

        assertEquals(7, kr.getMatch(0).getStartPos());
        assertEquals(11, kr.getMatch(0).getEndPos());
        assertEquals(7, kr.getMatch(1).getStartPos());
        assertEquals(12, kr.getMatch(1).getEndPos());
        assertEquals(156, kr.getMatch(2).getStartPos());
        assertEquals(160, kr.getMatch(2).getEndPos());
        assertEquals(156, kr.getMatch(3).getStartPos());
        assertEquals(161, kr.getMatch(3).getEndPos());
    }


    /**
     * Classnumber
     * Check the expansion offsets
     * */
    @Test
    public void testCase2 () {
        byte classNumber = 1;
        SpanExpansionQuery sq;
        // create new payload for the expansion offsets
        SpanTermQuery stq = new SpanTermQuery(new Term("tokens", "s:des"));
        sq = new SpanExpansionQuery(stq, 0, 2, -1, classNumber, true);
        kr = ki.search(sq, (short) 10);

        assertEquals(5, kr.getMatch(0).getStartPos());
        assertEquals(8, kr.getMatch(0).getEndPos());
        assertEquals(5, kr.getMatch(0).getStartPos(1)); // expansion 5,7
        assertEquals(7, kr.getMatch(0).getEndPos(1));
        // expansion offsets
        assertEquals(6, kr.getMatch(1).getStartPos(1));
        assertEquals(7, kr.getMatch(1).getEndPos(1));
        assertEquals(7, kr.getMatch(2).getStartPos(1));
        assertEquals(7, kr.getMatch(2).getEndPos(1));
        assertEquals(154, kr.getMatch(3).getStartPos(1));
        assertEquals(156, kr.getMatch(3).getEndPos(1));

        /*
         * for (Match km : kr.getMatches()){
         * System.out.println(km.getStartPos() +","+km.getEndPos()+" "
         * +km.getSnippetBrackets()); }
         */

        // add expansion offsets to the existing payload
        SpanElementQuery seq = new SpanElementQuery("tokens", "base/s:s");
        sq = new SpanExpansionQuery(seq, 1, 2, 0, classNumber, true);
        kr = ki.search(sq, (short) 10);

        assertEquals(13, kr.getMatch(0).getStartPos());
        assertEquals(26, kr.getMatch(0).getEndPos());
        assertEquals(13, kr.getMatch(1).getStartPos());
        assertEquals(27, kr.getMatch(1).getEndPos());

        assertEquals(25, kr.getMatch(2).getStartPos());
        assertEquals(35, kr.getMatch(2).getEndPos());
        assertEquals(34, kr.getMatch(2).getStartPos(1));
        assertEquals(35, kr.getMatch(2).getEndPos(1));

        assertEquals(25, kr.getMatch(3).getStartPos());
        assertEquals(36, kr.getMatch(3).getEndPos());
        assertEquals(34, kr.getMatch(3).getStartPos(1));
        assertEquals(36, kr.getMatch(3).getEndPos(1));

        /*
         * for (Match km : kr.getMatches()){
         * System.out.println(km.getStartPos() +","+km.getEndPos()+" "
         * +km.getSnippetBrackets()); }
         */
    }


    /**
     * Right expansion with exclusion
     * */
    @Test
    public void testCase3 () throws IOException {
        byte classNumber = 1;
        SpanTermQuery stq = new SpanTermQuery(new Term("tokens", "tt/p:NN"));
        SpanTermQuery notQuery = new SpanTermQuery(new Term("tokens",
                "s:Buchstabe"));

        SpanExpansionQuery seq = new SpanExpansionQuery(stq, notQuery, 2, 3, 0,
                classNumber, true);
        kr = ki.search(seq, (short) 20);

        assertEquals(6, kr.getMatch(0).getStartPos());
        assertEquals(9, kr.getMatch(0).getEndPos());
        assertEquals(7, kr.getMatch(0).getStartPos(1));
        assertEquals(9, kr.getMatch(0).getEndPos(1));

        assertEquals(9, kr.getMatch(2).getStartPos());
        assertEquals(12, kr.getMatch(2).getEndPos());

        assertEquals(9, kr.getMatch(3).getStartPos());
        assertEquals(13, kr.getMatch(3).getEndPos());
        assertEquals(10, kr.getMatch(3).getStartPos(1));
        assertEquals(13, kr.getMatch(3).getEndPos(1));

        /*
         * for (Match km : kr.getMatches()){
         * System.out.println(km.getStartPos() +","+km.getEndPos()+" "
         * +km.getSnippetBrackets()); }
         */
    }


    /**
     * Left expansion with exclusion
     * No expansion
     * */
    @Test
    public void testCase4 () throws IOException {
        byte classNumber = 1;
        SpanTermQuery stq = new SpanTermQuery(new Term("tokens", "tt/p:NN"));
        SpanTermQuery notQuery = new SpanTermQuery(new Term("tokens",
                "tt/p:ADJA"));

        SpanExpansionQuery seq = new SpanExpansionQuery(stq, notQuery, 0, 2,
                -1, classNumber, true);
        kr = ki.search(seq, (short) 10);

        assertEquals(6, kr.getMatch(0).getStartPos());
        assertEquals(7, kr.getMatch(0).getEndPos());
        assertEquals(6, kr.getMatch(0).getStartPos(1));
        assertEquals(6, kr.getMatch(0).getEndPos(1));

        assertEquals(12, kr.getMatch(4).getStartPos());
        assertEquals(13, kr.getMatch(4).getEndPos());

        assertEquals(12, kr.getMatch(5).getStartPos());
        assertEquals(15, kr.getMatch(5).getEndPos());
        assertEquals(12, kr.getMatch(5).getStartPos(1));
        assertEquals(14, kr.getMatch(5).getEndPos(1));

        assertEquals(13, kr.getMatch(6).getStartPos());
        assertEquals(15, kr.getMatch(6).getEndPos());
        assertEquals(13, kr.getMatch(6).getStartPos(1));
        assertEquals(14, kr.getMatch(6).getEndPos(1));

        /*
         * for (Match km : kr.getMatches()){
         * System.out.println(km.getStartPos() +","+km.getEndPos()+" "
         * +km.getSnippetBrackets()); }
         */

    }


    /**
     * Expansion over start and end documents start => cut to 0
     * TODO: end => to be handled in rendering process
     * 
     * @throws IOException
     * */
    @Test
    public void testCase5 () throws IOException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(createFieldDoc0());
        ki.commit();

        SpanTermQuery stq = new SpanTermQuery(new Term("base", "s:e"));
        // left expansion precedes 0
        SpanExpansionQuery seq = new SpanExpansionQuery(stq, 2, 2, -1, true);
        kr = ki.search(seq, (short) 10);

        assertEquals((long) 4, kr.getTotalResults());
        assertEquals(0, kr.getMatch(0).getStartPos());
        assertEquals(2, kr.getMatch(0).getEndPos());

        //right expansion exceeds end position
        seq = new SpanExpansionQuery(stq, 3, 3, 0, true);
        kr = ki.search(seq, (short) 10);

        assertEquals((long) 4, kr.getTotalResults());
        assertEquals(7, kr.getMatch(2).getStartPos());
        assertEquals(11, kr.getMatch(2).getEndPos());
        assertEquals(8, kr.getMatch(3).getStartPos());
        assertEquals(12, kr.getMatch(3).getEndPos());

        /*
         * for (Match km : kr.getMatches()){
         * System.out.println(km.getStartPos() +","+km.getEndPos()+" "
         * //+km.getSnippetBrackets() ); }
         */
    }


    /**
     * Expansion exclusion : multiple documents
     * 
     * @throws IOException
     * */
    @Test
    public void testCase6 () throws IOException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(createFieldDoc0()); // same doc
        ki.addDoc(createFieldDoc1()); // only not clause
        ki.addDoc(createFieldDoc2()); // only main clause
        ki.commit();

        SpanTermQuery stq = new SpanTermQuery(new Term("base", "s:e"));
        SpanTermQuery notQuery = new SpanTermQuery(new Term("base", "s:d"));

        SpanExpansionQuery seq = new SpanExpansionQuery(stq, notQuery, 2, 3, 0,
                true);
        kr = ki.search(seq, (short) 20);

        // notClause.doc() > firstSpans.doc()
        assertEquals(7, kr.getMatch(0).getStartPos());
        assertEquals(10, kr.getMatch(0).getEndPos());
        assertEquals(7, kr.getMatch(1).getStartPos());
        assertEquals(11, kr.getMatch(1).getEndPos());
        // !hasMoreNotClause
        assertEquals(2, kr.getMatch(4).getLocalDocID());
        assertEquals(1, kr.getMatch(4).getStartPos());
        assertEquals(4, kr.getMatch(4).getEndPos());
    }


    /**
     * Skip to
     * */
    @Test
    public void testCase7 () throws IOException, QueryException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(getClass().getResourceAsStream("/wiki/00001.json.gz"), true);
        ki.addDoc(getClass().getResourceAsStream("/wiki/00002.json.gz"), true);
        ki.commit();

        String jsonPath = getClass().getResource("/queries/poly3.json")
                .getFile();
        String jsonQuery = readFile(jsonPath);
        SpanQueryWrapper sqwi = new KrillQuery("tokens").fromKoral(jsonQuery);

        SpanQuery sq = sqwi.toQuery();
        //System.out.println(sq.toString());
        kr = ki.search(sq, (short) 20);

        assertEquals(205, kr.getMatch(0).getStartPos());
        assertEquals(208, kr.getMatch(0).getEndPos());

        /*
         * for (Match km : kr.getMatches()){
         * System.out.println(km.getStartPos() +","+km.getEndPos()+" "
         * +km.getSnippetBrackets() ); }
         */
    }


    /**
     * Query rewrite bug
     * 
     * Warning: This is not armoured by <base/s=t>!
     * 
     * @throws IOException
     * */
    @Test
    public void testQueryRewriteBug () throws IOException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(createFieldDoc0()); // ceccecdeec
        /*
        ki.addDoc(createFieldDoc1()); // bbccdd || only not clause
        ki.addDoc(createFieldDoc2()); // beccea | only main clause
        */
        ki.commit();

        // See /queries/bugs/repetition_group_rewrite
        RegexpQuery requery = new RegexpQuery(new Term("base", "s:[ac]"),
                RegExp.ALL);
        SpanMultiTermQueryWrapper<RegexpQuery> query = new SpanMultiTermQueryWrapper<RegexpQuery>(
                requery);
        SpanExpansionQuery seq = new SpanExpansionQuery(query, 1, 1, 1, true);
        SpanRepetitionQuery rep = new SpanRepetitionQuery(seq, 2, 2, true);

        // spanRepetition(
        //   spanExpansion(
        //     SpanMultiTermQueryWrapper(base:/s:[ac]/),
        //     []{1, 1},
        //     right
        //   ){2,2}
        // )

        kr = ki.search(query, (short) 20);
        assertEquals(5, kr.getTotalResults());
        assertEquals(0, kr.getMatch(0).getStartPos());
        assertEquals(1, kr.getMatch(0).getEndPos());
        assertEquals(2, kr.getMatch(1).getStartPos());
        assertEquals(3, kr.getMatch(1).getEndPos());
        assertEquals(3, kr.getMatch(2).getStartPos());
        assertEquals(4, kr.getMatch(2).getEndPos());
        assertEquals(5, kr.getMatch(3).getStartPos());
        assertEquals(6, kr.getMatch(3).getEndPos());
        assertEquals(9, kr.getMatch(4).getStartPos());
        assertEquals(10, kr.getMatch(4).getEndPos());

        kr = ki.search(seq, (short) 20);
        assertEquals(5, kr.getTotalResults());
        assertEquals(0, kr.getMatch(0).getStartPos());
        assertEquals(2, kr.getMatch(0).getEndPos());
        assertEquals(2, kr.getMatch(1).getStartPos());
        assertEquals(4, kr.getMatch(1).getEndPos());
        assertEquals(3, kr.getMatch(2).getStartPos());
        assertEquals(5, kr.getMatch(2).getEndPos());
        assertEquals(5, kr.getMatch(3).getStartPos());
        assertEquals(7, kr.getMatch(3).getEndPos());
        assertEquals(9, kr.getMatch(4).getStartPos());
        assertEquals(11, kr.getMatch(4).getEndPos());

        kr = ki.search(rep, (short) 20);

        // for (Match km : kr.getMatches()){
        // System.out.println(
        // km.getStartPos() +
        // "," +
        // km.getEndPos() +
        // " " +
        // km.getSnippetBrackets()
        // );
        // };

        assertEquals("[cecc]ecdeec", kr.getMatch(0).getSnippetBrackets());
        assertEquals("cec[cecd]eec", kr.getMatch(1).getSnippetBrackets());
        assertEquals((long) 2, kr.getTotalResults());
    }



    /**
     * Query rewrite bug
     * 
     * @throws IOException
     * */
    @Test
    public void testExpansionQueryBug3 () throws IOException, QueryException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(createFieldDoc3());
        ki.addDoc(createFieldDoc4());
        ki.commit();

        String json = readFile(getClass().getResource(
                "/queries/bugs/expansion_bug_3.jsonld").getFile());
        KrillQuery kq = new KrillQuery("base");
        SpanQuery sq = kq.fromKoral(json).toQuery();
        assertEquals(
                sq.toString(),
                "focus(254: spanContain(<base:base/s:t />, {254: spanExpansion(base:s:c, []{0, 4}, right)}))");

        kr = ki.search(sq, (short) 10);
        assertEquals("[c]ab", kr.getMatch(0).getSnippetBrackets());
        assertEquals("[ca]b", kr.getMatch(1).getSnippetBrackets());
        assertEquals("[cab]", kr.getMatch(2).getSnippetBrackets());
        assertEquals("[c]e", kr.getMatch(3).getSnippetBrackets());

        assertEquals("[ce]", kr.getMatch(4).getSnippetBrackets());
        assertEquals(5, kr.getTotalResults());

        sq = kq.builder().tag("base/s:t").toQuery();
        assertEquals(sq.toString(), "<base:base/s:t />");
        kr = ki.search(sq, (short) 5);
        assertEquals("[cab]", kr.getMatch(0).getSnippetBrackets());
        assertEquals("[ce]", kr.getMatch(1).getSnippetBrackets());
        assertEquals(2, kr.getTotalResults());
    }



    private FieldDocument createFieldDoc0 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-0");
        fd.addTV("base", "ceccecdeec",
                "[(0-1)s:c|_0$<i>0<i>1|<>:base/s:t$<b>64<i>0<i>10<i>10<b>0]"
                        + "[(1-2)s:e|_1$<i>1<i>2]" + "[(2-3)s:c|_2$<i>2<i>3]"
                        + "[(3-4)s:c|s:d|_3$<i>3<i>4]"
                        + "[(4-5)s:e|_4$<i>4<i>5]" + "[(5-6)s:c|_5$<i>5<i>6]"
                        + "[(6-7)s:d|_6$<i>6<i>7]" + "[(7-8)s:e|_7$<i>7<i>8]"
                        + "[(8-9)s:e|_8$<i>8<i>9]" + "[(9-10)s:c|_9$<i>9<i>10]");
        return fd;
    }


    private FieldDocument createFieldDoc1 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-1");
        fd.addTV("base", "bbccdd",
                "[(0-1)s:b|_0$<i>0<i>1|<>:base/s:t$<b>64<i>0<i>6<i>6<b>0]]"
                        + "[(1-2)s:b|_1$<i>1<i>2]" + "[(2-3)s:c|_2$<i>2<i>3]"
                        + "[(3-4)s:c|_3$<i>3<i>4]" + "[(4-5)s:d|_4$<i>4<i>5]"
                        + "[(5-6)s:d|_5$<i>5<i>6]");
        return fd;
    }


    private FieldDocument createFieldDoc2 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-2");
        fd.addTV("base", "beccea",
                "[(0-1)s:b|_0$<i>0<i>1|<>:base/s:t$<b>64<i>0<i>6<i>6<b>0]]"
                        + "[(1-2)s:e|_1$<i>1<i>2]" + "[(2-3)s:c|_2$<i>2<i>3]"
                        + "[(3-4)s:c|_3$<i>3<i>4]" + "[(4-5)s:e|_4$<i>4<i>5]"
                        + "[(5-6)s:a|_5$<i>5<i>6]");
        return fd;
    }


    private FieldDocument createFieldDoc3 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-3");
        fd.addTV("base", "cab",
                "[(0-1)s:c|_0$<i>0<i>1|<>:base/s:t$<b>64<i>0<i>3<i>3<b>0]]"
                        + "[(1-2)s:a|_1$<i>1<i>2]" + "[(2-3)s:b|_2$<i>2<i>3]");
        return fd;
    }


    private FieldDocument createFieldDoc4 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-3");
        fd.addTV("base", "ce",
                "[(0-1)s:c|_0$<i>0<i>1|<>:base/s:t$<b>64<i>0<i>2<i>2<b>0]]"
                        + "[(1-2)s:e|_1$<i>1<i>2]");
        return fd;
    }


    private String readFile (String path) {
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader in = new BufferedReader(new FileReader(path));
            String str;
            while ((str = in.readLine()) != null) {
                sb.append(str);
            }
            in.close();
        }
        catch (IOException e) {
            fail(e.getMessage());
        }
        return sb.toString();
    }
}
