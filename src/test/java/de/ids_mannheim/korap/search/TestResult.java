package de.ids_mannheim.korap.search;

import java.util.*;
import java.io.*;

import org.apache.lucene.search.spans.SpanQuery;

import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.KrillQuery;
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.response.Match;
import de.ids_mannheim.korap.query.QueryBuilder;
import de.ids_mannheim.korap.index.FieldDocument;

import de.ids_mannheim.korap.util.QueryException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import static de.ids_mannheim.korap.Test.*;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestResult {

    @Test
    public void checkJSONResult () throws Exception {
        KrillIndex ki = new KrillIndex();
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-1");
        fd.addString("UID", "1");
        fd.addTV("base", "abab", "[(0-1)s:a|i:a|_0#0-1|-:t$<i>4]"
                + "[(1-2)s:b|i:b|_1#1-2]" + "[(2-3)s:a|i:c|_2#2-3]"
                + "[(3-4)s:b|i:a|_3#3-4]");
        ki.addDoc(fd);
        fd = new FieldDocument();
        fd.addString("ID", "doc-2");
        fd.addString("UID", "2");
        fd.addTV("base", "aba", "[(0-1)s:a|i:a|_0#0-1|-:t$<i>3]"
                + "[(1-2)s:b|i:b|_1#1-2]" + "[(2-3)s:a|i:c|_2#2-3]");
        ki.addDoc(fd);

        // Commit!
        ki.commit();

        QueryBuilder kq = new QueryBuilder("base");
        SpanQuery q = (SpanQuery) kq.or(kq._(1, kq.seg("s:a")))
                .or(kq._(2, kq.seg("s:b"))).toQuery();
        Result kr = ki.search(q);
        assertEquals((long) 7, kr.getTotalResults());

        ObjectMapper mapper = new ObjectMapper();
        JsonNode res = mapper.readTree(kr.toJsonString());
        assertEquals(7, res.at("/meta/totalResults").asInt());
        assertEquals("spanOr([{1: base:s:a}, {2: base:s:b}])",
                res.at("/meta/serialQuery").asText());
        assertEquals(0, res.at("/startIndex").asInt());
        assertEquals(25, res.at("/meta/itemsPerPage").asInt());
        assertEquals("token", res.at("/meta/context/left/0").asText());
        assertEquals(6, res.at("/meta/context/left/1").asInt());
        assertEquals("token", res.at("/meta/context/right/0").asText());
        assertEquals(6, res.at("/meta/context/right/1").asInt());

        assertEquals("base", res.at("/matches/0/field").asText());
        /*
          Probably a Jackson bug
          assertTrue(res.at("/matches/0/startMore").asBoolean());
          assertTrue(res.at("/matches/0/endMore").asBoolean());
        */
        assertEquals(1, res.at("/matches/0/UID").asInt());
        assertEquals("doc-1", res.at("/matches/0/docID").asText());
        assertEquals("match-doc-1-p0-1(1)0-0", res.at("/matches/0/matchID")
                .asText());
        assertEquals(
                "<span class=\"context-left\"></span><mark><mark class=\"class-1 level-0\">a</mark></mark><span class=\"context-right\">bab</span>",
                res.at("/matches/0/snippet").asText());

        assertEquals("base", res.at("/matches/6/field").asText());
        /*
          Probably a Jackson bug
          assertEquals(true, res.at("/matches/6/startMore").asBoolean());
          assertEquals(true, res.at("/matches/6/endMore").asBoolean());
        */
        assertEquals(2, res.at("/matches/6/UID").asInt());
        assertEquals("doc-2", res.at("/matches/6/docID").asText());
        assertEquals("match-doc-2-p2-3(1)2-2", res.at("/matches/6/matchID")
                .asText());
        assertEquals(
                "<span class=\"context-left\">ab</span><mark><mark class=\"class-1 level-0\">a</mark></mark><span class=\"context-right\"></span>",
                res.at("/matches/6/snippet").asText());
    };


    @Test
    public void checkJSONResultWarningBug () throws Exception {
        KrillIndex ki = new KrillIndex();
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-1");
        fd.addString("UID", "1");
        fd.addTV("tokens", "abab", "[(0-1)s:a|i:a|_0#0-1|-:t$<i>4]"
                + "[(1-2)s:b|i:b|_1#1-2]" + "[(2-3)s:a|i:c|_2#2-3]"
                + "[(3-4)s:b|i:a|_3#3-4]");
        ki.addDoc(fd);
        ki.commit();

        String json = getString(getClass().getResource(
                "/queries/bugs/optionality_warning.jsonld").getFile());
        Krill ks = new Krill(json);

        Result kr = ks.apply(ki);
        assertEquals((long) 2, kr.getTotalResults());

        ObjectMapper mapper = new ObjectMapper();
        JsonNode res = mapper.readTree(kr.toJsonString());

        // Old:
        // assertEquals("Optionality of query is ignored", res.at("/warning").asText());
        assertEquals("Optionality of query is ignored", res.at("/warnings/0/1")
                .asText());
    };


    @Test
    public void checkJSONResultForJSONInput () throws Exception {
        KrillIndex ki = new KrillIndex();
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-1");
        fd.addString("UID", "1");
        fd.addTV("tokens", "abab", "[(0-1)s:a|i:a|_0#0-1|-:t$<i>4]"
                + "[(1-2)s:b|i:b|_1#1-2]" + "[(2-3)s:a|i:c|_2#2-3]"
                + "[(3-4)s:b|i:a|_3#3-4]");
        ki.addDoc(fd);
        fd = new FieldDocument();
        fd.addString("ID", "doc-2");
        fd.addString("UID", "2");
        fd.addTV("tokens", "aba", "[(0-1)s:a|i:a|_0#0-1|-:t$<i>3]"
                + "[(1-2)s:b|i:b|_1#1-2]" + "[(2-3)s:a|i:c|_2#2-3]");
        ki.addDoc(fd);

        // Commit!
        ki.commit();

        String json = getString(getClass().getResource(
                "/queries/bsp-result-check.jsonld").getFile());
        Krill ks = new Krill(json);
        Result kr = ks.apply(ki);
        assertEquals((long) 7, kr.getTotalResults());

        ObjectMapper mapper = new ObjectMapper();
        JsonNode res = mapper.readTree(kr.toJsonString());

        assertEquals(7, res.at("/meta/totalResults").asInt());
        assertEquals("spanOr([tokens:s:a, tokens:s:b])",
                res.at("/meta/serialQuery").asText());
        assertEquals(5, res.at("/meta/itemsPerPage").asInt());
        assertEquals(0, res.at("/meta/startIndex").asInt());

        // Request meta
        // assertEquals(1, res.at("/request/meta/startPage").asInt());
        // assertEquals(5, res.at("/request/meta/count").asInt());
        /*
        assertEquals("token", res.at("/request/meta/context/left/0").asText());
        assertEquals(3, res.at("/request/meta/context/left/1").asInt());
        assertEquals("char", res.at("/request/meta/context/right/0").asText());
        assertEquals(6, res.at("/request/meta/context/right/1").asInt());
        */
        assertEquals("token", res.at("/meta/context/left/0").asText());
        assertEquals(3, res.at("/meta/context/left/1").asInt());
        assertEquals("char", res.at("/meta/context/right/0").asText());
        assertEquals(6, res.at("/meta/context/right/1").asInt());

        // Query
        assertEquals("koral:group", res.at("/query/@type").asText());
        assertEquals("operation:or", res.at("/query/operation").asText());

        assertEquals("koral:token", res.at("/query/operands/0/@type").asText());
        assertEquals("koral:term", res.at("/query/operands/0/wrap/@type")
                .asText());
        assertEquals("orth", res.at("/query/operands/0/wrap/layer").asText());
        assertEquals("a", res.at("/query/operands/0/wrap/key").asText());
        assertEquals("match:eq", res.at("/query/operands/0/wrap/match")
                .asText());
        assertEquals("koral:token", res.at("/query/operands/1/@type").asText());
        assertEquals("koral:term", res.at("/query/operands/1/wrap/@type")
                .asText());
        assertEquals("orth", res.at("/query/operands/1/wrap/layer").asText());
        assertEquals("b", res.at("/query/operands/1/wrap/key").asText());
        assertEquals("match:eq", res.at("/query/operands/1/wrap/match")
                .asText());

        // Matches
        assertEquals(1, res.at("/matches/0/UID").asInt());
        assertEquals("doc-1", res.at("/matches/0/docID").asText());
        assertEquals("match-doc-1-p0-1", res.at("/matches/0/matchID").asText());
        assertEquals(
                "<span class=\"context-left\"></span><mark>a</mark><span class=\"context-right\">bab</span>",
                res.at("/matches/0/snippet").asText());

        // No primaryData serialization
        assertTrue(res.at("/matches/0/primaryData").isMissingNode());
    };


    @Test
    public void checkJSONTokenResult () throws Exception {
        KrillIndex ki = new KrillIndex();
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-1");
        fd.addString("UID", "1");
        fd.addTV("base", "abab", "[(0-1)s:a|i:a|_0#0-1|-:t$<i>4]"
                + "[(1-2)s:b|i:b|_1#1-2]" + "[(2-3)s:a|i:c|_2#2-3]"
                + "[(3-4)s:b|i:a|_3#3-4]");
        ki.addDoc(fd);
        fd = new FieldDocument();
        fd.addString("ID", "doc-2");
        fd.addString("UID", "2");
        fd.addTV("base", "aba", "[(0-1)s:a|i:a|_0#0-1|-:t$<i>3]"
                + "[(1-2)s:b|i:b|_1#1-2]" + "[(2-3)s:a|i:c|_2#2-3]");
        ki.addDoc(fd);

        // Commit!
        ki.commit();

        QueryBuilder kq = new QueryBuilder("base");
        SpanQuery q = (SpanQuery) kq.seq(kq.seg("s:a")).append(kq.seg("s:b"))
                .toQuery();
        Result kr = ki.search(q);

        assertEquals((long) 3, kr.getTotalResults());
        ObjectMapper mapper = new ObjectMapper();
        JsonNode res = mapper.readTree(kr.toTokenListJsonString());

        assertEquals(3, res.at("/meta/totalResults").asInt());
        assertEquals("spanNext(base:s:a, base:s:b)", res
                .at("/meta/serialQuery").asText());
        assertEquals(0, res.at("/meta/startIndex").asInt());
        assertEquals(25, res.at("/meta/itemsPerPage").asInt());

        assertEquals("doc-1", res.at("/matches/0/textSigle").asText());
        assertEquals(0, res.at("/matches/0/tokens/0/0").asInt());
        assertEquals(1, res.at("/matches/0/tokens/0/1").asInt());
        assertEquals(1, res.at("/matches/0/tokens/1/0").asInt());
        assertEquals(2, res.at("/matches/0/tokens/1/1").asInt());

        assertEquals("doc-1", res.at("/matches/1/textSigle").asText());
        assertEquals(2, res.at("/matches/1/tokens/0/0").asInt());
        assertEquals(3, res.at("/matches/1/tokens/0/1").asInt());
        assertEquals(3, res.at("/matches/1/tokens/1/0").asInt());
        assertEquals(4, res.at("/matches/1/tokens/1/1").asInt());

        assertEquals("doc-2", res.at("/matches/2/textSigle").asText());
        assertEquals(0, res.at("/matches/2/tokens/0/0").asInt());
        assertEquals(1, res.at("/matches/2/tokens/0/1").asInt());
        assertEquals(1, res.at("/matches/2/tokens/1/0").asInt());
        assertEquals(2, res.at("/matches/2/tokens/1/1").asInt());
    };


    public static String getString (String path) {
        StringBuilder contentBuilder = new StringBuilder();
        try {
            BufferedReader in = new BufferedReader(new FileReader(path));
            String str;
            while ((str = in.readLine()) != null) {
                contentBuilder.append(str);
            };
            in.close();
        }
        catch (IOException e) {
            fail(e.getMessage());
        }
        return contentBuilder.toString();
    };
};
