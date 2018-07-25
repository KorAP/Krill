package de.ids_mannheim.korap.collection;

import java.io.Serializable;
import java.util.Map;

import org.apache.lucene.search.DocIdSet;

public class CachedVCData implements Serializable{

    /** Auto generated
     * 
     */
    private static final long serialVersionUID = 5635087441839303653L;
    
    private Map<Integer, DocIdSet> docIdMap;
    
    public CachedVCData (Map<Integer, DocIdSet> docIdMap) {
        this.docIdMap = docIdMap;
    }
    
    public Map<Integer, DocIdSet> getDocIdMap () {
        return docIdMap;
    }

    public void setDocIdMap (Map<Integer, DocIdSet> docIdMap) {
        this.docIdMap = docIdMap;
    }
    
}
