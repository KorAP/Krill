package de.ids_mannheim.korap.query;

import de.ids_mannheim.korap.KrillQuery;
import de.ids_mannheim.korap.query.wrap.*;
import org.apache.lucene.util.automaton.RegExp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * QueryBuilder implements a simple API for wrapping
 * KrillQuery classes.
 * 
 * Build complex queries.
 * <blockquote><pre>
 * QueryBuilder qb = new QueryBuilder("tokens");
 * SpanQueryWrapper sqw = (SpanQueryWrapper)
 * qb.seq(
 * qb.empty(),
 * qb.seg(
 * qb.re("mate/p=N.*"),
 * qb.re("opennlp/p=N.*")
 * )
 * );
 * </pre></blockquote>
 * 
 * @author diewald
 */
public class QueryBuilder {
    private String field;

    // Logger
    private final static Logger log = LoggerFactory.getLogger(KrillQuery.class);

    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;
   
    // <legacy>
    public static final byte OVERLAP = SpanWithinQuery.OVERLAP,
            REAL_OVERLAP = SpanWithinQuery.REAL_OVERLAP,
            WITHIN = SpanWithinQuery.WITHIN,
            REAL_WITHIN = SpanWithinQuery.REAL_WITHIN,
            ENDSWITH = SpanWithinQuery.ENDSWITH,
            STARTSWITH = SpanWithinQuery.STARTSWITH,
            MATCH = SpanWithinQuery.MATCH;


    // </legacy>


    /**
     * Construct a new QueryBuilder object.
     */
    public QueryBuilder (String field) {
        this.field = field;
    };


    /**
     * Create a query object based on a regular expression.
     * 
     * <blockquote><pre>
     * QueryBuilder kq = new QueryBuilder("tokens");
     * SpanRegexQueryWrapper re = kq.re(".+?");
     * </pre></blockquote>
     * 
     * @param re
     *            The regular expession as a string.
     * @return A {@link SpanRegexQueryWrapper} object.
     */
    public SpanRegexQueryWrapper re (String re) {
        return new SpanRegexQueryWrapper(this.field, re);
    };


    /**
     * Create a query object based on a regular expression.
     * 
     * Supports flags as defined in
     * {@link org.apache.lucene.util.automaton.RegExp}:
     * <ul>
     * <li><tt>RegExp.ALL</tt> - enables all optional regexp
     * syntax</li>
     * <li><tt>RegExp.ANYSTRING</tt> - enables anystring (@)</li>
     * <li><tt>RegExp.AUTOMATON</tt> - enables named automata
     * (&lt;identifier&gt;)</li>
     * <li><tt>RegExp.COMPLEMENT</tt> - enables complement (~)</li>
     * <li><tt>RegExp.EMPTY</tt> - enables empty language (#)</li>
     * <li><tt>RegExp.INTERSECTION</tt> - enables intersection
     * (&amp;)</li>
     * <li><tt>RegExp.INTERVAL</tt> - enables numerical intervals
     * (&lt;n-m&gt;)</li>
     * <li><tt>RegExp.NONE</tt> - enables no optional regexp
     * syntax</li>
     * </ul>
     * 
     * <blockquote><pre>
     * QueryBuilder kq = new QueryBuilder("tokens");
     * SpanRegexQueryWrapper re = kq.re("[Aa]lternatives?",
     * RegExp.NONE);
     * </pre></blockquote>
     * 
     * @param re
     *            The regular expession as a string.
     * @param flags
     *            The flag for the regular expression.
     * @return A {@link SpanRegexQueryWrapper} object.
     */
    public SpanRegexQueryWrapper re (String re, int flags) {
        return new SpanRegexQueryWrapper(this.field, re, flags, false);
    };


    /**
     * Create a query object based on a regular expression.
     * 
     * Supports flags (see above) and case insensitivity.
     * 
     * <blockquote><pre>
     * QueryBuilder kq = new QueryBuilder("tokens");
     * SpanRegexQueryWrapper re = kq.re("alternatives?", RegExp.NONE,
     * true);
     * </pre></blockquote>
     * 
     * @param re
     *            The regular expession as a string.
     * @param flags
     *            The flag for the regular expression.
     * @param caseinsensitive
     *            A boolean value indicating case insensitivity.
     * @return A {@link SpanRegexQueryWrapper} object.
     */
    public SpanRegexQueryWrapper re (String re, int flags,
            boolean caseinsensitive) {
        return new SpanRegexQueryWrapper(this.field, re, flags,
                caseinsensitive);
    };


    /**
     * Create a query object based on a regular expression.
     * 
     * Supports case insensitivity.
     * 
     * <blockquote><pre>
     * QueryBuilder kq = new QueryBuilder("tokens");
     * SpanRegexQueryWrapper re = kq.re("alternatives?", true);
     * </pre></blockquote>
     * 
     * @param re
     *            The regular expession as a string.
     * @param flags
     *            The flag for the regular expression.
     * @return A {@link SpanRegexQueryWrapper} object.
     */
    public SpanRegexQueryWrapper re (String re, boolean caseinsensitive) {
        return new SpanRegexQueryWrapper(this.field, re,
                caseinsensitive);
    };


    /**
     * Create a query object based on a wildcard term.
     * <tt>*</tt> indicates an optional sequence of arbitrary
     * characters,
     * <tt>?</tt> indicates a single character,
     * <tt>\</tt> can be used for escaping.
     * 
     * @param wc
     *            The wildcard term as a string.
     * @return A {@link SpanWildcardQueryWrapper} object.
     */
    public SpanWildcardQueryWrapper wc (String wc) {
        return new SpanWildcardQueryWrapper(this.field, wc, false);
    };


    /**
     * Create a query object based on a wildcard term.
     * <tt>*</tt> indicates an optional sequence of arbitrary
     * characters,
     * <tt>?</tt> indicates a single character,
     * <tt>\</tt> can be used for escaping.
     * 
     * Supports case insensitivity.
     * 
     * <blockquote><pre>
     * QueryBuilder kq = new QueryBuilder("tokens");
     * SpanWildcardQueryWrapper wc = kq.wc("wall*", true);
     * </pre></blockquote>
     * 
     * @param wc
     *            The wildcard term as a string.
     * @param caseinsensitive
     *            A boolean value indicating case insensitivity.
     * @return A {@link SpanWildcardQueryWrapper} object.
     */
    public SpanWildcardQueryWrapper wc (String wc, boolean caseinsensitive) {
        return new SpanWildcardQueryWrapper(this.field, wc, caseinsensitive);
    };


    /**
     * Create a segment query object.
     * 
     * <blockquote><pre>
     * QueryBuilder kq = new QueryBuilder("tokens");
     * SpanSegmentQueryWrapper seg = kq.seg();
     * </pre></blockquote>
     * 
     * @return A {@link SpanSegmentQueryWrapper} object.
     */
    public SpanSegmentQueryWrapper seg () {
        return new SpanSegmentQueryWrapper(this.field);
    };


    /**
     * Create a segment query object.
     * Supports sequences of strings or {@link SpanRegexQueryWrapper},
     * and {@link SpanAlterQueryWrapper} objects.
     * 
     * <blockquote><pre>
     * QueryBuilder kq = new QueryBuilder("tokens");
     * SpanSegmentQueryWrapper seg = kq.seg(
     * kq.re("mate/p=.*?"),
     * kq.re("opennlp/p=.*?")
     * );
     * </pre></blockquote>
     * 
     * @param terms
     *            [] An array of terms, the segment consists of.
     * @return A {@link SpanSegmentQueryWrapper} object.
     */
    // Sequence of regular expression queries
    public SpanSegmentQueryWrapper seg (SpanRegexQueryWrapper ... terms) {
        SpanSegmentQueryWrapper ssq = new SpanSegmentQueryWrapper(this.field);
        for (SpanRegexQueryWrapper t : terms)
            ssq.with(t);
        return ssq;
    };


    // Sequence of alternative queries
    public SpanSegmentQueryWrapper seg (SpanAlterQueryWrapper ... terms) {
        SpanSegmentQueryWrapper ssq = new SpanSegmentQueryWrapper(this.field);
        for (SpanAlterQueryWrapper t : terms)
            ssq.with(t);
        return ssq;
    };


    // Sequence of alternative queries
    public SpanSegmentQueryWrapper seg (String ... terms) {
        SpanSegmentQueryWrapper ssq = new SpanSegmentQueryWrapper(this.field);
        for (String t : terms)
            ssq.with(t);
        return ssq;
    };


    /**
     * Create an empty query segment.
     * 
     * <blockquote><pre>
     * QueryBuilder kq = new QueryBuilder("tokens");
     * SpanRepetitionQueryWrapper seg = kq.empty();
     * </pre></blockquote>
     */
    public SpanRepetitionQueryWrapper empty () {
        return new SpanRepetitionQueryWrapper();
    };


    // TODO: Further JavaDocs


    /**
     * Create a segment alternation query object.
     * 
     * @param terms
     *            [] An array of alternative terms.
     */
    public SpanAlterQueryWrapper or (SpanQueryWrapper ... terms) {
        SpanAlterQueryWrapper ssaq = new SpanAlterQueryWrapper(this.field);
        for (SpanQueryWrapper t : terms)
            ssaq.or(t);
        return ssaq;
    };


    public SpanAlterQueryWrapper or (String ... terms) {
        SpanAlterQueryWrapper ssaq = new SpanAlterQueryWrapper(this.field);
        for (String t : terms)
            ssaq.or(t);
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
     * 
     * @param terms
     *            [] An array of segment defining terms.
     */
    public SpanSequenceQueryWrapper seq (SpanQueryWrapper ... terms) {
        SpanSequenceQueryWrapper sssq = new SpanSequenceQueryWrapper(
                this.field);
        for (SpanQueryWrapper t : terms)
            sssq.append(t);
        return sssq;
    };


    /**
     * Create a sequence of segments query object.
     * 
     * @param re
     *            A SpanSegmentRegexQuery, starting the sequence.
     */
    public SpanSequenceQueryWrapper seq (SpanRegexQueryWrapper re) {
        return new SpanSequenceQueryWrapper(this.field, re);
    };


    public SpanSequenceQueryWrapper seq (Object ... terms) {
        SpanSequenceQueryWrapper ssq = new SpanSequenceQueryWrapper(this.field);
        for (Object t : terms) {
            if (t instanceof SpanQueryWrapper) {
                ssq.append((SpanQueryWrapper) t);
            }
            else if (t instanceof SpanRegexQueryWrapper) {
                ssq.append((SpanRegexQueryWrapper) t);
            }
            else {
                log.error("{} is not an acceptable parameter for seq()",
                        t.getClass());
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
     * 
     * @param element
     *            A SpanQuery.
     * @param embedded
     *            A SpanQuery that is wrapped in the element.
     */
    public SpanWithinQueryWrapper contains (SpanQueryWrapper element,
            SpanQueryWrapper embedded) {
        return new SpanWithinQueryWrapper(element, embedded, WITHIN);
    };


    public SpanWithinQueryWrapper startswith (SpanQueryWrapper element,
            SpanQueryWrapper embedded) {
        return new SpanWithinQueryWrapper(element, embedded, STARTSWITH);
    };


    public SpanWithinQueryWrapper endswith (SpanQueryWrapper element,
            SpanQueryWrapper embedded) {
        return new SpanWithinQueryWrapper(element, embedded, ENDSWITH);
    };


    public SpanWithinQueryWrapper overlaps (SpanQueryWrapper element,
            SpanQueryWrapper embedded) {
        return new SpanWithinQueryWrapper(element, embedded, OVERLAP);
    };


    public SpanWithinQueryWrapper matches (SpanQueryWrapper element,
            SpanQueryWrapper embedded) {
        return new SpanWithinQueryWrapper(element, embedded, MATCH);
    };


    // Class
    public SpanClassQueryWrapper nr (byte number, SpanQueryWrapper element) {
        return new SpanClassQueryWrapper(element, number);
    };


    public SpanClassQueryWrapper nr (int number, SpanQueryWrapper element) {
        return new SpanClassQueryWrapper(element, number);
    };


    public SpanClassQueryWrapper nr (short number, SpanQueryWrapper element) {
        return new SpanClassQueryWrapper(element, number);
    };


    public SpanClassQueryWrapper nr (SpanQueryWrapper element) {
        return new SpanClassQueryWrapper(element);
    };


    // Focus
    public SpanFocusQueryWrapper focus (byte number, SpanQueryWrapper element) {
        return new SpanFocusQueryWrapper(element, number);
    };


    public SpanFocusQueryWrapper focus (int number, SpanQueryWrapper element) {
        return new SpanFocusQueryWrapper(element, number);
    };


    public SpanFocusQueryWrapper focus (short number,
            SpanQueryWrapper element) {
        return new SpanFocusQueryWrapper(element, number);
    };


    public SpanFocusQueryWrapper focus (SpanQueryWrapper element) {
        return new SpanFocusQueryWrapper(element);
    };


    // Repetition
    public SpanRepetitionQueryWrapper repeat (SpanQueryWrapper element,
            int exact) {
        return new SpanRepetitionQueryWrapper(element, exact);
    };


    public SpanRepetitionQueryWrapper repeat (SpanQueryWrapper element, int min,
            int max) {
        return new SpanRepetitionQueryWrapper(element, min, max);
    };

    // Optionality
    public SpanRepetitionQueryWrapper opt (SpanQueryWrapper element) {
        return new SpanRepetitionQueryWrapper(element, 0, 1);
    };
};
