package de.ids_mannheim.korap.query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.ToStringUtils;

import de.ids_mannheim.korap.query.spans.FocusSpans;

/**
 * Modify the span of a match to the boundaries of a certain class.
 * 
 * In case multiple classes are found with the very same number, the
 * span is maximized to start on the first occurrence from the left
 * and end on the last occurrence on the right.
 * 
 * In case the class to modify on is not found in the subquery, the
 * match is ignored.
 * 
 * @author diewald, margaretha
 * 
 * @see FocusSpans
 */
public class SpanFocusQuery extends SimpleSpanQuery {

    private List<Byte> classNumbers = new ArrayList<Byte>();
    private boolean isSorted = true;
    private boolean matchTemporaryClass = false;
    private boolean removeTemporaryClasses = false;


    /**
     * Construct a new SpanFocusQuery.
     * 
     * @param firstClause
     *            The nested {@link SpanQuery}, that contains one or
     *            more
     *            classed spans.
     * @param number
     *            The class number to focus on.
     */
    public SpanFocusQuery (SpanQuery sq, byte classNumber) {
        super(sq, true);
        classNumbers.add(classNumber);
    };


    public SpanFocusQuery (SpanQuery sq, List<Byte> classNumbers) {
        super(sq, true);
        this.classNumbers = classNumbers;
    };


    /**
     * Construct a new SpanFocusQuery. The class to focus on defaults
     * to
     * <tt>1</tt>.
     * 
     * @param firstClause
     *            The nested {@link SpanQuery}, that contains one or
     *            more
     *            classed spans.
     */
    public SpanFocusQuery (SpanQuery sq) {
        super(sq, true);
        classNumbers.add((byte) 1);
    };


    @Override
    public String toString (String field) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("focus(");
        if (matchTemporaryClass) {
            buffer.append("#");
        }
        if (classNumbers.size() > 1) {
            buffer.append("[");
            for (int i = 0; i < classNumbers.size(); i++) {
                buffer.append((short) classNumbers.get(i) & 0xFF);
                if (i != classNumbers.size() - 1) {
                    buffer.append(",");
                }
            }
            buffer.append("]");
        }
        else {
            buffer.append((short) classNumbers.get(0) & 0xFF).append(": ");
        }
        buffer.append(this.firstClause.toString());
        buffer.append(')');
        buffer.append(ToStringUtils.boost(getBoost()));
        return buffer.toString();
    };


    @Override
    public Spans getSpans (final LeafReaderContext context, Bits acceptDocs,
            Map<Term, TermContext> termContexts) throws IOException {
        return new FocusSpans(this, context, acceptDocs, termContexts);
    };


    @Override
    public Query rewrite (IndexReader reader) throws IOException {
        SpanFocusQuery clone = null;
        SpanQuery query = (SpanQuery) this.firstClause.rewrite(reader);

        if (query != this.firstClause) {
            if (clone == null)
                clone = this.clone();
            clone.firstClause = query;
        };

        if (clone != null)
            return clone;

        return this;
    };


    @Override
    public SpanFocusQuery clone () {
        SpanFocusQuery spanFocusQuery = new SpanFocusQuery(
                (SpanQuery) this.firstClause.clone(), this.getClassNumbers());
        spanFocusQuery.setBoost(getBoost());
        spanFocusQuery.setMatchTemporaryClass(this.matchTemporaryClass);
        spanFocusQuery.setSorted(this.isSorted);
        spanFocusQuery.setRemoveTemporaryClasses(this.removeTemporaryClasses);
        return spanFocusQuery;
    };


    @Override
    public boolean equals (Object o) {
        if (this == o)
            return true;
        if (!(o instanceof SpanFocusQuery))
            return false;

        final SpanFocusQuery spanFocusQuery = (SpanFocusQuery) o;

        if (!this.firstClause.equals(spanFocusQuery.firstClause))
            return false;
        if (this.getClassNumbers() != spanFocusQuery.getClassNumbers())
            return false;

        // Probably not necessary
        return getBoost() == spanFocusQuery.getBoost();
    };


    @Override
    public int hashCode () {
        int result = firstClause.hashCode();
        for (byte number : classNumbers)
            result = 31 * result + number;
        result += Float.floatToRawIntBits(getBoost());
        return result;
    }


    public List<Byte> getClassNumbers () {
        return classNumbers;
    }


    public void setClassNumbers (List<Byte> classNumbers) {
        this.classNumbers = classNumbers;
    }


    public boolean isSorted () {
        return isSorted;
    }


    public void setSorted (boolean isSorted) {
        this.isSorted = isSorted;
    }


    public boolean matchTemporaryClass () {
        return matchTemporaryClass;
    }


    public void setMatchTemporaryClass (boolean matchTemporaryClass) {
        this.matchTemporaryClass = matchTemporaryClass;
    }


    public boolean removeTemporaryClasses () {
        return removeTemporaryClasses;
    }


    public void setRemoveTemporaryClasses (boolean rem) {
        this.removeTemporaryClasses = rem;
    }

};
