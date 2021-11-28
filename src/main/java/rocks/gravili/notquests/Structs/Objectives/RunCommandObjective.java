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

package rocks.gravili.notquests.Structs.Objectives;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.Commands.NotQuestColors;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.Quest;

import java.util.ArrayList;
import java.util.List;

public class RunCommandObjective extends Objective {

    private final NotQuests main;
    private final String commandToRun;
    private final boolean ignoreCase;
    private final boolean cancelCommand;

    public RunCommandObjective(NotQuests main, final Quest quest, final int objectiveID, int amountToRun, String commandToRun, boolean ignoreCase, boolean cancelCommand) {
        super(main, quest, objectiveID, amountToRun);
        this.main = main;
        this.commandToRun = commandToRun;
        this.ignoreCase = ignoreCase;
        this.cancelCommand = cancelCommand;
    }

    public RunCommandObjective(NotQuests main, Quest quest, int objectiveNumber, int progressNeeded) {
        super(main, quest, objectiveNumber, progressNeeded);
        final String questName = quest.getQuestName();

        this.main = main;
        commandToRun = main.getDataManager().getQuestsConfig().getString("quests." + questName + ".objectives." + objectiveNumber + ".specifics.commandToRun");
        ignoreCase = main.getDataManager().getQuestsConfig().getBoolean("quests." + questName + ".objectives." + objectiveNumber + ".specifics.ignoreCase", false);
        cancelCommand = main.getDataManager().getQuestsConfig().getBoolean("quests." + questName + ".objectives." + objectiveNumber + ".specifics.cancelCommand", false);

    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addObjectiveBuilder) {
        manager.command(addObjectiveBuilder.literal("RunCommand")
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("Amount of times the command needs to be run."))
                .argument(StringArgument.<CommandSender>newBuilder("Command").withSuggestionsProvider(
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            final Audience audience = main.adventure().sender(context.getSender());
                            main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "[Enter command (put between \" \" if you want to use spaces)]", "");

                            ArrayList<String> completions = new ArrayList<>();

                            completions.add("<Enter command (put between \" \" if you want to use spaces)>");
                            return completions;
                        }
                ).quoted().build(), ArgumentDescription.of("Command"))
                .flag(
                        manager.flagBuilder("ignoreCase")
                                .withDescription(ArgumentDescription.of("Makes it so it doesn't matter whether the player uses uppercase or lowercase characters"))
                )
                .flag(
                        manager.flagBuilder("cancelCommand")
                                .withDescription(ArgumentDescription.of("Makes it so the command will be cancelled (not actually run) when entered while this objective is active"))
                )
                .meta(CommandMeta.DESCRIPTION, "Adds a new RunCommand Objective to a quest")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Quest quest = context.get("quest");
                    String command = context.get("Command");
                    final int amount = context.get("amount");
                    final boolean ignoreCase = context.flags().isPresent("ignoreCase");
                    final boolean cancelCommand = context.flags().isPresent("cancelCommand");

                    if (!command.startsWith("/")) {
                        command = "/" + command;
                    }

                    RunCommandObjective runCommandObjective = new RunCommandObjective(main, quest, quest.getObjectives().size() + 1, amount, command, ignoreCase, cancelCommand);
                    quest.addObjective(runCommandObjective, true);
                    audience.sendMessage(MiniMessage.miniMessage().parse(
                            NotQuestColors.successGradient + "RunCommands Objective successfully added to Quest " + NotQuestColors.highlightGradient
                                    + quest.getQuestName() + "</gradient>!</gradient>"
                    ));

                }));
    }

    @Override
    public String getObjectiveTaskDescription(final String eventualColor, final Player player) {
        return main.getLanguageManager().getString("chat.objectives.taskDescription.runCommand.base", player)
                .replaceAll("%EVENTUALCOLOR%", eventualColor)
                .replaceAll("%COMMANDTORUN%", getCommandToRun());
    }

    @Override
    public void save() {
        main.getDataManager().getQuestsConfig().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.commandToRun", getCommandToRun());
        main.getDataManager().getQuestsConfig().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.ignoreCase", isIgnoreCase());
        main.getDataManager().getQuestsConfig().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.cancelCommand", isCancelCommand());

    }

    public final String getCommandToRun() {
        return commandToRun;
    }

    public final long getAmountToRun() {
        return super.getProgressNeeded();
    }

    public final boolean isIgnoreCase() {
        return ignoreCase;
    }

    public final boolean isCancelCommand() {
        return cancelCommand;
    }
}
