package de.ids_mannheim.korap.index.collector;
import de.ids_mannheim.korap.KorapMatch;
import de.ids_mannheim.korap.index.MatchCollector;
import java.util.*;

public class MatchCollectorDB implements MatchCollector {

    /*
      Todo: In case there are multiple threads searching,
      the list should be synchrinized Collections.synchronizedList()
     */

    private String error;
    private int doccount = 0;
    private int matchcount = 0;
    private int doccollect = 0;

    private List matchCollector;
    private int bufferSize;

    private String tableName;

    /*
     * Create a new collector for database connections
     */
    public MatchCollectorDB (int bufferSize, String tableName) {
	this.bufferSize = bufferSize;
	this.tableName = tableName;
	this.matchCollector = new ArrayList<int[]>(bufferSize + 2);
    };

    /*
     * Add matches till the bufferSize exceeds - then commit to the database.
     */
    public void add (int uniqueDocID, int matchcount) {
	this.doccount++;
	this.matchcount += matchcount;
	this.matchCollector.add(new int[]{uniqueDocID, matchcount});
	if (this.doccollect++ > bufferSize)
	    this.commit();
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
	// This may also be a commit!
	return "{ \"documents\" : " + doccount + ", \"matches\" : " + matchcount + " }";
    };

    public void commit () {
	
	this.matchCollector.clear();
	this.doccollect = 0;
    };
};
