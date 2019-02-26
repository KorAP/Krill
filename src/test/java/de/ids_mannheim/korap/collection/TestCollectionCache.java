package de.ids_mannheim.korap.collection;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import de.ids_mannheim.korap.KrillCollection;
import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.index.FieldDocument;
import net.sf.ehcache.Cache;

public class TestCollectionCache {

    @Test
    public void testNullCache() throws IOException{
        Cache temp = KrillCollection.cache;
        assertTrue(KrillCollection.cache != null);
        
        KrillCollection.cache = null;
        KrillIndex ki = new KrillIndex();
        ki.addDoc(new FieldDocument());
        ki.commit();
        
        KrillCollection.cache = temp;
    }
}
