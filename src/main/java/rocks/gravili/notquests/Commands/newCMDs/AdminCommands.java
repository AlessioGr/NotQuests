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

package rocks.gravili.notquests.Commands.newCMDs;


import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.arguments.standard.StringArrayArgument;
import cloud.commandframework.bukkit.arguments.selector.SinglePlayerSelector;
import cloud.commandframework.bukkit.parsers.selector.SinglePlayerSelectorArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.Commands.newCMDs.arguments.ActiveQuestSelector;
import rocks.gravili.notquests.Commands.newCMDs.arguments.QuestSelector;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.*;
import rocks.gravili.notquests.Structs.Objectives.Objective;
import rocks.gravili.notquests.Structs.Objectives.TriggerCommandObjective;
import rocks.gravili.notquests.Structs.Triggers.Action;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static rocks.gravili.notquests.Commands.NotQuestColors.*;

public class AdminCommands {
    private final NotQuests main;
    private final PaperCommandManager<CommandSender> manager;
    private final Command.Builder<CommandSender> builder;
    public final ArrayList<String> placeholders;
    protected final MiniMessage miniMessage = MiniMessage.miniMessage();
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
                            final Audience audience = main.adventure().sender(context.getSender());
                            main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "[New Quest Name]", "");

                            ArrayList<String> completions = new ArrayList<>();

                            completions.add("<Enter new Quest Name>");
                            return completions;
                        }
                ).single().build(), ArgumentDescription.of("Quest Name"))
                .meta(CommandMeta.DESCRIPTION, "Create a new quest.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    audience.sendMessage(miniMessage.parse(main.getQuestManager().createQuest(context.get("Quest Name"))));
                }));


        manager.command(builder.literal("delete")
                .argument(StringArgument.<CommandSender>newBuilder("Quest Name").withSuggestionsProvider(
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            final Audience audience = main.adventure().sender(context.getSender());
                            main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "[Name of the Quest you want to delete]", "");

                            ArrayList<String> completions = new ArrayList<>();

                            for (Quest quest : main.getQuestManager().getAllQuests()) {
                                completions.add(quest.getQuestName());
                            }
                            return completions;
                        }
                ).single().build(), ArgumentDescription.of("Quest Name"))
                .meta(CommandMeta.DESCRIPTION, "Delete an existing Quest.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    audience.sendMessage(miniMessage.parse(main.getQuestManager().deleteQuest(context.get("Quest Name"))));
                }));


        handleActions();

        manager.command(builder.literal("give")
                .argument(SinglePlayerSelectorArgument.of("player"), ArgumentDescription.of("Player who should start the quest."))
                .argument(QuestSelector.of("quest", main), ArgumentDescription.of("Name of the Quest the player should start."))
                .meta(CommandMeta.DESCRIPTION, "Gives a player a quest without bypassing the Quest requirements.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    final SinglePlayerSelector singlePlayerSelector = context.get("player");

                    final Quest quest = context.get("quest");

                    if (singlePlayerSelector.hasAny() && singlePlayerSelector.getPlayer() != null) {
                        audience.sendMessage(miniMessage.parse(mainGradient + main.getQuestPlayerManager().acceptQuest(singlePlayerSelector.getPlayer(), quest, true, true)));
                    } else {
                        audience.sendMessage(miniMessage.parse(errorGradient + "Player is not online or was not found!</gradient>"));
                    }

                }));


        manager.command(builder.literal("forcegive")
                .argument(SinglePlayerSelectorArgument.of("player"), ArgumentDescription.of("Player who should start the quest."))
                .argument(QuestSelector.of("quest", main), ArgumentDescription.of("Name of the Quest the player should force-start."))
                .meta(CommandMeta.DESCRIPTION, "Force-gives a player a quest and bypasses the Quest requirements, max. accepts & cooldown.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    final SinglePlayerSelector singlePlayerSelector = context.get("player");

                    final Quest quest = context.get("quest");

                    if (singlePlayerSelector.hasAny() && singlePlayerSelector.getPlayer() != null) {
                        audience.sendMessage(miniMessage.parse(mainGradient + main.getQuestPlayerManager().forceAcceptQuest(singlePlayerSelector.getPlayer().getUniqueId(), quest)));
                    } else {
                        audience.sendMessage(miniMessage.parse(errorGradient + "Player is not online or was not found!</gradient>"));
                    }

                }));

        handleQuestPoints();

        manager.command(builder.literal("activeQuests")
                .argument(SinglePlayerSelectorArgument.of("player"), ArgumentDescription.of("Player whose active quests you want to see."))
                .meta(CommandMeta.DESCRIPTION, "Shows the active quests of a player.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    audience.sendMessage(Component.empty());
                    final SinglePlayerSelector singlePlayerSelector = context.get("player");
                    final Player player = singlePlayerSelector.getPlayer();
                    if (singlePlayerSelector.hasAny() && player != null) {
                        QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
                        if (questPlayer != null) {
                            audience.sendMessage(miniMessage.parse(mainGradient + "Active quests of player " + highlightGradient + player.getName() + "</gradient> <green>(online)</green>:"));
                            int counter = 1;
                            for (ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                                audience.sendMessage(miniMessage.parse(highlightGradient + counter + ".</gradient> " + mainGradient + activeQuest.getQuest().getQuestName()));
                                counter += 1;
                            }
                            audience.sendMessage(miniMessage.parse(unimportant + "Total active quests: " + highlight2Gradient + (counter - 1) + "</gradient>."));
                        } else {
                            audience.sendMessage(miniMessage.parse(errorGradient + "Seems like the player " + highlightGradient + player.getName() + "</gradient> <green>(online)</green> did not accept any active quests."));
                        }
                    } else {
                        OfflinePlayer offlinePlayer = main.getUtilManager().getOfflinePlayer(singlePlayerSelector.getSelector());
                        QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(offlinePlayer.getUniqueId());
                        if (questPlayer != null) {
                            audience.sendMessage(miniMessage.parse(mainGradient + "Active quests of player " + highlightGradient + offlinePlayer.getName() + "</gradient> <red>(offline)</red>:"));
                            int counter = 1;
                            for (ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                                audience.sendMessage(miniMessage.parse(highlightGradient + counter + ".</gradient> " + mainGradient + activeQuest.getQuest().getQuestName()));
                                counter += 1;
                            }
                            audience.sendMessage(miniMessage.parse(unimportant + "Total active quests: " + highlight2Gradient + (counter - 1) + "</gradient>."));
                        } else {
                            audience.sendMessage(miniMessage.parse(errorGradient + "Seems like the player " + highlightGradient + offlinePlayer.getName() + "</gradient> <red>(offline)</red> did not accept any active quests."));
                        }
                    }
                }));

        manager.command(builder.literal("completedQuests")
                .argument(SinglePlayerSelectorArgument.of("player"), ArgumentDescription.of("Player whose completed quests you want to see."))
                .meta(CommandMeta.DESCRIPTION, "Shows the completed quests of a player.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    audience.sendMessage(Component.empty());
                    final SinglePlayerSelector singlePlayerSelector = context.get("player");
                    final Player player = singlePlayerSelector.getPlayer();
                    if (singlePlayerSelector.hasAny() && player != null) {
                        QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
                        if (questPlayer != null) {
                            audience.sendMessage(miniMessage.parse(mainGradient + "Completed quests of player " + highlightGradient + player.getName() + "</gradient> <green>(online)</green>:"));
                            int counter = 1;
                            for (CompletedQuest completedQuest : questPlayer.getCompletedQuests()) {
                                resultDate.setTime(completedQuest.getTimeCompleted());
                                audience.sendMessage(miniMessage.parse(highlightGradient + counter + ".</gradient> " + highlight2Gradient + completedQuest.getQuest().getQuestName() + "</gradient>"
                                        + mainGradient + " Completed: </gradient>" + highlight2Gradient + resultDate + "</gradient>"
                                ));
                                counter += 1;
                            }

                            audience.sendMessage(miniMessage.parse(unimportant + "Total completed quests: " + highlight2Gradient + (counter - 1) + "</gradient>."));
                        } else {
                            audience.sendMessage(miniMessage.parse(errorGradient + "Seems like the player " + highlightGradient + player.getName() + "</gradient> <green>(online)</green> never completed any quests."));
                        }
                    } else {
                        OfflinePlayer offlinePlayer = main.getUtilManager().getOfflinePlayer(singlePlayerSelector.getSelector());
                        QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(offlinePlayer.getUniqueId());
                        if (questPlayer != null) {
                            audience.sendMessage(miniMessage.parse(mainGradient + "Completed quests of player " + highlightGradient + offlinePlayer.getName() + "</gradient> <red>(offline)</red>:"));
                            int counter = 1;
                            for (CompletedQuest completedQuest : questPlayer.getCompletedQuests()) {
                                resultDate.setTime(completedQuest.getTimeCompleted());
                                audience.sendMessage(miniMessage.parse(highlightGradient + counter + ".</gradient> " + highlight2Gradient + completedQuest.getQuest().getQuestName() + "</gradient>"
                                        + mainGradient + " Completed: </gradient>" + highlight2Gradient + resultDate + "</gradient>"
                                ));
                                counter += 1;
                            }

                            audience.sendMessage(miniMessage.parse(unimportant + "Total completed quests: " + highlight2Gradient + (counter - 1) + "</gradient>."));
                        } else {
                            audience.sendMessage(miniMessage.parse(errorGradient + "Seems like the player " + highlightGradient + offlinePlayer.getName() + "</gradient> <red>(offline)</red> never completed any quests."));
                        }
                    }
                }));


        manager.command(builder.literal("debug")

                .meta(CommandMeta.DESCRIPTION, "Toggles debug mode for yourself.")
                .senderType(Player.class)
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());

                    if (main.getQuestManager().isDebugEnabledPlayer((Player) context.getSender())) {
                        main.getQuestManager().removeDebugEnabledPlayer((Player) context.getSender());
                        audience.sendMessage(miniMessage.parse(successGradient + "Your debug mode has been disabled."));
                    } else {
                        main.getQuestManager().addDebugEnabledPlayer((Player) context.getSender());
                        audience.sendMessage(miniMessage.parse(successGradient + "Your debug mode has been enabled."));
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
                    final Audience audience = main.adventure().sender(context.getSender());
                    final SinglePlayerSelector singlePlayerSelector = context.get("player");
                    final Player player = singlePlayerSelector.getPlayer();
                    final ActiveQuest activeQuest = context.get("activeQuest");
                    if (player != null) {
                        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
                        if (questPlayer != null) {
                            questPlayer.failQuest(activeQuest);
                            audience.sendMessage(miniMessage.parse(
                                    successGradient + "The active quest " + highlightGradient + activeQuest.getQuest().getQuestName() + "</gradient> has been failed for player " + highlight2Gradient + player.getName() + "</gradient>!</gradient>"
                            ));

                        } else {
                            audience.sendMessage(miniMessage.parse(
                                    errorGradient + "Player " + highlightGradient + singlePlayerSelector.getSelector() + "</gradient> seems to not have accepted any quests!</gradient>"
                            ));
                        }
                    } else {
                        audience.sendMessage(miniMessage.parse(
                                errorGradient + "Player " + highlightGradient + singlePlayerSelector.getSelector() + "</gradient> is not online or was not found!</gradient>"
                        ));
                    }
                }));


        manager.command(builder.literal("completeQuest")
                .argument(SinglePlayerSelectorArgument.of("player"), ArgumentDescription.of("Player name whose quest should be completed."))
                .argument(ActiveQuestSelector.of("activeQuest", main, "player"), ArgumentDescription.of("Active quest which should be completed."))
                .meta(CommandMeta.DESCRIPTION, "Completes an active quest for a player")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    final SinglePlayerSelector singlePlayerSelector = context.get("player");
                    final Player player = singlePlayerSelector.getPlayer();
                    final ActiveQuest activeQuest = context.get("activeQuest");
                    if (player != null) {
                        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
                        if (questPlayer != null) {
                            questPlayer.forceActiveQuestCompleted(activeQuest);
                            audience.sendMessage(miniMessage.parse(
                                    successGradient + "The active quest " + highlightGradient + activeQuest.getQuest().getQuestName() + "</gradient> has been completed for player " + highlight2Gradient + player.getName() + "</gradient>!</gradient>"
                            ));
                            return;

                        } else {
                            audience.sendMessage(miniMessage.parse(
                                    errorGradient + "Player " + highlightGradient + singlePlayerSelector.getSelector() + "</gradient> seems to not have accepted any quests!</gradient>"
                            ));
                        }
                    } else {
                        audience.sendMessage(miniMessage.parse(
                                errorGradient + "Player " + highlightGradient + singlePlayerSelector.getSelector() + "</gradient> is not online or was not found!</gradient>"
                        ));
                    }
                }));

        manager.command(builder.literal("listObjectiveTypes")
                .meta(CommandMeta.DESCRIPTION, "Shows you a list of all available Objective Types.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    audience.sendMessage(Component.empty());
                    audience.sendMessage(miniMessage.parse(highlightGradient + "All objective types:</gradient>"));
                    for (final String objectiveType : main.getObjectiveManager().getObjectiveIdentifiers()) {
                        audience.sendMessage(miniMessage.parse(mainGradient + objectiveType));
                    }
                }));

        manager.command(builder.literal("listRequirementTypes")
                .meta(CommandMeta.DESCRIPTION, "Shows you a list of all available Requirement Types.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    audience.sendMessage(Component.empty());
                    audience.sendMessage(miniMessage.parse(highlightGradient + "All requirement types:</gradient>"));
                    for (final String requirementType : main.getRequirementManager().getRequirementIdentifiers()) {
                        audience.sendMessage(miniMessage.parse(mainGradient + requirementType));
                    }
                }));

        manager.command(builder.literal("listRewardTypes")
                .meta(CommandMeta.DESCRIPTION, "Shows you a list of all available Reward Types.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    audience.sendMessage(Component.empty());
                    audience.sendMessage(miniMessage.parse(highlightGradient + "All reward types:</gradient>"));
                    for (final String rewardType : main.getRewardManager().getRewardIdentifiers()) {
                        audience.sendMessage(miniMessage.parse(mainGradient + rewardType));
                    }
                }));

        manager.command(builder.literal("listTriggerTypes")
                .meta(CommandMeta.DESCRIPTION, "Shows you a list of all available Trigger Types.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    audience.sendMessage(Component.empty());
                    audience.sendMessage(miniMessage.parse(highlightGradient + "All trigger types:</gradient>"));
                    for (final String triggerType : main.getTriggerManager().getTriggerIdentifiers()) {
                        audience.sendMessage(miniMessage.parse(mainGradient + triggerType));
                    }
                }));

        manager.command(builder.literal("listAllQuests")
                .meta(CommandMeta.DESCRIPTION, "Shows you a list of all created Quests.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    audience.sendMessage(Component.empty());
                    int counter = 1;
                    audience.sendMessage(miniMessage.parse(highlightGradient + "All Quests:</gradient>"));
                    for (final Quest quest : main.getQuestManager().getAllQuests()) {
                        audience.sendMessage(miniMessage.parse(highlightGradient + counter + ".</gradient> " + mainGradient + quest.getQuestName()));
                        counter += 1;
                    }

                }));

        manager.command(builder.literal("listPlaceholders")
                .meta(CommandMeta.DESCRIPTION, "Shows you a list of all available Placeholders which can be used in Trigger or Action commands.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    audience.sendMessage(Component.empty());

                    audience.sendMessage(miniMessage.parse(highlightGradient + "All Placeholders (Case-sensitive):</gradient>"));
                    audience.sendMessage(miniMessage.parse(highlightGradient + "1.</gradient> " + highlight2Gradient + "{PLAYER} </gradient>" + mainGradient + "- Name of the player"));
                    audience.sendMessage(miniMessage.parse(highlightGradient + "2.</gradient> " + highlight2Gradient + "{PLAYERUUID} </gradient>" + mainGradient + "- UUID of the player"));
                    audience.sendMessage(miniMessage.parse(highlightGradient + "3.</gradient> " + highlight2Gradient + "{PLAYERX} </gradient>" + mainGradient + "- X coordinates of the player"));
                    audience.sendMessage(miniMessage.parse(highlightGradient + "4.</gradient> " + highlight2Gradient + "{PLAYERY} </gradient>" + mainGradient + "- Y coordinates of the player"));
                    audience.sendMessage(miniMessage.parse(highlightGradient + "5.</gradient> " + highlight2Gradient + "{PLAYERZ} </gradient>" + mainGradient + "- Z coordinates of the player"));
                    audience.sendMessage(miniMessage.parse(highlightGradient + "6.</gradient> " + highlight2Gradient + "{WORLD} </gradient>" + mainGradient + "- World name of the player"));
                    audience.sendMessage(miniMessage.parse(highlightGradient + "6.</gradient> " + highlight2Gradient + "{QUEST} </gradient>" + mainGradient + "- Quest name (if relevant)"));

                }));

        manager.command(builder.literal("triggerObjective")
                .argument(StringArgument.<CommandSender>newBuilder("Trigger Name").withSuggestionsProvider(
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            final Audience audience = main.adventure().sender(context.getSender());
                            main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "[Trigger Name]", "[Player Name]");

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
                    final Audience audience = main.adventure().sender(context.getSender());

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
                        audience.sendMessage(miniMessage.parse(errorGradient + "Objective TriggerCommand failed. Player " + highlightGradient + singlePlayerSelector.getSelector()
                                + "</gradient> is not online or was not found!</gradient>"
                        ));
                    }
                }));

        manager.command(builder.literal("resetAndRemoveQuestForAllPlayers")
                .argument(QuestSelector.of("quest", main), ArgumentDescription.of("Name of the Quest which should be reset and removed."))
                .meta(CommandMeta.DESCRIPTION, "Removes the quest from all players, removes it from completed quests, resets the accept cooldown and basically everything else.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    audience.sendMessage(Component.empty());

                    final Quest quest = context.get("Quest Name");
                    for (final QuestPlayer questPlayer : main.getQuestPlayerManager().getQuestPlayers()) {
                        final ArrayList<ActiveQuest> activeQuestsToRemove = new ArrayList<>();
                        for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                            if (activeQuest.getQuest().equals(quest)) {
                                activeQuestsToRemove.add(activeQuest);
                                audience.sendMessage(miniMessage.parse(successGradient + "Removed the quest as an active quest for the player with the UUID "
                                        + highlightGradient + questPlayer.getUUID().toString() + "</gradient> and name "
                                        + highlight2Gradient + Bukkit.getOfflinePlayer(questPlayer.getUUID()).getName() + "</gradient>.</gradient>"
                                ));

                            }
                        }

                        questPlayer.getActiveQuests().removeAll(activeQuestsToRemove);

                        final ArrayList<CompletedQuest> completedQuestsToRemove = new ArrayList<>();

                        for (final CompletedQuest completedQuest : questPlayer.getCompletedQuests()) {
                            if (completedQuest.getQuest().equals(quest)) {
                                completedQuestsToRemove.add(completedQuest);
                                audience.sendMessage(miniMessage.parse(successGradient + "Removed the quest as a completed quest for the player with the UUID "
                                        + highlightGradient + questPlayer.getUUID().toString() + "</gradient> and name "
                                        + highlight2Gradient + Bukkit.getOfflinePlayer(questPlayer.getUUID()).getName() + "</gradient>.</gradient>"
                                ));
                            }

                        }

                        questPlayer.getCompletedQuests().removeAll(completedQuestsToRemove);
                    }
                    audience.sendMessage(miniMessage.parse(successGradient + "Operation done!"));
                }));

        manager.command(builder.literal("resetAndFailQuestForAllPlayers")
                .argument(QuestSelector.of("quest", main), ArgumentDescription.of("Name of the Quest which should be reset and failed."))
                .meta(CommandMeta.DESCRIPTION, "Fails the quest from all players, removes it from completed quests, resets the accept cooldown and basically everything else.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    audience.sendMessage(Component.empty());

                    final Quest quest = context.get("Quest Name");


                    for (final QuestPlayer questPlayer : main.getQuestPlayerManager().getQuestPlayers()) {
                        final ArrayList<ActiveQuest> activeQuestsToRemove = new ArrayList<>();
                        for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                            if (activeQuest.getQuest().equals(quest)) {
                                activeQuestsToRemove.add(activeQuest);
                            }
                        }

                        for (final ActiveQuest activeQuest : activeQuestsToRemove) {
                            questPlayer.failQuest(activeQuest);
                            audience.sendMessage(miniMessage.parse(successGradient + "Failed the quest as an active quest for the player with the UUID "
                                    + highlightGradient + questPlayer.getUUID().toString() + "</gradient> and name "
                                    + highlight2Gradient + Bukkit.getOfflinePlayer(questPlayer.getUUID()).getName() + "</gradient>.</gradient>"
                            ));

                        }

                        // questPlayer.getActiveQuests().removeAll(activeQuestsToRemove);

                        final ArrayList<CompletedQuest> completedQuestsToRemove = new ArrayList<>();

                        for (final CompletedQuest completedQuest : questPlayer.getCompletedQuests()) {
                            if (completedQuest.getQuest().equals(quest)) {
                                completedQuestsToRemove.add(completedQuest);
                                audience.sendMessage(miniMessage.parse(successGradient + "Removed the quest as a completed quest for the player with the UUID "
                                        + highlightGradient + questPlayer.getUUID().toString() + "</gradient> and name "
                                        + highlight2Gradient + Bukkit.getOfflinePlayer(questPlayer.getUUID()).getName() + "</gradient>.</gradient>"
                                ));
                            }

                        }

                        questPlayer.getCompletedQuests().removeAll(completedQuestsToRemove);
                    }
                    audience.sendMessage(miniMessage.parse(successGradient + "Operation done!"));

                }));


        manager.command(builder.literal("reload", "load")
                .meta(CommandMeta.DESCRIPTION, "Loads from the NotQuests configuration file.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());

                    main.getDataManager().loadGeneralConfig();
                    main.getLanguageManager().loadLanguageConfig();
                    main.getConversationManager().loadConversationsFromConfig();
                    audience.sendMessage(Component.empty());
                    audience.sendMessage(miniMessage.parse(successGradient + "NotQuests general.yml, language configuration and conversations have been re-loaded. </gradient>" + unimportant + "If you want to reload more, please use the ServerUtils plugin (available on spigot) or restart the server. This reload command does not reload the quests file or the database."));
                }));

        manager.command(builder.literal("reload", "load")
                .literal("general.yml")
                .meta(CommandMeta.DESCRIPTION, "Reload the general.yml.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());

                    main.getDataManager().loadGeneralConfig();
                    audience.sendMessage(Component.empty());
                    audience.sendMessage(miniMessage.parse(successGradient + "General.yml has been reloaded."));
                }));

        manager.command(builder.literal("reload", "load")
                .literal("languages")
                .meta(CommandMeta.DESCRIPTION, "Reload the languages from conversations files.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());

                    main.getLanguageManager().loadLanguageConfig();
                    audience.sendMessage(Component.empty());
                    audience.sendMessage(miniMessage.parse(successGradient + "Languages have been reloaded."));
                }));

        manager.command(builder.literal("reload", "load")
                .literal("conversations")
                .meta(CommandMeta.DESCRIPTION, "Reload the conversations from conversations files.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());

                    main.getConversationManager().loadConversationsFromConfig();
                    audience.sendMessage(Component.empty());
                    audience.sendMessage(miniMessage.parse(successGradient + "Conversations have been reloaded."));
                }));

        manager.command(builder.literal("save")
                .meta(CommandMeta.DESCRIPTION, "Saves the NotQuests configuration file.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());

                    main.getDataManager().saveData();
                    audience.sendMessage(Component.empty());
                    audience.sendMessage(miniMessage.parse(successGradient + "NotQuests configuration and player data has been saved"));
                }));

        manager.command(builder.literal("version")
                .meta(CommandMeta.DESCRIPTION, "Displays the version of the NotQuests plugin you're using.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());


                    audience.sendMessage(miniMessage.parse(mainGradient + "Current NotQuests version: " + highlightGradient + main.getDescription().getVersion()));
                }));

    }


    public void handleQuestPoints() {
        manager.command(builder.literal("questpoints")
                .argument(SinglePlayerSelectorArgument.of("player"), ArgumentDescription.of("Player whose questpoints you want to see."))
                .literal("show", "view")
                .meta(CommandMeta.DESCRIPTION, "Shows questpoints of a player")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());

                    final SinglePlayerSelector singlePlayerSelector = context.get("player");

                    if (singlePlayerSelector.hasAny() && singlePlayerSelector.getPlayer() != null) {

                        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(singlePlayerSelector.getPlayer().getUniqueId());
                        if (questPlayer != null) {
                            audience.sendMessage(miniMessage.parse(mainGradient + "Quest points for player " + highlightGradient + singlePlayerSelector.getPlayer().getName() + "</gradient> <green>(online)</green>: " + highlight2Gradient + questPlayer.getQuestPoints()));
                        } else {
                            audience.sendMessage(miniMessage.parse(errorGradient + "Seems like the player " + highlightGradient + singlePlayerSelector.getPlayer().getName() + "</gradient> <green>(online)</green> does not have any quest points!"));
                        }
                    } else {

                        final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(singlePlayerSelector.getSelector());

                        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(offlinePlayer.getUniqueId());
                        if (questPlayer != null) {
                            audience.sendMessage(miniMessage.parse(mainGradient + "Quest points for player " + highlightGradient + offlinePlayer.getName() + "</gradient> <red>(offline)</red>: " + highlight2Gradient + questPlayer.getQuestPoints()));
                        } else {
                            audience.sendMessage(miniMessage.parse(errorGradient + "Seems like the player " + highlightGradient + offlinePlayer.getName() + "</gradient> <red>(offline)</red> does not have any quest points!"));
                        }
                    }


                }));


        manager.command(builder.literal("questpoints")
                .argument(SinglePlayerSelectorArgument.of("player"), ArgumentDescription.of("Player to whom you want to add questpoints to."))
                .literal("add")
                .argument(IntegerArgument.of("amount"), ArgumentDescription.of("Amount of questpoints to add"))
                .meta(CommandMeta.DESCRIPTION, "Add questpoints to a player")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    final SinglePlayerSelector singlePlayerSelector = context.get("player");
                    int questPointsToAdd = context.get("amount");

                    if (singlePlayerSelector.hasAny() && singlePlayerSelector.getPlayer() != null) {
                        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(singlePlayerSelector.getPlayer().getUniqueId());
                        if (questPlayer != null) {
                            long oldQuestPoints = questPlayer.getQuestPoints();
                            questPlayer.addQuestPoints(questPointsToAdd, false);
                            audience.sendMessage(miniMessage.parse(mainGradient + "Quest points for player " + highlightGradient + singlePlayerSelector.getPlayer().getName() + "</gradient> <green>(online)</green> have been set from " + unimportant + oldQuestPoints
                                    + unimportantClose + " to " + highlight2Gradient + (oldQuestPoints + questPointsToAdd) + "</gradient>.</gradient>"));
                        } else {
                            audience.sendMessage(miniMessage.parse(warningGradient + "Seems like the player " + highlightGradient + singlePlayerSelector.getPlayer().getName() + "</gradient> <green>(online)</green> never accepted any quests! A new QuestPlayer has been created for him."));
                            audience.sendMessage(miniMessage.parse(mainGradient + "Quest player creation status: " + highlightGradient + main.getQuestPlayerManager().createQuestPlayer(singlePlayerSelector.getPlayer().getUniqueId())));
                            final QuestPlayer newQuestPlayer = main.getQuestPlayerManager().getQuestPlayer(singlePlayerSelector.getPlayer().getUniqueId());
                            if (newQuestPlayer != null) {
                                long oldQuestPoints = newQuestPlayer.getQuestPoints();
                                newQuestPlayer.addQuestPoints(questPointsToAdd, false);
                                audience.sendMessage(miniMessage.parse(
                                        successGradient + "Quest points for player " + highlightGradient + singlePlayerSelector.getPlayer().getName()
                                                + "</gradient> <green>(online)</green> have been set from " + unimportant + oldQuestPoints + unimportantClose + " to "
                                                + highlight2Gradient + (oldQuestPoints + questPointsToAdd) + "</gradient>."
                                ));
                            } else {
                                audience.sendMessage(miniMessage.parse(errorGradient + "Something went wrong during the questPlayer creation!"));
                            }
                        }
                    } else {
                        final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(singlePlayerSelector.getSelector());
                        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(offlinePlayer.getUniqueId());
                        if (questPlayer != null) {
                            final long oldQuestPoints = questPlayer.getQuestPoints();
                            questPlayer.addQuestPoints(questPointsToAdd, false);
                            audience.sendMessage(miniMessage.parse(mainGradient + "Quest points for player " + highlightGradient + offlinePlayer.getName() + "</gradient> <red>(offline)</red> have been set from " + unimportant + oldQuestPoints
                                    + unimportantClose + " to " + highlight2Gradient + (oldQuestPoints + questPointsToAdd) + "</gradient>.</gradient>"));
                        } else {
                            audience.sendMessage(miniMessage.parse(warningGradient + "Seems like the player " + highlightGradient + offlinePlayer.getName() + "</gradient> <red>(offline)</red> never accepted any quests! A new QuestPlayer has been created for him."));
                            audience.sendMessage(miniMessage.parse(mainGradient + "Quest player creation status: " + highlightGradient + main.getQuestPlayerManager().createQuestPlayer(offlinePlayer.getUniqueId())));

                            final QuestPlayer newQuestPlayer = main.getQuestPlayerManager().getQuestPlayer(offlinePlayer.getUniqueId());
                            if (newQuestPlayer != null) {
                                final long oldQuestPoints = newQuestPlayer.getQuestPoints();
                                newQuestPlayer.addQuestPoints(questPointsToAdd, false);
                                audience.sendMessage(miniMessage.parse(
                                        successGradient + "Quest points for player " + highlightGradient + offlinePlayer.getName()
                                                + "</gradient> <red>(offline)</red> have been set from " + unimportant + oldQuestPoints + unimportantClose + " to "
                                                + highlight2Gradient + (oldQuestPoints + questPointsToAdd) + "</gradient>."));
                            } else {
                                audience.sendMessage(miniMessage.parse(errorGradient + "Something went wrong during the questPlayer creation!"));
                            }
                        }
                        if (questPlayer != null) {
                            audience.sendMessage(miniMessage.parse(mainGradient + "Quest points for player " + highlightGradient + offlinePlayer.getName() + "</gradient> <red>(offline)</red>: " + highlight2Gradient + questPlayer.getQuestPoints()));
                        } else {
                            audience.sendMessage(miniMessage.parse(errorGradient + "Seems like the player " + highlightGradient + offlinePlayer.getName() + "</gradient> <red>(offline)</red> does not have any quest points!"));
                        }
                    }
                }));

        manager.command(builder.literal("questpoints")
                .argument(SinglePlayerSelectorArgument.of("player"), ArgumentDescription.of("Player of whom you want to remove questpoints from."))
                .literal("remove", "deduct")
                .argument(IntegerArgument.of("amount"), ArgumentDescription.of("Amount of questpoints to remove"))
                .meta(CommandMeta.DESCRIPTION, "Remove questpoints from a player")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    final SinglePlayerSelector singlePlayerSelector = context.get("player");
                    int questPointsToRemove = context.get("amount");

                    if (singlePlayerSelector.hasAny() && singlePlayerSelector.getPlayer() != null) {
                        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(singlePlayerSelector.getPlayer().getUniqueId());
                        if (questPlayer != null) {
                            long oldQuestPoints = questPlayer.getQuestPoints();
                            questPlayer.removeQuestPoints(questPointsToRemove, false);
                            audience.sendMessage(miniMessage.parse(mainGradient + "Quest points for player " + highlightGradient + singlePlayerSelector.getPlayer().getName() + "</gradient> <green>(online)</green> have been set from " + unimportant + oldQuestPoints
                                    + unimportantClose + " to " + highlight2Gradient + (oldQuestPoints - questPointsToRemove) + "</gradient>.</gradient>"));
                        } else {
                            audience.sendMessage(miniMessage.parse(warningGradient + "Seems like the player " + highlightGradient + singlePlayerSelector.getPlayer().getName() + "</gradient> <green>(online)</green> never accepted any quests! A new QuestPlayer has been created for him."));
                            audience.sendMessage(miniMessage.parse(mainGradient + "Quest player creation status: " + highlightGradient + main.getQuestPlayerManager().createQuestPlayer(singlePlayerSelector.getPlayer().getUniqueId())));
                            final QuestPlayer newQuestPlayer = main.getQuestPlayerManager().getQuestPlayer(singlePlayerSelector.getPlayer().getUniqueId());
                            if (newQuestPlayer != null) {
                                long oldQuestPoints = newQuestPlayer.getQuestPoints();
                                newQuestPlayer.removeQuestPoints(questPointsToRemove, false);
                                audience.sendMessage(miniMessage.parse(
                                        successGradient + "Quest points for player " + highlightGradient + singlePlayerSelector.getPlayer().getName()
                                                + "</gradient> <green>(online)</green> have been set from " + unimportant + oldQuestPoints + unimportantClose + " to "
                                                + highlight2Gradient + (oldQuestPoints - questPointsToRemove) + "</gradient>."
                                ));
                            } else {
                                audience.sendMessage(miniMessage.parse(errorGradient + "Something went wrong during the questPlayer creation!"));
                            }
                        }
                    } else {
                        final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(singlePlayerSelector.getSelector());
                        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(offlinePlayer.getUniqueId());
                        if (questPlayer != null) {
                            final long oldQuestPoints = questPlayer.getQuestPoints();
                            questPlayer.removeQuestPoints(questPointsToRemove, false);
                            audience.sendMessage(miniMessage.parse(mainGradient + "Quest points for player " + highlightGradient + offlinePlayer.getName() + "</gradient> <red>(offline)</red> have been set from " + unimportant + oldQuestPoints
                                    + unimportantClose + " to " + highlight2Gradient + (oldQuestPoints - questPointsToRemove) + "</gradient>.</gradient>"));
                        } else {
                            audience.sendMessage(miniMessage.parse(warningGradient + "Seems like the player " + highlightGradient + offlinePlayer.getName() + "</gradient> <red>(offline)</red> never accepted any quests! A new QuestPlayer has been created for him."));
                            audience.sendMessage(miniMessage.parse(mainGradient + "Quest player creation status: " + highlightGradient + main.getQuestPlayerManager().createQuestPlayer(offlinePlayer.getUniqueId())));

                            final QuestPlayer newQuestPlayer = main.getQuestPlayerManager().getQuestPlayer(offlinePlayer.getUniqueId());
                            if (newQuestPlayer != null) {
                                final long oldQuestPoints = newQuestPlayer.getQuestPoints();
                                newQuestPlayer.removeQuestPoints(questPointsToRemove, false);
                                audience.sendMessage(miniMessage.parse(
                                        successGradient + "Quest points for player " + highlightGradient + offlinePlayer.getName()
                                                + "</gradient> <red>(offline)</red> have been set from " + unimportant + oldQuestPoints + unimportantClose + " to "
                                                + highlight2Gradient + (oldQuestPoints - questPointsToRemove) + "</gradient>."));
                            } else {
                                audience.sendMessage(miniMessage.parse(errorGradient + "Something went wrong during the questPlayer creation!"));
                            }
                        }
                        if (questPlayer != null) {
                            audience.sendMessage(miniMessage.parse(mainGradient + "Quest points for player " + highlightGradient + offlinePlayer.getName() + "</gradient> <red>(offline)</red>: " + highlight2Gradient + questPlayer.getQuestPoints()));
                        } else {
                            audience.sendMessage(miniMessage.parse(errorGradient + "Seems like the player " + highlightGradient + offlinePlayer.getName() + "</gradient> <red>(offline)</red> does not have any quest points!"));
                        }
                    }
                }));


        manager.command(builder.literal("questpoints")
                .argument(SinglePlayerSelectorArgument.of("player"), ArgumentDescription.of("Player whose questpoints amount you want to change."))
                .literal("set")
                .argument(IntegerArgument.of("amount"), ArgumentDescription.of("New questpoints amount"))
                .meta(CommandMeta.DESCRIPTION, "Set the questpoints of a player")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    final SinglePlayerSelector singlePlayerSelector = context.get("player");
                    int newQuestPointsAmount = context.get("amount");

                    if (singlePlayerSelector.hasAny() && singlePlayerSelector.getPlayer() != null) {
                        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(singlePlayerSelector.getPlayer().getUniqueId());
                        if (questPlayer != null) {
                            long oldQuestPoints = questPlayer.getQuestPoints();
                            questPlayer.setQuestPoints(newQuestPointsAmount, false);
                            audience.sendMessage(miniMessage.parse(mainGradient + "Quest points for player " + highlightGradient + singlePlayerSelector.getPlayer().getName() + "</gradient> <green>(online)</green> have been set from " + unimportant + oldQuestPoints
                                    + unimportantClose + " to " + highlight2Gradient + (newQuestPointsAmount) + "</gradient>.</gradient>"));
                        } else {
                            audience.sendMessage(miniMessage.parse(warningGradient + "Seems like the player " + highlightGradient + singlePlayerSelector.getPlayer().getName() + "</gradient> <green>(online)</green> never accepted any quests! A new QuestPlayer has been created for him."));
                            audience.sendMessage(miniMessage.parse(mainGradient + "Quest player creation status: " + highlightGradient + main.getQuestPlayerManager().createQuestPlayer(singlePlayerSelector.getPlayer().getUniqueId())));
                            final QuestPlayer newQuestPlayer = main.getQuestPlayerManager().getQuestPlayer(singlePlayerSelector.getPlayer().getUniqueId());
                            if (newQuestPlayer != null) {
                                long oldQuestPoints = newQuestPlayer.getQuestPoints();
                                newQuestPlayer.setQuestPoints(newQuestPointsAmount, false);
                                audience.sendMessage(miniMessage.parse(
                                        successGradient + "Quest points for player " + highlightGradient + singlePlayerSelector.getPlayer().getName()
                                                + "</gradient> <green>(online)</green> have been set from " + unimportant + oldQuestPoints + unimportantClose + " to "
                                                + highlight2Gradient + (newQuestPointsAmount) + "</gradient>."
                                ));
                            } else {
                                audience.sendMessage(miniMessage.parse(errorGradient + "Something went wrong during the questPlayer creation!"));
                            }
                        }
                    } else {
                        final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(singlePlayerSelector.getSelector());
                        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(offlinePlayer.getUniqueId());
                        if (questPlayer != null) {
                            final long oldQuestPoints = questPlayer.getQuestPoints();
                            questPlayer.setQuestPoints(newQuestPointsAmount, false);
                            audience.sendMessage(miniMessage.parse(mainGradient + "Quest points for player " + highlightGradient + offlinePlayer.getName() + "</gradient> <red>(offline)</red> have been set from " + unimportant + oldQuestPoints
                                    + unimportantClose + " to " + highlight2Gradient + (newQuestPointsAmount) + "</gradient>.</gradient>"));
                        } else {
                            audience.sendMessage(miniMessage.parse(warningGradient + "Seems like the player " + highlightGradient + offlinePlayer.getName() + "</gradient> <red>(offline)</red> never accepted any quests! A new QuestPlayer has been created for him."));
                            audience.sendMessage(miniMessage.parse(mainGradient + "Quest player creation status: " + highlightGradient + main.getQuestPlayerManager().createQuestPlayer(offlinePlayer.getUniqueId())));

                            final QuestPlayer newQuestPlayer = main.getQuestPlayerManager().getQuestPlayer(offlinePlayer.getUniqueId());
                            if (newQuestPlayer != null) {
                                final long oldQuestPoints = newQuestPlayer.getQuestPoints();
                                newQuestPlayer.setQuestPoints(newQuestPointsAmount, false);
                                audience.sendMessage(miniMessage.parse(
                                        successGradient + "Quest points for player " + highlightGradient + offlinePlayer.getName()
                                                + "</gradient> <red>(offline)</red> have been set from " + unimportant + oldQuestPoints + unimportantClose + " to "
                                                + highlight2Gradient + (newQuestPointsAmount) + "</gradient>."));
                            } else {
                                audience.sendMessage(miniMessage.parse(errorGradient + "Something went wrong during the questPlayer creation!"));
                            }
                        }
                        if (questPlayer != null) {
                            audience.sendMessage(miniMessage.parse(mainGradient + "Quest points for player " + highlightGradient + offlinePlayer.getName() + "</gradient> <red>(offline)</red>: " + highlight2Gradient + questPlayer.getQuestPoints()));
                        } else {
                            audience.sendMessage(miniMessage.parse(errorGradient + "Seems like the player " + highlightGradient + offlinePlayer.getName() + "</gradient> <red>(offline)</red> does not have any quest points!"));
                        }
                    }
                }));



    }


    public void handleActions() {
        //Actions
        manager.command(builder.literal("actions")
                .literal("add")
                .argument(StringArgument.<CommandSender>newBuilder("Action Name").withSuggestionsProvider(
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            final Audience audience = main.adventure().sender(context.getSender());
                            main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "[New, unique Action Name]", "<New console command>");

                            ArrayList<String> completions = new ArrayList<>();

                            completions.add("<Enter new, unique Action Name>");
                            return completions;
                        }
                ).single().build(), ArgumentDescription.of("Action Name"))
                .argument(StringArrayArgument.of("Console Command",
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            final Audience audience = main.adventure().sender(context.getSender());
                            main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "<New console command>", "");

                            ArrayList<String> completions = new ArrayList<>();

                            if (lastString.startsWith("{")) {
                                completions.addAll(placeholders);
                            } else {
                                completions.add("<Enter Console Command>");
                            }

                            return completions;
                        }
                ), ArgumentDescription.of("Console Command"))
                .meta(CommandMeta.DESCRIPTION, "Create a new action.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    final String actionName = context.get("Action Name");

                    boolean alreadyExists = false;
                    for (final Action action : main.getQuestManager().getAllActions()) {
                        if (action.getActionName().equalsIgnoreCase(actionName)) {
                            alreadyExists = true;
                            break;
                        }
                    }

                    if (!alreadyExists) {
                        final String consoleCommand = String.join(" ", (String[]) context.get("Console Command"));

                        audience.sendMessage(miniMessage.parse(mainGradient + "Trying to create Action with the name "
                                + highlightGradient + actionName + "</gradient> and console command " + highlight2Gradient + consoleCommand + "</gradient>...</gradient>"
                        ));

                        audience.sendMessage(miniMessage.parse(mainGradient + "Status: " + main.getQuestManager().createAction(actionName, consoleCommand)));

                    } else {
                        audience.sendMessage(miniMessage.parse(errorGradient + "Error! An action with the name " + highlightGradient + actionName + "</gradient> already exists!</gradient>"));

                    }
                }));
        manager.command(builder.literal("actions")
                .literal("edit")
                .argument(StringArgument.<CommandSender>newBuilder("Action Name").withSuggestionsProvider(
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            final Audience audience = main.adventure().sender(context.getSender());
                            main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "[Action Name]", "[...]");

                            ArrayList<String> completions = new ArrayList<>();

                            for (final Action action : main.getQuestManager().getAllActions()) {
                                completions.add(action.getActionName());
                            }
                            return completions;
                        }
                ).single().build(), ArgumentDescription.of("Action Name"))
                .literal("setCommand")
                .argument(StringArrayArgument.of("Console Command",
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            final Audience audience = main.adventure().sender(context.getSender());
                            main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "<New console command>", "");

                            ArrayList<String> completions = new ArrayList<>();

                            if (lastString.startsWith("{")) {
                                completions.addAll(placeholders);
                            } else {
                                completions.add("<Enter Console Command>");
                            }

                            return completions;
                        }
                ), ArgumentDescription.of("Console Command"))
                .meta(CommandMeta.DESCRIPTION, "Edits an action's command")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());

                    final String actionName = context.get("Action Name");
                    final String consoleCommand = String.join(" ", (String[]) context.get("Console Command"));

                    Action foundAction = null;
                    for (final Action action : main.getQuestManager().getAllActions()) {
                        if (action.getActionName().equalsIgnoreCase(actionName)) {
                            foundAction = action;
                            break;
                        }
                    }
                    if (foundAction != null) {
                        foundAction.setConsoleCommand(consoleCommand);
                        audience.sendMessage(miniMessage.parse(successGradient + "Console command of action " + highlightGradient + foundAction.getActionName() + "</gradient> has been set to " + highlight2Gradient + consoleCommand + "</gradient> </gradient>"));
                    } else {
                        audience.sendMessage(miniMessage.parse(errorGradient + "Error! Action with the name " + highlightGradient + actionName + "</gradient> does not exist!</gradient>"));

                    }
                }));


        manager.command(builder.literal("actions")
                .literal("edit")
                .argument(StringArgument.<CommandSender>newBuilder("Action Name").withSuggestionsProvider(
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            final Audience audience = main.adventure().sender(context.getSender());
                            main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "[Action Name]", "[...]");

                            ArrayList<String> completions = new ArrayList<>();

                            for (final Action action : main.getQuestManager().getAllActions()) {
                                completions.add(action.getActionName());
                            }
                            return completions;
                        }
                ).single().build(), ArgumentDescription.of("Action Name"))
                .literal("delete", "remove")
                .meta(CommandMeta.DESCRIPTION, "Removes an action")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());

                    final String actionName = context.get("Action Name");

                    Action foundAction = null;
                    for (final Action action : main.getQuestManager().getAllActions()) {
                        if (action.getActionName().equalsIgnoreCase(actionName)) {
                            foundAction = action;
                            break;
                        }
                    }


                    if (foundAction != null) {

                        main.getQuestManager().removeAction(foundAction);
                        audience.sendMessage(miniMessage.parse(successGradient + "Action with the name " + highlightGradient + foundAction.getActionName() + "</gradient> has been deleted.</gradient>"));

                    } else {
                        audience.sendMessage(miniMessage.parse(errorGradient + "Error! Action with the name " + highlightGradient + actionName + "</gradient> does not exist!</gradient>"));
                    }

                }));

        manager.command(builder.literal("actions")
                .literal("list")
                .meta(CommandMeta.DESCRIPTION, "Shows all existing actions.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    int counter = 1;
                    audience.sendMessage(miniMessage.parse(highlightGradient + "All Actions:"));
                    for (final Action action : main.getQuestManager().getAllActions()) {
                        audience.sendMessage(miniMessage.parse(highlightGradient + counter + ".</gradient> " + mainGradient + action.getActionName()));
                        audience.sendMessage(miniMessage.parse(veryUnimportant + "  └─ " + unimportant + "Command: " + highlight2Gradient + action.getConsoleCommand()));
                        counter += 1;
                    }
                }));
    }


    public void getProgress(CommandSender sender, Player player, String playerName, ActiveQuest activeQuest) {
        final Audience audience = main.adventure().sender(sender);

        audience.sendMessage(Component.empty());

        if (player != null) {


            QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
            if (questPlayer != null) {

                if (activeQuest != null) {
                    audience.sendMessage(miniMessage.parse(
                            mainGradient + "Completed Objectives for Quest " + highlightGradient + activeQuest.getQuest().getQuestName() + "</gradient> of player "
                                    + highlight2Gradient + playerName + "</gradient>" + " <green>(online)</green>:</gradient>"
                    ));
                    main.getQuestManager().sendCompletedObjectivesAndProgress((Player) sender, activeQuest);

                    audience.sendMessage(miniMessage.parse(
                            mainGradient + "Active Objectives for Quest " + highlightGradient + activeQuest.getQuest().getQuestName() + "</gradient> of player "
                                    + highlight2Gradient + playerName + "</gradient>" + " <green>(online)</green>:</gradient>"
                    ));
                    main.getQuestManager().sendActiveObjectivesAndProgress((Player) sender, activeQuest);


                } else {
                    audience.sendMessage(miniMessage.parse(
                            errorGradient + "Quest " + highlightGradient + activeQuest.getQuest().getQuestName() + "</gradient> was not found or active!"
                    ));
                    audience.sendMessage(miniMessage.parse(mainGradient + "Active quests of player " + highlightGradient + player.getName() + "</gradient> <green>(online)</green>:"));
                    int counter = 1;
                    for (ActiveQuest activeQuest1 : questPlayer.getActiveQuests()) {
                        audience.sendMessage(miniMessage.parse(highlightGradient + counter + ".</gradient> " + mainGradient + activeQuest1.getQuest().getQuestName()));
                        counter += 1;
                    }
                    audience.sendMessage(miniMessage.parse(unimportant + "Total active quests: " + highlight2Gradient + (counter - 1) + "</gradient>."));

                }

            } else {
                audience.sendMessage(miniMessage.parse(errorGradient + "Seems like the player " + highlightGradient + player.getName() + "</gradient> <green>(online)</green> did not accept any active quests."));

            }


        } else {
            OfflinePlayer offlinePlayer = main.getUtilManager().getOfflinePlayer(playerName);

            QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(offlinePlayer.getUniqueId());
            if (questPlayer != null) {


                if (activeQuest != null) {

                    audience.sendMessage(miniMessage.parse(
                            mainGradient + "Completed Objectives for Quest " + highlightGradient + activeQuest.getQuest().getQuestName() + "</gradient> of player "
                                    + highlight2Gradient + playerName + "</gradient>" + " <red>(offline)</red>:</gradient>"
                    ));
                    main.getQuestManager().sendCompletedObjectivesAndProgress((Player) sender, activeQuest);

                    audience.sendMessage(miniMessage.parse(
                            mainGradient + "Active Objectives for Quest " + highlightGradient + activeQuest.getQuest().getQuestName() + "</gradient> of player "
                                    + highlight2Gradient + playerName + "</gradient>" + " <red>(offline)</red>:</gradient>"
                    ));
                    main.getQuestManager().sendActiveObjectivesAndProgress((Player) sender, activeQuest);


                } else {
                    audience.sendMessage(miniMessage.parse(
                            errorGradient + "Quest " + highlightGradient + activeQuest.getQuest().getQuestName() + "</gradient> was not found or active!"
                    ));
                    audience.sendMessage(miniMessage.parse(mainGradient + "Active quests of player " + highlightGradient + offlinePlayer.getName() + "</gradient> <green>(online)</green>:"));
                    int counter = 1;
                    for (ActiveQuest activeQuest1 : questPlayer.getActiveQuests()) {
                        audience.sendMessage(miniMessage.parse(highlightGradient + counter + ".</gradient> " + mainGradient + activeQuest1.getQuest().getQuestName()));
                        counter += 1;
                    }

                }
            } else {
                audience.sendMessage(miniMessage.parse(errorGradient + "Seems like the player " + highlightGradient + offlinePlayer.getName() + "</gradient> <red>(offline)</red> did not accept any active quests."));
            }
        }

    }
}
