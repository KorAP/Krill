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
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class AbstractDocument extends Response {
    ObjectMapper mapper = new ObjectMapper();

    private String primaryData;

    @JsonIgnore
    public int internalDocID, localDocID, UID;

    // private HashMap<String, String> fieldMap;

    private MetaFieldsExt metaFields = new MetaFieldsExt();

    // Deprecated
    private String ID, corpusID;

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

        // LEGACY
        if (fields.contains("corpusID"))
            this.setCorpusID(doc.get("corpusID"));
        if (fields.contains("ID"))
            this.setID(doc.get("ID"));

        // valid
        if (fields.contains("UID"))
            this.setUID(doc.get("UID"));
        if (fields.contains("author"))
            this.setAuthor(doc.get("author"));
        if (fields.contains("textClass"))
            this.setTextClass(doc.get("textClass"));
        if (fields.contains("title"))
            this.setTitle(doc.get("title"));
        if (fields.contains("subTitle"))
            this.setSubTitle(doc.get("subTitle"));
        if (fields.contains("pubDate"))
            this.setPubDate(doc.get("pubDate"));
        if (fields.contains("pubPlace"))
            this.setPubPlace(doc.get("pubPlace"));

        // Temporary (later meta fields in term vector)
        if (fields.contains("foundries"))
            this.setFoundries(doc.get("foundries"));

        // New fields
        if (fields.contains("textSigle"))
            this.setTextSigle(doc.get("textSigle"));
        if (fields.contains("docSigle"))
            this.setDocSigle(doc.get("docSigle"));
        if (fields.contains("corpusSigle"))
            this.setCorpusSigle(doc.get("corpusSigle"));
        if (fields.contains("layerInfos"))
            this.setLayerInfos(doc.get("layerInfos"));
        if (fields.contains("tokenSource"))
            this.setTokenSource(doc.get("tokenSource"));
        if (fields.contains("editor"))
            this.setEditor(doc.get("editor"));

        if (fields.contains("corpusAuthor"))
            this.setCorpusAuthor(doc.get("corpusAuthor"));
        if (fields.contains("corpusEditor"))
            this.setCorpusEditor(doc.get("corpusEditor"));
        if (fields.contains("corpusTitle"))
            this.setCorpusTitle(doc.get("corpusTitle"));
        if (fields.contains("corpusSubTitle"))
            this.setCorpusSubTitle(doc.get("corpusSubTitle"));

        if (fields.contains("docAuthor"))
            this.setDocAuthor(doc.get("docAuthor"));
        if (fields.contains("docEditor"))
            this.setDocEditor(doc.get("docEditor"));
        if (fields.contains("docTitle"))
            this.setDocTitle(doc.get("docTitle"));
        if (fields.contains("docSubTitle"))
            this.setDocSubTitle(doc.get("docSubTitle"));

        if (fields.contains("publisher"))
            this.setPublisher(doc.get("publisher"));
        if (fields.contains("reference"))
            this.setReference(doc.get("reference"));
        if (fields.contains("creationDate"))
            this.setCreationDate(doc.get("creationDate"));
        if (fields.contains("keywords"))
            this.setKeywords(doc.get("keywords"));
        if (fields.contains("textClass"))
            this.setTextClass(doc.get("textClass"));
        if (fields.contains("textColumn"))
            this.setTextColumn(doc.get("textColumn"));
        if (fields.contains("textDomain"))
            this.setTextDomain(doc.get("textDomain"));
        if (fields.contains("textType"))
            this.setTextType(doc.get("textType"));
        if (fields.contains("textTypeArt"))
            this.setTextTypeArt(doc.get("textTypeArt"));
        if (fields.contains("textTypeRef"))
            this.setTextTypeRef(doc.get("textTypeRef"));
        if (fields.contains("language"))
            this.setLanguage(doc.get("language"));

        if (fields.contains("biblEditionStatement"))
            this.setBiblEditionStatement(doc.get("biblEditionStatement"));
        if (fields.contains("fileEditionStatement"))
            this.setFileEditionStatement(doc.get("fileEditionStatement"));

        // Legacy
        if (fields.contains("license"))
            this.setAvailability(doc.get("license"));
		else if (fields.contains("availability"))
            this.setAvailability(doc.get("availability"));

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
        // this.setField(field);
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
     * Set the publication date of the document.
     * 
     * @param date
     *            The date as a {@link KrillDate} compatible string
     *            representation.
     * @return A {@link KrillDate} object for chaining.
     */
    public void setPubDate (String pubDate) {
        this.addDateX("pubDate", pubDate);
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
     * Set the creation date of the document.
     * 
     * @param date
     *            The date as a {@link KrillDate} compatible string
     *            representation.
     * @return A {@link KrillDate} object for chaining.
     */
    public void setCreationDate (String creationDate) {
        this.addDateX("creationDate", creationDate);
    };


    /**
     * Set the creation date of the document.
     * 
     * @param date
     *            The date as a {@link KrillDate} object.
     * @return A {@link KrillDate} object for chaining.
     */
    /*
    public KrillDate setCreationDate (KrillDate date) {
        return (this.creationDate = date);
    };
    */

    /**
     * Get the name of the author of the document.
     * 
     * @return The name of the author as a string.
     */
    public String getAuthor () {
        return this.getFieldValue("author");
    };


    /**
     * Set the name of the author of the document.
     * 
     * @param author
     *            The name of the author as a string.
     */
    public void setAuthor (String author) {
        this.addTextX("author", author);
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
     * Set the text class of the document.
     * 
     * @param textClass
     *            The text class of the document as a string.
     */
    public void setTextClass (String textClass) {
        this.addKeywordsX("textClass", textClass);
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
     * Set the publication place of the document.
     * 
     * @param pubPlace
     *            The publication place of the document as a string.
     */
    public void setPubPlace (String pubPlace) {
        this.addStringX("pubPlace", pubPlace);
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
        this.UID = UID;
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
     * Set the title of the document.
     * 
     * @param title
     *            The title of the document as a string.
     */
    public void setTitle (String title) {
        this.addTextX("title", title);
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
     * Set the subtitle of the document.
     * 
     * @param subTitle
     *            The subtitle of the document as a string.
     */
    public void setSubTitle (String subTitle) {
        this.addTextX("subTitle", subTitle);
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
     * Set information on the foundries the document
     * is annotated with.
     * 
     * @param foundries
     *            The foundry information string.
     */
    public void setFoundries (String foundries) {
        this.addKeywordsX("foundries", foundries);
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


    /**
     * Set information on the layers the document
     * is annotated with as a string.
     * 
     * @param layerInfos
     *            The layer information string.
     */
    public void setLayerInfos (String layerInfos) {
        this.addStoredX("layerInfos", layerInfos);
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


    // This is the new text id
    /**
     * Set the text sigle as a string.
     * 
     * @param textSigle
     *            The text sigle as a string.
     */
    public void setTextSigle (String textSigle) {
        this.addStringX("textSigle", textSigle);
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


    // This is the new corpus id
    /**
     * Set the corpus sigle as a string.
     * 
     * @param corpusSigle
     *            The corpus sigle as a string.
     */
    public void setCorpusSigle (String corpusSigle) {
        this.addStringX("corpusSigle", corpusSigle);
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
     * Set the document sigle as a string.
     * 
     * @param docSigle
     *            The document sigle as a string.
     */
    public void setDocSigle (String docSigle) {
        this.addStringX("docSigle", docSigle);
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
     * Set the name of the publisher as a string.
     * 
     * @param publisher
     *            The name of the publisher as a string.
     */
    public void setPublisher (String publisher) {
        this.addStoredX("publisher", publisher);
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
     * Set the name of the editor as a string.
     * 
     * @param editor
     *            The name of the editor as a string.
     */
    public void setEditor (String editor) {
        this.addStoredX("editor", editor);
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
     * Set the type of the text as a string.
     * 
     * @param textType
     *            The type of the text as a string.
     */
    public void setTextType (String textType) {
        this.addStringX("textType", textType);
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
     * Set the type art of the text as a string.
     * 
     * @param textTypeArt
     *            The type art of the text as a string.
     */
    public void setTextTypeArt (String textTypeArt) {
        this.addStringX("textTypeArt", textTypeArt);
    };


    /**
     * Set the type reference of the text as a string.
     * 
     * @param textTypeRef
     *            The type reference of the text as a string.
     */
    public void setTextTypeRef (String textTypeRef) {
        this.addStringX("textTypeRef", textTypeRef);
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
     * Set the column of the text as a string.
     * 
     * @param textColumn
     *            The column of the text as a string.
     */
    public void setTextColumn (String textColumn) {
        this.addStringX("textColumn", textColumn);
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
     * Set the domain of the text as a string.
     * 
     * @param textDomain
     *            The domain of the text as a string.
     */
    public void setTextDomain (String textDomain) {
        this.addStringX("textDomain", textDomain);
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
     * Set the availability of the text as a string.
     * 
     * @param availability
     *            The availability of the text as a string.
     */
    public void setAvailability (String availability) {
        this.addStringX("availability", availability);
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
     * Set the file edition statement of the text as a string.
     * 
     * @param fileEditionStatement
     *            The file edition statement
     *            of the text as a string.
     */
    public void setFileEditionStatement (String fileEditionStatement) {
        this.addStoredX("fileEditionStatement", fileEditionStatement);
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
     * Set the bibliograhic edition statement of the text as a string.
     * 
     * @param biblEditionStatement
     *            The bibliograhic edition statement
     *            of the text as a string.
     */
    public void setBiblEditionStatement (String biblEditionStatement) {
        this.addStoredX("biblEditionStatement", biblEditionStatement);
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
     * Set the reference of the text as a string.
     * 
     * @param reference
     *            The reference of the text as a string.
     */
    public void setReference (String reference) {
        this.addStoredX("reference", reference);
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
     * Set the language of the text as a string.
     * 
     * @param language
     *            The language of the text as a string.
     */
    public void setLanguage (String language) {
        this.addStringX("language", language);
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
     * Set the corpus title of the text as a string.
     * 
     * @param corpusTitle
     *            The corpus title of the text as a string.
     */
    public void setCorpusTitle (String corpusTitle) {
        this.addTextX("corpusTitle", corpusTitle);
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
     * Set the corpus subtitle of the text as a string.
     * 
     * @param corpusSubTitle
     *            The corpus subtitle of the
     *            text as a string.
     */
    public void setCorpusSubTitle (String corpusSubTitle) {
        this.addTextX("corpusSubTitle", corpusSubTitle);
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
     * Set the corpus author of the text as a string.
     * 
     * @return The corpus author of the text as a string.
     */
    public void setCorpusAuthor (String corpusAuthor) {
        this.addTextX("corpusAuthor", corpusAuthor);
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
     * Set the corpus editor of the text as a string.
     * 
     * @param corpusEditor
     *            The corpus editor of the text as a string.
     */
    public void setCorpusEditor (String corpusEditor) {
        this.addStoredX("corpusEditor", corpusEditor);
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
     * Set the document title of the text as a string.
     * 
     * @param docTitle
     *            The document title of the text as a string.
     */
    public void setDocTitle (String docTitle) {
        this.addTextX("docTitle", docTitle);
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
     * Set the subtitle of the document of the text as a string.
     * 
     * @param docSubTitle
     *            The subtitle of the document of the
     *            text as a string.
     */
    public void setDocSubTitle (String docSubTitle) {
        this.addTextX("docSubTitle", docSubTitle);
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
     * Set the author of the document of the text as a string.
     * 
     * @param docAuthor
     *            The author of the document of the text as a string.
     */
    public void setDocAuthor (String docAuthor) {
        this.addTextX("docAuthor", docAuthor);
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
     * Set the editor of the document of the text as a string.
     * 
     * @param docEditor
     *            The editor of the document of the text as a string.
     */
    public void setDocEditor (String docEditor) {
        this.addStoredX("docEditor", docEditor);
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
     * Set the keywords of the text as a string.
     * 
     * @param keywords
     *            The keywords of the text as a string.
     */
    public void setKeywords (String keywords) {
        this.addKeywordsX("keywords", keywords);
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


    /**
     * Set information about the source of tokenization
     * as a string.
     * 
     * @param tokenSource
     *            The tokenization information as a string.
     */
    public void setTokenSource (String tokenSource) {
        this.addStoredX("tokenSource", tokenSource);
    };


    @Deprecated
    @JsonProperty("corpusID")
    public String getCorpusID () {
        return this.corpusID;
    };


    @Deprecated
    public void setCorpusID (String corpusID) {
        this.corpusID = corpusID;
    };


    @Deprecated
    @JsonProperty("ID")
    public String getID () {
        return this.ID;
    };


    @Deprecated
    public void setID (String ID) {
        this.ID = ID;
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
        MetaField mf = metaFields.get(field);

        if (mf != null) {
            return metaFields.get(field).values.get(0);
        };

        return null;
    };

    @JsonIgnore
    private void addStringX (String key, String value) {
        metaFields.add(
            key,
            new MetaField(
                key,
                "type:string",
                value
                )
            );
    };
    
    @JsonIgnore
    private void addStoredX (String key, String value) {
        metaFields.add(
            key,
            new MetaField(
                key,
                "type:attachement",
                value
                )
            );
    };

    @JsonIgnore
    private void addKeywordsX (String key, String value) {
        metaFields.add(
            key,
            new MetaField(
                key,
                "type:keywords",
                value
                )
            );
    };

    @JsonIgnore
    private void addTextX (String key, String value) {
        metaFields.add(
            key,
            new MetaField(
                key,
                "type:text",
                value
                )
            );
    };

    @JsonIgnore
    private void addDateX (String key, String value) {
        metaFields.add(
            key,
            new MetaField(
                key,
                "type:date",
                value
                )
            );
    };

};
