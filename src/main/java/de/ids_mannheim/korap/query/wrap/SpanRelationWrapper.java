package de.ids_mannheim.korap.query.wrap;

import org.apache.lucene.search.spans.SpanQuery;

import de.ids_mannheim.korap.constants.RelationDirection;
import de.ids_mannheim.korap.query.SpanFocusQuery;
import de.ids_mannheim.korap.query.SpanRelationMatchQuery;
import de.ids_mannheim.korap.query.SpanRelationQuery;
import de.ids_mannheim.korap.util.QueryException;

public class SpanRelationWrapper extends SpanQueryWrapper {

    private SpanQueryWrapper relationQuery;
    private SpanQueryWrapper subQuery1;
    private SpanQueryWrapper subQuery2;
    private RelationDirection direction;

    public SpanRelationWrapper (SpanQueryWrapper relationWrapper,
                                SpanQueryWrapper operand1,
                                SpanQueryWrapper operand2) {

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
        this.maybeUnsorted = true;
    }


    @Override
    public SpanQuery toFragmentQuery () throws QueryException {

        if (this.isNull() || this.isEmpty()) {
            return null;
        }

        SpanQuery relationTermQuery = relationQuery
                .retrieveNode(this.retrieveNode).toFragmentQuery();
        
//        SpanTermQuery relationTermQuery = (SpanTermQuery) relationQuery
//                .retrieveNode(this.retrieveNode).toFragmentQuery();
        if (relationTermQuery == null)
            return null;

        SpanRelationQuery rq = new SpanRelationQuery(relationTermQuery, true, direction);
        SpanQuery subq1, subq2;

        if (subQuery1.isEmpty) {
            if (!subQuery2.isEmpty) {
                // match target
                subq2 = subQuery2.retrieveNode(this.retrieveNode)
                        .toFragmentQuery();
                if (subQuery1.hasClass) {
                    rq.setSourceClass(subQuery1.getClassNumber());
                }

                return new SpanRelationMatchQuery(rq, subq2, true);
            }
        }
        else if (subQuery2.isEmpty) {
            if (!subQuery1.isEmpty) {
                // match source
                subq1 = subQuery1.retrieveNode(this.retrieveNode)
                        .toFragmentQuery();
                if (subQuery2.hasClass) {
                    rq.setTargetClass(subQuery2.getClassNumber());
                }
                return new SpanRelationMatchQuery(rq, subq1, true);
            }
        }
        else {
            // match both
            subq1 = subQuery1.retrieveNode(this.retrieveNode).toFragmentQuery();
            subq2 = subQuery2.retrieveNode(this.retrieveNode).toFragmentQuery();
            return new SpanRelationMatchQuery(rq, subq1, subq2, true);
        }

        // both empty
        if (subQuery1.hasClass) {
            rq.setSourceClass(subQuery1.getClassNumber());
        }
        if (subQuery2.hasClass) {
            rq.setTargetClass(subQuery2.getClassNumber());
        }
        SpanFocusQuery fq = new SpanFocusQuery(rq, rq.getTempClassNumbers());
        fq.setMatchTemporaryClass(true);
        fq.setRemoveTemporaryClasses(true);
        fq.setSorted(false);
        return fq;
    }
    
    public void setDirection (RelationDirection direction) {
        this.direction = direction;
    }
    public RelationDirection getDirection () {
        return direction;
    }
}
