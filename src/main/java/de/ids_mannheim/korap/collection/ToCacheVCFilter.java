package de.ids_mannheim.korap.collection;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Filter;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.FixedBitSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ids_mannheim.korap.KrillCollection;
import de.ids_mannheim.korap.collection.CollectionBuilder.Interface;
import net.sf.ehcache.Element;

/**
 * Filter for virtual corpus / collection that should be cached.
 * 
 * @author margaretha
 *
 */
public class ToCacheVCFilter extends Filter {
    private Filter filter;
    private CollectionBuilder.Interface cbi;
    private String cacheKey;
    private Map<Integer, DocBits> docIdMap;
    // EM: auto caching is disabled regarding issue #44 
    private boolean isAutoCachingEnabled = false;

    public final static Logger log = LoggerFactory.getLogger(ToCacheVCFilter.class);
            
    public ToCacheVCFilter (String cacheKey, Map<Integer, DocBits> docIdMap,
                            Interface cbi, Filter filter) {
        this.cacheKey = cacheKey;
        this.docIdMap = docIdMap;
        this.cbi = cbi;
        this.filter = filter;
    }

    @Override
    public DocIdSet getDocIdSet (LeafReaderContext context, Bits acceptDocs)
            throws IOException {

        DocIdSet docIdSet = filter.getDocIdSet(context, acceptDocs);

        final LeafReader reader = context.reader();
        int maxDoc = reader.maxDoc();
        FixedBitSet bitset = new FixedBitSet(maxDoc);

        if (docIdSet == null) {
            if (cbi.isNegative()) {
                bitset.set(0, maxDoc);
            }
            else {
                bitset.clear(0, maxDoc);
            }
        }
        else {
            bitset.or(docIdSet.iterator());
            if (cbi.isNegative()) {
                bitset.flip(0, maxDoc);
            }
        }

        if (isAutoCachingEnabled) {
            docIdMap.put(context.hashCode(),
                    new DocBits(bitset.getBits(), bitset.length()));
            CachedVCData cachedVCData =
                    new CachedVCData(new HashMap<>(docIdMap));

            KrillCollection.cache.remove(cacheKey);
            KrillCollection.cache.put(new Element(cacheKey, cachedVCData));
        }

        log.debug("To cache doc bits length: "+ docIdSet.bits().length());
        return docIdSet;
    }

    @Override
    public String toString () {
        return "referTo(" + this.cacheKey + ")";
    };
}
