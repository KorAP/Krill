package de.ids_mannheim.korap.query.wrap;

import org.apache.lucene.search.spans.SpanQuery;

import de.ids_mannheim.korap.query.SpanReferenceQuery;
import de.ids_mannheim.korap.util.QueryException;

public class SpanReferenceQueryWrapper extends SpanQueryWrapper {

    private SpanQueryWrapper subQuery;
    private byte classNum;


    public SpanReferenceQueryWrapper (SpanQueryWrapper subQueryWrapper,
                                      byte classNum) {
        this.subQuery = subQueryWrapper;
        if (subQuery != null) {
            this.isNull = false;
        }
        else
            return;

        if (subQuery.isEmpty) {
            this.isEmpty = true;
            return;
        }

        if (classNum < 0) {
            throw new IllegalArgumentException(
                    "Class number must be bigger than 0.");
        }
        this.classNum = classNum;
        this.maybeUnsorted = subQueryWrapper.maybeUnsorted();
    }


    @Override
    public SpanQuery toFragmentQuery () throws QueryException {

        if (this.isNull() || this.isEmpty()) {
            return null;
        }

        SpanQuery sq = subQuery.retrieveNode(this.retrieveNode)
                .toFragmentQuery();
        if (sq == null)
            return null;

        return new SpanReferenceQuery(sq, classNum, true);
    }

}
