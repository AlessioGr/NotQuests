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

package rocks.gravili.notquests.spigot.objectives.hooks.slimefun;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.spigot.NotQuests;
import rocks.gravili.notquests.spigot.objectives.Objective;
import rocks.gravili.notquests.spigot.structs.ActiveObjective;

public class SlimefunResearchObjective extends Objective {


    public SlimefunResearchObjective(NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addObjectiveBuilder) {
        if (!main.getIntegrationsManager().isSlimefunEnabled()) {
            return;
        }

        manager.command(addObjectiveBuilder.literal("SlimefunResearch")
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("Amount to spend on research"))
                .meta(CommandMeta.DESCRIPTION, "Adds a new SlimefunResearch Objective to a quest")
                .handler((context) -> {
                    SlimefunResearchObjective slimefunResearchobjective = new SlimefunResearchObjective(main);
                    slimefunResearchobjective.setProgressNeeded(context.get("amount"));

                    main.getObjectiveManager().addObjective(slimefunResearchobjective, context);
                }));
    }

    @Override
    public String getObjectiveTaskDescription(final String eventualColor, final Player player) {
        return main.getLanguageManager().getString("chat.objectives.taskDescription.SlimefunResearch.base", player)
                .replace("%EVENTUALCOLOR%", eventualColor);
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {

    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {

    }

    @Override
    public void onObjectiveUnlock(final ActiveObjective activeObjective) {

    }
}
