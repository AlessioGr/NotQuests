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

package rocks.gravili.notquests.paper.managers.registering;

import cloud.commandframework.Command;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.data.Category;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.actions.*;
import rocks.gravili.notquests.paper.structs.actions.hooks.betonquest.BetonQuestFireEventAction;
import rocks.gravili.notquests.paper.structs.actions.hooks.betonquest.BetonQuestFireInlineEventAction;
import rocks.gravili.notquests.paper.structs.conditions.Condition;
import rocks.gravili.notquests.paper.structs.objectives.Objective;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;

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
        //registerAction("GiveQuestPoints", GiveQuestPointsAction.class);
        registerAction("GiveItem", GiveItemAction.class);
        //registerAction("GiveMoney", GiveMoneyAction.class);
        //registerAction("GrantPermission", GrantPermissionAction.class);
        registerAction("SpawnMob", SpawnMobAction.class);
        registerAction("SendMessage", SendMessageAction.class);
        registerAction("BroadcastMessage", BroadcastMessageAction.class);

        registerAction("Number", NumberAction.class);
        registerAction("String", StringAction.class);
        registerAction("Boolean", BooleanAction.class);
        registerAction("List", ListAction.class);
        registerAction("ItemStackList", ItemStackListAction.class);

        registerAction("Beam", BeamAction.class);

        if(main.getIntegrationsManager().isBetonQuestEnabled()){
            registerAction("BetonQuestFireEvent", BetonQuestFireEventAction.class);
            registerAction("BetonQuestFireInlineEvent", BetonQuestFireInlineEventAction.class);
        }



    }


    public void registerAction(final String identifier, final Class<? extends Action> action) {
        if (main.getConfiguration().isDebug()) {
            main.getLogManager().info("Registering action <highlight>" + identifier);
        }
        actions.put(identifier, action);

        try {
            final Method commandHandler = action.getMethod("handleCommands", main.getClass(), PaperCommandManager.class, Command.Builder.class, ActionFor.class);
            if(action == NumberAction.class || action == StringAction.class || action == BooleanAction.class || action == ListAction.class || action == ItemStackListAction.class){
                commandHandler.invoke(action, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminEditAddRewardCommandBuilder()
                        .meta(CommandMeta.DESCRIPTION, "Creates a new " + identifier + " action"), ActionFor.QUEST);
                commandHandler.invoke(action, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminEditObjectiveAddRewardCommandBuilder()
                        .meta(CommandMeta.DESCRIPTION, "Creates a new " + identifier + " action"), ActionFor.OBJECTIVE);
                commandHandler.invoke(action, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminAddActionCommandBuilder()
                        .meta(CommandMeta.DESCRIPTION, "Creates a new " + identifier + " action")
                        .flag(main.getCommandManager().categoryFlag), ActionFor.ActionsYML); //For Actions.yml
            }else {
                commandHandler.invoke(action, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminEditAddRewardCommandBuilder().literal(identifier)
                        .meta(CommandMeta.DESCRIPTION, "Creates a new " + identifier + " action"), ActionFor.QUEST);
                commandHandler.invoke(action, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminEditObjectiveAddRewardCommandBuilder().literal(identifier)
                        .meta(CommandMeta.DESCRIPTION, "Creates a new " + identifier + " action"), ActionFor.OBJECTIVE);
                commandHandler.invoke(action, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminAddActionCommandBuilder().literal(identifier)
                        .meta(CommandMeta.DESCRIPTION, "Creates a new " + identifier + " action")
                        .flag(main.getCommandManager().categoryFlag), ActionFor.ActionsYML); //For Actions.yml
            }

        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }


    public final Class<? extends Action> getActionClass(@NotNull final String type) {
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

    public void addAction(final Action action, final CommandContext<CommandSender> context) {
        final Quest quest = context.getOrDefault("quest", null);
        Objective objectiveOfQuest = null;
        if (quest != null && context.contains("Objective ID")) {
            final int objectiveID = context.get("Objective ID");
            objectiveOfQuest = quest.getObjectiveFromID(objectiveID);
        }
        final String actionIdentifier = context.getOrDefault("Action Identifier", context.getOrDefault("action", ""));


        if (quest != null) {
            action.setQuest(quest);
            action.setCategory(quest.getCategory());
            if (objectiveOfQuest != null) {//Objective Reward
                action.setObjective(objectiveOfQuest);
                action.setActionID(objectiveOfQuest.getFreeRewardID());

                objectiveOfQuest.addReward(action, true); //TODO: Also do addAction which are executed when the objective is unlocked (and not just when completed)

                context.getSender().sendMessage(main.parse(
                        "<success>" + getActionType(action.getClass()) + " Reward successfully added to Objective <highlight>"
                                + objectiveOfQuest.getFinalName() + "</highlight>!"));
            } else { //Quest Reward
                action.setActionID(quest.getFreeRewardID());
                quest.addReward(action, true);

                context.getSender().sendMessage(main.parse(
                        "<success>" + getActionType(action.getClass()) + " Reward successfully added to Quest <highlight>"
                                + quest.getQuestName() + "</highlight>!"
                ));
            }
        } else {
            if (actionIdentifier != null && !actionIdentifier.isBlank()) { //actions.yml
                if (context.flags().contains(main.getCommandManager().categoryFlag)) {
                    final Category category = context.flags().getValue(main.getCommandManager().categoryFlag, main.getDataManager().getDefaultCategory());
                    action.setCategory(category);
                }

                if (main.getActionsYMLManager().getAction(actionIdentifier) == null) {
                    context.getSender().sendMessage(main.parse(main.getActionsYMLManager().addAction(actionIdentifier, action)));

                } else {
                    context.getSender().sendMessage(main.parse("<error>Error! An action with the name <highlight>" + actionIdentifier + "</highlight> already exists!"));

                }
            }
        }


    }


    public void executeActionWithConditions(final Action action, final QuestPlayer questPlayer, final CommandSender sender, final boolean silent, final Object... objects) {
        main.getLogManager().debug("Executing Action " + action.getActionName() + " of type " + action.getActionType() + " with conditions!");

        if (action.getConditions().size() == 0) {
            main.getLogManager().debug("   Skipping Conditions");
            action.execute(questPlayer, objects);
            if (!silent) {
                sender.sendMessage(main.parse("<success>Action with the name <highlight>" + action.getActionName() + "</highlight> has been executed!"));
            }
            return;
        }

        final StringBuilder unfulfilledConditions = new StringBuilder();
        for (final Condition condition : action.getConditions()) {
            final String check = condition.check(questPlayer);
            main.getLogManager().debug("   Condition Check Result: " + check);
            if (!check.isBlank()) {
                unfulfilledConditions.append("\n").append(check);
            }
        }

        if (!unfulfilledConditions.toString().isBlank()) {
            if (!silent) {
                sender.sendMessage(main.parse(main.getLanguageManager().getString("chat.action-not-all-conditions-fulfilled", questPlayer.getPlayer(), questPlayer) + unfulfilledConditions));
            }
            questPlayer.sendDebugMessage(
                    "Skipping action " + action.getActionName() + ". Unfulfilled conditions: " + unfulfilledConditions
            );
        } else {
            main.getLogManager().debug("   All Conditions fulfilled!");

            action.execute(questPlayer, objects);
            if (!silent) {
                sender.sendMessage(main.parse("<success>Action with the name <highlight>" + action.getActionName() + "</highlight> has been executed!"));
            }
        }
    }


    public void updateVariableActions() {
        try {
            for (final Class<? extends Action> action : getActions()) {
                final String identifier = getActionType(action);


                final Method commandHandler = action.getMethod("handleCommands", main.getClass(), PaperCommandManager.class, Command.Builder.class, ActionFor.class);
                if (action == NumberAction.class || action == StringAction.class || action == BooleanAction.class || action == ListAction.class || action == ItemStackListAction.class) {

                    main.getLogManager().info("Re-registering action " + identifier + " due to variable changes...");

                    commandHandler.invoke(action, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminEditAddRewardCommandBuilder()
                            .meta(CommandMeta.DESCRIPTION, "Creates a new " + identifier + " action"), ActionFor.QUEST);
                    commandHandler.invoke(action, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminEditObjectiveAddRewardCommandBuilder()
                            .meta(CommandMeta.DESCRIPTION, "Creates a new " + identifier + " action"), ActionFor.OBJECTIVE);
                    commandHandler.invoke(action, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminAddActionCommandBuilder()
                            .meta(CommandMeta.DESCRIPTION, "Creates a new " + identifier + " action")
                            .flag(main.getCommandManager().categoryFlag), ActionFor.ActionsYML); //For Actions.yml
                }
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
}
