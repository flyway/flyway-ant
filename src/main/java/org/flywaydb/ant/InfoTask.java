/*
 * Copyright 2017-2025 Tomas Tulka
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

import org.flywaydb.core.Flyway;
import org.flywaydb.core.internal.info.MigrationInfoDumper;

/**
 * Ant task to retrieve the complete information about the migrations including applied, pending and current migrations with details and status.
 */
public class InfoTask extends AbstractFlywayTask {
    @Override
    protected void doExecute(Flyway flyway) throws Exception {
        log.info("\n" + MigrationInfoDumper.dumpToAsciiTable(flyway.info().all()));
    }
}