package com.notquests.core.managers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import com.notquests.core.managers.tags.TagManager;
import com.notquests.core.managers.tags.TagType;
import com.notquests.core.structs.QuestPlayer.CompletedQuest;
import com.notquests.core.structs.QuestPlayer.FailedQuest;
import com.notquests.core.structs.QuestPlayer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class PlayerDatabase {
    private final Path dataFolder;
    private final Logger logger;
    private HikariDataSource dataSource;

    public PlayerDatabase(final Path dataFolder, final Logger logger) {
        this.dataFolder = Objects.requireNonNull(dataFolder, "dataFolder");
        this.logger = logger == null ? Logger.NO_OP : logger;
    }

    public synchronized void open(final ConfigurationManager configuration) {
        if (dataSource != null) {
            return;
        }
        final ConfigurationManager finalSettings = Objects.requireNonNull(configuration, "configuration");
        final HikariConfig config = new HikariConfig();
        if (!finalSettings.databaseEnabled()) {
            final Path sqliteFile = dataFolder.resolve("database_sqlite.db");
            ensureSqliteFile(sqliteFile);
            config.setJdbcUrl("jdbc:sqlite:" + sqliteFile);
            config.setConnectionInitSql("PRAGMA journal_mode=WAL; PRAGMA busy_timeout=30000");
            config.setMaximumPoolSize(20);
            config.setConnectionTimeout(30000);
        } else {
            config.setJdbcUrl("jdbc:mysql://"
                    + finalSettings.databaseHost()
                    + ":"
                    + finalSettings.databasePort()
                    + "/"
                    + finalSettings.databaseName());
            config.setUsername(finalSettings.databaseUsername());
            config.setPassword(finalSettings.databasePassword());
            config.setMaximumPoolSize(20);
        }
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        dataSource = new HikariDataSource(config);
    }

    public Connection connection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("Database connection has not been opened yet.");
        }
        return dataSource.getConnection();
    }

    public synchronized void close() {
        logger.info("Closing database connection...");
        if (dataSource == null) {
            logger.warn("Skipped closing database connection, because the data source is null. Was there a previous error which needs to be fixed? Check your console logs!");
            return;
        }
        if (dataSource.isClosed()) {
            logger.info("Skipped closing database connection: connection is already closed.");
            return;
        }
        try {
            dataSource.close();
        } catch (final RuntimeException exception) {
            logger.warn("Error closing database connection: " + exception.getMessage());
        }
    }

    private void ensureSqliteFile(final Path sqliteFile) {
        try {
            Files.createDirectories(sqliteFile.getParent());
            if (!Files.exists(sqliteFile)) {
                Files.createFile(sqliteFile);
            }
        } catch (final IOException exception) {
            logger.warn("File write error: database_sqlite.db - " + exception.getMessage());
        }
    }

    public static final String CREATE_QUEST_PLAYER_PROFILE_DATA = """
                CREATE TABLE IF NOT EXISTS `QuestPlayerProfileData` (`PlayerUUID` varchar(200), `CurrentProfile` varchar(200), PRIMARY KEY (PlayerUUID))
             """;

    public static final String CREATE_QUEST_PLAYER_DATA = """
                CREATE TABLE IF NOT EXISTS `QuestPlayerData` (`PlayerUUID` varchar(200), `QuestPoints` BIGINT(255), `Profile` varchar(200))
             """;

    public static final String CREATE_ACTIVE_QUESTS = """
                CREATE TABLE IF NOT EXISTS `ActiveQuests` (`QuestName` varchar(200), `PlayerUUID` varchar(200), `Profile` varchar(200))
             """;

    public static final String CREATE_COMPLETED_QUESTS = """
                CREATE TABLE IF NOT EXISTS `CompletedQuests` (`QuestName` varchar(200), `PlayerUUID` varchar(200), `TimeCompleted` BIGINT(255), `Profile` varchar(200))
             """;

    public static final String CREATE_FAILED_QUESTS = """
                CREATE TABLE IF NOT EXISTS `FailedQuests` (`QuestName` varchar(200), `PlayerUUID` varchar(200), `TimeFailed` BIGINT(255), `Profile` varchar(200))
             """;

    public static final String CREATE_ACTIVE_TRIGGERS = """
                CREATE TABLE IF NOT EXISTS `ActiveTriggers` (`TriggerType` varchar(200), `QuestName` varchar(200), `PlayerUUID` varchar(200), `CurrentProgress` BIGINT(255), `TriggerID` INT(255), `Profile` varchar(200))
             """;

    public static final String CREATE_TAGS = """
                CREATE TABLE IF NOT EXISTS `Tags` (`PlayerUUID` varchar(200), `TagIdentifier` varchar(200), `TagValue` varchar(200), `TagType` varchar(200), `Profile` varchar(200) )
             """;

    public static final String CREATE_ACTIVE_OBJECTIVES = """
                CREATE TABLE IF NOT EXISTS `ActiveObjectives` (`ObjectiveType` varchar(200), `QuestName` varchar(200), `PlayerUUID` varchar(200), `CurrentProgress` DOUBLE, `ObjectiveID` INT(255), `HasBeenCompleted` BOOLEAN, `ProgressNeeded` DOUBLE, `Profile` varchar(200))
            """;

    public static final List<String> TABLE_CREATION_STATEMENTS = List.of(
            CREATE_QUEST_PLAYER_PROFILE_DATA,
            CREATE_QUEST_PLAYER_DATA,
            CREATE_ACTIVE_QUESTS,
            CREATE_FAILED_QUESTS,
            CREATE_COMPLETED_QUESTS,
            CREATE_ACTIVE_OBJECTIVES,
            CREATE_ACTIVE_TRIGGERS,
            CREATE_TAGS);

    public static final String SELECT_QUEST_PLAYER_DATA_FOR_PLAYER = """
            SELECT * FROM QuestPlayerData WHERE PlayerUUID = ?;
          """;

    public static final String SELECT_ALL_QUEST_PLAYER_DATA = """
            SELECT * FROM QuestPlayerData;
          """;

    public static final String SELECT_QUEST_PLAYER_PROFILE_DATA = """
            SELECT * FROM QuestPlayerProfileData WHERE PlayerUUID = ?;
          """;

    public static final String SELECT_COMPLETED_QUESTS = """
            SELECT QuestName, TimeCompleted FROM CompletedQuests
            WHERE PlayerUUID = ? AND Profile = ?;
          """;

    public static final String SELECT_FAILED_QUESTS = """
            SELECT QuestName, TimeFailed FROM FailedQuests
            WHERE PlayerUUID = ? AND Profile = ?;
          """;

    public static final String SELECT_ACTIVE_QUESTS = """
            SELECT QuestName FROM ActiveQuests
            WHERE PlayerUUID = ? AND Profile = ?;
          """;

    public static final String SELECT_ACTIVE_TRIGGERS = """
            SELECT * FROM ActiveTriggers
            WHERE PlayerUUID = ? AND Profile = ? AND QuestName = ?;
          """;

    public static final String SELECT_ACTIVE_OBJECTIVES = """
            SELECT * FROM ActiveObjectives
            WHERE PlayerUUID = ? AND Profile = ? AND QuestName = ?;
          """;

    public static final String SELECT_ACTIVE_OBJECTIVES_FOR_PROFILE = """
            SELECT * FROM ActiveObjectives
            WHERE PlayerUUID = ? AND Profile = ?;
          """;

    public static final String DELETE_QUEST_PLAYER_PROFILE_DATA = """
            DELETE FROM QuestPlayerProfileData WHERE PlayerUUID = ?;
         """;

    public static final String INSERT_QUEST_PLAYER_PROFILE_DATA = """
            INSERT INTO QuestPlayerProfileData (PlayerUUID, CurrentProfile) VALUES (?, ?);
          """;

    public static final String DELETE_QUEST_PLAYER_DATA = """
            DELETE FROM QuestPlayerData WHERE PlayerUUID = ? AND Profile = ?;
         """;

    public static final String INSERT_QUEST_PLAYER_DATA = """
            INSERT INTO QuestPlayerData (PlayerUUID, QuestPoints, Profile) VALUES (?, ?, ?);
         """;

    public static final String DELETE_ACTIVE_QUESTS = """
            DELETE FROM ActiveQuests WHERE PlayerUUID = ? AND Profile = ?;
         """;

    public static final String INSERT_ACTIVE_QUEST = """
            INSERT INTO ActiveQuests (QuestName, PlayerUUID, Profile) VALUES (?, ?, ?);
         """;

    public static final String DELETE_ACTIVE_OBJECTIVES = """
            DELETE FROM ActiveObjectives WHERE PlayerUUID = ? AND Profile = ?;
         """;

    public static final String INSERT_ACTIVE_OBJECTIVE = """
            INSERT INTO ActiveObjectives (ObjectiveType, QuestName, PlayerUUID, CurrentProgress, ObjectiveID, HasBeenCompleted, ProgressNeeded, Profile) VALUES (?, ?, ?, ?, ?, ?, ?, ?);
         """;

    public static final String INSERT_ACTIVE_TRIGGER = """
            INSERT INTO ActiveTriggers (TriggerType, QuestName, PlayerUUID, CurrentProgress, TriggerID, Profile) VALUES (?, ?, ?, ?, ?, ?);
         """;

    public static final String DELETE_COMPLETED_QUESTS = """
            DELETE FROM CompletedQuests WHERE PlayerUUID = ? AND Profile = ?;
         """;

    public static final String INSERT_COMPLETED_QUEST = """
            INSERT INTO CompletedQuests (QuestName, PlayerUUID, TimeCompleted, Profile) VALUES (?, ?, ?, ?);
         """;

    public static final String DELETE_FAILED_QUESTS = """
            DELETE FROM FailedQuests WHERE PlayerUUID = ? AND Profile = ?;
         """;

    public static final String INSERT_FAILED_QUEST = """
            INSERT INTO FailedQuests (QuestName, PlayerUUID, TimeFailed, Profile) VALUES (?, ?, ?, ?);
         """;

    public static final String SELECT_TAGS = """
            SELECT TagIdentifier, TagValue, TagType FROM Tags
            WHERE PlayerUUID = ? AND Profile = ?;
         """;

    public static final String DELETE_TAGS = """
            DELETE FROM Tags WHERE PlayerUUID = ? AND Profile = ?;
         """;

    public static final String INSERT_TAG = """
            INSERT INTO Tags (PlayerUUID, TagIdentifier, TagValue, TagType, Profile) VALUES (?, ?, ?, ?, ?);
         """;

    public static void prepare(
            final Connection connection,
            final Logger logger) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            createTable(statement, logger, "QuestPlayerProfileData", CREATE_QUEST_PLAYER_PROFILE_DATA);
            createTable(statement, logger, "QuestPlayerData", CREATE_QUEST_PLAYER_DATA);
            createTable(statement, logger, "ActiveQuests", CREATE_ACTIVE_QUESTS);
            createTable(statement, logger, "FailedQuests", CREATE_FAILED_QUESTS);
            createTable(statement, logger, "CompletedQuests", CREATE_COMPLETED_QUESTS);
            createTable(statement, logger, "ActiveObjectives", CREATE_ACTIVE_OBJECTIVES);
            createTable(statement, logger, "ActiveTriggers", CREATE_ACTIVE_TRIGGERS);
            createTable(statement, logger, "Tags", CREATE_TAGS);
        }
    }

    private static void createTable(
            final Statement statement,
            final Logger logger,
            final String tableName,
            final String sql) throws SQLException {
        log(logger, "Creating database table '" + tableName + "' if it doesn't exist yet...");
        statement.executeUpdate(sql);
    }

    private static void log(final Logger logger, final String message) {
        if (logger != null) {
            logger.info(message);
        }
    }

    public record QuestPlayerDataRow(
            UUID playerUuid,
            long questPoints,
            String profile) {
        public static QuestPlayerDataRow read(final ResultSet resultSet, final UUID knownPlayerUuid)
                throws SQLException {
            final UUID uuid = knownPlayerUuid == null
                    ? UUID.fromString(resultSet.getString("PlayerUUID"))
                    : knownPlayerUuid;
            return new QuestPlayerDataRow(
                    uuid,
                    resultSet.getLong("QuestPoints"),
                    profileOrDefault(resultSet));
        }
    }

    public record QuestPlayerProfileRow(String currentProfile) {
        public static QuestPlayerProfileRow read(final ResultSet resultSet) throws SQLException {
            final String currentProfile = resultSet.getString("CurrentProfile");
            return new QuestPlayerProfileRow(
                    currentProfile == null || currentProfile.isBlank() ? "default" : currentProfile);
        }
    }

    public record ActiveQuestRow(String questName) {
        public static ActiveQuestRow read(final ResultSet resultSet) throws SQLException {
            return new ActiveQuestRow(resultSet.getString("QuestName"));
        }
    }

    public record QuestHistoryReadRow(String questName, long timestamp) {
        public static QuestHistoryReadRow completed(final ResultSet resultSet) throws SQLException {
            return new QuestHistoryReadRow(
                    resultSet.getString("QuestName"),
                    resultSet.getLong("TimeCompleted"));
        }

        public static QuestHistoryReadRow failed(final ResultSet resultSet) throws SQLException {
            return new QuestHistoryReadRow(
                    resultSet.getString("QuestName"),
                    resultSet.getLong("TimeFailed"));
        }
    }

    public record ActiveTriggerReadRow(String triggerType, long currentProgress, int triggerId) {
        public static ActiveTriggerReadRow read(final ResultSet resultSet) throws SQLException {
            return new ActiveTriggerReadRow(
                    resultSet.getString("TriggerType"),
                    resultSet.getLong("CurrentProgress"),
                    resultSet.getInt("TriggerID"));
        }
    }

    public record ActiveObjectiveReadRow(
            String objectiveType,
            String holderPath,
            double currentProgress,
            int objectiveId,
            boolean completed,
            double progressNeeded,
            boolean progressNeededNull) {
        public static ActiveObjectiveReadRow read(final ResultSet resultSet) throws SQLException {
            final String objectiveType = resultSet.getString("ObjectiveType");
            final String holderPath = resultSet.getString("QuestName");
            final double currentProgress = resultSet.getDouble("CurrentProgress");
            final int objectiveId = resultSet.getInt("ObjectiveID");
            final boolean completed = resultSet.getBoolean("HasBeenCompleted");
            final double progressNeeded = resultSet.getDouble("ProgressNeeded");
            final boolean progressNeededNull = resultSet.wasNull();
            return new ActiveObjectiveReadRow(
                    objectiveType,
                    holderPath,
                    currentProgress,
                    objectiveId,
                    completed,
                    progressNeeded,
                    progressNeededNull);
        }
    }

    public record ActiveTriggerRow(
            String triggerType,
            String questName,
            String playerUuid,
            long currentProgress,
            int triggerId,
            String profile) {
        public void bind(final PreparedStatement statement) throws SQLException {
            statement.setString(1, triggerType);
            statement.setString(2, questName);
            statement.setString(3, playerUuid);
            statement.setLong(4, currentProgress);
            statement.setInt(5, triggerId);
            statement.setString(6, profile);
        }
    }

    public record ActiveObjectiveRow(
            String objectiveType,
            String holderPath,
            String playerUuid,
            double currentProgress,
            int objectiveId,
            boolean completed,
            double progressNeeded,
            String profile) {
        public void bind(final PreparedStatement statement) throws SQLException {
            statement.setString(1, objectiveType);
            statement.setString(2, holderPath);
            statement.setString(3, playerUuid);
            statement.setDouble(4, currentProgress);
            statement.setInt(5, objectiveId);
            statement.setBoolean(6, completed);
            statement.setDouble(7, progressNeeded);
            statement.setString(8, profile);
        }
    }

    public record QuestHistoryRow(
            String questName,
            String playerUuid,
            long timestamp,
            String profile) {
        public void bind(final PreparedStatement statement) throws SQLException {
            statement.setString(1, questName);
            statement.setString(2, playerUuid);
            statement.setLong(3, timestamp);
            statement.setString(4, profile);
        }
    }

    private static String profileOrDefault(final ResultSet resultSet) throws SQLException {
        if (!hasColumn(resultSet, "Profile")) {
            return "default";
        }
        final String profile = resultSet.getString("Profile");
        return profile == null || profile.isBlank() ? "default" : profile;
    }

    private static boolean hasColumn(final ResultSet resultSet, final String column) {
        try {
            resultSet.findColumn(column);
            return true;
        } catch (final SQLException ignored) {
            return false;
        }
    }

    public record PlayerSnapshot(
            String playerIdentifier,
            String profile,
            String activeProfile,
            long questPoints,
            List<String> activeQuestNames,
            List<ActiveTriggerRow> activeTriggers,
            List<ActiveObjectiveRow> activeObjectives,
            List<QuestHistoryRow> completedQuests,
            List<QuestHistoryRow> failedQuests,
            Map<String, Object> tags) {
        public PlayerSnapshot(
                final String playerIdentifier,
                final String profile,
                final String activeProfile,
                final long questPoints,
                final List<String> activeQuestNames,
                final List<ActiveTriggerRow> activeTriggers,
                final List<ActiveObjectiveRow> activeObjectives,
                final List<QuestHistoryRow> completedQuests,
                final List<QuestHistoryRow> failedQuests) {
            this(
                    playerIdentifier,
                    profile,
                    activeProfile,
                    questPoints,
                    activeQuestNames,
                    activeTriggers,
                    activeObjectives,
                    completedQuests,
                    failedQuests,
                    Map.of());
        }

        public PlayerSnapshot {
            activeQuestNames = List.copyOf(activeQuestNames == null ? List.of() : activeQuestNames);
            activeTriggers = List.copyOf(activeTriggers == null ? List.of() : activeTriggers);
            activeObjectives = List.copyOf(activeObjectives == null ? List.of() : activeObjectives);
            completedQuests = List.copyOf(completedQuests == null ? List.of() : completedQuests);
            failedQuests = List.copyOf(failedQuests == null ? List.of() : failedQuests);
            tags = copyTags(tags);
            profile = profile == null || profile.isBlank() ? "default" : profile;
            activeProfile = activeProfile == null || activeProfile.isBlank() ? "default" : activeProfile;
        }
    }

    public static void save(final Connection connection, final List<PlayerSnapshot> players)
            throws SQLException {
        final boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try (
                PreparedStatement deleteProfile =
                        connection.prepareStatement(DELETE_QUEST_PLAYER_PROFILE_DATA);
                PreparedStatement insertProfile =
                        connection.prepareStatement(INSERT_QUEST_PLAYER_PROFILE_DATA);
                PreparedStatement deletePlayer =
                        connection.prepareStatement(DELETE_QUEST_PLAYER_DATA);
                PreparedStatement insertPlayer =
                        connection.prepareStatement(INSERT_QUEST_PLAYER_DATA);
                PreparedStatement deleteActiveQuests =
                        connection.prepareStatement(DELETE_ACTIVE_QUESTS);
                PreparedStatement deleteActiveObjectives =
                        connection.prepareStatement(DELETE_ACTIVE_OBJECTIVES);
                PreparedStatement insertActiveQuest =
                        connection.prepareStatement(INSERT_ACTIVE_QUEST);
                PreparedStatement insertActiveTrigger =
                        connection.prepareStatement(INSERT_ACTIVE_TRIGGER);
                PreparedStatement insertActiveObjective =
                        connection.prepareStatement(INSERT_ACTIVE_OBJECTIVE);
                PreparedStatement deleteCompleted =
                        connection.prepareStatement(DELETE_COMPLETED_QUESTS);
                PreparedStatement insertCompleted =
                        connection.prepareStatement(INSERT_COMPLETED_QUEST);
                PreparedStatement deleteFailed =
                        connection.prepareStatement(DELETE_FAILED_QUESTS);
                PreparedStatement insertFailed =
                        connection.prepareStatement(INSERT_FAILED_QUEST);
                PreparedStatement deleteTags =
                        connection.prepareStatement(DELETE_TAGS);
                PreparedStatement insertTag =
                        connection.prepareStatement(INSERT_TAG)) {
            for (final PlayerSnapshot player : players) {
                savePlayer(
                        player,
                        deleteProfile,
                        insertProfile,
                        deletePlayer,
                        insertPlayer,
                        deleteActiveQuests,
                        deleteActiveObjectives,
                        insertActiveQuest,
                        insertActiveTrigger,
                        insertActiveObjective,
                        deleteCompleted,
                        insertCompleted,
                        deleteFailed,
                        insertFailed,
                        deleteTags,
                        insertTag);
            }
            connection.commit();
        } catch (final Exception exception) {
            connection.rollback();
            if (exception instanceof SQLException sqlException) {
                throw sqlException;
            }
            throw new SQLException(exception);
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    private static void savePlayer(
            final PlayerSnapshot player,
            final PreparedStatement deleteProfile,
            final PreparedStatement insertProfile,
            final PreparedStatement deletePlayer,
            final PreparedStatement insertPlayer,
            final PreparedStatement deleteActiveQuests,
            final PreparedStatement deleteActiveObjectives,
            final PreparedStatement insertActiveQuest,
            final PreparedStatement insertActiveTrigger,
            final PreparedStatement insertActiveObjective,
            final PreparedStatement deleteCompleted,
            final PreparedStatement insertCompleted,
            final PreparedStatement deleteFailed,
            final PreparedStatement insertFailed,
            final PreparedStatement deleteTags,
            final PreparedStatement insertTag) throws SQLException {
        deleteProfile.setString(1, player.playerIdentifier());
        deleteProfile.executeUpdate();

        insertProfile.setString(1, player.playerIdentifier());
        insertProfile.setString(2, player.activeProfile());
        insertProfile.executeUpdate();

        deletePlayer.setString(1, player.playerIdentifier());
        deletePlayer.setString(2, player.profile());
        deletePlayer.executeUpdate();

        insertPlayer.setString(1, player.playerIdentifier());
        insertPlayer.setLong(2, player.questPoints());
        insertPlayer.setString(3, player.profile());
        insertPlayer.executeUpdate();

        deleteActiveQuests.setString(1, player.playerIdentifier());
        deleteActiveQuests.setString(2, player.profile());
        deleteActiveQuests.executeUpdate();

        deleteActiveObjectives.setString(1, player.playerIdentifier());
        deleteActiveObjectives.setString(2, player.profile());
        deleteActiveObjectives.executeUpdate();

        for (final String activeQuestName : player.activeQuestNames()) {
            insertActiveQuest.setString(1, activeQuestName);
            insertActiveQuest.setString(2, player.playerIdentifier());
            insertActiveQuest.setString(3, player.profile());
            insertActiveQuest.executeUpdate();
        }

        for (final ActiveTriggerRow activeTrigger : player.activeTriggers()) {
            activeTrigger.bind(insertActiveTrigger);
            insertActiveTrigger.executeUpdate();
        }

        for (final ActiveObjectiveRow activeObjective : player.activeObjectives()) {
            activeObjective.bind(insertActiveObjective);
            insertActiveObjective.executeUpdate();
        }

        deleteCompleted.setString(1, player.playerIdentifier());
        deleteCompleted.setString(2, player.profile());
        deleteCompleted.executeUpdate();

        for (final QuestHistoryRow completedQuest : player.completedQuests()) {
            completedQuest.bind(insertCompleted);
            insertCompleted.executeUpdate();
        }

        deleteFailed.setString(1, player.playerIdentifier());
        deleteFailed.setString(2, player.profile());
        deleteFailed.executeUpdate();

        for (final QuestHistoryRow failedQuest : player.failedQuests()) {
            failedQuest.bind(insertFailed);
            insertFailed.executeUpdate();
        }

        deleteTags.setString(1, player.playerIdentifier());
        deleteTags.setString(2, player.profile());
        deleteTags.executeUpdate();

        for (final Map.Entry<String, Object> tag : player.tags().entrySet()) {
            final var encoded = TagManager.encode(tag.getValue());
            if (encoded.isEmpty()) {
                continue;
            }
            insertTag.setString(1, player.playerIdentifier());
            insertTag.setString(2, tag.getKey());
            insertTag.setString(3, encoded.get().value());
            insertTag.setString(4, encoded.get().type().name());
            insertTag.setString(5, player.profile());
            insertTag.executeUpdate();
        }
    }

    public record LoadedPlayer(
            String playerIdentifier,
            String profile,
            String activeProfile,
            long questPoints,
            List<QuestHistoryReadRow> completedQuests,
            List<QuestHistoryReadRow> failedQuests,
            List<String> activeQuestNames,
            Map<String, List<ActiveTriggerReadRow>> activeTriggersByQuest,
            Map<String, List<ActiveObjectiveReadRow>> activeObjectivesByHolderPath,
            Map<String, Object> tags) {
        public LoadedPlayer(
                final String playerIdentifier,
                final String profile,
                final String activeProfile,
                final long questPoints,
                final List<QuestHistoryReadRow> completedQuests,
                final List<QuestHistoryReadRow> failedQuests,
                final List<String> activeQuestNames,
                final Map<String, List<ActiveTriggerReadRow>> activeTriggersByQuest,
                final Map<String, List<ActiveObjectiveReadRow>> activeObjectivesByHolderPath) {
            this(
                    playerIdentifier,
                    profile,
                    activeProfile,
                    questPoints,
                    completedQuests,
                    failedQuests,
                    activeQuestNames,
                    activeTriggersByQuest,
                    activeObjectivesByHolderPath,
                    Map.of());
        }

        public LoadedPlayer {
            profile = profile == null || profile.isBlank() ? "default" : profile;
            activeProfile = activeProfile == null || activeProfile.isBlank() ? "default" : activeProfile;
            completedQuests = List.copyOf(completedQuests == null ? List.of() : completedQuests);
            failedQuests = List.copyOf(failedQuests == null ? List.of() : failedQuests);
            activeQuestNames = List.copyOf(activeQuestNames == null ? List.of() : activeQuestNames);
            activeTriggersByQuest = copyMap(activeTriggersByQuest);
            activeObjectivesByHolderPath = copyMap(activeObjectivesByHolderPath);
            tags = copyTags(tags);
        }

        public List<ActiveTriggerReadRow> activeTriggers(final String questName) {
            return activeTriggersByQuest.getOrDefault(questName, List.of());
        }

        public List<ActiveObjectiveReadRow> activeObjectives(final String holderPath) {
            return activeObjectivesByHolderPath.getOrDefault(holderPath, List.of());
        }
    }

    public static List<LoadedPlayer> load(final Connection connection, final String playerIdentifier)
            throws SQLException {
        try (
                PreparedStatement playerDataForPlayer =
                        connection.prepareStatement(SELECT_QUEST_PLAYER_DATA_FOR_PLAYER);
                PreparedStatement allPlayerData =
                        connection.prepareStatement(SELECT_ALL_QUEST_PLAYER_DATA);
                PreparedStatement profileData =
                        connection.prepareStatement(SELECT_QUEST_PLAYER_PROFILE_DATA);
                PreparedStatement completedQuests =
                        connection.prepareStatement(SELECT_COMPLETED_QUESTS);
                PreparedStatement failedQuests =
                        connection.prepareStatement(SELECT_FAILED_QUESTS);
                PreparedStatement activeQuests =
                        connection.prepareStatement(SELECT_ACTIVE_QUESTS);
                PreparedStatement activeTriggers =
                        connection.prepareStatement(SELECT_ACTIVE_TRIGGERS);
                PreparedStatement activeObjectives =
                        connection.prepareStatement(SELECT_ACTIVE_OBJECTIVES_FOR_PROFILE);
                PreparedStatement tags =
                        connection.prepareStatement(SELECT_TAGS)) {
            final ArrayList<LoadedPlayer> players = new ArrayList<>();
            ResultSet playerRows = null;
            try {
                if (playerIdentifier == null || playerIdentifier.isBlank()) {
                    playerRows = allPlayerData.executeQuery();
                } else {
                    playerDataForPlayer.setString(1, playerIdentifier);
                    playerRows = playerDataForPlayer.executeQuery();
                }
                while (playerRows.next()) {
                    final QuestPlayerDataRow playerData =
                            QuestPlayerDataRow.read(
                                    playerRows,
                                    playerIdentifier == null || playerIdentifier.isBlank()
                                            ? null
                                            : UUID.fromString(playerIdentifier));
                    players.add(loadPlayer(
                            playerData,
                            profileData,
                            completedQuests,
                            failedQuests,
                            activeQuests,
                            activeTriggers,
                            activeObjectives,
                            tags));
                }
            } finally {
                if (playerRows != null) {
                    playerRows.close();
                }
            }
            return List.copyOf(players);
        }
    }

    private static LoadedPlayer loadPlayer(
            final QuestPlayerDataRow playerData,
            final PreparedStatement profileData,
            final PreparedStatement completedQuests,
            final PreparedStatement failedQuests,
            final PreparedStatement activeQuests,
            final PreparedStatement activeTriggers,
            final PreparedStatement activeObjectives,
            final PreparedStatement tags) throws SQLException {
        final String playerIdentifier = playerData.playerUuid().toString();
        final String profile = playerData.profile();
        bindPlayerProfile(completedQuests, playerIdentifier, profile);
        bindPlayerProfile(failedQuests, playerIdentifier, profile);
        bindPlayerProfile(activeQuests, playerIdentifier, profile);
        bindPlayerProfile(activeObjectives, playerIdentifier, profile);
        bindPlayerProfile(tags, playerIdentifier, profile);

        final List<String> activeQuestNames = activeQuestNames(activeQuests);
        final Map<String, List<ActiveTriggerReadRow>> triggersByQuest =
                activeTriggersByQuest(activeTriggers, playerIdentifier, profile, activeQuestNames);
        return new LoadedPlayer(
                playerIdentifier,
                profile,
                activeProfile(profileData, playerIdentifier),
                playerData.questPoints(),
                questHistory(completedQuests, true),
                questHistory(failedQuests, false),
                activeQuestNames,
                triggersByQuest,
                activeObjectivesByHolderPath(activeObjectives),
                tags(tags));
    }

    private static String activeProfile(final PreparedStatement profileData, final String playerIdentifier)
            throws SQLException {
        profileData.setString(1, playerIdentifier);
        try (ResultSet resultSet = profileData.executeQuery()) {
            if (resultSet.next()) {
                return QuestPlayerProfileRow.read(resultSet).currentProfile();
            }
            return "default";
        }
    }

    private static List<QuestHistoryReadRow> questHistory(
            final PreparedStatement statement,
            final boolean completed) throws SQLException {
        final ArrayList<QuestHistoryReadRow> rows = new ArrayList<>();
        try (ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                rows.add(completed
                        ? QuestHistoryReadRow.completed(resultSet)
                        : QuestHistoryReadRow.failed(resultSet));
            }
        }
        return rows;
    }

    private static List<String> activeQuestNames(final PreparedStatement statement) throws SQLException {
        final ArrayList<String> names = new ArrayList<>();
        try (ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                names.add(ActiveQuestRow.read(resultSet).questName());
            }
        }
        return names;
    }

    private static Map<String, List<ActiveTriggerReadRow>> activeTriggersByQuest(
            final PreparedStatement statement,
            final String playerIdentifier,
            final String profile,
            final List<String> activeQuestNames) throws SQLException {
        final LinkedHashMap<String, List<ActiveTriggerReadRow>> rows = new LinkedHashMap<>();
        for (final String activeQuestName : activeQuestNames) {
            statement.setString(1, playerIdentifier);
            statement.setString(2, profile);
            statement.setString(3, activeQuestName);
            final ArrayList<ActiveTriggerReadRow> triggers = new ArrayList<>();
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    triggers.add(ActiveTriggerReadRow.read(resultSet));
                }
            }
            rows.put(activeQuestName, triggers);
        }
        return rows;
    }

    private static Map<String, List<ActiveObjectiveReadRow>> activeObjectivesByHolderPath(
            final PreparedStatement statement) throws SQLException {
        final LinkedHashMap<String, List<ActiveObjectiveReadRow>> rows = new LinkedHashMap<>();
        try (ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                final ActiveObjectiveReadRow row =
                        ActiveObjectiveReadRow.read(resultSet);
                rows.computeIfAbsent(row.holderPath(), ignored -> new ArrayList<>()).add(row);
            }
        }
        rows.replaceAll((ignored, value) -> List.copyOf(value));
        return rows;
    }

    private static Map<String, Object> tags(final PreparedStatement statement) throws SQLException {
        final LinkedHashMap<String, Object> tags = new LinkedHashMap<>();
        try (ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                final String value = resultSet.getString("TagValue");
                if (value == null) {
                    continue;
                }
                final TagType type;
                try {
                    type = TagType.valueOf(resultSet.getString("TagType"));
                } catch (final RuntimeException ignored) {
                    continue;
                }
                final String identifier = resultSet.getString("TagIdentifier");
                if (identifier != null && !identifier.isBlank()) {
                    try {
                        tags.put(identifier.toLowerCase(Locale.ROOT), TagManager.parse(type, value));
                    } catch (final NumberFormatException ignored) {
                        // Skip only the malformed tag. Other tags and player data remain loadable.
                    }
                }
            }
        }
        return tags;
    }

    private static void bindPlayerProfile(
            final PreparedStatement statement,
            final String playerIdentifier,
            final String profile) throws SQLException {
        statement.setString(1, playerIdentifier);
        statement.setString(2, profile);
    }

    private static <T> Map<String, List<T>> copyMap(final Map<String, List<T>> map) {
        if (map == null || map.isEmpty()) {
            return Map.of();
        }
        final LinkedHashMap<String, List<T>> copy = new LinkedHashMap<>();
        map.forEach((key, value) -> copy.put(key, List.copyOf(value == null ? List.of() : value)));
        return Map.copyOf(copy);
    }

    private static Map<String, Object> copyTags(final Map<String, Object> tags) {
        if (tags == null || tags.isEmpty()) {
            return Map.of();
        }
        final LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
        tags.forEach((key, value) -> {
            if (key != null && !key.isBlank() && value != null) {
                copy.put(key.toLowerCase(Locale.ROOT), value);
            }
        });
        return Map.copyOf(copy);
    }

    public static List<PlayerSnapshot> snapshots(final List<? extends StoredPlayer> players) {
        final ArrayList<PlayerSnapshot> snapshots = new ArrayList<>();
        for (final StoredPlayer player : players == null ? List.<StoredPlayer>of() : players) {
            if (player != null) {
                snapshots.add(snapshot(player));
            }
        }
        return List.copyOf(snapshots);
    }

    public static PlayerSnapshot snapshot(final StoredPlayer player) {
        final ArrayList<String> activeQuestNames = new ArrayList<>();
        final ArrayList<ActiveTriggerRow> activeTriggers = new ArrayList<>();
        final ArrayList<ActiveObjectiveRow> activeObjectives = new ArrayList<>();
        final ArrayList<QuestHistoryRow> completedQuests = new ArrayList<>();
        final ArrayList<QuestHistoryRow> failedQuests = new ArrayList<>();

        for (final StoredActiveQuest activeQuest : player.activeQuests()) {
            activeQuestNames.add(activeQuest.questIdentifier());
            for (final StoredActiveTrigger activeTrigger : activeQuest.activeTriggers()) {
                activeTriggers.add(new ActiveTriggerRow(
                        activeTrigger.triggerType(),
                        activeQuest.questIdentifier(),
                        player.playerIdentifier(),
                        activeTrigger.currentProgress(),
                        activeTrigger.triggerId(),
                        player.profile()));
            }
            for (final StoredActiveObjective activeObjective : activeQuest.activeObjectives()) {
                collectActiveObjectiveRows(activeObjectives, activeObjective, player);
            }
            for (final StoredActiveObjective completedObjective : activeQuest.completedObjectives()) {
                collectCompletedActiveObjectiveRows(activeObjectives, completedObjective, player);
            }
        }

        for (final CompletedQuest completedQuest : player.completedQuests()) {
            completedQuests.add(new QuestHistoryRow(
                    completedQuest.questIdentifier(),
                    player.playerIdentifier(),
                    completedQuest.timeCompleted(),
                    player.profile()));
        }

        for (final FailedQuest failedQuest : player.failedQuests()) {
            failedQuests.add(new QuestHistoryRow(
                    failedQuest.questIdentifier(),
                    player.playerIdentifier(),
                    failedQuest.timeFailed(),
                    player.profile()));
        }

        return new PlayerSnapshot(
                player.playerIdentifier(),
                player.profile(),
                player.activeProfile(),
                player.questPoints(),
                activeQuestNames,
                activeTriggers,
                activeObjectives,
                completedQuests,
                failedQuests,
                player.tags());
    }

    private static void collectActiveObjectiveRows(
            final List<ActiveObjectiveRow> rows,
            final StoredActiveObjective activeObjective,
            final StoredPlayer player) {
        rows.add(new ActiveObjectiveRow(
                activeObjective.objectiveType(),
                activeObjective.holderPath(),
                player.playerIdentifier(),
                activeObjective.currentProgress(),
                activeObjective.objectiveId(),
                activeObjective.completed(),
                activeObjective.progressNeeded(),
                player.profile()));

        for (final StoredActiveObjective subActiveObjective : activeObjective.activeObjectives()) {
            collectActiveObjectiveRows(rows, subActiveObjective, player);
        }
    }

    private static void collectCompletedActiveObjectiveRows(
            final List<ActiveObjectiveRow> rows,
            final StoredActiveObjective activeObjective,
            final StoredPlayer player) {
        rows.add(new ActiveObjectiveRow(
                activeObjective.objectiveType(),
                activeObjective.holderPath(),
                player.playerIdentifier(),
                activeObjective.currentProgress(),
                activeObjective.objectiveId(),
                activeObjective.completed(),
                activeObjective.progressNeeded(),
                player.profile()));

        for (final StoredActiveObjective subCompletedObjective : activeObjective.completedObjectives()) {
            collectCompletedActiveObjectiveRows(rows, subCompletedObjective, player);
        }
    }

    public interface StoredPlayer {
        String playerIdentifier();

        String profile();

        String activeProfile();

        long questPoints();

        List<? extends StoredActiveQuest> activeQuests();

        List<CompletedQuest> completedQuests();

        List<FailedQuest> failedQuests();

        Map<String, Object> tags();
    }

    public interface StoredActiveQuest {
        String questIdentifier();

        List<? extends StoredActiveTrigger> activeTriggers();

        List<? extends StoredActiveObjective> activeObjectives();

        List<? extends StoredActiveObjective> completedObjectives();
    }

    public interface StoredActiveTrigger {
        String triggerType();

        long currentProgress();

        int triggerId();
    }

    public interface StoredActiveObjective {
        String objectiveType();

        String holderPath();

        double currentProgress();

        int objectiveId();

        boolean completed();

        double progressNeeded();

        List<? extends StoredActiveObjective> activeObjectives();

        List<? extends StoredActiveObjective> completedObjectives();
    }

    public interface Logger {
        Logger NO_OP = new Logger() {
            @Override public void info(final String message) {}

            @Override public void warn(final String message) {}

            @Override public void debug(final Exception exception) {}
        };

        void info(String message);

        void warn(String message);

        void debug(Exception exception);
    }
}
