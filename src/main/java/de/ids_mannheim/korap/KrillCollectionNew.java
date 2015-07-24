package de.ids_mannheim.korap;

import java.util.*;
import java.io.IOException;

import de.ids_mannheim.korap.collection.CollectionBuilderNew;
import de.ids_mannheim.korap.response.Notifications;

import org.apache.lucene.search.*;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.util.FixedBitSet;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.OpenBitSet;
import org.apache.lucene.util.DocIdBitSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KrillCollectionNew extends Notifications {
    private KrillIndex index;
    private CollectionBuilderNew.CollectionBuilderInterface cb;

    // Logger
    private final static Logger log = LoggerFactory
            .getLogger(KrillCollection.class);

    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = true;


    /**
     * Construct a new KrillCollection by passing a KrillIndex.
     * 
     * @param index
     *            The {@link KrillIndex} object.
     */
    public KrillCollectionNew (KrillIndex index) {
        this.index = index;
    };

    public KrillCollectionNew fromBuilder (CollectionBuilderNew.CollectionBuilderInterface cb) {
        this.cb = cb;
        return this;
    };

    public Filter toFilter () {
        if (this.cb == null)
            return null;

        return this.cb.toFilter();
    };

    public String toString () {
        Filter filter = this.toFilter();
        if (filter == null)
            return "";

        return filter.toString();
    };

    public FixedBitSet bits (AtomicReaderContext atomic) throws IOException {

        int maxDoc = atomic.reader().maxDoc();
        FixedBitSet bitset = new FixedBitSet(maxDoc);

        Filter filter;
        if (this.cb == null || (filter = this.cb.toFilter()) == null)
            return null;

        // Init vector
        DocIdSet docids = filter.getDocIdSet(atomic, atomic.reader().getLiveDocs());
        DocIdSetIterator filterIter = (docids == null) ? null : docids.iterator();

        if (filterIter == null) {
            if (!this.cb.isNegative())
                return null;

            bitset.set(0, maxDoc);
        }
        else {
            // Or bit set
            bitset.or(filterIter);

            // Revert for negation
            if (this.cb.isNegative())
                bitset.flip(0, maxDoc);
        };

        // Remove deleted docs
        /*
        System.err.println(atomic.reader().getClass());
        FixedBitSet livedocs = (FixedBitSet) atomic.reader().getLiveDocs();
        if (livedocs != null) {
            bitset.and(livedocs);
        };
        */

        return bitset;
    };

    /**
     * Search for the number of occurrences of different types,
     * e.g. <i>documents</i>, <i>sentences</i> etc. in the virtual
     * collection.
     * 
     * @param field
     *            The field containing the textual data and the
     *            annotations as a string.
     * @param type
     *            The type of meta information,
     *            e.g. <i>documents</i> or <i>sentences</i> as a
     *            string.
     * @return The number of the occurrences.
     * @throws IOException
     * @see KrillIndex#numberOf
     */
    public long numberOf (String field, String type) throws IOException {

        // No index defined
        if (this.index == null)
            return (long) -1;

        // This is redundant to index stuff
        if (type.equals("documents"))
            return this.docCount();
        
        return (long) 0;
        // return this.index.numberOf(this, field, type);
    };



    public long docCount () {

        // No index defined
        if (this.index == null)
            return (long) 0;

        long docCount = 0;
        try {
            FixedBitSet bitset;
            for (AtomicReaderContext atomic : this.index.reader().leaves()) {
                if ((bitset = this.bits(atomic)) != null)
                    docCount += bitset.cardinality();
            };
        }
        catch (IOException e) {
            log.warn(e.getLocalizedMessage());
        };
        return docCount;
    };
};
