package de.ids_mannheim.korap.index;

import java.util.*;

import de.ids_mannheim.korap.util.KrillDate;
import de.ids_mannheim.korap.index.FieldDocument;
import de.ids_mannheim.korap.response.Response;

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

    private KrillDate pubDate,
            // newly added
            creationDate;

    private HashMap<String, String> fieldMap;

    private String

    // No longer supported
    ID, corpusID, field, layerInfo, tokenization,

            // Still supported
            foundries, textClass, pubPlace,

            // Newly added for the corpus/doc/text distinction of DeReKo
            textSigle, docSigle, corpusSigle, title, subTitle, author, editor,
            docTitle, docSubTitle, docAuthor, docEditor, corpusTitle,
            corpusSubTitle, corpusAuthor, corpusEditor, textType, textTypeArt,
            textTypeRef, textColumn, textDomain, fileEditionStatement,
            biblEditionStatement, publisher, reference, language, license,
            pages, keywords,

            // Meta information regarding annotations
            tokenSource, layerInfos;


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
        if (fields.contains("tokenization"))
            this.setTokenization(doc.get("tokenization"));
        if (fields.contains("layerInfo"))
            this.setLayerInfo(doc.get("layerInfo"));

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
        if (fields.contains("license"))
            this.setLicense(doc.get("license"));
        if (fields.contains("pages"))
            this.setPages(doc.get("pages"));

        if (fields.contains("biblEditionStatement"))
            this.setBiblEditionStatement(doc.get("biblEditionStatement"));
        if (fields.contains("fileEditionStatement"))
            this.setFileEditionStatement(doc.get("fileEditionStatement"));
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
        this.setField(field);
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
        return this.pubDate;
    };


    /**
     * Get the publication date of the document
     * as a string.
     * 
     * @return A string containing the {@link KrillDate}.
     */
    @JsonProperty("pubDate")
    public String getPubDateString () {
        if (this.pubDate != null) {
            String date = this.pubDate.toDisplay();
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
    public KrillDate setPubDate (String date) {
        this.pubDate = new KrillDate(date);
        return this.pubDate;
    };


    /**
     * Set the publication date of the document.
     * 
     * @param date
     *            The date as a {@link KrillDate} object.
     * @return A {@link KrillDate} object for chaining.
     */
    public KrillDate setPubDate (KrillDate date) {
        return (this.pubDate = date);
    };


    /**
     * Get the creation date of the document
     * as a {@link KrillDate} object.
     * 
     * @return A {@link KrillDate} object for chaining.
     */
    @JsonIgnore
    public KrillDate getCreationDate () {
        return this.creationDate;
    };


    /**
     * Get the creation date of the document
     * as a string.
     * 
     * @return A string containing the {@link KrillDate}.
     */
    @JsonProperty("creationDate")
    public String getCreationDateString () {
        if (this.creationDate != null)
            return this.creationDate.toDisplay();
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
    public KrillDate setCreationDate (String date) {
        this.creationDate = new KrillDate(date);
        return this.creationDate;
    };


    /**
     * Set the creation date of the document.
     * 
     * @param date
     *            The date as a {@link KrillDate} object.
     * @return A {@link KrillDate} object for chaining.
     */
    public KrillDate setCreationDate (KrillDate date) {
        return (this.creationDate = date);
    };


    /**
     * Get the name of the author of the document.
     * 
     * @return The name of the author as a string.
     */
    public String getAuthor () {
        return this.author;
    };


    /**
     * Set the name of the author of the document.
     * 
     * @param author
     *            The name of the author as a string.
     */
    public void setAuthor (String author) {
        this.author = author;
    };


    /**
     * Get the text class of the document.
     * 
     * @return The text class of the document as a string.
     */
    public String getTextClass () {
        return this.textClass;
    };


    /**
     * Set the text class of the document.
     * 
     * @param textClass
     *            The text class of the document as a string.
     */
    public void setTextClass (String textClass) {
        this.textClass = textClass;
    };


    /**
     * Get the publication place of the document.
     * 
     * @return The publication place of the document as a string.
     */
    public String getPubPlace () {
        return this.pubPlace;
    };


    /**
     * Set the publication place of the document.
     * 
     * @param pubPlace
     *            The publication place of the document as a string.
     */
    public void setPubPlace (String pubPlace) {
        this.pubPlace = pubPlace;
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
        return this.title;
    };


    /**
     * Set the title of the document.
     * 
     * @param title
     *            The title of the document as a string.
     */
    public void setTitle (String title) {
        this.title = title;
    };


    /**
     * Get the subtitle of the document.
     * 
     * @return The subtitle of the document as a string.
     */
    public String getSubTitle () {
        return this.subTitle;
    };


    /**
     * Set the subtitle of the document.
     * 
     * @param subTitle
     *            The subtitle of the document as a string.
     */
    public void setSubTitle (String subTitle) {
        this.subTitle = subTitle;
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
        return this.foundries;
    };


    /**
     * Set information on the foundries the document
     * is annotated with.
     * 
     * @param foundries
     *            The foundry information string.
     */
    public void setFoundries (String foundries) {
        this.foundries = foundries;
    };


    /**
     * Get information on the layers the document
     * is annotated with as a string.
     * 
     * @return The layer information string.
     */
    public String getLayerInfos () {
        return this.layerInfos;
    };


    /**
     * Set information on the layers the document
     * is annotated with as a string.
     * 
     * @param layerInfos
     *            The layer information string.
     */
    public void setLayerInfos (String layerInfos) {
        this.layerInfos = layerInfos;
    };


    // This is the new text id
    /**
     * Get the text sigle as a string.
     * 
     * @return The text sigle as a string.
     */
    public String getTextSigle () {
        return this.textSigle;
    };


    // This is the new text id
    /**
     * Set the text sigle as a string.
     * 
     * @param textSigle
     *            The text sigle as a string.
     */
    public void setTextSigle (String textSigle) {
        this.textSigle = textSigle;
    };


    // This is the new corpus id
    /**
     * Get the corpus sigle as a string.
     * 
     * @return The corpus sigle as a string.
     */
    public String getCorpusSigle () {
        return this.corpusSigle;
    };


    // This is the new corpus id
    /**
     * Set the corpus sigle as a string.
     * 
     * @param corpusSigle
     *            The corpus sigle as a string.
     */
    public void setCorpusSigle (String corpusSigle) {
        this.corpusSigle = corpusSigle;
    };


    /**
     * Get the document sigle as a string.
     * 
     * @return The document sigle as a string.
     */
    public String getDocSigle () {
        return this.docSigle;
    };


    /**
     * Set the document sigle as a string.
     * 
     * @param docSigle
     *            The document sigle as a string.
     */
    public void setDocSigle (String docSigle) {
        this.docSigle = docSigle;
    };


    /**
     * Get the name of the publisher as a string.
     * 
     * @return The name of the publisher as a string.
     */
    public String getPublisher () {
        return this.publisher;
    };


    /**
     * Set the name of the publisher as a string.
     * 
     * @param publisher
     *            The name of the publisher as a string.
     */
    public void setPublisher (String publisher) {
        this.publisher = publisher;
    };


    /**
     * Get the name of the editor as a string.
     * 
     * @return The name of the editor as a string.
     */
    public String getEditor () {
        return this.editor;
    };


    /**
     * Set the name of the editor as a string.
     * 
     * @param editor
     *            The name of the editor as a string.
     */
    public void setEditor (String editor) {
        this.editor = editor;
    };


    /**
     * Get the type of the text as a string.
     * 
     * @return The type of the text as a string.
     */
    public String getTextType () {
        return this.textType;
    };


    /**
     * Set the type of the text as a string.
     * 
     * @param textType
     *            The type of the text as a string.
     */
    public void setTextType (String textType) {
        this.textType = textType;
    };


    /**
     * Get the type art of the text as a string.
     * 
     * @return The type art of the text as a string.
     */
    public String getTextTypeArt () {
        return this.textTypeArt;
    };


    /**
     * Set the type art of the text as a string.
     * 
     * @param textTypeArt
     *            The type art of the text as a string.
     */
    public void setTextTypeArt (String textTypeArt) {
        this.textTypeArt = textTypeArt;
    };


    /**
     * Set the type reference of the text as a string.
     * 
     * @param textTypeRef
     *            The type reference of the text as a string.
     */
    public void setTextTypeRef (String textTypeRef) {
        this.textTypeRef = textTypeRef;
    };


    /**
     * Get the type reference of the text as a string.
     * 
     * @return The type reference of the text as a string.
     */
    public String getTextTypeRef () {
        return this.textTypeRef;
    };


    /**
     * Get the column of the text as a string.
     * 
     * @return The column of the text as a string.
     */
    public String getTextColumn () {
        return this.textColumn;
    };


    /**
     * Set the column of the text as a string.
     * 
     * @param textColumn
     *            The column of the text as a string.
     */
    public void setTextColumn (String textColumn) {
        this.textColumn = textColumn;
    };


    /**
     * Get the domain of the text as a string.
     * 
     * @return The domain of the text as a string.
     */
    public String getTextDomain () {
        return this.textDomain;
    };


    /**
     * Set the domain of the text as a string.
     * 
     * @param textDomain
     *            The domain of the text as a string.
     */
    public void setTextDomain (String textDomain) {
        this.textDomain = textDomain;
    };


    /**
     * Get the license of the text as a string.
     * 
     * @return The license of the text as a string.
     */
    public String getLicense () {
        return this.license;
    };


    /**
     * Set the license of the text as a string.
     * 
     * @param license
     *            The license of the text as a string.
     */
    public void setLicense (String license) {
        this.license = license;
    };


    /**
     * Get the page numbers of the text as a string.
     * 
     * @return The page numbers of the text as a string.
     */
    public String getPages () {
        return this.pages;
    };


    /**
     * Set the page numbers of the text as a string.
     * 
     * @param pages
     *            The page numbers of the text as a string.
     */
    public void setPages (String pages) {
        this.pages = pages;
    };


    /**
     * Get the file edition statement of the text as a string.
     * 
     * @return The file edition statement of the text as a string.
     */
    public String getFileEditionStatement () {
        return this.fileEditionStatement;
    };


    /**
     * Set the file edition statement of the text as a string.
     * 
     * @param fileEditionStatement
     *            The file edition statement
     *            of the text as a string.
     */
    public void setFileEditionStatement (String fileEditionStatement) {
        this.fileEditionStatement = fileEditionStatement;
    };


    /**
     * Get the bibliograhic edition statement of the text as a string.
     * 
     * @return The bibliograhic edition statement of the text as a
     *         string.
     */
    public String getBiblEditionStatement () {
        return this.biblEditionStatement;
    };


    /**
     * Set the bibliograhic edition statement of the text as a string.
     * 
     * @param biblEditionStatement
     *            The bibliograhic edition statement
     *            of the text as a string.
     */
    public void setBiblEditionStatement (String biblEditionStatement) {
        this.biblEditionStatement = biblEditionStatement;
    };


    /**
     * Get the reference of the text as a string.
     * 
     * @return The reference of the text as a string.
     */
    public String getReference () {
        return this.reference;
    };


    /**
     * Set the reference of the text as a string.
     * 
     * @param reference
     *            The reference of the text as a string.
     */
    public void setReference (String reference) {
        this.reference = reference;
    };


    /**
     * Get the language of the text as a string.
     * 
     * @return The language of the text as a string.
     */
    public String getLanguage () {
        return this.language;
    };


    /**
     * Set the language of the text as a string.
     * 
     * @param language
     *            The language of the text as a string.
     */
    public void setLanguage (String language) {
        this.language = language;
    };


    /**
     * Get the corpus title of the text as a string.
     * 
     * @return The corpus title of the text as a string.
     */
    public String getCorpusTitle () {
        return this.corpusTitle;
    };


    /**
     * Set the corpus title of the text as a string.
     * 
     * @param corpusTitle
     *            The corpus title of the text as a string.
     */
    public void setCorpusTitle (String corpusTitle) {
        this.corpusTitle = corpusTitle;
    };


    /**
     * Get the corpus subtitle of the text as a string.
     * 
     * @return The corpus subtitle of the text as a string.
     */
    public String getCorpusSubTitle () {
        return this.corpusSubTitle;
    };


    /**
     * Set the corpus subtitle of the text as a string.
     * 
     * @param corpusSubTitle
     *            The corpus subtitle of the
     *            text as a string.
     */
    public void setCorpusSubTitle (String corpusSubTitle) {
        this.corpusSubTitle = corpusSubTitle;
    };


    /**
     * Get the corpus author of the text as a string.
     * 
     * @return The corpus author of the text as a string.
     */
    public String getCorpusAuthor () {
        return this.corpusAuthor;
    };


    /**
     * Set the corpus author of the text as a string.
     * 
     * @return The corpus author of the text as a string.
     */
    public void setCorpusAuthor (String corpusAuthor) {
        this.corpusAuthor = corpusAuthor;
    };


    /**
     * Get the corpus editor of the text as a string.
     * 
     * @return The corpus editor of the text as a string.
     */
    public String getCorpusEditor () {
        return this.corpusEditor;
    };


    /**
     * Set the corpus editor of the text as a string.
     * 
     * @param corpusEditor
     *            The corpus editor of the text as a string.
     */
    public void setCorpusEditor (String corpusEditor) {
        this.corpusEditor = corpusEditor;
    };


    /**
     * Get the document title of the text as a string.
     * 
     * @return The document title of the text as a string.
     */
    public String getDocTitle () {
        return this.docTitle;
    };


    /**
     * Set the document title of the text as a string.
     * 
     * @param docTitle
     *            The document title of the text as a string.
     */
    public void setDocTitle (String docTitle) {
        this.docTitle = docTitle;
    };


    /**
     * Get the subtitle of the document of the text as a string.
     * 
     * @return The subtitle of the document of the text as a string.
     */
    public String getDocSubTitle () {
        return this.docSubTitle;
    };


    /**
     * Set the subtitle of the document of the text as a string.
     * 
     * @param docSubTitle
     *            The subtitle of the document of the
     *            text as a string.
     */
    public void setDocSubTitle (String docSubTitle) {
        this.docSubTitle = docSubTitle;
    };


    /**
     * Get the author of the document of the text as a string.
     * 
     * @return The author of the document of the text as a string.
     */
    public String getDocAuthor () {
        return this.docAuthor;
    };


    /**
     * Set the author of the document of the text as a string.
     * 
     * @param docAuthor
     *            The author of the document of the text as a string.
     */
    public void setDocAuthor (String docAuthor) {
        this.docAuthor = docAuthor;
    };


    /**
     * Get the editor of the document of the text as a string.
     * 
     * @return The editor of the document of the text as a string.
     */
    public String getDocEditor () {
        return this.docEditor;
    };


    /**
     * Set the editor of the document of the text as a string.
     * 
     * @param docEditor
     *            The editor of the document of the text as a string.
     */
    public void setDocEditor (String docEditor) {
        this.docEditor = docEditor;
    };


    /**
     * Get the keywords of the text as a string.
     * 
     * @return The keywords of the text as a string.
     */
    public String getKeywords () {
        return this.keywords;
    };


    /**
     * Set the keywords of the text as a string.
     * 
     * @param keywords
     *            The keywords of the text as a string.
     */
    public void setKeywords (String keywords) {
        this.keywords = keywords;
    };


    /**
     * Get information about the source of tokenization
     * as a string.
     * 
     * @return The tokenization information as a string.
     */
    public String getTokenSource () {
        return this.tokenSource;
    };


    /**
     * Set information about the source of tokenization
     * as a string.
     * 
     * @param tokenSource
     *            The tokenization information as a string.
     */
    public void setTokenSource (String tokenSource) {
        this.tokenSource = tokenSource;
    };


    @Deprecated
    public void setTokenization (String tokenization) {
        this.tokenization = tokenization;
    };


    @Deprecated
    public String getTokenization () {
        return this.tokenization;
    };


    @Deprecated
    public void setLayerInfo (String layerInfo) {
        this.layerInfo = layerInfo;
    };


    @Deprecated
    public String getLayerInfo () {
        return this.layerInfo;
    };


    @Deprecated
    public void setField (String field) {
        this.field = field;
    };


    @Deprecated
    public String getField () {
        return this.field;
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
};
