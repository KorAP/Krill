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

/**
 * @author Nils Diewald
 *
 * KorapQuery implements a simple API for wrapping
 * KorAP Index I specific query classes.
 */
public class KorapQuery {
    private String field;
    private ObjectMapper json;

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

		// temporary
		if (json.get("position").asText().equals("contains") || json.get("position").asText().equals("within")) {
		    return new SpanWithinQueryWrapper(
			this.fromJSON(json.get("operands").get(0)),
			this.fromJSON(json.get("operands").get(1))
                    );
		};
		throw new QueryException("Unknown position type "+json.get("position").asText());

	    case "shrink":
		int number = 0;
		// temporary
		if (json.has("shrink"))
		    number = json.get("shrink").asInt();

		return new SpanMatchModifyQueryWrapper(this.fromJSON(json.get("operands").get(0)), number);
	    };
	    throw new QueryException("Unknown group relation");

	case "korap:token":
	    JsonNode value = json.get("@value");
	    SpanSegmentQueryWrapper ssegqw = new SpanSegmentQueryWrapper(this.field);
	    type = value.get("@type").asText();
	    if (type.equals("korap:term")) {
		switch (value.get("relation").asText()) {
		case "=":
		    ssegqw.with(value.get("@value").asText());
		    return ssegqw;
		case "!=":
		    throw new QueryException("Term relation != not yet supported");
		};
		throw new QueryException("Unknown term relation");
	    };
	    throw new QueryException("Unknown token type");


	case "korap:sequence":
	    if (!json.has("operands"))
		throw new QueryException("SpanSequenceQuery needs operands");

	    SpanSequenceQueryWrapper sseqqw = new SpanSequenceQueryWrapper(this.field);
	    for (JsonNode operand : json.get("operands")) {
		sseqqw.append(this.fromJSON(operand));
	    };
	    return sseqqw;
	};
	throw new QueryException("Unknown serialized query type: " + type);
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
