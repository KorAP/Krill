package de.ids_mannheim.korap.query;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.search.spans.TermSpans;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.spans.ExpandedExclusionSpans;
import de.ids_mannheim.korap.query.spans.ExpandedSpans;

/**
 * SpanExpansionQuery makes a span longer by stretching out the start
 * or the end
 * position of the span. The constraints of the expansion, such as how
 * large the
 * expansion should be (min and max position) and the direction of the
 * expansion
 * with respect to the original span, are specified in
 * ExpansionConstraint. The
 * direction is designated with the sign of a number, namely a
 * negative number
 * signifies the left direction, and a positive number (including 0)
 * signifies
 * the right direction.
 * 
 * <pre>
 * SpanTermQuery stq = new SpanTermQuery(new Term(&quot;tokens&quot;,
 * &quot;s:lightning&quot;));
 * SpanExpansionQuery seq = new SpanExpansionQuery(stq, 0, 2, -1,
 * true);
 * </pre>
 * 
 * In the example above, the SpanExpansionQuery describes that the
 * {@link TermSpans} of "lightning" may be expanded up to two token
 * positions to
 * the left.
 * 
 * <pre>
 * &quot;Trees are often struck by lightning because they are natural
 * lightning conductors to the ground.&quot;
 * </pre>
 * 
 * The matches for the sample text are:
 * 
 * <pre>
 * [struck by lightning]
 * [by lightning]
 * [lightning]
 * [are natural lightning]
 * [natural lightning]
 * [lightning]
 * </pre>
 * 
 * The expansion can also be specified to <em>not</em> contain any
 * direct/immediate /adjacent occurrence(s) of another span. Examples
 * in
 * Poliqarp:
 * 
 * <pre>
 * [orth=the][orth!=lightning] "the" must not be followed by
 * "lightning"
 * [pos!=ADJ]{1,2}[orth=jacket] one or two adjectives cannot precedes
 * "jacket"
 * </pre>
 * 
 * The SpanExpansionQuery for the latter Poliqarp query with left
 * direction from
 * "jacket" example is:
 * 
 * <pre>
 * SpanTermQuery notQuery = new SpanTermQuery(new
 * Term(&quot;tokens&quot;, &quot;tt:p:/ADJ&quot;));
 * SpanTermQuery stq = new SpanTermQuery(new Term(&quot;tokens&quot;,
 * &quot;s:jacket&quot;));
 * SpanExpansionQuery seq = new SpanExpansionQuery(stq, notQuery, 1,
 * 2, -1, true);
 * </pre>
 * 
 * Matches and non matches example:
 * 
 * <pre>
 * [a jacket] match
 * [such a jacket] non match, where such is an ADJ
 * [leather jacket] non match
 * [black leather jacket] non match
 * [large black leather jacket] non match
 * </pre>
 * 
 * The positions of the expansion parts can be optionally stored in
 * payloads
 * together with a class number.
 * 
 * @author margaretha
 */
public class SpanExpansionQuery extends SimpleSpanQuery {

    private int min, max; // min, max expansion position

    // if > 0, collect expansion offsets using this label
    private byte classNumber;

    // expansion direction with regard to the main span: 
    // < 0 	to the left of main span 
    // >= 0  to the right of main span
    private int direction;

    // if true, no occurrence of another span
    final boolean isExclusion;


    /**
     * Constructs a SpanExpansionQuery for simple expansion of the
     * specified {@link SpanQuery}.
     * 
     * @param firstClause
     *            a {@link SpanQuery}
     * @param min
     *            the minimum length of the expansion
     * @param max
     *            the maximum length of the expansion
     * @param direction
     *            the direction of the expansion
     * @param collectPayloads
     *            a boolean flag representing the value
     *            <code>true</code> if payloads are to be collected,
     *            otherwise
     *            <code>false</code>.
     */
    public SpanExpansionQuery (SpanQuery firstClause, int min, int max,
                               int direction, boolean collectPayloads) {
        super(firstClause, collectPayloads);
        if (max < min) {
            throw new IllegalArgumentException("The max position has to be "
                    + "bigger than or the same as min position.");
        }
        this.min = min;
        this.max = max;
        this.direction = direction;
        this.isExclusion = false;
    }


    /**
     * Constructs a SpanExpansionQuery for simple expansion of the
     * specified {@link SpanQuery} and stores expansion offsets in
     * payloads associated
     * with the given class number.
     * 
     * @param firstClause
     *            a {@link SpanQuery}
     * @param min
     *            the minimum length of the expansion
     * @param max
     *            the maximum length of the expansion
     * @param direction
     *            the direction of the expansion
     * @param classNumber
     *            the class number for storing expansion offsets in
     *            payloads
     * @param collectPayloads
     *            a boolean flag representing the value
     *            <code>true</code> if payloads are to be collected,
     *            otherwise
     *            <code>false</code>.
     */
    public SpanExpansionQuery (SpanQuery firstClause, int min, int max,
                               int direction, byte classNumber,
                               boolean collectPayloads) {
        this(firstClause, min, max, direction, collectPayloads);
        this.classNumber = classNumber;
    }


    /**
     * Constructs a SpanExpansionQuery for expansion of the first
     * {@link SpanQuery} with exclusions of the second
     * {@link SpanQuery}.
     * 
     * @param firstClause
     *            the SpanQuery to be expanded
     * @param notClause
     *            the SpanQuery to be excluded
     * @param min
     *            the minimum length of the expansion
     * @param max
     *            the maximum length of the expansion
     * @param direction
     *            the direction of the expansion
     * @param collectPayloads
     *            a boolean flag representing the value
     *            <code>true</code> if payloads are to be collected,
     *            otherwise
     *            <code>false</code>.
     */
    public SpanExpansionQuery (SpanQuery firstClause, SpanQuery notClause,
                               int min, int max, int direction,
                               boolean collectPayloads) {
        super(firstClause, notClause, collectPayloads);
        if (max < min) {
            throw new IllegalArgumentException("The max position has to be "
                    + "bigger than or the same as min position.");
        }
        this.min = min;
        this.max = max;
        this.direction = direction;
        this.isExclusion = true;
    }


    /**
     * Constructs a SpanExpansionQuery for expansion of the first
     * {@link SpanQuery} with exclusions of the second
     * {@link SpanQuery}, and
     * stores expansion offsets in payloads associated with the given
     * class
     * number.
     * 
     * @param firstClause
     *            the SpanQuery to be expanded
     * @param notClause
     *            the SpanQuery to be excluded
     * @param min
     *            the minimum length of the expansion
     * @param max
     *            the maximum length of the expansion
     * @param direction
     *            the direction of the expansion
     * @param classNumber
     *            the class number for storing expansion offsets in
     *            payloads
     * @param collectPayloads
     *            a boolean flag representing the value
     *            <code>true</code> if payloads are to be collected,
     *            otherwise
     *            <code>false</code>.
     */
    public SpanExpansionQuery (SpanQuery firstClause, SpanQuery notClause,
                               int min, int max, int direction,
                               byte classNumber, boolean collectPayloads) {
        this(firstClause, notClause, min, max, direction, collectPayloads);
        this.classNumber = classNumber;
    }


    @Override
    public SimpleSpanQuery clone () {
        SpanExpansionQuery sq = null;
        if (isExclusion) {
            sq = new SpanExpansionQuery(firstClause, secondClause, min, max,
                    direction, classNumber, collectPayloads);
        }
        else {
            sq = new SpanExpansionQuery(firstClause, min, max, direction,
                    classNumber, collectPayloads);
        }
        //sq.setBoost(sq.getBoost());
        return sq;
    }


    @Override
    public Spans getSpans (LeafReaderContext context, Bits acceptDocs,
            Map<Term, TermContext> termContexts) throws IOException {

        //	      Temporary:
        if (isExclusion)
            return new ExpandedExclusionSpans(this, context, acceptDocs,
                    termContexts);
        else

            return new ExpandedSpans(this, context, acceptDocs, termContexts);
    }


    @Override
    public String toString (String field) {
        StringBuilder sb = new StringBuilder();
        sb.append("spanExpansion(");
        sb.append(firstClause.toString());
        if (isExclusion && secondClause != null) {
            sb.append(", !");
            sb.append(secondClause.toString());
        }
        else {
            sb.append(", []");
        }
        sb.append("{");
        sb.append(min);
        sb.append(", ");
        sb.append(max);
        sb.append("}, ");
        if (direction < 0)
            sb.append("left");
        else
            sb.append("right");
        if (classNumber > 0) {
            sb.append(", class:");
            sb.append(classNumber);
        }
        sb.append(")");
        return sb.toString();
    }


    /**
     * Returns the minimum length of the expansion.
     * 
     * @return the minimum length of the expansion
     */
    public int getMin () {
        return min;
    }


    /**
     * Sets the minimum length of the expansion.
     * 
     * @param min
     *            the minimum length of the expansion
     */
    public void setMin (int min) {
        this.min = min;
    }


    /**
     * Returns the maximum length of the expansion.
     * 
     * @return the maximum length of the expansion
     */
    public int getMax () {
        return max;
    }


    /**
     * Sets the maximum length of the expansion.
     * 
     * @param max
     *            the maximum length of the expansion
     */
    public void setMax (int max) {
        this.max = max;
    }


    /**
     * Returns the class number associated with the expansion offsets
     * 
     * @return the class number associated with the expansion offsets
     */
    public byte getClassNumber () {
        return classNumber;
    }


    /**
     * Sets the class number associated with the expansion offsets
     * 
     * @param classNumber
     *            the class number associated with the expansion
     *            offsets
     */
    public void setClassNumber (byte classNumber) {
        this.classNumber = classNumber;
    }


    /**
     * Returns the direction of the expansion
     * 
     * @return the direction of the expansion
     */
    public int getDirection () {
        return direction;
    }


    /**
     * Sets the direction of the expansion
     * 
     * @param direction
     *            the direction of the expansion
     */
    public void setDirection (int direction) {
        this.direction = direction;
    }
}
