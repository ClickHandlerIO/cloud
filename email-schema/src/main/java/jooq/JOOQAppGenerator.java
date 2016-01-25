package jooq;

import io.clickhandler.sql.db.Database;
import io.clickhandler.sql.db.DbConfig;
import io.clickhandler.sql.db.SchemaGenerator;
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
        final DbConfig dbConfig = new DbConfig();
        dbConfig.setGenerateSchema(true);

        final Database database = new Database(
            dbConfig,
            new String[]{"entity"},
            new String[]{"data.schema"}
        );

        database.startAsync().awaitRunning();

        // Run jOOQ GenerationTool.
        GenerationTool.generate(
            SchemaGenerator.buildConfiguration(
                dbConfig,
                "data.schema",
                "email-schema/src/main/java"
            )
        );

        // Set Tables class to interface.
        try {
            String content = new String(Files.readAllBytes(Paths.get("email-schema/src/main/java/data/schema/Tables.java")), StandardCharsets.UTF_8);
            content = content.replaceAll("public class Tables", "public interface Tables");
            File tempFile = new File("email-schema/src/main/java/data/schema/Tables.java");
            Files.write(tempFile.toPath(), content.getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new RuntimeException("Error updating Tables class to interface!", e);
        }

        database.stopAsync().awaitTerminated();
    }
}
