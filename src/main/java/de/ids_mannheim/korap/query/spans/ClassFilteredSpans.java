package de.ids_mannheim.korap.query.spans;

import static de.ids_mannheim.korap.util.KrillByte.byte2int;

import java.io.IOException;
import java.util.BitSet;
import java.util.Map;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.SpanClassFilterQuery;
import de.ids_mannheim.korap.query.SpanClassFilterQuery.ClassOperation;

public class ClassFilteredSpans extends SimpleSpans {

    private BitSet bitset1, bitset2;
    private ClassOperation operation;
    private byte classNum1, classNum2;


    public ClassFilteredSpans (SpanClassFilterQuery query,
                               LeafReaderContext context, Bits acceptDocs,
                               Map<Term, TermContext> termContexts)
            throws IOException {
        super(query, context, acceptDocs, termContexts);
        this.operation = query.getOperation();
        this.classNum1 = query.getClassNum1();
        this.classNum2 = query.getClassNum2();
        hasMoreSpans = firstSpans.next();
    }


    @Override
    public boolean next () throws IOException {
        while (hasMoreSpans) {
            matchPayload.clear();
            bitset1 = null;
            bitset2 = null;
            if (isClassOperationValid()) {
                this.matchStartPosition = firstSpans.start();
                this.matchEndPosition = firstSpans.end();
                this.matchDocNumber = firstSpans.doc();
                this.matchPayload.addAll(firstSpans.getPayload());
                hasMoreSpans = firstSpans.next();
                return true;
            }
            hasMoreSpans = firstSpans.next();
        }
        return false;
    }


    private boolean isClassOperationValid () throws IOException {
        setBitsets();
        int cardinality = Math
                .max(bitset1.cardinality(), bitset2.cardinality());
        bitset1.and(bitset2);
        // System.out.println("cardinality:" + cardinality);
        switch (operation) {
            case DISJOINT:
                if (bitset1.cardinality() == 0)
                    return true;
                break;
            case EQUAL:
                if (cardinality == bitset1.cardinality())
                    return true;
                break;
            case DIFFER:
                if (cardinality == 0 || cardinality != bitset1.cardinality())
                    return true;
                break;
            case INCLUDE:
                if (bitset1.cardinality() == bitset2.cardinality()) {
                    return true;
                }
                break;
            case INTERSECT:
                if (bitset1.cardinality() > 0)
                    return true;
                break;
        }

        return false;
    }


    private void setBitsets () throws IOException {
        BitSet bs = new BitSet();
        int start, end;
        // System.out.println("------------------------");
        for (byte[] payload : firstSpans.getPayload()) {
            if (payload.length == 9) {
                start = byte2int(payload, 0) + 1;
                end = byte2int(payload, 4) + 1;
                if (payload[8] == classNum1) {
                    // System.out.println("bitset1 " + start + " " +
                    // end);
                    if (bitset1 == null) {
                        bitset1 = new BitSet();
                        bitset1.set(start, end);
                    }
                    else {
                        bs.set(start, end);
                        bitset1.or(bs);
                    }
                    // System.out.println(bitset1);
                }
                else if (payload[8] == classNum2) {
                    // System.out.println("#bitset2 " + start + " " +
                    // end);
                    if (bitset2 == null) {
                        bitset2 = new BitSet();
                        bitset2.set(start, end);
                    }
                    else {
                        bs.set(start, end);
                        bitset2.or(bs);
                        // System.out.println("OR #2");
                    }
                    // System.out.println(bitset2);
                }
            }
        }

    }


    @Override
    public boolean skipTo (int target) throws IOException {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public long cost () {
        // TODO Auto-generated method stub
        return 0;
    }
}
