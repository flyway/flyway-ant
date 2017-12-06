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

    abstract protected String getTargetName();

    protected String execute() {
        getBuildFileRule().executeTarget(getTargetName());
        return getBuildFileRule().getLog();
    }
}
