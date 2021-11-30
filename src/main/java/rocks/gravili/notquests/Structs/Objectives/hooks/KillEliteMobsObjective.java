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

package rocks.gravili.notquests.Structs.Objectives.hooks;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.flags.CommandFlag;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import rocks.gravili.notquests.Commands.NotQuestColors;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.Objectives.Objective;
import rocks.gravili.notquests.Structs.Quest;

import java.util.ArrayList;
import java.util.List;

public class KillEliteMobsObjective extends Objective {

    private final NotQuests main;
    private final String eliteMobToKillContainsName; //Blank: doesn't matter
    private final int minimumLevel, maximumLevel; //-1: doesn't matter
    private final String spawnReason; //Optional. If blank, any spawn reason will be used
    private final int minimumDamagePercentage; //How much damage the player has to do to the mob minimum. -1: Doesn't matter
    private final int amountToKill;

    public KillEliteMobsObjective(NotQuests main, final Quest quest, final int objectiveID, String eliteMobToKillContainsName, int minimumLevel, int maximumLevel, String spawnReason, int minimumDamagePercentage, int amountToKill) {
        super(main, quest, objectiveID, amountToKill);
        this.main = main;
        this.eliteMobToKillContainsName = eliteMobToKillContainsName;
        this.minimumLevel = minimumLevel;
        this.maximumLevel = maximumLevel;
        this.spawnReason = spawnReason;
        this.minimumDamagePercentage = minimumDamagePercentage;
        this.amountToKill = amountToKill;
    }

    public KillEliteMobsObjective(NotQuests main, Quest quest, int objectiveNumber, int progressNeeded) {
        super(main, quest, objectiveNumber, progressNeeded);
        final String questName = quest.getQuestName();

        this.main = main;
        eliteMobToKillContainsName = main.getDataManager().getQuestsConfig().getString("quests." + questName + ".objectives." + objectiveNumber + ".specifics.eliteMobToKill");
        minimumLevel = main.getDataManager().getQuestsConfig().getInt("quests." + questName + ".objectives." + objectiveNumber + ".specifics.minimumLevel");
        maximumLevel = main.getDataManager().getQuestsConfig().getInt("quests." + questName + ".objectives." + objectiveNumber + ".specifics.maximumLevel");
        spawnReason = main.getDataManager().getQuestsConfig().getString("quests." + questName + ".objectives." + objectiveNumber + ".specifics.spawnReason");
        minimumDamagePercentage = main.getDataManager().getQuestsConfig().getInt("quests." + questName + ".objectives." + objectiveNumber + ".specifics.minimumDamagePercentage");
        amountToKill = main.getDataManager().getQuestsConfig().getInt("quests." + questName + ".objectives." + objectiveNumber + ".specifics.amountToKill");

    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addObjectiveBuilder) {
        if (!main.isEliteMobsEnabled()) {
            return;
        }


        CommandFlag<String> mobname = CommandFlag
                .newBuilder("mobname")
                .withArgument(StringArgument.<CommandSender>newBuilder("Mob name contains").withSuggestionsProvider(
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            final Audience audience = main.adventure().sender(context.getSender());
                            main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "[Part of Elite Mob Name]", "");

                            ArrayList<String> completions = new ArrayList<>();

                            completions.add("any");
                            if (main.isEliteMobsEnabled()) {
                                completions.addAll(main.getDataManager().standardEliteMobNamesCompletions);
                            }
                            return completions;
                        }
                ).single().build())
                .build();

        CommandFlag<String> minimumLevel = CommandFlag
                .newBuilder("minimumLevel")
                .withArgument(StringArgument.<CommandSender>newBuilder("Minimum level").withSuggestionsProvider(
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            final Audience audience = main.adventure().sender(context.getSender());
                            main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "[Minimum Level]", "");

                            return new ArrayList<>(main.getDataManager().numberPositiveCompletions);
                        }
                ).single().build())
                .build();

        CommandFlag<String> maximumLevel = CommandFlag
                .newBuilder("maximumLevel")
                .withArgument(StringArgument.<CommandSender>newBuilder("Maximum level").withSuggestionsProvider(
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            final Audience audience = main.adventure().sender(context.getSender());
                            main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "[Maximum Level]", "");

                            return new ArrayList<>(main.getDataManager().numberPositiveCompletions);
                        }
                ).single().build())
                .build();


        CommandFlag<String> spawnReason = CommandFlag
                .newBuilder("spawnReason")
                .withArgument(StringArgument.<CommandSender>newBuilder("Spawn Reason").withSuggestionsProvider(
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            final Audience audience = main.adventure().sender(context.getSender());
                            main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "[Spawn Reason]", "");

                            ArrayList<String> completions = new ArrayList<>();
                            for (final CreatureSpawnEvent.SpawnReason spawnReasonS : CreatureSpawnEvent.SpawnReason.values()) {
                                completions.add(spawnReasonS.toString());
                            }
                            return completions;
                        }
                ).single().build())
                .build();

        CommandFlag<String> minimumDamagePercentage = CommandFlag
                .newBuilder("minimumDamagePercentage")
                .withArgument(StringArgument.<CommandSender>newBuilder("Minimum Damage Percentage").withSuggestionsProvider(
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            final Audience audience = main.adventure().sender(context.getSender());
                            main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "[Minimum Damage Percentage]", "");

                            ArrayList<String> completions = new ArrayList<>();
                            for (int i = 50; i <= 100; i++) {
                                completions.add("" + i);
                            }

                            return completions;
                        }
                ).single().build())
                .build();


        manager.command(addObjectiveBuilder.literal("KillEliteMobs")
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("Amount of kills needed"))
                .flag(mobname)
                .flag(minimumLevel)
                .flag(maximumLevel)
                .flag(spawnReason)
                .flag(minimumDamagePercentage)

                .meta(CommandMeta.DESCRIPTION, "Adds a new KillEliteMobs Objective to a quest")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());

                    //Cancel if EliteMobs is not found
                    if (!main.isEliteMobsEnabled()) {
                        audience.sendMessage(MiniMessage.miniMessage().parse(
                                NotQuestColors.errorGradient + "Error: The Elite Mobs integration is not enabled. Thus, you cannot create an EliteMobs Objective."
                        ));
                        return;
                    }
                    final Quest quest = context.get("quest");

                    final int amount = context.get("amount");

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

                    final String minimumDamagePercentageString = context.flags().getValue(minimumDamagePercentage, "any");

                    int minimumDamagePercentageInt = -1;
                    try {
                        minimumDamagePercentageInt = Integer.parseInt(minimumDamagePercentageString.replace("%", ""));
                    } catch (NumberFormatException ignored) {
                    }

                    KillEliteMobsObjective killEliteMobsObjective = new KillEliteMobsObjective(main, quest, quest.getObjectives().size() + 1,
                            mobNameString, minimumLevelInt, maximumLevelInt, spawnReasonString, minimumDamagePercentageInt, amount);

                    quest.addObjective(killEliteMobsObjective, true);

                    audience.sendMessage(MiniMessage.miniMessage().parse(
                            NotQuestColors.successGradient + "KillEliteMobs Objective successfully added to Quest " + NotQuestColors.highlightGradient
                                    + quest.getQuestName() + "</gradient>!</gradient>"
                    ));

                }));
    }

    @Override
    public void save() {
        main.getDataManager().getQuestsConfig().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.eliteMobToKill", getEliteMobToKillContainsName());
        main.getDataManager().getQuestsConfig().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.minimumLevel", getMinimumLevel());
        main.getDataManager().getQuestsConfig().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.maximumLevel", getMaximumLevel());
        main.getDataManager().getQuestsConfig().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.spawnReason", getSpawnReason());
        main.getDataManager().getQuestsConfig().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.minimumDamagePercentage", getMinimumDamagePercentage());
        main.getDataManager().getQuestsConfig().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.amountToKill", getAmountToKill());

    }

    public final String getEliteMobToKillContainsName() {
        return eliteMobToKillContainsName;
    }

    public final int getAmountToKill() {
        return amountToKill;
    }

    public final int getMinimumLevel() {
        return minimumLevel;
    }

    public final int getMaximumLevel() {
        return maximumLevel;
    }

    public final String getSpawnReason() {
        return spawnReason;
    }

    public final int getMinimumDamagePercentage() {
        return minimumDamagePercentage;
    }

    @Override
    public String getObjectiveTaskDescription(final String eventualColor, final Player player) {
        String toReturn;
        if (!getEliteMobToKillContainsName().isBlank()) {
            toReturn = main.getLanguageManager().getString("chat.objectives.taskDescription.killEliteMobs.base", player)
                    .replace("%EVENTUALCOLOR%", eventualColor)
                    .replace("%ELITEMOBNAME%", "" + getEliteMobToKillContainsName());
        } else {
            toReturn = main.getLanguageManager().getString("chat.objectives.taskDescription.killEliteMobs.any", player)
                    .replace("%EVENTUALCOLOR%", eventualColor);
        }
        if (getMinimumLevel() != -1) {
            if (getMaximumLevel() != -1) {
                toReturn += "\n        §7" + eventualColor + "Level: §f" + eventualColor + getMinimumLevel() + "-" + getMaximumLevel();
            } else {
                toReturn += "\n        §7" + eventualColor + "Minimum Level: §f" + eventualColor + getMinimumLevel();
            }
        } else {
            if (getMaximumLevel() != -1) {
                toReturn += "\n        §7" + eventualColor + "Maximum Level: §f" + eventualColor + getMaximumLevel();
            }
        }

        if (!getSpawnReason().isBlank()) {
            toReturn += "\n        §7" + eventualColor + "Spawned from: §f" + eventualColor + getSpawnReason();
        }

        if (getMinimumDamagePercentage() != -1) {
            toReturn += "\n        §7" + eventualColor + "Inflict minimum damage: §f" + eventualColor + getMinimumDamagePercentage() + "%";
        }
        return toReturn;
    }
}
