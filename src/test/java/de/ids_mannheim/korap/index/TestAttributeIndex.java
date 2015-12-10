package de.ids_mannheim.korap.index;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.junit.Test;

import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.query.SpanAttributeQuery;
import de.ids_mannheim.korap.query.SpanElementQuery;
import de.ids_mannheim.korap.query.SpanNextQuery;
import de.ids_mannheim.korap.query.SpanWithAttributeQuery;
import de.ids_mannheim.korap.response.Result;

public class TestAttributeIndex {

    private KrillIndex ki = new KrillIndex();
    private Result kr;
    private FieldDocument fd;


    public TestAttributeIndex () throws IOException {
        ki = new KrillIndex();
    }


    private FieldDocument createFieldDoc0 () {
        fd = new FieldDocument();
        fd.addString("ID", "doc-0");
        fd.addTV(
                "base",
                "bcbabd",
				"[(0-1)s:a|_1#0-1|"						
				+ "<>:div$<b>65<i>0<i>2<i>2<b>0<s>2|"
				+ "<>:div$<b>65<i>0<i>3<i>3<b>0<s>1|"
				+ "<>:s$<b>65<i>0<i>5<i>5<b>0<s>3|"
				+ "@:class=header$<i>3<s>1|@:class=header$<i>2<s>2]"

				+ "[(1-2)s:e|_2#1-2|"
				+ "<>:a$<b>65<i>1<i>2<i>2<b>0<s>1|@:class=header$<i>2<s>1]"
  
				+ "[(2-3)s:e|_3#2-3|"
				+ "<>:div$<b>65<i>2<i>5<i>5<b>0<s>1|@:class=time$<i>5<s>1]"
  
				+ "[(3-4)s:a|_4#3-4|"
				+ "<>:div$<b>65<i>3<i>5<i>5<b>0<s>1|@:class=header$<i>5<s>1]"
  
				+ "[(4-5)s:b|_5#4-5|"
				+ "<>:div$<b>65<i>4<i>5<i>5<b>0<s>1|"
				+ "<>:a$<b>65<i>4<i>5<i>5<b>0<s>2|@:class=header$<i>5<s>2]"
		  
				+ "[(5-6)s:d|_6#5-6|"
				+ "<>:s$<b>65<i>5<i>6<i>6<b>0<s>1|"
				+ "<>:div$<b>65<i>5<i>6<i>6<b>0<s>2|@:class=header$<i>6<s>1]"
  
				+ "[(6-7)s:d|_7#6-7|"
				+ "<>:div$<b>65<i>6<i>7<i>7<b>0<s>1"
				+ "<>:s$<b>65<i>6<i>7<i>7<b>0<s>2|"
				+ "|@:class=header$<i>7<s>1|@:class=header$<i>7<s>2]");

        return fd;
    }


    private FieldDocument createFieldDoc1 () {
        fd = new FieldDocument();
        fd.addString("ID", "doc-1");
        fd.addTV(
                "base",
                "bcbabd",
				"[(0-1)s:b|_1#0-1|"
						+ "<>:div$<b>65<i>0<i>3<i>3<b>0<s>1|@:class=header$<i>3<s>1|@:class=title$<i>3<s>1|@:class=book$<i>3<s>1]"
						+ "<>:s<b>65<i>0<i>5<i>5<b>0<s>2|"
						+ "[(1-2)s:c|_2#1-2|"
						+ "<>:div$<b>65<i>1<i>2<i>2<b>0<s>1|@:class=header$<i>2<s>1|@:class=title$<i>2<s>1]"
						+ "[(2-3)s:b|_3#2-3|"
						+ "<>:div$<b>65<i>2<i>3<i>5<b>0<s>1|@:class=book$<i>5<s>1]"
						+ "[(3-4)s:a|_4#3-4|"
						+ "<>:div$<b>65<i>3<i>5<i>5<b>0<s>1|@:class=title$<i>5<s>1]"
						+ "[(4-5)s:b|_5#4-5|"
						+ "<>:div$<b>65<i>4<i>5<i>5<b>0<s>1|@:class=header$<i>5<s>1|@:class=book$<i>5<s>1|@:class=title$<i>5<s>1]"
						+ "[(5-6)s:d|_6#5-6|"						
						+ "<>:div$<b>65<i>5<i>6<i>6<b>0<s>1|@:class=header$<i>6<s>1]"
						+ "<>:s$<b>65<i>5<i>6<i>6<b>0<s>2|"
						+ "[(6-7)s:d|_7#6-7|"
						+ "<>:div$<b>65<i>6<i>7<i>7<b>0<s>1|"
						+ "<>:s$<b>65<i>6<i>7<i>7<b>0<s>2|"
						+ "@:class=header$<i>7<s>1|@:class=title$<i>7<s>1]");

        return fd;
    }


    private FieldDocument createFieldDoc2 () {
        fd = new FieldDocument();
        fd.addString("ID", "doc-1");
        fd.addTV(
                "base",
                "bcbabd",
				"[(0-1)s:b|_1#0-1|"
						+ "<>:s$<b>65<i>0<i>5<i>5<b>0<s>1|"
						+ "<>:div$<b>65<i>0<i>3<i>3<b>0<s>2|@:class=header$<i>3<s>2|@:class=book$<i>5<s>1|@:class=book$<i>3<s>2]"
						+ "[(1-2)s:e|_2#1-2|"
						+ "<>:div$<b>65<i>1<i>2<i>2<b>0<s>1|"
						+ "<>:a$<b>65<i>1<i>2<i>2<b>0<s>2|@:class=book$<i>2<s>2|@:class=header$<i>2<s>1]"
						+ "[(2-3)s:b|_3#2-3|"
						+ "<>:div$<b>65<i>2<i>3<i>5<b>0<s>1|"
						+ "<>:a$<b>65<i>1<i>2<i>2<b>0<s>2|@:class=header$<i>2<s>2|@:class=book$<i>5<s>1]"
						+ "[(3-4)s:a|_4#3-4|"
						+ "<>:div$<b>65<i>3<i>5<i>5<b>0<s>1|@:class=title$<i>5<s>1]"
						+ "[(4-5)s:b|_5#4-5|"
						+ "<>:div$<b>65<i>4<i>5<i>5<b>0<s>1|@:class=header$<i>5<s>1|@:class=book$<i>5<s>1]"
						+ "[(5-6)s:d|_6#5-6|"
						+ "<>:s$<b>65<i>5<i>6<i>6<b>0<s>1|"
						+ "<>:div$<b>65<i>5<i>6<i>6<b>0<s>1|@:class=header$<i>6<s>1]"
						+ "[(6-7)s:d|_7#6-7|"
						+ "<>:div$<b>65<i>6<i>7<i>7<b>0<s>1|"
						+ "<>:s$<b>65<i>6<i>7<i>7<b>0<s>2|"
						+ "@:class=header$<i>7<s>1|@:class=book$<i>7<s>2]");

        return fd;
    }


    /**
     * Test matching elementRef
     * 
     * @throws IOException
     * */
    @Test
    public void testCase1 () throws IOException {
        ki.addDoc(createFieldDoc0());
        ki.commit();

        SpanAttributeQuery saq = new SpanAttributeQuery(new SpanTermQuery(
                new Term("base", "@:class=header")), true);

		SpanElementQuery seq = new SpanElementQuery("base", "div");

		// div with @class=header
		SpanQuery sq = new SpanWithAttributeQuery(seq, saq, true);

        kr = ki.search(sq, (short) 10);

		// for (int i = 0; i < kr.getTotalResults(); i++) {
		// System.out.println(kr.getMatch(i).getLocalDocID() + " "
		// + kr.getMatch(i).startPos + " " + kr.getMatch(i).endPos);
		// }
		//
		assertEquals((long) 4, kr.getTotalResults());
		assertEquals(0, kr.getMatch(0).getStartPos());
		assertEquals(2, kr.getMatch(0).getEndPos());
		assertEquals(0, kr.getMatch(1).getStartPos());
		assertEquals(3, kr.getMatch(1).getEndPos());
		assertEquals(3, kr.getMatch(2).getStartPos());
		assertEquals(5, kr.getMatch(2).getEndPos());
		assertEquals(6, kr.getMatch(3).getStartPos());
		assertEquals(7, kr.getMatch(3).getEndPos());
    }


    /**
     * Test multiple attributes and negation
     * 
     * @throws IOException
     * */
    @Test
    public void testCase2 () throws IOException {
        ki.addDoc(createFieldDoc1());
        ki.commit();
        // header and title
        List<SpanQuery> sql = new ArrayList<>();
        sql.add(new SpanAttributeQuery(new SpanTermQuery(new Term("base",
                "@:class=header")), true));
        sql.add(new SpanAttributeQuery(new SpanTermQuery(new Term("base",
                "@:class=title")), true));

        SpanQuery sq = new SpanWithAttributeQuery(new SpanElementQuery("base",
                "div"), sql, true);

        kr = ki.search(sq, (short) 10);

        assertEquals((long) 4, kr.getTotalResults());
        assertEquals(0, kr.getMatch(0).getStartPos());
        assertEquals(3, kr.getMatch(0).getEndPos());
        assertEquals(1, kr.getMatch(1).getStartPos());
        assertEquals(2, kr.getMatch(1).getEndPos());
        assertEquals(4, kr.getMatch(2).getStartPos());
        assertEquals(5, kr.getMatch(2).getEndPos());
        assertEquals(6, kr.getMatch(3).getStartPos());
        assertEquals(7, kr.getMatch(3).getEndPos());

        // Add not Attribute
        // header and title, not book
        sql.add(new SpanAttributeQuery(new SpanTermQuery(new Term("base",
                "@:class=book")), true, true));

        sq = new SpanWithAttributeQuery(new SpanElementQuery("base", "div"),
                sql, true);

        kr = ki.search(sq, (short) 10);

        assertEquals((long) 2, kr.getTotalResults());
        assertEquals(1, kr.getMatch(0).getStartPos());
        assertEquals(2, kr.getMatch(0).getEndPos());
        assertEquals(6, kr.getMatch(1).getStartPos());
        assertEquals(7, kr.getMatch(1).getEndPos());

        // Test multiple negations
        // header, not title, not book
        sql.remove(1);
        sql.add(new SpanAttributeQuery(new SpanTermQuery(new Term("base",
                "@:class=title")), true, true));

        sq = new SpanWithAttributeQuery(new SpanElementQuery("base", "div"),
                sql, true);

        kr = ki.search(sq, (short) 10);
        assertEquals((long) 1, kr.getTotalResults());
        assertEquals(5, kr.getMatch(0).getStartPos());
        assertEquals(6, kr.getMatch(0).getEndPos());
    }


    /**
     * Element with only not attributes
     * 
     * @throws IOException
     * */
    @Test
    public void testcase9 () throws IOException {

        ki.addDoc(createFieldDoc2());
        ki.commit();

        SpanAttributeQuery saq = new SpanAttributeQuery(new SpanTermQuery(
                new Term("base", "@:class=book")), true, true);
        SpanQuery sq = new SpanWithAttributeQuery(new SpanElementQuery("base",
                "div"), saq, true);

        kr = ki.search(sq, (short) 10);
        assertEquals(4, kr.getTotalResults());
        assertEquals(1, kr.getMatch(0).getStartPos());
        assertEquals(2, kr.getMatch(0).getEndPos());
        assertEquals(3, kr.getMatch(1).getStartPos());
        assertEquals(5, kr.getMatch(1).getEndPos());
        assertEquals(5, kr.getMatch(2).getStartPos());
        assertEquals(6, kr.getMatch(2).getEndPos());
        assertEquals(6, kr.getMatch(3).getStartPos());
        assertEquals(7, kr.getMatch(3).getEndPos());

        List<SpanQuery> sql = new ArrayList<>();
        sql.add(saq);
        sql.add(new SpanAttributeQuery(new SpanTermQuery(new Term("base",
                "@:class=header")), true, true));
        sq = new SpanWithAttributeQuery(new SpanElementQuery("base", "div"),
                sql, true);

        kr = ki.search(sq, (short) 10);
        assertEquals(1, kr.getTotalResults());
        assertEquals(3, kr.getMatch(0).getStartPos());
        assertEquals(5, kr.getMatch(0).getEndPos());


    }


    /**
     * same attribute types referring to different element types
     * */
    @Test
    public void testCase3 () throws IOException {
        ki.addDoc(createFieldDoc2());
        ki.commit();

        List<SpanQuery> sql = new ArrayList<>();
        sql.add(new SpanAttributeQuery(new SpanTermQuery(new Term("base",
                "@:class=header")), true));
        sql.add(new SpanAttributeQuery(new SpanTermQuery(new Term("base",
                "@:class=book")), true, true));
        SpanQuery sq = new SpanWithAttributeQuery(new SpanElementQuery("base",
                "div"), sql, true);

        kr = ki.search(sq, (short) 10);

        assertEquals((long) 3, kr.getTotalResults());
        assertEquals(1, kr.getMatch(0).getStartPos());
        assertEquals(2, kr.getMatch(0).getEndPos());
        assertEquals(5, kr.getMatch(1).getStartPos());
        assertEquals(6, kr.getMatch(1).getEndPos());
        assertEquals(6, kr.getMatch(2).getStartPos());
        assertEquals(7, kr.getMatch(2).getEndPos());
    }


    /** Test skipto doc for spanWithAttribute */
    @Test
    public void testCase4 () throws IOException {
        ki.addDoc(createFieldDoc1());
        ki.addDoc(createFieldDoc0());
        ki.addDoc(createFieldDoc2());
        ki.commit();

        SpanAttributeQuery saq = new SpanAttributeQuery(new SpanTermQuery(
                new Term("base", "@:class=book")), true);

        List<SpanQuery> sql = new ArrayList<>();
        sql.add(saq);

        SpanWithAttributeQuery sq = new SpanWithAttributeQuery(
                new SpanElementQuery("base", "div"), sql, true);

        kr = ki.search(sq, (short) 10);
        assertEquals((long) 6, kr.getTotalResults());

        SpanNextQuery snq = new SpanNextQuery(new SpanTermQuery(new Term(
                "base", "s:e")), sq);

        kr = ki.search(snq, (short) 10);

        assertEquals((long) 1, kr.getTotalResults());
        assertEquals(2, kr.getMatch(0).getLocalDocID());
        assertEquals(1, kr.getMatch(0).getStartPos());
        assertEquals(5, kr.getMatch(0).getEndPos());
    }


    /**
     * Arbitrary elements with a specific attribute.
     * */
    @Test
    public void testCase5 () throws IOException {
        ki.addDoc(createFieldDoc2());
        ki.commit();
        SpanAttributeQuery saq = new SpanAttributeQuery(new SpanTermQuery(
                new Term("base", "@:class=book")), true);

        SpanWithAttributeQuery swaq = new SpanWithAttributeQuery(saq, true);
        kr = ki.search(swaq, (short) 10);
        assertEquals(6, kr.getTotalResults());

        assertEquals(0, kr.getMatch(0).getStartPos());
        assertEquals(3, kr.getMatch(0).getEndPos());
        assertEquals(0, kr.getMatch(1).getStartPos());
        assertEquals(5, kr.getMatch(1).getEndPos());
        assertEquals(1, kr.getMatch(2).getStartPos());
        assertEquals(2, kr.getMatch(2).getEndPos());
        assertEquals(2, kr.getMatch(3).getStartPos());
        assertEquals(5, kr.getMatch(3).getEndPos());
        assertEquals(4, kr.getMatch(4).getStartPos());
        assertEquals(5, kr.getMatch(4).getEndPos());
        assertEquals(6, kr.getMatch(5).getStartPos());
        assertEquals(7, kr.getMatch(5).getEndPos());
    }


    /**
     * Arbitrary elements with multiple attributes.
     * */
    @Test
    public void testCase6 () throws IOException {
        ki.addDoc(createFieldDoc2());
        ki.commit();

        List<SpanQuery> sql = new ArrayList<>();
        sql.add(new SpanAttributeQuery(new SpanTermQuery(new Term("base",
                "@:class=header")), true));
        sql.add(new SpanAttributeQuery(new SpanTermQuery(new Term("base",
                "@:class=book")), true));

        SpanWithAttributeQuery swaq = new SpanWithAttributeQuery(sql, true);
        kr = ki.search(swaq, (short) 10);
        assertEquals(2, kr.getTotalResults());

        assertEquals(0, kr.getMatch(0).getStartPos());
        assertEquals(3, kr.getMatch(0).getEndPos());
        assertEquals(4, kr.getMatch(1).getStartPos());
        assertEquals(5, kr.getMatch(1).getEndPos());

        //		for (int i = 0; i < kr.getTotalResults(); i++) {
        //			System.out.println(kr.getMatch(i).getLocalDocID() + " "
        //					+ kr.getMatch(i).startPos + " " + kr.getMatch(i).endPos);
        //		}
    }


    /**
     * Arbitrary elements with an attribute and a not attribute.
     * */
    @Test
    public void testCase7 () throws IOException {
        ki.addDoc(createFieldDoc2());
        ki.commit();

        List<SpanQuery> sql = new ArrayList<>();
        sql.add(new SpanAttributeQuery(new SpanTermQuery(new Term("base",
                "@:class=header")), true, true));
        sql.add(new SpanAttributeQuery(new SpanTermQuery(new Term("base",
                "@:class=book")), true));

        SpanWithAttributeQuery swaq = new SpanWithAttributeQuery(sql, true);
        kr = ki.search(swaq, (short) 10);
        assertEquals(4, kr.getTotalResults());

        assertEquals(0, kr.getMatch(0).getStartPos());
        assertEquals(5, kr.getMatch(0).getEndPos());
        assertEquals(1, kr.getMatch(1).getStartPos());
        assertEquals(2, kr.getMatch(1).getEndPos());
        assertEquals(2, kr.getMatch(2).getStartPos());
        assertEquals(5, kr.getMatch(2).getEndPos());
        assertEquals(6, kr.getMatch(3).getStartPos());
        assertEquals(7, kr.getMatch(3).getEndPos());

        //		for (int i = 0; i < kr.getTotalResults(); i++) {
        //			System.out.println(kr.getMatch(i).getLocalDocID() + " "
        //					+ kr.getMatch(i).startPos + " " + kr.getMatch(i).endPos);
        //		}
    }


    /**
     * Arbitrary elements with only not attributes.
     * */
    @Test(expected = IllegalArgumentException.class)
    public void testCase8 () throws IOException {
        ki.addDoc(createFieldDoc2());
        ki.commit();

        List<SpanQuery> sql = new ArrayList<>();
        sql.add(new SpanAttributeQuery(new SpanTermQuery(new Term("base",
                "@:class=header")), true, true));
        sql.add(new SpanAttributeQuery(new SpanTermQuery(new Term("base",
                "@:class=book")), true, true));

        SpanWithAttributeQuery swaq = new SpanWithAttributeQuery(sql, true);
        kr = ki.search(swaq, (short) 10);
    }

}
