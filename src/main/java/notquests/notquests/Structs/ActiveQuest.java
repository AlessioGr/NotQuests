package notquests.notquests.Structs;


import notquests.notquests.NotQuests;
import notquests.notquests.Structs.Objectives.EscortNPCObjective;
import notquests.notquests.Structs.Objectives.Objective;
import notquests.notquests.Structs.Triggers.ActiveTrigger;
import notquests.notquests.Structs.Triggers.Trigger;
import notquests.notquests.Structs.Triggers.TriggerTypes.TriggerType;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.UUID;

/**
 * This is a special object for active quests. Apart from the Quest itself, it stores additional objects to track the quest progress.
 * This includes the active objectives and completed objectives, as well as triggers and the quest player who accepted the Quest.
 * <p>
 * All this information is saved in the Database, so the Player can continue from where they left off if the server or the plugin
 * restarts.
 *
 * @author Alessio Gravili
 */
public class ActiveQuest {
    private final NotQuests main;

    private final Quest quest;

    private final ArrayList<ActiveObjective> activeObjectives;
    private final ArrayList<ActiveObjective> completedObjectives;
    private final ArrayList<ActiveObjective> toRemove;
    private final ArrayList<ActiveTrigger> activeTriggers;

    private final QuestPlayer questPlayer;

    private CitizensHandler citizensHandler;


    public ActiveQuest(NotQuests main, Quest quest, QuestPlayer questPlayer) {
        this.main = main;
        this.quest = quest;
        this.questPlayer = questPlayer;
        activeObjectives = new ArrayList<>();
        toRemove = new ArrayList<>();
        completedObjectives = new ArrayList<>();
        activeTriggers = new ArrayList<>();

        if (main.isCitizensEnabled()) {
            citizensHandler = new CitizensHandler(main);
        }


        int triggerID = 1;
        for (final Trigger trigger : quest.getTriggers()) {
            ActiveTrigger activeTrigger = new ActiveTrigger(triggerID, trigger, this);
            activeTriggers.add(activeTrigger);
            triggerID++;
        }

        int objectiveID = 1;
        for (final Objective objective : quest.getObjectives()) {
            ActiveObjective activeObjective = new ActiveObjective(main, objectiveID, objective, this);
            activeObjectives.add(activeObjective);
            objectiveID++;
        }


    }

    public final Quest getQuest() {
        return quest;
    }

    public final ArrayList<ActiveTrigger> getActiveTriggers() {
        return activeTriggers;
    }

    public final ArrayList<ActiveObjective> getActiveObjectives() {
        return activeObjectives;
    }

    public final ArrayList<ActiveObjective> getCompletedObjectives() {
        return completedObjectives;
    }

    /*public void updateQuestStatus(){

        for(ActiveObjective activeObjective : activeObjectives){
            if(activeObjective.isCompleted()){
                toRemove.add(activeObjective);
                questPlayer.sendMessage("§aYou have successfully completed the objective §e" + activeObjective.getObjective().getObjectiveType() + "§a for quest §b" + quest.getQuestName() + "§a!");
            }
        }
        activeObjectives.removeAll(toRemove);
        completedObjectives.addAll(toRemove);
        toRemove.clear();

        if(activeObjectives.size() == 0){
            setCompleted();

        }
    }*/

    public final boolean isCompleted() {
        return activeObjectives.size() == 0;
    }

  /*  public void setCompleted(){
        completed = true;
        UUID playerUUID = questPlayer.getUUID();
        Player player = Bukkit.getPlayer(playerUUID);
        if(player != null) {
            if(questPlayer.getActiveQuests().size() > 0){
                for(ActiveQuest activeQuest : questPlayer.getActiveQuests()){
                    for(ActiveObjective activeObjective : activeQuest.getActiveObjectives()){
                        if(activeObjective.getObjective() instanceof OtherQuestObjective){
                            if( ((OtherQuestObjective) activeObjective.getObjective()).getOtherQuest().equals(quest) ){
                                activeObjective.addProgress(1);
                            }
                        }
                    }
                }
                questPlayer.updateOtherQuestObjectiveQuestStatus(quest);
            }
        }
    }*/

    public final QuestPlayer getQuestPlayer() {
        return questPlayer;
    }

    //For Citizens NPCs
    public void notifyActiveObjectiveCompleted(final ActiveObjective activeObjective, final boolean silent, final int NPCID) {
        if (activeObjective.isCompleted(NPCID)) {
            for (final ActiveTrigger activeTrigger : getActiveTriggers()) {
                if (activeTrigger.getTrigger().getTriggerType() == TriggerType.COMPLETE) { //Complete the quest
                    if (activeTrigger.getTrigger().getApplyOn() >= 1) { //Objective and not Quest
                        if (activeObjective.getObjectiveID() == activeTrigger.getTrigger().getApplyOn()) {

                            if (activeTrigger.getTrigger().getWorldName().equalsIgnoreCase("ALL")) {
                                activeTrigger.addAndCheckTrigger(this);
                            } else {
                                final Player player = Bukkit.getPlayer(getQuestPlayer().getUUID());
                                if (player != null && player.getWorld().getName().equalsIgnoreCase(activeTrigger.getTrigger().getWorldName())) {
                                    activeTrigger.addAndCheckTrigger(this);
                                }
                            }

                        }
                    }
                }
            }

            toRemove.add(activeObjective);
            if (!silent) {
                questPlayer.sendMessage("§aYou have successfully completed the objective §e" + activeObjective.getObjective().getObjectiveType() + "§a for quest §b" + quest.getQuestName() + "§a!");
                final Player player = Bukkit.getPlayer(questPlayer.getUUID());
                if (player != null) {
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, SoundCategory.MASTER, 75, 1.4f);

                }
            }
        }
    }

    //For Armor Stands
    public void notifyActiveObjectiveCompleted(final ActiveObjective activeObjective, final boolean silent, final UUID armorStandUUID) {
        if (activeObjective.isCompleted(armorStandUUID)) {
            for (final ActiveTrigger activeTrigger : getActiveTriggers()) {
                if (activeTrigger.getTrigger().getTriggerType() == TriggerType.COMPLETE) { //Complete the quest
                    if (activeTrigger.getTrigger().getApplyOn() >= 1) { //Objective and not Quest
                        if (activeObjective.getObjectiveID() == activeTrigger.getTrigger().getApplyOn()) {

                            if (activeTrigger.getTrigger().getWorldName().equalsIgnoreCase("ALL")) {
                                activeTrigger.addAndCheckTrigger(this);
                            } else {
                                final Player player = Bukkit.getPlayer(getQuestPlayer().getUUID());
                                if (player != null && player.getWorld().getName().equalsIgnoreCase(activeTrigger.getTrigger().getWorldName())) {
                                    activeTrigger.addAndCheckTrigger(this);
                                }
                            }

                        }
                    }
                }
            }

            toRemove.add(activeObjective);
            if (!silent) {
                questPlayer.sendMessage("§aYou have successfully completed the objective §e" + activeObjective.getObjective().getObjectiveType() + "§a for quest §b" + quest.getQuestName() + "§a!");
                final Player player = Bukkit.getPlayer(questPlayer.getUUID());
                if (player != null) {
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, SoundCategory.MASTER, 75, 1.4f);

                }
            }
        }
    }


    public void removeCompletedObjectives(final boolean notifyPlayer) {

        activeObjectives.removeAll(toRemove);
        completedObjectives.addAll(toRemove);
        toRemove.clear();


        for (final ActiveObjective activeObjectiveToCheckForIfUnlocked : activeObjectives) {
            activeObjectiveToCheckForIfUnlocked.updateUnlocked(notifyPlayer, true);
        }

        if (activeObjectives.size() == 0) {
            questPlayer.notifyActiveQuestCompleted(this);
        }
    }

    public void fail() {


        if (questPlayer.getActiveQuests().size() > 0) {
            final ArrayList<ActiveQuest> activeQuestsCopy = new ArrayList<>(questPlayer.getActiveQuests());

            //So, this is not a for loop anymore but instead it just uses the current activequest. That's because we had this error: "When miner (Gold Madness) & 3rdlife 2, this also fails 3rdlife2 if I fail miner (Gold Madness) for some reason"
            //So, this fixes it. Because the Trigger type FAIL should only apply for the current quest anyways. Other Active Quests dont matter for the FAIL trigger for the current quest
            //TODO: Add an option in the todo trigger creation to make it apply to a different quest (enter different quest name). If not, use current quest.

            // for(final ActiveQuest activeQuest : activeQuestsCopy){
            final ActiveQuest activeQuest = this;
            for (final ActiveTrigger activeTrigger : activeQuest.getActiveTriggers()) {
                if (activeTrigger.getTrigger().getTriggerType() == TriggerType.FAIL) {
                    if (activeTrigger.getTrigger().getApplyOn() == 0) { //Quest and not Objective
                        //System.out.println("§eAAA");

                        if (activeTrigger.getTrigger().getWorldName().equalsIgnoreCase("ALL")) {
                            activeTrigger.addAndCheckTrigger(activeQuest);
                            //System.out.println("§eAAA2");

                        } else {
                            final Player player = Bukkit.getPlayer(getQuestPlayer().getUUID());
                            if (player != null && player.getWorld().getName().equalsIgnoreCase(activeTrigger.getTrigger().getWorldName())) {
                                activeTrigger.addAndCheckTrigger(activeQuest);
                                //System.out.println("§eAAA3");

                            }
                        }


                    } else if (activeTrigger.getTrigger().getApplyOn() >= 1) { //Objective and not Quest

                        final ActiveObjective activeObjective = activeQuest.getActiveObjectiveFromID(activeTrigger.getTrigger().getApplyOn());
                        if (activeObjective != null && activeObjective.isUnlocked()) {


                            if (activeTrigger.getTrigger().getWorldName().equalsIgnoreCase("ALL")) {
                                activeTrigger.addAndCheckTrigger(activeQuest);
                            } else {
                                final Player player = Bukkit.getPlayer(getQuestPlayer().getUUID());
                                if (player != null && player.getWorld().getName().equalsIgnoreCase(activeTrigger.getTrigger().getWorldName())) {
                                    activeTrigger.addAndCheckTrigger(activeQuest);
                                }
                            }

                        }
                    }

                }
            }


            //  }
        }

        questPlayer.sendMessage(main.getLanguageManager().getString("chat.quest-failed").replaceAll("%QUESTNAME%", getQuest().getQuestName()));


        for (final ActiveObjective activeObjective : getActiveObjectives()) {
            if (activeObjective.getObjective() instanceof EscortNPCObjective) {
                if (main.isCitizensEnabled() && citizensHandler != null) {
                    citizensHandler.handleEscortObjective(activeObjective);


                }

            }
        }

    }

    public void updateObjectivesUnlocked(final boolean sendUpdateObjectivesUnlocked, final boolean triggerAcceptQuestTrigger) {
        for (final ActiveObjective activeObjective : activeObjectives) {
            activeObjective.updateUnlocked(sendUpdateObjectivesUnlocked, triggerAcceptQuestTrigger);
        }
    }

    public final ActiveObjective getActiveObjectiveFromID(final int objectiveID) {
        for (final ActiveObjective objective : activeObjectives) {
            if (objective.getObjectiveID() == objectiveID) {
                return objective;
            }
        }
        return null;
    }

    public final CitizensHandler getCitizensHandler() {
        return citizensHandler;
    }
}
