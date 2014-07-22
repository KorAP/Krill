package de.ids_mannheim.korap;

import de.ids_mannheim.korap.query.wrap.*;
import de.ids_mannheim.korap.util.QueryException;

import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.util.automaton.RegExp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.query.SpanWithinQuery;

import java.util.*;
import java.io.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
  TODO: Create Pre-filter while preparing a Query.
  The pre-filter will contain a boolena query with all
  necessary terms, supporting boolean OR, ignoring
  negation terms (and negation subqueries), like
  [base=Der]([base=alte]|[base=junge])[base=Mann & p!=ADJA]![base=war | base=lag]
  Search for all documents containing "s:Der" and ("s:alte" or "s:junge") and "s:Mann"
*/

/**
 * @author Nils Diewald
 *
 * KorapQuery implements a simple API for wrapping
 * KorAP Lucene Index specific query classes.
 */
public class KorapQuery {
    private String field;
    private ObjectMapper json;

    // The default foundry for lemmata and pos
    private String defaultFoundry = "mate/";

    // Logger
    private final static Logger log = LoggerFactory.getLogger(KorapQuery.class);

    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;

    public static final byte
	OVERLAP      = SpanWithinQuery.OVERLAP,
	REAL_OVERLAP = SpanWithinQuery.REAL_OVERLAP,
	WITHIN       = SpanWithinQuery.WITHIN,
	REAL_WITHIN  = SpanWithinQuery.REAL_WITHIN,
	ENDSWITH     = SpanWithinQuery.ENDSWITH,
	STARTSWITH   = SpanWithinQuery.STARTSWITH,
	MATCH        = SpanWithinQuery.MATCH;


    /**
     * Constructs a new base object for query generation.
     * @param field The specific index field for the query.
     */
    public KorapQuery (String field) {
	this.field = field;
	this.json = new ObjectMapper();
    };

    public SpanQueryWrapperInterface fromJSON (String jsonString) throws QueryException {
	JsonNode json;
	try {
	    json = this.json.readValue(jsonString, JsonNode.class);
	}
	catch (IOException e) {
	    throw new QueryException(e.getMessage());
	};

	if (!json.has("@type") && json.has("query"))
	    json = json.get("query");

	return this.fromJSON(json);
    };

    // http://fasterxml.github.io/jackson-databind/javadoc/2.2.0/com/fasterxml/jackson/databind/JsonNode.html
    // TODO: Exception messages are horrible!
    // TODO: Use the shortcuts implemented in this class instead of the wrapper constructors
    // TODO: Check for isArray()
    // TODO: Rename this span context!
    public SpanQueryWrapperInterface fromJSON (JsonNode json) throws QueryException {

	if (!json.has("@type")) {
	    throw new QueryException("JSON-LD group has no @type attribute");
	};

	String type = json.get("@type").asText();

	switch (type) {

	case "korap:group":
	    SpanClassQueryWrapper classWrapper;

	    if (!json.has("operation"))
		throw new QueryException("Group expects operation");

	    String operation = json.get("operation").asText();

	    // Get all operands
	    JsonNode operands = json.get("operands");

	    if (!json.has("operands") || !operands.isArray())
		throw new QueryException("Operation needs operand list");

	    switch (operation) {

	    case "operation:or":

		SpanAlterQueryWrapper ssaq = new SpanAlterQueryWrapper(this.field);
		for (JsonNode operand : operands) {
		    ssaq.or(this.fromJSON(operand));
		};
		return ssaq;

	    case "operation:position":
		if (!json.has("frame"))
		    throw new QueryException("Operation needs frame specification");

		if (operands.size() != 2)
		    throw new QueryException("Operation needs exactly two operands");

		// TODO: Check for operands

		String frame = json.has("frame") ? json.get("frame").asText() : "contains";
		byte flag = WITHIN;
		switch (frame) {
		case "frame:contains":
		    break;
		case "frame:strictlyContains":
		    flag = REAL_WITHIN;
		    break;
		case "frame:within":
		    break;
		case "frame:startswith":
		    flag = STARTSWITH;
		    break;
		case "frame:endswith":
		    flag = ENDSWITH;
		    break;
		case "frame:matches":
		    flag = MATCH;
		    break;
		case "frame:overlaps":
		    flag = OVERLAP;
		    break;
		case "frame:strictlyOverlaps":
		    flag = REAL_OVERLAP;
		    break;
		default:
		    throw new QueryException("Frame type unknown");
		};

		// Check for exclusion modificator
		Boolean exclude;
		if (json.has("exclude") && json.get("exclude").asBoolean())
		    throw new QueryException("Exclusion is currently not supported in position operations");

		return new SpanWithinQueryWrapper(
		    this.fromJSON(operands.get(0)),
		    this.fromJSON(operands.get(1)),
		    flag
		);

	    // TODO: This is DEPRECATED and should be communicated that way
	    case "operation:submatch":
		int number = 0;

		if (operands.size() != 1)
		    throw new QueryException("Operation needs exactly two operands");

		if (json.has("classRef")) {
		    if (json.has("classRefOp"))
			throw new QueryException("Class reference operators not supported yet");

		    number = json.get("classRef").get(0).asInt();
		}
		else if (json.has("spanRef")) {
		    throw new QueryException("Span references not supported yet");
		};

		return new SpanMatchModifyQueryWrapper(
		    this.fromJSON(operands.get(0)), number
                );

	    case "operation:sequence":
		if (operands.size() < 2)
		    throw new QueryException(
		        "SpanSequenceQuery needs at least two operands"
		    );

		SpanSequenceQueryWrapper sseqqw = this.seq();
		for (JsonNode operand : operands) {
		    sseqqw.append(this.fromJSON(operand));
		};

		// Say if the operand order is important
		if (json.has("inOrder"))
		    sseqqw.setInOrder(json.get("inOrder").asBoolean());

		// Introduce distance constraints
		if (json.has("distances")) {

		    // TODO
		    if (json.has("exclude") && json.get("exclude").asBoolean())
			throw new QueryException(
			    "Excluding distance constraints are not supported yet"
			);

		    // TEMPORARY: Workaround for group distances
		    JsonNode firstDistance = json.get("distances").get(0);
		    if (!firstDistance.has("@type"))
			throw new QueryException("Distances need a defined @type");

		    JsonNode distances;
		    if (firstDistance.get("@type").asText().equals("korap:group"))
			distances = firstDistance.get("operands");
		    else if (firstDistance.get("@type").asText().equals("korap:distance"))
			distances = json.get("distances");
		    else
			throw new QueryException("No valid distances defined");

		    for (JsonNode constraint : distances) {
			String unit = "w";
			if (constraint.has("key"))
			    unit = constraint.get("key").asText();

			sseqqw.withConstraint(
		            constraint.get("min").asInt(1),
			    constraint.get("max").asInt(1),
			    unit
			);
		    };
		};

		// inOrder was set without a distance constraint
		if (!sseqqw.isInOrder() && !sseqqw.hasConstraints()) {
		    sseqqw.withConstraint(1,1,"w");
		};

		return sseqqw;

	    case "operation:class":
		if (json.has("class")) {
		    if (operands.size() != 1)
			throw new QueryException(
			    "Class group expects exactly one operand in list"
			);
		    return new SpanClassQueryWrapper(this.fromJSON(operands.get(0)), json.get("class").asInt(0));
		};

		throw new QueryException("Class group expects class attribute");

	    case "operation:repetition":

		// temporary
		int min = json.get("min").asInt(1);
		int max = json.get("max").asInt(1);

		// Sanitize max
		if (max < 0)
		    max = 100;
		else if (max > 100)
		    max = 100;

		// Sanitize min
		if (min < 0)
		    min = 0;
		else if (min > 100)
		    max = 100;

		// Check relation between min and max
		if (min > max)
		    throw new QueryException("The maximum repetition value has to " +
					     "be greater or equal to the minimum repetition value");

		if (min == 0)
		    throw new QueryException("Minimum value of zero is not supported yet");

		return new SpanRepetitionQueryWrapper(
		    this.fromJSON(operands.get(0)), min, max
		);
	    };

	    throw new QueryException("Unknown group operation");

	case "korap:reference":
	    if (json.has("operation") && !json.get("operation").asText().equals("operation:focus"))
		throw new QueryException("Reference operation " + json.get("operation").asText() + " not supported yet");

	    int number = 0;

	    operands = json.get("operands");

	    if (operands.size() == 0)
		throw new QueryException("Focus with peripheral references is not supported yet");

	    if (operands.size() != 1)
		throw new QueryException("Operation needs exactly two operands");


	    if (json.has("classRef")) {
		if (json.has("classRefOp"))
		    throw new QueryException("Class reference operators not supported yet");

		number = json.get("classRef").get(0).asInt();
	    }
	    else if (json.has("spanRef")) {
		throw new QueryException("Span references not supported yet");
	    };

	    return new SpanMatchModifyQueryWrapper(
	        this.fromJSON(operands.get(0)), number
	    );

	case "korap:token":
	    if (!json.has("wrap"))
		throw new QueryException("Tokens need a wrap attribute");

	    return this._segFromJSON(json.get("wrap"));

	case "korap:span":
	    if (!json.has("key"))
		throw new QueryException("A span need at least a key definition");

	    return this._termFromJSON(json);
	};
	throw new QueryException("Unknown serialized query type: " + type);
    };



    private SpanQueryWrapperInterface _segFromJSON (JsonNode json) throws QueryException {
	String type = json.get("@type").asText();
	switch (type) {

	case "korap:term":
	    String match = "match:eq";
	    if (json.has("match"))
		match = json.get("match").asText();

	    switch (match) {
	    case "match:ne":
		return this.seg().without((SpanSegmentQueryWrapper) this._termFromJSON(json));
	    case "match:eq":
		return this._termFromJSON(json);
	    };

	    throw new QueryException("Match relation unknown");

	case "korap:termGroup":

	    if (!json.has("operands"))
		throw new QueryException("TermGroup expects operands");

	    SpanSegmentQueryWrapper ssegqw = this.seg();

	    if (!json.has("relation"))
		throw new QueryException("termGroup expects a relation");

	    switch (json.get("relation").asText()) {
	    case "relation:and":

		for (JsonNode operand : json.get("operands")) {
		    SpanQueryWrapperInterface part = this._segFromJSON(operand);
		    if (part instanceof SpanAlterQueryWrapper) {
			ssegqw.with((SpanAlterQueryWrapper) part);			
		    }
		    else if (part instanceof SpanRegexQueryWrapper) {
			ssegqw.with((SpanRegexQueryWrapper) part);
		    }
		    else if (part instanceof SpanSegmentQueryWrapper) {
			ssegqw.with((SpanSegmentQueryWrapper) part);
		    }
		    else {
			throw new QueryException("Object not supported in segment queries");
		    };
		};
		return ssegqw;

	    case "relation:or":
		SpanAlterQueryWrapper ssaq = new SpanAlterQueryWrapper(this.field);
		for (JsonNode operand : json.get("operands")) {
		    ssaq.or(this._segFromJSON(operand));
		};
		return ssaq;
	    };
	};
	throw new QueryException("Unknown token type");    
    };



    private SpanQueryWrapperInterface _termFromJSON (JsonNode json) throws QueryException {
	if (!json.has("key") || json.get("key").asText().length() < 1)
	    throw new QueryException("Terms and spans have to provide key attributes");
	    
	Boolean isTerm = json.get("@type").asText().equals("korap:term") ? true : false;
	Boolean isCaseInsensitive = false;

	if (json.has("caseInsensitive") && json.get("caseInsensitive").asBoolean())
	    isCaseInsensitive = true;

	StringBuilder value = new StringBuilder();

	// expect orth? expect lemma? 
	// s:den | i:den | cnx/l:die | mate/m:mood:ind | cnx/syn:@PREMOD |
	// mate/m:number:sg | opennlp/p:ART

	if (json.has("foundry") && json.get("foundry").asText().length() > 0)
	    value.append(json.get("foundry").asText()).append('/');

	// value.append(defaultFoundry).append('/');

	if (json.has("layer") && json.get("layer").asText().length() > 0) {
	    String layer = json.get("layer").asText();
	    switch (layer) {

	    case "lemma":
		layer = "l";
		break;

	    case "pos":
		layer = "p";
		break;

	    case "orth":
		layer = "s";
		break;
	    };

	    if (isCaseInsensitive && isTerm && layer.equals("s"))
		layer = "i";


	    // TEMPORARY
	    if (value.length() == 0 && (layer.equals("l") || layer.equals("p")))
		value.append(defaultFoundry);


	    value.append(layer).append(':');
	};

	if (json.has("key") && json.get("key").asText().length() > 0) {
	    String key = json.get("key").asText();
	    value.append(isCaseInsensitive ? key.toLowerCase() : key);
	};

	// Regular expression or wildcard
	if (isTerm && json.has("type")) {
	    switch (json.get("type").asText()) {
	    case "type:regex":
		return this.seg(this.re(value.toString(), isCaseInsensitive));
	    case "type:wildcard":
		return this.seq(this.wc(value.toString(), isCaseInsensitive));
	    };
	};

	if (json.has("value") && json.get("value").asText().length() > 0)
	    value.append(':').append(json.get("value").asText());

	if (isTerm)
	    return this.seg(value.toString());

	if (json.has("attr"))
	    throw new QueryException("Attributes not yet supported in spans");

	return this.tag(value.toString());
    };


    // SpanRegexQueryWrapper
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

    // SpanWildcardQueryWrapper
    /**
     * Create a query object based on a wildcard term.
     * @param wc The wildcard term as a string.
     */
    public SpanWildcardQueryWrapper wc (String wc) {
	return new SpanWildcardQueryWrapper(this.field, wc, false);
    };

    /**
     * Create a query object based on a wildcard term.
     * @param wc The wildcard term as a string.
     * @param caseinsensitive A boolean value indicating case insensitivity.
     */
    public SpanWildcardQueryWrapper wc (String wc, boolean caseinsensitive) {
	return new SpanWildcardQueryWrapper(this.field, wc, caseinsensitive);
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
    @Deprecated
    public SpanWithinQueryWrapper within (SpanQueryWrapperInterface element,
					  SpanQueryWrapperInterface embedded) {
	return new SpanWithinQueryWrapper(element, embedded);
    };

    public SpanWithinQueryWrapper contains (SpanQueryWrapperInterface element,
					  SpanQueryWrapperInterface embedded) {
	return new SpanWithinQueryWrapper(element, embedded, WITHIN);
    };

    public SpanWithinQueryWrapper startswith (SpanQueryWrapperInterface element,
					      SpanQueryWrapperInterface embedded) {
	return new SpanWithinQueryWrapper(element, embedded, STARTSWITH);
    };

    public SpanWithinQueryWrapper endswith (SpanQueryWrapperInterface element,
					    SpanQueryWrapperInterface embedded) {
	return new SpanWithinQueryWrapper(element, embedded, ENDSWITH);
    };

    public SpanWithinQueryWrapper overlaps (SpanQueryWrapperInterface element,
					    SpanQueryWrapperInterface embedded) {
	return new SpanWithinQueryWrapper(element, embedded, OVERLAP);
    }; 

    public SpanWithinQueryWrapper matches (SpanQueryWrapperInterface element,
					   SpanQueryWrapperInterface embedded) {
	return new SpanWithinQueryWrapper(element, embedded, MATCH);
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

    // Repetition
    public SpanRepetitionQueryWrapper repeat (SpanQueryWrapperInterface element, int exact) {
	return new SpanRepetitionQueryWrapper(element, exact);
    };

    public SpanRepetitionQueryWrapper repeat (SpanQueryWrapperInterface element, int min, int max) {
	return new SpanRepetitionQueryWrapper(element, min, max);
    };


    // split

};
