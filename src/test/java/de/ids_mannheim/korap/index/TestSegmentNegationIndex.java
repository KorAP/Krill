package de.ids_mannheim.korap.index;

import static org.junit.Assert.assertEquals;

import org.apache.lucene.search.spans.SpanQuery;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;

import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.KrillQuery;
import de.ids_mannheim.korap.query.wrap.SpanSegmentQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanSequenceQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanQueryWrapper;
import de.ids_mannheim.korap.response.Result;

@RunWith(JUnit4.class)
public class TestSegmentNegationIndex {
    private SpanQuery sq;
    private KrillIndex ki;
    private Result kr;
    private FieldDocument fd;
    private Logger log;


    @Test
    public void testcaseNegation () throws Exception {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc0());
        ki.addDoc(createFieldDoc1());
        ki.addDoc(createFieldDoc2());
        ki.addDoc(createFieldDoc3());
        ki.commit();
        SpanSegmentQueryWrapper ssqw = new SpanSegmentQueryWrapper("tokens",
                "s:b");
        ssqw.with("s:c");
        SpanSequenceQueryWrapper sqw = new SpanSequenceQueryWrapper("tokens",
                ssqw).append("s:d");

        kr = ki.search(sqw.toQuery(), (short) 10);

        assertEquals("totalResults", kr.getTotalResults(), 2);
        // Match #0
        assertEquals("doc-number", 0, kr.getMatch(0).getLocalDocID());
        assertEquals("StartPos (0)", 4, kr.getMatch(0).startPos);
        assertEquals("EndPos (0)", 6, kr.getMatch(0).endPos);

        // Match #1 in the other atomic index
        assertEquals("doc-number", 3, kr.getMatch(1).getLocalDocID());
        assertEquals("StartPos (0)", 0, kr.getMatch(1).startPos);
        assertEquals("EndPos (0)", 2, kr.getMatch(1).endPos);

        ssqw = new SpanSegmentQueryWrapper("tokens", "s:b");
        ssqw.without("s:c");
        sqw = new SpanSequenceQueryWrapper("tokens", ssqw).append("s:a");

        kr = ki.search(sqw.toQuery(), (short) 10);

        assertEquals("doc-number", 0, kr.getMatch(0).getLocalDocID());
        assertEquals("StartPos (0)", 2, kr.getMatch(0).startPos);
        assertEquals("EndPos (0)", 4, kr.getMatch(0).endPos);

        assertEquals("doc-number", 1, kr.getMatch(1).getLocalDocID());
        assertEquals("StartPos (1)", 1, kr.getMatch(1).startPos);
        assertEquals("EndPos (1)", 3, kr.getMatch(1).endPos);

        assertEquals("doc-number", 1, kr.getMatch(2).getLocalDocID());
        assertEquals("StartPos (2)", 2, kr.getMatch(2).startPos);
        assertEquals("EndPos (2)", 4, kr.getMatch(2).endPos);

        assertEquals("doc-number", 2, kr.getMatch(3).getLocalDocID());
        assertEquals("StartPos (3)", 1, kr.getMatch(3).startPos);
        assertEquals("EndPos (3)", 3, kr.getMatch(3).endPos);
    }


    @Test
    public void testcaseWarnings () throws Exception {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc0());
        ki.addDoc(createFieldDoc1());
        ki.addDoc(createFieldDoc2());
        ki.addDoc(createFieldDoc3());
        ki.commit();

        kr = ki.search(new Krill(
                "{\"query\" : { \"@type\" : \"koral:token\", \"wrap\" : { \"@type\" : \"koral:term\", \"key\" : \"a\", \"flags\" : [\"caseInsensitive\"], \"layer\" : \"orth\", \"match\" : \"match:eq\" }}}"));
        assertEquals("totalResults", kr.getTotalResults(), 6);
        assertEquals("Warning", kr.hasWarnings(), true);
        assertEquals("Warning text", kr.getWarning(0).getMessage(),
                "Flag is unknown");
        assertEquals("Warning text", kr.getWarning(0).toJsonString(),
                "[748,\"Flag is unknown\",\"caseInsensitive\"]");

        // Negation of segment
        kr = ki.search(new Krill(
                "{\"query\" : { \"@type\" : \"koral:token\", \"wrap\" : { \"@type\" : \"koral:term\", \"key\" : \"a\", \"flags\" : [\"flags:caseInsensitive\"], \"layer\" : \"orth\", \"match\" : \"match:ne\" }}}"));

        assertEquals("totalResults", kr.getTotalResults(), 4);
        assertEquals("Warning", kr.hasWarnings(), true);
        assertEquals("Warning text", kr.getWarning(0).getMessage(),
                "Exclusivity of query is ignored");

        // Flag parameter injection
        kr = ki.search(new Krill(
                "{\"query\" : { \"@type\" : \"koral:token\", \"wrap\" : { \"@type\" : \"koral:term\", \"key\" : \"a\", \"flags\" : [{ \"injection\" : true }], \"layer\" : \"orth\", \"match\" : \"match:ne\" }}}"));

        assertEquals("totalResults", kr.getTotalResults(), 6);
        assertEquals("Warning", kr.hasWarnings(), true);
        assertEquals("Warning text", kr.getWarning(0).getMessage(),
                "Flag is unknown");
        assertEquals("Warning text", kr.getWarning(0).toJsonString(),
                "[748,\"Flag is unknown\"]");
    };


    @Test
    public void testAllNegationsInTermGroup () throws Exception {
        // [orth!="des" & orth!="ihres"] [orth="Hauses"]
        ki = new KrillIndex();

        FieldDocument fd1 = new FieldDocument();
        fd1.addString("ID", "doc-neg-0");
        fd1.addTV("tokens", "des Hauses",
                "[(0-3)s:des|i:des|_1$<i>0<i>1]"
                + "[(4-10)s:Hauses|i:hauses|_2$<i>1<i>2]");
        ki.addDoc(fd1);

        FieldDocument fd2 = new FieldDocument();
        fd2.addString("ID", "doc-neg-1");
        fd2.addTV("tokens", "ihres Hauses",
                "[(0-5)s:ihres|i:ihres|_1$<i>0<i>1]"
                + "[(6-12)s:Hauses|i:hauses|_2$<i>1<i>2]");
        ki.addDoc(fd2);

        FieldDocument fd3 = new FieldDocument();
        fd3.addString("ID", "doc-neg-2");
        fd3.addTV("tokens", "eines Hauses",
                "[(0-5)s:eines|i:eines|_1$<i>0<i>1]"
                + "[(6-12)s:Hauses|i:hauses|_2$<i>1<i>2]");
        ki.addDoc(fd3);

        FieldDocument fd4 = new FieldDocument();
        fd4.addString("ID", "doc-neg-3");
        fd4.addTV("tokens", "meines Hauses",
                "[(0-6)s:meines|i:meines|_1$<i>0<i>1]"
                + "[(7-13)s:Hauses|i:hauses|_2$<i>1<i>2]");
        ki.addDoc(fd4);

        ki.commit();

        // Search using KoralQuery JSON:
        // [orth!="des" & orth!="ihres"] [orth="Hauses"]
        String json = "{\"query\": {\"@type\": \"koral:group\", \"operands\": ["
                + "{\"@type\": \"koral:token\", \"wrap\": {"
                + "\"@type\": \"koral:termGroup\", \"operands\": ["
                + "{\"@type\": \"koral:term\", \"key\": \"des\", \"layer\": \"orth\", \"match\": \"match:ne\", \"type\": \"type:regex\"},"
                + "{\"@type\": \"koral:term\", \"key\": \"ihres\", \"layer\": \"orth\", \"match\": \"match:ne\", \"type\": \"type:regex\"}"
                + "], \"relation\": \"relation:and\"}},"
                + "{\"@type\": \"koral:token\", \"wrap\": {"
                + "\"@type\": \"koral:term\", \"key\": \"Hauses\", \"layer\": \"orth\", \"match\": \"match:eq\", \"type\": \"type:regex\"}}"
                + "], \"operation\": \"operation:sequence\"}}";

        Krill krill = new Krill(json);
        kr = ki.search(krill);

        assertEquals("totalResults", 2, kr.getTotalResults());
        assertEquals("StartPos (0)", 0, kr.getMatch(0).startPos);
        assertEquals("EndPos (0)", 2, kr.getMatch(0).endPos);
        assertEquals("StartPos (1)", 0, kr.getMatch(1).startPos);
        assertEquals("EndPos (1)", 2, kr.getMatch(1).endPos);
    }


    @Test
    public void testAllNegationsOrInTermGroup () throws Exception {
        // [orth!="des" | orth!="ihres"] [orth="Hauses"]
        // By De Morgan: NOT(des) OR NOT(ihres) = NOT(des AND ihres)
        // Since a token can only have one orth value,
        // (des AND ihres) is always false, so NOT(false) = true.
        // Every token matches, so all "[...] Hauses" docs match.
        ki = new KrillIndex();

        FieldDocument fd1 = new FieldDocument();
        fd1.addString("ID", "doc-neg-0");
        fd1.addTV("tokens", "des Hauses",
                "[(0-3)s:des|i:des|_1$<i>0<i>1]"
                + "[(4-10)s:Hauses|i:hauses|_2$<i>1<i>2]");
        ki.addDoc(fd1);

        FieldDocument fd2 = new FieldDocument();
        fd2.addString("ID", "doc-neg-1");
        fd2.addTV("tokens", "ihres Hauses",
                "[(0-5)s:ihres|i:ihres|_1$<i>0<i>1]"
                + "[(6-12)s:Hauses|i:hauses|_2$<i>1<i>2]");
        ki.addDoc(fd2);

        FieldDocument fd3 = new FieldDocument();
        fd3.addString("ID", "doc-neg-2");
        fd3.addTV("tokens", "eines Hauses",
                "[(0-5)s:eines|i:eines|_1$<i>0<i>1]"
                + "[(6-12)s:Hauses|i:hauses|_2$<i>1<i>2]");
        ki.addDoc(fd3);

        FieldDocument fd4 = new FieldDocument();
        fd4.addString("ID", "doc-neg-3");
        fd4.addTV("tokens", "meines Hauses",
                "[(0-6)s:meines|i:meines|_1$<i>0<i>1]"
                + "[(7-13)s:Hauses|i:hauses|_2$<i>1<i>2]");
        ki.addDoc(fd4);

        ki.commit();

        // [orth!="des" | orth!="ihres"] [orth="Hauses"]
        String json = "{\"query\": {\"@type\": \"koral:group\", \"operands\": ["
                + "{\"@type\": \"koral:token\", \"wrap\": {"
                + "\"@type\": \"koral:termGroup\", \"operands\": ["
                + "{\"@type\": \"koral:term\", \"key\": \"des\", \"layer\": \"orth\", \"match\": \"match:ne\", \"type\": \"type:regex\"},"
                + "{\"@type\": \"koral:term\", \"key\": \"ihres\", \"layer\": \"orth\", \"match\": \"match:ne\", \"type\": \"type:regex\"}"
                + "], \"relation\": \"relation:or\"}},"
                + "{\"@type\": \"koral:token\", \"wrap\": {"
                + "\"@type\": \"koral:term\", \"key\": \"Hauses\", \"layer\": \"orth\", \"match\": \"match:eq\", \"type\": \"type:regex\"}}"
                + "], \"operation\": \"operation:sequence\"}}";

        Krill krill = new Krill(json);
        kr = ki.search(krill);

        assertEquals("totalResults", 4, kr.getTotalResults());
    }


    @Test
    public void testAllNegationsOrMultiValuedLayer () throws Exception {
        // [marmot/p!=ADJ | marmot/p!=NN] [orth="Baum"]
        // By De Morgan: NOT(ADJ) OR NOT(NN) = NOT(ADJ AND NN)
        // A position CAN have multiple POS tags (e.g. ADJ and NN).
        // Only tokens with BOTH ADJ and NN are excluded.
        // However - this may be up to interpretation, as ADJ is !=NN and vice versa!
        ki = new KrillIndex();

        // Token "alte" has BOTH marmot/p:ADJ and marmot/p:NN
        FieldDocument fd1 = new FieldDocument();
        fd1.addString("ID", "doc-multi-0");
        fd1.addTV("tokens", "alte Baum",
                "[(0-4)s:alte|i:alte|marmot/p:ADJ|marmot/p:NN|_1$<i>0<i>1]"
                + "[(5-9)s:Baum|i:baum|_2$<i>1<i>2]");
        ki.addDoc(fd1);

        // Token "grosse" has only marmot/p:ADJ (not NN)
        FieldDocument fd2 = new FieldDocument();
        fd2.addString("ID", "doc-multi-1");
        fd2.addTV("tokens", "grosse Baum",
                "[(0-6)s:grosse|i:grosse|marmot/p:ADJ|_1$<i>0<i>1]"
                + "[(7-11)s:Baum|i:baum|_2$<i>1<i>2]");
        ki.addDoc(fd2);

        // Token "kleiner" has only marmot/p:NN (not ADJ)
        FieldDocument fd3 = new FieldDocument();
        fd3.addString("ID", "doc-multi-2");
        fd3.addTV("tokens", "kleiner Baum",
                "[(0-7)s:kleiner|i:kleiner|marmot/p:NN|_1$<i>0<i>1]"
                + "[(8-12)s:Baum|i:baum|_2$<i>1<i>2]");
        ki.addDoc(fd3);

        // Token "der" has marmot/p:DET (neither ADJ nor NN)
        FieldDocument fd4 = new FieldDocument();
        fd4.addString("ID", "doc-multi-3");
        fd4.addTV("tokens", "der Baum",
                "[(0-3)s:der|i:der|marmot/p:DET|_1$<i>0<i>1]"
                + "[(4-8)s:Baum|i:baum|_2$<i>1<i>2]");
        ki.addDoc(fd4);

        ki.commit();

        // [marmot/p!=ADJ | marmot/p!=NN] [orth="Baum"]
        // De Morgan: NOT(ADJ AND NN) - only exclude tokens with BOTH
        String json = "{\"query\": {\"@type\": \"koral:group\", \"operands\": ["
                + "{\"@type\": \"koral:token\", \"wrap\": {"
                + "\"@type\": \"koral:termGroup\", \"operands\": ["
                + "{\"@type\": \"koral:term\", \"foundry\": \"marmot\", \"key\": \"ADJ\", \"layer\": \"pos\", \"match\": \"match:ne\", \"type\": \"type:regex\"},"
                + "{\"@type\": \"koral:term\", \"foundry\": \"marmot\", \"key\": \"NN\", \"layer\": \"pos\", \"match\": \"match:ne\", \"type\": \"type:regex\"}"
                + "], \"relation\": \"relation:or\"}},"
                + "{\"@type\": \"koral:token\", \"wrap\": {"
                + "\"@type\": \"koral:term\", \"key\": \"Baum\", \"layer\": \"orth\", \"match\": \"match:eq\", \"type\": \"type:regex\"}}"
                + "], \"operation\": \"operation:sequence\"}}";

        Krill krill = new Krill(json);
        kr = ki.search(krill);

        // doc-multi-0: "alte" has BOTH ADJ and NN -> ADJ AND NN = true
        //   -> NOT(true) = false -> excluded
        // doc-multi-1: "grosse" has only ADJ -> ADJ AND NN = false
        //   -> NOT(false) = true -> matches
        // doc-multi-2: "kleiner" has only NN -> ADJ AND NN = false
        //   -> NOT(false) = true -> matches
        // doc-multi-3: "der" has DET -> ADJ AND NN = false
        //   -> NOT(false) = true -> matches
        assertEquals("totalResults", 3, kr.getTotalResults());
    }


    private FieldDocument createFieldDoc0 () {
        fd = new FieldDocument();
        fd.addString("ID", "doc-0");
        fd.addTV("tokens", "bcbabd", "[(0-1)s:b|i:b|_1$<i>0<i>1]"
                + "[(1-2)s:c|i:c|s:b|_2$<i>1<i>2]"
                + "[(2-3)s:b|i:b|_3$<i>2<i>3|<>:e$<b>64<i>2<i>4<i>4<b>0]"
                + "[(3-4)s:a|i:a|_4$<i>3<i>4|<>:e$<b>64<i>3<i>5<i>5<b>0|"
                + "<>:e2$<b>64<i>3<i>5<i>5<b>0]"
                + "[(4-5)s:b|i:b|s:c|_5$<i>4<i>5]"
                + "[(5-6)s:d|i:d|_6$<i>5<i>6|<>:e2$<b>64<i>5<i>6<i>6<b>0]");
        return fd;
    }


    private FieldDocument createFieldDoc1 () {
        fd = new FieldDocument();
        fd.addString("ID", "doc-1");
        fd.addTV("tokens", "babaa", "[(0-1)s:b|i:b|s:c|_1$<i>0<i>1]"
                + "[(1-2)s:a|i:a|s:b|_2$<i>1<i>2|<>:e$<b>64<i>1<i>3<i>3<b>0]"
                + "[(2-3)s:b|i:b|s:a|_3$<i>2<i>3]"
                + "[(3-4)s:a|i:a|_4$<i>3<i>4]" + "[(4-5)s:a|i:a|_5$<i>4<i>5]");
        return fd;
    }


    private FieldDocument createFieldDoc2 () {
        fd = new FieldDocument();
        fd.addString("ID", "doc-2");
        fd.addTV("tokens", "bdb",
                "[(0-1)s:b|i:b|_1$<i>0<i>1]" + "[(1-2)s:d|i:d|s:b|_2$<i>1<i>2]"
                        + "[(2-3)s:b|i:b|s:a|_3$<i>2<i>3]");
        return fd;
    }


    private FieldDocument createFieldDoc3 () {
        fd = new FieldDocument();
        fd.addString("ID", "doc-3");
        fd.addTV("tokens", "bdb", "[(0-1)s:b|i:b|s:c|_1$<i>0<i>1]"
                + "[(1-2)s:d|_2$<i>1<i>2]" + "[(2-3)s:d|i:d|_3$<i>2<i>3]");
        return fd;
    }
}
