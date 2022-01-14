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

package rocks.gravili.notquests.spigot.managers.registering;

import cloud.commandframework.Command;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.paper.PaperCommandManager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import rocks.gravili.notquests.spigot.NotQuests;
import rocks.gravili.notquests.spigot.commands.NotQuestColors;
import rocks.gravili.notquests.spigot.structs.Quest;
import rocks.gravili.notquests.spigot.structs.QuestPlayer;
import rocks.gravili.notquests.spigot.structs.actions.*;
import rocks.gravili.notquests.spigot.structs.conditions.Condition;
import rocks.gravili.notquests.spigot.structs.objectives.Objective;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;

import static rocks.gravili.notquests.spigot.commands.NotQuestColors.*;

public class ActionManager {
    private final NotQuests main;

    private final HashMap<String, Class<? extends Action>> actions;


    public ActionManager(final NotQuests main) {
        this.main = main;
        actions = new HashMap<>();

        registerDefaultActions();
    }

    public void registerDefaultActions() {
        actions.clear();
        registerAction("Action", ActionAction.class);
        registerAction("GiveQuest", GiveQuestAction.class);
        registerAction("CompleteQuest", CompleteQuestAction.class);
        registerAction("FailQuest", FailQuestAction.class);
        registerAction("TriggerCommand", TriggerCommandAction.class);
        registerAction("StartConversation", StartConversationAction.class);

        registerAction("ConsoleCommand", ConsoleCommandAction.class);
        registerAction("GiveQuestPoints", GiveQuestPointsAction.class);
        registerAction("GiveItem", GiveItemAction.class);
        registerAction("GiveMoney", GiveMoneyAction.class);
        registerAction("GrantPermission", GrantPermissionAction.class);
        registerAction("SpawnMob", SpawnMobAction.class);
        registerAction("SendMessage", SendMessageAction.class);


    }


    public void registerAction(final String identifier, final Class<? extends Action> action) {
        main.getLogManager().info("Registering action <AQUA>" + identifier);
        actions.put(identifier, action);

        try {
            Method commandHandler = action.getMethod("handleCommands", main.getClass(), PaperCommandManager.class, Command.Builder.class, ActionFor.class);
            commandHandler.invoke(action, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminEditAddRewardCommandBuilder(), ActionFor.QUEST);
            commandHandler.invoke(action, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminEditObjectiveAddRewardCommandBuilder(), ActionFor.OBJECTIVE);
            commandHandler.invoke(action, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminAddActionCommandBuilder(), ActionFor.ActionsYML); //For Actions.yml
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }


    public final Class<? extends Action> getActionClass(final String type) {
        return actions.get(type);
    }

    public final String getActionType(final Class<? extends Action> action) {
        for (final String actionType : actions.keySet()) {
            if (actions.get(actionType).equals(action)) {
                return actionType;
            }
        }
        return null;
    }

    public final HashMap<String, Class<? extends Action>> getActionsAndIdentifiers() {
        return actions;
    }

    public final Collection<Class<? extends Action>> getActions() {
        return actions.values();
    }

    public final Collection<String> getActionIdentifiers() {
        return actions.keySet();
    }

    public void addAction(Action action, CommandContext<CommandSender> context) {
        Audience audience = main.adventure().sender(context.getSender());

        Quest quest = context.getOrDefault("quest", null);
        Objective objectiveOfQuest = null;
        if (quest != null && context.contains("Objective ID")) {
            int objectiveID = context.get("Objective ID");
            objectiveOfQuest = quest.getObjectiveFromID(objectiveID);
        }
        String actionIdentifier = context.getOrDefault("Action Identifier", "");

        if (quest != null) {
            action.setQuest(quest);
            if (objectiveOfQuest != null) {//Objective Reward
                action.setObjective(objectiveOfQuest);

                objectiveOfQuest.addReward(action, true); //TODO: Also do addAction which are executed when the objective is unlocked (and not just when completed)

                audience.sendMessage(MiniMessage.miniMessage().deserialize(
                        NotQuestColors.successGradient + getActionType(action.getClass()) + " Reward successfully added to Objective " + NotQuestColors.highlightGradient
                                + objectiveOfQuest.getObjectiveFinalName() + "</gradient>!</gradient>"));
            } else { //Quest Reward
                quest.addReward(action, true);

                audience.sendMessage(MiniMessage.miniMessage().deserialize(
                        NotQuestColors.successGradient + getActionType(action.getClass()) + " Reward successfully added to Quest " + NotQuestColors.highlightGradient
                                + quest.getQuestName() + "</gradient>!</gradient>"
                ));
            }
        } else {
            if (actionIdentifier != null && !actionIdentifier.isBlank()) { //actions.yml

                if (main.getActionsYMLManager().getAction(actionIdentifier) == null) {
                    main.getActionsYMLManager().addAction(actionIdentifier, action);
                    audience.sendMessage(MiniMessage.miniMessage().deserialize(
                            NotQuestColors.successGradient + getActionType(action.getClass()) + " Action with the name " + NotQuestColors.highlightGradient
                                    + actionIdentifier + "</gradient> has been created successfully!</gradient>"
                    ));
                } else {
                    audience.sendMessage(MiniMessage.miniMessage().deserialize(errorGradient + "Error! An action with the name " + highlightGradient + actionIdentifier + "</gradient> already exists!</gradient>"));

                }
            }
        }


    }


    public void executeActionWithConditions(Action action, QuestPlayer questPlayer, Audience audience, boolean silent, Object... objects) {
        main.getLogManager().debug("Executing Action with conditions!");

        if (action.getConditions().size() == 0) {
            main.getLogManager().debug("   Skipping Conditions");
            action.execute(questPlayer.getPlayer(), objects);
            if (!silent) {
                audience.sendMessage(MiniMessage.miniMessage().deserialize(successGradient + "Action with the name " + highlightGradient + action.getActionName() + "</gradient> has been executed!</gradient>"));
            }
            return;
        }

        StringBuilder unfulfilledConditions = new StringBuilder();
        for (final Condition condition : action.getConditions()) {
            final String check = condition.check(questPlayer, false);
            main.getLogManager().debug("   Condition Check Result: " + check);
            if (!check.isBlank()) {
                unfulfilledConditions.append("\n").append(check);
            }
        }

        if (!unfulfilledConditions.toString().isBlank()) {
            if (!silent) {
                audience.sendMessage(MiniMessage.miniMessage().deserialize(errorGradient + "You do not fulfill all the conditions this action needs! Conditions still needed:" + unfulfilledConditions));
            }
        } else {
            main.getLogManager().debug("   All Conditions fulfilled!");
            //Now loop through all the requirements again in order to enforce them
            for (final Condition condition : action.getConditions()) {
                condition.check(questPlayer, true);
            }
            action.execute(questPlayer.getPlayer(), objects);
            if (!silent) {
                audience.sendMessage(MiniMessage.miniMessage().deserialize(successGradient + "Action with the name " + highlightGradient + action.getActionName() + "</gradient> has been executed!</gradient>"));
            }
        }
    }


}
