package de.ids_mannheim.korap.index;

import static de.ids_mannheim.korap.TestSimple.getJSONQuery;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.junit.Test;

import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.query.DistanceConstraint;
import de.ids_mannheim.korap.query.SpanClassQuery;
import de.ids_mannheim.korap.query.SpanDistanceQuery;
import de.ids_mannheim.korap.query.SpanElementQuery;
import de.ids_mannheim.korap.query.SpanFocusQuery;
import de.ids_mannheim.korap.query.SpanNextQuery;
import de.ids_mannheim.korap.query.SpanReferenceQuery;
import de.ids_mannheim.korap.query.SpanRelationMatchQuery;
import de.ids_mannheim.korap.query.SpanRelationQuery;
import de.ids_mannheim.korap.query.wrap.SpanQueryWrapper;
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.util.QueryException;

public class TestReferenceIndex {
    private KrillIndex ki;
    private Result kr;

    @Test
    public void testCase1 () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(TestRelationIndex.createFieldDoc2());
        ki.commit();

        SpanTermQuery seq1 = new SpanTermQuery(new Term("base", "pos:V"));
        SpanElementQuery seq2 = new SpanElementQuery("base", "np");
        SpanClassQuery scq1 = new SpanClassQuery(seq1, (byte) 1);
        SpanClassQuery scq2 = new SpanClassQuery(seq2, (byte) 2);
        SpanNextQuery snq1 = new SpanNextQuery(scq1, scq2);

        SpanFocusQuery sfq1 = new SpanFocusQuery(snq1, (byte) 2);

        SpanRelationQuery srq = new SpanRelationQuery(new SpanTermQuery(
                new Term("base", "<:child-of")), true);
        // SpanSegmentQuery ssq = new SpanSegmentQuery(srq, sfq1,
        // true);
        // SpanFocusQuery sfq2 = new SpanFocusQuery(ssq, (byte) 1);
        // sfq2.setSorted(false);
        // sfq2.setMatchTemporaryClass(false);

        SpanElementQuery seq3 = new SpanElementQuery("base", "pp");
        SpanClassQuery scq3 = new SpanClassQuery(seq3, (byte) 3);
        // SpanSegmentQuery ssq2 = new SpanSegmentQuery(sfq2, scq3,
        // true);

        SpanRelationMatchQuery rq = new SpanRelationMatchQuery(srq, sfq1, scq3, true);

        // System.out.println(rq.toString());
        SpanFocusQuery sfq3 = new SpanFocusQuery(rq, (byte) 1);

        DistanceConstraint constraint = new DistanceConstraint(3, 3, true,
                false);
        SpanDistanceQuery sdq = new SpanDistanceQuery(sfq3, scq3, constraint,
                true);

        SpanReferenceQuery ref = new SpanReferenceQuery(sdq, (byte) 3, true);
        // System.out.println(ref.toString());

        kr = ki.search(ref, (short) 10);
        // for (Match km : kr.getMatches()) {
        // System.out.println(km.getStartPos() + "," + km.getEndPos()
        // + " "
        // + km.getSnippetBrackets());
        // }
        assertEquals(
                "spanReference(spanDistance(focus(1: focus(#[1,2]spanSegment("
                        + "focus(#1: spanSegment(spanRelation(base:<:child-of), focus(2: spanNext("
                        + "{1: base:pos:V}, {2: <base:np />})))), {3: <base:pp />}))), "
                        + "{3: <base:pp />}, [(w[3:3], ordered, notExcluded)]), 3)",
                ref.toString());

        assertEquals(1, kr.getMatch(0).getStartPos());
        assertEquals(7, kr.getMatch(0).getEndPos());
    }

    @Test
    public void testCase2() throws IOException, QueryException {

        String filepath = getClass().getResource(
                "/queries/reference/distance-reference.jsonld").getFile();
        SpanQueryWrapper sqwi = getJSONQuery(filepath);
        SpanQuery sq = sqwi.toQuery();

        // cat="vb" & cat="prp" & cat="nn" & #1 .notordered #2 & #1
        // .{0,2} #3 & #3 -> #2

        assertEquals(
            "spanReference(focus(#[1,2]spanSegment(focus(#2: "
                    + "spanSegment(spanRelation(tokens:>:stanford/d:tag), "
                    + "focus(3: spanDistance(focus(1: spanDistance({1: <tokens:vb />}, "
                    + "{2: <tokens:prp />}, [(w[0:1], notOrdered, notExcluded)])), "
                    + "{3: <tokens:nn />}, [(w[0:2], notOrdered, notExcluded)])))), "
                    + "{2: <tokens:prp />})), 2)",
            sq.toString());

        SpanElementQuery seq1 = new SpanElementQuery("tokens", "vb");
        // new SpanTermQuery(new Term("tokens", "c:vb"));
        SpanElementQuery seq2 = new SpanElementQuery("tokens", "prp");
        // new SpanTermQuery(new Term("tokens", "c:prp"));
        SpanElementQuery seq3 = new SpanElementQuery("tokens", "nn");
        // new SpanTermQuery(new Term("tokens", "c:nn"));
        SpanClassQuery scq1 = new SpanClassQuery(seq1, (byte) 1);
        SpanClassQuery scq2 = new SpanClassQuery(seq2, (byte) 2);
        SpanClassQuery scq3 = new SpanClassQuery(seq3, (byte) 3);

        // vb .{0,1} prp
        SpanDistanceQuery sdq1 = new SpanDistanceQuery(scq1, scq2,
                new DistanceConstraint(0, 1, false, false), true);
        SpanFocusQuery sfq1 = new SpanFocusQuery(sdq1, (byte) 1);

        // vb .{0,2} nn
        SpanDistanceQuery sdq2 = new SpanDistanceQuery(sfq1, scq3,
                new DistanceConstraint(0, 2, false, false), true);
        SpanFocusQuery sfq2 = new SpanFocusQuery(sdq2, (byte) 3);

        // nn -> prp
        SpanRelationQuery srq = new SpanRelationQuery(new SpanTermQuery(
                new Term("tokens", ">:stanford/d:tag")), true);
        SpanRelationMatchQuery rq = new SpanRelationMatchQuery(srq, sfq2, scq2,
                true);

        SpanReferenceQuery ref = new SpanReferenceQuery(rq, (byte) 2, true);

        ki = new KrillIndex();
        ki.addDoc(createFieldDoc0());
        ki.commit();

        kr = ki.search(ref, (short) 10);
        // for (Match km : kr.getMatches()) {
        // System.out.println(km.getStartPos() + "," + km.getEndPos()
        // + " "
        // + km.getSnippetBrackets());
        // }

        assertEquals(sq.toString(), ref.toString());
        assertEquals(2, kr.getMatch(0).getStartPos());
        assertEquals(4, kr.getMatch(0).getEndPos());
        assertEquals(5, kr.getMatch(1).getStartPos());
        assertEquals(9, kr.getMatch(1).getEndPos());
        assertEquals(11, kr.getMatch(2).getStartPos());
        assertEquals(13, kr.getMatch(2).getEndPos());

        // multiple references

        SpanFocusQuery sfq3 = new SpanFocusQuery(ref, (byte) 1);
        // vp -> nn
        SpanRelationMatchQuery rq2 = new SpanRelationMatchQuery(srq, sfq3,
                scq3, true);

        SpanReferenceQuery ref2 = new SpanReferenceQuery(rq2, (byte) 3, true);

        kr = ki.search(ref2, (short) 10);
        assertEquals(1, kr.getMatch(0).getStartPos());
        assertEquals(4, kr.getMatch(0).getEndPos());
        assertEquals(10, kr.getMatch(1).getStartPos());
        assertEquals(13, kr.getMatch(1).getEndPos());

        // for (Match km : kr.getMatches()) {
        // System.out.println(km.getStartPos() + "," + km.getEndPos()
        // + " "
        // + km.getSnippetBrackets());
        // }
    }

    // multiple references
    @Test
    public void testCase3() throws IOException, QueryException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc0());
        ki.commit();

        String filepath = getClass().getResource(
                "/queries/reference/distance-multiple-references.jsonld")
                .getFile();
        SpanQueryWrapper sqwi = getJSONQuery(filepath);
        SpanQuery sq = sqwi.toQuery();

        // 'cat="vb" & cat="prp" & cat="nn" & #1 .{0,1} #2 & #1 .{0,2}
        // #3 & #3 ->stanford/d #2 & #1 ->stanford #3' annis
        // without layer=c and + relation key
        assertEquals(
                "spanReference(focus(#[1,2]spanSegment(focus(#2: spanSegment(spanRelation(tokens:>:stanford/d:tag), "
                        + "focus(1: spanReference(focus(#[1,2]spanSegment(focus(#2: spanSegment(spanRelation(tokens:>:stanford/d:tag), "
                        + "focus(3: spanDistance(focus(1: spanDistance({1: <tokens:vb />}, {2: <tokens:prp />}, "
                        + "[(w[0:1], notOrdered, notExcluded)])), {3: <tokens:nn />}, [(w[0:2], notOrdered, notExcluded)])))), "
                        + "{2: <tokens:prp />})), 2)))), {3: <tokens:nn />})), 3)",
                sq.toString());
        kr = ki.search(sq, (short) 10);
        // for (Match km : kr.getMatches()) {
        // System.out.println(km.getStartPos() + "," + km.getEndPos()
        // + " "
        // + km.getSnippetBrackets());
        // }
        assertEquals(1, kr.getMatch(0).getStartPos());
        assertEquals(4, kr.getMatch(0).getEndPos());
        assertEquals(10, kr.getMatch(1).getStartPos());
        assertEquals(13, kr.getMatch(1).getEndPos());
    }

    // multiple document
    @Test
    public void testCase4() throws Exception {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc0());
        ki.addDoc(createFieldDoc1());
        ki.commit();

        String filepath = getClass().getResource(
                "/queries/reference/distance-reference.jsonld")
                .getFile();
        SpanQueryWrapper sqwi = getJSONQuery(filepath);
        SpanQuery sq = sqwi.toQuery();

        kr = ki.search(sq, (short) 10);

        assertEquals(4, kr.getTotalResults());
        assertEquals("doc-1", kr.getMatch(3).getDocID());
        assertEquals(2, kr.getMatch(3).getStartPos());
        assertEquals(4, kr.getMatch(3).getEndPos());
    }

    public static FieldDocument createFieldDoc1() {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-1");
        fd.addTV(
                "tokens",
                "Frankenstein, treat my daughter well. She is the one that saved your master who you hold so dear.",

                "[(0-12)s:Frankenstein|_0#0-12|<>:nn#0-12$<i>1<s>18|<>:s#0-37$<i>4<s>1|"
                        + "<>:np#0-13$<i>1<s>2|"
                        + "<:stanford/d:tag$<i>1<s>1<s>18<s>19]"

                        + "[(14-19)s:treat|_1#14-19|<>:vb#14-19$<i>2<s>19|<>:vp#14-36$<i>4<s>3|"
                        + ">:stanford/d:tag$<i>0<s>2<s>19<s>18|"
                        + ">:stanford/d:tag$<i>3<s>3<s>19<s>21|"
                        + ">:stanford/d:tag$<i>4<s>4<s>19<s>22]"

                        + "[(20-22)s:my|_2#20-22|<>:prp#20-22$<i>3<s>20|<>:np#20-31$<i>3<s>4]"

                        + "[(23-31)s:daughter|_3#23-31|<>:nn#23-31$<i>4<s>21|"
                        + ">:stanford/d:tag$<i>2<s>5<s>21<s>20]"

                        + "[(32-36)s:well|_4#32-36|<>:rb#32-36$<i>5<s>22|<>:advp#32-36$<i>4<s>5]");
        return fd;
    }

    public static FieldDocument createFieldDoc0 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-0");
        fd.addTV(
                "tokens",
                "Frankenstein, treat my daughter well. She is the one that saved your master who you hold so dear.",
                
                "[(0-12)s:Frankenstein|_0#0-12|<>:nn#0-12$<i>1<s>18|<>:s#0-37$<i>4<s>1|"
                        + "<>:np#0-13$<i>1<s>2|"
                        + "<:stanford/d:tag$<i>1<s>1<s>18<s>19]"

                + "[(14-19)s:treat|_1#14-19|<>:vb#14-19$<i>2<s>19|<>:vp#14-36$<i>4<s>3|"
                        + ">:stanford/d:tag$<i>0<s>2<s>19<s>18|"
                        + ">:stanford/d:tag$<i>3<s>3<s>19<s>21|"
                        + ">:stanford/d:tag$<i>4<s>4<s>19<s>22]"

                + "[(20-22)s:my|_2#20-22|<>:prp#20-22$<i>3<s>20|<>:np#20-31$<i>3<s>4]"

                + "[(23-31)s:daughter|_3#23-31|<>:nn#23-31$<i>4<s>21|"
                        + ">:stanford/d:tag$<i>2<s>5<s>21<s>20]"

                + "[(32-36)s:well|_4#32-36|<>:rb#32-36$<i>5<s>22|<>:advp#32-36$<i>4<s>5]"

                + "[(38-41)s:She|_5#38-41|<>:prp#38-41$<i>6<s>23|<>:s#38-97$<i>17<s>6]"

                + "[(42-44)s:is|_6#42-44|<>:vb#42-44$<i>7<s>24|<>:vp#42-96$<i>17<s>7]"

                + "[(45-48)s:the|_7#45-48|<>:dt#45-48$<i>8<s>25|<>:np#45-52$<i>8<s>8|<>:np#45-96$<i>17<s>9]"

                + "[(49-52)s:one|_8#49-52|<>:nn#49-52$<i>9<s>26|"
                        + ">:stanford/d:tag$<i>5<s>6<s>26<s>23|"
                        + ">:stanford/d:tag$<i>6<s>7<s>26<s>24|"
                        + ">:stanford/d:tag$<i>7<s>8<s>26<s>25|"
                        + ">:stanford/d:tag$<i>10<s>9<s>26<s>28]"
                
                + "[(53-57)s:that|_9#53-57|<>:rp#53-57$<i>10<s>27|<>:sb#53-96$<i>17<s>10]"
                                              
                + "[(58-63)s:saved|_10#58-63|<>:vb#58-63$<i>11<s>28|<>:s#58-96$<i>17<s>11|"
                        + "<>:vp#58-96$<i>17<s>12|"
                        + ">:stanford/d:tag$<i>9<s>10<s>28<s>27|"
                        + ">:stanford/d:tag$<i>12<s>11<s>28<s>30|"
                        + ">:stanford/d:tag$<i>15<s>12<s>28<s>33]"

                + "[(64-68)s:your|_11#64-68|<>:prp#64-68$<i>12<s>29|<>:np#64-75$<i>12<s>13]"

                + "[(69-75)s:master|_12#69-75|<>:nn#69-75$<i>13<s>30|"
                        + ">:stanford/d:tag$<i>11<s>13<s>30<s>29]"

                + "[(76-79)s:who|_13#76-79|<>:rp#76-79$<i>14<s>31|<>:sb#76-96$<i>17<s>14]"

                + "[(80-83)s:you|_14#80-83|<>:prp#80-83$<i>15<s>32|<>:s#80-96$<i>17<s>15]"

                + "[(84-88)s:hold|_15#84-88|<>:vb#84-88$<i>16<s>33|<>:vp#84-96$<i>17<s>16|"
                        + ">:stanford/d:tag$<i>13<s>14<s>33<s>31|"
                        + ">:stanford/d:tag$<i>14<s>15<s>33<s>32|"
                        + ">:stanford/d:tag$<i>17<s>16<s>33<s>35]"

                + "[(89-91)s:so|_16#89-91|<>:rb#89-91$<i>17<s>341|<>:adjp#89-96$<i>17<s>17]"

                + "[(92-96)s:dear|_17#92-96|<>:jj#92-96$<i>18<s>35|"
                        + ">:stanford/d:tag$<i>16<s>17<s>35<s>34]"
                 );
                        
        return fd;
    }
}
