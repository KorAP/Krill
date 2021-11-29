package de.ids_mannheim.korap.cache;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.lucene.index.LeafReaderContext;

import de.ids_mannheim.korap.IndexInfo;
import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.collection.DocBits;
import de.ids_mannheim.korap.collection.VirtualCorpusFilter;
import de.ids_mannheim.korap.collection.VirtualCorpusFilter.DocBitsSupplier;
import de.ids_mannheim.korap.util.Fingerprinter;
import de.ids_mannheim.korap.util.QueryException;

/**
 * 
 * @author margaretha
 *
 */
public class VirtualCorpusCache {

    public static final String CACHE_LOCATION = "vc-cache";
    public static final int CAPACITY = 5;
    public static final Map<String, Map<String, DocBits>> map = Collections
            .synchronizedMap(new LinkedHashMap<String, Map<String, DocBits>>(
                    CAPACITY, (float) 0.75, true) {

                private static final long serialVersionUID = 1815514581428132435L;

                @SuppressWarnings("rawtypes")
                @Override
                protected boolean removeEldestEntry (Map.Entry eldest) {
                    return size() > CAPACITY;
                }
            });

    private static IndexInfo indexInfo;

    private static final Set<String> vcToCleanUp = Collections
            .synchronizedSet(new HashSet<>());


    public VirtualCorpusCache () {
        File dir = new File(CACHE_LOCATION);
        dir.mkdirs();
    }


    public static void storeOnDisk (String vcId, String leafFingerprint,
            DocBits docBits) {
        File dir = new File(CACHE_LOCATION + "/" + vcId);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String filepath = dir + "/" + leafFingerprint;
        File f = new File(filepath);
        if (f.exists()) {
            f.delete();
        }
        try {
            ObjectOutputStream os = new ObjectOutputStream(
                    new FileOutputStream(f));
            os.writeObject(docBits);
            os.close();
        }
        catch (IOException e) {
            e.printStackTrace();
            System.err.println("Cannot write " + filepath);
        }
    }


    public static void store (String vcId, Map<String, DocBits> vcData) {
        map.put(vcId, vcData);
        for (String leafFingerprint : vcData.keySet()) {
            storeOnDisk(vcId, leafFingerprint, vcData.get(leafFingerprint));
        }

    }

    public static void store (String vcId, KrillIndex index)
            throws QueryException, IOException {
        
        DocBitsSupplier docBitsSupplier = new VirtualCorpusFilter(
                vcId).new DocBitsSupplier();
        String leafFingerprint;
        for (LeafReaderContext context : index.reader().leaves()) {
            leafFingerprint = Fingerprinter.create(
                    context.reader().getCombinedCoreAndDeletesKey().toString());
            leafFingerprint = Fingerprinter.normalizeSlash(leafFingerprint);
            
            getDocBits(vcId, leafFingerprint, () -> {
                try {
                    return docBitsSupplier.supplyDocBits(context,
                            context.reader().getLiveDocs());
                }
                catch (IOException | QueryException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }


    public static Map<String, DocBits> retrieve (String vcId) {
        if (map.containsKey(vcId)) {
            return map.get(vcId);
        }
        Map<String, DocBits> vcData = null;
        File dir = new File(CACHE_LOCATION + "/" + vcId);
        if (dir.exists()) {
            vcData = new HashMap<String, DocBits>();
            for (File f : dir.listFiles()) {
                ObjectInputStream ois;
                try {
                    ois = new ObjectInputStream(new FileInputStream(f));
                    DocBits d = (DocBits) ois.readObject();
                    vcData.put(f.getName(), d);
                    ois.close();
                }
                catch (IOException | ClassNotFoundException e) {
                    return null;
                }
            }
            map.put(vcId, vcData);
        }
        return vcData;

    }


    public static boolean contains (String vcId) {
        if (map.containsKey(vcId)) {
            return true;
        }
        else {
            File f = new File(CACHE_LOCATION + "/" + vcId);
            return f.exists();
        }
    }

    public static void delete (String vcId) {
        vcToCleanUp.remove(vcId);
        map.remove(vcId);
        File vc = new File(CACHE_LOCATION + "/" + vcId);
        if (vc.exists()) {
            for (File f : vc.listFiles()) {
                if (f.exists()) {
                    f.delete();
                }
            }
            vc.delete();
        }
    }

    public static void reset () {
        vcToCleanUp.clear();
        map.clear();

        File vcCache = new File(CACHE_LOCATION + "/");
        for (File vc : vcCache.listFiles()) {
            for (File f : vc.listFiles()) {
                if (f.exists()) {
                    f.delete();
                }
            }
            vc.delete();
        }
        vcCache.delete();
    }
    

    /**
     * Sets IndexInfo and checks if there is any VC to clean up. This
     * method is called every time an index is used in {@link Krill}.
     * 
     * When the VC cache knows that a leaf-fingerprint is not in the
     * map of a VC, it is marked for clean up. The cached VC will be
     * cleaned up, next time the index is used in {@link Krill}.
     * see {@link #getDocBits(String, String, Supplier)}
     */
    public static void setIndexInfo (IndexInfo indexInfo) {
        VirtualCorpusCache.indexInfo = indexInfo;
        synchronized (vcToCleanUp) {
            if (!vcToCleanUp.isEmpty()) {
                cleanup();
            }
        }
    }


    private static void cleanup () {
        final Set<String> currentLeafFingerprints = indexInfo
                .getAllLeafFingerprints();
        Map<String, DocBits> vcData;
        for (String vcId : vcToCleanUp) {
            vcData = map.get(vcId);
            vcData.keySet()
                    .removeIf(storedFingerPrint -> currentLeafFingerprints
                            .contains(storedFingerPrint) == false);
            store(vcId, vcData);
        }
        vcToCleanUp.clear();
    }


    /**
     * Gets DocBits for a single leaf from the VC cache or calculates
     * and stores it, if it doesn't exist in the cache. This can
     * happen when:
     * <ul>
     * <li> The VC has not been cached before</li>
     * <p>The VC will be cached with a single leaf-fingerprint in a
     * leafToDocBitMap. The map will be updated for the other leaf-
     * fingerprints and thus be cleaned up once.
     * </p>
     * <li>The index has been updated</li>
     * <p>
     * In this case, the VC may contain old leaf-fingerprints. It will
     * be clean up when the index is used next time.
     * </p>
     * </ul>
     * 
     * @see #setIndexInfo(IndexInfo)
     * @param vcId
     * @param leafFingerprint
     * @param calculateDocBits
     *            a supplier calculating the DocBits
     * @return DocBits
     */
    public static DocBits getDocBits (String vcId, String leafFingerprint,
            Supplier<DocBits> calculateDocBits) {
        DocBits docBits = null;
        Map<String, DocBits> leafToDocBitMap = retrieve(vcId);
        if (leafToDocBitMap == null) {
            leafToDocBitMap = Collections
                    .synchronizedMap(new HashMap<String, DocBits>());
            map.put(vcId, leafToDocBitMap);
        }
        else {
            docBits = leafToDocBitMap.get(leafFingerprint);
            if (docBits == null) {
                vcToCleanUp.add(vcId);
            }
        }
        if (docBits == null) {
            docBits = calculateDocBits.get();
            leafToDocBitMap.put(leafFingerprint, docBits);
            storeOnDisk(vcId, leafFingerprint, docBits);
        }
        return docBits;
    }
}
