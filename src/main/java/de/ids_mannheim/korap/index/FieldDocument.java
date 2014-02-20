package de.ids_mannheim.korap.index;

import org.apache.lucene.document.Document;
import de.ids_mannheim.korap.analysis.MultiTermTokenStream;
import de.ids_mannheim.korap.analysis.MultiTermToken;
import de.ids_mannheim.korap.KorapDocument;
import de.ids_mannheim.korap.util.KorapDate;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;

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

    /*
    @JsonCreator
    public FieldDocument(Map<String,Object> props) {
      this.id = (String) props.get("id");
      this.title = (String) props.get("title");
    };

    public FieldDocument (String json) {

	
	my $primary = ->{primary}
	corpus_id, pub_date, id, text_class (Array), author (Array), title, sub_title, pub_place

	foreach (->{fields}) {
	    foreach (data) {
		foreach () {
		}
	    }
	};
created timestamp
last_modified timestamp or KorapDate
	
    };
*/

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
	this.addText("textClass", textClass);
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

    @Override
    public void setCorpusID (String corpusID) {
	super.setCorpusID(corpusID);
	this.addString("corpusID", corpusID);
    };

    @Override
    public void setID (String ID) {
	super.setID(ID);
	this.addString("ID", ID);
    };

    @Override
    public void setLayerInfo (String layerInfo) {
	super.setLayerInfo(layerInfo);
	this.addStored("layerInfo", layerInfo);
    };
};
