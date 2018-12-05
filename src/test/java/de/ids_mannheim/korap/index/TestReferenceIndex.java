package de.ids_mannheim.korap.index;

import static de.ids_mannheim.korap.TestSimple.*;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.junit.Test;
import org.junit.Ignore;

import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.constants.RelationDirection;
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
import de.ids_mannheim.korap.response.Match;
import de.ids_mannheim.korap.util.QueryException;

public class TestReferenceIndex {
    private KrillIndex ki;
    private Result kr;


    @Test
    public void testCase1 () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(TestRelationIndex.createFieldDoc2());
        ki.commit();

        SpanTermQuery stq = new SpanTermQuery(new Term("base", "pos:V"));
        SpanElementQuery seq = new SpanElementQuery("base", "np");
        SpanClassQuery scq1 = new SpanClassQuery(stq, (byte) 1);
        SpanClassQuery scq2 = new SpanClassQuery(seq, (byte) 2);
        SpanNextQuery snq1 = new SpanNextQuery(scq1, scq2);

        SpanFocusQuery sfq1 = new SpanFocusQuery(snq1, (byte) 2);

        SpanRelationQuery srq = new SpanRelationQuery(
                new SpanTermQuery(new Term("base", "<:child-of")), true, 
                RelationDirection.LEFT);

        SpanElementQuery seq2 = new SpanElementQuery("base", "pp");
        SpanClassQuery scq3 = new SpanClassQuery(seq2, (byte) 3);

        SpanRelationMatchQuery rq = new SpanRelationMatchQuery(srq, sfq1, scq3,
                true);
        
        // focus on np
        SpanFocusQuery sfq2 = new SpanFocusQuery(rq, (byte) 1);

        DistanceConstraint constraint = new DistanceConstraint(3, 3, true,
                false);
        SpanDistanceQuery sdq = new SpanDistanceQuery(sfq2, scq3, constraint,
                true);

        SpanReferenceQuery ref = new SpanReferenceQuery(sdq, (byte) 3, true);

        kr = ki.search(ref, (short) 10);
        /*
         * for (Match km : kr.getMatches()) {
         * 
         * System.out.println(km.getStartPos() + "," + km.getEndPos()
         * + " " + km.getSnippetBrackets()); }
         * System.out.println(kr.getTotalResults());
         */
        
        // cat=V & cat=np & cat=pp & #1 . #2 & #3 ->child-of #2 & #1 .{3,3} #3
        assertEquals(
                "spanReference(spanDistance(focus(1: focus(#[1,2]spanSegment("
                        + "focus(#1: spanSegment(spanRelation(base:<:child-of), focus(2: spanNext("
                        + "{1: base:pos:V}, {2: <base:np />})))), {3: <base:pp />}),sorting)), "
                        + "{3: <base:pp />}, [(w[3:3], ordered, notExcluded)]), 3)",
                ref.toString());

        assertEquals(1, kr.getMatch(0).getStartPos());
        assertEquals(7, kr.getMatch(0).getEndPos());
    }


    @Test
    @Ignore
    public void testCase2 () throws IOException, QueryException {

        String filepath = getClass()
                .getResource("/queries/reference/distance-reference.jsonld")
                .getFile();
        SpanQueryWrapper sqwi = getJsonQuery(filepath);
        SpanQuery sq = sqwi.toQuery();

        // cat="vb" & cat="prp" & cat="nn" & #1 .{0,1} #2 & #1
        // .{0,2} #3 & #3 -> #2

        assertEquals(
                "spanReference(focus(#[1,2]spanSegment({2: <tokens:prp />}, focus(#2: "
                        + "spanSegment(spanRelation(tokens:>:stanford/d:tag), "
                        + "focus(3: spanDistance(focus(1: spanDistance({1: <tokens:vb />}, "
                        + "{2: <tokens:prp />}, [(w[1:2], notOrdered, notExcluded)]),sorting), "
                        + "{3: <tokens:nn />}, [(w[1:3], notOrdered, notExcluded)]),sorting)),sorting)"
                        + "),sorting), 2)",
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

        SpanDistanceQuery sdq1 = new SpanDistanceQuery(scq1, scq2,
                new DistanceConstraint(1, 2, false, false), true);
        SpanFocusQuery sfq1 = new SpanFocusQuery(sdq1, (byte) 1);
        sfq1.setSorted(false);

        SpanDistanceQuery sdq2 = new SpanDistanceQuery(sfq1, scq3,
                new DistanceConstraint(1, 3, false, false), true);
        SpanFocusQuery sfq2 = new SpanFocusQuery(sdq2, (byte) 3);
        sfq1.setSorted(false);

        // nn -> prp
        SpanRelationQuery srq = new SpanRelationQuery(
                new SpanTermQuery(new Term("tokens", ">:stanford/d:tag")),
                true, RelationDirection.RIGHT);
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
        SpanRelationMatchQuery rq2 = new SpanRelationMatchQuery(srq, sfq3, scq3,
                true);

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
    public void testCase3 () throws IOException, QueryException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc0());
        ki.commit();

        String filepath = getClass()
                .getResource(
                        "/queries/reference/distance-multiple-references.jsonld")
                .getFile();
        SpanQueryWrapper sqwi = getJsonQuery(filepath);
        SpanQuery sq = sqwi.toQuery();

        // 'cat="vb" & cat="prp" & cat="nn" & #1 .{0,1} #2 & #1 .{0,2}
        // #3 & #3 ->stanford/d #2 & #1 ->stanford #3' annis
        // without layer=c and + relation key
        assertEquals(
                "spanReference(focus(#[1,2]spanSegment({3: <tokens:nn />}, "
                        + "focus(#2: spanSegment(spanRelation(tokens:>:stanford/d:tag), "
                        + "focus(1: spanReference(focus(#[1,2]spanSegment({2: <tokens:prp />}, "
                        + "focus(#2: spanSegment(spanRelation(tokens:>:stanford/d:tag), "
                        + "focus(3: spanDistance(focus(1: spanDistance({1: <tokens:vb />}, "
                        + "{2: <tokens:prp />}, [(w[1:2], notOrdered, notExcluded)]),sorting), "
                        + "{3: <tokens:nn />}, [(w[1:3], notOrdered, notExcluded)]),sorting)),sorting)),sorting), 2),sorting)),sorting)),sorting), 3)",
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
    public void testCase4 () throws Exception {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc0());
        ki.addDoc(createFieldDoc1());
        ki.commit();

        String filepath = getClass()
                .getResource("/queries/reference/distance-reference.jsonld")
                .getFile();
        SpanQueryWrapper sqwi = getJsonQuery(filepath);
        SpanQuery sq = sqwi.toQuery();

        kr = ki.search(sq, (short) 10);

        /*
          for (Match km : kr.getMatches()) {
            System.out.println(km.getDocID() + ":" + km.getStartPos() + "," + km.getEndPos()
                               + " "
                               + km.getSnippetBrackets());
        }
        */

        assertEquals(4, kr.getTotalResults());
        assertEquals("doc-1", kr.getMatch(3).getDocID());
        
        assertEquals(2, kr.getMatch(3).getStartPos());
        assertEquals(4, kr.getMatch(3).getEndPos());
    }


    public static FieldDocument createFieldDoc1 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-1");
        fd.addTV("tokens",
                "Frankenstein, treat my daughter well. She is the one that saved your master who you hold so dear.",

                "[(0-12)s:Frankenstein|_0$<i>0<i>12|"
                        + "<>:nn$<b>64<i>0<i>12<i>1<b>0<s>3|"
                        + "<>:np$<b>64<i>0<i>13<i>1<b>0<s>2|"
                        + "<>:s$<b>64<i>0<i>37<i>4<b>0<s>1|"
                        + "<:stanford/d:tag$<b>32<i>1<s>3<s>1<s>0]"

                        + "[(14-19)s:treat|_1$<i>14<i>19|"
                        + "<>:vb$<b>64<i>14<i>19<i>2<b>0<s>1|"
                        + "<>:vp$<b>64<i>14<i>36<i>4<b>0<s>2|"
                        + ">:stanford/d:tag$<b>32<i>0<s>1<s>3<s>0|"
                        + ">:stanford/d:tag$<b>32<i>3<s>1<s>1<s>0|"
                        + ">:stanford/d:tag$<b>32<i>4<s>1<s>1<s>0]"

                        + "[(20-22)s:my|_2$<i>20<i>22|"
                        + "<>:prp$<b>64<i>20<i>22<i>3<b>0<s>1|"
                        + "<>:np$<b>64<i>20<i>31<i>3<b>0<s>2]"

                        + "[(23-31)s:daughter|_3$<i>23<i>31|"
                        + "<>:nn$<b>64<i>23<i>31<i>4<b>0<s>1|"
                        + ">:stanford/d:tag$<b>32<i>2<s>1<s>1<s>0]"

                        + "[(32-36)s:well|_4$<i>32<i>36|"
                        + "<>:rb$<b>64<i>32<i>36<i>5<b>0<s>1|"
                        + "<>:advp$<b>64<i>32<i>36<i>4<b>0<s>2]");
        return fd;
    }


    public static FieldDocument createFieldDoc0 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-0");
        fd.addTV("tokens",
                "Frankenstein, treat my daughter well. She is the one that saved your master who you hold so dear.",

                "[(0-12)s:Frankenstein|_0$<i>0<i>12|"
                        + "<>:nn$<b>64<i>0<i>12<i>1<b>0<s>3|"
                        + "<>:np$<b>64<i>0<i>13<i>1<b>0<s>2|"
                        + "<>:s$<b>64<i>0<i>37<i>5<b>0<s>1|"
                        + "<:stanford/d:tag$<b>32<i>1<s>3<s>1<s>0]"

                        + "[(14-19)s:treat|_1$<i>14<i>19|"
                        + "<>:vb$<b>64<i>14<i>19<i>2<b>0<s>1|"
                        + "<>:vp$<b>64<i>14<i>36<i>5<b>0<s>2|"
                        + ">:stanford/d:tag$<b>32<i>0<s>1<s>2<s>0|"
                        + ">:stanford/d:tag$<b>32<i>3<s>1<s>1<s>0|"
                        + ">:stanford/d:tag$<b>32<i>4<s>1<s>1<s>0]"

                        + "[(20-22)s:my|_2$<i>20<i>22|"
                        + "<>:prp$<b>64<i>20<i>22<i>3<b>0<s>1|"
                        + "<>:np$<b>64<i>20<i>31<i>4<b>0<s>2]"

                        + "[(23-31)s:daughter|_3$<i>23<i>31|"
                        + "<>:nn$<b>64<i>23<i>31<i>4<b>0<s>1|"
                        + ">:stanford/d:tag$<b>32<i>2<s>21<s>1<s>0]"

                        + "[(32-36)s:well|_4$<i>32<i>36|"
                        + "<>:rb$<b>64<i>32<i>36<i>5<b>0<s>1|"
                        + "<>:advp$<b>64<i>32<i>36<i>5<b>0<s>2]"

                        + "[(38-41)s:She|_5$<i>38<i>41|"
                        + "<>:prp$<b>64<i>38<i>41<i>6<b>0<s>3|"
                        + "<>:np$<b>64<i>38<i>41<i>6<b>0<s>2|"
                        + "<>:s$<b>64<i>38<i>97<i>18<b>0<s>1]"

                        + "[(42-44)s:is|_6$<i>42<i>44|"
                        + "<>:vb$<b>64<i>42<i>44<i>7<b>0<s>1|"
                        + "<>:vp$<b>64<i>42<i>96<i>18<b>0<s>2]"

                        + "[(45-48)s:the|_7$<i>45<i>48|"
                        + "<>:dt$<b>64<i>45<i>48<i>8<b>0<s>3|"
                        + "<>:np$<b>64<i>45<i>52<i>9<b>0<s>2|"
                        + "<>:np$<b>64<i>45<i>96<i>18<b>0<s>1]"

                        + "[(49-52)s:one|_8$<i>49<i>52|"
                        + "<>:nn$<b>64<i>49<i>52<i>9<b>0<s>1|"
                        + ">:stanford/d:tag$<b>32<i>5<s>1<s>3<s>0|"
                        + ">:stanford/d:tag$<b>32<i>6<s>1<s>1<s>0|"
                        + ">:stanford/d:tag$<b>32<i>7<s>1<s>3<s>0|"
                        + ">:stanford/d:tag$<b>32<i>10<s>1<s>1<s>0]"

                        + "[(53-57)s:that|_9$<i>53<i>57|"
                        + "<>:rp$<b>64<i>53<i>57<i>10<b>0<s>1|"
                        + "<>:sb$<b>64<i>53<i>96<i>18<b>0<s>2]"

                        + "[(58-63)s:saved|_10$<i>58<i>63|"
                        + "<>:vb$<b>64<i>58<i>63<i>11<b>0<s>1|"
                        + "<>:s$<b>64<i>58<i>96<i>18<b>0<s>2|"
                        + "<>:vp$<b>64<i>58<i>96<i>18<b>0<s>3|"
                        + ">:stanford/d:tag$<b>32<i>9<s>1<s>1<s>0|"
                        + ">:stanford/d:tag$<b>32<i>12<s>1<s>1<s>0|"
                        + ">:stanford/d:tag$<b>32<i>15<s>1<s>33<s>0]"

                        + "[(64-68)s:your|_11$<i>64<i>68|"
                        + "<>:prp$<b>64<i>64<i>68<i>12<b>0<s>1|"
                        + "<>:np$<b>64<i>64<i>75<i>13<b>0<s>2]"

                        + "[(69-75)s:master|_12$<i>69<i>75|"
                        + "<>:nn$<b>64<i>69<i>75<i>13<b>0<s>1|"
                        + ">:stanford/d:tag$<b>32<i>11<s>1<s>1<s>0]"

                        + "[(76-79)s:who|_13$<i>76<i>79|"
                        + "<>:rp$<b>64<i>76<i>79<i>14<b>0<s>1|"
                        + "<>:sb$<b>64<i>76<i>96<i>18<b>0<s>2]"

                        + "[(80-83)s:you|_14$<i>80<i>83|"
                        + "<>:prp$<b>64<i>80<i>83<i>15<b>0<s>3|"
                        + "<>:np$<b>64<i>80<i>83<i>15<b>0<s>2|"
                        + "<>:s$<b>64<i>80<i>96<i>18<b>0<s>1]"

                        + "[(84-88)s:hold|_15$<i>84<i>88|"
                        + "<>:vb$<b>64<i>84<i>88<i>16<b>0<s>2|"
                        + "<>:vp$<b>64<i>84<i>96<i>18<b>0<s>1|"
                        + ">:stanford/d:tag$<b>32<i>13<s>2<s>1<s>0|"
                        + ">:stanford/d:tag$<b>32<i>14<s>2<s>3<s>0|"
                        + ">:stanford/d:tag$<b>32<i>17<s>2<s>1<s>0]"

                        + "[(89-91)s:so|_16$<i>89<i>91|"
                        + "<>:rb$<b>64<i>89<i>91<i>17<b>0<s>1|"
                        + "<>:adjp$<b>64<i>89<i>96<i>18<b>0<s>2]"

                        + "[(92-96)s:dear|_17$<i>92<i>96|"
                        + "<>:jj$<b>64<i>92<i>96<i>18<b>0<s>1|"
                        + ">:stanford/d:tag$<b>32<i>16<s>1<s>1<s>0]");

        return fd;
    }
}
