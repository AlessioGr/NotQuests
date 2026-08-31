package com.notquests.paper.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import com.notquests.core.managers.PlayerDatabase;
import com.notquests.core.text.NotQuestsColors.Messages;
import com.notquests.paper.NotQuests;
import com.notquests.paper.PaperNotQuestsAdapter;
import com.notquests.paper.PaperPlayer;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

/**
 * INTEGRATION tests for the player-data save path. These run the <b>real</b> NotQuests persistence
 * code — {@code NotQuestsPlugin.saveAllPlayerData()} → the core-owned player database — against a
 * <b>real SQLite database</b> (the same driver/pool the server uses), with real
 * {@link PaperPlayer} adapter objects and core-owned quest state. No save logic is mocked.
 *
 * <p>Why the object graph is assembled by hand: the plugin's {@code onEnable()}/{@code onLoad()}
 * wires NMS packet handlers and Cloud/Brigadier commands, which cannot run off a real server, so we
 * construct exactly the managers the save touches (all NMS-free) and inject them via reflection
 * (NotQuests exposes only getters). Everything in the save + DB path is genuine production code.
 *
 * <p>Each test simulates what a real server hits when the database rejects a write part-way through
 * a save (disk full, lock timeout, constraint, dropped connection) by installing a {@code BEFORE
 * INSERT} trigger that aborts. Because the save runs each statement in JDBC autocommit with no
 * surrounding transaction, an already-executed {@code DELETE} is durable while the matching
 * {@code INSERT} fails — leaving the row permanently lost.
 *
 * <p>These tests assert the <b>correct</b> (atomic) outcome — that a failed save leaves the
 * previously-saved data intact — so they FAIL against the current code and will PASS once the save
 * is wrapped in a transaction (setAutoCommit(false)+commit/rollback).
 */
class PlayerDataSaveIntegrationTest {

  private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");

  private NotQuests main;

  @BeforeEach
  void setUp() throws Exception {
    MockBukkit.mock();
    final Plugin plugin = MockBukkit.createMockPlugin("NotQuests");

    // Real NotQuests + the real Paper leaves the save path uses (all NMS-free), wired via reflection
    // because NotQuests exposes only getters. No onEnable()/onLoad() (those need NMS).
    main = new NotQuests((org.bukkit.plugin.java.JavaPlugin) plugin);
    final Messages messages = new Messages(ignored -> null);
    setField(main, "messages", messages);
    main.getCorePlugin().console(ignored -> {});
    main.getCorePlugin().playerDatabase(main.getMain().getDataFolder().toPath(), PlayerDatabase.Logger.NO_OP);

    // Real SQLite connection (same code the server runs) + the plugin's real schema.
    java.nio.file.Files.createDirectories(main.getMain().getDataFolder().toPath());
    main.getCorePlugin().openPlayerDatabase();
    createSchema();

    registerPlayerWithOneActiveQuestAndPoints(PLAYER, 42L, "test-quest");
  }

  @AfterEach
  void tearDown() {
    MockBukkit.unmock();
  }

  @Test
  @DisplayName("a save that fails while re-inserting ActiveQuests must not lose the active quest")
  void activeQuestSurvivesFailedSave() throws Exception {
    // First save persists the active quest for real.
    main.getCorePlugin().saveAllPlayerData();
    assertEquals(1, rowCount("ActiveQuests"), "sanity: the real save persisted the active quest");

    // Now the DB starts rejecting ActiveQuests inserts mid-save (simulated infra failure).
    installAbortInsertTrigger("ActiveQuests");

    // Second save: the real code DELETEs the active quest (autocommit -> durable) then fails to
    // re-INSERT it. With a transaction this would roll back; without one, the quest is lost.
    main.getCorePlugin().saveAllPlayerData();

    assertEquals(
        1,
        rowCount("ActiveQuests"),
        "FAILS on current code: the non-transactional save deleted the active quest and could not "
            + "re-insert it, so the player's quest progress was lost");
  }

  @Test
  @DisplayName("a save that fails while re-inserting QuestPlayerData must not lose quest points")
  void questPointsSurviveFailedSave() throws Exception {
    main.getCorePlugin().saveAllPlayerData();
    assertEquals(1, rowCount("QuestPlayerData"), "sanity: the real save persisted the quest points");

    installAbortInsertTrigger("QuestPlayerData");

    main.getCorePlugin().saveAllPlayerData();

    assertEquals(
        1,
        rowCount("QuestPlayerData"),
        "FAILS on current code: the non-transactional save deleted the QuestPlayerData row and "
            + "could not re-insert it, so the player's quest points were lost");
  }

  @Test
  @DisplayName("saving tags must not delete previously inserted tags for the same player")
  void savingMultipleTagsKeepsEveryTag() throws Exception {
    final PaperPlayer questPlayer = PaperNotQuestsAdapter.asPaperPlayer(
        main.getCorePlugin().getOrCreatePlatformPlayer(PLAYER.toString()));
    main.getCorePlugin().setPlayerTagValue(PLAYER.toString(), "default", "chapter", "one");
    main.getCorePlugin().setPlayerTagValue(PLAYER.toString(), "default", "score", 7.0d);

    main.getCorePlugin().saveAllPlayerData();

    assertEquals(2, rowCount("Tags"));
  }

  // --- helpers (real DB + real object graph; no mocking of NotQuests logic) ---

  private void registerPlayerWithOneActiveQuestAndPoints(
      final UUID uuid, final long points, final String questName) throws Exception {
    final PaperPlayer questPlayer = PaperNotQuestsAdapter.asPaperPlayer(
        main.getCorePlugin().getOrCreatePlatformPlayer(uuid.toString()));
    main.getCorePlugin().setQuestPoints(questPlayer, points, false);
    main.getCorePlugin().setPlayerFinishedLoadingGeneralData(uuid.toString(), "default", true);
    main.getCorePlugin().setPlayerFinishedLoadingTags(uuid.toString(), "default", true);
    main.getCorePlugin().setPlayerCurrentlyLoading(uuid.toString(), "default", false);
    main.getCorePlugin().questPlayer(uuid.toString(), "default").addActiveQuest(questName);

  }

  private long rowCount(final String table) throws Exception {
    try (Connection c = main.getCorePlugin().playerDatabaseConnection();
        PreparedStatement ps =
            c.prepareStatement("SELECT COUNT(*) FROM " + table + " WHERE PlayerUUID = ?")) {
      ps.setString(1, PLAYER.toString());
      try (ResultSet rs = ps.executeQuery()) {
        rs.next();
        return rs.getLong(1);
      }
    }
  }

  private void installAbortInsertTrigger(final String table) throws Exception {
    try (Connection c = main.getCorePlugin().playerDatabaseConnection();
        Statement s = c.createStatement()) {
      s.executeUpdate(
          "CREATE TRIGGER fail_"
              + table
              + "_insert BEFORE INSERT ON "
              + table
              + " BEGIN SELECT RAISE(ABORT, 'simulated database write failure'); END;");
    }
  }

  /** The plugin's real player-runtime schema. */
  private void createSchema() throws Exception {
    try (Connection c = main.getCorePlugin().playerDatabaseConnection()) {
      PlayerDatabase.prepare(c, PlayerDatabase.Logger.NO_OP);
    }
  }

  private static void setField(final Object target, final String name, final Object value)
      throws Exception {
    Class<?> type = target.getClass();
    while (type != null) {
      try {
        final Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
        return;
      } catch (final NoSuchFieldException e) {
        type = type.getSuperclass();
      }
    }
    throw new NoSuchFieldException(name + " on " + target.getClass());
  }
}
