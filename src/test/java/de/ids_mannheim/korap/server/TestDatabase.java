package de.ids_mannheim.korap.server;

import de.ids_mannheim.korap.index.MatchCollector;
import de.ids_mannheim.korap.index.collector.MatchCollectorDB;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

import com.mchange.v2.c3p0.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;
import static org.junit.Assert.assertEquals;

public class TestDatabase {

    private Connection conn;
    private Statement stat;

    @Before
    public void setUp() throws Exception {
	Class.forName("org.sqlite.JDBC");
	conn = DriverManager.getConnection("jdbc:sqlite::memory:");
	this.stat = conn.createStatement();
	stat.executeUpdate("CREATE TABLE IF NOT EXISTS people (name TEXT, age INTEGER);");
	conn.setAutoCommit(false);
    };

    @Test
    public void TestDatabase () throws Exception {
	PreparedStatement prep = this.conn.prepareStatement(
	  "INSERT INTO people VALUES (?, ?);"
        );

	prep.setString(1, "Peter");
	prep.setString(2, "24");
	prep.addBatch();

	prep.setString(1, "Klaus");
	prep.setString(2, "31");
	prep.addBatch();

	prep.executeBatch();
	conn.setAutoCommit(true);

	ResultSet rs = stat.executeQuery("SELECT * FROM people;");

	rs.next();
	
	assertEquals(rs.getString("name"), "Peter");
	assertEquals(rs.getInt("age"), 24);

	rs.next();

	assertEquals(rs.getString("name"), "Klaus");
	assertEquals(rs.getInt("age"), 31);

	rs.close();
    };

    /*
     * The following tests don't work well with in-memory dbs and
     * temporary dbs - should be improved
     */

    @Ignore
    public void TestDatabasePool () throws Exception {
	ComboPooledDataSource cpds = new ComboPooledDataSource();
	// Connect to a temporary file instead of a in-memory file
	cpds.setDriverClass("org.sqlite.JDBC");
	cpds.setJdbcUrl("jdbc:sqlite:hui");
	cpds.setMaxStatements(100);

	// This is part of the match collector
	this.conn = cpds.getConnection();
	this.stat = conn.createStatement();
	stat.executeUpdate(
            "CREATE TABLE IF NOT EXISTS result_a (text_id INTEGER, match_count INTEGER);"
	);
	// conn.setAutoCommit(false);
	PreparedStatement prep = this.conn.prepareStatement(
	  "INSERT INTO result_a VALUES (?, ?);"
        );
	prep.setInt(1, 5);
	prep.setInt(2, 8000);
	prep.addBatch();
	prep.executeBatch();

	ResultSet rs = stat.executeQuery("SELECT * FROM result_a;");

	rs.next();
	
	assertEquals(rs.getInt("text_id"), 5);
	assertEquals(rs.getInt("match_count"), 8000);

	rs.close();

	// this.conn.close();

	MatchCollectorDB mc = new MatchCollectorDB(2000, "result_a");
	mc.setDBPool("sqlite", cpds);

	mc.add(9, 5000);
	mc.add(12, 6785);
	mc.add(39, 56576);

	mc.close();

	/*
	this.stat = this.conn.createStatement();
	stat.executeUpdate("CREATE TABLE IF NOT EXISTS result_a (text_id INTEGER, match_count INTEGER);");
	*/
    };

    @Ignore
    public void TestDatabasePoolConnector () throws Exception {
	ComboPooledDataSource cpds = new ComboPooledDataSource();
	// Connect to a temporary file instead of a in-memory file
	cpds.setDriverClass("org.sqlite.JDBC");
	cpds.setJdbcUrl("jdbc:sqlite:hui");
	cpds.setMaxStatements(100);

	// This is part of the match collector
	conn = cpds.getConnection();
	stat = conn.createStatement();
	// conn.setAutoCommit(true);
	stat.executeUpdate(
            "CREATE TABLE matchXYZ (text_id INTEGER, match_count INTEGER);"
	);

	MatchCollectorDB mc = new MatchCollectorDB(3, "matchXYZ");
	mc.setDBPool("sqlite", cpds);

	mc.add(9, 5000);
	mc.add(12, 6785);
	mc.add(39, 56576);
	// First commit

	mc.add(45, 5000);
	mc.add(67, 6785);
	mc.add(81, 56576);
	// Second commit

	mc.add(94, 456);
	mc.close();
	// Final commit

	// conn = cpds.getConnection();
	stat = conn.createStatement();
	ResultSet rs = stat.executeQuery("SELECT count('*') AS num FROM matchXYZ;");
	rs.next();
	assertEquals(7, rs.getInt("num"));
    };

    @Test
    public void TestMatchCollectorDB () throws Exception {
	MatchCollector mc = new MatchCollectorDB(2000, "matchXYZ");
	mc.add(5,7);
	mc.add(8,2);
	mc.add(9,10);
	mc.add(16,90);
	mc.commit();
	assertEquals(mc.getTotalResults(), 109);
	assertEquals(mc.getTotalResultDocs(), 4);
    };

    @After
    public void shutDown () throws Exception {
	this.conn.close();
    };
};
