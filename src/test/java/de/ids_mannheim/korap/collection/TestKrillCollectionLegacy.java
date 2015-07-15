package de.ids_mannheim.korap.collection;

import java.io.*;

import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.index.FieldDocument;
import de.ids_mannheim.korap.KrillCollection;
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.KrillQuery;
import de.ids_mannheim.korap.query.QueryBuilder;
import de.ids_mannheim.korap.collection.BooleanFilterOperation;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.spans.SpanQuery;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestKrillCollectionLegacy {

    @Test
    public void filterExample () throws Exception {

        // Construct index
        KrillIndex ki = new KrillIndex();
        // Indexing test files
        for (String i : new String[] { "00001", "00002", "00003", "00004",
                "00005", "00006", "02439" }) {
            ki.addDoc(
                    getClass().getResourceAsStream("/wiki/" + i + ".json.gz"),
                    true);
        };
        ki.commit();

        CollectionBuilder kf = new CollectionBuilder();

        // Create Virtual collections:
        KrillCollection kc = new KrillCollection(ki);

        assertEquals("Documents", 7, kc.numberOf("documents"));

        // The virtual collection consists of all documents that have
        // the textClass "reisen" and "freizeit"

        kc.filter(kf.and("textClass", "reisen").and("textClass",
                "freizeit-unterhaltung"));

        assertEquals("Documents", 5, kc.numberOf("documents"));
        assertEquals("Tokens", 1678, kc.numberOf("tokens"));
        assertEquals("Sentences", 194, kc.numberOf("sentences"));
        assertEquals("Paragraphs", 139, kc.numberOf("paragraphs"));

        // Subset this to all documents that have also the text
        kc.filter(kf.and("textClass", "kultur"));

        assertEquals("Documents", 1, kc.numberOf("documents"));
        assertEquals("Tokens", 405, kc.numberOf("tokens"));
        assertEquals("Sentences", 75, kc.numberOf("sentences"));
        assertEquals("Paragraphs", 48, kc.numberOf("paragraphs"));

        kc.filter(kf.and("corpusID", "WPD"));

        assertEquals("Documents", 1, kc.numberOf("documents"));
        assertEquals("Tokens", 405, kc.numberOf("tokens"));
        assertEquals("Sentences", 75, kc.numberOf("sentences"));
        assertEquals("Paragraphs", 48, kc.numberOf("paragraphs"));

        // Create a query
        QueryBuilder kq = new QueryBuilder("tokens");
        SpanQuery query = kq.seg("opennlp/p:NN").with("tt/p:NN").toQuery();

        Result kr = kc.search(query);
        assertEquals(kr.getTotalResults(), 70);

        kc.extend(kf.and("textClass", "uninteresting"));
        assertEquals("Documents", 1, kc.numberOf("documents"));

        kc.extend(kf.and("textClass", "wissenschaft"));

        assertEquals("Documents", 3, kc.numberOf("documents"));
        assertEquals("Tokens", 1669, kc.numberOf("tokens"));
        assertEquals("Sentences", 188, kc.numberOf("sentences"));
        assertEquals("Paragraphs", 130, kc.numberOf("paragraphs"));
        // System.err.println(kr.toJSON());
    };


    @Test
    public void filterExampleAtomic () throws Exception {

        // That's exactly the same test class, but with multiple atomic indices

        // Construct index
        KrillIndex ki = new KrillIndex();
        // Indexing test files
        for (String i : new String[] { "00001", "00002", "00003", "00004",
                "00005", "00006", "02439" }) {
            ki.addDoc(
                    getClass().getResourceAsStream("/wiki/" + i + ".json.gz"),
                    true);
            ki.commit();
        };

        CollectionBuilder kf = new CollectionBuilder();

        // Create Virtual collections:
        KrillCollection kc = new KrillCollection(ki);

        assertEquals("Documents", 7, kc.numberOf("documents"));

        // If this is set - everything is fine automatically ...
        kc.filter(kf.and("corpusID", "WPD"));
        assertEquals("Documents", 7, kc.numberOf("documents"));


        // The virtual collection consists of all documents that have the textClass "reisen" and "freizeit"

        kc.filter(kf.and("textClass", "reisen").and("textClass",
                "freizeit-unterhaltung"));

        assertEquals("Documents", 5, kc.numberOf("documents"));
        assertEquals("Tokens", 1678, kc.numberOf("tokens"));
        assertEquals("Sentences", 194, kc.numberOf("sentences"));
        assertEquals("Paragraphs", 139, kc.numberOf("paragraphs"));

        // Subset this to all documents that have also the text
        kc.filter(kf.and("textClass", "kultur"));

        assertEquals("Documents", 1, kc.numberOf("documents"));
        assertEquals("Tokens", 405, kc.numberOf("tokens"));
        assertEquals("Sentences", 75, kc.numberOf("sentences"));
        assertEquals("Paragraphs", 48, kc.numberOf("paragraphs"));

        // This is already filtered though ...
        kc.filter(kf.and("corpusID", "WPD"));

        assertEquals("Documents", 1, kc.numberOf("documents"));
        assertEquals("Tokens", 405, kc.numberOf("tokens"));
        assertEquals("Sentences", 75, kc.numberOf("sentences"));
        assertEquals("Paragraphs", 48, kc.numberOf("paragraphs"));

        // Create a query
        QueryBuilder kq = new QueryBuilder("tokens");
        SpanQuery query = kq.seg("opennlp/p:NN").with("tt/p:NN").toQuery();

        Result kr = kc.search(query);
        assertEquals(kr.getTotalResults(), 70);

        kc.extend(kf.and("textClass", "uninteresting"));
        assertEquals("Documents", 1, kc.numberOf("documents"));

        kc.extend(kf.and("textClass", "wissenschaft"));

        assertEquals("Documents", 3, kc.numberOf("documents"));
        assertEquals("Tokens", 1669, kc.numberOf("tokens"));
        assertEquals("Sentences", 188, kc.numberOf("sentences"));
        assertEquals("Paragraphs", 130, kc.numberOf("paragraphs"));
    };



    @Test
    public void filterExample2 () throws Exception {

        // Construct index
        KrillIndex ki = new KrillIndex();
        // Indexing test files
        for (String i : new String[] { "00001", "00002", "00003", "00004",
                "00005", "00006", "02439" }) {
            ki.addDoc(
                    getClass().getResourceAsStream("/wiki/" + i + ".json.gz"),
                    true);
        };
        ki.commit();

        ki.addDoc(getClass()
                .getResourceAsStream("/wiki/00012-fakemeta.json.gz"), true);

        ki.commit();

        CollectionBuilder kf = new CollectionBuilder();

        // Create Virtual collections:
        KrillCollection kc = new KrillCollection(ki);
        kc.filter(kf.and("textClass", "reisen").and("textClass",
                "freizeit-unterhaltung"));
        assertEquals("Documents", 5, kc.numberOf("documents"));
        assertEquals("Tokens", 1678, kc.numberOf("tokens"));
        assertEquals("Sentences", 194, kc.numberOf("sentences"));
        assertEquals("Paragraphs", 139, kc.numberOf("paragraphs"));

        // Create a query
        QueryBuilder kq = new QueryBuilder("tokens");
        SpanQuery query = kq.seg("opennlp/p:NN").with("tt/p:NN").toQuery();

        Result kr = kc.search(query);

        assertEquals(kr.getTotalResults(), 369);

        kc.filter(kf.and("corpusID", "QQQ"));

        assertEquals("Documents", 0, kc.numberOf("documents"));
        assertEquals("Tokens", 0, kc.numberOf("tokens"));
        assertEquals("Sentences", 0, kc.numberOf("sentences"));
        assertEquals("Paragraphs", 0, kc.numberOf("paragraphs"));
    };


    @Test
    public void uidCollection () throws IOException {

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

        kr = kc.search(sq);
        assertEquals((long) 39, kr.getTotalResults());
    };
};
