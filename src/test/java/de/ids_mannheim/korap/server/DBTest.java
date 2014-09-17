package de.ids_mannheim.korap.server;

import de.ids_mannheim.korap.index.MatchCollector;
import de.ids_mannheim.korap.index.collector.MatchCollectorDB;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

/*
  bitbucket.org/xerial/sqlite-jdbc
*/

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class DBTest {

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

    @Test
    public void TestMatchCollectorDB () throws Exception {
	MatchCollector mc = new MatchCollectorDB(2000, "matchXYZ");
	mc.add(5,7);
	mc.add(8,2);
	mc.add(9,10);
	mc.add(16,90);
	mc.commit();
	assertEquals(mc.getMatchCount(), 109);
	assertEquals(mc.getDocumentCount(), 4);
    };

    @After
    public void shutDown () throws Exception {
	this.conn.close();
    };
};
