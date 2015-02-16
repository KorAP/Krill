package de.ids_mannheim.korap.query.wrap;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;

import de.ids_mannheim.korap.query.SpanAttributeQuery;
import de.ids_mannheim.korap.query.SpanWithAttributeQuery;
import de.ids_mannheim.korap.query.SpanWithIdQuery;
import de.ids_mannheim.korap.util.QueryException;

public class SpanWithAttributeQueryWrapper extends SpanQueryWrapper {

	private SpanQueryWrapper withIdQueryWrapper = null;
	private SpanQueryWrapper attrQueryWrapper = null;
	private List<SpanQueryWrapper> queryWrapperList = null;

	public SpanWithAttributeQueryWrapper(SpanQueryWrapper withIdQuery,
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
	}

	public SpanWithAttributeQueryWrapper(SpanQueryWrapper withIdQuery,
			List<SpanQueryWrapper> attrList) {

		if (withIdQuery != null || attrList != null) {
			isNull = false;
		}
		if (withIdQuery.isEmpty) {
			isEmpty = true;
			return;
		}

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
		if (attrList.isEmpty()) {
			// not withattribute query, just a normal query
		}
		this.queryWrapperList = attrList;
		this.withIdQueryWrapper = withIdQuery;
	}

	public SpanAttributeQuery createSpanAttributeQuery(
			SpanQueryWrapper attrQueryWrapper) throws QueryException {
		SpanQuery sq = attrQueryWrapper.toQuery();
		if (sq == null) {
			isNull = true;
			return null;
		}
		if (sq instanceof SpanTermQuery) {
			return new SpanAttributeQuery(
					(SpanTermQuery) sq,
					attrQueryWrapper.isNegative, true);
		} 
		else {
			throw new IllegalArgumentException(
					"The subquery is not a SpanTermQuery.");
		}
	}

	@Override
	public SpanQuery toQuery() throws QueryException {

		if (isNull || isEmpty) return null;
		
		SpanWithIdQuery withIdQuery = (SpanWithIdQuery) withIdQueryWrapper
				.toQuery();
		if (withIdQuery == null) {
			isNull = true;
			return null;
		}
		
		if (attrQueryWrapper != null){
			SpanAttributeQuery attrQuery = createSpanAttributeQuery(attrQueryWrapper);
			if (attrQuery == null) {
				isNull = true;
				return null;
			}
			return new SpanWithAttributeQuery(withIdQuery, attrQuery, true);
		}
		else if (queryWrapperList != null) {
			if (queryWrapperList.isEmpty()) {
				return withIdQuery;
			}

			List<SpanQuery> attrQueries = new ArrayList<SpanQuery>();
			SpanQuery attrQuery;
			for (SpanQueryWrapper sqw : queryWrapperList) {
				attrQuery = createSpanAttributeQuery(sqw);
				if (attrQuery == null) {
					isNull = true;
					return null;
				}
				attrQueries.add(attrQuery);
			}
			return new SpanWithAttributeQuery(withIdQuery, attrQueries, true);
		}
		return null;		
	}
}
