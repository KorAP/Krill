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

import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.collection.DocBits;
import de.ids_mannheim.korap.response.Result;
import de.ids_mannheim.korap.util.QueryException;

public class TestVirtualCorpusCache {

    private KrillIndex ki;
    private String queryRefJson;

    public TestVirtualCorpusCache () throws IOException {
        ki = createIndex();

        String file = "/queries/collections/vc-ref/query-with-vc-ref.jsonld";
        InputStream is = getClass().getResourceAsStream(file);
        queryRefJson = IOUtils.toString(is, "utf-8");
    }


    private KrillIndex createIndex () throws IOException {
        KrillIndex ki = new KrillIndex();
        String[] docIds = new String[] { "00001", "00002", "00003" };
        int uid = 1;
        for (String i : docIds) {
            ki.addDoc(uid++,
                    getClass().getResourceAsStream("/wiki/" + i + ".json.gz"),
                    true);
        }
        ki.commit();

        ki.addDoc(uid++, getClass().getResourceAsStream("/wiki/00004.json.gz"),
                true);
        ki.commit();
        return ki;
    }


    @Test
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
    public void testReferToUncachedVC () throws IOException, QueryException {
        String vcId = "named-vc1";
        assertFalse(VirtualCorpusCache.contains(vcId));

        Krill krill = new Krill(queryRefJson);
        Result result = krill.apply(ki);
        assertEquals(27, result.getTotalResults());

        assertTrue(VirtualCorpusCache.contains(vcId));
        Map<String, DocBits> vc1 = VirtualCorpusCache.retrieve(vcId);
        assertNotNull(vc1);

        VirtualCorpusCache.delete(vcId);
        assertFalse(VirtualCorpusCache.contains(vcId));
    }


    @Test
    public void testUpdateCachedVC () throws IOException {
        String vcId = "named-vc1";
        // VC cache will be marked for cleaning up 
        // because of storing a new VC
        KrillIndex ki = createIndex();
        Krill krill = new Krill(queryRefJson);
        Result result = krill.apply(ki);
        assertEquals(27, result.getTotalResults());

        assertEquals(2,
                VirtualCorpusCache.map.get(vcId).keySet().size());

        ki.delDoc(2);
        ki.commit();

        // VC cache will be marked for cleaning up again
        // because of index change.
        krill = new Krill(queryRefJson);
        result = krill.apply(ki);
        assertEquals(17, result.getTotalResults());

        // The old leaf fingerprint should be cleaned up, thus the map 
        // should have the same size. But the fingerprints should be 
        // different from before the 1st cleaning up
        assertEquals(2,
                VirtualCorpusCache.map.get(vcId).keySet().size());

        // VC cache will be cleaned up for the 2nd time 
        // resulting the same leaf-fingerprints
        krill = new Krill(queryRefJson);
        result = krill.apply(ki);
        assertEquals(17, result.getTotalResults());

        assertEquals(2,
                VirtualCorpusCache.map.get(vcId).keySet().size());

        ki.close();

        VirtualCorpusCache.delete(vcId);
        assertFalse(VirtualCorpusCache.contains(vcId));
    }


}
