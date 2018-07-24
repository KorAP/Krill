package de.ids_mannheim.korap.collection;

import java.io.IOException;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Filter;
import org.apache.lucene.util.Bits;

public class NamedVCFilter extends Filter {

    private CachedCollection cachedCollection;
    
    public NamedVCFilter (CachedCollection cachedCollection) {
        this.cachedCollection = cachedCollection;
    }

    @Override
    public DocIdSet getDocIdSet (LeafReaderContext context, Bits acceptDocs)
            throws IOException {
        
        return cachedCollection.getDocIdMap().get(context);
    }

}
