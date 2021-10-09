/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021 Alessio Gravili
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

package notquests.notquests.Managers;

import notquests.notquests.NotQuests;
import notquests.notquests.Structs.*;
import notquests.notquests.Structs.Objectives.ObjectiveType;
import notquests.notquests.Structs.Triggers.ActiveTrigger;
import notquests.notquests.Structs.Triggers.TriggerTypes.TriggerType;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;

public class QuestPlayerManager {
    private final NotQuests main;

    private final ArrayList<QuestPlayer> questPlayers;

    private final HashMap<UUID, QuestPlayer> questPlayersAndUUIDs;

    public QuestPlayerManager(NotQuests notQuests) {
        this.main = notQuests;
        questPlayers = new ArrayList<>();
        questPlayersAndUUIDs = new HashMap<>();
    }

    public void loadPlayerData() {
        questPlayers.clear();
        questPlayersAndUUIDs.clear();


        //Quest Players
        try {
            ResultSet result = main.getDataManager().getDatabaseStatement().executeQuery("SELECT * FROM QuestPlayerData");
            while (result.next()) {
                final UUID uuid = UUID.fromString(result.getString("PlayerUUID"));
                createQuestPlayer(uuid);
                final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(uuid);

                final long questPoints = result.getLong("QuestPoints");
                main.getLogManager().log(Level.INFO, "Loaded player with uuid <AQUA>" + uuid + "</AQUA> and questPoints: " + questPoints);

                if (questPlayer != null) {
                    //QuestPoints
                    questPlayer.setQuestPoints(questPoints, false);


                } else {
                    main.getLogManager().log(Level.SEVERE, "ERROR: QuestPlayer with the UUID <AQUA>" + uuid + "</AQUA> could not be loaded from database");

                }

            }
            result.close();
        } catch (SQLException sqlException) {
            sqlException.printStackTrace();
        }

        //Active Quests
        final ArrayList<ActiveQuest> activeQuests = new ArrayList<>();
        try {

            for (final QuestPlayer questPlayer : questPlayers) {
                activeQuests.clear();


                //Completed Quests
                ResultSet completedQuestsResults = main.getDataManager().getDatabaseStatement().executeQuery("SELECT QuestName, TimeCompleted FROM CompletedQuests WHERE PlayerUUID = '" + questPlayer.getUUID().toString() + "';");
                while (completedQuestsResults.next()) {
                    final String questName = completedQuestsResults.getString("QuestName");
                    final Quest quest = main.getQuestManager().getQuest(questName);
                    if (quest != null) {
                        final long timeCompleted = completedQuestsResults.getLong("TimeCompleted");
                        if (timeCompleted > 0) {
                            final CompletedQuest completedQuest = new CompletedQuest(quest, questPlayer, timeCompleted);
                            questPlayer.addCompletedQuest(completedQuest);


                        } else {
                            main.getLogManager().log(Level.WARNING, "§9NotQuests > §cERROR: TimeCompleted from Quest with name §b" + questName + " §ccould not be loaded from database (requested for loading completed Quests)");


                        }

                    } else {
                        main.getLogManager().log(Level.WARNING, "§9NotQuests > §cERROR: Quest with name §b" + questName + " §ccould not be loaded from database (requested for loading completed Quests)");

                    }
                }
                completedQuestsResults.close();


                //Active Quests
                ResultSet activeQuestsResults = main.getDataManager().getDatabaseStatement().executeQuery("SELECT QuestName FROM ActiveQuests WHERE PlayerUUID = '" + questPlayer.getUUID() + "';");
                while (activeQuestsResults.next()) {
                    final String questName = activeQuestsResults.getString("QuestName");
                    final Quest quest = main.getQuestManager().getQuest(questName);
                    if (quest != null) {
                        final ActiveQuest activeQuest = new ActiveQuest(main, quest, questPlayer);
                        activeQuests.add(activeQuest);
                        questPlayer.forceAddActiveQuest(activeQuest, false); //Run begin/accept trigger when plugin reloads if true

                    } else {
                        main.getLogManager().log(Level.WARNING, "§9NotQuests > §cERROR: Quest with name §b" + questName + " §ccould not be loaded from database");

                    }
                }
                activeQuestsResults.close();

                for (ActiveQuest activeQuest : activeQuests) {

                    //Active Triggers
                    ResultSet activeQuestTriggerResults = main.getDataManager().getDatabaseStatement().executeQuery("SELECT * FROM ActiveTriggers WHERE PlayerUUID = '" + questPlayer.getUUID() + "' AND QuestName = '" + activeQuest.getQuest().getQuestName() + "';");
                    while (activeQuestTriggerResults.next()) {
                        final String triggerTypeString = activeQuestTriggerResults.getString("TriggerType");
                        final long currentProgress = activeQuestTriggerResults.getLong("currentProgress");


                        if (triggerTypeString != null) {
                            final TriggerType triggerType = TriggerType.valueOf(triggerTypeString);
                            final int triggerID = activeQuestTriggerResults.getInt("TriggerID");


                            for (ActiveTrigger activeTrigger : activeQuest.getActiveTriggers()) {
                                if (activeTrigger.getTrigger().getTriggerType().equals(triggerType) && activeTrigger.getTriggerID() == triggerID) {
                                    activeTrigger.addProgressSilent(currentProgress);
                                }

                            }


                        } else {
                            main.getLogManager().log(Level.WARNING, "ERROR: TriggerType for the Quest §b" + activeQuest.getQuest().getQuestName() + " §ccould not be loaded from database");

                        }
                    }
                    activeQuestTriggerResults.close();


                    //Active Objectives
                    ResultSet activeQuestObjectiveResults = main.getDataManager().getDatabaseStatement().executeQuery("SELECT * FROM ActiveObjectives WHERE PlayerUUID = '" + questPlayer.getUUID() + "' AND QuestName = '" + activeQuest.getQuest().getQuestName() + "';");
                    while (activeQuestObjectiveResults.next()) {
                        final String objectiveTypeString = activeQuestObjectiveResults.getString("ObjectiveType");
                        final long currentProgress = activeQuestObjectiveResults.getLong("currentProgress");
                        final boolean hasBeenCompleted = activeQuestObjectiveResults.getBoolean("HasBeenCompleted");

                        if (objectiveTypeString != null) {
                            final ObjectiveType objectiveType = ObjectiveType.valueOf(objectiveTypeString);
                            final int objectiveID = activeQuestObjectiveResults.getInt("ObjectiveID");


                            //So the active objectives are already there - we just need to fill them with progress data.
                            for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                                if (activeObjective.getObjective().getObjectiveType().equals(objectiveType) && activeObjective.getObjectiveID() == objectiveID) {
                                    //System.out.println("§4§lHAS BEEN COMPLETED: §b" + hasBeenCompleted + " §c- ID: §b" + objectiveID);
                                    activeObjective.setHasBeenCompleted(hasBeenCompleted);
                                    if (activeObjective.getObjective().getCompletionNPCID() == -1) { //Complete automatically
                                        if (activeObjective.getObjective().getCompletionArmorStandUUID() != null) { //Only complete if player has talked to the completion Armor Stand
                                            if (activeObjective.hasBeenCompleted()) {
                                                activeObjective.addProgressSilent(currentProgress, activeObjective.getObjective().getCompletionArmorStandUUID());

                                            } else {
                                                activeObjective.addProgressSilent(currentProgress, null);

                                            }
                                        } else {
                                            activeObjective.addProgressSilent(currentProgress, -1);
                                        }

                                    } else { //Only complete if player has talked to the completion NPC
                                        //System.out.println("§4§lTHIS IS WHAT I WANNA SEE FIRST");
                                        if (activeObjective.hasBeenCompleted()) {
                                            activeObjective.addProgressSilent(currentProgress, activeObjective.getObjective().getCompletionNPCID());

                                        } else {
                                            //System.out.println("§4§lTHIS IS WHAT I WANNA SEE TOO");
                                            activeObjective.addProgressSilent(currentProgress, -1);

                                        }
                                    }
                                }

                            }
                            activeQuest.removeCompletedObjectives(false);


                        } else {
                            main.getLogManager().log(Level.WARNING, "ERROR: ObjectiveType for the Quest §b" + activeQuest.getQuest().getQuestName() + " §ccould not be loaded from database");

                        }
                    }

                    //Update all active objectives to see if they are unlocked
                    for (final ActiveObjective activeObjectiveToCheckForIfUnlocked : activeQuest.getActiveObjectives()) {
                        activeObjectiveToCheckForIfUnlocked.updateUnlocked(false, true);
                    }


                    activeQuestObjectiveResults.close();
                }


                questPlayer.removeCompletedQuests();
            }

        } catch (SQLException sqlException) {
            sqlException.printStackTrace();
        }


    }

    public void savePlayerData() {

        //try{
        //if(main.getDataManager().getDatabaseStatement().isClosed()){
        main.getLogManager().log(Level.INFO, "Re-opening database connection...");

        main.getDataManager().refreshDatabaseConnection(false);
        //}
     /*   }catch(SQLException exception){
            exception.printStackTrace();
            System.out.println("caNotQuests > SQL Exception! getDatabaseStatement().isClosed() failed. Trying to re-open database connection...");
            main.getDataManager().refreshDatabaseConnection();
        }*/


        for (QuestPlayer questPlayer : questPlayers) {
            final long questPoints = questPlayer.getQuestPoints();
            final UUID questPlayerUUID = questPlayer.getUUID();
            try {
                //QuestPoints
                main.getDataManager().getDatabaseStatement().executeUpdate("DELETE FROM QuestPlayerData WHERE PlayerUUID = '" + questPlayerUUID.toString() + "';");
                main.getDataManager().getDatabaseStatement().executeUpdate("INSERT INTO QuestPlayerData (PlayerUUID, QuestPoints) VALUES ('" + questPlayerUUID + "', " + questPoints + ");");

                //Active Quests
                main.getDataManager().getDatabaseStatement().executeUpdate("DELETE FROM ActiveQuests WHERE PlayerUUID = '" + questPlayerUUID + "';");
                main.getDataManager().getDatabaseStatement().executeUpdate("DELETE FROM ActiveObjectives WHERE PlayerUUID = '" + questPlayerUUID + "';");
                for (ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                    main.getDataManager().getDatabaseStatement().executeUpdate("INSERT INTO ActiveQuests (QuestName, PlayerUUID) VALUES ('" + activeQuest.getQuest().getQuestName() + "', '" + questPlayerUUID + "');");
                    //Active Triggers
                    for (ActiveTrigger activeTrigger : activeQuest.getActiveTriggers()) {
                        main.getDataManager().getDatabaseStatement().executeUpdate("INSERT INTO ActiveTriggers (TriggerType, QuestName, PlayerUUID, CurrentProgress, TriggerID) VALUES ('" + activeTrigger.getTrigger().getTriggerType().toString() + "', '" + activeTrigger.getActiveQuest().getQuest().getQuestName() + "', '" + questPlayerUUID + "', " + activeTrigger.getCurrentProgress() + ", " + activeTrigger.getTriggerID() + ");");
                    }

                    //Active Objectives
                    for (ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                        main.getDataManager().getDatabaseStatement().executeUpdate("INSERT INTO ActiveObjectives (ObjectiveType, QuestName, PlayerUUID, CurrentProgress, ObjectiveID, HasBeenCompleted) VALUES ('" + activeObjective.getObjective().getObjectiveType().toString() + "', '" + activeObjective.getActiveQuest().getQuest().getQuestName() + "', '" + questPlayerUUID + "', " + activeObjective.getCurrentProgress() + ", " + activeObjective.getObjectiveID() + ", " + activeObjective.hasBeenCompleted() + ");");
                    }
                    //Active Objectives from completed Objective list
                    for (ActiveObjective completedObjective : activeQuest.getCompletedObjectives()) {
                        main.getDataManager().getDatabaseStatement().executeUpdate("INSERT INTO ActiveObjectives (ObjectiveType, QuestName, PlayerUUID, CurrentProgress, ObjectiveID, HasBeenCompleted) VALUES ('" + completedObjective.getObjective().getObjectiveType().toString() + "', '" + completedObjective.getActiveQuest().getQuest().getQuestName() + "', '" + questPlayerUUID + "', " + completedObjective.getCurrentProgress() + ", " + completedObjective.getObjectiveID() + ", " + completedObjective.hasBeenCompleted() + ");");
                    }
                }


                //Completed Quests
                main.getDataManager().getDatabaseStatement().executeUpdate("DELETE FROM CompletedQuests WHERE PlayerUUID = '" + questPlayerUUID + "';");
                for (CompletedQuest completedQuest : questPlayer.getCompletedQuests()) {
                    main.getDataManager().getDatabaseStatement().executeUpdate("INSERT INTO CompletedQuests (QuestName, PlayerUUID, TimeCompleted) VALUES ('" + completedQuest.getQuest().getQuestName() + "', '" + questPlayerUUID + "', " + completedQuest.getTimeCompleted() + ");");
                }


            } catch (SQLException sqlException) {
                main.getLogManager().log(Level.WARNING, "There was an error saving the playerdata of player with UUID <AQUA>" + questPlayer.getUUID() + "</AQUA>! Stacktrace:");

                sqlException.printStackTrace();
            }

        }
        main.getLogManager().log(Level.INFO, "PlayerData saved");

    }

    public final QuestPlayer getQuestPlayer(final UUID uuid) {
        return questPlayersAndUUIDs.get(uuid);
    }

    public final ArrayList<QuestPlayer> getQuestPlayers() {
        return questPlayers;
    }

    public String acceptQuest(final Player player, final Quest quest, final boolean triggerAcceptQuestTrigger, final boolean sendQuestInfo) {
        QuestPlayer questPlayer = getQuestPlayer(player.getUniqueId());
        if (questPlayer == null) {
            questPlayer = new QuestPlayer(main, player.getUniqueId());
            questPlayers.add(questPlayer);
            questPlayersAndUUIDs.put(player.getUniqueId(), questPlayer);
        }
        final ActiveQuest newActiveQuest = new ActiveQuest(main, quest, questPlayer);

        return questPlayer.addActiveQuest(newActiveQuest, triggerAcceptQuestTrigger, sendQuestInfo);
    }

    public String createQuestPlayer(UUID uuid) {
        QuestPlayer questPlayer = getQuestPlayer(uuid);
        if (questPlayer == null) {
            questPlayer = new QuestPlayer(main, uuid);
            questPlayers.add(questPlayer);
            questPlayersAndUUIDs.put(uuid, questPlayer);
            return "§aQuest player with uuid §b" + uuid + " §ahas been created successfully!";

        } else {
            return "§cQuest player already exists.";
        }
    }

    public String forceAcceptQuest(UUID uuid, Quest quest) { //Ignores max amount limit, cooldown and requirements
        QuestPlayer questPlayer = getQuestPlayer(uuid);
        if (questPlayer == null) {
            questPlayer = new QuestPlayer(main, uuid);
            questPlayers.add(questPlayer);
            questPlayersAndUUIDs.put(uuid, questPlayer);
        }
        return questPlayer.forceAddActiveQuest(new ActiveQuest(main, quest, questPlayer), true);
    }
}
