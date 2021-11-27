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

package rocks.gravili.notquests.Structs.Triggers.TriggerTypes;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import rocks.gravili.notquests.Commands.NotQuestColors;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.Quest;
import rocks.gravili.notquests.Structs.Triggers.Action;
import rocks.gravili.notquests.Structs.Triggers.Trigger;

import java.util.ArrayList;
import java.util.List;

public class WorldEnterTrigger extends Trigger {

    private final NotQuests main;
    private final String worldToEnterName;

    public WorldEnterTrigger(final NotQuests main, final Quest quest, final int triggerID, Action action, int applyOn, String worldName, long amountNeeded) {
        super(main, quest, triggerID, action, applyOn, worldName, amountNeeded);
        this.main = main;

        this.worldToEnterName = main.getDataManager().getQuestsConfig().getString("quests." + getQuest().getQuestName() + ".triggers." + triggerID + ".specifics.worldToEnter", "ALL");
    }

    public WorldEnterTrigger(final NotQuests main, final Quest quest, final int triggerID, Action action, int applyOn, String worldName, long amountNeeded, String worldToEnterName) {
        super(main, quest, triggerID, action, applyOn, worldName, amountNeeded);
        this.main = main;
        this.worldToEnterName = worldToEnterName;
    }

    public final String getWorldToEnterName() {
        return worldToEnterName;
    }

    @Override
    public void save() {
        main.getDataManager().getQuestsConfig().set("quests." + getQuest().getQuestName() + ".triggers." + getTriggerID() + ".specifics.worldToEnter", getWorldToEnterName());

    }

    @Override
    public String getTriggerDescription() {
        return "World to enter: §f" + getWorldToEnterName();
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addTriggerBuilder) {
        manager.command(addTriggerBuilder.literal("WORLDENTER")
                .argument(StringArgument.<CommandSender>newBuilder("world to enter").withSuggestionsProvider(
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            final Audience audience = main.adventure().sender(context.getSender());
                            main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "[World Name / 'ALL']", "[Amount of Enters]");

                            ArrayList<String> completions = new ArrayList<>();

                            completions.add("ALL");

                            for (final World world : Bukkit.getWorlds()) {
                                completions.add(world.getName());
                            }

                            return completions;
                        }
                ).single().build(), ArgumentDescription.of("Name of the world which needs to be entered"))
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("Amount of times the world needs to be entered."))
                .flag(main.getCommandManager().applyOn)
                .flag(main.getCommandManager().triggerWorldString)
                .meta(CommandMeta.DESCRIPTION, "Triggers when the player enters a specific world.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());

                    final Quest quest = context.get("quest");
                    final Action action = context.get("action");

                    final String worldToEnterName = context.get("world to enter");

                    int amountOfWorldEnters = context.get("amount");

                    final int applyOn = context.flags().getValue(main.getCommandManager().applyOn, 0); //0 = Quest
                    final String worldString = context.flags().getValue(main.getCommandManager().triggerWorldString, null);


                    WorldEnterTrigger worldEnterTrigger = new WorldEnterTrigger(main, quest, quest.getTriggers().size() + 1, action, applyOn, worldString, amountOfWorldEnters, worldToEnterName);

                    quest.addTrigger(worldEnterTrigger);

                    audience.sendMessage(MiniMessage.miniMessage().parse(
                            NotQuestColors.successGradient + "WORLDENTER Trigger successfully added to Quest " + NotQuestColors.highlightGradient
                                    + quest.getQuestName() + "</gradient>!</gradient>"
                    ));

                }));
    }
}
