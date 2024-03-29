package de.ids_mannheim.korap.index;

import java.util.*;
import java.io.*;

import java.nio.ByteBuffer;

import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PositionsToOffset {
    private String field;
    private LeafReaderContext atomic;
    private boolean processed = false;
    private Integer[] pair;
    private ByteBuffer bbOffset;

    HashSet<PositionsToOffsetArray> positions;
    HashMap<PositionsToOffsetArray, Integer[]> offsets;

    private final static Logger log = LoggerFactory
            .getLogger(PositionsToOffset.class);

    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;

    private class PositionsToOffsetArray {
        public int docID;
        public int pos;


        public PositionsToOffsetArray (int docID, int pos) {
            this.docID = docID;
            this.pos = pos;
        };


        public int hashCode () {
            long hashCode;
            hashCode = (docID * Integer.MAX_VALUE) - Integer.MAX_VALUE + pos;
            return Long.valueOf(hashCode).hashCode();
        };


        public boolean equals (Object obj) {
            if (obj instanceof PositionsToOffsetArray) {
                PositionsToOffsetArray ptoa = (PositionsToOffsetArray) obj;
                return (ptoa.docID == this.docID && ptoa.pos == this.pos);
            };
            return false;
        };
    };


    public PositionsToOffset (LeafReaderContext atomic, String field) {
        this.field = field;
        this.atomic = atomic;
        this.positions = new HashSet<>(64);
        this.offsets = new HashMap<>(64);
        this.bbOffset = ByteBuffer.allocate(8);
    };


    public void clear () {
        this.positions.clear();
        this.offsets.clear();
        this.bbOffset.clear();
        this.processed = false;
    };


    public void add (int docID, int pos) {
        this.add(new PositionsToOffsetArray(docID, pos));
    };


    public void add (PositionsToOffsetArray ptoa) {
        if (DEBUG)
            log.trace("Add positionsToOffsetArray {}/{}", ptoa.docID, ptoa.pos);
        if (ptoa.pos < 0)
            return;

        if (this.processed && this.exists(ptoa))
            return;

        if (DEBUG)
            log.trace("Reopen processing");

        this.positions.add(ptoa);
        this.processed = false;
    };


    public boolean exists (int docID, int pos) {
        return this.offsets.containsKey(new PositionsToOffsetArray(docID, pos));
    };


    public boolean exists (PositionsToOffsetArray ptoa) {
        return this.offsets.containsKey(ptoa);
    };


    public int start (int docID, int pos) {
        return this.start(new PositionsToOffsetArray(docID, pos));
    };


    public int start (PositionsToOffsetArray ptoa) {
        if (ptoa.pos < 0)
            return 0;

        if (!processed)
            this.offsets();

        Integer[] pair = this.offsets.get(ptoa);

        if (pair == null)
            return 0;

        return pair[0];
    };


    public int end (int docID, int pos) {
        return this.end(new PositionsToOffsetArray(docID, pos));
    };


    public int end (PositionsToOffsetArray ptoa) {
        if (ptoa.pos < 0)
            return -1;

        if (!processed)
            this.offsets();

        Integer[] pair = this.offsets.get(ptoa);
        if (pair == null)
            return -1;

        return pair[1];
    };


    public Integer[] span (int docID, int pos) {
        return this.span(new PositionsToOffsetArray(docID, pos));
    };


    public Integer[] span (PositionsToOffsetArray ptoa) {
        if (!processed)
            this.offsets();
        return this.offsets.get(ptoa);
    };


    public void addOffset (int docID, int pos, int startOffset, int endOffset) {
        offsets.put(new PositionsToOffsetArray(docID, pos),
                new Integer[] { startOffset, endOffset });
    };


    public HashMap<PositionsToOffsetArray, Integer[]> offsets () {
        if (processed)
            return offsets;

        if (DEBUG)
            log.trace("Process offsets");

        StringBuilder sb = new StringBuilder().append('_');

        try {
            Terms terms = atomic.reader().fields().terms(field);

            if (terms != null) {
                // TODO: Maybe reuse a termsEnum!

                final TermsEnum termsEnum = terms.iterator(null);

                for (PositionsToOffsetArray posDoc : positions) {
                    if (this.exists(posDoc))
                        continue;

                    int docID = posDoc.docID;

                    /*
                    int pos = posDoc[1];
                    Integer[] posDoc2 = new Integer[2];
                    posDoc2[0] = docID;
                    posDoc2[1] = pos;
                    */

                    sb.append(posDoc.pos);

                    Term term = new Term(field, sb.toString());
                    sb.setLength(1);

                    // Set the position in the iterator to the term that is seeked
                    if (termsEnum.seekExact(term.bytes())) {

                        if (DEBUG)
                            log.trace("Search for {} in doc {} with pos {}",
                                    term.toString(), posDoc.docID, posDoc.pos);

                        // Start an iterator to fetch all payloads of the term
                        DocsAndPositionsEnum docs = termsEnum.docsAndPositions(
                                null, null, DocsAndPositionsEnum.FLAG_PAYLOADS);

                        if (docs.advance(docID) == docID) {
                            docs.nextPosition();

                            BytesRef payload = docs.getPayload();

                            if (payload.length == 8) {
                                bbOffset.clear();
                                bbOffset.put(payload.bytes, payload.offset, 8);
                                bbOffset.rewind();
                                Integer[] offsetArray = new Integer[2];
                                offsetArray[0] = bbOffset.getInt();
                                offsetArray[1] = bbOffset.getInt();
                                offsets.put(posDoc, offsetArray);

                                if (DEBUG)
                                    log.trace("Found {}-{} for {}",
                                            offsetArray[0], offsetArray[1],
                                            term.toString());
                            }

                            else {
                                log.error("Doc {} has no offsets stored for {}",
                                        docID, term.toString());
                            };
                        };
                    };
                };
            };
        }
        catch (IOException e) {
            log.warn(e.getLocalizedMessage());
        };

        processed = true;
        positions.clear();
        return offsets;
    };


    public LeafReaderContext getLeafReader () {
        return this.atomic;
    };
};
