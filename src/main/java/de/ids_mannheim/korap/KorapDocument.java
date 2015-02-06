package de.ids_mannheim.korap;

import java.util.*;

import de.ids_mannheim.korap.util.KorapDate;
import de.ids_mannheim.korap.index.FieldDocument;
import de.ids_mannheim.korap.response.KorapResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.*;

/*
 * Todo:: Author and textClass may be arrays!
 */

/**
 * Abstract class representing a document in the
 * KorAP index.
 *
 * This model is rather specific to DeReKo data and
 * should be considered experimental. It may be replaced
 * by a more agnostic model.
 * string fields, e.g. may be combined with a prefix.
 *
 * @author diewald
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class KorapDocument extends KorapResponse {
    private String primaryData;

    @JsonIgnore
    public int
        internalDocID,
        localDocID,
        UID;

    private KorapDate
        pubDate,
        // newly added
        creationDate;

    private String

        // No longer supported
        ID,
        corpusID,
        field,
        layerInfo,
        tokenization,

        // Still supported
        foundries,
        textClass,
        pubPlace,

        // Newly added for the corpus/doc/text distinction of DeReKo
        textSigle, docSigle, corpusSigle,
        title,       subTitle,       author,       editor,
        docTitle,    docSubTitle,    docAuthor,    docEditor,
        corpusTitle, corpusSubTitle, corpusAuthor, corpusEditor,
        textType, textTypeArt, textTypeRef, textColumn, textDomain,
        fileEditionStatement, biblEditionStatement,
        publisher,
        reference,
        language,
        license,
        pages,
        keywords,

        // Meta information regarding annotations
        tokenSource,
        layerInfos;


    /**
     * Get the publication date of the document
     * as a {@link KorapDate} object.
     *
     * @return A {@link KorapDate} object for chaining.
     */
    @JsonIgnore
    public KorapDate getPubDate () {
        return this.pubDate;
    };


    /**
     * Get the publication date of the document
     * as a string.
     *
     * @return A string containing the {@link KorapDate}.
     */    
    @JsonProperty("pubDate")
    public String getPubDateString () {
        if (this.pubDate != null)
            return this.pubDate.toDisplay();
        return null;
    };


    /**
     * Set the publication date of the document.
     *
     * @param date The date as a {@link KorapDate}
     * compatible string representation.
     * @return A {@link KorapDate} object for chaining.
     */
    public KorapDate setPubDate (String date) {
        this.pubDate = new KorapDate(date);
        return this.pubDate;
    };


    /**
     * Set the publication date of the document.
     *
     * @param date The date as a {@link KorapDate} object.
     * @return A {@link KorapDate} object for chaining.
     */
    public KorapDate setPubDate (KorapDate date) {
        return (this.pubDate = date);
    };


    /**
     * Get the creation date of the document
     * as a {@link KorapDate} object.
     *
     * @return A {@link KorapDate} object for chaining.
     */
    @JsonIgnore
    public KorapDate getCreationDate () {
        return this.creationDate;
    };


    /**
     * Get the creation date of the document
     * as a string.
     *
     * @return A string containing the {@link KorapDate}.
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
     * @param date The date as a {@link KorapDate}
     * compatible string representation.
     * @return A {@link KorapDate} object for chaining.
     */
    public KorapDate setCreationDate (String date) {
        this.creationDate = new KorapDate(date);
        return this.creationDate;
    };


    /**
     * Set the creation date of the document.
     *
     * @param date The date as a {@link KorapDate} object.
     * @return A {@link KorapDate} object for chaining.
     */
    public KorapDate setCreationDate (KorapDate date) {
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
     * @param author The name of the author as a string.
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
     * @param textClass The text class of the document as a string.
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
     * @param pubPlace The publication place of the document as a string.
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
     * @param UID The unique identifier of the document as an integer.
     */
    public void setUID (int UID) {
        this.UID = UID;
    };


    /**
     * Set the unique identifier of the document.
     *
     * @param UID The unique identifier of the document as a
     *        string representing an integer.
     * @throws NumberFormatException
     */
    public void setUID (String UID) throws NumberFormatException {
        if (UID != null)
            this.UID = Integer.parseInt(UID);
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
     * @param title The title of the document as a string.
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
     * @param subTitle The subtitle of the document as a string.
     */
    public void setSubTitle (String subTitle) {
        this.subTitle = subTitle;
    };


    /**
     * Get the primary data of the document.
     *
     * @return The primary data of the document as a string.
     */
    public String getPrimaryData () {
        if (this.primaryData == null)
            return "";
        return this.primaryData;
    };


    /**
     * Get the primary data of the document,
     * starting with a given character offset.
     *
     * @param startOffset The starting character offset.
     * @return The substring of primary data of the document as a string.
     */
    public String getPrimaryData (int startOffset) {
        return this.primaryData.substring(startOffset);
    };


    /**
     * Get the primary data of the document,
     * starting with a given character offset and ending
     * with a given character offset.
     *
     * @param startOffset The starting character offset.
     * @param endOffset The ending character offset.
     * @return The substring of the primary data of the document as a string.
     */
    public String getPrimaryData (int startOffset, int endOffset) {
        return this.primaryData.substring(startOffset, endOffset);
    };


    /**
     * Set the primary data of the document.
     *
     * @param primary The primary data of the document
     *        as a string.
     */
    public void setPrimaryData (String primary) {
        this.primaryData = primary;
    };

    /**
     * Get the length of the primary data of the document
     * (i.e. the number of characters).
     *
     * @return The length of the primary data of the document as an integer.
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
     * @param foundries The foundry information string.
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
     * @param layerInfos The layer information string.
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
     * @param textSigle The text sigle as a string.
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
     * @param corpusSigle The corpus sigle as a string.
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
     * @param docSigle The document sigle as a string.
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
     * @param publisher The name of the publisher as a string.
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
     * @param editor The name of the editor as a string.
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
     * @param textType The type of the text as a string.
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
     * @param textTypeArt The type art of the text as a string.
     */
    public void setTextTypeArt (String textTypeArt) {
        this.textTypeArt = textTypeArt;
    };


    /**
     * Set the type reference of the text as a string. 
     *
     * @param textTypeRef The type reference of the text as a string.
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
     * @param textColumn The column of the text as a string.
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
     * @param textDomain The domain of the text as a string.
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
     * @param license The license of the text as a string.
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
     * @param pages The page numbers of the text as a string.
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
     * @param fileEditionStatement The file edition statement
     *        of the text as a string.
     */
    public void setFileEditionStatement (String fileEditionStatement) {
        this.fileEditionStatement = fileEditionStatement;
    };


    /**
     * Get the bibliograhic edition statement of the text as a string. 
     *
     * @return The bibliograhic edition statement of the text as a string.
     */
    public String getBiblEditionStatement () {
        return this.biblEditionStatement;
    };


    /**
     * Set the bibliograhic edition statement of the text as a string. 
     *
     * @param biblEditionStatement The bibliograhic edition statement
     *        of the text as a string.
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
     * @param reference The reference of the text as a string.
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
     * @param language The language of the text as a string.
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
     * @param corpusTitle The corpus title of the text as a string.
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
     * @param corpusSubTitle The corpus subtitle of the
     *        text as a string.
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
     * @param corpusEditor The corpus editor of the text as a string.
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
     * @param docTitle The document title of the text as a string.
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
     * @param docSubTitle The subtitle of the document of the
     *        text as a string.
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
     * @param docAuthor The author of the document of the text as a string.
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
     * @param docEditor The editor of the document of the text as a string.
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
     * @param keywords The keywords of the text as a string.
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
     * @param tokenSource The tokenization information as a string.
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
};
