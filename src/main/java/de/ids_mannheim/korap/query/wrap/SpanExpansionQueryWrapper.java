package de.ids_mannheim.korap.query.wrap;

import org.apache.lucene.search.RegexpQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.util.automaton.RegExp;
import org.apache.lucene.index.Term;
import de.ids_mannheim.korap.query.wrap.SpanQueryWrapper;
import de.ids_mannheim.korap.query.SpanExpansionQuery;
import de.ids_mannheim.korap.util.QueryException;

import java.util.*;

/*
 * TODO: SpanExpansionQueryWrapper currently does not support negative extensions!
 */
/**
 * @author diewald
 */

public class SpanExpansionQueryWrapper extends SpanQueryWrapper {
    private SpanQueryWrapper anchor;

    // < 0 	to the left of anchor span 
    // >= 0  to the right of anchor span
    private int direction;

    // if > 0, collect expansion offsets
    // using this label
    private byte classNumber;


    public SpanExpansionQueryWrapper (SpanQueryWrapper anchor, int min, int max,
                                      int direction, byte classNumber) {
        this.anchor = anchor;
        this.isNull = false;
        this.min = min;
        this.max = max;
        this.direction = direction;
        this.classNumber = classNumber;
        this.isExtended = true;
        if (direction >= 0)
            this.isExtendedToTheRight = true;
        this.maybeUnsorted = anchor.maybeUnsorted();
    };


    @Override
    public boolean isNull () {
        // Needs to be overwritten, as min and max do not indicate null value
        return this.isNull;
    };


    @Override
    public SpanQuery toFragmentQuery () throws QueryException {
        return new SpanExpansionQuery(
                this.anchor.retrieveNode(this.retrieveNode).toFragmentQuery(),
                this.getMin(), this.getMax(), this.direction, this.classNumber,
                true);
    };
};
