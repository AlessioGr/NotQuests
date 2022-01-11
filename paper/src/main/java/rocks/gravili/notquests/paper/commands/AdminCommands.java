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

package rocks.gravili.notquests.paper.commands;


import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.bukkit.arguments.selector.SinglePlayerSelector;
import cloud.commandframework.bukkit.parsers.WorldArgument;
import cloud.commandframework.bukkit.parsers.selector.SinglePlayerSelectorArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_18_R1.CraftChunk;
import org.bukkit.craftbukkit.v1_18_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R1.block.CraftBlockState;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.ActiveQuestSelector;
import rocks.gravili.notquests.paper.commands.arguments.QuestSelector;
import rocks.gravili.notquests.paper.structs.*;
import rocks.gravili.notquests.paper.structs.actions.Action;
import rocks.gravili.notquests.paper.structs.conditions.Condition;
import rocks.gravili.notquests.paper.structs.objectives.Objective;
import rocks.gravili.notquests.paper.structs.objectives.TriggerCommandObjective;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AdminCommands {
    private final NotQuests main;
    private final PaperCommandManager<CommandSender> manager;
    private final Command.Builder<CommandSender> builder;
    public final ArrayList<String> placeholders;
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
                .meta(CommandMeta.DESCRIPTION, "Create a new quest.")
                .handler((context) -> {
                    context.getSender().sendMessage(main.parse(main.getQuestManager().createQuest(context.get("Quest Name"))));
                }));

        manager.command(builder.literal("delete")
                .argument(StringArgument.<CommandSender>newBuilder("Quest Name").withSuggestionsProvider(
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[Name of the Quest you want to delete]", "");

                            ArrayList<String> completions = new ArrayList<>();

                            for (Quest quest : main.getQuestManager().getAllQuests()) {
                                completions.add(quest.getQuestName());
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
                        context.getSender().sendMessage(main.parse("<error>" + "Player is not online or was not found!"));
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
                        QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
                        if (questPlayer != null) {
                            context.getSender().sendMessage(main.parse("<main>Active quests of player <highlight>" + player.getName() + "</highlight> <green>(online)</green>:"));
                            int counter = 1;
                            for (ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                                context.getSender().sendMessage(main.parse("<highlight>" + counter + ".</highlight> <main>" + activeQuest.getQuest().getQuestName()));
                                counter += 1;
                            }
                            context.getSender().sendMessage(main.parse("<unimportant>Total active quests: <highlight2>" + (counter - 1) + "</highlight2>."));
                        } else {
                            context.getSender().sendMessage(main.parse("<error>Seems like the player <highlight>" + player.getName() + "</highlight> <green>(online)</green> did not accept any active quests."));
                        }
                    } else {
                        OfflinePlayer offlinePlayer = main.getUtilManager().getOfflinePlayer(singlePlayerSelector.getSelector());
                        QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(offlinePlayer.getUniqueId());
                        if (questPlayer != null) {
                            context.getSender().sendMessage(main.parse("<main>Active quests of player <highlight>" + offlinePlayer.getName() + "</highlight> <red>(offline)</red>:"));
                            int counter = 1;
                            for (ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                                context.getSender().sendMessage(main.parse("<highlight>" + counter + ".</highlight> <main>" + activeQuest.getQuest().getQuestName()));
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
                        QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
                        if (questPlayer != null) {
                            context.getSender().sendMessage(main.parse("<main>Completed quests of player <highlight>" + player.getName() + "</highlight> <green>(online)</green>:"));
                            int counter = 1;
                            for (CompletedQuest completedQuest : questPlayer.getCompletedQuests()) {
                                resultDate.setTime(completedQuest.getTimeCompleted());
                                context.getSender().sendMessage(main.parse("<highlight>" + counter + ".</highlight> <highlight2>" + completedQuest.getQuest().getQuestName()
                                        + "</highlight2> <main>Completed: </main> <highlight2>" + resultDate + "</highlight2>"
                                ));
                                counter += 1;
                            }

                            context.getSender().sendMessage(main.parse("<unimportant>Total completed quests: <highlight2>" + (counter - 1) + "</highlight2>."));
                        } else {
                            context.getSender().sendMessage(main.parse("<error>Seems like the player <highlight>" + player.getName() + "</highlight> <green>(online)</green> never completed any quests."));
                        }
                    } else {
                        OfflinePlayer offlinePlayer = main.getUtilManager().getOfflinePlayer(singlePlayerSelector.getSelector());
                        QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(offlinePlayer.getUniqueId());
                        if (questPlayer != null) {
                            context.getSender().sendMessage(main.parse("<main>Completed quests of player <highlight>" + offlinePlayer.getName() + "</highlight> <red>(offline)</red>:"));
                            int counter = 1;
                            for (CompletedQuest completedQuest : questPlayer.getCompletedQuests()) {
                                resultDate.setTime(completedQuest.getTimeCompleted());
                                context.getSender().sendMessage(main.parse("<main><highlight>" + counter + ".</highlight> <highlight2>" + completedQuest.getQuest().getQuestName()
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

                    if (main.getQuestManager().isDebugEnabledPlayer((Player) context.getSender())) {
                        main.getQuestManager().removeDebugEnabledPlayer((Player) context.getSender());
                        context.getSender().sendMessage(main.parse("<success>Your debug mode has been disabled."));
                    } else {
                        main.getQuestManager().addDebugEnabledPlayer((Player) context.getSender());
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
                        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
                        if (questPlayer != null) {
                            questPlayer.failQuest(activeQuest);
                            context.getSender().sendMessage(main.parse(
                                    "<main>The active quest <highlight>" + activeQuest.getQuest().getQuestName() + "</highlight> has been failed for player <highlight2>" + player.getName() + "</highlight2>!"
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
                        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
                        if (questPlayer != null) {
                            questPlayer.forceActiveQuestCompleted(activeQuest);
                            context.getSender().sendMessage(main.parse(
                                    "<success>The active quest <highlight>" + activeQuest.getQuest().getQuestName() + "</highlight> has been completed for player <highlight2>" + player.getName() + "</highlight2>!"
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
                        context.getSender().sendMessage(main.parse("<highlight>" + counter + ".</highlight> " + "<main>" + quest.getQuestName()));
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
                        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
                        if (questPlayer != null) {
                            if (questPlayer.getActiveQuests().size() > 0) {
                                for (ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                                    for (ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                                        if (activeObjective.isUnlocked()) {
                                            if (activeObjective.getObjective() instanceof TriggerCommandObjective triggerCommandObjective) {
                                                if (triggerCommandObjective.getTriggerName().equalsIgnoreCase(triggerName)) {
                                                    activeObjective.addProgress(1, -1);

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

        manager.command(builder.literal("resetAndRemoveQuestForAllPlayers")
                .argument(QuestSelector.of("quest", main), ArgumentDescription.of("Name of the Quest which should be reset and removed."))
                .meta(CommandMeta.DESCRIPTION, "Removes the quest from all players, removes it from completed quests, resets the accept cooldown and basically everything else.")
                .handler((context) -> {
                    context.getSender().sendMessage(Component.empty());

                    final Quest quest = context.get("quest");
                    for (final QuestPlayer questPlayer : main.getQuestPlayerManager().getQuestPlayers()) {
                        final ArrayList<ActiveQuest> activeQuestsToRemove = new ArrayList<>();
                        for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                            if (activeQuest.getQuest().equals(quest)) {
                                activeQuestsToRemove.add(activeQuest);
                                context.getSender().sendMessage(main.parse("<success>Removed the quest as an active quest for the player with the UUID <highlight>"
                                        + questPlayer.getUUID().toString() + "</highlight> and name <highlight2>"
                                        + Bukkit.getOfflinePlayer(questPlayer.getUUID()).getName() + "</highlight2>."
                                ));

                            }
                        }

                        questPlayer.getActiveQuests().removeAll(activeQuestsToRemove);

                        final ArrayList<CompletedQuest> completedQuestsToRemove = new ArrayList<>();

                        for (final CompletedQuest completedQuest : questPlayer.getCompletedQuests()) {
                            if (completedQuest.getQuest().equals(quest)) {
                                completedQuestsToRemove.add(completedQuest);
                                context.getSender().sendMessage(main.parse("<success>Removed the quest as a completed quest for the player with the UUID <highlight>"
                                        + questPlayer.getUUID().toString() + "</highlight> and name <highlight2>"
                                        + Bukkit.getOfflinePlayer(questPlayer.getUUID()).getName() + "</highlight2>."
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


                    for (final QuestPlayer questPlayer : main.getQuestPlayerManager().getQuestPlayers()) {
                        final ArrayList<ActiveQuest> activeQuestsToRemove = new ArrayList<>();
                        for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                            if (activeQuest.getQuest().equals(quest)) {
                                activeQuestsToRemove.add(activeQuest);
                            }
                        }

                        for (final ActiveQuest activeQuest : activeQuestsToRemove) {
                            questPlayer.failQuest(activeQuest);
                            context.getSender().sendMessage(main.parse("<success>Failed the quest as an active quest for the player with the UUID <highlight>"
                                    + questPlayer.getUUID().toString() + "</highlight> and name <highlight2>"
                                    + Bukkit.getOfflinePlayer(questPlayer.getUUID()).getName() + "</highlight2>."
                            ));

                        }

                        // questPlayer.getActiveQuests().removeAll(activeQuestsToRemove);

                        final ArrayList<CompletedQuest> completedQuestsToRemove = new ArrayList<>();

                        for (final CompletedQuest completedQuest : questPlayer.getCompletedQuests()) {
                            if (completedQuest.getQuest().equals(quest)) {
                                completedQuestsToRemove.add(completedQuest);
                                context.getSender().sendMessage(main.parse("<success>Removed the quest as a completed quest for the player with the UUID <highlight>"
                                        + questPlayer.getUUID().toString() + "</highlight> and name <highlight2>"
                                        + Bukkit.getOfflinePlayer(questPlayer.getUUID()).getName() + "</highlight2>."
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
                    main.getLanguageManager().loadLanguageConfig();
                    main.getConversationManager().loadConversationsFromConfig();
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
                    main.getLanguageManager().loadLanguageConfig();
                    context.getSender().sendMessage(Component.empty());
                    context.getSender().sendMessage(main.parse("<success>Languages have been reloaded."));
                }));

        manager.command(builder.literal("reload", "load")
                .literal("conversations")
                .meta(CommandMeta.DESCRIPTION, "Reload the conversations from conversations files.")
                .handler((context) -> {
                    main.getConversationManager().loadConversationsFromConfig();
                    context.getSender().sendMessage(Component.empty());
                    context.getSender().sendMessage(main.parse("<success>Conversations have been reloaded."));
                }));

        manager.command(builder.literal("save")
                .meta(CommandMeta.DESCRIPTION, "Saves the NotQuests configuration file.")
                .handler((context) -> {
                    main.getDataManager().saveData();
                    context.getSender().sendMessage(Component.empty());
                    context.getSender().sendMessage(main.parse("<success>NotQuests configuration and player data has been saved"));
                }));

        manager.command(builder.literal("version")
                .meta(CommandMeta.DESCRIPTION, "Displays the version of the NotQuests plugin you're using.")
                .handler((context) -> {
                    context.getSender().sendMessage(main.parse("<main>Current NotQuests version: <highlight>"+ main.getMain().getDescription().getVersion() + "</highlight> <highlight2>(Paper " + Bukkit.getVersion() + ")"));

                }));


        handleDebugCommands();


    }

    public void handleDebugCommands() {
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

        manager.command(builder.literal("debug")
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

                    main.sendMessage(player, "<success>Packet sent!");*/

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

                }));
    }

    public void handleQuestPoints() {
        manager.command(builder.literal("questpoints")
                .argument(SinglePlayerSelectorArgument.of("player"), ArgumentDescription.of("Player whose questpoints you want to see."))
                .literal("show", "view")
                .meta(CommandMeta.DESCRIPTION, "Shows questpoints of a player")
                .handler((context) -> {
                    final SinglePlayerSelector singlePlayerSelector = context.get("player");

                    if (singlePlayerSelector.hasAny() && singlePlayerSelector.getPlayer() != null) {

                        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(singlePlayerSelector.getPlayer().getUniqueId());
                        if (questPlayer != null) {
                            context.getSender().sendMessage(main.parse("<main>Quest points for player <highlight>" + singlePlayerSelector.getPlayer().getName() + "</highlight> <green>(online)</green>: <highlight2>" + questPlayer.getQuestPoints()));
                        } else {
                            context.getSender().sendMessage(main.parse("<error>Seems like the player <highlight>" + singlePlayerSelector.getPlayer().getName() + "</highlight> <green>(online)</green> does not have any quest points!"));
                        }
                    } else {

                        final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(singlePlayerSelector.getSelector());

                        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(offlinePlayer.getUniqueId());
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
                        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(singlePlayerSelector.getPlayer().getUniqueId());
                        if (questPlayer != null) {
                            long oldQuestPoints = questPlayer.getQuestPoints();
                            questPlayer.addQuestPoints(questPointsToAdd, false);
                            context.getSender().sendMessage(main.parse("<main>Quest points for player <highlight>" + singlePlayerSelector.getPlayer().getName() + "</highlight> <green>(online)</green> have been set from <unimportant>" + oldQuestPoints
                                    + "</unimportant> to <highlight2>"+ (oldQuestPoints + questPointsToAdd) + "</highlight2>."));
                        } else {
                            context.getSender().sendMessage(main.parse("<warn>Seems like the player <highlight>" + singlePlayerSelector.getPlayer().getName() + "</highlight> <green>(online)</green> never accepted any quests! A new QuestPlayer has been created for him."));
                            context.getSender().sendMessage(main.parse("<main>Quest player creation status: <highlight>" + main.getQuestPlayerManager().createQuestPlayer(singlePlayerSelector.getPlayer().getUniqueId())));
                            final QuestPlayer newQuestPlayer = main.getQuestPlayerManager().getQuestPlayer(singlePlayerSelector.getPlayer().getUniqueId());
                            if (newQuestPlayer != null) {
                                long oldQuestPoints = newQuestPlayer.getQuestPoints();
                                newQuestPlayer.addQuestPoints(questPointsToAdd, false);
                                context.getSender().sendMessage(main.parse(
                                        "<success>Quest points for player <highlight>"+ singlePlayerSelector.getPlayer().getName()
                                                + "</highlight> <green>(online)</green> have been set from <unimportant>" + oldQuestPoints + "</unimportant> to <highlight2>"
                                                + (oldQuestPoints + questPointsToAdd) + "</highlight2>."
                                ));
                            } else {
                                context.getSender().sendMessage(main.parse("<error>Something went wrong during the questPlayer creation!"));
                            }
                        }
                    } else {
                        final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(singlePlayerSelector.getSelector());
                        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(offlinePlayer.getUniqueId());
                        if (questPlayer != null) {
                            final long oldQuestPoints = questPlayer.getQuestPoints();
                            questPlayer.addQuestPoints(questPointsToAdd, false);
                            context.getSender().sendMessage(main.parse(  "<main>Quest points for player <highlight>" + offlinePlayer.getName() + "</highlight> <red>(offline)</red> have been set from <unimportant>" + oldQuestPoints
                                    + "</unimportant> to <highlight2>" + (oldQuestPoints + questPointsToAdd) + "</highlight2>."));
                        } else {
                            context.getSender().sendMessage(main.parse("<warn>Seems like the player <highlight>" + offlinePlayer.getName() + "</highlight> <red>(offline)</red> never accepted any quests! A new QuestPlayer has been created for him."));
                            context.getSender().sendMessage(main.parse("<main>Quest player creation status: <highlight>" + main.getQuestPlayerManager().createQuestPlayer(offlinePlayer.getUniqueId())));

                            final QuestPlayer newQuestPlayer = main.getQuestPlayerManager().getQuestPlayer(offlinePlayer.getUniqueId());
                            if (newQuestPlayer != null) {
                                final long oldQuestPoints = newQuestPlayer.getQuestPoints();
                                newQuestPlayer.addQuestPoints(questPointsToAdd, false);
                                context.getSender().sendMessage(main.parse(
                                        "<success>Quest points for player <highlight>" + offlinePlayer.getName()
                                                + "</highlight> <red>(offline)</red> have been set from <unimportant>" + oldQuestPoints + "</unimportant> to <highlight2>"
                                                + (oldQuestPoints + questPointsToAdd) + "</highlight2>."));
                            } else {
                                context.getSender().sendMessage(main.parse("<error>Something went wrong during the questPlayer creation!"));
                            }
                        }
                        if (questPlayer != null) {
                            context.getSender().sendMessage(main.parse("<main>Quest points for player <highlight>" + offlinePlayer.getName() + "</highlight> <red>(offline)</red>: <highlight2>"  + questPlayer.getQuestPoints()));
                        } else {
                            context.getSender().sendMessage(main.parse("<error>Seems like the player <highlight>" + offlinePlayer.getName() + "</highlight> <red>(offline)</red> does not have any quest points!"));
                        }
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
                        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(singlePlayerSelector.getPlayer().getUniqueId());
                        if (questPlayer != null) {
                            long oldQuestPoints = questPlayer.getQuestPoints();
                            questPlayer.removeQuestPoints(questPointsToRemove, false);
                            context.getSender().sendMessage(main.parse(  "<main>Quest points for player <highlight>" + singlePlayerSelector.getPlayer().getName() + "</highlight> <green>(online)</green> have been set from <unimportant>" + oldQuestPoints
                                    + "</unimportant> to <highlight2>" + (oldQuestPoints - questPointsToRemove) + "</highlight2>."));
                        } else {
                            context.getSender().sendMessage(main.parse("<warn>Seems like the player <highlight>" + singlePlayerSelector.getPlayer().getName() + "</highlight> <green>(online)</green> never accepted any quests! A new QuestPlayer has been created for him."));
                            context.getSender().sendMessage(main.parse("<main>Quest player creation status: <highlight>" + main.getQuestPlayerManager().createQuestPlayer(singlePlayerSelector.getPlayer().getUniqueId())));
                            final QuestPlayer newQuestPlayer = main.getQuestPlayerManager().getQuestPlayer(singlePlayerSelector.getPlayer().getUniqueId());
                            if (newQuestPlayer != null) {
                                long oldQuestPoints = newQuestPlayer.getQuestPoints();
                                newQuestPlayer.removeQuestPoints(questPointsToRemove, false);
                                context.getSender().sendMessage(main.parse(
                                        "<success>Quest points for player <highlight>" + singlePlayerSelector.getPlayer().getName()
                                                + "</highlight> <green>(online)</green> have been set from <unimportant>" + oldQuestPoints + "</unimportant> to <highlight2>"
                                                + (oldQuestPoints - questPointsToRemove) + "</highlight2>."
                                ));
                            } else {
                                context.getSender().sendMessage(main.parse( "<error>Something went wrong during the questPlayer creation!"));
                            }
                        }
                    } else {
                        final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(singlePlayerSelector.getSelector());
                        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(offlinePlayer.getUniqueId());
                        if (questPlayer != null) {
                            final long oldQuestPoints = questPlayer.getQuestPoints();
                            questPlayer.removeQuestPoints(questPointsToRemove, false);
                            context.getSender().sendMessage(main.parse( "<main>Quest points for player <highlight>" + offlinePlayer.getName() + "</highlight> <red>(offline)</red> have been set from <unimportant>" + oldQuestPoints
                                    + "</unimportant> to <highlight2>" + (oldQuestPoints - questPointsToRemove) + "</highlight2>."));
                        } else {
                            context.getSender().sendMessage(main.parse( "<warn>Seems like the player <highlight>" + offlinePlayer.getName() + "</highlight> <red>(offline)</red> never accepted any quests! A new QuestPlayer has been created for him."));
                            context.getSender().sendMessage(main.parse("<main>Quest player creation status: <highlight>" + main.getQuestPlayerManager().createQuestPlayer(offlinePlayer.getUniqueId())));

                            final QuestPlayer newQuestPlayer = main.getQuestPlayerManager().getQuestPlayer(offlinePlayer.getUniqueId());
                            if (newQuestPlayer != null) {
                                final long oldQuestPoints = newQuestPlayer.getQuestPoints();
                                newQuestPlayer.removeQuestPoints(questPointsToRemove, false);
                                context.getSender().sendMessage(main.parse(
                                        "<success>Quest points for player <highlight>" + offlinePlayer.getName()
                                                + "</highlight> <red>(offline)</red> have been set from <unimportant>" + oldQuestPoints + "</unimportant> to <highlight2>"
                                                + (oldQuestPoints - questPointsToRemove) + "</highlight2>."));
                            } else {
                                context.getSender().sendMessage(main.parse("<error>Something went wrong during the questPlayer creation!"));
                            }
                        }
                        if (questPlayer != null) {
                            context.getSender().sendMessage(main.parse("<main>Quest points for player <highlight>" + offlinePlayer.getName() + "</highlight> <red>(offline)</red>: <highlight2>" + questPlayer.getQuestPoints()));
                        } else {
                            context.getSender().sendMessage(main.parse("<error>Seems like the player <highlight>" + offlinePlayer.getName() + "</highlight> <red>(offline)</red> does not have any quest points!"));
                        }
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
                        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(singlePlayerSelector.getPlayer().getUniqueId());
                        if (questPlayer != null) {
                            long oldQuestPoints = questPlayer.getQuestPoints();
                            questPlayer.setQuestPoints(newQuestPointsAmount, false);
                            context.getSender().sendMessage(main.parse("<main>Quest points for player <highlight>" + singlePlayerSelector.getPlayer().getName() + "</highlight> <green>(online)</green> have been set from <unimportant>" + oldQuestPoints
                                    + "</unimportant> to <highlight2>" + (newQuestPointsAmount) + "</highlight2>."));
                        } else {
                            context.getSender().sendMessage(main.parse( "<warn>Seems like the player <highlight>" + singlePlayerSelector.getPlayer().getName() + "</highlight> <green>(online)</green> never accepted any quests! A new QuestPlayer has been created for him."));
                            context.getSender().sendMessage(main.parse("<main>Quest player creation status: <highlight>" + main.getQuestPlayerManager().createQuestPlayer(singlePlayerSelector.getPlayer().getUniqueId())));
                            final QuestPlayer newQuestPlayer = main.getQuestPlayerManager().getQuestPlayer(singlePlayerSelector.getPlayer().getUniqueId());
                            if (newQuestPlayer != null) {
                                long oldQuestPoints = newQuestPlayer.getQuestPoints();
                                newQuestPlayer.setQuestPoints(newQuestPointsAmount, false);
                                context.getSender().sendMessage(main.parse(
                                        "<success>Quest points for player <highlight>" + singlePlayerSelector.getPlayer().getName()
                                                + "</highlight> <green>(online)</green> have been set from <unimportant>" + oldQuestPoints + "</unimportant> to <highlight2>"
                                                + (newQuestPointsAmount) + "</highlight2>."
                                ));
                            } else {
                                context.getSender().sendMessage(main.parse("<error>Something went wrong during the questPlayer creation!"));
                            }
                        }
                    } else {
                        final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(singlePlayerSelector.getSelector());
                        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(offlinePlayer.getUniqueId());
                        if (questPlayer != null) {
                            final long oldQuestPoints = questPlayer.getQuestPoints();
                            questPlayer.setQuestPoints(newQuestPointsAmount, false);
                            context.getSender().sendMessage(main.parse("<main>Quest points for player <highlight>" + offlinePlayer.getName() + "</highlight2> <red>(offline)</red> have been set from <unimportant>" + oldQuestPoints
                                    + "</unimportant> to <highlight2>" + (newQuestPointsAmount) + "</highlight2>."));
                        } else {
                            context.getSender().sendMessage(main.parse("<warn>Seems like the player <highlight>" + offlinePlayer.getName() + "</highlight> <red>(offline)</red> never accepted any quests! A new QuestPlayer has been created for him."));
                            context.getSender().sendMessage(main.parse("<main>Quest player creation status: <highlight>" + main.getQuestPlayerManager().createQuestPlayer(offlinePlayer.getUniqueId())));

                            final QuestPlayer newQuestPlayer = main.getQuestPlayerManager().getQuestPlayer(offlinePlayer.getUniqueId());
                            if (newQuestPlayer != null) {
                                final long oldQuestPoints = newQuestPlayer.getQuestPoints();
                                newQuestPlayer.setQuestPoints(newQuestPointsAmount, false);
                                context.getSender().sendMessage(main.parse(
                                        "<success>Quest points for player <highlight>" + offlinePlayer.getName()
                                                + "</highlight> <red>(offline)</red> have been set from <unimportant>" + oldQuestPoints + "</unimportant> to <highlight2>"
                                                + (newQuestPointsAmount) + "</highlight2>."));
                            } else {
                                context.getSender().sendMessage(main.parse("<error>Something went wrong during the questPlayer creation!"));
                            }
                        }
                        if (questPlayer != null) {
                            context.getSender().sendMessage(main.parse("<main>Quest points for player <highlight>" + offlinePlayer.getName() + "</highlight> <red>(offline)</red>: <highlight2>" + questPlayer.getQuestPoints()));
                        } else {
                            context.getSender().sendMessage(main.parse( "<error>Seems like the player <highlight>" + offlinePlayer.getName() + "</highlight> <red>(offline)</red> does not have any quest points!"));
                        }
                    }
                }));


    }


    public void handleConditions() {

        manager.command(builder.literal("conditions")
                .literal("edit")
                .argument(StringArgument.<CommandSender>newBuilder("Condition Identifier").withSuggestionsProvider(
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[Condition Identifier (name)]", "[...]");

                            return new ArrayList<>(main.getConditionsYMLManager().getConditionsAndIdentifiers().keySet());

                        }
                ).single().build(), ArgumentDescription.of("Condition Identifier"))
                .literal("delete", "remove")
                .meta(CommandMeta.DESCRIPTION, "Removes a condition")
                .handler((context) -> {

                    final String conditionIdentifier = context.get("Condition Identifier");

                    if (main.getConditionsYMLManager().getCondition(conditionIdentifier) != null) {

                        main.getConditionsYMLManager().removeCondition(conditionIdentifier);
                        context.getSender().sendMessage(main.parse( "<success>Condition with the name <highlight>" + conditionIdentifier + "</highlight> has been deleted."));

                    } else {
                        context.getSender().sendMessage(main.parse("<error>Error! Condition with the name <highlight>" + conditionIdentifier + "</highlight> does not exist!"));
                    }
                }));

        manager.command(builder.literal("conditions")
                .literal("edit")
                .argument(StringArgument.<CommandSender>newBuilder("Condition Identifier").withSuggestionsProvider(
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[Condition Identifier (name)]", "[...]");

                            return new ArrayList<>(main.getConditionsYMLManager().getConditionsAndIdentifiers().keySet());

                        }
                ).single().build(), ArgumentDescription.of("Condition Identifier"))
                .literal("check")
                .argument(SinglePlayerSelectorArgument.optional("player selector"), ArgumentDescription.of("Player for which the condition will be checked"))
                .meta(CommandMeta.DESCRIPTION, "Checks a condition")
                .handler((context) -> {

                    final String conditionIdentifier = context.get("Condition Identifier");
                    final Condition foundCondition = main.getConditionsYMLManager().getCondition(conditionIdentifier);

                    if (foundCondition != null) {

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


                        QuestPlayer questPlayer = main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId());
                        String result = foundCondition.check(questPlayer);
                        if (result.isBlank()) {
                            result = "<success>Condition fulfilled!";
                        }
                        context.getSender().sendMessage(main.parse("<success>Condition with the name <highlight>" + conditionIdentifier + "</highlight> has been checked! Result:</success>\n" + result));


                    } else {
                        context.getSender().sendMessage(main.parse("<error>Error! Condition with the name <highlight>" + conditionIdentifier + "</highlight> does not exist!"));
                    }
                }));

        manager.command(builder.literal("conditions")
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
    }

    public void handleActions() {

        manager.command(builder.literal("actions")
                .literal("edit")
                .argument(StringArgument.<CommandSender>newBuilder("Action Identifier").withSuggestionsProvider(
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[Action Identifier (name)]", "[...]");

                            return new ArrayList<>(main.getActionsYMLManager().getActionsAndIdentifiers().keySet());

                        }
                ).single().build(), ArgumentDescription.of("Action Identifier"))
                .literal("delete", "remove")
                .meta(CommandMeta.DESCRIPTION, "Removes an action")
                .handler((context) -> {
                    final String actionIdentifier = context.get("Action Identifier");

                    if (main.getActionsYMLManager().getAction(actionIdentifier) != null) {

                        main.getActionsYMLManager().removeAction(actionIdentifier);
                        context.getSender().sendMessage(main.parse("<success>Action with the name <highlight2>" + actionIdentifier + "</highlight2> has been deleted."));

                    } else {
                        context.getSender().sendMessage(main.parse("<error>Error! Action with the name <highlight2>" + actionIdentifier + "</highlight2> does not exist!"));
                    }

                }));

        manager.command(builder.literal("actions")
                .literal("edit")
                .argument(StringArgument.<CommandSender>newBuilder("Action Identifier").withSuggestionsProvider(
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[Action Identifier (name)]", "[...]");

                            return new ArrayList<>(main.getActionsYMLManager().getActionsAndIdentifiers().keySet());

                        }
                ).single().build(), ArgumentDescription.of("Action Identifier"))
                .literal("execute", "run")
                .argument(SinglePlayerSelectorArgument.optional("player selector"), ArgumentDescription.of("Player for which the action will be executed"))
                .flag(
                        manager.flagBuilder("ignoreConditions")
                                .withDescription(ArgumentDescription.of("Ignores action conditions"))
                )
                .meta(CommandMeta.DESCRIPTION, "Executes an action")
                .handler((context) -> {
                    final String actionIdentifier = context.get("Action Identifier");
                    final Action foundAction = main.getActionsYMLManager().getAction(actionIdentifier);

                    if (foundAction != null) {

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
                            foundAction.execute(player);
                            context.getSender().sendMessage(main.parse("<success>Action with the name <highlight>" + actionIdentifier + "</highlight> has been executed!"));
                        } else {
                            main.getActionManager().executeActionWithConditions(foundAction, main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId()), player, false);
                        }


                    } else {
                        context.getSender().sendMessage(main.parse("<error>Error! Action with the name <highlight>" + actionIdentifier + "</highlight> does not exist!"));
                    }

                }));

        manager.command(builder.literal("actions")
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

        manager.command(builder.literal("actions")
                .literal("edit")
                .argument(StringArgument.<CommandSender>newBuilder("Action Identifier").withSuggestionsProvider(
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[Action Identifier (name)]", "[...]");

                            return new ArrayList<>(main.getActionsYMLManager().getActionsAndIdentifiers().keySet());

                        }
                ).single().build(), ArgumentDescription.of("Action Identifier"))
                .literal("conditions")
                .literal("clear")
                .meta(CommandMeta.DESCRIPTION, "Removes all conditions from this objective.")
                .handler((context) -> {

                    final String actionIdentifier = context.get("Action Identifier");
                    final Action foundAction = main.getActionsYMLManager().getAction(actionIdentifier);

                    if (foundAction != null) {
                        foundAction.clearConditions(main.getActionsYMLManager().getActionsConfig(), "actions." + actionIdentifier);
                        context.getSender().sendMessage(main.parse(
                                "<success>All conditions of action with identifier <highlight>" + actionIdentifier
                                        + "</highlight> have been removed!"
                        ));
                    } else {
                        context.getSender().sendMessage(main.parse("<error>Error! Action with the name <highlight>" + actionIdentifier + "</highlight> does not exist!"));
                    }


                }));

        manager.command(builder.literal("actions")
                .literal("edit")
                .argument(StringArgument.<CommandSender>newBuilder("Action Identifier").withSuggestionsProvider(
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[Action Identifier (name)]", "[...]");

                            return new ArrayList<>(main.getActionsYMLManager().getActionsAndIdentifiers().keySet());

                        }
                ).single().build(), ArgumentDescription.of("Action Identifier"))
                .literal("conditions")
                .literal("list", "show")
                .meta(CommandMeta.DESCRIPTION, "Lists all conditions of this objective.")
                .handler((context) -> {
                    final String actionIdentifier = context.get("Action Identifier");
                    final Action foundAction = main.getActionsYMLManager().getAction(actionIdentifier);

                    if (foundAction != null) {
                        context.getSender().sendMessage(main.parse(
                                "<highlight>Conditions of action with identifier <highlight2>" + actionIdentifier
                                        + "</highlight2>:>"
                        ));
                        int counter = 1;
                        for (Condition condition : foundAction.getConditions()) {
                            context.getSender().sendMessage(main.parse("<highlight>" + counter + ".</highlight> <main>" + condition.getConditionType()));
                            if(context.getSender() instanceof Player player){
                                context.getSender().sendMessage(main.parse("<main>" + condition.getConditionDescription(player)));
                            }else{
                                context.getSender().sendMessage(main.parse("<main>" + condition.getConditionDescription(null)));
                            }
                            counter += 1;
                        }

                        if (counter == 1) {
                            context.getSender().sendMessage(main.parse("<warn>This action has no conditions!"));
                        }
                    } else {
                        context.getSender().sendMessage(main.parse("<error>Error! Action with the name <highlight>" + actionIdentifier + "</highlight> does not exist!"));
                    }


                }));
    }


    public void getProgress(CommandSender sender, Player player, String playerName, ActiveQuest activeQuest) {
        sender.sendMessage(Component.empty());

        if (player != null) {


            QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
            if (questPlayer != null) {

                if (activeQuest != null) {
                    sender.sendMessage(main.parse(
                            "<main>Completed Objectives for Quest <highlight>" + activeQuest.getQuest().getQuestName() + "</highlight> of player <highlight2>"
                                    + playerName + "</highlight2> <green>(online)</green>:"
                    ));
                    main.getQuestManager().sendCompletedObjectivesAndProgress((Player) sender, activeQuest);

                    sender.sendMessage(main.parse(
                            "<main>>Active Objectives for Quest <highlight>" + activeQuest.getQuest().getQuestName() + "</highlight> of player <highlight2>"
                                    + playerName + "</highlight2> <green>(online)</green>:"
                    ));
                    main.getQuestManager().sendActiveObjectivesAndProgress((Player) sender, activeQuest);


                } else {
                    sender.sendMessage(main.parse(
                            "<error>Quest was not found or active!"
                    ));
                    sender.sendMessage(main.parse("<main>Active quests of player <highlight>" + player.getName() + "</highlight> <green>(online)</green>:"));
                    int counter = 1;
                    for (ActiveQuest activeQuest1 : questPlayer.getActiveQuests()) {
                        sender.sendMessage(main.parse("<highlight>" + counter + ".</highlight> <main>" + activeQuest1.getQuest().getQuestName()));
                        counter += 1;
                    }
                    sender.sendMessage(main.parse("<unimportant>Total active quests: <highlight2>" + (counter - 1) + "</highlight2>."));

                }

            } else {
                sender.sendMessage(main.parse("<error>Seems like the player <highlight>" + player.getName() + "</highlight> <green>(online)</green> did not accept any active quests."));
            }


        } else {
            OfflinePlayer offlinePlayer = main.getUtilManager().getOfflinePlayer(playerName);

            QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(offlinePlayer.getUniqueId());
            if (questPlayer != null) {


                if (activeQuest != null) {

                    sender.sendMessage(main.parse(
                            "<main>Completed Objectives for Quest <highlight>" + activeQuest.getQuest().getQuestName() + "</highlight> of player <highlight2>"
                                    + playerName + "</highlight2> <red>(offline)</red>:"
                    ));
                    main.getQuestManager().sendCompletedObjectivesAndProgress((Player) sender, activeQuest);

                    sender.sendMessage(main.parse(
                             "<main>Active Objectives for Quest <highlight>" + activeQuest.getQuest().getQuestName() + "</highlight> of player <highlight2>"
                                    + playerName + "</highlight2> <red>(offline)</red>:"
                    ));
                    main.getQuestManager().sendActiveObjectivesAndProgress((Player) sender, activeQuest);


                } else {
                    sender.sendMessage(main.parse(
                            "<error>Quest was not found or active!"
                    ));
                    sender.sendMessage(main.parse( "<main>Active quests of player <highlight>" + offlinePlayer.getName() + "</highlight> <green>(online)</green>:"));
                    int counter = 1;
                    for (ActiveQuest activeQuest1 : questPlayer.getActiveQuests()) {
                        sender.sendMessage(main.parse("<highlight>" + counter + ".</highlight> <main>" + activeQuest1.getQuest().getQuestName()));
                        counter += 1;
                    }

                }
            } else {
                sender.sendMessage(main.parse("<main>Seems like the player <highlight>" + offlinePlayer.getName() + "</highlight> <red>(offline)</red> did not accept any active quests."));
            }
        }

    }
}
