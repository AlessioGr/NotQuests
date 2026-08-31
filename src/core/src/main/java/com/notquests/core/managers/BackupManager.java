package com.notquests.core.managers;

import com.notquests.core.config.CategoryFiles;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/** Coordinates NotQuests backups without depending on a specific platform or config library. */
public final class BackupManager {
    private static final DateTimeFormatter BACKUP_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");

    private final Path dataFolder;
    private final Path backupFolder;
    private final Clock clock;
    private final Hooks hooks;
    private final ConfigurationManager configuration;
    private final Supplier<Boolean> currentlyLoading;
    private final Supplier<Collection<String>> categoryNames;
    private final Supplier<Path> sqliteDatabaseFile;

    public BackupManager(final Path dataFolder, final Hooks hooks) {
        this(dataFolder, hooks, null, () -> false, List::of, () -> null);
    }

    public BackupManager(
            final Path dataFolder,
            final Hooks hooks,
            final ConfigurationManager configuration,
            final Supplier<Boolean> currentlyLoading,
            final Supplier<Collection<String>> categoryNames,
            final Supplier<Path> sqliteDatabaseFile) {
        this.dataFolder = Objects.requireNonNull(dataFolder, "dataFolder");
        this.backupFolder = dataFolder.resolve("backups");
        this.clock = Clock.systemDefaultZone();
        this.hooks = Objects.requireNonNull(hooks, "hooks");
        this.configuration = configuration;
        this.currentlyLoading = currentlyLoading == null ? () -> false : currentlyLoading;
        this.categoryNames = categoryNames == null ? List::of : categoryNames;
        this.sqliteDatabaseFile = sqliteDatabaseFile == null ? () -> null : sqliteDatabaseFile;
    }

    public boolean prepareBackupFolder() {
        final boolean existed = Files.exists(backupFolder);
        try {
            Files.createDirectories(dataFolder);
            Files.createDirectories(backupFolder);
        } catch (final IOException exception) {
            hooks.disableSaving("There was an error creating the NotQuests backup folder.");
            hooks.warn("There was an error creating the NotQuests backup folder.");
            hooks.warn(exception.getMessage());
            return false;
        }
        if (!existed) {
            hooks.info("Backup Folder not found. Creating a new one...");
        }
        return true;
    }

    public void backupQuestConfig(final String categoryName, final QuestConfigBackup questConfig) {
        if (!prepareBackupFolder() || questConfig == null) {
            return;
        }
        final Path backupFile;
        try {
            backupFile = questConfigBackupFile(categoryName);
        } catch (final IOException exception) {
            hooks.warn("There was an error preparing the backup file for your quests.yml.");
            hooks.warn(exception.getMessage());
            return;
        }

        if (Files.exists(backupFile)) {
            return;
        }
        try {
            Files.createFile(backupFile);
        } catch (final IOException exception) {
            hooks.warn("There was an error creating the backup file for your quests.yml.");
            hooks.warn(exception.getMessage());
            return;
        }

        try {
            questConfig.save(backupFile);
            hooks.info("Your quests.yml of category <highlight>"
                    + safeCategoryName(categoryName)
                    + "</highlight> has been successfully backed up to <highlight2>"
                    + backupFile);
        } catch (final Exception exception) {
            hooks.warn("There was an error saving the backup file for your quests.yml.");
            hooks.warn(exception.getMessage());
        }
    }

    public void backupQuestConfigs(
            final Collection<String> categoryNames,
            final Function<String, Path> questConfigPath) {
        if (categoryNames == null || categoryNames.isEmpty() || questConfigPath == null) {
            return;
        }
        for (final String categoryName : categoryNames) {
            final Path questsFile = questConfigPath.apply(safeCategoryName(categoryName));
            if (questsFile == null || !Files.exists(questsFile)) {
                continue;
            }
            backupQuestConfig(
                    categoryName,
                    backupFile -> Files.copy(questsFile, backupFile, StandardCopyOption.REPLACE_EXISTING));
        }
    }

    public void backupQuestConfigs(final Collection<String> categoryNames) {
        backupQuestConfigs(
                categoryNames,
                categoryName -> CategoryFiles.categoryFolder(dataFolder, categoryName)
                        .resolve("quests.yml"));
    }

    public void backupQuestConfigsOnShutdown(final boolean enabled, final boolean currentlyLoading, final Collection<String> categoryNames) {
        if (!enabled) {
            return;
        }
        if (currentlyLoading) {
            hooks.warn("Quest data backup has been skipped, because the plugin is currently loading.");
            return;
        }
        backupQuestConfigs(categoryNames);
    }

    public void backupQuestConfigsOnShutdown() {
        if (configuration == null) {
            backupQuestConfigsOnShutdown(false, false, List.of());
            return;
        }
        backupQuestConfigsOnShutdown(
                configuration.backupQuestsOnShutdown(),
                Boolean.TRUE.equals(currentlyLoading.get()),
                categoryNames.get());
    }

    public void backupFullNotQuestsFolderForMigration(final String targetVersion) {
        try {
            prepareBackupFolder();
            final Path destination = backupFolder.resolve(
                    "notquests-full-before-migration-" + targetVersion + "-" + timestamp());
            copyDirectorySkippingBackups(dataFolder, destination);
            hooks.info("Full NotQuests folder backup for migration <highlight>"
                    + targetVersion
                    + "</highlight> has been saved to <highlight2>"
                    + destination);
        } catch (final IOException exception) {
            hooks.warn("There was an error creating the full NotQuests migration backup.");
            hooks.warn(exception.getMessage());
        }
    }

    public void backupDatabase(final boolean mysqlEnabled, final Path sqliteDatabaseFile) {
        if (!prepareBackupFolder()) {
            return;
        }

        hooks.info("Backing up database...");
        if (mysqlEnabled) {
            hooks.info("Cancelled: only SQLite databases can be backed up as of now, but you are using MySQL. "
                    + "Please backup your MySQL database manually from time to time.");
            return;
        }
        if (sqliteDatabaseFile == null || !Files.exists(sqliteDatabaseFile)) {
            hooks.info("No database to back-up!");
            return;
        }
        try {
            final Path newDatabaseBackupFile =
                    backupFolder.resolve("database_sqlite-backup-" + timestamp() + ".db");
            Files.copy(sqliteDatabaseFile, newDatabaseBackupFile, StandardCopyOption.REPLACE_EXISTING);
            hooks.info("Your sqlite database has been successfully backed up to <highlight2>"
                    + newDatabaseBackupFile);
        } catch (final IOException exception) {
            hooks.warn("There was an error saving the backup file for your sqlite database.");
            hooks.warn(exception.getMessage());
        }
    }

    public void backupDatabase() {
        if (configuration == null) {
            backupDatabase(false, sqliteDatabaseFile.get());
            return;
        }
        backupDatabase(configuration.databaseEnabled(), sqliteDatabaseFile.get());
    }

    private static String safeCategoryName(final String categoryName) {
        return categoryName == null || categoryName.isBlank() ? "default" : categoryName;
    }

    private Path questConfigBackupFile(final String categoryName) throws IOException {
        Files.createDirectories(dataFolder);
        Files.createDirectories(backupFolder);
        return backupFolder.resolve("quests-backup-"
                + safeCategoryName(categoryName)
                + "-"
                + timestamp()
                + ".yml");
    }

    private void copyDirectorySkippingBackups(final Path source, final Path destination)
            throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs)
                    throws IOException {
                if (!dir.equals(source) && isInside(dir, backupFolder)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                Files.createDirectories(destination.resolve(source.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
                    throws IOException {
                if (!isInside(file, backupFolder)) {
                    Files.copy(
                            file,
                            destination.resolve(source.relativize(file)),
                            StandardCopyOption.REPLACE_EXISTING);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private String timestamp() {
        return LocalDateTime.now(clock).format(BACKUP_TIMESTAMP);
    }

    private static boolean isInside(final Path path, final Path folder) {
        return path.toAbsolutePath().normalize().startsWith(folder.toAbsolutePath().normalize());
    }

    public interface Hooks {
        void info(String message);

        void warn(String message);

        void disableSaving(String reason);
    }

    @FunctionalInterface
    public interface QuestConfigBackup {
        void save(Path backupFile) throws Exception;
    }
}
