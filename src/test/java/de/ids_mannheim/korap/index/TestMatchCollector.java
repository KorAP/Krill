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
import de.ids_mannheim.korap.KorapQuery;
import de.ids_mannheim.korap.KorapMatch;
import de.ids_mannheim.korap.index.MatchCollector;
import de.ids_mannheim.korap.KorapCollection;
import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.query.SpanNextQuery;
import de.ids_mannheim.korap.query.SpanClassQuery;
import de.ids_mannheim.korap.index.FieldDocument;
import de.ids_mannheim.korap.model.MultiTermTokenStream;

import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.index.Term;

// mvn -Dtest=TestWithinIndex#indexExample1 test

// match is shrink and split

@RunWith(JUnit4.class)
public class TestMatchCollector {

    @Test
    public void indexExample1 () throws IOException {
	KorapIndex ki = new KorapIndex();

	// abcabcabac
	FieldDocument fd = new FieldDocument();
	fd.addString("ID", "doc-1");
	fd.addString("UID", "1");
	fd.addTV("base",
		 "abcabcabac",
		 "[(0-1)s:a|i:a|_0#0-1|-:t$<i>10]" +
		 "[(1-2)s:b|i:b|_1#1-2]" +
		 "[(2-3)s:c|i:c|_2#2-3]" +
		 "[(3-4)s:a|i:a|_3#3-4]" +
		 "[(4-5)s:b|i:b|_4#4-5]" +
		 "[(5-6)s:c|i:c|_5#5-6]" +
		 "[(6-7)s:a|i:a|_6#6-7]" +
		 "[(7-8)s:b|i:b|_7#7-8]" +
		 "[(8-9)s:a|i:a|_8#8-9]" +
		 "[(9-10)s:c|i:c|_9#9-10]");
	ki.addDoc(fd);

	fd = new FieldDocument();
	fd.addString("ID", "doc-2");
	fd.addString("UID", "2");
	fd.addTV("base",
		 "bcbabd",			 
		 "[(0-1)s:b|i:b|_1#0-1]" +
		 "[(1-2)s:c|i:c|s:b|_2#1-2]" +			 
		 "[(2-3)s:b|i:b|_3#2-3|<>:e#2-4$<i>4]" +
		 "[(3-4)s:a|i:a|_4#3-4|<>:e#3-5$<i>5|<>:e2#3-5$<i>5]" + 
		 "[(4-5)s:b|i:b|s:c|_5#4-5]" +			 
		 "[(5-6)s:d|i:d|_6#5-6|<>:e2#5-6$<i>6]");
	ki.addDoc(fd);

	ki.commit();

	SpanQuery sq;

	sq = new SpanTermQuery(new Term("base", "s:b"));
    Krill krill = new Krill(sq);
    krill.getMeta().setCount((short) 10);
	MatchCollector mc = ki.collect(
	  krill,
	  new MatchCollector()
	);

	assertEquals(mc.getTotalResults(), 5);
	assertEquals(mc.getTotalResultDocs(), 2);
    };
};
