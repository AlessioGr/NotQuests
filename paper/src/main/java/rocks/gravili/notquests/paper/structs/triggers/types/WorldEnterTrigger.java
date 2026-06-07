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
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.incendo.cloud.Command;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.suggestion.Suggestion;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.triggers.Trigger;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import static org.incendo.cloud.parser.standard.IntegerParser.integerParser;
import static org.incendo.cloud.parser.standard.StringParser.stringParser;

public class WorldEnterTrigger extends Trigger {

    private String worldToEnterName;

    public WorldEnterTrigger(final NotQuests main) {
        super(main);
    }

    public static void handleCommands(
            NotQuests main,
            PaperCommandManager<CommandSender> manager,
            Command.Builder<CommandSender> addTriggerBuilder) {
        manager.command(addTriggerBuilder
                .required("world to enter", stringParser(), Description.of("Name of the world which needs to be entered"), (context, lastString) -> {
                    main.getUtilManager().sendFancyCommandCompletion(context.sender(), lastString.input().split(" "), "[World Name / 'ALL']", "[Amount of Enters]");
                    ArrayList<Suggestion> completions = new ArrayList<>();
                    completions.add(Suggestion.suggestion("ALL"));

                    for (final World world : Bukkit.getWorlds()) {
                        completions.add(Suggestion.suggestion(world.getName()));
                    }
                    return CompletableFuture.completedFuture(completions);
                })
                .required("amount", integerParser(1), Description.of("Amount of times the world needs to be entered."))
                .flag(main.getCommandManager().applyOn)
                .flag(main.getCommandManager().triggerWorldString)
                .commandDescription(Description.of("Triggers when the player enters a specific world."))
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
