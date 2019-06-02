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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MigrateTaskTest extends AbstractAntTest {

    private static final String TARGET_NAME = "migrate-test";

    @Test
    public void basicTest() {
        assertThat(execute(TARGET_NAME))
                .contains("Successfully validated 1 migration");
    }

    @Test
    public void noMigrationsTest() {
        // there are no migrations with such a suffix
        setAntProperty("flyway.sqlMigrationSuffixes", ".xxx");

        assertThat(execute(TARGET_NAME))
                .contains("No migrations found");
    }
}
