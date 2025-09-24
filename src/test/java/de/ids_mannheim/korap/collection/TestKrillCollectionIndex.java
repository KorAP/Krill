package de.ids_mannheim.korap.collection;

import static de.ids_mannheim.korap.TestSimple.getJsonString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.KrillCollection;
import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.index.FieldDocument;
import de.ids_mannheim.korap.query.QueryBuilder;
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.response.SearchContext;
import de.ids_mannheim.korap.util.StatusCodes;


@RunWith(JUnit4.class)
public class TestKrillCollectionIndex {
    private KrillIndex ki;

    final String path = "/queries/collections/";

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
    public void testIndexWithNegation1 () throws IOException {
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
        
        kcn.fromBuilder(
            cb.andGroup().with(
                cb.term("author", "Frank").not()
                )
            .with(
                cb.term("author", "Sebastian").not()
                )
            );
        assertEquals("AndGroup(-author:Frank -author:Sebastian)", kcn.toString());
        assertEquals(1, kcn.docCount());


        kcn.fromBuilder(
            cb.andGroup().with(
                cb.term("author", "Peter")
                )
            .with(
                cb.andGroup().with(
                    cb.term("author", "Frank").not()
                    )
                .with(
                    cb.term("author", "Sebastian").not()
                    )
                )
            );
        assertEquals("AndGroup(author:Peter AndGroup(-author:Frank -author:Sebastian))", kcn.toString());
        assertEquals(1, kcn.docCount());

        kcn.fromBuilder(
            cb.andGroup().with(
                cb.re("textClass", "reis.*")
                )
            .with(
                cb.andGroup().with(
                    cb.term("author", "Frank").not()
                    )
                .with(
                    cb.term("author", "Sebastian").not()
                    )
                )
            );
        assertEquals("AndGroup(QueryWrapperFilter(textClass:/reis.*/) AndGroup(-author:Frank -author:Sebastian))", kcn.toString());
        assertEquals(1, kcn.docCount());
    };


    @Test
    public void testIndexWithNegation2 () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createDoc1());
        ki.commit();
        ki.addDoc(createDoc2());
        ki.commit();
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
       
        kcn.fromBuilder(
            cb.andGroup().with(
                cb.term("author", "Frank").not()
                )
            .with(
                cb.term("author", "Sebastian").not()
                )
            );
        assertEquals("AndGroup(-author:Frank -author:Sebastian)", kcn.toString());
        assertEquals(1, kcn.docCount());

        kcn.fromBuilder(
            cb.andGroup().with(
                cb.term("author", "Peter")
                )
            .with(
                cb.andGroup().with(
                    cb.term("author", "Frank").not()
                    )
                .with(
                    cb.term("author", "Sebastian").not()
                    )
                )
            );
        assertEquals("AndGroup(author:Peter AndGroup(-author:Frank -author:Sebastian))", kcn.toString());
        assertEquals(1, kcn.docCount());

        kcn.fromBuilder(
            cb.andGroup().with(
                cb.re("textClass", "reis..")
                )
            .with(
                cb.andGroup().with(
                    cb.term("author", "Frank").not()
                    )
                .with(
                    cb.term("author", "Sebastian").not()
                    )
                )
            );
        assertEquals("AndGroup(QueryWrapperFilter(textClass:/reis../) AndGroup(-author:Frank -author:Sebastian))", kcn.toString());
        assertEquals(1, kcn.docCount());
    };

    @Test
    public void testIndexWithNegation3 () throws IOException {

        // This is identical to above but the operands are switched
        ki = new KrillIndex();
        ki.addDoc(createDoc1());
        ki.commit();
        ki.addDoc(createDoc2());
        ki.commit();
        ki.addDoc(createDoc3());
        ki.commit();
        CollectionBuilder cb = new CollectionBuilder();
        KrillCollection kcn = new KrillCollection(ki);

        // orGroup with simple Negation
        kcn.fromBuilder(
            cb.orGroup().with(cb.term("author", "Peter"))
            .with(cb.term("textClass", "kultur").not()));
        assertEquals(2, kcn.docCount());

        kcn.fromBuilder(cb.orGroup().with(cb.term("author", "Sebastian"))
                        .with(cb.term("textClass", "kultur").not()));
        assertEquals(1, kcn.docCount());
       
        kcn.fromBuilder(
            cb.andGroup().with(
                cb.term("author", "Sebastian").not()
                )
            .with(
                cb.term("author", "Frank").not()
                )
            );
        assertEquals("AndGroup(-author:Sebastian -author:Frank)", kcn.toString());
        assertEquals(1, kcn.docCount());

        kcn.fromBuilder(
            cb.andGroup().with(
                cb.andGroup().with(
                    cb.term("author", "Sebastian").not()
                    )
                .with(
                    cb.term("author", "Frank").not()
                    )
                )
            .with(
                cb.term("author", "Peter")
                )
            );
        assertEquals("AndGroup(AndGroup(-author:Sebastian -author:Frank) author:Peter)", kcn.toString());
        assertEquals(1, kcn.docCount());

        kcn.fromBuilder(
            cb.andGroup().with(
                cb.andGroup().with(
                    cb.term("author", "Sebastian").not()
                    )
                .with(
                    cb.term("author", "Frank").not()
                    )
                )
            .with(
                cb.re("textClass", "reis..")
                )
            );
        assertEquals("AndGroup(AndGroup(-author:Sebastian -author:Frank) QueryWrapperFilter(textClass:/reis../))", kcn.toString());
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

        FieldDocument fd = ki.addDoc(createDoc1());
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

        kcn.fromBuilder(cb.re("text", "fra."));
        assertEquals(1, kcn.docCount());

        kcn.fromBuilder(cb.re("text", "fra.|ma.n"));
        assertEquals(3, kcn.docCount());

		String sv = fd.doc.getField("text").stringValue();
		assertEquals("Der alte  Mann ging über die Straße", sv);

        kcn.fromBuilder(cb.term("text", sv));
        assertEquals(1, kcn.docCount());
	};

    @Test
    public void testIndexWithIntegers () throws IOException {
        ki = new KrillIndex();

        FieldDocument fd = ki.addDoc(createDoc1());
        ki.addDoc(createDoc2());
        ki.addDoc(createDoc5001());
        ki.commit();

        CollectionBuilder cb = new CollectionBuilder();
        KrillCollection kcn = new KrillCollection(ki);

        assertEquals("toks:[2000.0 TO 4000.0]", cb.between("toks", 2000, 4000).toString());

        kcn.fromBuilder(cb.between("toks", 2000, 4000));
        assertEquals(1, kcn.docCount());

        kcn.fromBuilder(cb.geq("toks", 2000));
        assertEquals(1, kcn.docCount());

        kcn.fromBuilder(cb.leq("toks", 4000));
        assertEquals(1, kcn.docCount());

        kcn.fromBuilder(cb.leq("toks", 2000));
        assertEquals(0, kcn.docCount());

        kcn.fromBuilder(cb.geq("toks", 4000));
        assertEquals(0, kcn.docCount());

        kcn.fromBuilder(cb.lt("toks", 3000));
        assertEquals(0, kcn.docCount());

        kcn.fromBuilder(cb.lt("toks", 3001));
        assertEquals(1, kcn.docCount());

        kcn.fromBuilder(cb.gt("toks", 3000));
        assertEquals(0, kcn.docCount());

        kcn.fromBuilder(cb.gt("toks", 2999));
        assertEquals(1, kcn.docCount());

        kcn.fromBuilder(cb.eq("toks", 3000));
        assertEquals(1, kcn.docCount());

        kcn.fromBuilder(cb.eq("toks", 3001));
        assertEquals(0, kcn.docCount());
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

		kcn.fromBuilder(cb.term("text", "Der alte  Mann ging über die Straße"));
        assertEquals(1, kcn.docCount());

		kcn.fromBuilder(cb.text("text", "Der alte Mann"));
		assertEquals("QueryWrapperFilter(text:\"der alte mann\")", kcn.toString());
        assertEquals(1, kcn.docCount());
	};

	@Test
    public void testUnknownVC () throws IOException {
		ki = new KrillIndex();
		ki.addDoc(createDoc1());
		ki.commit();

		// This test was adopted from TestVCCaching,
		// But does not fail anymore for deserialization
        String json = _getJSONString("vc-ref/unknown-vc-ref.jsonld");

        KrillCollection kc = new KrillCollection(json);
		assertEquals("referTo(https://korap.ids-mannheim.de/@ndiewald/MyCorpus)", kc.getBuilder().toString());

		assertEquals("vcFilter(https://korap.ids-mannheim.de/@ndiewald/MyCorpus)",kc.toString());
		
        QueryBuilder kq = new QueryBuilder("field");
		
		Krill krill = new Krill(kq.seg("a").with("b"));
		krill.setCollection(kc);
		
		Result result = krill.apply(ki);

		assertEquals(StatusCodes.MISSING_COLLECTION, result.getError(0).getCode());
		assertTrue(result.getError(0).getMessage().startsWith("VC is not found"));
	};
	
    @Test
    public void testEmptyDocIdSetIterator () throws IOException {
        KrillIndex ki = new KrillIndex();
        // Indexing test files
        for (String i : new String[] { "00001", "00002", "00003", "00004",
                "00005", "00006", "02439" }) {
            ki.addDoc(getClass().getResourceAsStream("/wiki/" + i + ".json.gz"),
                    true);
        };
        ki.commit();

        String filename = "/queries/collections/vc-ref/query-with-vc-ref-klznkz66.jsonld";
        String json = getJsonString(getClass().getResource(filename).getFile());
        KrillCollection kc = new KrillCollection(json);
        kc.setIndex(ki);
        assertEquals(0, kc.numberOf("documents"));
    }
	
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
        assertEquals(70, kr.getTotalResults());


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
        assertEquals(70, kr.getTotalResults());

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

        assertEquals(369, kr.getTotalResults());

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
        assertEquals(0, kr.getTotalResults());
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

	@Test
    public void testKrillCollectionWithNonexistingNegation () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createDoc1()); // nachricht kultur reisen
        ki.addDoc(createDoc3()); // reisen finanzen
        ki.commit();

        KrillCollection kc = new KrillCollection(ki);
        CollectionBuilder cb = kc.build();

		kc.fromBuilder(cb.term("textClass","reisen"));
		assertEquals("textClass:reisen", kc.toString());
        assertEquals("Documents", 2, kc.numberOf("documents"));

		kc.fromBuilder(cb.andGroup().with(
						   cb.term("textClass","reisen")
						   ).with(
							   cb.term("textClass","nachricht").not()
							   ));
		assertEquals("AndGroup(textClass:reisen -textClass:nachricht)", kc.toString());
        assertEquals("Documents", 1, kc.numberOf("documents"));

		
		kc.fromBuilder(cb.andGroup().with(
						   cb.term("textClass","reisen")
						   ).with(
							   cb.term("textClass","reisen").not()
							   ));
		assertEquals("AndGroup(textClass:reisen -textClass:reisen)", kc.toString());
        assertEquals("Documents", 0, kc.numberOf("documents"));

		kc.fromBuilder(cb.andGroup().with(
						   cb.term("textClass","kultur")
						   ).with(
							   cb.term("textClass","finanzen").not()
							   ));
		assertEquals("AndGroup(textClass:kultur -textClass:finanzen)", kc.toString());
        assertEquals("Documents", 1, kc.numberOf("documents"));

		kc.fromBuilder(cb.andGroup().with(
						   cb.term("textClass","reisen")
						   ).with(
							   cb.term("textClass","Blabla").not()
							   ));
		assertEquals("AndGroup(textClass:reisen -textClass:Blabla)", kc.toString());
        assertEquals("Documents", 2, kc.numberOf("documents"));
    }


	@Test
    public void testKrillCollectionWithValueVectorNe () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createDoc1()); // nachricht kultur reisen
        ki.addDoc(createDoc2()); // kultur reisen
        ki.addDoc(createDoc3()); // reisen finanzen
        ki.commit();

		KrillCollection kc = new KrillCollection();
		kc.setIndex(ki);

        CollectionBuilder cb = kc.build();
		kc.fromBuilder(cb.orGroup().with(cb.term("textClass", "nachricht")).with(cb.term("textClass","finanzen")));
		assertEquals("OrGroup(textClass:nachricht textClass:finanzen)", kc.toString());
        assertEquals("Documents", 2, kc.numberOf("documents"));

		kc.fromBuilder(cb.term("textClass", "nachricht").not());
		assertEquals("-textClass:nachricht", kc.toString());
        assertEquals("Documents", 2, kc.numberOf("documents"));

        kc.fromBuilder(cb.orGroup().with(cb.term("textClass", "nachricht").not()).with(cb.term("textClass","finanzen").not()));
        assertEquals("OrGroup(-textClass:nachricht -textClass:finanzen)", kc.toString());
        assertEquals("Documents", 3, kc.numberOf("documents"));

        kc.fromBuilder(cb.orGroup().with(cb.term("textClass", "nachricht")).with(cb.term("textClass","finanzen")).not());
		assertEquals("-OrGroup(textClass:nachricht textClass:finanzen)", kc.toString());
        assertEquals("Documents", 1, kc.numberOf("documents"));

        Krill ks = new Krill(new QueryBuilder("tokens").seg("i:a"));
        ks.setCollection(kc);

        // Create a query        
        Result kr = ks.apply(ki);
        assertEquals(1, kr.getTotalResults());
        assertEquals("[[a]] c d", kr.getMatch(0).getSnippetBrackets());

        String json = _getJSONString("collection_with_vector_ne.jsonld");
        ks = new Krill(json);

        kc = ks.getCollection();
        kc.setIndex(ki);
        
        assertEquals("-OrGroup(textClass:nachricht textClass:finanzen)", kc.toString());
        assertEquals("Documents", 1, kc.numberOf("documents"));

        kr = ks.apply(ki);
        assertEquals("[[a]] c d", kr.getMatch(0).getSnippetBrackets());
        assertEquals(1, kr.getTotalResults());
    };

	@Test
    public void testKrillCollectionWithLargeVector () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createDoc1());
        ki.addDoc(createDoc2());
        ki.addDoc(createDoc3());
        ki.commit();
        ki.addDoc(createDoc5000());
        ki.commit();

        String json = _getJSONString("collection_large_vector.jsonld");
        KrillCollection kc = new KrillCollection(json);

        Krill ks = new Krill(new QueryBuilder("tokens").seg("i:a"));
        ks.setCollection(kc);
        kc.setIndex(ki);
        
        assertEquals("Documents", 4, kc.numberOf("documents"));

        Result kr = ks.apply(ki);
        assertEquals("[[a]] b c", kr.getMatch(0).getSnippetBrackets());
        assertEquals("[[a]] c d", kr.getMatch(1).getSnippetBrackets());
        assertEquals("[[a]] d e", kr.getMatch(2).getSnippetBrackets());
        assertEquals("[[a]] d e", kr.getMatch(3).getSnippetBrackets());
    };

	@Test
    public void testKrillCollectionWithLargeVectorAndLargeIndex () throws IOException {
        ki = new KrillIndex();
        for (int i = 0; i < 6000; i++) {
            FieldDocument fd = new FieldDocument();
            fd.addString("UID", Integer.toString(i));
            ki.addDoc(fd);
            if (i == 4500)
                ki.commit();
        };

        ki.commit();

        String json = _getJSONString("collection_large_vector.jsonld");
        KrillCollection kc = new KrillCollection(json);
        kc.setIndex(ki);
        
        assertEquals("Documents", 5000, kc.numberOf("documents"));
    };

    
    

    public static FieldDocument createDoc1 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("UID", "1");
        fd.addString("ID", "doc-1");
        fd.addString("author", "Frank");
        fd.addKeywords("textClass", "Nachricht Kultur Reisen");
        fd.addDate("pubDate", 20051210);
        fd.addText("text", "Der alte  Mann ging über die Straße");
        fd.addTV("tokens", "a b c", "[(0-1)s:a|i:a|_0$<i>0<i>1|-:t$<i>3]"
				 + "[(2-3)s:b|i:b|_1$<i>2<i>3]" + "[(4-5)s:c|i:c|_2$<i>4<i>5]");
        return fd;
    };


    public static FieldDocument createDoc2 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("UID", "2");
		fd.addString("ID", "doc-2");
        fd.addString("author", "Peter");
        fd.addKeywords("textClass", "Kultur Reisen");
        fd.addDate("pubDate", 20051207);
        fd.addText("text", "Der junge Mann hatte keine andere Wahl");
        fd.addTV("tokens", "a c d", "[(0-1)s:a|i:a|_0$<i>0<i>1|-:t$<i>3]"
				 + "[(2-3)s:c|i:c|_1$<i>2<i>3]" + "[(4-5)s:d|i:d|_2$<i>4<i>5]");
        return fd;
    };


    public static FieldDocument createDoc3 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("UID", "3");
		fd.addString("ID", "doc-3");
        fd.addString("author", "Sebastian");
        fd.addKeywords("textClass", "Reisen Finanzen");
        fd.addDate("pubDate", 20051216);
        fd.addText("text", "Die Frau und der Mann küssten sich");
        fd.addTV("tokens", "a d e", "[(0-1)s:a|i:a|_0$<i>0<i>1|-:t$<i>3]"
				 + "[(2-3)s:d|i:d|_1$<i>2<i>3]" + "[(4-5)s:e|i:e|_2$<i>4<i>5]");
        return fd;
    };

    public static FieldDocument createDoc5000 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("UID", "5000");
		fd.addString("ID", "doc-5000");
        fd.addString("author", "Sebastian");
        fd.addKeywords("textClass", "Kultur Finanzen");
        fd.addDate("pubDate", 20180202);
        fd.addText("text", "Die Frau und der Mann küssten sich");
        fd.addTV("tokens", "a d e", "[(0-1)s:a|i:a|_0$<i>0<i>1|-:t$<i>3]"
				 + "[(2-3)s:d|i:d|_1$<i>2<i>3]" + "[(4-5)s:e|i:e|_2$<i>4<i>5]");
        return fd;
    };

    public static FieldDocument createDoc5001 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("UID", "5001");
		fd.addString("ID", "doc-5001");
        fd.addInt("toks", 3000);
        fd.addDate("pubDate", 20180202);
        fd.addText("text", "Der alte  Mann ging über die Straße");
        fd.addTV("tokens", "a b c", "[(0-1)s:a|i:a|_0$<i>0<i>1|-:t$<i>3]"
				 + "[(2-3)s:b|i:b|_1$<i>2<i>3]" + "[(4-5)s:c|i:c|_2$<i>4<i>5]");
        return fd;
    };

    private String _getJSONString (String file) {
        return getJsonString(getClass().getResource(path + file).getFile());
    };
};
