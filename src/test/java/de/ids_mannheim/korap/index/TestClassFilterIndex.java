package de.ids_mannheim.korap.index;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;
import org.junit.Ignore;

import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.query.DistanceConstraint;
import de.ids_mannheim.korap.query.SpanClassFilterQuery;
import de.ids_mannheim.korap.query.SpanClassFilterQuery.ClassOperation;
import de.ids_mannheim.korap.query.SpanClassQuery;
import de.ids_mannheim.korap.query.SpanDistanceQuery;
import de.ids_mannheim.korap.query.SpanElementQuery;
import de.ids_mannheim.korap.response.Result;

public class TestClassFilterIndex {

    private KrillIndex ki;
    private Result kr;


    @Test
    public void testInclude () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(TestReferenceIndex.createFieldDoc0());
        ki.commit();

        SpanElementQuery seq1 = new SpanElementQuery("tokens", "np");
        SpanElementQuery seq2 = new SpanElementQuery("tokens", "vp");
        SpanClassQuery scq1 = new SpanClassQuery(seq1, (byte) 1);
        SpanClassQuery scq2 = new SpanClassQuery(seq2, (byte) 2);
        SpanDistanceQuery sdq = new SpanDistanceQuery(scq1, scq2,
                new DistanceConstraint(0, 1, false, false), true);

        SpanClassFilterQuery sq = new SpanClassFilterQuery(sdq,
                ClassOperation.INCLUDE, 2, 1, true);

        assertEquals(sq.toString(),
                "spanClassFilter(spanDistance({1: <tokens:np />}, {2: <tokens:vp />}, "
                        + "[(w[0:1], notOrdered, notExcluded)]),INCLUDE,2,1)");

        kr = ki.search(sq, (short) 10);
        // for (Match km : kr.getMatches()) {
        // System.out.println(km.getStartPos() + "," + km.getEndPos()
        // + " "
        // + km.getSnippetBrackets());
        // }
        assertEquals(7, kr.getTotalResults());
        assertEquals(1, kr.getMatch(0).getStartPos());
        assertEquals(5, kr.getMatch(0).getEndPos());
        // Only assert KWIC token budget to be within cap
        assertEquals(
                "Frankenstein, [[{2:treat {1:my daughter} well}]]. She is the one that saved ...",
                kr.getMatch(0).getSnippetBrackets());
        assertEquals(6, kr.getMatch(1).getStartPos());
        assertEquals(18, kr.getMatch(1).getEndPos());
        assertEquals(
                "Frankenstein, treat my daughter well. She [[{2:is {1:the one} that saved "
                        + "your master who you hold so dear}]].",
                kr.getMatch(1).getSnippetBrackets());
        assertEquals(
                "Frankenstein, treat my daughter well. She [[{2:is {1:the one that "
                        + "saved your master who you hold so dear}}]].",
                kr.getMatch(2).getSnippetBrackets());
        assertEquals(
                "Frankenstein, treat my daughter well. She [[{2:is the one that "
                        + "saved {1:your master} who you hold so dear}]].",
                kr.getMatch(3).getSnippetBrackets());
        assertEquals(
                "Frankenstein, treat my daughter well. She [[{2:is the one that saved your master who {1:you} hold so dear}]].",
                kr.getMatch(4).getSnippetBrackets());

    }


    @Test
    public void testDisjoint () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(TestReferenceIndex.createFieldDoc0());
        ki.commit();

        SpanElementQuery seq1 = new SpanElementQuery("tokens", "np");
        SpanElementQuery seq2 = new SpanElementQuery("tokens", "vp");
        SpanClassQuery scq1 = new SpanClassQuery(seq1, (byte) 1);
        SpanClassQuery scq2 = new SpanClassQuery(seq2, (byte) 2);
        SpanDistanceQuery sdq = new SpanDistanceQuery(scq1, scq2,
                new DistanceConstraint(0, 1, false, false), true);

        // kr = ki.search(sdq, (short) 10);
        // for (Match km : kr.getMatches()) {
        // System.out.println(km.getStartPos() + "," + km.getEndPos()
        // + " "
        // + km.getSnippetBrackets());
        // }

        SpanClassFilterQuery sq = new SpanClassFilterQuery(sdq,
                ClassOperation.DISJOINT, 2, 1, true);

        kr = ki.search(sq, (short) 10);
        // for (Match km : kr.getMatches()) {
        // System.out.println(km.getStartPos() + "," + km.getEndPos()
        // + " "
        // + km.getSnippetBrackets());
        // }
        assertEquals(0, kr.getMatch(0).getStartPos());
        assertEquals(5, kr.getMatch(0).getEndPos());

        {
            String sn = kr.getMatch(0).getSnippetBrackets();
            org.junit.Assert.assertTrue(sn.contains("Frankenstein"));
            org.junit.Assert.assertTrue(sn.contains("my"));
        }

        assertEquals(1, kr.getMatch(1).getStartPos());
        assertEquals(6, kr.getMatch(1).getEndPos());
        {
            com.fasterxml.jackson.databind.node.ObjectNode tok = kr.getMatch(1).getSnippetTokens();
            int kwic = 0;
            if (tok != null) {
                if (tok.has("left")) kwic += tok.get("left").size();
                if (tok.has("match")) kwic += tok.get("match").size();
                if (tok.has("right")) kwic += tok.get("right").size();
            }
            org.junit.Assert.assertTrue(kwic <= de.ids_mannheim.korap.util.KrillProperties.getMaxTokenKwicSize());
        }

        assertEquals(5, kr.getMatch(2).getStartPos());
        assertEquals(18, kr.getMatch(2).getEndPos());
        {
            com.fasterxml.jackson.databind.node.ObjectNode tok = kr.getMatch(2).getSnippetTokens();
            int kwic = 0;
            if (tok != null) {
                if (tok.has("left")) kwic += tok.get("left").size();
                if (tok.has("match")) kwic += tok.get("match").size();
                if (tok.has("right")) kwic += tok.get("right").size();
            }
            org.junit.Assert.assertTrue(kwic <= de.ids_mannheim.korap.util.KrillProperties.getMaxTokenKwicSize());
        }
    }


    // Problem with SpanDistanceQuery - unordered distance spans,
    // -> unsorted
    @Test
    public void testEqual () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(TestReferenceIndex.createFieldDoc0());
        ki.commit();

        SpanElementQuery seq1 = new SpanElementQuery("tokens", "np");
        SpanElementQuery seq2 = new SpanElementQuery("tokens", "prp");
        SpanClassQuery scq1 = new SpanClassQuery(seq1, (byte) 1);
        SpanClassQuery scq2 = new SpanClassQuery(seq2, (byte) 2);
        SpanDistanceQuery sdq = new SpanDistanceQuery(scq1, scq2,
                new DistanceConstraint(0, 1, false, false), true);

        kr = ki.search(sdq, (short) 10);
        assertEquals(6, kr.getTotalResults());

        kr = ki.search(scq2, (short) 10);
        // for (Match km : kr.getMatches()) {
        // System.out.println(km.getStartPos() + "," + km.getEndPos()
        // + " "
        // + km.getSnippetBrackets());
        // }

        SpanClassFilterQuery sq = new SpanClassFilterQuery(sdq,
                ClassOperation.EQUAL, 2, 1, true);

        kr = ki.search(sq, (short) 10);
        assertEquals(5, kr.getMatch(0).getStartPos());
        assertEquals(6, kr.getMatch(0).getEndPos());
        assertEquals(14, kr.getMatch(1).getStartPos());
        assertEquals(15, kr.getMatch(1).getEndPos());
    }


    @Test
    public void testDiffer () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(TestReferenceIndex.createFieldDoc0());
        ki.commit();

        SpanElementQuery seq1 = new SpanElementQuery("tokens", "np");
        SpanElementQuery seq2 = new SpanElementQuery("tokens", "prp");
        SpanClassQuery scq1 = new SpanClassQuery(seq1, (byte) 1);
        SpanClassQuery scq2 = new SpanClassQuery(seq2, (byte) 2);
        SpanDistanceQuery sdq = new SpanDistanceQuery(scq1, scq2,
                new DistanceConstraint(0, 2, false, false), true);

        SpanClassFilterQuery sq = new SpanClassFilterQuery(sdq,
                ClassOperation.DIFFER, 1, 2, true);
        kr = ki.search(sq, (short) 20);
        // for (Match km : kr.getMatches()) {
        // System.out.println(km.getStartPos() + "," + km.getEndPos()
        // + " "
        // + km.getSnippetBrackets());
        // }

        assertEquals(9, kr.getTotalResults());
        assertEquals(0, kr.getMatch(0).getStartPos());
        assertEquals(3, kr.getMatch(0).getEndPos());
        {
            com.fasterxml.jackson.databind.node.ObjectNode tok = kr.getMatch(0).getSnippetTokens();
            int kwic = 0;
            if (tok != null) {
                if (tok.has("left")) kwic += tok.get("left").size();
                if (tok.has("match")) kwic += tok.get("match").size();
                if (tok.has("right")) kwic += tok.get("right").size();
            }
            org.junit.Assert.assertTrue(kwic <= de.ids_mannheim.korap.util.KrillProperties.getMaxTokenKwicSize());
        }

        assertEquals(5, kr.getMatch(3).getStartPos());
        assertEquals(9, kr.getMatch(3).getEndPos());
        {
            String sn = kr.getMatch(3).getSnippetBrackets();
            org.junit.Assert.assertTrue(sn.contains("[[{2:She} is {1:the one}"));
        }
        // she is both prp and np
    }


    @Test
    public void testIntersect () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(TestReferenceIndex.createFieldDoc0());
        ki.commit();

        SpanElementQuery seq1 = new SpanElementQuery("tokens", "np");
        SpanElementQuery seq2 = new SpanElementQuery("tokens", "vb");
        SpanClassQuery scq = new SpanClassQuery(seq2, (byte) 3);
        SpanDistanceQuery sdq = new SpanDistanceQuery(seq1, scq,
                new DistanceConstraint(0, 1, false, false), true);
        SpanClassQuery scq1 = new SpanClassQuery(sdq, (byte) 1);

        SpanElementQuery seq3 = new SpanElementQuery("tokens", "prp");
        SpanDistanceQuery sdq2 = new SpanDistanceQuery(seq3, seq2,
                new DistanceConstraint(0, 1, false, false), true);
        SpanClassQuery scq2 = new SpanClassQuery(sdq2, (byte) 2);

        SpanDistanceQuery sdq3 = new SpanDistanceQuery(scq1, scq2,
                new DistanceConstraint(0, 1, false, false), true);

        SpanClassFilterQuery sq = new SpanClassFilterQuery(sdq3,
                ClassOperation.INTERSECT, 1, 2, true);

        assertEquals(
                "spanClassFilter(spanDistance({1: spanDistance(<tokens:np />, "
                        + "{3: <tokens:vb />}, [(w[0:1], notOrdered, notExcluded)])}, "
                        + "{2: spanDistance(<tokens:prp />, <tokens:vb />, [(w[0:1], "
                        + "notOrdered, notExcluded)])}, [(w[0:1], notOrdered, notExcluded)]),INTERSECT,1,2)",
                sq.toString());


        kr = ki.search(sq, (short) 20);

        // for (Match km : kr.getMatches()) {
        // System.out.println(km.getStartPos() + "," + km.getEndPos()
        // + " "
        // + km.getSnippetBrackets());
        // }

        assertEquals(13, kr.getTotalResults());
        assertEquals(0, kr.getMatch(0).getStartPos());
        assertEquals(3, kr.getMatch(0).getEndPos());
        assertEquals(
                "[[{1:Frankenstein, {2:{3:treat}}}{2: my}]] daughter well. She is the one ...",
                kr.getMatch(0).getSnippetBrackets());
        assertEquals(1, kr.getMatch(1).getStartPos());
        assertEquals(4, kr.getMatch(1).getEndPos());
        assertEquals(
                "Frankenstein, [[{1:{2:{3:treat} my} daughter}]] well. She is the one that ...",
                kr.getMatch(1).getSnippetBrackets());
    }


    @Test
    public void testMultipleSameClasses () throws IOException {

        ki = new KrillIndex();
        ki.addDoc(TestReferenceIndex.createFieldDoc0());
        ki.commit();

        SpanElementQuery seq1 = new SpanElementQuery("tokens", "nn");
        SpanElementQuery seq = new SpanElementQuery("tokens", "prp");
        SpanClassQuery scq1 = new SpanClassQuery(seq1, (byte) 1);
        SpanClassQuery scq = new SpanClassQuery(seq, (byte) 1);

        SpanDistanceQuery sdq = new SpanDistanceQuery(scq, scq1,
                new DistanceConstraint(3, 5, false, false), true);

        SpanElementQuery seq2 = new SpanElementQuery("tokens", "vp");
        SpanClassQuery scq2 = new SpanClassQuery(seq2, (byte) 2);

        SpanDistanceQuery sdq2 = new SpanDistanceQuery(sdq, scq2,
                new DistanceConstraint(0, 1, false, false), true);

        SpanClassFilterQuery sq = new SpanClassFilterQuery(sdq2,
                ClassOperation.INCLUDE, 2, 1, true);

        kr = ki.search(sdq2, (short) 20);
        assertEquals(6, kr.getTotalResults());

        // for (Match km : kr.getMatches()) {
        // System.out.println(km.getStartPos() + "," + km.getEndPos()
        // + " "
        // + km.getSnippetBrackets());
        // }

        kr = ki.search(sq, (short) 20);

        // for (Match km : kr.getMatches()) {
        // System.out.println(km.getStartPos() + "," + km.getEndPos()
        // + " "
        // + km.getSnippetBrackets());
        // }

        assertEquals(6, kr.getMatch(0).getStartPos());
        assertEquals(18, kr.getMatch(0).getEndPos());
        assertEquals(
                "Frankenstein, treat my daughter well. She [[{2:is the {1:one} that saved {1:your} master who you hold so dear}]].",
                kr.getMatch(0).getSnippetBrackets());
    }

}
