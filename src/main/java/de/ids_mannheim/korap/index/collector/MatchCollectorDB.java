package de.ids_mannheim.korap.index.collector;
import de.ids_mannheim.korap.KorapMatch;
import de.ids_mannheim.korap.index.MatchCollector;
import com.fasterxml.jackson.annotation.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.*;

public class MatchCollectorDB extends MatchCollector {

    /*
      Todo: In case there are multiple threads searching,
      the list should be synchrinized Collections.synchronizedList()
     */
    private String databaseType;
    private List matchCollector;
    private int bufferSize;
    private int docCollect;
    private String resultID;

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
	this.incrTotalResultDocs(1);
	this.incrTotalResults(matchCount);
	this.matchCollector.add(new int[]{UID, matchCount});

	if (this.docCollect++ > bufferSize)
	    this.commit();
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
    public void openConnection (String type, DataSource ds) throws SQLException {
	this.setDatabaseType(type);
	this.connection = ds.getConnection();
	this.connection.setAutoCommit(false);

	// Create prepared statement for multiple requests

	/*
	this.prepared = this.conn.prepareStatement(
	  "INSERT INTO people VALUES (?, ?);"
        );

	Only prepare if commit > buffersize!
Difference between mariadb and sqlite!
	*/

    };
    
    public void commit () {	

	/*
	 */
	this.matchCollector.clear();
	this.docCollect = 0;
    };

    public void close () {
	this.commit();
	try {
	    this.connection.close();
	}
	catch (SQLException e) {
	};
    };
};
