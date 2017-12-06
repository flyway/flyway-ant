package org.flywaydb.ant;

import org.apache.tools.ant.BuildFileRule;

abstract class AbstractAntTest {

    protected void configure(BuildFileRule buildRule, String dbFilename) {
        buildRule.configureProject(AbstractAntTest.class.getResource("/" + getBuildFilename()).getFile());

        buildRule.getProject().setProperty("db", dbFilename);
    }

    protected String getBuildFilename() {
        return "ant-build.xml";
    }

    abstract protected BuildFileRule getBuildFileRule();

    protected String execute(String targetName) {
        getBuildFileRule().executeTarget(targetName);
        return getBuildFileRule().getLog();
    }
}
