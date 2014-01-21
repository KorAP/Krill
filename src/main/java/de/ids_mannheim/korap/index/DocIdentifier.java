package de.ids_mannheim.korap.index;
import java.util.*;
import java.util.regex.*;


public class DocIdentifier {
    protected String corpusID, docID;

    public String getCorpusID () {
	return this.corpusID;
    };

    public void setCorpusID (String id) {
	if (id != null && !id.contains("!"))
	    this.corpusID = id;
    };

    public String getDocID () {
	return this.docID;
    };

    public void setDocID (String id) {
	if (!id.contains("!"))
	    this.docID = id;
    };
};
