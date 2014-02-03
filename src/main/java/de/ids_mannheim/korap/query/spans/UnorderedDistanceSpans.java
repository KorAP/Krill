package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.SpanDistanceQuery;

public class UnorderedDistanceSpans extends SimpleSpans{

	public UnorderedDistanceSpans(SpanDistanceQuery query,
			AtomicReaderContext context, Bits acceptDocs,
			Map<Term, TermContext> termContexts) throws IOException {
		super(query, context, acceptDocs, termContexts);
	}

	@Override
	public boolean next() throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean skipTo(int target) throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public long cost() {
		// TODO Auto-generated method stub
		return 0;
	}

}
