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

package notquests.notquests.Structs;


import notquests.notquests.NotQuests;
import notquests.notquests.Structs.Objectives.OtherQuestObjective;
import notquests.notquests.Structs.Requirements.*;
import notquests.notquests.Structs.Rewards.Reward;
import notquests.notquests.Structs.Triggers.ActiveTrigger;
import notquests.notquests.Structs.Triggers.TriggerTypes.TriggerType;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * The QuestPlayer Object is initialized for every player, once they join the server - loading its data from the database.
 * It contains all kinds of player data for that player, like active quests, completed quests and quest points.
 * Completed Quests are saved too, for things like Quest History (Future feature), or handling the maxAccepts per quest /
 * the quest cooldown.
 *
 * @author Alessio Gravili
 */
public class QuestPlayer {
    private final NotQuests main;

    private final UUID uuid;


    private final ArrayList<ActiveQuest> activeQuests, activeQuestsCopy;
    private final ArrayList<ActiveQuest> questsToComplete;
    private final ArrayList<ActiveQuest> questsToRemove;
    private final ArrayList<CompletedQuest> completedQuests; //has to accept multiple entries of the same value
    private long questPoints;

    public QuestPlayer(NotQuests main, UUID uuid) {
        this.main = main;
        this.uuid = uuid;
        activeQuests = new ArrayList<>();
        activeQuestsCopy = new ArrayList<>();
        questsToComplete = new ArrayList<>();
        questsToRemove = new ArrayList<>();
        completedQuests = new ArrayList<>();
    }

    public String addActiveQuest(final ActiveQuest quest, final boolean triggerAcceptQuestTrigger, final boolean sendQuestInfo) {
        //Configuration Option: general.max-active-quests-per-player
        if(main.getDataManager().getConfiguration().getMaxActiveQuestsPerPlayer() != -1 && activeQuests.size() >= main.getDataManager().getConfiguration().getMaxActiveQuestsPerPlayer()){
            return "§cYou can not accept more than §b" + main.getDataManager().getConfiguration().getMaxActiveQuestsPerPlayer() + " §cQuests.";
        }

        for (ActiveQuest activeQuest : activeQuests) {
            if (activeQuest.getQuest().equals(quest.getQuest())) {
                return main.getLanguageManager().getString("chat.quest-already-accepted");
            }
        }
        int completedAmount = 0;

        long mostRecentAcceptTime = 0;
        for (CompletedQuest completedQuest : completedQuests) {
            if (completedQuest.getQuest().equals(quest.getQuest())) {
                completedAmount += 1;
                if (completedQuest.getTimeCompleted() > mostRecentAcceptTime) {
                    mostRecentAcceptTime = completedQuest.getTimeCompleted();
                }
            }
        }

        final long acceptTimeDifference = System.currentTimeMillis() - mostRecentAcceptTime;
        final long acceptTimeDifferenceMinutes = TimeUnit.MILLISECONDS.toMinutes(acceptTimeDifference);


        final long timeToWaitInMinutes = quest.getQuest().getAcceptCooldown() - acceptTimeDifferenceMinutes;
        final double timeToWaitInHours = Math.round((timeToWaitInMinutes / 60f) * 10) / 10.0;
        final double timeToWaitInDays = Math.round((timeToWaitInHours / 24f) * 10) / 10.0;


        //Max Accepts:
        if (quest.getQuest().getMaxAccepts() <= -1 || completedAmount < quest.getQuest().getMaxAccepts()) {
            //Cooldown:
            if (acceptTimeDifferenceMinutes >= quest.getQuest().getAcceptCooldown()) {

                //Requirements
                StringBuilder requirementsStillNeeded = new StringBuilder();
                long questPointsToDeduct = 0;
                long moneyToDeduct = 0;

                for (Requirement requirement : quest.getQuest().getRequirements()) {
                    final long progressNeeded = requirement.getProgressNeeded();


                    if (requirement instanceof final OtherQuestRequirement otherQuestRequirement) {
                        final Quest otherQuest = otherQuestRequirement.getOtherQuest();

                        int otherQuestCompletedAmount = 0;

                        for (CompletedQuest completedQuest : completedQuests) {
                            if (completedQuest.getQuest().equals(otherQuest)) {
                                otherQuestCompletedAmount += 1;
                            }
                        }
                        if (otherQuestCompletedAmount < progressNeeded) {
                            requirementsStillNeeded.append("\n§eFinish the following quest: §b").append(otherQuestRequirement.getOtherQuestName()).append(" §7(").append(otherQuestRequirement.getProgressNeeded()).append(" times)").append("\n");
                        }
                    } else if (requirement instanceof final QuestPointsRequirement questPointsRequirement) {
                        final long questPointRequirementAmount = questPointsRequirement.getQuestPointRequirement();
                        final boolean deductQuestPoints = questPointsRequirement.isDeductQuestPoints();

                        if (getQuestPoints() < questPointRequirementAmount) {
                            requirementsStillNeeded.append("\n§eYou need §b").append(questPointRequirementAmount - getQuestPoints()).append(" §emore quest points.");
                        } else {
                            if (deductQuestPoints) {
                                questPointsToDeduct += questPointRequirementAmount;
                            }
                        }
                    } else if (requirement instanceof final MoneyRequirement moneyRequirement) {
                        final long moneyRequirementAmount = moneyRequirement.getMoneyRequirement();
                        final boolean deductMoney = moneyRequirement.isDeductMoney();
                        final Player player = Bukkit.getPlayer(getUUID());
                        if (player != null) {
                            if (!main.isVaultEnabled() || main.getEconomy() == null) {
                                requirementsStillNeeded.append("\n§eError: The server does not have vault enabled. Please ask the Owner to install Vault for money stuff to work.");
                            } else if (main.getEconomy().getBalance(player, player.getWorld().getName()) < moneyRequirementAmount) {
                                requirementsStillNeeded.append("\n§eYou need §b").append(moneyRequirementAmount - main.getEconomy().getBalance(player, player.getWorld().getName())).append(" §emore money.");
                            } else {
                                if (deductMoney) {
                                    moneyToDeduct += moneyRequirementAmount;
                                }
                            }
                        } else {
                            requirementsStillNeeded.append("\n§eError reading money requirement...");

                        }

                    } else if (requirement instanceof final PermissionRequirement permissionRequirement) {
                        final String requiredPermission = permissionRequirement.getRequiredPermission();

                        final Player player = Bukkit.getPlayer(uuid);
                        if (player != null) {
                            if (!player.hasPermission(requiredPermission)) {
                                requirementsStillNeeded.append("\n§eYou need the following permission: §b").append(requiredPermission).append("§e.");
                            }
                        } else {
                            requirementsStillNeeded.append("\n§eYou need to be online.");

                        }

                    }


                }
                if (!requirementsStillNeeded.toString().equals("")) {

                    return "§cYou do not fulfill all the requirements this quest needs! Requirement still needed:" + requirementsStillNeeded;
                }

                if (moneyToDeduct > 0) {
                    final Player player = Bukkit.getPlayer(getUUID());
                    if (player != null) {
                        if(main.isVaultEnabled()){
                            removeMoney(player, player.getWorld().getName(), moneyToDeduct, true);
                        }else{
                            main.getLogManager().log(Level.WARNING, "§eWarning: Could not deduct money, because Vault was not found. Please install Vault for money stuff to work.");
                            main.getLogManager().log(Level.WARNING, "§cError: Tried to load Economy when Vault is not enabled. Please report this to the plugin author (and I also recommend you installing Vault for money stuff to work)");
                            return "§cError deducting money, because Vault has not been found. Report this to an Admin.";
                        }
                    } else {
                        return "§cError getting player data from your UUID. Report this to an Admin.";
                    }

                }

                if (questPointsToDeduct > 0) {
                    removeQuestPoints(questPointsToDeduct, true);
                }


                finishAddingQuest(quest, triggerAcceptQuestTrigger, false);
                if (sendQuestInfo) {
                    final Player player = Bukkit.getPlayer(uuid);
                    if (player != null) {
                        player.sendMessage(main.getLanguageManager().getString("chat.objectives-label-after-quest-accepting"));
                        main.getQuestManager().sendActiveObjectivesAndProgress(player, quest);

                        if (!quest.getQuest().getQuestDisplayName().equals("")) {
                            player.sendTitle(main.getLanguageManager().getString("titles.quest-accepted.title"), main.getLanguageManager().getString("titles.quest-accepted.subtitle").replaceAll("%QUESTNAME%", quest.getQuest().getQuestDisplayName()), 2, 60, 8);
                        } else {
                            player.sendTitle(main.getLanguageManager().getString("titles.quest-accepted.title"), main.getLanguageManager().getString("titles.quest-accepted.subtitle").replaceAll("%QUESTNAME%", quest.getQuest().getQuestName()), 2, 60, 8);
                        }
                        player.playSound(player.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.MASTER, 100, 2);

                    }

                }


                return "accepted";
            } else {
                if (timeToWaitInMinutes < 60) {
                    return "§cThis quest is on a cooldown! You have to wait another §b" + timeToWaitInMinutes + " minutes §cuntil you can take it again.";
                } else {
                    if (timeToWaitInHours < 24) {
                        if (timeToWaitInHours == 1) {
                            return "§cThis quest is on a cooldown! You have to wait another §b" + timeToWaitInHours + " hour §cuntil you can take it again.";

                        } else {
                            return "§cThis quest is on a cooldown! You have to wait another §b" + timeToWaitInHours + " hours §cuntil you can take it again.";
                        }
                    } else {
                        if (timeToWaitInDays == 1) {
                            return "§cThis quest is on a cooldown! You have to wait another §b" + timeToWaitInDays + " day §cuntil you can take it again.";

                        } else {
                            return "§cThis quest is on a cooldown! You have to wait another §b" + timeToWaitInDays + " days §cuntil you can take it again.";
                        }
                    }


                }
            }

        } else {
            return "§cYou have finished this quests too many times already. You can only accept ot §b" + quest.getQuest().getMaxAccepts() + " §ctimes, but you have already accepted it §b" + completedAmount + " §c times.";
        }


    }


    private void finishAddingQuest(final ActiveQuest activeQuest, boolean triggerAcceptQuestTrigger, final boolean sendUpdateObjectivesUnlocked) {
        activeQuests.add(activeQuest);
        activeQuestsCopy.add(activeQuest);

        if (triggerAcceptQuestTrigger) {
            for (final ActiveTrigger activeTrigger : activeQuest.getActiveTriggers()) {
                if (activeTrigger.getTrigger().getTriggerType() == TriggerType.BEGIN) { //Start the quest
                    if (activeTrigger.getTrigger().getApplyOn() == 0) { //Quest and not objective

                        if (activeTrigger.getTrigger().getWorldName().equalsIgnoreCase("ALL")) {
                            activeTrigger.addAndCheckTrigger(activeQuest);
                        } else {
                            final Player player = Bukkit.getPlayer(getUUID());
                            if (player != null && player.getWorld().getName().equalsIgnoreCase(activeTrigger.getTrigger().getWorldName())) {
                                activeTrigger.addAndCheckTrigger(activeQuest);
                            }
                        }
                    }
                }
            }
        }

        activeQuest.updateObjectivesUnlocked(sendUpdateObjectivesUnlocked, triggerAcceptQuestTrigger);


    }

    public String forceAddActiveQuest(final ActiveQuest quest, boolean triggerAcceptQuestTrigger) { //ignores max amount, cooldown and requirements
        for (ActiveQuest activeQuest : activeQuests) {
            if (activeQuest.getQuest().equals(quest.getQuest())) {
                return main.getLanguageManager().getString("chat.quest-already-accepted");
            }
        }
        finishAddingQuest(quest, triggerAcceptQuestTrigger, false);
        return "§aSuccessfully accepted the quest (Forced).";
    }

    public final UUID getUUID() {
        return uuid;
    }

    public final ArrayList<ActiveQuest> getActiveQuests() {
        return activeQuests;
    }

    /*public void updateQuestStatus(){
        for(ActiveQuest activeQuest : activeQuests){
            activeQuest.updateQuestStatus();
            if(activeQuest.isCompleted()){
                giveReward(activeQuest.getQuest());
                questsToRemove.add(activeQuest);
            }
        }
        activeQuests.removeAll(questsToRemove);
        for(ActiveQuest activeQuest2 : questsToRemove) {
            completedQuests.add(new CompletedQuest(activeQuest2.getQuest(), this));
        }

        questsToRemove.clear();
    }*/

    public void giveReward(Quest quest) {
        for (Reward reward : quest.getRewards()) {
            reward.giveReward(Bukkit.getPlayer(uuid), quest);
        }
        Objects.requireNonNull(Bukkit.getPlayer(uuid)).sendMessage(main.getLanguageManager().getString("chat.quest-completed-and-rewards-given").replaceAll("%QUESTNAME%", quest.getQuestName()));

    }

    public void sendMessage(String message) {
        final Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            player.sendMessage(message);
        }
    }

    public final ArrayList<CompletedQuest> getCompletedQuests() {
        return completedQuests;
    }


    public void forceActiveQuestCompleted(ActiveQuest activeQuest) {
        for (final ActiveTrigger activeTrigger : activeQuest.getActiveTriggers()) {
            if (activeTrigger.getTrigger().getTriggerType() == TriggerType.COMPLETE) { //Complete the quest
                if (activeTrigger.getTrigger().getApplyOn() == 0) { //Quest and not objective

                    if (activeTrigger.getTrigger().getWorldName().equalsIgnoreCase("ALL")) {
                        activeTrigger.addAndCheckTrigger(activeQuest);
                    } else {
                        final Player player = Bukkit.getPlayer(getUUID());
                        if (player != null && player.getWorld().getName().equalsIgnoreCase(activeTrigger.getTrigger().getWorldName())) {
                            activeTrigger.addAndCheckTrigger(activeQuest);
                        }
                    }
                }
            }
        }


        giveReward(activeQuest.getQuest());
        questsToComplete.add(activeQuest);

        final Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            if (!activeQuest.getQuest().getQuestDisplayName().equals("")) {
                player.sendTitle(main.getLanguageManager().getString("titles.quest-completed.title"), main.getLanguageManager().getString("titles.quest-accepted.subtitle").replaceAll("%QUESTNAME%", activeQuest.getQuest().getQuestDisplayName()), 2, 60, 8);
            } else {
                player.sendTitle(main.getLanguageManager().getString("titles.quest-completed.title"), main.getLanguageManager().getString("titles.quest-accepted.subtitle").replaceAll("%QUESTNAME%", activeQuest.getQuest().getQuestName()), 2, 60, 8);
            }
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 100, 40);

        }


        for (ActiveQuest activeQuest2 : activeQuests) {
            for (ActiveObjective objective : activeQuest2.getActiveObjectives()) {
                if (objective.getObjective() instanceof OtherQuestObjective) {
                    if (((OtherQuestObjective) (objective.getObjective())).getOtherQuest().equals(activeQuest.getQuest())) {
                        objective.addProgress(1, -1);
                    }
                }
            }
        }
        removeCompletedQuests();
        //activeQuests.removeAll(questsToComplete);

    }

    public void notifyActiveQuestCompleted(ActiveQuest activeQuest) {
        for (final ActiveTrigger activeTrigger : activeQuest.getActiveTriggers()) {
            if (activeTrigger.getTrigger().getTriggerType() == TriggerType.COMPLETE) { //Complete the quest
                if (activeTrigger.getTrigger().getApplyOn() == 0) { //Quest and not objective

                    if (activeTrigger.getTrigger().getWorldName().equalsIgnoreCase("ALL")) {
                        activeTrigger.addAndCheckTrigger(activeQuest);
                    } else {
                        final Player player = Bukkit.getPlayer(getUUID());
                        if (player != null && player.getWorld().getName().equalsIgnoreCase(activeTrigger.getTrigger().getWorldName())) {
                            activeTrigger.addAndCheckTrigger(activeQuest);
                        }
                    }
                }
            }
        }

        if (activeQuest.isCompleted()) {
            giveReward(activeQuest.getQuest());
            questsToComplete.add(activeQuest);
            final Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                if (!activeQuest.getQuest().getQuestDisplayName().equals("")) {
                    player.sendTitle(main.getLanguageManager().getString("titles.quest-completed.title"), main.getLanguageManager().getString("titles.quest-accepted.subtitle").replaceAll("%QUESTNAME%", activeQuest.getQuest().getQuestDisplayName()), 2, 60, 8);
                } else {
                    player.sendTitle(main.getLanguageManager().getString("titles.quest-completed.title"), main.getLanguageManager().getString("titles.quest-accepted.subtitle").replaceAll("%QUESTNAME%", activeQuest.getQuest().getQuestName()), 2, 60, 8);
                }
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 100, 40);

            }
        }

        for (ActiveQuest activeQuest2 : activeQuests) {
            for (ActiveObjective objective : activeQuest2.getActiveObjectives()) {
                if (objective.getObjective() instanceof OtherQuestObjective) {
                    if (((OtherQuestObjective) (objective.getObjective())).getOtherQuest().equals(activeQuest.getQuest())) {
                        objective.addProgress(1, -1);
                    }
                }
            }
        }
    }

    public void setQuestPoints(long newQuestPoints, boolean notifyPlayer) {
        if (newQuestPoints < 0) { //Prevent questPoints from going below 0
            this.questPoints = 0;
        } else {
            this.questPoints = newQuestPoints;
        }

        if (notifyPlayer) {
            final Player player = Bukkit.getPlayer(getUUID());
            if (player != null) {
                player.sendMessage("§eYour quest points have been set to §b" + newQuestPoints + "§e.");
            }
        }

    }

    public void addQuestPoints(long questPointsToAdd, boolean notifyPlayer) {
        setQuestPoints(getQuestPoints() + questPointsToAdd, false);
        if (notifyPlayer) {
            final Player player = Bukkit.getPlayer(getUUID());
            if (player != null) {
                player.sendMessage("§b+" + questPointsToAdd + " §aquest points!");
            }
        }
    }

    public void removeQuestPoints(final long questPointsToRemove, final boolean notifyPlayer) {
        setQuestPoints(getQuestPoints() - questPointsToRemove, false);
        if (notifyPlayer) {
            final Player player = Bukkit.getPlayer(getUUID());
            if (player != null) {
                player.sendMessage("§b-" + questPointsToRemove + " §cquest points!");
            }
        }
    }

    private void removeMoney(final Player player, final String worldName, final long moneyToDeduct, final boolean notifyPlayer) {
        if(!main.isVaultEnabled() || main.getEconomy() == null){
            main.getLogManager().log(Level.WARNING, "§eWarning: Could not deduct money, because Vault was not found. Please install Vault for money stuff to work.");
            return;
        }
        main.getEconomy().withdrawPlayer(player, worldName, moneyToDeduct);
        if (notifyPlayer) {
            player.sendMessage("§b-" + moneyToDeduct + " §c$!");

        }
    }


    public final long getQuestPoints() {
        return questPoints;
    }

    public void removeCompletedQuests() {
        activeQuests.removeAll(questsToComplete);
        activeQuestsCopy.removeAll(questsToComplete);
        for (ActiveQuest activeQuest2 : questsToComplete) {
            completedQuests.add(new CompletedQuest(activeQuest2.getQuest(), this));
        }

        questsToComplete.clear();
    }

    public void addCompletedQuest(final CompletedQuest completedQuest) {
        completedQuests.add(completedQuest);

    }


    public void failQuest(ActiveQuest activeQuestToFail) {
        final ArrayList<ActiveQuest> activeQuestsCopy = new ArrayList<>(activeQuests);
        for (ActiveQuest foundActiveQuest : activeQuestsCopy) {
            if (activeQuestToFail.equals(foundActiveQuest)) {

                foundActiveQuest.fail();
                questsToRemove.add(foundActiveQuest);
                final Player player = Bukkit.getPlayer(uuid);

                if (player != null) {

                    if (!activeQuestToFail.getQuest().getQuestDisplayName().equals("")) {
                        player.sendTitle(main.getLanguageManager().getString("titles.quest-failed.title"), main.getLanguageManager().getString("titles.quest-failed.subtitle").replaceAll("%QUESTNAME%", activeQuestToFail.getQuest().getQuestDisplayName()), 2, 60, 8);
                    } else {
                        player.sendTitle(main.getLanguageManager().getString("titles.quest-failed.title"), main.getLanguageManager().getString("titles.quest-failed.subtitle").replaceAll("%QUESTNAME%", activeQuestToFail.getQuest().getQuestName()), 2, 60, 8);
                    }

                    player.playSound(player.getLocation(), Sound.ENTITY_RAVAGER_DEATH, SoundCategory.MASTER, 100, 1);

                }
            }
        }
        activeQuests.removeAll(questsToRemove);
        activeQuestsCopy.removeAll(questsToComplete);

        questsToComplete.clear();


    }

    public ArrayList<ActiveQuest> getActiveQuestsCopy() {
        return activeQuestsCopy;
    }

    public final boolean hasAcceptedQuest(final Quest quest) {
        for (final ActiveQuest activeQuest : activeQuests) {
            if (activeQuest.getQuest().equals(quest)) {
                return true;
            }
        }
        return false;
    }
}
