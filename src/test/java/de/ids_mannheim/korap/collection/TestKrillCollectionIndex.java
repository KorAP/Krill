package de.ids_mannheim.korap.collection;
import java.io.IOException;

import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.KrillCollection;
import de.ids_mannheim.korap.collection.CollectionBuilder;
import de.ids_mannheim.korap.index.FieldDocument;
import de.ids_mannheim.korap.index.TextAnalyzer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestKrillCollectionIndex {
    private KrillIndex ki;

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

        kcn.fromBuilder(cb.term("textClass", "reisen"));
        assertEquals(3, kcn.docCount());

        kcn.fromBuilder(cb.term("textClass", "kultur"));
        assertEquals(2, kcn.docCount());

        kcn.fromBuilder(cb.term("textClass", "finanzen"));
        assertEquals(1, kcn.docCount());

        // Simple orGroup tests
        kcn.fromBuilder(cb.orGroup().with(cb.term("author", "Frank")).with(cb.term("author", "Michael")));
        assertEquals(1, kcn.docCount());

        kcn.fromBuilder(cb.orGroup().with(cb.term("author", "Frank")).with(cb.term("author", "Sebastian")));
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
        kcn.fromBuilder(cb.orGroup().with(cb.term("ID", "doc-1")).with(cb.term("author", "Peter")));
        assertEquals(2, kcn.docCount());

        kcn.fromBuilder(cb.orGroup().with(cb.term("ID", "doc-1")).with(cb.term("author", "Frank")));
        assertEquals(1, kcn.docCount());

        kcn.fromBuilder(cb.orGroup().with(cb.term("ID", "doc-1")).with(cb.term("author", "Michael")));
        assertEquals(1, kcn.docCount());

        // Simple andGroup tests
        kcn.fromBuilder(cb.andGroup().with(cb.term("author", "Frank")).with(cb.term("author", "Michael")));
        assertEquals(0, kcn.docCount());

        kcn.fromBuilder(cb.andGroup().with(cb.term("ID", "doc-1")).with(cb.term("author", "Frank")));
        assertEquals(1, kcn.docCount());

        // andGroup in keyword field test
        kcn.fromBuilder(cb.andGroup().with(cb.term("textClass", "reisen")).with(cb.term("textClass", "finanzen")));
        assertEquals(1, kcn.docCount());

        kcn.fromBuilder(cb.andGroup().with(cb.term("textClass", "reisen")).with(cb.term("textClass", "kultur")));
        assertEquals(2, kcn.docCount());

        kcn.fromBuilder(cb.andGroup().with(cb.term("textClass", "finanzen")).with(cb.term("textClass", "kultur")));
        assertEquals(0, kcn.docCount());

        kcn.fromBuilder(cb.term("text", "mann"));
        assertEquals(3, kcn.docCount());

        kcn.fromBuilder(cb.term("text", "frau"));
        assertEquals(1, kcn.docCount());
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
        kcn.fromBuilder(
          cb.orGroup().with(cb.term("textClass", "kultur").not()).with(cb.term("author", "Peter"))
        );
        assertEquals(2, kcn.docCount());

        kcn.fromBuilder(
          cb.orGroup().with(cb.term("textClass", "kultur").not()).with(cb.term("author", "Sebastian"))
        );
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

        CharTermAttribute charTermAttribute =
            ts.addAttribute(CharTermAttribute.class);
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

        // "Frau" doesn't work!
        kcn.fromBuilder(cb.term("text", "frau"));
        assertEquals(1, kcn.docCount());

        kcn.fromBuilder(cb.re("text", "frau"));
        assertEquals(1, kcn.docCount());

        kcn.fromBuilder(cb.re("text", "frau|mann"));
        assertEquals(3, kcn.docCount());
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
