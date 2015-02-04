package de.ids_mannheim.korap;

import de.ids_mannheim.korap.collection.BooleanFilter;
import de.ids_mannheim.korap.collection.RegexFilter;
import de.ids_mannheim.korap.util.QueryException;
import de.ids_mannheim.korap.util.KorapDate;

import org.apache.lucene.search.Query;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/*
  Todo: WildCardFilter!
  Support: delete boolean etc.
  Support: supports foundries
*/

/**
 * @author diewald
 *
 * KorapFilter implements a simple API for creating meta queries
 * constituing Virtual Collections.
 */
public class KorapFilter {
    private BooleanFilter filter;

    // Logger
    private final static Logger log = LoggerFactory.getLogger(KorapFilter.class);

    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;
    
    public KorapFilter () {
        filter = new BooleanFilter();
    };

    public KorapFilter (JsonNode json) throws QueryException {
        filter = this.fromJSON(json, "tokens");
    };

    protected BooleanFilter fromJSON (JsonNode json, String field) throws QueryException {
        BooleanFilter bfilter = new BooleanFilter();

        // TODO: THIS UNFORTUNATELY BREAKS TESTS
        if (!json.has("@type"))
            throw new QueryException(701, "JSON-LD group has no @type attribute");

        String type = json.get("@type").asText();
        
        // Single filter
        if (type.equals("korap:doc")) {

            String key     = "tokens";
            String valtype = "type:string";
            String match   = "match:eq";

            if (json.has("key"))
                key = json.get("key").asText();
            
            if (json.has("type"))
                valtype = json.get("type").asText();

            // Filter based on date
            if (valtype.equals("type:date")) {

                if (!json.has("value"))
                    throw new QueryException(612, "Dates require value fields");
//-
                String dateStr = json.get("value").asText();
                if (json.has("match"))
                    match = json.get("match").asText();

                // TODO: This isn't stable yet
                switch (match) {
                case "match:eq":
                    bfilter.date(dateStr);
                    break;
                case "match:geq":
                    bfilter.since(dateStr);
                    break;
                case "match:leq":
                    bfilter.till(dateStr);
                    break;
                };
                /*
                  No good reason for gt or lt
                */
                return bfilter;
            }
            else if (valtype.equals("type:string")) {
                if (json.has("match"))
                    match = json.get("match").asText();

                if (match.equals("match:eq")) {
                    bfilter.and(key, json.get("value").asText());
                };
                return bfilter;
            };
        }

        // nested group
        else if (type.equals("korap:docGroup")) {
            if (!json.has("operands") || !json.get("operands").isArray())
//-
                throw new QueryException(612, "Groups need operands");

            String operation = "operation:and";
            if (json.has("operation"))
                operation = json.get("operation").asText();

            BooleanFilter group = new BooleanFilter();

            for (JsonNode operand : json.get("operands")) {
                if (operation.equals("operation:and")) {
                    group.and(this.fromJSON(operand, field));
                }
                else if (operation.equals("operation:or")) {
                    group.or(this.fromJSON(operand, field));
                }
                else {
                    throw new QueryException(613, "Unknown document group operation");
                };
            };
            bfilter.and(group);
            return bfilter;
        }

        // Unknown type
        else {
// -
            throw new QueryException(613, "Collection query type has to be doc or docGroup");
        };

        return new BooleanFilter();
    };

	/*
      String type = json.get("@type").asText();
      String field = _getField(json);

      if (type.equals("korap:term")) {
      this.fromJSON(json, field);
      }
      else if (type.equals("korap:group")) {
	    // TODO: relation
	    for (JsonNode operand : json.get("operands")) {
		this.fromJSON(operand, field);
	    };
	};
	*/
    //    };
    
    protected BooleanFilter fromJSONLegacy (JsonNode json, String field)
        throws QueryException {
        BooleanFilter bfilter = new BooleanFilter();

//-
        if (!json.has("@type"))
            throw new QueryException(612, "JSON-LD group has no @type attribute");
	
        String type = json.get("@type").asText();

        if (DEBUG)
            log.trace("@type: " + type);

        if (json.has("@field"))
            field = _getFieldLegacy(json);

        if (type.equals("korap:term")) {
            if (field != null && json.has("@value"))
                bfilter.and(field, json.get("@value").asText());
            return bfilter;
        }
        else if (type.equals("korap:group")) {
//-
            if (!json.has("relation"))
                throw new QueryException(612, "Group needs relation");

            if (!json.has("operands"))
//-
                throw new QueryException(612, "Group needs operand list");

		//return bfilter;

            String dateStr, till;
            JsonNode operands = json.get("operands");

            if (!operands.isArray())
//-
                throw new QueryException(612, "Group needs operand list");

            if (DEBUG)
                log.trace("relation found {}",  json.get("relation").asText());

            BooleanFilter group = new BooleanFilter();
	    
            switch (json.get("relation").asText())  {
            case "between":
                dateStr = _getDateLegacy(json, 0);
                till = _getDateLegacy(json, 1);
                if (dateStr != null && till != null)
                    bfilter.between(dateStr, till);
                break;

            case "until":
                dateStr = _getDateLegacy(json, 0);
                if (dateStr != null)
                    bfilter.till(dateStr);
                break;

            case "since":
                dateStr = _getDateLegacy(json, 0);
                if (dateStr != null)
                    bfilter.since(dateStr);
                break;

            case "equals":
                dateStr = _getDateLegacy(json, 0);
                if (dateStr != null)
                    bfilter.date(dateStr);
                break;

            case "and":
//-
                if (operands.size() < 1)
                    throw new QueryException(612, "Operation needs at least two operands");

                for (JsonNode operand : operands) {
                    group.and(this.fromJSONLegacy(operand, field));
                };
                bfilter.and(group);
                break;

            case "or":
//-
                if (operands.size() < 1)
                    throw new QueryException(612, "Operation needs at least two operands");

                for (JsonNode operand : operands) {
                    group.or(this.fromJSONLegacy(operand, field));
                };
                bfilter.and(group);
                break;

//-
            default:
                throw new QueryException(613, "Relation is not supported");
            };
        }
        else {
            throw new QueryException(613, "Filter type is not a supported group");
        };
        return bfilter;
    };

    private static String  _getFieldLegacy (JsonNode json)  {
        if (!json.has("@field"))
            return (String) null;

        String field = json.get("@field").asText();
        return field.replaceFirst("korap:field#", "");
    };

    private static String _getDateLegacy (JsonNode json, int index) {
        if (!json.has("operands"))
            return (String) null;

        if (!json.get("operands").has(index))
            return (String) null;

        JsonNode date = json.get("operands").get(index);

        if (!date.has("@type"))
            return (String) null;

        if (!date.get("@type").asText().equals("korap:date"))
            return (String) null;
        
        if (!date.has("@value"))
            return (String) null;

        return date.get("@value").asText();
    };

    
    public BooleanFilter and (String type, String ... terms) {
        BooleanFilter bf = new BooleanFilter();
        bf.and(type, terms);
        return bf;
    };

    public BooleanFilter or (String type, String ... terms) {
        if (DEBUG)
            log.debug("Got some terms here");
        BooleanFilter bf = new BooleanFilter();
        bf.or(type, terms);
        return bf;
    };

    public BooleanFilter and (String type, RegexFilter re) {
        BooleanFilter bf = new BooleanFilter();
        bf.and(type, re);
        return bf;
    };

    public BooleanFilter or (String type, RegexFilter re) {
        BooleanFilter bf = new BooleanFilter();
        bf.or(type, re);
        return bf;
    };

    public BooleanFilter since (String date) {
        BooleanFilter bf = new BooleanFilter();
        bf.since(date);
        return bf;
    };

    public BooleanFilter till (String date) {
        BooleanFilter bf = new BooleanFilter();
        bf.till(date);
        return bf;
    };

    public BooleanFilter date (String date) {
        BooleanFilter bf = new BooleanFilter();
        bf.date(date);
        return bf;
    };

    public BooleanFilter between (String date1, String date2) {
        BooleanFilter bf = new BooleanFilter();
        bf.between(date1, date2);
        return bf;
    };

    public RegexFilter re (String regex) {
        return new RegexFilter(regex);
    };

    public BooleanFilter getBooleanFilter()  {
        return this.filter;
    };

    public void setBooleanFilter (BooleanFilter bf) {
        this.filter = bf;
    };

    public Query toQuery () {
        return this.filter.toQuery();
    };

    public String toString () {
        return this.filter.toQuery().toString();
    };
};
