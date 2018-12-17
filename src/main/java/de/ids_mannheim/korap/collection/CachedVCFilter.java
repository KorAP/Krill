package de.ids_mannheim.korap.collection;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Filter;
import org.apache.lucene.util.Bits;

/**
 * Filter for virtual corpus/collection existing in the cache.
 * 
 * @author margaretha
 *
 */
public class CachedVCFilter extends Filter {

    public static final boolean DEBUG = false;

    public static Logger jlog = LogManager.getLogger(CachedVCFilter.class);

    private CachedVCData cachedCollection;
    private String cacheKey;

    public CachedVCFilter (String cacheKey, CachedVCData cachedCollection) {
        this.cacheKey = cacheKey;
        this.cachedCollection = cachedCollection;
    }

    @Override
    public DocIdSet getDocIdSet (LeafReaderContext context, Bits acceptDocs)
            throws IOException {
        DocBits docBits =
                cachedCollection.getDocIdMap().get(context.hashCode());

        if (docBits == null) {
            if (DEBUG)
                jlog.debug("LeafReaderContext is not found in the cache.");
            return null;
        }
        return docBits.createBitDocIdSet();
    }

    @Override
    public String toString () {
        return "referTo(cached:" + this.cacheKey + ")";
    };
}
