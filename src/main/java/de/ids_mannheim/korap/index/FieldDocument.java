package de.ids_mannheim.korap.index;

import de.ids_mannheim.korap.index.MultiTermTokenStream;
import de.ids_mannheim.korap.index.MultiTermToken;
import de.ids_mannheim.korap.index.AbstractDocument;
import de.ids_mannheim.korap.util.KrillDate;
import de.ids_mannheim.korap.util.CorpusDataException;
import de.ids_mannheim.korap.response.MetaField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;

import org.apache.lucene.analysis.TokenStream;

import java.util.*;
import java.io.StringReader;
import java.io.IOException;

/*
  TODO: Store primary data at base/cons field.
  All other Termvectors should have no stored field!

  TODO: Currently Character offsets are stored with positional
  information in the token stream. This is bad!
  The character offset may need a special encoding in Lucene
  To store the character offsets directly (not in the payloads),
  to make this less messy and speed things up.
*/

/*
 * Currently the Fielddocument is not ready to be used for KQ
 * serialization of fields - this is currently done in
 * response/Fields.
*/

/**
 * FieldDocument represents a simple API to create documents
 * for storing with KrillIndex. <i>Field</i> in the name resembles
 * the meaning of Lucene index fields.
 * 
 * @author diewald
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FieldDocument extends AbstractDocument {
    ObjectMapper mapper = new ObjectMapper();


    // Logger
	private final static Logger log = LoggerFactory.getLogger(FieldDocument.class);

	// This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;
    
    @JsonIgnore
    public Document doc = new Document();
    private FieldType tvField = new FieldType(TextField.TYPE_STORED);
    private FieldType tvNoField = new FieldType(TextField.TYPE_NOT_STORED);
    private FieldType keywordField = new FieldType(TextField.TYPE_STORED);

    {
        tvField.setStoreTermVectors(true);
        tvField.setStoreTermVectorPositions(true);
        tvField.setStoreTermVectorPayloads(true);
        tvField.setStoreTermVectorOffsets(false);

        tvNoField.setStoreTermVectors(true);
        tvNoField.setStoreTermVectorPositions(true);
        tvNoField.setStoreTermVectorPayloads(true);
        tvNoField.setStoreTermVectorOffsets(false);

        keywordField.setStoreTermVectors(false);
        keywordField.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
    };


    // see http://www.cowtowncoder.com/blog/archives/2011/07/entry_457.html

    public void addInt (String key, int value) {
        doc.add(new IntField(key, value, Field.Store.YES));
    };

    public void addInt (String key, String value) {
		if (value != null)
			this.addInt(key, Integer.parseInt(value));
    };

    @Override
    public void addDate (String key, String value) {
        mFields.add(
            key,
            new MetaField(
                key,
                "type:date",
                value
                )
            );
        KrillDate date = new KrillDate(value);
		if (date != null) {
			this.addInt(key, date.toString());
		};
    }

    @Override
    public void addText (String key, String value) {
        mFields.add(
            key,
            new MetaField(
                key,
                "type:text",
                value
                )
            );
		doc.add(new TextPrependedField(key, value));
    };


    @Override
    public void addKeywords (String key, String value) {
        mFields.add(
            key,
            new MetaField(
                key,
                "type:keywords",
                value
                )
            );

        doc.add(new Field(key, value, keywordField));
    };

    @Override
    public void addString (String key, String value) {
        mFields.add(
            key,
            new MetaField(
                key,
                "type:string",
                value
                )
            );
        doc.add(new StringField(key, value, Field.Store.YES));
    };

    public void addAttachement (String key, String value) {
        mFields.add(
            key,
            new MetaField(
                key,
                "type:attachement",
                value
                )
            );
        doc.add(new StoredField(key, value));
    };    

    @Override
    public void addStored (String key, String value) {
        mFields.add(
            key,
            new MetaField(
                key,
                "type:store",
                value
                )
            );
        doc.add(new StoredField(key, value));
    };


    public void addStored (String key, int value) {
        mFields.add(
            key,
            new MetaField(
                key,
                "type:store",
                new Integer(value).toString()
                )
            );
        doc.add(new StoredField(key, value));
    };


    public void addTV (String key, String value, String tsString) {
        this.addTV(key, value, new MultiTermTokenStream(tsString));
    };


    public void addTV (String key, String tsString) {
        this.addTV(key, new MultiTermTokenStream(tsString));
    };


    public void addTV (String key, String value, MultiTermTokenStream ts) {
        Field textField = new Field(key, value, tvField);
        textField.setTokenStream(ts);
        doc.add(textField);
    };


    public void addTV (String key, MultiTermTokenStream ts) {
        Field textField = new Field(key, ts, tvNoField);
        doc.add(textField);
    };


    public String toString () {
        return doc.toString();
    };


    public MultiTermTokenStream newMultiTermTokenStream (String ts) {
        return new MultiTermTokenStream(ts);
    };


    public MultiTermTokenStream newMultiTermTokenStream () {
        return new MultiTermTokenStream();
    };


    /**
     * Deserialize token stream data.
     */
    public void setData (Map<String, Object> node) {
        this.setPrimaryData((String) node.get("text"));

        String fieldName = (String) node.get("name");

        MultiTermTokenStream mtts = this.newMultiTermTokenStream();

        // Iterate over all tokens in stream
        for (ArrayList<String> token : (ArrayList<ArrayList<String>>) node
                .get("stream")) {

            try {
                // Initialize MultiTermToken
                MultiTermToken mtt = new MultiTermToken(token.remove(0));

                // Add rest of the list
                for (String term : token) {
                    mtt.add(term);
                };

                // Add MultiTermToken to stream
                mtts.addMultiTermToken(mtt);

            }
            catch (CorpusDataException cde) {
                this.addError(cde.getErrorCode(), cde.getMessage());
            };
        };

        // Add tokenstream to fielddocument
        this.addTV(fieldName, this.getPrimaryData(), mtts);

        // Get foundry info
        if (node.containsKey("foundries"))
            this.addKeywords(
                "foundries",
                (String) node.get("foundries")
                );

        // Get layer info
        if (node.containsKey("layerInfos"))
            this.addStored("layerInfos", (String) node.get("layerInfos"));

        // Get tokenSource info
        if (node.containsKey("tokenSource"))
            this.addStored("tokenSource", (String) node.get("tokenSource"));
    };

    
    /**
     * Deserialize koral:field types for meta data
     */
    public void setMetaFields (ArrayList<Map<String, JsonNode>> fields) {
        String type, key, value;
        StringBuffer sb = new StringBuffer();
        Iterator<JsonNode> i;

        for (Map<String, JsonNode> field : fields) {
            if (field.get("@type").asText().equals("koral:field")) {
                type = (String) field.get("type").asText();
                key = (String) field.get("key").asText();
                
                // Add string field
                if (type.equals("type:string") || type.equals("type:keywords")) {

                    // Field is an array
                    if (field.get("value").isArray()) {
                        i = field.get("value").elements();
                        
                        sb.setLength(0);
                        while (i.hasNext()) {
                            sb.append(i.next().asText()).append(" ");
                        };
                        if (sb.length() > 1) {
                            sb.setLength(sb.length() - 1);
                        };
                        this.addKeywords(key, sb.toString());
                    }
                    else {
                        this.addString(key, field.get("value").asText());
                    };
                }

                // Add text field
                else if (type.equals("type:text")) {
                    this.addText(key, field.get("value").asText());
                }

                // Add integer field
                else if (type.equals("type:integer")) {
                    this.addInt(key, field.get("value").asInt());
                }

                // Add attachement field
                else if (type.equals("type:attachement")) {
                    value = field.get("value").asText();
                    if (value.startsWith("data:")) {
                        this.addAttachement(key, value);
                    };
                }

                // Add date field
                else if (type.equals("type:date")) {
                    KrillDate date = new KrillDate(field.get("value").asText());
                    if (date != null) {
                        this.addInt(key, date.toString());
                    };
                }

                // Unknown
                else {
                    log.error("Unknown field type {}", type);
                };
            };
        }
    };    
    

    /**
     * Deserialize token stream data (LEGACY).
     */
    public void setFields (ArrayList<Map<String, Object>> fields) {
        Map<String, Object> primary = fields.remove(0);
        this.setPrimaryData((String) primary.get("primaryData"));

        for (Map<String, Object> field : fields) {

            String fieldName = (String) field.get("name");
            MultiTermTokenStream mtts = this.newMultiTermTokenStream();

            for (ArrayList<String> token : (ArrayList<ArrayList<String>>) field
                    .get("data")) {

                try {
                    MultiTermToken mtt = new MultiTermToken(token.remove(0));

                    for (String term : token) {
                        mtt.add(term);
                    };

                    mtts.addMultiTermToken(mtt);
                }
                catch (CorpusDataException cde) {
                    this.addError(cde.getErrorCode(), cde.getMessage());
                };
            };

            // TODO: This is normally dependend to the tokenization!
            //       Add this as meta information to the document
            // Store this information as well as tokenization information
            // as meta fields in the tokenization term vector
            if (field.containsKey("foundries")) {
                // TODO: Do not store positions!
                String foundries = (String) field.get("foundries");
                this.addKeywords("foundries", foundries);
            };

            this.addTV(fieldName, this.getPrimaryData(), mtts);
        };
    };
};
