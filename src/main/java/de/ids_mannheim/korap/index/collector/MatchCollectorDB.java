package de.ids_mannheim.korap.index.collector;
import de.ids_mannheim.korap.KorapMatch;
import de.ids_mannheim.korap.index.MatchCollector;
import java.util.*;

public class MatchCollectorDB extends MatchCollector {

    /*
      Todo: In case there are multiple threads searching,
      the list should be synchrinized Collections.synchronizedList()
     */
    private List matchCollector;
    private int bufferSize;
    private int doccollect;

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
	this.incrTotalResultDocs(1);
	this.incrTotalResults(matchcount);
	this.matchCollector.add(new int[]{uniqueDocID, matchcount});
	if (this.doccollect++ > bufferSize)
	    this.commit();
    };

    public void commit () {
	
	this.matchCollector.clear();
	this.doccollect = 0;
    };
};
