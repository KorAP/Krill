package de.ids_mannheim.korap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.ids_mannheim.korap.constants.RelationDirection;
import de.ids_mannheim.korap.query.QueryBuilder;
import de.ids_mannheim.korap.query.SpanWithinQuery;
import de.ids_mannheim.korap.query.wrap.SpanAlterQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanAttributeQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanClassQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanFocusQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanReferenceQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanRegexQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanWildcardQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanRelationWrapper;
import de.ids_mannheim.korap.query.wrap.SpanRepetitionQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanSegmentQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanSequenceQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanSimpleQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanSubspanQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanWithAttributeQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanWithinQueryWrapper;
import de.ids_mannheim.korap.response.Notifications;
import de.ids_mannheim.korap.util.QueryException;
import de.ids_mannheim.korap.util.StatusCodes;

/**
 * <p>
 * KrillQuery provides deserialization methods
 * for KoralQuery query objects.
 * </p>
 * 
 * <blockquote><pre>
 * // Create or receive a KoralQuery JSON string
 * String koral = "{\"@type\":"koral:group", ... }";
 * 
 * SpanQueryWrapper sqw = new
 * KrillQuery("tokens").fromKoral("{... JsonString ...}");
 * </pre></blockquote>
 * 
 * @author diewald
 */
/*
  TODO: Merge this with SpanQueryWrapper

  TODO: Use full-blown jsonld processor

  TODO: All queries with a final right expansion
  e.g. der alte []
  should be wrapped in a contains(<base/s=t>) to ensure
  they are not outside the text.
  
  TODO: Create Pre-filter while preparing a Query.
  The pre-filter will contain a boolena query with all
  necessary terms, supporting boolean OR, ignoring
  negation terms (and negation subqueries), like
  [base=Der]([base=alte]|[base=junge])[base=Mann & p!=ADJA]![base=war | base=lag]
  Search for all documents containing "s:Der" and ("s:alte" or "s:junge") and "s:Mann"
*/
public final class KrillQuery extends Notifications {
    private QueryBuilder builder;
    private String field;
    private JsonNode json;

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

    private static final int MAX_CLASS_NUM = 255; // 127;

    // Private class for koral:boundary objects
    private class Boundary {
        public int min, max;


        // Constructor for boundaries
        public Boundary (JsonNode json, int defaultMin, int defaultMax)
                throws QueryException {

            // No @type defined
            if (!json.has("@type")) {
                throw new QueryException(701,
                        "JSON-LD group has no @type attribute");
            };

            // Wrong @type defined
            if (!json.get("@type").asText().equals("koral:boundary"))
                throw new QueryException(702, "Boundary definition is invalid");

            // Set min boundary
            this.min = json.has("min") ? json.get("min").asInt(defaultMin)
                    : defaultMin;

            // Set max boundary
            this.max = json.has("max") ? json.get("max").asInt(defaultMax)
                    : defaultMax;

            if (DEBUG)
                log.trace("Found koral:boundary with {}:{}", min, max);
        };
    };


    /**
     * Constructs a new object for query deserialization
     * and building. Expects the name of an index field
     * to apply the query on (this should normally be
     * a token stream field).
     * 
     * @param field
     *            The specific index field for the query.
     */
    public KrillQuery (String field) {
        this.field = field;
    };


    /**
     * The index field associated with
     * the token stream.
     */
    public String getField () {
        if (field == null)
            return "tokens";
        return this.field;
    };


    /**
     * <p>Deserialize JSON-LD query to a {@link SpanQueryWrapper}
     * object.</p>
     * 
     * <blockquote><pre>
     * KrillQuery kq = new KrillQuery("tokens");
     * SpanQueryWrapper sqw = kq.fromKoral(
     * "{\"@type\" : \"koral:token\","+
     * "\"wrap\" : {" +
     * "\"@type\" : \"koral:term\"," +
     * "\"foundry\" : \"opennlp\"," +
     * "\"key\" : \"tree\"," +
     * "\"layer\" : \"orth\"," +
     * "\"match\" : \"match:eq\""+
     * "}}"
     * );
     * </pre></blockquote>
     * 
     * @param json
     *            String representing the JSON query string.
     * @return {@link SpanQueryWrapper} object.
     * @throws QueryException
     */
    public SpanQueryWrapper fromKoral (String json) throws QueryException {
        JsonNode jsonN;
        try {
            // Read Json string
            jsonN = new ObjectMapper().readValue(json, JsonNode.class);
        }

        // Something went wrong
        catch (IOException e) {
            String msg = e.getMessage();
            log.warn("Unable to parse JSON: " + msg.split("\n")[0]);
            throw new QueryException(621, "Unable to parse JSON");
        };

        // The query is nested in a parent query
        if (!jsonN.has("@type") && jsonN.has("query"))
            jsonN = jsonN.get("query");

        // Deserialize from node
        return this.fromKoral(jsonN);
    };


    /**
     * <p>Deserialize JSON-LD query as a {@link JsonNode} object
     * to a {@link SpanQueryWrapper} object.</p>
     * 
     * @param json
     *            {@link JsonNode} representing the JSON query string.
     * @return {@link SpanQueryWrapper} object.
     * @throws QueryException
     */
    // TODO: Exception messages are horrible!
    // TODO: Use the shortcuts implemented in the builder
    //       instead of the wrapper constructors
    // TODO: Rename this span context!
    public SpanQueryWrapper fromKoral (JsonNode json) throws QueryException {

        // Set this for reserialization - may be changed later on
        this.json = json;
        return this._fromKoral(json);
    };

    private SpanQueryWrapper _fromKoral (JsonNode json) throws QueryException {
        return _fromKoral(json, false);
    }
    private SpanQueryWrapper _fromKoral (JsonNode json, boolean isOperationRelation) throws QueryException {
        int number = 0;

        // Only accept @typed objects for the moment
        // TODO: Support @context for cosmas:...
        if (!json.has("@type"))
            throw new QueryException(701,
                    "JSON-LD group has no @type attribute");

        // Get @type for branching
        String type = json.get("@type").asText();

        switch (type) {
            case "koral:group":
                return this._groupFromJson(json);

            case "koral:reference":
                if (json.has("operation") && !json.get("operation").asText()
                        .equals("operation:focus"))
                    throw new QueryException(712,
                            "Unknown reference operation");

                if (!json.has("operands")) {
                    throw new QueryException(766,
                            "Peripheral references are currently not supported");
                }

                JsonNode operands = json.get("operands");

                if (!operands.isArray())
                    throw new QueryException(704,
                            "Operation needs operand list");

                if (operands.size() == 0)
                    throw new QueryException(704,
                            "Operation needs operand list");

                if (operands.size() != 1)
                    throw new QueryException(705,
                            "Number of operands is not acceptable");

                // Reference based on classes
                if (json.has("classRef")) {
                    if (json.has("classRefOp")) {
                        throw new QueryException(761,
                                "Class reference operators are currently not supported");
                    };

                    number = json.get("classRef").get(0).asInt();

                    if (number > MAX_CLASS_NUM)
                        throw new QueryException(709,
                                "Valid class numbers exceeded");
                }

                // Reference based on spans
                else if (json.has("spanRef")) {
                    JsonNode spanRef = json.get("spanRef");
                    int length = 0;
                    int startOffset = 0;
                    if (!spanRef.isArray() || spanRef.size() == 0) {
                        throw new QueryException(714,
                                "Span references expect a start position"
                                        + " and a length parameter");
                    };

                    if (spanRef.size() > 1)
                        length = spanRef.get(1).asInt(0);

                    startOffset = spanRef.get(0).asInt(0);

                    if (DEBUG)
                        log.trace("Wrap span reference {},{}", startOffset,
                                length);

                    SpanQueryWrapper sqw = this._fromKoral(operands.get(0));
                    SpanSubspanQueryWrapper ssqw = new SpanSubspanQueryWrapper(
                            sqw, startOffset, length);
                    return ssqw;
                };

                if (DEBUG)
                    log.trace("Wrap class reference {}", number);

                return new SpanFocusQueryWrapper(
                        this._fromKoral(operands.get(0)), number);

            case "koral:token":

                // The token is empty and should be treated like []
                if (!json.has("wrap"))
                    return new SpanRepetitionQueryWrapper();

                // Workaround so that "attr" can be wrapped (legacy) and be
                // next to wrap as well (spec)

                // Term has attribute
                if (json.has("attr")) {

                    JsonNode wrap = json.get("wrap");
                    JsonNode attrNode = json.get("attr");
                    if (wrap.has("foundry")) {
                        ((ObjectNode)attrNode).set("foundry", wrap.get("foundry"));
                    };
                    if (wrap.has("layer")) {
                        ((ObjectNode)attrNode).set("layer", wrap.get("layer"));
                    };
                    ((ObjectNode)json.get("wrap")).set("attr",attrNode);
                };
                
// Get wrapped token
                return this._segFromJson(json.get("wrap"));

            case "koral:span":                
                // EM: what to do with empty koral:span? 
                // it is allowed only in relation queries
                if (isOperationRelation && !json.has("key") && !json.has("wrap") && !json.has("attr")) {
                    return new SpanRepetitionQueryWrapper();
                }
                if (!json.has("wrap"))
                    return this._termFromJson(json);

                // Workaround so that "attr" can be wrapped (legacy) and be
                // next to wrap as well (spec)

                // Term has attribute
                if (json.has("attr")) {

                    JsonNode wrap = json.get("wrap");
                    JsonNode attrNode = json.get("attr");
                    if (wrap.has("foundry")) {
                        ((ObjectNode)attrNode).set("foundry", wrap.get("foundry"));
                    };
                    if (wrap.has("layer")) {
                        ((ObjectNode)attrNode).set("layer", wrap.get("layer"));
                    };
                    ((ObjectNode)json.get("wrap")).set("attr",attrNode);
                };
                
                // This is an ugly hack
                return this._termFromJson(json.get("wrap"), true);
        };

        // Unknown query type
        throw new QueryException(713, "Query type is not supported");
    };


    /**
     * <p>
     * Get the associated {@link QueryBuilder} object
     * for query building.
     * </p>
     * 
     * <blockquote><pre>
     * SpanQueryWrapper query = new
     * KrillQuery("tokens").builder().re("mate/p=N.*");
     * </pre></blockquote>
     * 
     * @return The {@link QueryBuilder}.
     */
    public QueryBuilder builder () {
        if (this.builder == null)
            this.builder = new QueryBuilder(this.field);
        return this.builder;
    };


    /**
     * Return the associated KoralQuery query object
     * as a {@link JsonNode}. This won't work,
     * if the object was build using a {@link QueryBuilder},
     * therefore it is limited to mirror a deserialized KoralQuery
     * object.
     * 
     * @return The {@link JsonNode} representing the query object
     *         of a deserialized KoralQuery object.
     */
    public JsonNode toJsonNode () {
        return this.json;
    };


    /**
     * Return the associated KoralQuery query object
     * as a JSON string. This won't work,
     * if the object was build using a {@link QueryBuilder},
     * therefore it is limited to mirror a deserialized KoralQuery
     * object.
     * 
     * @return A JSON string representing the query object
     *         of a deserialized KoralQuery object.
     */
    public String toJsonString () {
        if (this.json == null)
            return "{}";
        return this.json.toString();
    };


    // Deserialize koral:group
    private SpanQueryWrapper _groupFromJson (JsonNode json)
            throws QueryException {

        // No operation
        if (!json.has("operation"))
            throw new QueryException(703, "Group expects operation");

        // Get operation
        String operation = json.get("operation").asText();

        if (DEBUG)
            log.trace("Found {} group", operation);

        if (!json.has("operands"))
            throw new QueryException(704, "Operation needs operand list");

        // Get all operands
        JsonNode operands = json.get("operands");

        if (operands == null || !operands.isArray())
            throw new QueryException(704, "Operation needs operand list");

        if (DEBUG)
            log.trace("Operands are {}", operands);

        SpanQueryWrapper spanReferenceQueryWrapper = _operationReferenceFromJSON(
                json, operands);
        if (spanReferenceQueryWrapper != null) {
            return spanReferenceQueryWrapper;
        }

        // Branch on operation
        switch (operation) {
            case "operation:junction":
                return this._operationJunctionFromJson(operands);

            case "operation:position":
                return this._operationPositionFromJson(json, operands);

            case "operation:sequence":
                return this._operationSequenceFromJson(json, operands);

            case "operation:class":
                return this._operationClassFromJson(json, operands);

            case "operation:repetition":
                return this._operationRepetitionFromJson(json, operands);

            case "operation:relation":
                // if (!json.has("relType")) {
                // throw new QueryException(717,
                // "Missing relation node");
                // }
                if (json.has("relType"))
                    return _operationRelationFromJson(operands,
                            json.get("relType"));
                // EM: legacy
                else if (json.has("relation")) {
                    return _operationRelationFromJson(operands,
                            json.get("relation"));
                }
                else {
                    throw new QueryException(717, "Missing relation node");
                }
                /*throw new QueryException(765,
                  "Relations are currently not supported");*/

                // Gracefully warn on merge support
            case "operation:merge":
                this.addWarning(774,
                        "Merge operation is currently not supported");
                return _fromKoral(operands.get(0));

            // Deprecated in favor of operation:junction
            case "operation:or":
                return this._operationJunctionFromJson(operands);
            /*
              case "operation:submatch": // Deprecated in favor of koral:reference
              return this._operationSubmatchFromJson(json, operands);
            */
            case "operation:disjunction":
                return this._operationJunctionFromJson(operands);
        };

        // Unknown
        throw new QueryException(711, "Unknown group operation");
    };


    private SpanQueryWrapper _operationReferenceFromJSON (JsonNode node,
            JsonNode operands) throws QueryException {
        boolean isReference = false;
        int classNum = -1;
        int refOperandNum = -1;
        JsonNode childNode;

        for (int i = 0; i < operands.size(); i++) {
            childNode = operands.get(i);
            if (childNode.has("@type")
                    && childNode.get("@type").asText().equals("koral:reference")
                    && childNode.has("operation") && childNode.get("operation")
                            .asText().equals("operation:focus")
                    && !childNode.has("operands")) {

                if (childNode.has("classRef")) {
                    classNum = childNode.get("classRef").get(0).asInt();
                    refOperandNum = i;
                    isReference = true;
                    break;
                }
            }
        }

        if (isReference) {
            JsonNode resolvedNode = _resolveReference(node, operands,
                    refOperandNum, classNum);
            SpanQueryWrapper queryWrapper = this._fromKoral(resolvedNode);
            return new SpanReferenceQueryWrapper(queryWrapper,classNum);
        }

        return null;
    }


    private JsonNode _resolveReference (JsonNode node, JsonNode operands,
            int refOperandNum, int classNum) throws QueryException {
        JsonNode referent = null;
        ObjectMapper m = new ObjectMapper();
        ArrayNode newOperands = m.createArrayNode();
        boolean isReferentFound = false;
        for (int i = 0; i < operands.size(); i++) {
            if (i != refOperandNum) {
                if (!isReferentFound) {
                    referent = _extractReferentClass(operands.get(i), classNum);
                    if (referent != null)
                        isReferentFound = true;
                }
                newOperands.insert(i, operands.get(i));
            }
        }

        if (isReferentFound) {
            newOperands.insert(refOperandNum, referent);
            ((ObjectNode) node).set("operands", newOperands);
            return node;
        }
        else
            throw new QueryException("Referent node is not found");

    }


    private JsonNode _extractReferentClass (JsonNode node, int classNum) {
        JsonNode referent;
        if (node.has("classOut") && node.get("classOut").asInt() == classNum) {
            return node;
        }
        else {
            if (node.has("operands") && node.get("operands").isArray()) {
                for (JsonNode childOperand : node.get("operands")) {
                    referent = _extractReferentClass(childOperand, classNum);
                    if (referent != null) {
                        return referent;
                    }
                }
            }
        }
        return null;
    }


    private SpanQueryWrapper _operationRelationFromJson (JsonNode operands,
            JsonNode relation) throws QueryException {

        if (operands.size() < 2) {
            throw new QueryException(705,
                    "Number of operands is not acceptable");
        }

        SpanQueryWrapper operand1 = this._fromKoral(operands.get(0), true);
        SpanQueryWrapper operand2 = this._fromKoral(operands.get(1), true);

        RelationDirection direction;
        if (operand1.isEmpty() && !operand2.isEmpty()) {
            direction = RelationDirection.LEFT; // "<:";
        }
        else{
            direction = RelationDirection.RIGHT; // ">:"
        }

        if (!relation.has("@type")){
            throw new QueryException(701,
                    "JSON-LD group has no @type attribute");
        }
        
        if (relation.get("@type").asText().equals("koral:relation")) {
            SpanRelationWrapper spanRelationWrapper;
            SpanQueryWrapper relationTermWrapper;
            if (!relation.has("wrap")) {
                throw new QueryException(718, "Missing relation term.");
            }
            else{
                relationTermWrapper =
                    _termFromJson(relation.get("wrap"), false, false, direction);
                spanRelationWrapper = new SpanRelationWrapper(relationTermWrapper, operand1, operand2);
            }
            
            spanRelationWrapper.setDirection(direction);    
            return spanRelationWrapper;
        }
        else {
            throw new QueryException(713, "Query type is not supported");
        }
        
//        if (relation.has("boundary")){
//            _operationRepetitionFromJson(relation, operands);
//        }
//        else{
//            
//        }
    }


    // Deserialize operation:junction
    private SpanQueryWrapper _operationJunctionFromJson (JsonNode operands)
            throws QueryException {
        SpanAlterQueryWrapper ssaq = new SpanAlterQueryWrapper(this.field);
        for (JsonNode operand : operands) {
            ssaq.or(this._fromKoral(operand));
        };
        return ssaq;
    };


    // Deserialize operation:position
    private SpanQueryWrapper _operationPositionFromJson (JsonNode json,
            JsonNode operands) throws QueryException {
        if (operands.size() != 2)
            throw new QueryException(705,
                    "Number of operands is not acceptable");

        String frame = "contains";
        // Temporary workaround for wrongly set overlaps
        if (json.has("frames")) {
            JsonNode frameN = json.get("frames");
            if (frameN.isArray()) {

                // Treat the following temporarilly as "contains"
                // "frames:matches",
                // "frames:startsWith",
                // "frames:endsWith",
                // "frames:isAround"
                // This ignores nonunique frames

                int fs = 0;
                for (JsonNode frameS : frameN) {
                    switch (frameS.asText()) {
                    case "frames:matches":
                        fs++;
                        break;
                    case "frames:startsWith":
                        fs++;
                        break;
                    case "frames:isAround":
                        fs++;
                        break;
                    case "frames:endsWith":
                        fs++;
                        break;
                    default:
                        fs+=7;
                    }
                };

                
                if (fs == 4) {
                    frame = "contains";
                } else {
                    if (frameN.size() > 1) {
                        this.addMessage(0, "Frames not fully supported yet");
                    };
                    
                    frameN = frameN.get(0);
                    if (frameN != null && frameN.isValueNode())
                        frame = frameN.asText().substring(7);
                };
            };
        }
        // <legacyCode>
        else if (json.has("frame")) {
            this.addMessage(0, "Frame is deprecated");

            JsonNode frameN = json.get("frame");
            if (frameN != null && frameN.isValueNode())
                frame = frameN.asText().substring(6);
        };
        // </legacyCode>

        if (DEBUG)
            log.trace("Position frame is '{}'", frame);

        // Byte flag - should cover all 13 cases, i.e. two bytes long
        byte flag = WITHIN;
        JsonNode operand;
        switch (frame) {
            case "contains":
                operand = operands.get(0);
                if (operand.get("@type").asText().equals("koral:token")){
                    throw new QueryException(StatusCodes.INVALID_QUERY, 
                            "Token cannot contain another token or element.");
                }
                break;
            case "isAround":
                operand = operands.get(0);
                if (operand.get("@type").asText().equals("koral:token")){
                    throw new QueryException(StatusCodes.INVALID_QUERY, 
                            "Token cannot contain another token or element.");
                }
                this.addMessage(
                    0,
                    "'isAround' will have a different meaning in the future and is therefore temporarily deprecated in favor of 'contains'"
                    );

                break;
            case "strictlyContains":
                flag = REAL_WITHIN;
                break;
            case "isWithin":
                break;
            case "startsWith":
                flag = STARTSWITH;
                break;
            case "endsWith":
                flag = ENDSWITH;
                break;
            case "matches":
                flag = MATCH;
                break;
            case "overlaps":
                flag = OVERLAP;
                this.addWarning(769,
                        "Overlap variant currently interpreted as overlap");
                break;
            case "overlapsLeft":
                // Temporary workaround
                this.addWarning(769,
                        "Overlap variant currently interpreted as overlap");
                flag = OVERLAP;
                break;
            case "overlapsRight":
                // Temporary workaround
                this.addWarning(769,
                        "Overlap variant currently interpreted as overlap");
                flag = OVERLAP;
                break;
            case "strictlyOverlaps":
                flag = REAL_OVERLAP;
                break;

            // alignsLeft

            default:
                throw new QueryException(706, "Frame type is unknown");
        };

        
        // <legacyCode>
        Boolean exclude;
        if (json.has("exclude") && json.get("exclude").asBoolean()) {
            throw new QueryException(760,
                    "Exclusion is currently not supported in position operations");
        };
        // </legacyCode>

        // Create SpanWithin Query
        return new SpanWithinQueryWrapper(this._fromKoral(operands.get(0)),
                this._fromKoral(operands.get(1)), flag);
    };


    // Deserialize operation:repetition
    private SpanQueryWrapper _operationRepetitionFromJson (JsonNode json,
            JsonNode operands) throws QueryException {

        if (operands.size() != 1)
            throw new QueryException(705,
                    "Number of operands is not acceptable");

        int min = 0, max = 100;

        if (json.has("boundary")) {
            Boundary b = new Boundary(json.get("boundary"), 0, 100);
            min = b.min;
            max = b.max;
        }
        // <legacyCode>
        else {
            this.addMessage(0, "Setting boundary by min and max is deprecated");

            // Set minimum value
            if (json.has("min"))
                min = json.get("min").asInt(0);

            // Set maximum value
            if (json.has("max"))
                max = json.get("max").asInt(100);
        };
        // </legacyCode>

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

        SpanQueryWrapper sqw = this._fromKoral(operands.get(0));

        if (sqw.maybeExtension())
            return sqw.setMin(min).setMax(max);

        return new SpanRepetitionQueryWrapper(sqw, min, max);
    };


    // Deserialize operation:submatch
    @Deprecated
    private SpanQueryWrapper _operationSubmatchFromJson (JsonNode json,
            JsonNode operands) throws QueryException {

        int number = 1;

        this.addMessage(0, "operation:submatch is deprecated");

        if (operands.size() != 1)
            throw new QueryException(705,
                    "Number of operands is not acceptable");

        // Use class reference
        if (json.has("classRef")) {
            if (json.has("classRefOp")) {
                throw new QueryException(761,
                        "Class reference operators are currently not supported");
            };

            number = json.get("classRef").get(0).asInt();
        }

        // Use span reference
        else if (json.has("spanRef")) {
            throw new QueryException(762,
                    "Span references are currently not supported");
        };

        return new SpanFocusQueryWrapper(this._fromKoral(operands.get(0)),
                number);
    };


    // Deserialize operation:class
    private SpanQueryWrapper _operationClassFromJson (JsonNode json,
            JsonNode operands) throws QueryException {
        int number = 1;

        // Too many operands
        if (operands.size() != 1)
            throw new QueryException(705,
                    "Number of operands is not acceptable");

        // Get class number
        if (json.has("classOut")) {
            number = json.get("classOut").asInt(0);
        }
        // <legacyCode>
        else if (json.has("class")) {
            this.addMessage(0, "Class is deprecated");
            number = json.get("class").asInt(0);
        };
        // </legacyCode>

        // Class reference check
        if (json.has("classRefCheck")) {
            this.addWarning(764, "Class reference checks are currently "
                    + "not supported - results may not be correct");
        };

        // Class reference operation
        // This has to be done after class ref check
        if (json.has("classRefOp")) {
            throw new QueryException(761,
                    "Class reference operators are currently not supported");
        };

        // Number is set
        if (number > 0) {
            if (operands.size() != 1) {
                throw new QueryException(705,
                        "Number of operands is not acceptable");
            };

            if (DEBUG)
                log.trace("Found Class definition for {}", number);

            if (number > MAX_CLASS_NUM) {
                throw new QueryException(709, "Valid class numbers exceeded");
            };

            // Serialize operand
            SpanQueryWrapper sqw = this._fromKoral(operands.get(0));

            // Problematic
            if (sqw.maybeExtension())
                return sqw.setClassNumber(number);

            return new SpanClassQueryWrapper(sqw, number);
        };

        throw new QueryException(710, "Class attribute missing");
    };


    // Deserialize operation:sequence
    private SpanQueryWrapper _operationSequenceFromJson (JsonNode json,
            JsonNode operands) throws QueryException {

        // Sequence with only one operand
        if (operands.size() == 1)
            return this._fromKoral(operands.get(0));

        SpanSequenceQueryWrapper sseqqw = this.builder().seq();

        // Say if the operand order is important
        if (json.has("inOrder"))
            sseqqw.setInOrder(json.get("inOrder").asBoolean());

        // Introduce distance constraints
        // ATTENTION: Distances have to be set before segments are added
        if (json.has("distances")) {

            // THIS IS NO LONGER NECESSARY, AS IT IS COVERED BY FRAMES
            if (json.has("exclude") && json.get("exclude").asBoolean()) {
                throw new QueryException(763,
                        "Excluding distance constraints are currently not supported");
            };

            if (!json.get("distances").isArray()) {
                throw new QueryException(707,
                        "Distance Constraints have to be defined as arrays");
            };

            // TEMPORARY: Workaround for group distances
            JsonNode firstDistance = json.get("distances").get(0);

            if (!firstDistance.has("@type")) {
                throw new QueryException(701,
                        "JSON-LD group has no @type attribute");
            };

            JsonNode distances;
            if (firstDistance.get("@type").asText().equals("koral:group")) {
                if (!firstDistance.has("operands")
                        || !firstDistance.get("operands").isArray())
                    throw new QueryException(704,
                            "Operation needs operand list");

                distances = firstDistance.get("operands");
            }

            // Support korap distances
            // TODO: Support cosmas distances
            else if (firstDistance.get("@type").asText()
                    .equals("koral:distance")
                    || firstDistance.get("@type").asText()
                            .equals("cosmas:distance")) {
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
                    Boundary b = new Boundary(constraint.get("boundary"), 0,
                            100);
                    min = b.min;
                    max = b.max;
                }

                // <legacy>
                else {
                    if (constraint.has("min"))
                        min = constraint.get("min").asInt(0);
                    if (constraint.has("max"))
                        max = constraint.get("max").asInt(100);
                };
                // </legacy>

                // Add foundry and layer to the unit for new indices
                if (constraint.has("foundry") && constraint.has("layer")
                        && constraint.get("foundry").asText().length() > 0
                        && constraint.get("layer").asText().length() > 0) {

                    StringBuilder value = new StringBuilder();
                    value.append(constraint.get("foundry").asText());
                    value.append('/');
                    value.append(constraint.get("layer").asText());
                    value.append(':').append(unit);
                    unit = value.toString();
                }

                // Use default foundry and layer - currently only base is supported!
                else if (unit.equals("s") || unit.equals("p")
                        || unit.equals("t")) {
                    StringBuilder value = new StringBuilder();
                    unit = value.append("base/s:").append(unit).toString();
                };

                // Workaround for koral:distance vs cosmas:distance
                if (constraint.get("@type").asText().equals("koral:distance")) {
                    min++;
                    max++;
                };

                // Set distance exclusion
                Boolean exclusion = false;
                if (constraint.has("exclude"))
                    exclusion = constraint.get("exclude").asBoolean();

                // Sanitize boundary
                if (max < min)
                    max = min;

                if (DEBUG)
                    log.trace("Add distance constraint of '{}': {}-{}", unit,
                            min, max);

                sseqqw.withConstraint(min, max, unit, exclusion);
            };
        };

        // Add segments to sequence
        for (JsonNode operand : operands) {
            sseqqw.append(this._fromKoral(operand));
        };

        // inOrder was set to false without a distance constraint
        if (!sseqqw.isInOrder() && !sseqqw.hasConstraints()) {
            if (DEBUG)
                log.trace(
                        "Add distance constraint - for the normal inorder case");

            sseqqw.withConstraint(1, 1, "w");
        };

        return sseqqw;
    };

    // Deserialize koral:token
    private SpanQueryWrapper _segFromJson (JsonNode json)
            throws QueryException {

        if (!json.has("@type"))
            throw new QueryException(701,
                    "JSON-LD group has no @type attribute");

        String type = json.get("@type").asText();

        if (DEBUG)
            log.trace("Wrap new token definition by {}", type);

        // Branch on type
        switch (type) {
            case "koral:term":
                //            String match = "match:eq";
                //            if (json.has("match"))
                //                match = json.get("match").asText();
                //            
                //            switch (match) {
                //
                //            case "match:ne":
                //                if (DEBUG)
                //                    log.trace("Term is negated");
                //
                //                SpanSegmentQueryWrapper ssqw =
                //                    (SpanSegmentQueryWrapper) this._termFromJson(json);
                //
                //                ssqw.makeNegative();
                //
                //                return this.seg().without(ssqw);
                //
                //            case "match:eq":
                return this._termFromJson(json);
            //            };
            //
            //            throw new QueryException(741, "Match relation unknown");

            case "koral:termGroup":

                if (!json.has("operands"))
                    throw new QueryException(742,
                            "Term group needs operand list");

                // Get operands
                JsonNode operands = json.get("operands");

                SpanSegmentQueryWrapper ssegqw = this.builder().seg();

                if (!json.has("relation"))
                    throw new QueryException(743,
                            "Term group expects a relation");

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
                                throw new QueryException(744,
                                        "Operand not supported in term group");
                            };
                        };
                        return ssegqw;

                    case "relation:or":

                        SpanAlterQueryWrapper ssaq = new SpanAlterQueryWrapper(
                                this.field);
                        for (JsonNode operand : operands) {
                            ssaq.or(this._segFromJson(operand));
                        };
                        return ssaq;
                };
        };
        throw new QueryException(745, "Token type is not supported");
    };


    private SpanQueryWrapper _termFromJson (JsonNode json)
            throws QueryException {
        return this._termFromJson(json, false, false, null);
    }
    private SpanQueryWrapper _termFromJson (JsonNode json, boolean isSpan)
            throws QueryException {
        return this._termFromJson(json, isSpan, false, null);
    }

    // Attribute term
    private SpanQueryWrapper _termFromJson (JsonNode json, boolean isSpan, boolean isAttr)
            throws QueryException {
        return this._termFromJson(json, isSpan, true, null);
    }
    
    // Deserialize koral:term
    // TODO: Not optimal as it does not respect non-term
    private SpanQueryWrapper _termFromJson (JsonNode json, boolean isSpan, boolean isAttr, RelationDirection direction)
            throws QueryException {

        if (!json.has("@type")) {
            throw new QueryException(701,
                    "JSON-LD group has no @type attribute");
        };
        
        String termType = json.get("@type").asText();
        
        Boolean isTerm = termType.equals("koral:term") ? true
                : false;
        Boolean isCaseInsensitive = false;
       
        if (!json.has("key") ||
            (json.get("key").size() == 1 && json.get("key").asText().length() < 1)) {
            
            // It may have no key but an attribute
            if (!json.has("attr")) {
//                return new SpanRepetitionQueryWrapper();
                throw new QueryException(740,
                        "Key definition is missing in term or span");
            }
        };

        
        // Term is represented as a list of keys
        LinkedList<String> keys = new LinkedList<String>();

        if (json.has("key")) {
            if (json.get("key").size() > 1) {
                for (JsonNode value : json.get("key")) {
                    keys.push(value.asText());
                }
            } else {
                keys.push(json.get("key").asText());
            }
        }
        
        // Empty koral:span hack
        if (isSpan) {
            isTerm = false;
        };

        // <legacy>
        if (json.has("caseInsensitive")
                && json.get("caseInsensitive").asBoolean()) {
            isCaseInsensitive = true;
        }
        // </legacy>

        // Flags
        else if (json.has("flags") && json.get("flags").isArray()) {
            Iterator<JsonNode> flags = json.get("flags").elements();
            while (flags.hasNext()) {
                String flag = flags.next().asText();
                if (flag.equals("flags:caseInsensitive")) {
                    isCaseInsensitive = true;
                }
                else {
                    this.addWarning(748, "Flag is unknown", flag);
                };
            };
        };

        // expect orth? expect lemma? 
        // s:den | i:den | cnx/l:die | mate/m:mood:ind | cnx/syn:@PREMOD |
        // mate/m:number:sg | opennlp/p:ART

        StringBuilder value = new StringBuilder();
        LinkedList<String> values = new LinkedList<String>();

        if (direction != null)
            value.append(direction.value());
        else if (isAttr)
            value.append("@:");
        
        if (json.has("foundry") &&
            json.get("foundry").asText().length() > 0) {
            value.append(json.get("foundry").asText()).append('/');
        };


        // No default foundry defined
        if (json.has("layer") &&
            json.get("layer").asText().length() > 0) {
            String layer = json.get("layer").asText();
            switch (layer) {

            case "lemma":
                layer = "l";
                break;

            case "pos":
                layer = "p";
                break;

            case "orth":
                // TODO:
                //   THIS IS AN UGLY HACK!
                //   AND SHOULD BE NAMED "SURFACE" or . OR *
                layer = ".";
                break;

            case "struct":
                layer = "s";
                break;

            case "const":
                layer = "c";
                break;
            };

            if (isCaseInsensitive && isTerm) {
                if (layer.equals("."))
                    layer = "i";
                else {
                    this.addWarning(767,
                                    "Case insensitivity is currently not supported for this layer");
                };
            };

            // Ignore foundry for orth layer
            if (layer.equals(".")) {
                layer = "s";
                value.setLength(0);
            }
            else if (layer.equals("i")) {
                value.setLength(0);
            };

            value.append(layer).append(':');
        };

        // Remember the common prefix for all values
        int offset = value.length();

        // Iterate over all keys
        for (String key : keys) {

            // Reset to common prefix
            value.setLength(offset);

            // Add key to value

            if (isCaseInsensitive) {

                // This supports both legacy search and locale-dependent case-folding.
                // It mimics the Perl fc behaviour probably better than icu4j.
                if (key.toLowerCase().equals(key.toUpperCase().toLowerCase())) {
                    value.append(key.toLowerCase());
                } else {
                    value.append(key.toLowerCase());
                    values.push(value.toString());
                    value.setLength(offset);
                    value.append(key.toUpperCase().toLowerCase());
                };
            } else {
                value.append(key);
            };

            // TODO:
            //   This should iterate over all values as well
            if (json.has("value") && json.get("value").asText().length() > 0)
                value.append(':').append(json.get("value").asText());

            // Add to value list
            values.push(value.toString());
        };

        // Regular expression or wildcard
        if (isTerm) {

            // Create alter query
            SpanAlterQueryWrapper saqw = new SpanAlterQueryWrapper(this.field);
            
			String match = "match:eq";
			if (json.has("match")) {
				match = json.get("match").asText();
			};

			if (json.has("type")) {
				QueryBuilder qb = this.builder();

				// Branch on type
				switch (json.get("type").asText()) {
                case "type:regex": {

                    for (String v : values) {
                        
                        // The regex can be rewritten to an any token
                        if (v.matches("^[si]:\\.[\\+\\*]\\??$")) {
                            return new SpanRepetitionQueryWrapper();
                        };

                        SpanRegexQueryWrapper srqw = qb.re(v, isCaseInsensitive);

                        if (srqw.error != null) {
                            throw new QueryException(
                                StatusCodes.INVALID_QUERY,
                                "Invalid regex"
                                );
                        };
                        
                        saqw.or(srqw);
                    };

					if (match.equals("match:ne")) {
						if (DEBUG)
							log.trace("Term is negated");
                        saqw.setNegative(true);
						return saqw;
					}
					else if (match.equals("match:eq")) {
						return saqw;
					}
					throw new QueryException(741, "Match relation unknown");
                }
                case "type:wildcard": {

                    // Wildcard queries are deprecated in KoralQuery since 9/2017
                    
                    for (String v : values) {
                        saqw.or(qb.wc(v, isCaseInsensitive));
                    };

					if (match.equals("match:ne")) {
						if (DEBUG)
							log.trace("Term is negated");
                        saqw.setNegative(true);
						return saqw;
					}
					else if (match.equals("match:eq")) {
						return saqw;
					};
					throw new QueryException(741, "Match relation unknown");
				}
                case "type:string":
                    break;
					
                default:
                    this.addWarning(746,
									"Term type is not supported - treated as a string");
				};
			};

            // TODO:
            //   This could alternatively use
            //   https://github.com/tokuhirom/regexp-trie
            for (String v : values) {
                saqw.or(v);
            };
                
			if (match.equals("match:ne")) {                
				if (DEBUG)
					log.trace("Term is negated");

                // Segment "without" doesn't work in
                // attribute contexts
                saqw.setNegative(true);
				return saqw;
			}
			else if (match.equals("match:eq")) {
				return saqw;
			}
			else {
				throw new QueryException(741, "Match relation unknown");
			}
		};

        if (values.size() > 1) {
            throw new QueryException(
                0,
                "List representation for spans not yet supported"
                );
            
        };

        // Term has attribute
        if (json.has("attr")) {
            JsonNode attrNode = json.get("attr");
            if (!attrNode.has("@type")) {
                throw new QueryException(701,
                        "JSON-LD group has no @type attribute");
            }

            if (value.toString().isEmpty()) {
                return _createElementAttrFromJson(null, json, attrNode);
                // this.addWarning(771,
                // "Arbitraty elements with attributes are currently not supported.");
            }
            else {
                SpanQueryWrapper elementWithIdWrapper = this.builder()
                        .tag(value.toString());
                if (elementWithIdWrapper == null) {
                    return null;
                }
                return _createElementAttrFromJson(elementWithIdWrapper, json,
                        attrNode);
            }
        };
        return this.builder().tag(value.toString());
    };


    // Deserialize elements with attributes
    private SpanQueryWrapper _createElementAttrFromJson (
            SpanQueryWrapper elementWithIdWrapper, JsonNode json,
            JsonNode attrNode) throws QueryException {
        
        if (attrNode.get("@type").asText().equals("koral:term")) {
            SpanQueryWrapper attrWrapper = _attrFromJson(json.get("attr"));

            if (attrWrapper != null) {
                if (elementWithIdWrapper != null) {                  
                    return new SpanWithAttributeQueryWrapper(
                            elementWithIdWrapper, attrWrapper);
                }
                else {
                    return new SpanWithAttributeQueryWrapper(attrWrapper);
                }
            }
            else {
                throw new QueryException(747, "Attribute is null");
            }
        }
        else if (attrNode.get("@type").asText().equals("koral:termGroup")) {
            return _handleAttrGroup(elementWithIdWrapper, attrNode);
        }
        else {
            this.addWarning(715, "Attribute type is not supported");
        }
        return elementWithIdWrapper;
    }


    // Deserialize attribute groups
    private SpanQueryWrapper _handleAttrGroup (
            SpanQueryWrapper elementWithIdWrapper, JsonNode attrNode)
            throws QueryException {
        if (!attrNode.has("relation")) {
            throw new QueryException(743, "Term group expects a relation");
        }
        if (!attrNode.has("operands")) {
            throw new QueryException(742, "Term group needs operand list");
        }

        String relation = attrNode.get("relation").asText();
        JsonNode operands = attrNode.get("operands");

        SpanQueryWrapper attrWrapper;
        if ("relation:and".equals(relation)) {
            List<SpanQueryWrapper> wrapperList = new ArrayList<SpanQueryWrapper>();
            for (JsonNode operand : operands) {
                attrWrapper = _termFromJson(operand, false, true);
                if (attrWrapper == null) {
                    throw new QueryException(747, "Attribute is null");
                }
                wrapperList.add(attrWrapper);
            }

            if (elementWithIdWrapper != null) {
                return new SpanWithAttributeQueryWrapper(elementWithIdWrapper,
                        wrapperList);
            }
            else {
                return new SpanWithAttributeQueryWrapper(wrapperList);
            }
        }
        else if ("relation:or".equals(relation)) {
            SpanAlterQueryWrapper saq = new SpanAlterQueryWrapper(field);
            SpanWithAttributeQueryWrapper saqw;
            for (JsonNode operand : operands) {
                attrWrapper = _termFromJson(operand, false, true);
                if (attrWrapper == null) {
                    throw new QueryException(747, "Attribute is null");
                }
                if (elementWithIdWrapper != null) {
                    saqw = new SpanWithAttributeQueryWrapper(
                            elementWithIdWrapper, attrWrapper);
                }
                else {
                    saqw = new SpanWithAttributeQueryWrapper(attrWrapper);
                }
                saq.or(saqw);
            }
            return saq;
        }
        else {
            throw new QueryException(716, "Unknown relation");
        }
    }


    // Get attributes from a json termgroup
    private SpanQueryWrapper _attrFromJson (JsonNode attrNode)
            throws QueryException {

        if (attrNode.has("key")) {
            return _termFromJson(attrNode, false, true);
        }
        else if (attrNode.has("tokenarity") || attrNode.has("arity")) {
            this.addWarning(770, "Arity attributes are currently not supported"
                    + " - results may not be correct");
        }
        else if (attrNode.has("root")) {
            String rootValue = attrNode.get("root").asText();
            if (rootValue.equals("true") || rootValue.equals("false")) {

                return new SpanAttributeQueryWrapper(new SpanSimpleQueryWrapper(
                        this.field, "@root", Boolean.valueOf(rootValue)));
            }
        }
        return null;
    };
};
