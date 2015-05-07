package de.ids_mannheim.korap.query;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.spans.ReferenceSpans;

public class SpanReferenceQuery extends SimpleSpanQuery {

    private byte classNum;

    public SpanReferenceQuery (SpanQuery firstClause, byte classNum,
            boolean collectPayloads) {
        super(firstClause, collectPayloads);
        this.classNum = classNum;
    }


    @Override
    public SimpleSpanQuery clone () {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public Spans getSpans (AtomicReaderContext context, Bits acceptDocs,
            Map<Term, TermContext> termContexts) throws IOException {
        // TODO Auto-generated method stub
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


    public byte getClassNum() {
        return classNum;
    }

    public void setClassNum(byte classNum) {
        this.classNum = classNum;
    }

}
