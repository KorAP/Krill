package de.ids_mannheim.korap.query;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.spans.ExpandedExclusionSpans;
import de.ids_mannheim.korap.query.spans.ExpandedSpans;

/** Query to make a span longer by stretching out the start or the end 
 * 	position of the span. The constraints of the expansion, such as how 
 * 	large the expansion should be (min and max position) and the 
 * 	direction of the expansion with respect to the "main" span, are 
 * 	specified in ExpansionConstraint.
 * 
 * 	The expansion can be specified to not contain any direct/immediate
 * 	/adjacent occurrence(s) of another span. Examples:
 * 		[orth=der][orth!=Baum] 		"der" cannot be followed by "Baum" 
 * 		[pos!=ADJ]{1,2}[orth=Baum]	one or two adjectives cannot precedes 
 * 									"Baum"
 * 
 *  The offsets of the expansion parts can be collected by using a class 
 *  number. 
 * 
 * 	@author margaretha
 * */
public class SpanExpansionQuery extends SimpleSpanQuery{
	 
	private int min, max; // min, max expansion position
	
	// if > 0, collect expansion offsets using this label
	private byte classNumber;
	
	// expansion direction with regard to the main span: 
	// < 0 	to the left of main span 
	// >= 0  to the right of main span
	private int direction;	
	
	// if true, no occurrence of another span
	final boolean isExclusion; 
	
	/** Simple expansion for any/empty token. Use 
	 * 	{@link #SpanExpansionQuery(SpanQuery, SpanQuery, ExpansionConstraint, 
	 * 	boolean)} for expansion with exclusions of a specific spanquery. 
	 * */
	public SpanExpansionQuery(SpanQuery firstClause, int min, int max, int direction, 
			boolean collectPayloads) {
		super(firstClause, collectPayloads);
		this.min = min;
		this.max = max;
		this.direction = direction;
		this.isExclusion = false;
	}
	
	public SpanExpansionQuery(SpanQuery firstClause, int min, int max, int direction, 
			byte classNumber, boolean collectPayloads) {
		this(firstClause, min, max, direction, collectPayloads);		
		this.classNumber = classNumber;
	}
	
	/** Expansion with exclusions of the spanquery specified as the second 
	 * 	parameter.
	 * */
	public SpanExpansionQuery(SpanQuery firstClause, SpanQuery notClause, int min, 
			int max, int direction, boolean collectPayloads) {
		super(firstClause, notClause, collectPayloads);
		this.min = min;
		this.max = max;
		this.direction = direction;
		this.isExclusion = true;
	}
		
	
	@Override
	public SimpleSpanQuery clone() {
		SpanExpansionQuery sq = null;
		if (isExclusion){
			sq = new SpanExpansionQuery(firstClause, secondClause, min, max, 
					direction, collectPayloads);
		}
		else{
			sq = new SpanExpansionQuery(firstClause, min, max, direction, classNumber,
					collectPayloads);
		}
		//sq.setBoost(sq.getBoost());
		return sq;
	}

	@Override
	public Spans getSpans(AtomicReaderContext context, Bits acceptDocs,
			Map<Term, TermContext> termContexts) throws IOException {		 
		if (isExclusion)
			return new ExpandedExclusionSpans(this, context, acceptDocs, termContexts);		
		else
			return new ExpandedSpans(this, context, acceptDocs, termContexts);
	}

	@Override
	public String toString(String field) {
		StringBuilder sb = new StringBuilder();
		sb.append("spanExpansion(");
		sb.append(firstClause.toString());
		if (isExclusion && secondClause != null){
			sb.append(", !");
			sb.append(secondClause.toString());
		}
		else{
			sb.append(", []");
		}
		sb.append("{");
		sb.append(min);
		sb.append(", ");
		sb.append(max);
		sb.append("}, ");		
		if (direction < 0)
			sb.append("left");
		else sb.append("right");
		if (classNumber > 0){
			sb.append(", class:");
			sb.append(classNumber);			
		}
		sb.append(")");
		return sb.toString();
	}

	public int getMin() {
		return min;
	}

	public void setMin(int min) {
		this.min = min;
	}

	public int getMax() {
		return max;
	}

	public void setMax(int max) {
		this.max = max;
	}

	public byte getClassNumber() {
		return classNumber;
	}

	public void setClassNumber(byte classNumber) {
		this.classNumber = classNumber;
	}

	public int getDirection() {
		return direction;
	}

	public void setDirection(int direction) {
		this.direction = direction;
	}
}
