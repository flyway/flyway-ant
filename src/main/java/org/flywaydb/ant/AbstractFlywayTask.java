/*
 * Copyright 2010-2018 Boxfuse GmbH
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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.api.logging.Log;
import org.flywaydb.core.api.logging.LogFactory;
import org.flywaydb.core.internal.jdbc.DriverDataSource;
import org.flywaydb.core.internal.util.ExceptionUtils;
import org.flywaydb.core.internal.util.StringUtils;

/**
 * Base class for all Flyway Ant tasks.
 */
@SuppressWarnings({"UnusedDeclaration"})
public abstract class AbstractFlywayTask extends Task {
    /**
     * Property name prefix for placeholders that are configured through properties.
     */
    private static final String PLACEHOLDERS_PROPERTY_PREFIX = "flyway.placeholders.";

    /**
     * Flyway Configuration.
     */
    private FluentConfiguration flywayConfig;

    /**
     * Logger.
     */
    protected Log log;

    /**
     * The classpath used to load the JDBC driver and the migrations.
     */
    private Path classPath;

    /**
     * The fully qualified classname of the jdbc driver to use to connect to the database.<br>Also configurable with Ant Property: ${flyway.driver}
     */
    private String driver;

    /**
     * The jdbc url to use to connect to the database.<br>Also configurable with Ant Property: ${flyway.url}
     */
    private String url;

    /**
     * The user to use to connect to the database. (default: <i>blank</i>)<br>Also configurable with Ant Property: ${flyway.user}<br> The credentials can be
     * specified by user/password or serverId from settings.xml
     */
    private String user;

    /**
     * The password to use to connect to the database. (default: <i>blank</i>)<br>Also configurable with Ant Property: ${flyway.password}
     */
    private String password;

    /**
     * Locations on the classpath to scan recursively for migrations. Locations may contain both sql and java-based migrations. (default: db.migration)<br>Also
     * configurable with Ant Property: ${flyway.locations}
     */
    private String[] locations;

    /**
     * The custom MigrationResolvers to be used in addition or as replacement to the built-in (as determined by the skipDefaultResolvers property) ones for
     * resolving Migrations to apply. <p>(default: none)</p>
     */
    private String[] resolvers;

    /**
     * The callbacks for lifecycle notifications. <p>(default: none)</p>
     */
    private String[] callbacks;

    /**
     * A map of &lt;placeholder, replacementValue&gt; to apply to sql migration scripts.
     */
    private Map<String, String> placeholders;

    /**
     * Follow Flyway Core properties...
     */
    private String[] schemas;
    private Boolean skipDefaultResolvers;
    private Boolean skipDefaultCallbacks;
    private String table;
    private String baselineVersion;
    private String baselineDescription;
    private Boolean mixed;
    private Boolean group;
    private Boolean stream;
    private Boolean batch;
    private String installedBy;
    private Integer connectRetries;
    private String initSql;
    private SchemasElement schemasElement;
    private String encoding;
    private String sqlMigrationPrefix;
    private String repeatableSqlMigrationPrefix;
    private String undoSqlMigrationPrefix;
    private String sqlMigrationSeparator;
    private String[] sqlMigrationSuffixes;
    private String target;
    private Boolean cleanOnValidationError;
    private Boolean cleanDisabled;
    private Boolean outOfOrder;
    private Boolean placeholderReplacement;
    private String placeholderPrefix;
    private String placeholderSuffix;
    private Boolean ignoreMissingMigrations;
    private Boolean ignoreIgnoredMigrations;
    private Boolean ignorePendingMigrations;
    private Boolean ignoreFutureMigrations;
    private Boolean validateOnMigrate;
    private Boolean baselineOnMigrate;
    private String[] errorOverrides;
    private String dryRunOutput;
    private Boolean oracleSqlplus;
    private String licenseKey;

    /**
     * @param classpath The classpath used to load the JDBC driver and the migrations.<br>Also configurable with Ant Property: ${flyway.classpath}
     */
    public void setClasspath(Path classpath) {
        this.classPath = classpath;
    }

    /**
     * @param classpathref The reference to the classpath used to load the JDBC driver and the migrations.<br>Also configurable with Ant Property:
     *                     ${flyway.classpathref}
     */
    public void setClasspathref(Reference classpathref) {
        Path classPath = new Path(getProject());
        classPath.setRefid(classpathref);
        this.classPath = classPath;
    }

    /**
     * @param driver The fully qualified classname of the jdbc driver to use to connect to the database.<br> By default, the driver is autodetected based on the
     *               url.<br> Also configurable with Ant Property: ${flyway.driver}
     */
    public void setDriver(String driver) {
        this.driver = driver;
    }

    /**
     * @param url The jdbc url to use to connect to the database.<br>Also configurable with Ant Property: ${flyway.url}
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * @param user The user to use to connect to the database.<br>Also configurable with Ant Property: ${flyway.user}
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * @param password The password to use to connect to the database. (default: <i>blank</i>)<br>Also configurable with Ant Property: ${flyway.password}
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @param connectRetries The maximum number of retries when attempting to connect to the database. (default: <i>0</i>)<br>Also configurable with Ant
     *                       Property: ${flyway.connectRetries}
     */
    public void setConnectRetries(int connectRetries) {
        this.connectRetries = connectRetries;
    }

    /**
     * @param initSql The SQL statements to run to initialize a new database connection immediately after opening it. (default: <i>none</i>)<br>Also
     *                configurable with Ant Property: ${flyway.initSql}
     */
    public void setInitSql(String initSql) {
        this.initSql = initSql;
    }

    /**
     * @param schemas Comma-separated list of the schemas managed by Flyway. These schema names are case-sensitive.<br> (default: The default schema for the
     *                datasource connection) <p>Consequences:</p> <ul> <li>The first schema in the list will be automatically set as the default one during the
     *                migration.</li> <li>The first schema in the list will also be the one containing the metadata table.</li> <li>The schemas will be cleaned
     *                in the order of this list.</li> </ul>Also configurable with Ant Property: ${flyway.schemas}
     */
    public void setSchemas(String schemas) {
        this.schemas = StringUtils.tokenizeToStringArray(schemas, ",");
    }

    /**
     * @param resolvers The custom MigrationResolvers to be used in addition to the built-in ones for resolving Migrations to apply. <p>(default: none)</p>
     */
    public void setResolvers(String resolvers) {
        this.resolvers = StringUtils.tokenizeToStringArray(resolvers, ",");
    }

    /**
     * @param skipDefaultResolvers Whether built-int resolvers should be skipped. If true, only custom resolvers are used.<p>(default: false)</p> <br>Also
     *                             configurable with Ant Property: ${flyway.skipDefaultResolvers}
     */
    public void setSkipDefaultResolvers(boolean skipDefaultResolvers) {
        this.skipDefaultResolvers = skipDefaultResolvers;
    }

    /**
     * @param callbacks A comma-separated list of fully qualified FlywayCallback implementation class names.  These classes will be instantiated and wired into
     *                  the Flyway lifecycle notification events.
     */
    public void setCallbacks(String callbacks) {
        this.callbacks = StringUtils.tokenizeToStringArray(callbacks, ",");
    }

    /**
     * @param skipDefaultCallbacks Whether built-int callbacks should be skipped. If true, only custom callbacks are used.<p>(default: false)</p> <br>Also
     *                             configurable with Ant Property: ${flyway.skipDefaultCallbacks}
     */
    public void setSkipDefaultCallbacks(boolean skipDefaultCallbacks) {
        this.skipDefaultCallbacks = skipDefaultCallbacks;
    }

    /**
     * @param table <p>The name of the schema metadata table that will be used by Flyway.</p><p> By default (single-schema mode) the metadata table is placed
     *              in the default schema for the connection provided by the datasource. </p> <p> When the <i>flyway.schemas</i> property is set (multi-schema
     *              mode), the metadata table is placed in the first schema of the list. </p> (default: schema_version)<br>Also configurable with Ant Property:
     *              ${flyway.table}
     */
    public void setTable(String table) {
        this.table = table;
    }

    /**
     * @param baselineVersion The version to tag an existing schema with when executing baseline. (default: 1)<br>Also configurable with Ant Property:
     *                        ${flyway.baselineVersion}
     */
    public void setBaselineVersion(String baselineVersion) {
        this.baselineVersion = baselineVersion;
    }

    /**
     * @param baselineDescription The description to tag an existing schema with when executing baseline. (default: &lt;&lt; Flyway Baseline &gt;&gt;)<br>Also
     *                            configurable with Ant Property: ${flyway.baselineDescription}
     */
    public void setBaselineDescription(String baselineDescription) {
        this.baselineDescription = baselineDescription;
    }

    /**
     * Whether to allow mixing transactional and non-transactional statements within the same migration.<br> Also configurable with Ant Property:
     * ${flyway.mixed}
     *
     * @param mixed {@code true} if mixed migrations should be allowed. {@code false} if an error should be thrown instead. (default: {@code false})
     */
    public void setMixed(boolean mixed) {
        this.mixed = mixed;
    }

    /**
     * Whether to group all pending migrations together in the same transaction when applying them (only recommended for databases with support for DDL
     * transactions).
     * <p>
     * Also configurable with Ant Property: ${flyway.group}
     *
     * @param group {@code true} if migrations should be grouped. {@code false} if they should be applied individually instead. (default: {@code false})
     */
    public void setGroup(boolean group) {
        this.group = group;
    }

    /**
     * Whether to stream SQL migrations when executing them. Streaming doesn't load the entire migration in memory at once. Instead each statement is loaded
     * individually. This is particularly useful for very large SQL migrations composed of multiple MB or even GB of reference data, as this dramatically
     * reduces Flyway's memory consumption.
     * <p>
     * Also configurable with Ant Property: ${flyway.stream}
     *
     * @param stream {@code true} if migrations should be streamed. {@code false} if they should be loaded individually instead. (default: {@code false})
     */
    public void setStream(boolean stream) {
        this.stream = stream;
    }

    /**
     * Whether to batch SQL statements when executing them. Batching can save up to 99 percent of network roundtrips by sending up to 100 statements at once
     * over the network to the database, instead of sending each statement individually. This is particularly useful for very large SQL migrations composed of
     * multiple MB or even GB of reference data, as this can dramatically reduce the network overhead. This is supported for INSERT, UPDATE, DELETE, MERGE and
     * UPSERT statements. All other statements are automatically executed without batching.
     * <p>
     * Also configurable with Ant Property: ${flyway.batch}
     *
     * @param batch {@code true} if SQL statements should be batched. {@code false} if they should be sent individually instead. (default: {@code false})
     */
    public void setBatch(boolean batch) {
        this.batch = batch;
    }

    /**
     * The username that will be recorded in the metadata table as having applied the migration. <p>Also configurable with Ant Property:
     * ${flyway.installedBy}</p>
     *
     * @param installedBy The username or <i>blank</i> for the current database user of the connection. (default: <i>blank</i>).
     */
    public void setInstalledBy(String installedBy) {
        this.installedBy = installedBy;
    }

    /**
     * @param encoding The encoding of Sql migrations. (default: UTF-8)<br>Also configurable with Ant Property: ${flyway.encoding}
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * <p>Sql migrations have the following file name structure: prefixVERSIONseparatorDESCRIPTIONsuffix, which using the defaults translates to
     * V1_1__My_description.sql</p>
     *
     * @param sqlMigrationPrefix The file name prefix for Sql migrations (default: V).<br>Also configurable with Ant Property: ${flyway.sqlMigrationPrefix}
     */
    public void setSqlMigrationPrefix(String sqlMigrationPrefix) {
        this.sqlMigrationPrefix = sqlMigrationPrefix;
    }

    /**
     * <p>Repeatable sql migrations have the following file name structure: prefixSeparatorDESCRIPTIONsuffix , which using the defaults translates to
     * R__My_description.sql</p>
     *
     * @param repeatableSqlMigrationPrefix The file name prefix for repeatable sql migrations (default: R)<br>Also configurable with Ant Property:
     *                                     ${flyway.repeatableSqlMigrationPrefix}
     */
    public void setRepeatableSqlMigrationPrefix(String repeatableSqlMigrationPrefix) {
        this.repeatableSqlMigrationPrefix = repeatableSqlMigrationPrefix;
    }

    /**
     * <p>Undo SQL migrations have the following file name structure: prefixVERSIONseparatorDESCRIPTIONsuffix, which using the defaults translates to
     * U1.1__My_description.sql</p>
     *
     * @param undoSqlMigrationPrefix The file name prefix for undo SQL migrations (default: U)<br>Also configurable with Ant Property:
     *                               ${flyway.undoSqlMigrationPrefix }
     */
    public void setUndoSqlMigrationPrefix(String undoSqlMigrationPrefix) {
        this.undoSqlMigrationPrefix = undoSqlMigrationPrefix;
    }

    /**
     * <p>Sql migrations have the following file name structure: prefixVERSIONseparatorDESCRIPTIONsuffix , which using the defaults translates to
     * V1_1__My_description.sql</p>
     *
     * @param sqlMigrationSeparator The file name separator for Sql migrations (default: V)<br>Also configurable with Ant Property:
     *                              ${flyway.sqlMigrationPrefix}
     */
    public void setSqlMigrationSeparator(String sqlMigrationSeparator) {
        this.sqlMigrationSeparator = sqlMigrationSeparator;
    }

    /**
     * <p>Sql migrations have the following file name structure: prefixVERSIONseparatorDESCRIPTIONsuffix , which using the defaults translates to
     * V1_1__My_description.sql</p> <p>Multiple suffixes (like .sql,.pkg,.pkb) can be specified for easier compatibility with other tools such as editors with
     * specific file associations.</p>
     *
     * @param sqlMigrationSuffixes The file name suffixes for SQL migrations, comma-separated. (default: .sql)<br>Also configurable with Ant Property:
     *                             ${flyway.sqlMigrationSuffixes}
     */
    public void setSqlMigrationSuffixes(String sqlMigrationSuffixes) {
        this.sqlMigrationSuffixes = StringUtils.tokenizeToStringArray(sqlMigrationSuffixes, ",");
    }

    /**
     * @param target The target version up to which Flyway should consider migrations. Migrations with a higher version number will be ignored. The special
     *               value {@code current} designates the current version of the schema. (default: the latest version)<br>Also configurable with Ant Property:
     *               ${flyway.target}
     */
    public void setTarget(String target) {
        this.target = target;
    }

    /**
     * @param cleanOnValidationError Whether to automatically call clean or not when a validation error occurs. (default: {@code false})<br> <p> This is
     *                               exclusively intended as a convenience for development. Even tough we strongly recommend not to change migration scripts
     *                               once they have been checked into SCM and run, this provides a way of dealing with this case in a smooth manner. The
     *                               database will be wiped clean automatically, ensuring that the next migration will bring you back to the state checked into
     *                               SCM.</p> <p><b>Warning ! Do not enable in production !</b></p> <br>Also configurable with Ant Property:
     *                               ${flyway.cleanOnValidationError}
     */
    public void setCleanOnValidationError(boolean cleanOnValidationError) {
        this.cleanOnValidationError = cleanOnValidationError;
    }

    /**
     * @param cleanDisabled Whether to disable clean. (default: {@code false}) <p>This is especially useful for production environments where running clean can
     *                      be quite a career limiting move.</p>
     */
    public void setCleanDisabled(boolean cleanDisabled) {
        this.cleanDisabled = cleanDisabled;
    }

    /**
     * @param outOfOrder Allows migrations to be run "out of order" (default: {@code false}). <p>If you already have versions 1 and 3 applied, and now a version
     *                   2 is found, it will be applied too instead of being ignored.</p> Also configurable with Ant Property: ${flyway.outOfOrder}
     */
    public void setOutOfOrder(boolean outOfOrder) {
        this.outOfOrder = outOfOrder;
    }

    /**
     * @param placeholderReplacement Whether placeholders should be replaced. (default: true)<br>Also configurable with Ant Property:
     *                               ${flyway.placeholderReplacement}
     */
    public void setPlaceholderReplacement(boolean placeholderReplacement) {
        this.placeholderReplacement = placeholderReplacement;
    }

    /**
     * @param placeholderPrefix The prefix of every placeholder. (default: ${ )<br>Also configurable with Ant Property: ${flyway.placeholderPrefix}
     */
    public void setPlaceholderPrefix(String placeholderPrefix) {
        this.placeholderPrefix = placeholderPrefix;
    }

    /**
     * @param placeholderSuffix The suffix of every placeholder. (default: } )<br>Also configurable with Ant Property: ${flyway.placeholderSuffix}
     */
    public void setPlaceholderSuffix(String placeholderSuffix) {
        this.placeholderSuffix = placeholderSuffix;
    }

    /**
     * Ignore missing migrations when reading the metadata table. These are migrations that were performed by an older deployment of the application that are no
     * longer available in this version. For example: we have migrations available on the classpath with versions 1.0 and 3.0. The metadata table indicates that
     * a migration with version 2.0 (unknown to us) has also been applied. Instead of bombing out (fail fast) with an exception, a warning is logged and Flyway
     * continues normally. This is useful for situations where one must be able to deploy a newer version of the application even though it doesn't contain
     * migrations included with an older one anymore.
     *
     * @param ignoreMissingMigrations {@code true} to continue normally and log a warning, {@code false} to fail fast with an exception. (default: {@code
     *                                false})
     */
    public void setIgnoreMissingMigrations(boolean ignoreMissingMigrations) {
        this.ignoreMissingMigrations = ignoreMissingMigrations;
    }

    /**
     * Ignore ignored migrations when reading the schema history table. These are migrations that were added in between already migrated migrations in this
     * version. For example: we have migrations available on the classpath with versions from 1.0 to 3.0. The schema history table indicates that version 1 was
     * finished on 1.0.15, and the next one was 2.0.0. But with the next release a new migration was added to version 1: 1.0.16. Such scenario is ignored by
     * migrate command, but by default is rejected by validate. When ignoreIgnoredMigrations is enabled, such case will not be reported by validate command.
     * This is useful for situations where one must be able to deliver complete set of migrations in a delivery package for multiple versions of the product,
     * and allows for further development of older versions. <br>Also configurable with Ant Property: ${flyway.ignoreIgnoredMigrations}
     *
     * @param ignoreIgnoredMigrations {@code true} to continue normally and log a warning, {@code false} to fail fast with an exception. (default: {@code
     *                                false})
     */
    public void setIgnoreIgnoredMigrations(boolean ignoreIgnoredMigrations) {
        this.ignoreIgnoredMigrations = ignoreIgnoredMigrations;
    }

    /**
     * @param ignorePendingMigrations {@code true} Ignore pending migrations when reading the schema history table. (default: {@code false})<br> Also
     *                                configurable with Ant Property: ${flyway.ignorePendingMigrations}
     */
    public void setIgnorePendingMigrations(boolean ignorePendingMigrations) {
        this.ignorePendingMigrations = ignorePendingMigrations;
    }

    /**
     * Whether to ignore future migrations when reading the metadata table. These are migrations that were performed by a newer deployment of the application
     * that are not yet available in this version. For example: we have migrations available on the classpath up to version 3.0. The metadata table indicates
     * that a migration to version 4.0 (unknown to us) has already been applied. Instead of bombing out (fail fast) with an exception, a warning is logged and
     * Flyway continues normally. This is useful for situations where one must be able to redeploy an older version of the application after the database has
     * been migrated by a newer one. <br>Also configurable with Ant Property: ${flyway.ignoreFutureMigrations}
     *
     * @param ignoreFutureMigrations {@code true} to continue normally and log a warning, {@code false} to fail fast with an exception. (default: {@code true})
     */
    public void setIgnoreFutureMigrations(boolean ignoreFutureMigrations) {
        this.ignoreFutureMigrations = ignoreFutureMigrations;
    }

    /**
     * @param validateOnMigrate Whether to automatically call validate or not when running migrate. (default: {@code true})<br> Also configurable with Ant
     *                          Property: ${flyway.validateOnMigrate}
     */
    public void setValidateOnMigrate(boolean validateOnMigrate) {
        this.validateOnMigrate = validateOnMigrate;
    }

    /**
     * Whether to automatically call baseline when migrate is executed against a non-empty schema with no metadata table. This schema will then be baselined
     * with the {@code initialVersion} before executing the migrations. Only migrations above {@code initialVersion} will then be applied. <br> This is useful
     * for initial Flyway production deployments on projects with an existing DB. <br> Be careful when enabling this as it removes the safety net that ensures
     * Flyway does not migrate the wrong database in case of a configuration mistake! <br> Also configurable with Ant Property: ${flyway.baselineOnMigrate}
     *
     * @param baselineOnMigrate {@code true} if baseline should be called on migrate for non-empty schemas, {@code false} if not. (default: {@code false})
     */
    public void setBaselineOnMigrate(boolean baselineOnMigrate) {
        this.baselineOnMigrate = baselineOnMigrate;
    }

    /**
     * @param errorOverrides Rules for the built-in error handling that lets you override specific SQL states and errors codes from error to warning or from
     *                       warning to error, comma-separated. (default: *blank*)<br>Also configurable with Ant Property: ${flyway.errorOverrides}
     */
    public void setErrorOverrides(String errorOverrides) {
        this.errorOverrides = StringUtils.tokenizeToStringArray(errorOverrides, ",");
    }

    /**
     * @param dryRunOutput The file where to output the SQL statements of a migration dry run. (default: *Execute directly against the database*)<br>Also
     *                     configurable with Ant Property: ${flyway.dryRunOutput}
     */
    public void setDryRunOutput(String dryRunOutput) {
        this.dryRunOutput = dryRunOutput;
    }

    /**
     * @param oracleSqlplus Whether to Flyway's support for Oracle SQL*Plus commands should be activated. (default: *false*)<br>Also configurable with Ant
     *                      Property: ${flyway.oracleSqlplus}
     */
    public void setOracleSqlplus(boolean oracleSqlplus) {
        this.oracleSqlplus = oracleSqlplus;
    }

    /**
     * @param licenseKey Flyway's license key. (default: *blank*)<br>Also configurable with Ant Property: ${flyway.licenseKey}
     */
    public void setLicenseKey(String licenseKey) {
        this.licenseKey = licenseKey;
    }

    /**
     * Adds placeholders from a nested &lt;placeholders&gt; element. Called by Ant.
     *
     * @param placeholders The fully configured placeholders element.
     */
    public void addConfiguredPlaceholders(PlaceholdersElement placeholders) {
        this.placeholders = placeholders.placeholders;
    }

    /**
     * Do not use. For Ant itself.
     *
     * @param locationsElement The locations on the classpath.
     */
    public void addConfiguredLocations(LocationsElement locationsElement) {
        this.locations = locationsElement.locations.toArray(new String[locationsElement.locations.size()]);
    }

    /**
     * Do not use. For Ant itself.
     *
     * @param resolversElement The resolvers on the classpath.
     */
    public void addConfiguredResolvers(ResolversElement resolversElement) {
        this.resolvers = resolversElement.resolvers.toArray(new String[resolversElement.resolvers.size()]);
    }

    /**
     * Do not use. For Ant itself.
     *
     * @param callbacksElement The callbacks on the classpath.
     */
    public void addConfiguredCallbacks(CallbacksElement callbacksElement) {
        this.callbacks = callbacksElement.callbacks.toArray(new String[callbacksElement.callbacks.size()]);
    }

    /**
     * Do not use. For Ant itself.
     *
     * @param schemasElement The schemas.
     */
    public void addConfiguredSchemas(SchemasElement schemasElement) {
        this.schemas = schemasElement.schemas.toArray(new String[schemasElement.schemas.size()]);
    }

    /**
     * Creates the datasource base on the provided parameters.
     *
     * @return The fully configured datasource.
     */
    protected DataSource createDataSource() {
        String driverValue = useValueIfPropertyNotSet(driver, "driver");
        String urlValue = useValueIfPropertyNotSet(url, "url");
        String userValue = useValueIfPropertyNotSet(user, "user");
        String passwordValue = useValueIfPropertyNotSet(password, "password");

        return new DriverDataSource(Thread.currentThread().getContextClassLoader(), driverValue, urlValue, userValue, passwordValue, null);
    }

    /**
     * Retrieves a value either from an Ant property or if not set, directly.
     *
     * @param value          The value to check.
     * @param flywayProperty The flyway Ant property. Ex. 'url' for 'flyway.url'
     * @return The value.
     */
    protected String useValueIfPropertyNotSet(String value, String flywayProperty) {
        String propertyValue = getProject().getProperty("flyway." + flywayProperty);
        if (propertyValue != null) {
            return propertyValue;
        }

        return value;
    }

    /**
     * Retrieves a boolean value either from an Ant property or if not set, directly.
     *
     * @param value          The boolean value to check.
     * @param flywayProperty The flyway Ant property. Ex. 'url' for 'flyway.url'
     * @return The boolean value.
     */
    protected boolean useValueIfPropertyNotSet(boolean value, String flywayProperty) {
        String propertyValue = getProject().getProperty("flyway." + flywayProperty);
        if (propertyValue != null) {
            return Boolean.parseBoolean(propertyValue);
        }

        return value;
    }

    /**
     * Prepares the classpath this task runs in, so that it includes both the classpath for Flyway and the classpath for the JDBC drivers and migrations.
     */
    private void prepareClassPath() {
        Path classpath = getProject().getReference("flyway.classpath");
        if (classpath != null) {
            setClasspath(classpath);
        } else {
            Reference classpathRef = getProject().getReference("flyway.classpathref");
            if (classpathRef != null) {
                setClasspathref(classpathRef);
            }
        }

        ClassLoader classLoader = new AntClassLoader(getClass().getClassLoader(), getProject(), classPath);
        Thread.currentThread().setContextClassLoader(classLoader);
    }

    @Override
    public void init() throws BuildException {
        AntLogCreator.INSTANCE.setAntProject(getProject());
        LogFactory.setFallbackLogCreator(AntLogCreator.INSTANCE);

        prepareClassPath();

        log = LogFactory.getLog(getClass());

        flywayConfig = Flyway.configure(Thread.currentThread().getContextClassLoader());
        locations = locationsToStrings(flywayConfig.getLocations());
        placeholders = flywayConfig.getPlaceholders();
    }

    @Override
    public void execute() throws BuildException {
        try {
            // first, load configuration from the environment
            flywayConfig.configuration(System.getProperties());

            // second, load configuration from system properties
            Properties projectProperties = new Properties();
            projectProperties.putAll(getProject().getProperties());
            flywayConfig.configuration(projectProperties);

            // last, load configuration from build script properties
            flywayConfig.dataSource(createDataSource());

            if (resolvers != null) {
                flywayConfig.resolvers(resolvers);
            }
            if (callbacks != null) {
                flywayConfig.callbacks(callbacks);
            }
            if (schemas != null) {
                flywayConfig.schemas(schemas);
            }
            if (skipDefaultResolvers != null) {
                flywayConfig.skipDefaultResolvers(skipDefaultResolvers);
            }
            if (skipDefaultCallbacks != null) {
                flywayConfig.skipDefaultCallbacks(skipDefaultCallbacks);
            }
            if (table != null) {
                flywayConfig.table(table);
            }
            if (baselineVersion != null) {
                flywayConfig.baselineVersion(baselineVersion);
            }
            if (baselineDescription != null) {
                flywayConfig.baselineDescription(baselineDescription);
            }
            if (mixed != null) {
                flywayConfig.mixed(mixed);
            }
            if (group != null) {
                flywayConfig.group(group);
            }
            if (stream != null) {
                flywayConfig.stream(stream);
            }
            if (batch != null) {
                flywayConfig.batch(batch);
            }
            if (installedBy != null) {
                flywayConfig.installedBy(installedBy);
            }
            if (encoding != null) {
                flywayConfig.encoding(encoding);
            }
            if (sqlMigrationPrefix != null) {
                flywayConfig.sqlMigrationPrefix(sqlMigrationPrefix);
            }
            if (repeatableSqlMigrationPrefix != null) {
                flywayConfig.repeatableSqlMigrationPrefix(repeatableSqlMigrationPrefix);
            }
            if (undoSqlMigrationPrefix != null) {
                flywayConfig.undoSqlMigrationPrefix(undoSqlMigrationPrefix);
            }
            if (sqlMigrationSeparator != null) {
                flywayConfig.sqlMigrationSeparator(sqlMigrationSeparator);
            }
            if (sqlMigrationSuffixes != null) {
                flywayConfig.sqlMigrationSuffixes(sqlMigrationSuffixes);
            }
            if (target != null) {
                flywayConfig.target(target);
            }
            if (cleanOnValidationError != null) {
                flywayConfig.cleanOnValidationError(cleanOnValidationError);
            }
            if (cleanDisabled != null) {
                flywayConfig.cleanDisabled(cleanDisabled);
            }
            if (outOfOrder != null) {
                flywayConfig.outOfOrder(outOfOrder);
            }
            if (placeholderReplacement != null) {
                flywayConfig.placeholderReplacement(placeholderReplacement);
            }
            if (placeholderPrefix != null) {
                flywayConfig.placeholderPrefix(placeholderPrefix);
            }
            if (placeholderSuffix != null) {
                flywayConfig.placeholderSuffix(placeholderSuffix);
            }
            if (ignoreMissingMigrations != null) {
                flywayConfig.ignoreMissingMigrations(ignoreMissingMigrations);
            }
            if (ignoreIgnoredMigrations != null) {
                flywayConfig.ignoreIgnoredMigrations(ignoreIgnoredMigrations);
            }
            if (ignorePendingMigrations != null) {
                flywayConfig.ignorePendingMigrations(ignorePendingMigrations);
            }
            if (ignoreFutureMigrations != null) {
                flywayConfig.ignoreFutureMigrations(ignoreFutureMigrations);
            }
            if (validateOnMigrate != null) {
                flywayConfig.validateOnMigrate(validateOnMigrate);
            }
            if (baselineOnMigrate != null) {
                flywayConfig.baselineOnMigrate(baselineOnMigrate);
            }
            if (connectRetries != null) {
                flywayConfig.connectRetries(connectRetries);
            }
            if (initSql != null) {
                flywayConfig.initSql(initSql);
            }
            if (errorOverrides != null) {
                flywayConfig.errorOverrides(errorOverrides);
            }
            if (dryRunOutput != null) {
                flywayConfig.dryRunOutput(dryRunOutput);
            }
            if (oracleSqlplus != null) {
                flywayConfig.oracleSqlplus(oracleSqlplus);
            }
            if (licenseKey != null) {
                flywayConfig.licenseKey(licenseKey);
            }

            flywayConfig.locations(getLocations());
            flywayConfig.placeholders(loadPlaceholdersFromProperties(flywayConfig.getPlaceholders(), getProject().getProperties()));

            doExecute(flywayConfig.load());

        } catch (Exception e) {
            throw new BuildException("Flyway Error: " + e.toString(), ExceptionUtils.getRootCause(e));
        }
    }

    /**
     * Executes this task.
     *
     * @param flyway The flyway instance to operate on.
     * @throws Exception any exception
     */
    protected abstract void doExecute(Flyway flyway) throws Exception;

    /**
     * @return The locations configured through Ant.
     */
    private String[] getLocations() {
        String[] locationsVal = locations;
        String locationsProperty = getProject().getProperty("flyway.locations");
        if (locationsProperty != null) {
            locationsVal = StringUtils.tokenizeToStringArray(locationsProperty, ",");
        }

        //Adjust relative locations to be relative from Ant's basedir.
        File baseDir = getProject().getBaseDir();
        for (int i = 0; i < locationsVal.length; i++) {
            locationsVal[i] = adjustRelativeFileSystemLocationToBaseDir(baseDir, locationsVal[i]);
        }

        return locationsVal;
    }

    private String[] locationsToStrings(Location[] locations) {
        String[] locationsString = new String[locations.length];
        for (int i = 0; i < locations.length; i++) {
            locationsString[i] = locations[i].getDescriptor();
        }
        return locationsString;
    }

    /**
     * Adjusts a relative filesystem location to Ant's basedir. All other locations are left untouched.
     *
     * @param baseDir     Ant's basedir.
     * @param locationStr The location to adjust.
     * @return The adjusted location.
     */
    /* private -> testing */
    static String adjustRelativeFileSystemLocationToBaseDir(File baseDir, String locationStr) {
        Location location = new Location(locationStr);
        if (location.isFileSystem() && !new File(location.getPath()).isAbsolute()) {
            return Location.FILESYSTEM_PREFIX + baseDir.getAbsolutePath() + "/" + location.getPath();
        }
        return locationStr;
    }

    /**
     * Load the additional placeholders contained in these properties.
     *
     * @param currentPlaceholders The current placeholders map.
     * @param properties          The properties containing additional placeholders.
     */
    private static Map<String, String> loadPlaceholdersFromProperties(Map<String, String> currentPlaceholders, Hashtable properties) {
        Map<String, String> placeholders = new HashMap<String, String>(currentPlaceholders);
        for (Object property : properties.keySet()) {
            String propertyName = (String) property;
            if (propertyName.startsWith(PLACEHOLDERS_PROPERTY_PREFIX)
                && propertyName.length() > PLACEHOLDERS_PROPERTY_PREFIX.length()) {
                String placeholderName = propertyName.substring(PLACEHOLDERS_PROPERTY_PREFIX.length());
                String placeholderValue = (String) properties.get(propertyName);
                placeholders.put(placeholderName, placeholderValue);
            }
        }
        return placeholders;
    }

    /**
     * The nested &lt;locations&gt; element of the task. Contains 1 or more &lt;location&gt; sub-elements.
     */
    public static class LocationsElement {
        /**
         * The classpath locations.
         */
        List<String> locations = new ArrayList<String>();

        /**
         * Do not use. For Ant itself.
         *
         * @param location A location on the classpath.
         */
        public void addConfiguredLocation(LocationElement location) {
            locations.add(location.path);
        }
    }

    /**
     * One &lt;location&gt; sub-element within the &lt;locations&gt; element.
     */
    public static class LocationElement {
        /**
         * The path of the location.
         */
        private String path;

        /**
         * Do not use. For Ant itself.
         *
         * @param path The path of the location.
         */
        public void setPath(String path) {
            this.path = path;
        }
    }

    /**
     * The nested &lt;schemas&gt; element of the task. Contains 1 or more &lt;schema&gt; sub-elements.
     */
    public static class SchemasElement {
        /**
         * The schema names.
         */
        List<String> schemas = new ArrayList<String>();

        /**
         * Do not use. For Ant itself.
         *
         * @param schema A schema.
         */
        public void addConfiguredLocation(SchemaElement schema) {
            schemas.add(schema.name);
        }
    }

    /**
     * One &lt;location&gt; sub-element within the &lt;locations&gt; element.
     */
    public static class SchemaElement {
        /**
         * The name of the schema.
         */
        private String name;

        /**
         * Do not use. For Ant itself.
         *
         * @param name The name of the schema.
         */
        public void setPath(String name) {
            this.name = name;
        }
    }

    /**
     * The nested &lt;resolvers&gt; element of the task. Contains 1 or more &lt;resolver&gt; sub-elements.
     */
    public static class ResolversElement {
        /**
         * The classpath locations.
         */
        List<String> resolvers = new ArrayList<String>();

        /**
         * Do not use. For Ant itself.
         *
         * @param resolver A resolver on the classpath.
         */
        public void addConfiguredResolver(ResolverElement resolver) {
            resolvers.add(resolver.clazz);
        }
    }

    /**
     * One &lt;resolver&gt; sub-element within the &lt;resolvers&gt; element.
     */
    public static class ResolverElement {
        /**
         * The fully qualified class name of the resolver.
         */
        private String clazz;

        /**
         * Do not use. For Ant itself.
         *
         * @param clazz The fully qualified class name of the resolver.
         */
        public void setClass(String clazz) {
            this.clazz = clazz;
        }
    }

    /**
     * The nested &lt;callbacks&gt; element of the task. Contains 1 or more &lt;callback&gt; sub-elements.
     */
    public static class CallbacksElement {
        /**
         * The classpath locations.
         */
        List<String> callbacks = new ArrayList<String>();

        /**
         * Do not use. For Ant itself.
         *
         * @param callback A callback on the classpath.
         */
        public void addConfiguredCallback(CallbackElement callback) {
            callbacks.add(callback.clazz);
        }
    }

    /**
     * One &lt;callback&gt; sub-element within the &lt;callbacks&gt; element.
     */
    public static class CallbackElement {
        /**
         * The fully qualified class name of the callback.
         */
        private String clazz;

        /**
         * Do not use. For Ant itself.
         *
         * @param clazz The fully qualified class name of the callback.
         */
        public void setClass(String clazz) {
            this.clazz = clazz;
        }
    }

    /**
     * Nested &lt;placeholders&gt; element of the migrate Ant task.
     */
    public static class PlaceholdersElement {
        /**
         * A map of &lt;placeholder, replacementValue&gt; to apply to sql migration scripts.
         */
        Map<String, String> placeholders = new HashMap<String, String>();

        /**
         * Adds a placeholder from a nested &lt;placeholder&gt; element. Called by Ant.
         *
         * @param placeholder The fully configured placeholder element.
         */
        public void addConfiguredPlaceholder(PlaceholderElement placeholder) {
            placeholders.put(placeholder.name, placeholder.value);
        }
    }

    /**
     * Nested &lt;placeholder&gt; element inside the &lt;placeholders&gt; element of the migrate Ant task.
     */
    public static class PlaceholderElement {
        /**
         * The name of the placeholder.
         */
        private String name;

        /**
         * The value of the placeholder.
         */
        private String value;

        /**
         * @param name The name of the placeholder.
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * @param value The value of the placeholder.
         */
        public void setValue(String value) {
            this.value = value;
        }
    }
}