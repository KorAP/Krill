package de.ids_mannheim.korap.collection;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.store.MMapDirectory;
import org.junit.Test;

import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.KrillCollection;
import de.ids_mannheim.korap.KrillIndex;
import net.sf.ehcache.CacheManager;
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
    public void testCacheVC () throws IOException {
        InputStream is = getClass().getClassLoader()
                .getResourceAsStream("named-vc/named-vc-free.jsonld");
        String json = IOUtils.toString(is);

        KrillCollection kc = new KrillCollection(json);
        kc.setIndex(index);
        kc.storeInCache();

        Element element = KrillCollection.cache.get("cache-goe");
        CachedVCData cc = (CachedVCData) element.getObjectValue();

        assertTrue(cc.getDocIdMap().size() > 0);

        testSearchCachedVC();
        testClearCache();
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
    
    public void testClearCache () {
        CacheManager cacheManager = CacheManager.getInstance();
        cacheManager.clearAll();
        
        Element element = KrillCollection.cache.get("cache-goe");
        assertNull(element);
    }
}
