package io.clickhandler.email.jooq;

import io.clickhandler.sql.SchemaGenerator;
import io.clickhandler.sql.SqlConfig;
import io.clickhandler.sql.SqlDatabase;
import io.vertx.rxjava.core.Vertx;
import org.jooq.util.GenerationTool;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 *
 */
public class JOOQAppGenerator {
    public static void main(String[] args) throws Throwable {
        final SqlConfig dbConfig = new SqlConfig();
        dbConfig.setGenerateSchema(true);

        final SqlDatabase database = new SqlDatabase(
            Vertx.vertx(),
            dbConfig,
            new String[]{"io.clickhandler.email.entity"},
            new String[]{"io.clickhandler.data.schema"}
        );

        database.startAsync().awaitRunning();

        // Run jOOQ GenerationTool.
        GenerationTool.generate(
            SchemaGenerator.buildConfiguration(
                dbConfig,
                "io.clickhandler.data.schema",
                "email-schema/src/main/javaio/clickhandler"
            )
        );

        // Set Tables class to interface.
        try {
            String content = new String(Files.readAllBytes(Paths.get("email-schema/src/main/java/io/clickhandler/data/schema/Tables.java")), StandardCharsets.UTF_8);
            content = content.replaceAll("public class Tables", "public interface Tables");
            File tempFile = new File("email-schema/src/main/java/io/clickhandler/data/schema/Tables.java");
            Files.write(tempFile.toPath(), content.getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new RuntimeException("Error updating Tables class to interface!", e);
        }

        database.stopAsync().awaitTerminated();
    }
}
