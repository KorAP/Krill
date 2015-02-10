package de.ids_mannheim.korap.query.wrap;

import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ids_mannheim.korap.query.SpanSubspanQuery;
import de.ids_mannheim.korap.util.QueryException;

/**
 * Automatically handle the length parameter if it is less than 0, then no
 * SpanSubspanQuery is created, but a SpanQuery for the subquery ?
 * 
 * @author margaretha, diewald
 * 
 */
public class SpanSubspanQueryWrapper extends SpanQueryWrapper {

	private SpanQueryWrapper subquery;
	private int startOffset, length;

	private final static Logger log = LoggerFactory
			.getLogger(SpanSubspanQueryWrapper.class);

	// This advices the java compiler to ignore all loggings
	public static final boolean DEBUG = false;

	public SpanSubspanQueryWrapper(SpanQueryWrapper sqw, int startOffset,
			int length) throws QueryException {
		if (length < 0) {
			throw new QueryException(
					"SpanSubspanQuery cannot have length less than 0.");
		}

		this.subquery = sqw;
		if (subquery != null) {
			this.isNull = false;
		} else
			return;

		this.startOffset = startOffset;
		this.length = length;

		if (subquery.isEmpty()) {
			handleEmptySubquery();
		} else if (subquery.isNegative()) {
			handleNegativeSubquery();
		}
	}

	private void handleNegativeSubquery() {
		this.isNegative = true;
		if (startOffset < 0) {
			int max = Math.abs(startOffset) + length;
			subquery.setMax(max);
			startOffset = max + startOffset;
		} else {
			int endOffset = startOffset + length;
			if (subquery.getMax() > endOffset) {
				subquery.setMax(startOffset + length);
			}
		}
		subquery.setMin(startOffset);
		subquery.isOptional = false;

		max = subquery.max - subquery.min;
		min = max;
	}

	private void handleEmptySubquery() {
		if (subquery instanceof SpanRepetitionQueryWrapper) {
			this.isEmpty = true;
		}
		// subspan([]{,5}, 2) -> subspan([]{2,5}, 2)
		// e.g. subspan([]{0,6}, 8)
		if (startOffset >= subquery.getMax()) {
			this.isNull = true;
			return;
		}
		if (startOffset < 0) {
			startOffset = subquery.getMax() + startOffset;
		}
		subquery.isOptional = false;
		subquery.setMin(startOffset);

		// subspan([]{2,}, 2,5) -> subspan([]{2,5}, 2,5)
		int endOffset = startOffset + length;
		if (length == 0) {
			length = subquery.getMax() - startOffset;
		}
		else if (subquery.getMax() > endOffset || subquery.getMax() == 0) {
			subquery.setMax(endOffset);
		}
		else if (subquery.getMax() < endOffset) {
			length = subquery.max - subquery.min;
		}

		setMax(subquery.max);
		setMin(subquery.min);
	}

	@Override
	public SpanQuery toQuery() throws QueryException {

		if (this.isNull()) {
			// if (DEBUG) log.warn("Subquery of SpanSubspanquery is null.");
			return null;
		}

		SpanQuery sq = subquery.toQuery();
		if (sq == null)
			return null;
		if (sq instanceof SpanTermQuery) {
			if (subquery.isNegative()) {
				return sq;
			}
			else if ((startOffset == 0 || startOffset == -1) &&
					(length == 1 || length == 0)) {
				// if (DEBUG) log.warn("Not SpanSubspanQuery. " +
				// "Creating only a SpanQuery for the subquery.");
				return sq;
			}
			return null;
		}

		return new SpanSubspanQuery(sq, startOffset, length, true);
	}

	@Override
	public boolean isNegative() {
		return this.subquery.isNegative();
	};

	@Override
	public boolean isOptional() {
		if (startOffset > 0)
			return false;
		return this.subquery.isOptional();
	};
}
