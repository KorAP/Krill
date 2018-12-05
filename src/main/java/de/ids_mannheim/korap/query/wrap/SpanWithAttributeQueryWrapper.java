package de.ids_mannheim.korap.query.wrap;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;

import de.ids_mannheim.korap.query.SimpleSpanQuery;
import de.ids_mannheim.korap.query.SpanAttributeQuery;
import de.ids_mannheim.korap.query.SpanWithAttributeQuery;
import de.ids_mannheim.korap.util.QueryException;

/**
 * No optimization using expansion
 * 
 * @author margaretha
 */
public class SpanWithAttributeQueryWrapper extends SpanQueryWrapper {

    private SpanQueryWrapper withIdQueryWrapper = null;
    private SpanQueryWrapper attrQueryWrapper = null;
    private List<SpanQueryWrapper> queryWrapperList = null;
    private boolean isSingleAttribute = false;


    public SpanWithAttributeQueryWrapper (SpanQueryWrapper attrQuery)
            throws QueryException {
        if (attrQuery != null)
            isNull = false;
        if (attrQuery.isEmpty()) {
            isEmpty = true;
            return;
        }
        if (attrQuery.isNegative) {
            throw new QueryException(
                    "The query requires a positive attribute.");
        }
        this.attrQueryWrapper = attrQuery;
        this.maybeUnsorted = attrQuery.maybeUnsorted();
        this.isSingleAttribute = true;
        
    }


    public SpanWithAttributeQueryWrapper (List<SpanQueryWrapper> attrList)
            throws QueryException {

        if (attrList != null)
            isNull = false;
        if (attrList.isEmpty()) {
            throw new QueryException("No attribute is defined.");
        }

        boolean isAllNegative = true;
        for (SpanQueryWrapper sqw : attrList) {
            if (sqw == null) {
                isNull = true;
                return;
            }
            if (sqw.isEmpty) {
                isEmpty = true;
                return;
            }
            if (!sqw.isNegative) {
                isAllNegative = false;
            }
        }
        if (isAllNegative) {
            throw new QueryException("No positive attribute is defined.");
        }
        this.queryWrapperList = attrList;
    }


    public SpanWithAttributeQueryWrapper (SpanQueryWrapper withIdQuery,
                                          SpanQueryWrapper attrQuery) {

        if (withIdQuery != null || attrQuery != null) {
            isNull = false;
        }
        if (withIdQuery.isEmpty || attrQuery.isEmpty()) {
            isEmpty = true;
            return;
        }

        this.attrQueryWrapper = attrQuery;
        this.withIdQueryWrapper = withIdQuery;
        this.isSingleAttribute = true;
    }


    public SpanWithAttributeQueryWrapper (SpanQueryWrapper withIdQuery,
                                          List<SpanQueryWrapper> attrList) {

        if (withIdQuery != null || attrList != null) {
            isNull = false;
        }
        if (withIdQuery.isEmpty) {
            isEmpty = true;
            return;
        }
        // if (attrList.isEmpty()) {
        // not withattribute query, just a normal query
        // }

        for (SpanQueryWrapper sqw : attrList) {
            if (sqw == null) {
                isNull = true;
                return;
            }
            if (sqw.isEmpty) {
                isEmpty = true;
                return;
            }
        }

        this.queryWrapperList = attrList;
        this.withIdQueryWrapper = withIdQuery;
    }


    @Override
    public SpanQuery toFragmentQuery () throws QueryException {

        if (isNull || isEmpty)
            return null;
        if (withIdQueryWrapper != null) {
            return createSpecificSpanWithAttributeQuery();
        }
        else {
            return createArbitrarySpanWithAttributeQuery();
        }
    }


    private SpanQuery createSpecificSpanWithAttributeQuery ()
            throws QueryException {
        SimpleSpanQuery withIdQuery = (SimpleSpanQuery) withIdQueryWrapper
                .toFragmentQuery();
        if (withIdQuery == null) {
            isNull = true;
            return null;
        }
        if (isSingleAttribute) {
            return createSpanWithSingleAttributeQuery(withIdQuery);
        }
        else if (queryWrapperList.isEmpty()) {
            return withIdQuery;
        }
        else {
            return createSpanWithAttributeListQuery(withIdQuery);
        }
    }


    private SpanWithAttributeQuery createSpanWithSingleAttributeQuery (
            SimpleSpanQuery withIdQuery) throws QueryException {
        SpanAttributeQuery attrQuery = createSpanAttributeQuery(
                this.attrQueryWrapper);
        if (attrQuery != null) {
            if (withIdQuery != null) {
                return new SpanWithAttributeQuery(withIdQuery, attrQuery, true);
            }
            else {
                return new SpanWithAttributeQuery(attrQuery, true);
            }
        }
        isNull = true;
        return null;
    }


    private SpanAttributeQuery createSpanAttributeQuery (
            SpanQueryWrapper attrQueryWrapper) throws QueryException {
        SpanQuery sq = attrQueryWrapper.toFragmentQuery();
        if (sq != null) {
            if (sq instanceof SpanAttributeQuery)
                return (SpanAttributeQuery) sq;
            if (sq instanceof SpanTermQuery) {
                return new SpanAttributeQuery((SpanTermQuery) sq,
                        attrQueryWrapper.isNegative, true);
            }
            else {
                throw new IllegalArgumentException(
                        "The subquery is not a SpanTermQuery.");
            }
        }
        return null;
    }


    private SpanWithAttributeQuery createSpanWithAttributeListQuery (
            SimpleSpanQuery withIdQuery) throws QueryException {
        List<SpanQuery> attrQueries = new ArrayList<SpanQuery>();
        SpanQuery attrQuery = null;
        for (SpanQueryWrapper sqw : queryWrapperList) {
            attrQuery = createSpanAttributeQuery(sqw);
            if (attrQuery == null) {
                isNull = true;
                return null;
            }
            attrQueries.add(attrQuery);
        }

        if (withIdQuery != null) {
            return new SpanWithAttributeQuery(withIdQuery, attrQueries, true);
        }
        else {
            return new SpanWithAttributeQuery(attrQueries, true);
        }
    }


    private SpanQuery createArbitrarySpanWithAttributeQuery ()
            throws QueryException {
        if (isSingleAttribute) {
            return createSpanWithSingleAttributeQuery(null);
        }
        else if (queryWrapperList.isEmpty()) {
            throw new QueryException("No attribute is defined.");
        }
        else {
            return createSpanWithAttributeListQuery(null);
        }
    }


}
