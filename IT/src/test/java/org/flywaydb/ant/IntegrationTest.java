package org.flywaydb.ant;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

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
            assertThat(hasRecord).as("DB should have a record.").isTrue();

            String value = rs.getString("val");

            assertThat(value).as("Record should have a column 'val'.").isNotBlank();
            assertThat(value).as("Value of 'val' should be '123'.").isEqualTo("123");

            boolean hasMore = rs.next();
            assertThat(hasMore).as("DB should not have another records.").isFalse();

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
