/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2022 Alessio Gravili
 *
 * Licensed under the GNU General Public License v3. See the LICENSE file.
 */

package rocks.gravili.notquests.paper.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.DataManager;
import rocks.gravili.notquests.paper.managers.LogManager;
import rocks.gravili.notquests.paper.managers.QuestManager;
import rocks.gravili.notquests.paper.managers.QuestPlayerManager;
import rocks.gravili.notquests.paper.minimessage.MessageManager;
import rocks.gravili.notquests.paper.structs.ActiveQuest;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

/**
 * INTEGRATION tests for the player-data save path. These run the <b>real</b> NotQuests persistence
 * code — {@link QuestPlayerManager#saveAllPlayerDataAtOnce()} →
 * {@code savePlayerDataInternal(...)} → the real {@link DataManager} connection — against a
 * <b>real SQLite database</b> (the same driver/pool the server uses), with real
 * {@link QuestPlayer}/{@link Quest}/{@link ActiveQuest} objects. No save logic is mocked.
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
  private QuestPlayerManager questPlayerManager;
  private DataManager dataManager;

  @BeforeEach
  void setUp() throws Exception {
    MockBukkit.mock();
    final Plugin plugin = MockBukkit.createMockPlugin("NotQuests");

    // Real NotQuests + the real managers the save path uses (all NMS-free), wired via reflection
    // because NotQuests exposes only getters. No onEnable()/onLoad() (those need NMS).
    main = new NotQuests((org.bukkit.plugin.java.JavaPlugin) plugin);
    final MessageManager messageManager = new MessageManager(main);
    setField(main, "messageManager", messageManager);
    dataManager = new DataManager(main);
    setField(main, "dataManager", dataManager);
    setField(main, "logManager", new LogManager(main)); // real logger (uses MockBukkit console)
    setField(main, "questManager", new QuestManager(main));
    questPlayerManager = new QuestPlayerManager(main);
    setField(main, "questPlayerManager", questPlayerManager);

    // Real SQLite connection (same code the server runs) + the plugin's real schema.
    dataManager.prepareDataFolder();
    dataManager.openConnection();
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
    questPlayerManager.saveAllPlayerDataAtOnce();
    assertEquals(1, rowCount("ActiveQuests"), "sanity: the real save persisted the active quest");

    // Now the DB starts rejecting ActiveQuests inserts mid-save (simulated infra failure).
    installAbortInsertTrigger("ActiveQuests");

    // Second save: the real code DELETEs the active quest (autocommit -> durable) then fails to
    // re-INSERT it. With a transaction this would roll back; without one, the quest is lost.
    questPlayerManager.saveAllPlayerDataAtOnce();

    assertEquals(
        1,
        rowCount("ActiveQuests"),
        "FAILS on current code: the non-transactional save deleted the active quest and could not "
            + "re-insert it, so the player's quest progress was lost");
  }

  @Test
  @DisplayName("a save that fails while re-inserting QuestPlayerData must not lose quest points")
  void questPointsSurviveFailedSave() throws Exception {
    questPlayerManager.saveAllPlayerDataAtOnce();
    assertEquals(1, rowCount("QuestPlayerData"), "sanity: the real save persisted the quest points");

    installAbortInsertTrigger("QuestPlayerData");

    questPlayerManager.saveAllPlayerDataAtOnce();

    assertEquals(
        1,
        rowCount("QuestPlayerData"),
        "FAILS on current code: the non-transactional save deleted the QuestPlayerData row and "
            + "could not re-insert it, so the player's quest points were lost");
  }

  // --- helpers (real DB + real object graph; no mocking of NotQuests logic) ---

  private void registerPlayerWithOneActiveQuestAndPoints(
      final UUID uuid, final long points, final String questName) throws Exception {
    final Quest quest = new Quest(main, questName);
    final QuestPlayer questPlayer = new QuestPlayer(main, uuid, "default");
    setField(questPlayer, "questPoints", points); // bypass setQuestPoints() (fires Bukkit events)
    questPlayer.setFinishedLoadingGeneralData(true);
    questPlayer.setFinishedLoadingTags(true);
    questPlayer.setCurrentlyLoading(false);
    questPlayer.getActiveQuests().add(new ActiveQuest(main, quest, questPlayer));

    questPlayerManager.getQuestPlayersForUUIDs().put(uuid, new ArrayList<>(List.of(questPlayer)));
  }

  private long rowCount(final String table) throws Exception {
    try (Connection c = dataManager.getConnection();
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
    try (Connection c = dataManager.getConnection();
        Statement s = c.createStatement()) {
      s.executeUpdate(
          "CREATE TRIGGER fail_"
              + table
              + "_insert BEFORE INSERT ON "
              + table
              + " BEGIN SELECT RAISE(ABORT, 'simulated database write failure'); END;");
    }
  }

  /** The plugin's real table DDL (from DataManager.reloadDataInternal). */
  private void createSchema() throws Exception {
    try (Connection c = dataManager.getConnection();
        Statement s = c.createStatement()) {
      s.executeUpdate(
          "CREATE TABLE IF NOT EXISTS `QuestPlayerProfileData` (`PlayerUUID` varchar(200), `CurrentProfile` varchar(200), PRIMARY KEY (PlayerUUID))");
      s.executeUpdate(
          "CREATE TABLE IF NOT EXISTS `QuestPlayerData` (`PlayerUUID` varchar(200), `QuestPoints` BIGINT(255), `Profile` varchar(200))");
      s.executeUpdate(
          "CREATE TABLE IF NOT EXISTS `ActiveQuests` (`QuestName` varchar(200), `PlayerUUID` varchar(200), `Profile` varchar(200))");
      s.executeUpdate(
          "CREATE TABLE IF NOT EXISTS `CompletedQuests` (`QuestName` varchar(200), `PlayerUUID` varchar(200), `TimeCompleted` BIGINT(255), `Profile` varchar(200))");
      s.executeUpdate(
          "CREATE TABLE IF NOT EXISTS `FailedQuests` (`QuestName` varchar(200), `PlayerUUID` varchar(200), `TimeFailed` BIGINT(255), `Profile` varchar(200))");
      s.executeUpdate(
          "CREATE TABLE IF NOT EXISTS `ActiveTriggers` (`TriggerType` varchar(200), `QuestName` varchar(200), `PlayerUUID` varchar(200), `CurrentProgress` BIGINT(255), `TriggerID` INT(255), `Profile` varchar(200))");
      s.executeUpdate(
          "CREATE TABLE IF NOT EXISTS `ActiveObjectives` (`ObjectiveType` varchar(200), `QuestName` varchar(200), `PlayerUUID` varchar(200), `CurrentProgress` DOUBLE, `ObjectiveID` INT(255), `HasBeenCompleted` BOOLEAN, `ProgressNeeded` DOUBLE, `Profile` varchar(200))");
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
