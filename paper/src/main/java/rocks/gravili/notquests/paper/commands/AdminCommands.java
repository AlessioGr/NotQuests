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

package rocks.gravili.notquests.paper.commands;


import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.arguments.standard.StringArrayArgument;
import cloud.commandframework.bukkit.arguments.selector.SinglePlayerSelector;
import cloud.commandframework.bukkit.parsers.WorldArgument;
import cloud.commandframework.bukkit.parsers.selector.SinglePlayerSelectorArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.lang.StringUtils;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.*;
import rocks.gravili.notquests.paper.commands.arguments.variables.BooleanVariableValueArgument;
import rocks.gravili.notquests.paper.commands.arguments.wrappers.ItemStackSelection;
import rocks.gravili.notquests.paper.managers.data.Category;
import rocks.gravili.notquests.paper.managers.expressions.NumberExpression;
import rocks.gravili.notquests.paper.managers.npc.NQNPC;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.ActiveQuest;
import rocks.gravili.notquests.paper.structs.CompletedQuest;
import rocks.gravili.notquests.paper.structs.PredefinedProgressOrder;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.actions.Action;
import rocks.gravili.notquests.paper.structs.conditions.Condition;
import rocks.gravili.notquests.paper.structs.conditions.Condition.ConditionResult;
import rocks.gravili.notquests.paper.structs.objectives.Objective;
import rocks.gravili.notquests.paper.structs.objectives.TriggerCommandObjective;

public class AdminCommands {
    public final ArrayList<String> placeholders;
    private final NotQuests main;
    private final PaperCommandManager<CommandSender> manager;
    private final Command.Builder<CommandSender> builder;
    private final Date resultDate;


    public AdminCommands(final NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        this.main = main;
        this.manager = manager;
        this.builder = builder;



        placeholders = new ArrayList<>();
        placeholders.add("{PLAYER}");
        placeholders.add("{PLAYERUUID}");
        placeholders.add("{PLAYERX}");
        placeholders.add("{PLAYERY}");
        placeholders.add("{PLAYERZ}");
        placeholders.add("{WORLD}");
        placeholders.add("{QUEST}");
        placeholders.add("{{expression}}");

        resultDate = new Date();


        manager.command(builder.literal("create")
                .argument(StringArgument.<CommandSender>newBuilder("Quest Name").withSuggestionsProvider(
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[New Quest Name]", "");

                            ArrayList<String> completions = new ArrayList<>();

                            completions.add("<Enter new Quest Name>");
                            return completions;
                        }
                ).single().build(), ArgumentDescription.of("Quest Name"))
                .flag(main.getCommandManager().categoryFlag)
                .meta(CommandMeta.DESCRIPTION, "Create a new quest.")
                .handler((context) -> {
                    if (context.flags().contains(main.getCommandManager().categoryFlag)) {
                        final Category category = context.flags().getValue(main.getCommandManager().categoryFlag, main.getDataManager().getDefaultCategory());
                        context.getSender().sendMessage(main.parse(main.getQuestManager().createQuest(context.get("Quest Name"), category)));
                    }else{
                        context.getSender().sendMessage(main.parse(main.getQuestManager().createQuest(context.get("Quest Name"))));
                    }
                }));

        manager.command(builder.literal("delete")
                .argument(StringArgument.<CommandSender>newBuilder("Quest Name").withSuggestionsProvider(
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[Name of the Quest you want to delete]", "");

                            ArrayList<String> completions = new ArrayList<>();

                            for (Quest quest : main.getQuestManager().getAllQuests()) {
                                completions.add(quest.getIdentifier());
                            }
                            return completions;
                        }
                ).single().build(), ArgumentDescription.of("Quest Name"))
                .meta(CommandMeta.DESCRIPTION, "Delete an existing Quest.")
                .handler((context) -> {
                    context.getSender().sendMessage(main.parse(main.getQuestManager().deleteQuest(context.get("Quest Name"))));
                }));

        handleConditions();
        handleActions();

        manager.command(builder.literal("give")
                .argument(SinglePlayerSelectorArgument.of("player"), ArgumentDescription.of("Player who should start the quest."))
                .argument(QuestSelector.of("quest", main), ArgumentDescription.of("Name of the Quest the player should start."))
                .meta(CommandMeta.DESCRIPTION, "Gives a player a quest without bypassing the Quest requirements.")
                .handler((context) -> {
                    final SinglePlayerSelector singlePlayerSelector = context.get("player");

                    final Quest quest = context.get("quest");

                    if (singlePlayerSelector.hasAny() && singlePlayerSelector.getPlayer() != null) {
                        context.getSender().sendMessage(main.parse("<main>" + main.getQuestPlayerManager().acceptQuest(singlePlayerSelector.getPlayer(), quest, true, true)));
                    } else {
                        context.getSender().sendMessage(main.parse("<error>Player is not online or was not found!"));
                    }

                }));


        manager.command(builder.literal("forcegive")
                .argument(SinglePlayerSelectorArgument.of("player"), ArgumentDescription.of("Player who should start the quest."))
                .argument(QuestSelector.of("quest", main), ArgumentDescription.of("Name of the Quest the player should force-start."))
                .meta(CommandMeta.DESCRIPTION, "Force-gives a player a quest and bypasses the Quest requirements, max. accepts & cooldown.")
                .handler((context) -> {
                    final SinglePlayerSelector singlePlayerSelector = context.get("player");

                    final Quest quest = context.get("quest");

                    if (singlePlayerSelector.hasAny() && singlePlayerSelector.getPlayer() != null) {
                        context.getSender().sendMessage(main.parse("<main>" + main.getQuestPlayerManager().forceAcceptQuest(singlePlayerSelector.getPlayer().getUniqueId(), quest)));
                    } else {
                        context.getSender().sendMessage(main.parse("<error>Player is not online or was not found!"));
                    }

                }));

        handleQuestPoints();

        manager.command(builder.literal("activeQuests")
                .argument(SinglePlayerSelectorArgument.of("player"), ArgumentDescription.of("Player whose active quests you want to see."))
                .meta(CommandMeta.DESCRIPTION, "Shows the active quests of a player.")
                .handler((context) -> {
                    context.getSender().sendMessage(Component.empty());
                    final SinglePlayerSelector singlePlayerSelector = context.get("player");
                    final Player player = singlePlayerSelector.getPlayer();
                    if (singlePlayerSelector.hasAny() && player != null) {
                        QuestPlayer questPlayer = main.getQuestPlayerManager().getActiveQuestPlayer(player.getUniqueId());
                        if (questPlayer != null) {
                            context.getSender().sendMessage(main.parse("<main>Active quests of player <highlight>" + player.getName() + "</highlight> <green>(online)</green>:"));
                            int counter = 1;
                            for (ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                                context.getSender().sendMessage(main.parse("<highlight>" + counter + ".</highlight> <main>" + activeQuest.getQuest().getIdentifier()));
                                counter += 1;
                            }
                            context.getSender().sendMessage(main.parse("<unimportant>Total active quests: <highlight2>" + (counter - 1) + "</highlight2>."));
                        } else {
                            context.getSender().sendMessage(main.parse("<error>Seems like the player <highlight>" + player.getName() + "</highlight> <green>(online)</green> did not accept any active quests."));
                        }
                    } else {
                        OfflinePlayer offlinePlayer = main.getUtilManager().getOfflinePlayer(singlePlayerSelector.getSelector());
                        QuestPlayer questPlayer = main.getQuestPlayerManager().getActiveQuestPlayer(offlinePlayer.getUniqueId());
                        if (questPlayer != null) {
                            context.getSender().sendMessage(main.parse("<main>Active quests of player <highlight>" + offlinePlayer.getName() + "</highlight> <red>(offline)</red>:"));
                            int counter = 1;
                            for (ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                                context.getSender().sendMessage(main.parse("<highlight>" + counter + ".</highlight> <main>" + activeQuest.getQuest().getIdentifier()));
                                counter += 1;
                            }
                            context.getSender().sendMessage(main.parse("<unimportant>Total active quests: <highlight2>" + (counter - 1) + "</highlight2>."));
                        } else {
                            context.getSender().sendMessage(main.parse("<error>Seems like the player <highlight>" + offlinePlayer.getName() + "</highlight> <red>(offline)</red> did not accept any active quests."));
                        }
                    }
                }));

        manager.command(builder.literal("completedQuests")
                .argument(SinglePlayerSelectorArgument.of("player"), ArgumentDescription.of("Player whose completed quests you want to see."))
                .meta(CommandMeta.DESCRIPTION, "Shows the completed quests of a player.")
                .handler((context) -> {
                    context.getSender().sendMessage(Component.empty());
                    final SinglePlayerSelector singlePlayerSelector = context.get("player");
                    final Player player = singlePlayerSelector.getPlayer();
                    if (singlePlayerSelector.hasAny() && player != null) {
                        QuestPlayer questPlayer = main.getQuestPlayerManager().getActiveQuestPlayer(player.getUniqueId());
                        if (questPlayer != null) {
                            context.getSender().sendMessage(main.parse("<main>Completed quests of player <highlight>" + player.getName() + "</highlight> <green>(online)</green>:"));
                            int counter = 1;
                            for (CompletedQuest completedQuest : questPlayer.getCompletedQuests()) {
                                resultDate.setTime(completedQuest.getTimeCompleted());
                                context.getSender().sendMessage(main.parse("<highlight>" + counter + ".</highlight> <highlight2>" + completedQuest.getQuest().getIdentifier()
                                        + "</highlight2> <main>Completed: </main><highlight2>" + resultDate + "</highlight2>"
                                ));
                                counter += 1;
                            }

                            context.getSender().sendMessage(main.parse("<unimportant>Total completed quests: <highlight2>" + (counter - 1) + "</highlight2>."));
                        } else {
                            context.getSender().sendMessage(main.parse("<error>Seems like the player <highlight>" + player.getName() + "</highlight> <green>(online)</green> never completed any quests."));
                        }
                    } else {
                        OfflinePlayer offlinePlayer = main.getUtilManager().getOfflinePlayer(singlePlayerSelector.getSelector());
                        QuestPlayer questPlayer = main.getQuestPlayerManager().getActiveQuestPlayer(offlinePlayer.getUniqueId());
                        if (questPlayer != null) {
                            context.getSender().sendMessage(main.parse("<main>Completed quests of player <highlight>" + offlinePlayer.getName() + "</highlight> <red>(offline)</red>:"));
                            int counter = 1;
                            for (CompletedQuest completedQuest : questPlayer.getCompletedQuests()) {
                                resultDate.setTime(completedQuest.getTimeCompleted());
                                context.getSender().sendMessage(main.parse("<main><highlight>" + counter + ".</highlight> <highlight2>" + completedQuest.getQuest().getIdentifier()
                                        + "</highlight2> Completed: <highlight2>" + resultDate + "</highlight2>"
                                ));
                                counter += 1;
                            }

                            context.getSender().sendMessage(main.parse("<unimportant>Total completed quests: <highlight2>" + (counter - 1) + "</highlight2>."));
                        } else {
                            context.getSender().sendMessage(main.parse("<error>Seems like the player <highlight>" + offlinePlayer.getName() + "</highlight> <red>(offline)</red> never completed any quests."));
                        }
                    }
                }));


        manager.command(builder.literal("debug")

                .meta(CommandMeta.DESCRIPTION, "Toggles debug mode for yourself.")
                .senderType(Player.class)
                .handler((context) -> {

                    if (main.getQuestManager().isDebugEnabledPlayer(((Player) context.getSender()).getUniqueId())) {
                        main.getQuestManager().removeDebugEnabledPlayer(((Player) context.getSender()).getUniqueId());
                        context.getSender().sendMessage(main.parse("<success>Your debug mode has been disabled."));
                    } else {
                        main.getQuestManager().addDebugEnabledPlayer(((Player) context.getSender()).getUniqueId());
                        context.getSender().sendMessage(main.parse("<success>Your debug mode has been enabled."));
                    }

                }));

        manager.command(builder.literal("progress")
                .argument(SinglePlayerSelectorArgument.of("player"), ArgumentDescription.of("Player progress you want to see"))
                .argument(ActiveQuestSelector.of("activeQuest", main, "player"), ArgumentDescription.of("Quest name of the quest you wish to see the progress for."))
                .meta(CommandMeta.DESCRIPTION, "Shows the progress for a quest of another player")
                .handler((context) -> {
                    final SinglePlayerSelector singlePlayerSelector = context.get("player");
                    getProgress(context.getSender(), singlePlayerSelector.getPlayer(), singlePlayerSelector.getSelector(), context.get("activeQuest"));
                }));


        manager.command(builder.literal("failQuest")
                .argument(SinglePlayerSelectorArgument.of("player"), ArgumentDescription.of("Player name whose quest should be failed."))
                .argument(ActiveQuestSelector.of("activeQuest", main, "player"), ArgumentDescription.of("Active quest which should be failed."))
                .meta(CommandMeta.DESCRIPTION, "Fails an active quest for a player")
                .handler((context) -> {
                    final SinglePlayerSelector singlePlayerSelector = context.get("player");
                    final Player player = singlePlayerSelector.getPlayer();
                    final ActiveQuest activeQuest = context.get("activeQuest");
                    if (player != null) {
                        final QuestPlayer questPlayer = main.getQuestPlayerManager().getActiveQuestPlayer(player.getUniqueId());
                        if (questPlayer != null) {
                            questPlayer.failQuest(activeQuest);
                            context.getSender().sendMessage(main.parse(
                                    "<main>The active quest <highlight>" + activeQuest.getQuest().getIdentifier() + "</highlight> has been failed for player <highlight2>" + player.getName() + "</highlight2>!"
                            ));

                        } else {
                            context.getSender().sendMessage(main.parse(
                                    "<error>Player <highlight>" + singlePlayerSelector.getSelector() + "</highlight> seems to not have accepted any quests!"
                            ));
                        }
                    } else {
                        context.getSender().sendMessage(main.parse(
                                    "<error>Player <highlight>" + singlePlayerSelector.getSelector() + "</highlight> is not online or was not found!"
                        ));
                    }
                }));


        manager.command(builder.literal("completeQuest")
                .argument(SinglePlayerSelectorArgument.of("player"), ArgumentDescription.of("Player name whose quest should be completed."))
                .argument(ActiveQuestSelector.of("activeQuest", main, "player"), ArgumentDescription.of("Active quest which should be completed."))
                .meta(CommandMeta.DESCRIPTION, "Completes an active quest for a player")
                .handler((context) -> {
                    final SinglePlayerSelector singlePlayerSelector = context.get("player");
                    final Player player = singlePlayerSelector.getPlayer();
                    final ActiveQuest activeQuest = context.get("activeQuest");
                    if (player != null) {
                        final QuestPlayer questPlayer = main.getQuestPlayerManager().getActiveQuestPlayer(player.getUniqueId());
                        if (questPlayer != null) {
                            questPlayer.forceActiveQuestCompleted(activeQuest);
                            context.getSender().sendMessage(main.parse(
                                    "<success>The active quest <highlight>" + activeQuest.getQuest().getIdentifier() + "</highlight> has been completed for player <highlight2>" + player.getName() + "</highlight2>!"
                            ));

                        } else {
                            context.getSender().sendMessage(main.parse(
                                    "<error>Player <highlight>" + singlePlayerSelector.getSelector() + "</highlight> seems to not have accepted any quests!"
                            ));
                        }
                    } else {
                        context.getSender().sendMessage(main.parse(
                                "<error>Player <highlight>" + singlePlayerSelector.getSelector() + "</highlight> is not online or was not found!"
                        ));
                    }
                }));

        manager.command(builder.literal("listObjectiveTypes")
                .meta(CommandMeta.DESCRIPTION, "Shows you a list of all available Objective Types.")
                .handler((context) -> {
                    context.getSender().sendMessage(Component.empty());
                    context.getSender().sendMessage(main.parse( "<highlight>All objective types:"));
                    for (final String objectiveType : main.getObjectiveManager().getObjectiveIdentifiers()) {
                        context.getSender().sendMessage(main.parse("<main>" + objectiveType));
                    }
                }));

        manager.command(builder.literal("listRequirementTypes")
                .meta(CommandMeta.DESCRIPTION, "Shows you a list of all available Requirement Types.")
                .handler((context) -> {
                    context.getSender().sendMessage(Component.empty());
                    context.getSender().sendMessage(main.parse("<highlight>All requirement types:"));
                    for (final String requirementType : main.getConditionsManager().getConditionIdentifiers()) {
                        context.getSender().sendMessage(main.parse("<main>" + requirementType));
                    }
                }));

        manager.command(builder.literal("listActionTypes", "listRewardTyües")
                .meta(CommandMeta.DESCRIPTION, "Shows you a list of all available Action (Reward) Types.")
                .handler((context) -> {
                    context.getSender().sendMessage(Component.empty());
                    context.getSender().sendMessage(main.parse("<highlight>" + "All reward types:"));
                    for (final String rewardType : main.getActionManager().getActionIdentifiers()) {
                        context.getSender().sendMessage(main.parse("<main>" + rewardType));
                    }
                }));

        manager.command(builder.literal("listTriggerTypes")
                .meta(CommandMeta.DESCRIPTION, "Shows you a list of all available Trigger Types.")
                .handler((context) -> {
                    context.getSender().sendMessage(Component.empty());
                    context.getSender().sendMessage(main.parse("<highlight>" + "All trigger types:"));
                    for (final String triggerType : main.getTriggerManager().getTriggerIdentifiers()) {
                        context.getSender().sendMessage(main.parse("<main>" + triggerType));
                    }
                }));

        manager.command(builder.literal("listAllQuests")
                .meta(CommandMeta.DESCRIPTION, "Shows you a list of all created Quests.")
                .handler((context) -> {
                    context.getSender().sendMessage(Component.empty());
                    int counter = 1;
                    context.getSender().sendMessage(main.parse("<highlight>" + "All Quests:"));
                    for (final Quest quest : main.getQuestManager().getAllQuests()) {
                        context.getSender().sendMessage(main.parse("<highlight>" + counter + ".</highlight> " + "<main>" + quest.getIdentifier()));
                        counter += 1;
                    }

                }));

        manager.command(builder.literal("listPlaceholders")
                .meta(CommandMeta.DESCRIPTION, "Shows you a list of all available Placeholders which can be used in Trigger or Action commands.")
                .handler((context) -> {
                    context.getSender().sendMessage(Component.empty());

                    context.getSender().sendMessage(main.parse("<highlight>All Placeholders (Case-sensitive):"));
                    context.getSender().sendMessage(main.parse("<highlight>1.</highlight> <highlight2>{PLAYER}</highlight2> <main>- Name of the player"));
                    context.getSender().sendMessage(main.parse("<highlight>2.</highlight> <highlight2>{PLAYERUUID}</highlight2> <main>- UUID of the player"));
                    context.getSender().sendMessage(main.parse("<highlight>3.</highlight> <highlight2>{PLAYERX}</highlight2> <main>- X coordinates of the player"));
                    context.getSender().sendMessage(main.parse("<highlight>4.</highlight> <highlight2>{PLAYERY}</highlight2> <main>- Y coordinates of the player"));
                    context.getSender().sendMessage(main.parse("<highlight>5.</highlight> <highlight2>{PLAYERZ}</highlight2> <main>- Z coordinates of the player"));
                    context.getSender().sendMessage(main.parse("<highlight>6.</highlight> <highlight2>{WORLD}</highlight2> <main>- World name of the player"));
                    context.getSender().sendMessage(main.parse("<highlight>6.</highlight> <highlight2>{QUEST}</highlight2> <main>- Quest name (if relevant)"));

                }));

        manager.command(builder.literal("triggerObjective")
                .argument(StringArgument.<CommandSender>newBuilder("Trigger Name").withSuggestionsProvider(
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[Trigger Name]", "[Player Name]");

                            ArrayList<String> completions = new ArrayList<>();
                            for (final Quest quest : main.getQuestManager().getAllQuests()) {
                                for (final Objective objective : quest.getObjectives()) {
                                    if (objective instanceof final TriggerCommandObjective triggerCommandObjective) {
                                        completions.add(triggerCommandObjective.getTriggerName());
                                    }
                                }
                            }

                            return completions;
                        }
                ).single().build(), ArgumentDescription.of("Name of the trigger which should be triggered."))
                .argument(SinglePlayerSelectorArgument.of("Player Name"), ArgumentDescription.of("Player whose trigger should e triggered."))
                .meta(CommandMeta.DESCRIPTION, "This triggers the Trigger Command which is needed to complete a TriggerObjective (don't mistake it with Triggers & actions).")
                .handler((context) -> {

                    final String triggerName = context.get("Trigger Name");
                    final SinglePlayerSelector singlePlayerSelector = context.get("Player Name");
                    final Player player = singlePlayerSelector.getPlayer();
                    if (player != null) {
                        final QuestPlayer questPlayer = main.getQuestPlayerManager().getActiveQuestPlayer(player.getUniqueId());
                        if (questPlayer != null) {
                            if (questPlayer.getActiveQuests().size() > 0) {
                                for (ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                                    for (ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                                        if (activeObjective.isUnlocked()) {
                                            if (activeObjective.getObjective() instanceof final TriggerCommandObjective triggerCommandObjective) {
                                                if (triggerCommandObjective.getTriggerName().equalsIgnoreCase(triggerName)) {
                                                    activeObjective.addProgress(1, (NQNPC)null);

                                                }
                                            }
                                        }

                                    }
                                    activeQuest.removeCompletedObjectives(true);
                                }
                                questPlayer.removeCompletedQuests();
                            }
                        }

                    } else {
                        context.getSender().sendMessage(main.parse("<error>Objective TriggerCommand failed. Player <highlight>" + singlePlayerSelector.getSelector()
                                + "</highlight> is not online or was not found!"
                        ));
                    }
                }));

        manager.command(builder.literal("resetAndRemoveQuestForPlayer")
                .argument(SinglePlayerSelectorArgument.of("player"), ArgumentDescription.of("Player name"))
                .argument(QuestSelector.of("quest", main), ArgumentDescription.of("Name of the Quest which should be reset and removed."))
                .meta(CommandMeta.DESCRIPTION, "Removes the quest from a specific player players, removes it from completed quests, resets the accept cooldown and basically everything else.")
                .handler((context) -> {
                    context.getSender().sendMessage(Component.empty());

                    final SinglePlayerSelector singlePlayerSelector = context.get("player");
                    final Player player = singlePlayerSelector.getPlayer();
                    if(player == null){
                        context.getSender().sendMessage(main.parse(
                                "<error>Error: Player <highlight>" + singlePlayerSelector.getSelector() + "</highlight> not found."
                        ));
                        return;
                    }

                    final QuestPlayer questPlayer = main.getQuestPlayerManager().getActiveQuestPlayer(player.getUniqueId());

                    if(questPlayer == null){
                        context.getSender().sendMessage(main.parse(
                                "<error>Error: QuestPlayer of Player <highlight>" + singlePlayerSelector.getSelector() + "</highlight> not found."
                        ));
                        return;
                    }

                    final Quest quest = context.get("quest");
                    final ArrayList<ActiveQuest> activeQuestsToRemove = new ArrayList<>();
                    for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                        if (activeQuest.getQuest().equals(quest)) {
                            activeQuestsToRemove.add(activeQuest);
                            context.getSender().sendMessage(main.parse("<success>Removed the quest as an active quest for the player with the UUID <highlight>"
                                    + questPlayer.getUniqueId().toString() + "</highlight> and name <highlight2>"
                                    + Bukkit.getOfflinePlayer(questPlayer.getUniqueId()).getName() + "</highlight2>."
                            ));

                        }
                    }

                    questPlayer.getActiveQuests().removeAll(activeQuestsToRemove);

                    final ArrayList<CompletedQuest> completedQuestsToRemove = new ArrayList<>();

                    for (final CompletedQuest completedQuest : questPlayer.getCompletedQuests()) {
                        if (completedQuest.getQuest().equals(quest)) {
                            completedQuestsToRemove.add(completedQuest);
                            context.getSender().sendMessage(main.parse("<success>Removed the quest as a completed quest for the player with the UUID <highlight>"
                                    + questPlayer.getUniqueId().toString() + "</highlight> and name <highlight2>"
                                    + Bukkit.getOfflinePlayer(questPlayer.getUniqueId()).getName() + "</highlight2>."
                            ));
                        }

                    }

                    questPlayer.getCompletedQuests().removeAll(completedQuestsToRemove);
                    context.getSender().sendMessage(main.parse("<success>Operation done!"));
                }));

        manager.command(builder.literal("resetAndRemoveQuestForAllPlayers")
                .argument(QuestSelector.of("quest", main), ArgumentDescription.of("Name of the Quest which should be reset and removed."))
                .meta(CommandMeta.DESCRIPTION, "Removes the quest from all players, removes it from completed quests, resets the accept cooldown and basically everything else.")
                .handler((context) -> {
                    context.getSender().sendMessage(Component.empty());

                    final Quest quest = context.get("quest");
                    for (final QuestPlayer questPlayer : main.getQuestPlayerManager().getAllQuestPlayersForAllProfiles()) { //TODO: Doesn't include players which aren't loaded from the database
                        final ArrayList<ActiveQuest> activeQuestsToRemove = new ArrayList<>();
                        for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                            if (activeQuest.getQuest().equals(quest)) {
                                activeQuestsToRemove.add(activeQuest);
                                context.getSender().sendMessage(main.parse("<success>Removed the quest as an active quest for the player with the UUID <highlight>"
                                        + questPlayer.getUniqueId().toString() + "</highlight> and name <highlight2>"
                                        + Bukkit.getOfflinePlayer(questPlayer.getUniqueId()).getName() + "</highlight2>."
                                ));

                            }
                        }

                        questPlayer.getActiveQuests().removeAll(activeQuestsToRemove);

                        final ArrayList<CompletedQuest> completedQuestsToRemove = new ArrayList<>();

                        for (final CompletedQuest completedQuest : questPlayer.getCompletedQuests()) {
                            if (completedQuest.getQuest().equals(quest)) {
                                completedQuestsToRemove.add(completedQuest);
                                context.getSender().sendMessage(main.parse("<success>Removed the quest as a completed quest for the player with the UUID <highlight>"
                                        + questPlayer.getUniqueId().toString() + "</highlight> and name <highlight2>"
                                        + Bukkit.getOfflinePlayer(questPlayer.getUniqueId()).getName() + "</highlight2>."
                                ));
                            }

                        }

                        questPlayer.getCompletedQuests().removeAll(completedQuestsToRemove);
                    }
                    context.getSender().sendMessage(main.parse("<success>Operation done!"));
                }));

        manager.command(builder.literal("resetAndFailQuestForAllPlayers")
                .argument(QuestSelector.of("quest", main), ArgumentDescription.of("Name of the Quest which should be reset and failed."))
                .meta(CommandMeta.DESCRIPTION, "Fails the quest from all players, removes it from completed quests, resets the accept cooldown and basically everything else.")
                .handler((context) -> {
                    context.getSender().sendMessage(Component.empty());

                    final Quest quest = context.get("quest");


                    for (final QuestPlayer questPlayer : main.getQuestPlayerManager().getAllQuestPlayersForAllProfiles()) { //TODO: Doesn't include players which aren't loaded from the database
                        final ArrayList<ActiveQuest> activeQuestsToRemove = new ArrayList<>();
                        for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                            if (activeQuest.getQuest().equals(quest)) {
                                activeQuestsToRemove.add(activeQuest);
                            }
                        }

                        for (final ActiveQuest activeQuest : activeQuestsToRemove) {
                            questPlayer.failQuest(activeQuest);
                            context.getSender().sendMessage(main.parse("<success>Failed the quest as an active quest for the player with the UUID <highlight>"
                                    + questPlayer.getUniqueId().toString() + "</highlight> and name <highlight2>"
                                    + Bukkit.getOfflinePlayer(questPlayer.getUniqueId()).getName() + "</highlight2>."
                            ));

                        }

                        // questPlayer.getActiveQuests().removeAll(activeQuestsToRemove);

                        final ArrayList<CompletedQuest> completedQuestsToRemove = new ArrayList<>();

                        for (final CompletedQuest completedQuest : questPlayer.getCompletedQuests()) {
                            if (completedQuest.getQuest().equals(quest)) {
                                completedQuestsToRemove.add(completedQuest);
                                context.getSender().sendMessage(main.parse("<success>Removed the quest as a completed quest for the player with the UUID <highlight>"
                                        + questPlayer.getUniqueId().toString() + "</highlight> and name <highlight2>"
                                        + Bukkit.getOfflinePlayer(questPlayer.getUniqueId()).getName() + "</highlight2>."
                                ));
                            }

                        }

                        questPlayer.getCompletedQuests().removeAll(completedQuestsToRemove);
                    }
                    context.getSender().sendMessage(main.parse("<success>Operation done!"));

                }));


        manager.command(builder.literal("reload", "load")
                .meta(CommandMeta.DESCRIPTION, "Loads from the NotQuests configuration file.")
                .handler((context) -> {

                    main.getDataManager().loadGeneralConfig();
                    main.getLanguageManager().loadLanguageConfig(false);
                    if(main.getConversationManager() != null) {
                      main.getConversationManager().loadConversationsFromConfig();
                    }else{
                      context.getSender().sendMessage("<error> Loading conversations has been skipped: ConversationManager is null");
                    }
                    context.getSender().sendMessage(Component.empty());
                    context.getSender().sendMessage(main.parse("<success>NotQuests general.yml, language configuration and conversations have been re-loaded. <unimportant>If you want to reload more, please use the ServerUtils plugin (available on spigot) or restart the server. This reload command does not reload the quests file or the database."));
                }));

        manager.command(builder.literal("reload", "load")
                .literal("general.yml")
                .meta(CommandMeta.DESCRIPTION, "Reload the general.yml.")
                .handler((context) -> {
                    main.getDataManager().loadGeneralConfig();
                    context.getSender().sendMessage(Component.empty());
                    context.getSender().sendMessage(main.parse("<success>General.yml has been reloaded."));
                }));

        manager.command(builder.literal("reload", "load")
                .literal("languages")
                .meta(CommandMeta.DESCRIPTION, "Reload the languages from conversations files.")
                .handler((context) -> {
                    main.getLanguageManager().loadLanguageConfig(false);
                    context.getSender().sendMessage(Component.empty());
                    context.getSender().sendMessage(main.parse("<success>Languages have been reloaded."));
                }));

        manager.command(builder.literal("reload", "load")
                .literal("conversations")
                .meta(CommandMeta.DESCRIPTION, "Reload the conversations from conversations files.")
                .handler((context) -> {
                  if(main.getConversationManager() != null) {
                    main.getConversationManager().loadConversationsFromConfig();
                    context.getSender().sendMessage(main.parse("<success>Conversations have been reloaded."));
                  }else{
                    context.getSender().sendMessage("<error> Loading conversations has been skipped: ConversationManager is null");
                  }
                  context.getSender().sendMessage(Component.empty());
                }));

        manager.command(builder.literal("save")
                .meta(CommandMeta.DESCRIPTION, "Saves the NotQuests configuration file.")
                .handler((context) -> {
                    main.getDataManager().saveData();
                    context.getSender().sendMessage(Component.empty());
                    context.getSender().sendMessage(main.parse("<success>NotQuests configuration and player data has been saved"));
                }));

        manager.command(builder.literal("version", "ver", "v", "info")
                .meta(CommandMeta.DESCRIPTION, "Displays the version of the NotQuests plugin you're using.")
                .handler((context) -> {
                    context.getSender().sendMessage(main.parse("<main>NotQuests version: <highlight>"+ main.getMain().getDescription().getVersion() +
                            "\n<main>NotQuests module: <highlight>Paper" +
                            "\n<main>Server version: <highlight>" + Bukkit.getVersion() +
                            "\n<main>Server Brand: <highlight>" + Bukkit.getServer().getName() +
                            "\n<main>Java version: <highlight>" + (System.getProperty("java.version") != null ? System.getProperty("java.version") : "null") +
                            "\n<main>Enabled integrations: <highlight>" + main.getIntegrationsManager().getEnabledIntegrationString()
                    )
                            .hoverEvent(HoverEvent.showText(main.parse("<main>Click to copy this information to your clipboard.")))
                            .clickEvent(ClickEvent.copyToClipboard("**NotQuests version:** "+ main.getMain().getDescription().getVersion() +
                                    "\n**NotQuests module:** Paper" +
                                    "\n**Server version:** " + Bukkit.getVersion() +
                                    "\n**Server Brand:** " + Bukkit.getServer().getName() +
                                    "\n**Java version:** " + (System.getProperty("java.version") != null ? System.getProperty("java.version") : "null") +
                                    "\n**Enabled integrations:**" + main.getIntegrationsManager().getEnabledIntegrationDiscordString()
                            ))
                        );
                }));


        manager.command(builder.literal("editor")

                .meta(CommandMeta.DESCRIPTION, "Opens the web editor.")
                .handler((context) -> {
                    context.getSender().sendMessage(main.parse(
                            "<main>This feature is still in development. The web editor does not work at all yet. Sorry! This command just acts as a placeholder. Consult the NotQuests documentation for a tutorial on how to use NotQuests."
                    ));
                    if(true){
                        return;
                    }
                    context.getSender().sendMessage(main.parse(
                            "<main>Opening the web editor..."
                    ));

                    String jsonResult = main.getWebManager().openEditor();

                   /*context.getSender().sendMessage(main.parse(
                            "<main>Result: " + jsonResult
                    ));*/

                    String editorURL = "";

                    try {
                        JSONParser parser = new JSONParser();
                        Object resultObject = parser.parse(jsonResult);

                        if (resultObject instanceof JSONArray array) {
                            editorURL = "error";
                        }else if (resultObject instanceof JSONObject obj) {
                            editorURL = ""+(long)(obj.get("editor_id"));
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        context.getSender().sendMessage(main.parse(
                                "<error>Failed to parse json!"
                        ));
                        editorURL = "error";
                    }

                    editorURL = "https://editor.notquests.com/editor/" + editorURL;

                    context.getSender().sendMessage(main.parse(
                            "<success>Click following link to open the editor: \n<highlight><click:open_url:" + editorURL + "><hover:show_text:\"<highlight>Click to open the web editor\">" + editorURL
                    ));
                }));


        handleDebugCommands();

        final Command.Builder<CommandSender> categoryCommandsBuilder = builder.literal("categories");
        handleCategoryCommands(categoryCommandsBuilder);
    }

    public void handleCategoryCommands(final Command.Builder<CommandSender> builder){
        manager.command(builder.literal("list")
                .meta(CommandMeta.DESCRIPTION, "Lists all categories.")
                .handler((context) -> {
                    context.getSender().sendMessage(Component.empty());
                    context.getSender().sendMessage(main.parse( "<highlight>All categories:"));
                    int counter = 1;
                    for (final Category category : main.getDataManager().getCategories()) {
                        context.getSender().sendMessage(main.parse("<highlight>" + counter + ".</highlight> <main>" + category.getCategoryFullName()));
                        counter++;
                    }
                }));

        manager.command(builder.literal("create")
                .argument(StringArgument.<CommandSender>newBuilder("Category Name").withSuggestionsProvider(
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[Name of your new category]", "");

                            final ArrayList<String> suggestions = new ArrayList<>();
                            suggestions.add("<Enter new category name>");
                            for(final Category category : main.getDataManager().getCategories()){
                                suggestions.add(category.getCategoryFullName() + ".");
                            }

                            return suggestions;

                        }
                ).single().build(), ArgumentDescription.of("Name of your new category"))
                .meta(CommandMeta.DESCRIPTION, "Creates a new category.")
                .handler((context) -> {
                    String fullNewCategoryIdentifier = context.get("Category Name");
                    fullNewCategoryIdentifier = fullNewCategoryIdentifier.replaceAll("[^0-9a-zA-Z-._]", "_");

                    if(main.getDataManager().getCategory(fullNewCategoryIdentifier) != null){
                        context.getSender().sendMessage(main.parse(
                                "<error>Error: The category <highlight>" + fullNewCategoryIdentifier + "</highlight> already exists!"
                        ));
                        return;
                    }
                    if(fullNewCategoryIdentifier.endsWith(".") || fullNewCategoryIdentifier.startsWith(".")){
                        context.getSender().sendMessage(main.parse(
                                "<error>Error: The category <highlight>" + fullNewCategoryIdentifier + "</highlight> is invalid. It cannot contain a dot at the beginning or the end of the category. Dots are used to create a sub-category of an already existing category."
                        ));
                        return;
                    }

                    if(!fullNewCategoryIdentifier.contains(".")){
                        main.getDataManager().addCategory(
                                main.getDataManager().createCategory(fullNewCategoryIdentifier, null)
                        );
                        context.getSender().sendMessage(main.parse( "<success>Category <highlight>" + fullNewCategoryIdentifier + "</highlight> has successfully been created!"));
                    }else{
                        final String parentCategoryFullIdentifier = fullNewCategoryIdentifier.substring(0, fullNewCategoryIdentifier.lastIndexOf("."));
                        final Category foundParentCategory = main.getDataManager().getCategory(parentCategoryFullIdentifier);
                        if(foundParentCategory == null){
                            context.getSender().sendMessage(main.parse(
                                    "<error>Error: The parent company <highlight>" + parentCategoryFullIdentifier + "</highlight> does not exist."
                            ));
                            return;
                        }
                        main.getDataManager().addCategory(
                                main.getDataManager().createCategory(fullNewCategoryIdentifier.substring(fullNewCategoryIdentifier.lastIndexOf(".")+1), foundParentCategory)
                        );
                        context.getSender().sendMessage(main.parse( "<success>Category <highlight>" + fullNewCategoryIdentifier + "</highlight> has successfully been created!"));
                    }

                }));

        final Command.Builder<CommandSender>  editCategoryBuilder = builder
                .literal("edit", "e")
                .argument(CategorySelector.of("category", main), ArgumentDescription.of("Category Name"));

        handleCategoryEditCommands(editCategoryBuilder);
    }

    public void handleCategoryEditCommands(final Command.Builder<CommandSender> editCategoryBuilder){
      final Command.Builder<CommandSender> predefinedProgressOrderBuilder = editCategoryBuilder.literal("predefinedProgressOrder");

      manager.command(predefinedProgressOrderBuilder.literal("show")
          .meta(CommandMeta.DESCRIPTION, "Shows the current predefined order in which the quests inside this category need to be progressed for your quest.")
          .handler((context) -> {
            final Category category = context.get("category");
            context.getSender().sendMessage(Component.empty());

            final String predefinedProgressOrderString = category.getPredefinedProgressOrder() != null ? (category.getPredefinedProgressOrder().getReadableString())
                : "None"
                ;

            context.getSender().sendMessage(main.parse(
                "<success>Current predefined progress order of category <highlight>" + category.getCategoryFullName()
                    + "</highlight>: <highlight2>" + predefinedProgressOrderString
            ));
          }));

      manager.command(predefinedProgressOrderBuilder.literal("set")
          .literal("none")
          .meta(CommandMeta.DESCRIPTION, "Sets a predefined order in which the quests inside this category need to be progressed for your quest.")
          .handler((context) -> {
            final Category category = context.get("category");
            category.setPredefinedProgressOrder(null, true);
            context.getSender().sendMessage(Component.empty());
            context.getSender().sendMessage(main.parse(
                "<success>Predefined progress order of category <highlight>" + category.getCategoryFullName()
                    + "</highlight> have been removed!"
            ));
          }));

      manager.command(predefinedProgressOrderBuilder.literal("set")
          .literal("firstToLast")
          .meta(CommandMeta.DESCRIPTION, "Sets a predefined order in which the quests inside this category need to be progressed for your quest.")
          .handler((context) -> {
            final Category category = context.get("category");
            category.setPredefinedProgressOrder(PredefinedProgressOrder.firstToLast(), true);
            context.getSender().sendMessage(Component.empty());
            context.getSender().sendMessage(main.parse(
                "<success>Predefined progress order of category <highlight>" + category.getCategoryFullName()
                    + "</highlight> have been set to first to last!"
            ));
          }));

      manager.command(predefinedProgressOrderBuilder.literal("set")
          .literal("lastToFirst")
          .meta(CommandMeta.DESCRIPTION, "Sets a predefined order in which the quests inside this category need to be progressed for your quest.")
          .handler((context) -> {
            final Category category = context.get("category");
            category.setPredefinedProgressOrder(PredefinedProgressOrder.lastToFirst(), true);
            context.getSender().sendMessage(Component.empty());
            context.getSender().sendMessage(main.parse(
                "<success>Predefined progress order of category <highlight>" + category.getCategoryFullName()
                    + "</highlight> have been set to last to first!"
            ));
          }));

      manager.command(predefinedProgressOrderBuilder.literal("set")
          .literal("custom")
          .argument(StringArrayArgument.of("order",
              (context, lastString) -> {
                final List<String> allArgs = context.getRawInput();
                main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "<Enter custom order (numbers of objective IDs separated by space)>", "");
                ArrayList<String> completions = new ArrayList<>();
                final Category category = context.get("category");

                for(final Quest quest : category.getQuests()){
                  completions.add(quest.getIdentifier()+"");
                }

                return completions;
              }
          ), ArgumentDescription.of("Custom order. Example: 2 1 3 4 5 6 7 9 8"))
          .meta(CommandMeta.DESCRIPTION, "Sets a predefined order in which the quests need to be progressed in this category.")
          .handler((context) -> {
            final Category category = context.get("category");
            final String[] order = context.get("order");
            final String orderString = String.join(" ", order);
            final ArrayList<String> orderParsed = new ArrayList<>();
            Collections.addAll(orderParsed, order);

            category.setPredefinedProgressOrder(PredefinedProgressOrder.custom(orderParsed), true);
            context.getSender().sendMessage(Component.empty());
            context.getSender().sendMessage(main.parse(
                "<success>Predefined progress order of category <highlight>" + category.getCategoryFullName()
                    + "</highlight> have been set to custom with this order: " + orderString
            ));
          }));




        manager.command(editCategoryBuilder.literal("displayName")
                .literal("show")
                .meta(CommandMeta.DESCRIPTION, "Shows current Category display name.")
                .handler((context) -> {
                    final Category category = context.get("category");

                    context.getSender().sendMessage(main.parse(
                            "<main>Current display name of Category <highlight>" + category.getCategoryFullName() + "</highlight>: <highlight2>"
                                    + category.getDisplayName()
                    ));
                }));
        manager.command(editCategoryBuilder.literal("displayName")
                .literal("remove")
                .meta(CommandMeta.DESCRIPTION, "Removes current Category display name.")
                .handler((context) -> {
                    final Category category = context.get("category");

                    category.removeDisplayName(true);
                    context.getSender().sendMessage(main.parse("<success>Display name successfully removed from Category <highlight>"
                            + category.getCategoryFullName() + "</highlight>!"
                    ));
                }));

        manager.command(editCategoryBuilder.literal("displayName")
                .literal("set")
                .argument(StringArrayArgument.of("DisplayName",
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "<Enter new Category display name>", "");
                            ArrayList<String> completions = new ArrayList<>();

                            String rawInput = context.getRawInputJoined();
                            if (lastString.startsWith("{")) {
                                completions.addAll(main.getCommandManager().getAdminCommands().placeholders);
                            } else {
                                if(lastString.startsWith("<")){
                                    for(String color : main.getUtilManager().getMiniMessageTokens()){
                                        completions.add("<"+ color +">");
                                        //Now the closings. First we search IF it contains an opening and IF it doesnt contain more closings than the opening
                                        if(rawInput.contains("<"+color+">")){
                                            if(StringUtils.countMatches(rawInput, "<"+color+">") > StringUtils.countMatches(rawInput, "</"+color+">")){
                                                completions.add("</"+ color +">");
                                            }
                                        }
                                    }
                                }else{
                                    completions.add("<Enter new Category display name>");
                                }
                            }
                            return completions;
                        }
                ), ArgumentDescription.of("Category display name"))
                .meta(CommandMeta.DESCRIPTION, "Sets the new display name of the Category.")
                .handler((context) -> {
                    final Category category = context.get("category");

                    final String displayName = String.join(" ", (String[]) context.get("DisplayName"));

                    category.setDisplayName(displayName, true);
                    context.getSender().sendMessage(main.parse("<success>Display name successfully added to category <highlight>"
                            + category.getCategoryFullName() + "</highlight>! New display name: <highlight2>"
                            + category.getDisplayName()
                    ));
                }));

        manager.command(editCategoryBuilder.literal("guiItem")

                .argument(ItemStackSelectionArgument.of("material", main), ArgumentDescription.of("Material of item displayed in the category GUI."))
                .flag(
                        manager.flagBuilder("glow")
                                .withDescription(ArgumentDescription.of("Makes the item have the enchanted glow."))
                )
                .meta(CommandMeta.DESCRIPTION, "Sets the item displayed in the category GUI (default: book).")
                .handler((context) -> {
                    final Category category = context.get("category");
                    final boolean glow = context.flags().isPresent("glow");

                    final ItemStackSelection itemStackSelection= context.get("material");
                    ItemStack guiItem = itemStackSelection.toFirstItemStack();
                    if (guiItem == null) {
                        guiItem = new ItemStack(Material.BOOK, 1);
                    }

                    if (glow) {
                        guiItem.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 1);
                        ItemMeta meta = guiItem.getItemMeta();
                        if (meta == null) {
                            meta = Bukkit.getItemFactory().getItemMeta(guiItem.getType());
                        }
                        if (meta != null) {
                            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                            guiItem.setItemMeta(meta);
                        }

                    }


                    category.setGuiItem(guiItem, true);
                    context.getSender().sendMessage(main.parse(
                            "<success>GUI Item for Category <highlight>" + category.getCategoryFullName()
                                    + "</highlight> has been set to <highlight2>" + guiItem.getType().name() + "</highlight2>!"
                    ));


                }));
    }

    public void handleDebugCommands() {
        manager.command(builder.literal("debug")
                .literal("clearOwnChat")
                .meta(CommandMeta.DESCRIPTION, "Clears your own chat")
                .handler((context) -> {
                    final Component componentToSend = Component.text("").append(Component.newline()).append(Component.newline()).append(Component.newline()).append(Component.newline()).append(Component.newline()).append(Component.newline()).append(Component.newline()).append(Component.newline()).append(Component.newline()).append(Component.newline()).append(Component.newline()).append(Component.newline()).append(Component.newline()).append(Component.newline()).append(Component.newline()).append(Component.newline()).append(Component.newline()).append(Component.newline()).append(Component.newline()).append(Component.newline()).append(Component.newline()).append(Component.newline()).append(Component.newline()).append(Component.newline()).append(Component.newline()).append(Component.newline()).append(Component.newline()).append(Component.newline()).append(Component.newline());
                    context.getSender().sendMessage(componentToSend);
                }));

        manager.command(builder.literal("debug")
                .literal("worldInfo")
                .meta(CommandMeta.DESCRIPTION, "Shows you information about the current world")
                .senderType(Player.class)
                .handler((context) -> {
                    context.getSender().sendMessage(Component.empty());
                    Player player = (Player) context.getSender();
                    player.sendMessage(main.parse(
                            "<main>Current world name: <highlight>" + player.getWorld().getName() + "\n" +
                            "<main>Current world UUD: <highlight>" + player.getWorld().getUID().toString()

                    ));
                }));


        manager.command(builder.literal("debug")
                .literal("loadDataManagerUnsafe")
                .meta(CommandMeta.DESCRIPTION, "Calls the dataManager.reloadData() method. This starts loading all Config-, Quest-, and Player Data. Reload = Load")
                .handler((context) -> {
                    context.getSender().sendMessage(Component.empty());
                    context.getSender().sendMessage(main.parse(
                            "<main>Reloading DataManager..."
                    ));
                    main.getDataManager().reloadData(false);
                    context.getSender().sendMessage(main.parse(
                            "<success>DataManager has been reloaded!"

                    ));
                }));

        manager.command(builder.literal("debug")
                .literal("disablePluginAndSaving")
                .argument(StringArgument.of("reason"), ArgumentDescription.of("Reason for disabling the plugin"))
                .meta(CommandMeta.DESCRIPTION, "Disables NotQuests, saving & loading")
                .handler((context) -> {
                    context.getSender().sendMessage(Component.empty());

                    if(main.getDataManager().isDisabled()){
                        context.getSender().sendMessage(main.parse(
                                "<error>Error: NotQuests is already disabled"
                        ));
                        return;
                    }

                    final String reason = context.get("reason");
                    context.getSender().sendMessage(main.parse(
                            "<main>Disabling NotQuests..."
                    ));
                    main.getDataManager().disablePluginAndSaving(reason);

                }));

        manager.command(builder.literal("debug")
                .literal("showErrorsAndWarnings")
                .flag(
                        manager.flagBuilder("printToConsole")
                                .withDescription(ArgumentDescription.of("Prints the output to the console"))
                )
                .meta(CommandMeta.DESCRIPTION, "Shows the current errors and warnings NotQuests collected")
                .handler((context) -> {
                    final boolean printToConsole = context.flags().contains("printToConsole");


                    if (!printToConsole) {
                        context.getSender().sendMessage(Component.empty());
                        main.getDataManager().sendErrorsAndWarnings(context.getSender());
                    } else {
                        main.getMain().getServer().getConsoleSender().sendMessage(Component.empty());
                        main.getDataManager().sendErrorsAndWarnings(main.getMain().getServer().getConsoleSender());
                        context.getSender().sendMessage(
                                main.parse(
                                        "<success>Error and warnings have been printed to console successfully!"
                                )
                        );
                    }
                }));

        manager.command(builder.literal("debug")
                .literal("enablePluginAndSaving")
                .argument(StringArgument.of("reason"), ArgumentDescription.of("Reason for enabling the plugin"))
                .meta(CommandMeta.DESCRIPTION, "Enables NotQuests, saving & loading")
                .handler((context) -> {
                    context.getSender().sendMessage(Component.empty());

                    if (!main.getDataManager().isDisabled()) {
                        context.getSender().sendMessage(main.parse(
                                "<error>Error: NotQuests is already enabled"
                        ));
                        return;
                    }

                    final String reason = context.get("reason");
                    context.getSender().sendMessage(main.parse(
                            "<main>Enabling NotQuests..."
                    ));
                    main.getDataManager().enablePluginAndSaving(reason);

                }));

        manager.command(builder.literal("debug")
                .literal("testcommand")
                .meta(CommandMeta.DESCRIPTION, "You can probably ignore this.")
                .senderType(Player.class)
                .handler((context) -> {
                    context.getSender().sendMessage(Component.empty());
                    Player player = (Player) context.getSender();
                    ArrayList<Component> history = main.getConversationManager().getChatHistory().get(player.getUniqueId());
                    if (history != null) {
                        Component collectiveComponent = Component.text("");
                        for (Component component : history) {
                            if (component != null) {
                                // audience.sendMessage(component.append(Component.text("fg9023zf729ofz")));
                                collectiveComponent = collectiveComponent.append(component).append(Component.newline());
                            }
                        }

                        context.getSender().sendMessage(collectiveComponent);

                    } else {
                        context.getSender().sendMessage(main.parse("<error>No chat history!"));
                    }

                }));


        manager.command(builder.literal("debug")
                .literal("testcommand2")
                .meta(CommandMeta.DESCRIPTION, "You can probably ignore this.")
                .senderType(Player.class)
                .handler((context) -> {
                    context.getSender().sendMessage(Component.empty());
                    Player player = (Player) context.getSender();
                    ArrayList<Component> history = main.getConversationManager().getChatHistory().get(player.getUniqueId());
                    if (history != null) {
                        Component collectiveComponent = Component.text("");
                        for (int i = 0; i < history.size(); i++) {
                            Component component = history.get(i);
                            if (component != null) {
                                // audience.sendMessage(component.append(Component.text("fg9023zf729ofz")));
                                collectiveComponent = collectiveComponent.append(Component.text(i + ".", NamedTextColor.RED).append(component)).append(Component.newline());
                            }
                        }

                        context.getSender().sendMessage(collectiveComponent);

                    } else {
                        context.getSender().sendMessage(main.parse("<error>No chat history!"));
                    }

                }));

        manager.command(builder.literal("debug")
                .literal("beaconBeam")
                .argument(SinglePlayerSelectorArgument.of("player"), ArgumentDescription.of("Player name"))
                .argument(StringArgument.of("locationName"), ArgumentDescription.of("Location name"))
                .argument(WorldArgument.of("world"), ArgumentDescription.of("World name"))
                .argument(IntegerArgument.newBuilder("x"), ArgumentDescription.of("X coordinate"))
                .argument(IntegerArgument.newBuilder("y"), ArgumentDescription.of("Y coordinate"))
                .argument(IntegerArgument.newBuilder("z"), ArgumentDescription.of("Z coordinate"))
                .meta(CommandMeta.DESCRIPTION, "Spawns a beacon beam")
                .senderType(Player.class)
                .handler((context) -> {

                    final SinglePlayerSelector singlePlayerSelector = context.get("player");
                    final Player player = singlePlayerSelector.getPlayer();

                    final String locationName = context.get("locationName");

                    final World world = context.get("world");
                    final Vector coordinates = new Vector(context.get("x"), context.get("y"), context.get("z"));
                    final Location location = coordinates.toLocation(world);

                    if(player == null){
                        context.getSender().sendMessage(main.parse(
                                "<error>Error: Player not found."
                        ));
                        return;
                    }

                    /*if(main.getPacketManager().isModern()){
                        main.getPacketManager().getModernPacketInjector().spawnBeaconBeam(player, location);
                        main.sendMessage(player, "<success>Beacon beam spawned successfully!");
                    }*/


                    final QuestPlayer questPlayer = main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId());

                    questPlayer.getLocationsAndBeacons().put(locationName, location);
                    questPlayer.updateBeaconLocations(player);

                    main.sendMessage(context.getSender(), "<success>Beacon beam spawned successfully!");


                }));

       /* manager.command(builder.literal("debug")
                .literal("beaconBeamAdvanced")
                .argument(WorldArgument.of("world"), ArgumentDescription.of("World name"))
                .argument(IntegerArgument.newBuilder("x"), ArgumentDescription.of("X coordinate"))
                .argument(IntegerArgument.newBuilder("y"), ArgumentDescription.of("Y coordinate"))
                .argument(IntegerArgument.newBuilder("z"), ArgumentDescription.of("Z coordinate"))

                .meta(CommandMeta.DESCRIPTION, "Spawns a beacon beam")
                .senderType(Player.class)
                .handler((context) -> {

                    World world = context.get("world");
                    final Vector coordinates = new Vector(context.get("x"), context.get("y"), context.get("z"));
                    Location location = coordinates.toLocation(world);





                    Player player = (Player) context.getSender();



                    //Prepare Data
                    Connection connection = main.getPacketManager().getModernPacketInjector().getConnection(main.getPacketManager().getModernPacketInjector().getServerPlayer(player).connection);
                    location = location.clone();
                    BlockPos blockPos = new BlockPos(location.getX(), location.getY(), location.getZ());

                    Chunk chunk = location.getChunk();
                    CraftChunk craftChunk = (CraftChunk)chunk;
                    LevelChunk levelChunk = craftChunk.getHandle();

                    CraftWorld craftWorld = (CraftWorld)world;
                    ServerLevel serverLevel = craftWorld.getHandle();
                    //

                    BlockState beaconBlockState = location.getBlock().getState();
                    beaconBlockState.setType(Material.BEACON);

                    CraftBlockState craftBlockState = (CraftBlockState)beaconBlockState ;
                    net.minecraft.world.level.block.state.BlockState minecraftBlockState = craftBlockState.getHandle();


                    BlockState ironBlockState = location.getBlock().getState();
                    ironBlockState.setType(Material.IRON_BLOCK);


                    /*BlockPos blockPos3 = new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
                    SectionPos sectionPos = SectionPos.of(blockPos3);

                    //SectionPos.sectionRelativePos()


                    main.sendMessage(player, "<highlight>Section Pos: <main>" + sectionPos.asLong()
                    );
                    main.sendMessage(player, "<highlight>Section Pos Chunk x: <main>" + sectionPos.chunk().x
                    );
                    main.sendMessage(player, "<highlight>Section Pos blocks inside size: <main>" + sectionPos.blocksInside().toArray().length);

                    for(Object blockPos1 : sectionPos.blocksInside().toArray()){
                        BlockPos blockPos2 = (BlockPos) blockPos1;
                        BlockEntity blockEntity =  serverLevel.getBlockEntity(blockPos2);
                        if(blockEntity != null){
                            main.sendMessage(player, "<highlight>Section Pos blocks inside: <main>" + blockEntity.getBlockState().getClass().toString()
                            );
                        }

                    }
                    main.sendMessage(player, "<highlight>Section Pos short: <main>" + sectionPos.toShortString()
                    );


                    //PalettedContainer<BlockState> pcB = new PalettedContainer<>();


                    //net.minecraft.world.level.block.state.BlockState[] presetBlockStates = serverLevel.chunkPacketBlockController.getPresetBlockStates(world, chunkPos, b0 << 4);

                    //PalettedContainer<BlockState> datapaletteblock = new PalettedContainer<>(net.minecraft.world.level.block.Block.BLOCK_STATE_REGISTRY, Blocks.AIR.defaultBlockState(), PalettedContainer.Strategy.SECTION_STATES, presetBlockStates);



                   int sectionID = (int) (64/16)+ ( (383/16) / ((location.getBlockY())/16) );

                    main.sendMessage(player, "<highlight>LevelChunkSection Section ID: <main>" + sectionID);


                    LevelChunkSection section = levelChunk.getSection(sectionID);





                    main.sendMessage(player, "<highlight>LevelChunkSection Section Count: <main>" + levelChunk.getSectionsCount()
                    );
                    main.sendMessage(player, "<highlight>LevelChunkSection Sections length: <main>" + levelChunk.getSections().length
                    );

                    /*Iterator<net.minecraft.world.level.block.state.BlockState> it = section.getStates().registry.iterator();

                    ArrayList<String> names = new ArrayList<>();

                    while(it.hasNext()){
                        net.minecraft.world.level.block.state.BlockState blockState1 = it.next();
                        names.add(blockState1.getBlock().getClass().toString());

                    }
                    main.sendMessage(player, "<main>" + names.toString());*/
                    /*

                    ShortSet positions = ShortSet.of()


                    short count = 0;
                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                            for (int y = 0; y < 16; y++) {
                                section.setBlockState(x, y, z, minecraftBlockState);
                                count++;
                            }
                        }
                    }

                    main.sendMessage(player, "<highlight>Index 0 state: <main>" + section.states.get(0).getBlock().getClass().getName()
                    );




                    main.sendMessage(player, "<highlight>Positions: <main>" + positions.toString()
                    );




                    ClientboundSectionBlocksUpdatePacket clientboundSectionBlocksUpdatePacket = new ClientboundSectionBlocksUpdatePacket(
                            sectionPos,
                            positions,
                            section,
                            false);

                    main.sendMessage(player, "<main>Sending packet...");

                    connection.send(clientboundSectionBlocksUpdatePacket);

                    main.sendMessage(player, "<success>Packet sent!");*//*

                    player.sendBlockChange(location, beaconBlockState.getBlockData());

                    player.sendBlockChange(location.clone().add(0,-1,0), ironBlockState.getBlockData());
                    player.sendBlockChange(location.clone().add(-1,-1,0), ironBlockState.getBlockData());
                    player.sendBlockChange(location.clone().add(-1,-1,-1), ironBlockState.getBlockData());
                    player.sendBlockChange(location.clone().add(-1,-1,1), ironBlockState.getBlockData());
                    player.sendBlockChange(location.clone().add(1,-1,0), ironBlockState.getBlockData());
                    player.sendBlockChange(location.clone().add(1,-1,1), ironBlockState.getBlockData());
                    player.sendBlockChange(location.clone().add(1,-1,-1), ironBlockState.getBlockData());
                    player.sendBlockChange(location.clone().add(0,-1,1), ironBlockState.getBlockData());
                    player.sendBlockChange(location.clone().add(0,-1,-1), ironBlockState.getBlockData());

                }));*/
    }

    public void handleQuestPoints() {
        manager.command(builder.literal("questpoints")
                .argument(SinglePlayerSelectorArgument.of("player"), ArgumentDescription.of("Player whose questpoints you want to see."))
                .literal("show", "view")
                .meta(CommandMeta.DESCRIPTION, "Shows questpoints of a player")
                .handler((context) -> {
                    final SinglePlayerSelector singlePlayerSelector = context.get("player");

                    if (singlePlayerSelector.hasAny() && singlePlayerSelector.getPlayer() != null) {

                        final QuestPlayer questPlayer = main.getQuestPlayerManager().getActiveQuestPlayer(singlePlayerSelector.getPlayer().getUniqueId());
                        if (questPlayer != null) {
                            context.getSender().sendMessage(main.parse("<main>Quest points for player <highlight>" + singlePlayerSelector.getPlayer().getName() + "</highlight> <green>(online)</green>: <highlight2>" + questPlayer.getQuestPoints()));
                        } else {
                            context.getSender().sendMessage(main.parse("<error>Seems like the player <highlight>" + singlePlayerSelector.getPlayer().getName() + "</highlight> <green>(online)</green> does not have any quest points!"));
                        }
                    } else {

                        final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(singlePlayerSelector.getSelector());

                        final QuestPlayer questPlayer = main.getQuestPlayerManager().getActiveQuestPlayer(offlinePlayer.getUniqueId());
                        if (questPlayer != null) {
                            context.getSender().sendMessage(main.parse("<main>Quest points for player <highlight>" + offlinePlayer.getName() + "</highlight> <red>(offline)</red>: <highlight2>" + questPlayer.getQuestPoints()));
                        } else {
                            context.getSender().sendMessage(main.parse("<error>Seems like the player <highlight>" + offlinePlayer.getName() + "</highlight> <red>(offline)</red> does not have any quest points!"));
                        }
                    }


                }));


        manager.command(builder.literal("questpoints")
                .argument(SinglePlayerSelectorArgument.of("player"), ArgumentDescription.of("Player to whom you want to add questpoints to."))
                .literal("add")
                .argument(IntegerArgument.of("amount"), ArgumentDescription.of("Amount of questpoints to add"))
                .meta(CommandMeta.DESCRIPTION, "Add questpoints to a player")
                .handler((context) -> {
                    final SinglePlayerSelector singlePlayerSelector = context.get("player");
                    int questPointsToAdd = context.get("amount");

                    if (singlePlayerSelector.hasAny() && singlePlayerSelector.getPlayer() != null) {
                        final QuestPlayer questPlayer = main.getQuestPlayerManager().getOrCreateQuestPlayer(singlePlayerSelector.getPlayer().getUniqueId());
                        long oldQuestPoints = questPlayer.getQuestPoints();
                        questPlayer.addQuestPoints(questPointsToAdd, false);
                        context.getSender().sendMessage(main.parse("<main>Quest points for player <highlight>" + singlePlayerSelector.getPlayer().getName() + "</highlight> <green>(online)</green> have been set from <unimportant>" + oldQuestPoints
                                + "</unimportant> to <highlight2>"+ (oldQuestPoints + questPointsToAdd) + "</highlight2>."));
                    } else {
                        final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(singlePlayerSelector.getSelector());
                        final QuestPlayer questPlayer = main.getQuestPlayerManager().getOrCreateQuestPlayerFromDatabase(offlinePlayer.getUniqueId());
                        final long oldQuestPoints = questPlayer.getQuestPoints();
                        questPlayer.addQuestPoints(questPointsToAdd, false);
                        context.getSender().sendMessage(main.parse(  "<main>Quest points for player <highlight>" + offlinePlayer.getName() + "</highlight> <red>(offline)</red> have been set from <unimportant>" + oldQuestPoints
                                + "</unimportant> to <highlight2>" + (oldQuestPoints + questPointsToAdd) + "</highlight2>."));

                        context.getSender().sendMessage(main.parse("<main>Quest points for player <highlight>" + offlinePlayer.getName() + "</highlight> <red>(offline)</red>: <highlight2>"  + questPlayer.getQuestPoints()));
                    }
                }));

        manager.command(builder.literal("questpoints")
                .argument(SinglePlayerSelectorArgument.of("player"), ArgumentDescription.of("Player of whom you want to remove questpoints from."))
                .literal("remove", "deduct")
                .argument(IntegerArgument.of("amount"), ArgumentDescription.of("Amount of questpoints to remove"))
                .meta(CommandMeta.DESCRIPTION, "Remove questpoints from a player")
                .handler((context) -> {
                    final SinglePlayerSelector singlePlayerSelector = context.get("player");
                    int questPointsToRemove = context.get("amount");

                    if (singlePlayerSelector.hasAny() && singlePlayerSelector.getPlayer() != null) {
                        final QuestPlayer questPlayer = main.getQuestPlayerManager().getOrCreateQuestPlayer(singlePlayerSelector.getPlayer().getUniqueId());
                        long oldQuestPoints = questPlayer.getQuestPoints();
                        questPlayer.removeQuestPoints(questPointsToRemove, false);
                        context.getSender().sendMessage(main.parse(  "<main>Quest points for player <highlight>" + singlePlayerSelector.getPlayer().getName() + "</highlight> <green>(online)</green> have been set from <unimportant>" + oldQuestPoints
                                + "</unimportant> to <highlight2>" + (oldQuestPoints - questPointsToRemove) + "</highlight2>."));
                    } else {
                        final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(singlePlayerSelector.getSelector());
                        final QuestPlayer questPlayer = main.getQuestPlayerManager().getOrCreateQuestPlayerFromDatabase(offlinePlayer.getUniqueId());
                        final long oldQuestPoints = questPlayer.getQuestPoints();
                        questPlayer.removeQuestPoints(questPointsToRemove, false);
                        context.getSender().sendMessage(main.parse( "<main>Quest points for player <highlight>" + offlinePlayer.getName() + "</highlight> <red>(offline)</red> have been set from <unimportant>" + oldQuestPoints
                                + "</unimportant> to <highlight2>" + (oldQuestPoints - questPointsToRemove) + "</highlight2>."));

                        context.getSender().sendMessage(main.parse("<main>Quest points for player <highlight>" + offlinePlayer.getName() + "</highlight> <red>(offline)</red>: <highlight2>" + questPlayer.getQuestPoints()));
                    }
                }));


        manager.command(builder.literal("questpoints")
                .argument(SinglePlayerSelectorArgument.of("player"), ArgumentDescription.of("Player whose questpoints amount you want to change."))
                .literal("set")
                .argument(IntegerArgument.of("amount"), ArgumentDescription.of("New questpoints amount"))
                .meta(CommandMeta.DESCRIPTION, "Set the questpoints of a player")
                .handler((context) -> {
                    final SinglePlayerSelector singlePlayerSelector = context.get("player");
                    int newQuestPointsAmount = context.get("amount");

                    if (singlePlayerSelector.hasAny() && singlePlayerSelector.getPlayer() != null) {
                        final QuestPlayer questPlayer = main.getQuestPlayerManager().getOrCreateQuestPlayer(singlePlayerSelector.getPlayer().getUniqueId());
                        long oldQuestPoints = questPlayer.getQuestPoints();
                        questPlayer.setQuestPoints(newQuestPointsAmount, false);
                        context.getSender().sendMessage(main.parse("<main>Quest points for player <highlight>" + singlePlayerSelector.getPlayer().getName() + "</highlight> <green>(online)</green> have been set from <unimportant>" + oldQuestPoints
                                + "</unimportant> to <highlight2>" + (newQuestPointsAmount) + "</highlight2>."));
                    } else {
                        final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(singlePlayerSelector.getSelector());
                        final QuestPlayer questPlayer = main.getQuestPlayerManager().getOrCreateQuestPlayerFromDatabase(offlinePlayer.getUniqueId());
                        final long oldQuestPoints = questPlayer.getQuestPoints();
                        questPlayer.setQuestPoints(newQuestPointsAmount, false);
                        context.getSender().sendMessage(main.parse("<main>Quest points for player <highlight>" + offlinePlayer.getName() + "</highlight2> <red>(offline)</red> have been set from <unimportant>" + oldQuestPoints
                                + "</unimportant> to <highlight2>" + (newQuestPointsAmount) + "</highlight2>."));
                        context.getSender().sendMessage(main.parse("<main>Quest points for player <highlight>" + offlinePlayer.getName() + "</highlight> <red>(offline)</red>: <highlight2>" + questPlayer.getQuestPoints()));
                    }
                }));


    }


    public void handleConditions() {

        final Command.Builder<CommandSender> conditionsBuilder = builder.literal("conditions");

        final Command.Builder<CommandSender> conditionsEditBuilder = conditionsBuilder
                .literal("edit")
                .argument(ConditionSelector.of("condition", main), ArgumentDescription.of("Condition Name"));


        manager.command(conditionsEditBuilder
                .literal("delete", "remove")
                .meta(CommandMeta.DESCRIPTION, "Removes a condition")
                .handler((context) -> {

                    final Condition condition = context.get("condition");

                    main.getConditionsYMLManager().removeCondition(condition);
                    context.getSender().sendMessage(main.parse( "<success>Condition with the name <highlight>" + condition.getConditionName() + "</highlight> has been deleted."));
                }));

        manager.command(conditionsEditBuilder
                .literal("check")
                .argument(SinglePlayerSelectorArgument.optional("player selector"), ArgumentDescription.of("Player for which the condition will be checked"))
                .meta(CommandMeta.DESCRIPTION, "Checks a condition")
                .handler((context) -> {
                    final Condition condition = context.get("condition");

                    Player player = null;
                    if (context.contains("player selector")) {
                        final SinglePlayerSelector singlePlayerSelector = context.get("player selector");
                        player = singlePlayerSelector.getPlayer();
                    } else if (context.getSender() instanceof Player senderPlayer) {
                        player = senderPlayer;
                    }

                    if (player == null) {
                        context.getSender().sendMessage(main.parse("<error>Error! Player object not found!"));
                        return;
                    }


                    final QuestPlayer questPlayer = main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId());
                    final ConditionResult result = condition.check(questPlayer);
                    final String resultMessage = result.fulfilled() ? "<success>Condition fulfilled!" : result.message() ;
                    context.getSender().sendMessage(main.parse("<success>Condition with the name <highlight>" + condition.getConditionName() + "</highlight> has been checked! Result:</success>\n" + resultMessage));
                }));

        manager.command(conditionsBuilder
                .literal("list")
                .meta(CommandMeta.DESCRIPTION, "Shows all existing conditions.")
                .handler((context) -> {
                    int counter = 1;
                    context.getSender().sendMessage(main.parse("<highlight>All Conditions:"));
                    for (final String conditionIdentifier : main.getConditionsYMLManager().getConditionsAndIdentifiers().keySet()) {
                        final Condition condition = main.getConditionsYMLManager().getCondition(conditionIdentifier);
                        context.getSender().sendMessage(main.parse("<highlight>" + counter + ".</highlight> <main>" + conditionIdentifier));
                        context.getSender().sendMessage(main.parse("  <veryUnimportant>└─</veryUnimportant> <unimportant>Type: <highlight2>" + condition.getConditionType()));
                        counter += 1;
                    }
                }));

        manager.command(conditionsEditBuilder
                .literal("category")
                .literal("show")
                .meta(CommandMeta.DESCRIPTION, "Shows the current category of this Condition.")
                .handler((context) -> {
                    final Condition condition = context.get("condition");

                    context.getSender().sendMessage(main.parse(
                            "<main>Category for condition <highlight>" + condition.getConditionName() + "</highlight>: <highlight2>"
                                    + condition.getCategory().getCategoryFullName() + "</highlight2>."
                    ));
                }));

        manager.command(conditionsEditBuilder
                .literal("category")
                .literal("set")
                .argument(CategorySelector.of("category", main), ArgumentDescription.of("New category for this Condition."))
                .meta(CommandMeta.DESCRIPTION, "Changes the current category of this Condition.")
                .handler((context) -> {
                    final Condition condition = context.get("condition");

                    final Category category = context.get("category");
                    if(condition.getCategory().getCategoryFullName().equalsIgnoreCase(category.getCategoryFullName())){
                        context.getSender().sendMessage(main.parse(
                                "<error> Error: The condition <highlight>" + condition.getConditionName() + "</highlight> already has the category <highlight2>" + condition.getCategory().getCategoryFullName() + "</highlight2>."
                        ));
                        return;
                    }


                    context.getSender().sendMessage(main.parse(
                            "<success>Category for condition <highlight>" + condition.getConditionName() + "</highlight> has successfully been changed from <highlight2>"
                                    + condition.getCategory().getCategoryFullName() + "</highlight2> to <highlight2>" + category.getCategoryFullName() + "</highlight2>!"
                    ));

                    condition.switchCategory(category);

                }));




        manager.command(conditionsEditBuilder.literal("description")
                .literal("set")
                .argument(MiniMessageSelector.<CommandSender>newBuilder("description", main).withPlaceholders().build(), ArgumentDescription.of("Condition description"))
                .meta(CommandMeta.DESCRIPTION, "Sets the new description of the condition.")
                .handler((context) -> {
                    final Condition condition = context.get("condition");

                    final String description = String.join(" ", (String[]) context.get("description"));

                    condition.setDescription(description);

                    condition.getCategory().getConditionsConfig().set("conditions." + condition.getConditionName() + ".description", description);
                    condition.getCategory().saveConditionsConfig();


                    context.getSender().sendMessage(main.parse("<success>Description successfully added to condition  <highlight>" + condition.getConditionName() + "</highlight>! New description: <highlight2>"
                            + condition.getDescription()
                    ));
                }));

      manager.command(conditionsEditBuilder.literal("hidden")
          .literal("set")
          .argument(
              BooleanVariableValueArgument.newBuilder("hiddenStatusExpression", main, null),
              ArgumentDescription.of("Expression"))
          .meta(CommandMeta.DESCRIPTION, "Sets the new hidden status of the condition.")
          .handler((context) -> {
            final Condition condition = context.get("condition");

            final String hiddenStatusExpression = context.get("hiddenStatusExpression");
            final NumberExpression hiddenExpression = new NumberExpression(main, hiddenStatusExpression);


            condition.setHidden(hiddenExpression);

            condition.getCategory().getConditionsConfig().set("conditions." + condition.getConditionName() + ".hiddenStatusExpression", hiddenStatusExpression);
            condition.getCategory().saveConditionsConfig();


            context.getSender().sendMessage(main.parse("<success>Hidden status successfully added to condition  <highlight>" + condition.getConditionName() + "</highlight>! New hidden status: <highlight2>"
                + condition.getHiddenExpression()
            ));
          }));

        manager.command(conditionsEditBuilder.literal("description")
                .literal("remove", "delete")
                .meta(CommandMeta.DESCRIPTION, "Removes the description of the condition.")
                .handler((context) -> {
                    final Condition condition = context.get("condition");

                    condition.removeDescription();

                    condition.getCategory().getConditionsConfig().set("conditions." + condition.getConditionName() + ".description", "");
                    condition.getCategory().saveConditionsConfig();

                    context.getSender().sendMessage(main.parse("<success>Description successfully removed from condition <highlight>" + condition.getConditionName() + "</highlight>! New description: <highlight2>"
                            + condition.getDescription()
                    ));
                }));

        manager.command(conditionsEditBuilder.literal("description")
                .literal("show", "check")
                .meta(CommandMeta.DESCRIPTION, "Shows the description of the condition.")
                .handler((context) -> {
                    final Condition condition = context.get("condition");

                    context.getSender().sendMessage(main.parse("<main>Description of condition <highlight>" + condition.getConditionName() + "</highlight>:\n"
                            + condition.getDescription()
                    ));
                }));
    }

    public void handleActions() {

        final Command.Builder<CommandSender> actionsBuilder = main.getCommandManager().getAdminActionsCommandBuilder();

        final Command.Builder<CommandSender> actionsEditBuilder = main.getCommandManager().getAdminActionsEdituilder();


        manager.command(actionsEditBuilder
                .literal("delete", "remove")
                .meta(CommandMeta.DESCRIPTION, "Removes an action")
                .handler((context) -> {
                    final Action action = context.get("action");

                    main.getActionsYMLManager().removeAction(action);
                    context.getSender().sendMessage(main.parse("<success>Action with the name <highlight2>" + action.getActionName() + "</highlight2> has been deleted."));
                }));

        manager.command(actionsEditBuilder
                .literal("execute", "run")
                .argument(SinglePlayerSelectorArgument.optional("player selector"), ArgumentDescription.of("Player for which the action will be executed"))
                .flag(
                        manager.flagBuilder("ignoreConditions")
                                .withDescription(ArgumentDescription.of("Ignores action conditions"))
                )
                .flag(
                        manager.flagBuilder("silent")
                                .withDescription(ArgumentDescription.of("Doesn't show the action executed message"))
                )
                .meta(CommandMeta.DESCRIPTION, "Executes an action")
                .handler((context) -> {
                    final Action action = context.get("action");

                    Player player = null;
                    if (context.contains("player selector")) {
                        final SinglePlayerSelector singlePlayerSelector = context.get("player selector");
                        player = singlePlayerSelector.getPlayer();
                    } else if (context.getSender() instanceof Player senderPlayer) {
                        player = senderPlayer;
                    }

                    if (player == null) {
                        context.getSender().sendMessage(main.parse("<error>Error! Player object not found!"));
                        return;
                    }

                    if (context.flags().contains("ignoreConditions")) {
                        action.execute(main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId()));
                        context.getSender().sendMessage(main.parse("<success>Action with the name <highlight>" + action.getActionName() + "</highlight> has been executed!"));
                    } else {
                        main.getActionManager().executeActionWithConditions(action, main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId()), context.getSender(), context.flags().contains("silent"));
                    }

                }));

        manager.command(actionsBuilder
                .literal("list")
                .meta(CommandMeta.DESCRIPTION, "Shows all existing actions.")
                .handler((context) -> {
                    int counter = 1;
                    context.getSender().sendMessage(main.parse("<highlight>All Actions:"));
                    for (final String actionIdentifier : main.getActionsYMLManager().getActionsAndIdentifiers().keySet()) {
                        final Action action = main.getActionsYMLManager().getAction(actionIdentifier);
                        context.getSender().sendMessage(main.parse("<highlight>" + counter + ".</highlight> <main>" + actionIdentifier));
                        context.getSender().sendMessage(main.parse("  <veryUnimportant>└─</veryUnimportant> <unimportant>Type:</unimportant> <highlight2>" + action.getActionType()));
                        counter += 1;
                    }
                }));

        manager.command(actionsEditBuilder
                .literal("conditions")
                .literal("clear")
                .meta(CommandMeta.DESCRIPTION, "Removes all conditions from this objective.")
                .handler((context) -> {
                    final Action action = context.get("action");

                    action.clearConditions(action.getCategory().getActionsConfig(), "actions." + action.getActionName());
                    context.getSender().sendMessage(main.parse(
                            "<success>All conditions of action with identifier <highlight>" + action
                                    + "</highlight> have been removed!"
                    ));
                }));

        manager.command(actionsEditBuilder
                .literal("conditions")
                .literal("list", "show")
                .meta(CommandMeta.DESCRIPTION, "Lists all conditions of this objective.")
                .handler((context) -> {
                    final Action action = context.get("action");

                    context.getSender().sendMessage(main.parse(
                            "<highlight>Conditions of action with identifier <highlight2>" + action.getActionName()
                                    + "</highlight2>:"
                    ));
                    int counter = 1;
                    for (Condition condition : action.getConditions()) {
                        context.getSender().sendMessage(main.parse("<highlight>" + counter + ".</highlight> <main>" + condition.getConditionType()));
                        if(context.getSender() instanceof final Player player){
                            context.getSender().sendMessage(main.parse("<main>" + condition.getConditionDescription(main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId()))));
                        }else{
                            context.getSender().sendMessage(main.parse("<main>" + condition.getConditionDescription(null)));
                        }
                        counter += 1;
                    }

                    if (counter == 1) {
                        context.getSender().sendMessage(main.parse("<warn>This action has no conditions!"));
                    }


                }));




        final Command.Builder<CommandSender> editActionConditionsBuilder = actionsEditBuilder
                .literal("conditions")
                .literal("edit")
                .argument(IntegerArgument.<CommandSender>newBuilder("Condition ID").withMin(1).withSuggestionsProvider(
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[Condition ID]", "[...]");

                            ArrayList<String> completions = new ArrayList<>();

                            final Action action = context.get("action");

                            for (final Condition condition : action.getConditions()) {
                                completions.add("" + (action.getConditions().indexOf(condition) + 1));
                            }

                            return completions;
                        }
                ));

        manager.command(editActionConditionsBuilder.literal("delete", "remove")
                .meta(CommandMeta.DESCRIPTION, "Removes a condition from this Action.")
                .handler((context) -> {
                    final Action action = context.get("action");

                    int conditionID = context.get("Condition ID");
                    Condition condition = action.getConditions().get(conditionID-1);

                    if(condition == null){
                        context.getSender().sendMessage(main.parse(
                                "<error>Condition with the ID <highlight>" + conditionID + "</highlight> was not found!"
                        ));
                        return;
                    }

                    action.removeCondition(condition, true, action.getCategory().getActionsConfig(), "actions." + action.getActionName());

                    context.getSender().sendMessage(main.parse("<main>The condition with the ID <highlight>" + conditionID + "</highlight> of Action <highlight2>" + action.getActionName() + "</highlight2> has been removed!"));
                }));


        manager.command(editActionConditionsBuilder.literal("description")
                .literal("set")
                .argument(MiniMessageSelector.<CommandSender>newBuilder("description", main).withPlaceholders().build(), ArgumentDescription.of("Action condition description"))
                .meta(CommandMeta.DESCRIPTION, "Sets the new description of the Action condition.")
                .handler((context) -> {
                    final Action action = context.get("action");

                    int conditionID = context.get("Condition ID");
                    Condition condition = action.getConditions().get(conditionID-1);

                    if(condition == null){
                        context.getSender().sendMessage(main.parse(
                                "<error>Condition with the ID <highlight>" + conditionID + "</highlight> was not found!"
                        ));
                        return;
                    }

                    final String description = String.join(" ", (String[]) context.get("description"));

                    condition.setDescription(description);

                    action.getCategory().getActionsConfig().set("actions." + action.getActionName() + ".conditions." + (action.getConditions().indexOf(condition)+1) + ".description", condition.getDescription());
                    action.getCategory().saveActionsConfig();

                    context.getSender().sendMessage(main.parse("<success>Description successfully added to condition with ID <highlight>" + conditionID + "</highlight> of action <highlight2>"
                            + action.getActionName() + "</highlight2>! New description: <highlight2>"
                            + condition.getDescription()
                    ));
                }));

        manager.command(editActionConditionsBuilder.literal("description")
                .literal("remove", "delete")
                .meta(CommandMeta.DESCRIPTION, "Removes the description of the Action condition.")
                .handler((context) -> {
                    final Action action = context.get("action");

                    int conditionID = context.get("Condition ID");
                    Condition condition = action.getConditions().get(conditionID-1);

                    if(condition == null){
                        context.getSender().sendMessage(main.parse(
                                "<error>Condition with the ID <highlight>" + conditionID + "</highlight> was not found!"
                        ));
                        return;
                    }


                    condition.removeDescription();

                    action.getCategory().getActionsConfig().set("actions." + action.getActionName() + ".conditions." + (action.getConditions().indexOf(condition)+1) + ".description", "");
                    action.getCategory().saveActionsConfig();


                    context.getSender().sendMessage(main.parse("<success>Description successfully removed from condition with ID <highlight>" + conditionID + "</highlight> of action <highlight2>"
                            + action.getActionName() + "</highlight2>! New description: <highlight2>"
                            + condition.getDescription()
                    ));
                }));

        manager.command(editActionConditionsBuilder.literal("description")
                .literal("show", "check")
                .meta(CommandMeta.DESCRIPTION, "Shows the description of the Action condition.")
                .handler((context) -> {
                    final Action action = context.get("action");

                    int conditionID = context.get("Condition ID");
                    Condition condition = action.getConditions().get(conditionID-1);

                    if(condition == null){
                        context.getSender().sendMessage(main.parse(
                                "<error>Condition with the ID <highlight>" + conditionID + "</highlight> was not found!"
                        ));
                        return;
                    }


                    context.getSender().sendMessage(main.parse("<main>Description of condition with ID <highlight>" + conditionID + "</highlight> of action <highlight2>"
                            + action.getActionName() + "</highlight2>:\n"
                            + condition.getDescription()
                    ));
                }));


      manager.command(editActionConditionsBuilder.literal("hidden")
          .literal("set")
          .argument(
              BooleanVariableValueArgument.newBuilder("hiddenStatusExpression", main, null),
              ArgumentDescription.of("Expression"))
          .meta(CommandMeta.DESCRIPTION, "Sets the new hidden status of the Action condition.")
          .handler((context) -> {
            final Action action = context.get("action");

            int conditionID = context.get("Condition ID");
            Condition condition = action.getConditions().get(conditionID-1);

            if(condition == null){
              context.getSender().sendMessage(main.parse(
                  "<error>Condition with the ID <highlight>" + conditionID + "</highlight> was not found!"
              ));
              return;
            }

            final String hiddenStatusExpression = context.get("hiddenStatusExpression");
            final NumberExpression hiddenExpression = new NumberExpression(main, hiddenStatusExpression);

            condition.setHidden(hiddenExpression);

            action.getCategory().getActionsConfig().set("actions." + action.getActionName() + ".conditions." + (action.getConditions().indexOf(condition)+1) + ".hiddenStatusExpression", hiddenStatusExpression);
            action.getCategory().saveActionsConfig();

            context.getSender().sendMessage(main.parse("<success>Hidden status successfully added to condition with ID <highlight>" + conditionID + "</highlight> of action <highlight2>"
                + action.getActionName() + "</highlight2>! New hidden status: <highlight2>"
                + condition.getHiddenExpression()
            ));
          }));















        manager.command(actionsEditBuilder
                .literal("category")
                .literal("show")
                .meta(CommandMeta.DESCRIPTION, "Shows the current category of this Action.")
                .handler((context) -> {
                    final Action action = context.get("action");

                    context.getSender().sendMessage(main.parse(
                            "<main>Category for action <highlight>" + action.getActionName() + "</highlight>: <highlight2>"
                                    + action.getCategory().getCategoryFullName() + "</highlight2>."
                    ));
                }));

        manager.command(actionsEditBuilder
                .literal("category")
                .literal("set")
                .argument(CategorySelector.of("category", main), ArgumentDescription.of("New category for this Action."))
                .meta(CommandMeta.DESCRIPTION, "Changes the current category of this Action.")
                .handler((context) -> {
                    final Action action = context.get("action");
                    final Category category = context.get("category");
                    if(action.getCategory().getCategoryFullName().equalsIgnoreCase(category.getCategoryFullName())){
                        context.getSender().sendMessage(main.parse(
                                "<error> Error: The action <highlight>" + action.getActionName() + "</highlight> already has the category <highlight2>" + action.getCategory().getCategoryFullName() + "</highlight2>."
                        ));
                        return;
                    }


                    context.getSender().sendMessage(main.parse(
                            "<success>Category for action <highlight>" + action.getActionName() + "</highlight> has successfully been changed from <highlight2>"
                                    + action.getCategory().getCategoryFullName() + "</highlight2> to <highlight2>" + category.getCategoryFullName() + "</highlight2>!"
                    ));
                    action.switchCategory(category);

                }));
    }


    public void getProgress(CommandSender sender, Player player, String playerName, ActiveQuest activeQuest) {
        sender.sendMessage(Component.empty());

        if (player != null) {


            QuestPlayer questPlayer = main.getQuestPlayerManager().getActiveQuestPlayer(player.getUniqueId());
            if (questPlayer != null) {

                if (activeQuest != null) {
                    sender.sendMessage(main.parse(
                            "<main>Completed Objectives for Quest <highlight>" + activeQuest.getQuest().getIdentifier() + "</highlight> of player <highlight2>"
                                    + playerName + "</highlight2> <green>(online)</green>:"
                    ));
                    main.getQuestManager().sendCompletedObjectivesAndProgress(questPlayer, activeQuest);

                    sender.sendMessage(main.parse(
                            "<main>>Active Objectives for Quest <highlight>" + activeQuest.getQuest().getIdentifier() + "</highlight> of player <highlight2>"
                                    + playerName + "</highlight2> <green>(online)</green>:"
                    ));
                    main.getQuestManager().sendActiveObjectivesAndProgress(questPlayer, activeQuest, 0);


                } else {
                    sender.sendMessage(main.parse(
                            "<error>Quest was not found or active!"
                    ));
                    sender.sendMessage(main.parse("<main>Active quests of player <highlight>" + player.getName() + "</highlight> <green>(online)</green>:"));
                    int counter = 1;
                    for (ActiveQuest activeQuest1 : questPlayer.getActiveQuests()) {
                        sender.sendMessage(main.parse("<highlight>" + counter + ".</highlight> <main>" + activeQuest1.getQuest().getIdentifier()));
                        counter += 1;
                    }
                    sender.sendMessage(main.parse("<unimportant>Total active quests: <highlight2>" + (counter - 1) + "</highlight2>."));

                }

            } else {
                sender.sendMessage(main.parse("<error>Seems like the player <highlight>" + player.getName() + "</highlight> <green>(online)</green> did not accept any active quests."));
            }


        } else {
            OfflinePlayer offlinePlayer = main.getUtilManager().getOfflinePlayer(playerName);

            QuestPlayer questPlayer = main.getQuestPlayerManager().getActiveQuestPlayer(offlinePlayer.getUniqueId());
            if (questPlayer != null) {


                if (activeQuest != null) {

                    sender.sendMessage(main.parse(
                            "<main>Completed Objectives for Quest <highlight>" + activeQuest.getQuest().getIdentifier() + "</highlight> of player <highlight2>"
                                    + playerName + "</highlight2> <red>(offline)</red>:"
                    ));
                    main.getQuestManager().sendCompletedObjectivesAndProgress(questPlayer, activeQuest);

                    sender.sendMessage(main.parse(
                            "<main>Active Objectives for Quest <highlight>" + activeQuest.getQuest().getIdentifier() + "</highlight> of player <highlight2>"
                                    + playerName + "</highlight2> <red>(offline)</red>:"
                    ));
                    main.getQuestManager().sendActiveObjectivesAndProgress(questPlayer, activeQuest, 0);


                } else {
                    sender.sendMessage(main.parse(
                            "<error>Quest was not found or active!"
                    ));
                    sender.sendMessage(main.parse( "<main>Active quests of player <highlight>" + offlinePlayer.getName() + "</highlight> <green>(online)</green>:"));
                    int counter = 1;
                    for (ActiveQuest activeQuest1 : questPlayer.getActiveQuests()) {
                        sender.sendMessage(main.parse("<highlight>" + counter + ".</highlight> <main>" + activeQuest1.getQuest().getIdentifier()));
                        counter += 1;
                    }

                }
            } else {
                sender.sendMessage(main.parse("<main>Seems like the player <highlight>" + offlinePlayer.getName() + "</highlight> <red>(offline)</red> did not accept any active quests."));
            }
        }

    }
}
