/*
 * Copyright 2017-2020 Tomas Tulka
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
import org.flywaydb.core.internal.license.FlywayTeamsUpgradeRequiredException;
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
     * Property name prefix for JDBC properties that are configured through properties.
     */
    private static final String JDBC_PROPERTIES_PREFIX = "flyway.jdbcProperties.";

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
     * The fully qualified classname of the jdbc driver to use to connect to the database.
     */
    private String driver;

    /**
     * The jdbc url to use to connect to the database.
     */
    private String url;

    /**
     * The user to use to connect to the database.
     */
    private String user;

    /**
     * The password to use to connect to the database.
     */
    private String password;

    /**
     * Locations on the classpath to scan recursively for migrations. Locations may contain both sql and java-based migrations. (default: db.migration)
     */
    private String[] locations;

    /**
     * The custom MigrationResolvers to be used in addition or as replacement to the built-in (as determined by the skipDefaultResolvers property) ones for
     * resolving Migrations to apply.
     */
    private String[] resolvers;

    /**
     * The callbacks for lifecycle notifications.
     */
    private String[] callbacks;

    /**
     * A map of &lt;placeholder, replacementValue&gt; to apply to sql migration scripts.
     */
    private Map<String, String> placeholders;

    /**
     * A map of JDBC properties to pass to the JDBC driver when establishing a connection.
     */
    private Map<String, String> jdbcProperties;

    /**
     * @param classpath The classpath used to load the JDBC driver and the migrations.
     */
    public void setClasspath(Path classpath) {
        this.classPath = classpath;
    }

    /**
     * @param classpathref The reference to the classpath used to load the JDBC driver and the migrations.
     */
    public void setClasspathref(Reference classpathref) {
        Path classPath = new Path(getProject());
        classPath.setRefid(classpathref);
        this.classPath = classPath;
    }

    /**
     * @param driver The fully qualified classname of the jdbc driver to use to connect to the database. By default, the driver is autodetected based on the
     *               url.
     */
    public void setDriver(String driver) {
        this.driver = driver;
    }

    /**
     * @param url The jdbc url to use to connect to the database.
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * @param user The user to use to connect to the database.
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * @param password The password to use to connect to the database.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @param connectRetries The maximum number of retries when attempting to connect to the database. (default: 0)
     */
    public void setConnectRetries(int connectRetries) {
        this.flywayConfig.connectRetries(connectRetries);
    }

    /**
     * @param initSql The SQL statements to run to initialize a new database connection immediately after opening it.
     */
    public void setInitSql(String initSql) {
        this.flywayConfig.initSql(initSql);
    }

    /**
     * @param schemas Comma-separated list of the schemas managed by Flyway. These schema names are case-sensitive. (default: The default schema for the
     *                datasource connection)
     */
    public void setSchemas(String schemas) {
        this.flywayConfig.schemas(StringUtils.tokenizeToStringArray(schemas, ","));
    }

    /**
     * @param createSchemas Whether Flyway should attempt to create the schemas specified in the schemas property.
     */
    public void setCreateSchemas(boolean createSchemas) {
        this.flywayConfig.createSchemas(createSchemas);
    }

    /**
     * @param defaultSchema Sets the default schema managed by Flyway. If not specified, but flyway.schemas is, we use the first schema in that list.
     */
    public void setDefaultSchema(String defaultSchema) {
        this.flywayConfig.defaultSchema(defaultSchema);
    }

    /**
     * @param resolvers The custom MigrationResolvers to be used in addition to the built-in ones for resolving Migrations to apply.
     */
    public void setResolvers(String resolvers) {
        this.resolvers = StringUtils.tokenizeToStringArray(resolvers, ",");
    }

    /**
     * @param skipDefaultResolvers Whether built-int resolvers should be skipped. If true, only custom resolvers are used. (default: false)
     */
    public void setSkipDefaultResolvers(boolean skipDefaultResolvers) {
        this.flywayConfig.skipDefaultResolvers(skipDefaultResolvers);
    }

    /**
     * @param callbacks A comma-separated list of fully qualified FlywayCallback implementation class names.
     */
    public void setCallbacks(String callbacks) {
        this.callbacks = StringUtils.tokenizeToStringArray(callbacks, ",");
    }

    /**
     * @param skipDefaultCallbacks Whether built-int callbacks should be skipped. If true, only custom callbacks are used. (default: false)
     */
    public void setSkipDefaultCallbacks(boolean skipDefaultCallbacks) {
        this.flywayConfig.skipDefaultCallbacks(skipDefaultCallbacks);
    }

    /**
     * @param table <p>The name of the schema metadata table that will be used by Flyway. By default (single-schema mode) the metadata table is placed
     *              in the default schema for the connection provided by the datasource.
     */
    public void setTable(String table) {
        this.flywayConfig.table(table);
    }

    /**
     * @param baselineVersion The version to tag an existing schema with when executing baseline. (default: 1)
     */
    public void setBaselineVersion(String baselineVersion) {
        this.flywayConfig.baselineVersion(baselineVersion);
    }

    /**
     * @param baselineDescription The description to tag an existing schema with when executing baseline. (default: &lt;&lt; Flyway Baseline &gt;&gt;)
     */
    public void setBaselineDescription(String baselineDescription) {
        this.flywayConfig.baselineDescription(baselineDescription);
    }

    /**
     * @param mixed Whether to allow mixing transactional and non-transactional statements within the same migration. (default: false)
     */
    public void setMixed(boolean mixed) {
        this.flywayConfig.mixed(mixed);
    }

    /**
     * @param group true if migrations should be grouped. false if they should be applied individually instead. (default: false)
     */
    public void setGroup(boolean group) {
        this.flywayConfig.group(group);
    }

    /**
     * @param stream true if migrations should be streamed. false if they should be loaded individually instead. (default: false)
     */
    public void setStream(boolean stream) {
        this.flywayConfig.stream(stream);
    }

    /**
     * @param batch true if SQL statements should be batched. false if they should be sent individually instead. (default: false)
     */
    public void setBatch(boolean batch) {
        this.flywayConfig.batch(batch);
    }

    /**
     * @param installedBy The username or <i>blank</i> for the current database user of the connection.
     */
    public void setInstalledBy(String installedBy) {
        this.flywayConfig.installedBy(installedBy);
    }

    /**
     * @param encoding The encoding of Sql migrations. (default: UTF-8)
     */
    public void setEncoding(String encoding) {
        this.flywayConfig.encoding(encoding);
    }

    /**
     * @param sqlMigrationPrefix The file name prefix for Sql migrations (default: V).
     */
    public void setSqlMigrationPrefix(String sqlMigrationPrefix) {
        this.flywayConfig.sqlMigrationPrefix(sqlMigrationPrefix);
    }

    /**
     * @param repeatableSqlMigrationPrefix The file name prefix for repeatable sql migrations (default: R)
     */
    public void setRepeatableSqlMigrationPrefix(String repeatableSqlMigrationPrefix) {
        this.flywayConfig.repeatableSqlMigrationPrefix(repeatableSqlMigrationPrefix);
    }

    /**
     * @param undoSqlMigrationPrefix The file name prefix for undo SQL migrations (default: U)
     */
    public void setUndoSqlMigrationPrefix(String undoSqlMigrationPrefix) {
        this.flywayConfig.undoSqlMigrationPrefix(undoSqlMigrationPrefix);
    }

    /**
     * @param sqlMigrationSeparator The file name separator for Sql migrations (default: V)
     */
    public void setSqlMigrationSeparator(String sqlMigrationSeparator) {
        this.flywayConfig.sqlMigrationSeparator(sqlMigrationSeparator);
    }

    /**
     * @param sqlMigrationSuffixes The file name suffixes for SQL migrations, comma-separated. (default: .sql)
     */
    public void setSqlMigrationSuffixes(String sqlMigrationSuffixes) {
        this.flywayConfig.sqlMigrationSuffixes(StringUtils.tokenizeToStringArray(sqlMigrationSuffixes, ","));
    }

    /**
     * @param validateMigrationNaming Whether to ignore migration files whose names do not match the naming conventions. (default: false)
     */
    public void setValidateMigrationNaming(boolean validateMigrationNaming) {
        this.flywayConfig.validateMigrationNaming(validateMigrationNaming);
    }

    /**
     * @param tablespace where to create the schema history table.
     */
    public void setTablespace(String tablespace) {
        this.flywayConfig.tablespace(tablespace);
    }

    /**
     * @param target The target version up to which Flyway should consider migrations. Migrations with a higher version number will be ignored. The special
     *               value {@code current} designates the current version of the schema. (default: the latest version)
     */
    public void setTarget(String target) {
        this.flywayConfig.target(target);
    }

    /**
     * @param cleanOnValidationError Whether to automatically call clean or not when a validation error occurs. (default: false)
     */
    public void setCleanOnValidationError(boolean cleanOnValidationError) {
        this.flywayConfig.cleanOnValidationError(cleanOnValidationError);
    }

    /**
     * @param cleanDisabled Whether to disable clean. (default: false)
     */
    public void setCleanDisabled(boolean cleanDisabled) {
        this.flywayConfig.cleanDisabled(cleanDisabled);
    }

    /**
     * @param outOfOrder Allows migrations to be run "out of order" (default: false).
     */
    public void setOutOfOrder(boolean outOfOrder) {
        this.flywayConfig.outOfOrder(outOfOrder);
    }

    /**
     * @param placeholderReplacement Whether placeholders should be replaced. (default: true)
     */
    public void setPlaceholderReplacement(boolean placeholderReplacement) {
        this.flywayConfig.placeholderReplacement(placeholderReplacement);
    }

    /**
     * @param placeholderPrefix The prefix of every placeholder. (default: ${)
     */
    public void setPlaceholderPrefix(String placeholderPrefix) {
        this.flywayConfig.placeholderPrefix(placeholderPrefix);
    }

    /**
     * @param placeholderSuffix The suffix of every placeholder. (default: })
     */
    public void setPlaceholderSuffix(String placeholderSuffix) {
        this.flywayConfig.placeholderSuffix(placeholderSuffix);
    }

    /**
     * @param ignoreMissingMigrations true to continue normally and log a warning, false to fail fast with an exception. (default: false)
     */
    public void setIgnoreMissingMigrations(boolean ignoreMissingMigrations) {
        this.flywayConfig.ignoreMissingMigrations(ignoreMissingMigrations);
    }

    /**
     * Ignore ignored migrations when reading the schema history table.
     *
     * @param ignoreIgnoredMigrations true to continue normally and log a warning, false to fail fast with an exception. (default: false)
     */
    public void setIgnoreIgnoredMigrations(boolean ignoreIgnoredMigrations) {
        this.flywayConfig.ignoreIgnoredMigrations(ignoreIgnoredMigrations);
    }

    /**
     * @param ignorePendingMigrations true Ignore pending migrations when reading the schema history table. (default: false)
     */
    public void setIgnorePendingMigrations(boolean ignorePendingMigrations) {
        this.flywayConfig.ignorePendingMigrations(ignorePendingMigrations);
    }

    /**
     * @param ignoreFutureMigrations true to continue normally and log a warning, false to fail fast with an exception. (default: true)
     */
    public void setIgnoreFutureMigrations(boolean ignoreFutureMigrations) {
        this.flywayConfig.ignoreFutureMigrations(ignoreFutureMigrations);
    }

    /**
     * @param validateOnMigrate Whether to automatically call validate or not when running migrate. (default: true)
     */
    public void setValidateOnMigrate(boolean validateOnMigrate) {
        this.flywayConfig.validateOnMigrate(validateOnMigrate);
    }

    /**
     * @param baselineOnMigrate true if baseline should be called on migrate for non-empty schemas, false if not. (default: false)
     */
    public void setBaselineOnMigrate(boolean baselineOnMigrate) {
        this.flywayConfig.baselineOnMigrate(baselineOnMigrate);
    }

    /**
     * @param errorOverrides Rules for the built-in error handling that lets you override specific SQL states and errors codes from error to warning or from
     *                       warning to error, comma-separated.
     */
    public void setErrorOverrides(String errorOverrides) {
        this.flywayConfig.errorOverrides(StringUtils.tokenizeToStringArray(errorOverrides, ","));
    }

    /**
     * @param dryRunOutput The file where to output the SQL statements of a migration dry run. (default: *Execute directly against the database*)
     */
    public void setDryRunOutput(String dryRunOutput) {
        this.flywayConfig.dryRunOutput(dryRunOutput);
    }

    /**
     * @param cherryPicks A Comma separated list of migrations that Flyway should consider when migrating, undoing, or repairing. Leave blank to consider all
     *                    discovered migrations.
     */
    public void setCherryPick(String cherryPicks) {
        this.flywayConfig.cherryPick(StringUtils.tokenizeToStringArray(cherryPicks, ","));
    }

    /**
     * @param outputQueryResults Controls whether Flyway should output a table with the results of queries when executing migrations.
     */
    public void setOutputQueryResults(boolean outputQueryResults) {
        // FIXME not possible with the current api
        //this.flywayConfig.outputQueryResults(outputQueryResults);
    }

    /**
     * @param skipExecutingMigrations Whether Flyway should skip actually executing the contents of the migrations and only update the schema history table.
     */
    public void setSkipExecutingMigrations(boolean skipExecutingMigrations) {
        this.flywayConfig.skipExecutingMigrations(skipExecutingMigrations);
    }

    /**
     * @param oracleSqlplus Whether to Flyway's support for Oracle SQL*Plus commands should be activated. (default: false)
     */
    public void setOracleSqlplus(boolean oracleSqlplus) {
        this.flywayConfig.oracleSqlplus(oracleSqlplus);
    }

    /**
     * @param oracleSqlplusWarn Whether Flyway should issue a warning instead of an error whenever it encounters an Oracle SQL*Plus statement it doesn't yet
     *                          support. (default: false)
     */
    public void setOracleSqlplusWarn(boolean oracleSqlplusWarn) {
        this.flywayConfig.oracleSqlplusWarn(oracleSqlplusWarn);
    }

    /**
     * @param licenseKey Flyway's license key.
     */
    public void setLicenseKey(String licenseKey) {
        this.flywayConfig.licenseKey(licenseKey);
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
     * Adds jdbcProperty from a nested &lt;jdbcProperties&gt; element. Called by Ant.
     *
     * @param jdbcProperties The fully configured placeholders element.
     */
    public void addConfiguredJdbcProperties(JdbcPropertiesElement jdbcProperties) {
        this.jdbcProperties = jdbcProperties.jdbcProperties;
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
        this.flywayConfig.schemas(schemasElement.schemas.toArray(new String[schemasElement.schemas.size()]));
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

        return new DriverDataSource(Thread.currentThread().getContextClassLoader(), driverValue, urlValue, userValue, passwordValue);
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
        LogFactory.setLogCreator(AntLogCreator.INSTANCE);
        log = LogFactory.getLog(getClass());

        prepareClassPath();

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

            flywayConfig.locations(getLocations());
            flywayConfig.placeholders(loadPlaceholdersFromProperties(flywayConfig.getPlaceholders(), getProject().getProperties()));

            // jdbc properties only for the licensed version
            Map<String, String> jdbcProperties = loadJdbcPropertiesFromProperties(getJdbcPropertiesSafely(), getProject().getProperties());
            if (!jdbcProperties.isEmpty()) {
                flywayConfig.jdbcProperties(jdbcProperties);
            }

            doExecute(flywayConfig.load());

        } catch (Exception e) {
            throw new BuildException("Flyway Error: " + e.getMessage(), ExceptionUtils.getRootCause(e));
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
     * This must be done due to licensing, as the free variant throws an exception.
     */
    private Map<String, String> getJdbcPropertiesSafely() {
        try {
            return flywayConfig.getJdbcProperties();
        } catch (FlywayTeamsUpgradeRequiredException ignore) {
        }
        return new HashMap<>();
    }

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
            if (propertyName.startsWith(PLACEHOLDERS_PROPERTY_PREFIX) && propertyName.length() > PLACEHOLDERS_PROPERTY_PREFIX.length()) {
                String name = propertyName.substring(PLACEHOLDERS_PROPERTY_PREFIX.length());
                String value = (String) properties.get(propertyName);
                placeholders.put(name, value);
            }
        }
        return placeholders;
    }

    /**
     * Load the JDBC properties contained in these properties.
     *
     * @param currentJdbcProperties The current JDBC properties map.
     * @param properties            The properties containing additional JDBC properties.
     */
    private static Map<String, String> loadJdbcPropertiesFromProperties(Map<String, String> currentJdbcProperties, Hashtable properties) {
        Map<String, String> jdbcProperties = new HashMap<>(currentJdbcProperties);
        for (Object property : properties.keySet()) {
            String propertyName = (String) property;
            if (propertyName.startsWith(JDBC_PROPERTIES_PREFIX) && propertyName.length() > JDBC_PROPERTIES_PREFIX.length()) {
                String name = propertyName.substring(JDBC_PROPERTIES_PREFIX.length());
                String value = (String) properties.get(propertyName);
                jdbcProperties.put(name, value);
            }
        }
        return jdbcProperties;
    }

    /**
     * The nested &lt;locations&gt; element of the task. Contains 1 or more &lt;location&gt; sub-elements.
     */
    public static class LocationsElement {
        /**
         * The classpath locations.
         */
        List<String> locations = new ArrayList<>();

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
        List<String> schemas = new ArrayList<>();

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
        List<String> resolvers = new ArrayList<>();

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
        List<String> callbacks = new ArrayList<>();

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
        Map<String, String> placeholders = new HashMap<>();

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

    /**
     * Nested &lt;jdbcProperties&gt; element of the migrate Ant task.
     */
    public static class JdbcPropertiesElement {
        /**
         * A map of JDBC properties to apply to sql migration scripts.
         */
        Map<String, String> jdbcProperties = new HashMap<>();

        /**
         * Adds a jdbc property from a nested element. Called by Ant.
         *
         * @param jdbcProperty The fully configured jdbcProperty element.
         */
        public void addConfiguredPlaceholder(JdbcPropertyElement jdbcProperty) {
            jdbcProperties.put(jdbcProperty.name, jdbcProperty.value);
        }
    }

    /**
     * Nested &lt;jdbcProperty&gt; element inside the &lt;jdbcProperties&gt; element of the migrate Ant task.
     */
    public static class JdbcPropertyElement {
        /**
         * The name of the JDBC property.
         */
        private String name;

        /**
         * The value of the JDBC property.
         */
        private String value;

        /**
         * @param name The name of the JDBC property.
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * @param value The value of the JDBC property.
         */
        public void setValue(String value) {
            this.value = value;
        }
    }
}