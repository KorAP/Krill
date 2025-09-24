package de.ids_mannheim.korap.server;

import de.ids_mannheim.korap.response.MatchCollector;
import de.ids_mannheim.korap.response.collector.MatchCollectorDB;

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
import static org.junit.Assert.*;
import org.slf4j.bridge.SLF4JBridgeHandler;

public class TestDatabase {

    private Connection conn;
    private Statement stat;

    @Before
    public void setUp () throws Exception {
		SLF4JBridgeHandler.install();
		System.setProperty("com.mchange.v2.log.MLog", "com.mchange.v2.log.FallbackMLog");
		System.setProperty("com.mchange.v2.log.FallbackMLog.DEFAULT_CUTOFF_LEVEL", "WARNING");
		
        Class.forName("org.sqlite.JDBC");
        conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        this.stat = conn.createStatement();
        stat.executeUpdate(
                "CREATE TABLE IF NOT EXISTS people (name TEXT, age INTEGER);");
        conn.setAutoCommit(false);
    };


    @Test
    public void TestDatabase () throws Exception {
        PreparedStatement prep = this.conn
                .prepareStatement("INSERT INTO people VALUES (?, ?);");

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

        assertEquals("Peter", rs.getString("name"));
        assertEquals(24, rs.getInt("age"));

        rs.next();

        assertEquals("Klaus", rs.getString("name"));
        assertEquals(31, rs.getInt("age"));

        rs.close();
    };


    /*
     * The following tests don't work well with in-memory dbs and
     * temporary dbs - should be improved
     */

    @Test
    public void TestDatabasePool () throws Exception {
        ComboPooledDataSource cpds = new ComboPooledDataSource();
        // Connect to a temporary file instead of a in-memory file
        cpds.setDriverClass("org.sqlite.JDBC");
        cpds.setJdbcUrl("jdbc:sqlite:");
        cpds.setMaxStatements(100);

        // This is part of the match collector
        this.conn = cpds.getConnection();
        conn.setAutoCommit(false);
        this.stat = conn.createStatement();
        stat.executeUpdate(
                "CREATE TABLE IF NOT EXISTS result_a (text_id INTEGER, match_count INTEGER);");
        // conn.setAutoCommit(false);
        PreparedStatement prep = this.conn
                .prepareStatement("INSERT INTO result_a VALUES (?, ?);");
        prep.setInt(1, 5);
        prep.setInt(2, 8000);
        prep.addBatch();
        prep.executeBatch();

        ResultSet rs = stat.executeQuery("SELECT * FROM result_a;");
        rs.next();
        assertEquals(5, rs.getInt("text_id"));
        assertEquals(8000, rs.getInt("match_count"));
        rs.close();

        MatchCollectorDB mc = new MatchCollectorDB(2000, "result_a");
        mc.setDBPool("sqlite", cpds, this.conn);

        mc.add(9, 5000);
        mc.add(12, 6785);
        mc.add(39, 56576);

        mc.close(false);

        rs = stat.executeQuery("SELECT * FROM result_a;");
        assertTrue(rs.next());
        assertEquals(5, rs.getInt("text_id"));
        assertEquals(8000, rs.getInt("match_count"));
        rs.next();
        assertEquals(9, rs.getInt("text_id"));
        assertEquals(5000, rs.getInt("match_count"));
        rs.next();
        assertEquals(12, rs.getInt("text_id"));
        assertEquals(6785, rs.getInt("match_count"));
        rs.next();
        assertEquals(39, rs.getInt("text_id"));
        assertEquals(56576, rs.getInt("match_count"));

        rs.close();
    };


    @Test
    public void TestDatabasePoolCollector () throws Exception {
        ComboPooledDataSource cpds = new ComboPooledDataSource();
        // Connect to a temporary file instead of a in-memory file
        cpds.setDriverClass("org.sqlite.JDBC");
        cpds.setJdbcUrl("jdbc:sqlite:");
        cpds.setMaxStatements(100);

        // This is part of the match collector
        conn = cpds.getConnection();
        conn.setAutoCommit(false);
        Statement stat = conn.createStatement();
        stat.executeUpdate(
                "CREATE TABLE IF NOT EXISTS matchXYZ (text_id INTEGER, match_count INTEGER);");
        conn.commit();
        stat.close();

        MatchCollectorDB mc = new MatchCollectorDB(3, "matchXYZ");
        mc.setDBPool("sqlite", cpds, conn);

        mc.add(9, 5000);
        mc.add(12, 6785);
        mc.add(39, 56576);
        // First commit

        mc.add(45, 5000);
        mc.add(67, 6785);
        mc.add(81, 56576);
        // Second commit

        mc.add(94, 456);
        mc.close(false);
        // Final commit

        // conn = cpds.getConnection();
        stat = conn.createStatement();
        ResultSet rs = stat
                .executeQuery("SELECT count('*') AS num FROM matchXYZ;");

        assertEquals(7, rs.getInt("num"));

        rs = stat.executeQuery("SELECT text_id, match_count FROM matchXYZ;");
        assertTrue(rs.next());

        assertEquals(9, rs.getInt("text_id"));
        assertEquals(5000, rs.getInt("match_count"));
        assertTrue(rs.next());
        assertEquals(12, rs.getInt("text_id"));
        assertEquals(6785, rs.getInt("match_count"));
        assertTrue(rs.next());
        assertEquals(39, rs.getInt("text_id"));
        assertEquals(56576, rs.getInt("match_count"));
        assertTrue(rs.next());
        assertEquals(45, rs.getInt("text_id"));
        assertEquals(5000, rs.getInt("match_count"));
        assertTrue(rs.next());
        assertEquals(67, rs.getInt("text_id"));
        assertEquals(6785, rs.getInt("match_count"));
        assertTrue(rs.next());
        assertEquals(81, rs.getInt("text_id"));
        assertEquals(56576, rs.getInt("match_count"));
        assertTrue(rs.next());
        assertEquals(94, rs.getInt("text_id"));
        assertEquals(456, rs.getInt("match_count"));
        assertFalse(rs.next());
    };


    @Test
    public void TestMatchCollectorDB () throws Exception {
        MatchCollector mc = new MatchCollectorDB(2000, "matchXYZ");
        mc.add(5, 7);
        mc.add(8, 2);
        mc.add(9, 10);
        mc.add(16, 90);
        mc.commit();
        assertEquals(109, mc.getTotalResults());
        assertEquals(4, mc.getTotalResultDocs());
    };


    @After
    public void shutDown () throws Exception {
        this.conn.close();
    };
};
