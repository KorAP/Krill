package de.ids_mannheim.korap;

import java.util.*;

import de.ids_mannheim.korap.util.KorapDate;
import de.ids_mannheim.korap.document.KorapPrimaryData;
import de.ids_mannheim.korap.index.FieldDocument;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.*;

/* Todo:: Author and textClass may be arrays! */


/**
 * Abstract class representing a document in the KorAP index.
 *
 * @author ndiewald
 */
public abstract class KorapDocument {
    private KorapPrimaryData primaryData;

    @JsonIgnore
    public int internalDocID, localDocID;

    @JsonIgnore
    public String foundry;

    private String author, textClass, corpusID,
	           pubPlace, ID, title, subTitle;
    
    private KorapDate pubDate;

    /**
     * Set the publication date of the document the match occurs in.
     *
     * @param date The date as a KorapDate compatible string representation.
     * @return A KorapDate object for chaining.
     * @see KorapDate#Constructor(String)
     */
    public KorapDate setPubDate (String date) {
	//	ObjectMapper mapper = new ObjectMapper();
	this.pubDate = new KorapDate(date);
	return this.pubDate;
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
     * Get the publication date of the document the match occurs in as a KorapDate object.
     */
    @JsonIgnore
    public KorapDate getPubDate () {
	return this.pubDate;
    };

    @JsonProperty("pubDate")
    public String getPubDateString () {
	return this.pubDate.toDisplay();
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

    public void setCorpusID (String corpusID) {
	this.corpusID = corpusID;
    };

    @JsonProperty("corpusID")
    public String getCorpusID () {
	return this.corpusID;
    };

    public void setID (String ID) {
	this.ID = ID;
    };

    @JsonProperty("ID")
    public String getID () {
	return this.ID;
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
};
