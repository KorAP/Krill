package de.ids_mannheim.korap.response.match;

import java.util.*;
import java.util.regex.*;


// TODO: This should only use textSigle!

public class DocIdentifier {
    protected String textSigle, // fine
        corpusID, // LEGACY
        docID;    // LEGACY


    // Legacy
    public String getCorpusID () {
        return this.corpusID;
    };


    // Legacy
    public void setCorpusID (String id) {
        if (id != null && !id.contains("!"))
            this.corpusID = id;
    };


    // Legacy
    public String getDocID () {
        return this.docID;
    };


    // Legacy
    public void setDocID (String id) {
        if (id != null && !id.contains("!"))
            this.docID = id;
    };


    public String getTextSigle () {
        return this.textSigle;
    };


    public void setTextSigle (String id) {
        if (id != null && !id.contains("!"))
            this.textSigle = id;
    };
};
