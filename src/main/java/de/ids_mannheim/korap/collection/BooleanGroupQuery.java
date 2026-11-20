package de.ids_mannheim.korap.collection;

import java.io.IOException;
import java.util.*;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.ConstantScoreScorer;
import org.apache.lucene.search.ConstantScoreWeight;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;
import org.apache.lucene.util.BitSetIterator;
import org.apache.lucene.util.FixedBitSet;

import de.ids_mannheim.korap.KrillCollection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A container query that allows Boolean composition of queries
 * in groups (either or-groups or and-groups). It matches documents
 * only, without scores.
 *
 * @author Nils Diewald
 *
 *         This query is roughly based on
 *         org.apache.lucene.queries.BooleanFilter.
 */
public class BooleanGroupQuery extends Query {
    // Group is either an or- or an and-Group
    private boolean isOptional;

    // Logger
    private final static Logger log = LoggerFactory
            .getLogger(KrillCollection.class);

    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;

    // Init operands list
    private final List<GroupQueryOperand> operands = new ArrayList<>(3);

    // Operand in the query group
    private static class GroupQueryOperand {
        public Query query;
        public boolean isNegative;


        // Operand has query and negativity information
        public GroupQueryOperand (Query query, boolean negative) {
            this.query = query;
            this.isNegative = negative;
        };


        @Override
        public boolean equals (Object obj) {
            if (!(obj instanceof GroupQueryOperand))
                return false;
            GroupQueryOperand other = (GroupQueryOperand) obj;
            return this.isNegative == other.isNegative
                    && this.query.equals(other.query);
        };


        @Override
        public int hashCode () {
            return this.query.hashCode() ^ (this.isNegative ? 1 : 0);
        };
    };


    /**
     * Create a new BooleanGroupQuery.
     * Accepts a boolean parameter to make it an or-Group
     * (<pre>true</pre>) or an and-Group (<pre>false</pre>).
     */
    public BooleanGroupQuery (boolean optional) {
        this.isOptional = optional;
    };


    /**
     * Add an operand to the list of query operands.
     * The operand is a positive query that won't be flipped.
     */
    public final void with (Query query) {
        this.operands.add(new GroupQueryOperand(query, false));
    };


    /**
     * Add an operand to the list of query operands.
     * The operand is a negative query that will be flipped.
     */
    public final void without (Query query) {
        this.operands.add(new GroupQueryOperand(query, true));
    };


    @Override
    public boolean equals (Object obj) {
        if (!sameClassAs(obj))
            return false;

        final BooleanGroupQuery other = (BooleanGroupQuery) obj;
        return this.isOptional == other.isOptional
                && operands.equals(other.operands);
    };


    @Override
    public int hashCode () {
        return 31 * classHash()
                + (657153719 ^ operands.hashCode() ^ (isOptional ? 1 : 0));
    };


    @Override
    public String toString (String field) {
        StringBuilder buffer = new StringBuilder(
                this.isOptional ? "OrGroup(" : "AndGroup(");
        boolean first = true;
        for (final GroupQueryOperand operand : this.operands) {
            if (first)
                first = false;
            else
                buffer.append(" ");

            if (operand.isNegative)
                buffer.append('-');

            buffer.append(operand.query.toString());
        };
        return buffer.append(')').toString();
    };


    @Override
    public Weight createWeight (IndexSearcher searcher, boolean needsScores,
            float boost) throws IOException {

        // The weights of the operands are created once for all leaves
        final Weight[] operandWeights = new Weight[this.operands.size()];
        for (int i = 0; i < operandWeights.length; i++) {
            operandWeights[i] = searcher.createWeight(
                    searcher.rewrite(this.operands.get(i).query), false, 1f);
        };

        return new ConstantScoreWeight(this, boost) {
            // Not cached by Lucene's query cache
            @Override
            public boolean isCacheable (LeafReaderContext context) {
                return false;
            };


            @Override
            public Scorer scorer (LeafReaderContext atomic)
                    throws IOException {
                DocIdSetIterator docs = BooleanGroupQuery.this.docs(atomic,
                        operandWeights);
                if (docs == null)
                    return null;
                return new ConstantScoreScorer(this, score(), docs);
            };
        };
    };


    // The documents of the group in a leaf, or null if there are none
    private DocIdSetIterator docs (LeafReaderContext atomic,
            Weight[] operandWeights) throws IOException {
        int maxDoc = atomic.reader().maxDoc();
        FixedBitSet bitset = new FixedBitSet(maxDoc);
        FixedBitSet combinator = new FixedBitSet(maxDoc);
        boolean init = true;

        if (DEBUG) {
            log.debug("Filter on group {} in a corpus of {} docs", this.toString(), maxDoc);
        }

        for (int i = 0; i < operandWeights.length; i++) {
            GroupQueryOperand operand = this.operands.get(i);
            DocIdSetIterator filterIter = QueryDocs
                    .iterator(operandWeights[i], atomic);

            if (DEBUG)
                log.debug("> Filter to bitset of {} ({} negative)",
                          operand.query.toString(), operand.isNegative);


            // Filter resulted in no docs
            if (filterIter == null) {

                if (DEBUG)
                    log.debug("- Filter is null");

                // Filter matches everywhere
                if (operand.isNegative) {

                    // OR - This means, everything is allowed
                    if (this.isOptional) {
                        if (DEBUG) {
                            log.debug("- Filter to allow all documents (OR NEG NULL)");
                        }

                        // bitset.set(1, maxDoc);
                        bitset.set(0, maxDoc);

                        // Match all documents!
                        return new BitSetIterator(bitset, maxDoc);
                    }


                    // AND - The negation is irrelevant
                    if (init) {
                        if (DEBUG)
                            log.debug("- Initialize by setting to all");

                        bitset.set(0, maxDoc);
                    }

                    else if (DEBUG) {
                        log.debug("- Filter by ignoring this operand (AND NEG NULL)");
                    };
                }

                // AND with NULL
                // There is no possible match
                else if (!this.isOptional) {

                    if (DEBUG)
                        log.debug("- Filter to allow no documents (2)");
                    return null;
                }

                // OR WITH 0 - ignore!
                else if (DEBUG) {
                    log.debug("- Filter is ignorable");
                }

                if (DEBUG)
                    log.debug("- Check next operand");

            }

            // Initialize bitset
            else if (init) {

                // Set initially empty bitset to filterIter
                bitset.or(filterIter);

                if (DEBUG)
                    log.debug("- Filter is inital with card {}",
                              bitset.cardinality());

                // Flip the matching documents
                if (operand.isNegative) {
                    bitset.flip(0, maxDoc);
                    if (DEBUG)
                        log.debug(
                            "- Filter is negative - so flipped to card {} (1)",
                            bitset.cardinality());
                };
            }

            // Operate on bitsets
            else {

                if (DEBUG)
                    log.debug("- Filter is fine and operating");

                // Operator is negative and needs to be flipped
                if (operand.isNegative) {
                    if (this.isOptional) {
                        if (DEBUG)
                            log.debug("- Filter is negative optional");

                        // Negative or ... may be slow
                        combinator.or(filterIter);
                        combinator.flip(0, maxDoc);

                        if (DEBUG)
                            log.debug(
                                    "- Filter is negative - so flipped to card {} (2)",
                                    combinator.cardinality());

                        bitset.or(combinator);
                        combinator.clear(0, maxDoc);
                    }

                    // Negative and
                    else {
                        if (DEBUG)
                            log.debug("- Filter is negative not optional");
                        combinator.or(filterIter);
                        bitset.andNot(combinator);
                        combinator.clear(0, maxDoc);
                        if (DEBUG)
                            log.debug("- Filter is negative - so andNotted");
                    }
                }
                else if (this.isOptional) {
                    if (DEBUG)
                        log.debug("- Filter is simply optional");
                    bitset.or(filterIter);
                }
                else {
                    if (DEBUG)
                        log.debug("- Filter is simply not optional");
                    combinator.or(filterIter);
                    bitset.and(combinator);
                    combinator.clear(0, maxDoc);
                    // TODO: Check with nextSetBit() if the filter is not applicable
                };

                if (DEBUG)
                    log.debug("- Subresult has card {} ", bitset.cardinality());
            };

            if (DEBUG)
                log.debug("- Init is over");

            init = false;
        };

        if (DEBUG)
            log.debug("- Operand list ends");

        return new BitSetIterator(bitset, bitset.cardinality());
    };
};
