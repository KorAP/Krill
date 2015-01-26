package de.ids_mannheim.korap;

import de.ids_mannheim.korap.query.wrap.*;
import de.ids_mannheim.korap.response.Notifications;
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
  Todo: All queries with a final right expansion
  der alte []
  should be wrapped in a contains(<base/s=t>) to ensure
  they are not outside the text.

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
public class KorapQuery extends Notifications {
    private String field;
    private ObjectMapper json;

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

    private static final int MAX_CLASS_NUM = 255; // 127;

    /**
     * Constructs a new base object for query generation.
     * @param field The specific index field for the query.
     */
    public KorapQuery (String field) {
	this.field = field;
	this.json = new ObjectMapper();
    };

    /**
     * Private class for korap:boundary objects
     */
    private class Boundary {
	public int min, max;

	public Boundary (JsonNode json, int defaultMin, int defaultMax) throws QueryException {

	    if (!json.has("@type"))
		throw new QueryException(701, "JSON-LD group has no @type attribute");

	    if (!json.get("@type").asText().equals("korap:boundary"))
		throw new QueryException(702, "Boundary definition is invalid");

	    // Set min boundary
	    if (json.has("min"))
		this.min = json.get("min").asInt(defaultMin);
	    else
		this.min = defaultMin;

	    // Set max boundary
	    if (json.has("max"))
		this.max = json.get("max").asInt(defaultMax);
	    else
		this.max = defaultMax;

	    if (DEBUG)
		log.trace("Found korap:boundary with {}:{}", min, max);
	};
    };

    public SpanQueryWrapper fromJson (String jsonString) throws QueryException {
	JsonNode json;
	try {
	    json = this.json.readValue(jsonString, JsonNode.class);
	}
	catch (IOException e) {
	    String msg = e.getMessage();
	    log.warn("Unable to parse JSON: " + msg.split("\n")[0]);
	    throw new QueryException(621, "Unable to parse JSON");
	};

	if (!json.has("@type") && json.has("query"))
	    json = json.get("query");

	return this.fromJson(json);
    };

    // http://fasterxml.github.io/jackson-databind/javadoc/2.2.0/com/fasterxml/jackson/databind/JsonNode.html
    // TODO: Exception messages are horrible!
    // TODO: Use the shortcuts implemented in this class instead of the wrapper constructors
    // TODO: Check for isArray()
    // TODO: Rename this span context!
    public SpanQueryWrapper fromJson (JsonNode json) throws QueryException {

	int number = 0;

	if (!json.has("@type"))
	    throw new QueryException(701, "JSON-LD group has no @type attribute");

	String type = json.get("@type").asText();

	switch (type) {

	case "korap:group":
	    SpanClassQueryWrapper classWrapper;

	    if (!json.has("operation"))
		throw new QueryException(703, "Group expects operation");

	    String operation = json.get("operation").asText();

	    if (DEBUG)
		log.trace("Found {} group", operation);

	    if (!json.has("operands"))
		throw new QueryException(704, "Operation needs operand list");

	    // Get all operands
	    JsonNode operands = json.get("operands");

	    if (!operands.isArray())
		throw new QueryException(704, "Operation needs operand list");

	    if (DEBUG)
		log.trace("Operands are {}", operands);

	    switch (operation) {

	    case "operation:or":
		SpanAlterQueryWrapper ssaq = new SpanAlterQueryWrapper(this.field);
		for (JsonNode operand : operands) {
		    ssaq.or(this.fromJson(operand));
		};
		return ssaq;

	    case "operation:position":

		if (operands.size() != 2)
		    throw new QueryException(705, "Number of operands is not acceptable");

		// TODO: Check for operands
		// TODO: LEGACY and not future proof
		String frame = json.has("frame") ?
		    json.get("frame").asText() :
		    "frame:contains";

		if (DEBUG)
		    log.trace("Position frame is '{}'", frame);

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
		case "":
		    // Temporary workaround for wrongly set overlaps
		    if (json.has("frames")) {
			frame = json.get("frames").get(0).asText();
			if (frame.equals("frames:overlapsLeft") ||
			    frame.equals("frames:overlapsRight")) {
			    flag = OVERLAP;
			    break;
			};
		    };
		default:
		    throw new QueryException(706, "Frame type is unknown");
		};

		// Check for exclusion modificator
		Boolean exclude;
		if (json.has("exclude") && json.get("exclude").asBoolean())
		    throw new QueryException(
			760,
		        "Exclusion is currently not supported in position operations"
		    );

		return new SpanWithinQueryWrapper(
		    this.fromJson(operands.get(0)),
		    this.fromJson(operands.get(1)),
		    flag
		);

	    // TODO: This is DEPRECATED and should be communicated that way
	    case "operation:submatch":

		if (operands.size() != 1)
		    throw new QueryException(705, "Number of operands is not acceptable");

		if (json.has("classRef")) {
		    if (json.has("classRefOp"))
			throw new QueryException(
			    761,
			    "Class reference operators are currently not supported"
                    );

		    number = json.get("classRef").get(0).asInt();
		}
		else if (json.has("spanRef")) {
		    throw new QueryException(
			762,
		        "Span references are currently not supported"
                    );
		};

		return new SpanMatchModifyQueryWrapper(
		    this.fromJson(operands.get(0)), number
                );

	    case "operation:sequence":

		// Sequence with only one operand
		if (operands.size() == 1)
		    return this.fromJson(operands.get(0));

		SpanSequenceQueryWrapper sseqqw = this.seq();

		// Say if the operand order is important
		if (json.has("inOrder"))
		    sseqqw.setInOrder(json.get("inOrder").asBoolean());

		// Introduce distance constraints
		// ATTENTION: Distances have to be set before segments are added
		if (json.has("distances")) {

		    // TODO
		    if (json.has("exclude") && json.get("exclude").asBoolean())
			throw new QueryException(
			    763,
			    "Excluding distance constraints are currently not supported"
			);

		    if (!json.get("distances").isArray()) {
			throw new QueryException(
			    707,
			    "Distance Constraints have " +
			    "to be defined as arrays"
			);
		    };

		    // TEMPORARY: Workaround for group distances
		    JsonNode firstDistance = json.get("distances").get(0);

		    if (!firstDistance.has("@type"))
			throw new QueryException(701, "JSON-LD group has no @type attribute");

		    JsonNode distances;
		    if (firstDistance.get("@type").asText().equals("korap:group")) {
			if (!firstDistance.has("operands") ||
			    !firstDistance.get("operands").isArray())
			    throw new QueryException(704, "Operation needs operand list");

			distances = firstDistance.get("operands");
		    }

		    // Support korap distances
		    // Support cosmas distances
		    else if (
			     firstDistance.get("@type").asText().equals("korap:distance")
			     ||
			     firstDistance.get("@type").asText().equals("cosmas:distance")) {

			distances = json.get("distances");
		    }

		    else
			throw new QueryException(708, "No valid distances defined");

		    // Add all distance constraint to query
		    for (JsonNode constraint : distances) {
			String unit = "w";
			if (constraint.has("key"))
			    unit = constraint.get("key").asText();

			// There is a maximum of 100 fix
			int min = 0, max = 100;
			if (constraint.has("boundary")) {
			    Boundary b = new Boundary(constraint.get("boundary"), 0,100);
			    min = b.min;
			    max = b.max;
			}
			else {
			    if (constraint.has("min"))
				min = constraint.get("min").asInt(0);
			    if (constraint.has("max"))
				max = constraint.get("max").asInt(100);
			};

			// Add foundry and layer to the unit for new indices
			if (constraint.has("foundry") &&
			    constraint.has("layer") &&
			    constraint.get("foundry").asText().length() > 0 &&
			    constraint.get("layer").asText().length() > 0) {

			    StringBuilder value = new StringBuilder();
			    value.append(constraint.get("foundry").asText());
			    value.append('/');
			    value.append(constraint.get("layer").asText());
			    value.append(':').append(unit);
			    unit = value.toString();
			};

			// Sanitize boundary
			if (max < min)
			    max = min;

			if (DEBUG)
			    log.trace("Add distance constraint of '{}': {}-{}",
				      unit, min, max);

			sseqqw.withConstraint(min, max, unit);
		    };
		};

		// Add segments to sequence
		for (JsonNode operand : operands) {
		    sseqqw.append(this.fromJson(operand));
		};

		// inOrder was set to false without a distance constraint
		if (!sseqqw.isInOrder() && !sseqqw.hasConstraints()) {
		    sseqqw.withConstraint(1,1,"w");
		};

		return sseqqw;

	    case "operation:class":
		number = 1;

		if (json.has("classOut")) {
		    number = json.get("classOut").asInt(0);
		}
		// Legacy classes
		else if (json.has("class")) {
		    number = json.get("class").asInt(0);
		};

		if (json.has("classRefCheck"))
		    this.addWarning(
		        764,
			"Class reference checks are currently not supported - results may not be correct"
                    );

		if (json.has("classRefOp"))
		    throw new QueryException(
		        761,
			"Class reference operators are currently not supported"
                    );

		if (number > 0) {
		    if (operands.size() != 1)
			throw new QueryException(
			    705,
			    "Number of operands is not acceptable"
			);

		    if (DEBUG)
			log.trace("Found Class definition for {}", number);

		    if (number > MAX_CLASS_NUM) {
			throw new QueryException(
			    709, "Valid class numbers exceeded"
                        );
		    };

		    SpanQueryWrapper sqw = this.fromJson(operands.get(0));

		    // Problematic
		    if (sqw.maybeExtension())
			return sqw.setClassNumber(number);

		    return new SpanClassQueryWrapper(sqw, number);
		};

		throw new QueryException(710, "Class attribute missing");

	    case "operation:repetition":

		if (operands.size() != 1)
		    throw new QueryException(
		        705,
			"Number of operands is not acceptable"
		    );

		int min = 0;
		int max = 100;

		if (json.has("boundary")) {
		    Boundary b = new Boundary(json.get("boundary"), 0, 100);
		    min = b.min;
		    max = b.max;
		}
		else {
		    if (json.has("min"))
			min = json.get("min").asInt(0);
		    if (json.has("max"))
			max = json.get("max").asInt(100);

		    if (DEBUG)
			log.trace(
			    "Boundary is set by deprecated {}-{}",
			    min,
			    max);
		};

		// Sanitize max
		if (max < 0)
		    max = 100;
		else if (max > 100)
		    max = 100;

		// Sanitize min
		if (min < 0)
		    min = 0;
		else if (min > 100)
		    min = 100;
		
		// Check relation between min and max
		if (min > max)
		    max = max;

		SpanQueryWrapper sqw = this.fromJson(operands.get(0));

		if (sqw.maybeExtension())
		    return sqw.setMin(min).setMax(max);

		return new SpanRepetitionQueryWrapper(sqw, min, max);

	    case "operation:relation":
		throw new QueryException(765, "Relations are currently not supported");
	    };

	    throw new QueryException(711, "Unknown group operation");

	case "korap:reference":
	    if (json.has("operation") &&
		!json.get("operation").asText().equals("operation:focus"))
		throw new QueryException(712, "Unknown reference operation");

	    if (!json.has("operands"))
		throw new QueryException(
		    766, "Peripheral references are currently not supported"
                );

	    operands = json.get("operands");

	    if (!operands.isArray())
		throw new QueryException(704, "Operation needs operand list");

	    if (operands.size() == 0)
		throw new QueryException(704, "Operation needs operand list");

	    if (operands.size() != 1)
		throw new QueryException(705, "Number of operands is not acceptable");

	    if (json.has("classRef")) {
		if (json.has("classRefOp")) {
		    throw new QueryException(
		        761,
			"Class reference operators are currently not supported"
		    );
		};

		number = json.get("classRef").get(0).asInt();


		if (number > MAX_CLASS_NUM)
		    throw new QueryException(
		        709, "Valid class numbers exceeded"
                    );
	    }
	    else if (json.has("spanRef")) {
	        JsonNode spanRef = json.get("spanRef");
            return new SpanSubspanQueryWrapper(
                    fromJson(operands.get(0)), spanRef.get(0).asInt(),
                    spanRef.get(1).asInt());
//		throw new QueryException(
//		    762,
//		    "Span references are currently not supported"
//	        );
	    };

	    if (DEBUG)
		log.trace("Wrap class reference {}", number);

	    return new SpanMatchModifyQueryWrapper(
	        this.fromJson(operands.get(0)), number
	    );

	case "korap:token":

	    // The token is empty and should be treated like []
	    if (!json.has("wrap"))
		return new SpanRepetitionQueryWrapper();

	    return this._segFromJson(json.get("wrap"));

	case "korap:span":
	    return this._termFromJson(json);
	};
	throw new QueryException(713, "Query type is not supported");
    };



    private SpanQueryWrapper _segFromJson (JsonNode json) throws QueryException {

	if (!json.has("@type"))
	    throw new QueryException(701, "JSON-LD group has no @type attribute");

	String type = json.get("@type").asText();

	if (DEBUG)
	    log.trace("Wrap new token definition by {}", type);

	switch (type) {

	case "korap:term":
	    String match = "match:eq";
	    if (json.has("match"))
		match = json.get("match").asText();

	    switch (match) {
	    case "match:ne":
		if (DEBUG)
		    log.trace("Term is negated");
		SpanSegmentQueryWrapper ssqw =
		    (SpanSegmentQueryWrapper) this._termFromJson(json);
		ssqw.makeNegative();
		return this.seg().without(ssqw);
	    case "match:eq":
		return this._termFromJson(json);
	    };

	    throw new QueryException(741, "Match relation unknown");

	case "korap:termGroup":

	    if (!json.has("operands"))
		throw new QueryException(742, "Term group needs operand list");

	    // Get operands
	    JsonNode operands = json.get("operands");

	    SpanSegmentQueryWrapper ssegqw = this.seg();

	    if (!json.has("relation"))
		throw new QueryException(743, "Term group expects a relation");

	    switch (json.get("relation").asText()) {
	    case "relation:and":

		for (JsonNode operand : operands) {
		    SpanQueryWrapper part = this._segFromJson(operand);
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
			throw new QueryException(
			    744, "Operand not supported in term group"
                        );
		    };
		};
		return ssegqw;

	    case "relation:or":

		SpanAlterQueryWrapper ssaq = new SpanAlterQueryWrapper(this.field);
		for (JsonNode operand : operands) {
		    ssaq.or(this._segFromJson(operand));
		};
		return ssaq;
	    };
	};
	throw new QueryException(745, "Token type is not supported");    
    };


    private SpanQueryWrapper _termFromJson (JsonNode json) throws QueryException {
	if (!json.has("key") || json.get("key").asText().length() < 1)
	    throw new QueryException(740, "Key definition is missing in term or span");
	    
	if (!json.has("@type"))
	    throw new QueryException(701, "JSON-LD group has no @type attribute");

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

	// No default foundry defined

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
		// TODO: THIS IS A BUG! AND SHOULD BE NAMED "SURFACE"
		layer = "s";
		break;

	    case "struct":
		layer = "s";
		break;

	    case "const":
		layer = "c";
		break;
	    };

	    if (isCaseInsensitive && isTerm) {
		if (layer.equals("s")) {
		    layer = "i";
		}
		else {
		    this.addWarning(
			767,
		        "Case insensitivity is currently not supported for this layer"
		    );
		};
	    };

	    // Ignore foundry for orth layer
	    if (layer.equals("s") || layer.equals("i"))
		value.setLength(0);

	    value.append(layer).append(':');
	};

	if (json.has("key") && json.get("key").asText().length() > 0) {
	    String key = json.get("key").asText();
	    value.append(isCaseInsensitive ? key.toLowerCase() : key);
	};

	if (json.has("value") && json.get("value").asText().length() > 0)
	    value.append(':').append(json.get("value").asText());

	// Regular expression or wildcard
	if (isTerm && json.has("type")) {
	    switch (json.get("type").asText()) {
	    case "type:regex":
		return this.seg(this.re(value.toString(), isCaseInsensitive));
	    case "type:wildcard":
		return this.seq(this.wc(value.toString(), isCaseInsensitive));
	    case "type:string":
		break;
	    default:
		this.addWarning(746, "Term type is not supported - treated as a string");
	    };
	};

	if (isTerm)
	    return this.seg(value.toString());

	if (json.has("attr"))
	    this.addWarning(
	        768,
		"Attributes are currently not supported - results may not be correct");

	return this.tag(value.toString());
    };

    /*
    public boolean hasWarning () {
	if (this.warning != null)
	    return true;
	return true;
    };

    public String getWarning () {
        return this.warning;
    };

    public void addWarning (String msg) {
	if (msg == null)
	    return;
	if (this.warning == null)
	    this.warning = msg;
	else
	    this.warning += "; " + msg;
    };

    public void setWarning (String warning) {
	this.warning = warning;
    };
    */

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

    // Create an empty segment
    public SpanRepetitionQueryWrapper empty () {
	return new SpanRepetitionQueryWrapper();
    };

    // SpanSegmentAlterQueries
    /**
     * Create a segment alternation query object.
     * @param terms[] An array of alternative terms.
     */
    public SpanAlterQueryWrapper or (SpanQueryWrapper ... terms) {
	SpanAlterQueryWrapper ssaq = new SpanAlterQueryWrapper(this.field);
	for (SpanQueryWrapper t : terms) {
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
    public SpanSequenceQueryWrapper seq (SpanQueryWrapper ... terms) {
	SpanSequenceQueryWrapper sssq = new SpanSequenceQueryWrapper(this.field);
	for (SpanQueryWrapper t : terms) {
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
	    if (t instanceof SpanQueryWrapper) {
		ssq.append((SpanQueryWrapper) t);
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
    public SpanWithinQueryWrapper within (SpanQueryWrapper element,
					  SpanQueryWrapper embedded) {
	return new SpanWithinQueryWrapper(element, embedded);
    };

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
    public SpanClassQueryWrapper _ (byte number, SpanQueryWrapper element) {
	return new SpanClassQueryWrapper(element, number);
    };

    public SpanClassQueryWrapper _ (int number, SpanQueryWrapper element) {
	return new SpanClassQueryWrapper(element, number);
    };

    public SpanClassQueryWrapper _ (short number, SpanQueryWrapper element) {
	return new SpanClassQueryWrapper(element, number);
    };

    public SpanClassQueryWrapper _ (SpanQueryWrapper element) {
	return new SpanClassQueryWrapper(element);
    };

    // MatchModify
    public SpanMatchModifyQueryWrapper shrink (byte number, SpanQueryWrapper element) {
	return new SpanMatchModifyQueryWrapper(element, number);
    };

    public SpanMatchModifyQueryWrapper shrink (int number, SpanQueryWrapper element) {
	return new SpanMatchModifyQueryWrapper(element, number);
    };

    public SpanMatchModifyQueryWrapper shrink (short number, SpanQueryWrapper element) {
	return new SpanMatchModifyQueryWrapper(element, number);
    };

    public SpanMatchModifyQueryWrapper shrink (SpanQueryWrapper element) {
	return new SpanMatchModifyQueryWrapper(element);
    };

    // Repetition
    public SpanRepetitionQueryWrapper repeat (SpanQueryWrapper element, int exact) {
	return new SpanRepetitionQueryWrapper(element, exact);
    };

    public SpanRepetitionQueryWrapper repeat (SpanQueryWrapper element, int min, int max) {
	return new SpanRepetitionQueryWrapper(element, min, max);
    };
};
