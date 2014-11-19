package de.ids_mannheim.korap;

import java.util.*;

import de.ids_mannheim.korap.util.KorapDate;
import de.ids_mannheim.korap.document.KorapPrimaryData;
import de.ids_mannheim.korap.index.FieldDocument;
import de.ids_mannheim.korap.response.KorapResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.*;

/* Todo:: Author and textClass may be arrays! */

/**
 * Abstract class representing a document in the KorAP index.
 *
 * @author Nils Diewald
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class KorapDocument extends KorapResponse {
    private KorapPrimaryData primaryData;

    @JsonIgnore
    public int
	internalDocID,
	localDocID,
	UID;

    private KorapDate
	pubDate,
    // newly added
	creationDate
	;


    private String
    // No longer supported
	ID,
	corpusID,
	field,
	layerInfo,
	tokenization,

    // Still supported
	foundries,
	title,
	subTitle,
	author,
	textClass,
	pubPlace,

    // newly added
	textSigle,
	docSigle,
	corpusSigle,
	publisher,
	editor,
	textType,
	textTypeArt,
	textTypeRef,
	textColumn,
	textDomain,
	license,
	pages,
	fileEditionStatement,
	biblEditionStatement,
	reference,
	language,
	corpusTitle,
	corpusSubTitle,
	corpusAuthor,
	corpusEditor,
	docTitle,
	docSubTitle,
	docAuthor,
	docEditor,
	keywords,
	tokenSource,
	layerInfos
	;


    /**
     * Set the publication date of the document the match occurs in.
     *
     * @param date The date as a KorapDate compatible string representation.
     * @return A KorapDate object for chaining.
     * @see KorapDate#Constructor(String)
     */
    public KorapDate setPubDate (String date) {
	this.pubDate = new KorapDate(date);
	return this.pubDate;
    };

    /**
     * Set the creation date of the document the match occurs in.
     *
     * @param date The date as a KorapDate compatible string representation.
     * @return A KorapDate object for chaining.
     * @see KorapDate#Constructor(String)
     */
    public KorapDate setCreationDate (String date) {
	this.creationDate = new KorapDate(date);
	return this.creationDate;
    };


    /**
     * Set the publication date of the document the match occurs in.
     *
     * @param date The date as a KorapDate object.
     * @return A KorapDate object for chaining.
     * @see KorapDate
     */
    public KorapDate setPubDate (KorapDate date) {
	return (this.pubDate = date);
    };


    /**
     * Set the creation date of the document the match occurs in.
     *
     * @param date The date as a KorapDate object.
     * @return A KorapDate object for chaining.
     * @see KorapDate
     */
    public KorapDate setCreationDate (KorapDate date) {
	return (this.creationDate = date);
    };


    /**
     * Get the publication date of the document the match occurs in as a KorapDate object.
     */
    @JsonIgnore
    public KorapDate getPubDate () {
	return this.pubDate;
    };


    /**
     * Get the creation date of the document the match occurs in as a KorapDate object.
     */
    @JsonIgnore
    public KorapDate getCreationDate () {
	return this.creationDate;
    };

    @JsonProperty("pubDate")
    public String getPubDateString () {
	if (this.pubDate != null)
	    return this.pubDate.toDisplay();
	return null;
    };

    @JsonProperty("creationDate")
    public String getCreationDateString () {
	if (this.creationDate != null)
	    return this.creationDate.toDisplay();
	return null;
    };

    public void setAuthor (String author) {
	this.author = author;
    };

    public String getAuthor () {
	return this.author;
    };

    public void setTextClass (String textClass) {
	this.textClass = textClass;
    };

    public String getTextClass () {
	return this.textClass;
    };

    public void setPubPlace (String pubPlace) {
	this.pubPlace = pubPlace;
    };

    public String getPubPlace () {
	return this.pubPlace;
    };

    // No longer supported
    public void setCorpusID (String corpusID) {
	this.corpusID = corpusID;
    };

    // No longer supported
    @JsonProperty("corpusID")
    public String getCorpusID () {
	return this.corpusID;
    };

    // No longer supported
    public void setID (String ID) {
	this.ID = ID;
    };

    // No longer supported
    @JsonProperty("ID")
    public String getID () {
	return this.ID;
    };

    public void setUID (int UID) {
	this.UID = UID;
    };

    public void setUID (String UID) {
	if (UID != null)
	    this.UID = Integer.parseInt(UID);
    };


    @JsonProperty("UID")
    public int getUID () {
	return this.UID;
    };

    public void setTitle (String title) {
	this.title = title;
    };

    public String getTitle () {
	return this.title;
    };

    public void setSubTitle (String subTitle) {
	this.subTitle = subTitle;
    };

    public String getSubTitle () {
	return this.subTitle;
    };

    @JsonIgnore
    public void setPrimaryData (String primary) {
	this.primaryData = new KorapPrimaryData(primary);
    };

    public void setPrimaryData (KorapPrimaryData primary) {
	this.primaryData = primary;
    };

    public String getPrimaryData () {
	if (this.primaryData == null)
	    return "";
	return this.primaryData.toString();
    };

    public String getPrimaryData (int startOffset) {
	return this.primaryData.substring(startOffset);
    };

    public String getPrimaryData (int startOffset, int endOffset) {
	return this.primaryData.substring(startOffset, endOffset);
    };

    @JsonIgnore
    public int getPrimaryDataLength () {
	return this.primaryData.length();
    };

    public void setFoundries (String foundries) {
	this.foundries = foundries;
    };

    public String getFoundries () {
	return this.foundries;
    };

    // No longer supported
    public void setTokenization (String tokenization) {
	this.tokenization = tokenization;
    };

    // No longer supported
    public String getTokenization () {
	return this.tokenization;
    };

    // No longer supported
    public void setLayerInfo (String layerInfo) {
	this.layerInfo = layerInfo;
    };

    // No longer supported
    public String getLayerInfo () {
	return this.layerInfo;
    };

    public void setLayerInfos (String layerInfos) {
	this.layerInfos = layerInfos;
    };

    public String getLayerInfos () {
	return this.layerInfos;
    };

    // No longer necessary
    public void setField (String field) {
	this.field = field;
    };

    // No longer necessary
    public String getField () {
	return this.field;
    };

    // This is the new text id
    public String getTextSigle () {
	return this.textSigle;
    };

    // This is the new text id
    public void setTextSigle (String textSigle) {
	this.textSigle = textSigle;
    };

    // This is the new corpus id
    public String getCorpusSigle () {
	return this.corpusSigle;
    };

    // This is the new corpus id
    public void setCorpusSigle (String corpusSigle) {
	this.corpusSigle = corpusSigle;
    };

    public String getDocSigle () {
	return this.docSigle;
    };

    public void setDocSigle (String docSigle) {
	this.docSigle = docSigle;
    };

    public String getPublisher () {
	return this.publisher;
    };

    public void setPublisher (String publisher) {
	this.publisher = publisher;
    };

    public String getEditor () {
	return this.editor;
    };

    public void setEditor (String editor) {
	this.editor = editor;
    };

    public String getTextType () {
	return this.textType;
    };

    public void setTextType (String textType) {
	this.textType = textType;
    };

    public String getTextTypeArt () {
	return this.textTypeArt;
    };

    public void setTextTypeArt (String textTypeArt) {
	this.textTypeArt = textTypeArt;
    };

    public String getTextTypeRef () {
	return this.textTypeRef;
    };

    public void setTextTypeRef (String textTypeRef) {
	this.textTypeRef = textTypeRef;
    };

    public String getTextColumn () {
	return this.textColumn;
    };

    public void setTextColumn (String textColumn) {
	this.textColumn = textColumn;
    };

    public String getTextDomain () {
	return this.textDomain;
    };

    public void setTextDomain (String textDomain) {
	this.textDomain = textDomain;
    };

    public String getLicense () {
	return this.license;
    };

    public void setLicense (String license) {
	this.license = license;
    };

    public String getPages () {
	return this.pages;
    };

    public void setPages (String pages) {
	this.pages = pages;
    };

    public String getFileEditionStatement () {
	return this.fileEditionStatement;
    };

    public void setFileEditionStatement (String fileEditionStatement) {
	this.fileEditionStatement = fileEditionStatement;
    };

    public String getBiblEditionStatement () {
	return this.biblEditionStatement;
    };

    public void setBiblEditionStatement (String biblEditionStatement) {
	this.biblEditionStatement = biblEditionStatement;
    };

    public String getReference () {
	return this.reference;
    };

    public void setReference (String reference) {
	this.reference = reference;
    };

    public String getLanguage () {
	return this.language;
    };

    public void setLanguage (String language) {
	this.language = language;
    };

    public String getCorpusTitle () {
	return this.corpusTitle;
    };

    public void setCorpusTitle (String corpusTitle) {
	this.corpusTitle = corpusTitle;
    };

    public String getCorpusSubTitle () {
	return this.corpusSubTitle;
    };

    public void setCorpusSubTitle (String corpusSubTitle) {
	this.corpusSubTitle = corpusSubTitle;
    };

    public String getCorpusAuthor () {
	return this.corpusAuthor;
    };

    public void setCorpusAuthor (String corpusAuthor) {
	this.corpusAuthor = corpusAuthor;
    };

    public String getCorpusEditor () {
	return this.corpusEditor;
    };

    public void setCorpusEditor (String corpusEditor) {
	this.corpusEditor = corpusEditor;
    };

    public String getDocTitle () {
	return this.docTitle;
    };

    public void setDocTitle (String docTitle) {
	this.docTitle = docTitle;
    };

    public String getDocSubTitle () {
	return this.docSubTitle;
    };

    public void setDocSubTitle (String docSubTitle) {
	this.docSubTitle = docSubTitle;
    };

    public String getDocAuthor () {
	return this.docAuthor;
    };

    public void setDocAuthor (String docAuthor) {
	this.docAuthor = docAuthor;
    };

    public String getDocEditor () {
	return this.docEditor;
    };

    public void setDocEditor (String docEditor) {
	this.docEditor = docEditor;
    };

    public String getKeywords () {
	return this.keywords;
    };

    public void setKeywords (String keywords) {
	this.keywords = keywords;
    };

    public String getTokenSource () {
	return this.tokenSource;
    };

    public void setTokenSource (String tokenSource) {
	this.tokenSource = tokenSource;
    };
};
