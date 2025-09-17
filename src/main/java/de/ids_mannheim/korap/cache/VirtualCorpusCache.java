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
import java.util.regex.Pattern;

import org.apache.lucene.index.LeafReaderContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public final static Logger log = LoggerFactory
            .getLogger(VirtualCorpusCache.class);
    
    public static Pattern vcNamePattern = Pattern.compile("[a-zA-Z0-9]+[a-zA-Z_0-9-.]+");

    public static String CACHE_LOCATION = "vc-cache";
    
    public static int CAPACITY = 5;
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

    public static final Set<String> vcToCleanUp = Collections
            .synchronizedSet(new HashSet<>());
    
    public static volatile boolean isCleaning = false;


    public VirtualCorpusCache () {
        File dir = new File(CACHE_LOCATION);
        dir.mkdirs();
    }

    /**
     * Path traversal must not be allowed using the VC ID.
     * 
     * VC id may only have one slash with the following format:
     * [username]/[vc-name]
     * 
     * VC name may only contains alphabets, numbers, dashes and
     * full-stops. See {@link #vcNamePattern}
     * 
     * @param vcId
     * @return true if the given VC id is valid, false otherwise
     */
    private static boolean isVcIdValid (String vcId) {
//        if (vcId.contains("./")) {
//            return false;
//        }

        String[] parts = vcId.split("/");
        if (parts.length > 2) {
            return false;
        }

        String vcName = parts.length == 2 ? parts[1] : parts[0];
        if (!vcNamePattern.matcher(vcName).matches()) {
            return false;
        }

        return true;
    }


    public static void storeOnDisk (String vcId, String leafFingerprint,
            DocBits docBits) {
        if (!isVcIdValid(vcId)) {
            throw new IllegalArgumentException("Cannot cache VC due to invalid VC ID");
        }

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


    public static void store (String vcId, Map<String, DocBits> vcData){
        map.put(vcId, vcData);
        vcData.keySet().forEach(leafFingerprint -> {
            storeOnDisk(vcId, leafFingerprint, vcData.get(leafFingerprint));
        });
    }


    public static void store (String vcId, KrillIndex index) {

        if (!isVcIdValid(vcId)) {
            throw new IllegalArgumentException("Cannot cache VC due to invalid VC ID");
        }
        
        DocBitsSupplier docBitsSupplier = new VirtualCorpusFilter(
                vcId).getDocBitsSupplier();
        String leafFingerprint;
        for (LeafReaderContext context : index.reader().leaves()) {
            leafFingerprint = Fingerprinter.create(
                    context.reader().getCombinedCoreAndDeletesKey().toString());

            getDocBits(vcId, leafFingerprint, () -> {
                try {
                    return docBitsSupplier.supplyDocBits(context,
                            context.reader().getLiveDocs());
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }


    /** Retrieve a VC from the cache, either from the memory map or disk.
     * 
     * @param vcId
     * @return a map of index leaves and DocBits, otherwise null if not found.
     */
    public static Map<String, DocBits> retrieve (String vcId) {
        Map<String, DocBits> vcData = map.get(vcId);
        if (vcData != null) {
            return vcData;
        }
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
            vcData = Collections.synchronizedMap(vcData);
            map.put(vcId, vcData);
        }
        return vcData;

    }


    public static boolean contains (String vcId) {
        if (!isVcIdValid(vcId)) {
            return false;
        }

        if (map.containsKey(vcId)) {
            return true;
        }
        else {
            File f = new File(CACHE_LOCATION + "/" + vcId);
            return f.exists();
        }
    }


    /**
     * Deletes the VC from memory cache and disk cache. If VC doesn't
     * exist, the method keeps silent about it and no error will be
     * thrown because the deletion purpose has been achieved.
     * 
     * @param vcId
     */
    public static void delete (String vcId) {
        if (!isVcIdValid(vcId)) {
            return;
        }

        map.remove(vcId);
        if (!isCleaning) { 
        	vcToCleanUp.remove(vcId);
        }
        
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
        if (!vcCache.exists()) {
            return;
        }
        File[] vcs = vcCache.listFiles();
        if (vcs != null) {
            for (File vc : vcs) {
                File[] files = vc.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.exists()) {
                            f.delete();
                        }
                    }
                }
                vc.delete();
            }
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
     * 
     * @throws QueryException
     */
    public static void setIndexInfo (IndexInfo indexInfo) {
        VirtualCorpusCache.indexInfo = indexInfo;
        //synchronized (vcToCleanUp) {
            if (!vcToCleanUp.isEmpty() && !isCleaning) {
            	isCleaning = true;
                cleanup();
                isCleaning = false;
            }
        //}
    }


    /** Remove out-dated leaves that are not used anymore due to index update 
     * (i.e., by sending a close-index-reader-API request)
     * 
     */
    private static void cleanup () {
        final Set<String> currentLeafFingerprints = indexInfo
                .getAllLeafFingerprints();
        Map<String, DocBits> vcData;
        for (String vcId : vcToCleanUp) {
            vcData = retrieve(vcId);
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
     * @throws QueryException
     */
    public static DocBits getDocBits (String vcId, String leafFingerprint,
            Supplier<DocBits> calculateDocBits) {
        DocBits docBits = null;
        Map<String, DocBits> leafToDocBitMap = retrieve(vcId);
        // if VC is not in the cache (both memory and disk), 
        // put it in the memory map
        if (leafToDocBitMap == null) {
            leafToDocBitMap = Collections
                    .synchronizedMap(new HashMap<String, DocBits>());
            map.put(vcId, leafToDocBitMap);
        }
        else {
            docBits = leafToDocBitMap.get(leafFingerprint);
            // VC-id is the cache but there is no data for the leaf
            if (docBits == null && !isCleaning) {
                vcToCleanUp.add(vcId);
            }
        }
        if (docBits == null) {
        	/* Calculating docBits and storing in the cache
        	 * 
        	 * This process is triggered when finding a JSON-LD file at 
        	 * the named-vc folder that doesn't exist in the cache.
        	 * 
        	 * It should only happens at server start-up, or index update
        	 * for a small number of new leaves.
        	 * 
        	 * New named VC should *not* be added at a running instance, as 
        	 * it would trigger this process.
        	 */   
            docBits = calculateDocBits.get();
            leafToDocBitMap.put(leafFingerprint, docBits);
            storeOnDisk(vcId, leafFingerprint, docBits);
        }
        return docBits;
    }
}
