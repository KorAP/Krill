package de.ids_mannheim.korap.index;

import java.util.*;
import java.io.*;

import de.ids_mannheim.korap.index.MultiTerm;
import de.ids_mannheim.korap.index.MultiTermToken;
import de.ids_mannheim.korap.query.wrap.SpanSegmentQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanRegexQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanSequenceQueryWrapper;
import de.ids_mannheim.korap.query.SpanWithinQuery;

import de.ids_mannheim.korap.util.CorpusDataException;

import static de.ids_mannheim.korap.Test.*;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.index.TermContext;

import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.LeafReaderContext;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanMultiTermQueryWrapper;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanNotQuery;
import org.apache.lucene.search.spans.NearSpansOrdered;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.RegexpQuery;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.store.SimpleFSDirectory; // temporary

import org.apache.lucene.util.Version;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Bits;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestIndex { // extends LuceneTestCase {
    // Create index in RAM
    //    private Directory index = new RAMDirectory();

    private Directory index = new RAMDirectory();


    @Test
    public void multiTerm () throws CorpusDataException {
        MultiTerm test = new MultiTerm("test");
        assertEquals("test", test.getTerm());
        assertEquals(null, test.getPayload());
        assertEquals(0, test.getStart());
        assertEquals(0, test.getEnd());
        assertFalse(test.hasStoredOffsets());
        assertEquals("test", test.toString());

        test = new MultiTerm("test#0-4");
        assertEquals("test", test.getTerm());
        assertEquals(null, test.getPayload());
        assertEquals(0, test.getStart());
        assertEquals(4, test.getEnd());
        assertFalse(test.hasStoredOffsets());
        assertEquals("test#0-4", test.toString());

        test = new MultiTerm("<>:s:test#0-4$<i>67");
        assertEquals("<>:s:test", test.getTerm());
        assertEquals("[0 0 0 43]", test.getPayload().toString());
        assertEquals(0, test.getStart());
        assertEquals(4, test.getEnd());
        assertFalse(test.hasStoredOffsets());
        assertTrue(test.toString().startsWith("<>:s:test#0-4$"));

        test = new MultiTerm("xip/l:\\#normal#0-5$<i>3999");
        assertEquals("xip/l:#normal", test.getTerm());
        assertEquals("[0 0 f 9f]", test.getPayload().toString());
        assertEquals(0, test.getStart());
        assertEquals(5, test.getEnd());
        assertFalse(test.hasStoredOffsets());
        assertTrue(test.toString().startsWith("xip/l:\\#normal#0-5$"));
    };


    @Test
    public void multiTermToken () throws CorpusDataException {
        MultiTermToken test = new MultiTermToken("hunde", "pos:n", "m:gen:pl");
        assertEquals("hunde", test.terms.get(0).term);
        assertEquals("pos:n", test.terms.get(1).term);
        assertEquals("m:gen:pl", test.terms.get(2).term);

        test = new MultiTermToken("hunde", "pos:n", "m:gen:pl");
        assertEquals("hunde", test.terms.get(0).term);
        assertEquals("pos:n", test.terms.get(1).term);
        assertEquals("m:gen:pl", test.terms.get(2).term);
    };


    private List initIndexer () throws IOException {
        List<Map<String, String>> list = new ArrayList<>();

        Map<String, String> d1 = new HashMap<String, String>();
        d1.put("id", "w1");
        d1.put("corpus", "wiki");
        d1.put("author", "Nils Diewald");
        d1.put("title", "Wikipedia");
        d1.put("subtitle", "A test");
        d1.put("pubDate", "20130701");
        d1.put("pubPlace", "Mannheim");
        d1.put("textClass", "news sports");
        d1.put("textStr", "Er nahm den Hunden die Angst.");
        d1.put("text", "Er#0-2|PPER|er|c:nom;p:3;n:sg;g:masc|<>:s#0-29$<i>7 "
                + "nahm#3-7|VVFIN|nehmen|p:3;n:sg;t:past;m:ind| "
                + "den#8-11|ART|der|c:acc;n:sg;g:masc| "
                + "Hunden#12-18|NN|hund|c:acc;n:sg;g:masc| "
                + "die#19-22|ART|der|c:nom;n:sg;g:fem| "
                + "Angst#23-28|NN|angst|c:nom;n:sg;g:fem| " + ".#28-29|$.|.||");
        list.add(d1);

        Map<String, String> d2 = new HashMap<String, String>();

        d2.put("id", "w2");
        d2.put("corpus", "wiki");
        d2.put("author", "Peter Thomas");
        d2.put("title", "Waldartikel");
        d2.put("subtitle", "Another test");
        d2.put("pubDate", "20130723");
        d2.put("pubPlace", "Bielefeld");
        d2.put("textClass", "news");
        d2.put("textStr", "Sie liefen durch den Wald.");
        d2.put("text", "Sie#0-3|PPER|sie|c:nom;p:3;n:pl;g:all|<>:s#0-26$<i>6 "
                + "liefen#4-10|VVFIN|laufen|p:3;n:pl;t:past;m:ind| "
                + "durch#11-16|APPR|durch|| "
                + "den#17-20|ART|der|c:acc;n:sg;g:masc| "
                + "Wald#21-25|NN|wald|c:acc;n:sg;g:masc| " + ".#25-26|$.|.||");
        list.add(d2);

        Map<String, String> d3 = new HashMap<String, String>();
        d3.put("id", "w3");
        d3.put("corpus", "zeitung");
        d3.put("author", "Michael Meier");
        d3.put("title", "Angst");
        d3.put("subtitle", "Starr vor Angst");
        d3.put("pubDate", "20130713");
        d3.put("pubPlace", "Bielefeld");
        d3.put("textClass", "sports");
        d3.put("textStr",
                "Er wagte nicht, sich zu ruehren. Er war starr vor Angst.");
        d3.put("text", "Er#0-2|PPER|er|c:nom;n:sg;g:masc;p:3|<>:s#0-32$<i>8 "
                + "wagte#3-8|VVFIN|wagen|p:3;n:sg;t:past;m:ind| "
                + "nicht#9-14|PTKNEG|nicht|| " + ",#14-15|$,|,|| "
                + "sich#16-20|PRF|sich|c:acc;p:3;n:sg| "
                + "zu#21-23|PTKZU|zu|| " + "ruehren#24-31|VVFIN|ruehren|| "
                + ".#31-32|$.|.|| "
                + "Er#33-35|PPER|er|c:nom;p:3;n:sg;g:masc|<>:s#33-56$<i>14 "
                + "war#36-39|VAFIN|sein|p:3;n:sg;t:past;m:ind| "
                + "starr#40-45|ADJD|starr|comp:pos| " + "vor#46-49|APPR|vor|| "
                + "Angst#50-55|NN|angst|c:dat;n:sg;g:fem| " + ".#55-56|$.|.||");
        list.add(d3);

        return list;
    };

    @Test
    public void indexLucene () throws Exception {

        // Base analyzer for searching and indexing
        StandardAnalyzer analyzer = new StandardAnalyzer();

        // Based on
        // http://lucene.apache.org/core/4_0_0/core/org/apache/lucene/
        // analysis/Analyzer.html?is-external=true

        // Create configuration with base analyzer
        IndexWriterConfig config = new IndexWriterConfig(analyzer);

        // Add a document 1 with the correct fields
        IndexWriter w = new IndexWriter(index, config);

        Collection docs = initIndexer();

        @SuppressWarnings("unchecked")
        Iterator<Map<String, String>> i = (Iterator<Map<String, String>>) docs
                .iterator();

        for (; i.hasNext();) {
            addDoc(w, i.next());
        };

        assertEquals(3, w.numDocs());

        w.close();

        // Check directory
        DirectoryReader reader = DirectoryReader.open(index);
        assertEquals(docs.size(), reader.maxDoc());
        assertEquals(docs.size(), reader.numDocs());

        // Check searcher
        IndexSearcher searcher = new IndexSearcher(reader);

        // textClass
        // All texts of text class "news"
        assertEquals(2,
                searcher.search(new TermQuery(new Term("textClass", "news")),
                        10).totalHits);

        // textClass
        // All texts of text class "sports"
        assertEquals(2,
                searcher.search(new TermQuery(new Term("textClass", "sports")),
                        10).totalHits);

        // TextIndex
        // All docs containing "l:nehmen"
        assertEquals(1,
                searcher.search(new TermQuery(new Term("text", "l:nehmen")),
                        10).totalHits);

        // TextIndex
        // All docs containing "s:den"
        assertEquals(2,
                searcher.search(new TermQuery(new Term("text", "s:den")),
                        10).totalHits);

        /*
        assertEquals(3,
              searcher.search(
                new TermQuery(
                  new Term("text", "T")
            ), 10
          ).totalHits
            );
        */

        // BooleanQuery
        // All docs containing "s:den" and "l:sie"
        TermQuery s_den = new TermQuery(new Term("text", "s:den"));
        TermQuery l_sie = new TermQuery(new Term("text", "l:sie"));
        BooleanQuery bool = new BooleanQuery();
        bool.add(s_den, BooleanClause.Occur.MUST);
        bool.add(l_sie, BooleanClause.Occur.MUST);

        assertEquals(1, searcher.search(bool, 10).totalHits);

        // BooleanQuery
        // All docs containing "s:den" or "l:sie"
        bool = new BooleanQuery();
        bool.add(s_den, BooleanClause.Occur.SHOULD);
        bool.add(l_sie, BooleanClause.Occur.SHOULD);
        assertEquals(2, searcher.search(bool, 10).totalHits);


        // RegexpQuery
        // All docs containing ".{4}en" (liefen und Hunden)
        RegexpQuery srquery = new RegexpQuery(new Term("text", "s:.{4}en"));
        assertEquals(2, searcher.search(srquery, 10).totalHits);

        // RegexpQuery
        // All docs containing "E." (Er) (2x)
        srquery = new RegexpQuery(new Term("text", "s:E."));
        assertEquals(2, searcher.search(srquery, 10).totalHits);

        SpanRegexQueryWrapper ssrquery = new SpanRegexQueryWrapper("text",
                "s:E.");
        assertEquals(2, searcher.search(ssrquery.toQuery(), 10).totalHits);


        // RegexpQuery
        // All docs containing "E." (er) (0x)
        srquery = new RegexpQuery(new Term("text", "s:e."));
        assertEquals(0, searcher.search(srquery, 10).totalHits);

        ssrquery = new SpanRegexQueryWrapper("text", "s:e.");
        assertEquals(0, searcher.search(ssrquery.toQuery(), 10).totalHits);

        // RegexpQuery
        // All docs containing "E."/i ([Ee]r) (2x)
        srquery = new RegexpQuery(new Term("text", "i:e."));
        assertEquals(2, searcher.search(srquery, 10).totalHits);

        ssrquery = new SpanRegexQueryWrapper("text", "s:e.", true);
        assertEquals("SpanMultiTermQueryWrapper(text:/i:e./)",
                ssrquery.toQuery().toString());
        assertEquals(2, searcher.search(ssrquery.toQuery(), 10).totalHits);

        // All docs containing "ng"/x (Angst) (2x)
        srquery = new RegexpQuery(new Term("text", "s:.*ng.*"));
        assertEquals(2, searcher.search(srquery, 10).totalHits);

        // All docs containing "ng"/x (optional Regex enabled by default)
        srquery = new RegexpQuery(new Term("text", "s:@ng@"));
        assertEquals(2, searcher.search(srquery, 10).totalHits);

        // All docs containing "@ng@" (no optional query operators enabled)
        ssrquery = new SpanRegexQueryWrapper("text", "s:@ng@");
        assertEquals(0, searcher.search(ssrquery.toQuery(), 10).totalHits);

        // Check http://comments.gmane.org/gmane.comp.jakarta.lucene.user/52283
        // for Carstens question on wildcards
		// Wildcardquery
        // All docs containing ".{4}en" (liefen und Hunden)
        WildcardQuery swquery = new WildcardQuery(new Term("text", "s:*ng*"));
		assertEquals("text:s:*ng*", swquery.toString());
		assertEquals(2, searcher.search(swquery, 10).totalHits);

        // [base=angst]
        SpanTermQuery stq = new SpanTermQuery(new Term("text", "l:angst"));
        assertEquals(2, searcher.search(srquery, 10).totalHits);

        // vor Angst
        // [orth=vor][orth=Angst]
        SpanNearQuery snquery = new SpanNearQuery(
                new SpanQuery[] { new SpanTermQuery(new Term("text", "s:vor")),
                        new SpanTermQuery(new Term("text", "s:Angst")) },
                1, true);
        assertEquals(1, searcher.search(snquery, 10).totalHits);

        // Spannearquery [p:VVFIN][]{,5}[m:nom:sg:fem]
        snquery = new SpanNearQuery(
                new SpanQuery[] {
                        new SpanTermQuery(new Term("text", "p:VVFIN")),
                        new SpanSegmentQueryWrapper("text", "m:c:nom", "m:n:sg",
                                "m:g:fem").toQuery() },
                5,     // slop
                true   // inOrder
            // Possible: CollectPayloads
        );
        assertEquals(1, searcher.search(snquery, 10).totalHits);


        // Spannearquery [p:VVFIN][m:acc:sg:masc]
        snquery = new SpanNearQuery(new SpanQuery[] { new SpanTermQuery(
                new Term("text", "p:VVFIN")),
                new SpanNearQuery(
                        new SpanQuery[] {
                                new SpanTermQuery(new Term("text", "m:c:acc")),
                                new SpanNearQuery(
                                        new SpanQuery[] {
                                                new SpanTermQuery(new Term(
                                                        "text", "m:n:sg")),
                                                new SpanTermQuery(new Term(
                                                        "text", "m:g:masc")) },
                                        -1, false) },
                        -1,     // slop
                        false   // inOrder
                // Possible: CollectPayloads
                )
                // new SpanTermQuery(new Term("text", "m:-acc:--sg:masc"))
        }, 0,     // slop
                true   // inOrder
            // Possible: CollectPayloads
        );
        assertEquals(1, searcher.search(snquery, 10).totalHits);


        // Spannearquery [p:VVFIN|m:3:sg:past:ind]
        // Exact match!
        snquery = new SpanNearQuery(
                new SpanQuery[] {
                        new SpanTermQuery(new Term("text", "p:VVFIN")),
                        new SpanNearQuery(new SpanQuery[] {
                                new SpanTermQuery(new Term("text", "m:p:3")),
                                new SpanNearQuery(new SpanQuery[] {
                                        new SpanTermQuery(
                                                new Term("text", "m:n:sg")),
                                        new SpanNearQuery(
                                                new SpanQuery[] {
                                                        new SpanTermQuery(
                                                                new Term("text",
                                                                        "m:t:past")),
                                                        new SpanTermQuery(
                                                                new Term("text",
                                                                        "m:m:ind")), },
                                                -1, false) },
                                        -1, false) },
                                -1, false) },
                // new SpanTermQuery(new Term("text", "m:---3:--sg:past:-ind"))
                -1,     // slop
                false   // inOrder
        // Possible: CollectPayloads
        );
        assertEquals(2, searcher.search(snquery, 10).totalHits);

        // To make sure, this is not equal:
        // Spannearquery [p:VVFIN & m:3:sg:past:ind]
        // Exact match!
        // Maybe it IS equal
        snquery = new SpanNearQuery(
                new SpanQuery[] {
                        new SpanTermQuery(new Term("text", "p:VVFIN")),
                        new SpanTermQuery(new Term("text", "m:p:3")),
                        new SpanTermQuery(new Term("text", "m:n:sg")),
                        new SpanTermQuery(new Term("text", "m:t:past")),
                        new SpanTermQuery(new Term("text", "m:m:ind")), },
                -1,     // slop
                false   // inOrder
        // Possible: CollectPayloads
        );
        assertNotEquals(2, searcher.search(snquery, 10).totalHits);
        // assertEquals(2, searcher.search(snquery, 10).totalHits);

        // Spannearquery [p:VVFIN & m:3:sg & past:ind]
        SpanSegmentQueryWrapper sniquery = new SpanSegmentQueryWrapper("text",
                "p:VVFIN", "m:p:3", "m:n:sg", "m:t:past", "m:m:ind");
        assertEquals(2, searcher.search(sniquery.toQuery(), 10).totalHits);


        // Todo:

        /*
        sniquery = new SpanSegmentQuery(
              "text",
          "p:VVFIN",
          "m:p:3",
          "m:n:sg",
          "m:t:past",
          "m:m:ind"
            );
        */

        // Spannearquery [p:VVFIN][]{,5}[m:nom:sg:fem]
        snquery = new SpanNearQuery(
                new SpanQuery[] {
                        new SpanTermQuery(new Term("text", "p:VVFIN")),
                        new SpanSegmentQueryWrapper("text", "m:c:nom", "m:n:sg",
                                "m:g:fem").toQuery() },
                5,     // slop
                true   // inOrder
            // Possible: CollectPayloads
        );
        assertEquals(1, searcher.search(snquery, 10).totalHits);

        sniquery = new SpanSegmentQueryWrapper("text", "p:VVFIN", "m:p:3",
                "m:t:past", "m:m:ind", "m:n:sg");
        assertEquals(2, searcher.search(sniquery.toQuery(), 10).totalHits);

        // [p = VVFIN & m:p = 3 & m:t = past & m:n != pl] or
        // [p = VVFIN & m:p = 3 & m:t = past & !m:n = pl]
        // TODO: Problem: What should happen in case the category does not exist?
        // pssible solution: & ( m:n != pl & exists(m:n))
        sniquery = new SpanSegmentQueryWrapper("text", "p:VVFIN", "m:p:3",
                "m:t:past");
        SpanQuery snqquery = new SpanNotQuery(sniquery.toQuery(),
                new SpanTermQuery(new Term("text", "m:n:pl")));
        assertEquals(2, searcher.search(snqquery, 10).totalHits);

        // [p = NN & (m:c: = dat | m:c = acc)]
        snquery = new SpanNearQuery(
                new SpanQuery[] { new SpanTermQuery(new Term("text", "p:NN")),
                        new SpanOrQuery(
                                new SpanTermQuery(new Term("text", "m:c:nom")),
                                new SpanTermQuery(
                                        new Term("text", "m:c:acc"))) },
                -1, false);

        assertEquals(2, searcher.search(snqquery, 10).totalHits);

        // [p = NN & !(m:c: = nom | m:c = acc)]
        snqquery = new SpanNotQuery(new SpanTermQuery(new Term("text", "p:NN")),
                new SpanOrQuery(new SpanTermQuery(new Term("text", "m:c:nom")),
                        new SpanTermQuery(new Term("text", "m:c:acc"))));
        assertEquals(1, searcher.search(snqquery, 10).totalHits);

        // [p = NN & !(m:c = nom)]
        snqquery = new SpanNotQuery(new SpanTermQuery(new Term("text", "p:NN")),
                new SpanTermQuery(new Term("text", "m:c:nom")));
        assertEquals(3, searcher.search(snqquery, 10).totalHits);

        // [p=NN & !(m:c = acc)]
        snqquery = new SpanNotQuery(new SpanTermQuery(new Term("text", "p:NN")),
                new SpanTermQuery(new Term("text", "m:c:acc")));
        assertEquals(2, searcher.search(snqquery, 10).totalHits);

        // [p=PPER][][p=ART]
        snquery = new SpanNearQuery(
                new SpanQuery[] { new SpanTermQuery(new Term("text", "p:PPER")),
                        new SpanNearQuery(new SpanQuery[] {
                                new SpanTermQuery(new Term("text", "T")),
                                new SpanTermQuery(new Term("text", "p:ART")) },
                                0, true), },
                0, true);
        assertEquals(1, searcher.search(snquery, 10).totalHits);


        // Todo:
        // [orth=się][]{2,4}[base=bać]
        // [orth=się][orth!="[.!?,:]"]{,5}[base=bać]|[base=bać][base="on|ja|ty|my|wy"]?[orth=się]
        // [pos=subst & orth="a.*"]{2}
        // [tag=subst:sg:nom:n]
        // [case==acc & case==gen] ??
        // [case~acc & case~gen]
        // [case~~acc]
        // [base=bać][orth!=się]+[orth=się] within s

        // [][][p:VAFIN] within s
        // [][p:VAFIN] within s


        // [][][p:VAFIN]
        snquery = new SpanNearQuery(
                new SpanQuery[] {
                        new SpanNearQuery(new SpanQuery[] {
                                new SpanTermQuery(new Term("text", "T")),
                                new SpanTermQuery(new Term("text", "T")) }, 0,
                                true),
                        new SpanTermQuery(new Term("text", "p:VAFIN")) },
                0, true);
        assertEquals(1, searcher.search(snquery, 10).totalHits);

        /*
        http://stackoverflow.com/questions/1311199/finding-the-position-of-search-hits-from-lucene
        */

        StringBuilder payloadString = new StringBuilder();
        Map<Term, TermContext> termContexts = new HashMap<>();
        for (LeafReaderContext atomic : reader.leaves()) {
            Bits bitset = atomic.reader().getLiveDocs();
            //	    Spans spans = NearSpansOrdered();
            Spans spans = snquery.getSpans(atomic, bitset, termContexts);

            while (spans.next()) {
                int docid = atomic.docBase + spans.doc();
                if (spans.isPayloadAvailable()) {
                    for (byte[] payload : spans.getPayload()) {
                        /* retrieve payload for current matching span */
                        payloadString.append(new String(payload));
                        payloadString.append(" | ");
                    };
                };
            };
        };
        //	assertEquals(33, payloadString.length());
        assertEquals(0, payloadString.length());



        // [][][p:VAFIN]
        // without collecting payloads
        snquery = new SpanNearQuery(
                new SpanQuery[] {
                        new SpanNearQuery(new SpanQuery[] {
                                new SpanTermQuery(new Term("text", "T")),
                                new SpanTermQuery(new Term("text", "T")) }, 0,
                                true, false),
                        new SpanTermQuery(new Term("text", "p:VAFIN")) },
                0, true, false);
        assertEquals(1, searcher.search(snquery, 10).totalHits);

        payloadString = new StringBuilder();
        termContexts = new HashMap<>();
        for (LeafReaderContext atomic : reader.leaves()) {
            Bits bitset = atomic.reader().getLiveDocs();
            //	    Spans spans = NearSpansOrdered();
            Spans spans = snquery.getSpans(atomic, bitset, termContexts);

            while (spans.next()) {
                int docid = atomic.docBase + spans.doc();
                for (byte[] payload : spans.getPayload()) {
                    /* retrieve payload for current matching span */
                    payloadString.append(new String(payload));
                    payloadString.append(" | ");
                };
            };
        };
        assertEquals(0, payloadString.length());


        // [][][p:VAFIN] in s
        // ([e:s:<][]*[T] | [T & e:s:<]) [T] ([p:VAFIN & e:s:>] | [T][]*[e:s:>]

        /*
        
        SpanSegmentWithinQuery ssequery = new SpanSegmentWithinQuery(
            "text","s", new SpanSegmentSequenceQuery("text", "T", "T", "p:VAFIN")
            );
        assertEquals(0, searcher.search(ssequery.toQuery(), 10).totalHits);
        
        payloadString = new StringBuilder();
        termContexts = new HashMap<>();
        for (LeafReaderContext atomic : reader.leaves()) {
            Bits bitset = atomic.reader().getLiveDocs();
            // Spans spans = NearSpansOrdered();
            Spans spans = ssequery.toQuery().getSpans(atomic, bitset, termContexts);
        
            while (spans.next()) {
        	int docid = atomic.docBase + spans.doc();
        	for (byte[] payload : spans.getPayload()) {
        	/// retrieve payload for current matching span
        	    payloadString.append(new String(payload));
        	    payloadString.append(" | ");
        	};
            };
        };
        assertEquals(0, payloadString.length(), 1);
        
        ssequery = new SpanSegmentWithinQuery(
            "text","s", new SpanSegmentSequenceQuery("text", "T", "p:VAFIN")
            );
        
        assertEquals("for " + ssequery.toQuery(),
        	     1, searcher.search(ssequery.toQuery(), 10).totalHits);
        
        payloadString = new StringBuilder();
        termContexts = new HashMap<>();
        for (LeafReaderContext atomic : reader.leaves()) {
            Bits bitset = atomic.reader().getLiveDocs();
            // Spans spans = NearSpansOrdered();
            Spans spans = ssequery.toQuery().getSpans(atomic, bitset, termContexts);
        
            while (spans.next()) {
        	int docid = atomic.docBase + spans.doc();
        	for (byte[] payload : spans.getPayload()) {
        	    // retrieve payload for current matching span
        	    payloadString.append(new String(payload));
        	    payloadString.append(" | ");
        	};
        	fail("Doc: " + docid + " with " + spans.start() + "-" + spans.end() + " || " + payloadString.toString());
            };
        };
        assertEquals(20, payloadString.length());
        
        */

        // --------------------______>



        //	Spans spans = MultiSpansWrapper.wrap(searcher.getTopReaderContext(), ssequery.toQuery());
        /*
        TopDocs topDocs = is.search(snq, 1);
        Set<String> payloadSet = new HashSet<String>();
        for (int i = 0; i < topDocs.scoreDocs.length; i++) {
          while (spans.next()) {
            Collection<byte[]> payloads = spans.getPayload();
        
            for (final byte [] payload : payloads) {
              payloadSet.add(new String(payload, "UTF-8"));
            }
          }
        }
        */


        /*
        Alternativ:
        IndexReader reader = writer.getReader();
        writer.close();
        IndexSearcher searcher = newSearcher(reader);
        
        PayloadSpanUtil psu = new PayloadSpanUtil(searcher.getTopReaderContext());
        
        Collection<byte[]> payloads = psu.getPayloadsForQuery(new TermQuery(new Term(PayloadHelper.FIELD, "rr")));
        if(VERBOSE)
          System.out.println("Num payloads:" + payloads.size());
        for (final byte [] bytes : payloads) {
          if(VERBOSE)
            System.out.println(new String(bytes, "UTF-8"));
        }
        */



        /* new: */

        // PayloadHelper helper = new PayloadHelper();

        // Map<Term, TermContext> termContexts = new HashMap<>();
        //Spans spans;
        //spans = snquery.getSpans(searcher.getIndexReader());
        //    searcher = helper.setUp(similarity, 1000);
        /*
        IndexReader reader = search.getReader(querycontainer.getFoundry());
        Spans luceneSpans;
        Bits bitset = atomic.reader().getLiveDocs();
        for (byte[] payload : luceneSpans.getPayload())
        
        /* Iterate over all matching documents */
        /*
            while (luceneSpans.next() && total < config.getMaxhits()) {
        	Span matchSpan;
        	StringBuilder payloadString = new StringBuilder();
        	int docid = atomic.docBase + luceneSpans.doc();
        	String docname = search.retrieveDocname(docid,
        					querycontainer.getFoundry());
        					total++;
        
        	for (byte[] payload : luceneSpans.getPayload())
        */
        /* retrieve payload for current matching span */
        //				payloadString.append(new String(payload));

        /* create span containing result */
        /*
        		matchSpan = new Span(docname);
        		matchSpan.setIndexdocid(docid);
        		matchSpan.setLayer(querycontainer.getLayer());
        		matchSpan.storePayloads(payloadString.toString());
        		matchSpans.add(matchSpan);
        */
        /*
         * topdocs = searcher.search(new ConstantScoreQuery(corpusQ add
         * position to list of positions to be considered for later
         * searches
         */
        /*
        validValues.put(docname,
        		matchSpan.getPayload(config.getPrefix()));
        }
        */


        // Todo: API made by add() typisiert für queries, strings

        // SpanPayloadCheckQuery for sentences!

        /* Support regular expression in SpanSegmentQuery */
        // new Regexp();
        // new Term();

        /*
          Vielleicht: spanSegmentQuery(new Term(), new Wildcard(), new Regex());
         */

        // And Not ->
        //	SpanTermDiffQuery

        /*
        SpanNearQuery poquery = new SpanNearQuery(
        
        );
        */

        reader.close();


    };
};
