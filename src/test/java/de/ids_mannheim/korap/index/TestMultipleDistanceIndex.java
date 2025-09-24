package de.ids_mannheim.korap.index;

import static de.ids_mannheim.korap.TestSimple.*;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.spans.SpanMultiTermQueryWrapper;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.store.MMapDirectory;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.KrillCollection;
import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.KrillQuery;
import de.ids_mannheim.korap.query.DistanceConstraint;
import de.ids_mannheim.korap.query.SpanClassQuery;
import de.ids_mannheim.korap.query.SpanDistanceQuery;
import de.ids_mannheim.korap.query.SpanElementQuery;
import de.ids_mannheim.korap.query.SpanMultipleDistanceQuery;
import de.ids_mannheim.korap.query.SpanNextQuery;
import de.ids_mannheim.korap.query.wrap.SpanQueryWrapper;
import de.ids_mannheim.korap.response.Match;
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.util.QueryException;

@RunWith(JUnit4.class)
public class TestMultipleDistanceIndex {

    private KrillIndex ki;
    private Result kr;

    public SpanQuery createQuery (String x, String y,
            List<DistanceConstraint> constraints, boolean isOrdered) {

        SpanQuery sx = new SpanTermQuery(new Term("base", x));
        SpanQuery sy = new SpanTermQuery(new Term("base", y));

        return new SpanMultipleDistanceQuery(sx, sy, constraints, isOrdered,
                true);
    }


    public static DistanceConstraint createConstraint (String unit, int min, int max,
            boolean isOrdered, boolean exclusion) {
        return createConstraint("base", unit, min, max, isOrdered, exclusion);
    }


    public static DistanceConstraint createConstraint (String field, String unit,
            int min, int max, boolean isOrdered, boolean exclusion) {

        if (unit.equals("w")) {
            return new DistanceConstraint(min, max, isOrdered, exclusion);
        }
        return new DistanceConstraint(new SpanElementQuery(field, unit), min,
                max, isOrdered, exclusion);
    }


    private FieldDocument createFieldDoc0 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-0");
        fd.addTV("base", "text",
                "[(0-1)s:b|_1$<i>0<i>1|<>:s$<b>64<i>0<i>2<i>2<b>0|<>:p$<b>64<i>0<i>4<i>4<b>0]"
                        + "[(1-2)s:b|s:c|_2$<i>1<i>2]"
                        + "[(2-3)s:c|_3$<i>2<i>3|<>:s$<b>64<i>2<i>3<i>4<b>0]"
                        + "[(3-4)s:b|_4$<i>3<i>4]"
                        + "[(4-5)s:c|_5$<i>4<i>5|<>:s$<b>64<i>4<i>6<i>6<b>0|<>:p$<b>64<i>4<i>6<i>6<b>0]"
                        + "[(5-6)s:e|_6$<i>5<i>6]");
        return fd;
    }


    private FieldDocument createFieldDoc1 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-1");
        fd.addTV("base", "text",
                "[(0-1)s:c|_1$<i>0<i>1|<>:s$<b>64<i>0<i>2<i>2<b>0|<>:p$<b>64<i>0<i>4<i>4<b>0]"
                        + "[(1-2)s:c|s:e|_2$<i>1<i>2]"
                        + "[(2-3)s:e|_3$<i>2<i>3|<>:s$<b>64<i>2<i>3<i>4<b>0]"
                        + "[(3-4)s:c|_4$<i>3<i>4]"
                        + "[(4-5)s:e|_5$<i>4<i>5|<>:s$<b>64<i>4<i>6<i>6<b>0|<>:p$<b>64<i>4<i>6<i>6<b>0]"
                        + "[(5-6)s:c|_6$<i>5<i>6]");
        return fd;
    }


    private FieldDocument createFieldDoc2 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-2");
        fd.addTV("base", "text",
                "[(0-1)s:b|_1$<i>0<i>1|<>:s$<b>64<i>0<i>2<i>2<b>0|<>:p$<b>64<i>0<i>4<i>4<b>0]"
                        + "[(1-2)s:b|s:e|_2$<i>1<i>2]"
                        + "[(2-3)s:e|_3$<i>2<i>3|<>:s$<b>64<i>2<i>3<i>4<b>0]"
                        + "[(3-4)s:b|s:c|_4$<i>3<i>4]"
                        + "[(4-5)s:e|_5$<i>4<i>5|<>:s$<b>64<i>4<i>6<i>6<b>0|<>:p$<b>64<i>4<i>6<i>6<b>0]"
                        + "[(5-6)s:d|_6$<i>5<i>6]"
                        + "[(6-7)s:b|_7$<i>6<i>7|<>:s$<b>64<i>6<i>7<i>7<b>0|<>:p$<b>64<i>6<i>7<i>7<b>0]");
        return fd;
    }


    private FieldDocument createFieldDoc3 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-3");
        fd.addTV("base", "text",
                "[(0-1)s:b|_1$<i>0<i>1|<>:s$<b>64<i>0<i>2<i>2<b>0|<>:p$<b>64<i>0<i>4<i>4<b>0]"
                        + "[(1-2)s:b|s:c|_2$<i>1<i>2]"
                        + "[(2-3)s:c|_3$<i>2<i>3|<>:s$<b>64<i>2<i>3<i>5<b>0]"
                        + "[(3-4)s:b|_4$<i>3<i>4]" + "[(4-5)s:b|_5$<i>4<i>5]"
                        + "[(5-6)s:b|_6$<i>5<i>6]" + // gap
                        "[(6-7)s:c|_7$<i>6<i>7|<>:s$<b>64<i>6<i>7<i>7<b>0|<>:p$<b>64<i>6<i>7<i>7<b>0]");
        return fd;
    }


    private FieldDocument createFieldDoc4 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-4");
        fd.addTV("base", "text",
                "[(0-1)s:Zum|_1$<i>0<i>1|<>:s$<b>64<i>0<i>9<i>9<b>0]"
                        + "[(1-2)s:Begin|_2$<i>1<i>2]"
                        + "[(2-3)s:der|_3$<i>2<i>3]"
                        + "[(3-4)s:Veranstaltung|_4$<i>3<i>4]"
                        + "[(4-5)s:ruft|_5$<i>4<i>5]"
                        + "[(5-6)s:der|_6$<i>5<i>6]"
                        + "[(6-7)s:Moderator|_7$<i>6<i>7]"
                        + "[(7-8)s:die|_8$<i>7<i>8]"
                        + "[(8-9)s:Gäste|_9$<i>8<i>9]");
        return fd;
    }


    private FieldDocument createFieldDoc5 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-5");
        fd.addTV("tokens", "text",
                "[(0-1)s:Meine|_1$<i>0<i>1|<>:s$<b>64<i>0<i>9<i>9<b>0]"
                        + "[(1-2)l:Erfahrung|_2$<i>1<i>2]"
                        + "[(2-3)s:Meiner|_3$<i>2<i>3]"
                        + "[(3-4)l:Erfahrung|_4$<i>3<i>4]"
                        + "[(4-5)s:Mein|_5$<i>4<i>5]"
                        + "[(5-6)l:Erfahrung|_6$<i>5<i>6]"
                        + "[(6-7)s:Meinem|_7$<i>6<i>7]"
                        + "[(7-8)l:Erfahrung|_8$<i>7<i>8]"
                        + "[(8-9)s:Meinen|_9$<i>8<i>9]");
        return fd;
    }


    private FieldDocument createFieldDoc6 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-6");
        fd.addTV("tokens", "text",
                "[(0-1)s:Meine|_1$<i>0<i>1|<>:s$<b>64<i>0<i>5<i>5<b>0]"
                        + "[(1-2)s:Meiner|_2$<i>1<i>2]"
                        + "[(2-3)s:Mein|_3$<i>2<i>3]"
                        + "[(3-4)s:Meinem|_4$<i>3<i>4]"
                        + "[(4-5)s:Meinen|_5$<i>4<i>5]");
        return fd;
    }


    private FieldDocument createFieldDoc7 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-7");
        fd.addTV("tokens", "text",
                "[(0-1)l:Erfahrung|_1$<i>0<i>1|<>:s$<b>64<i>0<i>4<i>4<b>0]"
                        + "[(1-2)l:Erfahrung|_2$<i>1<i>2]"
                        + "[(2-3)l:Erfahrung|_3$<i>2<i>3]"
                        + "[(3-4)l:Erfahrung|_4$<i>3<i>4]");
        return fd;
    }


    private FieldDocument createFieldDoc8 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-8");
        fd.addTV("tokens", "text",
                "[(0-1)s:Meine|_1$<i>0<i>1|<>:s$<b>64<i>0<i>9<i>9<b>0]"
                        + "[(1-2)l:Erfahrung|_2$<i>1<i>2]"
                        + "[(2-3)s:Meiner|_3$<i>2<i>3]"
                        + "[(3-4)l:Erfahrung|_4$<i>3<i>4]"
                        + "[(4-5)s:Mein|_5$<i>4<i>5]"
                        + "[(5-6)l:Erfahrung|_6$<i>5<i>6]"
                        + "[(6-7)s:Meinem|_7$<i>6<i>7]"
                        + "[(7-8)l:Erfahrung|_8$<i>7<i>8]"
                        + "[(8-9)s:Meinen|_9$<i>8<i>9]");
        return fd;
    }


    @Test
    public void testQueryWithWildCard () throws IOException {
        // meine* /+w1:2,s0 &Erfahrung
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc5());
        ki.commit();

        // Check simple rewriting
        WildcardQuery wcquery =
                new WildcardQuery(new Term("tokens", "s:Meine*"));
        SpanMultiTermQueryWrapper<WildcardQuery> mtq =
                new SpanMultiTermQueryWrapper<WildcardQuery>(wcquery);

        assertEquals("tokens:s:Meine*", wcquery.toString());

        kr = ki.search(mtq, (short) 10);
        assertEquals(4, kr.getMatches().size());
        assertEquals(0, kr.getMatch(0).getStartPos());
        assertEquals(1, kr.getMatch(0).getEndPos());

        // Check rewriting in multidistance query
        SpanQuery sq = new SpanTermQuery(new Term("tokens", "l:Erfahrung"));
        kr = ki.search(sq, (short) 10);
        assertEquals(4, kr.getMatches().size());

        List<DistanceConstraint> constraints =
                new ArrayList<DistanceConstraint>();
        constraints.add(createConstraint("w", 1, 2, true, false));
        constraints.add(createConstraint("tokens", "s", 0, 0, true, false));

        SpanQuery mdsq =
                new SpanMultipleDistanceQuery(mtq, sq, constraints, true, true);
        assertEquals(
            "spanMultipleDistance(SpanMultiTermQueryWrapper(tokens:s:Meine*), "
                        + "tokens:l:Erfahrung, [(w[1:2], ordered, notExcluded), (s[0:0], "
                        + "ordered, notExcluded)])",
            mdsq.toString());

        kr = ki.search(mdsq, (short) 10);
        assertEquals(3, kr.getMatches().size());
        assertEquals(0, kr.getMatch(0).getStartPos());
        assertEquals(2, kr.getMatch(0).getEndPos());

        // Check skipping with multiple documents
        ki.addDoc(createFieldDoc6());
        ki.addDoc(createFieldDoc7());
        ki.addDoc(createFieldDoc8());
        ki.commit();
        kr = ki.search(mdsq, (short) 10);
        assertEquals(6, kr.getMatches().size());
    }

	
	@Test
    public void queryJSONwildcardNoFoundry () throws QueryException, IOException {
        // meine*
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc5());
        ki.commit();

        // treat merging gracefully
		SpanQueryWrapper sqw = getJsonQuery(
			getClass().getResource("/queries/bugs/cosmas_wildcards_missingfoundry.jsonld")
			.getFile());
		SpanQuery sq = sqw.toQuery();
		assertEquals("SpanMultiTermQueryWrapper(tokens:l:Erfahr*)", sq.toString());

		kr = ki.search(sq, (short) 10);
        assertEquals(4, kr.getMatches().size());
        assertEquals(1, kr.getMatch(0).getStartPos());
        assertEquals(2, kr.getMatch(0).getEndPos());
    };
	

    @Test
    public void testUnorderedTokenDistance () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc4());
        ki.commit();

        List<DistanceConstraint> constraints =
                new ArrayList<DistanceConstraint>();
        constraints.add(createConstraint("w", 0, 5, true, false));
        constraints.add(createConstraint("s", 0, 0, true, false));

        SpanQuery mdq;
        mdq = createQuery("s:Begin", "s:Moderator", constraints, false);
        kr = ki.search(mdq, (short) 10);
        assertEquals(1, kr.getMatch(0).getStartPos());
        assertEquals(7, kr.getMatch(0).getEndPos());

        SpanQuery sq = new SpanDistanceQuery(mdq,
                new SpanTermQuery(new Term("base", "s:ruft")),
                new DistanceConstraint(0, 0, false, false), true);

        kr = ki.search(sq, (short) 10);
        assertEquals(1, kr.getMatch(0).getStartPos());
        assertEquals(7, kr.getMatch(0).getEndPos());
    }


    /**
     * Unordered, same sentence
     */
    @Test
    public void testCase1 () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc0());
        ki.commit();

        List<DistanceConstraint> constraints =
                new ArrayList<DistanceConstraint>();
        constraints.add(createConstraint("w", 0, 2, false, false));
        constraints.add(createConstraint("s", 0, 0, false, false));

        SpanQuery mdq;
        mdq = createQuery("s:b", "s:c", constraints, false);
        kr = ki.search(mdq, (short) 10);

        assertEquals((long) 3, kr.getTotalResults());
        assertEquals(0, kr.getMatch(0).getStartPos());
        assertEquals(2, kr.getMatch(0).getEndPos());
        assertEquals(1, kr.getMatch(1).getStartPos());
        assertEquals(2, kr.getMatch(1).getEndPos());
        assertEquals(2, kr.getMatch(2).getStartPos());
        assertEquals(4, kr.getMatch(2).getEndPos());
    }


    /**
     * Ordered
     * Unordered
     * Two constraints
     * Three constraints
     */
    @Test
    public void testCase2 () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc0());
        ki.commit();

        // Ordered - two constraints
        List<DistanceConstraint> constraints =
                new ArrayList<DistanceConstraint>();
        constraints.add(createConstraint("w", 0, 2, true, false));
        constraints.add(createConstraint("s", 1, 1, true, false));

        SpanQuery mdq;
        mdq = createQuery("s:b", "s:c", constraints, true);
        kr = ki.search(mdq, (short) 10);
        assertEquals((long) 3, kr.getTotalResults());
        assertEquals(0, kr.getMatch(0).getStartPos());
        assertEquals(3, kr.getMatch(0).getEndPos());
        assertEquals(1, kr.getMatch(1).getStartPos());
        assertEquals(3, kr.getMatch(1).getEndPos());
        assertEquals(3, kr.getMatch(2).getStartPos());
        assertEquals(5, kr.getMatch(2).getEndPos());

        // Three constraints
        constraints.add(createConstraint("p", 0, 0, true, false));
        mdq = createQuery("s:b", "s:c", constraints, true);
        kr = ki.search(mdq, (short) 10);
        assertEquals((long) 2, kr.getTotalResults());


        // Unordered - two constraints
        constraints.clear();
        constraints.add(createConstraint("w", 0, 2, false, false));
        constraints.add(createConstraint("s", 1, 1, false, false));

        mdq = createQuery("s:c", "s:b", constraints, false);
        kr = ki.search(mdq, (short) 10);
        assertEquals((long) 4, kr.getTotalResults());
        assertEquals(1, kr.getMatch(2).getStartPos());
        assertEquals(4, kr.getMatch(2).getEndPos());

        // Three constraints
        constraints.add(createConstraint("p", 0, 0, false, false));
        mdq = createQuery("s:b", "s:c", constraints, false);
        kr = ki.search(mdq, (short) 10);
        assertEquals((long) 3, kr.getTotalResults());

    }


    /**
     * Multiple documents
     * Ensure same doc (inner term span)
     */
    @Test
    public void testCase3 () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc0());
        ki.addDoc(createFieldDoc1());
        ki.addDoc(createFieldDoc2());
        ki.commit();

        List<DistanceConstraint> constraints =
                new ArrayList<DistanceConstraint>();
        constraints.add(createConstraint("w", 1, 2, false, false));
        constraints.add(createConstraint("s", 1, 2, false, false));

        SpanQuery mdq;
        mdq = createQuery("s:b", "s:e", constraints, false);
        kr = ki.search(mdq, (short) 10);

        assertEquals((long) 5, kr.getTotalResults());
        assertEquals(3, kr.getMatch(0).getStartPos());
        assertEquals(6, kr.getMatch(0).getEndPos());
        assertEquals(2, kr.getMatch(1).getLocalDocID());
        assertEquals(1, kr.getMatch(2).getStartPos());
        assertEquals(4, kr.getMatch(2).getEndPos());
        assertEquals(3, kr.getMatch(3).getStartPos());
        assertEquals(5, kr.getMatch(3).getEndPos());
        assertEquals(4, kr.getMatch(4).getStartPos());
        assertEquals(7, kr.getMatch(4).getEndPos());

        //      System.out.print(kr.getTotalResults()+"\n");
        //      for (int i=0; i< kr.getTotalResults(); i++){
        //          System.out.println(
        //              kr.match(i).getLocalDocID()+" "+
        //              kr.match(i).startPos + " " +
        //              kr.match(i).endPos
        //          );
        //      }

    }


    /**
     * Skip to
     */
    @Test
    public void testCase4 () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc0());
        ki.addDoc(createFieldDoc3());
        ki.addDoc(createFieldDoc1());
        ki.addDoc(createFieldDoc2());
        ki.commit();

        List<DistanceConstraint> constraints =
                new ArrayList<DistanceConstraint>();
        constraints.add(createConstraint("w", 1, 2, false, false));
        constraints.add(createConstraint("s", 1, 2, false, false));

        SpanQuery mdq;
        mdq = createQuery("s:b", "s:c", constraints, false);

        SpanQuery sq = new SpanNextQuery(mdq,
                new SpanTermQuery(new Term("base", "s:e")));
        kr = ki.search(sq, (short) 10);

        assertEquals((long) 2, kr.getTotalResults());
        assertEquals(3, kr.getMatch(0).getStartPos());
        assertEquals(6, kr.getMatch(0).getEndPos());
        assertEquals(3, kr.getMatch(1).getLocalDocID());
        assertEquals(1, kr.getMatch(1).getStartPos());
        assertEquals(5, kr.getMatch(1).getEndPos());

    }


    /**
     * Same tokens: unordered yields twice the same results as ordered
     */
    @Test
    public void testCase5 () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc0());
        ki.addDoc(createFieldDoc1());
        ki.commit();

        // ordered
        List<DistanceConstraint> constraints =
                new ArrayList<DistanceConstraint>();
        constraints.add(createConstraint("w", 1, 2, true, false));
        constraints.add(createConstraint("s", 1, 2, true, false));

        SpanQuery mdq;
        mdq = createQuery("s:c", "s:c", constraints, false);
        kr = ki.search(mdq, (short) 10);

        assertEquals((long) 4, kr.getTotalResults());
        assertEquals(1, kr.getMatch(0).getStartPos());
        assertEquals(3, kr.getMatch(0).getEndPos());
        assertEquals(2, kr.getMatch(1).getStartPos());
        assertEquals(5, kr.getMatch(1).getEndPos());
        assertEquals(1, kr.getMatch(2).getLocalDocID());
        assertEquals(1, kr.getMatch(2).getStartPos());
        assertEquals(4, kr.getMatch(2).getEndPos());
        assertEquals(3, kr.getMatch(3).getStartPos());
        assertEquals(6, kr.getMatch(3).getEndPos());

        //unordered
        constraints = new ArrayList<DistanceConstraint>();
        constraints.add(createConstraint("w", 1, 2, false, false));
        constraints.add(createConstraint("s", 1, 2, false, false));

        mdq = createQuery("s:c", "s:c", constraints, false);
        kr = ki.search(mdq, (short) 10);
        assertEquals((long) 8, kr.getTotalResults());

    }


    /**
     * Exclusion
     * Gaps
     */
    @Test
    public void testCase6 () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc3());
        ki.commit();

        // First constraint - token exclusion
        SpanQuery sx = new SpanTermQuery(new Term("base", "s:b"));
        SpanQuery sy = new SpanTermQuery(new Term("base", "s:c"));

        DistanceConstraint dc1 = createConstraint("w", 0, 1, false, true);
        SpanDistanceQuery sq = new SpanDistanceQuery(sx, sy, dc1, true);

        kr = ki.search(sq, (short) 10);
        assertEquals((long) 1, kr.getTotalResults());
        // 4-5

        // Second constraint - element distance
        DistanceConstraint dc2 = createConstraint("s", 1, 1, false, false);
        sq = new SpanDistanceQuery(sx, sy, dc2, true);
        kr = ki.search(sq, (short) 10);
        // 0-3, 1-3, 1-4, 1-5, 3-7, 4-7
        assertEquals((long) 6, kr.getTotalResults());


        List<DistanceConstraint> constraints =
                new ArrayList<DistanceConstraint>();
        constraints.add(dc1);
        constraints.add(dc2);

        SpanQuery mdq;
        mdq = createQuery("s:b", "s:c", constraints, false);
        kr = ki.search(mdq, (short) 10);

        assertEquals((long) 2, kr.getTotalResults());
        assertEquals(1, kr.getMatch(0).getStartPos());
        assertEquals(5, kr.getMatch(0).getEndPos());
        assertEquals(4, kr.getMatch(1).getStartPos());
        assertEquals(7, kr.getMatch(1).getEndPos());
    }


    /**
     * Exclusion, multiple documents
     */
    @Test
    public void testCase7 () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc2());
        ki.commit();

        SpanQuery sx = new SpanTermQuery(new Term("base", "s:b"));
        SpanQuery sy = new SpanTermQuery(new Term("base", "s:c"));
        // Second constraint
        SpanDistanceQuery sq = new SpanDistanceQuery(sx, sy,
                createConstraint("s", 0, 0, false, true), true);
        kr = ki.search(sq, (short) 10);
        assertEquals((long) 3, kr.getTotalResults());
        // 0-1, 1-2, 6-7

        // Exclusion within the same sentence
        List<DistanceConstraint> constraints =
                new ArrayList<DistanceConstraint>();
        constraints.add(createConstraint("w", 0, 2, false, true));
        constraints.add(createConstraint("s", 0, 0, false, true));

        SpanQuery mdq;
        mdq = createQuery("s:b", "s:c", constraints, false);
        kr = ki.search(mdq, (short) 10);
        assertEquals((long) 2, kr.getTotalResults());
        assertEquals(0, kr.getMatch(0).getStartPos());
        assertEquals(1, kr.getMatch(0).getEndPos());
        assertEquals(6, kr.getMatch(1).getStartPos());
        assertEquals(7, kr.getMatch(1).getEndPos());


        // Third constraint
        sq = new SpanDistanceQuery(sx, sy,
                createConstraint("p", 0, 0, false, true), true);
        kr = ki.search(sq, (short) 10);
        assertEquals((long) 1, kr.getTotalResults());
        // 6-7

        constraints.add(createConstraint("p", 0, 0, false, true));
        mdq = createQuery("s:b", "s:c", constraints, false);
        kr = ki.search(mdq, (short) 10);

        assertEquals((long) 1, kr.getTotalResults());
        assertEquals(6, kr.getMatch(0).getStartPos());
        assertEquals(7, kr.getMatch(0).getEndPos());

    }
}
