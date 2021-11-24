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
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.arguments.standard.StringArrayArgument;
import cloud.commandframework.bukkit.parsers.selector.SinglePlayerSelectorArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.Commands.newCMDs.arguments.QuestSelector;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.Quest;
import rocks.gravili.notquests.Structs.Triggers.Action;

import java.util.ArrayList;
import java.util.List;

import static rocks.gravili.notquests.Commands.NotQuestColors.*;

public class AdminCommands {
    private final NotQuests main;
    private final PaperCommandManager<CommandSender> manager;
    private final Command.Builder<CommandSender> builder;

    public AdminCommands(final NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        this.main = main;
        this.manager = manager;
        this.builder = builder;


        manager.command(builder.literal("create")
                .senderType(Player.class)
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
                    audience.sendMessage(MiniMessage.miniMessage().parse(main.getQuestManager().createQuest(context.get("Quest Name"))));
                }));


        manager.command(builder.literal("delete")
                .senderType(Player.class)
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
                    audience.sendMessage(MiniMessage.miniMessage().parse(main.getQuestManager().deleteQuest(context.get("Quest Name"))));
                }));


        handleActions();


        manager.command(builder.literal("give")
                .senderType(Player.class)
                .argument(SinglePlayerSelectorArgument.of("player"), ArgumentDescription.of("Player who should start the quest."))
                .argument(new QuestSelector<>(
                        true,
                        "quest",
                        main
                ), ArgumentDescription.of("Name of the Quest the player should start."))
                .meta(CommandMeta.DESCRIPTION, "Gives a player a quest without bypassing the Quest requirements.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());

                    final String playerName = context.get("player");
                    final String questName = context.get("quest");
                    final Player player = Bukkit.getPlayer(playerName);
                    if (player != null) {
                        final Quest quest = main.getQuestManager().getQuest(questName);
                        if (quest != null) {
                            audience.sendMessage(MiniMessage.miniMessage().parse(mainGradient + main.getQuestPlayerManager().acceptQuest(player, quest, true, true)));
                        } else {
                            audience.sendMessage(MiniMessage.miniMessage().parse(errorGradient + "Quest " + highlightGradient + questName + "</gradient> does not exist.</gradient>"));
                        }
                    } else {
                        audience.sendMessage(MiniMessage.miniMessage().parse(errorGradient + "Player " + highlightGradient + playerName + "</gradient> is not online or was not found!</gradient>"));
                    }

                }));


    }


    public void handleActions() {
        //Actions
        manager.command(builder.literal("actions")
                .senderType(Player.class)
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

                            completions.add("<Enter Console Command>");

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

                        audience.sendMessage(MiniMessage.miniMessage().parse(mainGradient + "Trying to create Action with the name "
                                + highlightGradient + actionName + "</gradient> and console command " + highlight2Gradient + consoleCommand + "</gradient>...</gradient>"
                        ));

                        audience.sendMessage(MiniMessage.miniMessage().parse(mainGradient + "Status: " + main.getQuestManager().createAction(actionName, consoleCommand)));

                    } else {
                        audience.sendMessage(MiniMessage.miniMessage().parse(errorGradient + "Error! An action with the name " + highlightGradient + actionName + "</gradient> already exists!</gradient>"));

                    }
                }));
        manager.command(builder.literal("actions")
                .senderType(Player.class)
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

                            completions.add("<Enter Console Command>");

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
                        audience.sendMessage(MiniMessage.miniMessage().parse(successGradient + "Console command of action " + highlightGradient + foundAction.getActionName() + "</gradient> has been set to " + highlight2Gradient + consoleCommand + "</gradient> </gradient>"));
                    } else {
                        audience.sendMessage(MiniMessage.miniMessage().parse(errorGradient + "Error! Action with the name " + highlightGradient + actionName + "</gradient> does not exist!</gradient>"));

                    }
                }));


        manager.command(builder.literal("actions")
                .senderType(Player.class)
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
                        audience.sendMessage(MiniMessage.miniMessage().parse(successGradient + "Action with the name " + highlightGradient + foundAction.getActionName() + "</gradient> has been deleted.</gradient>"));

                    } else {
                        audience.sendMessage(MiniMessage.miniMessage().parse(errorGradient + "Error! Action with the name " + highlightGradient + actionName + "</gradient> does not exist!</gradient>"));
                    }

                }));

        manager.command(builder.literal("actions")
                .senderType(Player.class)
                .literal("list")
                .meta(CommandMeta.DESCRIPTION, "Shows all existing actions.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    int counter = 1;
                    audience.sendMessage(MiniMessage.miniMessage().parse(mainGradient + "All Actions:"));
                    for (final Action action : main.getQuestManager().getAllActions()) {
                        audience.sendMessage(MiniMessage.miniMessage().parse(highlightGradient + counter + ".</gradient> " + mainGradient + action.getActionName()));
                        audience.sendMessage(MiniMessage.miniMessage().parse(unimportant + "--- Command: " + highlight2Gradient + action.getConsoleCommand()));
                        counter += 1;
                    }
                }));
    }
}
