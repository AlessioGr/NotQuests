package com.notquests.core.managers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PlayerDatabaseConnectionTest {
    @TempDir
    Path tempDir;

    @Test
    void opensDefaultSqliteDatabaseInNotQuestsDataFolder() throws Exception {
        final PlayerDatabase provider = new PlayerDatabase(tempDir, null);

        provider.open(new ConfigurationManager());

        assertTrue(Files.exists(tempDir.resolve("database_sqlite.db")));
        try (var connection = provider.connection()) {
            assertFalse(connection.isClosed());
        } finally {
            provider.close();
        }
    }
}
