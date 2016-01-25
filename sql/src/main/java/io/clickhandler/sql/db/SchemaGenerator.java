package io.clickhandler.sql.db;

import com.google.common.base.Strings;
import org.jooq.util.jaxb.*;
import org.jooq.util.jaxb.Database;

/**
 *
 */
public class SchemaGenerator {
    public static Configuration buildConfiguration(DbConfig dbConfig, String packageName, String directory) {
        final String url = Strings.nullToEmpty(dbConfig.getUrl()).trim();

        String jdbcDriver = "";
        String jooqDatabase = "";
        String inputSchema = "";

        if (url.startsWith("jdbc:h2")) {
            jdbcDriver = "org.h2.Driver";
            jooqDatabase = "org.jooq.util.h2.H2Database";
            inputSchema = "public";
        } else if (url.startsWith("jdbc:mysql")) {
            jdbcDriver = "com.mysql.jdbc.Driver";
            jooqDatabase = "org.jooq.util.mysql.MySQLDatabase";
            inputSchema = dbConfig.getSchema();
        } else if (url.startsWith("jdbc:postgres")) {
            jdbcDriver = "org.postgresql.Driver";
            jooqDatabase = "org.jooq.util.postgres.PostgresDatabase";
            inputSchema = dbConfig.getSchema();
        }

        return new Configuration()
            .withJdbc(new Jdbc()
                .withDriver(jdbcDriver)
                .withUrl(url)
                .withUser(dbConfig.getUser())
                .withPassword(dbConfig.getPassword()))
            .withGenerator(new org.jooq.util.jaxb.Generator()
                .withName("org.jooq.util.DefaultGenerator")
                .withGenerate(new Generate()
                    .withFluentSetters(true))
                .withDatabase(new Database()
                    .withRecordVersionFields("version")
                    .withRecordTimestampFields("changed")
                    .withName(jooqDatabase)
                    .withIncludes(".*")
                    .withExcludes(".*Journal")
                    .withOutputSchemaToDefault(true)
                    .withInputSchema(inputSchema))
                .withTarget(new Target()
                    .withPackageName(packageName)
                    .withDirectory(directory)));
    }
}