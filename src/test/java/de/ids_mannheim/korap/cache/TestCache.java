package de.ids_mannheim.korap.cache;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import net.sf.jsr107cache.Cache;
import net.sf.jsr107cache.CacheException;
import net.sf.jsr107cache.CacheFactory;
import net.sf.jsr107cache.CacheManager;

@RunWith(JUnit4.class)
public class TestCache {

    @Test
    public void cache1 () {

        Cache cache;

        try {
            CacheFactory cacheFactory = CacheManager.getInstance()
                    .getCacheFactory();
            cache = cacheFactory.createCache(Collections.emptyMap());
        }

        catch (CacheException e) {
            // ...
            return;
        };

        cache.put("beispiel1", "Das ist ein Test");
        cache.put("beispiel2", "Das ist ein Versuch");
        cache.put("beispiel3", "Das ist ein Beispiel");

        assertEquals("Das ist ein Test", cache.get("beispiel1"));
        assertEquals("Das ist ein Versuch", cache.get("beispiel2"));
        assertEquals("Das ist ein Beispiel", cache.get("beispiel3"));
    };
};
