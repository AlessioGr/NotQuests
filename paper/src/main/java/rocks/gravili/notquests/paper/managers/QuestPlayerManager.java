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
import rocks.gravili.notquests.paper.structs.*;
import rocks.gravili.notquests.paper.structs.triggers.ActiveTrigger;

public class QuestPlayerManager {
  private final NotQuests main;

  private final HashMap<UUID, List<QuestPlayer>> questPlayersAndUUIDs; //Can contain multiple profiles since one UUID can have multiple profiles => multiple QuestPlayer
  private final HashMap<UUID, QuestPlayer> activeQuestPlayersAndUUIDs; //Only stores the current active profile


  public QuestPlayerManager(NotQuests notQuests) {
    this.main = notQuests;
    questPlayersAndUUIDs = new HashMap<>();
    activeQuestPlayersAndUUIDs = new HashMap<>();
  }

  public void loadSinglePlayerData(final UUID uuid) {
    if (!main.getConfiguration().loadPlayerData) {
      return;
    }
    if(main.getConfiguration().isVerboseStartupMessages()){
      main.getLogManager().info("Loading PlayerData of player %s...", uuid.toString());
    }
    questPlayersAndUUIDs.remove(uuid);
    activeQuestPlayersAndUUIDs.remove(uuid);

    loadPlayerDataInternal(uuid);


  }

  /**
   * Saves the player data of a single player. If saving player data on quit is enabled, this would
   * run in saveData() of DataManager where it would loop through all the players
   * @param player player whose data should be saved (for all their different QuestPlayer profiles)
   */
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


    final List<QuestPlayer> allProfileQuestPlayersForQuestPlayers = getQuestPlayersForUUIDs().get(player.getUniqueId());

    if(allProfileQuestPlayersForQuestPlayers == null) {
      main.getLogManager().debug("Saving of single PlayerData for " + player.getName() + " has been skipped, because they don't have any quest players / profiles.");
      return;
    }

    final ArrayList<QuestPlayer> questPlayersToLoad = new ArrayList<>();

    for(final QuestPlayer questPlayer : allProfileQuestPlayersForQuestPlayers){ //Error
      if (questPlayer == null) {
        return;
      }
      if(!questPlayer.isFinishedLoadingGeneralData()){
        main.getLogManager().info("Saving of PlayerData (Player UUID: %s, Player name: %s, Profile: %s) has been skipped, because PlayerData didn't even finish loading yet.",
                questPlayer.getUniqueId().toString(),
                questPlayer.getPlayer().getName(),
                questPlayer.getProfile()
        );
        return;
      }
      questPlayersToLoad.add(questPlayer);

      savePlayerDataInternal(questPlayersToLoad);

      if(main.getConfiguration().isVerboseStartupMessages()){
        main.getLogManager().info("PlayerData of player %s was saved (%s QuestPoints, Profile: %s)",
                player.getName(),
                questPlayer.getQuestPoints(),
                questPlayer.getProfile()
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
    }


    questPlayersAndUUIDs.remove(player.getUniqueId());
    activeQuestPlayersAndUUIDs.remove(player.getUniqueId());
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
    activeQuestPlayersAndUUIDs.clear();

    loadPlayerDataInternal(null);
    main.getTagManager().loadAllOnlinePlayerTags();
  }

  public void saveAllPlayerDataAtOnce() {
    if (!main.getConfiguration().savePlayerData) {
      main.getLogManager().info("Saving of PlayerData has been skipped...");
      return;
    }

    main.getLogManager().info("Saving player data...");

    savePlayerDataInternal(getAllQuestPlayersForAllProfiles());

    main.getLogManager().info("PlayerData of all players saved");
  }

  public final ArrayList<QuestPlayer> getAllQuestPlayersForAllProfiles(){
    return new ArrayList<>(){{
      questPlayersAndUUIDs.forEach((uuid, questPlayers) -> addAll(questPlayers));
    }};
  }

  public final HashMap<UUID, List<QuestPlayer>> getQuestPlayersForUUIDs() {
    return questPlayersAndUUIDs;
  }

  public final @Nullable QuestPlayer getActiveQuestPlayer(final UUID uuid) {
    return activeQuestPlayersAndUUIDs.get(uuid);
  }
  public final @Nullable QuestPlayer getQuestPlayer(final UUID uuid, final String profile) {
    for(final QuestPlayer questPlayer : questPlayersAndUUIDs.get(uuid)){
      if(profile.equals(questPlayer.getProfile())){
        return questPlayer;
      }
    }
    return null;
  }

  /*Useful for getting offline players*/
  public final @NotNull QuestPlayer getOrCreateQuestPlayerFromDatabase(@NotNull final UUID uuid ) {
    QuestPlayer foundQuestPlayer = getActiveQuestPlayer(uuid);
    if (foundQuestPlayer == null) {
      loadSinglePlayerData(uuid);
      foundQuestPlayer = getActiveQuestPlayer(uuid);
      foundQuestPlayer.setFinishedLoadingTags(true);
      return foundQuestPlayer;
    }
    return foundQuestPlayer;
  }
  public final @NotNull QuestPlayer getOrCreateQuestPlayer(@NotNull final UUID uuid) {
    QuestPlayer foundQuestPlayer = getActiveQuestPlayer(uuid);
    if (foundQuestPlayer == null) {
      foundQuestPlayer = new QuestPlayer(main, uuid, "default");
      foundQuestPlayer.setFinishedLoadingGeneralData(true);
      foundQuestPlayer.setFinishedLoadingTags(true);
      foundQuestPlayer.setCurrentlyLoading(false);
      if(questPlayersAndUUIDs.containsKey(uuid)){
        questPlayersAndUUIDs.get(uuid).add(foundQuestPlayer);
      } else {
        questPlayersAndUUIDs.put(uuid, List.of(foundQuestPlayer));
      }
      activeQuestPlayersAndUUIDs.put(uuid, foundQuestPlayer);
    }
    return foundQuestPlayer;
  }

  private String createQuestPlayer(final UUID uuid, final String profile, final boolean setAsCurrentProfile) {
    QuestPlayer questPlayer = getActiveQuestPlayer(uuid);

    if (questPlayer == null || !questPlayer.getProfile().equalsIgnoreCase(profile)) {
      questPlayer = new QuestPlayer(main, uuid, profile);



      if(questPlayersAndUUIDs.containsKey(uuid)){
        questPlayersAndUUIDs.get(uuid).add(questPlayer);
      } else {
        final ArrayList<QuestPlayer> newQuestPlayers = new ArrayList<>();
        newQuestPlayers.add(questPlayer);
        questPlayersAndUUIDs.put(uuid, newQuestPlayers);
      }
      if(setAsCurrentProfile){
        activeQuestPlayersAndUUIDs.put(uuid, questPlayer);
      }
      return "<success>Quest player with uuid <highlight>%s</highlight> has been created successfully!".formatted(uuid.toString());

    } else {
      return "<error>Quest player already exists.";
    }
  }

  public final Collection<QuestPlayer> getActiveQuestPlayers() {
    return activeQuestPlayersAndUUIDs.values();
  }

  public void changeProfile(final UUID uuid, final QuestPlayer newQuestPlayer){
    activeQuestPlayersAndUUIDs.put(uuid, newQuestPlayer);
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



  private void loadPlayerDataInternal(final @Nullable UUID playerUUID) {
    try (Connection connection = main.getDataManager().getConnection();
         final PreparedStatement questPlayerDataIfPlayerProvidedPS = connection.prepareStatement("""
            SELECT * FROM QuestPlayerData WHERE PlayerUUID = ?;
          """);
         final PreparedStatement questPlayerDataIfPlayerNotProvidedPS = connection.prepareStatement("""
            SELECT * FROM QuestPlayerData;
          """);
         final PreparedStatement questPlayerProfileDataPS = connection.prepareStatement("""
            SELECT * FROM QuestPlayerProfileData WHERE PlayerUUID = ?;
          """);
         final PreparedStatement completedQuestsPS = connection.prepareStatement("""
            SELECT QuestName, TimeCompleted FROM CompletedQuests
            WHERE PlayerUUID = ? AND Profile = ?;
          """);
         final PreparedStatement failedQuestsPS = connection.prepareStatement("""
            SELECT QuestName, TimeFailed FROM FailedQuests
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


      ResultSet questPlayerDataResult = null;
      try {
        if(playerUUID != null){
          questPlayerDataIfPlayerProvidedPS.setString(1, playerUUID.toString());
          questPlayerDataResult = questPlayerDataIfPlayerProvidedPS.executeQuery();
        } else {
          questPlayerDataResult = questPlayerDataIfPlayerNotProvidedPS.executeQuery();
        }




        main.getLogManager().debug("Before questPlayerDataResult.next()");
        while (questPlayerDataResult.next()) {
          main.getLogManager().debug("Next result!");

          final UUID uuid;
          if(playerUUID != null){
            uuid = playerUUID;
          }else{
            uuid = UUID.fromString(questPlayerDataResult.getString("PlayerUUID"));
          }
          completedQuestsPS.setString(1, uuid.toString());
          failedQuestsPS.setString(1, uuid.toString());
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
          failedQuestsPS.setString(2, profile);
          activeQuestsPS.setString(2, profile);
          activeQuestTriggersPS.setString(2, profile);
          activeQuestObjectivesPS.setString(2, profile);


          questPlayerProfileDataPS.setString(1, uuid.toString());

          try (final ResultSet currentQuestPlayerProfile = questPlayerProfileDataPS.executeQuery()) {
            String currentProfile = "default";
            while (currentQuestPlayerProfile.next()) {
              currentProfile = currentQuestPlayerProfile.getString("CurrentProfile");
              createQuestPlayer(uuid, profile, currentProfile == null || profile.equals(currentProfile) || currentProfile.isBlank());
              final QuestPlayer questPlayer = getQuestPlayer(uuid, profile);

              final long questPoints = questPlayerDataResult.getLong("QuestPoints");
              if (main.getConfiguration().isVerboseStartupMessages()) {
                main.getLogManager()
                        .info(
                                "Loaded player with uuid <highlight>%s</highlight> (Profile: %s) and questPoints: %s",
                                uuid.toString(),
                                profile,
                                questPoints);
              }


              if (questPlayer != null) {
                // QuestPoints
                questPlayer.setQuestPoints(questPoints, false);

              } else {
                main.getLogManager()
                        .severe(
                                "ERROR: QuestPlayer with the UUID <highlight>%s</highlight> for profile %s could not be loaded from database because it's null",
                                uuid.toString(),
                                profile
                        );

                return;
              }


              // Active Quests
              final ArrayList<ActiveQuest> activeQuests = new ArrayList<>();

              // Completed Quests
              try (final ResultSet completedQuestsResults = completedQuestsPS.executeQuery()) {
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
              }





              // Failed Quests
              try (final ResultSet failedQuestsResults = failedQuestsPS.executeQuery()) {
                while (failedQuestsResults.next()) {
                  final String questName = failedQuestsResults.getString("QuestName");
                  final Quest quest = main.getQuestManager().getQuest(questName);
                  if (quest != null) {
                    final long timeFailed = failedQuestsResults.getLong("TimeFailed");
                    if (timeFailed > 0) {
                      final FailedQuest failedQuest =
                              new FailedQuest(quest, questPlayer, timeFailed);
                      questPlayer.addFailedQuest(failedQuest);

                    } else {
                      main.getLogManager()
                              .warn(
                                      "ERROR: TimeFailed from Quest with name <highlight>"
                                              + questName
                                              + "</highlight> could not be loaded from database (requested for loading failed Quests)");
                    }

                  } else {
                    main.getLogManager()
                            .warn(
                                    "ERROR: Quest with name <highlight>"
                                            + questName
                                            + "</highlight> could not be loaded from database (requested for loading failed Quests)");
                  }
                }
              }



              // Active Quests
              try (final ResultSet activeQuestsResults = activeQuestsPS.executeQuery()) {
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
              }



              for (final ActiveQuest activeQuest : activeQuests) {

                // Active Triggers
                activeQuestTriggersPS.setString(3, activeQuest.getQuest().getIdentifier());
                try (final ResultSet activeQuestTriggerResults = activeQuestTriggersPS.executeQuery()) {
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
                }


                // Active Objectives
                handleLoadingOfActiveObjectives(activeQuestObjectivesPS, activeQuest);

              }

              questPlayer.removeCompletedQuests();

              questPlayer.setCurrentlyLoading(false);
              questPlayer.setFinishedLoadingGeneralData(true);


              if(playerUUID != null){
                //Load single player data => player actually joined and tagmanager wont load automatically after that
                final Player player = Bukkit.getPlayer(playerUUID);
                if(player != null){
                  questPlayer.onJoinAsync(player);
                  Bukkit.getScheduler()
                          .runTask(
                                  main.getMain(),
                                  () -> {
                                    questPlayer.onJoin(player);
                                  });
                }

              }
            }

          }






        }




      } finally {
        if (questPlayerDataResult != null) {
          questPlayerDataResult.close();
        }
      }


      if(playerUUID != null){
        if(getActiveQuestPlayer(playerUUID) == null){
          final QuestPlayer questPlayer = getOrCreateQuestPlayer(playerUUID);
          questPlayer.setCurrentlyLoading(false);
          questPlayer.setFinishedLoadingGeneralData(true);
          final Player player = Bukkit.getPlayer(playerUUID);
          if(player != null){
            questPlayer.onJoinAsync(player);
            Bukkit.getScheduler()
                    .runTask(
                            main.getMain(),
                            () -> {
                              questPlayer.onJoin(player);
                            });
          }

        }

      }
    } catch (Exception e) {
      if(playerUUID != null){
        main.getDataManager()
                .disablePluginAndSaving(
                        "There was a database error, so QuestPlayer loading for player <highlight>%s</highlight> has been disabled. (1.1)".formatted(
                                playerUUID.toString()
                        ),
                        e);
      }else{
        main.getLogManager().warn("There was an error saving the PlayerData! Stacktrace:");
      }
    }
  }

  private void handleLoadingOfActiveObjectives(final PreparedStatement activeQuestObjectivesPS, final ActiveObjectiveHolder activeObjectiveHolder) throws SQLException {
    String questName;
    if(activeObjectiveHolder instanceof final ActiveQuest activeQuest){
      questName = activeQuest.getQuestIdentifier();
    }else if(activeObjectiveHolder instanceof final ActiveObjective activeObjective){

      ActiveObjective lastActiveObjective = activeObjective;
      String counterWithSubId = ""+activeObjective.getObjectiveID();
      main.getLogManager().debug("Level: %s", activeObjective.getLevel());
      for(int i = 0; i < activeObjective.getLevel(); i++){
        if(lastActiveObjective.getActiveObjectiveHolder() instanceof final ActiveObjective parentActiveObjective){
          lastActiveObjective = parentActiveObjective;
          counterWithSubId = lastActiveObjective.getObjectiveID() + "."+counterWithSubId;
          main.getLogManager().debug("  counterWithSubId now1: %s", counterWithSubId);

        }else if(lastActiveObjective.getActiveObjectiveHolder() instanceof final ActiveQuest activeQuest){
          counterWithSubId = activeQuest.getQuestIdentifier() + "."+counterWithSubId;
          main.getLogManager().debug("  counterWithSubId now2: %s", counterWithSubId);
          break;
        }
      }
      main.getLogManager().debug("counterWithSubId: %s", counterWithSubId);

      /*if(counterWithSubId.endsWith(".")){
        counterWithSubId = counterWithSubId.substring(0, counterWithSubId.length()-1);
      }*/
      questName = counterWithSubId;

    }else {
      main.getLogManager().warn("Skipped loading of active objectives because the type of the ActiveObjectiveHolder (%s) was not found!", activeObjectiveHolder);
      return;
    }

    main.getLogManager().debug("Loading active objectives for quest/objective holder name <highlight>%s</highlight>. ActiveObjectiveHolder: <highlight2>%s</highlight2>", questName, activeObjectiveHolder);

    activeQuestObjectivesPS.setString(3, questName);

    try (final ResultSet activeQuestObjectiveResults = activeQuestObjectivesPS.executeQuery()){
      final ArrayList<ActiveObjective> activeObjectivesWithSubObjectives = new ArrayList<>();

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
          main.getLogManager().debug("  Active objective count (.next() for %s): %s", objectiveID, activeObjectiveHolder.getActiveObjectives().size());
          for (final ActiveObjective activeObjective : activeObjectiveHolder.getActiveObjectives()) {
            if (activeObjective.getObjective().getClass()
                    == main.getObjectiveManager().getObjectiveClass(objectiveTypeString)
                    && activeObjective.getObjectiveID() == objectiveID) {
              main.getLogManager().debug("  >Handling active objective <highlight>%s</highlight> (ID: %s) of holder <highlight2>%s</highlight2>", activeObjective.getObjective().getIdentifier(), activeObjective.getObjectiveID(), activeObjectiveHolder.getObjectiveHolder().getIdentifier());
              main.getLogManager().debug("  Has been completed: %s, currentProgress: %s, progressNeeded: %s", hasBeenCompleted, currentProgress, progressNeeded);
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
              if(!activeObjective.getActiveObjectives().isEmpty()){
                main.getLogManager().debug("    Active objective %s has %s more activeobjectives!", activeObjective.getObjective().getIdentifier(), activeObjective.getActiveObjectives().size());
                activeObjectivesWithSubObjectives.add(activeObjective);
              }
            }else{
              main.getLogManager().debug("  >Skipping active objective <highlight>%s</highlight> (ID: %s) of holder <highlight2>%s</highlight2>", activeObjective.getObjective().getIdentifier(), activeObjective.getObjectiveID(), activeObjectiveHolder.getObjectiveHolder().getIdentifier());

            }
          }
          activeObjectiveHolder.removeCompletedObjectives(false);



        } else {
          main.getLogManager()
                  .warn(
                          "ERROR: ObjectiveType for the Quest <highlight>"
                                  + activeObjectiveHolder.getObjectiveHolder().getIdentifier()
                                  + "</highlight> could not be loaded from database");
        }
      }

      // Update all active objectives to see if they are unlocked
      for (final ActiveObjective activeObjectiveToCheckForIfUnlocked :
              activeObjectiveHolder.getActiveObjectives()) {
        activeObjectiveToCheckForIfUnlocked.updateUnlocked(false, true);
      }

      for(final ActiveObjective activeObjectiveWithSubObjectives : activeObjectivesWithSubObjectives){
        main.getLogManager().debug("Loading active objective with sub-objectives...");
        handleLoadingOfActiveObjectives(activeQuestObjectivesPS, activeObjectiveWithSubObjectives);
        main.getLogManager().debug("    Done loading sub-aO's");

        activeObjectiveWithSubObjectives.removeCompletedObjectives(false);
      }
    }


  }




  private void savePlayerDataInternal(final List<QuestPlayer> questPlayers) {
    try (Connection connection = main.getDataManager().getConnection();
         final PreparedStatement deleteFromQuestPlayerProfileDataPS = connection.prepareStatement("""
            DELETE FROM QuestPlayerProfileData WHERE PlayerUUID = ?;
         """);

         final PreparedStatement insertIntoQuestPlayerProfileDataPS = connection.prepareStatement("""
            INSERT INTO QuestPlayerProfileData (PlayerUUID, CurrentProfile) VALUES (?, ?);
          """);

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
         """);

         final PreparedStatement deleteFromFailedQuestsPS = connection.prepareStatement("""
            DELETE FROM FailedQuests WHERE PlayerUUID = ? AND Profile = ?;
         """);
        final PreparedStatement insertIntoFailedQuestsPS = connection.prepareStatement("""
            INSERT INTO FailedQuests (QuestName, PlayerUUID, TimeFailed, Profile) VALUES (?, ?, ?, ?);
         """)
    ) {
      for (final QuestPlayer questPlayer : questPlayers) {
        final long questPoints = questPlayer.getQuestPoints();
        final UUID questPlayerUUID = questPlayer.getUniqueId();
        final String profile = questPlayer.getProfile();

        //Current Profile
        deleteFromQuestPlayerProfileDataPS.setString(1, questPlayerUUID.toString());
        deleteFromQuestPlayerProfileDataPS.executeUpdate();

        insertIntoQuestPlayerProfileDataPS.setString(1, questPlayerUUID.toString());
        final QuestPlayer activeQuestPlayer = activeQuestPlayersAndUUIDs.get(questPlayerUUID);
        insertIntoQuestPlayerProfileDataPS.setString(2, activeQuestPlayer != null ? activeQuestPlayer.getProfile() : "default");
        insertIntoQuestPlayerProfileDataPS.executeUpdate();

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
            handleSavingOfActiveObjectives(insertIntoActiveObjectivesPS, activeObjective, questPlayerUUID, profile);
          }
          // Active Objectives from completed Objective list
          for (final ActiveObjective completedObjective : activeQuest.getCompletedObjectives()) {
            handleSavingOfCompletedActiveObjectives(insertIntoActiveObjectivesPS, completedObjective, questPlayerUUID, profile);
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

        // Failed Quests
        deleteFromFailedQuestsPS.setString(1, questPlayerUUID.toString());
        deleteFromFailedQuestsPS.setString(2, profile);
        deleteFromFailedQuestsPS.executeUpdate();

        for (final FailedQuest failedQuest : questPlayer.getFailedQuests()) {
          insertIntoFailedQuestsPS.setString(1, failedQuest.getQuest().getIdentifier());
          insertIntoFailedQuestsPS.setString(2, questPlayerUUID.toString());
          insertIntoFailedQuestsPS.setLong(3, failedQuest.getTimeFailed());
          insertIntoFailedQuestsPS.setString(4, profile);
          insertIntoFailedQuestsPS.executeUpdate();
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

  private void handleSavingOfActiveObjectives(final PreparedStatement insertIntoActiveObjectivesPS, final ActiveObjective activeObjective, final UUID questPlayerUUID, final String profile) throws SQLException {
    insertIntoActiveObjectivesPS.setString(1, main.getObjectiveManager().getObjectiveType(activeObjective.getObjective().getClass()));

    ActiveObjective lastActiveObjective = activeObjective;
    String counterWithSubId = "";
    for(int i = 0; i < activeObjective.getLevel(); i++){
      if(lastActiveObjective.getActiveObjectiveHolder() instanceof final ActiveObjective parentActiveObjective){
        lastActiveObjective = parentActiveObjective;
        counterWithSubId = lastActiveObjective.getObjectiveID() + "."+counterWithSubId;
      }else if(lastActiveObjective.getActiveObjectiveHolder() instanceof final ActiveQuest activeQuest){
        counterWithSubId = activeQuest.getQuestIdentifier() + "."+counterWithSubId;
        break;
      }
    }
    if(counterWithSubId.endsWith(".")){
      counterWithSubId = counterWithSubId.substring(0, counterWithSubId.length()-1);
    }

    insertIntoActiveObjectivesPS.setString(2, /*activeObjective.getActiveObjectiveHolder().getObjectiveHolder().getIdentifier()*/counterWithSubId);
    insertIntoActiveObjectivesPS.setString(3, questPlayerUUID.toString());
    insertIntoActiveObjectivesPS.setDouble(4, activeObjective.getCurrentProgress());
    insertIntoActiveObjectivesPS.setInt(5, activeObjective.getObjectiveID());
    insertIntoActiveObjectivesPS.setBoolean(6, activeObjective.hasBeenCompleted());
    insertIntoActiveObjectivesPS.setDouble(7, activeObjective.getProgressNeeded());
    insertIntoActiveObjectivesPS.setString(8, profile);
    insertIntoActiveObjectivesPS.executeUpdate();

    if(!activeObjective.getActiveObjectives().isEmpty()){
      //Handle active sub-objectives here
      for(final ActiveObjective subActiveObjective : activeObjective.getActiveObjectives()){
        handleSavingOfActiveObjectives(insertIntoActiveObjectivesPS, subActiveObjective, questPlayerUUID, profile);
      }
    }
  }

  private void handleSavingOfCompletedActiveObjectives(final PreparedStatement insertIntoActiveObjectivesPS, final ActiveObjective completedObjective, final UUID questPlayerUUID, final String profile) throws SQLException {
    insertIntoActiveObjectivesPS.setString(1, main.getObjectiveManager().getObjectiveType(completedObjective.getObjective().getClass()));

    ActiveObjective lastActiveObjective = completedObjective;
    String counterWithSubId = "";
    for(int i = 0; i < completedObjective.getLevel(); i++){
      if(lastActiveObjective.getActiveObjectiveHolder() instanceof final ActiveObjective parentActiveObjective){
        lastActiveObjective = parentActiveObjective;
        counterWithSubId = lastActiveObjective.getObjectiveID() + "."+counterWithSubId;
      }else if(lastActiveObjective.getActiveObjectiveHolder() instanceof final ActiveQuest activeQuest){
        counterWithSubId = activeQuest.getQuestIdentifier() + "."+counterWithSubId;
        break;
      }
    }
    if(counterWithSubId.endsWith(".")){
      counterWithSubId = counterWithSubId.substring(0, counterWithSubId.length()-1);
    }

    insertIntoActiveObjectivesPS.setString(2, /*completedObjective.getActiveObjectiveHolder().getObjectiveHolder().getIdentifier()*/counterWithSubId);
    insertIntoActiveObjectivesPS.setString(3, questPlayerUUID.toString());
    insertIntoActiveObjectivesPS.setDouble(4, completedObjective.getCurrentProgress());
    insertIntoActiveObjectivesPS.setInt(5, completedObjective.getObjectiveID());
    insertIntoActiveObjectivesPS.setBoolean(6,  completedObjective.hasBeenCompleted());
    insertIntoActiveObjectivesPS.setDouble(7, completedObjective.getProgressNeeded());
    insertIntoActiveObjectivesPS.setString(8, profile);
    insertIntoActiveObjectivesPS.executeUpdate();

    if(!completedObjective.getCompletedObjectives().isEmpty()){
      //Handle active sub-objectives here
      for(final ActiveObjective subCompletedActiveObjective : completedObjective.getCompletedObjectives()){
        handleSavingOfCompletedActiveObjectives(insertIntoActiveObjectivesPS, subCompletedActiveObjective, questPlayerUUID, profile);
      }
    }
  }


}
