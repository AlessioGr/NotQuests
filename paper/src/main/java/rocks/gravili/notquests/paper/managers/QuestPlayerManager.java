/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021-2022 Alessio Gravili
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package rocks.gravili.notquests.paper.managers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.ActiveQuest;
import rocks.gravili.notquests.paper.structs.CompletedQuest;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.triggers.ActiveTrigger;

public class QuestPlayerManager {
  private final NotQuests main;

  private final HashMap<UUID, QuestPlayer> questPlayersAndUUIDs;

  public QuestPlayerManager(NotQuests notQuests) {
    this.main = notQuests;
    questPlayersAndUUIDs = new HashMap<>();
  }

  public void loadSinglePlayerData(final Player player) {
    if (!main.getConfiguration().loadPlayerData) {
      return;
    }
    final UUID uuid = player.getUniqueId();
    questPlayersAndUUIDs.remove(uuid);

    createQuestPlayer(uuid);
    final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(uuid);

    try (Connection connection = main.getDataManager().getConnection();
        Statement statement = connection.createStatement(); ) {
      ResultSet result =
          statement.executeQuery(
              "SELECT QuestPoints FROM QuestPlayerData WHERE PlayerUUID LIKE '"
                  + uuid.toString()
                  + "';");
      while (result.next()) {

        final long questPoints = result.getLong("QuestPoints");
        main.getLogManager()
            .info(
                "Loaded player with uuid <highlight>"
                    + uuid
                    + "</highlight> and questPoints: "
                    + questPoints);

        if (questPlayer != null) {
          // QuestPoints
          questPlayer.setQuestPoints(questPoints, false);
        } else {
          main.getLogManager()
              .severe(
                  "ERROR: QuestPlayer with the UUID <highlight>"
                      + uuid
                      + "</highlight> could not be loaded from database");
        }
      }

      if (questPlayer == null) {
        return;
      }

      // Active Quests
      final ArrayList<ActiveQuest> activeQuests = new ArrayList<>();
      // Completed Quests
      ResultSet completedQuestsResults =
          statement.executeQuery(
              "SELECT QuestName, TimeCompleted FROM CompletedQuests WHERE PlayerUUID = '"
                  + uuid
                  + "';");
      while (completedQuestsResults.next()) {
        final String questName = completedQuestsResults.getString("QuestName");
        final Quest quest = main.getQuestManager().getQuest(questName);
        if (quest != null) {
          final long timeCompleted = completedQuestsResults.getLong("TimeCompleted");
          if (timeCompleted > 0) {
            final CompletedQuest completedQuest =
                new CompletedQuest(quest, questPlayer, timeCompleted);
            questPlayer.addCompletedQuest(completedQuest);

          } else {
            main.getLogManager()
                .warn(
                    "ERROR: TimeCompleted from Quest with name <highlight>"
                        + questName
                        + "</highlight> could not be loaded from database (requested for loading completed Quests)");
          }

        } else {
          main.getLogManager()
              .warn(
                  "ERROR: Quest with name <highlight>"
                      + questName
                      + "</highlight> could not be loaded from database (requested for loading completed Quests)");
        }
      }
      // completedQuestsResults.close();

      // Active Quests
      ResultSet activeQuestsResults =
          statement.executeQuery(
              "SELECT QuestName FROM ActiveQuests WHERE PlayerUUID = '" + uuid + "';");
      while (activeQuestsResults.next()) {
        final String questName = activeQuestsResults.getString("QuestName");
        final Quest quest = main.getQuestManager().getQuest(questName);
        if (quest != null) {
          final ActiveQuest activeQuest = new ActiveQuest(main, quest, questPlayer);
          activeQuests.add(activeQuest);
          questPlayer.forceAddActiveQuestSilent(
              activeQuest, false); // Run begin/accept trigger when plugin reloads if true

        } else {
          main.getLogManager()
              .warn(
                  "ERROR: Quest with name <highlight>"
                      + questName
                      + "</highlight> could not be loaded from database");
        }
      }
      // activeQuestsResults.close();

      for (ActiveQuest activeQuest : activeQuests) {
        // Active Triggers
        ResultSet activeQuestTriggerResults =
            statement.executeQuery(
                "SELECT TriggerType, CurrentProgress, TriggerID FROM ActiveTriggers WHERE PlayerUUID = '"
                    + uuid
                    + "' AND QuestName = '"
                    + activeQuest.getQuest().getIdentifier()
                    + "';");
        while (activeQuestTriggerResults.next()) {
          final String triggerTypeString = activeQuestTriggerResults.getString("TriggerType");
          final long currentProgress = activeQuestTriggerResults.getLong("CurrentProgress");

          if (triggerTypeString != null) {
            final int triggerID = activeQuestTriggerResults.getInt("TriggerID");

            for (ActiveTrigger activeTrigger : activeQuest.getActiveTriggers()) {
              if (activeTrigger.getTrigger().getTriggerType().equals(triggerTypeString)
                  && activeTrigger.getTriggerID() == triggerID) {
                activeTrigger.addProgressSilent(currentProgress);
              }
            }

          } else {
            main.getLogManager()
                .warn(
                    "ERROR: TriggerType for the Quest <highlight>"
                        + activeQuest.getQuest().getIdentifier()
                        + "</highlight> could not be loaded from database");
          }
        }
        // activeQuestTriggerResults.close();

        // Active Objectives
        ResultSet activeQuestObjectiveResults =
            statement.executeQuery(
                "SELECT ObjectiveType, CurrentProgress, HasBeenCompleted, ObjectiveID, ProgressNeeded FROM ActiveObjectives WHERE PlayerUUID = '"
                    + uuid
                    + "' AND QuestName = '"
                    + activeQuest.getQuest().getIdentifier()
                    + "';");
        while (activeQuestObjectiveResults.next()) {
          final String objectiveTypeString = activeQuestObjectiveResults.getString("ObjectiveType");
          final double currentProgress = activeQuestObjectiveResults.getDouble("CurrentProgress");
          final boolean hasBeenCompleted =
              activeQuestObjectiveResults.getBoolean("HasBeenCompleted");
          final double progressNeeded = activeQuestObjectiveResults.getDouble("ProgressNeeded");
          final boolean progressNeededNull = activeQuestObjectiveResults.wasNull();

          if (objectiveTypeString != null) {
            final int objectiveID = activeQuestObjectiveResults.getInt("ObjectiveID");

            // So the active objectives are already there - we just need to fill them with progress
            // data.
            for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
              if (activeObjective.getObjective().getClass()
                      == main.getObjectiveManager().getObjectiveClass(objectiveTypeString)
                  && activeObjective.getObjectiveID() == objectiveID) {
                // System.out.println("§4§lHAS BEEN COMPLETED: §b" + hasBeenCompleted + " §c- ID:
                // §b" + objectiveID);
                if (!progressNeededNull) {
                  activeObjective.setProgressNeeded(progressNeeded);
                }
                activeObjective.setHasBeenCompleted(hasBeenCompleted);
                if (activeObjective.getObjective().getCompletionNPC() == null) { // Complete automatically
                  activeObjective.addProgress(currentProgress, true);
                } else { // Only complete if player has talked to the completion NPC
                  if (activeObjective.hasBeenCompleted()) {
                    activeObjective.addProgress(
                        currentProgress, activeObjective.getObjective().getCompletionNPC(), true);

                  } else {
                    activeObjective.addProgress(currentProgress, true);
                  }
                }
              }
            }
            activeQuest.removeCompletedObjectives(false);

          } else {
            main.getLogManager()
                .warn(
                    "ERROR: ObjectiveType for the Quest <highlight>"
                        + activeQuest.getQuest().getIdentifier()
                        + "</highlight> could not be loaded from database");
          }
        }

        // Update all active objectives to see if they are unlocked
        for (final ActiveObjective activeObjectiveToCheckForIfUnlocked :
            activeQuest.getActiveObjectives()) {
          activeObjectiveToCheckForIfUnlocked.updateUnlocked(false, true);
        }

        // activeQuestObjectiveResults.close();
      }

      questPlayer.removeCompletedQuests();

      questPlayer.setCurrentlyLoading(false);
      questPlayer.setFinishedLoadingGeneralData(true);

    } catch (Exception e) {
      main.getDataManager()
          .disablePluginAndSaving(
              "There was a database error, so QuestPlayer loading for player <highlight>"
                  + player.getName()
                  + "</highlight> has been disabled. (1.1)",
              e);
      return;
    }

    questPlayer.onJoinAsync(player);
    Bukkit.getScheduler()
        .runTask(
            main.getMain(),
            () -> {
              questPlayer.onJoin(player);
            });
  }

  public void saveSinglePlayerData(final Player player) {
    main.getLogManager().info("Saving PlayerData of player " + player.getName() + "...");

    if (!main.getConfiguration().savePlayerData) {
      main.getLogManager().info("Saving of playerdata has been skipped...");
      return;
    }

    QuestPlayer questPlayer = getQuestPlayer(player.getUniqueId());
    if (questPlayer == null) {
      return;
    }
    if(!questPlayer.isFinishedLoadingGeneralData()){
      main.getLogManager().info("Saving of playerdata has been skipped, because playerdata didn't even finish loading yet.");
      return;
    }

    final long questPoints = questPlayer.getQuestPoints();
    final UUID questPlayerUUID = questPlayer.getUniqueId();

    try (Connection connection = main.getDataManager().getConnection();
        Statement statement = connection.createStatement(); ) {
      // QuestPoints
      statement.executeUpdate(
          "DELETE FROM QuestPlayerData WHERE PlayerUUID = '" + questPlayerUUID.toString() + "';");
      statement.executeUpdate(
          "INSERT INTO QuestPlayerData (PlayerUUID, QuestPoints) VALUES ('"
              + questPlayerUUID
              + "', "
              + questPoints
              + ");");

      // Active Quests
      statement.executeUpdate(
          "DELETE FROM ActiveQuests WHERE PlayerUUID = '" + questPlayerUUID + "';");
      statement.executeUpdate(
          "DELETE FROM ActiveObjectives WHERE PlayerUUID = '" + questPlayerUUID + "';");
      for (ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
        statement.executeUpdate(
            "INSERT INTO ActiveQuests (QuestName, PlayerUUID) VALUES ('"
                + activeQuest.getQuest().getIdentifier()
                + "', '"
                + questPlayerUUID
                + "');");
        // Active Triggers
        for (ActiveTrigger activeTrigger : activeQuest.getActiveTriggers()) {
          statement.executeUpdate(
              "INSERT INTO ActiveTriggers (TriggerType, QuestName, PlayerUUID, CurrentProgress, TriggerID) VALUES ('"
                  + activeTrigger.getTrigger().getTriggerType()
                  + "', '"
                  + activeTrigger.getActiveQuest().getQuest().getIdentifier()
                  + "', '"
                  + questPlayerUUID
                  + "', "
                  + activeTrigger.getCurrentProgress()
                  + ", "
                  + activeTrigger.getTriggerID()
                  + ");");
        }

        // Active Objectives
        for (ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
          statement.executeUpdate(
              "INSERT INTO ActiveObjectives (ObjectiveType, QuestName, PlayerUUID, CurrentProgress, ObjectiveID, HasBeenCompleted, ProgressNeeded) VALUES ('"
                  + main.getObjectiveManager()
                      .getObjectiveType(activeObjective.getObjective().getClass())
                  + "', '"
                  + activeObjective.getActiveObjectiveHolder().getObjectiveHolder().getIdentifier()
                  + "', '"
                  + questPlayerUUID
                  + "', "
                  + activeObjective.getCurrentProgress()
                  + ", "
                  + activeObjective.getObjectiveID()
                  + ", "
                  + activeObjective.hasBeenCompleted()
                  + ", "
                  + activeObjective.getProgressNeeded()
                  + ");");
        }
        // Active Objectives from completed Objective list
        for (ActiveObjective completedObjective : activeQuest.getCompletedObjectives()) {
          statement.executeUpdate(
              "INSERT INTO ActiveObjectives (ObjectiveType, QuestName, PlayerUUID, CurrentProgress, ObjectiveID, HasBeenCompleted, ProgressNeeded) VALUES ('"
                  + main.getObjectiveManager()
                      .getObjectiveType(completedObjective.getObjective().getClass())
                  + "', '"
                  + completedObjective.getActiveObjectiveHolder().getObjectiveHolder().getIdentifier()
                  + "', '"
                  + questPlayerUUID
                  + "', "
                  + completedObjective.getCurrentProgress()
                  + ", "
                  + completedObjective.getObjectiveID()
                  + ", "
                  + completedObjective.hasBeenCompleted()
                  + ", "
                  + completedObjective.getProgressNeeded()
                  + ");");
        }
      }

      // Completed Quests
      statement.executeUpdate(
          "DELETE FROM CompletedQuests WHERE PlayerUUID = '" + questPlayerUUID + "';");
      for (CompletedQuest completedQuest : questPlayer.getCompletedQuests()) {
        statement.executeUpdate(
            "INSERT INTO CompletedQuests (QuestName, PlayerUUID, TimeCompleted) VALUES ('"
                + completedQuest.getQuest().getIdentifier()
                + "', '"
                + questPlayerUUID
                + "', "
                + completedQuest.getTimeCompleted()
                + ");");
      }
    } catch (Exception e) {
      main.getLogManager()
          .warn(
              "There was an error saving the playerdata of player with UUID <highlight>"
                  + questPlayer.getUniqueId()
                  + "</highlight>! Stacktrace:");
      e.printStackTrace();
      return;
    }

    main.getLogManager().info("PlayerData of player " + player.getName() + " was saved (" + questPoints + " QuestPoints)");

    questPlayer.onQuitAsync(player);
    if (!Bukkit.isPrimaryThread()) {
      Bukkit.getScheduler()
          .runTask(
              main.getMain(),
              () -> {
                questPlayer.onQuit(player);
              });
    } else {
      questPlayer.onQuit(player);
    }

    questPlayersAndUUIDs.remove(player.getUniqueId());
  }

  public void loadPlayerData() {
    if (!main.getConfiguration().loadPlayerData) {
      main.getLogManager().info("Loading of playerdata has been skipped...");
      return;
    }

    questPlayersAndUUIDs.clear();

    try (Connection connection = main.getDataManager().getConnection();
        Statement statement = connection.createStatement(); ) {
      // Quest Players
      ResultSet result = statement.executeQuery("SELECT * FROM QuestPlayerData");
      while (result.next()) {
        final UUID uuid = UUID.fromString(result.getString("PlayerUUID"));
        createQuestPlayer(uuid);
        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(uuid);

        final long questPoints = result.getLong("QuestPoints");
        main.getLogManager()
            .info(
                "Loaded player with uuid <highlight>"
                    + uuid
                    + "</highlight> and questPoints: "
                    + questPoints);

        if (questPlayer != null) {
          // QuestPoints
          questPlayer.setQuestPoints(questPoints, false);

        } else {
          main.getLogManager()
              .severe(
                  "ERROR: QuestPlayer with the UUID <highlight>"
                      + uuid
                      + "</highlight> could not be loaded from database");
        }
      }

      // Active Quests
      final ArrayList<ActiveQuest> activeQuests = new ArrayList<>();
      for (final QuestPlayer questPlayer : questPlayersAndUUIDs.values()) {
        activeQuests.clear();

        // Completed Quests
        ResultSet completedQuestsResults =
            statement.executeQuery(
                "SELECT QuestName, TimeCompleted FROM CompletedQuests WHERE PlayerUUID = '"
                    + questPlayer.getUniqueId().toString()
                    + "';");
        while (completedQuestsResults.next()) {
          final String questName = completedQuestsResults.getString("QuestName");
          final Quest quest = main.getQuestManager().getQuest(questName);
          if (quest != null) {
            final long timeCompleted = completedQuestsResults.getLong("TimeCompleted");
            if (timeCompleted > 0) {
              final CompletedQuest completedQuest =
                  new CompletedQuest(quest, questPlayer, timeCompleted);
              questPlayer.addCompletedQuest(completedQuest);

            } else {
              main.getLogManager()
                  .warn(
                      "ERROR: TimeCompleted from Quest with name <highlight>"
                          + questName
                          + "</highlight> could not be loaded from database (requested for loading completed Quests)");
            }

          } else {
            main.getLogManager()
                .warn(
                    "ERROR: Quest with name <highlight>"
                        + questName
                        + "</highlight> could not be loaded from database (requested for loading completed Quests)");
          }
        }
        // completedQuestsResults.close();

        // Active Quests
        ResultSet activeQuestsResults =
            statement.executeQuery(
                "SELECT QuestName FROM ActiveQuests WHERE PlayerUUID = '"
                    + questPlayer.getUniqueId()
                    + "';");
        while (activeQuestsResults.next()) {
          final String questName = activeQuestsResults.getString("QuestName");
          final Quest quest = main.getQuestManager().getQuest(questName);
          if (quest != null) {
            final ActiveQuest activeQuest = new ActiveQuest(main, quest, questPlayer);
            activeQuests.add(activeQuest);
            questPlayer.forceAddActiveQuestSilent(
                activeQuest, false); // Run begin/accept trigger when plugin reloads if true

          } else {
            main.getLogManager()
                .warn(
                    "ERROR: Quest with name <highlight>"
                        + questName
                        + "</highlight> could not be loaded from database");
          }
        }
        // activeQuestsResults.close();

        for (ActiveQuest activeQuest : activeQuests) {

          // Active Triggers
          ResultSet activeQuestTriggerResults =
              statement.executeQuery(
                  "SELECT * FROM ActiveTriggers WHERE PlayerUUID = '"
                      + questPlayer.getUniqueId()
                      + "' AND QuestName = '"
                      + activeQuest.getQuest().getIdentifier()
                      + "';");
          while (activeQuestTriggerResults.next()) {
            final String triggerTypeString = activeQuestTriggerResults.getString("TriggerType");
            final long currentProgress = activeQuestTriggerResults.getLong("CurrentProgress");

            if (triggerTypeString != null) {
              final int triggerID = activeQuestTriggerResults.getInt("TriggerID");

              for (ActiveTrigger activeTrigger : activeQuest.getActiveTriggers()) {
                if (activeTrigger.getTrigger().getTriggerType().equals(triggerTypeString)
                    && activeTrigger.getTriggerID() == triggerID) {
                  activeTrigger.addProgressSilent(currentProgress);
                }
              }

            } else {
              main.getLogManager()
                  .warn(
                      "ERROR: TriggerType for the Quest <highlight>"
                          + activeQuest.getQuest().getIdentifier()
                          + "</highlight> could not be loaded from database");
            }
          }
          // activeQuestTriggerResults.close();

          // Active Objectives
          ResultSet activeQuestObjectiveResults =
              statement.executeQuery(
                  "SELECT * FROM ActiveObjectives WHERE PlayerUUID = '"
                      + questPlayer.getUniqueId()
                      + "' AND QuestName = '"
                      + activeQuest.getQuest().getIdentifier()
                      + "';");
          while (activeQuestObjectiveResults.next()) {
            final String objectiveTypeString =
                activeQuestObjectiveResults.getString("ObjectiveType");
            final double currentProgress = activeQuestObjectiveResults.getDouble("CurrentProgress");
            final boolean hasBeenCompleted =
                activeQuestObjectiveResults.getBoolean("HasBeenCompleted");
            final double progressNeeded = activeQuestObjectiveResults.getDouble("ProgressNeeded");
            final boolean progressNeededNull = activeQuestObjectiveResults.wasNull();

            if (objectiveTypeString != null) {
              final int objectiveID = activeQuestObjectiveResults.getInt("ObjectiveID");

              // So the active objectives are already there - we just need to fill them with
              // progress data.
              for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                if (activeObjective.getObjective().getClass()
                        == main.getObjectiveManager().getObjectiveClass(objectiveTypeString)
                    && activeObjective.getObjectiveID() == objectiveID) {
                  // System.out.println("§4§lHAS BEEN COMPLETED: §b" + hasBeenCompleted + " §c- ID:
                  // §b" + objectiveID);
                  if (!progressNeededNull) {
                    activeObjective.setProgressNeeded(progressNeeded);
                  }
                  activeObjective.setHasBeenCompleted(hasBeenCompleted);
                  if (activeObjective.getObjective().getCompletionNPC() == null) { // Complete automatically
                    activeObjective.addProgress(currentProgress, true);
                  } else { // Only complete if player has talked to the completion NPC
                    if (activeObjective.hasBeenCompleted()) {
                      activeObjective.addProgress(
                          currentProgress,
                          activeObjective.getObjective().getCompletionNPC(),
                          true);

                    } else {
                      activeObjective.addProgress(currentProgress, true);
                    }
                  }
                }
              }
              activeQuest.removeCompletedObjectives(false);

            } else {
              main.getLogManager()
                  .warn(
                      "ERROR: ObjectiveType for the Quest <highlight>"
                          + activeQuest.getQuest().getIdentifier()
                          + "</highlight> could not be loaded from database");
            }
          }

          // Update all active objectives to see if they are unlocked
          for (final ActiveObjective activeObjectiveToCheckForIfUnlocked :
              activeQuest.getActiveObjectives()) {
            activeObjectiveToCheckForIfUnlocked.updateUnlocked(false, true);
          }

          // activeQuestObjectiveResults.close();
        }

        questPlayer.removeCompletedQuests();

        questPlayer.setCurrentlyLoading(false);
      }

      main.getTagManager().loadAllOnlinePlayerTags();

    } catch (Exception e) {
      main.getLogManager().warn("There was an error saving the playerdata! Stacktrace:");
      e.printStackTrace();
    }
  }

  public void savePlayerData() {
    if (!main.getConfiguration().savePlayerData) {
      main.getLogManager().info("Saving of playerdata has been skipped...");
      return;
    }

    main.getLogManager().info("Saving player data...");

    try (Connection connection = main.getDataManager().getConnection();
        Statement statement = connection.createStatement(); ) {
      for (QuestPlayer questPlayer : questPlayersAndUUIDs.values()) {
        final long questPoints = questPlayer.getQuestPoints();
        final UUID questPlayerUUID = questPlayer.getUniqueId();
        // QuestPoints
        statement.executeUpdate(
            "DELETE FROM QuestPlayerData WHERE PlayerUUID = '" + questPlayerUUID.toString() + "';");
        statement.executeUpdate(
            "INSERT INTO QuestPlayerData (PlayerUUID, QuestPoints) VALUES ('"
                + questPlayerUUID
                + "', "
                + questPoints
                + ");");

        // Active Quests
        statement.executeUpdate(
            "DELETE FROM ActiveQuests WHERE PlayerUUID = '" + questPlayerUUID + "';");
        statement.executeUpdate(
            "DELETE FROM ActiveObjectives WHERE PlayerUUID = '" + questPlayerUUID + "';");
        for (ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
          statement.executeUpdate(
              "INSERT INTO ActiveQuests (QuestName, PlayerUUID) VALUES ('"
                  + activeQuest.getQuest().getIdentifier()
                  + "', '"
                  + questPlayerUUID
                  + "');");
          // Active Triggers
          for (ActiveTrigger activeTrigger : activeQuest.getActiveTriggers()) {
            statement.executeUpdate(
                "INSERT INTO ActiveTriggers (TriggerType, QuestName, PlayerUUID, CurrentProgress, TriggerID) VALUES ('"
                    + activeTrigger.getTrigger().getTriggerType()
                    + "', '"
                    + activeTrigger.getActiveQuest().getQuest().getIdentifier()
                    + "', '"
                    + questPlayerUUID
                    + "', "
                    + activeTrigger.getCurrentProgress()
                    + ", "
                    + activeTrigger.getTriggerID()
                    + ");");
          }

          // Active Objectives
          for (ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
            statement.executeUpdate(
                "INSERT INTO ActiveObjectives (ObjectiveType, QuestName, PlayerUUID, CurrentProgress, ObjectiveID, HasBeenCompleted, ProgressNeeded) VALUES ('"
                    + main.getObjectiveManager()
                        .getObjectiveType(activeObjective.getObjective().getClass())
                    + "', '"
                    + activeObjective.getActiveObjectiveHolder().getObjectiveHolder().getIdentifier()
                    + "', '"
                    + questPlayerUUID
                    + "', "
                    + activeObjective.getCurrentProgress()
                    + ", "
                    + activeObjective.getObjectiveID()
                    + ", "
                    + activeObjective.hasBeenCompleted()
                    + ", "
                    + activeObjective.getProgressNeeded()
                    + ");");
          }
          // Active Objectives from completed Objective list
          for (ActiveObjective completedObjective : activeQuest.getCompletedObjectives()) {
            statement.executeUpdate(
                "INSERT INTO ActiveObjectives (ObjectiveType, QuestName, PlayerUUID, CurrentProgress, ObjectiveID, HasBeenCompleted, ProgressNeeded) VALUES ('"
                    + main.getObjectiveManager()
                        .getObjectiveType(completedObjective.getObjective().getClass())
                    + "', '"
                    + completedObjective.getActiveObjectiveHolder().getObjectiveHolder().getIdentifier()
                    + "', '"
                    + questPlayerUUID
                    + "', "
                    + completedObjective.getCurrentProgress()
                    + ", "
                    + completedObjective.getObjectiveID()
                    + ", "
                    + completedObjective.hasBeenCompleted()
                    + ", "
                    + completedObjective.getProgressNeeded()
                    + ");");
          }
        }

        // Completed Quests
        statement.executeUpdate(
            "DELETE FROM CompletedQuests WHERE PlayerUUID = '" + questPlayerUUID + "';");
        for (CompletedQuest completedQuest : questPlayer.getCompletedQuests()) {
          statement.executeUpdate(
              "INSERT INTO CompletedQuests (QuestName, PlayerUUID, TimeCompleted) VALUES ('"
                  + completedQuest.getQuest().getIdentifier()
                  + "', '"
                  + questPlayerUUID
                  + "', "
                  + completedQuest.getTimeCompleted()
                  + ");");
        }
      }
    } catch (Exception e) {
      main.getLogManager().warn("There was an error loading the playerdata! Stacktrace:");
      e.printStackTrace();
    }

    main.getLogManager().info("PlayerData of all players saved");
  }

  public final QuestPlayer getQuestPlayer(final UUID uuid) {
    return questPlayersAndUUIDs.get(uuid);
  }

  public final @NotNull QuestPlayer getOrCreateQuestPlayer(@NotNull final UUID uuid) {
    QuestPlayer foundQuestPlayer = getQuestPlayer(uuid);
    if (foundQuestPlayer == null) {
      foundQuestPlayer = new QuestPlayer(main, uuid);
      questPlayersAndUUIDs.put(uuid, foundQuestPlayer);
    }
    return foundQuestPlayer;
  }

  public final Collection<QuestPlayer> getQuestPlayers() {
    return questPlayersAndUUIDs.values();
  }

  public String acceptQuest(
      final Player player,
      final Quest quest,
      final boolean triggerAcceptQuestTrigger,
      final boolean sendQuestInfo) {
    QuestPlayer questPlayer = getOrCreateQuestPlayer(player.getUniqueId());
    final ActiveQuest newActiveQuest = new ActiveQuest(main, quest, questPlayer);

    return questPlayer.addActiveQuest(newActiveQuest, triggerAcceptQuestTrigger, sendQuestInfo);
  }

  public String acceptQuest(
      final QuestPlayer questPlayer,
      final Quest quest,
      final boolean triggerAcceptQuestTrigger,
      final boolean sendQuestInfo) {
    final ActiveQuest newActiveQuest = new ActiveQuest(main, quest, questPlayer);

    return questPlayer.addActiveQuest(newActiveQuest, triggerAcceptQuestTrigger, sendQuestInfo);
  }

  public final String createQuestPlayer(final UUID uuid) {
    QuestPlayer questPlayer = getQuestPlayer(uuid);
    if (questPlayer == null) {
      questPlayer = new QuestPlayer(main, uuid);
      questPlayersAndUUIDs.put(uuid, questPlayer);
      return "<success>Quest player with uuid <highlight>"
          + uuid
          + "</highlight> has been created successfully!";

    } else {
      return "<error>Quest player already exists.";
    }
  }

  public final String forceAcceptQuest(
      final UUID uuid, final Quest quest) { // Ignores max amount limit, cooldown and requirements
    final QuestPlayer questPlayer = getOrCreateQuestPlayer(uuid);

    return questPlayer.forceAddActiveQuest(new ActiveQuest(main, quest, questPlayer), true);
  }

  public void forceAcceptQuestSilent(
      final UUID uuid, final Quest quest) { // Ignores max amount limit, cooldown and requirements
    final QuestPlayer questPlayer = getOrCreateQuestPlayer(uuid);

    questPlayer.forceAddActiveQuestSilent(new ActiveQuest(main, quest, questPlayer), true);
  }
}
