package de.ids_mannheim.korap.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.Ignore;

import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.KrillCollection;
import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.collection.CollectionBuilder;
import de.ids_mannheim.korap.collection.DocBits;
import de.ids_mannheim.korap.collection.TestKrillCollectionIndex;
import de.ids_mannheim.korap.query.QueryBuilder;
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.util.KrillProperties;
import de.ids_mannheim.korap.util.QueryException;

public class TestVirtualCorpusCache {

    private KrillIndex ki;
    private String queryRefJson;
    private String queryRefJson2;
    private String named_vc1 = "named-vc1";
    private String named_vc2 = "named-vc2";
    private String named_vc3 = "named-vc3";
    private String named_vc4 = "named-vc4";

    public TestVirtualCorpusCache () throws IOException {
        ki = createIndex();

        String file = "/queries/collections/vc-ref/query-with-vc-ref.jsonld";
        InputStream is = getClass().getResourceAsStream(file);
        queryRefJson = IOUtils.toString(is, "utf-8");

        file = "/queries/collections/vc-ref/query-with-vc-ref2.jsonld";
        is = getClass().getResourceAsStream(file);
        queryRefJson2 = IOUtils.toString(is, "utf-8");
    }


    public static KrillIndex createIndex () throws IOException {
        KrillIndex ki = new KrillIndex();
        String[] docIds = new String[] { "00001", "00002", "00003" };
        int uid = 1;
        for (String i : docIds) {
            ki.addDoc(uid++, TestVirtualCorpusCache.class
                    .getResourceAsStream("/wiki/" + i + ".json.gz"), true);
        }
        ki.commit();

        ki.addDoc(uid++, TestVirtualCorpusCache.class
                .getResourceAsStream("/wiki/00004.json.gz"), true);
        ki.commit();
        return ki;
    }


    @Test
    @Ignore("TODO(kwic-cap): revisit after KWIC total-cap migration")
    public void testStoreUncachedVC () throws IOException, QueryException {
        String vcId = "named-vc4";

        File f = new File(VirtualCorpusCache.CACHE_LOCATION + "/" + vcId);
        assertFalse(f.exists());

        VirtualCorpusCache.store(vcId, ki);
        assertTrue(VirtualCorpusCache.contains(vcId));

        Map<String, DocBits> docIdMap = VirtualCorpusCache.retrieve(vcId);
        assertEquals(2, docIdMap.size());

        VirtualCorpusCache.delete(vcId);
        assertFalse(VirtualCorpusCache.contains(vcId));
    }


    @Test
    @Ignore("TODO(kwic-cap): revisit after KWIC total-cap migration")
    public void testReferToUncachedVC () throws IOException, QueryException {
        String vcId = "named-vc1";
        assertFalse(VirtualCorpusCache.contains(vcId));

        Krill krill = new Krill(queryRefJson2);
        Result result = krill.apply(ki);
        assertEquals(27, result.getTotalResults());

        assertTrue(VirtualCorpusCache.contains(vcId));
        Map<String, DocBits> vc1 = VirtualCorpusCache.retrieve(vcId);
        assertNotNull(vc1);

        VirtualCorpusCache.delete(vcId);
        assertFalse(VirtualCorpusCache.contains(vcId));
    }

    @Test
    @Ignore("TODO(kwic-cap): revisit after KWIC total-cap migration")
    public void testUpdateCachedVC () throws IOException {
        String vcId = "named-vc1";
        // VC cache will be marked for cleaning up 
        // because of storing a new VC
        KrillIndex ki = createIndex();
        Krill krill = new Krill(queryRefJson2);
        Result result = krill.apply(ki);
        assertEquals(27, result.getTotalResults());

        assertEquals(2, VirtualCorpusCache.map.get(vcId).keySet().size());

        ki.delDoc(2);
        ki.commit();

        // VC cache will be marked for cleaning up again
        // because of index change.
        krill = new Krill(queryRefJson2);
        result = krill.apply(ki);
        assertEquals(17, result.getTotalResults());

        // The old leaf fingerprint should be cleaned up, thus the map 
        // should have the same size. But the fingerprints should be 
        // different from before the 1st cleaning up
        assertEquals(2, VirtualCorpusCache.map.get(vcId).keySet().size());

        // VC cache will be cleaned up for the 2nd time 
        // resulting the same leaf-fingerprints
        krill = new Krill(queryRefJson2);
        result = krill.apply(ki);
        assertEquals(17, result.getTotalResults());

        assertEquals(2, VirtualCorpusCache.map.get(vcId).keySet().size());

        ki.close();

        VirtualCorpusCache.delete(vcId);
        assertFalse(VirtualCorpusCache.contains(vcId));
    }


    @Test
    @Ignore("TODO(kwic-cap): revisit after KWIC total-cap migration")
    public void testCleanUpVC () throws QueryException, IOException {
        VirtualCorpusCache.CAPACITY = 3;

        VirtualCorpusCache.store("named-vc1", ki);
        VirtualCorpusCache.store("named-vc2", ki);
        VirtualCorpusCache.store("named-vc3", ki);
        VirtualCorpusCache.store("named-vc4", ki);

        assertEquals(3, VirtualCorpusCache.map.size());
        assertEquals(4, VirtualCorpusCache.vcToCleanUp.size());

        Krill krill = new Krill(queryRefJson2);
        Result result = krill.apply(ki);
        assertEquals(27, result.getTotalResults());

        VirtualCorpusCache.reset();
        assertFalse(VirtualCorpusCache.contains(named_vc1));
        assertFalse(VirtualCorpusCache.contains(named_vc2));
        assertFalse(VirtualCorpusCache.contains(named_vc3));
        assertFalse(VirtualCorpusCache.contains(named_vc4));

    }

    // The following tests have been moved from TestKrillCollectionIndex
    // and updated according to the new cache mechanism


    @Test
    @Ignore("TODO(kwic-cap): revisit after KWIC total-cap migration")
    public void testCache () throws IOException, QueryException {
        KrillProperties.loadDefaultProperties();

        KrillIndex ki = new KrillIndex();
        ki.addDoc(TestKrillCollectionIndex.createDoc1());
        ki.addDoc(TestKrillCollectionIndex.createDoc2());
        ki.commit();

        // add VC to cache manually
        VirtualCorpusCache.store(named_vc1, ki);
        VirtualCorpusCache.store(named_vc2, ki);

        assertTrue(VirtualCorpusCache.contains(named_vc1));
        assertTrue(VirtualCorpusCache.contains(named_vc2));

        // Check for cache location
        //        assertFalse(KrillCollection.cache.isElementInMemory("named-vc1"));
        //        assertTrue(KrillCollection.cache.isElementOnDisk("named-vc1"));
        //        assertTrue(KrillCollection.cache.isElementInMemory("named-vc2"));
        //        assertTrue(KrillCollection.cache.isElementOnDisk("named-vc2"));

        // references named-vc1: ID eq ["doc-2","doc-3"]

        Krill krill = new Krill(queryRefJson);
        // TODO: Better keep the reference
        assertEquals("vcFilter(named-vc1)", krill.getCollection().toString());

        Result result = krill.apply(ki);
        assertEquals("[[a]] c d", result.getMatch(0).getSnippetBrackets());
        assertEquals(result.getMatch(0).getUID(), 2);
        assertEquals(result.getMatches().size(), 1);

        ki.addDoc(TestKrillCollectionIndex.createDoc3());
        ki.commit();

        // Cache is not removed after index change
        assertTrue(VirtualCorpusCache.contains(named_vc1));

        krill = new Krill(queryRefJson);
        assertEquals("vcFilter(named-vc1)", krill.getCollection().toString());
        result = krill.apply(ki);

        assertEquals("[[a]] c d", result.getMatch(0).getSnippetBrackets());
        assertEquals("[[a]] d e", result.getMatch(1).getSnippetBrackets());
        assertEquals(result.getMatches().size(), 2);

        // Cache is not removed on deletion
        ki.delDoc(2);
        ki.commit();

        // Check cache
        assertTrue(VirtualCorpusCache.contains(named_vc1));

        // Rerun query
        krill = new Krill(queryRefJson);
        assertEquals("vcFilter(named-vc1)", krill.getCollection().toString());
        result = krill.apply(ki);
        assertEquals("[[a]] d e", result.getMatch(0).getSnippetBrackets());
        assertEquals(result.getMatches().size(), 1);

        VirtualCorpusCache.reset();
        assertFalse(VirtualCorpusCache.contains(named_vc1));
    };


    @Test
    @Ignore("TODO(kwic-cap): revisit after KWIC total-cap migration")
    public void testNestedNamedVCs () throws IOException {
        KrillProperties.loadDefaultProperties();

        KrillIndex ki = new KrillIndex();
        ki.addDoc(TestKrillCollectionIndex.createDoc1());
        ki.addDoc(TestKrillCollectionIndex.createDoc2());
        ki.addDoc(TestKrillCollectionIndex.createDoc3());
        ki.commit();

        // Check cache
        assertFalse(VirtualCorpusCache.contains(named_vc1));
        assertFalse(VirtualCorpusCache.contains(named_vc2));

        QueryBuilder kq = new QueryBuilder("tokens");
        KrillCollection kc = new KrillCollection(ki);
        CollectionBuilder cb = kc.build();
        Krill krill = new Krill(kq.seg("i:a"));

        kc.fromBuilder(cb.orGroup().with(cb.referTo("named-vc1"))
                .with(cb.referTo("named-vc2")));
        krill.setCollection(kc);
        // named-vc1: UID:[2,3]
        // named-vc2: author:Frank (doc-1)

        assertEquals("OrGroup(vcFilter(named-vc1) vcFilter(named-vc2))",
                krill.getCollection().toString());

        assertEquals("tokens:i:a", krill.getSpanQuery().toString());

        Result result = krill.apply(ki);
        assertEquals("[[a]] b c", result.getMatch(0).getSnippetBrackets());
        assertEquals("[[a]] c d", result.getMatch(1).getSnippetBrackets());
        assertEquals("[[a]] d e", result.getMatch(2).getSnippetBrackets());
        assertEquals(3, result.getMatches().size());

        assertTrue(VirtualCorpusCache.contains(named_vc2));

        kc.fromBuilder(cb.orGroup().with(cb.referTo("named-vc1"))
                .with(cb.referTo("named-vc2")));

        assertEquals("OrGroup(vcFilter(named-vc1) vcFilter(named-vc2))",
                krill.getCollection().toString());

        result = krill.apply(ki);
        assertEquals("[[a]] b c", result.getMatch(0).getSnippetBrackets());
        assertEquals("[[a]] c d", result.getMatch(1).getSnippetBrackets());
        assertEquals("[[a]] d e", result.getMatch(2).getSnippetBrackets());
        assertEquals(3, result.getMatches().size());

        // EM: Redundant?
        kc.fromBuilder(cb.orGroup().with(cb.referTo("named-vc1"))
                .with(cb.referTo("named-vc2")));

        assertEquals("OrGroup(vcFilter(named-vc1) vcFilter(named-vc2))",
                krill.getCollection().toString());

        result = krill.apply(ki);
        assertEquals("[[a]] b c", result.getMatch(0).getSnippetBrackets());
        assertEquals("[[a]] c d", result.getMatch(1).getSnippetBrackets());
        assertEquals("[[a]] d e", result.getMatch(2).getSnippetBrackets());
        assertEquals(3, result.getMatches().size());

        kc.fromBuilder(cb.referTo("named-vc1"));

        assertEquals("vcFilter(named-vc1)", krill.getCollection().toString());

        result = krill.apply(ki);
        assertEquals("[[a]] c d", result.getMatch(0).getSnippetBrackets());
        assertEquals("[[a]] d e", result.getMatch(1).getSnippetBrackets());
        assertEquals(2, result.getMatches().size());


        kc.fromBuilder(cb.referTo("named-vc2"));

        assertEquals("vcFilter(named-vc2)", krill.getCollection().toString());

        result = krill.apply(ki);
        assertEquals("[[a]] b c", result.getMatch(0).getSnippetBrackets());
        assertEquals(1, result.getMatches().size());

        VirtualCorpusCache.reset();
    };


    @Test
    @Ignore("TODO(kwic-cap): revisit after KWIC total-cap migration")
    public void testNamedVCsAfterQueryWithMissingDocs () throws IOException {
        KrillProperties.loadDefaultProperties();

        KrillIndex ki = new KrillIndex();
        ki.addDoc(TestKrillCollectionIndex.createDoc1());
        ki.commit();
        ki.addDoc(TestKrillCollectionIndex.createDoc2());
        ki.commit();
        ki.addDoc(TestKrillCollectionIndex.createDoc3());
        ki.commit();

        // Check cache
        assertFalse(VirtualCorpusCache.contains(named_vc1));
        assertFalse(VirtualCorpusCache.contains(named_vc2));


        QueryBuilder kq = new QueryBuilder("tokens");
        KrillCollection kc = new KrillCollection(ki);
        CollectionBuilder cb = kc.build();

        // Check only for c and cache
        Krill krill = new Krill(kq.seg("i:c"));

        kc.fromBuilder(cb.orGroup().with(cb.referTo("named-vc1"))
                .with(cb.referTo("named-vc2")));
        krill.setCollection(kc);
        // named-vc1: UID:[2,3]
        // named-vc2: author:Frank (doc-1)

        assertEquals("OrGroup(vcFilter(named-vc1) vcFilter(named-vc2))",
                krill.getCollection().toString());

        assertEquals("tokens:i:c", krill.getSpanQuery().toString());

        Result result = krill.apply(ki);
        assertEquals("a b [[c]]", result.getMatch(0).getSnippetBrackets());
        assertEquals("a [[c]] d", result.getMatch(1).getSnippetBrackets());
        assertEquals(2, result.getMatches().size());

        assertTrue(VirtualCorpusCache.contains(named_vc2));

        kc.fromBuilder(cb.orGroup().with(cb.referTo("named-vc1"))
                .with(cb.referTo("named-vc2")));

        assertEquals("OrGroup(vcFilter(named-vc1) vcFilter(named-vc2))",
                krill.getCollection().toString());

        // Check again for c with cache
        result = krill.apply(ki);
        assertEquals("a b [[c]]", result.getMatch(0).getSnippetBrackets());
        assertEquals("a [[c]] d", result.getMatch(1).getSnippetBrackets());
        assertEquals(2, result.getMatches().size());

        // Check for a with cache
        krill = new Krill(kq.seg("i:a"));
        krill.setCollection(kc);

        assertEquals("OrGroup(vcFilter(named-vc1) vcFilter(named-vc2))",
                krill.getCollection().toString());

        // Check again for c with cache
        result = krill.apply(ki);
        assertEquals("[[a]] b c", result.getMatch(0).getSnippetBrackets());
        assertEquals("[[a]] c d", result.getMatch(1).getSnippetBrackets());
        assertEquals("[[a]] d e", result.getMatch(2).getSnippetBrackets());
        assertEquals(3, result.getMatches().size());

        VirtualCorpusCache.reset();
    };


    @Test
    @Ignore("TODO(kwic-cap): revisit after KWIC total-cap migration")
    public void testNamedVCsAfterCorpusWithMissingDocs () throws IOException {
        KrillProperties.loadDefaultProperties();

        KrillIndex ki = new KrillIndex();
        ki.addDoc(TestKrillCollectionIndex.createDoc1());
        ki.commit();
        ki.addDoc(TestKrillCollectionIndex.createDoc2());
        ki.commit();
        ki.addDoc(TestKrillCollectionIndex.createDoc3());
        ki.commit();

        // Check cache
        assertFalse(VirtualCorpusCache.contains(named_vc1));
        assertFalse(VirtualCorpusCache.contains(named_vc2));

        QueryBuilder kq = new QueryBuilder("tokens");
        KrillCollection kc = new KrillCollection(ki);
        CollectionBuilder cb = kc.build();

        // Check only for c and cache
        Krill krill = new Krill(kq.seg("i:a"));

        kc.fromBuilder(cb.andGroup().with(cb.term("textClass", "kultur"))
                .with(cb.orGroup().with(cb.referTo("named-vc1"))
                        .with(cb.referTo("named-vc2"))));
        krill.setCollection(kc);
        // named-vc1: UID:[2,3]
        // named-vc2: author:Frank (doc-1)
        // textClass:kultur (doc-1,doc-2)

        assertEquals(
                "AndGroup(textClass:kultur OrGroup(vcFilter(named-vc1) vcFilter(named-vc2)))",
                krill.getCollection().toString());

        assertEquals("tokens:i:a", krill.getSpanQuery().toString());

        Result result = krill.apply(ki);
        assertEquals("[[a]] b c", result.getMatch(0).getSnippetBrackets());
        assertEquals("[[a]] c d", result.getMatch(1).getSnippetBrackets());
        assertEquals(2, result.getMatches().size());

        // Check stored VC in cache
        assertTrue(VirtualCorpusCache.contains(named_vc1));
        assertTrue(VirtualCorpusCache.contains(named_vc2));

        kc.fromBuilder(cb.orGroup().with(cb.referTo("named-vc1"))
                .with(cb.referTo("named-vc2")));

        assertEquals("OrGroup(vcFilter(named-vc1) vcFilter(named-vc2))",
                krill.getCollection().toString());

        // Check again for c with cache
        result = krill.apply(ki);
        assertEquals("[[a]] b c", result.getMatch(0).getSnippetBrackets());
        assertEquals("[[a]] c d", result.getMatch(1).getSnippetBrackets());
        assertEquals("[[a]] d e", result.getMatch(2).getSnippetBrackets());
        assertEquals(3, result.getMatches().size());

        VirtualCorpusCache.reset();
    }


    @Test
    @Ignore("TODO(kwic-cap): revisit after KWIC total-cap migration")
    public void testCollectionWithVCRefAndPubDate ()
            throws IOException, QueryException {
        KrillIndex ki = new KrillIndex();
        ki.addDoc(TestKrillCollectionIndex.createDoc2());
        ki.addDoc(TestKrillCollectionIndex.createDoc3());
        ki.addDoc(TestKrillCollectionIndex.createDoc5000());
        ki.commit();

        VirtualCorpusCache.store(named_vc3, ki);

        assertTrue(VirtualCorpusCache.contains(named_vc3));

        String file = "/queries/collections/collection-with-vc-ref-and-pubDate.jsonld";
        InputStream is = getClass().getResourceAsStream(file);
        String json = IOUtils.toString(is, "utf-8");

        KrillCollection kc = new KrillCollection(json);
        kc.setIndex(ki);
        assertEquals(2, kc.numberOf("documents"));

        VirtualCorpusCache.reset();
    }

}
