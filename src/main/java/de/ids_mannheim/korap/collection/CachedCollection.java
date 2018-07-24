package de.ids_mannheim.korap.collection;

import java.util.Map;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.DocIdSet;

public class CachedCollection {

    private Map<LeafReaderContext, DocIdSet> docIdMap;
    
    public Map<LeafReaderContext, DocIdSet> getDocIdMap () {
        return docIdMap;
    }

    public void setDocIdMap (Map<LeafReaderContext, DocIdSet> docIdMap) {
        this.docIdMap = docIdMap;
    }
    
}
