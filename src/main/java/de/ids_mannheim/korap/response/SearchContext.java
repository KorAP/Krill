package de.ids_mannheim.korap.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.ids_mannheim.korap.util.KrillConfiguration;

public class SearchContext {
    ObjectMapper mapper = new ObjectMapper();

    private boolean spanType = false;

    @JsonIgnore
    public SearchContextSide left, right;

    @JsonIgnore
    public String spanContext;

    {
        left = new SearchContextSide();
        right = new SearchContextSide();
    };


    public SearchContext () {};
    
    // EM: not used?
    public SearchContext (String spanContext) {
        this.spanType = true;
        this.spanContext = spanContext;
    };

    // EM: seems to be deprecated. used in a deprecated search method
    public SearchContext (boolean leftTokenContext, short leftContext,
                          boolean rightTokenContext, short rightContext) {
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

        // <LEGACY>
        if (spanContext.equals("sentence")) {
            spanContext = "base/s:s";
        }
        else if (spanContext.equals("paragraph")) {
            spanContext = "base/s:p";
        };
        // </LEGACY>

        this.spanContext = spanContext;
        return this;
    };

    public void updateContext (KrillConfiguration krillConfig) {
        left.setMaxLength(krillConfig.getMaxContextTokens());
        right.setMaxLength(krillConfig.getMaxContextTokens());
        
        // update token length
        if (left.isToken) {
            left.setLength(left.getLength());
        }
        if (right.isToken) {
            right.setLength(right.getLength());
        }
    }
    
    
    public class SearchContextSide {
        private boolean isToken = true;
        private int length = 6;
        private int maxLength = 500;

        public SearchContextSide () {}
        
        public int getMaxLength () {
            return maxLength;
        }
        public void setMaxLength (int maxLength) {
            this.maxLength = maxLength;
        }
        
        public boolean isToken () {
            return this.isToken;
        };


        public boolean isCharacter () {
            return !(this.isToken);
        };


        public SearchContextSide setToken (boolean value) {
            this.isToken = value;
            return this;
        };


        public SearchContextSide setCharacter (boolean value) {
            this.isToken = !(value);
            return this;
        };


        public int getLength () {
            return this.length;
        };


        public SearchContextSide setLength (int value) {
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
        context.set("left", leftContext);
        context.set("right", rightContext);

        return context;
    };
};
