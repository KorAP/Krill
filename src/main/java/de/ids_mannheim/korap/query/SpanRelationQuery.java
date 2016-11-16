package de.ids_mannheim.korap.query;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.ToStringUtils;

import de.ids_mannheim.korap.query.spans.RelationSpans;

/**
 * SpanRelationQuery retrieves spans representing a relation between
 * tokens, elements, or a-token-and-an-element. Relation are marked
 * with prefix "<" or ">". The direction of the angle bracket
 * represents the direction of the corresponding relation. By default,
 * the relation is set ">".
 * <br/><br/>
 * 
 * This class provides two types of query:
 * <ol>
 * <li>querying any relations, for instance dependency relation
 * "<:xip/syntax-dep_rel".
 * 
 * <pre>SpanRelationQuery sq = new SpanRelationQuery(
 * new SpanTermQuery(
 * new Term("tokens","<:xip/syntax-dep_rel")),
 * true);
 * </pre>
 * </li>
 * <li>querying relations matching a certain type of sources/targets,
 * that are the left or the right sides of the relations. This query
 * is used within {@link SpanRelationPartQuery}, for instance, to
 * retrieve all dependency relations "<:xip/syntax-dep_rel" whose
 * sources (right side) are noun phrases.
 * <pre>
 * SpanRelationPartQuery rv =
 * new SpanRelationPartQuery(sq, new SpanElementQuery("tokens","np"),
 * true,
 * false, true);
 * </pre>
 * </li>
 * 
 * </ol>
 * 
 * @author margaretha
 */
public class SpanRelationQuery extends SimpleSpanQuery {

    private int direction = 0; // >
    private byte tempSourceNum = 1;
    private byte tempTargetNum = 2;
    private byte sourceClass;
    private byte targetClass;

    private List<Byte> tempClassNumbers = Arrays.asList(tempSourceNum,
            tempTargetNum);


    /**
     * Constructs a SpanRelationQuery based on the given span query.
     * 
     * @param firstClause
     *            a SpanQuery.
     * @param collectPayloads
     *            a boolean flag representing the value
     *            <code>true</code> if
     *            payloads are to be collected, otherwise
     *            <code>false</code>.
     */
    public SpanRelationQuery (SpanQuery firstClause, boolean collectPayloads) {
        super(firstClause, collectPayloads);
        SpanTermQuery st = (SpanTermQuery) firstClause;
        String direction = st.getTerm().text().substring(0, 1);
        if (direction.equals("<")) {
            this.direction = 1;
        }
    }


    public SpanRelationQuery (SpanQuery firstClause, List<Byte> classNumbers,
                              boolean collectPayloads) {
        this(firstClause, collectPayloads);
        this.tempClassNumbers = classNumbers;
        this.tempSourceNum = classNumbers.get(0);
        this.tempTargetNum = classNumbers.get(1);
    }


    @Override
    public SimpleSpanQuery clone () {
        SimpleSpanQuery sq = new SpanRelationQuery(
                (SpanQuery) this.firstClause.clone(), this.collectPayloads);
        return sq;
    }


    @Override
    public Spans getSpans (LeafReaderContext context, Bits acceptDocs,
            Map<Term, TermContext> termContexts) throws IOException {
        return new RelationSpans(this, context, acceptDocs, termContexts);
    }


    @Override
    public String toString (String field) {
        StringBuilder sb = new StringBuilder();
        if (sourceClass > 0) {
            sb.append("{");
            sb.append(sourceClass);
            sb.append(": source:");
        }
        if (targetClass > 0) {
            sb.append("{");
            sb.append(targetClass);
            sb.append(": target:");
        }
        sb.append("spanRelation(");
        sb.append(firstClause.toString(field));
        sb.append(")");
        if (sourceClass > 0) {
            sb.append("}");
        }
        if (targetClass > 0) {
            sb.append("}");
        }
        sb.append(ToStringUtils.boost(getBoost()));
        return sb.toString();
    }


    public int getDirection () {
        return direction;
    }


    public void setDirection (int direction) {
        this.direction = direction;
    }


    public List<Byte> getTempClassNumbers () {
        return tempClassNumbers;
    }


    public void setTempClassNumbers (List<Byte> classNumbers) {
        this.tempClassNumbers = classNumbers;
    }


    public byte getTempSourceNum () {
        return tempSourceNum;
    }


    public void setTempSourceNum (byte sourceNum) {
        this.tempSourceNum = sourceNum;
    }


    public byte getTempTargetNum () {
        return tempTargetNum;
    }


    public void setTempTargetNum (byte targetNum) {
        this.tempTargetNum = targetNum;
    }


    public byte getSourceClass () {
        return sourceClass;
    }


    public void setSourceClass (byte sourceClass)
            throws IllegalArgumentException {
        if (sourceClass < 1) {
            throw new IllegalArgumentException(
                    "Class number must be bigger than 0.");
        }

        this.sourceClass = sourceClass;
    }


    public byte getTargetClass () {
        return targetClass;
    }


    public void setTargetClass (byte targetClass)
            throws IllegalArgumentException {
        if (targetClass < 1) {
            throw new IllegalArgumentException(
                    "Class number must be bigger than 0.");
        }
        this.targetClass = targetClass;
    }
}
