package de.ids_mannheim.korap.index;

import java.util.*;

import de.ids_mannheim.korap.util.KrillDate;
import de.ids_mannheim.korap.index.FieldDocument;
import de.ids_mannheim.korap.response.Response;
import de.ids_mannheim.korap.response.MetaField;
import de.ids_mannheim.korap.response.MetaFieldsObj;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.fasterxml.jackson.databind.node.TextNode;

/*
 * Todo:: Author and textClass may be arrays!
 */

/**
 * Abstract class representing a document in the
 * Krill index.
 * 
 * This model is rather specific to DeReKo data and
 * should be considered experimental. It will be replaced
 * by a more agnostic model.
 * string fields, e.g. will be combined with a prefix.
 * For example d:pubDate will mean: A field with the key "pubDate"
 * of type date.
 * 
 * @author diewald
 */
@JsonInclude(Include.NON_EMPTY)
// @JsonIgnoreProperties(ignoreUnknown = true)
public abstract class AbstractDocument extends Response {
    ObjectMapper mapper = new ObjectMapper();
    
    private String primaryData;
    
    private static HashSet<String> legacyStringFields =
        new HashSet<String>(Arrays.asList(
                                "pubPlace",
                                "textSigle",
                                "docSigle",
                                "corpusSigle",
                                "textType",
                                "textTypeArt",
                                "textTypeRef",
                                "textColumn",
                                "textDomain",
                                "availability",
                                "language",
                                "corpusID", // Deprecated!
                                "ID"        // Deprecated!
                                ));
    
    private static HashSet<String> legacyTextFields =
        new HashSet<String>(Arrays.asList(
                                "author",
                                "title",
                                "subTitle",
                                "corpusTitle",
                                "corpusSubTitle",
                                "corpusAuthor",
                                "docTitle",
                                "docSubTitle",
                                "docAuthor"
                                ));

    private static HashSet<String> legacyKeywordsFields =
        new HashSet<String>(Arrays.asList(
                                "textClass",
                                "foundries",
                                "keywords"
                                ));

    private static HashSet<String> legacyStoredFields =
        new HashSet<String>(Arrays.asList(
                                "docEditor",
                                "tokenSource",
                                "layerInfos",
                                "publisher",
                                "editor",
                                "fileEditionStatement",
                                "biblEditionStatement",
                                "reference",
                                "corpusEditor"
                                ));

    private static HashSet<String> legacyDateFields =
        new HashSet<String>(Arrays.asList(
                                "pubDate",
                                "creationDate"
                                ));    
    
    @JsonIgnore
    public int internalDocID, localDocID, UID;

    @JsonIgnore
    public MetaFieldsObj mFields = new MetaFieldsObj();

    /**
     * Populate document meta information with information coming from
     * the index.
     * 
     * @param doc
     *            Document object.
     * @param field
     *            Primary data field.
     */
    public void populateDocument (Document doc, String field) {
        List<String> fieldList = new ArrayList<>(32);
        Iterator<IndexableField> fieldIterator = doc.getFields().iterator();
        while (fieldIterator.hasNext())
            fieldList.add(fieldIterator.next().name());

        this.populateDocument(doc, field, fieldList);
    };

    /**
     * Populate document meta information with information coming from
     * the index.
     * 
     * @param doc
     *            Document object.
     * @param field
     *            Primary data field.
     * @param fields
     *            Hash object with all supported fields.
     */
    public void populateDocument (Document doc, String field,
            List<String> fields) {
        this.setPrimaryData(doc.get(field));
        this.populateFields(doc, fields);
    };


    public void populateFields (Document doc) {
        ArrayList<String> fieldList = new ArrayList<>(32);
        Iterator<IndexableField> fieldIterator = doc.getFields().iterator();
        while (fieldIterator.hasNext()) {
            fieldList.add(fieldIterator.next().name());
        };

        // TODO: Sort alphabetically!
        
        this.populateFields(doc, fieldList);
    };


    public void populateFields (Document doc, List<String> fields) {
        // Remove all fields already set
        Iterator<String> fieldsIter = fields.iterator();
        while (fieldsIter.hasNext()) {
            if (mFields.contains(fieldsIter.next())) {
                fieldsIter.remove();
            };
        };

        
        if (fields.contains("UID")) {
            this.setUID(doc.get("UID"));
        };
        
        fieldsIter = fields.iterator();
        mFields.fieldsOrder = new ArrayList<>(16);

        while (fieldsIter.hasNext()) {
            String name = fieldsIter.next();

            // Remember - never serialize "tokens"
            if (name.equals("tokens") || name.equals("UID"))
                continue;

            mFields.fieldsOrder.add(name);

            IndexableField iField = doc.getField(name);
            
            if (iField == null)
                continue;
            
            MetaField mf = mFields.add(iField);

            // Legacy
            if (name.equals("license"))
                this.addString("availability", doc.get("license"));

        };
    };


    /**
     * Get the unique identifier of the document.
     * 
     * @return The unique identifier of the document as an integer.
     */
    @JsonProperty("UID")
    public int getUID () {
        return this.UID;
    };


    /**
     * Set the unique identifier of the document.
     * 
     * @param UID
     *            The unique identifier of the document as an integer.
     * @return The invocant for chaining.
     */
    public void setUID (int UID) {
        if (UID != 0) {
            this.UID = UID;
            this.addString("UID", new Integer(UID).toString());
        }       
    };


    /**
     * Set the unique identifier of the document.
     * 
     * @param UID
     *            The unique identifier of the document as a
     *            string representing an integer.
     * @return The invocant for chaining.
     * @throws NumberFormatException
     */
    public void setUID (String UID) throws NumberFormatException {
        if (UID != null) {
            this.UID = Integer.parseInt(UID);
            this.addString("UID", new Integer(this.UID).toString());
        };
    };


    /**
     * Get the primary data of the document.
     * 
     * @return The primary data of the document as a string.
     */
    @JsonIgnore
    public String getPrimaryData () {
        if (this.primaryData == null)
            return "";
        return this.primaryData;
    };


    /**
     * Get the primary data of the document,
     * starting with a given character offset.
     * 
     * @param startOffset
     *            The starting character offset.
     * @return The substring of primary data of the document as a
     *         string.
     */
    @JsonIgnore
    public String getPrimaryData (int startOffset) {
        return this.primaryData.substring(startOffset);
    };


    /**
     * Get the primary data of the document,
     * starting with a given character offset and ending
     * with a given character offset.
     * 
     * @param startOffset
     *            The starting character offset.
     * @param endOffset
     *            The ending character offset.
     * @return The substring of the primary data of the document as a
     *         string.
     */
    @JsonIgnore
    public String getPrimaryData (int startOffset, int endOffset) {
        return this.primaryData.substring(startOffset, endOffset);
    };


    /**
     * Set the primary data of the document.
     * 
     * @param primary
     *            The primary data of the document
     *            as a string.
     */
    public void setPrimaryData (String primary) {
        this.primaryData = primary;
    };


    /**
     * Get the length of the primary data of the document
     * (i.e. the number of characters).
     * 
     * @return The length of the primary data of the document as an
     *         integer.
     */
    @JsonIgnore
    public int getPrimaryDataLength () {
        return this.primaryData.length();
    };


    /**
     * Get the text sigle as a string.
     * 
     * @return The text sigle as a string.
     */
    public String getTextSigle () {
        return this.getFieldValue("textSigle");
    };


    /**
     * Get the document sigle as a string.
     * 
     * @return The document sigle as a string.
     */
    public String getDocSigle () {
        return this.getFieldValue("docSigle");
    };


    /**
     * Get the corpus sigle as a string.
     * 
     * @return The corpus sigle as a string.
     */
    public String getCorpusSigle () {
        return this.getFieldValue("corpusSigle");
    };


    @Deprecated
    public String getAvailability () {
        return this.getFieldValue("availability");
    };
    

    @Deprecated
    @JsonProperty("corpusID")
    public String getCorpusID () {
        return this.getFieldValue("corpusID");
    };

    @Deprecated
    @JsonProperty("ID")
    public String getID () {
        return this.getFieldValue("ID");
    };

    @JsonAnyGetter
    public Map<String, JsonNode> getLegacyMetaFields () {

        Iterator<MetaField> mfIterator = mFields.iterator();

        HashMap<String, JsonNode> map = new HashMap<>();

        while (mfIterator.hasNext()) {
            MetaField mf = mfIterator.next();
            if (mf == null)
                continue;
            String mfs = mf.key;
            String value = this.getFieldValue(mfs);
                if (value != null && (
                        legacyDateFields.contains(mfs) ||
                        legacyStoredFields.contains(mfs) ||
                        legacyTextFields.contains(mfs) ||
                        legacyStringFields.contains(mfs) ||
                        legacyKeywordsFields.contains(mfs) ||
                        legacyDateFields.contains(mfs)
                        )
                ) {
                map.put(mfs, new TextNode(value));
            }
        };
        
        return map;
    }

    
    @JsonAnySetter
    public void setLegacyMetaField (String name, JsonNode value) {
        
        // Treat legacy string fields
        if (legacyStringFields.contains(name)) {
            this.addString(name, value.asText());
        }

        // Treat legacy text fields
        else if (legacyTextFields.contains(name)) {
            this.addText(name, value.asText());
        }

        // Treat legacy keyword fields
        else if (legacyKeywordsFields.contains(name)) {
            this.addKeywords(name, value.asText());
        }

        // Treat legacy stored fields
        else if (legacyStoredFields.contains(name)) {
            this.addStored(name, value.asText());
        }

        // Treat legacy date fields
        else if (legacyDateFields.contains(name)) {
            this.addDate(name, value.asText());
        }
       
        else if (name.equals("license")) {
            this.addString("availability", value.asText());
        }

        // Temporarily - treat legacy store values introduced for Sgbr
        else if (name.equals("store")) {
            // TODO: Store all values
        };
        //
        // else {
        //    System.err.println("Unknown field: " + name);
        // };
    };
    

    /**
     * Serialize response as a {@link JsonNode}.
     * 
     * @return {@link JsonNode} representation of the response
     */
    @Override
    public JsonNode toJsonNode () {
        ObjectNode json = (ObjectNode) super.toJsonNode();
        json.putAll((ObjectNode) mapper.valueToTree(this));

        if (this.getUID() == 0)
            json.remove("UID");

        return json;
    };

    @JsonIgnore
    public String getFieldValue (String field) {
        MetaField mf = mFields.get(field);

        if (mf != null && mf.values.size() > 0) {
            return String.join(
                " ",
                mf.values
                );
        };

        return null;
    };


    @JsonIgnore
    public KrillDate getFieldValueAsDate (String field) {
        String date = this.getFieldValue(field);

        if (date == null)
            return null;

        return new KrillDate(date);
    };

    @JsonIgnore
    public void addString (String key, String value) {
        if (value == null)
            return;
        
        mFields.add(
            new MetaField(
                key,
                "type:string",
                value
                )
            );
    };


    @JsonIgnore
    public void addInt (String key, String value) {
        if (value == null)
            return;
        
        mFields.add(
            new MetaField(
                key,
                "type:integer",
                value
                )
            );
    };

    @JsonIgnore
    public void addInt (String key, int value) {
        this.addInt(key, new Integer(value).toString());
    };

    
    @JsonIgnore
    public void addStored (String key, String value) {
        if (value == null)
            return;

        mFields.add(
            new MetaField(
                key,
                "type:store",
                value
                )
            );
    };


    @JsonIgnore
    public void addAttachement (String key, String value) {
        if (value == null)
            return;

        mFields.add(
            new MetaField(
                key,
                "type:attachement",
                value
                )
            );
    };

    
    @JsonIgnore
    public void addKeywords (String key, String value) {
        if (value == null)
            return;

        mFields.add(
            new MetaField(
                key,
                "type:keywords",
                value
                )
            );
    };

    @JsonIgnore
    public void addText (String key, String value) {
        if (value == null)
            return;

        mFields.add(
            new MetaField(
                key,
                "type:text",
                value
                )
            );
    };

    @JsonIgnore
    public void addDate (String key, String value) {
        if (value == null)
            return;

        KrillDate date = new KrillDate(value);

        if (date == null)
            return;
        
        mFields.add(
            new MetaField(
                key,
                "type:date",
                date.toDisplay()
                )
            );
    };

    @JsonIgnore
    public void addDate (String key, int value) {
        this.addDate(key, new Integer(value).toString());
    };
};
