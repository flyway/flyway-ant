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

import org.apache.tools.ant.AntAssert;
import org.apache.tools.ant.BuildFileRule;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

public class InfoTaskTest extends AbstractAntTest {

    private static final String TARGET_NAME = "info-test";

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    @BeforeEach
    public void setUp(@TempDir Path temporaryFolder) {
        configure(buildRule, temporaryFolder.toAbsolutePath().toString());
    }

    @Override
    protected BuildFileRule getBuildFileRule() {
        return buildRule;
    }

    @Test
    public void basicTest() {
        String log = execute(TARGET_NAME);
        AntAssert.assertContains("| Category  | Version | Description  | Type | Installed On | State   |", log);
        AntAssert.assertContains("| Versioned | 1       | create table | SQL  |              | Pending |", log);
    }
}
