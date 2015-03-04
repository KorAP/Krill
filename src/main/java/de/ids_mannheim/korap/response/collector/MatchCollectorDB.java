package de.ids_mannheim.korap.response.collector;

import de.ids_mannheim.korap.server.Node;
import de.ids_mannheim.korap.response.Match;
import de.ids_mannheim.korap.response.MatchCollector;
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
    private final static Logger log = LoggerFactory.getLogger(Node.class);

    /*
     * Todo: In case there are multiple threads searching,
     * the list should be synchrinized Collections.synchronizedList()
     */
    private String databaseType;
    private List matchCollector;
    private int bufferSize, docCollect;
    private String resultID;

    // private Connection connection;
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
        this.matchCollector.add(new int[] { UID, matchCount });
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
    public void setDBPool (String type, DataSource ds, Connection conn)
            throws SQLException {
        this.setDatabaseType(type);
        this.connection = conn;
        this.pool = ds;
    };


    @JsonIgnore
    public void setDBPool (String type, DataSource ds) throws SQLException {
        this.setDatabaseType(type);
        this.pool = ds;
    };


    /*
      Create prepared statement for multiple requests
      this.prepared = this.conn.prepareStatement(
      "INSERT INTO people VALUES (?, ?);"
      );
      Only prepare if commit > buffersize!
      Difference between mariadb and sqlite!
    */


    /* TODO: Ensure the commit was successful! */
    public void commit () {
        if (this.pool == null)
            return;

        try {
            /*
             * This should be heavily optimized! It's aweful!
             * ARGHHHHHHH!
             */
            if (this.connection.isClosed())
                this.connection = this.pool.getConnection();

            StringBuilder sb = new StringBuilder();
            sb.append("INSERT INTO ").append(this.resultID)
                    .append(" (text_id, match_count) ");

            // SQLite batch insertion idiom
            if (this.getDatabaseType().equals("sqlite")) {
                for (int i = 1; i < this.docCollect; i++) {
                    sb.append("SELECT ?, ? UNION ");
                }
                if (this.docCollect == 1)
                    sb.append("VALUES (?, ?)");
                else
                    sb.append("SELECT ?, ?");
            }

            // MySQL batch insertion idiom
            else if (this.getDatabaseType().equals("mysql")) {
                sb.append(" VALUES ");
                for (int i = 1; i < this.docCollect; i++) {
                    sb.append("(?,?),");
                };
                sb.append("(?,?)");
            }

            // Unknown idiom
            else {
                log.error("Unsupported Database type");
                return;
            };

            // Prepare statement based on the string
            PreparedStatement prep = this.connection.prepareStatement(sb
                    .toString());

            int i = 1;
            ListIterator li = this.matchCollector.listIterator();
            while (li.hasNext()) {
                int[] v = (int[]) li.next();
                prep.setInt(i++, v[0]);
                prep.setInt(i++, v[1]);
            };

            prep.addBatch();
            prep.executeBatch();
            this.connection.commit();
        }

        // An SQL error occured ...
        catch (SQLException e) {
            log.error(e.getLocalizedMessage());
        };

        this.matchCollector.clear();
        this.docCollect = 0;
        return;
    };


    /*
     * Close collector and connection
     */
    public void close () {
        this.commit();
        try {
            this.connection.close();
        }
        catch (SQLException e) {
            log.warn(e.getLocalizedMessage());
        }
    };


    /*
     * Close collector and probably connection
     */
    public void close (boolean close) {
        if (close)
            this.close();
        else
            this.commit();
    };
};
