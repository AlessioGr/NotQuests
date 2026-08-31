package com.notquests.core.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.notquests.core.config.YamlConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class BackupManagerTest {
    @TempDir
    Path dataFolder;

    @Test
    void backsUpQuestConfigThroughPlatformSaveHook() throws Exception {
        final TestHooks hooks = new TestHooks();
        final BackupManager backups = new BackupManager(dataFolder, hooks);

        backups.backupQuestConfig("default", backupFile -> Files.writeString(backupFile, "quests: {}\n"));

        final List<Path> backupsCreated;
        try (var files = Files.list(dataFolder.resolve("backups"))) {
            backupsCreated = files.toList();
        }

        assertEquals(1, backupsCreated.size());
        assertTrue(backupsCreated.getFirst().getFileName().toString().startsWith("quests-backup-default-"));
        assertEquals("quests: {}\n", Files.readString(backupsCreated.getFirst()));
        assertTrue(hooks.infoMessages.stream().anyMatch(message -> message.contains("successfully backed up")));
    }

    @Test
    void backsUpAllExistingCategoryQuestConfigs() throws Exception {
        final TestHooks hooks = new TestHooks();
        final BackupManager backups = new BackupManager(dataFolder, hooks);
        Files.createDirectories(dataFolder.resolve("default"));
        Files.createDirectories(dataFolder.resolve("custom"));
        Files.writeString(dataFolder.resolve("default").resolve("quests.yml"), "default quests");
        Files.writeString(dataFolder.resolve("custom").resolve("quests.yml"), "custom quests");

        backups.backupQuestConfigs(
                List.of("default", "custom", "missing"),
                category -> dataFolder.resolve(category).resolve("quests.yml"));

        final List<String> backupsCreated;
        try (var files = Files.list(dataFolder.resolve("backups"))) {
            backupsCreated = files
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .toList();
        }

        assertEquals(2, backupsCreated.size());
        assertTrue(backupsCreated.stream().anyMatch(name -> name.startsWith("quests-backup-custom-")));
        assertTrue(backupsCreated.stream().anyMatch(name -> name.startsWith("quests-backup-default-")));
    }

    @Test
    void backsUpQuestConfigsFromCoreCategoryFolderLayout() throws Exception {
        final TestHooks hooks = new TestHooks();
        final BackupManager backups = new BackupManager(dataFolder, hooks);
        Files.createDirectories(dataFolder.resolve("default"));
        Files.createDirectories(dataFolder.resolve("story").resolve("daily"));
        Files.writeString(dataFolder.resolve("default").resolve("quests.yml"), "default quests");
        Files.writeString(dataFolder.resolve("story").resolve("daily").resolve("quests.yml"), "daily quests");

        backups.backupQuestConfigs(List.of("default", "story.daily", "missing"));

        final List<String> backupsCreated;
        try (var files = Files.list(dataFolder.resolve("backups"))) {
            backupsCreated = files
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .toList();
        }

        assertEquals(2, backupsCreated.size());
        assertTrue(backupsCreated.stream().anyMatch(name -> name.startsWith("quests-backup-default-")));
        assertTrue(backupsCreated.stream().anyMatch(name -> name.startsWith("quests-backup-story.daily-")));
    }

    @Test
    void shutdownQuestBackupHonorsEnabledAndLoadingPolicy() throws Exception {
        final TestHooks hooks = new TestHooks();
        final BackupManager backups = new BackupManager(dataFolder, hooks);
        Files.createDirectories(dataFolder.resolve("default"));
        Files.writeString(dataFolder.resolve("default").resolve("quests.yml"), "default quests");

        backups.backupQuestConfigsOnShutdown(false, false, List.of("default"));
        assertTrue(Files.notExists(dataFolder.resolve("backups")));

        backups.backupQuestConfigsOnShutdown(true, true, List.of("default"));
        assertTrue(hooks.warnMessages.stream().anyMatch(message -> message.contains("currently loading")));
        assertTrue(Files.notExists(dataFolder.resolve("backups")));

        backups.backupQuestConfigsOnShutdown(true, false, List.of("default"));

        final List<String> backupsCreated;
        try (var files = Files.list(dataFolder.resolve("backups"))) {
            backupsCreated = files.map(path -> path.getFileName().toString()).toList();
        }
        assertEquals(1, backupsCreated.size());
        assertTrue(backupsCreated.getFirst().startsWith("quests-backup-default-"));
    }

    @Test
    void policyAwareShutdownAndDatabaseBackupsReadCoreSettings() throws Exception {
        final ConfigurationManager settings = new ConfigurationManager();
        settings.loadFrom(YamlConfig.fromMap(Map.of(
                "storage",
                Map.of(
                        "backups", Map.of("create-when-server-shuts-down", true),
                        "database", Map.of("enabled", false)))));
        final TestHooks hooks = new TestHooks();
        final Path database = dataFolder.resolve("database_sqlite.db");
        Files.createDirectories(dataFolder.resolve("default"));
        Files.writeString(dataFolder.resolve("default").resolve("quests.yml"), "default quests");
        Files.writeString(database, "sqlite bytes");
        final BackupManager backups = new BackupManager(
                dataFolder,
                hooks,
                settings,
                () -> false,
                () -> List.of("default"),
                () -> database);

        backups.backupQuestConfigsOnShutdown();
        backups.backupDatabase();

        final List<String> backupsCreated;
        try (var files = Files.list(dataFolder.resolve("backups"))) {
            backupsCreated = files.map(path -> path.getFileName().toString()).sorted().toList();
        }

        assertTrue(backupsCreated.stream().anyMatch(name -> name.startsWith("quests-backup-default-")));
        assertTrue(backupsCreated.stream().anyMatch(name -> name.startsWith("database_sqlite-backup-")));
    }

    @Test
    void backsUpSqliteDatabaseButSkipsMysql() throws Exception {
        final TestHooks hooks = new TestHooks();
        final BackupManager backups = new BackupManager(dataFolder, hooks);
        final Path database = dataFolder.resolve("database_sqlite.db");
        Files.writeString(database, "sqlite bytes");

        backups.backupDatabase(false, database);
        backups.backupDatabase(true, database);

        final List<Path> databaseBackups;
        try (var files = Files.list(dataFolder.resolve("backups"))) {
            databaseBackups = files
                    .filter(path -> path.getFileName().toString().startsWith("database_sqlite-backup-"))
                    .toList();
        }

        assertEquals(1, databaseBackups.size());
        assertEquals("sqlite bytes", Files.readString(databaseBackups.getFirst()));
        assertTrue(hooks.infoMessages.stream().anyMatch(message -> message.contains("only SQLite databases")));
    }

    @Test
    void migrationBackupCopiesTheFolderButSkipsExistingBackups() throws Exception {
        final TestHooks hooks = new TestHooks();
        final BackupManager backups = new BackupManager(dataFolder, hooks);
        Files.writeString(dataFolder.resolve("config.yml"), "config");
        Files.createDirectories(dataFolder.resolve("backups"));
        Files.writeString(dataFolder.resolve("backups").resolve("old-backup.yml"), "old");

        backups.backupFullNotQuestsFolderForMigration("7.0.0");

        final Path migrationBackup;
        try (var files = Files.list(dataFolder.resolve("backups"))) {
            migrationBackup = files
                    .filter(Files::isDirectory)
                    .filter(path -> path.getFileName().toString().startsWith("notquests-full-before-migration-7.0.0-"))
                    .findFirst()
                    .orElseThrow();
        }

        assertEquals("config", Files.readString(migrationBackup.resolve("config.yml")));
        assertTrue(Files.notExists(migrationBackup.resolve("backups").resolve("old-backup.yml")));
    }

    private static final class TestHooks implements BackupManager.Hooks {
        private final List<String> infoMessages = new ArrayList<>();
        private final List<String> warnMessages = new ArrayList<>();

        @Override
        public void info(final String message) {
            infoMessages.add(message);
        }

        @Override
        public void warn(final String message) {
            warnMessages.add(message);
        }

        @Override
        public void disableSaving(final String reason) {
            warnMessages.add(reason);
        }
    }
}
