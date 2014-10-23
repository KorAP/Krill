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

  TODO: korap:reference doesn't work as expected:
  - Check with
    - focus(2:{1:[orth=der]{3:{2:[orth=Baum]}}})
    - focus(3:startswith(<s>,{3:[tt/p=ART]{1:{2:[tt/p=ADJA]{3,4}}[tt/p=NN]}}))
    - focus(3:endswith(<s>,{3:[tt/p=ART]{1:{2:[tt/p=ADJA]{3,4}}[tt/p=NN]}}))

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

    private static final int MAX_CLASS_NUM = 127;

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

	    if (!json.has("@type") ||
		!json.get("@type").asText().equals("korap:boundary")) {
		throw new QueryException("Boundary definition is not valid");
	    };

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
		log.trace("Found korap:boundary with {}:{}");
	};
    };

    public SpanQueryWrapper fromJSON (String jsonString) throws QueryException {
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
    public SpanQueryWrapper fromJSON (JsonNode json) throws QueryException {

	int number = 0;

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

	    if (DEBUG)
		log.trace("Found {} group", operation);

	    if (!json.has("operands"))
		throw new QueryException("Operation needs operand list");

	    // Get all operands
	    JsonNode operands = json.get("operands");

	    if (!operands.isArray())
		throw new QueryException("Operation needs operand list");

	    if (DEBUG)
		log.trace("Operands are {}", operands);

	    switch (operation) {

	    case "operation:or":

		SpanAlterQueryWrapper ssaq = new SpanAlterQueryWrapper(this.field);
		for (JsonNode operand : operands) {
		    ssaq.or(this.fromJSON(operand));
		};
		return ssaq;

	    case "operation:position":

		if (operands.size() != 2)
		    throw new QueryException("Operation needs exactly two operands");

		// TODO: Check for operands

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
		default:
		    throw new QueryException("Frame type unknown");
		};

		// Check for exclusion modificator
		Boolean exclude;
		if (json.has("exclude") && json.get("exclude").asBoolean())
		    throw new QueryException(
		        "Exclusion is currently not supported in position operations"
		    );

		return new SpanWithinQueryWrapper(
		    this.fromJSON(operands.get(0)),
		    this.fromJSON(operands.get(1)),
		    flag
		);

	    // TODO: This is DEPRECATED and should be communicated that way
	    case "operation:submatch":

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

			if (unit.equals("t"))
			    throw new QueryException(
			        "Text based distances are not supported yet"
                            );

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

			sseqqw.withConstraint(min, max, unit);
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

		    if (DEBUG)
			log.trace("Found Class definition for {}", json.get("class").asInt(0));

		    number = json.get("class").asInt(0);

		    if (number > MAX_CLASS_NUM)
			throw new QueryException("Class numbers limited to " + MAX_CLASS_NUM);

		    SpanQueryWrapper sqw = this.fromJSON(operands.get(0));

		    // Problematic
		    if (sqw.maybeExtension())
			return sqw.setClassNumber(number);

		    return new SpanClassQueryWrapper(sqw, number);
		};

		throw new QueryException("Class group expects class attribute");

	    case "operation:repetition":

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

		SpanQueryWrapper sqw = this.fromJSON(operands.get(0));

		if (sqw.maybeExtension())
		    return sqw.setMin(min).setMax(max);

		return new SpanRepetitionQueryWrapper(sqw, min, max);
	    };

	    throw new QueryException("Unknown group operation");

	case "korap:reference":
	    if (json.has("operation") &&
		!json.get("operation").asText().equals("operation:focus"))
		throw new QueryException("Reference operation " +
					 json.get("operation").asText() +
					 " not supported yet");

	    operands = json.get("operands");

	    if (operands.size() == 0) {
		throw new QueryException("Focus with peripheral references is not supported yet");
	    };

	    if (operands.size() != 1)
		throw new QueryException("Operation needs exactly two operands");


	    if (json.has("classRef")) {
		if (json.has("classRefOp"))
		    throw new QueryException("Class reference operators not supported yet");

		number = json.get("classRef").get(0).asInt();

		if (number > MAX_CLASS_NUM)
		    throw new QueryException("Class numbers limited to " + MAX_CLASS_NUM);

	    }
	    else if (json.has("spanRef")) {
		throw new QueryException("Span references not supported yet");
	    };

	    if (DEBUG)
		log.trace("Wrap class reference {}", number);

	    return new SpanMatchModifyQueryWrapper(
	        this.fromJSON(operands.get(0)), number
	    );

	case "korap:token":

	    // The token is empty and should be treated like []
	    if (!json.has("wrap"))
		return new SpanRepetitionQueryWrapper();

	    return this._segFromJSON(json.get("wrap"));

	case "korap:span":
	    if (!json.has("key"))
		throw new QueryException("A span needs at least a key definition");

	    return this._termFromJSON(json);
	};
	throw new QueryException("Unknown serialized query type: " + type);
    };



    private SpanQueryWrapper _segFromJSON (JsonNode json) throws QueryException {
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
		    (SpanSegmentQueryWrapper) this._termFromJSON(json);
		ssqw.makeNegative();
		return this.seg().without(ssqw);
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
		    SpanQueryWrapper part = this._segFromJSON(operand);
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



    private SpanQueryWrapper _termFromJSON (JsonNode json) throws QueryException {
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

    // split
};
