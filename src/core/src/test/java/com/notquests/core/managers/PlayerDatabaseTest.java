package com.notquests.core.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

class PlayerDatabaseTest {
    private static final String PLAYER_ID = "11111111-1111-1111-1111-111111111111";

    @Test
    void loadsAnUnmodifiedSixThreeDatabase() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            try (var statement = connection.createStatement()) {
                statement.executeUpdate("CREATE TABLE QuestPlayerProfileData (PlayerUUID varchar(200), CurrentProfile varchar(200), PRIMARY KEY (PlayerUUID))");
                statement.executeUpdate("CREATE TABLE QuestPlayerData (PlayerUUID varchar(200), QuestPoints BIGINT, Profile varchar(200))");
                statement.executeUpdate("CREATE TABLE ActiveQuests (QuestName varchar(200), PlayerUUID varchar(200), Profile varchar(200))");
                statement.executeUpdate("CREATE TABLE CompletedQuests (QuestName varchar(200), PlayerUUID varchar(200), TimeCompleted BIGINT, Profile varchar(200))");
                statement.executeUpdate("CREATE TABLE FailedQuests (QuestName varchar(200), PlayerUUID varchar(200), TimeFailed BIGINT, Profile varchar(200))");
                statement.executeUpdate("CREATE TABLE ActiveObjectives (ObjectiveType varchar(200), QuestName varchar(200), PlayerUUID varchar(200), CurrentProgress DOUBLE, ObjectiveID INT, HasBeenCompleted BOOLEAN, ProgressNeeded DOUBLE, Profile varchar(200))");
                statement.executeUpdate("CREATE TABLE ActiveTriggers (TriggerType varchar(200), QuestName varchar(200), PlayerUUID varchar(200), CurrentProgress BIGINT, TriggerID INT, Profile varchar(200))");
                statement.executeUpdate("CREATE TABLE Tags (PlayerUUID varchar(200), TagIdentifier varchar(200), TagValue varchar(200), TagType varchar(200), Profile varchar(200))");

                statement.executeUpdate("INSERT INTO QuestPlayerProfileData VALUES ('" + PLAYER_ID + "', 'story')");
                statement.executeUpdate("INSERT INTO QuestPlayerData VALUES ('" + PLAYER_ID + "', 37, 'story')");
                statement.executeUpdate("INSERT INTO ActiveQuests VALUES ('TheVirus', '" + PLAYER_ID + "', 'story')");
                statement.executeUpdate("INSERT INTO CompletedQuests VALUES ('Introduction', '" + PLAYER_ID + "', 1000, 'story')");
                statement.executeUpdate("INSERT INTO FailedQuests VALUES ('OldQuest', '" + PLAYER_ID + "', 2000, 'story')");
                statement.executeUpdate("INSERT INTO ActiveObjectives VALUES ('BreakBlocks', 'TheVirus', '" + PLAYER_ID + "', 12.5, 1, 0, 64.0, 'story')");
                statement.executeUpdate("INSERT INTO ActiveObjectives VALUES ('ReachLocation', 'TheVirus.1', '" + PLAYER_ID + "', 1.0, 2, 1, 1.0, 'story')");
                statement.executeUpdate("INSERT INTO ActiveTriggers VALUES ('BEGIN', 'TheVirus', '" + PLAYER_ID + "', 2, 3, 'story')");
                statement.executeUpdate("INSERT INTO Tags VALUES ('" + PLAYER_ID + "', 'chapter', 'winterfell', 'STRING', 'story')");
            }

            PlayerDatabase.prepare(connection, null);

            final PlayerDatabase.LoadedPlayer loaded =
                    PlayerDatabase.load(connection, PLAYER_ID).getFirst();
            assertEquals("story", loaded.profile());
            assertEquals("story", loaded.activeProfile());
            assertEquals(37, loaded.questPoints());
            assertEquals(List.of("TheVirus"), loaded.activeQuestNames());
            assertEquals("Introduction", loaded.completedQuests().getFirst().questName());
            assertEquals("OldQuest", loaded.failedQuests().getFirst().questName());
            assertEquals(2, loaded.activeTriggers("TheVirus").getFirst().currentProgress());
            assertEquals(12.5, loaded.activeObjectives("TheVirus").getFirst().currentProgress());
            assertEquals(
                    "ReachLocation",
                    loaded.activeObjectives("TheVirus.1").getFirst().objectiveType());
            assertTrue(loaded.activeObjectives("TheVirus.1").getFirst().completed());
            assertEquals(Map.of("chapter", "winterfell"), loaded.tags());
            assertTrue(tableExists(connection, "QuestPlayerData"));
            assertFalse(tableExists(connection, "QuestPlayer"));
        }
    }

    @Test
    void loadsTagsTogetherWithPlayerRuntime() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            PlayerDatabase.prepare(connection, null);
            PlayerDatabase.save(connection, List.of(snapshot(
                    7,
                    Map.of(
                            "boolean", true,
                            "integer", 12,
                            "float", 1.25f,
                            "double", 2.5d,
                            "String", "value"))));

            final PlayerDatabase.LoadedPlayer loaded =
                    PlayerDatabase.load(connection, PLAYER_ID).getFirst();

            assertEquals(7, loaded.questPoints());
            assertEquals(
                    Map.of(
                            "boolean", true,
                            "integer", 12,
                            "float", 1.25f,
                            "double", 2.5d,
                            "string", "value"),
                    loaded.tags());
        }
    }

    @Test
    void savingAnEmptyTagMapDeletesPreviouslyStoredTags() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            PlayerDatabase.prepare(connection, null);
            PlayerDatabase.save(connection, List.of(snapshot(7, Map.of("reputation", 5))));

            PlayerDatabase.save(connection, List.of(snapshot(8, Map.of())));

            final PlayerDatabase.LoadedPlayer loaded =
                    PlayerDatabase.load(connection, PLAYER_ID).getFirst();
            assertEquals(8, loaded.questPoints());
            assertEquals(Map.of(), loaded.tags());
        }
    }

    @Test
    void malformedNumericTagsDoNotAbortPlayerLoading() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            PlayerDatabase.prepare(connection, null);
            PlayerDatabase.save(connection, List.of(snapshot(7, Map.of("valid", 5))));
            try (var statement = connection.prepareStatement(PlayerDatabase.INSERT_TAG)) {
                insertTag(statement, "broken-integer", "not-an-integer", "INTEGER");
                insertTag(statement, "broken-float", "not-a-float", "FLOAT");
                insertTag(statement, "broken-double", "not-a-double", "DOUBLE");
            }

            final PlayerDatabase.LoadedPlayer loaded =
                    PlayerDatabase.load(connection, PLAYER_ID).getFirst();

            assertEquals(7, loaded.questPoints());
            assertEquals(Map.of("valid", 5), loaded.tags());
        }
    }

    @Test
    void tagInsertFailureRollsBackTheWholePlayerSave() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            PlayerDatabase.prepare(connection, null);
            PlayerDatabase.save(connection, List.of(snapshot(7, Map.of("reputation", 5))));
            try (var statement = connection.createStatement()) {
                statement.executeUpdate("""
                        CREATE TRIGGER reject_broken_tag
                        BEFORE INSERT ON Tags
                        WHEN NEW.TagIdentifier = 'broken'
                        BEGIN
                            SELECT RAISE(ABORT, 'forced tag insert failure');
                        END
                        """);
            }

            assertThrows(
                    SQLException.class,
                    () -> PlayerDatabase.save(
                            connection,
                            List.of(snapshot(99, Map.of("broken", "value")))));

            final PlayerDatabase.LoadedPlayer loaded =
                    PlayerDatabase.load(connection, PLAYER_ID).getFirst();
            assertEquals(7, loaded.questPoints());
            assertEquals(Map.of("reputation", 5), loaded.tags());
        }
    }

    private static PlayerDatabase.PlayerSnapshot snapshot(
            final long questPoints,
            final Map<String, Object> tags) {
        return new PlayerDatabase.PlayerSnapshot(
                PLAYER_ID,
                "default",
                "default",
                questPoints,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                tags);
    }

    private static void insertTag(
            final PreparedStatement statement,
            final String identifier,
            final String value,
            final String type) throws SQLException {
        statement.setString(1, PLAYER_ID);
        statement.setString(2, identifier);
        statement.setString(3, value);
        statement.setString(4, type);
        statement.setString(5, "default");
        statement.executeUpdate();
    }

    private static boolean tableExists(
            final java.sql.Connection connection,
            final String tableName) throws SQLException {
        try (var tables = connection.getMetaData().getTables(null, null, "%", new String[] {"TABLE"})) {
            while (tables.next()) {
                if (tableName.equalsIgnoreCase(tables.getString("TABLE_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }
}
