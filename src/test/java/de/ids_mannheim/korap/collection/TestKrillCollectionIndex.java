package de.ids_mannheim.korap.collection;

import java.io.IOException;

import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.KrillCollection;
import de.ids_mannheim.korap.collection.CollectionBuilder;
import de.ids_mannheim.korap.index.FieldDocument;
import de.ids_mannheim.korap.index.TextAnalyzer;
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.response.SearchContext;
import de.ids_mannheim.korap.util.StatusCodes;
import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.query.QueryBuilder;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestKrillCollectionIndex {
    private KrillIndex ki;


    @Test
    public void testKrillCollectionWithWrongJson () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createDoc1());
        ki.addDoc(createDoc2());
        ki.addDoc(createDoc3());
        ki.commit();

        KrillCollection kc = new KrillCollection("{lalala}");
		assertEquals("Unable to parse JSON", kc.getError(0).getMessage());
        kc.setIndex(ki);

        long docs = 0, tokens = 0, sentences = 0, paragraphs = 0;
        try {
            docs = kc.numberOf("documents");
            tokens = kc.numberOf("tokens");
            sentences = kc.numberOf("sentences");
            paragraphs = kc.numberOf("paragraphs");
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        assertEquals(0, docs);
        assertEquals(0, tokens);
        assertEquals(0, sentences);
        assertEquals(0, paragraphs);
        
        assertEquals(1, kc.getErrors().size());
        assertEquals(StatusCodes.UNABLE_TO_PARSE_JSON, kc.getErrors().get(0).getCode());
    }


    @Test
    public void testIndexWithCollectionBuilder () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createDoc1());
        ki.addDoc(createDoc2());
        ki.addDoc(createDoc3());
        ki.commit();
        CollectionBuilder cb = new CollectionBuilder();
        KrillCollection kcn = new KrillCollection(ki);

        // Simple string tests
        kcn.fromBuilder(cb.term("author", "Frank"));
        assertEquals(1, kcn.docCount());

        kcn.fromBuilder(cb.term("author", "Peter"));
        assertEquals(1, kcn.docCount());

        kcn.fromBuilder(cb.term("author", "Sebastian"));
        assertEquals(1, kcn.docCount());

        kcn.fromBuilder(cb.term("author", "Michael"));
        assertEquals(0, kcn.docCount());

        kcn.fromBuilder(cb.term("nothing", "nothing"));
        assertEquals(0, kcn.docCount());

        kcn.fromBuilder(cb.term("textClass", "reisen"));
        assertEquals(3, kcn.docCount());

        kcn.fromBuilder(cb.term("textClass", "kultur"));
        assertEquals(2, kcn.docCount());

        kcn.fromBuilder(cb.term("textClass", "finanzen"));
        assertEquals(1, kcn.docCount());

        // Simple orGroup tests
        kcn.fromBuilder(cb.orGroup().with(cb.term("author", "Frank"))
                .with(cb.term("author", "Michael")));
        assertEquals(1, kcn.docCount());

        kcn.fromBuilder(cb.orGroup().with(cb.term("author", "Frank"))
                .with(cb.term("author", "Sebastian")));
        assertEquals(2, kcn.docCount());

        kcn.fromBuilder(cb.orGroup().with(cb.term("author", "Frank"))
                .with(cb.term("author", "Sebastian"))
                .with(cb.term("author", "Peter")));
        assertEquals(3, kcn.docCount());

        kcn.fromBuilder(cb.orGroup().with(cb.term("author", "Huhu"))
                .with(cb.term("author", "Haha"))
                .with(cb.term("author", "Hehe")));
        assertEquals(0, kcn.docCount());

        // Multi field orGroup tests
        kcn.fromBuilder(cb.orGroup().with(cb.term("ID", "doc-1"))
                .with(cb.term("author", "Peter")));
        assertEquals(2, kcn.docCount());

        kcn.fromBuilder(cb.orGroup().with(cb.term("ID", "doc-1"))
                .with(cb.term("author", "Frank")));
        assertEquals(1, kcn.docCount());

        kcn.fromBuilder(cb.orGroup().with(cb.term("ID", "doc-1"))
                .with(cb.term("author", "Michael")));
        assertEquals(1, kcn.docCount());

        // Simple andGroup tests
        kcn.fromBuilder(cb.andGroup().with(cb.term("author", "Frank"))
                .with(cb.term("author", "Michael")));
        assertEquals(0, kcn.docCount());

        kcn.fromBuilder(cb.andGroup().with(cb.term("ID", "doc-1"))
                .with(cb.term("author", "Frank")));
        assertEquals(1, kcn.docCount());

        // andGroup in keyword field test
        kcn.fromBuilder(cb.andGroup().with(cb.term("textClass", "reisen"))
                .with(cb.term("textClass", "finanzen")));
        assertEquals(1, kcn.docCount());

        kcn.fromBuilder(cb.andGroup().with(cb.term("textClass", "reisen"))
                .with(cb.term("textClass", "kultur")));
        assertEquals(2, kcn.docCount());

        kcn.fromBuilder(cb.andGroup().with(cb.term("textClass", "finanzen"))
                .with(cb.term("textClass", "kultur")));
        assertEquals(0, kcn.docCount());

        kcn.fromBuilder(cb.term("text", "mann"));
        assertEquals(3, kcn.docCount());

        kcn.fromBuilder(cb.term("text", "frau"));
        assertEquals(1, kcn.docCount());
    };


	@Test
    public void testIndexWithRegex () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createDoc1());
        ki.addDoc(createDoc2());
        ki.addDoc(createDoc3());
        ki.commit();
        CollectionBuilder cb = new CollectionBuilder();
        KrillCollection kcn = new KrillCollection(ki);

		// Frank, Sebastian
		kcn.fromBuilder(cb.re("author", ".*an.*"));
        assertEquals(2, kcn.docCount());

		// Kultur & Reisen,
		// Reisen & Finanzen,
		// Nachricht & Kultur & Reisen
		kcn.fromBuilder(cb.re("textClass", ".*(ult|eis).*"));
        assertEquals(3, kcn.docCount());

		// Test in group
		kcn.fromBuilder(
			cb.andGroup().with(cb.term("textClass", "reisen")).with(cb.term("textClass", "kultur"))
			);
        assertEquals(2, kcn.docCount());

		kcn.fromBuilder(
			cb.andGroup().with(
				cb.re("textClass", ".*eis.*")
				).with(
					cb.re("textClass", ".*ult.*")
					)
			);
        assertEquals(2, kcn.docCount());

		kcn.fromBuilder(
			cb.andGroup().with(
				cb.re("textClass", ".*eis.*")
				).with(
					cb.orGroup().with(
						cb.re("textClass", ".*ult.*")
						).with(
							cb.re("textClass", ".*nan.*")
							)
					)
			);
        assertEquals(3, kcn.docCount());
	};

	
    @Test
    public void testIndexWithNegation () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createDoc1());
        ki.addDoc(createDoc2());
        ki.addDoc(createDoc3());
        ki.commit();
        CollectionBuilder cb = new CollectionBuilder();
        KrillCollection kcn = new KrillCollection(ki);

        // Simple negation tests
        kcn.fromBuilder(cb.term("author", "Frank").not());
        assertEquals(2, kcn.docCount());

        kcn.fromBuilder(cb.term("textClass", "reisen").not());
        assertEquals(0, kcn.docCount());

        kcn.fromBuilder(cb.term("textClass", "kultur").not());
        assertEquals(1, kcn.docCount());

        // orGroup with simple Negation
        kcn.fromBuilder(cb.orGroup().with(cb.term("textClass", "kultur").not())
                .with(cb.term("author", "Peter")));
        assertEquals(2, kcn.docCount());

        kcn.fromBuilder(cb.orGroup().with(cb.term("textClass", "kultur").not())
                .with(cb.term("author", "Sebastian")));
        assertEquals(1, kcn.docCount());
    };


    @Test
    public void testIndexWithMultipleCommitsAndDeletes () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createDoc1());
        ki.addDoc(createDoc2());
        ki.commit();
        CollectionBuilder cb = new CollectionBuilder();
        KrillCollection kcn = new KrillCollection(ki);

        kcn.fromBuilder(cb.term("author", "Frank"));
        assertEquals(1, kcn.docCount());
        kcn.fromBuilder(cb.term("author", "Peter"));
        assertEquals(1, kcn.docCount());
        kcn.fromBuilder(cb.term("author", "Sebastian"));
        assertEquals(0, kcn.docCount());
        kcn.fromBuilder(cb.term("author", "Michael").not());
        assertEquals(2, kcn.docCount());

        // Add Sebastians doc
        ki.addDoc(createDoc3());
        ki.commit();

        kcn.fromBuilder(cb.term("author", "Frank"));
        assertEquals(1, kcn.docCount());
        kcn.fromBuilder(cb.term("author", "Peter"));
        assertEquals(1, kcn.docCount());
        kcn.fromBuilder(cb.term("author", "Sebastian"));
        assertEquals(1, kcn.docCount());
        kcn.fromBuilder(cb.term("author", "Michael").not());
        assertEquals(3, kcn.docCount());

        // Remove one document
        ki.delDocs("author", "Peter");
        ki.commit();

        kcn.fromBuilder(cb.term("author", "Frank"));
        assertEquals(1, kcn.docCount());
        kcn.fromBuilder(cb.term("author", "Peter"));
        assertEquals(0, kcn.docCount());
        kcn.fromBuilder(cb.term("author", "Sebastian"));
        assertEquals(1, kcn.docCount());
        kcn.fromBuilder(cb.term("author", "Michael").not());
        assertEquals(2, kcn.docCount());

        // Readd Peter's doc
        ki.addDoc(createDoc2());
        ki.commit();

        kcn.fromBuilder(cb.term("author", "Frank"));
        assertEquals(1, kcn.docCount());
        kcn.fromBuilder(cb.term("author", "Peter"));
        assertEquals(1, kcn.docCount());
        kcn.fromBuilder(cb.term("author", "Sebastian"));
        assertEquals(1, kcn.docCount());
        kcn.fromBuilder(cb.term("author", "Michael").not());
        assertEquals(3, kcn.docCount());
    };


    @Test
    public void testIndexStream () throws IOException {
        ki = new KrillIndex();
        FieldDocument fd = ki.addDoc(createDoc1());
        ki.commit();
        Analyzer ana = new TextAnalyzer();
        TokenStream ts = fd.doc.getField("text").tokenStream(ana, null);

        CharTermAttribute charTermAttribute = ts
                .addAttribute(CharTermAttribute.class);
        ts.reset();

        ts.incrementToken();
        assertEquals("der", charTermAttribute.toString());
        ts.incrementToken();
        assertEquals("alte", charTermAttribute.toString());
        ts.incrementToken();
        assertEquals("mann", charTermAttribute.toString());
        ts.incrementToken();
        assertEquals("ging", charTermAttribute.toString());
        ts.incrementToken();
        assertEquals("über", charTermAttribute.toString());
        ts.incrementToken();
        assertEquals("die", charTermAttribute.toString());
        ts.incrementToken();
        assertEquals("straße", charTermAttribute.toString());
    };


    @Test
    public void testIndexWithDateRanges () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createDoc1());
        ki.addDoc(createDoc2());
        ki.addDoc(createDoc3());
        ki.commit();
        CollectionBuilder cb = new CollectionBuilder();
        KrillCollection kcn = new KrillCollection(ki);

        kcn.fromBuilder(cb.date("pubDate", "2005"));
        assertEquals(3, kcn.docCount());
        kcn.fromBuilder(cb.date("pubDate", "2005-12"));
        assertEquals(3, kcn.docCount());

        kcn.fromBuilder(cb.date("pubDate", "2005-12-10"));
        assertEquals(1, kcn.docCount());
        kcn.fromBuilder(cb.date("pubDate", "2005-12-16"));
        assertEquals(1, kcn.docCount());
        kcn.fromBuilder(cb.date("pubDate", "2005-12-07"));
        assertEquals(1, kcn.docCount());

        kcn.fromBuilder(cb.since("pubDate", "2005-12-07"));
        assertEquals(3, kcn.docCount());
        kcn.fromBuilder(cb.since("pubDate", "2005-12-10"));
        assertEquals(2, kcn.docCount());
        kcn.fromBuilder(cb.since("pubDate", "2005-12-16"));
        assertEquals(1, kcn.docCount());

        kcn.fromBuilder(cb.till("pubDate", "2005-12-16"));
        assertEquals(3, kcn.docCount());
        kcn.fromBuilder(cb.till("pubDate", "2005-12-10"));
        assertEquals(2, kcn.docCount());
        kcn.fromBuilder(cb.till("pubDate", "2005-12-07"));
        assertEquals(1, kcn.docCount());

        kcn.fromBuilder(cb.date("pubDate", "2005-12-10").not());
        assertEquals(2, kcn.docCount());
        kcn.fromBuilder(cb.date("pubDate", "2005-12-16").not());
        assertEquals(2, kcn.docCount());
        kcn.fromBuilder(cb.date("pubDate", "2005-12-07").not());
        assertEquals(2, kcn.docCount());
        kcn.fromBuilder(cb.date("pubDate", "2005-12-09").not());
        assertEquals(3, kcn.docCount());


        kcn.fromBuilder(cb.till("pubDate", "2005-12-16").not());
        assertEquals(0, kcn.docCount());
        kcn.fromBuilder(cb.till("pubDate", "2005-12-15").not());
        assertEquals(1, kcn.docCount());
        kcn.fromBuilder(cb.till("pubDate", "2005-12-10").not());
        assertEquals(1, kcn.docCount());
        kcn.fromBuilder(cb.till("pubDate", "2005-12-09").not());
        assertEquals(2, kcn.docCount());
        kcn.fromBuilder(cb.till("pubDate", "2005-12-07").not());
        assertEquals(2, kcn.docCount());
        kcn.fromBuilder(cb.till("pubDate", "2005-12-06").not());
        assertEquals(3, kcn.docCount());
    };


    @Test
    public void testIndexWithRegexes () throws IOException {
        ki = new KrillIndex();

        ki.addDoc(createDoc1());
        ki.addDoc(createDoc2());
        ki.addDoc(createDoc3());
        ki.commit();

        CollectionBuilder cb = new CollectionBuilder();
        KrillCollection kcn = new KrillCollection(ki);

        kcn.fromBuilder(cb.re("author", "Fran.*"));
        assertEquals(1, kcn.docCount());
        kcn.fromBuilder(cb.re("author", "Blin.*"));
        assertEquals(0, kcn.docCount());
        kcn.fromBuilder(cb.re("author", "Frank|Peter"));
        assertEquals(2, kcn.docCount());

		// "Frau" requires text request!
		kcn.fromBuilder(cb.text("text", "Frau"));
        assertEquals(1, kcn.docCount());

        kcn.fromBuilder(cb.term("text", "frau"));
        assertEquals(1, kcn.docCount());

        kcn.fromBuilder(cb.re("text", "frau"));
        assertEquals(1, kcn.docCount());

        kcn.fromBuilder(cb.re("text", "frau|mann"));
        assertEquals(3, kcn.docCount());
    };

	@Test
    public void testIndexWithTextStringQueries () throws IOException {
		ki = new KrillIndex();
		ki.addDoc(createDoc1());
		ki.commit();

        CollectionBuilder cb = new CollectionBuilder();
        KrillCollection kcn = new KrillCollection(ki);

        kcn.fromBuilder(cb.term("text", "mann"));
        assertEquals(1, kcn.docCount());

		kcn.fromBuilder(cb.text("text", "Mann"));
        assertEquals(1, kcn.docCount());

		// Simple string tests
        kcn.fromBuilder(cb.text("text", "Der alte Mann"));

		// Uses german analyzer for the createDocument
		assertEquals(kcn.toString(), "QueryWrapperFilter(text:\"der alte mann\")");
		assertEquals(1, kcn.docCount());
	};


    @Test
    public void filterExampleFromLegacy () throws Exception {

        // Construct index
        KrillIndex ki = new KrillIndex();
        // Indexing test files
        for (String i : new String[] { "00001", "00002", "00003", "00004",
                "00005", "00006", "02439" }) {
            ki.addDoc(getClass().getResourceAsStream("/wiki/" + i + ".json.gz"),
                    true);
        };
        ki.commit();

        // Create Virtual collections:
        KrillCollection kc = new KrillCollection(ki);

        assertEquals("Documents", 7, kc.numberOf("documents"));

        // The virtual collection consists of all documents that have
        // the textClass "reisen" and "freizeit"

        /*        kc.filter(kf.and("textClass", "reisen").and("textClass",
                "freizeit-unterhaltung"));
        */

        kc.fromBuilder(kc.build().andGroup()
                .with(kc.build().term("textClass", "reisen"))
                .with(kc.build().term("textClass", "freizeit-unterhaltung")));

        assertEquals("Documents", 5, kc.numberOf("documents"));
        assertEquals("Tokens", 1678, kc.numberOf("tokens"));
        assertEquals("Sentences", 194, kc.numberOf("sentences"));
        assertEquals("Paragraphs", 139, kc.numberOf("paragraphs"));


        // Subset this to all documents that have also the text
        // kc.filter(kf.and("textClass", "kultur"));
        /*
        kc.fromBuilder(
          kc.build().andGroup().with(
            kc.getBuilder()
          ).with(
            kc.build().term("textClass", "kultur")
          )
        );
        */

        kc.filter(kc.build().term("textClass", "kultur"));

        assertEquals("Documents", 1, kc.numberOf("documents"));
        assertEquals("Tokens", 405, kc.numberOf("tokens"));
        assertEquals("Sentences", 75, kc.numberOf("sentences"));
        assertEquals("Paragraphs", 48, kc.numberOf("paragraphs"));


        // kc.filter(kf.and("corpusID", "WPD"));
        kc.filter(kc.build().term("corpusID", "WPD"));

        assertEquals("Documents", 1, kc.numberOf("documents"));
        assertEquals("Tokens", 405, kc.numberOf("tokens"));
        assertEquals("Sentences", 75, kc.numberOf("sentences"));
        assertEquals("Paragraphs", 48, kc.numberOf("paragraphs"));

        // Create a query
        Krill ks = new Krill(
                new QueryBuilder("tokens").seg("opennlp/p:NN").with("tt/p:NN"));
        ks.setCollection(kc).getMeta().setStartIndex(0).setCount((short) 20)
                .setContext(
                        new SearchContext(true, (short) 5, true, (short) 5));

        Result kr = ks.apply(ki);

        /*
        Result kr = ki.search(kc, query, 0, (short) 20, true, (short) 5, true,
                (short) 5);
        */
        assertEquals(kr.getTotalResults(), 70);


        kc.extend(kc.build().term("textClass", "uninteresting"));
        assertEquals("Documents", 1, kc.numberOf("documents"));

        kc.extend(kc.build().term("textClass", "wissenschaft"));

        assertEquals("Documents", 3, kc.numberOf("documents"));
        assertEquals("Tokens", 1669, kc.numberOf("tokens"));
        assertEquals("Sentences", 188, kc.numberOf("sentences"));
        assertEquals("Paragraphs", 130, kc.numberOf("paragraphs"));
    };


    @Test
    public void filterExampleWithNullresult () throws Exception {

        // Construct index
        KrillIndex ki = new KrillIndex();
        // Indexing test files
        for (String i : new String[] { "00001", "00002" }) {
            ki.addDoc(getClass().getResourceAsStream("/wiki/" + i + ".json.gz"),
                    true);
        };
        ki.commit();

        // Create Virtual collections:
        KrillCollection kc = new KrillCollection(ki);

        assertEquals("Documents", 2, kc.numberOf("documents"));

        kc.fromBuilder(kc.build().term("textClass", "nichts"));

        assertEquals("Documents", 0, kc.numberOf("documents"));
        assertEquals("Tokens", 0, kc.numberOf("tokens"));
        assertEquals("Sentences", 0, kc.numberOf("sentences"));
        assertEquals("Paragraphs", 0, kc.numberOf("paragraphs"));
    };


    @Test
    public void filterExampleAtomicLegacy () throws Exception {

        // That's exactly the same test class, but with multiple atomic indices

        // Construct index
        KrillIndex ki = new KrillIndex();
        // Indexing test files
        for (String i : new String[] { "00001", "00002", "00003", "00004",
                "00005", "00006", "02439" }) {
            ki.addDoc(getClass().getResourceAsStream("/wiki/" + i + ".json.gz"),
                    true);
            ki.commit();
        };

        CollectionBuilder kf = new CollectionBuilder();

        // Create Virtual collections:
        KrillCollection kc = new KrillCollection(ki);

        assertEquals("Documents", 7, kc.numberOf("documents"));

        // If this is set - everything is fine automatically ...
        kc.filter(kc.build().term("corpusID", "WPD"));

        assertEquals("Documents", 7, kc.numberOf("documents"));

        // The virtual collection consists of all documents that have the textClass "reisen" and "freizeit"

        /*
        kc.filter(kf.and("textClass", "reisen").and("textClass",
                "freizeit-unterhaltung"));
        */
        kc.filter(kc.build().andGroup()
                .with(kc.build().term("textClass", "reisen"))
                .with(kc.build().term("textClass", "freizeit-unterhaltung")));

        assertEquals("Documents", 5, kc.numberOf("documents"));
        assertEquals("Tokens", 1678, kc.numberOf("tokens"));
        assertEquals("Sentences", 194, kc.numberOf("sentences"));
        assertEquals("Paragraphs", 139, kc.numberOf("paragraphs"));

        // Subset this to all documents that have also the text
        // kc.filter(kf.and("textClass", "kultur"));

        kc.filter(kc.build().term("textClass", "kultur"));

        assertEquals("Documents", 1, kc.numberOf("documents"));
        assertEquals("Tokens", 405, kc.numberOf("tokens"));
        assertEquals("Sentences", 75, kc.numberOf("sentences"));
        assertEquals("Paragraphs", 48, kc.numberOf("paragraphs"));

        // This is already filtered though ...
        // kc.filter(kf.and("corpusID", "WPD"));
        kc.filter(kc.build().term("corpusID", "WPD"));

        assertEquals("Documents", 1, kc.numberOf("documents"));
        assertEquals("Tokens", 405, kc.numberOf("tokens"));
        assertEquals("Sentences", 75, kc.numberOf("sentences"));
        assertEquals("Paragraphs", 48, kc.numberOf("paragraphs"));

        // Create a query
        Krill ks = new Krill(
                new QueryBuilder("tokens").seg("opennlp/p:NN").with("tt/p:NN"));
        ks.setCollection(kc).getMeta().setStartIndex(0).setCount((short) 20)
                .setContext(
                        new SearchContext(true, (short) 5, true, (short) 5));

        Result kr = ks.apply(ki);
        /*
        Result kr = ki.search(kc, query, 0, (short) 20, true, (short) 5, true,
                (short) 5);
        */
        assertEquals(kr.getTotalResults(), 70);

        // kc.extend(kf.and("textClass", "uninteresting"));
        kc.extend(kc.build().term("textClass", "uninteresting"));

        assertEquals("Documents", 1, kc.numberOf("documents"));

        kc.extend(kc.build().term("textClass", "wissenschaft"));

        assertEquals("Documents", 3, kc.numberOf("documents"));
        assertEquals("Tokens", 1669, kc.numberOf("tokens"));
        assertEquals("Sentences", 188, kc.numberOf("sentences"));
        assertEquals("Paragraphs", 130, kc.numberOf("paragraphs"));

        // System.err.println(kc.toString());
        // Test collectionbuilder simplifier!
        /*
        OrGroup(
                AndGroup(
                         corpusID:WPD
                         textClass:reisen
                         textClass:freizeit-unterhaltung
                         textClass:kultur
                         corpusID:WPD
                         )
                textClass:uninteresting
                textClass:wissenschaft
        )
        */

        assertTrue(ki.delDocs("textClass", "wissenschaft"));
        ki.commit();

        assertEquals("Documents", 1, kc.numberOf("documents"));
        assertEquals("Tokens", 405, kc.numberOf("tokens"));
        assertEquals("Sentences", 75, kc.numberOf("sentences"));
        assertEquals("Paragraphs", 48, kc.numberOf("paragraphs"));
    };


    @Test
    public void filterExample2Legacy () throws Exception {

        // Construct index
        KrillIndex ki = new KrillIndex();
        // Indexing test files
        for (String i : new String[] { "00001", "00002", "00003", "00004",
                "00005", "00006", "02439" }) {
            ki.addDoc(getClass().getResourceAsStream("/wiki/" + i + ".json.gz"),
                    true);
        };
        ki.commit();

        ki.addDoc(
                getClass().getResourceAsStream("/wiki/00012-fakemeta.json.gz"),
                true);

        ki.commit();

        /*
        CollectionBuilderLegacy kf = new CollectionBuilderLegacy();
        
        // Create Virtual collections:
        KrillCollectionLegacy kc = new KrillCollectionLegacy(ki);
        kc.filter(kf.and("textClass", "reisen").and("textClass",
                "freizeit-unterhaltung"));
        */

        KrillCollection kc = new KrillCollection(ki);
        CollectionBuilder cb = kc.build();
        kc.filter(cb.andGroup().with(cb.term("textClass", "reisen"))
                .with(cb.term("textClass", "freizeit-unterhaltung")));

        assertEquals("Documents", 5, kc.numberOf("documents"));
        assertEquals("Tokens", 1678, kc.numberOf("tokens"));
        assertEquals("Sentences", 194, kc.numberOf("sentences"));
        assertEquals("Paragraphs", 139, kc.numberOf("paragraphs"));


        // Create a query
        Krill ks = new Krill(
                new QueryBuilder("tokens").seg("opennlp/p:NN").with("tt/p:NN"));
        ks.setCollection(kc).getMeta().setStartIndex(0).setCount((short) 20)
                .setContext(
                        new SearchContext(true, (short) 5, true, (short) 5));

        Result kr = ks.apply(ki);

        assertEquals(kr.getTotalResults(), 369);

        // kc.filter(kf.and("corpusID", "QQQ"));
        kc.filter(cb.term("corpusID", "QQQ"));

        assertEquals("Documents", 0, kc.numberOf("documents"));
        assertEquals("Tokens", 0, kc.numberOf("tokens"));
        assertEquals("Sentences", 0, kc.numberOf("sentences"));
        assertEquals("Paragraphs", 0, kc.numberOf("paragraphs"));

        ks.setCollection(kc);

        // Create a query        
        kr = ks.apply(ki);
        /*
        kr = ki.search(kc, query, 0, (short) 20, true, (short) 5, true,
                (short) 5);
        */
        assertEquals(kr.getTotalResults(), 0);
    };


    @Test
    public void uidCollectionLegacy () throws IOException {

        // Construct index
        KrillIndex ki = new KrillIndex();
        // Indexing test files
        int uid = 1;
        for (String i : new String[] { "00001", "00002", "00003", "00004",
                "00005", "00006", "02439" }) {
            FieldDocument fd = ki.addDoc(uid++,
                    getClass().getResourceAsStream("/wiki/" + i + ".json.gz"),
                    true);
        };
        ki.commit();

        assertEquals("Documents", 7, ki.numberOf("documents"));
        assertEquals("Paragraphs", 174, ki.numberOf("paragraphs"));
        assertEquals("Sentences", 281, ki.numberOf("sentences"));
        assertEquals("Tokens", 2661, ki.numberOf("tokens"));

        SpanQuery sq = new SpanTermQuery(new Term("tokens", "s:der"));
        Result kr = ki.search(sq, (short) 10);
        assertEquals(86, kr.getTotalResults());

        // Create Virtual collections:
        KrillCollection kc = new KrillCollection();
        kc.filterUIDs(new String[] { "2", "3", "4" });
        kc.setIndex(ki);
        assertEquals("Documents", 3, kc.numberOf("documents"));

        assertEquals("Paragraphs", 46, kc.numberOf("paragraphs"));
        assertEquals("Sentences", 103, kc.numberOf("sentences"));
        assertEquals("Tokens", 1229, kc.numberOf("tokens"));


        Krill ks = new Krill(sq);
        ks.setCollection(kc).getMeta().setStartIndex(0).setCount((short) 20)
                .setContext(
                        new SearchContext(true, (short) 5, true, (short) 5));
        kr = ks.apply(ki);

        // kr = ki.search(kc, sq, 0, (short) 20, true, (short) 5, true, (short) 5);

        assertEquals((long) 39, kr.getTotalResults());
    };


    @Test
    public void uidCollectionWithDeletions () throws IOException {

        // Construct index
        KrillIndex ki = new KrillIndex();
        // Indexing test files
        int uid = 1;
        for (String i : new String[] { "00001", "00002", "00003", "00004",
                "00005", "00006", "02439" }) {
            FieldDocument fd = ki.addDoc(uid++,
                    getClass().getResourceAsStream("/wiki/" + i + ".json.gz"),
                    true);
        };
        ki.commit();


        assertEquals("Documents", 7, ki.numberOf("documents"));
        assertEquals("Paragraphs", 174, ki.numberOf("paragraphs"));
        assertEquals("Sentences", 281, ki.numberOf("sentences"));
        assertEquals("Tokens", 2661, ki.numberOf("tokens"));

        assertTrue(ki.delDoc(3));
        ki.commit();

        assertEquals("Documents", 6, ki.numberOf("documents"));

        assertEquals("Paragraphs", 146, ki.numberOf("paragraphs"));
        assertEquals("Sentences", 212, ki.numberOf("sentences"));
        assertEquals("Tokens", 2019, ki.numberOf("tokens"));

        assertTrue(ki.delDoc(2));
        assertTrue(ki.delDoc(3));
        assertTrue(ki.delDoc(4));
        assertTrue(ki.delDoc(5));
        assertTrue(ki.delDoc(6));
        assertTrue(ki.delDoc(7));
        ki.commit();

        assertEquals("Documents", 1, ki.numberOf("documents"));
        assertEquals("Paragraphs", 75, ki.numberOf("paragraphs"));
    };


    private FieldDocument createDoc1 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-1");
        fd.addString("author", "Frank");
        fd.addKeyword("textClass", "Nachricht Kultur Reisen");
        fd.addInt("pubDate", 20051210);
        fd.addText("text", "Der alte Mann ging über die Straße");
        return fd;
    };


    private FieldDocument createDoc2 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-2");
        fd.addString("author", "Peter");
        fd.addKeyword("textClass", "Kultur Reisen");
        fd.addInt("pubDate", 20051207);
        fd.addText("text", "Der junge Mann hatte keine andere Wahl");
        return fd;
    };


    private FieldDocument createDoc3 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-3");
        fd.addString("author", "Sebastian");
        fd.addKeyword("textClass", "Reisen Finanzen");
        fd.addInt("pubDate", 20051216);
        fd.addText("text", "Die Frau und der Mann küssten sich");
        return fd;
    };
};
