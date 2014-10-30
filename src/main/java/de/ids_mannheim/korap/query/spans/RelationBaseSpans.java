package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.SpanWithIdQuery;

public abstract class RelationBaseSpans extends SpansWithId{

	protected short leftId, rightId;
	protected int rightStart, rightEnd;
	
	public RelationBaseSpans(SpanWithIdQuery spanWithIdQuery,
			AtomicReaderContext context, Bits acceptDocs,
			Map<Term, TermContext> termContexts) throws IOException {
		super(spanWithIdQuery, context, acceptDocs, termContexts);
		// TODO Auto-generated constructor stub
	}
	
	public short getLeftId() {
		return leftId;
	}

	public void setLeftId(short leftId) {
		this.leftId = leftId;
	}

	public short getRightId() {
		return rightId;
	}

	public void setRightId(short rightId) {
		this.rightId = rightId;
	}

	public int getRightStart() {
		return rightStart;
	}

	public void setRightStart(int rightStart) {
		this.rightStart = rightStart;
	}

	public int getRightEnd() {
		return rightEnd;
	}

	public void setRightEnd(int rightEnd) {
		this.rightEnd = rightEnd;
	}
}
