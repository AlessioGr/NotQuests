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

package rocks.gravili.notquests.paper.structs.objectives;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.incendo.cloud.Command;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.suggestion.Suggestion;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.incendo.cloud.parser.standard.StringParser.greedyStringParser;

public class ReachLocationObjective extends Objective {
    private Location min, max;
    private String locationName;

    public ReachLocationObjective(NotQuests main) {
        super(main);
    }

    public static void handleCommands(
            NotQuests main,
            PaperCommandManager<CommandSender> manager,
            Command.Builder<CommandSender> addObjectiveBuilder,
            final int level) {
        if (!main.getIntegrationsManager().isWorldEditEnabled()) {
            return;
        }

        manager.command(addObjectiveBuilder
                .literal("worldeditselection")
                .required("Location Name", greedyStringParser(), Description.of("Location name"), (context, lastString) -> {
                    main.getUtilManager().sendFancyCommandCompletion(context.sender(), lastString.input().split(" "), "<Location Name>", "");
                    ArrayList<Suggestion> completions = new ArrayList<>();
                    completions.add(Suggestion.suggestion("<Enter new Location name>"));
                    return CompletableFuture.completedFuture(completions);
                })
                .handler((context) -> {
                    final String locationName = context.get("Location Name");
                    main.getIntegrationsManager().getWorldEditManager().handleReachLocationObjectiveCreation((Player) context.sender(), locationName, context, level);
                }));
    }

    @Override
    public String getTaskDescriptionInternal(
            final QuestPlayer questPlayer, final @Nullable ActiveObjective activeObjective) {
        return main.getLanguageManager()
                .getString(
                        "chat.objectives.taskDescription.reachLocation.base",
                        questPlayer,
                        activeObjective,
                        Map.of("%LOCATIONNAME%", getLocationName()));
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.minLocation", getMinLocation());
        configuration.set(initialPath + ".specifics.maxLocation", getMaxLocation());
        configuration.set(initialPath + ".specifics.locationName", getLocationName());
    }

    @Override
    public void onObjectiveUnlock(
            final ActiveObjective activeObjective,
            final boolean unlockedDuringPluginStartupQuestLoadingProcess) {
    }

    @Override
    public void onObjectiveCompleteOrLock(
            final ActiveObjective activeObjective,
            final boolean lockedOrCompletedDuringPluginStartupQuestLoadingProcess,
            final boolean completed) {
    }

    public final Location getMinLocation() {
        return min;
    }

    public void setMinLocation(final Location minLocation) {
        this.min = minLocation;
        if (getLocation() == null) {
            setLocation(minLocation, false);
        }
    }

    public final Location getMaxLocation() {
        return max;
    }

    public void setMaxLocation(final Location maxLocation) {
        this.max = maxLocation;
    }

    public final String getLocationName() {
        return locationName;
    }

    public void setLocationName(final String locationName) {
        this.locationName = locationName;
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        setMinLocation(configuration.getLocation(initialPath + ".specifics.minLocation"));
        setMaxLocation(configuration.getLocation(initialPath + ".specifics.maxLocation"));
        locationName = configuration.getString(initialPath + ".specifics.locationName");
    }
}
