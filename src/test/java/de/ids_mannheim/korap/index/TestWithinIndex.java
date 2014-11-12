package de.ids_mannheim.korap.index;

import java.util.*;
import java.io.*;

import org.apache.lucene.util.Version;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Bits;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import de.ids_mannheim.korap.KorapIndex;
import de.ids_mannheim.korap.KorapMatch;
import de.ids_mannheim.korap.KorapQuery;
import de.ids_mannheim.korap.KorapResult;
import de.ids_mannheim.korap.query.SpanElementQuery;
import de.ids_mannheim.korap.query.SpanWithinQuery;
import de.ids_mannheim.korap.query.SpanNextQuery;
import de.ids_mannheim.korap.query.SpanClassQuery;
import de.ids_mannheim.korap.query.wrap.SpanQueryWrapper;
import de.ids_mannheim.korap.util.QueryException;
import de.ids_mannheim.korap.index.FieldDocument;
import de.ids_mannheim.korap.analysis.MultiTermTokenStream;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.index.Term;

import java.nio.ByteBuffer;

// mvn -Dtest=TestWithinIndex#indexExample1 test

@RunWith(JUnit4.class)
public class TestWithinIndex {

    // Todo: primary data as a non-indexed field separated.

    @Test
    public void indexExample1a () throws IOException {
	KorapIndex ki = new KorapIndex();

	// <a>x<a>y<a>zhij</a>hij</a>hij</a>
	FieldDocument fd = new FieldDocument();
	fd.addTV("base",
		 "x   y   z   h   i   j   h   i   j   h   i   j   ",
		 "[(0-3)s:x|<>:a#0-36$<i>12]" + // 1
		 "[(3-6)s:y|<>:a#3-27$<i>9]" +  // 2
		 "[(6-9)s:z|<>:a#6-18$<i>6]" +  // 3
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
	KorapResult kr;

	sq = new SpanWithinQuery(
	    new SpanElementQuery("base", "a"),
            new SpanTermQuery(new Term("base", "s:h"))
        );

	kr = ki.search(sq, (short) 10);
	
	// System.err.println(kr.toJSON());

	assertEquals("totalResults", 6, kr.totalResults());

	assertEquals("StartPos (0)", 0, kr.match(0).startPos);
	assertEquals("EndPos (0)", 12, kr.match(0).endPos);
	assertEquals("StartPos (1)", 0, kr.match(1).startPos);
	assertEquals("EndPos (1)", 12, kr.match(1).endPos);
	assertEquals("StartPos (2)", 0, kr.match(2).startPos);
	assertEquals("EndPos (2)", 12, kr.match(2).endPos);
	assertEquals("StartPos (3)", 1, kr.match(3).startPos);
	assertEquals("EndPos (3)", 9, kr.match(3).endPos);
	assertEquals("StartPos (4)", 1, kr.match(4).startPos);
	assertEquals("EndPos (4)", 9, kr.match(4).endPos);
	assertEquals("StartPos (5)", 2, kr.match(5).startPos);
	assertEquals("EndPos (5)", 6, kr.match(5).endPos);

	assertEquals(1, ki.numberOf("documents"));
    };

    @Test
    public void indexExample1b () throws IOException {
	// Cases 9, 12, 13
	KorapIndex ki = new KorapIndex();

	// <a>x<a>y<a>zhij</a>hij</a>hij</a>
	FieldDocument fd = new FieldDocument();
	fd.addTV("base",
		 "x   y   z   h   i   j   h   i   j   h   i   j   ",
		 "[(0-3)s:x|<>:a#0-36$<i>12]" + // 1
		 "[(3-6)s:y|<>:a#3-27$<i>9]" +  // 2
		 "[(6-9)s:z|<>:a#6-18$<i>6]" +  // 3
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
	fd.addTV("base",
		 "x   y   z   h   i   j   h   i   j   h   i   j   ",
		 "[(0-3)s:x|<>:a#0-36$<i>12]" + // 1
		 "[(3-6)s:y|<>:a#3-27$<i>9]" +  // 2
		 "[(6-9)s:z|<>:a#6-18$<i>6]" +  // 3
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
	KorapResult kr;

	sq = new SpanWithinQuery(
	    new SpanElementQuery("base", "a"),
            new SpanTermQuery(new Term("base", "s:h"))
        );

	kr = ki.search(sq, (short) 15);
	
	assertEquals("totalResults", 12, kr.totalResults());




	//	System.err.println(kr.toJSON());

	
	assertEquals("StartPos (0)", 0, kr.match(0).startPos);
	assertEquals("EndPos (0)", 12, kr.match(0).endPos);
	assertEquals("Doc (0)", 0, kr.match(0).internalDocID);
	assertEquals("StartPos (1)", 0, kr.match(1).startPos);
	assertEquals("EndPos (1)", 12, kr.match(1).endPos);
	assertEquals("Doc (1)", 0, kr.match(1).internalDocID);
	assertEquals("StartPos (2)", 0, kr.match(2).startPos);
	assertEquals("EndPos (2)", 12, kr.match(2).endPos);
	assertEquals("Doc (2)", 0, kr.match(2).internalDocID);
	assertEquals("StartPos (3)", 1, kr.match(3).startPos);
	assertEquals("EndPos (3)", 9, kr.match(3).endPos);
	assertEquals("Doc (3)", 0, kr.match(3).internalDocID);
	assertEquals("StartPos (4)", 1, kr.match(4).startPos);
	assertEquals("EndPos (4)", 9, kr.match(4).endPos);
	assertEquals("Doc (4)", 0, kr.match(4).internalDocID);
	assertEquals("StartPos (5)", 2, kr.match(5).startPos);
	assertEquals("EndPos (5)", 6, kr.match(5).endPos);
	assertEquals("Doc (5)", 0, kr.match(5).internalDocID);

	assertEquals("StartPos (6)", 0, kr.match(6).startPos);
	assertEquals("EndPos (6)", 12, kr.match(6).endPos);
	assertEquals("Doc (6)", 1, kr.match(6).internalDocID);
	assertEquals("StartPos (7)", 0, kr.match(7).startPos);
	assertEquals("EndPos (7)", 12, kr.match(7).endPos);
	assertEquals("Doc (7)", 1, kr.match(7).internalDocID);
	assertEquals("StartPos (8)", 0, kr.match(8).startPos);
	assertEquals("EndPos (8)", 12, kr.match(8).endPos);
	assertEquals("Doc (8)", 1, kr.match(8).internalDocID);
	assertEquals("StartPos (9)", 1, kr.match(9).startPos);
	assertEquals("EndPos (9)", 9, kr.match(9).endPos);
	assertEquals("Doc (9)", 1, kr.match(9).internalDocID);
	assertEquals("StartPos (10)", 1, kr.match(10).startPos);
	assertEquals("EndPos (10)", 9, kr.match(10).endPos);
	assertEquals("Doc (10)", 1, kr.match(10).internalDocID);
	assertEquals("StartPos (11)", 2, kr.match(11).startPos);
	assertEquals("EndPos (11)", 6, kr.match(11).endPos);
	assertEquals("Doc (11)", 1, kr.match(11).internalDocID);

	assertEquals(2, ki.numberOf("documents"));
    };


    @Test
    public void indexExample1c () throws IOException {
	// Cases 9, 12, 13
	KorapIndex ki = new KorapIndex();

	// <a>x<a>y<a>zhij</a>hij</a>hij</a>
	FieldDocument fd = new FieldDocument();
	fd.addTV("base",
		 "x   y   z   h   i   j   h   i   j   h   i   j   ",
		 "[(0-3)s:x|<>:a#0-36$<i>12]" + // 1
		 "[(3-6)s:y|<>:a#3-27$<i>9]" +  // 2
		 "[(6-9)s:z|<>:a#6-18$<i>6]" +  // 3
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
	fd.addTV("base",
		 "x   y   z   a   b   c   a   b   c   a   b   c   ",
		 "[(0-3)s:x|<>:a#0-36$<i>12]" + // 1
		 "[(3-6)s:y|<>:a#3-27$<i>9]" +  // 2
		 "[(6-9)s:z|<>:a#6-18$<i>6]" +  // 3
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
	KorapResult kr;

	sq = new SpanWithinQuery(
	    new SpanElementQuery("base", "a"),
            new SpanTermQuery(new Term("base", "s:h"))
        );

	kr = ki.search(sq, (short) 15);
	
	// System.err.println(kr.toJSON());

	assertEquals("totalResults", 6, kr.totalResults());

	assertEquals("StartPos (0)", 0, kr.match(0).startPos);
	assertEquals("EndPos (0)", 12, kr.match(0).endPos);
	assertEquals("Doc (0)", 0, kr.match(0).internalDocID);
	assertEquals("StartPos (1)", 0, kr.match(1).startPos);
	assertEquals("EndPos (1)", 12, kr.match(1).endPos);
	assertEquals("Doc (1)", 0, kr.match(1).internalDocID);
	assertEquals("StartPos (2)", 0, kr.match(2).startPos);
	assertEquals("EndPos (2)", 12, kr.match(2).endPos);
	assertEquals("Doc (2)", 0, kr.match(2).internalDocID);
	assertEquals("StartPos (3)", 1, kr.match(3).startPos);
	assertEquals("EndPos (3)", 9, kr.match(3).endPos);
	assertEquals("Doc (3)", 0, kr.match(3).internalDocID);
	assertEquals("StartPos (4)", 1, kr.match(4).startPos);
	assertEquals("EndPos (4)", 9, kr.match(4).endPos);
	assertEquals("Doc (4)", 0, kr.match(4).internalDocID);
	assertEquals("StartPos (5)", 2, kr.match(5).startPos);
	assertEquals("EndPos (5)", 6, kr.match(5).endPos);
	assertEquals("Doc (5)", 0, kr.match(5).internalDocID);

	assertEquals(2, ki.numberOf("documents"));
    };


    @Test
    public void indexExample1d () throws IOException {
	// Cases 9, 12, 13
	KorapIndex ki = new KorapIndex();

	// <a>x<a>y<a>zhij</a>hij</a>hij</a>
	FieldDocument fd = new FieldDocument();
	fd.addTV("base",
		 "x   y   z   h   i   j   h   i   j   h   i   j   ",
		 "[(0-3)s:x|<>:a#0-36$<i>12]" + // 1
		 "[(3-6)s:y|<>:a#3-27$<i>9]" +  // 2
		 "[(6-9)s:z|<>:a#6-18$<i>6]" +  // 3
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
	fd.addTV("base",
		 "x   y   z   h   ",
		 "[(0-3)s:x]" +  // 1
		 "[(3-6)s:y]" +  // 2
		 "[(6-9)s:z]" +  // 3
		 "[(9-12)s:h]"); // 4
	ki.addDoc(fd);

	// <a>x<a>y<a>zabc</a>abc</a>abc</a>
	fd = new FieldDocument();
	fd.addTV("base",
		 "x   y   z   a   b   c   a   b   c   a   b   c   ",
		 "[(0-3)s:x|<>:a#0-36$<i>12]" + // 1
		 "[(3-6)s:y|<>:a#3-27$<i>9]" +  // 2
		 "[(6-9)s:z|<>:a#6-18$<i>6]" +  // 3
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
	KorapResult kr;

	sq = new SpanElementQuery("base", "a");
	kr = ki.search(sq, (short) 15);

	sq = new SpanWithinQuery(
	    new SpanElementQuery("base", "a"),
            new SpanTermQuery(new Term("base", "s:h"))
        );

	kr = ki.search(sq, (short) 15);
	
	// System.err.println(kr.toJSON());

	assertEquals("totalResults", 6, kr.totalResults());

	assertEquals("StartPos (0)", 0, kr.match(0).startPos);
	assertEquals("EndPos (0)", 12, kr.match(0).endPos);
	assertEquals("Doc (0)", 0, kr.match(0).internalDocID);
	assertEquals("StartPos (1)", 0, kr.match(1).startPos);
	assertEquals("EndPos (1)", 12, kr.match(1).endPos);
	assertEquals("Doc (1)", 0, kr.match(1).internalDocID);
	assertEquals("StartPos (2)", 0, kr.match(2).startPos);
	assertEquals("EndPos (2)", 12, kr.match(2).endPos);
	assertEquals("Doc (2)", 0, kr.match(2).internalDocID);
	assertEquals("StartPos (3)", 1, kr.match(3).startPos);
	assertEquals("EndPos (3)", 9, kr.match(3).endPos);
	assertEquals("Doc (3)", 0, kr.match(3).internalDocID);
	assertEquals("StartPos (4)", 1, kr.match(4).startPos);
	assertEquals("EndPos (4)", 9, kr.match(4).endPos);
	assertEquals("Doc (4)", 0, kr.match(4).internalDocID);
	assertEquals("StartPos (5)", 2, kr.match(5).startPos);
	assertEquals("EndPos (5)", 6, kr.match(5).endPos);
	assertEquals("Doc (5)", 0, kr.match(5).internalDocID);

	assertEquals(3, ki.numberOf("documents"));
    };





    @Test
    public void indexExample2a () throws IOException {
	KorapIndex ki = new KorapIndex();

	// <a><a><a>h</a>hij</a>hij</a>
	FieldDocument fd = new FieldDocument();
	fd.addTV("base",
		 "h   i   j   h   i   j   h   i   j   ",
		 "[(0-3)s:h|<>:a#0-27$<i>7|<>:a#0-18$<i>4|<>:a#0-36$<i>10]" + // 1
		 "[(3-6)s:h]" +    // 2
		 "[(12-15)s:i]" +  // 3
		 "[(15-18)s:j]" +  // 4
		 "[(18-21)s:h]" +  // 5
		 "[(21-24)s:i]" +  // 6
		 "[(24-27)s:j]" +  // 7
		 "[(27-30)s:h]" +  // 8
		 "[(30-33)s:i]" +  // 9
		 "[(33-36)s:j]");  // 10
	ki.addDoc(fd);

	// Save documents
	ki.commit();

	assertEquals(1, ki.numberOf("documents"));

	SpanQuery sq;
	KorapResult kr;

	sq = new SpanElementQuery("base", "a");
	kr = ki.search(sq, (short) 10);

	assertEquals("totalResults", 3, kr.totalResults());
	assertEquals("StartPos (0)", 0, kr.match(0).startPos);
	assertEquals("EndPos (0)", 4, kr.match(0).endPos);
	assertEquals("StartPos (1)", 0, kr.match(1).startPos);
	assertEquals("EndPos (1)", 7, kr.match(1).endPos);
	assertEquals("StartPos (2)", 0, kr.match(2).startPos);
	assertEquals("EndPos (2)", 10, kr.match(2).endPos);
	    
	sq = new SpanWithinQuery(
	    new SpanElementQuery("base", "a"),
            new SpanTermQuery(new Term("base", "s:h"))
        );

	kr = ki.search(sq, (short) 10);
	//	System.err.println(kr.toJSON());

	assertEquals("totalResults", 9, kr.totalResults());
	assertEquals("StartPos (0)", 0, kr.match(0).startPos);
	assertEquals("EndPos (0)", 4, kr.match(0).endPos);
	assertEquals("StartPos (1)", 0, kr.match(1).startPos);
	assertEquals("EndPos (1)", 4, kr.match(1).endPos);
	assertEquals("StartPos (2)", 0, kr.match(2).startPos);
	assertEquals("EndPos (2)", 7, kr.match(2).endPos);
	assertEquals("StartPos (3)", 0, kr.match(3).startPos);
	assertEquals("EndPos (3)", 7, kr.match(3).endPos);
	assertEquals("StartPos (4)", 0, kr.match(4).startPos);
	assertEquals("EndPos (4)", 7, kr.match(4).endPos);
	assertEquals("StartPos (5)", 0, kr.match(5).startPos);
	assertEquals("EndPos (5)", 10, kr.match(5).endPos);
	assertEquals("StartPos (6)", 0, kr.match(6).startPos);
	assertEquals("EndPos (6)", 10, kr.match(6).endPos);
	assertEquals("StartPos (7)", 0, kr.match(7).startPos);
	assertEquals("EndPos (7)", 10, kr.match(7).endPos);
	assertEquals("StartPos (8)", 0, kr.match(8).startPos);
	assertEquals("EndPos (8)", 10, kr.match(8).endPos);
    };

    @Test
    public void indexExample2b () throws IOException {
	KorapIndex ki = new KorapIndex();

	// 6,9,12
	// <a><a><a>h</a>hij</a>hij</a>h
	FieldDocument fd = new FieldDocument();
	fd.addTV("base",
		 "h   i   j   h   i   j   h   i   j   h   ",
		 "[(0-3)s:h|<>:a#0-27$<i>7|<>:a#0-18$<i>4|<>:a#0-36$<i>10]" + // 1
		 "[(3-6)s:h]" +    // 2
		 "[(12-15)s:i]" +  // 3
		 "[(15-18)s:j]" +  // 4
		 "[(18-21)s:h]" +  // 5
		 "[(21-24)s:i]" +  // 6
		 "[(24-27)s:j]" +  // 7
		 "[(27-30)s:h]" +  // 8
		 "[(30-33)s:i]" +  // 9
		 "[(33-36)s:j]" +  // 10
		 "[(37-40)s:h]");
	ki.addDoc(fd);

	// Save documents
	ki.commit();

	assertEquals(1, ki.numberOf("documents"));

	SpanQuery sq = new SpanElementQuery("base", "a");

	KorapResult kr = ki.search(sq, (short) 10);

	assertEquals("totalResults", 3, kr.totalResults());
	assertEquals("StartPos (0)", 0, kr.match(0).startPos);
	assertEquals("EndPos (0)", 4, kr.match(0).endPos);
	assertEquals("StartPos (1)", 0, kr.match(1).startPos);
	assertEquals("EndPos (1)", 7, kr.match(1).endPos);
	assertEquals("StartPos (2)", 0, kr.match(2).startPos);
	assertEquals("EndPos (2)", 10, kr.match(2).endPos);

	sq = new SpanWithinQuery(
	    new SpanElementQuery("base", "a"),
            new SpanTermQuery(new Term("base", "s:h"))
        );

	kr = ki.search(sq, (short) 10);
	//	System.err.println(kr.toJSON());

	assertEquals("totalResults", 9, kr.totalResults());
	assertEquals("StartPos (0)", 0, kr.match(0).startPos);
	assertEquals("EndPos (0)", 4, kr.match(0).endPos);
	assertEquals("StartPos (1)", 0, kr.match(1).startPos);
	assertEquals("EndPos (1)", 4, kr.match(1).endPos);
	assertEquals("StartPos (2)", 0, kr.match(2).startPos);
	assertEquals("EndPos (2)", 7, kr.match(2).endPos);
	assertEquals("StartPos (3)", 0, kr.match(3).startPos);
	assertEquals("EndPos (3)", 7, kr.match(3).endPos);
	assertEquals("StartPos (4)", 0, kr.match(4).startPos);
	assertEquals("EndPos (4)", 7, kr.match(4).endPos);
	assertEquals("StartPos (5)", 0, kr.match(5).startPos);
	assertEquals("EndPos (5)", 10, kr.match(5).endPos);
	assertEquals("StartPos (6)", 0, kr.match(6).startPos);
	assertEquals("EndPos (6)", 10, kr.match(6).endPos);
	assertEquals("StartPos (7)", 0, kr.match(7).startPos);
	assertEquals("EndPos (7)", 10, kr.match(7).endPos);
	assertEquals("StartPos (8)", 0, kr.match(8).startPos);
	assertEquals("EndPos (8)", 10, kr.match(8).endPos);
    };


    @Test
    public void indexExample2c () throws IOException {
	KorapIndex ki = new KorapIndex();

	// 2, 6, 9, 12
	// <a><a><a>h</a>hij</a>hij</a>h<a>i</i>
	FieldDocument fd = new FieldDocument();
	fd.addTV("base",
		 "h   i   j   h   i   j   h   i   j   h   i   ",
		 "[(0-3)s:h|<>:a#0-27$<i>7|<>:a#0-18$<i>4|<>:a#0-36$<i>10]" + // 1
		 "[(3-6)s:h]" +    // 2
		 "[(12-15)s:i]" +  // 3
		 "[(15-18)s:j]" +  // 4
		 "[(18-21)s:h]" +  // 5
		 "[(21-24)s:i]" +  // 6
		 "[(24-27)s:j]" +  // 7
		 "[(27-30)s:h]" +  // 8
		 "[(30-33)s:i]" +  // 9
		 "[(33-36)s:j]" +  // 10
		 "[(37-40)s:h]" +  // 11
		 "[(40-43)s:i|<>:a#40-43$<i>12]"); // 12
	ki.addDoc(fd);

	// Save documents
	ki.commit();

	assertEquals(1, ki.numberOf("documents"));

	SpanQuery sq = new SpanElementQuery("base", "a");

	KorapResult kr = ki.search(sq, (short) 10);

	assertEquals("totalResults", 4, kr.totalResults());
	assertEquals("StartPos (0)", 0, kr.match(0).startPos);
	assertEquals("EndPos (0)", 4, kr.match(0).endPos);
	assertEquals("StartPos (1)", 0, kr.match(1).startPos);
	assertEquals("EndPos (1)", 7, kr.match(1).endPos);
	assertEquals("StartPos (2)", 0, kr.match(2).startPos);
	assertEquals("EndPos (2)", 10, kr.match(2).endPos);
	assertEquals("StartPos (3)", 11, kr.match(3).startPos);
	assertEquals("EndPos (3)", 12, kr.match(3).endPos);

	sq = new SpanWithinQuery(
	    new SpanElementQuery("base", "a"),
            new SpanTermQuery(new Term("base", "s:h"))
        );

	kr = ki.search(sq, (short) 10);
	//	System.err.println(kr.toJSON());

	assertEquals("totalResults", 9, kr.totalResults());
	assertEquals("StartPos (0)", 0, kr.match(0).startPos);
	assertEquals("EndPos (0)", 4, kr.match(0).endPos);
	assertEquals("StartPos (1)", 0, kr.match(1).startPos);
	assertEquals("EndPos (1)", 4, kr.match(1).endPos);
	assertEquals("StartPos (2)", 0, kr.match(2).startPos);
	assertEquals("EndPos (2)", 7, kr.match(2).endPos);
	assertEquals("StartPos (3)", 0, kr.match(3).startPos);
	assertEquals("EndPos (3)", 7, kr.match(3).endPos);
	assertEquals("StartPos (4)", 0, kr.match(4).startPos);
	assertEquals("EndPos (4)", 7, kr.match(4).endPos);
	assertEquals("StartPos (5)", 0, kr.match(5).startPos);
	assertEquals("EndPos (5)", 10, kr.match(5).endPos);
	assertEquals("StartPos (6)", 0, kr.match(6).startPos);
	assertEquals("EndPos (6)", 10, kr.match(6).endPos);
	assertEquals("StartPos (7)", 0, kr.match(7).startPos);
	assertEquals("EndPos (7)", 10, kr.match(7).endPos);
	assertEquals("StartPos (8)", 0, kr.match(8).startPos);
	assertEquals("EndPos (8)", 10, kr.match(8).endPos);
    };


    @Test
    public void indexExample2d () throws IOException {
	KorapIndex ki = new KorapIndex();

	// 2, 6, 9, 12, 7
	// <a><a><a>h</a>hij</a>hij</a>h<a>h</h>
	FieldDocument fd = new FieldDocument();
	fd.addTV("base",
		 "h   i   j   h   i   j   h   i   j   h   i   ",
		 "[(0-3)s:h|<>:a#0-27$<i>7|<>:a#0-18$<i>4|<>:a#0-36$<i>10]" + // 1
		 "[(3-6)s:h]" +    // 2
		 "[(12-15)s:i]" +  // 3
		 "[(15-18)s:j]" +  // 4
		 "[(18-21)s:h]" +  // 5
		 "[(21-24)s:i]" +  // 6
		 "[(24-27)s:j]" +  // 7
		 "[(27-30)s:h]" +  // 8
		 "[(30-33)s:i]" +  // 9
		 "[(33-36)s:j]" +  // 10
		 "[(37-40)s:h]" +  // 11
		 "[(40-43)s:h|<>:a#40-43$<i>12]"); // 12
	ki.addDoc(fd);

	// Save documents
	ki.commit();

	assertEquals(1, ki.numberOf("documents"));

	SpanQuery sq = new SpanElementQuery("base", "a");

	KorapResult kr = ki.search(sq, (short) 10);

	assertEquals("totalResults", 4, kr.totalResults());
	assertEquals("StartPos (0)", 0, kr.match(0).startPos);
	assertEquals("EndPos (0)", 4, kr.match(0).endPos);
	assertEquals("StartPos (1)", 0, kr.match(1).startPos);
	assertEquals("EndPos (1)", 7, kr.match(1).endPos);
	assertEquals("StartPos (2)", 0, kr.match(2).startPos);
	assertEquals("EndPos (2)", 10, kr.match(2).endPos);
	assertEquals("StartPos (3)", 11, kr.match(3).startPos);
	assertEquals("EndPos (3)", 12, kr.match(3).endPos);

	sq = new SpanWithinQuery(
	    new SpanElementQuery("base", "a"),
            new SpanTermQuery(new Term("base", "s:h"))
        );

	kr = ki.search(sq, (short) 15);
	//	System.err.println(kr.toJSON());

	assertEquals("totalResults", 10, kr.totalResults());
	assertEquals("StartPos (0)", 0, kr.match(0).startPos);
	assertEquals("EndPos (0)", 4, kr.match(0).endPos);
	assertEquals("StartPos (1)", 0, kr.match(1).startPos);
	assertEquals("EndPos (1)", 4, kr.match(1).endPos);
	assertEquals("StartPos (2)", 0, kr.match(2).startPos);
	assertEquals("EndPos (2)", 7, kr.match(2).endPos);
	assertEquals("StartPos (3)", 0, kr.match(3).startPos);
	assertEquals("EndPos (3)", 7, kr.match(3).endPos);
	assertEquals("StartPos (4)", 0, kr.match(4).startPos);
	assertEquals("EndPos (4)", 7, kr.match(4).endPos);
	assertEquals("StartPos (5)", 0, kr.match(5).startPos);
	assertEquals("EndPos (5)", 10, kr.match(5).endPos);
	assertEquals("StartPos (6)", 0, kr.match(6).startPos);
	assertEquals("EndPos (6)", 10, kr.match(6).endPos);
	assertEquals("StartPos (7)", 0, kr.match(7).startPos);
	assertEquals("EndPos (7)", 10, kr.match(7).endPos);
	assertEquals("StartPos (8)", 0, kr.match(8).startPos);
	assertEquals("EndPos (8)", 10, kr.match(8).endPos);
	assertEquals("StartPos (9)", 11, kr.match(9).startPos);
	assertEquals("EndPos (9)", 12, kr.match(9).endPos);
    };


    @Test
    public void indexExample3 () throws IOException {
	KorapIndex ki = new KorapIndex();

	// <a><a><a>u</a></a></a>
	FieldDocument fd = new FieldDocument();
	fd.addTV("base",
		 "xyz",
		 "[(0-3)s:xyz|<>:a#0-3$<i>0|<>:a#0-3$<i>0|<>:a#0-3$<i>0|<>:b#0-3$<i>0]");
	ki.addDoc(fd);

	// <a><b>x<a>y<a>zcde</a>cde</a>cde</b></a>
	fd = new FieldDocument();
	fd.addTV("base",
		 "x   y   z   c   d   e   c   d   e   c   d   e   ",
		 "[(0-3)s:x|<>:a#0-36$<i>12|<>:b#0-36$<i>12]" +
		 "[(3-6)s:y|<>:a#3-27$<i>9]" +
		 "[(6-9)s:z|<>:a#6-18$<i>6]" +
		 "[(9-12)s:c]" +
		 "[(12-15)s:d]" +
		 "[(15-18)s:e]" +
		 "[(18-21)s:c]" +
		 "[(21-24)s:d]" +
		 "[(24-27)s:e]" +
		 "[(27-30)s:c]" +
		 "[(30-33)s:d]" +
		 "[(33-36)s:e]");
	ki.addDoc(fd);

	// xyz
	fd = new FieldDocument();
	fd.addTV("base",
		 "x   y   z   ",
		 "[(0-3)s:x]" +
		 "[(3-6)s:y]" +
		 "[(6-9)s:z]");
	ki.addDoc(fd);

	// <a>x<a><b>y<a>zcde</a>cde</b></a>cde</a>
	fd = new FieldDocument();
	fd.addTV("base",
		 "x   y   z   k   l   m   k   l   m   k   l   m   ",
		 "[(0-3)s:x|<>:a#0-3$<i>12]" +
		 "[(3-6)s:y|<>:a#3-6$<i>9|<>:b#3-6$<i>9]" +
		 "[(6-9)s:z|<>:a#6-9$<i>6]" +
		 "[(9-12)s:k]" +
		 "[(12-15)s:l]" +
		 "[(15-18)s:m]" +
		 "[(18-21)s:k]" +
		 "[(21-24)s:l]" +
		 "[(24-27)s:m]" +
		 "[(27-30)s:k]" +
		 "[(30-33)s:l]" +
		 "[(33-36)s:m]");
	ki.addDoc(fd);

	// <a><a><a>h</a>hhij</a>hij</a>hij</a>
	fd = new FieldDocument();
	fd.addTV("base",
		 "h   i   j   h   i   j   h   i   j   ",
		 "[(0-3)s:h|<>:a#0-27$<i>6|<>:a#0-18$<i>3|<>:a#0-36$<i>9]" +
		 "[(3-6)s:h]" +
		 "[(12-15)s:i]" +
		 "[(15-18)s:j]" +
		 "[(18-21)s:h]" +
		 "[(21-24)s:i]" +
		 "[(24-27)s:j]" +
		 "[(27-30)s:h]" +
		 "[(30-33)s:i]" +
		 "[(33-36)s:j]");
	ki.addDoc(fd);

	// xyz
	fd = new FieldDocument();
	fd.addTV("base",
		 "a  b  c  ",
		 "[(0-3)s:a]" +
		 "[(3-6)s:b]" +
		 "[(6-9)s:c]");
	ki.addDoc(fd);


	// Save documents
	ki.commit();

	assertEquals(6, ki.numberOf("documents"));

	SpanQuery sq = new SpanElementQuery("base", "a");

	KorapResult kr = ki.search(sq, (short) 15);

	//	System.err.println(ki.search(sq, (short) 10).toJSON());
	assertEquals("totalResults", 12, kr.totalResults());

	assertEquals("StartPos (0)", 0, kr.match(0).startPos);
	assertEquals("EndPos (0)", 0, kr.match(0).endPos);
	assertEquals("StartPos (1)", 0, kr.match(1).startPos);
	assertEquals("EndPos (1)", 0, kr.match(1).endPos);
	assertEquals("StartPos (2)", 0, kr.match(2).startPos);
	assertEquals("EndPos (2)", 0, kr.match(2).endPos);

	assertEquals("StartPos (3)", 0, kr.match(3).startPos);
	assertEquals("EndPos (3)", 12, kr.match(3).endPos);
	assertEquals("StartPos (4)", 1, kr.match(4).startPos);
	assertEquals("EndPos (4)", 9, kr.match(4).endPos);
	assertEquals("StartPos (5)", 2, kr.match(5).startPos);
	assertEquals("EndPos (5)", 6, kr.match(5).endPos);

	assertEquals("StartPos (6)", 0, kr.match(6).startPos);
	assertEquals("EndPos (6)", 12, kr.match(6).endPos);
	assertEquals("StartPos (7)", 1, kr.match(7).startPos);
	assertEquals("EndPos (7)", 9, kr.match(7).endPos);
	assertEquals("StartPos (8)", 2, kr.match(8).startPos);
	assertEquals("EndPos (8)", 6, kr.match(8).endPos);

	assertEquals("StartPos (9)", 0, kr.match(9).startPos);
	assertEquals("EndPos (9)", 3, kr.match(9).endPos);
	assertEquals("StartPos (10)", 0, kr.match(10).startPos);
	assertEquals("EndPos (10)", 6, kr.match(10).endPos);
	assertEquals("StartPos (11)", 0, kr.match(11).startPos);
	assertEquals("EndPos (11)", 9, kr.match(11).endPos);
    };

    @Test
    public void indexExample3Offsets () throws IOException {
	KorapIndex ki = new KorapIndex();

	// Er schrie: <s>"Das war ich!"</s>
	FieldDocument fd = new FieldDocument();
	fd = new FieldDocument();
	fd.addTV("base",
		 "Er schrie: \"Das war ich!\" und ging.",
		 "[(0-2)s:Er|_0#0-3]" +
		 "[(3-9)s:schrie|_1#3-9]" +
		 "[(12-15)s:Das|_2#12-15|<>:sentence#11-25$<i>5]" +
		 "[(16-19)s:war|_3#16-19]" +
		 "[(20-23)s:ich|_4#20-23]" +
		 "[(26-29)s:und|_5#26-29]" +
		 "[(30-34)s:ging|_6#30-34]");
	ki.addDoc(fd);

	// Save documents
	ki.commit();

	SpanQuery sq = new SpanClassQuery(new SpanElementQuery("base", "sentence"), (byte)3);
	KorapResult kr;
	kr = ki.search(sq, 0, (short) 15, true, (short) 1, true, (short) 1);
	assertEquals("totalResults", 1, kr.totalResults());

	assertEquals("... schrie: [\"{3:Das war ich}!\"] und ...",kr.match(0).snippetBrackets());
	assertEquals("<span class=\"context-left\"><span class=\"more\"></span>schrie: </span><span class=\"match\">&quot;<em class=\"class-3 level-0\">Das war ich</em>!&quot;</span><span class=\"context-right\"> und<span class=\"more\"></span></span>",kr.match(0).snippetHTML());


	kr = ki.search(sq, 0, (short) 15, true, (short) 0, true, (short) 0);
	assertEquals("... [\"{3:Das war ich}!\"] ...",kr.match(0).snippetBrackets());
	assertEquals("totalResults", 1, kr.totalResults());


	kr = ki.search(sq, 0, (short) 15, true, (short) 6, true, (short) 6);
	assertEquals("Er schrie: [\"{3:Das war ich}!\"] und ging.",kr.match(0).snippetBrackets());
	assertEquals("totalResults", 1, kr.totalResults());

	kr = ki.search(sq, 0, (short) 15, true, (short) 2, true, (short) 2);
	assertEquals("Er schrie: [\"{3:Das war ich}!\"] und ging ...",kr.match(0).snippetBrackets());
	assertEquals("totalResults", 1, kr.totalResults());


	sq = new SpanClassQuery(
          new SpanWithinQuery(
            new SpanElementQuery("base", "sentence"),
	    new SpanClassQuery(
              new SpanTermQuery(new Term("base", "s:Das")), (byte) 2
	    )
        ), (byte) 1);

	kr = ki.search(sq, (short) 15);
	assertEquals("Er schrie: [\"{1:{2:Das} war ich}!\"] und ging.",kr.match(0).snippetBrackets());
	assertEquals("totalResults", 1, kr.totalResults());

	sq = new SpanClassQuery(
          new SpanWithinQuery(
            new SpanElementQuery("base", "sentence"),
	    new SpanClassQuery(
              new SpanTermQuery(new Term("base", "s:war")), (byte) 2
	    )
        ), (byte) 1);

	kr = ki.search(sq, (short) 15);
	assertEquals("Er schrie: [\"{1:Das {2:war} ich}!\"] und ging.",kr.match(0).snippetBrackets());
	assertEquals("totalResults", 1, kr.totalResults());

	sq = new SpanClassQuery(
          new SpanWithinQuery(
            new SpanElementQuery("base", "sentence"),
	    new SpanClassQuery(
              new SpanTermQuery(new Term("base", "s:ich")), (byte) 2
	    )
        ), (byte) 1);

	kr = ki.search(sq, (short) 15);
	assertEquals("Er schrie: [\"{1:Das war {2:ich}}!\"] und ging.",kr.match(0).snippetBrackets());
	assertEquals("totalResults", 1, kr.totalResults());

	sq = new SpanClassQuery(
          new SpanWithinQuery(
            new SpanElementQuery("base", "sentence"),
	    new SpanClassQuery(
              new SpanTermQuery(new Term("base", "s:und")), (byte) 2
	    )
        ), (byte) 1);

	kr = ki.search(sq, (short) 15);
	assertEquals("totalResults", 0, kr.totalResults());

	sq = new SpanClassQuery(
          new SpanWithinQuery(
            new SpanElementQuery("base", "sentence"),
	    new SpanClassQuery(
              new SpanTermQuery(new Term("base", "s:schrie")), (byte) 2
	    )
        ), (byte) 1);

	kr = ki.search(sq, (short) 15);
	assertEquals("totalResults", 0, kr.totalResults());
    };

    //!! Offset is 1 token tooo long

    @Test
    public void indexExample4 () throws IOException {
	KorapIndex ki = new KorapIndex();

	// Case 1, 6, 7, 13
	// xy<a><a>x</a>b<a>c</a></a>x
	FieldDocument fd = new FieldDocument();
	fd.addTV("base",
		 "x  y  x  b  c  x  ",
		 "[(0-3)s:x|_0#0-3]" +
		 "[(3-6)s:y|_1#3-6]" +
		 "[(6-9)s:x|_2#6-9|<>:a#6-15$<i>5|<>:a#6-9$<i>3]" +
		 "[(9-12)s:b|_3#9-12]" +
		 "[(12-15)s:c|_4#12-15|<>:a#12-15$<i>5]" +
		 "[(15-18)s:x|_5#15-18]");
	ki.addDoc(fd);

	// Save documents
	ki.commit();

	assertEquals(1, ki.numberOf("documents"));

	SpanQuery sq = new SpanWithinQuery(
	    new SpanElementQuery("base", "a"),
            new SpanTermQuery(new Term("base", "s:x"))
        );

	KorapResult kr = ki.search(sq, (short) 10);

	// System.err.println(kr.toJSON());
	assertEquals("totalResults", 2, kr.totalResults());
	assertEquals("StartPos (0)", 2, kr.match(0).startPos);
	assertEquals("EndPos (0)", 3, kr.match(0).endPos);
	assertEquals("StartPos (1)", 2, kr.match(1).startPos);
	assertEquals("EndPos (1)", 5, kr.match(1).endPos);
    };


    @Test
    public void indexExample5 () throws IOException {
	// 1,2,3,6,9,10,12
	KorapIndex ki = new KorapIndex();

	// hij<a>hi<a>h<a>ij</a></a>hi</a>
	FieldDocument fd = new FieldDocument();
	fd.addTV("base",
		 "hijhihijhi",
		 "[(0-1)s:h|i:h|_0#0-1|-:a$<i>3|-:t$<i>10]" +
		 "[(1-2)s:i|i:i|_1#1-2]" +
		 "[(2-3)s:j|i:j|_2#2-3]" +
		 "[(3-4)s:h|i:h|_3#3-4|<>:a#3-10$<i>10]" +
		 "[(4-5)s:i|i:i|_4#4-5]" +
		 "[(5-6)s:h|i:h|_5#5-6|<>:a#5-8$<i>8]" +
		 "[(6-7)s:i|i:i|_6#6-7|<>:a#6-8$<i>8]" +
		 "[(7-8)s:j|i:j|_7#7-8]" +
		 "[(8-9)s:h|i:h|_8#8-9]" +
		 "[(9-10)s:i|i:i|_9#9-10]");
	ki.addDoc(fd);

	// Save documents
	ki.commit();

	assertEquals(1, ki.numberOf("documents"));

	SpanQuery sq = new SpanWithinQuery(
	    new SpanElementQuery("base", "a"),
            new SpanNextQuery(
	      new SpanTermQuery(new Term("base", "s:h")),
	      new SpanTermQuery(new Term("base", "s:i"))
            )
        );

	KorapResult kr = ki.search(sq, (short) 10);

	assertEquals("totalResults", 4, kr.totalResults());

	assertEquals("StartPos (0)", 3, kr.match(0).startPos);
	assertEquals("EndPos (0)", 10, kr.match(0).endPos);
	assertEquals("StartPos (1)", 3, kr.match(1).startPos);
	assertEquals("EndPos (1)", 10, kr.match(1).endPos);
	assertEquals("StartPos (2)", 3, kr.match(2).startPos);
	assertEquals("EndPos (2)", 10, kr.match(2).endPos);
	assertEquals("StartPos (3)", 5, kr.match(3).startPos);
	assertEquals("EndPos (3)", 8, kr.match(3).endPos);
    };

    @Test
    public void indexExample6 () throws IOException {
	KorapIndex ki = new KorapIndex();
	// 2,5,8,12,13
	// h<a><a>i</a>j</a><a>h</a>i j<a>h i</a>j
	FieldDocument fd = new FieldDocument();
	fd.addTV("base",
		 "hijhi jh ij",
		 "[(0-1)s:h|i:h|_0#0-1|-:a$<i>4|-:t$<i>9]" +
		 "[(1-2)s:i|i:i|_1#1-2|<>:a#1-2$<i>2|<>:a#1-3$<i>3]" +
		 "[(2-3)s:j|i:j|_2#2-3]" +
		 "[(3-4)s:h|i:h|_3#3-4|<>:a#3-4$<i>4]" +
		 "[(4-5)s:i|i:i|_4#4-5]" +
		 "[(6-7)s:j|i:j|_5#6-7]" +
		 "[(7-8)s:h|i:h|_6#7-8|<>:a#7-10$<i>8]" +
		 "[(9-10)s:i|i:i|_7#9-10]" +
		 "[(10-11)s:j|i:j|_8#10-11]");
	ki.addDoc(fd);

	// Save documents
	ki.commit();

	assertEquals(1, ki.numberOf("documents"));

	SpanQuery sq = new SpanWithinQuery(
          new SpanElementQuery("base", "a"),
            new SpanNextQuery(
	      new SpanTermQuery(new Term("base", "s:h")),
	      new SpanNextQuery(
		new SpanTermQuery(new Term("base", "s:i")),
  	        new SpanTermQuery(new Term("base", "s:j"))
	      )
            )
        );

	KorapResult kr = ki.search(sq, (short) 10);

	assertEquals("totalResults", 0, kr.totalResults());
    };


    @Test
    public void indexExample7 () throws IOException {
	KorapIndex ki = new KorapIndex();
	// 4,5,11,13
	// x<a>x h</a>i j h<a>i j</a>
	FieldDocument fd = new FieldDocument();
	fd.addTV("base",
		 "xx hi j hi j",
		 "[(0-1)s:x|i:x|_0#0-1|-:a$<i>2|-:t$<i>8]" +
		 "[(1-2)s:x|i:x|_1#1-2|<>:a#1-4$<i>3]" +
		 "[(3-4)s:h|i:h|_2#3-4]" +
		 "[(4-5)s:i|i:i|_3#4-5]" +
		 "[(6-7)s:j|i:j|_4#6-7]" +
		 "[(8-9)s:h|i:h|_5#8-9]" +
		 "[(9-10)s:i|i:i|_6#9-10|<>:a#9-12$<i>8]" +
		 "[(11-12)s:j|i:j|_7#11-12]");
	ki.addDoc(fd);

	// Save documents
	ki.commit();

	assertEquals(1, ki.numberOf("documents"));

	SpanQuery sq = new SpanWithinQuery(
          new SpanElementQuery("base", "a"),
            new SpanNextQuery(
	      new SpanTermQuery(new Term("base", "s:h")),
	      new SpanNextQuery(
		new SpanTermQuery(new Term("base", "s:i")),
  	        new SpanTermQuery(new Term("base", "s:j"))
	      )
            )
        );

	KorapResult kr = ki.search(sq, (short) 10);

	assertEquals("totalResults", 0, kr.totalResults());
    };
    
    /** SpanElementQueries 
     * */
    @Test
    public void indexExample8() throws IOException{	   
	KorapIndex ki = new KorapIndex();		
	FieldDocument fd = new FieldDocument();
	// <a>xx <e>hi j <e>hi j</e></e></a>
	fd.addTV("base",
		 "xx hi j hi j",
		 "[(0-1)s:x|i:x|_0#0-1|<>:a#1-12$<i>8]" +
		 "[(1-2)s:x|i:x|_1#1-2]" +
		 "[(3-4)s:h|i:h|_2#3-4|<>:e#3-12$<i>8]" +
		 "[(4-5)s:i|i:i|_3#4-5]" +
		 "[(6-7)s:j|i:j|_4#6-7]" +
		 "[(8-9)s:h|i:h|_5#8-9|<>:e#8-9$<i>8]" +
		 "[(9-10)s:i|i:i|_6#9-10]" +
		 "[(11-12)s:j|i:j|_7#11-12]");
	ki.addDoc(fd);
    };
    
	/**
	 * @throws IOException */
    // TODO: TEMP::
    //@Test
    public void queryJSONpoly2() throws QueryException, IOException {
	
	String jsonPath = getClass().getResource("/queries/poly2.json").getFile();
	String jsonPQuery = readFile(jsonPath);		
	SpanQueryWrapper sqwi = new KorapQuery("tokens").fromJSON(
								  jsonPQuery
								  );
		
	SpanWithinQuery sq = (SpanWithinQuery) sqwi.toQuery();
	//		System.out.println(sq.toString());
		
	KorapIndex ki = new KorapIndex();
	ki.addDocFile(
		      getClass().getResource("/wiki/DDD-08370.json.gz").getFile(),true);
	ki.addDocFile(
		      getClass().getResource("/wiki/PPP-02924.json.gz").getFile(),true);
	ki.commit();
	KorapResult kr = ki.search(sq, (short) 10);
//		for (KorapMatch km : kr.getMatches()){		
//			System.out.println(km.getStartPos() +","+km.getEndPos()+" "
//				+km.getSnippetBrackets());
//		}		
	
	assertEquals(2, kr.getTotalResults());
	assertEquals(0, kr.getMatch(0).getLocalDocID());
	assertEquals(76, kr.getMatch(0).getStartPos());
	assertEquals(93, kr.getMatch(0).getEndPos());
	assertEquals(1, kr.getMatch(0).getLocalDocID());
	assertEquals(237, kr.getMatch(1).getStartPos());
	assertEquals(252, kr.getMatch(1).getEndPos());
    }
	
	
    private String readFile(String path) {
	StringBuilder sb = new StringBuilder();
	try {
	    BufferedReader in = new BufferedReader(new FileReader(path));
	    String str;
	    while ((str = in.readLine()) != null) {
		sb.append(str);
	    };
	    in.close();
	} catch (IOException e) {
	    fail(e.getMessage());
	}
	return sb.toString();
    };
};
