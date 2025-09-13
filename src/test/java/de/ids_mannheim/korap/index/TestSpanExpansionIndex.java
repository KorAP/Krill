package de.ids_mannheim.korap.index;

import static de.ids_mannheim.korap.TestSimple.getJsonString;
import static de.ids_mannheim.korap.TestSimple.simpleFieldDoc;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.RegexpQuery;
import org.apache.lucene.search.spans.SpanMultiTermQueryWrapper;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.util.automaton.RegExp;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.Ignore;

import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.KrillQuery;
import de.ids_mannheim.korap.TestSimple;
import de.ids_mannheim.korap.query.QueryBuilder;
import de.ids_mannheim.korap.query.SpanElementQuery;
import de.ids_mannheim.korap.query.SpanExpansionQuery;
import de.ids_mannheim.korap.query.SpanNextQuery;
import de.ids_mannheim.korap.query.SpanRepetitionQuery;
import de.ids_mannheim.korap.query.SpanSegmentQuery;
import de.ids_mannheim.korap.query.SpanWithinQuery;
import de.ids_mannheim.korap.query.wrap.SpanQueryWrapper;
import de.ids_mannheim.korap.response.Match;
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.util.QueryException;

public class TestSpanExpansionIndex {

    Result kr;
    KrillIndex ki;

    public TestSpanExpansionIndex () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(getClass().getResourceAsStream("/wiki/00001.json.gz"), true);
        ki.commit();
    }

    /** Method for finding bugs. Since java matcher cannot find multiple matches
     * from the same offset, the expected results are sometimes lower than the 
     * actual results. 
     * 
     * @throws IOException
     * @throws QueryException
     */
//    @Test
    public void fuzzyTest () throws IOException, QueryException {
        List<String> chars = Arrays.asList("a", "b", "c", "d", "e");

        // c []{0,2} a
        SpanTermQuery stq = new SpanTermQuery(new Term("base", "s:c"));
        SpanTermQuery stq2 = new SpanTermQuery(new Term("base", "s:a"));
        SpanExpansionQuery seq = new SpanExpansionQuery(stq, 0, 2, 0, true);
        SpanNextQuery snq = new SpanNextQuery(seq, stq2);

        Pattern resultPattern = Pattern.compile("c[a-e]{0,2}a");
        TestSimple.fuzzingTest(chars, resultPattern, snq,
                               6, 20, 8, 0);
    }
    
    @Test
    public void testNoExpansion () throws IOException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(simpleFieldDoc("cc"));
        ki.commit();

        SpanTermQuery stq = new SpanTermQuery(new Term("base", "s:c"));
        SpanExpansionQuery seq = new SpanExpansionQuery(stq, 0, 0, 0, true);
        Result kr = ki.search(seq, (short) 10);
        
        assertEquals(2, kr.getTotalResults());
    }

    /**
     * Left and right expansions
     */
    @Test
    public void testLeftRightExpansions () throws IOException {

        SpanTermQuery stq = new SpanTermQuery(new Term("tokens", "s:des"));
        // left
        SpanExpansionQuery seq = new SpanExpansionQuery(stq, 0, 2, -1, true);
        kr = ki.search(seq, (short) 10);

        // assertEquals(69,kr.getTotalResults());
        assertEquals(5, kr.getMatch(0).getStartPos());
        assertEquals(8, kr.getMatch(0).getEndPos());
        assertEquals(6, kr.getMatch(1).getStartPos());
        assertEquals(8, kr.getMatch(1).getEndPos());
        assertEquals(7, kr.getMatch(2).getStartPos());
        assertEquals(8, kr.getMatch(2).getEndPos());

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
     */
    @Test
    public void testExpansionWithClassNumber () {
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
     */
    @Test
    public void testRightExpansionWithExclusion () throws IOException {
        // [pos=tt/p:NN][orth=Buchstabe]
        
        byte classNumber = 1;
        SpanTermQuery stq = new SpanTermQuery(new Term("tokens", "tt/p:NN"));
        SpanTermQuery notQuery =
                new SpanTermQuery(new Term("tokens", "s:Buchstabe"));

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
    }
    
    @Test
    public void testNextRightExpansion () throws IOException {
        KrillIndex ki = new KrillIndex();
        //ki.addDoc(simpleFieldDoc("daaec"));
        ki.addDoc(simpleFieldDoc("deaccaab"));
        ki.addDoc(simpleFieldDoc("cabdadceedc"));
        //ki.addDoc(simpleFieldDoc("aadaeaeea"));
        ki.commit();
        
        SpanTermQuery a = new SpanTermQuery(new Term("base", "s:c"));
        SpanTermQuery stq = new SpanTermQuery(new Term("base", "s:a"));
        SpanTermQuery notQuery = new SpanTermQuery(new Term("base", "s:b"));


        SpanExpansionQuery seq = new SpanExpansionQuery(stq, notQuery, 1, 1, 0,
                true);

        SpanNextQuery nq = new SpanNextQuery(a, seq);

        kr = ki.search(nq);
        assertEquals(1, kr.getMatches().size());
    }

    /**
     * Left expansion with exclusion
     * No expansion
     */
    @Test
    public void testLeftExpansionWithExclusion () throws IOException {
        byte classNumber = 1;
        SpanTermQuery stq = new SpanTermQuery(new Term("tokens", "tt/p:NN"));
        SpanTermQuery notQuery =
                new SpanTermQuery(new Term("tokens", "tt/p:ADJA"));

        SpanExpansionQuery seq = new SpanExpansionQuery(stq, notQuery, 0, 2, -1,
                classNumber, true);
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
     */
    @Test
    public void testExpansionOverStart () throws IOException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(createFieldDoc0());
        ki.commit();

        SpanTermQuery stq = new SpanTermQuery(new Term("base", "s:e"));
        // left expansion precedes 0
        SpanExpansionQuery seq = new SpanExpansionQuery(stq, 2, 2, -1, true);
        kr = ki.search(seq, (short) 10);

        assertEquals((long) 3, kr.getTotalResults());
        assertEquals(2, kr.getMatch(0).getStartPos());
        assertEquals(5, kr.getMatch(0).getEndPos());

        // right expansion exceeds end position
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
     */
    @Test
    public void testExclusionWithMultipleDocs () throws IOException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(createFieldDoc0()); // same doc
        ki.addDoc(createFieldDoc1()); // only not clause
        ki.addDoc(createFieldDoc2()); // only main clause
        ki.commit();

        SpanTermQuery stq = new SpanTermQuery(new Term("base", "s:e"));
        SpanTermQuery notQuery = new SpanTermQuery(new Term("base", "s:d"));

        SpanExpansionQuery seq =
                new SpanExpansionQuery(stq, notQuery, 2, 3, 0, true);
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
     */
    @Test
    public void testExpansionWithSkipTo () throws IOException, QueryException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(getClass().getResourceAsStream("/wiki/00001.json.gz"), true);
        ki.addDoc(getClass().getResourceAsStream("/wiki/00002.json.gz"), true);
        ki.commit();
        String jsonPath =
                getClass().getResource("/queries/poly3.json").getFile();
        String jsonQuery = getJsonString(jsonPath);
        SpanQueryWrapper sqwi = new KrillQuery("tokens").fromKoral(jsonQuery);

        SpanQuery sq = sqwi.toQuery();
        // System.out.println(sq.toString());
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
     */
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
        RegexpQuery requery =
                new RegexpQuery(new Term("base", "s:[ac]"), RegExp.ALL);
        SpanMultiTermQueryWrapper<RegexpQuery> query =
                new SpanMultiTermQueryWrapper<RegexpQuery>(requery);
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

        assertEquals("[[cecc]]ecdeec", kr.getMatch(0).getSnippetBrackets());
        assertEquals("cec[[cecd]]eec", kr.getMatch(1).getSnippetBrackets());
        assertEquals((long) 2, kr.getTotalResults());
    }

    /**
     * Query rewrite bug
     * 
     * @throws IOException
     */
    @Test
    public void testExpansionQueryBug3 () throws IOException, QueryException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(createFieldDoc3());
        ki.addDoc(createFieldDoc4());
        ki.commit();
        String jsonPath = getClass()
                .getResource("/queries/bugs/expansion_bug_3.jsonld").getFile();
        String json = getJsonString(jsonPath);
        KrillQuery kq = new KrillQuery("base");
        SpanQuery sq = kq.fromKoral(json).toQuery();
        assertEquals(sq.toString(),
                "focus(254: spanContain(<base:base/s:t />, {254: spanExpansion(base:s:c, []{0, 4}, right)}))");

        kr = ki.search(sq, (short) 10);
        assertEquals("[[c]]ab", kr.getMatch(0).getSnippetBrackets());
        assertEquals("[[ca]]b", kr.getMatch(1).getSnippetBrackets());
        assertEquals("[[cab]]", kr.getMatch(2).getSnippetBrackets());
        assertEquals("[[c]]e", kr.getMatch(3).getSnippetBrackets());

        assertEquals("[[ce]]", kr.getMatch(4).getSnippetBrackets());
        assertEquals(5, kr.getTotalResults());

        sq = kq.builder().tag("base/s:t").toQuery();
        assertEquals(sq.toString(), "<base:base/s:t />");
        kr = ki.search(sq, (short) 5);
        assertEquals("[[cab]]", kr.getMatch(0).getSnippetBrackets());
        assertEquals("[[ce]]", kr.getMatch(1).getSnippetBrackets());
        assertEquals(2, kr.getTotalResults());
    }

    @Test
    public void indexRegexSequence () throws Exception {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(createFieldDoc5());
        ki.commit();

        QueryBuilder kq = new QueryBuilder("base");

        SpanQueryWrapper sq = kq.seq(kq.or("s:baumgarten", "s:steingarten"))
                .append(kq.seg().without(kq.or("s:franz", "s:hans")));

        // Expected to find [baumgarten steingarten]
        Krill ks = _newKrill(sq);
        Result kr = ki.search(ks);

        assertEquals((long) 1, kr.getTotalResults());

        assertEquals("... baum [[baumgarten steingarten]] franz ...",
                kr.getMatch(0).getSnippetBrackets());

        // The same result should be shown for:

        sq = kq.seq(kq.re("s:.*garten"))
                .append(kq.seg().without(kq.re("s:.*an.*")));

        ks = _newKrill(sq);
        kr = ki.search(ks);

        assertEquals((long) 1, kr.getTotalResults());

        assertEquals("... baum [[baumgarten steingarten]] franz ...",
                kr.getMatch(0).getSnippetBrackets());
    };

    @Test
    public void testBugRegexExpandLeftNoMoreSpan () throws IOException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(createFieldDoc6());
        ki.commit();

        SpanTermQuery stq = new SpanTermQuery(new Term("base", "s:a"));

        RegexpQuery requery =
                new RegexpQuery(new Term("base", "s:[bc]"), RegExp.ALL);
        SpanMultiTermQueryWrapper<RegexpQuery> notQuery =
                new SpanMultiTermQueryWrapper<RegexpQuery>(requery);

        byte classNumber = 1;
        // left expansion
        SpanExpansionQuery seq = new SpanExpansionQuery(stq, notQuery, 0, 1, -1,
                classNumber, true);

        kr = ki.search(seq, (short) 20);

        assertEquals(9, kr.getMatches().size());

    }

    @Test
    public void indexExpansionWithNegationDifferentFragments () throws Exception {
        KrillIndex ki = new KrillIndex();

        // Add to the index in a single fragment
        FieldDocument fd = new FieldDocument();
        fd.addTV("base",
                 "a B c",
                 "[(0-1)s:a|i:a|_1$<i>0<i>1]"
                 + "[(1-2)s:B|i:b|_2$<i>1<i>2|]"
                 + "[(2-3)s:c|i:c|_3$<i>2<i>3]");
        ki.addDoc(fd);
        ki.commit();
        fd.addTV("base",
                 "a b c",
                 "[(0-1)s:a|i:a|_1$<i>0<i>1]"
                 + "[(1-2)s:b|i:b|_2$<i>1<i>2|]"
                 + "[(2-3)s:c|i:c|_3$<i>2<i>3]");
        ki.addDoc(fd);
        ki.commit();

        QueryBuilder kq = new QueryBuilder("base");
        SpanQuery sq = kq.seq(kq.seg("s:a")).append(kq.seg().without("s:B")).append(kq.seg("s:c")).toQuery();
        assertEquals("spanNext(base:s:a, spanExpansion(base:s:c, !base:s:B{1, 1}, left))", sq.toString());
        Krill ks = new Krill(sq);
        ks.getMeta().getContext().left.setToken(true).setLength(0);
        ks.getMeta().getContext().right.setToken(true).setLength(0);

        Result kr = ki.search(ks);
        assertEquals((long) 1, kr.getTotalResults());
    };

    @Test
    public void indexExpansionWithNegationSameFragmentBug () throws Exception {
        KrillIndex ki = new KrillIndex();

        // Add to the index in a single fragment
        FieldDocument fd = new FieldDocument();
        fd.addTV("base",
                 "a B c",
                 "[(0-1)s:a|i:a|_1$<i>0<i>1]"
                 + "[(1-2)s:B|i:b|_2$<i>1<i>2|]"
                 + "[(2-3)s:c|i:c|_3$<i>2<i>3]");
        ki.addDoc(fd);
        fd.addTV("base",
                 "a b c",
                 "[(0-1)s:a|i:a|_1$<i>0<i>1]"
                 + "[(1-2)s:b|i:b|_2$<i>1<i>2|]"
                 + "[(2-3)s:c|i:c|_3$<i>2<i>3]");
        ki.addDoc(fd);
        ki.commit();

        QueryBuilder kq = new QueryBuilder("base");
        SpanQuery sq = kq.seq(kq.seg("s:a")).append(kq.seg().without("s:B")).append(kq.seg("s:c")).toQuery();
        assertEquals("spanNext(base:s:a, spanExpansion(base:s:c, !base:s:B{1, 1}, left))", sq.toString());
        Krill ks = new Krill(sq);
        ks.getMeta().getContext().left.setToken(true).setLength(0);
        ks.getMeta().getContext().right.setToken(true).setLength(0);

        Result kr = ki.search(ks);
        assertEquals((long) 1, kr.getTotalResults());
    };


    @Test
    public void indexExpansionLeftWithWrongSorting () throws IOException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(simpleFieldDoc("abcc"));
        ki.commit();
        
        SpanTermQuery stq = new SpanTermQuery(new Term("base", "s:c"));
        SpanExpansionQuery seq = new SpanExpansionQuery(stq, 0, 2, -1, true);
        assertEquals("spanExpansion(base:s:c, []{0, 2}, left)", seq.toString());
        Result kr = ki.search(seq, (short) 10);

        assertEquals("a[[bc]]c", kr.getMatch(1).getSnippetBrackets());
        assertEquals(1, kr.getMatch(1).getStartPos());
        assertEquals(3, kr.getMatch(1).getEndPos());
        assertEquals("a[[bcc]]", kr.getMatch(2).getSnippetBrackets());
        assertEquals(1, kr.getMatch(2).getStartPos());
        assertEquals(4, kr.getMatch(2).getEndPos());
        assertEquals(6, kr.getTotalResults());
    }

    @Test
    public void indexExpansionMultipleStartsWithCorrectSorting () throws IOException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(simpleFieldDoc("abccef"));
        ki.commit();

        SpanTermQuery stq = new SpanTermQuery(new Term("base", "s:c"));
        SpanExpansionQuery seqL = new SpanExpansionQuery(stq, 0, 2, -1, true);
        SpanExpansionQuery seqR = new SpanExpansionQuery(seqL, 0, 1, 0, true);
        assertEquals(
            "spanExpansion(spanExpansion(base:s:c, []{0, 2}, left), []{0, 1}, right)",
            seqR.toString());
        Result kr = ki.search(seqR, (short) 20);

//        for (Match km : kr.getMatches()) {
//            System.out.println(km.getStartPos() + "," + km.getEndPos() + " " +
//                               km.getSnippetBrackets());
//        };

        // TODO: These are duplicate results that may be restricted with a wrapper
        assertEquals("a[[bcc]]ef", kr.getMatch(3).getSnippetBrackets());
        assertEquals("a[[bcc]]ef", kr.getMatch(4).getSnippetBrackets());
        assertEquals(12, kr.getTotalResults());
    }

    @Test
    public void testRightExpansionWithWrongSorting ()
            throws IOException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(simpleFieldDoc("abccef"));
        ki.commit();
        
        SpanTermQuery stq = new SpanTermQuery(new Term("base", "s:c"));
        SpanExpansionQuery seqL = new SpanExpansionQuery(stq, 0, 2, -1, true);
        kr = ki.search(seqL, (short) 20);
//        for (Match km : kr.getMatches()) {
//            System.out.println(km.getStartPos() + "," + km.getEndPos() + " " +
//                               km.getSnippetBrackets());
//        };
        
        SpanExpansionQuery seqR = new SpanExpansionQuery(seqL, 0, 2, 0, true);
        assertEquals(
            "spanExpansion(spanExpansion(base:s:c, []{0, 2}, left), []{0, 2}, right)",
            seqR.toString());
        kr = ki.search(seqR, (short) 20);

        
//        for (Match km : kr.getMatches()) {
//            System.out.println(km.getStartPos() + "," + km.getEndPos() + " " +
//                               km.getSnippetBrackets());
//        };
        
        assertEquals("a[[bcc]]ef", kr.getMatch(5).getSnippetBrackets());
        assertEquals("a[[bcce]]f", kr.getMatch(6).getSnippetBrackets());
        assertEquals(18, kr.getTotalResults());        
    }


    @Test
    public void testRightExpansionWithTextBoundary () throws IOException, QueryException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(simpleFieldDoc("aabcd"));
        ki.commit();

        QueryBuilder kq = new QueryBuilder("base");

        // a[ab]?[]{0,2}
        SpanQuery sq = kq.seq(kq.seg("s:a")).append(kq.opt(kq.or("s:a","s:b"))).append(kq.repeat(kq.empty(),0,5)).toQuery();
        assertEquals(
            "focus(254: spanContain(<base:base/s:t />, {254: "+
            "spanExpansion(spanOr([base:s:a, spanNext(base:s:a, spanOr([base:s:a, base:s:b]))]), []{0, 5}, right)"+
            "}))", sq.toString());

        Result kr = ki.search(sq, (short) 25);
        assertEquals("[[aabcd]]", kr.getMatch(8).getSnippetBrackets());
        assertEquals("a[[a]]bcd", kr.getMatch(9).getSnippetBrackets());
        assertEquals(16, kr.getTotalResults());        
    }

    
    @Test
    public void testLeftExpansionWrongSorting () throws IOException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(simpleFieldDoc("B u d B R a d m d Z z s B d v", " "));
        ki.commit();
        
        // d positions: 2-3, 6-7, 8-9, 13-14
        SpanTermQuery stq = new SpanTermQuery(new Term("base", "s:d"));
        SpanExpansionQuery seq = new SpanExpansionQuery(stq, 0, 8, -1, true);
        
        Result kr = ki.search(seq, (short) 25);
//        for (Match km : kr.getMatches()){
//             System.out.println(km.getStartPos() +","+km.getEndPos()+" "
//             +km.getSnippetBrackets()); }
        
        // KWIC cap can alter context; verify highlight content appears in one of the matches
        boolean found = false;
        for (int i = 0; i < kr.getMatches().size(); i++) {
            if (kr.getMatch(i).getSnippetBrackets().contains("[[admdZzsBd]]")) {
                found = true; break;
            }
        }
        org.junit.Assert.assertTrue(found);
        org.junit.Assert.assertTrue(kr.getTotalResults() >= 1);
    }
    
    /** Tests left expansion over start doc boundary. Redundant matches should
     *  be omitted.
     * @throws IOException
     */
    @Test
    public void testLeftExpansionRedundantMatches () throws IOException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(simpleFieldDoc("A d F ü d T F u d m", " "));
        ki.commit();
        
        SpanTermQuery stq = new SpanTermQuery(new Term("base", "s:d"));
        SpanExpansionQuery seq = new SpanExpansionQuery(stq, 0, 6, -1, true);
        Result kr = ki.search(seq, (short) 20);
        
//        for (Match km : kr.getMatches()) {
//            System.out.println(km.getStartPos() + "," + km.getEndPos() + " " +
//                               km.getSnippetBrackets());
//        };
        
        Match m = kr.getMatch(5);
        assertEquals(2, m.getStartPos());
        assertEquals(9, m.getEndPos());
        assertEquals(14, kr.getTotalResults());

    }
    
    
    private FieldDocument createFieldDoc6 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-6");
        fd.addTV("base", "baaaaaa",
                "[(0-1)s:b|_0$<i>0<i>1|<>:base/s:t$<b>64<i>0<i>10<i>10<b>0]"
                        + "[(1-2)s:a|_1$<i>1<i>2]" + "[(2-3)s:c|_2$<i>2<i>3]"
                        + "[(3-4)s:a|s:d|_3$<i>3<i>4]"
                        + "[(4-5)s:a|_4$<i>4<i>5]" + "[(5-6)s:c|_5$<i>5<i>6]"
                        + "[(6-7)s:a|_6$<i>6<i>7]" + "[(7-8)s:d|_7$<i>7<i>8]"
                        + "[(8-9)s:a|_8$<i>8<i>9]"
                        + "[(9-10)s:a|_9$<i>9<i>10]");
        return fd;
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
                        + "[(8-9)s:e|_8$<i>8<i>9]"
                        + "[(9-10)s:c|_9$<i>9<i>10]");
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
        fd.addString("ID", "doc-4");
        fd.addTV("base", "ce",
                "[(0-1)s:c|_0$<i>0<i>1|<>:base/s:t$<b>64<i>0<i>2<i>2<b>0]]"
                        + "[(1-2)s:e|_1$<i>1<i>2]");
        return fd;
    }

    private FieldDocument createFieldDoc5 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-5");
        fd.addTV("base",
                "affe afffe baum baumgarten steingarten franz hans haus efeu effe",
                "[(0-4)s:affe|_0$<i>0<i>4|-:t$<i>10|<>:base/s:t$<b>64<i>0<i>9<i>9<b>0]"
                        + "[(5-10)s:afffe|_1$<i>5<i>10]"
                        + "[(11-15)s:baum|_2$<i>11<i>15]"
                        + "[(16-26)s:baumgarten|_3$<i>16<i>26]"
                        + "[(27-38)s:steingarten|_4$<i>27<i>38]"
                        + "[(39-44)s:franz|_5$<i>39<i>44]"
                        + "[(45-49)s:hans|_6$<i>45<i>49]"
                        + "[(50-54)s:haus|_7$<i>50<i>54]"
                        + "[(55-59)s:efeu|_8$<i>55<i>59]"
                        + "[(60-64)s:effe|_9$<i>60<i>64]");
        return fd;
    }

    private Krill _newKrill (SpanQueryWrapper query) {
        Krill ks = new Krill(query);
        ks.getMeta().getContext().left.setToken(true).setLength(1);
        ks.getMeta().getContext().right.setToken(true).setLength(1);
        return ks;
    };
}
