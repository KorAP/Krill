package de.ids_mannheim.korap.index;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.annotation.*;


public class SearchContext {
    ObjectMapper mapper = new ObjectMapper();

    
    private boolean spanType = false;

    @JsonIgnore
    public SearchContextSide left, right;

    @JsonIgnore
    public String spanContext;

    {
	left  = new SearchContextSide();
	right = new SearchContextSide();
    };

    public SearchContext () {};

    public SearchContext (String spanContext) {
	this.spanType = true;
	this.spanContext = spanContext;
    };

    public SearchContext (boolean leftTokenContext,
			  short leftContext,
			  boolean rightTokenContext,
			  short rightContext) {
	this.spanType = false;
	this.left.setToken(leftTokenContext);
	this.left.setLength(leftContext);
	this.right.setToken(leftTokenContext);
	this.right.setLength(rightContext);
    };

    public boolean isSpanDefined () {
	return this.spanType;
    };

    public String getSpanContext () {
	return this.spanContext;
    };

    public SearchContext setSpanContext (String spanContext) {
	this.spanType = true;

	if (spanContext.equals("sentence")) {
	    spanContext = "s";
	}
	else if (spanContext.equals("paragraph")) {
	    spanContext = "p";
	};
	
	this.spanContext = spanContext;
	return this;
    };

    public class SearchContextSide {
	private boolean type = true;
	private short length = 6;
	private short maxLength = 500;
	
	public boolean isToken () {
	    return this.type;
	};
	
	public boolean isCharacter () {
	    return !(this.type);
	};

	public SearchContextSide setToken (boolean value) {
	    this.type = value;
	    return this;
	};

	public SearchContextSide setCharacter (boolean value) {
	    this.type = !(value);
	    return this;
	};

	public short getLength() {
	    return this.length;
	};
	
	public SearchContextSide setLength (short value) {
	    if (value >= 0) {
		if (value <= maxLength) {
		    this.length = value;
		}
		else {
		    this.length = this.maxLength;
		};
	    };
	    return this;
	};

	public SearchContextSide setLength (int value) {
	    return this.setLength((short) value);
	};

	public void fromJson (JsonNode json) {
	    String type = json.get(0).asText();
	    if (type.equals("token")) {
		this.setToken(true);
	    }
	    else if (type.equals("char")) {
		this.setCharacter(true);
	    };
	    this.setLength(json.get(1).asInt(this.length));
	};
    };


    public void fromJson (JsonNode context) {
	if (context.isContainerNode()) {
	    if (context.has("left"))
		this.left.fromJson(context.get("left"));
	    
	    if (context.has("right"))
		this.right.fromJson(context.get("right"));
	}
	else if (context.isValueNode()) {
	    this.setSpanContext(context.asText());
	};
    };

    public JsonNode toJsonNode () {

	if (this.isSpanDefined())
	    return new TextNode(this.spanContext);
	
	ArrayNode leftContext = mapper.createArrayNode();
	leftContext.add(this.left.isToken() ? "token" : "char");
	leftContext.add(this.left.getLength());

	ArrayNode rightContext = mapper.createArrayNode();
	rightContext.add(this.right.isToken() ? "token" : "char");
	rightContext.add(this.right.getLength());

	ObjectNode context = mapper.createObjectNode();
	context.put("left", leftContext);
	context.put("right", rightContext);

	return context;
    };

};
