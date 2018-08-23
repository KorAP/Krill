package de.ids_mannheim.korap.query;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.ToStringUtils;

import de.ids_mannheim.korap.query.spans.RepetitionSpans;

/**
 * SpanRepetitionQuery means that the given SpanQuery must appears
 * multiple times in a sequence. The number of repetition depends on
 * the minimum and the maximum number parameters. <br />
 * <br />
 * 
 * In the example below, SpanRepetitionQuery retrieves
 * {@link RepetitionSpans} consisting of the TermSpans "tt:p/ADJ" that
 * must appear at least once or consecutively two times. What appears
 * after the RepetitionSpans is not considered, so it is possible that
 * it is another "tt:p/ADJ". <br />
 * <br />
 * 
 * <pre>
 * SpanRepetitionQuery sq = new SpanRepetitionQuery(new
 * SpanTermQuery(new Term(
 * &quot;tokens&quot;, &quot;tt:p/ADJ&quot;)), 1, 2, true);
 * </pre>
 * 
 * For instance, "a large black leather jacket" contains the following
 * matches.
 * 
 * <pre>
 * [large]
 * [large black]
 * [black]
 * [black leather]
 * [leather]
 * </pre>
 * 
 * @author margaretha
 */
public class SpanRepetitionQuery extends SimpleSpanQuery {

    private int min, max;


    /**
     * Constructs a SpanRepetitionQuery for the given
     * {@link SpanQuery}.
     * 
     * @param sq
     *            a SpanQuery
     * @param min
     *            the minimum number of the required repetition
     * @param max
     *            the maximum number of the required repetition
     * @param collectPayloads
     *            a boolean flag representing the value
     *            <code>true</code> if payloads are to be collected,
     *            otherwise
     *            <code>false</code>.
     */
    public SpanRepetitionQuery (SpanQuery sq, int min, int max,
                                boolean collectPayloads) {
        super(sq, collectPayloads);
        if (min < 1) {
            throw new IllegalArgumentException(
                    "Minimum repetition must not lower than 1.");
        }
        this.min = min;
        this.max = max;
    }


    @Override
    public SimpleSpanQuery clone () {
        SpanRepetitionQuery sq = new SpanRepetitionQuery(
                (SpanQuery) this.firstClause.clone(), this.min, this.max,
                this.collectPayloads);
        sq.setBoost(getBoost());
        return sq;
    }


    @Override
    public Spans getSpans (LeafReaderContext context, Bits acceptDocs,
            Map<Term, TermContext> termContexts) throws IOException {
        return new RepetitionSpans(this, context, acceptDocs, termContexts);
    }


    @Override
    public String toString (String field) {
        StringBuilder sb = new StringBuilder();
        sb.append("spanRepetition(");
        sb.append(firstClause.toString(field));
        sb.append("{");
        sb.append(min);
        sb.append(",");
        sb.append(max);
        sb.append("})");
        sb.append(ToStringUtils.boost(getBoost()));
        return sb.toString();
    }


    /**
     * Returns the minimum number of required repetitions.
     * 
     * @return the minimum number of required repetitions
     */
    public int getMin () {
        return min;
    }


    /**
     * Sets the minimum number of required repetitions.
     * 
     * @param min
     *            the minimum number of required repetitions
     */
    public void setMin (int min) {
        this.min = min;
    }


    /**
     * Returns the maximum number of required repetitions.
     * 
     * @return the maximum number of required repetitions
     */
    public int getMax () {
        return max;
    }


    /**
     * Sets the maximum number of required repetitions.
     * 
     * @param max
     *            the maximum number of required repetitions
     */
    public void setMax (int max) {
        this.max = max;
    }

}
