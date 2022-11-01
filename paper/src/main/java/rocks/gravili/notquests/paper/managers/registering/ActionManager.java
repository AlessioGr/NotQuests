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
import cloud.commandframework.arguments.flags.CommandFlag;
import cloud.commandframework.bukkit.arguments.selector.SinglePlayerSelector;
import cloud.commandframework.bukkit.parsers.selector.SinglePlayerSelectorArgument;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.data.Category;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.actions.Action;
import rocks.gravili.notquests.paper.structs.actions.ActionAction;
import rocks.gravili.notquests.paper.structs.actions.ActionFor;
import rocks.gravili.notquests.paper.structs.actions.BeamAction;
import rocks.gravili.notquests.paper.structs.actions.BooleanAction;
import rocks.gravili.notquests.paper.structs.actions.BroadcastMessageAction;
import rocks.gravili.notquests.paper.structs.actions.CompleteQuestAction;
import rocks.gravili.notquests.paper.structs.actions.ConsoleCommandAction;
import rocks.gravili.notquests.paper.structs.actions.FailQuestAction;
import rocks.gravili.notquests.paper.structs.actions.GiveItemAction;
import rocks.gravili.notquests.paper.structs.actions.GiveQuestAction;
import rocks.gravili.notquests.paper.structs.actions.ItemStackListAction;
import rocks.gravili.notquests.paper.structs.actions.ListAction;
import rocks.gravili.notquests.paper.structs.actions.NumberAction;
import rocks.gravili.notquests.paper.structs.actions.ChatAction;
import rocks.gravili.notquests.paper.structs.actions.PlaySoundAction;
import rocks.gravili.notquests.paper.structs.actions.PlayerCommandAction;
import rocks.gravili.notquests.paper.structs.actions.SendMessageAction;
import rocks.gravili.notquests.paper.structs.actions.SpawnMobAction;
import rocks.gravili.notquests.paper.structs.actions.StartConversationAction;
import rocks.gravili.notquests.paper.structs.actions.StringAction;
import rocks.gravili.notquests.paper.structs.actions.TriggerCommandAction;
import rocks.gravili.notquests.paper.structs.actions.hooks.betonquest.BetonQuestFireEventAction;
import rocks.gravili.notquests.paper.structs.actions.hooks.betonquest.BetonQuestFireInlineEventAction;
import rocks.gravili.notquests.paper.structs.conditions.Condition;
import rocks.gravili.notquests.paper.structs.conditions.Condition.ConditionResult;
import rocks.gravili.notquests.paper.structs.objectives.Objective;

public class ActionManager {
  private final NotQuests main;
  private final CommandFlag<SinglePlayerSelector> playerSelectorCommandFlag;
  private final HashMap<String, Class<? extends Action>> actions;

  public ActionManager(final NotQuests main) {
    this.main = main;
    actions = new HashMap<>();
    playerSelectorCommandFlag = CommandFlag
        .newBuilder("player")
        .withArgument(SinglePlayerSelectorArgument.of("player"))
        .build();
    registerDefaultActions();
  }

  public void registerDefaultActions() {
    main.getLogManager().info("Registering actions...");

    actions.clear();
    registerAction("Action", ActionAction.class);
    registerAction("GiveQuest", GiveQuestAction.class);
    registerAction("CompleteQuest", CompleteQuestAction.class);
    registerAction("FailQuest", FailQuestAction.class);
    registerAction("TriggerCommand", TriggerCommandAction.class);
    registerAction("StartConversation", StartConversationAction.class);

    registerAction("ConsoleCommand", ConsoleCommandAction.class);
    registerAction("PlayerCommand", PlayerCommandAction.class);
    registerAction("Chat", ChatAction.class);

    // registerAction("GiveQuestPoints", GiveQuestPointsAction.class);
    registerAction("GiveItem", GiveItemAction.class);
    // registerAction("GiveMoney", GiveMoneyAction.class);
    // registerAction("GrantPermission", GrantPermissionAction.class);
    registerAction("SpawnMob", SpawnMobAction.class);
    registerAction("SendMessage", SendMessageAction.class);
    registerAction("BroadcastMessage", BroadcastMessageAction.class);

    registerAction("PlaySound", PlaySoundAction.class);


    registerAction("Number", NumberAction.class);
    registerAction("String", StringAction.class);
    registerAction("Boolean", BooleanAction.class);
    registerAction("List", ListAction.class);
    registerAction("ItemStackList", ItemStackListAction.class);

    registerAction("Beam", BeamAction.class);

    if (main.getIntegrationsManager().isBetonQuestEnabled()) {
      registerAction("BetonQuestFireEvent", BetonQuestFireEventAction.class);
      registerAction("BetonQuestFireInlineEvent", BetonQuestFireInlineEventAction.class);
    }
  }

  public void registerAction(final String identifier, final Class<? extends Action> action) {
    if (main.getConfiguration().isVerboseStartupMessages()) {
      main.getLogManager().info("Registering action <highlight>" + identifier);
    }
    actions.put(identifier, action);

    try {
      final Method commandHandler =
          action.getMethod(
              "handleCommands",
              main.getClass(),
              PaperCommandManager.class,
              Command.Builder.class,
              ActionFor.class);
      if (action == NumberAction.class
          || action == StringAction.class
          || action == BooleanAction.class
          || action == ListAction.class
          || action == ItemStackListAction.class) {
        commandHandler.invoke(
            action,
            main,
            main.getCommandManager().getPaperCommandManager(),
            main.getCommandManager()
                .getAdminEditAddRewardCommandBuilder()
                .meta(CommandMeta.DESCRIPTION, "Creates a new " + identifier + " action"),
            ActionFor.QUEST);
        commandHandler.invoke(
            action,
            main,
            main.getCommandManager().getPaperCommandManager(),
            main.getCommandManager()
                .getAdminEditObjectiveAddRewardCommandBuilder()
                .meta(CommandMeta.DESCRIPTION, "Creates a new " + identifier + " action"),
            ActionFor.OBJECTIVE);
        commandHandler.invoke(
            action,
            main,
            main.getCommandManager().getPaperCommandManager(),
            main.getCommandManager()
                .getAdminAddActionCommandBuilder()
                .meta(CommandMeta.DESCRIPTION, "Creates a new " + identifier + " action")
                .flag(main.getCommandManager().categoryFlag)
                .flag(main.getCommandManager().delayFlag),
            ActionFor.ActionsYML); // For Actions.yml

        commandHandler.invoke(
            action,
            main,
            main.getCommandManager().getPaperCommandManager(),
            main.getCommandManager()
                .getAdminExecuteActionCommandBuilder()
                .meta(CommandMeta.DESCRIPTION, "Executes a new " + identifier + " action inline")
                .flag(playerSelectorCommandFlag)
                .flag(main.getCommandManager().delayFlag),
            ActionFor.INLINE); // For inline /qa actions execute
      } else {
        commandHandler.invoke(
            action,
            main,
            main.getCommandManager().getPaperCommandManager(),
            main.getCommandManager()
                .getAdminEditAddRewardCommandBuilder()
                .literal(identifier)
                .meta(CommandMeta.DESCRIPTION, "Creates a new " + identifier + " action"),
            ActionFor.QUEST);
        commandHandler.invoke(
            action,
            main,
            main.getCommandManager().getPaperCommandManager(),
            main.getCommandManager()
                .getAdminEditObjectiveAddRewardCommandBuilder()
                .literal(identifier)
                .meta(CommandMeta.DESCRIPTION, "Creates a new " + identifier + " action"),
            ActionFor.OBJECTIVE);
        commandHandler.invoke(
            action,
            main,
            main.getCommandManager().getPaperCommandManager(),
            main.getCommandManager()
                .getAdminAddActionCommandBuilder()
                .literal(identifier)
                .meta(CommandMeta.DESCRIPTION, "Creates a new " + identifier + " action")
                .flag(main.getCommandManager().categoryFlag)
                .flag(main.getCommandManager().delayFlag),
            ActionFor.ActionsYML); // For Actions.yml


        commandHandler.invoke(
            action,
            main,
            main.getCommandManager().getPaperCommandManager(),
            main.getCommandManager()
                .getAdminExecuteActionCommandBuilder()
                .literal(identifier)
                .meta(CommandMeta.DESCRIPTION, "Executes a new " + identifier + " action inline")
                .flag(playerSelectorCommandFlag)
                .flag(main.getCommandManager().delayFlag),
            ActionFor.INLINE); // For inline /qa actions execute
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

  public void addAction(final Action action, final CommandContext<CommandSender> context, final ActionFor actionFor) {
    final Quest quest = context.getOrDefault("quest", null);
    Objective objectiveOfQuest = null;
    if (quest != null && context.contains("Objective ID")) {
      objectiveOfQuest = context.get("Objective ID"); //TODO: Support nested objectives
    }
    final String actionIdentifier =
        context.getOrDefault("Action Identifier", context.getOrDefault("action", ""));

    if (context.flags().contains(main.getCommandManager().delayFlag)) {
      final Duration delayDuration =
          context
              .flags()
              .getValue(
                  main.getCommandManager().delayFlag,
                  null);
      if(delayDuration != null){
        action.setExecutionDelay(delayDuration.toMillis());
      }
    }

    if (quest != null) {
      action.setObjectiveHolder(quest);
      action.setCategory(quest.getCategory());
      if (objectiveOfQuest != null) { // Objective Reward
        action.setObjective(objectiveOfQuest);
        action.setActionID(objectiveOfQuest.getFreeRewardID());

        objectiveOfQuest.addReward(
            action,
            true); // TODO: Also do addAction which are executed when the objective is unlocked (and
                   // not just when completed)

        context
            .getSender()
            .sendMessage(
                main.parse(
                    "<success>"
                        + getActionType(action.getClass())
                        + " Reward successfully added to Objective <highlight>"
                        + objectiveOfQuest.getDisplayNameOrIdentifier()
                        + "</highlight>!"));
      } else { // Quest Reward
        action.setActionID(quest.getFreeRewardID());
        quest.addReward(action, true);

        context
            .getSender()
            .sendMessage(
                main.parse(
                    "<success>"
                        + getActionType(action.getClass())
                        + " Reward successfully added to Quest <highlight>"
                        + quest.getIdentifier()
                        + "</highlight>!"));
      }
    } else {
      if(actionFor == ActionFor.INLINE){
        //Execute action here
        final SinglePlayerSelector singlePlayerSelector = context.flags().getValue(playerSelectorCommandFlag, null);

        final UUID uuid;
        if(singlePlayerSelector != null && singlePlayerSelector.hasAny() && singlePlayerSelector.getPlayer() != null){
          uuid = singlePlayerSelector.getPlayer().getUniqueId();
        }else if(context.getSender() instanceof final Player senderPlayer){
          uuid = senderPlayer.getUniqueId();
        } else {
          uuid = null;
        }

        if(uuid != null){
          action.execute(main.getQuestPlayerManager().getOrCreateQuestPlayer(uuid));
        }
      }else if (actionIdentifier != null && !actionIdentifier.isBlank()) { // actions.yml
          if (context.flags().contains(main.getCommandManager().categoryFlag)) {
            final Category category =
                context
                    .flags()
                    .getValue(
                        main.getCommandManager().categoryFlag,
                        main.getDataManager().getDefaultCategory());
            action.setCategory(category);
          }

          if (main.getActionsYMLManager().getAction(actionIdentifier) == null) {
            context
                .getSender()
                .sendMessage(
                    main.parse(main.getActionsYMLManager().addAction(actionIdentifier, action)));

          } else {
            context
                .getSender()
                .sendMessage(
                    main.parse(
                        "<error>Error! An action with the name <highlight>"
                            + actionIdentifier
                            + "</highlight> already exists!"));
          }
      }
    }
  }

  public void executeActionWithConditions(
      final Action action,
      final QuestPlayer questPlayer,
      final CommandSender sender,
      final boolean silent,
      final Object... objects) {
    executeActionWithConditions(action, questPlayer, sender, silent,-1, objects);
  }
  public void executeActionWithConditions(
      final Action action,
      final QuestPlayer questPlayer,
      final CommandSender sender,
      final boolean silent,
      final int delay,
      final Object... objects) {
    main.getLogManager()
        .debug(
            "Executing Action "
                + action.getActionName()
                + " of type "
                + action.getActionType()
                + " with conditions!");
    questPlayer.sendDebugMessage(
        "Executing Action "
            + action.getActionName()
            + " of type "
            + action.getActionType()
            + " with conditions!");

    if (action.getConditions().size() == 0) {
      main.getLogManager().debug("   Skipping Conditions");
      action.execute(questPlayer, delay, objects);
      if (!silent) {
        sender.sendMessage(
            main.parse(
                "<success>Action with the name <highlight>"
                    + action.getActionName()
                    + "</highlight> has been executed!"));
      }
      return;
    }

    final StringBuilder unfulfilledConditions = new StringBuilder();
    for (final Condition condition : action.getConditions()) {
      final ConditionResult check = condition.check(questPlayer);
      main.getLogManager().debug("   Condition Check Result: " + check.message());
      if (!check.fulfilled()) {
        unfulfilledConditions.append("\n").append(check.message());
      }
    }

    if (!unfulfilledConditions.toString().isBlank()) {
      if (!silent) {
        sender.sendMessage(
            main.parse(
                main.getLanguageManager()
                        .getString(
                            "chat.action-not-all-conditions-fulfilled",
                            questPlayer.getPlayer(),
                            questPlayer)
                    + unfulfilledConditions));
      }
      questPlayer.sendDebugMessage(
          "Skipping action "
              + action.getActionName()
              + ". Unfulfilled conditions: "
              + unfulfilledConditions);
    } else {
      main.getLogManager().debug("   All Conditions fulfilled!");

      action.execute(questPlayer, objects);
      if (!silent) {
        sender.sendMessage(
            main.parse(
                "<success>Action with the name <highlight>"
                    + action.getActionName()
                    + "</highlight> has been executed!"));
      }
    }
  }

  public void updateVariableActions() {
    try {
      for (final Class<? extends Action> action : getActions()) {
        final String identifier = getActionType(action);

        final Method commandHandler =
            action.getMethod(
                "handleCommands",
                main.getClass(),
                PaperCommandManager.class,
                Command.Builder.class,
                ActionFor.class);
        if (action == NumberAction.class
            || action == StringAction.class
            || action == BooleanAction.class
            || action == ListAction.class
            || action == ItemStackListAction.class) {

          main.getLogManager()
              .info("Re-registering action " + identifier + " due to variable changes...");

          commandHandler.invoke(
              action,
              main,
              main.getCommandManager().getPaperCommandManager(),
              main.getCommandManager()
                  .getAdminEditAddRewardCommandBuilder()
                  .meta(CommandMeta.DESCRIPTION, "Creates a new " + identifier + " action"),
              ActionFor.QUEST);
          commandHandler.invoke(
              action,
              main,
              main.getCommandManager().getPaperCommandManager(),
              main.getCommandManager()
                  .getAdminEditObjectiveAddRewardCommandBuilder()
                  .meta(CommandMeta.DESCRIPTION, "Creates a new " + identifier + " action"),
              ActionFor.OBJECTIVE);
          commandHandler.invoke(
              action,
              main,
              main.getCommandManager().getPaperCommandManager(),
              main.getCommandManager()
                  .getAdminAddActionCommandBuilder()
                  .meta(CommandMeta.DESCRIPTION, "Creates a new " + identifier + " action")
                  .flag(main.getCommandManager().categoryFlag)
                  .flag(main.getCommandManager().delayFlag),
              ActionFor.ActionsYML); // For Actions.yml

          commandHandler.invoke(
              action,
              main,
              main.getCommandManager().getPaperCommandManager(),
              main.getCommandManager()
                  .getAdminExecuteActionCommandBuilder()
                  .meta(CommandMeta.DESCRIPTION, "Executes a new " + identifier + " action inline")
                  .flag(playerSelectorCommandFlag)
                  .flag(main.getCommandManager().delayFlag),
              ActionFor.INLINE); // For inline /qa actions execute
        }
      }
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }
}
