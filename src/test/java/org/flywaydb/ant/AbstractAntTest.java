/*
 * Copyright 2010-2019 Boxfuse GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flywaydb.ant;

import org.apache.tools.ant.BuildFileRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

abstract class AbstractAntTest {

    private BuildFileRule buildFile;

    @BeforeEach
    void setUp(@TempDir Path temporaryFolder) {
        buildFile = new BuildFileRule();

        buildFile.configureProject(this.getClass().getResource("/" + getBuildFilename()).getFile());

        setAntProperty("db", temporaryFolder.toAbsolutePath().toString());
    }

    protected String getBuildFilename() {
        return "ant-build.xml";
    }

    protected final String execute(String targetName) {
        buildFile.executeTarget(targetName);
        return buildFile.getLog();
    }

    protected final void setAntProperty(String name, String value) {
        buildFile.getProject().setProperty(name, value);
    }
}
