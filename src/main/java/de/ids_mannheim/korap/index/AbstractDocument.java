package de.ids_mannheim.korap.index;

import java.util.*;

import de.ids_mannheim.korap.util.KrillDate;
import de.ids_mannheim.korap.index.FieldDocument;
import de.ids_mannheim.korap.response.Response;
import de.ids_mannheim.korap.response.MetaField;
import de.ids_mannheim.korap.response.MetaFieldsExt;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;


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
    public MetaFieldsExt mFields = new MetaFieldsExt();

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
        HashSet<String> fieldList = new HashSet<>(32);
        Iterator<IndexableField> fieldIterator = doc.getFields().iterator();
        while (fieldIterator.hasNext())
            fieldList.add(fieldIterator.next().name());

        this.populateDocument(doc, field, fieldList);
    };


    public void populateFields (Document doc) {

        HashSet<String> fieldList = new HashSet<>(32);
        Iterator<IndexableField> fieldIterator = doc.getFields().iterator();
        while (fieldIterator.hasNext())
            fieldList.add(fieldIterator.next().name());

        this.populateFields(doc, fieldList);
    };


    public void populateFields (Document doc, Collection<String> fields) {
        // Remember - never serialize "tokens"

        // TODO:
        //   Pupulate based on field types!

        if (fields.contains("UID"))
            this.setUID(doc.get("UID"));

        String field;
        Iterator<String> i = legacyTextFields.iterator();
        while (i.hasNext()) {
            field = i.next();
            if (fields.contains(field)) {
                this.addText(field, doc.get(field));
            };
        };

        i = legacyKeywordsFields.iterator();
        while (i.hasNext()) {
            field = i.next();
            if (fields.contains(field)) {
                this.addKeywords(field, doc.get(field));
            };
        };

        i = legacyStoredFields.iterator();
        while (i.hasNext()) {
            field = i.next();
            if (fields.contains(field)) {
                this.addStored(field, doc.get(field));
            };
        };

        i = legacyStringFields.iterator();
        while (i.hasNext()) {
            field = i.next();
            if (fields.contains(field)) {
                this.addString(field, doc.get(field));
            };
        };

        i = legacyDateFields.iterator();
        while (i.hasNext()) {
            field = i.next();
            if (fields.contains(field)) {
                this.addDate(field, doc.get(field));
            };
        };
        
        // Legacy
        if (fields.contains("license"))
            this.addString("availability", doc.get("license"));

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
            Collection<String> fields) {
        this.setPrimaryData(doc.get(field));
        this.populateFields(doc, fields);
    };


    /**
     * Get the publication date of the document
     * as a {@link KrillDate} object.
     * 
     * @return A {@link KrillDate} object for chaining.
     */
    @JsonIgnore
    public KrillDate getPubDate () {
        String pubDate = this.getFieldValue("pubDate");
        if (pubDate == null)
            return null;
        return new KrillDate(pubDate);
    };


    /**
     * Get the publication date of the document
     * as a string.
     * 
     * @return A string containing the {@link KrillDate}.
     */
    @JsonProperty("pubDate")
    public String getPubDateString () {
        KrillDate pubDate = this.getPubDate();

        if (pubDate != null) {
            String date = pubDate.toDisplay();
            if (date.length() == 0)
                return null;
            return date;
        };
        return null;
    };


    /**
     * Get the creation date of the document
     * as a {@link KrillDate} object.
     * 
     * @return A {@link KrillDate} object for chaining.
     */
    @JsonIgnore
    public KrillDate getCreationDate () {
        String creationDate = this.getFieldValue("creationDate");
        if (creationDate == null)
            return null;
        return new KrillDate(creationDate);
    };


    /**
     * Get the creation date of the document
     * as a string.
     * 
     * @return A string containing the {@link KrillDate}.
     */
    @JsonProperty("creationDate")
    public String getCreationDateString () {
        KrillDate creationDate = this.getCreationDate();

        if (creationDate != null) {
            String date = creationDate.toDisplay();
            if (date.length() == 0)
                return null;
            return date;
        };
        return null;
    };


    /**
     * Get the name of the author of the document.
     * 
     * @return The name of the author as a string.
     */
    public String getAuthor () {
        return this.getFieldValue("author");
    };

    
    /**
     * Get the text class of the document.
     * 
     * @return The text class of the document as a string.
     */
    public String getTextClass () {
        return this.getFieldValue("textClass");
    };


    /**
     * Get the publication place of the document.
     * 
     * @return The publication place of the document as a string.
     */
    public String getPubPlace () {
        return this.getFieldValue("pubPlace");
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
     * Get the title of the document.
     * 
     * @return The title of the document as a string.
     */
    public String getTitle () {
        return this.getFieldValue("title");
    };


    /**
     * Get the subtitle of the document.
     * 
     * @return The subtitle of the document as a string.
     */
    public String getSubTitle () {
        return this.getFieldValue("subTitle");
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
     * Get information on the foundries the document
     * is annotated with as a string.
     * 
     * @return The foundry information string.
     */
    public String getFoundries () {
        return this.getFieldValue("foundries");
    };


    /**
     * Get information on the layers the document
     * is annotated with as a string.
     * 
     * @return The layer information string.
     */
    public String getLayerInfos () {
        return this.getFieldValue("layerInfos");
    };


    // This is the new text id
    /**
     * Get the text sigle as a string.
     * 
     * @return The text sigle as a string.
     */
    public String getTextSigle () {
        return this.getFieldValue("textSigle");
    };


    // This is the new corpus id
    /**
     * Get the corpus sigle as a string.
     * 
     * @return The corpus sigle as a string.
     */
    public String getCorpusSigle () {
        return this.getFieldValue("corpusSigle");
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
     * Get the name of the publisher as a string.
     * 
     * @return The name of the publisher as a string.
     */
    public String getPublisher () {
        return this.getFieldValue("publisher");
    };


    /**
     * Get the name of the editor as a string.
     * 
     * @return The name of the editor as a string.
     */
    public String getEditor () {
        return this.getFieldValue("editor");
    };

    
    /**
     * Get the type of the text as a string.
     * 
     * @return The type of the text as a string.
     */
    public String getTextType () {
        return this.getFieldValue("textType");
    };


    /**
     * Get the type art of the text as a string.
     * 
     * @return The type art of the text as a string.
     */
    public String getTextTypeArt () {
        return this.getFieldValue("textTypeArt");
    };

    /**
     * Get the type reference of the text as a string.
     * 
     * @return The type reference of the text as a string.
     */
    public String getTextTypeRef () {
        return this.getFieldValue("textTypeRef");
    };


    /**
     * Get the column of the text as a string.
     * 
     * @return The column of the text as a string.
     */
    public String getTextColumn () {
        return this.getFieldValue("textColumn");
    };


    /**
     * Get the domain of the text as a string.
     * 
     * @return The domain of the text as a string.
     */
    public String getTextDomain () {
        return this.getFieldValue("textDomain");
    };


	/**
     * Get the availability of the text as a string.
     * 
     * @return The availability of the text as a string.
     */
    public String getAvailability () {
        return this.getFieldValue("availability");
    };
    

    /**
     * Get the file edition statement of the text as a string.
     * 
     * @return The file edition statement of the text as a string.
     */
    public String getFileEditionStatement () {
        return this.getFieldValue("fileEditionStatement");
    };


    /**
     * Get the bibliograhic edition statement of the text as a string.
     * 
     * @return The bibliograhic edition statement of the text as a
     *         string.
     */
    public String getBiblEditionStatement () {
        return this.getFieldValue("biblEditionStatement");
    };


    /**
     * Get the reference of the text as a string.
     * 
     * @return The reference of the text as a string.
     */
    public String getReference () {
        return this.getFieldValue("reference");
    };


    /**
     * Get the language of the text as a string.
     * 
     * @return The language of the text as a string.
     */
    public String getLanguage () {
        return this.getFieldValue("language");
    };


    /**
     * Get the corpus title of the text as a string.
     * 
     * @return The corpus title of the text as a string.
     */
    public String getCorpusTitle () {
        return this.getFieldValue("corpusTitle");
    };


    /**
     * Get the corpus subtitle of the text as a string.
     * 
     * @return The corpus subtitle of the text as a string.
     */
    public String getCorpusSubTitle () {
        return this.getFieldValue("corpusSubTitle");
    };


    /**
     * Get the corpus author of the text as a string.
     * 
     * @return The corpus author of the text as a string.
     */
    public String getCorpusAuthor () {
        return this.getFieldValue("corpusAuthor");
    };


    /**
     * Get the corpus editor of the text as a string.
     * 
     * @return The corpus editor of the text as a string.
     */
    public String getCorpusEditor () {
        return this.getFieldValue("corpusEditor");
    };


    /**
     * Get the document title of the text as a string.
     * 
     * @return The document title of the text as a string.
     */
    public String getDocTitle () {
        return this.getFieldValue("docTitle");
    };


    /**
     * Get the subtitle of the document of the text as a string.
     * 
     * @return The subtitle of the document of the text as a string.
     */
    public String getDocSubTitle () {
        return this.getFieldValue("docSubTitle");
    };


    /**
     * Get the author of the document of the text as a string.
     * 
     * @return The author of the document of the text as a string.
     */
    public String getDocAuthor () {
        return this.getFieldValue("docAuthor");
    };


    /**
     * Get the editor of the document of the text as a string.
     * 
     * @return The editor of the document of the text as a string.
     */
    public String getDocEditor () {
        return this.getFieldValue("docEditor");
    };


    /**
     * Get the keywords of the text as a string.
     * 
     * @return The keywords of the text as a string.
     */
    public String getKeywords () {
        return this.getFieldValue("keywords");
    };

    /**
     * Get information about the source of tokenization
     * as a string.
     * 
     * @return The tokenization information as a string.
     */
    public String getTokenSource () {
        return this.getFieldValue("tokenSource");
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
    private String getFieldValue (String field) {
        MetaField mf = mFields.get(field);

        if (mf != null) {
            return mFields.get(field).values.get(0);
        };

        return null;
    };

    @JsonIgnore
    public void addString (String key, String value) {
        mFields.add(
            key,
            new MetaField(
                key,
                "type:string",
                value
                )
            );
    };
    
    @JsonIgnore
    public void addStored (String key, String value) {
        mFields.add(
            key,
            new MetaField(
                key,
                "type:store",
                value
                )
            );
    };

    @JsonIgnore
    public void addKeywords (String key, String value) {
        mFields.add(
            key,
            new MetaField(
                key,
                "type:keywords",
                value
                )
            );
    };

    @JsonIgnore
    public void addText (String key, String value) {
        mFields.add(
            key,
            new MetaField(
                key,
                "type:text",
                value
                )
            );
    };

    @JsonIgnore
    public void addDate (String key, String value) {
        mFields.add(
            key,
            new MetaField(
                key,
                "type:date",
                value
                )
            );
    };

};
