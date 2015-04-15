package de.ids_mannheim.korap.query.wrap;

import java.util.ArrayList;

import org.apache.lucene.search.spans.SpanQuery;

import de.ids_mannheim.korap.query.SpanFocusQuery;
import de.ids_mannheim.korap.query.SpanSegmentQuery;
import de.ids_mannheim.korap.util.QueryException;

public class SpanRelationWrapper extends SpanQueryWrapper {

    private SpanQueryWrapper relationQuery;
    private SpanQueryWrapper subQuery1;
    private SpanQueryWrapper subQuery2;
    private byte[] classNumbers;

    public SpanRelationWrapper (SpanQueryWrapper relationWrapper,
            SpanQueryWrapper operand1, SpanQueryWrapper operand2,
            byte[] classNumbers) {

        this.relationQuery = relationWrapper;
        if (relationQuery != null) {
            this.isNull = false;
        }
        else
            return;

        if (relationQuery.isEmpty) {
            this.isEmpty = true;
            return;
        }

        this.subQuery1 = operand1;
        this.subQuery2 = operand2;
        this.classNumbers = classNumbers;
    }

    @Override
    public SpanQuery toQuery() throws QueryException {

        if (this.isNull() || this.isEmpty()) {
            return null;
        }

        SpanQuery sq = relationQuery.retrieveNode(this.retrieveNode).toQuery();
        if (sq == null) return null;

        ArrayList<Byte> classNumbers = new ArrayList<Byte>();
        for (byte c : this.classNumbers) {
            if (c > 0) {
                classNumbers.add(c);
            }
        }

        SpanQuery subq1, subq2;
        if (subQuery1.isEmpty) {
            if (!subQuery2.isEmpty) {
                // match subquery2
                subq2 = subQuery2.retrieveNode(this.retrieveNode).toQuery();
                if (subq2 != null) {
                    return new SpanFocusQuery(
                            new SpanSegmentQuery(sq, subq2, true), classNumbers);
                }
            }
        }
        else if (subQuery2.isEmpty) {
            if (!subQuery1.isEmpty) {
                // match subquery1
                subq1 = subQuery1.retrieveNode(this.retrieveNode).toQuery();
                if (subq1 != null) {
                    return new SpanFocusQuery(
                            new SpanSegmentQuery(sq, subq1, true),
                        classNumbers);
                }
            }               
        }
        else{
            // match both
            subq1 = subQuery1.retrieveNode(this.retrieveNode).toQuery();
            if (subq1 != null) {
                SpanFocusQuery fq = new SpanFocusQuery(new SpanSegmentQuery(sq, subq1,
                        true), (byte) 2);
                fq.setSorted(false);
                sq = fq;
            }
            
            subq2 = subQuery2.retrieveNode(this.retrieveNode).toQuery();
            if (subq2 != null){
                return new SpanFocusQuery(new SpanSegmentQuery(sq, subq2, true),
                    classNumbers);
            }
        }
        
        return new SpanFocusQuery(sq, classNumbers);
    }
}
