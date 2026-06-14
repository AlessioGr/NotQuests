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

package rocks.gravili.notquests.paper.structs.triggers.types;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.framework.NQArguments;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.structs.triggers.Trigger;

import java.util.ArrayList;
import java.util.List;

public class WorldEnterTrigger extends Trigger {

    private String worldToEnterName;

    public WorldEnterTrigger(final NotQuests main) {
        super(main);
    }

    public static void handleCommands(
            NotQuests main,
            NQCommandManager manager,
            NQCommandBuilder addTriggerBuilder) {
        manager.command(addTriggerBuilder
                .required("world to enter", NQArguments.stringArgument(), NQDescription.of("Name of the world which needs to be entered"), (context, input) -> {
                    List<String> completions = new ArrayList<>();
                    completions.add("ALL");

                    for (final World world : Bukkit.getWorlds()) {
                        completions.add(world.getName());
                    }
                    return completions;
                })
                .required("amount", NQArguments.integerArgument(), NQDescription.of("Amount of times the world needs to be entered."))
                .flag(main.getCommandManager().applyOn)
                .flag(main.getCommandManager().triggerWorldString)
                .commandDescription(NQDescription.of("Triggers when the player enters a specific world."))
                .handler(
                        (context) -> {
                            final String worldToEnterName = context.get("world to enter");

                            WorldEnterTrigger worldEnterTrigger = new WorldEnterTrigger(main);
                            worldEnterTrigger.setWorldToEnterName(worldToEnterName);

                            main.getTriggerManager().addTrigger(worldEnterTrigger, context);
                        }));
    }

    public final String getWorldToEnterName() {
        return worldToEnterName;
    }

    public void setWorldToEnterName(final String worldToEnterName) {
        this.worldToEnterName = worldToEnterName;
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.worldToEnter", getWorldToEnterName());
    }

    @Override
    public String getTriggerDescription() {
        return "World to enter: <WHITE>" + getWorldToEnterName();
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        this.worldToEnterName = configuration.getString(initialPath + ".specifics.worldToEnter", "ALL");
    }
}
