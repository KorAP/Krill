package de.ids_mannheim.korap.collection;

import java.io.IOException;
import java.util.*;

import org.apache.lucene.search.Filter;
import org.apache.lucene.util.FixedBitSet;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.BitsFilteredDocIdSet;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.KrillCollection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A container Filter that allows Boolean composition of Filters
 * in groups (either or-groups or and-groups).
 * 
 * @author Nils Diewald
 * 
 *         This filter is roughly based on
 *         org.apache.lucene.queries.BooleanFilter.
 */
public class BooleanGroupFilter extends Filter {
    // Group is either an or- or an and-Group
    private boolean isOptional;

    // Logger
    private final static Logger log = LoggerFactory
            .getLogger(KrillCollection.class);

    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;

    // Init operands list
    private final List<GroupFilterOperand> operands = new ArrayList<>(3);

    // Operand in the filter group
    private class GroupFilterOperand {
        public Filter filter;
        public boolean isNegative;


        // Operand has filter and negativity information
        public GroupFilterOperand (Filter filter, boolean negative) {
            this.filter = filter;
            this.isNegative = negative;
        };
    };


    /**
     * Create a new BooleanGroupFilter.
     * Accepts a boolean parameter to make it an or-Group
     * (<pre>true</pre>) or an and-Group (<pre>true</pre>).
     */
    public BooleanGroupFilter (boolean optional) {
        this.isOptional = optional;
    };


    /**
     * Add an operand to the list of filter operands.
     * The operand is a positive filter that won't be flipped.
     */
    public final void with (Filter filter) {
        this.operands.add(new GroupFilterOperand(filter, false));
    };


    /**
     * Add an operand to the list of filter operands.
     * The operand is a negative filter that will be flipped.
     */
    public final void without (Filter filter) {
        this.operands.add(new GroupFilterOperand(filter, true));
    };


    @Override
    public boolean equals (Object obj) {
        if (this == obj)
            return true;

        if ((obj == null) || (obj.getClass() != this.getClass()))
            return false;

        final BooleanGroupFilter other = (BooleanGroupFilter) obj;
        return operands.equals(other.operands);
    };


    @Override
    public int hashCode () {
        return 657153719 ^ operands.hashCode();
    };


    @Override
    public String toString () {
        StringBuilder buffer = new StringBuilder(this.isOptional ? "OrGroup("
                : "AndGroup(");
        boolean first = true;
        for (final GroupFilterOperand operand : this.operands) {
            if (first)
                first = false;
            else
                buffer.append(" ");

            if (operand.isNegative)
                buffer.append('-');

            buffer.append(operand.filter.toString());
        };
        return buffer.append(')').toString();
    };

    /*
    @Override
    public String toString (String str) {
        return this.toString();
    };
    */


    @Override
    public DocIdSet getDocIdSet (AtomicReaderContext context, Bits acceptDocs)
            throws IOException {
        final AtomicReader reader = context.reader();
        int maxDoc = reader.maxDoc();
        FixedBitSet bitset = new FixedBitSet(maxDoc);
        FixedBitSet combinator = new FixedBitSet(maxDoc);
        boolean init = true;

        if (DEBUG)
            log.debug("Start trying to filter on bitset of length {}", maxDoc);

        for (final GroupFilterOperand operand : this.operands) {
            final DocIdSet docids = operand.filter.getDocIdSet(context, null);
            final DocIdSetIterator filterIter = (docids == null) ? null
                    : docids.iterator();

            if (DEBUG)
                log.debug("> Filter to bitset of {} ({} negative)",
                        operand.filter.toString(), operand.isNegative);

            // Filter resulted in no docs
            if (filterIter == null) {

                if (DEBUG)
                    log.debug("- Filter is null");

                // Filter matches
                if (operand.isNegative) {

                    // This means, everything is allowed
                    if (this.isOptional) {

                        // Everything is allowed
                        if (DEBUG)
                            log.debug("- Filter to allow all documents");

                        bitset.set(0, maxDoc);
                        return BitsFilteredDocIdSet.wrap(bitset, acceptDocs);
                    };

                    // There is no possible match
                    if (DEBUG)
                        log.debug("- Filter to allow no documents (1)");
                    return null;
                }

                // The result is unimportant
                else if (this.isOptional) {
                    if (DEBUG)
                        log.debug("- Filter is ignorable");
                    continue;
                };

                // There is no possible match
                if (DEBUG)
                    log.debug("- Filter to allow no documents (2)");
                return null;
            }

            // Initialize bitset
            else if (init) {

                bitset.or(filterIter);

                if (DEBUG)
                    log.debug("- Filter is inial with card {}",
                            bitset.cardinality());

                // Flip the matching documents
                if (operand.isNegative) {
                    bitset.flip(0, maxDoc);
                    if (DEBUG)
                        log.debug(
                                "- Filter is negative - so flipped to card {} (1)",
                                bitset.cardinality());
                };

                init = false;
            }
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
                        bitset.andNot(filterIter);
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
                    bitset.and(filterIter);
                    // TODO: Check with nextSetBit() if the filter is not applicable
                };

                if (DEBUG)
                    log.debug("- Subresult has card {} ", bitset.cardinality());
            };
        };
        return BitsFilteredDocIdSet.wrap(bitset, acceptDocs);
    };
};
