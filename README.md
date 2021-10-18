#  Flyway Ant Tasks

Flyway Ant plugin provided as an AntLib.

## Dependency

```xml
<dependency>
    <groupId>com.ttulka.flyway</groupId>
    <artifactId>flyway-ant</artifactId>
    <version>2.16.1</version>
</dependency>
```

## Usage

```xml
<project ... xmlns:flyway="antlib:org.flywaydb.ant">
   <path id="flyway.lib.path">
       <!-- include all jars from the Flyway AntLib distribution -->
   </path>

   <path id="flyway.classpath">
       <!-- include all jars containing jdbc drivers -->
       <!-- include all jars and directories containing sql migrations -->
       <!-- include all jars and directories containing compiled java migrations -->
   </path>

   <taskdef uri="antlib:org.flywaydb.ant" resource="org/flywaydb/ant/antlib.xml"
       classpathref="flyway.lib.path"/>
</project>
```

## Tasks

| Name         | Description | 
| ------------ | ------------|
| `migrate`    | Migrates the database. |
| `clean`      | Drops all objects in the configured schemas. |
| `info`       | Prints the details and status information about all the migrations. |
| `validate`   | Validates the applied migrations against the ones available on the classpath. |
| `baseline`   | Baselines an existing database, excluding all migrations up to and including baselineVersion. |
| `repair`     | Repairs the metadata table. |

## Configuration

The Flyway Ant tasks can be configured in the following ways:

### Attributes of the task

```xml
<flyway:migrate driver="com.myvendor.Driver" password="mySecretPwd">
    <locations>
        <location path="largetest/migrations1"/>
        <location path="largetest/migrations2"/>
    </locations>
    <placeholders>
        <placeholder name="name" value="Mr. T"/>
    </placeholders>
    <schemas>
        <schema name="schema1"/>
    </schemas>
    <resolvers>
        <resolver class="com.mycomp.MyMigrationResolver"/>
    </resolvers>
    <callbacks>
        <callback class="com.mycomp.MyCallback"/>
    </callbacks>
</flyway:migrate>
```

### Through Ant properties

```xml
<!-- Properties are prefixed with flyway. -->
<property name="flyway.password" value="mySecretPwd"/>

<!-- List are defined as comma-separated values -->
<property name="flyway.schemas" value="schema1,schema2,schema3"/>

<!-- Individual placeholders are prefixed by flyway.placeholders. -->
<property name="flyway.placeholders.keyABC" value="valueXYZ"/>
<property name="flyway.placeholders.otherplaceholder" value="value123"/>
```

### Through System properties

```
ant -Dflyway.user=myUser -Dflyway.schemas=schema1,schema2 -Dflyway.placeholders.keyABC=valueXYZ
```

System properties *override* Ant properties *override* Task attributes.

## Documentation 

For more details see the [Wiki pages](https://github.com/flyway/flyway-ant/wiki).

## Maintenance
This repository is a community project and not officially maintained by the Flyway Team at Redgate.
This project is looked after only by the open source community. Community Maintainers are people who have agreed to be contacted with queries for support and maintenance.
Community Maintainers: 
- [@ttulka](https://github.com/ttulka)

If you would like to be named as an Community Maintainer, let us know via Twitter: https://twitter.com/flywaydb.


## License

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
