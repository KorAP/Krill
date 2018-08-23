package de.ids_mannheim.korap.collection;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;

/**
 * Virtual corpus data to cache
 * 
 * @author margaretha
 *
 */
public class CachedVCData implements Serializable {

    /**
     * Auto generated
     * 
     */
    private static final long serialVersionUID = 5635087441839303653L;

    private Map<Integer, DocBits> docIdMap;

    public CachedVCData (Map<Integer, DocBits> docIdMap) {
        this.docIdMap = docIdMap;
    }

    public Map<Integer, DocBits> getDocIdMap () {
        return docIdMap;
    }

    public void setDocIdMap (Map<Integer, DocBits> docIdMap) {
        this.docIdMap = docIdMap;
    }

    // EM: for optimization. has not been checked.
    // ehcache retrieves a byte[] much faster than a map, however,
    // there is an additional cost for converting a map to a byte[]
    // and vice versa.

    private byte[] toByteArray () throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(docIdMap);
        oos.flush();
        return bos.toByteArray();
    }

    private Map<Integer, DocBits> toMap (byte[] bytes)
            throws ClassNotFoundException, IOException {
        ByteArrayInputStream bis = null;
        ObjectInputStream ois = null;
        Map<Integer, DocBits> map = null;
        try {
            bis = new ByteArrayInputStream(bytes);
            ois = new ObjectInputStream(bis);
            map = (Map<Integer, DocBits>) ois.readObject();

        }
        finally {
            if (bis != null) {
                bis.close();
            }
            if (ois != null) {
                ois.close();
            }
        }
        return map;
    }

	public String toString () {
		return this.docIdMap.toString();
	}
}
