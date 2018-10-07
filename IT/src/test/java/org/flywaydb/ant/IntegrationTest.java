package org.flywaydb.ant;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class IntegrationTest {

    private static String DB = System.getenv("db");

    @Test
    public void testMigrations() throws Exception {
        Connection conn = null;
        Statement stmt = null;
        try {
            Class.forName("org.h2.Driver");

            conn = DriverManager.getConnection("jdbc:h2:" + DB, "sa", "");
            stmt = conn.createStatement();

            ResultSet rs = stmt.executeQuery("select val from TESTTABLE");

            boolean hasRecord = rs.next();
            assertTrue("DB should have a record.", hasRecord);

            String value = rs.getString("val");

            assertNotNull("Record should have a column 'val'.", value);
            assertEquals("Value of 'val' should be '123'.", "123", value);

            boolean hasMore = rs.next();
            assertFalse("DB should not have another records.", hasMore);

        } finally {
            if (stmt != null) {
                stmt.close();
            }
            if (conn != null) {
                conn.close();
            }
        }
    }
}
