package org.flywaydb.ant;

import java.io.IOException;

import org.apache.tools.ant.AntAssert;
import org.apache.tools.ant.BuildFileRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class BaselineTaskTest extends AbstractAntTest {

    private static final String TARGET_NAME = "baseline-test";

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

    @Test
    public void basicTest() {
        String log = execute(TARGET_NAME);
        AntAssert.assertContains("Successfully baselined schema with version: 1", log);
    }
}
