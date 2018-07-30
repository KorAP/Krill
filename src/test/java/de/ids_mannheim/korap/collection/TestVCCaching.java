package de.ids_mannheim.korap.collection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.store.MMapDirectory;
import org.junit.Test;

import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.KrillCollection;
import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.index.FieldDocument;
import net.sf.ehcache.Element;

public class TestVCCaching {

    private KrillIndex getSampleIndex () throws IOException {
        return new KrillIndex(new MMapDirectory(
                Paths.get(getClass().getResource("/sample-index").getFile())));
    }

    private KrillIndex index;

    public TestVCCaching () throws IOException {
        index = getSampleIndex();
    }

    @Test
    public void testCache () throws IOException {
        testManualAddToCache("named-vc/named-vc1.jsonld", "named-vc1");
        testManualAddToCache("named-vc/named-vc2.jsonld", "named-vc2");
        
        Element element = KrillCollection.cache.get("named-vc1");
        CachedVCData cc = (CachedVCData) element.getObjectValue();
        assertTrue(cc.getDocIdMap().size() > 0);
        
        element = KrillCollection.cache.get("named-vc2");
        cc = (CachedVCData) element.getObjectValue();
        assertTrue(cc.getDocIdMap().size() > 0);
        
        assertFalse(KrillCollection.cache.isElementInMemory("named-vc1"));
        assertTrue(KrillCollection.cache.isElementOnDisk("named-vc1"));
        assertTrue(KrillCollection.cache.isElementInMemory("named-vc2"));
        assertTrue(KrillCollection.cache.isElementOnDisk("named-vc2"));

        testSearchCachedVC();
        testAddDocToIndex();
        testDelDocFromIndex();
    }

    private void testManualAddToCache (String filename, String vcName) throws IOException {
        InputStream is = getClass().getClassLoader()
                .getResourceAsStream(filename);
        String json = IOUtils.toString(is);
        is.close();

        KrillCollection kc = new KrillCollection(json);
        kc.setIndex(index);
        kc.storeInCache(vcName);
    }

    private void testSearchCachedVC () throws IOException {
        InputStream is = getClass().getClassLoader()
                .getResourceAsStream("collection/query-with-vc-ref.jsonld");
        String json = IOUtils.toString(is);

        String result = new Krill(json).apply(this.index).toJsonString();
        assertNotNull(result);
        assertTrue(!result.isEmpty());

        // test with match:eq
        json.replaceFirst("match:ne", "match:eq");
        result = new Krill(json).apply(this.index).toJsonString();
        assertNotNull(result);
        assertTrue(!result.isEmpty());
    }

    private void testClearCache () {
        KrillCollection.cache.removeAll();

        Element element = KrillCollection.cache.get("named-vc1");
        assertNull(element);
    }

    public void testAddDocToIndex () throws IOException {
        testManualAddToCache("named-vc/named-vc1.jsonld", "named-vc1");

        FieldDocument fd = new FieldDocument();
        fd.addTV("base", "x  y", "[(0-3)s:x]" + // 1
                "[(3-4)s:y]" // 2
        );
        index.addDoc(fd);
        index.commit();
        
        Element element = KrillCollection.cache.get("named-vc1");
        assertNull(element);
    }
    
    public void testDelDocFromIndex () throws IOException {
        testManualAddToCache("named-vc/named-vc1.jsonld", "named-vc1");

        index.delDocs("textSigle", "GOE/AGF/00000");
        index.commit();
        
        Element element = KrillCollection.cache.get("named-vc1");
        assertNull(element);
    }
    
    @Test
    public void testAutoCaching () throws IOException {
        testSearchCachedVC();
        testClearCache();
    }
}
