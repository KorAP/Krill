package de.ids_mannheim.korap.util;

import java.io.IOException;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.SegmentCommitInfo;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.index.SegmentReader;

/**
 * Describes index readers by strings that only depend on what was
 * committed to the index: segment names, Lucene versions, numbers of
 * documents and deletions. They are the keys of the index
 * fingerprints (see {@link Fingerprinter}), e.g. for the search
 * cache and the virtual corpus cache: equal indexes have equal keys,
 * and every change of the index changes them.
 *
 * Up to Lucene 6, the <tt>toString()</tt> of the readers was used
 * as the key, and the strings created here are in the same format.
 * Since Lucene 7, it includes the diagnostics of each segment (e.g.
 * the time it was written and the Java version), so equal indexes
 * would get different keys.
 */
public final class ReaderKeys {

    private ReaderKeys () {};


    /**
     * The key of an index reader: of a directory reader, the
     * segments file, the index version and all segments; of a leaf
     * reader, its segment.
     */
    public static String of (IndexReader reader) {
        if (reader instanceof LeafReader)
            return of((LeafReader) reader);

        if (!(reader instanceof DirectoryReader))
            return reader.toString();

        DirectoryReader dirReader = (DirectoryReader) reader;
        StringBuilder s = new StringBuilder(
                reader.getClass().getSimpleName());
        s.append('(');
        try {
            s.append(dirReader.getIndexCommit().getSegmentsFileName())
                    .append(':').append(dirReader.getVersion());
        }
        catch (IOException e) {
            // The commit is not available - the leaves describe the
            // index nonetheless
        };
        for (LeafReaderContext leaf : reader.leaves()) {
            s.append(' ').append(of(leaf.reader()));
        };
        return s.append(')').toString();
    };


    /**
     * The key of a leaf reader: the name, Lucene version and number
     * of documents of its segment, its deletions and the generations
     * of its updates.
     */
    public static String of (LeafReader reader) {
        if (!(reader instanceof SegmentReader))
            return reader.toString();

        SegmentCommitInfo commitInfo = ((SegmentReader) reader)
                .getSegmentInfo();
        SegmentInfo info = commitInfo.info;

        StringBuilder s = new StringBuilder(info.name);
        s.append('(')
                .append(info.getVersion() == null ? "?" : info.getVersion())
                .append("):");
        s.append(info.getUseCompoundFile() ? 'c' : 'C');
        s.append(info.maxDoc());

        int delCount = info.maxDoc() - reader.numDocs();
        if (delCount != 0)
            s.append('/').append(delCount);

        if (info.getIndexSort() != null)
            s.append(":[indexSort=").append(info.getIndexSort()).append(']');

        if (commitInfo.getDelGen() != -1)
            s.append(":delGen=").append(commitInfo.getDelGen());
        if (commitInfo.getFieldInfosGen() != -1)
            s.append(":fieldInfosGen=").append(commitInfo.getFieldInfosGen());
        if (commitInfo.getDocValuesGen() != -1)
            s.append(":dvGen=").append(commitInfo.getDocValuesGen());

        return s.toString();
    };
};
