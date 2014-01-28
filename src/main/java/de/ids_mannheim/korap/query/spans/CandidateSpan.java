package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.Collection;

import org.apache.lucene.search.spans.Spans;

/** A span kept as a candidate for matching with another Span
 * 	@author margaretha
 * */
public class CandidateSpan {	
	private int doc,start,end;
	private long cost;
	private Collection<byte[]> payloads;
	private int position;
	
	public CandidateSpan(Spans span) throws IOException {
		this.doc = span.doc();
		this.start = span.start();
		this.end = span.end();
		this.cost = span.cost();
		
		if (span.isPayloadAvailable()){
			this.payloads = span.getPayload();
		}
		else{ 
			this.payloads = null;
		}
	}	
	
	public CandidateSpan(Spans span, int position) throws IOException {
		this(span);
		this.position = position;		
	}
	
	public int getDoc() {
		return doc;
	}
	public void setDoc(int doc) {
		this.doc = doc;
	}
	public int getStart() {
		return start;
	}
	public void setStart(int start) {
		this.start = start;
	}
	public int getEnd() {
		return end;
	}
	public void setEnd(int end) {
		this.end = end;
	}

	public Collection<byte[]> getPayloads() {
		return payloads;
	}

	public void setPayloads(Collection<byte[]> payloads) {
		this.payloads = payloads;
	}

	public long getCost() {
		return cost;
	}

	public void setCost(long cost) {
		this.cost = cost;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}
	
	
}
