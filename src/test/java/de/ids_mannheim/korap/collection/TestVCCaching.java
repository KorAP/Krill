package de.ids_mannheim.korap.collection;

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
        testAddToCache();
        testSearchCachedVC();
        testClearCache();
        testAddDocToIndex();
        testDelDocFromIndex();
    }

    private void testAddToCache () throws IOException {
        InputStream is = getClass().getClassLoader()
                .getResourceAsStream("named-vc/named-vc-free.jsonld");
        String json = IOUtils.toString(is);
        is.close();

        KrillCollection kc = new KrillCollection(json);
        kc.setIndex(index);
        kc.storeInCache();

        Element element = KrillCollection.cache.get("cache-goe");
        CachedVCData cc = (CachedVCData) element.getObjectValue();

        assertTrue(cc.getDocIdMap().size() > 0);
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

        Element element = KrillCollection.cache.get("cache-goe");
        assertNull(element);
    }

    public void testAddDocToIndex () throws IOException {
        testAddToCache();

        FieldDocument fd = new FieldDocument();
        fd.addTV("base", "x  y", "[(0-3)s:x]" + // 1
                "[(3-4)s:y]" // 2
        );
        index.addDoc(fd);
        index.commit();
        
        Element element = KrillCollection.cache.get("cache-goe");
        assertNull(element);
    }
    
    public void testDelDocFromIndex () throws IOException {
        testAddToCache();

        index.delDocs("textSigle", "GOE/AGF/00000");
        index.commit();
        
        Element element = KrillCollection.cache.get("cache-goe");
        assertNull(element);
    }
    
    @Test
    public void testAutoCaching () throws IOException {
        InputStream is = getClass().getClassLoader()
                .getResourceAsStream("collection/query-with-vc-ref.jsonld");
        String json = IOUtils.toString(is);

        String result = new Krill(json).apply(this.index).toJsonString();
        assertNotNull(result);
        assertTrue(!result.isEmpty());
    }
}
