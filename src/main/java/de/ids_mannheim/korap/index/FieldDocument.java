package de.ids_mannheim.korap.index;

import org.apache.lucene.document.Document;
import de.ids_mannheim.korap.analysis.MultiTermTokenStream;
import de.ids_mannheim.korap.analysis.MultiTermToken;
import de.ids_mannheim.korap.KorapDocument;
import de.ids_mannheim.korap.util.KorapDate;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import org.apache.lucene.document.TextField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.FieldInfo.IndexOptions;

import java.util.*;

/*
  Todo: Store primary data at base/cons field.
  All other Termvectors should have no stored field!
*/

/**
 * @author Nils Diewald
 *
 * FieldDocument implements a simple API to create documents for storing with KorapIndex.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FieldDocument extends KorapDocument {
    ObjectMapper mapper = new ObjectMapper();

    public Document doc = new Document();

    private FieldType tvField   = new FieldType(TextField.TYPE_STORED);
    private FieldType tvNoField = new FieldType(TextField.TYPE_NOT_STORED);
    private FieldType keywords  = new FieldType(TextField.TYPE_STORED);

    {
	tvField.setStoreTermVectors(true);
	tvField.setStoreTermVectorPositions(true);
	tvField.setStoreTermVectorPayloads(true);
	tvField.setStoreTermVectorOffsets(false);

	tvNoField.setStoreTermVectors(true);
	tvNoField.setStoreTermVectorPositions(true);
	tvNoField.setStoreTermVectorPayloads(true);
	tvNoField.setStoreTermVectorOffsets(false);

	keywords.setStoreTermVectors(true);
	keywords.setStoreTermVectorPositions(false);
	keywords.setStoreTermVectorPayloads(false);
	keywords.setStoreTermVectorOffsets(false);
	keywords.setIndexOptions(IndexOptions.DOCS_ONLY);
    }

    // see http://www.cowtowncoder.com/blog/archives/2011/07/entry_457.html

    public void addInt (String key, int value) {
	doc.add(new IntField(key, value, Field.Store.YES));
    };

    public void addInt (String key, String value) {
	this.addInt(key, Integer.parseInt(value));
    };

    public void addText (String key, String value) {
	doc.add(new TextField(key, value, Field.Store.YES));
    };

    public void addKeyword (String key, String value) {
	doc.add(new Field(key, value, keywords));
    };

    public void addString (String key, String value) {
	doc.add(new StringField(key, value, Field.Store.YES));
    };

    public void addStored (String key, String value) {
	doc.add(new StoredField(key, value));
    };

    public void addStored (String key, int value) {
	doc.add(new StoredField(key, value));
    };

    public void addTV (String key, String value, String tsString) {
	this.addTV(key, value, new MultiTermTokenStream(tsString));
    };

    public void addTV (String key, String tsString) {
	this.addTV(key, new MultiTermTokenStream(tsString));
    };

    public void addTV (String key, String value, MultiTermTokenStream ts) {
	Field textField = new Field( key, value, tvField );
	textField.setTokenStream( ts );
	doc.add(textField);
    };

    public void addTV (String key, MultiTermTokenStream ts) {
	Field textField = new Field( key, ts, tvNoField );
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
    public void setData (Map<String,Object> node) {
	this.setPrimaryData((String) node.get("text"));

	String fieldName = (String) node.get("name");
	MultiTermTokenStream mtts = this.newMultiTermTokenStream();

	// Iterate over all tokens in stream
	for (ArrayList<String> token : (ArrayList<ArrayList<String>>) node.get("stream")) {

	    // Initialize MultiTermToken
	    MultiTermToken mtt = new MultiTermToken(token.remove(0));

	    // Add rest of the list
	    for (String term : token)
		mtt.add(term);

	    // Add MultiTermToken to stream
	    mtts.addMultiTermToken(mtt);
	};

	// Add tokenstream to fielddocument
	this.addTV(fieldName, this.getPrimaryData(), mtts);

	// Get foundry info
	if (node.containsKey("foundries"))
	    this.setFoundries((String) node.get("foundries"));

	// Get layer info
	if (node.containsKey("layerInfos"))
	    this.setLayerInfos((String) node.get("layerInfos"));

	// Get tokenSource info
	if (node.containsKey("tokenSource"))
	    this.setTokenSource((String) node.get("tokenSource"));
    };

    /**
     * Deserialize token stream data (LEGACY).
     */
    public void setFields (ArrayList<Map<String,Object>> fields) {

	Map<String,Object> primary = fields.remove(0);
	this.setPrimaryData((String) primary.get("primaryData"));

	for (Map<String,Object> field : fields) {

	    String fieldName = (String) field.get("name");
	    MultiTermTokenStream mtts = this.newMultiTermTokenStream();

	    for (ArrayList<String> token : (ArrayList<ArrayList<String>>) field.get("data")) {

		MultiTermToken mtt = new MultiTermToken(token.remove(0));

		for (String term : token) {
		    mtt.add(term);
		};

		mtts.addMultiTermToken(mtt);
	    };

	    // TODO: This is normally dependend to the tokenization!
	    //       Add this as meta information to the document
	    // Store this information as well as tokenization information
	    // as meta fields in the tokenization term vector
	    if (field.containsKey("foundries")) {
		// TODO: Do not store positions!
		String foundries = (String) field.get("foundries");
		this.addKeyword("foundries", foundries);
		super.setFoundries(foundries);
	    };
	    if (field.containsKey("tokenization")) {
		String tokenization = (String) field.get("tokenization");
		this.addString("tokenization", tokenization);
		super.setTokenization(tokenization);
	    };

	    this.addTV(fieldName, this.getPrimaryData(), mtts);
	};
    };

    @Override
    public void setTextClass (String textClass) {
	super.setTextClass(textClass);
	this.addKeyword("textClass", textClass);
    };

    @Override
    public void setTitle (String title) {
	super.setTitle(title);
	this.addText("title", title);
    };

    @Override
    public void setSubTitle (String subTitle) {
	super.setSubTitle(subTitle);
	this.addText("subTitle", subTitle);
    };

    @Override
    public void setAuthor (String author) {
	super.setAuthor(author);
	this.addText("author", author);
    };

    @Override
    public void setPubPlace (String pubPlace) {
	super.setPubPlace(pubPlace);
	this.addString("pubPlace", pubPlace);
    };

    @JsonProperty("pubDate")
    @Override
    public KorapDate setPubDate (String pubDate) {
	KorapDate date = super.setPubDate(pubDate);
	this.addInt("pubDate", date.toString());
	return date;
    };

    @JsonProperty("creationDate")
    @Override
    public KorapDate setCreationDate (String creationDate) {
	KorapDate date = super.setCreationDate(creationDate);
	this.addInt("creationDate", date.toString());
	return date;
    };

    // No longer supported
    @Override
    public void setCorpusID (String corpusID) {
	super.setCorpusID(corpusID);
	this.addString("corpusID", corpusID);
    };

    // No longer supported
    @Override
    public void setID (String ID) {
	super.setID(ID);
	this.addString("ID", ID);
    };

    @Override
    public void setUID (int ID) {
	super.setUID(ID);
	this.addString("UID", new Integer(ID).toString());
    };

    // No longer supported
    @Override
    public void setLayerInfo (String layerInfo) {
	super.setLayerInfo(layerInfo);
	this.addStored("layerInfo", layerInfo);
    };

    @Override
    public void setTextSigle (String textSigle) {
	super.setTextSigle(textSigle);
	this.addString("textSigle", textSigle);
    };

    @Override
    public void setDocSigle (String docSigle) {
	super.setDocSigle(docSigle);
	this.addString("docSigle", docSigle);
    };

    @Override
    public void setCorpusSigle (String corpusSigle) {
	super.setCorpusSigle(corpusSigle);
	this.addString("corpusSigle", corpusSigle);
    };

    @Override
    public void setPublisher (String publisher) {
	super.setPublisher(publisher);
	this.addStored("publisher", publisher);
    };

    @Override
    public void setEditor (String editor) {
	super.setEditor(editor);
	this.addStored("editor", editor);
    };

    @Override
    public void setTextType (String textType) {
	super.setTextType(textType);
	this.addString("textType", textType);
    };

    @Override
    public void setTextTypeArt (String textTypeArt) {
	super.setTextTypeArt(textTypeArt);
	this.addString("textTypeArt", textTypeArt);
    };

    @Override
    public void setTextTypeRef (String textTypeRef) {
	super.setTextTypeRef(textTypeRef);
	this.addString("textTypeRef", textTypeRef);
    };

    @Override
    public void setTextColumn (String textColumn) {
	super.setTextColumn(textColumn);
	this.addStored("textColumn", textColumn);
    };

    @Override
    public void setTextDomain (String textDomain) {
	super.setTextDomain(textDomain);
	this.addString("textDomain", textDomain);
    };

    @Override
    public void setLicense (String license) {
	super.setLicense(license);
	this.addString("license", license);
    };

    @Override
    public void setPages (String pages) {
	super.setPages(pages);
	this.addStored("pages", pages);
    };

    @Override
    public void setFileEditionStatement (String fileEditionStatement) {
	super.setFileEditionStatement(fileEditionStatement);
	this.addStored("fileEditionStatement", fileEditionStatement);
    };

    @Override
    public void setBiblEditionStatement (String biblEditionStatement) {
	super.setBiblEditionStatement(biblEditionStatement);
	this.addStored("biblEditionStatement", biblEditionStatement);
    };

    @Override
    public void setReference (String reference) {
	super.setReference(reference);
	this.addStored("reference", reference);
    };

    @Override
    public void setLanguage (String language) {
	super.setLanguage(language);
	this.addString("language", language);
    };

    @Override
    public void setDocTitle (String docTitle) {
	super.setDocTitle(docTitle);
	this.addText("docTitle", docTitle);
    };

    @Override
    public void setDocSubTitle (String docSubTitle) {
	super.setDocSubTitle(docSubTitle);
	this.addText("docSubTitle", docSubTitle);
    };

    @Override
    public void setDocAuthor (String docAuthor) {
	super.setDocAuthor(docAuthor);
	this.addText("docAuthor", docAuthor);
    };

    @Override
    public void setDocEditor (String docEditor) {
	super.setDocEditor(docEditor);
	this.addStored("docEditor", docEditor);
    };

    @Override
    public void setCorpusTitle (String corpusTitle) {
	super.setCorpusTitle(corpusTitle);
	this.addText("corpusTitle", corpusTitle);
    };

    @Override
    public void setCorpusSubTitle (String corpusSubTitle) {
	super.setCorpusSubTitle(corpusSubTitle);
	this.addText("corpusSubTitle", corpusSubTitle);
    };

    @Override
    public void setCorpusAuthor (String corpusAuthor) {
	super.setCorpusAuthor(corpusAuthor);
	this.addText("corpusAuthor", corpusAuthor);
    };

    @Override
    public void setCorpusEditor (String corpusEditor) {
	super.setCorpusEditor(corpusEditor);
	this.addStored("corpusEditor", corpusEditor);
    };

    @Override
    public void setKeywords (String keywords) {
	super.setKeywords(keywords);
	this.addKeyword("keywords", keywords);
    };

    @Override
    public void setTokenSource (String tokenSource) {
	super.setTokenSource(tokenSource);
	this.addStored("tokenSource", tokenSource);
    };

    @Override
    public void setFoundries (String foundries) {
	super.setFoundries(foundries);
	this.addKeyword("foundries", foundries);
    };
};
