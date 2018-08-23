package de.ids_mannheim.korap.query;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.spans.ClassFilteredSpans;

/**
 * Filters query results by means of class operations.
 * 
 * @author margaretha
 * 
 */
public class SpanClassFilterQuery extends SimpleSpanQuery {

    public enum ClassOperation {
        DISJOINT, INTERSECT, INCLUDE, EQUAL, DIFFER
    }

    private ClassOperation operation;
    private byte classNum1, classNum2;


    public SpanClassFilterQuery (SpanQuery sq, ClassOperation type,
                                 int classNum1, int classNum2,
                                 boolean collectPayloads) {
        super(sq, collectPayloads);
        this.operation = type;
        this.classNum1 = (byte) classNum1;
        this.classNum2 = (byte) classNum2;
    }


    @Override
    public SimpleSpanQuery clone () {
        return new SpanClassFilterQuery((SpanQuery) firstClause.clone(),
                operation, classNum1, classNum2, collectPayloads);
    }


    @Override
    public Spans getSpans (LeafReaderContext context, Bits acceptDocs,
            Map<Term, TermContext> termContexts) throws IOException {
        return new ClassFilteredSpans(this, context, acceptDocs, termContexts);
    }


    @Override
    public String toString (String field) {
        StringBuilder sb = new StringBuilder();
        sb.append("spanClassFilter(");
        sb.append(firstClause.toString());
        sb.append(",");
        sb.append(operation);
        sb.append(",");
        sb.append(classNum1);
        sb.append(",");
        sb.append(classNum2);
        sb.append(")");
        return sb.toString();
    }


    public ClassOperation getOperation () {
        return operation;
    }


    public void setOperation (ClassOperation operation) {
        this.operation = operation;
    }


    public byte getClassNum1 () {
        return classNum1;
    }


    public void setClassNum1 (byte classNum1) {
        this.classNum1 = classNum1;
    }


    public byte getClassNum2 () {
        return classNum2;
    }


    public void setClassNum2 (byte classNum2) {
        this.classNum2 = classNum2;
    }

}
