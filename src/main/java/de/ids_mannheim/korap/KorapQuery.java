package de.ids_mannheim.korap;

import de.ids_mannheim.korap.query.wrap.*;
import de.ids_mannheim.korap.util.QueryException;

import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.util.automaton.RegExp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

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

    private String defaultFoundry = "mate/";

    // Logger
    private final static Logger log = LoggerFactory.getLogger(KorapQuery.class);

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
    // TODO: Check for the number of operands before getting them
    public SpanQueryWrapperInterface fromJSON (JsonNode json) throws QueryException {

	if (!json.has("@type")) {
	    throw new QueryException("JSON-LD group has no @type attribute");
	};

	String type = json.get("@type").asText();

	switch (type) {

	case "korap:group":
	    SpanClassQueryWrapper classWrapper;

	    if (!json.has("relation")) {
		if (json.has("class")) {
		    return new SpanClassQueryWrapper(
			this.fromJSON(json.get("operands").get(0)),
                        json.get("class").asInt(0)
                    );
		}
		throw new QueryException("Group needs a relation or a class");
	    };

	    String relation = json.get("relation").asText();

	    if (!json.has("operands"))
		throw new QueryException("Operation needs operands");

	    // Alternation
	    switch (relation) {

	    case "or":

		SpanAlterQueryWrapper ssaq = new SpanAlterQueryWrapper(this.field);
		for (JsonNode operand : json.get("operands")) {
		    ssaq.or(this.fromJSON(operand));
		};
		if (json.has("class")) {
		    return new SpanClassQueryWrapper(ssaq, json.get("class").asInt(0));
		};
		return ssaq;

	    case "position":
		if (!json.has("position"))
		    throw new QueryException("Operation needs position specification");

		String position = json.get("position").asText();
		short flag = 0;
		switch (position) {
		case "startswith":
		    flag = (short) 1;
		    break;
		case "endswith":
		    flag = (short) 2;
		    break;
		case "match":
		    flag = (short) 3;
		    break;
		};

		return new SpanWithinQueryWrapper(
		    this.fromJSON(json.get("operands").get(0)),
		    this.fromJSON(json.get("operands").get(1)),
		    flag
	        );

	    case "shrink":
		int number = 0;
		// temporary
		if (json.has("shrink"))
		    number = json.get("shrink").asInt();

		return new SpanMatchModifyQueryWrapper(this.fromJSON(json.get("operands").get(0)), number);
	    };
	    throw new QueryException("Unknown group relation");

	case "korap:token":
	    return this._segFromJSON(json.get("@value"));

	case "korap:sequence":
	    if (!json.has("operands"))
		throw new QueryException("SpanSequenceQuery needs operands");

	    JsonNode operands = json.get("operands");
	    if (!operands.isArray() || operands.size() < 2)
		throw new QueryException("SpanSequenceQuery needs operands");		
		
	    SpanSequenceQueryWrapper sseqqw = new SpanSequenceQueryWrapper(this.field);
	    for (JsonNode operand : json.get("operands")) {
		sseqqw.append(this.fromJSON(operand));
	    };
	    return sseqqw;

	case "korap:element":
	    String value = json.get("@value").asText().replace('=',':');
	    return this.tag(value);
	};
	throw new QueryException("Unknown serialized query type: " + type);
    };


    private SpanQueryWrapperInterface _segFromJSON (JsonNode json) throws QueryException {
	String type = json.get("@type").asText();
	switch (type) {

	case "korap:term":
	    switch (json.get("relation").asText()) {
	    case "=":
		String value = json.get("@value").asText();

		value = value.replaceFirst("base:", defaultFoundry +"l:").replaceFirst("orth:", "s:");
	
		if (json.has("@subtype") && json.get("@subtype").asText().equals("korap:regex")) {
		    if (value.charAt(0) == '\'' || value.charAt(0) == '"') {
			value = "s:" + value;
		    };
		    value = value.replace("'", "").replace("\"", "");

		    // Temporary
		    value = value.replace("_", "/");

		    return this.seg(this.re(value));
		};

		if (!value.matches("[^:]+?:.+"))
		    value = "s:" + value;

		// Temporary
		value = value.replace("_", "/");

		return this.seg(value);

	    case "!=":
		throw new QueryException("Term relation != not yet supported");
	    };
	    throw new QueryException("Unknown term relation");

	case "korap:group":
	    SpanSegmentQueryWrapper ssegqw = new SpanSegmentQueryWrapper(this.field);
	    switch (json.get("relation").asText()) {
	    case "and":
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
	    case "or":
		SpanAlterQueryWrapper ssaq = new SpanAlterQueryWrapper(this.field);
		for (JsonNode operand : json.get("operands")) {
		    ssaq.or(this._segFromJSON(operand));
		};
		return ssaq;
	    };
    };
    throw new QueryException("Unknown token type");    
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
