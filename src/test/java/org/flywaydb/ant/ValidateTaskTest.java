/*
 * Copyright 2017-2024 Tomas Tulka
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

public class ValidateTaskTest extends AbstractAntTest {

    private static final String TARGET_NAME = "validate-test";

    @Test
    public void basicTest() {
        assertThat(execute(TARGET_NAME))
                .contains("Successfully validated 1 migration");
    }
}
