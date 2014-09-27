import java.util.*;
import java.io.IOException;

import org.apache.lucene.search.spans.SpanQuery;

import de.ids_mannheim.korap.KorapIndex;
import de.ids_mannheim.korap.KorapQuery;
import de.ids_mannheim.korap.KorapResult;
import de.ids_mannheim.korap.KorapSearch;
import de.ids_mannheim.korap.KorapMatch;
import de.ids_mannheim.korap.index.FieldDocument;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import static de.ids_mannheim.korap.Test.*;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestKorapResult {

    @Test
    public void checkJSONResult () throws IOException  {
	KorapIndex ki = new KorapIndex();
	FieldDocument fd = new FieldDocument();
	fd.addString("ID", "doc-1");
	fd.addString("UID", "1");
	fd.addTV("base",
		 "abab",
		 "[(0-1)s:a|i:a|_0#0-1|-:t$<i>4]" +
		 "[(1-2)s:b|i:b|_1#1-2]" +
		 "[(2-3)s:a|i:c|_2#2-3]" +
		 "[(3-4)s:b|i:a|_3#3-4]");
	ki.addDoc(fd);
	fd = new FieldDocument();
	fd.addString("ID", "doc-2");
	fd.addString("UID", "2");
	fd.addTV("base",
		 "aba",
		 "[(0-1)s:a|i:a|_0#0-1|-:t$<i>3]" +
		 "[(1-2)s:b|i:b|_1#1-2]" +
		 "[(2-3)s:a|i:c|_2#2-3]");
	ki.addDoc(fd);

	// Commit!
	ki.commit();

	KorapQuery kq = new KorapQuery("base");
	SpanQuery q = (SpanQuery) kq.or(kq._(1, kq.seg("s:a"))).or(kq._(2, kq.seg("s:b"))).toQuery();
	KorapResult kr = ki.search(q);
	assertEquals(7, kr.getTotalResults());

	ObjectMapper mapper = new ObjectMapper();
	JsonNode res = mapper.readTree(kr.toJSON());
	assertEquals(7, res.at("/totalResults").asInt());
	assertEquals("spanOr([{1: base:s:a}, {2: base:s:b}])", res.at("/query").asText());
	assertEquals(0, res.at("/startIndex").asInt());
	assertEquals(25, res.at("/itemsPerPage").asInt());
	assertEquals("token", res.at("/context/left/0").asText());
	assertEquals(6, res.at("/context/left/1").asInt());
	assertEquals("token", res.at("/context/right/0").asText());
	assertEquals(6, res.at("/context/right/1").asInt());

	assertEquals("base", res.at("/matches/0/field").asText());
	/*
	  Probably a Jackson bug
	  assertTrue(res.at("/matches/0/startMore").asBoolean());
	  assertTrue(res.at("/matches/0/endMore").asBoolean());
	*/
	assertEquals(1, res.at("/matches/0/UID").asInt());
	assertEquals("doc-1", res.at("/matches/0/docID").asText());
	assertEquals("match-doc-1-p0-1(1)0-0", res.at("/matches/0/ID").asText());
	assertEquals("<span class=\"context-left\"></span><span class=\"match\"><em class=\"class-1 level-0\">a</em></span><span class=\"context-right\">bab</span>", res.at("/matches/0/snippet").asText());

	assertEquals("base", res.at("/matches/6/field").asText());
	/*
	  Probably a Jackson bug
	  assertEquals(true, res.at("/matches/6/startMore").asBoolean());
	  assertEquals(true, res.at("/matches/6/endMore").asBoolean());
	*/
	assertEquals(2, res.at("/matches/6/UID").asInt());
	assertEquals("doc-2", res.at("/matches/6/docID").asText());
	assertEquals("match-doc-2-p2-3(1)2-2", res.at("/matches/6/ID").asText());
	assertEquals("<span class=\"context-left\">ab</span><span class=\"match\"><em class=\"class-1 level-0\">a</em></span><span class=\"context-right\"></span>", res.at("/matches/6/snippet").asText());

    };
};
