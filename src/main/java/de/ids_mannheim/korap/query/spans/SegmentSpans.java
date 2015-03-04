package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.SpanSegmentQuery;

/**
 * SegmentSpans is an enumeration of Span matches in which that two
 * child spans
 * have exactly the same start and end positions.
 * 
 * @author margaretha
 * */
public class SegmentSpans extends NonPartialOverlappingSpans {

    private boolean isRelation;


    /**
     * Creates SegmentSpans from the given {@link SpanSegmentQuery}.
     * 
     * @param spanSegmentQuery
     *            a spanSegmentQuery.
     * @param context
     * @param acceptDocs
     * @param termContexts
     * @throws IOException
     */
    public SegmentSpans (SpanSegmentQuery spanSegmentQuery,
                         AtomicReaderContext context, Bits acceptDocs,
                         Map<Term, TermContext> termContexts)
            throws IOException {
        super(spanSegmentQuery, context, acceptDocs, termContexts);
        if (spanSegmentQuery.isRelation()) {
            SpansWithId s2 = (SpansWithId) secondSpans;
            // hacking for element query
            s2.hasSpanId = true;
            isRelation = true;
        }
    }


    /**
     * Check weather the start and end positions of the current
     * firstspan and
     * secondspan are identical.
     * 
     * */
    @Override
    protected int findMatch () {
        RelationSpans s1;
        SpansWithId s2;
        if (firstSpans.start() == secondSpans.start()
                && firstSpans.end() == secondSpans.end()) {

            if (isRelation) {
                s1 = (RelationSpans) firstSpans;
                s2 = (SpansWithId) secondSpans;

                //System.out.println("segment: " + s1.getRightStart() + " "
                // + s1.getRightEnd());
                if (s1.getLeftId() == s2.getSpanId()) {
                    setMatch();
                    return 0;
                }
            }
            else {
                setMatch();
                return 0;
            }
        }

        if (firstSpans.start() < secondSpans.start()
                || firstSpans.end() < secondSpans.end())
            return -1;

        return 1;
    }


    private void setMatch () {
        matchDocNumber = firstSpans.doc();
        matchStartPosition = firstSpans.start();
        matchEndPosition = firstSpans.end();
    }
}
