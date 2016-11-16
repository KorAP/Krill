package de.ids_mannheim.korap.query;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.spans.ReferenceSpans;

/**
 * SpanReferenceQuery ensures that a span involving in more than one
 * operations are indeed the same spans. Such a span is referred by a
 * class and cannot be ensured in one nested SpanQuery.
 * 
 * For instance in the following Annis query
 * 
 * <pre>
 * cat="vb" & cat="prp" & cat="nn" & #1 .{0,1} #2 & #1 .{0,2} #3
 * & #3 -> #2
 * </pre>
 * 
 * cat="prp" is referred by a class with number 2 and involves in two
 * operations. After resolving the first and second operations, class
 * number 3 and 2 have to be referred at the same time to solve the
 * third operation. However, only one class can be focused on from a
 * span at one time. Let say, class number 3 is focused on from the
 * resulting spans of the first and second operation, then it is
 * matched with a new span enumeration of cat="prp" for the third
 * operation.
 * 
 * SpanReferenceQuery ensures that cat="prp" spans in the third
 * operation are the same as the those in the first operation by
 * matching their positions using the class number 2 payloads kept in
 * spans focussing on the class number 3 (it keeps all the payloads
 * from previous operations).
 * 
 * @author margaretha
 *
 */
public class SpanReferenceQuery extends SimpleSpanQuery {

    private byte classNum;

    public SpanReferenceQuery (SpanQuery firstClause, byte classNum,
                               boolean collectPayloads) {
        super(firstClause, collectPayloads);
        this.classNum = classNum;
    }


    @Override
    public SimpleSpanQuery clone () {
        return null;
    }


    @Override
    public Spans getSpans (LeafReaderContext context, Bits acceptDocs,
            Map<Term, TermContext> termContexts) throws IOException {
        return new ReferenceSpans(this, context, acceptDocs, termContexts);
    }


    @Override
    public String toString (String field) {
        StringBuilder sb = new StringBuilder();
        sb.append("spanReference(");
        sb.append(firstClause.toString());
        sb.append(", ");
        sb.append(classNum);
        sb.append(")");
        return sb.toString();
    }


    /** Get the class number of the referred spans.
     * @return the class number of the referred spans
     */
    public byte getClassNum () {
        return classNum;
    }


    /** Set the class number of the referred spans. 
     * 
     * @param classNum the class number of the referred spans
     */
    public void setClassNum (byte classNum) {
        this.classNum = classNum;
    }

}
