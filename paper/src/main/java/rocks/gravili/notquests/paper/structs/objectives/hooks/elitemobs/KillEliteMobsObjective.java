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

package rocks.gravili.notquests.paper.structs.objectives.hooks.elitemobs;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.incendo.cloud.Command;
import org.incendo.cloud.component.TypedCommandComponent;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.parser.flag.CommandFlag;
import org.incendo.cloud.suggestion.Suggestion;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.objectives.Objective;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.incendo.cloud.parser.standard.StringParser.stringParser;
import static rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueParser.numberVariableParser;

public class KillEliteMobsObjective extends Objective {

    private String eliteMobToKillContainsName; // Blank: doesn't matter
    private int minimumLevel, maximumLevel; // -1: doesn't matter
    private String spawnReason; // Optional. If blank, any spawn reason will be used
    private int
            minimumDamagePercentage; // How much damage the player has to do to the mob minimum. -1:
    // Doesn't matter

    public KillEliteMobsObjective(NotQuests main) {
        super(main);
    }

    public static void handleCommands(
            NotQuests main,
            PaperCommandManager<CommandSender> manager,
            Command.Builder<CommandSender> addObjectiveBuilder,
            final int level) {
        if (!main.getIntegrationsManager().isEliteMobsEnabled()) {
            return;
        }

        CommandFlag<String> mobname = CommandFlag.builder("mobname")
                .withComponent(TypedCommandComponent.builder("mobname", stringParser())
                        .suggestionProvider((context, lastString) -> {
                            main.getUtilManager().sendFancyCommandCompletion((CommandSender) context.sender(), lastString.input().split(" "), "[Part of Elite Mob Name]", "");

                            ArrayList<Suggestion> completions = new ArrayList<>();
                            completions.add(Suggestion.suggestion("any"));
                            if (main.getIntegrationsManager().isEliteMobsEnabled()) {
                                completions.addAll(main.getDataManager().standardEliteMobNamesCompletions.stream().map(Suggestion::suggestion).toList());
                            }
                            return CompletableFuture.completedFuture(completions);
                        }))
                .withDescription(Description.of("Name of the Elite Mob"))
                .build();

        CommandFlag<String> minimumLevel = CommandFlag.builder("minimumLevel")
                .withComponent(TypedCommandComponent.builder("Minimum level", stringParser())
                        .suggestionProvider((context, lastString) -> {
                            main.getUtilManager().sendFancyCommandCompletion((CommandSender) context.sender(), lastString.input().split(" "), "[Minimum Level]", "");
                            return CompletableFuture.completedFuture(main.getDataManager().numberPositiveCompletions.stream().map(Suggestion::suggestion).toList());
                        }))
                .withDescription(Description.of("Minimum level"))
                .build();

        CommandFlag<String> maximumLevel = CommandFlag.builder("maximumLevel")
                .withComponent(TypedCommandComponent.builder("Maximum level", stringParser())
                        .suggestionProvider((context, lastString) -> {
                            main.getUtilManager().sendFancyCommandCompletion((CommandSender) context.sender(), lastString.input().split(" "), "[Minimum Level]", "");
                            return CompletableFuture.completedFuture(main.getDataManager().numberPositiveCompletions.stream().map(Suggestion::suggestion).toList());
                        }))
                .withDescription(Description.of("Maximum level"))
                .build();

        CommandFlag<String> spawnReason = CommandFlag.builder("spawnReason")
                .withComponent(TypedCommandComponent.builder("Spawn Reason", stringParser())
                        .suggestionProvider((context, lastString) -> {
                            main.getUtilManager().sendFancyCommandCompletion((CommandSender) context.sender(), lastString.input().split(" "), "[Spawn Reason]", "");

                            ArrayList<Suggestion> completions = new ArrayList<>();
                            completions.add(Suggestion.suggestion("any"));
                            for (final CreatureSpawnEvent.SpawnReason spawnReasonS : CreatureSpawnEvent.SpawnReason.values()) {
                                completions.add(Suggestion.suggestion(spawnReasonS.toString()));
                            }
                            return CompletableFuture.completedFuture(completions);
                        }))
                .withDescription(Description.of("Spawn Reason"))
                .build();

        CommandFlag<String> minimumDamagePercentage = CommandFlag.builder("minimumDamagePercentage")
                .withComponent(TypedCommandComponent.builder("Minimum Damage Percentage", stringParser())
                        .suggestionProvider((context, lastString) -> {
                            main.getUtilManager().sendFancyCommandCompletion((CommandSender) context.sender(), lastString.input().split(" "), "[Minimum Damage Percentage]", "");

                            ArrayList<Suggestion> completions = new ArrayList<>();
                            for (int i = 50; i <= 100; i++) {
                                completions.add(Suggestion.suggestion("" + i));
                            }

                            return CompletableFuture.completedFuture(completions);
                        }))
                .withDescription(Description.of("Minimum Damage Percentage"))
                .build();

        manager.command(addObjectiveBuilder
                .literal("KillEliteMobs")
                .required("amount", numberVariableParser("amount", null), Description.of("Amount of kills needed"))
                .flag(mobname)
                .flag(minimumLevel)
                .flag(maximumLevel)
                .flag(spawnReason)
                .flag(minimumDamagePercentage)
                .handler(
                        (context) -> {
                            final String amountExpression = context.get("amount");

                            String mobNameString = context.flags().getValue(mobname, "");
                            if (mobNameString == null || mobNameString.equalsIgnoreCase("any")) {
                                mobNameString = "";
                            }
                            mobNameString = mobNameString.replace("_", " ");
                            final String minimumLevelString = context.flags().getValue(minimumLevel, "any");
                            final String maximumLevelString = context.flags().getValue(maximumLevel, "any");

                            int minimumLevelInt = -1;
                            try {
                                minimumLevelInt = Integer.parseInt(minimumLevelString);
                            } catch (NumberFormatException e) {
                                minimumLevelInt = -1;
                            }

                            int maximumLevelInt = -1;
                            try {
                                maximumLevelInt = Integer.parseInt(maximumLevelString);
                            } catch (NumberFormatException e) {
                                maximumLevelInt = -1;
                            }

                            String spawnReasonString = context.flags().getValue(spawnReason, "");
                            if (spawnReasonString == null || spawnReasonString.equalsIgnoreCase("any")) {
                                spawnReasonString = "";
                            }

                            final String minimumDamagePercentageString =
                                    context.flags().getValue(minimumDamagePercentage, "any");

                            int minimumDamagePercentageInt = -1;
                            try {
                                minimumDamagePercentageInt =
                                        Integer.parseInt(minimumDamagePercentageString.replace("%", ""));
                            } catch (NumberFormatException ignored) {
                            }

                            KillEliteMobsObjective killEliteMobsObjective = new KillEliteMobsObjective(main);
                            killEliteMobsObjective.setEliteMobToKillContainsName(mobNameString);
                            killEliteMobsObjective.setMaximumLevel(maximumLevelInt);
                            killEliteMobsObjective.setMinimumLevel(minimumLevelInt);
                            killEliteMobsObjective.setProgressNeededExpression(amountExpression);
                            killEliteMobsObjective.setSpawnReason(spawnReasonString);
                            killEliteMobsObjective.setMinimumDamagePercentage(minimumDamagePercentageInt);

                            main.getObjectiveManager().addObjective(killEliteMobsObjective, context, level);
                        }));
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

    public final String getEliteMobToKillContainsName() {
        return eliteMobToKillContainsName;
    }

    public void setEliteMobToKillContainsName(final String eliteMobToKillContainsName) {
        this.eliteMobToKillContainsName = eliteMobToKillContainsName;
    }

    public final int getMinimumLevel() {
        return minimumLevel;
    }

    public void setMinimumLevel(final int minimumLevel) {
        this.minimumLevel = minimumLevel;
    }

    public final int getMaximumLevel() {
        return maximumLevel;
    }

    public void setMaximumLevel(final int maximumLevel) {
        this.maximumLevel = maximumLevel;
    }

    public final String getSpawnReason() {
        return spawnReason;
    }

    public void setSpawnReason(final String spawnReason) {
        this.spawnReason = spawnReason;
    }

    public final int getMinimumDamagePercentage() {
        return minimumDamagePercentage;
    }

    public void setMinimumDamagePercentage(final int minimumDamagePercentage) {
        this.minimumDamagePercentage = minimumDamagePercentage;
    }

    @Override
    public String getTaskDescriptionInternal(
            final QuestPlayer questPlayer, final @Nullable ActiveObjective activeObjective) {
        String toReturn;
        if (!getEliteMobToKillContainsName().isBlank()) {
            toReturn =
                    main.getLanguageManager()
                            .getString(
                                    "chat.objectives.taskDescription.killEliteMobs.base",
                                    questPlayer,
                                    activeObjective,
                                    Map.of("%ELITEMOBNAME%", getEliteMobToKillContainsName()));
        } else {
            toReturn =
                    main.getLanguageManager()
                            .getString(
                                    "chat.objectives.taskDescription.killEliteMobs.any",
                                    questPlayer,
                                    activeObjective);
        }
        if (getMinimumLevel() != -1) {
            if (getMaximumLevel() != -1) {
                toReturn += "\n        <GRAY>Level: <WHITE>" + getMinimumLevel() + "-" + getMaximumLevel();
            } else {
                toReturn += "\n        <GRAY>Minimum Level: <WHITE>" + getMinimumLevel();
            }
        } else {
            if (getMaximumLevel() != -1) {
                toReturn += "\n        <GRAY>Maximum Level: <WHITE>" + getMaximumLevel();
            }
        }

        if (!getSpawnReason().isBlank()) {
            toReturn += "\n        <GRAY>Spawned from: <WHITE>" + getSpawnReason();
        }

        if (getMinimumDamagePercentage() != -1) {
            toReturn +=
                    "\n        <GRAY>Inflict minimum damage: <WHITE>" + getMinimumDamagePercentage() + "%";
        }
        return toReturn;
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.eliteMobToKill", getEliteMobToKillContainsName());
        configuration.set(initialPath + ".specifics.minimumLevel", getMinimumLevel());
        configuration.set(initialPath + ".specifics.maximumLevel", getMaximumLevel());
        configuration.set(initialPath + ".specifics.spawnReason", getSpawnReason());
        configuration.set(
                initialPath + ".specifics.minimumDamagePercentage", getMinimumDamagePercentage());
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        eliteMobToKillContainsName = configuration.getString(initialPath + ".specifics.eliteMobToKill");
        minimumLevel = configuration.getInt(initialPath + ".specifics.minimumLevel");
        maximumLevel = configuration.getInt(initialPath + ".specifics.maximumLevel");
        spawnReason = configuration.getString(initialPath + ".specifics.spawnReason");
        minimumDamagePercentage =
                configuration.getInt(initialPath + ".specifics.minimumDamagePercentage");
    }
}
