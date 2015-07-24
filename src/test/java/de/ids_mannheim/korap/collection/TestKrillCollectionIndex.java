package de.ids_mannheim.korap.collection;
import java.io.IOException;

import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.KrillCollectionNew;
import de.ids_mannheim.korap.collection.CollectionBuilderNew;
import de.ids_mannheim.korap.index.FieldDocument;

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
        CollectionBuilderNew cb = new CollectionBuilderNew();
        KrillCollectionNew kcn = new KrillCollectionNew(ki);

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
        kcn.fromBuilder(cb.orGroup(cb.term("author", "Frank")).with(cb.term("author", "Michael")));
        assertEquals(1, kcn.docCount());

        kcn.fromBuilder(cb.orGroup(cb.term("author", "Frank")).with(cb.term("author", "Sebastian")));
        assertEquals(2, kcn.docCount());

        kcn.fromBuilder(cb.orGroup(cb.term("author", "Frank"))
                        .with(cb.term("author", "Sebastian"))
                        .with(cb.term("author", "Peter")));
        assertEquals(3, kcn.docCount());

        kcn.fromBuilder(cb.orGroup(cb.term("author", "Huhu"))
                        .with(cb.term("author", "Haha"))
                        .with(cb.term("author", "Hehe")));
        assertEquals(0, kcn.docCount());

        // Multi field orGroup tests
        kcn.fromBuilder(cb.orGroup(cb.term("ID", "doc-1")).with(cb.term("author", "Peter")));
        assertEquals(2, kcn.docCount());

        kcn.fromBuilder(cb.orGroup(cb.term("ID", "doc-1")).with(cb.term("author", "Frank")));
        assertEquals(1, kcn.docCount());

        kcn.fromBuilder(cb.orGroup(cb.term("ID", "doc-1")).with(cb.term("author", "Michael")));
        assertEquals(1, kcn.docCount());

        // Simple andGroup tests
        kcn.fromBuilder(cb.andGroup(cb.term("author", "Frank")).with(cb.term("author", "Michael")));
        assertEquals(0, kcn.docCount());

        kcn.fromBuilder(cb.andGroup(cb.term("ID", "doc-1")).with(cb.term("author", "Frank")));
        assertEquals(1, kcn.docCount());

        // andGroup in keyword field test
        kcn.fromBuilder(cb.andGroup(cb.term("textClass", "reisen")).with(cb.term("textClass", "finanzen")));
        assertEquals(1, kcn.docCount());

        kcn.fromBuilder(cb.andGroup(cb.term("textClass", "reisen")).with(cb.term("textClass", "kultur")));
        assertEquals(2, kcn.docCount());

        kcn.fromBuilder(cb.andGroup(cb.term("textClass", "finanzen")).with(cb.term("textClass", "kultur")));
        assertEquals(0, kcn.docCount());
    };

    @Test
    public void testIndexWithNegation () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createDoc1());
        ki.addDoc(createDoc2());
        ki.addDoc(createDoc3());
        ki.commit();
        CollectionBuilderNew cb = new CollectionBuilderNew();
        KrillCollectionNew kcn = new KrillCollectionNew(ki);

        // Simple negation tests
        kcn.fromBuilder(cb.term("author", "Frank").not());
        assertEquals(2, kcn.docCount());

        kcn.fromBuilder(cb.term("textClass", "reisen").not());
        assertEquals(0, kcn.docCount());

        kcn.fromBuilder(cb.term("textClass", "kultur").not());
        assertEquals(1, kcn.docCount());

        // orGroup with simple Negation
        kcn.fromBuilder(
          cb.orGroup(cb.term("textClass", "kultur").not()).with(cb.term("author", "Peter"))
        );
        assertEquals(2, kcn.docCount());

        kcn.fromBuilder(
          cb.orGroup(cb.term("textClass", "kultur").not()).with(cb.term("author", "Sebastian"))
        );
        assertEquals(1, kcn.docCount());
        
    };

    @Test
    public void testIndexWithMultipleCommits () throws IOException {
        ki = new KrillIndex();
        ki.addDoc(createDoc1());
        ki.addDoc(createDoc2());
        ki.commit();
        CollectionBuilderNew cb = new CollectionBuilderNew();
        KrillCollectionNew kcn = new KrillCollectionNew(ki);

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
    };

    // Todo: Test index with removes
    // Todo: Test with dates
    // Todo: Test with regex

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
