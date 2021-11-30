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

public class TriggerCommandObjective extends Objective {

    private final NotQuests main;
    private final String triggerName;


    public TriggerCommandObjective(NotQuests main, final Quest quest, final int objectiveID, String triggerName, int amountToTrigger) {
        super(main, quest, objectiveID, amountToTrigger);
        this.triggerName = triggerName;
        this.main = main;
    }

    public TriggerCommandObjective(NotQuests main, Quest quest, int objectiveNumber, int progressNeeded) {
        super(main, quest, objectiveNumber, progressNeeded);
        final String questName = quest.getQuestName();

        this.main = main;
        triggerName = main.getDataManager().getQuestsConfig().getString("quests." + questName + ".objectives." + objectiveNumber + ".specifics.triggerName");

    }

    @Override
    public String getObjectiveTaskDescription(final String eventualColor, final Player player) {
        return main.getLanguageManager().getString("chat.objectives.taskDescription.triggerCommand.base", player)
                .replace("%EVENTUALCOLOR%", eventualColor)
                .replace("%TRIGGERNAME%", "" + getTriggerName());
    }

    @Override
    public void save() {
        main.getDataManager().getQuestsConfig().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.triggerName", getTriggerName());
    }

    public final String getTriggerName() {
        return triggerName;
    }


    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addObjectiveBuilder) {
        manager.command(addObjectiveBuilder.literal("TriggerCommand")
                .senderType(Player.class)
                .argument(StringArgument.<CommandSender>newBuilder("Trigger name").withSuggestionsProvider(
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            final Audience audience = main.adventure().sender(context.getSender());
                            main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "[New Trigger Name]", "[Amount of triggers needed]");

                            ArrayList<String> completions = new ArrayList<>();
                            completions.add("<Enter new TriggerCommand name>");

                            return completions;
                        }
                ).single().build(), ArgumentDescription.of("Triggercommand name"))
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("Amount of times the trigger needs to be triggered to complete this objective."))
                .meta(CommandMeta.DESCRIPTION, "Adds a new TriggerCommand Objective to a quest")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Quest quest = context.get("quest");

                    final String triggerName = String.join(" ", (String[]) context.get("Trigger name"));

                    final int amount = context.get("amount");

                    TriggerCommandObjective triggerCommandObjective = new TriggerCommandObjective(main, quest, quest.getObjectives().size() + 1, triggerName, amount);
                    quest.addObjective(triggerCommandObjective, true);
                    audience.sendMessage(MiniMessage.miniMessage().parse(
                            NotQuestColors.successGradient + "TriggerCommand Objective successfully added to Quest " + NotQuestColors.highlightGradient
                                    + quest.getQuestName() + "</gradient>!</gradient>"
                    ));

                }));
    }
}
