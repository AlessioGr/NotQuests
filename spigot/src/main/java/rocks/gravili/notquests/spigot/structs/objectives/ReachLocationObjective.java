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

package rocks.gravili.notquests.spigot.structs.objectives;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.StringArrayArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import net.kyori.adventure.audience.Audience;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.spigot.NotQuests;
import rocks.gravili.notquests.spigot.structs.ActiveObjective;

import java.util.ArrayList;
import java.util.List;

public class ReachLocationObjective extends Objective {
    private Location min, max;
    private String locationName;

    public ReachLocationObjective(NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addObjectiveBuilder) {
        if (!main.getIntegrationsManager().isWorldEditEnabled()) {
            return;
        }

        manager.command(addObjectiveBuilder.literal("ReachLocation")
                .senderType(Player.class)
                .literal("worldeditselection")
                .argument(StringArrayArgument.of("Location Name",
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            final Audience audience = main.adventure().sender(context.getSender());
                            main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "<Location Name>", "");
                            ArrayList<String> completions = new ArrayList<>();
                            completions.add("<Enter new Location name>");
                            return completions;
                        }
                ), ArgumentDescription.of("Location name"))
                .meta(CommandMeta.DESCRIPTION, "Adds a new ReachLocation Objective to a quest")
                .handler((context) -> {
                    final String locationName = String.join(" ", (String[]) context.get("Location Name"));

                    main.getIntegrationsManager().getWorldEditManager().handleReachLocationObjectiveCreation((Player) context.getSender(), locationName, context);

                }));
    }

    public void setMinLocation(final Location minLocation) {
        this.min = minLocation;
    }

    public void setMaxLocation(final Location maxLocation) {
        this.max = maxLocation;
    }

    @Override
    public String getObjectiveTaskDescription(final String eventualColor, final Player player) {
        return main.getLanguageManager().getString("chat.objectives.taskDescription.reachLocation.base", player)
                .replace("%EVENTUALCOLOR%", eventualColor)
                .replace("%LOCATIONNAME%", getLocationName());
    }

    public void setLocationName(final String locationName) {
        this.locationName = locationName;
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.minLocation", getMinLocation());
        configuration.set(initialPath + ".specifics.maxLocation", getMaxLocation());
        configuration.set(initialPath + ".specifics.locationName", getLocationName());
    }

    @Override
    public void onObjectiveUnlock(ActiveObjective activeObjective) {

    }

    public final Location getMinLocation() {
        return min;
    }

    public final Location getMaxLocation() {
        return max;
    }

    public final String getLocationName() {
        return locationName;
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        min = configuration.getLocation(initialPath + ".specifics.minLocation");
        max = configuration.getLocation(initialPath + ".specifics.maxLocation");
        locationName = configuration.getString(initialPath + ".specifics.locationName");
    }
}
