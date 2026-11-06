package com.hospital.auth;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards against migration files that Flyway would happily mark as "applied" while running nothing:
 * empty files, files that were zero-filled (all NUL bytes) by a bad copy/sync, or files with no SQL in them.
 */
class MigrationFilesTest {

    private static final Pattern SQL_STATEMENT =
            Pattern.compile("\\b(CREATE|ALTER|INSERT|UPDATE|DROP|COMMENT)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern FLYWAY_NAME = Pattern.compile("V\\d+__[a-z0-9_]+\\.sql");

    @Test
    void everyMigrationContainsRealSql() throws IOException {
        Resource[] migrations = new PathMatchingResourcePatternResolver().getResources("classpath:db/migration/*.sql");
        assertThat(migrations).as("migrations on the classpath").isNotEmpty();

        for (Resource migration : migrations) {
            String name = migration.getFilename();
            byte[] bytes = migration.getContentAsByteArray();
            String sql = new String(bytes, StandardCharsets.UTF_8);

            assertThat(name).as("file name").matches(FLYWAY_NAME);
            assertThat(bytes.length).as("%s size", name).isGreaterThan(0);
            assertThat(sql).as("%s must not contain NUL bytes (zero-filled file?)", name).doesNotContain("\0");
            assertThat(SQL_STATEMENT.matcher(sql).find()).as("%s contains at least one SQL statement", name).isTrue();
        }
    }

    @Test
    void versionsAreUniqueAndContiguous() throws IOException {
        int[] versions = Arrays.stream(new PathMatchingResourcePatternResolver().getResources("classpath:db/migration/*.sql"))
                .map(Resource::getFilename)
                .mapToInt(n -> Integer.parseInt(n.substring(1, n.indexOf("__"))))
                .sorted()
                .toArray();
        for (int i = 0; i < versions.length; i++) {
            assertThat(versions[i]).as("migration versions should be V1..V%d with no gaps or duplicates", versions.length)
                    .isEqualTo(i + 1);
        }
    }
}
