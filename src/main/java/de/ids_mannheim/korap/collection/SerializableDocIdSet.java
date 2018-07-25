package de.ids_mannheim.korap.collection;

import java.io.Serializable;

import org.apache.lucene.util.BitDocIdSet;
import org.apache.lucene.util.BitSet;

public class SerializableDocIdSet extends BitDocIdSet implements Serializable {

    /**
     * Auto generated
     * 
     */
    private static final long serialVersionUID = 171797306573832807L;

    public SerializableDocIdSet (BitSet set) {
        super(set);
    }
}
