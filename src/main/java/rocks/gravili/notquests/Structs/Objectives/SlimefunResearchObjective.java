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
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.Commands.NotQuestColors;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.Quest;

public class SlimefunResearchObjective extends Objective {

    private final NotQuests main;


    public SlimefunResearchObjective(NotQuests main, final Quest quest, final int objectiveID, int progressNeeded) {
        super(main, quest, objectiveID, progressNeeded);
        this.main = main;

    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addObjectiveBuilder) {
        manager.command(addObjectiveBuilder.literal("SlimefunResearch")
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("Amount to spend on research"))
                .meta(CommandMeta.DESCRIPTION, "Adds a new SlimefunResearch Objective to a quest")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Quest quest = context.get("quest");

                    SlimefunResearchObjective slimefunResearchobjective = new SlimefunResearchObjective(main, quest, quest.getObjectives().size() + 1, context.get("amount"));

                    quest.addObjective(slimefunResearchobjective, true);
                    audience.sendMessage(MiniMessage.miniMessage().parse(
                            NotQuestColors.successGradient + "SlimefunResearch objective successfully added to Quest " + NotQuestColors.highlightGradient + quest.getQuestName() + "</gradient>!</gradient>"
                    ));

                }));
    }

    @Override
    public String getObjectiveTaskDescription(final String eventualColor, final Player player) {
        return "    &8└─ &7%EVENTUALCOLOR%Spend on research"
                .replaceAll("%EVENTUALCOLOR%", eventualColor);
    }

    @Override
    public void save() {

    }
}
