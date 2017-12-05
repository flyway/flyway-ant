package org.flywaydb.ant;

import java.io.IOException;

import org.apache.tools.ant.AntAssert;
import org.apache.tools.ant.BuildFileRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class MigrateTaskTest extends AbstractAntTest {

    private static final String TARGET_NAME = "migrate-test";

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setUp() throws IOException {
        configure(buildRule, temporaryFolder.newFile().getAbsolutePath());
    }

    @Override
    protected BuildFileRule getBuildFileRule() {
        return buildRule;
    }

    @Override
    protected String getTargetName() {
        return TARGET_NAME;
    }

    @Test
    public void noMigrationsTest() {
        // there are no migrations with such a suffix
        buildRule.getProject().setProperty("flyway.sqlMigrationSuffixes", ".xxx");

        String log = execute();
        AntAssert.assertContains("No migrations found", log);
    }

    @Test
    public void basicTest() {
        String log = execute();
        AntAssert.assertContains("Successfully validated 1 migration", log);
    }
}
