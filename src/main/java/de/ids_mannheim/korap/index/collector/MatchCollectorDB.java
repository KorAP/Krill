package de.ids_mannheim.korap.index.collector;
import de.ids_mannheim.korap.KorapNode;
import de.ids_mannheim.korap.KorapMatch;
import de.ids_mannheim.korap.index.MatchCollector;
import com.fasterxml.jackson.annotation.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MatchCollectorDB extends MatchCollector {

    // Logger
    private final static Logger log = LoggerFactory.getLogger(KorapNode.class);

    /*
      Todo: In case there are multiple threads searching,
      the list should be synchrinized Collections.synchronizedList()
     */
    private String databaseType;
    private List matchCollector;
    private int bufferSize, docCollect;
    private String resultID;

    //    private Connection connection;
    private DataSource pool;
    private Connection connection;
    private PreparedStatement prepared;

    /*
     * Create a new collector for database connections
     */
    public MatchCollectorDB (int bufferSize, String resultID) {
	this.bufferSize = bufferSize;
	this.resultID = resultID;
	this.matchCollector = new ArrayList<int[]>(bufferSize + 2);
    };

    /*
     * Add matches till the bufferSize exceeds - then commit to the database.
     */
    public void add (int UID, int matchCount) {
	if (this.docCollect == bufferSize)
	    this.commit();

	this.incrTotalResultDocs(1);
	this.incrTotalResults(matchCount);
	this.matchCollector.add(new int[]{UID, matchCount});
	this.docCollect++;
    };

    @JsonIgnore
    public void setDatabaseType (String type) {
	this.databaseType = type;
    };

    @JsonIgnore
    public String getDatabaseType () {
	return this.databaseType;
    };

    @JsonIgnore
    public void setDBPool (String type, DataSource ds) throws SQLException {
	this.setDatabaseType(type);
	this.pool = ds;

	// Create prepared statement for multiple requests

	/*
	this.prepared = this.conn.prepareStatement(
	  "INSERT INTO people VALUES (?, ?);"
        );

	Only prepare if commit > buffersize!
Difference between mariadb and sqlite!
	*/

    };
    
    /* TODO: Ensure the commit was successful! */
    public void commit () {	
	if (this.pool == null)
	    return;

	try {
	    // This should be heavily optimized! It's aweful!
	    /*
	     * ARGHHHHHHH!
	     */

	    if (this.connection == null)
		this.connection = this.pool.getConnection();

	    // TODO: Create a BEGIN ... COMMIT Transaction
	    // connection.setAutoCommit(true);

	    StringBuilder sb = new StringBuilder();
	    sb.append("INSERT INTO ");
	    sb.append(this.resultID);
	    sb.append(" (text_id, match_count) ");

	    // SQLite insertion idiom
	    if (this.getDatabaseType().equals("sqlite")) {
		for (int i = 1; i < this.docCollect; i++) {
		    sb.append("SELECT ?, ? UNION ");
		}
		if (this.docCollect == 1)
		    sb.append("VALUES (?, ?)");
		else
		    sb.append("SELECT ?, ?");
	    }

	    // MySQL insertion idiom
	    else if (this.getDatabaseType().equals("mysql")) {
		sb.append(" VALUES ");
		for (int i = 1; i < this.docCollect; i++) {
		    sb.append("(?,?),");
		};
		sb.append("(?,?)");
	    }
	    else {
		log.error("Unsupported Database type");
		return;
	    };

	    // System.err.println(sb.toString());

	    PreparedStatement prep = connection.prepareStatement(sb.toString());

	    int i = 1;
	    ListIterator li = this.matchCollector.listIterator(); 
	    while (li.hasNext()) {
		int[] v = (int[]) li.next();
		// System.err.println("Has " + i + ":" + v[0]);
		prep.setInt(i++, v[0]);
		// System.err.println("Has " + i + ":" + v[1]);
		prep.setInt(i++, v[1]);
		// System.err.println("-");
	    };

	    // System.err.println(sb.toString());

	    prep.addBatch();
	    prep.executeBatch();
	    // connection.setAutoCommit(false);
	    //	    connection.close();
	    this.matchCollector.clear();
	    this.docCollect = 0;
	}
	catch (SQLException e) {
	    this.matchCollector.clear();
	    this.docCollect = 0;
	    System.err.println("Error: " + e.getLocalizedMessage());
	    log.error(e.getLocalizedMessage());
	};
	return;
    };

    public void close () {
	this.commit();
	/*
	try {
	    this.connection.close();
	}
	catch (SQLException e) {
	};
	*/
    };
};
