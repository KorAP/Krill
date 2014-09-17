package de.ids_mannheim.korap.index.collector;
import de.ids_mannheim.korap.index.MatchCollector;
import de.ids_mannheim.korap.KorapMatch;
import java.util.*;

public class MatchCollectorTest implements MatchCollector {

    private String error;
    private int doccount = 0;
    private int matchcount = 0;

    public void add (int uniqueDocID, int matchcount) {
	this.doccount++;
	this.matchcount += matchcount;
    };

    public void setError(String msg) {
        this.error = msg;
    };

    public void setBenchmarkHitCounter(long t1, long t2) {
    };

    public int getMatchCount () {
	return matchcount;
    };

    public int getDocumentCount () {
	return doccount;
    };

    public String toJSON () {
	// This is also a commit!
	return "{ \"documents\" : " + doccount + ", \"matches\" : " + matchcount + " }";
    };

    public void commit() {
    };
};
