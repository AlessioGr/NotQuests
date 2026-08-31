package com.notquests.core.managers.tags;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.notquests.core.managers.PlayerDatabase;
import com.notquests.core.structs.QuestPlayer;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

class TagManagerPersistenceTest {
    private final TagManager persistence = new TagManager();

    @Test
    void loadOnJoinSkipsPlayersWithExistingTags() throws Exception {
        final QuestPlayer player = new QuestPlayer("player-uuid", "default");
        player.setTagValue("reputation", 5);

        final TagManager.LoadSummary result =
                persistence.loadOnJoin(null, player, "Player", null, false);

        assertTrue(result.skipped());
        assertEquals(1, result.loaded());
        assertEquals(5, player.getTagValue("reputation"));
    }

    @Test
    void saveOnQuitSkipsUntilTagsFinishedLoading() throws Exception {
        final QuestPlayer player = new QuestPlayer("player-uuid", "default");
        player.setTagValue("reputation", 5);

        final TagManager.SaveSummary result =
                persistence.saveOnQuit(null, player, "Player", null, false);

        assertTrue(result.skipped());
        assertEquals(0, result.saved());
    }

    @Test
    void saveOnQuitSkipsEmptyLoadedTags() throws Exception {
        final QuestPlayer player = new QuestPlayer("player-uuid", "default");
        player.setFinishedLoadingTags(true);

        final TagManager.SaveSummary result =
                persistence.saveOnQuit(null, player, "Player", null, false);

        assertTrue(result.skipped());
        assertEquals(0, result.saved());
    }

    @Test
    void loadOnJoinSkipsRowsWithUnknownTagTypesAndStillMarksTagsLoaded() throws Exception {
        final QuestPlayer player = new QuestPlayer("player-uuid", "default");
        try (var connection = DriverManager.getConnection("jdbc:sqlite::memory:");
                var statement = connection.createStatement()) {
            PlayerDatabase.prepare(connection, null);
            statement.executeUpdate("""
                    INSERT INTO Tags (PlayerUUID, TagIdentifier, TagValue, TagType, Profile)
                    VALUES ('player-uuid', 'bad', 'value', 'UNKNOWN', 'default')
                    """);
            statement.executeUpdate("""
                    INSERT INTO Tags (PlayerUUID, TagIdentifier, TagValue, TagType, Profile)
                    VALUES ('player-uuid', 'good', 'value', 'STRING', 'default')
                    """);

            final TagManager.LoadSummary result =
                    persistence.loadOnJoin(connection, player, "Player", null, false);

            assertEquals(1, result.loaded());
            assertEquals("value", player.getTagValue("good"));
            assertEquals(null, player.getTagValue("bad"));
            assertTrue(player.isFinishedLoadingTags());
        }
    }

    @Test
    void batchLoadKeepsOnlinePlayerSkipMessagesInCore() {
        final CapturingLogger logger = new CapturingLogger();

        persistence.loadAllOnlinePlayers(
                () -> {
                    throw new SQLException("Should not open a connection for players without quest state.");
                },
                List.of(new TagManager.OnlinePlayerTags(null, "player-uuid", "Player")),
                logger,
                false);

        assertEquals(List.of(
                "Loading tags of all online players...",
                "Loading tags of all online player Player",
                "Loading Saving tags of all online player Player because they have no questplayer."),
                logger.infoMessages);
    }

    @Test
    void batchSaveKeepsOnlinePlayerSkipMessagesInCore() {
        final CapturingLogger logger = new CapturingLogger();

        persistence.saveAllOnlinePlayers(
                () -> {
                    throw new SQLException("Should not open a connection for players without quest state.");
                },
                List.of(new TagManager.OnlinePlayerTags(null, "player-uuid", "Player")),
                logger,
                false);

        assertEquals(List.of(
                "Saving tags of all online players...",
                "Saving tags of all online player Player",
                "Skip Saving tags of all online player Player because they have no questplayer."),
                logger.infoMessages);
    }

    private static final class CapturingLogger implements TagManager.BatchLogger {
        private final List<String> infoMessages = new ArrayList<>();

        @Override
        public void info(final String message, final Object... args) {
            infoMessages.add(message.formatted(args));
        }

        @Override
        public void warn(final String message, final Object... args) {}

        @Override
        public void severe(final String message, final Object... args) {}

        @Override
        public void stackTrace(final Throwable throwable) {}
    }
}
