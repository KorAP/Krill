package de.ids_mannheim.korap;

import org.apache.lucene.search.spans.SpanQuery;

import de.ids_mannheim.korap.query.wrap.*;
import org.apache.lucene.util.automaton.RegExp;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Nils Diewald
 *
 * KorapQuery implements a simple API for wrapping
 * KorAP Index I specific query classes.
 */
public class KorapQuery {
    private String field;

    // Logger
    private final static Logger log = LoggerFactory.getLogger(KorapQuery.class);

    /**
     * Constructs a new base object for query generation.
     * @param field The specific index field for the query.
     */
    public KorapQuery (String field) {
	this.field = field;
    };


    // SpanSegmentRegexQuery
    /**
     * Create a query object based on a regular expression.
     * @param re The regular expession as a string.
     */
    public SpanRegexQueryWrapper re (String re) {
	return new SpanRegexQueryWrapper(this.field, re, RegExp.ALL, false);
    };

    /**
     * Create a query object based on a regular expression.
     * @param re The regular expession as a string.
     * @param flas The regular expession flag as an integer.
     */
    public SpanRegexQueryWrapper re (String re, int flags) {
	return new SpanRegexQueryWrapper(this.field, re, flags, false);
    };


    /**
     * Create a query object based on a regular expression.
     * @param re The regular expession as a string.
     * @param flag The regular expession flag.
     * @param caseinsensitive A boolean value indicating case insensitivity.
     */
    public SpanRegexQueryWrapper re (String re, int flags, boolean caseinsensitive) {
	return new SpanRegexQueryWrapper(this.field, re, flags, caseinsensitive);
    };


    /**
     * Create a query object based on a regular expression.
     * @param re The regular expession as a string.
     * @param caseinsensitive A boolean value indicating case insensitivity.
     */
    public SpanRegexQueryWrapper re (String re, boolean caseinsensitive) {
	return new SpanRegexQueryWrapper(this.field, re, RegExp.ALL, caseinsensitive);
    };


    // SpanSegmentQueries
    /**
     * Create a segment query object.
     */
    public SpanSegmentQueryWrapper seg () {
	return new SpanSegmentQueryWrapper(this.field);
    };


    /**
     * Create a segment query object.
     * @param terms[] An array of terms, the segment consists of.
     */
    public SpanSegmentQueryWrapper seg (SpanRegexQueryWrapper ... terms) {
	SpanSegmentQueryWrapper ssq = new SpanSegmentQueryWrapper(this.field);
	for (SpanRegexQueryWrapper t : terms) {
	    ssq.with(t);
	};
	return ssq;
    };

    public SpanSegmentQueryWrapper seg (SpanAlterQueryWrapper ... terms) {
	SpanSegmentQueryWrapper ssq = new SpanSegmentQueryWrapper(this.field);
	for (SpanAlterQueryWrapper t : terms) {
	    ssq.with(t);
	};
	return ssq;
    };

    public SpanSegmentQueryWrapper seg (String ... terms) {
	SpanSegmentQueryWrapper ssq = new SpanSegmentQueryWrapper(this.field);
	for (String t : terms) {
	    ssq.with(t);
	};
	return ssq;
    };

    // SpanSegmentAlterQueries
    /**
     * Create a segment alternation query object.
     * @param terms[] An array of alternative terms.
     */
    public SpanAlterQueryWrapper or (SpanQueryWrapperInterface ... terms) {
	SpanAlterQueryWrapper ssaq = new SpanAlterQueryWrapper(this.field);
	for (SpanQueryWrapperInterface t : terms) {
	    ssaq.or(t);
	};
	return ssaq;
    };

    public SpanAlterQueryWrapper or (String ... terms) {
	SpanAlterQueryWrapper ssaq = new SpanAlterQueryWrapper(this.field);
	for (String t : terms) {
	    ssaq.or(t);
	};
	return ssaq;
    };


    // SpanSegmentSequenceQueries
    /**
     * Create a sequence of segments query object.
     */
    public SpanSequenceQueryWrapper seq () {
	return new SpanSequenceQueryWrapper(this.field);
    };


    /**
     * Create a sequence of segments query object.
     * @param terms[] An array of segment defining terms.
     */
    public SpanSequenceQueryWrapper seq (SpanQueryWrapperInterface ... terms) {
	SpanSequenceQueryWrapper sssq = new SpanSequenceQueryWrapper(this.field);
	for (SpanQueryWrapperInterface t : terms) {
	    sssq.append(t);
	};
	return sssq;
    };


    /**
     * Create a sequence of segments query object.
     * @param re A SpanSegmentRegexQuery, starting the sequence.
     */
    public SpanSequenceQueryWrapper seq (SpanRegexQueryWrapper re) {
	return new SpanSequenceQueryWrapper(this.field, re);
    };


    public SpanSequenceQueryWrapper seq (Object ... terms) {
	SpanSequenceQueryWrapper ssq = new SpanSequenceQueryWrapper(this.field);
	for (Object t : terms) {
	    if (t instanceof SpanQueryWrapperInterface) {
		ssq.append((SpanQueryWrapperInterface) t);
	    }
	    else if (t instanceof SpanRegexQueryWrapper) {
		ssq.append((SpanRegexQueryWrapper) t);
	    }
	    else {
		log.error("{} is not an acceptable parameter for seq()", t.getClass());
		return ssq;
	    };
	};
	return ssq;
    };

    public SpanElementQueryWrapper tag (String element) {
	return new SpanElementQueryWrapper(this.field, element);
    };

    /**
     * Create a wrapping within query object.
     * @param element A SpanQuery.
     * @param embedded A SpanQuery that is wrapped in the element.
     */
    public SpanWithinQueryWrapper within (SpanQueryWrapperInterface element,
					  SpanQueryWrapperInterface embedded) {
	return new SpanWithinQueryWrapper(element, embedded);
    };



    // Class
    public SpanClassQueryWrapper _ (byte number, SpanQueryWrapperInterface element) {
	return new SpanClassQueryWrapper(element, number);
    };

    public SpanClassQueryWrapper _ (int number, SpanQueryWrapperInterface element) {
	return new SpanClassQueryWrapper(element, number);
    };

    public SpanClassQueryWrapper _ (short number, SpanQueryWrapperInterface element) {
	return new SpanClassQueryWrapper(element, number);
    };

    public SpanClassQueryWrapper _ (SpanQueryWrapperInterface element) {
	return new SpanClassQueryWrapper(element);
    };

    // MatchModify
    public SpanMatchModifyQueryWrapper shrink (byte number, SpanQueryWrapperInterface element) {
	return new SpanMatchModifyQueryWrapper(element, number);
    };

    public SpanMatchModifyQueryWrapper shrink (int number, SpanQueryWrapperInterface element) {
	return new SpanMatchModifyQueryWrapper(element, number);
    };

    public SpanMatchModifyQueryWrapper shrink (short number, SpanQueryWrapperInterface element) {
	return new SpanMatchModifyQueryWrapper(element, number);
    };

    public SpanMatchModifyQueryWrapper shrink (SpanQueryWrapperInterface element) {
	return new SpanMatchModifyQueryWrapper(element);
    };

    // split

};
