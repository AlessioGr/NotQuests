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

import java.sql.*;
import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
    if(main.getConfiguration().isVerboseStartupMessages()){
      main.getLogManager().info("Loading PlayerData of player %s...", player.getName());
    }
    final UUID uuid = player.getUniqueId();
    questPlayersAndUUIDs.remove(uuid);


    loadPlayerDataInternal(player);


  }

  public void saveSinglePlayerData(final Player player) {
    if(player == null){
      main.getLogManager().warn("Saving of single PlayerData has been skipped for a certain player, as they are null");
      return;
    }
    if(main.getConfiguration().isVerboseStartupMessages()){
      main.getLogManager().info("Saving PlayerData of player %s...", player.getName());
    }

    if (!main.getConfiguration().savePlayerData) {
      main.getLogManager().info("Saving of PlayerData has been skipped...");
      return;
    }

    QuestPlayer questPlayer = getQuestPlayer(player.getUniqueId());
    if (questPlayer == null) {
      return;
    }
    if(!questPlayer.isFinishedLoadingGeneralData()){
      main.getLogManager().info("Saving of PlayerData has been skipped, because PlayerData didn't even finish loading yet.");
      return;
    }


    savePlayerDataInternal(List.of(questPlayer));


    if(main.getConfiguration().isVerboseStartupMessages()){
      main.getLogManager().info("PlayerData of player %s was saved (%s QuestPoints)",
        player.getName(),
        questPlayer.getQuestPoints()
      );
    }

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

  private boolean isColumnThere(final ResultSet rs, final String column){
    try{
      rs.findColumn(column);
      return true;
    } catch (SQLException ignored){
      return false;
    }

  }

  public void loadAllPlayerDataAtOnce() {
    if (!main.getConfiguration().loadPlayerData) {
      main.getLogManager().info("Loading of PlayerData has been skipped...");
      return;
    }

    questPlayersAndUUIDs.clear();

    loadPlayerDataInternal(null);
    main.getTagManager().loadAllOnlinePlayerTags();
  }

  public void savePlayerData() {
    if (!main.getConfiguration().savePlayerData) {
      main.getLogManager().info("Saving of PlayerData has been skipped...");
      return;
    }

    main.getLogManager().info("Saving player data...");

    savePlayerDataInternal(questPlayersAndUUIDs.values().stream().toList());

    main.getLogManager().info("PlayerData of all players saved");
  }

  public final QuestPlayer getQuestPlayer(final UUID uuid) {
    return questPlayersAndUUIDs.get(uuid);
  }

  public final @NotNull QuestPlayer getOrCreateQuestPlayer(@NotNull final UUID uuid) {
    QuestPlayer foundQuestPlayer = getQuestPlayer(uuid);
    if (foundQuestPlayer == null) {
      foundQuestPlayer = new QuestPlayer(main, uuid, "default");
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

  public final String createQuestPlayer(final UUID uuid, final String profile) {
    QuestPlayer questPlayer = getQuestPlayer(uuid);
    if (questPlayer == null) {
      questPlayer = new QuestPlayer(main, uuid, profile);
      questPlayersAndUUIDs.put(uuid, questPlayer);
      return "<success>Quest player with uuid <highlight>%s</highlight> has been created successfully!".formatted(uuid);

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



  private void loadPlayerDataInternal(final @Nullable Player player) {
    try (Connection connection = main.getDataManager().getConnection();
         final PreparedStatement questPlayerDataPSIfPlayerProvided = connection.prepareStatement("""
            SELECT * FROM QuestPlayerData WHERE PlayerUUID LIKE ?;
          """);
         final PreparedStatement questPlayerDataPSIfPlayerNotProvided = connection.prepareStatement("""
            SELECT * FROM QuestPlayerData;
          """);
         final PreparedStatement completedQuestsPS = connection.prepareStatement("""
            SELECT QuestName, TimeCompleted FROM CompletedQuests
            WHERE PlayerUUID = ? AND Profile = ?;
          """);
         final PreparedStatement activeQuestsPS = connection.prepareStatement("""
            SELECT QuestName FROM ActiveQuests
            WHERE PlayerUUID = ? AND Profile = ?;
          """);
         final PreparedStatement activeQuestTriggersPS = connection.prepareStatement("""
            SELECT * FROM ActiveTriggers
            WHERE PlayerUUID = ? AND Profile = ? AND QuestName = ?;
          """);
         final PreparedStatement activeQuestObjectivesPS = connection.prepareStatement("""
            SELECT * FROM ActiveObjectives
            WHERE PlayerUUID = ? AND Profile = ? AND QuestName = ?;
          """)
    ) {


      final ResultSet questPlayerDataResult;
      if(player != null){
        questPlayerDataPSIfPlayerProvided.setString(1, player.getUniqueId().toString());
        questPlayerDataResult = questPlayerDataPSIfPlayerProvided.executeQuery();
      }else{
        questPlayerDataResult = questPlayerDataPSIfPlayerNotProvided.executeQuery();
      }

      main.getLogManager().debug("Before questPlayerDataResult.next()");
      while (questPlayerDataResult.next()) {
        main.getLogManager().debug("Next result!");

        final UUID uuid;
        if(player != null){
          uuid = player.getUniqueId();
        }else{
          uuid = UUID.fromString(questPlayerDataResult.getString("PlayerUUID"));
        }
        completedQuestsPS.setString(1, uuid.toString());
        activeQuestsPS.setString(1, uuid.toString());
        activeQuestTriggersPS.setString(1, uuid.toString());
        activeQuestObjectivesPS.setString(1, uuid.toString());

        String profile;
        if(isColumnThere(questPlayerDataResult, "Profile")){
          profile = questPlayerDataResult.getString("Profile");
          if(profile == null || profile.isBlank()){
            profile = "default";
          }
        }else{
          profile = "default";
        }

        main.getLogManager().debug("Profile: %s", profile);


        completedQuestsPS.setString(2, profile);
        activeQuestsPS.setString(2, profile);
        activeQuestTriggersPS.setString(2, profile);
        activeQuestObjectivesPS.setString(2, profile);

        createQuestPlayer(uuid, profile);
        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(uuid);

        final long questPoints = questPlayerDataResult.getLong("QuestPoints");
        if (main.getConfiguration().isVerboseStartupMessages()) {
          main.getLogManager()
                  .info(
                          "Loaded player with uuid <highlight>%s</highlight> and questPoints: %s",
                          uuid.toString(),
                          questPoints);
        }


        if (questPlayer != null) {
          // QuestPoints
          questPlayer.setQuestPoints(questPoints, false);

        } else {
          main.getLogManager()
                  .severe(
                          "ERROR: QuestPlayer with the UUID <highlight>%s</highlight> could not be loaded from database",
                          uuid.toString());

          return;
        }

        // Active Quests
        final ArrayList<ActiveQuest> activeQuests = new ArrayList<>();

        // Completed Quests
        final ResultSet completedQuestsResults = completedQuestsPS.executeQuery();

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
        ResultSet activeQuestsResults = activeQuestsPS.executeQuery();

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

        for (final ActiveQuest activeQuest : activeQuests) {

          // Active Triggers
          activeQuestTriggersPS.setString(3, activeQuest.getQuest().getIdentifier());
          final ResultSet activeQuestTriggerResults = activeQuestTriggersPS.executeQuery();
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
          activeQuestObjectivesPS.setString(3, activeQuest.getQuest().getIdentifier());
          ResultSet activeQuestObjectiveResults = activeQuestObjectivesPS.executeQuery();

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
        questPlayer.setFinishedLoadingGeneralData(true);


        if(player != null){
          //Load single player data => player actually joined and tagmanager wont load automatically after that
          questPlayer.onJoinAsync(player);
          Bukkit.getScheduler()
                  .runTask(
                          main.getMain(),
                          () -> {
                            questPlayer.onJoin(player);
                          });
        }
      }
      if(player != null){
        final QuestPlayer questPlayer = main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId());
        questPlayer.setCurrentlyLoading(false);
        questPlayer.setFinishedLoadingGeneralData(true);
      }
    } catch (Exception e) {
      if(player != null){
        main.getDataManager()
                .disablePluginAndSaving(
                        "There was a database error, so QuestPlayer loading for player <highlight>%s</highlight> has been disabled. (1.1)".formatted(
                                player.getName()
                        ),
                        e);
      }else{
        main.getLogManager().warn("There was an error saving the PlayerData! Stacktrace:");
      }
    }
  }


  private void savePlayerDataInternal(final List<QuestPlayer> questPlayers) {
    try (Connection connection = main.getDataManager().getConnection();
         final PreparedStatement deleteFromQuestPlayerDataPS = connection.prepareStatement("""
            DELETE FROM QuestPlayerData WHERE PlayerUUID = ? AND Profile = ?;
         """);
         final PreparedStatement insertIntoQuestPlayerDataPS = connection.prepareStatement("""
            INSERT INTO QuestPlayerData (PlayerUUID, QuestPoints, Profile) VALUES (?, ?, ?);
         """);

         final PreparedStatement deleteFromActiveQuestsPS = connection.prepareStatement("""
            DELETE FROM ActiveQuests WHERE PlayerUUID = ? AND Profile = ?;
         """);
         final PreparedStatement deleteFromActiveObjectivesPS = connection.prepareStatement("""
            DELETE FROM ActiveObjectives WHERE PlayerUUID = ? AND Profile = ?;
         """);

         final PreparedStatement insertIntoActiveQuestsPS = connection.prepareStatement("""
            INSERT INTO ActiveQuests (QuestName, PlayerUUID, Profile) VALUES (?, ?, ?);
         """);

         final PreparedStatement insertIntoActiveTriggersPS = connection.prepareStatement("""
            INSERT INTO ActiveTriggers (TriggerType, QuestName, PlayerUUID, CurrentProgress, TriggerID, Profile) VALUES (?, ?, ?, ?, ?, ?);
         """);

         final PreparedStatement insertIntoActiveObjectivesPS = connection.prepareStatement("""
            INSERT INTO ActiveObjectives (ObjectiveType, QuestName, PlayerUUID, CurrentProgress, ObjectiveID, HasBeenCompleted, ProgressNeeded, Profile) VALUES (?, ?, ?, ?, ?, ?, ?, ?);
         """);

         final PreparedStatement deleteFromCompletedQuestsPS = connection.prepareStatement("""
            DELETE FROM CompletedQuests WHERE PlayerUUID = ? AND Profile = ?;
         """);
         final PreparedStatement insertIntoCompletedQuestsPS = connection.prepareStatement("""
            INSERT INTO CompletedQuests (QuestName, PlayerUUID, TimeCompleted, Profile) VALUES (?, ?, ?, ?);
         """)
    ) {
      for (final QuestPlayer questPlayer : questPlayers) {
        final long questPoints = questPlayer.getQuestPoints();
        final UUID questPlayerUUID = questPlayer.getUniqueId();
        final String profile = questPlayer.getProfile();
        // QuestPoints
        deleteFromQuestPlayerDataPS.setString(1, questPlayerUUID.toString());
        deleteFromQuestPlayerDataPS.setString(2, profile);
        deleteFromQuestPlayerDataPS.executeUpdate();

        insertIntoQuestPlayerDataPS.setString(1, questPlayerUUID.toString());
        insertIntoQuestPlayerDataPS.setLong(2, questPoints);
        insertIntoQuestPlayerDataPS.setString(3, profile);
        insertIntoQuestPlayerDataPS.executeUpdate();

        // Active Quests and Active Objectives
        deleteFromActiveQuestsPS.setString(1, questPlayerUUID.toString());
        deleteFromActiveQuestsPS.setString(2, profile);
        deleteFromActiveQuestsPS.executeUpdate();
        deleteFromActiveObjectivesPS.setString(1, questPlayerUUID.toString());
        deleteFromActiveObjectivesPS.setString(2, profile);
        deleteFromActiveObjectivesPS.executeUpdate();

        for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
          insertIntoActiveQuestsPS.setString(1, activeQuest.getQuest().getIdentifier());
          insertIntoActiveQuestsPS.setString(2, questPlayerUUID.toString());
          insertIntoActiveQuestsPS.setString(3, profile);
          insertIntoActiveQuestsPS.executeUpdate();

          // Active Triggers
          for (final ActiveTrigger activeTrigger : activeQuest.getActiveTriggers()) {
            insertIntoActiveTriggersPS.setString(1, activeTrigger.getTrigger().getTriggerType());
            insertIntoActiveTriggersPS.setString(2, activeTrigger.getActiveQuest().getQuest().getIdentifier());
            insertIntoActiveTriggersPS.setString(3, questPlayerUUID.toString());
            insertIntoActiveTriggersPS.setLong(4, activeTrigger.getCurrentProgress());
            insertIntoActiveTriggersPS.setInt(5, activeTrigger.getTriggerID());
            insertIntoActiveTriggersPS.setString(6, profile);
            insertIntoActiveTriggersPS.executeUpdate();
          }

          // Active Objectives
          for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
            insertIntoActiveObjectivesPS.setString(1, main.getObjectiveManager().getObjectiveType(activeObjective.getObjective().getClass()));
            insertIntoActiveObjectivesPS.setString(2, activeObjective.getActiveObjectiveHolder().getObjectiveHolder().getIdentifier());
            insertIntoActiveObjectivesPS.setString(3, questPlayerUUID.toString());
            insertIntoActiveObjectivesPS.setDouble(4, activeObjective.getCurrentProgress());
            insertIntoActiveObjectivesPS.setInt(5, activeObjective.getObjectiveID());
            insertIntoActiveObjectivesPS.setBoolean(6, activeObjective.hasBeenCompleted());
            insertIntoActiveObjectivesPS.setDouble(7, activeObjective.getProgressNeeded());
            insertIntoActiveObjectivesPS.setString(8, profile);
            insertIntoActiveObjectivesPS.executeUpdate();
          }
          // Active Objectives from completed Objective list
          for (final ActiveObjective completedObjective : activeQuest.getCompletedObjectives()) {
            insertIntoActiveObjectivesPS.setString(1, main.getObjectiveManager().getObjectiveType(completedObjective.getObjective().getClass()));
            insertIntoActiveObjectivesPS.setString(2, completedObjective.getActiveObjectiveHolder().getObjectiveHolder().getIdentifier());
            insertIntoActiveObjectivesPS.setString(3, questPlayerUUID.toString());
            insertIntoActiveObjectivesPS.setDouble(4, completedObjective.getCurrentProgress());
            insertIntoActiveObjectivesPS.setInt(5, completedObjective.getObjectiveID());
            insertIntoActiveObjectivesPS.setBoolean(6,  completedObjective.hasBeenCompleted());
            insertIntoActiveObjectivesPS.setDouble(7, completedObjective.getProgressNeeded());
            insertIntoActiveObjectivesPS.setString(8, profile);
            insertIntoActiveObjectivesPS.executeUpdate();
          }
        }

        // Completed Quests
        deleteFromCompletedQuestsPS.setString(1, questPlayerUUID.toString());
        deleteFromCompletedQuestsPS.setString(2, profile);
        deleteFromCompletedQuestsPS.executeUpdate();

        for (final CompletedQuest completedQuest : questPlayer.getCompletedQuests()) {
          insertIntoCompletedQuestsPS.setString(1, completedQuest.getQuest().getIdentifier());
          insertIntoCompletedQuestsPS.setString(2, questPlayerUUID.toString());
          insertIntoCompletedQuestsPS.setLong(3, completedQuest.getTimeCompleted());
          insertIntoCompletedQuestsPS.setString(4, profile);
          insertIntoCompletedQuestsPS.executeUpdate();
        }
      }
    } catch (Exception e) {
      if(questPlayers.size() == 1){
        main.getLogManager()
                .warn(
                        "There was an error saving the PlayerData of player with UUID <highlight>%s</highlight>! Stacktrace:", questPlayers.get(0).getUniqueId());
      }else{
        main.getLogManager().warn("There was an error loading the PlayerData! Stacktrace:");
      }
      e.printStackTrace();
    }

  }


  }
